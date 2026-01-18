package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.ItemViewer.ItemData;
import net.felix.utilities.ItemViewer.PriceData;
import net.felix.utilities.ItemViewer.BlueprintShopData;
import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility-Klasse für das Clipboard (Bauplan-Kosten)
 */
public class ClipboardUtility {
    
    /**
     * Repräsentiert einen Clipboard-Eintrag (ein Bauplan)
     */
    public static class ClipboardEntry {
        public String blueprintName;
        public PriceData price;
        public BlueprintShopData blueprintShop;
        public int quantity; // Anzahl (Standard: 1)
        public Integer clipboardId; // Optional: Interne ID für Baupläne mit doppelten Namen (nicht sichtbar)
        
        public ClipboardEntry(String blueprintName, PriceData price, BlueprintShopData blueprintShop, Integer clipboardId) {
            this.blueprintName = blueprintName;
            this.price = price;
            this.blueprintShop = blueprintShop;
            this.quantity = 1;
            this.clipboardId = clipboardId; // null wenn nicht vorhanden
        }
    }
    
    // Liste der Clipboard-Einträge
    private static final List<ClipboardEntry> clipboardEntries = new ArrayList<>();
    
    // KeyBinding für das Togglen des Clipboards
    private static KeyBinding toggleClipboardKeyBinding;
    
    // Flag für asynchrones Laden
    private static volatile boolean clipboardEntriesLoaded = false;
    private static Thread loadClipboardThread = null;
    
    // Flag für asynchrones Speichern (verhindert mehrfaches gleichzeitiges Speichern)
    private static final AtomicBoolean saveScheduled = new AtomicBoolean(false);
    private static Thread saveClipboardThread = null;
    
    /**
     * Initialisiert das Clipboard-System
     */
    public static void initialize() {
        // WICHTIG: KeyBinding ZUERST registrieren (wie bei KillsUtility und BPViewerUtility)
        // Muss VOR ClientTickEvents.END_CLIENT_TICK.register() passieren!
        registerKeyBinding();
        
        // Lade gespeicherte Clipboard-Einträge aus der Config asynchron (um Lag beim Start zu vermeiden)
        loadClipboardEntriesAsync();
        
        // Initialisiere Seitenanzahl (wichtig: muss vor dem ersten Rendering passieren)
        // Wird nach dem Laden aktualisiert
        updateTotalPages();
        
        // Initialisiere ClipboardCoinCollector
        ClipboardCoinCollector.initialize();
        
        // Initialisiere ClipboardPaperShredsCollector
        ClipboardPaperShredsCollector.initialize();
        
        // Initialisiere ClipboardAmbossRessourceCollector
        ClipboardAmbossRessourceCollector.initialize();
        
        // Initialisiere persistente Materialien/Ressourcen
        CollectedMaterialsResourcesStorage.initialize();
        
        // Initialisiere ClipboardMaterialCollector
        ClipboardMaterialCollector.initialize();
        
        // Registriere Chat-Event für Coin-Updates
        net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            String messageText = message.getString();
            if (messageText != null && !messageText.isEmpty()) {
                // Ignoriere unsere eigenen CCLive-Nachrichten um Endlosschleifen zu vermeiden
                if (!messageText.contains("[CCLive-Debug]")) {
                    boolean shouldSuppress = ClipboardCoinCollector.processChatMessage(messageText);
                    if (shouldSuppress) {
                        return false; // Nachricht unterdrücken
                    }
                }
            }
            return true; // Nachricht anzeigen
        });
        
        // Registriere HUD-Rendering für Overlay außerhalb von Inventaren
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client == null) return;
            
            // Nur rendern wenn kein Screen offen ist (außerhalb von Inventaren)
            if (client.currentScreen == null) {
                int mouseX = (int) client.mouse.getX();
                int mouseY = (int) client.mouse.getY();
                float delta = 1.0f; // Delta wird nicht benötigt für statisches Rendering
                net.felix.utilities.DragOverlay.ClipboardDraggableOverlay.renderInGame(drawContext, mouseX, mouseY, delta);
                
                // Render Button Tooltips
                net.felix.utilities.DragOverlay.ClipboardDraggableOverlay.renderButtonTooltips(drawContext, mouseX, mouseY);
            }
        });
        
        // Mouse-Clicks werden über ScreenMixin.onMouseClicked behandelt
        // Für HUD (außerhalb von Screens) müssen wir einen anderen Ansatz verwenden
        // Wir verwenden ClientTickEvents um auf Mausklicks zu reagieren
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen == null && client.mouse != null) {
                // Prüfe ob Maus geklickt wurde (wird in einem Mixin behandelt)
                // Für jetzt: Button-Clicks werden in HandledScreenMixin und ScreenMixin behandelt
            }
            
            // Aktualisiere hideHover Flag jeden Tick (nicht nur beim Rendering)
            // Dies stellt sicher, dass F1/Tab sofort erkannt werden, auch wenn Rendering gedrosselt ist
            net.felix.utilities.DragOverlay.ClipboardDraggableOverlay.updateHideHover();
            
            // Prüfe Clipboard-Toggle Hotkey (funktioniert sowohl im HUD als auch in Screens)
            // Im HUD: wasPressed() funktioniert direkt (wie bei KillsUtility und BPViewerUtility)
            // In Screens: wird über ScreenMixin.handleKeyPress behandelt
            if (toggleClipboardKeyBinding != null && toggleClipboardKeyBinding.wasPressed()) {
                // Prüfe ob ein Textfeld fokussiert ist (Chat, etc.) - ignoriere dann den Hotkey
                if (client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
                    return; // Ignoriere Hotkey wenn Chat offen ist
                }
                
                toggleClipboard();
            }
        });
    }
    
    /**
     * Registriert den KeyBinding für das Togglen des Clipboards
     */
    private static void registerKeyBinding() {
        toggleClipboardKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cclive-utilities.toggle-clipboard",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN, // Kein Standard-Key (unbelegt)
            "categories.cclive-utilities.itemviewer"
        ));
    }
    
    /**
     * Togglet die Sichtbarkeit des Clipboards
     * Synchronisiert sowohl clipboardEnabled als auch showClipboard (wie im Overlay Picker)
     */
    public static void toggleClipboard() {
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        boolean oldValue = config.clipboardEnabled;
        boolean newValue = !oldValue;
        config.clipboardEnabled = newValue;
        config.showClipboard = newValue; // Synchronisiere mit clipboardEnabled Option
        CCLiveUtilitiesConfig.HANDLER.save();
    }
    
    /**
     * Behandelt Key-Press direkt (für Verwendung in Mixins, wenn Screens geöffnet sind)
     * @param keyCode Der Key-Code (z.B. GLFW.GLFW_KEY_F8)
     * @param scanCode Der Scan-Code (für bessere Key-Erkennung)
     * @return true wenn der Key behandelt wurde
     */
    public static boolean handleKeyPress(int keyCode, int scanCode) {
        try {
            // Prüfe ob der gedrückte Key dem konfigurierten KeyBinding entspricht
            if (toggleClipboardKeyBinding != null) {
                // Verwende matchesKey um zu prüfen, ob der gedrückte Key dem konfigurierten KeyBinding entspricht
                if (toggleClipboardKeyBinding.matchesKey(keyCode, scanCode)) {
                    // Prüfe ob ein Textfeld fokussiert ist (Chat, Inventar-Suchfeld, etc.)
                    // Verhindert, dass der Hotkey aktiviert wird, wenn der Spieler tippt
                    net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                    if (client != null && isTextFieldFocused(client)) {
                        return false; // Ignoriere Hotkey wenn Textfeld fokussiert ist
                    }
                    
                    toggleClipboard();
                    return true;
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
        return false;
    }
    
    /**
     * Prüft ob ein Textfeld fokussiert ist (Chat, Inventar-Suchfeld, etc.)
     * Verhindert, dass Hotkeys aktiviert werden, wenn der Spieler tippt
     */
    private static boolean isTextFieldFocused(net.minecraft.client.MinecraftClient client) {
        if (client == null || client.currentScreen == null) {
            return false;
        }
        
        // Prüfe ob ChatScreen offen ist
        if (client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            return true;
        }
        
        // Prüfe ob ein TextFieldWidget fokussiert ist (z.B. in Inventaren)
        // Verwende Reflection um auf fokussierte Widgets zuzugreifen
        try {
            // Prüfe ob Screen ein fokussiertes Widget hat
            java.lang.reflect.Method getFocusedMethod = client.currentScreen.getClass().getMethod("getFocused");
            if (getFocusedMethod != null) {
                Object focused = getFocusedMethod.invoke(client.currentScreen);
                if (focused instanceof net.minecraft.client.gui.widget.TextFieldWidget) {
                    net.minecraft.client.gui.widget.TextFieldWidget textField = (net.minecraft.client.gui.widget.TextFieldWidget) focused;
                    if (textField.isFocused()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Silent error handling - Reflection kann fehlschlagen
        }
        
        return false;
    }
    
    /**
     * Fügt einen Bauplan zum Clipboard hinzu (überladene Methode mit ItemData)
     * @param itemData ItemData des Bauplans
     * @return true wenn erfolgreich hinzugefügt, false wenn nicht gefunden
     */
    public static boolean addBlueprint(ItemData itemData) {
        if (itemData == null) {
            return false;
        }
        
        // Prüfe ob es ein Bauplan ist
        if (itemData.info == null || !Boolean.TRUE.equals(itemData.info.blueprint)) {
            return false; // Kein Bauplan
        }
        
        // Verwende clipboard_id falls vorhanden
        Integer clipboardId = itemData.clipboard_id;
        
        // Prüfe ob bereits vorhanden (nach Name + clipboard_id)
        // Thread-sicher: Synchronisiere Zugriff auf clipboardEntries
        synchronized (clipboardEntries) {
            for (ClipboardEntry entry : clipboardEntries) {
                boolean nameMatches = entry.blueprintName.equals(itemData.name);
                boolean idMatches = (entry.clipboardId == null && clipboardId == null) || 
                                   (entry.clipboardId != null && entry.clipboardId.equals(clipboardId));
                
                if (nameMatches && idMatches) {
                    // Bereits vorhanden, erhöhe Anzahl
                    entry.quantity++;
                    // Speichere in Config asynchron
                    saveClipboardEntries();
                    updateTotalPages();
                    return true;
                }
            }
            
            // Neuer Eintrag
            ClipboardEntry entry = new ClipboardEntry(itemData.name, itemData.price, itemData.blueprint_shop, clipboardId);
            clipboardEntries.add(entry);
        }
        
        // Speichere in Config asynchron
        saveClipboardEntries();
        updateTotalPages();
        
        return true;
    }
    
    /**
     * Fügt einen Bauplan zum Clipboard hinzu (Legacy-Methode für Rückwärtskompatibilität)
     * @param blueprintName Name des Bauplans
     * @return true wenn erfolgreich hinzugefügt, false wenn nicht gefunden
     */
    public static boolean addBlueprint(String blueprintName) {
        // Suche den Bauplan in items.json
        ItemData itemData = ItemViewerUtility.findItemByName(blueprintName);
        
        if (itemData == null) {
            return false; // Bauplan nicht gefunden
        }
        
        // Verwende die neue Methode mit ItemData
        return addBlueprint(itemData);
    }
    
    /**
     * Entfernt einen Bauplan aus dem Clipboard
     * @param blueprintName Name des Bauplans
     * @param clipboardId Optional: clipboard_id für Baupläne mit doppelten Namen
     * @return true wenn erfolgreich entfernt
     */
    public static boolean removeBlueprint(String blueprintName, Integer clipboardId) {
        boolean removed;
        synchronized (clipboardEntries) {
            removed = clipboardEntries.removeIf(entry -> {
                boolean nameMatches = entry.blueprintName.equals(blueprintName);
                boolean idMatches = (entry.clipboardId == null && clipboardId == null) || 
                                   (entry.clipboardId != null && entry.clipboardId.equals(clipboardId));
                return nameMatches && idMatches;
            });
        }
        if (removed) {
            // Speichere in Config asynchron
            saveClipboardEntries();
            updateTotalPages();
            // Entferne nicht mehr benötigte Materialien aus der Clipboard-Speicherung
            ClipboardDraggableOverlay.cleanupUnusedMaterials();
        }
        return removed;
    }
    
    /**
     * Entfernt einen Bauplan aus dem Clipboard (Legacy-Methode für Rückwärtskompatibilität)
     * @param blueprintName Name des Bauplans
     * @return true wenn erfolgreich entfernt
     */
    public static boolean removeBlueprint(String blueprintName) {
        // Entferne alle Einträge mit diesem Namen (falls mehrere vorhanden)
        return removeBlueprint(blueprintName, null);
    }
    
    /**
     * Entfernt alle Baupläne aus dem Clipboard
     */
    public static void clearClipboard() {
        synchronized (clipboardEntries) {
            clipboardEntries.clear();
        }
        // Speichere in Config asynchron
        saveClipboardEntries();
        updateTotalPages();
        // Entferne alle Materialien aus der Clipboard-Speicherung
        ClipboardDraggableOverlay.cleanupUnusedMaterials();
    }
    
    /**
     * Lädt Clipboard-Einträge aus der Config asynchron im Hintergrund
     */
    private static void loadClipboardEntriesAsync() {
        if (loadClipboardThread != null && loadClipboardThread.isAlive()) {
            return; // Bereits am Laden
        }
        
        clipboardEntriesLoaded = false;
        
        loadClipboardThread = new Thread(() -> {
            try {
                loadClipboardEntries();
                clipboardEntriesLoaded = true;
                
                // Aktualisiere Seitenanzahl nach dem Laden (auf dem Hauptthread)
                net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                    updateTotalPages();
                });
            } catch (Exception e) {
                System.err.println("Fehler beim asynchronen Laden der Clipboard-Einträge: " + e.getMessage());
                e.printStackTrace();
                clipboardEntriesLoaded = true; // Setze Flag trotzdem, um Endlosschleifen zu vermeiden
            }
        }, "CCLive-Clipboard-Loader");
        loadClipboardThread.setDaemon(true); // Thread wird beendet, wenn Hauptthread beendet wird
        loadClipboardThread.start();
    }
    
    /**
     * Lädt Clipboard-Einträge aus der Config (synchron, für asynchronen Aufruf)
     */
    private static void loadClipboardEntries() {
        clipboardEntries.clear();
        
        List<CCLiveUtilitiesConfig.ClipboardEntryData> savedEntries = 
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardEntries;
        
        if (savedEntries == null || savedEntries.isEmpty()) {
            return;
        }
        
        for (CCLiveUtilitiesConfig.ClipboardEntryData savedEntry : savedEntries) {
            if (savedEntry.blueprintName == null || savedEntry.blueprintName.isEmpty()) {
                continue;
            }
            
            // Warte bis items.json geladen ist
            int maxWait = 100; // Maximal 1 Sekunde warten (10 Checks à 100ms)
            int waited = 0;
            while (!ItemViewerUtility.areItemsLoaded() && waited < maxWait) {
                try {
                    Thread.sleep(100);
                    waited++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // Suche den Bauplan in items.json (mit clipboard_id falls vorhanden)
            ItemData itemData;
            if (savedEntry.clipboardId != null) {
                itemData = ItemViewerUtility.findItemByNameAndClipboardId(savedEntry.blueprintName, savedEntry.clipboardId);
            } else {
                itemData = ItemViewerUtility.findItemByName(savedEntry.blueprintName);
            }
            
            if (itemData == null) {
                continue; // Bauplan nicht gefunden, überspringe
            }
            
            // Prüfe ob es ein Bauplan ist
            if (itemData.info == null || itemData.info.blueprint == null || !itemData.info.blueprint) {
                continue; // Kein Bauplan, überspringe
            }
            
            // Erstelle ClipboardEntry (verwende clipboardId aus savedEntry oder itemData)
            Integer clipboardId = savedEntry.clipboardId != null ? savedEntry.clipboardId : itemData.clipboard_id;
            ClipboardEntry entry = new ClipboardEntry(savedEntry.blueprintName, itemData.price, itemData.blueprint_shop, clipboardId);
            entry.quantity = Math.max(1, savedEntry.quantity); // Mindestens 1
            clipboardEntries.add(entry);
        }
    }
    
    /**
     * Prüft ob Clipboard-Einträge geladen sind
     */
    public static boolean areClipboardEntriesLoaded() {
        return clipboardEntriesLoaded;
    }
    
    /**
     * Speichert Clipboard-Einträge in der Config asynchron (um Lag zu vermeiden)
     */
    public static void saveClipboardEntries() {
        // Verwende AtomicBoolean um sicherzustellen, dass nur ein Save-Thread läuft
        if (!saveScheduled.compareAndSet(false, true)) {
            return; // Speichern bereits geplant
        }
        
        // Beende vorherigen Save-Thread falls noch aktiv
        if (saveClipboardThread != null && saveClipboardThread.isAlive()) {
            try {
                saveClipboardThread.interrupt();
            } catch (Exception e) {
                // Ignoriere Fehler
            }
        }
        
        saveClipboardThread = new Thread(() -> {
            try {
                // Warte kurz, um mehrere aufeinanderfolgende Speichervorgänge zu bündeln
                Thread.sleep(200); // 200ms Debounce
                
                // Erstelle Kopie der Einträge (Thread-sicher)
                List<CCLiveUtilitiesConfig.ClipboardEntryData> savedEntries = new ArrayList<>();
                synchronized (clipboardEntries) {
                    for (ClipboardEntry entry : clipboardEntries) {
                        savedEntries.add(new CCLiveUtilitiesConfig.ClipboardEntryData(
                            entry.blueprintName,
                            entry.quantity,
                            entry.clipboardId // Speichere clipboardId (kann null sein)
                        ));
                    }
                }
                
                // Speichere auf dem Hauptthread (Config-Handler muss auf dem Hauptthread laufen)
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                if (client != null) {
                    client.execute(() -> {
                        try {
                            CCLiveUtilitiesConfig.HANDLER.instance().clipboardEntries = savedEntries;
                            CCLiveUtilitiesConfig.HANDLER.save();
                        } catch (Exception e) {
                            System.err.println("Fehler beim Speichern der Clipboard-Einträge: " + e.getMessage());
                            e.printStackTrace();
                        } finally {
                            saveScheduled.set(false);
                        }
                    });
                } else {
                    saveScheduled.set(false);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                saveScheduled.set(false);
            } catch (Exception e) {
                System.err.println("Fehler beim asynchronen Speichern der Clipboard-Einträge: " + e.getMessage());
                e.printStackTrace();
                saveScheduled.set(false);
            }
        }, "CCLive-Clipboard-Saver");
        saveClipboardThread.setDaemon(true);
        saveClipboardThread.start();
    }
    
    /**
     * Gibt alle Clipboard-Einträge zurück (Thread-sicher)
     */
    public static List<ClipboardEntry> getEntries() {
        synchronized (clipboardEntries) {
            return new ArrayList<>(clipboardEntries);
        }
    }
    
    /**
     * Gibt einen Clipboard-Eintrag für eine bestimmte Seite zurück
     * Seite 1 = Gesamtliste (null), Seite 2+ = einzelner Bauplan
     * @param page Seitenzahl (1-basiert)
     * @return ClipboardEntry für die Seite, oder null für Seite 1 (Gesamtliste)
     */
    public static ClipboardEntry getEntryForPage(int page) {
        if (page <= 1) {
            return null; // Seite 1 = Gesamtliste
        }
        
        int index = page - 2; // Seite 2 = Index 0, Seite 3 = Index 1, etc.
        synchronized (clipboardEntries) {
            if (index >= 0 && index < clipboardEntries.size()) {
                return clipboardEntries.get(index);
            }
        }
        
        return null;
    }
    
    /**
     * Aktualisiert die Gesamtanzahl der Seiten
     * Seite 1 = Gesamtliste, Seite 2+ = einzelne Baupläne
     */
    private static void updateTotalPages() {
        // Seite 1 (Gesamtliste) + Anzahl der Baupläne
        int size;
        synchronized (clipboardEntries) {
            size = clipboardEntries.size();
        }
        int totalPages = 1 + size;
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardTotalPages = totalPages;
        CCLiveUtilitiesConfig.HANDLER.save();
        
        // Stelle sicher, dass currentPage im gültigen Bereich ist
        int currentPage = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCurrentPage;
        if (currentPage > totalPages) {
            CCLiveUtilitiesConfig.HANDLER.instance().clipboardCurrentPage = totalPages;
            CCLiveUtilitiesConfig.HANDLER.save();
        }
    }
    
    /**
     * Gibt die Gesamtanzahl der Seiten zurück
     */
    public static int getTotalPages() {
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardTotalPages;
    }
    
    /**
     * Gibt die aktuelle Seite zurück
     */
    public static int getCurrentPage() {
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardCurrentPage;
    }
    
    /**
     * Setzt die aktuelle Seite
     */
    public static void setCurrentPage(int page) {
        int totalPages = getTotalPages();
        page = Math.max(1, Math.min(page, totalPages));
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardCurrentPage = page;
        CCLiveUtilitiesConfig.HANDLER.save();
    }
}

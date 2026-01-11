package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.ItemViewer.ItemData;
import net.felix.utilities.ItemViewer.PriceData;
import net.felix.utilities.ItemViewer.BlueprintShopData;
import net.felix.utilities.ItemViewer.ItemViewerUtility;

import java.util.ArrayList;
import java.util.List;

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
        
        public ClipboardEntry(String blueprintName, PriceData price, BlueprintShopData blueprintShop) {
            this.blueprintName = blueprintName;
            this.price = price;
            this.blueprintShop = blueprintShop;
            this.quantity = 1;
        }
    }
    
    // Liste der Clipboard-Einträge
    private static final List<ClipboardEntry> clipboardEntries = new ArrayList<>();
    
    /**
     * Initialisiert das Clipboard-System
     */
    public static void initialize() {
        // Lade gespeicherte Clipboard-Einträge aus der Config
        loadClipboardEntries();
        
        // Initialisiere Seitenanzahl (wichtig: muss vor dem ersten Rendering passieren)
        updateTotalPages();
        
        // Initialisiere ClipboardCoinCollector
        ClipboardCoinCollector.initialize();
        
        // Initialisiere ClipboardPaperShredsCollector
        ClipboardPaperShredsCollector.initialize();
        
        // Initialisiere ClipboardAmbossRessourceCollector
        ClipboardAmbossRessourceCollector.initialize();
        
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
        });
    }
    
    /**
     * Fügt einen Bauplan zum Clipboard hinzu
     * @param blueprintName Name des Bauplans
     * @return true wenn erfolgreich hinzugefügt, false wenn nicht gefunden
     */
    public static boolean addBlueprint(String blueprintName) {
        // Suche den Bauplan in items.json
        ItemData itemData = ItemViewerUtility.findItemByName(blueprintName);
        
        if (itemData == null) {
            return false; // Bauplan nicht gefunden
        }
        
        // Prüfe ob es ein Bauplan ist
        if (itemData.info == null || !Boolean.TRUE.equals(itemData.info.blueprint)) {
            return false; // Kein Bauplan
        }
        
        // Prüfe ob bereits vorhanden
        for (ClipboardEntry entry : clipboardEntries) {
            if (entry.blueprintName.equals(blueprintName)) {
                // Bereits vorhanden, erhöhe Anzahl
                entry.quantity++;
                // Speichere in Config
                saveClipboardEntries();
                updateTotalPages();
                return true;
            }
        }
        
        // Neuer Eintrag
        ClipboardEntry entry = new ClipboardEntry(blueprintName, itemData.price, itemData.blueprint_shop);
        clipboardEntries.add(entry);
        
        // Speichere in Config
        saveClipboardEntries();
        updateTotalPages();
        
        return true;
    }
    
    /**
     * Entfernt einen Bauplan aus dem Clipboard
     * @param blueprintName Name des Bauplans
     * @return true wenn erfolgreich entfernt
     */
    public static boolean removeBlueprint(String blueprintName) {
        boolean removed = clipboardEntries.removeIf(entry -> entry.blueprintName.equals(blueprintName));
        if (removed) {
            // Speichere in Config
            saveClipboardEntries();
            updateTotalPages();
            // Entferne nicht mehr benötigte Materialien aus der Clipboard-Speicherung
            ClipboardDraggableOverlay.cleanupUnusedMaterials();
        }
        return removed;
    }
    
    /**
     * Entfernt alle Baupläne aus dem Clipboard
     */
    public static void clearClipboard() {
        clipboardEntries.clear();
        // Speichere in Config
        saveClipboardEntries();
        updateTotalPages();
        // Entferne alle Materialien aus der Clipboard-Speicherung
        ClipboardDraggableOverlay.cleanupUnusedMaterials();
    }
    
    /**
     * Lädt Clipboard-Einträge aus der Config
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
            
            // Suche den Bauplan in items.json
            ItemData itemData = ItemViewerUtility.findItemByName(savedEntry.blueprintName);
            
            if (itemData == null) {
                continue; // Bauplan nicht gefunden, überspringe
            }
            
            // Prüfe ob es ein Bauplan ist
            if (itemData.info == null || itemData.info.blueprint == null || !itemData.info.blueprint) {
                continue; // Kein Bauplan, überspringe
            }
            
            // Erstelle ClipboardEntry
            ClipboardEntry entry = new ClipboardEntry(savedEntry.blueprintName, itemData.price, itemData.blueprint_shop);
            entry.quantity = Math.max(1, savedEntry.quantity); // Mindestens 1
            clipboardEntries.add(entry);
        }
    }
    
    /**
     * Speichert Clipboard-Einträge in der Config
     */
    public static void saveClipboardEntries() {
        List<CCLiveUtilitiesConfig.ClipboardEntryData> savedEntries = new ArrayList<>();
        
        for (ClipboardEntry entry : clipboardEntries) {
            savedEntries.add(new CCLiveUtilitiesConfig.ClipboardEntryData(
                entry.blueprintName,
                entry.quantity
            ));
        }
        
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardEntries = savedEntries;
        CCLiveUtilitiesConfig.HANDLER.save();
    }
    
    /**
     * Gibt alle Clipboard-Einträge zurück
     */
    public static List<ClipboardEntry> getEntries() {
        return new ArrayList<>(clipboardEntries);
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
        if (index >= 0 && index < clipboardEntries.size()) {
            return clipboardEntries.get(index);
        }
        
        return null;
    }
    
    /**
     * Aktualisiert die Gesamtanzahl der Seiten
     * Seite 1 = Gesamtliste, Seite 2+ = einzelne Baupläne
     */
    private static void updateTotalPages() {
        // Seite 1 (Gesamtliste) + Anzahl der Baupläne
        int totalPages = 1 + clipboardEntries.size();
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

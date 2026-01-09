package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sammelt Amboss- und Ressourcen-Anzahlen aus dem Ressourcen-Bag Inventar (Symbol Ⳅ)
 * Liest die Anzahl aus den Tooltips der Items im Format: "Name\nAnzahl x\nFür Details klicken"
 */
public class ClipboardAmbossRessourceCollector {
    private static final Map<String, Long> ambossItems = new HashMap<>(); // ItemName -> Anzahl
    private static final Map<String, Long> ressourceItems = new HashMap<>(); // ItemName -> Anzahl
    
    // Pattern für die Anzahl-Zeile im Tooltip: "Anzahl" + Leerzeichen + Zahl (z.B. "Anzahl 10000", "Anzahl 1,000")
    // Format: "Anzahl" (gold) + " " + Zahl (white)
    // Matcht: eine oder mehrere Ziffern, gefolgt von optionalen Komma-Gruppen (jeweils 3 Ziffern)
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("Anzahl\\s+(\\d+(?:,\\d{3})*)", Pattern.CASE_INSENSITIVE);
    
    // Cache für den letzten Screen-Titel, um zu erkennen wenn sich das Inventar ändert
    private static String lastScreenTitle = "";
    private static int lastSlotCount = 0;
    
    /**
     * Initialisiert den ClipboardAmbossRessourceCollector
     */
    public static void initialize() {
        // Registriere Tick-Event
        ClientTickEvents.END_CLIENT_TICK.register(ClipboardAmbossRessourceCollector::onClientTick);
    }
    
    /**
     * Wird jeden Tick aufgerufen
     * Läuft nur wenn alle Bedingungen erfüllt sind:
     * - Clipboard aktiviert und Baupläne vorhanden
     * - Inventar geöffnet
     * - Ressourcen-Bag Inventar (Symbol Ⳅ im Titel)
     */
    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        
        // Prüfe zuerst ob Clipboard aktiv ist und Baupläne vorhanden sind
        if (!shouldCollectAmbossRessource()) {
            return;
        }
        
        // Prüfe ob ein Inventar geöffnet ist
        if (!(client.currentScreen instanceof HandledScreen<?> handledScreen)) {
            // Inventar geschlossen - Cache zurücksetzen
            lastScreenTitle = "";
            lastSlotCount = 0;
            return;
        }
        
        // Prüfe ob wir im Ressourcen-Bag Inventar sind (Symbol Ⳅ im Titel)
        net.minecraft.text.Text titleText = handledScreen.getTitle();
        String screenTitle = titleText != null ? titleText.getString() : "";
        
        if (!net.felix.utilities.Overall.ZeichenUtility.containsUiRessourceBag(screenTitle)) {
            // Nicht im Ressourcen-Bag - Cache zurücksetzen
            lastScreenTitle = "";
            lastSlotCount = 0;
            return;
        }
        
        // Prüfe ob sich das Inventar geändert hat (neue Seite oder neu geladen)
        int currentSlotCount = handledScreen.getScreenHandler().slots.size();
        boolean inventoryChanged = !screenTitle.equals(lastScreenTitle) || currentSlotCount != lastSlotCount;
        
        if (inventoryChanged) {
            // Inventar hat sich geändert - lösche Cache und sammle neu
            ambossItems.clear();
            ressourceItems.clear();
            lastScreenTitle = screenTitle;
            lastSlotCount = currentSlotCount;
        }
        
        // Sammle Items aus allen Slots
        try {
            List<Slot> slots = handledScreen.getScreenHandler().slots;
            for (Slot slot : slots) {
                ItemStack stack = slot.getStack();
                if (stack.isEmpty()) {
                    continue;
                }
                
                // Hole Tooltip über Reflection (wie in ClipboardPaperShredsCollector)
                List<Text> tooltip = getTooltip(handledScreen, client, stack);
                if (tooltip == null || tooltip.isEmpty()) {
                    continue;
                }
                
                // Parse Tooltip nach Format: "Name\nAnzahl 10000\n\nFür Details klicken"
                // Format: Zeile 0 = Name, Zeile 1 = "Anzahl" + " " + Zahl, Zeile 2 = leer, Zeile 3 = "Für Details klicken"
                String itemName = null;
                long amount = 0;
                
                // Durchsuche alle Tooltip-Zeilen
                for (int i = 0; i < tooltip.size(); i++) {
                    String lineText = tooltip.get(i).getString();
                    
                    // Erste Zeile ist der Name
                    if (i == 0) {
                        itemName = lineText.trim();
                        continue;
                    }
                    
                    // Suche nach "Anzahl" + Zahl in jeder Zeile
                    Matcher matcher = AMOUNT_PATTERN.matcher(lineText);
                    if (matcher.find()) {
                        try {
                            // Entferne Tausendertrennzeichen (Kommas)
                            String amountStr = matcher.group(1).replace(",", "");
                            amount = Long.parseLong(amountStr);
                            break; // Gefunden, keine weiteren Zeilen prüfen
                        } catch (NumberFormatException e) {
                            // Fehler beim Parsen - ignoriere und suche weiter
                        }
                    }
                }
                
                // Wenn wir Name und Anzahl haben, speichere es
                if (itemName != null && !itemName.isEmpty() && amount > 0) {
                    // Prüfe ob es ein Amboss- oder Ressource-Item ist
                    // Wir müssen prüfen, ob dieses Item in den Clipboard-Einträgen als Amboss oder Ressource verwendet wird
                    if (isAmbossItem(itemName)) {
                        ambossItems.put(itemName, amount);
                    } else if (isRessourceItem(itemName)) {
                        ressourceItems.put(itemName, amount);
                    }
                }
            }
        } catch (Exception e) {
            // Fehler beim Zugriff auf Slots - ignoriere
        }
    }
    
    /**
     * Holt den Tooltip eines Items über Reflection
     */
    private static List<Text> getTooltip(HandledScreen<?> handledScreen, MinecraftClient client, ItemStack stack) {
        try {
            java.lang.reflect.Method[] methods = HandledScreen.class.getDeclaredMethods();
            for (java.lang.reflect.Method method : methods) {
                if (!"getTooltipFromItem".equals(method.getName())) {
                    continue;
                }
                
                Class<?>[] params = method.getParameterTypes();
                method.setAccessible(true);
                
                try {
                    // Signatur: getTooltipFromItem(MinecraftClient, ItemStack)
                    if (params.length == 2 &&
                        net.minecraft.client.MinecraftClient.class.isAssignableFrom(params[0]) &&
                        ItemStack.class.isAssignableFrom(params[1])) {
                        @SuppressWarnings("unchecked")
                        List<Text> result = (List<Text>) method.invoke(handledScreen, client, stack);
                        return result;
                    }
                    
                    // Signatur: getTooltipFromItem(ItemStack)
                    if (params.length == 1 &&
                        ItemStack.class.isAssignableFrom(params[0])) {
                        @SuppressWarnings("unchecked")
                        List<Text> result = (List<Text>) method.invoke(handledScreen, stack);
                        return result;
                    }
                } catch (Exception e) {
                    // Versuche nächste Signatur
                    continue;
                }
            }
        } catch (Exception e) {
            // Fehler beim Suchen der Methode - ignoriere
        }
        return null;
    }
    
    /**
     * Prüft ob ein Item ein Amboss-Item ist (basierend auf den Clipboard-Einträgen)
     */
    private static boolean isAmbossItem(String itemName) {
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        if (entries == null) {
            return false;
        }
        
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (entry.price != null && entry.price.Amboss != null && 
                entry.price.Amboss.itemName != null && entry.price.Amboss.itemName.equals(itemName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Prüft ob ein Item ein Ressource-Item ist (basierend auf den Clipboard-Einträgen)
     */
    private static boolean isRessourceItem(String itemName) {
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        if (entries == null) {
            return false;
        }
        
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (entry.price != null && entry.price.Ressource != null && 
                entry.price.Ressource.itemName != null && entry.price.Ressource.itemName.equals(itemName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Prüft ob Amboss/Ressource gesammelt werden sollen
     * Nur wenn Clipboard aktiviert ist und Baupläne vorhanden sind
     */
    private static boolean shouldCollectAmbossRessource() {
        // Prüfe ob Clipboard aktiviert ist
        boolean clipboardEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled;
        boolean showClipboard = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showClipboard;
        
        if (!clipboardEnabled || !showClipboard) {
            return false;
        }
        
        // Prüfe ob Baupläne vorhanden sind
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        return entries != null && !entries.isEmpty();
    }
    
    /**
     * Gibt die aktuelle Anzahl eines Amboss-Items zurück
     */
    public static long getAmbossAmount(String itemName) {
        return ambossItems.getOrDefault(itemName, 0L);
    }
    
    /**
     * Gibt die aktuelle Anzahl eines Ressource-Items zurück
     */
    public static long getRessourceAmount(String itemName) {
        return ressourceItems.getOrDefault(itemName, 0L);
    }
}

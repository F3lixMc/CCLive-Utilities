package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.felix.utilities.Overall.InformationenUtility;

import java.util.ArrayList;
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
    private static final Pattern SCHMIED_PATTERN =
        Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s+(.+)", Pattern.CASE_INSENSITIVE);
    
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
        boolean isRessourceBag = net.felix.utilities.Overall.ZeichenUtility.containsUiRessourceBag(screenTitle);
        boolean isSchmiedInventory = isSchmiedInventory(handledScreen);
        
        if (!isRessourceBag && !isSchmiedInventory) {
            // Nicht im Ressourcen-Bag oder Schmied - Cache zurücksetzen
            lastScreenTitle = "";
            lastSlotCount = 0;
            return;
        }
        
        // Prüfe ob sich das Inventar geändert hat (neue Seite oder neu geladen)
        int currentSlotCount = handledScreen.getScreenHandler().slots.size();
        boolean inventoryChanged = !screenTitle.equals(lastScreenTitle) || currentSlotCount != lastSlotCount;
        
        if (inventoryChanged) {
            // Inventar hat sich geändert - aktualisiere Cache, aber lösche NICHT die Werte
            // Die Werte bleiben erhalten, auch wenn wir die Seite wechseln
            lastScreenTitle = screenTitle;
            lastSlotCount = currentSlotCount;
        }
        
        // Sammle Items aus allen Slots
        // WICHTIG: Wir überschreiben nur die Werte, die wir auf der aktuellen Seite sehen
        // Werte, die nicht auf der aktuellen Seite sind, bleiben erhalten
        try {
            List<Slot> slots = handledScreen.getScreenHandler().slots;
            int maxIndex = Math.min(53, slots.size() - 1);
            for (int i = 0; i <= maxIndex; i++) {
                Slot slot = slots.get(i);
                ItemStack stack = slot.getStack();
                if (stack.isEmpty()) {
                    continue;
                }
                
                // Hole Tooltip über Data Component API (wie in ItemInfoUtility)
                List<Text> tooltip = getTooltip(handledScreen, client, stack);
                if (tooltip == null || tooltip.isEmpty()) {
                    continue;
                }
                
                if (isRessourceBag) {
                    // Parse Tooltip nach Format: "Name\nAnzahl 10000\n\nFür Details klicken"
                    String itemName = null;
                    long amount = 0;
                    boolean amountFound = false;
                    
                    // Durchsuche alle Tooltip-Zeilen
                    for (int j = 0; j < tooltip.size(); j++) {
                        String lineText = tooltip.get(j).getString();
                        String cleanLineText = cleanTooltipLine(lineText);
                        
                        // Erste Zeile ist der Name
                        if (j == 0) {
                            itemName = cleanLineText;
                            continue;
                        }
                        
                        // Suche nach "Anzahl" + Zahl in jeder Zeile
                        Matcher matcher = AMOUNT_PATTERN.matcher(cleanLineText);
                        if (matcher.find()) {
                            amount = parseAmount(matcher.group(1));
                            amountFound = true;
                            break; // Gefunden, keine weiteren Zeilen prüfen
                        }
                    }
                    
                    // Wenn wir Name und Anzahl haben, speichere es
                    if (itemName != null && !itemName.isEmpty() && amountFound) {
                        String cleanItemName = cleanTooltipLine(itemName);
                        if (!isCoinsName(cleanItemName)) {
                            updateResourceFromBag(cleanItemName, amount);
                        }
                    }
                } else if (isSchmiedInventory) {
                    for (Text line : tooltip) {
                        String cleanLineText = cleanTooltipLine(line.getString());
                        Matcher matcher = SCHMIED_PATTERN.matcher(cleanLineText);
                        if (matcher.find()) {
                            String name = cleanTooltipLine(matcher.group(3));
                            long amount = parseAmount(matcher.group(1));
                            if (!isAincraftMaterial(name) && !isCoinsName(name)) {
                                updateResourceAmounts(name, amount);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fehler beim Zugriff auf Slots - ignoriere
        }
    }
    
    /**
     * Holt den Tooltip eines Items über Data Component API (wie in ItemInfoUtility)
     */
    private static List<Text> getTooltip(HandledScreen<?> handledScreen, MinecraftClient client, ItemStack stack) {
        List<Text> tooltip = new ArrayList<>();
        try {
            // Add item name
            tooltip.add(stack.getName());
            
            // Read lore from Data Component API (1.21.7) - wie in ItemInfoUtility
            var loreComponent = stack.get(net.minecraft.component.DataComponentTypes.LORE);
            if (loreComponent != null) {
                tooltip.addAll(loreComponent.lines());
            }
        } catch (Exception e) {
            // Silent error handling
        }
        return tooltip.isEmpty() ? null : tooltip;
    }
    
    private static void updateResourceAmounts(String itemName, long amount) {
        if (itemName == null || itemName.isEmpty()) {
            return;
        }
        String cleanItemName = cleanTooltipLine(itemName);
        if (cleanItemName.isEmpty()) {
            return;
        }
        boolean isAmboss = isAmbossItem(cleanItemName);
        boolean isRessource = isRessourceItem(cleanItemName);
        
        if (!isAmboss && !isRessource) {
            return;
        }
        
        if (isAmboss) {
            ambossItems.put(cleanItemName, amount);
        }
        if (isRessource) {
            ressourceItems.put(cleanItemName, amount);
        }
        
        Map<String, Long> updates = new HashMap<>();
        updates.put(cleanItemName, amount);
        CollectedMaterialsResourcesStorage.updateResources(updates);
    }
    
    private static void updateResourceFromBag(String itemName, long amount) {
        if (itemName == null || itemName.isEmpty()) {
            return;
        }
        String cleanItemName = cleanTooltipLine(itemName);
        if (cleanItemName.isEmpty()) {
            return;
        }
        // Ressourcen-Bag enthält Ressourcen; speichere direkt ohne Clipboard-Filter
        ressourceItems.put(cleanItemName, amount);
        CollectedMaterialsResourcesStorage.updateResource(cleanItemName, amount);
    }
    
    private static String cleanTooltipLine(String lineText) {
        if (lineText == null) {
            return "";
        }
        return lineText.replaceAll("§[0-9a-fk-or]", "")
                       .replaceAll("[\\u3400-\\u4DBF]", "")
                       .trim();
    }
    
    private static long parseAmount(String amountText) {
        if (amountText == null) {
            return 0L;
        }
        String cleaned = amountText.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return 0L;
        }
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
    
    private static boolean isAincraftMaterial(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return false;
        }
        return InformationenUtility.getMaterialFloorInfo(itemName) != null;
    }
    
    private static boolean isCoinsName(String name) {
        return name != null && "coins".equalsIgnoreCase(name.trim());
    }
    
    private static boolean isSchmiedInventory(HandledScreen<?> handledScreen) {
        String title = handledScreen.getTitle() != null ? handledScreen.getTitle().getString() : "";
        String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "")
                                 .replaceAll("[\\u3400-\\u4DBF]", "");
        
        return cleanTitle.contains("Baupläne [Waffen]") ||
               cleanTitle.contains("Baupläne [Rüstung]") ||
               cleanTitle.contains("Baupläne [Werkzeuge]") ||
               cleanTitle.contains("Favorisierte [Waffenbaupläne]") ||
               cleanTitle.contains("Favorisierte [Rüstungsbaupläne]") ||
               cleanTitle.contains("Favorisierte [Werkzeugbaupläne]") ||
               cleanTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools") ||
               cleanTitle.contains("Bauplan [Shop]");
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
    
    public static void resetCollectedResources() {
        ambossItems.clear();
        ressourceItems.clear();
    }
}

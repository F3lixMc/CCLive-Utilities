package net.felix.utilities.Other.Clipboard;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.utilities.Aincraft.ItemInfoUtility;
import net.felix.utilities.Overall.InformationenUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sammelt Ressourcen und Essenzen aus Inventaren mit dem Zeichen essence_bag_ui (Slots 10–43)
 * sowie Amboss-/Ressourcen-Anzahlen aus Schmiede-, Modul- und Ingenieur-Inventaren.
 * Ressource: Name, darunter „Anzahl XXX“. Essenz: „NAME [Essenz]“, darunter „Tier X - XXX“.
 */
public class ClipboardAmbossRessourceCollector {
    private static final Map<String, Long> ambossItems = new HashMap<>(); // ItemName -> Anzahl
    private static final Map<String, Long> ressourceItems = new HashMap<>(); // ItemName -> Anzahl
    
    // Pattern für die Anzahl-Zeile im Tooltip: "Anzahl" + Leerzeichen + Zahl (z.B. "Anzahl 10000", "Anzahl 1,000")
    // Format: "Anzahl" (gold) + " " + Zahl (white)
    // Matcht: eine oder mehrere Ziffern, gefolgt von optionalen Komma-Gruppen (jeweils 3 Ziffern)
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "Anzahl\\s+(\\d{1,3}(?:[.,]\\d{3})*|\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ESSENCE_TIER_PATTERN = Pattern.compile(
            "Tier\\s+(\\d+)\\s*-\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)", Pattern.CASE_INSENSITIVE);
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
     * Läuft wenn ein passendes Inventar geöffnet ist (unabhängig vom Clipboard-Overlay):
     * - Inventar geöffnet
     * - Essenz-Tasche (Zeichen essence_bag_ui): Ressourcen und Essenzen in den Slots 10–43
     */
    private static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        
        // Prüfe ob ein Inventar geöffnet ist
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> handledScreen)) {
            // Inventar geschlossen - Cache zurücksetzen
            lastScreenTitle = "";
            lastSlotCount = 0;
            return;
        }
        
        net.minecraft.network.chat.Component titleText = handledScreen.getTitle();
        String screenTitle = titleText != null ? titleText.getString() : "";
        boolean isEssenceBag = net.felix.utilities.Overall.ZeichenUtility.containsEssenceBagUi(screenTitle);
        boolean isCostInventory = ClipboardCostInventoryUtility.usesSchmiedCostTooltipFormat(handledScreen);
        boolean isFishingCraft = net.felix.utilities.Overall.ZeichenUtility.isFishingComponentsCraftTitle(screenTitle);
        
        if (!isEssenceBag && !isCostInventory && !isFishingCraft) {
            lastScreenTitle = "";
            lastSlotCount = 0;
            return;
        }
        
        // Prüfe ob sich das Inventar geändert hat (neue Seite oder neu geladen)
        int currentSlotCount = handledScreen.getMenu().slots.size();
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
            List<Slot> slots = handledScreen.getMenu().slots;
            int minIndex = isEssenceBag ? 10 : (isFishingCraft ? 9 : 0);
            int maxIndex = isEssenceBag
                    ? Math.min(43, slots.size() - 1)
                    : (isFishingCraft ? Math.min(44, slots.size() - 1) : Math.min(53, slots.size() - 1));
            for (int i = minIndex; i <= maxIndex; i++) {
                Slot slot = slots.get(i);
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) {
                    continue;
                }
                
                // Hole Tooltip über Data Component API (wie in ItemInfoUtility)
                List<Component> tooltip = getTooltip(handledScreen, client, stack);
                if (tooltip == null || tooltip.isEmpty()) {
                    continue;
                }
                
                if (isEssenceBag) {
                    readEssenceBagStack(tooltip);
                } else if (isCostInventory || isFishingCraft) {
                    for (Component line : tooltip) {
                        String cleanLineText = cleanTooltipLine(line.getString());
                        Matcher matcher = SCHMIED_PATTERN.matcher(cleanLineText);
                        if (matcher.find()) {
                            String name = cleanTooltipLine(matcher.group(3));
                            long amount = parseAmount(matcher.group(1));
                            if (!isAincraftMaterial(name) && !ItemInfoUtility.isFishingRarityMaterial(name)
                                    && !isCoinsName(name)
                                    && !ClipboardCostTooltipParser.shouldIgnoreCostName(name)
                                    && !(isFishingCraft && isNonMaterialCost(name))) {
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
     * Essenz-Tasche, Slots 10–43.
     * Ressource: Zeile 1 Name, danach „Anzahl XXX“.
     * Essenz: Zeile 1 „NAME [Essenz]“, danach „Tier X - XXX“.
     */
    private static void readEssenceBagStack(List<Component> tooltip) {
        String itemName = cleanTooltipLine(tooltip.get(0).getString());
        if (itemName.isEmpty() || isCoinsName(itemName)) {
            return;
        }
        if (itemName.contains("[Essenz]")) {
            for (int j = 1; j < tooltip.size(); j++) {
                Matcher matcher = ESSENCE_TIER_PATTERN.matcher(cleanTooltipLine(tooltip.get(j).getString()));
                if (!matcher.find()) {
                    continue;
                }
                String storedName = itemName;
                String tierLabel = "Tier " + matcher.group(1);
                if (!storedName.toLowerCase(java.util.Locale.ROOT).contains(tierLabel.toLowerCase(java.util.Locale.ROOT))) {
                    storedName = storedName + " " + tierLabel;
                }
                updateEssenceFromBag(storedName, parseAmount(matcher.group(2)));
                return;
            }
            return;
        }
        for (int j = 1; j < tooltip.size(); j++) {
            Matcher matcher = AMOUNT_PATTERN.matcher(cleanTooltipLine(tooltip.get(j).getString()));
            if (matcher.find()) {
                updateResourceFromBag(itemName, parseAmount(matcher.group(1)));
                return;
            }
        }
    }

    /**
     * Holt den Tooltip eines Items über Data Component API (wie in ItemInfoUtility)
     */
    private static List<Component> getTooltip(AbstractContainerScreen<?> handledScreen, Minecraft client, ItemStack stack) {
        List<Component> tooltip = new ArrayList<>();
        try {
            // Add item name
            tooltip.add(stack.getHoverName());
            
            // Read lore from Data Component API (1.21.7) - wie in ItemInfoUtility
            var loreComponent = stack.get(net.minecraft.core.component.DataComponents.LORE);
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

    private static void updateEssenceFromBag(String itemName, long amount) {
        if (itemName == null || itemName.isEmpty()) {
            return;
        }
        String cleanItemName = cleanTooltipLine(itemName);
        if (cleanItemName.isEmpty()) {
            return;
        }
        CollectedMaterialsResourcesStorage.updateEssence(cleanItemName, amount);
    }
    
    private static String cleanTooltipLine(String lineText) {
        if (lineText == null) {
            return "";
        }
        return lineText.replaceAll("§#[0-9A-Fa-f]{6}", "")
                       .replaceAll("§[0-9a-fk-or]", "")
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

    private static boolean isNonMaterialCost(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return "Kaktus".equalsIgnoreCase(trimmed) || "Seelen".equalsIgnoreCase(trimmed);
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

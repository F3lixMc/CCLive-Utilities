package net.felix.utilities.Other.Clipboard;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.utilities.Aincraft.ItemInfoUtility;
import net.felix.utilities.Overall.InformationenUtility;
import net.felix.utilities.Overall.ZeichenUtility;
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
 * Sammelt Fisch-Materialien aus ui_ressource_bag (Slots 10–34) und Mob-Materialien
 * aus dem Inventar „Mob-Materialien“ (Slots 1–8, 10–17, 19–26, 28–35, 37–44, 46–53).
 * Tooltip: Name, darunter „MATERIALNAME (ANZAHL)“.
 * Schmiede / Angel-Craft bleiben „AKTUELLE / BENÖTIGTE MATERIALNAME“.
 */
public class ClipboardMaterialCollector {
    private static final Pattern MATERIAL_BAG_PATTERN =
        Pattern.compile("(.+?)\\s*\\(\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*\\)?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_AMOUNT_PATTERN =
        Pattern.compile("^(.+?)\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCHMIED_PATTERN =
        Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s+(.+)", Pattern.CASE_INSENSITIVE);
    
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(ClipboardMaterialCollector::onClientTick);
    }
    
    private static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> handledScreen)) {
            return;
        }
        
        String title = handledScreen.getTitle() != null ? handledScreen.getTitle().getString() : "";
        boolean isFishBag = ZeichenUtility.containsUiRessourceBag(title)
                || ZeichenUtility.containsUiMaterialBag(title);
        boolean isMobMaterials = title.contains("Mob-Materialien");
        boolean isCostInventory = ClipboardCostInventoryUtility.usesSchmiedCostTooltipFormat(handledScreen);
        boolean isFishingCraft = ZeichenUtility.isFishingComponentsCraftTitle(title);
        
        if (!isFishBag && !isMobMaterials && !isCostInventory && !isFishingCraft
                && !ZeichenUtility.containsEssenceBagUi(title)
                && looksLikeFishMaterialBag(handledScreen, title)) {
            isFishBag = true;
        }
        
        if (!isFishBag && !isMobMaterials && !isCostInventory && !isFishingCraft) {
            return;
        }
        
        Map<String, Long> collectedMaterials = new HashMap<>();
        Map<String, Long> collectedResources = new HashMap<>();
        try {
            List<Slot> slots = handledScreen.getMenu().slots;
            int minIndex = isFishingCraft && !isFishBag && !isMobMaterials ? 9 : 0;
            int maxIndex = isFishingCraft && !isFishBag && !isMobMaterials
                    ? Math.min(44, slots.size() - 1)
                    : Math.min(53, slots.size() - 1);
            for (int i = minIndex; i <= maxIndex; i++) {
                if ((isFishBag || isMobMaterials) && !isMaterialBagSlot(i, isFishBag, isMobMaterials)) {
                    continue;
                }
                Slot slot = slots.get(i);
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) {
                    continue;
                }
                
                List<Component> tooltip = getTooltip(stack);
                if (tooltip == null || tooltip.isEmpty()) {
                    continue;
                }
                
                if (isFishBag || isMobMaterials) {
                    readMaterialBagStack(tooltip, collectedMaterials);
                } else if (isCostInventory || isFishingCraft) {
                    for (Component line : tooltip) {
                        String cleanLine = cleanTooltipLine(line.getString());
                        if (cleanLine.isEmpty()) {
                            continue;
                        }
                        Matcher matcher = SCHMIED_PATTERN.matcher(cleanLine);
                        if (matcher.find()) {
                            String name = cleanMaterialName(matcher.group(3));
                            long amount = parseAmount(matcher.group(1));
                            if (!name.isEmpty() && !isCoinsName(name)
                                    && !ClipboardCostTooltipParser.shouldIgnoreCostName(name)
                                    && !(isFishingCraft && isNonMaterialCost(name))) {
                                if (isClipboardMaterial(name)) {
                                    collectedMaterials.put(name, amount);
                                } else {
                                    collectedResources.put(name, amount);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
        
        if (!collectedMaterials.isEmpty()) {
            CollectedMaterialsResourcesStorage.updateMaterials(collectedMaterials);
        }
        if (!collectedResources.isEmpty()) {
            CollectedMaterialsResourcesStorage.updateResources(collectedResources);
        }
    }

    private static String lastCheckedTitle = null;
    private static long lastContentCheckTime = 0L;
    private static boolean lastContentCheckResult = false;

    /**
     * Fallback, falls das Titelzeichen nicht passt: Fisch-Tasche erkennen, wenn die Slots 10–34
     * Items mit „Seltenheit Name (Anzahl)“ im Tooltip haben.
     */
    private static boolean looksLikeFishMaterialBag(AbstractContainerScreen<?> screen, String title) {
        long now = System.currentTimeMillis();
        if (title.equals(lastCheckedTitle) && now - lastContentCheckTime < 500L) {
            return lastContentCheckResult;
        }
        lastCheckedTitle = title;
        lastContentCheckTime = now;
        lastContentCheckResult = false;
        try {
            List<Slot> slots = screen.getMenu().slots;
            int last = Math.min(34, slots.size() - 1);
            for (int i = 10; i <= last; i++) {
                ItemStack stack = slots.get(i).getItem();
                if (stack.isEmpty()) {
                    continue;
                }
                List<Component> tooltip = getTooltip(stack);
                if (tooltip == null) {
                    continue;
                }
                for (int line = 1; line < tooltip.size(); line++) {
                    Matcher matcher = MATERIAL_BAG_PATTERN.matcher(prepareMaterialLine(tooltip.get(line).getString()));
                    if (matcher.find() && ItemInfoUtility.isFishingRarityMaterial(cleanMaterialName(matcher.group(1)))) {
                        lastContentCheckResult = true;
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
        return false;
    }

    
    /** Fisch-Tasche: 10–34. Mob-Materialien: jede Reihe ohne die erste Spalte, bis Slot 53. */
    private static boolean isMaterialBagSlot(int slot, boolean fishBag, boolean mobMaterials) {
        if (mobMaterials) {
            return slot >= 1 && slot <= 53 && slot % 9 != 0;
        }
        return fishBag && slot >= 10 && slot <= 34;
    }

    /**
     * Zeile 1 ist der Itemname (z. B. „Heilbutt“).
     * Darunter: „Gewöhnliche Heilbuttflosse (12)“, „Seltener Heilbuttkopf (3)“.
     * Die Anzahl kann aus der Aincraft-Ziffernschrift stammen.
     */
    private static void readMaterialBagStack(List<Component> tooltip, Map<String, Long> collectedMaterials) {
        for (int lineIndex = 1; lineIndex < tooltip.size(); lineIndex++) {
            String cleanLine = prepareMaterialLine(tooltip.get(lineIndex).getString());
            if (cleanLine.isEmpty()) {
                continue;
            }
            Matcher matcher = MATERIAL_BAG_PATTERN.matcher(cleanLine);
            if (!matcher.find()) {
                matcher = TRAILING_AMOUNT_PATTERN.matcher(cleanLine);
                if (!matcher.matches() || !isClipboardMaterial(cleanMaterialName(matcher.group(1)))) {
                    continue;
                }
            }
            String name = cleanMaterialName(matcher.group(1));
            if (name.isEmpty() || isCoinsName(name)) {
                continue;
            }
            collectedMaterials.put(name, parseAmount(matcher.group(2)));
        }
    }

    /** Ziffernschrift zuerst in normale Zahlen umwandeln, danach Pixel-Abstand und Farben entfernen. */
    private static String prepareMaterialLine(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String decoded = ZeichenUtility.decodeTabWidgetText(raw);
        return stripFormatting(decoded)
                .replace('（', '(')
                .replace('）', ')');
    }

    private static List<Component> getTooltip(ItemStack stack) {
        List<Component> tooltip = new ArrayList<>();
        try {
            tooltip.add(stack.getHoverName());
            var loreComponent = stack.get(net.minecraft.core.component.DataComponents.LORE);
            if (loreComponent != null) {
                tooltip.addAll(loreComponent.lines());
            }
        } catch (Exception e) {
            // Silent error handling
        }
        return tooltip.isEmpty() ? null : tooltip;
    }
    
    private static String cleanTooltipLine(String lineText) {
        if (lineText == null) {
            return "";
        }
        return stripFormatting(lineText);
    }
    
    private static String cleanMaterialName(String name) {
        return stripFormatting(name);
    }

    private static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("§#[0-9A-Fa-f]{6}", "")
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
    
    private static boolean isAincraftMaterial(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return false;
        }
        return InformationenUtility.getMaterialFloorInfo(itemName) != null;
    }

    private static boolean isClipboardMaterial(String itemName) {
        return isAincraftMaterial(itemName) || ItemInfoUtility.isFishingRarityMaterial(itemName);
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
}

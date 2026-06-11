package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.felix.utilities.Aincraft.ItemInfoUtility;
import net.felix.utilities.Overall.InformationenUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sammelt Materialien aus dem Material-Bag Inventar (Symbol ⳅ), Schmiede-Inventaren
 * und dem Angel-Komponenten-Craft-Menü (ui_components_craft).
 * Material Bag Format: "Materialname (Anzahl)"
 * Schmiede / Angel-Craft Format: "AKTUELLE / BENÖTIGTE MATERIALNAME"
 */
public class ClipboardMaterialCollector {
    private static final Pattern MATERIAL_BAG_PATTERN =
        Pattern.compile("(.+?)\\s*\\((\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCHMIED_PATTERN =
        Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s+(.+)", Pattern.CASE_INSENSITIVE);
    
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(ClipboardMaterialCollector::onClientTick);
    }
    
    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        if (!(client.currentScreen instanceof HandledScreen<?> handledScreen)) {
            return;
        }
        
        String title = handledScreen.getTitle() != null ? handledScreen.getTitle().getString() : "";
        boolean isMaterialBag = net.felix.utilities.Overall.ZeichenUtility.containsUiMaterialBag(title);
        boolean isSchmiedInventory = isSchmiedInventory(handledScreen);
        boolean isFishingCraft = net.felix.utilities.Overall.ZeichenUtility.isFishingComponentsCraftTitle(title);
        
        if (!isMaterialBag && !isSchmiedInventory && !isFishingCraft) {
            return;
        }
        
        Map<String, Long> collectedMaterials = new HashMap<>();
        Map<String, Long> collectedResources = new HashMap<>();
        try {
            List<Slot> slots = handledScreen.getScreenHandler().slots;
            int minIndex = isFishingCraft ? 9 : 0;
            int maxIndex = isFishingCraft ? Math.min(44, slots.size() - 1) : Math.min(53, slots.size() - 1);
            for (int i = minIndex; i <= maxIndex; i++) {
                Slot slot = slots.get(i);
                ItemStack stack = slot.getStack();
                if (stack.isEmpty()) {
                    continue;
                }
                
                List<Text> tooltip = getTooltip(stack);
                if (tooltip == null || tooltip.isEmpty()) {
                    continue;
                }
                
                for (Text line : tooltip) {
                    String cleanLine = cleanTooltipLine(line.getString());
                    if (cleanLine.isEmpty()) {
                        continue;
                    }
                    
                    if (isMaterialBag) {
                        Matcher matcher = MATERIAL_BAG_PATTERN.matcher(cleanLine);
                        if (matcher.find()) {
                            String name = cleanMaterialName(matcher.group(1));
                            long amount = parseAmount(matcher.group(2));
                            if (!name.isEmpty() && !isCoinsName(name)) {
                                collectedMaterials.put(name, amount);
                            }
                        }
                    } else if (isSchmiedInventory || isFishingCraft) {
                        Matcher matcher = SCHMIED_PATTERN.matcher(cleanLine);
                        if (matcher.find()) {
                            String name = cleanMaterialName(matcher.group(3));
                            long amount = parseAmount(matcher.group(1));
                            if (!name.isEmpty() && !isCoinsName(name)
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
    
    private static List<Text> getTooltip(ItemStack stack) {
        List<Text> tooltip = new ArrayList<>();
        try {
            tooltip.add(stack.getName());
            var loreComponent = stack.get(net.minecraft.component.DataComponentTypes.LORE);
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
        return lineText.replaceAll("§[0-9a-fk-or]", "")
                       .replaceAll("[\\u3400-\\u4DBF]", "")
                       .trim();
    }
    
    private static String cleanMaterialName(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("§[0-9a-fk-or]", "")
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
}

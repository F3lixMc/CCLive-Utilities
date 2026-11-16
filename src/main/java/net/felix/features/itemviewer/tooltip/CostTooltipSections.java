package net.felix.features.itemviewer.tooltip;

import net.felix.features.itemviewer.data.CostItem;
import net.felix.features.itemviewer.data.ItemData;
import net.felix.features.itemviewer.data.PriceData;
import net.felix.features.itemviewer.inventory.FoundItemChecker;
import net.felix.features.itemviewer.inventory.OwnedResources;
import net.felix.features.itemviewer.ui.ViewerState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import static net.felix.features.itemviewer.tooltip.ForgingTooltipText.ITEM_SCORE_VALUE_COLOR;

/**
 * Preis-, Kosten- und Bauplan-Shop-Abschnitte des Tooltips.
 */
public final class CostTooltipSections {

    private CostTooltipSections() {
    }

    static final int COST_GREEN = 0xFF55FF55;

    /**
     * Fügt eine Kosten-Zeile zum Tooltip hinzu, falls vorhanden
     */
    static void appendPriceSection(java.util.List<Component> tooltipLines, ItemData hoveredItem, boolean leadingEmptyLine) {
        appendPriceSection(tooltipLines, hoveredItem, leadingEmptyLine, true);
    }

    static void appendPriceSection(java.util.List<Component> tooltipLines, ItemData hoveredItem,
            boolean leadingEmptyLine, boolean includeCosts) {
        if (!includeCosts || !ViewerState.isShowCosts() || hoveredItem == null || hoveredItem.price == null) {
            return;
        }
        if (leadingEmptyLine) {
            tooltipLines.add(Component.empty());
        }
        addCostSectionHeader(tooltipLines, "Kosten:", hoveredItem.price, hoveredItem);
        addCostItem(tooltipLines, hoveredItem.price.coin);
        addCostItem(tooltipLines, hoveredItem.price.cactus);
        addCostItem(tooltipLines, hoveredItem.price.soul);
        addCostItem(tooltipLines, hoveredItem.price.material1);
        addCostItem(tooltipLines, hoveredItem.price.material2);
        addCostItem(tooltipLines, hoveredItem.price.material3);
        addCostItem(tooltipLines, hoveredItem.price.material4);
        addCostItem(tooltipLines, hoveredItem.price.material5);
        addCostItem(tooltipLines, hoveredItem.price.Amboss != null ? hoveredItem.price.Amboss : hoveredItem.price.amboss);
        addCostItem(tooltipLines, hoveredItem.price.Ressource != null ? hoveredItem.price.Ressource : hoveredItem.price.ressource);
        addLevelCostItem(tooltipLines, hoveredItem.price.Level);
    }

    static void appendBlueprintShopSection(java.util.List<Component> tooltipLines, ItemData hoveredItem) {
        appendBlueprintShopSection(tooltipLines, hoveredItem, true);
    }

    static void appendBlueprintShopSection(java.util.List<Component> tooltipLines, ItemData hoveredItem,
            boolean includeCosts) {
        if (!includeCosts || !ViewerState.isShowCosts() || hoveredItem == null
                || hoveredItem.blueprint_shop == null || hoveredItem.blueprint_shop.price == null) {
            return;
        }
        tooltipLines.add(Component.empty());
        tooltipLines.add(Component.literal("Bauplan-Shop:")
                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)));
        PriceData shopPrice = hoveredItem.blueprint_shop.price;
        addCostItem(tooltipLines, shopPrice.coin);
        addCostItem(tooltipLines, shopPrice.cactus);
        addCostItem(tooltipLines, shopPrice.soul);
        addCostItem(tooltipLines, shopPrice.material1);
        addCostItem(tooltipLines, shopPrice.material2);
        addCostItem(tooltipLines, shopPrice.material3);
        addCostItem(tooltipLines, shopPrice.material4);
        addCostItem(tooltipLines, shopPrice.material5);
        addCostItem(tooltipLines, shopPrice.Amboss != null ? shopPrice.Amboss : shopPrice.amboss);
        addCostItem(tooltipLines, shopPrice.Ressource != null ? shopPrice.Ressource : shopPrice.ressource);
        addCostItem(tooltipLines, shopPrice.paper_shreds);
    }

    private static void addCostSectionHeader(java.util.List<Component> tooltipLines, String label, PriceData price, ItemData item) {
        if (!ViewerState.isShowOwnedResources() || !shouldShowCraftableCount(item)) {
            tooltipLines.add(Component.literal(label)
                    .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)));
            return;
        }
        int craftable = OwnedResources.calculateCraftableCount(price);
        if (craftable < 0) {
            tooltipLines.add(Component.literal(label)
                    .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)));
            return;
        }
        MutableComponent header = Component.literal(label)
                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false))
                .append(Component.literal("     " + craftable + "x Herstellbar")
                        .setStyle(Style.EMPTY.withColor(ITEM_SCORE_VALUE_COLOR).withItalic(false)));
        tooltipLines.add(header);
    }

    private static boolean shouldShowCraftableCount(ItemData item) {
        if (item == null) {
            return true;
        }
        if (item.category != null) {
            String category = item.category.toLowerCase(java.util.Locale.ROOT);
            if ("modules".equals(category) || "module_bags".equals(category) || "licence".equals(category)) {
                return false;
            }
        }
        if (item.info != null) {
            if (Boolean.TRUE.equals(item.info.module) || Boolean.TRUE.equals(item.info.licence)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fügt eine Kosten-Zeile zum Tooltip hinzu, falls vorhanden
     */
    private static void addCostItem(java.util.List<Component> tooltipLines, CostItem costItem) {
        if (costItem == null || costItem.itemName == null || costItem.amount == null) {
            return;
        }

        if (ViewerState.isShowOwnedResources()) {
            addCostItemWithOwned(tooltipLines, costItem);
            return;
        }

        String amountStr = formatAmount(costItem.amount);
        String materialLine = amountStr + " " + costItem.itemName;
        
        // Prüfe ob es ein Aincraft-Material ist und füge Ebene hinzu
        net.felix.features.information.materials.MaterialFloorInfo floorInfo = 
            net.felix.features.information.materials.MaterialDatabase.getMaterialFloorInfo(costItem.itemName);
        
        if (floorInfo != null) {
            MutableComponent materialText = Component.literal(materialLine)
                .setStyle(Style.EMPTY.withColor(0xFF55FF55).withItalic(false));
            
            String floorText = net.felix.features.information.materials.MaterialRarityColors.formatMaterialLocationSuffix(floorInfo);
            int rarityColor = net.felix.features.information.materials.MaterialRarityColors.getMaterialLocationRarityColorArgb(floorInfo);
            Component floorTextObj = Component.literal(floorText)
                .setStyle(Style.EMPTY.withColor(rarityColor).withItalic(false));
            
            tooltipLines.add(materialText.append(floorTextObj));
        } else {
            tooltipLines.add(Component.literal(materialLine)
                .setStyle(Style.EMPTY.withColor(0xFF55FF55).withItalic(false)));
        }
    }

    private static void addCostItemWithOwned(java.util.List<Component> tooltipLines, CostItem costItem) {
        java.math.BigDecimal owned = OwnedResources.getOwnedAmount(costItem.itemName);
        java.math.BigDecimal needed = OwnedResources.parseNeededAmount(costItem.amount);
        int lineColor;
        if (needed.signum() == 0) {
            lineColor = 0xFF00FF00;
        } else if (owned.compareTo(needed) >= 0) {
            lineColor = 0xFF00FF00;
        } else {
            lineColor = 0xFFFF5555;
        }

        String ownedStr = OwnedResources.formatOwnedAmountForDisplay(costItem.itemName);
        String neededStr = OwnedResources.formatNeededAmountForDisplay(costItem.itemName, costItem.amount);
        String materialLine = ownedStr + " / " + neededStr + " " + costItem.itemName;

        net.felix.features.information.materials.MaterialFloorInfo floorInfo =
            net.felix.features.information.materials.MaterialDatabase.getMaterialFloorInfo(costItem.itemName);
        if (floorInfo != null) {
            MutableComponent materialText = Component.literal(materialLine)
                .setStyle(Style.EMPTY.withColor(lineColor).withItalic(false));
            String floorText = net.felix.features.information.materials.MaterialRarityColors.formatMaterialLocationSuffix(floorInfo);
            int rarityColor = net.felix.features.information.materials.MaterialRarityColors.getMaterialLocationRarityColorArgb(floorInfo);
            materialText.append(Component.literal(floorText)
                .setStyle(Style.EMPTY.withColor(rarityColor).withItalic(false)));
            tooltipLines.add(materialText);
        } else {
            tooltipLines.add(Component.literal(materialLine)
                .setStyle(Style.EMPTY.withColor(lineColor).withItalic(false)));
        }
    }

    /**
     * Fügt eine Level-Kosten-Zeile zum Tooltip hinzu (Zahl in gelb)
     */
    private static void addLevelCostItem(java.util.List<Component> tooltipLines, CostItem costItem) {
        if (costItem != null && costItem.itemName != null && costItem.amount != null) {
            String amountStr = formatAmount(costItem.amount);
            
            // Erstelle Text mit Zahl in gelb und Item-Name in grün
            String amountPart = amountStr + " ";
            String namePart = costItem.itemName;
            
            MutableComponent levelText = Component.literal(amountPart)
                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)) // Gelb für Zahl
                .append(Component.literal(namePart)
                    .setStyle(Style.EMPTY.withColor(0xFF55FF55).withItalic(false))); // Grün für Name
            
            tooltipLines.add(levelText);
        }
    }

    /**
     * Formatiert einen Amount-Wert: Ganze Zahlen ohne ".0", andere bleiben wie sie sind
     */
    public static String formatAmount(Object amount) {
        if (amount == null) {
            return "0";
        }
        
        // Wenn es bereits ein String ist, gib ihn zurück
        if (amount instanceof String) {
            return (String) amount;
        }
        
        // Wenn es eine Zahl ist, prüfe ob es eine ganze Zahl ist
        if (amount instanceof Number) {
            Number num = (Number) amount;
            double doubleValue = num.doubleValue();
            
            // Prüfe ob es eine ganze Zahl ist (ohne Dezimalstellen)
            if (doubleValue == Math.floor(doubleValue)) {
                // Ganze Zahl: ohne Dezimalstellen anzeigen
                return String.valueOf(num.longValue());
            } else {
                // Zahl mit Dezimalstellen: so anzeigen wie sie ist
                return String.valueOf(doubleValue);
            }
        }
        
        // Fallback: toString()
        return amount.toString();
    }

    public static boolean isItemFound(ItemData item) {
        return FoundItemChecker.isFound(item);
    }
}

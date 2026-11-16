package net.felix.features.itemviewer.tooltip;

import net.felix.features.itemviewer.data.ItemData;
import net.felix.features.itemviewer.data.LocationData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import static net.felix.features.itemviewer.tooltip.ForgingTooltipText.buildForgingConditionText;
import static net.felix.features.itemviewer.tooltip.ForgingTooltipText.buildItemScoreText;
import static net.felix.features.itemviewer.tooltip.ForgingTooltipText.resolveForgingCondition;

/**
 * Tooltip-Zeilen zu Fundort, Item-Score und Schmiedebedingung.
 */
public final class FoundAtTooltipSection {

    private FoundAtTooltipSection() {
    }

    static void appendFoundAtLocationLines(java.util.List<Component> tooltipLines, ItemData hoveredItem) {
        appendFoundAtLocationLines(tooltipLines, hoveredItem, true);
    }

    static void appendFoundAtLocationLines(java.util.List<Component> tooltipLines, ItemData hoveredItem, boolean trailingEmptyLine) {
        boolean isBlueprint = hoveredItem.info != null && Boolean.TRUE.equals(hoveredItem.info.blueprint);
        Component itemScoreText = isBlueprint ? buildItemScoreText(hoveredItem.itemScore) : null;
        Component forgingConditionText = isBlueprint
                ? buildForgingConditionText(resolveForgingCondition(hoveredItem)) : null;
        boolean hasLocation = hoveredItem.foundAt != null && !hoveredItem.foundAt.isEmpty();
        String locationLine = null;

        if (hasLocation) {
            LocationData firstLocation = hoveredItem.foundAt.get(0);
            if (firstLocation.floor != null && !firstLocation.floor.isEmpty()) {
                locationLine = firstLocation.floor;
            } else if (firstLocation.collection != null && !firstLocation.collection.isEmpty()) {
                locationLine = firstLocation.collection;
            }
        }

        if (itemScoreText == null && forgingConditionText == null && locationLine == null) {
            return;
        }

        if (itemScoreText != null) {
            tooltipLines.add(Component.empty());
            tooltipLines.add(itemScoreText);
        }
        if (forgingConditionText != null) {
            tooltipLines.add(forgingConditionText);
        }
        if (locationLine != null) {
            tooltipLines.add(Component.literal(locationLine)
                .setStyle(Style.EMPTY.withColor(0xFFFF00FF).withItalic(false)));
        }
        if (trailingEmptyLine) {
            tooltipLines.add(Component.empty());
        }
    }
}

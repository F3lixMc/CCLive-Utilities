package net.felix.features.itemviewer.tooltip;

import net.felix.features.itemviewer.data.ItemData;
import net.felix.features.itemviewer.search.ForgingConditionCalculator;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Texte fuer Schmiedebedingung und Item-Score.
 */
public final class ForgingTooltipText {

    private ForgingTooltipText() {
    }

    /** Liest Schmiedegrad aus Anzeigenamen wie "Schwert [Drachengeschmiedet]". */
    public static String extractForgingConditionFromName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        int start = name.lastIndexOf('[');
        int end = name.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return null;
        }
        String inner = name.substring(start + 1, end).trim()
            .replaceAll("§[0-9a-fk-or]", "");
        if (inner.isEmpty()) {
            return null;
        }
        return switch (inner.toLowerCase()) {
            case "sternengeschmiedet" -> "Sternengeschmiedet";
            case "blitzgeschmiedet" -> "Blitzgeschmiedet";
            case "dämonengeschmiedet", "daemonengeschmiedet" -> "Dämonengeschmiedet";
            case "drachengeschmiedet" -> "Drachengeschmiedet";
            case "titangeschmiedet" -> "Titangeschmiedet";
            case "lavageschmiedet" -> "Lavageschmiedet";
            case "frostgeschmiedet" -> "Frostgeschmiedet";
            default -> null;
        };
    }

    static final int ITEM_SCORE_VALUE_COLOR = 0xFF54FCFC;

    public static String resolveForgingCondition(ItemData item) {
        if (item == null) {
            return null;
        }
        if (item.forgingCondition != null && !item.forgingCondition.isBlank()) {
            return item.forgingCondition;
        }
        return ForgingConditionCalculator.resolveMaxForgingCondition(item.itemScore);
    }

    static Component buildForgingConditionText(String forgingCondition) {
        if (forgingCondition == null || forgingCondition.isBlank()) {
            return null;
        }

        int conditionColor = ForgingConditionCalculator.getDisplayColorArgb(forgingCondition);
        MutableComponent text = Component.literal("[")
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false));
        text.append(Component.literal(forgingCondition)
            .setStyle(Style.EMPTY.withColor(conditionColor).withItalic(false)));
        text.append(Component.literal("] möglich")
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
        return text;
    }

    static Component buildItemScoreText(String itemScore) {
        String value = formatItemScoreValue(itemScore);
        if (value == null) {
            return null;
        }

        MutableComponent text = Component.literal("[")
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false));
        text.append(Component.literal("ItemScore")
            .setStyle(Style.EMPTY.withColor(ITEM_SCORE_VALUE_COLOR).withItalic(false)));
        text.append(Component.literal("] ")
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
        text.append(Component.literal(value)
            .setStyle(Style.EMPTY.withColor(ITEM_SCORE_VALUE_COLOR).withItalic(false)));
        return text;
    }

    private static String formatItemScoreValue(String itemScore) {
        if (itemScore == null || itemScore.isBlank()) {
            return null;
        }
        if ("NaN".equalsIgnoreCase(itemScore.trim())) {
            return "NaN";
        }
        try {
            double value = Double.parseDouble(itemScore.trim().replace(',', '.'));
            if (Double.isNaN(value)) {
                return "NaN";
            }
            if (value == Math.rint(value)) {
                return String.valueOf((long) value);
            }
            String formatted = String.valueOf(value);
            if (formatted.contains(".") && formatted.endsWith("0")) {
                formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
            }
            return formatted;
        } catch (NumberFormatException ignored) {
            return itemScore.trim();
        }
    }
}

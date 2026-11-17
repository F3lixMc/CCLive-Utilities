package net.felix.utilities.Overall.itemviewer.tooltip;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.felix.utilities.Overall.itemviewer.data.CatchChance;
import net.felix.utilities.Overall.itemviewer.data.ItemData;
import net.felix.utilities.Overall.itemviewer.data.ItemInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import static net.felix.utilities.Overall.itemviewer.tooltip.CostTooltipSections.COST_GREEN;
import static net.felix.utilities.Overall.itemviewer.tooltip.CostTooltipSections.appendPriceSection;
import static net.felix.utilities.Overall.itemviewer.tooltip.CostTooltipSections.isItemFound;
import static net.felix.utilities.Overall.itemviewer.tooltip.FoundAtTooltipSection.appendFoundAtLocationLines;
import static net.felix.utilities.Overall.itemviewer.tooltip.ItemTooltipBuilder.appendInteractionHints;

/**
 * Tooltip-Abschnitte fuer Angel-Komponenten und Fischfallen (Modifier, Fangchancen, Shiny).
 */
public final class FishingTooltipSections {

    private FishingTooltipSections() {
    }

    private static final Pattern FISHING_MODIFIER_VALUE = Pattern.compile("[+-]?[\\d.,]+(?:%|cg|lo|k|m|b)?");

    /**
     * Angel-Komponenten: weißer Text, Zahlen/Vorzeichen in [] in Kosten-Grün.
     * Beispiel: Anbeißgeschwindigkeit: [-10% - 5%]
     */
    static MutableComponent buildFishingComponentModifierText(String modifier) {
        MutableComponent text = Component.literal("⦁ ")
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false));
        
        int bracketStart = modifier.indexOf('[');
        int bracketEnd = modifier.lastIndexOf(']');
        if (bracketStart < 0 || bracketEnd <= bracketStart) {
            text.append(Component.literal(modifier)
                .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
            return text;
        }
        
        text.append(Component.literal(modifier.substring(0, bracketStart + 1))
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
        appendFishingModifierBracketContent(text, modifier.substring(bracketStart + 1, bracketEnd));
        text.append(Component.literal(modifier.substring(bracketEnd))
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
        return text;
    }

    private static void appendFishingModifierBracketContent(MutableComponent text, String inside) {
        Matcher matcher = FISHING_MODIFIER_VALUE.matcher(inside);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                text.append(Component.literal(inside.substring(lastEnd, matcher.start()))
                    .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
            }
            text.append(Component.literal(matcher.group())
                .setStyle(Style.EMPTY.withColor(COST_GREEN).withItalic(false)));
            lastEnd = matcher.end();
        }
        if (lastEnd < inside.length()) {
            text.append(Component.literal(inside.substring(lastEnd))
                .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
        }
    }

    private static final int FISH_TRAP_LABEL_GREEN = 0xFF55FF55;

    private static final int FISH_TRAP_BLUEPRINT_BLUE = 0xFF5555FF;

    private static final int[] SHINY_LETTER_COLORS = {
        0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF
    };

    static void appendFishTrapTooltipLines(java.util.List<Component> tooltipLines, ItemData hoveredItem, ItemInfo info,
            boolean includeInteractionHints, boolean includeCosts) {
        appendFoundAtLocationLines(tooltipLines, hoveredItem);

        if (info.catch_time != null && !info.catch_time.isEmpty()) {
            MutableComponent catchTime = Component.literal("Fangzeit: ")
                .setStyle(Style.EMPTY.withColor(FISH_TRAP_LABEL_GREEN).withItalic(false));
            catchTime.append(Component.literal(info.catch_time)
                .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
            tooltipLines.add(catchTime);
        }

        if (info.capacity != null) {
            MutableComponent capacity = Component.literal("Kapazität: ")
                .setStyle(Style.EMPTY.withColor(FISH_TRAP_LABEL_GREEN).withItalic(false));
            capacity.append(Component.literal(String.valueOf(info.capacity))
                .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
            tooltipLines.add(capacity);
        }

        if (info.catch_chances != null && !info.catch_chances.isEmpty()) {
            tooltipLines.add(Component.empty());
            tooltipLines.add(Component.literal("Fangchancen:")
                .setStyle(Style.EMPTY.withColor(FISH_TRAP_LABEL_GREEN).withItalic(false)));
            for (CatchChance chance : info.catch_chances) {
                if (chance == null || chance.label == null || chance.label.isEmpty()) {
                    continue;
                }
                tooltipLines.add(buildFishTrapCatchChanceLine(chance));
            }
        }

        if (info.blueprint != null && info.blueprint) {
            tooltipLines.add(Component.empty());
            boolean isFound = isItemFound(hoveredItem);
            MutableComponent statusText = Component.literal("Status:")
                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false));
            statusText.append(Component.literal(" ")
                .setStyle(Style.EMPTY.withItalic(false)));
            if (isFound) {
                statusText.append(Component.literal("[Gefunden]")
                    .setStyle(Style.EMPTY.withColor(0xFF00FF00).withItalic(false)));
            } else {
                statusText.append(Component.literal("[Nicht Gefunden]")
                    .setStyle(Style.EMPTY.withColor(0xFFFF5555).withItalic(false)));
            }
            tooltipLines.add(statusText);
        }

        appendPriceSection(tooltipLines, hoveredItem, true, includeCosts);

        if (info.prerequisites != null && !info.prerequisites.isEmpty()) {
            tooltipLines.add(Component.empty());
            tooltipLines.add(Component.literal("Voraussetzungen:")
                .setStyle(Style.EMPTY.withColor(FISH_TRAP_LABEL_GREEN).withItalic(false)));
            for (String prerequisite : info.prerequisites) {
                if (prerequisite == null || prerequisite.isEmpty()) {
                    continue;
                }
                MutableComponent prereqLine = Component.literal("  • ")
                    .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false));
                prereqLine.append(Component.literal(prerequisite)
                    .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                prereqLine.append(Component.literal(" [Bauplan]")
                    .setStyle(Style.EMPTY.withColor(FISH_TRAP_BLUEPRINT_BLUE).withItalic(false)));
                tooltipLines.add(prereqLine);
            }
        }

        if (includeInteractionHints) {
            appendInteractionHints(tooltipLines, hoveredItem, info);
        }
    }

    private static MutableComponent buildFishTrapCatchChanceLine(CatchChance chance) {
        MutableComponent line = Component.literal("  • ")
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false));
        if ("shiny".equalsIgnoreCase(chance.rarity)) {
            line.append(buildShinyRarityLabel(chance.label));
        } else {
            line.append(Component.literal(chance.label)
                .setStyle(Style.EMPTY.withColor(getFishTrapCatchRarityColor(chance.rarity)).withItalic(false)));
        }
        String percent = chance.percent != null ? chance.percent : "";
        line.append(Component.literal(": " + percent)
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
        return line;
    }

    private static MutableComponent buildShinyRarityLabel(String label) {
        MutableComponent shinyText = Component.empty();
        String text = label != null ? label : "Shiny";
        for (int i = 0; i < text.length(); i++) {
            int color = i < SHINY_LETTER_COLORS.length
                ? SHINY_LETTER_COLORS[i]
                : SHINY_LETTER_COLORS[SHINY_LETTER_COLORS.length - 1];
            shinyText.append(Component.literal(String.valueOf(text.charAt(i)))
                .setStyle(Style.EMPTY.withColor(color).withItalic(false)));
        }
        return shinyText;
    }

    private static int getFishTrapCatchRarityColor(String rarity) {
        if (rarity == null) {
            return 0xFFFFFFFF;
        }
        return switch (rarity.toLowerCase()) {
            case "uncommon" -> 0xFF55FF55;
            case "rare" -> 0xFF5555FF;
            case "epic" -> 0xFFAA00AA;
            case "legendary" -> 0xFFFFAA00;
            default -> 0xFFFFFFFF;
        };
    }
}

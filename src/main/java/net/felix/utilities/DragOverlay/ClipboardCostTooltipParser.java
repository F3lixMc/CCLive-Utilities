package net.felix.utilities.DragOverlay;

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

/** Liest Kosten-Zeilen (AKTUELL / BENÖTIGT NAME) aus Inventar-Tooltips. */
public final class ClipboardCostTooltipParser {

    private static final Pattern SCHMIED_PATTERN = Pattern.compile(
            "(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s+(.+)",
            Pattern.CASE_INSENSITIVE);

    private ClipboardCostTooltipParser() {
    }

    public record CostLine(String name, long current, long required, boolean isMaterial) {
    }

    public static List<CostLine> parseTooltip(ItemStack stack, boolean isFishingCraft) {
        List<CostLine> lines = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return lines;
        }
        List<Text> tooltip = readTooltip(stack);
        if (tooltip == null) {
            return lines;
        }
        for (Text line : tooltip) {
            CostLine parsed = parseLine(line.getString(), isFishingCraft);
            if (parsed != null) {
                lines.add(parsed);
            }
        }
        return lines;
    }

    /** Normalisierter Tooltip-Inhalt zum Erkennen von Änderungen am geklickten Item. */
    public static String buildTooltipSnapshot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        List<Text> tooltip = readTooltip(stack);
        if (tooltip == null) {
            return "";
        }
        StringBuilder snapshot = new StringBuilder();
        for (Text line : tooltip) {
            snapshot.append(cleanTooltipLine(line.getString())).append('\n');
        }
        return snapshot.toString();
    }

    public static Map<String, Long> scanInventoryCurrents(HandledScreen<?> screen, boolean isFishingCraft) {
        Map<String, Long> currents = new HashMap<>();
        if (screen == null) {
            return currents;
        }
        int minIndex = isFishingCraft ? 9 : 0;
        int maxIndex = isFishingCraft
                ? Math.min(44, screen.getScreenHandler().slots.size() - 1)
                : Math.min(53, screen.getScreenHandler().slots.size() - 1);
        try {
            List<Slot> slots = screen.getScreenHandler().slots;
            for (int i = minIndex; i <= maxIndex; i++) {
                ItemStack stack = slots.get(i).getStack();
                if (stack.isEmpty()) {
                    continue;
                }
                for (CostLine line : parseTooltip(stack, isFishingCraft)) {
                    currents.put(line.name(), line.current());
                }
            }
        } catch (Exception ignored) {
            // Silent error handling
        }
        return currents;
    }

    private static CostLine parseLine(String rawLine, boolean isFishingCraft) {
        String cleanLine = cleanTooltipLine(rawLine);
        if (cleanLine.isEmpty()) {
            return null;
        }
        Matcher matcher = SCHMIED_PATTERN.matcher(cleanLine);
        if (!matcher.find()) {
            return null;
        }
        String name = cleanMaterialName(matcher.group(3));
        if (name.isEmpty() || isCoinsName(name) || shouldIgnoreCostName(name)
                || (isFishingCraft && isNonMaterialCost(name))) {
            return null;
        }
        long current = parseAmount(matcher.group(1));
        long required = parseAmount(matcher.group(2));
        boolean isMaterial = isClipboardMaterial(name);
        return new CostLine(name, current, required, isMaterial);
    }

    private static List<Text> readTooltip(ItemStack stack) {
        List<Text> tooltip = new ArrayList<>();
        try {
            tooltip.add(stack.getName());
            var loreComponent = stack.get(net.minecraft.component.DataComponentTypes.LORE);
            if (loreComponent != null) {
                tooltip.addAll(loreComponent.lines());
            }
        } catch (Exception ignored) {
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

    private static boolean isClipboardMaterial(String itemName) {
        return InformationenUtility.getMaterialFloorInfo(itemName) != null
                || ItemInfoUtility.isFishingRarityMaterial(itemName);
    }

    public static boolean shouldIgnoreCostName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return "Gegner".equalsIgnoreCase(trimmed)
                || "Blöcke".equalsIgnoreCase(trimmed)
                || "Bloecke".equalsIgnoreCase(trimmed)
                || "Kaktus".equalsIgnoreCase(trimmed)
                || "Seelen".equalsIgnoreCase(trimmed);
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

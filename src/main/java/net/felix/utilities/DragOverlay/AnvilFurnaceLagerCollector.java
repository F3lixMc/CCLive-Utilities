package net.felix.utilities.DragOverlay;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.felix.utilities.Overall.ZeichenUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erkennt Ressourcen im Amboss- und Schmelzofen-Inventar über das Item {@code Lager}.
 * Tooltip-Format (1–2 Blöcke):
 * <pre>
 * [Ressourcenname]
 * [Anzahl / Max-Kapazität]
 * </pre>
 * Beim Linksklick werden die angezeigten Mengen direkt addiert.
 */
public final class AnvilFurnaceLagerCollector {

    private static final Pattern AMOUNT_MAX_PATTERN = Pattern.compile(
            "^(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*$");

    private static final Pattern BRACKETED_AMOUNT_MAX_PATTERN = Pattern.compile(
            "^\\[(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\]\\s*$");

    private static final Pattern BRACKETED_RESOURCE_NAME_PATTERN = Pattern.compile("^\\[([^\\]]+)]\\s*$");

    private AnvilFurnaceLagerCollector() {
    }

    /**
     * Vor dem eigentlichen Slot-Klick aufrufen (HandledScreen.mouseClicked).
     */
    public static void handleMouseClick(HandledScreen<?> screen, double mouseX, double mouseY, int button, int screenX, int screenY) {
        if (button != 0 || screen == null) {
            return;
        }

        String title = screen.getTitle() != null ? screen.getTitle().getString() : "";
        if (!ZeichenUtility.containsAnvilMainUi(title) && !ZeichenUtility.containsFurnaceMainUi(title)) {
            return;
        }

        Slot hoveredSlot = findHoveredSlot(screen, mouseX, mouseY, screenX, screenY);
        if (hoveredSlot == null || !hoveredSlot.hasStack()) {
            return;
        }

        ItemStack stack = hoveredSlot.getStack();
        if (!isLagerItem(stack)) {
            return;
        }

        Map<String, Long> resources = parseLagerResources(getTooltip(stack));
        if (!resources.isEmpty()) {
            CollectedMaterialsResourcesStorage.addResources(resources);
        }
    }

    static Map<String, Long> parseLagerResources(List<Text> tooltip) {
        Map<String, Long> resources = new HashMap<>();
        if (tooltip == null) {
            return resources;
        }

        List<String> lines = new ArrayList<>();
        for (Text line : tooltip) {
            String cleanLine = cleanTooltipLine(line.getString());
            if (cleanLine.isEmpty() || isLagerItemName(cleanLine)) {
                continue;
            }
            lines.add(cleanLine);
        }

        String pendingName = null;
        for (String line : lines) {
            Matcher bracketedAmount = BRACKETED_AMOUNT_MAX_PATTERN.matcher(line);
            if (bracketedAmount.matches()) {
                if (pendingName != null && !pendingName.isEmpty()) {
                    long amount = parseAmount(bracketedAmount.group(1));
                    if (amount > 0) {
                        resources.put(pendingName, amount);
                    }
                }
                pendingName = null;
                continue;
            }

            Matcher bracketedName = BRACKETED_RESOURCE_NAME_PATTERN.matcher(line);
            if (bracketedName.matches()) {
                pendingName = cleanTooltipLine(bracketedName.group(1));
                continue;
            }

            String unwrapped = unwrapBrackets(line);
            Matcher amountMatcher = AMOUNT_MAX_PATTERN.matcher(unwrapped);
            if (amountMatcher.matches()) {
                if (pendingName != null && !pendingName.isEmpty()) {
                    long amount = parseAmount(amountMatcher.group(1));
                    if (amount > 0) {
                        resources.put(pendingName, amount);
                    }
                }
                pendingName = null;
            } else if (!unwrapped.isEmpty()) {
                pendingName = unwrapped;
            }
        }

        return resources;
    }

    private static boolean isLagerItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return isLagerItemName(cleanTooltipLine(stack.getName().getString()));
    }

    private static boolean isLagerItemName(String name) {
        return "Lager".equalsIgnoreCase(unwrapBrackets(name));
    }

    private static String unwrapBrackets(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static Slot findHoveredSlot(HandledScreen<?> screen, double mouseX, double mouseY, int screenX, int screenY) {
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.x + screenX <= mouseX && mouseX < slot.x + screenX + 16
                    && slot.y + screenY <= mouseY && mouseY < slot.y + screenY + 16) {
                return slot;
            }
        }
        return null;
    }

    private static List<Text> getTooltip(ItemStack stack) {
        List<Text> tooltip = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return tooltip;
        }
        try {
            tooltip.add(stack.getName());
            var loreComponent = stack.get(net.minecraft.component.DataComponentTypes.LORE);
            if (loreComponent != null) {
                tooltip.addAll(loreComponent.lines());
            }
        } catch (Exception ignored) {
            // Silent error handling
        }
        return tooltip;
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
}

package net.felix.utilities.Overall.clipboard.collector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.utilities.Other.glyphs.ZeichenUtility;
import net.felix.utilities.Overall.clipboard.storage.CollectedMaterialsResourcesStorage;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Erkennt Ressourcen im Amboss- und Schmelzofen-Inventar über das Item {@code Lager}.
 * Tooltip-Format (1–2 Blöcke):
 * <pre>
 * [Ressourcenname]
 * [Anzahl / Max-Kapazität]
 * </pre>
 * Beim Linksklick wird der Stand gemerkt. Gutgeschrieben wird er erst,
 * wenn der Tooltip innerhalb von 60 Ticks auf 0 fällt.
 */
public final class AnvilFurnaceLagerCollector {

    private static final ResourcePickupGate PICKUP = new ResourcePickupGate();

    private static final Pattern AMOUNT_MAX_PATTERN = Pattern.compile(
            "^(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*$");

    private static final Pattern BRACKETED_AMOUNT_MAX_PATTERN = Pattern.compile(
            "^\\[(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\]\\s*$");

    private static final Pattern BRACKETED_RESOURCE_NAME_PATTERN = Pattern.compile("^\\[([^\\]]+)]\\s*$");

    private AnvilFurnaceLagerCollector() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(AnvilFurnaceLagerCollector::onClientTick);
    }

    /**
     * Vor dem eigentlichen Slot-Klick aufrufen (AbstractContainerScreen.mouseClicked).
     */
    public static void handleMouseClick(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button, int screenX, int screenY) {
        if (button != 0 || screen == null) {
            return;
        }

        String title = screen.getTitle() != null ? screen.getTitle().getString() : "";
        if (!ZeichenUtility.containsAnvilMainUi(title) && !ZeichenUtility.containsFurnaceMainUi(title)) {
            return;
        }

        Slot hoveredSlot = findHoveredSlot(screen, mouseX, mouseY, screenX, screenY);
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return;
        }

        ItemStack stack = hoveredSlot.getItem();
        if (!isLagerItem(stack)) {
            return;
        }

        PICKUP.arm(screenToken(title), parseLagerResources(getTooltip(stack)));
    }

    private static void onClientTick(net.minecraft.client.Minecraft client) {
        if (!PICKUP.isPending()) {
            return;
        }
        boolean connected = client != null && client.player != null;
        if (!connected || client.gui.screen() == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            PICKUP.observe(null, Map.of(), connected);
            return;
        }
        String title = screen.getTitle() != null ? screen.getTitle().getString() : "";
        String token = screenToken(title);
        ItemStack lager = findLagerStack(screen);
        Map<String, Long> current = lager == null ? Map.of() : parseLagerResources(getTooltip(lager));
        Map<String, Long> credit = PICKUP.observe(token, current, true, lager != null);
        if (!credit.isEmpty()) {
            CollectedMaterialsResourcesStorage.addResources(credit);
        }
    }

    private static String screenToken(String title) {
        if (ZeichenUtility.containsAnvilMainUi(title)) {
            return "anvil";
        }
        if (ZeichenUtility.containsFurnaceMainUi(title)) {
            return "furnace";
        }
        return "";
    }

    static Map<String, Long> parseLagerResources(List<Component> tooltip) {
        List<String> lines = new ArrayList<>();
        if (tooltip != null) {
            for (Component line : tooltip) {
                String cleanLine = cleanTooltipLine(line.getString());
                if (cleanLine.isEmpty() || isLagerItemName(cleanLine)) {
                    continue;
                }
                lines.add(cleanLine);
            }
        }
        return parseLagerLines(lines);
    }

    static Map<String, Long> parseLagerLines(List<String> lines) {
        Map<String, Long> resources = new HashMap<>();
        if (lines == null) {
            return resources;
        }

        String pendingName = null;
        for (String rawLine : lines) {
            String line = StorageLineText.withoutRate(rawLine);
            Matcher bracketedAmount = BRACKETED_AMOUNT_MAX_PATTERN.matcher(line);
            if (bracketedAmount.matches()) {
                if (pendingName != null && !pendingName.isEmpty()) {
                    resources.put(pendingName, parseAmount(bracketedAmount.group(1)));
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
                    resources.put(pendingName, parseAmount(amountMatcher.group(1)));
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
        return isLagerItemName(cleanTooltipLine(stack.getHoverName().getString()));
    }

    private static boolean isLagerItemName(String name) {
        return "Lager".equalsIgnoreCase(unwrapBrackets(name));
    }

    static ItemStack findLagerStack(AbstractContainerScreen<?> screen) {
        if (screen.getMenu() == null) {
            return null;
        }
        for (Slot slot : screen.getMenu().slots) {
            if (slot.hasItem() && isLagerItem(slot.getItem())) {
                return slot.getItem();
            }
        }
        return null;
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

    private static Slot findHoveredSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int screenX, int screenY) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot.x + screenX <= mouseX && mouseX < slot.x + screenX + 16
                    && slot.y + screenY <= mouseY && mouseY < slot.y + screenY + 16) {
                return slot;
            }
        }
        return null;
    }

    private static List<Component> getTooltip(ItemStack stack) {
        List<Component> tooltip = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return tooltip;
        }
        try {
            tooltip.add(stack.getHoverName());
            var loreComponent = stack.get(net.minecraft.core.component.DataComponents.LORE);
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

package net.felix.utilities.Factory.mklevel;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Grüner Rahmen um Machtkristalle in „Machtkristalle Verbessern“,
 * wenn XP-Fortschritt, Coins, jede Essenz und die XP-Kosten erfüllt sind.
 * Dieselbe Taste wie die Schmiedezustands-Rahmen blendet ihn aus.
 */
public final class MkUpgradeFrames {

    private static final int SLOT_SIZE = 16;
    private static final Pattern PROGRESS = Pattern.compile(
            "(?i)^.*?(\\d[\\d.,]*)\\s*/\\s*(\\d[\\d.,]*)\\s+XP\\s*\\[[^\\]]+\\]\\s*$");
    private static final Pattern COINS = Pattern.compile(
            "(?i).*Coins\\s*:\\s*\\(\\s*(\\d[\\d.,]*)\\s*/\\s*(\\d[\\d.,]*)\\s*\\).*");
    private static final Pattern ESSENCE = Pattern.compile(
            "(?i).*\\[Essenz\\].*?:\\s*\\(\\s*(\\d[\\d.,]*)\\s*/\\s*(\\d[\\d.,]*)\\s*\\).*");
    private static final Pattern XP_COST = Pattern.compile(
            "(?i).*\\bXP\\s*:\\s*\\(\\s*(\\d[\\d.,]*)\\s*/\\s*(\\d[\\d.,]*)\\s*\\).*");

    private MkUpgradeFrames() {
    }

    public static void render(GuiGraphicsExtractor context, AbstractContainerScreen<?> screen, int screenX, int screenY) {
        if (context == null || screen == null || screen.getMenu() == null || screen.getTitle() == null) {
            return;
        }
        if (!net.felix.utilities.Town.schmied.SchmiedTrackerUtility.areStateFramesVisible()) {
            return;
        }
        String title = net.felix.utilities.Aincraft.information.tooltip.TooltipTextCleaner
                .getPlainTextFromText(screen.getTitle());
        if (title == null || !title.contains("Machtkristalle Verbessern")) {
            return;
        }
        boolean background = CCLiveUtilitiesConfig.HANDLER.instance().schmiedTrackerItemDisplayMode
                == net.felix.utilities.Town.schmied.SchmiedItemDisplayMode.BACKGROUND;
        int color = CCLiveUtilitiesConfig.HANDLER.instance().titangeschmiedetColor.getRGB();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.hasItem() && costsFulfilled(tooltipOf(slot.getItem()))) {
                drawFrame(context, screenX + slot.x, screenY + slot.y, color, background);
            }
        }
    }

    /**
     * Erfüllt, wenn jede vorhandene Zielmenge erreicht ist:
     * XP-Fortschritt, Coins, jede Essenz und die XP-Kosten. Aktuell zählt ab dem Ziel.
     */
    static boolean costsFulfilled(List<Component> tooltip) {
        if (tooltip == null || tooltip.isEmpty()) {
            return false;
        }
        boolean progress = false;
        boolean coins = false;
        boolean essence = false;
        boolean xpCost = false;
        for (Component line : tooltip) {
            String text = clean(line == null ? null : line.getString());
            if (text.isEmpty()) {
                continue;
            }
            if (met(PROGRESS, text)) {
                progress = true;
                continue;
            }
            if (COINS.matcher(text).matches() || lookingAt(COINS, text)) {
                if (!met(COINS, text)) {
                    return false;
                }
                coins = true;
                continue;
            }
            if (lookingAt(ESSENCE, text)) {
                if (!met(ESSENCE, text)) {
                    return false;
                }
                essence = true;
                continue;
            }
            if (lookingAt(XP_COST, text)) {
                if (!met(XP_COST, text)) {
                    return false;
                }
                xpCost = true;
            }
        }
        return progress && coins && essence && xpCost;
    }

    private static boolean lookingAt(Pattern pattern, String text) {
        return pattern.matcher(text).find();
    }

    private static boolean met(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return false;
        }
        long current = amount(matcher.group(1));
        long target = amount(matcher.group(2));
        return current >= 0L && target >= 0L && current >= target;
    }

    private static long amount(String text) {
        String digits = text == null ? "" : text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return -1L;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private static String clean(String line) {
        if (line == null) {
            return "";
        }
        return line.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF]", "")
                .trim();
    }

    /** Dieselbe Geometrie wie die Schmiedegrad-Rahmen. */
    private static void drawFrame(GuiGraphicsExtractor context, int x, int y, int color, boolean background) {
        if (background) {
            int backgroundColor = (color & 0x00FFFFFF) | 0x80000000;
            context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, backgroundColor);
            return;
        }
        context.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y, color);
        context.fill(x - 1, y + SLOT_SIZE, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, color);
        context.fill(x - 1, y - 1, x, y + SLOT_SIZE + 1, color);
        context.fill(x + SLOT_SIZE, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, color);
    }

    private static List<Component> tooltipOf(ItemStack stack) {
        java.util.ArrayList<Component> tooltip = new java.util.ArrayList<>();
        tooltip.add(stack.getHoverName());
        var lore = stack.get(net.minecraft.core.component.DataComponents.LORE);
        if (lore != null) {
            tooltip.addAll(lore.lines());
        }
        return tooltip;
    }
}

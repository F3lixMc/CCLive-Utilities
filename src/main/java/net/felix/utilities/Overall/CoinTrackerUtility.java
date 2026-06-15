package net.felix.utilities.Overall;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.CoinTrackerDisplayMode;
import net.felix.utilities.Town.EquipmentDisplayUtility;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Liest Seelen, Coins und Kaktus aus der HUD-Bossbar per Pixel-Font-Dekodierung
 * und berechnet Session-Statistiken wie Gewinn und Coins pro Minute.
 * Im Overlay werden nur Coins und Session-Werte angezeigt.
 */
public class CoinTrackerUtility {

    private static boolean isInitialized = false;
    private static boolean isTracking = false;
    private static boolean showOverlays = true;
    private static boolean firstUpdate = true;

    private static long initialCoins = -1;
    private static long currentCoins = -1;
    private static long gainedCoins = 0;
    private static long currentSouls = -1;
    private static long currentCactus = -1;
    private static String currentSoulsDisplay = "";
    private static String currentCoinsDisplay = "";
    private static String currentCactusDisplay = "";
    private static double coinsPerMinute = 0.0;

    private static long sessionStartTime = 0;
    private static long lastHudUpdateTime = 0;
    private static String currentDimension = null;

    private static KeyBinding resetKeyBinding;

    private static final int MIN_OVERLAY_WIDTH = 120;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 5;
    private static final long HUD_TIMEOUT_MS = 10_000L;

    public static void initialize() {
        if (isInitialized) {
            return;
        }

        try {
            resetKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.cclive-utilities.coin-tracker-reset",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "category.cclive-utilities.overall"
            ));

            ClientTickEvents.END_CLIENT_TICK.register(CoinTrackerUtility::onClientTick);
            HudRenderCallback.EVENT.register((drawContext, tickDelta) -> onHudRender(drawContext, tickDelta));
            isInitialized = true;
        } catch (Exception e) {
            // Silent error handling
        }
    }

    /**
     * Wird vom BossBarMixin aufgerufen, wenn eine Bossbar mit HUD-Statistik-Struktur erkannt wird.
     */
    public static void processBossBar(String bossBarText) {
        if (!isActiveOnFloor()) {
            return;
        }
        if (!BossBarHudValueDecoder.looksLikeHudStatsBar(bossBarText)) {
            return;
        }

        try {
            BossBarHudValueDecoder.HudStats stats = BossBarHudValueDecoder.parseHudStats(bossBarText);
            if (!stats.hasCoins()) {
                return;
            }

            lastHudUpdateTime = System.currentTimeMillis();
            isTracking = true;

            if (stats.souls >= 0) {
                currentSouls = stats.souls;
            }
            if (stats.cactus >= 0) {
                currentCactus = stats.cactus;
            }
            if (stats.soulsDisplay != null && !stats.soulsDisplay.isEmpty()) {
                currentSoulsDisplay = stats.soulsDisplay;
            }
            if (stats.coinsDisplay != null && !stats.coinsDisplay.isEmpty()) {
                currentCoinsDisplay = stats.coinsDisplay;
            }
            if (stats.cactusDisplay != null && !stats.cactusDisplay.isEmpty()) {
                currentCactusDisplay = stats.cactusDisplay;
            }

            long coins = stats.coins;
            if (firstUpdate) {
                initialCoins = coins;
                currentCoins = coins;
                gainedCoins = 0;
                firstUpdate = false;
                if (sessionStartTime == 0) {
                    sessionStartTime = System.currentTimeMillis();
                }
            } else if (coins > currentCoins) {
                currentCoins = coins;
                gainedCoins = currentCoins - initialCoins;
            } else if (coins < currentCoins) {
                initialCoins = coins;
                currentCoins = coins;
                gainedCoins = 0;
                sessionStartTime = System.currentTimeMillis();
            } else {
                currentCoins = coins;
                gainedCoins = currentCoins - initialCoins;
            }

            updateCoinsPerMinute();
        } catch (Exception e) {
            // Silent error handling
        }
    }

    private static void onClientTick(MinecraftClient client) {
        if (!isEnabled() || client.player == null || client.world == null) {
            if (isTracking) {
                resetSession();
            }
            return;
        }

        if (!isActiveOnFloor()) {
            if (isTracking) {
                resetSession();
            }
            return;
        }

        checkTabKey();
        handleResetKey();
        checkDimensionChange(client);
        checkHudTimeout();
        updateCoinsPerMinute();
    }

    private static void checkTabKey() {
        if (KeyBindingUtility.isPlayerListKeyPressed()) {
            showOverlays = false;
        } else {
            showOverlays = true;
        }
    }

    private static void handleResetKey() {
        if (resetKeyBinding != null && resetKeyBinding.wasPressed()) {
            resetSession();
        }
    }

    private static void checkDimensionChange(MinecraftClient client) {
        try {
            String newDimension = client.world.getRegistryKey().getValue().toString();
            if (currentDimension != null && !currentDimension.equals(newDimension)) {
                resetSession();
            }
            currentDimension = newDimension;
        } catch (Exception e) {
            // Silent error handling
        }
    }

    private static void checkHudTimeout() {
        if (!isTracking || lastHudUpdateTime == 0) {
            return;
        }
        if (System.currentTimeMillis() - lastHudUpdateTime > HUD_TIMEOUT_MS) {
            isTracking = false;
        }
    }

    private static void updateCoinsPerMinute() {
        if (sessionStartTime == 0 || gainedCoins <= 0) {
            coinsPerMinute = 0.0;
            return;
        }
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        if (elapsed <= 0) {
            coinsPerMinute = 0.0;
            return;
        }
        double minutes = elapsed / 60000.0;
        coinsPerMinute = gainedCoins / minutes;
    }

    public static void resetSession() {
        initialCoins = -1;
        currentCoins = -1;
        gainedCoins = 0;
        currentSouls = -1;
        currentCactus = -1;
        currentSoulsDisplay = "";
        currentCoinsDisplay = "";
        currentCactusDisplay = "";
        coinsPerMinute = 0.0;
        firstUpdate = true;
        sessionStartTime = System.currentTimeMillis();
        isTracking = false;
        lastHudUpdateTime = 0;
    }

    private static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!shouldRenderOverlay() || !isTracking || currentCoins < 0) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) {
            return;
        }

        if (showOverlays && !EquipmentDisplayUtility.isEquipmentOverlayActive()) {
            renderCoinTrackerDisplay(context, client);
        }
    }

    private static void renderCoinTrackerDisplay(DrawContext context, MinecraftClient client) {
        if (client.getWindow() == null) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerX;
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerY;
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerScale;
        if (scale <= 0) {
            scale = 1.0f;
        }

        String title = "Coin Tracker";
        String coinsText = "Coins: " + formatHudValueDisplay(currentCoinsDisplay, currentCoins);
        String gainedText = "Gewinn: " + (gainedCoins >= 0 ? "+" : "") + formatAbbreviatedDisplay(gainedCoins);
        String cpmText = "CPM: " + formatAbbreviatedDisplay(Math.round(coinsPerMinute));

        String timeText = "Zeit: 00:00";
        if (sessionStartTime > 0) {
            long sessionDuration = System.currentTimeMillis() - sessionStartTime;
            long minutes = sessionDuration / 60000;
            long seconds = (sessionDuration % 60000) / 1000;
            timeText = String.format(Locale.ROOT, "Zeit: %02d:%02d", minutes, seconds);
        }

        int headerColor = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerHeaderColor.getRGB();
        int textColor = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerTextColor.getRGB();

        int maxTextWidth = client.textRenderer.getWidth(title);
        for (String line : new String[] { coinsText, gainedText, cpmText, timeText }) {
            maxTextWidth = Math.max(maxTextWidth, client.textRenderer.getWidth(line));
        }
        int overlayWidth = Math.max(MIN_OVERLAY_WIDTH, maxTextWidth + PADDING * 2);

        int baseX = screenWidth - MIN_OVERLAY_WIDTH - xOffset;
        boolean isOnLeftSide = baseX < screenWidth / 2;
        int xPosition;
        if (isOnLeftSide) {
            xPosition = screenWidth - MIN_OVERLAY_WIDTH - xOffset;
        } else {
            xPosition = screenWidth - overlayWidth - xOffset;
        }

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(xPosition, yOffset);
        matrices.scale(scale, scale);

        int overlayHeight = getCurrentOverlayHeight();
        if (CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerShowBackground) {
            context.fill(0, 0, overlayWidth, overlayHeight, 0xC0101010);
        }

        int textY = PADDING;
        context.drawText(client.textRenderer, Text.literal(title), PADDING, textY, headerColor, true);
        textY += LINE_HEIGHT;
        context.drawText(client.textRenderer, Text.literal(coinsText), PADDING, textY, textColor, true);
        textY += LINE_HEIGHT;
        context.drawText(client.textRenderer, Text.literal(gainedText), PADDING, textY, textColor, true);
        textY += LINE_HEIGHT;
        context.drawText(client.textRenderer, Text.literal(cpmText), PADDING, textY, textColor, true);
        textY += LINE_HEIGHT;
        context.drawText(client.textRenderer, Text.literal(timeText), PADDING, textY, textColor, true);

        matrices.popMatrix();
    }

    private static String formatHudValueDisplay(String hudDisplay, long fallbackValue) {
        if (hudDisplay != null && !hudDisplay.isEmpty()) {
            String normalized = BossBarHudValueDecoder.normalizeHudDisplay(hudDisplay);
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        if (fallbackValue < 0) {
            return "-";
        }
        return formatAbbreviatedDisplay(fallbackValue);
    }

    private static String formatAbbreviatedDisplay(long number) {
        if (number < 0) {
            return "-";
        }
        if (number < 1000) {
            return String.valueOf(number);
        }
        return formatAbbreviated(number);
    }

    private static String formatAbbreviated(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        }
        if (number >= 1_000_000_000_000L) {
            return formatAbbreviatedValue(number / 1_000_000_000_000.0, "T");
        }
        if (number >= 1_000_000_000L) {
            return formatAbbreviatedValue(number / 1_000_000_000.0, "B");
        }
        if (number >= 1_000_000L) {
            return formatAbbreviatedValue(number / 1_000_000.0, "M");
        }
        return formatAbbreviatedValue(number / 1_000.0, "K");
    }

    private static String formatAbbreviatedValue(double value, String suffix) {
        double rounded = Math.round(value * 10.0) / 10.0;
        if (rounded == Math.floor(rounded)) {
            return String.format(Locale.ROOT, "%.0f%s", rounded, suffix);
        }
        return String.format(Locale.ROOT, "%.1f%s", rounded, suffix);
    }

    private static final int OVERLAY_LINE_COUNT = 5;

    public static int getCurrentOverlayWidth(MinecraftClient client) {
        if (client == null || client.textRenderer == null) {
            return MIN_OVERLAY_WIDTH;
        }
        int maxTextWidth = client.textRenderer.getWidth("Coin Tracker");
        for (String line : getOverlayLines()) {
            maxTextWidth = Math.max(maxTextWidth, client.textRenderer.getWidth(line));
        }
        return Math.max(MIN_OVERLAY_WIDTH, maxTextWidth + PADDING * 2);
    }

    static String[] getOverlayLines() {
        return new String[] {
                "Coins: " + formatHudValueDisplay(currentCoinsDisplay, currentCoins),
                "Gewinn: " + (gainedCoins >= 0 ? "+" : "") + formatAbbreviatedDisplay(gainedCoins),
                "CPM: " + formatAbbreviatedDisplay(Math.round(coinsPerMinute)),
                "Zeit: 00:00"
        };
    }

    public static int getCurrentOverlayHeight() {
        return PADDING + LINE_HEIGHT * OVERLAY_LINE_COUNT + PADDING;
    }

    public static boolean isTracking() {
        return isTracking;
    }

    public static long getCurrentCoins() {
        return currentCoins;
    }

    public static long getGainedCoins() {
        return gainedCoins;
    }

    public static double getCoinsPerMinute() {
        return coinsPerMinute;
    }

    public static String formatCpmForScoreboard() {
        return formatAbbreviatedDisplay(Math.round(coinsPerMinute));
    }

    public static long getCurrentSouls() {
        return currentSouls;
    }

    public static long getCurrentCactus() {
        return currentCactus;
    }

    public static boolean isAllowedDimension() {
        return BossBarHudValueDecoder.isFloorDimension();
    }

    private static boolean isTrackingAllowed() {
        return isActiveOnFloor()
                && CCLiveUtilitiesConfig.HANDLER.instance().showCoinTracker;
    }

    private static boolean isActiveOnFloor() {
        return isEnabled() && isAllowedDimension();
    }

    private static boolean shouldRenderOverlay() {
        return isActiveOnFloor()
                && CCLiveUtilitiesConfig.HANDLER.instance().showCoinTracker
                && CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerDisplayMode == CoinTrackerDisplayMode.OVERLAY;
    }

    public static boolean usesOverlayDisplay() {
        return CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerDisplayMode == CoinTrackerDisplayMode.OVERLAY;
    }

    private static boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().enableMod
                && CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerEnabled;
    }

    public static String formatNumber(long number) {
        return String.format(Locale.GERMANY, "%,d", number);
    }
}

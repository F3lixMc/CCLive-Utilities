package net.felix.utilities.Overall;

import net.felix.utilities.Other.Clipboard.CollectedMaterialsResourcesStorage;
import net.felix.utilities.Overall.KeyCategories;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.CoinTrackerDisplayMode;
import net.felix.utilities.Town.EquipmentDisplayUtility;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private static BigDecimal initialCoins = null;
    private static BigDecimal currentCoins = null;
    private static BigDecimal gainedCoins = BigDecimal.ZERO;
    private static long currentSouls = -1;
    private static long currentCactus = -1;
    private static String currentCoinsDisplay = "";
    private static String currentSoulsDisplay = "";
    private static String currentCactusDisplay = "";
    private static double coinsPerMinute = 0.0;

    private static long sessionStartTime = 0;
    private static long lastHudUpdateTime = 0;
    private static long lastCpmUpdateTime = 0;
    private static BigDecimal lastCpmGainedCoins = null;
    private static String currentDimension = null;

    private static KeyMapping resetKeyMapping;

    private static final int MIN_OVERLAY_WIDTH = 120;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 5;
    private static final long HUD_TIMEOUT_MS = 10_000L;

    public static void initialize() {
        if (isInitialized) {
            return;
        }

        try {
            resetKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.cclive-utilities.coin-tracker-reset",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    KeyCategories.of("cclive-utilities", "overall")
            ));

            ClientTickEvents.END_CLIENT_TICK.register(CoinTrackerUtility::onClientTick);
            HudElementRegistry.addLast(
                    Identifier.fromNamespaceAndPath("cclive-utilities", "coin_tracker"),
                    CoinTrackerUtility::onHudRender);
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

            if (stats.souls != null && stats.souls.signum() >= 0) {
                currentSouls = HudNumberSuffixUtility.toLongOrMax(stats.souls);
                if (stats.soulsDisplay != null && !stats.soulsDisplay.isEmpty()) {
                    currentSoulsDisplay = BossBarHudValueDecoder.normalizeHudDisplayLowercase(stats.soulsDisplay);
                }
                CollectedMaterialsResourcesStorage.updateOtherHudValue(
                        CollectedMaterialsResourcesStorage.OTHER_SEELEN, stats.souls, currentSoulsDisplay);
            }
            if (stats.cactus != null && stats.cactus.signum() >= 0) {
                currentCactus = HudNumberSuffixUtility.toLongOrMax(stats.cactus);
                if (stats.cactusDisplay != null && !stats.cactusDisplay.isEmpty()) {
                    currentCactusDisplay = BossBarHudValueDecoder.normalizeHudDisplayLowercase(stats.cactusDisplay);
                }
                CollectedMaterialsResourcesStorage.updateOtherHudValue(
                        CollectedMaterialsResourcesStorage.OTHER_KAKTUS, stats.cactus, currentCactusDisplay);
            }
            if (stats.coinsDisplay != null && !stats.coinsDisplay.isEmpty()) {
                currentCoinsDisplay = stats.coinsDisplay;
            }

            BigDecimal coins = stats.coins;
            if (firstUpdate) {
                initialCoins = coins;
                currentCoins = coins;
                gainedCoins = BigDecimal.ZERO;
                firstUpdate = false;
                sessionStartTime = System.currentTimeMillis();
            } else if (coins.compareTo(currentCoins) > 0) {
                currentCoins = coins;
                gainedCoins = currentCoins.subtract(initialCoins);
            } else if (coins.compareTo(currentCoins) < 0) {
                initialCoins = coins;
                currentCoins = coins;
                gainedCoins = BigDecimal.ZERO;
                sessionStartTime = System.currentTimeMillis();
            } else {
                currentCoins = coins;
                gainedCoins = currentCoins.subtract(initialCoins);
            }

            updateCoinsPerMinute();
        } catch (Exception e) {
            // Silent error handling
        }
    }

    private static void onClientTick(Minecraft client) {
        if (!isEnabled() || client.player == null || client.level == null) {
            if (isTracking) {
                resetSession();
            }
            currentDimension = null;
            return;
        }

        checkDimensionChange(client);

        if (!isActiveOnFloor()) {
            if (isTracking) {
                resetSession();
            }
            return;
        }

        checkTabKey();
        handleResetKey();
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
        if (resetKeyMapping != null && resetKeyMapping.consumeClick()) {
            resetSession();
        }
    }

    private static void checkDimensionChange(Minecraft client) {
        try {
            String newDimension = client.level.dimension().identifier().toString();
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
        long now = System.currentTimeMillis();
        boolean coinsChanged = lastCpmGainedCoins == null || gainedCoins.compareTo(lastCpmGainedCoins) != 0;
        if (!SessionRateUtility.shouldRecalculate(lastCpmUpdateTime, coinsChanged)) {
            return;
        }

        lastCpmUpdateTime = now;
        lastCpmGainedCoins = gainedCoins;

        if (sessionStartTime == 0 || gainedCoins.signum() <= 0) {
            coinsPerMinute = 0.0;
            return;
        }
        long elapsed = now - sessionStartTime;
        if (elapsed <= 0) {
            coinsPerMinute = 0.0;
            return;
        }
        double minutes = elapsed / 60000.0;
        coinsPerMinute = gainedCoins.divide(BigDecimal.valueOf(minutes), 2, RoundingMode.HALF_UP).doubleValue();
    }

    public static void resetSession() {
        initialCoins = null;
        currentCoins = null;
        gainedCoins = BigDecimal.ZERO;
        currentSouls = -1;
        currentCactus = -1;
        currentCoinsDisplay = "";
        currentSoulsDisplay = "";
        currentCactusDisplay = "";
        coinsPerMinute = 0.0;
        firstUpdate = true;
        sessionStartTime = 0;
        isTracking = false;
        lastHudUpdateTime = 0;
        lastCpmUpdateTime = 0;
        lastCpmGainedCoins = null;
    }

    public static void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        if (!shouldRenderOverlay() || !isTracking || currentCoins == null) {
            return;
        }
        if (!HudOverlayVisibility.shouldRenderWorldHudOverlays()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.options.hideGui) {
            return;
        }

        if (showOverlays && !EquipmentDisplayUtility.isEquipmentOverlayActive()) {
            renderCoinTrackerDisplay(context, client);
        }
    }

    private static void renderCoinTrackerDisplay(GuiGraphicsExtractor context, Minecraft client) {
        if (client.getWindow() == null) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerX;
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerY;
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerScale;
        if (scale <= 0) {
            scale = 1.0f;
        }

        String title = "Coin Tracker";
        String coinsText = "Coins: " + formatHudValueDisplay(currentCoinsDisplay, currentCoins);
        String gainedText = "Gewinn: " + (gainedCoins.signum() >= 0 ? "+" : "")
                + formatAbbreviatedDisplay(gainedCoins);
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

        int maxTextWidth = client.font.width(title);
        for (String line : new String[] { coinsText, gainedText, cpmText, timeText }) {
            maxTextWidth = Math.max(maxTextWidth, client.font.width(line));
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

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(xPosition, yOffset);
        matrices.scale(scale, scale);

        int overlayHeight = getCurrentOverlayHeight();
        if (CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerShowBackground) {
            context.fill(0, 0, overlayWidth, overlayHeight, 0x80000000);
        }

        int textY = PADDING;
        context.text(client.font, Component.literal(title), PADDING, textY, headerColor, true);
        textY += LINE_HEIGHT;
        context.text(client.font, Component.literal(coinsText), PADDING, textY, textColor, true);
        textY += LINE_HEIGHT;
        context.text(client.font, Component.literal(gainedText), PADDING, textY, textColor, true);
        textY += LINE_HEIGHT;
        context.text(client.font, Component.literal(cpmText), PADDING, textY, textColor, true);
        textY += LINE_HEIGHT;
        context.text(client.font, Component.literal(timeText), PADDING, textY, textColor, true);

        matrices.popMatrix();
    }

    private static String formatHudValueDisplay(String hudDisplay, BigDecimal fallbackValue) {
        if (hudDisplay != null && !hudDisplay.isEmpty()) {
            String normalized = BossBarHudValueDecoder.normalizeHudDisplay(hudDisplay);
            if (!normalized.isEmpty()) {
                BigDecimal parsed = HudNumberSuffixUtility.parseSuffixedValue(normalized);
                if (parsed != null) {
                    return formatCoinTrackerValue(parsed);
                }
            }
        }
        return formatCoinTrackerValue(fallbackValue);
    }

    /**
     * Coin-Tracker-Anzeige: Dezimal-Komma (1,25k), kein Tausendertrennzeichen im Koeffizienten.
     * Werte unter 1000 ohne Suffix (250 statt 250,000 / 250k).
     */
    private static String formatCoinTrackerValue(BigDecimal number) {
        if (number == null) {
            return "-";
        }
        if (number.signum() < 0) {
            return "-";
        }

        BigDecimal abs = number.stripTrailingZeros();
        if (abs.compareTo(BigDecimal.valueOf(1000)) < 0) {
            return abs.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }

        String suffix = HudNumberSuffixUtility.suffixForValue(abs);
        if (suffix.isEmpty()) {
            return abs.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }

        BigDecimal coefficient = HudNumberSuffixUtility.valuePerSuffixUnit(abs, suffix);
        return formatCoinTrackerCoefficient(coefficient) + suffix.toLowerCase(Locale.ROOT);
    }

    private static String formatCoinTrackerCoefficient(BigDecimal coefficient) {
        BigDecimal rounded = coefficient.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.toPlainString().replace('.', ',');
    }

    private static String formatAbbreviatedDisplay(BigDecimal number) {
        return formatCoinTrackerValue(number);
    }

    private static String formatAbbreviatedDisplay(long number) {
        if (number < 0) {
            return "-";
        }
        return formatCoinTrackerValue(BigDecimal.valueOf(number));
    }

    private static final int OVERLAY_LINE_COUNT = 5;

    public static int getCurrentOverlayWidth(Minecraft client) {
        if (client == null || client.font == null) {
            return MIN_OVERLAY_WIDTH;
        }
        int maxTextWidth = client.font.width("Coin Tracker");
        for (String line : getOverlayLines()) {
            maxTextWidth = Math.max(maxTextWidth, client.font.width(line));
        }
        return Math.max(MIN_OVERLAY_WIDTH, maxTextWidth + PADDING * 2);
    }

    static String[] getOverlayLines() {
        return new String[] {
                "Coins: " + formatHudValueDisplay(currentCoinsDisplay, currentCoins),
                "Gewinn: " + (gainedCoins.signum() >= 0 ? "+" : "") + formatAbbreviatedDisplay(gainedCoins),
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
        return HudNumberSuffixUtility.toLongOrMax(currentCoins);
    }

    public static long getGainedCoins() {
        return HudNumberSuffixUtility.toLongOrMax(gainedCoins);
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

    public static String getCurrentSoulsDisplay() {
        return currentSoulsDisplay;
    }

    public static String getCurrentCactusDisplay() {
        return currentCactusDisplay;
    }

    public static boolean isAllowedDimension() {
        return BossBarHudValueDecoder.isFloorDimension();
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

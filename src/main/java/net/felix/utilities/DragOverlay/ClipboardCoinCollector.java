package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.BossBarHudValueDecoder;

import java.math.BigDecimal;

/**
 * Liest Current Coins für das Clipboard aus der HUD-Bossbar (Pixel-Font-Dekodierung).
 * Dimensionsunabhängig: Factory- und Aincraft-Font werden automatisch erkannt.
 */
public final class ClipboardCoinCollector {

    private static final long HUD_TIMEOUT_MS = 10_000L;

    private static boolean isActive = false;
    private static BigDecimal currentCoins = BigDecimal.ZERO;
    private static String currentCoinsDisplay = "";
    private static long lastHudUpdateTime = 0;

    private ClipboardCoinCollector() {
    }

    public static void initialize() {
        if (isActive) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(ClipboardCoinCollector::onClientTick);
        isActive = true;
    }

    /**
     * Wird vom BossBarMixin aufgerufen, wenn eine Bossbar mit HUD-Statistik-Struktur erkannt wird.
     */
    public static void processBossBar(String bossBarText) {
        if (!isActive || !CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled) {
            return;
        }

        try {
            BossBarHudValueDecoder.ParsedValue coins = BossBarHudValueDecoder.parseCoinsFromBossBar(bossBarText);
            if (!coins.isValid()) {
                return;
            }
            lastHudUpdateTime = System.currentTimeMillis();
            currentCoins = coins.numericValue;
            if (coins.display != null && !coins.display.isEmpty()) {
                currentCoinsDisplay = coins.display;
            }
        } catch (Exception ignored) {
            // Silent error handling
        }
    }

    private static void onClientTick(MinecraftClient client) {
        if (!isActive || client.player == null || client.world == null) {
            return;
        }
        if (!CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled) {
            return;
        }
        if (lastHudUpdateTime > 0
                && System.currentTimeMillis() - lastHudUpdateTime > HUD_TIMEOUT_MS) {
            currentCoins = BigDecimal.ZERO;
            currentCoinsDisplay = "";
            lastHudUpdateTime = 0;
        }
    }

    public static BigDecimal getCurrentCoins() {
        return currentCoins;
    }

    public static String getCurrentCoinsDisplay() {
        return currentCoinsDisplay;
    }

    public static void setCoins(BigDecimal coins) {
        currentCoins = coins != null ? coins : BigDecimal.ZERO;
        lastHudUpdateTime = System.currentTimeMillis();
    }

    public static void shutdown() {
        isActive = false;
        currentCoins = BigDecimal.ZERO;
        currentCoinsDisplay = "";
        lastHudUpdateTime = 0;
    }

    public static boolean isActive() {
        return isActive;
    }
}

package net.felix.utilities.ItemViewer;

import net.felix.utilities.DragOverlay.CollectedMaterialsResourcesStorage;
import net.felix.utilities.Overall.BossBarHudValueDecoder;
import net.felix.utilities.Overall.HudNumberSuffixUtility;

import java.math.BigDecimal;

/**
 * Liest Kaktus und Seelen aus der HUD-Bossbar, wenn die Besitz-Anzeige im Item Viewer aktiv ist.
 * Unabhängig vom Coin-Tracker (Dimension, Einstellungen).
 */
public final class ItemViewerHudStatsCollector {

    private static BigDecimal currentSoulsAmount = null;
    private static BigDecimal currentCactusAmount = null;
    private static long currentSouls = -1;
    private static long currentCactus = -1;
    private static String currentSoulsDisplay = "";
    private static String currentCactusDisplay = "";

    private ItemViewerHudStatsCollector() {
    }

    public static void processBossBar(String bossBarText) {
        if (!ItemViewerUtility.isShowOwnedResources()) {
            return;
        }
        if (!BossBarHudValueDecoder.looksLikeHudStatsBar(bossBarText)) {
            return;
        }

        try {
            BossBarHudValueDecoder.HudStats stats = BossBarHudValueDecoder.parseHudStats(bossBarText);
            if (stats.souls != null && stats.souls.signum() >= 0) {
                currentSoulsAmount = stats.souls;
                currentSouls = HudNumberSuffixUtility.toLongOrMax(stats.souls);
                if (stats.soulsDisplay != null && !stats.soulsDisplay.isEmpty()) {
                    currentSoulsDisplay = BossBarHudValueDecoder.normalizeHudDisplayLowercase(stats.soulsDisplay);
                }
                CollectedMaterialsResourcesStorage.updateOtherHudValue(
                        CollectedMaterialsResourcesStorage.OTHER_SEELEN, stats.souls, currentSoulsDisplay);
            }
            if (stats.cactus != null && stats.cactus.signum() >= 0) {
                currentCactusAmount = stats.cactus;
                currentCactus = HudNumberSuffixUtility.toLongOrMax(stats.cactus);
                if (stats.cactusDisplay != null && !stats.cactusDisplay.isEmpty()) {
                    currentCactusDisplay = BossBarHudValueDecoder.normalizeHudDisplayLowercase(stats.cactusDisplay);
                }
                CollectedMaterialsResourcesStorage.updateOtherHudValue(
                        CollectedMaterialsResourcesStorage.OTHER_KAKTUS, stats.cactus, currentCactusDisplay);
            }
        } catch (Exception ignored) {
            // Silent error handling
        }
    }

    public static void clear() {
        currentSoulsAmount = null;
        currentCactusAmount = null;
        currentSouls = -1;
        currentCactus = -1;
        currentSoulsDisplay = "";
        currentCactusDisplay = "";
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

    public static boolean hasSoulsData() {
        return (currentSoulsAmount != null && currentSoulsAmount.signum() >= 0)
                || currentSouls >= 0
                || (currentSoulsDisplay != null && !currentSoulsDisplay.isBlank());
    }

    public static boolean hasCactusData() {
        return (currentCactusAmount != null && currentCactusAmount.signum() >= 0)
                || currentCactus >= 0
                || (currentCactusDisplay != null && !currentCactusDisplay.isBlank());
    }

    public static BigDecimal getSoulsAmount() {
        if (currentSoulsAmount != null && currentSoulsAmount.signum() >= 0) {
            return currentSoulsAmount;
        }
        if (currentSoulsDisplay != null && !currentSoulsDisplay.isBlank()) {
            return BossBarHudValueDecoder.parseHudAmount(currentSoulsDisplay);
        }
        if (currentSouls >= 0) {
            return BigDecimal.valueOf(currentSouls);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal getCactusAmount() {
        if (currentCactusAmount != null && currentCactusAmount.signum() >= 0) {
            return currentCactusAmount;
        }
        if (currentCactusDisplay != null && !currentCactusDisplay.isBlank()) {
            return BossBarHudValueDecoder.parseHudAmount(currentCactusDisplay);
        }
        if (currentCactus >= 0) {
            return BigDecimal.valueOf(currentCactus);
        }
        return BigDecimal.ZERO;
    }
}

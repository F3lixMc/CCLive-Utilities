package net.felix.utilities.ItemViewer;

import net.felix.utilities.DragOverlay.ClipboardAmbossRessourceCollector;
import net.felix.utilities.DragOverlay.ClipboardCoinCollector;
import net.felix.utilities.DragOverlay.CollectedMaterialsResourcesStorage;
import net.felix.utilities.Overall.BossBarHudValueDecoder;
import net.felix.utilities.Overall.CoinTrackerUtility;
import net.felix.utilities.Overall.HudNumberSuffixUtility;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Liefert aktuelle Besitzmengen für die Kosten-Anzeige im Item Viewer.
 */
public final class ItemViewerOwnedResources {

    private ItemViewerOwnedResources() {
    }

    private static boolean usesAbbreviatedUnit(String itemName) {
        return "Kaktus".equalsIgnoreCase(itemName) || "Seelen".equalsIgnoreCase(itemName);
    }

    public static String formatOwnedAmountForDisplay(String itemName) {
        if ("Kaktus".equalsIgnoreCase(itemName)) {
            String display = getCactusDisplay();
            if (display != null && !display.isBlank()) {
                return display;
            }
        } else if ("Seelen".equalsIgnoreCase(itemName)) {
            String display = getSoulsDisplay();
            if (display != null && !display.isBlank()) {
                return display;
            }
        }
        BigDecimal owned = getOwnedAmount(itemName);
        if (usesAbbreviatedUnit(itemName) && owned.signum() > 0) {
            return normalizeAbbreviatedDisplay(HudNumberSuffixUtility.formatAbbreviated(owned));
        }
        return formatAmountForDisplay(owned);
    }

    public static String formatNeededAmountForDisplay(String itemName, Object amount) {
        if (usesAbbreviatedUnit(itemName) && amount instanceof String str && !str.isBlank()) {
            return str;
        }
        return formatAmountForDisplay(amount);
    }

    /**
     * Wie oft ein Bauplan mit den aktuellen Besitzmengen herstellbar ist (Minimum über alle Kosten).
     * @return {@code -1} wenn keine vergleichbaren Kosten vorhanden sind
     */
    public static int calculateCraftableCount(PriceData price) {
        if (price == null) {
            return -1;
        }
        CostItem amboss = price.Amboss != null ? price.Amboss : price.amboss;
        CostItem ressource = price.Ressource != null ? price.Ressource : price.ressource;
        CostItem[] costs = {
                price.coin, price.cactus, price.soul,
                price.material1, price.material2, price.material3, price.material4, price.material5,
                amboss, ressource, price.paper_shreds
        };

        int minCrafts = Integer.MAX_VALUE;
        boolean hasCost = false;
        for (CostItem costItem : costs) {
            int crafts = craftsAffordableFromCost(costItem);
            if (crafts < 0) {
                continue;
            }
            hasCost = true;
            minCrafts = Math.min(minCrafts, crafts);
        }
        if (!hasCost) {
            return -1;
        }
        return minCrafts == Integer.MAX_VALUE ? 0 : minCrafts;
    }

    public static BigDecimal getOwnedAmount(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return BigDecimal.ZERO;
        }
        String trimmed = itemName.trim();
        if ("Coins".equalsIgnoreCase(trimmed)) {
            BigDecimal coins = ClipboardCoinCollector.getCurrentCoins();
            if (coins != null && coins.signum() > 0) {
                return coins;
            }
            return BigDecimal.valueOf(CoinTrackerUtility.getCurrentCoins());
        }
        if ("Kaktus".equalsIgnoreCase(trimmed)) {
            return resolveAbbreviatedOwnedAmount(
                    ItemViewerUtility.isShowOwnedResources() && ItemViewerHudStatsCollector.hasCactusData(),
                    ItemViewerHudStatsCollector::getCactusAmount,
                    CollectedMaterialsResourcesStorage.OTHER_KAKTUS,
                    CoinTrackerUtility::getCurrentCactusDisplay);
        }
        if ("Seelen".equalsIgnoreCase(trimmed)) {
            return resolveAbbreviatedOwnedAmount(
                    ItemViewerUtility.isShowOwnedResources() && ItemViewerHudStatsCollector.hasSoulsData(),
                    ItemViewerHudStatsCollector::getSoulsAmount,
                    CollectedMaterialsResourcesStorage.OTHER_SEELEN,
                    CoinTrackerUtility::getCurrentSoulsDisplay);
        }
        if ("Pergamentfetzen".equalsIgnoreCase(trimmed)) {
            return BigDecimal.valueOf(CollectedMaterialsResourcesStorage.getOtherAmount(trimmed));
        }

        long stored = CollectedMaterialsResourcesStorage.getSyncedOwnedAmount(trimmed);
        if (stored > 0) {
            return BigDecimal.valueOf(stored);
        }
        long amboss = ClipboardAmbossRessourceCollector.getAmbossAmount(trimmed);
        if (amboss > 0) {
            return BigDecimal.valueOf(amboss);
        }
        long ressource = ClipboardAmbossRessourceCollector.getRessourceAmount(trimmed);
        if (ressource > 0) {
            return BigDecimal.valueOf(ressource);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal parseNeededAmount(Object amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (amount instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (amount instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String str = amount.toString().trim();
        if (str.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (HudNumberSuffixUtility.extractSuffix(str) != null) {
            return BossBarHudValueDecoder.parseHudAmount(str);
        }
        BigDecimal suffixed = HudNumberSuffixUtility.parseSuffixedValue(str.replace(" ", ""));
        if (suffixed != null) {
            return suffixed;
        }
        String plain = str.replace(".", "").replace(",", "");
        try {
            return new BigDecimal(plain);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public static String formatAmountForDisplay(Object amount) {
        if (amount == null) {
            return "0";
        }
        if (amount instanceof String str) {
            BigDecimal parsed = parseNeededAmount(str);
            if (parsed.signum() != 0 || "0".equals(str.trim())) {
                return HudNumberSuffixUtility.formatWithSeparators(parsed);
            }
            return str;
        }
        if (amount instanceof BigDecimal bigDecimal) {
            return HudNumberSuffixUtility.formatWithSeparators(bigDecimal);
        }
        if (amount instanceof Number) {
            return HudNumberSuffixUtility.formatWithSeparators(new BigDecimal(amount.toString()));
        }
        return amount.toString();
    }

    private static String getCactusDisplay() {
        if (ItemViewerUtility.isShowOwnedResources()) {
            String display = ItemViewerHudStatsCollector.getCurrentCactusDisplay();
            if (display != null && !display.isBlank()) {
                return normalizeAbbreviatedDisplay(display);
            }
        }
        String stored = CollectedMaterialsResourcesStorage.getOtherDisplay(
                CollectedMaterialsResourcesStorage.OTHER_KAKTUS);
        if (stored != null && !stored.isBlank()) {
            return stored;
        }
        return normalizeAbbreviatedDisplay(CoinTrackerUtility.getCurrentCactusDisplay());
    }

    private static String getSoulsDisplay() {
        if (ItemViewerUtility.isShowOwnedResources()) {
            String display = ItemViewerHudStatsCollector.getCurrentSoulsDisplay();
            if (display != null && !display.isBlank()) {
                return normalizeAbbreviatedDisplay(display);
            }
        }
        String stored = CollectedMaterialsResourcesStorage.getOtherDisplay(
                CollectedMaterialsResourcesStorage.OTHER_SEELEN);
        if (stored != null && !stored.isBlank()) {
            return stored;
        }
        return normalizeAbbreviatedDisplay(CoinTrackerUtility.getCurrentSoulsDisplay());
    }

    private static String normalizeAbbreviatedDisplay(String display) {
        if (display == null || display.isBlank()) {
            return "";
        }
        return BossBarHudValueDecoder.normalizeHudDisplayLowercase(display);
    }

    private static BigDecimal resolveAbbreviatedOwnedAmount(
            boolean useLive,
            java.util.function.Supplier<BigDecimal> liveAmount,
            String storageKey,
            java.util.function.Supplier<String> coinTrackerDisplay) {
        if (useLive) {
            BigDecimal live = liveAmount.get();
            if (live != null && live.signum() >= 0) {
                return live;
            }
        }
        BigDecimal stored = CollectedMaterialsResourcesStorage.getOtherAmountBigDecimal(storageKey);
        if (stored.signum() > 0) {
            return stored;
        }
        return BossBarHudValueDecoder.parseHudAmount(coinTrackerDisplay.get());
    }

    private static int craftsAffordableFromCost(CostItem costItem) {
        if (costItem == null || costItem.itemName == null || costItem.amount == null) {
            return -1;
        }
        BigDecimal needed = parseNeededAmount(costItem.amount);
        if (needed.signum() <= 0) {
            return -1;
        }
        BigDecimal owned = getOwnedAmount(costItem.itemName);
        BigDecimal crafts = owned.divide(needed, 0, RoundingMode.FLOOR);
        if (crafts.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            return Integer.MAX_VALUE;
        }
        return crafts.intValue();
    }
}

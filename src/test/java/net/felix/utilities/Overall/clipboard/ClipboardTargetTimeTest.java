package net.felix.utilities.Overall.clipboard;
import org.junit.jupiter.api.Test;


import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipboardTargetTimeTest {
    @Test
    void durationUsesApproxMinutesOrHours() {
        assertEquals("3min", ClipboardTargetTime.formatDuration(3.5));
        assertEquals("59min", ClipboardTargetTime.formatDuration(59.0));
        assertEquals("1h", ClipboardTargetTime.formatDuration(90.0));
        assertEquals("1h", ClipboardTargetTime.formatDuration(119.0));
        assertEquals("2h", ClipboardTargetTime.formatDuration(120.0));
        assertEquals("30sek", ClipboardTargetTime.formatDuration(0.5));
        assertEquals("1sek", ClipboardTargetTime.formatDuration(0.01));
        assertEquals("1h 30min", ClipboardTargetTime.formatDurationDetailed(90.0));
        assertEquals("1h", ClipboardTargetTime.formatDurationDetailed(60.0));
        assertEquals("1h 59min", ClipboardTargetTime.formatDurationDetailed(119.0));
        assertEquals("3min 30sek", ClipboardTargetTime.formatDurationDetailed(3.5));
        assertEquals("30sek", ClipboardTargetTime.formatDurationDetailed(0.5));
        assertEquals("1h 30min", ClipboardTargetTime.chooseTimeText("1h", "1h 30min", 80, 48, 140));
        assertEquals("1h", ClipboardTargetTime.chooseTimeText("1h", "1h 30min", 80, 48, 127));
        assertEquals(" ≈ 1h", ClipboardTargetTime.formatRemaining(BigDecimal.valueOf(120), 2.0));
        assertEquals(" ≈ 3min", ClipboardTargetTime.formatRemaining(BigDecimal.valueOf(7), 2.0));
        assertEquals("", ClipboardTargetTime.formatRemaining(BigDecimal.ZERO, 5.0));
        assertEquals("", ClipboardTargetTime.formatRemaining(BigDecimal.TEN, 0.0));
    }

    @Test
    void coinRateIgnoresDisplayMode() {
        assertEquals(12.0, ClipboardTargetTime.coinRate(true, true, 0, 12.0));
        assertEquals(12.0, ClipboardTargetTime.coinRate(true, true, 1, 12.0));
        assertEquals(0.0, ClipboardTargetTime.coinRate(false, true, 0, 12.0));
        assertEquals(0.0, ClipboardTargetTime.coinRate(true, false, 1, 12.0));
    }

    @Test
    void farmRateMatchesOnlyTheActiveResource() {
        assertEquals(8.0, ClipboardTargetTime.farmRate("Eichenholz", "Eichenholz", 8.0));
        assertEquals(0.0, ClipboardTargetTime.farmRate("Fichtenholz", "Eichenholz", 8.0));
    }

    @Test
    void excludedLinesNeverReceiveARate() {
        assertEquals(0.0, ClipboardTargetTime.rateFor(2, "Kettenglied", false, false, true, true, 0, 10, 10, "Kettenglied", 10));
        assertEquals(0.0, ClipboardTargetTime.rateFor(3, "Kupferbarren", false, false, true, true, 0, 10, 10, "Kupferbarren", 10));
        assertEquals(0.0, ClipboardTargetTime.rateFor(3, "Eisenbarren", false, false, true, true, 0, 10, 10, "Eisenbarren", 10));
        assertEquals(0.0, ClipboardTargetTime.rateFor(3, "Goldbarren", false, false, true, true, 0, 10, 10, "Goldbarren", 10));
        assertEquals(0.0, ClipboardTargetTime.rateFor(3, "Netherite Schrott", false, false, true, true, 0, 10, 10, "Netherite Schrott", 10));
        assertEquals(0.0, ClipboardTargetTime.rateFor(1, "Holz", true, false, true, true, 0, 10, 10, "Holz", 10));
        assertEquals(0.0, ClipboardTargetTime.rateFor(1, "Holz", false, true, true, true, 0, 10, 10, "Holz", 10));
        assertEquals(4.0, ClipboardTargetTime.rateFor(1, "Holz", false, false, true, true, 0, 9, 4, "Erz", 8));
        assertEquals(8.0, ClipboardTargetTime.rateFor(3, "Eichenholz", false, false, true, true, 0, 9, 4, "Eichenholz", 8));
        assertEquals(9.0, ClipboardTargetTime.rateFor(0, "Coins", false, false, true, true, 1, 9, 4, "Eichenholz", 8));
    }

    @Test
    void sameNameKeepsSeparateCategories() {
        assertFalse(ClipboardTargetTime.totalKey(1, "Klammer")
                .equals(ClipboardTargetTime.totalKey(2, "Klammer")));
        assertEquals(0.0, ClipboardTargetTime.rateFor(2, "Klammer", false, false, true, true, 0, 1, 6, null, 0));
        assertEquals(6.0, ClipboardTargetTime.rateFor(1, "Klammer", false, false, true, true, 0, 1, 6, null, 0));
        assertEquals(0.0, ClipboardTargetTime.rateFor(1, "Kupferbarren", false, false, true, true, 0, 1, 6, null, 0));
    }

    @Test
    void rowWindowKeepsShopLinesAndScrollsNormalLines() {
        ClipboardTargetTime.CostWindow limited = ClipboardTargetTime.window(5, 2, 5, 0);
        assertEquals(0, limited.start);
        assertEquals(3, limited.count);
        assertTrue(limited.scrollable);

        ClipboardTargetTime.CostWindow shopOnly = ClipboardTargetTime.window(4, 2, 1, 0);
        assertEquals(0, shopOnly.count);
        assertFalse(shopOnly.scrollable);
        assertTrue(shopOnly.overflow);

        ClipboardTargetTime.CostWindow all = ClipboardTargetTime.window(5, 2, 0, 3);
        assertEquals(0, all.start);
        assertEquals(5, all.count);
        assertFalse(all.overflow);

        ClipboardTargetTime.CostWindow scrolled = ClipboardTargetTime.window(5, 2, 5, 9);
        assertEquals(2, scrolled.start);
        assertEquals(3, scrolled.count);
    }

    @Test
    void statusTextDistinguishesEmptyClipboardFromFinishedCosts() {
        assertEquals(ClipboardTargetTime.NO_BLUEPRINTS, ClipboardTargetTime.statusText(false, 0, 0));
        assertEquals(ClipboardTargetTime.ALL_COLLECTED, ClipboardTargetTime.statusText(true, 0, 0));
        assertNull(ClipboardTargetTime.statusText(true, 4, 0));
        assertNull(ClipboardTargetTime.statusText(true, 2, 2));
    }

    @Test
    void shopCostsMergeOnlyWhileTheOptionIsOn() {
        Map<String, BigDecimal> normal = Map.of(ClipboardTargetTime.totalKey(0, "Coins"), BigDecimal.valueOf(100),
                ClipboardTargetTime.totalKey(1, "Holz"), BigDecimal.TEN);
        Map<String, BigDecimal> shop = Map.of(ClipboardTargetTime.totalKey(0, "Coins"), BigDecimal.valueOf(40),
                ClipboardTargetTime.totalKey(1, "Pergamentfetzen"), BigDecimal.valueOf(2));
        Map<String, BigDecimal> off = ClipboardTargetTime.mergeCosts(normal, shop, false);
        assertEquals(BigDecimal.valueOf(100), off.get(ClipboardTargetTime.totalKey(0, "Coins")));
        assertFalse(off.containsKey(ClipboardTargetTime.totalKey(1, "Pergamentfetzen")));

        Map<String, BigDecimal> on = ClipboardTargetTime.mergeCosts(normal, shop, true);
        assertEquals(BigDecimal.valueOf(140), on.get(ClipboardTargetTime.totalKey(0, "Coins")));
        assertEquals(BigDecimal.valueOf(2), on.get(ClipboardTargetTime.totalKey(1, "Pergamentfetzen")));
    }

    @Test
    void completionUsesTheOverviewQuantityOnTheMergedList() {
        String coins = ClipboardTargetTime.totalKey(0, "Coins");
        Map<String, BigDecimal> needed = ClipboardTargetTime.mergeCosts(
                Map.of(coins, BigDecimal.valueOf(10)),
                Map.of(coins, BigDecimal.valueOf(5)),
                true);
        Map<String, BigDecimal> owned = Map.of(coins, BigDecimal.valueOf(30));
        assertTrue(ClipboardTargetTime.costsComplete(needed, owned, 2));
        assertFalse(ClipboardTargetTime.costsComplete(needed, owned, 3));
        assertTrue(ClipboardTargetTime.costsComplete(
                ClipboardTargetTime.mergeCosts(Map.of(coins, BigDecimal.TEN), Map.of(coins, BigDecimal.TEN), false),
                owned, 3));
    }

    @Test
    void mergedCoinTimeUsesTheWholeMissingAmount() {
        assertEquals(" ≈ 15min", ClipboardTargetTime.timeLabels(BigDecimal.valueOf(30), 2.0)[0]);
        assertEquals("", ClipboardTargetTime.timeLabels(BigDecimal.valueOf(30), 0.0)[0]);
        assertEquals("", ClipboardTargetTime.formatRemaining(BigDecimal.valueOf(4), 0.0));
    }

    @Test
    void maxLinesAcceptsOnlyPositiveIntegersOrBlank() {
        int[] parsed = new int[1];
        assertTrue(ClipboardTargetTime.tryParseMaxLines("", parsed));
        assertEquals(0, parsed[0]);
        assertTrue(ClipboardTargetTime.tryParseMaxLines("4", parsed));
        assertEquals(4, parsed[0]);
        assertFalse(ClipboardTargetTime.tryParseMaxLines("0", parsed));
        assertFalse(ClipboardTargetTime.tryParseMaxLines("abc", parsed));
    }
}

package net.felix.utilities.Community.leaderboards.panel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardLayoutTest {
    @Test
    void defaultSizeIsEightyFivePercentAndKeepsTheGapAtAnotherSize() {
        assertEquals(0.85f, LeaderboardInventoryOverlay.DEFAULT_SCALE);
        assertGap(LeaderboardInventoryOverlay.DEFAULT_SCALE);
        assertGap(1.1f);
        float resized = LeaderboardInventoryOverlay.scaleFromSize(300, 400);
        int width = LeaderboardInventoryOverlay.scaledWidth(resized);
        int height = LeaderboardInventoryOverlay.scaledHeight(resized);
        assertEquals(width, Math.round(LeaderboardPanelLayout.TEX_W * resized));
        assertEquals(height, Math.round(LeaderboardPanelLayout.TEX_H * resized));
        assertGap(resized);
    }

    @Test
    void arrowHitsStayOnTheScaledButtons() {
        assertArrow(LeaderboardInventoryOverlay.DEFAULT_SCALE, LeaderboardPanelLayout.PAGE_PREV);
        assertArrow(LeaderboardInventoryOverlay.DEFAULT_SCALE, LeaderboardPanelLayout.PAGE_NEXT);
        assertArrow(1.1f, LeaderboardPanelLayout.TYPE_PREV);
        assertArrow(1.1f, LeaderboardPanelLayout.TYPE_NEXT);
    }

    @Test
    void wheelUsesTheArrowPagesAndStopsOutsideOrWhenHidden() {
        assertEquals(2, LeaderboardInventoryOverlay.pageAfter(0, 25, -1));
        assertEquals(1, LeaderboardInventoryOverlay.pageAfter(0, 25, 1));
        assertEquals(0, LeaderboardInventoryOverlay.pageAfter(2, 25, 1));
        assertEquals(0, LeaderboardInventoryOverlay.pageAfter(0, 10, -1));
        assertEquals(0, LeaderboardInventoryOverlay.pageAfter(0, 10, 1));

        assertEquals(1, LeaderboardInventoryOverlay.scrollDirection(-1));
        assertEquals(-1, LeaderboardInventoryOverlay.scrollDirection(1));
        assertTrue(LeaderboardInventoryOverlay.scrollHitsBoard(true, true, -1));
        assertTrue(LeaderboardInventoryOverlay.scrollHitsBoard(true, true, 1));
        assertFalse(LeaderboardInventoryOverlay.scrollHitsBoard(true, false, -1));
        assertFalse(LeaderboardInventoryOverlay.scrollHitsBoard(false, true, -1));
        assertFalse(LeaderboardInventoryOverlay.scrollHitsBoard(false, true, 1));
        assertFalse(LeaderboardInventoryOverlay.scrollHitsBoard(true, true, 0));
    }

    private static void assertGap(float scale) {
        int left = 400;
        int top = 20;
        int imageHeight = 166;
        int width = LeaderboardInventoryOverlay.scaledWidth(scale);
        int height = LeaderboardInventoryOverlay.scaledHeight(scale);
        int x = LeaderboardInventoryOverlay.panelX(left, width);
        int y = LeaderboardInventoryOverlay.panelY(top, imageHeight, height);
        assertEquals(left - 1, x + width);
        assertEquals(top + (imageHeight - height) / 2, y);
        assertFalse(LeaderboardInventoryOverlay.contains(left - 1, y + height / 2.0, x, y, width, height));
        assertTrue(LeaderboardInventoryOverlay.contains(x, y, x, y, width, height));
        assertFalse(LeaderboardInventoryOverlay.contains(x - 1, y + 4, x, y, width, height));
    }

    private static void assertArrow(float scale, LeaderboardPanelLayout.Hit hit) {
        int left = 500;
        int top = 30;
        int imageHeight = 166;
        int width = LeaderboardInventoryOverlay.scaledWidth(scale);
        int height = LeaderboardInventoryOverlay.scaledHeight(scale);
        int x = LeaderboardInventoryOverlay.panelX(left, width);
        int y = LeaderboardInventoryOverlay.panelY(top, imageHeight, height);
        double mouseX = x + ((hit.x1() + hit.x2()) / 2.0 + 0.5) * scale;
        double mouseY = y + ((hit.y1() + hit.y2()) / 2.0 + 0.5) * scale;
        assertSame(hit, LeaderboardInventoryOverlay.hitAt(mouseX, mouseY, x, y, scale));
        assertTrue(LeaderboardInventoryOverlay.contains(mouseX, mouseY, x, y, width, height));
        assertNull(LeaderboardInventoryOverlay.hitAt(left - 0.5, mouseY, x, y, scale));
    }
}

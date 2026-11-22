package net.felix.utilities.Overall.clipboard;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClipboardLayoutTest {
    @Test
    void clampUsesTheLargestFootprintSoAShorterPageDoesNotDropTheOrigin() {
        ClipboardLayout.Point tall = ClipboardLayout.clampOrigin(100, 100, 50, 150, 200, 200);
        ClipboardLayout.Point shortPage = ClipboardLayout.clampOrigin(100, 100, 50, 40, 200, 200);
        assertEquals(50, tall.y());
        assertEquals(100, shortPage.y());
        assertEquals(20, ClipboardLayout.clampOrigin(20, 20, 50, 150, 200, 200).y());
    }

    @Test
    void sharedWidthKeepsHiddenPinnedEntriesAndLetsOverviewOnlyGrow() {
        int[] pinned = {220, 400, 190};
        assertEquals(400, ClipboardLayout.sharedWidth(180, 200, pinned, false, 8));
        assertEquals(500, ClipboardLayout.sharedWidth(180, 500, pinned, false, 8));
        assertEquals(408, ClipboardLayout.sharedWidth(180, 100, new int[] {400}, true, 8));
    }

    @Test
    void arrowsStayOnTheSharedQuantityBlock() {
        int left = ClipboardLayout.arrowLeft(300, 40, 30, 5, 8);
        assertEquals(left, ClipboardLayout.arrowLeft(300, 40, 30, 5, 8));
        assertTrue(ClipboardLayout.arrowLeft(300, 60, 30, 5, 8) < left);
    }

    @Test
    void normalCostsIgnoreShopAndTreatAnEmptyListAsComplete() {
        assertTrue(ClipboardLayout.normalCostsComplete(new boolean[] {true, true}));
        assertFalse(ClipboardLayout.normalCostsComplete(new boolean[] {true, false}));
        assertTrue(ClipboardLayout.normalCostsComplete(new boolean[0]));
    }

    @Test
    void hidingAMiddleEntryKeepsTheOtherBlueprintsOnTheirStoredSlots() {
        boolean[] completed = {false, true, false};
        int[] visible = ClipboardLayout.visibleStoredIndexes(completed, true);
        assertArrayEquals(new int[] {0, 2}, visible);
        assertArrayEquals(new int[] {0, 1, 2}, ClipboardLayout.visibleStoredIndexes(completed, false));
        assertEquals(2, ClipboardLayout.pageForStoredIndex(visible, 0));
        assertEquals(3, ClipboardLayout.pageForStoredIndex(visible, 2));
        assertEquals(3, ClipboardLayout.pageForStoredIndex(visible, 1));
        assertEquals(1, ClipboardLayout.pageForStoredIndex(visible, -1));
        assertEquals(0, ClipboardLayout.storedIndexForPage(visible, 2));
        assertEquals(2, ClipboardLayout.storedIndexForPage(visible, 3));
    }
}

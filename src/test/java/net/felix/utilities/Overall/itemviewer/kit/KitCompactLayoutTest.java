package net.felix.utilities.Overall.itemviewer.kit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitCompactLayoutTest {
    @Test
    void shrinksToTheFilteredItemsAndKeepsAPageWhenTheHeightIsLimited() {
        assertEquals(1, KitCompactLayout.gridForCount(1, 9, 8)[0]);
        assertEquals(1, KitCompactLayout.gridForCount(1, 9, 8)[1]);
        int[] six = KitCompactLayout.gridForCount(6, 9, 8);
        assertEquals(6, six[0]);
        assertEquals(1, six[1]);
        int[] paged = KitCompactLayout.gridForCount(40, 6, 2);
        assertEquals(6, paged[0]);
        assertEquals(2, paged[1]);
    }
}

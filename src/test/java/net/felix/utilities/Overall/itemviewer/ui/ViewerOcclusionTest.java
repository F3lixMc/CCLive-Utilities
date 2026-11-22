package net.felix.utilities.Overall.itemviewer.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewerOcclusionTest {

    @Test
    void gridFavoritesAndPopupCoverThePointBesideThemDoesNot() {
        int[] areas = {
                10, 40, 80, 60,
                100, 10, 16, 16,
                200, 20, 40, 30
        };
        assertTrue(ViewerInputHandler.coversAny(20, 50, areas));
        assertTrue(ViewerInputHandler.coversAny(108, 18, areas));
        assertTrue(ViewerInputHandler.coversAny(210, 30, areas));
        assertFalse(ViewerInputHandler.coversAny(5, 5, areas));
        assertFalse(ViewerInputHandler.coversAny(90, 50, areas));
    }
}

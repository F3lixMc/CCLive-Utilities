package net.felix.utilities.Aincraft.blueprints.viewer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintViewerHeightTest {

    @Test
    void heightFollowsVisibleRowsAndKeepsEqualInsets() {
        int titleOnly = BPViewerUtility.heightForVisibleRows(0, 1.0f);
        int oneRow = BPViewerUtility.heightForVisibleRows(1, 1.0f);
        int threeRows = BPViewerUtility.heightForVisibleRows(3, 1.0f);
        assertTrue(oneRow > titleOnly);
        assertEquals(23, titleOnly);
        assertEquals(35, oneRow);
        assertEquals(59, threeRows);
        assertEquals(33, BPViewerUtility.heightForVisibleRows(1, 2.0f));
        assertEquals(4, BPViewerUtility.visibleRowCount(4, 1, false));
        assertEquals(2, BPViewerUtility.visibleRowCount(4, 2, true));
        assertEquals(1, BPViewerUtility.visibleRowCount(4, 0, true));
        assertEquals(0, BPViewerUtility.visibleRowCount(0, 0, true));
    }
}

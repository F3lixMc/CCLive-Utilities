package net.felix.utilities.Aincraft.blueprints.viewer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoundStorageKeyTest {
    @Test
    void namesOutsideTheFloorListKeepTheCatalogName() {
        assertEquals("Holzschwert", BPViewerUtility.foundStorageKey("Holzschwert", null));
        assertEquals("Holzschwert", BPViewerUtility.foundStorageKey("Holzschwert", "rare"));
        assertEquals("Holzschwert:epic", BPViewerUtility.foundStorageKey("Holzschwert", "epic"));
        assertEquals("Holzschwert:legendary", BPViewerUtility.foundStorageKey("Holzschwert", "legendary"));
    }
}

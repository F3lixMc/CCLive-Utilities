package net.felix.utilities.ItemViewer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemNameLookupTest {
    @Test
    void containerBlueprintNameMatchesTheJeiItemName() {
        assertEquals("Meister-Rute", ItemViewerUtility.cleanItemNameForLookup("Meister-Rute - [Bauplan]"));
        assertEquals("Meister-Rute", ItemViewerUtility.cleanItemNameForLookup("Meister-Rute - [Bauplan] [e12]"));
        assertEquals("Meister-Rute", ItemViewerUtility.cleanItemNameForLookup("Meister-Rute [Bauplan]"));
        assertEquals("Schwert", ItemViewerUtility.cleanItemNameForLookup("Schwert [Drachengeschmiedet]"));
    }

    @Test
    void favoritesCoverBlueprintsModulesAndPowerCrystals() {
        ItemData blueprint = named("Abgenutze Armbrust", "blueprints");
        blueprint.info = new ItemInfo();
        blueprint.info.blueprint = true;
        ItemData module = named("Erzbrecher #1", "modules");
        ItemData crystal = named("Machtkristall der Opferung", "power_crystals");
        ItemData slot = named("Machtkristall Slot 1", "power_crystal_slots");
        slot.info = new ItemInfo();
        slot.info.power_crystal = true;

        assertTrue(ItemViewerUtility.isFavoritable(blueprint));
        assertTrue(ItemViewerUtility.isFavoritable(module));
        assertTrue(ItemViewerUtility.isFavoritable(crystal));
        assertFalse(ItemViewerUtility.isFavoritable(slot));
        assertFalse(ItemViewerUtility.isFavoritable(named("", "modules")));
    }

    private static ItemData named(String name, String category) {
        ItemData item = new ItemData();
        item.name = name;
        item.category = category;
        return item;
    }
}

package net.felix.utilities.Overall.itemviewer.inventory;

import net.felix.utilities.Overall.itemviewer.data.ItemData;
import net.felix.utilities.Overall.itemviewer.data.ItemInfo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemNameLookupTest {
    @Test
    void containerBlueprintNameMatchesTheJeiItemName() {
        assertEquals("Meister-Rute", InventoryItemCapture.cleanItemNameForLookup("Meister-Rute - [Bauplan]"));
        assertEquals("Meister-Rute", InventoryItemCapture.cleanItemNameForLookup("Meister-Rute - [Bauplan] [e12]"));
        assertEquals("Meister-Rute", InventoryItemCapture.cleanItemNameForLookup("Meister-Rute [Bauplan]"));
        assertEquals("Schwert", InventoryItemCapture.cleanItemNameForLookup("Schwert [Drachengeschmiedet]"));
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

        assertTrue(ItemData.isFavoritable(blueprint));
        assertTrue(ItemData.isFavoritable(module));
        assertTrue(ItemData.isFavoritable(crystal));
        assertFalse(ItemData.isFavoritable(slot));
        assertFalse(ItemData.isFavoritable(named("", "modules")));
    }

    private static ItemData named(String name, String category) {
        ItemData item = new ItemData();
        item.name = name;
        item.category = category;
        return item;
    }
}

package net.felix.utilities.Aincraft.blueprints.viewer;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintKitFilterTest {

    @Test
    void pagesStartOnTheKitPageAndFallBackToCommon() {
        assertEquals(List.of("common", "uncommon", "rare", "epic", "legendary"), BlueprintKitFilter.pageOrder(false));
        assertEquals("kitfilter", BlueprintKitFilter.pageOrder(true).get(0));
        assertEquals("common", BlueprintKitFilter.step("kitfilter", true, 1));
        assertEquals("kitfilter", BlueprintKitFilter.step("common", true, -1));
        assertEquals("common", BlueprintKitFilter.step("common", false, -1));
        assertEquals("legendary", BlueprintKitFilter.step("legendary", true, 1));
        assertEquals("kitfilter", BlueprintKitFilter.pageAfter(false, true, "rare"));
        assertEquals("epic", BlueprintKitFilter.pageAfter(true, true, "epic"));
        assertEquals("common", BlueprintKitFilter.pageAfter(true, false, "kitfilter"));
        assertEquals("legendary", BlueprintKitFilter.pageAfter(true, false, "legendary"));
    }

    @Test
    void kitPageSortsByFloorThenNameAndOmitsUnknownItems() {
        List<BlueprintKitFilter.CatalogItem> catalog = List.of(
                item("floor_10", "common", "Zepter"),
                item("floor_2", "rare", "Anfänger Hacke"),
                item("floor_2", "common", "Holzschwert"),
                item("floor_lobby", "common", "Ohne Nummer"),
                item("floor_0", "uncommon", "Anfang"),
                item("floor_4", "epic", "Modul"));
        List<BlueprintKitFilter.KitPart> parts = List.of(
                part("Anfang", null),
                part("Zepter", null),
                part("Holzschwert ×2", null),
                part("Anfänger Hacke", "rare"),
                part("Ohne Nummer", null),
                part("Keine Bauplan", null));

        List<BlueprintKitFilter.Entry> entries = BlueprintKitFilter.kitPage(catalog, parts);
        assertEquals(List.of("Anfang", "Anfänger Hacke", "Holzschwert", "Zepter", "Ohne Nummer"), names(entries));
        assertEquals("Holzschwert (Ebene 2)", entries.get(2).label());
        assertEquals("Ohne Nummer", entries.get(4).label());
        assertNull(entries.get(4).floor());
    }

    @Test
    void sameNameAndRarityKeepsTheLowestFloor() {
        List<BlueprintKitFilter.CatalogItem> catalog = List.of(
                item("floor_5", "common", "Holzschwert"),
                item("floor_1", "common", "Holzschwert"),
                item("floor_3", "common", "Holzschwert"));
        List<BlueprintKitFilter.Entry> entries = BlueprintKitFilter.kitPage(catalog, List.of(part("Holzschwert", null)));
        assertEquals(1, entries.size());
        assertEquals(1, entries.get(0).floor());
        assertEquals("Holzschwert (Ebene 1)", entries.get(0).label());
    }

    @Test
    void rarityKeepsOnlyTheMatchingDrachenzahn() {
        List<BlueprintKitFilter.CatalogItem> catalog = List.of(
                item("floor_8", "epic", "Drachenzahn"),
                item("floor_9", "legendary", "Drachenzahn"));
        List<BlueprintKitFilter.Entry> epic = BlueprintKitFilter.kitPage(catalog, List.of(part("Drachenzahn", "Epic")));
        assertEquals(1, epic.size());
        assertEquals("epic", epic.get(0).rarity());

        List<BlueprintKitFilter.Entry> both = BlueprintKitFilter.kitPage(catalog, List.of(part("Drachenzahn", null)));
        assertEquals(Set.of("epic", "legendary"), Set.of(both.get(0).rarity(), both.get(1).rarity()));
    }

    @Test
    void rarityPageStaysOnTheCurrentFloorAndKitRarity() {
        List<BlueprintKitFilter.CatalogItem> catalog = List.of(
                item("floor_2", "epic", "Drachenzahn"),
                item("floor_2", "legendary", "Drachenzahn"),
                item("floor_2", "epic", "Anderes"),
                item("floor_4", "epic", "Drachenzahn"));
        List<BlueprintKitFilter.KitPart> parts = List.of(part("Drachenzahn", "epic"));
        List<BlueprintKitFilter.Entry> page = BlueprintKitFilter.rarityPage(catalog, parts, "floor_2", "epic");
        assertEquals(List.of("Drachenzahn"), names(page));
        assertEquals("Drachenzahn", page.get(0).label());
        assertTrue(BlueprintKitFilter.rarityPage(catalog, parts, null, "epic").isEmpty());
    }

    @Test
    void colorsFollowFoundStatusOnlyWhileFiltered() {
        assertEquals(BlueprintKitFilter.FOUND_EPIC, BlueprintKitFilter.nameColor("epic", true, true));
        assertEquals(BlueprintKitFilter.MISSING_EPIC, BlueprintKitFilter.nameColor("Epic", false, true));
        assertEquals(BlueprintKitFilter.FOUND_LEGENDARY, BlueprintKitFilter.nameColor("legendary", true, true));
        assertEquals(BlueprintKitFilter.MISSING_LEGENDARY, BlueprintKitFilter.nameColor("legendary", false, true));
        assertEquals(0xFF5555FF, BlueprintKitFilter.FOUND_RARE);
        assertEquals(BlueprintKitFilter.FOUND_RARE, BlueprintKitFilter.nameColor("rare", true, true));
        assertEquals(BlueprintKitFilter.MISSING_RARE, BlueprintKitFilter.nameColor("rare", false, true));
        assertEquals(BlueprintKitFilter.FOUND_UNCOMMON, BlueprintKitFilter.nameColor("uncommon", true, true));
        assertEquals(BlueprintKitFilter.MISSING_UNCOMMON, BlueprintKitFilter.nameColor("uncommon", false, true));
        assertEquals(0xFFFFFFFF, BlueprintKitFilter.nameColor("epic", true, false));
        assertEquals(0xFF888888, BlueprintKitFilter.nameColor("epic", false, false));
    }

    @Test
    void foundStatusUsesTheEntryRarityAndUpdatesWithoutRebuildingMembership() {
        assertTrue(BlueprintKitFilter.isFound("Drachenzahn", "epic", (name, rarity) -> "epic".equals(rarity)));
        assertFalse(BlueprintKitFilter.isFound("Drachenzahn", "legendary", (name, rarity) -> "epic".equals(rarity)));
        assertFalse(BlueprintKitFilter.isFound("Drachenzahn", "common", (name, rarity) -> true));
        assertTrue(BlueprintKitFilter.isFound("Holzschwert", "common", (name, rarity) -> "Holzschwert".equals(name)));

        List<BlueprintKitFilter.Entry> entries = BlueprintKitFilter.kitPage(
                List.of(item("floor_1", "common", "Holzschwert")),
                List.of(part("Holzschwert", null)));
        assertEquals(1, BlueprintKitFilter.filteredVisibleRows(entries.size(), 1, true));
        assertNull(BlueprintKitFilter.placeholder(entries.size(), 1, true));
        assertEquals(1, BlueprintKitFilter.filteredVisibleRows(entries.size(), 0, true));
        assertEquals(BlueprintKitFilter.ALL_FOUND, BlueprintKitFilter.placeholder(entries.size(), 0, true));
        assertEquals(BlueprintKitFilter.NONE, BlueprintKitFilter.placeholder(0, 0, true));
        assertEquals(1, BlueprintKitFilter.filteredVisibleRows(0, 0, false));
    }

    @Test
    void titleWidensTheBoxWhenItIsLongerThanEveryRow() {
        assertEquals(245, BlueprintKitFilter.contentWidth(50, 45, true, 200, List.of(10, 20)));
        assertEquals(180, BlueprintKitFilter.contentWidth(50, 45, true, 40, List.of(100, 135)));
        assertEquals(200, BlueprintKitFilter.contentWidth(200, 45, false, 400, List.of(10)));
    }

    @Test
    void listKeyChangesWhenTheSameKitChangesOrTheCatalogReloads() {
        String sameName = BlueprintKitFilter.listKey("custom|1", "Münz-Kit 1", "", signature("Holzschwert", "epic"), 1);
        String edited = BlueprintKitFilter.listKey("custom|1", "Münz-Kit 1", "", signature("Drachenzahn", "legendary"), 1);
        String reloaded = BlueprintKitFilter.listKey("custom|1", "Münz-Kit 1", "", signature("Holzschwert", "epic"), 2);
        assertFalse(sameName.equals(edited));
        assertFalse(sameName.equals(reloaded));
        assertEquals(
                BlueprintKitFilter.contentSignature(List.of(part("B", "rare"), part("A", null))),
                BlueprintKitFilter.contentSignature(List.of(part("A", null), part("B", "Rare"))));
    }

    private static BlueprintKitFilter.CatalogItem item(String floor, String rarity, String name) {
        return new BlueprintKitFilter.CatalogItem(floor, rarity, name);
    }

    private static BlueprintKitFilter.KitPart part(String name, String rarity) {
        return new BlueprintKitFilter.KitPart(name, rarity);
    }

    private static List<String> names(List<BlueprintKitFilter.Entry> entries) {
        return entries.stream().map(BlueprintKitFilter.Entry::name).toList();
    }

    private static String signature(String name, String rarity) {
        return BlueprintKitFilter.contentSignature(List.of(part(name, rarity)));
    }
}

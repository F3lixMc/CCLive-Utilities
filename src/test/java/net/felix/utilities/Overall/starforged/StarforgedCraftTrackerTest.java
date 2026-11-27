package net.felix.utilities.Overall.starforged;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarforgedCraftTrackerTest {

	@AfterEach
	void reset() {
		StarforgedCraftTracker.reset();
	}

	@Test
	void parsesSingleCraftName() {
		assertEquals("Schattenstahl Leggings",
				StarforgedCraftTracker.craftedName("[Legend] Schattenstahl Leggings erfolgreich hergestellt!"));
		// Chat timestamp prefix and stack counter from client mods are ignored.
		assertEquals("Schattenstahl Leggings", StarforgedCraftTracker.craftedName(
				"§7[§622:16§7] §r[Legend] Schattenstahl Leggings erfolgreich hergestellt! §r[x9]"));
		assertNull(StarforgedCraftTracker.craftedName("[Legend] Welt erfolgreich geladen!"));
		assertNull(StarforgedCraftTracker.craftedName("Spieler >> [Legend] Axt erfolgreich hergestellt!"));
	}

	@Test
	void detectsBlueprintBatch() {
		assertTrue(StarforgedCraftTracker.isBlueprintBatch("[Legend] Du hast den Bauplan erfolgreich 10x gebaut!"));
		assertTrue(StarforgedCraftTracker.isBlueprintBatch(
				"§7[§622:13§7] §r[Legend] Du hast den Bauplan erfolgreich 10x gebaut! §r[x4]"));
		assertFalse(StarforgedCraftTracker.isBlueprintBatch("[Legend] Schmiedezustand erhalten: [Frostgeschmiedet]"));
	}

	@Test
	void recognisesBlueprintInventoryAndCraftAction() {
		assertTrue(StarforgedCraftTracker.isBlueprintInventory("Baupläne [Rüstung]"));
		assertFalse(StarforgedCraftTracker.isBlueprintInventory("Bauplan [Shop]"));
		assertTrue(StarforgedCraftTracker.isCraftAction(List.of("Stufe 3", "[Linksklick]: Herstellen")));
		assertFalse(StarforgedCraftTracker.isCraftAction(List.of("[Linksklick] Kaufen")));
	}

	@Test
	void cleansItemNamesAndMatchesChatName() {
		assertEquals("Schattenstahl Leggings",
				StarforgedCraftTracker.itemName("Schattenstahl Leggings - [Bauplan] [e3]"));
		assertTrue(StarforgedCraftTracker.sameItem("Schattenstahl Leggings - [Bauplan]", "Schattenstahl Leggings"));
		assertFalse(StarforgedCraftTracker.sameItem("Frostaxt - [Bauplan]", "Schattenstahl Leggings"));
	}

	@Test
	void remembersCraftNameOnlyBriefly() {
		StarforgedCraftTracker.recordChat("[Legend] Frostaxt erfolgreich hergestellt!", 1000L);
		assertEquals("Frostaxt", StarforgedCraftTracker.recentCraftName(2000L));
		assertNull(StarforgedCraftTracker.recentCraftName(1000L + StarforgedCraftTracker.CRAFT_MESSAGE_MEMORY_MS + 1));
		// A batch replaces the name, so the clicked blueprint is used instead.
		StarforgedCraftTracker.recordChat("[Legend] Du hast den Bauplan erfolgreich 10x gebaut!", 3000L);
		assertNull(StarforgedCraftTracker.recentCraftName(3100L));
		assertTrue(StarforgedCraftTracker.recentCraftWasBatch(3100L));
		StarforgedCraftTracker.recordChat("[Legend] Frostaxt erfolgreich hergestellt!", 4000L);
		assertFalse(StarforgedCraftTracker.recentCraftWasBatch(4100L));
	}
}

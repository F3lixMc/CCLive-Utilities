package net.felix.utilities.Factory.essencefarm;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EssenceFarmCrystalMenuTest {

	@Test
	void readsLevelAndEssenceCost() {
		var info = EssenceFarmCrystalMenu.parse(List.of(
				"[Machtkristall der Opferung]",
				"Level 57 [||||||||||] Level 58",
				"2094937052/112456950 XP [100%]",
				"",
				"Kosten:",
				" - Coins: (11423865111/65280470)",
				" - Vindicator [Essenz] Tier 3: (46894/59000)",
				" - XP: (2094937052/112456950)")).orElseThrow();
		assertEquals("Opferung", info.name());
		assertEquals(57, info.level());
		assertEquals("Vindicator [Essenz] Tier 3", info.essenceKey());
		assertEquals(46894, info.owned());
		assertEquals(59000, info.required());
	}

	@Test
	void crystalWithoutEssenceCostStillGivesLevel() {
		var info = EssenceFarmCrystalMenu.parse(List.of("[Machtkristall des Kaktus]", "Level 12 [|||] Level 13")).orElseThrow();
		assertEquals("Kaktus", info.name());
		assertEquals(12, info.level());
		assertNull(info.essenceKey());
	}

	@Test
	void otherItemsAreIgnored() {
		assertTrue(EssenceFarmCrystalMenu.parse(List.of("Zurück", "Level 3")).isEmpty());
		assertTrue(EssenceFarmCrystalMenu.parse(List.of("[Machtkristall der Gier]", "Kosten:")).isEmpty());
	}
}

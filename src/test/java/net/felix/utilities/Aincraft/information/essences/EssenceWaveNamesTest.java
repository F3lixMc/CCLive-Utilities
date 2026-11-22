package net.felix.utilities.Aincraft.information.essences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EssenceWaveNamesTest {

	@Test
	void mkLevelNameMapsToEssenceBagKeys() {
		List<String> keys = EssenceWaveNames.storageKeysForMkLevelName("Pferd T1");
		assertTrue(keys.contains("Pferde [Essenz] Tier 1"));
		assertTrue(keys.contains("Pferd [Essenz] Tier 1"));
		assertEquals(List.of(), EssenceWaveNames.storageKeysForMkLevelName("Pferd"));
	}
}

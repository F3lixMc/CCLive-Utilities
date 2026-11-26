package net.felix.utilities.Overall.customtextures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PowerCrystalCatalogTest {

	@Test
	void matchesAllNamesFromCcAddonsPack() {
		assertEquals("kaktus", PowerCrystalCatalog.matchName("[Machtkristall des Kaktus]"));
		assertEquals("mehrfachtreffer", PowerCrystalCatalog.matchName("[Machtkristall der Mehrfachtreffer]"));
		assertEquals("zeit", PowerCrystalCatalog.matchName("[Machtkristall der Zeit]"));
		assertEquals("seelen", PowerCrystalCatalog.matchName("[Machtkristall der Seelen]"));
		assertEquals("opferung", PowerCrystalCatalog.matchName("[Machtkristall der Opferung]"));
		assertEquals("gier", PowerCrystalCatalog.matchName("[Machtkristall der Gier]"));
		assertEquals("materialismus", PowerCrystalCatalog.matchName("[Machtkristall des Materialismus]"));
		assertEquals("schwaeche", PowerCrystalCatalog.matchName("[Machtkristall der Schwäche]"));
		assertEquals("mehrfachtreffer", PowerCrystalCatalog.matchName("[Power Crystal of Multistrike]"));
		assertEquals(8, PowerCrystalCatalog.textureCount());
	}

	@Test
	void toleratesColorsAndMissingBrackets() {
		assertEquals("kaktus", PowerCrystalCatalog.matchName("§f[§bMachtkristall des Kaktus§f]"));
		assertEquals("zeit", PowerCrystalCatalog.matchName("Machtkristall der Zeit"));
	}

	@Test
	void rejectsOtherItems() {
		assertNull(PowerCrystalCatalog.matchName("[Machtkristall der Unbekannten]"));
		assertNull(PowerCrystalCatalog.matchName("Diamant"));
		assertNull(PowerCrystalCatalog.matchName(null));
	}
}

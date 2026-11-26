package net.felix.utilities.Overall.customtextures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CharmCatalogTest {

	private static String charmKey(String name) {
		CharmCatalog.Charm charm = CharmCatalog.matchCharmName(name);
		return charm == null ? null : charm.key();
	}

	private static String fragmentKey(String name) {
		CharmCatalog.Charm charm = CharmCatalog.matchFragmentName(name);
		return charm == null ? null : charm.key();
	}

	@Test
	void catalogHas46UniqueCharms() {
		assertEquals(46, CharmCatalog.all().size());
		Set<String> keys = new HashSet<>();
		Set<String> phrases = new HashSet<>();
		for (CharmCatalog.Charm charm : CharmCatalog.all()) {
			assertTrue(keys.add(charm.key()), charm.key());
			assertTrue(phrases.add(charm.phrase()), charm.phrase());
		}
	}

	@Test
	void everyCharmMatchesItsOwnNames() {
		for (CharmCatalog.Charm charm : CharmCatalog.all()) {
			assertEquals(charm.key(), charmKey(charm.displayName()), charm.displayName());
			assertEquals(charm.key(), fragmentKey(charm.displayName() + " [Fragment]"), charm.phrase());
			assertEquals(charm.key(), charmKey(charm.englishName()), charm.english());
			assertEquals(charm.key(), fragmentKey(charm.englishName() + " [Fragment]"), charm.english());
			assertEquals(charm.key(), fragmentKey(charm.phrase() + "-Talismanfragment"), charm.phrase());
		}
	}

	@Test
	void matchesCharmNamesWithColorsAndVariants() {
		assertEquals("anvilcapacity", charmKey("§6Anhänger der Schmiedekammer"));
		assertEquals("anvilcapacity", charmKey("Talisman der Schmiedekammer"));
		assertEquals("anvilspeed", charmKey("Anhänger des glühenden Hammers"));
		assertEquals("vitality", charmKey("Anhänger des Lebens"));
		assertEquals("health", charmKey("Anhänger der Lebenskraft"));
		// Crafting menu: exact custom_name parts joined as getString() returns them.
		assertEquals("essencedropchance", charmKey("Anhänger der Ätherquelle [Bauplan]"));
		assertEquals("essencedropchance", charmKey("Charm of Aether Source [Blueprint]"));
		assertNull(fragmentKey("Anhänger der Ätherquelle [Bauplan]"));
	}

	@Test
	void matchesFragmentNamesWithCaseEndings() {
		assertEquals("anvilcapacity", fragmentKey("Schmiedekammer-Talismanfragment"));
		assertEquals("anvilexperience", fragmentKey("Meisterschmied-Talismanfragment"));
		assertEquals("anvilspeed", fragmentKey("Glühender Hammer-Talismanfragment"));
		assertEquals("anvilspeed", fragmentKey("Hammer-Talismanfragment"));
		assertEquals("vitality", fragmentKey("Leben-Talismanfragment"));
		assertEquals("lumberjackyieldchance", fragmentKey("Holz-Talismanfragment"));
		assertEquals("furnacecapacity", fragmentKey("Endlose Esse-Talismanfragment"));
		assertEquals("criticalhitdamage", fragmentKey("Todesstoß-Talismanfragment"));
		assertEquals("anvilcapacity", fragmentKey("§dAnhänger der Schmiedekammer §7[Fragment]"));
		assertEquals("strength", fragmentKey("Charm of Titan's Might [Fragment]"));
	}

	@Test
	void rejectsOtherItems() {
		assertNull(charmKey("Gleißender Talisman der Wallwacht"));
		assertNull(charmKey("Anhänger des Himmels"));
		assertNull(charmKey("Schmiedekammer-Talismanfragment"));
		assertNull(charmKey("Diamantschwert"));
		assertNull(charmKey(null));
		assertNull(fragmentKey("Anhänger der Schmiedekammer"));
		assertNull(fragmentKey("Unbekannt-Talismanfragment"));
		assertNull(fragmentKey(""));
		assertNull(fragmentKey("Anhänger des Himmels [Fragment]"));
	}

	/** Every exact name the cc-addons resource pack matches must resolve to the same texture key. */
	@Test
	void matchesAllNamesFromCcAddonsPack() throws java.io.IOException {
		java.io.InputStream in = CharmCatalogTest.class.getResourceAsStream("/charms/cc_addons_names.tsv");
		assertNotNull(in);
		java.util.List<String> lines;
		try (java.io.BufferedReader reader = new java.io.BufferedReader(
				new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
			lines = reader.lines().filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
		}
		assertEquals(186, lines.size());
		for (String line : lines) {
			String[] parts = line.split("\t");
			String actual = parts[1].equals("fragment") ? fragmentKey(parts[2]) : charmKey(parts[2]);
			assertEquals(parts[0], actual, parts[2]);
		}
	}

	@Test
	void onlyConsumptionHasNoTexture() {
		for (CharmCatalog.Charm charm : CharmCatalog.all()) {
			assertEquals(!charm.key().equals("consumption"), charm.textured(), charm.key());
		}
		assertEquals("consumption", charmKey("Anhänger des Verbrauchs"));
	}

	@Test
	void byKeyLooksUpAndHandlesNull() {
		assertNotNull(CharmCatalog.byKey("bossdamage"));
		assertNull(CharmCatalog.byKey("nope"));
		assertNull(CharmCatalog.byKey(null));
	}
}

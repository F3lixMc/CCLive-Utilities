package net.felix.utilities.Overall.customtextures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The backpack charms (Anhänger) of Cactus Clicker and name matching for charm and fragment items.
 * Pure logic, no Minecraft classes, so it can be unit tested.
 *
 * <p>In-game names: "Anhänger der Schmiedekammer" / "Charm of Smithing Chamber"; fragments carry a
 * " [Fragment]" suffix. Matching stays tolerant ("Talisman" instead of "Anhänger", German case endings)
 * so small server-side renames do not break it.
 */
public final class CharmCatalog {

	/**
	 * One charm.
	 * @param key texture key ({@code textures/charms/<key>.png}), stable
	 * @param article "der" or "des"
	 * @param phrase German name part after the article, as in-game
	 * @param english English name part after "Charm of"
	 * @param effect German effect name, empty if unknown
	 * @param textured whether bundled textures exist for this charm
	 */
	public record Charm(String key, String article, String phrase, String english, String effect, boolean textured) {
		/** In-game German name, e.g. "Anhänger der Schmiedekammer". */
		public String displayName() {
			return "Anhänger " + article + " " + phrase;
		}

		/** In-game English name, e.g. "Charm of Smithing Chamber". */
		public String englishName() {
			return "Charm of " + english;
		}
	}

	private static final Map<String, Charm> BY_KEY = new LinkedHashMap<>();

	static {
		add("anvilcapacity", "der", "Schmiedekammer", "Smithing Chamber", "Ambosskapazität");
		add("axedamage", "des", "Spalters", "Splitter", "Axtschaden");
		add("cactusyield", "der", "Dornenblüte", "Thornbloom", "Kaktusertrag");
		add("arrowdamage", "der", "Pfeilspitze", "Arrowhead", "Pfeilschaden");
		add("anvilexperience", "des", "Meisterschmieds", "Master Smith", "Ambosserfahrung");
		add("recyclercapacity", "der", "Last", "Burden", "Recycler-Kapazität");
		add("wastecapacity", "des", "Abfalls", "Waste", "Abfall-Kapazität");
		add("crossbowdamage", "des", "Bolzenmeisters", "Boltmaster", "Armbrustschaden");
		add("characterexperience", "der", "Erkenntnis", "Insight", "Charakter-Erfahrung");
		add("dexterity", "der", "Fingerfertigkeit", "Dexterity", "Geschicklichkeit");
		add("evade", "des", "Windschritts", "Windstep", "Ausweichchance");
		add("bowdamage", "des", "Weitschusses", "Longshot", "Bogenschaden");
		add("criticalhitdamage", "des", "Todesstoßes", "Deathblow", "Kritischer Trefferschaden");
		add("defense", "des", "Bollwerks", "Bulwark", "Verteidigung");
		add("shieldcapacity", "des", "Wächters", "Guardian", "Schildkapazität");
		add("multihit", "der", "Mehrfachentreffer", "Multikill", "Mehrfachtreffer");
		add("coinchance", "des", "Goldhorters", "Gold Hoarder", "Münzchance");
		add("anvilspeed", "des", "Glühenden Hammers", "Glowing Hammer", "Ambossgeschwindigkeit");
		add("researchtime", "der", "Forschung", "Research", "Forschungszeit");
		add("sworddamage", "der", "Klingenmacht", "Blade Power", "Schwertschaden");
		add("smeltingspeed", "der", "Schmelzflamme", "Smoldering Flame", "Schmelzgeschwindigkeit");
		// No bundled texture and unknown effect yet.
		BY_KEY.put("consumption", new Charm("consumption", "des", "Verbrauchs", "Consumption", "", false));
		add("vitality", "des", "Lebens", "Life", "Vitalität");
		add("dropchance", "des", "Sammlers", "Collector", "Dropchance");
		add("shieldspeed", "des", "Aegis", "Aegis", "Schildgeschwindigkeit");
		add("runningspeed", "des", "Sturmritts", "Stormride", "Laufgeschwindigkeit");
		add("essencedropchance", "der", "Ätherquelle", "Aether Source", "Essenz-Dropchance");
		add("lifesteal", "des", "Vampirs", "Vampire", "Lebensentzug");
		add("factoryexperience", "der", "Industrie", "Industry", "Fabrik-Erfahrung");
		add("coinamount", "des", "Goldsegens", "Golden Blessing", "Münzmenge");
		add("criticalhitchance", "des", "Schicksalsschlags", "Fateful Strike", "Kritische Trefferchance");
		add("furnaceefficiency", "des", "Flammenherzens", "Flameheart", "Schmelzofen-Effizienz");
		add("bladedamage", "der", "Scharfenklinge", "Sharpened Blade", "Klingenschaden");
		add("damage", "der", "Vernichtung", "Destruction", "Schaden");
		add("strength", "der", "Titanenkraft", "Titan's Might", "Stärke");
		add("health", "der", "Lebenskraft", "Vitality", "Lebenspunkte");
		add("furnacecapacity", "der", "Endlosen Esse", "Endless Forge", "Schmelzofen-Kapazität");
		add("soulsyield", "der", "Seelenernte", "Soul Harvest", "Seelenertrag");
		add("bossdamage", "der", "Urgewalt", "Primal Force", "Bossschaden");
		add("lumberjackexperience", "des", "Waldhüters", "Forest Warden", "Holzfällererfahrung");
		add("lumberjackyieldchance", "des", "Holzes", "Wood", "Holzertragchance");
		add("miningexperience", "des", "Tiefengräbers", "Deep Digger", "Bergbauerfahrung");
		add("miningyieldchance", "des", "Erzes", "Ore", "Bergbauertragschance");
		add("fishingexperience", "des", "Anglers", "Fisher", "Angelerfahrung");
		// In-game spelling is "Fischerrei".
		add("fishingefficiency", "der", "Fischerrei", "Fishing", "Angeleffizienz");
		add("fishingspeed", "des", "Köders", "Bait", "Angelgeschwindigkeit");
	}

	// Wiki spellings that differ from the in-game names; the cc-addons pack matches both.
	private static final Map<String, String> GERMAN_ALIASES = Map.of(
			"Mehrfachtreffer", "multihit",
			"Forschers", "researchtime",
			"Fischerei", "fishingefficiency");

	private static final String[] CASE_ENDINGS = { "es", "en", "er", "s", "n", "e" };

	private static final Pattern FRAGMENT_SUFFIX = Pattern.compile("^(.+?)\\s*\\[fragment\\]$");
	// Crafting menu shows charms as "Anhänger der X [Bauplan]"; they get the charm texture.
	private static final Pattern BLUEPRINT_SUFFIX = Pattern.compile("^(.+?)\\s*\\[(?:bauplan|blueprint)\\]$");
	// "Anhänger der X", "Talisman des X". Group 1 = X.
	private static final Pattern GERMAN_NAME = Pattern.compile(
			"^(?:.*\\s)?(?:anhänger|talisman)\\s+(?:der|des)\\s+(.+)$");
	private static final Pattern ENGLISH_NAME = Pattern.compile("^(?:.*\\s)?(?:charm|talisman)\\s+of\\s+(?:the\\s+)?(.+)$");
	// Older formats: "X-Talismanfragment", "X Anhängerfragment". Group 1 = X.
	private static final Pattern LEGACY_FRAGMENT = Pattern.compile("^(.+?)[\\s-]*(?:talisman|anhänger)fragment$");

	private CharmCatalog() {
	}

	private static void add(String key, String article, String phrase, String english, String effect) {
		BY_KEY.put(key, new Charm(key, article, phrase, english, effect, true));
	}

	public static List<Charm> all() {
		return Collections.unmodifiableList(new ArrayList<>(BY_KEY.values()));
	}

	public static Charm byKey(String key) {
		return key == null ? null : BY_KEY.get(key);
	}

	/** Charm for a charm item name like "Anhänger der Schmiedekammer", or null. Fragments are rejected. */
	public static Charm matchCharmName(String rawName) {
		String name = clean(rawName);
		if (name.isEmpty() || name.contains("fragment")) {
			return null;
		}
		Matcher blueprint = BLUEPRINT_SUFFIX.matcher(name);
		if (blueprint.matches()) {
			name = blueprint.group(1);
		}
		return matchFullName(name, false);
	}

	/** Charm for a fragment name like "Anhänger der Schmiedekammer [Fragment]", or null. */
	public static Charm matchFragmentName(String rawName) {
		String name = clean(rawName);
		Matcher suffix = FRAGMENT_SUFFIX.matcher(name);
		if (suffix.matches()) {
			return matchFullName(suffix.group(1), true);
		}
		Matcher legacy = LEGACY_FRAGMENT.matcher(name);
		return legacy.matches() ? matchGermanPhrase(legacy.group(1), true) : null;
	}

	private static Charm matchFullName(String name, boolean allowLastWord) {
		Matcher german = GERMAN_NAME.matcher(name);
		if (german.matches()) {
			return matchGermanPhrase(german.group(1), allowLastWord);
		}
		Matcher english = ENGLISH_NAME.matcher(name);
		if (english.matches()) {
			String[] words = words(english.group(1));
			for (Charm charm : BY_KEY.values()) {
				if (String.join(" ", words).equals(String.join(" ", words(charm.english())))) {
					return charm;
				}
			}
		}
		return null;
	}

	/**
	 * Finds the charm whose German phrase matches; with {@code allowLastWord} a single word may also match
	 * the last word of a multi-word phrase ("Hammer" for "Glühenden Hammers") if that is unambiguous.
	 */
	private static Charm matchGermanPhrase(String phrase, boolean allowLastWord) {
		String[] words = words(phrase);
		if (words.length == 0) {
			return null;
		}
		for (Charm charm : BY_KEY.values()) {
			if (wordsMatch(words, words(charm.phrase()))) {
				return charm;
			}
		}
		for (Map.Entry<String, String> alias : GERMAN_ALIASES.entrySet()) {
			if (wordsMatch(words, words(alias.getKey()))) {
				return BY_KEY.get(alias.getValue());
			}
		}
		if (!allowLastWord || words.length != 1) {
			return null;
		}
		Charm found = null;
		for (Charm charm : BY_KEY.values()) {
			String[] own = words(charm.phrase());
			if (own.length > 1 && wordMatches(words[0], own[own.length - 1])) {
				if (found != null) {
					return null;
				}
				found = charm;
			}
		}
		return found;
	}

	private static boolean wordsMatch(String[] a, String[] b) {
		if (a.length != b.length) {
			return false;
		}
		for (int i = 0; i < a.length; i++) {
			if (!wordMatches(a[i], b[i])) {
				return false;
			}
		}
		return true;
	}

	/** True if both words are equal or differ only by a German case ending. */
	static boolean wordMatches(String a, String b) {
		if (a.equals(b) || stem(a).equals(stem(b))) {
			return true;
		}
		// "Leben" vs "Lebens": one word is the other plus an ending.
		for (String ending : CASE_ENDINGS) {
			if (a.equals(b + ending) || b.equals(a + ending)) {
				return true;
			}
		}
		return false;
	}

	/** Strips at most one German case ending; keeps at least three letters. */
	static String stem(String word) {
		for (String ending : CASE_ENDINGS) {
			if (word.length() - ending.length() >= 3 && word.endsWith(ending)) {
				return word.substring(0, word.length() - ending.length());
			}
		}
		return word;
	}

	private static String[] words(String phrase) {
		String cleaned = phrase.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}]+", " ").trim();
		return cleaned.isEmpty() ? new String[0] : cleaned.split(" ");
	}

	/** Lowercase, no color codes, no private-use or CJK glyphs the server uses as icons. */
	static String clean(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "")
				.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF\\uE000-\\uF8FF]", "")
				.replaceAll("\\s+", " ")
				.trim()
				.toLowerCase(Locale.ROOT);
	}
}

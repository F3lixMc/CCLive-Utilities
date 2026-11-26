package net.felix.utilities.Overall.customtextures;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The power crystals (Machtkristalle) of Cactus Clicker, e.g. "[Machtkristall des Kaktus]" on
 * {@code minecraft:diamond}. Pure logic, unit tested. Names from the user's cc-addons pack (2026-11-26).
 */
public final class PowerCrystalCatalog {

	// Normalized name part after "Machtkristall der/des" or "Power Crystal of" -> texture key.
	private static final Map<String, String> BY_PHRASE = new LinkedHashMap<>();

	static {
		BY_PHRASE.put("kaktus", "kaktus");
		BY_PHRASE.put("mehrfachtreffer", "mehrfachtreffer");
		BY_PHRASE.put("zeit", "zeit");
		BY_PHRASE.put("seelen", "seelen");
		BY_PHRASE.put("opferung", "opferung");
		BY_PHRASE.put("gier", "gier");
		BY_PHRASE.put("materialismus", "materialismus");
		BY_PHRASE.put("schwäche", "schwaeche");
		// English names; only Multistrike is confirmed so far.
		BY_PHRASE.put("multistrike", "mehrfachtreffer");
	}

	private static final Pattern NAME = Pattern.compile(
			"^\\[?\\s*(?:machtkristall\\s+(?:der|des)|power crystal of(?: the)?)\\s+(.+?)\\s*\\]?$");

	private PowerCrystalCatalog() {
	}

	/** Texture key for a power crystal name, or null. */
	public static String matchName(String rawName) {
		String name = CharmCatalog.clean(rawName);
		Matcher matcher = NAME.matcher(name);
		return matcher.matches() ? BY_PHRASE.get(matcher.group(1)) : null;
	}

	/** Number of distinct textures. */
	static int textureCount() {
		return (int) BY_PHRASE.values().stream().distinct().count();
	}
}

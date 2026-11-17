package net.felix.utilities.Aincraft.information.essences;

import static net.felix.utilities.Aincraft.information.essences.EssenceDatabase.essencesDatabase;

/**
 * Zuordnung von Essenznamen zu Wellen.
 */
public final class EssenceWaveNames {

	private EssenceWaveNames() {
	}

	/**
	 * MkLevel mob name (without trailing {@code T<n>}) to the mob base string used in Essenz.json names, when they differ.
	 */
	private static String mkLevelMobNameToEssenzMobBase(String mkMobName) {
		if (mkMobName == null) {
			return null;
		}
		switch (mkMobName) {
			case "Zombiepiglin":
				return "Zombifizierter Piglin";
			case "Piglin Brute":
				return "Piglin Grobian";
			case "Ozelot":
				return "Ocelot";
			default:
				return null;
		}
	}

	public static String getWaveDisplayForEssenceName(String essenceJsonName) {
		EssenceInfo info = essencesDatabase.get(essenceJsonName);
		if (info == null || info.wave == null) {
			return "?";
		}
		return String.valueOf(info.wave);
	}

	/**
	 * Wave display for essence bag tooltips: mob base (e.g. "Pferde") + tier number.
	 */
	static String getWaveDisplayForEssenceMobAndTier(String mobBase, String tier) {
		if (mobBase == null || mobBase.isEmpty() || tier == null || tier.isEmpty()) {
			return null;
		}
		
		java.util.LinkedHashSet<String> candidateKeys = new java.util.LinkedHashSet<>();
		candidateKeys.add(mobBase + " [Essenz] Tier " + tier);
		candidateKeys.add(convertToPlural(mobBase) + " [Essenz] Tier " + tier);
		
		for (String key : candidateKeys) {
			EssenceInfo info = essencesDatabase.get(key);
			if (info != null && info.wave != null) {
				return String.valueOf(info.wave);
			}
		}
		
		String mkWave = getWaveDisplayForMkLevelEssence(mobBase + " T" + tier);
		if (mkWave != null && !"?".equals(mkWave)) {
			return mkWave;
		}
		mkWave = getWaveDisplayForMkLevelEssence(convertToPlural(mobBase) + " T" + tier);
		if (mkWave != null && !"?".equals(mkWave)) {
			return mkWave;
		}
		
		return null;
	}

	/**
	 * Wave display for MkLevel essence names (e.g. "Pferd T1"). Returns {@code null} if the name format is invalid.
	 */
	private static boolean isValidMkLevelEssenceName(String mkLevelEssenceName) {
		if (mkLevelEssenceName == null || mkLevelEssenceName.isEmpty()) {
			return false;
		}
		String[] parts = mkLevelEssenceName.trim().split("\\s+");
		return parts.length >= 2 && parts[parts.length - 1].matches("(?i)T\\d+");
	}

	public static String getWaveDisplayForMkLevelEssence(String mkLevelEssenceName) {
		if (!isValidMkLevelEssenceName(mkLevelEssenceName)) {
			return null;
		}
		EssenceInfo info = findEssenceInfoForMkLevelName(mkLevelEssenceName);
		if (info == null || info.wave == null) {
			return "?";
		}
		return String.valueOf(info.wave);
	}

	/**
	 * Finds essence data from MkLevel format (e.g., "Pferd T1", "Piglin Brute T9", "Pferde T8") via Essenz.json keys.
	 */
	private static EssenceInfo findEssenceInfoForMkLevelName(String mkLevelEssenceName) {
		if (mkLevelEssenceName == null || mkLevelEssenceName.isEmpty()) {
			return null;
		}
		
		String[] parts = mkLevelEssenceName.trim().split("\\s+");
		if (parts.length < 2) {
			return null;
		}
		
		String last = parts[parts.length - 1];
		if (!last.matches("(?i)T\\d+")) {
			return null;
		}
		String tierStr = last.replaceFirst("(?i)T", "").trim();
		String mobName = String.join(" ", java.util.Arrays.copyOfRange(parts, 0, parts.length - 1));
		if (mobName.isEmpty()) {
			return null;
		}
		
		java.util.LinkedHashSet<String> candidateKeys = new java.util.LinkedHashSet<>();
		String aliasBase = mkLevelMobNameToEssenzMobBase(mobName);
		if (aliasBase != null) {
			candidateKeys.add(aliasBase + " [Essenz] Tier " + tierStr);
		}
		candidateKeys.add(mobName + " [Essenz] Tier " + tierStr);
		candidateKeys.add(convertToPlural(mobName) + " [Essenz] Tier " + tierStr);
		
		for (String key : candidateKeys) {
			EssenceInfo essenceInfo = essencesDatabase.get(key);
			if (essenceInfo != null) {
				return essenceInfo;
			}
		}
		
		if (mobName.equals("Piglin") || mobName.equals("Pigling")) {
			String swap = mobName.equals("Piglin") ? "Pigling" : "Piglin";
			EssenceInfo altInfo = essencesDatabase.get(swap + " [Essenz] Tier " + tierStr);
			if (altInfo != null) {
				return altInfo;
			}
		}
		
		return null;
	}

	/**
	 * Converts a singular essence name to plural form (matching Essenz.json format)
	 * @param singular The singular name (e.g., "Pferd")
	 * @return The plural form (e.g., "Pferde")
	 */
	private static String convertToPlural(String singular) {
		if (singular == null || singular.isEmpty()) {
			return singular;
		}
		
		// Check if already in plural form (e.g., "Hühner" in MKLevel.json)
		// This handles cases where MKLevel.json already has plural forms
		if (singular.equals("Hühner")) {
			return "Hühner";
		}
		if (singular.equals("Pferde")) {
			return "Pferde";
		}
		
		// Mapping for special cases
		switch (singular) {
			case "Polarbär": return "Polarbär";
			case "Pferd": return "Pferde";
			case "Lohe": return "Lohen";
			case "Ziege": return "Ziegen";
			case "Biene": return "Bienen";
			case "Huhn": return "Hühner";
			case "Piglin": return "Piglin"; // Can be "Piglin" or "Pigling" in Essenz.json
			case "Piglin Brute": return "Piglin Grobian";
			case "Schneemann": return "Schneemann"; // Stays the same
			case "Wächter": return "Wächter"; // Stays the same
			case "Fuchs": return "Fuchs"; // Stays the same
			case "Creeper": return "Creeper"; // Stays the same
			case "Frosch": return "Frosch"; // Stays the same
			case "Vindicator": return "Vindicator"; // Stays the same
			case "Hexe": return "Hexen";
			case "Schildkröte": return "Schildkröten";
			case "Spinne": return "Spinnen";
			case "Axolotl": return "Axolotl"; // Stays the same
			case "Enderman": return "Enderman"; // Stays the same
			case "Zoglin": return "Zoglin"; // Stays the same
			case "Panda": return "Panda"; // Stays the same
			case "Evoker": return "Evoker"; // Stays the same
			case "Ertrunkener": return "Ertrunkener"; // Stays the same
			case "Ocelot": return "Ocelot"; // Stays the same
			case "Zombiepferd": return "Zombiepferde";
			case "Husk": return "Husk"; // Stays the same
			case "Wither": return "Wither"; // Stays the same
			case "Skelett": return "Skelett"; // Stays the same
			case "Witherskelett": return "Witherskelett"; // Stays the same
			case "Stray": return "Stray"; // Stays the same
			case "Zombie": return "Zombie"; // Stays the same
			case "Wolf": return "Wolf"; // Stays the same
			case "Endermite": return "Endermite"; // Stays the same
			case "Hoglin": return "Hoglin"; // Stays the same
			default:
				// For names ending in "e", often just add "n"
				if (singular.endsWith("e")) {
					return singular + "n";
				}
				// For other names, try adding "e" or "en"
				return singular + "e";
		}
	}
}

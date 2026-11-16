package net.felix.utilities.Aincraft.information.materials;


/**
 * Prismarin-Materialien und ihre Schreibweisen.
 */
public final class PrismarinMaterials {

	private PrismarinMaterials() {
	}

	private static final java.util.List<String> PRISMARIN_MATERIAL_NAMES = java.util.List.of(
			"Prismarin-Splitter",
			"Prismarin-Fragment",
			"Prismarinplatten",
			"Prismarin-Muschel",
			"Herz des Meeres"
	);

	private static final java.util.Map<String, String> PRISMARIN_MATERIAL_RARITIES = java.util.Map.of(
			"Prismarin-Splitter", "common",
			"Prismarin-Fragment", "uncommon",
			"Prismarinplatten", "rare",
			"Prismarin-Muschel", "epic",
			"Herz des Meeres", "legendary"
	);

	/** Lookup ohne Bindestriche/Leerzeichen; "Prismarine" wird wie "Prismarin" behandelt. */
	private static final java.util.Map<String, String> PRISMARIN_CANONICAL_BY_KEY = buildPrismarinCanonicalLookup();

	public static boolean isPrismarinMaterial(String materialName) {
		return resolvePrismarinMaterialName(materialName) != null;
	}

	/**
	 * Mappt Chat-/Tooltip-Varianten auf den kanonischen Prismarin-Namen
	 * (z. B. {@code Prismarine-Splitter} → {@code Prismarin-Splitter},
	 * {@code Prismarine-Platten}/{@code Prismarineplatten} → {@code Prismarinplatten}).
	 *
	 * @return kanonischer Name oder {@code null}
	 */
	public static String resolvePrismarinMaterialName(String materialName) {
		if (materialName == null || materialName.isEmpty()) {
			return null;
		}
		return PRISMARIN_CANONICAL_BY_KEY.get(normalizePrismarinLookupKey(materialName));
	}

	private static java.util.Map<String, String> buildPrismarinCanonicalLookup() {
		java.util.Map<String, String> lookup = new java.util.HashMap<>();
		for (String canonical : PRISMARIN_MATERIAL_NAMES) {
			lookup.put(normalizePrismarinLookupKey(canonical), canonical);
		}
		// Häufige Chat-Varianten (Prismarine vs. Prismarin, Bindestrich bei Platten)
		lookup.put(normalizePrismarinLookupKey("Prismarine-Splitter"), "Prismarin-Splitter");
		lookup.put(normalizePrismarinLookupKey("Prismarine-Fragment"), "Prismarin-Fragment");
		lookup.put(normalizePrismarinLookupKey("Prismarine-Platten"), "Prismarinplatten");
		lookup.put(normalizePrismarinLookupKey("Prismarineplatten"), "Prismarinplatten");
		lookup.put(normalizePrismarinLookupKey("Prismarin-Platten"), "Prismarinplatten");
		lookup.put(normalizePrismarinLookupKey("Prismarine-Muschel"), "Prismarin-Muschel");
		return java.util.Collections.unmodifiableMap(lookup);
	}

	private static String normalizePrismarinLookupKey(String name) {
		String cleaned = name.replaceAll("§[0-9a-fk-or]", "").trim().toLowerCase(java.util.Locale.ROOT);
		cleaned = cleaned.replace("prismarine", "prismarin");
		return cleaned.replace("-", "").replace(" ", "");
	}

	static MaterialFloorInfo getPrismarinMaterialFloorInfo(String materialName) {
		String canonical = resolvePrismarinMaterialName(materialName);
		if (canonical == null) {
			return null;
		}
		String rarity = PRISMARIN_MATERIAL_RARITIES.get(canonical);
		if (rarity == null) {
			return null;
		}
		return new MaterialFloorInfo(0, rarity, rarity);
	}
}

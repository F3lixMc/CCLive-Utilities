package net.felix.utilities.Aincraft.information.materials;

import static net.felix.utilities.Aincraft.information.ponds.FishingMaterials.hasPondLocation;

/**
 * Farben und Textformat fuer Material-Fundorte.
 */
public final class MaterialRarityColors {

	private MaterialRarityColors() {
	}

	/**
	 * Gets the custom hex color for a rarity level (Aincraft-Ebenen)
	 */
	public static int getRarityColor(String rarity) {
		if (rarity == null) {
			return 0x808080;
		}
		switch (rarity.toLowerCase()) {
			case "common":
				return 0xFFFFFF; // White
			case "uncommon":
				return 0x1EFC00; // #1EFC00
			case "rare":
				return 0x006FDA; // #006FDA
			case "epic":
				return 0xA134EB; // #A134EB
			case "legendary":
				return 0xFC7E00; // #FC7E00
			case "mob":
				return 0xFFFFFF; // White for mob names
			default:
				return 0x808080; // Gray
		}
	}

	/** Seltenheitsfarben für Fisch-Materialien / Teich-Anzeige */
	public static int getFishMaterialRarityColor(String rarity) {
		if (rarity == null) {
			return 0x808080;
		}
		switch (rarity.toLowerCase()) {
			case "common":
				return 0xFFFFFF; // White
			case "uncommon":
				return 0x54FC54; // #54FC54
			case "rare":
				return 0x5454FC; // #5454FC
			case "epic":
				return 0xA800A8; // #A800A8
			case "legendary":
				return 0xFCA800; // #FCA800
			default:
				return 0x808080; // Gray
		}
	}

	public static int getMaterialLocationRarityColor(MaterialFloorInfo info) {
		if (info == null) {
			return 0x808080;
		}
		if (hasPondLocation(info)) {
			return getFishMaterialRarityColor(info.rarity);
		}
		return getRarityColor(info.rarity);
	}

	public static int getMaterialLocationRarityColorArgb(MaterialFloorInfo info) {
		return 0xFF000000 | (getMaterialLocationRarityColor(info) & 0xFFFFFF);
	}

	/** Suffix für Material-Zeilen: {@code (Ebene X)} oder {@code (Teich1, Teich2)} */
	public static String formatMaterialLocationSuffix(MaterialFloorInfo info) {
		if (info == null) {
			return "";
		}
		if (info.ponds != null && !info.ponds.isEmpty()) {
			return " (" + String.join(", ", info.ponds) + ")";
		}
		if (info.pond != null && !info.pond.isEmpty()) {
			return " (" + info.pond + ")";
		}
		if (info.floor > 0) {
			return " (Ebene " + info.floor + ")";
		}
		return "";
	}
}

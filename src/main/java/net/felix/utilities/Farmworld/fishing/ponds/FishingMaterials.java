package net.felix.utilities.Farmworld.fishing.ponds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.felix.utilities.Aincraft.materials.data.MaterialFloorInfo;
import net.minecraft.network.chat.Component;

import static net.felix.utilities.Aincraft.materials.data.MaterialRarityColors.formatMaterialLocationSuffix;
import static net.felix.utilities.Aincraft.materials.data.MaterialRarityColors.getFishMaterialRarityColor;
import static net.felix.utilities.Farmworld.fishing.ponds.PondDatabase.fishFamilyDisplayOrderIndex;
import static net.felix.utilities.Farmworld.fishing.ponds.PondDatabase.fishFamilyToPonds;
import static net.felix.utilities.Farmworld.fishing.ponds.PondDatabase.sortedFishFamilies;

/**
 * Fisch-Materialien: Familien, Raritaet und Fundort-Zusatz in Tooltips.
 */
public final class FishingMaterials {

	private FishingMaterials() {
	}

	/** Fisch-Materialien: „Legendäres Thunfischfleisch“, „Gewöhnliche Oktopuswirbelsäule“, … */
	public static final Pattern FISHING_RARITY_MATERIAL_PATTERN = Pattern.compile(
		"^(Gewöhnlich|Ungewöhnlich|Selten|Episch|Legendär)[a-zäöüß]*\\s+.+",
		Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
	);

	private static final Pattern FISHING_RARITY_PREFIX_PATTERN = Pattern.compile(
		"^(Gewöhnlich|Ungewöhnlich|Selten|Episch|Legendär)[a-zäöüß]*\\s+(.+)$",
		Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
	);

	private static final Pattern LEADING_FRACTION_AMOUNT_PATTERN = Pattern.compile(
		"^\\d+(?:[.,]\\d+)?\\s*/\\s*\\d+(?:[.,]\\d+)?\\s+"
	);

	private static final Pattern TRAILING_NUMERIC_AMOUNT_PATTERN = Pattern.compile(
		"\\s*\\([\\d.,]+\\)\\s*$"
	);

	public static boolean isFishingRarityMaterial(String materialName) {
		return materialName != null && FISHING_RARITY_MATERIAL_PATTERN.matcher(materialName.trim()).matches();
	}

	public static String getFishFamilyFromMaterial(String materialName) {
		return extractFishFamilyFromMaterial(materialName);
	}

	public static int getFishFamilyDisplayOrderIndex(String family) {
		if (family == null || family.isEmpty()) {
			return Integer.MAX_VALUE;
		}
		return fishFamilyDisplayOrderIndex.getOrDefault(family, Integer.MAX_VALUE);
	}

	public static String getFishPartSuffixFromMaterial(String materialName, String family) {
		if (materialName == null || materialName.isEmpty()) {
			return "";
		}
		Matcher matcher = FISHING_RARITY_PREFIX_PATTERN.matcher(materialName.trim());
		if (!matcher.matches()) {
			return materialName.toLowerCase(java.util.Locale.ROOT);
		}
		String materialPart = matcher.group(2);
		if (family != null && !family.isEmpty()
				&& materialPart.length() >= family.length()
				&& materialPart.regionMatches(true, 0, family, 0, family.length())) {
			return materialPart.substring(family.length()).toLowerCase(java.util.Locale.ROOT);
		}
		return materialPart.toLowerCase(java.util.Locale.ROOT);
	}

	public static String getFishingMaterialRarityKey(String materialName) {
		return getRarityKeyFromFishingMaterial(materialName);
	}

	private static String extractFishingMaterialNameFromLine(String cleanLineText) {
		if (cleanLineText == null || cleanLineText.isEmpty()) {
			return null;
		}

		String text = cleanLineText;
		if (text.contains("[")) {
			text = text.substring(0, text.indexOf("[")).trim();
		}

		text = TRAILING_NUMERIC_AMOUNT_PATTERN.matcher(text).replaceAll("").trim();
		text = LEADING_FRACTION_AMOUNT_PATTERN.matcher(text).replaceFirst("").trim();

		return isFishingRarityMaterial(text) ? text : null;
	}

	private static String extractFishFamilyFromMaterial(String materialName) {
		Matcher matcher = FISHING_RARITY_PREFIX_PATTERN.matcher(materialName.trim());
		if (!matcher.matches()) {
			return null;
		}

		String materialPart = matcher.group(2);
		for (String family : sortedFishFamilies) {
			if (materialPart.length() >= family.length()
					&& materialPart.regionMatches(true, 0, family, 0, family.length())) {
				return family;
			}
		}
		return null;
	}

	private static String getRarityKeyFromFishingMaterial(String materialName) {
		String lower = materialName.trim().toLowerCase();
		if (lower.startsWith("gewöhnlich")) {
			return "common";
		}
		if (lower.startsWith("ungewöhnlich")) {
			return "uncommon";
		}
		if (lower.startsWith("selten")) {
			return "rare";
		}
		if (lower.startsWith("episch")) {
			return "epic";
		}
		if (lower.startsWith("legendär")) {
			return "legendary";
		}
		return "common";
	}

	public static MaterialFloorInfo getFishingMaterialFloorInfo(String materialName) {
		if (!isFishingRarityMaterial(materialName)) {
			return null;
		}

		String family = extractFishFamilyFromMaterial(materialName);
		if (family == null) {
			return null;
		}

		Set<String> pondSet = fishFamilyToPonds.get(family);
		if (pondSet == null || pondSet.isEmpty()) {
			return null;
		}

		List<String> ponds = new ArrayList<>(pondSet);
		Collections.sort(ponds);
		String rarity = getRarityKeyFromFishingMaterial(materialName);
		return new MaterialFloorInfo(0, null, ponds, rarity, rarity);
	}

	public static boolean hasPondLocation(MaterialFloorInfo info) {
		return info != null
			&& ((info.ponds != null && !info.ponds.isEmpty())
				|| (info.pond != null && !info.pond.isEmpty()));
	}

	private static boolean lineAlreadyHasPondSuffix(String cleanLineText, MaterialFloorInfo info) {
		if (info.ponds != null) {
			for (String pond : info.ponds) {
				if (cleanLineText.contains(pond)) {
					return true;
				}
			}
		}
		if (info.pond != null && cleanLineText.contains(info.pond)) {
			return true;
		}
		return false;
	}

	public static void appendFishingMaterialLocationToLine(
			List<Component> lines,
			int lineIndex,
			Component line,
			String cleanLineText,
			boolean isSpecialInventory,
			boolean isMaterialBagInventory) {
		if (isMaterialBagInventory && lineIndex == 0) {
			return;
		}
		if (isSpecialInventory && lineIndex > 0) {
			return;
		}

		String fishingMaterialName = extractFishingMaterialNameFromLine(cleanLineText);
		if (fishingMaterialName == null) {
			return;
		}

		MaterialFloorInfo info = getFishingMaterialFloorInfo(fishingMaterialName);
		if (!hasPondLocation(info) || lineAlreadyHasPondSuffix(cleanLineText, info)) {
			return;
		}

		String suffix = formatMaterialLocationSuffix(info);
		if (suffix.isEmpty()) {
			return;
		}

		int color = getFishMaterialRarityColor(info.rarity);
		Component locationText = Component.literal(suffix).withStyle(style -> style.withColor(color));
		lines.set(lineIndex, line.copy().append(locationText));
	}
}

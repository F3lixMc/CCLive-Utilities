package net.felix.utilities.Aincraft.fishing.ponds;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

import static net.felix.utilities.Aincraft.fishing.ponds.PondDatabase.pondsDatabase;
import static net.felix.utilities.Aincraft.information.tooltip.TooltipTextCleaner.cleanTooltipLineForMatching;
import static net.felix.utilities.Aincraft.materials.data.MaterialRarityColors.getRarityColor;

/**
 * Teich-Zuordnung in Schnellreise-Tooltips.
 */
public final class PondTooltip {

	private PondTooltip() {
	}

	/**
	 * Sucht in Tooltip-Zeilen nach Biom-Namen und fügt die Teich-Zuordnung hinzu.
	 */
	public static void checkForPondInformation(List<Component> lines) {
		if (pondsDatabase.isEmpty()) {
			return;
		}

		List<PondInfo> sortedPonds = new ArrayList<>(pondsDatabase);
		sortedPonds.sort((a, b) -> Integer.compare(b.biome.length(), a.biome.length()));

		for (int i = 0; i < lines.size(); i++) {
			String cleanLineText = cleanTooltipLineForMatching(lines.get(i).getString());
			if (cleanLineText.isEmpty()) {
				continue;
			}

			for (PondInfo pondInfo : sortedPonds) {
				if (!tooltipLineContainsBiome(cleanLineText, pondInfo.biome)) {
					continue;
				}

				String pondDisplay = "-> " + pondInfo.pond;
				if (tooltipAlreadyContainsPondLine(lines, pondInfo.pond)) {
					return;
				}

				int rareColor = getRarityColor("rare");
				Component pondText = Component.literal(pondDisplay)
						.withStyle(style -> style.withColor(rareColor));
				lines.add(i + 1, pondText);
				return;
			}
		}
	}

	private static boolean tooltipLineContainsBiome(String cleanLineText, String biome) {
		if (biome == null || biome.isEmpty() || cleanLineText == null || cleanLineText.isEmpty()) {
			return false;
		}

		String normalizedBiome = normalizePondMatchText(biome);
		String normalizedLine = normalizePondMatchText(cleanLineText);

		if (normalizedLine.contains("[" + normalizedBiome + "]")) {
			return true;
		}
		if (normalizedLine.contains(normalizedBiome)) {
			return true;
		}

		java.util.regex.Matcher bracketMatcher = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]").matcher(normalizedLine);
		while (bracketMatcher.find()) {
			String bracketContent = normalizePondMatchText(bracketMatcher.group(1));
			if (bracketContent.equals(normalizedBiome)) {
				return true;
			}
		}

		return false;
	}

	private static String normalizePondMatchText(String text) {
		if (text == null) {
			return "";
		}
		return text.replaceAll("\\s+", " ").trim().toLowerCase();
	}

	private static boolean tooltipAlreadyContainsPondLine(List<Component> lines, String pondName) {
		for (Component line : lines) {
			String text = cleanTooltipLineForMatching(line.getString());
			if (text.contains("-> " + pondName)) {
				return true;
			}
		}
		return false;
	}
}

package net.felix.features.information.farmzone;

import java.util.ArrayList;
import java.util.List;
import net.felix.utilities.Overall.hud.ScoreboardSidebarReader;
import net.minecraft.client.Minecraft;

import static net.felix.features.information.collections.CollectionTracker.biomDetected;
import static net.felix.features.information.collections.CollectionTracker.checkCollectionBiomChange;
import static net.felix.features.information.collections.CollectionTracker.lastScoreboardCheck;

/**
 * Erkennt Farmzone und Biom ueber die Scoreboard-Seitenleiste.
 */
public final class FarmzoneScoreboard {

	private FarmzoneScoreboard() {
	}

	/**
	 * Farmzone = Farmwelt ({@code minecraft:overworld}) + Cactus-Clicker-Scoreboard mit erkanntem Biom.
	 * Gleiche Logik wie beim Collection-Overlay.
	 */
	public static boolean isInFarmzone(Minecraft client) {
		return isInFarmworldDimension(client) && biomDetected;
	}

	/**
	 * Aktualisiert den Biom-Cache vom Scoreboard (sofort, ohne Rate-Limit).
	 */
	public static void refreshFarmzoneScoreboardCache(Minecraft client) {
		if (client != null && isInFarmworldDimension(client)) {
			lastScoreboardCheck = 0L;
			checkCollectionBiomChange(client);
		}
	}

	/**
	 * Checks if player is in farmworld dimension
	 */
	public static boolean isInFarmworldDimension(Minecraft client) {
		try {
			if (client == null || client.level == null) {
				return false;
			}
			
			String dimensionId = client.level.dimension().identifier().toString().toLowerCase();
			// Farmworld is usually minecraft:overworld
			return dimensionId.equals("minecraft:overworld");
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Reads all scoreboard lines from sidebar in order (top to bottom).
	 * Nutzt dieselbe Team-/Display-Auflösung wie {@link ScoreboardSidebarReader} (26.1 API).
	 */
	public static List<String> readScoreboardLines(net.minecraft.world.scores.Scoreboard scoreboard, 
			net.minecraft.world.scores.Objective sidebarObjective) {
		List<String> lines = new ArrayList<>();
		try {
			for (ScoreboardSidebarReader.Row row : ScoreboardSidebarReader.readRows(scoreboard, sidebarObjective)) {
				if (row == null || row.name() == null) {
					continue;
				}
				lines.add(stripScoreboardDisplayText(row.name().getString()));
			}
		} catch (Exception e) {
			// Silent error handling
		}
		return lines;
	}

	/** Entfernt §-Codes und Resourcepack-Glyphs (CJK/PUA), die im Scoreboard-Text stecken können. */
	public static String stripScoreboardDisplayText(String text) {
		if (text == null) {
			return "";
		}
		return text
				.replaceAll("§[0-9a-fk-or]", "")
				.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF\\uE000-\\uF8FF]", "")
				.trim();
	}

	/**
	 * Sidebar-Zeilen wie im HUD (ohne §-Codes), für andere Utilities (z. B. npcAlerts Kombo-Kiste).
	 */
	public static List<String> readCleanSidebarLines(Minecraft client) {
		List<String> out = new ArrayList<>();
		if (client == null || client.level == null) {
			return out;
		}
		try {
			net.minecraft.world.scores.Scoreboard scoreboard = client.level.getScoreboard();
			net.minecraft.world.scores.Objective sidebarObjective =
				scoreboard.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR);
			if (sidebarObjective == null) {
				return out;
			}
			return readScoreboardLines(scoreboard, sidebarObjective);
		} catch (Exception e) {
			return out;
		}
	}

	/**
	 * Helper: Checks if a line is empty (only whitespace/formatting)
	 */
	public static boolean isEmptyLine(String s) {
		if (s == null) return true;
		return stripScoreboardDisplayText(s).isEmpty();
	}

	/**
	 * Helper: Checks if a line is the "Biom" label
	 * Can be "Biom" or "▌ Biom"
	 */
	private static boolean isBiomLabel(String s) {
		if (s == null) return false;
		String clean = stripScoreboardDisplayText(s);
		// Remove common prefix symbols if present
		clean = clean.replace("▌", "").replace("|", "").trim();
		return clean.equalsIgnoreCase("Biom");
	}

	/**
	 * Helper: Checks if a line starts with arrow (-> or ➥)
	 */
	private static boolean isArrowLine(String s) {
		if (s == null) return false;
		String clean = stripScoreboardDisplayText(s);
		return clean.startsWith("->") || clean.startsWith("➥");
	}

	/**
	 * Helper: Extracts text after arrow (-> or ➥)
	 * Removes square brackets if present: "[Kohle Mine]" -> "Kohle Mine"
	 */
	private static String extractAfterArrow(String s) {
		if (s == null) return null;
		String clean = stripScoreboardDisplayText(s);
		
		// Find arrow position
		int arrowIdx = -1;
		if (clean.startsWith("➥")) {
			arrowIdx = 0;
			clean = clean.substring(1).trim();
		} else {
			int idx = clean.indexOf("->");
			if (idx != -1) {
				arrowIdx = idx;
				clean = clean.substring(idx + 2).trim();
			}
		}
		
		if (arrowIdx == -1) return clean;
		
		// Remove square brackets if present
		if (clean.startsWith("[") && clean.endsWith("]")) {
			clean = clean.substring(1, clean.length() - 1).trim();
		}
		
		return clean;
	}

	/**
	 * Extracts biom name from scoreboard lines
	 */
	public static String extractBiomFromLines(List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (isBiomLabel(line)) {
				// Look for next non-empty line with arrow
				for (int j = i + 1; j < lines.size(); j++) {
					String next = lines.get(j);
					if (isEmptyLine(next)) continue;
					if (isArrowLine(next)) {
						return extractAfterArrow(next);
					}
				}
			}
		}
		return null;
	}
}

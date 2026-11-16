package net.felix.features.information.mklevel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.felix.features.information.essences.EssenceWaveNames.getWaveDisplayForMKLevelEssence;
import static net.felix.features.information.mklevel.MKLevelState.mkLevelTotalEssencesCache;
import static net.felix.features.information.util.JsonConfigFiles.getConfigFilePath;
import static net.felix.features.information.util.NumberFormatting.formatNumberWithSeparator;


/**
 * MK-Level-Daten (MKLevel.json) und Filter fuer die Anzeige.
 */
public final class MKLevelDatabase {

	private MKLevelDatabase() {
	}

	// MKLevel tracking - List of level data from MKLevel.json
	private static List<MKLevelInfo> mkLevelDatabase = new ArrayList<>();

	private static List<CombinedWaveInfo> mkLevelCombinedWavesDatabase = new ArrayList<>();

	/**
	 * Loads the MKLevel database from MKLevel.json
	 */
	public static void loadMKLevelDatabase() {
		try {
			// Versuche zuerst lokale Datei zu laden, sonst Mod-Ressourcen
			java.nio.file.Path resource = getConfigFilePath("MKLevel.json");
			if (resource == null) {
				return;
			}
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
					
					// Load levels array
					if (root.has("levels") && root.get("levels").isJsonArray()) {
						com.google.gson.JsonArray levelsArray = root.getAsJsonArray("levels");
						
						for (var element : levelsArray) {
							JsonObject levelData = element.getAsJsonObject();
							int level = levelData.get("Level").getAsInt();
							String essence = levelData.get("Essence").getAsString();
							int amount = levelData.get("Amount").getAsInt();
							
							mkLevelDatabase.add(new MKLevelInfo(level, essence, amount));
						}
					}
					
					// Load combined_waves array
					if (root.has("combined_waves") && root.get("combined_waves").isJsonArray()) {
						com.google.gson.JsonArray combinedWavesArray = root.getAsJsonArray("combined_waves");
						
						for (var element : combinedWavesArray) {
							JsonObject waveData = element.getAsJsonObject();
							String level = waveData.get("Level").getAsString();
							int wave = waveData.get("Wave").getAsInt();
							
							// Parse "without" array if present
							List<String> without = new ArrayList<>();
							if (waveData.has("without")) {
								if (waveData.get("without").isJsonArray()) {
									com.google.gson.JsonArray withoutArray = waveData.getAsJsonArray("without");
									for (var withoutElement : withoutArray) {
										String withoutStr = withoutElement.getAsString();
										// Split by comma in case a single array element contains multiple essences
										String[] parts = withoutStr.split(",");
										for (String part : parts) {
											String trimmed = part.trim();
											if (!trimmed.isEmpty()) {
												without.add(trimmed);
											}
										}
									}
								} else if (waveData.get("without").isJsonPrimitive()) {
									// Handle case where "without" is a single string (comma-separated)
									String withoutStr = waveData.get("without").getAsString();
									String[] parts = withoutStr.split(",");
									for (String part : parts) {
										String trimmed = part.trim();
										if (!trimmed.isEmpty()) {
											without.add(trimmed);
										}
									}
								}
							}
							
							mkLevelCombinedWavesDatabase.add(new CombinedWaveInfo(level, wave, without));
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load MKLevel database: " + e.getMessage());
			// Silent error handling
		}
	}

	/**
	 * Filters MKLevel entries based on search text
	 * Searches in Level, Essence, and Amount
	 */
	private static List<TotalEssenceInfo> getMKLevelTotalEssences() {
		if (mkLevelTotalEssencesCache != null) {
			return mkLevelTotalEssencesCache;
		}
		
		java.util.LinkedHashMap<String, Integer> totals = new java.util.LinkedHashMap<>();
		for (MKLevelInfo entry : mkLevelDatabase) {
			totals.merge(entry.essence, entry.amount, Integer::sum);
		}
		
		List<TotalEssenceInfo> result = new ArrayList<>();
		for (java.util.Map.Entry<String, Integer> entry : totals.entrySet()) {
			result.add(new TotalEssenceInfo(entry.getKey(), entry.getValue()));
		}
		
		result.sort((a, b) -> {
			int waveA = mkLevelParseWaveSortKey(getWaveDisplayForMKLevelEssence(a.essence));
			int waveB = mkLevelParseWaveSortKey(getWaveDisplayForMKLevelEssence(b.essence));
			if (waveA != waveB) {
				return Integer.compare(waveA, waveB);
			}
			return a.essence.compareToIgnoreCase(b.essence);
		});
		
		mkLevelTotalEssencesCache = result;
		return result;
	}

	private static int mkLevelParseWaveSortKey(String waveDisplay) {
		if (waveDisplay == null || "?".equals(waveDisplay)) {
			return Integer.MAX_VALUE;
		}
		try {
			return Integer.parseInt(waveDisplay);
		} catch (NumberFormatException e) {
			return Integer.MAX_VALUE;
		}
	}

	static List<TotalEssenceInfo> filterTotalEssenceEntries(String searchText) {
		List<TotalEssenceInfo> all = getMKLevelTotalEssences();
		if (searchText == null || searchText.trim().isEmpty()) {
			return all;
		}
		
		String searchLower = searchText.toLowerCase().trim();
		Integer waveFromSearch = null;
		boolean isWaveOnlySearch = false;
		java.util.regex.Pattern wavePattern = java.util.regex.Pattern.compile("welle\\s*:?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
		java.util.regex.Matcher waveMatcher = wavePattern.matcher(searchText);
		if (waveMatcher.find()) {
			try {
				waveFromSearch = Integer.parseInt(waveMatcher.group(1));
			} catch (NumberFormatException e) {
				// Ignore
			}
		} else if (searchLower.startsWith("welle") || searchLower.equals("welle:")) {
			isWaveOnlySearch = true;
		}
		
		List<TotalEssenceInfo> filtered = new ArrayList<>();
		for (TotalEssenceInfo entry : all) {
			boolean matches = false;
			
			if (isWaveOnlySearch) {
				matches = true;
			} else if (waveFromSearch != null) {
				String waveDisplay = getWaveDisplayForMKLevelEssence(entry.essence);
				if (waveDisplay != null && !"?".equals(waveDisplay) && waveDisplay.contains(String.valueOf(waveFromSearch))) {
					matches = true;
				}
			} else {
				if (entry.essence.toLowerCase().contains(searchLower)) {
					matches = true;
				}
				if (!matches) {
					String amountStr = formatNumberWithSeparator(entry.totalAmount);
					if (amountStr.contains(searchLower) || String.valueOf(entry.totalAmount).contains(searchLower)) {
						matches = true;
					}
				}
				if (!matches) {
					String waveDisplay = getWaveDisplayForMKLevelEssence(entry.essence);
					if (waveDisplay != null && waveDisplay.toLowerCase().contains(searchLower)) {
						matches = true;
					}
				}
			}
			
			if (matches) {
				filtered.add(entry);
			}
		}
		
		return filtered;
	}

	static List<MKLevelInfo> filterMKLevelEntries(String searchText) {
		if (searchText == null || searchText.trim().isEmpty()) {
			return mkLevelDatabase;
		}
		
		String searchLower = searchText.toLowerCase().trim();
		List<MKLevelInfo> filtered = new ArrayList<>();
		
		// Check if search text contains "level" followed by a number (e.g., "Level 10", "level10", "level 10", "Level: 10")
		Integer levelFromSearch = null;
		boolean isLevelOnlySearch = false;
		// Pattern matches: "level" (case-insensitive), optional ":", optional whitespace, then digits
		java.util.regex.Pattern levelPattern = java.util.regex.Pattern.compile("level\\s*:?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
		java.util.regex.Matcher levelMatcher = levelPattern.matcher(searchText);
		if (levelMatcher.find()) {
			try {
				levelFromSearch = Integer.parseInt(levelMatcher.group(1));
			} catch (NumberFormatException e) {
				// Ignore
			}
		} else {
			// Check if search text starts with "level" (for live search, e.g., "lev", "leve", "level", "level:")
			if (searchLower.startsWith("level") || searchLower.equals("level:")) {
				isLevelOnlySearch = true;
			}
		}
		
		// Check if search text contains "welle" followed by a number (e.g., "Welle 701", "welle: 701", "welle701")
		Integer waveFromSearch = null;
		boolean isWaveOnlySearch = false;
		// Pattern matches: "welle" (case-insensitive), optional ":", optional whitespace, then digits
		java.util.regex.Pattern wavePattern = java.util.regex.Pattern.compile("welle\\s*:?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
		java.util.regex.Matcher waveMatcher = wavePattern.matcher(searchText);
		if (waveMatcher.find()) {
			try {
				waveFromSearch = Integer.parseInt(waveMatcher.group(1));
			} catch (NumberFormatException e) {
				// Ignore
			}
		} else {
			// Check if search text starts with "welle" (for live search, e.g., "wel", "welle", "welle:")
			if (searchLower.startsWith("welle") || searchLower.equals("welle:")) {
				isWaveOnlySearch = true;
			}
		}
		
		for (MKLevelInfo entry : mkLevelDatabase) {
			boolean matches = false;
			
			// If search text starts with "Level" (for live search), show all entries
			if (isLevelOnlySearch) {
				matches = true;
			}
			// If search text starts with "Welle" (for live search), show all entries
			else if (isWaveOnlySearch) {
				matches = true;
			}
			// Search for "Welle X" pattern - search for waves containing the number as substring
			else if (waveFromSearch != null) {
				String waveDisplay = getWaveDisplayForMKLevelEssence(entry.essence);
				if (waveDisplay != null && !"?".equals(waveDisplay)) {
					String searchWaveStr = String.valueOf(waveFromSearch);
					if (waveDisplay.contains(searchWaveStr)) {
						matches = true;
					}
				}
			}
			// Search for "Level X" pattern (prioritize exact level match)
			else if (levelFromSearch != null && entry.level == levelFromSearch) {
				matches = true;
			}
			// General search: check all fields for matches
			else {
				// Search in displayed text "-Level X" - check if search text matches any part of the displayed text
				String displayedLevelText = "-Level " + entry.level;
				if (displayedLevelText.toLowerCase().contains(searchLower)) {
					matches = true;
				}
				
				// Search in Level (as number) - only if not already matched
				if (!matches && String.valueOf(entry.level).contains(searchLower)) {
					matches = true;
				}
				
				// Search in Essence
				if (!matches && entry.essence.toLowerCase().contains(searchLower)) {
					matches = true;
				}
				
				// Search in Amount
				if (!matches) {
					String amountStr = formatNumberWithSeparator(entry.amount);
					if (amountStr.contains(searchLower) || String.valueOf(entry.amount).contains(searchLower)) {
						matches = true;
					}
				}
				
				// Search in Wave display (number or "?")
				if (!matches) {
					String waveDisplay = getWaveDisplayForMKLevelEssence(entry.essence);
					if (waveDisplay != null && waveDisplay.toLowerCase().contains(searchLower)) {
						matches = true;
					}
				}
			}
			
			if (matches) {
				filtered.add(entry);
			}
		}
		
		return filtered;
	}

	/**
	 * Filters combined waves entries based on search text
	 * Searches in Level, Wave, and Without fields
	 */
	static List<CombinedWaveInfo> filterCombinedWaves(String searchText) {
		if (searchText == null || searchText.trim().isEmpty()) {
			return mkLevelCombinedWavesDatabase;
		}
		
		String searchLower = searchText.toLowerCase().trim();
		List<CombinedWaveInfo> filtered = new ArrayList<>();
		
		// Check if search text contains "welle" followed by a number
		Integer waveFromSearch = null;
		boolean isWaveOnlySearch = false;
		java.util.regex.Pattern wavePattern = java.util.regex.Pattern.compile("welle\\s*:?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
		java.util.regex.Matcher waveMatcher = wavePattern.matcher(searchText);
		if (waveMatcher.find()) {
			try {
				waveFromSearch = Integer.parseInt(waveMatcher.group(1));
			} catch (NumberFormatException e) {
				// Ignore
			}
		} else {
			// Check if search text starts with "welle" (for live search)
			if (searchLower.startsWith("welle") || searchLower.equals("welle:")) {
				isWaveOnlySearch = true;
			}
		}
		
		for (CombinedWaveInfo entry : mkLevelCombinedWavesDatabase) {
			boolean matches = false;
			
			// If search text starts with "Welle" (for live search), show all entries
			if (isWaveOnlySearch) {
				matches = true;
			}
			// Search for "Welle X" pattern - search for waves containing the number as substring
			else if (waveFromSearch != null) {
				String waveStr = String.valueOf(entry.wave);
				String searchWaveStr = String.valueOf(waveFromSearch);
				if (waveStr.contains(searchWaveStr)) {
					matches = true;
				}
			}
			// General search: check all fields for matches
			else {
				// Search in Level (e.g., "1-13", "25+30")
				if (entry.level.toLowerCase().contains(searchLower)) {
					matches = true;
				}
				
				// Search in Wave (as number)
				if (!matches && String.valueOf(entry.wave).contains(searchLower)) {
					matches = true;
				}
				
				// Search in Without
				if (!matches && entry.without != null) {
					for (String withoutItem : entry.without) {
						if (withoutItem.toLowerCase().contains(searchLower)) {
							matches = true;
							break;
						}
					}
				}
			}
			
			if (matches) {
				filtered.add(entry);
			}
		}
		
		return filtered;
	}

	/**
	 * Lädt die MKLevel-Datenbank neu (nach Update vom Server)
	 */
	public static void reloadMKLevelDatabase() {
		mkLevelDatabase.clear();
		mkLevelCombinedWavesDatabase.clear();
		mkLevelTotalEssencesCache = null;
		loadMKLevelDatabase();
	}
}

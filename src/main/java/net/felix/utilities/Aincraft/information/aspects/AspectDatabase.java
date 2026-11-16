package net.felix.utilities.Aincraft.information.aspects;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;

import static net.felix.utilities.Aincraft.information.util.JsonConfigFiles.getConfigFilePath;

/**
 * Aspekt-Datenbank (Aspekte.json) und Namensauflosung.
 */
public final class AspectDatabase {

	private AspectDatabase() {
	}

	// Aspect tracking
	public static Map<String, AspectInfo> aspectsDatabase = new HashMap<>();

	private static Map<String, AspectInfo> tooltipAspectDisplayNameLookup = new HashMap<>();

	/**
	 * Gets the aspect name for a blueprint item name
	 * @param blueprintName The cleaned blueprint name (without "[Bauplan]")
	 * @return The aspect name, or null if not found
	 */
	public static String getAspectInfoForBlueprint(String blueprintName) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			return null;
		}
		
		// Remove any remaining formatting
		String cleanName = blueprintName.replaceAll("§[0-9a-fk-or]", "");
		cleanName = cleanName.replaceAll("[\\u3400-\\u4DBF]", "");
		cleanName = cleanName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		
		AspectInfo aspectInfo = aspectsDatabase.get(cleanName);
		if (aspectInfo != null) {
			return aspectInfo.aspectName;
		}
		
		return null;
	}

	/**
	 * Liest den Aspekt-Namen aus einer ⭐-Tooltip-Zeile, z. B. "[Mond Aspekt]" aus
	 * "⭐ [Mond Aspekt] (Transferiert)".
	 */
	public static String extractBracketAspectNameFromStarLine(String lineText) {
		if (lineText == null || lineText.isEmpty()) {
			return null;
		}
		String cleanLine = lineText.replaceAll("§[0-9a-fk-or]", "");
		int starIndex = cleanLine.indexOf("⭐");
		int searchFrom = starIndex >= 0 ? starIndex : 0;
		int start = cleanLine.indexOf('[', searchFrom);
		if (start < 0) {
			return null;
		}
		int end = cleanLine.indexOf(']', start);
		if (end <= start) {
			return null;
		}
		String aspectName = cleanLine.substring(start + 1, end).trim();
		if (aspectName.isEmpty() || "transferiert".equalsIgnoreCase(aspectName)) {
			return null;
		}
		return aspectName;
	}

	/**
	 * Wandelt einen Aspekt-Namen aus Aspekte.json in die Tooltip-Schreibweise um,
	 * z. B. "Aspekt des Mondes" -> "Mond Aspekt".
	 */
	public static String toTooltipAspectDisplayName(String jsonAspectName) {
		if (jsonAspectName == null || jsonAspectName.isEmpty() || "-".equals(jsonAspectName)) {
			return null;
		}
		String trimmed = jsonAspectName.trim();
		if (trimmed.regionMatches(true, 0, "Aspekt der ", 0, "Aspekt der ".length())) {
			return trimmed.substring("Aspekt der ".length()).trim() + " Aspekt";
		}
		if (trimmed.regionMatches(true, 0, "Aspekt des ", 0, "Aspekt des ".length())) {
			String genitive = trimmed.substring("Aspekt des ".length()).trim();
			return genitiveFromDes(genitive) + " Aspekt";
		}
		if (trimmed.regionMatches(true, 0, "Aspekt von ", 0, "Aspekt von ".length())) {
			return trimmed.substring("Aspekt von ".length()).trim() + " Aspekt";
		}
		return trimmed;
	}

	private static String genitiveFromDes(String genitive) {
		if (genitive == null || genitive.isEmpty()) {
			return "";
		}
		if (genitive.endsWith("es") && genitive.length() > 2) {
			return genitive.substring(0, genitive.length() - 2);
		}
		if (genitive.endsWith("s") && genitive.length() > 1) {
			return genitive.substring(0, genitive.length() - 1);
		}
		return genitive;
	}

	/**
	 * Normalisiert Tooltip-Aspektnamen für den Abgleich, z. B. "Blitz Aspekt" und "Blitzaspekt" -> "blitzaspekt".
	 */
	private static String normalizeAspectTooltipName(String name) {
		if (name == null || name.isEmpty()) {
			return "";
		}
		return name.replaceAll("§[0-9a-fk-or]", "").replaceAll("\\s+", "").toLowerCase();
	}

	/**
	 * Findet Aspekt-Infos anhand des Tooltip-Namens in eckigen Klammern (transferierte Aspekte).
	 */
	public static AspectInfo getAspectInfoByTooltipDisplayName(String tooltipAspectName) {
		if (tooltipAspectName == null || tooltipAspectName.isEmpty()) {
			return null;
		}
		String cleanName = tooltipAspectName.replaceAll("§[0-9a-fk-or]", "").trim();
		if (cleanName.isEmpty()) {
			return null;
		}

		AspectInfo directMatch = tooltipAspectDisplayNameLookup.get(cleanName.toLowerCase());
		if (directMatch != null) {
			return directMatch;
		}

		String normalizedInput = normalizeAspectTooltipName(cleanName);
		AspectInfo normalizedMatch = tooltipAspectDisplayNameLookup.get(normalizedInput);
		if (normalizedMatch != null) {
			return normalizedMatch;
		}

		for (Map.Entry<String, AspectInfo> entry : tooltipAspectDisplayNameLookup.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(cleanName)
					|| normalizeAspectTooltipName(entry.getKey()).equals(normalizedInput)) {
				return entry.getValue();
			}
		}

		Map<String, AspectInfo> uniqueAspects = new HashMap<>();
		for (AspectInfo info : aspectsDatabase.values()) {
			if (info == null || info.aspectName == null || info.aspectName.equals("-")) {
				continue;
			}
			uniqueAspects.putIfAbsent(info.aspectName, info);
		}
		for (AspectInfo info : uniqueAspects.values()) {
			String displayName = toTooltipAspectDisplayName(info.aspectName);
			if (displayName != null && (displayName.equalsIgnoreCase(cleanName)
					|| normalizeAspectTooltipName(displayName).equals(normalizedInput))) {
				return info;
			}
			if (info.aspectName.equalsIgnoreCase(cleanName)
					|| normalizeAspectTooltipName(info.aspectName).equals(normalizedInput)) {
				return info;
			}
		}

		return null;
	}

	private static void buildTooltipAspectDisplayNameLookup() {
		tooltipAspectDisplayNameLookup.clear();
		Map<String, AspectInfo> uniqueAspects = new HashMap<>();
		for (AspectInfo info : aspectsDatabase.values()) {
			if (info == null || info.aspectName == null || info.aspectName.equals("-")) {
				continue;
			}
			uniqueAspects.putIfAbsent(info.aspectName, info);
		}
		for (Map.Entry<String, AspectInfo> entry : uniqueAspects.entrySet()) {
			String displayName = toTooltipAspectDisplayName(entry.getKey());
			if (displayName != null && !displayName.isEmpty()) {
				tooltipAspectDisplayNameLookup.put(displayName.toLowerCase(), entry.getValue());
				tooltipAspectDisplayNameLookup.put(normalizeAspectTooltipName(displayName), entry.getValue());
			}
			tooltipAspectDisplayNameLookup.put(entry.getKey().toLowerCase(), entry.getValue());
			tooltipAspectDisplayNameLookup.put(normalizeAspectTooltipName(entry.getKey()), entry.getValue());
		}
	}

	/**
	 * Gets the full AspectInfo for a blueprint item name
	 * @param blueprintName The cleaned blueprint name (without "[Bauplan]")
	 * @return The AspectInfo, or null if not found
	 */
	public static AspectInfo getAspectInfoForBlueprintFull(String blueprintName) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			return null;
		}
		
		// Remove any remaining formatting
		String cleanName = blueprintName.replaceAll("§[0-9a-fk-or]", "");
		cleanName = cleanName.replaceAll("[\\u3400-\\u4DBF]", "");
		cleanName = cleanName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		
		return aspectsDatabase.get(cleanName);
	}

	/**
	 * Loads the aspects database from Aspekte.json
	 */
	public static void loadAspectsDatabase() {
		try {
			// Versuche zuerst lokale Datei zu laden, sonst Mod-Ressourcen
			java.nio.file.Path resource = getConfigFilePath("Aspekte.json");
			if (resource == null) {
				return;
			}
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					
					for (String itemName : json.keySet()) {
						JsonObject itemData = json.getAsJsonObject(itemName);
						String aspectName = itemData.get("aspect_name").getAsString();
						String aspectDescription = itemData.get("aspect_description").getAsString();
						
						// Only store items that have aspect information
						if (!aspectName.isEmpty() || !aspectDescription.isEmpty()) {
							aspectsDatabase.put(itemName, new AspectInfo(aspectName, aspectDescription));
						}
					}
					buildTooltipAspectDisplayNameLookup();
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load aspects database: " + e.getMessage());
			// Silent error handling
		}
	}

	/**
	 * Lädt die Aspects-Datenbank neu (nach Update vom Server)
	 */
	public static void reloadAspectsDatabase() {
		aspectsDatabase.clear();
		tooltipAspectDisplayNameLookup.clear();
		loadAspectsDatabase();
	}
}

package net.felix.utilities.Aincraft.blueprints.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;

import static net.felix.utilities.Aincraft.blueprints.tooltip.BlueprintFloorTooltip.getDrachenzahnFloorFromColor;
import static net.felix.utilities.Overall.util.JsonConfigFiles.getConfigFilePath;

/**
 * Ebenen-Zuordnung der Blaupausen (blueprints.json).
 */
public final class BlueprintDatabase {

	private BlueprintDatabase() {
	}

	// Blueprint floor tracking - Maps: "blueprint name" -> floor number (e.g., "Drachenzahn" -> 1)
	public static Map<String, Integer> blueprintFloorMap = new HashMap<>();

	/**
	 * Gets the floor number for a blueprint item name
	 * @param blueprintName The cleaned blueprint name (without "[Bauplan]")
	 * @param itemNameText Optional Text object for color detection (for "Drachenzahn")
	 * @return The floor number, or null if not found
	 */
	public static Integer getFloorNumberForBlueprint(String blueprintName, Component itemNameText) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			return null;
		}
		
		// Remove any remaining formatting
		String cleanName = blueprintName.replaceAll("§[0-9a-fk-or]", "");
		cleanName = cleanName.replaceAll("[\\u3400-\\u4DBF]", "");
		cleanName = cleanName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		
		// Special handling for "Drachenzahn" which appears in multiple floors
		if (cleanName.equals("Drachenzahn")) {
			if (itemNameText != null) {
				return getDrachenzahnFloorFromColor(itemNameText);
			}
			// If no Text provided, return null (can't determine floor without color)
			return null;
		}
		
		// For other blueprints, get floor number from map
		return blueprintFloorMap.get(cleanName);
	}

	/**
	 * Gets the floor number for a blueprint item name (simplified version without color detection)
	 * @param blueprintName The cleaned blueprint name (without "[Bauplan]")
	 * @return The floor number, or null if not found
	 */
	public static Integer getFloorNumberForBlueprint(String blueprintName) {
		return getFloorNumberForBlueprint(blueprintName, null);
	}

	/**
	 * Loads the blueprints database from blueprints.json to map blueprint names to floor numbers
	 */
	public static void loadBlueprintsDatabase() {
		try {
			// Versuche zuerst lokale Datei zu laden, sonst Mod-Ressourcen
			java.nio.file.Path resource = getConfigFilePath("blueprints.json");
			if (resource == null) {
				return;
			}
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					JsonObject floors = json.getAsJsonObject("floors");
					
					for (String floorKey : floors.keySet()) {
						// Extract floor number from "floor_X" format
						final int floorNumber;
						try {
							String floorNumStr = floorKey.replace("floor_", "");
							floorNumber = Integer.parseInt(floorNumStr);
						} catch (NumberFormatException e) {
							// If parsing fails, default to 1
							continue; // Skip this floor if we can't parse the number
						}
						
						JsonObject floorData = floors.getAsJsonObject(floorKey);
						JsonObject blueprints = floorData.getAsJsonObject("blueprints");
						
						// Iterate through all rarities
						for (String rarityKey : blueprints.keySet()) {
							JsonObject rarityData = blueprints.getAsJsonObject(rarityKey);
							com.google.gson.JsonArray itemsArray = rarityData.getAsJsonArray("items");
							
							// Add each blueprint item to the map
							itemsArray.forEach(element -> {
								String blueprintName = element.getAsString();
								// Store the floor number for this blueprint
								blueprintFloorMap.put(blueprintName, floorNumber);
							});
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load blueprints database: " + e.getMessage());
			// Silent error handling
		}
	}

	/**
	 * Lädt die Blueprints-Datenbank neu (nach Update vom Server)
	 */
	public static void reloadBlueprintsDatabase() {
		blueprintFloorMap.clear();
		loadBlueprintsDatabase();
	}
}

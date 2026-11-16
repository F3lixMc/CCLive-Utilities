package net.felix.utilities.Aincraft.materials.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;

import static net.felix.utilities.Aincraft.fishing.ponds.FishingMaterials.getFishingMaterialFloorInfo;
import static net.felix.utilities.Aincraft.fishing.ponds.PondDatabase.loadPondMaterialsFromFarmworld;
import static net.felix.utilities.Aincraft.materials.data.PrismarinMaterials.getPrismarinMaterialFloorInfo;
import static net.felix.utilities.Overall.util.JsonConfigFiles.getConfigFilePath;

/**
 * Material-Datenbank (Aincraft.json) und Ebenen-Abfrage.
 */
public final class MaterialDatabase {

	private MaterialDatabase() {
	}

	// Materials tracking
	public static Map<String, MaterialInfo> materialsDatabase = new HashMap<>();

	/**
	 * Loads the materials database from materials.json
	 */
	public static void loadMaterialsDatabase() {
		try {
			// Versuche zuerst lokale Datei zu laden, sonst Mod-Ressourcen
			java.nio.file.Path resource = getConfigFilePath("Aincraft.json");
			if (resource == null) {
				return;
			}
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					JsonObject floors = json.getAsJsonObject("floors");
					
					for (String floorKey : floors.keySet()) {
						JsonObject floorData = floors.getAsJsonObject(floorKey);
						
						// Extract floor number
						int floorNumber = Integer.parseInt(floorKey.replace("floor_", ""));
						
						// Add mob name to database
						if (floorData.has("mob")) {
							var mobElement = floorData.get("mob");
							// Support both string and array formats
							if (mobElement.isJsonArray()) {
								// Array format: ["Name1", "Name2", ...]
								com.google.gson.JsonArray mobArray = mobElement.getAsJsonArray();
								for (var element : mobArray) {
									String mobName = element.getAsString();
									if (!mobName.isEmpty()) {
										materialsDatabase.put(mobName, new MaterialInfo(floorNumber, "mob", "WHITE"));
									}
								}
							} else {
								// String format: "Name" (backward compatibility)
								String mobName = mobElement.getAsString();
								if (!mobName.isEmpty()) {
									materialsDatabase.put(mobName, new MaterialInfo(floorNumber, "mob", "WHITE"));
								}
							}
						}
						
						// Add materials to database
						JsonObject materials = floorData.getAsJsonObject("materials");
						for (String rarityKey : materials.keySet()) {
							JsonObject rarityData = materials.getAsJsonObject(rarityKey);
							String color = rarityData.get("color").getAsString();
							com.google.gson.JsonArray materialsArray = rarityData.getAsJsonArray("materials");
							
							for (var element : materialsArray) {
								String materialName = element.getAsString();
								if (!materialName.isEmpty()) {
									materialsDatabase.put(materialName, new MaterialInfo(floorNumber, rarityKey, color));
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}

	/**
	 * Gets material floor information for a given material name
	 * @param materialName Name of the material
	 * @return MaterialFloorInfo with floor, rarity, and color, or null if not found
	 */
	public static MaterialFloorInfo getMaterialFloorInfo(String materialName) {
		if (materialName == null || materialName.isEmpty()) {
			return null;
		}
		
		MaterialInfo info = materialsDatabase.get(materialName);
		if (info != null) {
			return new MaterialFloorInfo(info.floor, info.pond, null, info.rarity, info.color);
		}

		MaterialFloorInfo prismarinInfo = getPrismarinMaterialFloorInfo(materialName);
		if (prismarinInfo != null) {
			return prismarinInfo;
		}

		return getFishingMaterialFloorInfo(materialName);
	}

	/**
	 * Lädt die Materials-Datenbank neu (nach Update vom Server)
	 */
	public static void reloadMaterialsDatabase() {
		materialsDatabase.clear();
		loadMaterialsDatabase();
		loadPondMaterialsFromFarmworld();
	}
}

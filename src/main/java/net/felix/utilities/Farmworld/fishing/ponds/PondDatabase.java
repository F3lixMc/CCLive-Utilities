package net.felix.utilities.Farmworld.fishing.ponds;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.felix.utilities.Aincraft.materials.data.MaterialInfo;

import static net.felix.utilities.Aincraft.materials.data.MaterialDatabase.materialsDatabase;
import static net.felix.utilities.Overall.util.JsonConfigFiles.getConfigFilePath;

/**
 * Teich-Datenbank aus der Farmworld-Konfiguration.
 */
public final class PondDatabase {

	private PondDatabase() {
	}

	public static final List<PondInfo> pondsDatabase = new ArrayList<>();

	static final Map<String, Set<String>> fishFamilyToPonds = new HashMap<>();

	static final List<String> sortedFishFamilies = new ArrayList<>();

	private static final List<String> fishFamilyDisplayOrder = new ArrayList<>();

	static final Map<String, Integer> fishFamilyDisplayOrderIndex = new HashMap<>();

	public static void clearPondMaterialsFromDatabase() {
		materialsDatabase.entrySet().removeIf(entry -> entry.getValue().pond != null);
	}

	public static void loadPondMaterialsFromJson(JsonObject json) {
		if (json == null || !json.has("ponds")) {
			return;
		}
		fishFamilyToPonds.clear();
		fishFamilyDisplayOrder.clear();
		fishFamilyDisplayOrderIndex.clear();
		com.google.gson.JsonArray pondsArray = json.getAsJsonArray("ponds");
		for (var element : pondsArray) {
			JsonObject pondData = element.getAsJsonObject();
			String pondName = pondData.has("pond") ? pondData.get("pond").getAsString() : "";
			if (pondName.isEmpty()) {
				continue;
			}

			if (pondData.has("fish_familys")) {
				com.google.gson.JsonArray familiesArray = pondData.getAsJsonArray("fish_familys");
				for (var familyElement : familiesArray) {
					String family = familyElement.getAsString().trim();
					if (!family.isEmpty()) {
						fishFamilyToPonds.computeIfAbsent(family, key -> new LinkedHashSet<>()).add(pondName);
						if (!fishFamilyDisplayOrderIndex.containsKey(family)) {
							fishFamilyDisplayOrderIndex.put(family, fishFamilyDisplayOrder.size());
							fishFamilyDisplayOrder.add(family);
						}
					}
				}
			}

			if (!pondData.has("materials")) {
				continue;
			}
			JsonObject pondMaterials = pondData.getAsJsonObject("materials");
			for (String rarityKey : pondMaterials.keySet()) {
				JsonObject rarityData = pondMaterials.getAsJsonObject(rarityKey);
				String color = rarityData.get("color").getAsString();
				com.google.gson.JsonArray materialsArray = rarityData.getAsJsonArray("materials");
				for (var materialElement : materialsArray) {
					String materialName = materialElement.getAsString();
					if (!materialName.isEmpty()) {
						materialsDatabase.put(materialName,
								new MaterialInfo(0, pondName, rarityKey, color));
					}
				}
			}
		}
		rebuildSortedFishFamilies();
	}

	private static void rebuildSortedFishFamilies() {
		sortedFishFamilies.clear();
		sortedFishFamilies.addAll(fishFamilyToPonds.keySet());
		sortedFishFamilies.sort((a, b) -> Integer.compare(b.length(), a.length()));
	}

	public static void loadPondMaterialsFromFarmworld() {
		try {
			java.nio.file.Path resource = getConfigFilePath("Farmworld.json");
			if (resource == null) {
				return;
			}
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					loadPondMaterialsFromJson(json);
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}
}

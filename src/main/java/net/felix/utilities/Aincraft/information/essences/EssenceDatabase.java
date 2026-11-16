package net.felix.utilities.Aincraft.information.essences;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;

import static net.felix.utilities.Overall.util.JsonConfigFiles.getConfigFilePath;

/**
 * Essenz-Datenbank (Essenz.json).
 */
public final class EssenceDatabase {

	private EssenceDatabase() {
	}

	// Essence tracking
	static Map<String, EssenceInfo> essencesDatabase = new HashMap<>();

	/**
	 * Loads the essences database from Essenz.json
	 */
	public static void loadEssencesDatabase() {
		try {
			// Versuche zuerst lokale Datei zu laden, sonst Mod-Ressourcen
			java.nio.file.Path resource = getConfigFilePath("Essenz.json");
			if (resource == null) {
				return;
			}
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					com.google.gson.JsonArray essencesArray = json.getAsJsonArray("essences");
					
					for (var element : essencesArray) {
						JsonObject essenceData = element.getAsJsonObject();
						String name = essenceData.get("name").getAsString();
						if (name.isEmpty() || !essenceData.has("wave")) {
							continue;
						}
						var waveElement = essenceData.get("wave");
						if (!waveElement.isJsonPrimitive()) {
							continue;
						}
						Integer wave = null;
						var wavePrimitive = waveElement.getAsJsonPrimitive();
						if (wavePrimitive.isNumber()) {
							wave = wavePrimitive.getAsInt();
						} else if (wavePrimitive.isString() && !"?".equals(wavePrimitive.getAsString().trim())) {
							try {
								wave = Integer.parseInt(wavePrimitive.getAsString().trim());
							} catch (NumberFormatException ignored) {
								continue;
							}
						}
						essencesDatabase.put(name, new EssenceInfo(wave));
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}

	/**
	 * Lädt die Essences-Datenbank neu (nach Update vom Server)
	 */
	public static void reloadEssencesDatabase() {
		essencesDatabase.clear();
		loadEssencesDatabase();
	}
}

package net.felix.utilities.Aincraft.information.collections;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;

import static net.felix.utilities.Aincraft.information.util.JsonConfigFiles.getConfigFilePath;

/**
 * Collection-Stufen (Farmworld-Konfiguration).
 */
public final class CollectionDatabase {

	private CollectionDatabase() {
	}

	// Collection tracking
	static Map<Integer, Integer> collectionsDatabase = new HashMap<>(); // collection number -> amount needed

	/**
	 * Loads the collections database from Collections.json
	 */
	public static void loadCollectionsDatabase() {
		try {
			// Versuche zuerst lokale Datei zu laden, sonst Mod-Ressourcen
			java.nio.file.Path resource = getConfigFilePath("Collections.json");
			if (resource == null) {
				return;
			}
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					JsonObject collections = json.getAsJsonObject("collections");
					
					for (String collectionKey : collections.keySet()) {
						JsonObject collectionData = collections.getAsJsonObject(collectionKey);
						int collectionNumber = Integer.parseInt(collectionKey.replace("collection_", ""));
						int amount = collectionData.get("amount").getAsInt();
						
						collectionsDatabase.put(collectionNumber, amount);
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load collections database: " + e.getMessage());
			// Silent error handling
		}
	}

	/**
	 * Lädt die Collections-Datenbank neu (nach Update vom Server)
	 */
	public static void reloadCollectionsDatabase() {
		collectionsDatabase.clear();
		loadCollectionsDatabase();
	}
}

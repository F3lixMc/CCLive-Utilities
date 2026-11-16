package net.felix.features.information.gadgets;

import net.fabricmc.loader.api.FabricLoader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;

import static net.felix.features.information.licenses.LicenseDatabase.LICENSES_CONFIG_FILE;


/**
 * Gadget-Datenbank (Name/Alias -> Level -> Ort).
 */
public final class GadgetDatabase {

	private GadgetDatabase() {
	}

	// Gadget tracking - Maps: "name/alias" -> Map<"level", "location">
	static Map<String, Map<String, String>> gadgetsDatabase = new HashMap<>();

	/**
	 * Loads the gadgets database from Farmworld.json
	 */
	public static void loadGadgetsDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(LICENSES_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Gadgets config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					
					// Load all gadget categories
					String[] gadgetCategories = {"mining_gadgets", "logging_gadgets", "gadget_bags", 
						"magnet_gadgets", "smelting_gadgets", "distribution_gadgets"};
					
					for (String category : gadgetCategories) {
						if (json.has(category)) {
							com.google.gson.JsonArray gadgetsArray = json.getAsJsonArray(category);
							
							for (var element : gadgetsArray) {
								JsonObject gadgetData = element.getAsJsonObject();
								String name = gadgetData.get("name").getAsString();
								String level = gadgetData.get("level").getAsString();
								String location = gadgetData.get("location").getAsString();
								
								if (!name.isEmpty() && !level.isEmpty() && !location.isEmpty()) {
									// Store by name
									gadgetsDatabase.computeIfAbsent(name, k -> new HashMap<>()).put(level, location);
									
									// Also store by alias if it exists
									if (gadgetData.has("aliases")) {
										String aliases = gadgetData.get("aliases").getAsString();
										if (!aliases.isEmpty()) {
											// Split aliases by "||" and store each one separately
											String[] aliasParts = aliases.split("\\|\\|");
											for (String alias : aliasParts) {
												alias = alias.trim();
												if (!alias.isEmpty()) {
													gadgetsDatabase.computeIfAbsent(alias, k -> new HashMap<>()).put(level, location);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load gadgets database: " + e.getMessage());
			// Silent error handling
		}
	}
}

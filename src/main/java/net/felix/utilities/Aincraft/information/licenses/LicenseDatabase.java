package net.felix.utilities.Aincraft.information.licenses;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import net.felix.utilities.Aincraft.information.ponds.PondInfo;

import static net.felix.utilities.Aincraft.information.ponds.PondDatabase.clearPondMaterialsFromDatabase;
import static net.felix.utilities.Aincraft.information.ponds.PondDatabase.loadPondMaterialsFromJson;
import static net.felix.utilities.Aincraft.information.ponds.PondDatabase.pondsDatabase;
import static net.felix.utilities.Aincraft.information.util.JsonConfigFiles.getConfigFilePath;

/**
 * Lizenz-Datenbank aus der Farmworld-Konfiguration.
 */
public final class LicenseDatabase {

	private LicenseDatabase() {
	}

	// License tracking
	static Map<String, LicenseInfo> licensesDatabase = new HashMap<>();

	public static final String LICENSES_CONFIG_FILE = "assets/cclive-utilities/Farmworld.json";

	/**
	 * Loads the licenses database from Farmworld.json
	 */
	public static void loadLicensesDatabase() {
		try {
			// Versuche zuerst lokale Datei zu laden, sonst Mod-Ressourcen
			java.nio.file.Path resource = getConfigFilePath("Farmworld.json");
			if (resource == null) {
				return;
			}
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					com.google.gson.JsonArray licensesArray = json.getAsJsonArray("licenses");
					
					for (var element : licensesArray) {
						JsonObject licenseData = element.getAsJsonObject();
						String name = licenseData.get("name").getAsString();
						String location = licenseData.get("location").getAsString();
						
						// Store the license with its name as key
						if (!name.isEmpty() && !location.isEmpty()) {
							LicenseInfo licenseInfo = new LicenseInfo(location);
							licensesDatabase.put(name, licenseInfo);
							
							// Also store the alias if it exists
							if (licenseData.has("aliases")) {
								String aliases = licenseData.get("aliases").getAsString();
								if (!aliases.isEmpty()) {
									// Split aliases by "||" and store each one separately
									String[] aliasParts = aliases.split("\\|\\|");
									for (String alias : aliasParts) {
										alias = alias.trim();
										if (!alias.isEmpty()) {
											licensesDatabase.put(alias, licenseInfo);
										}
									}
								}
							}
						}
					}

					if (json.has("ponds")) {
						com.google.gson.JsonArray pondsArray = json.getAsJsonArray("ponds");
						for (var element : pondsArray) {
							JsonObject pondData = element.getAsJsonObject();
							String biome = pondData.has("biome") ? pondData.get("biome").getAsString() : "";
							String pond = pondData.has("pond") ? pondData.get("pond").getAsString() : "";
							if (!biome.isEmpty() && !pond.isEmpty()) {
								pondsDatabase.add(new PondInfo(biome, pond));
							}
						}
						loadPondMaterialsFromJson(json);
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load licenses database: " + e.getMessage());
			// Silent error handling
		}
	}

	/**
	 * Lädt die Licenses-Datenbank neu (nach Update vom Server)
	 */
	public static void reloadLicensesDatabase() {
		licensesDatabase.clear();
		pondsDatabase.clear();
		clearPondMaterialsFromDatabase();
		loadLicensesDatabase();
	}
}

package net.felix.utilities.Overall.util;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Ermittelt den Speicherort der JSON-Konfigurationsdateien.
 */
public final class JsonConfigFiles {

	private JsonConfigFiles() {
	}

	/**
	 * Versucht eine Datei von einem lokalen Pfad zu laden, falls vorhanden, sonst von Mod-Ressourcen
	 */
	public static java.nio.file.Path getConfigFilePath(String fileName) {
		try {
			java.nio.file.Path configDir = net.felix.CCLiveUtilities.getConfigDir();
			java.nio.file.Path modConfigDir = configDir.resolve("cclive-utilities");
			java.nio.file.Path localFile = modConfigDir.resolve(fileName);
			
			// Prüfe ob lokale Datei existiert
			if (java.nio.file.Files.exists(localFile)) {
				return localFile;
			}
		} catch (Exception e) {
			// Ignoriere Fehler
		}
		
		// Fallback zu Mod-Ressourcen
		try {
			String resourcePath;
			if (fileName.equals("MKLevel.json")) {
				resourcePath = "assets/MKLevel.json";
			} else {
				resourcePath = "assets/cclive-utilities/" + fileName;
			}
			
			return FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(resourcePath)
				.orElseThrow(() -> new RuntimeException("Config file not found: " + fileName));
		} catch (Exception e) {
			return null;
		}
	}
}

package net.felix.utilities.Overall;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.config.LeaderboardConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Utility-Klasse zum automatischen Update von JSON-Config-Dateien vom Server
 */
public class JsonConfigUpdateUtility {
    
    private static boolean serverCheckRegistered = false;
    
    /**
     * Initialisiert die Utility und registriert Server-Join-Event für Version-Checks
     */
    public static void initialize() {
        if (serverCheckRegistered) {
            return;
        }
        
        // Registriere Server-Join-Event für Version-Checks (nur einmal)
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            checkAndUpdateAllConfigs();
        });
        
        serverCheckRegistered = true;
    }
    
    /**
     * Prüft und aktualisiert alle JSON-Config-Dateien vom Server
     */
    private static void checkAndUpdateAllConfigs() {
        CompletableFuture.runAsync(() -> {
            try {
                LeaderboardManager manager = LeaderboardManager.getInstance();
                if (manager == null || !manager.isEnabled() || !manager.isRegistered()) {
                    return; // Leaderboard-System nicht aktiv, kein Server-Check
                }
                
                LeaderboardConfig config = new LeaderboardConfig();
                String serverUrl = config.getServerUrl();
                
                // Prüfe und aktualisiere alle Configs parallel
                checkAndUpdateConfig("aincraft", "Aincraft.json", CCLiveUtilitiesConfig.HANDLER.instance().aincraftConfigVersion, 
                    (version) -> CCLiveUtilitiesConfig.HANDLER.instance().aincraftConfigVersion = version, serverUrl);
                
                checkAndUpdateConfig("aspekte", "Aspekte.json", CCLiveUtilitiesConfig.HANDLER.instance().aspekteConfigVersion,
                    (version) -> CCLiveUtilitiesConfig.HANDLER.instance().aspekteConfigVersion = version, serverUrl);
                
                checkAndUpdateConfig("blueprints", "blueprints.json", CCLiveUtilitiesConfig.HANDLER.instance().blueprintsConfigVersion,
                    (version) -> CCLiveUtilitiesConfig.HANDLER.instance().blueprintsConfigVersion = version, serverUrl);
                
                checkAndUpdateConfig("cardsstatues", "CardsStatues.json", CCLiveUtilitiesConfig.HANDLER.instance().cardsstatuesConfigVersion,
                    (version) -> CCLiveUtilitiesConfig.HANDLER.instance().cardsstatuesConfigVersion = version, serverUrl);
                
                checkAndUpdateConfig("collections", "Collections.json", CCLiveUtilitiesConfig.HANDLER.instance().collectionsConfigVersion,
                    (version) -> CCLiveUtilitiesConfig.HANDLER.instance().collectionsConfigVersion = version, serverUrl);
                
                checkAndUpdateConfig("essenz", "Essenz.json", CCLiveUtilitiesConfig.HANDLER.instance().essenzConfigVersion,
                    (version) -> CCLiveUtilitiesConfig.HANDLER.instance().essenzConfigVersion = version, serverUrl);
                
                checkAndUpdateConfig("farmworld", "Farmworld.json", CCLiveUtilitiesConfig.HANDLER.instance().farmworldConfigVersion,
                    (version) -> CCLiveUtilitiesConfig.HANDLER.instance().farmworldConfigVersion = version, serverUrl);
                
                checkAndUpdateConfig("kits", "Kits.json", CCLiveUtilitiesConfig.HANDLER.instance().kitsConfigVersion,
                    (version) -> CCLiveUtilitiesConfig.HANDLER.instance().kitsConfigVersion = version, serverUrl);
                
                checkAndUpdateConfig("mklevel", "MKLevel.json", CCLiveUtilitiesConfig.HANDLER.instance().mklevelConfigVersion,
                    (version) -> CCLiveUtilitiesConfig.HANDLER.instance().mklevelConfigVersion = version, serverUrl);
                
            } catch (Exception e) {
                // Fehler beim Server-Check sind nicht kritisch
                System.err.println("⚠️ [JsonConfigUpdate] Fehler beim Prüfen der Config-Versionen: " + e.getMessage());
            }
        });
    }
    
    /**
     * Prüft die Server-Version für eine spezifische Config-Datei und lädt sie bei Bedarf
     * 
     * @param endpointName Name des Endpoints (z.B. "aincraft" für /aincraft/version)
     * @param fileName Name der lokalen Datei (z.B. "Aincraft.json")
     * @param localVersion Aktuelle lokale Version
     * @param versionSetter Callback zum Setzen der neuen Version
     * @param serverUrl Server-URL
     */
    private static void checkAndUpdateConfig(String endpointName, String fileName, int localVersion, 
                                             java.util.function.Consumer<Integer> versionSetter, String serverUrl) {
        CompletableFuture.runAsync(() -> {
            try {
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                
                // Prüfe Server-Version
                java.net.http.HttpRequest versionRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(serverUrl + "/" + endpointName + "/version"))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .header("User-Agent", "CCLive-Utilities/1.0")
                    .GET()
                    .build();
                
                java.net.http.HttpResponse<String> versionResponse = httpClient.send(versionRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
                
                if (versionResponse.statusCode() == 200) {
                    JsonObject versionJson = JsonParser.parseString(versionResponse.body()).getAsJsonObject();
                    int serverVersion = versionJson.has("version") ? versionJson.get("version").getAsInt() : 0;
                    
                    if (serverVersion > localVersion) {
                        // Neue Version verfügbar, lade Datei
                        java.net.http.HttpRequest dataRequest = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create(serverUrl + "/" + endpointName + "/data"))
                            .timeout(java.time.Duration.ofSeconds(30))
                            .header("User-Agent", "CCLive-Utilities/1.0")
                            .GET()
                            .build();
                        
                        java.net.http.HttpResponse<String> dataResponse = httpClient.send(dataRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
                        
                        if (dataResponse.statusCode() == 200) {
                            JsonObject dataJson = JsonParser.parseString(dataResponse.body()).getAsJsonObject();
                            String jsonData = dataJson.has("data") ? dataJson.get("data").getAsString() : null;
                            
                            if (jsonData != null) {
                                // Validiere JSON
                                JsonParser.parseString(jsonData);
                                
                                // Speichere lokal
                                Path localFile = getLocalConfigPath(fileName);
                                if (localFile != null) {
                                    Files.createDirectories(localFile.getParent());
                                    try (OutputStreamWriter writer = new OutputStreamWriter(
                                            Files.newOutputStream(localFile), StandardCharsets.UTF_8)) {
                                        writer.write(jsonData);
                                    }
                                    
                                    // Aktualisiere Version in Config
                                    versionSetter.accept(serverVersion);
                                    CCLiveUtilitiesConfig.HANDLER.save();
                                    
                                    // Lade die neue Datei in InformationenUtility neu
                                    reloadConfigInInformationenUtility(fileName);
                                    
                                    System.out.println("✅ [JsonConfigUpdate] " + fileName + " aktualisiert (Version " + serverVersion + ")");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Fehler beim Server-Check sind nicht kritisch
                System.err.println("⚠️ [JsonConfigUpdate] Fehler beim Prüfen von " + fileName + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Gibt den Pfad zur lokalen Config-Datei zurück
     */
    private static Path getLocalConfigPath(String fileName) {
        try {
            Path configDir = net.felix.CCLiveUtilities.getConfigDir();
            Path modConfigDir = configDir.resolve("cclive-utilities");
            return modConfigDir.resolve(fileName);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Lädt eine Config-Datei in InformationenUtility neu
     */
    private static void reloadConfigInInformationenUtility(String fileName) {
        try {
            // InformationenUtility hat verschiedene Load-Methoden für verschiedene Dateien
            // Wir müssen die entsprechende Methode aufrufen
            switch (fileName) {
                case "Aincraft.json":
                    // InformationenUtility lädt Aincraft.json in loadMaterialsDatabase()
                    // Wir müssen die Datenbank neu laden
                    net.felix.utilities.Overall.InformationenUtility.reloadMaterialsDatabase();
                    break;
                case "Aspekte.json":
                    net.felix.utilities.Overall.InformationenUtility.reloadAspectsDatabase();
                    break;
                case "blueprints.json":
                    net.felix.utilities.Overall.InformationenUtility.reloadBlueprintsDatabase();
                    break;
                case "CardsStatues.json":
                    net.felix.utilities.Overall.InformationenUtility.reloadCardsStatuesDatabase();
                    break;
                case "Collections.json":
                    net.felix.utilities.Overall.InformationenUtility.reloadCollectionsDatabase();
                    break;
                case "Essenz.json":
                    net.felix.utilities.Overall.InformationenUtility.reloadEssencesDatabase();
                    break;
                case "Farmworld.json":
                    net.felix.utilities.Overall.InformationenUtility.reloadLicensesDatabase();
                    break;
                case "Kits.json":
                    net.felix.utilities.Town.KitFilterUtility.reloadKitsDatabase();
                    break;
                case "MKLevel.json":
                    net.felix.utilities.Overall.InformationenUtility.reloadMKLevelDatabase();
                    break;
            }
        } catch (Exception e) {
            System.err.println("⚠️ [JsonConfigUpdate] Fehler beim Neuladen von " + fileName + " in InformationenUtility: " + e.getMessage());
        }
    }
}

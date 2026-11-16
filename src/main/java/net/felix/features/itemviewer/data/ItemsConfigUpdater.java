package net.felix.features.itemviewer.data;

import net.felix.features.itemviewer.ui.ItemStackCache;

import static net.felix.features.itemviewer.data.ItemCatalog.allItems;
import static net.felix.features.itemviewer.data.ItemCatalog.itemByName;
import static net.felix.features.itemviewer.data.ItemCatalog.itemsLoaded;
import static net.felix.features.itemviewer.data.ItemCatalog.loadItemsAsync;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.filteredItems;

/**
 * Prueft beim Serverbeitritt, ob eine neuere items.json verfuegbar ist, und aktualisiert die lokale Datei.
 */
public final class ItemsConfigUpdater {

    private ItemsConfigUpdater() {
    }

    public static final String ITEMS_CONFIG_FILE = "assets/cclive-utilities/items.json";

    public static final String LOCAL_ITEMS_FILE = "items.json";

    public static boolean serverCheckRegistered = false;

    /**
     * Prüft die Server-Version und lädt ggf. eine neue Datei
     */
    public static void checkAndUpdateItemsConfig() {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                net.felix.leaderboards.LeaderboardManager manager = net.felix.leaderboards.LeaderboardManager.getInstance();
                if (manager == null) {
                    return;
                }
                
                net.felix.leaderboards.config.LeaderboardConfig config = new net.felix.leaderboards.config.LeaderboardConfig();
                String serverUrl = config.getServerUrl();
                
                // Items-Download braucht keine Registrierung (nur GET)
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest versionRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(serverUrl + "/items/version"))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .header("User-Agent", "CCLive-Utilities/1.0")
                    .GET()
                    .build();
                
                java.net.http.HttpResponse<String> versionResponse = httpClient.send(versionRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
                
                if (versionResponse.statusCode() == 200) {
                    com.google.gson.JsonObject versionJson = com.google.gson.JsonParser.parseString(versionResponse.body()).getAsJsonObject();
                    int serverVersion = versionJson.has("version") ? versionJson.get("version").getAsInt() : 0;
                    int localVersion = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().itemsConfigVersion;
                    
                    if (serverVersion > localVersion) {
                        // Neue Version verfügbar, lade Datei
                        java.net.http.HttpRequest dataRequest = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create(serverUrl + "/items/data"))
                            .timeout(java.time.Duration.ofSeconds(30)) // Längeres Timeout für große Datei
                            .header("User-Agent", "CCLive-Utilities/1.0")
                            .GET()
                            .build();
                        
                        java.net.http.HttpResponse<String> dataResponse = httpClient.send(dataRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
                        
                        if (dataResponse.statusCode() == 200) {
                            com.google.gson.JsonObject dataJson = com.google.gson.JsonParser.parseString(dataResponse.body()).getAsJsonObject();
                            String jsonData = dataJson.has("data") ? dataJson.get("data").getAsString() : null;
                            
                            if (jsonData != null) {
                                // Validiere JSON
                                com.google.gson.JsonParser.parseString(jsonData);
                                
                                // Speichere lokal
                                java.nio.file.Path localFile = getLocalItemsPath();
                                if (localFile != null) {
                                    java.nio.file.Files.createDirectories(localFile.getParent());
                                    try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                                            java.nio.file.Files.newOutputStream(localFile), java.nio.charset.StandardCharsets.UTF_8)) {
                                        writer.write(jsonData);
                                    }
                                    
                                    // Aktualisiere Version in Config
                                    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().itemsConfigVersion = serverVersion;
                                    net.felix.CCLiveUtilitiesConfig.HANDLER.save();
                                    
                                    // Lade die neue Datei
                                    itemsLoaded = false;
                                    allItems.clear();
                                    filteredItems.clear();
                                    itemByName.clear();
                                    ItemStackCache.clearItemStackCache();
                                    loadItemsAsync();
                                    
                                    System.out.println("✅ [ItemViewer] Items-Config aktualisiert (Version " + serverVersion + ")");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Fehler beim Server-Check sind nicht kritisch
                System.err.println("⚠️ [ItemViewer] Fehler beim Prüfen der Items-Config-Version: " + e.getMessage());
            }
        });
    }

    /**
     * Gibt den Pfad zur lokalen items.json zurück
     */
    static java.nio.file.Path getLocalItemsPath() {
        try {
            java.nio.file.Path configDir = net.felix.CCLiveUtilities.getConfigDir();
            java.nio.file.Path modConfigDir = configDir.resolve("cclive-utilities");
            return modConfigDir.resolve(LOCAL_ITEMS_FILE);
        } catch (Exception e) {
            return null;
        }
    }
}

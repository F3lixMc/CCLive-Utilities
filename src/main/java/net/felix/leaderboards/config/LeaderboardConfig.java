package net.felix.leaderboards.config;

/**
 * Konfiguration für das Leaderboard-System
 */
public class LeaderboardConfig {
    // Server-Konfiguration
    private String serverUrl = "https://cclive-utilities.net"; // Produktions-Server
    private boolean enabled = true;
    
    // Update-Intervalle (in Millisekunden)
    private int statsUpdateInterval = 30000; // 30 Sekunden
    private int collectionUpdateInterval = 10000; // 10 Sekunden
    private int floorUpdateInterval = 5000; // 5 Sekunden
    
    // HTTP-Timeouts
    private int connectionTimeout = 5000; // 5 Sekunden
    private int readTimeout = 10000; // 10 Sekunden
    
    // Debug-Modus
    private boolean debugMode = false;
    
    public LeaderboardConfig() {
        loadFromProperties();
    }
    
    /**
     * Lädt Konfiguration aus Properties oder verwendet Standardwerte
     */
    private void loadFromProperties() {
        // TODO: Später aus config.properties laden
        // Für jetzt verwenden wir die Standardwerte
        
        // Für Development/Testing
        if (System.getProperty("leaderboard.debug") != null) {
            debugMode = true;
            // Silent error handling("🐛 LeaderboardConfig: Debug-Modus aktiviert");
        }
        
        // Server-URL aus System Property überschreiben
        String customUrl = System.getProperty("leaderboard.server.url");
        if (customUrl != null && !customUrl.isEmpty()) {
            serverUrl = customUrl;
            // Silent error handling("🔧 LeaderboardConfig: Server-URL überschrieben: " + serverUrl);
        }
    }
    
    // Getter
    public String getServerUrl() { return serverUrl; }
    public boolean isEnabled() { return enabled; }
    public int getStatsUpdateInterval() { return statsUpdateInterval; }
    public int getCollectionUpdateInterval() { return collectionUpdateInterval; }
    public int getFloorUpdateInterval() { return floorUpdateInterval; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public boolean isDebugMode() { return debugMode; }
    
    // Setter (für dynamische Konfiguration)
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
}

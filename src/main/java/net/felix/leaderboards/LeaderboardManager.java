package net.felix.leaderboards;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.felix.leaderboards.collectors.DataCollector;
import net.felix.leaderboards.collectors.StatsCollector;
import net.felix.leaderboards.collectors.CollectionCollector;
import net.felix.leaderboards.collectors.FloorProgressCollector;
import net.felix.leaderboards.collectors.RareMobCollector;
import net.felix.leaderboards.collectors.CoinCollector;
import net.felix.leaderboards.collectors.MenuHoverCollector;
import net.felix.leaderboards.collectors.FloorKillsCollector;
import net.felix.utilities.DebugUtility;
import net.felix.leaderboards.http.HttpClient;
import net.felix.leaderboards.config.LeaderboardConfig;
import net.felix.leaderboards.cooldown.LeaderboardCooldownManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zentraler Manager für alle Leaderboard-Funktionen
 * Verwaltet die verschiedenen Datensammler und die Kommunikation mit dem Server
 */
public class LeaderboardManager {
    private static final Gson gson = new Gson();
    private static LeaderboardManager instance;
    
    // Konfiguration und HTTP-Client
    private final LeaderboardConfig config;
    private final HttpClient httpClient;
    
    // Datensammler
    private final Map<String, DataCollector> collectors = new HashMap<>();
    
    // Cache für letzte gesendete Werte (verhindert unnötige Updates)
    private final Map<String, Object> lastSentValues = new ConcurrentHashMap<>();
    
    // Spieler-Token für Authentifizierung
    private String playerToken = null;
    private String playerName = null;
    
    // Status
    private boolean isEnabled = true;
    private boolean isRegistered = false;
    private boolean isChatEventRegistered = false;
    
    private LeaderboardManager() {
        this.config = new LeaderboardConfig();
        this.httpClient = new HttpClient(config);
        initializeCollectors();
    }
    
    public static LeaderboardManager getInstance() {
        if (instance == null) {
            instance = new LeaderboardManager();
        }
        return instance;
    }
    
    /**
     * Initialisiert alle Datensammler
     */
    private void initializeCollectors() {
        // Coin-Sammler (zufällige /cc coins Aufrufe)
        collectors.put("coins", new CoinCollector());
        
        // Menu-Hover-Sammler (Alltime Kills, Collections aus Tooltips)
        collectors.put("menu_hover", new MenuHoverCollector());
        
        // Floor-Kills-Sammler (Floor-spezifische Kills)
        collectors.put("floor_kills", new FloorKillsCollector());
        
        // Collection-Sammler (Chat-basiert, als Fallback)
        collectors.put("collections", new CollectionCollector());
        
        // Floor-Progress-Sammler (Floor-Zeiten, wird später für Custom UI verwendet)
        collectors.put("floors", new FloorProgressCollector());
        
        // Rare Mob-Sammler (integriert mit MobTimerUtility)
        collectors.put("rare_mobs", new RareMobCollector());
        
        // Statistik-Sammler (Fallback für andere Stats)
        collectors.put("stats", new StatsCollector());
        
        System.out.println("✅ LeaderboardManager: " + collectors.size() + " Datensammler initialisiert");
    }
    
    
    /**
     * Startet das Leaderboard-System
     */
    public void initialize() {
        if (!config.isEnabled()) {
            System.out.println("⚠️ LeaderboardManager: System deaktiviert in Konfiguration");
            return;
        }
        
        // Registriere Chat-Event für Coin-Updates
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (isEnabled) { // Entferne isRegistered-Bedingung für Chat-Processing
                return !processChatMessage(message); // Rückgabe invertiert: true = anzeigen, false = unterdrücken
            }
            return true;
        });
        
        // Starte alle Datensammler
        for (Map.Entry<String, DataCollector> entry : collectors.entrySet()) {
            try {
                entry.getValue().initialize();
                System.out.println("✅ Datensammler '" + entry.getKey() + "' gestartet");
            } catch (Exception e) {
                System.err.println("❌ Fehler beim Starten von Sammler '" + entry.getKey() + "': " + e.getMessage());
            }
        }
        
        // Registriere Server-Join Event für verzögerte Registrierung
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            System.out.println("🌐 Server beigetreten - starte Spieler-Registrierung...");
            schedulePlayerRegistration();
        });
        
        System.out.println("🚀 LeaderboardManager vollständig initialisiert!");
    }
    
    /**
     * Plant die Spieler-Registrierung mit Retry-Mechanismus
     */
    private void schedulePlayerRegistration() {
        // Versuche sofort zu registrieren
        if (!tryRegisterPlayer()) {
            // Falls fehlgeschlagen, versuche es alle 5 Sekunden erneut (max. 12 Versuche = 1 Minute)
            scheduleRegistrationRetry(0);
        }
    }
    
    /**
     * Retry-Mechanismus für die Registrierung
     */
    private void scheduleRegistrationRetry(int attempt) {
        if (attempt >= 12) { // Max. 12 Versuche (1 Minute)
            System.out.println("⚠️ Spieler-Registrierung nach 1 Minute aufgegeben");
            return;
        }
        
        // Warte 5 Sekunden und versuche erneut
        CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.SECONDS).execute(() -> {
            if (!isRegistered && !tryRegisterPlayer()) {
                scheduleRegistrationRetry(attempt + 1);
            }
        });
    }
    
    /**
     * Versucht den Spieler zu registrieren
     * @return true wenn erfolgreich, false wenn Spieler noch nicht verfügbar
     */
    private boolean tryRegisterPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            System.out.println("⚠️ Spieler noch nicht verfügbar - Retry in 5 Sekunden");
            return false;
        }
        
        registerPlayer();
        return true;
    }
    
    /**
     * Registriert den aktuellen Spieler beim Server
     */
    private void registerPlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            System.out.println("⚠️ Spieler nicht verfügbar - Registrierung später versuchen");
            return;
        }
        
        playerName = client.player.getName().getString();
        
        CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestData = new JsonObject();
                requestData.addProperty("player", playerName);
                
                JsonObject response = httpClient.post("/register", requestData);
                if (response != null && response.has("token")) {
                    playerToken = response.get("token").getAsString();
                    isRegistered = true;
                    System.out.println("✅ Spieler '" + playerName + "' erfolgreich registriert!");
                    return true;
                }
            } catch (Exception e) {
                System.err.println("❌ Registrierung fehlgeschlagen: " + e.getMessage());
            }
            return false;
        }).thenAccept(success -> {
            if (!success) {
                System.out.println("⚠️ Registrierung fehlgeschlagen - Leaderboards deaktiviert");
                isEnabled = false;
            }
        });
    }
    
    /**
     * Sendet einen Score-Update an den Server (mit Cooldown-System)
     * Nur für automatische Updates - manuelle Updates verwenden updateScoreManual()
     * @param boardName Name des Leaderboards
     * @param score Der neue Score-Wert
     */
    public void updateScore(String boardName, long score) {
        System.out.println("🔥 DEBUG: updateScore aufgerufen - boardName=" + boardName + ", score=" + score);
        System.out.println("🔥 DEBUG: isEnabled=" + isEnabled + ", isRegistered=" + isRegistered + ", playerToken=" + (playerToken != null ? "vorhanden" : "null"));
        
        if (!isEnabled || !isRegistered || playerToken == null) {
            System.out.println("🔥 DEBUG: updateScore ABGEBROCHEN - Bedingungen nicht erfüllt");
            return;
        }
        
        // Prüfe ob sich der Wert geändert hat
        Object lastValue = lastSentValues.get(boardName);
        System.out.println("🔥 DEBUG: Wert-Check - lastValue=" + lastValue + ", newScore=" + score);
        if (lastValue != null && lastValue.equals(score)) {
            System.out.println("🔥 DEBUG: updateScore ABGEBROCHEN - Wert hat sich nicht geändert");
            return; // Keine Änderung
        }
        
        LeaderboardCooldownManager cooldownManager = LeaderboardCooldownManager.getInstance();
        
        // Prüfe Cooldown
        System.out.println("🔥 DEBUG: Cooldown-Check für " + boardName);
        if (!cooldownManager.canUpdateScore(boardName)) {
            System.out.println("🔥 DEBUG: updateScore ABGEBROCHEN - Cooldown aktiv");
            // Füge zur Warteschlange hinzu
            cooldownManager.queueScoreUpdate(boardName, score);
            
            if (config.isDebugMode()) {
                long timeRemaining = cooldownManager.getTimeUntilNextScoreUpdate(boardName);
                System.out.println("⏳ Score Update für " + boardName + " in Warteschlange (" + 
                    cooldownManager.formatTimeRemaining(timeRemaining) + ")");
            }
            return;
        }
        System.out.println("🔥 DEBUG: Alle Checks bestanden - sende HTTP-Request");
        
        // Sende Update asynchron
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("🔥 DEBUG: Sende HTTP-Request für " + boardName + " = " + score);
                JsonObject requestData = new JsonObject();
                requestData.addProperty("board", boardName);
                requestData.addProperty("score", score);
                
                JsonObject response = httpClient.postWithToken("/update", requestData, playerToken);
                System.out.println("🔥 DEBUG: HTTP-Response erhalten: " + (response != null ? response.toString() : "null"));
                if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                    lastSentValues.put(boardName, score);
                    cooldownManager.markScoreUpdated(boardName);
                    
                    if (config.isDebugMode()) {
                        System.out.println("📊 Score Update: " + boardName + " = " + score);
                        DebugUtility.debugLeaderboard("✅ Erfolgreich an Server gesendet");
                    }
                    
                    // Callback für erfolgreiche Coin-Updates (immer aufrufen, nicht nur im Debug-Modus)
                    if ("current_coins".equals(boardName)) {
                        CoinCollector coinCollector = (CoinCollector) collectors.get("coins");
                        if (coinCollector != null) {
                            coinCollector.onServerUpdateSuccess(score);
                        }
                    }
                } else {
                    System.err.println("❌ Score Update fehlgeschlagen für " + boardName);
                    if (config.isDebugMode()) {
                        DebugUtility.debugLeaderboard("❌ Server-Fehler bei " + boardName);
                    }
                    
                    // Callback für fehlgeschlagene Coin-Updates (immer aufrufen, nicht nur im Debug-Modus)
                    if ("current_coins".equals(boardName)) {
                        CoinCollector coinCollector = (CoinCollector) collectors.get("coins");
                        if (coinCollector != null) {
                            coinCollector.onServerUpdateFailure();
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Fehler beim Score Update für " + boardName + ": " + e.getMessage());
                
                // Callback für Exception-Fehler
                if ("current_coins".equals(boardName)) {
                    CoinCollector coinCollector = (CoinCollector) collectors.get("coins");
                    if (coinCollector != null) {
                        coinCollector.onServerUpdateFailure();
                    }
                }
            }
        });
    }
    
    /**
     * Sendet einen manuellen Score-Update an den Server (OHNE Cooldown-System)
     * Für manuelle Befehle wie /cc coins - wird immer gesendet
     * @param boardName Name des Leaderboards
     * @param score Der neue Score-Wert
     */
    public void updateScoreManual(String boardName, long score) {
        System.out.println("🔥 DEBUG: updateScoreManual aufgerufen - boardName=" + boardName + ", score=" + score);
        System.out.println("🔥 DEBUG: isEnabled=" + isEnabled + ", isRegistered=" + isRegistered + ", playerToken=" + (playerToken != null ? "vorhanden" : "null"));
        
        if (!isEnabled || !isRegistered || playerToken == null) {
            System.out.println("🔥 DEBUG: updateScoreManual ABGEBROCHEN - Bedingungen nicht erfüllt");
            return;
        }
        
        // Prüfe ob sich der Wert geändert hat
        Object lastValue = lastSentValues.get(boardName);
        System.out.println("🔥 DEBUG: Wert-Check - lastValue=" + lastValue + ", newScore=" + score);
        if (lastValue != null && lastValue.equals(score)) {
            System.out.println("🔥 DEBUG: updateScoreManual ABGEBROCHEN - Wert hat sich nicht geändert");
            return; // Keine Änderung
        }
        
        // KEIN Cooldown-Check für manuelle Updates!
        System.out.println("🔥 DEBUG: Manueller Update - überspringe Cooldown-Check");
        System.out.println("🔥 DEBUG: Alle Checks bestanden - sende HTTP-Request");
        
        // Sende Update asynchron
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("🔥 DEBUG: Sende HTTP-Request für " + boardName + " = " + score);
                JsonObject requestData = new JsonObject();
                requestData.addProperty("board", boardName);
                requestData.addProperty("score", score);
                
                JsonObject response = httpClient.postWithToken("/update", requestData, playerToken);
                System.out.println("🔥 DEBUG: HTTP-Response erhalten: " + (response != null ? response.toString() : "null"));
                if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                    lastSentValues.put(boardName, score);
                    // Für manuelle Updates KEINEN Cooldown setzen - das würde automatische Updates blockieren!
                    
                    if (config.isDebugMode()) {
                        System.out.println("📊 Manueller Score Update: " + boardName + " = " + score);
                        DebugUtility.debugLeaderboard("✅ Erfolgreich an Server gesendet");
                    }
                    
                    // Callback für erfolgreiche Coin-Updates (immer aufrufen, nicht nur im Debug-Modus)
                    if ("current_coins".equals(boardName)) {
                        CoinCollector coinCollector = (CoinCollector) collectors.get("coins");
                        if (coinCollector != null) {
                            coinCollector.onServerUpdateSuccess(score);
                        }
                    }
                } else {
                    System.err.println("❌ Manueller Score Update fehlgeschlagen für " + boardName);
                    if (config.isDebugMode()) {
                        DebugUtility.debugLeaderboard("❌ Server-Fehler bei " + boardName);
                    }
                    
                    // Callback für fehlgeschlagene Coin-Updates (immer aufrufen, nicht nur im Debug-Modus)
                    if ("current_coins".equals(boardName)) {
                        CoinCollector coinCollector = (CoinCollector) collectors.get("coins");
                        if (coinCollector != null) {
                            coinCollector.onServerUpdateFailure();
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Fehler beim manuellen Score Update für " + boardName + ": " + e.getMessage());
                
                // Callback für Exception-Fehler
                if ("current_coins".equals(boardName)) {
                    CoinCollector coinCollector = (CoinCollector) collectors.get("coins");
                    if (coinCollector != null) {
                        coinCollector.onServerUpdateFailure();
                    }
                }
            }
        });
    }
    
    /**
     * Holt die Top 10 eines Leaderboards (mit Cooldown-System)
     */
    public CompletableFuture<JsonObject> getLeaderboard(String boardName) {
        if (!isRegistered || playerToken == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        LeaderboardCooldownManager cooldownManager = LeaderboardCooldownManager.getInstance();
        
        // Prüfe Cooldown
        if (!cooldownManager.canFetchLeaderboard(boardName)) {
            if (config.isDebugMode()) {
                long timeRemaining = cooldownManager.getTimeUntilNextLeaderboardFetch(boardName);
                System.out.println("⏳ Leaderboard-Fetch für " + boardName + " noch im Cooldown (" + 
                    cooldownManager.formatTimeRemaining(timeRemaining) + ")");
            }
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject result = httpClient.get("/leaderboard/" + boardName + "/" + playerName);
                if (result != null) {
                    cooldownManager.markLeaderboardFetched(boardName);
                }
                return result;
            } catch (Exception e) {
                System.err.println("❌ Fehler beim Abrufen von Leaderboard " + boardName + ": " + e.getMessage());
                return null;
            }
        });
    }
    
    /**
     * Stoppt alle Datensammler
     */
    public void shutdown() {
        for (Map.Entry<String, DataCollector> entry : collectors.entrySet()) {
            try {
                entry.getValue().shutdown();
            } catch (Exception e) {
                System.err.println("❌ Fehler beim Stoppen von Sammler '" + entry.getKey() + "': " + e.getMessage());
            }
        }
        System.out.println("🛑 LeaderboardManager gestoppt");
    }
    
    // Getter/Setter
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { this.isEnabled = enabled; }
    public boolean isRegistered() { return isRegistered; }
    public String getPlayerName() { return playerName; }
    
    /**
     * Verarbeitet eingehende Chat-Nachrichten für Datensammlung
     * @return true wenn die Nachricht unterdrückt werden soll, false wenn sie angezeigt werden soll
     */
    private boolean processChatMessage(Text message) {
        if (message == null) return false;
        
        String messageText = message.getString(); // Verwende getString() für korrekte Textextraktion
        if (messageText.isEmpty()) return false;
        
        // Ignoriere unsere eigenen CCLive-Nachrichten um Endlosschleifen zu vermeiden
        if (messageText.contains("[CCLive-Debug]")) {
            return false;
        }
        
        try {
            // Weiterleitung an CoinCollector
            CoinCollector coinCollector = (CoinCollector) collectors.get("coins");
            if (coinCollector != null && coinCollector.isActive()) {
                boolean shouldSuppress = coinCollector.processChatMessage(messageText);
                if (shouldSuppress) {
                    return true; // Nachricht unterdrücken
                }
            }
            
            // Weiterleitung an CollectionCollector (Fallback)
            CollectionCollector collectionCollector = (CollectionCollector) collectors.get("collections");
            if (collectionCollector != null && collectionCollector.isActive()) {
                // TODO: Implementiere processChatMessage in CollectionCollector falls nötig
            }
            
        } catch (Exception e) {
            if (config.isDebugMode()) {
                System.err.println("❌ Fehler bei Chat-Nachricht-Verarbeitung: " + e.getMessage());
            }
        }
        
        return false; // Nachricht nicht unterdrücken
    }
    
    
    /**
     * Manueller Refresh der Registrierung (falls beim Start fehlgeschlagen)
     */
    public void refreshRegistration() {
        isRegistered = false;
        playerToken = null;
        registerPlayer();
    }
    
    /**
     * Gibt einen spezifischen Datensammler zurück
     */
    public DataCollector getCollector(String name) {
        return collectors.get(name);
    }
    
    // =================== GETTER METHODEN FÜR COMMANDS ===================
    
    /**
     * Gibt den HTTP-Client zurück
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }
    
    /**
     * Gibt den Player-Token zurück
     */
    public String getPlayerToken() {
        return playerToken;
    }
}

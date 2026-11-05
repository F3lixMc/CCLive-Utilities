package net.felix.leaderboards;

import com.google.gson.JsonObject;
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
import net.felix.leaderboards.collectors.FarmworldCollectionsCollector;
import net.felix.identity.ModRegistrationTransport;
import net.felix.identity.PlayerRegistrationService;
import net.felix.leaderboards.http.HttpClient;
import net.felix.utilities.Other.DebugUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.felix.leaderboards.config.LeaderboardConfig;
import net.felix.leaderboards.cooldown.LeaderboardCooldownManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zentraler Manager für alle Leaderboard-Funktionen
 * Verwaltet die verschiedenen Datensammler und die Kommunikation mit dem Server
 */
public class LeaderboardManager {
    private static LeaderboardManager instance;
    
    // Konfiguration und HTTP-Client
    private final LeaderboardConfig config;
    private final HttpClient httpClient;
    
    // Datensammler
    private final Map<String, DataCollector> collectors = new HashMap<>();
    
    // Cache für letzte gesendete Werte (verhindert unnötige Updates)
    private final Map<String, Object> lastSentValues = new ConcurrentHashMap<>();
    
    // Spieler-Token für Authentifizierung
    private final PlayerRegistrationService registration;
    
    // Online-Status (Heartbeat)
    private static final long PRESENCE_HEARTBEAT_SECONDS = 30;
    private java.util.concurrent.ScheduledExecutorService presenceScheduler;
    
    // Status
    private boolean isEnabled = true;
    
    private LeaderboardManager() {
        this.config = new LeaderboardConfig();
        this.httpClient = new HttpClient(config);
        this.registration = new PlayerRegistrationService(
                new ModRegistrationTransport(httpClient),
                millis -> Thread.sleep(millis),
                new PlayerRegistrationService.Log() {
                    @Override
                    public void info(String message) {
                        System.out.println("[LeaderboardManager] " + message);
                    }

                    @Override
                    public void warn(String message) {
                        System.err.println("[LeaderboardManager] " + message);
                    }
                }
        );
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
        
        // Farmworld-Collection-Sammler (Zone-basierte Collections)
        collectors.put("farmworld_collections", new FarmworldCollectionsCollector());
        
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
        
        // Season-Gate (Upload-Sperre + lokaler Progress-Reset) vor Score-Updates
        SeasonChangeGate.getInstance().initialize();

        // Registriere Server-Join Event für verzögerte Registrierung
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            System.out.println("🌐 [LeaderboardManager] Server beigetreten - starte Spieler-Registrierung...");
            schedulePlayerRegistration();
        });
        
        // Online-Status: Heartbeat starten und beim Verlassen des Servers abmelden
        startPresenceHeartbeat();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> sendPresenceLeave());
        
        // Prüfe ob Spieler bereits auf einem Server ist (z.B. wenn Mod während des Spiels geladen wird)
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null && client.getConnection() != null) {
            System.out.println("🌐 [LeaderboardManager] Spieler bereits auf Server - starte sofortige Registrierung...");
            schedulePlayerRegistration();
        }
        
        System.out.println("🚀 LeaderboardManager vollständig initialisiert!");
    }

    /** Verwirft lastSentValues und Cooldown-Pending nach Season-Reset (kein Backend-Löschen). */
    public void discardSeasonComparisonState() {
        lastSentValues.clear();
        try {
            LeaderboardCooldownManager.getInstance().resetAllCooldowns();
        } catch (Exception ignored) {
        }
    }

    /**
     * Sendet regelmäßig einen Heartbeat an den Server, damit die Website anzeigen kann,
     * wie viele Spieler gerade online sind (GET /presence/count).
     */
    private void startPresenceHeartbeat() {
        if (presenceScheduler != null) {
            return;
        }
        presenceScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "CCLive-Presence");
            thread.setDaemon(true);
            return thread;
        });
        presenceScheduler.scheduleWithFixedDelay(() -> {
            try {
                if (!canSendPresence()) {
                    return;
                }
                httpClient.postWithToken("/presence/heartbeat", new JsonObject(), getPlayerToken());
            } catch (Exception e) {
                // Server nicht erreichbar - beim nächsten Intervall erneut versuchen
            }
        }, 5, PRESENCE_HEARTBEAT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * Meldet den Spieler sofort als offline ab (beim Verlassen eines Servers).
     */
    private void sendPresenceLeave() {
        if (!isEnabled || !isRegistered() || getPlayerToken() == null) {
            return;
        }
        final String token = getPlayerToken();
        CompletableFuture.runAsync(() -> {
            try {
                httpClient.postWithToken("/presence/leave", new JsonObject(), token);
            } catch (Exception e) {
                // Ignorieren - der Server entfernt den Spieler nach dem Timeout automatisch
            }
        });
    }

    private boolean canSendPresence() {
        if (!isEnabled || !isRegistered() || getPlayerToken() == null) {
            return false;
        }
        if (!net.felix.CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        return client != null && client.player != null && client.getConnection() != null;
    }

    private boolean canUploadSeasonalScores() {
        return SeasonChangeGate.getInstance().areSeasonUploadsAllowed();
    }
    
    /**
     * Startet höchstens eine gemeinsame Registrierung. Spieler-Verfügbarkeit
     * wird abgewartet, ohne pro Tick neue Nachweise zu starten.
     */
    private void schedulePlayerRegistration() {
        if (!net.felix.CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            System.out.println("⚠️ [LeaderboardManager] Datentracking deaktiviert - Registrierung übersprungen");
            return;
        }
        CompletableFuture.runAsync(() -> {
            for (int attempt = 0; attempt < 12; attempt++) {
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.player != null) {
                    registration.ensureRegistered();
                    return;
                }
                try {
                    Thread.sleep(5_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            System.out.println("⚠️ Spieler-Registrierung: Spieler nach Wartezeit nicht verfügbar");
        });
    }
    
    /**
     * Sendet einen Score-Update an den Server (mit Cooldown-System)
     * Nur für automatische Updates - manuelle Updates verwenden updateScoreManual()
     * @param boardName Name des Leaderboards
     * @param score Der neue Score-Wert
     */
    public void updateScore(String boardName, long score) {
        if (config.isDebugMode()) {
            System.out.println("🔥 DEBUG: updateScore aufgerufen - boardName=" + boardName + ", score=" + score);
            System.out.println("🔥 DEBUG: isEnabled=" + isEnabled + ", isRegistered=" + isRegistered() + ", playerToken=" + (getPlayerToken() != null ? "vorhanden" : "null"));
        }
        
        // Prüfe ob Tracker-Aktivität aktiviert ist
        if (!net.felix.CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScore ABGEBROCHEN - Datentracking deaktiviert");
            }
            return;
        }

        if (!canUploadSeasonalScores()) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScore ABGEBROCHEN - Season-Gate noch nicht freigegeben");
            }
            return;
        }
        
        if (!isEnabled || !isRegistered() || getPlayerToken() == null) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScore ABGEBROCHEN - Bedingungen nicht erfüllt");
            }
            return;
        }
        
        // Prüfe ob sich der Wert geändert hat
        Object lastValue = lastSentValues.get(boardName);
        if (config.isDebugMode()) {
            System.out.println("🔥 DEBUG: Wert-Check - lastValue=" + lastValue + ", newScore=" + score);
        }
        if (lastValue != null && lastValue.equals(score)) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScore ABGEBROCHEN - Wert hat sich nicht geändert");
            }
            return; // Keine Änderung
        }
        
        LeaderboardCooldownManager cooldownManager = LeaderboardCooldownManager.getInstance();
        
        // Prüfe Cooldown
        if (!cooldownManager.canUpdateScore(boardName)) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScore ABGEBROCHEN - Cooldown aktiv");
            }
            // Füge zur Warteschlange hinzu
            cooldownManager.queueScoreUpdate(boardName, score);
            
            if (config.isDebugMode()) {
                long timeRemaining = cooldownManager.getTimeUntilNextScoreUpdate(boardName);
                System.out.println("⏳ Score Update für " + boardName + " in Warteschlange (" + 
                    cooldownManager.formatTimeRemaining(timeRemaining) + ")");
            }
            return;
        }
        if (config.isDebugMode()) {
            System.out.println("🔥 DEBUG: Alle Checks bestanden - sende HTTP-Request");
        }
        
        // Sende Update asynchron
        CompletableFuture.runAsync(() -> {
            try {
                if (config.isDebugMode()) {
                    System.out.println("🔥 DEBUG: Sende HTTP-Request für " + boardName + " = " + score);
                }
                JsonObject requestData = new JsonObject();
                requestData.addProperty("board", boardName);
                requestData.addProperty("score", score);
                
                JsonObject response = httpClient.postWithToken("/update", requestData, getPlayerToken());
                if (config.isDebugMode()) {
                    System.out.println("🔥 DEBUG: HTTP-Response erhalten: " + (response != null ? response.toString() : "null"));
                }
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
        if (config.isDebugMode()) {
            System.out.println("🔥 DEBUG: updateScoreManual aufgerufen - boardName=" + boardName + ", score=" + score);
            System.out.println("🔥 DEBUG: isEnabled=" + isEnabled + ", isRegistered=" + isRegistered() + ", playerToken=" + (getPlayerToken() != null ? "vorhanden" : "null"));
        }
        
        // Prüfe ob Tracker-Aktivität aktiviert ist
        if (!net.felix.CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScoreManual ABGEBROCHEN - Datentracking deaktiviert");
            }
            return;
        }

        if (!canUploadSeasonalScores()) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScoreManual ABGEBROCHEN - Season-Gate noch nicht freigegeben");
            }
            return;
        }
        
        if (!isEnabled || !isRegistered() || getPlayerToken() == null) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScoreManual ABGEBROCHEN - Bedingungen nicht erfüllt");
            }
            return;
        }
        
        // Prüfe ob sich der Wert geändert hat
        Object lastValue = lastSentValues.get(boardName);
        if (config.isDebugMode()) {
            System.out.println("🔥 DEBUG: Wert-Check - lastValue=" + lastValue + ", newScore=" + score);
        }
        if (lastValue != null && lastValue.equals(score)) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScoreManual ABGEBROCHEN - Wert hat sich nicht geändert");
            }
            return; // Keine Änderung
        }
        
        // KEIN Cooldown-Check für manuelle Updates!
        if (config.isDebugMode()) {
            System.out.println("🔥 DEBUG: Manueller Update - überspringe Cooldown-Check");
            System.out.println("🔥 DEBUG: Alle Checks bestanden - sende HTTP-Request");
        }
        
        // Sende Update asynchron
        CompletableFuture.runAsync(() -> {
            try {
                if (config.isDebugMode()) {
                    System.out.println("🔥 DEBUG: Sende HTTP-Request für " + boardName + " = " + score);
                }
                JsonObject requestData = new JsonObject();
                requestData.addProperty("board", boardName);
                requestData.addProperty("score", score);
                
                JsonObject response = httpClient.postWithToken("/update", requestData, getPlayerToken());
                if (config.isDebugMode()) {
                    System.out.println("🔥 DEBUG: HTTP-Response erhalten: " + (response != null ? response.toString() : "null"));
                }
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
     * Sendet einen additiven Score-Update an den Server (OHNE Cooldown UND OHNE Wert-Prüfung)
     * Für additive Werte wie Playtime, die regelmäßig denselben Wert senden müssen
     * @param boardName Name des Leaderboards
     * @param score Der zu addierende Score-Wert
     */
    public void updateScoreAdditive(String boardName, long score) {
        if (config.isDebugMode()) {
            System.out.println("🔥 DEBUG: updateScoreAdditive aufgerufen - boardName=" + boardName + ", score=" + score);
            System.out.println("🔥 DEBUG: isEnabled=" + isEnabled + ", isRegistered=" + isRegistered() + ", playerToken=" + (getPlayerToken() != null ? "vorhanden" : "null"));
        }
        
        // Prüfe ob Tracker-Aktivität aktiviert ist
        if (!net.felix.CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScoreAdditive ABGEBROCHEN - Datentracking deaktiviert");
            }
            return;
        }

        if (!canUploadSeasonalScores()) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScoreAdditive ABGEBROCHEN - Season-Gate noch nicht freigegeben");
            }
            return;
        }
        
        if (!isEnabled || !isRegistered() || getPlayerToken() == null) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScoreAdditive ABGEBROCHEN - Bedingungen nicht erfüllt");
            }
            return;
        }
        
        if (score <= 0) {
            if (config.isDebugMode()) {
                System.out.println("🔥 DEBUG: updateScoreAdditive ABGEBROCHEN - score <= 0");
            }
            return; // Keine negativen oder null Werte senden
        }
        
        // KEIN Cooldown-Check und KEINE Wert-Prüfung für additive Updates!
        // Der Server summiert automatisch, daher können wir denselben Wert mehrfach senden
        
        // Sende Update asynchron
        CompletableFuture.runAsync(() -> {
            try {
                if (config.isDebugMode()) {
                    System.out.println("🔥 DEBUG: Sende HTTP-Request für " + boardName + " = +" + score + " (additiv)");
                }
                JsonObject requestData = new JsonObject();
                requestData.addProperty("board", boardName);
                requestData.addProperty("score", score);
                
                JsonObject response = httpClient.postWithToken("/update", requestData, getPlayerToken());
                if (config.isDebugMode()) {
                    System.out.println("🔥 DEBUG: HTTP-Response erhalten: " + (response != null ? response.toString() : "null"));
                }
                if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                    // Für additive Updates NICHT lastSentValues aktualisieren, damit wiederholte Werte durchgehen
                    
                    if (config.isDebugMode()) {
                        System.out.println("📊 Additiver Score Update: " + boardName + " +" + score);
                    }
                } else {
                    System.err.println("❌ Additiver Score Update fehlgeschlagen für " + boardName);
                }
            } catch (Exception e) {
                System.err.println("❌ Fehler beim additiven Score Update für " + boardName + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Holt die Top 10 eines Leaderboards (mit Cooldown-System)
     */
    public CompletableFuture<JsonObject> getLeaderboard(String boardName) {
        if (!isRegistered() || getPlayerToken() == null) {
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
                JsonObject result = httpClient.get("/leaderboard/" + boardName + "/" + getPlayerName());
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
        if (presenceScheduler != null) {
            presenceScheduler.shutdownNow();
            presenceScheduler = null;
        }
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
    public boolean isRegistered() { return registration.hasToken(); }
    public String getPlayerName() { return registration.playerName(); }
    
    /**
     * Verarbeitet eingehende Chat-Nachrichten für Datensammlung
     * @return true wenn die Nachricht unterdrückt werden soll, false wenn sie angezeigt werden soll
     */
    private boolean processChatMessage(Component message) {
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
     * True nur wenn bereits ein Token vorliegt. Startet keine neue Registrierung.
     */
    public boolean ensureRegisteredSync() {
        return registration.hasToken();
    }

    /**
     * Hängt sich an die laufende Registrierung oder startet genau eine.
     */
    public CompletableFuture<Boolean> ensureRegisteredAsync() {
        if (!net.felix.CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            return CompletableFuture.completedFuture(false);
        }
        return registration.ensureRegistered();
    }

    /**
     * Wartet auf Worker-Threads auf die gemeinsame Registrierung.
     */
    public boolean awaitRegistration(long timeoutMs) {
        if (!net.felix.CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            return false;
        }
        return registration.await(timeoutMs);
    }

    /**
     * Manueller Refresh über die zentrale Registrierung.
     * Kein zweiter Vorgang während eines laufenden; 429-Wartezeit bleibt.
     */
    public PlayerRegistrationService.RefreshOutcome refreshRegistration() {
        if (!net.felix.CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            return new PlayerRegistrationService.RefreshOutcome(
                    PlayerRegistrationService.RefreshKind.RATE_LIMITED,
                    CompletableFuture.completedFuture(false),
                    0L
            );
        }
        isEnabled = true;
        return registration.refresh();
    }

    public PlayerRegistrationService.Status connectionStatus() {
        return registration.status(net.felix.CCLiveUtilitiesConfig.isDataTrackingEnabled() && isEnabled && config.isEnabled());
    }
    
    /**
     * Diagnose der CCLive-Verbindung ohne Tokenwerte.
     */
    public void printDiagnostics() {
        PlayerRegistrationService.Status status = connectionStatus();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("CCLive-Verbindung");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  Verbindung: " + (status.trackingEnabled() ? "bereit" : "nicht bereit"));
        System.out.println("  Registrierung läuft: " + (status.registrationRunning() ? "ja" : "nein"));
        System.out.println("  Spieler: " + (status.playerName() != null ? status.playerName() : "unbekannt"));
        System.out.println("  Identität: " + (status.playerUuid() != null ? status.playerUuid() : "fehlt"));
        System.out.println("  Token: " + (status.hasToken() ? "vorhanden" : "fehlt"));
        System.out.println("  Nächste Wiederholung: " + formatRetry(status.retryAfterMs()));
        System.out.println("═══════════════════════════════════════════════════════");
    }

    public static String formatRetry(long retryAfterMs) {
        if (retryAfterMs <= 0) {
            return "sofort möglich";
        }
        long seconds = Math.max(1L, (retryAfterMs + 999L) / 1000L);
        return "in " + seconds + "s";
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
        return registration.token();
    }
}

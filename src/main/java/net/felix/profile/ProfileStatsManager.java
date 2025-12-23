package net.felix.profile;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.http.HttpClient;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.collectors.FloorProgressCollector;
import net.felix.leaderboards.collectors.CoinCollector;
import net.felix.leaderboards.collectors.DataCollector;
import net.felix.utilities.Aincraft.CardsStatuesUtility;
import net.felix.utilities.Aincraft.BPViewerUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Manager für Profil-Stats (nicht-rankbasierte Statistiken)
 * Sammelt und sendet Stats wie höchste Welle, Floor, Karten/Statuen-Level, etc.
 */
public class ProfileStatsManager {
    private static ProfileStatsManager instance;
    
    private final HttpClient httpClient;
    
    // Tracking-Variablen
    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 1200; // Alle 60 Sekunden (20 ticks/sec * 60)
    
    // Gespeicherte Max-Werte (lokal)
    private int highestFloor = 0;
    private int highestWave = 0; // TODO: Später implementieren
    private long maxCoins = 0; // TODO: Später implementieren
    private long maxDamage = 0; // TODO: Später implementieren
    
    // Additive Werte (seit letztem Update)
    private int messagesSentSinceUpdate = 0;
    
    // Blueprint-Count (gesamt, wird direkt gesetzt, nicht additiv)
    private int currentBlueprintCount = 0;
    
    // Playtime-Tracking (für Leaderboard)
    private long playtimeTicks = 0; // Ticks seit letztem Update
    private static final int PLAYTIME_UPDATE_INTERVAL = 1200; // Alle 60 Sekunden (20 ticks/sec * 60)
    
    // Erlaubter Server für Playtime-Tracking
    // Option 1: Server-IP und Port (z.B. "play.example.com:25565" oder "192.168.1.100:25565")
    // Option 2: Server-Name aus der Server-Liste (wie du ihn in Minecraft benannt hast)
    // Option 3: Beide leer lassen = Server-Prüfung deaktiviert (trackt auf allen Servern)
    
    // Server-IP/Port (z.B. "play.example.com:25565" oder "192.168.1.100:25565")
    // Leer lassen wenn nicht bekannt oder nicht verwendet werden soll
    private static final String ALLOWED_SERVER_ADDRESS = "playlegend.net:25565";
    
    // Server-Name aus der Server-Liste (wie du ihn in Minecraft benannt hast)
    // Leer lassen wenn nicht verwendet werden soll
    private static final String ALLOWED_SERVER_NAME = ""; // z.B. "Mein Produktionsserver"
    
    // Server-Prüfung deaktivieren für Testing (auf true setzen zum Testen)
    private static final boolean DISABLE_SERVER_CHECK = true; // true = Server-Prüfung überspringen (für Testing)
    
    // Cache für aktuellen Server-Namen (wird beim Join gesetzt)
    private String currentServerName = null;
    
    // Flag: Ist der Spieler auf dem erlaubten Server für Playtime-Tracking?
    // Wird beim Server-Join gesetzt und beim Disconnect zurückgesetzt
    private boolean isOnAllowedServerForPlaytime = false;
    
    // Karten/Statuen-Level (Name -> Level)
    private final Map<String, Integer> cardsLevels = new HashMap<>();
    private final Map<String, Integer> statuesLevels = new HashMap<>();
    
    
    // Status
    private boolean isEnabled = true;
    private boolean isInitialized = false;
    
    private ProfileStatsManager() {
        LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
        this.httpClient = leaderboardManager.getHttpClient();
    }
    
    public static ProfileStatsManager getInstance() {
        if (instance == null) {
            instance = new ProfileStatsManager();
        }
        return instance;
    }
    
    /**
     * Initialisiert den ProfileStatsManager
     */
    public void initialize() {
        if (isInitialized) {
            return;
        }
        
        // Registriere Tick-Event für periodische Updates
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        // Registriere Server-Join Event (Reset + Server-Prüfung + Server-Name speichern)
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            updateServerName(client);
            // Prüfe beim Join, ob es der erlaubte Server ist (nur einmal beim Join)
            isOnAllowedServerForPlaytime = isOnAllowedServer(client);
            resetCounters();
        });
        
        // Registriere Disconnect-Event: Finales Update + Reset Server-Flag
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (isEnabled) {
                // Sende restliche Playtime BEVOR wir das Flag zurücksetzen
                if (isOnAllowedServerForPlaytime && playtimeTicks > 0) {
                    sendPlaytimeToLeaderboard(); // Sende restliche Playtime
                    playtimeTicks = 0;
                }
                
                sendStatsUpdate(true); // Finales Update für andere Stats
            }
            
            // Reset Server-Flag und Server-Name
            isOnAllowedServerForPlaytime = false;
            currentServerName = null;
        });
        
        isInitialized = true;
        if (isDebugEnabled()) {
            System.out.println("[ProfileStats] ✅ ProfileStatsManager initialisiert");
        }
    }
    
    /**
     * Wird jeden Tick aufgerufen
     */
    private void onClientTick(MinecraftClient client) {
        if (!isEnabled || client.player == null || client.world == null) {
            return;
        }
        
        // Playtime NUR auf Multiplayer-Servern UND auf dem erlaubten Server tracken
        // getNetworkHandler() ist null im Singleplayer, nicht null auf Servern
        // isOnAllowedServerForPlaytime wurde bereits beim Server-Join geprüft (effizienter)
        boolean isOnServer = client.getNetworkHandler() != null;
        
        if (isOnServer && isOnAllowedServerForPlaytime) {
            // Playtime tracken (für Leaderboard) - nur auf dem erlaubten Server
            playtimeTicks++;
            
            // Playtime regelmäßig an Leaderboard senden
            if (playtimeTicks >= PLAYTIME_UPDATE_INTERVAL) {
                if (isDebugEnabled()) {
                    System.out.println("⏱️ [ProfileStats] PLAYTIME_UPDATE_INTERVAL erreicht - playtimeTicks=" + playtimeTicks);
                }
                sendPlaytimeToLeaderboard();
                playtimeTicks = 0; // Reset nach Update
            }
        } else {
            // Playtime wird nicht getrackt (nicht auf Server oder nicht auf erlaubtem Server)
        }
        
        // Periodisch Stats sammeln und senden (auch im Singleplayer, falls gewünscht)
        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            collectAndSendStats();
        }
    }
    
    /**
     * Aktualisiert den aktuellen Server-Namen (wird beim Join aufgerufen)
     * Versucht mehrere Methoden, um den Server-Namen zu bekommen (wie im F3-Screen angezeigt)
     */
    private void updateServerName(MinecraftClient client) {
        try {
            boolean debug = isDebugEnabled();
            if (debug) {
                System.out.println("=== [ProfileStats] Server-Name Debug Log ===");
            }
            
            // Methode 1: getCurrentServerEntry (Server-Liste)
            String serverEntryName = null;
            try {
                if (client.getCurrentServerEntry() != null) {
                    serverEntryName = client.getCurrentServerEntry().name;
                    if (debug) {
                        System.out.println("[ProfileStats] getCurrentServerEntry().name = '" + serverEntryName + "'");
                    }
                } else {
                    if (debug) {
                        System.out.println("[ProfileStats] getCurrentServerEntry() = null");
                    }
                }
            } catch (Exception e) {
                if (debug) {
                    System.out.println("[ProfileStats] getCurrentServerEntry() Fehler: " + e.getMessage());
                }
            }
            
            // Methode 2: NetworkHandler ServerInfo (wenn verbunden)
            String serverInfoName = null;
            try {
                if (client.getNetworkHandler() != null) {
                    if (client.getNetworkHandler().getServerInfo() != null) {
                        serverInfoName = client.getNetworkHandler().getServerInfo().name;
                        if (debug) {
                            System.out.println("[ProfileStats] getNetworkHandler().getServerInfo().name = '" + serverInfoName + "'");
                        }
                    } else {
                        if (debug) {
                            System.out.println("[ProfileStats] getNetworkHandler().getServerInfo() = null");
                        }
                    }
                } else {
                    if (debug) {
                        System.out.println("[ProfileStats] getNetworkHandler() = null");
                    }
                }
            } catch (Exception e) {
                if (debug) {
                    System.out.println("[ProfileStats] getNetworkHandler() Fehler: " + e.getMessage());
                }
            }
            
            // Entscheide welchen Wert verwenden
            if (serverEntryName != null && !serverEntryName.isEmpty()) {
                currentServerName = serverEntryName;
                if (debug) {
                    System.out.println("[ProfileStats] ✅ Verwende getCurrentServerEntry: '" + currentServerName + "'");
                }
            } else if (serverInfoName != null && !serverInfoName.isEmpty()) {
                currentServerName = serverInfoName;
                if (debug) {
                    System.out.println("[ProfileStats] ✅ Verwende getNetworkHandler().getServerInfo(): '" + currentServerName + "'");
                }
            } else {
                currentServerName = null;
                if (debug) {
                    System.out.println("[ProfileStats] ❌ WARNUNG: Server-Name konnte nicht erkannt werden!");
                }
            }
            
            if (debug) {
                System.out.println("[ProfileStats] Erwarteter Server-Name: '" + ALLOWED_SERVER_NAME + "'");
                System.out.println("[ProfileStats] Aktueller Server-Name: '" + currentServerName + "'");
                System.out.println("[ProfileStats] Match: " + (currentServerName != null && currentServerName.equalsIgnoreCase(ALLOWED_SERVER_NAME)));
                System.out.println("=== [ProfileStats] Debug Log Ende ===");
            }
            
        } catch (Exception e) {
            System.err.println("[ProfileStats] Fehler beim Erkennen des Server-Namens: " + e.getMessage());
            e.printStackTrace();
            currentServerName = null;
        }
    }
    
    /**
     * Prüft ob der Spieler auf dem erlaubten Server ist (für Playtime-Tracking)
     * 
     * Verwendet mehrere Methoden zur Server-Erkennung:
     * 1. Server-IP/Port (wenn ALLOWED_SERVER_ADDRESS gesetzt ist)
     * 2. Server-Name aus der Server-Liste (wenn ALLOWED_SERVER_NAME gesetzt ist)
     * 3. Beide müssen übereinstimmen (wenn beide gesetzt sind)
     */
    private boolean isOnAllowedServer(MinecraftClient client) {
        // Server-Prüfung überspringen für Testing
        if (DISABLE_SERVER_CHECK) {
            return true; // Immer erlauben wenn Server-Check deaktiviert
        }
        
        // Wenn beide leer sind, tracke auf allen Servern
        if ((ALLOWED_SERVER_ADDRESS == null || ALLOWED_SERVER_ADDRESS.isEmpty()) &&
            (ALLOWED_SERVER_NAME == null || ALLOWED_SERVER_NAME.isEmpty())) {
            if (isDebugEnabled()) {
                System.out.println("[ProfileStats] ⚠️ Keine Server-Prüfung konfiguriert → Playtime-Tracking auf allen Servern aktiviert");
            }
            return true;
        }
        
        boolean matchesAddress = true;
        boolean matchesName = true;
        
        // Prüfe Server-IP/Port
        if (ALLOWED_SERVER_ADDRESS != null && !ALLOWED_SERVER_ADDRESS.isEmpty()) {
            try {
                String currentAddress = null;
                if (client.getNetworkHandler() != null && client.getNetworkHandler().getConnection() != null) {
                    var address = client.getNetworkHandler().getConnection().getAddress();
                    if (address != null) {
                        currentAddress = address.toString();
                        // Entferne "/" am Anfang falls vorhanden
                        if (currentAddress.startsWith("/")) {
                            currentAddress = currentAddress.substring(1);
                        }
                    }
                }
                
                if (currentAddress != null) {
                    matchesAddress = currentAddress.equalsIgnoreCase(ALLOWED_SERVER_ADDRESS);
                    if (isDebugEnabled()) {
                        System.out.println("[ProfileStats] Server-Adresse: '" + currentAddress + "' (erwartet: '" + ALLOWED_SERVER_ADDRESS + "') → " + (matchesAddress ? "✅" : "❌"));
                    }
                } else {
                    matchesAddress = false;
                    if (isDebugEnabled()) {
                        System.out.println("[ProfileStats] ❌ Server-Adresse konnte nicht ermittelt werden");
                    }
                }
            } catch (Exception e) {
                System.err.println("[ProfileStats] Fehler beim Prüfen der Server-Adresse: " + e.getMessage());
                matchesAddress = false;
            }
        }
        
        // Prüfe Server-Name aus der Liste
        if (ALLOWED_SERVER_NAME != null && !ALLOWED_SERVER_NAME.isEmpty()) {
            try {
                String currentName = null;
                if (client.getCurrentServerEntry() != null) {
                    currentName = client.getCurrentServerEntry().name;
                } else if (client.getNetworkHandler() != null && client.getNetworkHandler().getServerInfo() != null) {
                    currentName = client.getNetworkHandler().getServerInfo().name;
                }
                
                if (currentName != null) {
                    matchesName = currentName.equalsIgnoreCase(ALLOWED_SERVER_NAME);
                    if (isDebugEnabled()) {
                        System.out.println("[ProfileStats] Server-Name: '" + currentName + "' (erwartet: '" + ALLOWED_SERVER_NAME + "') → " + (matchesName ? "✅" : "❌"));
                    }
                } else {
                    matchesName = false;
                    if (isDebugEnabled()) {
                        System.out.println("[ProfileStats] ❌ Server-Name konnte nicht ermittelt werden");
                    }
                }
            } catch (Exception e) {
                System.err.println("[ProfileStats] Fehler beim Prüfen des Server-Namens: " + e.getMessage());
                matchesName = false;
            }
        }
        
        // Wenn beide gesetzt sind, müssen beide übereinstimmen
        // Wenn nur eine gesetzt ist, muss diese übereinstimmen
        boolean isAllowed = matchesAddress && matchesName;
        
        if (isDebugEnabled()) {
            if (isAllowed) {
                System.out.println("[ProfileStats] ✅ Spieler ist auf dem erlaubten Server → Playtime-Tracking aktiviert");
            } else {
                System.out.println("[ProfileStats] ⚠️ Spieler ist NICHT auf dem erlaubten Server → Playtime-Tracking deaktiviert");
            }
        }
        
        return isAllowed;
    }
    
    /**
     * Sendet Playtime an Leaderboard-System
     */
    private void sendPlaytimeToLeaderboard() {
        boolean debug = isDebugEnabled();
        if (debug) {
            System.out.println("⏱️ [ProfileStats] sendPlaytimeToLeaderboard() aufgerufen");
            LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
            System.out.println("⏱️ [ProfileStats] isRegistered: " + leaderboardManager.isRegistered());
            System.out.println("⏱️ [ProfileStats] playtimeTicks: " + playtimeTicks);
            System.out.println("⏱️ [ProfileStats] isOnAllowedServerForPlaytime: " + isOnAllowedServerForPlaytime);
        }
        
        LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
        if (!leaderboardManager.isRegistered()) {
            if (debug) {
                System.out.println("⏱️ [ProfileStats] ABGEBROCHEN - nicht registriert");
            }
            return;
        }
        
        // Berechne Playtime in Sekunden
        long playtimeSeconds = playtimeTicks / 20; // Ticks zu Sekunden
        if (debug) {
            System.out.println("⏱️ [ProfileStats] playtimeSeconds berechnet: " + playtimeSeconds);
        }
        
        if (playtimeSeconds <= 0) {
            if (debug) {
                System.out.println("⏱️ [ProfileStats] ABGEBROCHEN - playtimeSeconds <= 0");
            }
            return; // Keine Playtime zu senden
        }
        
        // Sende als Leaderboard "playtime" (additiv)
        // Der Server summiert automatisch
        // Verwende updateScoreAdditive() damit additive Updates nicht blockiert werden
        if (debug) {
            System.out.println("⏱️ [ProfileStats] Rufe updateScoreAdditive auf für playtime = " + playtimeSeconds + " Sekunden");
        }
        leaderboardManager.updateScoreAdditive("playtime", playtimeSeconds);
        
        if (debug) {
            System.out.println("[ProfileStats] ⏱️ Playtime gesendet: " + playtimeSeconds + " Sekunden (gesamt: " + (playtimeTicks / 20) + " Ticks)");
        }
    }
    
    /**
     * Sammelt alle aktuellen Stats und sendet sie an den Server
     */
    private void collectAndSendStats() {
        collectStats();
        sendStatsUpdate(false);
    }
    
    /**
     * Sammelt alle aktuellen Stats aus verschiedenen Quellen
     */
    private void collectStats() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        
        // 1. Höchster Floor (aus FloorProgressCollector)
        try {
            LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
            DataCollector floorCollector = leaderboardManager.getCollector("floors");
            if (floorCollector instanceof FloorProgressCollector) {
                FloorProgressCollector fpc = (FloorProgressCollector) floorCollector;
                String currentFloor = fpc.getCurrentFloor();
                if (currentFloor != null) {
                    // Extrahiere Floor-Nummer (z.B. "floor_1" -> 1)
                    try {
                        String floorNum = currentFloor.replace("floor_", "");
                        int floor = Integer.parseInt(floorNum);
                        if (floor > highestFloor) {
                            highestFloor = floor;
                        }
                    } catch (NumberFormatException e) {
                        // Ignoriere
                    }
                }
            }
        } catch (Exception e) {
            // Ignoriere Fehler
        }
        
        // 2. Karten-Level (aus CardsStatuesUtility)
        try {
            CardsStatuesUtility.CardData currentCard = CardsStatuesUtility.getCurrentCard();
            if (currentCard != null && currentCard.getName() != null && currentCard.getLevel() != null) {
                try {
                    int level = Integer.parseInt(currentCard.getLevel());
                    String cardName = currentCard.getName();
                    // Nur updaten wenn höher
                    cardsLevels.put(cardName, Math.max(cardsLevels.getOrDefault(cardName, 0), level));
                } catch (NumberFormatException e) {
                    // Ignoriere
                }
            }
        } catch (Exception e) {
            // Ignoriere Fehler
        }
        
        // 3. Statuen-Level (aus CardsStatuesUtility)
        try {
            CardsStatuesUtility.StatueData currentStatue = CardsStatuesUtility.getCurrentStatue();
            if (currentStatue != null && currentStatue.getName() != null && currentStatue.getLevel() != null) {
                try {
                    int level = Integer.parseInt(currentStatue.getLevel());
                    String statueName = currentStatue.getName();
                    // Nur updaten wenn höher
                    statuesLevels.put(statueName, Math.max(statuesLevels.getOrDefault(statueName, 0), level));
                } catch (NumberFormatException e) {
                    // Ignoriere
                }
            }
        } catch (Exception e) {
            // Ignoriere Fehler
        }
        
        // 4. Blueprint-Count (aus BPViewerUtility) - Gesamtzahl direkt setzen
        try {
            BPViewerUtility bpViewer = BPViewerUtility.getInstance();
            if (bpViewer != null) {
                // Verwende die autoritative Quelle: foundBlueprints
                // Dies ist die Hauptliste aller tatsächlich gefundenen Blueprints
                // (wird beim Finden aktualisiert und beim Laden gespeichert)
                currentBlueprintCount = bpViewer.getFoundBlueprintsCount();
            }
        } catch (Exception e) {
            // Ignoriere Fehler
        }
        
        // 5. Messages Sent wird bereits über onMessageSent() gezählt
        //    (wird aktuell im ChatScreenMixin bei jeder gesendeten Chat-Nachricht aufgerufen)
        
        // 6. Max Coins (aus CoinCollector)
        try {
            LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
            DataCollector coinsCollector = leaderboardManager.getCollector("coins");
            if (coinsCollector instanceof CoinCollector) {
                CoinCollector cc = (CoinCollector) coinsCollector;
                long currentCoins = cc.getCoins();
                if (currentCoins > 0) {
                    updateMaxCoins(currentCoins);
                }
            }
        } catch (Exception e) {
            // Ignoriere Fehler
        }
        
        // 7. Highest Wave (TODO: Quelle noch definieren)
        
        // 8. Max Damage (TODO: Quelle noch definieren)
    }
    
    /**
     * Sendet Stats-Update an den Server
     */
    private void sendStatsUpdate(boolean isFinal) {
        LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
        if (!leaderboardManager.isRegistered() || leaderboardManager.getPlayerToken() == null) {
            if (isDebugEnabled()) {
                System.out.println("[ProfileStats] ❌ Stats-Update abgebrochen: Leaderboard nicht registriert oder kein Token");
            }
            return; // Nicht registriert, kein Update
        }
        
        // Bereite JSON-Payload vor
        JsonObject payload = new JsonObject();
        
        // Max-Werte (nur wenn sich geändert haben)
        if (highestFloor > 0) {
            payload.addProperty("highest_floor", highestFloor);
        }
        if (highestWave > 0) {
            payload.addProperty("highest_wave", highestWave);
        }
        if (maxCoins > 0) {
            payload.addProperty("max_coins", maxCoins);
        }
        if (maxDamage > 0) {
            payload.addProperty("max_damage", maxDamage);
        }
        
        // Additive Werte (nur wenn > 0)
        if (messagesSentSinceUpdate > 0) {
            payload.addProperty("messages_sent", messagesSentSinceUpdate);
        }
        
        // Blueprint-Count: Gesamtzahl direkt setzen (nicht additiv)
        if (currentBlueprintCount > 0) {
            payload.addProperty("blueprints_found", currentBlueprintCount);
        }
        
        // Chosen Stat für Hover-Anzeige (immer senden, wenn gesetzt)
        String chosenStat = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().hoverStatsChosenStat;
        if (chosenStat != null && !chosenStat.isEmpty()) {
            payload.addProperty("chosen_stat", chosenStat);
        }
        
        // Karten/Statuen (nur wenn vorhanden)
        if (!cardsLevels.isEmpty()) {
            JsonObject cardsJson = new JsonObject();
            for (Map.Entry<String, Integer> entry : cardsLevels.entrySet()) {
                cardsJson.addProperty(entry.getKey(), entry.getValue());
            }
            payload.add("cards", cardsJson);
        }
        
        if (!statuesLevels.isEmpty()) {
            JsonObject statuesJson = new JsonObject();
            for (Map.Entry<String, Integer> entry : statuesLevels.entrySet()) {
                statuesJson.addProperty(entry.getKey(), entry.getValue());
            }
            payload.add("statues", statuesJson);
        }
        
        // Wenn Payload leer ist, kein sinnvolles Update
        if (payload.entrySet().isEmpty()) {
            if (isDebugEnabled()) {
                System.out.println("[ProfileStats] ⚠️ Stats-Update übersprungen (Payload leer)");
            }
            return;
        }

        if (isDebugEnabled()) {
            System.out.println("[ProfileStats] 📤 Sende Stats-Update " + (isFinal ? "(final)" : "") + ": " + payload.toString());
        }

        // Sende Update asynchron
        CompletableFuture.supplyAsync(() -> {
            try {
                httpClient.postWithToken("/profile/update", payload, leaderboardManager.getPlayerToken());
                return true;
            } catch (Exception e) {
                System.err.println("❌ Fehler beim Senden von Profil-Stats: " + e.getMessage());
                return false;
            }
        }).thenAccept(success -> {
            if (success && !isFinal) {
                // Reset additive Werte nach erfolgreichem Update
                resetAdditiveCounters();
            }
        });
    }
    
    /**
     * Setzt additive Counter zurück (nach erfolgreichem Update)
     */
    private void resetAdditiveCounters() {
        messagesSentSinceUpdate = 0;
        // currentBlueprintCount wird nicht zurückgesetzt, da es die Gesamtzahl ist
        // playtimeTicks wird nicht zurückgesetzt, da es für Leaderboard verwendet wird
    }
    
    /**
     * Setzt alle Counter zurück (beim Server-Join)
     */
    private void resetCounters() {
        resetAdditiveCounters();
        playtimeTicks = 0; // Playtime auch zurücksetzen beim Join
        highestFloor = 0;
        highestWave = 0;
        maxCoins = 0;
        maxDamage = 0;
        currentBlueprintCount = 0;
        cardsLevels.clear();
        statuesLevels.clear();
    }
    
    /**
     * Wird aufgerufen wenn eine Nachricht gesendet wurde (von ChatManager)
     */
    public void onMessageSent() {
        messagesSentSinceUpdate++;
    }
    
    /**
     * Setzt den Blueprint-Count zurück und sendet den Wert sofort an den Server
     * Verwendet den speziellen Reset-Endpoint, der blueprints_found in der SQL-Datenbank zurücksetzt
     */
    public void resetBlueprintCount() {
        currentBlueprintCount = 0;
        
        // Sende Reset-Request an den Server (setzt blueprints_found in SQL auf 0)
        if (isEnabled && httpClient != null) {
            LeaderboardManager manager = LeaderboardManager.getInstance();
            if (manager.isEnabled() && manager.isRegistered()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        JsonObject payload = new JsonObject();
                        // Leeres Payload ist OK, der Endpoint setzt blueprints_found automatisch auf 0
                        
                        JsonObject response = httpClient.postWithToken(
                            "/profile/reset-blueprints",
                            payload,
                            manager.getPlayerToken()
                        );
                        
                        if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
                            // Erfolgreich zurückgesetzt
                        }
                    } catch (Exception e) {
                        // Ignoriere Fehler
                    }
                });
            }
        }
    }
    
    /**
     * Wird aufgerufen wenn ein neuer Max-Wert erreicht wurde
     */
    public void updateMaxCoins(long coins) {
        if (coins > maxCoins) {
            maxCoins = coins;
        }
    }
    
    public void updateMaxDamage(long damage) {
        if (damage > maxDamage) {
            maxDamage = damage;
        }
    }
    
    public void updateHighestWave(int wave) {
        if (wave > highestWave) {
            highestWave = wave;
        }
    }
    
    
    // Getter
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { this.isEnabled = enabled; }
    
    private boolean isDebugEnabled() {
        try {
            return CCLiveUtilitiesConfig.HANDLER.instance().playerStatsDebugging;
        } catch (Exception e) {
            return false;
        }
    }
}


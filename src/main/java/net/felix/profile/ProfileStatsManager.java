package net.felix.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.felix.CCLiveUtilities;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.http.HttpClient;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.collectors.FloorProgressCollector;
import net.felix.leaderboards.collectors.CoinCollector;
import net.felix.leaderboards.collectors.DataCollector;
import net.felix.utilities.Aincraft.CardsStatuesUtility;
import net.felix.utilities.Aincraft.BPViewerUtility;
import net.felix.utilities.Overall.ZeichenUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    
    // Validierungs-Variablen für Karten/Statuen-Menüs
    private final Map<Integer, ItemStack> lastKnownCardsItems = new HashMap<>();
    private final Map<Integer, ItemStack> lastKnownStatuesItems = new HashMap<>();
    
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
    
    // Flag: Ist der Spieler auf dem erlaubten Server für Playtime-Tracking?
    // Wird beim Server-Join gesetzt und beim Disconnect zurückgesetzt
    private boolean isOnAllowedServerForPlaytime = false;
    
    // Karten/Statuen-Level (Name -> Level)
    private final Map<String, Integer> cardsLevels = new HashMap<>();
    private final Map<String, Integer> cardPrestigeLevels = new HashMap<>();
    private final Map<String, Integer> statuesLevels = new HashMap<>();
    
    // JSON-Datei für persistente Speicherung
    private static final String SAVE_FILE_NAME = "cards_statues_levels.json";
    private final File saveFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Status
    private boolean isEnabled = true;
    private boolean isInitialized = false;
    
    private ProfileStatsManager() {
        LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
        this.httpClient = leaderboardManager.getHttpClient();
        
        // Erstelle File-Pfad für JSON-Datei
        Path configDir = CCLiveUtilities.getConfigDir().resolve("cclive-utilities");
        this.saveFile = configDir.resolve(SAVE_FILE_NAME).toFile();
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
        
        // Lade gespeicherte Karten/Statuen-Level aus JSON-Datei
        loadCardsStatuesLevels();
        
        // Registriere Screen-Event für Validierung von Karten/Statuen-Menüs
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen) {
                String title = screen.getTitle().getString();
                if (title == null) {
                    return;
                }
                
                // Entferne Formatierungscodes für Vergleich
                String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "");
                
                // Prüfe ob es das Karten-Menü (㭆) oder Statuen-Menü (㭂) ist
                boolean isCardsMenu = ZeichenUtility.isCardsMenuTitle(cleanTitle);
                boolean isStatuesMenu = ZeichenUtility.isStatuesMenuTitle(cleanTitle);
                
                if (isCardsMenu) {
                    // Warte kurz bis Inventar geladen ist, dann scanne
                    new Thread(() -> {
                        try {
                            Thread.sleep(500); // 500ms warten
                            if (client.gui.screen() == screen && client.gui.screen() instanceof AbstractContainerScreen) {
                                scanCardsMenu((AbstractContainerScreen<?>) client.gui.screen(), client);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                } else if (isStatuesMenu) {
                    // Warte kurz bis Inventar geladen ist, dann scanne
                    new Thread(() -> {
                        try {
                            Thread.sleep(500); // 500ms warten
                            if (client.gui.screen() == screen && client.gui.screen() instanceof AbstractContainerScreen) {
                                scanStatuesMenu((AbstractContainerScreen<?>) client.gui.screen(), client);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            }
        });
        
        // Registriere Tick-Event für periodische Updates
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        // Registriere Server-Join Event (Reset + Server-Prüfung)
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
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
            
            // Reset Server-Flag
            isOnAllowedServerForPlaytime = false;
        });
        
        isInitialized = true;
        if (isDebugEnabled()) {
            // Silent error handling("[ProfileStats] ✅ ProfileStatsManager initialisiert");
        }
    }
    
    /**
     * Wird jeden Tick aufgerufen
     */
    private void onClientTick(Minecraft client) {
        if (!isEnabled || client.player == null || client.level == null) {
            return;
        }
        
        // Playtime NUR auf Multiplayer-Servern UND auf dem erlaubten Server tracken
        // getNetworkHandler() ist null im Singleplayer, nicht null auf Servern
        // isOnAllowedServerForPlaytime wurde bereits beim Server-Join geprüft (effizienter)
        boolean isOnServer = client.getConnection() != null;
        
        if (isOnServer && isOnAllowedServerForPlaytime) {
            // Playtime tracken (für Leaderboard) - nur auf dem erlaubten Server
            playtimeTicks++;
            
            // Playtime regelmäßig an Leaderboard senden
            if (playtimeTicks >= PLAYTIME_UPDATE_INTERVAL) {
                if (isDebugEnabled()) {
                    // Silent error handling("⏱️ [ProfileStats] PLAYTIME_UPDATE_INTERVAL erreicht - playtimeTicks=" + playtimeTicks);
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
            // Führe collectAndSendStats asynchron aus, um Freezes zu vermeiden
            CompletableFuture.runAsync(() -> {
                collectAndSendStats();
            });
        }
        
        // Prüfe ob Karten/Statuen-Menü geöffnet ist und ob sich Items geändert haben
        // Nur alle 10 Ticks prüfen (0.5 Sekunden) um Performance zu verbessern
        if (client.player.tickCount % 10 == 0) {
            checkCardsStatuesMenuForChanges(client);
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
    private boolean isOnAllowedServer(Minecraft client) {
        // Server-Prüfung überspringen für Testing
        if (DISABLE_SERVER_CHECK) {
            return true; // Immer erlauben wenn Server-Check deaktiviert
        }
        
        // Wenn beide leer sind, tracke auf allen Servern
        if ((ALLOWED_SERVER_ADDRESS == null || ALLOWED_SERVER_ADDRESS.isEmpty()) &&
            (ALLOWED_SERVER_NAME == null || ALLOWED_SERVER_NAME.isEmpty())) {
            if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] ⚠️ Keine Server-Prüfung konfiguriert → Playtime-Tracking auf allen Servern aktiviert");
            }
            return true;
        }
        
        boolean matchesAddress = true;
        boolean matchesName = true;
        
        // Prüfe Server-IP/Port
        if (ALLOWED_SERVER_ADDRESS != null && !ALLOWED_SERVER_ADDRESS.isEmpty()) {
            try {
                String currentAddress = null;
                if (client.getConnection() != null && client.getConnection().getConnection() != null) {
                    var address = client.getConnection().getConnection().getRemoteAddress();
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
                        // Silent error handling("[ProfileStats] Server-Adresse: '" + currentAddress + "' (erwartet: '" + ALLOWED_SERVER_ADDRESS + "') → " + (matchesAddress ? "✅" : "❌"));
                    }
                } else {
                    matchesAddress = false;
                    if (isDebugEnabled()) {
                        // Silent error handling("[ProfileStats] ❌ Server-Adresse konnte nicht ermittelt werden");
                    }
                }
            } catch (Exception e) {
                // Silent error handling("[ProfileStats] Fehler beim Prüfen der Server-Adresse: " + e.getMessage());
                matchesAddress = false;
            }
        }
        
        // Prüfe Server-Name aus der Liste
        if (ALLOWED_SERVER_NAME != null && !ALLOWED_SERVER_NAME.isEmpty()) {
            try {
                String currentName = null;
                if (client.getCurrentServer() != null) {
                    currentName = client.getCurrentServer().name;
                } else if (client.getConnection() != null && client.getConnection().getServerData() != null) {
                    currentName = client.getConnection().getServerData().name;
                }
                
                if (currentName != null) {
                    matchesName = currentName.equalsIgnoreCase(ALLOWED_SERVER_NAME);
                    if (isDebugEnabled()) {
                        // Silent error handling("[ProfileStats] Server-Name: '" + currentName + "' (erwartet: '" + ALLOWED_SERVER_NAME + "') → " + (matchesName ? "✅" : "❌"));
                    }
                } else {
                    matchesName = false;
                    if (isDebugEnabled()) {
                        // Silent error handling("[ProfileStats] ❌ Server-Name konnte nicht ermittelt werden");
                    }
                }
            } catch (Exception e) {
                // Silent error handling("[ProfileStats] Fehler beim Prüfen des Server-Namens: " + e.getMessage());
                matchesName = false;
            }
        }
        
        // Wenn beide gesetzt sind, müssen beide übereinstimmen
        // Wenn nur eine gesetzt ist, muss diese übereinstimmen
        boolean isAllowed = matchesAddress && matchesName;
        
        if (isDebugEnabled()) {
            if (isAllowed) {
                // Silent error handling("[ProfileStats] ✅ Spieler ist auf dem erlaubten Server → Playtime-Tracking aktiviert");
            } else {
                // Silent error handling("[ProfileStats] ⚠️ Spieler ist NICHT auf dem erlaubten Server → Playtime-Tracking deaktiviert");
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
            // Silent error handling("⏱️ [ProfileStats] sendPlaytimeToLeaderboard() aufgerufen");
            // Silent error handling("⏱️ [ProfileStats] isRegistered: " + leaderboardManager.isRegistered());
            // Silent error handling("⏱️ [ProfileStats] playtimeTicks: " + playtimeTicks);
            // Silent error handling("⏱️ [ProfileStats] isOnAllowedServerForPlaytime: " + isOnAllowedServerForPlaytime);
        }
        
        LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
        if (!leaderboardManager.isRegistered()) {
            if (debug) {
                // Silent error handling("⏱️ [ProfileStats] ABGEBROCHEN - nicht registriert");
            }
            return;
        }
        
        // Berechne Playtime in Sekunden
        long playtimeSeconds = playtimeTicks / 20; // Ticks zu Sekunden
        if (debug) {
            // Silent error handling("⏱️ [ProfileStats] playtimeSeconds berechnet: " + playtimeSeconds);
        }
        
        if (playtimeSeconds <= 0) {
            if (debug) {
                // Silent error handling("⏱️ [ProfileStats] ABGEBROCHEN - playtimeSeconds <= 0");
            }
            return; // Keine Playtime zu senden
        }
        
        // Sende als Leaderboard "playtime" (additiv)
        // Der Server summiert automatisch
        // Verwende updateScoreAdditive() damit additive Updates nicht blockiert werden
        if (debug) {
            // Silent error handling("⏱️ [ProfileStats] Rufe updateScoreAdditive auf für playtime = " + playtimeSeconds + " Sekunden");
        }
        leaderboardManager.updateScoreAdditive("playtime", playtimeSeconds);
        
        if (debug) {
            // Silent error handling("[ProfileStats] ⏱️ Playtime gesendet: " + playtimeSeconds + " Sekunden (gesamt: " + (playtimeTicks / 20) + " Ticks)");
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
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        
        // 1. Höchster Floor (BPViewerUtility / FloorProgressCollector)
        try {
            String currentFloor = null;
            BPViewerUtility bpViewer = BPViewerUtility.getInstance();
            if (bpViewer != null) {
                currentFloor = bpViewer.getActiveFloor();
            }
            if (currentFloor == null) {
                LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
                DataCollector floorCollector = leaderboardManager.getCollector("floors");
                if (floorCollector instanceof FloorProgressCollector) {
                    currentFloor = ((FloorProgressCollector) floorCollector).getCurrentFloor();
                }
            }
            if (currentFloor != null && currentFloor.startsWith("floor_")) {
                try {
                    int floor = Integer.parseInt(currentFloor.substring("floor_".length()));
                    if (floor > highestFloor) {
                        highestFloor = floor;
                    }
                } catch (NumberFormatException e) {
                    // Ignoriere
                }
            }
        } catch (Exception e) {
            // Ignoriere Fehler
        }
        
        // 2. Karten-Level (aus CardsStatuesUtility)
        boolean cardUpdated = false;
        try {
            CardsStatuesUtility.CardData currentCard = CardsStatuesUtility.getCurrentCard();
            if (currentCard != null && currentCard.getName() != null && currentCard.getLevel() != null) {
                try {
                    int level = Integer.parseInt(currentCard.getLevel());
                    String cardName = currentCard.getName();
                    if (rememberCard(cardName, level, currentCard.getPrestige())) {
                        cardUpdated = true;
                        if (isDebugEnabled()) {
                            // Silent error handling("[ProfileStats] ✅ Karte aus Chat aktualisiert: " + cardName + " = Level " + level);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignoriere
                }
            }
        } catch (Exception e) {
            // Ignoriere Fehler
        }
        
        // 3. Statuen-Level (aus CardsStatuesUtility)
        boolean statueUpdated = false;
        try {
            CardsStatuesUtility.StatueData currentStatue = CardsStatuesUtility.getCurrentStatue();
            if (currentStatue != null && currentStatue.getName() != null && currentStatue.getLevel() != null) {
                try {
                    int level = Integer.parseInt(currentStatue.getLevel());
                    String statueName = currentStatue.getName();
                    // Nur updaten wenn höher
                    int oldLevel = statuesLevels.getOrDefault(statueName, 0);
                    if (level > oldLevel) {
                        statuesLevels.put(statueName, level);
                        statueUpdated = true;
                        if (isDebugEnabled()) {
                            // Silent error handling("[ProfileStats] ✅ Statue aus Chat aktualisiert: " + statueName + " = Level " + level);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignoriere
                }
            }
        } catch (Exception e) {
            // Ignoriere Fehler
        }
        
        // Speichere sofort in JSON-Datei wenn Karte/Statue aus Chat aktualisiert wurde
        if (cardUpdated || statueUpdated) {
            saveCardsStatuesLevels();
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
                // Silent error handling("[ProfileStats] ❌ Stats-Update abgebrochen: Leaderboard nicht registriert oder kein Token");
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
        String chosenStat = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().hoverStatsChosenStat != null 
            ? net.felix.CCLiveUtilitiesConfig.HANDLER.instance().hoverStatsChosenStat.getValue() 
            : "playtime";
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

            JsonObject prestigeJson = new JsonObject();
            for (Map.Entry<String, Integer> entry : cardsLevels.entrySet()) {
                int prestige = cardPrestigeLevels.getOrDefault(entry.getKey(), 0);
                prestigeJson.addProperty(entry.getKey(), prestige);
            }
            payload.add("card_prestiges", prestigeJson);
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
                // Silent error handling("[ProfileStats] ⚠️ Stats-Update übersprungen (Payload leer)");
            }
            return;
        }

        if (isDebugEnabled()) {
            // Silent error handling("[ProfileStats] 📤 Sende Stats-Update " + (isFinal ? "(final)" : "") + ": " + payload.toString());
        }

        // Sende Update asynchron
        CompletableFuture.supplyAsync(() -> {
            try {
                httpClient.postWithToken("/profile/update", payload, leaderboardManager.getPlayerToken());
                return true;
            } catch (Exception e) {
                // Silent error handling("❌ Fehler beim Senden von Profil-Stats: " + e.getMessage());
                return false;
            }
        }).thenAccept(success -> {
            if (success && !isFinal) {
                // Reset additive Werte nach erfolgreichem Update
                resetAdditiveCounters();
                // Speichere Karten/Statuen-Level nach erfolgreichem Update
                saveCardsStatuesLevels();
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
     * WICHTIG: cardsLevels und statuesLevels werden NICHT geleert,
     * da diese aus der JSON-Datei geladen werden sollen
     */
    private void resetCounters() {
        resetAdditiveCounters();
        playtimeTicks = 0; // Playtime auch zurücksetzen beim Join
        highestFloor = 0;
        highestWave = 0;
        maxCoins = 0;
        maxDamage = 0;
        currentBlueprintCount = 0;
        // NICHT löschen: cardsLevels und statuesLevels bleiben erhalten
        // Diese werden beim Start aus der JSON-Datei geladen
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
            if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] ⚔️ Neuer Max-Damage: " + maxDamage);
            }
        }
    }
    
    public void updateHighestWave(int wave) {
        if (wave > highestWave) {
            highestWave = wave;
        }
    }
    
    /**
     * Tooltip-Zeile 2 ist {@code 㔛㔙㔖§#FFB600♦㔛㔙㔖} für Prestige 1. Gezählt wird nur die goldene Raute.
     * Ohne Prestige ist Zeile 2 leer, das Level bleibt in Zeile 3.
     * Ein Stern in §#6E627D ist Level 0. Jeder Stern in §#FFD477 ist ein Level, maximal 5.
     * Nach einem Prestige beginnt das Kartenlevel wieder bei 1, die Rauten steigen.
     */
    private record CardProgress(int level, int prestige) {}

    private static CardProgress readCardProgress(List<Component> lines) {
        int prestige = 0;
        for (Component line : lines) {
            prestige += countDiamonds(line);
        }
        int level = lines.size() > 2 ? readCardLevelLine(lines.get(2)) : -1;
        return new CardProgress(level, prestige);
    }

    private static int readCardLevelLine(Component line) {
        if (line == null) {
            return -1;
        }
        String text = line.getString();
        if (text != null && text.contains("Maximale Stufe erreicht!")) {
            return 5;
        }
        int[] stars = countStars(line);
        if (stars[0] == 0 && stars[1] == 0) {
            return -1;
        }
        return Math.min(stars[0], 5);
    }

    /**
     * Prestige steht als goldene Raute in der Zeile, umgeben von Pixel-Spacern:
     * {@code 㔛㔙㔖§#FFB600♦㔛㔙㔖} ist Prestige 1.
     * Gezählt wird nur ♦ hinter der Farbe #FFB600.
     */
    private static int countDiamonds(Component line) {
        if (line == null) {
            return 0;
        }
        int fromText = countGoldDiamonds(line.getString(), false);
        int[] fromStyle = new int[1];
        line.visit((style, string) -> {
            if (string == null || string.isEmpty()) {
                return Optional.empty();
            }
            boolean gold = style.getColor() != null && (style.getColor().getValue() & 0xFFFFFF) == 0xFFB600;
            fromStyle[0] += countGoldDiamonds(string, gold);
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return Math.max(fromText, fromStyle[0]);
    }

    private static int countGoldDiamonds(String text, boolean gold) {
        int count = 0;
        for (int i = 0; i < text.length(); ) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                if (text.charAt(i + 1) == '#' && i + 7 < text.length()) {
                    gold = text.substring(i + 2, i + 8).equalsIgnoreCase("FFB600");
                    i += 8;
                    continue;
                }
                i += 2;
                continue;
            }
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            if (gold && codePoint == '♦') {
                count++;
            }
        }
        return count;
    }

    /**
     * Prestige zählt stärker als das Kartenlevel, weil das Level nach einem Prestige wieder von vorne beginnt.
     */
    private boolean rememberCard(String cardName, int level, int prestige) {
        if (cardName == null || cardName.isEmpty() || level < 0 || prestige < 0) {
            return false;
        }
        int oldLevel = cardsLevels.getOrDefault(cardName, 0);
        int oldPrestige = cardPrestigeLevels.getOrDefault(cardName, 0);
        boolean known = cardsLevels.containsKey(cardName);
        if (!known || prestige > oldPrestige || (prestige == oldPrestige && level != oldLevel)) {
            cardsLevels.put(cardName, level);
            cardPrestigeLevels.put(cardName, prestige);
            return true;
        }
        return false;
    }

    /**
     * Liest die Statuen-Stufe aus allen Tooltip-Zeilen.
     * Normale Ziffern und die Aincraft-Ziffernschrift nach "Stufe" zählen.
     */
    private static int readStatueLevel(List<String> lines) {
        int level = 0;
        boolean maxReached = false;
        for (String lineText : lines) {
            if (lineText == null) {
                continue;
            }
            String cleanLine = lineText.replaceAll("§[0-9a-fk-or]", "");
            if (cleanLine.contains("Maximale Stufe erreicht!")) {
                maxReached = true;
                continue;
            }
            if (!cleanLine.contains("Stufe") || cleanLine.contains("Nächste")) {
                continue;
            }
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("Stufe[\\s:]*?(\\d+)").matcher(cleanLine);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ignored) {
                    // weiter mit der Schrift-Ziffer
                }
            }
            int marker = cleanLine.toLowerCase().indexOf("stufe");
            if (marker >= 0) {
                int decoded = decodeAincraftNumber(cleanLine.substring(marker + "stufe".length()));
                if (decoded >= 0) {
                    return decoded;
                }
            }
        }
        if (level > 0) {
            return level;
        }
        return maxReached ? 100 : 0;
    }

    private static int decodeAincraftNumber(String text) {
        if (text == null || text.isEmpty()) {
            return -1;
        }
        java.util.Map<Character, Integer> digits = ZeichenUtility.getAincraftBottomFontNumbers();
        if (digits == null || digits.isEmpty()) {
            return -1;
        }
        StringBuilder number = new StringBuilder();
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (codePoint > 0xFFFF) {
                continue;
            }
            Integer digit = digits.get((char) codePoint);
            if (digit != null) {
                number.append(digit);
            }
        }
        if (number.length() == 0) {
            return -1;
        }
        try {
            return Integer.parseInt(number.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Such-Platzhalter (schwarzer Beton mit angehängtem [Ausgeblendet]) sind keine eigenen Karten oder Statuen. */
    private static boolean isSearchHiddenItem(ItemStack itemStack) {
        String name = itemStack.getHoverName().getString();
        return name != null && name.contains("[Ausgeblendet]");
    }

    private static String stripSearchHiddenMarker(String name) {
        if (name == null) {
            return "";
        }
        return name.replace(" [Ausgeblendet]", "").replace("[Ausgeblendet]", "").trim();
    }

    /**
     * Lädt Karten/Statuen-Level aus der JSON-Datei
     */
    private void loadCardsStatuesLevels() {
        if (!saveFile.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(saveFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            
            // Lade Karten-Level
            boolean cleanedHiddenNames = false;
            if (json.has("cards") && json.get("cards").isJsonObject()) {
                JsonObject cardsJson = json.getAsJsonObject("cards");
                cardsLevels.clear();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : cardsJson.entrySet()) {
                    String cardName = stripSearchHiddenMarker(entry.getKey());
                    int level = entry.getValue().getAsInt();
                    if (!cardName.equals(entry.getKey())) {
                        cleanedHiddenNames = true;
                    }
                    if (!cardName.isEmpty()) {
                        int prestige = 0;
                        cardsLevels.put(cardName, level);
                        cardPrestigeLevels.putIfAbsent(cardName, prestige);
                    }
                }
            }
            if (json.has("card_prestiges") && json.get("card_prestiges").isJsonObject()) {
                JsonObject prestigeJson = json.getAsJsonObject("card_prestiges");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : prestigeJson.entrySet()) {
                    String cardName = stripSearchHiddenMarker(entry.getKey());
                    int prestige = entry.getValue().getAsInt();
                    if (!cardName.equals(entry.getKey())) {
                        cleanedHiddenNames = true;
                    }
                    if (!cardName.isEmpty() && prestige >= 0) {
                        cardPrestigeLevels.merge(cardName, prestige, Math::max);
                    }
                }
            }
            
            // Lade Statuen-Level
            if (json.has("statues") && json.get("statues").isJsonObject()) {
                JsonObject statuesJson = json.getAsJsonObject("statues");
                statuesLevels.clear();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : statuesJson.entrySet()) {
                    String statueName = stripSearchHiddenMarker(entry.getKey());
                    int level = entry.getValue().getAsInt();
                    if (!statueName.equals(entry.getKey())) {
                        cleanedHiddenNames = true;
                    }
                    if (!statueName.isEmpty()) {
                        statuesLevels.merge(statueName, level, Math::max);
                    }
                }
            }
            if (cleanedHiddenNames) {
                saveCardsStatuesLevels();
            }
        } catch (IOException e) {
            // Silent error handling("❌ [ProfileStats] Fehler beim Laden von Karten/Statuen-Level: " + e.getMessage());
        }
    }
    
    /**
     * Speichert Karten/Statuen-Level in die JSON-Datei (asynchron, um Freezes zu vermeiden)
     */
    private void saveCardsStatuesLevels() {
        // Erstelle Kopien der Maps für asynchrones Schreiben (Thread-sicher)
        final Map<String, Integer> cardsCopy = new HashMap<>(cardsLevels);
        final Map<String, Integer> prestigeCopy = new HashMap<>(cardPrestigeLevels);
        final Map<String, Integer> statuesCopy = new HashMap<>(statuesLevels);
        
        // Führe File I/O asynchron aus, um Freezes zu vermeiden
        CompletableFuture.runAsync(() -> {
            try {
                // Erstelle Verzeichnis falls nicht vorhanden
                saveFile.getParentFile().mkdirs();
                
                JsonObject json = new JsonObject();
                
                // Speichere Karten-Level
                JsonObject cardsJson = new JsonObject();
                for (Map.Entry<String, Integer> entry : cardsCopy.entrySet()) {
                    cardsJson.addProperty(entry.getKey(), entry.getValue());
                }
                json.add("cards", cardsJson);

                JsonObject prestigeJson = new JsonObject();
                for (Map.Entry<String, Integer> entry : cardsCopy.entrySet()) {
                    prestigeJson.addProperty(entry.getKey(), prestigeCopy.getOrDefault(entry.getKey(), 0));
                }
                json.add("card_prestiges", prestigeJson);
                
                // Speichere Statuen-Level
                JsonObject statuesJson = new JsonObject();
                for (Map.Entry<String, Integer> entry : statuesCopy.entrySet()) {
                    statuesJson.addProperty(entry.getKey(), entry.getValue());
                }
                json.add("statues", statuesJson);
                
                // Schreibe in Datei
                try (FileWriter writer = new FileWriter(saveFile)) {
                    gson.toJson(json, writer);
                }
            } catch (IOException e) {
                // Silent error handling("❌ [ProfileStats] Fehler beim Speichern von Karten/Statuen-Level: " + e.getMessage());
            }
        });
    }
    
    
    /**
     * Wird aufgerufen, wenn eine Karte aus dem Chat gelesen wurde
     */
    public void onCardFromChat(CardsStatuesUtility.CardData cardData) {
        if (cardData == null) {
            if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] ⚠️ onCardFromChat: cardData ist null");
            }
            return;
        }
        if (cardData.getName() == null || cardData.getName().isEmpty()) {
            if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] ⚠️ onCardFromChat: Karten-Name ist null oder leer");
            }
            return;
        }
        if (cardData.getLevel() == null || cardData.getLevel().isEmpty()) {
            if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] ⚠️ onCardFromChat: Karten-Level ist null oder leer für: " + cardData.getName());
            }
            return;
        }
        try {
            int level = Integer.parseInt(cardData.getLevel());
            String cardName = cardData.getName();
            if (rememberCard(cardName, level, cardData.getPrestige())) {
                saveCardsStatuesLevels();
                if (isDebugEnabled()) {
                    // Silent error handling("[ProfileStats] ✅ Karte aus Chat sofort gespeichert: " + cardName + " = Level " + level);
                }
            } else if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] ⚠️ Karten-Level nicht höher: " + cardName + " (alt: " + oldLevel + ", neu: " + level + ")");
            }
        } catch (NumberFormatException e) {
            if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] ❌ Fehler beim Parsen von Karten-Level: " + cardData.getLevel() + " für " + cardData.getName());
            }
        }
    }
    
    /**
     * Wird aufgerufen, wenn eine Statue aus dem Chat gelesen wurde
     */
    public void onStatueFromChat(CardsStatuesUtility.StatueData statueData) {
        if (statueData == null) {
            if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] ⚠️ onStatueFromChat: statueData ist null");
            }
            return;
        }
        if (statueData.getName() == null || statueData.getName().isEmpty()) {
            if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] ⚠️ onStatueFromChat: Statue-Name ist null oder leer");
            }
            return;
        }
        if (statueData.getLevel() == null || statueData.getLevel().isEmpty()) {
            if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] ⚠️ onStatueFromChat: Statue-Level ist null oder leer für: " + statueData.getName());
            }
            return;
        }
        try {
            int level = Integer.parseInt(statueData.getLevel());
            String statueName = statueData.getName();
            // Nur updaten wenn höher
            int oldLevel = statuesLevels.getOrDefault(statueName, 0);
            if (level > oldLevel) {
                statuesLevels.put(statueName, level);
                saveCardsStatuesLevels();
                if (isDebugEnabled()) {
                    // Silent error handling("[ProfileStats] ✅ Statue aus Chat sofort gespeichert: " + statueName + " = Level " + level);
                }
            } else if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] ⚠️ Statue-Level nicht höher: " + statueName + " (alt: " + oldLevel + ", neu: " + level + ")");
            }
        } catch (NumberFormatException e) {
            if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] ❌ Fehler beim Parsen von Statue-Level: " + statueData.getLevel() + " für " + statueData.getName());
            }
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
    
    /**
     * Scannt das Karten-Menü und validiert/aktualisiert Karten-Level
     */
    private void scanCardsMenu(AbstractContainerScreen<?> screen, Minecraft client) {
        if (screen.getMenu() == null || client.player == null) {
            return;
        }
        
        // Scanne alle Slots (Karten können überall sein)
        lastKnownCardsItems.clear();
        for (int slotIndex = 0; slotIndex < screen.getMenu().slots.size(); slotIndex++) {
            Slot slot = screen.getMenu().slots.get(slotIndex);
            ItemStack itemStack = slot.getItem();
            
            // Speichere Item für Vergleich (auch wenn leer)
            if (itemStack != null && !itemStack.isEmpty()) {
                lastKnownCardsItems.put(slotIndex, itemStack.copy());
            } else {
                lastKnownCardsItems.remove(slotIndex);
            }
            
            if (itemStack == null || itemStack.isEmpty() || isSearchHiddenItem(itemStack)) {
                continue;
            }
            
            // Lese Tooltip des Items
            List<Component> tooltip = getItemTooltip(itemStack, client.player);
            if (tooltip == null || tooltip.isEmpty()) {
                continue;
            }
            
            List<String> lines = new ArrayList<>();
            boolean isCard = false;
            for (Component line : tooltip) {
                String lineText = line.getString();
                if (lineText == null) {
                    continue;
                }
                lines.add(lineText);
                if (lineText.contains("[Karte]")) {
                    isCard = true;
                }
            }
            if (!isCard || lines.isEmpty()) {
                continue;
            }

            String cardName = stripSearchHiddenMarker(lines.get(0)
                    .replaceAll("§[0-9a-fk-or]", "")
                    .replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "")
                    .replaceAll("\\[Karte\\]", "")
                    .trim());
            CardProgress progress = readCardProgress(tooltip);
            
            if (!cardName.isEmpty() && progress.level() >= 0) {
                rememberCard(cardName, progress.level(), progress.prestige());
            }
        }
        
        // Speichere nach Validierung
        saveCardsStatuesLevels();
    }
    
    /**
     * Scannt das Statuen-Menü und validiert/aktualisiert Statuen-Level
     */
    private void scanStatuesMenu(AbstractContainerScreen<?> screen, Minecraft client) {
        if (screen.getMenu() == null || client.player == null) {
            return;
        }
        
        // Scanne alle Slots (wie bei Karten)
        lastKnownStatuesItems.clear();
        for (int slotIndex = 0; slotIndex < screen.getMenu().slots.size(); slotIndex++) {
            
            Slot slot = screen.getMenu().slots.get(slotIndex);
            ItemStack itemStack = slot.getItem();
            
            // Speichere Item für Vergleich (auch wenn leer)
            if (itemStack != null && !itemStack.isEmpty()) {
                lastKnownStatuesItems.put(slotIndex, itemStack.copy());
            } else {
                lastKnownStatuesItems.remove(slotIndex);
            }
            
            if (itemStack == null || itemStack.isEmpty() || isSearchHiddenItem(itemStack)) {
                continue;
            }
            
            // Lese Tooltip des Items
            List<Component> tooltip = getItemTooltip(itemStack, client.player);
            if (tooltip == null || tooltip.isEmpty()) {
                continue;
            }
            
            // Debug: Zeige alle Tooltip-Zeilen
            if (isDebugEnabled()) {
                // Silent error handling("[ProfileStats] 🔍 Tooltip-Zeilen für Statue:");
                for (int i = 0; i < tooltip.size(); i++) {
                    // Silent error handling("  [" + i + "] " + tooltip.get(i).getString());
                }
            }
            
            // [Statue] und "Stufe" können in beliebiger Reihenfolge stehen
            List<String> lines = new ArrayList<>();
            boolean isStatue = false;
            for (Component line : tooltip) {
                String lineText = line.getString();
                if (lineText == null) {
                    continue;
                }
                lines.add(lineText);
                if (lineText.contains("[Statue]")) {
                    isStatue = true;
                }
            }
            if (!isStatue) {
                continue;
            }

            String statueName = null;
            if (!lines.isEmpty()) {
                statueName = lines.get(0)
                        .replaceAll("§[0-9a-fk-or]", "")
                        .replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "")
                        .replaceAll("\\[Statue\\]", "")
                        .trim();
                statueName = stripSearchHiddenMarker(statueName);
            }
            int statueLevel = readStatueLevel(lines);
            
            // Wenn Statue gefunden, aktualisiere Level (nur wenn höher)
            if (isStatue && statueName != null && !statueName.isEmpty() && statueLevel > 0) {
                int currentLevel = statuesLevels.getOrDefault(statueName, 0);
                if (statueLevel > currentLevel) {
                    statuesLevels.put(statueName, statueLevel);
                    if (isDebugEnabled()) {
                        // Silent error handling("[ProfileStats] ✅ Statue validiert/aktualisiert: " + statueName + " = Level " + statueLevel);
                    }
                } else if (isDebugEnabled()) {
                    // Silent error handling("[ProfileStats] ⚠️ Statue-Level nicht höher: " + statueName + " (alt: " + currentLevel + ", neu: " + statueLevel + ")");
                }
            } else if (isDebugEnabled() && isStatue) {
                // Silent error handling("[ProfileStats] ⚠️ Statue erkannt aber nicht gespeichert - Name: " + statueName + ", Level: " + statueLevel);
            }
        }
        
        // Speichere nach Validierung
        saveCardsStatuesLevels();
    }
    
    /**
     * Liest Tooltip-Daten (Lore) aus einem ItemStack
     */
    private List<Component> getItemTooltip(ItemStack itemStack, net.minecraft.world.entity.player.Player player) {
        List<Component> tooltip = new ArrayList<>();
        // Füge den Item-Namen hinzu
        tooltip.add(itemStack.getHoverName());
        
        // Lese die Lore über die Data Component API
        var loreComponent = itemStack.get(DataComponents.LORE);
        if (loreComponent != null) {
            tooltip.addAll(loreComponent.lines());
        }
        return tooltip;
    }
    
    /**
     * Zählt Sterne in einer Tooltip-Zeile.
     * Index 0 sind farbige Sterne (das Kartenlevel), Index 1 ausgegraute Sterne (Level 0).
     */
    private static int[] countStars(Component line) {
        int[] counts = new int[2];
        if (line == null) {
            return counts;
        }
        String raw = line.getString();
        if (raw != null && raw.contains("§")) {
            return countStarsInFormattedText(raw);
        }
        line.visit((style, string) -> {
            if (string == null || string.isEmpty()) {
                return Optional.empty();
            }
            int starKind = starKind(style.getColor());
            for (int offset = 0; offset < string.length(); ) {
                int codePoint = string.codePointAt(offset);
                offset += Character.charCount(codePoint);
                if (codePoint == '⭐' && starKind >= 0) {
                    counts[starKind]++;
                }
            }
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return counts;
    }

    private static int[] countStarsInFormattedText(String line) {
        int[] counts = new int[2];
        int starKind = -1;
        for (int i = 0; i < line.length(); ) {
            char c = line.charAt(i);
            if (c == '§' && i + 1 < line.length()) {
                if (line.charAt(i + 1) == '#' && i + 7 < line.length()) {
                    starKind = starKindFromHex(line.substring(i + 2, i + 8));
                    i += 8;
                    continue;
                }
                i += 2;
                continue;
            }
            int codePoint = line.codePointAt(i);
            i += Character.charCount(codePoint);
            if (codePoint == '⭐' && starKind >= 0) {
                counts[starKind]++;
            }
        }
        return counts;
    }

    /** 0 = Kartenlevel, 1 = ausgegraut §#6E627D. Jede andere Sternfarbe, z. B. §#CADADB, zählt als Level. */
    private static int starKind(TextColor color) {
        if (color == null) {
            return -1;
        }
        return starKindFromHex(String.format("%06X", color.getValue() & 0xFFFFFF));
    }

    private static int starKindFromHex(String hex) {
        if (hex != null && hex.equalsIgnoreCase("6E627D")) {
            return 1;
        }
        return hex == null ? -1 : 0;
    }
    
    /**
     * Prüft ob Karten/Statuen-Menü geöffnet ist und ob sich Items geändert haben (Seitenwechsel)
     */
    private void checkCardsStatuesMenuForChanges(Minecraft client) {
        if (client.gui.screen() == null || !(client.gui.screen() instanceof AbstractContainerScreen)) {
            lastKnownCardsItems.clear();
            lastKnownStatuesItems.clear();
            return;
        }
        
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
        String title = screen.getTitle().getString();
        if (title == null) {
            return;
        }
        
        // Entferne Formatierungscodes für Vergleich
        String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "");
        
        // Prüfe ob es das Karten-Menü (㭆) oder Statuen-Menü (㭂) ist
        boolean isCardsMenu = ZeichenUtility.isCardsMenuTitle(cleanTitle);
        boolean isStatuesMenu = ZeichenUtility.isStatuesMenuTitle(cleanTitle);
        
        if (isCardsMenu) {
            // Prüfe ob sich Items geändert haben (Seitenwechsel)
            if (hasCardsMenuChanged(screen)) {
                scanCardsMenu(screen, client);
            }
        } else {
            lastKnownCardsItems.clear();
        }
        
        if (isStatuesMenu) {
            // Prüfe ob sich Items geändert haben (Seitenwechsel)
            if (hasStatuesMenuChanged(screen)) {
                scanStatuesMenu(screen, client);
            }
        } else {
            lastKnownStatuesItems.clear();
        }
    }
    
    /**
     * Prüft ob sich die Items im Karten-Menü geändert haben
     */
    private boolean hasCardsMenuChanged(AbstractContainerScreen<?> screen) {
        if (screen.getMenu() == null) {
            return false;
        }
        
        // Wenn lastKnownCardsItems leer ist, ist es der erste Scan → immer true
        if (lastKnownCardsItems.isEmpty()) {
            return true;
        }
        
        // Prüfe alle Slots
        for (int slotIndex = 0; slotIndex < screen.getMenu().slots.size(); slotIndex++) {
            Slot slot = screen.getMenu().slots.get(slotIndex);
            ItemStack currentItem = slot.getItem();
            ItemStack lastKnownItem = lastKnownCardsItems.get(slotIndex);
            
            if (currentItem == null || currentItem.isEmpty()) {
                if (lastKnownItem != null && !lastKnownItem.isEmpty()) {
                    return true; // Item wurde entfernt
                }
            } else {
                if (lastKnownItem == null || lastKnownItem.isEmpty()) {
                    return true; // Neues Item wurde hinzugefügt
                } else if (!areItemsEqual(currentItem, lastKnownItem)) {
                    return true; // Item hat sich geändert
                }
            }
        }
        
        return false;
    }
    
    /**
     * Prüft ob sich die Items im Statuen-Menü geändert haben
     */
    private boolean hasStatuesMenuChanged(AbstractContainerScreen<?> screen) {
        if (screen.getMenu() == null) {
            return false;
        }
        
        // Wenn lastKnownStatuesItems leer ist, ist es der erste Scan → immer true
        if (lastKnownStatuesItems.isEmpty()) {
            return true;
        }
        
        // Prüfe alle Slots (wie bei Karten)
        for (int slotIndex = 0; slotIndex < screen.getMenu().slots.size(); slotIndex++) {
            
            Slot slot = screen.getMenu().slots.get(slotIndex);
            ItemStack currentItem = slot.getItem();
            ItemStack lastKnownItem = lastKnownStatuesItems.get(slotIndex);
            
            if (currentItem == null || currentItem.isEmpty()) {
                if (lastKnownItem != null && !lastKnownItem.isEmpty()) {
                    return true; // Item wurde entfernt
                }
            } else {
                if (lastKnownItem == null || lastKnownItem.isEmpty()) {
                    return true; // Neues Item wurde hinzugefügt
                } else if (!areItemsEqual(currentItem, lastKnownItem)) {
                    return true; // Item hat sich geändert
                }
            }
        }
        
        return false;
    }
    
    /**
     * Prüft ob zwei ItemStacks gleich sind (für Vergleich)
     */
    private boolean areItemsEqual(ItemStack item1, ItemStack item2) {
        if (item1 == item2) {
            return true;
        }
        if (item1 == null || item2 == null) {
            return false;
        }
        // Vergleiche Item-Typ und Name
        if (item1.getItem() != item2.getItem()) {
            return false;
        }
        // Vergleiche Custom Name
        String name1 = item1.getHoverName().getString();
        String name2 = item2.getHoverName().getString();
        return name1.equals(name2);
    }
}

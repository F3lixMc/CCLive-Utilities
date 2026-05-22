package net.felix.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    
    // Validierungs-Variablen für Karten/Statuen-Menüs
    private boolean isInCardsMenu = false;
    private boolean isInStatuesMenu = false;
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
    
    // Cache für aktuellen Server-Namen (wird beim Join gesetzt)
    private String currentServerName = null;
    
    // Flag: Ist der Spieler auf dem erlaubten Server für Playtime-Tracking?
    // Wird beim Server-Join gesetzt und beim Disconnect zurückgesetzt
    private boolean isOnAllowedServerForPlaytime = false;
    
    // Karten/Statuen-Level (Name -> Level)
    private final Map<String, Integer> cardsLevels = new HashMap<>();
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
            if (screen instanceof HandledScreen) {
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
                            if (client.currentScreen == screen && client.currentScreen instanceof HandledScreen) {
                                scanCardsMenu((HandledScreen<?>) client.currentScreen, client);
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
                            if (client.currentScreen == screen && client.currentScreen instanceof HandledScreen) {
                                scanStatuesMenu((HandledScreen<?>) client.currentScreen, client);
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
            // Silent error handling("[ProfileStats] ✅ ProfileStatsManager initialisiert");
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
        if (client.player.age % 10 == 0) {
            checkCardsStatuesMenuForChanges(client);
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
                // Silent error handling("=== [ProfileStats] Server-Name Debug Log ===");
            }
            
            // Methode 1: getCurrentServerEntry (Server-Liste)
            String serverEntryName = null;
            try {
                if (client.getCurrentServerEntry() != null) {
                    serverEntryName = client.getCurrentServerEntry().name;
                    if (debug) {
                        // Silent error handling("[ProfileStats] getCurrentServerEntry().name = '" + serverEntryName + "'");
                    }
                } else {
                    if (debug) {
                        // Silent error handling("[ProfileStats] getCurrentServerEntry() = null");
                    }
                }
            } catch (Exception e) {
                if (debug) {
                    // Silent error handling("[ProfileStats] getCurrentServerEntry() Fehler: " + e.getMessage());
                }
            }
            
            // Methode 2: NetworkHandler ServerInfo (wenn verbunden)
            String serverInfoName = null;
            try {
                if (client.getNetworkHandler() != null) {
                    if (client.getNetworkHandler().getServerInfo() != null) {
                        serverInfoName = client.getNetworkHandler().getServerInfo().name;
                        if (debug) {
                            // Silent error handling("[ProfileStats] getNetworkHandler().getServerInfo().name = '" + serverInfoName + "'");
                        }
                    } else {
                        if (debug) {
                            // Silent error handling("[ProfileStats] getNetworkHandler().getServerInfo() = null");
                        }
                    }
                } else {
                    if (debug) {
                        // Silent error handling("[ProfileStats] getNetworkHandler() = null");
                    }
                }
            } catch (Exception e) {
                if (debug) {
                    // Silent error handling("[ProfileStats] getNetworkHandler() Fehler: " + e.getMessage());
                }
            }
            
            // Entscheide welchen Wert verwenden
            if (serverEntryName != null && !serverEntryName.isEmpty()) {
                currentServerName = serverEntryName;
                if (debug) {
                    // Silent error handling("[ProfileStats] ✅ Verwende getCurrentServerEntry: '" + currentServerName + "'");
                }
            } else if (serverInfoName != null && !serverInfoName.isEmpty()) {
                currentServerName = serverInfoName;
                if (debug) {
                    // Silent error handling("[ProfileStats] ✅ Verwende getNetworkHandler().getServerInfo(): '" + currentServerName + "'");
                }
            } else {
                currentServerName = null;
                if (debug) {
                    // Silent error handling("[ProfileStats] ❌ WARNUNG: Server-Name konnte nicht erkannt werden!");
                }
            }
            
            if (debug) {
                // Silent error handling("[ProfileStats] Erwarteter Server-Name: '" + ALLOWED_SERVER_NAME + "'");
                // Silent error handling("[ProfileStats] Aktueller Server-Name: '" + currentServerName + "'");
                // Silent error handling("[ProfileStats] Match: " + (currentServerName != null && currentServerName.equalsIgnoreCase(ALLOWED_SERVER_NAME)));
                // Silent error handling("=== [ProfileStats] Debug Log Ende ===");
            }
            
        } catch (Exception e) {
            // Silent error handling("[ProfileStats] Fehler beim Erkennen des Server-Namens: " + e.getMessage());
            // Silent error handling
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
                if (client.getCurrentServerEntry() != null) {
                    currentName = client.getCurrentServerEntry().name;
                } else if (client.getNetworkHandler() != null && client.getNetworkHandler().getServerInfo() != null) {
                    currentName = client.getNetworkHandler().getServerInfo().name;
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
            LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
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
        boolean cardUpdated = false;
        try {
            CardsStatuesUtility.CardData currentCard = CardsStatuesUtility.getCurrentCard();
            if (currentCard != null && currentCard.getName() != null && currentCard.getLevel() != null) {
                try {
                    int level = Integer.parseInt(currentCard.getLevel());
                    String cardName = currentCard.getName();
                    // Nur updaten wenn höher
                    int oldLevel = cardsLevels.getOrDefault(cardName, 0);
                    if (level > oldLevel) {
                        cardsLevels.put(cardName, level);
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
     * Lädt Karten/Statuen-Level aus der JSON-Datei
     */
    private void loadCardsStatuesLevels() {
        if (!saveFile.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(saveFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            
            // Lade Karten-Level
            if (json.has("cards") && json.get("cards").isJsonObject()) {
                JsonObject cardsJson = json.getAsJsonObject("cards");
                cardsLevels.clear();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : cardsJson.entrySet()) {
                    String cardName = entry.getKey();
                    int level = entry.getValue().getAsInt();
                    cardsLevels.put(cardName, level);
                }
            }
            
            // Lade Statuen-Level
            if (json.has("statues") && json.get("statues").isJsonObject()) {
                JsonObject statuesJson = json.getAsJsonObject("statues");
                statuesLevels.clear();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : statuesJson.entrySet()) {
                    String statueName = entry.getKey();
                    int level = entry.getValue().getAsInt();
                    statuesLevels.put(statueName, level);
                }
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
            // Nur updaten wenn höher
            int oldLevel = cardsLevels.getOrDefault(cardName, 0);
            if (level > oldLevel) {
                cardsLevels.put(cardName, level);
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
    private void scanCardsMenu(HandledScreen<?> screen, MinecraftClient client) {
        if (screen.getScreenHandler() == null || client.player == null) {
            return;
        }
        
        // Scanne alle Slots (Karten können überall sein)
        lastKnownCardsItems.clear();
        for (int slotIndex = 0; slotIndex < screen.getScreenHandler().slots.size(); slotIndex++) {
            Slot slot = screen.getScreenHandler().slots.get(slotIndex);
            ItemStack itemStack = slot.getStack();
            
            // Speichere Item für Vergleich (auch wenn leer)
            if (itemStack != null && !itemStack.isEmpty()) {
                lastKnownCardsItems.put(slotIndex, itemStack.copy());
            } else {
                lastKnownCardsItems.remove(slotIndex);
            }
            
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            
            // Lese Tooltip des Items
            List<Text> tooltip = getItemTooltip(itemStack, client.player);
            if (tooltip == null || tooltip.isEmpty()) {
                continue;
            }
            
            // Prüfe ob es eine Karte ist (Tooltip enthält "[Karte]")
            boolean isCard = false;
            String cardName = null;
            int cardLevel = 0;
            
            for (Text line : tooltip) {
                String lineText = line.getString();
                if (lineText == null) {
                    continue;
                }
                
                // Prüfe ob es eine Karte ist
                if (lineText.contains("[Karte]")) {
                    isCard = true;
                    // Name ist die erste Zeile (ohne Formatierung)
                    if (tooltip.size() > 0) {
                        String nameLine = tooltip.get(0).getString();
                        if (nameLine != null) {
                            // Entferne Formatierungscodes und Unicode
                            cardName = nameLine.replaceAll("§[0-9a-fk-or]", "")
                                             .replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "")
                                             .trim();
                            // Entferne "[Karte]" aus dem Namen
                            cardName = cardName.replaceAll("\\[Karte\\]", "").trim();
                        }
                    }
                }
                
                // Zähle Sterne (⭐) für Level
                if (isCard) {
                    int starCount = countStars(lineText);
                    if (starCount > 0) {
                        cardLevel = starCount;
                    }
                    // Backup: Maximale Stufe erreicht
                    if (lineText.contains("Maximale Stufe erreicht!")) {
                        cardLevel = 5;
                    }
                }
            }
            
            // Wenn Karte gefunden, aktualisiere Level (nur wenn höher)
            if (isCard && cardName != null && !cardName.isEmpty() && cardLevel > 0) {
                int currentLevel = cardsLevels.getOrDefault(cardName, 0);
                if (cardLevel > currentLevel) {
                    cardsLevels.put(cardName, cardLevel);
                    if (isDebugEnabled()) {
                        // Silent error handling("[ProfileStats] ✅ Karte validiert/aktualisiert: " + cardName + " = Level " + cardLevel);
                    }
                }
            }
        }
        
        // Speichere nach Validierung
        saveCardsStatuesLevels();
    }
    
    /**
     * Scannt das Statuen-Menü und validiert/aktualisiert Statuen-Level
     */
    private void scanStatuesMenu(HandledScreen<?> screen, MinecraftClient client) {
        if (screen.getScreenHandler() == null || client.player == null) {
            return;
        }
        
        // Scanne alle Slots (wie bei Karten)
        lastKnownStatuesItems.clear();
        for (int slotIndex = 0; slotIndex < screen.getScreenHandler().slots.size(); slotIndex++) {
            
            Slot slot = screen.getScreenHandler().slots.get(slotIndex);
            ItemStack itemStack = slot.getStack();
            
            // Speichere Item für Vergleich (auch wenn leer)
            if (itemStack != null && !itemStack.isEmpty()) {
                lastKnownStatuesItems.put(slotIndex, itemStack.copy());
            } else {
                lastKnownStatuesItems.remove(slotIndex);
            }
            
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            
            // Lese Tooltip des Items
            List<Text> tooltip = getItemTooltip(itemStack, client.player);
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
            
            // Prüfe ob es eine Statue ist (Tooltip enthält "[Statue]")
            boolean isStatue = false;
            String statueName = null;
            int statueLevel = 0;
            
            for (Text line : tooltip) {
                String lineText = line.getString();
                if (lineText == null) {
                    continue;
                }
                
                // Prüfe ob es eine Statue ist
                if (lineText.contains("[Statue]")) {
                    isStatue = true;
                    if (isDebugEnabled()) {
                        // Silent error handling("[ProfileStats] 🔍 [Statue] gefunden in Tooltip-Zeile: " + lineText);
                    }
                    // Name ist die erste Zeile (ohne Formatierung)
                    if (tooltip.size() > 0) {
                        String nameLine = tooltip.get(0).getString();
                        if (nameLine != null) {
                            // Entferne Formatierungscodes und Unicode
                            statueName = nameLine.replaceAll("§[0-9a-fk-or]", "")
                                                .replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "")
                                                .trim();
                            // Entferne "[Statue]" aus dem Namen
                            statueName = statueName.replaceAll("\\[Statue\\]", "").trim();
                            if (isDebugEnabled()) {
                                // Silent error handling("[ProfileStats] 🔍 Statue-Name extrahiert: " + statueName);
                            }
                        }
                    }
                }
                
                // Suche nach "Stufe: XX" für Level (auch ohne Doppelpunkt, falls Formatierungscodes dazwischen sind)
                if (isStatue) {
                    // Entferne Formatierungscodes für bessere Suche
                    String cleanLine = lineText.replaceAll("§[0-9a-fk-or]", "");
                    if (cleanLine.contains("Stufe") && !cleanLine.contains("Nächste")) {
                        if (isDebugEnabled()) {
                            // Silent error handling("[ProfileStats] 🔍 'Stufe' gefunden in Zeile: " + lineText + " (clean: " + cleanLine + ")");
                        }
                        // Suche nach Zahl nach "Stufe" (mit oder ohne Doppelpunkt)
                        String levelStr = cleanLine.replaceAll(".*[Ss]tufe\\s*:?\\s*", "").replaceAll("[^0-9].*", "").trim();
                        if (levelStr.isEmpty()) {
                            // Fallback: Entferne alles außer Zahlen
                            levelStr = cleanLine.replaceAll("[^0-9]", "").trim();
                        }
                        if (!levelStr.isEmpty()) {
                            try {
                                statueLevel = Integer.parseInt(levelStr);
                                if (isDebugEnabled()) {
                                    // Silent error handling("[ProfileStats] 🔍 Statue-Level geparst: " + statueLevel);
                                }
                            } catch (NumberFormatException e) {
                                if (isDebugEnabled()) {
                                    // Silent error handling("[ProfileStats] ❌ Fehler beim Parsen von Statue-Level: " + levelStr);
                                }
                            }
                        } else if (isDebugEnabled()) {
                            // Silent error handling("[ProfileStats] ⚠️ Keine Zahl nach 'Stufe' gefunden in: " + cleanLine);
                        }
                    }
                }
                // Backup: Maximale Stufe erreicht
                if (isStatue && lineText.contains("Maximale Stufe erreicht!")) {
                    statueLevel = 40;
                    if (isDebugEnabled()) {
                        // Silent error handling("[ProfileStats] 🔍 Statue-Level (max): 40");
                    }
                }
            }
            
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
    private List<Text> getItemTooltip(ItemStack itemStack, net.minecraft.entity.player.PlayerEntity player) {
        List<Text> tooltip = new ArrayList<>();
        // Füge den Item-Namen hinzu
        tooltip.add(itemStack.getName());
        
        // Lese die Lore über die Data Component API
        var loreComponent = itemStack.get(DataComponentTypes.LORE);
        if (loreComponent != null) {
            tooltip.addAll(loreComponent.lines());
        }
        return tooltip;
    }
    
    /**
     * Zählt die Anzahl der Sterne (⭐) in einer Zeile
     */
    private int countStars(String line) {
        if (line == null || line.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == '⭐') {
                count++;
            }
        }
        return Math.min(count, 5); // Maximal 5 Sterne
    }
    
    /**
     * Prüft ob Karten/Statuen-Menü geöffnet ist und ob sich Items geändert haben (Seitenwechsel)
     */
    private void checkCardsStatuesMenuForChanges(MinecraftClient client) {
        if (client.currentScreen == null || !(client.currentScreen instanceof HandledScreen)) {
            isInCardsMenu = false;
            isInStatuesMenu = false;
            lastKnownCardsItems.clear();
            lastKnownStatuesItems.clear();
            return;
        }
        
        HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;
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
            isInCardsMenu = true;
        } else {
            isInCardsMenu = false;
            lastKnownCardsItems.clear();
        }
        
        if (isStatuesMenu) {
            // Prüfe ob sich Items geändert haben (Seitenwechsel)
            if (hasStatuesMenuChanged(screen)) {
                scanStatuesMenu(screen, client);
            }
            isInStatuesMenu = true;
        } else {
            isInStatuesMenu = false;
            lastKnownStatuesItems.clear();
        }
    }
    
    /**
     * Prüft ob sich die Items im Karten-Menü geändert haben
     */
    private boolean hasCardsMenuChanged(HandledScreen<?> screen) {
        if (screen.getScreenHandler() == null) {
            return false;
        }
        
        // Wenn lastKnownCardsItems leer ist, ist es der erste Scan → immer true
        if (lastKnownCardsItems.isEmpty()) {
            return true;
        }
        
        // Prüfe alle Slots
        for (int slotIndex = 0; slotIndex < screen.getScreenHandler().slots.size(); slotIndex++) {
            Slot slot = screen.getScreenHandler().slots.get(slotIndex);
            ItemStack currentItem = slot.getStack();
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
    private boolean hasStatuesMenuChanged(HandledScreen<?> screen) {
        if (screen.getScreenHandler() == null) {
            return false;
        }
        
        // Wenn lastKnownStatuesItems leer ist, ist es der erste Scan → immer true
        if (lastKnownStatuesItems.isEmpty()) {
            return true;
        }
        
        // Prüfe alle Slots (wie bei Karten)
        for (int slotIndex = 0; slotIndex < screen.getScreenHandler().slots.size(); slotIndex++) {
            
            Slot slot = screen.getScreenHandler().slots.get(slotIndex);
            ItemStack currentItem = slot.getStack();
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
        String name1 = item1.getName().getString();
        String name2 = item2.getName().getString();
        return name1.equals(name2);
    }
}

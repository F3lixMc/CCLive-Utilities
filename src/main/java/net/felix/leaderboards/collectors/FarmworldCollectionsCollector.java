package net.felix.leaderboards.collectors;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.felix.leaderboards.LeaderboardManager;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

/**
 * Sammelt Collection-Daten aus der Farmworld-Dimension
 * Erkennt Zonen über das Scoreboard und liest Collection-Werte aus der Bossbar
 */
public class FarmworldCollectionsCollector implements DataCollector {
    private boolean isActive = false;
    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 1200; // Alle 60 Sekunden
    
    // DEBUG: Deaktiviere das Senden von Daten an den Server, bis die Erkennung vollständig getestet ist
    private static final boolean ENABLE_SERVER_UPDATES = true;
    
    // Cache-Mechanismus für Zone-Wechsel: Speichere Wert regelmäßig (alle 1 Sekunde)
    private int cacheUpdateCounter = 0;
    private static final int CACHE_UPDATE_INTERVAL = 20; // Alle 20 Ticks = 1 Sekunde
    private int cachedCollectionForCurrentZone = 0; // Gecachter Wert für die aktuelle Zone
    
    // Cache für Zone-Collection-Daten
    private final Map<String, Long> zoneCollections = new HashMap<>();
    private String lastZone = null;
    private int lastTotalCollection = 0; // Absoluter Collection-Wert aus Bossbar
    
    // Timing-Mechanismus für Zone-Wechsel: Verzögerung beim Lesen des Bossbar-Werts
    private String pendingZone = null; // Zone, die noch initialisiert werden muss
    private int pendingZoneTicks = 0; // Anzahl Ticks, die seit dem Zone-Wechsel vergangen sind
    private static final int ZONE_CHANGE_DELAY_TICKS = 50; // Warte 50 Ticks (2.5 Sekunden) nach Zone-Wechsel, damit Bossbar sich aktualisiert hat
    
    // Mapping: Scoreboard-Zonenname -> Collection-Name
    private static final Map<String, String> ZONE_TO_COLLECTION = new HashMap<>();
    static {
        ZONE_TO_COLLECTION.put("[Eichenwald]", "oak_collection");
        ZONE_TO_COLLECTION.put("[Schwefelfelder]", "sulfur_collection");
        ZONE_TO_COLLECTION.put("[Pilzwald]", "mushroom_collection");
        ZONE_TO_COLLECTION.put("[Fichtenwald]", "spruce_collection");
        ZONE_TO_COLLECTION.put("[Dunkeleichenwald]", "dark_oak_collection");
        ZONE_TO_COLLECTION.put("[Mangrovensumpf]", "mangrove_collection");
        ZONE_TO_COLLECTION.put("[Strand]", "jungle_collection");
        ZONE_TO_COLLECTION.put("[Bambuswald]", "bamboo_collection");
        ZONE_TO_COLLECTION.put("[Goldmine]", "raw_gold_collection");
        ZONE_TO_COLLECTION.put("[Kohle Mine]", "coal_collection");
        ZONE_TO_COLLECTION.put("[Karmesinwald]", "crimson_collection");
        ZONE_TO_COLLECTION.put("[Kupfermine]", "raw_copper_collection");
        ZONE_TO_COLLECTION.put("[Quartzmine]", "quartz_collection");
        ZONE_TO_COLLECTION.put("[Wirrwald]", "warped_collection");
        ZONE_TO_COLLECTION.put("[Diamantmine]", "diamond_collection");
        ZONE_TO_COLLECTION.put("[Antike Mine]", "ancient_debris_collection");
        ZONE_TO_COLLECTION.put("[Echomine]", "echo_collection");
        ZONE_TO_COLLECTION.put("[Eisenmine]", "raw_iron_collection");
        ZONE_TO_COLLECTION.put("[Obsidian]", "obsidian_collection");
        ZONE_TO_COLLECTION.put("[Obsidian Mine]", "obsidian_collection");
    }
    
    // Chinesische Zahlen-Mapping (wie in KillsUtility)
    private static final Map<Character, Integer> CHINESE_NUMBERS = new HashMap<>();
    static {
        CHINESE_NUMBERS.put('㚏', 0);
        CHINESE_NUMBERS.put('㚐', 1);
        CHINESE_NUMBERS.put('㚑', 2);
        CHINESE_NUMBERS.put('㚒', 3);
        CHINESE_NUMBERS.put('㚓', 4);
        CHINESE_NUMBERS.put('㚔', 5);
        CHINESE_NUMBERS.put('㚕', 6);
        CHINESE_NUMBERS.put('㚖', 7);
        CHINESE_NUMBERS.put('㚗', 8);
        CHINESE_NUMBERS.put('㚘', 9);
    }
    
    @Override
    public void initialize() {
        if (isActive) {
            System.out.println("⚠️ [FarmworldCollectionsCollector] Bereits initialisiert - überspringe");
            return;
        }
        
        System.out.println("🔍 [FarmworldCollectionsCollector] Starte Initialisierung...");
        // Registriere Tick-Event für Collection-Tracking
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        isActive = true;
        System.out.println("✅ [FarmworldCollectionsCollector] FarmworldCollectionsCollector initialisiert und aktiv");
    }
    
    private void onClientTick(MinecraftClient client) {
        if (!isActive || client.player == null || client.world == null) {
            return;
        }
        
        // Prüfe ob wir in der Farmworld-Dimension sind
        if (!isInFarmworldDimension(client)) {
            return;
        }
        
        // Cache-Mechanismus: Aktualisiere gecachten Wert regelmäßig (alle 1 Sekunde)
        cacheUpdateCounter++;
        if (cacheUpdateCounter >= CACHE_UPDATE_INTERVAL) {
            cacheUpdateCounter = 0;
            updateCachedCollection();
        }
        
        // Zone-Wechsel-Erkennung (alle 20 Ticks = 1x pro Sekunde)
        if (tickCounter % 20 == 0) {
            checkZoneChange(client);
        }
        
        // Collection-Updates nur alle 60 Sekunden
        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            updateCollectionInCurrentZone(client);
            updateAllCollections(); // Berechne und sende All Collections
        }
    }
    
    /**
     * Prüft ob wir in der Farmworld-Dimension sind
     * Doppelcheck: Dimension muss minecraft:overworld sein UND eine Zone muss im Scoreboard stehen
     */
    private boolean isInFarmworldDimension(MinecraftClient client) {
        try {
            if (client.world == null) {
                return false;
            }
            
            // Erste Bedingung: Dimension muss minecraft:overworld sein
            String dimensionId = client.world.getRegistryKey().getValue().toString();
            if (!dimensionId.equals("minecraft:overworld")) {
                return false;
            }
            
            // Zweite Bedingung: Eine Zone muss im Scoreboard stehen
            String zone = getCurrentZone(client);
            return zone != null;
        } catch (Exception e) {
            System.err.println("❌ [FarmworldCollections] Fehler bei Dimension-Check: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Aktualisiert den gecachten Collection-Wert für die aktuelle Zone
     */
    private void updateCachedCollection() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return;
            
            String currentZone = getCurrentZone(client);
            if (currentZone == null) return;
            
            // Nur cachen wenn wir noch in der gleichen Zone sind UND nicht auf einen Zone-Wechsel warten
            if (currentZone.equals(lastZone) && pendingZone == null) {
                int currentCollection = getCollectionFromBossbar(client);
                cachedCollectionForCurrentZone = currentCollection;
            }
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    /**
     * Prüft ob sich die Zone geändert hat
     */
    private void checkZoneChange(MinecraftClient client) {
        try {
            String currentZone = getCurrentZone(client);
            if (currentZone == null) {
                // Reset pending zone wenn keine Zone gefunden
                pendingZone = null;
                pendingZoneTicks = 0;
                return;
            }
            
            // Prüfe ob sich die Zone geändert hat
            if (!currentZone.equals(lastZone)) {
                System.out.println("🔄 [FarmworldCollections] ZONE-WECHSEL: " + lastZone + " → " + currentZone);
                
                // Zone-Wechsel erkannt - speichere die vorherige Zone IMMER (forceSend=true)
                // So wird der Wert auch gesendet, wenn er noch nicht übermittelt wurde
                if (lastZone != null && cachedCollectionForCurrentZone > 0) {
                    updateZoneScore(lastZone, cachedCollectionForCurrentZone, true);
                } else if (lastZone != null && lastTotalCollection > 0) {
                    // Fallback: Falls kein gecachter Wert vorhanden, verwende lastTotalCollection
                    updateZoneScore(lastZone, lastTotalCollection, true);
                }
                
                // Nach dem Speichern der vorherigen Zone All Collections sofort aktualisieren
                // (zusätzlich zum 60-Sekunden-Intervall), damit kurze Zonen nicht verloren gehen
                updateAllCollections();

                // Neue Zone als "pending" markieren - warte auf Bossbar-Update
                pendingZone = currentZone;
                pendingZoneTicks = 0;
                lastZone = currentZone; // Aktualisiere lastZone sofort, damit wir nicht in eine Schleife geraten
            } else if (pendingZone != null && currentZone.equals(pendingZone)) {
                // Wir warten noch auf die Initialisierung der neuen Zone
                pendingZoneTicks++;
                
                if (pendingZoneTicks >= ZONE_CHANGE_DELAY_TICKS) {
                    // Verzögerung abgelaufen - jetzt Bossbar-Wert abrufen
                    int newZoneCollection = getCollectionFromBossbar(client);
                    lastTotalCollection = newZoneCollection;
                    cachedCollectionForCurrentZone = newZoneCollection;
                    
                    // Wenn der aktuelle Wert niedriger ist als der gespeicherte Wert,
                    // bedeutet das, dass wir die Zone neu betreten haben
                    long cachedCollection = zoneCollections.getOrDefault(pendingZone, 0L);
                    if (newZoneCollection < cachedCollection) {
                        zoneCollections.put(pendingZone, (long) newZoneCollection);
                        
                        // DEBUG: Nur senden wenn aktiviert
                        if (ENABLE_SERVER_UPDATES) {
                            LeaderboardManager.getInstance().updateScore(getCollectionNameForZone(pendingZone), newZoneCollection);
                        }
                    }
                    
                    // Reset pending zone
                    pendingZone = null;
                    pendingZoneTicks = 0;
                }
            } else {
                // Gleiche Zone - aktualisiere lastTotalCollection kontinuierlich (nur wenn höher)
                // Aber nur wenn wir nicht auf eine Zone-Wechsel warten
                if (pendingZone == null) {
                    int currentCollection = getCollectionFromBossbar(client);
                    if (currentCollection > lastTotalCollection) {
                        lastTotalCollection = currentCollection;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ [FarmworldCollections] Fehler bei Zone-Check: " + e.getMessage());
        }
    }
    
    /**
     * Aktualisiert Collection-Daten in der aktuellen Zone (alle 60 Sekunden)
     */
    private void updateCollectionInCurrentZone(MinecraftClient client) {
        try {
            String currentZone = getCurrentZone(client);
            if (currentZone == null) {
                return;
            }
            
            // Nur updaten wenn wir noch in der gleichen Zone sind
            if (currentZone.equals(lastZone)) {
                int totalCollection = getCollectionFromBossbar(client);
                
                // Aktualisiere lastTotalCollection immer
                lastTotalCollection = totalCollection;
                
                // Wenn Collection sich erhöht hat ODER wenn der aktuelle Wert niedriger ist als der Cache-Wert
                long cachedCollection = zoneCollections.getOrDefault(currentZone, 0L);
                if (totalCollection != cachedCollection) {
                    updateZoneScore(currentZone, totalCollection);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ [FarmworldCollections] Fehler beim 60-Sekunden-Update: " + e.getMessage());
        }
    }
    
    /**
     * Aktualisiert den Score für eine Zone
     * @param zone Die Zone
     * @param collection Der Collection-Wert
     * @param forceSend Wenn true, wird IMMER gesendet (z.B. bei Zone-Wechsel), auch wenn sich der Wert nicht geändert hat
     */
    private void updateZoneScore(String zone, int collection, boolean forceSend) {
        if (zone == null || collection < 0) return;
        
        String collectionName = getCollectionNameForZone(zone);
        if (collectionName == null) return;
        
        // Hole bisherige Collection für diese Zone
        long currentTotal = zoneCollections.getOrDefault(zone, 0L);
        
        // Wenn der neue Wert niedriger ist als der gespeicherte Wert, bedeutet das,
        // dass wir die Zone neu betreten haben - überschreibe den Cache
        long newTotal;
        if (collection < currentTotal) {
            // Zone wurde neu betreten - verwende den aktuellen Wert
            newTotal = (long) collection;
        } else {
            // Normaler Fall - verwende den höheren Wert
            newTotal = Math.max(currentTotal, (long) collection);
        }
        
        // Updaten wenn sich der Wert geändert hat ODER wenn forceSend=true (z.B. bei Zone-Wechsel)
        if (newTotal != currentTotal || forceSend) {
            zoneCollections.put(zone, newTotal);
            
            // DEBUG: Nur senden wenn aktiviert
            if (ENABLE_SERVER_UPDATES) {
                LeaderboardManager.getInstance().updateScore(collectionName, newTotal);
            }
        }
    }
    
    /**
     * Aktualisiert den Score für eine Zone (ohne forceSend, verwendet Standard-Logik)
     */
    private void updateZoneScore(String zone, int collection) {
        updateZoneScore(zone, collection, false);
    }
    
    /**
     * Berechnet die Summe aller Collection-Werte aus allen Zonen
     */
    public long getTotalCollections() {
        return zoneCollections.values().stream().mapToLong(Long::longValue).sum();
    }
    
    /**
     * Aktualisiert All Collections (Summe aller Zone-Collections)
     */
    private void updateAllCollections() {
        try {
            long calculatedAllCollections = getTotalCollections();
            
            // Prüfe ob sich der Wert geändert hat und größer als 0 ist
            if (calculatedAllCollections > 0) {
                // DEBUG: Nur senden wenn aktiviert
                if (ENABLE_SERVER_UPDATES) {
                    LeaderboardManager.getInstance().updateScore("all_collections", calculatedAllCollections);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ [FarmworldCollections] Fehler beim Aktualisieren der All Collections: " + e.getMessage());
        }
    }
    
    /**
     * Ermittelt die aktuelle Zone aus dem Sidebar-Scoreboard (rechts)
     */
    private String getCurrentZone(MinecraftClient client) {
        try {
            if (client == null || client.world == null) {
                return null;
            }
            
            Scoreboard scoreboard = client.world.getScoreboard();
            if (scoreboard == null) {
                return null;
            }
            
            // Hole das Sidebar-Objektiv (wird rechts angezeigt)
            ScoreboardObjective sidebarObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (sidebarObjective == null) {
                return null;
            }
            
            // Helper method to remove Minecraft formatting codes (§ codes)
            java.util.function.Function<String, String> removeFormatting = (text) -> {
                if (text == null) return "";
                return text.replaceAll("§[0-9a-fk-or]", "").trim();
            };
            
            // Versuche alle Scoreboard-Einträge zu lesen
            // Verwende verschiedene Ansätze, da die API je nach MC-Version unterschiedlich ist
            
            // Ansatz 1: Versuche über getAllPlayerScores (Collection zurück) - OFFIZIELLE API
            java.util.Collection<?> scores = null;
            try {
                java.lang.reflect.Method getAllPlayerScoresMethod = scoreboard.getClass().getMethod("getAllPlayerScores", ScoreboardObjective.class);
                scores = (java.util.Collection<?>) getAllPlayerScoresMethod.invoke(scoreboard, sidebarObjective);
                
                // Durchsuche alle Scores
                for (Object scoreObj : scores) {
                    if (scoreObj != null) {
                        try {
                            // Versuche getPlayerName() oder ähnliche Methoden
                            java.lang.reflect.Method getPlayerNameMethod = scoreObj.getClass().getMethod("getPlayerName");
                            String playerName = (String) getPlayerNameMethod.invoke(scoreObj);
                            if (playerName != null) {
                                String cleanName = removeFormatting.apply(playerName);
                                
                                // Prüfe ob dieser Name eine Zone enthält
                                for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                    if (cleanName.contains(zoneName)) {
                                        return zoneName;
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            // Ignoriere Fehler
                        }
                    }
                }
            } catch (Exception e) {
                // Silent error handling
                
                // Ansatz 2: Versuche über getPlayerScores (Collection zurück)
                try {
                    java.lang.reflect.Method getPlayerScoresMethod = scoreboard.getClass().getMethod("getPlayerScores", ScoreboardObjective.class);
                    scores = (java.util.Collection<?>) getPlayerScoresMethod.invoke(scoreboard, sidebarObjective);
                    
                    // Durchsuche alle Scores (gleiche Logik wie oben)
                    for (Object scoreObj : scores) {
                        if (scoreObj != null) {
                            try {
                                java.lang.reflect.Method getPlayerNameMethod = scoreObj.getClass().getMethod("getPlayerName");
                                String playerName = (String) getPlayerNameMethod.invoke(scoreObj);
                                if (playerName != null) {
                                    String cleanName = removeFormatting.apply(playerName);
                                    
                                    for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                        if (cleanName.contains(zoneName)) {
                                            return zoneName;
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                // Ignoriere Fehler
                            }
                        }
                    }
                } catch (Exception e2) {
                    // Silent error handling
                    
                    // Ansatz 3: Versuche playerObjectives direkt zu durchsuchen (Map<String, Map<ScoreboardObjective, ScoreboardPlayerScore>>)
                    // Da das Feld obfuskiert ist, identifizieren wir es durch seine Struktur:
                    // Es ist eine Map<String, Map<ScoreboardObjective, ScoreboardPlayerScore>>
                    java.util.Map<String, java.util.Map<ScoreboardObjective, ?>> playerObjectives = null;
                    try {
                        
                        java.lang.reflect.Field[] fields = scoreboard.getClass().getDeclaredFields();
                        
                        for (java.lang.reflect.Field field : fields) {
                            if (java.util.Map.class.isAssignableFrom(field.getType())) {
                                field.setAccessible(true);
                                Object fieldValue = field.get(scoreboard);
                                if (fieldValue instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) fieldValue;
                                    
                                    // Prüfe ob dies playerObjectives ist:
                                    // 1. Name enthält "player" oder "objective" ODER
                                    // 2. Map hat String-Keys und Map-Values (Map<String, Map<?, ?>>)
                                    boolean isPlayerObjectives = false;
                                    
                                    if (field.getName().contains("player") && field.getName().contains("objective")) {
                                        isPlayerObjectives = true;
                                    } else if (map.size() > 10) {
                                        // Prüfe ob die Values Maps sind (Map<String, Map<?, ?>>)
                                        int mapValueCount = 0;
                                        for (Object value : map.values()) {
                                            if (value instanceof java.util.Map) {
                                                mapValueCount++;
                                            }
                                        }
                                        // Wenn mindestens 50% der Values Maps sind, ist es wahrscheinlich playerObjectives
                                        if (mapValueCount > map.size() * 0.5) {
                                            isPlayerObjectives = true;
                                            System.out.println("🔍 [FarmworldCollections] Zone-Suche: Map " + field.getName() + " identifiziert als 'playerObjectives' (hat " + mapValueCount + " Map-Values von " + map.size() + " Einträgen)");
                                        }
                                    }
                                    
                                    if (isPlayerObjectives) {
                                        @SuppressWarnings("unchecked")
                                        java.util.Map<String, java.util.Map<ScoreboardObjective, ?>> playerObjectivesMap = (java.util.Map<String, java.util.Map<ScoreboardObjective, ?>>) map;
                                        playerObjectives = playerObjectivesMap;
                                        System.out.println("✅ [FarmworldCollections] Zone-Suche: playerObjectives gefunden: " + field.getName() + " (Größe: " + map.size() + ")");
                                        break;
                                    }
                                }
                            }
                        }
                        
                        if (playerObjectives == null) {
                            throw new Exception("playerObjectives Map nicht gefunden");
                        }
                        
                        System.out.println("✅ [FarmworldCollections] Zone-Suche: playerObjectives gefunden, " + playerObjectives.size() + " Einträge");
                            
                            // Priorisiere sb_display_* Keys
                            java.util.List<String> sbDisplayKeys = new java.util.ArrayList<>();
                            java.util.List<String> otherKeys = new java.util.ArrayList<>();
                            
                            for (String playerName : playerObjectives.keySet()) {
                                if (playerName.startsWith("sb_display_")) {
                                    sbDisplayKeys.add(playerName);
                                } else {
                                    otherKeys.add(playerName);
                                }
                            }
                            
                            // Durchsuche zuerst sb_display_* Keys
                            for (String playerName : sbDisplayKeys) {
                                String cleanName = removeFormatting.apply(playerName);
                                System.out.println("  📋 playerObjectives sb_display Key: '" + playerName + "' → Clean: '" + cleanName + "'");
                                
                                // Prüfe ob dieser Name eine Zone enthält
                                for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                    if (cleanName.contains(zoneName)) {
                                        
                                        return zoneName;
                                    }
                                }
                                
                                // Prüfe auch die Values (Map<ScoreboardObjective, ScoreboardPlayerScore>)
                                java.util.Map<ScoreboardObjective, ?> playerScores = playerObjectives.get(playerName);
                                if (playerScores != null && playerScores.containsKey(sidebarObjective)) {
                                    Object scoreObj = playerScores.get(sidebarObjective);
                                    if (scoreObj != null) {
                                        try {
                                            java.lang.reflect.Method getPlayerNameMethod = scoreObj.getClass().getMethod("getPlayerName");
                                            String scorePlayerName = (String) getPlayerNameMethod.invoke(scoreObj);
                                            if (scorePlayerName != null) {
                                                String cleanScoreName = removeFormatting.apply(scorePlayerName);
                                                System.out.println("    📋 Score-Player-Name: '" + scorePlayerName + "' → Clean: '" + cleanScoreName + "'");
                                                
                                                for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                                    if (cleanScoreName.contains(zoneName)) {
                                                        
                                                        return zoneName;
                                                    }
                                                }
                                            }
                                        } catch (Exception ex) {
                                            // Ignoriere Fehler
                                        }
                                    }
                                }
                            }
                            
                            // Dann durchsuche andere Keys
                            for (String playerName : otherKeys) {
                                String cleanName = removeFormatting.apply(playerName);
                                
                                // Prüfe ob dieser Name eine Zone enthält
                                for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                    if (cleanName.contains(zoneName)) {
                                        
                                        return zoneName;
                                    }
                                }
                            }
                    } catch (Exception e3) {
                        System.out.println("⚠️ [FarmworldCollections] Zone-Suche: playerObjectives Durchsuchung fehlgeschlagen: " + e3.getMessage());
                    }
                    
                    // Ansatz 4: Versuche über Felder direkt auf die Score-Map zuzugreifen
                    // WICHTIG: Wir suchen nur in der "scores" Map, die die Scores für alle Objectives enthält
                    // Aber wir müssen prüfen, ob die Scores zu unserem Sidebar-Objective gehören
                    try {
                        
                        java.lang.reflect.Field[] fields = scoreboard.getClass().getDeclaredFields();
                        
                        
                        // Finde die "scores" Map - das ist die Map, die alle Scores für alle Objectives enthält
                        // Da die Feldnamen obfuskiert sind, identifizieren wir sie durch ihre Struktur:
                        // Die scores Map ist normalerweise die größte Map (>50 Einträge) und enthält String-Keys
                        java.util.Map<?, ?> scoresMap = null;
                        java.util.Map<?, ?> largestMap = null;
                        int largestSize = 0;
                        
                        for (java.lang.reflect.Field field : fields) {
                            if (java.util.Map.class.isAssignableFrom(field.getType())) {
                                field.setAccessible(true);
                                Object fieldValue = field.get(scoreboard);
                                if (fieldValue instanceof java.util.Map) {
                                    @SuppressWarnings("unchecked")
                                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) fieldValue;
                                    
                                    // Prüfe ob dies die "scores" Map ist:
                                    // 1. Name enthält "score" ODER
                                    // 2. Map ist groß (>50) und hat String-Keys
                                    boolean isScoresMap = false;
                                    
                                    if (field.getName().equals("scores") || field.getName().contains("score")) {
                                        isScoresMap = true;
                                    } else if (map.size() > 50) { // scores Map ist normalerweise groß
                                        // Prüfe ob die Keys Strings sind (scores Map hat String-Keys)
                                        int stringKeyCount = 0;
                                        for (Object key : map.keySet()) {
                                            if (key instanceof String) {
                                                stringKeyCount++;
                                            }
                                        }
                                        // Wenn mindestens 80% der Keys Strings sind, ist es wahrscheinlich die scores Map
                                        if (stringKeyCount > map.size() * 0.8) {
                                            isScoresMap = true;
                                            System.out.println("🔍 [FarmworldCollections] Zone-Suche: Map " + field.getName() + " identifiziert als 'scores' Map (hat " + stringKeyCount + " String-Keys von " + map.size() + " Einträgen)");
                                        }
                                    }
                                    
                                    // Tracke die größte Map als Fallback
                                    if (map.size() > largestSize) {
                                        largestSize = map.size();
                                        largestMap = map;
                                    }
                                    
                                    if (isScoresMap) {
                                        scoresMap = map;
                                        System.out.println("✅ [FarmworldCollections] Zone-Suche: 'scores' Map gefunden: " + field.getName() + " (Größe: " + map.size() + ")");
                                        break;
                                    }
                                }
                            }
                        }
                        
                        // Fallback: Wenn keine scores Map gefunden wurde, verwende die größte Map
                        if (scoresMap == null && largestMap != null && largestSize > 50) {
                            scoresMap = largestMap;
                            System.out.println("⚠️ [FarmworldCollections] Zone-Suche: 'scores' Map nicht eindeutig identifiziert, verwende größte Map (Größe: " + largestSize + ")");
                        }
                        
                        // Wenn wir die scores Map gefunden haben, durchsuche nur diese
                        if (scoresMap != null) {
                            
                            
                            // WICHTIG: Prüfe direkt die Keys der scores Map (die Zone kann als Key stehen, z.B. "|||->[Kupfermine]")
                            // Priorisiere sb_display_* Keys, da diese wahrscheinlich Sidebar-Einträge sind
                            java.util.List<java.util.Map.Entry<?, ?>> sbDisplayEntries = new java.util.ArrayList<>();
                            java.util.List<java.util.Map.Entry<?, ?>> otherEntries = new java.util.ArrayList<>();
                            
                            for (java.util.Map.Entry<?, ?> entry : scoresMap.entrySet()) {
                                Object key = entry.getKey();
                                if (key instanceof String) {
                                    String keyString = (String) key;
                                    if (keyString.startsWith("sb_display_")) {
                                        sbDisplayEntries.add(entry);
                                    } else {
                                        otherEntries.add(entry);
                                    }
                                } else {
                                    otherEntries.add(entry);
                                }
                            }
                            
                            // OPTIMIERUNG: Durchsuche zuerst nur die sb_display_* Einträge und prüfe direkt field_1418
                            // Die Zone steht in field_1418 des Team-Objekts (net.minecraft.class_268)
                            for (java.util.Map.Entry<?, ?> entry : sbDisplayEntries) {
                                Object key = entry.getKey();
                                Object value = entry.getValue();
                                
                                // Die Zone steht in field_1418 des Team-Objekts
                                if (value != null && value.getClass().getName().contains("class_268")) {
                                    try {
                                        // Direkt field_1418 prüfen (das ist das Feld, das die Zone enthält)
                                        java.lang.reflect.Field field1418 = value.getClass().getDeclaredField("field_1418");
                                        field1418.setAccessible(true);
                                        Object fieldValue = field1418.get(value);
                                        
                                        if (fieldValue != null) {
                                            String extractedText = null;
                                            if (fieldValue instanceof Text) {
                                                extractedText = ((Text) fieldValue).getString();
                                            } else if (fieldValue instanceof String) {
                                                extractedText = (String) fieldValue;
                                            } else if (fieldValue instanceof net.minecraft.text.MutableText) {
                                                extractedText = ((net.minecraft.text.MutableText) fieldValue).getString();
                                            }
                                            
                                            if (extractedText != null && !extractedText.isEmpty()) {
                                                // Prüfe ob dieser Text eine Zone enthält
                                                String cleanText = removeFormatting.apply(extractedText);
                                                for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                                    if (cleanText.contains(zoneName)) {
                                                        System.out.println("✅ [FarmworldCollections] Zone-Suche: Zone gefunden! '" + zoneName + "' in field_1418 (Key: " + key + "): '" + extractedText + "'");
                                                        return zoneName;
                                                    }
                                                }
                                            }
                                        }
                                    } catch (java.lang.NoSuchFieldException ex) {
                                        // field_1418 nicht gefunden, versuche alternativen Ansatz
                                        // Fallback: Durchsuche alle Felder (nur wenn field_1418 nicht existiert)
                                        try {
                                            java.lang.reflect.Field[] teamFields = value.getClass().getDeclaredFields();
                                            for (java.lang.reflect.Field teamField : teamFields) {
                                                try {
                                                    teamField.setAccessible(true);
                                                    Object teamFieldValue = teamField.get(value);
                                                    
                                                    if (teamFieldValue != null) {
                                                        String extractedText = null;
                                                        if (teamFieldValue instanceof Text) {
                                                            extractedText = ((Text) teamFieldValue).getString();
                                                        } else if (teamFieldValue instanceof String) {
                                                            extractedText = (String) teamFieldValue;
                                                        } else if (teamFieldValue instanceof net.minecraft.text.MutableText) {
                                                            extractedText = ((net.minecraft.text.MutableText) teamFieldValue).getString();
                                                        }
                                                        
                                                        if (extractedText != null && !extractedText.isEmpty()) {
                                                            String cleanText = removeFormatting.apply(extractedText);
                                                            for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                                                if (cleanText.contains(zoneName)) {
                                                                    System.out.println("✅ [FarmworldCollections] Zone-Suche: Zone gefunden! '" + zoneName + "' in Team-Feld " + teamField.getName() + " (Key: " + key + "): '" + extractedText + "'");
                                                                    return zoneName;
                                                                }
                                                            }
                                                        }
                                                    }
                                                } catch (Exception ex2) {
                                                    // Ignoriere Fehler
                                                }
                                            }
                                        } catch (Exception ex3) {
                                            // Ignoriere Fehler
                                        }
                                    } catch (Exception ex) {
                                        // Ignoriere andere Fehler
                                    }
                                }
                            }
                            
                            // Fallback: Falls keine Zone in sb_display_* gefunden wurde, durchsuche andere Keys
                            for (java.util.Map.Entry<?, ?> entry : otherEntries) {
                                Object key = entry.getKey();
                                if (key instanceof String) {
                                    String keyString = (String) key;
                                    String cleanKey = removeFormatting.apply(keyString);
                                    
                                    // Prüfe ob dieser Key eine Zone enthält
                                    for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                        if (cleanKey.contains(zoneName)) {
                                            
                                            return zoneName;
                                        }
                                    }
                                }
                            }
                            
                            // Fallback: Falls keine Zone in sb_display_* gefunden wurde, durchsuche andere Einträge
                            for (java.util.Map.Entry<?, ?> entry : scoresMap.entrySet()) {
                                Object key = entry.getKey();
                                Object value = entry.getValue();
                                
                                // Überspringe sb_display_* Keys, die bereits durchsucht wurden
                                if (key instanceof String && ((String) key).startsWith("sb_display_")) {
                                    continue;
                                }
                                
                                // Prüfe andere Team-Objekte (nur als Fallback)
                                if (value != null && value.getClass().getName().contains("class_268")) {
                                    String text = null;
                                    try {
                                        // Versuche verschiedene Methoden/Felder
                                        java.lang.reflect.Method[] methods = value.getClass().getDeclaredMethods();
                                        for (java.lang.reflect.Method method : methods) {
                                            if (method.getName().contains("DisplayName") || method.getName().contains("Name") || method.getName().contains("Text")) {
                                                try {
                                                    method.setAccessible(true);
                                                    Object result = method.invoke(value);
                                                    if (result instanceof Text) {
                                                        text = ((Text) result).getString();
                                                        break;
                                                    } else if (result instanceof String) {
                                                        text = (String) result;
                                                        break;
                                                    }
                                                } catch (Exception ex) {
                                                    // Ignoriere Fehler
                                                }
                                            }
                                        }
                                        
                                        // Falls keine Methode funktioniert hat, versuche Felder
                                        if (text == null) {
                                            java.lang.reflect.Field[] teamFields = value.getClass().getDeclaredFields();
                                            for (java.lang.reflect.Field teamField : teamFields) {
                                                try {
                                                    teamField.setAccessible(true);
                                                    Object teamFieldValue = teamField.get(value);
                                                    if (teamFieldValue instanceof Text) {
                                                        text = ((Text) teamFieldValue).getString();
                                                        break;
                                                    } else if (teamFieldValue instanceof String) {
                                                        text = (String) teamFieldValue;
                                                        break;
                                                    }
                                                } catch (Exception ex) {
                                                    // Ignoriere Fehler
                                                }
                                            }
                                        }
                                        
                                        // Prüfe ob dieser Text eine Zone enthält
                                        if (text != null && !text.isEmpty()) {
                                            String cleanText = removeFormatting.apply(text);
                                            for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                                if (cleanText.contains(zoneName)) {
                                                    System.out.println("✅ [FarmworldCollections] Zone-Suche: Zone gefunden! '" + zoneName + "' in Team-Objekt (Key: " + key + "): '" + text + "'");
                                                    return zoneName;
                                                }
                                            }
                                        }
                                    } catch (Exception ex) {
                                        // Ignoriere Fehler
                                    }
                                }
                                        
                                        // Prüfe ob der Value direkt ein ScoreboardPlayerScore ist (field_1431 hat diese Struktur)
                                        if (value != null && value.getClass().getName().contains("ScoreboardPlayerScore")) {
                                            System.out.println("  ✅ ScoreboardPlayerScore gefunden! Versuche Text zu extrahieren...");
                                            
                                            // Versuche Text aus dem Score-Objekt zu extrahieren
                                            String text = null;
                                            try {
                                                // Methode 1: getDisplayName()
                                                try {
                                                    java.lang.reflect.Method getDisplayNameMethod = value.getClass().getMethod("getDisplayName");
                                                    Text displayName = (Text) getDisplayNameMethod.invoke(value);
                                                    if (displayName != null) {
                                                        text = displayName.getString();
                                                        System.out.println("  ✅ getDisplayName() erfolgreich: '" + text + "'");
                                                    }
                                                } catch (Exception ex1) {
                                                    System.out.println("  ⚠️ getDisplayName() fehlgeschlagen: " + ex1.getMessage());
                                                    
                                                    // Methode 2: playerName Feld
                                                    try {
                                                        java.lang.reflect.Field playerNameField = value.getClass().getDeclaredField("playerName");
                                                        playerNameField.setAccessible(true);
                                                        text = (String) playerNameField.get(value);
                                                        System.out.println("  ✅ playerName Feld erfolgreich: '" + text + "'");
                                                    } catch (Exception ex2) {
                                                        System.out.println("  ⚠️ playerName Feld fehlgeschlagen: " + ex2.getMessage());
                                                        
                                                        // Methode 3: Alle String-Felder durchsuchen
                                                        java.lang.reflect.Field[] scoreFields = value.getClass().getDeclaredFields();
                                                        for (java.lang.reflect.Field scoreField : scoreFields) {
                                                            if (scoreField.getType() == String.class) {
                                                                scoreField.setAccessible(true);
                                                                Object scoreFieldValue = scoreField.get(value);
                                                                if (scoreFieldValue instanceof String && !((String) scoreFieldValue).isEmpty()) {
                                                                    text = (String) scoreFieldValue;
                                                                    System.out.println("  ✅ String-Feld gefunden (" + scoreField.getName() + "): '" + text + "'");
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (Exception ex3) {
                                                System.out.println("  ❌ Fehler beim Extrahieren von Text: " + ex3.getMessage());
                                            }
                                            
                                            if (text != null && !text.isEmpty()) {
                                                String cleanText = removeFormatting.apply(text);
                                                System.out.println("  📋 Scoreboard-Eintrag: Raw='" + text + "' → Clean='" + cleanText + "'");
                                                
                                                // Prüfe ob dieser Text eine Zone enthält
                                                for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                                    if (cleanText.contains(zoneName)) {
                                                        System.out.println("✅ [FarmworldCollections] Zone-Suche: Zone gefunden! '" + zoneName + "' in Scoreboard-Eintrag (Key: " + key + ")");
                                                        return zoneName;
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Prüfe ob der Value ein Scores-Objekt ist (field_1426 hat diese Struktur)
                                        if (value != null && value.getClass().getName().contains("Scores") && !value.getClass().getName().contains("ScoreboardPlayerScore")) {
                                            System.out.println("  ✅ Scores-Objekt gefunden! Versuche Text zu extrahieren...");
                                            
                                            try {
                                                // Durchsuche alle Felder des Scores-Objekts
                                                java.lang.reflect.Field[] scoresFields = value.getClass().getDeclaredFields();
                                                for (java.lang.reflect.Field scoresField : scoresFields) {
                                                    if (scoresField.getType() == String.class || Text.class.isAssignableFrom(scoresField.getType()) || java.util.Map.class.isAssignableFrom(scoresField.getType())) {
                                                        try {
                                                            scoresField.setAccessible(true);
                                                            Object scoresFieldValue = scoresField.get(value);
                                                            
                                                            if (scoresFieldValue instanceof String) {
                                                                String text = (String) scoresFieldValue;
                                                                String cleanText = removeFormatting.apply(text);
                                                                System.out.println("  📋 Scores-Feld " + scoresField.getName() + ": '" + text + "' → Clean: '" + cleanText + "'");
                                                                
                                                                for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                                                    if (cleanText.contains(zoneName)) {
                                                                        System.out.println("✅ [FarmworldCollections] Zone-Suche: Zone gefunden! '" + zoneName + "' in Scores-Feld (Key: " + key + "): '" + text + "'");
                                                                        return zoneName;
                                                                    }
                                                                }
                                                            } else if (scoresFieldValue instanceof Text) {
                                                                String text = ((Text) scoresFieldValue).getString();
                                                                String cleanText = removeFormatting.apply(text);
                                                                System.out.println("  📋 Scores-Feld " + scoresField.getName() + " (Text): '" + text + "' → Clean: '" + cleanText + "'");
                                                                
                                                                for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                                                    if (cleanText.contains(zoneName)) {
                                                                        System.out.println("✅ [FarmworldCollections] Zone-Suche: Zone gefunden! '" + zoneName + "' in Scores-Feld (Key: " + key + "): '" + text + "'");
                                                                        return zoneName;
                                                                    }
                                                                }
                                                            } else if (scoresFieldValue instanceof java.util.Map) {
                                                                // Wenn es eine Map ist, durchsuche auch diese (enthält möglicherweise ScoreboardPlayerScore-Objekte)
                                                                @SuppressWarnings("unchecked")
                                                                java.util.Map<?, ?> innerMap = (java.util.Map<?, ?>) scoresFieldValue;
                                                                
                                                                // Prüfe Keys
                                                                for (Object innerKey : innerMap.keySet()) {
                                                                    if (innerKey instanceof String) {
                                                                        String innerKeyString = (String) innerKey;
                                                                        String cleanInnerKey = removeFormatting.apply(innerKeyString);
                                                                        
                                                                        for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                                                            if (cleanInnerKey.contains(zoneName)) {
                                                                                System.out.println("✅ [FarmworldCollections] Zone-Suche: Zone gefunden! '" + zoneName + "' in Scores-InnerMap-Key (Key: " + key + "): '" + innerKeyString + "'");
                                                                                return zoneName;
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                // Prüfe Values (könnten ScoreboardPlayerScore-Objekte sein)
                                                                for (Object innerValue : innerMap.values()) {
                                                                    if (innerValue != null && innerValue.getClass().getName().contains("ScoreboardPlayerScore")) {
                                                                        try {
                                                                            // Versuche Text aus dem ScoreboardPlayerScore zu extrahieren
                                                                            java.lang.reflect.Method getDisplayNameMethod = innerValue.getClass().getMethod("getDisplayName");
                                                                            Text displayName = (Text) getDisplayNameMethod.invoke(innerValue);
                                                                            if (displayName != null) {
                                                                                String text = displayName.getString();
                                                                                String cleanText = removeFormatting.apply(text);
                                                                                System.out.println("  📋 Scores-InnerMap-Score: '" + text + "' → Clean: '" + cleanText + "'");
                                                                                
                                                                                for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                                                                    if (cleanText.contains(zoneName)) {
                                                                                        System.out.println("✅ [FarmworldCollections] Zone-Suche: Zone gefunden! '" + zoneName + "' in Scores-InnerMap-Score (Key: " + key + "): '" + text + "'");
                                                                                        return zoneName;
                                                                                    }
                                                                                }
                                                                            }
                                                                        } catch (Exception ex) {
                                                                            // Ignoriere Fehler
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        } catch (Exception ex) {
                                                            // Ignoriere Fehler
                                                        }
                                                    }
                                                }
                                            } catch (Exception ex) {
                                                System.out.println("  ⚠️ Fehler beim Extrahieren von Text aus Scores-Objekt: " + ex.getMessage());
                                            }
                                        }
                                        
                                        // Prüfe ob der Value eine Collection ist
                                        if (value instanceof java.util.Collection) {
                                            @SuppressWarnings("unchecked")
                                            java.util.Collection<?> tempScores = (java.util.Collection<?>) value;
                                            if (!tempScores.isEmpty()) {
                                                Object first = tempScores.iterator().next();
                                                if (first != null && first.getClass().getName().contains("ScoreboardPlayerScore")) {
                                                    scores = tempScores;
                                                    System.out.println("✅ [FarmworldCollections] Zone-Suche: Score-Collection in scores-Map gefunden (Key: " + key + ")");
                                                    break;
                                                }
                                            }
                                        }
                                        
                                        // Prüfe ob der Key das Objective ist und der Value die Scores
                                        if (key instanceof ScoreboardObjective && value instanceof java.util.Collection) {
                                            @SuppressWarnings("unchecked")
                                            java.util.Collection<?> tempScores = (java.util.Collection<?>) value;
                                            if (!tempScores.isEmpty()) {
                                                scores = tempScores;
                                                
                                                break;
                                            }
                                        }
                            }
                            // Nach der Durchsuchung der scores Map sind wir fertig
                            
                        } else {
                            
                            // Fallback: Durchsuche alle Maps (nur wenn scores Map nicht gefunden wurde)
                            for (java.lang.reflect.Field field : fields) {
                                if (java.util.Map.class.isAssignableFrom(field.getType())) {
                                    field.setAccessible(true);
                                    Object fieldValue = field.get(scoreboard);
                                    if (fieldValue instanceof java.util.Map) {
                                        @SuppressWarnings("unchecked")
                                        java.util.Map<?, ?> map = (java.util.Map<?, ?>) fieldValue;
                                        System.out.println("🔍 [FarmworldCollections] Zone-Suche: Map-Feld gefunden: " + field.getName() + " (Größe: " + map.size() + ")");
                                        
                                        // Prüfe Keys
                                        for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                                            Object key = entry.getKey();
                                            if (key instanceof String) {
                                                String keyString = (String) key;
                                                String cleanKey = removeFormatting.apply(keyString);
                                                
                                                for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                                    if (cleanKey.contains(zoneName)) {
                                                        System.out.println("✅ [FarmworldCollections] Zone-Suche: Zone gefunden! '" + zoneName + "' in Map-Key (Feld: " + field.getName() + "): '" + keyString + "'");
                                                        return zoneName;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e3) {
                        System.out.println("⚠️ [FarmworldCollections] Zone-Suche: Feld-Zugriff fehlgeschlagen: " + e3.getMessage());
                        e3.printStackTrace();
                    }
                }
            }
            
            // Wenn wir Scores gefunden haben, durchsuche sie
            if (scores != null && !scores.isEmpty()) {
                System.out.println("🔍 [FarmworldCollections] Zone-Suche: " + scores.size() + " Scoreboard-Einträge über Collection gefunden");
                int index = 0;
                for (Object scoreObj : scores) {
                    if (scoreObj == null) {
                        
                        index++;
                        continue;
                    }
                    
                    System.out.println("🔍 [FarmworldCollections] Zone-Suche: Score #" + index + " Typ: " + scoreObj.getClass().getName());
                    
                    // Versuche Text aus Score-Objekt zu extrahieren
                    String text = null;
                    String rawText = null;
                    try {
                        // Methode 1: getDisplayName()
                        try {
                            java.lang.reflect.Method getDisplayNameMethod = scoreObj.getClass().getMethod("getDisplayName");
                            Text displayName = (Text) getDisplayNameMethod.invoke(scoreObj);
                            if (displayName != null) {
                                rawText = displayName.getString();
                                text = rawText;
                                System.out.println("  ✅ getDisplayName() erfolgreich");
                            }
                        } catch (Exception e) {
                            System.out.println("  ⚠️ getDisplayName() fehlgeschlagen: " + e.getMessage());
                            // Methode 2: playerName Feld
                            try {
                                java.lang.reflect.Field playerNameField = scoreObj.getClass().getDeclaredField("playerName");
                                playerNameField.setAccessible(true);
                                rawText = (String) playerNameField.get(scoreObj);
                                text = rawText;
                                System.out.println("  ✅ playerName Feld erfolgreich");
                            } catch (Exception e2) {
                                System.out.println("  ⚠️ playerName Feld fehlgeschlagen: " + e2.getMessage());
                                // Methode 3: Durchsuche alle Felder nach String-Feldern
                                java.lang.reflect.Field[] fields = scoreObj.getClass().getDeclaredFields();
                                System.out.println("  🔍 Durchsuche " + fields.length + " Felder...");
                                for (java.lang.reflect.Field field : fields) {
                                    if (field.getType() == String.class) {
                                        field.setAccessible(true);
                                        Object fieldValue = field.get(scoreObj);
                                        if (fieldValue instanceof String && !((String) fieldValue).isEmpty()) {
                                            rawText = (String) fieldValue;
                                            text = rawText;
                                            System.out.println("  ✅ String-Feld gefunden: " + field.getName() + " = '" + rawText + "'");
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ [FarmworldCollections] Zone-Suche: Fehler beim Extrahieren von Text aus Score #" + index + ": " + e.getMessage());
                        e.printStackTrace();
                        index++;
                        continue;
                    }
                    
                    if (text == null || text.isEmpty()) {
                        
                        index++;
                        continue;
                    }
                    
                    String cleanText = removeFormatting.apply(text);
                    
                    
                    // Prüfe ob dieser Text eine Zone enthält
                    boolean zoneFound = false;
                    for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                        if (cleanText.contains(zoneName)) {
                            
                            return zoneName;
                        }
                    }
                    if (!zoneFound) {
                        System.out.println("  ⚠️ Keine Zone in diesem Eintrag gefunden");
                    }
                    index++;
                }
                System.out.println("📊 [FarmworldCollections] Zone-Suche: Alle " + scores.size() + " Collection-Einträge durchsucht, keine Zone gefunden");
            } else {
                
                
                // Ansatz 4: Versuche alle Player-Namen aus den Maps zu extrahieren
                
                try {
                    java.lang.reflect.Field[] fields = scoreboard.getClass().getDeclaredFields();
                    java.util.Set<String> playerNames = new java.util.HashSet<>();
                    
                    for (java.lang.reflect.Field field : fields) {
                        if (java.util.Map.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            Object fieldValue = field.get(scoreboard);
                            if (fieldValue instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<?, ?> map = (java.util.Map<?, ?>) fieldValue;
                                
                                // Wenn der Key ein String ist, könnte es ein Player-Name sein
                                for (Object key : map.keySet()) {
                                    if (key instanceof String) {
                                        playerNames.add((String) key);
                                    }
                                }
                                
                                // Wenn der Value eine Map ist, durchsuche auch diese
                                for (Object value : map.values()) {
                                    if (value instanceof java.util.Map) {
                                        @SuppressWarnings("unchecked")
                                        java.util.Map<?, ?> innerMap = (java.util.Map<?, ?>) value;
                                        for (Object innerKey : innerMap.keySet()) {
                                            if (innerKey instanceof String) {
                                                playerNames.add((String) innerKey);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    System.out.println("🔍 [FarmworldCollections] Zone-Suche: " + playerNames.size() + " Player-Namen aus Maps extrahiert");
                    
                    // WICHTIG: Prüfe zuerst, ob einer der Player-Namen eine Zone enthält!
                    for (String playerName : playerNames) {
                        System.out.println("  📋 Player-Name: '" + playerName + "'");
                        String cleanPlayerName = removeFormatting.apply(playerName);
                        
                        // Prüfe ob dieser Player-Name eine Zone enthält
                        for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                            if (cleanPlayerName.contains(zoneName)) {
                                
                                return zoneName;
                            }
                        }
                    }
                    
                    // Versuche für jeden Player-Namen den Score zu holen
                    java.lang.reflect.Method getPlayerScoreMethod = null;
                    try {
                        getPlayerScoreMethod = scoreboard.getClass().getMethod("getPlayerScore", String.class, ScoreboardObjective.class);
                    } catch (Exception e) {
                        // Versuche alternative Methoden-Signaturen
                        try {
                            getPlayerScoreMethod = scoreboard.getClass().getMethod("method_835", String.class, ScoreboardObjective.class);
                        } catch (Exception e2) {
                            // Weitere Versuche...
                        }
                    }
                    
                    if (getPlayerScoreMethod == null) {
                        
                    } else {
                    
                        // Verwende die extrahierten Player-Namen
                        java.util.List<String> playerNamesToTry = new java.util.ArrayList<>(playerNames);
                        
                        // Füge auch Standard-Formate hinzu
                        for (int i = 0; i < 16; i++) {
                            String hex = Integer.toHexString(i);
                            playerNamesToTry.add("§" + hex);
                        }
                        playerNamesToTry.add("§r");
                        
                        System.out.println("🔍 [FarmworldCollections] Zone-Suche: Teste " + playerNamesToTry.size() + " Player-Namen...");
                        
                        int foundScores = 0;
                        for (String playerName : playerNamesToTry) {
                        try {
                            Object scoreObj = getPlayerScoreMethod.invoke(scoreboard, playerName, sidebarObjective);
                            
                            if (scoreObj != null) {
                                foundScores++;
                                // Versuche Text zu extrahieren
                                String text = null;
                                String rawText = null;
                                try {
                                    // Methode 1: getDisplayName()
                                    java.lang.reflect.Method getDisplayNameMethod = scoreObj.getClass().getMethod("getDisplayName");
                                    Text displayName = (Text) getDisplayNameMethod.invoke(scoreObj);
                                    if (displayName != null) {
                                        rawText = displayName.getString();
                                        text = rawText;
                                    }
                                } catch (Exception e) {
                                    // Methode 2: playerName Feld
                                    try {
                                        java.lang.reflect.Field playerNameField = scoreObj.getClass().getDeclaredField("playerName");
                                        playerNameField.setAccessible(true);
                                        rawText = (String) playerNameField.get(scoreObj);
                                        text = rawText;
                                    } catch (Exception e2) {
                                        // Methode 3: Alle String-Felder durchsuchen
                                        java.lang.reflect.Field[] scoreFields = scoreObj.getClass().getDeclaredFields();
                                        for (java.lang.reflect.Field field : scoreFields) {
                                            if (field.getType() == String.class) {
                                                field.setAccessible(true);
                                                Object fieldValue = field.get(scoreObj);
                                                if (fieldValue instanceof String && !((String) fieldValue).isEmpty()) {
                                                    rawText = (String) fieldValue;
                                                    text = rawText;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                if (text != null && !text.isEmpty()) {
                                    String cleanText = removeFormatting.apply(text);
                                    
                                    
                                    // Prüfe ob dieser Text eine Zone enthält
                                    for (String zoneName : ZONE_TO_COLLECTION.keySet()) {
                                        if (cleanText.contains(zoneName)) {
                                            
                                            return zoneName;
                                        }
                                    }
                                } else {
                                    // Zeige auch Score-Objekte ohne Text
                                    System.out.println("📋 [FarmworldCollections] Zone-Suche: Player-Name '" + playerName + "' → Score-Objekt gefunden, aber kein Text extrahiert (Typ: " + scoreObj.getClass().getName() + ")");
                                }
                            }
                        } catch (Exception e) {
                            // Ignoriere einzelne Fehler
                        }
                    }
                    
                        
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ [FarmworldCollections] Zone-Suche: Player-Name-Ansatz fehlgeschlagen: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Keine Zone gefunden - silent return
        } catch (Exception e) {
            System.err.println("❌ [FarmworldCollections] Fehler bei Zone-Suche: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    // Statische Variable für den aktuellen Collection-Wert (wird vom BossBarMixin gesetzt)
    private static int currentBossbarCollection = 0;
    private static long lastBossbarUpdate = 0;
    
    /**
     * Liest den Collection-Wert aus der Bossbar
     * Wird vom BossBarMixin aufgerufen, wenn eine Collection-Bossbar gefunden wird
     */
    public static void processBossBarCollection(String bossBarName) {
        try {
            int collection = decodeChineseNumber(bossBarName);
            if (collection >= 0) {
                currentBossbarCollection = collection;
                lastBossbarUpdate = System.currentTimeMillis();
            }
        } catch (Exception e) {
            System.err.println("❌ [FarmworldCollections] Fehler bei Bossbar-Verarbeitung: " + e.getMessage());
        }
    }
    
    /**
     * Gibt den aktuellen Collection-Wert aus der Bossbar zurück
     */
    private int getCollectionFromBossbar(MinecraftClient client) {
        // Prüfe ob der Wert noch aktuell ist (max. 5 Sekunden alt)
        long currentTime = System.currentTimeMillis();
        long age = currentTime - lastBossbarUpdate;
        if (age > 5000) {
            // Wert ist zu alt, zurück zu 0
            return 0;
        }
        return currentBossbarCollection;
    }
    
    /**
     * Dekodiert eine chinesische Zahl aus einem Text
     */
    private static int decodeChineseNumber(String text) {
        try {
            StringBuilder numberStr = new StringBuilder();
            for (char c : text.toCharArray()) {
                Integer digit = CHINESE_NUMBERS.get(c);
                if (digit != null) {
                    numberStr.append(digit);
                }
            }
            if (numberStr.length() > 0) {
                return Integer.parseInt(numberStr.toString());
            }
        } catch (Exception e) {
            // Silent error handling
        }
        return -1;
    }
    
    /**
     * Gibt den Collection-Namen für eine Zone zurück
     */
    private String getCollectionNameForZone(String zone) {
        return ZONE_TO_COLLECTION.get(zone);
    }
    
    @Override
    public void shutdown() {
        // Speichere aktuelle Collection beim Shutdown (forceSend=true, damit Wert gesendet wird)
        if (lastZone != null && lastTotalCollection > 0) {
            updateZoneScore(lastZone, lastTotalCollection, true);
        }
        
        isActive = false;
        System.out.println("🛑 FarmworldCollectionsCollector gestoppt");
    }
    
    @Override
    public String getName() {
        return "FarmworldCollectionsCollector";
    }
    
    @Override
    public boolean isActive() {
        return isActive;
    }
}


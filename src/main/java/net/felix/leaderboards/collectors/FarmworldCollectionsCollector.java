package net.felix.leaderboards.collectors;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.leaderboards.LeaderboardManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
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
    private int pendingZoneTicks = 0; // Ticks seit Zone-Wechsel
    private static final int ZONE_READ_DELAY_TICKS = 100; // 5 Sekunden (20 TPS), bis Bossbar-Wert gelesen wird
    private String stableZone = null; // Zone, deren Bossbar-Wert bereits verlässlich gelesen wurde
    private static boolean bossBarReadingAllowed = false; // Bossbar erst nach Verzögerung auswerten
    
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
        ZONE_TO_COLLECTION.put("[Obsidianmine]", "obsidian_collection");
        ZONE_TO_COLLECTION.put("[Amethystmine]", "amethyst_collection");
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
            // System.out.println("⚠️ [FarmworldCollectionsCollector] Bereits initialisiert - überspringe");
            return;
        }
        
        // System.out.println("🔍 [FarmworldCollectionsCollector] Starte Initialisierung...");
        // Registriere Tick-Event für Collection-Tracking
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        isActive = true;
        // System.out.println("✅ [FarmworldCollectionsCollector] FarmworldCollectionsCollector initialisiert und aktiv");
    }
    
    private void onClientTick(Minecraft client) {
        if (!isActive || client.player == null || client.level == null) {
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

        // Bossbar-Wert nach Zone-Wechsel erst nach 5 Sekunden lesen
        if (pendingZone != null) {
            pendingZoneTicks++;
            if (pendingZoneTicks >= ZONE_READ_DELAY_TICKS) {
                completePendingZoneRead(client);
            }
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
    private boolean isInFarmworldDimension(Minecraft client) {
        try {
            if (client.level == null) {
                return false;
            }
            
            // Erste Bedingung: Dimension muss minecraft:overworld sein
            String dimensionId = client.level.dimension().identifier().toString();
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
            Minecraft client = Minecraft.getInstance();
            if (client == null) return;
            
            String currentZone = getCurrentZone(client);
            if (currentZone == null) return;
            
            // Nur cachen wenn wir noch in der gleichen Zone sind UND die Zone stabil ist
            if (currentZone.equals(lastZone) && stableZone != null && currentZone.equals(stableZone) && bossBarReadingAllowed) {
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
    private void checkZoneChange(Minecraft client) {
        try {
            String currentZone = getCurrentZone(client);
            if (currentZone == null) {
                pendingZone = null;
                pendingZoneTicks = 0;
                stableZone = null;
                bossBarReadingAllowed = false;
                return;
            }
            
            // Prüfe ob sich die Zone geändert hat
            if (!currentZone.equals(lastZone)) {
                // System.out.println("🔄 [FarmworldCollections] ZONE-WECHSEL: " + lastZone + " → " + currentZone);
                
                // WICHTIG: Prüfe SOFORT ob die neue Zone gesperrt ist (bevor wir lastZone aktualisieren)
                // Wenn die Zone gesperrt ist, ignorieren wir sie komplett
                if (isZoneLocked()) {
                    pendingZone = null;
                    pendingZoneTicks = 0;
                    stableZone = null;
                    bossBarReadingAllowed = false;
                    return;
                }
                
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

                // Neue Zone als "pending" markieren - warte 5 Sekunden, bevor Bossbar gelesen wird
                pendingZone = currentZone;
                pendingZoneTicks = 0;
                stableZone = null;
                bossBarReadingAllowed = false;
                currentBossbarCollection = 0;
                lastBossbarUpdate = 0;
                lastZone = currentZone;
            } else if (pendingZone != null && currentZone.equals(pendingZone)) {
                // Wartezeit läuft in onClientTick (pendingZoneTicks)
                
                // Prüfe ob sich die Zone während der Wartezeit geändert hat
                // (kann passieren bei Teleportation - Scoreboard aktualisiert sich verzögert)
                String detectedZone = getCurrentZone(client);
                if (detectedZone != null && !detectedZone.equals(pendingZone)) {
                    // Zone hat sich geändert - reset pendingZone und starte neu
                    // WICHTIG: Setze lastZone zurück, damit wir nicht den falschen Wert senden
                    lastZone = null;
                    pendingZone = null;
                    pendingZoneTicks = 0;
                    stableZone = null;
                    bossBarReadingAllowed = false;
                    return;
                }
                
                // Prüfe ob "Nicht Freigeschalten!" im Title/Subtitle steht
                if (isZoneLocked()) {
                    lastZone = null;
                    pendingZone = null;
                    pendingZoneTicks = 0;
                    stableZone = null;
                    bossBarReadingAllowed = false;
                    return;
                }
            } else if (stableZone != null && currentZone.equals(stableZone) && bossBarReadingAllowed) {
                // Zone ist bereits stabil - aktualisiere lastTotalCollection kontinuierlich (nur wenn höher)
                int currentCollection = getCollectionFromBossbar(client);
                if (currentCollection > lastTotalCollection) {
                    lastTotalCollection = currentCollection;
                }
            } else {
                // Gleiche Zone, aber noch nicht stabil - warte weiter
                // (Dieser Fall sollte eigentlich nicht auftreten, aber zur Sicherheit)
            }
        } catch (Exception e) {
            System.err.println("❌ [FarmworldCollections] Fehler bei Zone-Check: " + e.getMessage());
        }
    }
    
    /**
     * Liest nach Ablauf der Zone-Wartezeit den ersten Bossbar-Wert und aktiviert weiteres Tracking.
     */
    private void completePendingZoneRead(Minecraft client) {
        if (pendingZone == null) {
            return;
        }

        if (isZoneLocked()) {
            lastZone = null;
            pendingZone = null;
            pendingZoneTicks = 0;
            stableZone = null;
            bossBarReadingAllowed = false;
            return;
        }

        String currentZone = getCurrentZone(client);
        if (currentZone == null || !currentZone.equals(pendingZone)) {
            return;
        }

        stableZone = pendingZone;
        bossBarReadingAllowed = true;
        pendingZone = null;
        pendingZoneTicks = 0;

        int newZoneCollection = getCollectionFromBossbar(client);
        lastTotalCollection = newZoneCollection;
        cachedCollectionForCurrentZone = newZoneCollection;

        long cachedCollection = zoneCollections.getOrDefault(stableZone, 0L);
        if (newZoneCollection < cachedCollection) {
            zoneCollections.put(stableZone, (long) newZoneCollection);
            if (ENABLE_SERVER_UPDATES) {
                LeaderboardManager.getInstance().updateScore(getCollectionNameForZone(stableZone), newZoneCollection);
            }
        }
    }
    
    /**
     * Aktualisiert Collection-Daten in der aktuellen Zone (alle 60 Sekunden)
     */
    private void updateCollectionInCurrentZone(Minecraft client) {
        try {
            String currentZone = getCurrentZone(client);
            if (currentZone == null) {
                return;
            }
            
            // Nur updaten wenn wir noch in der gleichen Zone sind UND die Zone stabil ist (10 Sekunden im Scoreboard)
            if (currentZone.equals(lastZone) && stableZone != null && currentZone.equals(stableZone) && bossBarReadingAllowed) {
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
    private String getCurrentZone(Minecraft client) {
        if (client == null || client.level == null) return null;
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return null;

        // Use the same public API and team decoration as the 26.3 sidebar renderer.
        for (net.minecraft.world.scores.PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
            if (entry.isHidden()) continue;
            Component line = net.minecraft.world.scores.PlayerTeam.formatNameForTeam(
                    scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
            String clean = line.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
            for (String zone : ZONE_TO_COLLECTION.keySet()) {
                if (clean.contains(zone)) return zone;
            }
        }
        return null;
    }
    
    // Statische Variable für den aktuellen Collection-Wert (wird vom BossBarMixin gesetzt)
    private static int currentBossbarCollection = 0;
    private static long lastBossbarUpdate = 0;
    
    // Statische Variablen für Title/Subtitle (werden vom TitleMixin gesetzt)
    private static String currentTitle = "";
    private static String currentSubtitle = "";
    private static long lastTitleUpdate = 0;
    private static final long TITLE_CACHE_TIMEOUT = 3000; // 3 Sekunden Cache-Timeout
    
    /**
     * Liest den Collection-Wert aus der Bossbar
     * Wird vom BossBarMixin aufgerufen, wenn eine Collection-Bossbar gefunden wird
     */
    public static void processBossBarCollection(String bossBarName) {
        if (!bossBarReadingAllowed) {
            return;
        }
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
    private int getCollectionFromBossbar(Minecraft client) {
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
    
    /**
     * Wird vom TitleMixin aufgerufen, wenn ein Title gesetzt wird
     */
    public static void processTitle(String title) {
        if (title != null) {
            currentTitle = title;
            lastTitleUpdate = System.currentTimeMillis();
        }
    }
    
    /**
     * Wird vom TitleMixin aufgerufen, wenn ein Subtitle gesetzt wird
     */
    public static void processSubtitle(String subtitle) {
        if (subtitle != null) {
            currentSubtitle = subtitle;
            lastTitleUpdate = System.currentTimeMillis();
        }
    }
    
    /**
     * Prüft ob die aktuelle Zone nicht freigeschalten ist
     * (wenn "Nicht Freigeschalten!" im Title oder Subtitle steht)
     */
    private boolean isZoneLocked() {
        // Prüfe ob Title/Subtitle noch aktuell sind (max. 3 Sekunden alt)
        long currentTime = System.currentTimeMillis();
        long age = currentTime - lastTitleUpdate;
        if (age > TITLE_CACHE_TIMEOUT) {
            // Title ist zu alt, ignoriere Check
            return false;
        }
        
        // Entferne Formatierungscodes für bessere Erkennung
        String cleanTitle = removeFormatCodes(currentTitle);
        String cleanSubtitle = removeFormatCodes(currentSubtitle);
        
        // Prüfe ob "Nicht Freigeschalten!" im Title oder Subtitle steht
        return cleanTitle.contains("Nicht Freigeschalten!") || 
               cleanSubtitle.contains("Nicht Freigeschalten!");
    }
    
    /**
     * Entfernt Minecraft-Formatierungscodes aus einem String
     */
    private String removeFormatCodes(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
    
    @Override
    public void shutdown() {
        // Speichere aktuelle Collection beim Shutdown (forceSend=true, damit Wert gesendet wird)
        if (lastZone != null && lastTotalCollection > 0) {
            updateZoneScore(lastZone, lastTotalCollection, true);
        }
        
        isActive = false;
        // System.out.println("🛑 FarmworldCollectionsCollector gestoppt");
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


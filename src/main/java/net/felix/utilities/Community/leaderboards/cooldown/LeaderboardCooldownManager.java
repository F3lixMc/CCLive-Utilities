package net.felix.utilities.Community.leaderboards.cooldown;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet Cooldowns für Leaderboard-Updates
 * Verhindert zu häufige Server-Anfragen und schont Ressourcen
 */
public class LeaderboardCooldownManager {
    private static LeaderboardCooldownManager instance;
    
    // Cooldown-Zeiten in Millisekunden
    private static final long DEFAULT_SCORE_UPDATE_COOLDOWN = 30000; // 30 Sekunden
    private static final long DEFAULT_LEADERBOARD_FETCH_COOLDOWN = 60000; // 1 Minute
    
    // Spezifische Cooldowns für verschiedene Leaderboard-Typen
    private static final Map<String, Long> SCORE_UPDATE_COOLDOWNS = new HashMap<>();
    private static final Map<String, Long> LEADERBOARD_FETCH_COOLDOWNS = new HashMap<>();
    
    static {
        // Coins - längerer Cooldown da nur stündlich aktualisiert
        SCORE_UPDATE_COOLDOWNS.put("current_coins", 300000L); // 5 Minuten
        LEADERBOARD_FETCH_COOLDOWNS.put("current_coins", 300000L); // 5 Minuten
        
        // Collections - mittlerer Cooldown
        SCORE_UPDATE_COOLDOWNS.put("*_collection", 60000L); // 1 Minute
        LEADERBOARD_FETCH_COOLDOWNS.put("*_collection", 120000L); // 2 Minuten
        
        // Floor Kills - kurzer Cooldown da aktiv gespielt
        SCORE_UPDATE_COOLDOWNS.put("floor_*", 15000L); // 15 Sekunden
        LEADERBOARD_FETCH_COOLDOWNS.put("floor_*", 60000L); // 1 Minute
        
        // Rare Mobs - mittlerer Cooldown
        SCORE_UPDATE_COOLDOWNS.put("alltime_rare_mob_kills", 60000L); // 1 Minute
        LEADERBOARD_FETCH_COOLDOWNS.put("alltime_rare_mob_kills", 120000L); // 2 Minuten
        
        // Alltime Kills - mittlerer Cooldown
        SCORE_UPDATE_COOLDOWNS.put("alltime_kills", 60000L); // 1 Minute
        LEADERBOARD_FETCH_COOLDOWNS.put("alltime_kills", 120000L); // 2 Minuten
    }
    
    // Tracking der letzten Updates/Fetches
    private final Map<String, Long> lastScoreUpdates = new ConcurrentHashMap<>();
    private final Map<String, Long> lastLeaderboardFetches = new ConcurrentHashMap<>();
    
    // Warteschlange für verzögerte Updates
    private final Map<String, Object> pendingScoreUpdates = new ConcurrentHashMap<>();
    
    private LeaderboardCooldownManager() {}
    
    public static LeaderboardCooldownManager getInstance() {
        if (instance == null) {
            instance = new LeaderboardCooldownManager();
        }
        return instance;
    }
    
    /**
     * Prüft ob ein Score-Update erlaubt ist
     */
    public boolean canUpdateScore(String leaderboardName) {
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastScoreUpdates.get(leaderboardName);
        
        if (lastUpdate == null) {
            return true; // Erstes Update immer erlaubt
        }
        
        long cooldown = getScoreUpdateCooldown(leaderboardName);
        return (currentTime - lastUpdate) >= cooldown;
    }
    
    /**
     * Prüft ob ein Leaderboard-Fetch erlaubt ist
     */
    public boolean canFetchLeaderboard(String leaderboardName) {
        long currentTime = System.currentTimeMillis();
        Long lastFetch = lastLeaderboardFetches.get(leaderboardName);
        
        if (lastFetch == null) {
            return true; // Erster Fetch immer erlaubt
        }
        
        long cooldown = getLeaderboardFetchCooldown(leaderboardName);
        return (currentTime - lastFetch) >= cooldown;
    }
    
    /**
     * Markiert ein Score-Update als durchgeführt
     */
    public void markScoreUpdated(String leaderboardName) {
        lastScoreUpdates.put(leaderboardName, System.currentTimeMillis());
        
        // Entferne aus pending Updates falls vorhanden
        pendingScoreUpdates.remove(leaderboardName);
    }
    
    /**
     * Markiert ein Leaderboard-Fetch als durchgeführt
     */
    public void markLeaderboardFetched(String leaderboardName) {
        lastLeaderboardFetches.put(leaderboardName, System.currentTimeMillis());
    }
    
    /**
     * Fügt ein Score-Update zur Warteschlange hinzu
     */
    public void queueScoreUpdate(String leaderboardName, Object scoreValue) {
        pendingScoreUpdates.put(leaderboardName, scoreValue);
    }
    
    /**
     * Gibt die Zeit bis zum nächsten möglichen Score-Update zurück (in Millisekunden)
     */
    public long getTimeUntilNextScoreUpdate(String leaderboardName) {
        Long lastUpdate = lastScoreUpdates.get(leaderboardName);
        if (lastUpdate == null) {
            return 0; // Sofort möglich
        }
        
        long cooldown = getScoreUpdateCooldown(leaderboardName);
        long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdate;
        
        return Math.max(0, cooldown - timeSinceLastUpdate);
    }
    
    /**
     * Gibt die Zeit bis zum nächsten möglichen Leaderboard-Fetch zurück (in Millisekunden)
     */
    public long getTimeUntilNextLeaderboardFetch(String leaderboardName) {
        Long lastFetch = lastLeaderboardFetches.get(leaderboardName);
        if (lastFetch == null) {
            return 0; // Sofort möglich
        }
        
        long cooldown = getLeaderboardFetchCooldown(leaderboardName);
        long timeSinceLastFetch = System.currentTimeMillis() - lastFetch;
        
        return Math.max(0, cooldown - timeSinceLastFetch);
    }
    
    /**
     * Gibt alle wartenden Score-Updates zurück
     */
    public Map<String, Object> getPendingScoreUpdates() {
        return new HashMap<>(pendingScoreUpdates);
    }
    
    /**
     * Prüft ob Score-Updates wartend sind
     */
    public boolean hasPendingScoreUpdates() {
        return !pendingScoreUpdates.isEmpty();
    }
    
    /**
     * Ermittelt das Score-Update-Cooldown für ein Leaderboard
     */
    private long getScoreUpdateCooldown(String leaderboardName) {
        // Prüfe spezifische Cooldowns
        for (Map.Entry<String, Long> entry : SCORE_UPDATE_COOLDOWNS.entrySet()) {
            if (matchesPattern(leaderboardName, entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return DEFAULT_SCORE_UPDATE_COOLDOWN;
    }
    
    /**
     * Ermittelt das Leaderboard-Fetch-Cooldown für ein Leaderboard
     */
    private long getLeaderboardFetchCooldown(String leaderboardName) {
        // Prüfe spezifische Cooldowns
        for (Map.Entry<String, Long> entry : LEADERBOARD_FETCH_COOLDOWNS.entrySet()) {
            if (matchesPattern(leaderboardName, entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return DEFAULT_LEADERBOARD_FETCH_COOLDOWN;
    }
    
    /**
     * Prüft ob ein Leaderboard-Name einem Pattern entspricht
     */
    private boolean matchesPattern(String leaderboardName, String pattern) {
        if (pattern.equals(leaderboardName)) {
            return true; // Exakte Übereinstimmung
        }
        
        if (pattern.contains("*")) {
            // Wildcard-Pattern
            String regex = pattern.replace("*", ".*");
            return leaderboardName.matches(regex);
        }
        
        return false;
    }
    
    /**
     * Formatiert eine Zeit in Millisekunden zu einem lesbaren String
     */
    public String formatTimeRemaining(long milliseconds) {
        if (milliseconds <= 0) {
            return "Jetzt verfügbar";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Resettet alle Cooldowns (für Testing oder Admin-Funktionen)
     */
    public void resetAllCooldowns() {
        lastScoreUpdates.clear();
        lastLeaderboardFetches.clear();
        pendingScoreUpdates.clear();
        // Silent error handling("🔄 Alle Leaderboard-Cooldowns zurückgesetzt");
    }
    
    /**
     * Resettet Cooldowns für ein bestimmtes Leaderboard
     */
    public void resetCooldown(String leaderboardName) {
        lastScoreUpdates.remove(leaderboardName);
        lastLeaderboardFetches.remove(leaderboardName);
        pendingScoreUpdates.remove(leaderboardName);
        // Silent error handling("🔄 Cooldown für " + leaderboardName + " zurückgesetzt");
    }
}

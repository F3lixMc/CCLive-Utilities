package net.felix.leaderboards.collectors;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.leaderboards.LeaderboardManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import java.util.HashMap;
import java.util.Map;

/**
 * Sammelt allgemeine Spieler-Statistiken wie Kills, Coins, etc.
 */
public class StatsCollector implements DataCollector {
    private boolean isActive = false;
    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 600; // Alle 30 Sekunden (20 ticks/sec * 30)
    
    // Cache für letzte Werte
    private final Map<String, Long> lastValues = new HashMap<>();
    
    @Override
    public void initialize() {
        if (isActive) return;
        
        // Registriere Tick-Event für regelmäßige Updates
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        isActive = true;
        // Silent error handling("✅ StatsCollector initialisiert");
    }
    
    private void onClientTick(Minecraft client) {
        if (!isActive || client.player == null || client.level == null) {
            return;
        }
        
        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            // Führe updateStats asynchron aus, um Freezes zu vermeiden
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                updateStats(client);
            });
        }
    }
    
    /**
     * Aktualisiert alle Statistiken
     */
    private void updateStats(Minecraft client) {
        try {
            Scoreboard scoreboard = client.level.getScoreboard();
            if (scoreboard == null) return;
            
            // Suche nach relevanten Scoreboard-Objektiven
            for (Objective objective : scoreboard.getObjectives()) {
                String objectiveName = objective.getName();
                
                // Prüfe verschiedene mögliche Scoreboard-Namen
                if (isRelevantObjective(objectiveName)) {
                    updateScoreboardStats(scoreboard, objective, client);
                }
            }
            
            // Fallback: Versuche Statistiken aus anderen Quellen zu lesen
            updateFallbackStats(client);
            
        } catch (Exception e) {
            System.err.println("❌ Fehler beim Aktualisieren der Statistiken: " + e.getMessage());
        }
    }
    
    /**
     * Prüft ob ein Scoreboard-Objektiv relevant ist
     */
    private boolean isRelevantObjective(String name) {
        return name.toLowerCase().contains("kill") || 
               name.toLowerCase().contains("coin") || 
               name.toLowerCase().contains("money") ||
               name.toLowerCase().contains("death") ||
               name.toLowerCase().contains("score");
    }
    
    /**
     * Aktualisiert Statistiken basierend auf Scoreboard-Daten
     */
    private void updateScoreboardStats(Scoreboard scoreboard, Objective objective, Minecraft client) {
        try {
            // TODO: ScoreboardPlayerScore ist in dieser MC-Version nicht verfügbar
            // Implementiere alternative Methode zur Score-Erfassung
            
            // Fallback: Verwende Reflection oder andere Methoden
            long score = 0; // Placeholder
            String objectiveName = objective.getName().toLowerCase();
            
            // Mappe Scoreboard-Namen auf Leaderboard-Namen
            String leaderboardName = mapScoreboardToLeaderboard(objectiveName);
            if (leaderboardName != null) {
                updateIfChanged(leaderboardName, score);
            }
        } catch (Exception e) {
            // Ignoriere Fehler bei einzelnen Scoreboards
        }
    }
    
    /**
     * Fallback-Methode für Statistiken ohne Scoreboard
     */
    private void updateFallbackStats(Minecraft client) {
        // TODO: Implementiere alternative Methoden zur Statistik-Erfassung
        // z.B. durch Chat-Nachrichten-Parsing, GUI-Analyse, etc.
        
        // Beispiel: Aktuelle Coins aus dem Inventar oder Chat
        // updateCoinsFromInventory(client);
    }
    
    /**
     * Mappt Scoreboard-Namen auf Leaderboard-Namen
     */
    private String mapScoreboardToLeaderboard(String scoreboardName) {
        if (scoreboardName.contains("kill")) {
            return "alltime_kills";
        } else if (scoreboardName.contains("coin") || scoreboardName.contains("money")) {
            return "current_coins";
        }
        // Weitere Mappings hier hinzufügen
        return null;
    }
    
    /**
     * Aktualisiert einen Wert nur wenn er sich geändert hat
     */
    private void updateIfChanged(String leaderboardName, long newValue) {
        Long lastValue = lastValues.get(leaderboardName);
        if (lastValue == null || !lastValue.equals(newValue)) {
            lastValues.put(leaderboardName, newValue);
            LeaderboardManager.getInstance().updateScore(leaderboardName, newValue);
        }
    }
    
    @Override
    public void shutdown() {
        isActive = false;
        // Silent error handling("🛑 StatsCollector gestoppt");
    }
    
    @Override
    public String getName() {
        return "StatsCollector";
    }
    
    @Override
    public boolean isActive() {
        return isActive;
    }
}

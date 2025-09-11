package net.felix.leaderboards.collectors;

import net.felix.leaderboards.LeaderboardManager;

/**
 * Sammelt Rare Mob-Daten (integriert mit der bestehenden MobTimerUtility)
 */
public class RareMobCollector implements DataCollector {
    private boolean isActive = false;
    private long rareMobKills = 0;
    
    @Override
    public void initialize() {
        if (isActive) return;
        
        // Diese Klasse fungiert hauptsächlich als Bridge zur MobTimerUtility
        // Die eigentliche Datensammlung erfolgt über die bestehende MobTimerUtility
        
        isActive = true;
        System.out.println("✅ RareMobCollector initialisiert (Bridge zu MobTimerUtility)");
    }
    
    /**
     * Wird von der MobTimerUtility aufgerufen wenn ein Rare Mob getötet wurde
     */
    public void onRareMobKilled() {
        if (!isActive) return;
        
        rareMobKills++;
        LeaderboardManager.getInstance().updateScore("alltime_rare_mob_kills", rareMobKills);
        
        System.out.println("💀 Rare Mob getötet! Total: " + rareMobKills);
    }
    
    /**
     * Wird von der MobTimerUtility aufgerufen wenn ein Rare Mob despawnt ist
     * (könnte für andere Statistiken verwendet werden)
     */
    public void onRareMobDespawned(String floor, long lifetimeSeconds) {
        if (!isActive) return;
        
        // TODO: Hier könnten weitere Statistiken gesammelt werden
        // z.B. durchschnittliche Lebensdauer pro Floor, etc.
        
        System.out.println("👻 Rare Mob despawned auf " + floor + " nach " + lifetimeSeconds + "s");
    }
    
    /**
     * Setzt die Anzahl der Rare Mob Kills manuell (für Initialisierung oder Korrektur)
     */
    public void setRareMobKills(long kills) {
        this.rareMobKills = kills;
        LeaderboardManager.getInstance().updateScore("alltime_rare_mob_kills", kills);
    }
    
    /**
     * Gibt die aktuelle Anzahl der Rare Mob Kills zurück
     */
    public long getRareMobKills() {
        return rareMobKills;
    }
    
    /**
     * Statische Methode für einfachen Zugriff aus der MobTimerUtility
     */
    public static void notifyRareMobKilled() {
        LeaderboardManager manager = LeaderboardManager.getInstance();
        if (manager.isEnabled()) {
            // Hole den RareMobCollector aus dem Manager
            // TODO: Implementiere Getter im LeaderboardManager falls nötig
            System.out.println("💀 Rare Mob Kill-Benachrichtigung empfangen");
        }
    }
    
    @Override
    public void shutdown() {
        isActive = false;
        System.out.println("🛑 RareMobCollector gestoppt");
    }
    
    @Override
    public String getName() {
        return "RareMobCollector";
    }
    
    @Override
    public boolean isActive() {
        return isActive;
    }
}

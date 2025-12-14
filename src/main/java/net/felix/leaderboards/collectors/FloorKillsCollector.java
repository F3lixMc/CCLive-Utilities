package net.felix.leaderboards.collectors;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.utilities.Aincraft.KillsUtility;
import net.felix.utilities.Aincraft.BPViewerUtility;

import java.util.HashMap;
import java.util.Map;

/**
 * Sammelt Floor-spezifische Kill-Daten
 * Integriert mit KillsUtility und BPViewerUtility
 */
public class FloorKillsCollector implements DataCollector {
    private boolean isActive = false;
    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 100; // Alle 5 Sekunden
    
    // Cache für Floor-Kill-Daten
    private final Map<String, Long> floorKills = new HashMap<>();
    private String lastFloor = null;
    private int lastNewKills = 0;
    
    @Override
    public void initialize() {
        if (isActive) return;
        
        // Registriere Tick-Event für Kill-Tracking
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        isActive = true;
        System.out.println("✅ FloorKillsCollector initialisiert");
    }
    
    private void onClientTick(MinecraftClient client) {
        if (!isActive || client.player == null || client.world == null) {
            return;
        }
        
        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            updateFloorKills();
        }
    }
    
    /**
     * Aktualisiert Floor-spezifische Kill-Daten
     */
    private void updateFloorKills() {
        try {
            // Hole aktuellen Floor vom BPViewer
            String currentFloor = getCurrentFloor();
            if (currentFloor == null) return;
            
            // Hole neue Kills von der KillsUtility
            int newKills = KillsUtility.getNewKills();
            
            // Prüfe ob sich der Floor geändert hat
            if (!currentFloor.equals(lastFloor)) {
                // Floor-Wechsel - aktualisiere vorherigen Floor falls vorhanden
                if (lastFloor != null && lastNewKills > 0) {
                    updateFloorScore(lastFloor, lastNewKills);
                }
                
                lastFloor = currentFloor;
                lastNewKills = newKills;
            } else if (newKills > lastNewKills) {
                // Neue Kills im gleichen Floor
                lastNewKills = newKills;
                updateFloorScore(currentFloor, newKills);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Fehler beim Aktualisieren der Floor-Kills: " + e.getMessage());
        }
    }
    
    /**
     * Aktualisiert den Score für einen Floor
     */
    private void updateFloorScore(String floor, int kills) {
        if (floor == null || kills <= 0) return;
        
        // Hole bisherige Kills für diesen Floor
        long currentTotal = floorKills.getOrDefault(floor, 0L);
        long newTotal = Math.max(currentTotal, (long) kills);
        
        if (newTotal > currentTotal) {
            floorKills.put(floor, newTotal);
            LeaderboardManager.getInstance().updateScore(floor, newTotal);
            
            System.out.println("🗡️ Floor-Kills Update: " + floor + " = " + newTotal + " kills");
        }
    }
    
    /**
     * Ermittelt den aktuellen Floor
     */
    private String getCurrentFloor() {
        try {
            // Verwende BPViewerUtility für konsistente Floor-Erkennung
            BPViewerUtility bpViewer = BPViewerUtility.getInstance();
            if (bpViewer != null) {
                return bpViewer.getActiveFloor();
            }
            
            // Fallback: Direkte Dimension-Abfrage
            return getCurrentFloorDirect();
            
        } catch (Exception e) {
            // Fallback bei Fehlern
            return getCurrentFloorDirect();
        }
    }
    
    /**
     * Direkte Floor-Erkennung als Fallback
     */
    private String getCurrentFloorDirect() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.world != null) {
                String dimensionId = client.world.getRegistryKey().getValue().toString();
                
                if (dimensionId.startsWith("minecraft:floor_")) {
                    String floorPart = dimensionId.substring("minecraft:floor_".length());
                    String[] parts = floorPart.split("_");
                    if (parts.length >= 1) {
                        String floorNumber = parts[0];
                        return "floor_" + floorNumber;
                    }
                }
            }
        } catch (Exception e) {
            // Ignoriere Fehler
        }
        return null;
    }
    
    /**
     * Setzt Floor-Kills manuell (für Testing oder Korrektur)
     */
    public void setFloorKills(String floor, long kills) {
        if (floor != null) {
            floorKills.put(floor, kills);
            LeaderboardManager.getInstance().updateScore(floor, kills);
        }
    }
    
    /**
     * Gibt die Kills für einen Floor zurück
     */
    public Long getFloorKills(String floor) {
        return floorKills.get(floor);
    }
    
    /**
     * Gibt den aktuellen Floor zurück
     */
    public String getCurrentFloorName() {
        return lastFloor;
    }
    
    /**
     * Gibt die aktuellen neuen Kills zurück
     */
    public int getCurrentNewKills() {
        return lastNewKills;
    }
    
    /**
     * Berechnet die Gesamtkills über alle Floors
     */
    public long getTotalKills() {
        return floorKills.values().stream().mapToLong(Long::longValue).sum();
    }
    
    /**
     * Gibt alle Floor-Kill-Daten zurück
     */
    public Map<String, Long> getAllFloorKills() {
        return new HashMap<>(floorKills);
    }
    
    /**
     * Resettet die Daten für einen Floor
     */
    public void resetFloorKills(String floor) {
        if (floor != null) {
            floorKills.remove(floor);
            LeaderboardManager.getInstance().updateScore(floor, 0);
        }
    }
    
    /**
     * Resettet alle Floor-Kill-Daten
     */
    public void resetAllFloorKills() {
        for (String floor : floorKills.keySet()) {
            LeaderboardManager.getInstance().updateScore(floor, 0);
        }
        floorKills.clear();
    }
    
    @Override
    public void shutdown() {
        // Speichere aktuelle Kills beim Shutdown
        if (lastFloor != null && lastNewKills > 0) {
            updateFloorScore(lastFloor, lastNewKills);
        }
        
        isActive = false;
        System.out.println("🛑 FloorKillsCollector gestoppt");
    }
    
    @Override
    public String getName() {
        return "FloorKillsCollector";
    }
    
    @Override
    public boolean isActive() {
        return isActive;
    }
}

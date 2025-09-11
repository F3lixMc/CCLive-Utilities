package net.felix.leaderboards.collectors;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.felix.leaderboards.LeaderboardManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Sammelt Floor-Progress-Daten (Zeit pro Ebene, erreichte Ebenen)
 */
public class FloorProgressCollector implements DataCollector {
    private boolean isActive = false;
    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 100; // Alle 5 Sekunden
    
    // Tracking-Daten
    private String currentFloor = null;
    private Instant floorStartTime = null;
    private final Map<String, Long> bestFloorTimes = new HashMap<>(); // in Millisekunden
    private final Map<String, Integer> floorCompletions = new HashMap<>();
    
    @Override
    public void initialize() {
        if (isActive) return;
        
        // Registriere Tick-Event für Floor-Überwachung
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        // Registriere Disconnect-Event
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (isActive) {
                handleFloorExit();
            }
        });
        
        isActive = true;
        System.out.println("✅ FloorProgressCollector initialisiert");
    }
    
    private void onClientTick(MinecraftClient client) {
        if (!isActive || client.player == null || client.world == null) {
            return;
        }
        
        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            checkFloorChange(client);
        }
    }
    
    /**
     * Prüft ob sich der aktuelle Floor geändert hat
     */
    private void checkFloorChange(MinecraftClient client) {
        try {
            String newFloor = getCurrentFloor(client);
            
            if (!isEqual(currentFloor, newFloor)) {
                // Floor hat sich geändert
                if (currentFloor != null) {
                    handleFloorExit();
                }
                
                if (newFloor != null) {
                    handleFloorEnter(newFloor);
                }
                
                currentFloor = newFloor;
            }
        } catch (Exception e) {
            System.err.println("❌ Fehler bei Floor-Überwachung: " + e.getMessage());
        }
    }
    
    /**
     * Behandelt das Betreten eines neuen Floors
     */
    private void handleFloorEnter(String floor) {
        floorStartTime = Instant.now();
        System.out.println("🏠 Floor betreten: " + floor);
    }
    
    /**
     * Behandelt das Verlassen eines Floors
     */
    private void handleFloorExit() {
        if (currentFloor != null && floorStartTime != null) {
            long timeSpent = Instant.now().toEpochMilli() - floorStartTime.toEpochMilli();
            
            // Aktualisiere Statistiken
            updateFloorStats(currentFloor, timeSpent);
            
            System.out.println("🏠 Floor verlassen: " + currentFloor + " (Zeit: " + (timeSpent / 1000) + "s)");
        }
        
        floorStartTime = null;
    }
    
    /**
     * Aktualisiert Floor-Statistiken
     */
    private void updateFloorStats(String floor, long timeSpent) {
        // Aktualisiere beste Zeit (nur wenn besser oder erste Zeit)
        Long currentBest = bestFloorTimes.get(floor);
        if (currentBest == null || timeSpent < currentBest) {
            bestFloorTimes.put(floor, timeSpent);
            
            // Sende beste Zeit in Sekunden an Server
            long timeInSeconds = timeSpent / 1000;
            LeaderboardManager.getInstance().updateScore(floor, timeInSeconds);
            
            System.out.println("🏆 Neue Bestzeit für " + floor + ": " + timeInSeconds + "s");
        }
        
        // Aktualisiere Completion-Count
        int completions = floorCompletions.getOrDefault(floor, 0) + 1;
        floorCompletions.put(floor, completions);
    }
    
    /**
     * Ermittelt den aktuellen Floor basierend auf der Welt-Dimension
     */
    private String getCurrentFloor(MinecraftClient client) {
        try {
            if (client.world == null) return null;
            
            String dimensionId = client.world.getRegistryKey().getValue().toString();
            
            if (dimensionId.startsWith("minecraft:floor_")) {
                String floorPart = dimensionId.substring("minecraft:floor_".length());
                String[] parts = floorPart.split("_");
                if (parts.length >= 1) {
                    String floorNumber = parts[0];
                    return "floor_" + floorNumber;
                }
            }
        } catch (Exception e) {
            // Ignoriere Fehler bei der Floor-Erkennung
        }
        return null;
    }
    
    /**
     * Hilfsmethode für String-Vergleich (null-safe)
     */
    private boolean isEqual(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }
    
    /**
     * Gibt die beste Zeit für einen Floor zurück
     */
    public Long getBestTime(String floor) {
        return bestFloorTimes.get(floor);
    }
    
    /**
     * Gibt die Anzahl der Completions für einen Floor zurück
     */
    public Integer getCompletions(String floor) {
        return floorCompletions.get(floor);
    }
    
    /**
     * Setzt eine Floor-Zeit manuell (für Testing)
     */
    public void setBestTime(String floor, long timeInSeconds) {
        bestFloorTimes.put(floor, timeInSeconds * 1000); // Konvertiere zu Millisekunden
        LeaderboardManager.getInstance().updateScore(floor, timeInSeconds);
    }
    
    /**
     * Gibt den aktuellen Floor zurück
     */
    public String getCurrentFloor() {
        return currentFloor;
    }
    
    /**
     * Gibt die aktuelle Floor-Zeit zurück (falls in einem Floor)
     */
    public Long getCurrentFloorTime() {
        if (floorStartTime != null) {
            return Instant.now().toEpochMilli() - floorStartTime.toEpochMilli();
        }
        return null;
    }
    
    @Override
    public void shutdown() {
        if (isActive && currentFloor != null) {
            handleFloorExit();
        }
        
        isActive = false;
        currentFloor = null;
        floorStartTime = null;
        
        System.out.println("🛑 FloorProgressCollector gestoppt");
    }
    
    @Override
    public String getName() {
        return "FloorProgressCollector";
    }
    
    @Override
    public boolean isActive() {
        return isActive;
    }
}

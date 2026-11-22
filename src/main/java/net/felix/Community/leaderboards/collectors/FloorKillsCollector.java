package net.felix.Community.leaderboards.collectors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.Community.chat.ignore.ChatIgnoreList;
import net.felix.Community.leaderboards.LeaderboardManager;
import net.felix.Community.leaderboards.sync.SeasonChangeGate;
import net.felix.utilities.Aincraft.blueprints.viewer.BPViewerUtility;
import net.felix.utilities.Aincraft.kills.KillsUtility;
import net.minecraft.client.Minecraft;

/**
 * Sammelt Floor-spezifische Kill-Daten
 * Integriert mit KillsUtility und BPViewerUtility
 */
public class FloorKillsCollector implements DataCollector {
    private boolean isActive = false;
    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 1200; // Alle 60 Sekunden
    
    // Cache-Mechanismus für Floor-Wechsel: Speichere Wert regelmäßig (alle 1 Sekunde)
    private int cacheUpdateCounter = 0;
    private static final int CACHE_UPDATE_INTERVAL = 20; // Alle 20 Ticks = 1 Sekunde
    private int cachedKillsForCurrentFloor = 0; // Gecachter Wert für den aktuellen Floor (wird regelmäßig aktualisiert)
    
    // Cache für Floor-Kill-Daten der gebundenen Seasonphase
    private final Map<String, Long> floorKills = new HashMap<>();
    private static FloorKillsCollector active;
    private String boundAccount;
    private int boundSeason = -1;
    private boolean boundPreseason;
    private boolean phaseBound;
    private String lastFloor = null;
    private int lastTotalKills = 0; // Absoluter Kill-Wert aus Bossbar
    
    // Alltime Kills Tracking
    private long lastSentAlltimeKills = 0; // Zuletzt gesendeter Alltime-Kills-Wert
    private long lastMenuAlltimeKills = 0; // Zuletzt aus Menü ausgelesener Wert (für Doppelcheck)
    
    @Override
    public void initialize() {
        if (isActive) {
            System.out.println("⚠️ [FloorKillsCollector] Bereits initialisiert - überspringe");
            return;
        }
        
        System.out.println("🔍 [FloorKillsCollector] Starte Initialisierung...");
        // Registriere Tick-Event für Kill-Tracking
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        active = this;
        
        isActive = true;
        System.out.println("✅ [FloorKillsCollector] FloorKillsCollector initialisiert und aktiv");
    }
    
    private void onClientTick(Minecraft client) {
        if (!isActive || client.player == null || client.level == null) {
            return;
        }
        
        // Cache-Mechanismus: Aktualisiere gecachten Wert regelmäßig (alle 1 Sekunde)
        // So haben wir beim Floor-Wechsel immer einen aktuellen Wert des alten Floors
        cacheUpdateCounter++;
        if (cacheUpdateCounter >= CACHE_UPDATE_INTERVAL) {
            cacheUpdateCounter = 0;
            updateCachedKills();
        }
        
        // Floor-Wechsel-Erkennung (alle 20 Ticks = 1x pro Sekunde)
        if (tickCounter % 20 == 0) {
            checkFloorChange();
        }
        
        // Kill-Updates nur alle 60 Sekunden
        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            updateKillsInCurrentFloor();
            updateAlltimeKills(); // Berechne und sende Alltime Kills
        }
    }
    
    /**
     * Aktualisiert den gecachten Kill-Wert für den aktuellen Floor
     * Wird regelmäßig aufgerufen (alle 1 Sekunde), damit wir beim Floor-Wechsel
     * den korrekten Wert des alten Floors haben, bevor die Bossbar aktualisiert wird
     */
    private void updateCachedKills() {
        try {
            String currentFloor = getCurrentFloor();
            if (currentFloor == null) return;
            
            // Nur cachen wenn wir noch im gleichen Floor sind
            if (currentFloor.equals(lastFloor)) {
                int currentKills = KillsUtility.getTotalKills();
                cachedKillsForCurrentFloor = currentKills;
                // Cache-Update Logging entfernt (zu häufig)
            }
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    /**
     * Prüft sofort bei jedem Tick, ob sich der Floor geändert hat
     * Speichert den vorherigen Floor sofort beim Wechsel
     * Aktualisiert auch lastTotalKills kontinuierlich, damit es immer aktuell ist
     */
    private void checkFloorChange() {
        try {
            String currentFloor = getCurrentFloor();
            if (currentFloor == null) return;
            
            // Prüfe ob sich der Floor geändert hat
            if (!currentFloor.equals(lastFloor)) {
                // Floor-Wechsel erkannt - speichere den vorherigen Floor
                // WICHTIG: Verwende den gecachten Wert statt den aktuellen Bossbar-Wert,
                // da die Bossbar bereits den Wert des neuen Floors zeigt!
                if (lastFloor != null && cachedKillsForCurrentFloor > 0) {
                    updateFloorScore(lastFloor, cachedKillsForCurrentFloor);
                    System.out.println("🔄 Floor-Wechsel erkannt: " + lastFloor + " → " + currentFloor + " (Gecachte Kills: " + cachedKillsForCurrentFloor + ", Aktuelle Bossbar: " + KillsUtility.getTotalKills() + ")");
                } else if (lastFloor != null && lastTotalKills > 0) {
                    // Fallback: Falls kein gecachter Wert vorhanden, verwende lastTotalKills
                    updateFloorScore(lastFloor, lastTotalKills);
                    System.out.println("🔄 Floor-Wechsel erkannt (Fallback): " + lastFloor + " → " + currentFloor + " (Kills: " + lastTotalKills + ")");
                }

                // Nach dem Speichern des vorherigen Floors Alltime-Kills sofort aktualisieren
                // (zusätzlich zum 60-Sekunden-Intervall), damit kurze Floors nicht verloren gehen
                updateAlltimeKills();

                // Neuen Floor setzen
                lastFloor = currentFloor;
                int newFloorKills = KillsUtility.getTotalKills();
                lastTotalKills = newFloorKills; // Initialisiere mit aktuellem Wert für neuen Floor
                cachedKillsForCurrentFloor = newFloorKills; // Cache auch für neuen Floor initialisieren
            } else {
                // Gleicher Floor - aktualisiere lastTotalKills kontinuierlich (nur wenn höher)
                // So haben wir beim Floor-Wechsel immer den aktuellsten Wert
                int currentKills = KillsUtility.getTotalKills();
                if (currentKills > lastTotalKills) {
                    lastTotalKills = currentKills;
                }
            }
        } catch (Exception e) {
            // Silent error handling für häufige Checks
        }
    }
    
    /**
     * Aktualisiert Kill-Daten im aktuellen Floor (alle 60 Sekunden)
     */
    private void updateKillsInCurrentFloor() {
        try {
            String currentFloor = getCurrentFloor();
            if (currentFloor == null) return;
            
            // Nur updaten wenn wir noch im gleichen Floor sind
            if (currentFloor.equals(lastFloor)) {
                int totalKills = KillsUtility.getTotalKills();
                
                // Aktualisiere lastTotalKills immer (auch wenn niedriger, für korrekte Floor-Wechsel-Erkennung)
                lastTotalKills = totalKills;
                
                long cachedKills = floorKills.getOrDefault(currentFloor, 0L);
                if (totalKills > cachedKills) {
                    updateFloorScore(currentFloor, totalKills);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Fehler beim Aktualisieren der Floor-Kills: " + e.getMessage());
        }
    }
    
    /**
     * Prüft ob ein Floor-Name gültig ist (nur floor_1 bis floor_100)
     */
    private boolean isValidFloorName(String floor) {
        if (floor == null || !floor.startsWith("floor_")) {
            return false;
        }
        String floorNumber = floor.substring("floor_".length());
        try {
            int floorNum = Integer.parseInt(floorNumber);
            return floorNum >= 1 && floorNum <= 100; // Erlaube floor_1 bis floor_100
        } catch (NumberFormatException e) {
            return false; // Ungültige Floor-Namen wie "all", "legendary", "none" werden abgelehnt
        }
    }
    
    /**
     * Aktualisiert den Score für einen Floor
     */
    private void updateFloorScore(String floor, long kills) {
        if (floor == null || kills < 0) return; // Erlaube auch 0 (falls Bossbar 0 zeigt)
        
        // Validiere Floor-Name - ignoriere ungültige Floor-Namen
        if (!isValidFloorName(floor)) {
            System.out.println("⚠️ [FloorKillsCollector] Ungültiger Floor-Name ignoriert: " + floor);
            return;
        }
        
        usePhase();
        if (!phaseBound) {
            return;
        }
        long currentTotal = floorKills.getOrDefault(floor, 0L);
        long newTotal = FloorKillStore.shared().read(boundAccount, boundSeason, boundPreseason, floor, kills);
        
        // Nur updaten wenn der gespeicherte Stand gestiegen ist
        if (newTotal != currentTotal) {
            floorKills.put(floor, newTotal);
            // Nur bei signifikanten Änderungen loggen (>100 kills Unterschied)
            if (Math.abs(newTotal - currentTotal) > 100) {
                System.out.println("🗡️ [FloorKillsCollector] Update für " + floor + ": " + currentTotal + " -> " + newTotal + " kills");
            }
            LeaderboardManager.getInstance().updateScore(floor, newTotal);
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
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.level != null) {
                String dimensionId = client.level.dimension().identifier().toString();
                
                if (dimensionId.startsWith("minecraft:floor_")) {
                    String floorPart = dimensionId.substring("minecraft:floor_".length());
                    String[] parts = floorPart.split("_");
                    if (parts.length >= 1) {
                        String floorNumber = parts[0];
                        // Validiere: Nur floor_1 bis floor_100 sind gültig
                        try {
                            int floorNum = Integer.parseInt(floorNumber);
                            if (floorNum >= 1 && floorNum <= 100) {
                                return "floor_" + floorNumber;
                            }
                        } catch (NumberFormatException e) {
                            // floorNumber ist keine Zahl (z.B. "all", "legendary", "none")
                            // Ignoriere diese ungültigen Floor-Namen
                            System.out.println("⚠️ [FloorKillsCollector] Ungültige Dimension erkannt: " + dimensionId);
                        }
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
            updateFloorScore(floor, kills);
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
     * Gibt die aktuellen totalen Kills zurück (absoluter Wert aus Bossbar)
     */
    public int getCurrentTotalKills() {
        return lastTotalKills;
    }
    
    /**
     * Berechnet die Gesamtkills über alle Floors
     */
    public long getTotalKills() {
        return floorKills.values().stream().mapToLong(Long::longValue).sum();
    }
    
    /**
     * Aktualisiert Alltime Kills (PRIMÄR: berechnete Summe aller Floor-Kills)
     * Führt Doppelcheck mit Menü-Wert durch (falls verfügbar)
     */
    private void updateAlltimeKills() {
        try {
            usePhase();
            if (!phaseBound) {
                return;
            }
            // PRIMÄR: Summe der gespeicherten Ebenenstände dieser Phase
            long calculatedAlltimeKills = getTotalKills();
            
            // Prüfe ob sich der Wert geändert hat
            if (calculatedAlltimeKills != lastSentAlltimeKills && calculatedAlltimeKills > 0) {
                // Doppelcheck: Vergleiche mit Menü-Wert (falls verfügbar)
                if (lastMenuAlltimeKills > 0) {
                    long difference = Math.abs(calculatedAlltimeKills - lastMenuAlltimeKills);
                    double percentageDiff = (difference * 100.0) / Math.max(calculatedAlltimeKills, lastMenuAlltimeKills);
                    
                    if (percentageDiff > 10.0) { // Mehr als 10% Unterschied
                        System.out.println("⚠️ [FloorKillsCollector] WARNUNG: Große Differenz zwischen berechneter Summe und Menü-Wert!");
                        System.out.println("   Berechnet (PRIMÄR): " + calculatedAlltimeKills);
                        System.out.println("   Aus Menü (SEKUNDÄR): " + lastMenuAlltimeKills);
                        System.out.println("   Differenz: " + difference + " (" + String.format("%.2f", percentageDiff) + "%)");
                        System.out.println("   → Berechneter Wert wird verwendet (Summe aller Floor-Kills)");
                    } else {
                        System.out.println("✅ [FloorKillsCollector] Doppelcheck OK: Berechnet=" + calculatedAlltimeKills + ", Menü=" + lastMenuAlltimeKills);
                    }
                }
                
                // Sende berechnete Summe an Server (PRIMÄR)
                lastSentAlltimeKills = calculatedAlltimeKills;
                LeaderboardManager.getInstance().updateScore("alltime_kills", calculatedAlltimeKills);
            }
        } catch (Exception e) {
            System.err.println("❌ Fehler beim Aktualisieren der Alltime Kills: " + e.getMessage());
        }
    }
    
    /**
     * Setzt den Alltime-Kills-Wert aus dem Menü (für Doppelcheck)
     * Wird vom MenuHoverCollector aufgerufen
     */
    public void setMenuAlltimeKills(long kills) {
        if (kills > 0) {
            lastMenuAlltimeKills = kills;
            System.out.println("📋 [FloorKillsCollector] Menü-Alltime-Kills empfangen: " + kills);
        }
    }
    
    /**
     * Gibt den zuletzt berechneten Alltime-Kills-Wert zurück
     */
    public long getLastSentAlltimeKills() {
        return lastSentAlltimeKills;
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
            if (phaseBound) {
                FloorKillStore.shared().remove(boundAccount, boundSeason, boundPreseason, floor);
            }
            LeaderboardManager.getInstance().updateScore(floor, 0);
        }
    }
    
    /**
     * Resettet alle Floor-Kill-Daten
     */
    public void resetAllFloorKills() {
        for (String floor : new ArrayList<>(floorKills.keySet())) {
            LeaderboardManager.getInstance().updateScore(floor, 0);
        }
        floorKills.clear();
        if (phaseBound) {
            FloorKillStore.shared().clear(boundAccount, boundSeason, boundPreseason);
        }
    }

    /** Löscht nur die Stände dieses Kontos und dieser Seasonphase. */
    public static void dropStoredPhase(String account, int seasonId, boolean preseason) {
        FloorKillStore.shared().clear(account, seasonId, preseason);
        FloorKillsCollector current = active;
        if (current != null) {
            current.forgetBound(account, seasonId, preseason);
        }
    }

    private void usePhase() {
        String account = ChatIgnoreList.localAccountId();
        Integer seasonId = SeasonChangeGate.confirmedSeasonId();
        if (account == null || seasonId == null) {
            return;
        }
        boolean preseason = CCLiveUtilitiesConfig.HANDLER.instance().lastProcessedPreseason;
        if (phaseBound && account.equals(boundAccount) && seasonId == boundSeason && preseason == boundPreseason) {
            return;
        }
        boundAccount = account;
        boundSeason = seasonId;
        boundPreseason = preseason;
        phaseBound = true;
        floorKills.clear();
        floorKills.putAll(FloorKillStore.shared().floors(account, seasonId, preseason));
        lastSentAlltimeKills = 0L;
    }

    private void forgetBound(String account, int seasonId, boolean preseason) {
        if (phaseBound && account.equals(boundAccount) && seasonId == boundSeason && preseason == boundPreseason) {
            floorKills.clear();
            phaseBound = false;
            lastSentAlltimeKills = 0L;
        }
    }
    
    @Override
    public void shutdown() {
        // Speichere aktuelle Kills beim Shutdown (absoluter Wert)
        if (lastFloor != null && lastTotalKills > 0) {
            updateFloorScore(lastFloor, lastTotalKills);
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

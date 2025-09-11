package net.felix.utilities;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.felix.leaderboards.LeaderboardManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class MobTimerUtility {
    private static final String MOB_SPAWN_MESSAGE = "Ein seltener Mob ist erschienen";
    private static final String MOB_DESPAWN_MESSAGE = "Der seltene Mob ist verschwunden";
    
    // Timer für aktuelle seltene Mobs (unterstützt mehrere gleichzeitig)
    private static final Map<UUID, MobTimer> activeMobTimers = new HashMap<>();
    private static boolean isEnabled = true;
    
    // Intelligente Lebensdauer-Daten aus der JSON
    private static Map<String, FloorData> floorData = new HashMap<>();
    private static final Gson gson = new Gson();
    
    // Hologramm-Pattern für seltene Mobs (ähnlich wie BossHPUtility)
    private static final Pattern rareMobPattern = Pattern.compile("(.+?)\\|{5}(\\d+)\\|{5}");
    
    // Hologramm-Überwachung für seltene Mobs (pro Timer)
    private static final Map<UUID, HologramData> hologramData = new HashMap<>();
    
    // TEST-FUNKTION: Manueller Floor für das Testen
    private static String testFloor = null;
    
    public static void initialize() {
        // Lade die Lebensdauer-Daten
        loadLifespanData();
        
        // Registriere nur ALLOW_GAME - das fängt alle Nachrichten-Typen ab
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            onChatMessage(message, overlay);
            return true;
        });
        
        // Registriere Tick-Event für Hologramm-Überwachung und Countdown
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && client.player != null) {
                updateMobTimers();
                checkRareMobHologram();
            }
        });
        
        // Registriere Disconnect Event um alle Timer zu stoppen
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            stopAllTimers();
        });
        

    }
    
    private static void loadCooldownData() {
        try {
            // Lade die Cooldown-Daten aus der Ressourcen-Datei
            InputStream inputStream = MobTimerUtility.class.getClassLoader()
                .getResourceAsStream("assets/cclive-utilities/rare_mob_cooldown.json");
            
            if (inputStream != null) {
                try (Reader reader = new InputStreamReader(inputStream)) {
                    JsonObject jsonData = gson.fromJson(reader, JsonObject.class);
                    JsonObject cooldowns = jsonData.getAsJsonObject("rare_mob_cooldowns");
                    
                    if (cooldowns != null) {
                        for (String levelKey : cooldowns.keySet()) {
                            JsonObject levelData = cooldowns.getAsJsonObject(levelKey);
                            // Diese Zeilen sind nicht mehr nötig, da wir FloorData verwenden
                            System.out.println("Geladen: " + levelKey + " = " + levelData.get("current_average_seconds").getAsInt() + "s");
                        }
                    }
                }
                inputStream.close();
            } else {
                System.out.println("Cooldown-Datei nicht gefunden!");
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Laden der Cooldown-Daten: " + e.getMessage());
        }
    }
    
    private static void onChatMessage(Text message, boolean overlay) {
        if (!isEnabled) {
            return;
        }
        
        // Extrahiere den kompletten Text aus der JSON-Struktur
        String messageText = extractFullText(message);
        
        // Debug: Zeige alle empfangenen Nachrichten
        System.out.println("Empfangene Nachricht: '" + messageText + "'");
        
        if (messageText.contains(MOB_SPAWN_MESSAGE)) {
            System.out.println("SPAWN erkannt!");
            // Mob ist erschienen - starte Timer
            handleMobSpawn();
        } else if (messageText.contains(MOB_DESPAWN_MESSAGE)) {
            System.out.println("DESPAWN erkannt!");
            // Mob ist verschwunden - stoppe Timer und zeige Ergebnis
            handleMobDespawn();
        }
    }
    
    private static String extractFullText(Text text) {
        if (text == null) return "";
        
        StringBuilder fullText = new StringBuilder();
        
        // Füge den Haupttext hinzu
        if (text.getContent() != null) {
            fullText.append(text.getContent());
        }
        
        // Füge alle Extra-Texte hinzu
        for (Text sibling : text.getSiblings()) {
            fullText.append(extractFullText(sibling));
        }
        
        return fullText.toString();
    }
    
    private static void handleMobSpawn() {
        String currentFloor = getCurrentFloor();
        if (currentFloor != null) {
            FloorData floorInfo = floorData.get(currentFloor);
            if (floorInfo != null) {
                // Erstelle neuen Timer für diesen Mob
                MobTimer newTimer = new MobTimer(currentFloor, floorInfo.currentAverageSeconds);
                activeMobTimers.put(newTimer.mobId, newTimer);
                
                // Initialisiere Hologramm-Daten für diesen Timer
                hologramData.put(newTimer.mobId, new HologramData());
                
                System.out.println("⏱️ Mob-Timer #" + newTimer.mobId.toString().substring(0, 8) + " gestartet für " + currentFloor + "!");
                System.out.println("⏱️ Erwartete Lebensdauer: " + formatDuration(Duration.ofSeconds(floorInfo.currentAverageSeconds)));
                System.out.println("⏱️ Mob verschwindet in: " + formatDuration(Duration.ofSeconds(floorInfo.currentAverageSeconds)));
                System.out.println("📊 Aktive Timer: " + activeMobTimers.size());
            } else {
                System.out.println("? Unbekannter Floor: " + currentFloor + " - Timer nicht gestartet");
            }
        } else {
            System.out.println("? Kein Floor erkannt - Timer nicht gestartet");
        }
    }
    
    private static void handleMobDespawn() {
        // Diese Methode wird nicht mehr benötigt, da wir Hologramm-Überwachung verwenden
        // Alle Timer werden über checkRareMobHologram() verwaltet
        System.out.println("ℹ️ Chat-Despawn erkannt - Timer wird über Hologramm-Überwachung verwaltet");
    }
    
    private static String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long millis = duration.toMillis() % 1000;
        
        if (minutes > 0) {
            return String.format("%d:%02d.%03d", minutes, seconds, millis);
        } else {
            return String.format("%d.%03ds", seconds, millis);
        }
    }
    
    private static void analyzeCooldown(Duration duration, String floor, UUID timerId) {
        long totalSeconds = duration.getSeconds();
        
        if (floor != null) {
            // Vergleiche mit dem Cooldown der aktuellen Floor
            FloorData floorInfo = floorData.get(floor);
            if (floorInfo != null) {
                int expectedCooldown = floorInfo.currentAverageSeconds;
                int difference = Math.abs((int) totalSeconds - expectedCooldown);
                
                if (difference <= 5) {
                    System.out.println("✓ Floor " + floor.replace("floor_", "") + " bestätigt! (Erwartet: " + expectedCooldown + "s, Gemessen: " + totalSeconds + "s, Differenz: " + difference + "s)");
                } else if (difference <= 15) {
                    System.out.println("? Floor " + floor.replace("floor_", "") + " ähnlich (Erwartet: " + expectedCooldown + "s, Gemessen: " + totalSeconds + "s, Differenz: " + difference + "s)");
                } else {
                    System.out.println("⚠ Floor " + floor.replace("floor_", "") + " Abweichung (Erwartet: " + expectedCooldown + "s, Gemessen: " + totalSeconds + "s, Differenz: " + difference + "s)");
                }
                
                // Aktualisiere die Floor-Daten mit der neuen Messung
                floorInfo.updateWithMeasurement((int) totalSeconds);
                System.out.println("📊 Neue Durchschnittszeit für " + floor + ": " + floorInfo.currentAverageSeconds + "s (basierend auf " + floorInfo.measurementsCount + " Messungen)");
            } else {
                System.out.println("? Unbekannter Floor: " + floor + " - Gemessene Zeit: " + totalSeconds + "s");
            }
        } else {
            System.out.println("? Kein Floor erkannt - Gemessene Zeit: " + totalSeconds + "s");
        }
        
        // Zeige auch alle anderen möglichen Ebenen an
        System.out.println("--- Alle möglichen Ebenen ---");
        for (Map.Entry<String, FloorData> entry : floorData.entrySet()) {
            String level = entry.getKey();
            FloorData levelInfo = entry.getValue();
            int expectedCooldown = levelInfo.currentAverageSeconds;
            int difference = Math.abs((int) totalSeconds - expectedCooldown);
            
            if (difference <= 5) {
                System.out.println("✓ " + level + " (Erwartet: " + expectedCooldown + "s, Gemessen: " + totalSeconds + "s, Differenz: " + difference + "s)");
            } else if (difference <= 15) {
                System.out.println("? " + level + " (Erwartet: " + expectedCooldown + "s, Gemessen: " + totalSeconds + "s, Differenz: " + difference + "s)");
            }
        }
    }
    
    private static String getCurrentFloor() {
        // TEST-MODUS: Wenn ein Test-Floor gesetzt ist, verwende diesen
        if (testFloor != null) {
            return testFloor;
        }
        
        // Normale automatische Floor-Erkennung
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
            System.out.println("Fehler bei der Floor-Erkennung: " + e.getMessage());
        }
        return null;
    }
    
    public static void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
    
    public static boolean isEnabled() {
        return isEnabled;
    }
    
    public static int getActiveTimers() {
        return activeMobTimers.size();
    }
    
    public static void clearAllTimers() {
        activeMobTimers.clear();
        hologramData.clear();
    }
    
    /**
     * Stoppt alle aktiven Timer und leert die Daten
     * Wird aufgerufen wenn Spieler die Welt verlässt
     */
    public static void stopAllTimers() {
        if (!activeMobTimers.isEmpty()) {
            System.out.println("Alle " + activeMobTimers.size() + " aktiven Timer werden gestoppt (Welt verlassen)");
            // KEINE Messung bei Welt verlassen - nur Timer stoppen
            activeMobTimers.clear();
            hologramData.clear();
        }
    }
    
    // ===== TEST-FUNKTIONEN =====
    
    /**
     * Setzt den Floor manuell für das Testen
     * @param floor Der Floor (z.B. "floor_1", "floor_3", etc.)
     */
    public static void setTestFloor(String floor) {
        testFloor = floor;
        System.out.println("🧪 TEST-MODUS: Floor manuell auf " + floor + " gesetzt!");
        System.out.println("🧪 Verfügbare Floors: " + String.join(", ", floorData.keySet()));
    }
    
    /**
     * Entfernt den manuellen Test-Floor
     */
    public static void clearTestFloor() {
        testFloor = null;
        System.out.println("🧪 TEST-MODUS: Manueller Floor entfernt - automatische Erkennung aktiviert");
    }
    
    /**
     * Zeigt den aktuellen Test-Floor an
     */
    public static void showTestFloor() {
        if (testFloor != null) {
            System.out.println("🧪 AKTUELLER TEST-FLOOR: " + testFloor);
        } else {
            System.out.println("🧪 KEIN TEST-FLOOR: Automatische Erkennung aktiv");
        }
    }
    
    /**
     * Zeigt alle verfügbaren Floors an
     */
    public static void listAvailableFloors() {
        System.out.println("🧪 VERFÜGBARE FLOORS:");
        for (Map.Entry<String, FloorData> entry : floorData.entrySet()) {
            String floorKey = entry.getKey();
            FloorData data = entry.getValue();
            System.out.println("  " + floorKey + " (" + data.name + "): " + data.currentAverageSeconds + "s");
        }
    }
    
    // Innere Klasse für Mob-Timer
    private static class MobTimer {
        private final UUID mobId;
        private final Instant spawnTime;
        private final String floor;
        private final int expectedLifespan;
        private boolean isActive;
        private boolean wasKilled;
        
        public MobTimer(String floor, int expectedLifespan) {
            this.mobId = UUID.randomUUID();
            this.spawnTime = Instant.now();
            this.floor = floor;
            this.expectedLifespan = expectedLifespan;
            this.isActive = true;
            this.wasKilled = false;
        }
        
        public Duration getElapsedTime() {
            return Duration.between(spawnTime, Instant.now());
        }
        
        public Duration getRemainingTime() {
            return Duration.ofSeconds(expectedLifespan).minus(getElapsedTime());
        }
        
        public boolean isExpired() {
            return getElapsedTime().getSeconds() >= expectedLifespan;
        }
        
        public void markAsKilled() {
            this.wasKilled = true;
            this.isActive = false;
        }
        
        public void deactivate() {
            this.isActive = false;
        }

        public boolean isActive() {
            return isActive;
        }

        public Instant getSpawnTime() {
            return spawnTime;
        }

        public String getFloor() {
            return floor;
        }
    }
    
    // Innere Klasse für Floor-Daten
    private static class FloorData {
        public final String name;
        public final int initialLifespanSeconds;
        public int currentAverageSeconds;
        public int measurementsCount;
        public int minLifespanSeconds;
        public int maxLifespanSeconds;
        
        public FloorData(String name, int initialLifespanSeconds, int currentAverageSeconds, 
                        int measurementsCount, int minLifespanSeconds, int maxLifespanSeconds) {
            this.name = name;
            this.initialLifespanSeconds = initialLifespanSeconds;
            this.currentAverageSeconds = currentAverageSeconds;
            this.measurementsCount = measurementsCount;
            this.minLifespanSeconds = minLifespanSeconds;
            this.maxLifespanSeconds = maxLifespanSeconds;
        }
        
        // Aktualisiert die Durchschnittswerte basierend auf neuen Messungen
        public void updateWithMeasurement(int newLifespanSeconds) {
            measurementsCount++;
            
            // Aktualisiere Min/Max
            if (newLifespanSeconds < minLifespanSeconds) {
                minLifespanSeconds = newLifespanSeconds;
            }
            if (newLifespanSeconds > maxLifespanSeconds) {
                maxLifespanSeconds = newLifespanSeconds;
            }
            
            // Berechne neuen Durchschnitt
            int totalSeconds = (currentAverageSeconds * (measurementsCount - 1)) + newLifespanSeconds;
            currentAverageSeconds = totalSeconds / measurementsCount;
        }
    }
    
    // Innere Klasse für Hologramm-Daten pro Timer
    private static class HologramData {
        public boolean isVisible = false;
        public String mobName = null;
        public int lastHP = 0;
        
        public void reset() {
            isVisible = false;
            mobName = null;
            lastHP = 0;
        }
    }
    
    private static void loadLifespanData() {
        try {
            // Lade die Lebensdauer-Daten aus der Ressourcen-Datei
            InputStream inputStream = MobTimerUtility.class.getClassLoader()
                .getResourceAsStream("assets/cclive-utilities/rare_mob_cooldown.json");
            
            if (inputStream != null) {
                try (Reader reader = new InputStreamReader(inputStream)) {
                    JsonObject jsonData = gson.fromJson(reader, JsonObject.class);
                    JsonObject cooldowns = jsonData.getAsJsonObject("rare_mob_cooldowns");
                    
                    if (cooldowns != null) {
                        for (String levelKey : cooldowns.keySet()) {
                            JsonObject levelData = cooldowns.getAsJsonObject(levelKey);
                            
                            FloorData data = new FloorData(
                                levelData.get("name").getAsString(),
                                levelData.get("initial_lifespan_seconds").getAsInt(),
                                levelData.get("current_average_seconds").getAsInt(),
                                levelData.get("measurements_count").getAsInt(),
                                levelData.get("min_lifespan_seconds").getAsInt(),
                                levelData.get("max_lifespan_seconds").getAsInt()
                            );
                            
                            floorData.put(levelKey, data);
                            System.out.println("Geladen: " + levelKey + " = " + data.currentAverageSeconds + "s (Durchschnitt)");
                        }
                    }
                }
                inputStream.close();
            } else {
                System.out.println("Lebensdauer-Datei nicht gefunden!");
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Laden der Lebensdauer-Daten: " + e.getMessage());
        }
    }
    
    private static void updateMobTimers() {
        // Überwache alle aktiven Timer
        for (Map.Entry<UUID, MobTimer> entry : activeMobTimers.entrySet()) {
            UUID timerId = entry.getKey();
            MobTimer timer = entry.getValue();
            
            if (timer.isActive) {
                Duration remaining = timer.getRemainingTime();
                
                if (remaining.isNegative()) {
                    // Timer ist abgelaufen
                    System.out.println("⏱️ Mob-Timer #" + timerId.toString().substring(0, 8) + " abgelaufen! Mob sollte verschwunden sein.");
                    timer.deactivate();
                } else {
                    // Zeige Countdown alle 10 Sekunden (häufiger Updates)
                    long seconds = remaining.getSeconds();
                    if (seconds % 10 == 0 && seconds > 0) {
                        System.out.println("⏱️ Timer #" + timerId.toString().substring(0, 8) + " - Mob verschwindet in: " + formatDuration(remaining));
                    }
                }
            }
        }
        
        // Entferne inaktive Timer
        activeMobTimers.entrySet().removeIf(entry -> !entry.getValue().isActive);
    }
    
    private static void checkRareMobHologram() {
        if (activeMobTimers.isEmpty()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        
        // Iteriere durch alle aktiven Timer
        for (Map.Entry<UUID, MobTimer> entry : activeMobTimers.entrySet()) {
            UUID timerId = entry.getKey();
            MobTimer timer = entry.getValue();
            HologramData hologramData = MobTimerUtility.hologramData.get(timerId);
            
            if (hologramData == null) continue;
            
            // Suche nach Hologramm-Text in der Nähe des Spielers
            boolean hologramFound = false;
            String currentMobName = null;
            int currentHP = 0;
            
            // Überprüfe alle Entities in der Nähe
            for (Entity entity : client.world.getEntities()) {
                if (entity.hasCustomName()) {
                    String customName = entity.getCustomName().getString();
                    
                    // Prüfe ob es ein "Seltener Mob" Hologramm ist
                    if (customName.contains("Seltener Mob")) {
                        hologramFound = true;
                        currentMobName = customName;
                        
                        // Extrahiere HP aus dem Hologramm (falls verfügbar)
                        if (entity instanceof LivingEntity) {
                            LivingEntity livingEntity = (LivingEntity) entity;
                            currentHP = (int) livingEntity.getHealth();
                        }
                        break;
                    }
                }
            }
            
            // Aktualisiere Hologramm-Daten
            if (hologramFound) {
                if (!hologramData.isVisible) {
                    // Hologramm wurde gerade sichtbar
                    hologramData.isVisible = true;
                    hologramData.mobName = currentMobName;
                    hologramData.lastHP = currentHP;
                    System.out.println("Hologramm für Timer #" + timerId + " sichtbar: " + currentMobName);
                } else {
                    // Hologramm war bereits sichtbar - prüfe HP-Änderungen
                    if (currentHP < hologramData.lastHP) {
                        System.out.println("HP-Änderung für Timer #" + timerId + ": " + hologramData.lastHP + " → " + currentHP);
                        hologramData.lastHP = currentHP;
                        
                        // Wenn HP auf 0 oder darunter fällt, Mob wurde getötet
                        if (currentHP <= 0) {
                            System.out.println("Mob für Timer #" + timerId + " wurde getötet!");
                            timer.markAsKilled();
                            timer.deactivate();
                            // KEINE Messung bei Kill - nur Timer stoppen
                            
                            // Benachrichtige Leaderboard-System über Rare Mob Kill
                            try {
                                LeaderboardManager.getInstance().updateScore("alltime_rare_mob_kills", 
                                    LeaderboardManager.getInstance().isRegistered() ? 1 : 0);
                            } catch (Exception e) {
                                System.err.println("❌ Fehler beim Senden des Rare Mob Kill an Leaderboard: " + e.getMessage());
                            }
                        }
                    }
                }
            } else {
                // Hologramm nicht mehr sichtbar - Mob ist verschwunden
                if (hologramData.isVisible) {
                    System.out.println("Hologramm für Timer #" + timerId + " verschwunden - Mob despawnt!");
                    hologramData.isVisible = false;
                    timer.deactivate();
                    // ✅ MESSUNG bei Despawn - das ist die echte Lebensdauer!
                    analyzeCooldown(Duration.between(timer.getSpawnTime(), Instant.now()), timer.getFloor(), timerId);
                }
            }
        }
        
        // Entferne inaktive Timer
        activeMobTimers.entrySet().removeIf(entry -> !entry.getValue().isActive());
        hologramData.entrySet().removeIf(entry -> !activeMobTimers.containsKey(entry.getKey()));
    }
}

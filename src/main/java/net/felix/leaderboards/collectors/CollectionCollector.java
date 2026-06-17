package net.felix.leaderboards.collectors;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.utilities.Other.DebugUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Sammelt Collection-Daten durch Tooltip-Parsing in Collection-Inventaren
 * Erkennt Collection-Werte, sendet sie an Server und zeigt eigenen Rang in Tooltips
 */
public class CollectionCollector implements DataCollector {
    private boolean isActive = false;
    
    // Pattern für Collection-Tooltips basierend auf Placeholder-System (Hauptinventare)
    // Beispiel: "12,345/25,000" aus "%objective_score_{Collections}_{Mangrove}%,%objective_score_{dezimal}_{Dezimal}%/25,000"
    private static final Pattern MAIN_COLLECTION_VALUE_PATTERN = Pattern.compile("([0-9,]+)/([0-9,]+)");
    
    // Pattern für Progress-Item in Unterinventaren
    // Beispiel: "10,130/50" aus "&f 10,130&a/&f50"
    private static final Pattern SUB_COLLECTION_VALUE_PATTERN = Pattern.compile("([0-9,]+)&a/&f([0-9,]+)");
    
    // Pattern für Collection-Namen aus Item-Namen (ohne Level-Suffix)
    // Beispiel: "Mangroven Holz VII" -> "Mangroven Holz" oder "Mangroven Holz I" -> "Mangroven Holz"
    private static final Pattern ITEM_NAME_PATTERN = Pattern.compile("(.+?)\\s+[IVX]+$");
    
    // Collection-Inventar Titel
    private static final String WOOD_COLLECTION_TITLE = "[Holzfäller Sammlung]";
    private static final String ORE_COLLECTION_TITLE = "[Bergbau Sammlung]";
    
    // Mapping von Material-Namen zu Leaderboard-Namen
    private static final Map<String, String> MATERIAL_MAPPING = new HashMap<>();
    
    static {
        // Holz-Arten (basierend auf den Unterinventar-Titeln)
        MATERIAL_MAPPING.put("mangroven holz", "mangrove_collection");
        MATERIAL_MAPPING.put("bambus", "bamboo_collection");
        MATERIAL_MAPPING.put("eichenholz", "oak_collection");
        MATERIAL_MAPPING.put("karmesinholz", "crimson_collection");
        MATERIAL_MAPPING.put("fichtenholz", "spruce_collection");
        MATERIAL_MAPPING.put("dschungelholz", "jungle_collection");
        MATERIAL_MAPPING.put("wirrwarr holz", "warped_collection");
        MATERIAL_MAPPING.put("dunkles eichenholz", "dark_oak_collection");
        MATERIAL_MAPPING.put("pilzholz", "mushroom_collection");
        
        // Erze (basierend auf den Unterinventar-Titeln)
        MATERIAL_MAPPING.put("rohes kupfer", "raw_copper_collection");
        MATERIAL_MAPPING.put("obsidian", "obsidian_collection");
        MATERIAL_MAPPING.put("diamant", "diamond_collection");
        MATERIAL_MAPPING.put("kohle", "coal_collection");
        MATERIAL_MAPPING.put("sulfur", "sulfur_collection");
        MATERIAL_MAPPING.put("echo kristall", "echo_collection");
        MATERIAL_MAPPING.put("rohes eisen", "raw_iron_collection");
        MATERIAL_MAPPING.put("rohes gold", "raw_gold_collection");
        MATERIAL_MAPPING.put("antiker schutt", "ancient_debris_collection");
        MATERIAL_MAPPING.put("quartz", "quartz_collection");
    }
    
    // Cache für Collection-Werte
    private final Map<String, Long> collectionValues = new HashMap<>();
    
    // Cache für Player-Rankings (für Tooltip-Enhancement)
    private final Map<String, Integer> playerRankings = new HashMap<>();
    
    // Cache für letzte Rank-Abfrage (verhindert zu häufige Requests)
    private final Map<String, Long> lastRankFetch = new HashMap<>();
    
    // Zeitbasierte Cooldown-Konfiguration
    private static final int TESTING_INTERVAL_MINUTES = 2;  // Für Testing: alle 2 Minuten
    private static final int PRODUCTION_INTERVAL_MINUTES = 10; // Für Production: alle 10 Minuten
    private static final boolean USE_TESTING_INTERVAL = true; // TODO: Später auf false setzen
    
    // Deutsche Zeitzone
    private static final ZoneId GERMAN_TIMEZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Shift-Taste Status für Leaderboard-Overlay
    private boolean isShiftPressed = false;
    
    @Override
    public void initialize() {
        if (isActive) return;
        
        // Registriere Tooltip-Event für Collection-Parsing und Enhancement
        ItemTooltipCallback.EVENT.register((stack, context, tooltipType, lines) -> {
            if (!isActive) return;
            
            // Nur in Collection-Inventar verarbeiten
            if (!isInCollectionInventory()) return;
            
            // Shift-Status aktualisieren
            updateShiftStatus();
            
            // Collection-Daten aus Tooltip extrahieren
            parseCollectionTooltip(lines);
            
            // Tooltip mit Leaderboard-Informationen erweitern
            enhanceCollectionTooltip(lines, stack);
        });
        
        // Registriere Screen-Events für automatisches Scanning in Unterinventaren
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!isActive) return;
            
            // Prüfe ob es ein Unterinventar ist
            if (screen instanceof HandledScreen) {
                String screenTitle = screen.getTitle().getString();
                if (screenTitle.endsWith(" Collection")) {
                    // Warte kurz bis Inventar geladen ist, dann scanne automatisch
                    new Thread(() -> {
                        try {
                            Thread.sleep(500); // 500ms warten
                            scanSubInventoryAutomatically();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            }
        });
        
        isActive = true;
        // Silent error handling("✅ CollectionCollector initialisiert (Tooltip-basiert)");
    }
    
    /**
     * Prüft ob wir uns in einem Collection-Inventar befinden
     */
    private boolean isInCollectionInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) return false;
        
        String screenTitle = client.currentScreen.getTitle().getString();
        
        // Prüfe auf Haupt-Collection-Inventare
        if (screenTitle.equals(WOOD_COLLECTION_TITLE) || screenTitle.equals(ORE_COLLECTION_TITLE)) {
            return true;
        }
        
        // Prüfe auf Unterinventare (enden mit " Collection")
        return screenTitle.endsWith(" Collection");
    }
    
    /**
     * Prüft ob wir uns in einem Haupt-Collection-Inventar befinden
     */
    private boolean isInMainCollectionInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) return false;
        
        String screenTitle = client.currentScreen.getTitle().getString();
        return screenTitle.equals(WOOD_COLLECTION_TITLE) || screenTitle.equals(ORE_COLLECTION_TITLE);
    }
    
    /**
     * Prüft ob wir uns in einem Unter-Collection-Inventar befinden
     */
    private boolean isInSubCollectionInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) return false;
        
        String screenTitle = client.currentScreen.getTitle().getString();
        return screenTitle.endsWith(" Collection");
    }
    
    /**
     * Aktualisiert den Shift-Status für Leaderboard-Overlay
     */
    private void updateShiftStatus() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            isShiftPressed = false;
            return;
        }
        
        // Prüfe beide Shift-Tasten (Links und Rechts)
        boolean leftShift = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT);
        boolean rightShift = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_RIGHT_SHIFT);
        
        isShiftPressed = leftShift || rightShift;
        
        if (DebugUtility.isLeaderboardDebuggingEnabled() && isShiftPressed) {
            // Silent error handling("🔧 Shift gedrückt - Leaderboard-Overlay aktiviert");
        }
    }
    
    /**
     * Parsed Collection-Daten aus einem Tooltip
     */
    private void parseCollectionTooltip(List<Text> lines) {
        if (lines.isEmpty()) return;
        
        // Unterschiedliche Logik für Haupt- vs Unterinventare
        if (isInMainCollectionInventory()) {
            parseMainCollectionTooltip(lines);
        } else if (isInSubCollectionInventory()) {
            parseSubCollectionTooltip(lines);
        }
    }
    
    /**
     * Parsed Collection-Daten aus Hauptinventar-Tooltips (mit Placeholder-System)
     */
    private void parseMainCollectionTooltip(List<Text> lines) {
        String collectionName = extractCollectionNameFromTooltip(lines);
        if (collectionName == null) return;
        
        // Suche nach dem Placeholder-Wert in der Lore
        for (Text line : lines) {
            String lineText = line.getString();
            
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                // Silent error handling("🔍 Main Collection Tooltip Line: " + lineText);
            }
            
            // Prüfe auf "12,345/25,000" Format (aus Placeholder)
            Matcher valueMatcher = MAIN_COLLECTION_VALUE_PATTERN.matcher(lineText);
            if (valueMatcher.find()) {
                String valueStr = valueMatcher.group(1).replace(",", "");
                
                try {
                    long collectionValue = Long.parseLong(valueStr);
                    handleCollectionData(collectionName, collectionValue);
                    
                    if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                        // Silent error handling("📦 Main Collection-Wert gefunden: " + collectionName + " = " + collectionValue);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("❌ Fehler beim Parsen der Main Collection-Zahl: " + valueStr);
                }
                break;
            }
        }
    }
    
    /**
     * Parsed Collection-Daten aus Unterinventar-Tooltips (Progress-Item)
     */
    private void parseSubCollectionTooltip(List<Text> lines) {
        // Bestimme Collection-Name aus Screen-Titel
        String collectionName = extractCollectionNameFromScreen();
        if (collectionName == null) return;
        
        // Prüfe ob es sich um das Progress-Item handelt (Item-Name sollte "XYZ I" sein)
        if (lines.isEmpty()) return;
        String itemName = lines.get(0).getString();
        
        // Prüfe ob Item-Name mit Level I endet (Progress-Item)
        if (!itemName.endsWith(" I")) return;
        
        // Suche nach Progress-Wert in der Lore
        for (Text line : lines) {
            String lineText = line.getString();
            
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                // Silent error handling("🔍 Sub Collection Tooltip Line: " + lineText);
            }
            
            // Prüfe auf "10,130&a/&f50" Format
            Matcher valueMatcher = SUB_COLLECTION_VALUE_PATTERN.matcher(lineText);
            if (valueMatcher.find()) {
                String valueStr = valueMatcher.group(1).replace(",", "");
                
                try {
                    long collectionValue = Long.parseLong(valueStr);
                    handleCollectionData(collectionName, collectionValue);
                    
                    if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                        // Silent error handling("📦 Sub Collection-Wert gefunden: " + collectionName + " = " + collectionValue);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("❌ Fehler beim Parsen der Sub Collection-Zahl: " + valueStr);
                }
                break;
            }
        }
    }
    
    /**
     * Extrahiert Collection-Name aus dem aktuellen Screen-Titel
     */
    private String extractCollectionNameFromScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) return null;
        
        String screenTitle = client.currentScreen.getTitle().getString();
        
        // Für Unterinventare: "Mangroven Holz Collection" -> "mangroven holz"
        if (screenTitle.endsWith(" Collection")) {
            return screenTitle.substring(0, screenTitle.length() - " Collection".length()).toLowerCase();
        }
        
        return null;
    }
    
    /**
     * Verarbeitet erkannte Collection-Daten
     */
    private void handleCollectionData(String materialName, long value) {
        String leaderboardName = findLeaderboardName(materialName);
        if (leaderboardName == null) {
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                // Silent error handling("⚠️ Unbekannte Collection: " + materialName);
            }
            return;
        }
        
        // Prüfe ob sich der Wert geändert hat
        long currentValue = collectionValues.getOrDefault(leaderboardName, 0L);
        if (currentValue != value) {
            collectionValues.put(leaderboardName, value);
            
            // Sende an Server
            LeaderboardManager.getInstance().updateScore(leaderboardName, value);
            
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                // Silent error handling("📦 Collection Update: " + materialName + " = " + value + " (war: " + currentValue + ")");
            }
        }
        
        // Hole Player-Rang nach Cooldown
        fetchPlayerRankIfNeeded(leaderboardName);
    }
    
    /**
     * Holt Player-Rang vom Server basierend auf zeitbasiertem Cooldown
     */
    private void fetchPlayerRankIfNeeded(String leaderboardName) {
        if (!canFetchLeaderboardNow()) {
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                LocalDateTime nextFetch = getNextFetchTime();
                String nextFetchStr = nextFetch.format(TIME_FORMATTER);
                // Silent error handling("⏳ Leaderboard-Fetch für " + leaderboardName + " - nächster Fetch: " + nextFetchStr);
            }
            return;
        }
        
        // Markiere als abgerufen
        lastRankFetch.put(leaderboardName, System.currentTimeMillis());
        
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            LocalDateTime now = LocalDateTime.now(GERMAN_TIMEZONE);
            // Silent error handling("🕐 Leaderboard-Fetch gestartet um " + now.format(TIME_FORMATTER) + " für: " + leaderboardName);
        }
        
        // Asynchron Rang abrufen
        LeaderboardManager.getInstance().getLeaderboard(leaderboardName)
            .thenAccept(response -> {
                if (response != null && response.has("playerRank")) {
                    int rank = response.get("playerRank").getAsInt();
                    setPlayerRank(leaderboardName, rank);
                    
                    if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                        // Silent error handling("📊 Player-Rang aktualisiert: " + leaderboardName + " = #" + rank);
                    }
                }
            });
    }
    
    /**
     * Prüft ob ein Leaderboard-Fetch jetzt erlaubt ist (zeitbasiert)
     */
    private boolean canFetchLeaderboardNow() {
        LocalDateTime now = LocalDateTime.now(GERMAN_TIMEZONE);
        int intervalMinutes = USE_TESTING_INTERVAL ? TESTING_INTERVAL_MINUTES : PRODUCTION_INTERVAL_MINUTES;
        
        // Prüfe ob wir auf einem gültigen Zeitpunkt sind (6:26, 6:28, 6:30 etc.)
        int currentMinute = now.getMinute();
        
        // Berechne ob aktuelle Minute ein gültiges Intervall ist
        return (currentMinute % intervalMinutes) == 0;
    }
    
    /**
     * Berechnet die nächste erlaubte Fetch-Zeit
     */
    private LocalDateTime getNextFetchTime() {
        LocalDateTime now = LocalDateTime.now(GERMAN_TIMEZONE);
        int intervalMinutes = USE_TESTING_INTERVAL ? TESTING_INTERVAL_MINUTES : PRODUCTION_INTERVAL_MINUTES;
        
        int currentMinute = now.getMinute();
        int minutesUntilNext = intervalMinutes - (currentMinute % intervalMinutes);
        
        return now.plusMinutes(minutesUntilNext).withSecond(0).withNano(0);
    }
    
    /**
     * Berechnet die verbleibende Zeit bis zum nächsten Fetch in Sekunden
     */
    private long getSecondsUntilNextFetch() {
        LocalDateTime now = LocalDateTime.now(GERMAN_TIMEZONE);
        LocalDateTime nextFetch = getNextFetchTime();
        
        // Wenn wir genau auf einem Fetch-Zeitpunkt sind, nächsten Intervall nehmen
        if (canFetchLeaderboardNow()) {
            int intervalMinutes = USE_TESTING_INTERVAL ? TESTING_INTERVAL_MINUTES : PRODUCTION_INTERVAL_MINUTES;
            nextFetch = nextFetch.plusMinutes(intervalMinutes);
        }
        
        return java.time.Duration.between(now, nextFetch).getSeconds();
    }
    
    /**
     * Formatiert Sekunden zu mm:ss Format
     */
    private String formatCountdown(long seconds) {
        if (seconds <= 0) {
            // Wenn gerade Fetch-Zeit ist, zeige nächsten Countdown
            int intervalMinutes = USE_TESTING_INTERVAL ? TESTING_INTERVAL_MINUTES : PRODUCTION_INTERVAL_MINUTES;
            long nextCountdownSeconds = intervalMinutes * 60;
            long nextMinutes = nextCountdownSeconds / 60;
            return String.format("%d:00", nextMinutes);
        }
        
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        
        return String.format("%d:%02d", minutes, remainingSeconds);
    }
    
    /**
     * Findet den passenden Leaderboard-Namen für ein Material
     */
    private String findLeaderboardName(String materialName) {
        // Direkte Suche
        String directMatch = MATERIAL_MAPPING.get(materialName);
        if (directMatch != null) {
            return directMatch;
        }
        
        // Fuzzy-Suche für ähnliche Namen
        for (Map.Entry<String, String> entry : MATERIAL_MAPPING.entrySet()) {
            if (materialName.contains(entry.getKey()) || entry.getKey().contains(materialName)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Erweitert Collection-Tooltips mit Leaderboard-Informationen
     */
    private void enhanceCollectionTooltip(List<Text> lines, net.minecraft.item.ItemStack stack) {
        if (lines.isEmpty()) return;
        
        // Nur in Hauptinventaren Rang zu Tooltip hinzufügen
        if (isInMainCollectionInventory()) {
            enhanceMainCollectionTooltip(lines, stack);
        } else if (isInSubCollectionInventory()) {
            enhanceSubCollectionTooltip(lines, stack);
        }
    }
    
    /**
     * Erweitert Hauptinventar-Tooltips mit Rang-Informationen
     */
    private void enhanceMainCollectionTooltip(List<Text> lines, net.minecraft.item.ItemStack stack) {
        String collectionName = extractCollectionNameFromTooltip(lines);
        if (collectionName == null) return;
        
        String leaderboardName = findLeaderboardName(collectionName);
        if (leaderboardName == null) return;
        
        // Füge eigenen Rang zur Lore hinzu
        addOwnRankToTooltip(lines, leaderboardName);
        
        // Zeige Leaderboard-Overlay wenn Shift gedrückt
        if (isShiftPressed) {
            prepareLeaderboardOverlay(leaderboardName);
        }
    }
    
    /**
     * Behandelt Unterinventar-Tooltips (Links-Rendering ohne Shift)
     */
    private void enhanceSubCollectionTooltip(List<Text> lines, net.minecraft.item.ItemStack stack) {
        // Nur bei Progress-Items (enden mit " I")
        if (lines.isEmpty()) return;
        String itemName = lines.get(0).getString();
        if (!itemName.endsWith(" I")) return;
        
        String collectionName = extractCollectionNameFromScreen();
        if (collectionName == null) return;
        
        String leaderboardName = findLeaderboardName(collectionName);
        if (leaderboardName == null) return;
        
        // Standardmäßiges Links-Leaderboard-Rendering (ohne Shift erforderlich)
        prepareSubInventoryLeaderboardRendering(leaderboardName);
        
        // Zusätzlich: Shift für erweiterte Optionen
        if (isShiftPressed) {
            prepareLeaderboardOverlay(leaderboardName);
            
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                // Silent error handling("🏆 Sub-Inventar Shift-Overlay vorbereitet für: " + leaderboardName);
            }
        }
    }
    
    /**
     * Bereitet standardmäßiges Links-Leaderboard für Unterinventare vor
     */
    private void prepareSubInventoryLeaderboardRendering(String leaderboardName) {
        // TODO: Implementiere Links-Leaderboard-Rendering
        // Position: Links neben dem Inventar
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            // Silent error handling("📊 Links-Leaderboard vorbereitet für: " + leaderboardName);
        }
        
        // Hier würde das Links-Rendering implementiert werden:
        // - Position berechnen (links neben Inventar)
        // - Top10 Leaderboard abrufen
        // - Rendering-Pipeline aufrufen
        
        // Für jetzt: Hole aktuelles Leaderboard
        LeaderboardManager.getInstance().getLeaderboard(leaderboardName)
            .thenAccept(response -> {
                if (response != null) {
                    if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                        // Silent error handling("📊 Leaderboard-Daten für Links-Rendering erhalten: " + leaderboardName);
                        // // Silent error handling("🔍 Response: " + response.toString());
                    }
                    // TODO: Hier würde das Rendering aufgerufen werden
                }
            });
    }
    
    /**
     * Extrahiert Collection-Name aus Tooltip-Zeilen (Alternative Methode)
     */
    private String extractCollectionNameFromTooltip(List<Text> lines) {
        // Verwende Screen-Titel als primäre Quelle
        String screenName = extractCollectionNameFromScreen();
        if (screenName != null) {
            return screenName;
        }
        
        // Fallback: Suche in Item-Namen (erste Zeile)
        if (!lines.isEmpty()) {
            String itemName = lines.get(0).getString();
            
            // Entferne Level-Suffix (z.B. "Mangroven Holz VII" -> "Mangroven Holz")
            Matcher matcher = ITEM_NAME_PATTERN.matcher(itemName);
            if (matcher.find()) {
                return matcher.group(1).toLowerCase().trim();
            }
        }
        
        return null;
    }
    
    /**
     * Fügt eigenen Rang zur Tooltip hinzu
     */
    private void addOwnRankToTooltip(List<Text> lines, String leaderboardName) {
        // Finde Position über "Click for details" (sollte am Ende sein)
        int insertIndex = lines.size();
        
        // Suche nach "Click for details" und füge davor ein
        for (int i = lines.size() - 1; i >= 0; i--) {
            String lineText = lines.get(i).getString();
            if (lineText.contains("Click for details")) {
                insertIndex = i;
                break;
            }
        }
        
        // Hole Rang und Score für dieses Leaderboard
        int playerRank = getPlayerRank(leaderboardName);
        long playerScore = getCollectionValue(leaderboardName);
        
        // Berechne Countdown bis zum nächsten Update
        long secondsRemaining = getSecondsUntilNextFetch();
        String countdownText = formatCountdown(secondsRemaining);
        
        if (playerRank > 0) {
            // Füge Leaderboard-Sektion hinzu
            lines.add(insertIndex++, Text.literal(""));
            lines.add(insertIndex++, Text.literal("===Collection Leaderboard===").formatted(Formatting.YELLOW));
            lines.add(insertIndex++, Text.literal("Dein Rang: " + playerRank + ". " + formatNumber(playerScore)).formatted(Formatting.GREEN));
            
            // Countdown-Text (wird nur bei erneutem Hover aktualisiert)
            lines.add(insertIndex++, Text.literal("Update in: " + countdownText).formatted(Formatting.GRAY));
            
            lines.add(insertIndex++, Text.literal("Shift für Top10 Leaderboard").formatted(Formatting.AQUA));
            lines.add(insertIndex++, Text.literal("=====================").formatted(Formatting.YELLOW));
            
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                // Silent error handling("📊 Rang zu Tooltip hinzugefügt: " + leaderboardName + " #" + playerRank + " (Update in: " + countdownText + ")");
            }
        } else {
            // Zeige "Laden..." wenn noch kein Rang verfügbar
            lines.add(insertIndex++, Text.literal(""));
            lines.add(insertIndex++, Text.literal("===Collection Leaderboard===").formatted(Formatting.YELLOW));
            lines.add(insertIndex++, Text.literal("Lade Rang...").formatted(Formatting.GRAY));
            
            // Countdown-Text (wird nur bei erneutem Hover aktualisiert)
            lines.add(insertIndex++, Text.literal("Update in: " + countdownText).formatted(Formatting.GRAY));
            
            lines.add(insertIndex++, Text.literal("Shift für Top10 Leaderboard").formatted(Formatting.AQUA));
            lines.add(insertIndex++, Text.literal("=====================").formatted(Formatting.YELLOW));
        }
    }
    
    /**
     * Formatiert Zahlen mit Tausender-Trennzeichen
     */
    private String formatNumber(long number) {
        return String.format("%,d", number);
    }
    
    /**
     * Bereitet Leaderboard-Overlay vor (für späteres Rendering)
     */
    private void prepareLeaderboardOverlay(String leaderboardName) {
        // TODO: Implementiere Leaderboard-Overlay Rendering
        // Für jetzt nur Debug-Ausgabe und Datenabfrage
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            // Silent error handling
        }
        
        // Hier könnte später das Top10 Leaderboard abgerufen und gerendert werden
        // LeaderboardManager.getInstance().getTopScores(leaderboardName, 10);
    }
    
    
    /**
     * Setzt einen Collection-Wert manuell (für Testing oder externe Updates)
     */
    public void setCollectionValue(String leaderboardName, long value) {
        if (MATERIAL_MAPPING.containsValue(leaderboardName)) {
            collectionValues.put(leaderboardName, value);
            LeaderboardManager.getInstance().updateScore(leaderboardName, value);
        }
    }
    
    /**
     * Gibt den aktuellen Collection-Wert zurück
     */
    public long getCollectionValue(String leaderboardName) {
        return collectionValues.getOrDefault(leaderboardName, 0L);
    }
    
    /**
     * Prüft ob Shift-Taste gerade gedrückt ist
     */
    public boolean isShiftPressed() {
        return isShiftPressed;
    }
    
    /**
     * Setzt einen Player-Rang für ein Leaderboard (für zukünftige Tooltip-Anzeige)
     */
    public void setPlayerRank(String leaderboardName, int rank) {
        playerRankings.put(leaderboardName, rank);
        
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            // Silent error handling
        }
    }
    
    /**
     * Gibt den Player-Rang für ein Leaderboard zurück
     */
    public int getPlayerRank(String leaderboardName) {
        return playerRankings.getOrDefault(leaderboardName, -1);
    }
    
    /**
     * Gibt die aktuelle deutsche Zeit zurück (für Debugging)
     */
    public String getCurrentGermanTime() {
        LocalDateTime now = LocalDateTime.now(GERMAN_TIMEZONE);
        return now.format(TIME_FORMATTER);
    }
    
    /**
     * Gibt die nächste Fetch-Zeit zurück (für Debugging)
     */
    public String getNextFetchTimeString() {
        LocalDateTime nextFetch = getNextFetchTime();
        return nextFetch.format(TIME_FORMATTER);
    }
    
    /**
     * Prüft ob gerade ein Fetch möglich ist (für Debugging)
     */
    public boolean canFetchNow() {
        return canFetchLeaderboardNow();
    }
    
    /**
     * Gibt den aktuellen Countdown-Text zurück (für Debugging)
     */
    public String getCurrentCountdown() {
        long seconds = getSecondsUntilNextFetch();
        return formatCountdown(seconds);
    }
    
    /**
     * Scannt Unterinventar automatisch nach Progress-Items
     */
    private void scanSubInventoryAutomatically() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null || !(client.currentScreen instanceof HandledScreen)) {
            return;
        }
        
        String screenTitle = client.currentScreen.getTitle().getString();
        if (!screenTitle.endsWith(" Collection")) {
            return;
        }
        
        // Extrahiere Collection-Name
        String collectionName = screenTitle.substring(0, screenTitle.length() - " Collection".length()).toLowerCase();
        String leaderboardName = findLeaderboardName(collectionName);
        
        if (leaderboardName == null) {
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                // Silent error handling("⚠️ Unterinventar + Collection nicht erkannt: " + collectionName);
            }
            return;
        }
        
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            // Silent error handling("📋 Unterinventar + Collection " + collectionName + " erkannt");
        }
        
        // Scanne alle Items im Inventar
        HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;
        if (screen.getScreenHandler() != null) {
            for (Slot slot : screen.getScreenHandler().slots) {
                ItemStack stack = slot.getStack();
                if (stack.isEmpty()) continue;
                
                String itemName = stack.getName().getString();
                
                // Prüfe ob es das Progress-Item ist (endet mit " I")
                if (itemName.endsWith(" I")) {
                    scanProgressItem(stack, collectionName, leaderboardName);
                    break; // Nur ein Progress-Item pro Inventar
                }
            }
        }
    }
    
    /**
     * Scannt ein Progress-Item und bereitet das System für automatische Wert-Extraktion vor
     */
    private void scanProgressItem(ItemStack stack, String collectionName, String leaderboardName) {
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            // Silent error handling("🔍 Progress-Item gefunden: " + stack.getName().getString());
            // Silent error handling("📋 Unterinventar + Collection " + collectionName + " erkannt");
            // Silent error handling("💡 System bereit - Wert wird beim ersten Hover extrahiert");
        }
        
        // Starte Leaderboard-Rendering vorbereitung
        startSubInventoryLeaderboardRendering(leaderboardName);
        
        // Der eigentliche Wert wird beim ersten Hover über das Progress-Item extrahiert
        // Das passiert automatisch durch das bestehende Tooltip-Event System
    }
    
    /**
     * Startet das Leaderboard-Rendering für Unterinventare
     */
    private void startSubInventoryLeaderboardRendering(String leaderboardName) {
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            // Silent error handling("🎨 LB wird gerendert (Rendering aktuell noch nicht vorhanden): " + leaderboardName);
        }
        
        // Hole Leaderboard-Daten
        LeaderboardManager.getInstance().getLeaderboard(leaderboardName)
            .thenAccept(response -> {
                if (response != null) {
                    if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                        // Silent error handling("📊 Leaderboard-Daten für Rendering erhalten: " + leaderboardName);
                        // TODO: Hier würde das echte Rendering starten
                    }
                }
            });
    }
    
    @Override
    public void shutdown() {
        isActive = false;
        collectionValues.clear();
        playerRankings.clear();
        lastRankFetch.clear();
        // Silent error handling("🛑 CollectionCollector gestoppt");
    }
    
    @Override
    public String getName() {
        return "CollectionCollector";
    }
    
    @Override
    public boolean isActive() {
        return isActive;
    }
}

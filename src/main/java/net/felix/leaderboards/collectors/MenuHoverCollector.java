package net.felix.leaderboards.collectors;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.felix.leaderboards.LeaderboardManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sammelt Daten aus Menü-Hovertexten (Alltime Kills, Collections)
 */
public class MenuHoverCollector implements DataCollector {
    private boolean isActive = false;
    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // Jede Sekunde prüfen
    
    // Pattern für verschiedene Datentypen
    private static final Pattern ALLTIME_KILLS_PATTERN = Pattern.compile("(?i)kills?.*?([0-9,]+)");
    private static final Pattern COLLECTION_PATTERN = Pattern.compile("(?i)(.+?)\\s+collection.*?([0-9,]+)");
    
    // Cache für letzte Werte
    private final Map<String, Long> lastValues = new HashMap<>();
    
    @Override
    public void initialize() {
        if (isActive) return;
        
        // Registriere Tick-Event für Menü-Überwachung
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        isActive = true;
        System.out.println("✅ MenuHoverCollector initialisiert");
    }
    
    private void onClientTick(MinecraftClient client) {
        if (!isActive || client.player == null || client.world == null) {
            return;
        }
        
        tickCounter++;
        if (tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            checkCurrentScreen(client);
        }
    }
    
    /**
     * Prüft den aktuellen Screen auf relevante Hover-Texte
     */
    private void checkCurrentScreen(MinecraftClient client) {
        Screen currentScreen = client.currentScreen;
        
        if (currentScreen instanceof HandledScreen<?> handledScreen) {
            // Prüfe Slot unter der Maus
            checkHoveredSlot(handledScreen, client);
        }
    }
    
    /**
     * Prüft den Slot unter der Maus auf relevante Informationen
     */
    private void checkHoveredSlot(HandledScreen<?> screen, MinecraftClient client) {
        try {
            // TODO: Implementiere alternative Methode zur Tooltip-Erfassung
            // Da getTooltipFromItem protected ist, brauchen wir einen anderen Ansatz
            // Möglichkeiten:
            // 1. Reflection verwenden
            // 2. Mixin erstellen
            // 3. Alternative Datenquelle finden
            
            // Für jetzt deaktiviert bis wir eine bessere Lösung haben
            
        } catch (Exception e) {
            // Ignoriere Fehler bei der Tooltip-Analyse
        }
    }
    
    /**
     * Verarbeitet einen Tooltip auf relevante Informationen
     */
    private void processTooltip(List<Text> tooltip) {
        if (tooltip == null || tooltip.isEmpty()) return;
        
        StringBuilder fullTooltip = new StringBuilder();
        for (Text line : tooltip) {
            fullTooltip.append(extractTextContent(line)).append("\n");
        }
        
        String tooltipText = fullTooltip.toString();
        
        // Prüfe auf Alltime Kills
        checkAlltimeKills(tooltipText);
        
        // Prüfe auf Collections
        checkCollections(tooltipText);
    }
    
    /**
     * Prüft auf Alltime Kills im Tooltip
     */
    private void checkAlltimeKills(String text) {
        Matcher matcher = ALLTIME_KILLS_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                String killsStr = matcher.group(1).replace(",", "");
                long kills = Long.parseLong(killsStr);
                
                updateIfChanged("alltime_kills", kills);
            } catch (NumberFormatException e) {
                // Ignoriere Parse-Fehler
            }
        }
    }
    
    /**
     * Prüft auf Collection-Daten im Tooltip
     */
    private void checkCollections(String text) {
        Matcher matcher = COLLECTION_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                String materialName = matcher.group(1).toLowerCase().trim();
                String amountStr = matcher.group(2).replace(",", "");
                long amount = Long.parseLong(amountStr);
                
                // Mappe Material-Namen auf Leaderboard-Namen
                String leaderboardName = mapMaterialToLeaderboard(materialName);
                if (leaderboardName != null) {
                    updateIfChanged(leaderboardName, amount);
                }
            } catch (NumberFormatException e) {
                // Ignoriere Parse-Fehler
            }
        }
    }
    
    /**
     * Mappt Material-Namen auf Leaderboard-Namen
     */
    private String mapMaterialToLeaderboard(String materialName) {
        // Verwende das gleiche Mapping wie im CollectionCollector
        Map<String, String> mapping = new HashMap<>();
        mapping.put("oak", "oak_collection");
        mapping.put("eiche", "oak_collection");
        mapping.put("jungle", "jungle_collection");
        mapping.put("dschungel", "jungle_collection");
        mapping.put("spruce", "spruce_collection");
        mapping.put("fichte", "spruce_collection");
        mapping.put("bamboo", "bamboo_collection");
        mapping.put("bambus", "bamboo_collection");
        mapping.put("coal", "coal_collection");
        mapping.put("kohle", "coal_collection");
        mapping.put("copper", "raw_copper_collection");
        mapping.put("kupfer", "raw_copper_collection");
        mapping.put("iron", "raw_iron_collection");
        mapping.put("eisen", "raw_iron_collection");
        mapping.put("gold", "raw_gold_collection");
        mapping.put("diamond", "diamond_collection");
        mapping.put("diamant", "diamond_collection");
        // ... weitere Mappings
        
        String direct = mapping.get(materialName);
        if (direct != null) return direct;
        
        // Fuzzy-Suche
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (materialName.contains(entry.getKey()) || entry.getKey().contains(materialName)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Extrahiert den Text-Inhalt aus einer Text-Komponente
     */
    private String extractTextContent(Text text) {
        if (text == null) return "";
        
        StringBuilder content = new StringBuilder();
        content.append(text.getString());
        
        for (Text sibling : text.getSiblings()) {
            content.append(extractTextContent(sibling));
        }
        
        return content.toString();
    }
    
    /**
     * Aktualisiert einen Wert nur wenn er sich geändert hat
     */
    private void updateIfChanged(String leaderboardName, long newValue) {
        Long lastValue = lastValues.get(leaderboardName);
        if (lastValue == null || !lastValue.equals(newValue)) {
            lastValues.put(leaderboardName, newValue);
            LeaderboardManager.getInstance().updateScore(leaderboardName, newValue);
            System.out.println("📊 Menu-Hover Update: " + leaderboardName + " = " + newValue);
        }
    }
    
    /**
     * Setzt einen Wert manuell (für Testing)
     */
    public void setValue(String leaderboardName, long value) {
        updateIfChanged(leaderboardName, value);
    }
    
    /**
     * Gibt einen gespeicherten Wert zurück
     */
    public Long getValue(String leaderboardName) {
        return lastValues.get(leaderboardName);
    }
    
    @Override
    public void shutdown() {
        isActive = false;
        lastValues.clear();
        System.out.println("🛑 MenuHoverCollector gestoppt");
    }
    
    @Override
    public String getName() {
        return "MenuHoverCollector";
    }
    
    @Override
    public boolean isActive() {
        return isActive;
    }
}

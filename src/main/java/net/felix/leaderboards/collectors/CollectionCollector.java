package net.felix.leaderboards.collectors;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.utilities.DebugUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sammelt Collection-Daten durch Tooltip-Parsing in Collection-Inventaren
 * Erkennt Collection-Werte, sendet sie an Server und zeigt eigenen Rang in Tooltips
 */
public class CollectionCollector implements DataCollector {
    private boolean isActive = false;
    
    // Pattern für Collection-Tooltips
    // Beispiel: "Oak Collection: 12,345,678" oder "Level: 15 (12,345 / 50,000)"
    private static final Pattern COLLECTION_VALUE_PATTERN = Pattern.compile("(.+?)\\s*Collection:\\s*([0-9,]+)");
    private static final Pattern COLLECTION_LEVEL_PATTERN = Pattern.compile("Level:\\s*([0-9]+)\\s*\\(([0-9,]+)\\s*/\\s*([0-9,]+)\\)");
    
    // Collection-Inventar Erkennung (PLACEHOLDER - muss angepasst werden)
    private static final String COLLECTION_INVENTORY_TITLE = "Collections"; // TODO: Echten Titel herausfinden
    
    // Mapping von Material-Namen zu Leaderboard-Namen
    private static final Map<String, String> MATERIAL_MAPPING = new HashMap<>();
    
    static {
        // Holz-Arten
        MATERIAL_MAPPING.put("oak", "oak_collection");
        MATERIAL_MAPPING.put("eiche", "oak_collection");
        MATERIAL_MAPPING.put("jungle", "jungle_collection");
        MATERIAL_MAPPING.put("dschungel", "jungle_collection");
        MATERIAL_MAPPING.put("spruce", "spruce_collection");
        MATERIAL_MAPPING.put("fichte", "spruce_collection");
        MATERIAL_MAPPING.put("bamboo", "bamboo_collection");
        MATERIAL_MAPPING.put("bambus", "bamboo_collection");
        MATERIAL_MAPPING.put("mushroom", "mushroom_collection");
        MATERIAL_MAPPING.put("pilz", "mushroom_collection");
        MATERIAL_MAPPING.put("dark oak", "dark_oak_collection");
        MATERIAL_MAPPING.put("dunkeleiche", "dark_oak_collection");
        MATERIAL_MAPPING.put("mangrove", "mangrove_collection");
        MATERIAL_MAPPING.put("mangroven", "mangrove_collection");
        MATERIAL_MAPPING.put("crimson", "crimson_collection");
        MATERIAL_MAPPING.put("karmesin", "crimson_collection");
        MATERIAL_MAPPING.put("warped", "warped_collection");
        MATERIAL_MAPPING.put("wirrwarr", "warped_collection");
        
        // Erze
        MATERIAL_MAPPING.put("coal", "coal_collection");
        MATERIAL_MAPPING.put("kohle", "coal_collection");
        MATERIAL_MAPPING.put("copper", "raw_copper_collection");
        MATERIAL_MAPPING.put("kupfer", "raw_copper_collection");
        MATERIAL_MAPPING.put("iron", "raw_iron_collection");
        MATERIAL_MAPPING.put("eisen", "raw_iron_collection");
        MATERIAL_MAPPING.put("gold", "raw_gold_collection");
        MATERIAL_MAPPING.put("diamond", "diamond_collection");
        MATERIAL_MAPPING.put("diamant", "diamond_collection");
        
        // Spezielle Materialien
        MATERIAL_MAPPING.put("sulfur", "sulfur_collection");
        MATERIAL_MAPPING.put("schwefel", "sulfur_collection");
        MATERIAL_MAPPING.put("quartz", "quartz_collection");
        MATERIAL_MAPPING.put("obsidian", "obsidian_collection");
        MATERIAL_MAPPING.put("ancient debris", "ancient_debris_collection");
        MATERIAL_MAPPING.put("antiker schutt", "ancient_debris_collection");
        MATERIAL_MAPPING.put("echo", "echo_collection");
        MATERIAL_MAPPING.put("echokristall", "echo_collection");
    }
    
    // Cache für Collection-Werte
    private final Map<String, Long> collectionValues = new HashMap<>();
    
    @Override
    public void initialize() {
        if (isActive) return;
        
        // Registriere Tooltip-Event für Collection-Parsing
        ItemTooltipCallback.EVENT.register((stack, context, tooltipType, lines) -> {
            if (!isActive) return;
            
            // Nur in Collection-Inventar verarbeiten
            if (!isInCollectionInventory()) return;
            
            // Collection-Daten aus Tooltip extrahieren
            parseCollectionTooltip(lines);
        });
        
        isActive = true;
        System.out.println("✅ CollectionCollector initialisiert (Tooltip-basiert)");
    }
    
    /**
     * Prüft ob wir uns im Collection-Inventar befinden
     */
    private boolean isInCollectionInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) return false;
        
        String screenTitle = client.currentScreen.getTitle().getString();
        
        // TODO: Hier den echten Collection-Inventar-Titel einfügen
        // Beispiele: "Collections", "Sammlungen", oder Unicode-Zeichen
        return screenTitle.contains(COLLECTION_INVENTORY_TITLE) ||
               screenTitle.contains("Sammlungen") ||
               screenTitle.contains("Collection");
    }
    
    /**
     * Parsed Collection-Daten aus einem Tooltip
     */
    private void parseCollectionTooltip(List<Text> lines) {
        if (lines.isEmpty()) return;
        
        String collectionName = null;
        long collectionValue = 0;
        
        for (Text line : lines) {
            String lineText = line.getString();
            
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                System.out.println("🔍 Collection Tooltip Line: " + lineText);
            }
            
            // Prüfe auf "Material Collection: 12,345,678" Format
            Matcher valueMatcher = COLLECTION_VALUE_PATTERN.matcher(lineText);
            if (valueMatcher.find()) {
                collectionName = valueMatcher.group(1).toLowerCase().trim();
                String valueStr = valueMatcher.group(2).replace(",", "");
                
                try {
                    collectionValue = Long.parseLong(valueStr);
                    handleCollectionData(collectionName, collectionValue);
                } catch (NumberFormatException e) {
                    System.err.println("❌ Fehler beim Parsen der Collection-Zahl: " + valueStr);
                }
                break;
            }
            
            // Prüfe auf "Level: 15 (12,345 / 50,000)" Format
            Matcher levelMatcher = COLLECTION_LEVEL_PATTERN.matcher(lineText);
            if (levelMatcher.find()) {
                String currentStr = levelMatcher.group(2).replace(",", "");
                
                try {
                    long currentProgress = Long.parseLong(currentStr);
                    if (collectionName != null) {
                        handleCollectionData(collectionName, currentProgress);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("❌ Fehler beim Parsen des Collection-Progress: " + currentStr);
                }
            }
        }
    }
    
    /**
     * Verarbeitet erkannte Collection-Daten
     */
    private void handleCollectionData(String materialName, long value) {
        String leaderboardName = findLeaderboardName(materialName);
        if (leaderboardName == null) {
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                System.out.println("⚠️ Unbekannte Collection: " + materialName);
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
                System.out.println("📦 Collection Update: " + materialName + " = " + value + " (war: " + currentValue + ")");
            }
        }
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
    
    @Override
    public void shutdown() {
        isActive = false;
        collectionValues.clear();
        System.out.println("🛑 CollectionCollector gestoppt");
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

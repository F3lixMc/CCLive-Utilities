package net.felix.leaderboards.collectors;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.utilities.DebugUtility;

import java.lang.reflect.Method;
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
    // Pattern für Alltime Kills: erkennt "x / y Kills" Format, extrahiert nur x (alltime kills)
    // Beispiel: "10% (50250 / 50000) Kills" -> extrahiert 50250
    private static final Pattern ALLTIME_KILLS_PATTERN = Pattern.compile("(?i)\\(([0-9,]+)\\s*/\\s*[0-9,]+\\)\\s*kills?", Pattern.CASE_INSENSITIVE);
    // Fallback: Falls kein "x / y" Format gefunden wird, versuche einfaches Pattern
    private static final Pattern ALLTIME_KILLS_FALLBACK_PATTERN = Pattern.compile("(?i)kills?.*?([0-9,]+)");
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
        
        if (currentScreen == null) {
            return; // Kein Screen geöffnet
        }
        
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            DebugUtility.debugLeaderboard("📋 Screen geöffnet: " + currentScreen.getClass().getSimpleName());
        }
        
        if (currentScreen instanceof HandledScreen<?> handledScreen) {
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                DebugUtility.debugLeaderboard("📋 HandledScreen erkannt, prüfe Slot unter Maus...");
            }
            // Generische Hover-Auswertung für alle Menüs
            checkHoveredSlot(handledScreen, client);
        } else if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            // Debug: Zeige Screen-Typ wenn kein HandledScreen
            DebugUtility.debugLeaderboard("📋 Screen geöffnet (kein HandledScreen): " + currentScreen.getClass().getSimpleName());
        }
    }
    
    /**
     * Prüft den Slot unter der Maus auf relevante Informationen
     */
    private void checkHoveredSlot(HandledScreen<?> screen, MinecraftClient client) {
        try {
            // Hole Mausposition
            double mouseX = client.mouse.getX() * (double) client.getWindow().getScaledWidth() / (double) client.getWindow().getWidth();
            double mouseY = client.mouse.getY() * (double) client.getWindow().getScaledHeight() / (double) client.getWindow().getHeight();
            
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                DebugUtility.debugLeaderboard("📋 Mausposition: X=" + mouseX + ", Y=" + mouseY);
                DebugUtility.debugLeaderboard("📋 Anzahl Slots: " + screen.getScreenHandler().slots.size());
            }
            
            // Finde Slot unter der Maus (wie in HandledScreenMixin)
            Slot hoveredSlot = null;
            int slotCount = 0;
            for (Slot slot : screen.getScreenHandler().slots) {
                slotCount++;
                // Hole Screen-Position (x, y) via Reflection
                try {
                    java.lang.reflect.Field xField = HandledScreen.class.getDeclaredField("x");
                    java.lang.reflect.Field yField = HandledScreen.class.getDeclaredField("y");
                    xField.setAccessible(true);
                    yField.setAccessible(true);
                    int x = xField.getInt(screen);
                    int y = yField.getInt(screen);
                    
                    if (DebugUtility.isLeaderboardDebuggingEnabled() && slotCount <= 5) {
                        // Zeige erste 5 Slots für Debug
                        DebugUtility.debugLeaderboard("📋 Slot #" + slotCount + ": x=" + (slot.x + x) + ", y=" + (slot.y + y) + ", hasStack=" + slot.hasStack());
                    }
                    
                    if (slot.x + x <= mouseX && mouseX < slot.x + x + 16 &&
                        slot.y + y <= mouseY && mouseY < slot.y + y + 16) {
                        hoveredSlot = slot;
                        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                            DebugUtility.debugLeaderboard("✅ Slot unter Maus gefunden: #" + slotCount);
                        }
                        break;
                    }
                } catch (Exception e) {
                    if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                        DebugUtility.debugLeaderboard("⚠️ Fehler beim Prüfen von Slot #" + slotCount + ": " + e.getMessage());
                    }
                    // Fallback: Prüfe einfach alle Slots mit Items (nur wenn kein Slot gefunden wurde)
                    if (hoveredSlot == null && slot.hasStack()) {
                        hoveredSlot = slot;
                        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                            DebugUtility.debugLeaderboard("📋 Fallback: Verwende Slot #" + slotCount + " (hat Item)");
                        }
                    }
                }
            }
            
            if (hoveredSlot == null) {
                if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                    DebugUtility.debugLeaderboard("❌ Kein Slot unter der Maus gefunden");
                }
                return;
            }
            
            if (!hoveredSlot.hasStack()) {
                if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                    DebugUtility.debugLeaderboard("❌ Slot hat kein Item");
                }
                return;
            }
            
            ItemStack stack = hoveredSlot.getStack();
            if (stack == null || stack.isEmpty()) {
                if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                    DebugUtility.debugLeaderboard("❌ ItemStack ist leer");
                }
                return;
            }
            
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                DebugUtility.debugLeaderboard("📋 Item gefunden: " + stack.getName().getString());
            }
            
            // Tooltip über Helper-Methode via Reflection auslesen
            List<Text> tooltip = getTooltipFromItem(screen, client, stack);
            
            if (tooltip == null || tooltip.isEmpty()) {
                if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                    DebugUtility.debugLeaderboard("❌ Tooltip ist leer oder null");
                }
                return;
            }
            
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                DebugUtility.debugLeaderboard("✅ Tooltip gefunden mit " + tooltip.size() + " Zeilen");
                // Zeige erste 3 Zeilen für Debug
                for (int i = 0; i < Math.min(3, tooltip.size()); i++) {
                    DebugUtility.debugLeaderboard("   Zeile " + (i+1) + ": " + tooltip.get(i).getString());
                }
            }
            
            processTooltip(tooltip);
        } catch (Exception e) {
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                DebugUtility.debugLeaderboard("❌ Fehler beim Auslesen des Tooltips: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Liest den Tooltip eines Items aus einem HandledScreen via Reflection aus.
     * Robust gegen unterschiedliche Methodensignaturen von getTooltipFromItem.
     */
    @SuppressWarnings("unchecked")
    private List<Text> getTooltipFromItem(HandledScreen<?> screen, MinecraftClient client, ItemStack stack) {
        try {
            Method[] methods = HandledScreen.class.getDeclaredMethods();
            for (Method method : methods) {
                if (!"getTooltipFromItem".equals(method.getName())) {
                    continue;
                }
                
                Class<?>[] params = method.getParameterTypes();
                method.setAccessible(true);
                
                try {
                    // Signatur: getTooltipFromItem(MinecraftClient, ItemStack)
                    if (params.length == 2 &&
                        MinecraftClient.class.isAssignableFrom(params[0]) &&
                        ItemStack.class.isAssignableFrom(params[1])) {
                        return (List<Text>) method.invoke(screen, client, stack);
                    }
                    
                    // Signatur: getTooltipFromItem(ItemStack)
                    if (params.length == 1 &&
                        ItemStack.class.isAssignableFrom(params[0])) {
                        return (List<Text>) method.invoke(screen, stack);
                    }
                } catch (Exception inner) {
                    // Versuche andere Overloads weiter
                }
            }
            
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                DebugUtility.debugLeaderboard("❌ Keine passende getTooltipFromItem-Methode gefunden");
            }
        } catch (Exception e) {
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                DebugUtility.debugLeaderboard("❌ Fehler bei getTooltipFromItem-Reflection: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
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
        
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            DebugUtility.debugLeaderboard("📋 Vollständiger Tooltip-Text:");
            DebugUtility.debugLeaderboard(tooltipText);
        }
        
        // Prüfe auf Alltime Kills
        checkAlltimeKills(tooltipText);
        
        // Prüfe auf Collections
        checkCollections(tooltipText);
    }
    
    /**
     * Prüft auf Alltime Kills im Tooltip
     * Extrahiert den Wert aus "x / y" Format (nur x wird verwendet)
     */
    private void checkAlltimeKills(String text) {
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            DebugUtility.debugLeaderboard("📋 Prüfe auf Alltime Kills im Text...");
        }
        
        // Prüfe zuerst auf "x / y" Format (primäres Pattern)
        Matcher matcher = ALLTIME_KILLS_PATTERN.matcher(text);
        boolean found = matcher.find();
        
        if (DebugUtility.isLeaderboardDebuggingEnabled()) {
            DebugUtility.debugLeaderboard("📋 Primäres Pattern (x / y): " + (found ? "GEFUNDEN" : "nicht gefunden"));
        }
        
        if (!found) {
            // Fallback: Versuche einfaches Pattern
            matcher = ALLTIME_KILLS_FALLBACK_PATTERN.matcher(text);
            found = matcher.find();
            
            if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                DebugUtility.debugLeaderboard("📋 Fallback-Pattern: " + (found ? "GEFUNDEN" : "nicht gefunden"));
            }
        }
        
        if (found) {
            try {
                String killsStr = matcher.group(1).replace(",", "");
                long kills = Long.parseLong(killsStr);
                
                // Debug-Log nur wenn Leaderboard-Debugging aktiviert ist
                if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                    String textSnippet = text.substring(Math.max(0, matcher.start()-20), Math.min(text.length(), matcher.end()+20));
                    DebugUtility.debugLeaderboard("📋 Alltime Kills aus Menü erkannt: " + kills);
                    DebugUtility.debugLeaderboard("   Text-Ausschnitt: '" + textSnippet + "'");
                }
                
                // SEKUNDÄR: Sende nur an FloorKillsCollector für Doppelcheck (nicht direkt an Server)
                // Der FloorKillsCollector verwendet die berechnete Summe als PRIMÄR
                DataCollector floorKillsCollector = LeaderboardManager.getInstance().getCollector("floor_kills");
                if (floorKillsCollector instanceof FloorKillsCollector) {
                    ((FloorKillsCollector) floorKillsCollector).setMenuAlltimeKills(kills);
                    if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                        DebugUtility.debugLeaderboard("📋 Menü-Wert an FloorKillsCollector gesendet für Doppelcheck");
                    }
                } else {
                    // Fallback: Falls FloorKillsCollector nicht verfügbar, sende direkt
                    if (DebugUtility.isLeaderboardDebuggingEnabled()) {
                        DebugUtility.debugLeaderboard("⚠️ FloorKillsCollector nicht verfügbar, sende direkt (Fallback)");
                    }
                    updateIfChanged("alltime_kills", kills);
                }
            } catch (NumberFormatException e) {
                System.err.println("❌ [MenuHoverCollector] Fehler beim Parsen der Alltime Kills: " + e.getMessage());
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

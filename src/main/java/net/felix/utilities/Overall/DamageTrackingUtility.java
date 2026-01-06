package net.felix.utilities.Overall;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.text.Text;
import net.felix.profile.ProfileStatsManager;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility zum Tracken des maximalen Schadens, der dem Spieler angezeigt wird.
 * Erkennt Damage-Hologramme (ArmorStands mit numerischem CustomName) und
 * aktualisiert den max_damage Wert im ProfileStatsManager.
 */
public class DamageTrackingUtility {
    private static boolean isInitialized = false;
    private static int tickCounter = 0;
    private static final int SCAN_INTERVAL = 10; // Alle 10 Ticks scannen (2x pro Sekunde)
    private static final double SCAN_RADIUS = 7.0; // 7 Blöcke Umkreis
    
    // Pattern zum Entfernen von Minecraft-Formatierungscodes (§0-§f, §r, etc.)
    private static final Pattern FORMAT_CODE_PATTERN = Pattern.compile("§[0-9a-fk-or]");
    
    // Pattern zum Prüfen, ob ein String eine reine Zahl ist
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");
    
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        try {
            // Registriere Tick-Event für Damage-Scanning
            ClientTickEvents.END_CLIENT_TICK.register(DamageTrackingUtility::onClientTick);
            
            isInitialized = true;
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    /**
     * Wird in jedem Client-Tick aufgerufen
     */
    private static void onClientTick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }
        
        // Nur alle SCAN_INTERVAL Ticks scannen
        tickCounter++;
        if (tickCounter < SCAN_INTERVAL) {
            return;
        }
        tickCounter = 0;
        
        // Scanne nach Damage-Hologrammen
        scanForDamageHolograms(client);
    }
    
    /**
     * Scannt alle ArmorStands im Umkreis und extrahiert Damage-Werte
     */
    private static void scanForDamageHolograms(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }
        
        // Erstelle Bounding Box um den Spieler
        Box scanBox = client.player.getBoundingBox().expand(SCAN_RADIUS);
        
        // Sammle alle Damage-Werte (Set verhindert Duplikate)
        Set<Integer> damageValues = new HashSet<>();
        
        // Iteriere durch alle Entities im Umkreis
        for (Entity entity : client.world.getOtherEntities(client.player, scanBox, 
                e -> e instanceof ArmorStandEntity)) {
            
            if (entity instanceof ArmorStandEntity armorStand) {
                // Prüfe ob ArmorStand einen CustomName hat
                if (!armorStand.hasCustomName()) {
                    continue;
                }
                
                // Extrahiere CustomName-Text
                Text customNameText = armorStand.getCustomName();
                if (customNameText == null) {
                    continue;
                }
                
                String customName = customNameText.getString();
                if (customName == null || customName.isEmpty()) {
                    continue;
                }
                
                // Entferne Formatierungscodes
                String cleaned = removeFormatCodes(customName);
                
                // Prüfe ob der bereinigte Text eine reine Zahl ist
                if (isNumeric(cleaned)) {
                    try {
                        int damageValue = Integer.parseInt(cleaned);
                        damageValues.add(damageValue);
                    } catch (NumberFormatException e) {
                        // Sollte nicht passieren, da isNumeric() bereits prüft
                        // Aber sicherheitshalber ignorieren
                    }
                }
            }
        }
        
        // Wenn Damage-Werte gefunden wurden, aktualisiere den Max-Wert
        if (!damageValues.isEmpty()) {
            int maxFound = damageValues.stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);
            
            if (maxFound > 0) {
                ProfileStatsManager.getInstance().updateMaxDamage(maxFound);
            }
        }
    }
    
    /**
     * Entfernt Minecraft-Formatierungscodes aus einem String
     * (z.B. "§f4437" -> "4437" oder "4437" -> "4437")
     */
    private static String removeFormatCodes(String text) {
        if (text == null) {
            return "";
        }
        // Ersetze alle Formatierungscodes (§0-§f, §r, §k, §l, §m, §n, §o) mit leerem String
        return FORMAT_CODE_PATTERN.matcher(text).replaceAll("");
    }
    
    /**
     * Prüft ob ein String eine reine Zahl ist (nur Ziffern)
     */
    private static boolean isNumeric(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return NUMBER_PATTERN.matcher(text).matches();
    }
}


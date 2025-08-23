package net.felix.utilities;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.network.ClientPlayerEntity;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.felix.CCLiveUtilitiesConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WellenTrackerUtility {
    
    private static boolean isInitialized = false;
    private static boolean isTrackingMonsters = false;
    private static boolean showOverlays = true;
    
    // Hotkey variable
    private static KeyBinding toggleKeyBinding;
    
    // Monster tracking data
    private static final Map<String, Integer> currentWaveMonsters = new HashMap<>();
    private static final Map<String, Map<String, Integer>> waveData = new HashMap<>();
    private static String currentWave = "";
    private static String currentDimension = "";
    private static String playerName = "";
    
    // Wave timing
    private static long waveStartTime = 0;
    

    
    // Text Display tracking (much more reliable than entity tracking)
    private static final Map<Entity, MonsterInfo> trackedTextDisplays = new HashMap<>();
    
    // Track ALL monsters that spawn (for comparison with death count)
    private static final Map<String, Integer> spawnedMonsters = new HashMap<>();
    
    // Monster info class to store monster data
    private static class MonsterInfo {
        final String entityType;
        final String displayName;
        final double maxHealth;
        final long spawnTime;
        
        MonsterInfo(String entityType, String displayName, double maxHealth) {
            this.entityType = entityType;
            this.displayName = displayName;
            this.maxHealth = maxHealth;
            this.spawnTime = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return entityType + " (" + displayName + ")";
        }
    }
    
    // Rendering constants
    private static final int LINE_HEIGHT = 13;
    private static final int OVERLAY_WIDTH = 200;
    private static final int OVERLAY_HEIGHT = 120;
    private static final int TEXT_PADDING = 10;
    
    // Chinese character mapping for waves
    private static final Map<String, String> CHINESE_TO_NUMBER = new HashMap<>();
    static {
        CHINESE_TO_NUMBER.put("㞂", "0");
        CHINESE_TO_NUMBER.put("㞃", "1");
        CHINESE_TO_NUMBER.put("㞄", "2");
        CHINESE_TO_NUMBER.put("㞅", "3");
        CHINESE_TO_NUMBER.put("㞆", "4");
        CHINESE_TO_NUMBER.put("㞇", "5");
        CHINESE_TO_NUMBER.put("㞈", "6");
        CHINESE_TO_NUMBER.put("㞉", "7");
        CHINESE_TO_NUMBER.put("㞊", "8");
        CHINESE_TO_NUMBER.put("㞋", "9");
    }
    
    // File paths for logging
    private static final String LOG_FILE_PATH = "wellen_monster_log.txt";
    private static final String DEBUG_LOG_FILE_PATH = "wellen_monster_debug.log";
    
    // Logger settings
    private static final boolean ENABLE_DEBUG_LOGGING = true;
    private static final boolean ENABLE_CONSOLE_LOGGING = true;
    
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        try {
            // Log initialization
            logDebug("=== Monster Tracker Utility Initialization Started ===");
            
            // Register hotkey
            registerHotkey();
            logDebug("Hotkey registered successfully");
            
            // Client-side events
            ClientTickEvents.END_CLIENT_TICK.register(WellenTrackerUtility::onClientTick);
            logDebug("Client tick event registered");
            
            // Register HUD rendering
            HudRenderCallback.EVENT.register((drawContext, tickDelta) -> onHudRender(drawContext, tickDelta));
            logDebug("HUD rendering event registered");
            
            isInitialized = true;
            logDebug("=== Monster Tracker Utility Initialization Completed ===");
        } catch (Exception e) {
            logError("Failed to initialize Monster Tracker Utility: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void registerHotkey() {
        // Register toggle hotkey
        toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cclive-utilities.wellen-toggle",
            InputUtil.Type.KEYSYM,
            InputUtil.UNKNOWN_KEY.getCode(), // Unbound key
            "category.cclive-utilities.wellen"
        ));
        
        // Register debug hotkey
        KeyBinding debugKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cclive-utilities.wellen-debug",
            InputUtil.Type.KEYSYM,
            org.lwjgl.glfw.GLFW.GLFW_KEY_F9, // F9 key for debug
            "category.cclive-utilities.wellen"
        ));
        
        logDebug("Debug hotkey registered: F9");
    }
    
    private static void onClientTick(MinecraftClient client) {
        // Check Tab key for overlay visibility
        checkTabKey();
        
        // Handle hotkey
        handleHotkey();
        
        // Check configuration
        if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
            !CCLiveUtilitiesConfig.HANDLER.instance().wellenTrackerEnabled || 
            !CCLiveUtilitiesConfig.HANDLER.instance().showWellenTracker) {
            return;
        }
        
        if (client.player == null) {
            if (isTrackingMonsters) {
                logDebug("Player is null, stopping monster tracking");
                isTrackingMonsters = false;
            }
            return;
        }
        
        // Check tracking conditions (dimension changes, etc.)
        checkTrackingConditions(client);
        
        // If tracking is active, update monster counts
        if (isTrackingMonsters) {
            updateMonsterTracking(client);
        }
    }
    
    private static void checkTabKey() {
        // Check if player list key is pressed (respects custom key bindings)
        if (KeyBindingUtility.isPlayerListKeyPressed()) {
            if (showOverlays) {
                logDebug("Tab key pressed, hiding overlays");
                showOverlays = false; // Hide overlays when player list key is pressed
            }
        } else {
            if (!showOverlays) {
                logDebug("Tab key released, showing overlays");
                showOverlays = true; // Show overlays when player list key is not pressed
            }
        }
    }
    
    private static void handleHotkey() {
        if (toggleKeyBinding.wasPressed()) {
            // Toggle monster tracking
            isTrackingMonsters = !isTrackingMonsters;
            if (isTrackingMonsters) {
                logDebug("Hotkey pressed: Monster tracking ENABLED");
                
                // Clear all old tracking data when starting fresh
                trackedTextDisplays.clear();
                currentWaveMonsters.clear();
                spawnedMonsters.clear();
                
                // Set wave start time when manually starting tracking
                if (waveStartTime == 0) {
                    waveStartTime = System.currentTimeMillis();
                    logDebug("🚀 Manual tracking started at " + new java.util.Date(waveStartTime));
                }
                
                logDebug("🧹 Cleared all tracking data for fresh start");
            } else {
                logDebug("Hotkey pressed: Monster tracking DISABLED");
                resetTrackingData();
            }
        }
        
        // Check for debug hotkey (F9)
        if (org.lwjgl.glfw.GLFW.glfwGetKey(org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), org.lwjgl.glfw.GLFW.GLFW_KEY_F9) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            logDebug("Debug hotkey (F9) pressed - printing debug info");
            printDebugInfo();
        }
        
        // Check for file test hotkey (F10)
        if (org.lwjgl.glfw.GLFW.glfwGetKey(org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), org.lwjgl.glfw.GLFW.GLFW_KEY_F10) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            logDebug("File test hotkey (F10) pressed - testing file creation");
            testFileCreation();
        }
        
        // Check for scoreboard debug hotkey (F11)
        if (org.lwjgl.glfw.GLFW.glfwGetKey(org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), org.lwjgl.glfw.GLFW.GLFW_KEY_F11) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            logDebug("Scoreboard debug hotkey (F11) pressed - checking scoreboard");
            debugScoreboard(MinecraftClient.getInstance());
        }
    }
    
    private static void checkTrackingConditions(MinecraftClient client) {
        if (client.player == null) return;
        
        // Get player name
        String newPlayerName = client.player.getName().getString();
        if (!newPlayerName.equals(playerName)) {
            logDebug("Player name changed: '" + playerName + "' -> '" + newPlayerName + "'");
            playerName = newPlayerName;
        }
        
        // Check dimension
        String newDimension = client.world.getRegistryKey().getValue().toString();
        if (!newDimension.equals(currentDimension)) {
            logDebug("Dimension changed: '" + currentDimension + "' -> '" + newDimension + "'");
            currentDimension = newDimension;
        }
        
        // If tracking is manually enabled via hotkey, always check for waves in bossbar
        if (isTrackingMonsters) {
            updateCurrentWave(client);
        }
    }
    

    
    private static void updateCurrentWave(MinecraftClient client) {
        try {
            // Get bossbar text using reflection
            if (client.inGameHud != null && client.inGameHud.getBossBarHud() != null) {
                BossBarHud bossBarHud = client.inGameHud.getBossBarHud();
                
                // Try different field names for bossbars
                Map<UUID, ClientBossBar> bossBars = null;
                String[] possibleFieldNames = {"field_2060", "bossBars", "bossbars", "bossBars", "bars", "bossBarMap"};
                
                for (String fieldName : possibleFieldNames) {
                    try {
                        java.lang.reflect.Field bossBarsField = bossBarHud.getClass().getDeclaredField(fieldName);
                        bossBarsField.setAccessible(true);
                        Object fieldValue = bossBarsField.get(bossBarHud);
                        
                        if (fieldValue instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<UUID, ClientBossBar> tempBars = (Map<UUID, ClientBossBar>) fieldValue;
                            if (tempBars != null && !tempBars.isEmpty()) {
                                bossBars = tempBars;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
                
                if (bossBars != null) {
                    for (ClientBossBar bossBar : bossBars.values()) {
                        String bossBarText = bossBar.getName().getString();
                        
                        String decodedWave = decodeChineseWave(bossBarText);
                        
                        if (!decodedWave.isEmpty() && !decodedWave.equals(currentWave)) {
                            // New wave detected
                            if (!currentWave.isEmpty()) {
                                // Save previous wave data
                                saveWaveData();
                            }
                            
                            currentWave = decodedWave;
                            currentWaveMonsters.clear();
                            spawnedMonsters.clear();
                            trackedTextDisplays.clear();
                            waveStartTime = System.currentTimeMillis();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logError("Error updating current wave: " + e.getMessage());
        }
    }
    
    private static String decodeChineseWave(String text) {
        StringBuilder waveNumber = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            if (CHINESE_TO_NUMBER.containsKey(character)) {
                waveNumber.append(CHINESE_TO_NUMBER.get(character));
                logDebug("Decoded chinese character: '" + character + "' -> '" + CHINESE_TO_NUMBER.get(character) + "'");
            }
        }
        String result = waveNumber.toString();
        logDebug("Final decoded wave number: '" + result + "' from text: '" + text + "'");
        return result;
    }
    
    private static void updateMonsterTracking(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        
        // Get entities around player (radius of 64 blocks)
        double radius = 64.0;
        double playerX = client.player.getX();
        double playerY = client.player.getY();
        double playerZ = client.player.getZ();
        
        // Track Monster Entities - Only Count Disappeared Monsters
        int newMonstersTracked = 0;
        int monstersDisappeared = 0;
        
        // Check for monsters that are no longer valid (died/despawned)
        for (Entity trackedEntity : new HashSet<>(trackedTextDisplays.keySet())) {
            if (trackedEntity.isRemoved() || !trackedEntity.isAlive()) {
                MonsterInfo monsterInfo = trackedTextDisplays.remove(trackedEntity);
                if (monsterInfo != null) {
                    logDebug("Monster disappeared/died: " + monsterInfo.entityType + " - " + monsterInfo.displayName + " (removed from tracking)");
                    monstersDisappeared++;
                }
            }
        }
        
        // Additional safety check: Remove any tracked entities that are no longer in the world
        for (Entity trackedEntity : new HashSet<>(trackedTextDisplays.keySet())) {
            boolean stillExists = false;
            for (Entity worldEntity : client.world.getEntities()) {
                if (worldEntity.getId() == trackedEntity.getId()) {
                    stillExists = true;
                    break;
                }
            }
            
            if (!stillExists) {
                MonsterInfo monsterInfo = trackedTextDisplays.remove(trackedEntity);
                if (monsterInfo != null) {
                    logDebug("Safety cleanup: Removed non-existent tracked entity: " + monsterInfo.entityType + " - " + monsterInfo.displayName);
                }
            }
        }
        
        // Now check for new monsters to track OR update existing ones
        for (Entity entity : client.world.getEntities()) {
            if (isValidMonster(entity)) {
                double monsterDistance = Math.sqrt(
                    Math.pow(entity.getX() - playerX, 2) +
                    Math.pow(entity.getY() - playerY, 2) +
                    Math.pow(entity.getZ() - playerZ, 2)
                );
                
                if (monsterDistance <= radius) {
                    // Check if this monster is already being tracked using entity ID
                    Entity alreadyTrackedEntity = null;
                    for (Entity trackedEntity : trackedTextDisplays.keySet()) {
                        if (trackedEntity.getId() == entity.getId()) {
                            alreadyTrackedEntity = trackedEntity;
                            break;
                        }
                    }
                    
                    if (alreadyTrackedEntity != null) {
                         // Monster is already tracked - check if HP changed (monster died)
                         String entityType = getEntityTypeName(entity);
                         String displayName = "";
                         
                         if (entity.hasCustomName()) {
                             net.minecraft.text.Text customName = entity.getCustomName();
                             if (customName != null) {
                                 displayName = customName.getString().trim();
                             }
                         }
                         
                         if (displayName.isEmpty()) {
                             displayName = entity.getName().getString();
                         }
                         
                         if (displayName.contains("|||||")) {
                             String[] parts = displayName.split("\\|\\|\\|\\|\\|");
                             if (parts.length >= 2) {
                                 try {
                                     double currentHP = Double.parseDouble(parts[1]);
                                     
                                     // If HP dropped to 0, monster died - count it and remove from tracking
                                     if (currentHP <= 0) {
                                         MonsterInfo monsterInfo = trackedTextDisplays.remove(alreadyTrackedEntity);
                                         if (monsterInfo != null) {
                                             // INCREASE dead monster count
                                             int oldCount = currentWaveMonsters.getOrDefault(monsterInfo.entityType, 0);
                                             int newCount = oldCount + 1;
                                             currentWaveMonsters.put(monsterInfo.entityType, newCount);
                                             
                                             logDebug("Monster DIED (HP=0) - COUNTING: " + monsterInfo.entityType + " - " + displayName + 
                                                     " (dead count: " + oldCount + " -> " + newCount + ")");
                                             monstersDisappeared++;
                                         }
                                     } else if (currentHP > 0) {
                                         logDebug("Monster HP updated: " + entityType + " - " + displayName + " (HP: " + currentHP + ") - keeping tracked");
                                     }
                                 } catch (NumberFormatException e) {
                                     logDebug("Could not parse HP from display name: " + displayName + " - keeping tracked");
                                 }
                             }
                         }
                     } else {
                        // New monster to track
                        if (isTrackingMonsters) {
                            String entityType = getEntityTypeName(entity);
                            String displayName = "";
                            
                            if (entity.hasCustomName()) {
                                net.minecraft.text.Text customName = entity.getCustomName();
                                if (customName != null) {
                                    displayName = customName.getString().trim();
                                }
                            }
                            
                            if (displayName.isEmpty()) {
                                displayName = entity.getName().getString();
                            }
                            
                            // Track ALL new monsters that spawn in this wave (both alive and dead)
                            logDebug("Found new monster to track: " + entityType + " - " + displayName + 
                                    " at distance " + String.format("%.1f", monsterDistance) + 
                                    " blocks (ID: " + entity.getId() + ")");
                            
                            // Create monster info
                            MonsterInfo monsterInfo = new MonsterInfo(
                                entityType, 
                                displayName, 
                                entity instanceof LivingEntity ? ((LivingEntity) entity).getMaxHealth() : 0.0
                            );
                            
                            trackedTextDisplays.put(entity, monsterInfo);
                            
                            // Track ALL spawned monsters in this wave
                            int oldSpawnCount = spawnedMonsters.getOrDefault(entityType, 0);
                            spawnedMonsters.put(entityType, oldSpawnCount + 1);
                            
                            logDebug("New monster tracked: " + entityType + " - " + displayName + " (ID: " + entity.getId() + ") - SPAWN COUNT: " + oldSpawnCount + " -> " + (oldSpawnCount + 1));
                            newMonstersTracked++;
                        } else {
                            logDebug("Ignoring existing monster: " + getEntityTypeName(entity) + " - wave not started yet");
                        }
                    }
                }
            }
        }
        
        // Clean up any remaining invalid entities
        for (Entity textDisplayEntity : new HashSet<>(trackedTextDisplays.keySet())) {
            if (textDisplayEntity.isRemoved()) {
                MonsterInfo monsterInfo = trackedTextDisplays.remove(textDisplayEntity);
                if (monsterInfo != null) {
                    // Count this monster as dead when it disappears
                    int oldCount = currentWaveMonsters.getOrDefault(monsterInfo.entityType, 0);
                    int newCount = oldCount + 1;
                    currentWaveMonsters.put(monsterInfo.entityType, newCount);
                    
                    logDebug("Monster DISAPPEARED - COUNTING: " + monsterInfo.entityType + " - " + monsterInfo.displayName + 
                            " (dead count: " + oldCount + " -> " + newCount + ")");
                    monstersDisappeared++;
                }
            }
        }
        
        // Log summary if there were changes
        if (newMonstersTracked > 0 || monstersDisappeared > 0) {
            logDebug("Monster tracking update - New tracked: " + newMonstersTracked + ", Dead (counted): " + monstersDisappeared + 
                    ", Total tracked: " + trackedTextDisplays.size());
            
            // Log spawn vs death statistics
            logDebug("=== SPAWN vs DEATH STATISTICS ===");
            for (String monsterType : spawnedMonsters.keySet()) {
                int spawned = spawnedMonsters.get(monsterType);
                int dead = currentWaveMonsters.getOrDefault(monsterType, 0);
                logDebug(monsterType + ": Spawned=" + spawned + ", Dead=" + dead + ", Difference=" + (spawned - dead));
            }
            logDebug("================================");
        }
    }
    
    /**
     * Check if an entity is a valid mob that should be tracked
     */
    private static boolean isValidMonster(Entity entity) {
        if (entity == null || entity.isRemoved() || !(entity instanceof LivingEntity)) {
            return false;
        }
        
        // Exclude players (both client player and other players)
        if (entity instanceof net.minecraft.client.network.ClientPlayerEntity || 
            entity instanceof net.minecraft.entity.player.PlayerEntity) {
            return false;
        }
        
        // Exclude armor stands
        if (entity.getType() == EntityType.ARMOR_STAND) {
            return false;
        }
        
        // Track ALL mobs (hostile, passive, neutral, boss) except players and armor stands
        return true;
    }
    
    private static String getEntityTypeName(Entity entity) {
        // Hostile mobs
        if (entity.getType() == EntityType.SKELETON) return "Skelett";
        if (entity.getType() == EntityType.ZOMBIE) return "Zombie";
        if (entity.getType() == EntityType.SPIDER) return "Spinne";
        if (entity.getType() == EntityType.CREEPER) return "Creeper";
        if (entity.getType() == EntityType.ENDERMAN) return "Enderman";
        if (entity.getType() == EntityType.WITCH) return "Hexe";
        if (entity.getType() == EntityType.SLIME) return "Schleim";
        if (entity.getType() == EntityType.BLAZE) return "Lohe";
        if (entity.getType() == EntityType.GHAST) return "Ghast";
        if (entity.getType() == EntityType.MAGMA_CUBE) return "Magma Cube";
        if (entity.getType() == EntityType.SHULKER) return "Shulker";
        if (entity.getType() == EntityType.GUARDIAN) return "Wächter";
        if (entity.getType() == EntityType.ELDER_GUARDIAN) return "Elder Wächter";
        if (entity.getType() == EntityType.WITHER_SKELETON) return "Wither Skelett";
        if (entity.getType() == EntityType.STRAY) return "Stray";
        if (entity.getType() == EntityType.HUSK) return "Husk";
        if (entity.getType() == EntityType.DROWNED) return "Ertrunkener";
        if (entity.getType() == EntityType.PHANTOM) return "Phantom";
        if (entity.getType() == EntityType.VEX) return "Vex";
        if (entity.getType() == EntityType.EVOKER) return "Beschwörer";
        if (entity.getType() == EntityType.VINDICATOR) return "Vindicator";
        if (entity.getType() == EntityType.PILLAGER) return "Plünderer";
        if (entity.getType() == EntityType.RAVAGER) return "Verwüster";
        if (entity.getType() == EntityType.HOGLIN) return "Hoglin";
        if (entity.getType() == EntityType.ZOGLIN) return "Zoglin";
        if (entity.getType() == EntityType.PIGLIN) return "Piglin";
        if (entity.getType() == EntityType.PIGLIN_BRUTE) return "Piglin Brute";
        if (entity.getType() == EntityType.ZOMBIFIED_PIGLIN) return "Zombifizierter Piglin";
        if (entity.getType() == EntityType.SILVERFISH) return "Silberfisch";
        if (entity.getType() == EntityType.ENDERMITE) return "Endermite";
        if (entity.getType() == EntityType.CAVE_SPIDER) return "Höhlenspinne";
        if (entity.getType() == EntityType.ENDER_DRAGON) return "Ender Drache";
        if (entity.getType() == EntityType.WITHER) return "Wither";
        if (entity.getType() == EntityType.WARDEN) return "Warden";
        
        // Passive mobs
        if (entity.getType() == EntityType.HORSE) return "Pferd";
        if (entity.getType() == EntityType.COW) return "Kuh";
        if (entity.getType() == EntityType.PIG) return "Schwein";
        if (entity.getType() == EntityType.SHEEP) return "Schaf";
        if (entity.getType() == EntityType.CHICKEN) return "Huhn";
        if (entity.getType() == EntityType.BEE) return "Biene";
        if (entity.getType() == EntityType.VILLAGER) return "Dorfbewohner";
        if (entity.getType() == EntityType.IRON_GOLEM) return "Eisen-Golem";
        if (entity.getType() == EntityType.SNOW_GOLEM) return "Schnee-Golem";
        if (entity.getType() == EntityType.LLAMA) return "Llama";
        if (entity.getType() == EntityType.TRADER_LLAMA) return "Händler-Llama";
        if (entity.getType() == EntityType.WOLF) return "Wolf";
        if (entity.getType() == EntityType.CAT) return "Katze";
        if (entity.getType() == EntityType.OCELOT) return "Ozelot";
        if (entity.getType() == EntityType.FOX) return "Fuchs";
        if (entity.getType() == EntityType.RABBIT) return "Hase";
        if (entity.getType() == EntityType.POLAR_BEAR) return "Eisbär";
        if (entity.getType() == EntityType.PANDA) return "Panda";
        if (entity.getType() == EntityType.TURTLE) return "Schildkröte";
        if (entity.getType() == EntityType.DOLPHIN) return "Delfin";
        if (entity.getType() == EntityType.SQUID) return "Tintenfisch";
        if (entity.getType() == EntityType.GLOW_SQUID) return "Leucht-Tintenfisch";
        if (entity.getType() == EntityType.AXOLOTL) return "Axolotl";
        if (entity.getType() == EntityType.GOAT) return "Ziege";
        if (entity.getType() == EntityType.FROG) return "Frosch";
        if (entity.getType() == EntityType.TADPOLE) return "Kaulquappe";
        if (entity.getType() == EntityType.ALLAY) return "Allay";
        if (entity.getType() == EntityType.SNIFFER) return "Sniffer";
        if (entity.getType() == EntityType.CAMEL) return "Kamel";
        if (entity.getType() == EntityType.MOOSHROOM) return "Mooshroom";
        if (entity.getType() == EntityType.STRIDER) return "Strider";
        if (entity.getType() == EntityType.BAT) return "Fledermaus";
        if (entity.getType() == EntityType.PARROT) return "Papagei";
        if (entity.getType() == EntityType.OCELOT) return "Ozelot";
        if (entity.getType() == EntityType.MULE) return "Maultier";
        if (entity.getType() == EntityType.DONKEY) return "Esel";
        if (entity.getType() == EntityType.SKELETON_HORSE) return "Skelett-Pferd";
        if (entity.getType() == EntityType.ZOMBIE_HORSE) return "Zombie-Pferd";

        
        // Fallback to entity type name
        return entity.getType().getUntranslatedName();
    }
    
    private static void saveWaveData() {
        logDebug("saveWaveData() called - currentWave: '" + currentWave + "', currentWaveMonsters size: " + currentWaveMonsters.size());
        
        if (currentWave.isEmpty()) {
            logDebug("Cannot save wave data: currentWave is empty");
            return;
        }
        
        if (currentWaveMonsters.isEmpty()) {
            logDebug("Cannot save wave data: currentWaveMonsters is empty");
            return;
        }
        
        logDebug("Saving wave data for wave " + currentWave + " with " + currentWaveMonsters.size() + " mob types");
        
        // Log current monster data before saving
        for (Map.Entry<String, Integer> entry : currentWaveMonsters.entrySet()) {
            logDebug("  - " + entry.getKey() + ": " + entry.getValue());
        }
        
        // Save to wave data map
        waveData.put(currentWave, new HashMap<>(currentWaveMonsters));
        logDebug("Wave data saved to memory. Total waves in memory: " + waveData.size());
        
        // Write to file
        writeToLogFile();
    }
    
    private static void writeToLogFile() {
        try {
            File logFile = new File(LOG_FILE_PATH);
            logDebug("Attempting to write to file: " + logFile.getAbsolutePath());
            logDebug("File exists: " + logFile.exists());
            logDebug("Can write: " + logFile.canWrite());
            logDebug("Parent directory exists: " + (logFile.getParentFile() == null || logFile.getParentFile().exists()));
            
            // Ensure parent directory exists
            if (logFile.getParentFile() != null && !logFile.getParentFile().exists()) {
                boolean dirCreated = logFile.getParentFile().mkdirs();
                logDebug("Created parent directories: " + dirCreated);
            }
            
            PrintWriter writer = new PrintWriter(new FileWriter(logFile, false)); // false = overwrite
            
            logDebug("Writing " + waveData.size() + " waves to file");
            
            if (waveData.isEmpty()) {
                logDebug("No wave data to write - waveData is empty");
                writer.println("Keine Wellen-Daten verfügbar");
            } else {
                // Sort waves numerically (1, 2, 3, 10, 11, 20, 100, etc.)
                List<String> sortedWaves = new ArrayList<>(waveData.keySet());
                sortedWaves.sort((wave1, wave2) -> {
                    try {
                        // Try to parse as numbers for proper numerical sorting
                        int num1 = Integer.parseInt(wave1);
                        int num2 = Integer.parseInt(wave2);
                        return Integer.compare(num1, num2);
                    } catch (NumberFormatException e) {
                        // If not numbers, use string comparison as fallback
                        return wave1.compareTo(wave2);
                    }
                });
                
                logDebug("Writing " + sortedWaves.size() + " waves in sorted order: " + sortedWaves);
                
                // Write all wave data in sorted order
                for (String waveKey : sortedWaves) {
                    Map<String, Integer> monsters = waveData.get(waveKey);
                    logDebug("Writing wave: " + waveKey);
                    writer.println("Welle " + waveKey);
                    
                    int mobsWritten = 0;
                    for (Map.Entry<String, Integer> monsterEntry : monsters.entrySet()) {
                        if (monsterEntry.getValue() > 0) {
                            writer.println("-" + monsterEntry.getKey() + " x" + monsterEntry.getValue());
                            mobsWritten++;
                        }
                    }
                    logDebug("  - Wrote " + mobsWritten + " mob types for wave " + waveKey);
                    writer.println();
                }
            }
            
            writer.close();
            logDebug("File write completed successfully! File size: " + logFile.length() + " bytes");
            logDebug("File exists after write: " + logFile.exists());
        } catch (IOException e) {
            logError("IOException while writing to log file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            logError("Unexpected error while writing to log file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void resetTrackingData() {
        logDebug("Resetting all tracking data");
        currentWaveMonsters.clear();
        trackedTextDisplays.clear();
        currentWave = "";
        currentDimension = "";
        waveStartTime = 0;
    }
    
    private static void onHudRender(DrawContext context, RenderTickCounter tickDelta) {
        if (!showOverlays || !isTrackingMonsters) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
        
        // Calculate position based on configuration
        int x = screenWidth + CCLiveUtilitiesConfig.HANDLER.instance().wellenTrackerX;
        int y = CCLiveUtilitiesConfig.HANDLER.instance().wellenTrackerY;
        
        // Draw background if enabled
        if (CCLiveUtilitiesConfig.HANDLER.instance().wellenTrackerShowBackground) {
            context.fill(x, y, x + OVERLAY_WIDTH, y + OVERLAY_HEIGHT, 0x80000000);
        }
        
        // Draw monster tracking information
        drawMonsterInfo(context, x + TEXT_PADDING, y + TEXT_PADDING, client);
    }
    
    private static void drawMonsterInfo(DrawContext context, int x, int y, MinecraftClient client) {
        // Draw header
        Text headerText = Text.literal("Monster Tracker");
        context.drawText(client.textRenderer, headerText, x, y, 
            CCLiveUtilitiesConfig.HANDLER.instance().wellenTrackerHeaderColor.getRGB(), true);
        y += LINE_HEIGHT;
        
        // Draw current wave
        if (!currentWave.isEmpty()) {
            Text waveText = Text.literal("Welle: " + currentWave);
            context.drawText(client.textRenderer, waveText, x, y, 
                CCLiveUtilitiesConfig.HANDLER.instance().wellenTrackerTextColor.getRGB(), true);
            y += LINE_HEIGHT;
        }
        
        // Draw current monster counts
        if (!currentWaveMonsters.isEmpty()) {
            Text statsText = Text.literal("Aktuelle Monster:");
            context.drawText(client.textRenderer, statsText, x, y, 
                CCLiveUtilitiesConfig.HANDLER.instance().wellenTrackerTextColor.getRGB(), true);
            y += LINE_HEIGHT;
            
            for (Map.Entry<String, Integer> entry : currentWaveMonsters.entrySet()) {
                if (entry.getValue() > 0) {
                    Text entryText = Text.literal(entry.getKey() + ": " + entry.getValue());
                    context.drawText(client.textRenderer, entryText, x, y, 
                        CCLiveUtilitiesConfig.HANDLER.instance().wellenTrackerTextColor.getRGB(), true);
                    y += LINE_HEIGHT;
                }
            }
        }
    }
    
    // ===== LOGGING METHODS =====
    
    private static void logDebug(String message) {
        if (!ENABLE_DEBUG_LOGGING) return;
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        String logMessage = "[" + timestamp + "] [DEBUG] " + message;
        
        // Console logging
        if (ENABLE_CONSOLE_LOGGING) {
            System.out.println(logMessage);
        }
        
        // File logging
        writeToDebugLog(logMessage);
    }
    
    private static void logError(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        String logMessage = "[" + timestamp + "] [ERROR] " + message;
        
        // Console logging
        if (ENABLE_CONSOLE_LOGGING) {
            System.err.println(logMessage);
        }
        
        // File logging
        writeToDebugLog(logMessage);
    }
    
    private static void writeToDebugLog(String message) {
        try {
            File debugFile = new File(DEBUG_LOG_FILE_PATH);
            PrintWriter writer = new PrintWriter(new FileWriter(debugFile, true)); // true = append
            
            writer.println(message);
            writer.close();
        } catch (IOException e) {
            // Silent error handling for debug logging
        }
    }
    
    // ===== PUBLIC METHODS =====
    
    public static boolean isTrackingMonsters() {
        return isTrackingMonsters;
    }
    
    public static String getCurrentWave() {
        return currentWave;
    }
    
    public static Map<String, Integer> getCurrentWaveMonsters() {
        return new HashMap<>(currentWaveMonsters);
    }
    
    public static Map<String, Map<String, Integer>> getWaveData() {
        return new HashMap<>(waveData);
    }
    

    
    // ===== DEBUG METHODS =====
    
    public static void printDebugInfo() {
        logDebug("=== DEBUG INFO ===");
        logDebug("Initialized: " + isInitialized);
        logDebug("Tracking Monsters: " + isTrackingMonsters);
        logDebug("Show Overlays: " + showOverlays);
        logDebug("Current Wave: '" + currentWave + "'");
        logDebug("Current Dimension: '" + currentDimension + "'");
        logDebug("Player Name: '" + playerName + "'");
        logDebug("Tracked Text Displays: " + trackedTextDisplays.size());
        logDebug("Current Wave Monsters: " + currentWaveMonsters.size());
        logDebug("Total Wave Data: " + waveData.size());
        
        // Check file system
        File logFile = new File(LOG_FILE_PATH);
        logDebug("Log file path: " + logFile.getAbsolutePath());
        logDebug("Log file exists: " + logFile.exists());
        if (logFile.exists()) {
            logDebug("Log file size: " + logFile.length() + " bytes");
            logDebug("Log file last modified: " + new java.util.Date(logFile.lastModified()));
        }
        
        File debugLogFile = new File(DEBUG_LOG_FILE_PATH);
        logDebug("Debug log file path: " + debugLogFile.getAbsolutePath());
        logDebug("Debug log file exists: " + debugLogFile.exists());
        if (debugLogFile.exists()) {
            logDebug("Debug log file size: " + debugLogFile.length() + " bytes");
        }
        
        if (!currentWaveMonsters.isEmpty()) {
            logDebug("Current Mob Counts:");
            for (Map.Entry<String, Integer> entry : currentWaveMonsters.entrySet()) {
                logDebug("  " + entry.getKey() + ": " + entry.getValue());
            }
        }
        
        if (!waveData.isEmpty()) {
            logDebug("Stored Wave Data:");
            for (Map.Entry<String, Map<String, Integer>> waveEntry : waveData.entrySet()) {
                logDebug("  Welle " + waveEntry.getKey() + ": " + waveEntry.getValue().size() + " mob types");
            }
        }
        logDebug("=== END DEBUG INFO ===");
    }
    
    /**
     * Test method to manually create log file with test data
     */
    public static void testFileCreation() {
        logDebug("=== TESTING FILE CREATION ===");
        
        // Create test data
        Map<String, Integer> testMobs = new HashMap<>();
        testMobs.put("Test-Skelett", 5);
        testMobs.put("Test-Zombie", 3);
        testMobs.put("Test-Pferd", 2);
        
        // Save test data
        currentWave = "TEST";
        currentWaveMonsters.clear();
        currentWaveMonsters.putAll(testMobs);
        
        logDebug("Created test data for wave: " + currentWave);
        logDebug("Test mobs: " + currentWaveMonsters.size());
        
        // Try to save
        saveWaveData();
        
        logDebug("=== FILE CREATION TEST COMPLETED ===");
    }
    
    /**
     * Debug method to check all scoreboard information
     */
    public static void debugScoreboard(MinecraftClient client) {
        logDebug("=== SCOREBOARD DEBUG ===");
        logDebug("Scoreboard debugging removed - use hotkey to toggle tracking instead");
        logDebug("=== END SCOREBOARD DEBUG ===");
    }
}

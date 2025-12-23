package net.felix.mixin;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.felix.utilities.Aincraft.KillsUtility;
import net.felix.utilities.Factory.WaveUtility;
import net.felix.leaderboards.collectors.FarmworldCollectionsCollector;
import net.felix.utilities.Overall.ZeichenUtility;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class BossBarMixin {
    
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderBossBar(DrawContext context, CallbackInfo ci) {
        try {
            // Get the bossbars from the BossBarHud
            BossBarHud bossBarHud = (BossBarHud) (Object) this;
            
            // Try different field names for bossbars - updated for 1.21.7
            Map<UUID, ClientBossBar> bossBars = null;
            String[] possibleFieldNames = {
                "field_2060", "field_2061", "field_2062", // Common obfuscated field names
                "bossBars", "bossbars", "bossBars", "bars", "bossBarMap",
                "clientBossBars", "bossBarEntries", "entries", "bossBarList"
            };
            
            // First try to find the field by type
            java.lang.reflect.Field[] fields = bossBarHud.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        Object fieldValue = field.get(bossBarHud);
                        if (fieldValue instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<UUID, ClientBossBar> tempBars = (Map<UUID, ClientBossBar>) fieldValue;
                            if (tempBars != null && !tempBars.isEmpty()) {
                                bossBars = tempBars;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Continue trying other fields
                    }
                }
            }
            
            // If we didn't find it by type, try the known field names
            if (bossBars == null) {
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
                        // Silent error handling
                    }
                }
            }
            
            if (bossBars != null) {
                int index = 0;
                for (ClientBossBar bossBar : bossBars.values()) {
                    index++;
                    String name = bossBar.getName().getString();
                    
                    // Fabrik-Wellen (2. Bossbar) verarbeiten
                    WaveUtility.processBossBarWave(name, index);
                    
                    // Look for bossbar that contains kill information (Floors)
                    // Usually it's the top bossbar that shows kills
                    // Use the same Chinese characters as in KillsUtility
                    if (name.contains("Kills") || name.contains("Kill") || 
                        name.matches(".*[" + ZeichenUtility.getAincraftBottomFont() + "].*")) {
                        
                        // Process the kill information
                        KillsUtility.processBossBarKills(name);
                        // Don't break here - continue checking for collection bossbars
                    }
                    
                    // Look for bossbar that contains collection information (Farmworld)
                    // Collection bossbars also contain Chinese numbers
                    // Check if it's not a kills bossbar but contains Chinese numbers
                    if (!name.contains("Kills") && !name.contains("Kill") && 
                        name.matches(".*[㚏㚐㚑㚒㚓㚔㚕㚖㚗㚘].*")) {
                        
                        // Process the collection information
                        FarmworldCollectionsCollector.processBossBarCollection(name);
                    }
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
    }
} 
package net.felix.mixin;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.felix.utilities.KillsUtility;

import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class BossBarMixin {
    
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderBossBar(DrawContext context, CallbackInfo ci) {
        try {
            // Get the bossbars from the BossBarHud
            BossBarHud bossBarHud = (BossBarHud) (Object) this;
            
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
                    // Silent error handling
                }
            }
            
            if (bossBars != null) {
                for (ClientBossBar bossBar : bossBars.values()) {
                    String name = bossBar.getName().getString();
                    
                    // Look for bossbar that contains kill information
                    // Usually it's the top bossbar that shows kills
                    if (name.contains("Kills") || name.contains("Kill") || 
                        name.matches(".*[㚯㚰㚱㚲㚳㚴㚵㚶㚷㚸].*")) {
                        
                        // Process the kill information
                        KillsUtility.processBossBarKills(name);
                        break; // Found the kills bossbar, no need to check others
                    }
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
    }
} 
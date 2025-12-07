package net.felix.mixin;

import net.felix.utilities.Other.PlayericonUtility.PlayerIconUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Mixin to add the CCLive-Utilities icon next to player names in the tab list.
 */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    
    @Shadow
    @Final
    private MinecraftClient client;
    
    
    @Inject(
        method = "renderLatencyIcon",
        at = @At("RETURN")
    )
    private void renderIconAfterLatency(
        DrawContext context,
        int width,
        int x,
        int y,
        PlayerListEntry entry,
        CallbackInfo ci
    ) {
        try {
            if (entry == null || client == null || client.player == null) {
                return;
            }
            
            if (entry.getProfile() == null) {
                return;
            }
            
            UUID playerUuid = entry.getProfile().getId();
            
            // Only show icon for players who have the mod installed
            boolean shouldShowIcon = PlayerIconUtility.hasMod(playerUuid);
            
            if (shouldShowIcon) {
                // Render icon to the left of the latency icon
                // Position it slightly higher for better alignment
                int iconX = x - PlayerIconUtility.getDefaultIconWidth() - 4;
                int iconY = y - 1; // Move up by 1 pixel for better vertical centering
                // Use slightly larger size for better sharpness
                PlayerIconUtility.renderIcon(context, iconX, iconY, 10);
            }
        } catch (Exception e) {
            // Silently fail if there's any error
            // Ignore errors
        }
    }
}


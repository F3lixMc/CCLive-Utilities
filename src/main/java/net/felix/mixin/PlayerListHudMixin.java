package net.felix.mixin;

import net.felix.utilities.Other.PlayericonUtility.PlayerIconUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
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
    
    /**
     * Render icon after the player name text (right side).
     * This uses the renderLatencyIcon method as a hook point and calculates position from name width.
     */
    @Inject(
        method = "renderLatencyIcon",
        at = @At("RETURN")
    )
    private void renderIconAfterPlayerName(
        DrawContext context,
        int width,
        int x,
        int y,
        PlayerListEntry entry,
        CallbackInfo ci
    ) {
        try {
            if (entry == null || client == null || client.player == null || client.textRenderer == null) {
                return;
            }
            
            if (entry.getProfile() == null) {
                return;
            }
            
            UUID playerUuid = entry.getProfile().getId();
            
            // Only show icon for players who have the mod installed
            boolean shouldShowIcon = PlayerIconUtility.hasMod(playerUuid);
            
            if (shouldShowIcon) {
                // Get player display name to calculate text width
                Text playerName = entry.getDisplayName();
                int nameWidth = client.textRenderer.getWidth(playerName);
                
                // In the tab list, the name is typically rendered at a fixed x position
                // The x parameter passed to renderLatencyIcon is where the latency icon is rendered
                // The name is rendered before the latency icon, so we need to calculate backwards
                // Tab list names usually start around x=2-4 pixels from the left edge
                // We'll use a simpler approach: place icon right after the name width
                // The name typically starts at x=2, so name ends at x=2+nameWidth
                int nameStartX = 2; // Typical start position for tab list names
                int nameEndX = nameStartX + nameWidth;
                
                // Position icon to the right of the player name
                int iconX = nameEndX + 2; // 2 pixels spacing after name
                int iconY = y; // Same vertical position as text
                
                // Use slightly larger size for better visibility
                PlayerIconUtility.renderIcon(context, iconX, iconY, 8);
            }
        } catch (Exception e) {
            // Silently fail if there's any error
        }
    }
}


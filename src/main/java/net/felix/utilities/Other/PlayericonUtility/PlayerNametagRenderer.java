package net.felix.utilities.Other.PlayericonUtility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Renders the CCLive-Utilities icon above player heads (nametags).
 * NOTE: WorldRenderEvents is no longer available in Fabric API 0.138.4+1.21.10.
 * Nametag icon rendering is currently disabled.
 */
public class PlayerNametagRenderer {
    
    private static boolean initialized = false;
    
    /**
     * Initialize the nametag renderer.
     * WorldRenderEvents was removed in Fabric API 0.138.4+1.21.10.
     * Rendering above player heads is temporarily disabled.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
    }
    
    /**
     * Render icon above player head.
     */
    private static void renderNametagIcon(PlayerEntity player) {
        // WorldRenderEvents removed in Fabric API 0.138.4+1.21.10
        // 3D nametag icon rendering is currently disabled
    }
}
























package net.felix.utilities.Other.PlayericonUtility;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.felix.CCLiveUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Renders the CCLive-Utilities icon above player heads (nametags).
 */
public class PlayerNametagRenderer {
    
    private static boolean initialized = false;
    
    /**
     * Initialize the nametag renderer.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        // Register world render event to render icons above player heads
        LevelRenderEvents.END_MAIN.register(context -> {
            try {
                Minecraft client = Minecraft.getInstance();
                if (client == null || client.level == null || client.player == null) {
                    return;
                }
                
                // Additional safety checks
                if (client.font == null || context == null || context.gameRenderer().getMainCamera() == null) {
                    return;
                }
                
                // Render icons for all players
                for (var entity : client.level.entitiesForRendering()) {
                    try {
                        if (!(entity instanceof Player)) {
                            continue;
                        }
                        
                        Player player = (Player) entity;
                        
                        // Skip local player's own rendering to avoid issues
                        if (player.equals(client.player)) {
                            continue;
                        }
                        
                        // Only render if player is visible and within distance
                        if (player.isInvisible() || !client.player.hasLineOfSight(player)) {
                            continue;
                        }
                        
                        // Prüfe ob Nametag-Icons in der Config aktiviert sind
                        if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showPlayerNametagIcon) {
                            continue;
                        }
                        
                        // Only show icon for players who have the mod installed
                        boolean shouldShowIcon = PlayerIconUtility.hasMod(player.getUUID());
                        
                        if (!shouldShowIcon) {
                            continue;
                        }
                        
                        // Render icon above player head
                        renderNametagIcon(context, player);
                    } catch (Exception e) {
                        // Silently skip this player if there's an error
                        continue;
                    }
                }
            } catch (Exception e) {
                // Silently fail
            }
        });
        
        initialized = true;
    }
    
    /**
     * Render icon above player head.
     */
    private static void renderNametagIcon(LevelRenderContext context, Player player) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.font == null) {
                return;
            }
            
            // Hide icon if F1 is pressed (HUD hidden)
            if (client.options.hideGui) {
                return;
            }
            
            // Additional safety checks
            if (context == null || context.gameRenderer().getMainCamera() == null || context.poseStack() == null || context.bufferSource() == null) {
                return;
            }
            
            // Get player position
            Vec3 cameraPos = context.gameRenderer().getMainCamera().position();
            Vec3 playerPos = player.position();
            
            // Calculate offset above player head
            double offsetY = player.getBbHeight() + 0.5;
            double x = playerPos.x;
            double y = playerPos.y + offsetY;
            double z = playerPos.z;
            
            // Calculate distance
            double dx = x - cameraPos.x;
            double dy = y - cameraPos.y;
            double dz = z - cameraPos.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            
            // Don't render if too far away or too close (avoid division by zero)
            if (distance > 64.0 || distance < 0.1) {
                return;
            }
            
            // Get the matrix stack
            PoseStack matrices = context.poseStack();
            if (matrices == null) {
                return;
            }
            
            // Push matrix
            matrices.pushPose();
            
            try {
                // Translate to player position (same as nametag)
                matrices.translate((float)dx, (float)dy, (float)dz);
                
                // Billboard rotation - face camera exactly like nametags do
                // Get camera rotation
                float cameraYaw = context.gameRenderer().getMainCamera().yRot();
                float cameraPitch = context.gameRenderer().getMainCamera().xRot();
                
                // Apply billboard rotation (same as EntityRenderer.renderLabelIfPresent)
                matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-cameraYaw));
                matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(cameraPitch));
                
                // Scale based on distance (same scaling as nametags)
                // Nametags use a scale factor that makes them appear consistent size
                float baseScale = 0.025f; // Base scale for nametags
                matrices.scale(-baseScale, -baseScale, baseScale); // Negative Y for correct orientation
                
                // Get player display name to calculate text width
                net.minecraft.network.chat.Component playerName = player.getDisplayName();
                int nameWidth = client.font.width(playerName);
                
                // Icon size: approximately 8 pixels in world space
                // Increase size slightly to keep it sharper at distance
                // This will be scaled by the baseScale above
                float iconSize = 10.0f; // Slightly larger for better visibility at distance
                
                // Position icon to the right of the nametag, vertically centered
                // Format: <rang> <spielername> <icon>
                // The nametag bar is centered at y=0 in the coordinate space
                // With negative Y scale, positive Y values move down
                float spacing = 6.0f; // Space between nametag and icon
                float iconOffsetX = nameWidth / 2.0f + spacing; // Right side of nametag
                // Center icon vertically in the nametag bar
                // With negative Y scale, we need a positive offset to move down
                // Fine-tune the offset to center the icon properly in the nametag bar
                float iconOffsetY = 3.70f; // Offset down to center in nametag bar
                
                matrices.translate(iconOffsetX, iconOffsetY, 0.0f);
                
                // Render icon as texture
                // Use full brightness for nametag icons (same as text rendering)
                int lightLevel = 15728880; // Full brightness
                renderIconTexture(matrices, context.bufferSource(), iconSize, lightLevel, context);
            } finally {
                // Always pop the matrix, even if there was an error
                matrices.popPose();
            }
        } catch (Exception e) {
            // Silently fail
        }
    }
    
    /**
     * Render the icon as a texture in 3D space.
     */
    private static void renderIconTexture(PoseStack matrices, MultiBufferSource vertexConsumers, float size, int light, LevelRenderContext context) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.getTextureManager() == null) {
                return;
            }
            
            // Get icon identifier - use the same as PlayerIconUtility
            Identifier iconId = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/icon.png");
            Identifier iconIdAlt = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "icon");
            
            // Try to get the texture - verify it exists
            Identifier iconToUse = iconId;
            try {
                if (client.getResourceManager() != null) {
                    var resource = client.getResourceManager().getResource(iconId);
                    if (resource.isEmpty()) {
                        // Try alternative location
                        var resourceAlt = client.getResourceManager().getResource(iconIdAlt);
                        if (resourceAlt.isPresent()) {
                            iconToUse = iconIdAlt;
                        } else {
                            // If neither exists, use a fallback or return
                            return;
                        }
                    }
                } else {
                    // Fallback to alternative if resource manager not available
                    iconToUse = iconIdAlt;
                }
            } catch (Exception e) {
                // Try alternative on error
                iconToUse = iconIdAlt;
            }
            
            // Get the matrix
            Matrix4f matrix = matrices.last().pose();
            
            // Create vertex consumer with the correct render layer for text rendering
            // Use the same render layer as text (RenderLayer.getText) for proper blending
            RenderType renderLayer = RenderTypes.text(iconToUse);
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
            
            // Ensure texture is bound (should be handled by RenderLayer, but just in case)
            if (client.getTextureManager() != null) {
                try {
                    // The texture should be automatically bound by the RenderLayer
                    // But we can verify it exists
                    var texture = client.getTextureManager().getTexture(iconToUse);
                    if (texture == null) {
                        // Texture not loaded, try to load it
                        return; // Skip rendering if texture not available
                    }
                } catch (Exception e) {
                    // Texture might not be loaded yet, that's okay
                }
            }
            
            // Calculate half size for centering
            float halfSize = size / 2.0f;
            
            // Render a quad with the icon texture
            // Using the correct vertex format for Minecraft 1.21.7
            // Since we use negative Y scale, we need to flip the texture coordinates
            // Top-left (will be bottom-left after negative Y scale)
            vertexConsumer.addVertex(matrix, -halfSize, halfSize, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(0.0f, 1.0f) // Flipped Y coordinate
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0f, 0.0f, 1.0f);
            
            // Top-right (will be bottom-right after negative Y scale)
            vertexConsumer.addVertex(matrix, halfSize, halfSize, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(1.0f, 1.0f) // Flipped Y coordinate
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0f, 0.0f, 1.0f);
            
            // Bottom-right (will be top-right after negative Y scale)
            vertexConsumer.addVertex(matrix, halfSize, -halfSize, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(1.0f, 0.0f) // Flipped Y coordinate
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0f, 0.0f, 1.0f);
            
            // Bottom-left (will be top-left after negative Y scale)
            vertexConsumer.addVertex(matrix, -halfSize, -halfSize, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(0.0f, 0.0f) // Flipped Y coordinate
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0f, 0.0f, 1.0f);
        } catch (Exception e) {
            // Silently fail if texture rendering fails
        }
    }
}
























package net.felix.utilities.Other.PlayericonUtility;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.felix.CCLiveUtilities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
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
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null || client.world == null || client.player == null) {
                    return;
                }
                
                // Additional safety checks
                if (client.textRenderer == null || context == null || context.camera() == null) {
                    return;
                }
                
                // Render icons for all players
                for (var entity : client.world.getEntities()) {
                    try {
                        if (!(entity instanceof PlayerEntity)) {
                            continue;
                        }
                        
                        PlayerEntity player = (PlayerEntity) entity;
                        
                        // Skip local player's own rendering to avoid issues
                        if (player.equals(client.player)) {
                            continue;
                        }
                        
                        // Only render if player is visible and within distance
                        if (player.isInvisible() || !client.player.canSee(player)) {
                            continue;
                        }
                        
                        // Only show icon for players who have the mod installed
                        boolean shouldShowIcon = PlayerIconUtility.hasMod(player.getUuid());
                        
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
    private static void renderNametagIcon(WorldRenderContext context, PlayerEntity player) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.textRenderer == null) {
                return;
            }
            
            // Additional safety checks
            if (context == null || context.camera() == null || context.matrixStack() == null || context.consumers() == null) {
                return;
            }
            
            // Get player position
            Vec3d cameraPos = context.camera().getPos();
            Vec3d playerPos = player.getPos();
            
            // Calculate offset above player head
            double offsetY = player.getHeight() + 0.5;
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
            MatrixStack matrices = context.matrixStack();
            if (matrices == null) {
                return;
            }
            
            // Push matrix
            matrices.push();
            
            try {
                // Translate to player position (same as nametag)
                matrices.translate((float)dx, (float)dy, (float)dz);
                
                // Billboard rotation - face camera exactly like nametags do
                // Get camera rotation
                float cameraYaw = context.camera().getYaw();
                float cameraPitch = context.camera().getPitch();
                
                // Apply billboard rotation (same as EntityRenderer.renderLabelIfPresent)
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-cameraYaw));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(cameraPitch));
                
                // Scale based on distance (same scaling as nametags)
                // Nametags use a scale factor that makes them appear consistent size
                float baseScale = 0.025f; // Base scale for nametags
                matrices.scale(-baseScale, -baseScale, baseScale); // Negative Y for correct orientation
                
                // Get player display name to calculate text width
                net.minecraft.text.Text playerName = player.getDisplayName();
                int nameWidth = client.textRenderer.getWidth(playerName);
                
                // Icon size: approximately 8 pixels in world space
                // Increase size slightly to keep it sharper at distance
                // This will be scaled by the baseScale above
                float iconSize = 10.0f; // Slightly larger for better visibility at distance
                
                // Position icon to the left of the nametag, vertically centered
                // Like LabyMod: icon should be left of the name, centered in the nametag bar
                // The nametag bar is centered at y=0 in the coordinate space
                // With negative Y scale, positive Y values move down
                float spacing = 0.0f; // Space between icon and nametag (like LabyMod)
                float iconOffsetX = -nameWidth / 2.0f - iconSize - spacing;
                // Center icon vertically in the nametag bar
                // With negative Y scale, we need a positive offset to move down
                // Fine-tune the offset to center the icon properly in the nametag bar
                float iconOffsetY = 3.70f; // Offset down to center in nametag bar
                
                matrices.translate(iconOffsetX, iconOffsetY, 0.0f);
                
                // Render icon as texture
                // Use full brightness for nametag icons (same as text rendering)
                int lightLevel = 15728880; // Full brightness
                renderIconTexture(matrices, context.consumers(), iconSize, lightLevel, context);
            } finally {
                // Always pop the matrix, even if there was an error
                matrices.pop();
            }
        } catch (Exception e) {
            // Silently fail
        }
    }
    
    /**
     * Render the icon as a texture in 3D space.
     */
    private static void renderIconTexture(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float size, int light, WorldRenderContext context) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getTextureManager() == null) {
                return;
            }
            
            // Get icon identifier - use the same as PlayerIconUtility
            Identifier iconId = Identifier.of(CCLiveUtilities.MOD_ID, "textures/icon.png");
            Identifier iconIdAlt = Identifier.of(CCLiveUtilities.MOD_ID, "icon");
            
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
                            System.err.println("[CCLive-Utilities] Icon texture not found: " + iconId + " or " + iconIdAlt);
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
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            
            // Create vertex consumer with the correct render layer for text rendering
            // Use the same render layer as text (RenderLayer.getText) for proper blending
            RenderLayer renderLayer = RenderLayer.getText(iconToUse);
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
            vertexConsumer.vertex(matrix, -halfSize, halfSize, 0.0f)
                .color(255, 255, 255, 255)
                .texture(0.0f, 1.0f) // Flipped Y coordinate
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0.0f, 0.0f, 1.0f);
            
            // Top-right (will be bottom-right after negative Y scale)
            vertexConsumer.vertex(matrix, halfSize, halfSize, 0.0f)
                .color(255, 255, 255, 255)
                .texture(1.0f, 1.0f) // Flipped Y coordinate
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0.0f, 0.0f, 1.0f);
            
            // Bottom-right (will be top-right after negative Y scale)
            vertexConsumer.vertex(matrix, halfSize, -halfSize, 0.0f)
                .color(255, 255, 255, 255)
                .texture(1.0f, 0.0f) // Flipped Y coordinate
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0.0f, 0.0f, 1.0f);
            
            // Bottom-left (will be top-left after negative Y scale)
            vertexConsumer.vertex(matrix, -halfSize, -halfSize, 0.0f)
                .color(255, 255, 255, 255)
                .texture(0.0f, 0.0f) // Flipped Y coordinate
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(0.0f, 0.0f, 1.0f);
        } catch (Exception e) {
            // Silently fail if texture rendering fails
        }
    }
}









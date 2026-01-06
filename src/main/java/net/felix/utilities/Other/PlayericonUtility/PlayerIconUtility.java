package net.felix.utilities.Other.PlayericonUtility;

import net.felix.CCLiveUtilities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class for managing and rendering the CCLive-Utilities icon next to player names.
 * Similar to how LabyMod displays icons for players using the mod.
 */
public class PlayerIconUtility {
    // Try both possible locations for the icon
    private static final Identifier ICON_IDENTIFIER = Identifier.of(CCLiveUtilities.MOD_ID, "textures/icon.png");
    private static final Identifier ICON_IDENTIFIER_ALT = Identifier.of(CCLiveUtilities.MOD_ID, "icon");
    private static boolean iconLoaded = false;
    
    // Set of player UUIDs that have the mod installed
    private static final Set<UUID> playersWithMod = new HashSet<>();
    
    /**
     * Initialize the icon utility by loading the icon texture.
     */
    public static void initialize() {
        loadIcon();
        // Initialize nametag rendering
        try {
            PlayerNametagRenderer.initialize();
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    /**
     * Load the icon from resources.
     */
    private static void loadIcon() {
        if (iconLoaded) {
            return;
        }
        
        // The icon is loaded automatically by Minecraft's resource system
        // We just need to verify it exists - but only when client is ready
        // Don't try to load during mod initialization
        iconLoaded = true; // Mark as loaded, actual verification happens on first render
    }
    
    /**
     * Ensure the icon is loaded. Call this before rendering.
     */
    private static void ensureIconLoaded() {
        if (!iconLoaded) {
            loadIcon();
        }
    }
    
    /**
     * Check if a player has the mod installed.
     */
    public static boolean hasMod(UUID playerUuid) {
        return playersWithMod.contains(playerUuid);
    }
    
    /**
     * Mark a player as having the mod installed.
     */
    public static void addPlayerWithMod(UUID playerUuid) {
        playersWithMod.add(playerUuid);
    }
    
    /**
     * Remove a player from the mod list (e.g., when they disconnect).
     */
    public static void removePlayer(UUID playerUuid) {
        playersWithMod.remove(playerUuid);
    }
    
    /**
     * Clear all players from the mod list.
     */
    public static void clearPlayers() {
        playersWithMod.clear();
    }
    
    /**
     * Render the icon next to a player name.
     * @param context The draw context
     * @param x The x position (will be adjusted to place icon before text)
     * @param y The y position
     * @param size The size of the icon (both width and height)
     */
    public static void renderIcon(DrawContext context, int x, int y, int size) {
        try {
            ensureIconLoaded();
            
            if (!iconLoaded) {
                return;
            }
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || context == null) {
                return;
            }
            
            // Try the primary icon location first, fallback to alternative
            Identifier iconToUse = ICON_IDENTIFIER;
            try {
                if (client.getResourceManager() != null) {
                    var resource = client.getResourceManager().getResource(ICON_IDENTIFIER);
                    if (resource.isEmpty()) {
                        iconToUse = ICON_IDENTIFIER_ALT;
                    }
                }
            } catch (Exception e) {
                iconToUse = ICON_IDENTIFIER_ALT;
            }
            
            // Draw the icon using the correct drawTexture signature
            // Wrap in try-catch to prevent crashes if RenderPipelines is not available
            try {
                context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    iconToUse,
                    x, y,
                    0.0f, 0.0f,
                    size, size,
                    size, size
                );
            } catch (NoClassDefFoundError | NoSuchFieldError e) {
                // RenderPipelines might not be available in some versions
                // Try alternative method without RenderPipelines
                try {
                    // Fallback: Use drawTexturedQuad if available
                    java.lang.reflect.Method drawTexturedQuadMethod = context.getClass().getMethod(
                        "drawTexturedQuad",
                        Identifier.class, int.class, int.class, int.class, int.class, 
                        float.class, float.class, float.class, float.class
                    );
                    drawTexturedQuadMethod.invoke(context, iconToUse, x, y, x + size, y + size, 0.0f, 1.0f, 0.0f, 1.0f);
                } catch (Exception e2) {
                    // Both methods failed, silently fail
                }
            } catch (Exception e) {
                // Other exceptions, silently fail
            }
        } catch (Exception e) {
            // Silently fail if texture is not available or any other error occurs
        }
    }
    
    /**
     * Render the icon next to a player name with default size (8 pixels).
     */
    public static void renderIcon(DrawContext context, int x, int y) {
        renderIcon(context, x, y, 8);
    }
    
    /**
     * Get the width of the icon (for spacing calculations).
     */
    public static int getIconWidth(int size) {
        return size + 2; // Icon width + spacing
    }
    
    /**
     * Get the default icon width.
     */
    public static int getDefaultIconWidth() {
        return getIconWidth(8);
    }
    
    /**
     * Initialize world rendering for nametag icons.
     * This sets up the WorldRenderEvents callback to render icons above player heads.
     */
    public static void initializeWorldRendering() {
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.world == null || client.player == null) {
                return;
            }
            
            // Render icons above player heads
            for (var entity : client.world.getEntities()) {
                if (!(entity instanceof net.minecraft.entity.player.PlayerEntity)) {
                    continue;
                }
                
                net.minecraft.entity.player.PlayerEntity player = (net.minecraft.entity.player.PlayerEntity) entity;
                UUID playerUuid = player.getUuid();
                
                // Check if player has the mod
                if (!hasMod(playerUuid)) {
                    continue;
                }
                
                // Only render if player is visible and within render distance
                if (!player.isInvisible() && client.player.canSee(player)) {
                    renderNametagIcon(context, player);
                }
            }
        });
    }
    
    /**
     * Render icon above player head (nametag).
     * This is a simplified 3D rendering approach.
     */
    private static void renderNametagIcon(
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context,
        net.minecraft.entity.player.PlayerEntity player
    ) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.textRenderer == null) {
                return;
            }
            
            // Get player position
            double x = player.getX();
            double y = player.getY() + player.getHeight() + 0.5; // Above head
            double z = player.getZ();
            
            // Calculate camera position
            net.minecraft.util.math.Vec3d cameraPos = context.camera().getPos();
            double dx = x - cameraPos.x;
            double dy = y - cameraPos.y;
            double dz = z - cameraPos.z;
            
            // Calculate distance
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > 64.0) {
                return; // Too far away
            }
            
            // Get the matrix stack
            net.minecraft.client.util.math.MatrixStack matrices = context.matrixStack();
            
            // Push matrix
            matrices.push();
            
            // Translate to player position
            matrices.translate((float)dx, (float)dy, (float)dz);
            
            // Face camera (billboard)
            matrices.multiplyPositionMatrix(new org.joml.Matrix4f().rotationY(
                (float)(-Math.atan2(dx, dz))
            ));
            matrices.multiplyPositionMatrix(new org.joml.Matrix4f().rotationX(
                (float)(Math.asin(dy / distance))
            ));
            
            // Scale based on distance (smaller when farther)
            float scale = (float)(0.02 * (1.0 - distance / 64.0) + 0.01);
            matrices.scale(scale, scale, scale);
            
            // Render icon
            // Note: This is a simplified approach. For proper 3D rendering,
            // you'd need to use the texture manager and proper vertex consumers.
            // For now, we'll skip 3D rendering and focus on 2D (tab list and chat).
            
            matrices.pop();
        } catch (Exception e) {
            // Silently fail if rendering fails
        }
    }
}
























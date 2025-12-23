package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.KillsUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

/**
 * Draggable Overlay für den Kills Utility
 */
public class KillsUtilityDraggableOverlay implements DraggableOverlay {
    
    private static final int MIN_OVERLAY_WIDTH = 65; // Fallback width if KillsUtility is not available
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 5;
    
    @Override
    public String getOverlayName() {
        return "Kills Utility";
    }
    
    @Override
    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityX;
        
        // Use unscaled width for positioning (same as KillsUtility line 557)
        // getCurrentOverlayWidth() returns unscaled width
        int unscaledWidth = client != null ? KillsUtility.getCurrentOverlayWidth(client) : MIN_OVERLAY_WIDTH;
        
        // Use the exact same logic as KillsUtility.renderKillsOverlay()
        // Determine if overlay is on left or right side of screen
        // Base position calculation (assuming default width)
        int baseX = screenWidth - MIN_OVERLAY_WIDTH - xOffset;
        boolean isOnLeftSide = baseX < screenWidth / 2;
        
        // Calculate X position based on side (exact same as KillsUtility lines 590-601)
        if (isOnLeftSide) {
            // Keep left edge fixed, expand to the right
            // Calculate left edge position from offset
            // xOffset is distance from right edge with default width
            int leftEdgeX = screenWidth - MIN_OVERLAY_WIDTH - xOffset;
            return leftEdgeX;
        } else {
            // Keep right edge fixed, expand to the left
            // Right edge stays at: screenWidth - xOffset
            // xPosition = screenWidth - overlayWidth - xOffset (exact same as KillsUtility line 600)
            // Use unscaled width for positioning, just like KillsUtility does
            return screenWidth - unscaledWidth - xOffset;
        }
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityY;
    }
    
    @Override
    public int getWidth() {
        // Use the actual overlay width from KillsUtility and apply scale
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            float scale = CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityScale;
            if (scale <= 0) scale = 1.0f;
            return (int) (MIN_OVERLAY_WIDTH * scale);
        }
        int unscaledWidth = KillsUtility.getCurrentOverlayWidth(client);
        // Apply scale factor to match the visual size in the real overlay
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityScale;
        if (scale <= 0) scale = 1.0f; // Sicherheitscheck
        return (int) (unscaledWidth * scale);
    }
    
    @Override
    public int getHeight() {
        // Use the actual overlay height from KillsUtility and apply scale
        int unscaledHeight = KillsUtility.getCurrentOverlayHeight();
        // Apply scale factor to match the visual size in the real overlay
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityScale;
        if (scale <= 0) scale = 1.0f; // Sicherheitscheck
        return (int) (unscaledHeight * scale);
    }
    
    @Override
    public void setPosition(int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        
        // Use unscaled width for positioning (same as KillsUtility line 557)
        // getCurrentOverlayWidth() returns unscaled width
        int unscaledWidth = KillsUtility.getCurrentOverlayWidth(client);
        
        // Use the exact same logic as KillsUtility for determining side
        // Determine if overlay is on left or right side based on current position
        boolean isOnLeftSide = x < screenWidth / 2;
        
        // Calculate xOffset based on side (same logic as KillsUtility uses)
        int xOffset;
        if (isOnLeftSide) {
            // On left side: xOffset is distance from right edge with default width
            xOffset = screenWidth - MIN_OVERLAY_WIDTH - x;
        } else {
            // On right side: xOffset is distance from right edge (same as KillsUtility line 600)
            // Use unscaled width for positioning, just like KillsUtility does
            // This keeps the right edge fixed: screenWidth - overlayWidth - xOffset = x
            // So: xOffset = screenWidth - overlayWidth - x
            xOffset = screenWidth - unscaledWidth - x;
        }
        int yOffset = y;
        
        CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityY = yOffset;
    }
    
    @Override
    public void setSize(int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Get current unscaled dimensions
        int unscaledWidth = KillsUtility.getCurrentOverlayWidth(client);
        int unscaledHeight = KillsUtility.getCurrentOverlayHeight();
        
        // Calculate scale based on width and height
        float scaleX = (float) width / unscaledWidth;
        float scaleY = (float) height / unscaledHeight;
        float scale = (scaleX + scaleY) / 2.0f;
        
        // Clamp scale to reasonable values (0.1 to 5.0)
        scale = Math.max(0.1f, Math.min(5.0f, scale));
        
        CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityScale = scale;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Get unscaled dimensions and position
        int unscaledWidth = client != null ? KillsUtility.getCurrentOverlayWidth(client) : MIN_OVERLAY_WIDTH;
        int unscaledHeight = KillsUtility.getCurrentOverlayHeight();
        int x = getX();
        int y = getY();
        
        // Get scale
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityScale;
        if (scale <= 0) scale = 1.0f;
        
        // Calculate scaled dimensions
        int scaledWidth = (int) (unscaledWidth * scale);
        int scaledHeight = (int) (unscaledHeight * scale);
        
        // Render border for edit mode (unscaled, so it's always visible)
        context.drawBorder(x, y, scaledWidth, scaledHeight, 0xFFFF0000);
        
        // Use Matrix transformations for scaling (same as KillsUtility line 606-615)
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        
        // Translate to position and scale from there (same as KillsUtility line 614-615)
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        // Render background if enabled (scaled, relative to matrix)
        if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowBackground) {
            context.fill(0, 0, unscaledWidth, unscaledHeight, 0x80000000);
        }
        
        // Render overlay name (scaled, relative to matrix)
        context.drawText(
            client.textRenderer,
            getOverlayName(),
            PADDING, PADDING, // Relative position (same as KillsUtility line 627)
            0xFFFFFFFF,
            true
        );
        
        // Render sample text (matching actual overlay content, scaled, relative positions)
        int currentY = PADDING + 15; // Same as KillsUtility line 561
        context.drawText(
            client.textRenderer,
            "KPM: 123.4",
            PADDING, currentY, // Relative position
            0xFFFFFFFF,
            true
        );
        
        currentY += LINE_HEIGHT;
        context.drawText(
            client.textRenderer,
            "Kills: 12345",
            PADDING, currentY, // Relative position
            0xFFFFFFFF,
            true
        );
        
        currentY += LINE_HEIGHT;
        context.drawText(
            client.textRenderer,
            "Zeit: 12:34",
            PADDING, currentY, // Relative position
            0xFFFFFFFF,
            true
        );
        
        // Only show "Nächste Ebene" if enabled in config
        if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowNextLevel) {
            currentY += LINE_HEIGHT;
            context.drawText(
                client.textRenderer,
                "Nächste Ebene: ?",
                PADDING, currentY, // Relative position
                0xFFFFFFFF,
                true
            );
        }
        
        // Only show "Benötigte Kills" if enabled in config
        if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowRequiredKills) {
            currentY += LINE_HEIGHT;
            context.drawText(
                client.textRenderer,
                "Benötigte Kills: ?",
                PADDING, currentY, // Relative position
                0xFFFFFFFF,
                true
            );
        }
        
        matrices.popMatrix();
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityEnabled && 
               CCLiveUtilitiesConfig.HANDLER.instance().showKillsUtility;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Kills Utility - Shows kill and death statistics");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityX = 570;
        CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityY = 100;
        CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityScale = 1.0f;
    }
    
    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityScale = 1.0f;
    }
}



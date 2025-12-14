package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.KillsUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

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
        int dynamicWidth = getWidth();
        
        // Determine if overlay is on left or right side of screen
        int baseX = screenWidth - MIN_OVERLAY_WIDTH - xOffset;
        boolean isOnLeftSide = baseX < screenWidth / 2;
        
        // Calculate X position based on side (same logic as in KillsUtility)
        if (isOnLeftSide) {
            // Keep left edge fixed, expand to the right
            int leftEdgeX = screenWidth - MIN_OVERLAY_WIDTH - xOffset;
            return leftEdgeX;
        } else {
            // Keep right edge fixed, expand to the left
            return screenWidth - dynamicWidth - xOffset;
        }
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityY;
    }
    
    @Override
    public int getWidth() {
        // Use the actual overlay width from KillsUtility
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return MIN_OVERLAY_WIDTH;
        }
        return KillsUtility.getCurrentOverlayWidth(client);
    }
    
    @Override
    public int getHeight() {
        // Use the actual overlay height from KillsUtility
        return KillsUtility.getCurrentOverlayHeight();
    }
    
    @Override
    public void setPosition(int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int dynamicWidth = getWidth();
        
        // Determine if overlay is on left or right side of screen
        boolean isOnLeftSide = x < screenWidth / 2;
        
        // Calculate xOffset based on side (same logic as in KillsUtility)
        int xOffset;
        if (isOnLeftSide) {
            // On left side: xOffset is distance from right edge with default width
            xOffset = screenWidth - MIN_OVERLAY_WIDTH - x;
        } else {
            // On right side: xOffset is distance from right edge with dynamic width
            xOffset = screenWidth - dynamicWidth - x;
        }
        int yOffset = y;
        
        CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityY = yOffset;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render background if enabled
        if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowBackground) {
            context.fill(x, y, x + width, y + height, 0x80000000);
        }
        
        // Render border for edit mode
        context.drawBorder(x, y, width, height, 0xFFFF0000);
        
        // Render overlay name
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            getOverlayName(),
            x + 5, y + 5,
            0xFFFFFFFF,
            true
        );
        
        // Render sample text (matching actual overlay content)
        int currentY = y + 20;
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "KPM: 123.4",
            x + PADDING, currentY,
            0xFFFFFFFF,
            true
        );
        
        currentY += LINE_HEIGHT;
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Kills: 12345",
            x + PADDING, currentY,
            0xFFFFFFFF,
            true
        );
        
        currentY += LINE_HEIGHT;
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Zeit: 12:34",
            x + PADDING, currentY,
            0xFFFFFFFF,
            true
        );
        
        // Only show "Nächste Ebene" if enabled in config
        if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowNextLevel) {
            currentY += LINE_HEIGHT;
            context.drawText(
                MinecraftClient.getInstance().textRenderer,
                "Nächste Ebene: ?",
                x + PADDING, currentY,
                0xFFFFFFFF,
                true
            );
        }
        
        // Only show "Benötigte Kills" if enabled in config
        if (CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityShowRequiredKills) {
            currentY += LINE_HEIGHT;
            context.drawText(
                MinecraftClient.getInstance().textRenderer,
                "Benötigte Kills: ?",
                x + PADDING, currentY,
                0xFFFFFFFF,
                true
            );
        }
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
    }
}



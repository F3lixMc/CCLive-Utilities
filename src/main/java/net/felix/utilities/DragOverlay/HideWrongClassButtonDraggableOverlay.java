package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Draggable Overlay für den Hide Wrong Class Button
 */
public class HideWrongClassButtonDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 120;
    private static final int DEFAULT_HEIGHT = 20;
    
    @Override
    public String getOverlayName() {
        return "Hide wrong class Button";
    }
    
    @Override
    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonX;
        
        // Calculate position based on right edge (same as SchmiedTrackerUtility)
        int baseX = screenWidth - DEFAULT_WIDTH - 20; // Right edge minus button width minus margin
        return baseX + xOffset;
    }
    
    @Override
    public int getY() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonY;
        
        // Calculate position based on top edge (same as SchmiedTrackerUtility)
        int baseY = 20; // Top edge with margin
        return baseY + yOffset;
    }
    
    @Override
    public int getWidth() {
        return DEFAULT_WIDTH;
    }
    
    @Override
    public int getHeight() {
        return DEFAULT_HEIGHT;
    }
    
    @Override
    public void setPosition(int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        
        // Calculate offset from right edge (same as SchmiedTrackerUtility)
        int baseX = screenWidth - DEFAULT_WIDTH - 20;
        int baseY = 20;
        
        int xOffset = x - baseX;
        int yOffset = y - baseY;
        
        CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonY = yOffset;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render button background
        context.fill(x, y, x + width, y + height, 0xFF4B6A69);
        
        // Render border for edit mode
        context.drawBorder(x, y, width, height, 0xFFFF0000);
        
        // Render button text
        String buttonText = "Hide wrong class";
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(buttonText);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            buttonText,
            textX, textY,
            0xFFFFFFFF,
            true
        );
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassEnabled;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Hide wrong class Button - Toggles visibility of items not suitable for your class");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonX = -80;
        CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonY = 80;
    }
    
    @Override
    public boolean isResizeArea(int mouseX, int mouseY) {
        // Hide Wrong Class Button doesn't support resizing
        return false;
    }
}


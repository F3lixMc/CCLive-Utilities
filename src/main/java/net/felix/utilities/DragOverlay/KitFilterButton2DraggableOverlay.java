package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Draggable Overlay für den Kit Filter Button 2
 */
public class KitFilterButton2DraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 20;
    
    @Override
    public String getOverlayName() {
        return "Kit Filter Button 2";
    }
    
    @Override
    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2X;
        
        // Calculate position based on right edge
        int baseX = screenWidth - DEFAULT_WIDTH - 20;
        return baseX + xOffset;
    }
    
    @Override
    public int getY() {
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2Y;
        
        // Calculate position based on top edge
        int baseY = 75;
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
        
        // Calculate offset from right edge
        int baseX = screenWidth - DEFAULT_WIDTH - 20;
        int baseY = 75;
        
        int xOffset = x - baseX;
        int yOffset = y - baseY;
        
        CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2X = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2Y = yOffset;
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
        String buttonText = "Kit 2";
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(buttonText);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            buttonText,
            textX, textY,
            0xFF404040,
            false
        );
        
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().enableMod &&
               CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButtonsEnabled;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Kit Filter Button 2 - Filter items by kit type and level");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2X = -100;
        CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2Y = 75;
    }
    
    @Override
    public boolean isResizeArea(int mouseX, int mouseY) {
        // Kit Filter Buttons don't support resizing
        return false;
    }
}


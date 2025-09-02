package net.felix.utilities;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Draggable Overlay für den Kills Utility
 */
public class KillsUtilityDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 65;
    private static final int DEFAULT_HEIGHT = 60;
    
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
        return screenWidth - DEFAULT_WIDTH - xOffset;
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityY;
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
        int xOffset = screenWidth - DEFAULT_WIDTH - x;
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
        
        // Render sample text
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Kills: 123",
            x + 8, y + 25,
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Deaths: 45",
            x + 8, y + 38,
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

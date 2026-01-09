package net.felix.utilities.DragOverlay.TabInfo;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Draggable Overlay für das Star Aspect Overlay (für Items mit "⭐" im Tooltip)
 */
public class StarAspectOverlayDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 210;
    private static final int DEFAULT_HEIGHT = 110;
    
    @Override
    public String getOverlayName() {
        return "Star Aspect Overlay";
    }
    
    @Override
    public int getX() {
        return CCLiveUtilitiesConfig.HANDLER.instance().starAspectOverlayX;
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().starAspectOverlayY;
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
        CCLiveUtilitiesConfig.HANDLER.instance().starAspectOverlayX = x;
        CCLiveUtilitiesConfig.HANDLER.instance().starAspectOverlayY = y;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render background if enabled
        if (CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayShowBackground) {
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
            "Aspekt Name",
            x + 8, y + 25,
            0xFFFCA800,
            true
        );
        
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Beschreibung des Aspektes",
            x + 8, y + 38,
            0xFF54FC54,
            true
        );
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled && 
               CCLiveUtilitiesConfig.HANDLER.instance().showAspectOverlay;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Star Aspect Overlay - Shows aspect information for items with ⭐ in tooltip");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().starAspectOverlayX = 7;
        CCLiveUtilitiesConfig.HANDLER.instance().starAspectOverlayY = 15;
    }
}


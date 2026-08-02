package net.felix.utilities.DragOverlay.Schmied;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.Overall.DraggableOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/**
 * Draggable Overlay für den Hide Wrong Class Button
 */
public class HideWrongClassButtonDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 120;
    private static final int DEFAULT_HEIGHT = 20;
    
    private int getUnscaledWidth() {
        return DEFAULT_WIDTH;
    }
    
    private int getUnscaledHeight() {
        return DEFAULT_HEIGHT;
    }
    
    @Override
    public String getOverlayName() {
        return "Hide wrong class Button";
    }
    
    @Override
    public int getX() {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonX;
        
        // Calculate position based on right edge (same as SchmiedTrackerUtility)
        int baseX = screenWidth - DEFAULT_WIDTH - 20; // Right edge minus button width minus margin
        return baseX + xOffset;
    }
    
    @Override
    public int getY() {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) return 0;
        
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonY;
        
        // Calculate position based on top edge (same as SchmiedTrackerUtility)
        int baseY = 20; // Top edge with margin
        return baseY + yOffset;
    }
    
    @Override
    public int getWidth() {
        int unscaledWidth = getUnscaledWidth();
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonScale;
        if (scale <= 0) scale = 1.0f;
        return (int) (unscaledWidth * scale);
    }
    
    @Override
    public int getHeight() {
        int unscaledHeight = getUnscaledHeight();
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonScale;
        if (scale <= 0) scale = 1.0f;
        return (int) (unscaledHeight * scale);
    }
    
    @Override
    public void setPosition(int x, int y) {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) return;
        
        int screenWidth = client.getWindow().getGuiScaledWidth();
        
        // Calculate offset from right edge (same as SchmiedTrackerUtility)
        int baseX = screenWidth - DEFAULT_WIDTH - 20;
        int baseY = 20;
        
        int xOffset = x - baseX;
        int yOffset = y - baseY;
        
        CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonY = yOffset;
    }
    
    @Override
    public void setSize(int width, int height) {
        int unscaledWidth = getUnscaledWidth();
        int unscaledHeight = getUnscaledHeight();
        
        // Calculate scale based on width and height
        float scaleX = (float) width / unscaledWidth;
        float scaleY = (float) height / unscaledHeight;
        float scale = (scaleX + scaleY) / 2.0f;
        
        // Clamp scale to reasonable values (0.1 to 5.0)
        scale = Math.max(0.1f, Math.min(5.0f, scale));
        
        CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonScale = scale;
        // Position stays the same - overlay grows from top-left corner
    }
    
    @Override
    public void renderInEditMode(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        
        int unscaledWidth = getUnscaledWidth();
        int unscaledHeight = getUnscaledHeight();
        int x = getX();
        int y = getY();
        
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonScale;
        if (scale <= 0) scale = 1.0f;
        
        int scaledWidth = (int) (unscaledWidth * scale);
        int scaledHeight = (int) (unscaledHeight * scale);
        
        // Use Matrix transformations for scaling
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        
        // Translate to position and scale from there
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        // Render button background (scaled, relative to matrix)
        context.fill(0, 0, unscaledWidth, unscaledHeight, 0xFF4B6A69);
        
        // Render button text (scaled, relative to matrix)
        String buttonText = "Hide wrong class";
        int textWidth = client.font.width(buttonText);
        int textX = (unscaledWidth - textWidth) / 2;
        int textY = (unscaledHeight - 8) / 2;
        
        context.text(
            client.font,
            buttonText,
            textX, textY,
            0xFFFFFFFF,
            true
        );
        
        matrices.popMatrix();
        
        // Render border for edit mode AFTER content (so it's always visible on top)
        context.outline(x, y, scaledWidth, scaledHeight, 0xFFFF0000);
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassEnabled &&
               CCLiveUtilitiesConfig.HANDLER.instance().showHideWrongClassButton;
    }
    
    @Override
    public Component getTooltip() {
        return Component.literal("Hide wrong class Button - Toggles visibility of items not suitable for your class");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonX = -195;
        CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonY = 126;
        CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonScale = 1.0f;
    }
    
    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonScale = 1.0f;
    }
    
    @Override
    public boolean isResizeArea(int mouseX, int mouseY) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Check if mouse is in the bottom-right corner (resize area)
        int resizeAreaSize = 8;
        return mouseX >= x + width - resizeAreaSize && mouseX <= x + width &&
               mouseY >= y + height - resizeAreaSize && mouseY <= y + height;
    }
}

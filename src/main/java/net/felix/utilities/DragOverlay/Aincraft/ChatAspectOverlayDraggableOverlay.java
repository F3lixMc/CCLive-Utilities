package net.felix.utilities.DragOverlay.Aincraft;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.Overall.DraggableOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Draggable Overlay für das Chat Aspect Overlay
 */
public class ChatAspectOverlayDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 210;
    private static final int DEFAULT_HEIGHT = 110;
    
    @Override
    public String getOverlayName() {
        return "Chat Aspect Overlay";
    }
    
    @Override
    public int getX() {
        return CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayX;
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayY;
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
        CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayX = x;
        CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayY = y;
    }
    
    @Override
    public void renderInEditMode(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render background if enabled
        if (CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayShowBackground) {
            context.fill(x, y, x + width, y + height, 0x80000000);
        }
        
        // Render border for edit mode
        context.outline(x, y, width, height, 0xFFFF0000);
        
        // Render overlay name
        context.text(
            Minecraft.getInstance().font,
            getOverlayName(),
            x + 5, y + 5,
            0xFFFFFFFF,
            true
        );
        
        // Render sample text
        context.text(
            Minecraft.getInstance().font,
            "Aspect: Fire",
            x + 8, y + 25,
            0xFFFCA800,
            true
        );
        
        context.text(
            Minecraft.getInstance().font,
            "Description: Burns enemies",
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
        return CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayEnabled;
    }
    
    @Override
    public Component getTooltip() {
        return Component.literal("Chat Aspect Overlay - Shows aspect information for blueprint items in chat");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayX = 7;
        CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayY = 15;
    }
}


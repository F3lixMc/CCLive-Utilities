package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DraggableOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Draggable Overlay für das Aspect Overlay
 */
public class AspectOverlayDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 100;
    
    @Override
    public String getOverlayName() {
        return "Aspect Overlay";
    }
    
    @Override
    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayX;
        return screenWidth - DEFAULT_WIDTH - xOffset;
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayY;
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
        
        CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayY = yOffset;
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
            "Aspect: Fire",
            x + 8, y + 25,
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Description: Burns enemies",
            x + 8, y + 38,
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Item: Fire Sword",
            x + 8, y + 51,
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
        return CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled && 
               CCLiveUtilitiesConfig.HANDLER.instance().showAspectOverlay;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Aspect Overlay - Shows aspect information for blueprint items");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayX = 415;
        CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayY = 60;
    }
}

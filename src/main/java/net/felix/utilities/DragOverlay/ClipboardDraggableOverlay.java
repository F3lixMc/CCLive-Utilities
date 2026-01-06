package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Draggable Overlay für das Clipboard (Bauplan-Kosten)
 */
public class ClipboardDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 300;
    
    @Override
    public String getOverlayName() {
        return "Clipboard";
    }
    
    @Override
    public int getX() {
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardX;
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardY;
    }
    
    @Override
    public int getWidth() {
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardWidth;
    }
    
    @Override
    public int getHeight() {
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardHeight;
    }
    
    @Override
    public void setPosition(int x, int y) {
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardX = x;
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardY = y;
    }
    
    @Override
    public void setSize(int width, int height) {
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardWidth = Math.max(150, width); // Mindestbreite 150
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardHeight = Math.max(200, height); // Mindesthöhe 200
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render background
        context.fill(x, y, x + width, y + height, 0x80000000);
        
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
        
        // Render sample content
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Bauplan-Kosten",
            x + 5, y + 25,
            0xFFAAAAAA,
            true
        );
        
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Gesamtliste",
            x + 5, y + 40,
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Eichenholz: 10x",
            x + 5, y + 55,
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Kohle: 5x",
            x + 5, y + 70,
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
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled && 
               CCLiveUtilitiesConfig.HANDLER.instance().showClipboard;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Clipboard - Zeigt Bauplan-Kosten an");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardX = 10;
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardY = 10;
    }
    
    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardWidth = DEFAULT_WIDTH;
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardHeight = DEFAULT_HEIGHT;
    }
}


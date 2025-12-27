package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.TabInfoUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Draggable Overlay für das große Tab-Info Overlay
 */
public class TabInfoMainDraggableOverlay implements DraggableOverlay {
    
    @Override
    public String getOverlayName() {
        return "Tab Info (Haupt)";
    }
    
    @Override
    public int getX() {
        return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayX;
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayY;
    }
    
    @Override
    public int getWidth() {
        // Berechne die tatsächliche Breite des Overlays
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 200;
        
        // Verwende eine geschätzte Breite basierend auf typischen Inhalten
        return 250;
    }
    
    @Override
    public int getHeight() {
        // Berechne die tatsächliche Höhe basierend auf der Anzahl der Zeilen
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 100;
        
        int lineCount = TabInfoUtility.getMainOverlayLineCount();
        int lineHeight = client.textRenderer.fontHeight + 2;
        int padding = 5;
        return (lineCount * lineHeight) + (padding * 2);
    }
    
    @Override
    public void setPosition(int x, int y) {
        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayX = x;
        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayY = y;
    }
    
    @Override
    public void setSize(int width, int height) {
        // Größe kann nicht geändert werden, wird automatisch berechnet
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render border for edit mode
        context.drawBorder(x, y, width, height, 0xFFFF0000);
        
        // Render background
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // Render overlay name
        context.drawText(
            client.textRenderer,
            getOverlayName(),
            x + 5, y + 5,
            0xFFFFFFFF,
            true
        );
        
        // Render sample content
        int currentY = y + 20;
        context.drawText(
            client.textRenderer,
            "Forschung: 5 / 10",
            x + 5, currentY,
            0xFFFFFFFF,
            true
        );
        
        currentY += client.textRenderer.fontHeight + 2;
        context.drawText(
            client.textRenderer,
            "Amboss: 100 / 500 50.0%",
            x + 5, currentY,
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
        return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoUtilityEnabled;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Tab Info Haupt-Overlay - Zeigt alle Tab-Informationen");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayX = 5;
        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayY = 5;
    }
    
    @Override
    public void resetSizeToDefault() {
        // Größe kann nicht zurückgesetzt werden
    }
}


package net.felix.utilities.DragOverlay.Farmworld;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.felix.utilities.Overall.InformationenUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

/**
 * Draggable Overlay für das Collection Overlay
 */
public class CollectionDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 80;
    private static final int LINE_HEIGHT = 11;
    private static final int PADDING = 5;
    
    @Override
    public String getOverlayName() {
        return "Collection";
    }
    
    @Override
    public int getX() {
        return CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayX;
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayY;
    }
    
    @Override
    public int getWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            float scale = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayScale;
            if (scale <= 0) scale = 1.0f;
            return (int) (DEFAULT_WIDTH * scale);
        }
        int unscaledWidth = getUnscaledWidth(client);
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayScale;
        if (scale <= 0) scale = 1.0f;
        return (int) (unscaledWidth * scale);
    }
    
    @Override
    public int getHeight() {
        int unscaledHeight = getUnscaledHeight();
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayScale;
        if (scale <= 0) scale = 1.0f;
        return (int) (unscaledHeight * scale);
    }
    
    private int getUnscaledWidth(MinecraftClient client) {
        // Use the actual overlay width from InformationenUtility (same as KillsUtility pattern)
        return client != null ? InformationenUtility.getCurrentCollectionOverlayWidth(client) : DEFAULT_WIDTH;
    }
    
    private int getUnscaledHeight() {
        // Use the actual overlay height from InformationenUtility
        return InformationenUtility.getCurrentCollectionOverlayHeight();
    }
    
    @Override
    public void setPosition(int x, int y) {
        CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayX = x;
        CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayY = y;
    }
    
    @Override
    public void setSize(int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Get current unscaled dimensions
        int unscaledWidth = getUnscaledWidth(client);
        int unscaledHeight = getUnscaledHeight();
        
        // Calculate scale based on width and height
        float scaleX = (float) width / unscaledWidth;
        float scaleY = (float) height / unscaledHeight;
        float scale = (scaleX + scaleY) / 2.0f;
        
        // Clamp scale to reasonable values (0.1 to 5.0)
        scale = Math.max(0.1f, Math.min(5.0f, scale));
        
        CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayScale = scale;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Get unscaled dimensions and position
        int unscaledWidth = getUnscaledWidth(client);
        int unscaledHeight = getUnscaledHeight();
        int x = getX();
        int y = getY();
        
        // Get scale
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayScale;
        if (scale <= 0) scale = 1.0f;
        
        // Calculate scaled dimensions
        int scaledWidth = (int) (unscaledWidth * scale);
        int scaledHeight = (int) (unscaledHeight * scale);
        
        // Render border for edit mode (unscaled, so it's always visible)
        context.drawBorder(x, y, scaledWidth, scaledHeight, 0xFFFF0000);
        
        // Use Matrix transformations for scaling
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        
        // Translate to position and scale from there
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        // Render background (scaled, relative to matrix)
        context.fill(0, 0, unscaledWidth, unscaledHeight, 0x80000000);
        
        // Render overlay name (scaled, relative to matrix)
        context.drawText(
            client.textRenderer,
            getOverlayName(),
            PADDING, PADDING,
            0xFFFFFFFF,
            true
        );
        
        // Render sample text with "-" instead of example values (scaled, relative positions)
        int currentY = PADDING + 15;
        context.drawText(
            client.textRenderer,
            "Zeit: -",
            PADDING, currentY,
            0xFFFFFFFF,
            true
        );
        
        currentY += LINE_HEIGHT;
        context.drawText(
            client.textRenderer,
            "Blöcke: -",
            PADDING, currentY,
            0xFFFFFFFF,
            true
        );
        
        currentY += LINE_HEIGHT;
        context.drawText(
            client.textRenderer,
            "Blöcke/min: -",
            PADDING, currentY,
            0xFFFFFFFF,
            true
        );
        
        currentY += LINE_HEIGHT;
        context.drawText(
            client.textRenderer,
            "Benötigte Blöcke: -",
            PADDING, currentY,
            0xFFFFFFFF,
            true
        );
        
        currentY += LINE_HEIGHT;
        context.drawText(
            client.textRenderer,
            "Nächste Collection: -",
            PADDING, currentY,
            0xFFFFFFFF,
            true
        );
        
        matrices.popMatrix();
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Collection - Shows collection progress and statistics");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayX = 10;
        CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayY = 10;
        CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayScale = 1.0f;
    }
    
    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayScale = 1.0f;
    }
}


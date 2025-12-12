package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.InformationenUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Draggable Overlay für Mining und Lumberjack Overlays
 * Beide Overlays teilen sich die gleiche Position
 */
public class MiningLumberjackDraggableOverlay implements DraggableOverlay {
    
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 5;
    private static final int LINES = 5; // Header, Last XP, XP/Min, Time to next level, Required XP
    
    @Override
    public String getOverlayName() {
        return "Mining / Holzfäller";
    }
    
    @Override
    public int getX() {
        return CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayX;
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayY;
    }
    
    @Override
    public int getWidth() {
        // Use the actual overlay width from InformationenUtility (dynamically calculated based on current XP data)
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return 200;
        }
        
        // Get the maximum width from both overlays (they share the same position)
        int miningWidth = InformationenUtility.getMiningOverlayWidth(client);
        int lumberjackWidth = InformationenUtility.getLumberjackOverlayWidth(client);
        
        // Return the maximum width so the overlay editor shows the correct size
        return Math.max(miningWidth, lumberjackWidth);
    }
    
    @Override
    public int getHeight() {
        return PADDING * 2 + LINE_HEIGHT * LINES;
    }
    
    @Override
    public void setPosition(int x, int y) {
        // Update both mining and lumberjack positions (they share the same position)
        CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayX = x;
        CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayY = y;
        // Lumberjack uses the same position (miningOverlayX/Y)
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render background if enabled
        if (CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayShowBackground) {
            context.fill(x, y, x + width, y + height, 0x80000000);
        }
        
        // Render border for edit mode
        context.drawBorder(x, y, width, height, 0xFFFF0000);
        
        // Render overlay name (without level in editor)
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            getOverlayName(),
            x + PADDING, y + PADDING,
            0xFFFFFFFF,
            true
        );
        
        // Render sample text (matching actual overlay content)
        
        int currentY = y + PADDING + LINE_HEIGHT;
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Letzte XP: 1234",
            x + PADDING, currentY,
            0xFFFFFFFF,
            true
        );
        
        currentY += LINE_HEIGHT;
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "XP/Min: 123.4",
            x + PADDING, currentY,
            0xFFFFFFFF,
            true
        );
        
        currentY += LINE_HEIGHT;
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Zeit bis Level: 12:34",
            x + PADDING, currentY,
            0xFFFFFFFF,
            true
        );
        
        currentY += LINE_HEIGHT;
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            "Benötigte XP: 12345",
            x + PADDING, currentY,
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
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        // Check if the main toggle is enabled and either overlay is enabled
        return config.enableMod && 
               config.miningLumberjackOverlayEnabled &&
               ((config.miningOverlayEnabled && config.showMiningOverlay) ||
                (config.lumberjackOverlayEnabled && config.showLumberjackOverlay));
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Mining / Holzfäller Overlay - Shows XP information for mining and lumberjack activities");
    }
    
    @Override
    public void resetToDefault() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getWindow() != null) {
            int screenHeight = client.getWindow().getScaledHeight();
            // Position: am linken Rand (X=5) und vertikal mittig (Y = screenHeight / 2 - overlayHeight / 2)
            // Da wir die Höhe nicht direkt haben, verwenden wir einen geschätzten Wert
            // Typische Overlay-Höhe ist etwa 80 Pixel (5 Zeilen * 12 + Padding)
            int estimatedOverlayHeight = 80;
            int centerY = (screenHeight / 2) - (estimatedOverlayHeight / 2);
            CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayX = 5;
            CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayY = Math.max(5, centerY); // Mindestens 5 Pixel vom Rand
        } else {
            // Fallback wenn Client nicht verfügbar ist
            CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayX = 5;
            CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayY = 200;
        }
    }
}


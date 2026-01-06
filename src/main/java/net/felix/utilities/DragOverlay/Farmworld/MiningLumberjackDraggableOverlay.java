package net.felix.utilities.DragOverlay.Farmworld;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.DraggableOverlay;
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
        // Calculate X position using the same logic as renderMiningOverlay()
        // Use unscaled width for positioning (same as actual overlay)
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayX;
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
        int baseX = CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayX;
        int overlayWidth = calculateUnscaledWidth(); // Use unscaled width for positioning
        int minOverlayWidth = 100;
        
        // Determine if overlay is on left or right side of screen
        boolean isOnLeftSide = baseX < screenWidth / 2;
        
        // Calculate X position based on side (same logic as renderMiningOverlay)
        int x;
        if (isOnLeftSide) {
            // On left side: keep left edge fixed, expand to the right
            x = baseX;
        } else {
            // On right side: keep right edge fixed, expand to the left
            // Right edge at minimum width is: baseX + minOverlayWidth
            // Keep this right edge fixed, so left edge moves left when width increases
            int rightEdge = baseX + minOverlayWidth;
            x = rightEdge - overlayWidth;
        }
        
        // Ensure overlay stays within screen bounds
        return Math.max(0, Math.min(x, screenWidth - overlayWidth));
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayY;
    }
    
    private int calculateUnscaledWidth() {
        // Calculate unscaled width using the exact same logic as renderMiningOverlay() and renderLumberjackOverlay()
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return 200;
        }
        
        // Use the exact same calculation as in renderMiningOverlay() and renderLumberjackOverlay()
        int padding = 5;
        int minOverlayWidth = 100;
        
        // Check which overlay is currently being displayed
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        boolean showMining = config.miningOverlayEnabled && config.showMiningOverlay;
        boolean showLumberjack = config.lumberjackOverlayEnabled && config.showLumberjackOverlay;
        
        // Calculate width for mining overlay (same logic as renderMiningOverlay)
        String miningHeader = InformationenUtility.getMiningOverlayHeader();
        String[] miningTexts = InformationenUtility.getMiningOverlayTexts(client);
        int miningMaxWidth = Math.max(
            Math.max(client.textRenderer.getWidth(miningHeader),
                Math.max(client.textRenderer.getWidth(miningTexts[1]),
                    Math.max(client.textRenderer.getWidth(miningTexts[2]),
                        Math.max(client.textRenderer.getWidth(miningTexts[3]),
                            client.textRenderer.getWidth(miningTexts[4]))))),
            minOverlayWidth);
        int miningWidth = miningMaxWidth + padding * 2;
        
        // Calculate width for lumberjack overlay (same logic as renderLumberjackOverlay)
        String lumberjackHeader = InformationenUtility.getLumberjackOverlayHeader();
        String[] lumberjackTexts = InformationenUtility.getLumberjackOverlayTexts(client);
        int lumberjackMaxWidth = Math.max(
            Math.max(client.textRenderer.getWidth(lumberjackHeader),
                Math.max(client.textRenderer.getWidth(lumberjackTexts[1]),
                    Math.max(client.textRenderer.getWidth(lumberjackTexts[2]),
                        Math.max(client.textRenderer.getWidth(lumberjackTexts[3]),
                            client.textRenderer.getWidth(lumberjackTexts[4]))))),
            minOverlayWidth);
        int lumberjackWidth = lumberjackMaxWidth + padding * 2;
        
        // Return the width of the overlay that would be displayed, or the maximum if both are enabled
        if (showMining && showLumberjack) {
            // Both enabled: use the maximum width (they share position, so we need to show the wider one)
            return Math.max(miningWidth, lumberjackWidth);
        } else if (showMining) {
            // Only mining enabled
            return miningWidth;
        } else if (showLumberjack) {
            // Only lumberjack enabled
            return lumberjackWidth;
        } else {
            // Neither enabled: use maximum as fallback
            return Math.max(miningWidth, lumberjackWidth);
        }
    }
    
    private int calculateUnscaledHeight() {
        return PADDING * 2 + LINE_HEIGHT * LINES;
    }
    
    @Override
    public int getWidth() {
        // Return scaled width
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayScale;
        return Math.round(calculateUnscaledWidth() * scale);
    }
    
    @Override
    public int getHeight() {
        // Return scaled height
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayScale;
        return Math.round(calculateUnscaledHeight() * scale);
    }
    
    @Override
    public void setPosition(int x, int y) {
        // Calculate baseX using the same logic as renderMiningOverlay()
        // We need to reverse the calculation: from the actual x position, calculate baseX
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayX = x;
            CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayY = y;
            return;
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
        int overlayWidth = calculateUnscaledWidth(); // Use unscaled width for positioning
        int minOverlayWidth = 100;
        
        // Determine if overlay is on left or right side of screen
        boolean isOnLeftSide = x < screenWidth / 2;
        
        // Calculate baseX based on side (reverse of getX() calculation)
        int baseX;
        if (isOnLeftSide) {
            // On left side: baseX is the same as x
            baseX = x;
        } else {
            // On right side: baseX is calculated from the right edge
            // Right edge at minimum width is: baseX + minOverlayWidth
            // Actual right edge is: x + overlayWidth
            // We want to keep the right edge at minimum width fixed, so:
            // baseX + minOverlayWidth = x + overlayWidth
            // baseX = x + overlayWidth - minOverlayWidth
            // But we want to keep the right edge at the position where it was when saved
            // So we calculate: rightEdge = x + overlayWidth, then baseX = rightEdge - minOverlayWidth
            int rightEdge = x + overlayWidth;
            baseX = rightEdge - minOverlayWidth;
        }
        
        // Update both mining and lumberjack positions (they share the same position)
        CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayX = baseX;
        CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayY = y;
        // Lumberjack uses the same position (miningOverlayX/Y)
    }
    
    @Override
    public void setSize(int width, int height) {
        // Calculate scale based on unscaled dimensions
        int unscaledWidth = calculateUnscaledWidth();
        int unscaledHeight = calculateUnscaledHeight();
        
        // Calculate scale based on width and height
        float scaleX = (float) width / unscaledWidth;
        float scaleY = (float) height / unscaledHeight;
        float scale = (scaleX + scaleY) / 2.0f;
        
        // Clamp scale to reasonable values (0.1 to 5.0)
        scale = Math.max(0.1f, Math.min(5.0f, scale));
        
        CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayScale = scale;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int scaledWidth = getWidth();
        int scaledHeight = getHeight();
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayScale;
        
        // Get actual texts from the overlays to calculate correct width
        // But replace values with "?" for display in editor
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        String[] miningTexts = InformationenUtility.getMiningOverlayTexts(client);
        String[] lumberjackTexts = InformationenUtility.getLumberjackOverlayTexts(client);
        
        // Use the texts from the overlay that has the maximum width (to match getWidth() logic)
        int miningWidth = InformationenUtility.getMiningOverlayWidth(client);
        int lumberjackWidth = InformationenUtility.getLumberjackOverlayWidth(client);
        String[] texts = miningWidth >= lumberjackWidth ? miningTexts : lumberjackTexts;
        
        // Replace values with "-" for display in editor
        String[] displayTexts = new String[texts.length];
        displayTexts[0] = texts[0]; // Header stays the same
        displayTexts[1] = texts[1].replaceAll(": .+", ": -"); // "Letzte XP: 1234" -> "Letzte XP: -"
        displayTexts[2] = texts[2].replaceAll(": .+", ": -"); // "XP/Min: 123.4" -> "XP/Min: -"
        displayTexts[3] = texts[3].replaceAll(": .+", ": -"); // "Zeit bis Level: 12:34" -> "Zeit bis Level: -"
        displayTexts[4] = texts[4].replaceAll(": .+", ": -"); // "Benötigte XP: 12345" -> "Benötigte XP: -"
        
        // Apply matrix transformations for scaling
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        // Render background if enabled (within scaled matrix)
        if (CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayShowBackground) {
            int unscaledWidth = calculateUnscaledWidth();
            int unscaledHeight = calculateUnscaledHeight();
            context.fill(0, 0, unscaledWidth, unscaledHeight, 0x80000000);
        }
        
        // Render header (first text) - scaled
        context.drawText(
            client.textRenderer,
            displayTexts[0],
            PADDING, PADDING,
            0xFFFFFF00, // Yellow color like in actual overlay
            true
        );
        
        // Render other texts (lastXP, xpPerMin, timeToNext, requiredXP) - scaled
        int currentY = PADDING + LINE_HEIGHT;
        for (int i = 1; i < displayTexts.length; i++) {
            context.drawText(
                client.textRenderer,
                displayTexts[i],
                PADDING, currentY,
                0xFFFFFFFF,
                true
            );
            currentY += LINE_HEIGHT;
        }
        
        matrices.popMatrix();
        
        // Render border after scaling (using scaled dimensions)
        context.drawBorder(x, y, scaledWidth, scaledHeight, 0xFFFF0000);
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
        CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayX = 0;
        CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayY = 109;
        CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayScale = 1.0f;
    }
    
    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayScale = 1.0f;
    }
}


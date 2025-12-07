package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.ActionBarData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import org.joml.Matrix3x2fStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.List;

/**
 * Draggable Overlay für den Material Tracker
 */
public class MaterialTrackerDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 155;
    private static final int DEFAULT_HEIGHT = 103;
    private static final Identifier MATERIALS_BACKGROUND_TEXTURE = Identifier.of("cclive-utilities", "textures/gui/materials_background.png");
    
    @Override
    public String getOverlayName() {
        return "Material Tracker";
    }
    
    @Override
    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerX;
        int dynamicWidth = getWidth();
        
        // Determine if overlay is on left or right side of screen
        int baseX = screenWidth - DEFAULT_WIDTH - xOffset;
        boolean isOnLeftSide = baseX < screenWidth / 2;
        
        // Adjust X position based on side
        // If on left side: expand to the right (keep left edge fixed)
        // If on right side: expand to the left (keep right edge fixed)
        if (isOnLeftSide) {
            // Keep left edge fixed, expand to the right
            return baseX;
        } else {
            // Keep right edge fixed, expand to the left
            return screenWidth - dynamicWidth - xOffset;
        }
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerY;
    }
    
    @Override
    public int getWidth() {
        // Calculate dynamic width based on current material data (same logic as MaterialTrackerUtility)
        return calculateDynamicWidth();
    }
    
    @Override
    public int getHeight() {
        // Calculate dynamic height based on current material data (same logic as MaterialTrackerUtility)
        return calculateDynamicHeight();
    }
    
    @Override
    public void setPosition(int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int dynamicWidth = getWidth();
        
        // Determine if overlay is on left or right side of screen
        boolean isOnLeftSide = x < screenWidth / 2;
        
        // Calculate xOffset based on side
        int xOffset;
        if (isOnLeftSide) {
            // On left side: xOffset is distance from left edge
            xOffset = screenWidth - DEFAULT_WIDTH - x;
        } else {
            // On right side: xOffset is distance from right edge
            xOffset = screenWidth - dynamicWidth - x;
        }
        int yOffset = y;
        
        CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerY = yOffset;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Use Matrix transformations for scaling (like Blueprint Viewer)
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        
        // Scale based on config
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerScale;
        if (scale <= 0) scale = 1.0f; // Sicherheitscheck
        
        // Translate to position and scale from there
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        // Calculate unscaled dimensions for rendering
        int unscaledWidth = (int) (width / scale);
        int unscaledHeight = (int) (height / scale);
        
        // Render background (scaled)
        net.felix.OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerOverlayType;
        
        switch (overlayType) {
            case CUSTOM:
                try {
                    context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        MATERIALS_BACKGROUND_TEXTURE,
                        0, 0, // Position (relative to matrix)
                        0.0f, 0.0f,
                        unscaledWidth, unscaledHeight, // Size (unscaled, will be scaled by matrix)
                        unscaledWidth, unscaledHeight
                    );
                } catch (Exception e) {
                    context.fill(0, 0, unscaledWidth, unscaledHeight, 0x80000000);
                }
                break;
            case BLACK:
                context.fill(0, 0, unscaledWidth, unscaledHeight, 0x80000000);
                break;
            case NONE:
                // No background
                break;
        }
        
        // Render border for edit mode (scaled)
        context.drawBorder(0, 0, unscaledWidth, unscaledHeight, 0xFFFF0000);
        
        // Render overlay name (scaled)
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            getOverlayName(),
            5, 5, // Position (relative to matrix)
            0xFFFFFFFF,
            true
        );
        
        // Render real material data if available, otherwise sample data (scaled)
        renderMaterialData(context, 0, 0, unscaledWidth, unscaledHeight);
        
        // Restore matrix transformations
        matrices.popMatrix();
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerEnabled && 
               CCLiveUtilitiesConfig.HANDLER.instance().showMaterialTracker;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Material Tracker - Shows collected materials from action bar");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerX = 1;
        CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerY = 35;
    }
    
    /**
     * Render real material data if available, otherwise sample data
     * Uses matrix-relative positioning for scaling
     */
    private void renderMaterialData(DrawContext context, int x, int y, int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        try {
            // Try to get real material data from ActionBarData
            List<Object> materials = ActionBarData.getFilteredTexts();
            
            if (materials != null && !materials.isEmpty()) {
                // Render real data (limit to 3 items for edit mode)
                int materialY = 20; // Relative to matrix
                int count = 0;
                
                for (Object materialObj : materials) {
                    if (count >= 3) break; // Limit to 3 items for edit mode
                    
                    String materialText;
                    if (materialObj instanceof net.minecraft.text.Text) {
                        materialText = ((net.minecraft.text.Text) materialObj).getString();
                    } else {
                        materialText = materialObj.toString();
                    }
                    
                    // Truncate if too long for available width
                    int availableWidth = width - 16; // 8px margin on each side
                    int textWidth = client.textRenderer.getWidth(materialText);
                    if (textWidth > availableWidth) {
                        int maxChars = (int) ((double) availableWidth / textWidth * materialText.length());
                        if (maxChars > 3) {
                            materialText = materialText.substring(0, maxChars - 3) + "...";
                        } else {
                            materialText = "...";
                        }
                    }
                    
                    context.drawText(
                        client.textRenderer,
                        materialText,
                        8, materialY, // Position relative to matrix
                        0xFFFFFFFF,
                        true
                    );
                    
                    materialY += 12;
                    count++;
                }
                
                if (materials.size() > 3) {
                    context.drawText(
                        client.textRenderer,
                        "... and " + (materials.size() - 3) + " more",
                        8, materialY, // Position relative to matrix
                        0xFF888888,
                        true
                    );
                }
                
                return; // Successfully rendered real data
            }
        } catch (Exception e) {
            // Fall through to sample data
        }
        
        // Render sample data if real data is not available (scaled)
        context.drawText(
            client.textRenderer,
            "Prächtiges Eselhaar [1067]",
            8, 20, // Position relative to matrix
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            client.textRenderer,
            "Goldener Drache [234]",
            8, 32, // Position relative to matrix
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            client.textRenderer,
            "Mystisches Holz [89]",
            8, 44, // Position relative to matrix
            0xFFFFFFFF,
            true
        );
    }
    
    /**
     * Calculate dynamic width based on current material data (same logic as MaterialTrackerUtility)
     */
    private int calculateDynamicWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return DEFAULT_WIDTH;
        
        try {
            // Get materials from ActionBarData
            List<Object> texts = ActionBarData.getFilteredTexts();
            
            if (texts == null || texts.isEmpty()) {
                float scale = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerScale;
                if (scale <= 0) scale = 1.0f; // Sicherheitscheck
                return (int) (DEFAULT_WIDTH * scale);
            }
            
            // Calculate width based on text content (exact same logic as MaterialTrackerUtility)
            int dynamicWidth = calculateRequiredWidth(client, texts);
            float scale = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerScale;
            if (scale <= 0) scale = 1.0f; // Sicherheitscheck
            
            int baseWidth = Math.max(DEFAULT_WIDTH, dynamicWidth);
            return (int) (baseWidth * scale);
        } catch (Exception e) {
            // Fallback to default width if calculation fails
            float scale = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerScale;
            if (scale <= 0) scale = 1.0f; // Sicherheitscheck
            return (int) (DEFAULT_WIDTH * scale);
        }
    }
    
    /**
     * Calculate required width for materials (exact same logic as MaterialTrackerUtility)
     */
    private int calculateRequiredWidth(MinecraftClient client, List<Object> texts) {
        int maxWidth = 100; // MIN_TEXT_WIDTH from MaterialTrackerUtility
        
        for (Object textObj : texts) {
            net.minecraft.text.Text textComponent;
            if (textObj instanceof net.minecraft.text.Text) {
                textComponent = (net.minecraft.text.Text) textObj;
            } else {
                textComponent = net.minecraft.text.Text.literal(textObj.toString());
            }
            
            // Calculate text width (same as MaterialTrackerUtility)
            int textWidth = client.textRenderer.getWidth(textComponent);
            
            // Add padding (same as MaterialTrackerUtility: 10px left + 5px right)
            int totalWidth = textWidth + 15;
            
            // Update maximum width
            maxWidth = Math.max(maxWidth, totalWidth);
        }
        
        return maxWidth;
    }
    
    /**
     * Calculate dynamic height based on current material data (same logic as MaterialTrackerUtility)
     */
    private int calculateDynamicHeight() {
        try {
            // Get materials from ActionBarData
            List<Object> texts = ActionBarData.getFilteredTexts();
            
            if (texts == null || texts.isEmpty()) {
                // Use default height if no materials
                float scale = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerScale;
                if (scale <= 0) scale = 1.0f; // Sicherheitscheck
                return (int) ((DEFAULT_HEIGHT - 23) * scale);
            }
            
            // Calculate height based on number of materials
            // Base height for title and padding
            int baseHeight = 20; // Title height
            int materialHeight = texts.size() * 13; // 13px per material (LINE_HEIGHT from MaterialTrackerUtility)
            int bottomPadding = 10; // Bottom padding
            
            int totalHeight = baseHeight + materialHeight + bottomPadding;
            
            // Apply scale factor (same logic as MaterialTrackerUtility)
            float scale = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerScale;
            if (scale <= 0) scale = 1.0f; // Sicherheitscheck
            return (int) (totalHeight * scale);
        } catch (Exception e) {
            // Fallback to default height if calculation fails
            float scale = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerScale;
            if (scale <= 0) scale = 1.0f; // Sicherheitscheck
            return (int) ((DEFAULT_HEIGHT - 23) * scale);
        }
    }
}



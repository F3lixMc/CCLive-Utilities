package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.ActionBarData;
import net.felix.utilities.DraggableOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
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
        
        // Adjust X position so overlay expands to the left (stays at right edge)
        return screenWidth - dynamicWidth - xOffset;
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
        int xOffset = screenWidth - dynamicWidth - x;
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
        
        // Render background
        net.felix.OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerOverlayType;
        
        switch (overlayType) {
            case CUSTOM:
                try {
                    context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        MATERIALS_BACKGROUND_TEXTURE,
                        x, y,
                        0.0f, 0.0f,
                        width, height,
                        width, height
                    );
                } catch (Exception e) {
                    context.fill(x, y, x + width, y + height, 0x80000000);
                }
                break;
            case BLACK:
                context.fill(x, y, x + width, y + height, 0x80000000);
                break;
            case NONE:
                // No background
                break;
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
        
        // Render real material data if available, otherwise sample data
        renderMaterialData(context, x, y);
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
     * Uses simple positioning like normal overlays
     */
    private void renderMaterialData(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        try {
            // Try to get real material data from ActionBarData
            List<Object> materials = ActionBarData.getFilteredTexts();
            
            if (materials != null && !materials.isEmpty()) {
                // Render real data (limit to 3 items for edit mode)
                int materialY = y + 20;
                int count = 0;
                
                for (Object materialObj : materials) {
                    if (count >= 3) break; // Limit to 3 items for edit mode
                    
                    String materialText;
                    if (materialObj instanceof net.minecraft.text.Text) {
                        materialText = ((net.minecraft.text.Text) materialObj).getString();
                    } else {
                        materialText = materialObj.toString();
                    }
                    
                    // Truncate if too long
                    if (materialText.length() > 25) {
                        materialText = materialText.substring(0, 22) + "...";
                    }
                    
                    context.drawText(
                        client.textRenderer,
                        materialText,
                        x + 8, materialY,
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
                        x + 8, materialY,
                        0xFF888888,
                        true
                    );
                }
                
                return; // Successfully rendered real data
            }
        } catch (Exception e) {
            // Fall through to sample data
        }
        
        // Render sample data if real data is not available
        context.drawText(
            client.textRenderer,
            "Prächtiges Eselhaar [1067]",
            x + 8, y + 20,
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            client.textRenderer,
            "Goldener Drache [234]",
            x + 8, y + 32,
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            client.textRenderer,
            "Mystisches Holz [89]",
            x + 8, y + 44,
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
                return DEFAULT_WIDTH;
            }
            
            // Calculate width based on text content (exact same logic as MaterialTrackerUtility)
            int dynamicWidth = calculateRequiredWidth(client, texts);
            float scale = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerScale;
            int scaledOverlayWidth = (int) (Math.max(DEFAULT_WIDTH, dynamicWidth) * scale);
            int actualOverlayWidth = Math.max(DEFAULT_WIDTH, scaledOverlayWidth);
            
            return actualOverlayWidth;
        } catch (Exception e) {
            // Fallback to default width if calculation fails
            return DEFAULT_WIDTH;
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
            return (int) (totalHeight * scale);
        } catch (Exception e) {
            // Fallback to default height if calculation fails
            float scale = CCLiveUtilitiesConfig.HANDLER.instance().materialTrackerScale;
            return (int) ((DEFAULT_HEIGHT - 23) * scale);
        }
    }
}

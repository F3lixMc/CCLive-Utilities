package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.BPViewerUtility;
import net.felix.utilities.DraggableOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Draggable Overlay für den Blueprint Viewer
 */
public class BlueprintViewerDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 100;
    private static final Identifier BLUEPRINT_BACKGROUND_TEXTURE = Identifier.of("cclive-utilities", "textures/gui/blueprint_background.png");
    
    @Override
    public String getOverlayName() {
        return "Blueprint Viewer";
    }
    
    @Override
    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerX;
        int dynamicWidth = getWidth();
        
        // Adjust X position so overlay expands to the left (stays at right edge)
        return screenWidth - dynamicWidth - xOffset;
    }
    
    @Override
    public int getY() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenHeight = client.getWindow().getScaledHeight();
        int yPercent = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerY;
        
        // Calculate Y position from percent
        int y = screenHeight * yPercent / 100;
        
        // If the overlay is positioned near the bottom (high percentage), 
        // adjust it to account for the dynamic height
        if (yPercent >= 90) {
            // Position at the bottom edge, accounting for the actual overlay height
            y = screenHeight - getHeight();
        }
        
        return Math.max(0, y);
    }
    
    @Override
    public int getWidth() {
        // Calculate dynamic width based on current blueprint data
        return calculateDynamicWidth();
    }
    
    @Override
    public int getHeight() {
        // Calculate dynamic height based on current blueprint data
        return calculateDynamicHeight();
    }
    
    @Override
    public void setPosition(int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int dynamicWidth = getWidth();
        
        int xOffset = screenWidth - dynamicWidth - x;
        
        // Calculate Y percent with better precision to avoid rounding errors
        int yPercent = Math.round((float) y * 100.0f / screenHeight);
        
        // Clamp to valid range (0-100)
        yPercent = Math.max(0, Math.min(100, yPercent));
        
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerY = yPercent;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render background based on overlay type
        net.felix.OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerOverlayType;
        
        if (overlayType == net.felix.OverlayType.CUSTOM) {
            try {
                context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    BLUEPRINT_BACKGROUND_TEXTURE,
                    x, y,
                    0.0f, 0.0f,
                    width, height,
                    width, height
                );
            } catch (Exception e) {
                context.fill(x, y, x + width, y + height, 0x80000000);
            }
        } else if (overlayType == net.felix.OverlayType.BLACK) {
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
        
        // Render real blueprint data if available, otherwise sample data
        renderBlueprintData(context, x, y, width, height);
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerEnabled && 
               CCLiveUtilitiesConfig.HANDLER.instance().showBlueprintViewer;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Blueprint Viewer - Shows blueprint information and materials");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerX = 1;
        CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerY = 2; // 2% vom oberen Rand
    }
    
    /**
     * Calculate dynamic width based on current blueprint data (same logic as BPViewerUtility)
     */
    private int calculateDynamicWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return DEFAULT_WIDTH;
        
        try {
            // Get current blueprint data using reflection
            BPViewerUtility instance = BPViewerUtility.getInstance();
            String activeFloor = instance.getActiveFloor();
            
            if (activeFloor == null) {
                return DEFAULT_WIDTH;
            }
            
            // Use reflection to access the private config field
            java.lang.reflect.Field configField = BPViewerUtility.class.getDeclaredField("config");
            configField.setAccessible(true);
            BPViewerUtility.BlueprintConfig config = (BPViewerUtility.BlueprintConfig) configField.get(instance);
            
            BPViewerUtility.BlueprintConfig.FloorData floorData = config.getFloorData(activeFloor);
            
            if (floorData == null || floorData.blueprints == null) {
                return DEFAULT_WIDTH;
            }
            
            // Get current rarity data
            String currentRarity = getCurrentRarity();
            BPViewerUtility.BlueprintConfig.RarityData rarityData = floorData.blueprints.get(currentRarity);
            
            if (rarityData == null || rarityData.items == null) {
                return DEFAULT_WIDTH;
            }
            
            // Calculate width based on text content (same logic as BPViewerUtility)
            int maxWidth = DEFAULT_WIDTH;
            for (String blueprint : rarityData.items) {
                String displayText = blueprint.startsWith("- ") ? blueprint.substring(2) : blueprint;
                int textWidth = client.textRenderer.getWidth(displayText);
                // Add padding for the text (left margin + right margin) - increased for longer German text
                textWidth += 45;
                if (textWidth > maxWidth) {
                    maxWidth = textWidth;
                }
            }
            
            // Apply the same -15px adjustment as in BPViewerUtility
            int baseWidth = maxWidth - 15;
            
            // Apply scale factor to match the visual size in the real overlay
            float scale = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerScale;
            if (scale <= 0) scale = 1.0f; // Sicherheitscheck
            
            return (int) (baseWidth * scale);
        } catch (Exception e) {
            // Fallback to default width if reflection fails
            return DEFAULT_WIDTH;
        }
    }
    
    /**
     * Calculate dynamic height based on current blueprint data (same logic as BPViewerUtility)
     */
    private int calculateDynamicHeight() {
        try {
            // Get current blueprint data using reflection
            BPViewerUtility instance = BPViewerUtility.getInstance();
            String activeFloor = instance.getActiveFloor();
            
            if (activeFloor == null) {
                return DEFAULT_HEIGHT;
            }
            
            // Use reflection to access the private config field
            java.lang.reflect.Field configField = BPViewerUtility.class.getDeclaredField("config");
            configField.setAccessible(true);
            BPViewerUtility.BlueprintConfig config = (BPViewerUtility.BlueprintConfig) configField.get(instance);
            
            BPViewerUtility.BlueprintConfig.FloorData floorData = config.getFloorData(activeFloor);
            
            if (floorData == null || floorData.blueprints == null) {
                return DEFAULT_HEIGHT;
            }
            
            // Get current rarity data
            String currentRarity = getCurrentRarity();
            BPViewerUtility.BlueprintConfig.RarityData rarityData = floorData.blueprints.get(currentRarity);
            
            if (rarityData == null || rarityData.items == null) {
                return DEFAULT_HEIGHT;
            }
            
            // Calculate height based on blueprint count (same logic as BPViewerUtility)
            int baseHeight = 20;
            int blueprintHeight = rarityData.items.size() * 12;
            int bottomPadding = 5;
            
            int totalHeight = baseHeight + blueprintHeight + bottomPadding;
            
            // Apply scale factor to match the visual size in the real overlay
            float scale = CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerScale;
            if (scale <= 0) scale = 1.0f; // Sicherheitscheck
            
            return (int) (totalHeight * scale);
        } catch (Exception e) {
            // Fallback to default height if reflection fails
            return DEFAULT_HEIGHT;
        }
    }
    
    /**
     * Get current rarity (same logic as BPViewerUtility)
     */
    private String getCurrentRarity() {
        // Try to get current rarity from BPViewerUtility instance
        try {
            BPViewerUtility instance = BPViewerUtility.getInstance();
            // Use reflection to access the private currentRarity field
            java.lang.reflect.Field field = BPViewerUtility.class.getDeclaredField("currentRarity");
            field.setAccessible(true);
            return (String) field.get(instance);
        } catch (Exception e) {
            // Fallback to default rarity
            return "common";
        }
    }
    
    /**
     * Render real blueprint data if available, otherwise sample data
     */
    private void renderBlueprintData(DrawContext context, int x, int y, int width, int height) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        try {
            // Try to get real blueprint data
            BPViewerUtility instance = BPViewerUtility.getInstance();
            String activeFloor = instance.getActiveFloor();
            
            if (activeFloor != null) {
                // Use reflection to access the private config field
                java.lang.reflect.Field configField = BPViewerUtility.class.getDeclaredField("config");
                configField.setAccessible(true);
                BPViewerUtility.BlueprintConfig config = (BPViewerUtility.BlueprintConfig) configField.get(instance);
                
                BPViewerUtility.BlueprintConfig.FloorData floorData = config.getFloorData(activeFloor);
                
                if (floorData != null && floorData.blueprints != null) {
                    String currentRarity = getCurrentRarity();
                    BPViewerUtility.BlueprintConfig.RarityData rarityData = floorData.blueprints.get(currentRarity);
                    
                    if (rarityData != null && rarityData.items != null && !rarityData.items.isEmpty()) {
                        // Render real data
                        String rarityDisplay = currentRarity.toUpperCase();
                        context.drawText(
                            client.textRenderer,
                            rarityDisplay,
                            x + 8, y + 20,
                            getRarityColor(currentRarity),
                            true
                        );
                        
                        // Render first few blueprints (limit to fit in edit mode)
                        int blueprintY = y + 25;
                        int count = 0;
                        int maxItems = Math.min(3, (height - 30) / 12); // Calculate how many items fit
                        
                        for (String blueprint : rarityData.items) {
                            if (count >= maxItems) break;
                            
                            String displayText = blueprint.startsWith("- ") ? blueprint.substring(2) : blueprint;
                            // Truncate if too long for the available width
                            int availableWidth = width - 16; // 8px margin on each side
                            int textWidth = client.textRenderer.getWidth(displayText);
                            if (textWidth > availableWidth) {
                                // Find the maximum characters that fit
                                int maxChars = (int) ((double) availableWidth / textWidth * displayText.length());
                                if (maxChars > 3) {
                                    displayText = displayText.substring(0, maxChars - 3) + "...";
                                } else {
                                    displayText = "...";
                                }
                            }
                            
                            context.drawText(
                                client.textRenderer,
                                displayText,
                                x + 8, blueprintY,
                                0xFFFFFFFF,
                                true
                            );
                            
                            blueprintY += 12;
                            count++;
                        }
                        
                        if (rarityData.items.size() > maxItems) {
                            context.drawText(
                                client.textRenderer,
                                "... and " + (rarityData.items.size() - maxItems) + " more",
                                x + 8, blueprintY,
                                0xFF888888,
                                true
                            );
                        }
                        
                        return; // Successfully rendered real data
                    }
                }
            }
        } catch (Exception e) {
            // Fall through to sample data
        }
        
        // Render sample data if real data is not available (adapt to dynamic size)
        context.drawText(
            client.textRenderer,
            "COMMON",
            x + 8, y + 5,
            0xFFFFFFFF,
            true
        );
        
        int blueprintY = y + 25;
        int maxItems = Math.min(3, (height - 30) / 12); // Calculate how many items fit
        
        if (maxItems >= 1) {
            String text1 = "Anfänger Hacke";
            int availableWidth = width - 16;
            int textWidth = client.textRenderer.getWidth(text1);
            if (textWidth > availableWidth) {
                int maxChars = (int) ((double) availableWidth / textWidth * text1.length());
                if (maxChars > 3) {
                    text1 = text1.substring(0, maxChars - 3) + "...";
                } else {
                    text1 = "...";
                }
            }
            context.drawText(
                client.textRenderer,
                text1,
                x + 8, blueprintY,
                0xFFFFFFFF,
                true
            );
            blueprintY += 12;
        }
        
        if (maxItems >= 2) {
            String text2 = "Anfänger Axt";
            int availableWidth = width - 16;
            int textWidth = client.textRenderer.getWidth(text2);
            if (textWidth > availableWidth) {
                int maxChars = (int) ((double) availableWidth / textWidth * text2.length());
                if (maxChars > 3) {
                    text2 = text2.substring(0, maxChars - 3) + "...";
                } else {
                    text2 = "...";
                }
            }
            context.drawText(
                client.textRenderer,
                text2,
                x + 8, blueprintY,
                0xFFFFFFFF,
                true
            );
            blueprintY += 12;
        }
        
        if (maxItems >= 3) {
            context.drawText(
                client.textRenderer,
                "... and more",
                x + 8, blueprintY,
                0xFF888888,
                true
            );
        }
    }
    
    /**
     * Get color for rarity (same as BPViewerUtility)
     */
    private int getRarityColor(String rarity) {
        switch (rarity.toLowerCase()) {
            case "common": return 0xFFFFFFFF;      // White
            case "uncommon": return 0xFF00FF00;    // Green
            case "rare": return 0xFF0000FF;        // Blue
            case "epic": return 0xFFFF00FF;        // Magenta
            case "legendary": return 0xFFFFFF00;   // Yellow
            default: return 0xFFFFFFFF;            // White as default
        }
    }
}

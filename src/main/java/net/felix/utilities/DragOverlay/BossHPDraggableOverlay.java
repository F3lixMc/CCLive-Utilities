package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DraggableOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Draggable Overlay für die Boss HP Anzeige
 */
public class BossHPDraggableOverlay implements DraggableOverlay {
    
    private static final int PADDING = 4;
    
    @Override
    public String getOverlayName() {
        return "";
    }
    
    @Override
    public int getX() {
        // Return the left edge position for dragging (right edge minus width)
        int rightEdge = CCLiveUtilitiesConfig.HANDLER.instance().bossHPX;
        int width = getWidth();
        return rightEdge - width;
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().bossHPY;
    }
    
        @Override
    public int getWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 200; // Fallback width

        // Get actual boss data to calculate real width (exactly like the standard overlay)
        BossData bossData = getCurrentBossData();
        if (bossData != null) {
            String bossText = bossData.getBossText();

            if (bossText != null && !bossText.isEmpty()) {
                String[] parts = bossText.split("\\|{5}");
                if (parts.length >= 2) {
                    String displayText = parts[0].trim();
                    String displayHP = parts[1].trim();

                    int nameWidth = client.textRenderer.getWidth(displayText);
                    int hpWidth = displayHP.isEmpty() ? 0 : client.textRenderer.getWidth(displayHP);
                    // Use exact same calculation as standard overlay
                    int totalWidth = nameWidth + (displayHP.isEmpty() ? 0 : 10 + hpWidth);
                    return totalWidth + PADDING * 2;
                }
            }
        }

        // No boss data available
        
        // If no boss data and no test overlay, return minimum width (like standard overlay when nothing is shown)
        return 50; // Minimum width when nothing is displayed
    }
    
    @Override
    public int getHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 20; // Fallback height
        
        return client.textRenderer.fontHeight + PADDING * 2;
    }
    
    @Override
    public void setPosition(int x, int y) {
        // x is the left edge position from dragging, convert to right edge for storage
        int width = getWidth();
        CCLiveUtilitiesConfig.HANDLER.instance().bossHPX = x + width;
        CCLiveUtilitiesConfig.HANDLER.instance().bossHPY = y;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX(); // This is now the left edge position
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render background at the left edge position
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // Render border for edit mode
        context.drawBorder(x, y, width, height, 0xFFFF0000);
        
        // Don't render overlay name - show only the actual content
        
        // Render sample boss data - use the right edge position like standard overlay
        int rightEdgeX = x + width;
        renderBossData(context, rightEdgeX, y);
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().bossHPEnabled && 
               CCLiveUtilitiesConfig.HANDLER.instance().showBossHP;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Boss HP - Shows boss health information");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().bossHPX = 562;
        CCLiveUtilitiesConfig.HANDLER.instance().bossHPY = 201;
    }
    
    /**
     * Render actual boss data for preview - exactly like the standard overlay
     */
    private void renderBossData(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Get boss data exactly like the standard overlay
        BossData bossData = getCurrentBossData();
        
        if (bossData != null && bossData.getName() != null) {
            // Parse the data exactly like the standard overlay does
            String bossText = bossData.getBossText();
            
            String displayText = null;
            String displayHP = null;
            
            if (bossText != null && !bossText.isEmpty()) {
                String[] parts = bossText.split("\\|{5}");
                if (parts.length >= 2) {
                    displayText = parts[0].trim();
                    displayHP = parts[1].trim();
                }
            }
            
                                    if (displayText != null) {
                            // Render exactly like the standard overlay - with left-growing logic
                            int nameWidth = client.textRenderer.getWidth(displayText);
                            int hpWidth = displayHP.isEmpty() ? 0 : client.textRenderer.getWidth(displayHP);
                            int totalWidth = nameWidth + (displayHP.isEmpty() ? 0 : 10 + hpWidth);
                            
                            // Calculate position like standard overlay - grows to the left
                            int renderX = x - totalWidth - PADDING * 2;

                            // Render boss name (white)
                            context.drawText(
                                client.textRenderer,
                                displayText,
                                renderX + PADDING, y + PADDING,
                                0xFFFFFFFF,
                                true
                            );

                            // Render HP (red) if available - use exact same spacing as standard overlay
                            if (!displayHP.isEmpty()) {
                                context.drawText(
                                    client.textRenderer,
                                    displayHP,
                                    renderX + PADDING + nameWidth + 10, y + PADDING,
                                    0xFFFF5555,
                                    true
                                );
                            }
                        }
                } else {
            // No boss data to display
            // If no boss data and no test overlay, don't render anything (like standard overlay)
        }
    }
    
    /**
     * Data class for boss information
     */
    private static class BossData {
        private final String name;
        private final String hp;
        private final String bossText;
        
        public BossData(String name, String hp, String bossText) {
            this.name = name;
            this.hp = hp;
            this.bossText = bossText;
        }
        
        public String getName() {
            return name;
        }
        
        public String getHP() {
            return hp;
        }
        
        public String getBossText() {
            return bossText;
        }
    }
    
    /**
     * Get current boss data from BossHPUtility using public methods
     * Only returns real boss data, no test content
     */
    private BossData getCurrentBossData() {
        try {
            // Get real boss data using public methods
            String bossName = net.felix.utilities.BossHPUtility.getCurrentBossName();
            String bossText = net.felix.utilities.BossHPUtility.getCurrentBossText();
            
            // Only return real boss data, no test content
            if (bossName != null && bossText != null && !bossName.isEmpty() && !bossText.isEmpty()) {
                // Parse HP from boss text (format: "BossName|||||HP|||||")
                String[] parts = bossText.split("\\|{5}");
                if (parts.length >= 2) {
                    String hp = parts[1].trim();
                    return new BossData(bossName, hp, bossText);
                }
            }
            
            // No real boss data available
            
            return null;
        } catch (Exception e) {
            // If getting data fails, return null
            return null;
        }
    }
}

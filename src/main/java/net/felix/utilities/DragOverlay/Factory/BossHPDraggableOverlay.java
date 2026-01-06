package net.felix.utilities.DragOverlay.Factory;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

/**
 * Draggable Overlay für die Boss HP Anzeige
 */
public class BossHPDraggableOverlay implements DraggableOverlay {
    
    private static final int PADDING = 4;
    
    @Override
    public String getOverlayName() {
        return "BossHP";
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
    
    /**
     * Calculate unscaled width (without applying scale factor)
     */
    private int calculateUnscaledWidth() {
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
                    int firstLineWidth = nameWidth + (displayHP.isEmpty() ? 0 : 10 + hpWidth);
                    
                    // Include DPM line width (only if DPM is enabled)
                    int totalWidth = firstLineWidth;
                    if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowDPM) {
                        String dpmText = "DPM: XXXX";
                        int dpmWidth = client.textRenderer.getWidth(dpmText);
                        totalWidth = Math.max(firstLineWidth, dpmWidth);
                    }
                    
                    return totalWidth + PADDING * 2;
                }
            }
        }

        // No boss data available - calculate width for preview text "Boss-Name: XXXX" and optionally "DPM: XXXX"
        String previewText = "Boss-Name: XXXX";
        int previewWidth = client.textRenderer.getWidth(previewText);
        if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowDPM) {
            String dpmText = "DPM: XXXX";
            previewWidth = Math.max(previewWidth, client.textRenderer.getWidth(dpmText));
        }
        return previewWidth + PADDING * 2;
    }
    
    /**
     * Calculate unscaled height (without applying scale factor)
     */
    private int calculateUnscaledHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 20; // Fallback height
        
        final int LINE_SPACING = 2; // Abstand zwischen Zeilen
        
        // Height includes: first line (Boss-Name + HP) + optionally DPM line
        int height = client.textRenderer.fontHeight + PADDING * 2; // First line
        if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowDPM) {
            height += client.textRenderer.fontHeight + LINE_SPACING; // DPM line
        }
        
        return height;
    }
    
    @Override
    public int getWidth() {
        // Return scaled width
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().bossHPScale;
        if (scale <= 0) scale = 1.0f; // Safety check
        return Math.round(calculateUnscaledWidth() * scale);
    }
    
    @Override
    public int getHeight() {
        // Return scaled height
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().bossHPScale;
        if (scale <= 0) scale = 1.0f; // Safety check
        return Math.round(calculateUnscaledHeight() * scale);
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
        
        // Get scale
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().bossHPScale;
        if (scale <= 0) scale = 1.0f;
        
        // Get unscaled dimensions
        int unscaledWidth = calculateUnscaledWidth();
        int unscaledHeight = calculateUnscaledHeight();
        
        // Render background at the left edge position (scaled)
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // Render border for edit mode (scaled)
        context.drawBorder(x, y, width, height, 0xFFFF0000);
        
        // Render preview text "Boss-Name: XXXX" and optionally "DPM: XXXX" with scale
        // Always show "XXXX" in preview, never real HP values
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            final int LINE_SPACING = 2;
            String previewText = "Boss-Name: XXXX";
            
            // Use Matrix transformations for scaling
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            
            // Translate to position and scale from there
            matrices.translate(x, y);
            matrices.scale(scale, scale);
            
            // Render preview text (first line) - centered horizontally
            int textWidth = client.textRenderer.getWidth(previewText);
            int textX = (unscaledWidth - textWidth) / 2;
            int textY = PADDING;
            
            context.drawText(client.textRenderer, previewText, textX, textY, 0xFFFFFFFF, false);
            
            // Render DPM text (second line) - only if DPM is enabled
            if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowDPM) {
                String dpmText = "DPM: XXXX";
                int dpmTextWidth = client.textRenderer.getWidth(dpmText);
                int dpmTextX = (unscaledWidth - dpmTextWidth) / 2;
                int dpmTextY = PADDING + client.textRenderer.fontHeight + LINE_SPACING;
                
                context.drawText(client.textRenderer, dpmText, dpmTextX, dpmTextY, 0xFFFFFF00, false); // Gelb für DPM
            }
            
            matrices.popMatrix();
        }
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
        CCLiveUtilitiesConfig.HANDLER.instance().bossHPX = 854;
        CCLiveUtilitiesConfig.HANDLER.instance().bossHPY = 261;
    }
    
    @Override
    public void setSize(int width, int height) {
        // Get current unscaled dimensions
        int unscaledWidth = calculateUnscaledWidth();
        int unscaledHeight = calculateUnscaledHeight();
        
        // Calculate scale based on width and height
        float scaleX = (float) width / unscaledWidth;
        float scaleY = (float) height / unscaledHeight;
        float scale = (scaleX + scaleY) / 2.0f;
        
        // Clamp scale to reasonable values (0.1 to 5.0)
        scale = Math.max(0.1f, Math.min(5.0f, scale));
        
        CCLiveUtilitiesConfig.HANDLER.instance().bossHPScale = scale;
    }
    
    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().bossHPScale = 1.0f;
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
            String bossName = net.felix.utilities.Factory.BossHPUtility.getCurrentBossName();
            String bossText = net.felix.utilities.Factory.BossHPUtility.getCurrentBossText();
            
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

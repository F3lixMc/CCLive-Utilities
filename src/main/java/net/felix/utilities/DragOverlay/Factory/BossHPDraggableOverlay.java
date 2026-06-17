package net.felix.utilities.DragOverlay.Factory;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

import java.math.BigInteger;
import java.util.Locale;

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
        // Calculate X position using the same logic as Mining/Holzfäller overlays
        // baseX is the left edge position (like Mining overlays)
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return CCLiveUtilitiesConfig.HANDLER.instance().bossHPX;
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
        int baseX = CCLiveUtilitiesConfig.HANDLER.instance().bossHPX;
        int overlayWidth = getWidth(); // Use scaled width for positioning
        
        // Determine if overlay is on left or right side of screen
        boolean isOnLeftSide = baseX < screenWidth / 2;
        
        // Calculate X position based on side (same logic as Mining overlays)
        int x;
        if (isOnLeftSide) {
            // On left side: keep left edge fixed, expand to the right
            x = baseX;
        } else {
            // On right side: keep right edge fixed, expand to the left
            // Right edge is: baseX (since baseX is on the right side, it represents the right edge)
            // Keep this right edge fixed, so left edge moves left when width increases
            x = baseX - overlayWidth;
        }
        
        // Ensure overlay stays within screen bounds
        return Math.max(0, Math.min(x, screenWidth - overlayWidth));
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().bossHPY;
    }
    
    /**
     * Calculate unscaled width (without applying scale factor)
     * Uses actual boss data to calculate real width dynamically
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
                    String rawHP = parts[1].trim();
                    
                    // Format HP with thousand separator (exactly like standard overlay)
                    String displayHP = "";
                    BigInteger currentHP = null;
                    try {
                        currentHP = new BigInteger(rawHP);
                        displayHP = net.felix.utilities.Factory.BossHPUtility.formatBigInteger(currentHP);
                    } catch (NumberFormatException e) {
                        displayHP = rawHP;
                    }

                    String nameWithColon = displayHP.isEmpty() ? displayText : (displayText + ": ");
                    int nameWidth = client.textRenderer.getWidth(nameWithColon);
                    int hpWidth = displayHP.isEmpty() ? 0 : client.textRenderer.getWidth(displayHP);
                    
                    // Calculate percentage text (exactly like standard overlay)
                    String percentageText = null;
                    BigInteger initialBossHP = net.felix.utilities.Factory.BossHPUtility.getInitialBossHP();
                    if (!displayHP.isEmpty() && 
                        net.felix.utilities.Factory.BossHPUtility.isBossActive() &&
                        CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowPercentage &&
                        initialBossHP != null && currentHP != null && 
                        initialBossHP.compareTo(BigInteger.ZERO) > 0) {
                        // Calculate percentage: (currentHP / initialBossHP) * 100
                        double percentage = currentHP.doubleValue() / initialBossHP.doubleValue() * 100.0;
                        // Format with one decimal place
                        percentageText = String.format(Locale.US, "%.1f%%", percentage);
                    }
                    
                    int separatorWidth = percentageText != null ? client.textRenderer.getWidth("|") : 0;
                    int percentageWidth = percentageText != null ? client.textRenderer.getWidth(percentageText) : 0;
                    
                    // Use exact same calculation as standard overlay (including percentage if applicable)
                    int firstLineWidth = nameWidth + (displayHP.isEmpty() ? 0 : 4 + hpWidth + 
                        (percentageText != null ? 5 + separatorWidth + 5 + percentageWidth : 0));
                    
                    // Calculate DPM text (exactly like standard overlay)
                    String dpmText = null;
                    if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowDPM && 
                        initialBossHP != null && currentHP != null) {
                        long bossFightStartTime = net.felix.utilities.Factory.BossHPUtility.getBossFightStartTime();
                        if (bossFightStartTime > 0) {
                            long currentTime = System.currentTimeMillis();
                            long fightDuration = currentTime - bossFightStartTime;
                            
                            if (fightDuration > 0) {
                                // Calculate damage dealt
                                BigInteger damageDealt = initialBossHP.subtract(currentHP);
                                
                                // Calculate DPM (Damage Per Minute)
                                double minutes = fightDuration / 60000.0;
                                if (minutes > 0) {
                                    double damageDealtDouble = damageDealt.doubleValue();
                                    double dpm = damageDealtDouble / minutes;
                                    // Format DPM with thousand separator
                                    dpmText = String.format("DPM: %,.0f", dpm);
                                }
                            }
                        }
                    }
                    
                    int dpmWidth = dpmText != null ? client.textRenderer.getWidth(dpmText) : 0;
                    
                    String lastDmgLine = null;
                    String overallDmgLine = null;
                    if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowLastDmg && initialBossHP != null && currentHP != null) {
                        java.math.BigInteger ld = net.felix.utilities.Factory.BossHPUtility.getDisplayedLastDamage();
                        String lastVal = ld == null ? "-" : net.felix.utilities.Factory.BossHPUtility.formatBigInteger(ld);
                        int cd = net.felix.utilities.Factory.BossHPUtility.getLastDmgIntervalCountdown();
                        lastDmgLine = "Last Dmg: " + lastVal + " | " + cd + "s";
                    }
                    if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowOverallDmg && initialBossHP != null && currentHP != null
                        && initialBossHP.compareTo(BigInteger.ZERO) > 0) {
                        BigInteger totalDealt = initialBossHP.subtract(currentHP);
                        if (totalDealt.compareTo(BigInteger.ZERO) < 0) {
                            totalDealt = BigInteger.ZERO;
                        }
                        double removedPct = totalDealt.doubleValue() / initialBossHP.doubleValue() * 100.0;
                        overallDmgLine = "Overall DMG: " + net.felix.utilities.Factory.BossHPUtility.formatBigInteger(totalDealt)
                            + " | " + String.format(Locale.US, "%.1f%%", removedPct);
                    }
                    int lastDmgWidth = lastDmgLine != null ? client.textRenderer.getWidth(lastDmgLine) : 0;
                    int overallDmgWidth = overallDmgLine != null ? client.textRenderer.getWidth(overallDmgLine) : 0;
                    
                    int totalWidth = Math.max(
                        firstLineWidth,
                        Math.max(dpmWidth, Math.max(lastDmgWidth, overallDmgWidth))
                    );
                    
                    return totalWidth + PADDING * 2;
                }
            }
        }

        // No boss data available - calculate width for preview text (compact preview)
        // Use smaller example values for preview to keep overlay compact
        String previewText = "Boss-Name: XXXX";
        int previewWidth = client.textRenderer.getWidth(previewText);
        
        // Add HP width estimate (use smaller value for preview)
        String previewHP = "100,000";
        int previewHPWidth = client.textRenderer.getWidth(previewHP);
        previewWidth += 10 + previewHPWidth; // 10 pixels spacing + HP width
        
        // Add percentage width if enabled (use smaller value for preview)
        if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowPercentage) {
            String previewPercentageText = "50.0%";
            int percentageWidth = client.textRenderer.getWidth(previewPercentageText);
            int separatorWidth = client.textRenderer.getWidth("|");
            previewWidth += 5 + separatorWidth + 5 + percentageWidth; // Spacing + separator + spacing + percentage
        }
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowDPM) {
            String dpmText = "DPM: XXXX";
            previewWidth = Math.max(previewWidth, client.textRenderer.getWidth(dpmText));
        }
        if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowLastDmg) {
            previewWidth = Math.max(previewWidth, client.textRenderer.getWidth("Last Dmg: 9,999,999 | 2s"));
        }
        if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowOverallDmg) {
            previewWidth = Math.max(previewWidth, client.textRenderer.getWidth("Overall DMG: 9,999,999 | 100.0%"));
        }
        return previewWidth + PADDING * 2;
    }
    
    /**
     * Calculate unscaled height (without applying scale factor)
     * Uses actual boss data to calculate real height dynamically
     */
    private int calculateUnscaledHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 20; // Fallback height
        
        final int LINE_SPACING = 2; // Abstand zwischen Zeilen
        
        // Height includes: first line (Boss-Name + HP) + optionally DPM line
        int height = client.textRenderer.fontHeight + PADDING * 2; // First line
        
        // Check if DPM should be displayed
        boolean shouldShowDPM = false;
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowDPM) {
            BossData bossData = getCurrentBossData();
            
            // If boss data is available, check if DPM should actually be displayed (exactly like standard overlay)
            if (bossData != null && net.felix.utilities.Factory.BossHPUtility.isBossActive()) {
                String bossText = bossData.getBossText();
                if (bossText != null && !bossText.isEmpty()) {
                    String[] parts = bossText.split("\\|{5}");
                    if (parts.length >= 2) {
                        try {
                            BigInteger currentHP = new BigInteger(parts[1].trim());
                            BigInteger initialBossHP = net.felix.utilities.Factory.BossHPUtility.getInitialBossHP();
                            long bossFightStartTime = net.felix.utilities.Factory.BossHPUtility.getBossFightStartTime();
                            
                            // DPM is shown if initialBossHP, currentHP, and fightStartTime are available
                            if (initialBossHP != null && currentHP != null && bossFightStartTime > 0) {
                                long currentTime = System.currentTimeMillis();
                                long fightDuration = currentTime - bossFightStartTime;
                                if (fightDuration > 0) {
                                    double minutes = fightDuration / 60000.0;
                                    if (minutes > 0) {
                                        shouldShowDPM = true;
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                            // HP could not be parsed, DPM won't be shown
                        }
                    }
                }
            } else {
                // No boss data available (preview mode) - show DPM line if option is enabled
                // This ensures the preview text "DPM: XXXX" is visible in the overlay
                shouldShowDPM = true;
            }
        }
        
        if (shouldShowDPM) {
            height += client.textRenderer.fontHeight + LINE_SPACING; // DPM line
        }
        int damageLineCount = 0;
        if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowLastDmg) {
            damageLineCount++;
        }
        if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowOverallDmg) {
            damageLineCount++;
        }
        height += (client.textRenderer.fontHeight + LINE_SPACING) * damageLineCount;
        
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
        // Calculate baseX using the same logic as Mining/Holzfäller overlays
        // We need to reverse the calculation: from the actual x position, calculate baseX
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            CCLiveUtilitiesConfig.HANDLER.instance().bossHPX = x;
            CCLiveUtilitiesConfig.HANDLER.instance().bossHPY = y;
            return;
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
        int overlayWidth = getWidth(); // Use scaled width for positioning
        
        // Determine if overlay is on left or right side of screen
        boolean isOnLeftSide = x < screenWidth / 2;
        
        // Calculate baseX based on side (reverse of getX() calculation)
        int baseX;
        if (isOnLeftSide) {
            // On left side: baseX is the same as x (left edge)
            baseX = x;
        } else {
            // On right side: baseX is the right edge (x + overlayWidth)
            // We store the right edge as baseX so it stays fixed when width changes
            baseX = x + overlayWidth;
        }
        
        CCLiveUtilitiesConfig.HANDLER.instance().bossHPX = baseX;
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
            
            int lineY = PADDING + client.textRenderer.fontHeight + LINE_SPACING;
            // Render DPM text (second line) - only if DPM is enabled
            if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowDPM) {
                String dpmText = "DPM: XXXX";
                int dpmTextWidth = client.textRenderer.getWidth(dpmText);
                int dpmTextX = (unscaledWidth - dpmTextWidth) / 2;
                
                context.drawText(client.textRenderer, dpmText, dpmTextX, lineY, 0xFFFFFF00, false); // Gelb für DPM
                lineY += client.textRenderer.fontHeight + LINE_SPACING;
            }
            if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowLastDmg) {
                String lastPreview = "Last Dmg: XXXX | 2s";
                int lw = client.textRenderer.getWidth(lastPreview);
                context.drawText(client.textRenderer, lastPreview, (unscaledWidth - lw) / 2, lineY, 0xFFFFFF00, false);
                lineY += client.textRenderer.fontHeight + LINE_SPACING;
            }
            if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowOverallDmg) {
                String overallPreview = "Overall DMG: XXXX | XX.X%";
                int ow = client.textRenderer.getWidth(overallPreview);
                context.drawText(client.textRenderer, overallPreview, (unscaledWidth - ow) / 2, lineY, 0xFFFFFF00, false);
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
     * Data class for boss information
     */
    private static class BossData {
        private final String bossText;
        
        public BossData(String bossText) {
            this.bossText = bossText;
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
                    return new BossData(bossText);
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

package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.CardsStatuesUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Draggable Overlay für die Statues
 */
public class StatuesDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 162;
    private static final int DEFAULT_HEIGHT = 62;
    private static final Identifier STATUES_BACKGROUND_TEXTURE = Identifier.of("cclive-utilities", "textures/gui/statuen_background.png");
    
    @Override
    public String getOverlayName() {
        return "Statues";
    }
    
    @Override
    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().statueX;
        // Use the same position calculation as CardsStatuesUtility line 454
        // Position is calculated without -11 offset, but we need to account for it visually
        int baseX = screenWidth - xOffset;
        // Apply -11 offset for visual representation (same as renderStatueBackground line 115)
        return baseX - 11;
    }
    
    @Override
    public int getY() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenHeight = client.getWindow().getScaledHeight();
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().statueY;
        // Use the same position calculation as CardsStatuesUtility line 455
        // Position is calculated without -11 offset, but we need to account for it visually
        int baseY = screenHeight - yOffset;
        // Apply -11 offset for visual representation (same as renderStatueBackground line 115)
        return baseY - 11;
    }
    
    @Override
    public int getWidth() {
        // Apply scale factor to match the visual size in the real overlay
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayScale;
        if (scale <= 0) scale = 1.0f; // Sicherheitscheck
        return (int) (DEFAULT_WIDTH * scale);
    }
    
    @Override
    public int getHeight() {
        // Apply scale factor to match the visual size in the real overlay
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayScale;
        if (scale <= 0) scale = 1.0f; // Sicherheitscheck
        return (int) (DEFAULT_HEIGHT * scale);
    }
    
    @Override
    public void setPosition(int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        int xOffset = screenWidth - x - 11; // +11 für Textur-Offset
        int yOffset = screenHeight - y - 11; // +11 für Textur-Offset
        
        CCLiveUtilitiesConfig.HANDLER.instance().statueX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().statueY = yOffset;
    }
    
    @Override
    public void setSize(int width, int height) {
        // Calculate scale based on width (or height if width is not available)
        // Use average of width and height scale for better consistency
        float scaleX = (float) width / DEFAULT_WIDTH;
        float scaleY = (float) height / DEFAULT_HEIGHT;
        float scale = (scaleX + scaleY) / 2.0f;
        
        // Clamp scale to reasonable values (0.1 to 5.0)
        scale = Math.max(0.1f, Math.min(5.0f, scale));
        
        CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayScale = scale;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        // Use the same position calculation as the actual overlay (without -11 offset in position)
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().statueX;
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().statueY;
        
        // Position without -11 offset (same as CardsStatuesUtility line 454)
        int baseX = screenWidth - xOffset;
        int baseY = screenHeight - yOffset;
        
        // Get scale
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayScale;
        if (scale <= 0) scale = 1.0f;
        
        // Use Matrix transformations for scaling (same as CardsStatuesUtility)
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        
        // Translate to position and scale from there (same as renderStatueBackground line 107-108)
        matrices.translate(baseX, baseY);
        matrices.scale(scale, scale);
        
        // Render background based on overlay type (scaled, relative to matrix)
        net.felix.OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayType;
        
        if (overlayType == net.felix.OverlayType.CUSTOM) {
            try {
                context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    STATUES_BACKGROUND_TEXTURE,
                    -11, -11, // Relative position (same as renderStatueBackground line 115)
                    0.0f, 0.0f,
                    DEFAULT_WIDTH, DEFAULT_HEIGHT, // Size (will be scaled by matrix)
                    DEFAULT_WIDTH, DEFAULT_HEIGHT // Texture size
                );
            } catch (Exception e) {
                context.fill(-11, -11, -11 + DEFAULT_WIDTH, -11 + DEFAULT_HEIGHT, 0x80000000);
            }
        } else if (overlayType == net.felix.OverlayType.BLACK) {
            context.fill(-11, -11, -11 + DEFAULT_WIDTH, -11 + DEFAULT_HEIGHT, 0x80000000);
        }
        
        // Render overlay name (scaled, relative to matrix)
        // Position is relative to the matrix, which is translated to baseX, baseY
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            getOverlayName(),
            -10, -10, // Relative position (for overlay name, approximate)
            0xFFFFFFFF,
            true
        );
        
        // Render real statue data if available, otherwise sample data (scaled)
        renderStatueData(context, scale);
        
        matrices.popMatrix();
        
        // Render border AFTER the overlay to ensure it's visible on top
        // Calculate scaled dimensions and visual position for border
        // The overlay is rendered at baseX, baseY with -11 offset, then scaled
        // Visual position: baseX - 11 * scale, baseY - 11 * scale
        // Visual size: DEFAULT_WIDTH * scale, DEFAULT_HEIGHT * scale
        // Use Math.round for more accurate rounding
        int borderX = Math.round(baseX - 11 * scale);
        int borderY = Math.round(baseY - 11 * scale);
        int borderWidth = Math.round(DEFAULT_WIDTH * scale);
        int borderHeight = Math.round(DEFAULT_HEIGHT * scale);
        
        // Use drawBorder like all other overlays for consistency
        context.drawBorder(borderX, borderY, borderWidth, borderHeight, 0xFFFF0000);
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().cardsStatuesEnabled && 
               CCLiveUtilitiesConfig.HANDLER.instance().statueEnabled &&
               CCLiveUtilitiesConfig.HANDLER.instance().showStatue;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Statues - Shows statue information and levels");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().statueX = 151;
        CCLiveUtilitiesConfig.HANDLER.instance().statueY = 60;
        CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayScale = 1.0f;
    }
    
    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayScale = 1.0f;
    }
    
    /**
     * Render real statue data if available, otherwise sample data
     * Uses scaled positioning with matrix transformations (same as CardsStatuesUtility)
     */
    private void renderStatueData(DrawContext context, float scale) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Text positions are relative to the matrix (0-based, same as CardsStatuesUtility line 572)
        // The matrix is already translated to baseX, baseY and scaled
        // First line: x=1, y=-1 (same as CardsStatuesUtility)
        int lineCount = 0;
        
        try {
            // Try to get real statue data from CardsStatuesUtility
            CardsStatuesUtility.StatueData currentStatue = getCurrentStatueData();
            
            if (currentStatue != null && currentStatue.getName() != null) {
                // Render real data (scaled, relative positions, same as CardsStatuesUtility line 572)
                String statueName = currentStatue.getName();
                if (statueName.length() > 25) {
                    statueName = statueName.substring(0, 22) + "...";
                }
                
                int textY = -1 + (lineCount * 12); // Same as CardsStatuesUtility line 572
                context.drawText(
                    client.textRenderer,
                    statueName,
                    1, textY, // Same as CardsStatuesUtility line 581
                    0xFFFFFFFF,
                    true
                );
                lineCount++;
                
                if (currentStatue.getLevel() != null) {
                    textY = -1 + (lineCount * 12);
                    context.drawText(
                        client.textRenderer,
                        "Stufe: " + currentStatue.getLevel(),
                        1, textY, // Same as CardsStatuesUtility line 581
                        0xFFFFFFFF,
                        true
                    );
                    lineCount++;
                }
                
                if (currentStatue.getEffect() != null) {
                    String effect = currentStatue.getEffect();
                    if (effect.length() > 25) {
                        effect = effect.substring(0, 22) + "...";
                    }
                    textY = -1 + (lineCount * 12);
                    context.drawText(
                        client.textRenderer,
                        effect,
                        1, textY, // Same as CardsStatuesUtility line 581
                        0xFF00FF00,
                        true
                    );
                }
                
                return; // Successfully rendered real data
            }
        } catch (Exception e) {
            // Fall through to sample data
        }
        
        // Render sample data if real data is not available (scaled, relative positions)
        int textY = -1 + (lineCount * 12);
        context.drawText(
            client.textRenderer,
            "Krieger Statue",
            1, textY, // Same as CardsStatuesUtility line 581
            0xFFFFFFFF,
            true
        );
        lineCount++;
        
        textY = -1 + (lineCount * 12);
        context.drawText(
            client.textRenderer,
            "Stufe: 3",
            1, textY, // Same as CardsStatuesUtility line 581
            0xFFFFFFFF,
            true
        );
        lineCount++;
        
        textY = -1 + (lineCount * 12);
        context.drawText(
            client.textRenderer,
            "+15% Verteidigung",
            1, textY, // Same as CardsStatuesUtility line 581
            0xFF00FF00,
            true
        );
    }
    
    /**
     * Get current statue data from CardsStatuesUtility using reflection
     */
    private CardsStatuesUtility.StatueData getCurrentStatueData() {
        try {
            // Use reflection to access the private currentStatue field
            java.lang.reflect.Field field = CardsStatuesUtility.class.getDeclaredField("currentStatue");
            field.setAccessible(true);
            return (CardsStatuesUtility.StatueData) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }
}


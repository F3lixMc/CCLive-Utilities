package net.felix.utilities.DragOverlay.Aincraft;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.CardsStatuesUtility;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Draggable Overlay für die Cards
 */
public class CardsDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 162;
    private static final int DEFAULT_HEIGHT = 62;
    private static final Identifier CARDS_BACKGROUND_TEXTURE = Identifier.of("cclive-utilities", "textures/gui/karten_background.png");
    
    @Override
    public String getOverlayName() {
        return "Cards";
    }
    
    @Override
    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().cardX;
        // Use the same position calculation as CardsStatuesUtility line 450
        // Position is calculated without -11 offset, but we need to account for it visually
        int baseX = screenWidth - xOffset;
        // Apply -11 offset for visual representation (same as renderCardBackground line 80)
        return baseX - 11;
    }
    
    @Override
    public int getY() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenHeight = client.getWindow().getScaledHeight();
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().cardY;
        // Use the same position calculation as CardsStatuesUtility line 451
        // Position is calculated without -11 offset, but we need to account for it visually
        int baseY = screenHeight - yOffset;
        // Apply -11 offset for visual representation (same as renderCardBackground line 80)
        return baseY - 11;
    }
    
    @Override
    public int getWidth() {
        // Apply scale factor to match the visual size in the real overlay
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayScale;
        if (scale <= 0) scale = 1.0f; // Sicherheitscheck
        return (int) (DEFAULT_WIDTH * scale);
    }
    
    @Override
    public int getHeight() {
        // Apply scale factor to match the visual size in the real overlay
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayScale;
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
        
        CCLiveUtilitiesConfig.HANDLER.instance().cardX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().cardY = yOffset;
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
        
        CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayScale = scale;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        // Use the same position calculation as the actual overlay (without -11 offset in position)
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().cardX;
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().cardY;
        
        // Position without -11 offset (same as CardsStatuesUtility line 450)
        int baseX = screenWidth - xOffset;
        int baseY = screenHeight - yOffset;
        
        // Get scale
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayScale;
        if (scale <= 0) scale = 1.0f;
        
        // Use Matrix transformations for scaling (same as CardsStatuesUtility)
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        
        // Translate to position and scale from there (same as renderCardBackground line 72-73)
        matrices.translate(baseX, baseY);
        matrices.scale(scale, scale);
        
        // Render background based on overlay type (scaled, relative to matrix)
        net.felix.OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayType;
        
        if (overlayType == net.felix.OverlayType.CUSTOM) {
            try {
                context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    CARDS_BACKGROUND_TEXTURE,
                    -11, -11, // Relative position (same as renderCardBackground line 80)
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
        
        // Render real card data if available, otherwise sample data (scaled)
        renderCardData(context, scale);
        
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
               CCLiveUtilitiesConfig.HANDLER.instance().cardEnabled &&
               CCLiveUtilitiesConfig.HANDLER.instance().showCard;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Cards - Shows card information and levels");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().cardX = 151;
        CCLiveUtilitiesConfig.HANDLER.instance().cardY = 125;
        CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayScale = 1.0f;
    }
    
    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayScale = 1.0f;
    }
    
    /**
     * Render real card data if available, otherwise sample data
     * Uses scaled positioning with matrix transformations (same as CardsStatuesUtility)
     */
    private void renderCardData(DrawContext context, float scale) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Text positions are relative to the matrix (0-based, same as CardsStatuesUtility line 523-524)
        // The matrix is already translated to baseX, baseY and scaled
        // First line: x=1, y=-1 (same as CardsStatuesUtility)
        int lineCount = 0;
        
        try {
            // Try to get real card data from CardsStatuesUtility
            CardsStatuesUtility.CardData currentCard = getCurrentCardData();
            
            if (currentCard != null && currentCard.getName() != null) {
                // Render real data (scaled, relative positions, same as CardsStatuesUtility line 514)
                String cardName = currentCard.getName();
                if (cardName.length() > 25) {
                    cardName = cardName.substring(0, 22) + "...";
                }
                
                int textY = -1 + (lineCount * 12); // Same as CardsStatuesUtility line 514
                context.drawText(
                    client.textRenderer,
                    cardName,
                    1, textY, // Same as CardsStatuesUtility line 523
                    0xFFFFFFFF,
                    true
                );
                lineCount++;
                
                if (currentCard.getLevel() != null) {
                    textY = -1 + (lineCount * 12);
                    context.drawText(
                        client.textRenderer,
                        "Stufe: " + currentCard.getLevel(),
                        1, textY, // Same as CardsStatuesUtility line 523
                        0xFFFFFFFF,
                        true
                    );
                    lineCount++;
                }
                
                if (currentCard.getEffect() != null) {
                    String effect = currentCard.getEffect();
                    if (effect.length() > 25) {
                        effect = effect.substring(0, 22) + "...";
                    }
                    textY = -1 + (lineCount * 12);
                    context.drawText(
                        client.textRenderer,
                        effect,
                        1, textY, // Same as CardsStatuesUtility line 523
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
            "Kraft Karte",
            1, textY, // Same as CardsStatuesUtility line 523
            0xFFFFFFFF,
            true
        );
        lineCount++;
        
        textY = -1 + (lineCount * 12);
        context.drawText(
            client.textRenderer,
            "Stufe: 5",
            1, textY, // Same as CardsStatuesUtility line 523
            0xFFFFFFFF,
            true
        );
        lineCount++;
        
        textY = -1 + (lineCount * 12);
        context.drawText(
            client.textRenderer,
            "+10% Schaden",
            1, textY, // Same as CardsStatuesUtility line 523
            0xFF00FF00,
            true
        );
    }
    
    /**
     * Get current card data from CardsStatuesUtility using reflection
     */
    private CardsStatuesUtility.CardData getCurrentCardData() {
        try {
            // Use reflection to access the private currentCard field
            java.lang.reflect.Field field = CardsStatuesUtility.class.getDeclaredField("currentCard");
            field.setAccessible(true);
            return (CardsStatuesUtility.CardData) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }
}



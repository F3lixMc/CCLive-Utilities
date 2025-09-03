package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.CardsStatuesUtility;
import net.felix.utilities.DraggableOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
        return screenWidth - xOffset - 11; // -11 für Textur-Offset
    }
    
    @Override
    public int getY() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenHeight = client.getWindow().getScaledHeight();
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().cardY;
        return screenHeight - yOffset - 11; // -11 für Textur-Offset
    }
    
    @Override
    public int getWidth() {
        return DEFAULT_WIDTH;
    }
    
    @Override
    public int getHeight() {
        return DEFAULT_HEIGHT;
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
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render background based on overlay type
        net.felix.OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayType;
        
        if (overlayType == net.felix.OverlayType.CUSTOM) {
            try {
                context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    CARDS_BACKGROUND_TEXTURE,
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
        
        // Render real card data if available, otherwise sample data
        renderCardData(context, x, y);
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
        CCLiveUtilitiesConfig.HANDLER.instance().cardX = 143;
        CCLiveUtilitiesConfig.HANDLER.instance().cardY = 125;
    }
    
    /**
     * Render real card data if available, otherwise sample data
     * Uses simple positioning like normal overlays
     */
    private void renderCardData(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        try {
            // Try to get real card data from CardsStatuesUtility
            CardsStatuesUtility.CardData currentCard = getCurrentCardData();
            
            if (currentCard != null && currentCard.getName() != null) {
                // Render real data
                String cardName = currentCard.getName();
                if (cardName.length() > 25) {
                    cardName = cardName.substring(0, 22) + "...";
                }
                
                context.drawText(
                    client.textRenderer,
                    cardName,
                    x + 8, y + 20,
                    0xFFFFFFFF,
                    true
                );
                
                if (currentCard.getLevel() != null) {
                    context.drawText(
                        client.textRenderer,
                        "Stufe: " + currentCard.getLevel(),
                        x + 8, y + 32,
                        0xFFFFFFFF,
                        true
                    );
                }
                
                if (currentCard.getEffect() != null) {
                    String effect = currentCard.getEffect();
                    if (effect.length() > 25) {
                        effect = effect.substring(0, 22) + "...";
                    }
                    context.drawText(
                        client.textRenderer,
                        effect,
                        x + 8, y + 44,
                        0xFF00FF00,
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
            "Kraft Karte",
            x + 8, y + 20,
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            client.textRenderer,
            "Stufe: 5",
            x + 8, y + 32,
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            client.textRenderer,
            "+10% Schaden",
            x + 8, y + 44,
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

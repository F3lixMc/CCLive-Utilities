package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.CardsStatuesUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
        return screenWidth - xOffset - 11; // -11 für Textur-Offset
    }
    
    @Override
    public int getY() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenHeight = client.getWindow().getScaledHeight();
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().statueY;
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
        
        CCLiveUtilitiesConfig.HANDLER.instance().statueX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().statueY = yOffset;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render background based on overlay type
        net.felix.OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayType;
        
        if (overlayType == net.felix.OverlayType.CUSTOM) {
            try {
                context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    STATUES_BACKGROUND_TEXTURE,
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
        
        // Render real statue data if available, otherwise sample data
        renderStatueData(context, x, y);
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
    }
    
    /**
     * Render real statue data if available, otherwise sample data
     * Uses simple positioning like normal overlays
     */
    private void renderStatueData(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        try {
            // Try to get real statue data from CardsStatuesUtility
            CardsStatuesUtility.StatueData currentStatue = getCurrentStatueData();
            
            if (currentStatue != null && currentStatue.getName() != null) {
                // Render real data
                String statueName = currentStatue.getName();
                if (statueName.length() > 25) {
                    statueName = statueName.substring(0, 22) + "...";
                }
                
                context.drawText(
                    client.textRenderer,
                    statueName,
                    x + 8, y + 20,
                    0xFFFFFFFF,
                    true
                );
                
                if (currentStatue.getLevel() != null) {
                    context.drawText(
                        client.textRenderer,
                        "Stufe: " + currentStatue.getLevel(),
                        x + 8, y + 32,
                        0xFFFFFFFF,
                        true
                    );
                }
                
                if (currentStatue.getEffect() != null) {
                    String effect = currentStatue.getEffect();
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
            "Krieger Statue",
            x + 8, y + 20,
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            client.textRenderer,
            "Stufe: 3",
            x + 8, y + 32,
            0xFFFFFFFF,
            true
        );
        
        context.drawText(
            client.textRenderer,
            "+15% Verteidigung",
            x + 8, y + 44,
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


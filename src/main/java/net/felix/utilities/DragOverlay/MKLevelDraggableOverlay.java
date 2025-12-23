package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

/**
 * Draggable Overlay für das MKLevel Overlay
 */
public class MKLevelDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 166; // Standard inventory height
    
    @Override
    public String getOverlayName() {
        return "MKLevel";
    }
    
    @Override
    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int xPos = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelX;
        
        // Wenn xPos -1 ist, positioniere links vom Inventar
        if (xPos == -1) {
            // Versuche die Inventar-Position zu bekommen
            int inventoryX = 0;
            net.minecraft.client.gui.screen.Screen screenToCheck = client.currentScreen;
            
            // Wenn wir im F6-Editor sind, versuche auf previousScreen zuzugreifen
            if (client.currentScreen instanceof net.felix.utilities.DragOverlay.OverlayEditorScreen) {
                try {
                    java.lang.reflect.Field previousScreenField = net.felix.utilities.DragOverlay.OverlayEditorScreen.class.getDeclaredField("previousScreen");
                    previousScreenField.setAccessible(true);
                    net.minecraft.client.gui.screen.Screen previousScreen = (net.minecraft.client.gui.screen.Screen) previousScreenField.get(client.currentScreen);
                    if (previousScreen != null && previousScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?>) {
                        screenToCheck = previousScreen;
                    }
                } catch (Exception e) {
                    // Fallback
                }
            }
            
            // Versuche die Inventar-X-Position zu bekommen
            if (screenToCheck instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen) {
                try {
                    java.lang.reflect.Field xField = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField("x");
                    xField.setAccessible(true);
                    inventoryX = xField.getInt(handledScreen);
                    
                    // Positioniere links vom Inventar
                    int scaledWidth = getWidth();
                    return inventoryX - scaledWidth - 10; // 10px Abstand links vom Inventar
                } catch (Exception e) {
                    // Fallback: rechts positionieren
                }
            }
            
            // Fallback: rechts positionieren (wenn kein Inventar verfügbar)
            int scaledWidth = getWidth();
            return screenWidth - scaledWidth - 10; // 10px Abstand vom rechten Rand
        }
        
        // Ansonsten verwende die absolute X-Position (obere linke Ecke)
        return xPos;
    }
    
    @Override
    public int getY() {
        // Y-Position: -1 = am Inventar ausrichten, >= 0 = absolute Position
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelY;
        if (yOffset == -1) {
            // Im F6-Editor: Wenn -1, zeige eine Standard-Position (zentriert)
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                int screenHeight = client.getWindow().getScaledHeight();
                return (screenHeight - getHeight()) / 2;
            }
        }
        return yOffset;
    }
    
    /**
     * Berechnet die unskalierte Breite
     */
    private int calculateUnscaledWidth() {
        return DEFAULT_WIDTH;
    }
    
    /**
     * Berechnet die unskalierte Höhe
     */
    private int calculateUnscaledHeight() {
        // Höhe entspricht der Inventar-Höhe (wird dynamisch angepasst)
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen == null) {
            return DEFAULT_HEIGHT;
        }
        
        net.minecraft.client.gui.screen.Screen screenToCheck = client.currentScreen;
        
        // Wenn wir im F6-Editor sind, versuche auf previousScreen zuzugreifen
        if (client.currentScreen instanceof net.felix.utilities.DragOverlay.OverlayEditorScreen) {
            try {
                java.lang.reflect.Field previousScreenField = net.felix.utilities.DragOverlay.OverlayEditorScreen.class.getDeclaredField("previousScreen");
                previousScreenField.setAccessible(true);
                net.minecraft.client.gui.screen.Screen previousScreen = (net.minecraft.client.gui.screen.Screen) previousScreenField.get(client.currentScreen);
                if (previousScreen != null && previousScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?>) {
                    screenToCheck = previousScreen;
                }
            } catch (Exception e) {
                // Fallback to current screen
            }
        }
        
        // Versuche die tatsächliche Inventar-Höhe zu bekommen
        if (screenToCheck instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen) {
            try {
                java.lang.reflect.Field bgHeightField = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField("backgroundHeight");
                bgHeightField.setAccessible(true);
                return bgHeightField.getInt(handledScreen);
            } catch (Exception e) {
                // Fallback to default
            }
        }
        
        return DEFAULT_HEIGHT;
    }
    
    @Override
    public int getWidth() {
        // Return scaled width
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale;
        if (scale <= 0) scale = 1.0f; // Safety check
        return Math.round(calculateUnscaledWidth() * scale);
    }
    
    @Override
    public int getHeight() {
        // Return scaled height
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale;
        if (scale <= 0) scale = 1.0f; // Safety check
        return Math.round(calculateUnscaledHeight() * scale);
    }
    
    @Override
    public void setPosition(int x, int y) {
        // Speichere die absolute X-Position (obere linke Ecke)
        // Dies ermöglicht, dass die obere linke Ecke beim Resize fixiert bleibt
        CCLiveUtilitiesConfig.HANDLER.instance().mkLevelX = x;
        
        // Y-Position: Speichere die absolute Y-Position
        // -1 = am Inventar ausrichten, >= 0 = absolute Position
        CCLiveUtilitiesConfig.HANDLER.instance().mkLevelY = y;
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
        
        CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale = scale;
    }
    
    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale = 1.0f;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Get scale
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale;
        if (scale <= 0) scale = 1.0f;
        
        // Render background (scaled)
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // Render border for edit mode (scaled)
        context.drawBorder(x, y, width, height, 0xFFFF0000);
        
        // Render preview text with scale using matrix transformation
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(x, y);
            matrices.scale(scale, scale);
            
            int padding = 5;
            int lineHeight = 12;
            int textX = padding;
            int textY = padding;
            
            // Render preview entries
            String previewLevel = "-Level 2";
            String previewEssence = " Pferd T1, 3.000";
            
            context.drawText(client.textRenderer, previewLevel, textX, textY, 0xFFFFFF00, true);
            textY += lineHeight;
            context.drawText(client.textRenderer, previewEssence, textX, textY, 0xFFFFFFFF, true);
            textY += lineHeight;
            
            context.drawText(client.textRenderer, "-Level 3", textX, textY, 0xFFFFFF00, true);
            textY += lineHeight;
            context.drawText(client.textRenderer, " Lohe T1, 4.000", textX, textY, 0xFFFFFFFF, true);
            
            matrices.popMatrix();
        }
    }
    
    @Override
    public void savePosition() {
        CCLiveUtilitiesConfig.HANDLER.save();
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().enableMod &&
               CCLiveUtilitiesConfig.HANDLER.instance().informationenUtilityEnabled &&
               CCLiveUtilitiesConfig.HANDLER.instance().mkLevelEnabled;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("MKLevel Overlay - Zeigt Level, Essence und Amount für Machtkristalle Verbessern");
    }
    
    @Override
    public void resetToDefault() {
        // Standard-Position: -1 = automatisch links vom Inventar, am Inventar ausgerichtet
        CCLiveUtilitiesConfig.HANDLER.instance().mkLevelX = -1;
        CCLiveUtilitiesConfig.HANDLER.instance().mkLevelY = -1; // -1 = am Inventar ausrichten
        savePosition();
    }
}


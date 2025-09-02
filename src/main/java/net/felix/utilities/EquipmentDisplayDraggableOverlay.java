package net.felix.utilities;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Draggable Overlay für die Equipment Display
 */
public class EquipmentDisplayDraggableOverlay implements DraggableOverlay {
    
    private static final int DEFAULT_WIDTH = 80; // Much smaller width for armor display
    private static final int DEFAULT_HEIGHT = 20; // Much smaller height for armor display
    private static final Identifier LEFT_OVERLAY_TEXTURE = Identifier.of("cclive-utilities", "textures/gui/left_overlay.png");
    private static final Identifier RIGHT_OVERLAY_TEXTURE = Identifier.of("cclive-utilities", "textures/gui/right_overlay.png");
    
    @Override
    public String getOverlayName() {
        return "Rüstungs Wert";
    }
    
    @Override
    public int getX() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayArmorX;
        
        // Calculate the exact position like in EquipmentDisplayUtility
        String armorText = String.format("Rüstung: %s", formatNumber(getArmorValueFromEquipmentDisplay()));
        int armorTextWidth = client.textRenderer.getWidth(armorText);
        int armorX = (screenWidth - armorTextWidth) / 2 + xOffset;
        
        // Use the exact same position as the standard overlay
        return armorX;
    }
    
    @Override
    public int getY() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return 0;
        
        int screenHeight = client.getWindow().getScaledHeight();
        int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayArmorY;
        
        // Calculate the exact position like in EquipmentDisplayUtility
        int armorY = screenHeight - yOffset;
        
        // Use the exact same position as the standard overlay
        return armorY;
    }
    
    @Override
    public int getWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return DEFAULT_WIDTH;
        
        // Calculate the exact width of the armor text + padding (same as EquipmentDisplayUtility)
        String armorText = String.format("Rüstung: %s", formatNumber(getArmorValueFromEquipmentDisplay()));
        int armorTextWidth = client.textRenderer.getWidth(armorText);
        
        // Add padding like in EquipmentDisplayUtility
        int padding = 4;
        return armorTextWidth + (padding * 2);
    }
    
    @Override
    public int getHeight() {
        // Calculate the exact height of the armor text + padding (same as EquipmentDisplayUtility)
        int textHeight = 12; // Standard text height
        int padding = 4;
        return textHeight + (padding * 2);
    }
    
    @Override
    public void setPosition(int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Calculate offset like in EquipmentDisplayUtility
        String armorText = String.format("Rüstung: %s", formatNumber(getArmorValueFromEquipmentDisplay()));
        int armorTextWidth = client.textRenderer.getWidth(armorText);
        int baseX = (screenWidth - armorTextWidth) / 2;
        int baseY = screenHeight;
        
        int xOffset = x - baseX;
        int yOffset = baseY - y;
        
        CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayArmorX = xOffset;
        CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayArmorY = yOffset;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Render the actual armor display first (like in EquipmentDisplayUtility)
        renderArmorDisplay(context, x, y);
        
        // Render border for edit mode OUTSIDE the black overlay (1 pixel offset)
        context.drawBorder(x - 1, y - 1, width + 2, height + 2, 0xFFFF0000);
        
        // Render overlay name above the overlay (smaller)
        context.drawText(
            MinecraftClient.getInstance().textRenderer,
            getOverlayName(),
            x + 2, y - 12,
            0xFFFFFFFF,
            true
        );
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayEnabled && 
               CCLiveUtilitiesConfig.HANDLER.instance().showEquipmentDisplay;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Equipment Display - Shows equipment statistics and armor values");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayArmorX = -134;
        CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayArmorY = 300;
    }
    
    /**
     * Render the actual armor display (like in EquipmentDisplayUtility)
     */
    private void renderArmorDisplay(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Try to get real armor value from EquipmentDisplayUtility using reflection
        double armorValue = getArmorValueFromEquipmentDisplay();
        
        // Render armor text (same format as EquipmentDisplayUtility)
        String armorText = String.format("Rüstung: %s", formatNumber(armorValue));
        
        // The overlay is already positioned correctly, so just draw the text at the overlay position
        // Draw background if overlay type is not NONE (same as EquipmentDisplayUtility)
        net.felix.OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayOverlayType;
        if (overlayType != net.felix.OverlayType.NONE) {
            // The overlay already has the correct size, so just fill it
            context.fill(x, y, x + getWidth(), y + getHeight(), 0x80000000);
        }
        
        // Draw the armor text at the correct position (with padding)
        int padding = 4;
        context.drawText(
            client.textRenderer,
            armorText,
            x + padding,
            y + padding,
            0xFFFFFFFF,
            true
        );
    }
    
    /**
     * Get armor value from EquipmentDisplayUtility using reflection
     */
    private double getArmorValueFromEquipmentDisplay() {
        try {
            // Use reflection to access the private totalArmor field
            java.lang.reflect.Field totalArmorField = EquipmentDisplayUtility.class.getDeclaredField("totalArmor");
            totalArmorField.setAccessible(true);
            return (Double) totalArmorField.get(null);
        } catch (Exception e) {
            // Fallback to sample value if reflection fails
            return 100.0;
        }
    }
    
    /**
     * Format number like in EquipmentDisplayUtility
     */
    private String formatNumber(double value) {
        if (value == (int) value) {
            // Glatte Zahl ohne Nachkommastellen
            return String.valueOf((int) value);
        } else {
            // Zahl mit Nachkommastellen
            return String.format("%.1f", value);
        }
    }
}

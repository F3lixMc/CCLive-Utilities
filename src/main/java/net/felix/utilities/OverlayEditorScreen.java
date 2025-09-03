package net.felix.utilities;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.AspectOverlayDraggableOverlay;
import net.felix.utilities.DragOverlay.BlueprintViewerDraggableOverlay;
import net.felix.utilities.DragOverlay.BossHPDraggableOverlay;
import net.felix.utilities.DragOverlay.CardsDraggableOverlay;
import net.felix.utilities.DragOverlay.EquipmentDisplayDraggableOverlay;
import net.felix.utilities.DragOverlay.HideUncraftableButtonDraggableOverlay;
import net.felix.utilities.DragOverlay.KillsUtilityDraggableOverlay;
import net.felix.utilities.DragOverlay.MaterialTrackerDraggableOverlay;
import net.felix.utilities.DragOverlay.StatuesDraggableOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen für das Drag-and-Drop-Positionieren von Overlays
 */
public class OverlayEditorScreen extends Screen {
    
    private final List<DraggableOverlay> overlays = new ArrayList<>();
    private DraggableOverlay draggingOverlay = null;
    private DraggableOverlay resizingOverlay = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int resizeStartX = 0;
    private int resizeStartY = 0;
    private int resizeStartWidth = 0;
    private int resizeStartHeight = 0;
    
    private ButtonWidget doneButton;
    private ButtonWidget resetButton;
    private TextWidget titleWidget;
    
    // Store the previous screen so we can restore it
    private Screen previousScreen;
    
    public OverlayEditorScreen() {
        super(Text.literal("Overlay Editor"));
        // Store the current screen before opening the overlay editor
        this.previousScreen = MinecraftClient.getInstance().currentScreen;
        initializeOverlays();
    }
    
    private void initializeOverlays() {
        // Check if we're in a blueprint inventory screen (where Hide Uncraftable button and Aspect Overlay work)
        boolean isInBlueprintInventory = isInInventoryScreen();
        
        // BossHP overlay is available in all dimensions
        overlays.add(new BossHPDraggableOverlay());
        
        if (isInBlueprintInventory) {
            // In blueprint inventory screens, show overlays that are relevant for blueprint inventories
            overlays.add(new AspectOverlayDraggableOverlay());
            overlays.add(new HideUncraftableButtonDraggableOverlay());
        } else {
            // Check if player is in a floor dimension
            boolean isInFloorDimension = isInFloorDimension();
            
            if (isInFloorDimension) {
                // In floor dimensions, only show floor-specific overlays
                overlays.add(new CardsDraggableOverlay());
                overlays.add(new StatuesDraggableOverlay());
                overlays.add(new BlueprintViewerDraggableOverlay());
                overlays.add(new MaterialTrackerDraggableOverlay());
                overlays.add(new KillsUtilityDraggableOverlay());
            } else {
                // Check if we're in an equipment chest inventory (where Equipment Display works)
                boolean isInEquipmentChest = isInEquipmentChestInventory();
                
                if (isInEquipmentChest) {
                    // In equipment chest inventories, show equipment display overlay
                    overlays.add(new EquipmentDisplayDraggableOverlay());
                }
                // Note: Aspect Overlay and Hide Uncraftable Button are only available in blueprint inventories
                // Note: Equipment Display is only available in equipment chest inventories (with Unicode characters 㬥, 㬦, 㬧, 㬨)
                // Note: Cards, Statues, BlueprintViewer, Material Tracker, and Kills are only available in floor dimensions
            }
        }
    }
    
    /**
     * Check if the player is currently in a blueprint inventory screen where Hide Uncraftable button and Aspect Overlay work
     */
    private boolean isInInventoryScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen == null) {
            return false;
        }
        
        // Check if the current screen is a HandledScreen (inventory-like screen)
        if (!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) {
            return false;
        }
        
        // Get the screen title to check if it's a blueprint inventory
        net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen = 
            (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
        
        String title = handledScreen.getTitle().getString();
        
        // Remove Minecraft formatting codes and Unicode characters for comparison
        String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "")
                               .replaceAll("[\\u3400-\\u4DBF]", "");
        
        // Check if the clean title contains any of the allowed blueprint inventory names
        // (same logic as in HandledScreenMixin.isBlueprintInventory())
        return cleanTitle.contains("Baupläne [Waffen]") ||
               cleanTitle.contains("Baupläne [Rüstung]") ||
               cleanTitle.contains("Baupläne [Werkzeuge]") ||
               cleanTitle.contains("Bauplan [Shop]") ||
               cleanTitle.contains("Favorisierte [Rüstungsbaupläne]") ||
               cleanTitle.contains("Favorisierte [Waffenbaupläne]") ||
               cleanTitle.contains("Favorisierte [Werkzeugbaupläne]") ||
               cleanTitle.contains("Favorisierte [Shop-Baupläne]");
    }
    
    /**
     * Check if the player is currently in an equipment chest inventory where Equipment Display works
     */
    private boolean isInEquipmentChestInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen == null) {
            return false;
        }
        
        // Check if the current screen is a HandledScreen (inventory-like screen)
        if (!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) {
            return false;
        }
        
        // Get the screen title to check if it contains the equipment chest Unicode characters
        net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen = 
            (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
        
        String title = handledScreen.getTitle().getString();
        
        // Check if the title contains any of the equipment chest Unicode characters
        // (same logic as in EquipmentDisplayUtility)
        return title.contains("㬥") || title.contains("㬦") || title.contains("㬧") || title.contains("㬨");
    }
    
    /**
     * Check if the player is currently in a floor dimension
     */
    private boolean isInFloorDimension() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return false;
        }
        
        // Check if the player is in a floor dimension by checking the dimension identifier
        String dimensionId = client.world.getRegistryKey().getValue().toString();
        
        // Floor dimensions typically have "floor" in their identifier
        return dimensionId.contains("floor") || dimensionId.contains("dungeon");
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Title
        titleWidget = new TextWidget(
            Text.literal("Overlay Editor - Drag & Drop to reposition overlays"),
            textRenderer
        );
        titleWidget.setPosition(width / 2 - titleWidget.getWidth() / 2, 20);
        addDrawableChild(titleWidget);
        
        // Done Button
        doneButton = ButtonWidget.builder(
            Text.literal("Done"),
            button -> close()
        ).dimensions(width / 2 - 100, height - 30, 80, 20).build();
        addDrawableChild(doneButton);
        
        // Reset Button
        resetButton = ButtonWidget.builder(
            Text.literal("Reset All"),
            button -> resetAllOverlays()
        ).dimensions(width / 2 + 20, height - 30, 80, 20).build();
        addDrawableChild(resetButton);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the previous screen in the background if it exists
        if (previousScreen != null) {
            previousScreen.render(context, mouseX, mouseY, delta);
        }
        
        // Render very transparent background overlay
        context.fill(0, 0, width, height, 0x20000000);
        
        // Render title
        titleWidget.render(context, mouseX, mouseY, delta);
        
        // Render all overlays in edit mode
        for (DraggableOverlay overlay : overlays) {
            if (overlay.isEnabled()) {
                overlay.renderInEditMode(context, mouseX, mouseY, delta);
                
                // Render resize handle if hovered
                if (overlay.isResizeArea(mouseX, mouseY)) {
                    renderResizeHandle(context, overlay);
                }
            }
        }
        
        // Render instructions
        renderInstructions(context);
        
        // Render buttons
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderResizeHandle(DrawContext context, DraggableOverlay overlay) {
        int handleSize = 8;
        int x = overlay.getX() + overlay.getWidth() - handleSize;
        int y = overlay.getY() + overlay.getHeight() - handleSize;
        
        // Draw resize handle (small square)
        context.fill(x, y, x + handleSize, y + handleSize, 0xFFFFFFFF);
        context.fill(x + 1, y + 1, x + handleSize - 1, y + handleSize - 1, 0xFF000000);
    }
    
    private void renderInstructions(DrawContext context) {
        int y = height - 80;
        String[] instructions = {
            "Left Click + Drag: Move overlay",
            "Right Click + Drag: Resize overlay (if supported)",
            "ESC: Close editor"
        };
        
        for (String instruction : instructions) {
            context.drawText(textRenderer, instruction, 10, y, 0xFFFFFFFF, false);
            y += 12;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Check for resize area first
            for (DraggableOverlay overlay : overlays) {
                if (overlay.isEnabled() && overlay.isResizeArea((int) mouseX, (int) mouseY)) {
                    resizingOverlay = overlay;
                    resizeStartX = (int) mouseX;
                    resizeStartY = (int) mouseY;
                    resizeStartWidth = overlay.getWidth();
                    resizeStartHeight = overlay.getHeight();
                    return true;
                }
            }
            
            // Check for drag area
            for (DraggableOverlay overlay : overlays) {
                if (overlay.isEnabled() && overlay.isHovered((int) mouseX, (int) mouseY)) {
                    draggingOverlay = overlay;
                    dragOffsetX = (int) mouseX - overlay.getX();
                    dragOffsetY = (int) mouseY - overlay.getY();
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (draggingOverlay != null) {
                int newX = (int) mouseX - dragOffsetX;
                int newY = (int) mouseY - dragOffsetY;
                
                // Keep overlay within screen bounds
                newX = Math.max(0, Math.min(newX, width - draggingOverlay.getWidth()));
                newY = Math.max(0, Math.min(newY, height - draggingOverlay.getHeight()));
                
                draggingOverlay.setPosition(newX, newY);
                return true;
            }
            
            if (resizingOverlay != null) {
                int deltaWidth = (int) mouseX - resizeStartX;
                int deltaHeight = (int) mouseY - resizeStartY;
                
                int newWidth = Math.max(50, resizeStartWidth + deltaWidth);
                int newHeight = Math.max(20, resizeStartHeight + deltaHeight);
                
                resizingOverlay.setSize(newWidth, newHeight);
                return true;
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (draggingOverlay != null) {
                draggingOverlay.savePosition();
                draggingOverlay = null;
                return true;
            }
            
            if (resizingOverlay != null) {
                resizingOverlay.savePosition();
                resizingOverlay = null;
                return true;
            }
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void resetAllOverlays() {
        for (DraggableOverlay overlay : overlays) {
            if (overlay.isEnabled()) {
                overlay.resetToDefault();
            }
        }
    }
    
    @Override
    public void close() {
        // Save all overlay positions
        for (DraggableOverlay overlay : overlays) {
            overlay.savePosition();
        }
        
        // Save configuration
        CCLiveUtilitiesConfig.HANDLER.save();
        
        // Reset the overlay editor open status
        OverlayEditorUtility.setOverlayEditorOpen(false);
        
        // Restore the previous screen instead of closing
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && previousScreen != null) {
            client.setScreen(previousScreen);
        } else {
            super.close();
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}

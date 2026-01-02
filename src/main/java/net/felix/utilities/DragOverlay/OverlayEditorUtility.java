package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility für den Overlay Editor
 */
public class OverlayEditorUtility {
    
    private static boolean isInitialized = false;
    private static final AtomicBoolean f6KeyPressed = new AtomicBoolean(false);
    private static boolean isOverlayEditorOpen = false;
    
    // KeyBinding for the overlay editor
    private static KeyBinding overlayEditorKeyBinding;
    
    // Track last screen size to detect changes
    private static int lastScreenWidth = -1;
    private static int lastScreenHeight = -1;
    
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        try {
            // Register key binding
            registerKeyBinding();
            
            // Register client tick events
            ClientTickEvents.END_CLIENT_TICK.register(OverlayEditorUtility::onClientTick);
            
            isInitialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void registerKeyBinding() {
        // Register overlay editor key binding
        overlayEditorKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cclive-utilities.overlay-editor",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F6, // Default to F6
            "categories.cclive-utilities.overlay"
        ));
    }
    
    private static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.getWindow() == null) {
            return;
        }
        
        // Check for screen size changes and adjust overlay positions
        int currentScreenWidth = client.getWindow().getScaledWidth();
        int currentScreenHeight = client.getWindow().getScaledHeight();
        
        if (lastScreenWidth != -1 && lastScreenHeight != -1) {
            // Screen size changed
            if (currentScreenWidth != lastScreenWidth || currentScreenHeight != lastScreenHeight) {
                adjustOverlayPositions(client, lastScreenWidth, lastScreenHeight, currentScreenWidth, currentScreenHeight);
            }
        }
        
        lastScreenWidth = currentScreenWidth;
        lastScreenHeight = currentScreenHeight;
        
        // Use the registered KeyBinding instead of hardcoded key detection
        if (overlayEditorKeyBinding != null && overlayEditorKeyBinding.wasPressed()) {
            // Overlay Editor is always enabled
                toggleOverlayEditor();
        }
    }
    
    /**
     * Adjusts overlay positions when screen size changes to keep them within bounds
     */
    private static void adjustOverlayPositions(MinecraftClient client, int oldWidth, int oldHeight, int newWidth, int newHeight) {
        if (client.getWindow() == null) return;
        
        // Adjust overlays with absolute positioning
        adjustOverlayPositionAbsolute("mkLevelX", "mkLevelY", "mkLevelScale", 200, 166, newWidth, newHeight);
        adjustOverlayPositionAbsolute("collectionOverlayX", "collectionOverlayY", "collectionOverlayScale", 200, 80, newWidth, newHeight);
        adjustOverlayPositionAbsolute("equipmentDisplayX", "equipmentDisplayY", "equipmentDisplayScale", 200, 100, newWidth, newHeight);
        adjustOverlayPositionAbsolute("aspectOverlayX", "aspectOverlayY", "aspectOverlayScale", 210, 110, newWidth, newHeight);
        adjustOverlayPositionAbsolute("chatAspectOverlayX", "chatAspectOverlayY", "chatAspectOverlayScale", 210, 110, newWidth, newHeight);
        
        // Adjust overlays with relative positioning (from right edge or left/right side detection)
        adjustOverlayPositionRelative("killsUtilityX", "killsUtilityY", "killsUtilityScale", 200, 100, newWidth, newHeight);
        adjustOverlayPositionRelative("miningOverlayX", "miningOverlayY", "miningLumberjackOverlayScale", 200, 100, newWidth, newHeight);
        adjustOverlayPositionRelative("materialTrackerX", "materialTrackerY", "materialTrackerScale", 200, 100, newWidth, newHeight);
        adjustOverlayPositionRelative("blueprintViewerX", "blueprintViewerY", "blueprintViewerScale", 200, 100, newWidth, newHeight);
        adjustOverlayPositionRelative("bossHPX", "bossHPY", "bossHPScale", 200, 50, newWidth, newHeight);
        adjustOverlayPositionRelative("cardsX", "cardsY", "cardsScale", 200, 100, newWidth, newHeight);
        adjustOverlayPositionRelative("statuesX", "statuesY", "statuesScale", 200, 100, newWidth, newHeight);
        
        // Adjust button positions (they use relative positioning from right edge)
        adjustButtonPosition("kitFilterButton1X", "kitFilterButton1Y", "kitFilterButton1Scale", 100, 20, 50, newWidth, newHeight);
        adjustButtonPosition("kitFilterButton2X", "kitFilterButton2Y", "kitFilterButton2Scale", 100, 20, 75, newWidth, newHeight);
        adjustButtonPosition("kitFilterButton3X", "kitFilterButton3Y", "kitFilterButton3Scale", 100, 20, 100, newWidth, newHeight);
        adjustButtonPosition("hideUncraftableButtonX", "hideUncraftableButtonY", "hideUncraftableButtonScale", 120, 20, 20, newWidth, newHeight);
        adjustButtonPosition("hideWrongClassButtonX", "hideWrongClassButtonY", "hideWrongClassButtonScale", 120, 20, 20, newWidth, newHeight);
        
        // Adjust TabInfo overlays
        adjustTabInfoOverlay("tabInfoX", "tabInfoY", "tabInfoScale", 200, 100, newWidth, newHeight);
    }
    
    /**
     * Adjusts overlay position for overlays with relative positioning (left/right side detection)
     * Uses DraggableOverlay instances to get actual positions
     */
    private static void adjustOverlayPositionRelative(String xField, String yField, String scaleField, int defaultWidth, int defaultHeight, int screenWidth, int screenHeight) {
        try {
            // Create overlay instances to get actual positions
            DraggableOverlay overlay = null;
            if (xField.equals("killsUtilityX")) {
                overlay = new KillsUtilityDraggableOverlay();
            } else if (xField.equals("miningOverlayX")) {
                overlay = new MiningLumberjackDraggableOverlay();
            } else if (xField.equals("materialTrackerX")) {
                overlay = new MaterialTrackerDraggableOverlay();
            } else if (xField.equals("blueprintViewerX")) {
                overlay = new BlueprintViewerDraggableOverlay();
            } else if (xField.equals("bossHPX")) {
                overlay = new BossHPDraggableOverlay();
            } else if (xField.equals("cardsX")) {
                overlay = new CardsDraggableOverlay();
            } else if (xField.equals("statuesX")) {
                overlay = new StatuesDraggableOverlay();
            }
            
            if (overlay == null) {
                // Fallback to simple adjustment
                adjustOverlayPositionAbsolute(xField, yField, scaleField, defaultWidth, defaultHeight, screenWidth, screenHeight);
                return;
            }
            
            // Get actual position from overlay
            int actualX = overlay.getX();
            int actualY = overlay.getY();
            int width = overlay.getWidth();
            int height = overlay.getHeight();
            
            // Check if overlay is outside screen and adjust
            boolean needsAdjustment = false;
            int newX = actualX;
            int newY = actualY;
            
            if (actualX < 0) {
                newX = 0;
                needsAdjustment = true;
            } else if (actualX + width > screenWidth) {
                newX = Math.max(0, screenWidth - width);
                needsAdjustment = true;
            }
            
            if (actualY < 0) {
                newY = 0;
                needsAdjustment = true;
            } else if (actualY + height > screenHeight) {
                newY = Math.max(0, screenHeight - height);
                needsAdjustment = true;
            }
            
            // Update position if adjustment is needed
            if (needsAdjustment) {
                overlay.setPosition(newX, newY);
            }
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    /**
     * Adjusts a single overlay position to keep it within screen bounds (absolute positioning)
     */
    private static void adjustOverlayPositionAbsolute(String xField, String yField, String scaleField, int defaultWidth, int defaultHeight, int screenWidth, int screenHeight) {
        try {
            CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
            java.lang.reflect.Field xFieldRef = CCLiveUtilitiesConfig.class.getDeclaredField(xField);
            java.lang.reflect.Field yFieldRef = CCLiveUtilitiesConfig.class.getDeclaredField(yField);
            java.lang.reflect.Field scaleFieldRef = CCLiveUtilitiesConfig.class.getDeclaredField(scaleField);
            
            xFieldRef.setAccessible(true);
            yFieldRef.setAccessible(true);
            scaleFieldRef.setAccessible(true);
            
            int x = xFieldRef.getInt(config);
            int y = yFieldRef.getInt(config);
            float scale = scaleFieldRef.getFloat(config);
            if (scale <= 0) scale = 1.0f;
            
            int width = (int) (defaultWidth * scale);
            int height = (int) (defaultHeight * scale);
            
            // Adjust X position if overlay is outside screen
            if (x < 0) {
                x = 0;
            } else if (x + width > screenWidth) {
                x = Math.max(0, screenWidth - width);
            }
            
            // Adjust Y position if overlay is outside screen
            if (y < 0) {
                y = 0;
            } else if (y + height > screenHeight) {
                y = Math.max(0, screenHeight - height);
            }
            
            xFieldRef.setInt(config, x);
            yFieldRef.setInt(config, y);
        } catch (Exception e) {
            // Silent error handling - field might not exist or be accessible
        }
    }
    
    /**
     * Adjusts button positions (which use relative positioning from right edge)
     */
    private static void adjustButtonPosition(String xOffsetField, String yOffsetField, String scaleField, int defaultWidth, int defaultHeight, int baseY, int screenWidth, int screenHeight) {
        try {
            CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
            java.lang.reflect.Field xOffsetFieldRef = CCLiveUtilitiesConfig.class.getDeclaredField(xOffsetField);
            java.lang.reflect.Field yOffsetFieldRef = CCLiveUtilitiesConfig.class.getDeclaredField(yOffsetField);
            java.lang.reflect.Field scaleFieldRef = CCLiveUtilitiesConfig.class.getDeclaredField(scaleField);
            
            xOffsetFieldRef.setAccessible(true);
            yOffsetFieldRef.setAccessible(true);
            scaleFieldRef.setAccessible(true);
            
            int xOffset = xOffsetFieldRef.getInt(config);
            int yOffset = yOffsetFieldRef.getInt(config);
            float scale = scaleFieldRef.getFloat(config);
            if (scale <= 0) scale = 1.0f;
            
            // Calculate actual position from offset
            int baseX = screenWidth - defaultWidth - 20;
            int actualX = baseX + xOffset;
            int actualY = baseY + yOffset;
            
            int width = (int) (defaultWidth * scale);
            int height = (int) (defaultHeight * scale);
            
            // Adjust if button is outside screen
            if (actualX < 0) {
                xOffset = -baseX;
            } else if (actualX + width > screenWidth) {
                xOffset = screenWidth - baseX - width;
            }
            
            if (actualY < 0) {
                yOffset = -baseY;
            } else if (actualY + height > screenHeight) {
                yOffset = screenHeight - baseY - height;
            }
            
            xOffsetFieldRef.setInt(config, xOffset);
            yOffsetFieldRef.setInt(config, yOffset);
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    /**
     * Adjusts TabInfo overlay positions
     */
    private static void adjustTabInfoOverlay(String xField, String yField, String scaleField, int defaultWidth, int defaultHeight, int screenWidth, int screenHeight) {
        try {
            CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
            java.lang.reflect.Field xFieldRef = CCLiveUtilitiesConfig.class.getDeclaredField(xField);
            java.lang.reflect.Field yFieldRef = CCLiveUtilitiesConfig.class.getDeclaredField(yField);
            java.lang.reflect.Field scaleFieldRef = CCLiveUtilitiesConfig.class.getDeclaredField(scaleField);
            
            xFieldRef.setAccessible(true);
            yFieldRef.setAccessible(true);
            scaleFieldRef.setAccessible(true);
            
            int x = xFieldRef.getInt(config);
            int y = yFieldRef.getInt(config);
            float scale = scaleFieldRef.getFloat(config);
            if (scale <= 0) scale = 1.0f;
            
            int width = (int) (defaultWidth * scale);
            int height = (int) (defaultHeight * scale);
            
            // Adjust X position if overlay is outside screen
            if (x < 0) {
                x = 0;
            } else if (x + width > screenWidth) {
                x = Math.max(0, screenWidth - width);
            }
            
            // Adjust Y position if overlay is outside screen
            if (y < 0) {
                y = 0;
            } else if (y + height > screenHeight) {
                y = Math.max(0, screenHeight - height);
            }
            
            xFieldRef.setInt(config, x);
            yFieldRef.setInt(config, y);
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    public static void toggleOverlayEditor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            if (isOverlayEditorOpen) {
                // Close the overlay editor
                closeOverlayEditor();
            } else {
                // Open the overlay editor
                openOverlayEditor();
            }
        }
    }
    
    public static void openOverlayEditor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            try {
            // Don't replace the current screen, render the overlay editor as an overlay
            // This way the inventory stays visible underneath
            client.setScreen(new OverlayEditorScreen());
            isOverlayEditorOpen = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void closeOverlayEditor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            if (client.currentScreen instanceof OverlayEditorScreen) {
                client.currentScreen.close();
            }
            isOverlayEditorOpen = false;
        }
    }
    
    public static void setOverlayEditorOpen(boolean open) {
        isOverlayEditorOpen = open;
    }
    
    /**
     * Get the overlay editor key binding (for use in screens)
     * @return The KeyBinding instance, or null if not initialized
     */
    public static KeyBinding getOverlayEditorKeyBinding() {
        return overlayEditorKeyBinding;
    }
    
    /**
     * Handle key press directly (for use in mixins when screens are open)
     * @param keyCode The key code (e.g., GLFW.GLFW_KEY_F6)
     * @return true if the key was handled
     */
    public static boolean handleKeyPress(int keyCode) {
        try {
            // Check if the pressed key matches the configured key binding
            // This ensures the same key works in inventories as outside
            if (overlayEditorKeyBinding != null) {
                // Use matchesKey to check if the pressed key matches the configured key binding
                if (overlayEditorKeyBinding.matchesKey(keyCode, -1)) {
                    toggleOverlayEditor();
                    return true;
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
        return false;
    }
}

package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
        if (client.player == null) {
            return;
        }
        
        // Use the registered KeyBinding instead of hardcoded key detection
        if (overlayEditorKeyBinding != null && overlayEditorKeyBinding.wasPressed()) {
            // Overlay Editor is always enabled
                toggleOverlayEditor();
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

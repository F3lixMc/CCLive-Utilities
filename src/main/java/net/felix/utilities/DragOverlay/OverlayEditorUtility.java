package net.felix.utilities.DragOverlay;

import net.felix.utilities.Overall.KeyCategories;

import net.minecraft.resources.Identifier;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Utility für den Overlay Editor
 */
public class OverlayEditorUtility {
    
    private static boolean isInitialized = false;
    private static boolean isOverlayEditorOpen = false;
    
    // KeyMapping for the overlay editor
    private static KeyMapping overlayEditorKeyMapping;
    
    
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        try {
            // Register key binding
            registerKeyMapping();
            
            // Register client tick events
            ClientTickEvents.END_CLIENT_TICK.register(OverlayEditorUtility::onClientTick);
            
            isInitialized = true;
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    private static void registerKeyMapping() {
        // Register overlay editor key binding
        overlayEditorKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.cclive-utilities.overlay-editor",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6, // Default to F6
            KeyCategories.of("cclive-utilities", "overlay")
        ));
    }
    
    private static void onClientTick(Minecraft client) {
        if (client.player == null || client.getWindow() == null) {
            return;
        }
        
        // Use the registered KeyMapping instead of hardcoded key detection
        if (overlayEditorKeyMapping != null && overlayEditorKeyMapping.consumeClick()) {
            // Overlay Editor is always enabled
                toggleOverlayEditor();
        }
    }
    
    public static void toggleOverlayEditor() {
        Minecraft client = Minecraft.getInstance();
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
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            try {
            // Don't replace the current screen, render the overlay editor as an overlay
            // This way the inventory stays visible underneath
            client.setScreen(new OverlayEditorScreen());
            isOverlayEditorOpen = true;
            } catch (Exception e) {
                // Silent error handling
            }
        }
    }
    
    public static void closeOverlayEditor() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            if (client.screen instanceof OverlayEditorScreen) {
                client.screen.onClose();
            }
            isOverlayEditorOpen = false;
        }
    }
    
    public static void setOverlayEditorOpen(boolean open) {
        isOverlayEditorOpen = open;
    }
    
    /**
     * Get the overlay editor key binding (for use in screens)
     * @return The KeyMapping instance, or null if not initialized
     */
    public static KeyMapping getOverlayEditorKeyMapping() {
        return overlayEditorKeyMapping;
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
            if (overlayEditorKeyMapping != null) {
                // Use matchesKey to check if the pressed key matches the configured key binding
                if (overlayEditorKeyMapping.matches(new net.minecraft.client.input.KeyEvent(keyCode, -1, 0))) {
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

package net.felix.utilities;

import net.felix.CCLiveUtilitiesConfig;
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
    private static KeyBinding overlayEditorKeyBinding;
    private static final AtomicBoolean f6KeyPressed = new AtomicBoolean(false);
    private static boolean isOverlayEditorOpen = false;
    
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        try {
            // Register keybinding
            registerKeyBinding();
            
            // Register client tick events
            ClientTickEvents.END_CLIENT_TICK.register(OverlayEditorUtility::onClientTick);
            
            isInitialized = true;
        } catch (Exception e) {
            // Silent error handling
        }
    }
    
    private static void registerKeyBinding() {
        overlayEditorKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cclive-utilities.overlay-editor",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F6, // F6 key as default
            "category.cclive-utilities.overlay"
        ));
    }
    
    private static void onClientTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        
        // Check if overlay editor key is pressed using standard keybind (works in most screens)
        if (overlayEditorKeyBinding != null && overlayEditorKeyBinding.wasPressed()) {
            if (CCLiveUtilitiesConfig.HANDLER.instance().overlayEditorEnabled && 
                CCLiveUtilitiesConfig.HANDLER.instance().showOverlayEditor) {
                toggleOverlayEditor();
            }
        }
        
        // Additional F6 key detection for inventory screens where keybind might not work
        if (client.getWindow() != null) {
            boolean f6CurrentlyPressed = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_F6) == GLFW.GLFW_PRESS;
            
            // Detect key press (transition from not pressed to pressed) - single press detection
            if (f6CurrentlyPressed && !f6KeyPressed.get()) {
                if (CCLiveUtilitiesConfig.HANDLER.instance().overlayEditorEnabled && 
                    CCLiveUtilitiesConfig.HANDLER.instance().showOverlayEditor) {
                    toggleOverlayEditor();
                }
            }
            
            f6KeyPressed.set(f6CurrentlyPressed);
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
            // Don't replace the current screen, render the overlay editor as an overlay
            // This way the inventory stays visible underneath
            client.setScreen(new OverlayEditorScreen());
            isOverlayEditorOpen = true;
        }
    }
    
    public static void closeOverlayEditor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen instanceof OverlayEditorScreen) {
            client.currentScreen.close();
            isOverlayEditorOpen = false;
        }
    }
    
    public static void setOverlayEditorOpen(boolean open) {
        isOverlayEditorOpen = open;
    }
}

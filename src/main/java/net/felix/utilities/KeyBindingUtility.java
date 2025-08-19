package net.felix.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.option.GameOptions;

/**
 * Utility class for handling key bindings in a centralized way.
 * This ensures that custom key bindings are respected instead of hardcoded keys.
 */
public class KeyBindingUtility {
    
    /**
     * Checks if the player list key is currently pressed.
     * This uses the actual key binding instead of hardcoded TAB.
     * 
     * @return true if the player list key is pressed, false otherwise
     */
    public static boolean isPlayerListKeyPressed() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return false;
            }
            
            GameOptions options = client.options;
            KeyBinding playerListKey = options.playerListKey;
            
            if (playerListKey == null) {
                // Fallback to TAB if player list key is not available
                return InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_TAB);
            }
            
            // Check if the actual player list key binding is pressed
            return playerListKey.isPressed();
            
        } catch (Exception e) {
            // Fallback to TAB in case of any errors
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.getWindow() != null) {
                    return InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_TAB);
                }
            } catch (Exception fallbackError) {
                // Silent error handling
            }
            return false;
        }
    }
    
    /**
     * Checks if the player list key was just pressed (not held down).
     * This is useful for toggle functionality.
     * 
     * @return true if the player list key was just pressed, false otherwise
     */
    public static boolean wasPlayerListKeyPressed() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
                return false;
            }
            
            GameOptions options = client.options;
            KeyBinding playerListKey = options.playerListKey;
            
            if (playerListKey == null) {
                // Fallback: check if TAB was just pressed
                return InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_TAB);
            }
            
            // Check if the actual player list key binding was just pressed
            return playerListKey.wasPressed();
            
        } catch (Exception e) {
            // Fallback to TAB in case of any errors
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.getWindow() != null) {
                    return InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_TAB);
                }
            } catch (Exception fallbackError) {
                // Silent error handling
            }
            return false;
        }
    }
}

package net.felix.utilities.Overall;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

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
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.options == null) {
                return false;
            }
            
            Options options = client.options;
            KeyMapping playerListKey = options.keyPlayerList;
            
            if (playerListKey == null) {
                // Fallback to TAB if player list key is not available
                return InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_TAB);
            }
            
            // Check if the actual player list key binding is pressed
            return playerListKey.isDown();
            
        } catch (Exception e) {
            // Fallback to TAB in case of any errors
            try {
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.getWindow() != null) {
                    return InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_TAB);
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
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.options == null) {
                return false;
            }
            
            Options options = client.options;
            KeyMapping playerListKey = options.keyPlayerList;
            
            if (playerListKey == null) {
                // Fallback: check if TAB was just pressed
                return InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_TAB);
            }
            
            // Check if the actual player list key binding was just pressed
            return playerListKey.consumeClick();
            
        } catch (Exception e) {
            // Fallback to TAB in case of any errors
            try {
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.getWindow() != null) {
                    return InputConstants.isKeyDown(client.getWindow(), InputConstants.KEY_TAB);
                }
            } catch (Exception fallbackError) {
                // Silent error handling
            }
            return false;
        }
    }
}

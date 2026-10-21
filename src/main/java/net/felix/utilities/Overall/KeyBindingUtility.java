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
    /**
     * SDL-Scancodes für Ziffern sind nicht 0–9 hintereinander: zuerst 1–9, dann 0.
     */
    public static Character digitFromScancode(int keyCode) {
        if (keyCode >= InputConstants.KEY_1 && keyCode <= InputConstants.KEY_0) {
            int offset = keyCode - InputConstants.KEY_1;
            return offset == 9 ? '0' : (char) ('1' + offset);
        }
        if (keyCode >= InputConstants.KEY_NUMPAD1 && keyCode <= InputConstants.KEY_NUMPAD0) {
            int offset = keyCode - InputConstants.KEY_NUMPAD1;
            return offset == 9 ? '0' : (char) ('1' + offset);
        }
        return null;
    }

    /**
     * Buchstabe zur physischen US-Taste. Y/Z bleiben für deutsche Tastaturen getauscht.
     */
    public static String letterFromScancode(int keyCode, boolean shift) {
        if (keyCode < InputConstants.KEY_A || keyCode > InputConstants.KEY_Z) {
            return null;
        }
        if (keyCode == InputConstants.KEY_Y) {
            return shift ? "Z" : "z";
        }
        if (keyCode == InputConstants.KEY_Z) {
            return shift ? "Y" : "y";
        }
        char upper = (char) ('A' + (keyCode - InputConstants.KEY_A));
        return String.valueOf(shift ? upper : Character.toLowerCase(upper));
    }

    public static String functionKeyName(int keyCode) {
        if (keyCode >= InputConstants.KEY_F1 && keyCode <= InputConstants.KEY_F12) {
            return "F" + (keyCode - InputConstants.KEY_F1 + 1);
        }
        if (keyCode >= InputConstants.KEY_F13 && keyCode <= InputConstants.KEY_F24) {
            return "F" + (13 + keyCode - InputConstants.KEY_F13);
        }
        return null;
    }

    /**
     * SDL-Mausbuttons (links = 1) auf die früheren GLFW-Werte abbilden, die der Rest der Mod vergleicht.
     */
    public static int legacyMouseButton(int sdlButton) {
        if (sdlButton == InputConstants.MOUSE_BUTTON_LEFT) {
            return 0;
        }
        if (sdlButton == InputConstants.MOUSE_BUTTON_RIGHT) {
            return 1;
        }
        if (sdlButton == InputConstants.MOUSE_BUTTON_MIDDLE) {
            return 2;
        }
        return sdlButton;
    }

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
                return InputConstants.isKeyDown(InputConstants.KEY_TAB);
            }
            
            // Check if the actual player list key binding is pressed
            return playerListKey.isDown();
            
        } catch (Exception e) {
            // Fallback to TAB in case of any errors
            try {
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.getWindow() != null) {
                    return InputConstants.isKeyDown(InputConstants.KEY_TAB);
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
                return InputConstants.isKeyDown(InputConstants.KEY_TAB);
            }
            
            // Check if the actual player list key binding was just pressed
            return playerListKey.consumeClick();
            
        } catch (Exception e) {
            // Fallback to TAB in case of any errors
            try {
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.getWindow() != null) {
                    return InputConstants.isKeyDown(InputConstants.KEY_TAB);
                }
            } catch (Exception fallbackError) {
                // Silent error handling
            }
            return false;
        }
    }
}

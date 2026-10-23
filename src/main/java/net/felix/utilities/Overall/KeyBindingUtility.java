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

    public static boolean isShiftDown(int modifiers) {
        return (modifiers & InputConstants.MOD_SHIFT) != 0;
    }

    public static boolean isCtrlDown(int modifiers) {
        return (modifiers & InputConstants.MOD_CONTROL) != 0;
    }

    /**
     * AltGr ist rechte Alt-Taste, unter Windows oft zusammen mit Strg.
     */
    public static boolean isAltGrDown(int modifiers) {
        return (modifiers & MOD_RALT) != 0
                || ((modifiers & InputConstants.MOD_ALT) != 0 && (modifiers & InputConstants.MOD_CONTROL) != 0);
    }

    /**
     * Zeichen zur physischen Taste auf einem deutschen Layout.
     * {@code keyCode} ist der SDL-Scancode aus {@code KeyEvent.key()}.
     */
    public static String characterFromScancode(int keyCode, int modifiers) {
        if (isCtrlDown(modifiers) && !isAltGrDown(modifiers)) {
            return null;
        }
        if (isAltGrDown(modifiers)) {
            return germanAltGrCharacter(keyCode);
        }

        boolean shift = isShiftDown(modifiers);
        boolean caps = (modifiers & InputConstants.MOD_CAPS_LOCK) != 0;
        String letter = letterFromScancode(keyCode, shift ^ caps);
        if (letter != null) {
            return letter;
        }
        if (keyCode == InputConstants.KEY_SPACE) {
            return " ";
        }

        Character digit = digitFromScancode(keyCode);
        if (digit != null && keyCode >= InputConstants.KEY_NUMPAD1 && keyCode <= InputConstants.KEY_NUMPAD0) {
            return String.valueOf(digit);
        }
        if (keyCode == InputConstants.KEY_ADD || keyCode == org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_PLUS) {
            return "+";
        }
        if (keyCode == org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_MINUS) {
            return "-";
        }
        if (keyCode == InputConstants.KEY_MULTIPLY || keyCode == org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_MULTIPLY) {
            return "*";
        }
        if (keyCode == org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_DIVIDE) {
            return "/";
        }
        if (keyCode == InputConstants.KEY_NUMPADCOMMA || keyCode == org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_PERIOD) {
            return shift ? "." : ",";
        }

        if (digit != null) {
            if (!shift) {
                return String.valueOf(digit);
            }
            return germanShiftedDigit(keyCode);
        }
        return germanSymbol(keyCode, shift, caps);
    }

    private static final int MOD_RALT = 0x200;

    private static String germanShiftedDigit(int keyCode) {
        if (keyCode == InputConstants.KEY_1) return "!";
        if (keyCode == InputConstants.KEY_2) return "\"";
        if (keyCode == InputConstants.KEY_3) return "§";
        if (keyCode == InputConstants.KEY_4) return "$";
        if (keyCode == InputConstants.KEY_5) return "%";
        if (keyCode == InputConstants.KEY_6) return "&";
        if (keyCode == InputConstants.KEY_7) return "/";
        if (keyCode == InputConstants.KEY_8) return "(";
        if (keyCode == InputConstants.KEY_9) return ")";
        if (keyCode == InputConstants.KEY_0) return "=";
        return null;
    }

    private static String germanSymbol(int keyCode, boolean shift, boolean caps) {
        boolean upper = shift ^ caps;
        if (keyCode == InputConstants.KEY_MINUS) return shift ? "?" : "ß";
        if (keyCode == InputConstants.KEY_EQUALS) return shift ? "`" : "´";
        if (keyCode == InputConstants.KEY_LBRACKET) return upper ? "Ü" : "ü";
        if (keyCode == InputConstants.KEY_RBRACKET) return shift ? "*" : "+";
        if (keyCode == InputConstants.KEY_BACKSLASH || keyCode == org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_NONUSHASH) {
            return shift ? "'" : "#";
        }
        if (keyCode == InputConstants.KEY_SEMICOLON) return upper ? "Ö" : "ö";
        if (keyCode == InputConstants.KEY_APOSTROPHE) return upper ? "Ä" : "ä";
        if (keyCode == InputConstants.KEY_COMMA) return shift ? ";" : ",";
        if (keyCode == InputConstants.KEY_PERIOD) return shift ? ":" : ".";
        if (keyCode == InputConstants.KEY_SLASH) return shift ? "_" : "-";
        if (keyCode == InputConstants.KEY_GRAVE) return shift ? "°" : "^";
        if (keyCode == org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_NONUSBACKSLASH) return shift ? ">" : "<";
        return null;
    }

    private static String germanAltGrCharacter(int keyCode) {
        if (keyCode == InputConstants.KEY_Q) return "@";
        if (keyCode == InputConstants.KEY_E) return "€";
        if (keyCode == InputConstants.KEY_2) return "²";
        if (keyCode == InputConstants.KEY_3) return "³";
        if (keyCode == InputConstants.KEY_7) return "{";
        if (keyCode == InputConstants.KEY_8) return "[";
        if (keyCode == InputConstants.KEY_9) return "]";
        if (keyCode == InputConstants.KEY_0) return "}";
        if (keyCode == InputConstants.KEY_MINUS) return "\\";
        if (keyCode == InputConstants.KEY_RBRACKET) return "~";
        if (keyCode == org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_NONUSBACKSLASH) return "|";
        if (keyCode == InputConstants.KEY_M) return "µ";
        return null;
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

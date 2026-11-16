package net.felix.features.itemviewer;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.felix.features.itemviewer.data.ItemData;
import net.felix.utilities.Other.clipboard.ClipboardUtility;
import net.felix.utilities.Overall.KeyCategories;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import static net.felix.features.itemviewer.inventory.InventoryItemCapture.tryPinHoveredInventoryItem;
import static net.felix.features.itemviewer.search.SearchFieldState.isTextFieldFocused;
import static net.felix.features.itemviewer.search.SearchFieldState.searchFieldFocused;
import static net.felix.features.itemviewer.search.SearchFieldState.setSearchFieldFocused;
import static net.felix.features.itemviewer.ui.ViewerState.getHoveredItemFromGrid;
import static net.felix.features.itemviewer.ui.ViewerState.isMinimized;
import static net.felix.features.itemviewer.ui.ViewerState.isVisible;

/**
 * Tastenbelegungen des Item Viewers (Ein-/Ausblenden, Clipboard-Pin) und deren Anzeigetexte.
 */
public final class ItemViewerHotkeys {

    private ItemViewerHotkeys() {
    }

    public static KeyMapping toggleKeyMapping;

    private static KeyMapping clipboardPinKeyMapping;

    static void registerKeybind() {
        toggleKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.cclive-utilities.itemviewer.toggle",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_I,
            KeyCategories.of("cclive-utilities", "itemviewer")
        ));
        
        // Registriere KeyMapping für Bauplan Anpinnen (Clipboard)
        clipboardPinKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.cclive-utilities.clipboard.pin",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_P,
            KeyCategories.of("cclive-utilities", "itemviewer")
        ));
    }

    /**
     * Gibt den formatierten Hotkey-Text für das Toggle des ItemViewers zurück
     * @return Formatierter Hotkey-Text (z.B. "I")
     * Verwendet getBoundKeyLocalizedText() um den aktuellen Hotkey zu bekommen (auch nach Config-Änderungen)
     */
    public static String getToggleHotkeyText() {
        if (toggleKeyMapping == null) {
            return "I"; // Fallback
        }
        
        try {
            // Verwende getBoundKeyLocalizedText() um den aktuellen Hotkey-Text zu bekommen
            // Dies funktioniert auch nach Änderungen in der Config
            java.lang.reflect.Method getBoundKeyLocalizedTextMethod = toggleKeyMapping.getClass().getMethod("getBoundKeyLocalizedText");
            Object localizedText = getBoundKeyLocalizedTextMethod.invoke(toggleKeyMapping);
            if (localizedText instanceof net.minecraft.network.chat.Component) {
                String hotkeyString = ((net.minecraft.network.chat.Component) localizedText).getString();
                // Entferne mögliche Formatierungs-Codes
                hotkeyString = hotkeyString.replaceAll("§[0-9a-fk-or]", "");
                return hotkeyString;
            }
        } catch (Exception e) {
            // Fallback: Versuche über getBoundKey() und KeyCode
            try {
                java.lang.reflect.Method getBoundKeyMethod = toggleKeyMapping.getClass().getMethod("getBoundKey");
                Object boundKey = getBoundKeyMethod.invoke(toggleKeyMapping);
                
                // Hole den KeyCode
                java.lang.reflect.Method getCodeMethod = boundKey.getClass().getMethod("getValue");
                int keyCode = (Integer) getCodeMethod.invoke(boundKey);
                
                // Konvertiere GLFW Key Code zu lesbarem String
                String letter = net.felix.utilities.Overall.KeyBindingUtility.letterFromScancode(keyCode, true);
                Character digit = net.felix.utilities.Overall.KeyBindingUtility.digitFromScancode(keyCode);
                if (letter != null) {
                    return letter;
                } else if (digit != null) {
                    return String.valueOf(digit);
                } else {
                    return "Key " + keyCode;
                }
            } catch (Exception e2) {
                // Fallback: Standard "I"
                return "I";
            }
        }
        
        return "I"; // Fallback
    }

    /**
     * Gibt den formatierten Hotkey-Text für das Clipboard-Anpinnen zurück
     * @return Formatierter Hotkey-Text (z.B. "P")
     * Verwendet getBoundKeyLocalizedText() um den aktuellen Hotkey zu bekommen (auch nach Config-Änderungen)
     */
    public static String getClipboardPinHotkeyText() {
        if (clipboardPinKeyMapping == null) {
            return "P"; // Fallback
        }
        
        try {
            // Verwende getBoundKeyLocalizedText() direkt um den aktuellen Hotkey-Text zu bekommen
            // Dies funktioniert auch nach Änderungen in der Config
            net.minecraft.network.chat.Component localizedText = clipboardPinKeyMapping.getTranslatedKeyMessage();
            if (localizedText != null) {
                String hotkeyString = localizedText.getString();
                // Entferne mögliche Formatierungs-Codes
                hotkeyString = hotkeyString.replaceAll("§[0-9a-fk-or]", "");
                if (!hotkeyString.isEmpty()) {
                    return hotkeyString;
                }
            }
        } catch (Exception e) {
            // Fallback: Versuche über getBoundKey() und KeyCode via Reflection
        }
        
        // Fallback: Versuche über getBoundKey() und KeyCode via Reflection
        try {
            java.lang.reflect.Method getBoundKeyMethod = clipboardPinKeyMapping.getClass().getMethod("getBoundKey");
            Object boundKeyObj = getBoundKeyMethod.invoke(clipboardPinKeyMapping);
            if (boundKeyObj != null) {
                // Hole den KeyCode
                java.lang.reflect.Method getCodeMethod = boundKeyObj.getClass().getMethod("getValue");
                int keyCode = (Integer) getCodeMethod.invoke(boundKeyObj);
                
                // Konvertiere GLFW Key Code zu lesbarem String
                String letter = net.felix.utilities.Overall.KeyBindingUtility.letterFromScancode(keyCode, true);
                Character digit = net.felix.utilities.Overall.KeyBindingUtility.digitFromScancode(keyCode);
                if (letter != null) {
                    return letter;
                } else if (digit != null) {
                    return String.valueOf(digit);
                } else if (keyCode == InputConstants.KEY_SPACE) {
                    return "SPACE";
                } else if (keyCode == InputConstants.KEY_LSHIFT || keyCode == InputConstants.KEY_RSHIFT) {
                    return "SHIFT";
                } else if (keyCode == InputConstants.KEY_LCONTROL || keyCode == InputConstants.KEY_RCONTROL) {
                    return "CTRL";
                } else if (keyCode == InputConstants.KEY_LALT || keyCode == InputConstants.KEY_RALT) {
                    return "ALT";
                } else {
                    // Versuche über getTranslationKey() einen lesbaren Namen zu bekommen
                    try {
                        java.lang.reflect.Method getTranslationKeyMethod = boundKeyObj.getClass().getMethod("getTranslationKey");
                        String translationKey = (String) getTranslationKeyMethod.invoke(boundKeyObj);
                        if (translationKey != null && !translationKey.isEmpty()) {
                            // Extrahiere den Key-Namen aus dem Translation-Key
                            String[] parts = translationKey.split("\\.");
                            if (parts.length > 0) {
                                String keyName = parts[parts.length - 1];
                                // Konvertiere key.keyboard.x zu "X"
                                if (keyName.length() == 1) {
                                    return keyName.toUpperCase();
                                }
                                return keyName.toUpperCase();
                            }
                        }
                    } catch (Exception e2) {
                        // Ignoriere
                    }
                    return "Key " + keyCode;
                }
            }
        } catch (Exception e) {
            // Fallback: Standard "P"
        }
        
        return "P"; // Fallback
    }

    /**
     * Verarbeitet die Hotkeys pro Client-Tick, solange kein Screen offen ist
     * (bei offenem Screen greift {@link #handleKeyPress(int, int, int)} über den ScreenMixin).
     */
    public static void onClientTick(Minecraft client) {
        if (toggleKeyMapping != null && toggleKeyMapping.consumeClick()) {
            // Verhindert, dass der Hotkey aktiviert wird, wenn der Spieler "I" in ein Textfeld tippt
            if (isTextFieldFocused(client)) {
                return;
            }

            // Gleiche Funktionalität wie die Buttons: minimieren/maximieren statt ausblenden
            if (isVisible) {
                isMinimized = !isMinimized;
            } else {
                isVisible = true;
                isMinimized = false;
            }
        }

        if (clipboardPinKeyMapping != null && clipboardPinKeyMapping.consumeClick()
                && client.gui.screen() == null) {
            if (isTextFieldFocused(client)) {
                return;
            }

            if (isVisible()) {
                ItemData hoveredItem = getHoveredItemFromGrid();
                if (hoveredItem != null && ClipboardUtility.isClipboardPinnable(hoveredItem)) {
                    ClipboardUtility.addBlueprint(hoveredItem);
                }
            }
        }
    }

    /**
     * Wird vom ScreenMixin aufgerufen, um Keybinds auch in Screens zu behandeln
     * @param keyCode GLFW key code
     * @param scanCode GLFW scan code
     * @param modifiers Modifier keys
     * @return true wenn der Keybind behandelt wurde, false sonst
     */
    public static boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        Minecraft client = Minecraft.getInstance();
        
        // Prüfe Clipboard-Pin Hotkey
        if (clipboardPinKeyMapping != null && clipboardPinKeyMapping.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0))) {
            // Prüfe ob ein Textfeld fokussiert ist
            if (isTextFieldFocused(client)) {
                return false; // Ignoriere Hotkey wenn Textfeld fokussiert ist
            }
            
            // Prüfe zuerst, ob ItemViewer sichtbar ist und ein Item gehovered wird
            // Wenn ja, hat ItemViewer Priorität
            if (isVisible()) {
                // Hole gehoveres Item aus dem Grid
                ItemData hoveredItem = getHoveredItemFromGrid();
                
                if (hoveredItem != null && net.felix.utilities.Other.clipboard.ClipboardUtility.isClipboardPinnable(hoveredItem)) {
                    net.felix.utilities.Other.clipboard.ClipboardUtility.addBlueprint(hoveredItem);
                    return true;
                }
            }
            
            // Wenn kein Item im Grid gehovered wurde (oder ItemViewer nicht sichtbar), prüfe Inventar
            if (client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> handledScreen) {
                if (tryPinHoveredInventoryItem(handledScreen, client)) {
                    return true;
                }
            }
        }
        
        // Prüfe Toggle Hotkey
        if (toggleKeyMapping == null) {
            return false;
        }
        
        // Prüfe ob unser Keybind gedrückt wurde
        // Verwende matchesKey() um zu prüfen ob der Keybind passt
        if (toggleKeyMapping.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0))) {
            // Prüfe ob ein Textfeld fokussiert ist (Chat, Inventar-Suchfeld, etc.)
            // Verhindert, dass der Hotkey aktiviert wird, wenn der Spieler "I" tippt
            if (isTextFieldFocused(client)) {
                return false; // Ignoriere Hotkey wenn Textfeld fokussiert ist
            }
            
            // Wenn Suchfeld fokussiert ist, nicht togglen (nur Fokus entfernen)
            if (searchFieldFocused) {
                setSearchFieldFocused(false);
                return true;
            }
            // Gleiche Funktionalität wie die Buttons: minimieren/maximieren statt ausblenden
            if (isVisible) {
                // Wenn sichtbar, toggle minimiert-Status
                isMinimized = !isMinimized;
            } else {
                // Wenn ausgeblendet, einblenden und ausklappen
                isVisible = true;
                isMinimized = false;
            }
            return true;
        }
        
        return false;
    }
}

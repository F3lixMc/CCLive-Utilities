package net.felix.utilities.Aincraft.itemviewer.filtermenu;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.gui.Font;

import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.ROW_HEIGHT;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.draftInputFieldText;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.focusedInputKey;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.groups;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.inputCursorBlinkTime;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.inputCursorVisible;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.inputFieldStates;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.inputFieldText;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.scrollOffset;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterSelection.commitInputFieldChange;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterSelection.optionKey;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.InputFieldView.computeVisibleSlice;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.InputFieldView.findFieldLayout;

/**
 * Textbearbeitung der Eingabefelder (Cursor, Auswahl, Einfuegen, Loeschen, Zeichenfilter).
 */
public final class InputFieldEditor {

    private InputFieldEditor() {
    }

    static final int COST_AMOUNT_FIELD_CHARS = 7;

    static final List<String> COST_MENU_OPERATORS = List.of("=", ">=", "<=");

    static final int COST_ITEM_FIELD_CHARS = 12;

    private static final int COST_ITEM_MAX_LENGTH = 32;

    private static final int COST_AMOUNT_MAX_LENGTH = 8;

    public static boolean isCtrlDown(int modifiers) {
        return (modifiers & InputConstants.MOD_CONTROL) != 0;
    }

    public static boolean isShiftDown(int modifiers) {
        return (modifiers & InputConstants.MOD_SHIFT) != 0;
    }

    static int clampCursor(int cursor, int textLength) {
        return Math.max(0, Math.min(cursor, textLength));
    }

    static InputFieldState getOrCreateState(String inputKey) {
        return inputFieldStates.computeIfAbsent(inputKey, key -> new InputFieldState());
    }

    static void setInputFieldText(String inputKey, String value) {
        inputFieldText.put(inputKey, value);
        draftInputFieldText.put(inputKey, value);
        InputFieldState state = getOrCreateState(inputKey);
        state.cursor = value.length();
        state.clearSelection();
    }

    static void clearInputField(String inputKey) {
        setInputFieldText(inputKey, "");
    }

    private static int maxLengthForType(RowInputType type) {
        return switch (type) {
            case MODIFIER_DIGIT -> 1;
            case COST_AMOUNT -> COST_AMOUNT_MAX_LENGTH;
            case COST_ITEM -> COST_ITEM_MAX_LENGTH;
            default -> 0;
        };
    }

    static String deleteSelection(String text, InputFieldState state) {
        if (!state.hasSelection()) {
            return text;
        }
        int min = state.selectionMin();
        int max = state.selectionMax();
        state.cursor = min;
        state.clearSelection();
        return text.substring(0, min) + text.substring(max);
    }

    static String deleteBeforeCursor(String text, InputFieldState state) {
        text = deleteSelection(text, state);
        if (state.cursor > 0) {
            text = text.substring(0, state.cursor - 1) + text.substring(state.cursor);
            state.cursor--;
        }
        return text;
    }

    static String deleteAfterCursor(String text, InputFieldState state) {
        text = deleteSelection(text, state);
        if (state.cursor < text.length()) {
            text = text.substring(0, state.cursor) + text.substring(state.cursor + 1);
        }
        return text;
    }

    static void moveCursorHorizontal(String text, InputFieldState state, int delta, boolean extendSelection) {
        int next = clampCursor(state.cursor + delta, text.length());
        if (extendSelection) {
            if (!state.hasSelection() && state.selectionStart < 0) {
                state.selectionStart = state.cursor;
                state.selectionEnd = state.cursor;
            }
            state.cursor = next;
            state.selectionEnd = next;
        } else {
            state.clearSelection();
            state.cursor = next;
        }
    }

    static void moveCursorTo(String text, InputFieldState state, int position, boolean extendSelection) {
        int next = clampCursor(position, text.length());
        if (extendSelection) {
            if (!state.hasSelection() && state.selectionStart < 0) {
                state.selectionStart = state.cursor;
                state.selectionEnd = state.cursor;
            }
            state.cursor = next;
            state.selectionEnd = next;
        } else {
            state.clearSelection();
            state.cursor = next;
        }
    }

    static void insertAtCursor(String insert) {
        if (focusedInputKey == null || insert == null || insert.isEmpty()) {
            return;
        }
        RowInputType type = getInputTypeForKey(focusedInputKey);
        InputFieldState state = getOrCreateState(focusedInputKey);
        String text = deleteSelection(inputFieldText.getOrDefault(focusedInputKey, ""), state);
        int maxLen = maxLengthForType(type);
        int pos = clampCursor(state.cursor, text.length());

        if (type == RowInputType.MODIFIER_DIGIT) {
            text = String.valueOf(insert.charAt(0));
            state.cursor = text.length();
        } else {
            String allowed = filterInsertForType(insert, type);
            if (allowed.isEmpty()) {
                return;
            }
            if (text.length() + allowed.length() > maxLen) {
                allowed = allowed.substring(0, maxLen - text.length());
                if (allowed.isEmpty()) {
                    return;
                }
            }
            text = text.substring(0, pos) + allowed + text.substring(pos);
            state.cursor = pos + allowed.length();
        }

        inputFieldText.put(focusedInputKey, text);
        state.clearSelection();
        commitInputFieldChange(focusedInputKey);
        updateInputCursorBlink();
    }

    private static String filterInsertForType(String insert, RowInputType type) {
        if (type == RowInputType.COST_AMOUNT) {
            if (focusedInputKey != null && isItemScoreAmountField(focusedInputKey)) {
                return filterDecimalInsert(
                        inputFieldText.getOrDefault(focusedInputKey, ""), insert);
            }
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < insert.length(); i++) {
                char c = insert.charAt(i);
                if (c >= '0' && c <= '9') {
                    digits.append(c);
                }
            }
            return digits.toString();
        }
        return insert.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
    }

    static boolean isItemScoreAmountField(String inputKey) {
        return optionKey("Att.-Anzahl", "ItemScore").equals(inputKey);
    }

    private static String filterDecimalInsert(String current, String insert) {
        StringBuilder result = new StringBuilder();
        boolean hasSeparator = current.contains(".") || current.contains(",");
        for (int i = 0; i < insert.length(); i++) {
            char c = insert.charAt(i);
            if (c >= '0' && c <= '9') {
                result.append(c);
            } else if ((c == '.' || c == ',') && !hasSeparator) {
                result.append('.');
                hasSeparator = true;
            }
        }
        return result.toString();
    }

    static boolean isModifierOnlyKey(int keyCode) {
        return keyCode == InputConstants.KEY_LSHIFT
                || keyCode == InputConstants.KEY_RSHIFT
                || keyCode == InputConstants.KEY_LCONTROL
                || keyCode == InputConstants.KEY_RCONTROL
                || keyCode == InputConstants.KEY_LALT
                || keyCode == InputConstants.KEY_RALT
                || keyCode == InputConstants.KEY_CAPSLOCK;
    }

    static RowInputType getInputTypeForKey(String inputKey) {
        for (FilterGroup group : groups) {
            for (FilterOption option : group.options) {
                if (inputKey.equals(optionKey(group.title, option.label))) {
                    return option.inputType;
                }
            }
        }
        return RowInputType.NONE;
    }

    static Character resolveDigitKey(int keyCode) {
        return net.felix.utilities.Overall.keybinding.KeyBindingUtility.digitFromScancode(keyCode);
    }

    static String resolveItemTextKey(int keyCode, int modifiers) {
        boolean shift = isShiftDown(modifiers);
        if (keyCode == InputConstants.KEY_SPACE) {
            return " ";
        }
        if (keyCode == InputConstants.KEY_MINUS || keyCode == org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_MINUS) {
            return shift ? "_" : "-";
        }
        String letter = net.felix.utilities.Overall.keybinding.KeyBindingUtility.letterFromScancode(keyCode, shift);
        if (letter != null) {
            return letter;
        }
        Character digit = net.felix.utilities.Overall.keybinding.KeyBindingUtility.digitFromScancode(keyCode);
        if (digit != null) {
            return String.valueOf(digit);
        }
        return null;
    }

    static void focusInputFieldAtMouse(Font tr, String inputKey, double mouseX) {
        InputFieldState state = getOrCreateState(inputKey);
        String text = inputFieldText.getOrDefault(inputKey, "");
        FieldLayout layout = findFieldLayout(inputKey);
        if (layout == null) {
            state.cursor = text.length();
            state.clearSelection();
            return;
        }
        state.cursor = resolveCursorIndexAtX(tr, text, layout, mouseX, state);
        state.clearSelection();
    }

    private static int resolveCursorIndexAtX(Font tr, String text, FieldLayout layout, double mouseX,
            InputFieldState referenceState) {
        if (text.isEmpty()) {
            return 0;
        }

        int bestIndex = text.length();
        double bestDistance = Double.MAX_VALUE;
        for (int index = 0; index <= text.length(); index++) {
            InputFieldState probe = new InputFieldState();
            probe.cursor = index;
            if (referenceState.hasSelection()) {
                probe.selectionStart = referenceState.selectionStart;
                probe.selectionEnd = referenceState.selectionEnd;
            }

            VisibleFieldSlice slice = computeVisibleSlice(tr, text, layout.fieldW(), probe, true, layout.inputType());
            int textX = layout.fieldX();
            if (slice.centered()) {
                textX = layout.fieldX() + (layout.fieldW() - tr.width(slice.display())) / 2;
            }

            int caretIndex = Math.max(0, Math.min(slice.displayCursor(), slice.display().length()));
            int caretX = textX + tr.width(slice.display().substring(0, caretIndex));
            double distance = Math.abs(mouseX - caretX);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    static String findInputFieldAt(double mouseX, double mouseY) {
        for (FilterGroup group : groups) {
            for (FilterOption option : group.options) {
                if (option.inputType == RowInputType.NONE || option.inputType == RowInputType.COST_OPERATOR) {
                    continue;
                }
                int rowY = option.y - scrollOffset;
                if (mouseX >= option.countFieldX && mouseX < option.countFieldX + option.countFieldW
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    return optionKey(group.title, option.label);
                }
            }
        }
        return null;
    }

    static void updateInputCursorBlink() {
        long now = System.currentTimeMillis();
        if (now - inputCursorBlinkTime > 500) {
            inputCursorVisible = !inputCursorVisible;
            inputCursorBlinkTime = now;
        }
    }
}

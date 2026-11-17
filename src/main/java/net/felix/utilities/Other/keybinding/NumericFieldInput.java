package net.felix.utilities.Other.keybinding;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Ziffernfeld mit Cursor, Markierung, Pfeiltasten, Pos1, Ende und Strg-Kürzeln.
 */
public final class NumericFieldInput {
    public enum Result {
        IGNORED,
        HANDLED,
        SUBMIT,
        CANCEL
    }

    public static final class State {
        public String text = "";
        public int cursor;
        public int anchor = -1;

        public void setText(String value) {
            text = value == null ? "" : value;
            cursor = text.length();
            anchor = -1;
        }

        public void focusAtEnd() {
            cursor = text.length();
            anchor = -1;
        }
    }

    private NumericFieldInput() {
    }

    public static Result handle(State state, int keyCode, int modifiers, int maxLength) {
        if (state == null) {
            return Result.IGNORED;
        }
        clamp(state);
        boolean ctrl = KeyBindingUtility.isCtrlDown(modifiers) && !KeyBindingUtility.isAltGrDown(modifiers);
        boolean shift = KeyBindingUtility.isShiftDown(modifiers);

        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            clearSelection(state);
            return Result.SUBMIT;
        }
        if (keyCode == InputConstants.KEY_ESCAPE) {
            clearSelection(state);
            return Result.CANCEL;
        }
        if (ctrl && keyCode == InputConstants.KEY_A) {
            if (!state.text.isEmpty()) {
                state.anchor = 0;
                state.cursor = state.text.length();
            }
            return Result.HANDLED;
        }
        if (ctrl && keyCode == InputConstants.KEY_C) {
            copySelection(state);
            return Result.HANDLED;
        }
        if (ctrl && keyCode == InputConstants.KEY_X) {
            copySelection(state);
            deleteSelection(state);
            return Result.HANDLED;
        }
        if (ctrl && keyCode == InputConstants.KEY_V) {
            pasteDigits(state, maxLength);
            return Result.HANDLED;
        }
        if (keyCode == InputConstants.KEY_BACKSPACE) {
            if (!deleteSelection(state)) {
                int from = ctrl ? 0 : Math.max(0, state.cursor - 1);
                if (from < state.cursor) {
                    state.text = state.text.substring(0, from) + state.text.substring(state.cursor);
                    state.cursor = from;
                }
            }
            return Result.HANDLED;
        }
        if (keyCode == InputConstants.KEY_DELETE) {
            if (!deleteSelection(state) && state.cursor < state.text.length()) {
                state.text = state.text.substring(0, state.cursor) + state.text.substring(state.cursor + 1);
            }
            return Result.HANDLED;
        }
        if (keyCode == InputConstants.KEY_LEFT) {
            moveCursor(state, ctrl ? 0 : state.cursor - 1, shift);
            return Result.HANDLED;
        }
        if (keyCode == InputConstants.KEY_RIGHT) {
            moveCursor(state, ctrl ? state.text.length() : state.cursor + 1, shift);
            return Result.HANDLED;
        }
        if (keyCode == InputConstants.KEY_HOME) {
            moveCursor(state, 0, shift);
            return Result.HANDLED;
        }
        if (keyCode == InputConstants.KEY_END) {
            moveCursor(state, state.text.length(), shift);
            return Result.HANDLED;
        }
        if (ctrl) {
            return Result.HANDLED;
        }
        Character digit = KeyBindingUtility.digitFromScancode(keyCode);
        if (digit != null) {
            insert(state, String.valueOf(digit), maxLength);
            return Result.HANDLED;
        }
        return Result.IGNORED;
    }

    public static void draw(GuiGraphicsExtractor context, Font font, State state, int x, int y, int color, boolean focused) {
        if (state == null || font == null) {
            return;
        }
        clamp(state);
        String text = state.text;
        if (focused && state.anchor >= 0 && state.anchor != state.cursor) {
            int start = Math.min(state.anchor, state.cursor);
            int end = Math.max(state.anchor, state.cursor);
            int startX = x + font.width(text.substring(0, start));
            int endX = x + font.width(text.substring(0, end));
            context.fill(startX, y - 1, Math.max(endX, startX + 1), y + 9, 0xFF3355AA);
        }
        if (!text.isEmpty()) {
            context.text(font, text, x, y, color, false);
        }
        if (focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cursorX = x + font.width(text.substring(0, state.cursor));
            context.fill(cursorX, y - 1, cursorX + 1, y + 9, 0xFFFFFFFF);
        }
    }

    private static void insert(State state, String value, int maxLength) {
        deleteSelection(state);
        if (value == null || value.isEmpty() || state.text.length() >= maxLength) {
            return;
        }
        int room = maxLength - state.text.length();
        if (value.length() > room) {
            value = value.substring(0, room);
        }
        state.text = state.text.substring(0, state.cursor) + value + state.text.substring(state.cursor);
        state.cursor += value.length();
        state.anchor = -1;
    }

    private static void pasteDigits(State state, int maxLength) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        String clipboard = client.keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isEmpty()) {
            return;
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < clipboard.length(); i++) {
            char c = clipboard.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        insert(state, digits.toString(), maxLength);
    }

    private static void copySelection(State state) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || state.text.isEmpty()) {
            return;
        }
        String copied = state.text;
        if (state.anchor >= 0 && state.anchor != state.cursor) {
            int start = Math.min(state.anchor, state.cursor);
            int end = Math.max(state.anchor, state.cursor);
            copied = state.text.substring(start, end);
        }
        client.keyboardHandler.setClipboard(copied);
    }

    private static boolean deleteSelection(State state) {
        if (state.anchor < 0 || state.anchor == state.cursor) {
            return false;
        }
        int start = Math.min(state.anchor, state.cursor);
        int end = Math.max(state.anchor, state.cursor);
        state.text = state.text.substring(0, start) + state.text.substring(end);
        state.cursor = start;
        state.anchor = -1;
        return true;
    }

    private static void moveCursor(State state, int index, boolean extend) {
        int clamped = Math.max(0, Math.min(state.text.length(), index));
        if (extend) {
            if (state.anchor < 0) {
                state.anchor = state.cursor;
            }
            state.cursor = clamped;
            return;
        }
        state.cursor = clamped;
        state.anchor = -1;
    }

    private static void clearSelection(State state) {
        state.anchor = -1;
    }

    private static void clamp(State state) {
        if (state.text == null) {
            state.text = "";
        }
        state.cursor = Math.max(0, Math.min(state.text.length(), state.cursor));
        if (state.anchor > state.text.length()) {
            state.anchor = state.text.length();
        }
    }
}

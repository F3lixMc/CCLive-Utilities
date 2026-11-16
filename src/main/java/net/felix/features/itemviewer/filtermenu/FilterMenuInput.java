package net.felix.features.itemviewer.filtermenu;

import com.mojang.blaze3d.platform.InputConstants;
import net.felix.features.itemviewer.search.SearchFieldState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import static net.felix.features.itemviewer.filtermenu.FilterMenu.close;
import static net.felix.features.itemviewer.filtermenu.FilterMenu.open;
import static net.felix.features.itemviewer.filtermenu.FilterMenuDimensions.CLOSE_BUTTON_SIZE;
import static net.felix.features.itemviewer.filtermenu.FilterMenuDimensions.PICKER_BUTTON_SIZE;
import static net.felix.features.itemviewer.filtermenu.FilterMenuDimensions.PICKER_MAX_VISIBLE;
import static net.felix.features.itemviewer.filtermenu.FilterMenuDimensions.ROW_HEIGHT;
import static net.felix.features.itemviewer.filtermenu.FilterMenuLayout.computePanelLayout;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.focusedInputKey;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.groups;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.hovered;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.inputFieldText;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.openPickerKey;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerDropdownEntries;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerScrollOffset;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerScrollbarDragOffset;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerScrollbarDragging;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.scrollOffset;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.scrollbarDragOffset;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.scrollbarDragging;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.selectedOptionKeys;
import static net.felix.features.itemviewer.filtermenu.FilterSelection.applySelections;
import static net.felix.features.itemviewer.filtermenu.FilterSelection.clearAllInputFields;
import static net.felix.features.itemviewer.filtermenu.FilterSelection.commitInputFieldChange;
import static net.felix.features.itemviewer.filtermenu.FilterSelection.optionKey;
import static net.felix.features.itemviewer.filtermenu.FilterSelection.toggleOption;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.COST_MENU_OPERATORS;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.clampCursor;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.clearInputField;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.deleteAfterCursor;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.deleteBeforeCursor;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.deleteSelection;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.findInputFieldAt;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.focusInputFieldAtMouse;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.getInputTypeForKey;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.getOrCreateState;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.insertAtCursor;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.isCtrlDown;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.isItemScoreAmountField;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.isModifierOnlyKey;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.isShiftDown;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.moveCursorHorizontal;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.moveCursorTo;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.resolveDigitKey;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.resolveItemTextKey;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.setInputFieldText;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.updateInputCursorBlink;
import static net.felix.features.itemviewer.filtermenu.ItemPicker.clampPickerScrollOffset;
import static net.felix.features.itemviewer.filtermenu.ItemPicker.handleItemPickerDropdownClick;
import static net.felix.features.itemviewer.filtermenu.ItemPicker.handlePickerScrollbarClick;
import static net.felix.features.itemviewer.filtermenu.ItemPicker.resetPickerScrollbarState;
import static net.felix.features.itemviewer.filtermenu.ItemPicker.setPickerScrollFromThumbTop;
import static net.felix.features.itemviewer.filtermenu.ItemPicker.toggleItemPicker;
import static net.felix.features.itemviewer.filtermenu.MainScrollbar.clampScrollOffset;
import static net.felix.features.itemviewer.filtermenu.MainScrollbar.finalizeScrollLayout;
import static net.felix.features.itemviewer.filtermenu.MainScrollbar.handleScrollbarClick;
import static net.felix.features.itemviewer.filtermenu.MainScrollbar.setScrollFromThumbTop;

/**
 * Maus- und Tastatur-Eingaben des Filter-Menues (Klick, Scroll, Drag, Taste).
 */
public final class FilterMenuInput {

    private FilterMenuInput() {
    }

    public static boolean handleClick(double mouseX, double mouseY, int button) {
        if (!open) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        PanelLayout layout = computePanelLayout(client);
        Font tr = client.font;
        finalizeScrollLayout(layout, tr);

        if (button == 0 && openPickerKey != null) {
            if (handlePickerScrollbarClick(mouseX, mouseY)) {
                return true;
            }
            if (handleItemPickerDropdownClick(mouseX, mouseY)) {
                return true;
            }
        }

        if (button == 1) {
            String inputKey = findInputFieldAt(mouseX, mouseY);
            if (inputKey != null) {
                clearInputField(inputKey);
                focusedInputKey = inputKey;
                commitInputFieldChange(inputKey);
                return true;
            }
            return false;
        }

        if (button != 0) {
            return false;
        }

        String inputKey = findInputFieldAt(mouseX, mouseY);
        if (inputKey == null) {
            String operatorKey = findOperatorFieldAt(mouseX, mouseY);
            if (operatorKey != null) {
                cycleCostOperator(operatorKey);
                return true;
            }
        }
        if (inputKey != null) {
            focusedInputKey = inputKey;
            focusInputFieldAtMouse(tr, inputKey, mouseX);
            SearchFieldState.blurSearchFieldFocus();
            updateInputCursorBlink();
            return true;
        }
        focusedInputKey = null;

        if (mouseX >= layout.closeX && mouseX < layout.closeX + CLOSE_BUTTON_SIZE
                && mouseY >= layout.closeY && mouseY < layout.closeY + CLOSE_BUTTON_SIZE) {
            close();
            return true;
        }

        if (mouseX >= layout.clearX && mouseX < layout.clearX + layout.clearW
                && mouseY >= layout.clearY && mouseY < layout.clearY + 16) {
            selectedOptionKeys.clear();
            clearAllInputFields();
            focusedInputKey = null;
            openPickerKey = null;
            resetPickerScrollbarState();
            applySelections();
            return true;
        }

        if (mouseX < layout.boxX || mouseX >= layout.boxX + layout.boxWidth
                || mouseY < layout.boxY || mouseY >= layout.boxY + layout.boxHeight) {
            close();
            return true;
        }

        if (layout.scrollable && handleScrollbarClick(mouseX, mouseY, layout)) {
            return true;
        }

        if (mouseY >= layout.contentTop && mouseY < layout.contentBottom
                && mouseX >= layout.contentX && mouseX < layout.contentScrollRight) {
            for (FilterGroup group : groups) {
                for (FilterOption option : group.options) {
                    int hitY = option.y - scrollOffset;
                    if (option.pickerButtonX >= 0) {
                        if (mouseX >= option.pickerButtonX && mouseX < option.pickerButtonX + PICKER_BUTTON_SIZE
                                && mouseY >= hitY && mouseY < hitY + ROW_HEIGHT) {
                            toggleItemPicker(optionKey(group.title, option.label));
                            return true;
                        }
                    }
                    if (option.inputType != RowInputType.NONE) {
                        if (mouseX >= option.x && mouseX < option.countFieldX
                                && mouseY >= hitY && mouseY < hitY + ROW_HEIGHT) {
                            openPickerKey = null;
                            toggleOption(group, option);
                            return true;
                        }
                    } else if (mouseX >= option.x && mouseX < option.x + option.width
                            && mouseY >= hitY && mouseY < hitY + ROW_HEIGHT) {
                        openPickerKey = null;
                        toggleOption(group, option);
                        return true;
                    }
                }
            }
            if (openPickerKey != null) {
                openPickerKey = null;
            }
            return true;
        }

        return true;
    }

    public static boolean handleScroll(double mouseX, double mouseY, double vertical) {
        if (!open) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        PanelLayout layout = computePanelLayout(client);
        finalizeScrollLayout(layout, client.font);

        if (openPickerKey != null) {
            boolean overPanel = mouseX >= layout.boxX && mouseX < layout.boxX + layout.boxWidth
                    && mouseY >= layout.boxY && mouseY < layout.boxY + layout.boxHeight;
            if (overPanel && pickerDropdownEntries.size() > PICKER_MAX_VISIBLE) {
                pickerScrollOffset = clampPickerScrollOffset(pickerScrollOffset - (int) vertical);
                return true;
            }
        }

        boolean overPanel = mouseX >= layout.boxX && mouseX < layout.boxX + layout.boxWidth
                && mouseY >= layout.boxY && mouseY < layout.boxY + layout.boxHeight;
        boolean overScrollArea = mouseY >= layout.contentTop && mouseY < layout.contentBottom
                && mouseX >= layout.contentX && mouseX < layout.contentX + layout.contentWidth;
        if (!hovered && !overPanel) {
            return false;
        }
        if (!layout.scrollable || !overScrollArea) {
            return false;
        }
        focusedInputKey = null;
        openPickerKey = null;
        scrollOffset = clampScrollOffset(scrollOffset - (int) (vertical * 12));
        return true;
    }

    public static boolean handleMouseDrag(double mouseX, double mouseY, int button) {
        if (!open || button != 0) {
            return false;
        }
        if (pickerScrollbarDragging) {
            setPickerScrollFromThumbTop((int) mouseY - pickerScrollbarDragOffset);
            return true;
        }
        if (!scrollbarDragging) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        PanelLayout layout = computePanelLayout(client);
        finalizeScrollLayout(layout, client.font);
        if (!layout.scrollable) {
            return false;
        }
        setScrollFromThumbTop((int) mouseY - scrollbarDragOffset, layout);
        return true;
    }

    public static boolean handleMouseRelease(double mouseX, double mouseY, int button) {
        if (pickerScrollbarDragging && button == 0) {
            pickerScrollbarDragging = false;
            pickerScrollbarDragOffset = 0;
            return true;
        }
        if (!scrollbarDragging || button != 0) {
            return false;
        }
        scrollbarDragging = false;
        scrollbarDragOffset = 0;
        return true;
    }

    public static boolean isScrollbarDragging() {
        return scrollbarDragging || pickerScrollbarDragging;
    }

    public static boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!open || focusedInputKey == null) {
            return false;
        }

        updateInputCursorBlink();

        if (isModifierOnlyKey(keyCode)) {
            return true;
        }

        if (keyCode == InputConstants.KEY_ESCAPE) {
            focusedInputKey = null;
            return true;
        }

        InputFieldState state = getOrCreateState(focusedInputKey);
        String text = inputFieldText.getOrDefault(focusedInputKey, "");
        state.cursor = clampCursor(state.cursor, text.length());

        if (isCtrlDown(modifiers)) {
            if (keyCode == InputConstants.KEY_A) {
                if (!text.isEmpty()) {
                    state.selectionStart = 0;
                    state.selectionEnd = text.length();
                    state.cursor = text.length();
                }
                return true;
            }
            if (keyCode == InputConstants.KEY_C) {
                if (state.hasSelection()) {
                    int min = state.selectionMin();
                    int max = state.selectionMax();
                    Minecraft client = Minecraft.getInstance();
                    if (client != null) {
                        client.keyboardHandler.setClipboard(text.substring(min, max));
                    }
                }
                return true;
            }
            if (keyCode == InputConstants.KEY_V) {
                Minecraft client = Minecraft.getInstance();
                if (client != null) {
                    String clipboard = client.keyboardHandler.getClipboard();
                    if (clipboard != null && !clipboard.isEmpty()) {
                        insertAtCursor(clipboard);
                    }
                }
                return true;
            }
            if (keyCode == InputConstants.KEY_X) {
                if (state.hasSelection()) {
                    int min = state.selectionMin();
                    int max = state.selectionMax();
                    Minecraft client = Minecraft.getInstance();
                    if (client != null) {
                        client.keyboardHandler.setClipboard(text.substring(min, max));
                    }
                    text = deleteSelection(text, state);
                    inputFieldText.put(focusedInputKey, text);
                    commitInputFieldChange(focusedInputKey);
                }
                return true;
            }
        }

        if (keyCode == InputConstants.KEY_BACKSPACE) {
            text = deleteBeforeCursor(text, state);
            inputFieldText.put(focusedInputKey, text);
            commitInputFieldChange(focusedInputKey);
            return true;
        }
        if (keyCode == InputConstants.KEY_DELETE) {
            text = deleteAfterCursor(text, state);
            inputFieldText.put(focusedInputKey, text);
            commitInputFieldChange(focusedInputKey);
            return true;
        }
        if (keyCode == InputConstants.KEY_LEFT) {
            moveCursorHorizontal(text, state, -1, isShiftDown(modifiers));
            return true;
        }
        if (keyCode == InputConstants.KEY_RIGHT) {
            moveCursorHorizontal(text, state, 1, isShiftDown(modifiers));
            return true;
        }
        if (keyCode == InputConstants.KEY_HOME) {
            moveCursorTo(text, state, 0, isShiftDown(modifiers));
            return true;
        }
        if (keyCode == InputConstants.KEY_END) {
            moveCursorTo(text, state, text.length(), isShiftDown(modifiers));
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            focusedInputKey = null;
            return true;
        }

        RowInputType inputType = getInputTypeForKey(focusedInputKey);
        if (inputType == RowInputType.MODIFIER_DIGIT) {
            Character digit = resolveDigitKey(keyCode);
            if (digit != null) {
                insertAtCursor(String.valueOf(digit));
                return true;
            }
        } else if (inputType == RowInputType.COST_AMOUNT) {
            Character digit = resolveDigitKey(keyCode);
            if (digit != null) {
                insertAtCursor(String.valueOf(digit));
                return true;
            }
            if (isItemScoreAmountField(focusedInputKey)
                    && (keyCode == InputConstants.KEY_PERIOD || keyCode == InputConstants.KEY_COMMA
                    || keyCode == InputConstants.KEY_NUMPADCOMMA)) {
                insertAtCursor(".");
                return true;
            }
        } else if (inputType == RowInputType.COST_ITEM) {
            String typed = resolveItemTextKey(keyCode, modifiers);
            if (typed != null) {
                insertAtCursor(typed);
                return true;
            }
        }

        return true;
    }

    private static void cycleCostOperator(String operatorKey) {
        String current = inputFieldText.getOrDefault(operatorKey, "=");
        int index = COST_MENU_OPERATORS.indexOf(current);
        String next = COST_MENU_OPERATORS.get((index + 1) % COST_MENU_OPERATORS.size());
        setInputFieldText(operatorKey, next);
        int separator = operatorKey.indexOf('\0');
        if (separator >= 0) {
            String groupTitle = operatorKey.substring(0, separator);
            String label = operatorKey.substring(separator + 1);
            String anzahlGroup = "Att.-Operator".equals(groupTitle) ? "Att.-Anzahl" : "Anzahl";
            if (selectedOptionKeys.contains(optionKey(anzahlGroup, label))) {
                applySelections();
            }
        }
    }

    static String findOperatorFieldAt(double mouseX, double mouseY) {
        for (FilterGroup group : groups) {
            if (!"Operator".equals(group.title) && !"Att.-Operator".equals(group.title)) {
                continue;
            }
            for (FilterOption option : group.options) {
                if (option.inputType != RowInputType.COST_OPERATOR) {
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
}

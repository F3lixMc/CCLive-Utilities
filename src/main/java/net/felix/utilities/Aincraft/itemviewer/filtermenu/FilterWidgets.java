package net.felix.utilities.Aincraft.itemviewer.filtermenu;

import net.felix.utilities.Aincraft.itemviewer.ui.ViewerState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.CHECKBOX_SIZE;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.PICKER_BUTTON_SIZE;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.ROW_HEIGHT;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.focusedInputKey;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.inputCursorVisible;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.inputFieldText;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.openPickerKey;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterSelection.isOptionSelected;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterSelection.optionKey;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.InputFieldEditor.COST_MENU_OPERATORS;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.InputFieldEditor.clampCursor;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.InputFieldEditor.getOrCreateState;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.InputFieldEditor.updateInputCursorBlink;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.InputFieldView.computeVisibleSlice;

/**
 * Zeichen-Bausteine: Checkbox, Eingabefeld, Panel, Buttons.
 */
public final class FilterWidgets {

    private FilterWidgets() {
    }

    static void drawCheckbox(GuiGraphicsExtractor context, Font tr, FilterGroup group, FilterOption option, int rowY) {
        boolean selected = isOptionSelected(group, option);
        int boxX = option.x;
        int boxY = rowY + (ROW_HEIGHT - CHECKBOX_SIZE) / 2;
        int labelY = rowY + (ROW_HEIGHT - tr.lineHeight) / 2;

        context.fill(boxX, boxY, boxX + CHECKBOX_SIZE, boxY + CHECKBOX_SIZE, selected ? 0xFF2D6A2D : 0xFF202020);
        int border = selected ? 0xFF55FF55 : 0xFF808080;
        context.fill(boxX, boxY, boxX + CHECKBOX_SIZE, boxY + 1, border);
        context.fill(boxX, boxY + CHECKBOX_SIZE - 1, boxX + CHECKBOX_SIZE, boxY + CHECKBOX_SIZE, border);
        context.fill(boxX, boxY, boxX + 1, boxY + CHECKBOX_SIZE, border);
        context.fill(boxX + CHECKBOX_SIZE - 1, boxY, boxX + CHECKBOX_SIZE, boxY + CHECKBOX_SIZE, border);

        if (selected) {
            drawCheckmark(context, boxX, boxY);
        }

        int labelX = boxX + CHECKBOX_SIZE + 4;
        context.text(tr, option.label, labelX, labelY, 0xFFFFFFFF, false);

        if (option.inputType != RowInputType.NONE) {
            int colonX = labelX + tr.width(option.label);
            context.text(tr, ":", colonX, labelY, 0xFFFFFFFF, false);

            int fieldX = option.countFieldX;
            int underlineY = labelY + tr.lineHeight;
            String fieldKey = optionKey(group.title, option.label);
            boolean fieldFocused = fieldKey.equals(focusedInputKey);
            String fieldText = inputFieldText.getOrDefault(fieldKey, "");
            if (option.inputType == RowInputType.COST_OPERATOR) {
                drawOperatorField(context, tr, fieldX, labelY, underlineY, option.countFieldW, fieldText);
            } else {
                drawInputField(context, tr, fieldX, labelY, underlineY, option.countFieldW, fieldKey, fieldText,
                        option.inputType, fieldFocused);
            }

            if (option.pickerButtonX >= 0) {
                int pickerY = rowY + (ROW_HEIGHT - PICKER_BUTTON_SIZE) / 2;
                boolean pickerHovered = ViewerState.getLastMouseX() >= option.pickerButtonX
                        && ViewerState.getLastMouseX() < option.pickerButtonX + PICKER_BUTTON_SIZE
                        && ViewerState.getLastMouseY() >= pickerY
                        && ViewerState.getLastMouseY() < pickerY + PICKER_BUTTON_SIZE;
                boolean pickerOpen = fieldKey.equals(openPickerKey);
                drawSmallButton(context, option.pickerButtonX, pickerY, PICKER_BUTTON_SIZE, pickerHovered || pickerOpen);
                int arrowX = option.pickerButtonX + (PICKER_BUTTON_SIZE - tr.width("v")) / 2;
                int arrowY = pickerY + (PICKER_BUTTON_SIZE - tr.lineHeight) / 2;
                context.text(tr, "\u25BC", arrowX, arrowY, 0xFFFFFFFF, false);
            }
        }
    }

    private static void drawOperatorField(GuiGraphicsExtractor context, Font tr, int fieldX, int labelY, int underlineY,
            int fieldW, String operatorText) {
        String display = COST_MENU_OPERATORS.contains(operatorText) ? operatorText : "=";
        context.fill(fieldX, underlineY, fieldX + fieldW, underlineY + 1, 0xFFAAAAAA);
        int textX = fieldX + (fieldW - tr.width(display)) / 2;
        context.text(tr, display, textX, labelY, 0xFFFFFFFF, false);
    }

    private static void drawInputField(GuiGraphicsExtractor context, Font tr, int fieldX, int labelY, int underlineY,
            int fieldW, String fieldKey, String fieldText, RowInputType inputType, boolean fieldFocused) {
        InputFieldState state = getOrCreateState(fieldKey);
        state.cursor = clampCursor(state.cursor, fieldText.length());

        int underlineColor = fieldFocused ? 0xFFFFFFFF : 0xFFAAAAAA;
        context.fill(fieldX, underlineY, fieldX + fieldW, underlineY + 1, underlineColor);

        if (!fieldFocused && fieldText.isEmpty()) {
            return;
        }

        VisibleFieldSlice slice = computeVisibleSlice(tr, fieldText, fieldW, state, fieldFocused, inputType);
        int textX = fieldX;
        if (slice.centered()) {
            textX = fieldX + (fieldW - tr.width(slice.display())) / 2;
        }

        if (fieldFocused && slice.displaySelStart() >= 0 && slice.displaySelEnd() > slice.displaySelStart()) {
            int selStartX = textX + tr.width(slice.display().substring(0, slice.displaySelStart()));
            int selEndX = textX + tr.width(slice.display().substring(0, slice.displaySelEnd()));
            context.fill(selStartX, labelY, selEndX, labelY + tr.lineHeight, 0xFF2A5080);
        }

        if (!slice.display().isEmpty()) {
            context.text(tr, slice.display(), textX, labelY, 0xFFFFFFFF, false);
        }

        if (fieldFocused) {
            updateInputCursorBlink();
            if (inputCursorVisible) {
                int caretIndex = fieldText.isEmpty() ? 0 : Math.max(0, slice.displayCursor());
                String beforeCaret = slice.display().isEmpty() ? "" : slice.display().substring(0, caretIndex);
                int caretX = textX + tr.width(beforeCaret);
                context.fill(caretX, labelY, caretX + 1, labelY + tr.lineHeight, 0xFFFFFFFF);
            }
        }
    }

    /** Kleines Häkchen per Pixel – zuverlässiger als Unicode-Zeichen in der MC-Schrift. */
    private static void drawCheckmark(GuiGraphicsExtractor context, int boxX, int boxY) {
        int color = 0xFFFFFFFF;
        context.fill(boxX + 2, boxY + 5, boxX + 3, boxY + 6, color);
        context.fill(boxX + 3, boxY + 6, boxX + 4, boxY + 7, color);
        context.fill(boxX + 4, boxY + 5, boxX + 5, boxY + 6, color);
        context.fill(boxX + 5, boxY + 4, boxX + 6, boxY + 5, color);
        context.fill(boxX + 6, boxY + 3, boxX + 7, boxY + 4, color);
        context.fill(boxX + 7, boxY + 2, boxX + 8, boxY + 3, color);
    }

    static void drawCenteredButtonLabel(GuiGraphicsExtractor context, Font tr, String label, int x, int y, int width, int height) {
        int textWidth = tr.width(label);
        int textHeight = tr.lineHeight;
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - textHeight) / 2;
        context.text(tr, label, textX, textY, 0xFFFFFFFF, false);
    }

    static void drawPanel(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0xFF000000);
        context.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
        context.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        context.fill(x, y, x + 1, y + height, 0xFFFFFFFF);
        context.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
    }

    static void drawSmallButton(GuiGraphicsExtractor context, int x, int y, int width, int height, boolean hovered) {
        context.fill(x, y, x + width, y + height, hovered ? 0xFF404040 : 0x80000000);
        context.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
        context.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        context.fill(x, y, x + 1, y + height, 0xFFFFFFFF);
        context.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
    }

    static void drawSmallButton(GuiGraphicsExtractor context, int x, int y, int size, boolean hovered) {
        drawSmallButton(context, x, y, size, size, hovered);
    }
}

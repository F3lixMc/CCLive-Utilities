package net.felix.utilities.Aincraft.itemviewer.filtermenu;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterCatalog.FILTER_ROW_TITLES;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.CHECKBOX_SIZE;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.CLOSE_BUTTON_MARGIN;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.CLOSE_BUTTON_SIZE;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.COLUMN_GAP;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.CONTENT_PADDING;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.HEADER_GAP;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.PICKER_BUTTON_GAP;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.PICKER_BUTTON_SIZE;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.ROW_GRID_GAP;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuDimensions.ROW_HEIGHT;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.contentHeight;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterMenuState.groups;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterSelection.displayGroupTitle;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterSelection.findGroupByTitle;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.FilterSelection.getRowGroups;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.InputFieldEditor.COST_AMOUNT_FIELD_CHARS;
import static net.felix.utilities.Aincraft.itemviewer.filtermenu.InputFieldEditor.COST_ITEM_FIELD_CHARS;

/**
 * Berechnet Panel-, Spalten- und Zeilen-Layout.
 */
public final class FilterMenuLayout {

    private FilterMenuLayout() {
    }

    public static PanelLayout computePanelLayout(Minecraft client) {
        Font tr = client.font;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int maxRowWidth = 0;
        for (List<String> rowTitles : FILTER_ROW_TITLES) {
            int rowWidth = 0;
            for (int i = 0; i < rowTitles.size(); i++) {
                FilterGroup group = findGroupByTitle(rowTitles.get(i));
                if (group != null) {
                    rowWidth += computeColumnWidth(tr, group);
                    if (i < rowTitles.size() - 1) {
                        rowWidth += COLUMN_GAP;
                    }
                }
            }
            maxRowWidth = Math.max(maxRowWidth, rowWidth);
        }

        int desiredWidth = CONTENT_PADDING * 2 + maxRowWidth;
        int boxWidth = Math.min(Math.max(480, desiredWidth), screenWidth - 20);
        int boxHeight = Math.min(400, screenHeight - 40);
        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = (screenHeight - boxHeight) / 2;

        int clearW = tr.width("Alle löschen") + 12;
        PanelLayout layout = new PanelLayout();
        layout.screenWidth = screenWidth;
        layout.screenHeight = screenHeight;
        layout.boxX = boxX;
        layout.boxY = boxY;
        layout.boxWidth = boxWidth;
        layout.boxHeight = boxHeight;
        layout.closeX = boxX + boxWidth - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_MARGIN;
        layout.closeY = boxY + 5;
        layout.contentX = boxX + CONTENT_PADDING;
        layout.contentTop = boxY + 32;
        layout.contentBottom = boxY + boxHeight - 36;
        layout.contentWidth = boxWidth - CONTENT_PADDING * 2;
        layout.clearX = boxX + (boxWidth - clearW) / 2;
        layout.clearY = boxY + boxHeight - 24;
        layout.clearW = clearW;
        return layout;
    }

    private static int computeColumnWidth(Font tr, FilterGroup group) {
        int width = tr.width(displayGroupTitle(group.title));
        for (FilterOption option : group.options) {
            if (option.inputType != RowInputType.NONE) {
                int fieldWidth = inputFieldWidth(tr, option.inputType);
                int pickerExtra = hasItemPicker(option.inputType, group.title)
                        ? PICKER_BUTTON_SIZE + PICKER_BUTTON_GAP : 0;
                int rowWidth = CHECKBOX_SIZE + 4 + tr.width(option.label) + tr.width(":")
                        + 2 + fieldWidth + pickerExtra;
                width = Math.max(width, rowWidth);
            } else {
                width = Math.max(width, CHECKBOX_SIZE + 4 + tr.width(option.label));
            }
        }
        return width + 4;
    }

    private static boolean hasItemPicker(RowInputType inputType, String groupTitle) {
        return inputType == RowInputType.COST_ITEM && "Item".equals(groupTitle);
    }

    private static int inputFieldWidth(Font tr, RowInputType inputType) {
        return switch (inputType) {
            case MODIFIER_DIGIT -> tr.width("0");
            case COST_OPERATOR -> tr.width(">=");
            case COST_AMOUNT -> tr.width("0".repeat(COST_AMOUNT_FIELD_CHARS));
            case COST_ITEM -> tr.width("0".repeat(COST_ITEM_FIELD_CHARS));
            default -> 0;
        };
    }

    static void layoutGroups(Font tr, int contentX, int contentTop, int contentWidth) {
        if (groups.isEmpty()) {
            contentHeight = 0;
            return;
        }

        int rowY = contentTop;
        int contentBottomY = contentTop;

        for (int row = 0; row < FILTER_ROW_TITLES.size(); row++) {
            List<FilterGroup> rowGroups = getRowGroups(row);
            if (rowGroups.isEmpty()) {
                continue;
            }

            int headerY = rowY;
            int optionsStartY = rowY + tr.lineHeight + HEADER_GAP;
            int rowMaxBottom = optionsStartY;
            int columnX = contentX;

            for (FilterGroup group : rowGroups) {
                int colWidth = computeColumnWidth(tr, group);
                group.columnX = columnX;
                group.columnWidth = colWidth;
                group.headerY = headerY;

                int optionY = optionsStartY;
                for (FilterOption option : group.options) {
                    option.x = columnX;
                    option.y = optionY;
                    if (option.inputType != RowInputType.NONE) {
                        int labelX = columnX + CHECKBOX_SIZE + 4;
                        int colonX = labelX + tr.width(option.label);
                        int fieldWidth = inputFieldWidth(tr, option.inputType);
                        option.countFieldX = colonX + tr.width(":") + 2;
                        option.countFieldW = fieldWidth;
                        option.pickerButtonX = hasItemPicker(option.inputType, group.title)
                                ? option.countFieldX + fieldWidth + PICKER_BUTTON_GAP : -1;
                        option.width = colWidth;
                    } else {
                        option.width = colWidth;
                        option.pickerButtonX = -1;
                    }
                    optionY += ROW_HEIGHT;
                }
                group.bottomY = optionY;
                rowMaxBottom = Math.max(rowMaxBottom, optionY);
                columnX += colWidth + COLUMN_GAP;
            }

            contentBottomY = rowMaxBottom;
            rowY = rowMaxBottom + ROW_GRID_GAP;
        }

        contentHeight = contentBottomY - contentTop;
    }
}

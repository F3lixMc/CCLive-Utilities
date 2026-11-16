package net.felix.features.itemviewer.filtermenu;

import java.util.List;
import net.felix.features.itemviewer.search.CostNamePickerEntry;
import net.felix.features.itemviewer.search.SearchFieldState;
import net.felix.features.itemviewer.search.SearchSuggestions;
import net.felix.features.itemviewer.ui.ViewerState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static net.felix.features.itemviewer.filtermenu.FilterMenuDimensions.PICKER_FLOOR_LABEL_COLOR;
import static net.felix.features.itemviewer.filtermenu.FilterMenuDimensions.PICKER_FLOOR_LABEL_GAP;
import static net.felix.features.itemviewer.filtermenu.FilterMenuDimensions.PICKER_MAX_VISIBLE;
import static net.felix.features.itemviewer.filtermenu.FilterMenuDimensions.PICKER_OPTION_HEIGHT;
import static net.felix.features.itemviewer.filtermenu.FilterMenuDimensions.PICKER_PADDING;
import static net.felix.features.itemviewer.filtermenu.FilterMenuDimensions.PICKER_SCROLLBAR_GAP;
import static net.felix.features.itemviewer.filtermenu.FilterMenuDimensions.PICKER_SCROLLBAR_WIDTH;
import static net.felix.features.itemviewer.filtermenu.FilterMenuDimensions.ROW_HEIGHT;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.focusedInputKey;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.groups;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.openPickerKey;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerDropdownEntries;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerDropdownH;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerDropdownW;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerDropdownX;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerDropdownY;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerScrollOffset;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerScrollbarDragOffset;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerScrollbarDragging;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerScrollbarThumbHeight;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerScrollbarThumbTop;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerScrollbarTrackBottom;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerScrollbarTrackTop;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerScrollbarTrackX;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.pickerScrollbarVisible;
import static net.felix.features.itemviewer.filtermenu.FilterMenuState.scrollOffset;
import static net.felix.features.itemviewer.filtermenu.FilterSelection.commitInputFieldChange;
import static net.felix.features.itemviewer.filtermenu.FilterSelection.optionKey;
import static net.felix.features.itemviewer.filtermenu.InputFieldEditor.setInputFieldText;

/**
 * Dropdown zur Auswahl von Item-Namen (Rendern, Scrollbar, Klick).
 */
public final class ItemPicker {

    private ItemPicker() {
    }

    static String pickerListDisplayName(String label) {
        if ("Ressource".equals(label)) {
            return "Ressourcen";
        }
        return label;
    }

    static void resetPickerScrollbarState() {
        pickerScrollOffset = 0;
        pickerScrollbarDragging = false;
        pickerScrollbarDragOffset = 0;
        pickerScrollbarVisible = false;
    }

    static void toggleItemPicker(String fieldKey) {
        if (fieldKey.equals(openPickerKey)) {
            openPickerKey = null;
            resetPickerScrollbarState();
            return;
        }
        openPickerKey = fieldKey;
        resetPickerScrollbarState();
        focusedInputKey = fieldKey;
        SearchFieldState.blurSearchFieldFocus();
    }

    private static List<CostNamePickerEntry> getPickerEntries(String fieldKey) {
        int separator = fieldKey.indexOf('\0');
        if (separator < 0) {
            return List.of();
        }
        String label = fieldKey.substring(separator + 1);
        return switch (label) {
            case "Material" -> SearchSuggestions.getKnownMaterialPickerEntries();
            case "Ressource" -> SearchSuggestions.getKnownResourcePickerEntries();
            case "Amboss" -> SearchSuggestions.getKnownAmbossPickerEntries();
            case "Ofen" -> SearchSuggestions.getKnownOfenPickerEntries();
            default -> List.of();
        };
    }

    private static FilterOption findOptionForKey(String fieldKey) {
        for (FilterGroup group : groups) {
            for (FilterOption option : group.options) {
                if (fieldKey.equals(optionKey(group.title, option.label))) {
                    return option;
                }
            }
        }
        return null;
    }

    static int clampPickerScrollOffset(int offset) {
        int maxScroll = Math.max(0, pickerDropdownEntries.size() - PICKER_MAX_VISIBLE);
        return Math.max(0, Math.min(offset, maxScroll));
    }

    private static boolean isMouseOverItemPickerDropdown(double mouseX, double mouseY) {
        return mouseX >= pickerDropdownX && mouseX < pickerDropdownX + pickerDropdownW
                && mouseY >= pickerDropdownY && mouseY < pickerDropdownY + pickerDropdownH;
    }

    static void renderOpenItemPicker(GuiGraphicsExtractor context, Font tr, PanelLayout layout) {
        pickerScrollbarVisible = false;
        if (openPickerKey == null) {
            pickerDropdownEntries = List.of();
            return;
        }
        FilterOption option = findOptionForKey(openPickerKey);
        if (option == null || option.pickerButtonX < 0) {
            return;
        }

        List<CostNamePickerEntry> entries = getPickerEntries(openPickerKey);
        pickerDropdownEntries = entries;
        if (entries.isEmpty()) {
            int rowY = option.y - scrollOffset;
            int listX = option.pickerButtonX;
            int listY = rowY + ROW_HEIGHT;
            int listW = Math.max(80, tr.width("Keine Einträge") + PICKER_PADDING * 2);
            int listH = PICKER_OPTION_HEIGHT;
            pickerDropdownX = listX;
            pickerDropdownY = listY;
            pickerDropdownW = listW;
            pickerDropdownH = listH;
            context.fill(listX, listY, listX + listW, listY + listH, 0xFF202020);
            context.fill(listX, listY, listX + listW, listY + 1, 0xFF808080);
            context.fill(listX, listY + listH - 1, listX + listW, listY + listH, 0xFF808080);
            context.fill(listX, listY, listX + 1, listY + listH, 0xFF808080);
            context.fill(listX + listW - 1, listY, listX + listW, listY + listH, 0xFF808080);
            context.text(tr, "Keine Einträge", listX + PICKER_PADDING, listY + 3, 0xFFAAAAAA, false);
            return;
        }

        pickerScrollOffset = clampPickerScrollOffset(pickerScrollOffset);
        boolean scrollable = entries.size() > PICKER_MAX_VISIBLE;
        int maxRowWidth = 0;
        for (CostNamePickerEntry entry : entries) {
            int rowWidth = tr.width(entry.name());
            String floorLabel = entry.floorLabel();
            if (!floorLabel.isEmpty()) {
                rowWidth += PICKER_FLOOR_LABEL_GAP + tr.width(floorLabel);
            }
            maxRowWidth = Math.max(maxRowWidth, rowWidth);
        }
        int listW = maxRowWidth + PICKER_PADDING * 2 + (scrollable ? PICKER_SCROLLBAR_WIDTH + PICKER_SCROLLBAR_GAP : 0);
        int visibleCount = Math.min(PICKER_MAX_VISIBLE, entries.size());
        int listH = visibleCount * PICKER_OPTION_HEIGHT;

        int rowY = option.y - scrollOffset;
        int listX = option.pickerButtonX;
        int listY = rowY + ROW_HEIGHT;
        if (listY + listH > layout.boxY + layout.boxHeight - 8) {
            listY = rowY - listH;
        }
        if (listX + listW > layout.boxX + layout.boxWidth - 8) {
            listX = Math.max(layout.boxX + 8, layout.boxX + layout.boxWidth - 8 - listW);
        }

        pickerDropdownX = listX;
        pickerDropdownY = listY;
        pickerDropdownW = listW;
        pickerDropdownH = listH;

        context.fill(listX, listY, listX + listW, listY + listH, 0xFF202020);
        context.fill(listX, listY, listX + listW, listY + 1, 0xFF808080);
        context.fill(listX, listY + listH - 1, listX + listW, listY + listH, 0xFF808080);
        context.fill(listX, listY, listX + 1, listY + listH, 0xFF808080);
        context.fill(listX + listW - 1, listY, listX + listW, listY + listH, 0xFF808080);

        int contentRight = listX + listW - (scrollable ? PICKER_SCROLLBAR_WIDTH + PICKER_SCROLLBAR_GAP : 0);
        context.enableScissor(listX, listY, contentRight, listY + listH);
        for (int i = 0; i < visibleCount; i++) {
            int entryIndex = pickerScrollOffset + i;
            if (entryIndex >= entries.size()) {
                break;
            }
            CostNamePickerEntry entry = entries.get(entryIndex);
            int optionY = listY + i * PICKER_OPTION_HEIGHT;
            boolean hovered = ViewerState.getLastMouseX() >= listX
                    && ViewerState.getLastMouseX() < contentRight
                    && ViewerState.getLastMouseY() >= optionY
                    && ViewerState.getLastMouseY() < optionY + PICKER_OPTION_HEIGHT;
            if (hovered) {
                context.fill(listX + 1, optionY, contentRight - 1, optionY + PICKER_OPTION_HEIGHT, 0xFF335588);
            }
            context.text(tr, entry.name(), listX + PICKER_PADDING, optionY + 3, entry.textColor(), false);
            String floorLabel = entry.floorLabel();
            if (!floorLabel.isEmpty()) {
                int floorWidth = tr.width(floorLabel);
                context.text(tr, floorLabel, contentRight - PICKER_PADDING - floorWidth, optionY + 3,
                        PICKER_FLOOR_LABEL_COLOR, false);
            }
        }
        context.disableScissor();

        if (scrollable) {
            int trackX = listX + listW - PICKER_SCROLLBAR_WIDTH - 1;
            int trackTop = listY + 1;
            int trackBottom = listY + listH - 1;
            int maxScroll = entries.size() - PICKER_MAX_VISIBLE;
            int thumbHeight = computePickerThumbHeight(listH, entries.size());
            int thumbTravel = listH - 2 - thumbHeight;
            int thumbTop = trackTop + (maxScroll == 0 ? 0 : thumbTravel * pickerScrollOffset / maxScroll);

            pickerScrollbarVisible = true;
            pickerScrollbarTrackX = trackX;
            pickerScrollbarTrackTop = trackTop;
            pickerScrollbarTrackBottom = trackBottom;
            pickerScrollbarThumbTop = thumbTop;
            pickerScrollbarThumbHeight = thumbHeight;

            context.fill(trackX, trackTop, trackX + PICKER_SCROLLBAR_WIDTH, trackBottom, 0xFF2A2A2A);
            context.fill(trackX, trackTop, trackX + PICKER_SCROLLBAR_WIDTH, trackTop + 1, 0xFF606060);
            context.fill(trackX, trackBottom - 1, trackX + PICKER_SCROLLBAR_WIDTH, trackBottom, 0xFF606060);
            boolean thumbHovered = !pickerScrollbarDragging
                    && ViewerState.getLastMouseX() >= trackX
                    && ViewerState.getLastMouseX() < trackX + PICKER_SCROLLBAR_WIDTH
                    && ViewerState.getLastMouseY() >= thumbTop
                    && ViewerState.getLastMouseY() < thumbTop + thumbHeight;
            int thumbColor = pickerScrollbarDragging ? 0xFFB0B0B0 : (thumbHovered ? 0xFFA0A0A0 : 0xFF909090);
            context.fill(trackX + 1, thumbTop, trackX + PICKER_SCROLLBAR_WIDTH - 1, thumbTop + thumbHeight, thumbColor);
        }
    }

    private static int computePickerThumbHeight(int listH, int entryCount) {
        return Math.max(8, (listH - 2) * PICKER_MAX_VISIBLE / entryCount);
    }

    static void setPickerScrollFromThumbTop(int thumbTop) {
        if (!pickerScrollbarVisible) {
            return;
        }
        int trackHeight = pickerScrollbarTrackBottom - pickerScrollbarTrackTop;
        int thumbHeight = pickerScrollbarThumbHeight;
        int thumbTravel = trackHeight - thumbHeight;
        int maxScroll = Math.max(0, pickerDropdownEntries.size() - PICKER_MAX_VISIBLE);
        if (thumbTravel <= 0 || maxScroll <= 0) {
            pickerScrollOffset = 0;
            return;
        }
        int target = thumbTop - pickerScrollbarTrackTop;
        target = Math.max(0, Math.min(target, thumbTravel));
        pickerScrollOffset = (int) ((long) target * maxScroll / thumbTravel);
        pickerScrollOffset = clampPickerScrollOffset(pickerScrollOffset);
    }

    static boolean handlePickerScrollbarClick(double mouseX, double mouseY) {
        if (!pickerScrollbarVisible || openPickerKey == null) {
            return false;
        }
        if (mouseX < pickerScrollbarTrackX || mouseX >= pickerScrollbarTrackX + PICKER_SCROLLBAR_WIDTH
                || mouseY < pickerScrollbarTrackTop || mouseY >= pickerScrollbarTrackBottom) {
            return false;
        }
        int maxScroll = Math.max(0, pickerDropdownEntries.size() - PICKER_MAX_VISIBLE);
        if (maxScroll <= 0) {
            return true;
        }

        int thumbTop = pickerScrollbarThumbTop;
        int thumbHeight = pickerScrollbarThumbHeight;
        if (mouseY >= thumbTop && mouseY < thumbTop + thumbHeight) {
            pickerScrollbarDragging = true;
            pickerScrollbarDragOffset = (int) mouseY - thumbTop;
            return true;
        }

        setPickerScrollFromThumbTop((int) mouseY - thumbHeight / 2);
        pickerScrollbarDragging = true;
        pickerScrollbarDragOffset = (int) mouseY - pickerScrollbarThumbTop;
        return true;
    }

    static boolean handleItemPickerDropdownClick(double mouseX, double mouseY) {
        if (!isMouseOverItemPickerDropdown(mouseX, mouseY)) {
            return false;
        }
        if (pickerScrollbarVisible
                && mouseX >= pickerScrollbarTrackX
                && mouseX < pickerScrollbarTrackX + PICKER_SCROLLBAR_WIDTH) {
            return true;
        }
        if (pickerDropdownEntries.isEmpty()) {
            return true;
        }
        int relativeY = (int) mouseY - pickerDropdownY;
        int clickedIndex = pickerScrollOffset + relativeY / PICKER_OPTION_HEIGHT;
        if (clickedIndex < 0 || clickedIndex >= pickerDropdownEntries.size()) {
            return true;
        }
        String selected = pickerDropdownEntries.get(clickedIndex).name();
        String fieldKey = openPickerKey;
        setInputFieldText(fieldKey, selected);
        commitInputFieldChange(fieldKey);
        focusedInputKey = fieldKey;
        openPickerKey = null;
        resetPickerScrollbarState();
        return true;
    }
}

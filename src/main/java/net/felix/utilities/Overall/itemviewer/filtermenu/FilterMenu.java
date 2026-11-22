package net.felix.utilities.Overall.itemviewer.filtermenu;

import java.util.List;
import net.felix.utilities.Overall.itemviewer.data.ItemData;
import net.minecraft.client.Minecraft;

import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterCatalog.rebuildGroups;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.focusedInputKey;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.openPickerKey;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.scrollOffset;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.scrollbarDragOffset;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.scrollbarDragging;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterSelection.applySelections;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterSelection.ensureInputFields;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterSelection.persistAllInputDrafts;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterSelection.restoreInputDrafts;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterSelection.restoreSelectionDrafts;
import static net.felix.utilities.Overall.itemviewer.filtermenu.ItemPicker.resetPickerScrollbarState;
import static net.felix.utilities.Overall.itemviewer.filtermenu.SearchQuerySync.syncSelectionsFromSearch;

/**
 * Einstiegspunkt des Filter-Menues: oeffnen, schliessen, Escape.
 */
public final class FilterMenu {

    private FilterMenu() {
    }

    static boolean open = false;

    public static boolean isOpen() {
        return open;
    }

    public static boolean contains(double mouseX, double mouseY) {
        if (!open) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        PanelLayout layout = FilterMenuLayout.computePanelLayout(client);
        return mouseX >= layout.boxX && mouseX < layout.boxX + layout.boxWidth
                && mouseY >= layout.boxY && mouseY < layout.boxY + layout.boxHeight;
    }

    public static void open(List<ItemData> items, String currentSearch) {
        rebuildGroups(items);
        ensureInputFields();
        syncSelectionsFromSearch(currentSearch);
        restoreInputDrafts();
        restoreSelectionDrafts();
        open = true;
        scrollOffset = 0;
        focusedInputKey = null;
        openPickerKey = null;
        resetPickerScrollbarState();
    }

    public static void close() {
        if (open) {
            persistAllInputDrafts();
            applySelections();
        }
        open = false;
        scrollOffset = 0;
        scrollbarDragging = false;
        scrollbarDragOffset = 0;
        focusedInputKey = null;
        openPickerKey = null;
        resetPickerScrollbarState();
    }

    public static void toggle(List<ItemData> items, String currentSearch) {
        if (open) {
            close();
        } else {
            open(items, currentSearch);
        }
    }

    public static boolean handleEscape() {
        if (!open) {
            return false;
        }
        if (focusedInputKey != null) {
            focusedInputKey = null;
            return true;
        }
        if (openPickerKey != null) {
            openPickerKey = null;
            resetPickerScrollbarState();
            return true;
        }
        close();
        return true;
    }
}

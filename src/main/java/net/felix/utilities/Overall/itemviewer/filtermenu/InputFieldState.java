package net.felix.utilities.Overall.itemviewer.filtermenu;


public class InputFieldState {
    int cursor;
    int selectionStart = -1;
    int selectionEnd = -1;

    void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }

    boolean hasSelection() {
        return selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd;
    }

    int selectionMin() {
        return Math.min(selectionStart, selectionEnd);
    }

    int selectionMax() {
        return Math.max(selectionStart, selectionEnd);
    }
}

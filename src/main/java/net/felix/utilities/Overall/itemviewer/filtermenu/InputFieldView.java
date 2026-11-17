package net.felix.utilities.Overall.itemviewer.filtermenu;

import net.minecraft.client.gui.Font;

import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.groups;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterSelection.optionKey;
import static net.felix.utilities.Overall.itemviewer.filtermenu.InputFieldEditor.clampCursor;

/**
 * Sichtbarer Ausschnitt eines Eingabefelds (Fenster, Cursor-Mapping, Abschneiden).
 */
public final class InputFieldView {

    private InputFieldView() {
    }

    static VisibleFieldSlice computeVisibleSlice(Font tr, String text, int maxWidth,
            InputFieldState state, boolean focused, RowInputType inputType) {
        int cursor = clampCursor(state.cursor, text.length());
        if (text.isEmpty()) {
            return new VisibleFieldSlice("", 0, -1, -1, false);
        }

        if (!focused) {
            String display = truncateToFieldWidth(tr, text, maxWidth, false);
            boolean centered = (inputType == RowInputType.MODIFIER_DIGIT || inputType == RowInputType.COST_AMOUNT)
                    && display.equals(text);
            return new VisibleFieldSlice(display, -1, -1, -1, centered);
        }

        int selMin = state.hasSelection() ? state.selectionMin() : -1;
        int selMax = state.hasSelection() ? state.selectionMax() : -1;

        if (tr.width(text) <= maxWidth) {
            boolean centered = inputType == RowInputType.MODIFIER_DIGIT || inputType == RowInputType.COST_AMOUNT;
            return new VisibleFieldSlice(text, cursor, selMin, selMax, centered);
        }

        VisibleWindow window = findVisibleWindow(tr, text, maxWidth, cursor, selMin, selMax);
        int displayCursor = window.prefixLength() + (cursor - window.start());
        displayCursor = Math.max(0, Math.min(displayCursor, window.display().length()));

        int displaySelStart = mapSelectionIndexToDisplay(window.start(), window.end(), window.prefixLength(), selMin);
        int displaySelEnd = mapSelectionIndexToDisplay(window.start(), window.end(), window.prefixLength(), selMax);

        return new VisibleFieldSlice(window.display(), displayCursor, displaySelStart, displaySelEnd, false);
    }

    private static VisibleWindow findVisibleWindow(Font tr, String text, int maxWidth, int cursor,
            int selMin, int selMax) {
        String ellipsis = "...";
        int textLength = text.length();
        boolean hasSelection = selMin >= 0 && selMax > selMin;

        VisibleWindow best = null;
        for (int start = 0; start < textLength; start++) {
            for (int end = start + 1; end <= textLength; end++) {
                if (cursor < start || cursor > end) {
                    continue;
                }
                String left = start > 0 ? ellipsis : "";
                String right = end < textLength ? ellipsis : "";
                String display = left + text.substring(start, end) + right;
                if (tr.width(display) > maxWidth) {
                    continue;
                }

                VisibleWindow candidate = new VisibleWindow(start, end, display, left.length());
                if (best == null || preferVisibleWindow(text, cursor, selMin, selMax, hasSelection, best, candidate)) {
                    best = candidate;
                }
            }
        }

        if (best != null) {
            return best;
        }

        boolean showEnd = cursor > textLength / 2;
        String display = truncateToFieldWidth(tr, text, maxWidth, showEnd);
        if (showEnd) {
            int start = 0;
            for (int candidateStart = 0; candidateStart < textLength; candidateStart++) {
                if (tr.width(ellipsis + text.substring(candidateStart)) <= maxWidth) {
                    start = candidateStart;
                    break;
                }
            }
            return new VisibleWindow(start, textLength, display, start > 0 ? ellipsis.length() : 0);
        }
        int end = textLength;
        while (end > 0 && tr.width(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return new VisibleWindow(0, end, display, 0);
    }

    private static boolean preferVisibleWindow(String text, int cursor, int selMin, int selMax, boolean hasSelection,
            VisibleWindow current, VisibleWindow candidate) {
        if (hasSelection) {
            int currentOverlap = selectionOverlap(current, selMin, selMax);
            int candidateOverlap = selectionOverlap(candidate, selMin, selMax);
            if (candidateOverlap != currentOverlap) {
                return candidateOverlap > currentOverlap;
            }
        }
        int currentVisible = current.end() - current.start();
        int candidateVisible = candidate.end() - candidate.start();
        if (candidateVisible != currentVisible) {
            return candidateVisible > currentVisible;
        }
        if (cursor >= text.length() - 1 || cursor == text.length()) {
            return candidate.start() < current.start();
        }
        if (cursor <= 0) {
            return candidate.start() < current.start();
        }
        int currentCenter = (current.start() + current.end()) / 2;
        int candidateCenter = (candidate.start() + candidate.end()) / 2;
        return Math.abs(candidateCenter - cursor) < Math.abs(currentCenter - cursor);
    }

    private static int selectionOverlap(VisibleWindow window, int selMin, int selMax) {
        return Math.max(0, Math.min(window.end(), selMax) - Math.max(window.start(), selMin));
    }

    private static int mapSelectionIndexToDisplay(int textStart, int textEnd, int prefixLen, int index) {
        if (index < 0) {
            return -1;
        }
        int contentStart = prefixLen;
        int contentEnd = prefixLen + (textEnd - textStart);
        if (index <= textStart) {
            return contentStart;
        }
        if (index >= textEnd) {
            return contentEnd;
        }
        return prefixLen + (index - textStart);
    }

    private static String truncateToFieldWidth(Font tr, String text, int maxWidth, boolean showEnd) {
        if (text.isEmpty() || tr.width(text) <= maxWidth) {
            return text;
        }
        if (showEnd) {
            String ellipsis = "...";
            for (int start = 0; start < text.length(); start++) {
                String candidate = ellipsis + text.substring(start);
                if (tr.width(candidate) <= maxWidth) {
                    return candidate;
                }
            }
            for (int start = 0; start < text.length(); start++) {
                String candidate = text.substring(start);
                if (tr.width(candidate) <= maxWidth) {
                    return candidate;
                }
            }
            return text.substring(text.length() - 1);
        }
        String display = text;
        while (!display.isEmpty() && tr.width(display + "...") > maxWidth) {
            display = display.substring(0, display.length() - 1);
        }
        return display + "...";
    }

    static FieldLayout findFieldLayout(String inputKey) {
        for (FilterGroup group : groups) {
            for (FilterOption option : group.options) {
                if (inputKey.equals(optionKey(group.title, option.label))) {
                    return new FieldLayout(option.countFieldX, option.countFieldW, option.inputType);
                }
            }
        }
        return null;
    }
}

package net.felix.features.itemviewer.filtermenu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.felix.features.itemviewer.search.CostNamePickerEntry;

/**
 * Gemeinsamer Zustand des Filter-Menues (Scroll, Auswahl, Eingabefelder, Picker).
 */
public final class FilterMenuState {

    private FilterMenuState() {
    }

    public static int scrollOffset = 0;

    public static boolean hovered = false;

    static boolean scrollbarDragging = false;

    static int scrollbarDragOffset = 0;

    public static int contentHeight = 0;

    static int viewportHeight = 0;

    static final Set<String> selectedOptionKeys = new LinkedHashSet<>();

    static final Set<String> draftSelectedOptionKeys = new LinkedHashSet<>();

    static final Map<String, String> inputFieldText = new LinkedHashMap<>();

    static final Map<String, String> draftInputFieldText = new LinkedHashMap<>();

    static final Map<String, InputFieldState> inputFieldStates = new LinkedHashMap<>();

    static final List<FilterGroup> groups = new ArrayList<>();

    static String focusedInputKey = null;

    static String openPickerKey = null;

    static int pickerScrollOffset = 0;

    static boolean pickerScrollbarDragging = false;

    static int pickerScrollbarDragOffset = 0;

    static boolean pickerScrollbarVisible = false;

    static int pickerScrollbarTrackX;

    static int pickerScrollbarTrackTop;

    static int pickerScrollbarTrackBottom;

    static int pickerScrollbarThumbTop;

    static int pickerScrollbarThumbHeight;

    static int pickerDropdownX;

    static int pickerDropdownY;

    static int pickerDropdownW;

    static int pickerDropdownH;

    static List<CostNamePickerEntry> pickerDropdownEntries = List.of();

    static long inputCursorBlinkTime = 0;

    static boolean inputCursorVisible = true;
}

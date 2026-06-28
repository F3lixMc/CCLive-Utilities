package net.felix.utilities.ItemViewer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Checkbox-Filter-Menü für den Item Viewer.
 * Ausgewählte Filter werden als Suchtext (#Tag, +Modifier, …) gesetzt.
 */
public final class ItemViewerFilterMenu {

    private static final int CHECKBOX_SIZE = 10;
    private static final int CLOSE_BUTTON_SIZE = 15;
    private static final int CLOSE_BUTTON_MARGIN = 5;
    private static final int TITLE_Y = 10;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 4;
    private static final int SCROLLBAR_END_CAP = 2;
    private static final int COLUMN_GAP = 14;
    private static final int ROW_HEIGHT = 14;
    private static final int HEADER_GAP = 6;
    private static final int CONTENT_PADDING = 12;

    private static final String[] MODIFIER_CATEGORIES = {
            "Andere", "Herstellung", "Fähigkeiten", "Schaden", "Verteidigung", "Attribute"
    };

    private static final String[] COST_CATEGORY_LABELS = {
            "Amboss", "Ofen", "Material", "Ressource"
    };

    private static final Map<String, String> COST_CATEGORY_KEYS = Map.of(
            "Amboss", "amboss",
            "Ofen", "ofen",
            "Material", "material",
            "Ressource", "ressource"
    );

    private static final Map<String, String> COST_LABEL_BY_KEY = Map.of(
            "amboss", "Amboss",
            "ofen", "Ofen",
            "material", "Material",
            "ressource", "Ressource"
    );

    private static final String[] ATTRIBUTE_LABELS = {
            "Ebene", "Schaden", "Rüstung", "Abbaugeschwindigkeit", "ItemScore"
    };

    private static final Map<String, String> ATTRIBUTE_SEARCH_NAME = Map.of(
            "Ebene", "Ebene",
            "Schaden", "Schaden",
            "Rüstung", "Rüstung",
            "Abbaugeschwindigkeit", "Abbaugeschwindigkeit",
            "ItemScore", "ItemScore"
    );

    private static final int COST_AMOUNT_FIELD_CHARS = 7;
    private static final List<String> COST_MENU_OPERATORS = List.of("=", ">=", "<=");
    private static final int COST_ITEM_FIELD_CHARS = 12;
    private static final int COST_ITEM_MAX_LENGTH = 32;
    private static final int COST_AMOUNT_MAX_LENGTH = 8;
    private static final int PICKER_BUTTON_SIZE = 10;
    private static final int PICKER_BUTTON_GAP = 2;
    private static final int PICKER_OPTION_HEIGHT = 14;
    private static final int PICKER_MAX_VISIBLE = 8;
    private static final int PICKER_PADDING = 4;
    private static final int PICKER_SCROLLBAR_WIDTH = 8;
    private static final int PICKER_SCROLLBAR_GAP = 2;
    private static final int PICKER_FLOOR_LABEL_GAP = 8;
    private static final int PICKER_FLOOR_LABEL_COLOR = 0xFFAAAAAA;

    private static final List<String> RARITY_ORDER = List.of(
            "common", "uncommon", "rare", "epic", "legendary"
    );

    private static final int ROW_GRID_GAP = 10;

    private static final List<List<String>> FILTER_ROW_TITLES = List.of(
            List.of("Rüstung", "Angel-Komponenten", "Typ", "Waffen", "Werkzeuge"),
            List.of("Schmiedezustand", "Modifier", "Seltenheit"),
            List.of("Item", "Operator", "Anzahl"),
            List.of("Attribute", "Att.-Operator", "Att.-Anzahl")
    );

    /** Feste Filter-Gruppen (Titel → Definition). */
    private static final List<FilterGroupDef> FILTER_GROUP_DEFS = List.of(
            new FilterGroupDef("Rüstung", List.of(
                    tag("Helm", "helm"),
                    tag("Brust", "brustplatte"),
                    tag("Hose", "hose"),
                    tag("Schuhe", "schuhe"),
                    tag("Ring", "ring"),
                    tag("Handschuhe", "handschuhe"),
                    tag("Schulter", "schulter"),
                    tag("Halskette", "halskette"),
                    tag("Gürtel", "gürtel")
            )),
            new FilterGroupDef("Werkzeuge", List.of(
                    tag("Spitzhacke", "spitzhacke"),
                    tags("Axt", "#werkzeug", "#axt"),
                    tag("Hacke", "hacke"),
                    tag("Angel", "angel")
            )),
            new FilterGroupDef("Waffen", List.of(
                    tag("Schwert", "schwert"),
                    tags("Axt", "#waffe", "#axt"),
                    tag("Bogen", "bogen"),
                    tag("Armbrust", "armbrust")
            )),
            new FilterGroupDef("Modifier", modifierOptions()),
            new FilterGroupDef("Seltenheit", rarityOptions()),
            new FilterGroupDef("Schmiedezustand", forgingOptions()),
            new FilterGroupDef("Angel-Komponenten", List.of(
                    tag("Gewicht", "gewicht"),
                    tag("Haken", "haken"),
                    tag("Rolle", "rolle"),
                    tag("Köder", "köder"),
                    tag("Schnur", "schnur"),
                    tag("Pose", "pose"),
                    tag("Rute", "rute"),
                    tag("Spinner", "spinner")
            )),
            new FilterGroupDef("Typ", List.of(
                    tag("Platte", "platte"),
                    tag("Leder", "leder"),
                    tag("Accessoire", "accessoire"),
                    tag("Waffe", "waffe"),
                    tags("Werkzeug", "#werkzeug", "#werkzeuge")
            )),
            new FilterGroupDef("Verschiedenes", List.of(
                    tag("Module", "modul"),
                    tag("Modultasche", "modultasche"),
                    tag("Machtkristalle", "machtkristall"),
                    tag("Karten", "kartenslot"),
                    tag("Lizenzen", "lizenz"),
                    tag("Fähigkeiten", "fähigkeit")
            )),
            new FilterGroupDef("Item", costCategoryOptions()),
            new FilterGroupDef("Operator", costCategoryOptions()),
            new FilterGroupDef("Anzahl", costCategoryOptions()),
            new FilterGroupDef("Attribute", attributeOptions()),
            new FilterGroupDef("Att.-Operator", attributeOptions()),
            new FilterGroupDef("Att.-Anzahl", attributeOptions())
    );

    private static boolean open = false;
    private static int scrollOffset = 0;
    private static boolean hovered = false;
    private static boolean scrollbarDragging = false;
    private static int scrollbarDragOffset = 0;
    private static int contentHeight = 0;
    private static int viewportHeight = 0;

    private static final Set<String> selectedOptionKeys = new LinkedHashSet<>();
    private static final Set<String> draftSelectedOptionKeys = new LinkedHashSet<>();
    private static final Map<String, String> inputFieldText = new LinkedHashMap<>();
    private static final Map<String, String> draftInputFieldText = new LinkedHashMap<>();
    private static final Map<String, InputFieldState> inputFieldStates = new LinkedHashMap<>();
    private static final List<FilterGroup> groups = new ArrayList<>();
    private static String focusedInputKey = null;
    private static String openPickerKey = null;
    private static int pickerScrollOffset = 0;
    private static boolean pickerScrollbarDragging = false;
    private static int pickerScrollbarDragOffset = 0;
    private static boolean pickerScrollbarVisible = false;
    private static int pickerScrollbarTrackX;
    private static int pickerScrollbarTrackTop;
    private static int pickerScrollbarTrackBottom;
    private static int pickerScrollbarThumbTop;
    private static int pickerScrollbarThumbHeight;
    private static int pickerDropdownX;
    private static int pickerDropdownY;
    private static int pickerDropdownW;
    private static int pickerDropdownH;
    private static List<ItemViewerUtility.CostNamePickerEntry> pickerDropdownEntries = List.of();
    private static long inputCursorBlinkTime = 0;
    private static boolean inputCursorVisible = true;

    private static final class InputFieldState {
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

    private record VisibleFieldSlice(String display, int displayCursor, int displaySelStart, int displaySelEnd,
                                     boolean centered) {
    }

    private enum RowInputType {
        NONE,
        MODIFIER_DIGIT,
        COST_OPERATOR,
        COST_AMOUNT,
        COST_ITEM
    }

    private ItemViewerFilterMenu() {
    }

    public static boolean isOpen() {
        return open;
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

    public static void render(DrawContext context) {
        if (!open) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        PanelLayout layout = computePanelLayout(client);
        TextRenderer tr = client.textRenderer;

        context.fill(0, 0, layout.screenWidth, layout.screenHeight, 0x80000000);
        drawPanel(context, layout.boxX, layout.boxY, layout.boxWidth, layout.boxHeight);

        boolean closeHovered = ItemViewerUtility.getLastMouseX() >= layout.closeX && ItemViewerUtility.getLastMouseX() < layout.closeX + CLOSE_BUTTON_SIZE
                && ItemViewerUtility.getLastMouseY() >= layout.closeY && ItemViewerUtility.getLastMouseY() < layout.closeY + CLOSE_BUTTON_SIZE;
        drawCloseButton(context, layout.closeX, layout.closeY, closeHovered);

        String title = "Filter";
        int titleWidth = tr.getWidth(title);
        int titleY = layout.boxY + TITLE_Y;
        context.drawText(tr, title, layout.boxX + (layout.boxWidth - titleWidth) / 2, titleY, 0xFFFFFF00, true);

        hovered = ItemViewerUtility.getLastMouseX() >= layout.boxX && ItemViewerUtility.getLastMouseX() < layout.boxX + layout.boxWidth
                && ItemViewerUtility.getLastMouseY() >= layout.boxY && ItemViewerUtility.getLastMouseY() < layout.boxY + layout.boxHeight;

        finalizeScrollLayout(layout, tr);
        scrollOffset = clampScrollOffset(scrollOffset);

        context.enableScissor(layout.contentX, layout.contentTop, layout.contentScrollRight, layout.contentBottom);
        for (int row = 0; row < FILTER_ROW_TITLES.size(); row++) {
            List<FilterGroup> rowGroups = getRowGroups(row);
            for (int i = 0; i < rowGroups.size(); i++) {
                FilterGroup group = rowGroups.get(i);
                int headerY = group.headerY - scrollOffset;
                if (headerY + tr.fontHeight >= layout.contentTop && headerY <= layout.contentBottom) {
                    context.drawText(tr, displayGroupTitle(group.title), group.columnX, headerY, 0xFFFFFF55, false);
                }
                if (i < rowGroups.size() - 1) {
                    FilterGroup next = rowGroups.get(i + 1);
                    int dividerX = group.columnX + group.columnWidth + COLUMN_GAP / 2;
                    int dividerTop = Math.min(group.headerY, next.headerY) - scrollOffset;
                    int dividerBottom = Math.max(group.bottomY, next.bottomY) - scrollOffset;
                    if (dividerBottom > layout.contentTop && dividerTop < layout.contentBottom) {
                        context.fill(dividerX, Math.max(dividerTop, layout.contentTop),
                                dividerX + 1, Math.min(dividerBottom, layout.contentBottom), 0xFF505050);
                    }
                }
            }
        }
        for (FilterGroup group : groups) {
            for (FilterOption option : group.options) {
                int drawY = option.y - scrollOffset;
                if (drawY + ROW_HEIGHT < layout.contentTop || drawY > layout.contentBottom) {
                    continue;
                }
                drawCheckbox(context, tr, group, option, drawY);
            }
        }
        context.disableScissor();

        if (layout.scrollable) {
            renderScrollbar(context, layout);
        }

        boolean clearHovered = ItemViewerUtility.getLastMouseX() >= layout.clearX && ItemViewerUtility.getLastMouseX() < layout.clearX + layout.clearW
                && ItemViewerUtility.getLastMouseY() >= layout.clearY && ItemViewerUtility.getLastMouseY() < layout.clearY + 16;
        drawSmallButton(context, layout.clearX, layout.clearY, layout.clearW, 16, clearHovered);
        drawCenteredButtonLabel(context, tr, "Alle löschen", layout.clearX, layout.clearY, layout.clearW, 16);
        renderOpenItemPicker(context, tr, layout);
        renderHoverTooltips(context, tr);
    }

    private static void renderHoverTooltips(DrawContext context, TextRenderer tr) {
        double mouseX = ItemViewerUtility.getLastMouseX();
        double mouseY = ItemViewerUtility.getLastMouseY();

        for (FilterGroup group : groups) {
            if (!"Item".equals(group.title)) {
                continue;
            }
            for (FilterOption option : group.options) {
                if (option.pickerButtonX < 0) {
                    continue;
                }
                int rowY = option.y - scrollOffset;
                int pickerY = rowY + (ROW_HEIGHT - PICKER_BUTTON_SIZE) / 2;
                if (mouseX >= option.pickerButtonX && mouseX < option.pickerButtonX + PICKER_BUTTON_SIZE
                        && mouseY >= pickerY && mouseY < pickerY + PICKER_BUTTON_SIZE) {
                    String listName = pickerListDisplayName(option.label);
                    context.drawTooltip(tr,
                            List.of(Text.literal("Klicken um " + listName + "-Liste zu öffnen")),
                            (int) mouseX, (int) mouseY);
                    return;
                }
            }
        }

        if (findOperatorFieldAt(mouseX, mouseY) != null) {
            context.drawTooltip(tr,
                    List.of(Text.literal("Klicken zum wechseln (=, >=, <=)")),
                    (int) mouseX, (int) mouseY);
        }
    }

    private static String pickerListDisplayName(String label) {
        if ("Ressource".equals(label)) {
            return "Ressourcen";
        }
        return label;
    }

    public static boolean handleClick(double mouseX, double mouseY, int button) {
        if (!open) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }
        PanelLayout layout = computePanelLayout(client);
        TextRenderer tr = client.textRenderer;
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
            ItemViewerUtility.blurSearchFieldFocus();
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }
        PanelLayout layout = computePanelLayout(client);
        finalizeScrollLayout(layout, client.textRenderer);

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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }
        PanelLayout layout = computePanelLayout(client);
        finalizeScrollLayout(layout, client.textRenderer);
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

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            focusedInputKey = null;
            return true;
        }

        InputFieldState state = getOrCreateState(focusedInputKey);
        String text = inputFieldText.getOrDefault(focusedInputKey, "");
        state.cursor = clampCursor(state.cursor, text.length());

        if (isCtrlDown(modifiers)) {
            if (keyCode == GLFW.GLFW_KEY_A) {
                if (!text.isEmpty()) {
                    state.selectionStart = 0;
                    state.selectionEnd = text.length();
                    state.cursor = text.length();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_C) {
                if (state.hasSelection()) {
                    int min = state.selectionMin();
                    int max = state.selectionMax();
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null) {
                        client.keyboard.setClipboard(text.substring(min, max));
                    }
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_V) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    String clipboard = client.keyboard.getClipboard();
                    if (clipboard != null && !clipboard.isEmpty()) {
                        insertAtCursor(clipboard);
                    }
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_X) {
                if (state.hasSelection()) {
                    int min = state.selectionMin();
                    int max = state.selectionMax();
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null) {
                        client.keyboard.setClipboard(text.substring(min, max));
                    }
                    text = deleteSelection(text, state);
                    inputFieldText.put(focusedInputKey, text);
                    commitInputFieldChange(focusedInputKey);
                }
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            text = deleteBeforeCursor(text, state);
            inputFieldText.put(focusedInputKey, text);
            commitInputFieldChange(focusedInputKey);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            text = deleteAfterCursor(text, state);
            inputFieldText.put(focusedInputKey, text);
            commitInputFieldChange(focusedInputKey);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            moveCursorHorizontal(text, state, -1, isShiftDown(modifiers));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            moveCursorHorizontal(text, state, 1, isShiftDown(modifiers));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            moveCursorTo(text, state, 0, isShiftDown(modifiers));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            moveCursorTo(text, state, text.length(), isShiftDown(modifiers));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
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
                    && (keyCode == GLFW.GLFW_KEY_PERIOD || keyCode == GLFW.GLFW_KEY_COMMA
                    || keyCode == GLFW.GLFW_KEY_KP_DECIMAL)) {
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

    private static boolean isCtrlDown(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    private static boolean isShiftDown(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    private static int clampCursor(int cursor, int textLength) {
        return Math.max(0, Math.min(cursor, textLength));
    }

    private static InputFieldState getOrCreateState(String inputKey) {
        return inputFieldStates.computeIfAbsent(inputKey, key -> new InputFieldState());
    }

    private static void setInputFieldText(String inputKey, String value) {
        inputFieldText.put(inputKey, value);
        draftInputFieldText.put(inputKey, value);
        InputFieldState state = getOrCreateState(inputKey);
        state.cursor = value.length();
        state.clearSelection();
    }

    private static void clearInputField(String inputKey) {
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

    private static String deleteSelection(String text, InputFieldState state) {
        if (!state.hasSelection()) {
            return text;
        }
        int min = state.selectionMin();
        int max = state.selectionMax();
        state.cursor = min;
        state.clearSelection();
        return text.substring(0, min) + text.substring(max);
    }

    private static String deleteBeforeCursor(String text, InputFieldState state) {
        text = deleteSelection(text, state);
        if (state.cursor > 0) {
            text = text.substring(0, state.cursor - 1) + text.substring(state.cursor);
            state.cursor--;
        }
        return text;
    }

    private static String deleteAfterCursor(String text, InputFieldState state) {
        text = deleteSelection(text, state);
        if (state.cursor < text.length()) {
            text = text.substring(0, state.cursor) + text.substring(state.cursor + 1);
        }
        return text;
    }

    private static void moveCursorHorizontal(String text, InputFieldState state, int delta, boolean extendSelection) {
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

    private static void moveCursorTo(String text, InputFieldState state, int position, boolean extendSelection) {
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

    private static void insertAtCursor(String insert) {
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

    private static boolean isItemScoreAmountField(String inputKey) {
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

    private static boolean isModifierOnlyKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_SHIFT
                || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
                || keyCode == GLFW.GLFW_KEY_LEFT_CONTROL
                || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
                || keyCode == GLFW.GLFW_KEY_LEFT_ALT
                || keyCode == GLFW.GLFW_KEY_RIGHT_ALT
                || keyCode == GLFW.GLFW_KEY_CAPS_LOCK;
    }

    private static RowInputType getInputTypeForKey(String inputKey) {
        for (FilterGroup group : groups) {
            for (FilterOption option : group.options) {
                if (inputKey.equals(optionKey(group.title, option.label))) {
                    return option.inputType;
                }
            }
        }
        return RowInputType.NONE;
    }

    private static Character resolveDigitKey(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return (char) ('0' + (keyCode - GLFW.GLFW_KEY_0));
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return (char) ('0' + (keyCode - GLFW.GLFW_KEY_KP_0));
        }
        return null;
    }

    private static String resolveItemTextKey(int keyCode, int modifiers) {
        boolean shift = isShiftDown(modifiers);
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            return " ";
        }
        if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
            return shift ? "_" : "-";
        }
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            char letter;
            if (keyCode == GLFW.GLFW_KEY_Y) {
                letter = shift ? 'Z' : 'z';
            } else if (keyCode == GLFW.GLFW_KEY_Z) {
                letter = shift ? 'Y' : 'y';
            } else {
                letter = shift ? (char) keyCode : (char) (keyCode + 32);
            }
            return String.valueOf(letter);
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + (keyCode - GLFW.GLFW_KEY_0)));
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return String.valueOf((char) ('0' + (keyCode - GLFW.GLFW_KEY_KP_0)));
        }
        return null;
    }

    private static int clampScrollOffset(int offset) {
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        return Math.max(0, Math.min(offset, maxScroll));
    }

    private static void finalizeScrollLayout(PanelLayout layout, TextRenderer tr) {
        layoutGroups(tr, layout.contentX, layout.contentTop, layout.contentWidth);
        viewportHeight = layout.contentBottom - layout.contentTop;
        layout.scrollable = contentHeight > viewportHeight;
        if (layout.scrollable) {
            int scrollContentWidth = layout.contentWidth - SCROLLBAR_WIDTH - SCROLLBAR_GAP;
            layoutGroups(tr, layout.contentX, layout.contentTop, scrollContentWidth);
            layout.scrollable = contentHeight > viewportHeight;
            layout.scrollBarX = layout.contentX + layout.contentWidth - SCROLLBAR_WIDTH;
            layout.contentScrollRight = layout.scrollBarX - SCROLLBAR_GAP;
        } else {
            layout.scrollBarX = 0;
            layout.contentScrollRight = layout.contentX + layout.contentWidth;
        }
    }

    private static int computeThumbHeight(int trackHeight) {
        if (contentHeight <= 0) {
            return trackHeight;
        }
        return Math.max(16, (int) ((long) trackHeight * viewportHeight / contentHeight));
    }

    private static int computeThumbY(PanelLayout layout) {
        int trackHeight = layout.contentBottom - layout.contentTop;
        int thumbHeight = computeThumbHeight(trackHeight);
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (maxScroll <= 0) {
            return layout.contentTop;
        }
        return layout.contentTop + (int) ((long) (trackHeight - thumbHeight) * scrollOffset / maxScroll);
    }

    private static void renderScrollbar(DrawContext context, PanelLayout layout) {
        int trackTop = layout.contentTop;
        int trackBottom = layout.contentBottom;
        int barX = layout.scrollBarX;
        int trackHeight = trackBottom - trackTop;

        context.fill(barX, trackTop, barX + SCROLLBAR_WIDTH, trackBottom, 0xFF2A2A2A);
        context.fill(barX, trackTop, barX + SCROLLBAR_WIDTH, trackTop + SCROLLBAR_END_CAP, 0xFF606060);
        context.fill(barX, trackBottom - SCROLLBAR_END_CAP, barX + SCROLLBAR_WIDTH, trackBottom, 0xFF606060);
        context.fill(barX, trackTop, barX + 1, trackBottom, 0xFF606060);
        context.fill(barX + SCROLLBAR_WIDTH - 1, trackTop, barX + SCROLLBAR_WIDTH, trackBottom, 0xFF606060);
        context.drawBorder(barX, trackTop, SCROLLBAR_WIDTH, trackHeight, 0xFF808080);

        int thumbY = computeThumbY(layout);
        int thumbHeight = computeThumbHeight(trackHeight);
        boolean thumbHovered = !scrollbarDragging && ItemViewerUtility.getLastMouseX() >= barX
                && ItemViewerUtility.getLastMouseX() < barX + SCROLLBAR_WIDTH
                && ItemViewerUtility.getLastMouseY() >= thumbY
                && ItemViewerUtility.getLastMouseY() < thumbY + thumbHeight;
        int thumbColor = scrollbarDragging ? 0xFFB0B0B0 : (thumbHovered ? 0xFFA0A0A0 : 0xFF888888);
        context.fill(barX + 1, thumbY, barX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, thumbColor);
        context.fill(barX + 1, thumbY, barX + SCROLLBAR_WIDTH - 1, thumbY + SCROLLBAR_END_CAP, 0xFFC8C8C8);
        context.fill(barX + 1, thumbY + thumbHeight - SCROLLBAR_END_CAP, barX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, 0xFF707070);
        context.drawBorder(barX + 1, thumbY, SCROLLBAR_WIDTH - 2, thumbHeight, 0xFFC0C0C0);
    }

    private static void setScrollFromThumbTop(int thumbTop, PanelLayout layout) {
        int trackHeight = layout.contentBottom - layout.contentTop;
        int thumbHeight = computeThumbHeight(trackHeight);
        int thumbTravel = trackHeight - thumbHeight;
        int target = thumbTop - layout.contentTop;
        target = Math.max(0, Math.min(target, thumbTravel));
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (thumbTravel <= 0 || maxScroll <= 0) {
            scrollOffset = 0;
            return;
        }
        scrollOffset = (int) ((long) target * maxScroll / thumbTravel);
        scrollOffset = clampScrollOffset(scrollOffset);
    }

    private static boolean handleScrollbarClick(double mouseX, double mouseY, PanelLayout layout) {
        if (!layout.scrollable) {
            return false;
        }
        int barX = layout.scrollBarX;
        if (mouseX < barX || mouseX >= barX + SCROLLBAR_WIDTH
                || mouseY < layout.contentTop || mouseY >= layout.contentBottom) {
            return false;
        }
        int trackHeight = layout.contentBottom - layout.contentTop;
        int thumbHeight = computeThumbHeight(trackHeight);
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (maxScroll <= 0) {
            return true;
        }

        int thumbY = computeThumbY(layout);
        if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
            scrollbarDragging = true;
            scrollbarDragOffset = (int) mouseY - thumbY;
            return true;
        }

        setScrollFromThumbTop((int) mouseY - thumbHeight / 2, layout);
        thumbY = computeThumbY(layout);
        scrollbarDragging = true;
        scrollbarDragOffset = (int) mouseY - thumbY;
        return true;
    }

    private static void drawCloseButton(DrawContext context, int x, int y, boolean hovered) {
        drawSmallButton(context, x, y, CLOSE_BUTTON_SIZE, hovered);
        drawCloseX(context, x, y, CLOSE_BUTTON_SIZE);
    }

    private static void drawCloseX(DrawContext context, int boxX, int boxY, int size) {
        int color = 0xFFFFFFFF;
        int margin = 4;
        int span = size - margin * 2;
        for (int i = 0; i < span; i++) {
            int px = boxX + margin + i;
            context.fill(px, boxY + margin + i, px + 1, boxY + margin + i + 1, color);
            context.fill(px, boxY + size - margin - 1 - i, px + 1, boxY + size - margin - i, color);
        }
    }

    private static PanelLayout computePanelLayout(MinecraftClient client) {
        TextRenderer tr = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

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

        int clearW = tr.getWidth("Alle löschen") + 12;
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

    private static FilterGroup findGroupByTitle(String title) {
        for (FilterGroup group : groups) {
            if (group.title.equals(title)) {
                return group;
            }
        }
        return null;
    }

    private static List<FilterGroup> getRowGroups(int rowIndex) {
        int start = 0;
        for (int i = 0; i < rowIndex; i++) {
            start += FILTER_ROW_TITLES.get(i).size();
        }
        int end = start + FILTER_ROW_TITLES.get(rowIndex).size();
        return groups.subList(start, end);
    }

    private static String optionKey(String groupTitle, String optionLabel) {
        return groupTitle + '\0' + optionLabel;
    }

    private static void toggleOption(FilterGroup group, FilterOption option) {
        String key = optionKey(group.title, option.label);
        if (selectedOptionKeys.contains(key)) {
            selectedOptionKeys.remove(key);
        } else {
            if (!isMultiSelectGroup(group.title)) {
                clearGroupSelections(group);
            }
            selectedOptionKeys.add(key);
        }
        applySelections();
    }

    private static boolean isMultiSelectGroup(String groupTitle) {
        return "Modifier".equals(groupTitle) || "Item".equals(groupTitle) || "Operator".equals(groupTitle)
                || "Anzahl".equals(groupTitle) || "Attribute".equals(groupTitle) || "Att.-Operator".equals(groupTitle)
                || "Att.-Anzahl".equals(groupTitle);
    }

    private static String displayGroupTitle(String groupTitle) {
        return switch (groupTitle) {
            case "Att.-Operator" -> "Operator";
            case "Att.-Anzahl" -> "Anzahl";
            default -> groupTitle;
        };
    }

    private static boolean isAttributeLabel(String label) {
        for (String attributeLabel : ATTRIBUTE_LABELS) {
            if (attributeLabel.equals(label)) {
                return true;
            }
        }
        return false;
    }

    private static String attributeLabelForStatName(String statName) {
        if (statName == null) {
            return null;
        }
        for (String label : ATTRIBUTE_LABELS) {
            if (label.equalsIgnoreCase(statName)) {
                return label;
            }
        }
        return null;
    }

    private static void clearGroupSelections(FilterGroup group) {
        for (FilterOption groupOption : group.options) {
            selectedOptionKeys.remove(optionKey(group.title, groupOption.label));
        }
    }

    private static boolean isOptionSelected(FilterGroup group, FilterOption option) {
        return selectedOptionKeys.contains(optionKey(group.title, option.label));
    }

    private static void applySelections() {
        String search = buildSearchFromSelectedOptions();
        ItemViewerUtility.setSearchFromFilterMenu(search);
    }

    private static String buildSearchFromSelectedOptions() {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (FilterGroup group : groups) {
            for (FilterOption option : group.options) {
                if (!isOptionSelected(group, option)) {
                    continue;
                }
                if (option.inputType == RowInputType.MODIFIER_DIGIT) {
                    tokens.add(formatModifierSearchToken(group, option));
                } else if (option.inputType == RowInputType.COST_ITEM) {
                    String costToken = formatItemCostSearchToken(group, option);
                    if (costToken != null) {
                        tokens.add(costToken);
                    }
                } else if (option.inputType == RowInputType.COST_AMOUNT) {
                    if (isAttributeLabel(option.label)) {
                        String attrToken = formatAttributeAnzahlSearchToken(group, option);
                        if (attrToken != null) {
                            tokens.add(attrToken);
                        }
                    } else {
                        String costToken = formatAnzahlCostSearchToken(group, option);
                        if (costToken != null) {
                            tokens.add(costToken);
                        }
                    }
                } else if ("Attribute".equals(group.title)) {
                    String attrToken = formatAttributeOnlySearchToken(option);
                    if (attrToken != null) {
                        tokens.add(attrToken);
                    }
                } else if (option.inputType != RowInputType.COST_OPERATOR) {
                    tokens.addAll(option.tokens);
                }
            }
        }
        return String.join(", ", tokens);
    }

    private static void persistAllInputDrafts() {
        draftInputFieldText.putAll(inputFieldText);
        draftSelectedOptionKeys.clear();
        for (String key : selectedOptionKeys) {
            if (isPersistedSelectionKey(key)) {
                draftSelectedOptionKeys.add(key);
            }
        }
    }

    private static void commitInputFieldChange(String inputKey) {
        draftInputFieldText.put(inputKey, inputFieldText.getOrDefault(inputKey, ""));
        if (selectedOptionKeys.contains(inputKey)) {
            applySelections();
        }
    }

    private static void restoreInputDrafts() {
        for (Map.Entry<String, String> entry : draftInputFieldText.entrySet()) {
            if (!selectedOptionKeys.contains(entry.getKey())) {
                inputFieldText.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void restoreSelectionDrafts() {
        selectedOptionKeys.addAll(draftSelectedOptionKeys);
    }

    private static boolean isPersistedSelectionKey(String key) {
        int separator = key.indexOf('\0');
        if (separator < 0) {
            return false;
        }
        return isMultiSelectGroup(key.substring(0, separator));
    }

    private static String formatModifierSearchToken(FilterGroup group, FilterOption option) {
        String base = option.tokens.get(0);
        String count = inputFieldText.getOrDefault(optionKey(group.title, option.label), "").trim();
        if (!count.isEmpty()) {
            return base + ":" + count;
        }
        return base;
    }

    private static String formatItemCostSearchToken(FilterGroup group, FilterOption option) {
        String category = option.tokens.get(0);
        String value = inputFieldText.getOrDefault(optionKey(group.title, option.label), "").trim();
        if (value.isEmpty()) {
            return null;
        }
        return category + ":" + value;
    }

    private static String formatAnzahlCostSearchToken(FilterGroup group, FilterOption option) {
        String category = option.tokens.get(0);
        String digits = extractAmountDigits(
                inputFieldText.getOrDefault(optionKey(group.title, option.label), "").trim());
        if (digits.isEmpty()) {
            return null;
        }
        String operator = getCostOperatorForLabel(option.label);
        if ("=".equals(operator)) {
            return category + ":" + digits;
        }
        return category + ":" + operator + digits;
    }

    private static String getCostOperatorForLabel(String label) {
        String operator = inputFieldText.getOrDefault(optionKey("Operator", label), "=").trim();
        return COST_MENU_OPERATORS.contains(operator) ? operator : "=";
    }

    private static String getAttributeOperatorForLabel(String label) {
        String operator = inputFieldText.getOrDefault(optionKey("Att.-Operator", label), "=").trim();
        return COST_MENU_OPERATORS.contains(operator) ? operator : "=";
    }

    private static String formatAttributeOnlySearchToken(FilterOption option) {
        String label = option.label;
        String anzahlKey = optionKey("Att.-Anzahl", label);
        if (selectedOptionKeys.contains(anzahlKey)) {
            String digits = extractAmountDigits(inputFieldText.getOrDefault(anzahlKey, "").trim());
            if (!digits.isEmpty()) {
                return null;
            }
        }
        String searchName = ATTRIBUTE_SEARCH_NAME.get(label);
        return searchName != null ? "@" + searchName : null;
    }

    private static String formatAttributeAnzahlSearchToken(FilterGroup group, FilterOption option) {
        String label = option.label;
        String searchName = ATTRIBUTE_SEARCH_NAME.get(label);
        if (searchName == null) {
            return null;
        }
        String amountValue = "ItemScore".equals(label)
                ? extractDecimalAmount(inputFieldText.getOrDefault(optionKey(group.title, option.label), "").trim())
                : extractAmountDigits(inputFieldText.getOrDefault(optionKey(group.title, option.label), "").trim());
        if (amountValue.isEmpty()) {
            return null;
        }
        if ("Ebene".equals(label)) {
            amountValue = clampEbeneDigits(amountValue);
            if (amountValue.isEmpty()) {
                return null;
            }
        }
        String operator = getAttributeOperatorForLabel(label);
        if ("Ebene".equals(label)) {
            if ("=".equals(operator)) {
                return "@Ebene=" + amountValue;
            }
            return "@Ebene" + operator + amountValue;
        }
        if ("=".equals(operator)) {
            return "@" + searchName + "=" + amountValue;
        }
        return "@" + searchName + operator + amountValue;
    }

    private static String formatStatValueForSync(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static String clampEbeneDigits(String digits) {
        if (digits.isEmpty()) {
            return digits;
        }
        try {
            int value = Integer.parseInt(digits);
            value = Math.max(1, Math.min(100, value));
            return String.valueOf(value);
        } catch (NumberFormatException e) {
            return digits;
        }
    }

    private static String extractAmountDigits(String value) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        return digits.toString();
    }

    private static String extractDecimalAmount(String value) {
        StringBuilder amount = new StringBuilder();
        boolean hasSeparator = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                amount.append(c);
            } else if ((c == '.' || c == ',') && !hasSeparator) {
                amount.append('.');
                hasSeparator = true;
            }
        }
        return amount.toString();
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

    private static String findOperatorFieldAt(double mouseX, double mouseY) {
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

    private static void syncSelectionsFromSearch(String currentSearch) {
        selectedOptionKeys.clear();
        resetInputFieldsForSync();
        if (currentSearch == null || currentSearch.isBlank()) {
            return;
        }
        SearchQuery query = ItemSearchParser.parse(currentSearch.trim());
        for (ModifierFilter filter : query.modifierFilters) {
            if (filter.modifier == null) {
                continue;
            }
            for (String modifier : MODIFIER_CATEGORIES) {
                if (modifier.equalsIgnoreCase(filter.modifier)) {
                    String key = optionKey("Modifier", modifier);
                    if (filter.count != null) {
                        int count = filter.count;
                        String digit = String.valueOf(Math.abs(count) % 10);
                        setInputFieldText(key, digit);
                    }
                    selectedOptionKeys.add(key);
                    break;
                }
            }
        }
        for (CostFilter filter : query.costFilters) {
            if (filter.category == null) {
                continue;
            }
            String category = filter.category.toLowerCase(Locale.ROOT);
            if (category.startsWith("material") && !category.equals("material")) {
                category = "material";
            }
            String label = COST_LABEL_BY_KEY.get(category);
            if (label == null) {
                continue;
            }
            if (filter.itemName != null && !filter.itemName.isBlank()) {
                String key = optionKey("Item", label);
                selectedOptionKeys.add(key);
                setInputFieldText(key, filter.itemName.trim());
            } else if (filter.amount != null) {
                String key = optionKey("Anzahl", label);
                selectedOptionKeys.add(key);
                setInputFieldText(key, String.valueOf(filter.amount));
                String opKey = optionKey("Operator", label);
                String operator = filter.amountOperator != null && COST_MENU_OPERATORS.contains(filter.amountOperator)
                        ? filter.amountOperator
                        : "=";
                setInputFieldText(opKey, operator);
                if (!"=".equals(operator)) {
                    selectedOptionKeys.add(opKey);
                }
            }
        }
        for (StatFilter filter : query.statFilters) {
            String label = attributeLabelForStatName(filter.statName);
            if (label == null) {
                continue;
            }
            if (filter.operator != null && filter.value != null) {
                String key = optionKey("Att.-Anzahl", label);
                selectedOptionKeys.add(key);
                setInputFieldText(key, formatStatValueForSync(filter.value));
                String opKey = optionKey("Att.-Operator", label);
                String operator = filter.operator;
                setInputFieldText(opKey, COST_MENU_OPERATORS.contains(operator) ? operator : "=");
                if (!"=".equals(operator)) {
                    selectedOptionKeys.add(opKey);
                }
            } else {
                selectedOptionKeys.add(optionKey("Attribute", label));
            }
        }
        for (FloorFilter filter : query.floorFilters) {
            if (filter.operator == null || filter.value == null) {
                continue;
            }
            String label = "Ebene";
            String key = optionKey("Att.-Anzahl", label);
            selectedOptionKeys.add(key);
            setInputFieldText(key, String.valueOf(filter.value));
            String opKey = optionKey("Att.-Operator", label);
            String operator = filter.operator;
            setInputFieldText(opKey, COST_MENU_OPERATORS.contains(operator) ? operator : "=");
            if (!"=".equals(operator)) {
                selectedOptionKeys.add(opKey);
            }
        }
        List<FilterOptionDef> matched = new ArrayList<>();
        List<String> matchedGroupTitles = new ArrayList<>();
        for (FilterGroupDef def : FILTER_GROUP_DEFS) {
            for (FilterOptionDef optionDef : def.options) {
                if (optionDef.matchesQuery(query)) {
                    matched.add(optionDef);
                    matchedGroupTitles.add(def.title);
                }
            }
        }
        Map<String, String> singleSelectByGroup = new LinkedHashMap<>();
        Set<String> modifierKeys = new LinkedHashSet<>();
        for (int i = 0; i < matched.size(); i++) {
            FilterOptionDef optionDef = matched.get(i);
            String groupTitle = matchedGroupTitles.get(i);
            boolean subsetOfAnother = false;
            for (int j = 0; j < matched.size(); j++) {
                if (i == j) {
                    continue;
                }
                FilterOptionDef other = matched.get(j);
                if (other.tokens.size() > optionDef.tokens.size()
                        && other.tokens.containsAll(optionDef.tokens)) {
                    subsetOfAnother = true;
                    break;
                }
            }
            if (!subsetOfAnother) {
                String key = optionKey(groupTitle, optionDef.label);
                if (isMultiSelectGroup(groupTitle)) {
                    modifierKeys.add(key);
                } else {
                    singleSelectByGroup.put(groupTitle, key);
                }
            }
        }
        selectedOptionKeys.addAll(singleSelectByGroup.values());
        selectedOptionKeys.addAll(modifierKeys);
    }

    private static void rebuildGroups(List<ItemData> items) {
        groups.clear();
        Map<String, FilterGroupDef> defsByTitle = new LinkedHashMap<>();
        for (FilterGroupDef def : FILTER_GROUP_DEFS) {
            defsByTitle.put(def.title, def);
        }
        for (List<String> rowTitles : FILTER_ROW_TITLES) {
            for (String title : rowTitles) {
                FilterGroupDef def = defsByTitle.get(title);
                if (def == null) {
                    continue;
                }
                List<FilterOption> options = new ArrayList<>();
                RowInputType inputType = rowInputTypeForGroup(def.title);
                for (FilterOptionDef optionDef : def.options) {
                    options.add(new FilterOption(optionDef.label, optionDef.tokens, inputType));
                }
                groups.add(new FilterGroup(def.title, options));
            }
        }
    }

    private static FilterOptionDef tag(String label, String tagName) {
        return new FilterOptionDef(label, "#" + tagName.toLowerCase(Locale.ROOT));
    }

    private static FilterOptionDef tags(String label, String... tokens) {
        return new FilterOptionDef(label, tokens);
    }

    private static List<FilterOptionDef> costCategoryOptions() {
        List<FilterOptionDef> options = new ArrayList<>();
        for (String label : COST_CATEGORY_LABELS) {
            String category = COST_CATEGORY_KEYS.get(label);
            options.add(new FilterOptionDef(label, category));
        }
        return options;
    }

    private static List<FilterOptionDef> attributeOptions() {
        List<FilterOptionDef> options = new ArrayList<>();
        for (String label : ATTRIBUTE_LABELS) {
            options.add(new FilterOptionDef(label, "@" + label));
        }
        return options;
    }

    private static RowInputType rowInputTypeForGroup(String groupTitle) {
        return switch (groupTitle) {
            case "Modifier" -> RowInputType.MODIFIER_DIGIT;
            case "Operator", "Att.-Operator" -> RowInputType.COST_OPERATOR;
            case "Anzahl", "Att.-Anzahl" -> RowInputType.COST_AMOUNT;
            case "Item" -> RowInputType.COST_ITEM;
            default -> RowInputType.NONE;
        };
    }

    private static List<FilterOptionDef> modifierOptions() {
        List<FilterOptionDef> options = new ArrayList<>();
        for (String modifier : MODIFIER_CATEGORIES) {
            options.add(new FilterOptionDef(modifier, "+" + modifier));
        }
        return options;
    }

    private static List<FilterOptionDef> rarityOptions() {
        List<FilterOptionDef> options = new ArrayList<>();
        for (String rarity : RARITY_ORDER) {
            options.add(new FilterOptionDef(formatRarityLabel(rarity), "#" + rarity));
        }
        return options;
    }

    private static List<FilterOptionDef> forgingOptions() {
        return List.of(
                tag("Frostgeschmiedet", "frostgeschmiedet"),
                tag("Lavageschmiedet", "lavageschmiedet"),
                tag("Titangeschmiedet", "titangeschmiedet"),
                tag("Drachengeschmiedet", "drachengeschmiedet"),
                tag("Dämonengeschmiedet", "dämonengeschmiedet"),
                tag("Blitzgeschmiedet", "blitzgeschmiedet"),
                tag("Sternengeschmiedet", "sternengeschmiedet")
        );
    }

    private static int computeColumnWidth(TextRenderer tr, FilterGroup group) {
        int width = tr.getWidth(displayGroupTitle(group.title));
        for (FilterOption option : group.options) {
            if (option.inputType != RowInputType.NONE) {
                int fieldWidth = inputFieldWidth(tr, option.inputType);
                int pickerExtra = hasItemPicker(option.inputType, group.title)
                        ? PICKER_BUTTON_SIZE + PICKER_BUTTON_GAP : 0;
                int rowWidth = CHECKBOX_SIZE + 4 + tr.getWidth(option.label) + tr.getWidth(":")
                        + 2 + fieldWidth + pickerExtra;
                width = Math.max(width, rowWidth);
            } else {
                width = Math.max(width, CHECKBOX_SIZE + 4 + tr.getWidth(option.label));
            }
        }
        return width + 4;
    }

    private static boolean hasItemPicker(RowInputType inputType, String groupTitle) {
        return inputType == RowInputType.COST_ITEM && "Item".equals(groupTitle);
    }

    private static int inputFieldWidth(TextRenderer tr, RowInputType inputType) {
        return switch (inputType) {
            case MODIFIER_DIGIT -> tr.getWidth("0");
            case COST_OPERATOR -> tr.getWidth(">=");
            case COST_AMOUNT -> tr.getWidth("0".repeat(COST_AMOUNT_FIELD_CHARS));
            case COST_ITEM -> tr.getWidth("0".repeat(COST_ITEM_FIELD_CHARS));
            default -> 0;
        };
    }

    private static void ensureInputFields() {
        for (FilterGroup group : groups) {
            for (FilterOption option : group.options) {
                if (option.inputType != RowInputType.NONE) {
                    String key = optionKey(group.title, option.label);
                    if (option.inputType == RowInputType.COST_OPERATOR) {
                        inputFieldText.putIfAbsent(key, "=");
                    } else {
                        inputFieldText.putIfAbsent(key, "");
                    }
                }
            }
        }
    }

    private static void resetInputFieldsForSync() {
        inputFieldText.clear();
        inputFieldStates.clear();
        ensureInputFields();
    }

    private static void clearAllInputFields() {
        resetInputFieldsForSync();
        draftInputFieldText.clear();
        draftSelectedOptionKeys.clear();
    }

    private static void layoutGroups(TextRenderer tr, int contentX, int contentTop, int contentWidth) {
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
            int optionsStartY = rowY + tr.fontHeight + HEADER_GAP;
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
                        int colonX = labelX + tr.getWidth(option.label);
                        int fieldWidth = inputFieldWidth(tr, option.inputType);
                        option.countFieldX = colonX + tr.getWidth(":") + 2;
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

    private static void drawCheckbox(DrawContext context, TextRenderer tr, FilterGroup group, FilterOption option, int rowY) {
        boolean selected = isOptionSelected(group, option);
        int boxX = option.x;
        int boxY = rowY + (ROW_HEIGHT - CHECKBOX_SIZE) / 2;
        int labelY = rowY + (ROW_HEIGHT - tr.fontHeight) / 2;

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
        context.drawText(tr, option.label, labelX, labelY, 0xFFFFFFFF, false);

        if (option.inputType != RowInputType.NONE) {
            int colonX = labelX + tr.getWidth(option.label);
            context.drawText(tr, ":", colonX, labelY, 0xFFFFFFFF, false);

            int fieldX = option.countFieldX;
            int underlineY = labelY + tr.fontHeight;
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
                boolean pickerHovered = ItemViewerUtility.getLastMouseX() >= option.pickerButtonX
                        && ItemViewerUtility.getLastMouseX() < option.pickerButtonX + PICKER_BUTTON_SIZE
                        && ItemViewerUtility.getLastMouseY() >= pickerY
                        && ItemViewerUtility.getLastMouseY() < pickerY + PICKER_BUTTON_SIZE;
                boolean pickerOpen = fieldKey.equals(openPickerKey);
                drawSmallButton(context, option.pickerButtonX, pickerY, PICKER_BUTTON_SIZE, pickerHovered || pickerOpen);
                int arrowX = option.pickerButtonX + (PICKER_BUTTON_SIZE - tr.getWidth("v")) / 2;
                int arrowY = pickerY + (PICKER_BUTTON_SIZE - tr.fontHeight) / 2;
                context.drawText(tr, "\u25BC", arrowX, arrowY, 0xFFFFFFFF, false);
            }
        }
    }

    private static void drawOperatorField(DrawContext context, TextRenderer tr, int fieldX, int labelY, int underlineY,
            int fieldW, String operatorText) {
        String display = COST_MENU_OPERATORS.contains(operatorText) ? operatorText : "=";
        context.fill(fieldX, underlineY, fieldX + fieldW, underlineY + 1, 0xFFAAAAAA);
        int textX = fieldX + (fieldW - tr.getWidth(display)) / 2;
        context.drawText(tr, display, textX, labelY, 0xFFFFFFFF, false);
    }

    private static void drawInputField(DrawContext context, TextRenderer tr, int fieldX, int labelY, int underlineY,
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
            textX = fieldX + (fieldW - tr.getWidth(slice.display())) / 2;
        }

        if (fieldFocused && slice.displaySelStart() >= 0 && slice.displaySelEnd() > slice.displaySelStart()) {
            int selStartX = textX + tr.getWidth(slice.display().substring(0, slice.displaySelStart()));
            int selEndX = textX + tr.getWidth(slice.display().substring(0, slice.displaySelEnd()));
            context.fill(selStartX, labelY, selEndX, labelY + tr.fontHeight, 0xFF2A5080);
        }

        if (!slice.display().isEmpty()) {
            context.drawText(tr, slice.display(), textX, labelY, 0xFFFFFFFF, false);
        }

        if (fieldFocused) {
            updateInputCursorBlink();
            if (inputCursorVisible) {
                int caretIndex = fieldText.isEmpty() ? 0 : Math.max(0, slice.displayCursor());
                String beforeCaret = slice.display().isEmpty() ? "" : slice.display().substring(0, caretIndex);
                int caretX = textX + tr.getWidth(beforeCaret);
                context.fill(caretX, labelY, caretX + 1, labelY + tr.fontHeight, 0xFFFFFFFF);
            }
        }
    }

    private static VisibleFieldSlice computeVisibleSlice(TextRenderer tr, String text, int maxWidth,
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

        if (tr.getWidth(text) <= maxWidth) {
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

    private record VisibleWindow(int start, int end, String display, int prefixLength) {
    }

    private static VisibleWindow findVisibleWindow(TextRenderer tr, String text, int maxWidth, int cursor,
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
                if (tr.getWidth(display) > maxWidth) {
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
                if (tr.getWidth(ellipsis + text.substring(candidateStart)) <= maxWidth) {
                    start = candidateStart;
                    break;
                }
            }
            return new VisibleWindow(start, textLength, display, start > 0 ? ellipsis.length() : 0);
        }
        int end = textLength;
        while (end > 0 && tr.getWidth(text.substring(0, end) + ellipsis) > maxWidth) {
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

    private static String truncateToFieldWidth(TextRenderer tr, String text, int maxWidth, boolean showEnd) {
        if (text.isEmpty() || tr.getWidth(text) <= maxWidth) {
            return text;
        }
        if (showEnd) {
            String ellipsis = "...";
            for (int start = 0; start < text.length(); start++) {
                String candidate = ellipsis + text.substring(start);
                if (tr.getWidth(candidate) <= maxWidth) {
                    return candidate;
                }
            }
            for (int start = 0; start < text.length(); start++) {
                String candidate = text.substring(start);
                if (tr.getWidth(candidate) <= maxWidth) {
                    return candidate;
                }
            }
            return text.substring(text.length() - 1);
        }
        String display = text;
        while (!display.isEmpty() && tr.getWidth(display + "...") > maxWidth) {
            display = display.substring(0, display.length() - 1);
        }
        return display + "...";
    }

    private record FieldLayout(int fieldX, int fieldW, RowInputType inputType) {
    }

    private static FieldLayout findFieldLayout(String inputKey) {
        for (FilterGroup group : groups) {
            for (FilterOption option : group.options) {
                if (inputKey.equals(optionKey(group.title, option.label))) {
                    return new FieldLayout(option.countFieldX, option.countFieldW, option.inputType);
                }
            }
        }
        return null;
    }

    private static void focusInputFieldAtMouse(TextRenderer tr, String inputKey, double mouseX) {
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

    private static int resolveCursorIndexAtX(TextRenderer tr, String text, FieldLayout layout, double mouseX,
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
                textX = layout.fieldX() + (layout.fieldW() - tr.getWidth(slice.display())) / 2;
            }

            int caretIndex = Math.max(0, Math.min(slice.displayCursor(), slice.display().length()));
            int caretX = textX + tr.getWidth(slice.display().substring(0, caretIndex));
            double distance = Math.abs(mouseX - caretX);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private static String findInputFieldAt(double mouseX, double mouseY) {
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

    private static void updateInputCursorBlink() {
        long now = System.currentTimeMillis();
        if (now - inputCursorBlinkTime > 500) {
            inputCursorVisible = !inputCursorVisible;
            inputCursorBlinkTime = now;
        }
    }

    private static void resetPickerScrollbarState() {
        pickerScrollOffset = 0;
        pickerScrollbarDragging = false;
        pickerScrollbarDragOffset = 0;
        pickerScrollbarVisible = false;
    }

    private static void toggleItemPicker(String fieldKey) {
        if (fieldKey.equals(openPickerKey)) {
            openPickerKey = null;
            resetPickerScrollbarState();
            return;
        }
        openPickerKey = fieldKey;
        resetPickerScrollbarState();
        focusedInputKey = fieldKey;
        ItemViewerUtility.blurSearchFieldFocus();
    }

    private static List<ItemViewerUtility.CostNamePickerEntry> getPickerEntries(String fieldKey) {
        int separator = fieldKey.indexOf('\0');
        if (separator < 0) {
            return List.of();
        }
        String label = fieldKey.substring(separator + 1);
        return switch (label) {
            case "Material" -> ItemViewerUtility.getKnownMaterialPickerEntries();
            case "Ressource" -> ItemViewerUtility.getKnownResourcePickerEntries();
            case "Amboss" -> ItemViewerUtility.getKnownAmbossPickerEntries();
            case "Ofen" -> ItemViewerUtility.getKnownOfenPickerEntries();
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

    private static int clampPickerScrollOffset(int offset) {
        int maxScroll = Math.max(0, pickerDropdownEntries.size() - PICKER_MAX_VISIBLE);
        return Math.max(0, Math.min(offset, maxScroll));
    }

    private static boolean isMouseOverItemPickerDropdown(double mouseX, double mouseY) {
        return mouseX >= pickerDropdownX && mouseX < pickerDropdownX + pickerDropdownW
                && mouseY >= pickerDropdownY && mouseY < pickerDropdownY + pickerDropdownH;
    }

    private static void renderOpenItemPicker(DrawContext context, TextRenderer tr, PanelLayout layout) {
        pickerScrollbarVisible = false;
        if (openPickerKey == null) {
            pickerDropdownEntries = List.of();
            return;
        }
        FilterOption option = findOptionForKey(openPickerKey);
        if (option == null || option.pickerButtonX < 0) {
            return;
        }

        List<ItemViewerUtility.CostNamePickerEntry> entries = getPickerEntries(openPickerKey);
        pickerDropdownEntries = entries;
        if (entries.isEmpty()) {
            int rowY = option.y - scrollOffset;
            int listX = option.pickerButtonX;
            int listY = rowY + ROW_HEIGHT;
            int listW = Math.max(80, tr.getWidth("Keine Einträge") + PICKER_PADDING * 2);
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
            context.drawText(tr, "Keine Einträge", listX + PICKER_PADDING, listY + 3, 0xFFAAAAAA, false);
            return;
        }

        pickerScrollOffset = clampPickerScrollOffset(pickerScrollOffset);
        boolean scrollable = entries.size() > PICKER_MAX_VISIBLE;
        int maxRowWidth = 0;
        for (ItemViewerUtility.CostNamePickerEntry entry : entries) {
            int rowWidth = tr.getWidth(entry.name());
            String floorLabel = entry.floorLabel();
            if (!floorLabel.isEmpty()) {
                rowWidth += PICKER_FLOOR_LABEL_GAP + tr.getWidth(floorLabel);
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
            ItemViewerUtility.CostNamePickerEntry entry = entries.get(entryIndex);
            int optionY = listY + i * PICKER_OPTION_HEIGHT;
            boolean hovered = ItemViewerUtility.getLastMouseX() >= listX
                    && ItemViewerUtility.getLastMouseX() < contentRight
                    && ItemViewerUtility.getLastMouseY() >= optionY
                    && ItemViewerUtility.getLastMouseY() < optionY + PICKER_OPTION_HEIGHT;
            if (hovered) {
                context.fill(listX + 1, optionY, contentRight - 1, optionY + PICKER_OPTION_HEIGHT, 0xFF335588);
            }
            context.drawText(tr, entry.name(), listX + PICKER_PADDING, optionY + 3, entry.textColor(), false);
            String floorLabel = entry.floorLabel();
            if (!floorLabel.isEmpty()) {
                int floorWidth = tr.getWidth(floorLabel);
                context.drawText(tr, floorLabel, contentRight - PICKER_PADDING - floorWidth, optionY + 3,
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
                    && ItemViewerUtility.getLastMouseX() >= trackX
                    && ItemViewerUtility.getLastMouseX() < trackX + PICKER_SCROLLBAR_WIDTH
                    && ItemViewerUtility.getLastMouseY() >= thumbTop
                    && ItemViewerUtility.getLastMouseY() < thumbTop + thumbHeight;
            int thumbColor = pickerScrollbarDragging ? 0xFFB0B0B0 : (thumbHovered ? 0xFFA0A0A0 : 0xFF909090);
            context.fill(trackX + 1, thumbTop, trackX + PICKER_SCROLLBAR_WIDTH - 1, thumbTop + thumbHeight, thumbColor);
        }
    }

    private static int computePickerThumbHeight(int listH, int entryCount) {
        return Math.max(8, (listH - 2) * PICKER_MAX_VISIBLE / entryCount);
    }

    private static void setPickerScrollFromThumbTop(int thumbTop) {
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

    private static boolean handlePickerScrollbarClick(double mouseX, double mouseY) {
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

    private static boolean handleItemPickerDropdownClick(double mouseX, double mouseY) {
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

    /** Kleines Häkchen per Pixel – zuverlässiger als Unicode-Zeichen in der MC-Schrift. */
    private static void drawCheckmark(DrawContext context, int boxX, int boxY) {
        int color = 0xFFFFFFFF;
        context.fill(boxX + 2, boxY + 5, boxX + 3, boxY + 6, color);
        context.fill(boxX + 3, boxY + 6, boxX + 4, boxY + 7, color);
        context.fill(boxX + 4, boxY + 5, boxX + 5, boxY + 6, color);
        context.fill(boxX + 5, boxY + 4, boxX + 6, boxY + 5, color);
        context.fill(boxX + 6, boxY + 3, boxX + 7, boxY + 4, color);
        context.fill(boxX + 7, boxY + 2, boxX + 8, boxY + 3, color);
    }

    private static void drawCenteredButtonLabel(DrawContext context, TextRenderer tr, String label, int x, int y, int width, int height) {
        int textWidth = tr.getWidth(label);
        int textHeight = tr.fontHeight;
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - textHeight) / 2;
        context.drawText(tr, label, textX, textY, 0xFFFFFFFF, false);
    }

    private static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0xFF000000);
        context.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
        context.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        context.fill(x, y, x + 1, y + height, 0xFFFFFFFF);
        context.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
    }

    private static void drawSmallButton(DrawContext context, int x, int y, int width, int height, boolean hovered) {
        context.fill(x, y, x + width, y + height, hovered ? 0xFF404040 : 0x80000000);
        context.fill(x, y, x + width, y + 1, 0xFFFFFFFF);
        context.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        context.fill(x, y, x + 1, y + height, 0xFFFFFFFF);
        context.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
    }

    private static void drawSmallButton(DrawContext context, int x, int y, int size, boolean hovered) {
        drawSmallButton(context, x, y, size, size, hovered);
    }

    private static String capitalizeFirst(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() == 1) {
            return value.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String formatRarityLabel(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "common" -> "Gewöhnlich";
            case "uncommon" -> "Ungewöhnlich";
            case "rare" -> "Selten";
            case "epic" -> "Episch";
            case "legendary" -> "Legendär";
            default -> capitalizeFirst(rarity);
        };
    }

    private static String normalizeTagToken(String token) {
        if (token.startsWith("#")) {
            return token.substring(1).toLowerCase(Locale.ROOT);
        }
        return token.toLowerCase(Locale.ROOT);
    }

    private static final class FilterGroupDef {
        final String title;
        final List<FilterOptionDef> options;

        FilterGroupDef(String title, List<FilterOptionDef> options) {
            this.title = title;
            this.options = options;
        }
    }

    private static final class FilterOptionDef {
        final String label;
        final List<String> tokens;

        FilterOptionDef(String label, String... tokens) {
            this.label = label;
            this.tokens = List.of(tokens);
        }

        boolean matchesQuery(SearchQuery query) {
            for (String token : tokens) {
                if (token.startsWith("+")) {
                    String modifier = token.substring(1);
                    int colon = modifier.indexOf(':');
                    if (colon >= 0) {
                        modifier = modifier.substring(0, colon);
                    }
                    boolean found = false;
                    for (ModifierFilter filter : query.modifierFilters) {
                        if (filter.modifier != null && filter.modifier.equalsIgnoreCase(modifier)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                } else {
                    String tag = normalizeTagToken(token);
                    boolean found = false;
                    for (String queryTag : query.tags) {
                        if (queryTag.equalsIgnoreCase(tag)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private static final class PanelLayout {
        int screenWidth;
        int screenHeight;
        int boxX;
        int boxY;
        int boxWidth;
        int boxHeight;
        int closeX;
        int closeY;
        int contentX;
        int contentTop;
        int contentBottom;
        int contentWidth;
        int contentScrollRight;
        int scrollBarX;
        boolean scrollable;
        int clearX;
        int clearY;
        int clearW;
    }

    private static final class FilterGroup {
        final String title;
        final List<FilterOption> options;
        int columnX;
        int columnWidth;
        int headerY;
        int bottomY;

        FilterGroup(String title, List<FilterOption> options) {
            this.title = title;
            this.options = options;
        }
    }

    private static final class FilterOption {
        final String label;
        final List<String> tokens;
        final RowInputType inputType;
        int x;
        int y;
        int width;
        int countFieldX;
        int countFieldW;
        int pickerButtonX = -1;

        FilterOption(String label, List<String> tokens, RowInputType inputType) {
            this.label = label;
            this.tokens = tokens;
            this.inputType = inputType;
        }
    }
}

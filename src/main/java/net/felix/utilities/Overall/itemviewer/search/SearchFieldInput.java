package net.felix.utilities.Overall.itemviewer.search;

import com.mojang.blaze3d.platform.InputConstants;
import net.felix.utilities.Overall.itemviewer.ui.ViewerPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import static net.felix.utilities.Overall.itemviewer.kit.KitEditorMode.isKitEditorMode;
import static net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline.currentPage;
import static net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline.currentSearch;
import static net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline.scheduleApplyFilters;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.clearSelection;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.cursorPosition;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.hasSelection;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.searchCursorBlinkTime;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.searchCursorVisible;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.searchFieldFocused;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.selectionEnd;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.selectionStart;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.setSearchFieldFocused;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.SEARCH_HEIGHT;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.VIEWER_PADDING;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.calculateViewerPositionForScreen;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getOrCalculateViewerPosition;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getSearchFieldX;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.helpScreenOpen;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.isVisible;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.lastMouseX;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.lastMouseY;

/**
 * Tastatureingaben im Suchfeld (Tippen, Loeschen, Cursorbewegung).
 */
public final class SearchFieldInput {

    private SearchFieldInput() {
    }

    /**
     * Behandelt Tastatur-Input für das Suchfeld (basierend auf SearchBarUtility)
     * @param keyCode GLFW key code
     * @param scanCode GLFW scan code
     * @param modifiers Modifier keys
     * @return true wenn der Input behandelt wurde, false sonst
     */
    public static boolean handleSearchFieldKeyPress(int keyCode, int scanCode, int modifiers) {
        Minecraft client = Minecraft.getInstance();
        if (!canReceiveKeyInput(client)) {
            return false;
        }
        if (!focusIfMouseOverSearch(client)) {
            return false;
        }

        if (!searchInputKeyLogged) {
            SEARCH_INPUT_LOG.info("Viewer search received a key event");
            searchInputKeyLogged = true;
        }
        client.textInputManager().startTextInput(SEARCH_TEXT_INPUT_OWNER);
        searchCursorVisible = true;
        searchCursorBlinkTime = System.currentTimeMillis();

        if (keyCode == InputConstants.KEY_ESCAPE) {
            if (helpScreenOpen) {
                helpScreenOpen = false;
                return true;
            }
            setSearchFieldFocused(false);
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            return true;
        }

        boolean ctrl = net.felix.utilities.Other.keybinding.KeyBindingUtility.isCtrlDown(modifiers)
                && !net.felix.utilities.Other.keybinding.KeyBindingUtility.isAltGrDown(modifiers);
        boolean shift = net.felix.utilities.Other.keybinding.KeyBindingUtility.isShiftDown(modifiers);

        if (ctrl && handleCtrlShortcut(client, keyCode)) {
            return true;
        }
        handleEditingKey(keyCode, shift);
        // Printable text is delivered by CharacterEvent, independently of keyboard layout.
        return true;
    }

    /**
     * Prüft, ob der aktuelle Screen Sucheingaben erlaubt (Container, Inventar, Kit-Editor) und der Viewer sichtbar ist.
     * Setzt andernfalls den Fokus zurück. Chat und andere Screens werden nie abgefangen.
     */
    private static boolean canReceiveKeyInput(Minecraft client) {
        if (client == null || client.gui.screen() == null) {
            setSearchFieldFocused(false);
            return false;
        }
        if (client.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen) {
            setSearchFieldFocused(false);
            return false;
        }
        boolean canUseSearchInput = client.gui.screen() instanceof AbstractContainerScreen<?>
                || client.gui.screen() instanceof InventoryScreen
                || isKitEditorMode()
                || net.felix.utilities.Overall.itemviewer.ui.ViewerState.externalGridPick != null;
        if (!canUseSearchInput) {
            setSearchFieldFocused(false);
            return false;
        }
        if (!isVisible()) {
            setSearchFieldFocused(false);
            return false;
        }
        return true;
    }

    /**
     * Fokussiert das Suchfeld, wenn die Maus darüber ist.
     * @return true, wenn das Suchfeld danach den Fokus hat und die Taste verarbeiten soll
     */
    private static boolean focusIfMouseOverSearch(Minecraft client) {
        ViewerPosition pos;
        if (net.felix.utilities.Overall.itemviewer.ui.ViewerState.externalGridPick != null) {
            pos = net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.calculateViewerPositionBesideCenteredInventory(client.gui.screen());
        } else if (client.gui.screen() instanceof AbstractContainerScreen<?> handledScreen) {
            pos = getOrCalculateViewerPosition(handledScreen, client.gui.screen());
        } else {
            // Für nicht-AbstractContainerScreen Screens (z.B. Spielerinventar)
            pos = calculateViewerPositionForScreen(client.gui.screen());
        }
        int searchX = getSearchFieldX(pos);
        int searchY = pos.viewerY + VIEWER_PADDING;

        boolean mouseOverSearch = lastMouseX >= searchX && lastMouseX < searchX + pos.searchWidth &&
                                 lastMouseY >= searchY && lastMouseY < searchY + SEARCH_HEIGHT;

        if (!mouseOverSearch && !searchFieldFocused) {
            return false;
        }
        if (mouseOverSearch) {
            setSearchFieldFocused(true);
        }
        return searchFieldFocused;
    }

    /** Strg+C/V/A sowie Strg+Backspace/Delete. @return true, wenn die Tastenkombination verarbeitet wurde */
    private static boolean handleCtrlShortcut(Minecraft client, int keyCode) {
        switch (keyCode) {
            case InputConstants.KEY_C -> copyToClipboard(client);
            case InputConstants.KEY_V -> pasteFromClipboard(client);
            case InputConstants.KEY_A -> selectAll();
            case InputConstants.KEY_BACKSPACE -> deleteSearchWord(false);
            case InputConstants.KEY_DELETE -> deleteSearchWord(true);
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void copyToClipboard(Minecraft client) {
        if (hasSelection()) {
            int start = Math.min(selectionStart, selectionEnd);
            int end = Math.max(selectionStart, selectionEnd);
            client.keyboardHandler.setClipboard(currentSearch.substring(start, end));
        } else if (!currentSearch.isEmpty()) {
            client.keyboardHandler.setClipboard(currentSearch);
        }
    }

    private static void pasteFromClipboard(Minecraft client) {
        String clipboardText = client.keyboardHandler.getClipboard();
        if (clipboardText != null && !clipboardText.isEmpty()) {
            for (int i = 0; i < clipboardText.length(); i++) {
                insertCharacter(clipboardText.charAt(i));
            }
        }
    }

    private static void selectAll() {
        if (!currentSearch.isEmpty()) {
            selectionStart = 0;
            selectionEnd = currentSearch.length();
            cursorPosition = currentSearch.length();
        }
    }

    /** Backspace, Entf, Pfeiltasten, Pos1 und Ende. */
    private static void handleEditingKey(int keyCode, boolean shift) {
        switch (keyCode) {
            case InputConstants.KEY_BACKSPACE -> deleteBackward();
            case InputConstants.KEY_DELETE -> deleteForward();
            case InputConstants.KEY_LEFT -> moveCursorLeft(shift);
            case InputConstants.KEY_RIGHT -> moveCursorRight(shift);
            case InputConstants.KEY_HOME -> {
                clearSelection();
                cursorPosition = 0;
            }
            case InputConstants.KEY_END -> {
                clearSelection();
                cursorPosition = currentSearch.length();
            }
            default -> {
            }
        }
    }

    /** Löscht den markierten Text und setzt den Cursor an dessen Anfang. */
    private static void deleteSelection() {
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        currentSearch = currentSearch.substring(0, start) + currentSearch.substring(end);
        cursorPosition = start;
        clearSelection();
    }

    /** Wendet den geänderten Suchtext an und springt zurück zur ersten Seite. */
    private static void applyTextChange() {
        scheduleApplyFilters();
        currentPage = 0;
    }

    private static void deleteBackward() {
        if (hasSelection()) {
            deleteSelection();
            applyTextChange();
        } else if (cursorPosition > 0) {
            currentSearch = currentSearch.substring(0, cursorPosition - 1) + currentSearch.substring(cursorPosition);
            cursorPosition--;
            applyTextChange();
        }
    }

    private static void deleteForward() {
        if (hasSelection()) {
            deleteSelection();
            applyTextChange();
        } else if (cursorPosition < currentSearch.length()) {
            currentSearch = currentSearch.substring(0, cursorPosition) + currentSearch.substring(cursorPosition + 1);
            applyTextChange();
        }
    }

    private static void moveCursorLeft(boolean extendSelection) {
        if (extendSelection) {
            startSelectionIfNone();
            if (cursorPosition > 0) {
                cursorPosition--;
                selectionEnd = cursorPosition;
            }
        } else {
            clearSelection();
            if (cursorPosition > 0) {
                cursorPosition--;
            }
        }
    }

    private static void moveCursorRight(boolean extendSelection) {
        if (extendSelection) {
            startSelectionIfNone();
            if (cursorPosition < currentSearch.length()) {
                cursorPosition++;
                selectionEnd = cursorPosition;
            }
        } else {
            clearSelection();
            if (cursorPosition < currentSearch.length()) {
                cursorPosition++;
            }
        }
    }

    private static void startSelectionIfNone() {
        if (selectionStart == -1) {
            selectionStart = cursorPosition;
            selectionEnd = cursorPosition;
        }
    }
    public static boolean handleSearchFieldCharTyped(net.minecraft.client.input.CharacterEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (!searchFieldFocused || !isVisible() || client == null || client.gui.screen() == null
                || client.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen) return false;
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?>)
                && !isKitEditorMode()
                && net.felix.utilities.Overall.itemviewer.ui.ViewerState.externalGridPick == null) {
            return false;
        }
        if (!searchInputCharacterLogged) {
            SEARCH_INPUT_LOG.info("Viewer search received a character event (allowed={})", event.isAllowedChatCharacter());
            searchInputCharacterLogged = true;
        }
        if (!event.isAllowedChatCharacter()) return true;
        String text = event.codepointAsString();
        for (int i = 0; i < text.length(); i++) insertCharacter(text.charAt(i));
        searchCursorVisible = true;
        searchCursorBlinkTime = System.currentTimeMillis();
        return true;
    }

    /**
     * Fügt ein Zeichen in die Suche ein
     */
    public static void insertCharacter(char character) {
        if (hasSelection()) {
            // Ersetze markierten Text
            int start = Math.min(selectionStart, selectionEnd);
            int end = Math.max(selectionStart, selectionEnd);
            currentSearch = currentSearch.substring(0, start) + character + currentSearch.substring(end);
            cursorPosition = start + 1;
            clearSelection();
        } else {
            // Füge an Cursor-Position ein
            currentSearch = currentSearch.substring(0, cursorPosition) + character + currentSearch.substring(cursorPosition);
            cursorPosition++;
        }
        scheduleApplyFilters();
        currentPage = 0; // Zurück zur ersten Seite
    }

    /**
     * Löscht ein ganzes Wort in der Suchzeile (Strg+Backspace / Strg+Delete).
     * @param forward true = Wort rechts vom Cursor, false = Wort links vom Cursor
     */
    private static void deleteSearchWord(boolean forward) {
        if (hasSelection()) {
            deleteSelection();
        } else if (forward) {
            if (cursorPosition >= currentSearch.length()) {
                return;
            }
            int end = cursorPosition;
            while (end < currentSearch.length() && Character.isWhitespace(currentSearch.charAt(end))) {
                end++;
            }
            while (end < currentSearch.length() && !Character.isWhitespace(currentSearch.charAt(end))) {
                end++;
            }
            if (end > cursorPosition) {
                currentSearch = currentSearch.substring(0, cursorPosition) + currentSearch.substring(end);
            }
        } else {
            if (cursorPosition <= 0) {
                return;
            }
            int start = cursorPosition;
            while (start > 0 && Character.isWhitespace(currentSearch.charAt(start - 1))) {
                start--;
            }
            while (start > 0 && !Character.isWhitespace(currentSearch.charAt(start - 1))) {
                start--;
            }
            if (start < cursorPosition) {
                currentSearch = currentSearch.substring(0, start) + currentSearch.substring(cursorPosition);
                cursorPosition = start;
            }
        }
        applyTextChange();
    }

    /**
     * Berechnet die Cursor-Position basierend auf der Mausklick-Position
     */
    public static int calculateCursorPosition(int clickX, String text, net.minecraft.client.gui.Font textRenderer) {
        if (text.isEmpty()) {
            return 0;
        }
        
        // Finde die beste Position basierend auf der Klick-Position
        int bestPosition = 0;
        int bestDistance = Integer.MAX_VALUE;
        
        for (int i = 0; i <= text.length(); i++) {
            int textWidth = textRenderer.width(text.substring(0, i));
            int distance = Math.abs(clickX - textWidth);
            
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPosition = i;
            }
        }
        
        return bestPosition;
    }

    static final org.slf4j.Logger SEARCH_INPUT_LOG = org.slf4j.LoggerFactory.getLogger("CCLive-SearchInput");

    static boolean searchInputKeyLogged;

    static boolean searchInputCharacterLogged;

    public static final Object SEARCH_TEXT_INPUT_OWNER = new Object();
}

package net.felix.features.itemviewer.search;

import net.felix.features.itemviewer.ui.ViewerPosition;
import net.minecraft.client.Minecraft;

import static net.felix.features.itemviewer.search.SearchFieldInput.SEARCH_INPUT_LOG;
import static net.felix.features.itemviewer.search.SearchFieldInput.SEARCH_TEXT_INPUT_OWNER;
import static net.felix.features.itemviewer.search.SearchFieldInput.searchInputCharacterLogged;
import static net.felix.features.itemviewer.search.SearchFieldInput.searchInputKeyLogged;
import static net.felix.features.itemviewer.ui.ViewerLayout.SEARCH_HEIGHT;
import static net.felix.features.itemviewer.ui.ViewerLayout.VIEWER_PADDING;
import static net.felix.features.itemviewer.ui.ViewerLayout.getSearchFieldX;
import static net.felix.features.itemviewer.ui.ViewerLayout.resolveViewerPositionForMouseInput;
import static net.felix.features.itemviewer.ui.ViewerState.helpScreenOpen;
import static net.felix.features.itemviewer.ui.ViewerState.isMinimized;
import static net.felix.features.itemviewer.ui.ViewerState.isVisible;

/**
 * Zustand des Suchfelds: Fokus, Cursor, Textauswahl.
 */
public final class SearchFieldState {

    private SearchFieldState() {
    }

    // Selektion für Suchfeld
    public static int selectionStart = -1; // Start der Text-Selektion (-1 = keine Selektion)

    public static int selectionEnd = -1;   // Ende der Text-Selektion

    public static int cursorPosition = 0;  // Cursor-Position im Suchfeld

    /**
     * Prüft ob ein Textfeld fokussiert ist (Chat, Inventar-Suchfeld, etc.)
     * Verhindert, dass Hotkeys aktiviert werden, wenn der Spieler tippt
     */
    public static boolean isTextFieldFocused(Minecraft client) {
        if (client == null || client.gui.screen() == null) {
            return false;
        }
        
        // Prüfe ob ChatScreen offen ist
        if (client.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen) {
            return true;
        }
        
        // Prüfe ob unser eigenes Suchfeld fokussiert ist
        if (searchFieldFocused) {
            return true;
        }
        
        // Prüfe ob ein TextFieldWidget fokussiert ist (z.B. in Inventaren)
        // Verwende Reflection um auf fokussierte Widgets zuzugreifen
        try {
            // Prüfe ob Screen ein fokussiertes Widget hat
            java.lang.reflect.Method getFocusedMethod = client.gui.screen().getClass().getMethod("getFocused");
            if (getFocusedMethod != null) {
                Object focused = getFocusedMethod.invoke(client.gui.screen());
                if (focused instanceof net.minecraft.client.gui.components.EditBox) {
                    net.minecraft.client.gui.components.EditBox textField = (net.minecraft.client.gui.components.EditBox) focused;
                    if (textField.isFocused()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Reflection fehlgeschlagen - ignoriere (nicht kritisch)
        }
        
        return false;
    }

    public static boolean isTextFieldFocusedPublic(Minecraft client) {
        return isTextFieldFocused(client);
    }

    /**
     * Prüft ob eine Selektion vorhanden ist
     */
    public static boolean hasSelection() {
        return selectionStart >= 0 && selectionEnd > selectionStart;
    }

    /**
     * Löscht die aktuelle Text-Selektion
     */
    public static void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }

    // Suchfeld-Fokus
    public static boolean searchFieldFocused = false;

    public static long searchCursorBlinkTime = 0;

    public static boolean searchCursorVisible = true;

    public static void setSearchFieldFocused(boolean focused) {
        if (focused && !searchFieldFocused) {
            searchInputKeyLogged = false;
            searchInputCharacterLogged = false;
            SEARCH_INPUT_LOG.info("Viewer search focused; enabling SDL text input");
        }
        searchFieldFocused = focused;
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        // SDL delivers character events only while a text input owner is active.
        if (focused) {
            client.textInputManager().startTextInput(SEARCH_TEXT_INPUT_OWNER);
        } else {
            client.textInputManager().stopTextInput(SEARCH_TEXT_INPUT_OWNER);
        }
    }

    public static boolean isSearchFieldFocused() {
        return searchFieldFocused;
    }

    public static void blurSearchFieldFocus() {
        setSearchFieldFocused(false);
        searchCursorVisible = false;
        clearSelection();
    }

    public static void blurSearchFieldFocusUnlessClickOnField(double mouseX, double mouseY, int button) {
        if (!searchFieldFocused) {
            return;
        }
        if (helpScreenOpen || !isVisible() || isMinimized) {
            blurSearchFieldFocus();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            blurSearchFieldFocus();
            return;
        }

        ViewerPosition pos = resolveViewerPositionForMouseInput(client);
        if (pos == null) {
            blurSearchFieldFocus();
            return;
        }

        int searchX = getSearchFieldX(pos);
        int searchY = pos.viewerY + VIEWER_PADDING;
        boolean onSearchField = mouseX >= searchX && mouseX < searchX + pos.searchWidth
                && mouseY >= searchY && mouseY < searchY + SEARCH_HEIGHT;
        if (!onSearchField) {
            blurSearchFieldFocus();
        }
    }
}

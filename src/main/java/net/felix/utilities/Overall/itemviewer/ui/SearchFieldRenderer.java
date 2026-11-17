package net.felix.utilities.Overall.itemviewer.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline.currentSearch;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldInput.SEARCH_TEXT_INPUT_OWNER;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.cursorPosition;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.hasSelection;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.searchCursorBlinkTime;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.searchCursorVisible;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.searchFieldFocused;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.selectionEnd;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.selectionStart;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.SEARCH_HEIGHT;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.lastMouseX;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.lastMouseY;

/**
 * Zeichnet das Suchfeld mit Cursor und Textauswahl.
 */
public final class SearchFieldRenderer {

    private SearchFieldRenderer() {
    }

    /**
     * Rendert Text mit Auswahl-Highlighting
     */
    public static void renderTextWithSelection(GuiGraphicsExtractor context, Minecraft client, String text, int x, int y, int textColor, int maxWidth) {
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        
        // Zeichne Text vor der Auswahl
        if (start > 0) {
            String beforeSelection = text.substring(0, start);
            context.text(client.font, Component.literal(beforeSelection), x, y, textColor, false);
        }
        
        // Berechne Position für die Auswahl
        int selectionX = x + client.font.width(text.substring(0, start));
        String selectedText = text.substring(start, end);
        int selectionWidth = client.font.width(selectedText);
        
        // Zeichne Auswahl-Hintergrund
        context.fill(selectionX, y - 1, selectionX + selectionWidth, y + 9, 0xFF0078D4); // Blauer Auswahl-Hintergrund
        
        // Zeichne ausgewählten Text (weiß auf blau)
        context.text(client.font, Component.literal(selectedText), selectionX, y, 0xFFFFFFFF, false);
        
        // Zeichne Text nach der Auswahl
        if (end < text.length()) {
            String afterSelection = text.substring(end);
            int afterX = selectionX + selectionWidth;
            context.text(client.font, Component.literal(afterSelection), afterX, y, textColor, false);
        }
    }

    /**
     * Rendert das Suchfeld
     */
    static void renderSearchField(GuiGraphicsExtractor context, int x, int y, int width) {
        Minecraft client = Minecraft.getInstance();

        if (searchFieldFocused) {
            client.textInputManager().startTextInput(SEARCH_TEXT_INPUT_OWNER);
            client.textInputManager().setTextInputArea(x, y, x + width, y + SEARCH_HEIGHT);
            long currentTime = System.currentTimeMillis();
            if (currentTime - searchCursorBlinkTime > 500) {
                searchCursorVisible = !searchCursorVisible;
                searchCursorBlinkTime = currentTime;
            }
        } else {
            searchCursorVisible = false;
        }
        
        // Hintergrund (heller wenn fokussiert)
        int bgColor = searchFieldFocused ? 0xFF1A1A1A : 0xFF000000;
        context.fill(x, y, x + width, y + SEARCH_HEIGHT, bgColor);
        
        // Rahmen (heller wenn fokussiert)
        int borderColor = searchFieldFocused ? 0xFFFFFFFF : 0xFF808080;
        context.fill(x, y, x + width, y + 1, borderColor); // Oben
        context.fill(x, y + SEARCH_HEIGHT - 1, x + width, y + SEARCH_HEIGHT, borderColor); // Unten
        context.fill(x, y, x + 1, y + SEARCH_HEIGHT, borderColor); // Links
        context.fill(x + width - 1, y, x + width, y + SEARCH_HEIGHT, borderColor); // Rechts
        
        // Text
        String searchText = currentSearch.isEmpty() ? "Suche..." : currentSearch;
        int textColor = currentSearch.isEmpty() ? 0xFF808080 : 0xFFFFFFFF;
        
        // Text kürzen wenn zu lang
        int maxTextWidth = width - 6; // 3px Padding links + 3px rechts
        String displayText = searchText;
        boolean isTextTruncated = false;
        if (client.font.width(displayText) > maxTextWidth) {
            // Kürze Text von hinten
            String originalText = displayText;
            while (client.font.width(displayText + "...") > maxTextWidth && displayText.length() > 0) {
                displayText = displayText.substring(0, displayText.length() - 1);
            }
            displayText += "...";
            // Prüfe ob Text tatsächlich gekürzt wurde (nicht nur "..." hinzugefügt)
            isTextTruncated = !displayText.equals(originalText + "...");
        }
        
        // Rendere Text mit Selektion
        if (hasSelection() && searchFieldFocused && !currentSearch.isEmpty()) {
            renderTextWithSelection(context, client, currentSearch, x + 3, y + 6, textColor, width - 6);
        } else {
            context.text(client.font, Component.literal(displayText), x + 3, y + 6, textColor, false);
        }
        
        // Cursor anzeigen wenn fokussiert (nur wenn keine Selektion)
        if (searchFieldFocused && !hasSelection() && searchCursorVisible) {
            int cursorX = x + 3;
            if (cursorPosition > 0 && cursorPosition <= currentSearch.length()) {
                cursorX += client.font.width(currentSearch.substring(0, cursorPosition));
            }
            context.fill(cursorX, y + 4, cursorX + 1, y + SEARCH_HEIGHT - 4, 0xFFFFFFFF);
        }
        
        // Tooltip anzeigen wenn Text abgeschnitten wurde und Maus über Suchleiste ist
        if (isTextTruncated && !currentSearch.isEmpty()) {
            boolean isHovered = lastMouseX >= x && lastMouseX < x + width &&
                               lastMouseY >= y && lastMouseY < y + SEARCH_HEIGHT;
            if (isHovered) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal(currentSearch));
                // Minecraft's drawTooltip passt automatisch die Position an
                context.setComponentTooltipForNextFrame(client.font, tooltip, lastMouseX, lastMouseY);
            }
        }
    }
}

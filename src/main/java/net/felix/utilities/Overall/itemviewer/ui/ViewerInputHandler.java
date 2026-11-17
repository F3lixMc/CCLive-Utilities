package net.felix.utilities.Overall.itemviewer.ui;

import com.google.gson.JsonObject;
import net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenu;
import net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuInput;
import net.felix.utilities.Overall.itemviewer.inventory.FavoriteBlueprintsStore;
import net.felix.utilities.Overall.itemviewer.inventory.HudStatsReader;
import net.felix.utilities.Overall.itemviewer.inventory.InventoryFilter;
import net.felix.utilities.Overall.itemviewer.search.SortMode;
import net.felix.utilities.Overall.itemviewer.tooltip.TooltipCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import static net.felix.utilities.Overall.itemviewer.data.ItemCatalog.allItems;
import static net.felix.utilities.Overall.itemviewer.inventory.FoundStatus.blueprintJsonMap;
import static net.felix.utilities.Overall.itemviewer.kit.KitEditorMode.KIT_BUTTON_INDEX;
import static net.felix.utilities.Overall.itemviewer.kit.KitEditorMode.KIT_BUTTON_SIZE;
import static net.felix.utilities.Overall.itemviewer.kit.KitEditorMode.isKitEditorMode;
import static net.felix.utilities.Overall.itemviewer.kit.KitEditorMode.kitEditorBackgroundScreen;
import static net.felix.utilities.Overall.itemviewer.kit.KitEditorMode.kitEditorPickHandler;
import static net.felix.utilities.Overall.itemviewer.kit.KitEditorMode.kitEditorPickTarget;
import static net.felix.utilities.Overall.itemviewer.kit.KitEditorMode.kitFilterActive;
import static net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline.applyFilters;
import static net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline.currentPage;
import static net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline.currentSearch;
import static net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline.filteredItems;
import static net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline.setSortMode;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldInput.calculateCursorPosition;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldInput.insertCharacter;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.blurSearchFieldFocus;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.clearSelection;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.cursorPosition;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.searchCursorBlinkTime;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.searchCursorVisible;
import static net.felix.utilities.Overall.itemviewer.search.SearchFieldState.setSearchFieldFocused;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.FILTER_BUTTON_SIZE;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.HELP_BUTTON_SIZE;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.MINIMIZE_BUTTON_SIZE;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.PAGINATION_HEIGHT;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.SEARCH_HEIGHT;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.SORT_DROPDOWN_HEIGHT;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.SORT_OPTION_HEIGHT;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.SYMBOL_BUTTON_SIZE;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.VIEWER_PADDING;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getMinimizeButtonY;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getSearchFieldX;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getSideButtonX;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getViewerSettingsButtonY;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getViewerSettingsCostsButtonX;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getViewerSettingsMenuWidth;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getViewerSettingsOwnedButtonX;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.resolveAbstractContainerScreenForInput;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.resolveViewerPositionForMouseInput;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.activeCategoryOverlay;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.favoritesMode;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.helpScreenHovered;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.helpScreenOpen;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.helpScreenScrollOffset;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.hoveredItemForClick;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.isMinimized;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.isSymbolMenuOpen;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.isViewerSettingsMenuOpen;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.isVisible;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.showCosts;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.showOwnedResources;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.sortDropdownOpen;

/**
 * Maus-Eingaben im Item Viewer: Klicks und Scrollen.
 */
public final class ViewerInputHandler {

    private ViewerInputHandler() {
    }

    /** Prüft, ob die Maus im Rechteck (x, y, width, height) liegt; rechte und untere Kante zählen nicht dazu. */
    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** Anzahl der Seiten für die aktuell gefilterten Items bei der aktuellen Viewer-Größe (mindestens 1). */
    private static int computeTotalPages(ViewerPosition pos) {
        int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
        int gridAvailableWidth = pos.viewerWidth - VIEWER_PADDING * 2;
        int gridAvailableHeight = paginationY - (pos.viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5 + SORT_DROPDOWN_HEIGHT + 5);
        int itemsPerPage = ItemViewerGrid.computeItemsPerPage(gridAvailableWidth, gridAvailableHeight);
        int totalPages = (int) Math.ceil((double) filteredItems.size() / (double) itemsPerPage);
        return totalPages == 0 ? 1 : totalPages;
    }

    /**
     * Behandelt Mausrad-Scrolling (wird vom Mixin aufgerufen)
     * @param mouseX Maus X-Position
     * @param mouseY Maus Y-Position
     * @param vertical Scroll-Delta (positiv = runter, negativ = hoch)
     * @return true wenn das Scroll-Event behandelt wurde, false sonst
     */
    public static boolean handleMouseScroll(double mouseX, double mouseY, double vertical) {
        if (FilterMenuInput.handleScroll(mouseX, mouseY, vertical)) {
            return true;
        }
        // Prüfe zuerst ob Hilfe-Overlay geöffnet ist und Maus darüber ist
        if (helpScreenOpen && helpScreenHovered) {
            int scrollAmount = (int) (vertical * 12);
            helpScreenScrollOffset -= scrollAmount;
            helpScreenScrollOffset = Math.max(0, helpScreenScrollOffset);
            return true;
        }

        if (!isVisible()) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        ViewerPosition pos = resolveViewerPositionForMouseInput(client);
        if (pos == null) {
            return false;
        }

        if (!isInside(mouseX, mouseY, pos.viewerX, pos.viewerY, pos.viewerWidth, pos.viewerHeight)) {
            return false;
        }

        int totalPages = computeTotalPages(pos);
        if (vertical > 0) {
            // Hoch scrollen = vorherige Seite
            if (currentPage > 0) {
                currentPage--;
                return true;
            }
        } else if (vertical < 0) {
            // Runter scrollen = nächste Seite
            if (currentPage < totalPages - 1) {
                currentPage++;
                return true;
            }
        }
        return false;
    }

    /**
     * Behandelt Maus-Klicks (wird vom Mixin aufgerufen).
     * Die Reihenfolge der Handler entspricht der Klick-Priorität: das zuerst zuständige Element verbraucht den Klick.
     */
    public static boolean handleMouseClick(double mouseX, double mouseY, int button) {
        Minecraft client = Minecraft.getInstance();

        // Minimierter Button (rechts unten) funktioniert auch ohne Screen
        if (handleMinimizedButtonClick(client, mouseX, mouseY, button)) {
            return true;
        }

        // Wenn minimiert, keine weiteren Klicks abfangen, damit die Buttons darunter sie verarbeiten können
        if (isMinimized && isVisible) {
            return false;
        }

        if (!isClickTargetScreenAvailable(client)) {
            return false;
        }

        // Hilfe-Overlay hat höchste Priorität und verbraucht alle Klicks, solange es geöffnet ist
        if (helpScreenOpen) {
            handleHelpOverlayClick(client, mouseX, mouseY, button);
            return true;
        }

        ViewerPosition pos = resolveViewerPositionForMouseInput(client);
        if (pos == null) {
            return false;
        }

        if (handleSideButtonsClick(pos, mouseX, mouseY, button)) {
            return true;
        }
        // Symbol-Menü vor der Suchleiste, damit Klicks auf Menü-Buttons funktionieren
        if (handleSymbolMenuClick(pos, mouseX, mouseY, button)) {
            return true;
        }
        if (handleSearchFieldClick(client, pos, mouseX, mouseY, button)) {
            return true;
        }
        if (handleFavoriteBlueprintClick(pos, mouseX, mouseY, button)) {
            return true;
        }
        if (handleToolbarButtonClick(pos, mouseX, mouseY, button)) {
            return true;
        }
        if (handleSortDropdownListClick(pos, mouseX, mouseY, button)) {
            return true;
        }
        if (handlePaginationClick(client, pos, mouseX, mouseY, button)) {
            return true;
        }
        return handleKitEditorPickClick(pos, mouseX, mouseY, button);
    }

    /** Klick auf den Maximieren-Button, solange der Viewer minimiert ist. */
    private static boolean handleMinimizedButtonClick(Minecraft client, double mouseX, double mouseY, int button) {
        if (!(isMinimized && isVisible && button == 0)) {
            return false;
        }
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int buttonX = screenWidth - MINIMIZE_BUTTON_SIZE - 10;
        int buttonY = screenHeight - MINIMIZE_BUTTON_SIZE - 10;
        if (isInside(mouseX, mouseY, buttonX, buttonY, MINIMIZE_BUTTON_SIZE, MINIMIZE_BUTTON_SIZE)) {
            isMinimized = false;
            return true;
        }
        return false;
    }

    /**
     * Prüft, ob es einen Screen gibt, auf dem der Viewer Klicks verarbeiten darf.
     * Im Kit-Editor ohne Hintergrund-Screen genügt irgendein offener Screen.
     */
    private static boolean isClickTargetScreenAvailable(Minecraft client) {
        if (!isKitEditorMode() || kitEditorBackgroundScreen != null) {
            AbstractContainerScreen<?> handledScreen = resolveAbstractContainerScreenForInput(client);
            if (handledScreen == null) {
                return false;
            }
            String currentTitle = handledScreen.getTitle().getString();
            return isKitEditorMode()
                    || !net.felix.utilities.Other.glyphs.ZeichenUtility.shouldHideItemViewerForInventoryTitle(currentTitle)
                    || helpScreenOpen;
        }
        return client.gui.screen() != null;
    }

    /** Klick bei geöffnetem Hilfe-Overlay: Schließen-Button oder Klick außerhalb schließt es (nur Linksklick). */
    private static void handleHelpOverlayClick(Minecraft client, double mouseX, double mouseY, int button) {
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int boxWidth = Math.min(400, screenWidth - 40);
        int boxHeight = Math.min(450, screenHeight - 40);
        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = (screenHeight - boxHeight) / 2;
        int closeButtonX = boxX + boxWidth - 20;
        int closeButtonY = boxY + 5;
        int closeButtonSize = 15;

        if (button != 0) {
            return;
        }
        boolean onCloseButton = isInside(mouseX, mouseY, closeButtonX, closeButtonY, closeButtonSize, closeButtonSize);
        boolean outsideBox = !isInside(mouseX, mouseY, boxX, boxY, boxWidth, boxHeight);
        if (onCloseButton || outsideBox) {
            helpScreenOpen = false;
        }
    }

    /** Viewer-Einstellungen-Menü (Kosten/Besitz-Schalter) und Einklappen-Button neben dem Viewer. */
    private static boolean handleSideButtonsClick(ViewerPosition pos, double mouseX, double mouseY, int button) {
        if (isMinimized || button != 0) {
            return false;
        }
        int sideButtonX = getSideButtonX(pos);
        int settingsButtonY = getViewerSettingsButtonY(pos);
        int menuWidth = getViewerSettingsMenuWidth();
        int menuX = sideButtonX - menuWidth;
        int costsButtonX = getViewerSettingsCostsButtonX(menuX);
        int ownedButtonX = getViewerSettingsOwnedButtonX(menuX);

        if (isViewerSettingsMenuOpen) {
            if (isInside(mouseX, mouseY, costsButtonX, settingsButtonY, MINIMIZE_BUTTON_SIZE, MINIMIZE_BUTTON_SIZE)) {
                showCosts = !showCosts;
                TooltipCache.invalidateTooltipCache();
                return true;
            }
            if (isInside(mouseX, mouseY, ownedButtonX, settingsButtonY, MINIMIZE_BUTTON_SIZE, MINIMIZE_BUTTON_SIZE)) {
                showOwnedResources = !showOwnedResources;
                if (!showOwnedResources) {
                    HudStatsReader.clear();
                }
                TooltipCache.invalidateTooltipCache();
                return true;
            }
            // Klick irgendwo im Menü verbraucht den Klick
            int menuAreaWidth = sideButtonX + MINIMIZE_BUTTON_SIZE - menuX;
            if (isInside(mouseX, mouseY, menuX, settingsButtonY, menuAreaWidth, MINIMIZE_BUTTON_SIZE)) {
                return true;
            }
        }

        int minimizeButtonY = getMinimizeButtonY(pos);
        if (isInside(mouseX, mouseY, sideButtonX, minimizeButtonY, MINIMIZE_BUTTON_SIZE, MINIMIZE_BUTTON_SIZE)) {
            isMinimized = true;
            return true;
        }
        return false;
    }

    /**
     * Symbol-Menü (@, &lt;, &gt;) links vom Symbol-Button (nur Linksklick, nur wenn geöffnet).
     * Ein Klick außerhalb schließt das Menü, wird aber nicht verbraucht.
     */
    private static boolean handleSymbolMenuClick(ViewerPosition pos, double mouseX, double mouseY, int button) {
        if (!(button == 0 && isSymbolMenuOpen)) {
            return false;
        }
        char[] symbols = {'@', '<', '>'};
        int menuButtonWidth = 20;
        int menuButtonHeight = SYMBOL_BUTTON_SIZE; // Gleiche Höhe wie Stern-Button
        int menuWidth = menuButtonWidth * symbols.length + 2;
        int menuX = pos.symbolButtonX - menuWidth; // Links vom Button
        int menuY = pos.symbolButtonY;

        boolean withinMenuRows = mouseY >= menuY && mouseY <= menuY + menuButtonHeight;
        for (int i = 0; i < symbols.length; i++) {
            int buttonLeft = menuX + (menuButtonWidth + 1) * i;
            if (mouseX >= buttonLeft && mouseX <= buttonLeft + menuButtonWidth && withinMenuRows) {
                insertCharacter(symbols[i]);
                isSymbolMenuOpen = false;
                return true;
            }
        }

        // Klick im Menü-Bereich, aber nicht auf einem Button - verhindere andere Klicks
        if (mouseX >= menuX && mouseX <= pos.symbolButtonX + SYMBOL_BUTTON_SIZE && withinMenuRows) {
            return true;
        }

        // Wenn außerhalb des Menüs geklickt wird, schließe es
        if (mouseX < menuX || mouseX >= pos.symbolButtonX + SYMBOL_BUTTON_SIZE
                || mouseY < menuY || mouseY >= menuY + menuButtonHeight) {
            isSymbolMenuOpen = false;
        }
        return false;
    }

    /** Linksklick setzt Fokus und Cursor im Suchfeld, Rechtsklick leert es. */
    private static boolean handleSearchFieldClick(Minecraft client, ViewerPosition pos, double mouseX, double mouseY, int button) {
        int searchX = getSearchFieldX(pos);
        int searchY = pos.viewerY + VIEWER_PADDING;
        if (!isInside(mouseX, mouseY, searchX, searchY, pos.searchWidth, SEARCH_HEIGHT)) {
            return false;
        }
        if (button == 0) {
            setSearchFieldFocused(true);
            searchCursorVisible = true;
            searchCursorBlinkTime = System.currentTimeMillis();
            // Cursor-Position aus der Klick-Position berechnen
            int clickX = (int) (mouseX - searchX - 3); // 3px Padding links
            cursorPosition = calculateCursorPosition(clickX, currentSearch, client.font);
            clearSelection(); // Auswahl löschen beim Klick
            return true;
        }
        if (button == 1) {
            currentSearch = "";
            cursorPosition = 0;
            clearSelection();
            applyFilters();
            return true;
        }
        return false;
    }

    /** Rechtsklick auf einen Blueprint im Grid schaltet seinen Favoriten-Status um. */
    private static boolean handleFavoriteBlueprintClick(ViewerPosition pos, double mouseX, double mouseY, int button) {
        if (!(button == 1 && hoveredItemForClick != null)) {
            return false;
        }
        int gridX = pos.viewerX + VIEWER_PADDING;
        int gridY = pos.dropdownY + SORT_DROPDOWN_HEIGHT + 5;
        int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
        int gridAvailableHeight = paginationY - gridY;
        int slotSize = ItemViewerGrid.getSlotSize();
        int gridAvailableWidth = pos.viewerWidth - VIEWER_PADDING * 2;
        int gridColumns = Math.max(1, gridAvailableWidth / slotSize);
        int gridRows = Math.max(1, gridAvailableHeight / slotSize);
        if (!isInside(mouseX, mouseY, gridX, gridY, gridColumns * slotSize, gridRows * slotSize)) {
            return false;
        }

        if (!net.felix.utilities.Overall.itemviewer.data.ItemData.isFavoritable(hoveredItemForClick)) {
            return false;
        }

        toggleFavorite();
        TooltipCache.invalidateTooltipCache();
        if (favoritesMode) {
            applyFilters();
        }
        return true;
    }

    /** Schaltet den Favoriten-Status des aktuell gehoverten Blueprints um. */
    private static void toggleFavorite() {
        if (FavoriteBlueprintsStore.isFavorite(hoveredItemForClick.name)) {
            FavoriteBlueprintsStore.removeFavorite(hoveredItemForClick.name);
            return;
        }
        JsonObject jsonObj = hoveredItemForClick.jsonObject;
        if (jsonObj == null) {
            // Fallback: Hole aus Map
            jsonObj = blueprintJsonMap.get(hoveredItemForClick.name);
        }
        if (jsonObj != null) {
            FavoriteBlueprintsStore.addFavorite(jsonObj);
        }
    }

    /** Hilfe-, Filter-, Symbol-, Favoriten-, Kit-, Anwenden- und Sortier-Dropdown-Button. */
    private static boolean handleToolbarButtonClick(ViewerPosition pos, double mouseX, double mouseY, int button) {
        if (button == 0 && isInside(mouseX, mouseY, pos.helpButtonX, pos.helpButtonY, HELP_BUTTON_SIZE, HELP_BUTTON_SIZE)) {
            helpScreenOpen = !helpScreenOpen;
            blurSearchFieldFocus();
            if (helpScreenOpen) {
                FilterMenu.close();
            }
            return true;
        }

        if (button == 0 && isInside(mouseX, mouseY, pos.filterButtonX, pos.filterButtonY, FILTER_BUTTON_SIZE, FILTER_BUTTON_SIZE)) {
            FilterMenu.toggle(allItems, currentSearch);
            blurSearchFieldFocus();
            if (FilterMenu.isOpen()) {
                helpScreenOpen = false;
                activeCategoryOverlay = null;
            }
            return true;
        }

        if (button == 0 && isInside(mouseX, mouseY, pos.symbolButtonX, pos.symbolButtonY, SYMBOL_BUTTON_SIZE, SYMBOL_BUTTON_SIZE)) {
            isSymbolMenuOpen = !isSymbolMenuOpen;
            return true;
        }

        if (button == 0 && isInside(mouseX, mouseY, pos.favoritesButtonX, pos.favoritesButtonY, SORT_DROPDOWN_HEIGHT, SORT_DROPDOWN_HEIGHT)) {
            favoritesMode = !favoritesMode;
            applyFilters(); // Aktualisiere Filter
            return true;
        }

        if (handleKitButtonClick(pos, mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0 && isInside(mouseX, mouseY, pos.applyButtonX, pos.applyButtonY, pos.applyButtonWidth, SORT_DROPDOWN_HEIGHT)) {
            InventoryFilter.toggleFilter();
            return true;
        }

        if (button == 0 && isInside(mouseX, mouseY, pos.dropdownX, pos.dropdownY, pos.dropdownWidth, SORT_DROPDOWN_HEIGHT)) {
            sortDropdownOpen = !sortDropdownOpen;
            return true;
        }
        return false;
    }

    /** Linksklick schaltet den Kit-Filter um (oder öffnet die Kit-Auswahl), Rechtsklick öffnet die Kit-Auswahl. */
    private static boolean handleKitButtonClick(ViewerPosition pos, double mouseX, double mouseY, int button) {
        if (!isInside(mouseX, mouseY, pos.kitButtonX, pos.kitButtonY, KIT_BUTTON_SIZE, KIT_BUTTON_SIZE)) {
            return false;
        }
        if (button == 0) {
            net.felix.utilities.Town.kits.filter.KitFilterUtility.KitSelection kitSelection =
                    net.felix.utilities.Town.kits.filter.KitFilterUtility.getKitSelection(KIT_BUTTON_INDEX);
            if (kitSelection != null) {
                kitFilterActive = !kitFilterActive;
                applyFilters(); // Aktualisiere Filter
            } else {
                // Kein Kit ausgewählt - öffne Kit-Auswahlmenü
                net.felix.utilities.Town.kits.filter.KitFilterUtility.openKitSelectionScreen(KIT_BUTTON_INDEX);
            }
            return true;
        }
        if (button == 1) {
            net.felix.utilities.Town.kits.filter.KitFilterUtility.openKitSelectionScreen(KIT_BUTTON_INDEX);
            return true;
        }
        return false;
    }

    /**
     * Optionen der geöffneten Sortier-Liste (nur Linksklick).
     * Ein Klick außerhalb der Liste schließt sie, wird aber nicht verbraucht.
     */
    private static boolean handleSortDropdownListClick(ViewerPosition pos, double mouseX, double mouseY, int button) {
        if (!(button == 0 && sortDropdownOpen)) {
            return false;
        }
        int dropdownListY = pos.dropdownY + SORT_DROPDOWN_HEIGHT;
        int optionIndex = 0;
        for (SortMode mode : SortMode.values()) {
            int optionY = dropdownListY + optionIndex * SORT_OPTION_HEIGHT;
            if (isInside(mouseX, mouseY, pos.dropdownX, optionY, pos.dropdownWidth, SORT_OPTION_HEIGHT)) {
                setSortMode(mode);
                sortDropdownOpen = false;
                return true;
            }
            optionIndex++;
        }

        if (mouseX < pos.dropdownX || mouseX >= pos.dropdownX + pos.dropdownWidth
                || mouseY < pos.dropdownY || mouseY >= dropdownListY + SortMode.values().length * SORT_OPTION_HEIGHT) {
            sortDropdownOpen = false;
        }
        return false;
    }

    /** "Zurück"- und "Weiter"-Button der Seitenleiste (nur Linksklick, nur wenn die Seite wechseln kann). */
    private static boolean handlePaginationClick(Minecraft client, ViewerPosition pos, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
        int paginationX = pos.viewerX + VIEWER_PADDING;
        int paginationWidth = pos.viewerWidth - VIEWER_PADDING * 2;
        int totalPages = computeTotalPages(pos);

        // Button-Breite dynamisch (gleiche Logik wie beim Rendering)
        int prevTextWidth = client.font.width("< Zurück");
        int nextTextWidth = client.font.width("Weiter >");
        int buttonWidth = Math.max(prevTextWidth, nextTextWidth) + 10; // 10px Padding

        int prevX = paginationX;
        if (isInside(mouseX, mouseY, prevX, paginationY, buttonWidth, PAGINATION_HEIGHT) && currentPage > 0) {
            currentPage--;
            return true;
        }

        int nextX = paginationX + paginationWidth - buttonWidth;
        if (isInside(mouseX, mouseY, nextX, paginationY, buttonWidth, PAGINATION_HEIGHT) && currentPage < totalPages - 1) {
            currentPage++;
            return true;
        }
        return false;
    }

    /** Kit-Editor: Linksklick auf ein Item im Grid übernimmt Item/Icon. */
    private static boolean handleKitEditorPickClick(ViewerPosition pos, double mouseX, double mouseY, int button) {
        if (!(isKitEditorMode() && button == 0 && hoveredItemForClick != null && kitEditorPickHandler != null)) {
            return false;
        }
        int gridX = pos.viewerX + VIEWER_PADDING;
        int gridY = pos.dropdownY + SORT_DROPDOWN_HEIGHT + 5;
        int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
        int gridWidth = pos.viewerWidth - VIEWER_PADDING * 2;
        int gridHeight = paginationY - gridY;
        if (isInside(mouseX, mouseY, gridX, gridY, gridWidth, gridHeight)) {
            kitEditorPickHandler.accept(hoveredItemForClick, kitEditorPickTarget);
            return true;
        }
        return false;
    }
}
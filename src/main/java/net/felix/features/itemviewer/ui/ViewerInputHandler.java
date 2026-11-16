package net.felix.features.itemviewer.ui;

import com.google.gson.JsonObject;
import net.felix.features.itemviewer.inventory.FavoriteBlueprintsStore;
import net.felix.features.itemviewer.inventory.HudStatsReader;
import net.felix.features.itemviewer.inventory.InventoryFilter;
import net.felix.features.itemviewer.search.SortMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import static net.felix.features.itemviewer.data.ItemCatalog.allItems;
import static net.felix.features.itemviewer.inventory.FoundStatus.blueprintJsonMap;
import static net.felix.features.itemviewer.kit.KitEditorMode.KIT_BUTTON_INDEX;
import static net.felix.features.itemviewer.kit.KitEditorMode.KIT_BUTTON_SIZE;
import static net.felix.features.itemviewer.kit.KitEditorMode.isKitEditorMode;
import static net.felix.features.itemviewer.kit.KitEditorMode.kitEditorBackgroundScreen;
import static net.felix.features.itemviewer.kit.KitEditorMode.kitEditorPickHandler;
import static net.felix.features.itemviewer.kit.KitEditorMode.kitEditorPickTarget;
import static net.felix.features.itemviewer.kit.KitEditorMode.kitFilterActive;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.applyFilters;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.currentPage;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.currentSearch;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.filteredItems;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.setSortMode;
import static net.felix.features.itemviewer.search.SearchFieldInput.calculateCursorPosition;
import static net.felix.features.itemviewer.search.SearchFieldInput.insertCharacter;
import static net.felix.features.itemviewer.search.SearchFieldState.blurSearchFieldFocus;
import static net.felix.features.itemviewer.search.SearchFieldState.clearSelection;
import static net.felix.features.itemviewer.search.SearchFieldState.cursorPosition;
import static net.felix.features.itemviewer.search.SearchFieldState.searchCursorBlinkTime;
import static net.felix.features.itemviewer.search.SearchFieldState.searchCursorVisible;
import static net.felix.features.itemviewer.search.SearchFieldState.setSearchFieldFocused;
import static net.felix.features.itemviewer.ui.ViewerLayout.FILTER_BUTTON_SIZE;
import static net.felix.features.itemviewer.ui.ViewerLayout.HELP_BUTTON_SIZE;
import static net.felix.features.itemviewer.ui.ViewerLayout.MINIMIZE_BUTTON_SIZE;
import static net.felix.features.itemviewer.ui.ViewerLayout.PAGINATION_HEIGHT;
import static net.felix.features.itemviewer.ui.ViewerLayout.SEARCH_HEIGHT;
import static net.felix.features.itemviewer.ui.ViewerLayout.SORT_DROPDOWN_HEIGHT;
import static net.felix.features.itemviewer.ui.ViewerLayout.SORT_OPTION_HEIGHT;
import static net.felix.features.itemviewer.ui.ViewerLayout.SYMBOL_BUTTON_SIZE;
import static net.felix.features.itemviewer.ui.ViewerLayout.VIEWER_PADDING;
import static net.felix.features.itemviewer.ui.ViewerLayout.getMinimizeButtonY;
import static net.felix.features.itemviewer.ui.ViewerLayout.getSearchFieldX;
import static net.felix.features.itemviewer.ui.ViewerLayout.getSideButtonX;
import static net.felix.features.itemviewer.ui.ViewerLayout.getViewerSettingsButtonY;
import static net.felix.features.itemviewer.ui.ViewerLayout.getViewerSettingsCostsButtonX;
import static net.felix.features.itemviewer.ui.ViewerLayout.getViewerSettingsMenuWidth;
import static net.felix.features.itemviewer.ui.ViewerLayout.getViewerSettingsOwnedButtonX;
import static net.felix.features.itemviewer.ui.ViewerLayout.resolveAbstractContainerScreenForInput;
import static net.felix.features.itemviewer.ui.ViewerLayout.resolveViewerPositionForMouseInput;
import static net.felix.features.itemviewer.ui.ViewerState.activeCategoryOverlay;
import static net.felix.features.itemviewer.ui.ViewerState.favoritesMode;
import static net.felix.features.itemviewer.ui.ViewerState.helpScreenHovered;
import static net.felix.features.itemviewer.ui.ViewerState.helpScreenOpen;
import static net.felix.features.itemviewer.ui.ViewerState.helpScreenScrollOffset;
import static net.felix.features.itemviewer.ui.ViewerState.hoveredItemForClick;
import static net.felix.features.itemviewer.ui.ViewerState.isMinimized;
import static net.felix.features.itemviewer.ui.ViewerState.isSymbolMenuOpen;
import static net.felix.features.itemviewer.ui.ViewerState.isViewerSettingsMenuOpen;
import static net.felix.features.itemviewer.ui.ViewerState.isVisible;
import static net.felix.features.itemviewer.ui.ViewerState.showCosts;
import static net.felix.features.itemviewer.ui.ViewerState.showOwnedResources;
import static net.felix.features.itemviewer.ui.ViewerState.sortDropdownOpen;

/**
 * Maus-Eingaben im Item Viewer: Klicks und Scrollen.
 */
public final class ViewerInputHandler {

    private ViewerInputHandler() {
    }

    /**
     * Behandelt Mausrad-Scrolling (wird vom Mixin aufgerufen)
     * @param mouseX Maus X-Position
     * @param mouseY Maus Y-Position
     * @param vertical Scroll-Delta (positiv = runter, negativ = hoch)
     * @return true wenn das Scroll-Event behandelt wurde, false sonst
     */
    public static boolean handleMouseScroll(double mouseX, double mouseY, double vertical) {
        if (ItemViewerFilterMenu.handleScroll(mouseX, mouseY, vertical)) {
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
        
        if (mouseX >= pos.viewerX && mouseX < pos.viewerX + pos.viewerWidth &&
            mouseY >= pos.viewerY && mouseY < pos.viewerY + pos.viewerHeight) {
            
            int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
            int gridAvailableWidth = pos.viewerWidth - VIEWER_PADDING * 2;
            int gridAvailableHeight = paginationY - (pos.viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5 + SORT_DROPDOWN_HEIGHT + 5);
            int itemsPerPage = ItemViewerGrid.computeItemsPerPage(gridAvailableWidth, gridAvailableHeight);
            int totalPages = (int) Math.ceil((double) filteredItems.size() / (double) itemsPerPage);
            if (totalPages == 0) totalPages = 1;
            
            // Scrolle durch Seiten
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
        }
        
        return false;
    }

    /**
     * Behandelt Maus-Klicks (wird vom Mixin aufgerufen)
     */
    public static boolean handleMouseClick(double mouseX, double mouseY, int button) {
        Minecraft client = Minecraft.getInstance();
        
        // Prüfe Klick auf minimierten Button (rechts unten, wenn minimiert) - auch ohne Screen
        if (isMinimized && isVisible && button == 0) {
            int screenWidth = client.getWindow().getGuiScaledWidth();
            int screenHeight = client.getWindow().getGuiScaledHeight();
            int buttonX = screenWidth - MINIMIZE_BUTTON_SIZE - 10;
            int buttonY = screenHeight - MINIMIZE_BUTTON_SIZE - 10;
            
            if (mouseX >= buttonX && mouseX < buttonX + MINIMIZE_BUTTON_SIZE &&
                mouseY >= buttonY && mouseY < buttonY + MINIMIZE_BUTTON_SIZE) {
                // Maximieren
                isMinimized = false;
                return true;
            }
        }
        
        // Wenn minimiert, keine weiteren Klicks abfangen (außer Minimize-Button oben)
        // Dies ermöglicht es den Buttons darunter, Klicks zu verarbeiten
        if (isMinimized && isVisible) {
            return false;
        }
        
        if (!isKitEditorMode() || kitEditorBackgroundScreen != null) {
            AbstractContainerScreen<?> handledScreen = resolveAbstractContainerScreenForInput(client);
            if (handledScreen == null) {
                return false;
            }
            String currentTitle = handledScreen.getTitle().getString();
            if (!isKitEditorMode()
                    && net.felix.utilities.Overall.ZeichenUtility.shouldHideItemViewerForInventoryTitle(currentTitle)
                    && !helpScreenOpen) {
                return false;
            }
        } else if (client.gui.screen() == null) {
            return false;
        }
        
        // Prüfe ZUERST Klick auf Hilfe-Overlay Schließen-Button (höchste Priorität wenn geöffnet)
        if (helpScreenOpen) {
            int screenWidth = client.getWindow().getGuiScaledWidth();
            int screenHeight = client.getWindow().getGuiScaledHeight();
            int boxWidth = Math.min(400, screenWidth - 40);
            int boxHeight = Math.min(450, screenHeight - 40);
            int boxX = (screenWidth - boxWidth) / 2;
            int boxY = (screenHeight - boxHeight) / 2;
            int closeButtonX = boxX + boxWidth - 20;
            int closeButtonY = boxY + 5;
            int closeButtonSize = 15;
            
            if (mouseX >= closeButtonX && mouseX < closeButtonX + closeButtonSize &&
                mouseY >= closeButtonY && mouseY < closeButtonY + closeButtonSize) {
                if (button == 0) {
                    helpScreenOpen = false;
                    return true;
                }
            }
            
            // Klick außerhalb des Hilfe-Overlays schließt es
            if (mouseX < boxX || mouseX >= boxX + boxWidth ||
                mouseY < boxY || mouseY >= boxY + boxHeight) {
                if (button == 0) {
                    helpScreenOpen = false;
                    return true;
                }
            }
            // Wenn Hilfe-Overlay geöffnet ist, verhindere alle anderen Klicks
            return true;
        }
        
        ViewerPosition pos = resolveViewerPositionForMouseInput(client);
        if (pos == null) {
            return false;
        }
        
        // Prüfe Klick auf Viewer-Einstellungen-Menü (vor Einklappen-Button)
        if (!isMinimized && button == 0) {
            int sideButtonX = getSideButtonX(pos);
            int settingsButtonY = getViewerSettingsButtonY(pos);
            int menuWidth = getViewerSettingsMenuWidth();
            int menuX = sideButtonX - menuWidth;
            int menuButtonWidth = MINIMIZE_BUTTON_SIZE;
            int costsButtonX = getViewerSettingsCostsButtonX(menuX);
            int ownedButtonX = getViewerSettingsOwnedButtonX(menuX);

            if (isViewerSettingsMenuOpen) {
                if (mouseX >= costsButtonX && mouseX < costsButtonX + menuButtonWidth
                        && mouseY >= settingsButtonY && mouseY < settingsButtonY + MINIMIZE_BUTTON_SIZE) {
                    showCosts = !showCosts;
                    ItemViewerGrid.invalidateTooltipCache();
                    return true;
                }
                if (mouseX >= ownedButtonX && mouseX < ownedButtonX + menuButtonWidth
                        && mouseY >= settingsButtonY && mouseY < settingsButtonY + MINIMIZE_BUTTON_SIZE) {
                    showOwnedResources = !showOwnedResources;
                    if (!showOwnedResources) {
                        HudStatsReader.clear();
                    }
                    ItemViewerGrid.invalidateTooltipCache();
                    return true;
                }

                if (mouseX >= menuX && mouseX < sideButtonX + MINIMIZE_BUTTON_SIZE
                        && mouseY >= settingsButtonY && mouseY < settingsButtonY + MINIMIZE_BUTTON_SIZE) {
                    return true;
                }
            }

            int minimizeButtonY = getMinimizeButtonY(pos);
            if (mouseX >= sideButtonX && mouseX < sideButtonX + MINIMIZE_BUTTON_SIZE
                    && mouseY >= minimizeButtonY && mouseY < minimizeButtonY + MINIMIZE_BUTTON_SIZE) {
                isMinimized = true;
                return true;
            }
        }
        
        // Prüfe Klick auf Symbol-Menü (wenn geöffnet, VOR Suchleiste, nur Linksklick)
        // Priorisiere Symbol-Menü vor Suchleiste, damit Klicks auf Menü-Buttons funktionieren
        if (button == 0 && isSymbolMenuOpen) {
            int menuButtonWidth = 20;
            int menuButtonHeight = SYMBOL_BUTTON_SIZE; // Gleiche Höhe wie Stern-Button
            int menuWidth = menuButtonWidth * 3 + 2;
            int menuX = pos.symbolButtonX - menuWidth; // Links vom Button
            int menuY = pos.symbolButtonY;
            
            // @-Button im Menü (links)
            if (mouseX >= menuX && mouseX <= menuX + menuButtonWidth &&
                mouseY >= menuY && mouseY <= menuY + menuButtonHeight) {
                insertCharacter('@');
                isSymbolMenuOpen = false;
                return true;
            }
            
            // <-Button im Menü (mitte)
            if (mouseX >= menuX + menuButtonWidth + 1 && mouseX <= menuX + menuButtonWidth * 2 + 1 &&
                mouseY >= menuY && mouseY <= menuY + menuButtonHeight) {
                insertCharacter('<');
                isSymbolMenuOpen = false;
                return true;
            }
            
            // >-Button im Menü (rechts)
            if (mouseX >= menuX + (menuButtonWidth + 1) * 2 && mouseX <= menuX + menuButtonWidth * 3 + 2 &&
                mouseY >= menuY && mouseY <= menuY + menuButtonHeight) {
                insertCharacter('>');
                isSymbolMenuOpen = false;
                return true;
            }
            
            // Wenn Klick im Menü-Bereich ist (auch wenn nicht genau auf einem Button), verhindere andere Klicks
            if (mouseX >= menuX && mouseX <= pos.symbolButtonX + SYMBOL_BUTTON_SIZE &&
                mouseY >= menuY && mouseY <= menuY + menuButtonHeight) {
                // Klick im Menü-Bereich, aber nicht auf einem Button - verhindere andere Klicks
                return true;
            }
            
            // Wenn außerhalb des Menüs geklickt wird, schließe es
            if (mouseX < menuX || mouseX >= pos.symbolButtonX + SYMBOL_BUTTON_SIZE ||
                mouseY < menuY || mouseY >= menuY + menuButtonHeight) {
                isSymbolMenuOpen = false;
            }
        }
        
        // Berechne Suchfeld-Position (wird mehrfach verwendet)
        int searchX = getSearchFieldX(pos);
        int searchY = pos.viewerY + VIEWER_PADDING;
        
        // Prüfe Klick auf Suchfeld (setzt Fokus)
        if (mouseX >= searchX && mouseX < searchX + pos.searchWidth &&
            mouseY >= searchY && mouseY < searchY + SEARCH_HEIGHT) {
            if (button == 0) {
                // Linksklick: Fokus setzen und Cursor-Position berechnen
                setSearchFieldFocused(true);
                searchCursorVisible = true;
                searchCursorBlinkTime = System.currentTimeMillis();
                // Berechne Cursor-Position basierend auf Mausklick-Position
                int clickX = (int) (mouseX - searchX - 3); // 3px Padding links
                cursorPosition = calculateCursorPosition(clickX, currentSearch, client.font);
                clearSelection(); // Auswahl löschen beim Klick
                return true;
            } else if (button == 1) {
                // Rechtsklick: Text löschen
                currentSearch = "";
                cursorPosition = 0;
                clearSelection();
                applyFilters();
                return true;
            }
        }
        
        // Prüfe Rechtsklick auf gehoverten Blueprint (vor anderen Klicks)
        if (button == 1 && hoveredItemForClick != null) {
            // Prüfe ob Klick im Grid-Bereich ist
            int gridX = pos.viewerX + VIEWER_PADDING;
            int gridY = pos.dropdownY + SORT_DROPDOWN_HEIGHT + 5;
            int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
            int gridAvailableHeight = paginationY - gridY;
            int slotSize = ItemViewerGrid.getSlotSize();
            int gridAvailableWidth = pos.viewerWidth - VIEWER_PADDING * 2;
            int gridColumns = Math.max(1, gridAvailableWidth / slotSize);
            int gridRows = Math.max(1, gridAvailableHeight / slotSize);
            int gridWidth = gridColumns * slotSize;
            int gridHeight = gridRows * slotSize;
            
            if (mouseX >= gridX && mouseX < gridX + gridWidth &&
                mouseY >= gridY && mouseY < gridY + gridHeight) {
                
                // Prüfe ob es ein Blueprint ist
                if (hoveredItemForClick.info != null && 
                    Boolean.TRUE.equals(hoveredItemForClick.info.blueprint) &&
                    hoveredItemForClick.name != null && !hoveredItemForClick.name.isEmpty()) {
                    
                    // Toggle Favoriten-Status
                    boolean isFavorite = FavoriteBlueprintsStore.isFavorite(hoveredItemForClick.name);
                    if (isFavorite) {
                        // Entferne aus Favoriten
                        FavoriteBlueprintsStore.removeFavorite(hoveredItemForClick.name);
                    } else {
                        // Füge zu Favoriten hinzu
                        JsonObject jsonObj = hoveredItemForClick.jsonObject;
                        if (jsonObj == null) {
                            // Fallback: Hole aus Map
                            jsonObj = blueprintJsonMap.get(hoveredItemForClick.name);
                        }
                        if (jsonObj != null) {
                            FavoriteBlueprintsStore.addFavorite(jsonObj);
                        }
                    }

                    ItemViewerGrid.invalidateTooltipCache();

                    if (favoritesMode) {
                        applyFilters();
                    }
                    
                    return true;
                }
            }
        }
        
        // Prüfe Klick auf Hilfe-Button (nur Linksklick)
        if (button == 0 && mouseX >= pos.helpButtonX && mouseX < pos.helpButtonX + HELP_BUTTON_SIZE &&
            mouseY >= pos.helpButtonY && mouseY < pos.helpButtonY + HELP_BUTTON_SIZE) {
            helpScreenOpen = !helpScreenOpen;
            blurSearchFieldFocus();
            if (helpScreenOpen) {
                ItemViewerFilterMenu.close();
            }
            return true;
        }
        
        if (button == 0 && mouseX >= pos.filterButtonX && mouseX < pos.filterButtonX + FILTER_BUTTON_SIZE &&
            mouseY >= pos.filterButtonY && mouseY < pos.filterButtonY + FILTER_BUTTON_SIZE) {
            ItemViewerFilterMenu.toggle(allItems, currentSearch);
            blurSearchFieldFocus();
            if (ItemViewerFilterMenu.isOpen()) {
                helpScreenOpen = false;
                activeCategoryOverlay = null;
            }
            return true;
        }
        
        // Prüfe Klick auf Symbol-Button (nur Linksklick)
        if (button == 0 && mouseX >= pos.symbolButtonX && mouseX < pos.symbolButtonX + SYMBOL_BUTTON_SIZE &&
            mouseY >= pos.symbolButtonY && mouseY < pos.symbolButtonY + SYMBOL_BUTTON_SIZE) {
            // Toggle Menü
            isSymbolMenuOpen = !isSymbolMenuOpen;
            return true;
        }
        
        // Prüfe Klick auf Favoriten-Button (nur Linksklick)
        if (button == 0 && mouseX >= pos.favoritesButtonX && mouseX < pos.favoritesButtonX + SORT_DROPDOWN_HEIGHT &&
            mouseY >= pos.favoritesButtonY && mouseY < pos.favoritesButtonY + SORT_DROPDOWN_HEIGHT) {
            favoritesMode = !favoritesMode;
            applyFilters(); // Aktualisiere Filter
            return true;
        }
        
        // Prüfe Klick auf Kit-Button
        if (mouseX >= pos.kitButtonX && mouseX < pos.kitButtonX + KIT_BUTTON_SIZE &&
            mouseY >= pos.kitButtonY && mouseY < pos.kitButtonY + KIT_BUTTON_SIZE) {
            if (button == 0) {
                // Linksklick: Toggle Kit-Filter (Hintergrund-Filterung)
                net.felix.utilities.Town.KitFilterUtility.KitSelection kitSelection = 
                    net.felix.utilities.Town.KitFilterUtility.getKitSelection(KIT_BUTTON_INDEX);
                if (kitSelection != null) {
                    kitFilterActive = !kitFilterActive;
                    applyFilters(); // Aktualisiere Filter
                } else {
                    // Kein Kit ausgewählt - öffne Kit-Auswahlmenü
                    net.felix.utilities.Town.KitFilterUtility.openKitSelectionScreen(KIT_BUTTON_INDEX);
                }
                return true;
            } else if (button == 1) {
                // Rechtsklick: Öffne Kit-Auswahlmenü
                net.felix.utilities.Town.KitFilterUtility.openKitSelectionScreen(KIT_BUTTON_INDEX);
                return true;
            }
        }
        
        if (button == 0 && mouseX >= pos.applyButtonX && mouseX < pos.applyButtonX + pos.applyButtonWidth &&
            mouseY >= pos.applyButtonY && mouseY < pos.applyButtonY + SORT_DROPDOWN_HEIGHT) {
            InventoryFilter.toggleFilter();
            return true;
        }
        
        // Prüfe Klick auf Dropdown-Button (nur Linksklick)
        if (button == 0 && mouseX >= pos.dropdownX && mouseX < pos.dropdownX + pos.dropdownWidth &&
            mouseY >= pos.dropdownY && mouseY < pos.dropdownY + SORT_DROPDOWN_HEIGHT) {
            sortDropdownOpen = !sortDropdownOpen;
            return true;
        }
        
        // Prüfe Klick auf Dropdown-Optionen (wenn geöffnet, nur Linksklick)
        if (button == 0 && sortDropdownOpen) {
            int dropdownListY = pos.dropdownY + SORT_DROPDOWN_HEIGHT;
            int optionIndex = 0;
            for (SortMode mode : SortMode.values()) {
                int optionY = dropdownListY + optionIndex * SORT_OPTION_HEIGHT;
                if (mouseX >= pos.dropdownX && mouseX < pos.dropdownX + pos.dropdownWidth &&
                    mouseY >= optionY && mouseY < optionY + SORT_OPTION_HEIGHT) {
                    setSortMode(mode);
                    sortDropdownOpen = false;
                    return true;
                }
                optionIndex++;
            }
            
            // Klick außerhalb des Dropdowns schließt es
            if (mouseX < pos.dropdownX || mouseX >= pos.dropdownX + pos.dropdownWidth ||
                mouseY < pos.dropdownY || mouseY >= dropdownListY + SortMode.values().length * SORT_OPTION_HEIGHT) {
                sortDropdownOpen = false;
            }
        }
        
        // Prüfe Klick auf Pagination-Buttons (nur Linksklick)
        if (button == 0) {
            int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
            int paginationX = pos.viewerX + VIEWER_PADDING;
            int paginationWidth = pos.viewerWidth - VIEWER_PADDING * 2;
            
            // Berechne Items pro Seite basierend auf aktueller Grid-Breite und -Höhe
            int gridAvailableWidth = pos.viewerWidth - VIEWER_PADDING * 2;
            int gridAvailableHeight = paginationY - (pos.viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5 + SORT_DROPDOWN_HEIGHT + 5);
            int itemsPerPage = ItemViewerGrid.computeItemsPerPage(gridAvailableWidth, gridAvailableHeight);
            int totalPages = (int) Math.ceil((double) filteredItems.size() / (double) itemsPerPage);
            if (totalPages == 0) totalPages = 1;
            
            // Berechne Button-Breite dynamisch (gleiche Logik wie beim Rendering)
            int prevTextWidth = client.font.width("< Zurück");
            int nextTextWidth = client.font.width("Weiter >");
            int buttonWidth = Math.max(prevTextWidth, nextTextWidth) + 10; // 10px Padding
            
            // Previous-Button
            int prevX = paginationX;
            if (mouseX >= prevX && mouseX < prevX + buttonWidth &&
                mouseY >= paginationY && mouseY < paginationY + PAGINATION_HEIGHT) {
                if (currentPage > 0) {
                    currentPage--;
                    return true;
                }
            }
            
            // Next-Button
            int nextX = paginationX + paginationWidth - buttonWidth;
            if (mouseX >= nextX && mouseX < nextX + buttonWidth &&
                mouseY >= paginationY && mouseY < paginationY + PAGINATION_HEIGHT) {
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    return true;
                }
            }
        }
        
        // Kit-Editor: Linksklick auf Item im Grid übernimmt Item/Icon
        if (isKitEditorMode() && button == 0 && hoveredItemForClick != null && kitEditorPickHandler != null) {
            int gridX = pos.viewerX + VIEWER_PADDING;
            int gridY = pos.dropdownY + SORT_DROPDOWN_HEIGHT + 5;
            int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
            int gridWidth = pos.viewerWidth - VIEWER_PADDING * 2;
            int gridHeight = paginationY - gridY;
            if (mouseX >= gridX && mouseX < gridX + gridWidth
                    && mouseY >= gridY && mouseY < gridY + gridHeight) {
                kitEditorPickHandler.accept(hoveredItemForClick, kitEditorPickTarget);
                return true;
            }
        }
        
        return false;
    }
}

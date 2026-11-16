package net.felix.features.itemviewer.ui;

import java.util.ArrayList;
import java.util.List;
import net.felix.features.itemviewer.data.ItemData;
import net.felix.features.itemviewer.filtermenu.FilterMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

import static net.felix.features.itemviewer.ItemViewerHotkeys.getToggleHotkeyText;
import static net.felix.features.itemviewer.data.ItemCatalog.itemsLoaded;
import static net.felix.features.itemviewer.kit.KitEditorMode.KIT_BUTTON_INDEX;
import static net.felix.features.itemviewer.kit.KitEditorMode.KIT_BUTTON_SIZE;
import static net.felix.features.itemviewer.kit.KitEditorMode.renderApplyButton;
import static net.felix.features.itemviewer.kit.KitEditorMode.renderKitButton;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.currentPage;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.filteredItems;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.getCurrentPageItems;
import static net.felix.features.itemviewer.ui.SearchFieldRenderer.renderSearchField;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.checkSymbolButtonHover;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.checkViewerSettingsButtonHover;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.renderFavoritesButton;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.renderFilterButton;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.renderHelpButton;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.renderMinimizeButton;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.renderSortDropdownButton;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.renderSortDropdownList;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.renderSymbolButton;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.renderSymbolMenu;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.renderViewerSettingsButton;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.renderViewerSettingsMenu;
import static net.felix.features.itemviewer.ui.ViewerLayout.FILTER_BUTTON_SIZE;
import static net.felix.features.itemviewer.ui.ViewerLayout.HELP_BUTTON_SIZE;
import static net.felix.features.itemviewer.ui.ViewerLayout.MINIMIZE_BUTTON_SIZE;
import static net.felix.features.itemviewer.ui.ViewerLayout.PAGINATION_HEIGHT;
import static net.felix.features.itemviewer.ui.ViewerLayout.SEARCH_HEIGHT;
import static net.felix.features.itemviewer.ui.ViewerLayout.SORT_DROPDOWN_HEIGHT;
import static net.felix.features.itemviewer.ui.ViewerLayout.SYMBOL_BUTTON_SIZE;
import static net.felix.features.itemviewer.ui.ViewerLayout.VIEWER_PADDING;
import static net.felix.features.itemviewer.ui.ViewerLayout.calculateViewerPositionForScreen;
import static net.felix.features.itemviewer.ui.ViewerLayout.currentGridAvailableHeight;
import static net.felix.features.itemviewer.ui.ViewerLayout.currentGridAvailableWidth;
import static net.felix.features.itemviewer.ui.ViewerLayout.getMinimizeButtonY;
import static net.felix.features.itemviewer.ui.ViewerLayout.getOrCalculateViewerPosition;
import static net.felix.features.itemviewer.ui.ViewerLayout.getSearchFieldX;
import static net.felix.features.itemviewer.ui.ViewerLayout.getSideButtonX;
import static net.felix.features.itemviewer.ui.ViewerLayout.getViewerSettingsButtonY;
import static net.felix.features.itemviewer.ui.ViewerLayout.getViewerSettingsCostsButtonX;
import static net.felix.features.itemviewer.ui.ViewerLayout.getViewerSettingsMenuWidth;
import static net.felix.features.itemviewer.ui.ViewerLayout.getViewerSettingsOwnedButtonX;
import static net.felix.features.itemviewer.ui.ViewerState.helpScreenOpen;
import static net.felix.features.itemviewer.ui.ViewerState.hoveredItemForClick;
import static net.felix.features.itemviewer.ui.ViewerState.isMinimized;
import static net.felix.features.itemviewer.ui.ViewerState.isSymbolMenuOpen;
import static net.felix.features.itemviewer.ui.ViewerState.isViewerSettingsMenuOpen;
import static net.felix.features.itemviewer.ui.ViewerState.isVisible;
import static net.felix.features.itemviewer.ui.ViewerState.lastMouseX;
import static net.felix.features.itemviewer.ui.ViewerState.lastMouseY;
import static net.felix.features.itemviewer.ui.ViewerState.lastRenderedScreenType;
import static net.felix.features.itemviewer.ui.ViewerState.shouldShowItemViewerButtons;
import static net.felix.features.itemviewer.ui.ViewerState.showCosts;
import static net.felix.features.itemviewer.ui.ViewerState.showOwnedResources;
import static net.felix.features.itemviewer.ui.ViewerState.sortDropdownOpen;
import static net.felix.features.itemviewer.ui.ViewerState.updateMousePosition;

/**
 * Zeichnet den Item Viewer: Raster, Seitenleiste, Tooltips, Pagination und minimierten Button.
 */
public final class ViewerRenderer {

    private ViewerRenderer() {
    }

    /**
     * Rendert den minimierten Button (wird von Mixins aufgerufen, nach Screen-Rendering)
     */
    public static void renderMinimizedButtonIfNeeded(GuiGraphicsExtractor context) {
        if (isMinimized && isVisible) {
            Minecraft client = Minecraft.getInstance();
            if (client != null && shouldShowItemViewerButtons(client)) {
                renderMinimizedButton(context, client);
            }
        }
    }

    /**
     * Rendert den Item Viewer für einen Screen (nicht-AbstractContainerScreen)
     */
    private static void renderItemViewerForScreen(GuiGraphicsExtractor context, Minecraft client, Screen screen) {
        if (!isVisible()) {
            return;
        }
        
        // Berechne Position
        ViewerPosition pos = calculateViewerPositionForScreen(screen);
        
        // Rendere Item Viewer mit ViewerPosition
        renderItemViewer(context, client, pos);
    }

    public static void renderItemViewerInScreen(GuiGraphicsExtractor context, Minecraft client, AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        // Prüfe ob sichtbar (inkl. Dimension-Prüfung)
        if (!isVisible()) {
            return;
        }
        
        // Wenn minimiert, nicht rendern (Button wird in Mixins gerendert, damit er über dem dunklen Hintergrund liegt)
        if (isMinimized) {
            return;
        }
        
        // Prüfe ob Items geladen sind
        if (!itemsLoaded) {
            return; // Warte bis Items geladen sind
        }
        
        // Prüfe ob das Menü eines der Special Menus No JEI Zeichen enthält
        // Diese Menüs sollten keine JEI UI anzeigen
        if (screen != null) {
            net.minecraft.network.chat.Component titleText = screen.getTitle();
            String titleWithUnicode = titleText.getString(); // Behält Unicode-Zeichen
            if (net.felix.utilities.Overall.ZeichenUtility.containsSpecialMenusNoJei(titleWithUnicode)) {
                return; // KEINE JEI UI in diesen speziellen Menüs
            }
            if (net.felix.utilities.Overall.ZeichenUtility.shouldHideItemViewerForInventoryTitle(titleWithUnicode)) {
                return; // Equipment / Legend+ / Luftschiff / Glimfang-Shop: kein Item-Viewer
            }
        }
        
        // DEBUG: Logge UI-Rendering nur wenn sich der Screen-Typ ändert
        String currentScreenType = "";
        if (screen != null) {
            currentScreenType = "AbstractContainerScreen (Kiste) - " + screen.getClass().getSimpleName();
        } else if (client.gui.screen() != null && !(client.gui.screen() instanceof AbstractContainerScreen<?>)) {
            currentScreenType = "Spielerinventar - " + client.gui.screen().getClass().getSimpleName();
        }
        
        if (!currentScreenType.equals(lastRenderedScreenType)) {
            lastRenderedScreenType = currentScreenType;
        }
        
        // Update mouse position
        updateMousePosition(mouseX, mouseY);
        
        // Rendere Item-Viewer
        if (screen != null) {
            // AbstractContainerScreen (wird im AbstractContainerScreenMixin behandelt)
            renderItemViewer(context, client, screen);
        } else if (client.gui.screen() != null && !(client.gui.screen() instanceof AbstractContainerScreen<?>)) {
            // Nicht-AbstractContainerScreen (z.B. Spielerinventar)
            renderItemViewerForScreen(context, client, client.gui.screen());
        }
    }

    public static void renderItemViewer(GuiGraphicsExtractor context, Minecraft client, AbstractContainerScreen<?> handledScreen) {
        Screen screen = (Screen) handledScreen;
        
        renderItemViewer(context, client, getOrCalculateViewerPosition(handledScreen, screen));
    }

    /**
     * Rendert den Item Viewer mit einer gegebenen ViewerPosition
     */
    public static void renderItemViewer(GuiGraphicsExtractor context, Minecraft client, ViewerPosition pos) {
        
        // Rendere Hintergrund (sehr transparent, damit Kisten-Hintergrund durchscheint)
        // JEI verwendet keinen Hintergrund oder einen sehr transparenten
        context.fill(pos.viewerX, pos.viewerY, pos.viewerX + pos.viewerWidth, pos.viewerY + pos.viewerHeight,
                     0x40000000); // Sehr transparenter schwarzer Hintergrund (25% Transparenz)
        
        // Rendere Rahmen
        context.fill(pos.viewerX, pos.viewerY, pos.viewerX + pos.viewerWidth, pos.viewerY + 1, 0xFF808080); // Oben
        context.fill(pos.viewerX, pos.viewerY + pos.viewerHeight - 1, pos.viewerX + pos.viewerWidth, pos.viewerY + pos.viewerHeight, 0xFF808080); // Unten
        context.fill(pos.viewerX, pos.viewerY, pos.viewerX + 1, pos.viewerY + pos.viewerHeight, 0xFF808080); // Links
        context.fill(pos.viewerX + pos.viewerWidth - 1, pos.viewerY, pos.viewerX + pos.viewerWidth, pos.viewerY + pos.viewerHeight, 0xFF808080); // Rechts
        
        int currentY = pos.viewerY + VIEWER_PADDING;
        
        // Rendere Hilfe-Button (links)
        renderHelpButton(context, pos.helpButtonX, pos.helpButtonY);
        renderFilterButton(context, pos.filterButtonX, pos.filterButtonY);
        
        // Rendere Suchfeld (rechts neben Filter-Button)
        int searchX = getSearchFieldX(pos); // 5px Abstand
        renderSearchField(context, searchX, currentY, pos.searchWidth);
        
        // Prüfe Hover über Symbol-Button (für Menü-Öffnung)
        checkSymbolButtonHover(pos.symbolButtonX, pos.symbolButtonY);
        
        // Rendere Symbol-Button (oben rechts neben der Suchleiste)
        renderSymbolButton(context, pos.symbolButtonX, pos.symbolButtonY);
        
        // Rendere Symbol-Menü wenn geöffnet
        if (isSymbolMenuOpen) {
            renderSymbolMenu(context, pos.symbolButtonX, pos.symbolButtonY);
        }
        
        currentY += SEARCH_HEIGHT + 5;
        
        // Rendere Sortierungs-Dropdown-Button (ohne Liste, Liste wird später gerendert)
        renderSortDropdownButton(context, pos.dropdownX, currentY);
        
        // Rendere Favoriten-Button (rechts vom Dropdown)
        renderFavoritesButton(context, pos.favoritesButtonX, pos.favoritesButtonY);
        
        // Rendere Kit-Button (rechts vom Favoriten-Button)
        renderKitButton(context, pos.kitButtonX, pos.kitButtonY);
        renderApplyButton(context, pos.applyButtonX, pos.applyButtonY, pos.applyButtonWidth);
        
        currentY += SORT_DROPDOWN_HEIGHT + 5;
        
        // Rendere Item-Grid
        // Berechne verfügbare Breite für das Grid
        int gridAvailableWidth = pos.viewerWidth - VIEWER_PADDING * 2;
        currentGridAvailableWidth = gridAvailableWidth; // Speichere für Pagination etc.
        
        // Berechne verfügbare Höhe für das Grid (viewerHeight minus Suchfeld, Dropdown, Pagination)
        int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
        int gridAvailableHeight = paginationY - currentY; // Von currentY (nach Dropdown) bis Pagination
        currentGridAvailableHeight = gridAvailableHeight; // Speichere für Pagination etc.
        
        if (!itemsLoaded) {
            context.text(client.font, "Items werden geladen...", pos.viewerX + VIEWER_PADDING, currentY, 0xFFAAAAAA, true);
            hoveredItemForClick = null;
        } else {
        List<ItemData> currentPageItems = getCurrentPageItems();
        if (currentPageItems != null && !currentPageItems.isEmpty()) {
            int gridX = pos.viewerX + VIEWER_PADDING;
            int gridY = currentY;
            ItemViewerGrid grid = new ItemViewerGrid(currentPageItems, gridX, gridY, lastMouseX, lastMouseY, gridAvailableWidth, gridAvailableHeight);
            grid.render(context);
            
            // Speichere gehoveres Item für Rechtsklick-Handling
            hoveredItemForClick = grid.getHoveredItem();
            
            // Rendere Tooltip
            grid.renderTooltip(context);
        } else {
            hoveredItemForClick = null;
        }
        }
        
        // Rendere Dropdown-Liste NACH dem Grid (damit sie darüber liegt)
        if (sortDropdownOpen) {
            renderSortDropdownList(context, pos.dropdownX, pos.viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5);
        }
        
        // Rendere Pagination am unteren Rand (paginationY wurde bereits oben berechnet)
        renderPagination(context, pos.viewerX + VIEWER_PADDING, paginationY, pos.viewerWidth - VIEWER_PADDING * 2);
        
        // Rendere Minimierungs-Button (außerhalb des Overlays, am linken Rand) - nur wenn Item Viewer angezeigt wird
        if (shouldShowItemViewerButtons(client)) {
            int sideButtonX = getSideButtonX(pos);
            int settingsButtonY = getViewerSettingsButtonY(pos);
            checkViewerSettingsButtonHover(sideButtonX, settingsButtonY);
            renderViewerSettingsButton(context, sideButtonX, settingsButtonY);
            if (isViewerSettingsMenuOpen) {
                renderViewerSettingsMenu(context, sideButtonX, settingsButtonY);
            }
            renderMinimizeButton(context, sideButtonX, getMinimizeButtonY(pos));
        }
        
        // Rendere Button-Tooltips (am Ende, damit sie über allem liegen)
        renderButtonTooltips(context, pos);
        
        // Hilfe-Overlay wird jetzt in den Mixins (ScreenMixin/AbstractContainerScreenMixin) gerendert,
        // nach dem ItemViewer, damit es über allen Items liegt
        // AspectOverlay wird auch in den Mixins gerendert
    }

    /**
     * Rendert Tooltips für die Buttons (Hilfe, Symbol, Favoriten)
     */
    public static void renderButtonTooltips(GuiGraphicsExtractor context, ViewerPosition pos) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        
        // Prüfe Hover über Hilfe-Button
        boolean helpHovered = lastMouseX >= pos.helpButtonX && lastMouseX < pos.helpButtonX + HELP_BUTTON_SIZE &&
                             lastMouseY >= pos.helpButtonY && lastMouseY < pos.helpButtonY + HELP_BUTTON_SIZE;
        
        boolean filterHovered = lastMouseX >= pos.filterButtonX && lastMouseX < pos.filterButtonX + FILTER_BUTTON_SIZE &&
                                lastMouseY >= pos.filterButtonY && lastMouseY < pos.filterButtonY + FILTER_BUTTON_SIZE;
        
        // Prüfe Hover über Symbol-Button (nur wenn Menü nicht geöffnet ist, um Konflikte zu vermeiden)
        boolean symbolHovered = !isSymbolMenuOpen && 
                               lastMouseX >= pos.symbolButtonX && lastMouseX < pos.symbolButtonX + SYMBOL_BUTTON_SIZE &&
                               lastMouseY >= pos.symbolButtonY && lastMouseY < pos.symbolButtonY + SYMBOL_BUTTON_SIZE;
        
        // Prüfe Hover über Favoriten-Button
        boolean favoritesHovered = lastMouseX >= pos.favoritesButtonX && lastMouseX < pos.favoritesButtonX + SORT_DROPDOWN_HEIGHT &&
                                   lastMouseY >= pos.favoritesButtonY && lastMouseY < pos.favoritesButtonY + SORT_DROPDOWN_HEIGHT;
        
        // Prüfe Hover über Kit-Button
        boolean kitHovered = lastMouseX >= pos.kitButtonX && lastMouseX < pos.kitButtonX + KIT_BUTTON_SIZE &&
                             lastMouseY >= pos.kitButtonY && lastMouseY < pos.kitButtonY + KIT_BUTTON_SIZE;
        
        boolean applyHovered = lastMouseX >= pos.applyButtonX && lastMouseX < pos.applyButtonX + pos.applyButtonWidth &&
                               lastMouseY >= pos.applyButtonY && lastMouseY < pos.applyButtonY + SORT_DROPDOWN_HEIGHT;
        
        // Rendere Tooltip für Hilfe-Button
        if (helpHovered && !helpScreenOpen) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Hilfs Übersicht"));
            // Minecraft's drawTooltip passt automatisch die Position an, damit der Tooltip nicht aus dem Bildschirm rausgeht
            context.setComponentTooltipForNextFrame(client.font, tooltip, lastMouseX, lastMouseY);
        }
        
        if (filterHovered && !FilterMenu.isOpen()) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Filter"));
            context.setComponentTooltipForNextFrame(client.font, tooltip, lastMouseX, lastMouseY);
        }
        
        // Rendere Tooltip für Symbol-Button
        if (symbolHovered) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Sonderzeichen"));
            // Berechne Tooltip-Position: Wenn nicht genug Platz rechts ist, verschiebe nach links
            int tooltipX = lastMouseX;
            int tooltipWidth = client.font.width("Sonderzeichen") + 10; // Geschätzte Breite
            int screenWidth = client.getWindow().getGuiScaledWidth();
            
            // Wenn Tooltip rechts rausgehen würde, verschiebe nach links
            if (tooltipX + tooltipWidth > screenWidth - 10) {
                tooltipX = screenWidth - tooltipWidth - 10;
            }
            
            context.setComponentTooltipForNextFrame(client.font, tooltip, tooltipX, lastMouseY);
        }
        
        // Rendere Tooltip für Favoriten-Button
        if (favoritesHovered) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Favoriten"));
            // Minecraft's drawTooltip passt automatisch die Position an
            context.setComponentTooltipForNextFrame(client.font, tooltip, lastMouseX, lastMouseY);
        }
        
        // Rendere Tooltip für Kit-Button
        if (kitHovered) {
            List<Component> tooltip = new ArrayList<>();
            // Hole aktuelle Kit-Auswahl
            net.felix.utilities.Town.kits.filter.KitFilterUtility.KitSelection kitSelection = 
                net.felix.utilities.Town.kits.filter.KitFilterUtility.getKitSelection(KIT_BUTTON_INDEX);
            
            if (kitSelection != null) {
                tooltip.add(Component.literal("Linksklick: Nach Kit Suchen"));
                tooltip.add(Component.literal("Rechtsklick: Kit auswählen"));
                tooltip.add(Component.empty());
                tooltip.add(Component.literal(kitSelection.getDisplayName())); // z.B. "Münz-Kit Stufe 3"
            } else {
                tooltip.add(Component.literal("Linksklick: Nach Kit Suchen"));
                tooltip.add(Component.literal("Rechtsklick: Kit auswählen"));
            }
            // Minecraft's drawTooltip passt automatisch die Position an
            context.setComponentTooltipForNextFrame(client.font, tooltip, lastMouseX, lastMouseY);
        }
        
        if (applyHovered) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Filterung in geöffnetem Inventar anwenden"));
            context.setComponentTooltipForNextFrame(client.font, tooltip, lastMouseX, lastMouseY);
        }
        
        // Prüfe Hover über Viewer-Einstellungen-Button (über dem Einklappen-Button)
        int sideButtonX = getSideButtonX(pos);
        int settingsButtonY = getViewerSettingsButtonY(pos);
        boolean settingsHovered = !isViewerSettingsMenuOpen
                && lastMouseX >= sideButtonX && lastMouseX < sideButtonX + MINIMIZE_BUTTON_SIZE
                && lastMouseY >= settingsButtonY && lastMouseY < settingsButtonY + MINIMIZE_BUTTON_SIZE;
        if (settingsHovered) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Anzeige-Einstellungen"));
            context.setComponentTooltipForNextFrame(client.font, tooltip, lastMouseX, lastMouseY);
        } else if (isViewerSettingsMenuOpen) {
            int menuWidth = getViewerSettingsMenuWidth();
            int menuX = sideButtonX - menuWidth;
            int menuButtonWidth = MINIMIZE_BUTTON_SIZE;
            int costsButtonX = getViewerSettingsCostsButtonX(menuX);
            int ownedButtonX = getViewerSettingsOwnedButtonX(menuX);
            if (lastMouseX >= costsButtonX && lastMouseX < costsButtonX + menuButtonWidth
                    && lastMouseY >= settingsButtonY && lastMouseY < settingsButtonY + MINIMIZE_BUTTON_SIZE) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal(showCosts ? "Kosten ausblenden" : "Kosten anzeigen"));
                context.setComponentTooltipForNextFrame(client.font, tooltip, lastMouseX, lastMouseY);
            } else if (lastMouseX >= ownedButtonX && lastMouseX < ownedButtonX + menuButtonWidth
                    && lastMouseY >= settingsButtonY && lastMouseY < settingsButtonY + MINIMIZE_BUTTON_SIZE) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal("Gesammelte Materialien/Ressourcen in Kosten und"));
                tooltip.add(Component.literal(showOwnedResources
                        ? "Herstellungs-Anzahl ausblenden"
                        : "Herstellungs-Anzahl einblenden"));
                context.setComponentTooltipForNextFrame(client.font, tooltip, lastMouseX, lastMouseY);
            }
        }

        // Prüfe Hover über Minimierungs-Button (außerhalb des Overlays, am linken Rand)
        int minimizeButtonY = getMinimizeButtonY(pos);
        boolean minimizeHovered = lastMouseX >= sideButtonX && lastMouseX < sideButtonX + MINIMIZE_BUTTON_SIZE
                && lastMouseY >= minimizeButtonY && lastMouseY < minimizeButtonY + MINIMIZE_BUTTON_SIZE;
        
        // Rendere Tooltip für Minimierungs-Button
        if (minimizeHovered) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("ItemViewer einklappen"));
            String hotkeyText = getToggleHotkeyText();
            tooltip.add(Component.literal("(Hotkey: " + hotkeyText + ")"));
            // Minecraft's drawTooltip passt automatisch die Position an
            context.setComponentTooltipForNextFrame(client.font, tooltip, lastMouseX, lastMouseY);
        }
    }

    /**
     * Rendert den minimierten Button (rechts unten am Screen)
     */
    public static void renderMinimizedButton(GuiGraphicsExtractor context, Minecraft client) {
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int buttonX = screenWidth - MINIMIZE_BUTTON_SIZE - 10; // 10px vom rechten Rand
        int buttonY = screenHeight - MINIMIZE_BUTTON_SIZE - 10; // 10px vom unteren Rand
        
        // Prüfe ob Maus über Button ist
        boolean isHovered = lastMouseX >= buttonX && lastMouseX < buttonX + MINIMIZE_BUTTON_SIZE &&
                           lastMouseY >= buttonY && lastMouseY < buttonY + MINIMIZE_BUTTON_SIZE;
        
        // Button-Hintergrund
        int bgColor = isHovered ? 0xFF404040 : 0x80000000;
        context.fill(buttonX, buttonY, buttonX + MINIMIZE_BUTTON_SIZE, buttonY + MINIMIZE_BUTTON_SIZE, bgColor);
        
        // Button-Rahmen
        int borderColor = isHovered ? 0xFFFFFFFF : 0xFF808080;
        context.fill(buttonX, buttonY, buttonX + MINIMIZE_BUTTON_SIZE, buttonY + 1, borderColor); // Oben
        context.fill(buttonX, buttonY + MINIMIZE_BUTTON_SIZE - 1, buttonX + MINIMIZE_BUTTON_SIZE, buttonY + MINIMIZE_BUTTON_SIZE, borderColor); // Unten
        context.fill(buttonX, buttonY, buttonX + 1, buttonY + MINIMIZE_BUTTON_SIZE, borderColor); // Links
        context.fill(buttonX + MINIMIZE_BUTTON_SIZE - 1, buttonY, buttonX + MINIMIZE_BUTTON_SIZE, buttonY + MINIMIZE_BUTTON_SIZE, borderColor); // Rechts
        
        // Maximierungs-Symbol (Pfeil nach links) - "◀" - mittig positioniert
        String maximizeSymbol = "◀";
        int textWidth = client.font.width(maximizeSymbol);
        int textX = buttonX + (MINIMIZE_BUTTON_SIZE - textWidth) / 2; // Horizontal zentriert
        int textY = buttonY + (MINIMIZE_BUTTON_SIZE - client.font.lineHeight) / 2 + 1; // Vertikal zentriert (+1 für bessere Zentrierung)
        context.text(client.font, Component.literal(maximizeSymbol), textX, textY, 0xFFFFFFFF, false);
        
        // Rendere Tooltip für minimierten Button (wenn gehovered)
        if (isHovered) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("ItemViewer ausklappen"));
            String hotkeyText = getToggleHotkeyText();
            tooltip.add(Component.literal("(Hotkey: " + hotkeyText + ")"));
            // Minecraft's drawTooltip passt automatisch die Position an
            context.setComponentTooltipForNextFrame(client.font, tooltip, lastMouseX, lastMouseY);
        }
    }

    /**
     * Rendert Pagination-Buttons
     */
    private static void renderPagination(GuiGraphicsExtractor context, int x, int y, int width) {
        Minecraft client = Minecraft.getInstance();
        
        int itemsPerPage = ItemViewerGrid.computeItemsPerPage(currentGridAvailableWidth, currentGridAvailableHeight);
        int totalPages = (int) Math.ceil((double) filteredItems.size() / (double) itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        
        // Berechne Button-Breite dynamisch basierend auf Text
        String prevText = "< Zurück";
        String nextText = "Weiter >";
        int prevTextWidth = client.font.width(prevText);
        int nextTextWidth = client.font.width(nextText);
        int buttonWidth = Math.max(prevTextWidth, nextTextWidth) + 10; // 10px Padding
        int buttonHeight = PAGINATION_HEIGHT;
        
        // Previous-Button
        boolean canGoPrevious = currentPage > 0;
        int prevX = x;
        int prevBgColor = canGoPrevious ? 0xFF404040 : 0xFF202020;
        context.fill(prevX, y, prevX + buttonWidth, y + buttonHeight, prevBgColor);
        context.fill(prevX, y, prevX + buttonWidth, y + 1, 0xFF808080);
        context.fill(prevX, y + buttonHeight - 1, prevX + buttonWidth, y + buttonHeight, 0xFF808080);
        context.fill(prevX, y, prevX + 1, y + buttonHeight, 0xFF808080);
        context.fill(prevX + buttonWidth - 1, y, prevX + buttonWidth, y + buttonHeight, 0xFF808080);
        
        int prevTextX = prevX + (buttonWidth - prevTextWidth) / 2;
        int prevTextColor = canGoPrevious ? 0xFFFFFFFF : 0xFF808080;
        context.text(client.font, Component.literal(prevText), prevTextX, y + 6, prevTextColor, false);
        
        // Seiten-Info
        String pageText = (currentPage + 1) + " / " + totalPages;
        int pageTextWidth = client.font.width(pageText);
        int pageTextX = x + (width - pageTextWidth) / 2;
        context.text(client.font, Component.literal(pageText), pageTextX, y + 6, 0xFFFFFFFF, false);
        
        // Next-Button
        boolean canGoNext = currentPage < totalPages - 1;
        int nextX = x + width - buttonWidth;
        int nextBgColor = canGoNext ? 0xFF404040 : 0xFF202020;
        context.fill(nextX, y, nextX + buttonWidth, y + buttonHeight, nextBgColor);
        context.fill(nextX, y, nextX + buttonWidth, y + 1, 0xFF808080);
        context.fill(nextX, y + buttonHeight - 1, nextX + buttonWidth, y + buttonHeight, 0xFF808080);
        context.fill(nextX, y, nextX + 1, y + buttonHeight, 0xFF808080);
        context.fill(nextX + buttonWidth - 1, y, nextX + buttonWidth, y + buttonHeight, 0xFF808080);
        
        int nextTextX = nextX + (buttonWidth - nextTextWidth) / 2;
        int nextTextColor = canGoNext ? 0xFFFFFFFF : 0xFF808080;
        context.text(client.font, Component.literal(nextText), nextTextX, y + 6, nextTextColor, false);
    }
}

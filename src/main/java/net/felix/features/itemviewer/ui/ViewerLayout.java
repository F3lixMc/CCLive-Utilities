package net.felix.features.itemviewer.ui;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.features.itemviewer.search.SortMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import static net.felix.features.itemviewer.kit.KitEditorMode.KIT_BUTTON_SIZE;
import static net.felix.features.itemviewer.kit.KitEditorMode.getApplyButtonWidth;
import static net.felix.features.itemviewer.kit.KitEditorMode.isKitEditorMode;
import static net.felix.features.itemviewer.kit.KitEditorMode.kitEditorBackgroundScreen;
import static net.felix.features.itemviewer.kit.KitEditorMode.resolveKitEditorViewerPosition;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.currentPage;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.invalidatePageItemsCache;
import static net.felix.features.itemviewer.ui.ViewerButtonsRenderer.VIEWER_SETTINGS_MENU_BUTTON_COUNT;
import static net.felix.features.itemviewer.ui.ViewerState.helpScreenOpen;

/**
 * Groessen- und Positionsberechnung des Item Viewers relativ zum Inventar.
 */
public final class ViewerLayout {

    private ViewerLayout() {
    }

    private static int cachedSortDropdownWidth = -1;

    private static ViewerPosition cachedHandledViewerPosition;

    private static AbstractContainerScreen<?> cachedHandledViewerPositionScreen;

    private static int cachedViewerScreenWidth = -1;

    private static int cachedViewerScreenHeight = -1;

    private static int cachedViewerInventoryX = -1;

    private static int cachedViewerInventoryWidth = -1;

    private static int cachedViewerInventoryY = -1;

    private static int cachedViewerInventoryHeight = -1;

    private static int cachedViewerMaxSlots = -1;

    private static java.lang.reflect.Field handledScreenXField;

    private static java.lang.reflect.Field handledScreenYField;

    private static java.lang.reflect.Field handledScreenBackgroundWidthField;

    private static java.lang.reflect.Field handledScreenBackgroundHeightField;

    private static boolean handledScreenFieldsResolved = false;

    private static final int VIEWER_VERTICAL_MARGIN = 10;

    // Pagination
    // Aktuelle verfügbare Grid-Breite (wird dynamisch berechnet)
    public static int currentGridAvailableWidth = 108; // Standard: 6 Spalten * 18px

    // Aktuelle verfügbare Grid-Höhe (wird dynamisch berechnet)
    public static int currentGridAvailableHeight = 144; // Standard: 8 Zeilen * 18px

    /**
     * Berechnet die Viewer-Position für nicht-AbstractContainerScreen Screens
     */
    /**
     * Viewer-Breite/-Höhe aus maximalem Platz; Grid höchstens laut Config ({@link CCLiveUtilitiesConfig#getItemViewerMaxSlots()} Slots).
     */
    private static int[] computeViewerSize(int maxViewerWidth, int maxViewerHeight) {
        int minViewerWidth = ItemViewerGrid.getDefaultGridWidth() + VIEWER_PADDING * 2;
        int gridSpaceW = Math.max(ItemViewerGrid.getDefaultGridWidth(), maxViewerWidth - VIEWER_PADDING * 2);
        int gridSpaceH = Math.max(ItemViewerGrid.getGridHeight(), maxViewerHeight - VIEWER_GRID_CHROME_HEIGHT);
        int[] grid = ItemViewerGrid.computeGridDimensions(gridSpaceW, gridSpaceH);
        int viewerWidth = Math.max(minViewerWidth, grid[0] * ItemViewerGrid.getSlotSize() + VIEWER_PADDING * 2);
        int viewerHeight = VIEWER_GRID_CHROME_HEIGHT + grid[1] * ItemViewerGrid.getSlotSize();
        return new int[] { viewerWidth, viewerHeight };
    }

    public static ViewerPosition calculateViewerPositionForScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        
        int maxViewerWidth = screenWidth - VIEWER_PADDING * 2;
        int maxAllowedWidth = (int) (screenWidth * 0.4);
        int cappedMaxWidth = Math.min(maxViewerWidth, maxAllowedWidth);
        int maxViewerHeight = screenHeight - VIEWER_PADDING * 2;
        int[] viewerSize = computeViewerSize(cappedMaxWidth, maxViewerHeight);
        int viewerWidth = viewerSize[0];
        int viewerHeight = viewerSize[1];
        
        int viewerX = (screenWidth - viewerWidth) / 2;
        int viewerY = computeCenteredViewerY(screen, 0, screenHeight, viewerHeight);
        
        int dropdownWidth = getSortDropdownWidth(client);
        
        // Suchfeld nutzt volle Breite (minus Hilfe- und Filter-Button)
        int searchWidth = viewerWidth - VIEWER_PADDING * 2 - HELP_BUTTON_SIZE - 5 - FILTER_BUTTON_SIZE - 5;
        
        // Dropdown wird unter die Suchleiste platziert
        int dropdownX = viewerX + VIEWER_PADDING;
        int dropdownY = viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5;
        
        // Hilfe-Button Position (2px nach oben verschoben)
        int helpButtonX = viewerX + VIEWER_PADDING;
        int helpButtonY = viewerY + VIEWER_PADDING;
        int filterButtonX = helpButtonX + HELP_BUTTON_SIZE + 5;
        int filterButtonY = helpButtonY;
        
        // Symbol-Button Position (rechts bündig mit Suchleiste)
        int searchX = filterButtonX + FILTER_BUTTON_SIZE + 5;
        int symbolButtonX = searchX + searchWidth - SYMBOL_BUTTON_SIZE;
        int symbolButtonY = dropdownY; // Gleiche Y-Position wie Dropdown
        
        // Favoriten-Button Position (rechts vom Dropdown, gleiche Höhe)
        int favoritesButtonX = dropdownX + dropdownWidth + 5; // 5px Abstand zum Dropdown
        int favoritesButtonY = dropdownY; // Gleiche Y-Position wie Dropdown
        
        // Kit-Button Position (rechts vom Favoriten-Button, gleiche Höhe)
        int kitButtonX = favoritesButtonX + SORT_DROPDOWN_HEIGHT + 5; // 5px Abstand zum Favoriten-Button
        int kitButtonY = dropdownY; // Gleiche Y-Position wie Dropdown
        
        int applyButtonWidth = getApplyButtonWidth(client);
        int applyButtonX = kitButtonX + KIT_BUTTON_SIZE + 5;
        int applyButtonY = dropdownY;
        
        int minButtonRowWidth = VIEWER_PADDING + dropdownWidth + 5 + SORT_DROPDOWN_HEIGHT + 5 + KIT_BUTTON_SIZE + 5 + applyButtonWidth + VIEWER_PADDING;
        if (viewerWidth < minButtonRowWidth) {
            viewerWidth = minButtonRowWidth;
        }
        
        return new ViewerPosition(viewerX, viewerY, viewerWidth, viewerHeight, dropdownX, dropdownY, dropdownWidth, searchWidth, helpButtonX, helpButtonY, filterButtonX, filterButtonY, symbolButtonX, symbolButtonY, favoritesButtonX, favoritesButtonY, kitButtonX, kitButtonY, applyButtonX, applyButtonY, applyButtonWidth);
    }

    public static int getSearchFieldX(ViewerPosition pos) {
        return pos.helpButtonX + HELP_BUTTON_SIZE + 5 + FILTER_BUTTON_SIZE + 5;
    }

    // UI-Konstanten
    public static final int VIEWER_PADDING = 5;

    public static final int SEARCH_HEIGHT = 20;

    static final int HELP_BUTTON_SIZE = 20; // Gleiche Höhe wie Suchleiste

    static final int FILTER_BUTTON_SIZE = 20;

    public static final int SORT_DROPDOWN_HEIGHT = 20;

    static final int SORT_OPTION_HEIGHT = 16;

    static final int SORT_DROPDOWN_PADDING = 6; // Padding links/rechts im Dropdown

    static final int PAGINATION_HEIGHT = 20;

    /** Höhe der UI oberhalb/unterhalb des Item-Grids (Padding, Suche, Dropdown, Pagination). */
    private static final int VIEWER_GRID_CHROME_HEIGHT =
            VIEWER_PADDING + SEARCH_HEIGHT + 5 + SORT_DROPDOWN_HEIGHT + 5 + PAGINATION_HEIGHT + VIEWER_PADDING;

    static final int MINIMIZE_BUTTON_SIZE = 20; // Größe des Minimierungs-Buttons

    private static final int SIDE_BUTTON_GAP = 3;

    // Symbol-Button Variablen (für @, <, >)
    static final int SYMBOL_BUTTON_SIZE = 20; // Gleiche Höhe wie Dropdown für symmetrisches Aussehen

    static int getViewerSettingsMenuWidth() {
        // Linker Rand (1px) + Buttons + Trennlinien dazwischen (wie Symbol-Menü)
        return MINIMIZE_BUTTON_SIZE * VIEWER_SETTINGS_MENU_BUTTON_COUNT
                + (VIEWER_SETTINGS_MENU_BUTTON_COUNT - 1) + 1;
    }

    static int getViewerSettingsCostsButtonX(int menuX) {
        return menuX + 1;
    }

    static int getViewerSettingsOwnedButtonX(int menuX) {
        return menuX + MINIMIZE_BUTTON_SIZE + 1;
    }

    static int getSideButtonX(ViewerPosition pos) {
        return pos.viewerX - MINIMIZE_BUTTON_SIZE - 3;
    }

    static int getMinimizeButtonY(ViewerPosition pos) {
        return pos.viewerY + pos.viewerHeight - MINIMIZE_BUTTON_SIZE - VIEWER_PADDING + 5;
    }

    static int getViewerSettingsButtonY(ViewerPosition pos) {
        return getMinimizeButtonY(pos) - MINIMIZE_BUTTON_SIZE - SIDE_BUTTON_GAP;
    }

    public static ViewerPosition resolveViewerPositionForMouseInput(Minecraft client) {
        if (isKitEditorMode()) {
            return resolveKitEditorViewerPosition();
        }
        if (client.gui.screen() == null) {
            return null;
        }
        AbstractContainerScreen<?> handledScreen = resolveAbstractContainerScreenForInput(client);
        if (handledScreen == null) {
            return null;
        }
        String currentTitle = handledScreen.getTitle().getString();
        if (net.felix.utilities.Overall.ZeichenUtility.shouldHideItemViewerForInventoryTitle(currentTitle)
                && !helpScreenOpen) {
            return null;
        }
        return getOrCalculateViewerPosition(handledScreen, client.gui.screen());
    }

    /**
     * Berechnet die Viewer-Position und Dimensionen (wird für Rendering und Klick-Handling verwendet)
     */
    public static void invalidateViewerPositionCache() {
        cachedHandledViewerPosition = null;
        cachedHandledViewerPositionScreen = null;
        cachedViewerMaxSlots = -1;
    }

    public static void onMaxSlotsConfigChanged() {
        invalidateViewerPositionCache();
        invalidatePageItemsCache();
        currentPage = 0;
    }

    public static ViewerPosition getOrCalculateViewerPosition(AbstractContainerScreen<?> handledScreen, Screen screen) {
        int inventoryX = readAbstractContainerScreenInventoryX(handledScreen, screen);
        int inventoryWidth = readAbstractContainerScreenInventoryWidth(handledScreen);
        int inventoryY = readAbstractContainerScreenInventoryY(handledScreen, screen);
        int inventoryHeight = readAbstractContainerScreenInventoryHeight(handledScreen);
        int maxSlots = CCLiveUtilitiesConfig.getItemViewerMaxSlots();
        if (cachedHandledViewerPosition != null
                && cachedHandledViewerPositionScreen == handledScreen
                && cachedViewerScreenWidth == screen.width
                && cachedViewerScreenHeight == screen.height
                && cachedViewerInventoryX == inventoryX
                && cachedViewerInventoryWidth == inventoryWidth
                && cachedViewerInventoryY == inventoryY
                && cachedViewerInventoryHeight == inventoryHeight
                && cachedViewerMaxSlots == maxSlots) {
            return cachedHandledViewerPosition;
        }
        cachedHandledViewerPosition = calculateViewerPosition(handledScreen, screen, inventoryX, inventoryWidth);
        cachedHandledViewerPositionScreen = handledScreen;
        cachedViewerScreenWidth = screen.width;
        cachedViewerScreenHeight = screen.height;
        cachedViewerInventoryX = inventoryX;
        cachedViewerInventoryWidth = inventoryWidth;
        cachedViewerInventoryY = inventoryY;
        cachedViewerInventoryHeight = inventoryHeight;
        cachedViewerMaxSlots = maxSlots;
        return cachedHandledViewerPosition;
    }

    private static void resolveAbstractContainerScreenFields() {
        if (handledScreenFieldsResolved) {
            return;
        }
        try {
            handledScreenXField = AbstractContainerScreen.class.getDeclaredField("leftPos");
            handledScreenXField.setAccessible(true);
        } catch (Exception ignored) {
        }
        try {
            handledScreenYField = AbstractContainerScreen.class.getDeclaredField("topPos");
            handledScreenYField.setAccessible(true);
        } catch (Exception ignored) {
        }
        try {
            handledScreenBackgroundWidthField = AbstractContainerScreen.class.getDeclaredField("imageWidth");
            handledScreenBackgroundWidthField.setAccessible(true);
        } catch (Exception ignored) {
        }
        try {
            handledScreenBackgroundHeightField = AbstractContainerScreen.class.getDeclaredField("imageHeight");
            handledScreenBackgroundHeightField.setAccessible(true);
        } catch (Exception ignored) {
        }
        handledScreenFieldsResolved = true;
    }

    private static int readAbstractContainerScreenInventoryX(AbstractContainerScreen<?> handledScreen, Screen screen) {
        resolveAbstractContainerScreenFields();
        if (handledScreenXField != null) {
            try {
                return handledScreenXField.getInt(handledScreen);
            } catch (Exception ignored) {
            }
        }
        int width = readAbstractContainerScreenInventoryWidth(handledScreen);
        return (screen.width - width) / 2;
    }

    private static int readAbstractContainerScreenInventoryWidth(AbstractContainerScreen<?> handledScreen) {
        resolveAbstractContainerScreenFields();
        if (handledScreenBackgroundWidthField != null) {
            try {
                return handledScreenBackgroundWidthField.getInt(handledScreen);
            } catch (Exception ignored) {
            }
        }
        return 176;
    }

    private static int readAbstractContainerScreenInventoryY(AbstractContainerScreen<?> handledScreen, Screen screen) {
        resolveAbstractContainerScreenFields();
        if (handledScreenYField != null) {
            try {
                return handledScreenYField.getInt(handledScreen);
            } catch (Exception ignored) {
            }
        }
        int height = readAbstractContainerScreenInventoryHeight(handledScreen);
        return (screen.height - height) / 2;
    }

    private static int readAbstractContainerScreenInventoryHeight(AbstractContainerScreen<?> handledScreen) {
        resolveAbstractContainerScreenFields();
        if (handledScreenBackgroundHeightField != null) {
            try {
                return handledScreenBackgroundHeightField.getInt(handledScreen);
            } catch (Exception ignored) {
            }
        }
        return 166;
    }

    /** Vertikal zentriert (gleicher Abstand oben/unten), am Inventar ausgerichtet wenn möglich. */
    private static int computeCenteredViewerY(Screen screen, int referenceY, int referenceHeight, int viewerHeight) {
        int viewerY = referenceY + (referenceHeight - viewerHeight) / 2;
        int maxY = screen.height - viewerHeight - VIEWER_VERTICAL_MARGIN;
        return Math.max(VIEWER_VERTICAL_MARGIN, Math.min(viewerY, maxY));
    }

    private static int getSortDropdownWidth(Minecraft client) {
        if (cachedSortDropdownWidth > 0) {
            return cachedSortDropdownWidth;
        }
        int maxDropdownWidth = 0;
        for (SortMode mode : SortMode.values()) {
            int textWidth = client.font.width(mode.getDisplayName());
            maxDropdownWidth = Math.max(maxDropdownWidth, textWidth);
        }
        cachedSortDropdownWidth = maxDropdownWidth + SORT_DROPDOWN_PADDING * 2 + 15;
        return cachedSortDropdownWidth;
    }

    private static ViewerPosition calculateViewerPosition(AbstractContainerScreen<?> handledScreen, Screen screen, int inventoryX, int inventoryWidth) {
        int inventoryY = readAbstractContainerScreenInventoryY(handledScreen, screen);
        int inventoryHeight = readAbstractContainerScreenInventoryHeight(handledScreen);
        return buildViewerPositionForInventoryArea(screen, inventoryX, inventoryWidth, inventoryY, inventoryHeight);
    }

    /** Gleiche Viewer-Position wie neben einem zentrierten Schmied-Inventar (176×166). */
    public static ViewerPosition calculateViewerPositionBesideCenteredInventory(Screen screen) {
        int inventoryWidth = 176;
        int inventoryHeight = 166;
        int inventoryX = (screen.width - inventoryWidth) / 2;
        int inventoryY = (screen.height - inventoryHeight) / 2;
        return buildViewerPositionForInventoryArea(screen, inventoryX, inventoryWidth, inventoryY, inventoryHeight);
    }

    private static ViewerPosition buildViewerPositionForInventoryArea(Screen screen, int inventoryX, int inventoryWidth,
            int inventoryY, int inventoryHeight) {
        Minecraft client = Minecraft.getInstance();

        int availableWidthRight = screen.width - (inventoryX + inventoryWidth) - 10;
        int availableWidthLeft = inventoryX - 20;

        boolean placeRight = availableWidthRight >= ItemViewerGrid.getDefaultGridWidth() + VIEWER_PADDING * 2;
        int viewerX;
        int maxViewerWidth;

        if (placeRight) {
            viewerX = inventoryX + inventoryWidth + 10;
            maxViewerWidth = screen.width - viewerX - 10;
        } else {
            viewerX = 10;
            maxViewerWidth = availableWidthLeft;
        }

        int maxAllowedWidth = Math.min(maxViewerWidth, (int) (screen.width * 0.4));
        int maxViewerHeight = screen.height - 20;
        int[] viewerSize = computeViewerSize(maxAllowedWidth, maxViewerHeight);
        int viewerWidth = viewerSize[0];
        int viewerHeight = viewerSize[1];

        int dropdownWidth = getSortDropdownWidth(client);
        int viewerY = computeCenteredViewerY(screen, inventoryY, inventoryHeight, viewerHeight);
        
        int helpButtonX = viewerX + VIEWER_PADDING;
        int helpButtonY = viewerY + VIEWER_PADDING;
        int filterButtonX = helpButtonX + HELP_BUTTON_SIZE + 5;
        int filterButtonY = helpButtonY;
        
        int symbolButtonX = viewerX + viewerWidth - VIEWER_PADDING - SYMBOL_BUTTON_SIZE;
        int symbolButtonY = viewerY + VIEWER_PADDING;
        
        int searchX = filterButtonX + FILTER_BUTTON_SIZE + 5;
        int searchWidth = symbolButtonX - searchX - 5;
        
        int dropdownX = viewerX + VIEWER_PADDING;
        int dropdownY = viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5;
        
        // Favoriten-Button Position (rechts vom Dropdown, gleiche Höhe)
        int favoritesButtonX = dropdownX + dropdownWidth + 5; // 5px Abstand zum Dropdown
        int favoritesButtonY = dropdownY; // Gleiche Y-Position wie Dropdown
        
        // Kit-Button Position (rechts vom Favoriten-Button, gleiche Höhe)
        int kitButtonX = favoritesButtonX + SORT_DROPDOWN_HEIGHT + 5; // 5px Abstand zum Favoriten-Button
        int kitButtonY = dropdownY; // Gleiche Y-Position wie Dropdown
        
        int applyButtonWidth = getApplyButtonWidth(client);
        int applyButtonX = kitButtonX + KIT_BUTTON_SIZE + 5;
        int applyButtonY = dropdownY;
        
        int minButtonRowWidth = VIEWER_PADDING + dropdownWidth + 5 + SORT_DROPDOWN_HEIGHT + 5 + KIT_BUTTON_SIZE + 5 + applyButtonWidth + VIEWER_PADDING;
        if (viewerWidth < minButtonRowWidth) {
            viewerWidth = minButtonRowWidth;
        }
        
        return new ViewerPosition(viewerX, viewerY, viewerWidth, viewerHeight, dropdownX, dropdownY, dropdownWidth, searchWidth, helpButtonX, helpButtonY, filterButtonX, filterButtonY, symbolButtonX, symbolButtonY, favoritesButtonX, favoritesButtonY, kitButtonX, kitButtonY, applyButtonX, applyButtonY, applyButtonWidth);
    }

    static AbstractContainerScreen<?> resolveAbstractContainerScreenForInput(Minecraft client) {
        if (client.gui.screen() == null) {
            return null;
        }
        if (isKitEditorMode() && kitEditorBackgroundScreen != null) {
            return kitEditorBackgroundScreen;
        }
        if (client.gui.screen() instanceof AbstractContainerScreen<?> handledScreen) {
            return handledScreen;
        }
        return null;
    }
}

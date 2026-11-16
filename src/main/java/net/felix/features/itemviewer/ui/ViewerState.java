package net.felix.features.itemviewer.ui;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.features.itemviewer.data.ItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import static net.felix.features.itemviewer.data.ItemCatalog.itemsLoaded;
import static net.felix.features.itemviewer.inventory.InventoryItemCapture.isInventoryOpen;
import static net.felix.features.itemviewer.kit.KitEditorMode.isKitEditorMode;

/**
 * Laufzeit-Zustand der Item-Viewer-Oberflaeche: Sichtbarkeit, Menues, Maus und Hover.
 */
public final class ViewerState {

    private ViewerState() {
    }

    public static boolean isVisible = true; // Standard: sichtbar

    public static boolean isMinimized = false; // Minimiert (rechts unten)

    /**
     * Gibt das aktuell gehoverte Item aus dem Grid zurück
     */
    public static ItemData getHoveredItemFromGrid() {
        // Das Grid wird in renderItemViewerInScreen gerendert
        // Wir müssen das gehoverte Item aus dem aktuellen Grid holen
        // Da das Grid nicht statisch gespeichert wird, müssen wir es anders lösen
        // Für jetzt: verwende hoveredItemForClick (wird im Grid gesetzt)
        return hoveredItemForClick;
    }

    /**
     * Wird vom AbstractContainerScreenMixin oder ScreenMixin aufgerufen, um den Item Viewer zu rendern
     * Rendert nach dem Screen-Rendering, damit es über dem dunklen Hintergrund liegt
     */
    // Debug: Letzter gerenderter Screen-Typ (um Logs zu reduzieren)
    static String lastRenderedScreenType = "";

    static boolean showOwnedResources = false;

    static boolean showCosts = true;

    // Maus-Position für Hover-Detection
    public static int lastMouseX = 0;

    public static int lastMouseY = 0;

    // Tracke letzten gehoverten Slot für Clipboard-Pin-Funktion
    public static net.minecraft.world.inventory.Slot lastHoveredSlot = null;

    public static net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> lastHoveredScreen = null;

    // Dropdown-State
    static boolean sortDropdownOpen = false;

    public static boolean helpScreenOpen = false;

    static int helpScreenScrollOffset = 0;

    static boolean helpScreenHovered = false;

    // Favoriten-Modus
    public static boolean favoritesMode = false;

    // Gehovertes Item (für Rechtsklick-Handling)
    static ItemData hoveredItemForClick = null;

    public static boolean isSymbolMenuOpen = false;

    public static boolean isSymbolButtonHovered = false;

    static boolean isViewerSettingsMenuOpen = false;

    static boolean isViewerSettingsButtonHovered = false;

    static CategoryButton activeCategoryOverlay = null;

    public static boolean isShowOwnedResources() {
        return showOwnedResources;
    }

    public static boolean isShowCosts() {
        return showCosts;
    }

    /**
     * Aktualisiert die Maus-Position (wird vom Mixin aufgerufen)
     */
    public static void updateMousePosition(int mouseX, int mouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    /**
     * Aktualisiert den gehoverten Slot (wird vom Mixin aufgerufen)
     */
    public static void updateHoveredSlot(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen, net.minecraft.world.inventory.Slot slot) {
        lastHoveredScreen = screen;
        lastHoveredSlot = slot;
        
        // Debug: Zeige nur wenn sich der Slot ändert (reduziere Ausgabe)
        if (slot != null && slot.hasItem()) {
            net.minecraft.world.item.ItemStack stack = slot.getItem();
            if (stack != null && !stack.isEmpty()) {
                // Slot wird getrackt
            }
        }
    }

    /** Zuletzt gehoverter Inventar-Slot (kann null sein). */
    public static net.minecraft.world.inventory.Slot getLastHoveredSlot() {
        return lastHoveredSlot;
    }

    public static double getLastMouseX() {
        return lastMouseX;
    }

    public static double getLastMouseY() {
        return lastMouseY;
    }

    public static boolean isVisible() {
        if (isKitEditorMode()) {
            return true;
        }
        // Prüfe Config-Option
        if (!CCLiveUtilitiesConfig.HANDLER.instance().showItemViewer) {
            return false;
        }
        
        // Deaktiviere in der general_lobby Dimension
        try {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.level != null) {
                String dimensionId = client.level.dimension().identifier().toString();
                if ("minecraft:general_lobby".equals(dimensionId)) {
                    return false;
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
        
        return isVisible;
    }

    /**
     * True while the Item Viewer is showing and the cursor is over an item in the grid
     * (uses hover from the previous/current render frame).
     */
    public static boolean hasItemViewerHover() {
        return isVisible() && !isMinimized && hoveredItemForClick != null;
    }

    /**
     * Prüft, ob der Item Viewer minimiert (eingeklappt) ist
     * @return true wenn minimiert, false wenn ausgeklappt
     */
    public static boolean isMinimized() {
        return isMinimized;
    }

    /**
     * Prüft, ob die Item Viewer Buttons angezeigt werden sollen
     * Verwendet die gleichen Bedingungen wie renderItemViewerInScreen
     * @param client MinecraftClient Instanz
     * @return true wenn Buttons angezeigt werden sollen, false sonst
     */
    public static boolean shouldShowItemViewerButtons(Minecraft client) {
        if (client == null) {
            return false;
        }
        
        // Prüfe ob sichtbar (inkl. Dimension-Prüfung)
        if (!isVisible()) {
            return false;
        }
        
        // Prüfe ob Items geladen sind
        if (!itemsLoaded) {
            return false;
        }
        
        // Prüfe ob in einem Inventar
        if (!isInventoryOpen(client)) {
            return false;
        }
        
        // Prüfe ob das Menü eines der Special Menus No JEI Zeichen enthält
        if (client.gui.screen() instanceof AbstractContainerScreen<?> handledScreen) {
            net.minecraft.network.chat.Component titleText = handledScreen.getTitle();
            String titleWithUnicode = titleText.getString(); // Behält Unicode-Zeichen
            if (net.felix.utilities.Overall.ZeichenUtility.containsSpecialMenusNoJei(titleWithUnicode)) {
                return false; // KEINE Buttons in diesen speziellen Menüs
            }
            if (net.felix.utilities.Overall.ZeichenUtility.shouldHideItemViewerForInventoryTitle(titleWithUnicode)) {
                return false;
            }
        }
        
        return true;
    }
}

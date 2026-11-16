package net.felix.features.itemviewer.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import static net.felix.features.itemviewer.ui.ViewerState.helpScreenOpen;

/**
 * Leitet Anzeige und Eingaben an das Filtermenue weiter.
 */
public final class FilterOverlayControls {

    private FilterOverlayControls() {
    }

    public static void renderFilterOverlay(GuiGraphicsExtractor context) {
        ItemViewerFilterMenu.render(context);
    }

    public static boolean isOverlayOpen() {
        return helpScreenOpen || ItemViewerFilterMenu.isOpen();
    }

    public static boolean handleFilterOverlayEscape() {
        return ItemViewerFilterMenu.handleEscape();
    }

    public static void closeFilterOverlay() {
        ItemViewerFilterMenu.close();
    }

    public static boolean handleFilterOverlayClick(double mouseX, double mouseY, int button) {
        return ItemViewerFilterMenu.handleClick(mouseX, mouseY, button);
    }

    public static boolean handleFilterOverlayDrag(double mouseX, double mouseY, int button) {
        return ItemViewerFilterMenu.handleMouseDrag(mouseX, mouseY, button);
    }

    public static boolean handleFilterOverlayRelease(double mouseX, double mouseY, int button) {
        return ItemViewerFilterMenu.handleMouseRelease(mouseX, mouseY, button);
    }

    public static boolean handleFilterOverlayKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!ItemViewerFilterMenu.isOpen()) {
            return false;
        }
        return ItemViewerFilterMenu.handleKeyPress(keyCode, scanCode, modifiers);
    }
}

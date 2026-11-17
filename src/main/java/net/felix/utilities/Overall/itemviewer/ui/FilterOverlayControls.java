package net.felix.utilities.Overall.itemviewer.ui;

import net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenu;
import net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuInput;
import net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.helpScreenOpen;

/**
 * Leitet Anzeige und Eingaben an das Filtermenue weiter.
 */
public final class FilterOverlayControls {

    private FilterOverlayControls() {
    }

    public static void renderFilterOverlay(GuiGraphicsExtractor context) {
        FilterMenuRenderer.render(context);
    }

    public static boolean isOverlayOpen() {
        return helpScreenOpen || FilterMenu.isOpen();
    }

    public static boolean handleFilterOverlayEscape() {
        return FilterMenu.handleEscape();
    }

    public static void closeFilterOverlay() {
        FilterMenu.close();
    }

    public static boolean handleFilterOverlayClick(double mouseX, double mouseY, int button) {
        return FilterMenuInput.handleClick(mouseX, mouseY, button);
    }

    public static boolean handleFilterOverlayDrag(double mouseX, double mouseY, int button) {
        return FilterMenuInput.handleMouseDrag(mouseX, mouseY, button);
    }

    public static boolean handleFilterOverlayRelease(double mouseX, double mouseY, int button) {
        return FilterMenuInput.handleMouseRelease(mouseX, mouseY, button);
    }

    public static boolean handleFilterOverlayKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!FilterMenu.isOpen()) {
            return false;
        }
        return FilterMenuInput.handleKeyPress(keyCode, scanCode, modifiers);
    }
}

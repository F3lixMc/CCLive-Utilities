package net.felix.utilities.Aincraft.itemviewer.ui;


/**
 * Hilfsklasse für Viewer-Position
 */
public class ViewerPosition {
    public final int viewerX, viewerY, viewerWidth, viewerHeight;
    public final int dropdownX, dropdownY, dropdownWidth, searchWidth;
    public final int helpButtonX, helpButtonY;
    public final int filterButtonX, filterButtonY;
    public final int symbolButtonX, symbolButtonY;
    public final int favoritesButtonX, favoritesButtonY;
    public final int kitButtonX, kitButtonY;
    public final int applyButtonX, applyButtonY, applyButtonWidth;
    
    ViewerPosition(int viewerX, int viewerY, int viewerWidth, int viewerHeight,
                  int dropdownX, int dropdownY, int dropdownWidth, int searchWidth,
                  int helpButtonX, int helpButtonY, int filterButtonX, int filterButtonY,
                  int symbolButtonX, int symbolButtonY,
                  int favoritesButtonX, int favoritesButtonY, int kitButtonX, int kitButtonY,
                  int applyButtonX, int applyButtonY, int applyButtonWidth) {
        this.viewerX = viewerX;
        this.viewerY = viewerY;
        this.viewerWidth = viewerWidth;
        this.viewerHeight = viewerHeight;
        this.dropdownX = dropdownX;
        this.dropdownY = dropdownY;
        this.dropdownWidth = dropdownWidth;
        this.searchWidth = searchWidth;
        this.helpButtonX = helpButtonX;
        this.helpButtonY = helpButtonY;
        this.filterButtonX = filterButtonX;
        this.filterButtonY = filterButtonY;
        this.symbolButtonX = symbolButtonX;
        this.symbolButtonY = symbolButtonY;
        this.favoritesButtonX = favoritesButtonX;
        this.favoritesButtonY = favoritesButtonY;
        this.kitButtonX = kitButtonX;
        this.kitButtonY = kitButtonY;
        this.applyButtonX = applyButtonX;
        this.applyButtonY = applyButtonY;
        this.applyButtonWidth = applyButtonWidth;
    }
}

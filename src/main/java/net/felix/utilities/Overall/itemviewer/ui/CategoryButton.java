package net.felix.utilities.Overall.itemviewer.ui;


/**
 * Datenstruktur für Kategorie-Buttons
 */
public class CategoryButton {
    final String name;
    final java.util.List<String> tags;
    int x, y, width, height;

    CategoryButton(String name, java.util.List<String> tags) {
        this.name = name;
        this.tags = tags;
    }
}

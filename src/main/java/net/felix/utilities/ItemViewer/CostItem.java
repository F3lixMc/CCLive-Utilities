package net.felix.utilities.ItemViewer;

/**
 * Datenstruktur für eine einzelne Kosten-Kategorie
 * Enthält nur Text und Zahlen (keine Item-IDs)
 */
public class CostItem {
    public String itemName;  // "Coins", "Kaktus", "Hasenfell", etc.
    public Object amount;    // Kann Integer (26) oder String ("80.358bj", "22.625ad") sein
}


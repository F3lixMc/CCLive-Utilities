package net.felix.utilities.ItemViewer;

/**
 * Datenstruktur für Zusatzinfos eines Items
 */
public class ItemInfo {
    public String aspect;        // "Erde", "Feuer", etc. (kann leer sein "")
    public String rarity;        // "common", "uncommon", "rare", "epic", "legendary"
    public String description;   // Beschreibung des Items (kann leer sein "")
    public String type;          // "Platte", "Waffe", etc.
    public String stats;         // "Rüstung 30", etc.
    public Boolean blueprint;    // true wenn es ein Bauplan ist
    public Boolean module;       // true wenn es ein Modul ist
    public Boolean ability;      // true wenn es eine Fähigkeit ist
    public Boolean rune;         // true wenn es eine Rune ist
}


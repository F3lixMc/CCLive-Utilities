package net.felix.features.itemviewer.search;

/**
 * Filter für einen spezifischen Modifier
 */
public class ModifierFilter {
    public String modifier;  // Modifier-Name (z.B. "Andere")
    public Integer count;    // Anzahl (z.B. 2 für "Andere:2", null wenn nicht angegeben)
}


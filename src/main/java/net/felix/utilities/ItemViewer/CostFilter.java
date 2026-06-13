package net.felix.utilities.ItemViewer;

/**
 * Filter für eine spezifische Kosten-Kategorie
 */
public class CostFilter {
    public String category;  // "amboss", "ressource", "material", "material1"-"material5", "cactus", "soul", "coin", "ofen"
    public Integer amount;   // Zahl (z.B. 0, 5, 1000)
    public String itemName;  // Item-Name (z.B. "Eichenholz", "Diamant")
}


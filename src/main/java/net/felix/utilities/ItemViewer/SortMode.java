package net.felix.utilities.ItemViewer;

import java.util.Comparator;
import java.util.List;

/**
 * Sortier-Modi für Items
 */
public enum SortMode {
    DEFAULT("Standard", null), // Wird separat behandelt
    NAME_AZ("Name A-Z", Comparator.comparing(item -> item.name.toLowerCase())),
    NAME_ZA("Name Z-A", Comparator.comparing((ItemData item) -> item.name.toLowerCase()).reversed()),
    FLOOR_ASC("Ebene 1-100", Comparator.comparing(SortMode::getMinFloor)),
    FLOOR_DESC("Ebene 100-1", Comparator.comparing(SortMode::getMinFloor).reversed());
    
    private final String displayName;
    private final Comparator<ItemData> comparator;
    
    SortMode(String displayName, Comparator<ItemData> comparator) {
        this.displayName = displayName;
        this.comparator = comparator;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Comparator<ItemData> getComparator() {
        if (this == DEFAULT) {
            return getDefaultComparator();
        }
        return comparator;
    }
    
    private static Comparator<ItemData> getDefaultComparator() {
        return (item1, item2) -> {
            // Hole Kategorie-Index von jedem Item
            int categoryIndex1 = getCategoryIndex(item1);
            int categoryIndex2 = getCategoryIndex(item2);
            
            // Vergleiche nach Kategorie-Reihenfolge
            int compare = Integer.compare(categoryIndex1, categoryIndex2);
            if (compare != 0) return compare;
            
            // Gleiche Kategorie: nach Name A-Z
            return item1.name.compareToIgnoreCase(item2.name);
        };
    }
    
    // Kategorien-Reihenfolge für Sortierung
    private static final List<String> CATEGORY_ORDER = java.util.Arrays.asList(
        "blueprints",
        "abilities",
        "modules",
        "module_bags",
        "runes",
        "power_crystals",
        "power_crystal_slots",
        "card_slots",
        "essences",
        "items" // Fallback für alte Struktur
    );
    
    /**
     * Gibt den Kategorie-Index für ein Item zurück (für Sortierung)
     * @param item Das Item
     * @return Index der Kategorie basierend auf category-Feld, 999 wenn keine Kategorie gefunden
     */
    private static int getCategoryIndex(ItemData item) {
        if (item.category == null || item.category.isEmpty()) {
            return 999; // Keine Kategorie
        }
        
        String category = item.category.toLowerCase();
        int index = CATEGORY_ORDER.indexOf(category);
        
        if (index != -1) {
            return index;
        }
        
        return 999; // Kategorie nicht in der Liste
    }
    
    private static int getMinFloor(ItemData item) {
        // Extrahiere minimale Floor-Nummer aus foundAt
        // Unterstützt sowohl "floor_X" als auch "Ebene X" Formate
        if (item.foundAt == null || item.foundAt.isEmpty()) return 999;
        
        int minFloor = 999;
        for (LocationData location : item.foundAt) {
            if (location.floor != null && !location.floor.isEmpty()) {
                try {
                    int floorNum = -1;
                    String floorStr = location.floor.toLowerCase();
                    
                    // Prüfe "floor_X" Format
                    if (floorStr.startsWith("floor_")) {
                        floorNum = Integer.parseInt(floorStr.substring(6));
                    }
                    // Prüfe "Ebene X" Format
                    else if (floorStr.contains("ebene")) {
                        // Extrahiere Zahl aus "Ebene X" oder "ebene X"
                        String floorNumStr = floorStr.replaceAll("[^0-9]", "");
                        if (!floorNumStr.isEmpty()) {
                            floorNum = Integer.parseInt(floorNumStr);
                        }
                    }
                    // Prüfe ob es nur eine Zahl ist
                    else {
                        String floorNumStr = floorStr.replaceAll("[^0-9]", "");
                        if (!floorNumStr.isEmpty()) {
                            floorNum = Integer.parseInt(floorNumStr);
                        }
                    }
                    
                    if (floorNum > 0) {
                        minFloor = Math.min(minFloor, floorNum);
                    }
                } catch (NumberFormatException e) {
                    // Ignoriere
                }
            }
        }
        return minFloor;
    }
}


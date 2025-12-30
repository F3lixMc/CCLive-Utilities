package net.felix.utilities.ItemViewer;

import java.util.Arrays;
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
    
    private static final List<String> DEFAULT_TAG_ORDER = Arrays.asList(
        "armor", "weapon", "tool", "module", "rune", "ability"
    );
    
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
            // Hole ersten Tag von jedem Item (in der definierten Reihenfolge)
            String tag1 = getFirstTagInOrder(item1);
            String tag2 = getFirstTagInOrder(item2);
            
            // Vergleiche nach Tag-Reihenfolge
            int index1 = DEFAULT_TAG_ORDER.indexOf(tag1);
            int index2 = DEFAULT_TAG_ORDER.indexOf(tag2);
            
            // Beide haben einen Tag in der Liste
            if (index1 != -1 && index2 != -1) {
                int compare = Integer.compare(index1, index2);
                if (compare != 0) return compare;
                // Gleicher Tag: nach Name A-Z
                return item1.name.compareToIgnoreCase(item2.name);
            }
            
            // Nur item1 hat einen Tag → kommt zuerst
            if (index1 != -1) return -1;
            
            // Nur item2 hat einen Tag → kommt zuerst
            if (index2 != -1) return 1;
            
            // Beide haben keinen passenden Tag: nach Name A-Z
            return item1.name.compareToIgnoreCase(item2.name);
        };
    }
    
    private static String getFirstTagInOrder(ItemData item) {
        if (item.tags == null || item.tags.isEmpty()) return null;
        
        // Finde ersten Tag, der in DEFAULT_TAG_ORDER vorkommt
        for (String tag : item.tags) {
            String lowerTag = tag.toLowerCase();
            if (DEFAULT_TAG_ORDER.contains(lowerTag)) {
                return lowerTag;
            }
        }
        
        return null;
    }
    
    private static int getMinFloor(ItemData item) {
        // Extrahiere minimale Floor-Nummer aus foundAt
        if (item.foundAt == null || item.foundAt.isEmpty()) return 999;
        
        int minFloor = 999;
        for (LocationData location : item.foundAt) {
            if (location.floor != null && location.floor.startsWith("floor_")) {
                try {
                    int floorNum = Integer.parseInt(location.floor.substring(6));
                    minFloor = Math.min(minFloor, floorNum);
                } catch (NumberFormatException e) {
                    // Ignoriere
                }
            }
        }
        return minFloor;
    }
}


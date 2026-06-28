package net.felix.utilities.ItemViewer;

import java.util.Comparator;
import java.util.List;

/**
 * Sortier-Modi für Items
 */
public enum SortMode {
    DEFAULT("Standard", null), // Wird separat behandelt
    NAME_AZ("Name A-Z", SortMode::compareNameAz),
    NAME_ZA("Name Z-A", SortMode::compareNameZa),
    FLOOR_ASC("Ebene 1-100", Comparator.comparing(SortMode::getMinFloor)),
    FLOOR_DESC("Ebene 100-1", Comparator.comparing(SortMode::getMinFloor).reversed()),
    NOT_FOUND("Nicht Gefunden", null);
    
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
        if (this == DEFAULT || this == NOT_FOUND) {
            return getDefaultComparator();
        }
        return comparator;
    }

    public boolean filtersNotFoundOnly() {
        return this == NOT_FOUND;
    }
    
    private static Comparator<ItemData> getDefaultComparator() {
        return (item1, item2) -> {
            // Hole Kategorie-Index von jedem Item
            int categoryIndex1 = getCategoryIndex(item1);
            int categoryIndex2 = getCategoryIndex(item2);
            
            // Vergleiche nach Kategorie-Reihenfolge
            int compare = Integer.compare(categoryIndex1, categoryIndex2);
            if (compare != 0) return compare;
            
            // Gleiche Kategorie: spezielle Reihenfolge, sonst Name A-Z
            int withinCategory = compareWithinCategory(item1, item2);
            if (withinCategory != 0) {
                return withinCategory;
            }
            return item1.name.compareToIgnoreCase(item2.name);
        };
    }

    private static int compareNameAz(ItemData item1, ItemData item2) {
        int withinCategory = compareWithinCategory(item1, item2);
        if (withinCategory != 0) {
            return withinCategory;
        }
        String name1 = item1.name != null ? item1.name : "";
        String name2 = item2.name != null ? item2.name : "";
        return name1.compareToIgnoreCase(name2);
    }

    private static int compareNameZa(ItemData item1, ItemData item2) {
        int withinCategory = compareWithinCategory(item1, item2);
        if (withinCategory != 0) {
            return -withinCategory;
        }
        String name1 = item1.name != null ? item1.name : "";
        String name2 = item2.name != null ? item2.name : "";
        return name2.compareToIgnoreCase(name1);
    }

    private static int compareWithinCategory(ItemData item1, ItemData item2) {
        if (item1.category == null || item2.category == null
                || !item1.category.equalsIgnoreCase(item2.category)) {
            return 0;
        }
        if ("card_slots".equalsIgnoreCase(item1.category)) {
            return Integer.compare(getCardSlotOrderIndex(item1), getCardSlotOrderIndex(item2));
        }
        if ("fish_traps".equalsIgnoreCase(item1.category)) {
            return Integer.compare(getFishTrapOrderIndex(item1), getFishTrapOrderIndex(item2));
        }
        if ("licence".equalsIgnoreCase(item1.category)) {
            return Integer.compare(getLicenseOrderIndex(item1), getLicenseOrderIndex(item2));
        }
        return 0;
    }

    private static int getCardSlotOrderIndex(ItemData item) {
        if (item.tags != null) {
            for (String tag : item.tags) {
                if (tag != null && tag.startsWith("#")) {
                    try {
                        return Integer.parseInt(tag.substring(1).trim());
                    } catch (NumberFormatException ignored) {
                        // nächster Tag
                    }
                }
            }
        }
        if (item.name != null) {
            int hashIndex = item.name.lastIndexOf('#');
            if (hashIndex >= 0 && hashIndex < item.name.length() - 1) {
                try {
                    return Integer.parseInt(item.name.substring(hashIndex + 1).trim());
                } catch (NumberFormatException ignored) {
                    // Fallback unten
                }
            }
        }
        return 999;
    }

    private static final List<String> FISH_TRAP_ORDER = java.util.Arrays.asList(
        "Jungelholz Fischreuse",
        "Fichtenholzreuse",
        "Bambusholz Fischreuse",
        "Pilzholz Fischreuse",
        "Dunkeleichenholz Fischreuse",
        "Mangrovenholz Fischreuse",
        "Karmesinholz Fischreuse",
        "Wirrwarr Holz Fischreuse"
    );

    private static int getFishTrapOrderIndex(ItemData item) {
        if (item.name == null || item.name.isEmpty()) {
            return 999;
        }
        int index = FISH_TRAP_ORDER.indexOf(item.name);
        return index != -1 ? index : 999;
    }

    private static final List<String> LICENSE_ORDER = java.util.Arrays.asList(
            "Eichenholz Lizenz",
            "Smaragdsee",
            "Kohlemine Lizenz",
            "Rußwasserteich",
            "Kupfermine Lizenz",
            "Erzschimmer See",
            "Dschungelholz/Strand Lizenz",
            "Perlenbucht",
            "Fichtenwald Lizenz",
            "Nebelsee",
            "Bambuswald Lizenz",
            "Eisenmine Lizenz",
            "Blutwasser See",
            "Pilzholzwald Lizenz",
            "Sporenweiher",
            "Dunkles Eichenholz Lizenz",
            "Nachtschattensee",
            "Goldmine Lizenz",
            "Goldsee",
            "Mangrovensumpf Lizenz",
            "Wurzelbucht",
            "Diamantmine Lizenz",
            "Schwefelfelder Lizenz",
            "Schwefeldampfsee",
            "Quartzmine Lizenz",
            "Obsidianmine Lizenz",
            "Karmesinholzwald Lizenz",
            "Infernoweiher",
            "Wirrholzwald Lizenz",
            "Antike Mine Lizenz",
            "Lavaquellsee",
            "Echokristallminen Lizenz"
    );

    private static int getLicenseOrderIndex(ItemData item) {
        if (item.name == null || item.name.isEmpty()) {
            return 999;
        }
        int index = LICENSE_ORDER.indexOf(item.name);
        return index != -1 ? index : 999;
    }
    
    // Kategorien-Reihenfolge für Sortierung
    private static final List<String> CATEGORY_ORDER = java.util.Arrays.asList(
        "blueprints",
        "fishing_components",
        "fish_traps",
        "abilities",
        "modules",
        "module_bags",
        "runes",
        "power_crystals",
        "power_crystal_slots",
        "card_slots",
        "licence",
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


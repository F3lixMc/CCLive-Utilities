package net.felix.utilities.ItemViewer;

/**
 * Filter-Logik für Items
 */
public class ItemFilter {
    
    public static boolean matchesSearch(ItemData item, SearchQuery query) {
        // Tags: Alle müssen matchen (AND)
        if (!query.tags.isEmpty()) {
            if (item.tags == null || !item.tags.containsAll(query.tags)) {
                return false;
            }
        }
        
        // Aspekt
        if (query.aspect != null) {
            if (item.info == null || item.info.aspect == null ||
                !item.info.aspect.equalsIgnoreCase(query.aspect)) {
                return false;
            }
        }
        
        // Name
        if (!query.nameSearch.isEmpty()) {
            if (item.name == null ||
                !item.name.toLowerCase().contains(query.nameSearch.toLowerCase())) {
                return false;
            }
        }
        
        // Kosten-Filter
        for (CostFilter filter : query.costFilters) {
            if (!matchesCostFilter(item, filter)) {
                return false;
            }
        }
        
        return true;
    }
    
    private static boolean matchesCostFilter(ItemData item, CostFilter filter) {
        if (item.price == null) return false;
        
        CostItem costItem = null;
        
        switch (filter.category.toLowerCase()) {
            case "amboss":
                costItem = item.price.Amboss;
                break;
            case "ressource":
                costItem = item.price.Ressource;
                break;
            case "material1":
                costItem = item.price.material1;
                break;
            case "material2":
                costItem = item.price.material2;
                break;
            case "cactus":
                costItem = item.price.cactus;
                break;
            case "soul":
            case "seele":
                costItem = item.price.soul;
                break;
            case "coin":
            case "coins":
                costItem = item.price.coin;
                break;
        }
        
        if (costItem == null) return false;
        
        // Prüfe Betrag (kann Integer oder String sein)
        if (filter.amount != null) {
            if (costItem.amount == null) {
                return false;
            }
            // Vergleiche als String, da amount auch formatierte Strings sein kann
            if (!costItem.amount.toString().equals(filter.amount.toString())) {
                return false;
            }
        }
        
        // Prüfe Item-Name
        if (filter.itemName != null) {
            if (costItem.itemName == null ||
                !costItem.itemName.equalsIgnoreCase(filter.itemName)) {
                return false;
            }
        }
        
        return true;
    }
}


package net.felix.utilities.ItemViewer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter-Logik für Items
 */
public class ItemFilter {
    
    private static final java.util.Set<String> OFEN_MATERIALS = java.util.Set.of(
            "Netherite Schrott",
            "Eisenbarren",
            "Goldbarren",
            "Kupferbarren"
    );
    
    public static boolean matchesSearch(ItemData item, SearchQuery query) {
        // Tags: Alle müssen matchen (AND)
        // Vollständiger Tag (#Schuhe) → exakt; Prefix (#Schuh) → Teilstring (Live-Suche)
        if (!query.tags.isEmpty()) {
            // Erstelle eine kombinierte Liste aller Tags (aus item.tags, info.type, info.piece, info.rarity)
            java.util.List<String> allItemTags = new java.util.ArrayList<>();
            
            // Füge Tags aus item.tags hinzu
            if (item.tags != null) {
                for (String tag : item.tags) {
                    if (tag != null && !tag.isEmpty()) {
                        allItemTags.add(tag);
                    }
                }
            }
            
            // Füge info.type, info.piece, info.rarity als Tags hinzu (wie in getAllTagsForCategory)
            if (item.info != null) {
                if (item.info.type != null && !item.info.type.isEmpty()) {
                    allItemTags.add(item.info.type);
                }
                if (item.info.piece != null && !item.info.piece.isEmpty()) {
                    allItemTags.add(item.info.piece);
                }
                if (item.info.rarity != null && !item.info.rarity.isEmpty()) {
                    allItemTags.add(item.info.rarity);
                }
            }
            
            // Wenn keine Tags vorhanden sind, kann der gesuchte Tag nicht matchen
            if (allItemTags.isEmpty()) {
                return false;
            }
            
            for (String searchTag : query.tags) {
                boolean tagMatches = false;
                for (String itemTag : allItemTags) {
                    if (ItemViewerUtility.matchesSearchTag(itemTag, searchTag)) {
                        tagMatches = true;
                        break;
                    }
                }
                if (!tagMatches) {
                    return false;
                }
            }
        }
        
        // Aspekt-Suche (ähnlich wie Floor-Suche)
        if (query.aspect != null && !query.aspect.isEmpty()) {
            boolean aspectMatches = false;
            String queryAspectLower = query.aspect.toLowerCase().trim();
            
            // Prüfe zuerst item.info.aspect (falls vorhanden)
            if (item.info != null && item.info.aspect != null) {
                String itemAspect = item.info.aspect;
                // Unterstütze Teilstring-Matching für Live-Suche
                if (itemAspect.equalsIgnoreCase(queryAspectLower) || 
                    itemAspect.toLowerCase().contains(queryAspectLower)) {
                    aspectMatches = true;
                }
            }
            
            // Wenn nicht gefunden und es ein Blueprint ist, hole Aspekt dynamisch aus dem Namen
            if (!aspectMatches && item.info != null && Boolean.TRUE.equals(item.info.blueprint) && item.name != null) {
                // Entferne Formatierungscodes für Vergleich
                String cleanItemName = item.name.replaceAll("§[0-9a-fk-or]", "");
                String aspectName = net.felix.utilities.Overall.InformationenUtility.getAspectInfoForBlueprint(cleanItemName);
                if (aspectName != null && !aspectName.isEmpty()) {
                    // Unterstütze Teilstring-Matching für Live-Suche
                    String aspectNameLower = aspectName.toLowerCase();
                    if (aspectName.equalsIgnoreCase(queryAspectLower) || 
                        aspectNameLower.contains(queryAspectLower)) {
                        aspectMatches = true;
                    }
                }
            }
            
            if (!aspectMatches) {
                return false;
            }
        }
        
        // Floor-Suche (explizit: "Ebene 49", "e4", "[e4]", etc.)
        if (query.floor != null) {
            boolean floorMatches = false;
            for (int floorNum : FloorNumberExtractor.extractFromItem(item)) {
                if (floorNum == query.floor) {
                    floorMatches = true;
                    break;
                }
            }
            if (!floorMatches) {
                return false;
            }
        }
        
        // Floor-Vergleichs-Filter (z.B. @Ebene>50, @Ebene<50, etc.)
        for (FloorFilter filter : query.floorFilters) {
            if (!matchesFloorFilter(item, filter)) {
                return false;
            }
        }
        
        // Name und Floor (im Namen)
        if (!query.nameSearch.isEmpty()) {
            boolean nameMatches = false;
            String searchLower = query.nameSearch.toLowerCase();
            String searchExact = query.nameSearch;
            
            // Prüfe Item-Name
            if (item.name != null &&
                item.name.toLowerCase().contains(searchLower)) {
                nameMatches = true;
            }
            
            // Prüfe Modifier (z.B. "Fähigkeit" findet "Fähigkeiten")
            if (!nameMatches && item.info != null && item.info.modifier != null) {
                for (String modifier : item.info.modifier) {
                    if (modifier != null && !modifier.isEmpty() &&
                        modifier.toLowerCase().contains(searchLower)) {
                        nameMatches = true;
                        break;
                    }
                }
            }
            
            // Prüfe Fähigkeiten-Items (Kategorie abilities / info.ability)
            if (!nameMatches && item.info != null &&
                (searchLower.contains("fähigkeit") || searchLower.contains("faehigkeit") || searchLower.contains("ability"))) {
                if (Boolean.TRUE.equals(item.info.ability) ||
                    "abilities".equals(item.category)) {
                    nameMatches = true;
                }
            }
            
            // Prüfe Aspekt (ähnlich wie im Blueprint Shop) - nur wenn Name nicht matched
            if (!nameMatches && item.info != null && Boolean.TRUE.equals(item.info.blueprint) && item.name != null) {
                // Entferne Formatierungscodes für Vergleich
                String cleanItemName = item.name.replaceAll("§[0-9a-fk-or]", "");
                String aspectName = net.felix.utilities.Overall.InformationenUtility.getAspectInfoForBlueprint(cleanItemName);
                if (aspectName != null && !aspectName.isEmpty()) {
                    String aspectLower = aspectName.toLowerCase();
                    // Prüfe ob Aspekt-Name den Suchtext enthält (wie im Blueprint Shop)
                    if (aspectName.contains(searchExact) || aspectLower.contains(searchLower)) {
                        nameMatches = true;
                    }
                }
            }
            
            // Prüfe Floor (Ebene) in foundAt und [eX] im Namen
            if (!nameMatches) {
                for (int floorNum : FloorNumberExtractor.extractFromItem(item)) {
                    String floorStr = String.valueOf(floorNum);
                    if (floorStr.contains(searchExact) || floorStr.toLowerCase().contains(searchLower)) {
                        nameMatches = true;
                        break;
                    }
                }
            }
            
            if (!nameMatches) {
                return false;
            }
        }
        
        // Kosten-Filter (pro Kategorie kombiniert – z.B. material:Spinnfaden + material:<50 am selben Slot)
        Map<String, List<CostFilter>> costFiltersByCategory = new LinkedHashMap<>();
        for (CostFilter filter : query.costFilters) {
            if (filter.category == null) {
                continue;
            }
            String category = normalizeCostCategory(filter.category);
            costFiltersByCategory.computeIfAbsent(category, key -> new ArrayList<>()).add(filter);
        }
        for (Map.Entry<String, List<CostFilter>> entry : costFiltersByCategory.entrySet()) {
            if (!matchesCombinedCostFilters(item, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        
        // Modifier-Filter
        for (ModifierFilter filter : query.modifierFilters) {
            if (!matchesModifierFilter(item, filter)) {
                return false;
            }
        }
        
        // Stat-Filter (z.B. @Abbaugeschwindigkeit>100)
        for (StatFilter filter : query.statFilters) {
            if (!matchesStatFilter(item, filter)) {
                return false;
            }
        }
        
        return true;
    }
    
    private static String normalizeCostCategory(String category) {
        String normalized = category.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("material") && !normalized.equals("material")) {
            return "material";
        }
        return normalized;
    }

    private static boolean matchesCombinedCostFilters(ItemData item, String category, List<CostFilter> filters) {
        if (filters.isEmpty()) {
            return true;
        }
        if ("material".equals(category)) {
            return matchesCombinedMaterialFilters(item, filters);
        }
        if (item.price == null) {
            return false;
        }
        CostItem costItem = getCostItemForCategory(item.price, category, item);
        for (CostFilter filter : filters) {
            if (!matchesSingleCostItem(costItem, filter)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesCombinedMaterialFilters(ItemData item, List<CostFilter> filters) {
        if (filters.size() == 1) {
            CostFilter only = filters.get(0);
            if (only.amount != null && only.amount == 0 && only.itemName == null && isExactAmountOperator(only)) {
                return matchesMaterialZeroFilter(item);
            }
        }
        java.util.List<CostItem> materials = getMaterialCostItems(item.price);
        if (materials.isEmpty()) {
            for (CostFilter filter : filters) {
                if (!matchesSingleCostItem(null, filter)) {
                    return false;
                }
            }
            return true;
        }
        for (CostItem material : materials) {
            boolean allMatch = true;
            for (CostFilter filter : filters) {
                if (!matchesSingleCostItem(material, filter)) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExactAmountOperator(CostFilter filter) {
        return filter.amountOperator == null || "=".equals(filter.amountOperator);
    }

    private static boolean matchesMaterialZeroFilter(ItemData item) {
        java.util.List<CostItem> materials = getMaterialCostItems(item.price);
        if (materials.isEmpty()) {
            return true;
        }
        CostFilter zeroFilter = new CostFilter();
        zeroFilter.amount = 0;
        for (CostItem material : materials) {
            if (!matchesSingleCostItem(material, zeroFilter)) {
                return false;
            }
        }
        return true;
    }
    
    private static CostItem getCostItemForCategory(PriceData price, String category, ItemData item) {
        return switch (category) {
            case "amboss" -> price.Amboss != null ? price.Amboss : price.amboss;
            case "ressource" -> price.Ressource != null ? price.Ressource : price.ressource;
            case "material1" -> price.material1;
            case "material2" -> price.material2;
            case "material3" -> price.material3;
            case "material4" -> price.material4;
            case "material5" -> price.material5;
            case "cactus", "kaktus" -> price.cactus;
            case "soul", "seele" -> price.soul;
            case "coin", "coins" -> price.coin;
            case "ofen" -> getOfenCostItem(item);
            default -> null;
        };
    }
    
    private static java.util.List<CostItem> getMaterialCostItems(PriceData price) {
        java.util.List<CostItem> materials = new java.util.ArrayList<>();
        if (price == null) {
            return materials;
        }
        if (price.material1 != null) materials.add(price.material1);
        if (price.material2 != null) materials.add(price.material2);
        if (price.material3 != null) materials.add(price.material3);
        if (price.material4 != null) materials.add(price.material4);
        if (price.material5 != null) materials.add(price.material5);
        return materials;
    }
    
    private static boolean matchesSingleCostItem(CostItem costItem, CostFilter filter) {
        if (filter.amount != null) {
            if (filter.amount == 0 && isExactAmountOperator(filter)) {
                if (costItem == null) {
                    return true;
                }
                if (costItem.amount != null) {
                    String amountStr = costItem.amount.toString().trim();
                    if (!amountStr.isEmpty() && !amountStr.equals("0")) {
                        return false;
                    }
                }
                return true;
            }
            
            if (costItem == null || costItem.amount == null) {
                return false;
            }
            Integer itemAmount = parseCostItemAmount(costItem.amount);
            if (itemAmount == null) {
                return false;
            }
            String operator = filter.amountOperator != null ? filter.amountOperator : "=";
            if (!ComparisonUtils.compareInt(itemAmount, filter.amount, operator)) {
                return false;
            }
        }
        
        if (filter.itemName != null) {
            if (costItem == null || costItem.itemName == null) {
                return false;
            }
            if (!ItemViewerUtility.matchesCostItemName(costItem.itemName, filter.itemName)) {
                return false;
            }
        }
        
        if (filter.amount == null && filter.itemName == null) {
            return costItem != null;
        }
        
        return true;
    }

    private static Integer parseCostItemAmount(Object amount) {
        if (amount == null) {
            return null;
        }
        if (amount instanceof Number number) {
            return number.intValue();
        }
        String amountStr = amount.toString().trim();
        if (amountStr.isEmpty()) {
            return null;
        }
        Matcher matcher = Pattern.compile("^(\\d+)").matcher(amountStr.replace(",", "").replace(".", ""));
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
    
    private static boolean isOfenMaterial(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return false;
        }
        for (String ofenMaterial : OFEN_MATERIALS) {
            if (ofenMaterial.equalsIgnoreCase(itemName)) {
                return true;
            }
        }
        return false;
    }
    
    private static CostItem getOfenCostItem(ItemData item) {
        if (item.price == null) {
            return null;
        }
        if (item.price.Ressource != null && isOfenMaterial(item.price.Ressource.itemName)) {
            return item.price.Ressource;
        }
        if (item.price.ressource != null && isOfenMaterial(item.price.ressource.itemName)) {
            return item.price.ressource;
        }
        return null;
    }
    
    private static boolean matchesModifierFilter(ItemData item, ModifierFilter filter) {
        if (item.info == null || item.info.modifier == null || item.info.modifier.isEmpty()) {
            return false;
        }
        
        // Zähle wie oft der Modifier vorkommt (unterstützt auch Teilstring-Matches)
        int count = 0;
        for (String modifier : item.info.modifier) {
            if (modifier != null && !modifier.isEmpty()) {
                // Exakter Match oder Teilstring-Match (case-insensitive)
                if (modifier.equalsIgnoreCase(filter.modifier) || 
                    modifier.toLowerCase().contains(filter.modifier.toLowerCase())) {
                    count++;
                }
            }
        }
        
        // Wenn keine Anzahl angegeben wurde, prüfe nur ob Modifier vorhanden ist
        if (filter.count == null) {
            return count > 0;
        }
        
        // Wenn Anzahl angegeben wurde, prüfe ob exakt diese Anzahl vorhanden ist
        return count == filter.count;
    }
    
    private static boolean matchesStatFilter(ItemData item, StatFilter filter) {
        if (filter.statName != null && "itemscore".equalsIgnoreCase(filter.statName.trim())) {
            return matchesItemScoreFilter(item, filter);
        }

        if (item.info == null || item.info.stats == null) {
            return false;
        }
        
        // Parse Stats (kann String oder List<String> sein)
        List<String> statsList = new ArrayList<>();
        if (item.info.stats instanceof String) {
            String statsStr = (String) item.info.stats;
            if (!statsStr.isEmpty()) {
                statsList.add(statsStr);
            }
        } else if (item.info.stats instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> statsObjList = (List<Object>) item.info.stats;
            for (Object statObj : statsObjList) {
                if (statObj != null) {
                    statsList.add(statObj.toString());
                }
            }
        }
        
        // Suche nach dem gesuchten Stat
        for (String statLine : statsList) {
            if (statLine == null || statLine.isEmpty()) {
                continue;
            }
            
            // Entferne Farbcodes für die Suche (wichtig für Live-Suche)
            String cleanStatLine = statLine.replaceAll("§[0-9a-fk-or]", "");
            
            // Prüfe ob Stat-Name im String enthalten ist (case-insensitive, unterstützt Live-Suche)
            // Beispiel: "Schaden" matched auch "Schad", "Sch", etc.
            String statLineLower = cleanStatLine.toLowerCase();
            String filterStatNameLower = filter.statName.toLowerCase();
            if (!statLineLower.contains(filterStatNameLower)) {
                continue;
            }
            
            // Wenn kein Operator/Wert angegeben wurde, reicht es wenn der Stat-Name gefunden wurde (Live-Suche)
            if (filter.operator == null || filter.value == null) {
                return true; // Stat-Name gefunden, keine weitere Prüfung nötig
            }
            
            // Finde die Position des Stat-Namens im bereinigten String
            int statNameIndex = statLineLower.indexOf(filterStatNameLower);
            if (statNameIndex < 0) {
                continue;
            }
            
            // Extrahiere den Wert aus dem Stat-String
            // Wichtig: Verwende die Position des gefundenen Teilstrings, aber suche nach der nächsten Zahl
            // Format: "StatName Wert" oder "StatName Wert,XXX" (mit Komma als Tausendertrennzeichen)
            // Beispiele: "Abbaugeschwindigkeit 11", "Schaden 9,011", "Rüstung 127"
            // Für Live-Suche: Finde die nächste Zahl nach dem gefundenen Teilstring
            String valuePart = cleanStatLine.substring(statNameIndex + filterStatNameLower.length()).trim();
            
            // Entferne alle nicht-numerischen Zeichen am Anfang (außer Minus für negative Zahlen)
            valuePart = valuePart.replaceAll("^[^0-9\\-]+", "");
            
            // Suche nach der ersten Zahl im String (unterstützt verschiedene Formate)
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]+)?|[0-9]+(?:[.,][0-9]+)?)");
            java.util.regex.Matcher matcher = pattern.matcher(valuePart);
            
            if (matcher.find()) {
                String numberStr = matcher.group(1);
                
                // Parse die Zahl
                if (numberStr.contains(",") && numberStr.contains(".")) {
                    numberStr = numberStr.replace(",", "");
                } else if (numberStr.contains(",")) {
                    int lastComma = numberStr.lastIndexOf(',');
                    if (lastComma >= 0 && numberStr.length() - lastComma - 1 > 2) {
                        numberStr = numberStr.replace(",", "");
                    } else {
                        numberStr = numberStr.replace(',', '.');
                    }
                } else if (numberStr.contains(".")) {
                    int lastDot = numberStr.lastIndexOf('.');
                    if (lastDot >= 0 && numberStr.length() - lastDot - 1 > 2) {
                        numberStr = numberStr.replace(".", "");
                    }
                }
                
                try {
                    Double statValue = Double.parseDouble(numberStr);
                    // Vergleiche Werte basierend auf Operator
                    return compareStatValue(statValue, filter.value, filter.operator);
                } catch (NumberFormatException e) {
                    // Ignoriere
                }
            }
        }
        
        return false;
    }

    private static boolean matchesItemScoreFilter(ItemData item, StatFilter filter) {
        if (item.itemScore == null || item.itemScore.isBlank()
                || "NaN".equalsIgnoreCase(item.itemScore.trim())) {
            return false;
        }

        try {
            double score = Double.parseDouble(item.itemScore.trim().replace(',', '.'));
            if (Double.isNaN(score)) {
                return false;
            }
            if (filter.operator == null || filter.value == null) {
                return true;
            }
            return compareStatValue(score, filter.value, filter.operator);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Vergleicht zwei Stat-Werte basierend auf dem Operator
     * @param itemValue Der Wert des Items
     * @param filterValue Der Vergleichswert aus dem Filter
     * @param operator Der Vergleichsoperator (">", "<", ">=", "<=", "=")
     * @return true wenn der Vergleich erfolgreich ist
     */
    private static boolean compareStatValue(Double itemValue, Double filterValue, String operator) {
        if (itemValue == null || filterValue == null) {
            return false;
        }
        return ComparisonUtils.compareDouble(itemValue, filterValue, operator);
    }
    
    /**
     * Prüft ob ein Item einem Floor-Filter mit Vergleichsoperator entspricht
     * @param item Das zu prüfende Item
     * @param filter Der Floor-Filter mit Operator und Wert
     * @return true wenn das Item dem Filter entspricht
     */
    private static boolean matchesFloorFilter(ItemData item, FloorFilter filter) {
        if (filter.value == null || filter.operator == null) {
            return false;
        }
        
        java.util.List<Integer> itemFloors = FloorNumberExtractor.extractFromItem(item);
        if (itemFloors.isEmpty()) {
            return false;
        }
        
        for (int floorNum : itemFloors) {
            if (ComparisonUtils.compareInt(floorNum, filter.value, filter.operator)) {
                return true;
            }
        }
        
        return false;
    }
}


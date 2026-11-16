package net.felix.features.itemviewer.search;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.felix.features.itemviewer.data.CostItem;
import net.felix.features.itemviewer.data.ItemData;
import net.felix.features.itemviewer.data.PriceData;

/**
 * Filter-Logik für Items
 */
public class ItemFilter {
    
    private static final Pattern STAT_NUMBER_PATTERN =
            Pattern.compile("([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]+)?|[0-9]+(?:[.,][0-9]+)?)");

    private static final java.util.Set<String> OFEN_MATERIALS = java.util.Set.of(
            "Netherite Schrott",
            "Eisenbarren",
            "Goldbarren",
            "Kupferbarren"
    );
    
    public static boolean matchesSearch(ItemData item, SearchQuery query) {
        if (!query.tags.isEmpty() && !matchesAllTags(item, query.tags)) {
            return false;
        }
        if (query.aspect != null && !query.aspect.isEmpty() && !matchesAspectSearch(item, query.aspect)) {
            return false;
        }
        if (query.floor != null && !FloorNumberExtractor.extractFromItem(item).contains(query.floor)) {
            return false;
        }
        // Floor-Vergleichs-Filter (z.B. @Ebene>50, @Ebene<50, etc.)
        for (FloorFilter filter : query.floorFilters) {
            if (!matchesFloorFilter(item, filter)) {
                return false;
            }
        }
        if (!query.nameSearch.isEmpty() && !matchesNameSearch(item, query.nameSearch)) {
            return false;
        }
        if (!matchesAllCostFilters(item, query.costFilters)) {
            return false;
        }
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

    /**
     * Tags: Alle müssen matchen (AND).
     * Vollständiger Tag (#Schuhe) → exakt; Prefix (#Schuh) → Teilstring (Live-Suche).
     */
    private static boolean matchesAllTags(ItemData item, List<String> searchTags) {
        List<String> allItemTags = collectItemTags(item);
        // Wenn keine Tags vorhanden sind, kann der gesuchte Tag nicht matchen
        if (allItemTags.isEmpty()) {
            return false;
        }
        for (String searchTag : searchTags) {
            boolean tagMatches = false;
            for (String itemTag : allItemTags) {
                if (SearchSuggestions.matchesSearchTag(itemTag, searchTag)) {
                    tagMatches = true;
                    break;
                }
            }
            if (!tagMatches) {
                return false;
            }
        }
        return true;
    }

    /** Alle Tags eines Items: item.tags sowie info.type, info.piece und info.rarity (wie in getAllTagsForCategory). */
    private static List<String> collectItemTags(ItemData item) {
        List<String> allItemTags = new ArrayList<>();
        if (item.tags != null) {
            for (String tag : item.tags) {
                if (tag != null && !tag.isEmpty()) {
                    allItemTags.add(tag);
                }
            }
        }
        if (item.info != null) {
            addIfNotEmpty(allItemTags, item.info.type);
            addIfNotEmpty(allItemTags, item.info.piece);
            addIfNotEmpty(allItemTags, item.info.rarity);
        }
        return allItemTags;
    }

    private static void addIfNotEmpty(List<String> target, String value) {
        if (value != null && !value.isEmpty()) {
            target.add(value);
        }
    }

    /** Aspekt-Suche (ähnlich wie Floor-Suche); Teilstring-Matching für die Live-Suche. */
    private static boolean matchesAspectSearch(ItemData item, String aspectQuery) {
        String queryAspectLower = aspectQuery.toLowerCase().trim();

        // Zuerst item.info.aspect (falls vorhanden)
        if (item.info != null && item.info.aspect != null) {
            String itemAspect = item.info.aspect;
            if (itemAspect.equalsIgnoreCase(queryAspectLower) || itemAspect.toLowerCase().contains(queryAspectLower)) {
                return true;
            }
        }

        // Bei Blueprints den Aspekt dynamisch aus dem Namen holen
        String aspectName = getBlueprintAspectName(item);
        return aspectName != null
            && (aspectName.equalsIgnoreCase(queryAspectLower) || aspectName.toLowerCase().contains(queryAspectLower));
    }

    /** Aspekt eines Blueprints aus der Aspekt-Datenbank (ohne Formatierungscodes im Namen); sonst null. */
    private static String getBlueprintAspectName(ItemData item) {
        if (item.info == null || !Boolean.TRUE.equals(item.info.blueprint) || item.name == null) {
            return null;
        }
        String cleanItemName = item.name.replaceAll("§[0-9a-fk-or]", "");
        String aspectName = net.felix.features.information.aspects.AspectDatabase.getAspectInfoForBlueprint(cleanItemName);
        return aspectName != null && !aspectName.isEmpty() ? aspectName : null;
    }

    /** Name-Suche: Itemname, Modifier, Fähigkeiten-Items, Blueprint-Aspekt oder Ebene (Floor). */
    private static boolean matchesNameSearch(ItemData item, String nameSearch) {
        String searchLower = nameSearch.toLowerCase();
        return matchesItemName(item, searchLower)
            || matchesModifierName(item, searchLower)
            || matchesAbilitySearch(item, searchLower)
            || matchesBlueprintAspectName(item, nameSearch, searchLower)
            || matchesFloorText(item, nameSearch, searchLower);
    }

    private static boolean matchesItemName(ItemData item, String searchLower) {
        return item.name != null && item.name.toLowerCase().contains(searchLower);
    }

    /** Modifier, z.B. "Fähigkeit" findet "Fähigkeiten". */
    private static boolean matchesModifierName(ItemData item, String searchLower) {
        if (item.info == null || item.info.modifier == null) {
            return false;
        }
        for (String modifier : item.info.modifier) {
            if (modifier != null && !modifier.isEmpty() && modifier.toLowerCase().contains(searchLower)) {
                return true;
            }
        }
        return false;
    }

    /** Fähigkeiten-Items (Kategorie abilities / info.ability). */
    private static boolean matchesAbilitySearch(ItemData item, String searchLower) {
        if (item.info == null) {
            return false;
        }
        boolean searchesAbility = searchLower.contains("fähigkeit") || searchLower.contains("faehigkeit")
            || searchLower.contains("ability");
        return searchesAbility && (Boolean.TRUE.equals(item.info.ability) || "abilities".equals(item.category));
    }

    /** Aspekt eines Blueprints (wie im Blueprint Shop). */
    private static boolean matchesBlueprintAspectName(ItemData item, String searchExact, String searchLower) {
        String aspectName = getBlueprintAspectName(item);
        return aspectName != null && (aspectName.contains(searchExact) || aspectName.toLowerCase().contains(searchLower));
    }

    /** Ebene (Floor) in foundAt und [eX] im Namen. */
    private static boolean matchesFloorText(ItemData item, String searchExact, String searchLower) {
        for (int floorNum : FloorNumberExtractor.extractFromItem(item)) {
            String floorStr = String.valueOf(floorNum);
            if (floorStr.contains(searchExact) || floorStr.toLowerCase().contains(searchLower)) {
                return true;
            }
        }
        return false;
    }

    /** Kosten-Filter pro Kategorie kombiniert – z.B. material:Spinnfaden + material:<50 am selben Slot. */
    private static boolean matchesAllCostFilters(ItemData item, List<CostFilter> costFilters) {
        Map<String, List<CostFilter>> costFiltersByCategory = new LinkedHashMap<>();
        for (CostFilter filter : costFilters) {
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
            if (!ValueComparison.compareInt(itemAmount, filter.amount, operator)) {
                return false;
            }
        }
        
        if (filter.itemName != null) {
            if (costItem == null || costItem.itemName == null) {
                return false;
            }
            if (!SearchSuggestions.matchesCostItemName(costItem.itemName, filter.itemName)) {
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

        String filterStatNameLower = filter.statName.toLowerCase();
        for (String statLine : collectStatLines(item.info.stats)) {
            if (statLine == null || statLine.isEmpty()) {
                continue;
            }

            // Entferne Farbcodes für die Suche (wichtig für Live-Suche)
            String cleanStatLine = statLine.replaceAll("§[0-9a-fk-or]", "");

            // Stat-Name case-insensitive suchen; Teilstrings unterstützen die Live-Suche
            // Beispiel: "Schaden" matched auch "Schad", "Sch", etc.
            String statLineLower = cleanStatLine.toLowerCase();
            int statNameIndex = statLineLower.indexOf(filterStatNameLower);
            if (statNameIndex < 0) {
                continue;
            }

            // Wenn kein Operator/Wert angegeben wurde, reicht es wenn der Stat-Name gefunden wurde (Live-Suche)
            if (filter.operator == null || filter.value == null) {
                return true;
            }

            // Wert nach dem gefundenen Stat-Namen
            // Format: "StatName Wert" oder "StatName Wert,XXX" (mit Komma als Tausendertrennzeichen)
            // Beispiele: "Abbaugeschwindigkeit 11", "Schaden 9,011", "Rüstung 127"
            String valuePart = cleanStatLine.substring(statNameIndex + filterStatNameLower.length()).trim();
            Double statValue = parseFirstNumber(valuePart);
            if (statValue != null) {
                return compareStatValue(statValue, filter.value, filter.operator);
            }
        }

        return false;
    }

    /** Stats können als String oder List vorliegen; liefert die einzelnen Zeilen. */
    private static List<String> collectStatLines(Object stats) {
        List<String> statsList = new ArrayList<>();
        if (stats instanceof String statsStr) {
            if (!statsStr.isEmpty()) {
                statsList.add(statsStr);
            }
        } else if (stats instanceof List<?> statsObjList) {
            for (Object statObj : statsObjList) {
                if (statObj != null) {
                    statsList.add(statObj.toString());
                }
            }
        }
        return statsList;
    }

    /**
     * Liest die erste Zahl am Anfang des Textes (nach nicht-numerischen Zeichen, außer Minus),
     * unterstützt Tausendertrennzeichen und Dezimalstellen mit Komma oder Punkt.
     * @return die Zahl oder null, wenn keine (gültige) Zahl gefunden wurde
     */
    private static Double parseFirstNumber(String valuePart) {
        // Entferne alle nicht-numerischen Zeichen am Anfang (außer Minus für negative Zahlen)
        valuePart = valuePart.replaceAll("^[^0-9\\-]+", "");

        Matcher matcher = STAT_NUMBER_PATTERN.matcher(valuePart);
        if (!matcher.find()) {
            return null;
        }
        String numberStr = matcher.group(1);
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
            return Double.parseDouble(numberStr);
        } catch (NumberFormatException e) {
            return null;
        }
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
        return ValueComparison.compareDouble(itemValue, filterValue, operator);
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
            if (ValueComparison.compareInt(floorNum, filter.value, filter.operator)) {
                return true;
            }
        }
        
        return false;
    }
}


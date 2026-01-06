package net.felix.utilities.ItemViewer;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter-Logik für Items
 */
public class ItemFilter {
    
    public static boolean matchesSearch(ItemData item, SearchQuery query) {
        // Tags: Alle müssen matchen (AND)
        // Unterstützt Live-Suche: @Schuhe, @Schuh, @Schu, @Sch, @Sc, @S werden alle erkannt
        if (!query.tags.isEmpty()) {
            if (item.tags == null) {
                return false;
            }
            // Für jeden gesuchten Tag prüfen, ob mindestens ein Item-Tag den gesuchten Tag enthält (case-insensitive)
            // Unterstützt Live-Suche: #gürtel, #gürt, #gür, #gü, #g werden alle erkannt
            for (String searchTag : query.tags) {
                boolean tagMatches = false;
                String searchTagLower = searchTag.toLowerCase();
                for (String itemTag : item.tags) {
                    String itemTagLower = itemTag.toLowerCase();
                    // Prüfe ob Item-Tag den gesuchten Tag enthält (case-insensitive)
                    if (itemTagLower.contains(searchTagLower)) {
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
        
        // Floor-Suche (explizit: "Ebene 49", "floor_1", etc.)
        if (query.floor != null) {
            boolean floorMatches = false;
            if (item.foundAt != null && !item.foundAt.isEmpty()) {
                for (LocationData location : item.foundAt) {
                    if (location.floor != null) {
                        String floorStr = location.floor.toLowerCase();
                        // Prüfe ob Floor-String die gesuchte Nummer enthält
                        // Unterstützt "Ebene 49", "floor_49", "49", etc.
                        if (floorStr.contains(String.valueOf(query.floor))) {
                            // Extrahiere Zahl aus Floor-String
                            String floorNumStr = floorStr.replaceAll("[^0-9]", "");
                            if (!floorNumStr.isEmpty()) {
                                try {
                                    int floorNum = Integer.parseInt(floorNumStr);
                                    if (floorNum == query.floor) {
                                        floorMatches = true;
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                    // Ignoriere
                                }
                            }
                        }
                    }
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
            
            // Prüfe Floor (Ebene) - nur wenn Name nicht matched
            if (!nameMatches && item.foundAt != null && !item.foundAt.isEmpty()) {
                for (LocationData location : item.foundAt) {
                    if (location.floor != null &&
                        location.floor.toLowerCase().contains(searchLower)) {
                        nameMatches = true;
                        break;
                    }
                }
            }
            
            if (!nameMatches) {
                return false;
            }
        }
        
        // Kosten-Filter
        for (CostFilter filter : query.costFilters) {
            if (!matchesCostFilter(item, filter)) {
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
        
        // Prüfe Betrag (kann Integer oder String sein)
        if (filter.amount != null) {
            // Wenn amount 0 ist, prüfe ob costItem leer/null ist
            if (filter.amount == 0) {
                // Wenn costItem null ist (Feld fehlt), matched es (fehlend = 0)
                if (costItem == null) {
                    return true;
                }
                // Wenn costItem.amount null, leer oder "0" ist, dann matched es
                if (costItem.amount != null) {
                    String amountStr = costItem.amount.toString().trim();
                    if (!amountStr.isEmpty() && !amountStr.equals("0")) {
                        return false;
                    }
                }
                // Wenn costItem.amount null ist, matched es auch (leer = 0)
                return true;
            } else {
                // Für andere Werte: costItem muss existieren
                if (costItem == null) {
                    return false;
                }
                if (costItem.amount == null) {
                    return false;
                }
                // Vergleiche als String, da amount auch formatierte Strings sein kann
                if (!costItem.amount.toString().equals(filter.amount.toString())) {
                    return false;
                }
            }
        }
        
        // Wenn nur Item-Name gesucht wird, muss costItem existieren
        if (filter.itemName != null) {
            if (costItem == null) {
                return false;
            }
            if (costItem.itemName == null ||
                !costItem.itemName.equalsIgnoreCase(filter.itemName)) {
                return false;
            }
        }
        
        // Wenn weder amount noch itemName gesucht wird, aber costItem existiert, matched es
        if (filter.amount == null && filter.itemName == null) {
            return costItem != null;
        }
        
        return true;
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
                
                // Parse die Zahl (gleiche Logik wie in extractStatValue)
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
    
    /**
     * Extrahiert den numerischen Wert aus einem Stat-String
     * @param statLine Der Stat-String (z.B. "Abbaugeschwindigkeit 11" oder "Schaden 9,011" oder "Schaden 1,234")
     * @param statName Der Name des Stats (z.B. "Abbaugeschwindigkeit")
     * @return Der extrahierte Wert oder null wenn nicht gefunden
     */
    private static Double extractStatValue(String statLine, String statName) {
        try {
            // Entferne Stat-Name und extrahiere den Wert
            String valuePart = statLine;
            int statNameIndex = statLine.toLowerCase().indexOf(statName.toLowerCase());
            if (statNameIndex >= 0) {
                valuePart = statLine.substring(statNameIndex + statName.length()).trim();
            }
            
            // Entferne Farbcodes
            valuePart = valuePart.replaceAll("§[0-9a-fk-or]", "");
            
            // Suche nach der ersten Zahl im String (unterstützt verschiedene Formate)
            // Pattern: findet Zahlen mit optionalen Tausendertrennzeichen
            // Beispiele: "11", "9,011", "1,234", "1234", "1.234", "1,234.56"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]+)?|[0-9]+(?:[.,][0-9]+)?)");
            java.util.regex.Matcher matcher = pattern.matcher(valuePart);
            
            if (matcher.find()) {
                String numberStr = matcher.group(1);
                
                // Prüfe ob es Tausendertrennzeichen gibt (mehrere Trennzeichen)
                if (numberStr.contains(",") && numberStr.contains(".")) {
                    // Beide vorhanden: Punkt ist Dezimaltrennzeichen, Komma ist Tausendertrennzeichen
                    // Beispiel: "1,234.56" -> "1234.56"
                    numberStr = numberStr.replace(",", "");
                } else if (numberStr.contains(",")) {
                    // Nur Komma: Prüfe ob Tausendertrennzeichen oder Dezimaltrennzeichen
                    // Wenn nach dem letzten Komma mehr als 2 Ziffern kommen, ist es Tausendertrennzeichen
                    int lastComma = numberStr.lastIndexOf(',');
                    if (lastComma >= 0 && numberStr.length() - lastComma - 1 > 2) {
                        // Tausendertrennzeichen: Entferne alle Kommas
                        numberStr = numberStr.replace(",", "");
                    } else {
                        // Dezimaltrennzeichen: Ersetze durch Punkt
                        numberStr = numberStr.replace(',', '.');
                    }
                } else if (numberStr.contains(".")) {
                    // Nur Punkt: Prüfe ob Tausendertrennzeichen oder Dezimaltrennzeichen
                    // Wenn nach dem letzten Punkt mehr als 2 Ziffern kommen, ist es Tausendertrennzeichen
                    int lastDot = numberStr.lastIndexOf('.');
                    if (lastDot >= 0 && numberStr.length() - lastDot - 1 > 2) {
                        // Tausendertrennzeichen: Entferne alle Punkte
                        numberStr = numberStr.replace(".", "");
                    }
                    // Sonst ist es bereits eine Dezimalzahl mit Punkt
                }
                
                return Double.parseDouble(numberStr);
            }
            
            return null;
        } catch (NumberFormatException e) {
            return null;
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
        
        switch (operator) {
            case ">":
                return itemValue > filterValue;
            case "<":
                return itemValue < filterValue;
            case ">=":
                return itemValue >= filterValue;
            case "<=":
                return itemValue <= filterValue;
            case "=":
            case "==":
                // Verwende eine kleine Toleranz für Gleitkomma-Vergleiche
                return Math.abs(itemValue - filterValue) < 0.0001;
            default:
                return false;
        }
    }
    
    /**
     * Prüft ob ein Item einem Floor-Filter mit Vergleichsoperator entspricht
     * @param item Das zu prüfende Item
     * @param filter Der Floor-Filter mit Operator und Wert
     * @return true wenn das Item dem Filter entspricht
     */
    private static boolean matchesFloorFilter(ItemData item, FloorFilter filter) {
        if (item.foundAt == null || item.foundAt.isEmpty()) {
            return false;
        }
        
        // Durchsuche alle foundAt-Locations nach Floor-Werten
        for (LocationData location : item.foundAt) {
            if (location.floor != null) {
                String floorStr = location.floor.toLowerCase();
                // Extrahiere Zahl aus Floor-String (unterstützt "Ebene 49", "floor_49", "49", etc.)
                String floorNumStr = floorStr.replaceAll("[^0-9]", "");
                if (!floorNumStr.isEmpty()) {
                    try {
                        int floorNum = Integer.parseInt(floorNumStr);
                        // Vergleiche mit dem Filter-Wert
                        if (compareFloorValue(floorNum, filter.value, filter.operator)) {
                            return true; // Mindestens eine Location matched
                        }
                    } catch (NumberFormatException e) {
                        // Ignoriere ungültige Floor-Werte
                    }
                }
            }
        }
        
        return false; // Keine Location matched
    }
    
    /**
     * Vergleicht zwei Floor-Werte basierend auf dem Operator
     * @param itemValue Der Floor-Wert des Items
     * @param filterValue Der Vergleichswert aus dem Filter
     * @param operator Der Vergleichsoperator (">", "<", ">=", "<=", "=")
     * @return true wenn der Vergleich erfolgreich ist
     */
    private static boolean compareFloorValue(Integer itemValue, Integer filterValue, String operator) {
        if (itemValue == null || filterValue == null) {
            return false;
        }
        
        switch (operator) {
            case ">":
                return itemValue > filterValue;
            case "<":
                return itemValue < filterValue;
            case ">=":
                return itemValue >= filterValue;
            case "<=":
                return itemValue <= filterValue;
            case "=":
            case "==":
                return itemValue.equals(filterValue);
            default:
                return false;
        }
    }
}


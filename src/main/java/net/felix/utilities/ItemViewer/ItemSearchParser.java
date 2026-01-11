package net.felix.utilities.ItemViewer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser für Suchanfragen
 */
public class ItemSearchParser {
    // Tag-Pattern: #<tag> - unterstützt auch Teilstrings für Live-Suche
    // #Schuhe, #Schuh, #Schu, #Sch, #Sc, #S werden alle erkannt
    // Unterstützt auch Umlaute (ü, ä, ö) und andere Unicode-Buchstaben
    private static final Pattern TAG_PATTERN = Pattern.compile("#([\\p{L}\\p{N}_]*)", Pattern.UNICODE_CASE);
    // Stat-Pattern: @StatName>Wert, @StatName<Wert, @StatName>=Wert, @StatName<=Wert, @StatName=Wert
    // Floor-Vergleichs-Pattern: @Ebene>50, @Ebene<50, @Ebene>=50, @Ebene<=50, etc. (MUSS VOR STAT_PATTERN geparst werden)
    private static final Pattern FLOOR_COMPARISON_PATTERN = Pattern.compile("@(?:ebene|floor)\\s*(>=|<=|>|<|=)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    // Aspekt-Pattern: @Aspekt Name oder @AspektName (MUSS VOR STAT_NAME_PATTERN geparst werden)
    // Unterstützt auch direkte Aspekt-Namen nach @ (z.B. @Erde, @Flamme) wenn sie bekannte Aspekte sind
    // Pattern erlaubt Leerzeichen nach "aspekt" oder "aspect" (optional)
    private static final Pattern ASPECT_AT_PATTERN = Pattern.compile("@(?:aspekt|aspect)\\s+([\\w\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STAT_PATTERN = Pattern.compile("@([\\w]+)\\s*([><=]+)\\s*([\\d.,]+)", Pattern.CASE_INSENSITIVE);
    // Stat-Name-Pattern (ohne Operator/Wert für Live-Suche): @StatName
    private static final Pattern STAT_NAME_PATTERN = Pattern.compile("@([\\w]+)(?![><=])", Pattern.CASE_INSENSITIVE);
    // Modifier-Pattern: +modifier oder +modifier:anzahl
    private static final Pattern MODIFIER_PATTERN = Pattern.compile(
        "\\+([\\w\\s]+)(?::(\\d+))?",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern COST_CATEGORY_PATTERN = Pattern.compile(
        "(amboss|ressource|material1|material2|kaktus|seele|coin|coins)[\\s:]*([\\w\\s\\d]+)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ASPECT_PATTERN = Pattern.compile(
        "aspekt[\\s:]+(\\w+)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FLOOR_PATTERN = Pattern.compile(
        "(?:ebene|floor)[\\s:_]+(\\d+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Bekannte Aspekte (kann erweitert werden)
    private static final List<String> KNOWN_ASPECTS = List.of(
        "erde", "feuer", "flamme", "wasser", "luft", "licht", "dunkelheit"
    );
    
    public static SearchQuery parse(String searchText) {
        SearchQuery query = new SearchQuery();
        
        if (searchText == null || searchText.trim().isEmpty()) {
            return query;
        }
        
        // Unterstütze kombinierte Suche mit Komma (z.B. "#Schwert, @Schaden>100")
        // Teile durch Kommas auf, aber ignoriere Kommas innerhalb von Anführungszeichen oder speziellen Kontexten
        String[] parts = searchText.split(",");
        
        // Wenn mehrere Teile vorhanden sind, parse jeden Teil separat
        if (parts.length > 1) {
            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty()) continue;
                
                // Parse jeden Teil als separate Query
                SearchQuery partQuery = parseSingleQuery(part);
                
                // Kombiniere die Ergebnisse
                query.tags.addAll(partQuery.tags);
                query.statFilters.addAll(partQuery.statFilters);
                query.modifierFilters.addAll(partQuery.modifierFilters);
                query.costFilters.addAll(partQuery.costFilters);
                query.floorFilters.addAll(partQuery.floorFilters);
                if (partQuery.aspect != null && query.aspect == null) {
                    query.aspect = partQuery.aspect;
                }
                if (partQuery.floor != null && query.floor == null) {
                    query.floor = partQuery.floor;
                }
                // Name-Suche: Kombiniere mit " UND " (alle müssen matchen)
                if (!partQuery.nameSearch.isEmpty()) {
                    if (query.nameSearch.isEmpty()) {
                        query.nameSearch = partQuery.nameSearch;
                    } else {
                        query.nameSearch += " " + partQuery.nameSearch;
                    }
                }
            }
            return query;
        }
        
        // Normale Suche ohne Komma
        String remaining = searchText.trim();
        
        // Floor-Vergleichs-Filter: @Ebene>50, @Ebene<50, etc. (MUSS VOR STAT_PATTERN geparst werden)
        Matcher floorComparisonMatcher = FLOOR_COMPARISON_PATTERN.matcher(remaining);
        while (floorComparisonMatcher.find()) {
            String operator = floorComparisonMatcher.group(1).trim();
            String valueStr = floorComparisonMatcher.group(2).trim();
            
            try {
                int value = Integer.parseInt(valueStr);
                
                FloorFilter filter = new FloorFilter();
                filter.operator = operator;
                filter.value = value;
                
                query.floorFilters.add(filter);
                remaining = remaining.replace(floorComparisonMatcher.group(0), "").trim();
            } catch (NumberFormatException e) {
                // Ignoriere ungültige Werte
            }
        }
        
        // Aspekt-Suche: @Aspekt Name oder direkter Aspekt-Name (MUSS VOR STAT_PATTERN geparst werden)
        Matcher aspectAtMatcher = ASPECT_AT_PATTERN.matcher(remaining);
        if (aspectAtMatcher.find()) {
            String aspectName = aspectAtMatcher.group(1).trim();
            if (!aspectName.isEmpty()) {
                query.aspect = aspectName;
                remaining = remaining.replace(aspectAtMatcher.group(0), "").trim();
            }
        } else {
            // Prüfe ob direkter Aspekt-Name nach @ (z.B. @Erde, @Flamme)
            // Muss VOR STAT_NAME_PATTERN geparst werden
            for (String knownAspect : KNOWN_ASPECTS) {
                // Prüfe ob @ gefolgt von bekanntem Aspekt (mit oder ohne Leerzeichen)
                Pattern directAspectPattern = Pattern.compile("@\\s*" + Pattern.quote(knownAspect) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher directAspectMatcher = directAspectPattern.matcher(remaining);
                if (directAspectMatcher.find()) {
                    query.aspect = knownAspect;
                    remaining = remaining.replace(directAspectMatcher.group(0), "").trim();
                    break;
                }
            }
        }
        
        // Stat-Filter: @StatName>Wert, @StatName<Wert, etc.
        Matcher statMatcher = STAT_PATTERN.matcher(remaining);
        while (statMatcher.find()) {
            String statName = statMatcher.group(1).trim();
            String operator = statMatcher.group(2).trim();
            String valueStr = statMatcher.group(3).trim();
            
            try {
                // Ersetze Komma durch Punkt für Parsing (z.B. "9,011" -> "9.011")
                String normalizedValue = valueStr.replace(',', '.');
                double value = Double.parseDouble(normalizedValue);
                
                StatFilter filter = new StatFilter();
                filter.statName = statName;
                filter.operator = operator;
                filter.value = value;
                
                query.statFilters.add(filter);
                remaining = remaining.replace(statMatcher.group(0), "").trim();
            } catch (NumberFormatException e) {
                // Ignoriere ungültige Werte
            }
        }
        
        // Tags: #<tag> - unterstützt auch Teilstrings für Live-Suche
        // #Schuhe, #Schuh, #Schu, #Sch, #Sc, #S werden alle erkannt
        Matcher tagMatcher = TAG_PATTERN.matcher(remaining);
        while (tagMatcher.find()) {
            String tag = tagMatcher.group(1).toLowerCase();
            // Nur hinzufügen wenn nicht leer (sonst würde @ alleine auch matchen)
            if (!tag.isEmpty()) {
                query.tags.add(tag);
                remaining = remaining.replace(tagMatcher.group(0), "").trim();
            }
        }
        
        // Floor-Suche: "Ebene 49", "floor_1", etc.
        Matcher floorMatcher = FLOOR_PATTERN.matcher(remaining);
        if (floorMatcher.find()) {
            query.floor = Integer.parseInt(floorMatcher.group(1));
            remaining = remaining.replace(floorMatcher.group(0), "").trim();
        }
        
        // Modifier-Filter: +modifier oder +modifier:anzahl
        Matcher modifierMatcher = MODIFIER_PATTERN.matcher(remaining);
        while (modifierMatcher.find()) {
            String modifierName = modifierMatcher.group(1).trim();
            String countStr = modifierMatcher.group(2);
            
            ModifierFilter filter = new ModifierFilter();
            filter.modifier = modifierName;
            
            // Prüfe ob Anzahl angegeben wurde
            if (countStr != null && !countStr.isEmpty()) {
                try {
                    filter.count = Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    // Ignoriere ungültige Anzahl
                }
            }
            
            query.modifierFilters.add(filter);
            remaining = remaining.replace(modifierMatcher.group(0), "").trim();
        }
        
        // Kosten-Kategorien: "amboss:0", "amboss: 0", "ressource:Eichenholz", etc.
        Matcher costMatcher = COST_CATEGORY_PATTERN.matcher(remaining);
        while (costMatcher.find()) {
            String category = costMatcher.group(1).toLowerCase();
            String value = costMatcher.group(2).trim();
            
            CostFilter filter = new CostFilter();
            filter.category = category;
            
            // Prüfe ob Wert eine Zahl ist
            try {
                filter.amount = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Keine Zahl = Item-Name
                filter.itemName = value;
            }
            
            query.costFilters.add(filter);
            remaining = remaining.replace(costMatcher.group(0), "").trim();
        }
        
        // Aspekte: "Aspekt: Erde" oder direkter Aspekt-Name (nur wenn noch nicht über @Aspekt gefunden)
        if (query.aspect == null) {
            Matcher aspectMatcher = ASPECT_PATTERN.matcher(remaining);
            if (aspectMatcher.find()) {
                query.aspect = aspectMatcher.group(1);
                remaining = remaining.replace(aspectMatcher.group(0), "").trim();
            } else {
                // Prüfe ob direkter Aspekt-Name
                for (String knownAspect : KNOWN_ASPECTS) {
                    if (remaining.toLowerCase().contains(knownAspect)) {
                        query.aspect = knownAspect;
                        remaining = remaining.replaceAll("(?i)" + knownAspect, "").trim();
                        break;
                    }
                }
            }
        }
        
        // Rest = Name-Suche (wird immer als Name-Suche behandelt, auch wenn es ein einzelnes Wort ist)
        // Modifier-Suche muss explizit mit <modifier> oder modifier:name angegeben werden
        query.nameSearch = remaining;
        
        return query;
    }
    
    /**
     * Parst eine einzelne Suchanfrage (ohne Komma-Trennung)
     */
    private static SearchQuery parseSingleQuery(String searchText) {
        SearchQuery query = new SearchQuery();
        
        if (searchText == null || searchText.trim().isEmpty()) {
            return query;
        }
        
        String remaining = searchText.trim();
        
        // Floor-Vergleichs-Filter: @Ebene>50, @Ebene<50, etc. (MUSS VOR STAT_PATTERN geparst werden)
        Matcher floorComparisonMatcher = FLOOR_COMPARISON_PATTERN.matcher(remaining);
        while (floorComparisonMatcher.find()) {
            String operator = floorComparisonMatcher.group(1).trim();
            String valueStr = floorComparisonMatcher.group(2).trim();
            
            try {
                int value = Integer.parseInt(valueStr);
                
                FloorFilter filter = new FloorFilter();
                filter.operator = operator;
                filter.value = value;
                
                query.floorFilters.add(filter);
                remaining = remaining.replace(floorComparisonMatcher.group(0), "").trim();
            } catch (NumberFormatException e) {
                // Ignoriere ungültige Werte
            }
        }
        
        // Stat-Filter: @StatName>Wert, @StatName<Wert, etc. (mit Operator und Wert)
        Matcher statMatcher = STAT_PATTERN.matcher(remaining);
        while (statMatcher.find()) {
            String statName = statMatcher.group(1).trim();
            String operator = statMatcher.group(2).trim();
            String valueStr = statMatcher.group(3).trim();
            
            try {
                // Ersetze Komma durch Punkt für Parsing (z.B. "9,011" -> "9.011")
                String normalizedValue = valueStr.replace(',', '.');
                double value = Double.parseDouble(normalizedValue);
                
                StatFilter filter = new StatFilter();
                filter.statName = statName;
                filter.operator = operator;
                filter.value = value;
                
                query.statFilters.add(filter);
                remaining = remaining.replace(statMatcher.group(0), "").trim();
            } catch (NumberFormatException e) {
                // Ignoriere ungültige Werte
            }
        }
        
        // Aspekt-Suche: @Aspekt Name oder direkter Aspekt-Name (MUSS VOR STAT_NAME_PATTERN geparst werden)
        Matcher aspectAtMatcher = ASPECT_AT_PATTERN.matcher(remaining);
        if (aspectAtMatcher.find()) {
            String aspectName = aspectAtMatcher.group(1).trim();
            if (!aspectName.isEmpty()) {
                query.aspect = aspectName;
                remaining = remaining.replace(aspectAtMatcher.group(0), "").trim();
            }
        } else {
            // Prüfe ob direkter Aspekt-Name nach @ (z.B. @Erde, @Flamme)
            // Muss VOR STAT_NAME_PATTERN geparst werden
            for (String knownAspect : KNOWN_ASPECTS) {
                // Prüfe ob @ gefolgt von bekanntem Aspekt (mit oder ohne Leerzeichen)
                Pattern directAspectPattern = Pattern.compile("@\\s*" + Pattern.quote(knownAspect) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher directAspectMatcher = directAspectPattern.matcher(remaining);
                if (directAspectMatcher.find()) {
                    query.aspect = knownAspect;
                    remaining = remaining.replace(directAspectMatcher.group(0), "").trim();
                    break;
                }
            }
        }
        
        // Stat-Name-Suche (ohne Operator/Wert für Live-Suche): @StatName
        // Wichtig: Muss NACH dem STAT_PATTERN und ASPECT_AT_PATTERN geparst werden
        Matcher statNameMatcher = STAT_NAME_PATTERN.matcher(remaining);
        while (statNameMatcher.find()) {
            String statName = statNameMatcher.group(1).trim();
            // Überspringe "aspekt" oder "aspect" da diese bereits über ASPECT_AT_PATTERN behandelt werden
            if (!statName.isEmpty() && !statName.equalsIgnoreCase("aspekt") && !statName.equalsIgnoreCase("aspect")) {
                StatFilter filter = new StatFilter();
                filter.statName = statName;
                filter.operator = null; // Kein Operator = nur Stat-Name-Suche
                filter.value = null;    // Kein Wert = nur Stat-Name-Suche
                
                query.statFilters.add(filter);
                remaining = remaining.replace(statNameMatcher.group(0), "").trim();
            }
        }
        
        // Tags: #<tag> - unterstützt auch Teilstrings für Live-Suche
        Matcher tagMatcher = TAG_PATTERN.matcher(remaining);
        while (tagMatcher.find()) {
            String tag = tagMatcher.group(1).toLowerCase();
            if (!tag.isEmpty()) {
                query.tags.add(tag);
                remaining = remaining.replace(tagMatcher.group(0), "").trim();
            }
        }
        
        // Floor-Suche: "Ebene 49", "floor_1", etc.
        Matcher floorMatcher = FLOOR_PATTERN.matcher(remaining);
        if (floorMatcher.find()) {
            query.floor = Integer.parseInt(floorMatcher.group(1));
            remaining = remaining.replace(floorMatcher.group(0), "").trim();
        }
        
        // Modifier-Filter: +modifier oder +modifier:anzahl
        Matcher modifierMatcher = MODIFIER_PATTERN.matcher(remaining);
        while (modifierMatcher.find()) {
            String modifierName = modifierMatcher.group(1).trim();
            String countStr = modifierMatcher.group(2);
            
            ModifierFilter filter = new ModifierFilter();
            filter.modifier = modifierName;
            
            if (countStr != null && !countStr.isEmpty()) {
                try {
                    filter.count = Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    // Ignoriere ungültige Anzahl
                }
            }
            
            query.modifierFilters.add(filter);
            remaining = remaining.replace(modifierMatcher.group(0), "").trim();
        }
        
        // Kosten-Kategorien: "amboss:0", "amboss: 0", "ressource:Eichenholz", etc.
        Matcher costMatcher = COST_CATEGORY_PATTERN.matcher(remaining);
        while (costMatcher.find()) {
            String category = costMatcher.group(1).toLowerCase();
            String value = costMatcher.group(2).trim();
            
            CostFilter filter = new CostFilter();
            filter.category = category;
            
            try {
                filter.amount = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                filter.itemName = value;
            }
            
            query.costFilters.add(filter);
            remaining = remaining.replace(costMatcher.group(0), "").trim();
        }
        
        // Aspekte: "Aspekt: Erde" oder direkter Aspekt-Name (nur wenn noch nicht über @Aspekt gefunden)
        if (query.aspect == null) {
            Matcher aspectMatcher = ASPECT_PATTERN.matcher(remaining);
            if (aspectMatcher.find()) {
                query.aspect = aspectMatcher.group(1);
                remaining = remaining.replace(aspectMatcher.group(0), "").trim();
            } else {
                for (String knownAspect : KNOWN_ASPECTS) {
                    if (remaining.toLowerCase().contains(knownAspect)) {
                        query.aspect = knownAspect;
                        remaining = remaining.replaceAll("(?i)" + knownAspect, "").trim();
                        break;
                    }
                }
            }
        }
        
        // Rest = Name-Suche
        query.nameSearch = remaining;
        
        return query;
    }
}


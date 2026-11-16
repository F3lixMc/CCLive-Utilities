package net.felix.utilities.Aincraft.itemviewer.search;

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
    // Stat-Pattern: @StatName>Wert, @StatName>=Wert, etc. (>= und <= vor > und <)
    // Floor-Vergleichs-Pattern: @Ebene>50, @e>=5, etc. (MUSS VOR STAT_PATTERN geparst werden)
    private static final Pattern FLOOR_COMPARISON_AT_PATTERN = Pattern.compile(
        "@(?:ebene|floor|e)\\s*(>=|<=|>|<|=)\\s*(\\d+)",
        Pattern.CASE_INSENSITIVE
    );
    // Ebenen-Vergleich ohne @: >e5, >=ebene 10, <e4, [e4] mit Operator davor
    private static final Pattern FLOOR_COMPARISON_PLAIN_PATTERN = Pattern.compile(
        "(?:^|[\\s,])(>=|<=|>|<|=)\\s*(?:\\[)?(?:ebene\\s*|e\\s*)(\\d+)\\s*\\]?(?=[\\s,]|$)",
        Pattern.CASE_INSENSITIVE
    );
    // Exakte Ebenen-Suche: e4, ebene 4, [e4]
    private static final Pattern FLOOR_SIMPLE_PATTERN = Pattern.compile(
        "^(?:\\[)?(?:ebene\\s*|e\\s*)(\\d+)\\s*\\]?$",
        Pattern.CASE_INSENSITIVE
    );
    // Aspekt-Pattern: @Aspekt Name oder @AspektName (MUSS VOR STAT_NAME_PATTERN geparst werden)
    private static final Pattern ASPECT_AT_PATTERN = Pattern.compile("@(?:aspekt|aspect)\\s+([\\p{L}\\p{N}_\\s]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern STAT_PATTERN = Pattern.compile(
        "@([\\p{L}\\p{N}_]+)\\s*(>=|<=|>|<|=)\\s*([\\d.,]+)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );
    // Stat-Name-Pattern (ohne Operator/Wert für Live-Suche): @StatName
    private static final Pattern STAT_NAME_PATTERN = Pattern.compile(
        "@([\\p{L}\\p{N}_]+)(?![><=])",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );
    // Modifier-Pattern: +modifier oder +modifier:anzahl (Unicode für Umlaute wie Fähigkeiten)
    private static final Pattern MODIFIER_PATTERN = Pattern.compile(
        "\\+([\\p{L}\\p{N}_\\s]+)(?::(\\d+))?",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );
    // Kosten-Kategorien: nur als eigenständiges Wort mit Trenner (z.B. "kaktus:0", "ressource:Eichenholz")
    // Wortgrenzen verhindern, dass "Kaktusernter" fälschlich als Kaktus-Kostenfilter gelesen wird
    private static final Pattern COST_CATEGORY_PATTERN = Pattern.compile(
        "\\b(amboss|ressource|material1|material2|material3|material4|material5|material|kaktus|seele|coin|coins|ofen)\\b(?:\\s*:\\s*|\\s+)([\\p{L}\\p{N}_\\s\\d.,<>=]+)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
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
        String[] parts = searchText.split(",");
        if (parts.length > 1) {
            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty()) continue;
                mergePartQuery(query, parseSingleQuery(part));
            }
            return query;
        }

        // Normale Suche ohne Komma
        String remaining = searchText.trim();
        remaining = parseFloorComparisons(remaining, query);
        // Aspekt-Suche MUSS VOR STAT_PATTERN geparst werden
        remaining = parseAspectAtSearch(remaining, query);
        remaining = parseStatFilters(remaining, query);
        remaining = parseTags(remaining, query);
        remaining = parseFloorExactSearch(remaining, query);
        remaining = parseModifierFilters(remaining, query);
        remaining = parseCostFilters(remaining, query);
        remaining = parseAspectKeyword(remaining, query);

        query.nameSearch = remaining;
        return query;
    }

    /** Fügt die Ergebnisse eines Teils einer kommagetrennten Suche in die Gesamtsuche ein. */
    private static void mergePartQuery(SearchQuery query, SearchQuery partQuery) {
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

    /**
     * Parst eine einzelne Suchanfrage (ohne Komma-Trennung)
     */
    private static SearchQuery parseSingleQuery(String searchText) {
        SearchQuery query = new SearchQuery();

        if (searchText == null || searchText.trim().isEmpty()) {
            return query;
        }

        String remaining = searchText.trim();
        remaining = parseFloorComparisons(remaining, query);
        // Stat-Filter mit Operator und Wert vor der Aspekt-Suche
        remaining = parseStatFilters(remaining, query);
        remaining = parseAspectAtSearch(remaining, query);
        // Stat-Name ohne Operator/Wert: MUSS NACH STAT_PATTERN und ASPECT_AT_PATTERN geparst werden
        remaining = parseStatNames(remaining, query);
        remaining = parseTags(remaining, query);
        remaining = parseFloorExactSearch(remaining, query);
        remaining = parseModifierFilters(remaining, query);
        remaining = parseCostFilters(remaining, query);
        remaining = parseAspectKeyword(remaining, query);

        query.nameSearch = remaining;
        return query;
    }

    /** Aspekt-Suche: @Aspekt Name oder direkter Aspekt-Name nach @ (z.B. @Erde, @Flamme). */
    private static String parseAspectAtSearch(String text, SearchQuery query) {
        String remaining = text;
        Matcher aspectAtMatcher = ASPECT_AT_PATTERN.matcher(remaining);
        if (aspectAtMatcher.find()) {
            String aspectName = aspectAtMatcher.group(1).trim();
            if (!aspectName.isEmpty()) {
                query.aspect = aspectName;
                remaining = remaining.replace(aspectAtMatcher.group(0), "").trim();
            }
            return remaining;
        }
        // Prüfe ob @ gefolgt von bekanntem Aspekt (mit oder ohne Leerzeichen); VOR STAT_NAME_PATTERN
        for (String knownAspect : KNOWN_ASPECTS) {
            Pattern directAspectPattern = Pattern.compile("@\\s*" + Pattern.quote(knownAspect) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher directAspectMatcher = directAspectPattern.matcher(remaining);
            if (directAspectMatcher.find()) {
                query.aspect = knownAspect;
                remaining = remaining.replace(directAspectMatcher.group(0), "").trim();
                break;
            }
        }
        return remaining;
    }

    /** Stat-Filter: @StatName>Wert, @StatName<Wert, etc. */
    private static String parseStatFilters(String text, SearchQuery query) {
        String remaining = text;
        Matcher statMatcher = STAT_PATTERN.matcher(remaining);
        while (statMatcher.find()) {
            String statName = statMatcher.group(1).trim();
            String operator = statMatcher.group(2).trim();
            String valueStr = statMatcher.group(3).trim();

            try {
                // Ersetze Komma durch Punkt für Parsing (z.B. "9,011" -> "9.011")
                double value = Double.parseDouble(valueStr.replace(',', '.'));

                StatFilter filter = new StatFilter();
                filter.statName = statName;
                filter.operator = ValueComparison.normalizeOperator(operator);
                filter.value = value;

                query.statFilters.add(filter);
                remaining = remaining.replace(statMatcher.group(0), "").trim();
            } catch (NumberFormatException e) {
                // Ignoriere ungültige Werte
            }
        }
        return remaining;
    }

    /** Stat-Name-Suche ohne Operator/Wert (Live-Suche): @StatName. */
    private static String parseStatNames(String text, SearchQuery query) {
        String remaining = text;
        Matcher statNameMatcher = STAT_NAME_PATTERN.matcher(remaining);
        while (statNameMatcher.find()) {
            String statName = statNameMatcher.group(1).trim();
            // "aspekt", "ebene" usw. werden bereits über ihre eigenen Pattern behandelt
            if (!statName.isEmpty() && !statName.equalsIgnoreCase("aspekt") && !statName.equalsIgnoreCase("aspect")
                && !statName.equalsIgnoreCase("ebene") && !statName.equalsIgnoreCase("floor") && !statName.equalsIgnoreCase("e")) {
                StatFilter filter = new StatFilter();
                filter.statName = statName;
                filter.operator = null; // Kein Operator = nur Stat-Name-Suche
                filter.value = null;    // Kein Wert = nur Stat-Name-Suche

                query.statFilters.add(filter);
                remaining = remaining.replace(statNameMatcher.group(0), "").trim();
            }
        }
        return remaining;
    }

    /**
     * Tags: #&lt;tag&gt; - unterstützt auch Teilstrings für Live-Suche
     * (#Schuhe, #Schuh, #Schu, #Sch, #Sc, #S werden alle erkannt).
     */
    private static String parseTags(String text, SearchQuery query) {
        String remaining = text;
        Matcher tagMatcher = TAG_PATTERN.matcher(remaining);
        while (tagMatcher.find()) {
            String tag = tagMatcher.group(1).toLowerCase();
            // Nur hinzufügen wenn nicht leer
            if (!tag.isEmpty()) {
                query.tags.add(SearchSuggestions.normalizeSearchTag(tag));
                remaining = remaining.replace(tagMatcher.group(0), "").trim();
            }
        }
        return remaining;
    }

    /** Modifier-Filter: +modifier oder +modifier:anzahl. */
    private static String parseModifierFilters(String text, SearchQuery query) {
        String remaining = text;
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
        return remaining;
    }

    /** Kosten-Kategorien: "amboss:0", "amboss: 0", "ressource:Eichenholz", etc. */
    private static String parseCostFilters(String text, SearchQuery query) {
        String remaining = text;
        Matcher costMatcher = COST_CATEGORY_PATTERN.matcher(remaining);
        while (costMatcher.find()) {
            String category = costMatcher.group(1).toLowerCase();
            String value = costMatcher.group(2).trim();

            CostFilter filter = new CostFilter();
            filter.category = category;
            applyCostFilterValue(filter, value);

            query.costFilters.add(filter);
            remaining = remaining.replace(costMatcher.group(0), "").trim();
        }
        return remaining;
    }

    /** "Aspekt: Erde" oder direkter Aspekt-Name (nur wenn noch nicht über @Aspekt gefunden). */
    private static String parseAspectKeyword(String text, SearchQuery query) {
        if (query.aspect != null) {
            return text;
        }
        Matcher aspectMatcher = ASPECT_PATTERN.matcher(text);
        if (aspectMatcher.find()) {
            query.aspect = aspectMatcher.group(1);
            return text.replace(aspectMatcher.group(0), "").trim();
        }
        return parseKnownAspectName(text, query);
    }
    private static void applyCostFilterValue(CostFilter filter, String rawValue) {
        String value = rawValue.trim();
        if (value.isEmpty()) {
            return;
        }
        Matcher opMatcher = Pattern.compile("^(>=|<=|>|<|=)\\s*(\\d+(?:[.,]\\d+)?)$").matcher(value);
        if (opMatcher.matches()) {
            filter.amountOperator = ValueComparison.normalizeOperator(opMatcher.group(1));
            filter.amount = parseCostFilterAmount(opMatcher.group(2));
            return;
        }
        try {
            filter.amount = parseCostFilterAmount(value);
        } catch (NumberFormatException e) {
            filter.itemName = value;
        }
    }

    private static int parseCostFilterAmount(String value) {
        String normalized = value.trim().replace(",", "").replace(".", "");
        return Integer.parseInt(normalized);
    }
    
    private static String parseFloorComparisons(String text, SearchQuery query) {
        String remaining = text;
        
        Matcher atMatcher = FLOOR_COMPARISON_AT_PATTERN.matcher(remaining);
        StringBuffer atBuffer = new StringBuffer();
        while (atMatcher.find()) {
            addFloorFilter(query, atMatcher.group(1), atMatcher.group(2));
            atMatcher.appendReplacement(atBuffer, " ");
        }
        atMatcher.appendTail(atBuffer);
        remaining = atBuffer.toString().replaceAll("\\s+", " ").trim();
        
        Matcher plainMatcher = FLOOR_COMPARISON_PLAIN_PATTERN.matcher(remaining);
        StringBuffer plainBuffer = new StringBuffer();
        while (plainMatcher.find()) {
            addFloorFilter(query, plainMatcher.group(1), plainMatcher.group(2));
            plainMatcher.appendReplacement(plainBuffer, " ");
        }
        plainMatcher.appendTail(plainBuffer);
        remaining = plainBuffer.toString().replaceAll("\\s+", " ").trim();
        
        return remaining;
    }
    
    private static String parseFloorExactSearch(String text, SearchQuery query) {
        String remaining = text;
        
        Matcher simpleMatcher = FLOOR_SIMPLE_PATTERN.matcher(remaining.trim());
        if (simpleMatcher.matches()) {
            query.floor = Integer.parseInt(simpleMatcher.group(1));
            return "";
        }
        
        Matcher floorMatcher = FLOOR_PATTERN.matcher(remaining);
        if (floorMatcher.find()) {
            query.floor = Integer.parseInt(floorMatcher.group(1));
            remaining = remaining.replace(floorMatcher.group(0), "").trim();
        }
        
        return remaining;
    }
    
    private static String parseKnownAspectName(String remaining, SearchQuery query) {
        for (String knownAspect : KNOWN_ASPECTS) {
            Pattern aspectWordPattern = Pattern.compile(
                "\\b" + Pattern.quote(knownAspect) + "\\b",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
            );
            Matcher matcher = aspectWordPattern.matcher(remaining);
            if (matcher.find()) {
                query.aspect = knownAspect;
                return matcher.replaceAll("").trim();
            }
        }
        return remaining;
    }

    private static void addFloorFilter(SearchQuery query, String operator, String valueStr) {
        try {
            FloorFilter filter = new FloorFilter();
            filter.operator = ValueComparison.normalizeOperator(operator);
            filter.value = Integer.parseInt(valueStr.trim());
            query.floorFilters.add(filter);
        } catch (NumberFormatException e) {
            // Ignoriere ungültige Werte
        }
    }
}


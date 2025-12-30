package net.felix.utilities.ItemViewer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser für Suchanfragen
 */
public class ItemSearchParser {
    private static final Pattern TAG_PATTERN = Pattern.compile("@(\\w+)");
    private static final Pattern COST_CATEGORY_PATTERN = Pattern.compile(
        "(amboss|ressource|material1|material2|kaktus|seele|coin|coins)[\\s:]+([\\w\\s]+)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ASPECT_PATTERN = Pattern.compile(
        "aspekt[\\s:]+(\\w+)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Bekannte Aspekte (kann erweitert werden)
    private static final List<String> KNOWN_ASPECTS = List.of(
        "erde", "feuer", "wasser", "luft", "licht", "dunkelheit"
    );
    
    public static SearchQuery parse(String searchText) {
        SearchQuery query = new SearchQuery();
        
        if (searchText == null || searchText.trim().isEmpty()) {
            return query;
        }
        
        String remaining = searchText.trim();
        
        // Tags: @<tag>
        Matcher tagMatcher = TAG_PATTERN.matcher(remaining);
        while (tagMatcher.find()) {
            query.tags.add(tagMatcher.group(1).toLowerCase());
            remaining = remaining.replace(tagMatcher.group(0), "").trim();
        }
        
        // Kosten-Kategorien: "amboss:0", "ressource:Eichenholz", etc.
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
        
        // Aspekte: "Aspekt: Erde" oder direkter Aspekt-Name
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
        
        // Rest = Name-Suche
        query.nameSearch = remaining;
        
        return query;
    }
}


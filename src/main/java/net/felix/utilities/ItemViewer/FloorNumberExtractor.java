package net.felix.utilities.ItemViewer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrahiert Ebenen-Nummern aus Item-Daten (foundAt, Name mit [eX])
 */
public final class FloorNumberExtractor {
    
    private static final Pattern FLOOR_TAG_PATTERN = Pattern.compile(
        "\\[\\s*e\\s*(\\d+)\\s*\\]",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BRACKET_CONTENT_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");
    private static final Pattern E_NUMBER_IN_BRACKET_PATTERN = Pattern.compile(
        "e\\s*(\\d+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private FloorNumberExtractor() {
    }
    
    public static List<Integer> extractFromItem(ItemData item) {
        List<Integer> floors = new ArrayList<>();
        
        if (item.foundAt != null) {
            for (LocationData location : item.foundAt) {
                Integer floor = parseFloorString(location.floor);
                if (floor != null) {
                    addIfAbsent(floors, floor);
                }
            }
        }
        
        if (item.name != null && !item.name.isEmpty()) {
            String cleanName = item.name.replaceAll("§[0-9a-fk-or]", "");
            
            Matcher singleMatcher = FLOOR_TAG_PATTERN.matcher(cleanName);
            while (singleMatcher.find()) {
                addIfAbsent(floors, Integer.parseInt(singleMatcher.group(1)));
            }
            
            Matcher bracketMatcher = BRACKET_CONTENT_PATTERN.matcher(cleanName);
            while (bracketMatcher.find()) {
                String bracketContent = bracketMatcher.group(1);
                if (!bracketContent.toLowerCase().contains("e")) {
                    continue;
                }
                Matcher numberMatcher = E_NUMBER_IN_BRACKET_PATTERN.matcher(bracketContent);
                while (numberMatcher.find()) {
                    addIfAbsent(floors, Integer.parseInt(numberMatcher.group(1)));
                }
            }
        }
        
        return floors;
    }
    
    public static Integer parseFloorString(String floorStr) {
        if (floorStr == null || floorStr.isEmpty()) {
            return null;
        }
        String floorNumStr = floorStr.toLowerCase().replaceAll("[^0-9]", "");
        if (floorNumStr.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(floorNumStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private static void addIfAbsent(List<Integer> floors, int floor) {
        if (!floors.contains(floor)) {
            floors.add(floor);
        }
    }
}

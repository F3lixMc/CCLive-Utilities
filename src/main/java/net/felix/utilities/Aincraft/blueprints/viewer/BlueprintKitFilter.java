package net.felix.utilities.Aincraft.blueprints.viewer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

/**
 * Reine Zuordnung der Kit-Baupläne. Fundstatus und die aktuelle Ebene bleiben draußen,
 * damit die Liste nur bei Auswahl, Inhalt oder einem Daten-Reload neu entsteht.
 */
public final class BlueprintKitFilter {

    public static final String PAGE = "kitfilter";
    public static final String[] RARITIES = {"common", "uncommon", "rare", "epic", "legendary"};

    public static final int TITLE_AQUA = 0xFF55FFFF;

    public static final int FOUND_COMMON = 0xFFFFFFFF;
    public static final int MISSING_COMMON = 0xFF888888;
    public static final int FOUND_UNCOMMON = 0xFF00FF00;
    public static final int MISSING_UNCOMMON = 0xFF007A00;
    public static final int FOUND_RARE = 0xFF5555FF;
    public static final int MISSING_RARE = 0xFF2A2A80;
    public static final int FOUND_EPIC = 0xFFAA00FF;
    public static final int MISSING_EPIC = 0xFF550088;
    public static final int FOUND_LEGENDARY = 0xFFFF8800;
    public static final int MISSING_LEGENDARY = 0xFF884400;

    public static final String NONE = "Keine Kit-Baupläne";
    public static final String ALL_FOUND = "Alle gefunden";

    private static final Pattern FORMAT_CODES = Pattern.compile("§[0-9a-fk-orA-FK-OR]");
    private static final Pattern STACK_SUFFIX = Pattern.compile("\\s*×\\d+$");

    private BlueprintKitFilter() {
    }

    public record CatalogItem(String floorKey, String rarity, String name) {
    }

    public record KitPart(String name, String rarity) {
    }

    public record Entry(String name, String rarity, Integer floor, String label) {
    }

    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        String cleaned = FORMAT_CODES.matcher(name).replaceAll("");
        if (cleaned.startsWith("- ")) {
            cleaned = cleaned.substring(2);
        }
        cleaned = STACK_SUFFIX.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }

    /**
     * {@code null} heißt: die Kitzeile nennt keine Seltenheit. Ein leerer String heißt:
     * es steht etwas dabei, das keine Katalog-Seltenheit ist, und deshalb nichts passt.
     */
    public static String rarityConstraint(String rarity) {
        if (rarity == null || rarity.isBlank()) {
            return null;
        }
        String normalized = rarity.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "common", "uncommon", "rare", "epic", "legendary" -> normalized;
            default -> "";
        };
    }

    public static Integer floorNumber(String floorKey) {
        if (floorKey == null || !floorKey.startsWith("floor_")) {
            return null;
        }
        String rest = floorKey.substring("floor_".length());
        int end = 0;
        while (end < rest.length() && Character.isDigit(rest.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return null;
        }
        try {
            return Integer.parseInt(rest.substring(0, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static List<String> pageOrder(boolean effective) {
        List<String> pages = new ArrayList<>();
        if (effective) {
            pages.add(PAGE);
        }
        for (String rarity : RARITIES) {
            pages.add(rarity);
        }
        return pages;
    }

    public static String step(String current, boolean effective, int delta) {
        List<String> pages = pageOrder(effective);
        int index = pages.indexOf(current);
        if (index < 0) {
            return pages.get(0);
        }
        int next = index + delta;
        if (next < 0 || next >= pages.size()) {
            return current;
        }
        return pages.get(next);
    }

    public static String pageAfter(boolean wasEffective, boolean effective, String current) {
        if (effective && !wasEffective) {
            return PAGE;
        }
        if (!effective && PAGE.equals(current)) {
            return "common";
        }
        if (current == null || (!effective && pageOrder(false).indexOf(current) < 0)) {
            return "common";
        }
        return current;
    }

    public static String listKey(String selectionId, String displayName, String extra, String contentSignature, long catalogGeneration) {
        return (selectionId == null ? "" : selectionId)
                + "\u0000" + (displayName == null ? "" : displayName)
                + "\u0000" + (extra == null ? "" : extra)
                + "\u0000" + (contentSignature == null ? "" : contentSignature)
                + "\u0000" + catalogGeneration;
    }

    public static String contentSignature(List<KitPart> parts) {
        List<String> lines = new ArrayList<>();
        if (parts != null) {
            for (KitPart part : parts) {
                if (part == null) {
                    continue;
                }
                String rarity = part.rarity() == null ? "" : part.rarity().trim().toLowerCase(Locale.ROOT);
                lines.add(normalizeName(part.name()) + "|" + rarity);
            }
        }
        lines.sort(String::compareTo);
        return String.join("\n", lines);
    }

    public static List<Entry> kitPage(List<CatalogItem> catalog, List<KitPart> parts) {
        Map<String, Entry> unique = new LinkedHashMap<>();
        if (catalog != null) {
            for (CatalogItem item : catalog) {
                if (item == null || !matchesAny(item, parts)) {
                    continue;
                }
                String name = displayName(item.name());
                String rarity = catalogRarity(item.rarity());
                if (name.isEmpty() || rarity == null) {
                    continue;
                }
                Integer floor = floorNumber(item.floorKey());
                String key = normalizeName(name).toLowerCase(Locale.ROOT) + "|" + rarity;
                Entry candidate = new Entry(name, rarity, floor, label(name, floor));
                Entry existing = unique.get(key);
                if (existing == null || prefer(candidate, existing)) {
                    unique.put(key, candidate);
                }
            }
        }
        List<Entry> entries = new ArrayList<>(unique.values());
        entries.sort(Comparator
                .comparing((Entry entry) -> entry.floor() == null)
                .thenComparing(entry -> entry.floor() == null ? 0 : entry.floor())
                .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    public static List<Entry> rarityPage(List<CatalogItem> catalog, List<KitPart> parts, String floorKey, String rarity) {
        List<Entry> entries = new ArrayList<>();
        String wanted = catalogRarity(rarity);
        if (catalog == null || floorKey == null || wanted == null) {
            return entries;
        }
        for (CatalogItem item : catalog) {
            if (item == null || !floorKey.equals(item.floorKey()) || !matchesAny(item, parts)) {
                continue;
            }
            String itemRarity = catalogRarity(item.rarity());
            if (!wanted.equals(itemRarity)) {
                continue;
            }
            String name = displayName(item.name());
            if (name.isEmpty()) {
                continue;
            }
            entries.add(new Entry(name, itemRarity, floorNumber(floorKey), name));
        }
        return entries;
    }

    public static String label(String name, Integer floor) {
        if (floor == null) {
            return name;
        }
        return name + " (Ebene " + floor + ")";
    }

    public static String displayName(String raw) {
        if (raw == null) {
            return "";
        }
        String name = raw.startsWith("- ") ? raw.substring(2) : raw;
        return FORMAT_CODES.matcher(name).replaceAll("").trim();
    }

    public static boolean matches(CatalogItem item, KitPart part) {
        if (item == null || part == null || !namesMatch(item.name(), part.name())) {
            return false;
        }
        String constraint = rarityConstraint(part.rarity());
        if (constraint == null) {
            return true;
        }
        if (constraint.isEmpty()) {
            return false;
        }
        return constraint.equals(catalogRarity(item.rarity()));
    }

    public static int nameColor(String rarity, boolean found, boolean filteredColors) {
        if (!filteredColors) {
            return found ? FOUND_COMMON : MISSING_COMMON;
        }
        String normalized = catalogRarity(rarity);
        if (normalized == null) {
            return found ? FOUND_COMMON : MISSING_COMMON;
        }
        return switch (normalized) {
            case "uncommon" -> found ? FOUND_UNCOMMON : MISSING_UNCOMMON;
            case "rare" -> found ? FOUND_RARE : MISSING_RARE;
            case "epic" -> found ? FOUND_EPIC : MISSING_EPIC;
            case "legendary" -> found ? FOUND_LEGENDARY : MISSING_LEGENDARY;
            default -> found ? FOUND_COMMON : MISSING_COMMON;
        };
    }

    public static boolean isFound(String name, String rarity, BiPredicate<String, String> exact) {
        String clean = displayName(name);
        if ("Drachenzahn".equalsIgnoreCase(clean)) {
            String constraint = rarityConstraint(rarity);
            if (!"epic".equals(constraint) && !"legendary".equals(constraint)) {
                return false;
            }
            return exact != null && exact.test(clean, constraint);
        }
        return exact != null && exact.test(clean, rarityConstraint(rarity));
    }

    public static int filteredVisibleRows(int rawCount, int notFound, boolean missingMode) {
        if (rawCount <= 0) {
            return 1;
        }
        if (!missingMode) {
            return rawCount;
        }
        if (notFound <= 0) {
            return 1;
        }
        return notFound;
    }

    public static String placeholder(int rawCount, int notFound, boolean missingMode) {
        if (rawCount <= 0) {
            return NONE;
        }
        if (missingMode && notFound <= 0) {
            return ALL_FOUND;
        }
        return null;
    }

    public static int contentWidth(int minimum, int padding, boolean includeTitle, int titleWidth, List<Integer> rowWidths) {
        int max = minimum;
        if (includeTitle) {
            max = Math.max(max, titleWidth + padding);
        }
        if (rowWidths != null) {
            for (int rowWidth : rowWidths) {
                max = Math.max(max, rowWidth + padding);
            }
        }
        return max;
    }

    private static boolean matchesAny(CatalogItem item, List<KitPart> parts) {
        if (parts == null) {
            return false;
        }
        for (KitPart part : parts) {
            if (matches(item, part)) {
                return true;
            }
        }
        return false;
    }

    private static boolean namesMatch(String catalogName, String kitName) {
        String catalog = normalizeName(catalogName);
        String kit = normalizeName(kitName);
        return !catalog.isEmpty() && catalog.equalsIgnoreCase(kit);
    }

    private static String catalogRarity(String rarity) {
        String constraint = rarityConstraint(rarity);
        if (constraint == null || constraint.isEmpty()) {
            return null;
        }
        return constraint;
    }

    private static boolean prefer(Entry candidate, Entry existing) {
        if (candidate.floor() == null) {
            return false;
        }
        if (existing.floor() == null) {
            return true;
        }
        return candidate.floor() < existing.floor();
    }
}

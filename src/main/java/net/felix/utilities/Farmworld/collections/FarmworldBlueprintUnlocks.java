package net.felix.utilities.Farmworld.collections;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.felix.utilities.Aincraft.blueprints.viewer.BPViewerUtility;
import net.felix.utilities.Farmworld.collections.data.CollectionDatabase;

import static net.felix.utilities.Other.util.JsonConfigFiles.getConfigFilePath;

/**
 * Markiert Farmwelt-Baupläne als gefunden, sobald die Collection der Zone die nötige Stufe erreicht.
 * Die Stufen stehen in Farmworld.json, die Namen sind die des Item-Viewers.
 */
public final class FarmworldBlueprintUnlocks {

    private static final Pattern LOCATION = Pattern.compile("^(.+?) Collection ([IVX]+)$");
    private static final Pattern ITEM_COLLECTION = Pattern.compile(
            "^(.+?)(?:\\s+Collection)?\\s+(III|II|IV|VIII|VII|VI|IX|V|I|X)$");
    private static final int[] DEFAULT_THRESHOLDS = {
            50, 100, 250, 1000, 2500, 5000, 10000, 25000, 50000, 100000
    };
    private static final Map<String, String> LICENSE_CATALOG = Map.of(
            "Strand [Lizenz]", "Dschungelholz/Strand Lizenz",
            "Dunkeleichenwald [Lizenz]", "Dunkles Eichenholz Lizenz",
            "Pilzwald [Lizenz]", "Pilzholzwald Lizenz",
            "Karmesinwald [Lizenz]", "Karmesinholzwald Lizenz",
            "Echomine [Lizenz]", "Echokristallminen Lizenz"
    );
    private static final Map<String, List<String>> ZONE_RESOURCES = Map.ofEntries(
            Map.entry("[Kohle Mine]", List.of("Kohle")),
            Map.entry("[Bambuswald]", List.of("Bambus")),
            Map.entry("[Dunkeleichenwald]", List.of("Dunkeleichenholz")),
            Map.entry("[Mangrovensumpf]", List.of("Mangrovenholz")),
            Map.entry("[Strand]", List.of("Dschungelholz")),
            Map.entry("[Fichtenwald]", List.of("Fichtenholz")),
            Map.entry("[Pilzwald]", List.of("Pilz", "Pilzholz")),
            Map.entry("[Eisenmine]", List.of("Eisen")),
            Map.entry("[Goldmine]", List.of("Gold")),
            Map.entry("[Kupfermine]", List.of("Kupfer")),
            Map.entry("[Diamantmine]", List.of("Diamant")),
            Map.entry("[Quartzmine]", List.of("Quartz")),
            Map.entry("[Schwefelfelder]", List.of("Schwefel")),
            Map.entry("[Obsidianmine]", List.of("Obsidian")),
            Map.entry("[Karmesinwald]", List.of("Karmesinholz")),
            Map.entry("[Wirrwald]", List.of("Wirrholz")),
            Map.entry("[Echomine]", List.of("Echokristall")),
            Map.entry("[Antike Mine]", List.of("Netherite")),
            Map.entry("[Amethystmine]", List.of("Amethyst")),
            Map.entry("[Eichenwald]", List.of("Eichenholz"))
    );

    private static List<Unlock> unlocks = List.of();
    private static boolean loaded;

    private FarmworldBlueprintUnlocks() {
    }

    public record Unlock(String resource, int level, List<String> names) {
    }

    public static void apply(String zone, long blocks) {
        List<String> resources = resourcesFor(zone);
        if (resources.isEmpty() || blocks < 0) {
            return;
        }
        int reached = reachedLevel(blocks, CollectionDatabase.collectionsDatabase);
        if (reached <= 0) {
            return;
        }
        ensureLoaded();
        for (Unlock unlock : unlocks) {
            if (unlock.level() > reached || !matches(resources, unlock.resource())) {
                continue;
            }
            for (String name : unlock.names()) {
                BPViewerUtility.markCatalogBlueprintFound(name);
            }
        }
    }

    public static int reachedLevel(long blocks, Map<Integer, Integer> thresholds) {
        int reached = 0;
        for (int level = 1; level <= 10; level++) {
            int required = threshold(thresholds, level);
            if (blocks >= required) {
                reached = level;
            }
        }
        return reached;
    }

    /** Namen aus items.json, deren Fundort eine Collection-Stufe ist, zum Beispiel „Fichtenholz V“. */
    public static List<Unlock> readItems(JsonObject root) {
        List<Unlock> found = new ArrayList<>();
        if (root == null) {
            return found;
        }
        for (String group : root.keySet()) {
            JsonElement element = root.get(group);
            if (element == null || !element.isJsonArray()) {
                continue;
            }
            for (JsonElement item : element.getAsJsonArray()) {
                if (!item.isJsonObject()) {
                    continue;
                }
                JsonObject object = item.getAsJsonObject();
                if (!object.has("name") || !object.get("name").isJsonPrimitive()
                        || !object.has("foundAt") || !object.get("foundAt").isJsonArray()) {
                    continue;
                }
                String name = object.get("name").getAsString();
                for (JsonElement place : object.getAsJsonArray("foundAt")) {
                    if (!place.isJsonObject() || !place.getAsJsonObject().has("collection")
                            || !place.getAsJsonObject().get("collection").isJsonPrimitive()) {
                        continue;
                    }
                    Unlock unlock = collectionUnlock(place.getAsJsonObject().get("collection").getAsString(), name);
                    if (unlock != null) {
                        found.add(unlock);
                    }
                }
            }
        }
        return found;
    }

    static List<String> resourcesFor(String label) {
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        String zone = resolveZone(label);
        if (zone != null) {
            names.addAll(ZONE_RESOURCES.getOrDefault(zone, List.of()));
        }
        String bare = stripBrackets(label);
        if (!bare.isEmpty()) {
            names.add(bare);
        }
        return List.copyOf(names);
    }

    public static List<Unlock> read(JsonObject root) {
        List<Unlock> found = new ArrayList<>();
        if (root == null) {
            return found;
        }
        for (String group : root.keySet()) {
            JsonElement element = root.get(group);
            if (element == null || !element.isJsonArray()) {
                continue;
            }
            for (JsonElement entry : element.getAsJsonArray()) {
                if (!entry.isJsonObject()) {
                    continue;
                }
                Unlock unlock = readEntry(entry.getAsJsonObject());
                if (unlock != null) {
                    found.add(unlock);
                }
            }
        }
        return found;
    }

    static List<String> catalogNames(String alias, String level) {
        if (alias == null || alias.isBlank()) {
            return List.of();
        }
        String primary = alias.split("\\|\\|")[0].trim();
        Set<String> names = new LinkedHashSet<>();
        names.add(primary);
        if (primary.endsWith(" [Lizenz]")) {
            names.add(LICENSE_CATALOG.getOrDefault(primary, primary.replace(" [Lizenz]", " Lizenz")));
        }
        int number = levelNumber(level);
        if (number > 0) {
            if ("Modultasche".equalsIgnoreCase(primary)) {
                names.add("Modul-Tasche #" + number);
            } else if ("Magnet [Modul]".equalsIgnoreCase(primary)) {
                String magnet = switch (number) {
                    case 1 -> "Kupfermagnet";
                    case 2 -> "Eisenmagnet";
                    case 3 -> "Goldmagnet";
                    case 4 -> "Diamantmagnet";
                    default -> null;
                };
                if (magnet != null) {
                    names.add(magnet);
                }
            } else {
                String base = primary.replace(" [Modul]", "").replace("[", "").replace("]", "").trim();
                if (!base.isEmpty()) {
                    names.add(base + " #" + number);
                    names.add(base);
                }
            }
        }
        names.removeIf(String::isBlank);
        return List.copyOf(names);
    }

    private static Unlock readEntry(JsonObject entry) {
        if (!entry.has("location") || !entry.get("location").isJsonPrimitive()) {
            return null;
        }
        Matcher matcher = LOCATION.matcher(entry.get("location").getAsString().trim());
        if (!matcher.matches()) {
            return null;
        }
        String alias = entry.has("aliases") ? entry.get("aliases").getAsString() : "";
        String level = entry.has("level") ? entry.get("level").getAsString() : "";
        List<String> names = catalogNames(alias, level);
        int required = roman(matcher.group(2));
        if (names.isEmpty() || required <= 0) {
            return null;
        }
        return new Unlock(matcher.group(1).trim(), required, names);
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        List<Unlock> all = new ArrayList<>();
        all.addAll(read(loadJson("Farmworld.json")));
        all.addAll(readItems(loadJson("items.json")));
        unlocks = all;
    }

    private static JsonObject loadJson(String fileName) {
        try {
            var path = getConfigFilePath(fileName);
            if (path == null) {
                return null;
            }
            try (var reader = new InputStreamReader(java.nio.file.Files.newInputStream(path), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean matches(List<String> resources, String resource) {
        if (resource == null) {
            return false;
        }
        for (String candidate : resources) {
            if (candidate != null && candidate.equalsIgnoreCase(resource)) {
                return true;
            }
        }
        return false;
    }

    private static String resolveZone(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String trimmed = label.trim();
        if (ZONE_RESOURCES.containsKey(trimmed)) {
            return trimmed;
        }
        String bare = stripBrackets(trimmed).toLowerCase(Locale.ROOT);
        for (String key : ZONE_RESOURCES.keySet()) {
            String inner = key.substring(1, key.length() - 1).toLowerCase(Locale.ROOT);
            if (inner.equals(bare)) {
                return key;
            }
        }
        return switch (bare) {
            case "fichtenholz", "fichtenwald" -> "[Fichtenwald]";
            case "kohle", "kohlemine", "kohle mine" -> "[Kohle Mine]";
            case "dschungelholz", "strand" -> "[Strand]";
            case "eichenholz", "eichenwald" -> "[Eichenwald]";
            case "dunkeleichenholz", "dunkeleichenwald" -> "[Dunkeleichenwald]";
            case "pilz", "pilzholz", "pilzwald" -> "[Pilzwald]";
            default -> null;
        };
    }

    private static String stripBrackets(String label) {
        if (label == null) {
            return "";
        }
        String trimmed = label.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length() > 2) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static Unlock collectionUnlock(String collection, String name) {
        if (collection == null || name == null || name.isBlank()) {
            return null;
        }
        Matcher matcher = ITEM_COLLECTION.matcher(collection.trim());
        if (!matcher.matches()) {
            return null;
        }
        int required = roman(matcher.group(2));
        if (required <= 0) {
            return null;
        }
        return new Unlock(matcher.group(1).trim(), required, List.of(name.trim()));
    }

    private static int threshold(Map<Integer, Integer> thresholds, int level) {
        if (thresholds != null && thresholds.containsKey(level)) {
            return thresholds.get(level);
        }
        return DEFAULT_THRESHOLDS[level - 1];
    }

    private static int levelNumber(String level) {
        if (level == null || level.isBlank()) {
            return 0;
        }
        String trimmed = level.trim();
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(trimmed);
        }
        return roman(trimmed.toUpperCase(Locale.ROOT));
    }

    static int roman(String value) {
        return switch (value) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            case "VI" -> 6;
            case "VII" -> 7;
            case "VIII" -> 8;
            case "IX" -> 9;
            case "X" -> 10;
            default -> 0;
        };
    }
}

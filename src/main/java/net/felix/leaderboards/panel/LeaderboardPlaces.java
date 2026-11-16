package net.felix.leaderboards.panel;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.felix.features.information.collections.CollectionTracker;
import net.felix.features.information.farmzone.FarmzoneScoreboard;

/**
 * Welche Rangliste beim Öffnen des Inventars vorn liegt, hängt vom Aufenthaltsort ab.
 * Die Pfeile drehen nur die Liste dieses Ortes.
 */
public final class LeaderboardPlaces {
    public enum Style {
        SHORT,
        PLAYTIME,
        LEVEL,
        FACTORY
    }

    /** {@code boardId} ist der Board-Name auf dem Server. Null heißt: dafür gibt es noch keins. */
    public record Entry(String boardId, String title, String header, Style style, char unit) {
    }

    public record Rotation(List<Entry> entries, int defaultIndex, String placeKey) {
    }

    /** Reihenfolge wie die Lizenzen: Eichenwald, Kohlemine, Kupfermine, Strand, … */
    private static final String[] FARM_ZONES = {
        "Eichenwald", "Kohlemine", "Kupfermine", "Strand", "Fichtenwald",
        "Bambuswald", "Eisenmine", "Pilzwald", "Dunkeleichenwald", "Goldmine",
        "Mangrovensumpf", "Diamantmine", "Schwefelfelder", "Quartzmine",
        "Obsidianmine", "Karmesinwald", "Wirrwald", "Antike Mine", "Echomine",
        "Amethystmine"
    };

    private static final Entry PLAYTIME = new Entry("playtime", "Spielzeit", "Zeit", Style.PLAYTIME, 'M');
    private static final Entry COINS = new Entry("current_coins", "Aktuelle Coins", "Coins", Style.SHORT, 'C');
    private static final Entry RESOURCES = new Entry("all_collections", "Alle Ressourcen", "Blöcke", Style.SHORT, 'B');
    private static final Entry KILLS = new Entry("alltime_kills", "Alltime Kills", "Kills", Style.SHORT, 'K');
    private static final Entry ALL_KILLS = new Entry("alltime_kills", "Alle Kills", "Kills", Style.SHORT, 'K');
    private static final Entry SMITH = new Entry("smith_skill", "Schmiedeskill", "Level/XP", Style.LEVEL, 'X');
    private static final Entry FACTORY = new Entry("factory_level", "Fabrik Level", "Level", Style.FACTORY, 'L');

    private LeaderboardPlaces() {
    }

    /**
     * Alle Boards des Sammelabrufs, einschließlich Schmiedeskill und Fabriklevel.
     */
    public static List<String> syncBoardIds() {
        Set<String> ids = new LinkedHashSet<>();
        ids.add(PLAYTIME.boardId());
        ids.add(COINS.boardId());
        ids.add(RESOURCES.boardId());
        ids.add(KILLS.boardId());
        ids.add(SMITH.boardId());
        ids.add(FACTORY.boardId());
        for (String zone : FARM_ZONES) {
            String boardId = zoneBoard(zone);
            if (boardId != null) {
                ids.add(boardId);
            }
        }
        for (int floor = 1; floor <= 100; floor++) {
            ids.add("floor_" + floor);
        }
        return List.copyOf(ids);
    }

    public static Rotation forPlayer(Minecraft client) {
        Place place = detect(client);
        return switch (place.kind) {
            case AINCRAFT -> aincraft(place.floor);
            case FARM -> farm(place.zone);
            case CITY -> standard(SMITH);
            case FACTORY -> standard(FACTORY);
        };
    }

    private static Rotation standard(Entry selected) {
        List<Entry> entries = List.of(PLAYTIME, COINS, RESOURCES, KILLS, SMITH, FACTORY);
        String placeKey = selected == SMITH ? "city" : "factory";
        return new Rotation(entries, entries.indexOf(selected), placeKey);
    }

    private static Rotation aincraft(int floor) {
        int shown = floor >= 1 && floor <= 100 ? floor : 1;
        Entry ebene = new Entry("floor_" + shown, "Ebene " + shown, "Kills", Style.SHORT, 'K');
        return new Rotation(List.of(ALL_KILLS, ebene, SMITH, PLAYTIME, COINS, RESOURCES), 1, "aincraft");
    }

    private static Rotation farm(String zoneName) {
        int at = zoneIndex(zoneName);
        String current;
        if (at >= 0) {
            current = FARM_ZONES[at];
        } else if (zoneName != null && !zoneName.isBlank()) {
            current = zoneName.trim();
        } else {
            current = FARM_ZONES[0];
            at = 0;
        }
        List<Entry> entries = new ArrayList<>();
        entries.add(RESOURCES);
        entries.add(zone(current));
        if (at >= 0) {
            for (int step = 1; step < FARM_ZONES.length; step++) {
                entries.add(zone(FARM_ZONES[(at + step) % FARM_ZONES.length]));
            }
        } else {
            for (String name : FARM_ZONES) {
                entries.add(zone(name));
            }
        }
        entries.add(KILLS);
        entries.add(SMITH);
        entries.add(PLAYTIME);
        entries.add(COINS);
        return new Rotation(entries, 1, "farm");
    }

    private static Entry zone(String name) {
        return new Entry(zoneBoard(name), name, "Blöcke", Style.SHORT, 'B');
    }

    /** Dieselben Board-Namen, die der Farmwelt-Sammler an den Server schickt. */
    private static String zoneBoard(String name) {
        return switch (normalize(name)) {
            case "eichenwald" -> "oak_collection";
            case "schwefelfelder" -> "sulfur_collection";
            case "pilzwald" -> "mushroom_collection";
            case "fichtenwald" -> "spruce_collection";
            case "dunkeleichenwald" -> "dark_oak_collection";
            case "mangrovensumpf" -> "mangrove_collection";
            case "strand" -> "jungle_collection";
            case "bambuswald" -> "bamboo_collection";
            case "goldmine" -> "raw_gold_collection";
            case "kohlemine" -> "coal_collection";
            case "karmesinwald" -> "crimson_collection";
            case "kupfermine" -> "raw_copper_collection";
            case "quartzmine" -> "quartz_collection";
            case "wirrwald" -> "warped_collection";
            case "diamantmine" -> "diamond_collection";
            case "antikemine" -> "ancient_debris_collection";
            case "echomine" -> "echo_collection";
            case "eisenmine" -> "raw_iron_collection";
            case "obsidianmine" -> "obsidian_collection";
            case "amethystmine" -> "amethyst_collection";
            default -> null;
        };
    }

    private static int zoneIndex(String zoneName) {
        if (zoneName == null) {
            return -1;
        }
        String wanted = normalize(zoneName);
        for (int i = 0; i < FARM_ZONES.length; i++) {
            if (normalize(FARM_ZONES[i]).equals(wanted)) {
                return i;
            }
        }
        return -1;
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace("[", "").replace("]", "").replace(" ", "");
    }

    private enum Kind {
        FACTORY,
        CITY,
        FARM,
        AINCRAFT
    }

    private record Place(Kind kind, int floor, String zone) {
    }

    private static Place detect(Minecraft client) {
        if (client == null || client.level == null) {
            return new Place(Kind.FACTORY, 1, null);
        }
        String path = client.level.dimension().identifier().getPath().toLowerCase(Locale.ROOT);
        if (path.contains("floor")) {
            return new Place(Kind.AINCRAFT, floorNumber(path), null);
        }
        if (seesSchmied(client)) {
            return new Place(Kind.CITY, 1, null);
        }
        if (client.level.dimension() == Level.OVERWORLD || "overworld".equals(path)) {
            FarmzoneScoreboard.refreshFarmzoneScoreboardCache(client);
            String zone = CollectionTracker.getDetectedFarmzoneName();
            if (zone != null && !zone.isBlank()) {
                return new Place(Kind.FARM, 1, zone);
            }
            return new Place(Kind.CITY, 1, null);
        }
        if (isCity(path)) {
            return new Place(Kind.CITY, 1, null);
        }
        return new Place(Kind.FACTORY, 1, null);
    }

    /** Namensschild „Schmied“ in der Nähe, so wie es über dem NPC am Stadt-Spawn steht. */
    private static boolean seesSchmied(Minecraft client) {
        if (client.player == null || client.level == null) {
            return false;
        }
        AABB area = client.player.getBoundingBox().inflate(64.0);
        try {
            for (Entity entity : client.level.getEntities(client.player, area, entity -> true)) {
                if (entity == null) {
                    continue;
                }
                if (isSchmiedName(entity.getCustomName()) || isSchmiedName(entity.getDisplayName()) || isSchmiedName(entity.getName())) {
                    return true;
                }
                if (entity instanceof Display.TextDisplay text && isSchmiedName(text.getText())) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
            return false;
        }
        return false;
    }

    static boolean isSchmiedName(Component name) {
        if (name == null) {
            return false;
        }
        String text = name.getString().replaceAll("§[0-9a-fk-or]", "").trim();
        return text.equalsIgnoreCase("Schmied");
    }

    private static boolean isCity(String path) {
        return path.equals("city") || path.equals("stadt") || path.equals("town")
            || path.contains("plot") || path.contains("stadt") || path.contains("city");
    }

    private static int floorNumber(String path) {
        int marker = path.indexOf("floor_");
        if (marker < 0) {
            return 1;
        }
        String rest = path.substring(marker + "floor_".length());
        int end = 0;
        while (end < rest.length() && Character.isDigit(rest.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 1;
        }
        try {
            return Integer.parseInt(rest.substring(0, end));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}

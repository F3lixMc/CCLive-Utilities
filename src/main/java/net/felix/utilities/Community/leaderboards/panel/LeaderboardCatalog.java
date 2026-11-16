package net.felix.utilities.Community.leaderboards.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Typen, die die Pfeile oben durchschalten. Die Board-ID ist die, die der Server schon kennt.
 */
public final class LeaderboardCatalog {
    public enum ValueKind {
        NUMBER,
        PLAYTIME
    }

    public record Board(String id, String label, ValueKind kind) {
        public String format(long score) {
            if (kind == ValueKind.PLAYTIME) {
                return formatPlaytime(score);
            }
            return String.format(Locale.GERMANY, "%,d", score);
        }
    }

    private static final List<Board> BOARDS = new ArrayList<>();

    static {
        add("alltime_kills", "Alle Kills", ValueKind.NUMBER);
        add("current_coins", "Coins", ValueKind.NUMBER);
        add("playtime", "Spielzeit", ValueKind.PLAYTIME);
        add("all_collections", "Alle Collections", ValueKind.NUMBER);
        add("alltime_rare_mob_kills", "Seltene Mobs", ValueKind.NUMBER);
        for (int floor = 1; floor <= 100; floor++) {
            add("floor_" + floor, "Ebene " + floor, ValueKind.NUMBER);
        }
    }

    private LeaderboardCatalog() {
    }

    public static List<Board> boards() {
        return BOARDS;
    }

    public static Board board(int index) {
        int size = BOARDS.size();
        int wrapped = Math.floorMod(index, size);
        return BOARDS.get(wrapped);
    }

    private static void add(String id, String label, ValueKind kind) {
        BOARDS.add(new Board(id, label, kind));
    }

    private static String formatPlaytime(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        StringBuilder text = new StringBuilder();
        if (days > 0) {
            text.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            text.append(hours).append("h ");
        }
        text.append(minutes).append("m");
        return text.toString().trim();
    }
}

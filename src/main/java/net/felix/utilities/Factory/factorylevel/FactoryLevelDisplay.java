package net.felix.utilities.Factory.factorylevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Zielzeilen und Restzeit-Text des Fabriklevel-Trackers, ohne Minecraft-Zustand.
 */
public final class FactoryLevelDisplay {
    public static final String TITLE = "Fabriklevel Progress";
    public static final String ALL_UNLOCKED = "Alle Ziele freigeschaltet";
    public static final int TITLE_COLOR = 0xFFFFFF00;
    public static final int LABEL_COLOR = 0xFFFFFFFF;
    public static final int GOAL_COLOR = 0xFF55FF55;
    public static final int UNLOCK_COLOR = 0xFF55FFFF;
    public static final int TIME_COLOR = 0xFFFFFF55;
    public static final String GOAL_LABEL = "Nächstes Ziel: ";
    public static final String DURATION_LABEL = "Dauer bis Ziel: ";

    private static final List<Unlock> UNLOCKS = List.of(
            new Unlock(3, "Forschungsraum"),
            new Unlock(6, "Luftschiff"),
            new Unlock(7, "Wellen"),
            new Unlock(9, "Talentbaum"),
            new Unlock(10, "Aincraft"),
            new Unlock(15, "Farmzone"),
            new Unlock(20, "Modultasche 1"),
            new Unlock(25, "Fischen"),
            new Unlock(35, "Jäger NPC"),
            new Unlock(45, "Seelen NPC"),
            new Unlock(50, "Runenslot 1", "Buff-Turm"),
            new Unlock(53, "Whitefeather NPC"),
            new Unlock(55, "Modultasche 2"),
            new Unlock(60, "Kaktusfarm"),
            new Unlock(70, "Items erforschen"),
            new Unlock(80, "Machtkristallslot 1"),
            new Unlock(90, "Minions"),
            new Unlock(100, "Modultasche 3", "Runenslot 2"),
            new Unlock(110, "Essenzen NPC"),
            new Unlock(150, "Machtkristallslot 2", "Runenslot 3"),
            new Unlock(250, "Recycler"),
            new Unlock(350, "Machtkristallslot 3"),
            new Unlock(400, "Modultasche 4"),
            new Unlock(600, "Modultasche 5"),
            new Unlock(650, "Produktion"),
            new Unlock(800, "Modultasche 6"),
            new Unlock(900, "Amethyst NPC"),
            new Unlock(1000, "Alchemie")
    );

    private FactoryLevelDisplay() {
    }

    public static Integer targetLevel(int experienceLevel) {
        Unlock next = nextUnlock(experienceLevel);
        return next == null ? null : next.level;
    }

    public static List<String> body(int experienceLevel, FactoryLevelEstimate.Status status, long remainingMillis) {
        Unlock next = nextUnlock(experienceLevel);
        if (next == null) {
            return List.of(ALL_UNLOCKED);
        }
        List<String> lines = new ArrayList<>();
        lines.add(GOAL_LABEL + "Level " + next.level);
        for (String name : next.names) {
            lines.add("• " + name);
        }
        if (next.names.length > 1) {
            lines.add("");
        }
        lines.add(durationLine(status, remainingMillis));
        return List.copyOf(lines);
    }

    public record LinePaint(String label, int labelColor, String value, int valueColor) {
    }

    public static LinePaint paint(String line) {
        if (line.startsWith(DURATION_LABEL)) {
            return new LinePaint(DURATION_LABEL, LABEL_COLOR, line.substring(DURATION_LABEL.length()), TIME_COLOR);
        }
        if (line.startsWith(GOAL_LABEL)) {
            return new LinePaint(GOAL_LABEL, LABEL_COLOR, line.substring(GOAL_LABEL.length()), GOAL_COLOR);
        }
        return new LinePaint("", 0, line, colorFor(line));
    }

    public static int colorFor(String line) {
        if (line.startsWith("•")) {
            return UNLOCK_COLOR;
        }
        return GOAL_COLOR;
    }

    /** Titel plus alle Zeilen darunter, inklusive der Leerzeilen. */
    public static int lineCount(int experienceLevel) {
        return 1 + body(experienceLevel, FactoryLevelEstimate.Status.MEASURING, 0L).size();
    }

    public static String durationLine(FactoryLevelEstimate.Status status, long remainingMillis) {
        String value;
        if (status == FactoryLevelEstimate.Status.STALLED) {
            value = "Kein XP-Zuwachs";
        } else if (status == FactoryLevelEstimate.Status.READY) {
            value = formatDuration(remainingMillis);
        } else {
            value = "Wird ermittelt …";
        }
        return DURATION_LABEL + value;
    }

    public static String formatDuration(long remainingMillis) {
        if (remainingMillis <= 0L) {
            return "1sek";
        }
        long day = 24L * 60L * 60L * 1000L;
        long hour = 60L * 60L * 1000L;
        long minute = 60L * 1000L;
        if (remainingMillis >= day) {
            return ceilDiv(remainingMillis, day) + "d";
        }
        if (remainingMillis >= hour) {
            return ceilDiv(remainingMillis, hour) + "h";
        }
        if (remainingMillis >= minute) {
            return ceilDiv(remainingMillis, minute) + "m";
        }
        long seconds = ceilDiv(remainingMillis, 1000L);
        if (seconds < 1L) {
            seconds = 1L;
        }
        return seconds + "sek";
    }

    private static Unlock nextUnlock(int experienceLevel) {
        for (Unlock unlock : UNLOCKS) {
            if (unlock.level > experienceLevel) {
                return unlock;
            }
        }
        return null;
    }

    private static long ceilDiv(long value, long unit) {
        return (value + unit - 1L) / unit;
    }

    private record Unlock(int level, String... names) {
    }
}

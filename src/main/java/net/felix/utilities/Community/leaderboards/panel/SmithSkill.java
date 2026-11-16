package net.felix.utilities.Community.leaderboards.panel;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Schmiedeskill aus der Tab-Liste.
 * Nur die nächste nicht leere Zeile unter [Schmiede Skill] gehört dazu.
 * Eine folgende Widget-Überschrift oder eine unlesbare Zeile verwirft die Erkennung.
 */
public final class SmithSkill {
    public static final String BOARD = "smith_skill";
    public static final int MAX_LEVEL = Integer.MAX_VALUE;
    public static final long MAX_XP = Long.MAX_VALUE;

    private static final Pattern VALUE = Pattern.compile(
        "^(.+?)\\s*/\\s*(.+?)(?:\\s+XP)?\\s*\\[(\\d+)\\]\\s*$",
        Pattern.CASE_INSENSITIVE);

    private SmithSkill() {
    }

    public record Stand(int level, long xp) {
    }

    public static Stand read(List<String> lines) {
        if (lines == null) {
            return null;
        }
        int header = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (strip(lines.get(i)).contains("[Schmiede Skill]")) {
                header = i;
                break;
            }
        }
        if (header < 0) {
            return null;
        }
        for (int i = header + 1; i < lines.size(); i++) {
            String next = strip(lines.get(i));
            if (next.isEmpty()) {
                continue;
            }
            if (next.startsWith("[")) {
                return null;
            }
            return parseValue(next);
        }
        return null;
    }

    /** Eine Zeile fürs Player-Stats-Debug: warum gelesen oder verworfen wurde. */
    public static String diagnose(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "Schmiedeskill: Tab-Liste leer";
        }
        int header = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (strip(lines.get(i)).contains("[Schmiede Skill]")) {
                header = i;
                break;
            }
        }
        if (header < 0) {
            return "Schmiedeskill: kein [Schmiede Skill] in " + lines.size() + " Tab-Zeilen";
        }
        for (int i = header + 1; i < lines.size(); i++) {
            String next = strip(lines.get(i));
            if (next.isEmpty()) {
                continue;
            }
            if (next.startsWith("[")) {
                return "Schmiedeskill: nächste Zeile ist ein anderes Widget: " + clip(next);
            }
            Stand stand = parseValue(next);
            if (stand == null) {
                return "Schmiedeskill: Wertezeile ungültig: " + clip(next);
            }
            return "Schmiedeskill gelesen: Level " + stand.level() + ", XP " + xpText(stand.xp());
        }
        return "Schmiedeskill: [Schmiede Skill] ohne Wertezeile";
    }

    private static String clip(String line) {
        String compact = line.replace('\n', ' ').trim();
        if (compact.length() > 80) {
            return compact.substring(0, 80);
        }
        return compact;
    }

    public static Stand parseValue(String line) {
        if (line == null) {
            return null;
        }
        Matcher matcher = VALUE.matcher(strip(line));
        if (!matcher.matches()) {
            return null;
        }
        Long xp = LeaderboardNumbers.parseMagnitude(matcher.group(1));
        Long target = LeaderboardNumbers.parseMagnitude(matcher.group(2));
        if (xp == null || target == null || xp < 0L) {
            return null;
        }
        try {
            int level = Integer.parseInt(matcher.group(3));
            if (level < 0) {
                return null;
            }
            return new Stand(level, xp);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String display(int level, long xp) {
        return level + "/" + LeaderboardNumbers.magnitude(xp);
    }

    public static String display(Stand stand) {
        if (stand == null) {
            return "-";
        }
        return display(stand.level(), stand.xp());
    }

    public static boolean fits(int level, long xp) {
        return level >= 0 && xp >= 0L;
    }

    public static String levelText(int level) {
        return Integer.toString(level);
    }

    public static String xpText(long xp) {
        return Long.toString(xp);
    }

    public static Long xpWithinLong(String text) {
        if (text == null || !text.matches("\\d+")) {
            return null;
        }
        try {
            BigInteger value = new BigInteger(text);
            if (value.signum() < 0 || value.compareTo(BigInteger.valueOf(MAX_XP)) > 0) {
                return null;
            }
            return value.longValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String strip(String line) {
        if (line == null) {
            return "";
        }
        return line.replaceAll("§[0-9a-fk-or]", "").trim();
    }
}

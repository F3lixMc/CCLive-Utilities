package net.felix.utilities.Overall.eventsound.chat;

import java.util.List;
import net.felix.utilities.Overall.eventsound.data.EventSoundMatchMode;
import net.felix.utilities.Overall.eventsound.data.EventSoundRule;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prüft Chat-Zeilen gegen Schmiedezustände, Bauplan-Drops und Custom-Texte.
 */
public final class EventSoundMatcher {
    public static final List<String> SMITHING_STATES = List.of(
            "Frostgeschmiedet",
            "Lavageschmiedet",
            "Titangeschmiedet",
            "Drachengeschmiedet",
            "Dämonengeschmiedet",
            "Blitzgeschmiedet",
            "Sternengeschmiedet");

    private static final Pattern SMITHING_PATTERN = Pattern.compile(
            "^\\[Legend\\]\\s*Schmiedezustand erhalten:\\s*\\[([^\\]]+)\\].*",
            Pattern.DOTALL);

    private static final Pattern[] BLUEPRINT_PATTERNS = {
            Pattern.compile("\\[Legend\\]\\s*-?\\s*Du erh[äa]lt?st\\s+(.+?)\\s+-\\s+\\[Bauplan\\]"),
            Pattern.compile("\\+\\s*1x\\s+(.+?)\\s+-\\s+\\[Bauplan\\]"),
            Pattern.compile("Du erh[äa]lt?st\\s+(.+?)\\s+-\\s+\\[Bauplan\\]")
    };

    private static final String NAME_TOKEN = "<name>";
    private static final String[] PLAYER_ARROWS = { ">>", "＞＞", "»", "›", "≫" };
    private static final Pattern VANILLA_PLAYER_CHAT = Pattern.compile("^.{0,24}<[A-Za-z0-9_]{2,16}>\\s+");

    private EventSoundMatcher() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "")
                .trim();
    }

    public static boolean isPlayerChat(String raw) {
        return arrowEnd(raw) >= 0 || (raw != null && VANILLA_PLAYER_CHAT.matcher(raw.trim()).find());
    }

    public static String smithingState(String normalized) {
        if (normalized == null || normalized.isEmpty() || isPlayerChat(normalized)) {
            return null;
        }
        int legendIndex = normalized.indexOf("[Legend]");
        if (legendIndex < 0) {
            return null;
        }
        Matcher matcher = SMITHING_PATTERN.matcher(normalized.substring(legendIndex));
        if (!matcher.matches()) {
            return null;
        }
        return canonicalSmithingState(matcher.group(1));
    }

    public static String canonicalSmithingState(String rawState) {
        if (rawState == null) {
            return null;
        }
        if ("Sternengeschmiedet".equals(rawState) || "Sterngeschmiedet".equals(rawState) || "Starforged".equals(rawState)) {
            return "Sternengeschmiedet";
        }
        for (String state : SMITHING_STATES) {
            if (state.equals(rawState)) {
                return state;
            }
        }
        return null;
    }

    public static String blueprintName(String normalized) {
        if (normalized == null || normalized.isEmpty() || isPlayerChat(normalized)) {
            return null;
        }
        for (Pattern pattern : BLUEPRINT_PATTERNS) {
            Matcher matcher = pattern.matcher(normalized);
            if (matcher.find()) {
                String name = cleanName(matcher.group(1));
                return name.isEmpty() ? null : name;
            }
        }
        return null;
    }

    public static String cleanName(String name) {
        return normalize(name);
    }

    public static boolean matchesCustom(EventSoundRule rule, String normalized) {
        if (rule == null || rule.pattern == null || rule.pattern.isBlank() || normalized == null) {
            return false;
        }
        return matchesPrepared(rule, isPlayerChat(normalized), isPlayerChat(normalized) ? messageBody(normalized) : normalize(normalized));
    }

    public static boolean matchesPrepared(EventSoundRule rule, boolean player, String text) {
        if (rule == null || rule.pattern == null || rule.pattern.isBlank() || text == null) {
            return false;
        }
        if (player && !rule.fromPlayer) {
            return false;
        }
        if (!player && !rule.fromServer) {
            return false;
        }
        EventSoundMatchMode mode = rule.matchMode == null ? EventSoundMatchMode.CONTAINS : rule.matchMode;
        return compileCustom(rule.pattern, mode).matcher(text).find();
    }

    /** Nachricht ohne den Spielernamen vor dem Pfeil. */
    public static String messageBody(String raw) {
        int end = arrowEnd(raw);
        String body = end >= 0 ? raw.substring(end) : stripVanillaPrefix(raw);
        return normalize(body);
    }

    private static int arrowEnd(String raw) {
        if (raw == null || raw.isEmpty()) {
            return -1;
        }
        int best = -1;
        int end = -1;
        for (String arrow : PLAYER_ARROWS) {
            int index = raw.indexOf(arrow);
            if (index >= 0 && (best < 0 || index < best)) {
                best = index;
                end = index + arrow.length();
            }
        }
        return end;
    }

    private static String stripVanillaPrefix(String raw) {
        if (raw == null) {
            return "";
        }
        Matcher matcher = VANILLA_PLAYER_CHAT.matcher(raw.trim());
        if (!matcher.find()) {
            return raw;
        }
        return raw.trim().substring(matcher.end());
    }

    static Pattern compileCustom(String input, EventSoundMatchMode mode) {
        String[] parts = input.split(Pattern.quote(NAME_TOKEN), -1);
        StringBuilder regex = new StringBuilder();
        if (mode == EventSoundMatchMode.EXACT) {
            regex.append('^');
        }
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                regex.append("\\S+");
            }
            regex.append(Pattern.quote(parts[index]));
        }
        if (mode == EventSoundMatchMode.EXACT) {
            regex.append('$');
        }
        return Pattern.compile(regex.toString());
    }
}

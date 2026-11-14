package net.felix.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bestätigt einen registrierten Spieler für die Ignore-Liste und lässt das eigene Konto aus.
 */
public final class ChatIgnoreRules {
    public static final String UNKNOWN = "Kein registrierter CCLive-Spieler.";
    public static final String SELF = "Du kannst dich nicht selbst ignorieren.";

    public record Match(String name, long playerId, boolean self) {
    }

    private ChatIgnoreRules() {
    }

    public static List<Match> suggestions(List<Match> matches, String localName, Long localPlayerId) {
        List<Match> shown = new ArrayList<>();
        if (matches == null) {
            return shown;
        }
        for (Match match : matches) {
            if (match != null && !isSelf(match, localName, localPlayerId)) {
                shown.add(match);
            }
        }
        return shown;
    }

    public static Match confirmed(String typed, List<Match> matches, String localName, Long localPlayerId) {
        Match exact = exact(typed, matches);
        if (exact == null || isSelf(exact, localName, localPlayerId)) {
            return null;
        }
        return exact;
    }

    public static String rejection(String typed, List<Match> matches, String localName, Long localPlayerId) {
        Match exact = exact(typed, matches);
        if (exact == null) {
            return UNKNOWN;
        }
        if (isSelf(exact, localName, localPlayerId)) {
            return SELF;
        }
        return null;
    }

    private static Match exact(String typed, List<Match> matches) {
        if (typed == null || matches == null) {
            return null;
        }
        String key = typed.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            return null;
        }
        for (Match match : matches) {
            if (match != null && match.name != null && match.name.trim().toLowerCase(Locale.ROOT).equals(key)) {
                return match;
            }
        }
        return null;
    }

    private static boolean isSelf(Match match, String localName, Long localPlayerId) {
        if (match.self) {
            return true;
        }
        return ChatIgnoreList.isOwn(match.name, localName, match.playerId, localPlayerId);
    }
}

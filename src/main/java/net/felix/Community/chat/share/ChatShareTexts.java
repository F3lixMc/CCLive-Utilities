package net.felix.Community.chat.share;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.felix.Community.playericon.PlayerIconUtility;

/**
 * Reiner Text für Share-Zeilen im PlayLegend-Chat.
 * Der Body ist der String, der an sendChat geht. Hinter {@code >>} fällt genau ein Server-Leerzeichen weg.
 */
public final class ChatShareTexts {
    public static final int CHAT_LIMIT = 256;
    public static final String TOO_LONG = "Nachricht ist zu lang.";
    public static final String ONE_SHARE = "Es kann nur Item/Build pro Nachricht geteilt werden.";
    public static final String NO_SHARE = "Kein Item/Build zum Teilen vorhanden.";

    private static final Pattern TOKEN = Pattern.compile("(?i)!(item|build)\\b");

    private ChatShareTexts() {
    }

    public record Replacement(
            String text,
            boolean share,
            String shareType,
            String displayName,
            int nameStart,
            int nameLength,
            String error) {
        public static Replacement plain(String text) {
            return new Replacement(text, false, null, null, 0, 0, null);
        }

        public static Replacement failed(String error) {
            return new Replacement(null, false, null, null, 0, 0, error);
        }
    }

    /**
     * Ersetzt höchstens einen Platzhalter. {@code itemName} und {@code buildName} sind schon bereinigt
     * und nur gesetzt, wenn ein fertiger Share existiert.
     */
    public static Replacement replace(String message, String itemName, String buildName) {
        if (message == null) {
            return Replacement.plain("");
        }
        Matcher matcher = TOKEN.matcher(message);
        int count = 0;
        String kind = null;
        int start = -1;
        int end = -1;
        while (matcher.find()) {
            count++;
            if (count == 1) {
                kind = matcher.group(1);
                start = matcher.start();
                end = matcher.end();
            }
        }
        if (count == 0) {
            return Replacement.plain(message);
        }
        if (count > 1) {
            return Replacement.failed(ONE_SHARE);
        }
        boolean build = kind.equalsIgnoreCase("build");
        String name = build ? buildName : itemName;
        if (name == null || name.isEmpty()) {
            return Replacement.failed(NO_SHARE);
        }
        String text = message.substring(0, start) + name + message.substring(end);
        if (text.length() > CHAT_LIMIT) {
            return Replacement.failed(TOO_LONG);
        }
        return new Replacement(text, true, build ? "BUILD" : "ITEM", name, start, name.length(), null);
    }

    public static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                out.append(Character.forDigit((value >> 4) & 0xF, 16));
                out.append(Character.forDigit(value & 0xF, 16));
            }
            return out.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** Klartextname vor {@code >>}, Icon und Präfix gehören nicht dazu. */
    public static String senderName(String plain) {
        if (plain == null) {
            return null;
        }
        int arrow = plain.indexOf(">>");
        if (arrow <= 0) {
            return null;
        }
        int end = arrow;
        while (end > 0 && Character.isWhitespace(plain.charAt(end - 1))) {
            end--;
        }
        if (end > 0 && PlayerIconUtility.isChatIconMarker(plain.charAt(end - 1))) {
            end--;
            while (end > 0 && Character.isWhitespace(plain.charAt(end - 1))) {
                end--;
            }
        }
        int start = end;
        while (start > 0 && isNameChar(plain.charAt(start - 1))) {
            start--;
        }
        if (end - start < 2) {
            return null;
        }
        return plain.substring(start, end);
    }

    /**
     * Nachrichtentext nach {@code >>}. Genau ein führendes Leerzeichen fällt weg,
     * weitere Leerzeichen des Textes bleiben.
     */
    public static String bodyAfterArrow(String plain) {
        if (plain == null) {
            return null;
        }
        int arrow = plain.indexOf(">>");
        if (arrow < 0) {
            return null;
        }
        String body = plain.substring(arrow + 2);
        if (!body.isEmpty() && body.charAt(0) == ' ') {
            body = body.substring(1);
        }
        return body;
    }

    public static boolean spanMatches(String body, int start, int length, String displayName) {
        if (body == null || displayName == null || length != displayName.length()) {
            return false;
        }
        if (start < 0 || start + length > body.length()) {
            return false;
        }
        return body.substring(start, start + length).equals(displayName);
    }

    private static boolean isNameChar(char ch) {
        return (ch >= 'A' && ch <= 'Z')
                || (ch >= 'a' && ch <= 'z')
                || (ch >= '0' && ch <= '9')
                || ch == '_';
    }
}

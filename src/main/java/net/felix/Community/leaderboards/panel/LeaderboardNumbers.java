package net.felix.Community.leaderboards.panel;

import java.math.BigInteger;

/**
 * Anzeige der Rohwerte.
 * K = Tausend, M = Million, B = Milliarde, T = Billion, Q = die Stufe danach.
 * Immer drei Nachkommastellen, abgeschnitten, ohne führende Nullen davor.
 */
public final class LeaderboardNumbers {
    private static final long[] STEP = {
        1L,
        1_000L,
        1_000_000L,
        1_000_000_000L,
        1_000_000_000_000L,
        1_000_000_000_000_000L
    };
    private static final char[] SUFFIX = {'\0', 'K', 'M', 'B', 'T', 'Q'};

    private LeaderboardNumbers() {
    }

    public static String magnitude(long number) {
        if (number < 0) {
            number = 0;
        }
        if (number < 1_000L) {
            return Long.toString(number);
        }
        int tier = 1;
        while (tier < STEP.length - 1 && number >= STEP[tier + 1]) {
            tier++;
        }
        long step = STEP[tier];
        long whole = number / step;
        long fraction = (number % step) * 1000L / step;
        return whole + "." + String.format("%03d", fraction) + SUFFIX[tier];
    }

    /**
     * Umkehrung von {@link #magnitude(long)}.
     * {@code 1.000M} ist 1.000.000. Eine Zahl ohne Suffix bleibt eine ganze Zahl.
     * Kommas sind Tausendertrennzeichen ({@code 1,310} → 1310).
     * Größer als ein Java-long ist keine gültige Zahl.
     */
    public static Long parseMagnitude(String text) {
        if (text == null) {
            return null;
        }
        String raw = text.trim().replace(",", "");
        if (raw.isEmpty() || raw.charAt(0) == '-') {
            return null;
        }
        int last = raw.length() - 1;
        if (Character.isLetter(raw.charAt(last))) {
            raw = raw.substring(0, last) + Character.toUpperCase(raw.charAt(last));
        }
        if (raw.chars().allMatch(Character::isDigit)) {
            return wholeLong(raw);
        }
        int dot = raw.indexOf('.');
        if (dot <= 0 || raw.length() != dot + 5) {
            return null;
        }
        char suffix = raw.charAt(raw.length() - 1);
        long scale = scale(suffix);
        if (scale < 0L || raw.charAt(dot) != '.') {
            return null;
        }
        String wholeText = raw.substring(0, dot);
        String fractionText = raw.substring(dot + 1, dot + 4);
        if (!wholeText.chars().allMatch(Character::isDigit) || !fractionText.chars().allMatch(Character::isDigit)) {
            return null;
        }
        BigInteger whole = new BigInteger(wholeText);
        BigInteger fraction = new BigInteger(fractionText);
        BigInteger step = BigInteger.valueOf(scale);
        BigInteger value = whole.multiply(step).add(fraction.multiply(step).divide(BigInteger.valueOf(1000L)));
        if (value.signum() < 0 || value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            return null;
        }
        return value.longValue();
    }

    private static Long wholeLong(String digits) {
        try {
            BigInteger value = new BigInteger(digits);
            if (value.signum() < 0 || value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                return null;
            }
            return value.longValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long scale(char suffix) {
        return switch (suffix) {
            case 'K' -> STEP[1];
            case 'M' -> STEP[2];
            case 'B' -> STEP[3];
            case 'T' -> STEP[4];
            case 'Q' -> STEP[5];
            default -> -1L;
        };
    }

    /**
     * Sekunden. 60 Sekunden werden eine Minute, 60 Minuten eine Stunde, 24 Stunden ein Tag.
     * Sekunden nur, solange noch keine volle Minute erreicht ist. Danach entfallen sie.
     * Tage, Stunden und Minuten nur, wenn sie nicht 0 sind.
     */
    public static String playtime(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        if (seconds < 60L) {
            return seconds + "sek";
        }
        long minutesTotal = seconds / 60L;
        long minutes = minutesTotal % 60L;
        long hoursTotal = minutesTotal / 60L;
        long hours = hoursTotal % 24L;
        long days = hoursTotal / 24L;

        StringBuilder text = new StringBuilder();
        appendPart(text, days > 0, days + "D");
        appendPart(text, hours > 0, hours + "H");
        appendPart(text, minutes > 0, minutes + "m");
        if (text.isEmpty()) {
            return "0sek";
        }
        return text.toString();
    }

    private static void appendPart(StringBuilder text, boolean show, String part) {
        if (!show) {
            return;
        }
        if (!text.isEmpty()) {
            text.append(' ');
        }
        text.append(part);
    }
}

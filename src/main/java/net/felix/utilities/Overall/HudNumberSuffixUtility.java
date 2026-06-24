package net.felix.utilities.Overall;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * HUD-Zahlensuffixe: K, M, B, T, danach AA..AZ, BA..BZ, … (jeweils ×1000).
 */
public final class HudNumberSuffixUtility {

    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000L);
    private static final List<String> SUFFIXES = buildSuffixList();

    private HudNumberSuffixUtility() {
    }

    private static List<String> buildSuffixList() {
        List<String> suffixes = new ArrayList<>();
        suffixes.add("");
        suffixes.add("K");
        suffixes.add("M");
        suffixes.add("B");
        suffixes.add("T");

        for (char first = 'A'; first <= 'Z'; first++) {
            for (char second = 'A'; second <= 'Z'; second++) {
                suffixes.add("" + first + second);
            }
        }
        return List.copyOf(suffixes);
    }

    public static int getSuffixTier(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return 0;
        }
        return SUFFIXES.indexOf(suffix.toUpperCase(Locale.ROOT));
    }

    public static boolean isKnownSuffix(String suffix) {
        return getSuffixTier(suffix) > 0;
    }

    public static BigDecimal multiplierForSuffix(String suffix) {
        int tier = getSuffixTier(suffix);
        if (tier <= 0) {
            return null;
        }
        return THOUSAND.pow(tier);
    }

    /**
     * Längste bekannte Suffix-Kette am Ende (zuerst 2 Buchstaben, dann 1).
     */
    public static String extractSuffix(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String trimmed = value.trim().replace(" ", "");
        if (trimmed.length() >= 2) {
            String twoLetter = trimmed.substring(trimmed.length() - 2).toUpperCase(Locale.ROOT);
            if (isTwoLetterSuffix(twoLetter) && isKnownSuffix(twoLetter)) {
                return twoLetter;
            }
        }
        char lastChar = trimmed.charAt(trimmed.length() - 1);
        if (Character.isLetter(lastChar)) {
            String oneLetter = String.valueOf(lastChar).toUpperCase(Locale.ROOT);
            if (isKnownSuffix(oneLetter)) {
                return oneLetter;
            }
        }
        return null;
    }

    public static BigDecimal parseSuffixedValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String trimmed = value.trim().replace(" ", "");
        if (trimmed.isEmpty()) {
            return null;
        }

        String suffix = extractSuffix(trimmed);
        if (suffix != null) {
            BigDecimal multiplier = multiplierForSuffix(suffix);
            BigDecimal base = parseDecimalNumber(trimmed.substring(0, trimmed.length() - suffix.length()));
            if (multiplier != null && base != null) {
                return base.multiply(multiplier);
            }
            return null;
        }
        return parsePlainNumber(trimmed);
    }

    private static boolean isTwoLetterSuffix(String suffix) {
        return suffix.length() == 2
                && Character.isLetter(suffix.charAt(0))
                && Character.isLetter(suffix.charAt(1));
    }

    public static String suffixForValue(BigDecimal value) {
        if (value == null || value.compareTo(THOUSAND) < 0) {
            return "";
        }
        for (int tier = SUFFIXES.size() - 1; tier >= 1; tier--) {
            BigDecimal multiplier = THOUSAND.pow(tier);
            if (value.compareTo(multiplier) >= 0) {
                return SUFFIXES.get(tier);
            }
        }
        return "";
    }

    public static BigDecimal valuePerSuffixUnit(BigDecimal value, String suffix) {
        int tier = getSuffixTier(suffix);
        if (tier <= 0 || value == null) {
            return value;
        }
        return value.divide(THOUSAND.pow(tier), 4, RoundingMode.HALF_UP);
    }

    public static String formatAbbreviatedValue(BigDecimal value, String suffix) {
        if (value == null || suffix == null) {
            return "0";
        }
        BigDecimal rounded = value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        if (rounded.scale() <= 0) {
            return rounded.toPlainString() + suffix;
        }
        return rounded.toPlainString() + suffix;
    }

    public static String formatAbbreviated(BigDecimal number) {
        if (number == null) {
            return "-";
        }
        if (number.signum() < 0) {
            return "-";
        }
        if (number.compareTo(THOUSAND) < 0) {
            return number.toPlainString();
        }
        String suffix = suffixForValue(number);
        if (suffix.isEmpty()) {
            return number.toPlainString();
        }
        return formatAbbreviatedValue(valuePerSuffixUnit(number, suffix), suffix);
    }

    public static String formatWithSeparators(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal normalized = value.stripTrailingZeros();
        String plain = normalized.toPlainString();
        int dotIndex = plain.indexOf('.');
        String intPart = dotIndex >= 0 ? plain.substring(0, dotIndex) : plain;
        String fracPart = dotIndex >= 0 ? plain.substring(dotIndex) : "";

        StringBuilder grouped = new StringBuilder();
        int len = intPart.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                grouped.append('.');
            }
            grouped.append(intPart.charAt(i));
        }
        return grouped.append(fracPart).toString();
    }

    public static boolean fitsInLong(BigDecimal value) {
        return value != null
                && value.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) >= 0
                && value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) <= 0
                && value.stripTrailingZeros().scale() <= 0;
    }

    public static long toLongOrMax(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return -1L;
        }
        if (!fitsInLong(value)) {
            return Long.MAX_VALUE;
        }
        return value.longValue();
    }

    private static BigDecimal parsePlainNumber(String decoded) {
        if (decoded == null || decoded.isEmpty()) {
            return null;
        }
        String cleaned = decoded.replaceAll("[^0-9.,]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        cleaned = cleaned.replace(".", "").replace(",", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseDecimalNumber(String numberPart) {
        if (numberPart == null || numberPart.isEmpty()) {
            return null;
        }
        String normalized = numberPart.replace(" ", "");
        int lastDot = normalized.lastIndexOf('.');
        int lastComma = normalized.lastIndexOf(',');
        int decimalSep = Math.max(lastDot, lastComma);

        if (decimalSep >= 0) {
            String intPart = normalized.substring(0, decimalSep).replace(".", "").replace(",", "");
            String fracPart = normalized.substring(decimalSep + 1).replace(".", "").replace(",", "");
            if (intPart.isEmpty()) {
                intPart = "0";
            }
            try {
                return new BigDecimal(intPart + "." + fracPart, MathContext.UNLIMITED);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        String digitsOnly = normalized.replace(".", "").replace(",", "");
        try {
            return new BigDecimal(digitsOnly);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

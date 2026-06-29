package net.felix.utilities.Overall;

import net.minecraft.client.MinecraftClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dekodiert Werte aus der oberen HUD-Bossbar anhand der Pixel-Font-Glyphen.
 * Factory: Souls, Coins, Cactus
 * Aincraft (floor_X): Souls, Coins, Ebene, Cactus
 */
public final class BossBarHudValueDecoder {

    private static final char VALUE_SEPARATOR = '㓾';
    private static final char VALUE_PREFIX = '㔘';

    private BossBarHudValueDecoder() {
    }

    public static boolean looksLikeHudStatsBar(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.indexOf(VALUE_PREFIX) >= 0;
    }

    /**
     * @return dekodierte Wert-Strings in Reihenfolge (typisch: Souls, Coins, Cactus)
     */
    public static List<String> extractDecodedValues(String text) {
        return extractDecodedValues(text, getMappingForCurrentDimension());
    }

    public static List<String> extractDecodedValues(String text, Map<Character, String> mapping) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty() || mapping == null || mapping.isEmpty()) {
            return result;
        }

        String pixelSpacer = ZeichenUtility.getPixelSpacer();
        List<String> rawValueStrings = extractValueStringsByStructure(text, mapping, pixelSpacer);

        for (String valueString : rawValueStrings) {
            StringBuilder decoded = new StringBuilder();
            for (char c : valueString.toCharArray()) {
                String decodedChar = mapping.get(c);
                if (decodedChar != null) {
                    decoded.append(decodedChar);
                }
            }
            String decodedValue = decoded.toString().trim();
            if (!decodedValue.isEmpty()) {
                result.add(decodedValue);
            }
        }
        return result;
    }

    /**
     * Liest Coins aus der Bossbar ohne Dimensions-Fontwahl – probiert Factory- und Aincraft-Mapping.
     */
    public static ParsedValue parseCoinsFromBossBar(String bossBarText) {
        if (!looksLikeHudStatsBar(bossBarText)) {
            return ParsedValue.invalid();
        }

        ParsedValue best = ParsedValue.invalid();
        int bestScore = -1;

        Map<Character, String> factoryMapping = ZeichenUtility.getFactoryFontFirstLine();
        Map<Character, String> aincraftMapping = ZeichenUtility.getAincraftFontFirstLine();

        if (factoryMapping != null && !factoryMapping.isEmpty()) {
            int score = scoreCoinsParse(bossBarText, factoryMapping);
            if (score >= 0) {
                ParsedValue candidate = parseCoinsWithMapping(bossBarText, factoryMapping);
                if (candidate.isValid() && score > bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
        }

        if (aincraftMapping != null && !aincraftMapping.isEmpty()) {
            int score = scoreCoinsParse(bossBarText, aincraftMapping);
            if (score >= 0) {
                ParsedValue candidate = parseCoinsWithMapping(bossBarText, aincraftMapping);
                if (candidate.isValid() && score > bestScore) {
                    best = candidate;
                }
            }
        }

        return best;
    }

    private static ParsedValue parseCoinsWithMapping(String bossBarText, Map<Character, String> mapping) {
        List<String> values = extractDecodedValues(bossBarText, mapping);
        if (values.size() < 2) {
            return ParsedValue.invalid();
        }
        return parseValue(values.get(1));
    }

    private static int scoreCoinsParse(String bossBarText, Map<Character, String> mapping) {
        List<String> values = extractDecodedValues(bossBarText, mapping);
        if (values.size() < 2) {
            return -1;
        }
        ParsedValue coins = parseValue(values.get(1));
        if (!coins.isValid()) {
            return -1;
        }
        int score = values.size() * 10 + coins.display.length();
        if (values.size() >= 1 && parseValue(values.get(0)).isValid()) {
            score += 5;
        }
        return score;
    }

    public static long parseNumericValue(String decoded) {
        ParsedValue parsed = parseValue(decoded);
        return parsed.toLongOrMax();
    }

    /**
     * Parst einen dekodierten HUD-Wert inkl. Suffix (K, M, B, T, …, AA, AB, …).
     */
    public static ParsedValue parseValue(String decoded) {
        if (decoded == null || decoded.isEmpty()) {
            return ParsedValue.invalid();
        }

        String trimmed = decoded.trim().replace(" ", "");
        if (trimmed.isEmpty()) {
            return ParsedValue.invalid();
        }

        BigDecimal numericValue = HudNumberSuffixUtility.parseSuffixedValue(trimmed);
        if (numericValue == null || numericValue.signum() < 0) {
            return ParsedValue.invalid();
        }
        String suffix = HudNumberSuffixUtility.extractSuffix(trimmed);
        if (suffix == null) {
            suffix = "";
        }
        return new ParsedValue(numericValue, trimmed, suffix);
    }

    public static final class ParsedValue {
        public final BigDecimal numericValue;
        /** Anzeige-String wie aus der HUD (z. B. {@code 56,9B}) */
        public final String display;
        public final String suffix;

        public ParsedValue(BigDecimal numericValue, String display, String suffix) {
            this.numericValue = numericValue;
            this.display = display;
            this.suffix = suffix;
        }

        public static ParsedValue invalid() {
            return new ParsedValue(null, "", "");
        }

        public boolean isValid() {
            return numericValue != null && numericValue.signum() >= 0;
        }

        public boolean hasSuffix() {
            return suffix != null && !suffix.isEmpty();
        }

        public long toLongOrMax() {
            return HudNumberSuffixUtility.toLongOrMax(numericValue);
        }
    }

    /**
     * Normalisiert einen HUD-Anzeige-String fürs Overlay.
     * z. B. {@code 1,256 K} → {@code 1.256K}, {@code 56,9B} → {@code 56.9B}
     */
    public static String normalizeHudDisplay(String display) {
        if (display == null || display.isEmpty()) {
            return "";
        }

        String trimmed = display.trim().replace(" ", "");
        if (trimmed.isEmpty()) {
            return "";
        }

        String suffix = HudNumberSuffixUtility.extractSuffix(trimmed);
        if (suffix == null) {
            return normalizeFullNumber(trimmed);
        }

        String numberPart = trimmed.substring(0, trimmed.length() - suffix.length());
        return normalizeAbbreviatedNumberPart(numberPart) + suffix;
    }

    /** Wie {@link #normalizeHudDisplay(String)}, Suffix in Kleinbuchstaben (z. B. {@code 4.154fv}). */
    public static String normalizeHudDisplayLowercase(String display) {
        String normalized = normalizeHudDisplay(display);
        if (normalized.isEmpty()) {
            return normalized;
        }
        String suffix = HudNumberSuffixUtility.extractSuffix(normalized);
        if (suffix == null || suffix.isEmpty()) {
            return normalized;
        }
        return normalized.substring(0, normalized.length() - suffix.length())
                + suffix.toLowerCase(java.util.Locale.ROOT);
    }

    /** Parst HUD-Abkürzungen (z. B. {@code 1.524kg}, {@code 999.999aa}) für Mengenvergleiche. */
    public static BigDecimal parseHudAmount(String display) {
        if (display == null || display.isBlank()) {
            return BigDecimal.ZERO;
        }
        String normalized = normalizeHudDisplayLowercase(display);
        BigDecimal parsed = HudNumberSuffixUtility.parseSuffixedValue(normalized);
        return parsed != null ? parsed : BigDecimal.ZERO;
    }

    private static String normalizeFullNumber(String numberPart) {
        if (numberPart == null || numberPart.isEmpty()) {
            return "";
        }
        String digits = numberPart.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return numberPart;
        }
        try {
            return String.format(java.util.Locale.GERMANY, "%,d", Long.parseLong(digits))
                    .replace(',', '.');
        } catch (NumberFormatException e) {
            return numberPart;
        }
    }

    private static String normalizeAbbreviatedNumberPart(String numberPart) {
        if (numberPart == null || numberPart.isEmpty()) {
            return "";
        }

        int lastComma = numberPart.lastIndexOf(',');
        int lastDot = numberPart.lastIndexOf('.');
        int sepIndex = Math.max(lastComma, lastDot);

        if (sepIndex < 0) {
            return numberPart;
        }

        char sep = numberPart.charAt(sepIndex);
        String afterSep = numberPart.substring(sepIndex + 1);
        String beforeSep = numberPart.substring(0, sepIndex);

        if (afterSep.length() == 3 && afterSep.chars().allMatch(Character::isDigit)) {
            return beforeSep + "." + afterSep;
        }

        if (sep == ',') {
            return beforeSep + "." + afterSep;
        }

        return numberPart;
    }

    public static HudStats parseHudStats(String bossBarText) {
        List<String> values = extractDecodedValues(bossBarText);
        HudStats stats = new HudStats();
        boolean floorDimension = isFloorDimension();

        if (values.size() >= 1) {
            ParsedValue souls = parseValue(values.get(0));
            if (souls.isValid()) {
                stats.souls = souls.numericValue;
                stats.soulsDisplay = souls.display;
            }
        }
        if (values.size() >= 2) {
            ParsedValue coins = parseValue(values.get(1));
            if (coins.isValid()) {
                stats.coins = coins.numericValue;
                stats.coinsDisplay = coins.display;
            }
        }
        if (floorDimension && values.size() >= 3) {
            ParsedValue floor = parseValue(values.get(2));
            if (floor.isValid()) {
                stats.floorLevel = floor.numericValue;
                stats.floorDisplay = floor.display;
            }
        }

        int cactusIndex = floorDimension ? 3 : 2;
        if (values.size() > cactusIndex) {
            ParsedValue cactus = parseValue(values.get(cactusIndex));
            if (cactus.isValid()) {
                stats.cactus = cactus.numericValue;
                stats.cactusDisplay = cactus.display;
            }
        }
        return stats;
    }

    public static boolean isFloorDimension() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.world != null) {
            String dimensionPath = client.world.getRegistryKey().getValue().getPath();
            return dimensionPath.toLowerCase().contains("floor");
        }
        return false;
    }

    private static Map<Character, String> getMappingForCurrentDimension() {
        return isFloorDimension()
                ? ZeichenUtility.getAincraftFontFirstLine()
                : ZeichenUtility.getFactoryFontFirstLine();
    }

    private static List<String> extractValueStringsByStructure(
            String originalText,
            Map<Character, String> mapping,
            String pixelSpacer) {
        List<String> valueStrings = new ArrayList<>();
        if (originalText == null || originalText.isEmpty()) {
            return valueStrings;
        }

        for (int i = 0; i < originalText.length(); i++) {
            if (originalText.charAt(i) != VALUE_PREFIX) {
                continue;
            }

            int valueStart = i + 1;
            while (valueStart < originalText.length()
                    && pixelSpacer.indexOf(originalText.charAt(valueStart)) >= 0) {
                valueStart++;
            }

            int valueEnd = -1;
            for (int j = valueStart; j < originalText.length(); j++) {
                char c = originalText.charAt(j);

                if (c == VALUE_PREFIX && j > valueStart) {
                    valueEnd = j;
                    break;
                }

                if (j < originalText.length() - 1) {
                    char next = originalText.charAt(j + 1);
                    if ((c == '㔆' || c == '㔃' || c == '㔅') && next == VALUE_SEPARATOR) {
                        valueEnd = j;
                        break;
                    }
                    if ((c == '㔆' || c == '㔃' || c == '㔅')
                            && j + 1 < originalText.length()
                            && originalText.charAt(j + 1) == VALUE_PREFIX) {
                        valueEnd = j + 1;
                        break;
                    }
                }

                if (c == VALUE_SEPARATOR && j + 1 < originalText.length()
                        && originalText.charAt(j + 1) == VALUE_PREFIX) {
                    valueEnd = j;
                    break;
                }
            }

            if (valueEnd == -1) {
                valueEnd = originalText.length();
            }

            if (valueEnd > valueStart) {
                StringBuilder valueString = new StringBuilder();
                for (int k = valueStart; k < valueEnd; k++) {
                    char c = originalText.charAt(k);
                    if (mapping.containsKey(c)) {
                        valueString.append(c);
                    }
                }
                if (valueString.length() > 0) {
                    valueStrings.add(valueString.toString());
                }
            }

            i = valueEnd - 1;
        }
        return valueStrings;
    }

    public static final class HudStats {
        public BigDecimal souls;
        public BigDecimal coins;
        public BigDecimal cactus;
        public BigDecimal floorLevel;
        public String soulsDisplay = "";
        public String coinsDisplay = "";
        public String cactusDisplay = "";
        public String floorDisplay = "";

        public boolean hasCoins() {
            return coins != null && coins.signum() >= 0;
        }
    }
}

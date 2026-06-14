package net.felix.utilities.Overall;

import net.minecraft.client.MinecraftClient;

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
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }

        Map<Character, String> mapping = getMappingForCurrentDimension();
        if (mapping.isEmpty()) {
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

    public static long parseNumericValue(String decoded) {
        ParsedValue parsed = parseValue(decoded);
        return parsed.value;
    }

    /**
     * Parst einen dekodierten HUD-Wert inkl. optionalem Suffix-Buchstaben (K, M, B, T).
     */
    public static ParsedValue parseValue(String decoded) {
        if (decoded == null || decoded.isEmpty()) {
            return ParsedValue.invalid();
        }

        String trimmed = decoded.trim().replace(" ", "");
        if (trimmed.isEmpty()) {
            return ParsedValue.invalid();
        }

        char lastChar = trimmed.charAt(trimmed.length() - 1);
        if (Character.isLetter(lastChar)) {
            String suffix = String.valueOf(lastChar).toUpperCase(java.util.Locale.ROOT);
            double multiplier = suffixMultiplier(suffix);
            if (multiplier > 0) {
                String numberPart = trimmed.substring(0, trimmed.length() - 1).trim();
                double base = parseDecimalNumber(numberPart);
                if (base >= 0) {
                    long value = Math.round(base * multiplier);
                    return new ParsedValue(value, trimmed, suffix);
                }
            }
        }

        long value = parsePlainNumericValue(trimmed);
        if (value < 0) {
            return ParsedValue.invalid();
        }
        return new ParsedValue(value, trimmed, "");
    }

    private static long parsePlainNumericValue(String decoded) {
        if (decoded == null || decoded.isEmpty()) {
            return -1;
        }
        String cleaned = decoded.replaceAll("[^0-9.,]", "");
        if (cleaned.isEmpty()) {
            return -1;
        }
        cleaned = cleaned.replace(".", "").replace(",", "");
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static double parseDecimalNumber(String numberPart) {
        if (numberPart == null || numberPart.isEmpty()) {
            return -1;
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
                return Double.parseDouble(intPart + "." + fracPart);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        String digitsOnly = normalized.replace(".", "").replace(",", "");
        try {
            return Double.parseDouble(digitsOnly);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static double suffixMultiplier(String suffix) {
        return switch (suffix) {
            case "K" -> 1_000.0;
            case "M" -> 1_000_000.0;
            case "B" -> 1_000_000_000.0;
            case "T" -> 1_000_000_000_000.0;
            default -> -1;
        };
    }

    public static final class ParsedValue {
        public final long value;
        /** Anzeige-String wie aus der HUD (z. B. {@code 56,9B}) */
        public final String display;
        public final String suffix;

        public ParsedValue(long value, String display, String suffix) {
            this.value = value;
            this.display = display;
            this.suffix = suffix;
        }

        public static ParsedValue invalid() {
            return new ParsedValue(-1, "", "");
        }

        public boolean isValid() {
            return value >= 0;
        }

        public boolean hasSuffix() {
            return suffix != null && !suffix.isEmpty();
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

        char lastChar = trimmed.charAt(trimmed.length() - 1);
        if (!Character.isLetter(lastChar)) {
            return normalizeFullNumber(trimmed);
        }

        String suffix = String.valueOf(lastChar);
        String numberPart = trimmed.substring(0, trimmed.length() - 1);
        return normalizeAbbreviatedNumberPart(numberPart) + suffix;
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
                stats.souls = souls.value;
                stats.soulsDisplay = souls.display;
            }
        }
        if (values.size() >= 2) {
            ParsedValue coins = parseValue(values.get(1));
            if (coins.isValid()) {
                stats.coins = coins.value;
                stats.coinsDisplay = coins.display;
            }
        }
        if (floorDimension && values.size() >= 3) {
            ParsedValue floor = parseValue(values.get(2));
            if (floor.isValid()) {
                stats.floorLevel = floor.value;
                stats.floorDisplay = floor.display;
            }
        }

        int cactusIndex = floorDimension ? 3 : 2;
        if (values.size() > cactusIndex) {
            ParsedValue cactus = parseValue(values.get(cactusIndex));
            if (cactus.isValid()) {
                stats.cactus = cactus.value;
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
        public long souls = -1;
        public long coins = -1;
        public long cactus = -1;
        public long floorLevel = -1;
        public String soulsDisplay = "";
        public String coinsDisplay = "";
        public String cactusDisplay = "";
        public String floorDisplay = "";

        public boolean hasCoins() {
            return coins >= 0;
        }
    }
}

package net.felix.utilities.Aincraft;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.felix.utilities.Overall.ActionBarData;
import net.felix.utilities.Overall.HudNumberSuffixUtility;
import net.minecraft.util.Formatting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Berechnet Materialien pro Minute aus Action-Bar-Session-Daten.
 * Scoreboard-Zeilen (➥ ANZAHL) werden per Materialfarbe zugeordnet.
 */
public final class MaterialRateUtility {

    private static final Pattern SCOREBOARD_MATERIAL_COUNT =
            Pattern.compile("➥\\s*(\\d+)");
    private static final int RATE_SUFFIX_COLOR = 0xAAAAAA;

    private static final Map<String, MaterialSession> sessions = new HashMap<>();

    private MaterialRateUtility() {
    }

    public static void resetSession() {
        sessions.clear();
    }

    public static void updateFromActionBar() {
        Map<String, Integer> materials = ActionBarData.getMaterials();
        if (materials.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            String cleanName = entry.getKey();
            int count = entry.getValue();
            Integer nameColorRgb = ActionBarData.getMaterialNameColorRgb(cleanName);
            if (nameColorRgb == null) {
                Text original = ActionBarData.getMaterialText(cleanName);
                if (original != null) {
                    nameColorRgb = extractLegacyColorRgb(original.getString());
                }
            }
            String sessionKey = normalizeMaterialName(cleanName);

            MaterialSession session = sessions.get(sessionKey);
            if (session == null) {
                session = new MaterialSession(cleanName, nameColorRgb, count, now);
                sessions.put(sessionKey, session);
                session.updateRate(now);
            } else if (count < session.initialCount) {
                session.reset(count, now, nameColorRgb);
                session.updateRate(now);
            } else {
                session.currentCount = count;
                session.nameColorRgb = nameColorRgb;
                session.updateRate(now);
            }
        }
    }

    public static Text appendRateForMaterial(String materialName, Text baseText) {
        if (baseText == null) {
            return Text.empty();
        }

        MaterialSession session = findSessionByNormalizedName(materialName);
        double rate = session != null ? session.perMinute : 0.0;
        return appendRateSuffix(baseText, rate);
    }

    public static Text appendRateToScoreboardLine(Text lineText, int sortedMaterialIndex) {
        if (lineText == null) {
            return Text.empty();
        }

        MaterialSession session = findSessionForScoreboardLine(lineText, sortedMaterialIndex);
        double rate = session != null ? session.perMinute : 0.0;
        return appendRateSuffix(lineText, rate);
    }

    public static boolean isScoreboardMaterialLine(String cleanLine) {
        if (cleanLine == null || cleanLine.isEmpty()) {
            return false;
        }
        String trimmed = cleanLine.stripLeading();
        return trimmed.startsWith("➥") && SCOREBOARD_MATERIAL_COUNT.matcher(trimmed).find();
    }

    public static String formatRate(double rate) {
        if (rate <= 0) {
            return "x/min";
        }
        return formatRateValue(rate);
    }

    private static String formatRateValue(double rate) {
        long rounded = Math.round(rate);
        if (rounded > 0) {
            return HudNumberSuffixUtility.formatAbbreviated(BigDecimal.valueOf(rounded)) + "/min";
        }
        return BigDecimal.valueOf(rate)
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "/min";
    }

    private static MutableText appendRateSuffix(Text baseText, double rate) {
        MutableText result = baseText.copy();
        result.append(Text.literal(" -> " + formatRate(rate))
                .setStyle(Style.EMPTY.withColor(RATE_SUFFIX_COLOR)));
        return result;
    }

    public static double getRateForMaterial(String materialName) {
        MaterialSession session = findSessionByNormalizedName(materialName);
        return session != null ? session.perMinute : 0.0;
    }

    private static MaterialSession findSessionForScoreboardLine(Text lineText, int sortedMaterialIndex) {
        String clean = cleanDisplayText(lineText.getString());
        if (!isScoreboardMaterialLine(clean)) {
            return null;
        }

        Integer lineColor = extractLastNonWhiteColorRgb(lineText);
        if (lineColor == null) {
            lineColor = extractLegacyColorRgb(lineText.getString());
        }
        Integer lineCount = extractScoreboardCount(clean);

        MaterialSession colorMatch = null;
        for (MaterialSession session : sessions.values()) {
            if (lineColor != null && colorsMatch(lineColor, session.nameColorRgb)) {
                if (lineCount != null && lineCount == session.currentCount) {
                    return session;
                }
                if (colorMatch == null) {
                    colorMatch = session;
                }
            }
        }

        if (colorMatch != null) {
            return colorMatch;
        }

        if (lineCount != null) {
            MaterialSession countMatch = findUniqueSessionByCount(lineCount);
            if (countMatch != null) {
                return countMatch;
            }
        }

        return findSessionBySortedIndex(sortedMaterialIndex);
    }

    private static MaterialSession findSessionBySortedIndex(int sortedMaterialIndex) {
        if (sortedMaterialIndex < 0) {
            return null;
        }

        java.util.List<String> sortedNames = ActionBarData.getSortedMaterialNames();
        if (sortedMaterialIndex >= sortedNames.size()) {
            return null;
        }

        return findSessionByNormalizedName(sortedNames.get(sortedMaterialIndex));
    }

    private static boolean colorsMatch(Integer lineColor, Integer sessionColor) {
        if (lineColor == null || sessionColor == null) {
            return false;
        }
        if (lineColor.equals(sessionColor)) {
            return true;
        }

        Integer lineLegacy = toLegacyFormattingRgb(lineColor);
        Integer sessionLegacy = toLegacyFormattingRgb(sessionColor);
        return lineLegacy != null && lineLegacy.equals(sessionLegacy);
    }

    private static Integer toLegacyFormattingRgb(int rgb) {
        for (Formatting formatting : Formatting.values()) {
            if (formatting.getColorValue() != null && (formatting.getColorValue() & 0xFFFFFF) == rgb) {
                return formatting.getColorValue() & 0xFFFFFF;
            }
        }
        return rgb;
    }

    private static MaterialSession findUniqueSessionByCount(int count) {
        MaterialSession match = null;
        for (MaterialSession session : sessions.values()) {
            if (session.currentCount == count) {
                if (match != null) {
                    return null;
                }
                match = session;
            }
        }
        return match;
    }

    private static Integer extractScoreboardCount(String cleanLine) {
        Matcher matcher = SCOREBOARD_MATERIAL_COUNT.matcher(cleanLine.stripLeading());
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static MaterialSession findSessionByNormalizedName(String name) {
        String normalized = normalizeMaterialName(name);
        if (normalized.isEmpty()) {
            return null;
        }
        MaterialSession direct = sessions.get(normalized);
        if (direct != null) {
            return direct;
        }
        for (MaterialSession session : sessions.values()) {
            if (normalizeMaterialName(session.cleanName).equals(normalized)) {
                return session;
            }
        }
        return null;
    }

    private static Integer extractLastNonWhiteColorRgb(Text text) {
        if (text == null) {
            return null;
        }

        Integer last = colorRgb(text.getStyle().getColor());
        for (Text sibling : text.getSiblings()) {
            Integer nested = extractLastNonWhiteColorRgb(sibling);
            if (nested != null) {
                last = nested;
            }
        }
        return last;
    }

    private static Integer colorRgb(TextColor color) {
        if (color == null) {
            return null;
        }
        int rgb = color.getRgb() & 0xFFFFFF;
        if (rgb == 0xFFFFFF) {
            return null;
        }
        return rgb;
    }

    private static Integer extractLegacyColorRgb(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        Integer lastColor = null;
        for (int i = 0; i < raw.length() - 1; i++) {
            if (raw.charAt(i) != '\u00A7') {
                continue;
            }
            Formatting formatting = Formatting.byCode(raw.charAt(i + 1));
            if (formatting != null && formatting.getColorValue() != null) {
                lastColor = formatting.getColorValue() & 0xFFFFFF;
            }
        }
        return lastColor;
    }

    private static String normalizeMaterialName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        return name.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF]", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static String cleanDisplayText(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        return line.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF]", "")
                .trim();
    }

    private static final class MaterialSession {
        final String cleanName;
        Integer nameColorRgb;
        int initialCount;
        int currentCount;
        long sessionStartMs;
        double perMinute;

        MaterialSession(String cleanName, Integer nameColorRgb, int initialCount, long sessionStartMs) {
            this.cleanName = cleanName;
            this.nameColorRgb = nameColorRgb;
            this.initialCount = initialCount;
            this.currentCount = initialCount;
            this.sessionStartMs = sessionStartMs;
            this.perMinute = 0.0;
        }

        void reset(int newInitial, long now, Integer nameColorRgb) {
            this.initialCount = newInitial;
            this.currentCount = newInitial;
            this.sessionStartMs = now;
            this.nameColorRgb = nameColorRgb;
            this.perMinute = 0.0;
        }

        void updateRate(long now) {
            long elapsed = now - sessionStartMs;
            if (elapsed <= 0) {
                perMinute = 0.0;
                return;
            }

            int gained = currentCount - initialCount;
            if (gained <= 0) {
                perMinute = 0.0;
                return;
            }

            double minutes = elapsed / 60000.0;
            perMinute = gained / minutes;
        }
    }
}

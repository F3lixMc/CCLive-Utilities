package net.felix.utilities.Other.clipboard;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Entscheidet Restzeit, Zeilenfenster und Leertexte für das Clipboard.
 * Rechnet ohne Minecraft-Client, damit die Regeln testbar bleiben.
 */
public final class ClipboardTargetTime {
    public static final String NO_BLUEPRINTS = "Keine Baupläne im Clipboard";
    public static final String ALL_COLLECTED = "Alle Materialien gesammelt";
    public static final int TIME_COLOR = 0xFFFFFF55;
    public static final int COLLECTED_COLOR = 0xFF00FF00;

    private static final long MILLIS_PER_MINUTE = 60_000L;

    private ClipboardTargetTime() {
    }

    public static String formatUnit(long remainingMillis) {
        if (remainingMillis <= 0L) {
            return "1s";
        }
        long seconds = remainingMillis / 1000L;
        if (seconds <= 0L) {
            return "1s";
        }
        if (seconds >= 3600L) {
            return (seconds / 3600L) + "h";
        }
        if (seconds >= 60L) {
            return (seconds / 60L) + "m";
        }
        return seconds + "s";
    }

    public static String formatRemaining(BigDecimal missing, double perMinute) {
        if (missing == null || missing.signum() <= 0 || !positiveRate(perMinute)) {
            return "";
        }
        BigDecimal millis = missing.multiply(BigDecimal.valueOf(MILLIS_PER_MINUTE))
                .divide(BigDecimal.valueOf(perMinute), 0, java.math.RoundingMode.DOWN);
        long remaining = millis.longValue();
        if (remaining <= 0L) {
            remaining = 1L;
        }
        return " (" + formatUnit(remaining) + ")";
    }

    /**
     * @param displayMode wird absichtlich ignoriert: Overlay und Scoreboard teilen dieselbe Coin-Rate
     */
    public static double coinRate(boolean trackerEnabled, boolean onFloor, int displayMode, double perMinute) {
        if (!trackerEnabled || !onFloor || !positiveRate(perMinute)) {
            return 0.0;
        }
        return perMinute;
    }

    public static double farmRate(String lineName, String trackedName, double perMinute) {
        if (!sameName(lineName, trackedName) || !positiveRate(perMinute)) {
            return 0.0;
        }
        return perMinute;
    }

    public static double rateFor(int category, String itemName, boolean shopLine, boolean complete,
                                 boolean coinTrackerEnabled, boolean onFloor, int coinDisplayMode, double coinPerMinute,
                                 double materialPerMinute, String farmName, double farmPerMinute) {
        if (shopLine || complete || category == 2 || isFurnaceItem(itemName)) {
            return 0.0;
        }
        if (category == 0) {
            return coinRate(coinTrackerEnabled, onFloor, coinDisplayMode, coinPerMinute);
        }
        if (category == 1) {
            return positiveRate(materialPerMinute) ? materialPerMinute : 0.0;
        }
        if (category == 3) {
            return farmRate(itemName, farmName, farmPerMinute);
        }
        return 0.0;
    }

    public static boolean isFurnaceItem(String itemName) {
        String normalized = normalize(itemName);
        return "netherite schrott".equals(normalized)
                || "eisenbarren".equals(normalized)
                || "goldbarren".equals(normalized)
                || "kupferbarren".equals(normalized);
    }

    public static String totalKey(int category, String itemName) {
        if (itemName != null && itemName.equalsIgnoreCase("Coins")) {
            return "0\0coins";
        }
        return category + "\0" + normalize(itemName);
    }

    public static String statusText(boolean hasEntries, int fullNormalCount, int visibleNormalCount) {
        if (!hasEntries) {
            return NO_BLUEPRINTS;
        }
        if (fullNormalCount == 0 && visibleNormalCount == 0) {
            return ALL_COLLECTED;
        }
        return null;
    }

    public static CostWindow window(int normalCount, int shopCount, int maxLinesOrZero, int scrollOffset) {
        int budget = maxLinesOrZero <= 0
                ? Math.max(0, normalCount)
                : Math.max(0, maxLinesOrZero - Math.max(0, shopCount));
        int maxOffset = Math.max(0, normalCount - budget);
        int start = Math.max(0, Math.min(scrollOffset, maxOffset));
        int count = Math.min(budget, Math.max(0, normalCount - start));
        boolean scrollable = budget > 0 && normalCount > budget;
        boolean overflow = normalCount > count;
        return new CostWindow(start, count, maxOffset, scrollable, overflow);
    }

    public static boolean tryParseMaxLines(String raw, int[] out) {
        if (out == null || out.length == 0) {
            return false;
        }
        if (raw == null || raw.isBlank()) {
            out[0] = 0;
            return true;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value > 0) {
                out[0] = value;
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    public static boolean sameName(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        return !a.isEmpty() && a.equals(b);
    }

    public static String normalize(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        return name.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF]", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static boolean positiveRate(double perMinute) {
        return perMinute > 0.0 && !Double.isNaN(perMinute) && !Double.isInfinite(perMinute);
    }

    public static final class CostWindow {
        public final int start;
        public final int count;
        public final int maxOffset;
        public final boolean scrollable;
        public final boolean overflow;

        public CostWindow(int start, int count, int maxOffset, boolean scrollable, boolean overflow) {
            this.start = start;
            this.count = count;
            this.maxOffset = maxOffset;
            this.scrollable = scrollable;
            this.overflow = overflow;
        }
    }
}

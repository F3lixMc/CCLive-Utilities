package net.felix.utilities.Other.clipboard;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * Entscheidet Restzeit, Zeilenfenster und Leertexte für das Clipboard.
 * Rechnet ohne Minecraft-Client, damit die Regeln testbar bleiben.
 */
public final class ClipboardTargetTime {
    public static final String NO_BLUEPRINTS = "Keine Baupläne im Clipboard";
    public static final String ALL_COLLECTED = "Alle Materialien gesammelt";
    /** Gelbe Restzeit. */
    public static final int TIME_COLOR = 0xFFFFFF55;
    public static final int COLLECTED_COLOR = 0xFF00FF00;
    /** Platzhalter, solange eine Rate noch fehlt. Wird als mittige Punkte gezeichnet. */
    public static final String CALCULATING = "\u0000";
    public static final String CALCULATING_PREFIX = " ≈ ";
    public static final int CALCULATING_DOT_COUNT = 3;
    public static final int CALCULATING_DOT_SIZE = 2;
    public static final int CALCULATING_DOT_GAP = 2;
    /** Oberkante der Punkte in der Textzeile, auf der Höhe eines Bindestrichs. */
    public static final int CALCULATING_DOT_TOP = 3;

    private ClipboardTargetTime() {
    }

    /**
     * Nur die größte Einheit: ab 60 Minuten {@code 1h}, darunter {@code 59min}, darunter {@code 30sek}.
     */
    public static String formatDuration(double minutes) {
        if (!positiveRate(minutes)) {
            return "";
        }
        long totalSeconds = Math.max(1L, (long) Math.floor(minutes * 60.0));
        long hours = totalSeconds / 3600L;
        if (hours > 0) {
            return hours + "h";
        }
        long mins = totalSeconds / 60L;
        if (mins > 0) {
            return mins + "min";
        }
        return totalSeconds + "sek";
    }

    /**
     * Nächste Einheit dazu, wenn sie nicht null ist: {@code 1h 30min} oder {@code 3min 30sek}.
     * Die kurze Form bleibt {@link #formatDuration}.
     */
    public static String formatDurationDetailed(double minutes) {
        if (!positiveRate(minutes)) {
            return "";
        }
        long totalSeconds = Math.max(1L, (long) Math.floor(minutes * 60.0));
        long hours = totalSeconds / 3600L;
        long mins = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            if (mins > 0) {
                return hours + "h " + mins + "min";
            }
            return hours + "h";
        }
        if (mins > 0) {
            if (seconds > 0) {
                return mins + "min " + seconds + "sek";
            }
            return mins + "min";
        }
        return totalSeconds + "sek";
    }

    /**
     * Die längere Zeit nur, wenn sie rechts noch in die Zeile passt. Sonst die kurze Einheit.
     */
    public static String chooseTimeText(String compact, String detailed, int cursor, int detailedWidth, int rightEdge) {
        if (detailed != null && !detailed.isEmpty() && !detailed.equals(compact)
                && cursor + detailedWidth <= rightEdge) {
            return detailed;
        }
        return compact == null ? "" : compact;
    }

    public static boolean isCalculating(String text) {
        return CALCULATING.equals(text);
    }

    public static int calculatingDotsWidth() {
        return CALCULATING_DOT_COUNT * CALCULATING_DOT_SIZE
                + (CALCULATING_DOT_COUNT - 1) * CALCULATING_DOT_GAP;
    }

    public static String formatRemaining(BigDecimal missing, double perMinute) {
        if (missing == null || missing.signum() <= 0 || !positiveRate(perMinute)) {
            return "";
        }
        return " ≈ " + formatDuration(missing.doubleValue() / perMinute);
    }

    /** Kurze und ausführliche Restzeit. Ohne positive Rate bleiben beide leer. */
    public static String[] timeLabels(BigDecimal missing, double perMinute) {
        if (missing == null || missing.signum() <= 0 || !positiveRate(perMinute)) {
            return new String[] {"", ""};
        }
        double minutes = missing.doubleValue() / perMinute;
        return new String[] {
                " ≈ " + formatDuration(minutes),
                " ≈ " + formatDurationDetailed(minutes)
        };
    }

    /**
     * Shopkosten kommen nur bei aktivierter Option in die normale Liste.
     * Gleiche Schlüssel, etwa Coins, werden addiert.
     */
    public static Map<String, BigDecimal> mergeCosts(Map<String, BigDecimal> normal, Map<String, BigDecimal> shop,
                                                     boolean includeShop) {
        Map<String, BigDecimal> merged = new java.util.LinkedHashMap<>();
        addAll(merged, normal);
        if (includeShop) {
            addAll(merged, shop);
        }
        return merged;
    }

    /**
     * Jede benötigte Menge wird mit {@code quantity} multipliziert.
     * Eine leere Liste gilt als vollständig, fehlende Bestände nicht.
     */
    public static boolean costsComplete(Map<String, BigDecimal> needed, Map<String, BigDecimal> owned, int quantity) {
        if (needed == null || needed.isEmpty()) {
            return true;
        }
        int factor = Math.max(1, quantity);
        for (Map.Entry<String, BigDecimal> entry : needed.entrySet()) {
            BigDecimal amount = entry.getValue() == null ? BigDecimal.ZERO : entry.getValue();
            BigDecimal required = amount.multiply(BigDecimal.valueOf(factor));
            BigDecimal have = owned == null ? null : owned.get(entry.getKey());
            if (have == null || have.compareTo(required) < 0) {
                return false;
            }
        }
        return true;
    }

    private static void addAll(Map<String, BigDecimal> target, Map<String, BigDecimal> source) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, BigDecimal> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            target.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
        }
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

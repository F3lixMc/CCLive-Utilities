package net.felix.utilities.DragOverlay;

import net.felix.utilities.Aincraft.MaterialRateUtility;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Berechnet Ressourcen pro Minute aus Farmzone-Actionbar ({@code +ZAHL NAME [ANZAHL]}).
 * Der erste {@code [ANZAHL]}-Wert ist die Baseline; der Timer startet erst bei der ersten Erhöhung.
 */
public final class FarmzoneResourceRateUtility {

    /** Mindestzeit nach erstem Zuwachs, bevor eine Rate angezeigt wird (verhindert Start-Spike). */
    private static final long MIN_RATE_ELAPSED_MS = 2000L;
    private static final Pattern SCOREBOARD_RESOURCE_NAME =
            Pattern.compile("➥\\s*(.+)");
    private static final int RATE_SUFFIX_COLOR = 0xAAAAAA;

    private static String currentResourceName;
    private static long initialCount;
    private static long currentCount;
    private static long sessionStartMs;
    private static double perMinute;

    private FarmzoneResourceRateUtility() {
    }

    public static void resetSession() {
        currentResourceName = null;
        initialCount = 0;
        currentCount = 0;
        sessionStartMs = 0;
        perMinute = 0.0;
    }

    /**
     * Aktualisiert die Rate laufend (wie XP/Min), auch ohne neue Actionbar-Nachricht.
     */
    public static void tick() {
        if (currentResourceName == null || sessionStartMs <= 0) {
            return;
        }
        updateRate(System.currentTimeMillis());
    }

    public static void update(String resourceName, long count) {
        if (resourceName == null || resourceName.isEmpty() || count < 0) {
            return;
        }

        long now = System.currentTimeMillis();
        String normalizedNew = normalizeResourceName(resourceName);

        if (currentResourceName == null || !normalizeResourceName(currentResourceName).equals(normalizedNew)) {
            startSession(resourceName, count);
            return;
        }

        if (count < initialCount) {
            startSession(resourceName, count);
            return;
        }

        currentResourceName = resourceName;

        if (count > currentCount) {
            currentCount = count;
            if (sessionStartMs <= 0 && currentCount > initialCount) {
                sessionStartMs = now;
            }
            updateRate(now);
        }
    }

    public static String getOverlayText() {
        return "Ressource: " + MaterialRateUtility.formatRate(perMinute);
    }

    public static String getCurrentResourceName() {
        return currentResourceName;
    }

    public static double getPerMinute() {
        return perMinute;
    }

    public static boolean isScoreboardResourceLine(String cleanLine) {
        if (cleanLine == null || cleanLine.isEmpty()) {
            return false;
        }
        String trimmed = cleanLine.stripLeading();
        if (!trimmed.startsWith("➥")) {
            return false;
        }
        return !trimmed.matches("➥\\s*\\d+");
    }

    public static boolean matchesTrackedResourceLine(String cleanLine) {
        if (!isScoreboardResourceLine(cleanLine)) {
            return false;
        }
        if (currentResourceName == null) {
            return true;
        }

        String lineName = extractResourceNameFromScoreboardLine(cleanLine);
        if (lineName.isEmpty()) {
            return false;
        }
        return normalizeResourceName(lineName).equals(normalizeResourceName(currentResourceName));
    }

    public static Text appendRateToScoreboardLine(Text lineText) {
        if (lineText == null) {
            return Text.empty();
        }

        MutableText result = lineText.copy();
        result.append(Text.literal(" -> " + MaterialRateUtility.formatRate(perMinute))
                .setStyle(Style.EMPTY.withColor(RATE_SUFFIX_COLOR)));
        return result;
    }

    private static String extractResourceNameFromScoreboardLine(String cleanLine) {
        Matcher matcher = SCOREBOARD_RESOURCE_NAME.matcher(cleanLine.stripLeading());
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).trim();
    }

    private static void startSession(String resourceName, long count) {
        currentResourceName = resourceName;
        initialCount = count;
        currentCount = count;
        sessionStartMs = 0;
        perMinute = 0.0;
    }

    private static void updateRate(long now) {
        if (sessionStartMs <= 0) {
            perMinute = 0.0;
            return;
        }

        long gained = currentCount - initialCount;
        if (gained <= 0) {
            perMinute = 0.0;
            return;
        }

        long elapsed = now - sessionStartMs;
        if (elapsed < MIN_RATE_ELAPSED_MS) {
            perMinute = 0.0;
            return;
        }

        double minutes = elapsed / 60000.0;
        perMinute = gained / minutes;
    }

    private static String normalizeResourceName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        return name.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}

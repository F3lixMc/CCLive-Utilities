package net.felix.utilities.Other.clipboard.overlay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.felix.utilities.Other.clipboard.ClipboardUtility;
import net.felix.utilities.Other.clipboard.collector.ClipboardPaperShredsCollector;
import net.felix.utilities.Other.clipboard.storage.CollectedMaterialsResourcesStorage;
import net.felix.utilities.Overall.itemviewer.data.CostItem;

/**
 * Ungefähre Restzeit für Clipboard-Materialien/Ressourcen.
 * <pre>
 *   Rate (Stück/min) = abgebaute Menge / Minuten seit Farm-Start
 *   Zeit             = noch benötigte Menge / Rate
 * </pre>
 * Der Startbestand wird beim ersten Actionbar-Drop gemerkt. Bei einem echten Materialwechsel
 * (vorheriges Material länger als {@link #ACTIVE_WINDOW_MS} ohne Drop) startet die Session neu.
 * Beim Ebenen-/Dimensionswechsel werden alle Sessions verworfen. Alle gerade aktiven
 * Materialien/Ressourcen zeigen gleichzeitig eine Zeit – ohne Mengen-Cap.
 */
public final class ClipboardTimeEstimate {

    /** Ohne neuen Abbau gilt die Session danach als beendet / nicht mehr angezeigt. */
    private static final long ACTIVE_WINDOW_MS = 60_000L;
    /** Erst nach dieser Messdauer eine Rate anzeigen (verhindert Start-Spitzen). */
    private static final long MIN_ELAPSED_MS = 5_000L;
    private static final long UPDATE_INTERVAL_MS = 1000L;
    private static final String[] FURNACE_RESOURCES = {
            "Eisenbarren", "Goldbarren", "Kupferbarren", "Netherite Schrott"
    };

    private static final Map<String, Session> sessions = new HashMap<>();
    private static final Map<String, CachedEstimate> cache = new HashMap<>();
    /** Zuletzt per Actionbar gemeldetes Material (für Wechsel-Erkennung). */
    private static String lastFarmedKey = null;
    /** Letzte Dimension – bei Wechsel (z. B. Floor) Sessions zurücksetzen. */
    private static String lastDimension = null;

    private ClipboardTimeEstimate() {
    }

    /** Eine Farm-Messung für ein Material. */
    private static final class Session {
        /** Bestand beim Farm-Start (erster Actionbar-Drop). */
        long baselineCount;
        long startMs;
        long lastCount;
        long lastGainMs;

        Session(long count, long nowMs) {
            restart(count, nowMs);
        }

        void restart(long count, long nowMs) {
            this.baselineCount = count;
            this.startMs = nowMs;
            this.lastCount = count;
            this.lastGainMs = nowMs;
        }

        void registerGain(long count, long nowMs) {
            this.lastCount = count;
            this.lastGainMs = nowMs;
        }

        boolean isActive(long nowMs) {
            return nowMs - lastGainMs <= ACTIVE_WINDOW_MS;
        }

        long gained() {
            return Math.max(0L, lastCount - baselineCount);
        }

        /**
         * Rate = abgebaut / vergangene Minuten seit Start (inkl. kurze Pausen bis {@link #ACTIVE_WINDOW_MS}).
         */
        double ratePerMinute(long nowMs) {
            long gained = gained();
            long elapsedMs = nowMs - startMs;
            if (!isActive(nowMs) || gained <= 0 || elapsedMs < MIN_ELAPSED_MS) {
                return 0.0;
            }
            return gained / (elapsedMs / 60000.0);
        }
    }

    private static final class CachedEstimate {
        final long computedAtMs;
        final BigDecimal remaining;
        final boolean showTime;
        final String text;

        CachedEstimate(long computedAtMs, BigDecimal remaining, boolean showTime, String text) {
            this.computedAtMs = computedAtMs;
            this.remaining = remaining;
            this.showTime = showTime;
            this.text = text;
        }
    }

    /**
     * Meldet einen neuen Gesamtbestand aus der Actionbar ({@code +X Name [GESAMT]}).
     * Der erste Aufruf setzt den Startbestand.
     */
    public static synchronized void recordCount(String itemName, long count) {
        if (itemName == null || itemName.isBlank() || count < 0 || !isSupported(itemName)) {
            return;
        }
        checkDimensionChange();
        long now = System.currentTimeMillis();
        String key = CollectedMaterialsResourcesStorage.normalizeName(itemName);
        boolean materialSwitched = isMaterialSwitch(key, now);
        lastFarmedKey = key;

        Session session = sessions.get(key);
        if (session == null) {
            sessions.put(key, new Session(count, now));
            cache.remove(key);
            return;
        }
        if (count == session.lastCount) {
            return;
        }
        // Materialwechsel, Bestand gesunken (abgegeben) oder Pause zu lang: neu starten
        if (materialSwitched || count < session.lastCount || !session.isActive(now)) {
            session.restart(count, now);
            cache.remove(key);
            return;
        }
        session.registerGain(count, now);
        cache.remove(key);
    }

    /**
     * true, wenn zuvor ein anderes Material gefarmt wurde und dieses nicht mehr aktiv ist.
     * Mehrere gleichzeitige Drops (alle innerhalb von {@link #ACTIVE_WINDOW_MS}) zählen nicht
     * als Wechsel.
     */
    private static boolean isMaterialSwitch(String key, long nowMs) {
        if (lastFarmedKey == null || lastFarmedKey.equals(key)) {
            return false;
        }
        Session previous = sessions.get(lastFarmedKey);
        if (previous == null) {
            return true;
        }
        return !previous.isActive(nowMs);
    }

    /** Verwirft alle Messungen (z. B. beim Zurücksetzen des Clipboards oder Ebenenwechsel). */
    public static synchronized void reset() {
        sessions.clear();
        cache.clear();
        lastFarmedKey = null;
    }

    /**
     * Behält nur Sessions für noch benötigte Materialien (nach Bauplan entfernen).
     * Leere Menge → vollständiger Reset.
     */
    public static synchronized void retainOnly(java.util.Set<String> requiredNormalizedNames) {
        if (requiredNormalizedNames == null || requiredNormalizedNames.isEmpty()) {
            reset();
            return;
        }
        sessions.keySet().removeIf(key -> !requiredNormalizedNames.contains(key));
        cache.keySet().removeIf(key -> !requiredNormalizedNames.contains(key));
        if (lastFarmedKey != null && !requiredNormalizedNames.contains(lastFarmedKey)) {
            lastFarmedKey = null;
        }
    }

    private static void checkDimensionChange() {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client == null || client.level == null) {
            return;
        }
        String dimension = client.level.dimension().identifier().toString();
        if (lastDimension != null && !lastDimension.equals(dimension)) {
            sessions.clear();
            cache.clear();
            lastFarmedKey = null;
        }
        lastDimension = dimension;
    }

    /**
     * @param itemName  Name des Materials/der Ressource
     * @param remaining noch fehlende Menge (&lt;= 0 heißt fertig)
     * @return {@code " ≈ /"} sobald die Einstellung aktiv ist (noch nicht fertig),
     *         {@code " ≈ Wird errechnet"} während des Farmens ohne Rate,
     *         {@code " ≈ 3.5 min"} mit Rate, leer wenn fertig
     */
    public static synchronized String buildTimeText(String itemName, BigDecimal remaining) {
        if (itemName == null || itemName.isEmpty() || remaining == null || !isSupported(itemName)) {
            return "";
        }
        // Fertig: keinen Zeit-Suffix anzeigen
        if (remaining.signum() <= 0) {
            return "";
        }

        checkDimensionChange();
        long now = System.currentTimeMillis();
        String key = CollectedMaterialsResourcesStorage.normalizeName(itemName);
        boolean showTime = shouldShowTime(key, now);

        CachedEstimate cached = cache.get(key);
        if (cached != null
                && now - cached.computedAtMs < UPDATE_INTERVAL_MS
                && cached.remaining.compareTo(remaining) == 0
                && cached.showTime == showTime) {
            return cached.text;
        }

        // Standard mit aktivierter Einstellung: Platzhalter, bis eine Rate vorliegt
        String text = " ≈ /";
        if (showTime) {
            Session session = sessions.get(key);
            if (session != null) {
                double rate = session.ratePerMinute(now);
                if (rate > 0.0) {
                    text = " ≈ " + formatDuration(remaining.doubleValue() / rate);
                } else {
                    text = " ≈ Wird errechnet";
                }
            }
        }
        cache.put(key, new CachedEstimate(now, remaining, showTime, text));
        return text;
    }

    /** Jedes Material mit aktivem Abbau (Drop innerhalb von {@link #ACTIVE_WINDOW_MS}) zeigt eine Zeit. */
    private static boolean shouldShowTime(String key, long nowMs) {
        Session mine = sessions.get(key);
        return mine != null && mine.isActive(nowMs);
    }

    private static boolean isSupported(String itemName) {
        String trimmed = itemName.trim();
        return !"Coins".equalsIgnoreCase(trimmed)
                && !CollectedMaterialsResourcesStorage.OTHER_KAKTUS.equalsIgnoreCase(trimmed)
                && !CollectedMaterialsResourcesStorage.OTHER_SEELEN.equalsIgnoreCase(trimmed)
                && !ClipboardPaperShredsCollector.STORAGE_NAME.equalsIgnoreCase(trimmed)
                && !isFurnaceResource(trimmed)
                && !isAmbossCost(trimmed);
    }

    private static boolean isFurnaceResource(String itemName) {
        for (String furnaceResource : FURNACE_RESOURCES) {
            if (furnaceResource.equalsIgnoreCase(itemName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAmbossCost(String itemName) {
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        if (entries == null) {
            return false;
        }
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (entry == null || entry.price == null) {
                continue;
            }
            CostItem amboss = entry.price.Amboss != null ? entry.price.Amboss : entry.price.amboss;
            if (amboss != null && amboss.itemName != null && amboss.itemName.equalsIgnoreCase(itemName)) {
                return true;
            }
        }
        return false;
    }

    private static String formatDuration(double minutes) {
        if (minutes >= 60.0) {
            return BigDecimal.valueOf(minutes / 60.0).setScale(1, RoundingMode.HALF_UP).toPlainString() + " h";
        }
        return BigDecimal.valueOf(minutes).setScale(1, RoundingMode.HALF_UP).toPlainString() + " min";
    }
}

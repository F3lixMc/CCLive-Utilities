package net.felix.Community.leaderboards.collectors;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Absolute Ebenenstände je Konto und Seasonphase. Derselbe Wert wird nicht addiert, ein niedrigerer nicht übernommen.
 */
public final class FloorKillBook {
    private final Map<String, Map<String, Long>> phases = new LinkedHashMap<>();

    public long read(String account, int seasonId, boolean preseason, String floor, long reading) {
        long stored = stored(account, seasonId, preseason, floor);
        if (account == null || account.isBlank() || floor == null || floor.isBlank() || reading <= stored) {
            return stored;
        }
        phase(account, seasonId, preseason).put(floor, reading);
        return reading;
    }

    public long stored(String account, int seasonId, boolean preseason, String floor) {
        if (account == null || floor == null) {
            return 0L;
        }
        return phases.getOrDefault(key(account, seasonId, preseason), Map.of()).getOrDefault(floor, 0L);
    }

    public long sum(String account, int seasonId, boolean preseason) {
        long total = 0L;
        for (long kills : floors(account, seasonId, preseason).values()) {
            total += kills;
        }
        return total;
    }

    public Map<String, Long> floors(String account, int seasonId, boolean preseason) {
        return new LinkedHashMap<>(phases.getOrDefault(key(account, seasonId, preseason), Map.of()));
    }

    public boolean clear(String account, int seasonId, boolean preseason) {
        return phases.remove(key(account, seasonId, preseason)) != null;
    }

    public boolean remove(String account, int seasonId, boolean preseason, String floor) {
        Map<String, Long> floors = phases.get(key(account, seasonId, preseason));
        if (floors == null || floors.remove(floor) == null) {
            return false;
        }
        if (floors.isEmpty()) {
            phases.remove(key(account, seasonId, preseason));
        }
        return true;
    }

    public Map<String, Map<String, Long>> snapshot() {
        Map<String, Map<String, Long>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Long>> entry : phases.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return copy;
    }

    public static FloorKillBook restore(Map<String, Map<String, Long>> snapshot) {
        FloorKillBook book = new FloorKillBook();
        if (snapshot == null) {
            return book;
        }
        for (Map.Entry<String, Map<String, Long>> entry : snapshot.entrySet()) {
            book.phases.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return book;
    }

    private Map<String, Long> phase(String account, int seasonId, boolean preseason) {
        return phases.computeIfAbsent(key(account, seasonId, preseason), key -> new LinkedHashMap<>());
    }

    static String key(String account, int seasonId, boolean preseason) {
        return account + "|" + seasonId + "|" + preseason;
    }
}

package net.felix.Community.profile;

import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Eine laufende Profilanfrage pro Spieler und Season. Ein bestätigter 404 bleibt kurz gespeichert.
 * Netzwerkfehler erzeugen keinen solchen Eintrag.
 */
public final class ProfileFetchCoalescer {
    public static final long MISSING_TTL_MS = 60_000L;

    private final ConcurrentHashMap<Key, Cached> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Key, CompletableFuture<JsonObject>> inflight = new ConcurrentHashMap<>();
    private final long ttlMs;

    public ProfileFetchCoalescer(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    public record Key(String player, String season) {
    }

    public record Result(int status, JsonObject body) {
    }

    @FunctionalInterface
    public interface Fetcher {
        Result fetch(String player) throws Exception;
    }

    public JsonObject available(String player, String season) {
        Cached cached = cache.get(key(player, season));
        if (cached == null || cached.missing) {
            return null;
        }
        return cached.stats;
    }

    public boolean fresh(String player, String season, long now) {
        Cached cached = cache.get(key(player, season));
        return cached != null && now - cached.at < ttlMs;
    }

    CompletableFuture<JsonObject> pendingResult(String player, String season) {
        return inflight.get(key(player, season));
    }

    public boolean pending(String player, String season) {
        return inflight.containsKey(key(player, season));
    }

    /**
     * Startet höchstens einen Abruf. Der Aufrufer wartet nicht auf die Antwort.
     */
    public void request(String player, String season, Fetcher fetcher, long now) {
        if (player == null || player.isBlank() || fetcher == null || fresh(player, season, now)) {
            return;
        }
        Key key = key(player, season);
        CompletableFuture<JsonObject> created = new CompletableFuture<>();
        if (inflight.putIfAbsent(key, created) != null) {
            return;
        }
        Cached previous = cache.get(key);
        CompletableFuture.runAsync(() -> {
            try {
                finish(key, created, previous, fetcher.fetch(player), now);
            } catch (Exception e) {
                finish(key, created, previous, null, now);
            } finally {
                inflight.remove(key, created);
            }
        });
    }

    public JsonObject fetch(String player, String season, Fetcher fetcher, long now) throws InterruptedException {
        if (player == null || player.isBlank() || fetcher == null) {
            return null;
        }
        Key key = key(player, season);
        Cached cached = cache.get(key);
        if (cached != null && now - cached.at < ttlMs) {
            return cached.stats;
        }
        CompletableFuture<JsonObject> created = new CompletableFuture<>();
        CompletableFuture<JsonObject> existing = inflight.putIfAbsent(key, created);
        if (existing != null) {
            return existing.join();
        }
        try {
            Result result = fetcher.fetch(player);
            if (result != null && result.status() == 404) {
                cache.put(key, new Cached(null, true, now));
                created.complete(null);
                return null;
            }
            if (result != null && result.status() == 200 && result.body() != null) {
                cache.put(key, new Cached(result.body(), false, now));
                created.complete(result.body());
                return result.body();
            }
            JsonObject fallback = positive(cached);
            created.complete(fallback);
            return fallback;
        } catch (Exception e) {
            JsonObject fallback = positive(cached);
            created.complete(fallback);
            return fallback;
        } finally {
            inflight.remove(key, created);
        }
    }

    public boolean remembersMissing(String player, String season, long now) {
        Cached cached = cache.get(key(player, season));
        return cached != null && cached.missing && now - cached.at < ttlMs;
    }

    private static Key key(String player, String season) {
        return new Key(
                player == null ? "" : player.toLowerCase(java.util.Locale.ROOT),
                season == null || season.isBlank() ? "current" : season);
    }

    private void finish(Key key, CompletableFuture<JsonObject> created, Cached previous, Result result, long now) {
        if (result != null && result.status() == 404) {
            cacheMiss(key, created, now);
            return;
        }
        if (result != null && result.status() == 200 && result.body() != null) {
            cacheHit(key, created, result.body(), now);
            return;
        }
        created.complete(positive(previous));
    }

    private void cacheMiss(Key key, CompletableFuture<JsonObject> created, long now) {
        cache.put(key, new Cached(null, true, now));
        created.complete(null);
    }

    private void cacheHit(Key key, CompletableFuture<JsonObject> created, JsonObject body, long now) {
        cache.put(key, new Cached(body, false, now));
        created.complete(body);
    }

    private static JsonObject positive(Cached cached) {
        return cached != null && !cached.missing ? cached.stats : null;
    }

    private static final class Cached {
        private final JsonObject stats;
        private final boolean missing;
        private final long at;

        private Cached(JsonObject stats, boolean missing, long at) {
            this.stats = stats;
            this.missing = missing;
            this.at = at;
        }
    }
}

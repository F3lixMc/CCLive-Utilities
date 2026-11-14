package net.felix.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.http.HttpClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Öffentliche Domainfreigabe aus {@code GET /chat/rules}. Eine fehlgeschlagene Abfrage behält die letzte Liste.
 */
public final class ChatAllowedDomains {
    private static final long REFRESH_MS = 30_000L;
    private static final Object LOCK = new Object();
    private static volatile List<String> domains = List.of();
    private static volatile long loadedAt;

    private ChatAllowedDomains() {
    }

    public static List<String> current() {
        long now = System.currentTimeMillis();
        if (loadedAt > 0L && now - loadedAt < REFRESH_MS) {
            return domains;
        }
        synchronized (LOCK) {
            now = System.currentTimeMillis();
            if (loadedAt > 0L && now - loadedAt < REFRESH_MS) {
                return domains;
            }
            List<String> fresh = fetch();
            if (fresh != null) {
                domains = List.copyOf(fresh);
                loadedAt = now;
            }
            return domains;
        }
    }

    private static List<String> fetch() {
        try {
            LeaderboardManager manager = LeaderboardManager.getInstance();
            HttpClient http = manager == null ? null : manager.getHttpClient();
            String token = manager == null ? null : manager.getPlayerToken();
            if (http == null || token == null || token.isBlank()) {
                return null;
            }
            HttpClient.JsonResponse response = http.getResultWithToken("/chat/rules", token);
            if (response.status() != 200 || response.body() == null || !response.body().has("domains")) {
                return null;
            }
            JsonElement rows = response.body().get("domains");
            if (!rows.isJsonArray()) {
                return null;
            }
            List<String> parsed = new ArrayList<>();
            for (JsonElement element : (JsonArray) rows) {
                if (element.isJsonPrimitive()) {
                    String host = element.getAsString();
                    if (host != null && !host.isBlank()) {
                        parsed.add(host.trim().toLowerCase());
                    }
                }
            }
            return parsed;
        } catch (Exception e) {
            return null;
        }
    }
}

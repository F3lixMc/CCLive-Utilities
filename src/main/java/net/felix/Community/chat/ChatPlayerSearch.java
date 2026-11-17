package net.felix.Community.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.felix.Community.leaderboards.LeaderboardManager;
import net.felix.Community.leaderboards.http.HttpClient;
import net.minecraft.client.Minecraft;

/**
 * Spielersuche für normale Mod-Nutzer. Der Admin-Endpunkt bleibt ungenutzt.
 * Dieselbe Abfrage läuft über {@code /chat/players/search} mit dem Spieler-Token.
 */
public final class ChatPlayerSearch {
    public static final String UNREACHABLE = "Die Spielersuche ist gerade nicht erreichbar.";
    static final long DELAY_MS = 200L;
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static volatile Long knownSelfId;

    public record Result(String query, List<ChatIgnoreRules.Match> matches, Long selfId, String error) {
        public static Result empty(String query) {
            return new Result(query == null ? "" : query, List.of(), knownSelfId, null);
        }
    }

    private ChatPlayerSearch() {
    }

    static boolean isCurrent(long seq, String query, long currentSeq, String currentText) {
        String typed = currentText == null ? "" : currentText.trim();
        return seq == currentSeq && query != null && query.equals(typed);
    }

    public static Long selfId() {
        return knownSelfId;
    }

    public static void cancel() {
        SEQUENCE.incrementAndGet();
    }

    public static void search(String query, Consumer<Result> callback) {
        long seq = SEQUENCE.incrementAndGet();
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < 2) {
            deliver(seq, Result.empty(trimmed), callback);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (seq != SEQUENCE.get()) {
                return;
            }
            deliver(seq, load(trimmed), callback);
        });
    }

    private static void deliver(long seq, Result result, Consumer<Result> callback) {
        if (callback == null || seq != SEQUENCE.get()) {
            return;
        }
        if (result.selfId != null) {
            knownSelfId = result.selfId;
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> {
                if (seq == SEQUENCE.get()) {
                    callback.accept(result);
                }
            });
        } else {
            callback.accept(result);
        }
    }

    private static Result load(String query) {
        LeaderboardManager manager = LeaderboardManager.getInstance();
        HttpClient http = manager == null ? null : manager.getHttpClient();
        String token = manager == null ? null : manager.getPlayerToken();
        if (http == null || token == null || token.isBlank()) {
            return new Result(query, List.of(), knownSelfId, UNREACHABLE);
        }
        try {
            String path = "/chat/players/search?name=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpClient.JsonResponse response = http.getResultWithToken(path, token);
            if (response.status() != 200 || response.body() == null) {
                return new Result(query, List.of(), knownSelfId, UNREACHABLE);
            }
            JsonObject body = response.body();
            Long reportedSelf = knownSelfId;
            if (body.has("self_id") && body.get("self_id").isJsonPrimitive()) {
                reportedSelf = body.get("self_id").getAsLong();
            }
            List<ChatIgnoreRules.Match> matches = new ArrayList<>();
            JsonArray rows = body.has("players") && body.get("players").isJsonArray()
                ? body.getAsJsonArray("players") : new JsonArray();
            for (JsonElement element : rows) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject row = element.getAsJsonObject();
                if (!row.has("player") || !row.has("player_id")) {
                    continue;
                }
                boolean self = row.has("self") && row.get("self").isJsonPrimitive() && row.get("self").getAsBoolean();
                matches.add(new ChatIgnoreRules.Match(
                    row.get("player").getAsString(),
                    row.get("player_id").getAsLong(),
                    self));
            }
            return new Result(query, matches, reportedSelf, null);
        } catch (Exception e) {
            return new Result(query, List.of(), knownSelfId, UNREACHABLE);
        }
    }
}

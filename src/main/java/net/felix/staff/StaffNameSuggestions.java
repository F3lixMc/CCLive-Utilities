package net.felix.staff;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.felix.leaderboards.http.HttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public final class StaffNameSuggestions {
    private static final long DEBOUNCE_MS = 250L;
    private static final long CACHE_MS = 30_000L;
    private static final AtomicLong searchSeq = new AtomicLong();
    private static volatile long lastRequestAt;
    private static volatile String lastQuery = "";
    private static volatile List<String> lastNames = List.of();
    private static volatile List<String> lastMods = List.of();
    private static volatile long lastModsAt;
    private static final long SANCTION_CACHE_MS = 1_500L;
    private static final AtomicLong sanctionGeneration = new AtomicLong();
    private static final AtomicLong muteSeq = new AtomicLong();
    private static final AtomicLong banSeq = new AtomicLong();
    private static volatile List<String> muteNames = List.of();
    private static volatile List<String> banNames = List.of();
    private static volatile long muteAt;
    private static volatile long banAt;

    private StaffNameSuggestions() {
    }

    public static SuggestionProvider<FabricClientCommandSource> verifiedPlayers() {
        return (context, builder) -> suggest(builder.getRemaining(), false, builder);
    }

    public static SuggestionProvider<FabricClientCommandSource> currentModerators() {
        return (context, builder) -> suggest(builder.getRemaining(), true, builder);
    }

    public static SuggestionProvider<FabricClientCommandSource> activeSanctions(String kind) {
        return (context, builder) -> suggestActive(builder.getRemaining(), kind, builder);
    }

    public static void invalidateSanctions() {
        sanctionGeneration.incrementAndGet();
        muteNames = List.of();
        banNames = List.of();
        muteAt = 0L;
        banAt = 0L;
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggest(
            String remaining,
            boolean moderatorsOnly,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder
    ) {
        long seq = searchSeq.incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(DEBOUNCE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return builder.build();
            }
            if (seq != searchSeq.get()) {
                return builder.build();
            }
            List<String> names = moderatorsOnly ? loadModerators() : loadVerified(remaining);
            if (seq != searchSeq.get()) {
                return builder.build();
            }
            String prefix = remaining == null ? "" : remaining.toLowerCase(Locale.ROOT);
            for (String name : names) {
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    builder.suggest(name);
                }
            }
            return builder.build();
        });
    }

    private static List<String> loadVerified(String remaining) {
        long now = System.currentTimeMillis();
        String query = remaining == null ? "" : remaining.trim();
        if (query.length() < 2) return List.of();
        if (query.equalsIgnoreCase(lastQuery) && now - lastRequestAt < CACHE_MS) {
            return lastNames;
        }
        lastRequestAt = now;
        HttpClient http = StaffClientAccess.http();
        String token = StaffClientAccess.token();
        if (http == null || token == null || token.isBlank()) return lastNames;
        try {
            HttpClient.JsonResponse response = http.getResultWithToken(
                    "/moderation/players/search?name=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8),
                    token
            );
            if (response.status() != 200 || response.body() == null) return lastNames;
            JsonArray rows = response.body().getAsJsonArray("players");
            List<String> names = new ArrayList<>();
            if (rows != null) {
                for (JsonElement element : rows) {
                    if (!element.isJsonObject()) continue;
                    JsonObject row = element.getAsJsonObject();
                    if (row.has("player")) names.add(row.get("player").getAsString());
                }
            }
            lastQuery = query;
            lastNames = names;
            return names;
        } catch (Exception e) {
            return lastNames;
        }
    }

    private static List<String> loadModerators() {
        long now = System.currentTimeMillis();
        if (now - lastModsAt < CACHE_MS) return lastMods;
        HttpClient http = StaffClientAccess.http();
        String token = StaffClientAccess.token();
        if (http == null || token == null || token.isBlank()) return lastMods;
        try {
            HttpClient.JsonResponse response = http.getResultWithToken("/moderation/staff/moderators", token);
            if (response.status() != 200 || response.body() == null) return lastMods;
            JsonArray rows = response.body().getAsJsonArray("moderators");
            List<String> names = new ArrayList<>();
            if (rows != null) {
                for (JsonElement element : rows) {
                    if (!element.isJsonObject()) continue;
                    JsonObject row = element.getAsJsonObject();
                    if (row.has("player")) names.add(row.get("player").getAsString());
                }
            }
            lastMods = names;
            lastModsAt = now;
            return names;
        } catch (Exception e) {
            return lastMods;
        }
    }

    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestActive(
            String remaining,
            String kind,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder
    ) {
        AtomicLong seq = "ban".equals(kind) ? banSeq : muteSeq;
        long token = seq.incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(DEBOUNCE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return builder.build();
            }
            if (token != seq.get()) {
                return builder.build();
            }
            List<String> names = loadActive(kind);
            if (token != seq.get()) {
                return builder.build();
            }
            String prefix = remaining == null ? "" : remaining.toLowerCase(Locale.ROOT);
            for (String name : names) {
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    builder.suggest(name);
                }
            }
            return builder.build();
        });
    }

    private static List<String> loadActive(String kind) {
        long generation = sanctionGeneration.get();
        long now = System.currentTimeMillis();
        boolean ban = "ban".equals(kind);
        long cachedAt = ban ? banAt : muteAt;
        List<String> cached = ban ? banNames : muteNames;
        if (cachedAt != 0L && now - cachedAt < SANCTION_CACHE_MS && generation == sanctionGeneration.get()) {
            return cached;
        }
        HttpClient http = StaffClientAccess.http();
        String token = StaffClientAccess.token();
        if (http == null || token == null || token.isBlank()) return List.of();
        try {
            HttpClient.JsonResponse response = http.getResultWithToken(
                "/moderation/actions/active?kind=" + (ban ? "ban" : "mute"),
                token
            );
            if (generation != sanctionGeneration.get()) return List.of();
            if (response.status() != 200 || response.body() == null) return List.of();
            JsonArray rows = response.body().getAsJsonArray("sanctions");
            List<String> names = new ArrayList<>();
            if (rows != null) {
                for (JsonElement element : rows) {
                    if (!element.isJsonObject()) continue;
                    JsonObject row = element.getAsJsonObject();
                    if (!row.has("player") || row.get("player").isJsonNull()) continue;
                    String name = row.get("player").getAsString();
                    if (name == null || name.isBlank() || names.contains(name)) continue;
                    names.add(name);
                }
            }
            if (generation != sanctionGeneration.get()) return List.of();
            if (ban) {
                banNames = names;
                banAt = System.currentTimeMillis();
            } else {
                muteNames = names;
                muteAt = System.currentTimeMillis();
            }
            return names;
        } catch (Exception e) {
            return List.of();
        }
    }
}

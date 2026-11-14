package net.felix.leaderboards;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.felix.leaderboards.http.HttpClient;
import net.felix.leaderboards.panel.LeaderboardPlaces;
import net.felix.leaderboards.panel.SmithSkill;
import net.felix.utilities.Other.DebugUtility;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.RejectedExecutionException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Liest die erreichbaren Ranglisten im Servertakt.
 * Eine Runde ist eine HTTP-Anfrage. Ein Thread, ein Timer. Die GUI löst keinen Abruf aus.
 */
public final class LeaderboardSync {
    private static final long RETRY_INITIAL_MS = 3_000L;
    private static final long RETRY_MAX_MS = 300_000L;

    private static final LeaderboardSync INSTANCE = new LeaderboardSync();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "CCLive-LeaderboardSync");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong epoch = new AtomicLong();
    private volatile ScheduledFuture<?> pending;
    private volatile LeaderboardSnapshot snapshot = LeaderboardSnapshot.EMPTY;
    private volatile SyncClock syncClock;
    private long retryDelayMs = RETRY_INITIAL_MS;

    private LeaderboardSync() {
    }

    public static LeaderboardSync getInstance() {
        return INSTANCE;
    }

    public LeaderboardSnapshot snapshot() {
        return snapshot;
    }

    public SyncClock syncClock() {
        return syncClock;
    }

    public void onJoin() {
        long current = epoch.incrementAndGet();
        submit(() -> {
            if (current != epoch.get()) {
                return;
            }
            retryDelayMs = RETRY_INITIAL_MS;
            cancelPending();
            runRound(current, 0L);
        });
    }

    public void onDisconnect() {
        epoch.incrementAndGet();
        syncClock = null;
        submit(this::cancelPending);
    }

    public void shutdown() {
        epoch.incrementAndGet();
        cancelPending();
        scheduler.shutdownNow();
    }

    private void runRound(long roundEpoch, long scheduledAt) {
        if (roundEpoch != epoch.get()) {
            return;
        }
        Fetch fetch = loadRound(roundEpoch, scheduledAt);
        if (roundEpoch != epoch.get()) {
            return;
        }
        if (fetch.loaded() == null) {
            long delay = retryDelayMs;
            retryDelayMs = Math.min(RETRY_MAX_MS, Math.max(RETRY_INITIAL_MS, delay * 2));
            report(failureText(fetch.failure() == null ? "Unbekannter Fehler" : fetch.failure(), delay));
            schedule(delay, roundEpoch, scheduledAt);
            return;
        }
        RoundLoaded loaded = fetch.loaded();
        snapshot = applyRound(snapshot, loaded);
        syncClock = new SyncClock(loaded.receivedAt(), loaded.serverTime(), loaded.nextSyncAt());
        retryDelayMs = RETRY_INITIAL_MS;
        long jitter = ThreadLocalRandom.current().nextInt(5_001);
        long delay = LeaderboardSyncSchedule.delayUntilNext(
            loaded.receivedAt, loaded.serverTime, loaded.nextSyncAt, jitter, System.currentTimeMillis());
        report(successText(
            loaded.seasonId,
            loaded.preseason,
            boardCount(loaded.snapshot, LeaderboardPlaces.syncBoardIds()),
            entryCount(loaded.snapshot, LeaderboardPlaces.syncBoardIds()),
            System.currentTimeMillis() + delay,
            ZoneId.systemDefault()));
        schedule(delay, roundEpoch, loaded.nextSyncAt);
    }

    private Fetch loadRound(long roundEpoch, long scheduledAt) {
        LeaderboardManager manager = LeaderboardManager.getInstance();
        if (roundEpoch != epoch.get()) {
            return Fetch.fail("Abgebrochen");
        }
        if (manager == null || !manager.isRegistered()) {
            return Fetch.fail("Spieler noch nicht registriert");
        }
        String playerName = manager.getPlayerName();
        HttpClient http = manager.getHttpClient();
        if (http == null) {
            return Fetch.fail("Kein HTTP-Client");
        }
        Fetch fetch = fetchRound(path -> {
            String requestPath = scheduledAt > 0 ? path + "?scheduled_at=" + scheduledAt : path;
            HttpClient.JsonResponse response = http.getResult(requestPath);
            return new SyncResponse(response.status(), response.body());
        }, playerName, LeaderboardPlaces.syncBoardIds());
        if (roundEpoch != epoch.get()) {
            return Fetch.fail("Abgebrochen");
        }
        return fetch;
    }

    /** Eine Anfrage. Ungültige oder unvollständige Antworten behalten den alten Stand. */
    static Fetch fetchRound(SyncClient client, String playerName, List<String> boardIds) {
        if (client == null || boardIds == null || boardIds.isEmpty()) {
            return Fetch.fail("Ungültige Antwort");
        }
        if (playerName == null || playerName.isBlank()) {
            return Fetch.fail("Kein Spielername");
        }
        SyncResponse response;
        try {
            response = client.get(syncPath(playerName));
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Fetch.fail(connectionReason(e));
        }
        if (response == null) {
            return Fetch.fail("Keine Antwort");
        }
        if (response.status != 200) {
            return Fetch.fail(response.status <= 0 ? "Keine Antwort" : "HTTP " + response.status);
        }
        String problem = explain(response.body, boardIds);
        if (problem != null) {
            return Fetch.fail(problem);
        }
        long receivedAt = System.currentTimeMillis();
        LeaderboardSnapshot parsed = readSnapshot(response.body, boardIds);
        if (parsed == null) {
            return Fetch.fail("Ungültige Antwort");
        }
        try {
            return Fetch.ok(new RoundLoaded(
                parsed,
                receivedAt,
                response.body.get("serverTime").getAsLong(),
                response.body.get("nextSyncAt").getAsLong(),
                response.body.get("seasonId").getAsInt(),
                readPreseason(response.body)));
        } catch (RuntimeException e) {
            return Fetch.fail("Ungültige Antwort");
        }
    }

    static String connectionReason(Exception error) {
        String message = error == null ? null : error.getMessage();
        if (message == null || message.isBlank()) {
            return "Verbindungsfehler";
        }
        String compact = message.replace('\n', ' ').trim();
        if (compact.length() > 120) {
            compact = compact.substring(0, 120);
        }
        return "Verbindungsfehler: " + compact;
    }

    static String explain(JsonObject json, List<String> boardIds) {
        if (json == null
            || !json.has("seasonId") || json.get("seasonId").isJsonNull()
            || !json.has("isPreseason") || json.get("isPreseason").isJsonNull()
            || !json.has("serverTime") || json.get("serverTime").isJsonNull()
            || !json.has("nextSyncAt") || json.get("nextSyncAt").isJsonNull()
            || !json.has("boards") || !json.get("boards").isJsonObject()) {
            return "Ungültige Antwort";
        }
        JsonObject boards = json.getAsJsonObject("boards");
        for (String boardId : boardIds) {
            if (boardId == null || !boards.has(boardId) || !boards.get(boardId).isJsonObject()) {
                return "Fehlendes Board " + boardId;
            }
            JsonObject board = boards.getAsJsonObject(boardId);
            if (!board.has("top") || !board.get("top").isJsonArray()) {
                return "Ungültiges Board " + boardId;
            }
        }
        return null;
    }

    static String successText(int seasonId, boolean preseason, int boards, int entries, long nextAttemptMillis, ZoneId zone) {
        String when = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(zone == null ? ZoneId.systemDefault() : zone)
            .format(Instant.ofEpochMilli(nextAttemptMillis));
        return "Sync erfolgreich: " + SeasonChangeGate.resetLabel(seasonId, preseason) + ", "
            + boards + (boards == 1 ? " Board, " : " Boards, ")
            + entries + (entries == 1 ? " Eintrag, " : " Einträge, ")
            + "nächster Sync " + when;
    }

    static String failureText(String reason, long delayMs) {
        return "Sync fehlgeschlagen: " + reason + ". Nächster Versuch in " + waitText(delayMs);
    }

    static String waitText(long delayMs) {
        long delay = Math.max(0L, delayMs);
        if (delay < 1_000L) {
            return delay + " ms";
        }
        long seconds = delay / 1_000L;
        if (seconds < 60) {
            return seconds + " s";
        }
        long minutes = seconds / 60;
        long rest = seconds % 60;
        if (rest == 0) {
            return minutes + " min";
        }
        return minutes + " min " + rest + " s";
    }

    static int boardCount(LeaderboardSnapshot snapshot, List<String> boardIds) {
        int count = 0;
        for (String boardId : boardIds) {
            if (snapshot != null && snapshot.board(boardId) != null) {
                count++;
            }
        }
        return count;
    }

    static int entryCount(LeaderboardSnapshot snapshot, List<String> boardIds) {
        int count = 0;
        for (String boardId : boardIds) {
            LeaderboardSnapshot.Board board = snapshot == null ? null : snapshot.board(boardId);
            if (board != null) {
                count += board.rows().size();
            }
        }
        return count;
    }

    private static void report(String line) {
        try {
            DebugUtility.debugLeaderboardSync(line);
        } catch (Throwable ignored) {
        }
    }

    static String syncPath(String playerName) {
        return "/leaderboard/sync/" + URLEncoder.encode(playerName, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static LeaderboardSnapshot readSnapshot(JsonObject json, List<String> boardIds) {
        if (!usable(json)) {
            return null;
        }
        JsonObject rawBoards = json.getAsJsonObject("boards");
        Map<String, LeaderboardSnapshot.Board> boards = new LinkedHashMap<>();
        try {
            for (String boardId : boardIds) {
                if (boardId == null || !rawBoards.has(boardId) || !rawBoards.get(boardId).isJsonObject()) {
                    return null;
                }
                boards.put(boardId, parseBoard(rawBoards.getAsJsonObject(boardId)));
            }
        } catch (RuntimeException e) {
            return null;
        }
        return new LeaderboardSnapshot(boards);
    }

    static LeaderboardSnapshot applyRound(LeaderboardSnapshot current, RoundLoaded loaded) {
        if (loaded == null) {
            return current;
        }
        return LeaderboardSyncSchedule.keepUntilComplete(current, loaded.snapshot, true);
    }

    private static boolean usable(JsonObject json) {
        return json != null
            && json.has("seasonId") && !json.get("seasonId").isJsonNull()
            && json.has("isPreseason") && !json.get("isPreseason").isJsonNull()
            && json.has("serverTime") && !json.get("serverTime").isJsonNull()
            && json.has("nextSyncAt") && !json.get("nextSyncAt").isJsonNull()
            && json.has("boards") && json.get("boards").isJsonObject();
    }

    @FunctionalInterface
    interface SyncClient {
        SyncResponse get(String path) throws Exception;
    }

    record SyncResponse(int status, JsonObject body) {
    }

    record Fetch(RoundLoaded loaded, String failure) {
        static Fetch ok(RoundLoaded loaded) {
            return new Fetch(loaded, null);
        }

        static Fetch fail(String reason) {
            return new Fetch(null, reason);
        }
    }

    private static LeaderboardSnapshot.Board parseBoard(JsonObject json) {
        JsonArray top = json.getAsJsonArray("top");
        List<LeaderboardSnapshot.Row> rows = new java.util.ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            if (!top.get(i).isJsonObject()) {
                continue;
            }
            JsonObject row = top.get(i).getAsJsonObject();
            String name = text(row, "player");
            if (name == null) {
                continue;
            }
            int rank = row.has("rank") && !row.get("rank").isJsonNull()
                ? row.get("rank").getAsInt()
                : rows.size() + 1;
            LeaderboardSnapshot.Row parsed = readRow(row, rank);
            if (parsed != null) {
                rows.add(parsed);
            }
        }
        return new LeaderboardSnapshot.Board(rows, parseSelf(json));
    }

    private static LeaderboardSnapshot.Row parseSelf(JsonObject json) {
        if (!json.has("self") || json.get("self").isJsonNull()) {
            return null;
        }
        if (!json.get("self").isJsonObject()) {
            throw new IllegalArgumentException("self");
        }
        JsonObject raw = json.getAsJsonObject("self");
        String name = text(raw, "player");
        if (name == null || !raw.has("rank") || raw.get("rank").isJsonNull()) {
            throw new IllegalArgumentException("self");
        }
        int rank = raw.get("rank").getAsInt();
        if (rank <= 0) {
            throw new IllegalArgumentException("self");
        }
        LeaderboardSnapshot.Row parsed = readRow(raw, rank);
        if (parsed == null) {
            throw new IllegalArgumentException("self");
        }
        return parsed;
    }

    private static LeaderboardSnapshot.Row readRow(JsonObject row, int rank) {
        String name = text(row, "player");
        if (name == null) {
            return null;
        }
        if (row.has("level") || row.has("xp")) {
            Integer level = boundedInt(text(row, "level"), SmithSkill.MAX_LEVEL);
            Long xp = SmithSkill.xpWithinLong(text(row, "xp"));
            if (level == null || xp == null) {
                return null;
            }
            return new LeaderboardSnapshot.Row(rank, name, 0L, level, xp);
        }
        long score = row.has("score") && !row.get("score").isJsonNull() ? row.get("score").getAsLong() : 0L;
        return new LeaderboardSnapshot.Row(rank, name, score);
    }

    private static Integer boundedInt(String text, int max) {
        Long value = SmithSkill.xpWithinLong(text);
        if (value == null || value > max) {
            return null;
        }
        return value.intValue();
    }

    private static String text(JsonObject object, String field) {
        if (!object.has(field) || object.get(field).isJsonNull()) {
            return null;
        }
        String value = object.get(field).getAsString().trim();
        return value.isEmpty() ? null : value;
    }

    private void submit(Runnable task) {
        try {
            scheduler.execute(task);
        } catch (RejectedExecutionException ignored) {
        }
    }

    private void schedule(long delayMs, long roundEpoch, long scheduledAt) {
        cancelPending();
        try {
            pending = scheduler.schedule(() -> {
                if (roundEpoch != epoch.get()) {
                    return;
                }
                runRound(roundEpoch, scheduledAt);
            }, Math.max(0L, delayMs), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ignored) {
            pending = null;
        }
    }

    private void cancelPending() {
        ScheduledFuture<?> current = pending;
        pending = null;
        if (current != null) {
            current.cancel(false);
        }
    }

    public record SyncClock(long receivedAt, long serverTime, long nextSyncAt) {
    }

    record RoundLoaded(
        LeaderboardSnapshot snapshot,
        long receivedAt,
        long serverTime,
        long nextSyncAt,
        int seasonId,
        boolean preseason
    ) {
    }

    private static boolean readPreseason(JsonObject json) {
        com.google.gson.JsonElement pre = json.get("isPreseason");
        if (pre.isJsonPrimitive() && pre.getAsJsonPrimitive().isBoolean()) {
            return pre.getAsBoolean();
        }
        return pre.getAsInt() != 0;
    }
}

package net.felix.leaderboards;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.felix.leaderboards.panel.LeaderboardPlaces;
import net.felix.leaderboards.panel.SmithSkill;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardSyncRoundTest {
    @Test
    void oneRequestAdoptsEmptyBoardsAndKeepsTieRank() {
        List<String> ids = LeaderboardPlaces.syncBoardIds();
        AtomicInteger calls = new AtomicInteger();
        LeaderboardSync.Fetch loaded = LeaderboardSync.fetchRound(path -> {
            calls.incrementAndGet();
            assertEquals(LeaderboardSync.syncPath("Ada"), path);
            return new LeaderboardSync.SyncResponse(200, payload(ids, false));
        }, "Ada", ids);

        assertEquals(1, calls.get());
        LeaderboardSnapshot.Board playtime = loaded.loaded().snapshot().board("playtime");
        assertEquals(2, playtime.self().rank());
        assertEquals("Bea", playtime.self().name());
        assertEquals(10L, playtime.self().score());
        assertEquals(playtime.rows().get(1).rank(), playtime.self().rank());

        LeaderboardSnapshot.Board empty = loaded.loaded().snapshot().board("floor_100");
        assertTrue(empty.rows().isEmpty());
        assertNull(empty.self());
        assertEquals(126, ids.size());
    }

    @Test
    void incompleteOrFailedResponseKeepsThePreviousSnapshot() {
        List<String> ids = LeaderboardPlaces.syncBoardIds();
        AtomicInteger calls = new AtomicInteger();
        LeaderboardSnapshot previous = new LeaderboardSnapshot(java.util.Map.of(
            "playtime", new LeaderboardSnapshot.Board(
                List.of(new LeaderboardSnapshot.Row(1, "Ada", 4L)), null)
        ));

        LeaderboardSync.Fetch missing = LeaderboardSync.fetchRound(path -> {
            calls.incrementAndGet();
            return new LeaderboardSync.SyncResponse(200, payload(ids, true));
        }, "Ada", ids);
        assertNull(missing.loaded());
        assertEquals("Fehlendes Board floor_100", missing.failure());
        assertSame(previous, LeaderboardSync.applyRound(previous, missing.loaded()));

        LeaderboardSync.Fetch failed = LeaderboardSync.fetchRound(path -> {
            calls.incrementAndGet();
            throw new IOException("down");
        }, "Ada", ids);
        assertNull(failed.loaded());
        assertEquals("Verbindungsfehler: down", failed.failure());
        assertSame(previous, LeaderboardSync.applyRound(previous, failed.loaded()));
        assertEquals(2, calls.get());

        LeaderboardSync.Fetch unnamed = LeaderboardSync.fetchRound(path -> {
            calls.incrementAndGet();
            return new LeaderboardSync.SyncResponse(200, payload(ids, false));
        }, " ", ids);
        assertNull(unnamed.loaded());
        assertEquals("Kein Spielername", unnamed.failure());
        assertEquals(2, calls.get());

        LeaderboardSync.Fetch http = LeaderboardSync.fetchRound(path -> {
            calls.incrementAndGet();
            return new LeaderboardSync.SyncResponse(503, null);
        }, "Ada", ids);
        assertEquals("HTTP 503", http.failure());
        assertEquals(
            "Sync fehlgeschlagen: HTTP 503. Nächster Versuch in 6 s",
            LeaderboardSync.failureText(http.failure(), 6_000L));
        assertEquals(
            "Sync erfolgreich: Preseason 8, 124 Boards, 2 Einträge, nächster Sync 12:30:02",
            LeaderboardSync.successText(9, true, 124, 2, Instant.parse("2026-11-10T12:30:02Z").toEpochMilli(), ZoneOffset.UTC));
        assertEquals(
            "Sync erfolgreich: Season 8, 124 Boards, 2 Einträge, nächster Sync 12:30:02",
            LeaderboardSync.successText(9, false, 124, 2, Instant.parse("2026-11-10T12:30:02Z").toEpochMilli(), ZoneOffset.UTC));
    }

    @Test
    void smithSkillKeepsLevelTenAndOneXpThroughTheSync() {
        List<String> ids = LeaderboardPlaces.syncBoardIds();
        assertTrue(ids.contains("smith_skill"));
        assertEquals(126, ids.size());
        JsonObject json = payload(ids, false);
        JsonObject smith = json.getAsJsonObject("boards").getAsJsonObject("smith_skill");
        JsonArray top = new JsonArray();
        top.add(smithRow("High", 1, "10", "1"));
        smith.add("top", top);
        smith.add("self", smithRow("High", 1, "10", "1"));

        LeaderboardSync.Fetch loaded = LeaderboardSync.fetchRound(
            path -> new LeaderboardSync.SyncResponse(200, json), "High", ids);
        LeaderboardSnapshot.Row row = loaded.loaded().snapshot().board("smith_skill").self();
        assertEquals(Integer.valueOf(10), row.smithLevel());
        assertEquals(Long.valueOf(1L), row.smithXp());
        assertEquals("10/1", SmithSkill.display(row.smithLevel(), row.smithXp()));
        assertEquals(row.smithLevel(), loaded.loaded().snapshot().board("smith_skill").rows().get(0).smithLevel());
        assertEquals(row.smithXp(), loaded.loaded().snapshot().board("smith_skill").rows().get(0).smithXp());
    }

    private static JsonObject payload(List<String> ids, boolean dropLastFloor) {
        JsonObject boards = new JsonObject();
        for (String id : ids) {
            if (dropLastFloor && "floor_100".equals(id)) {
                continue;
            }
            JsonObject board = new JsonObject();
            board.add("top", new JsonArray());
            board.add("self", JsonNull.INSTANCE);
            boards.add(id, board);
        }
        JsonObject playtime = boards.getAsJsonObject("playtime");
        JsonArray top = new JsonArray();
        top.add(row("Ada", 1, 10));
        top.add(row("Bea", 2, 10));
        playtime.add("top", top);
        playtime.add("self", row("Bea", 2, 10));

        JsonObject json = new JsonObject();
        json.addProperty("seasonId", 9);
        json.addProperty("isPreseason", true);
        json.addProperty("serverTime", 1_000);
        json.addProperty("nextSyncAt", 2_000);
        json.add("boards", boards);
        return json;
    }

    private static JsonObject row(String name, int rank, long score) {
        JsonObject row = new JsonObject();
        row.addProperty("player", name);
        row.addProperty("rank", rank);
        row.addProperty("score", score);
        return row;
    }

    private static JsonObject smithRow(String name, int rank, String level, String xp) {
        JsonObject row = new JsonObject();
        row.addProperty("player", name);
        row.addProperty("rank", rank);
        row.addProperty("level", level);
        row.addProperty("xp", xp);
        return row;
    }
}

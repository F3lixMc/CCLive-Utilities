package net.felix.leaderboards;

import net.felix.leaderboards.panel.LeaderboardPlaces;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardSyncScheduleTest {
    @Test
    void roundThatCrossesTheBoundaryKeepsTheFirstDeadline() {
        long serverTime = Instant.parse("2026-11-10T12:29:59.000Z").toEpochMilli();
        long nextSyncAt = Instant.parse("2026-11-10T12:30:00.000Z").toEpochMilli();
        long receivedAt = serverTime;
        long jitter = 2_000L;
        long finishedAt = Instant.parse("2026-11-10T12:30:01.000Z").toEpochMilli();

        long delay = LeaderboardSyncSchedule.delayUntilNext(
            receivedAt, serverTime, nextSyncAt, jitter, finishedAt);

        assertEquals(1_000L, delay);
        long later = Instant.parse("2026-11-10T12:30:05.000Z").toEpochMilli();
        assertEquals(jitter, LeaderboardSyncSchedule.delayUntilNext(
            receivedAt, serverTime, nextSyncAt, jitter, later));
    }

    @Test
    void quarterPastWaitsUntilHalfHour() {
        long serverTime = Instant.parse("2026-11-10T12:15:00.000Z").toEpochMilli();
        long nextSyncAt = Instant.parse("2026-11-10T12:30:00.000Z").toEpochMilli();
        assertEquals(15 * 60_000L, LeaderboardSyncSchedule.delayUntilNext(
            serverTime, serverTime, nextSyncAt, 0L, serverTime));
    }

    @Test
    void failedRoundKeepsThePreviousSnapshot() {
        LeaderboardSnapshot previous = new LeaderboardSnapshot(java.util.Map.of(
            "floor_7", new LeaderboardSnapshot.Board(List.of(new LeaderboardSnapshot.Row(1, "Ada", 10L)), null)
        ));
        LeaderboardSnapshot incoming = new LeaderboardSnapshot(java.util.Map.of());
        assertEquals(previous, LeaderboardSyncSchedule.keepUntilComplete(previous, incoming, false));
        assertEquals(incoming, LeaderboardSyncSchedule.keepUntilComplete(previous, incoming, true));
        assertEquals(previous, LeaderboardSyncSchedule.keepUntilComplete(previous, null, true));
    }

    @Test
    void pagingStaysLocalAndANewPageAppearsWithTheNextSnapshot() {
        List<LeaderboardSnapshot.Row> ten = rows(10);
        assertEquals(10, LeaderboardSnapshot.pageRows(ten, 0).size());
        assertTrue(LeaderboardSnapshot.pageRows(ten, 1).isEmpty());
        assertEquals(0, LeaderboardSyncSchedule.clampPage(0, ten.size()));

        List<LeaderboardSnapshot.Row> fifteen = rows(15);
        assertEquals(5, LeaderboardSnapshot.pageRows(fifteen, 1).size());
        assertEquals(11, LeaderboardSnapshot.pageRows(fifteen, 1).get(0).rank());
        assertEquals(1, LeaderboardSyncSchedule.clampPage(4, fifteen.size()));
        assertEquals(0, LeaderboardSyncSchedule.clampPage(0, 0));
    }

    @Test
    void reachableFloorsAreAlreadyInTheSyncSet() {
        List<String> ids = LeaderboardPlaces.syncBoardIds();
        assertTrue(ids.contains("floor_1"));
        assertTrue(ids.contains("floor_42"));
        assertTrue(ids.contains("floor_100"));
        assertTrue(ids.contains("oak_collection"));
        assertTrue(ids.contains("playtime"));
        assertTrue(ids.contains("smith_skill"));
        assertTrue(ids.contains("factory_level"));
        assertEquals(126, ids.size());
    }

    private static List<LeaderboardSnapshot.Row> rows(int count) {
        List<LeaderboardSnapshot.Row> rows = new ArrayList<>();
        for (int rank = 1; rank <= count; rank++) {
            rows.add(new LeaderboardSnapshot.Row(rank, "P" + rank, rank));
        }
        return rows;
    }
}

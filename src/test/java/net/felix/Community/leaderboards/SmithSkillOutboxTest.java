package net.felix.Community.leaderboards;

import java.nio.file.Path;
import net.felix.Community.leaderboards.panel.SmithSkill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmithSkillOutboxTest {
    private static final String ADA = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String BO = "bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final SmithSkillOutbox.Phase PRESEASON = new SmithSkillOutbox.Phase(9, true);
    private static final SmithSkillOutbox.Phase SEASON = new SmithSkillOutbox.Phase(9, false);

    @TempDir
    Path temp;

    @Test
    void joinBeforeTheWindowStoresWithoutUpload() {
        SmithSkillOutbox outbox = outbox();
        SmithSkillOutbox.Clock clock = clockAt(minutes(30));
        outbox.observe(ADA, PRESEASON, stand(3, 10));

        assertNull(outbox.begin(ADA, PRESEASON, minutes(24), clock, true));
        assertEquals(stand(3, 10), outbox.best(ADA, PRESEASON));
        assertFalse(outbox.inFlight(ADA));
    }

    @Test
    void joinInsideTheWindowUploadsOnce() {
        SmithSkillOutbox outbox = outbox();
        SmithSkillOutbox.Clock clock = clockAt(minutes(30));
        outbox.observe(ADA, PRESEASON, stand(3, 10));

        SmithSkillOutbox.Attempt attempt = outbox.begin(ADA, PRESEASON, minutes(26), clock, true);
        assertEquals(stand(3, 10), attempt.stand());
        assertEquals(minutes(30), attempt.nextSyncAt());
        assertNull(outbox.begin(ADA, PRESEASON, minutes(27), clock, true));
    }

    @Test
    void repeatedReadsKeepTheHighestAndSkipUnchangedSaves() {
        SmithSkillOutbox outbox = outbox();
        outbox.observe(ADA, PRESEASON, stand(3, 10));
        outbox.observe(ADA, PRESEASON, stand(3, 50));
        outbox.observe(ADA, PRESEASON, stand(4, 1));
        int writes = outbox.saves();
        outbox.observe(ADA, PRESEASON, stand(4, 1));
        outbox.observe(ADA, PRESEASON, stand(3, 999));

        assertEquals(stand(4, 1), outbox.best(ADA, PRESEASON));
        assertEquals(writes, outbox.saves());
        assertTrue(SmithSkillOutbox.better(stand(4, 1), stand(3, 1_000L)));
    }

    @Test
    void missingWidgetDoesNotCreateAnEntry() {
        SmithSkillOutbox outbox = outbox();
        outbox.observe(ADA, PRESEASON, null);
        outbox.observe(ADA, null, stand(2, 5));

        assertNull(outbox.best(ADA, PRESEASON));
        assertEquals(0, outbox.saves());
    }

    @Test
    void slowRequestBlocksASecondUploadAndALateReplyStaysOnItsPhase() {
        SmithSkillOutbox outbox = outbox();
        SmithSkillOutbox.Clock clock = clockAt(minutes(30));
        outbox.observe(ADA, PRESEASON, stand(5, 8));
        outbox.observe(BO, SEASON, stand(1, 1));
        SmithSkillOutbox.Attempt ada = outbox.begin(ADA, PRESEASON, minutes(28), clock, true);
        assertNull(outbox.begin(ADA, PRESEASON, minutes(29), clock, true));

        outbox.complete(ada, true, minutes(29));
        assertEquals(minutes(30), outbox.uploadedSyncAt(ADA, PRESEASON));
        assertNull(outbox.uploadedSyncAt(BO, SEASON));
        assertNull(outbox.uploaded(BO, SEASON));
        assertFalse(outbox.inFlight(ADA));
    }

    @Test
    void failureRetriesWhileTheWindowIsOpenAndThenWaits() {
        SmithSkillOutbox outbox = outbox();
        SmithSkillOutbox.Clock clock = clockAt(minutes(30));
        outbox.observe(ADA, PRESEASON, stand(6, 2));
        SmithSkillOutbox.Attempt first = outbox.begin(ADA, PRESEASON, minutes(25), clock, true);
        outbox.complete(first, false, minutes(25));

        assertNull(outbox.begin(ADA, PRESEASON, minutes(25) + 1_000L, clock, true));
        SmithSkillOutbox.Attempt retry = outbox.begin(ADA, PRESEASON, minutes(25) + SmithSkillOutbox.RETRY_INITIAL_MS, clock, true);
        assertEquals(stand(6, 2), retry.stand());
        outbox.complete(retry, false, minutes(25) + SmithSkillOutbox.RETRY_INITIAL_MS);

        assertNull(outbox.begin(ADA, PRESEASON, minutes(31), clock, true));
        assertNull(outbox.uploadedSyncAt(ADA, PRESEASON));
        assertEquals(stand(6, 2), outbox.best(ADA, PRESEASON));
    }

    @Test
    void growthAfterSuccessWaitsForTheNextWindow() {
        SmithSkillOutbox outbox = outbox();
        SmithSkillOutbox.Clock clock = clockAt(minutes(30));
        outbox.observe(ADA, PRESEASON, stand(7, 4));
        outbox.complete(outbox.begin(ADA, PRESEASON, minutes(26), clock, true), true, minutes(26));
        outbox.observe(ADA, PRESEASON, stand(7, 90));

        assertNull(outbox.begin(ADA, PRESEASON, minutes(29), clock, true));
        SmithSkillOutbox.Clock next = clockAt(minutes(60));
        assertEquals(stand(7, 90), outbox.begin(ADA, PRESEASON, minutes(56), next, true).stand());
    }

    @Test
    void restartInsideTheSameWindowKeepsFurtherXpForLater() {
        Path file = temp.resolve("smith-skill.json");
        SmithSkillOutbox first = new SmithSkillOutbox(file);
        SmithSkillOutbox.Clock clock = clockAt(minutes(30));
        first.observe(ADA, PRESEASON, stand(8, 10));
        first.complete(first.begin(ADA, PRESEASON, minutes(26), clock, true), true, minutes(26));

        SmithSkillOutbox restarted = new SmithSkillOutbox(file);
        restarted.load();
        restarted.observe(ADA, PRESEASON, stand(8, 40));

        assertEquals(stand(8, 40), restarted.best(ADA, PRESEASON));
        assertEquals(stand(8, 10), restarted.uploaded(ADA, PRESEASON));
        assertEquals(minutes(30), restarted.uploadedSyncAt(ADA, PRESEASON));
        assertNull(restarted.begin(ADA, PRESEASON, minutes(29), clock, true));
        assertEquals(stand(8, 40), restarted.begin(ADA, PRESEASON, minutes(56), clockAt(minutes(60)), true).stand());
    }

    @Test
    void sameSeasonIdStartsEmptyWhenPreseasonEnds() {
        SmithSkillOutbox outbox = outbox();
        outbox.observe(ADA, PRESEASON, stand(9, 3));
        SmithSkillOutbox.Clock clock = clockAt(minutes(30));
        outbox.complete(outbox.begin(ADA, PRESEASON, minutes(26), clock, true), true, minutes(26));
        outbox.observe(ADA, null, stand(1, 1));

        assertEquals(stand(9, 3), outbox.best(ADA, PRESEASON));
        assertEquals(minutes(30), outbox.uploadedSyncAt(ADA, PRESEASON));
        assertNull(outbox.best(ADA, SEASON));
        outbox.observe(ADA, SEASON, stand(2, 5));
        assertEquals(stand(2, 5), outbox.best(ADA, SEASON));
        assertNull(outbox.uploadedSyncAt(ADA, SEASON));
        assertEquals(stand(9, 3), outbox.best(ADA, PRESEASON));
    }

    @Test
    void latePreseasonSuccessDoesNotMarkTheRegularPhase() {
        SmithSkillOutbox outbox = outbox();
        SmithSkillOutbox.Clock clock = clockAt(minutes(30));
        outbox.observe(ADA, PRESEASON, stand(4, 7));
        SmithSkillOutbox.Attempt preseason = outbox.begin(ADA, PRESEASON, minutes(27), clock, true);
        outbox.observe(ADA, SEASON, stand(1, 2));

        outbox.complete(preseason, true, minutes(29));

        assertEquals(minutes(30), outbox.uploadedSyncAt(ADA, PRESEASON));
        assertNull(outbox.uploadedSyncAt(ADA, SEASON));
        assertEquals(stand(1, 2), outbox.begin(ADA, SEASON, minutes(29), clock, true).stand());
    }

    @Test
    void skewedServerClockOpensFiveMinutesBeforeTheClientSync() {
        long serverTime = minutes(15);
        long nextSyncAt = minutes(30);
        long receivedAt = serverTime + 60_000L;
        SmithSkillOutbox.Clock clock = new SmithSkillOutbox.Clock(receivedAt, serverTime, nextSyncAt);
        long target = SmithSkillOutbox.clientSyncAt(clock);

        assertEquals(receivedAt + (nextSyncAt - serverTime), target);
        assertFalse(SmithSkillOutbox.windowOpen(target - SmithSkillOutbox.WINDOW_MS - 1L, clock));
        assertTrue(SmithSkillOutbox.windowOpen(target - SmithSkillOutbox.WINDOW_MS, clock));
        assertFalse(SmithSkillOutbox.windowOpen(target, clock));
    }

    @Test
    void closedWindowAndMissingPermissionDoNotSend() {
        SmithSkillOutbox outbox = outbox();
        SmithSkillOutbox.Clock clock = clockAt(minutes(30));
        outbox.observe(ADA, PRESEASON, stand(3, 3));

        assertNull(outbox.begin(ADA, PRESEASON, minutes(26), clock, false));
        assertNull(outbox.begin(ADA, PRESEASON, minutes(26), null, true));
    }

    private SmithSkillOutbox outbox() {
        return new SmithSkillOutbox(null);
    }

    private static SmithSkill.Stand stand(int level, long xp) {
        return new SmithSkill.Stand(level, xp);
    }

    private static SmithSkillOutbox.Clock clockAt(long nextSyncAt) {
        return new SmithSkillOutbox.Clock(nextSyncAt, nextSyncAt, nextSyncAt);
    }

    private static long minutes(int minute) {
        return minute * 60_000L;
    }
}

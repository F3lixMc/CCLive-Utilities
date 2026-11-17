package net.felix.Community.leaderboards;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FactoryLevelOutboxTest {
    private static final String ADA = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final SmithSkillOutbox.Phase PRESEASON = new SmithSkillOutbox.Phase(9, true);
    private static final SmithSkillOutbox.Phase SEASON = new SmithSkillOutbox.Phase(9, false);

    @TempDir
    Path temp;

    @Test
    void windowUploadsOnceAndAFailureWaits() {
        FactoryLevelOutbox outbox = new FactoryLevelOutbox(null);
        SmithSkillOutbox.Clock clock = clockAt(minutes(30));
        outbox.observe(ADA, PRESEASON, 12);
        assertNull(outbox.begin(ADA, PRESEASON, minutes(20), clock, true));

        FactoryLevelOutbox.Attempt attempt = outbox.begin(ADA, PRESEASON, minutes(26), clock, true);
        assertEquals(12, attempt.level());
        outbox.complete(attempt, false, minutes(26));
        assertNull(outbox.begin(ADA, PRESEASON, minutes(26) + 1_000L, clock, true));
        assertEquals(12, outbox.begin(ADA, PRESEASON, minutes(26) + SmithSkillOutbox.RETRY_INITIAL_MS, clock, true).level());
        assertNull(outbox.begin(ADA, PRESEASON, minutes(31), clock, true));
    }

    @Test
    void restartInsideTheSameWindowKeepsFurtherLevelForLater() {
        Path file = temp.resolve("factory-level.json");
        FactoryLevelOutbox first = new FactoryLevelOutbox(file);
        SmithSkillOutbox.Clock clock = clockAt(minutes(30));
        first.observe(ADA, PRESEASON, 40);
        first.complete(first.begin(ADA, PRESEASON, minutes(26), clock, true), true, minutes(26));

        FactoryLevelOutbox restarted = new FactoryLevelOutbox(file);
        restarted.load();
        restarted.observe(ADA, PRESEASON, 55);

        assertEquals(55, restarted.best(ADA, PRESEASON));
        assertEquals(40, restarted.uploaded(ADA, PRESEASON));
        assertEquals(minutes(30), restarted.uploadedSyncAt(ADA, PRESEASON));
        assertNull(restarted.begin(ADA, PRESEASON, minutes(29), clock, true));
        assertEquals(55, restarted.begin(ADA, PRESEASON, minutes(56), clockAt(minutes(60)), true).level());
    }

    @Test
    void sameSeasonIdStartsEmptyWhenPreseasonEnds() {
        FactoryLevelOutbox outbox = new FactoryLevelOutbox(null);
        outbox.observe(ADA, PRESEASON, 80);
        outbox.observe(ADA, null, 1);
        assertEquals(80, outbox.best(ADA, PRESEASON));
        assertNull(outbox.best(ADA, SEASON));
        outbox.observe(ADA, SEASON, 3);
        assertEquals(3, outbox.best(ADA, SEASON));
        assertEquals(80, outbox.best(ADA, PRESEASON));
    }

    @Test
    void equalOrLowerLevelDoesNotReplaceTheBest() {
        FactoryLevelOutbox outbox = new FactoryLevelOutbox(null);
        outbox.observe(ADA, PRESEASON, 10);
        int saves = outbox.saves();
        outbox.observe(ADA, PRESEASON, 10);
        outbox.observe(ADA, PRESEASON, 4);
        assertEquals(saves, outbox.saves());
        assertEquals(10, outbox.best(ADA, PRESEASON));
    }

    private static SmithSkillOutbox.Clock clockAt(long nextSyncAt) {
        return new SmithSkillOutbox.Clock(nextSyncAt, nextSyncAt, nextSyncAt);
    }

    private static long minutes(int minute) {
        return minute * 60_000L;
    }
}

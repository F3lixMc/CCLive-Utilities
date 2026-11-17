package net.felix.Community.leaderboards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonChangeGateTest {
    @Test
    void sameIdDropsPreseasonAndNextIdsResetAgain() {
        assertTrue(SeasonChangeGate.needsLocalReset(9, true, 9, false));
        assertTrue(SeasonChangeGate.needsLocalReset(9, false, 10, true));
        assertTrue(SeasonChangeGate.needsLocalReset(10, true, 10, false));
        assertFalse(SeasonChangeGate.needsLocalReset(9, true, 9, true));
        assertFalse(SeasonChangeGate.needsLocalReset(10, false, 10, false));
    }

    @Test
    void missingProcessedSeasonIsNotAConfirmedSeason() {
        assertEquals(null, SeasonChangeGate.confirmedSeasonId(-1));
        assertEquals(Integer.valueOf(9), SeasonChangeGate.confirmedSeasonId(9));
    }

    @Test
    void labelUsesSeasonNumberNotDatabaseId() {
        assertEquals("Season 8", SeasonChangeGate.resetLabel(9, false));
        assertEquals("Preseason 9", SeasonChangeGate.resetLabel(10, true));
        assertEquals("Season 9", SeasonChangeGate.resetLabel(10, false));
        assertEquals("Preseason 8", SeasonChangeGate.resetLabel(9, true));
    }
}

package net.felix.utilities.Community.leaderboards;

import net.felix.utilities.Other.server.ServerConnectionGate.MatchState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresenceCountTest {
    @Test
    void onlyAConnectedPlayLegendUserCounts() {
        assertTrue(LeaderboardManager.countsOnPlayLegend(
            true, true, true, true, true, true, MatchState.ALLOWED));
    }

    @Test
    void mainMenuSingleplayerAndOtherServersDoNotCount() {
        assertFalse(LeaderboardManager.countsOnPlayLegend(
            true, true, true, true, false, false, MatchState.DISALLOWED));
        assertFalse(LeaderboardManager.countsOnPlayLegend(
            true, true, true, true, true, true, MatchState.DISALLOWED));
        assertFalse(LeaderboardManager.countsOnPlayLegend(
            true, true, true, true, true, true, MatchState.UNKNOWN));
        assertFalse(LeaderboardManager.countsOnPlayLegend(
            true, true, true, true, true, false, MatchState.ALLOWED));
    }
}

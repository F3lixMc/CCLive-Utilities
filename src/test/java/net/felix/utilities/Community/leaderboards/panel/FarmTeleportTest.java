package net.felix.utilities.Community.leaderboards.panel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmTeleportTest {
    @Test
    void aJumpInsideTheFarmDropsTheManualBoard() {
        assertTrue(LeaderboardInventoryOverlay.isFarmZoneTeleport(48.0 * 48.0, true, true));
        assertFalse(LeaderboardInventoryOverlay.isFarmZoneTeleport(47.0 * 47.0, true, true));
        assertFalse(LeaderboardInventoryOverlay.isFarmZoneTeleport(200.0 * 200.0, false, true));
        assertFalse(LeaderboardInventoryOverlay.isFarmZoneTeleport(200.0 * 200.0, true, false));
    }
}

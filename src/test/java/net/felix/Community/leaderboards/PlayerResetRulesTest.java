package net.felix.Community.leaderboards;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerResetRulesTest {
    @Test
    void repeatedResetAAfterBDoesNotDeleteOrReplaceTheUploadId() {
        Set<String> applied = new HashSet<>();
        assertTrue(PlayerResetRules.appliesFreshReset("A", applied));
        applied.add("A");
        String current = PlayerResetRules.currentUploadId(null, "A", true);
        assertTrue(PlayerResetRules.appliesFreshReset("B", applied));
        applied.add("B");
        current = PlayerResetRules.currentUploadId(current, "B", true);
        assertFalse(PlayerResetRules.appliesFreshReset("A", applied));
        assertEquals("B", PlayerResetRules.currentUploadId(current, "A", false));
        assertFalse(PlayerResetRules.acceptsUpload("alltime_kills", "A", current));
        assertTrue(PlayerResetRules.acceptsUpload("alltime_kills", "B", current));
    }

    @Test
    void offlineOrderKeepsSeasonAWhenSeasonBIsCurrent() {
        assertEquals(8, PlayerResetRules.seasonToDelete(8));
        assertNull(PlayerResetRules.seasonToDelete(null));
        assertNull(PlayerResetRules.seasonToDelete(-1));
    }

    @Test
    void missingResetIdStaysCompatibleUntilAResetExists() {
        assertTrue(PlayerResetRules.acceptsUpload("alltime_kills", null, null));
        assertFalse(PlayerResetRules.acceptsUpload("smith_skill", null, "B"));
        assertTrue(PlayerResetRules.acceptsUpload("playtime", null, "B"));
    }
}

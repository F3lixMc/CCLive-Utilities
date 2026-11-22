package net.felix.Community.leaderboards.reset;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerFullResetLedgerTest {
    @Test
    void classScreenFiresOnceUntilItCloses() {
        PlayerFullResetLedger ledger = new PlayerFullResetLedger();
        assertTrue(ledger.opening(true));
        assertFalse(ledger.opening(true));
        assertFalse(ledger.opening(false));
        assertTrue(ledger.opening(true));
    }

    @Test
    void orderIsStoredBeforeLocalClearAndKeepsItsSeason() {
        PlayerFullResetLedger ledger = new PlayerFullResetLedger();
        assertNull(ledger.stage("id", "account", null, false));
        PlayerFullResetLedger.Order order = ledger.stage("A", "account", 8, false);
        assertFalse(order.localCleared());
        assertEquals(1, ledger.pendingLocal().size());
        assertEquals(8, ledger.pendingLocal().get(0).seasonId());
        ledger.markLocalCleared("A");
        assertTrue(ledger.pendingServer().get(0).localCleared());
        assertFalse(ledger.blocks("account", 9, false));
        assertTrue(ledger.blocks("account", 8, false));
    }

    @Test
    void repeatedResetAAfterBDoesNotReplaceTheUploadId() {
        PlayerFullResetLedger ledger = new PlayerFullResetLedger();
        ledger.stage("A", "account", 8, false);
        ledger.markLocalCleared("A");
        ledger.markServerApplied("A");
        ledger.stage("B", "account", 8, false);
        ledger.markLocalCleared("B");
        ledger.markServerApplied("B");
        assertFalse(ledger.markServerApplied("A"));
        assertEquals("B", ledger.uploadId("account", 8, false));
        assertFalse(ledger.blocks("account", 8, false));
    }

    @Test
    void unfinishedLocalClearSurvivesReload() {
        PlayerFullResetLedger ledger = new PlayerFullResetLedger();
        ledger.stage("A", "account", 8, true);
        PlayerFullResetLedger loaded = PlayerFullResetLedger.fromJson(JsonParser.parseString(ledger.toJson().toString()).getAsJsonObject());
        assertEquals(1, loaded.pendingLocal().size());
        assertEquals(8, loaded.pendingLocal().get(0).seasonId());
        assertTrue(loaded.pendingLocal().get(0).preseason());
        assertFalse(loaded.pendingLocal().get(0).localCleared());
    }
}

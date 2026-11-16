package net.felix.utilities.Community.chat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRulesAcceptanceTest {
    @TempDir
    Path temp;

    @Test
    void onlyCcliveWritesAndAtCcliveAreGated() {
        assertTrue(ChatRulesAcceptance.isRulesGatedSend("hallo", true));
        assertTrue(ChatRulesAcceptance.isRulesGatedSend("@cclive hallo", false));
        assertTrue(ChatRulesAcceptance.isRulesGatedSend("@cclive hallo", true));
        assertFalse(ChatRulesAcceptance.isRulesGatedSend("@cclive", false));
        assertFalse(ChatRulesAcceptance.isRulesGatedSend("@cclive   ", true));
        assertFalse(ChatRulesAcceptance.isRulesGatedSend("hallo", false));
        assertFalse(ChatRulesAcceptance.isRulesGatedSend("@global hallo", true));
        assertFalse(ChatRulesAcceptance.isRulesGatedSend("@lp hallo", true));
        assertFalse(ChatRulesAcceptance.isRulesGatedSend("/cclive chatrules", true));
    }

    @Test
    void firstSendHoldsDraftUntilAcceptedSeason() {
        ChatRulesAcceptance store = ChatRulesAcceptance.load(temp.resolve("accepted.json"));
        assertTrue(store.shouldHold("konto-a", 9));
        store.hold("erster text");
        assertEquals("erster text", store.draft());
        assertFalse(store.isAccepted("konto-a", 9));
        assertFalse(store.accept("konto-a", 9, false));
        assertEquals("erster text", store.draft());
        assertFalse(store.accept("konto-a", null, true));
        assertTrue(store.accept("konto-a", 9, true));
        assertFalse(store.shouldHold("konto-a", 9));
    }

    @Test
    void escapeKeepsDraftAndDoesNotAccept() {
        ChatRulesAcceptance store = ChatRulesAcceptance.load(temp.resolve("accepted.json"));
        store.hold("@cclive noch da");
        assertEquals("@cclive noch da", store.draft());
        assertTrue(store.shouldHold("konto-a", 9));
        store.hold("@cclive noch da");
        assertEquals("@cclive noch da", store.draft());
        assertFalse(store.isAccepted("konto-a", 9));
    }

    @Test
    void reconnectKeepsAcceptanceUntilSeasonOrAccountChanges() {
        Path file = temp.resolve("accepted.json");
        ChatRulesAcceptance first = ChatRulesAcceptance.load(file);
        assertTrue(first.accept("konto-a", 9, true));
        first.hold("weg nach neustart");
        first.clearDraft();

        ChatRulesAcceptance reloaded = ChatRulesAcceptance.load(file);
        assertNull(reloaded.draft());
        assertTrue(reloaded.isAccepted("konto-a", 9));
        assertTrue(reloaded.isAccepted("konto-a", null));
        assertFalse(reloaded.shouldHold("konto-a", null));
        assertTrue(reloaded.shouldHold("konto-a", 10));
        assertTrue(reloaded.shouldHold("konto-b", 9));
        assertTrue(reloaded.accept("konto-b", 9, true));
        assertTrue(reloaded.isAccepted("konto-a", 9));
        assertFalse(reloaded.shouldHold("konto-b", 9));
    }
}

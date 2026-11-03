package net.felix.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebsiteLinkMessagesTest {
    @Test
    void mojang500IsTemporaryAndNotAlreadyConnected() {
        String message = WebsiteLinkMessages.mojangJoinFailure(500);
        assertTrue(message.contains("vorübergehend"));
        assertFalse(message.contains(WebsiteLinkMessages.ALREADY_CONNECTED));
        assertFalse(WebsiteLinkMessages.isAlreadyConnected(500, null));
        assertFalse(WebsiteLinkMessages.isAlreadyConnected(500, "already_connected"));
        assertTrue(WebsiteLinkMessages.isAlreadyConnected(409, "already_connected"));
    }
}

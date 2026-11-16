package net.felix.utilities.Overall.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConnectionGateTest {
    @Test
    void onlyAcceptsTheProductionAddress() {
        assertTrue(ServerConnectionGate.matchesAllowed("playlegend.net"));
        assertTrue(ServerConnectionGate.matchesAllowed("PLAYLEGEND.NET:25565"));
        assertFalse(ServerConnectionGate.matchesAllowed("test.playlegend.net"));
        assertFalse(ServerConnectionGate.matchesAllowed("playlegend.net.example"));
        assertFalse(ServerConnectionGate.matchesAllowed("playlegend.net:25566"));
        assertFalse(ServerConnectionGate.matchesAllowed("playlegend.net/192.0.2.1"));
        assertFalse(ServerConnectionGate.matchesAllowed("192.0.2.1:25565"));
    }
}

package net.felix.utilities.Other.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @org.junit.jupiter.api.Test
    void betaNameReachesTheSameServer() {
        assertTrue(ServerConnectionGate.matchesAllowed("beta.playlegend.net"));
        assertTrue(ServerConnectionGate.matchesAllowed("BETA.playlegend.net:25565"));
        assertFalse(ServerConnectionGate.matchesAllowed("beta.playlegend.net:25566"));
        assertFalse(ServerConnectionGate.matchesAllowed("beta.playlegend.net.example"));
    }
    @Test
    void devServerAllowsFeaturesButNoUploads() {
        assertEquals(ServerConnectionGate.MatchState.FEATURES_ONLY, ServerConnectionGate.classify("dev.playlegend.net"));
        assertEquals(ServerConnectionGate.MatchState.FEATURES_ONLY, ServerConnectionGate.classify("DEV.playlegend.net:25565"));
        assertFalse(ServerConnectionGate.matchesAllowed("dev.playlegend.net"));
        assertEquals(ServerConnectionGate.MatchState.FEATURES_ONLY, ServerConnectionGate.classify("stage.playlegend.net:25565"));
        assertFalse(ServerConnectionGate.matchesAllowed("stage.playlegend.net"));
        assertEquals(ServerConnectionGate.MatchState.DISALLOWED, ServerConnectionGate.classify("dev.playlegend.net:25566"));
        assertEquals(ServerConnectionGate.MatchState.ALLOWED, ServerConnectionGate.classify("playlegend.net"));
        assertEquals(ServerConnectionGate.MatchState.DISALLOWED, ServerConnectionGate.classify("example.net"));
    }
}

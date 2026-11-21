package net.felix.Community.leaderboards.collectors;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FloorKillStoreTest {
    @Test
    void restartKeepsTheHigherStand() throws Exception {
        Path file = Files.createTempDirectory("floor-kills").resolve("floor-kills.json");
        FloorKillStore store = new FloorKillStore(file);
        assertEquals(100L, store.read("account", 8, false, "floor_1", 100L));
        assertEquals(100L, store.read("account", 8, false, "floor_1", 100L));
        assertEquals(100L, store.read("account", 8, false, "floor_1", 40L));
        store.read("account", 8, false, "floor_2", 25L);
        store.read("account", 9, true, "floor_1", 7L);

        FloorKillStore restarted = new FloorKillStore(file);
        assertEquals(125L, restarted.sum("account", 8, false));
        assertEquals(7L, restarted.sum("account", 9, true));
        restarted.clear("account", 8, false);
        assertEquals(0L, new FloorKillStore(file).sum("account", 8, false));
        assertEquals(7L, new FloorKillStore(file).sum("account", 9, true));
    }
}

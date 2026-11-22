package net.felix.Community.leaderboards.collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FloorKillBookTest {
    @Test
    void sameReadingDoesNotAddAndLowerReadingDoesNotShrink() {
        FloorKillBook book = new FloorKillBook();
        assertEquals(100L, book.read("account", 8, false, "floor_1", 100L));
        assertEquals(100L, book.read("account", 8, false, "floor_1", 100L));
        assertEquals(100L, book.read("account", 8, false, "floor_1", 40L));
        book.read("account", 8, false, "floor_2", 25L);
        assertEquals(125L, book.sum("account", 8, false));
    }

    @Test
    void restartAndOtherPhaseKeepTheirOwnStands() {
        FloorKillBook book = new FloorKillBook();
        book.read("account", 8, false, "floor_1", 100L);
        book.read("account", 9, true, "floor_1", 7L);
        FloorKillBook restored = FloorKillBook.restore(book.snapshot());
        assertEquals(100L, restored.sum("account", 8, false));
        assertEquals(7L, restored.sum("account", 9, true));
        restored.clear("account", 8, false);
        assertEquals(0L, restored.sum("account", 8, false));
        assertEquals(7L, restored.sum("account", 9, true));
    }
}

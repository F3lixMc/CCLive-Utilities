package net.felix.Community.leaderboards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FactoryLevelTest {
    @Test
    void onlyAFactoryReadingBetweenOneAndTwoThousandCounts() {
        assertEquals(1, FactoryLevel.capture(true, 1));
        assertEquals(2000, FactoryLevel.capture(true, 2000));
        assertNull(FactoryLevel.capture(false, 10));
        assertNull(FactoryLevel.capture(true, 0));
        assertNull(FactoryLevel.capture(true, 2001));
    }
}

package net.felix.Community.leaderboards.panel;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelFontRunsTest {
    @Test
    void runsCoverTheSamePixelsAsPlot() {
        String text = "M8/A.Ö";
        Set<Long> fromPlot = new HashSet<>();
        PixelFont57.plot(text, 2, 3, (x, y) -> fromPlot.add(pack(x, y)));

        Set<Long> fromRuns = new HashSet<>();
        int[] runCount = {0};
        PixelFont57.plotRuns(text, 2, 3, (x, y, width) -> {
            runCount[0]++;
            for (int i = 0; i < width; i++) {
                fromRuns.add(pack(x + i, y));
            }
        });

        assertEquals(fromPlot, fromRuns);
        assertTrue(runCount[0] < fromPlot.size());
        assertTrue(runCount[0] > 0);
    }

    private static long pack(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }
}

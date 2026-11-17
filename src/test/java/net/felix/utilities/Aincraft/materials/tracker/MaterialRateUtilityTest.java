package net.felix.utilities.Aincraft.materials.tracker;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialRateUtilityTest {
    @BeforeEach
    void reset() {
        MaterialRateUtility.resetSession();
    }

    @Test
    void clipboardRateWaitsForGainAndHidesAfterStall() {
        long start = 1_000_000L;
        MaterialRateUtility.observeCount("Holz", 10, start);
        assertEquals(0.0, MaterialRateUtility.getRateForMaterial("Holz", start + 10_000L));

        MaterialRateUtility.observeCount("Holz", 20, start + 1_000L);
        assertTrue(MaterialRateUtility.immediateRate("Holz") > 0.0);
        assertEquals(0.0, MaterialRateUtility.getRateForMaterial("Holz", start + 1_000L + 7_000L));
        assertTrue(MaterialRateUtility.getRateForMaterial("Holz", start + 1_000L + 8_000L) > 0.0);

        assertEquals(0.0, MaterialRateUtility.getRateForMaterial("Holz", start + 1_000L + 30_001L));
    }

    @Test
    void unchangedOrFallingCountDoesNotCreateClipboardRate() {
        long start = 5_000L;
        MaterialRateUtility.observeCount("Stein", 40, start);
        MaterialRateUtility.observeCount("Stein", 40, start + 20_000L);
        assertEquals(0.0, MaterialRateUtility.getRateForMaterial("Stein", start + 20_000L));

        MaterialRateUtility.observeCount("Stein", 50, start + 21_000L);
        MaterialRateUtility.observeCount("Stein", 10, start + 30_000L);
        assertEquals(0.0, MaterialRateUtility.getRateForMaterial("Stein", start + 40_000L));
    }

    @Test
    void resetClearsTheMeasuredRate() {
        MaterialRateUtility.observeCount("Holz", 1, 100L);
        MaterialRateUtility.observeCount("Holz", 5, 9_000L);
        MaterialRateUtility.resetSession();
        assertEquals(0.0, MaterialRateUtility.getRateForMaterial("Holz", 20_000L));
    }

    @Test
    void scoreboardUsesParenthesesAndOverlayKeepsArrow() {
        Component idle = Component.literal("➥ 4");
        String idleScoreboard = MaterialRateUtility.appendRateToScoreboardLine(idle, 0).getString();
        String idleOverlay = MaterialRateUtility.appendRateForMaterial("Holz", idle).getString();
        assertTrue(idleScoreboard.endsWith(" (x/min)"));
        assertFalse(idleScoreboard.contains("->"));
        assertTrue(idleOverlay.endsWith(" -> x/min"));

        long start = 10_000L;
        MaterialRateUtility.observeCount("Holz", 10, start);
        MaterialRateUtility.observeCount("Holz", 20, start + 1_000L);
        Component line = Component.literal("➥ 20");
        String scoreboard = MaterialRateUtility.appendRateToScoreboardLine(line, 0).getString();
        String overlay = MaterialRateUtility.appendRateForMaterial("Holz", line).getString();
        assertTrue(scoreboard.startsWith("➥ 20 ("));
        assertTrue(scoreboard.endsWith("/min)"));
        assertFalse(scoreboard.contains("->"));
        assertFalse(scoreboard.contains("(x/min)"));
        assertTrue(overlay.contains(" -> "));
        assertFalse(overlay.contains("("));
        assertTrue(MaterialRateUtility.getRateForMaterial("Holz", start + 1_000L) == 0.0);
        assertTrue(MaterialRateUtility.immediateRate("Holz") > 0.0);
    }
}

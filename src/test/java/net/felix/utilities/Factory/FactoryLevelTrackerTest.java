package net.felix.utilities.Factory;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactoryLevelTrackerTest {
    @Test
    void nextTargetUsesOneLinePerUnlock() {
        assertEquals(List.of(
                "Nächstes Ziel: Level 150",
                "• Machtkristallslot 2",
                "• Runenslot 3",
                "",
                "Dauer bis Ziel: 2h"
        ), FactoryLevelDisplay.body(149, FactoryLevelEstimate.Status.READY, 2L * 60L * 60L * 1000L));

        assertEquals(List.of(
                "Nächstes Ziel: Level 100",
                "• Gadgetslot 3",
                "• Runenslot 2",
                "",
                "Dauer bis Ziel: Wird ermittelt …"
        ), FactoryLevelDisplay.body(99, FactoryLevelEstimate.Status.MEASURING, 0L));

        assertEquals(List.of(
                "Nächstes Ziel: Level 80",
                "• Machtkristallslot 1",
                "Dauer bis Ziel: Wird ermittelt …"
        ), FactoryLevelDisplay.body(79, FactoryLevelEstimate.Status.MEASURING, 0L));

        assertTrue(FactoryLevelDisplay.lineCount(149) > FactoryLevelDisplay.lineCount(79));
        assertTrue(FactoryLevelDisplay.body(349, FactoryLevelEstimate.Status.MEASURING, 0L).contains("• Machtkristallslot 3"));
        assertTrue(FactoryLevelDisplay.body(249, FactoryLevelEstimate.Status.MEASURING, 0L).contains("• Recycler"));
    }

    @Test
    void level1000HasNoTargetOrTimeLine() {
        List<String> lines = FactoryLevelDisplay.body(1000, FactoryLevelEstimate.Status.READY, 5_000L);
        assertEquals(List.of("Alle Ziele freigeschaltet"), lines);
        assertFalse(lines.stream().anyMatch(line -> line.contains("Dauer")));
        assertFalse(lines.stream().anyMatch(line -> line.contains("Nächstes Ziel")));
        assertEquals(2, FactoryLevelDisplay.lineCount(1000));
        assertEquals(List.of("Alle Ziele freigeschaltet"),
                FactoryLevelDisplay.body(1500, FactoryLevelEstimate.Status.READY, 0L));
    }

    @Test
    void durationUsesOneRoundedUnit() {
        assertEquals("1d", FactoryLevelDisplay.formatDuration(24L * 60L * 60L * 1000L));
        assertEquals("2d", FactoryLevelDisplay.formatDuration(25L * 60L * 60L * 1000L));
        assertEquals("2h", FactoryLevelDisplay.formatDuration(2L * 60L * 60L * 1000L));
        assertEquals("2h", FactoryLevelDisplay.formatDuration(90L * 60L * 1000L));
        assertEquals("2m", FactoryLevelDisplay.formatDuration(61L * 1000L));
        assertEquals("59sek", FactoryLevelDisplay.formatDuration(59L * 1000L));
        assertEquals("1sek", FactoryLevelDisplay.formatDuration(1L));
        assertEquals("1sek", FactoryLevelDisplay.formatDuration(0L));
    }

    @Test
    void levelUpStaysAnIncrease() {
        FactoryLevelEstimate estimate = new FactoryLevelEstimate();
        estimate.observe(0L, 9.98);
        estimate.observe(FactoryLevelEstimate.MIN_SPAN_MS, 10.0);

        assertEquals(FactoryLevelEstimate.Status.READY, estimate.status(FactoryLevelEstimate.MIN_SPAN_MS));
        OptionalLong remaining = estimate.remainingMillis(FactoryLevelEstimate.MIN_SPAN_MS, 10.0, 11.0);
        assertTrue(remaining.isPresent());
        assertTrue(remaining.getAsLong() > 0L);

        FactoryLevelEstimate exact = new FactoryLevelEstimate();
        exact.observe(0L, 10.0);
        exact.observe(FactoryLevelEstimate.MIN_SPAN_MS, 10.5);
        assertEquals(OptionalLong.of(8_000L),
                exact.remainingMillis(FactoryLevelEstimate.MIN_SPAN_MS, 10.5, 11.0));
    }

    @Test
    void progressDropDiscardsTheRate() {
        FactoryLevelEstimate estimate = new FactoryLevelEstimate();
        estimate.observe(0L, 10.0);
        estimate.observe(FactoryLevelEstimate.MIN_SPAN_MS, 10.2);
        assertEquals(FactoryLevelEstimate.Status.READY, estimate.status(FactoryLevelEstimate.MIN_SPAN_MS));

        long droppedAt = FactoryLevelEstimate.MIN_SPAN_MS + 1_000L;
        estimate.observe(droppedAt, 9.5);
        assertEquals(FactoryLevelEstimate.Status.MEASURING, estimate.status(droppedAt));
        assertTrue(estimate.remainingMillis(droppedAt, 9.5, 10.0).isEmpty());
    }

    @Test
    void stallDropsTheOldCountdownUntilGrowthRestarts() {
        FactoryLevelEstimate estimate = new FactoryLevelEstimate();
        estimate.observe(0L, 4.0);
        estimate.observe(FactoryLevelEstimate.MIN_SPAN_MS, 4.05);
        long stalledAt = FactoryLevelEstimate.MIN_SPAN_MS + FactoryLevelEstimate.STALL_MS;
        estimate.observe(stalledAt, 4.05);

        assertEquals(FactoryLevelEstimate.Status.STALLED, estimate.status(stalledAt));
        assertTrue(estimate.remainingMillis(stalledAt, 4.05, 5.0).isEmpty());
        assertEquals("Dauer bis Ziel: Kein XP-Zuwachs",
                FactoryLevelDisplay.durationLine(FactoryLevelEstimate.Status.STALLED, 0L));

        long regrowthAt = stalledAt + 1_000L;
        estimate.observe(regrowthAt, 4.20);
        assertEquals(FactoryLevelEstimate.Status.MEASURING, estimate.status(regrowthAt));
        assertEquals("Dauer bis Ziel: Wird ermittelt …",
                FactoryLevelDisplay.durationLine(FactoryLevelEstimate.Status.MEASURING, 0L));
    }

    @Test
    void factoryStateUpdatesOnlyWhenTheWorldOrDimensionChanges() {
        FactoryLevelWatch watch = new FactoryLevelWatch();
        Object home = new Object();
        Object floor = new Object();

        assertTrue(watch.observe(home, "minecraft:fires", "Fires"));
        assertTrue(watch.inFactory());
        assertFalse(watch.observe(home, "minecraft:fires", "Fires"));
        assertTrue(watch.inFactory());

        assertTrue(watch.observe(floor, "minecraft:floor", "Fires"));
        assertFalse(watch.inFactory());

        watch.reset();
        assertFalse(watch.inFactory());

        Object rejoined = new Object();
        assertTrue(watch.observe(rejoined, "minecraft:fires", "Fires"));
        assertTrue(watch.inFactory());
        assertTrue(watch.observe(new Object(), "minecraft:fires", "Fires"));
    }

    @Test
    void factoryDimensionMatchesWaveRule() {
        assertTrue(FactoryLevelPresence.isFactory("minecraft:fires", "Fires"));
        assertTrue(FactoryLevelPresence.isFactory("minecraft:fires_2", "fires"));
        assertFalse(FactoryLevelPresence.isFactory("minecraft:fires_floor", "fires"));
        assertFalse(FactoryLevelPresence.isFactory("minecraft:floor", "fires"));
        assertFalse(FactoryLevelPresence.isFactory("minecraft:other", "fires"));
    }
}

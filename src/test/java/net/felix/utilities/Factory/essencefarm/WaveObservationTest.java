package net.felix.utilities.Factory.essencefarm;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaveObservationTest {

	@Test
	void medianIgnoresSingleOutlier() {
		WaveObservation obs = new WaveObservation();
		obs.addRun(Map.of("vindicator", 30), null);
		obs.addRun(Map.of("vindicator", 31), null);
		obs.addRun(Map.of("vindicator", 900), null);
		assertEquals(31, obs.medianCounts().get("vindicator"));
	}

	@Test
	void rejectsRunsFarAboveStaticCountOrTooLarge() {
		WaveObservation obs = new WaveObservation();
		assertFalse(obs.addRun(Map.of("vindicator", 100), Map.of("vindicator", 30)));
		assertFalse(obs.addRun(Map.of("vindicator", WaveObservation.MAX_MOBS_PER_RUN + 1), null));
		assertTrue(obs.addRun(Map.of("vindicator", 32), Map.of("vindicator", 30)));
		assertEquals(32, obs.medianMobs());
	}

	@Test
	void keepsOnlyRecentRuns() {
		WaveObservation obs = new WaveObservation();
		for (int i = 0; i < WaveObservation.KEPT_RUNS + 3; i++) {
			obs.addRun(Map.of("zombie", 10 + i), null);
		}
		assertEquals(WaveObservation.KEPT_RUNS, obs.recent.size());
	}

	@Test
	void averagesCycles() {
		WaveObservation obs = new WaveObservation();
		obs.addCycle(100_000);
		obs.addCycle(0);
		obs.addCycle(140_000);
		assertEquals(2, obs.cycles);
		assertEquals(120_000, obs.avgCycleMs);
	}
}

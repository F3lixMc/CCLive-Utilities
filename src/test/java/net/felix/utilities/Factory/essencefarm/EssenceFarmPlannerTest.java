package net.felix.utilities.Factory.essencefarm;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EssenceFarmPlannerTest {

	private static final EssenceTierTable TIERS = new EssenceTierTable(List.of(
			new EssenceTierTable.Mob("horse", "Pferd", List.of("Pferde"), "Pferde", new long[] {701, 1401}),
			new EssenceTierTable.Mob("blaze", "Lohe", List.of(), "Lohen", new long[] {710, 1410}),
			new EssenceTierTable.Mob("wolf", "Wolf", List.of(), "Wolf", new long[] {820, 1520})));

	@Test
	void tierCountsThresholdsBelowWave() {
		assertEquals(1, TIERS.tierAt("horse", 1));
		assertEquals(1, TIERS.tierAt("horse", 701));
		assertEquals(2, TIERS.tierAt("horse", 702));
		assertEquals(3, TIERS.tierAt("horse", 5000));
		assertEquals(-1, TIERS.tierAt("unknown", 10));
	}

	@Test
	void namesMapToTypesAndBagKeys() {
		assertEquals("horse", TIERS.typeForName("Pferde"));
		assertEquals("horse", TIERS.typeForName("pferd"));
		assertEquals("Pferd T2", TIERS.displayName("horse", 2));
		assertEquals("Lohen [Essenz] Tier 1", TIERS.bagKey("blaze", 1));
	}

	@Test
	void singleEssenceUsesRunsRoundedUpWithMultiplier() {
		var plan = EssenceFarmPlanner.plan(
				List.of(new EssenceFarmPlanner.Need("horse", 1, 100)),
				List.of(new EssenceFarmPlanner.WaveData(475, 18, Map.of("horse", 6))),
				TIERS, 2.0, EssenceFarmPlanner.DEFAULT_FACTOR);
		assertEquals(1, plan.steps().size());
		// 6 horses * 2 essences = 12 per run, 100 / 12 -> 9 runs
		assertEquals(9, plan.steps().get(0).runs());
		assertEquals(475, plan.steps().get(0).wave());
		assertTrue(plan.unreachable().isEmpty());
	}

	@Test
	void sharedWaveCoversSeveralEssences() {
		var plan = EssenceFarmPlanner.plan(
				List.of(new EssenceFarmPlanner.Need("horse", 1, 60), new EssenceFarmPlanner.Need("blaze", 1, 40)),
				List.of(
						new EssenceFarmPlanner.WaveData(475, 18, Map.of("horse", 6, "blaze", 4)),
						new EssenceFarmPlanner.WaveData(300, 10, Map.of("horse", 6)),
						new EssenceFarmPlanner.WaveData(310, 10, Map.of("blaze", 4))),
				TIERS, 1.0, EssenceFarmPlanner.DEFAULT_FACTOR);
		// Shared wave: 10 runs * 18 = 180 kills; separate waves: 10*10 + 10*10 = 200 kills
		assertEquals(1, plan.steps().size());
		assertEquals(475, plan.steps().get(0).wave());
		assertEquals(10, plan.steps().get(0).runs());
		assertEquals(2, plan.steps().get(0).covered().size());
	}

	@Test
	void wrongTierAndMissingWavesAreUnreachable() {
		var plan = EssenceFarmPlanner.plan(
				List.of(new EssenceFarmPlanner.Need("horse", 2, 50), new EssenceFarmPlanner.Need("wolf", 1, 10)),
				List.of(new EssenceFarmPlanner.WaveData(475, 18, Map.of("horse", 6))),
				TIERS, 1.0, EssenceFarmPlanner.DEFAULT_FACTOR);
		assertTrue(plan.steps().isEmpty());
		assertEquals(2, plan.unreachable().size());
	}

	@Test
	void satisfiedNeedsProduceNoSteps() {
		var plan = EssenceFarmPlanner.plan(
				List.of(new EssenceFarmPlanner.Need("horse", 1, 0)),
				List.of(new EssenceFarmPlanner.WaveData(475, 18, Map.of("horse", 6))),
				TIERS, 1.0, EssenceFarmPlanner.DEFAULT_FACTOR);
		assertTrue(plan.steps().isEmpty());
		assertTrue(plan.unreachable().isEmpty());
	}
}

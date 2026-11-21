package net.felix.utilities.Factory.essencefarm;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EssenceFarmNeedsTest {

	private static final EssenceTierTable TIERS = new EssenceTierTable(List.of(
			new EssenceTierTable.Mob("horse", "Pferd", List.of("Pferde"), "Pferde", new long[] {701}),
			new EssenceTierTable.Mob("blaze", "Lohe", List.of(), "Lohen", new long[] {710})));

	private static final Map<Integer, EssenceFarmNeeds.LevelCost> COSTS = Map.of(
			2, new EssenceFarmNeeds.LevelCost("Pferd T1", 3000),
			3, new EssenceFarmNeeds.LevelCost("Lohe T1", 4000),
			4, new EssenceFarmNeeds.LevelCost("Pferd T1", 5000),
			5, new EssenceFarmNeeds.LevelCost("Wither T8", 6000));

	@Test
	void sumsSameEssenceAcrossCrystalsAndSubtractsStockOnce() {
		var result = EssenceFarmNeeds.compute(List.of(range(1, 2), range(3, 4)), COSTS::get, TIERS,
				key -> key.equals("Pferde [Essenz] Tier 1") ? 1000L : 0L);
		assertEquals(1, result.requirements().size());
		var req = result.requirements().get(0);
		assertEquals("horse", req.type());
		assertEquals(8000, req.total());
		assertEquals(7000, req.missing());
	}

	@Test
	void unknownEssencesAreReportedNotPlanned() {
		var result = EssenceFarmNeeds.compute(List.of(range(3, 5)), COSTS::get, TIERS, key -> 0L);
		assertEquals(1, result.requirements().size());
		assertEquals(Set.of("Wither T8"), result.unknownEssences());
	}

	@Test
	void stockAboveTotalMeansNothingMissing() {
		var result = EssenceFarmNeeds.compute(List.of(range(2, 3)), COSTS::get, TIERS, key -> 99999L);
		assertEquals(0, result.needs().get(0).amount());
	}

	@Test
	void targetAtOrBelowCurrentPlansNothing() {
		var result = EssenceFarmNeeds.compute(List.of(range(4, 4), range(4, 2)), COSTS::get, TIERS, key -> 0L);
		assertEquals(0, result.requirements().size());
	}

	private static EssenceFarmNeeds.LevelRange range(int current, int target) {
		return new EssenceFarmNeeds.LevelRange(current, target);
	}

	@Test
	void formatsDurations() {
		assertEquals("", EssenceFarmNeeds.formatDuration(0));
		assertEquals("45m", EssenceFarmNeeds.formatDuration(45 * 60_000L));
		assertEquals("1h 20m", EssenceFarmNeeds.formatDuration(80 * 60_000L));
		assertEquals("2d 3h", EssenceFarmNeeds.formatDuration(51 * 3_600_000L));
	}
}

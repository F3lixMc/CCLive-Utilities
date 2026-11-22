package net.felix.utilities.Farmworld.collections.tracker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoneBossBarGateTest {
	@Test
	void previousZoneCountDoesNotUnlockTheNewZone() {
		ZoneBossBarGate gate = new ZoneBossBarGate();
		gate.onZoneChanged(25_000);

		assertEquals(ZoneBossBarGate.Decision.IGNORE, gate.decide(25_000));
		assertEquals(ZoneBossBarGate.Decision.TRACK_ONLY, gate.decide(5_520));
		assertEquals(ZoneBossBarGate.Decision.TRACK_AND_UNLOCK, gate.decide(5_520));
		assertEquals(ZoneBossBarGate.Decision.IGNORE, gate.decide(25_000));
	}

	@Test
	void nextDifferentCountUnlocksAfterTheFirstFreshSample() {
		ZoneBossBarGate gate = new ZoneBossBarGate();
		gate.onZoneChanged(25_000);

		assertEquals(ZoneBossBarGate.Decision.TRACK_ONLY, gate.decide(12_000));
		assertEquals(ZoneBossBarGate.Decision.TRACK_AND_UNLOCK, gate.decide(5_520));
	}

	@Test
	void sameZoneKeepsMarkingLaterLevels() {
		ZoneBossBarGate gate = new ZoneBossBarGate();

		assertEquals(ZoneBossBarGate.Decision.TRACK_AND_UNLOCK, gate.decide(5_520));
		assertEquals(ZoneBossBarGate.Decision.TRACK_AND_UNLOCK, gate.decide(10_000));
	}
}

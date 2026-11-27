package net.felix.utilities.Overall.starforged;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarforgedAnimationTest {

	@Test
	void detectsStarforgedSmithingMessage() {
		assertTrue(StarforgedAnimationUtility.isStarforgedMessage(
				"[Legend] Schmiedezustand erhalten: [Sternengeschmiedet]"));
		assertTrue(StarforgedAnimationUtility.isStarforgedMessage(
				"§r[Legend] Schmiedezustand erhalten: [Sterngeschmiedet] Schwert"));
		assertTrue(StarforgedAnimationUtility.isStarforgedMessage(
				"[Legend] Schmiedezustand erhalten: [Starforged]"));
	}

	@Test
	void ignoresOtherStatesAndPlayerChat() {
		assertFalse(StarforgedAnimationUtility.isStarforgedMessage(
				"[Legend] Schmiedezustand erhalten: [Frostgeschmiedet]"));
		assertFalse(StarforgedAnimationUtility.isStarforgedMessage(
				"Spieler >> [Legend] Schmiedezustand erhalten: [Sternengeschmiedet]"));
		assertFalse(StarforgedAnimationUtility.isStarforgedMessage("Sternengeschmiedet"));
		assertFalse(StarforgedAnimationUtility.isStarforgedMessage(null));
	}

	@Test
	void hsvToRgbHitsPrimaryColorsAndWraps() {
		assertEquals(0xFF0000, StarforgedAnimationMath.hsvToRgb(0f, 1f, 1f));
		assertEquals(0x00FF00, StarforgedAnimationMath.hsvToRgb(1f / 3f, 1f, 1f));
		assertEquals(0x0000FF, StarforgedAnimationMath.hsvToRgb(2f / 3f, 1f, 1f));
		assertEquals(0xFF0000, StarforgedAnimationMath.hsvToRgb(1f, 1f, 1f));
		assertEquals(0xFF0000, StarforgedAnimationMath.hsvToRgb(-1f, 1f, 1f));
		assertEquals(0xFFFFFF, StarforgedAnimationMath.hsvToRgb(0.4f, 0f, 1f));
	}

	@Test
	void pulseStartsFullAndDecays() {
		assertEquals(0f, StarforgedAnimationMath.pulse(99f, 100f, 200f));
		assertEquals(1f, StarforgedAnimationMath.pulse(100f, 100f, 200f));
		assertEquals(0f, StarforgedAnimationMath.pulse(300f, 100f, 200f));
		assertTrue(StarforgedAnimationMath.pulse(150f, 100f, 200f) < 1f);
	}

	@Test
	void strikesAreAscendingAndBeforeImpact() {
		float previous = 0f;
		for (float strike : StarforgedOverlay.STRIKES) {
			assertTrue(strike > previous);
			assertTrue(strike < StarforgedOverlay.IMPACT);
			previous = strike;
		}
		assertTrue(StarforgedOverlay.TOTAL_MS > StarforgedOverlay.IMPACT);
	}
}

package net.felix.utilities.Factory.essencefarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EssenceFarmNumberControllerTest {

	@Test
	void wholeNumbersAcceptPartialInputAndRejectAnythingAboveTheMaximum() {
		assertEquals(0, EssenceFarmNumberController.parseWhole("", 1000));
		assertEquals(80, EssenceFarmNumberController.parseWhole("80", 1000));
		assertEquals(1000, EssenceFarmNumberController.parseWhole("1000", 1000));
		assertEquals(-1, EssenceFarmNumberController.parseWhole("1001", 1000));
		assertEquals(-1, EssenceFarmNumberController.parseWhole("12a", 1000));
		assertTrue(EssenceFarmNumberController.isWholeText("", 1000));
		assertTrue(EssenceFarmNumberController.isWholeText("57", 1000));
		assertFalse(EssenceFarmNumberController.isWholeText("1001", 1000));
	}

	@Test
	void decimalsAcceptCommaAndPointWithoutGrouping() {
		assertEquals(0, EssenceFarmNumberController.parseDecimal("", 1_000_000), 0);
		assertEquals(409, EssenceFarmNumberController.parseDecimal("409", 1_000_000), 0);
		assertEquals(409.5, EssenceFarmNumberController.parseDecimal("409,5", 1_000_000), 1e-9);
		assertEquals(409.5, EssenceFarmNumberController.parseDecimal("409.5", 1_000_000), 1e-9);
		assertEquals(409, EssenceFarmNumberController.parseDecimal("409,", 1_000_000), 0);
		assertEquals(4.095, EssenceFarmNumberController.parseDecimal("4.095", 1_000_000), 1e-9);
		assertTrue(EssenceFarmNumberController.isDecimalText("409,", 1_000_000));
		assertFalse(EssenceFarmNumberController.isDecimalText("4,0,9", 1_000_000));
		assertEquals("0", EssenceFarmNumberController.formatDropChance(-1));
		assertEquals("409", EssenceFarmNumberController.formatDropChance(409));
		assertEquals("409,5", EssenceFarmNumberController.formatDropChance(409.5));
	}

	@Test
	void typedTargetStaysEvenWhenItIsNotAboveTheCurrentLevel() {
		try {
			EssenceFarmCrystals.setTarget("Zahlentest", 40);
			EssenceFarmCrystals.Crystal crystal = EssenceFarmCrystals.get("Zahlentest");
			crystal.level = 57;
			assertEquals(40, crystal.target);
			assertEquals(0, crystal.effectiveTarget());
			EssenceFarmCrystals.setTarget("Zahlentest", 80);
			assertEquals(80, crystal.target);
			assertEquals(80, crystal.effectiveTarget());
		} finally {
			EssenceFarmCrystals.forget("Zahlentest");
		}
	}
}

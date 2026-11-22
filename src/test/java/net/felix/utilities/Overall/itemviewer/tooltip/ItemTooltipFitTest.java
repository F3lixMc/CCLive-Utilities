package net.felix.utilities.Overall.itemviewer.tooltip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTooltipFitTest {
	@Test
	void fittingTooltipKeepsFullSize() {
		ItemTooltipFit.Placement placement = ItemTooltipFit.place(80, 40, 400, 300, 20, 20, 2);
		assertTrue(placement.vanilla());
		assertEquals(1f, placement.scale());
	}

	@Test
	void tallTooltipShrinksToTheScreenMargin() {
		ItemTooltipFit.Placement placement = ItemTooltipFit.place(40, 400, 200, 150, 100, 80, 1);
		assertTrue(placement.scale() < 1f);
		assertTrue(placement.outerY() >= ItemTooltipFit.MARGIN);
		assertTrue(placement.outerY() + placement.outerHeight() <= 150 - ItemTooltipFit.MARGIN);
		assertTrue(placement.outerX() >= ItemTooltipFit.MARGIN);
		assertTrue(placement.outerX() + placement.outerWidth() <= 200 - ItemTooltipFit.MARGIN);
	}

	@Test
	void wideTooltipUsesTheWholeScreenRatherThanTheGapBesideTheCursor() {
		ItemTooltipFit.Placement placement = ItemTooltipFit.place(500, 20, 200, 200, 190, 20, 1);
		assertTrue(placement.scale() < 1f);
		assertTrue(placement.outerWidth() <= 200 - ItemTooltipFit.MARGIN * 2);
		assertTrue(placement.outerX() >= ItemTooltipFit.MARGIN);
	}

	@Test
	void crispTextFillsTheFrame() {
		ItemTooltipFit.Placement placement = ItemTooltipFit.place(40, 176, 400, 190, 10, 10, 2);
		assertEquals(0.5f, placement.scale());
		assertEquals(Math.round(176 * 0.5f) + ItemTooltipFit.BACKGROUND_OUTSET * 2, placement.outerHeight());
		assertEquals(Math.round(40 * 0.5f) + ItemTooltipFit.BACKGROUND_OUTSET * 2, placement.outerWidth());
		assertTrue(placement.outerY() + placement.outerHeight() <= 190 - ItemTooltipFit.MARGIN);
	}

	@Test
	void downscaledTooltipKeepsPixelsAndATightFrame() {
		ItemTooltipFit.Placement placement = ItemTooltipFit.place(200, 400, 400, 300, 40, 40, 2);
		assertEquals(0.5f, placement.scale());
		assertEquals(Math.round(400 * 0.5f) + ItemTooltipFit.BACKGROUND_OUTSET * 2, placement.outerHeight());
		assertTrue(placement.outerY() >= ItemTooltipFit.MARGIN);
		assertTrue(placement.outerY() + placement.outerHeight() <= 300 - ItemTooltipFit.MARGIN);
		assertTrue(placement.outerX() + placement.outerWidth() <= 400 - ItemTooltipFit.MARGIN);
	}

	@Test
	void textKeepsTheNeededScaleWhenNoWholePixelFits() {
		assertEquals(0.4f, ItemTooltipFit.screenPixelScale(0.4f, 2), 0.001f);
	}
}

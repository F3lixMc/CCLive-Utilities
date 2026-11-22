package net.felix.utilities.DragOverlay.dock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayScreenFitTest {
	@Test
	void heldEdgeStaysWhenTheSizeChanges() {
		Map<String, OverlayScreenFit.Pin> pins = null;
		OverlayDockRect first = fitFree(300, 50, 400, 300, pins);
		assertEquals(300, first.x());
		assertEquals(350, first.right());
		pins = lastPins(300, 50, 400, 300, pins);

		OverlayDockRect grown = fitFree(300, 120, 400, 300, pins);
		assertEquals(230, grown.x());
		assertEquals(350, grown.right());
		pins = lastPins(300, 120, 400, 300, pins);

		OverlayDockRect wider = fitFree(300, 130, 400, 300, pins);
		assertEquals(220, wider.x());
		assertEquals(350, wider.right());
	}

	@Test
	void bottomEdgeStaysIndependentOfTheHorizontalEdge() {
		Map<String, OverlayScreenFit.Pin> pins = lastPins(20, 40, 200, 100, null, 40, 40);
		OverlayDockRect grown = fitFree(20, 40, 200, 100, pins, 40, 80);
		assertEquals(20, grown.x());
		assertEquals(80, grown.bottom());
	}

	@Test
	void slackKeepsTheHeldEdgeUntilThePreferredSideFitsAgain() {
		Map<String, OverlayScreenFit.Pin> pins = lastPins(300, 120, 400, 300, lastPins(300, 50, 400, 300, null));
		OverlayDockRect stillHeld = fitFree(300, 110, 400, 300, pins);
		assertEquals(350, stillHeld.right());

		OverlayDockRect released = fitFree(300, 40, 400, 300, pins);
		assertEquals(300, released.x());
	}

	@Test
	void overlayLargerThanTheScreenShrinksUntilItFits() {
		OverlayScreenFit.Fit fit = freeFit(10, 10, 250, 80, 100, 100, null);
		OverlayDockRect rect = fit.rects().get("mining");
		assertTrue(fit.factors().get("mining") < 1f);
		assertTrue(rect.x() >= 0);
		assertTrue(rect.y() >= 0);
		assertTrue(rect.right() <= 100);
		assertTrue(rect.bottom() <= 100);
	}

	@Test
	void largerScreenRestoresFullSize() {
		Map<String, OverlayScreenFit.Pin> pins = freeFit(10, 10, 250, 80, 100, 100, null).pins();
		OverlayScreenFit.Fit fit = freeFit(10, 10, 250, 80, 400, 300, pins);
		assertEquals(1f, fit.factors().get("mining"));
		assertEquals(250, fit.rects().get("mining").w());
	}

	@Test
	void dockGroupUsesOneFactorAndKeepsTheGap() {
		Map<String, OverlayDockRect> desired = new LinkedHashMap<>();
		desired.put("mining", new OverlayDockRect(0, 0, 200, 40));
		desired.put("clipboard", new OverlayDockRect(0, 0, 200, 40));
		OverlayDockLink link = new OverlayDockLink("clipboard", "mining", OverlayDockSide.BOTTOM, 0, false);
		OverlayScreenFit.Fit fit = OverlayDockEngine.resolveFitted(desired, List.of(link), null, 100, 100, null);
		assertEquals(fit.factors().get("mining"), fit.factors().get("clipboard"));
		assertTrue(fit.factors().get("mining") < 1f);
		OverlayDockRect anchor = fit.rects().get("mining");
		OverlayDockRect child = fit.rects().get("clipboard");
		assertEquals(anchor.x(), child.x());
		assertEquals(anchor.bottom(), child.y());
		assertTrue(child.bottom() <= 100);
		assertTrue(anchor.right() <= 100);
	}

	@Test
	void singleReportDoesNotMoveTheGroupBeforeEveryoneIsPresent() {
		Map<String, OverlayDockRect> onlyAnchor = new LinkedHashMap<>();
		onlyAnchor.put("mining", new OverlayDockRect(0, 0, 40, 20));
		OverlayDockLink link = new OverlayDockLink("clipboard", "mining", OverlayDockSide.RIGHT, 0, false);
		OverlayDockRect anchor = OverlayDockEngine.resolve(onlyAnchor, List.of(link), null, 200, 200).get("mining");
		assertEquals(0, anchor.x());

		Map<String, OverlayDockRect> both = new LinkedHashMap<>();
		both.put("mining", new OverlayDockRect(0, 0, 40, 20));
		both.put("clipboard", new OverlayDockRect(0, 0, 180, 20));
		OverlayDockRect moved = OverlayDockEngine.resolve(both, List.of(link), null, 200, 200).get("mining");
		assertTrue(moved.x() < 0 || moved.right() <= 200);
		assertEquals(moved.right(), OverlayDockEngine.resolve(both, List.of(link), null, 200, 200).get("clipboard").x());
	}

	private static OverlayDockRect fitFree(int x, int width, int screenW, int screenH,
			Map<String, OverlayScreenFit.Pin> pins) {
		return fitFree(x, width, screenW, screenH, pins, 10, 20);
	}

	private static OverlayDockRect fitFree(int x, int width, int screenW, int screenH,
			Map<String, OverlayScreenFit.Pin> pins, int y, int height) {
		return freeFit(x, y, width, height, screenW, screenH, pins).rects().get("mining");
	}

	private static Map<String, OverlayScreenFit.Pin> lastPins(int x, int width, int screenW, int screenH,
			Map<String, OverlayScreenFit.Pin> pins) {
		return lastPins(x, width, screenW, screenH, pins, 10, 20);
	}

	private static Map<String, OverlayScreenFit.Pin> lastPins(int x, int width, int screenW, int screenH,
			Map<String, OverlayScreenFit.Pin> pins, int y, int height) {
		return freeFit(x, y, width, height, screenW, screenH, pins).pins();
	}

	private static OverlayScreenFit.Fit freeFit(int x, int y, int width, int height, int screenW, int screenH,
			Map<String, OverlayScreenFit.Pin> pins) {
		Map<String, OverlayDockRect> desired = new LinkedHashMap<>();
		desired.put("mining", new OverlayDockRect(x, y, width, height));
		return OverlayDockEngine.resolveFitted(desired, List.of(), null, screenW, screenH, pins);
	}
}

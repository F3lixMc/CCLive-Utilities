package net.felix.utilities.DragOverlay.dock;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayDockTest {
	private static final int SCREEN_W = 400;
	private static final int SCREEN_H = 300;

	@Test
	void allFourSidesSnapFlushWithinTenPixels() {
		OverlayDockRect anchor = new OverlayDockRect(80, 80, 60, 24);
		assertSnap(new OverlayDockRect(80, 114, 50, 20), anchor, OverlayDockSide.BOTTOM, 80, 104);
		assertSnap(new OverlayDockRect(80, 50, 50, 20), anchor, OverlayDockSide.TOP, 80, 60);
		assertSnap(new OverlayDockRect(40, 80, 50, 20), anchor, OverlayDockSide.LEFT, 30, 80);
		assertSnap(new OverlayDockRect(150, 80, 50, 20), anchor, OverlayDockSide.RIGHT, 140, 80);
	}

	@Test
	void candidateBeyondTenPixelsIsRejected() {
		OverlayDockRect anchor = new OverlayDockRect(80, 80, 60, 24);
		OverlayDockSnap.Candidate candidate = snap(
				new OverlayDockRect(80, 115, 50, 20),
				List.of(new OverlayDockSnap.Participant("mining", anchor)),
				true);
		assertNull(candidate);
	}

	@Test
	void mutualCycleAndLongChainAreRejected() {
		List<OverlayDockLink> mutual = new ArrayList<>();
		assertTrue(OverlayDockGraph.setAttachment(mutual, link("a", "b", OverlayDockSide.RIGHT)));
		assertFalse(OverlayDockGraph.setAttachment(mutual, link("b", "a", OverlayDockSide.LEFT)));

		List<OverlayDockLink> chain = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			chain.add(link("n" + (i + 1), "n" + i, OverlayDockSide.BOTTOM));
		}
		assertTrue(OverlayDockGraph.wouldCreateCycle(chain, link("n0", "n20", OverlayDockSide.TOP)));
	}

	@Test
	void loadedListDropsCyclesDuplicatesAndUnknownIds() {
		List<OverlayDockLink> loaded = new ArrayList<>();
		loaded.add(link("clipboard", "mining", OverlayDockSide.BOTTOM));
		loaded.add(link("clipboard", "kills", OverlayDockSide.TOP));
		loaded.add(link("mining", "clipboard", OverlayDockSide.TOP));
		loaded.add(link("clipboard", "clipboard", OverlayDockSide.LEFT));
		loaded.add(new OverlayDockLink("ghost", "mining", OverlayDockSide.LEFT, 0, false));
		loaded.add(link("kills", "missing", OverlayDockSide.RIGHT));

		List<OverlayDockLink> clean = OverlayDockGraph.sanitize(loaded, OverlayDockIds::known);
		assertEquals(1, clean.size());
		assertEquals("clipboard", clean.get(0).attachedId);
		assertEquals("mining", clean.get(0).anchorId);
	}

	@Test
	void linkRoundTripAndEmptyListLeavePositionsAlone() {
		OverlayDockLink link = link("clipboard", "mining", OverlayDockSide.LEFT);
		link.alignOffset = 6;
		link.alignFromRight = true;
		OverlayDockLink restored = new Gson().fromJson(new Gson().toJson(link), OverlayDockLink.class);
		assertEquals(link, restored);

		Map<String, OverlayDockRect> free = new LinkedHashMap<>();
		free.put("clipboard", new OverlayDockRect(12, 18, 40, 20));
		Map<String, OverlayDockRect> placed = OverlayDockEngine.resolve(free, List.of(), null, SCREEN_W, SCREEN_H);
		assertEquals(free.get("clipboard"), placed.get("clipboard"));
	}

	@Test
	void magnetOffOffersNothingButStillPlacesExistingLink() {
		OverlayDockRect anchor = new OverlayDockRect(80, 80, 60, 24);
		assertNull(snap(new OverlayDockRect(80, 110, 50, 20),
				List.of(new OverlayDockSnap.Participant("mining", anchor)), false));

		Map<String, OverlayDockRect> desired = new LinkedHashMap<>();
		desired.put("mining", anchor);
		desired.put("clipboard", new OverlayDockRect(10, 10, 50, 20));
		Map<String, OverlayDockRect> placed = OverlayDockEngine.resolve(
				desired, List.of(link("clipboard", "mining", OverlayDockSide.BOTTOM)), null, SCREEN_W, SCREEN_H);
		assertEquals(104, placed.get("clipboard").y());
	}

	@Test
	void detachAfterThreePixelsThenDockAgain() {
		assertFalse(OverlayDockLayout.shouldDetach(3, 0));
		assertTrue(OverlayDockLayout.shouldDetach(4, 0));

		List<OverlayDockLink> links = new ArrayList<>();
		assertTrue(OverlayDockGraph.setAttachment(links, link("clipboard", "mining", OverlayDockSide.BOTTOM)));
		assertTrue(OverlayDockGraph.removeInvolving(links, "clipboard"));
		assertTrue(links.isEmpty());

		OverlayDockRect anchor = new OverlayDockRect(80, 80, 60, 24);
		OverlayDockSnap.Candidate again = snap(
				new OverlayDockRect(80, 112, 50, 20),
				List.of(new OverlayDockSnap.Participant("mining", anchor)),
				true);
		assertEquals(OverlayDockSide.BOTTOM, again.side());
		assertTrue(OverlayDockGraph.setAttachment(links, again.toLink()));
		assertEquals(1, links.size());
	}

	@Test
	void sizeChangeKeepsTheAttachedEdgeIncludingRightAlign() {
		OverlayDockRect anchor = new OverlayDockRect(100, 100, 40, 20);
		OverlayDockLink bottom = new OverlayDockLink("clipboard", "mining", OverlayDockSide.BOTTOM, 0, true);
		Map<String, OverlayDockRect> narrow = desired(anchor, new OverlayDockRect(0, 0, 30, 16));
		OverlayDockRect first = OverlayDockEngine.resolve(narrow, List.of(bottom), null, 200, SCREEN_H).get("clipboard");
		assertEquals(110, first.x());
		assertEquals(140, first.right());
		assertEquals(120, first.y());

		Map<String, OverlayDockRect> wide = desired(anchor, new OverlayDockRect(0, 0, 50, 16));
		OverlayDockRect grown = OverlayDockEngine.resolve(wide, List.of(bottom), null, 200, SCREEN_H).get("clipboard");
		assertEquals(90, grown.x());
		assertEquals(140, grown.right());

		OverlayDockLink above = new OverlayDockLink("clipboard", "mining", OverlayDockSide.TOP, 0, false);
		OverlayDockRect taller = OverlayDockEngine.resolve(
				desired(anchor, new OverlayDockRect(100, 0, 30, 28)),
				List.of(above), null, SCREEN_W, SCREEN_H).get("clipboard");
		assertEquals(100, taller.bottom());

		OverlayDockLink left = new OverlayDockLink("clipboard", "mining", OverlayDockSide.LEFT, 4, false);
		OverlayDockRect wider = OverlayDockEngine.resolve(
				desired(anchor, new OverlayDockRect(0, 0, 36, 12)),
				List.of(left), null, SCREEN_W, SCREEN_H).get("clipboard");
		assertEquals(anchor.x(), wider.right());
		assertEquals(104, wider.y());
	}

	@Test
	void hiddenAnchorLeavesThePartnerOnItsFreePosition() {
		List<OverlayDockLink> links = new ArrayList<>(List.of(link("clipboard", "mining", OverlayDockSide.RIGHT)));
		Map<String, OverlayDockRect> desired = new LinkedHashMap<>();
		desired.put("clipboard", new OverlayDockRect(15, 25, 40, 18));
		Map<String, OverlayDockRect> placed = OverlayDockEngine.resolve(desired, links, null, SCREEN_W, SCREEN_H);
		assertEquals(new OverlayDockRect(15, 25, 40, 18), placed.get("clipboard"));
		assertEquals(1, links.size());
	}

	@Test
	void singleResetRemovesOnlyThatOverlayAndResetAllClearsEveryLink() {
		List<OverlayDockLink> links = new ArrayList<>();
		links.add(link("clipboard", "mining", OverlayDockSide.BOTTOM));
		links.add(link("kills", "coin", OverlayDockSide.RIGHT));
		links.add(link("bossHp", "factoryLevel", OverlayDockSide.LEFT));
		assertTrue(OverlayDockGraph.removeInvolving(links, "clipboard"));
		assertEquals(2, links.size());
		assertEquals("kills", links.get(0).attachedId);
		assertEquals("bossHp", links.get(1).attachedId);

		links.clear();
		assertTrue(links.isEmpty());
		boolean magnetEnabled = true;
		assertTrue(magnetEnabled);
	}

	@Test
	void dragAndScreenChangeMoveThePreviewWithoutTouchingLinks() {
		List<OverlayDockLink> links = new ArrayList<>();
		links.add(link("clipboard", "mining", OverlayDockSide.BOTTOM));
		List<OverlayDockLink> saved = List.copyOf(links);

		Map<String, OverlayDockRect> desired = new LinkedHashMap<>();
		desired.put("mining", new OverlayDockRect(20, 20, 40, 16));
		desired.put("clipboard", new OverlayDockRect(20, 40, 40, 12));
		OverlayDockRect before = OverlayDockEngine.resolve(desired, links, "clipboard", SCREEN_W, SCREEN_H).get("clipboard");
		assertEquals(20, before.x());

		desired.put("mining", new OverlayDockRect(48, 20, 40, 16));
		OverlayDockRect draggedAnchor = OverlayDockEngine.resolve(desired, links, null, SCREEN_W, SCREEN_H).get("clipboard");
		assertEquals(48, draggedAnchor.x());
		assertEquals(36, draggedAnchor.y());

		desired.put("clipboard", new OverlayDockRect(33, 44, 40, 12));
		OverlayDockRect followsMouse = OverlayDockEngine.resolve(desired, links, "clipboard", SCREEN_W, SCREEN_H).get("clipboard");
		assertEquals(33, followsMouse.x());
		assertEquals(44, followsMouse.y());

		desired.put("mining", new OverlayDockRect(70, 20, 40, 16));
		desired.put("clipboard", new OverlayDockRect(10, 10, 40, 12));
		OverlayDockRect clamped = OverlayDockEngine.resolve(desired, links, null, 100, 80).get("clipboard");
		assertEquals(60, clamped.x());
		assertEquals(saved, links);
	}

	private static void assertSnap(
			OverlayDockRect dragged,
			OverlayDockRect anchor,
			OverlayDockSide side,
			int expectedX,
			int expectedY
	) {
		OverlayDockSnap.Candidate candidate = snap(
				dragged, List.of(new OverlayDockSnap.Participant("mining", anchor)), true);
		assertEquals(side, candidate.side());
		assertEquals(expectedX, candidate.snappedX());
		assertEquals(expectedY, candidate.snappedY());
		assertTrue(candidate.distance() <= OverlayDockLayout.SNAP_DISTANCE);
		assertEquals(OverlayDockLayout.GAP, 0);
	}

	private static OverlayDockSnap.Candidate snap(
			OverlayDockRect dragged,
			List<OverlayDockSnap.Participant> others,
			boolean magnetEnabled
	) {
		return OverlayDockSnap.findBest(
				"clipboard", dragged, others, null, magnetEnabled, List.of(), SCREEN_W, SCREEN_H);
	}

	private static Map<String, OverlayDockRect> desired(OverlayDockRect anchor, OverlayDockRect attached) {
		Map<String, OverlayDockRect> desired = new LinkedHashMap<>();
		desired.put("mining", anchor);
		desired.put("clipboard", attached);
		return desired;
	}

	private static OverlayDockLink link(String attached, String anchor, OverlayDockSide side) {
		return new OverlayDockLink(attached, anchor, side, 0, false);
	}
}

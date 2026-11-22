package net.felix.utilities.DragOverlay.dock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Andock-Angebot. Der endgültige Abstand ist 0, die Suche reicht 10 GUI-Pixel.
 * Bildschirmgröße kommt von außen, damit die Rechnung ohne Client läuft.
 */
public final class OverlayDockSnap {
	static final int MIN_EDGE_OVERLAP = 4;
	static final int HYSTERESIS_PX = 2;

	private OverlayDockSnap() {
	}

	public record Candidate(
			String attachedId,
			String anchorId,
			OverlayDockSide side,
			int snappedX,
			int snappedY,
			int alignOffset,
			boolean alignFromRight,
			int distance,
			int dragIconX,
			int dragIconY,
			int anchorIconX,
			int anchorIconY
	) {
		public String stickyKey() {
			return anchorId + "|" + side.name();
		}

		public OverlayDockLink toLink() {
			return new OverlayDockLink(attachedId, anchorId, side, alignOffset, alignFromRight);
		}
	}

	public record Participant(String id, OverlayDockRect rect) {
	}

	public static Candidate findBest(
			String draggedId,
			OverlayDockRect dragged,
			List<Participant> others,
			Candidate sticky,
			boolean magnetEnabled,
			List<OverlayDockLink> links,
			int screenW,
			int screenH
	) {
		if (!magnetEnabled || draggedId == null || dragged == null) {
			return null;
		}
		List<Candidate> candidates = new ArrayList<>();
		if (others != null) {
			for (Participant other : others) {
				if (other == null || other.id() == null || other.id().equals(draggedId) || other.rect() == null) {
					continue;
				}
				addSideCandidates(candidates, draggedId, dragged, other.id(), other.rect(), others, screenW, screenH);
			}
		}
		if (candidates.isEmpty()) {
			return null;
		}
		candidates.sort(Comparator
				.comparingInt(Candidate::distance)
				.thenComparingInt(candidate -> candidate.side().ordinal())
				.thenComparing(Candidate::anchorId));
		Candidate best = null;
		for (Candidate candidate : candidates) {
			if (OverlayDockGraph.wouldCreateCycle(links, candidate.toLink())) {
				continue;
			}
			best = candidate;
			break;
		}
		if (best == null) {
			return null;
		}
		if (sticky != null) {
			for (Candidate candidate : candidates) {
				if (!Objects.equals(candidate.stickyKey(), sticky.stickyKey())) {
					continue;
				}
				if (OverlayDockGraph.wouldCreateCycle(links, candidate.toLink())) {
					continue;
				}
				if (candidate.distance() <= best.distance() + HYSTERESIS_PX) {
					return candidate;
				}
				break;
			}
		}
		return best;
	}

	/** Nur derselbe Anker und dieselbe Seite bleiben beim Loslassen gültig. */
	public static Candidate revalidate(
			String draggedId,
			OverlayDockRect dragged,
			List<Participant> others,
			Candidate proposed,
			boolean magnetEnabled,
			List<OverlayDockLink> links,
			int screenW,
			int screenH
	) {
		if (proposed == null) {
			return null;
		}
		Candidate fresh = findBest(draggedId, dragged, others, proposed, magnetEnabled, links, screenW, screenH);
		if (fresh == null || !Objects.equals(fresh.stickyKey(), proposed.stickyKey())) {
			return null;
		}
		return fresh;
	}

	private static void addSideCandidates(
			List<Candidate> out,
			String draggedId,
			OverlayDockRect dragged,
			String anchorId,
			OverlayDockRect anchor,
			List<Participant> others,
			int screenW,
			int screenH
	) {
		int gap = OverlayDockLayout.GAP;
		consider(out, draggedId, dragged, anchorId, anchor, OverlayDockSide.BOTTOM, others, screenW, screenH,
				anchor.bottom() + gap, true);
		consider(out, draggedId, dragged, anchorId, anchor, OverlayDockSide.TOP, others, screenW, screenH,
				anchor.y() - gap - dragged.h(), true);
		consider(out, draggedId, dragged, anchorId, anchor, OverlayDockSide.RIGHT, others, screenW, screenH,
				anchor.right() + gap, false);
		consider(out, draggedId, dragged, anchorId, anchor, OverlayDockSide.LEFT, others, screenW, screenH,
				anchor.x() - gap - dragged.w(), false);
	}

	private static void consider(
			List<Candidate> out,
			String draggedId,
			OverlayDockRect dragged,
			String anchorId,
			OverlayDockRect anchor,
			OverlayDockSide side,
			List<Participant> others,
			int screenW,
			int screenH,
			int fixedCoord,
			boolean freeAxisIsX
	) {
		int preferredFree = freeAxisIsX ? dragged.x() : dragged.y();
		int minFree;
		int maxFree;
		if (freeAxisIsX) {
			minFree = anchor.x() + MIN_EDGE_OVERLAP - dragged.w();
			maxFree = anchor.right() - MIN_EDGE_OVERLAP;
		} else {
			minFree = anchor.y() + MIN_EDGE_OVERLAP - dragged.h();
			maxFree = anchor.bottom() - MIN_EDGE_OVERLAP;
		}
		if (minFree > maxFree) {
			minFree = freeAxisIsX ? anchor.x() : anchor.y();
			maxFree = freeAxisIsX ? anchor.right() - dragged.w() : anchor.bottom() - dragged.h();
			if (minFree > maxFree) {
				minFree = maxFree = freeAxisIsX ? anchor.x() : anchor.y();
			}
		}
		int bestFree = Math.max(minFree, Math.min(preferredFree, maxFree));
		OverlayDockRect snapped = makeRect(bestFree, fixedCoord, dragged, freeAxisIsX);
		if (!fitsScreen(snapped, screenW, screenH) || !edgesOverlap(snapped, anchor, side)) {
			return;
		}
		if (overlapsAny(snapped, draggedId, anchorId, others)) {
			Integer slid = findClearFreeCoord(
					dragged, fixedCoord, freeAxisIsX, preferredFree, minFree, maxFree,
					draggedId, anchorId, others, screenW, screenH, side, anchor);
			if (slid == null) {
				return;
			}
			bestFree = slid;
			snapped = makeRect(bestFree, fixedCoord, dragged, freeAxisIsX);
			if (!fitsScreen(snapped, screenW, screenH)
					|| !edgesOverlap(snapped, anchor, side)
					|| overlapsAny(snapped, draggedId, anchorId, others)) {
				return;
			}
		}
		int correction = correctionDistance(dragged, snapped);
		if (correction > OverlayDockLayout.SNAP_DISTANCE) {
			return;
		}
		boolean alignFromRight = freeAxisIsX && OverlayDockEngine.rightOfCenter(snapped.x(), screenW);
		int alignOffset = alignOffsetFor(side, anchor, snapped, alignFromRight);
		int[] icons = iconPoints(dragged, snapped, anchor, side);
		out.add(new Candidate(
				draggedId, anchorId, side,
				snapped.x(), snapped.y(), alignOffset, alignFromRight, correction,
				icons[0], icons[1], icons[2], icons[3]));
	}

	private static OverlayDockRect makeRect(int free, int fixed, OverlayDockRect dragged, boolean freeAxisIsX) {
		return freeAxisIsX
				? new OverlayDockRect(free, fixed, dragged.w(), dragged.h())
				: new OverlayDockRect(fixed, free, dragged.w(), dragged.h());
	}

	private static int correctionDistance(OverlayDockRect from, OverlayDockRect to) {
		return Math.max(Math.abs(to.x() - from.x()), Math.abs(to.y() - from.y()));
	}

	private static int alignOffsetFor(
			OverlayDockSide side,
			OverlayDockRect anchor,
			OverlayDockRect snapped,
			boolean alignFromRight
	) {
		return switch (side) {
			case TOP, BOTTOM -> alignFromRight
					? anchor.right() - snapped.right()
					: snapped.x() - anchor.x();
			case LEFT, RIGHT -> snapped.y() - anchor.y();
		};
	}

	private static Integer findClearFreeCoord(
			OverlayDockRect dragged,
			int fixedCoord,
			boolean freeAxisIsX,
			int preferredFree,
			int minFree,
			int maxFree,
			String draggedId,
			String anchorId,
			List<Participant> others,
			int screenW,
			int screenH,
			OverlayDockSide side,
			OverlayDockRect anchor
	) {
		Integer best = null;
		int bestCorrection = Integer.MAX_VALUE;
		int step = Math.max(1, Math.min(2, (maxFree - minFree) / 64 + 1));
		for (int free = minFree; free <= maxFree; free += step) {
			best = considerFree(best, bestCorrection, free, preferredFree, dragged, fixedCoord, freeAxisIsX,
					draggedId, anchorId, others, screenW, screenH, side, anchor);
			if (best != null) {
				OverlayDockRect trial = makeRect(best, fixedCoord, dragged, freeAxisIsX);
				bestCorrection = correctionDistance(dragged, trial);
			}
		}
		int[] extras = {minFree, maxFree, Math.max(minFree, Math.min(preferredFree, maxFree))};
		for (int free : extras) {
			Integer next = considerFree(best, bestCorrection, free, preferredFree, dragged, fixedCoord, freeAxisIsX,
					draggedId, anchorId, others, screenW, screenH, side, anchor);
			if (next != null && !next.equals(best)) {
				best = next;
				bestCorrection = correctionDistance(dragged, makeRect(best, fixedCoord, dragged, freeAxisIsX));
			} else if (best == null) {
				best = next;
			}
		}
		return best;
	}

	private static Integer considerFree(
			Integer best,
			int bestCorrection,
			int free,
			int preferredFree,
			OverlayDockRect dragged,
			int fixedCoord,
			boolean freeAxisIsX,
			String draggedId,
			String anchorId,
			List<Participant> others,
			int screenW,
			int screenH,
			OverlayDockSide side,
			OverlayDockRect anchor
	) {
		OverlayDockRect trial = makeRect(free, fixedCoord, dragged, freeAxisIsX);
		int correction = correctionDistance(dragged, trial);
		if (correction > OverlayDockLayout.SNAP_DISTANCE) {
			return best;
		}
		if (!fitsScreen(trial, screenW, screenH)
				|| !edgesOverlap(trial, anchor, side)
				|| overlapsAny(trial, draggedId, anchorId, others)) {
			return best;
		}
		if (correction < bestCorrection
				|| (correction == bestCorrection && best != null
				&& Math.abs(free - preferredFree) < Math.abs(best - preferredFree))) {
			return free;
		}
		return best;
	}

	private static boolean fitsScreen(OverlayDockRect snapped, int screenW, int screenH) {
		return snapped.x() >= -2 && snapped.y() >= -2
				&& snapped.right() <= screenW + 2
				&& snapped.bottom() <= screenH + 2;
	}

	private static boolean overlapsAny(
			OverlayDockRect snapped,
			String draggedId,
			String anchorId,
			List<Participant> others
	) {
		if (others == null) {
			return false;
		}
		for (Participant other : others) {
			if (other == null || other.rect() == null) {
				continue;
			}
			if (other.id().equals(draggedId) || other.id().equals(anchorId)) {
				continue;
			}
			if (snapped.x() < other.rect().right() && snapped.right() > other.rect().x()
					&& snapped.y() < other.rect().bottom() && snapped.bottom() > other.rect().y()) {
				return true;
			}
		}
		return false;
	}

	private static boolean edgesOverlap(OverlayDockRect dragged, OverlayDockRect anchor, OverlayDockSide side) {
		return switch (side) {
			case TOP, BOTTOM -> {
				int start = Math.max(dragged.x(), anchor.x());
				int end = Math.min(dragged.right(), anchor.right());
				yield end - start >= Math.min(MIN_EDGE_OVERLAP, Math.min(dragged.w(), anchor.w()));
			}
			case LEFT, RIGHT -> {
				int start = Math.max(dragged.y(), anchor.y());
				int end = Math.min(dragged.bottom(), anchor.bottom());
				yield end - start >= Math.min(MIN_EDGE_OVERLAP, Math.min(dragged.h(), anchor.h()));
			}
		};
	}

	private static int[] iconPoints(
			OverlayDockRect current,
			OverlayDockRect snapped,
			OverlayDockRect anchor,
			OverlayDockSide side
	) {
		int dragX;
		int dragY;
		int anchorX;
		int anchorY;
		switch (side) {
			case BOTTOM -> {
				int mid = overlapMid(snapped.x(), snapped.right(), anchor.x(), anchor.right());
				dragX = clamp(mid, current.x(), current.right());
				dragY = current.y();
				anchorX = clamp(mid, anchor.x(), anchor.right());
				anchorY = anchor.bottom();
			}
			case TOP -> {
				int mid = overlapMid(snapped.x(), snapped.right(), anchor.x(), anchor.right());
				dragX = clamp(mid, current.x(), current.right());
				dragY = current.bottom();
				anchorX = clamp(mid, anchor.x(), anchor.right());
				anchorY = anchor.y();
			}
			case RIGHT -> {
				int mid = overlapMid(snapped.y(), snapped.bottom(), anchor.y(), anchor.bottom());
				dragX = current.x();
				dragY = clamp(mid, current.y(), current.bottom());
				anchorX = anchor.right();
				anchorY = clamp(mid, anchor.y(), anchor.bottom());
			}
			case LEFT -> {
				int mid = overlapMid(snapped.y(), snapped.bottom(), anchor.y(), anchor.bottom());
				dragX = current.right();
				dragY = clamp(mid, current.y(), current.bottom());
				anchorX = anchor.x();
				anchorY = clamp(mid, anchor.y(), anchor.bottom());
			}
			default -> {
				dragX = current.x();
				dragY = current.y();
				anchorX = anchor.x();
				anchorY = anchor.y();
			}
		}
		return new int[]{dragX, dragY, anchorX, anchorY};
	}

	private static int overlapMid(int a0, int a1, int b0, int b1) {
		int start = Math.max(a0, b0);
		int end = Math.min(a1, b1);
		if (end > start) {
			return (start + end) / 2;
		}
		return (a0 + a1) / 2;
	}

	private static int clamp(int value, int minInclusive, int maxExclusive) {
		if (maxExclusive <= minInclusive) {
			return minInclusive;
		}
		return Math.max(minInclusive, Math.min(value, maxExclusive - 1));
	}
}

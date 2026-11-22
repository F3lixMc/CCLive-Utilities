package net.felix.utilities.DragOverlay.dock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Zyklen und doppelte Anhänger. Die Kette darf beliebig lang sein;
 * eine feste Schrittgrenze gibt es nicht.
 */
public final class OverlayDockGraph {
	private OverlayDockGraph() {
	}

	public static boolean wouldCreateCycle(List<OverlayDockLink> links, OverlayDockLink candidate) {
		if (candidate == null || !candidate.isStructural()) {
			return true;
		}
		Map<String, String> parent = new HashMap<>();
		if (links != null) {
			for (OverlayDockLink link : links) {
				if (link == null || !link.isStructural()) {
					continue;
				}
				if (candidate.attachedId.equals(link.attachedId)) {
					continue;
				}
				parent.putIfAbsent(link.attachedId, link.anchorId);
			}
		}
		Set<String> seen = new HashSet<>();
		String cursor = candidate.anchorId;
		while (cursor != null) {
			if (!seen.add(cursor)) {
				return true;
			}
			if (cursor.equals(candidate.attachedId)) {
				return true;
			}
			cursor = parent.get(cursor);
		}
		return false;
	}

	/**
	 * Entfernt ungültige IDs, Selbstverbindungen, ein zweites Anhängen derselben
	 * {@code attachedId} und jede Kante, die einen Zyklus schließt.
	 */
	public static List<OverlayDockLink> sanitize(List<OverlayDockLink> raw, Predicate<String> known) {
		List<OverlayDockLink> kept = new ArrayList<>();
		if (raw == null) {
			return kept;
		}
		Predicate<String> ids = known == null ? id -> true : known;
		Set<String> seenAttached = new HashSet<>();
		for (OverlayDockLink link : raw) {
			if (link == null || !link.isStructural()) {
				continue;
			}
			if (!ids.test(link.attachedId) || !ids.test(link.anchorId)) {
				continue;
			}
			if (!seenAttached.add(link.attachedId)) {
				continue;
			}
			if (wouldCreateCycle(kept, link)) {
				continue;
			}
			kept.add(link);
		}
		return kept;
	}

	public static boolean removeInvolving(List<OverlayDockLink> links, String overlayId) {
		if (links == null || overlayId == null) {
			return false;
		}
		return links.removeIf(link -> link == null
				|| overlayId.equals(link.attachedId)
				|| overlayId.equals(link.anchorId));
	}

	public static boolean setAttachment(List<OverlayDockLink> links, OverlayDockLink candidate) {
		if (links == null || candidate == null || !candidate.isStructural()) {
			return false;
		}
		if (wouldCreateCycle(links, candidate)) {
			return false;
		}
		links.removeIf(link -> link == null || candidate.attachedId.equals(link.attachedId));
		links.add(new OverlayDockLink(
				candidate.attachedId,
				candidate.anchorId,
				candidate.side,
				candidate.alignOffset,
				candidate.alignFromRight));
		return true;
	}

	public static boolean sameContent(List<OverlayDockLink> left, List<OverlayDockLink> right) {
		List<OverlayDockLink> a = left == null ? List.of() : left;
		List<OverlayDockLink> b = right == null ? List.of() : right;
		return a.equals(b);
	}
}

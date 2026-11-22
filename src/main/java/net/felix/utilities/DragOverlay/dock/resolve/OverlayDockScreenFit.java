package net.felix.utilities.DragOverlay.dock.resolve;

import net.felix.utilities.DragOverlay.dock.model.OverlayDockLink;
import net.felix.utilities.DragOverlay.dock.model.OverlayDockRect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schiebt fertig aufgelöste Overlays in den Bildschirm und verkleinert eine Gruppe nur,
 * wenn sie danach immer noch größer als der Bildschirm ist.
 * Die Wunschrechtecke bleiben die Eingabe. Die festgehaltene Kante liegt im {@link Pin}.
 */
public final class OverlayDockScreenFit {
	public static final int RETURN_SLACK = 4;

	public static final class Pin {
		private boolean holdFarX;
		private boolean holdFarY;
		private boolean seenX;
		private boolean seenY;
		private int heldNearX;
		private int heldFarX;
		private int heldNearY;
		private int heldFarY;

		public Pin copy() {
			Pin copy = new Pin();
			copy.holdFarX = holdFarX;
			copy.holdFarY = holdFarY;
			copy.seenX = seenX;
			copy.seenY = seenY;
			copy.heldNearX = heldNearX;
			copy.heldFarX = heldFarX;
			copy.heldNearY = heldNearY;
			copy.heldFarY = heldFarY;
			return copy;
		}
	}

	public record Fit(Map<String, OverlayDockRect> rects, Map<String, Float> factors, Map<String, Pin> pins) {
	}

	private record Axis(int origin, boolean holdFar, boolean seen, int heldNear, int heldFar) {
	}

	private OverlayDockScreenFit() {
	}

	public static Fit apply(
			Map<String, OverlayDockRect> placed,
			List<OverlayDockLink> links,
			String skipId,
			int screenW,
			int screenH,
			Map<String, Pin> previousPins
	) {
		Map<String, OverlayDockRect> rects = new LinkedHashMap<>();
		if (placed != null) {
			rects.putAll(placed);
		}
		Map<String, Float> factors = new LinkedHashMap<>();
		Map<String, Pin> pins = new LinkedHashMap<>();
		for (String id : rects.keySet()) {
			factors.put(id, 1f);
		}
		Map<String, String> rootOf = roots(rects, links);
		Map<String, List<String>> groups = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : rootOf.entrySet()) {
			groups.computeIfAbsent(entry.getValue(), key -> new ArrayList<>()).add(entry.getKey());
		}
		for (Map.Entry<String, List<String>> group : groups.entrySet()) {
			boolean dragged = skipId != null && group.getValue().contains(skipId);
			Pin previous = previousPins == null ? null : previousPins.get(group.getKey());
			if (dragged) {
				fitDragged(rects, factors, group.getValue(), screenW, screenH);
				if (previous != null) {
					pins.put(group.getKey(), previous.copy());
				}
				continue;
			}
			Pin next = fitGroup(rects, factors, group.getValue(), screenW, screenH, previous);
			pins.put(group.getKey(), next);
		}
		return new Fit(rects, factors, pins);
	}

	private static void fitDragged(Map<String, OverlayDockRect> rects, Map<String, Float> factors,
			List<String> members, int screenW, int screenH) {
		Bounds bounds = bounds(rects, members);
		if (bounds.w <= screenW && bounds.h <= screenH) {
			return;
		}
		float factor = factor(bounds.w, bounds.h, screenW, screenH);
		scale(rects, factors, members, bounds, factor, false, false);
	}

	private static Pin fitGroup(Map<String, OverlayDockRect> rects, Map<String, Float> factors,
			List<String> members, int screenW, int screenH, Pin previous) {
		Bounds bounds = bounds(rects, members);
		Pin prior = previous == null ? new Pin() : previous;
		Axis x = placeAxis(bounds.x, bounds.w, screenW, prior.holdFarX, prior.seenX, prior.heldNearX, prior.heldFarX);
		Axis y = placeAxis(bounds.y, bounds.h, screenH, prior.holdFarY, prior.seenY, prior.heldNearY, prior.heldFarY);
		int dx = x.origin - bounds.x;
		int dy = y.origin - bounds.y;
		if (dx != 0 || dy != 0) {
			for (String id : members) {
				OverlayDockRect rect = rects.get(id);
				rects.put(id, rect.withPos(rect.x() + dx, rect.y() + dy));
			}
			bounds = new Bounds(bounds.x + dx, bounds.y + dy, bounds.w, bounds.h);
		}
		float factor = factor(bounds.w, bounds.h, screenW, screenH);
		if (factor < 0.999f) {
			scale(rects, factors, members, bounds, factor, x.holdFar, y.holdFar);
		}
		Pin next = new Pin();
		next.holdFarX = x.holdFar;
		next.holdFarY = y.holdFar;
		next.seenX = x.seen;
		next.seenY = y.seen;
		next.heldNearX = x.heldNear;
		next.heldFarX = x.heldFar;
		next.heldNearY = y.heldNear;
		next.heldFarY = y.heldFar;
		return next;
	}

	private static Axis placeAxis(int origin, int size, int screen, boolean holdFar, boolean seen,
			int heldNear, int heldFar) {
		int end = origin + size;
		boolean fits = origin >= 0 && end <= screen;
		boolean fitsSlack = origin >= 0 && end <= screen - RETURN_SLACK;
		if (holdFar && seen) {
			if (fitsSlack && size <= screen) {
				return remember(origin, size, false);
			}
			return settle(heldFar - size, size, screen, true, heldFar);
		}
		if (!holdFar && seen && origin < 0 && size <= screen) {
			if (fitsSlack) {
				return remember(origin, size, false);
			}
			return settle(heldNear, size, screen, false, heldNear);
		}
		if (fits && size <= screen) {
			return remember(origin, size, false);
		}
		if (size > screen) {
			if (origin < 0) {
				return settle(0, size, screen, false, 0);
			}
			int far = Math.min(end, screen);
			return settle(far - size, size, screen, true, far);
		}
		if (origin >= 0 && end > screen) {
			int far = seen ? heldFar : Math.min(end, screen);
			return settle(far - size, size, screen, true, far);
		}
		int near = seen ? heldNear : 0;
		return settle(near, size, screen, false, near);
	}

	private static Axis remember(int origin, int size, boolean holdFar) {
		return new Axis(origin, holdFar, true, origin, origin + size);
	}

	/**
	 * Liegt die Kante so, dass die Fläche trotz passender Größe herausragt, wird sie einmal nachgeschoben.
	 * Die neue Kante wird festgehalten, nicht die schon verschobene Ecke als neue Wunschposition.
	 */
	private static Axis settle(int origin, int size, int screen, boolean holdFar, int held) {
		int placed = origin;
		if (size <= screen) {
			if (placed < 0) {
				placed = 0;
			}
			if (placed + size > screen) {
				placed = screen - size;
			}
		}
		int near = holdFar ? placed : held;
		int far = holdFar ? held : placed + size;
		if (size <= screen) {
			near = placed;
			far = placed + size;
		}
		return new Axis(placed, holdFar, true, near, far);
	}

	private static float factor(int width, int height, int screenW, int screenH) {
		if (screenW <= 0 || screenH <= 0) {
			return 1f;
		}
		if (width <= screenW && height <= screenH) {
			return 1f;
		}
		return Math.min(screenW / (float) Math.max(1, width), screenH / (float) Math.max(1, height));
	}

	private static void scale(Map<String, OverlayDockRect> rects, Map<String, Float> factors, List<String> members,
			Bounds bounds, float factor, boolean holdRight, boolean holdBottom) {
		int anchorX = holdRight ? bounds.x + bounds.w : bounds.x;
		int anchorY = holdBottom ? bounds.y + bounds.h : bounds.y;
		for (String id : members) {
			OverlayDockRect rect = rects.get(id);
			int x = holdRight
					? anchorX - Math.round((anchorX - rect.x()) * factor)
					: anchorX + Math.round((rect.x() - anchorX) * factor);
			int y = holdBottom
					? anchorY - Math.round((anchorY - rect.y()) * factor)
					: anchorY + Math.round((rect.y() - anchorY) * factor);
			int w = Math.max(1, Math.round(rect.w() * factor));
			int h = Math.max(1, Math.round(rect.h() * factor));
			rects.put(id, new OverlayDockRect(x, y, w, h));
			factors.put(id, factor);
		}
	}

	private record Bounds(int x, int y, int w, int h) {
	}

	private static Bounds bounds(Map<String, OverlayDockRect> rects, List<String> members) {
		int left = Integer.MAX_VALUE;
		int top = Integer.MAX_VALUE;
		int right = Integer.MIN_VALUE;
		int bottom = Integer.MIN_VALUE;
		for (String id : members) {
			OverlayDockRect rect = rects.get(id);
			left = Math.min(left, rect.x());
			top = Math.min(top, rect.y());
			right = Math.max(right, rect.right());
			bottom = Math.max(bottom, rect.bottom());
		}
		return new Bounds(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
	}

	private static Map<String, String> roots(Map<String, OverlayDockRect> rects, List<OverlayDockLink> links) {
		Map<String, String> parent = new LinkedHashMap<>();
		for (String id : rects.keySet()) {
			parent.put(id, id);
		}
		if (links != null) {
			for (OverlayDockLink link : links) {
				if (link == null || !link.isStructural()) {
					continue;
				}
				if (!parent.containsKey(link.attachedId) || !parent.containsKey(link.anchorId)) {
					continue;
				}
				union(parent, link.attachedId, link.anchorId);
			}
		}
		Map<String, String> rootOf = new LinkedHashMap<>();
		for (String id : parent.keySet()) {
			rootOf.put(id, find(parent, id));
		}
		return rootOf;
	}

	private static void union(Map<String, String> parent, String left, String right) {
		String a = find(parent, left);
		String b = find(parent, right);
		if (!a.equals(b)) {
			if (a.compareTo(b) < 0) {
				parent.put(b, a);
			} else {
				parent.put(a, b);
			}
		}
	}

	private static String find(Map<String, String> parent, String id) {
		String root = parent.get(id);
		if (root == null || root.equals(id)) {
			return id;
		}
		String found = find(parent, root);
		parent.put(id, found);
		return found;
	}
}

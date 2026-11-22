package net.felix.utilities.DragOverlay.dock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reine Platzierung. Schreibt keine Config und liest keinen Minecraft-Client.
 * Ein Anker, der nicht in {@code desired} steht, wird nicht angewendet:
 * der Anhänger bleibt auf seiner freien Position.
 */
public final class OverlayDockEngine {
	private OverlayDockEngine() {
	}

	public static Map<String, OverlayDockRect> resolve(
			Map<String, OverlayDockRect> desired,
			List<OverlayDockLink> links,
			String skipId,
			int screenW,
			int screenH
	) {
		return resolveFitted(desired, links, skipId, screenW, screenH, null).rects();
	}

	/**
	 * Zuerst Verbindungen, danach eine gemeinsame Verschiebung und nur bei Bedarf eine gemeinsame Skalierung.
	 * {@code pins} wird nicht verändert. Die zurückgegebenen Pins gehören zum nächsten Bild.
	 */
	public static OverlayScreenFit.Fit resolveFitted(
			Map<String, OverlayDockRect> desired,
			List<OverlayDockLink> links,
			String skipId,
			int screenW,
			int screenH,
			Map<String, OverlayScreenFit.Pin> pins
	) {
		Map<String, OverlayDockRect> placed = new LinkedHashMap<>();
		if (desired != null) {
			placed.putAll(desired);
		}
		for (OverlayDockLink link : topological(links)) {
			if (skipId != null && skipId.equals(link.attachedId)) {
				continue;
			}
			OverlayDockRect anchor = placed.get(link.anchorId);
			OverlayDockRect attached = placed.get(link.attachedId);
			if (anchor == null || attached == null) {
				continue;
			}
			placed.put(link.attachedId, placeAttached(anchor, attached, link));
		}
		return OverlayScreenFit.apply(placed, links, skipId, screenW, screenH, pins);
	}

	public static OverlayDockRect placeAttached(OverlayDockRect anchor, OverlayDockRect attached, OverlayDockLink link) {
		int x = attached.x();
		int y = attached.y();
		switch (link.side) {
			case BOTTOM -> {
				y = anchor.bottom() + OverlayDockLayout.GAP;
				x = horizontalAlongEdge(anchor, attached, link);
			}
			case TOP -> {
				y = anchor.y() - OverlayDockLayout.GAP - attached.h();
				x = horizontalAlongEdge(anchor, attached, link);
			}
			case RIGHT -> {
				x = anchor.right() + OverlayDockLayout.GAP;
				y = anchor.y() + link.alignOffset;
			}
			case LEFT -> {
				x = anchor.x() - OverlayDockLayout.GAP - attached.w();
				y = anchor.y() + link.alignOffset;
			}
		}
		return attached.withPos(x, y);
	}

	/**
	 * Live-Größe an die Editor-Vorschau legen, ohne die Vorschau selbst zu verändern.
	 * Links der Mitte teilen sich die linke obere Ecke, rechts die rechte obere.
	 * Eine Dock-Kante teilt sich diese Kante.
	 */
	public static OverlayDockRect liveAlignedToPreview(
			OverlayDockRect preview,
			int liveW,
			int liveH,
			boolean rightOfCenter,
			OverlayDockLink link,
			boolean skipped
	) {
		if (preview == null) {
			return new OverlayDockRect(0, 0, liveW, liveH);
		}
		if (link != null && !skipped) {
			return switch (link.side) {
				case BOTTOM -> new OverlayDockRect(sharedCornerX(preview, liveW, rightOfCenter), preview.y(), liveW, liveH);
				case TOP -> new OverlayDockRect(
						sharedCornerX(preview, liveW, rightOfCenter),
						preview.y() + preview.h() - liveH,
						liveW, liveH);
				case RIGHT -> new OverlayDockRect(preview.x(), preview.y(), liveW, liveH);
				case LEFT -> new OverlayDockRect(preview.x() + preview.w() - liveW, preview.y(), liveW, liveH);
			};
		}
		return new OverlayDockRect(sharedCornerX(preview, liveW, rightOfCenter), preview.y(), liveW, liveH);
	}

	public static int sharedCornerX(OverlayDockRect preview, int liveW, boolean rightOfCenter) {
		if (rightOfCenter) {
			return preview.x() + preview.w() - liveW;
		}
		return preview.x();
	}

	public static boolean rightOfCenter(int left, int screenW) {
		return left >= screenW / 2;
	}

	private static int horizontalAlongEdge(OverlayDockRect anchor, OverlayDockRect attached, OverlayDockLink link) {
		if (link.alignFromRight) {
			return anchor.right() - link.alignOffset - attached.w();
		}
		return anchor.x() + link.alignOffset;
	}

	public static OverlayDockRect clamp(OverlayDockRect rect, int screenW, int screenH) {
		int x = rect.x();
		int y = rect.y();
		if (rect.w() < screenW) {
			x = Math.max(0, Math.min(x, screenW - rect.w()));
		} else if (screenW > 0) {
			x = 0;
		}
		if (rect.h() < screenH) {
			y = Math.max(0, Math.min(y, screenH - rect.h()));
		} else if (screenH > 0) {
			y = 0;
		}
		return rect.withPos(x, y);
	}

	private static List<OverlayDockLink> topological(List<OverlayDockLink> links) {
		Map<String, OverlayDockLink> byAttached = new HashMap<>();
		if (links != null) {
			for (OverlayDockLink link : links) {
				if (link != null && link.isStructural()) {
					byAttached.putIfAbsent(link.attachedId, link);
				}
			}
		}
		List<OverlayDockLink> ordered = new ArrayList<>();
		Set<String> visiting = new HashSet<>();
		Set<String> done = new HashSet<>();
		for (String id : byAttached.keySet()) {
			visit(id, byAttached, visiting, done, ordered);
		}
		return ordered;
	}

	private static void visit(
			String id,
			Map<String, OverlayDockLink> byAttached,
			Set<String> visiting,
			Set<String> done,
			List<OverlayDockLink> ordered
	) {
		if (done.contains(id) || visiting.contains(id)) {
			return;
		}
		OverlayDockLink link = byAttached.get(id);
		if (link == null) {
			done.add(id);
			return;
		}
		visiting.add(id);
		visit(link.anchorId, byAttached, visiting, done, ordered);
		visiting.remove(id);
		done.add(id);
		ordered.add(link);
	}
}

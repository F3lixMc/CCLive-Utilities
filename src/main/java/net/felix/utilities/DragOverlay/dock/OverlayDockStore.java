package net.felix.utilities.DragOverlay.dock;

import net.felix.utilities.DragOverlay.dock.model.OverlayDockIds;
import net.felix.utilities.DragOverlay.dock.model.OverlayDockLink;
import net.felix.utilities.DragOverlay.dock.resolve.OverlayDockGraph;

import java.util.ArrayList;
import java.util.List;
import net.felix.CCLiveUtilitiesConfig;

/** Gespeicherte Verbindungen. Live-Positionen schreibt diese Klasse nicht. */
public final class OverlayDockStore {
	private OverlayDockStore() {
	}

	public static boolean isMagnetEnabled() {
		return CCLiveUtilitiesConfig.HANDLER.instance().overlayMagnetEnabled;
	}

	public static void setMagnetEnabled(boolean enabled) {
		CCLiveUtilitiesConfig.HANDLER.instance().overlayMagnetEnabled = enabled;
		OverlayDockLayout.invalidatePositions();
	}

	public static List<OverlayDockLink> links() {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (config.overlayDockLinks == null) {
			config.overlayDockLinks = new ArrayList<>();
		}
		List<OverlayDockLink> clean = OverlayDockGraph.sanitize(config.overlayDockLinks, OverlayDockIds::known);
		if (!OverlayDockGraph.sameContent(clean, config.overlayDockLinks)) {
			config.overlayDockLinks = new ArrayList<>(clean);
			OverlayDockLayout.invalidatePositions();
		}
		return List.copyOf(config.overlayDockLinks);
	}

	/** true, wenn die geladene Liste bereinigt wurde und gespeichert werden soll. */
	public static boolean sanitizePersisted() {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (config.overlayDockLinks == null) {
			config.overlayDockLinks = new ArrayList<>();
			return true;
		}
		List<OverlayDockLink> clean = OverlayDockGraph.sanitize(config.overlayDockLinks, OverlayDockIds::known);
		if (OverlayDockGraph.sameContent(clean, config.overlayDockLinks)) {
			return false;
		}
		config.overlayDockLinks = new ArrayList<>(clean);
		OverlayDockLayout.invalidatePositions();
		return true;
	}

	public static OverlayDockLink findAttachment(String attachedId) {
		if (attachedId == null) {
			return null;
		}
		for (OverlayDockLink link : links()) {
			if (attachedId.equals(link.attachedId)) {
				return link;
			}
		}
		return null;
	}

	public static boolean involves(String overlayId) {
		if (overlayId == null) {
			return false;
		}
		for (OverlayDockLink link : links()) {
			if (overlayId.equals(link.attachedId) || overlayId.equals(link.anchorId)) {
				return true;
			}
		}
		return false;
	}

	public static boolean removeLinksInvolving(String overlayId) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (config.overlayDockLinks == null) {
			config.overlayDockLinks = new ArrayList<>();
			return false;
		}
		boolean removed = OverlayDockGraph.removeInvolving(config.overlayDockLinks, overlayId);
		if (removed) {
			OverlayDockLayout.invalidatePositions();
		}
		return removed;
	}

	public static void clearAll() {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (config.overlayDockLinks == null) {
			config.overlayDockLinks = new ArrayList<>();
		} else {
			config.overlayDockLinks.clear();
		}
		OverlayDockLayout.invalidatePositions();
	}

	public static boolean setAttachment(OverlayDockLink candidate) {
		if (candidate == null || !candidate.isStructural() || !OverlayDockIds.known(candidate.attachedId)
				|| !OverlayDockIds.known(candidate.anchorId)) {
			return false;
		}
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (config.overlayDockLinks == null) {
			config.overlayDockLinks = new ArrayList<>();
		}
		List<OverlayDockLink> clean = OverlayDockGraph.sanitize(config.overlayDockLinks, OverlayDockIds::known);
		if (!OverlayDockGraph.setAttachment(clean, candidate)) {
			return false;
		}
		config.overlayDockLinks = new ArrayList<>(clean);
		OverlayDockLayout.invalidatePositions();
		return true;
	}
}

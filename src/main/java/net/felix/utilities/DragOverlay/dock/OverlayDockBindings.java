package net.felix.utilities.DragOverlay.dock;

import net.felix.utilities.DragOverlay.dock.model.OverlayDockIds;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Farmworld.collections.tracker.CollectionTracker;

/** Sichtbarkeit, die nicht aus der Config-Schalterstellung allein folgt. */
public final class OverlayDockBindings {
	private OverlayDockBindings() {
	}

	public static void register() {
		OverlayDockLayout.bindRuntimeDrawn(OverlayDockIds.COLLECTION, OverlayDockBindings::collectionDrawn);
		OverlayDockLayout.bindRuntimeDrawn(OverlayDockIds.MINING, OverlayDockBindings::miningDrawn);
	}

	private static boolean collectionDrawn() {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return config.showCollectionOverlay && CollectionTracker.isBiomDetected();
	}

	private static boolean miningDrawn() {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		boolean enabled = config.miningOverlayEnabled && (config.showMiningOverlay || config.showLumberjackOverlay);
		return enabled && CollectionTracker.isBiomDetected();
	}
}

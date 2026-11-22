package net.felix.utilities.DragOverlay.dock;

import net.felix.utilities.Aincraft.blueprints.viewer.BPViewerUtility;
import net.felix.utilities.Aincraft.cardsstatues.overlay.CardsStatuesUtility;
import net.felix.utilities.Aincraft.cointracker.CoinTrackerUtility;
import net.felix.utilities.Aincraft.information.InformationFeature;
import net.felix.utilities.Aincraft.kills.KillsUtility;
import net.felix.utilities.Aincraft.materials.tracker.MaterialTrackerUtility;
import net.felix.utilities.Factory.bosshp.BossHPUtility;
import net.felix.utilities.Factory.essencefarm.EssenceFarmTracker;
import net.felix.utilities.Factory.factorylevel.FactoryLevelTracker;
import net.felix.utilities.Factory.mklevel.overlay.MkLevelOverlayRenderer;
import net.felix.utilities.Overall.aspekte.overlay.AspectOverlay;
import net.felix.utilities.Overall.clipboard.overlay.ClipboardDraggableOverlay;
import net.felix.utilities.Overall.npcalerts.NpcAlertsFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Meldet die aktuellen Größen aller gerade sichtbaren Overlays, ohne sie zu zeichnen.
 * Ausgeblendete Overlays werden ausgelassen und nicht abgewartet.
 */
public final class OverlayDockMeasures {
	private OverlayDockMeasures() {
	}

	public static void collect() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		InformationFeature.contributeLayout(client);
		CoinTrackerUtility.contributeLayout(client);
		KillsUtility.contributeLayout(client);
		MaterialTrackerUtility.contributeLayout(client);
		CardsStatuesUtility.contributeLayout(client);
		BossHPUtility.contributeLayout(client);
		FactoryLevelTracker.contributeLayout(client);
		EssenceFarmTracker.contributeLayout(client);
		BPViewerUtility.contributeLayout(client);
		NpcAlertsFeature.contributeLayout(client);
		if (client.gui.screen() == null || client.gui.screen() instanceof AbstractContainerScreen<?>) {
			ClipboardDraggableOverlay.contributeLayout(client);
		}
		AspectOverlay.contributeLayout(client);
		if (client.gui.screen() instanceof AbstractContainerScreen<?> screen) {
			MkLevelOverlayRenderer.contributeLayout(client, screen);
		}
	}
}

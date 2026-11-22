package net.felix.utilities.Aincraft.information;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.blueprints.data.BlueprintDatabase;
import net.felix.utilities.Aincraft.cardsstatues.data.CardsStatuesDatabase;
import net.felix.utilities.Aincraft.information.essences.EssenceDatabase;
import net.felix.utilities.Aincraft.information.gadgets.GadgetDatabase;
import net.felix.utilities.Aincraft.information.licenses.LicenseDatabase;
import net.felix.utilities.Aincraft.information.tooltip.InformationTooltipHandler;
import net.felix.utilities.Aincraft.materials.data.MaterialDatabase;
import net.felix.utilities.Factory.mklevel.data.MkLevelDatabase;
import net.felix.utilities.Factory.mklevel.overlay.MkLevelInputHandler;
import net.felix.utilities.Farmworld.collections.data.CollectionDatabase;
import net.felix.utilities.Farmworld.collections.overlay.CollectionOverlay;
import net.felix.utilities.Farmworld.collections.tracker.CollectionTracker;
import net.felix.utilities.Farmworld.farmzone.FarmzoneResourceRateUtility;
import net.felix.utilities.Farmworld.farmzone.FarmzoneScoreboard;
import net.felix.utilities.Farmworld.miningxp.overlay.MiningLumberjackOverlay;
import net.felix.utilities.Farmworld.miningxp.tracker.MiningLumberjackTracker;
import net.felix.utilities.Other.hud.HudOverlayVisibility;
import net.felix.utilities.Overall.aspekte.data.AspectDatabase;
import net.felix.utilities.Overall.aspekte.overlay.AspectOverlay;
import net.felix.utilities.Overall.aspekte.overlay.AspectOverlayRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Einstiegspunkt der Informationen-Features: laedt die Datenbanken und registriert
 * Tick-, HUD- und Tooltip-Callbacks. Die eigentliche Logik liegt in den Unterpaketen.
 */
public final class InformationFeature {

	public static boolean isInitialized = false;

	private InformationFeature() {
	}

	public static void initialize() {
		if (isInitialized) {
			return;
		}

		loadDatabases();

		// Register collection hotkeys
		CollectionTracker.registerCollectionHotkeys();

		// Initialize aspect overlay and renderer
		AspectOverlay.initialize();
		AspectOverlayRenderer.initialize();

		// Register client tick event for continuous XP calculation (like KillsUtility)
		ClientTickEvents.END_CLIENT_TICK.register(InformationFeature::onClientTick);

		// Register HUD render callback for mining, lumberjack and collection overlays
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("cclive-utilities", "informationen"), InformationFeature::onHudRender);

		// Register tooltip callback for material information
		ItemTooltipCallback.EVENT.register(InformationTooltipHandler::onItemTooltip);

		isInitialized = true;
	}

	private static void loadDatabases() {
		MaterialDatabase.loadMaterialsDatabase();
		EssenceDatabase.loadEssencesDatabase();
		CardsStatuesDatabase.loadCardsStatuesDatabase();
		AspectDatabase.loadAspectsDatabase();
		LicenseDatabase.loadLicensesDatabase();
		GadgetDatabase.loadGadgetsDatabase();
		BlueprintDatabase.loadBlueprintsDatabase();
		MkLevelDatabase.loadMkLevelDatabase();
		CollectionDatabase.loadCollectionsDatabase();
	}

	/**
	 * Client tick callback for continuous XP calculation (like KillsUtility)
	 */
	public static void onClientTick(Minecraft client) {
		// Performance-Optimierung: Frühe Returns wenn Mod deaktiviert
		if (!CCLiveUtilitiesConfig.isModRuntimeEnabled()) {
			return;
		}

		if (client == null || client.level == null || client.player == null) {
			return;
		}

		CollectionTracker.handleCollectionHotkeys();
		MiningLumberjackTracker.onClientTick(client);
		tickResourceRate(client);
		CollectionTracker.onClientTick(client);
		MkLevelInputHandler.onClientTick(client);
	}

	private static void tickResourceRate(Minecraft client) {
		if (FarmzoneScoreboard.isInFarmzone(client)) {
			FarmzoneResourceRateUtility.tick();
		} else {
			FarmzoneResourceRateUtility.resetSession();
		}
	}

	/**
	 * HUD Render callback for mining and lumberjack overlays
	 */
	public static void contributeLayout(Minecraft client) {
		if (client == null || client.level == null || client.player == null || client.gui == null || client.gui.hud == null) {
			return;
		}
		if (client.gui.hud.isHidden() || !MiningLumberjackTracker.areOverlaysAllowed()) {
			return;
		}
		boolean shouldHideOverlays = isInOwnOrFloorDimension(client);
		if (CCLiveUtilitiesConfig.isModRuntimeEnabled() &&
			CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().showMiningOverlay &&
			MiningLumberjackTracker.isMiningOverlayActive() &&
			!shouldHideOverlays) {
			MiningLumberjackOverlay.renderMiningOverlay(null, client);
		}
		if (CCLiveUtilitiesConfig.isModRuntimeEnabled() &&
			CollectionTracker.isOverlayActive() &&
			!shouldHideOverlays) {
			CollectionOverlay.renderCollectionOverlay(null, client);
		}
		if (CCLiveUtilitiesConfig.isModRuntimeEnabled() &&
			CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().lumberjackOverlayEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().showLumberjackOverlay &&
			MiningLumberjackTracker.isLumberjackOverlayActive() &&
			!shouldHideOverlays) {
			MiningLumberjackOverlay.renderLumberjackOverlay(null, client);
		}
	}

	public static void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null || client.player == null) {
			return;
		}
		if (!HudOverlayVisibility.shouldRenderWorldHudOverlays()) {
			return;
		}

		// Hide overlay if F1 menu (debug screen) is open
		if (client.gui.hud.isHidden()) {
			return;
		}

		// Hide overlays if Tab key is pressed or F1 is toggled
		if (!MiningLumberjackTracker.areOverlaysAllowed()) {
			return;
		}

		// Update XP data from tab list (works even when tab list is closed)
		MiningLumberjackTracker.updateXpFromTabList(client);

		// Update overlay visibility timers (after XP data update)
		MiningLumberjackTracker.refreshOverlayVisibility();

		boolean shouldHideOverlays = isInOwnOrFloorDimension(client);

		// Render mining overlay (only when XP changed recently and not in player's own dimension or floor dimension)
		if (CCLiveUtilitiesConfig.isModRuntimeEnabled() &&
			CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().showMiningOverlay &&
			MiningLumberjackTracker.isMiningOverlayActive() &&
			!shouldHideOverlays) {
			MiningLumberjackOverlay.renderMiningOverlay(context, client);
		}

		// Render collection overlay (only if biom is detected in scoreboard)
		if (CCLiveUtilitiesConfig.isModRuntimeEnabled() &&
			CollectionTracker.isOverlayActive() &&
			!shouldHideOverlays) {
			// Try to read scoreboard when overlay is active
			CollectionTracker.checkCollectionBiomChange(client);
			CollectionOverlay.renderCollectionOverlay(context, client);
		}

		// Render lumberjack overlay (only when XP changed recently and not in player's own dimension or floor dimension)
		if (CCLiveUtilitiesConfig.isModRuntimeEnabled() &&
			CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().lumberjackOverlayEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().showLumberjackOverlay &&
			MiningLumberjackTracker.isLumberjackOverlayActive() &&
			!shouldHideOverlays) {
			MiningLumberjackOverlay.renderLumberjackOverlay(context, client);
		}
	}

	/**
	 * Overlays are hidden in the player's own dimension (dimension name matches player name)
	 * and in dimensions that contain "floor".
	 */
	private static boolean isInOwnOrFloorDimension(Minecraft client) {
		if (client.player == null || client.level == null) {
			return false;
		}
		String playerName = client.player.getName().getString().toLowerCase();
		String dimensionPath = client.level.dimension().identifier().getPath();
		boolean isInPlayerNameDimension = dimensionPath.equals(playerName);
		boolean isInFloorDimension = dimensionPath.toLowerCase().contains("floor");
		return isInPlayerNameDimension || isInFloorDimension;
	}
}

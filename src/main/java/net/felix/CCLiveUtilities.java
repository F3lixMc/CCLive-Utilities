package net.felix;

import java.nio.file.Path;
import java.nio.file.Paths;
import net.fabricmc.api.ModInitializer;
import net.felix.Community.chat.ChatManager;
import net.felix.Community.leaderboards.LeaderboardManager;
import net.felix.Community.leaderboards.reset.PlayerFullReset;
import net.felix.Community.profile.PlayerHoverStatsUtility;
import net.felix.Community.profile.ProfileStatsManager;
import net.felix.commands.CCLiveCommands;
import net.felix.utilities.Aincraft.blueprints.viewer.BPViewerUtility;
import net.felix.utilities.Aincraft.cardsstatues.overlay.CardsStatuesUtility;
import net.felix.utilities.Aincraft.information.InformationFeature;
import net.felix.utilities.Aincraft.kills.DamageTrackingUtility;
import net.felix.utilities.Aincraft.kills.KillAnimationUtility;
import net.felix.utilities.Aincraft.kills.KillsUtility;
import net.felix.utilities.Aincraft.materials.tracker.MaterialTrackerUtility;
import net.felix.utilities.DragOverlay.editor.OverlayEditorUtility;
import net.felix.utilities.Factory.bosshp.BossHPUtility;
import net.felix.utilities.Factory.factorylevel.FactoryLevelTracker;
import net.felix.utilities.Other.update.UpdateCheckerUtility;
import net.felix.utilities.Overall.animationblocker.AnimationBlockerUtility;
import net.felix.utilities.Overall.npcalerts.NpcAlertsFeature;
import net.felix.utilities.Town.equipment.EquipmentDisplayUtility;
import net.felix.utilities.Town.kits.filter.KitFilterUtility;
import net.felix.utilities.Town.schmied.SchmiedTrackerUtility;

public class CCLiveUtilities implements ModInitializer {
	public static final String MOD_ID = "cclive-utilities";



	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.



		// Initialize configuration
		// Migriere Config-Datei von config/ nach config/cclive-utilities/ (falls nötig)
		CCLiveUtilitiesConfig.migrateConfigLocation();
		
		CCLiveUtilitiesConfig.HANDLER.load();
		
		// Migriere Overlay-Typ von String zu Enum
		CCLiveUtilitiesConfig.migrateOverlayType();
		
		// Migriere Config-Felder: Füge fehlende Felder mit Default-Werten hinzu
		CCLiveUtilitiesConfig.migrateConfigFields();

		// Initialisiere ZeichenUtility zuerst (wird von anderen Utilities benötigt)
		net.felix.utilities.Other.glyphs.ZeichenUtility.initialize();

		// Server-Gate: Features/Uploads nur auf PlayLegend (Dev + Bypass-Spieler ausgenommen)
		net.felix.utilities.Other.server.ServerConnectionGate.initialize();
		
		// Initialisiere JSON-Config-Update-Utility (für automatische Updates vom Server)
		net.felix.utilities.Other.config.JsonConfigUpdateUtility.initialize();

		// Initialisiere alle Utility-Klassen
		EquipmentDisplayUtility.initialize();
		SchmiedTrackerUtility.initialize();
		MaterialTrackerUtility.initialize();
		BossHPUtility.initialize();
		FactoryLevelTracker.initialize();
		net.felix.utilities.Factory.essencefarm.EssenceFarmTracker.initialize();
		CardsStatuesUtility.initialize();
		KillsUtility.initialize();
		InformationFeature.initialize();
		AnimationBlockerUtility.initialize();
		KillAnimationUtility.initialize();
		BPViewerUtility.initialize();
		net.felix.utilities.Farmworld.fishing.found.FishingComponentFoundUtility.initialize();
		net.felix.utilities.Farmworld.fishing.found.FishTrapFoundUtility.initialize();
		UpdateCheckerUtility.initialize();
		OverlayEditorUtility.initialize();
		KitFilterUtility.initialize();
		DamageTrackingUtility.initialize();
		net.felix.utilities.Aincraft.cointracker.CoinTrackerUtility.initialize();
		net.felix.utilities.Overall.fragmentdrop.FragmentDropAnimationUtility.initialize();
		NpcAlertsFeature.initialize();
		net.felix.utilities.Other.debug.DebugUtility.initializeItemLogger();
		net.felix.utilities.Overall.clipboard.ClipboardUtility.initialize();
		// Offene Spieler-Resets vor den ersten Fortschritts-Uploads abschließen
		PlayerFullReset.initialize();
		// Initialisiere Leaderboard-System
		LeaderboardManager.getInstance().initialize();
		
		// Initialisiere Chat-System
		try {
			ChatManager.getInstance().initialize();
			net.felix.Community.chat.ChatNameColorTracker.getInstance().initialize();
			net.felix.Community.chat.share.ItemShareManager.getInstance().initialize();
			net.felix.Community.chat.buildshare.BuildShareManager.getInstance().initialize();
		} catch (Exception e) {
			// Silent error handling
		}
		
		// Initialisiere Profile-Stats-System
		try {
			ProfileStatsManager.getInstance().initialize();
		} catch (Exception e) {
			// Silent error handling
		}
		
		// Initialisiere Player Hover Stats Utility
		try {
			PlayerHoverStatsUtility.initialize();
		} catch (Exception e) {
			// Silent error handling
		}
		
		// DevTools (Allowlist / Fabric-Dev; unabhängig von ServerConnectionGate)
		try {
			net.felix.utilities.Other.devtools.DevToolsRegistry.initialize();
			net.felix.utilities.Other.devtools.DevToolsHotkeyDispatcher.initialize();
		} catch (Exception e) {
			// Silent error handling
		}

		try {
			net.felix.utilities.Overall.eventsound.config.EventSoundStore.load();
		} catch (Exception e) {
			// Silent error handling
		}

		// Registriere alle CCLive-Commands (Blueprint + Leaderboard)
		CCLiveCommands.register();
		
		// Initialisiere Player Icon System
		try {
			net.felix.Community.playericon.PlayerIconUtility.initialize();
			net.felix.Community.playericon.PlayerIconNetworking.initialize();
		} catch (Exception e) {
			// Silent error handling
		}

	}

	public static Path getConfigDir() {
		return Paths.get("config");
	}
}
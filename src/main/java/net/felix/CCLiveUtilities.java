package net.felix;

import net.fabricmc.api.ModInitializer;
import net.felix.utilities.Aincraft.CardsStatuesUtility;
import net.felix.utilities.Overall.InformationenUtility;
import net.felix.utilities.Aincraft.BPViewerUtility;
import net.felix.utilities.Aincraft.KillsUtility;
import net.felix.utilities.Aincraft.MaterialTrackerUtility;
import net.felix.utilities.DragOverlay.OverlayEditorUtility;
import net.felix.utilities.Factory.BossHPUtility;
import net.felix.utilities.Other.UpdateCheckerUtility;
import net.felix.utilities.Overall.AnimationBlockerUtility;
import net.felix.utilities.Overall.KillAnimationUtility;
import net.felix.utilities.Overall.DamageTrackingUtility;
import net.felix.utilities.Overall.TabInfo.TabInfoUtility;
import net.felix.utilities.Town.EquipmentDisplayUtility;
import net.felix.utilities.Town.KitFilterUtility;
import net.felix.utilities.Town.SchmiedTrackerUtility;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.commands.CCLiveCommands;
import net.felix.chat.ChatManager;
import net.felix.profile.ProfileStatsManager;
import net.felix.profile.PlayerHoverStatsUtility;

import java.nio.file.Path;
import java.nio.file.Paths;

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

		// Initialisiere ZeichenUtility zuerst (wird von anderen Utilities benötigt)
		net.felix.utilities.Overall.ZeichenUtility.initialize();

		// Initialisiere alle Utility-Klassen
		EquipmentDisplayUtility.initialize();
		SchmiedTrackerUtility.initialize();
		MaterialTrackerUtility.initialize();
		BossHPUtility.initialize();
		CardsStatuesUtility.initialize();
		KillsUtility.initialize();
		InformationenUtility.initialize();
		AnimationBlockerUtility.initialize();
		KillAnimationUtility.initialize();
		BPViewerUtility.initialize();
		UpdateCheckerUtility.initialize();
		OverlayEditorUtility.initialize();
		KitFilterUtility.initialize();
		DamageTrackingUtility.initialize();
		TabInfoUtility.initialize();
		net.felix.utilities.DragOverlay.ClipboardUtility.initialize();
		
		// Initialisiere Leaderboard-System
		LeaderboardManager.getInstance().initialize();
		
		// Initialisiere Chat-System
		try {
			ChatManager.getInstance().initialize();
			System.out.println("✅ ChatManager initialisiert");
		} catch (Exception e) {
			System.err.println("[CCLive-Utilities] Error initializing ChatManager: " + e.getMessage());
			e.printStackTrace();
		}
		
		// Initialisiere Profile-Stats-System
		try {
			ProfileStatsManager.getInstance().initialize();
			System.out.println("✅ ProfileStatsManager initialisiert");
		} catch (Exception e) {
			System.err.println("[CCLive-Utilities] Error initializing ProfileStatsManager: " + e.getMessage());
			e.printStackTrace();
		}
		
		// Initialisiere Player Hover Stats Utility
		try {
			PlayerHoverStatsUtility.initialize();
			System.out.println("✅ PlayerHoverStatsUtility initialisiert");
		} catch (Exception e) {
			System.err.println("[CCLive-Utilities] Error initializing PlayerHoverStatsUtility: " + e.getMessage());
			e.printStackTrace();
		}
		
		// Registriere alle CCLive-Commands (Blueprint + Leaderboard)
		CCLiveCommands.register();
		
		// Initialisiere Player Icon System
		try {
			net.felix.utilities.Other.PlayericonUtility.PlayerIconUtility.initialize();
			net.felix.utilities.Other.PlayericonUtility.PlayerIconNetworking.initialize();
			System.out.println("✅ Player Icon System initialisiert");
		} catch (Exception e) {
			System.err.println("[CCLive-Utilities] Error initializing Player Icon System: " + e.getMessage());
			e.printStackTrace();
		}

	}

	public static Path getConfigDir() {
		return Paths.get("config");
	}
}
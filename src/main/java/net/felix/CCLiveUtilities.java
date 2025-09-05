package net.felix;

import net.fabricmc.api.ModInitializer;
import net.felix.utilities.EquipmentDisplayUtility;
import net.felix.utilities.SchmiedTrackerUtility;
import net.felix.utilities.MaterialTrackerUtility;
import net.felix.utilities.BossHPUtility;
import net.felix.utilities.CardsStatuesUtility;
import net.felix.utilities.KillsUtility;
import net.felix.utilities.InformationenUtility;
import net.felix.utilities.AnimationBlockerUtility;
import net.felix.utilities.BPViewerUtility;
import net.felix.utilities.UpdateCheckerUtility;
import net.felix.utilities.DragOverlay.OverlayEditorUtility;


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
		CCLiveUtilitiesConfig.HANDLER.load();
		
		// Migriere Overlay-Typ von String zu Enum
		CCLiveUtilitiesConfig.migrateOverlayType();

		// Initialisiere alle Utility-Klassen
		EquipmentDisplayUtility.initialize();
		SchmiedTrackerUtility.initialize();
		MaterialTrackerUtility.initialize();
		BossHPUtility.initialize();
		CardsStatuesUtility.initialize();
		KillsUtility.initialize();
		InformationenUtility.initialize();
		AnimationBlockerUtility.initialize();
		BPViewerUtility.initialize();
		UpdateCheckerUtility.initialize();
		OverlayEditorUtility.initialize();

	}

	public static Path getConfigDir() {
		return Paths.get("config");
	}
}
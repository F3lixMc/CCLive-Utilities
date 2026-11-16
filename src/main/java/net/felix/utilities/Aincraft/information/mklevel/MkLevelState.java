package net.felix.utilities.Aincraft.information.mklevel;

import java.util.List;

/**
 * Laufzeit-Zustand des MK-Level-Overlays (Scroll, Suche, aktiver Tab).
 */
public final class MkLevelState {

	private MkLevelState() {
	}

	// MkLevel overlay scroll variables - separate offsets for each tab
	private static int mkLevelScrollOffsetIndividual = 0; // Scroll offset for "Einzelne Wellen"

	private static int mkLevelScrollOffsetCombined = 0; // Scroll offset for "Kombinierte Wellen"

	private static int mkLevelScrollOffsetTotalEssences = 0; // Scroll offset for "Gesamt Essenzen"

	static boolean mkLevelOverlayHovered = false;

	static boolean isInMkLevelInventory = false;

	// MkLevel overlay search variables
	static String mkLevelSearchText = "";

	static boolean mkLevelSearchFocused = false;

	static int mkLevelSearchCursorPosition = 0;

	static long mkLevelSearchCursorBlinkTime = 0;

	static boolean mkLevelSearchCursorVisible = true;

	// MkLevel overlay height cache (updated when overlay is rendered, used by F6 editor)
	static int mkLevelLastKnownHeight = 166; // Default height

	static MkLevelOverlayTab mkLevelActiveTab = MkLevelOverlayTab.INDIVIDUAL;

	static List<TotalEssenceInfo> mkLevelTotalEssencesCache = null;

	// Helper method to get current scroll offset based on active tab
	static int getMkLevelScrollOffset() {
		return switch (mkLevelActiveTab) {
			case INDIVIDUAL -> mkLevelScrollOffsetIndividual;
			case COMBINED -> mkLevelScrollOffsetCombined;
			case TOTAL_ESSENCES -> mkLevelScrollOffsetTotalEssences;
		};
	}

	// Helper method to set current scroll offset based on active tab
	static void setMkLevelScrollOffset(int offset) {
		switch (mkLevelActiveTab) {
			case INDIVIDUAL -> mkLevelScrollOffsetIndividual = offset;
			case COMBINED -> mkLevelScrollOffsetCombined = offset;
			case TOTAL_ESSENCES -> mkLevelScrollOffsetTotalEssences = offset;
		}
	}

	// MkLevel scrollbar dragging state
	static boolean mkLevelScrollbarDragging = false;

	/**
	 * Gets the last known height of the MkLevel overlay (cached from last render)
	 * Used by F6 editor to avoid reflection issues on server
	 */
	public static int getMkLevelLastKnownHeight() {
		return mkLevelLastKnownHeight;
	}
}

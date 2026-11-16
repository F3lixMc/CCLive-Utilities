package net.felix.utilities.Factory.mklevel.data;

import java.util.List;
import net.felix.utilities.Factory.mklevel.overlay.MkLevelOverlayTab;

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

	public static boolean mkLevelOverlayHovered = false;

	public static boolean isInMkLevelInventory = false;

	// MkLevel overlay search variables
	public static String mkLevelSearchText = "";

	public static boolean mkLevelSearchFocused = false;

	public static int mkLevelSearchCursorPosition = 0;

	public static long mkLevelSearchCursorBlinkTime = 0;

	public static boolean mkLevelSearchCursorVisible = true;

	// MkLevel overlay height cache (updated when overlay is rendered, used by F6 editor)
	public static int mkLevelLastKnownHeight = 166; // Default height

	public static MkLevelOverlayTab mkLevelActiveTab = MkLevelOverlayTab.INDIVIDUAL;

	static List<TotalEssenceInfo> mkLevelTotalEssencesCache = null;

	// Helper method to get current scroll offset based on active tab
	public static int getMkLevelScrollOffset() {
		return switch (mkLevelActiveTab) {
			case INDIVIDUAL -> mkLevelScrollOffsetIndividual;
			case COMBINED -> mkLevelScrollOffsetCombined;
			case TOTAL_ESSENCES -> mkLevelScrollOffsetTotalEssences;
		};
	}

	// Helper method to set current scroll offset based on active tab
	public static void setMkLevelScrollOffset(int offset) {
		switch (mkLevelActiveTab) {
			case INDIVIDUAL -> mkLevelScrollOffsetIndividual = offset;
			case COMBINED -> mkLevelScrollOffsetCombined = offset;
			case TOTAL_ESSENCES -> mkLevelScrollOffsetTotalEssences = offset;
		}
	}

	// MkLevel scrollbar dragging state
	public static boolean mkLevelScrollbarDragging = false;

	/**
	 * Gets the last known height of the MkLevel overlay (cached from last render)
	 * Used by F6 editor to avoid reflection issues on server
	 */
	public static int getMkLevelLastKnownHeight() {
		return mkLevelLastKnownHeight;
	}
}

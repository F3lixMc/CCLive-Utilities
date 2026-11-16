package net.felix.features.information.xp;


// Mining & Lumberjack XP Tracking
public class XPData {
	long currentXP = 0;
	long requiredXP = 0;
	int level = 0;
	long initialXP = -1; // XP when tracking started (like initialKills in KillsUtility)
	long newXP = 0; // New XP gained since tracking started (current - initial)
	long sessionStartTime = 0; // When tracking started
	double xpPerMinute = 0.0; // Average XP per minute (calculated continuously)
	long lastXpmUpdateTime = 0;
	long lastXpmNewXp = 0;
	long lastGainedXP = 0; // Last XP gained in a single update (for display)
	long lastXPChangeTime = 0; // When XP last changed (for overlay visibility timer)
	boolean shouldShowOverlay = false; // Whether overlay should be visible
	boolean isInitializedInCurrentDimension = false; // Whether values have been initialized in current dimension
}

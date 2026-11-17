package net.felix.utilities.Farmworld.miningxp.tracker;


// Mining & Lumberjack XP Tracking
public class SkillXpData {
	public long currentXp = 0;
	public long requiredXp = 0;
	public int level = 0;
	public long initialXp = -1; // XP when tracking started (like initialKills in KillsUtility)
	public long newXp = 0; // New XP gained since tracking started (current - initial)
	public long sessionStartTime = 0; // When tracking started
	public double xpPerMinute = 0.0; // Average XP per minute (calculated continuously)
	public long lastXpmUpdateTime = 0;
	public long lastXpmNewXp = 0;
	public long lastGainedXp = 0; // Last XP gained in a single update (for display)
	public long lastXpChangeTime = 0; // When XP last changed (for overlay visibility timer)
	public boolean shouldShowOverlay = false; // Whether overlay should be visible
	public boolean isInitializedInCurrentDimension = false; // Whether values have been initialized in current dimension
}

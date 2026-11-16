package net.felix.utilities.Factory.mklevel.data;


/**
 * Data class to store MkLevel information
 */
public class MkLevelInfo {
	public final int level;
	public final String essence;
	public final int amount;
	
	public MkLevelInfo(int level, String essence, int amount) {
		this.level = level;
		this.essence = essence;
		this.amount = amount;
	}
}

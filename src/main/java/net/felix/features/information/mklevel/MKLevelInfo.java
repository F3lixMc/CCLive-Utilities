package net.felix.features.information.mklevel;


/**
 * Data class to store MKLevel information
 */
public class MKLevelInfo {
	public final int level;
	public final String essence;
	public final int amount;
	
	public MKLevelInfo(int level, String essence, int amount) {
		this.level = level;
		this.essence = essence;
		this.amount = amount;
	}
}

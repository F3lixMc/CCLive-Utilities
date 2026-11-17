package net.felix.utilities.Factory.mklevel.data;


/**
 * Aggregated essence totals across all MK levels
 */
public class TotalEssenceInfo {
	public final String essence;
	public final int totalAmount;
	
	public TotalEssenceInfo(String essence, int totalAmount) {
		this.essence = essence;
		this.totalAmount = totalAmount;
	}
}

package net.felix.utilities.Aincraft.blueprints.data;


/**
 * Data class to store blueprint name and color information
 */
public class BlueprintNameAndColor {
	public final String name;
	/** One of: common, uncommon, rare, epic, legendary (from item name color), or null if unknown */
	public final String rarity;
	
	public BlueprintNameAndColor(String name, String rarity) {
		this.name = name;
		this.rarity = rarity;
	}
}

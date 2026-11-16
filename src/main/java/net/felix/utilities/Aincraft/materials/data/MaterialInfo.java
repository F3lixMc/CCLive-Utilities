package net.felix.utilities.Aincraft.materials.data;


/**
 * Data class to store material information
 */
public class MaterialInfo {
	public final int floor;
	public final String pond;
	public final String rarity;
	public final String color;
	
	public MaterialInfo(int floor, String rarity, String color) {
		this(floor, null, rarity, color);
	}
	
	public MaterialInfo(int floor, String pond, String rarity, String color) {
		this.floor = floor;
		this.pond = pond;
		this.rarity = rarity;
		this.color = color;
	}
}

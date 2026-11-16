package net.felix.features.information.materials;

import java.util.List;

/**
 * Public data class to return material information
 */
public class MaterialFloorInfo {
	public final int floor;
	public final String pond;
	public final List<String> ponds;
	public final String rarity;
	public final String color;
	
	public MaterialFloorInfo(int floor, String rarity, String color) {
		this(floor, null, null, rarity, color);
	}
	
	public MaterialFloorInfo(int floor, String pond, String rarity, String color) {
		this(floor, pond, null, rarity, color);
	}

	public MaterialFloorInfo(int floor, String pond, List<String> ponds, String rarity, String color) {
		this.floor = floor;
		this.pond = pond;
		this.ponds = ponds;
		this.rarity = rarity;
		this.color = color;
	}
}

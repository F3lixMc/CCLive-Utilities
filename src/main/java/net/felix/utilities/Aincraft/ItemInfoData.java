package net.felix.utilities.Aincraft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ItemInfoData {
	String id;
	Integer customModelData;
	String name;
	String floor;
	String rarity;
	Map<String, Map<String, Object>> price = new LinkedHashMap<>();
	Map<String, Map<String, Object>> blueprintShopPrice;
	boolean aspect;
	String type;
	String piece;
	List<String> modifiers = new ArrayList<>();
	List<String> stats;
	boolean blueprint;
	boolean fishingComponent;
	List<String> tags = new ArrayList<>();
}

final class ItemInfoBlueprintInfo {
	String floor;
	String rarity;
}

package net.felix.utilities.DragOverlay.dock;

import java.util.Set;

/** Stabile IDs frei verschiebbarer HUD-Overlays. Container-Ansichten stehen hier nicht. */
public final class OverlayDockIds {
	public static final String CLIPBOARD = "clipboard";
	public static final String BLUEPRINT = "blueprint";
	public static final String COLLECTION = "collection";
	public static final String MINING = "mining";
	public static final String COIN = "coin";
	public static final String KILLS = "kills";
	public static final String MATERIAL = "material";
	public static final String BOSS_HP = "bossHp";
	public static final String FACTORY = "factoryLevel";
	public static final String ESSENCE = "essence";
	public static final String NPC_MAIN = "npcMain";
	public static final String CHAT_ASPECT = "chatAspect";
	public static final String ASPECT = "aspect";
	public static final String STAR_ASPECT = "starAspect";
	public static final String CARDS = "cards";
	public static final String STATUES = "statues";
	public static final String MK_LEVEL = "mkLevel";

	private static final String NPC_PREFIX = "npc:";

	private static final Set<String> FIXED = Set.of(
			CLIPBOARD, BLUEPRINT, COLLECTION, MINING, COIN, KILLS, MATERIAL,
			BOSS_HP, FACTORY, ESSENCE, NPC_MAIN, CHAT_ASPECT, ASPECT, STAR_ASPECT,
			CARDS, STATUES, MK_LEVEL);

	private OverlayDockIds() {
	}

	public static String npc(String configKey) {
		return NPC_PREFIX + configKey;
	}

	public static boolean known(String id) {
		if (id == null || id.isBlank()) {
			return false;
		}
		if (id.startsWith(NPC_PREFIX)) {
			return id.length() > NPC_PREFIX.length();
		}
		return FIXED.contains(id);
	}
}

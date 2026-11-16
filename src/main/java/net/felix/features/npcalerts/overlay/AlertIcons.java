package net.felix.features.npcalerts.overlay;

import net.felix.CCLiveUtilities;
import net.minecraft.resources.Identifier;

/**
 * Icon-Texturen der NPC-Alerts.
 */
public final class AlertIcons {

	private AlertIcons() {
	}

	// Icon Identifier für Forschung, Amboss, Schmelzofen, Recycler, Seelen, Essenzen, Jäger und Machtkristalle
	public static final Identifier FORSCHUNG_ICON = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_forschung.png");

	public static final Identifier AMBOSS_ICON = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_anvil.png");

	public static final Identifier SCHMELZOFEN_ICON = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_ofen.png");

	public static final Identifier RECYCLER_ICON = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_recycler.png");

	public static final Identifier SEELEN_ICON = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_seelen.png");

	public static final Identifier ESSENZEN_ICON = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_essences.png");

	public static final Identifier JAEGER_ICON = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_bogen.png");

	public static final Identifier KOMBO_KISTE_ICON = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_kombo_kiste.png");

	public static final Identifier MACHTKRISTALL_ICON = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_machtkristall.png");

	/**
	 * Icon-Textur eines Alerts anhand seines Config-Keys, oder {@code null} wenn der Key kein Icon hat.
	 */
	public static Identifier iconForKey(String configKey) {
		if (configKey == null) {
			return null;
		}
		return switch (configKey) {
			case "forschung" -> FORSCHUNG_ICON;
			case "amboss" -> AMBOSS_ICON;
			case "schmelzofen" -> SCHMELZOFEN_ICON;
			case "seelen" -> SEELEN_ICON;
			case "essenzen" -> ESSENZEN_ICON;
			case "jaeger" -> JAEGER_ICON;
			case "komboKiste" -> KOMBO_KISTE_ICON;
			case "recyclerSlot1", "recyclerSlot2", "recyclerSlot3" -> RECYCLER_ICON;
			case "machtkristalle", "machtkristalleSlot1", "machtkristalleSlot2", "machtkristalleSlot3" -> MACHTKRISTALL_ICON;
			default -> null;
		};
	}

	/**
	 * Label, das statt des Icons gezeichnet wird, wenn die Textur nicht geladen werden kann.
	 */
	public static String fallbackLabelForKey(String configKey) {
		if (configKey == null) {
			return null;
		}
		return switch (configKey) {
			case "forschung" -> "Forschung: ";
			case "amboss" -> "Amboss: ";
			case "schmelzofen" -> "Schmelzofen: ";
			case "seelen" -> "Seelen: ";
			case "essenzen" -> "Essenzen: ";
			case "jaeger" -> "Jäger: ";
			case "komboKiste" -> "Kombo Kiste: ";
			case "recyclerSlot1" -> "Recycler Slot 1: ";
			case "recyclerSlot2" -> "Recycler Slot 2: ";
			case "recyclerSlot3" -> "Recycler Slot 3: ";
			case "machtkristalle", "machtkristalleSlot1", "machtkristalleSlot2", "machtkristalleSlot3" -> "MK: ";
			default -> null;
		};
	}
}

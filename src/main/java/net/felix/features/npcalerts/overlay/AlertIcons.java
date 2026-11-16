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
}

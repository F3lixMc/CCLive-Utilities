package net.felix.features.npcalerts.overlay;

import net.felix.CCLiveUtilitiesConfig;

/**
 * Config-Zugriff der NPC-Alerts: welche Alerts aktiv sind, Skalierung und Farben pro Config-Key.
 */
public final class AlertSettings {

	private AlertSettings() {
	}

	/** NPC Alerts Utility aktiv und Overlays nicht global ausgeblendet („Overlays Ein/Aus“). */
	public static boolean areNpcAlertsOverlaysActive() {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return config.npcAlertsUtilityEnabled && config.npcAlertsOverlaysVisible;
	}

	/** Einzelner Collector aktiv (Overlay-Eintrag in den Einstellungen). */
	public static boolean isNpcAlertCollectorEnabled(String configKey) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return switch (configKey) {
			case "forschung" -> config.showNpcAlertsForschung;
			case "amboss" -> config.showNpcAlertsAmboss;
			case "schmelzofen" -> config.showNpcAlertsSchmelzofen;
			case "jaeger" -> config.showNpcAlertsJaeger;
			case "seelen" -> config.showNpcAlertsSeelen;
			case "essenzen" -> config.showNpcAlertsEssenzen;
			case "komboKiste" -> config.showNpcAlertsKomboKiste;
			case "machtkristalle" -> config.showNpcAlertsMachtkristalle;
			case "recycler", "recyclerSlot1", "recyclerSlot2", "recyclerSlot3" ->
				config.showNpcAlertsRecyclerSlot1 || config.showNpcAlertsRecyclerSlot2 || config.showNpcAlertsRecyclerSlot3;
			default -> true;
		};
	}

	public static boolean isNpcAlertRecyclerSlotEnabled(int slotIndex) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return switch (slotIndex) {
			case 0 -> config.showNpcAlertsRecyclerSlot1;
			case 1 -> config.showNpcAlertsRecyclerSlot2;
			case 2 -> config.showNpcAlertsRecyclerSlot3;
			default -> false;
		};
	}

	public static boolean isNpcAlertMachtkristallSlotEnabled(int slotIndex) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return switch (slotIndex) {
			case 0 -> config.showNpcAlertsMachtkristalleSlot1;
			case 1 -> config.showNpcAlertsMachtkristalleSlot2;
			case 2 -> config.showNpcAlertsMachtkristalleSlot3;
			default -> false;
		};
	}

	/**
	 * Gibt den Scale-Faktor für ein separates Overlay zurück
	 */
	static float getSeparateOverlayScale(String configKey) {
		if (configKey == null) return 1.0f;
		switch (configKey) {
			case "forschung":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungScale;
			case "amboss":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossScale;
			case "schmelzofen":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenScale;
			case "jaeger":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerScale;
			case "komboKiste":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteScale;
			case "seelen":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenScale;
			case "essenzen":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenScale;
			case "machtkristalle":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleScale;
			case "machtkristalleSlot1":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Scale;
			case "machtkristalleSlot2":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Scale;
			case "machtkristalleSlot3":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Scale;
			case "recycler":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerScale;
			case "recyclerSlot1":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Scale;
			case "recyclerSlot2":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Scale;
			case "recyclerSlot3":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Scale;
			default:
				return 1.0f;
		}
	}

	/**
	 * Gibt die konfigurierte Textfarbe für einen NPC Alerts-Eintrag zurück
	 */
	public static int getTextColorForConfigKey(String configKey) {
		if (configKey == null) return 0xFFFFFFFF;
		java.awt.Color color;
		switch (configKey) {
			case "forschung":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungTextColor;
				break;
			case "amboss":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossTextColor;
				break;
			case "schmelzofen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenTextColor;
				break;
			case "jaeger":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerTextColor;
				break;
			case "komboKiste":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteTextColor;
				break;
			case "seelen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenTextColor;
				break;
			case "essenzen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenTextColor;
				break;
			case "machtkristalle":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleTextColor;
				break;
			case "recyclerSlot1":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1TextColor;
				break;
			case "recyclerSlot2":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2TextColor;
				break;
			case "recyclerSlot3":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3TextColor;
				break;
			default:
				return 0xFFFFFFFF;
		}
		return (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
	}

	/**
	 * Gibt die konfigurierte Prozentfarbe für einen NPC Alerts-Eintrag zurück
	 */
	public static int getPercentColorForConfigKey(String configKey) {
		if (configKey == null) return 0xFFFFFF00;
		java.awt.Color color;
		switch (configKey) {
			case "forschung":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungPercentColor;
				break;
			case "amboss":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossPercentColor;
				break;
			case "schmelzofen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenPercentColor;
				break;
			case "jaeger":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerPercentColor;
				break;
			case "seelen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenPercentColor;
				break;
			case "essenzen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenPercentColor;
				break;
			case "machtkristalle":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristallePercentColor;
				break;
			case "recyclerSlot1":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1PercentColor;
				break;
			case "recyclerSlot2":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2PercentColor;
				break;
			case "recyclerSlot3":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3PercentColor;
				break;
			default:
				return 0xFFFFFF00;
		}
		return (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
	}
}

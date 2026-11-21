package net.felix.utilities.Overall.npcalerts.overlay;

import net.felix.CCLiveUtilitiesConfig;

/**
 * Amboss-Slot 2: zweite Mengen-Zeile im gemeinsamen Overlay, oder ein eigenes Overlay.
 */
public final class AmbossSlots {

    private AmbossSlots() {
    }

    public static boolean slot2Enabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossSlot2;
    }

    /** Zweite Zeile im Amboss-Overlay, solange Slot 2 nicht einzeln gezeichnet wird. */
    public static boolean slot2InSharedOverlay() {
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        return config.showNpcAlertsAmbossSlot2 && !config.npcAlertsAmbossSlot2Separate;
    }

    /** Eigenes Overlay, sobald Amboss getrennt ist und Slot 2 auf Einzeln steht. */
    public static boolean slot2SeparateOverlay() {
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        return config.showNpcAlertsAmboss
                && config.npcAlertsAmbossSeparateOverlay
                && config.showNpcAlertsAmbossSlot2
                && config.npcAlertsAmbossSlot2Separate;
    }
}

package net.felix.utilities.Overall.npcalerts.config;

import net.felix.CCLiveUtilitiesConfig;

/**
 * Position, Sichtbarkeit und Anzeige-Optionen (Icon, Prozent) eines getrennten NPC-Alert-Overlays anhand des Config-Keys.
 */
public final class SeparateOverlayConfig {

    private SeparateOverlayConfig() {
    }

    public static boolean isInfoEnabled(String configKey) {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss;
            case "ambossSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss
                        && CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossSlot2;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofen;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaeger;
            case "komboKiste":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsKomboKiste;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelen;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzen;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle;
            case "machtkristalleSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle && 
                       CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1;
            case "machtkristalleSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle && 
                       CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2;
            case "machtkristalleSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle && 
                       CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3;
            case "recycler":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 || 
                       CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 || 
                       CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3;
            default:
                return false;
        }
    }

    public static boolean isSeparateOverlayEnabled(String configKey) {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSeparateOverlay;
            case "ambossSlot2":
                return net.felix.utilities.Overall.npcalerts.overlay.AmbossSlots.slot2SeparateOverlay();
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenSeparateOverlay;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerSeparateOverlay;
            case "komboKiste":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteSeparateOverlay;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenSeparateOverlay;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenSeparateOverlay;
            case "machtkristalle":
                // Prüfe ob separate Overlay aktiviert ist UND ob nicht alle Slots einzeln sind
                if (!CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay) {
                    return false;
                }
                // Wenn alle 3 Slots einzeln sind, gibt es kein Multi-Line-Overlay mehr
                boolean allSlotsSeparate = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Separate &&
                                          CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Separate &&
                                          CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Separate;
                return !allSlotsSeparate;
            case "recycler":
                // Prüfe ob mindestens ein Recycler-Slot "Separates Overlay" aktiviert hat
                boolean recyclerSlot1Active = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
                                            CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay;
                boolean recyclerSlot2Active = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
                                            CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay;
                boolean recyclerSlot3Active = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
                                            CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay;
                if (!recyclerSlot1Active && !recyclerSlot2Active && !recyclerSlot3Active) {
                    return false;
                }
                // Wenn alle 3 Slots einzeln sind, gibt es kein Multi-Line-Overlay mehr
                boolean allRecyclerSlotsSeparate = (recyclerSlot1Active && CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Separate) &&
                                                   (recyclerSlot2Active && CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Separate) &&
                                                   (recyclerSlot3Active && CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Separate);
                return !allRecyclerSlotsSeparate;
            case "machtkristalleSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay && 
                       CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Separate &&
                       CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1;
            case "machtkristalleSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay && 
                       CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Separate &&
                       CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2;
            case "machtkristalleSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay && 
                       CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Separate &&
                       CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay && 
                       CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Separate &&
                       CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay && 
                       CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Separate &&
                       CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay && 
                       CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Separate &&
                       CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3;
            default:
                return false;
        }
    }

    public static int loadX(String configKey) {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungX;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossX;
            case "ambossSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSlot2X;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenX;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerX;
            case "komboKiste":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteX;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenX;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenX;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleX;
            case "machtkristalleSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1X;
            case "machtkristalleSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2X;
            case "machtkristalleSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3X;
            case "recycler":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerX;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1X;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2X;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3X;
            default:
                return 10;
        }
    }

    public static int loadY(String configKey) {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungY;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossY;
            case "ambossSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSlot2Y;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenY;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerY;
            case "komboKiste":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteY;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenY;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenY;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleY;
            case "machtkristalleSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Y;
            case "machtkristalleSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Y;
            case "machtkristalleSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Y;
            case "recycler":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerY;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Y;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Y;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Y;
            default:
                return 10;
        }
    }

    public static void saveX(String configKey, int x) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungX = x;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossX = x;
                break;
            case "ambossSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSlot2X = x;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenX = x;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerX = x;
                break;
            case "komboKiste":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteX = x;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenX = x;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenX = x;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleX = x;
                break;
            case "machtkristalleSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1X = x;
                break;
            case "machtkristalleSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2X = x;
                break;
            case "machtkristalleSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3X = x;
                break;
            case "recycler":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerX = x;
                break;
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1X = x;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2X = x;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3X = x;
                break;
        }
    }

    public static void saveY(String configKey, int y) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungY = y;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossY = y;
                break;
            case "ambossSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSlot2Y = y;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenY = y;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerY = y;
                break;
            case "komboKiste":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteY = y;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenY = y;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenY = y;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleY = y;
                break;
            case "machtkristalleSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Y = y;
                break;
            case "machtkristalleSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Y = y;
                break;
            case "machtkristalleSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Y = y;
                break;
            case "recycler":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerY = y;
                break;
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Y = y;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Y = y;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Y = y;
                break;
        }
    }

    public static int getDefaultY(String configKey) {
        switch (configKey) {
            case "forschung":
                return 10;
            case "amboss":
                return 50;
            case "ambossSlot2":
                return 70;
            case "schmelzofen":
                return 90;
            case "jaeger":
                return 130;
            case "komboKiste":
                return 230;
            case "seelen":
                return 170;
            case "essenzen":
                return 210;
            case "machtkristalle":
                return 250;
            case "recycler":
                return 290;
            case "recyclerSlot1":
                return 290;
            case "recyclerSlot2":
                return 330;
            case "recyclerSlot3":
                return 370;
            default:
                return 10;
        }
    }

    public static boolean isIconVisible(String configKey) {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowIcon;
            case "amboss":
            case "ambossSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowIcon;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowIcon;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowIcon;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowIcon;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowIcon;
            case "komboKiste":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowIcon;
            case "machtkristalle":
            case "machtkristalleSlot1":
            case "machtkristalleSlot2":
            case "machtkristalleSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowIcon;
            case "recycler":
                // Prüfe ob mindestens ein Recycler-Slot das Icon aktiviert hat
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1ShowIcon || 
                       CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2ShowIcon || 
                       CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3ShowIcon;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1ShowIcon;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2ShowIcon;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3ShowIcon;
            default:
                return false;
        }
    }

    public static boolean isPercentVisible(String configKey) {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschungPercent;
            case "amboss":
            case "ambossSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofenPercent;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaegerPercent;
            case "komboKiste":
                return false;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelenPercent;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzenPercent;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
            default:
                return false;
        }
    }
}

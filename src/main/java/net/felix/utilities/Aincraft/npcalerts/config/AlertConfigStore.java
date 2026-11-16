package net.felix.utilities.Aincraft.npcalerts.config;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.OverlayEditorScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Liest und schreibt die Einstellungen eines einzelnen NPC-Alerts (Prozent, Icon, Hintergrund, Bildschirmmeldungen, getrennte Overlays, Slots) anhand des Config-Keys.
 */
public final class AlertConfigStore {

    private AlertConfigStore() {
    }

    public static boolean getShowPercent(String configKey) {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschungPercent;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofenPercent;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaegerPercent;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelenPercent;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzenPercent;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristallePercentOver100;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
            default:
                return false;
        }
    }

    public static boolean getShowMachtkristalleLevel(String configKey) {
        return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleLevel;
    }

    public static void setShowMachtkristalleLevel(String configKey, boolean value) {
        CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleLevel = value;
    }

    public static boolean getSeparateOverlay(String configKey) {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSeparateOverlay;
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
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay;
            case "recycler":
                // Für "recycler" prüfe ob mindestens ein Slot "Separates Overlay" hat
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay ||
                       CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay ||
                       CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay;
            default:
                return false;
        }
    }

    public static void setSeparateOverlay(String configKey, Screen parent, boolean value) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay = value;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSeparateOverlay = value;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenSeparateOverlay = value;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerSeparateOverlay = value;
                break;
            case "komboKiste":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteSeparateOverlay = value;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenSeparateOverlay = value;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenSeparateOverlay = value;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay = value;
                break;
            case "recycler":
                // Für "recycler" setze alle 3 Slots
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay = value;
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay = value;
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay = value;
                break;
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay = value;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay = value;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay = value;
                break;
        }
        
        // Aktualisiere das Overlay-Editor-Screen, wenn es geöffnet ist
        if (parent instanceof OverlayEditorScreen) {
            ((OverlayEditorScreen) parent).refreshOverlays();
        }
    }

    /**
     * Gibt zurück, ob ein bestimmter Machtkristall-Slot einzeln gerendert werden soll
     */
    public static boolean getMachtkristallSlotSeparate(String configKey, int slotIndex) {
        if (!configKey.equals("machtkristalle")) {
            return false;
        }
        switch (slotIndex) {
            case 0:
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Separate;
            case 1:
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Separate;
            case 2:
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Separate;
            default:
                return false;
        }
    }

    /**
     * Setzt, ob ein bestimmter Machtkristall-Slot einzeln gerendert werden soll
     */
    public static void setMachtkristallSlotSeparate(String configKey, int slotIndex, boolean value) {
        if (!configKey.equals("machtkristalle")) {
            return;
        }
        switch (slotIndex) {
            case 0:
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Separate = value;
                break;
            case 1:
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Separate = value;
                break;
            case 2:
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Separate = value;
                break;
        }
    }

    /**
     * Gibt zurück, ob ein bestimmter Recycler-Slot aktiviert ist
     */
    public static boolean getRecyclerSlotEnabled(String configKey, int slotIndex) {
        boolean isRecycler = configKey.equals("recycler");
        boolean isRecyclerSlot = configKey.equals("recyclerSlot1") || configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        if (!isRecycler && !isRecyclerSlot) {
            return false;
        }
        switch (slotIndex) {
            case 0:
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1;
            case 1:
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2;
            case 2:
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3;
            default:
                return false;
        }
    }

    /**
     * Setzt, ob ein bestimmter Recycler-Slot aktiviert ist
     */
    public static void setRecyclerSlotEnabled(String configKey, Screen parent, int slotIndex, boolean value) {
        boolean isRecycler = configKey.equals("recycler");
        boolean isRecyclerSlot = configKey.equals("recyclerSlot1") || configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        if (!isRecycler && !isRecyclerSlot) {
            return;
        }
        switch (slotIndex) {
            case 0:
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 = value;
                break;
            case 1:
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 = value;
                break;
            case 2:
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 = value;
                break;
        }
        
        // Aktualisiere das Overlay-Editor-Screen, wenn es geöffnet ist
        if (parent instanceof OverlayEditorScreen) {
            ((OverlayEditorScreen) parent).refreshOverlays();
        }
    }

    /**
     * Gibt zurück, ob ein bestimmter Recycler-Slot einzeln gerendert werden soll
     */
    public static boolean getRecyclerSlotSeparate(String configKey, int slotIndex) {
        boolean isRecycler = configKey.equals("recycler");
        boolean isRecyclerSlot = configKey.equals("recyclerSlot1") || configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        if (!isRecycler && !isRecyclerSlot) {
            return false;
        }
        switch (slotIndex) {
            case 0:
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Separate;
            case 1:
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Separate;
            case 2:
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Separate;
            default:
                return false;
        }
    }

    /**
     * Setzt, ob ein bestimmter Recycler-Slot einzeln gerendert werden soll
     */
    public static void setRecyclerSlotSeparate(String configKey, int slotIndex, boolean value) {
        boolean isRecycler = configKey.equals("recycler");
        boolean isRecyclerSlot = configKey.equals("recyclerSlot1") || configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        if (!isRecycler && !isRecyclerSlot) {
            return;
        }
        switch (slotIndex) {
            case 0:
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Separate = value;
                break;
            case 1:
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Separate = value;
                break;
            case 2:
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Separate = value;
                break;
        }
    }

    public static double getWarnPercent(String configKey) {
        switch (configKey) {
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossWarnPercent;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenWarnPercent;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerWarnPercent;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenWarnPercent;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenWarnPercent;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleWarnPercent;
            default:
                return -1.0;
        }
    }

    public static void setWarnPercent(String configKey, double value) {
        switch (configKey) {
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossWarnPercent = value;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenWarnPercent = value;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerWarnPercent = value;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenWarnPercent = value;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenWarnPercent = value;
                break;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent = value;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleWarnPercent = value;
                break;
        }
    }

    public static boolean getShowIcon(String configKey) {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowIcon;
            case "amboss":
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
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowIcon;
            case "recycler":
                // Für "recycler" prüfe ob mindestens ein Slot Icon aktiviert hat
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

    public static void setShowIcon(String configKey, boolean value) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowIcon = value;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowIcon = value;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowIcon = value;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowIcon = value;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowIcon = value;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowIcon = value;
                break;
            case "komboKiste":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowIcon = value;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowIcon = value;
                break;
            case "recycler":
                // Für "recycler" setze alle 3 Slots
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1ShowIcon = value;
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2ShowIcon = value;
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3ShowIcon = value;
                break;
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1ShowIcon = value;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2ShowIcon = value;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3ShowIcon = value;
                break;
        }
    }

    public static boolean getShowBackground(String configKey) {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowBackground;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowBackground;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowBackground;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowBackground;
            case "komboKiste":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowBackground;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowBackground;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowBackground;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowBackground;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerShowBackground;
            default:
                return true;
        }
    }

    public static void setShowBackground(String configKey, boolean value) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowBackground = value;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowBackground = value;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowBackground = value;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowBackground = value;
                break;
            case "komboKiste":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowBackground = value;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowBackground = value;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowBackground = value;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowBackground = value;
                break;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerShowBackground = value;
                break;
        }
    }

    public static boolean getShowScreenMessage(String configKey) {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungScreenMessage;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossScreenMessage;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenScreenMessage;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerScreenMessage;
            case "komboKiste":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteScreenMessage;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenScreenMessage;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenScreenMessage;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleScreenMessage;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerScreenMessage;
            default:
                return false;
        }
    }

    public static void setShowScreenMessage(String configKey, boolean value) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungScreenMessage = value;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossScreenMessage = value;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenScreenMessage = value;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerScreenMessage = value;
                break;
            case "komboKiste":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteScreenMessage = value;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenScreenMessage = value;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenScreenMessage = value;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleScreenMessage = value;
                break;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerScreenMessage = value;
                break;
        }
    }

    public static boolean getShowScreenMessageWhenOverlayDisabled(String configKey) {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungScreenMessageWhenOverlayDisabled;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossScreenMessageWhenOverlayDisabled;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenScreenMessageWhenOverlayDisabled;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerScreenMessageWhenOverlayDisabled;
            case "komboKiste":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteScreenMessageWhenOverlayDisabled;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenScreenMessageWhenOverlayDisabled;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenScreenMessageWhenOverlayDisabled;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleScreenMessageWhenOverlayDisabled;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerScreenMessageWhenOverlayDisabled;
            default:
                return false;
        }
    }

    public static void setShowScreenMessageWhenOverlayDisabled(String configKey, boolean value) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungScreenMessageWhenOverlayDisabled = value;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossScreenMessageWhenOverlayDisabled = value;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenScreenMessageWhenOverlayDisabled = value;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerScreenMessageWhenOverlayDisabled = value;
                break;
            case "komboKiste":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteScreenMessageWhenOverlayDisabled = value;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenScreenMessageWhenOverlayDisabled = value;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenScreenMessageWhenOverlayDisabled = value;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleScreenMessageWhenOverlayDisabled = value;
                break;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerScreenMessageWhenOverlayDisabled = value;
                break;
        }
    }

    /**
     * Gibt zurück, ob ein bestimmter Machtkristall-Slot aktiviert ist
     */
    public static boolean getMachtkristalleSlotEnabled(String configKey, int slotIndex) {
        if (!configKey.equals("machtkristalle")) {
            return false;
        }
        switch (slotIndex) {
            case 0:
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1;
            case 1:
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2;
            case 2:
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3;
            default:
                return false;
        }
    }

    /**
     * Setzt, ob ein bestimmter Machtkristall-Slot aktiviert ist
     */
    public static void setMachtkristalleSlotEnabled(String configKey, Screen parent, int slotIndex, boolean value) {
        if (!configKey.equals("machtkristalle")) {
            return;
        }
        switch (slotIndex) {
            case 0:
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1 = value;
                break;
            case 1:
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2 = value;
                break;
            case 2:
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3 = value;
                break;
        }
        
        // Aktualisiere das Overlay-Editor-Screen, wenn es geöffnet ist
        if (parent instanceof OverlayEditorScreen) {
            ((OverlayEditorScreen) parent).refreshOverlays();
        }
    }
}

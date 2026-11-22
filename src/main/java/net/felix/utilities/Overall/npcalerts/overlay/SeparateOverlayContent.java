package net.felix.utilities.Overall.npcalerts.overlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.npcalerts.data.CapacityData;
import net.felix.utilities.Overall.npcalerts.data.MachtkristallSlot;
import net.felix.utilities.Overall.npcalerts.data.NpcAlertState;
import net.felix.utilities.Overall.npcalerts.message.ScreenMessages;
import net.felix.utilities.Overall.npcalerts.parser.KomboKisteReader;

import static net.felix.utilities.Overall.npcalerts.config.SeparateOverlayConfig.isIconVisible;

/**
 * Anzeigetext, Prozenttext und Warnstatus eines getrennten NPC-Alert-Overlays.
 */
public final class SeparateOverlayContent {

    private SeparateOverlayContent() {
    }

    public static String getDisplayText(String configKey, String displayName) {
        boolean showIcon = isIconVisible(configKey);
        switch (configKey) {
            case "forschung":
                return showIcon ? NpcAlertState.forschung.getDisplayString() : "Forschung: " + NpcAlertState.forschung.getDisplayString();
            case "amboss":
                return showIcon ? NpcAlertState.ambossCapacity.getDisplayString() : "Amboss: " + NpcAlertState.ambossCapacity.getDisplayString();
            case "ambossSlot2":
                return showIcon ? NpcAlertState.ambossSlot2.getDisplayString() : "Amboss Slot 2: " + NpcAlertState.ambossSlot2.getDisplayString();
            case "schmelzofen":
                return showIcon ? NpcAlertState.schmelzofenCapacity.getDisplayString() : "Schmelzofen: " + NpcAlertState.schmelzofenCapacity.getDisplayString();
            case "jaeger":
                return showIcon ? NpcAlertState.jaegerCapacity.getDisplayStringWithoutCurrentSuffix() : "Jäger: " + NpcAlertState.jaegerCapacity.getDisplayStringWithoutCurrentSuffix();
            case "komboKiste": {
                String fraction = KomboKisteReader.getKomboKisteFractionDisplay();
                return showIcon ? fraction : "Kombo Kiste: " + fraction;
            }
            case "seelen":
                return showIcon ? NpcAlertState.seelenCapacity.getDisplayString() : "Seelen: " + NpcAlertState.seelenCapacity.getDisplayString();
            case "essenzen":
                return showIcon ? NpcAlertState.essenzenCapacity.getDisplayString() : "Essenzen: " + NpcAlertState.essenzenCapacity.getDisplayString();
            case "machtkristalle":
                // Für Machtkristalle: zeige alle 3 Slots
                // Hinweis: Prozentwerte werden separat gerendert (in gelb)
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    MachtkristallSlot slot = NpcAlertState.machtkristallSlots[i];
                    if (i > 0) sb.append("\n");
                    if (slot.isEmpty()) {
                        sb.append(slot.getDisplayTextForEmptySlot(i + 1));
                    } else {
                        String displayText = slot.getDisplayText();
                        if (displayText != null) {
                            sb.append(displayText);
                        }
                    }
                }
                return sb.toString();
            case "machtkristalleSlot1":
                // Für einzelnen MK-Slot 1
                MachtkristallSlot slot1 = NpcAlertState.machtkristallSlots[0];
                if (slot1.isNotFound()) {
                    return "MK 1: Nicht im Widget";
                } else if (slot1.isEmpty()) {
                    return "MK 1: -";
                } else {
                    String displayText = slot1.getDisplayText();
                    return displayText != null ? displayText : "MK 1: ?";
                }
            case "machtkristalleSlot2":
                // Für einzelnen MK-Slot 2
                MachtkristallSlot slot2 = NpcAlertState.machtkristallSlots[1];
                if (slot2.isNotFound()) {
                    return "MK 2: Nicht im Widget";
                } else if (slot2.isEmpty()) {
                    return "MK 2: -";
                } else {
                    String displayText = slot2.getDisplayText();
                    return displayText != null ? displayText : "MK 2: ?";
                }
            case "machtkristalleSlot3":
                // Für einzelnen MK-Slot 3
                MachtkristallSlot slot3 = NpcAlertState.machtkristallSlots[2];
                if (slot3.isNotFound()) {
                    return "MK 3: Nicht im Widget";
                } else if (slot3.isEmpty()) {
                    return "MK 3: -";
                } else {
                    String displayText = slot3.getDisplayText();
                    return displayText != null ? displayText : "MK 3: ?";
                }
            case "recycler":
                // Für Recycler: zeige alle 3 Slots, die im Multi-Line-Overlay sind
                // Prüfe welche Slots aktiv sind und einzeln gerendert werden (diese werden nicht im Multi-Line-Overlay sein)
                boolean recyclerSlot1Active = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
                                            CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay;
                boolean recyclerSlot2Active = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
                                            CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay;
                boolean recyclerSlot3Active = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
                                            CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay;
                boolean slot1Separate = recyclerSlot1Active && CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Separate;
                boolean slot2Separate = recyclerSlot2Active && CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Separate;
                boolean slot3Separate = recyclerSlot3Active && CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Separate;
                
                StringBuilder recyclerSb = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    boolean slotActive = (i == 0 && recyclerSlot1Active) || (i == 1 && recyclerSlot2Active) || (i == 2 && recyclerSlot3Active);
                    boolean slotSeparate = (i == 0 && slot1Separate) || (i == 1 && slot2Separate) || (i == 2 && slot3Separate);
                    // Überspringe Slots, die nicht aktiv sind oder einzeln gerendert werden
                    if (!slotActive || slotSeparate) {
                        continue;
                    }
                    
                    CapacityData recyclerSlot = (i == 0) ? NpcAlertState.recyclerSlot1 : 
                                                              (i == 1) ? NpcAlertState.recyclerSlot2 : 
                                                              NpcAlertState.recyclerSlot3;
                    if (recyclerSb.length() > 0) recyclerSb.append("\n");
                    String slotText = showIcon ? recyclerSlot.getDisplayString() : "Recycler Slot " + (i + 1) + ": " + recyclerSlot.getDisplayString();
                    recyclerSb.append(slotText);
                }
                return recyclerSb.toString();
            case "recyclerSlot1":
                return showIcon ? NpcAlertState.recyclerSlot1.getDisplayString() : "Recycler Slot 1: " + NpcAlertState.recyclerSlot1.getDisplayString();
            case "recyclerSlot2":
                return showIcon ? NpcAlertState.recyclerSlot2.getDisplayString() : "Recycler Slot 2: " + NpcAlertState.recyclerSlot2.getDisplayString();
            case "recyclerSlot3":
                return showIcon ? NpcAlertState.recyclerSlot3.getDisplayString() : "Recycler Slot 3: " + NpcAlertState.recyclerSlot3.getDisplayString();
            default:
                return displayName + ": X / Y";
        }
    }

    public static String getPercentText(String configKey) {
        switch (configKey) {
            case "forschung":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschungPercent) {
                    if (NpcAlertState.forschung.isValid()) {
                        // Forschung zählt runter: wenn current näher an max ist, ist der Prozent höher
                        return PercentFormatter.formatPercent(
                            NpcAlertState.forschung.current, 
                            NpcAlertState.forschung.max
                        );
                    } else {
                        // Kein Prozent, solange keine gültigen Werte (Anzeige: "Nicht im Tab-Widget")
                        return null;
                    }
                }
                break;
            case "amboss":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent && 
                    NpcAlertState.ambossCapacity.isValid()) {
                    return PercentFormatter.formatPercent(
                        NpcAlertState.ambossCapacity.current, 
                        NpcAlertState.ambossCapacity.max
                    );
                }
                break;
            case "ambossSlot2":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent &&
                    NpcAlertState.ambossSlot2.isValid()) {
                    return PercentFormatter.formatPercent(
                        NpcAlertState.ambossSlot2.current,
                        NpcAlertState.ambossSlot2.max
                    );
                }
                break;
            case "schmelzofen":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofenPercent && 
                    NpcAlertState.schmelzofenCapacity.isValid()) {
                    return PercentFormatter.formatPercent(
                        NpcAlertState.schmelzofenCapacity.current, 
                        NpcAlertState.schmelzofenCapacity.max
                    );
                }
                break;
            case "jaeger":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaegerPercent && 
                    NpcAlertState.jaegerCapacity.isValid()) {
                    return PercentFormatter.formatPercent(
                        NpcAlertState.jaegerCapacity.current, 
                        NpcAlertState.jaegerCapacity.max
                    );
                }
                break;
            case "seelen":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelenPercent && 
                    NpcAlertState.seelenCapacity.isValid()) {
                    return PercentFormatter.formatPercent(
                        NpcAlertState.seelenCapacity.current, 
                        NpcAlertState.seelenCapacity.max
                    );
                }
                break;
            case "essenzen":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzenPercent && 
                    NpcAlertState.essenzenCapacity.isValid()) {
                    return PercentFormatter.formatPercent(
                        NpcAlertState.essenzenCapacity.current, 
                        NpcAlertState.essenzenCapacity.max
                    );
                }
                break;
            case "machtkristalleSlot1":
                // Für einzelnen MK-Slot 1
                MachtkristallSlot slot1 = NpcAlertState.machtkristallSlots[0];
                if (!slot1.isEmpty() && !slot1.isNotFound()) {
                    return slot1.getPercentText();
                }
                break;
            case "machtkristalleSlot2":
                // Für einzelnen MK-Slot 2
                MachtkristallSlot slot2 = NpcAlertState.machtkristallSlots[1];
                if (!slot2.isEmpty() && !slot2.isNotFound()) {
                    return slot2.getPercentText();
                }
                break;
            case "machtkristalleSlot3":
                // Für einzelnen MK-Slot 3
                MachtkristallSlot slot3 = NpcAlertState.machtkristallSlots[2];
                if (!slot3.isEmpty() && !slot3.isNotFound()) {
                    return slot3.getPercentText();
                }
                break;
            case "recycler":
                // Für Multi-Line-Overlay: Prozente werden separat für jede Zeile gerendert
                return null;
            case "recyclerSlot1":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent && 
                    NpcAlertState.recyclerSlot1.isValid()) {
                    return PercentFormatter.formatPercent(
                        NpcAlertState.recyclerSlot1.current, 
                        NpcAlertState.recyclerSlot1.max
                    );
                }
                break;
            case "recyclerSlot2":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent && 
                    NpcAlertState.recyclerSlot2.isValid()) {
                    return PercentFormatter.formatPercent(
                        NpcAlertState.recyclerSlot2.current, 
                        NpcAlertState.recyclerSlot2.max
                    );
                }
                break;
            case "recyclerSlot3":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent && 
                    NpcAlertState.recyclerSlot3.isValid()) {
                    return PercentFormatter.formatPercent(
                        NpcAlertState.recyclerSlot3.current, 
                        NpcAlertState.recyclerSlot3.max
                    );
                }
                break;
        }
        return null;
    }

    public static boolean isWarningVisible(String configKey) {
        switch (configKey) {
            case "forschung":
                if (NpcAlertState.forschung.isValid()) {
                    int warnValue = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungWarnValue;
                    return warnValue >= 0 && NpcAlertState.forschung.current <= warnValue;
                }
                break;
            case "amboss":
                if (NpcAlertState.ambossCapacity.isValid()) {
                    double currentPercent = ((double)NpcAlertState.ambossCapacity.current / 
                        (double)NpcAlertState.ambossCapacity.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "ambossSlot2":
                if (NpcAlertState.ambossSlot2.isValid()) {
                    double currentPercent = ((double) NpcAlertState.ambossSlot2.current /
                        (double) NpcAlertState.ambossSlot2.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "schmelzofen":
                if (NpcAlertState.schmelzofenCapacity.isValid()) {
                    double currentPercent = ((double)NpcAlertState.schmelzofenCapacity.current / 
                        (double)NpcAlertState.schmelzofenCapacity.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "jaeger":
                if (NpcAlertState.jaegerCapacity.isValid()) {
                    double currentPercent = ((double)NpcAlertState.jaegerCapacity.current / 
                        (double)NpcAlertState.jaegerCapacity.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "komboKiste":
                if (NpcAlertState.komboKiste.isValid()) {
                    return NpcAlertState.komboKiste.current >= NpcAlertState.komboKiste.max;
                }
                break;
            case "seelen":
                if (NpcAlertState.seelenCapacity.isValid()) {
                    double currentPercent = ((double)NpcAlertState.seelenCapacity.current / 
                        (double)NpcAlertState.seelenCapacity.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "essenzen":
                if (NpcAlertState.essenzenCapacity.isValid()) {
                    double currentPercent = ((double)NpcAlertState.essenzenCapacity.current / 
                        (double)NpcAlertState.essenzenCapacity.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "machtkristalle":
                for (int i = 0; i < 3; i++) {
                    if (ScreenMessages.shouldShowMachtkristallWarning(NpcAlertState.machtkristallSlots[i])) {
                        return true;
                    }
                }
                break;
            case "recyclerSlot1":
                if (NpcAlertState.recyclerSlot1.isValid()) {
                    double currentPercent = ((double)NpcAlertState.recyclerSlot1.current / 
                        (double)NpcAlertState.recyclerSlot1.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
                    // Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
                    return warnPercent >= 0 && currentPercent <= warnPercent;
                }
                break;
            case "recyclerSlot2":
                if (NpcAlertState.recyclerSlot2.isValid()) {
                    double currentPercent = ((double)NpcAlertState.recyclerSlot2.current / 
                        (double)NpcAlertState.recyclerSlot2.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
                    // Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
                    return warnPercent >= 0 && currentPercent <= warnPercent;
                }
                break;
            case "recyclerSlot3":
                if (NpcAlertState.recyclerSlot3.isValid()) {
                    double currentPercent = ((double)NpcAlertState.recyclerSlot3.current / 
                        (double)NpcAlertState.recyclerSlot3.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
                    // Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
                    return warnPercent >= 0 && currentPercent <= warnPercent;
                }
                break;
            case "machtkristalleSlot1":
                return ScreenMessages.shouldShowMachtkristallWarning(NpcAlertState.machtkristallSlots[0]);
            case "machtkristalleSlot2":
                return ScreenMessages.shouldShowMachtkristallWarning(NpcAlertState.machtkristallSlots[1]);
            case "machtkristalleSlot3":
                return ScreenMessages.shouldShowMachtkristallWarning(NpcAlertState.machtkristallSlots[2]);
        }
        return false;
    }

    /**
     * Anzeigetext eines Machtkristall-Slots (index 0-2); mit Icon ohne "MK n:"-Präfix.
     */
    public static String machtkristallDisplayText(MachtkristallSlot slot, int index, boolean showIcon) {
        String prefix = "MK " + (index + 1) + ": ";
        // isNotFound() muss VOR isEmpty() geprüft werden, da isNotFound() true ist wenn name = ""
        if (slot.isNotFound()) {
            return showIcon ? "Nicht im Tab-Widget" : prefix + "Nicht im Tab-Widget";
        }
        if (slot.isEmpty()) {
            return showIcon ? "-" : prefix + "-";
        }
        String displayText = slot.getDisplayText();
        if (displayText == null) {
            return showIcon ? "?" : prefix + "?";
        }
        // Entferne "MK [Name]: " Präfix wenn Icon angezeigt wird
        return showIcon ? MachtkristallSlot.stripIconPrefix(displayText) : displayText;
    }
}
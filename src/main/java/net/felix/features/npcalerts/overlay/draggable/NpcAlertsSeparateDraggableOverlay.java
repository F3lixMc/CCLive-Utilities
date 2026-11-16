package net.felix.features.npcalerts.overlay.draggable;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.features.npcalerts.data.CapacityData;
import net.felix.features.npcalerts.data.MachtkristallSlot;
import net.felix.features.npcalerts.data.NpcAlertState;
import net.felix.features.npcalerts.message.ScreenMessages;
import net.felix.features.npcalerts.overlay.PercentFormatter;
import net.felix.utilities.DragOverlay.Overall.DraggableOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import static net.felix.features.npcalerts.NpcAlertsFeature.isInGeneralLobby;
import static net.felix.features.npcalerts.config.SeparateOverlayConfig.getDefaultY;
import static net.felix.features.npcalerts.config.SeparateOverlayConfig.isIconVisible;
import static net.felix.features.npcalerts.config.SeparateOverlayConfig.isInfoEnabled;
import static net.felix.features.npcalerts.config.SeparateOverlayConfig.isPercentVisible;
import static net.felix.features.npcalerts.config.SeparateOverlayConfig.isSeparateOverlayEnabled;
import static net.felix.features.npcalerts.config.SeparateOverlayConfig.loadX;
import static net.felix.features.npcalerts.config.SeparateOverlayConfig.loadY;
import static net.felix.features.npcalerts.config.SeparateOverlayConfig.saveX;
import static net.felix.features.npcalerts.config.SeparateOverlayConfig.saveY;
import static net.felix.features.npcalerts.overlay.AlertIcons.AMBOSS_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.ESSENZEN_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.FORSCHUNG_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.JAEGER_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.KOMBO_KISTE_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.MACHTKRISTALL_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.RECYCLER_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.SCHMELZOFEN_ICON;
import static net.felix.features.npcalerts.overlay.AlertIcons.SEELEN_ICON;
import static net.felix.features.npcalerts.overlay.SeparateOverlayContent.getDisplayText;
import static net.felix.features.npcalerts.overlay.SeparateOverlayContent.getPercentText;
import static net.felix.features.npcalerts.overlay.SeparateOverlayContent.isWarningVisible;

/**
 * Draggable Overlay für einzelne NPC Alerts Overlays
 */
public class NpcAlertsSeparateDraggableOverlay implements DraggableOverlay {

    private static final float MIN_SCALE = 0.05f;

    private final String configKey;

    private final String displayName;

    // Icon Identifier für Forschung, Amboss, Schmelzofen, Seelen, Essenzen, Jäger, Machtkristalle und Recycler
    public NpcAlertsSeparateDraggableOverlay(String infoName, String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    @Override
    public String getOverlayName() {
        return "NPC Alerts: " + displayName;
    }

    @Override
    public int getX() {
        // Calculate X position using the same logic as Mining/Holzfäller overlays
        // baseX is the left edge position (like Mining overlays)
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return loadX(configKey);
        }
        
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int baseX = loadX(configKey);
        int overlayWidth = getWidth(); // Use scaled width for positioning
        
        // Determine if overlay is on left or right side of screen
        boolean isOnLeftSide = baseX < screenWidth / 2;
        
        // Calculate X position based on side (same logic as Mining overlays)
        int x;
        if (isOnLeftSide) {
            // On left side: keep left edge fixed, expand to the right
            x = baseX;
        } else {
            // On right side: keep right edge fixed, expand to the left
            // Right edge is: baseX (since baseX is on the right side, it represents the right edge)
            // Keep this right edge fixed, so left edge moves left when width increases
            x = baseX - overlayWidth;
        }
        
        // Ensure overlay stays within screen bounds
        return Math.max(0, Math.min(x, screenWidth - overlayWidth));
    }

    @Override
    public int getY() {
        return loadY(configKey);
    }

    /**
     * Gibt den Scale-Wert für dieses Overlay zurück
     */
    private float getScale() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungScale;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossScale;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenScale;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerScale;
            case "komboKiste":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteScale;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenScale;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenScale;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleScale;
            case "machtkristalleSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Scale;
            case "machtkristalleSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Scale;
            case "machtkristalleSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Scale;
            case "recycler":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerScale;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Scale;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Scale;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Scale;
            default:
                return 1.0f;
        }
    }

    /**
     * Setzt den Scale-Wert für dieses Overlay
     */
    private void setScale(float scale) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungScale = scale;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossScale = scale;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenScale = scale;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerScale = scale;
                break;
            case "komboKiste":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteScale = scale;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenScale = scale;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenScale = scale;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleScale = scale;
                break;
            case "machtkristalleSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Scale = scale;
                break;
            case "machtkristalleSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Scale = scale;
                break;
            case "machtkristalleSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Scale = scale;
                break;
            case "recycler":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerScale = scale;
                break;
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Scale = scale;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Scale = scale;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Scale = scale;
                break;
        }
    }

    /**
     * Berechnet die unskalierte Breite
     * Dynamische Verbreiterung basierend auf Inhalt (wie BossHP-Overlay)
     */
    private int calculateUnscaledWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return 200;
        
        // Für Machtkristalle: berechne maximale Breite über alle Zeilen, die im Multi-Line-Overlay sind
        if ("machtkristalle".equals(configKey)) {
            boolean showIcon = isIconVisible(configKey);
            // Prüfe welche Slots einzeln gerendert werden (diese werden nicht im Multi-Line-Overlay sein)
            boolean slot1Separate = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Separate;
            boolean slot2Separate = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Separate;
            boolean slot3Separate = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Separate;
            
            int maxWidth = 0;
            for (int i = 0; i < 3; i++) {
                boolean slotSeparate = (i == 0 && slot1Separate) || (i == 1 && slot2Separate) || (i == 2 && slot3Separate);
                // Überspringe Slots, die einzeln gerendert werden
                if (slotSeparate) {
                    continue;
                }
                
                MachtkristallSlot slot = NpcAlertState.machtkristallSlots[i];
                int width = 0;
                
                // Wenn Icon aktiviert ist, füge Icon-Breite hinzu
                if (showIcon) {
                    int iconSize = (int)(client.font.lineHeight * 1.5);
                    width += iconSize + 2; // Icon + Abstand
                    width += client.font.width(": "); // Doppelpunkt nach Icon
                }
                
                // Text-Breite (gleiche Logik wie in renderSeparateOverlays)
                String displayText;
                if (slot.isNotFound()) {
                    displayText = showIcon ? "Nicht im Tab-Widget" : "MK " + (i + 1) + ": Nicht im Tab-Widget";
                } else if (slot.isEmpty()) {
                    displayText = showIcon ? "-" : "MK " + (i + 1) + ": -";
                } else {
                    displayText = slot.getDisplayText();
                    if (displayText == null) {
                        displayText = showIcon ? "?" : "MK " + (i + 1) + ": ?";
                    } else if (showIcon) {
                        // Entferne "MK [Name]: " Präfix wenn Icon angezeigt wird
                        displayText = MachtkristallSlot.stripIconPrefix(displayText);
                    }
                }
                width += client.font.width(displayText);
                
                // Prozent-Breite
                String percentText = slot.getPercentText();
                if (percentText != null) {
                    width += client.font.width(" " + percentText);
                }
                
                if (width > maxWidth) {
                    maxWidth = width;
                }
            }
            int padding = 5;
            // Berechne Breite komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
            return maxWidth + (padding * 2);
        }
        
        // Für Recycler: mehrere Zeilen möglich
        if ("recycler".equals(configKey)) {
            boolean showIcon = isIconVisible(configKey);
            boolean showPercent = isPercentVisible(configKey);
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
            
            int maxWidth = 0;
            for (int i = 0; i < 3; i++) {
                // Prüfe ob dieser Slot aktiviert ist
                boolean slotEnabled;
                switch (i) {
                    case 0:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1;
                        break;
                    case 1:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2;
                        break;
                    case 2:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3;
                        break;
                    default:
                        slotEnabled = true;
                        break;
                }
                
                // Überspringe diesen Slot, wenn er deaktiviert ist
                if (!slotEnabled) {
                    continue;
                }
                
                boolean slotActive = (i == 0 && recyclerSlot1Active) || (i == 1 && recyclerSlot2Active) || (i == 2 && recyclerSlot3Active);
                boolean slotSeparate = (i == 0 && slot1Separate) || (i == 1 && slot2Separate) || (i == 2 && slot3Separate);
                // Überspringe Slots, die nicht aktiv sind oder einzeln gerendert werden
                if (!slotActive || slotSeparate) {
                    continue;
                }
                
                CapacityData recyclerSlot = (i == 0) ? NpcAlertState.recyclerSlot1 : 
                                                          (i == 1) ? NpcAlertState.recyclerSlot2 : 
                                                          NpcAlertState.recyclerSlot3;
                int width = 0;
                
                // Wenn Icon aktiviert ist, füge Icon-Breite hinzu
                if (showIcon) {
                    int iconSize = (int)(client.font.lineHeight * 1.5);
                    width += iconSize + 2; // Icon + Abstand
                    width += client.font.width(": "); // Doppelpunkt nach Icon
                }
                
                // Text-Breite
                String displayText;
                if (!recyclerSlot.isValid()) {
                    displayText = showIcon ? "Nicht im Tab-Widget" : "Recycler Slot " + (i + 1) + ": Nicht im Tab-Widget";
                } else {
                    displayText = showIcon ? recyclerSlot.getDisplayString() : "Recycler Slot " + (i + 1) + ": " + recyclerSlot.getDisplayString();
                }
                width += client.font.width(displayText);
                
                // Prozent-Breite
                if (showPercent && recyclerSlot.isValid()) {
                    String percentText = PercentFormatter.formatPercent(recyclerSlot.current, recyclerSlot.max);
                    width += client.font.width(" " + percentText);
                }
                
                if (width > maxWidth) {
                    maxWidth = width;
                }
            }
            int padding = 5;
            // Berechne Breite komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
            return maxWidth + (padding * 2);
        }
        
        // Für einzelne MK Slots: berechne Breite basierend auf tatsächlichem Text
        if ("machtkristalleSlot1".equals(configKey) || "machtkristalleSlot2".equals(configKey) || "machtkristalleSlot3".equals(configKey)) {
            int slotIndex = configKey.equals("machtkristalleSlot1") ? 0 : (configKey.equals("machtkristalleSlot2") ? 1 : 2);
            
            // Prüfe ob dieser Slot aktiviert ist
            boolean slotEnabled;
            switch (slotIndex) {
                case 0:
                    slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1;
                    break;
                case 1:
                    slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2;
                    break;
                case 2:
                    slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3;
                    break;
                default:
                    slotEnabled = true;
                    break;
            }
            
            // Wenn Slot deaktiviert ist, gib 0 zurück (Overlay wird nicht gerendert)
            if (!slotEnabled) {
                return 0;
            }
            
            MachtkristallSlot slot = NpcAlertState.machtkristallSlots[slotIndex];
            boolean showIcon = isIconVisible(configKey);
            String percentText = slot.getPercentText();
            boolean showPercent = percentText != null;
            
            int width = 0;
            // Wenn Icon aktiviert ist, füge Icon-Breite hinzu
            if (showIcon) {
                int iconSize = (int)(client.font.lineHeight * 1.5);
                width += iconSize + 2; // Icon + Abstand
                width += client.font.width(": "); // Doppelpunkt nach Icon
            }
            
            // Text-Breite (gleiche Logik wie in renderSeparateOverlays)
            String displayText;
            if (slot.isNotFound()) {
                displayText = showIcon ? "Nicht im Tab-Widget" : "MK " + (slotIndex + 1) + ": Nicht im Tab-Widget";
            } else if (slot.isEmpty()) {
                displayText = showIcon ? "-" : "MK " + (slotIndex + 1) + ": -";
            } else {
                displayText = slot.getDisplayText();
                if (displayText == null) {
                    displayText = showIcon ? "?" : "MK " + (slotIndex + 1) + ": ?";
                } else if (showIcon) {
                    // Entferne "MK [Name]: " Präfix wenn Icon angezeigt wird
                    displayText = MachtkristallSlot.stripIconPrefix(displayText);
                }
            }
            width += client.font.width(displayText);
            
            // Prozent-Breite
            if (showPercent && percentText != null) {
                width += client.font.width(" " + percentText);
            }
            
            int padding = 5;
            // Berechne Breite komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
            return width + (padding * 2);
        }
        
        String text = getDisplayText(configKey, displayName);
        String percentText = getPercentText(configKey);
        boolean showPercent = isPercentVisible(configKey);
        boolean showIcon = isIconVisible(configKey);
        
        int width = 0;
        // Wenn Icon aktiviert ist, füge Icon-Breite hinzu
        if (showIcon && (configKey != null && ("forschung".equals(configKey) || "amboss".equals(configKey) || 
                                                "schmelzofen".equals(configKey) || "seelen".equals(configKey) || 
                                                "essenzen".equals(configKey) || "jaeger".equals(configKey) || 
                                                "komboKiste".equals(configKey) ||
                                                "machtkristalle".equals(configKey) ||
                                                "machtkristalleSlot1".equals(configKey) || "machtkristalleSlot2".equals(configKey) || 
                                                "machtkristalleSlot3".equals(configKey) ||
                                                "recyclerSlot1".equals(configKey) || "recyclerSlot2".equals(configKey) || 
                                                "recyclerSlot3".equals(configKey)))) {
            int iconSize = (int)(client.font.lineHeight * 1.5);
            width += iconSize + 2; // Icon + Abstand
            width += client.font.width(": "); // Doppelpunkt nach Icon
        }
        width += client.font.width(text);
        if (showPercent && percentText != null) {
            width += client.font.width(" " + percentText);
        }
        
        int padding = 5;
        // Berechne Breite komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
        return width + (padding * 2);
    }

    /**
     * Berechnet die unskalierte Höhe
     */
    private int calculateUnscaledHeight() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return 20;
        int lineHeight = client.font.lineHeight + 2;
        int padding = 5;
        
        // Für Machtkristalle: mehrere Zeilen möglich
        if ("machtkristalle".equals(configKey)) {
            // Prüfe welche Slots einzeln gerendert werden (diese werden nicht im Multi-Line-Overlay sein)
            boolean slot1Separate = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Separate;
            boolean slot2Separate = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Separate;
            boolean slot3Separate = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Separate;
            
            // Zähle nur die Slots, die im Multi-Line-Overlay sind UND aktiviert sind
            int lineCount = 0;
            for (int i = 0; i < 3; i++) {
                // Prüfe ob dieser Slot aktiviert ist
                boolean slotEnabled;
                switch (i) {
                    case 0:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1;
                        break;
                    case 1:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2;
                        break;
                    case 2:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3;
                        break;
                    default:
                        slotEnabled = true;
                        break;
                }
                
                // Überspringe diesen Slot, wenn er deaktiviert ist
                if (!slotEnabled) {
                    continue;
                }
                
                boolean slotSeparate = (i == 0 && slot1Separate) || (i == 1 && slot2Separate) || (i == 2 && slot3Separate);
                if (!slotSeparate) {
                    lineCount++;
                }
            }
            
            return (lineCount * lineHeight) + (padding * 2);
        }
        
        // Für Recycler: mehrere Zeilen möglich
        if ("recycler".equals(configKey)) {
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
            
            // Zähle nur die Slots, die im Multi-Line-Overlay sind UND aktiviert sind
            int lineCount = 0;
            for (int i = 0; i < 3; i++) {
                // Prüfe ob dieser Slot aktiviert ist
                boolean slotEnabled;
                switch (i) {
                    case 0:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1;
                        break;
                    case 1:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2;
                        break;
                    case 2:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3;
                        break;
                    default:
                        slotEnabled = true;
                        break;
                }
                
                // Überspringe diesen Slot, wenn er deaktiviert ist
                if (!slotEnabled) {
                    continue;
                }
                
                boolean slotActive = (i == 0 && recyclerSlot1Active) || (i == 1 && recyclerSlot2Active) || (i == 2 && recyclerSlot3Active);
                boolean slotSeparate = (i == 0 && slot1Separate) || (i == 1 && slot2Separate) || (i == 2 && slot3Separate);
                if (slotActive && !slotSeparate) {
                    lineCount++;
                }
            }
            
            return (lineCount * lineHeight) + (padding * 2);
        }
        
        // Für Recycler: mehrere Zeilen möglich
        if ("recycler".equals(configKey)) {
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
            
            // Zähle nur die Slots, die im Multi-Line-Overlay sind UND aktiviert sind
            int lineCount = 0;
            for (int i = 0; i < 3; i++) {
                // Prüfe ob dieser Slot aktiviert ist
                boolean slotEnabled;
                switch (i) {
                    case 0:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1;
                        break;
                    case 1:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2;
                        break;
                    case 2:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3;
                        break;
                    default:
                        slotEnabled = true;
                        break;
                }
                
                // Überspringe diesen Slot, wenn er deaktiviert ist
                if (!slotEnabled) {
                    continue;
                }
                
                boolean slotActive = (i == 0 && recyclerSlot1Active) || (i == 1 && recyclerSlot2Active) || (i == 2 && recyclerSlot3Active);
                boolean slotSeparate = (i == 0 && slot1Separate) || (i == 1 && slot2Separate) || (i == 2 && slot3Separate);
                if (slotActive && !slotSeparate) {
                    lineCount++;
                }
            }
            
            return (lineCount * lineHeight) + (padding * 2);
        }
        
        return lineHeight + (padding * 2);
    }

    @Override
    public int getWidth() {
        // Return scaled width
        float scale = getScale();
        if (scale <= 0) scale = 1.0f; // Safety check
        return Math.round(calculateUnscaledWidth() * scale);
    }

    @Override
    public int getHeight() {
        // Return scaled height
        float scale = getScale();
        if (scale <= 0) scale = 1.0f; // Safety check
        return Math.round(calculateUnscaledHeight() * scale);
    }

    @Override
    public void setPosition(int x, int y) {
        // Calculate baseX using the same logic as Mining/Holzfäller overlays
        // We need to reverse the calculation: from the actual x position, calculate baseX
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            saveX(configKey, x);
            saveY(configKey, y);
            return;
        }
        
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int overlayWidth = getWidth(); // Use scaled width for positioning
        
        // Determine if overlay is on left or right side of screen
        boolean isOnLeftSide = x < screenWidth / 2;
        
        // Calculate baseX based on side (reverse of getX() calculation)
        int baseX;
        if (isOnLeftSide) {
            // On left side: baseX is the same as x (left edge)
            baseX = x;
        } else {
            // On right side: baseX is the right edge (x + overlayWidth)
            // We store the right edge as baseX so it stays fixed when width changes
            baseX = x + overlayWidth;
        }
        
        saveX(configKey, baseX);
        saveY(configKey, y);
    }

    @Override
    public void setSize(int width, int height) {
        // Get current unscaled dimensions
        int unscaledWidth = calculateUnscaledWidth();
        int unscaledHeight = calculateUnscaledHeight();
        
        // Calculate scale based on width and height
        float scaleX = (float) width / unscaledWidth;
        float scaleY = (float) height / unscaledHeight;
        float scale = Math.max(MIN_SCALE, Math.min(scaleX, scaleY));
        setScale(scale);
    }

    @Override
    public void renderInEditMode(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Get scale
        float scale = getScale();
        if (scale <= 0) scale = 1.0f;
        
        // Render border for edit mode (scaled)
        context.outline(x, y, width, height, 0xFFFF0000);
        
        // Render background (scaled)
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // Render content with scale using matrix transformation
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        // Render actual content (same as real overlay)
        String text = getDisplayText(configKey, displayName);
        String percentText = getPercentText(configKey);
        boolean showPercent = isPercentVisible(configKey);
        boolean showWarning = isWarningVisible(configKey);
        boolean showIcon = isIconVisible(configKey);
        
        int currentX = 5; // Relativ zu (x, y) nach Matrix-Transformation
        int lineHeight = client.font.lineHeight + 2;
        int unscaledHeight = calculateUnscaledHeight();
        int overlayCenterY = unscaledHeight / 2;
        int currentY = overlayCenterY - client.font.lineHeight / 2;
        
        // Lade Farben aus Config (für Recycler-Slots)
        int textColor = 0xFFFFFFFF;
        int percentColor = 0xFFFFFF00;
        if (configKey != null) {
            if ("recyclerSlot1".equals(configKey)) {
                java.awt.Color color = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1TextColor;
                textColor = (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
                java.awt.Color percentColorObj = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1PercentColor;
                percentColor = (percentColorObj.getAlpha() << 24) | (percentColorObj.getRed() << 16) | (percentColorObj.getGreen() << 8) | percentColorObj.getBlue();
            } else if ("recyclerSlot2".equals(configKey)) {
                java.awt.Color color = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2TextColor;
                textColor = (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
                java.awt.Color percentColorObj = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2PercentColor;
                percentColor = (percentColorObj.getAlpha() << 24) | (percentColorObj.getRed() << 16) | (percentColorObj.getGreen() << 8) | percentColorObj.getBlue();
            } else if ("recyclerSlot3".equals(configKey)) {
                java.awt.Color color = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3TextColor;
                textColor = (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
                java.awt.Color percentColorObj = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3PercentColor;
                percentColor = (percentColorObj.getAlpha() << 24) | (percentColorObj.getRed() << 16) | (percentColorObj.getGreen() << 8) | percentColorObj.getBlue();
            }
        }
        int warningColor = 0xFFFF0000;
        
        // Bestimme Textfarbe: rot und blinkend wenn Warnung aktiv ist
        int currentTextColor = textColor;
        if (showWarning) {
            // Blink-Animation: alle 300ms wechseln
            boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
            if (isVisible) {
                currentTextColor = warningColor;
            } else {
                currentTextColor = textColor;
            }
        }
        
        // Zeichne Icon statt Text, wenn aktiviert (für Forschung, Amboss, Schmelzofen, Seelen, Essenzen, Jäger, Machtkristalle und Recycler)
        if (showIcon && (configKey != null && ("forschung".equals(configKey) || "amboss".equals(configKey) || 
                                                "schmelzofen".equals(configKey) || "seelen".equals(configKey) || 
                                                "essenzen".equals(configKey) || "jaeger".equals(configKey) || 
                                                "komboKiste".equals(configKey) ||
                                                "machtkristalleSlot1".equals(configKey) || "machtkristalleSlot2".equals(configKey) || 
                                                "machtkristalleSlot3".equals(configKey) ||
                                                "recyclerSlot1".equals(configKey) || "recyclerSlot2".equals(configKey) || 
                                                "recyclerSlot3".equals(configKey)))) {
            int iconSize = lineHeight;
            // Zentriere Icon vertikal: Overlay-Mitte minus die Hälfte der Icon-Höhe (overlayCenterY wurde bereits berechnet)
            int iconY = overlayCenterY - iconSize / 2;
            Identifier iconToUse = null;
            
            if ("forschung".equals(configKey)) {
                iconToUse = FORSCHUNG_ICON;
            } else if ("amboss".equals(configKey)) {
                iconToUse = AMBOSS_ICON;
            } else if ("schmelzofen".equals(configKey)) {
                iconToUse = SCHMELZOFEN_ICON;
            } else if ("seelen".equals(configKey)) {
                iconToUse = SEELEN_ICON;
            } else if ("essenzen".equals(configKey)) {
                iconToUse = ESSENZEN_ICON;
            } else if ("jaeger".equals(configKey)) {
                iconToUse = JAEGER_ICON;
            } else if ("komboKiste".equals(configKey)) {
                iconToUse = KOMBO_KISTE_ICON;
            } else if ("machtkristalleSlot1".equals(configKey) || "machtkristalleSlot2".equals(configKey) || 
                       "machtkristalleSlot3".equals(configKey)) {
                iconToUse = MACHTKRISTALL_ICON;
            } else if ("recycler".equals(configKey) || "recyclerSlot1".equals(configKey) || "recyclerSlot2".equals(configKey) || "recyclerSlot3".equals(configKey)) {
                iconToUse = RECYCLER_ICON;
            }
            
            if (iconToUse != null) {
                try {
                    context.blit(
                        RenderPipelines.GUI_TEXTURED,
                        iconToUse,
                        currentX, iconY,
                        0.0f, 0.0f,
                        iconSize, iconSize,
                        iconSize, iconSize
                    );
                    currentX += iconSize + 2; // Abstand nach Icon
                } catch (Exception e) {
                    // Fallback: Zeichne Text wenn Icon nicht geladen werden kann
                }
            }
            // Zeichne Doppelpunkt nach dem Icon
            context.text(
                client.font,
                ": ",
                currentX, currentY,
                currentTextColor,
                true
            );
            currentX += client.font.width(": ");
        }
        
        // Render main text (mehrzeilig für Machtkristalle)
        if ("machtkristalle".equals(configKey)) {
            // Prüfe welche Slots einzeln gerendert werden (diese werden nicht im Multi-Line-Overlay sein)
            boolean slot1Separate = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Separate;
            boolean slot2Separate = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Separate;
            boolean slot3Separate = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Separate;
            
            // Für Multi-Line-Overlay: Starte oben mit PADDING, nicht zentriert
            int lineY = 5; // PADDING
            int mkPercentColor = 0xFFFFFF00; // Gelb für Prozente
            int iconSize = showIcon ? lineHeight : 0;
            
            for (int i = 0; i < 3; i++) {
                // Prüfe ob dieser Slot aktiviert ist
                boolean slotEnabled;
                switch (i) {
                    case 0:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1;
                        break;
                    case 1:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2;
                        break;
                    case 2:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3;
                        break;
                    default:
                        slotEnabled = true;
                        break;
                }
                
                // Überspringe diesen Slot, wenn er deaktiviert ist
                if (!slotEnabled) {
                    continue;
                }
                
                boolean slotSeparate = (i == 0 && slot1Separate) || (i == 1 && slot2Separate) || (i == 2 && slot3Separate);
                // Überspringe Slots, die einzeln gerendert werden
                if (slotSeparate) {
                    continue;
                }
                
                MachtkristallSlot slot = NpcAlertState.machtkristallSlots[i];
                int lineX = currentX;
                int lineCenterY = lineY + lineHeight / 2;
                
                // Berechne displayText (gleiche Logik wie in renderSeparateOverlays)
                String displayText;
                String slotPercentText = null;
                if (slot.isNotFound()) {
                    displayText = showIcon ? "Nicht im Tab-Widget" : "MK " + (i + 1) + ": Nicht im Tab-Widget";
                } else if (slot.isEmpty()) {
                    displayText = showIcon ? "-" : "MK " + (i + 1) + ": -";
                } else {
                    displayText = slot.getDisplayText();
                    slotPercentText = slot.getPercentText();
                    if (displayText == null) {
                        displayText = showIcon ? "?" : "MK " + (i + 1) + ": ?";
                    } else if (showIcon) {
                        // Entferne "MK [Name]: " Präfix wenn Icon angezeigt wird
                        displayText = MachtkristallSlot.stripIconPrefix(displayText);
                    }
                }
                
                boolean lineShowWarning = ScreenMessages.shouldShowMachtkristallWarning(slot);
                int lineTextColor = textColor;
                int linePercentColor = mkPercentColor;
                if (lineShowWarning) {
                    boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
                    lineTextColor = isVisible ? warningColor : textColor;
                    linePercentColor = lineTextColor;
                }
                
                // Berechne Text-Y-Position (vertikal zentriert zum Icon, wie im originalen Overlay)
                int textYForIcon = lineY; // Standard Y-Position für Text
                
                // Zeichne Icon für jede Zeile, wenn aktiviert
                if (showIcon) {
                    int iconY = lineCenterY - iconSize / 2;
                    try {
                        context.blit(
                            RenderPipelines.GUI_TEXTURED,
                            MACHTKRISTALL_ICON,
                            lineX, iconY,
                            0.0f, 0.0f,
                            iconSize, iconSize,
                            iconSize, iconSize
                        );
                        lineX += iconSize + 2; // Icon + Abstand
                        // Zentriere Text vertikal zum Icon: Icon-Mitte minus die Hälfte der Text-Höhe
                        textYForIcon = lineCenterY - client.font.lineHeight / 2;
                    } catch (Exception e) {
                        // Fallback: Zeichne Text wenn Icon nicht geladen werden kann
                    }
                    // Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
                    context.text(
                        client.font,
                        ": ",
                        lineX, textYForIcon,
                        lineTextColor,
                        true
                    );
                    lineX += client.font.width(": ");
                }
                
                // Zeichne Haupttext (vertikal zentriert zum Icon, wenn Icon aktiviert)
                context.text(
                    client.font,
                    displayText,
                    lineX, textYForIcon,
                    lineTextColor,
                    true
                );
                lineX += client.font.width(displayText);
                
                // Zeichne Prozentwert in gelb, falls vorhanden (vertikal zentriert zum Icon)
                if (slotPercentText != null) {
                    context.text(
                        client.font,
                        " " + slotPercentText,
                        lineX, textYForIcon,
                        linePercentColor,
                        true
                    );
                }
                
                lineY += lineHeight;
            }
        } else if ("recycler".equals(configKey)) {
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
            
            // Für Multi-Line-Overlay: Starte oben mit PADDING, nicht zentriert
            int lineY = 5; // PADDING
            int iconSize = showIcon ? lineHeight : 0;
            
            for (int i = 0; i < 3; i++) {
                // Lade Farben für diesen Slot aus Config
                int recyclerTextColor = 0xFFFFFFFF;
                int recyclerPercentColor = 0xFFFFFF00;
                switch (i) {
                    case 0:
                        java.awt.Color color1 = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1TextColor;
                        recyclerTextColor = (color1.getAlpha() << 24) | (color1.getRed() << 16) | (color1.getGreen() << 8) | color1.getBlue();
                        java.awt.Color percentColor1 = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1PercentColor;
                        recyclerPercentColor = (percentColor1.getAlpha() << 24) | (percentColor1.getRed() << 16) | (percentColor1.getGreen() << 8) | percentColor1.getBlue();
                        break;
                    case 1:
                        java.awt.Color color2 = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2TextColor;
                        recyclerTextColor = (color2.getAlpha() << 24) | (color2.getRed() << 16) | (color2.getGreen() << 8) | color2.getBlue();
                        java.awt.Color percentColor2 = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2PercentColor;
                        recyclerPercentColor = (percentColor2.getAlpha() << 24) | (percentColor2.getRed() << 16) | (percentColor2.getGreen() << 8) | percentColor2.getBlue();
                        break;
                    case 2:
                        java.awt.Color color3 = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3TextColor;
                        recyclerTextColor = (color3.getAlpha() << 24) | (color3.getRed() << 16) | (color3.getGreen() << 8) | color3.getBlue();
                        java.awt.Color percentColor3 = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3PercentColor;
                        recyclerPercentColor = (percentColor3.getAlpha() << 24) | (percentColor3.getRed() << 16) | (percentColor3.getGreen() << 8) | percentColor3.getBlue();
                        break;
                }
                
                int recyclerCurrentTextColor = recyclerTextColor;
                // Prüfe ob dieser Slot aktiviert ist
                boolean slotEnabled;
                switch (i) {
                    case 0:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1;
                        break;
                    case 1:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2;
                        break;
                    case 2:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3;
                        break;
                    default:
                        slotEnabled = true;
                        break;
                }
                
                // Überspringe diesen Slot, wenn er deaktiviert ist
                if (!slotEnabled) {
                    continue;
                }
                
                boolean slotActive = (i == 0 && recyclerSlot1Active) || (i == 1 && recyclerSlot2Active) || (i == 2 && recyclerSlot3Active);
                boolean slotSeparate = (i == 0 && slot1Separate) || (i == 1 && slot2Separate) || (i == 2 && slot3Separate);
                // Überspringe Slots, die nicht aktiv sind oder einzeln gerendert werden
                if (!slotActive || slotSeparate) {
                    continue;
                }
                
                CapacityData recyclerSlot = (i == 0) ? NpcAlertState.recyclerSlot1 : 
                                                          (i == 1) ? NpcAlertState.recyclerSlot2 : 
                                                          NpcAlertState.recyclerSlot3;
                int lineX = currentX;
                int lineCenterY = lineY + lineHeight / 2;
                
                // Berechne displayText
                String displayText;
                String slotPercentText = null;
                if (!recyclerSlot.isValid()) {
                    displayText = showIcon ? "Nicht im Tab-Widget" : "Recycler Slot " + (i + 1) + ": Nicht im Tab-Widget";
                } else {
                    displayText = showIcon ? recyclerSlot.getDisplayString() : "Recycler Slot " + (i + 1) + ": " + recyclerSlot.getDisplayString();
                    if (showPercent && recyclerSlot.isValid()) {
                        slotPercentText = PercentFormatter.formatPercent(recyclerSlot.current, recyclerSlot.max);
                    }
                }
                
                // Berechne Text-Y-Position (vertikal zentriert zum Icon, wie im originalen Overlay)
                int textYForIcon = lineY; // Standard Y-Position für Text
                
                // Zeichne Icon für jede Zeile, wenn aktiviert
                if (showIcon) {
                    int iconY = lineCenterY - iconSize / 2;
                    try {
                        context.blit(
                            RenderPipelines.GUI_TEXTURED,
                            RECYCLER_ICON,
                            lineX, iconY,
                            0.0f, 0.0f,
                            iconSize, iconSize,
                            iconSize, iconSize
                        );
                        lineX += iconSize + 2; // Icon + Abstand
                        // Zentriere Text vertikal zum Icon: Icon-Mitte minus die Hälfte der Text-Höhe
                        textYForIcon = lineCenterY - client.font.lineHeight / 2;
                    } catch (Exception e) {
                        // Fallback: Zeichne Text wenn Icon nicht geladen werden kann
                    }
                    // Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
                    context.text(
                        client.font,
                        ": ",
                        lineX, textYForIcon,
                        recyclerCurrentTextColor,
                        true
                    );
                    lineX += client.font.width(": ");
                }
                
                // Zeichne Haupttext (vertikal zentriert zum Icon, wenn Icon aktiviert)
                context.text(
                    client.font,
                    displayText,
                    lineX, textYForIcon,
                    recyclerCurrentTextColor,
                    true
                );
                lineX += client.font.width(displayText);
                
                // Zeichne Prozentwert mit konfigurierter Farbe, falls vorhanden (vertikal zentriert zum Icon)
                if (slotPercentText != null) {
                    context.text(
                        client.font,
                        " " + slotPercentText,
                        lineX, textYForIcon,
                        recyclerPercentColor,
                        true
                    );
                }
                
                lineY += lineHeight;
            }
        } else {
            // Einzeiliger Text
            // Für einzelne MK Slots: entferne "MK [Name]: " wenn Icon aktiviert ist
            String displayText = text;
            if (showIcon && (configKey != null && ("machtkristalleSlot1".equals(configKey) || 
                                                    "machtkristalleSlot2".equals(configKey) || 
                                                    "machtkristalleSlot3".equals(configKey)))) {
                // Entferne "MK [Name]: " oder "MK X: " Präfix wenn Icon angezeigt wird
                displayText = MachtkristallSlot.stripIconPrefix(displayText);
            }
            context.text(
                client.font,
                displayText,
                currentX, currentY,
                currentTextColor,
                true
            );
            currentX += client.font.width(displayText);
            
            // Render percent if enabled
            // Wenn Warnung aktiv ist, blinkt auch der Prozentwert rot
            if (showPercent && percentText != null) {
                int currentPercentColor = showWarning ? currentTextColor : percentColor;
                context.text(
                    client.font,
                    " " + percentText,
                    currentX, currentY,
                    currentPercentColor,
                    true
                );
                currentX += client.font.width(" " + percentText);
            }
        }
        
        matrices.popMatrix();
    }

    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }

    @Override
    public boolean isEnabled() {
        
        if (!CCLiveUtilitiesConfig.isModRuntimeEnabled()) {
            return false;
        }
if (!CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsUtilityEnabled) {
            return false;
        }
        if (!CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsOverlaysVisible) {
            return false;
        }
        
        // Hide overlay if in general_lobby dimension
        if (isInGeneralLobby()) {
            return false;
        }
        
        // Prüfe ob die Information selbst aktiviert ist
        if (!isInfoEnabled(configKey)) {
            return false;
        }
        
        // Prüfe ob das separate Overlay aktiviert ist
        return isSeparateOverlayEnabled(configKey);
    }

    @Override
    public Component getTooltip() {
        return Component.literal("NPC Alerts: " + displayName + " - Separates Overlay");
    }

    @Override
    public void resetToDefault() {
        // Setze auf Standard-Positionen basierend auf configKey
        int defaultX = 10;
        int defaultY = getDefaultY(configKey);
        saveX(configKey, defaultX);
        saveY(configKey, defaultY);
    }

    @Override
    public void resetSizeToDefault() {
        setScale(1.0f);
    }

}

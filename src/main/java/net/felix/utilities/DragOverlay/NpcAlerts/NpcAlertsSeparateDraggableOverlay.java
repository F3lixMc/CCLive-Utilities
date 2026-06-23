package net.felix.utilities.DragOverlay.NpcAlerts;

import net.felix.CCLiveUtilities;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.felix.utilities.Overall.NpcAlerts.NpcAlertsUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Draggable Overlay für einzelne NPC Alerts Overlays
 */
public class NpcAlertsSeparateDraggableOverlay implements DraggableOverlay {

    private static final float MIN_SCALE = 0.05f;
    
    private final String configKey;
    private final String displayName;
    
    // Icon Identifier für Forschung, Amboss, Schmelzofen, Seelen, Essenzen, Jäger, Machtkristalle und Recycler
    private static final Identifier FORSCHUNG_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_forschung.png");
    private static final Identifier AMBOSS_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_anvil.png");
    private static final Identifier SCHMELZOFEN_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_ofen.png");
    private static final Identifier SEELEN_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_seelen.png");
    private static final Identifier ESSENZEN_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_essences.png");
    private static final Identifier JAEGER_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_bogen.png");
    private static final Identifier KOMBO_KISTE_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_kombo_kiste.png");
    private static final Identifier MACHTKRISTALL_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_machtkristall.png");
    private static final Identifier RECYCLER_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_recycler.png");
    
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return getXFromConfig();
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
        int baseX = getXFromConfig();
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
        return getYFromConfig();
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 200;
        
        // Für Machtkristalle: berechne maximale Breite über alle Zeilen, die im Multi-Line-Overlay sind
        if ("machtkristalle".equals(configKey)) {
            boolean showIcon = getShowIcon();
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
                
                NpcAlertsUtility.MachtkristallSlot slot = NpcAlertsUtility.machtkristallSlots[i];
                int width = 0;
                
                // Wenn Icon aktiviert ist, füge Icon-Breite hinzu
                if (showIcon) {
                    int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
                    width += iconSize + 2; // Icon + Abstand
                    width += client.textRenderer.getWidth(": "); // Doppelpunkt nach Icon
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
                        displayText = NpcAlertsUtility.MachtkristallSlot.stripIconPrefix(displayText);
                    }
                }
                width += client.textRenderer.getWidth(displayText);
                
                // Prozent-Breite
                String percentText = slot.getPercentText();
                if (percentText != null) {
                    width += client.textRenderer.getWidth(" " + percentText);
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
            boolean showIcon = getShowIcon();
            boolean showPercent = getShowPercent();
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
                
                NpcAlertsUtility.CapacityData recyclerSlot = (i == 0) ? NpcAlertsUtility.recyclerSlot1 : 
                                                          (i == 1) ? NpcAlertsUtility.recyclerSlot2 : 
                                                          NpcAlertsUtility.recyclerSlot3;
                int width = 0;
                
                // Wenn Icon aktiviert ist, füge Icon-Breite hinzu
                if (showIcon) {
                    int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
                    width += iconSize + 2; // Icon + Abstand
                    width += client.textRenderer.getWidth(": "); // Doppelpunkt nach Icon
                }
                
                // Text-Breite
                String displayText;
                if (!recyclerSlot.isValid()) {
                    displayText = showIcon ? "Nicht im Tab-Widget" : "Recycler Slot " + (i + 1) + ": Nicht im Tab-Widget";
                } else {
                    displayText = showIcon ? recyclerSlot.getDisplayString() : "Recycler Slot " + (i + 1) + ": " + recyclerSlot.getDisplayString();
                }
                width += client.textRenderer.getWidth(displayText);
                
                // Prozent-Breite
                if (showPercent && recyclerSlot.isValid()) {
                    String percentText = NpcAlertsUtility.calculatePercent(recyclerSlot.current, recyclerSlot.max);
                    width += client.textRenderer.getWidth(" " + percentText);
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
            
            NpcAlertsUtility.MachtkristallSlot slot = NpcAlertsUtility.machtkristallSlots[slotIndex];
            boolean showIcon = getShowIcon();
            String percentText = slot.getPercentText();
            boolean showPercent = percentText != null;
            
            int width = 0;
            // Wenn Icon aktiviert ist, füge Icon-Breite hinzu
            if (showIcon) {
                int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
                width += iconSize + 2; // Icon + Abstand
                width += client.textRenderer.getWidth(": "); // Doppelpunkt nach Icon
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
                    displayText = NpcAlertsUtility.MachtkristallSlot.stripIconPrefix(displayText);
                }
            }
            width += client.textRenderer.getWidth(displayText);
            
            // Prozent-Breite
            if (showPercent && percentText != null) {
                width += client.textRenderer.getWidth(" " + percentText);
            }
            
            int padding = 5;
            // Berechne Breite komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
            return width + (padding * 2);
        }
        
        String text = getDisplayText();
        String percentText = getPercentText();
        boolean showPercent = getShowPercent();
        boolean showIcon = getShowIcon();
        
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
            int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
            width += iconSize + 2; // Icon + Abstand
            width += client.textRenderer.getWidth(": "); // Doppelpunkt nach Icon
        }
        width += client.textRenderer.getWidth(text);
        if (showPercent && percentText != null) {
            width += client.textRenderer.getWidth(" " + percentText);
        }
        
        int padding = 5;
        // Berechne Breite komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
        return width + (padding * 2);
    }
    
    /**
     * Berechnet die unskalierte Höhe
     */
    private int calculateUnscaledHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 20;
        int lineHeight = client.textRenderer.fontHeight + 2;
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            setXInConfig(x);
            setYInConfig(y);
            return;
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
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
        
        setXInConfig(baseX);
        setYInConfig(y);
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
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Get scale
        float scale = getScale();
        if (scale <= 0) scale = 1.0f;
        
        // Render border for edit mode (scaled)
        context.drawBorder(x, y, width, height, 0xFFFF0000);
        
        // Render background (scaled)
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // Render content with scale using matrix transformation
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        // Render actual content (same as real overlay)
        String text = getDisplayText();
        String percentText = getPercentText();
        boolean showPercent = getShowPercent();
        boolean showWarning = getShowWarning();
        boolean showIcon = getShowIcon();
        
        int currentX = 5; // Relativ zu (x, y) nach Matrix-Transformation
        int lineHeight = client.textRenderer.fontHeight + 2;
        int unscaledHeight = calculateUnscaledHeight();
        int overlayCenterY = unscaledHeight / 2;
        int currentY = overlayCenterY - client.textRenderer.fontHeight / 2;
        
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
                    context.drawTexture(
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
            context.drawText(
                client.textRenderer,
                ": ",
                currentX, currentY,
                currentTextColor,
                true
            );
            currentX += client.textRenderer.getWidth(": ");
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
                
                NpcAlertsUtility.MachtkristallSlot slot = NpcAlertsUtility.machtkristallSlots[i];
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
                        displayText = NpcAlertsUtility.MachtkristallSlot.stripIconPrefix(displayText);
                    }
                }
                
                boolean lineShowWarning = NpcAlertsUtility.shouldShowMachtkristallWarning(slot);
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
                        context.drawTexture(
                            RenderPipelines.GUI_TEXTURED,
                            MACHTKRISTALL_ICON,
                            lineX, iconY,
                            0.0f, 0.0f,
                            iconSize, iconSize,
                            iconSize, iconSize
                        );
                        lineX += iconSize + 2; // Icon + Abstand
                        // Zentriere Text vertikal zum Icon: Icon-Mitte minus die Hälfte der Text-Höhe
                        textYForIcon = lineCenterY - client.textRenderer.fontHeight / 2;
                    } catch (Exception e) {
                        // Fallback: Zeichne Text wenn Icon nicht geladen werden kann
                    }
                    // Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
                    context.drawText(
                        client.textRenderer,
                        ": ",
                        lineX, textYForIcon,
                        lineTextColor,
                        true
                    );
                    lineX += client.textRenderer.getWidth(": ");
                }
                
                // Zeichne Haupttext (vertikal zentriert zum Icon, wenn Icon aktiviert)
                context.drawText(
                    client.textRenderer,
                    displayText,
                    lineX, textYForIcon,
                    lineTextColor,
                    true
                );
                lineX += client.textRenderer.getWidth(displayText);
                
                // Zeichne Prozentwert in gelb, falls vorhanden (vertikal zentriert zum Icon)
                if (slotPercentText != null) {
                    context.drawText(
                        client.textRenderer,
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
                
                NpcAlertsUtility.CapacityData recyclerSlot = (i == 0) ? NpcAlertsUtility.recyclerSlot1 : 
                                                          (i == 1) ? NpcAlertsUtility.recyclerSlot2 : 
                                                          NpcAlertsUtility.recyclerSlot3;
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
                        slotPercentText = NpcAlertsUtility.calculatePercent(recyclerSlot.current, recyclerSlot.max);
                    }
                }
                
                // Berechne Text-Y-Position (vertikal zentriert zum Icon, wie im originalen Overlay)
                int textYForIcon = lineY; // Standard Y-Position für Text
                
                // Zeichne Icon für jede Zeile, wenn aktiviert
                if (showIcon) {
                    int iconY = lineCenterY - iconSize / 2;
                    try {
                        context.drawTexture(
                            RenderPipelines.GUI_TEXTURED,
                            RECYCLER_ICON,
                            lineX, iconY,
                            0.0f, 0.0f,
                            iconSize, iconSize,
                            iconSize, iconSize
                        );
                        lineX += iconSize + 2; // Icon + Abstand
                        // Zentriere Text vertikal zum Icon: Icon-Mitte minus die Hälfte der Text-Höhe
                        textYForIcon = lineCenterY - client.textRenderer.fontHeight / 2;
                    } catch (Exception e) {
                        // Fallback: Zeichne Text wenn Icon nicht geladen werden kann
                    }
                    // Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
                    context.drawText(
                        client.textRenderer,
                        ": ",
                        lineX, textYForIcon,
                        recyclerCurrentTextColor,
                        true
                    );
                    lineX += client.textRenderer.getWidth(": ");
                }
                
                // Zeichne Haupttext (vertikal zentriert zum Icon, wenn Icon aktiviert)
                context.drawText(
                    client.textRenderer,
                    displayText,
                    lineX, textYForIcon,
                    recyclerCurrentTextColor,
                    true
                );
                lineX += client.textRenderer.getWidth(displayText);
                
                // Zeichne Prozentwert mit konfigurierter Farbe, falls vorhanden (vertikal zentriert zum Icon)
                if (slotPercentText != null) {
                    context.drawText(
                        client.textRenderer,
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
                displayText = NpcAlertsUtility.MachtkristallSlot.stripIconPrefix(displayText);
            }
            context.drawText(
                client.textRenderer,
                displayText,
                currentX, currentY,
                currentTextColor,
                true
            );
            currentX += client.textRenderer.getWidth(displayText);
            
            // Render percent if enabled
            // Wenn Warnung aktiv ist, blinkt auch der Prozentwert rot
            if (showPercent && percentText != null) {
                int currentPercentColor = showWarning ? currentTextColor : percentColor;
                context.drawText(
                    client.textRenderer,
                    " " + percentText,
                    currentX, currentY,
                    currentPercentColor,
                    true
                );
                currentX += client.textRenderer.getWidth(" " + percentText);
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
        if (!isInfoEnabled()) {
            return false;
        }
        
        // Prüfe ob das separate Overlay aktiviert ist
        return getSeparateOverlayEnabled();
    }
    
    /**
     * Prüft, ob der Spieler in der "general_lobby" Dimension ist
     */
    private boolean isInGeneralLobby() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return false;
        }
        
        String dimensionPath = client.world.getRegistryKey().getValue().getPath();
        return dimensionPath.equals("general_lobby");
    }
    
    private boolean isInfoEnabled() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss;
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
    
    @Override
    public Text getTooltip() {
        return Text.literal("NPC Alerts: " + displayName + " - Separates Overlay");
    }
    
    @Override
    public void resetToDefault() {
        // Setze auf Standard-Positionen basierend auf configKey
        int defaultX = 10;
        int defaultY = getDefaultY();
        setXInConfig(defaultX);
        setYInConfig(defaultY);
    }
    
    @Override
    public void resetSizeToDefault() {
        setScale(1.0f);
    }
    
    private boolean getSeparateOverlayEnabled() {
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
    
    private int getXFromConfig() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungX;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossX;
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
    
    private int getYFromConfig() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungY;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossY;
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
    
    private void setXInConfig(int x) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungX = x;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossX = x;
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
    
    private void setYInConfig(int y) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungY = y;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossY = y;
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
    
    private int getDefaultY() {
        switch (configKey) {
            case "forschung":
                return 10;
            case "amboss":
                return 50;
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
    
    private String getDisplayText() {
        boolean showIcon = getShowIcon();
        switch (configKey) {
            case "forschung":
                return showIcon ? NpcAlertsUtility.forschung.getDisplayString() : "Forschung: " + NpcAlertsUtility.forschung.getDisplayString();
            case "amboss":
                return showIcon ? NpcAlertsUtility.ambossKapazitaet.getDisplayString() : "Amboss: " + NpcAlertsUtility.ambossKapazitaet.getDisplayString();
            case "schmelzofen":
                return showIcon ? NpcAlertsUtility.schmelzofenKapazitaet.getDisplayString() : "Schmelzofen: " + NpcAlertsUtility.schmelzofenKapazitaet.getDisplayString();
            case "jaeger":
                return showIcon ? NpcAlertsUtility.jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix() : "Jäger: " + NpcAlertsUtility.jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix();
            case "komboKiste": {
                String fraction = NpcAlertsUtility.getKomboKisteFractionDisplay();
                return showIcon ? fraction : "Kombo Kiste: " + fraction;
            }
            case "seelen":
                return showIcon ? NpcAlertsUtility.seelenKapazitaet.getDisplayString() : "Seelen: " + NpcAlertsUtility.seelenKapazitaet.getDisplayString();
            case "essenzen":
                return showIcon ? NpcAlertsUtility.essenzenKapazitaet.getDisplayString() : "Essenzen: " + NpcAlertsUtility.essenzenKapazitaet.getDisplayString();
            case "machtkristalle":
                // Für Machtkristalle: zeige alle 3 Slots
                // Hinweis: Prozentwerte werden separat gerendert (in gelb)
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    NpcAlertsUtility.MachtkristallSlot slot = NpcAlertsUtility.machtkristallSlots[i];
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
                NpcAlertsUtility.MachtkristallSlot slot1 = NpcAlertsUtility.machtkristallSlots[0];
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
                NpcAlertsUtility.MachtkristallSlot slot2 = NpcAlertsUtility.machtkristallSlots[1];
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
                NpcAlertsUtility.MachtkristallSlot slot3 = NpcAlertsUtility.machtkristallSlots[2];
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
                    
                    NpcAlertsUtility.CapacityData recyclerSlot = (i == 0) ? NpcAlertsUtility.recyclerSlot1 : 
                                                              (i == 1) ? NpcAlertsUtility.recyclerSlot2 : 
                                                              NpcAlertsUtility.recyclerSlot3;
                    if (recyclerSb.length() > 0) recyclerSb.append("\n");
                    String slotText = showIcon ? recyclerSlot.getDisplayString() : "Recycler Slot " + (i + 1) + ": " + recyclerSlot.getDisplayString();
                    recyclerSb.append(slotText);
                }
                return recyclerSb.toString();
            case "recyclerSlot1":
                return showIcon ? NpcAlertsUtility.recyclerSlot1.getDisplayString() : "Recycler Slot 1: " + NpcAlertsUtility.recyclerSlot1.getDisplayString();
            case "recyclerSlot2":
                return showIcon ? NpcAlertsUtility.recyclerSlot2.getDisplayString() : "Recycler Slot 2: " + NpcAlertsUtility.recyclerSlot2.getDisplayString();
            case "recyclerSlot3":
                return showIcon ? NpcAlertsUtility.recyclerSlot3.getDisplayString() : "Recycler Slot 3: " + NpcAlertsUtility.recyclerSlot3.getDisplayString();
            default:
                return displayName + ": X / Y";
        }
    }
    
    private boolean getShowIcon() {
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
    
    private String getPercentText() {
        switch (configKey) {
            case "forschung":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschungPercent) {
                    if (NpcAlertsUtility.forschung.isValid()) {
                        // Forschung zählt runter: wenn current näher an max ist, ist der Prozent höher
                        return NpcAlertsUtility.calculatePercent(
                            NpcAlertsUtility.forschung.current, 
                            NpcAlertsUtility.forschung.max
                        );
                    } else {
                        // Kein Prozent, solange keine gültigen Werte (Anzeige: "Nicht im Tab-Widget")
                        return null;
                    }
                }
                break;
            case "amboss":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent && 
                    NpcAlertsUtility.ambossKapazitaet.isValid()) {
                    return NpcAlertsUtility.calculatePercent(
                        NpcAlertsUtility.ambossKapazitaet.current, 
                        NpcAlertsUtility.ambossKapazitaet.max
                    );
                }
                break;
            case "schmelzofen":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofenPercent && 
                    NpcAlertsUtility.schmelzofenKapazitaet.isValid()) {
                    return NpcAlertsUtility.calculatePercent(
                        NpcAlertsUtility.schmelzofenKapazitaet.current, 
                        NpcAlertsUtility.schmelzofenKapazitaet.max
                    );
                }
                break;
            case "jaeger":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaegerPercent && 
                    NpcAlertsUtility.jaegerKapazitaet.isValid()) {
                    return NpcAlertsUtility.calculatePercent(
                        NpcAlertsUtility.jaegerKapazitaet.current, 
                        NpcAlertsUtility.jaegerKapazitaet.max
                    );
                }
                break;
            case "seelen":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelenPercent && 
                    NpcAlertsUtility.seelenKapazitaet.isValid()) {
                    return NpcAlertsUtility.calculatePercent(
                        NpcAlertsUtility.seelenKapazitaet.current, 
                        NpcAlertsUtility.seelenKapazitaet.max
                    );
                }
                break;
            case "essenzen":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzenPercent && 
                    NpcAlertsUtility.essenzenKapazitaet.isValid()) {
                    return NpcAlertsUtility.calculatePercent(
                        NpcAlertsUtility.essenzenKapazitaet.current, 
                        NpcAlertsUtility.essenzenKapazitaet.max
                    );
                }
                break;
            case "machtkristalleSlot1":
                // Für einzelnen MK-Slot 1
                NpcAlertsUtility.MachtkristallSlot slot1 = NpcAlertsUtility.machtkristallSlots[0];
                if (!slot1.isEmpty() && !slot1.isNotFound()) {
                    return slot1.getPercentText();
                }
                break;
            case "machtkristalleSlot2":
                // Für einzelnen MK-Slot 2
                NpcAlertsUtility.MachtkristallSlot slot2 = NpcAlertsUtility.machtkristallSlots[1];
                if (!slot2.isEmpty() && !slot2.isNotFound()) {
                    return slot2.getPercentText();
                }
                break;
            case "machtkristalleSlot3":
                // Für einzelnen MK-Slot 3
                NpcAlertsUtility.MachtkristallSlot slot3 = NpcAlertsUtility.machtkristallSlots[2];
                if (!slot3.isEmpty() && !slot3.isNotFound()) {
                    return slot3.getPercentText();
                }
                break;
            case "recycler":
                // Für Multi-Line-Overlay: Prozente werden separat für jede Zeile gerendert
                return null;
            case "recyclerSlot1":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent && 
                    NpcAlertsUtility.recyclerSlot1.isValid()) {
                    return NpcAlertsUtility.calculatePercent(
                        NpcAlertsUtility.recyclerSlot1.current, 
                        NpcAlertsUtility.recyclerSlot1.max
                    );
                }
                break;
            case "recyclerSlot2":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent && 
                    NpcAlertsUtility.recyclerSlot2.isValid()) {
                    return NpcAlertsUtility.calculatePercent(
                        NpcAlertsUtility.recyclerSlot2.current, 
                        NpcAlertsUtility.recyclerSlot2.max
                    );
                }
                break;
            case "recyclerSlot3":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent && 
                    NpcAlertsUtility.recyclerSlot3.isValid()) {
                    return NpcAlertsUtility.calculatePercent(
                        NpcAlertsUtility.recyclerSlot3.current, 
                        NpcAlertsUtility.recyclerSlot3.max
                    );
                }
                break;
        }
        return null;
    }
    
    private boolean getShowPercent() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschungPercent;
            case "amboss":
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
    
    private boolean getShowWarning() {
        switch (configKey) {
            case "forschung":
                if (NpcAlertsUtility.forschung.isValid()) {
                    int warnValue = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungWarnValue;
                    return warnValue >= 0 && NpcAlertsUtility.forschung.current <= warnValue;
                }
                break;
            case "amboss":
                if (NpcAlertsUtility.ambossKapazitaet.isValid()) {
                    double currentPercent = ((double)NpcAlertsUtility.ambossKapazitaet.current / 
                        (double)NpcAlertsUtility.ambossKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "schmelzofen":
                if (NpcAlertsUtility.schmelzofenKapazitaet.isValid()) {
                    double currentPercent = ((double)NpcAlertsUtility.schmelzofenKapazitaet.current / 
                        (double)NpcAlertsUtility.schmelzofenKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "jaeger":
                if (NpcAlertsUtility.jaegerKapazitaet.isValid()) {
                    double currentPercent = ((double)NpcAlertsUtility.jaegerKapazitaet.current / 
                        (double)NpcAlertsUtility.jaegerKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "komboKiste":
                if (NpcAlertsUtility.komboKiste.isValid()) {
                    return NpcAlertsUtility.komboKiste.current >= NpcAlertsUtility.komboKiste.max;
                }
                break;
            case "seelen":
                if (NpcAlertsUtility.seelenKapazitaet.isValid()) {
                    double currentPercent = ((double)NpcAlertsUtility.seelenKapazitaet.current / 
                        (double)NpcAlertsUtility.seelenKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "essenzen":
                if (NpcAlertsUtility.essenzenKapazitaet.isValid()) {
                    double currentPercent = ((double)NpcAlertsUtility.essenzenKapazitaet.current / 
                        (double)NpcAlertsUtility.essenzenKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "machtkristalle":
                for (int i = 0; i < 3; i++) {
                    if (NpcAlertsUtility.shouldShowMachtkristallWarning(NpcAlertsUtility.machtkristallSlots[i])) {
                        return true;
                    }
                }
                break;
            case "recyclerSlot1":
                if (NpcAlertsUtility.recyclerSlot1.isValid()) {
                    double currentPercent = ((double)NpcAlertsUtility.recyclerSlot1.current / 
                        (double)NpcAlertsUtility.recyclerSlot1.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
                    // Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
                    return warnPercent >= 0 && currentPercent <= warnPercent;
                }
                break;
            case "recyclerSlot2":
                if (NpcAlertsUtility.recyclerSlot2.isValid()) {
                    double currentPercent = ((double)NpcAlertsUtility.recyclerSlot2.current / 
                        (double)NpcAlertsUtility.recyclerSlot2.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
                    // Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
                    return warnPercent >= 0 && currentPercent <= warnPercent;
                }
                break;
            case "recyclerSlot3":
                if (NpcAlertsUtility.recyclerSlot3.isValid()) {
                    double currentPercent = ((double)NpcAlertsUtility.recyclerSlot3.current / 
                        (double)NpcAlertsUtility.recyclerSlot3.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
                    // Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
                    return warnPercent >= 0 && currentPercent <= warnPercent;
                }
                break;
            case "machtkristalleSlot1":
                return NpcAlertsUtility.shouldShowMachtkristallWarning(NpcAlertsUtility.machtkristallSlots[0]);
            case "machtkristalleSlot2":
                return NpcAlertsUtility.shouldShowMachtkristallWarning(NpcAlertsUtility.machtkristallSlots[1]);
            case "machtkristalleSlot3":
                return NpcAlertsUtility.shouldShowMachtkristallWarning(NpcAlertsUtility.machtkristallSlots[2]);
        }
        return false;
    }
}


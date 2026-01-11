package net.felix.utilities.DragOverlay.TabInfo;

import net.felix.CCLiveUtilities;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.felix.utilities.Overall.TabInfo.TabInfoUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Draggable Overlay für einzelne Tab-Info Overlays
 */
public class TabInfoSeparateDraggableOverlay implements DraggableOverlay {
    
    private final String configKey;
    private final String displayName;
    
    // Icon Identifier für Forschung, Amboss, Schmelzofen, Seelen, Essenzen, Jäger, Machtkristalle und Recycler
    private static final Identifier FORSCHUNG_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_forschung.png");
    private static final Identifier AMBOSS_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_anvil.png");
    private static final Identifier SCHMELZOFEN_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_ofen.png");
    private static final Identifier SEELEN_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_seelen.png");
    private static final Identifier ESSENZEN_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_essences.png");
    private static final Identifier JAEGER_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_bogen.png");
    private static final Identifier MACHTKRISTALL_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_machtkristall.png");
    private static final Identifier RECYCLER_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_recycler.png");
    
    public TabInfoSeparateDraggableOverlay(String infoName, String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }
    
    @Override
    public String getOverlayName() {
        return "Tab Info: " + displayName;
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
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungScale;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossScale;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenScale;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerScale;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenScale;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenScale;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleScale;
            case "machtkristalleSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Scale;
            case "machtkristalleSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Scale;
            case "machtkristalleSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Scale;
            case "recycler":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerScale;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Scale;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Scale;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Scale;
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
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungScale = scale;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossScale = scale;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenScale = scale;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerScale = scale;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenScale = scale;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenScale = scale;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleScale = scale;
                break;
            case "machtkristalleSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Scale = scale;
                break;
            case "machtkristalleSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Scale = scale;
                break;
            case "machtkristalleSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Scale = scale;
                break;
            case "recycler":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerScale = scale;
                break;
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Scale = scale;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Scale = scale;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Scale = scale;
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
            boolean slot1Separate = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Separate;
            boolean slot2Separate = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Separate;
            boolean slot3Separate = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Separate;
            
            int maxWidth = 0;
            for (int i = 0; i < 3; i++) {
                boolean slotSeparate = (i == 0 && slot1Separate) || (i == 1 && slot2Separate) || (i == 2 && slot3Separate);
                // Überspringe Slots, die einzeln gerendert werden
                if (slotSeparate) {
                    continue;
                }
                
                TabInfoUtility.MachtkristallSlot slot = TabInfoUtility.machtkristallSlots[i];
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
                        displayText = displayText.replaceFirst("^MK [^:]+: ", "");
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
            boolean recyclerSlot1Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
                                        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay;
            boolean recyclerSlot2Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
                                        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay;
            boolean recyclerSlot3Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
                                        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay;
            boolean slot1Separate = recyclerSlot1Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate;
            boolean slot2Separate = recyclerSlot2Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate;
            boolean slot3Separate = recyclerSlot3Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate;
            
            int maxWidth = 0;
            for (int i = 0; i < 3; i++) {
                // Prüfe ob dieser Slot aktiviert ist
                boolean slotEnabled;
                switch (i) {
                    case 0:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1;
                        break;
                    case 1:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2;
                        break;
                    case 2:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3;
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
                
                TabInfoUtility.CapacityData recyclerSlot = (i == 0) ? TabInfoUtility.recyclerSlot1 : 
                                                          (i == 1) ? TabInfoUtility.recyclerSlot2 : 
                                                          TabInfoUtility.recyclerSlot3;
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
                    String percentText = TabInfoUtility.calculatePercent(recyclerSlot.current, recyclerSlot.max);
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
                    slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot1;
                    break;
                case 1:
                    slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot2;
                    break;
                case 2:
                    slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot3;
                    break;
                default:
                    slotEnabled = true;
                    break;
            }
            
            // Wenn Slot deaktiviert ist, gib 0 zurück (Overlay wird nicht gerendert)
            if (!slotEnabled) {
                return 0;
            }
            
            TabInfoUtility.MachtkristallSlot slot = TabInfoUtility.machtkristallSlots[slotIndex];
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
                    displayText = displayText.replaceFirst("^MK [^:]+: ", "");
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
            boolean slot1Separate = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Separate;
            boolean slot2Separate = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Separate;
            boolean slot3Separate = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Separate;
            
            // Zähle nur die Slots, die im Multi-Line-Overlay sind UND aktiviert sind
            int lineCount = 0;
            for (int i = 0; i < 3; i++) {
                // Prüfe ob dieser Slot aktiviert ist
                boolean slotEnabled;
                switch (i) {
                    case 0:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot1;
                        break;
                    case 1:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot2;
                        break;
                    case 2:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot3;
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
            
            // Berechne die tatsächliche Zeilenhöhe unter Berücksichtigung von Icons
            int actualLineHeight = lineHeight;
            boolean showIcon = getShowIcon();
            if (showIcon) {
                int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
                actualLineHeight = Math.max(actualLineHeight, iconSize);
            }
            return (lineCount * actualLineHeight) + (padding * 2);
        }
        
        // Für Recycler: mehrere Zeilen möglich
        if ("recycler".equals(configKey)) {
            // Prüfe welche Slots aktiv sind und einzeln gerendert werden (diese werden nicht im Multi-Line-Overlay sein)
            boolean recyclerSlot1Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
                                        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay;
            boolean recyclerSlot2Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
                                        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay;
            boolean recyclerSlot3Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
                                        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay;
            boolean slot1Separate = recyclerSlot1Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate;
            boolean slot2Separate = recyclerSlot2Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate;
            boolean slot3Separate = recyclerSlot3Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate;
            
            // Zähle nur die Slots, die im Multi-Line-Overlay sind UND aktiviert sind
            int lineCount = 0;
            for (int i = 0; i < 3; i++) {
                // Prüfe ob dieser Slot aktiviert ist
                boolean slotEnabled;
                switch (i) {
                    case 0:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1;
                        break;
                    case 1:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2;
                        break;
                    case 2:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3;
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
            
            // Berechne die tatsächliche Zeilenhöhe unter Berücksichtigung von Icons
            int actualLineHeight = lineHeight;
            boolean showIcon = getShowIcon();
            if (showIcon) {
                int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
                actualLineHeight = Math.max(actualLineHeight, iconSize);
            }
            return (lineCount * actualLineHeight) + (padding * 2);
        }
        
        // Für Recycler: mehrere Zeilen möglich
        if ("recycler".equals(configKey)) {
            // Prüfe welche Slots aktiv sind und einzeln gerendert werden (diese werden nicht im Multi-Line-Overlay sein)
            boolean recyclerSlot1Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
                                        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay;
            boolean recyclerSlot2Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
                                        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay;
            boolean recyclerSlot3Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
                                        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay;
            boolean slot1Separate = recyclerSlot1Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate;
            boolean slot2Separate = recyclerSlot2Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate;
            boolean slot3Separate = recyclerSlot3Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate;
            
            // Zähle nur die Slots, die im Multi-Line-Overlay sind UND aktiviert sind
            int lineCount = 0;
            for (int i = 0; i < 3; i++) {
                // Prüfe ob dieser Slot aktiviert ist
                boolean slotEnabled;
                switch (i) {
                    case 0:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1;
                        break;
                    case 1:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2;
                        break;
                    case 2:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3;
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
            
            // Berechne die tatsächliche Zeilenhöhe unter Berücksichtigung von Icons
            int actualLineHeight = lineHeight;
            boolean showIcon = getShowIcon();
            if (showIcon) {
                int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
                actualLineHeight = Math.max(actualLineHeight, iconSize);
            }
            return (lineCount * actualLineHeight) + (padding * 2);
        }
        
        // Berechne die tatsächliche Zeilenhöhe unter Berücksichtigung von Icons
        int actualLineHeight = lineHeight;
        boolean showIcon = getShowIcon();
        if (showIcon && configKey != null && ("forschung".equals(configKey) || "amboss".equals(configKey) || 
                                               "schmelzofen".equals(configKey) || "seelen".equals(configKey) || 
                                               "essenzen".equals(configKey) || "jaeger".equals(configKey) || 
                                               "machtkristalle".equals(configKey) ||
                                               "machtkristalleSlot1".equals(configKey) || "machtkristalleSlot2".equals(configKey) || 
                                               "machtkristalleSlot3".equals(configKey) ||
                                               "recyclerSlot1".equals(configKey) || "recyclerSlot2".equals(configKey) || 
                                               "recyclerSlot3".equals(configKey))) {
            int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
            // Die tatsächliche Höhe ist das Maximum aus Icon-Höhe und Text-Höhe
            actualLineHeight = Math.max(actualLineHeight, iconSize);
        }
        
        return actualLineHeight + (padding * 2);
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
        float scale = (scaleX + scaleY) / 2.0f;
        
        // Keine Grenzen für Scale
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
        // Vertikal zentriert - berücksichtige Icon-Höhe wenn aktiviert
        int lineHeight = client.textRenderer.fontHeight + 2;
        int actualLineHeight = lineHeight;
        if (showIcon && configKey != null) {
            int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
            // Die tatsächliche Höhe ist das Maximum aus Icon-Höhe und Text-Höhe
            actualLineHeight = Math.max(actualLineHeight, iconSize);
        }
        // Berechne unskalierte Höhe für Zentrierung
        int unscaledHeight = calculateUnscaledHeight();
        // Zentriere: Overlay-Mitte, dann verschiebe nach oben um die Hälfte der fontHeight (da Text-Baseline unten ist)
        int overlayCenterY = unscaledHeight / 2;
        int currentY = overlayCenterY - client.textRenderer.fontHeight / 2;
        
        // Lade Farben aus Config (für Recycler-Slots)
        int textColor = 0xFFFFFFFF;
        int percentColor = 0xFFFFFF00;
        if (configKey != null) {
            if ("recyclerSlot1".equals(configKey)) {
                java.awt.Color color = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1TextColor;
                textColor = (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
                java.awt.Color percentColorObj = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1PercentColor;
                percentColor = (percentColorObj.getAlpha() << 24) | (percentColorObj.getRed() << 16) | (percentColorObj.getGreen() << 8) | percentColorObj.getBlue();
            } else if ("recyclerSlot2".equals(configKey)) {
                java.awt.Color color = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2TextColor;
                textColor = (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
                java.awt.Color percentColorObj = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2PercentColor;
                percentColor = (percentColorObj.getAlpha() << 24) | (percentColorObj.getRed() << 16) | (percentColorObj.getGreen() << 8) | percentColorObj.getBlue();
            } else if ("recyclerSlot3".equals(configKey)) {
                java.awt.Color color = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3TextColor;
                textColor = (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
                java.awt.Color percentColorObj = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3PercentColor;
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
                                                "machtkristalleSlot1".equals(configKey) || "machtkristalleSlot2".equals(configKey) || 
                                                "machtkristalleSlot3".equals(configKey) ||
                                                "recyclerSlot1".equals(configKey) || "recyclerSlot2".equals(configKey) || 
                                                "recyclerSlot3".equals(configKey)))) {
            int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
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
            boolean slot1Separate = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Separate;
            boolean slot2Separate = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Separate;
            boolean slot3Separate = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Separate;
            
            // Für Multi-Line-Overlay: Starte oben mit PADDING, nicht zentriert
            int lineY = 5; // PADDING
            int mkPercentColor = 0xFFFFFF00; // Gelb für Prozente
            int iconSize = showIcon ? (int)(client.textRenderer.fontHeight * 1.5) : 0;
            
            for (int i = 0; i < 3; i++) {
                // Prüfe ob dieser Slot aktiviert ist
                boolean slotEnabled;
                switch (i) {
                    case 0:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot1;
                        break;
                    case 1:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot2;
                        break;
                    case 2:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot3;
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
                
                TabInfoUtility.MachtkristallSlot slot = TabInfoUtility.machtkristallSlots[i];
                int lineX = currentX;
                int lineCenterY = lineY + actualLineHeight / 2;
                
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
                        displayText = displayText.replaceFirst("^MK [^:]+: ", "");
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
                        currentTextColor,
                        true
                    );
                    lineX += client.textRenderer.getWidth(": ");
                }
                
                // Zeichne Haupttext (vertikal zentriert zum Icon, wenn Icon aktiviert)
                context.drawText(
                    client.textRenderer,
                    displayText,
                    lineX, textYForIcon,
                    currentTextColor,
                    true
                );
                lineX += client.textRenderer.getWidth(displayText);
                
                // Zeichne Prozentwert in gelb, falls vorhanden (vertikal zentriert zum Icon)
                if (slotPercentText != null) {
                    context.drawText(
                        client.textRenderer,
                        " " + slotPercentText,
                        lineX, textYForIcon,
                        mkPercentColor,
                        true
                    );
                }
                
                lineY += actualLineHeight;
            }
        } else if ("recycler".equals(configKey)) {
            // Prüfe welche Slots aktiv sind und einzeln gerendert werden (diese werden nicht im Multi-Line-Overlay sein)
            boolean recyclerSlot1Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
                                        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay;
            boolean recyclerSlot2Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
                                        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay;
            boolean recyclerSlot3Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
                                        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay;
            boolean slot1Separate = recyclerSlot1Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate;
            boolean slot2Separate = recyclerSlot2Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate;
            boolean slot3Separate = recyclerSlot3Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate;
            
            // Für Multi-Line-Overlay: Starte oben mit PADDING, nicht zentriert
            int lineY = 5; // PADDING
            int iconSize = showIcon ? (int)(client.textRenderer.fontHeight * 1.5) : 0;
            
            for (int i = 0; i < 3; i++) {
                // Lade Farben für diesen Slot aus Config
                int recyclerTextColor = 0xFFFFFFFF;
                int recyclerPercentColor = 0xFFFFFF00;
                switch (i) {
                    case 0:
                        java.awt.Color color1 = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1TextColor;
                        recyclerTextColor = (color1.getAlpha() << 24) | (color1.getRed() << 16) | (color1.getGreen() << 8) | color1.getBlue();
                        java.awt.Color percentColor1 = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1PercentColor;
                        recyclerPercentColor = (percentColor1.getAlpha() << 24) | (percentColor1.getRed() << 16) | (percentColor1.getGreen() << 8) | percentColor1.getBlue();
                        break;
                    case 1:
                        java.awt.Color color2 = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2TextColor;
                        recyclerTextColor = (color2.getAlpha() << 24) | (color2.getRed() << 16) | (color2.getGreen() << 8) | color2.getBlue();
                        java.awt.Color percentColor2 = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2PercentColor;
                        recyclerPercentColor = (percentColor2.getAlpha() << 24) | (percentColor2.getRed() << 16) | (percentColor2.getGreen() << 8) | percentColor2.getBlue();
                        break;
                    case 2:
                        java.awt.Color color3 = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3TextColor;
                        recyclerTextColor = (color3.getAlpha() << 24) | (color3.getRed() << 16) | (color3.getGreen() << 8) | color3.getBlue();
                        java.awt.Color percentColor3 = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3PercentColor;
                        recyclerPercentColor = (percentColor3.getAlpha() << 24) | (percentColor3.getRed() << 16) | (percentColor3.getGreen() << 8) | percentColor3.getBlue();
                        break;
                }
                
                int recyclerCurrentTextColor = recyclerTextColor;
                // Prüfe ob dieser Slot aktiviert ist
                boolean slotEnabled;
                switch (i) {
                    case 0:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1;
                        break;
                    case 1:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2;
                        break;
                    case 2:
                        slotEnabled = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3;
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
                
                TabInfoUtility.CapacityData recyclerSlot = (i == 0) ? TabInfoUtility.recyclerSlot1 : 
                                                          (i == 1) ? TabInfoUtility.recyclerSlot2 : 
                                                          TabInfoUtility.recyclerSlot3;
                int lineX = currentX;
                int lineCenterY = lineY + actualLineHeight / 2;
                
                // Berechne displayText
                String displayText;
                String slotPercentText = null;
                if (!recyclerSlot.isValid()) {
                    displayText = showIcon ? "Nicht im Tab-Widget" : "Recycler Slot " + (i + 1) + ": Nicht im Tab-Widget";
                } else {
                    displayText = showIcon ? recyclerSlot.getDisplayString() : "Recycler Slot " + (i + 1) + ": " + recyclerSlot.getDisplayString();
                    if (showPercent && recyclerSlot.isValid()) {
                        slotPercentText = TabInfoUtility.calculatePercent(recyclerSlot.current, recyclerSlot.max);
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
                
                lineY += actualLineHeight;
            }
        } else {
            // Einzeiliger Text
            // Für einzelne MK Slots: entferne "MK [Name]: " wenn Icon aktiviert ist
            String displayText = text;
            if (showIcon && (configKey != null && ("machtkristalleSlot1".equals(configKey) || 
                                                    "machtkristalleSlot2".equals(configKey) || 
                                                    "machtkristalleSlot3".equals(configKey)))) {
                // Entferne "MK [Name]: " oder "MK X: " Präfix wenn Icon angezeigt wird
                displayText = displayText.replaceFirst("^MK [^:]+: ", "");
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
        if (!CCLiveUtilitiesConfig.HANDLER.instance().tabInfoUtilityEnabled) {
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
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschung;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmboss;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofen;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaeger;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelen;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzen;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle;
            case "machtkristalleSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle && 
                       CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot1;
            case "machtkristalleSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle && 
                       CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot2;
            case "machtkristalleSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle && 
                       CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot3;
            case "recycler":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 || 
                       CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 || 
                       CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3;
            default:
                return false;
        }
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Tab Info: " + displayName + " - Separates Overlay");
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
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungSeparateOverlay;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossSeparateOverlay;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenSeparateOverlay;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerSeparateOverlay;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenSeparateOverlay;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenSeparateOverlay;
            case "machtkristalle":
                // Prüfe ob separate Overlay aktiviert ist UND ob nicht alle Slots einzeln sind
                if (!CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay) {
                    return false;
                }
                // Wenn alle 3 Slots einzeln sind, gibt es kein Multi-Line-Overlay mehr
                boolean allSlotsSeparate = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Separate &&
                                          CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Separate &&
                                          CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Separate;
                return !allSlotsSeparate;
            case "recycler":
                // Prüfe ob mindestens ein Recycler-Slot "Separates Overlay" aktiviert hat
                boolean recyclerSlot1Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
                                            CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay;
                boolean recyclerSlot2Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
                                            CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay;
                boolean recyclerSlot3Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
                                            CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay;
                if (!recyclerSlot1Active && !recyclerSlot2Active && !recyclerSlot3Active) {
                    return false;
                }
                // Wenn alle 3 Slots einzeln sind, gibt es kein Multi-Line-Overlay mehr
                boolean allRecyclerSlotsSeparate = (recyclerSlot1Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate) &&
                                                   (recyclerSlot2Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate) &&
                                                   (recyclerSlot3Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate);
                return !allRecyclerSlotsSeparate;
            case "machtkristalleSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay && 
                       CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Separate &&
                       CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot1;
            case "machtkristalleSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay && 
                       CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Separate &&
                       CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot2;
            case "machtkristalleSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay && 
                       CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Separate &&
                       CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot3;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay && 
                       CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate &&
                       CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay && 
                       CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate &&
                       CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay && 
                       CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate &&
                       CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3;
            default:
                return false;
        }
    }
    
    private int getXFromConfig() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungX;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossX;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenX;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerX;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenX;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenX;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleX;
            case "machtkristalleSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1X;
            case "machtkristalleSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2X;
            case "machtkristalleSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3X;
            case "recycler":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerX;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1X;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2X;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3X;
            default:
                return 10;
        }
    }
    
    private int getYFromConfig() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungY;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossY;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenY;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerY;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenY;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenY;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleY;
            case "machtkristalleSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Y;
            case "machtkristalleSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Y;
            case "machtkristalleSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Y;
            case "recycler":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerY;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Y;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Y;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Y;
            default:
                return 10;
        }
    }
    
    private void setXInConfig(int x) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungX = x;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossX = x;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenX = x;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerX = x;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenX = x;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenX = x;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleX = x;
                break;
            case "machtkristalleSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1X = x;
                break;
            case "machtkristalleSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2X = x;
                break;
            case "machtkristalleSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3X = x;
                break;
            case "recycler":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerX = x;
                break;
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1X = x;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2X = x;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3X = x;
                break;
        }
    }
    
    private void setYInConfig(int y) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungY = y;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossY = y;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenY = y;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerY = y;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenY = y;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenY = y;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleY = y;
                break;
            case "machtkristalleSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Y = y;
                break;
            case "machtkristalleSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Y = y;
                break;
            case "machtkristalleSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Y = y;
                break;
            case "recycler":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerY = y;
                break;
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Y = y;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Y = y;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Y = y;
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
                return showIcon ? TabInfoUtility.forschung.getDisplayString() : "Forschung: " + TabInfoUtility.forschung.getDisplayString();
            case "amboss":
                return showIcon ? TabInfoUtility.ambossKapazitaet.getDisplayString() : "Amboss: " + TabInfoUtility.ambossKapazitaet.getDisplayString();
            case "schmelzofen":
                return showIcon ? TabInfoUtility.schmelzofenKapazitaet.getDisplayString() : "Schmelzofen: " + TabInfoUtility.schmelzofenKapazitaet.getDisplayString();
            case "jaeger":
                return showIcon ? TabInfoUtility.jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix() : "Jäger: " + TabInfoUtility.jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix();
            case "seelen":
                return showIcon ? TabInfoUtility.seelenKapazitaet.getDisplayString() : "Seelen: " + TabInfoUtility.seelenKapazitaet.getDisplayString();
            case "essenzen":
                return showIcon ? TabInfoUtility.essenzenKapazitaet.getDisplayString() : "Essenzen: " + TabInfoUtility.essenzenKapazitaet.getDisplayString();
            case "machtkristalle":
                // Für Machtkristalle: zeige alle 3 Slots
                // Hinweis: Prozentwerte werden separat gerendert (in gelb)
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    TabInfoUtility.MachtkristallSlot slot = TabInfoUtility.machtkristallSlots[i];
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
                TabInfoUtility.MachtkristallSlot slot1 = TabInfoUtility.machtkristallSlots[0];
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
                TabInfoUtility.MachtkristallSlot slot2 = TabInfoUtility.machtkristallSlots[1];
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
                TabInfoUtility.MachtkristallSlot slot3 = TabInfoUtility.machtkristallSlots[2];
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
                boolean recyclerSlot1Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
                                            CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay;
                boolean recyclerSlot2Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
                                            CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay;
                boolean recyclerSlot3Active = CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
                                            CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay;
                boolean slot1Separate = recyclerSlot1Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate;
                boolean slot2Separate = recyclerSlot2Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate;
                boolean slot3Separate = recyclerSlot3Active && CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate;
                
                StringBuilder recyclerSb = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    boolean slotActive = (i == 0 && recyclerSlot1Active) || (i == 1 && recyclerSlot2Active) || (i == 2 && recyclerSlot3Active);
                    boolean slotSeparate = (i == 0 && slot1Separate) || (i == 1 && slot2Separate) || (i == 2 && slot3Separate);
                    // Überspringe Slots, die nicht aktiv sind oder einzeln gerendert werden
                    if (!slotActive || slotSeparate) {
                        continue;
                    }
                    
                    TabInfoUtility.CapacityData recyclerSlot = (i == 0) ? TabInfoUtility.recyclerSlot1 : 
                                                              (i == 1) ? TabInfoUtility.recyclerSlot2 : 
                                                              TabInfoUtility.recyclerSlot3;
                    if (recyclerSb.length() > 0) recyclerSb.append("\n");
                    String slotText = showIcon ? recyclerSlot.getDisplayString() : "Recycler Slot " + (i + 1) + ": " + recyclerSlot.getDisplayString();
                    recyclerSb.append(slotText);
                }
                return recyclerSb.toString();
            case "recyclerSlot1":
                return showIcon ? TabInfoUtility.recyclerSlot1.getDisplayString() : "Recycler Slot 1: " + TabInfoUtility.recyclerSlot1.getDisplayString();
            case "recyclerSlot2":
                return showIcon ? TabInfoUtility.recyclerSlot2.getDisplayString() : "Recycler Slot 2: " + TabInfoUtility.recyclerSlot2.getDisplayString();
            case "recyclerSlot3":
                return showIcon ? TabInfoUtility.recyclerSlot3.getDisplayString() : "Recycler Slot 3: " + TabInfoUtility.recyclerSlot3.getDisplayString();
            default:
                return displayName + ": X / Y";
        }
    }
    
    private boolean getShowIcon() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungShowIcon;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossShowIcon;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenShowIcon;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenShowIcon;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenShowIcon;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerShowIcon;
            case "machtkristalle":
            case "machtkristalleSlot1":
            case "machtkristalleSlot2":
            case "machtkristalleSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleShowIcon;
            case "recycler":
                // Prüfe ob mindestens ein Recycler-Slot das Icon aktiviert hat
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1ShowIcon || 
                       CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2ShowIcon || 
                       CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3ShowIcon;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1ShowIcon;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2ShowIcon;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3ShowIcon;
            default:
                return false;
        }
    }
    
    private String getPercentText() {
        switch (configKey) {
            case "forschung":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschungPercent) {
                    if (TabInfoUtility.forschung.isValid()) {
                        // Forschung zählt runter: wenn current näher an max ist, ist der Prozent höher
                        return TabInfoUtility.calculatePercent(
                            TabInfoUtility.forschung.current, 
                            TabInfoUtility.forschung.max
                        );
                    } else {
                        // Prüfe ob "Nicht im Widget" angezeigt wird
                        String displayString = TabInfoUtility.forschung.getDisplayString();
                        if (displayString != null && displayString.contains("Nicht im Widget")) {
                            // Keine Prozentanzeige wenn "Nicht im Widget"
                            return null;
                        } else {
                            // Zeige "?%" wenn Daten noch nicht verfügbar sind
                            return "?%";
                        }
                    }
                }
                break;
            case "amboss":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmbossPercent && 
                    TabInfoUtility.ambossKapazitaet.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.ambossKapazitaet.current, 
                        TabInfoUtility.ambossKapazitaet.max
                    );
                }
                break;
            case "schmelzofen":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofenPercent && 
                    TabInfoUtility.schmelzofenKapazitaet.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.schmelzofenKapazitaet.current, 
                        TabInfoUtility.schmelzofenKapazitaet.max
                    );
                }
                break;
            case "jaeger":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaegerPercent && 
                    TabInfoUtility.jaegerKapazitaet.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.jaegerKapazitaet.current, 
                        TabInfoUtility.jaegerKapazitaet.max
                    );
                }
                break;
            case "seelen":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelenPercent && 
                    TabInfoUtility.seelenKapazitaet.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.seelenKapazitaet.current, 
                        TabInfoUtility.seelenKapazitaet.max
                    );
                }
                break;
            case "essenzen":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzenPercent && 
                    TabInfoUtility.essenzenKapazitaet.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.essenzenKapazitaet.current, 
                        TabInfoUtility.essenzenKapazitaet.max
                    );
                }
                break;
            case "machtkristalleSlot1":
                // Für einzelnen MK-Slot 1
                TabInfoUtility.MachtkristallSlot slot1 = TabInfoUtility.machtkristallSlots[0];
                if (!slot1.isEmpty() && !slot1.isNotFound()) {
                    return slot1.getPercentText();
                }
                break;
            case "machtkristalleSlot2":
                // Für einzelnen MK-Slot 2
                TabInfoUtility.MachtkristallSlot slot2 = TabInfoUtility.machtkristallSlots[1];
                if (!slot2.isEmpty() && !slot2.isNotFound()) {
                    return slot2.getPercentText();
                }
                break;
            case "machtkristalleSlot3":
                // Für einzelnen MK-Slot 3
                TabInfoUtility.MachtkristallSlot slot3 = TabInfoUtility.machtkristallSlots[2];
                if (!slot3.isEmpty() && !slot3.isNotFound()) {
                    return slot3.getPercentText();
                }
                break;
            case "recycler":
                // Für Multi-Line-Overlay: Prozente werden separat für jede Zeile gerendert
                return null;
            case "recyclerSlot1":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerPercent && 
                    TabInfoUtility.recyclerSlot1.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.recyclerSlot1.current, 
                        TabInfoUtility.recyclerSlot1.max
                    );
                }
                break;
            case "recyclerSlot2":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerPercent && 
                    TabInfoUtility.recyclerSlot2.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.recyclerSlot2.current, 
                        TabInfoUtility.recyclerSlot2.max
                    );
                }
                break;
            case "recyclerSlot3":
                if (CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerPercent && 
                    TabInfoUtility.recyclerSlot3.isValid()) {
                    return TabInfoUtility.calculatePercent(
                        TabInfoUtility.recyclerSlot3.current, 
                        TabInfoUtility.recyclerSlot3.max
                    );
                }
                break;
        }
        return null;
    }
    
    private boolean getShowPercent() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschungPercent;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmbossPercent;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofenPercent;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaegerPercent;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelenPercent;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzenPercent;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerPercent;
            default:
                return false;
        }
    }
    
    private boolean getShowWarning() {
        switch (configKey) {
            case "forschung":
                if (TabInfoUtility.forschung.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.forschung.current / (double)TabInfoUtility.forschung.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungWarnPercent;
                    // Warnung: wenn Prozent UNTER dem Warnwert ist (da Forschung runter zählt)
                    return warnPercent >= 0 && currentPercent < warnPercent;
                }
                break;
            case "amboss":
                if (TabInfoUtility.ambossKapazitaet.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.ambossKapazitaet.current / 
                        (double)TabInfoUtility.ambossKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "schmelzofen":
                if (TabInfoUtility.schmelzofenKapazitaet.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.schmelzofenKapazitaet.current / 
                        (double)TabInfoUtility.schmelzofenKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "jaeger":
                if (TabInfoUtility.jaegerKapazitaet.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.jaegerKapazitaet.current / 
                        (double)TabInfoUtility.jaegerKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "seelen":
                if (TabInfoUtility.seelenKapazitaet.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.seelenKapazitaet.current / 
                        (double)TabInfoUtility.seelenKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "essenzen":
                if (TabInfoUtility.essenzenKapazitaet.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.essenzenKapazitaet.current / 
                        (double)TabInfoUtility.essenzenKapazitaet.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenWarnPercent;
                    return warnPercent >= 0 && currentPercent >= warnPercent;
                }
                break;
            case "recyclerSlot1":
                if (TabInfoUtility.recyclerSlot1.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.recyclerSlot1.current / 
                        (double)TabInfoUtility.recyclerSlot1.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
                    // Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
                    return warnPercent >= 0 && currentPercent <= warnPercent;
                }
                break;
            case "recyclerSlot2":
                if (TabInfoUtility.recyclerSlot2.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.recyclerSlot2.current / 
                        (double)TabInfoUtility.recyclerSlot2.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
                    // Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
                    return warnPercent >= 0 && currentPercent <= warnPercent;
                }
                break;
            case "recyclerSlot3":
                if (TabInfoUtility.recyclerSlot3.isValid()) {
                    double currentPercent = ((double)TabInfoUtility.recyclerSlot3.current / 
                        (double)TabInfoUtility.recyclerSlot3.max) * 100.0;
                    double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
                    // Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
                    return warnPercent >= 0 && currentPercent <= warnPercent;
                }
                break;
            case "machtkristalleSlot1":
                TabInfoUtility.MachtkristallSlot slot1 = TabInfoUtility.machtkristallSlots[0];
                if (!slot1.isNotFound() && !slot1.isEmpty() && slot1.xpData.isValid()) {
                    String percentText = slot1.getPercentText();
                    if (percentText != null) {
                        double currentPercent = parsePercentValue(percentText);
                        double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleWarnPercent;
                        return currentPercent >= 0 && warnPercent >= 0 && currentPercent >= warnPercent;
                    }
                }
                break;
            case "machtkristalleSlot2":
                TabInfoUtility.MachtkristallSlot slot2 = TabInfoUtility.machtkristallSlots[1];
                if (!slot2.isNotFound() && !slot2.isEmpty() && slot2.xpData.isValid()) {
                    String percentText = slot2.getPercentText();
                    if (percentText != null) {
                        double currentPercent = parsePercentValue(percentText);
                        double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleWarnPercent;
                        return currentPercent >= 0 && warnPercent >= 0 && currentPercent >= warnPercent;
                    }
                }
                break;
            case "machtkristalleSlot3":
                TabInfoUtility.MachtkristallSlot slot3 = TabInfoUtility.machtkristallSlots[2];
                if (!slot3.isNotFound() && !slot3.isEmpty() && slot3.xpData.isValid()) {
                    String percentText = slot3.getPercentText();
                    if (percentText != null) {
                        double currentPercent = parsePercentValue(percentText);
                        double warnPercent = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleWarnPercent;
                        return currentPercent >= 0 && warnPercent >= 0 && currentPercent >= warnPercent;
                    }
                }
                break;
        }
        return false;
    }
    
    /**
     * Extrahiert den numerischen Prozentwert aus einem String (z.B. "10%" -> 10.0)
     */
    private static double parsePercentValue(String percentText) {
        if (percentText == null || percentText.trim().isEmpty()) {
            return -1.0;
        }
        try {
            // Entferne "%" und Leerzeichen, ersetze Komma durch Punkt
            String cleaned = percentText.replace("%", "").trim().replace(",", ".");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }
}


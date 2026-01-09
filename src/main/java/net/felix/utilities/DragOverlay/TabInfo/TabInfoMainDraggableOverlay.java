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
import java.util.List;

/**
 * Draggable Overlay für das große Tab-Info Overlay
 */
public class TabInfoMainDraggableOverlay implements DraggableOverlay {
    
    // Icon Identifier für Forschung, Amboss, Schmelzofen, Seelen, Essenzen, Jäger, Machtkristalle und Recycler
    private static final Identifier FORSCHUNG_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_forschung.png");
    private static final Identifier AMBOSS_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_anvil.png");
    private static final Identifier SCHMELZOFEN_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_ofen.png");
    private static final Identifier SEELEN_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_seelen.png");
    private static final Identifier ESSENZEN_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_essences.png");
    private static final Identifier JAEGER_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_bogen.png");
    private static final Identifier MACHTKRISTALL_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_machtkristall.png");
    private static final Identifier RECYCLER_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_recycler.png");
    
    @Override
    public String getOverlayName() {
        return "Tab Info (Haupt)";
    }
    
    @Override
    public int getX() {
        // Calculate X position using the same logic as Mining/Holzfäller overlays
        // baseX is the left edge position (like Mining overlays)
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayX;
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
        int baseX = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayX;
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
        return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayY;
    }
    
    /**
     * Berechnet die unskalierte Breite
     * Verwendet die tatsächlichen Werte aus dem originalen Overlay für genaue Breitenberechnung
     * Dynamische Verbreiterung basierend auf Inhalt (wie BossHP-Overlay)
     */
    private int calculateUnscaledWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 200; // Fallback width
        
        // Verwende die tatsächlichen Zeilen aus dem originalen Overlay für genaue Breitenberechnung
        List<TabInfoUtility.LineWithPercent> lines = TabInfoUtility.getMainOverlayLines();
        if (lines.isEmpty()) {
            // Fallback: Verwende Edit-Mode Zeilen wenn keine echten Daten verfügbar
            lines = TabInfoUtility.getMainOverlayLinesForEditMode();
        }
        // Wenn leer, verwende minimale Breite (aber nur wenn Hintergrund aktiviert ist)
        if (lines.isEmpty()) {
            if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayShowBackground) {
                return 50; // Minimale Breite
            }
            return 200; // Fallback width
        }
        
        int maxWidth = 0;
        for (TabInfoUtility.LineWithPercent line : lines) {
            int width = 0;
            // Wenn Icon aktiviert ist, füge Icon-Breite hinzu
            if (line.showIcon && (line.configKey != null && ("forschung".equals(line.configKey) || "amboss".equals(line.configKey) || 
                                                               "schmelzofen".equals(line.configKey) || "seelen".equals(line.configKey) || 
                                                               "essenzen".equals(line.configKey) || "jaeger".equals(line.configKey) || 
                                                               "machtkristalle".equals(line.configKey) ||
                                                               "recyclerSlot1".equals(line.configKey) || "recyclerSlot2".equals(line.configKey) || 
                                                               "recyclerSlot3".equals(line.configKey)))) {
                int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
                width += iconSize + 2; // Icon + Abstand
                width += client.textRenderer.getWidth(": "); // Doppelpunkt nach Icon
            }
            width += client.textRenderer.getWidth(line.text);
            // Berücksichtige Prozente in der Breitenberechnung (wie im originalen Overlay)
            // aber rendere sie nicht im F6-Overlay
            if (line.showPercent && line.percentText != null) {
                width += client.textRenderer.getWidth(" " + line.percentText); // Abstand + Prozente
            }
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        
        final int PADDING = 5;
        // Berechne Breite komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
        return maxWidth + (PADDING * 2);
    }
    
    /**
     * Berechnet die unskalierte Höhe
     */
    private int calculateUnscaledHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return 100;
        
        List<TabInfoUtility.LineWithPercent> lines = TabInfoUtility.getMainOverlayLinesForEditMode();
        // Wenn leer, verwende minimale Höhe (aber nur wenn Hintergrund aktiviert ist)
        if (lines.isEmpty()) {
            if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayShowBackground) {
                return 20; // Minimale Höhe
            }
            return 100; // Fallback
        }
        
        final int PADDING = 5;
        final int LINE_HEIGHT = client.textRenderer.fontHeight + 2;
        
        // Berechne die tatsächliche Zeilenhöhe unter Berücksichtigung von Icons
        int actualLineHeight = LINE_HEIGHT;
        int iconLineCount = 0;
        for (TabInfoUtility.LineWithPercent line : lines) {
            if (line.showIcon && line.configKey != null) {
                int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
                actualLineHeight = Math.max(actualLineHeight, iconSize);
                iconLineCount++;
            }
        }
        
        // Berücksichtige zusätzlichen Abstand für Zeilen mit Icons (2 Pixel pro Icon-Zeile)
        return (lines.size() * actualLineHeight) + (iconLineCount * 2) + (PADDING * 2);
    }
    
    @Override
    public int getWidth() {
        // Return scaled width
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayScale;
        if (scale <= 0) scale = 1.0f; // Safety check
        return Math.round(calculateUnscaledWidth() * scale);
    }
    
    @Override
    public int getHeight() {
        // Return scaled height
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayScale;
        if (scale <= 0) scale = 1.0f; // Safety check
        return Math.round(calculateUnscaledHeight() * scale);
    }
    
    @Override
    public void setPosition(int x, int y) {
        // Calculate baseX using the same logic as Mining/Holzfäller overlays
        // We need to reverse the calculation: from the actual x position, calculate baseX
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayX = x;
            CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayY = y;
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
        
        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayX = baseX;
        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayY = y;
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
        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayScale = scale;
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
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayScale;
        if (scale <= 0) scale = 1.0f;
        
        // Render border for edit mode (scaled)
        context.drawBorder(x, y, width, height, 0xFFFF0000);
        
        // Render background (scaled)
        if (CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayShowBackground) {
            context.fill(x, y, x + width, y + height, 0x80000000);
        }
        
        // Hole die Zeilen für das Overlay
        List<TabInfoUtility.LineWithPercent> lines = TabInfoUtility.getMainOverlayLinesForEditMode();
        // Rendere auch wenn leer, wenn Hintergrund aktiviert ist
        if (lines.isEmpty() && !CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayShowBackground) {
            return;
        }
        
        // Render content with scale using matrix transformation
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        // Wenn keine Zeilen vorhanden sind, rendere nur Hintergrund (wird bereits oben gerendert)
        if (lines.isEmpty()) {
            matrices.popMatrix();
            return;
        }
        
        final int PADDING = 5;
        final int LINE_HEIGHT = client.textRenderer.fontHeight + 2;
        
        // Berechne die tatsächliche Zeilenhöhe unter Berücksichtigung von Icons
        int actualLineHeight = LINE_HEIGHT;
        for (TabInfoUtility.LineWithPercent line : lines) {
            if (line.showIcon && line.configKey != null) {
                int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
                actualLineHeight = Math.max(actualLineHeight, iconSize);
            }
        }
        
        // Berechne die tatsächliche Text-Höhe (inkl. zusätzlicher Abstände für Icon-Zeilen)
        int totalTextHeight = 0;
        for (TabInfoUtility.LineWithPercent line : lines) {
            totalTextHeight += actualLineHeight;
            if (line.showIcon && line.configKey != null) {
                totalTextHeight += 2; // Zusätzlicher Abstand für Icon-Zeilen
            }
        }
        
        // Berechne unskalierte Höhe für Zentrierung
        int unscaledHeight = calculateUnscaledHeight();
        
        // Zentriere: Overlay-Mitte, dann verschiebe nach oben um die Hälfte der Text-Höhe
        int overlayCenterY = unscaledHeight / 2;
        int currentY = overlayCenterY - totalTextHeight / 2;
        int textColor = 0xFFFFFFFF;
        
        int xPosition = 0; // Relativ zu (x, y) nach Matrix-Transformation
        
        // Rendere alle Zeilen (gleiche Logik wie im Original)
        for (TabInfoUtility.LineWithPercent line : lines) {
            if (line.text == null || line.text.trim().isEmpty()) {
                currentY += actualLineHeight;
                continue;
            }
            
            try {
                int currentX = xPosition + PADDING;
                
                // Zeichne Icon statt Text, wenn aktiviert (für Forschung, Amboss, Schmelzofen, Seelen, Essenzen, Jäger, Machtkristalle und Recycler)
                if (line.showIcon && (line.configKey != null)) {
                    int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
                    int lineCenterY = currentY + actualLineHeight / 2;
                    int iconY = lineCenterY - iconSize / 2;
                    Identifier iconToUse = null;
                    String fallbackText = null;
                    
                    if ("forschung".equals(line.configKey)) {
                        iconToUse = FORSCHUNG_ICON;
                        fallbackText = "Forschung: ";
                    } else if ("amboss".equals(line.configKey)) {
                        iconToUse = AMBOSS_ICON;
                        fallbackText = "Amboss: ";
                    } else if ("schmelzofen".equals(line.configKey)) {
                        iconToUse = SCHMELZOFEN_ICON;
                        fallbackText = "Schmelzofen: ";
                    } else if ("seelen".equals(line.configKey)) {
                        iconToUse = SEELEN_ICON;
                        fallbackText = "Seelen: ";
                    } else if ("essenzen".equals(line.configKey)) {
                        iconToUse = ESSENZEN_ICON;
                        fallbackText = "Essenzen: ";
                    } else if ("jaeger".equals(line.configKey)) {
                        iconToUse = JAEGER_ICON;
                        fallbackText = "Jäger: ";
                    } else if ("machtkristalle".equals(line.configKey)) {
                        iconToUse = MACHTKRISTALL_ICON;
                        fallbackText = "MK: ";
                    } else if ("recyclerSlot1".equals(line.configKey)) {
                        iconToUse = RECYCLER_ICON;
                        fallbackText = "Recycler Slot 1: ";
                    } else if ("recyclerSlot2".equals(line.configKey)) {
                        iconToUse = RECYCLER_ICON;
                        fallbackText = "Recycler Slot 2: ";
                    } else if ("recyclerSlot3".equals(line.configKey)) {
                        iconToUse = RECYCLER_ICON;
                        fallbackText = "Recycler Slot 3: ";
                    }
                    
                    if (iconToUse != null) {
                        boolean iconDrawn = false;
                        int textYForIcon = currentY;
                        try {
                            context.drawTexture(
                                RenderPipelines.GUI_TEXTURED,
                                iconToUse,
                                currentX, iconY,
                                0.0f, 0.0f,
                                iconSize, iconSize,
                                iconSize, iconSize
                            );
                            currentX += iconSize + 2;
                            iconDrawn = true;
                            textYForIcon = lineCenterY - client.textRenderer.fontHeight / 2;
                        } catch (Exception e) {
                            // Fallback: Zeichne Text wenn Icon nicht geladen werden kann
                            if (fallbackText != null) {
                                context.drawText(
                                    client.textRenderer,
                                    Text.literal(fallbackText),
                                    currentX,
                                    currentY,
                                    textColor,
                                    true
                                );
                                currentX += client.textRenderer.getWidth(fallbackText);
                            }
                        }
                        // Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
                        if (iconDrawn) {
                            context.drawText(
                                client.textRenderer,
                                Text.literal(": "),
                                currentX,
                                textYForIcon,
                                textColor,
                                true
                            );
                            currentX += client.textRenderer.getWidth(": ");
                        }
                        // Zeichne die Werte nach dem Doppelpunkt (vertikal zentriert zum Icon)
                        context.drawText(
                            client.textRenderer,
                            Text.literal(line.text),
                            currentX,
                            textYForIcon,
                            textColor,
                            true
                        );
                        currentX += client.textRenderer.getWidth(line.text);
                    } else {
                        // Fallback: Zeichne normalen Text
                        context.drawText(
                            client.textRenderer,
                            Text.literal(line.text),
                            currentX,
                            currentY,
                            textColor,
                            true
                        );
                        currentX += client.textRenderer.getWidth(line.text);
                    }
                } else {
                    // Zeichne Haupttext (inkl. "Amboss:" wenn kein Icon)
                    context.drawText(
                        client.textRenderer,
                        Text.literal(line.text),
                        currentX,
                        currentY,
                        textColor,
                        true
                    );
                    currentX += client.textRenderer.getWidth(line.text);
                }
                
                // Prozente werden im F6-Overlay nicht gerendert (nur im originalen Overlay)
                // Die Breite wird aber trotzdem berücksichtigt, damit die Größe übereinstimmt
            } catch (Exception e) {
                // Ignoriere Fehler
            }
            
            // Verwende actualLineHeight statt LINE_HEIGHT, um größere Icons zu berücksichtigen
            int lineSpacing = actualLineHeight;
            if (line.showIcon && line.configKey != null) {
                lineSpacing += 2; // Zusätzlicher Abstand für Icon-Zeilen
            }
            currentY += lineSpacing;
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
        
        // Prüfe ob es Zeilen gibt, die im Haupt-Overlay angezeigt werden sollen
        List<TabInfoUtility.LineWithPercent> lines = TabInfoUtility.getMainOverlayLines();
        return !lines.isEmpty();
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
    
    @Override
    public Text getTooltip() {
        return Text.literal("Tab Info Haupt-Overlay - Zeigt alle Tab-Informationen");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayX = 5;
        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayY = 5;
    }
    
    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayScale = 1.0f;
    }
}

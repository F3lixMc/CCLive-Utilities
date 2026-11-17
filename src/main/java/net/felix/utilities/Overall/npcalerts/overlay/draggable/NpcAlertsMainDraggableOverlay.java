package net.felix.utilities.Overall.npcalerts.overlay.draggable;

import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.felix.utilities.Overall.npcalerts.overlay.AlertIcons;
import net.felix.utilities.Overall.npcalerts.overlay.AlertSettings;
import net.felix.utilities.Overall.npcalerts.overlay.LineWithPercent;
import net.felix.utilities.Overall.npcalerts.overlay.MainOverlayLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import static net.felix.utilities.Overall.npcalerts.NpcAlertsFeature.isInGeneralLobby;

/**
 * Draggable Overlay für das große NPC Alerts Overlay
 */
public class NpcAlertsMainDraggableOverlay implements DraggableOverlay {

    private static final float MIN_SCALE = 0.05f;

    private static final int OVERLAY_PADDING = 5;
    private static final int WARNING_COLOR = 0xFFFF0000;
    private static final long WARNING_BLINK_MILLIS = 300;

    @Override
    public String getOverlayName() {
        return "NPC Alerts (Haupt)";
    }

    @Override
    public int getX() {
        // Calculate X position using the same logic as Mining/Holzfäller overlays
        // baseX is the left edge position (like Mining overlays)
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayX;
        }
        
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int baseX = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayX;
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
        return CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayY;
    }

    /**
     * Berechnet die unskalierte Breite
     * Verwendet die tatsächlichen Werte aus dem originalen Overlay für genaue Breitenberechnung
     * Dynamische Verbreiterung basierend auf Inhalt (wie BossHP-Overlay)
     */
    private int calculateUnscaledWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return 200; // Fallback width
        
        // Verwende die tatsächlichen Zeilen aus dem originalen Overlay für genaue Breitenberechnung
        List<LineWithPercent> lines = MainOverlayLines.getMainOverlayLines();
        if (lines.isEmpty()) {
            // Fallback: Verwende Edit-Mode Zeilen wenn keine echten Daten verfügbar
            lines = MainOverlayLines.getMainOverlayLinesForEditMode();
        }
        // Wenn leer, verwende minimale Breite (aber nur wenn Hintergrund aktiviert ist)
        if (lines.isEmpty()) {
            if (CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayShowBackground) {
                return 50; // Minimale Breite
            }
            return 200; // Fallback width
        }
        
        int maxWidth = 0;
        for (LineWithPercent line : lines) {
            maxWidth = Math.max(maxWidth, measureLineWidth(client, line));
        }
        // Berechne Breite komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
        return maxWidth + (OVERLAY_PADDING * 2);
    }

    /**
     * Breite einer Zeile inkl. Icon und Prozentwert (der Prozentwert wird im F6-Overlay
     * nicht gerendert, aber berücksichtigt, damit die Größe mit dem echten Overlay übereinstimmt).
     */
    private static int measureLineWidth(Minecraft client, LineWithPercent line) {
        int width = 0;
        if (line.showIcon && AlertIcons.iconForKey(line.configKey) != null) {
            int iconSize = (int) (client.font.lineHeight * 1.5);
            width += iconSize + 2; // Icon + Abstand
            width += client.font.width(": "); // Doppelpunkt nach Icon
        }
        width += client.font.width(line.text);
        if (line.showPercent && line.percentText != null) {
            width += client.font.width(" " + line.percentText); // Abstand + Prozente
        }
        return width;
    }
    /**
     * Berechnet die unskalierte Höhe
     */
    private int calculateUnscaledHeight() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return 100;
        
        List<LineWithPercent> lines = MainOverlayLines.getMainOverlayLinesForEditMode();
        // Wenn leer, verwende minimale Höhe (aber nur wenn Hintergrund aktiviert ist)
        if (lines.isEmpty()) {
            if (CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayShowBackground) {
                return 20; // Minimale Höhe
            }
            return 100; // Fallback
        }
        
        final int PADDING = 5;
        final int LINE_HEIGHT = client.font.lineHeight + 2;
        
        return (lines.size() * LINE_HEIGHT) + (PADDING * 2);
    }

    @Override
    public int getWidth() {
        // Return scaled width
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayScale;
        if (scale <= 0) scale = 1.0f; // Safety check
        return Math.round(calculateUnscaledWidth() * scale);
    }

    @Override
    public int getHeight() {
        // Return scaled height
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayScale;
        if (scale <= 0) scale = 1.0f; // Safety check
        return Math.round(calculateUnscaledHeight() * scale);
    }

    @Override
    public void setPosition(int x, int y) {
        // Calculate baseX using the same logic as Mining/Holzfäller overlays
        // We need to reverse the calculation: from the actual x position, calculate baseX
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayX = x;
            CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayY = y;
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
        
        CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayX = baseX;
        CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayY = y;
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
        CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayScale = scale;
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
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayScale;
        if (scale <= 0) scale = 1.0f;
        
        // Render border for edit mode (scaled)
        context.outline(x, y, width, height, 0xFFFF0000);
        
        // Render background (scaled)
        if (CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayShowBackground) {
            context.fill(x, y, x + width, y + height, 0x80000000);
        }
        
        // Hole die Zeilen für das Overlay
        List<LineWithPercent> lines = MainOverlayLines.getMainOverlayLinesForEditMode();
        // Rendere auch wenn leer, wenn Hintergrund aktiviert ist
        if (lines.isEmpty() && !CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayShowBackground) {
            return;
        }
        
        // Render content with scale using matrix transformation
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);

        // Ohne Zeilen wird nur der Hintergrund gezeichnet (bereits oben)
        if (!lines.isEmpty()) {
            drawLines(context, client, lines);
        }

        matrices.popMatrix();
    }

    /** Zeichnet alle Zeilen untereinander, vertikal zentriert im Overlay (gleiche Logik wie im Original). */
    private void drawLines(GuiGraphicsExtractor context, Minecraft client, List<LineWithPercent> lines) {
        int lineHeight = client.font.lineHeight + 2;
        int totalTextHeight = lines.size() * lineHeight;
        int overlayCenterY = calculateUnscaledHeight() / 2;
        int currentY = overlayCenterY - totalTextHeight / 2;

        for (LineWithPercent line : lines) {
            if (line.text == null || line.text.trim().isEmpty()) {
                currentY += lineHeight;
                continue;
            }

            int textColor = AlertSettings.getTextColorForConfigKey(line.configKey);
            try {
                drawLine(context, client, line, currentY, lineHeight, textColor);
            } catch (Exception e) {
                // Ignoriere Fehler
            }

            int lineSpacing = lineHeight;
            if (line.showIcon && line.configKey != null) {
                lineSpacing += 2; // Zusätzlicher Abstand für Icon-Zeilen
            }
            currentY += lineSpacing;
        }
    }

    /**
     * Zeichnet eine Zeile: Icon + ": " + Wert oder (ohne Icon) der komplette Text.
     * Prozente werden im F6-Overlay nicht gerendert, nur in der Breite berücksichtigt.
     */
    private static void drawLine(GuiGraphicsExtractor context, Minecraft client, LineWithPercent line,
                                 int currentY, int lineHeight, int textColor) {
        int currentX = OVERLAY_PADDING;
        // Rot und blinkend, wenn eine Warnung aktiv ist
        int currentTextColor = line.showWarning ? blinkingWarningColor(textColor) : textColor;

        Identifier icon = line.showIcon ? AlertIcons.iconForKey(line.configKey) : null;
        if (icon == null) {
            // Haupttext (inkl. "Amboss:" wenn kein Icon)
            context.text(client.font, Component.literal(line.text), currentX, currentY, currentTextColor, true);
            return;
        }

        int iconSize = lineHeight;
        int lineCenterY = currentY + lineHeight / 2;
        boolean iconDrawn = false;
        int textY = currentY;
        try {
            context.blit(
                RenderPipelines.GUI_TEXTURED,
                icon,
                currentX, lineCenterY - iconSize / 2,
                0.0f, 0.0f,
                iconSize, iconSize,
                iconSize, iconSize
            );
            currentX += iconSize + 2;
            iconDrawn = true;
            textY = lineCenterY - client.font.lineHeight / 2;
        } catch (Exception e) {
            // Fallback: Zeichne Text wenn Icon nicht geladen werden kann
            String fallbackText = AlertIcons.fallbackLabelForKey(line.configKey);
            if (fallbackText != null) {
                context.text(client.font, Component.literal(fallbackText), currentX, currentY, currentTextColor, true);
                currentX += client.font.width(fallbackText);
            }
        }
        // Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
        if (iconDrawn) {
            context.text(client.font, Component.literal(": "), currentX, textY, currentTextColor, true);
            currentX += client.font.width(": ");
        }
        // Werte nach dem Doppelpunkt (vertikal zentriert zum Icon)
        context.text(client.font, Component.literal(line.text), currentX, textY, currentTextColor, true);
    }

    /** Wechselt alle 300 ms zwischen Warnfarbe (Rot) und der normalen Textfarbe. */
    private static int blinkingWarningColor(int normalColor) {
        boolean isVisible = (System.currentTimeMillis() / WARNING_BLINK_MILLIS) % 2 == 0;
        return isVisible ? WARNING_COLOR : normalColor;
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
        
        // Prüfe ob es Zeilen gibt, die im Haupt-Overlay angezeigt werden sollen
        List<LineWithPercent> lines = MainOverlayLines.getMainOverlayLines();
        return !lines.isEmpty();
    }

    @Override
    public Component getTooltip() {
        return Component.literal("NPC Alerts Haupt-Overlay - Zeigt alle NPC Alertsrmationen");
    }

    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayX = 5;
        CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayY = 5;
    }

    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayScale = 1.0f;
    }

}

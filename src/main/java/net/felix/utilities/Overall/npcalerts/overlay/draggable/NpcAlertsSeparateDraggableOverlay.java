package net.felix.utilities.Overall.npcalerts.overlay.draggable;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.felix.utilities.Overall.npcalerts.data.CapacityData;
import net.felix.utilities.Overall.npcalerts.data.MachtkristallSlot;
import net.felix.utilities.Overall.npcalerts.data.NpcAlertState;
import net.felix.utilities.Overall.npcalerts.message.ScreenMessages;
import net.felix.utilities.Overall.npcalerts.overlay.AlertIcons;
import net.felix.utilities.Overall.npcalerts.overlay.AlertSettings;
import net.felix.utilities.Overall.npcalerts.overlay.PercentFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import static net.felix.utilities.Overall.npcalerts.NpcAlertsFeature.isInGeneralLobby;
import static net.felix.utilities.Overall.npcalerts.config.SeparateOverlayConfig.getDefaultY;
import static net.felix.utilities.Overall.npcalerts.config.SeparateOverlayConfig.isIconVisible;
import static net.felix.utilities.Overall.npcalerts.config.SeparateOverlayConfig.isInfoEnabled;
import static net.felix.utilities.Overall.npcalerts.config.SeparateOverlayConfig.isPercentVisible;
import static net.felix.utilities.Overall.npcalerts.config.SeparateOverlayConfig.isSeparateOverlayEnabled;
import static net.felix.utilities.Overall.npcalerts.config.SeparateOverlayConfig.loadX;
import static net.felix.utilities.Overall.npcalerts.config.SeparateOverlayConfig.loadY;
import static net.felix.utilities.Overall.npcalerts.config.SeparateOverlayConfig.saveX;
import static net.felix.utilities.Overall.npcalerts.config.SeparateOverlayConfig.saveY;
import static net.felix.utilities.Overall.npcalerts.overlay.SeparateOverlayContent.getDisplayText;
import static net.felix.utilities.Overall.npcalerts.overlay.SeparateOverlayContent.getPercentText;
import static net.felix.utilities.Overall.npcalerts.overlay.SeparateOverlayContent.isWarningVisible;
import static net.felix.utilities.Overall.npcalerts.overlay.SeparateOverlayContent.machtkristallDisplayText;

/**
 * Draggable Overlay für einzelne NPC Alerts Overlays
 */
public class NpcAlertsSeparateDraggableOverlay implements DraggableOverlay {

    private static final float MIN_SCALE = 0.05f;
    private static final int OVERLAY_PADDING = 5;
    private static final int WARNING_COLOR = 0xFFFF0000;
    private static final int PERCENT_COLOR_DEFAULT = 0xFFFFFF00;
    private static final long WARNING_BLINK_MILLIS = 300;

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

        if ("machtkristalle".equals(configKey)) {
            return measureMachtkristallOverlayWidth(client);
        }
        if ("recycler".equals(configKey)) {
            return measureRecyclerOverlayWidth(client);
        }
        if (isMachtkristallSlotKey(configKey)) {
            return measureSingleMachtkristallSlotWidth(client);
        }
        return measureSingleLineWidth(client);
    }

    /** Breite des Multi-Line-Overlays: breiteste Zeile aller Machtkristall-Slots, die nicht einzeln gerendert werden. */
    private int measureMachtkristallOverlayWidth(Minecraft client) {
        boolean showIcon = isIconVisible(configKey);
        int maxWidth = 0;
        for (int i = 0; i < 3; i++) {
            // Wie bisher werden hier auch deaktivierte Slots berücksichtigt (nur "einzeln" wird übersprungen)
            if (isMachtkristallSlotSeparate(i)) {
                continue;
            }
            MachtkristallSlot slot = NpcAlertState.machtkristallSlots[i];
            int width = iconPrefixWidth(client, showIcon)
                + client.font.width(machtkristallDisplayText(slot, i, showIcon))
                + percentSuffixWidth(client, slot.getPercentText());
            maxWidth = Math.max(maxWidth, width);
        }
        return maxWidth + OVERLAY_PADDING * 2;
    }

    /** Breite des Multi-Line-Overlays: breiteste Zeile aller Recycler-Slots im gemeinsamen Overlay. */
    private int measureRecyclerOverlayWidth(Minecraft client) {
        boolean showIcon = isIconVisible(configKey);
        boolean showPercent = isPercentVisible(configKey);
        int maxWidth = 0;
        for (int i : AlertSettings.getRecyclerSlotsInSharedOverlay()) {
            CapacityData slot = recyclerSlotAt(i);
            int width = iconPrefixWidth(client, showIcon)
                + client.font.width(recyclerDisplayText(slot, i, showIcon))
                + percentSuffixWidth(client, recyclerPercentText(slot, showPercent));
            maxWidth = Math.max(maxWidth, width);
        }
        return maxWidth + OVERLAY_PADDING * 2;
    }

    /** Breite eines einzelnen Machtkristall-Slot-Overlays (0, wenn der Slot deaktiviert ist). */
    private int measureSingleMachtkristallSlotWidth(Minecraft client) {
        int slotIndex = machtkristallSlotIndex(configKey);
        if (!AlertSettings.isNpcAlertMachtkristallSlotEnabled(slotIndex)) {
            return 0;
        }
        MachtkristallSlot slot = NpcAlertState.machtkristallSlots[slotIndex];
        boolean showIcon = isIconVisible(configKey);
        int width = iconPrefixWidth(client, showIcon)
            + client.font.width(machtkristallDisplayText(slot, slotIndex, showIcon))
            + percentSuffixWidth(client, slot.getPercentText());
        return width + OVERLAY_PADDING * 2;
    }

    private int measureSingleLineWidth(Minecraft client) {
        String text = getDisplayText(configKey, displayName);
        String percentText = isPercentVisible(configKey) ? getPercentText(configKey) : null;
        boolean showIcon = isIconVisible(configKey) && AlertIcons.iconForKey(configKey) != null;
        int width = iconPrefixWidth(client, showIcon)
            + client.font.width(text)
            + percentSuffixWidth(client, percentText);
        return width + OVERLAY_PADDING * 2;
    }

    /** Breite von Icon, Abstand und Doppelpunkt (0 ohne Icon). */
    private static int iconPrefixWidth(Minecraft client, boolean showIcon) {
        if (!showIcon) {
            return 0;
        }
        int iconSize = (int) (client.font.lineHeight * 1.5);
        return iconSize + 2 + client.font.width(": ");
    }

    private static int percentSuffixWidth(Minecraft client, String percentText) {
        return percentText == null ? 0 : client.font.width(" " + percentText);
    }

    /**
     * Berechnet die unskalierte Höhe
     */
    private int calculateUnscaledHeight() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return 20;
        int lineHeight = client.font.lineHeight + 2;

        int lineCount = 1;
        if ("machtkristalle".equals(configKey)) {
            lineCount = AlertSettings.getMachtkristallSlotsInSharedOverlay().size();
        } else if ("recycler".equals(configKey)) {
            lineCount = AlertSettings.getRecyclerSlotsInSharedOverlay().size();
        }
        return lineCount * lineHeight + OVERLAY_PADDING * 2;
    }

    private static boolean isMachtkristallSlotKey(String key) {
        return "machtkristalleSlot1".equals(key) || "machtkristalleSlot2".equals(key) || "machtkristalleSlot3".equals(key);
    }

    private static int machtkristallSlotIndex(String key) {
        return "machtkristalleSlot1".equals(key) ? 0 : ("machtkristalleSlot2".equals(key) ? 1 : 2);
    }

    private static boolean isMachtkristallSlotSeparate(int slotIndex) {
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        return switch (slotIndex) {
            case 0 -> config.npcAlertsMachtkristalleSlot1Separate;
            case 1 -> config.npcAlertsMachtkristalleSlot2Separate;
            case 2 -> config.npcAlertsMachtkristalleSlot3Separate;
            default -> false;
        };
    }

    private static CapacityData recyclerSlotAt(int slotIndex) {
        return switch (slotIndex) {
            case 0 -> NpcAlertState.recyclerSlot1;
            case 1 -> NpcAlertState.recyclerSlot2;
            default -> NpcAlertState.recyclerSlot3;
        };
    }

    private static String recyclerDisplayText(CapacityData slot, int slotIndex, boolean showIcon) {
        if (!slot.isValid()) {
            return showIcon ? "Nicht im Tab-Widget" : "Recycler Slot " + (slotIndex + 1) + ": Nicht im Tab-Widget";
        }
        return showIcon ? slot.getDisplayString() : "Recycler Slot " + (slotIndex + 1) + ": " + slot.getDisplayString();
    }

    private static String recyclerPercentText(CapacityData slot, boolean showPercent) {
        return showPercent && slot.isValid() ? PercentFormatter.formatPercent(slot.current, slot.max) : null;
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

        float scale = getScale();
        if (scale <= 0) scale = 1.0f;

        // Rahmen und Hintergrund im Edit-Modus (skaliert)
        context.outline(x, y, width, height, 0xFFFF0000);
        context.fill(x, y, x + width, y + height, 0x80000000);

        // Inhalt mit Skalierung über die Matrix zeichnen
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);

        drawContent(context, client);

        matrices.popMatrix();
    }

    /** Zeichnet den Inhalt wie das echte Overlay (relativ zu (x, y) nach der Matrix-Transformation). */
    private void drawContent(GuiGraphicsExtractor context, Minecraft client) {
        boolean showIcon = isIconVisible(configKey);
        int lineHeight = client.font.lineHeight + 2;
        if ("machtkristalle".equals(configKey)) {
            drawMachtkristallLines(context, client, showIcon, lineHeight);
        } else if ("recycler".equals(configKey)) {
            drawRecyclerLines(context, client, showIcon, lineHeight);
        } else {
            drawSingleLine(context, client, showIcon, lineHeight);
        }
    }

    /** Einzeiliges Overlay: optional Icon mit Doppelpunkt, Text und Prozentwert. */
    private void drawSingleLine(GuiGraphicsExtractor context, Minecraft client, boolean showIcon, int lineHeight) {
        String text = getDisplayText(configKey, displayName);
        String percentText = getPercentText(configKey);
        boolean showPercent = isPercentVisible(configKey);
        boolean showWarning = isWarningVisible(configKey);

        int overlayCenterY = calculateUnscaledHeight() / 2;
        int currentY = overlayCenterY - client.font.lineHeight / 2;
        int currentX = OVERLAY_PADDING;

        // Farben aus der Config gibt es nur für die Recycler-Slots
        int textColor = 0xFFFFFFFF;
        int percentColor = PERCENT_COLOR_DEFAULT;
        if (isRecyclerSlotKey(configKey)) {
            textColor = AlertSettings.getTextColorForConfigKey(configKey);
            percentColor = AlertSettings.getPercentColorForConfigKey(configKey);
        }
        // Rot und blinkend, wenn eine Warnung aktiv ist
        int currentTextColor = showWarning ? blinkingWarningColor(textColor) : textColor;

        Identifier icon = AlertIcons.iconForKey(configKey);
        if (showIcon && icon != null) {
            int iconSize = lineHeight;
            int iconY = overlayCenterY - iconSize / 2;
            if (blitIcon(context, icon, currentX, iconY, iconSize)) {
                currentX += iconSize + 2;
            }
            context.text(client.font, ": ", currentX, currentY, currentTextColor, true);
            currentX += client.font.width(": ");
        }

        // Einzelne MK-Slots: "MK [Name]: " entfernen, wenn das Icon angezeigt wird
        String displayText = text;
        if (showIcon && isMachtkristallSlotKey(configKey)) {
            displayText = MachtkristallSlot.stripIconPrefix(displayText);
        }
        context.text(client.font, displayText, currentX, currentY, currentTextColor, true);
        currentX += client.font.width(displayText);

        // Bei aktiver Warnung blinkt auch der Prozentwert rot
        if (showPercent && percentText != null) {
            int currentPercentColor = showWarning ? currentTextColor : percentColor;
            context.text(client.font, " " + percentText, currentX, currentY, currentPercentColor, true);
        }
    }

    /** Multi-Line-Overlay mit allen Machtkristall-Slots, die nicht einzeln gerendert werden. */
    private void drawMachtkristallLines(GuiGraphicsExtractor context, Minecraft client, boolean showIcon, int lineHeight) {
        int lineY = OVERLAY_PADDING;
        for (int i : AlertSettings.getMachtkristallSlotsInSharedOverlay()) {
            MachtkristallSlot slot = NpcAlertState.machtkristallSlots[i];
            String displayText = machtkristallDisplayText(slot, i, showIcon);
            String slotPercentText = (slot.isNotFound() || slot.isEmpty()) ? null : slot.getPercentText();

            int lineTextColor = 0xFFFFFFFF;
            int linePercentColor = PERCENT_COLOR_DEFAULT;
            if (ScreenMessages.shouldShowMachtkristallWarning(slot)) {
                lineTextColor = blinkingWarningColor(lineTextColor);
                linePercentColor = lineTextColor;
            }

            drawIconTextLine(context, client, showIcon ? AlertIcons.MACHTKRISTALL_ICON : null, lineY, lineHeight,
                displayText, slotPercentText, lineTextColor, linePercentColor);
            lineY += lineHeight;
        }
    }

    /** Multi-Line-Overlay mit allen Recycler-Slots im gemeinsamen Overlay (Farben je Slot aus der Config). */
    private void drawRecyclerLines(GuiGraphicsExtractor context, Minecraft client, boolean showIcon, int lineHeight) {
        boolean showPercent = isPercentVisible(configKey);
        int lineY = OVERLAY_PADDING;
        for (int i : AlertSettings.getRecyclerSlotsInSharedOverlay()) {
            CapacityData slot = recyclerSlotAt(i);
            String slotKey = "recyclerSlot" + (i + 1);
            drawIconTextLine(context, client, showIcon ? AlertIcons.RECYCLER_ICON : null, lineY, lineHeight,
                recyclerDisplayText(slot, i, showIcon), recyclerPercentText(slot, showPercent),
                AlertSettings.getTextColorForConfigKey(slotKey), AlertSettings.getPercentColorForConfigKey(slotKey));
            lineY += lineHeight;
        }
    }

    /**
     * Eine Zeile eines Multi-Line-Overlays: optional Icon + Doppelpunkt (icon != null), Text, Prozentwert.
     * Der Text wird vertikal zum Icon zentriert.
     */
    private static void drawIconTextLine(GuiGraphicsExtractor context, Minecraft client, Identifier icon, int lineY,
                                         int lineHeight, String displayText, String percentText,
                                         int textColor, int percentColor) {
        int lineX = OVERLAY_PADDING;
        int lineCenterY = lineY + lineHeight / 2;
        int textY = lineY;

        if (icon != null) {
            int iconSize = lineHeight;
            if (blitIcon(context, icon, lineX, lineCenterY - iconSize / 2, iconSize)) {
                lineX += iconSize + 2;
                textY = lineCenterY - client.font.lineHeight / 2;
            }
            context.text(client.font, ": ", lineX, textY, textColor, true);
            lineX += client.font.width(": ");
        }

        context.text(client.font, displayText, lineX, textY, textColor, true);
        lineX += client.font.width(displayText);

        if (percentText != null) {
            context.text(client.font, " " + percentText, lineX, textY, percentColor, true);
        }
    }

    /** Zeichnet das Icon; false, wenn es nicht geladen werden konnte. */
    private static boolean blitIcon(GuiGraphicsExtractor context, Identifier icon, int x, int y, int size) {
        try {
            context.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0.0f, 0.0f, size, size, size, size);
            return true;
        } catch (Exception e) {
            // Fallback: nur Text zeichnen, wenn das Icon nicht geladen werden kann
            return false;
        }
    }

    private static boolean isRecyclerSlotKey(String key) {
        return "recyclerSlot1".equals(key) || "recyclerSlot2".equals(key) || "recyclerSlot3".equals(key);
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

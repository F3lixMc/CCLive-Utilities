package net.felix.utilities.Overall.npcalerts.ui;

import java.util.ArrayList;
import java.util.List;
import net.felix.CCLiveUtilities;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.OverlayEditorScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Screen für NPC Alerts-Einstellungen
 * Ähnlich wie das Overlay-Settings-Menü
 */
public class NpcAlertsSettingsScreen extends Screen {
    
    private static final int TOP_SETTINGS_CHECKBOX_ROWS = 2; // „Overlays Ein/Aus“ + „Gesamt Overlay Hintergrund“
    private static final String OVERLAYS_MASTER_TOGGLE_LABEL = "Overlays Ein/Aus";
    private static final String MAIN_BACKGROUND_LABEL = "Gesamt Overlay Hintergrund";

    private static final int BOX_WIDTH = 300;
    private static final int ROW_SPACING = 25;
    private static final int CHECKBOX_SIZE = 10;
    private static final int GEAR_SIZE = 12;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int GLOBAL_ROW = 0;
    private static final int MAIN_BACKGROUND_ROW = 1;
    private static final int FIRST_ENTRY_ROW = 2;
    // Buttons des OverlayEditorScreen: 4 Buttons à 80 px mit 10 px Abstand
    private static final int PARENT_BUTTON_WIDTH = 80;
    private static final int PARENT_BUTTON_HEIGHT = 20;
    private static final int PARENT_BUTTON_SPACING = 10;
    private static final int PARENT_BUTTONS_TOTAL_WIDTH = 4 * PARENT_BUTTON_WIDTH + 3 * PARENT_BUTTON_SPACING;
    
    // Settings Icon Identifier
    private static final Identifier SETTINGS_ICON = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_settings.png");
    
    private final Screen parent;
    private StringWidget titleWidget;
    private List<NpcAlertsEntry> entries;
    
    public NpcAlertsSettingsScreen(Screen parent) {
        super(Component.literal("NPC Alerts Settings"));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Title
        // Erstelle Einträge
        entries = new ArrayList<>();
        entries.add(new NpcAlertsEntry("Forschung", "forschung", () -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung = val));
        entries.add(new NpcAlertsEntry("Amboss Kapazität", "amboss", () -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss = val));
        entries.add(new NpcAlertsEntry("Schmelzofen Kapazität", "schmelzofen", () -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofen, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofen = val));
        entries.add(new NpcAlertsEntry("Jäger Kapazität", "jaeger", () -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaeger, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaeger = val));
        entries.add(new NpcAlertsEntry("Seelen Kapazität", "seelen", () -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelen, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelen = val));
        entries.add(new NpcAlertsEntry("Essenzen Kapazität", "essenzen", () -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzen, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzen = val));
        entries.add(new NpcAlertsEntry("Kombo Kiste", "komboKiste", () -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsKomboKiste, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsKomboKiste = val));
        entries.add(new NpcAlertsEntry("Machtkristalle", "machtkristalle", () -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle = val));
        entries.add(new NpcAlertsEntry("Recycler", "recycler", 
            () -> CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 || 
                  CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 || 
                  CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3, 
            val -> {
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 = val;
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 = val;
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 = val;
            }));

        titleWidget = new StringWidget(
            Component.literal("NPC Alerts Settings"),
            font
        );
        int boxHeight = computeSettingsBoxHeight();
        int boxY = height / 2 - boxHeight / 2;
        titleWidget.setPosition(width / 2 - titleWidget.getWidth() / 2, boxY - 24);
        addRenderableWidget(titleWidget);
    }

    private int computeSettingsBoxHeight() {
        return Math.min(500, (entries.size() + TOP_SETTINGS_CHECKBOX_ROWS) * 25 + 40);
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Render the previous screen in the background if it exists
        if (parent != null) {
            parent.extractRenderState(context, mouseX, mouseY, delta);
        }

        // Render very transparent background overlay
        context.fill(0, 0, width, height, 0x20000000);

        // Render title
        titleWidget.extractRenderState(context, mouseX, mouseY, delta);

        renderSettingsBox(context, mouseX, mouseY);

        // Render buttons
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    // ------------------------------------------------------------------ Layout

    private int boxX() {
        return width / 2 - BOX_WIDTH / 2;
    }

    private int boxY() {
        return height / 2 - computeSettingsBoxHeight() / 2;
    }

    private int checkboxX() {
        return boxX() + 10;
    }

    private static int labelX(int checkboxX) {
        return checkboxX + CHECKBOX_SIZE + 5;
    }

    /** Y-Position der Zeile (0 = Overlays Ein/Aus, 1 = Gesamt Overlay Hintergrund, ab 2 die Einträge). */
    private int rowY(int rowIndex) {
        return boxY() + 35 + rowIndex * ROW_SPACING;
    }

    private int closeButtonX() {
        return boxX() + BOX_WIDTH - CLOSE_BUTTON_SIZE - 5;
    }

    private int closeButtonY() {
        return boxY() + 5;
    }

    private int gearX() {
        return boxX() + BOX_WIDTH - GEAR_SIZE - 10;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int rectWidth, int rectHeight) {
        return mouseX >= x && mouseX <= x + rectWidth && mouseY >= y && mouseY <= y + rectHeight;
    }

    private boolean isOverCloseButton(double mouseX, double mouseY) {
        return isInside(mouseX, mouseY, closeButtonX(), closeButtonY(), CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
    }

    /** Maus über Checkbox oder Beschriftung der Zeile. */
    private boolean isOverRow(double mouseX, double mouseY, int rowY, String label) {
        int checkboxX = checkboxX();
        return isInside(mouseX, mouseY, checkboxX, rowY, CHECKBOX_SIZE, CHECKBOX_SIZE)
            || isInside(mouseX, mouseY, labelX(checkboxX), rowY, font.width(label), font.lineHeight);
    }

    private boolean isOverGear(double mouseX, double mouseY, int rowY) {
        return isInside(mouseX, mouseY, gearX(), rowY - 1, GEAR_SIZE, GEAR_SIZE);
    }

    // ------------------------------------------------------------------ Rendering

    private void renderSettingsBox(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int boxX = boxX();
        int boxY = boxY();
        int boxHeight = computeSettingsBoxHeight();

        context.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + boxHeight, 0xFF000000);
        context.outline(boxX, boxY, BOX_WIDTH, boxHeight, 0xFFFFFFFF);
        context.text(font, "NPC Alerts Einstellungen", boxX + 10, boxY + 10, 0xFFFFFF00, false);
        drawCloseButton(context, mouseX, mouseY);

        var config = CCLiveUtilitiesConfig.HANDLER.instance();
        // Zeile 1: Alle NPC Alerts-Overlays sichtbar (nur Sichtbarkeit)
        boolean overlaysVisible = config.npcAlertsUtilityEnabled && config.npcAlertsOverlaysVisible;
        drawCheckboxRow(context, rowY(GLOBAL_ROW), OVERLAYS_MASTER_TOGGLE_LABEL, overlaysVisible);
        drawRowHighlightIfHovered(context, mouseX, mouseY, rowY(GLOBAL_ROW), OVERLAYS_MASTER_TOGGLE_LABEL);

        // Zeile 2: Gesamt Overlay Hintergrund
        drawCheckboxRow(context, rowY(MAIN_BACKGROUND_ROW), MAIN_BACKGROUND_LABEL, config.npcAlertsMainOverlayShowBackground);
        drawRowHighlightIfHovered(context, mouseX, mouseY, rowY(MAIN_BACKGROUND_ROW), MAIN_BACKGROUND_LABEL);

        for (int i = 0; i < entries.size(); i++) {
            drawEntryRow(context, mouseX, mouseY, entries.get(i), rowY(FIRST_ENTRY_ROW + i));
        }
    }

    /** Rotes Kreuz oben rechts zum Schließen. */
    private void drawCloseButton(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int closeButtonX = closeButtonX();
        int closeButtonY = closeButtonY();
        int crossColor = isOverCloseButton(mouseX, mouseY) ? 0xFFFF6666 : 0xFFFF0000; // Helleres Rot beim Hover

        // Das Kreuz (X) besteht aus zwei diagonalen Linien
        drawDiagonalLine(context, closeButtonX + 2, closeButtonY + 2,
                        closeButtonX + CLOSE_BUTTON_SIZE - 2, closeButtonY + CLOSE_BUTTON_SIZE - 2, crossColor, 2);
        drawDiagonalLine(context, closeButtonX + CLOSE_BUTTON_SIZE - 2, closeButtonY + 2,
                        closeButtonX + 2, closeButtonY + CLOSE_BUTTON_SIZE - 2, crossColor, 2);
    }

    /** Checkbox (mit Häkchen, wenn aktiv) und Beschriftung; ausgegraut, wenn nicht aktiv. */
    private void drawCheckboxRow(GuiGraphicsExtractor context, int rowY, String label, boolean enabled) {
        int checkboxX = checkboxX();
        context.fill(checkboxX, rowY, checkboxX + CHECKBOX_SIZE, rowY + CHECKBOX_SIZE, 0xFF808080);
        context.outline(checkboxX, rowY, CHECKBOX_SIZE, CHECKBOX_SIZE, 0xFFFFFFFF);
        if (enabled) {
            drawCheckboxCheckmark(context, checkboxX, rowY, CHECKBOX_SIZE);
        }
        context.text(font, label, labelX(checkboxX), rowY + 1, enabled ? 0xFFFFFFFF : 0xFF808080, false);
    }

    /** Hover-Hintergrund für Checkbox und Beschriftung; wird nach allen Elementen gezeichnet, damit er darüber liegt. */
    private void drawRowHighlightIfHovered(GuiGraphicsExtractor context, int mouseX, int mouseY, int rowY, String label) {
        if (isOverRow(mouseX, mouseY, rowY, label)) {
            int checkboxX = checkboxX();
            int hoverEndX = labelX(checkboxX) + font.width(label) + 2;
            context.fill(checkboxX - 2, rowY - 1, hoverEndX, rowY + CHECKBOX_SIZE + 1, 0x40FFFFFF);
        }
    }

    /** Eintrag mit Checkbox, Name und Zahnrad-Icon am Zeilenende. */
    private void drawEntryRow(GuiGraphicsExtractor context, int mouseX, int mouseY, NpcAlertsEntry entry, int rowY) {
        drawCheckboxRow(context, rowY, entry.displayName(), entry.isEnabled().get());

        int gearX = gearX();
        int gearY = rowY - 1;
        // Weißer Rahmen um das Zahnrad-Icon
        context.outline(gearX - 1, gearY - 1, GEAR_SIZE + 2, GEAR_SIZE + 2, 0xFFFFFFFF);
        drawGearIcon(context, gearX, gearY, GEAR_SIZE);

        drawRowHighlightIfHovered(context, mouseX, mouseY, rowY, entry.displayName());
        if (isOverGear(mouseX, mouseY, rowY)) {
            context.fill(gearX - 1, gearY - 1, gearX + GEAR_SIZE + 1, gearY + GEAR_SIZE + 1, 0x40FFFFFF);
        }
    }

    // ------------------------------------------------------------------ Klicks

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = net.felix.utilities.Other.keybinding.KeyBindingUtility.legacyMouseButton(event.button());
        if (button == 0) {
            // Zuerst das Schließen-Kreuz prüfen
            if (isOverCloseButton(mouseX, mouseY)) {
                onClose();
                return true;
            }

            // Klick auf einen Button im Parent-Screen (OverlayEditorScreen)?
            if (isOverParentButtons(mouseX, mouseY)) {
                // Wenn es nicht der "NPC Alerts" Button ist, diesen Screen schließen, bevor der Klick weitergeleitet wird
                if (!isOverParentNpcAlertsButton(mouseX)) {
                    onClose();
                }
                if (parent instanceof OverlayEditorScreen) {
                    return parent.mouseClicked(event, doubled);
                }
            }

            if (handleSettingsClick(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    private int parentButtonsStartX() {
        return width / 2 - PARENT_BUTTONS_TOTAL_WIDTH / 2;
    }

    /** Alle 4 Buttons des OverlayEditorScreen (bei height - 30, Höhe 20, Breite 80, Abstand 10). */
    private boolean isOverParentButtons(double mouseX, double mouseY) {
        return isInside(mouseX, mouseY, parentButtonsStartX(), height - 30, PARENT_BUTTONS_TOTAL_WIDTH, PARENT_BUTTON_HEIGHT);
    }

    /** Der zweite Button des Parents ist "NPC Alerts". */
    private boolean isOverParentNpcAlertsButton(double mouseX) {
        int npcAlertsButtonX = parentButtonsStartX() + PARENT_BUTTON_WIDTH + PARENT_BUTTON_SPACING;
        return mouseX >= npcAlertsButtonX && mouseX <= npcAlertsButtonX + PARENT_BUTTON_WIDTH;
    }

    private boolean handleSettingsClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        // Klick außerhalb der Box wird nicht behandelt
        if (!isInside(mouseX, mouseY, boxX(), boxY(), BOX_WIDTH, computeSettingsBoxHeight())) {
            return false;
        }

        if (isOverRow(mouseX, mouseY, rowY(GLOBAL_ROW), OVERLAYS_MASTER_TOGGLE_LABEL)) {
            var config = CCLiveUtilitiesConfig.HANDLER.instance();
            boolean enable = !config.npcAlertsUtilityEnabled || !config.npcAlertsOverlaysVisible;
            if (enable) config.npcAlertsUtilityEnabled = true;
            config.npcAlertsOverlaysVisible = enable;
            saveAndRefreshParent();
            return true;
        }

        if (isOverRow(mouseX, mouseY, rowY(MAIN_BACKGROUND_ROW), MAIN_BACKGROUND_LABEL)) {
            var config = CCLiveUtilitiesConfig.HANDLER.instance();
            config.npcAlertsMainOverlayShowBackground = !config.npcAlertsMainOverlayShowBackground;
            saveAndRefreshParent();
            return true;
        }

        for (int i = 0; i < entries.size(); i++) {
            NpcAlertsEntry entry = entries.get(i);
            int rowY = rowY(FIRST_ENTRY_ROW + i);
            if (isOverRow(mouseX, mouseY, rowY, entry.displayName())) {
                entry.setEnabled().accept(!entry.isEnabled().get());
                saveAndRefreshParent();
                return true;
            }
            if (isOverGear(mouseX, mouseY, rowY)) {
                // Detail-Screen für diese Information öffnen
                if (minecraft != null) {
                    minecraft.gui.setScreen(new NpcAlertsDetailScreen(this, entry.displayName(), entry.configKey()));
                }
                return true;
            }
        }
        return false;
    }

    private void saveAndRefreshParent() {
        CCLiveUtilitiesConfig.HANDLER.save();
        refreshParentOverlays();
    }

    /** Aktualisiert den Overlay-Editor-Screen, wenn er geöffnet ist. */
    private void refreshParentOverlays() {
        if (parent instanceof OverlayEditorScreen) {
            ((OverlayEditorScreen) parent).refreshOverlays();
        }
    }

    @Override
    public void onClose() {
        refreshParentOverlays();

        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    // ------------------------------------------------------------------ Zeichenhelfer

    private static void drawCheckboxCheckmark(GuiGraphicsExtractor context, int checkboxX, int topY, int checkboxSize) {
        int checkX = checkboxX + 2;
        int checkY = topY + 2;
        int checkSize = checkboxSize - 4;
        for (int i = 0; i < checkSize / 2; i++) {
            int px = checkX + i;
            int py = checkY + checkSize / 2 + i;
            if (px < checkboxX + checkboxSize - 2 && py < topY + checkboxSize - 2) {
                context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
            }
        }
        for (int i = 0; i < checkSize / 2; i++) {
            int px = checkX + checkSize / 2 + i;
            int py = checkY + checkSize - 2 - i;
            if (px < checkboxX + checkboxSize - 2 && py >= topY + 2) {
                context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
            }
        }
    }
    /**
     * Zeichnet eine diagonale Linie
     */
    private void drawDiagonalLine(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int color, int width) {
        // Einfache Implementierung: Zeichne mehrere Pixel entlang der Diagonale
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            // Zeichne einen kleinen Quadrat für die Linie
            for (int dx = -width/2; dx <= width/2; dx++) {
                for (int dy = -width/2; dy <= width/2; dy++) {
                    if (dx*dx + dy*dy <= (width/2)*(width/2)) {
                        context.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, color);
                    }
                }
            }
        }
    }
    
    /**
     * Zeichnet das Settings-Icon (Zahnräder)
     */
    private void drawGearIcon(GuiGraphicsExtractor context, int x, int y, int size) {
        try {
            // Zeichne das Settings-Icon aus der Textur
            context.blit(
                RenderPipelines.GUI_TEXTURED,
                SETTINGS_ICON,
                x, y,
                0.0f, 0.0f,
                size, size,
                size, size
            );
        } catch (Exception e) {
            // Fallback: Zeichne ein einfaches Zahnrad-Icon als gefüllte Formen
            // Äußerer Rahmen
            context.outline(x, y, size, size, 0xFFFFFFFF);
            
            // Diagonale Linien (als gefüllte Rechtecke)
            int lineWidth = 1;
            // Hauptdiagonale
            for (int i = 0; i < size; i++) {
                int px = x + i;
                int py = y + i;
                if (px < x + size && py < y + size) {
                    context.fill(px, py, px + lineWidth, py + lineWidth, 0xFFFFFFFF);
                }
            }
            // Nebendiagonale
            for (int i = 0; i < size; i++) {
                int px = x + size - 1 - i;
                int py = y + i;
                if (px >= x && py < y + size) {
                    context.fill(px, py, px + lineWidth, py + lineWidth, 0xFFFFFFFF);
                }
            }
            
            // Horizontal und vertikal
            int centerX = x + size / 2;
            int centerY = y + size / 2;
            context.fill(centerX - 1, y, centerX + 1, y + size, 0xFFFFFFFF);
            context.fill(x, centerY - 1, x + size, centerY + 1, 0xFFFFFFFF);
        }
    }
}

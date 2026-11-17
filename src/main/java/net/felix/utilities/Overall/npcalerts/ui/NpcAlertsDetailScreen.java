package net.felix.utilities.Overall.npcalerts.ui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Set;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.OverlayEditorScreen;
import net.felix.utilities.Other.keybinding.KeyBindingUtility;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.getMachtkristallSlotSeparate;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.getMachtkristalleSlotEnabled;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.getRecyclerSlotEnabled;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.getRecyclerSlotSeparate;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.getSeparateOverlay;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.getShowBackground;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.getShowIcon;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.getShowMachtkristalleLevel;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.getShowPercent;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.getShowScreenMessage;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.getShowScreenMessageWhenOverlayDisabled;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.getWarnPercent;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.setMachtkristallSlotSeparate;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.setMachtkristalleSlotEnabled;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.setRecyclerSlotEnabled;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.setRecyclerSlotSeparate;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.setSeparateOverlay;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.setShowBackground;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.setShowIcon;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.setShowMachtkristalleLevel;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.setShowScreenMessage;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.setShowScreenMessageWhenOverlayDisabled;
import static net.felix.utilities.Overall.npcalerts.config.AlertConfigStore.setWarnPercent;
import static net.felix.utilities.Overall.npcalerts.ui.AlertColorOptions.createColorConfigScreen;

/**
 * Detail-Screen für einzelne NPC Alerts-Einstellungen
 */
public class NpcAlertsDetailScreen extends Screen {

    private final Screen parent;

    private final String infoName;

    private final String configKey;

    private StringWidget titleWidget;

    private String warnPercentInput = ""; // Warn-Prozentwert oder Kombo-Zielwert

    private boolean isEditingWarnPercent = false;

    public NpcAlertsDetailScreen(Screen parent, String infoName, String configKey) {
        super(Component.literal("NPC Alerts Detail Settings"));
        this.parent = parent;
        this.infoName = infoName;
        this.configKey = configKey;
    }

    private boolean isMachtkristalleKey() {
        return "machtkristalle".equals(configKey);
    }

    private String getPercentCheckboxLabel() {
        return isMachtkristalleKey() ? "Prozentwert über 100%" : "Prozente anzeigen";
    }

    private static final String SCREEN_MESSAGE_WHEN_DISABLED_LABEL =
        "Bildschirm-Nachricht bei deaktiviertem Alert Overlay";

    private static final int SETTINGS_ROW_SCREEN_MESSAGE = 60;

    private static final int SETTINGS_ROW_SCREEN_MESSAGE_WHEN_DISABLED = 75;

    private static final int SETTINGS_ROW_PERCENT = 100;

    private static final int SETTINGS_ROW_LEVEL = 125;

    private static final int SETTINGS_EXTRA_HEIGHT = 15;

    private static final int SETTINGS_BOX_WIDTH = 300;

    private static final int CRYSTAL_EXTRA_HEIGHT = 85;

    private static final int CRYSTAL_SEPARATE_EXTRA_HEIGHT = 60;

    private static final int CRYSTAL_BOTTOM_TRIM = 30;

    private static final int PANEL_TOP_MARGIN = 36;

    private static final int PANEL_EDGE_MARGIN = 12;

    private float settingsScale() {
        if (!isMachtkristalleKey() || !getSeparateOverlay(configKey)) return 1.0f;
        int panelHeight = 275 + SETTINGS_EXTRA_HEIGHT + CRYSTAL_EXTRA_HEIGHT
            + CRYSTAL_SEPARATE_EXTRA_HEIGHT - CRYSTAL_BOTTOM_TRIM;
        float fitWidth = Math.max(1, width - 2 * PANEL_EDGE_MARGIN) / (float) SETTINGS_BOX_WIDTH;
        // An odd panel height can shift the integer box origin by half a pixel.
        float fitHeight = Math.max(1, height - PANEL_TOP_MARGIN - PANEL_EDGE_MARGIN) / (panelHeight + 1.0f);
        return Math.min(0.85f, Math.min(fitWidth, fitHeight));
    }

    private float settingsCenterY(float scale) {
        return scale == 1.0f ? height / 2.0f : (height + PANEL_TOP_MARGIN - PANEL_EDGE_MARGIN) / 2.0f;
    }

    private int getScreenMessageCheckboxY(int boxY) {
        return boxY + SETTINGS_ROW_SCREEN_MESSAGE;
    }

    private int getScreenMessageWhenDisabledCheckboxY(int boxY) {
        return boxY + SETTINGS_ROW_SCREEN_MESSAGE_WHEN_DISABLED;
    }

    private int getPercentCheckboxY(int boxY) {
        return boxY + SETTINGS_ROW_PERCENT;
    }

    private int getMachtkristalleLevelCheckboxY(int boxY) {
        return boxY + SETTINGS_ROW_LEVEL;
    }

    private int getWarnInputY(int boxY) {
        if ("forschung".equals(configKey)) return getMachtkristalleLevelCheckboxY(boxY);
        return isMachtkristalleKey() ? boxY + 150 : boxY + 125;
    }

    private int getSettingsBlockEndY(int boxY, boolean supportsPercent) {
        if (isMachtkristalleKey()) {
            return boxY + 200;
        }
        if (supportsPercent) {
            return boxY + 175;
        }
        if ("komboKiste".equals(configKey)) {
            return boxY + 155;
        }
        return boxY + 125;
    }

    private void clampMachtkristallWarnPercentIfNeeded() {
        if (!isMachtkristalleKey()
            || CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristallePercentOver100) {
            return;
        }
        double warn = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleWarnPercent;
        if (warn > 100) {
            CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleWarnPercent = 100;
            warnPercentInput = "100";
        }
    }

    private boolean isMachtkristallWarnCappedAt100() {
        return isMachtkristalleKey()
            && !CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristallePercentOver100;
    }

    @Override
    protected void init() {
        super.init();
        
        // Title
        titleWidget = new StringWidget(
            Component.literal(infoName + " - Einstellungen"),
            font
        );
        titleWidget.setPosition(width / 2 - titleWidget.getWidth() / 2, 20);
        addRenderableWidget(titleWidget);
        
        if ("komboKiste".equals(configKey)) {
            int z = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteZielwert;
            if (z < 1) {
                z = 1;
            }
            warnPercentInput = String.valueOf(z);
        } else if ("forschung".equals(configKey)) {
            int warnValue = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungWarnValue;
            warnPercentInput = warnValue >= 0 ? String.valueOf(warnValue) : "";
        } else {
            clampMachtkristallWarnPercentIfNeeded();
            double currentWarnPercent = getWarnPercent(configKey);
            if (currentWarnPercent >= 0) {
                warnPercentInput = String.valueOf((int) Math.round(currentWarnPercent));
            } else {
                warnPercentInput = "";
            }
        }
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
        
        // Render settings box
        float scale = settingsScale();
        float offsetX = width / 2.0f * (1.0f - scale);
        float offsetY = settingsCenterY(scale) - height / 2.0f * scale;
        context.pose().pushMatrix();
        try {
            context.pose().translate(offsetX, offsetY);
            context.pose().scale(scale, scale);
            renderSettingsBox(context, (int) Math.floor((mouseX - offsetX) / scale),
                (int) Math.floor((mouseY - offsetY) / scale));
        } finally {
            context.pose().popMatrix();
        }
        
        // Render buttons
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private static final Set<String> ICON_OPTION_KEYS = Set.of(
        "forschung", "amboss", "schmelzofen", "seelen", "essenzen", "jaeger", "komboKiste", "machtkristalle",
        "recycler", "recyclerSlot1", "recyclerSlot2", "recyclerSlot3");

    private static final Set<String> PERCENT_KEYS = Set.of(
        "forschung", "amboss", "schmelzofen", "jaeger", "seelen", "essenzen",
        "recycler", "recyclerSlot1", "recyclerSlot2", "recyclerSlot3", "machtkristalle");

    private static final Set<String> RECYCLER_KEYS = Set.of("recycler", "recyclerSlot1", "recyclerSlot2", "recyclerSlot3");

    private static final String BACKGROUND_LABEL = "Hintergrund anzeigen";

    private static final String SCREEN_MESSAGE_LABEL = "Bildschirm-Nachricht";

    private static final String LEVEL_LABEL = "Level anzeigen";

    private static final int SETTINGS_ROW_BACKGROUND = 35;

    private static final int CONTENT_PADDING = 10;

    private static final int CHECKBOX_SIZE = 10;

    private static final int CHECKBOX_TEXT_GAP = 5;

    private static final int SLOT_ROW_SPACING = 20;

    private static final int SLOT_COUNT = 3;

    private static final int BUTTON_WIDTH = 280;

    private static final int BUTTON_HEIGHT = 20;

    private static final int BUTTON_GAP = 10;

    private static final int INPUT_WIDTH = 100;

    private static final int INPUT_HEIGHT = 16;

    private static final int CLOSE_BUTTON_SIZE = 12;

    private static final int COLOR_ENABLED_TEXT = 0xFFFFFFFF;

    private static final int COLOR_DISABLED_TEXT = 0xFF808080;

    private static final int COLOR_HOVER_HIGHLIGHT = 0x40FFFFFF;

    /** Position und Größe der Einstellungs-Box samt den davon abhängigen Zeilen (gemeinsam für Zeichnen und Klicks). */
    private record SettingsLayout(int boxX, int boxY, int boxHeight, boolean hasIconOption, boolean supportsPercent,
            int iconButtonY, int colorButtonY, int separateButtonY) {

        int contentX() {
            return boxX + CONTENT_PADDING;
        }

        /** Y der ersten Slot-Checkbox (Machtkristalle/Recycler), direkt unter dem "Separates Overlay"-Button. */
        int slotStartY() {
            return separateButtonY + BUTTON_HEIGHT + BUTTON_GAP;
        }

        int closeButtonX() {
            return boxX + SETTINGS_BOX_WIDTH - CLOSE_BUTTON_SIZE - 5;
        }

        int closeButtonY() {
            return boxY + 5;
        }
    }

    private boolean isRecyclerFamilyKey() {
        return RECYCLER_KEYS.contains(configKey);
    }

    private boolean hasSlotCheckboxes() {
        return isMachtkristalleKey() || isRecyclerFamilyKey();
    }

    private SettingsLayout computeLayout() {
        boolean hasIconOption = ICON_OPTION_KEYS.contains(configKey);
        boolean supportsPercent = PERCENT_KEYS.contains(configKey);

        // Basis-Höhe; mit Icon-Option (und Hintergrund-Checkbox) höher
        int boxHeight = (hasIconOption ? 275 : 235) + SETTINGS_EXTRA_HEIGHT;
        // Für Machtkristalle und Recycler: 3 Slot-Checkboxen (immer), 3 "Einzeln"-Checkboxen bei Separates Overlay;
        // unten bleibt leerer Platz, der abgezogen wird
        boolean hasSeparateOverlay = hasSlotCheckboxes() && getSeparateOverlay(configKey);
        if (isMachtkristalleKey()) {
            boxHeight += CRYSTAL_EXTRA_HEIGHT - CRYSTAL_BOTTOM_TRIM;
            if (hasSeparateOverlay) {
                boxHeight += CRYSTAL_SEPARATE_EXTRA_HEIGHT;
            }
        } else if (isRecyclerFamilyKey()) {
            boxHeight += 60 - 30;
            if (hasSeparateOverlay) {
                boxHeight += 60;
            }
        }

        int boxX = width / 2 - SETTINGS_BOX_WIDTH / 2;
        // Vertikal zentriert (gleicher Abstand oben und unten)
        int boxY = (height - boxHeight) / 2;

        int blockEndY = getSettingsBlockEndY(boxY, supportsPercent);
        int colorButtonY = hasIconOption ? (blockEndY + BUTTON_HEIGHT + BUTTON_GAP) : blockEndY;
        int separateButtonY = colorButtonY + BUTTON_HEIGHT + BUTTON_GAP;
        return new SettingsLayout(boxX, boxY, boxHeight, hasIconOption, supportsPercent, blockEndY, colorButtonY, separateButtonY);
    }

    private static boolean isInsideInclusive(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    /** Maus liegt auf der Checkbox oder auf ihrem Label. */
    private boolean isOverCheckbox(double mouseX, double mouseY, int x, int y, String label) {
        int textX = x + CHECKBOX_SIZE + CHECKBOX_TEXT_GAP;
        return isInsideInclusive(mouseX, mouseY, x, y, CHECKBOX_SIZE, CHECKBOX_SIZE)
            || isInsideInclusive(mouseX, mouseY, textX, y, font.width(label), font.lineHeight);
    }

    private void renderSettingsBox(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        SettingsLayout layout = computeLayout();
        drawBoxFrame(context, layout, mouseX, mouseY);
        drawBaseCheckboxes(context, layout);
        drawPercentSection(context, layout);
        drawButtons(context, layout);
        drawSlotCheckboxes(context, layout);
        drawHoverHighlights(context, layout, mouseX, mouseY);
    }

    /** Hintergrund, Rahmen, Titel und rotes Schließen-Kreuz oben rechts. */
    private void drawBoxFrame(GuiGraphicsExtractor context, SettingsLayout layout, int mouseX, int mouseY) {
        int boxX = layout.boxX();
        int boxY = layout.boxY();
        context.fill(boxX, boxY, boxX + SETTINGS_BOX_WIDTH, boxY + layout.boxHeight(), 0xFF000000);
        context.outline(boxX, boxY, SETTINGS_BOX_WIDTH, layout.boxHeight(), 0xFFFFFFFF);
        context.text(font, infoName + " - Einstellungen", boxX + 10, boxY + 10, 0xFFFFFF00, false);

        int closeX = layout.closeButtonX();
        int closeY = layout.closeButtonY();
        boolean isHovering = isInsideInclusive(mouseX, mouseY, closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
        int crossColor = isHovering ? 0xFFFF6666 : 0xFFFF0000; // Helleres Rot beim Hover
        drawDiagonalLine(context, closeX + 2, closeY + 2,
            closeX + CLOSE_BUTTON_SIZE - 2, closeY + CLOSE_BUTTON_SIZE - 2, crossColor, 2);
        drawDiagonalLine(context, closeX + CLOSE_BUTTON_SIZE - 2, closeY + 2,
            closeX + 2, closeY + CLOSE_BUTTON_SIZE - 2, crossColor, 2);
    }

    /** Checkbox mit Label; aktivierte Checkboxen bekommen ein Häkchen und weißen Text. */
    private void drawCheckbox(GuiGraphicsExtractor context, int x, int y, boolean checked, String label) {
        context.fill(x, y, x + CHECKBOX_SIZE, y + CHECKBOX_SIZE, 0xFF808080);
        context.outline(x, y, CHECKBOX_SIZE, CHECKBOX_SIZE, 0xFFFFFFFF);
        if (checked) {
            drawCheckmark(context, x, y);
        }
        context.text(font, label, x + CHECKBOX_SIZE + CHECKBOX_TEXT_GAP, y + 1,
            checked ? COLOR_ENABLED_TEXT : COLOR_DISABLED_TEXT, false);
    }

    /** Häkchen (✓) als zwei Linien: von links oben zur Mitte und von der Mitte nach rechts unten. */
    private void drawCheckmark(GuiGraphicsExtractor context, int x, int y) {
        int checkX = x + 2;
        int checkY = y + 2;
        int checkSize = CHECKBOX_SIZE - 4;
        for (int i = 0; i < checkSize / 2; i++) {
            int px = checkX + i;
            int py = checkY + checkSize / 2 + i;
            if (px < x + CHECKBOX_SIZE - 2 && py < y + CHECKBOX_SIZE - 2) {
                context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
            }
        }
        for (int i = 0; i < checkSize / 2; i++) {
            int px = checkX + checkSize / 2 + i;
            int py = checkY + checkSize - 2 - i;
            if (px < x + CHECKBOX_SIZE - 2 && py >= y + 2) {
                context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
            }
        }
    }

    /** Hintergrund- und Bildschirm-Nachricht-Checkboxen (für alle Informationen). */
    private void drawBaseCheckboxes(GuiGraphicsExtractor context, SettingsLayout layout) {
        int x = layout.contentX();
        drawCheckbox(context, x, layout.boxY() + SETTINGS_ROW_BACKGROUND, getShowBackground(configKey), BACKGROUND_LABEL);
        drawCheckbox(context, x, getScreenMessageCheckboxY(layout.boxY()), getShowScreenMessage(configKey), SCREEN_MESSAGE_LABEL);
        drawCheckbox(context, x, getScreenMessageWhenDisabledCheckboxY(layout.boxY()),
            getShowScreenMessageWhenOverlayDisabled(configKey), SCREEN_MESSAGE_WHEN_DISABLED_LABEL);
    }

    /** Prozent-Checkbox samt Warn-Eingabe, Kombo-Kiste-Zielwert oder Hinweis, dass es keine Zusatzeinstellungen gibt. */
    private void drawPercentSection(GuiGraphicsExtractor context, SettingsLayout layout) {
        int boxY = layout.boxY();
        int x = layout.contentX();
        if (layout.supportsPercent()) {
            drawCheckbox(context, x, getPercentCheckboxY(boxY), getShowPercent(configKey), getPercentCheckboxLabel());
            if (isMachtkristalleKey()) {
                drawCheckbox(context, x, getMachtkristalleLevelCheckboxY(boxY),
                    getShowMachtkristalleLevel(configKey), LEVEL_LABEL);
            }
            int inputY = getWarnInputY(boxY);
            if ("forschung".equals(configKey)) {
                drawNumberInput(context, x, inputY, 80, "Warnen ab:", warnPercentInput, "",
                    "(Leer = deaktiviert, 0-23)");
            } else {
                drawNumberInput(context, x, inputY, 80, "Warnen ab %:", warnPercentInput, "%",
                    "(Leer lassen oder -1 = deaktiviert)");
            }
        } else if ("komboKiste".equals(configKey)) {
            String targetText = warnPercentInput.isEmpty() ? "1" : warnPercentInput;
            drawNumberInput(context, x, getPercentCheckboxY(boxY), 125, "Zielwert (rechts):", targetText, "",
                "Warnung bei Erreichen oder darüber (min. 1)");
        } else {
            context.text(font, "Keine zusätzlichen Einstellungen verfügbar",
                x, getPercentCheckboxY(boxY), COLOR_DISABLED_TEXT, false);
        }
    }

    /**
     * Label, Eingabefeld (mit Cursor-Blinken beim Bearbeiten), optionalem Suffix hinter dem Wert
     * (nur wenn ein Wert eingegeben ist) und grauem Hinweis darunter.
     */
    private void drawNumberInput(GuiGraphicsExtractor context, int x, int y, int fieldOffset, String label,
            String valueText, String suffix, String hint) {
        context.text(font, label, x, y + 3, 0xFFFFFFFF, false);
        int fieldX = x + fieldOffset;
        context.fill(fieldX, y, fieldX + INPUT_WIDTH, y + INPUT_HEIGHT, isEditingWarnPercent ? 0xFF404040 : 0xFF202020);
        context.outline(fieldX, y, INPUT_WIDTH, INPUT_HEIGHT, isEditingWarnPercent ? 0xFFFFFF00 : 0xFF808080);

        int textX = fieldX + 3;
        if (!valueText.isEmpty()) {
            context.text(font, valueText, textX, y + 4, 0xFFFFFFFF, false);
            textX += font.width(valueText);
            if (!suffix.isEmpty()) {
                context.text(font, suffix, textX, y + 4, 0xFFFFFFFF, false);
                textX += font.width(suffix);
            }
        }
        if (isEditingWarnPercent && System.currentTimeMillis() % 1000 < 500) {
            context.text(font, "_", textX, y + 4, 0xFFFFFFFF, false);
        }
        context.text(font, hint, x, y + INPUT_HEIGHT + 5, COLOR_DISABLED_TEXT, false);
    }

    /** Icon-Umschalter (falls unterstützt), Farben- und "Separates Overlay"-Button. */
    private void drawButtons(GuiGraphicsExtractor context, SettingsLayout layout) {
        int x = layout.contentX();
        if (layout.hasIconOption()) {
            boolean showIcon = getShowIcon(configKey);
            drawButton(context, x, layout.iconButtonY(), showIcon ? "Icon anzeigen: AN" : "Icon anzeigen: AUS",
                showIcon ? 0xFF00FF00 : 0xFFFF0000);
        }
        drawButton(context, x, layout.colorButtonY(), "Farben", 0xFFFFFFFF);
        boolean separate = getSeparateOverlay(configKey);
        drawButton(context, x, layout.separateButtonY(), separate ? "Separates Overlay: AN" : "Separates Overlay: AUS",
            separate ? 0xFF00FF00 : 0xFFFF0000);
    }

    private void drawButton(GuiGraphicsExtractor context, int x, int y, String text, int borderColor) {
        context.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, 0xFF404040);
        context.outline(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, borderColor);
        int textX = x + (BUTTON_WIDTH - font.width(text)) / 2;
        int textY = y + (BUTTON_HEIGHT - font.lineHeight) / 2 + 1;
        context.text(font, text, textX, textY, 0xFFFFFFFF, false);
    }

    private String slotEnabledLabel(int slot) {
        return (isMachtkristalleKey() ? "MK Slot " : "Recycler Slot ") + (slot + 1) + " ein/aus";
    }

    private String slotSeparateLabel(int slot) {
        return (isMachtkristalleKey() ? "MK " : "Recycler ") + (slot + 1) + " Einzeln";
    }

    private boolean isSlotEnabled(int slot) {
        return isMachtkristalleKey()
            ? getMachtkristalleSlotEnabled(configKey, slot)
            : getRecyclerSlotEnabled(configKey, slot);
    }

    private boolean isSlotSeparate(int slot) {
        return isMachtkristalleKey()
            ? getMachtkristallSlotSeparate(configKey, slot)
            : getRecyclerSlotSeparate(configKey, slot);
    }

    private void toggleSlotEnabled(int slot) {
        boolean newValue = !isSlotEnabled(slot);
        if (isMachtkristalleKey()) {
            setMachtkristalleSlotEnabled(configKey, parent, slot, newValue);
        } else {
            setRecyclerSlotEnabled(configKey, parent, slot, newValue);
        }
    }

    private void toggleSlotSeparate(int slot) {
        boolean newValue = !isSlotSeparate(slot);
        if (isMachtkristalleKey()) {
            setMachtkristallSlotSeparate(configKey, slot, newValue);
        } else {
            setRecyclerSlotSeparate(configKey, slot, newValue);
        }
    }

    /** Y-Position der Slot-Checkbox: erst die "ein/aus"-Zeilen, darunter die "Einzeln"-Zeilen. */
    private static int slotRowY(SettingsLayout layout, int slot, boolean separateRow) {
        int rowOffset = (separateRow ? SLOT_COUNT : 0) + slot;
        return layout.slotStartY() + rowOffset * SLOT_ROW_SPACING;
    }

    /** Slot-Checkboxen für Machtkristalle/Recycler; "Einzeln" nur bei aktivem "Separates Overlay". */
    private void drawSlotCheckboxes(GuiGraphicsExtractor context, SettingsLayout layout) {
        if (!hasSlotCheckboxes()) {
            return;
        }
        int x = layout.contentX();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            drawCheckbox(context, x, slotRowY(layout, slot, false), isSlotEnabled(slot), slotEnabledLabel(slot));
        }
        if (getSeparateOverlay(configKey)) {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                drawCheckbox(context, x, slotRowY(layout, slot, true), isSlotSeparate(slot), slotSeparateLabel(slot));
            }
        }
    }

    /** Hover-Feedback für alle interaktiven Elemente (nach allen Elementen, damit es darüber liegt). */
    private void drawHoverHighlights(GuiGraphicsExtractor context, SettingsLayout layout, int mouseX, int mouseY) {
        int boxY = layout.boxY();
        int x = layout.contentX();

        drawCheckboxHover(context, mouseX, mouseY, x, boxY + SETTINGS_ROW_BACKGROUND, BACKGROUND_LABEL, true);
        drawCheckboxHover(context, mouseX, mouseY, x, getScreenMessageCheckboxY(boxY), SCREEN_MESSAGE_LABEL, true);
        drawCheckboxHover(context, mouseX, mouseY, x, getScreenMessageWhenDisabledCheckboxY(boxY),
            SCREEN_MESSAGE_WHEN_DISABLED_LABEL, true);

        if (layout.supportsPercent()) {
            drawCheckboxHover(context, mouseX, mouseY, x, getPercentCheckboxY(boxY), getPercentCheckboxLabel(), true);
            if (isMachtkristalleKey()) {
                drawCheckboxHover(context, mouseX, mouseY, x, getMachtkristalleLevelCheckboxY(boxY), LEVEL_LABEL, true);
            }
        }

        if (layout.hasIconOption()) {
            drawButtonHover(context, mouseX, mouseY, x, layout.iconButtonY());
        }
        drawButtonHover(context, mouseX, mouseY, x, layout.colorButtonY());
        drawButtonHover(context, mouseX, mouseY, x, layout.separateButtonY());

        if (hasSlotCheckboxes()) {
            // Beim Recycler ist bei den "ein/aus"-Zeilen nur die Checkbox selbst hervorgehoben, nicht das Label
            boolean highlightLabelOfEnabledRows = isMachtkristalleKey();
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                drawCheckboxHover(context, mouseX, mouseY, x, slotRowY(layout, slot, false),
                    slotEnabledLabel(slot), highlightLabelOfEnabledRows);
            }
            if (getSeparateOverlay(configKey)) {
                for (int slot = 0; slot < SLOT_COUNT; slot++) {
                    drawCheckboxHover(context, mouseX, mouseY, x, slotRowY(layout, slot, true),
                        slotSeparateLabel(slot), true);
                }
            }
        }
    }

    private void drawCheckboxHover(GuiGraphicsExtractor context, int mouseX, int mouseY, int x, int y, String label,
            boolean highlightLabel) {
        if (!isOverCheckbox(mouseX, mouseY, x, y, label)) {
            return;
        }
        if (highlightLabel) {
            int textEndX = x + CHECKBOX_SIZE + CHECKBOX_TEXT_GAP + font.width(label);
            context.fill(x - 2, y - 1, textEndX + 2, y + CHECKBOX_SIZE + 1, COLOR_HOVER_HIGHLIGHT);
        } else {
            context.fill(x - 1, y - 1, x + CHECKBOX_SIZE + 1, y + CHECKBOX_SIZE + 1, COLOR_HOVER_HIGHLIGHT);
        }
    }

    private void drawButtonHover(GuiGraphicsExtractor context, int mouseX, int mouseY, int x, int y) {
        if (isInsideInclusive(mouseX, mouseY, x, y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            context.fill(x - 1, y - 1, x + BUTTON_WIDTH + 1, y + BUTTON_HEIGHT + 1, COLOR_HOVER_HIGHLIGHT);
        }
    }
    private void setShowPercent(boolean value) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschungPercent = value;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent = value;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofenPercent = value;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaegerPercent = value;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelenPercent = value;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzenPercent = value;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristallePercentOver100 = value;
                if (!value) {
                    clampMachtkristallWarnPercentIfNeeded();
                }
                break;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent = value;
                break;
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        float scale = settingsScale();
        double mouseX = (event.x() - width / 2.0f) / scale + width / 2.0f;
        double mouseY = (event.y() - settingsCenterY(scale)) / scale + height / 2.0f;
        int button = net.felix.utilities.Other.keybinding.KeyBindingUtility.legacyMouseButton(event.button());
        if (button == 0) {
            // Schließen-Kreuz (gleiche Position wie beim Rendering)
            SettingsLayout layout = computeLayout();
            if (isInsideInclusive(mouseX, mouseY, layout.closeButtonX(), layout.closeButtonY(),
                    CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)) {
                // Speichere Eingaben beim Schließen
                saveWarnPercent();
                onClose();
                return true;
            }
            if (handleSettingsClick(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    private void saveConfig() {
        CCLiveUtilitiesConfig.HANDLER.save();
    }

    /**
     * Linksklick in der Einstellungs-Box. Die Reihenfolge entspricht der Darstellung von oben nach unten;
     * Klicks auf Checkboxen vor dem Eingabefeld lassen den Bearbeiten-Zustand unverändert.
     */
    private boolean handleSettingsClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        SettingsLayout layout = computeLayout();
        int boxX = layout.boxX();
        int boxY = layout.boxY();
        if (mouseX < boxX || mouseX > boxX + SETTINGS_BOX_WIDTH || mouseY < boxY || mouseY > boxY + layout.boxHeight()) {
            isEditingWarnPercent = false;
            return false;
        }

        if (handleBaseCheckboxClick(layout, mouseX, mouseY)) {
            return true;
        }
        if (layout.supportsPercent()) {
            if (handlePercentCheckboxClick(layout, mouseX, mouseY)) {
                return true;
            }
            // Warn-Eingabefeld
            int fieldX = boxX + 90;
            if (isInsideInclusive(mouseX, mouseY, fieldX, getWarnInputY(boxY), INPUT_WIDTH, INPUT_HEIGHT)) {
                isEditingWarnPercent = true;
                return true;
            }
            isEditingWarnPercent = false;
        } else if ("komboKiste".equals(configKey)) {
            int fieldX = boxX + 135;
            if (isInsideInclusive(mouseX, mouseY, fieldX, getPercentCheckboxY(boxY), INPUT_WIDTH, INPUT_HEIGHT)) {
                isEditingWarnPercent = true;
                return true;
            }
            isEditingWarnPercent = false;
        }

        if (handleButtonClick(layout, mouseX, mouseY)) {
            return true;
        }
        return handleSlotCheckboxClick(layout, mouseX, mouseY);
    }

    /** Hintergrund-, Bildschirm-Nachricht- und "bei deaktiviertem Overlay"-Checkbox. */
    private boolean handleBaseCheckboxClick(SettingsLayout layout, double mouseX, double mouseY) {
        int x = layout.contentX();
        int boxY = layout.boxY();

        if (isOverCheckbox(mouseX, mouseY, x, boxY + SETTINGS_ROW_BACKGROUND, BACKGROUND_LABEL)) {
            setShowBackground(configKey, !getShowBackground(configKey));
            saveConfig();
            return true;
        }
        if (isOverCheckbox(mouseX, mouseY, x, getScreenMessageCheckboxY(boxY), SCREEN_MESSAGE_LABEL)) {
            setShowScreenMessage(configKey, !getShowScreenMessage(configKey));
            saveConfig();
            return true;
        }
        if (isOverCheckbox(mouseX, mouseY, x, getScreenMessageWhenDisabledCheckboxY(boxY), SCREEN_MESSAGE_WHEN_DISABLED_LABEL)) {
            setShowScreenMessageWhenOverlayDisabled(configKey, !getShowScreenMessageWhenOverlayDisabled(configKey));
            saveConfig();
            return true;
        }
        return false;
    }

    /** Prozent-Checkbox und (bei Machtkristallen) Level-Checkbox. */
    private boolean handlePercentCheckboxClick(SettingsLayout layout, double mouseX, double mouseY) {
        int x = layout.contentX();
        int boxY = layout.boxY();

        if (isOverCheckbox(mouseX, mouseY, x, getPercentCheckboxY(boxY), getPercentCheckboxLabel())) {
            setShowPercent(!getShowPercent(configKey));
            saveConfig();
            return true;
        }
        if (isMachtkristalleKey()
                && isOverCheckbox(mouseX, mouseY, x, getMachtkristalleLevelCheckboxY(boxY), LEVEL_LABEL)) {
            setShowMachtkristalleLevel(configKey, !getShowMachtkristalleLevel(configKey));
            saveConfig();
            return true;
        }
        return false;
    }

    /** Icon-Umschalter, Farben-Button (öffnet den Farben-Screen) und "Separates Overlay"-Button. */
    private boolean handleButtonClick(SettingsLayout layout, double mouseX, double mouseY) {
        int x = layout.contentX();

        if (layout.hasIconOption() && isInsideInclusive(mouseX, mouseY, x, layout.iconButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            setShowIcon(configKey, !getShowIcon(configKey));
            saveConfig();
            return true;
        }
        if (isInsideInclusive(mouseX, mouseY, x, layout.colorButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            // Öffne YACL-Screen für beide Farben
            Screen colorScreen = createColorConfigScreen(this, configKey);
            minecraft.gui.setScreen(colorScreen);
            return true;
        }
        if (isInsideInclusive(mouseX, mouseY, x, layout.separateButtonY(), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            setSeparateOverlay(configKey, parent, !getSeparateOverlay(configKey));
            saveConfig();
            return true;
        }
        return false;
    }

    /** Slot-Checkboxen für Machtkristalle/Recycler; "Einzeln" nur bei aktivem "Separates Overlay". */
    private boolean handleSlotCheckboxClick(SettingsLayout layout, double mouseX, double mouseY) {
        if (!hasSlotCheckboxes()) {
            return false;
        }
        int x = layout.contentX();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (isOverCheckbox(mouseX, mouseY, x, slotRowY(layout, slot, false), slotEnabledLabel(slot))) {
                toggleSlotEnabled(slot);
                saveConfig();
                return true;
            }
        }
        if (getSeparateOverlay(configKey)) {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                if (isOverCheckbox(mouseX, mouseY, x, slotRowY(layout, slot, true), slotSeparateLabel(slot))) {
                    toggleSlotSeparate(slot);
                    saveConfig();
                    return true;
                }
            }
        }
        return false;
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

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (isEditingWarnPercent) {
            int key = event.key();
            Character digit = KeyBindingUtility.digitFromScancode(key);
            if (digit != null) {
                if (warnPercentInput.length() < 12) {
                    warnPercentInput += digit;
                    saveWarnPercent();
                }
                return true;
            }
            if (key == InputConstants.KEY_BACKSPACE) {
                if (!warnPercentInput.isEmpty()) {
                    warnPercentInput = warnPercentInput.substring(0, warnPercentInput.length() - 1);
                    saveWarnPercent();
                }
                return true;
            }
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
                saveWarnPercent();
                isEditingWarnPercent = false;
                return true;
            }
            if (key == InputConstants.KEY_ESCAPE) {
                isEditingWarnPercent = false;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private void saveWarnPercent() {
        if ("komboKiste".equals(configKey)) {
            try {
                int z = 1000;
                if (!warnPercentInput.trim().isEmpty()) {
                    z = Integer.parseInt(warnPercentInput.trim());
                }
                if (z < 1) {
                    z = 1;
                }
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteZielwert = z;
                CCLiveUtilitiesConfig.HANDLER.save();
            } catch (NumberFormatException e) {
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteZielwert = 1000;
                CCLiveUtilitiesConfig.HANDLER.save();
            }
            return;
        }
        if ("forschung".equals(configKey)) {
            try {
                int value = -1;
                if (!warnPercentInput.trim().isEmpty()) {
                    value = Integer.parseInt(warnPercentInput.trim());
                }
                if (value < 0) {
                    value = -1;
                } else if (value > 23) {
                    value = 23;
                }
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungWarnValue = value;
                CCLiveUtilitiesConfig.HANDLER.save();
            } catch (NumberFormatException e) {
                CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungWarnValue = -1;
                CCLiveUtilitiesConfig.HANDLER.save();
            }
            return;
        }
        try {
            double value = -1.0;
            if (!warnPercentInput.trim().isEmpty()) {
                value = Integer.parseInt(warnPercentInput.trim());
                if (value < 0) {
                    value = -1.0;
                } else if (isMachtkristallWarnCappedAt100() || !isMachtkristalleKey()) {
                    if (value > 100) {
                        value = 100.0;
                    }
                }
            }
            setWarnPercent(configKey, value);
            if (isMachtkristallWarnCappedAt100() && value == 100.0) {
                warnPercentInput = "100";
            }
            CCLiveUtilitiesConfig.HANDLER.save();
        } catch (NumberFormatException e) {
            // Ungültige Eingabe - setze auf -1 (deaktiviert)
            setWarnPercent(configKey, -1.0);
            CCLiveUtilitiesConfig.HANDLER.save();
        }
    }

    @Override
    public void onClose() {
        saveWarnPercent();
        // Aktualisiere das Overlay-Editor-Screen, wenn es geöffnet ist
        // Gehe durch die Parent-Hierarchie, um das OverlayEditorScreen zu finden
        Screen currentParent = parent;
        while (currentParent != null) {
            if (currentParent instanceof OverlayEditorScreen) {
                ((OverlayEditorScreen) currentParent).refreshOverlays();
                break;
            } else if (currentParent instanceof net.felix.utilities.Overall.npcalerts.ui.NpcAlertsSettingsScreen) {
                // NpcAlertsSettingsScreen hat auch ein parent, das das OverlayEditorScreen sein könnte
                // Wir müssen das parent des NpcAlertsSettingsScreen prüfen
                try {
                    java.lang.reflect.Field parentField = net.felix.utilities.Overall.npcalerts.ui.NpcAlertsSettingsScreen.class.getDeclaredField("parent");
                    parentField.setAccessible(true);
                    Screen settingsParent = (Screen) parentField.get(currentParent);
                    if (settingsParent instanceof OverlayEditorScreen) {
                        ((OverlayEditorScreen) settingsParent).refreshOverlays();
                    }
                } catch (Exception e) {
                    // Fallback: Versuche einfach das parent zu verwenden
                }
                break;
            }
            break;
        }
        
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

}

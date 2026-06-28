package net.felix.utilities.Overall.NpcAlerts;

import net.felix.CCLiveUtilities;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.OverlayEditorScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen für NPC Alerts-Einstellungen
 * Ähnlich wie das Overlay-Settings-Menü
 */
public class NpcAlertsSettingsScreen extends Screen {
    
    private static final int TOP_SETTINGS_CHECKBOX_ROWS = 2; // „Overlays Ein/Aus“ + „Gesamt Overlay Hintergrund“
    private static final String OVERLAYS_MASTER_TOGGLE_LABEL = "Overlays Ein/Aus";
    
    // Settings Icon Identifier
    private static final Identifier SETTINGS_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_settings.png");
    
    private final Screen parent;
    private TextWidget titleWidget;
    private List<NpcAlertsEntry> entries;
    
    public NpcAlertsSettingsScreen(Screen parent) {
        super(Text.literal("NPC Alerts Settings"));
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

        titleWidget = new TextWidget(
            Text.literal("NPC Alerts Settings"),
            textRenderer
        );
        int boxHeight = computeSettingsBoxHeight();
        int boxY = height / 2 - boxHeight / 2;
        titleWidget.setPosition(width / 2 - titleWidget.getWidth() / 2, boxY - 24);
        addDrawableChild(titleWidget);
    }

    private int computeSettingsBoxHeight() {
        return Math.min(500, (entries.size() + TOP_SETTINGS_CHECKBOX_ROWS) * 25 + 40);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the previous screen in the background if it exists
        if (parent != null) {
            parent.render(context, mouseX, mouseY, delta);
        }
        
        // Render very transparent background overlay
        context.fill(0, 0, width, height, 0x20000000);
        
        // Render title
        titleWidget.render(context, mouseX, mouseY, delta);
        
        // Render settings box
        renderSettingsBox(context, mouseX, mouseY);
        
        // Render buttons
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderSettingsBox(DrawContext context, int mouseX, int mouseY) {
        int boxWidth = 300;
        int boxHeight = computeSettingsBoxHeight();
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        // Hintergrund
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        
        // Rahmen
        context.drawStrokedRectangle(boxX, boxY, boxWidth, boxHeight, 0xFFFFFFFF);
        
        // Titel
        context.drawText(textRenderer, "NPC Alerts Einstellungen", boxX + 10, boxY + 10, 0xFFFFFF00, false);
        
        // Rotes Kreuz oben rechts zum Schließen
        int closeButtonSize = 12;
        int closeButtonX = boxX + boxWidth - closeButtonSize - 5;
        int closeButtonY = boxY + 5;
        
        // Prüfe ob Maus über dem Kreuz ist
        boolean isHovering = mouseX >= closeButtonX && mouseX <= closeButtonX + closeButtonSize &&
                             mouseY >= closeButtonY && mouseY <= closeButtonY + closeButtonSize;
        int crossColor = isHovering ? 0xFFFF6666 : 0xFFFF0000; // Helleres Rot beim Hover
        
        // Zeichne das Kreuz (X) - zwei diagonale Linien
        drawDiagonalLine(context, closeButtonX + 2, closeButtonY + 2, 
                        closeButtonX + closeButtonSize - 2, closeButtonY + closeButtonSize - 2, crossColor, 2);
        drawDiagonalLine(context, closeButtonX + closeButtonSize - 2, closeButtonY + 2, 
                        closeButtonX + 2, closeButtonY + closeButtonSize - 2, crossColor, 2);
        
        int y = boxY + 35;
        int checkboxSize = 10;
        int checkboxSpacing = 25;
        int checkboxX = boxX + 10;
        
        // Zeile 1: Alle NPC Alerts-Overlays sichtbar (nur Sichtbarkeit)
        boolean overlaysVisible = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsOverlaysVisible;
        int globalCheckboxY = y;
        context.fill(checkboxX, globalCheckboxY, checkboxX + checkboxSize, globalCheckboxY + checkboxSize, 0xFF808080);
        context.drawStrokedRectangle(checkboxX, globalCheckboxY, checkboxSize, checkboxSize, 0xFFFFFFFF);
        if (overlaysVisible) {
            drawCheckboxCheckmark(context, checkboxX, globalCheckboxY, checkboxSize);
        }
        int globalTextX = checkboxX + checkboxSize + 5;
        int globalTextWidth = textRenderer.getWidth(OVERLAYS_MASTER_TOGGLE_LABEL);
        int globalTextHeight = textRenderer.fontHeight;
        context.drawText(textRenderer, OVERLAYS_MASTER_TOGGLE_LABEL, globalTextX, globalCheckboxY + 1, overlaysVisible ? 0xFFFFFFFF : 0xFF808080, false);
        boolean hoverGlobalBox = mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
            mouseY >= globalCheckboxY && mouseY <= globalCheckboxY + checkboxSize;
        boolean hoverGlobalText = mouseX >= globalTextX && mouseX <= globalTextX + globalTextWidth &&
            mouseY >= globalCheckboxY && mouseY <= globalCheckboxY + globalTextHeight;
        if (hoverGlobalBox || hoverGlobalText) {
            context.fill(checkboxX - 2, globalCheckboxY - 1, globalTextX + globalTextWidth + 2, globalCheckboxY + checkboxSize + 1, 0x40FFFFFF);
        }
        
        y += checkboxSpacing;
        
        // Zeile 2: Gesamt Overlay Hintergrund
        boolean mainOverlayBackground = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayShowBackground;
        int mainCheckboxY = y;
        
        // Checkbox-Hintergrund
        context.fill(checkboxX, mainCheckboxY, checkboxX + checkboxSize, mainCheckboxY + checkboxSize, 0xFF808080);
        context.drawStrokedRectangle(checkboxX, mainCheckboxY, checkboxSize, checkboxSize, 0xFFFFFFFF);
        
        if (mainOverlayBackground) {
            drawCheckboxCheckmark(context, checkboxX, mainCheckboxY, checkboxSize);
        }
        
        // Eintrags-Name
        String mainEntryName = "Gesamt Overlay Hintergrund";
        int mainTextX = checkboxX + checkboxSize + 5;
        int mainTextWidth = textRenderer.getWidth(mainEntryName);
        int mainTextHeight = textRenderer.fontHeight;
        context.drawText(textRenderer, mainEntryName, mainTextX, mainCheckboxY + 1, mainOverlayBackground ? 0xFFFFFFFF : 0xFF808080, false);
        
        // Prüfe ob Maus über Checkbox oder Text ist
        boolean isHoveringMainCheckbox = mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
                                        mouseY >= mainCheckboxY && mouseY <= mainCheckboxY + checkboxSize;
        boolean isHoveringMainText = mouseX >= mainTextX && mouseX <= mainTextX + mainTextWidth &&
                                   mouseY >= mainCheckboxY && mouseY <= mainCheckboxY + mainTextHeight;
        boolean isHoveringMainEntry = isHoveringMainCheckbox || isHoveringMainText;
        
        // Zeichne Hover-Hintergrund NACH allen Elementen, damit es darüber liegt
        if (isHoveringMainEntry) {
            // Hover-Hintergrund für Checkbox und Text
            int hoverStartX = checkboxX - 2;
            int hoverEndX = mainTextX + mainTextWidth + 2;
            context.fill(hoverStartX, mainCheckboxY - 1, hoverEndX, mainCheckboxY + checkboxSize + 1, 0x40FFFFFF);
        }
        
        y += checkboxSpacing;
        
        // Checkboxen für alle Einträge
        for (NpcAlertsEntry entry : entries) {
            boolean isEnabled = entry.isEnabled.get();
            
            // Checkbox-Hintergrund
            int checkboxY = y;
            context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFF808080);
            context.drawStrokedRectangle(checkboxX, checkboxY, checkboxSize, checkboxSize, 0xFFFFFFFF);
            
            // Checkmark wenn aktiviert
            if (isEnabled) {
                // Zeichne Häkchen (✓)
                int checkX = checkboxX + 2;
                int checkY = checkboxY + 2;
                int checkSize = checkboxSize - 4;
                // Zeichne Häkchen als zwei Linien
                // Linke Linie (von oben-links nach mitte)
                for (int i = 0; i < checkSize / 2; i++) {
                    int px = checkX + i;
                    int py = checkY + checkSize / 2 + i;
                    if (px < checkboxX + checkboxSize - 2 && py < checkboxY + checkboxSize - 2) {
                        context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                    }
                }
                // Rechte Linie (von mitte nach unten-rechts)
                for (int i = 0; i < checkSize / 2; i++) {
                    int px = checkX + checkSize / 2 + i;
                    int py = checkY + checkSize - 2 - i;
                    if (px < checkboxX + checkboxSize - 2 && py >= checkboxY + 2) {
                        context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                    }
                }
            }
            
            // Eintrags-Name
            String entryName = entry.displayName;
            int textX = checkboxX + checkboxSize + 5;
            int textWidth = textRenderer.getWidth(entryName);
            int textHeight = textRenderer.fontHeight;
            context.drawText(textRenderer, entryName, textX, checkboxY + 1, isEnabled ? 0xFFFFFFFF : 0xFF808080, false);
            
            // Zahnrad-Icon am Ende der Zeile
            int gearSize = 12;
            int gearX = boxX + boxWidth - gearSize - 10;
            int gearY = checkboxY - 1;
            
            // Prüfe ob Maus über Checkbox, Text oder Icon ist
            boolean isHoveringCheckbox = mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
                                        mouseY >= checkboxY && mouseY <= checkboxY + checkboxSize;
            boolean isHoveringText = mouseX >= textX && mouseX <= textX + textWidth &&
                                   mouseY >= checkboxY && mouseY <= checkboxY + textHeight;
            boolean isHoveringGear = mouseX >= gearX && mouseX <= gearX + gearSize &&
                                     mouseY >= gearY && mouseY <= gearY + gearSize;
            boolean isHoveringEntry = isHoveringCheckbox || isHoveringText;
            
            // Zeichne weißen Rahmen um das Zahnrad-Icon
            context.drawStrokedRectangle(gearX - 1, gearY - 1, gearSize + 2, gearSize + 2, 0xFFFFFFFF);
            
            drawGearIcon(context, gearX, gearY, gearSize);
            
            // Zeichne Hover-Hintergrund NACH allen Elementen, damit es darüber liegt
            if (isHoveringEntry) {
                // Hover-Hintergrund für Checkbox und Text
                int hoverStartX = checkboxX - 2;
                int hoverEndX = textX + textWidth + 2;
                context.fill(hoverStartX, checkboxY - 1, hoverEndX, checkboxY + checkboxSize + 1, 0x40FFFFFF);
            }
            
            // Zeichne Hover-Hintergrund für Zahnrad-Icon
            if (isHoveringGear) {
                context.fill(gearX - 1, gearY - 1, gearX + gearSize + 1, gearY + gearSize + 1, 0x40FFFFFF);
            }
            
            y += checkboxSpacing;
        }
    }
    
    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0) {
            // Prüfe zuerst ob auf das Schließen-Kreuz geklickt wurde
            int boxWidth = 300;
            int boxHeight = computeSettingsBoxHeight();
            int boxX = width / 2 - boxWidth / 2;
            int boxY = height / 2 - boxHeight / 2;
            
            int closeButtonSize = 12;
            int closeButtonX = boxX + boxWidth - closeButtonSize - 5;
            int closeButtonY = boxY + 5;
            
            if (mouseX >= closeButtonX && mouseX <= closeButtonX + closeButtonSize &&
                mouseY >= closeButtonY && mouseY <= closeButtonY + closeButtonSize) {
                close();
                return true;
            }
            
            // Prüfe ob Klick auf einen Button im Parent-Screen (OverlayEditorScreen) ist
            // Buttons sind bei: height - 30, Höhe 20, Breite 80, Abstand 10
            int buttonY = height - 30;
            int buttonHeight = 20;
            int buttonWidth = 80;
            int buttonSpacing = 10;
            int totalButtonsWidth = 4 * buttonWidth + 3 * buttonSpacing; // 350
            int startX = width / 2 - totalButtonsWidth / 2;
            
            // Prüfe ob Klick im Button-Bereich ist (alle 4 Buttons zusammen)
            if (mouseY >= buttonY && mouseY <= buttonY + buttonHeight &&
                mouseX >= startX && mouseX <= startX + totalButtonsWidth) {
                // Prüfe welcher Button geklickt wurde
                int button2X = startX + buttonWidth + buttonSpacing; // NPC Alerts
                
                // Wenn es nicht der "NPC Alerts" Button ist, schließe diesen Screen
                if (mouseX < button2X || mouseX > button2X + buttonWidth) {
                    // Schließe den NpcAlertsSettingsScreen, bevor der Klick weitergeleitet wird
                    close();
                }
                
                // Klick ist auf einen Button im Parent-Screen - leite an Parent weiter
                if (parent != null && parent instanceof OverlayEditorScreen) {
                    return parent.mouseClicked(new net.minecraft.client.gui.Click(mouseX, mouseY, new net.minecraft.client.input.MouseInput(button, 1)), false);
                }
            }
            
            if (handleSettingsClick(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(event, isDoubleClick);
    }
    
    private boolean handleSettingsClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        
        int boxWidth = 300;
        int boxHeight = computeSettingsBoxHeight();
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        // Prüfe ob Klick innerhalb des Overlays ist
        if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + boxHeight) {
            // Klick außerhalb - schließe Overlay
            return false;
        }
        
        // Prüfe ob Klick auf eine Checkbox, den Namen oder das Zahnrad-Icon ist
        int y = boxY + 35;
        int checkboxSize = 10;
        int checkboxSpacing = 25;
        int checkboxX = boxX + 10;
        int textX = checkboxX + checkboxSize + 5;
        int gearSize = 12;
        
        int globalCheckboxY = y;
        int globalTextX = checkboxX + checkboxSize + 5;
        int globalTextWidth = textRenderer.getWidth(OVERLAYS_MASTER_TOGGLE_LABEL);
        int globalTextHeight = textRenderer.fontHeight;
        boolean clickedGlobalBox = mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
            mouseY >= globalCheckboxY && mouseY <= globalCheckboxY + checkboxSize;
        boolean clickedGlobalText = mouseX >= globalTextX && mouseX <= globalTextX + globalTextWidth &&
            mouseY >= globalCheckboxY && mouseY <= globalCheckboxY + globalTextHeight;
        if (clickedGlobalBox || clickedGlobalText) {
            CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsOverlaysVisible =
                !CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsOverlaysVisible;
            CCLiveUtilitiesConfig.HANDLER.save();
            if (parent instanceof OverlayEditorScreen) {
                ((OverlayEditorScreen) parent).refreshOverlays();
            }
            return true;
        }
        
        y += checkboxSpacing;
        
        int mainCheckboxY = y;
        String mainEntryName = "Gesamt Overlay Hintergrund";
        int mainTextX = checkboxX + checkboxSize + 5;
        int mainTextWidth = textRenderer.getWidth(mainEntryName);
        int mainTextHeight = textRenderer.fontHeight;
        
        boolean clickedOnMainCheckbox = (mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
                                       mouseY >= mainCheckboxY && mouseY <= mainCheckboxY + checkboxSize);
        boolean clickedOnMainText = (mouseX >= mainTextX && mouseX <= mainTextX + mainTextWidth &&
                                   mouseY >= mainCheckboxY && mouseY <= mainCheckboxY + mainTextHeight);
        
        if (clickedOnMainCheckbox || clickedOnMainText) {
            // Toggle Gesamt Overlay Hintergrund
            boolean newValue = !CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayShowBackground;
            CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayShowBackground = newValue;
            CCLiveUtilitiesConfig.HANDLER.save();
            
            // Aktualisiere das Overlay-Editor-Screen, wenn es geöffnet ist
            if (parent instanceof OverlayEditorScreen) {
                ((OverlayEditorScreen) parent).refreshOverlays();
            }
            
            return true;
        }
        
        y += checkboxSpacing;
        
        for (NpcAlertsEntry entry : entries) {
            int checkboxY = y;
            int textY = checkboxY;
            // Verwende die tatsächliche Text-Breite für korrekte Click-Erkennung
            String entryName = entry.displayName;
            int textWidth = textRenderer.getWidth(entryName);
            int textHeight = textRenderer.fontHeight;
            int gearX = boxX + boxWidth - gearSize - 10;
            int gearY = checkboxY - 1;
            
            // Prüfe ob Klick auf Checkbox
            boolean clickedOnCheckbox = (mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
                                       mouseY >= checkboxY && mouseY <= checkboxY + checkboxSize);
            
            // Prüfe ob Klick auf Text
            boolean clickedOnText = (mouseX >= textX && mouseX <= textX + textWidth &&
                                   mouseY >= textY && mouseY <= textY + textHeight);
            
            // Prüfe ob Klick auf Zahnrad-Icon
            boolean clickedOnGear = (mouseX >= gearX && mouseX <= gearX + gearSize &&
                                    mouseY >= gearY && mouseY <= gearY + gearSize);
            
            if (clickedOnCheckbox || clickedOnText) {
                // Toggle Eintrag
                boolean newValue = !entry.isEnabled.get();
                entry.setEnabled.accept(newValue);
                CCLiveUtilitiesConfig.HANDLER.save();
                
                // Aktualisiere das Overlay-Editor-Screen, wenn es geöffnet ist
                if (parent instanceof OverlayEditorScreen) {
                    ((OverlayEditorScreen) parent).refreshOverlays();
                }
                
                return true;
            } else if (clickedOnGear) {
                // Öffne Detail-Screen für diese Information
                if (client != null) {
                    client.setScreen(new NpcAlertsDetailScreen(this, entry.displayName, entry.configKey));
                }
                return true;
            }
            
            y += checkboxSpacing;
        }
        
        return false;
    }
    
    @Override
    public void close() {
        // Aktualisiere das Overlay-Editor-Screen, wenn es geöffnet ist
        if (parent instanceof OverlayEditorScreen) {
            ((OverlayEditorScreen) parent).refreshOverlays();
        }
        
        if (client != null) {
            client.setScreen(parent);
        }
    }
    
    private static void drawCheckboxCheckmark(DrawContext context, int checkboxX, int topY, int checkboxSize) {
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
    private void drawDiagonalLine(DrawContext context, int x1, int y1, int x2, int y2, int color, int width) {
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
    private void drawGearIcon(DrawContext context, int x, int y, int size) {
        try {
            // Zeichne das Settings-Icon aus der Textur
            context.drawTexture(
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
            context.drawStrokedRectangle(x, y, size, size, 0xFFFFFFFF);
            
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
    
    /**
     * Hilfsklasse für NPC Alerts-Einträge
     */
    private static class NpcAlertsEntry {
        String displayName;
        String configKey; // Für Detail-Screen
        java.util.function.Supplier<Boolean> isEnabled;
        java.util.function.Consumer<Boolean> setEnabled;
        
        NpcAlertsEntry(String displayName, String configKey, java.util.function.Supplier<Boolean> isEnabled, java.util.function.Consumer<Boolean> setEnabled) {
            this.displayName = displayName;
            this.configKey = configKey;
            this.isEnabled = isEnabled;
            this.setEnabled = setEnabled;
        }
    }
}


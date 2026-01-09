package net.felix.utilities.Overall.TabInfo;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.DragOverlay.OverlayEditorScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import java.awt.Color;

/**
 * Detail-Screen für einzelne Tab-Info-Einstellungen
 */
public class TabInfoDetailScreen extends Screen {
    
    private final Screen parent;
    private final String infoName;
    private final String configKey;
    private TextWidget titleWidget;
    private String warnPercentInput = ""; // Eingabefeld für Warn-Prozentwert
    private boolean isEditingWarnPercent = false;
    
    public TabInfoDetailScreen(Screen parent, String infoName, String configKey) {
        super(Text.literal("Tab Info Detail Settings"));
        this.parent = parent;
        this.infoName = infoName;
        this.configKey = configKey;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Title
        titleWidget = new TextWidget(
            Text.literal(infoName + " - Einstellungen"),
            textRenderer
        );
        titleWidget.setPosition(width / 2 - titleWidget.getWidth() / 2, 20);
        addDrawableChild(titleWidget);
        
        // Initialisiere Warn-Prozentwert Eingabefeld
        double currentWarnPercent = getWarnPercent();
        if (currentWarnPercent >= 0) {
            warnPercentInput = String.format("%.1f", currentWarnPercent);
        } else {
            warnPercentInput = "";
        }
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
    
    /**
     * Berechnet die boxY Position des TabInfoSettingsScreen Overlays
     * (9 Einträge: 1 "Haupt Overlay Hintergrund" + 8 normale Einträge * 25 + 40 = 265, max 500)
     */
    private int getSettingsScreenBoxY() {
        int settingsBoxHeight = Math.min(500, 9 * 25 + 40); // 9 Einträge im Settings-Screen (1 "Haupt Overlay Hintergrund" + 8 normale)
        return height / 2 - settingsBoxHeight / 2;
    }
    
    private void renderSettingsBox(DrawContext context, int mouseX, int mouseY) {
        int boxWidth = 300;
        // Höher für Amboss, Schmelzofen und Recycler (Icon-Option) und Hintergrund-Checkbox
        boolean hasIconOption = configKey.equals("forschung") || configKey.equals("amboss") || 
                               configKey.equals("schmelzofen") || configKey.equals("seelen") || 
                               configKey.equals("essenzen") || configKey.equals("jaeger") || 
                               configKey.equals("machtkristalle") ||
                               configKey.equals("recycler") || configKey.equals("recyclerSlot1") || 
                               configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        // Für Machtkristalle und Recycler: zusätzliche Höhe wenn "Separates Overlay" aktiviert ist (für 3 Checkboxen)
        boolean isMachtkristalle = configKey.equals("machtkristalle");
        boolean isRecycler = configKey.equals("recycler");
        boolean isRecyclerSlot = configKey.equals("recyclerSlot1") || configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        boolean hasSeparateOverlay = (isMachtkristalle || isRecycler || isRecyclerSlot) && getSeparateOverlay();
        int boxHeight = hasIconOption ? 250 : 210;
        if (hasSeparateOverlay) {
            boxHeight += 60; // Zusätzliche Höhe für 3 Checkboxen (20px pro Checkbox)
        }
        int boxX = width / 2 - boxWidth / 2;
        int boxY = getSettingsScreenBoxY(); // Oben bündig mit Settings-Screen
        
        // Hintergrund
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        
        // Rahmen
        context.drawBorder(boxX, boxY, boxWidth, boxHeight, 0xFFFFFFFF);
        
        // Titel
        context.drawText(textRenderer, infoName + " - Einstellungen", boxX + 10, boxY + 10, 0xFFFFFF00, false);
        
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
        
        // Prüfe ob diese Information Prozente unterstützt
        boolean supportsPercent = configKey.equals("forschung") || configKey.equals("amboss") || 
                                 configKey.equals("schmelzofen") || configKey.equals("jaeger") || 
                                 configKey.equals("seelen") || configKey.equals("essenzen") || 
                                 configKey.equals("recycler") || configKey.equals("recyclerSlot1") || 
                                 configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3") || 
                                 configKey.equals("machtkristalle");
        
        // Hintergrund Checkbox (für alle Informationen) - als oberstes Element
        int backgroundCheckboxY = boxY + 35;
        int backgroundCheckboxX = boxX + 10;
        int backgroundCheckboxSize = 10;
        int backgroundCheckboxYPos = backgroundCheckboxY;
        
        boolean showBackground = getShowBackground();
        
        // Checkbox-Hintergrund
        context.fill(backgroundCheckboxX, backgroundCheckboxYPos, backgroundCheckboxX + backgroundCheckboxSize, backgroundCheckboxYPos + backgroundCheckboxSize, 0xFF808080);
        context.drawBorder(backgroundCheckboxX, backgroundCheckboxYPos, backgroundCheckboxSize, backgroundCheckboxSize, 0xFFFFFFFF);
        
        // Checkmark wenn aktiviert
        if (showBackground) {
            // Zeichne Häkchen (✓) - gleiche Logik wie in TabInfoSettingsScreen
            int checkX = backgroundCheckboxX + 2;
            int checkY = backgroundCheckboxYPos + 2;
            int checkSize = backgroundCheckboxSize - 4;
            // Zeichne Häkchen als zwei Linien
            // Linke Linie (von oben-links nach mitte)
            for (int i = 0; i < checkSize / 2; i++) {
                int px = checkX + i;
                int py = checkY + checkSize / 2 + i;
                if (px < backgroundCheckboxX + backgroundCheckboxSize - 2 && py < backgroundCheckboxYPos + backgroundCheckboxSize - 2) {
                    context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                }
            }
            // Rechte Linie (von mitte nach unten-rechts)
            for (int i = 0; i < checkSize / 2; i++) {
                int px = checkX + checkSize / 2 + i;
                int py = checkY + checkSize - 2 - i;
                if (px < backgroundCheckboxX + backgroundCheckboxSize - 2 && py >= backgroundCheckboxYPos + 2) {
                    context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                }
            }
        }
        
        // Text
        int backgroundTextX = backgroundCheckboxX + backgroundCheckboxSize + 5;
        context.drawText(textRenderer, "Hintergrund anzeigen", backgroundTextX, backgroundCheckboxYPos + 1, 
            showBackground ? 0xFFFFFFFF : 0xFF808080, false);
        
        // Variablen für Prozente Checkbox (außerhalb if-Block für Hover-Feedback)
        int checkboxSize = 10;
        int checkboxX = boxX + 10;
        int checkboxY = boxY + 60;
        
        if (supportsPercent) {
            // Prozente Checkbox
            
            boolean showPercent = getShowPercent();
            
            // Checkbox-Hintergrund
            context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFF808080);
            context.drawBorder(checkboxX, checkboxY, checkboxSize, checkboxSize, 0xFFFFFFFF);
            
            // Checkmark wenn aktiviert
            if (showPercent) {
                // Zeichne Häkchen (✓) - gleiche Logik wie in TabInfoSettingsScreen
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
            
            // Text
            context.drawText(textRenderer, "Prozente anzeigen", checkboxX + checkboxSize + 5, checkboxY + 1, 
                showPercent ? 0xFFFFFFFF : 0xFF808080, false);
            
            // Warn-Prozentwert Eingabe
            int inputY = boxY + 85;
            int inputX = boxX + 10;
            int inputWidth = 100;
            int inputHeight = 16;
            
            // Label
            context.drawText(textRenderer, "Warn bei %:", inputX, inputY + 3, 0xFFFFFFFF, false);
            
            // Eingabefeld Hintergrund
            int fieldX = inputX + 80;
            context.fill(fieldX, inputY, fieldX + inputWidth, inputY + inputHeight, 
                isEditingWarnPercent ? 0xFF404040 : 0xFF202020);
            context.drawBorder(fieldX, inputY, inputWidth, inputHeight, 
                isEditingWarnPercent ? 0xFFFFFF00 : 0xFF808080);
            
            // Eingabefeld Text
            String displayText = warnPercentInput;
            boolean showCursor = isEditingWarnPercent && System.currentTimeMillis() % 1000 < 500;
            
            // Zeichne den Text (ohne Cursor)
            int textX = fieldX + 3;
            if (!displayText.isEmpty()) {
                context.drawText(textRenderer, displayText, textX, inputY + 4, 0xFFFFFFFF, false);
                textX += textRenderer.getWidth(displayText);
            }
            
            // Zeichne das %-Zeichen (wenn Text vorhanden ist)
            if (!displayText.isEmpty()) {
                context.drawText(textRenderer, "%", textX, inputY + 4, 0xFFFFFFFF, false);
                textX += textRenderer.getWidth("%");
            }
            
            // Zeichne den blinkenden Cursor separat
            if (showCursor) {
                context.drawText(textRenderer, "_", textX, inputY + 4, 0xFFFFFFFF, false);
            }
            
            // Hinweis
            context.drawText(textRenderer, "(Leer lassen oder -1 = deaktiviert)", 
                inputX, inputY + inputHeight + 5, 0xFF808080, false);
        } else {
            // Diese Information unterstützt keine Prozente
            context.drawText(textRenderer, "Keine zusätzlichen Einstellungen verfügbar", 
                boxX + 10, boxY + 60, 0xFF808080, false);
        }
        
        // Variablen für Icon Button (außerhalb if-Block für Hover-Feedback)
        int iconButtonY = boxY + (supportsPercent ? 135 : 85);
        int iconButtonX = boxX + 10;
        int iconButtonWidth = 280;
        int iconButtonHeight = 20;
        
        // Icon/Text Toggle Button (für Amboss, Schmelzofen und Recycler)
        if (hasIconOption) {
            
            boolean showIcon = getShowIcon();
            String iconButtonText = showIcon ? "Icon anzeigen: AN" : "Icon anzeigen: AUS";
            int iconButtonColor = showIcon ? 0xFF00FF00 : 0xFFFF0000;
            
            // Button Hintergrund
            context.fill(iconButtonX, iconButtonY, iconButtonX + iconButtonWidth, iconButtonY + iconButtonHeight, 0xFF404040);
            context.drawBorder(iconButtonX, iconButtonY, iconButtonWidth, iconButtonHeight, iconButtonColor);
            
            // Button Text
            int iconTextX = iconButtonX + (iconButtonWidth - textRenderer.getWidth(iconButtonText)) / 2;
            int iconTextY = iconButtonY + (iconButtonHeight - textRenderer.fontHeight) / 2 + 1;
            context.drawText(textRenderer, iconButtonText, iconTextX, iconTextY, 0xFFFFFFFF, false);
        }
        
        // Farben Button (für alle Informationen)
        int colorButtonY = hasIconOption ? (iconButtonY + iconButtonHeight + 10) : (boxY + (supportsPercent ? 135 : 85));
        int colorButtonX = boxX + 10;
        int colorButtonWidth = 280;
        int colorButtonHeight = 20;
        
        // Button Hintergrund
        context.fill(colorButtonX, colorButtonY, colorButtonX + colorButtonWidth, colorButtonY + colorButtonHeight, 0xFF404040);
        context.drawBorder(colorButtonX, colorButtonY, colorButtonWidth, colorButtonHeight, 0xFFFFFFFF);
        
        // Button Text
        String colorButtonText = "Farben";
        int colorTextX = colorButtonX + (colorButtonWidth - textRenderer.getWidth(colorButtonText)) / 2;
        int colorTextY = colorButtonY + (colorButtonHeight - textRenderer.fontHeight) / 2 + 1;
        context.drawText(textRenderer, colorButtonText, colorTextX, colorTextY, 0xFFFFFFFF, false);
        
        // Separate Overlay Button (für alle Informationen)
        int buttonY = colorButtonY + colorButtonHeight + 10;
        int buttonX = boxX + 10;
        int buttonWidth = 280;
        int buttonHeight = 20;
        
        boolean hasSeparateOverlayButton = getSeparateOverlay();
        String buttonText = hasSeparateOverlayButton ? "Separates Overlay: AN" : "Separates Overlay: AUS";
        int buttonColor = hasSeparateOverlayButton ? 0xFF00FF00 : 0xFFFF0000;
        
        // Button Hintergrund
        context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, 0xFF404040);
        context.drawBorder(buttonX, buttonY, buttonWidth, buttonHeight, buttonColor);
        
        // Button Text
        int textX = buttonX + (buttonWidth - textRenderer.getWidth(buttonText)) / 2;
        int textY = buttonY + (buttonHeight - textRenderer.fontHeight) / 2 + 1;
        context.drawText(textRenderer, buttonText, textX, textY, 0xFFFFFFFF, false);
        
        // Machtkristall Slot-Checkboxen (nur wenn "Separates Overlay" aktiviert ist)
        if (configKey.equals("machtkristalle") && hasSeparateOverlay) {
            int mkCheckboxStartY = buttonY + buttonHeight + 10;
            int mkCheckboxSize = 10;
            int mkCheckboxX = boxX + 10;
            int mkCheckboxSpacing = 20;
            
            for (int i = 0; i < 3; i++) {
                int mkCheckboxY = mkCheckboxStartY + (i * mkCheckboxSpacing);
                boolean mkSlotSeparate = getMachtkristallSlotSeparate(i);
                
                // Checkbox-Hintergrund
                context.fill(mkCheckboxX, mkCheckboxY, mkCheckboxX + mkCheckboxSize, mkCheckboxY + mkCheckboxSize, 0xFF808080);
                context.drawBorder(mkCheckboxX, mkCheckboxY, mkCheckboxSize, mkCheckboxSize, 0xFFFFFFFF);
                
                // Checkmark wenn aktiviert
                if (mkSlotSeparate) {
                    // Zeichne Häkchen (✓) - gleiche Logik wie in TabInfoSettingsScreen
                    int checkX = mkCheckboxX + 2;
                    int checkY = mkCheckboxY + 2;
                    int checkSize = mkCheckboxSize - 4;
                    // Zeichne Häkchen als zwei Linien
                    // Linke Linie (von oben-links nach mitte)
                    for (int j = 0; j < checkSize / 2; j++) {
                        int px = checkX + j;
                        int py = checkY + checkSize / 2 + j;
                        if (px < mkCheckboxX + mkCheckboxSize - 2 && py < mkCheckboxY + mkCheckboxSize - 2) {
                            context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                        }
                    }
                    // Rechte Linie (von mitte nach unten-rechts)
                    for (int j = 0; j < checkSize / 2; j++) {
                        int px = checkX + checkSize / 2 + j;
                        int py = checkY + checkSize - 2 - j;
                        if (px < mkCheckboxX + mkCheckboxSize - 2 && py >= mkCheckboxY + 2) {
                            context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                        }
                    }
                }
                
                // Text
                String mkCheckboxText = "MK " + (i + 1) + " Einzeln";
                int mkTextX = mkCheckboxX + mkCheckboxSize + 5;
                context.drawText(textRenderer, mkCheckboxText, mkTextX, mkCheckboxY + 1, 
                    mkSlotSeparate ? 0xFFFFFFFF : 0xFF808080, false);
            }
        }
        
        // Recycler Slot-Checkboxen (nur wenn "Separates Overlay" aktiviert ist)
        if ((isRecycler || isRecyclerSlot) && hasSeparateOverlay) {
            int recyclerCheckboxStartY = buttonY + buttonHeight + 10;
            int recyclerCheckboxSize = 10;
            int recyclerCheckboxX = boxX + 10;
            int recyclerCheckboxSpacing = 20;
            
            for (int i = 0; i < 3; i++) {
                int recyclerCheckboxY = recyclerCheckboxStartY + (i * recyclerCheckboxSpacing);
                boolean recyclerSlotSeparate = getRecyclerSlotSeparate(i);
                
                // Checkbox-Hintergrund
                context.fill(recyclerCheckboxX, recyclerCheckboxY, recyclerCheckboxX + recyclerCheckboxSize, recyclerCheckboxY + recyclerCheckboxSize, 0xFF808080);
                context.drawBorder(recyclerCheckboxX, recyclerCheckboxY, recyclerCheckboxSize, recyclerCheckboxSize, 0xFFFFFFFF);
                
                // Checkmark wenn aktiviert
                if (recyclerSlotSeparate) {
                    // Zeichne Häkchen (✓) - gleiche Logik wie in TabInfoSettingsScreen
                    int checkX = recyclerCheckboxX + 2;
                    int checkY = recyclerCheckboxY + 2;
                    int checkSize = recyclerCheckboxSize - 4;
                    // Zeichne Häkchen als zwei Linien
                    // Linke Linie (von oben-links nach mitte)
                    for (int j = 0; j < checkSize / 2; j++) {
                        int px = checkX + j;
                        int py = checkY + checkSize / 2 + j;
                        if (px < recyclerCheckboxX + recyclerCheckboxSize - 2 && py < recyclerCheckboxY + recyclerCheckboxSize - 2) {
                            context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                        }
                    }
                    // Rechte Linie (von mitte nach unten-rechts)
                    for (int j = 0; j < checkSize / 2; j++) {
                        int px = checkX + checkSize / 2 + j;
                        int py = checkY + checkSize - 2 - j;
                        if (px < recyclerCheckboxX + recyclerCheckboxSize - 2 && py >= recyclerCheckboxY + 2) {
                            context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                        }
                    }
                }
                
                // Text
                String recyclerCheckboxText = "Recycler " + (i + 1) + " Einzeln";
                int recyclerTextX = recyclerCheckboxX + recyclerCheckboxSize + 5;
                context.drawText(textRenderer, recyclerCheckboxText, recyclerTextX, recyclerCheckboxY + 1, 
                    recyclerSlotSeparate ? 0xFFFFFFFF : 0xFF808080, false);
            }
        }
        
        // Hover-Feedback für alle interaktiven Elemente (NACH allen Elementen, damit es darüber liegt)
        
        // Hintergrund Checkbox Hover
        int backgroundTextWidth = textRenderer.getWidth("Hintergrund anzeigen");
        int backgroundTextHeight = textRenderer.fontHeight;
        boolean isHoveringBackgroundCheckbox = mouseX >= backgroundCheckboxX && mouseX <= backgroundCheckboxX + backgroundCheckboxSize &&
                                              mouseY >= backgroundCheckboxYPos && mouseY <= backgroundCheckboxYPos + backgroundCheckboxSize;
        boolean isHoveringBackgroundText = mouseX >= backgroundTextX && mouseX <= backgroundTextX + backgroundTextWidth &&
                                          mouseY >= backgroundCheckboxYPos && mouseY <= backgroundCheckboxYPos + backgroundTextHeight;
        if (isHoveringBackgroundCheckbox || isHoveringBackgroundText) {
            int hoverStartX = backgroundCheckboxX - 2;
            int hoverEndX = backgroundTextX + backgroundTextWidth + 2;
            context.fill(hoverStartX, backgroundCheckboxYPos - 1, hoverEndX, backgroundCheckboxYPos + backgroundCheckboxSize + 1, 0x40FFFFFF);
        }
        
        if (supportsPercent) {
            // Prozente Checkbox Hover
            int percentTextX = checkboxX + checkboxSize + 5;
            int percentTextWidth = textRenderer.getWidth("Prozente anzeigen");
            int percentTextHeight = textRenderer.fontHeight;
            boolean isHoveringPercentCheckbox = mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
                                              mouseY >= checkboxY && mouseY <= checkboxY + checkboxSize;
            boolean isHoveringPercentText = mouseX >= percentTextX && mouseX <= percentTextX + percentTextWidth &&
                                           mouseY >= checkboxY && mouseY <= checkboxY + percentTextHeight;
            if (isHoveringPercentCheckbox || isHoveringPercentText) {
                int hoverStartX = checkboxX - 2;
                int hoverEndX = percentTextX + percentTextWidth + 2;
                context.fill(hoverStartX, checkboxY - 1, hoverEndX, checkboxY + checkboxSize + 1, 0x40FFFFFF);
            }
        }
        
        // Icon Toggle Button Hover
        if (hasIconOption) {
            boolean isHoveringIconButton = mouseX >= iconButtonX && mouseX <= iconButtonX + iconButtonWidth &&
                                         mouseY >= iconButtonY && mouseY <= iconButtonY + iconButtonHeight;
            if (isHoveringIconButton) {
                context.fill(iconButtonX - 1, iconButtonY - 1, iconButtonX + iconButtonWidth + 1, iconButtonY + iconButtonHeight + 1, 0x40FFFFFF);
            }
        }
        
        // Farben Button Hover
        boolean isHoveringColorButton = mouseX >= colorButtonX && mouseX <= colorButtonX + colorButtonWidth &&
                                       mouseY >= colorButtonY && mouseY <= colorButtonY + colorButtonHeight;
        if (isHoveringColorButton) {
            context.fill(colorButtonX - 1, colorButtonY - 1, colorButtonX + colorButtonWidth + 1, colorButtonY + colorButtonHeight + 1, 0x40FFFFFF);
        }
        
        // Separate Overlay Button Hover
        boolean isHoveringSeparateButton = mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                                         mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
        if (isHoveringSeparateButton) {
            context.fill(buttonX - 1, buttonY - 1, buttonX + buttonWidth + 1, buttonY + buttonHeight + 1, 0x40FFFFFF);
        }
        
        // Machtkristall Slot-Checkboxen Hover (nur wenn "Separates Overlay" aktiviert ist)
        if (configKey.equals("machtkristalle") && hasSeparateOverlay) {
            int mkCheckboxStartY = buttonY + buttonHeight + 10;
            int mkCheckboxSize = 10;
            int mkCheckboxX = boxX + 10;
            int mkCheckboxSpacing = 20;
            
            for (int i = 0; i < 3; i++) {
                int mkCheckboxY = mkCheckboxStartY + (i * mkCheckboxSpacing);
                String mkCheckboxText = "MK " + (i + 1) + " Einzeln";
                int mkTextX = mkCheckboxX + mkCheckboxSize + 5;
                int mkTextWidth = textRenderer.getWidth(mkCheckboxText);
                int mkTextHeight = textRenderer.fontHeight;
                
                boolean isHoveringMkCheckbox = mouseX >= mkCheckboxX && mouseX <= mkCheckboxX + mkCheckboxSize &&
                                              mouseY >= mkCheckboxY && mouseY <= mkCheckboxY + mkCheckboxSize;
                boolean isHoveringMkText = mouseX >= mkTextX && mouseX <= mkTextX + mkTextWidth &&
                                         mouseY >= mkCheckboxY && mouseY <= mkCheckboxY + mkTextHeight;
                
                if (isHoveringMkCheckbox || isHoveringMkText) {
                    int hoverStartX = mkCheckboxX - 2;
                    int hoverEndX = mkTextX + mkTextWidth + 2;
                    context.fill(hoverStartX, mkCheckboxY - 1, hoverEndX, mkCheckboxY + mkCheckboxSize + 1, 0x40FFFFFF);
                }
            }
        }
        
        // Recycler Slot-Checkboxen Hover (nur wenn "Separates Overlay" aktiviert ist)
        if ((isRecycler || isRecyclerSlot) && hasSeparateOverlay) {
            int recyclerCheckboxStartY = buttonY + buttonHeight + 10;
            int recyclerCheckboxSize = 10;
            int recyclerCheckboxX = boxX + 10;
            int recyclerCheckboxSpacing = 20;
            
            for (int i = 0; i < 3; i++) {
                int recyclerCheckboxY = recyclerCheckboxStartY + (i * recyclerCheckboxSpacing);
                String recyclerCheckboxText = "Recycler " + (i + 1) + " Einzeln";
                int recyclerTextX = recyclerCheckboxX + recyclerCheckboxSize + 5;
                int recyclerTextWidth = textRenderer.getWidth(recyclerCheckboxText);
                int recyclerTextHeight = textRenderer.fontHeight;
                
                boolean isHoveringRecyclerCheckbox = mouseX >= recyclerCheckboxX && mouseX <= recyclerCheckboxX + recyclerCheckboxSize &&
                                                    mouseY >= recyclerCheckboxY && mouseY <= recyclerCheckboxY + recyclerCheckboxSize;
                boolean isHoveringRecyclerText = mouseX >= recyclerTextX && mouseX <= recyclerTextX + recyclerTextWidth &&
                                                mouseY >= recyclerCheckboxY && mouseY <= recyclerCheckboxY + recyclerTextHeight;
                
                if (isHoveringRecyclerCheckbox || isHoveringRecyclerText) {
                    int hoverStartX = recyclerCheckboxX - 2;
                    int hoverEndX = recyclerTextX + recyclerTextWidth + 2;
                    context.fill(hoverStartX, recyclerCheckboxY - 1, hoverEndX, recyclerCheckboxY + recyclerCheckboxSize + 1, 0x40FFFFFF);
                }
            }
        }
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
    
    private void setShowPercent(boolean value) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschungPercent = value;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmbossPercent = value;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofenPercent = value;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaegerPercent = value;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelenPercent = value;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzenPercent = value;
                break;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerPercent = value;
                break;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Prüfe ob auf das Schließen-Kreuz geklickt wurde
            // Verwende exakt die gleiche Berechnung wie im Rendering
            int boxWidth = 300;
            int boxX = width / 2 - boxWidth / 2;
            int boxY = getSettingsScreenBoxY(); // Oben bündig mit Settings-Screen
            
            int closeButtonSize = 12;
            int closeButtonX = boxX + boxWidth - closeButtonSize - 5;
            int closeButtonY = boxY + 5;
            
            if (mouseX >= closeButtonX && mouseX <= closeButtonX + closeButtonSize &&
                mouseY >= closeButtonY && mouseY <= closeButtonY + closeButtonSize) {
                // Speichere Warn-Prozentwert beim Schließen
                saveWarnPercent();
                close();
                return true;
            }
            if (handleSettingsClick(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private boolean handleSettingsClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        
        int boxWidth = 300;
        // Höher für Amboss, Schmelzofen und Recycler (Icon-Option) und Hintergrund-Checkbox
        boolean hasIconOption = configKey.equals("forschung") || configKey.equals("amboss") || 
                               configKey.equals("schmelzofen") || configKey.equals("seelen") || 
                               configKey.equals("essenzen") || configKey.equals("jaeger") || 
                               configKey.equals("machtkristalle") ||
                               configKey.equals("recycler") || configKey.equals("recyclerSlot1") || 
                               configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        // Für Machtkristalle und Recycler: zusätzliche Höhe wenn "Separates Overlay" aktiviert ist (für 3 Checkboxen)
        boolean isMachtkristalle = configKey.equals("machtkristalle");
        boolean isRecycler = configKey.equals("recycler");
        boolean isRecyclerSlot = configKey.equals("recyclerSlot1") || configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        boolean hasSeparateOverlay = (isMachtkristalle || isRecycler || isRecyclerSlot) && getSeparateOverlay();
        int boxHeight = hasIconOption ? 250 : 210;
        if (hasSeparateOverlay) {
            boxHeight += 60; // Zusätzliche Höhe für 3 Checkboxen (20px pro Checkbox)
        }
        int boxX = width / 2 - boxWidth / 2;
        int boxY = getSettingsScreenBoxY(); // Oben bündig mit Settings-Screen
        
        // Prüfe ob Klick innerhalb des Overlays ist
        if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + boxHeight) {
            isEditingWarnPercent = false;
            return false;
        }
        
        // Prüfe ob diese Information Prozente unterstützt
        boolean supportsPercent = configKey.equals("forschung") || configKey.equals("amboss") || 
                                 configKey.equals("schmelzofen") || configKey.equals("jaeger") || 
                                 configKey.equals("seelen") || configKey.equals("essenzen") || 
                                 configKey.equals("recycler") || configKey.equals("recyclerSlot1") || 
                                 configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3") || 
                                 configKey.equals("machtkristalle");
        
        // Hintergrund Checkbox (für alle Informationen) - als oberstes Element
        int backgroundCheckboxY = boxY + 35;
        int backgroundCheckboxX = boxX + 10;
        int backgroundCheckboxSize = 10;
        int backgroundTextX = backgroundCheckboxX + backgroundCheckboxSize + 5;
        // Verwende die tatsächliche Text-Breite für korrekte Click-Erkennung
        int backgroundTextWidth = textRenderer.getWidth("Hintergrund anzeigen");
        int backgroundTextHeight = textRenderer.fontHeight;
        
        // Prüfe ob Klick auf Checkbox oder Text
        boolean clickedOnBackgroundCheckbox = (mouseX >= backgroundCheckboxX && mouseX <= backgroundCheckboxX + backgroundCheckboxSize &&
                                             mouseY >= backgroundCheckboxY && mouseY <= backgroundCheckboxY + backgroundCheckboxSize);
        boolean clickedOnBackgroundText = (mouseX >= backgroundTextX && mouseX <= backgroundTextX + backgroundTextWidth &&
                                          mouseY >= backgroundCheckboxY && mouseY <= backgroundCheckboxY + backgroundTextHeight);
        
        if (clickedOnBackgroundCheckbox || clickedOnBackgroundText) {
            // Toggle Hintergrund
            boolean newValue = !getShowBackground();
            setShowBackground(newValue);
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        
        if (supportsPercent) {
            // Prozente Checkbox
            int y = boxY + 60;
            int checkboxSize = 10;
            int checkboxX = boxX + 10;
            int checkboxY = y;
            int textX = checkboxX + checkboxSize + 5;
            // Verwende die tatsächliche Text-Breite für korrekte Click-Erkennung
            int textWidth = textRenderer.getWidth("Prozente anzeigen");
            int textHeight = textRenderer.fontHeight;
            
            // Prüfe ob Klick auf Checkbox oder Text
            boolean clickedOnCheckbox = (mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
                                       mouseY >= checkboxY && mouseY <= checkboxY + checkboxSize);
            boolean clickedOnText = (mouseX >= textX && mouseX <= textX + textWidth &&
                                   mouseY >= checkboxY && mouseY <= checkboxY + textHeight);
            
            if (clickedOnCheckbox || clickedOnText) {
                // Toggle Prozente
                boolean newValue = !getShowPercent();
                setShowPercent(newValue);
                CCLiveUtilitiesConfig.HANDLER.save();
                return true;
            }
            
            // Warn-Prozentwert Eingabefeld
            int inputY = boxY + 85;
            int fieldX = boxX + 90;
            int inputWidth = 100;
            int inputHeight = 16;
            
            boolean clickedOnInput = (mouseX >= fieldX && mouseX <= fieldX + inputWidth &&
                                    mouseY >= inputY && mouseY <= inputY + inputHeight);
            
            if (clickedOnInput) {
                isEditingWarnPercent = true;
                return true;
            } else {
                isEditingWarnPercent = false;
            }
        }
        
        // Berechne hasIconOption für Click-Handler (gleiche Logik wie in renderSettingsBox)
        boolean hasIconOptionClick = configKey.equals("forschung") || configKey.equals("amboss") || 
                                    configKey.equals("schmelzofen") || configKey.equals("seelen") || 
                                    configKey.equals("essenzen") || configKey.equals("jaeger") || 
                                    configKey.equals("machtkristalle") ||
                                    configKey.equals("recycler") || configKey.equals("recyclerSlot1") || 
                                    configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        
        // Icon/Text Toggle Button (für Amboss, Schmelzofen und Recycler)
        if (hasIconOptionClick) {
            int iconButtonY = boxY + (supportsPercent ? 135 : 85);
            int iconButtonX = boxX + 10;
            int iconButtonWidth = 280;
            int iconButtonHeight = 20;
            
            boolean clickedOnIconButton = (mouseX >= iconButtonX && mouseX <= iconButtonX + iconButtonWidth &&
                                          mouseY >= iconButtonY && mouseY <= iconButtonY + iconButtonHeight);
            
            if (clickedOnIconButton) {
                // Toggle Icon/Text
                boolean newValue = !getShowIcon();
                setShowIcon(newValue);
                CCLiveUtilitiesConfig.HANDLER.save();
                return true;
            }
        }
        
        // Farben Button (für alle Informationen)
        // Variablen für Icon Button (außerhalb if-Block für Click-Handler)
        int iconButtonYClick = boxY + (supportsPercent ? 135 : 85);
        int iconButtonHeightClick = 20;
        int colorButtonY = hasIconOptionClick ? (iconButtonYClick + iconButtonHeightClick + 10) : (boxY + (supportsPercent ? 135 : 85));
        int colorButtonX = boxX + 10;
        int colorButtonWidth = 280;
        int colorButtonHeight = 20;
        
        boolean clickedOnColorButton = (mouseX >= colorButtonX && mouseX <= colorButtonX + colorButtonWidth &&
                                        mouseY >= colorButtonY && mouseY <= colorButtonY + colorButtonHeight);
        
        if (clickedOnColorButton) {
            // Öffne YACL-Screen für beide Farben
            Screen colorScreen = createColorConfigScreen(this, configKey);
            client.setScreen(colorScreen);
            return true;
        }
        
        // Separate Overlay Button
        int buttonY = colorButtonY + colorButtonHeight + 10;
        int buttonX = boxX + 10;
        int buttonWidth = 280;
        int buttonHeight = 20;
        
        boolean clickedOnButton = (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                                  mouseY >= buttonY && mouseY <= buttonY + buttonHeight);
        
        if (clickedOnButton) {
            // Toggle Separate Overlay
            boolean newValue = !getSeparateOverlay();
            setSeparateOverlay(newValue);
            CCLiveUtilitiesConfig.HANDLER.save();
            return true;
        }
        
        // Machtkristall Slot-Checkboxen (nur wenn "Separates Overlay" aktiviert ist)
        if (configKey.equals("machtkristalle") && getSeparateOverlay()) {
            int mkCheckboxStartY = buttonY + buttonHeight + 10;
            int mkCheckboxSize = 10;
            int mkCheckboxX = boxX + 10;
            int mkCheckboxSpacing = 20;
            
            for (int i = 0; i < 3; i++) {
                int mkCheckboxY = mkCheckboxStartY + (i * mkCheckboxSpacing);
                String mkCheckboxText = "MK " + (i + 1) + " Einzeln";
                int mkTextX = mkCheckboxX + mkCheckboxSize + 5;
                int mkTextWidth = textRenderer.getWidth(mkCheckboxText);
                int mkTextHeight = textRenderer.fontHeight;
                
                boolean clickedOnMkCheckbox = (mouseX >= mkCheckboxX && mouseX <= mkCheckboxX + mkCheckboxSize &&
                                              mouseY >= mkCheckboxY && mouseY <= mkCheckboxY + mkCheckboxSize);
                boolean clickedOnMkText = (mouseX >= mkTextX && mouseX <= mkTextX + mkTextWidth &&
                                         mouseY >= mkCheckboxY && mouseY <= mkCheckboxY + mkTextHeight);
                
                if (clickedOnMkCheckbox || clickedOnMkText) {
                    // Toggle MK Slot Separate
                    boolean newValue = !getMachtkristallSlotSeparate(i);
                    setMachtkristallSlotSeparate(i, newValue);
                    CCLiveUtilitiesConfig.HANDLER.save();
                    return true;
                }
            }
        }
        
        // Recycler Slot-Checkboxen (nur wenn "Separates Overlay" aktiviert ist)
        if ((isRecycler || isRecyclerSlot) && getSeparateOverlay()) {
            // Verwende exakt die gleichen Berechnungen wie beim Rendering
            int recyclerCheckboxStartY = buttonY + buttonHeight + 10;
            int recyclerCheckboxSize = 10;
            int recyclerCheckboxX = boxX + 10;
            int recyclerCheckboxSpacing = 20;
            
            for (int i = 0; i < 3; i++) {
                int recyclerCheckboxY = recyclerCheckboxStartY + (i * recyclerCheckboxSpacing);
                String recyclerCheckboxText = "Recycler " + (i + 1) + " Einzeln";
                int recyclerTextX = recyclerCheckboxX + recyclerCheckboxSize + 5;
                int recyclerTextWidth = textRenderer.getWidth(recyclerCheckboxText);
                int recyclerTextHeight = textRenderer.fontHeight;
                
                // Verwende exakt die gleichen Berechnungen wie beim Rendering
                boolean clickedOnRecyclerCheckbox = (mouseX >= recyclerCheckboxX && mouseX <= recyclerCheckboxX + recyclerCheckboxSize &&
                                                    mouseY >= recyclerCheckboxY && mouseY <= recyclerCheckboxY + recyclerCheckboxSize);
                boolean clickedOnRecyclerText = (mouseX >= recyclerTextX && mouseX <= recyclerTextX + recyclerTextWidth &&
                                                mouseY >= recyclerCheckboxY && mouseY <= recyclerCheckboxY + recyclerTextHeight);
                
                if (clickedOnRecyclerCheckbox || clickedOnRecyclerText) {
                    // Toggle Recycler Slot Separate
                    boolean newValue = !getRecyclerSlotSeparate(i);
                    setRecyclerSlotSeparate(i, newValue);
                    CCLiveUtilitiesConfig.HANDLER.save();
                    return true;
                }
            }
        }
        
        return false;
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
    
    private boolean getSeparateOverlay() {
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
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay;
            case "recycler":
                // Für "recycler" prüfe ob mindestens ein Slot "Separates Overlay" hat
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay ||
                       CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay ||
                       CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay;
            default:
                return false;
        }
    }
    
    private void setSeparateOverlay(boolean value) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungSeparateOverlay = value;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossSeparateOverlay = value;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenSeparateOverlay = value;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerSeparateOverlay = value;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenSeparateOverlay = value;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenSeparateOverlay = value;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay = value;
                break;
            case "recycler":
                // Für "recycler" setze alle 3 Slots
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay = value;
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay = value;
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay = value;
                break;
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay = value;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay = value;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay = value;
                break;
        }
        
        // Aktualisiere das Overlay-Editor-Screen, wenn es geöffnet ist
        if (parent instanceof OverlayEditorScreen) {
            ((OverlayEditorScreen) parent).refreshOverlays();
        }
    }
    
    /**
     * Gibt zurück, ob ein bestimmter Machtkristall-Slot einzeln gerendert werden soll
     */
    private boolean getMachtkristallSlotSeparate(int slotIndex) {
        if (!configKey.equals("machtkristalle")) {
            return false;
        }
        switch (slotIndex) {
            case 0:
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Separate;
            case 1:
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Separate;
            case 2:
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Separate;
            default:
                return false;
        }
    }
    
    /**
     * Setzt, ob ein bestimmter Machtkristall-Slot einzeln gerendert werden soll
     */
    private void setMachtkristallSlotSeparate(int slotIndex, boolean value) {
        if (!configKey.equals("machtkristalle")) {
            return;
        }
        switch (slotIndex) {
            case 0:
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Separate = value;
                break;
            case 1:
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Separate = value;
                break;
            case 2:
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Separate = value;
                break;
        }
    }
    
    /**
     * Gibt zurück, ob ein bestimmter Recycler-Slot einzeln gerendert werden soll
     */
    private boolean getRecyclerSlotSeparate(int slotIndex) {
        boolean isRecycler = configKey.equals("recycler");
        boolean isRecyclerSlot = configKey.equals("recyclerSlot1") || configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        if (!isRecycler && !isRecyclerSlot) {
            return false;
        }
        switch (slotIndex) {
            case 0:
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate;
            case 1:
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate;
            case 2:
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate;
            default:
                return false;
        }
    }
    
    /**
     * Setzt, ob ein bestimmter Recycler-Slot einzeln gerendert werden soll
     */
    private void setRecyclerSlotSeparate(int slotIndex, boolean value) {
        boolean isRecycler = configKey.equals("recycler");
        boolean isRecyclerSlot = configKey.equals("recyclerSlot1") || configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        if (!isRecycler && !isRecyclerSlot) {
            return;
        }
        switch (slotIndex) {
            case 0:
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate = value;
                break;
            case 1:
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate = value;
                break;
            case 2:
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate = value;
                break;
        }
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (isEditingWarnPercent) {
            // Erlaube nur Zahlen, Punkt und Komma
            if ((chr >= '0' && chr <= '9') || chr == '.' || chr == ',') {
                if (warnPercentInput.length() < 10) { // Maximale Länge
                    char c = chr;
                    if (c == ',') c = '.'; // Komma zu Punkt konvertieren
                    // Prüfe, ob bereits ein Punkt vorhanden ist
                    if (c == '.' && warnPercentInput.contains(".")) {
                        return true; // Nur ein Punkt erlaubt
                    }
                    warnPercentInput += c;
                    // Automatisch speichern nach jeder Eingabe
                    saveWarnPercent();
                }
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isEditingWarnPercent) {
            if (keyCode == 259) { // Backspace
                if (!warnPercentInput.isEmpty()) {
                    warnPercentInput = warnPercentInput.substring(0, warnPercentInput.length() - 1);
                    // Automatisch speichern nach Backspace
                    saveWarnPercent();
                }
                return true;
            } else if (keyCode == 257 || keyCode == 335) { // Enter oder Numpad Enter
                saveWarnPercent();
                isEditingWarnPercent = false;
                return true;
            } else if (keyCode == 256) { // Escape
                isEditingWarnPercent = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void saveWarnPercent() {
        try {
            double value = -1.0;
            if (!warnPercentInput.trim().isEmpty()) {
                value = Double.parseDouble(warnPercentInput.replace(",", "."));
                // Begrenze auf 0-100 (0 ist erlaubt für Warnung bei 0%)
                if (value < 0) value = -1.0;
                if (value > 100) value = 100.0;
            }
            setWarnPercent(value);
            CCLiveUtilitiesConfig.HANDLER.save();
        } catch (NumberFormatException e) {
            // Ungültige Eingabe - setze auf -1 (deaktiviert)
            setWarnPercent(-1.0);
            CCLiveUtilitiesConfig.HANDLER.save();
        }
    }
    
    private double getWarnPercent() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungWarnPercent;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossWarnPercent;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenWarnPercent;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerWarnPercent;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenWarnPercent;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenWarnPercent;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleWarnPercent;
            default:
                return -1.0;
        }
    }
    
    private void setWarnPercent(double value) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungWarnPercent = value;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossWarnPercent = value;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenWarnPercent = value;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerWarnPercent = value;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenWarnPercent = value;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenWarnPercent = value;
                break;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent = value;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleWarnPercent = value;
                break;
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
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleShowIcon;
            case "recycler":
                // Für "recycler" prüfe ob mindestens ein Slot Icon aktiviert hat
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
    
    private void setShowIcon(boolean value) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungShowIcon = value;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossShowIcon = value;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenShowIcon = value;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenShowIcon = value;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenShowIcon = value;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerShowIcon = value;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleShowIcon = value;
                break;
            case "recycler":
                // Für "recycler" setze alle 3 Slots
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1ShowIcon = value;
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2ShowIcon = value;
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3ShowIcon = value;
                break;
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1ShowIcon = value;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2ShowIcon = value;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3ShowIcon = value;
                break;
        }
    }
    
    private boolean getShowBackground() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungShowBackground;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossShowBackground;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenShowBackground;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerShowBackground;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenShowBackground;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenShowBackground;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleShowBackground;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerShowBackground;
            default:
                return true;
        }
    }
    
    private void setShowBackground(boolean value) {
        switch (configKey) {
            case "forschung":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungShowBackground = value;
                break;
            case "amboss":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossShowBackground = value;
                break;
            case "schmelzofen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenShowBackground = value;
                break;
            case "jaeger":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerShowBackground = value;
                break;
            case "seelen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenShowBackground = value;
                break;
            case "essenzen":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenShowBackground = value;
                break;
            case "machtkristalle":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleShowBackground = value;
                break;
            case "recycler":
            case "recyclerSlot1":
            case "recyclerSlot2":
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerShowBackground = value;
                break;
        }
    }
    
    private Color getTextColor() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungTextColor;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossTextColor;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenTextColor;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerTextColor;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenTextColor;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenTextColor;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleTextColor;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1TextColor;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2TextColor;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3TextColor;
            default:
                return new Color(0xFFFFFFFF);
        }
    }
    
    private Color getPercentColor() {
        switch (configKey) {
            case "forschung":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungPercentColor;
            case "amboss":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossPercentColor;
            case "schmelzofen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenPercentColor;
            case "jaeger":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerPercentColor;
            case "seelen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenPercentColor;
            case "essenzen":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenPercentColor;
            case "machtkristalle":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristallePercentColor;
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1PercentColor;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2PercentColor;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3PercentColor;
            default:
                return new Color(0xFFFFFF00);
        }
    }
    
    /**
     * Erstellt einen YACL-Screen für die Farboptionen eines Tab-Info-Eintrags
     * Enthält sowohl Textfarbe als auch Prozentfarbe
     */
    private static Screen createColorConfigScreen(Screen parent, String configKey) {
        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Tab Info Farben"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Farben"))
                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Farben"))
                                .option(createColorOption(configKey, true, "Textfarbe", "Farbe für den Text"))
                                .option(createColorOption(configKey, false, "Prozentfarbe", "Farbe für die Prozentwerte"))
                                .build())
                        .build())
                .save(() -> {
                    CCLiveUtilitiesConfig.HANDLER.save();
                })
                .build()
                .generateScreen(parent);
    }
    
    /**
     * Erstellt eine Color-Option für einen Tab-Info-Eintrag
     */
    private static Option<Color> createColorOption(String configKey, boolean isTextColor, String name, String description) {
        Color defaultValue = isTextColor ? new Color(0xFFFFFFFF) : new Color(0xFFFFFF00);
        
        // Getter und Setter basierend auf configKey
        java.util.function.Supplier<Color> getter;
        java.util.function.Consumer<Color> setter;
        
        if (isTextColor) {
            switch (configKey) {
                case "forschung":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungTextColor = val;
                    break;
                case "amboss":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossTextColor = val;
                    break;
                case "schmelzofen":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenTextColor = val;
                    break;
                case "jaeger":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerTextColor = val;
                    break;
                case "seelen":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenTextColor = val;
                    break;
                case "essenzen":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenTextColor = val;
                    break;
                case "machtkristalle":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleTextColor = val;
                    break;
                case "recyclerSlot1":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1TextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1TextColor = val;
                    break;
                case "recyclerSlot2":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2TextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2TextColor = val;
                    break;
                case "recyclerSlot3":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3TextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3TextColor = val;
                    break;
                default:
                    getter = () -> defaultValue;
                    setter = (val) -> {};
            }
        } else {
            switch (configKey) {
                case "forschung":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungPercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungPercentColor = val;
                    break;
                case "amboss":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossPercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossPercentColor = val;
                    break;
                case "schmelzofen":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenPercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenPercentColor = val;
                    break;
                case "jaeger":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerPercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerPercentColor = val;
                    break;
                case "seelen":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenPercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenPercentColor = val;
                    break;
                case "essenzen":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenPercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenPercentColor = val;
                    break;
                case "machtkristalle":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristallePercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristallePercentColor = val;
                    break;
                case "recyclerSlot1":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1PercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1PercentColor = val;
                    break;
                case "recyclerSlot2":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2PercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2PercentColor = val;
                    break;
                case "recyclerSlot3":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3PercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3PercentColor = val;
                    break;
                default:
                    getter = () -> defaultValue;
                    setter = (val) -> {};
            }
        }
        
        return Option.<Color>createBuilder()
                .name(Text.literal(name))
                .description(OptionDescription.of(Text.literal(description)))
                .binding(defaultValue, getter, setter)
                .controller(ColorControllerBuilder::create)
                .build();
    }
    
    @Override
    public void close() {
        // Aktualisiere das Overlay-Editor-Screen, wenn es geöffnet ist
        // Gehe durch die Parent-Hierarchie, um das OverlayEditorScreen zu finden
        Screen currentParent = parent;
        while (currentParent != null) {
            if (currentParent instanceof OverlayEditorScreen) {
                ((OverlayEditorScreen) currentParent).refreshOverlays();
                break;
            } else if (currentParent instanceof net.felix.utilities.Overall.TabInfo.TabInfoSettingsScreen) {
                // TabInfoSettingsScreen hat auch ein parent, das das OverlayEditorScreen sein könnte
                // Wir müssen das parent des TabInfoSettingsScreen prüfen
                try {
                    java.lang.reflect.Field parentField = net.felix.utilities.Overall.TabInfo.TabInfoSettingsScreen.class.getDeclaredField("parent");
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
        
        if (client != null) {
            client.setScreen(parent);
        }
    }
}


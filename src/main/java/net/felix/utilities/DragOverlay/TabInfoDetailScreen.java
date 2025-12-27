package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

/**
 * Detail-Screen für einzelne Tab-Info-Einstellungen
 */
public class TabInfoDetailScreen extends Screen {
    
    private final Screen parent;
    private final String infoName;
    private final String configKey;
    private TextWidget titleWidget;
    private ButtonWidget doneButton;
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
        
        // Done Button
        doneButton = ButtonWidget.builder(
            Text.literal("Done"),
            button -> {
                // Speichere Warn-Prozentwert beim Schließen
                saveWarnPercent();
                close();
            }
        ).dimensions(width / 2 - 40, height - 30, 80, 20).build();
        addDrawableChild(doneButton);
        
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
    
    private void renderSettingsBox(DrawContext context, int mouseX, int mouseY) {
        int boxWidth = 300;
        int boxHeight = 180; // Höher für zusätzliche Optionen
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        // Hintergrund
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        
        // Rahmen
        context.drawBorder(boxX, boxY, boxWidth, boxHeight, 0xFFFFFFFF);
        
        // Titel
        context.drawText(textRenderer, infoName + " - Einstellungen", boxX + 10, boxY + 10, 0xFFFFFF00, false);
        
        // Prüfe ob diese Information Prozente unterstützt
        boolean supportsPercent = configKey.equals("amboss") || configKey.equals("schmelzofen") || 
                                 configKey.equals("jaeger") || configKey.equals("seelen") || 
                                 configKey.equals("essenzen") || configKey.equals("recyclerSlot1") || 
                                 configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        
        if (supportsPercent) {
            // Prozente Checkbox
            int y = boxY + 35;
            int checkboxSize = 10;
            int checkboxX = boxX + 10;
            int checkboxY = y;
            
            boolean showPercent = getShowPercent();
            
            // Checkbox-Hintergrund
            context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFF808080);
            context.drawBorder(checkboxX, checkboxY, checkboxSize, checkboxSize, 0xFFFFFFFF);
            
            // Checkmark wenn aktiviert
            if (showPercent) {
                // Zeichne Häkchen (✓)
                int checkX = checkboxX + 2;
                int checkY = checkboxY + 2;
                int checkSize = checkboxSize - 4;
                // Zeichne Häkchen als zwei Linien
                for (int i = 0; i < checkSize / 2; i++) {
                    int px = checkX + i;
                    int py = checkY + checkSize / 2 + i;
                    if (px < checkboxX + checkboxSize - 2 && py < checkboxY + checkboxSize - 2) {
                        context.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
                    }
                }
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
            y = boxY + 60;
            int inputX = boxX + 10;
            int inputY = y;
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
            String displayText = warnPercentInput.isEmpty() ? "-1 (deaktiviert)" : warnPercentInput + "%";
            if (isEditingWarnPercent && System.currentTimeMillis() % 1000 < 500) {
                displayText += "_"; // Blinkender Cursor
            }
            context.drawText(textRenderer, displayText, fieldX + 3, inputY + 4, 0xFFFFFFFF, false);
            
            // Hinweis
            context.drawText(textRenderer, "(Leer lassen oder -1 = deaktiviert)", 
                inputX, inputY + inputHeight + 5, 0xFF808080, false);
        } else {
            // Diese Information unterstützt keine Prozente
            context.drawText(textRenderer, "Keine zusätzlichen Einstellungen verfügbar", 
                boxX + 10, boxY + 35, 0xFF808080, false);
        }
        
        // Separate Overlay Button (für alle Informationen)
        int buttonY = boxY + (supportsPercent ? 110 : 60);
        int buttonX = boxX + 10;
        int buttonWidth = 280;
        int buttonHeight = 20;
        
        boolean hasSeparateOverlay = getSeparateOverlay();
        String buttonText = hasSeparateOverlay ? "Separates Overlay: AN" : "Separates Overlay: AUS";
        int buttonColor = hasSeparateOverlay ? 0xFF00FF00 : 0xFFFF0000;
        
        // Button Hintergrund
        context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, 0xFF404040);
        context.drawBorder(buttonX, buttonY, buttonWidth, buttonHeight, buttonColor);
        
        // Button Text
        int textX = buttonX + (buttonWidth - textRenderer.getWidth(buttonText)) / 2;
        int textY = buttonY + (buttonHeight - textRenderer.fontHeight) / 2 + 1;
        context.drawText(textRenderer, buttonText, textX, textY, 0xFFFFFFFF, false);
    }
    
    private boolean getShowPercent() {
        switch (configKey) {
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
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1Percent;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2Percent;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3Percent;
            default:
                return false;
        }
    }
    
    private void setShowPercent(boolean value) {
        switch (configKey) {
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
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1Percent = value;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2Percent = value;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3Percent = value;
                break;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
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
        int boxHeight = 180;
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        // Prüfe ob Klick innerhalb des Overlays ist
        if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + boxHeight) {
            isEditingWarnPercent = false;
            return false;
        }
        
        // Prüfe ob diese Information Prozente unterstützt
        boolean supportsPercent = configKey.equals("amboss") || configKey.equals("schmelzofen") || 
                                 configKey.equals("jaeger") || configKey.equals("seelen") || 
                                 configKey.equals("essenzen") || configKey.equals("recyclerSlot1") || 
                                 configKey.equals("recyclerSlot2") || configKey.equals("recyclerSlot3");
        
        if (supportsPercent) {
            // Prozente Checkbox
            int y = boxY + 35;
            int checkboxSize = 10;
            int checkboxX = boxX + 10;
            int checkboxY = y;
            int textX = checkboxX + checkboxSize + 5;
            int textWidth = boxWidth - textX - 10;
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
            int inputY = boxY + 60;
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
        
        // Separate Overlay Button
        int buttonY = boxY + (supportsPercent ? 110 : 60);
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
        
        return false;
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
                // Begrenze auf 0-100
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
            case "recyclerSlot1":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1WarnPercent;
            case "recyclerSlot2":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2WarnPercent;
            case "recyclerSlot3":
                return CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3WarnPercent;
            default:
                return -1.0;
        }
    }
    
    private void setWarnPercent(double value) {
        switch (configKey) {
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
            case "recyclerSlot1":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1WarnPercent = value;
                break;
            case "recyclerSlot2":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2WarnPercent = value;
                break;
            case "recyclerSlot3":
                CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3WarnPercent = value;
                break;
        }
    }
    
    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}


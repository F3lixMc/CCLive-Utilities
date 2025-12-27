package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen für Tab-Info-Einstellungen
 * Ähnlich wie das Overlay-Settings-Menü
 */
public class TabInfoSettingsScreen extends Screen {
    
    private final Screen parent;
    private TextWidget titleWidget;
    private ButtonWidget doneButton;
    private List<TabInfoEntry> entries;
    
    public TabInfoSettingsScreen(Screen parent) {
        super(Text.literal("Tab Info Settings"));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Title
        titleWidget = new TextWidget(
            Text.literal("Tab Info Settings"),
            textRenderer
        );
        titleWidget.setPosition(width / 2 - titleWidget.getWidth() / 2, 20);
        addDrawableChild(titleWidget);
        
        // Done Button
        doneButton = ButtonWidget.builder(
            Text.literal("Done"),
            button -> close()
        ).dimensions(width / 2 - 40, height - 30, 80, 20).build();
        addDrawableChild(doneButton);
        
        // Erstelle Einträge
        entries = new ArrayList<>();
        entries.add(new TabInfoEntry("Forschung", "forschung", () -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschung, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschung = val));
        entries.add(new TabInfoEntry("Amboss Kapazität", "amboss", () -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmboss, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmboss = val));
        entries.add(new TabInfoEntry("Schmelzofen Kapazität", "schmelzofen", () -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofen, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofen = val));
        entries.add(new TabInfoEntry("Jäger Kapazität", "jaeger", () -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaeger, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaeger = val));
        entries.add(new TabInfoEntry("Seelen Kapazität", "seelen", () -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelen, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelen = val));
        entries.add(new TabInfoEntry("Essenzen Kapazität", "essenzen", () -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzen, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzen = val));
        entries.add(new TabInfoEntry("Machtkristalle", "machtkristalle", () -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle = val));
        entries.add(new TabInfoEntry("Recycler Slot 1", "recyclerSlot1", () -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 = val));
        entries.add(new TabInfoEntry("Recycler Slot 2", "recyclerSlot2", () -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 = val));
        entries.add(new TabInfoEntry("Recycler Slot 3", "recyclerSlot3", () -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3, 
            val -> CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 = val));
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
        int boxHeight = Math.min(500, entries.size() * 25 + 40);
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        // Hintergrund
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        
        // Rahmen
        context.drawBorder(boxX, boxY, boxWidth, boxHeight, 0xFFFFFFFF);
        
        // Titel
        context.drawText(textRenderer, "Tab Info Einstellungen", boxX + 10, boxY + 10, 0xFFFFFF00, false);
        
        // Checkboxen für alle Einträge
        int y = boxY + 35;
        int checkboxSize = 10;
        int checkboxSpacing = 25;
        
        for (TabInfoEntry entry : entries) {
            boolean isEnabled = entry.isEnabled.get();
            
            // Checkbox-Hintergrund
            int checkboxX = boxX + 10;
            int checkboxY = y;
            context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFF808080);
            context.drawBorder(checkboxX, checkboxY, checkboxSize, checkboxSize, 0xFFFFFFFF);
            
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
            context.drawText(textRenderer, entryName, textX, checkboxY + 1, isEnabled ? 0xFFFFFFFF : 0xFF808080, false);
            
            // Zahnrad-Icon am Ende der Zeile
            int gearSize = 12;
            int gearX = boxX + boxWidth - gearSize - 10;
            int gearY = checkboxY - 1;
            drawGearIcon(context, gearX, gearY, gearSize);
            
            y += checkboxSpacing;
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
        int boxHeight = Math.min(500, entries.size() * 25 + 40);
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
        int textWidth = boxWidth - textX - 30; // Platz für Zahnrad-Icon
        int gearSize = 12;
        
        for (TabInfoEntry entry : entries) {
            int checkboxY = y;
            int textY = checkboxY;
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
                return true;
            } else if (clickedOnGear) {
                // Öffne Detail-Screen für diese Information
                if (client != null) {
                    client.setScreen(new TabInfoDetailScreen(this, entry.displayName, entry.configKey));
                }
                return true;
            }
            
            y += checkboxSpacing;
        }
        
        return false;
    }
    
    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
    
    /**
     * Zeichnet ein Zahnrad-Icon (vereinfachte Version mit fill)
     */
    private void drawGearIcon(DrawContext context, int x, int y, int size) {
        // Zeichne ein einfaches Zahnrad-Icon als gefüllte Formen
        // Verwende ein einfaches Icon: ein Quadrat mit diagonalen Linien
        // Äußerer Rahmen
        context.drawBorder(x, y, size, size, 0xFFFFFFFF);
        
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
    
    /**
     * Hilfsklasse für Tab-Info-Einträge
     */
    private static class TabInfoEntry {
        String displayName;
        String configKey; // Für Detail-Screen
        java.util.function.Supplier<Boolean> isEnabled;
        java.util.function.Consumer<Boolean> setEnabled;
        
        TabInfoEntry(String displayName, String configKey, java.util.function.Supplier<Boolean> isEnabled, java.util.function.Consumer<Boolean> setEnabled) {
            this.displayName = displayName;
            this.configKey = configKey;
            this.isEnabled = isEnabled;
            this.setEnabled = setEnabled;
        }
    }
}


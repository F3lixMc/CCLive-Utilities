package net.felix.features.itemviewer.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static net.felix.features.itemviewer.ui.CategoryButtons.CATEGORY_BUTTON_DEFS;
import static net.felix.features.itemviewer.ui.CategoryButtons.computeCategoryButtons;
import static net.felix.features.itemviewer.ui.CategoryButtons.renderCategoryButtons;
import static net.felix.features.itemviewer.ui.ViewerState.activeCategoryOverlay;
import static net.felix.features.itemviewer.ui.ViewerState.helpScreenHovered;
import static net.felix.features.itemviewer.ui.ViewerState.helpScreenOpen;
import static net.felix.features.itemviewer.ui.ViewerState.helpScreenScrollOffset;
import static net.felix.features.itemviewer.ui.ViewerState.lastMouseX;
import static net.felix.features.itemviewer.ui.ViewerState.lastMouseY;

/**
 * Hilfe-Overlay des Item Viewers (Anzeige, Klicks, Schliessen).
 */
public final class HelpOverlay {

    private HelpOverlay() {
    }

    /**
     * Rendert das Hilfe-Overlay (wird von den Mixins aufgerufen, damit es über allem liegt)
     */
    public static void renderHelpOverlay(GuiGraphicsExtractor context) {
        if (helpScreenOpen) {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                drawHelpScreen(context, client);
            }
        }
    }

    /**
     * Behandelt ESC-Taste zum Schließen des Hilfe-Overlays
     * @return true wenn ESC behandelt wurde (Overlay war offen und wurde geschlossen)
     */
    public static boolean handleHelpOverlayEscape() {
        if (helpScreenOpen) {
            helpScreenOpen = false;
            activeCategoryOverlay = null;
            helpScreenScrollOffset = 0; // Reset scroll when closing
            return true;
        }
        return false;
    }

    /**
     * Schließt das Hilfe-Overlay (wird aufgerufen, wenn das Inventar geschlossen wird)
     */
    public static void closeHelpOverlay() {
        helpScreenOpen = false;
        activeCategoryOverlay = null;
        helpScreenScrollOffset = 0; // Reset scroll when closing
    }

    /**
     * Prüft ob das Hilfe-Overlay geöffnet ist
     */
    public static boolean isHelpOverlayOpen() {
        return helpScreenOpen;
    }

    /**
     * Behandelt Klicks auf das Hilfe-Overlay (wird vom Mixin aufgerufen, damit es vor anderen Klicks geprüft wird)
     */
    public static boolean handleHelpOverlayClick(double mouseX, double mouseY, int button) {
        if (!helpScreenOpen || button != 0) {
            return false;
        }
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        
        // Berechne boxHeight mit gleicher Logik wie beim Rendering
        int buttonHeight = 16;
        int maxButtonsPerRow = 4;
        int buttonSpacingY = 20; // Muss mit computeCategoryButtons übereinstimmen
        int numButtonRows = (int) Math.ceil((double) CATEGORY_BUTTON_DEFS.size() / maxButtonsPerRow);
        int baseHeight = 500; // Erhöht von 450 auf 500 für mehr Platz
        // Berechne benötigte Höhe für Button-Bereich (inkl. Header und Abstände)
        int headerHeight = client.font.lineHeight;
        int spacing = 4;
        // Gesamte Button-Bereich-Höhe: Header + Abstand + (alle Zeilen * Button-Höhe) + (Abstände zwischen Zeilen)
        int buttonAreaHeight = headerHeight + spacing + (numButtonRows * buttonHeight) + ((numButtonRows - 1) * buttonSpacingY);
        // Zusätzliche Höhe: Button-Bereich + 20px Padding unten + 15px Abstand zum Text
        int additionalHeight = buttonAreaHeight + 35; // 20px Padding unten + 15px Abstand zum Text
        int boxWidth = Math.min(400, screenWidth - 40);
        int boxHeight = Math.min(baseHeight + additionalHeight, screenHeight - 40);
        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = (screenHeight - boxHeight) / 2;
        int closeButtonX = boxX + boxWidth - 20;
        int closeButtonY = boxY + 5;
        int closeButtonSize = 15;
        
        // Prüfe Klick auf Schließen-Button
        if (mouseX >= closeButtonX && mouseX < closeButtonX + closeButtonSize &&
            mouseY >= closeButtonY && mouseY < closeButtonY + closeButtonSize) {
            helpScreenOpen = false;
            activeCategoryOverlay = null;
            return true;
        }

        // Berechne Button-Position (gleiche Logik wie beim Rendering)
        // Berechne buttonY so, dass alle Buttons innerhalb des Overlays bleiben
        // Die letzte Zeile sollte bei boxY + boxHeight - 8 enden
        // Die letzte Zeile endet bei: buttonY + (numButtonRows - 1) * buttonSpacingY + buttonHeight
        // Also: buttonY = boxY + boxHeight - 8 - (numButtonRows - 1) * buttonSpacingY - buttonHeight
        int buttonY = boxY + boxHeight - 8 - ((numButtonRows - 1) * buttonSpacingY) - buttonHeight;
        int startX = boxX + 15;
        
        // Prüfe Klick auf Tag-Overlay (wenn geöffnet) - VOR Button-Checks, damit Overlay-Klicks nicht Buttons blockieren
        if (activeCategoryOverlay != null && activeCategoryOverlay.tags != null && !activeCategoryOverlay.tags.isEmpty()) {
            int lineHeight = client.font.lineHeight + 2;
            int maxTagsPerColumn = 10;
            int numTags = activeCategoryOverlay.tags.size();
            
            // Berechne Anzahl der Spalten (jede Spalte hat maximal 10 Tags)
            int numColumns = (int) Math.ceil((double) numTags / maxTagsPerColumn);
            
            // Berechne Anzahl der Zeilen (maximal 10 pro Spalte)
            int numRows = Math.min(maxTagsPerColumn, numTags);
            int overlayHeight = numRows * lineHeight + 6;
            
            // Berechne Breite für jede Spalte
            java.util.List<Integer> columnWidths = new java.util.ArrayList<>();
            for (int col = 0; col < numColumns; col++) {
                int columnWidth = 0;
                int startIdx = col * maxTagsPerColumn;
                int endIdx = Math.min(startIdx + maxTagsPerColumn, numTags);
                for (int i = startIdx; i < endIdx; i++) {
                    columnWidth = Math.max(columnWidth, client.font.width(activeCategoryOverlay.tags.get(i)));
                }
                columnWidth += 12; // Padding
                columnWidths.add(columnWidth);
            }
            
            // Berechne Gesamtbreite des Overlays
            int columnSpacing = 8; // Abstand zwischen den Spalten
            int overlayWidth = 0;
            for (int i = 0; i < columnWidths.size(); i++) {
                overlayWidth += columnWidths.get(i);
                if (i < columnWidths.size() - 1) {
                    overlayWidth += columnSpacing;
                }
            }
            
            int ox = activeCategoryOverlay.x;
            int oy = activeCategoryOverlay.y + activeCategoryOverlay.height + 6;
            if (oy + overlayHeight > boxY + boxHeight - 8) {
                oy = activeCategoryOverlay.y - overlayHeight - 6;
            }
            if (ox + overlayWidth > boxX + boxWidth - 8) {
                ox = boxX + boxWidth - overlayWidth - 8;
            }
            
            // Wenn Klick im Tag-Overlay-Bereich, verhindere andere Aktionen
            if (mouseX >= ox && mouseX < ox + overlayWidth &&
                mouseY >= oy && mouseY < oy + overlayHeight) {
                return true; // Klick im Tag-Overlay, verhindere andere Aktionen
            }
        }
        
        // Kategorie-Buttons prüfen (VOR anderen Checks, damit Klicks funktionieren)
        java.util.List<CategoryButton> buttons = computeCategoryButtons(client.font, boxX, boxY, boxWidth, boxHeight, buttonY, startX);
        for (CategoryButton btn : buttons) {
            if (mouseX >= btn.x && mouseX < btn.x + btn.width &&
                mouseY >= btn.y && mouseY < btn.y + btn.height) {
                // Toggle aktuelles Overlay
                if (activeCategoryOverlay != null && activeCategoryOverlay.name.equals(btn.name)) {
                    activeCategoryOverlay = null;
                } else {
                    activeCategoryOverlay = btn;
                }
                return true;
            }
        }
        
        // Klick auf andere Bereiche innerhalb der Hilfe-Box schließt das Kategorie-Overlay (falls offen)
        if (activeCategoryOverlay != null) {
            activeCategoryOverlay = null;
            return true;
        }
        
        // Klick außerhalb des Hilfe-Overlays schließt es
        if (mouseX < boxX || mouseX >= boxX + boxWidth ||
            mouseY < boxY || mouseY >= boxY + boxHeight) {
            helpScreenOpen = false;
            activeCategoryOverlay = null;
            return true;
        }
        
        // Klick innerhalb des Overlays verhindert andere Klicks
        return true;
    }

    /**
     * Zeichnet das Hilfe-Overlay für den Item Viewer
     */
    public static void drawHelpScreen(GuiGraphicsExtractor context, Minecraft client) {
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        
        // Hintergrund-Overlay
        context.fill(0, 0, screenWidth, screenHeight, 0x80000000);
        
        // Berechne benötigte Höhe für Button-Bereich (mehrere Zeilen möglich)
        int buttonHeight = 16;
        int maxButtonsPerRow = 4;
        int buttonSpacingY = 20; // Muss mit computeCategoryButtons übereinstimmen
        int numButtonRows = (int) Math.ceil((double) CATEGORY_BUTTON_DEFS.size() / maxButtonsPerRow);
        
        // Hilfe-Box - Angepasste Größe für verschiedene Bildschirmgrößen
        // Mindesthöhe: 500px (erhöht für mehr Platz) + zusätzlicher Platz für Button-Zeilen
        int baseHeight = 500; // Erhöht von 450 auf 500 für mehr Platz
        // Berechne benötigte Höhe für Button-Bereich (inkl. Header und Abstände)
        int headerHeight = client.font.lineHeight;
        int spacing = 4;
        // Gesamte Button-Bereich-Höhe: Header + Abstand + (alle Zeilen * Button-Höhe) + (Abstände zwischen Zeilen)
        int buttonAreaHeight = headerHeight + spacing + (numButtonRows * buttonHeight) + ((numButtonRows - 1) * buttonSpacingY);
        // Zusätzliche Höhe: Button-Bereich + 20px Padding unten + 15px Abstand zum Text
        int additionalHeight = buttonAreaHeight + 35; // 20px Padding unten + 15px Abstand zum Text
        int boxWidth = Math.min(400, screenWidth - 40); // Max 400px oder Bildschirmbreite - 40px
        int boxHeight = Math.min(baseHeight + additionalHeight, screenHeight - 40); // Dynamische Höhe basierend auf Button-Zeilen
        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = (screenHeight - boxHeight) / 2;
        
        // Prüfe ob Maus über dem Hilfe-Overlay ist (für Scrolling)
        helpScreenHovered = lastMouseX >= boxX && lastMouseX < boxX + boxWidth &&
                           lastMouseY >= boxY && lastMouseY < boxY + boxHeight;
        
        // Box-Hintergrund
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        
        // Box-Rahmen
        context.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFFFFFFFF);
        context.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFFFFFFFF);
        context.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFFFFFFFF);
        context.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFFFFFFFF);
        
        // Schließen-Button (X oben rechts)
        int closeButtonX = boxX + boxWidth - 20;
        int closeButtonY = boxY + 5;
        int closeButtonSize = 15;
        boolean closeHovered = lastMouseX >= closeButtonX && lastMouseX < closeButtonX + closeButtonSize &&
                               lastMouseY >= closeButtonY && lastMouseY < closeButtonY + closeButtonSize;
        
        // Schließen-Button Hintergrund
        context.fill(closeButtonX, closeButtonY, closeButtonX + closeButtonSize, closeButtonY + closeButtonSize, 
                     closeHovered ? 0xFF404040 : 0x80000000);
        // Schließen-Button Rahmen
        context.fill(closeButtonX, closeButtonY, closeButtonX + closeButtonSize, closeButtonY + 1, 0xFFFFFFFF);
        context.fill(closeButtonX, closeButtonY + closeButtonSize - 1, closeButtonX + closeButtonSize, closeButtonY + closeButtonSize, 0xFFFFFFFF);
        context.fill(closeButtonX, closeButtonY, closeButtonX + 1, closeButtonY + closeButtonSize, 0xFFFFFFFF);
        context.fill(closeButtonX + closeButtonSize - 1, closeButtonY, closeButtonX + closeButtonSize, closeButtonY + closeButtonSize, 0xFFFFFFFF);
        
        // X-Zeichen im Schließen-Button
        String closeText = "×";
        int closeTextWidth = client.font.width(closeText);
        int closeTextHeight = client.font.lineHeight;
        int closeTextX = closeButtonX + (closeButtonSize - closeTextWidth) / 2 + 1; // Horizontal zentriert (+1 für bessere visuelle Zentrierung)
        int closeTextY = closeButtonY + (closeButtonSize - closeTextHeight) / 2 + 1; // Vertikal zentriert (+1 für bessere Zentrierung)
        context.text(
            client.font,
            closeText,
            closeTextX,
            closeTextY,
            0xFFFFFFFF,
            true
        );
        
        // Titel
        String title = "Item Viewer Hilfe";
        int titleWidth = client.font.width(title);
        context.text(
            client.font,
            title,
            boxX + (boxWidth - titleWidth) / 2,
            boxY + 10,
            0xFFFFFF00,
            true
        );
        
        // Hilfe-Text mit Scrolling
        int textStartY = boxY + 35;
        int lineHeight = 11;
        int textX = boxX + 15;
        int maxTextWidth = boxWidth - 30;
        
        // Berechne verfügbare Höhe für Text (oberhalb der Buttons)
        int numRows = (int) Math.ceil((double) CATEGORY_BUTTON_DEFS.size() / maxButtonsPerRow);
        // headerHeight und spacing wurden bereits oben definiert
        int buttonY = boxY + boxHeight - 8 - ((numRows - 1) * buttonSpacingY) - buttonHeight;
        int minButtonYForText = textStartY + 15; // Mindestens 15px nach dem letzten Text
        if (buttonY < minButtonYForText) {
            buttonY = minButtonYForText;
        }
        int availableTextHeight = buttonY - textStartY - 15; // Verfügbare Höhe für Text
        
        String[] helpText = {
            "Grundlegende Suche:",
            "• Suche nach Item-Namen",
            "",
            "Erweiterte Suche:",
            "• #Tag - Tags (z.B. #Schuhe exakt, #Schuh als Vorschau mit Teiltreffern)",
            "• @Stat>Wert - Stats (z.B. @Schaden>=100, @Rüstung<50)",
            "• @Ebene>Wert - Ebenen (z.B. @Ebene>=50, @Ebene<30, >e5, e4)",
            "• Operatoren: >, <, =, >=, <=",
            "• +Modifier - Suche nach Modifiern (z.B. +Andere, +Andere:2)",
            "• Kosten:Anzahl - Suche nach Kosten (z.B. amboss:0, material:<50, ressource:>100)",
            "• material:Anzahl / material:Name - Ebenen-Material (z.B. material:51, material:Spinnfaden, material:<50). Operatoren: >, <, =, >=, <=",
            "• Gültige Kategorien: material, ressource, ofen, amboss",
            "• @Aspekt Name - Suche nach Aspekten (z.B. @Aspekt der Flamme)",
            "",
            "Sortierung:",
            "• Standard - Sortiert nach Tags",
            "• Name A-Z - Alphabetisch nach Namen",
            "• Ebene Ebene 1-100 - Sortiert nach Ebene",
            "",
            "Navigation:",
            "• Nutze die Pfeile unten für Seitenwechsel oder Scrollen",
            "• ESC schließt das Hilfe-Overlay",
            ""
        };
        
        // Berechne Gesamthöhe des Textes (mit Umbrechung)
        java.util.List<String> allTextLines = new java.util.ArrayList<>();
        for (String line : helpText) {
            if (client.font.width(line) > maxTextWidth && !line.isEmpty()) {
                String[] words = line.split(" ");
                String currentLine = "";
                for (String word : words) {
                    String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                    if (client.font.width(testLine) > maxTextWidth) {
                        if (!currentLine.isEmpty()) {
                            allTextLines.add(currentLine);
                            currentLine = word;
                        } else {
                            allTextLines.add(word);
                        }
                    } else {
                        currentLine = testLine;
                    }
                }
                if (!currentLine.isEmpty()) {
                    allTextLines.add(currentLine);
                }
            } else {
                allTextLines.add(line);
            }
        }
        
        int totalTextHeight = allTextLines.size() * lineHeight;
        
        // Begrenze Scroll-Offset
        int maxScrollOffset = Math.max(0, totalTextHeight - availableTextHeight);
        helpScreenScrollOffset = Math.min(helpScreenScrollOffset, maxScrollOffset);
        
        // Berechne Start-Index basierend auf Scroll-Offset
        int startLineIndex = helpScreenScrollOffset / lineHeight;
        int visibleLines = (int) Math.ceil((double) availableTextHeight / lineHeight) + 1;
        
        // Zeichne sichtbare Zeilen
        int textY = textStartY - (helpScreenScrollOffset % lineHeight);
        for (int i = startLineIndex; i < allTextLines.size() && i < startLineIndex + visibleLines; i++) {
            String line = allTextLines.get(i);
            if (textY >= textStartY - lineHeight && textY < buttonY) {
                context.text(
                    client.font,
                    line,
                    textX,
                    textY,
                    0xFFFFFFFF,
                    true
                );
            }
            textY += lineHeight;
        }
        
        // Zeige Scroll-Indikatoren wenn nötig
        if (helpScreenScrollOffset > 0) {
            String moreText = String.format("↑ %d weitere (Scrollen)", startLineIndex);
            int moreTextWidth = client.font.width(moreText);
            context.text(
                client.font,
                moreText,
                boxX + (boxWidth - moreTextWidth) / 2,
                textStartY - 2,
                0xFFFFFF00,
                true
            );
        }
        
        // Kategorie-Buttons und Tag-Overlay (ganz unten)
        // numRows und buttonY wurden bereits oben berechnet
        // buttonY wurde bereits oben berechnet
        int finalButtonY = buttonY;
        
        // Prüfe ob genug Platz vorhanden ist (maxScrollOffset == 0 bedeutet genug Platz)
        boolean hasEnoughSpace = (maxScrollOffset == 0);
        
        if (hasEnoughSpace) {
            // Bei genug Platz (z.B. GUI Scale 3): Buttons immer anzeigen, keine Scroll-Logik
            renderCategoryButtons(context, client.font, boxX, boxY, boxWidth, boxHeight, finalButtonY);
        } else {
            // Bei wenig Platz (z.B. GUI Scale 4): Buttons nur anzeigen, wenn gescrollt wurde
            if (helpScreenScrollOffset > 0) {
                // Zeige "↓ X weitere (Scrollen)" nur wenn noch mehr Text vorhanden ist
                if (helpScreenScrollOffset < maxScrollOffset) {
                    int remainingLines = allTextLines.size() - (startLineIndex + visibleLines);
                    if (remainingLines > 0) {
                        String moreText = String.format("↓ %d weitere (Scrollen)", remainingLines);
                        int moreTextWidth = client.font.width(moreText);
                        context.text(
                            client.font,
                            moreText,
                            boxX + (boxWidth - moreTextWidth) / 2,
                            buttonY - lineHeight - 2,
                            0xFFFFFF00,
                            true
                        );
                    }
                }
                renderCategoryButtons(context, client.font, boxX, boxY, boxWidth, boxHeight, finalButtonY);
            } else {
                // Zeige Hinweis zum Scrollen in der untersten Zeile (nur wenn noch mehr Text vorhanden ist)
                String scrollHint = "↓ Scrollen für Kategorien mit Tags";
                int scrollHintWidth = client.font.width(scrollHint);
                int scrollHintY = buttonY - lineHeight - 2 + 15; // 15px tiefer
                context.text(
                    client.font,
                    scrollHint,
                    boxX + (boxWidth - scrollHintWidth) / 2,
                    scrollHintY,
                    0xFFFFFF00,
                    true
                );
            }
        }
    }
}

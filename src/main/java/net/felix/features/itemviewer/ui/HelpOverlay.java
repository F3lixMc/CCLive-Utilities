package net.felix.features.itemviewer.ui;

import java.util.ArrayList;
import java.util.List;
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

    private static final int BUTTON_HEIGHT = 16;
    private static final int MAX_BUTTONS_PER_ROW = 4;
    private static final int BUTTON_SPACING_Y = 20; // Muss mit computeCategoryButtons übereinstimmen
    private static final int BASE_HEIGHT = 500; // Mindesthöhe der Box vor dem Button-Bereich
    private static final int CLOSE_BUTTON_SIZE = 15;

    private static final String[] HELP_TEXT = {
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

        HelpBox box = computeBox(client);

        // Klick auf Schließen-Button
        if (isInside(mouseX, mouseY, box.closeButtonX(), box.closeButtonY(), CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)) {
            helpScreenOpen = false;
            activeCategoryOverlay = null;
            return true;
        }

        // Tag-Overlay (wenn geöffnet) VOR den Button-Checks, damit Overlay-Klicks Buttons nicht blockieren
        if (isClickInsideTagOverlay(client, box, mouseX, mouseY)) {
            return true;
        }

        // Kategorie-Buttons (gleiche Position wie beim Rendering, aber ohne Mindest-Abstand zum Text)
        int buttonY = computeButtonY(box);
        int startX = box.x() + 15;
        List<CategoryButton> buttons = computeCategoryButtons(client.font, box.x(), box.y(), box.width(), box.height(), buttonY, startX);
        for (CategoryButton btn : buttons) {
            if (isInside(mouseX, mouseY, btn.x, btn.y, btn.width, btn.height)) {
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
        if (!isInside(mouseX, mouseY, box.x(), box.y(), box.width(), box.height())) {
            helpScreenOpen = false;
            activeCategoryOverlay = null;
            return true;
        }

        // Klick innerhalb des Overlays verhindert andere Klicks
        return true;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** Position und Größe der Hilfe-Box (angepasst an Bildschirmgröße und Anzahl der Button-Zeilen). */
    private record HelpBox(int x, int y, int width, int height) {
        int closeButtonX() {
            return x + width - 20;
        }

        int closeButtonY() {
            return y + 5;
        }
    }

    private static int numButtonRows() {
        return (int) Math.ceil((double) CATEGORY_BUTTON_DEFS.size() / MAX_BUTTONS_PER_ROW);
    }

    /** Gleiche Logik für Rendering und Klick-Behandlung. */
    private static HelpBox computeBox(Minecraft client) {
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int numButtonRows = numButtonRows();
        // Gesamte Button-Bereich-Höhe: Header + Abstand + (alle Zeilen * Button-Höhe) + (Abstände zwischen Zeilen)
        int headerHeight = client.font.lineHeight;
        int spacing = 4;
        int buttonAreaHeight = headerHeight + spacing + (numButtonRows * BUTTON_HEIGHT) + ((numButtonRows - 1) * BUTTON_SPACING_Y);
        // Zusätzliche Höhe: Button-Bereich + 20px Padding unten + 15px Abstand zum Text
        int additionalHeight = buttonAreaHeight + 35;

        int boxWidth = Math.min(400, screenWidth - 40); // Max 400px oder Bildschirmbreite - 40px
        int boxHeight = Math.min(BASE_HEIGHT + additionalHeight, screenHeight - 40);
        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = (screenHeight - boxHeight) / 2;
        return new HelpBox(boxX, boxY, boxWidth, boxHeight);
    }

    /**
     * Y-Position der Kategorie-Buttons, sodass alle Zeilen innerhalb der Box bleiben:
     * die letzte Zeile endet bei boxY + boxHeight - 8.
     */
    private static int computeButtonY(HelpBox box) {
        return box.y() + box.height() - 8 - ((numButtonRows() - 1) * BUTTON_SPACING_Y) - BUTTON_HEIGHT;
    }

    /** Prüft, ob der Klick im geöffneten Tag-Overlay einer Kategorie liegt (verhindert dann andere Aktionen). */
    private static boolean isClickInsideTagOverlay(Minecraft client, HelpBox box, double mouseX, double mouseY) {
        CategoryButton overlay = activeCategoryOverlay;
        if (overlay == null || overlay.tags == null || overlay.tags.isEmpty()) {
            return false;
        }
        int lineHeight = client.font.lineHeight + 2;
        int maxTagsPerColumn = 10;
        int numTags = overlay.tags.size();

        // Jede Spalte hat maximal 10 Tags
        int numColumns = (int) Math.ceil((double) numTags / maxTagsPerColumn);
        int numRows = Math.min(maxTagsPerColumn, numTags);
        int overlayHeight = numRows * lineHeight + 6;

        // Gesamtbreite: Breite jeder Spalte (längster Tag + Padding) plus Abstand zwischen den Spalten
        int columnSpacing = 8;
        int overlayWidth = 0;
        for (int col = 0; col < numColumns; col++) {
            int columnWidth = 0;
            int startIdx = col * maxTagsPerColumn;
            int endIdx = Math.min(startIdx + maxTagsPerColumn, numTags);
            for (int i = startIdx; i < endIdx; i++) {
                columnWidth = Math.max(columnWidth, client.font.width(overlay.tags.get(i)));
            }
            columnWidth += 12; // Padding
            overlayWidth += columnWidth;
            if (col < numColumns - 1) {
                overlayWidth += columnSpacing;
            }
        }

        int ox = overlay.x;
        int oy = overlay.y + overlay.height + 6;
        if (oy + overlayHeight > box.y() + box.height() - 8) {
            oy = overlay.y - overlayHeight - 6;
        }
        if (ox + overlayWidth > box.x() + box.width() - 8) {
            ox = box.x() + box.width() - overlayWidth - 8;
        }
        return isInside(mouseX, mouseY, ox, oy, overlayWidth, overlayHeight);
    }

    /**
     * Zeichnet das Hilfe-Overlay für den Item Viewer
     */
    public static void drawHelpScreen(GuiGraphicsExtractor context, Minecraft client) {
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        // Hintergrund-Overlay
        context.fill(0, 0, screenWidth, screenHeight, 0x80000000);

        HelpBox box = computeBox(client);

        // Prüfe ob Maus über dem Hilfe-Overlay ist (für Scrolling)
        helpScreenHovered = isInside(lastMouseX, lastMouseY, box.x(), box.y(), box.width(), box.height());

        drawBoxBackground(context, box);
        drawCloseButton(context, client, box);

        // Titel
        String title = "Item Viewer Hilfe";
        int titleWidth = client.font.width(title);
        context.text(client.font, title, box.x() + (box.width() - titleWidth) / 2, box.y() + 10, 0xFFFFFF00, true);

        // Hilfe-Text mit Scrolling, oberhalb der Kategorie-Buttons
        int textStartY = box.y() + 35;
        int lineHeight = 11;
        int buttonY = computeButtonY(box);
        int minButtonYForText = textStartY + 15; // Mindestens 15px nach dem letzten Text
        if (buttonY < minButtonYForText) {
            buttonY = minButtonYForText;
        }
        int availableTextHeight = buttonY - textStartY - 15;

        List<String> allTextLines = wrapLines(client, HELP_TEXT, box.width() - 30);
        int totalTextHeight = allTextLines.size() * lineHeight;

        // Begrenze Scroll-Offset
        int maxScrollOffset = Math.max(0, totalTextHeight - availableTextHeight);
        helpScreenScrollOffset = Math.min(helpScreenScrollOffset, maxScrollOffset);

        int startLineIndex = helpScreenScrollOffset / lineHeight;
        int visibleLines = (int) Math.ceil((double) availableTextHeight / lineHeight) + 1;

        drawVisibleTextLines(context, client, box, allTextLines, textStartY, lineHeight, buttonY, startLineIndex, visibleLines);

        if (helpScreenScrollOffset > 0) {
            drawCenteredHint(context, client, box, String.format("↑ %d weitere (Scrollen)", startLineIndex), textStartY - 2);
        }

        drawCategorySection(context, client, box, buttonY, lineHeight, maxScrollOffset,
                startLineIndex, visibleLines, allTextLines.size());
    }

    /** Box-Hintergrund mit weißem 1px-Rahmen. */
    private static void drawBoxBackground(GuiGraphicsExtractor context, HelpBox box) {
        int boxX = box.x();
        int boxY = box.y();
        int boxWidth = box.width();
        int boxHeight = box.height();
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        context.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFFFFFFFF);
        context.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFFFFFFFF);
        context.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFFFFFFFF);
        context.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFFFFFFFF);
    }

    /** Schließen-Button (X oben rechts) mit Hover-Farbe, Rahmen und zentriertem "×". */
    private static void drawCloseButton(GuiGraphicsExtractor context, Minecraft client, HelpBox box) {
        int closeButtonX = box.closeButtonX();
        int closeButtonY = box.closeButtonY();
        int closeButtonSize = CLOSE_BUTTON_SIZE;
        boolean closeHovered = isInside(lastMouseX, lastMouseY, closeButtonX, closeButtonY, closeButtonSize, closeButtonSize);

        context.fill(closeButtonX, closeButtonY, closeButtonX + closeButtonSize, closeButtonY + closeButtonSize,
                closeHovered ? 0xFF404040 : 0x80000000);
        context.fill(closeButtonX, closeButtonY, closeButtonX + closeButtonSize, closeButtonY + 1, 0xFFFFFFFF);
        context.fill(closeButtonX, closeButtonY + closeButtonSize - 1, closeButtonX + closeButtonSize, closeButtonY + closeButtonSize, 0xFFFFFFFF);
        context.fill(closeButtonX, closeButtonY, closeButtonX + 1, closeButtonY + closeButtonSize, 0xFFFFFFFF);
        context.fill(closeButtonX + closeButtonSize - 1, closeButtonY, closeButtonX + closeButtonSize, closeButtonY + closeButtonSize, 0xFFFFFFFF);

        // +1 für bessere visuelle Zentrierung
        String closeText = "×";
        int closeTextX = closeButtonX + (closeButtonSize - client.font.width(closeText)) / 2 + 1;
        int closeTextY = closeButtonY + (closeButtonSize - client.font.lineHeight) / 2 + 1;
        context.text(client.font, closeText, closeTextX, closeTextY, 0xFFFFFFFF, true);
    }

    /** Bricht zu lange Zeilen an Wortgrenzen um; leere Zeilen und kurze Zeilen bleiben unverändert. */
    private static List<String> wrapLines(Minecraft client, String[] lines, int maxTextWidth) {
        List<String> wrapped = new ArrayList<>();
        for (String line : lines) {
            if (client.font.width(line) <= maxTextWidth || line.isEmpty()) {
                wrapped.add(line);
                continue;
            }
            String currentLine = "";
            for (String word : line.split(" ")) {
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                if (client.font.width(testLine) > maxTextWidth) {
                    if (!currentLine.isEmpty()) {
                        wrapped.add(currentLine);
                        currentLine = word;
                    } else {
                        wrapped.add(word);
                    }
                } else {
                    currentLine = testLine;
                }
            }
            if (!currentLine.isEmpty()) {
                wrapped.add(currentLine);
            }
        }
        return wrapped;
    }

    /** Zeichnet die aktuell sichtbaren Textzeilen entsprechend dem Scroll-Offset. */
    private static void drawVisibleTextLines(GuiGraphicsExtractor context, Minecraft client, HelpBox box,
            List<String> allTextLines, int textStartY, int lineHeight, int buttonY, int startLineIndex, int visibleLines) {
        int textX = box.x() + 15;
        int textY = textStartY - (helpScreenScrollOffset % lineHeight);
        for (int i = startLineIndex; i < allTextLines.size() && i < startLineIndex + visibleLines; i++) {
            if (textY >= textStartY - lineHeight && textY < buttonY) {
                context.text(client.font, allTextLines.get(i), textX, textY, 0xFFFFFFFF, true);
            }
            textY += lineHeight;
        }
    }

    private static void drawCenteredHint(GuiGraphicsExtractor context, Minecraft client, HelpBox box, String text, int y) {
        int textWidth = client.font.width(text);
        context.text(client.font, text, box.x() + (box.width() - textWidth) / 2, y, 0xFFFFFF00, true);
    }

    /**
     * Kategorie-Buttons ganz unten. Bei genug Platz (z.B. GUI Scale 3) immer sichtbar; bei wenig Platz
     * (z.B. GUI Scale 4) erst nach dem Scrollen, davor erscheint ein Hinweis.
     */
    private static void drawCategorySection(GuiGraphicsExtractor context, Minecraft client, HelpBox box, int buttonY,
            int lineHeight, int maxScrollOffset, int startLineIndex, int visibleLines, int totalLineCount) {
        boolean hasEnoughSpace = (maxScrollOffset == 0);
        if (hasEnoughSpace) {
            renderCategoryButtons(context, client.font, box.x(), box.y(), box.width(), box.height(), buttonY);
            return;
        }

        if (helpScreenScrollOffset > 0) {
            // "↓ X weitere (Scrollen)" nur wenn noch mehr Text vorhanden ist
            if (helpScreenScrollOffset < maxScrollOffset) {
                int remainingLines = totalLineCount - (startLineIndex + visibleLines);
                if (remainingLines > 0) {
                    drawCenteredHint(context, client, box,
                            String.format("↓ %d weitere (Scrollen)", remainingLines), buttonY - lineHeight - 2);
                }
            }
            renderCategoryButtons(context, client.font, box.x(), box.y(), box.width(), box.height(), buttonY);
        } else {
            // Hinweis zum Scrollen in der untersten Zeile (15px tiefer)
            drawCenteredHint(context, client, box, "↓ Scrollen für Kategorien mit Tags", buttonY - lineHeight - 2 + 15);
        }
    }
}
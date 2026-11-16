package net.felix.features.itemviewer.ui;

import java.util.Arrays;
import net.felix.features.itemviewer.data.ItemData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static net.felix.features.itemviewer.data.ItemCatalog.allItems;
import static net.felix.features.itemviewer.ui.ViewerState.activeCategoryOverlay;

/**
 * Kategorie-Buttons mit zugehoerigen Such-Tags.
 */
public final class CategoryButtons {

    private CategoryButtons() {
    }

    // Kategorie-Buttons im Hilfe-Overlay
    static final java.util.List<CategoryButton> CATEGORY_BUTTON_DEFS = Arrays.asList(
            new CategoryButton("Baupläne", java.util.List.of("bauplan")),
            new CategoryButton("Angel-Komponenten", java.util.List.of("komponente")),
            new CategoryButton("Fisch-Reusen", java.util.List.of("fischreuse")),
            new CategoryButton("Module", java.util.List.of("modul")),
            new CategoryButton("Modul-Taschen", java.util.List.of("modultasche")),
            new CategoryButton("Machtkristall", java.util.List.of("machtkristall")),
            new CategoryButton("MK-Slots", java.util.List.of("slot")),
            new CategoryButton("Fähigkeiten", java.util.List.of("ability")),
            new CategoryButton("Karten Slots", java.util.List.of("card")),
            new CategoryButton("Lizenzen", java.util.List.of("lizenz"))
    );

    /**
     * Rendert die Kategorie-Buttons und ein optionales Tag-Overlay
     */
    static void renderCategoryButtons(GuiGraphicsExtractor context, net.minecraft.client.gui.Font tr,
                                              int boxX, int boxY, int boxWidth, int boxHeight, int buttonY) {
        // Berechne Position: Buttons stehen bei buttonY (bereits berechnet)
        int headerHeight = tr.lineHeight;
        int spacing = 4; // Kleiner Abstand zwischen Header und Buttons (Text direkt über Buttons)
        int headerY = buttonY - headerHeight - spacing; // Header steht direkt über den Buttons
        int startX = boxX + 15;

        context.text(tr, Component.literal("Kategorien mit Tags"), startX, headerY, 0xFFFFFF00, true);

        java.util.List<CategoryButton> buttons = computeCategoryButtons(tr, boxX, boxY, boxWidth, boxHeight, buttonY, startX);

        for (CategoryButton btn : buttons) {
            boolean isActive = activeCategoryOverlay != null && activeCategoryOverlay.name.equals(btn.name);
            int bg = isActive ? 0x8040FF40 : 0x80202020;
            int border = isActive ? 0x8000FF00 : 0xFF808080;
            context.fill(btn.x, btn.y, btn.x + btn.width, btn.y + btn.height, bg);
            context.fill(btn.x, btn.y, btn.x + btn.width, btn.y + 1, border);
            context.fill(btn.x, btn.y + btn.height - 1, btn.x + btn.width, btn.y + btn.height, border);
            context.fill(btn.x, btn.y, btn.x + 1, btn.y + btn.height, border);
            context.fill(btn.x + btn.width - 1, btn.y, btn.x + btn.width, btn.y + btn.height, border);

            int textW = tr.width(btn.name);
            int textX = btn.x + (btn.width - textW) / 2;
            int textY = btn.y + (btn.height - tr.lineHeight) / 2;
            context.text(tr, Component.literal(btn.name), textX, textY, 0xFFFFFFFF, false);
        }

        // Overlay mit Tags
        if (activeCategoryOverlay != null && activeCategoryOverlay.tags != null && !activeCategoryOverlay.tags.isEmpty()) {
            int lineHeight = tr.lineHeight + 2;
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
                    columnWidth = Math.max(columnWidth, tr.width(activeCategoryOverlay.tags.get(i)));
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

            // Hintergrund
            context.fill(ox, oy, ox + overlayWidth, oy + overlayHeight, 0xCC000000);
            context.fill(ox, oy, ox + overlayWidth, oy + 1, 0xFFFFFFFF);
            context.fill(ox, oy + overlayHeight - 1, ox + overlayWidth, oy + overlayHeight, 0xFFFFFFFF);
            context.fill(ox, oy, ox + 1, oy + overlayHeight, 0xFFFFFFFF);
            context.fill(ox + overlayWidth - 1, oy, ox + overlayWidth, oy + overlayHeight, 0xFFFFFFFF);
            
            // Trennlinien zwischen Spalten
            int currentX = ox;
            for (int col = 0; col < numColumns - 1; col++) {
                currentX += columnWidths.get(col);
                context.fill(currentX + columnSpacing / 2, oy, currentX + columnSpacing / 2 + 1, oy + overlayHeight, 0xFFFFFFFF);
                currentX += columnSpacing;
            }

            // Rendere Tags in allen Spalten
            for (int col = 0; col < numColumns; col++) {
                int columnX = ox;
                for (int i = 0; i < col; i++) {
                    columnX += columnWidths.get(i) + columnSpacing;
                }
                
                int ty = oy + 3;
                int startIdx = col * maxTagsPerColumn;
                int endIdx = Math.min(startIdx + maxTagsPerColumn, numTags);
                for (int i = startIdx; i < endIdx; i++) {
                    context.text(tr, Component.literal(activeCategoryOverlay.tags.get(i)), columnX + 6, ty, 0xFF55FFFF, false);
                    ty += lineHeight;
                }
            }
        }
    }

    /**
     * Berechnet die Positionen der Kategorie-Buttons
     */
    static java.util.List<CategoryButton> computeCategoryButtons(net.minecraft.client.gui.Font tr,
                                                                         int boxX, int boxY, int boxWidth, int boxHeight,
                                                                         int buttonY, int startX) {
        java.util.List<CategoryButton> buttons = new java.util.ArrayList<>();
        int spacing = 6;
        int buttonSpacingY = 20; // Abstand zwischen Button-Zeilen
        int maxButtonsPerRow = 4; // Maximal 4 Buttons pro Zeile
        int cursorX = startX;
        int currentY = buttonY;
        int buttonsInCurrentRow = 0;

        for (CategoryButton def : CATEGORY_BUTTON_DEFS) {
            // Hole alle Tags für diese Kategorie dynamisch
            java.util.List<String> allTags = getAllTagsForCategory(def.name);
            
            int btnW = tr.width(def.name) + 12;
            int btnH = 16;
            CategoryButton btn = new CategoryButton(def.name, allTags);
            
            // Wenn bereits 4 Buttons in dieser Zeile, starte neue Zeile
            if (buttonsInCurrentRow >= maxButtonsPerRow) {
                cursorX = startX;
                currentY += buttonSpacingY;
                buttonsInCurrentRow = 0;
            }
            
            btn.x = cursorX;
            btn.y = currentY;
            btn.width = btnW;
            btn.height = btnH;
            buttons.add(btn);
            cursorX += btnW + spacing;
            buttonsInCurrentRow++;
        }
        return buttons;
    }

    /**
     * Sammelt alle Tags für eine Kategorie aus den Items
     */
    private static java.util.List<String> getAllTagsForCategory(String categoryName) {
        java.util.Set<String> tagSet = new java.util.HashSet<>();
        
        // Mappe Kategorie-Name zu category-Feld
        String categoryKey = null;
        switch (categoryName) {
            case "Baupläne":
                categoryKey = "blueprints";
                break;
            case "Angel-Komponenten":
                categoryKey = "fishing_components";
                break;
            case "Fisch-Reusen":
                categoryKey = "fish_traps";
                break;
            case "Module":
                categoryKey = "modules";
                break;
            case "Modul-Taschen":
                categoryKey = "module_bags";
                break;
            case "Machtkristall":
                categoryKey = "power_crystals";
                break;
            case "MK-Slots":
                categoryKey = "power_crystal_slots";
                break;
            case "Fähigkeiten":
                categoryKey = "abilities";
                break;
            case "Karten Slots":
                categoryKey = "card_slots";
                break;
            case "Lizenzen":
                categoryKey = "licence";
                break;
        }
        
        if (categoryKey != null) {
            // Sammle alle Tags aus Items dieser Kategorie
            for (ItemData item : allItems) {
                if (item.category != null && item.category.equals(categoryKey)) {
                    if (item.tags != null) {
                        for (String tag : item.tags) {
                            if (tag != null && !tag.isEmpty()) {
                                tagSet.add(tag);
                            }
                        }
                    }
                    // Sammle auch Tags aus info.type, info.piece, info.rarity
                    if (item.info != null) {
                        if (item.info.type != null && !item.info.type.isEmpty()) {
                            tagSet.add(item.info.type.toLowerCase());
                        }
                        if (item.info.piece != null && !item.info.piece.isEmpty()) {
                            tagSet.add(item.info.piece.toLowerCase());
                        }
                        if (item.info.rarity != null && !item.info.rarity.isEmpty()) {
                            tagSet.add(item.info.rarity.toLowerCase());
                        }
                    }
                }
            }
        } else {
            // Debug: Kategorie nicht gefunden
            System.out.println("[ItemViewer] Kategorie '" + categoryName + "' nicht in switch-case gefunden!");
        }
        
        // Sortiere Tags (Karten-Slots: #1, #2, … numerisch)
        java.util.List<String> sortedTags = new java.util.ArrayList<>(tagSet);
        if ("card_slots".equals(categoryKey)) {
            sortedTags.sort((a, b) -> {
                Integer numA = parseHashTagNumber(a);
                Integer numB = parseHashTagNumber(b);
                if (numA != null && numB != null) {
                    int cmp = Integer.compare(numA, numB);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                return String.CASE_INSENSITIVE_ORDER.compare(a, b);
            });
        } else {
            sortedTags.sort(String.CASE_INSENSITIVE_ORDER);
        }
        return sortedTags;
    }

    private static Integer parseHashTagNumber(String tag) {
        if (tag == null || !tag.startsWith("#") || tag.length() < 2) {
            return null;
        }
        try {
            return Integer.parseInt(tag.substring(1).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

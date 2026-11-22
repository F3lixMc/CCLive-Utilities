package net.felix.utilities.Overall.itemviewer.ui;

import net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenu;
import net.felix.utilities.Overall.itemviewer.search.SortMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import static net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline.currentSortMode;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.FILTER_BUTTON_SIZE;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.HELP_BUTTON_SIZE;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.MINIMIZE_BUTTON_SIZE;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.SORT_DROPDOWN_HEIGHT;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.SORT_DROPDOWN_PADDING;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.SORT_OPTION_HEIGHT;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.SYMBOL_BUTTON_SIZE;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getViewerSettingsCostsButtonX;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getViewerSettingsMenuWidth;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getViewerSettingsOwnedButtonX;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.favoritesMode;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.isSymbolButtonHovered;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.isSymbolMenuOpen;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.isViewerSettingsButtonHovered;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.isViewerSettingsMenuOpen;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.lastMouseX;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.lastMouseY;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.showCosts;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.showOwnedResources;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.sortDropdownOpen;

/**
 * Zeichnet die Buttons des Item Viewers: Hilfe, Filter, Einstellungen, Favoriten, Symbole und Sortierung.
 */
public final class ViewerButtonsRenderer {

    private ViewerButtonsRenderer() {
    }

    private static final Identifier HELP_ICON_TEXTURE =
        Identifier.fromNamespaceAndPath("cclive-utilities", "textures/icons/hint.png");

    private static final Identifier FILTER_ICON_TEXTURE = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/icons/lupe_icon.png");

    private static final Identifier STAR_ICON_TEXTURE = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/icons/star_icon.png");

    public static final Identifier APPLE_ICON_TEXTURE = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/icons/apple_icon_2.png");

    private static final Identifier VIEWER_SETTINGS_ICON = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/alert_icons/alert_icons_settings.png");

    static final int VIEWER_SETTINGS_MENU_BUTTON_COUNT = 2;

    /**
     * Rendert den Hilfe-Button
     */
    static void renderHelpButton(GuiGraphicsExtractor context, int x, int y) {
        Minecraft client = Minecraft.getInstance();
        
        // Prüfe ob Maus über Button ist
        boolean isHovered = lastMouseX >= x && lastMouseX < x + HELP_BUTTON_SIZE &&
                           lastMouseY >= y && lastMouseY < y + HELP_BUTTON_SIZE;
        
        // Button-Hintergrund
        int bgColor = isHovered ? 0xFF404040 : 0x80000000;
        context.fill(x, y, x + HELP_BUTTON_SIZE, y + HELP_BUTTON_SIZE, bgColor);
        
        // Button-Rahmen
        int borderColor = isHovered ? 0xFFFFFFFF : 0xFF808080;
        context.fill(x, y, x + HELP_BUTTON_SIZE, y + 1, borderColor); // Oben
        context.fill(x, y + HELP_BUTTON_SIZE - 1, x + HELP_BUTTON_SIZE, y + HELP_BUTTON_SIZE, borderColor); // Unten
        context.fill(x, y, x + 1, y + HELP_BUTTON_SIZE, borderColor); // Links
        context.fill(x + HELP_BUTTON_SIZE - 1, y, x + HELP_BUTTON_SIZE, y + HELP_BUTTON_SIZE, borderColor); // Rechts
        
        try {
            int iconSize = 16;
            int iconX = x + (HELP_BUTTON_SIZE - iconSize) / 2;
            int iconY = y + (HELP_BUTTON_SIZE - iconSize) / 2 + 1;
            context.blit(
                    RenderPipelines.GUI_TEXTURED,
                    HELP_ICON_TEXTURE,
                    iconX, iconY,
                    0.0f, 0.0f,
                    iconSize, iconSize,
                    iconSize, iconSize
            );
        } catch (Exception ignored) {
            String questionMark = "?";
            int textWidth = client.font.width(questionMark);
            int textX = x + (HELP_BUTTON_SIZE - textWidth) / 2;
            int textY = y + (HELP_BUTTON_SIZE - 9) / 2;
            context.text(client.font, Component.literal(questionMark), textX, textY, 0xFFFFFFFF, false);
        }
    }

    static void renderFilterButton(GuiGraphicsExtractor context, int x, int y) {
        Minecraft client = Minecraft.getInstance();
        boolean isHovered = lastMouseX >= x && lastMouseX < x + FILTER_BUTTON_SIZE
                && lastMouseY >= y && lastMouseY < y + FILTER_BUTTON_SIZE;
        boolean active = FilterMenu.isOpen();
        int bgColor = active ? 0xFF2D4A2D : (isHovered ? 0xFF404040 : 0x80000000);
        context.fill(x, y, x + FILTER_BUTTON_SIZE, y + FILTER_BUTTON_SIZE, bgColor);
        int borderColor = active || isHovered ? 0xFFFFFFFF : 0xFF808080;
        context.fill(x, y, x + FILTER_BUTTON_SIZE, y + 1, borderColor);
        context.fill(x, y + FILTER_BUTTON_SIZE - 1, x + FILTER_BUTTON_SIZE, y + FILTER_BUTTON_SIZE, borderColor);
        context.fill(x, y, x + 1, y + FILTER_BUTTON_SIZE, borderColor);
        context.fill(x + FILTER_BUTTON_SIZE - 1, y, x + FILTER_BUTTON_SIZE, y + FILTER_BUTTON_SIZE, borderColor);
        try {
            int iconSize = 16;
            int iconX = x + (FILTER_BUTTON_SIZE - iconSize) / 2;
            int iconY = y + (FILTER_BUTTON_SIZE - iconSize) / 2;
            context.blit(
                    RenderPipelines.GUI_TEXTURED,
                    FILTER_ICON_TEXTURE,
                    iconX, iconY,
                    0.0f, 0.0f,
                    iconSize, iconSize,
                    iconSize, iconSize
            );
        } catch (Exception ignored) {
            String label = "F";
            int textWidth = client.font.width(label);
            context.text(client.font, Component.literal(label),
                    x + (FILTER_BUTTON_SIZE - textWidth) / 2, y + (FILTER_BUTTON_SIZE - 9) / 2, 0xFFFFFFFF, false);
        }
    }

    static void renderViewerSettingsButton(GuiGraphicsExtractor context, int x, int y) {

        isViewerSettingsButtonHovered = lastMouseX >= x && lastMouseX < x + MINIMIZE_BUTTON_SIZE
                && lastMouseY >= y && lastMouseY < y + MINIMIZE_BUTTON_SIZE;

        int bgColor = (isViewerSettingsButtonHovered || isViewerSettingsMenuOpen) ? 0x80FFFF00 : 0x80000000;
        context.fill(x, y, x + MINIMIZE_BUTTON_SIZE, y + MINIMIZE_BUTTON_SIZE, bgColor);

        int borderColor = (isViewerSettingsButtonHovered || isViewerSettingsMenuOpen) ? 0xFFFFFF00 : 0xFF808080;
        context.fill(x, y, x + MINIMIZE_BUTTON_SIZE, y + 1, borderColor);
        context.fill(x, y + MINIMIZE_BUTTON_SIZE - 1, x + MINIMIZE_BUTTON_SIZE, y + MINIMIZE_BUTTON_SIZE, borderColor);
        context.fill(x, y, x + 1, y + MINIMIZE_BUTTON_SIZE, borderColor);
        context.fill(x + MINIMIZE_BUTTON_SIZE - 1, y, x + MINIMIZE_BUTTON_SIZE, y + MINIMIZE_BUTTON_SIZE, borderColor);

        drawViewerSettingsGearIcon(context, x, y, MINIMIZE_BUTTON_SIZE);
    }

    private static void drawViewerSettingsGearIcon(GuiGraphicsExtractor context, int x, int y, int buttonSize) {
        try {
            int iconSize = 16;
            int iconX = x + (buttonSize - iconSize) / 2;
            int iconY = y + (buttonSize - iconSize) / 2;
            context.blit(
                    RenderPipelines.GUI_TEXTURED,
                    VIEWER_SETTINGS_ICON,
                    iconX, iconY,
                    0.0f, 0.0f,
                    iconSize, iconSize,
                    iconSize, iconSize
            );
        } catch (Exception ignored) {
            Minecraft client = Minecraft.getInstance();
            context.text(client.font, Component.literal("⚙"),
                    x + (buttonSize - client.font.width("⚙")) / 2,
                    y + (buttonSize - client.font.lineHeight) / 2 + 1,
                    0xFFFFFF00, false);
        }
    }

    static void checkViewerSettingsButtonHover(int buttonX, int buttonY) {
        boolean wasHovered = isViewerSettingsButtonHovered;
        isViewerSettingsButtonHovered = lastMouseX >= buttonX && lastMouseX <= buttonX + MINIMIZE_BUTTON_SIZE
                && lastMouseY >= buttonY && lastMouseY <= buttonY + MINIMIZE_BUTTON_SIZE;

        int menuWidth = getViewerSettingsMenuWidth();
        int menuX = buttonX - menuWidth;
        int menuHeight = MINIMIZE_BUTTON_SIZE;

        boolean mouseOverMenu = false;
        if (isViewerSettingsMenuOpen) {
            mouseOverMenu = lastMouseX >= menuX && lastMouseX <= buttonX + MINIMIZE_BUTTON_SIZE
                    && lastMouseY >= buttonY && lastMouseY <= buttonY + menuHeight;
        }

        boolean mouseOverButtonOrMenu = isViewerSettingsButtonHovered || mouseOverMenu;

        if (isViewerSettingsButtonHovered && !wasHovered && !isViewerSettingsMenuOpen) {
            isViewerSettingsMenuOpen = true;
        } else if (!mouseOverButtonOrMenu && isViewerSettingsMenuOpen) {
            isViewerSettingsMenuOpen = false;
        }
    }

    static void renderViewerSettingsMenu(GuiGraphicsExtractor context, int buttonX, int buttonY) {
        Minecraft client = Minecraft.getInstance();

        int menuButtonWidth = MINIMIZE_BUTTON_SIZE;
        int menuButtonHeight = MINIMIZE_BUTTON_SIZE;
        int menuWidth = getViewerSettingsMenuWidth();
        int menuHeight = menuButtonHeight;
        int menuX = buttonX - menuWidth;

        boolean mouseOverMenu = lastMouseX >= menuX && lastMouseX <= buttonX + MINIMIZE_BUTTON_SIZE
                && lastMouseY >= buttonY && lastMouseY <= buttonY + menuHeight;
        boolean mouseOverButtonOrMenu = isViewerSettingsButtonHovered || mouseOverMenu;
        if (!mouseOverButtonOrMenu && isViewerSettingsMenuOpen) {
            isViewerSettingsMenuOpen = false;
            return;
        }

        context.fill(menuX, buttonY, menuX + menuWidth, buttonY + menuHeight, 0xFF000000);
        context.fill(menuX, buttonY, menuX + menuWidth, buttonY + 1, 0xFFFFFFFF);
        context.fill(menuX, buttonY + menuHeight - 1, menuX + menuWidth, buttonY + menuHeight, 0xFFFFFFFF);
        context.fill(menuX, buttonY, menuX + 1, buttonY + menuHeight, 0xFFFFFFFF);
        context.fill(menuX + menuWidth - 1, buttonY, menuX + menuWidth, buttonY + menuHeight, 0xFFFFFFFF);

        int costsButtonX = getViewerSettingsCostsButtonX(menuX);
        renderViewerSettingsMenuToggleButton(context, client, costsButtonX, buttonY, menuButtonWidth, menuButtonHeight, "$", showCosts);
        context.fill(menuX + MINIMIZE_BUTTON_SIZE, buttonY, menuX + MINIMIZE_BUTTON_SIZE + 1, buttonY + menuHeight, 0xFFFFFFFF);
        int ownedButtonX = getViewerSettingsOwnedButtonX(menuX);
        renderViewerSettingsMenuToggleButton(context, client, ownedButtonX, buttonY, menuButtonWidth, menuButtonHeight, "x/x", showOwnedResources);
    }

    private static void renderViewerSettingsMenuToggleButton(
            GuiGraphicsExtractor context, Minecraft client, int x, int y, int width, int height, String label, boolean active) {
        boolean hovered = lastMouseX >= x && lastMouseX < x + width && lastMouseY >= y && lastMouseY < y + height;
        int bgColor = active ? 0xFF305030 : (hovered ? 0x80404040 : 0x80202020);
        context.fill(x, y + 1, x + width, y + height - 1, bgColor);
        int textColor = active ? 0xFF55FF55 : 0xFFFFFFFF;
        if ("$".equals(label)) {
            float scale = 1.5f;
            Matrix3x2fStack matrices = context.pose();
            matrices.pushMatrix();
            matrices.translate(x + width / 2.0f, y + height / 2.0f);
            matrices.scale(scale, scale);
            int textWidth = client.font.width(label);
            context.text(
                    client.font,
                    Component.literal(label),
                    -textWidth / 2,
                    -client.font.lineHeight / 2,
                    textColor,
                    false
            );
            matrices.popMatrix();
        } else {
            int textWidth = client.font.width(label);
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height - client.font.lineHeight) / 2;
            context.text(client.font, Component.literal(label), textX, textY, textColor, false);
        }
    }

    static void renderMinimizeButton(GuiGraphicsExtractor context, int x, int y) {
        renderSideButton(context, x, y, "▶", false);
    }

    private static void renderSideButton(GuiGraphicsExtractor context, int x, int y, String label, boolean active) {
        Minecraft client = Minecraft.getInstance();

        boolean isHovered = lastMouseX >= x && lastMouseX < x + MINIMIZE_BUTTON_SIZE
                && lastMouseY >= y && lastMouseY < y + MINIMIZE_BUTTON_SIZE;

        int bgColor = active ? 0xFF305030 : (isHovered ? 0xFF404040 : 0x80000000);
        context.fill(x, y, x + MINIMIZE_BUTTON_SIZE, y + MINIMIZE_BUTTON_SIZE, bgColor);

        int borderColor = active ? 0xFF55FF55 : (isHovered ? 0xFFFFFFFF : 0xFF808080);
        context.fill(x, y, x + MINIMIZE_BUTTON_SIZE, y + 1, borderColor);
        context.fill(x, y + MINIMIZE_BUTTON_SIZE - 1, x + MINIMIZE_BUTTON_SIZE, y + MINIMIZE_BUTTON_SIZE, borderColor);
        context.fill(x, y, x + 1, y + MINIMIZE_BUTTON_SIZE, borderColor);
        context.fill(x + MINIMIZE_BUTTON_SIZE - 1, y, x + MINIMIZE_BUTTON_SIZE, y + MINIMIZE_BUTTON_SIZE, borderColor);

        int textWidth = client.font.width(label);
        int textX = x + (MINIMIZE_BUTTON_SIZE - textWidth) / 2;
        int textY = y + (MINIMIZE_BUTTON_SIZE - client.font.lineHeight) / 2 + 1;
        context.text(client.font, Component.literal(label), textX, textY, 0xFFFFFFFF, false);
    }

    /**
     * Rendert den Favoriten-Button
     */
    static void renderFavoritesButton(GuiGraphicsExtractor context, int x, int y) {
        Minecraft client = Minecraft.getInstance();
        
        // Prüfe ob Maus über Button ist
        boolean isHovered = lastMouseX >= x && lastMouseX < x + SORT_DROPDOWN_HEIGHT &&
                           lastMouseY >= y && lastMouseY < y + SORT_DROPDOWN_HEIGHT;
        
        // Button-Hintergrund (grün wenn aktiv, sonst normal, 50% transparenter wenn aktiv)
        int bgColor = favoritesMode ? (isHovered ? 0x8060FF60 : 0x8040FF40) : (isHovered ? 0xFF404040 : 0xFF202020);
        context.fill(x, y, x + SORT_DROPDOWN_HEIGHT, y + SORT_DROPDOWN_HEIGHT, bgColor);
        
        // Button-Rahmen (grün wenn aktiv, sonst normal, 50% transparenter wenn aktiv)
        int borderColor = favoritesMode ? 0x8000FF00 : (isHovered ? 0xFFFFFFFF : 0xFF808080);
        context.fill(x, y, x + SORT_DROPDOWN_HEIGHT, y + 1, borderColor); // Oben
        context.fill(x, y + SORT_DROPDOWN_HEIGHT - 1, x + SORT_DROPDOWN_HEIGHT, y + SORT_DROPDOWN_HEIGHT, borderColor); // Unten
        context.fill(x, y, x + 1, y + SORT_DROPDOWN_HEIGHT, borderColor); // Links
        context.fill(x + SORT_DROPDOWN_HEIGHT - 1, y, x + SORT_DROPDOWN_HEIGHT, y + SORT_DROPDOWN_HEIGHT, borderColor); // Rechts
        
        // Rendere Stern-Icon (Textur)
        try {
            // Versuche Textur zu rendern (16x16 Pixel, zentriert im 20x20 Button)
            int iconSize = 16;
            int iconX = x + (SORT_DROPDOWN_HEIGHT - iconSize) / 2;
            int iconY = y + (SORT_DROPDOWN_HEIGHT - iconSize) / 2;
            
            // Verwende drawTexture mit RenderPipeline
            context.blit(
                RenderPipelines.GUI_TEXTURED,
                STAR_ICON_TEXTURE,
                iconX, iconY,
                0.0f, 0.0f,
                iconSize, iconSize,
                iconSize, iconSize
            );
        } catch (Exception e) {
            // Fallback: Zeige "★" als Text wenn Textur nicht geladen werden kann
            String starSymbol = "★";
            int starTextWidth = client.font.width(starSymbol);
            int starTextHeight = client.font.lineHeight;
            int starX = x + (SORT_DROPDOWN_HEIGHT - starTextWidth) / 2;
            int starY = y + (SORT_DROPDOWN_HEIGHT - starTextHeight) / 2;
            int starColor = favoritesMode ? 0x80FFFF00 : 0xFFFFFF00; // Gelb, 50% transparenter wenn aktiv
            context.text(client.font, Component.literal(starSymbol), starX, starY, starColor, false);
        }
    }

    /**
     * Rendert nur den Sortierungs-Dropdown-Button (ohne Liste)
     */
    static void renderSortDropdownButton(GuiGraphicsExtractor context, int x, int y) {
        Minecraft client = Minecraft.getInstance();
        
        // Berechne Dropdown-Breite dynamisch
        int maxDropdownWidth = 0;
        for (SortMode mode : SortMode.values()) {
            int textWidth = client.font.width(mode.getDisplayName());
            maxDropdownWidth = Math.max(maxDropdownWidth, textWidth);
        }
        int dropdownWidth = maxDropdownWidth + SORT_DROPDOWN_PADDING * 2 + 15; // +15 für Pfeil
        
        // Prüfe ob Maus über Dropdown-Button ist
        boolean isHovered = lastMouseX >= x && lastMouseX < x + dropdownWidth &&
                           lastMouseY >= y && lastMouseY < y + SORT_DROPDOWN_HEIGHT;
        
        // Dropdown-Button
        int bgColor = isHovered ? 0xFF404040 : 0xFF202020;
        context.fill(x, y, x + dropdownWidth, y + SORT_DROPDOWN_HEIGHT, bgColor);
        
        // Button-Rahmen
        int borderColor = sortDropdownOpen ? 0xFFFFFFFF : 0xFF808080;
        context.fill(x, y, x + dropdownWidth, y + 1, borderColor); // Oben
        context.fill(x, y + SORT_DROPDOWN_HEIGHT - 1, x + dropdownWidth, y + SORT_DROPDOWN_HEIGHT, borderColor); // Unten
        context.fill(x, y, x + 1, y + SORT_DROPDOWN_HEIGHT, borderColor); // Links
        context.fill(x + dropdownWidth - 1, y, x + dropdownWidth, y + SORT_DROPDOWN_HEIGHT, borderColor); // Rechts
        
        // Button-Text (aktueller Sort-Modus)
        String buttonText = currentSortMode.getDisplayName();
        int textX = x + SORT_DROPDOWN_PADDING;
        int textColor = 0xFFFFFFFF;
        context.text(client.font, Component.literal(buttonText), textX, y + 6, textColor, false);
        
        // Dropdown-Pfeil (▼)
        String arrow = sortDropdownOpen ? "▲" : "▼";
        int arrowX = x + dropdownWidth - client.font.width(arrow) - SORT_DROPDOWN_PADDING;
        context.text(client.font, Component.literal(arrow), arrowX, y + 6, 0xFF808080, false);
    }

    /**
     * Rendert die Dropdown-Liste (wird NACH dem Item-Grid gerendert, damit sie darüber liegt)
     */
    static void renderSortDropdownList(GuiGraphicsExtractor context, int x, int y) {
        Minecraft client = Minecraft.getInstance();
        
        // Berechne Dropdown-Breite dynamisch
        int maxDropdownWidth = 0;
        for (SortMode mode : SortMode.values()) {
            int textWidth = client.font.width(mode.getDisplayName());
            maxDropdownWidth = Math.max(maxDropdownWidth, textWidth);
        }
        int dropdownWidth = maxDropdownWidth + SORT_DROPDOWN_PADDING * 2 + 15; // +15 für Pfeil
        
        // Position der Dropdown-Liste (unter dem Button)
        int dropdownListY = y + SORT_DROPDOWN_HEIGHT;
        int dropdownListHeight = SortMode.values().length * SORT_OPTION_HEIGHT;
        
        // Hintergrund der Dropdown-Liste
        context.fill(x, dropdownListY, x + dropdownWidth, dropdownListY + dropdownListHeight, 0xFF202020);
        context.fill(x, dropdownListY, x + dropdownWidth, dropdownListY + 1, 0xFF808080); // Oben
        context.fill(x, dropdownListY + dropdownListHeight - 1, x + dropdownWidth, dropdownListY + dropdownListHeight, 0xFF808080); // Unten
        context.fill(x, dropdownListY, x + 1, dropdownListY + dropdownListHeight, 0xFF808080); // Links
        context.fill(x + dropdownWidth - 1, dropdownListY, x + dropdownWidth, dropdownListY + dropdownListHeight, 0xFF808080); // Rechts
        
        // Rendere Optionen
        int optionIndex = 0;
        for (SortMode mode : SortMode.values()) {
            int optionY = dropdownListY + optionIndex * SORT_OPTION_HEIGHT;
            boolean isOptionHovered = lastMouseX >= x && lastMouseX < x + dropdownWidth &&
                                     lastMouseY >= optionY && lastMouseY < optionY + SORT_OPTION_HEIGHT;
            boolean isSelected = currentSortMode == mode;
            
            // Option-Hintergrund
            if (isOptionHovered || isSelected) {
                int optionBgColor = isSelected ? 0xFF404040 : 0xFF303030;
                context.fill(x, optionY, x + dropdownWidth, optionY + SORT_OPTION_HEIGHT, optionBgColor);
            }
            
            // Option-Text
            String optionText = mode.getDisplayName();
            int optionTextX = x + SORT_DROPDOWN_PADDING;
            int optionTextColor = isSelected ? 0xFFFFFFFF : 0xFFCCCCCC;
            context.text(client.font, Component.literal(optionText), optionTextX, optionY + 4, optionTextColor, false);
            
            optionIndex++;
        }
    }

    /**
     * Prüft ob die Maus über dem Symbol-Button ist und öffnet/schließt das Menü entsprechend
     */
    public static void checkSymbolButtonHover(int buttonX, int buttonY) {
        // Prüfe ob Maus über Symbol-Button ist
        boolean wasHovered = isSymbolButtonHovered;
        isSymbolButtonHovered = (lastMouseX >= buttonX && lastMouseX <= buttonX + SYMBOL_BUTTON_SIZE &&
                                 lastMouseY >= buttonY && lastMouseY <= buttonY + SYMBOL_BUTTON_SIZE);
        
        // Berechne Menü-Position (links vom Button)
        int menuButtonWidth = 20;
        int menuButtonHeight = SYMBOL_BUTTON_SIZE; // Gleiche Höhe wie Button (symmetrisch zum Dropdown)
        int menuWidth = menuButtonWidth * 3 + 2; // 3 Buttons (@, <, >) + 2px Abstand
        int menuHeight = menuButtonHeight;
        int menuX = buttonX - menuWidth; // Links vom Button
        int menuY = buttonY;
        
        // Prüfe ob Maus über dem Menü ist (nur wenn Menü bereits geöffnet ist)
        boolean mouseOverMenu = false;
        if (isSymbolMenuOpen) {
            mouseOverMenu = (lastMouseX >= menuX && lastMouseX <= buttonX + SYMBOL_BUTTON_SIZE &&
                             lastMouseY >= menuY && lastMouseY <= menuY + menuHeight);
        }
        
        // Prüfe ob Maus über Button oder Menü ist
        boolean mouseOverButtonOrMenu = isSymbolButtonHovered || mouseOverMenu;
        
        // Öffne Menü nur beim Hover über den Button (nicht über den geschlossenen Menü-Bereich)
        if (isSymbolButtonHovered && !wasHovered && !isSymbolMenuOpen) {
            // Öffne Menü beim Hover über Button
            isSymbolMenuOpen = true;
        } else if (!mouseOverButtonOrMenu && isSymbolMenuOpen) {
            // Maus ist weder über Button noch über Menü - schließe Menü
            isSymbolMenuOpen = false;
        }
    }

    /**
     * Rendert den Symbol-Button (Apple-Icon)
     */
    static void renderSymbolButton(GuiGraphicsExtractor context, int x, int y) {
        Minecraft client = Minecraft.getInstance();
        
        // Prüfe ob Maus über Button ist
        isSymbolButtonHovered = lastMouseX >= x && lastMouseX < x + SYMBOL_BUTTON_SIZE &&
                               lastMouseY >= y && lastMouseY < y + SYMBOL_BUTTON_SIZE;
        
        // Button-Hintergrund (gelb wenn gehovered oder Menü offen)
        int bgColor = (isSymbolButtonHovered || isSymbolMenuOpen) ? 0x80FFFF00 : 0x80000000;
        context.fill(x, y, x + SYMBOL_BUTTON_SIZE, y + SYMBOL_BUTTON_SIZE, bgColor);
        
        // Button-Rahmen (grau wie andere Buttons, gelb wenn gehovered oder Menü offen)
        int borderColor = (isSymbolButtonHovered || isSymbolMenuOpen) ? 0xFFFFFF00 : 0xFF808080;
        context.fill(x, y, x + SYMBOL_BUTTON_SIZE, y + 1, borderColor);
        context.fill(x, y + SYMBOL_BUTTON_SIZE - 1, x + SYMBOL_BUTTON_SIZE, y + SYMBOL_BUTTON_SIZE, borderColor);
        context.fill(x, y, x + 1, y + SYMBOL_BUTTON_SIZE, borderColor);
        context.fill(x + SYMBOL_BUTTON_SIZE - 1, y, x + SYMBOL_BUTTON_SIZE, y + SYMBOL_BUTTON_SIZE, borderColor);
        
        // Rendere Apple-Icon (Textur)
        try {
            // Versuche Textur zu rendern (16x16 Pixel, zentriert im 20x20 Button)
            int iconSize = 16;
            int iconX = x + (SYMBOL_BUTTON_SIZE - iconSize) / 2;
            int iconY = y + (SYMBOL_BUTTON_SIZE - iconSize) / 2;
            
            // Verwende drawTexture mit RenderPipeline
            context.blit(
                RenderPipelines.GUI_TEXTURED,
                APPLE_ICON_TEXTURE,
                iconX, iconY,
                0.0f, 0.0f,
                iconSize, iconSize,
                iconSize, iconSize
            );
        } catch (Exception e) {
            // Fallback: Zeige "!" als Text wenn Textur nicht geladen werden kann
            String exclamationSymbol = "!";
            int exclamationTextWidth = client.font.width(exclamationSymbol);
            int exclamationTextHeight = client.font.lineHeight;
            int exclamationX = x + (SYMBOL_BUTTON_SIZE - exclamationTextWidth) / 2;
            int exclamationY = y + (SYMBOL_BUTTON_SIZE - exclamationTextHeight) / 2;
            context.text(
                client.font,
                Component.literal(exclamationSymbol),
                exclamationX,
                exclamationY,
                0xFFFFFF00,
                false
            );
        }
    }

    /**
     * Rendert das Symbol-Menü (erscheint beim Hovern über den Stern-Button, nach links)
     */
    static void renderSymbolMenu(GuiGraphicsExtractor context, int buttonX, int buttonY) {
        Minecraft client = Minecraft.getInstance();
        
        int menuButtonWidth = 20;
        int menuButtonHeight = SYMBOL_BUTTON_SIZE; // Gleiche Höhe wie Stern-Button
        int menuWidth = menuButtonWidth * 3 + 2; // 3 Buttons + 2px Abstand
        int menuHeight = menuButtonHeight;
        
        // Menü-Position (links vom Button, da nach links aufgehen soll)
        int menuX = buttonX - menuWidth; // Links vom Button
        int menuY = buttonY; // Gleiche Y-Position wie Button
        
        // Prüfe ob Maus über dem Menü ist (inkl. Button-Bereich)
        boolean mouseOverMenu = (lastMouseX >= menuX && lastMouseX <= buttonX + SYMBOL_BUTTON_SIZE &&
                                 lastMouseY >= menuY && lastMouseY <= menuY + menuHeight);
        
        // Prüfe ob Maus über Button oder Menü ist
        boolean mouseOverButtonOrMenu = isSymbolButtonHovered || mouseOverMenu;
        
        // Öffne Menü beim ersten Hover, schließe es wenn Maus weg ist
        if (!mouseOverButtonOrMenu && isSymbolMenuOpen) {
            isSymbolMenuOpen = false;
            return;
        }
        
        // Menü-Hintergrund
        context.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xFF000000);
        
        // Menü-Rahmen
        context.fill(menuX, menuY, menuX + menuWidth, menuY + 1, 0xFFFFFFFF);
        context.fill(menuX, menuY + menuHeight - 1, menuX + menuWidth, menuY + menuHeight, 0xFFFFFFFF);
        context.fill(menuX, menuY, menuX + 1, menuY + menuHeight, 0xFFFFFFFF);
        context.fill(menuX + menuWidth - 1, menuY, menuX + menuWidth, menuY + menuHeight, 0xFFFFFFFF);
        
        // @-Button (links)
        int atButtonX = menuX + 1;
        boolean atHovered = lastMouseX >= atButtonX && lastMouseX < atButtonX + menuButtonWidth &&
                            lastMouseY >= menuY && lastMouseY < menuY + menuButtonHeight;
        context.fill(atButtonX, menuY + 1, atButtonX + menuButtonWidth, menuY + menuButtonHeight - 1, 
                     atHovered ? 0x80404040 : 0x80202020);
        // Zentriere Text vertikal
        int textY = menuY + (menuButtonHeight - client.font.lineHeight) / 2;
        context.text(
            client.font,
            Component.literal("@"),
            atButtonX + 7,
            textY,
            0xFFFFFFFF,
            false
        );
        
        // Trennlinie zwischen @ und <
        int divider1X = menuX + menuButtonWidth;
        context.fill(divider1X, menuY, divider1X + 1, menuY + menuHeight, 0xFFFFFFFF);
        
        // <-Button (mitte)
        int lessButtonX = menuX + menuButtonWidth + 1;
        boolean lessHovered = lastMouseX >= lessButtonX && lastMouseX < lessButtonX + menuButtonWidth &&
                              lastMouseY >= menuY && lastMouseY < menuY + menuButtonHeight;
        context.fill(lessButtonX, menuY + 1, lessButtonX + menuButtonWidth, menuY + menuButtonHeight - 1,
                     lessHovered ? 0x80404040 : 0x80202020);
        context.text(
            client.font,
            Component.literal("<"),
            lessButtonX + 7,
            textY,
            0xFFFFFFFF,
            false
        );
        
        // Trennlinie zwischen < und >
        int divider2X = menuX + menuButtonWidth * 2 + 1;
        context.fill(divider2X, menuY, divider2X + 1, menuY + menuHeight, 0xFFFFFFFF);
        
        // >-Button (rechts)
        int greaterButtonX = menuX + (menuButtonWidth + 1) * 2;
        boolean greaterHovered = lastMouseX >= greaterButtonX && lastMouseX < greaterButtonX + menuButtonWidth &&
                                  lastMouseY >= menuY && lastMouseY < menuY + menuButtonHeight;
        context.fill(greaterButtonX, menuY + 1, greaterButtonX + menuButtonWidth, menuY + menuButtonHeight - 1,
                     greaterHovered ? 0x80404040 : 0x80202020);
        context.text(
            client.font,
            Component.literal(">"),
            greaterButtonX + 7,
            textY,
            0xFFFFFFFF,
            false
        );
    }
}

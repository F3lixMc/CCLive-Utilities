package net.felix.utilities.ItemViewer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.felix.utilities.Aincraft.BPViewerUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility für den Item-Viewer (ähnlich NEI/JEI)
 */
public class ItemViewerUtility {
    
    private static boolean isInitialized = false;
    private static boolean isVisible = true; // Standard: sichtbar
    private static KeyBinding toggleKeyBinding;
    
    private static List<ItemData> allItems = new ArrayList<>();
    private static List<ItemData> filteredItems = new ArrayList<>();
    private static String currentSearch = "";
    private static SortMode currentSortMode = SortMode.DEFAULT;
    
    // Selektion für Suchfeld
    private static int selectionStart = -1; // Start der Text-Selektion (-1 = keine Selektion)
    private static int selectionEnd = -1;   // Ende der Text-Selektion
    
    // Pagination
    private static final int ITEMS_PER_PAGE = 48; // 6x8 Grid
    private static int currentPage = 0;
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String ITEMS_CONFIG_FILE = "assets/cclive-utilities/items.json";
    
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        try {
            // Lade Items-Datei
            loadItems();
            
            // Registriere Keybind
            registerKeybind();
            
            // Registriere Client Tick Events
            ClientTickEvents.END_CLIENT_TICK.register(ItemViewerUtility::onClientTick);
            
            // Registriere HUD-Rendering
            HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
                onHudRender(drawContext, tickDelta);
            });
            
            isInitialized = true;
        } catch (Exception e) {
            System.err.println("Fehler beim Initialisieren des Item-Viewers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void loadItems() {
        try {
            // Lade aus Mod-Ressourcen
            var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
                .orElseThrow(() -> new RuntimeException("Mod container not found"))
                .findPath(ITEMS_CONFIG_FILE)
                .orElseThrow(() -> new RuntimeException("Items config file not found"));
            
            try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
                try (var reader = new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)) {
                    Type type = new TypeToken<ItemListData>(){}.getType();
                    ItemListData data = gson.fromJson(reader, type);
                    if (data != null && data.items != null) {
                        allItems = data.items;
                        filteredItems = new ArrayList<>(allItems);
                        applySorting();
                        System.out.println("✅ [ItemViewer] " + allItems.size() + " Items geladen");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ [ItemViewer] Fehler beim Laden der Items-Datei: " + e.getMessage());
            // Erstelle leere Liste als Fallback
            allItems = new ArrayList<>();
            filteredItems = new ArrayList<>();
        }
    }
    
    private static void registerKeybind() {
        toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.cclive-utilities.itemviewer.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            "category.cclive-utilities"
        ));
    }
    
    private static void onClientTick(MinecraftClient client) {
        if (toggleKeyBinding != null && toggleKeyBinding.wasPressed()) {
            isVisible = !isVisible;
        }
    }
    
    /**
     * Wird vom ScreenMixin aufgerufen, um Keybinds auch in Screens zu behandeln
     * @param keyCode GLFW key code
     * @param scanCode GLFW scan code
     * @param modifiers Modifier keys
     * @return true wenn der Keybind behandelt wurde, false sonst
     */
    public static boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (toggleKeyBinding == null) {
            return false;
        }
        
        // Prüfe ob unser Keybind gedrückt wurde
        // Verwende matchesKey() um zu prüfen ob der Keybind passt
        if (toggleKeyBinding.matchesKey(keyCode, scanCode)) {
            // Wenn Suchfeld fokussiert ist, nicht togglen (nur Fokus entfernen)
            if (searchFieldFocused) {
                searchFieldFocused = false;
                return true;
            }
            // Toggle direkt, ohne wasPressed() zu prüfen (wird im ScreenMixin aufgerufen)
            isVisible = !isVisible;
            return true;
        }
        
        return false;
    }
    
    /**
     * Behandelt Tastatur-Input für das Suchfeld (basierend auf SearchBarUtility)
     * @param keyCode GLFW key code
     * @param scanCode GLFW scan code
     * @param modifiers Modifier keys
     * @return true wenn der Input behandelt wurde, false sonst
     */
    public static boolean handleSearchFieldKeyPress(int keyCode, int scanCode, int modifiers) {
        // DEBUG: Logge alle Keypresses im Textfeld
        System.out.println("[ItemViewer] KeyPress erkannt: keyCode=" + keyCode + ", scanCode=" + scanCode + ", modifiers=" + modifiers);
        
        if (!isVisible) {
            searchFieldFocused = false;
            return false;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null) {
            searchFieldFocused = false;
            return false;
        }
        
        // Prüfe ob Maus über Suchfeld ist
        ViewerPosition pos;
        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            pos = calculateViewerPosition(handledScreen, client.currentScreen);
        } else {
            // Für nicht-HandledScreen Screens (z.B. Spielerinventar)
            pos = calculateViewerPositionForScreen(client.currentScreen);
        }
        int searchX = pos.helpButtonX + HELP_BUTTON_SIZE + 5;
        int searchY = pos.viewerY + VIEWER_PADDING;
        
        boolean mouseOverSearch = lastMouseX >= searchX && lastMouseX < searchX + pos.searchWidth &&
                                 lastMouseY >= searchY && lastMouseY < searchY + SEARCH_HEIGHT;
        
        if (!mouseOverSearch && !searchFieldFocused) {
            return false;
        }
        
        // Setze Fokus wenn Maus über Suchfeld ist
        if (mouseOverSearch) {
            searchFieldFocused = true;
        }
        
        if (!searchFieldFocused) {
            return false;
        }
        
        // ESC-Taste
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (helpScreenOpen) {
                helpScreenOpen = false;
                return true;
            }
            searchFieldFocused = false;
            return true;
        }
        
        // Enter-Taste
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            return true;
        }
        
        // Strg-Funktionen
        if (modifiers == 2) { // 2 = Strg
            switch (keyCode) {
                case 67: // Strg+C - Kopieren
                    if (!currentSearch.isEmpty()) {
                        client.keyboard.setClipboard(currentSearch);
                    }
                    return true;
                case 86: // Strg+V - Einfügen
                    String clipboardText = client.keyboard.getClipboard();
                    if (clipboardText != null && !clipboardText.isEmpty()) {
                        currentSearch += clipboardText;
                        applyFilters();
                        currentPage = 0;
                    }
                    return true;
                case 65: // Strg+A - Alles markieren
                    if (!currentSearch.isEmpty()) {
                        selectionStart = 0;
                        selectionEnd = currentSearch.length();
                    }
                    return true;
            }
        }
        
        // Backspace-Taste
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection()) {
                // Lösche markierten Text
                int start = Math.min(selectionStart, selectionEnd);
                int end = Math.max(selectionStart, selectionEnd);
                currentSearch = currentSearch.substring(0, start) + currentSearch.substring(end);
                clearSelection();
                applyFilters();
                currentPage = 0;
            } else if (!currentSearch.isEmpty()) {
                currentSearch = currentSearch.substring(0, currentSearch.length() - 1);
                applyFilters();
                currentPage = 0;
            }
            return true;
        }
        
        // Delete-Taste
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            // Delete wird wie Backspace behandelt (einfache Version)
            if (!currentSearch.isEmpty()) {
                currentSearch = currentSearch.substring(0, currentSearch.length() - 1);
                applyFilters();
                currentPage = 0;
            }
            return true;
        }
        
        // Leertaste
        if (keyCode == 32) {
            insertCharacter(' ');
            return true;
        }
        
        // Sonderzeichen (KeyCodes wie in SearchBarUtility)
        // Auf deutschen Tastaturen: Komma ohne Shift, Semikolon mit Shift
        if (keyCode == 188 || keyCode == 44) { // Komma/Semikolon
            insertCharacter(modifiers == 1 ? ';' : ',');
            return true;
        }
        
        // Auf deutschen Tastaturen: Punkt ohne Shift, Doppelpunkt mit Shift
        if (keyCode == 190 || keyCode == 46) { // Punkt/Doppelpunkt
            insertCharacter(modifiers == 1 ? ':' : '.');
            return true;
        }
        
        if (keyCode == 189 || keyCode == 47) { // Minus
            insertCharacter('-');
            return true;
        }
        
        if (keyCode == 187) { // Plus/Equal
            insertCharacter(modifiers == 1 ? '+' : '=');
            return true;
        }
        
        if (keyCode == 186) { // Semikolon/Doppelpunkt
            // DEBUG: Logge spezifisch für Semikolon/Doppelpunkt
            System.out.println("[ItemViewer] Semikolon/Doppelpunkt erkannt: keyCode=186, modifiers=" + modifiers + " -> " + (modifiers == 1 ? ':' : ';'));
            // Prüfe ob Shift gedrückt ist (modifiers == 1 = Shift, wie bei anderen Zeichen)
            insertCharacter(modifiers == 1 ? ':' : ';');
            return true;
        }
        
        if (keyCode == 222) { // Apostroph/Anführungszeichen
            insertCharacter('"');
            return true;
        }
        
        if (keyCode == 192) { // Grave Accent
            insertCharacter('`');
            return true;
        }
        
        if (keyCode == 53 && modifiers == 1) { // Prozent mit Shift
            insertCharacter('%');
            return true;
        }
        
        if (keyCode == 92) { // Backslash
            insertCharacter('#');
            return true;
        }
        
        // AltGr-Kombinationen
        if (modifiers == 6) {
            switch (keyCode) {
                case 56:
                    insertCharacter('[');
                    return true;
                case 57:
                    insertCharacter(']');
                    return true;
                case 55:
                    insertCharacter('{');
                    return true;
                case 48:
                    insertCharacter('}');
                    return true;
                case 81:
                    insertCharacter('@');
                    return true;
            }
        }
        
        // Shift-Kombinationen
        if (modifiers == 1) {
            switch (keyCode) {
                case 55:
                    insertCharacter('/');
                    return true;
                case 56:
                    insertCharacter('(');
                    return true;
                case 57:
                    insertCharacter(')');
                    return true;
                case 48:
                    insertCharacter('=');
                    return true;
                case 220:
                    insertCharacter('|');
                    return true;
            }
        }
        
        // Separate ß-Taste
        if (keyCode == 45) {
            insertCharacter('ß');
            return true;
        }
        
        // Separate Umlaut-Tasten
        if (keyCode == 39) { // ä
            insertCharacter('ä');
            return true;
        }
        
        if (keyCode == 59) { // ö
            insertCharacter('ö');
            return true;
        }
        
        if (keyCode == 91) { // ü
            insertCharacter('ü');
            return true;
        }
        
        // Numpad-Tasten
        if (keyCode == 334) {
            insertCharacter('+');
            return true;
        }
        
        if (keyCode == 333) {
            insertCharacter('-');
            return true;
        }
        
        // Numpad-Zahlen 0-9
        if (keyCode == 320) {
            insertCharacter('0');
            return true;
        }
        
        if (keyCode >= 321 && keyCode <= 329) {
            char number = (char) ('0' + (keyCode - 321 + 1));
            insertCharacter(number);
            return true;
        }
        
        // Zahlen 0-9
        if (modifiers == 0 && keyCode >= 48 && keyCode <= 57) {
            char number = (char) keyCode;
            insertCharacter(number);
            return true;
        }
        
        // Buchstaben A-Z (QWERTZ Layout)
        if (keyCode >= 65 && keyCode <= 90) {
            char letter;
            // QWERTZ Layout: Y und Z tauschen
            if (keyCode == 89) {
                letter = modifiers == 1 ? 'Z' : 'z';
            } else if (keyCode == 90) {
                letter = modifiers == 1 ? 'Y' : 'y';
            } else {
                // Normale Buchstaben: Großbuchstaben bei Shift, Kleinbuchstaben ohne Shift
                letter = modifiers == 1 ? (char) keyCode : (char) (keyCode + 32);
            }
            insertCharacter(letter);
            return true;
        }
        
        return false;
    }
    
    /**
     * Fügt ein Zeichen in die Suche ein
     */
    private static void insertCharacter(char character) {
        if (hasSelection()) {
            // Ersetze markierten Text
            int start = Math.min(selectionStart, selectionEnd);
            int end = Math.max(selectionStart, selectionEnd);
            currentSearch = currentSearch.substring(0, start) + character + currentSearch.substring(end);
            clearSelection();
        } else {
            // Füge am Ende ein
            currentSearch += character;
        }
        applyFilters();
        currentPage = 0; // Zurück zur ersten Seite
    }
    
    /**
     * Prüft ob eine Selektion vorhanden ist
     */
    private static boolean hasSelection() {
        return selectionStart >= 0 && selectionEnd > selectionStart;
    }
    
    /**
     * Löscht die aktuelle Text-Selektion
     */
    private static void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }
    
    /**
     * Rendert Text mit Auswahl-Highlighting
     */
    private static void renderTextWithSelection(DrawContext context, MinecraftClient client, String text, int x, int y, int textColor, int maxWidth) {
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        
        // Zeichne Text vor der Auswahl
        if (start > 0) {
            String beforeSelection = text.substring(0, start);
            context.drawText(client.textRenderer, Text.literal(beforeSelection), x, y, textColor, false);
        }
        
        // Berechne Position für die Auswahl
        int selectionX = x + client.textRenderer.getWidth(text.substring(0, start));
        String selectedText = text.substring(start, end);
        int selectionWidth = client.textRenderer.getWidth(selectedText);
        
        // Zeichne Auswahl-Hintergrund
        context.fill(selectionX, y - 1, selectionX + selectionWidth, y + 9, 0xFF0078D4); // Blauer Auswahl-Hintergrund
        
        // Zeichne ausgewählten Text (weiß auf blau)
        context.drawText(client.textRenderer, Text.literal(selectedText), selectionX, y, 0xFFFFFFFF, false);
        
        // Zeichne Text nach der Auswahl
        if (end < text.length()) {
            String afterSelection = text.substring(end);
            int afterX = selectionX + selectionWidth;
            context.drawText(client.textRenderer, Text.literal(afterSelection), afterX, y, textColor, false);
        }
    }
    
    private static void onHudRender(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        // Rendere Item Viewer für Screens, die keine HandledScreen sind (z.B. Spielerinventar)
        // Wird über ScreenMixin.onRender behandelt, daher hier nicht mehr nötig
        // Diese Methode bleibt als Fallback, falls ScreenMixin nicht greift
    }
    
    /**
     * Rendert den Item Viewer für einen Screen (nicht-HandledScreen)
     */
    private static void renderItemViewerForScreen(DrawContext context, MinecraftClient client, Screen screen) {
        if (!isVisible) {
            return;
        }
        
        // Berechne Position
        ViewerPosition pos = calculateViewerPositionForScreen(screen);
        
        // Rendere Item Viewer mit ViewerPosition
        renderItemViewer(context, client, pos);
    }
    
    /**
     * Berechnet die Viewer-Position für nicht-HandledScreen Screens
     */
    private static ViewerPosition calculateViewerPositionForScreen(Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Nutze volle Bildschirmbreite/-höhe
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        
        // Berechne Viewer-Dimensionen
        int gridWidth = ItemViewerGrid.getGridWidth();
        int minViewerWidth = gridWidth + VIEWER_PADDING * 2;
        int maxViewerWidth = screenWidth - VIEWER_PADDING * 2;
        int viewerWidth = Math.max(minViewerWidth, maxViewerWidth);
        
        // Zentriere horizontal
        int viewerX = (screenWidth - viewerWidth) / 2;
        int viewerY = VIEWER_PADDING;
        int viewerHeight = screenHeight - VIEWER_PADDING * 2;
        
        // Berechne Dropdown-Breite
        int maxDropdownWidth = 0;
        for (SortMode mode : SortMode.values()) {
            int textWidth = client.textRenderer.getWidth(mode.getDisplayName());
            maxDropdownWidth = Math.max(maxDropdownWidth, textWidth);
        }
        int dropdownWidth = maxDropdownWidth + SORT_DROPDOWN_PADDING * 2 + 15;
        
        // Suchfeld nutzt volle Breite (minus Hilfe-Button)
        int searchWidth = viewerWidth - VIEWER_PADDING * 2 - HELP_BUTTON_SIZE - 5;
        
        // Dropdown wird unter die Suchleiste platziert
        int dropdownX = viewerX + VIEWER_PADDING;
        int dropdownY = viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5;
        
        // Hilfe-Button Position (2px nach oben verschoben)
        int helpButtonX = viewerX + VIEWER_PADDING;
        int helpButtonY = viewerY + VIEWER_PADDING;
        
        return new ViewerPosition(viewerX, viewerY, viewerWidth, viewerHeight, dropdownX, dropdownY, dropdownWidth, searchWidth, helpButtonX, helpButtonY);
    }
    
    /**
     * Wird vom HandledScreenMixin oder ScreenMixin aufgerufen, um den Item Viewer zu rendern
     * Rendert nach dem Screen-Rendering, damit es über dem dunklen Hintergrund liegt
     */
    // Debug: Letzter gerenderter Screen-Typ (um Logs zu reduzieren)
    private static String lastRenderedScreenType = "";
    
    public static void renderItemViewerInScreen(DrawContext context, MinecraftClient client, HandledScreen<?> screen, int mouseX, int mouseY) {
        // Prüfe ob sichtbar
        if (!isVisible) {
            return;
        }
        
        // DEBUG: Logge UI-Rendering nur wenn sich der Screen-Typ ändert
        String currentScreenType = "";
        if (screen != null) {
            currentScreenType = "HandledScreen (Kiste) - " + screen.getClass().getSimpleName();
        } else if (client.currentScreen != null && !(client.currentScreen instanceof HandledScreen<?>)) {
            currentScreenType = "Spielerinventar - " + client.currentScreen.getClass().getSimpleName();
        }
        
        if (!currentScreenType.equals(lastRenderedScreenType)) {
            System.out.println("[ItemViewer] UI wird gerendert: " + currentScreenType);
            lastRenderedScreenType = currentScreenType;
        }
        
        // Update mouse position
        updateMousePosition(mouseX, mouseY);
        
        // Rendere Item-Viewer
        if (screen != null) {
            // HandledScreen (wird im HandledScreenMixin behandelt)
            renderItemViewer(context, client, screen);
        } else if (client.currentScreen != null && !(client.currentScreen instanceof HandledScreen<?>)) {
            // Nicht-HandledScreen (z.B. Spielerinventar)
            renderItemViewerForScreen(context, client, client.currentScreen);
        }
    }
    
    // UI-Konstanten
    private static final int VIEWER_PADDING = 5;
    private static final int SEARCH_HEIGHT = 20;
    private static final int HELP_BUTTON_SIZE = 18;
    private static final int SORT_DROPDOWN_HEIGHT = 20;
    private static final int SORT_OPTION_HEIGHT = 16;
    private static final int SORT_DROPDOWN_PADDING = 6; // Padding links/rechts im Dropdown
    private static final int PAGINATION_HEIGHT = 20;
    
    // Maus-Position für Hover-Detection
    private static int lastMouseX = 0;
    private static int lastMouseY = 0;
    
    // Dropdown-State
    private static boolean sortDropdownOpen = false;
    private static boolean helpScreenOpen = false;
    
    // Suchfeld-Fokus
    private static boolean searchFieldFocused = false;
    
    private static void renderItemViewer(DrawContext context, MinecraftClient client, HandledScreen<?> handledScreen) {
        Screen screen = (Screen) handledScreen;
        
        // Berechne Viewer-Position (gleiche Logik wie beim Klick-Handling)
        ViewerPosition pos = calculateViewerPosition(handledScreen, screen);
        
        renderItemViewer(context, client, pos);
    }
    
    /**
     * Rendert den Item Viewer mit einer gegebenen ViewerPosition
     */
    private static void renderItemViewer(DrawContext context, MinecraftClient client, ViewerPosition pos) {
        
        // Rendere Hintergrund (sehr transparent, damit Kisten-Hintergrund durchscheint)
        // JEI verwendet keinen Hintergrund oder einen sehr transparenten
        context.fill(pos.viewerX, pos.viewerY, pos.viewerX + pos.viewerWidth, pos.viewerY + pos.viewerHeight,
                     0x40000000); // Sehr transparenter schwarzer Hintergrund (25% Transparenz)
        
        // Rendere Rahmen
        context.fill(pos.viewerX, pos.viewerY, pos.viewerX + pos.viewerWidth, pos.viewerY + 1, 0xFF808080); // Oben
        context.fill(pos.viewerX, pos.viewerY + pos.viewerHeight - 1, pos.viewerX + pos.viewerWidth, pos.viewerY + pos.viewerHeight, 0xFF808080); // Unten
        context.fill(pos.viewerX, pos.viewerY, pos.viewerX + 1, pos.viewerY + pos.viewerHeight, 0xFF808080); // Links
        context.fill(pos.viewerX + pos.viewerWidth - 1, pos.viewerY, pos.viewerX + pos.viewerWidth, pos.viewerY + pos.viewerHeight, 0xFF808080); // Rechts
        
        int currentY = pos.viewerY + VIEWER_PADDING;
        
        // Rendere Hilfe-Button (links)
        renderHelpButton(context, pos.helpButtonX, pos.helpButtonY);
        
        // Rendere Suchfeld (rechts neben Hilfe-Button, volle Breite)
        int searchX = pos.helpButtonX + HELP_BUTTON_SIZE + 5; // 5px Abstand
        renderSearchField(context, searchX, currentY, pos.searchWidth);
        
        currentY += SEARCH_HEIGHT + 5;
        
        // Rendere Sortierungs-Dropdown-Button (ohne Liste, Liste wird später gerendert)
        renderSortDropdownButton(context, pos.dropdownX, currentY);
        
        currentY += SORT_DROPDOWN_HEIGHT + 5;
        
        // Rendere Item-Grid
        List<ItemData> currentPageItems = getCurrentPageItems();
        if (currentPageItems != null && !currentPageItems.isEmpty()) {
            int gridX = pos.viewerX + VIEWER_PADDING;
            int gridY = currentY;
            ItemViewerGrid grid = new ItemViewerGrid(currentPageItems, gridX, gridY, lastMouseX, lastMouseY);
            grid.render(context);
            
            // Rendere Tooltip
            grid.renderTooltip(context);
        }
        
        // Rendere Dropdown-Liste NACH dem Grid (damit sie darüber liegt)
        if (sortDropdownOpen) {
            renderSortDropdownList(context, pos.dropdownX, pos.viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5);
        }
        
        // Rendere Pagination am unteren Rand
        int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
        renderPagination(context, pos.viewerX + VIEWER_PADDING, paginationY, pos.viewerWidth - VIEWER_PADDING * 2);
        
        // Rendere Hilfe-Overlay wenn geöffnet (nach allem anderen, damit es oben liegt)
        if (helpScreenOpen) {
            drawHelpScreen(context, client);
        }
    }
    
    /**
     * Zeichnet das Hilfe-Overlay für den Item Viewer
     */
    private static void drawHelpScreen(DrawContext context, MinecraftClient client) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Hintergrund-Overlay
        context.fill(0, 0, screenWidth, screenHeight, 0x80000000);
        
        // Hilfe-Box - Angepasste Größe für verschiedene Bildschirmgrößen
        int boxWidth = Math.min(400, screenWidth - 40); // Max 400px oder Bildschirmbreite - 40px
        int boxHeight = Math.min(450, screenHeight - 40); // Max 450px oder Bildschirmhöhe - 40px
        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = (screenHeight - boxHeight) / 2;
        
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
        int closeTextWidth = client.textRenderer.getWidth(closeText);
        int closeTextHeight = client.textRenderer.fontHeight;
        context.drawText(
            client.textRenderer,
            closeText,
            closeButtonX + (closeButtonSize - closeTextWidth) / 2,
            closeButtonY + (closeButtonSize - closeTextHeight) / 2,
            0xFFFFFFFF,
            true
        );
        
        // Titel
        String title = "Item Viewer Hilfe";
        int titleWidth = client.textRenderer.getWidth(title);
        context.drawText(
            client.textRenderer,
            title,
            boxX + (boxWidth - titleWidth) / 2,
            boxY + 10,
            0xFFFFFF00,
            true
        );
        
        // Hilfe-Text
        int textY = boxY + 35;
        int lineHeight = 11;
        int textX = boxX + 15;
        int maxTextWidth = boxWidth - 30;
        
        String[] helpText = {
            "Grundlegende Suche:",
            "• Suche nach Item-Namen, Tags oder Aspekten",
            "• Beispiel: 'Schwert' findet alle Schwerter",
            "",
            "Erweiterte Suche:",
            "• @Tag - Suche nach Tags (z.B. @Ring, @Rüstung)",
            "• @Aspekt>Wert - Suche nach Aspekten (z.B. @Schaden>100)",
            "• Kombiniere mehrere Suchbegriffe mit Komma",
            "",
            "Sortierung:",
            "• Standard - Sortiert nach Tags",
            "• Name A-Z - Alphabetisch nach Namen",
            "• Name Z-A - Umgekehrt alphabetisch",
            "• Preis - Nach Preis sortiert",
            "",
            "Navigation:",
            "• Nutze die Pfeile unten für Pagination",
            "• ESC schließt das Hilfe-Overlay",
            ""
        };
        
        // Zeichne alle Zeilen
        for (String line : helpText) {
            // Text umbrechen wenn nötig
            if (client.textRenderer.getWidth(line) > maxTextWidth && !line.isEmpty()) {
                // Einfache Umbrechung: Teile bei Leerzeichen
                String[] words = line.split(" ");
                String currentLine = "";
                for (String word : words) {
                    String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                    if (client.textRenderer.getWidth(testLine) > maxTextWidth) {
                        if (!currentLine.isEmpty()) {
                            context.drawText(
                                client.textRenderer,
                                currentLine,
                                textX,
                                textY,
                                0xFFFFFFFF,
                                true
                            );
                            textY += lineHeight;
                            currentLine = word;
                        } else {
                            // Wort ist zu lang, zeichne es trotzdem
                            context.drawText(
                                client.textRenderer,
                                word,
                                textX,
                                textY,
                                0xFFFFFFFF,
                                true
                            );
                            textY += lineHeight;
                        }
                    } else {
                        currentLine = testLine;
                    }
                }
                if (!currentLine.isEmpty()) {
                    context.drawText(
                        client.textRenderer,
                        currentLine,
                        textX,
                        textY,
                        0xFFFFFFFF,
                        true
                    );
                    textY += lineHeight;
                }
            } else {
                context.drawText(
                    client.textRenderer,
                    line,
                    textX,
                    textY,
                    0xFFFFFFFF,
                    true
                );
                textY += lineHeight;
            }
        }
    }
    
    /**
     * Rendert den Hilfe-Button
     */
    private static void renderHelpButton(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        
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
        
        // Fragezeichen (zentriert)
        String questionMark = "?";
        int textWidth = client.textRenderer.getWidth(questionMark);
        int textX = x + (HELP_BUTTON_SIZE - textWidth) / 2;
        int textY = y + (HELP_BUTTON_SIZE - 9) / 2; // 9 ist die Text-Höhe, zentriert vertikal
        context.drawText(client.textRenderer, Text.literal(questionMark), textX, textY, 0xFFFFFFFF, false);
    }
    
    /**
     * Rendert das Suchfeld
     */
    private static void renderSearchField(DrawContext context, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Hintergrund (heller wenn fokussiert)
        int bgColor = searchFieldFocused ? 0xFF1A1A1A : 0xFF000000;
        context.fill(x, y, x + width, y + SEARCH_HEIGHT, bgColor);
        
        // Rahmen (heller wenn fokussiert)
        int borderColor = searchFieldFocused ? 0xFFFFFFFF : 0xFF808080;
        context.fill(x, y, x + width, y + 1, borderColor); // Oben
        context.fill(x, y + SEARCH_HEIGHT - 1, x + width, y + SEARCH_HEIGHT, borderColor); // Unten
        context.fill(x, y, x + 1, y + SEARCH_HEIGHT, borderColor); // Links
        context.fill(x + width - 1, y, x + width, y + SEARCH_HEIGHT, borderColor); // Rechts
        
        // Text
        String searchText = currentSearch.isEmpty() ? "Suche..." : currentSearch;
        int textColor = currentSearch.isEmpty() ? 0xFF808080 : 0xFFFFFFFF;
        
        // Text kürzen wenn zu lang
        int maxTextWidth = width - 6; // 3px Padding links + 3px rechts
        String displayText = searchText;
        if (client.textRenderer.getWidth(displayText) > maxTextWidth) {
            // Kürze Text von hinten
            while (client.textRenderer.getWidth(displayText + "...") > maxTextWidth && displayText.length() > 0) {
                displayText = displayText.substring(0, displayText.length() - 1);
            }
            displayText += "...";
        }
        
        // Rendere Text mit Selektion
        if (hasSelection() && searchFieldFocused && !currentSearch.isEmpty()) {
            renderTextWithSelection(context, client, currentSearch, x + 3, y + 6, textColor, width - 6);
        } else {
            context.drawText(client.textRenderer, Text.literal(displayText), x + 3, y + 6, textColor, false);
        }
        
        // Cursor anzeigen wenn fokussiert (nur wenn keine Selektion)
        if (searchFieldFocused && !hasSelection()) {
            int cursorX = x + 3;
            if (!currentSearch.isEmpty()) {
                cursorX += client.textRenderer.getWidth(currentSearch);
            }
            // Blinkender Cursor (einfache Version, blinkt nicht)
            context.fill(cursorX, y + 4, cursorX + 1, y + SEARCH_HEIGHT - 4, 0xFFFFFFFF);
        }
    }
    
    /**
     * Rendert nur den Sortierungs-Dropdown-Button (ohne Liste)
     */
    private static void renderSortDropdownButton(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Berechne Dropdown-Breite dynamisch
        int maxDropdownWidth = 0;
        for (SortMode mode : SortMode.values()) {
            int textWidth = client.textRenderer.getWidth(mode.getDisplayName());
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
        context.drawText(client.textRenderer, Text.literal(buttonText), textX, y + 6, textColor, false);
        
        // Dropdown-Pfeil (▼)
        String arrow = sortDropdownOpen ? "▲" : "▼";
        int arrowX = x + dropdownWidth - client.textRenderer.getWidth(arrow) - SORT_DROPDOWN_PADDING;
        context.drawText(client.textRenderer, Text.literal(arrow), arrowX, y + 6, 0xFF808080, false);
    }
    
    /**
     * Rendert die Dropdown-Liste (wird NACH dem Item-Grid gerendert, damit sie darüber liegt)
     */
    private static void renderSortDropdownList(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Berechne Dropdown-Breite dynamisch
        int maxDropdownWidth = 0;
        for (SortMode mode : SortMode.values()) {
            int textWidth = client.textRenderer.getWidth(mode.getDisplayName());
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
            context.drawText(client.textRenderer, Text.literal(optionText), optionTextX, optionY + 4, optionTextColor, false);
            
            optionIndex++;
        }
    }
    
    /**
     * Rendert Pagination-Buttons
     */
    private static void renderPagination(DrawContext context, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        int totalPages = (int) Math.ceil((double) filteredItems.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        
        // Berechne Button-Breite dynamisch basierend auf Text
        String prevText = "< Zurück";
        String nextText = "Weiter >";
        int prevTextWidth = client.textRenderer.getWidth(prevText);
        int nextTextWidth = client.textRenderer.getWidth(nextText);
        int buttonWidth = Math.max(prevTextWidth, nextTextWidth) + 10; // 10px Padding
        int buttonHeight = PAGINATION_HEIGHT;
        
        // Previous-Button
        boolean canGoPrevious = currentPage > 0;
        int prevX = x;
        int prevBgColor = canGoPrevious ? 0xFF404040 : 0xFF202020;
        context.fill(prevX, y, prevX + buttonWidth, y + buttonHeight, prevBgColor);
        context.fill(prevX, y, prevX + buttonWidth, y + 1, 0xFF808080);
        context.fill(prevX, y + buttonHeight - 1, prevX + buttonWidth, y + buttonHeight, 0xFF808080);
        context.fill(prevX, y, prevX + 1, y + buttonHeight, 0xFF808080);
        context.fill(prevX + buttonWidth - 1, y, prevX + buttonWidth, y + buttonHeight, 0xFF808080);
        
        int prevTextX = prevX + (buttonWidth - prevTextWidth) / 2;
        int prevTextColor = canGoPrevious ? 0xFFFFFFFF : 0xFF808080;
        context.drawText(client.textRenderer, Text.literal(prevText), prevTextX, y + 6, prevTextColor, false);
        
        // Seiten-Info
        String pageText = (currentPage + 1) + " / " + totalPages;
        int pageTextWidth = client.textRenderer.getWidth(pageText);
        int pageTextX = x + (width - pageTextWidth) / 2;
        context.drawText(client.textRenderer, Text.literal(pageText), pageTextX, y + 6, 0xFFFFFFFF, false);
        
        // Next-Button
        boolean canGoNext = currentPage < totalPages - 1;
        int nextX = x + width - buttonWidth;
        int nextBgColor = canGoNext ? 0xFF404040 : 0xFF202020;
        context.fill(nextX, y, nextX + buttonWidth, y + buttonHeight, nextBgColor);
        context.fill(nextX, y, nextX + buttonWidth, y + 1, 0xFF808080);
        context.fill(nextX, y + buttonHeight - 1, nextX + buttonWidth, y + buttonHeight, 0xFF808080);
        context.fill(nextX, y, nextX + 1, y + buttonHeight, 0xFF808080);
        context.fill(nextX + buttonWidth - 1, y, nextX + buttonWidth, y + buttonHeight, 0xFF808080);
        
        int nextTextX = nextX + (buttonWidth - nextTextWidth) / 2;
        int nextTextColor = canGoNext ? 0xFFFFFFFF : 0xFF808080;
        context.drawText(client.textRenderer, Text.literal(nextText), nextTextX, y + 6, nextTextColor, false);
    }
    
    /**
     * Aktualisiert die Maus-Position (wird vom Mixin aufgerufen)
     */
    public static void updateMousePosition(int mouseX, int mouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }
    
    /**
     * Berechnet die Viewer-Position und Dimensionen (wird für Rendering und Klick-Handling verwendet)
     */
    private static ViewerPosition calculateViewerPosition(HandledScreen<?> handledScreen, Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Hole Inventar-Position
        int inventoryX = 0;
        int inventoryWidth = 176;
        
        try {
            java.lang.reflect.Field xField = HandledScreen.class.getDeclaredField("x");
            xField.setAccessible(true);
            inventoryX = xField.getInt(handledScreen);
            
            java.lang.reflect.Field widthField = HandledScreen.class.getDeclaredField("backgroundWidth");
            widthField.setAccessible(true);
            inventoryWidth = widthField.getInt(handledScreen);
        } catch (Exception e) {
            // Fallback
        }
        
        // Berechne verfügbaren Platz
        int availableWidthRight = screen.width - (inventoryX + inventoryWidth) - 10;
        int availableWidthLeft = inventoryX - 20; // 10px links + 10px rechts Padding
        
        boolean placeRight = availableWidthRight >= ItemViewerGrid.getGridWidth() + VIEWER_PADDING * 2;
        int viewerX;
        int maxViewerWidth;
        
        if (placeRight) {
            // Nutze volle verfügbare Breite rechts vom Inventar
            viewerX = inventoryX + inventoryWidth + 10;
            maxViewerWidth = screen.width - viewerX - 10; // Volle Breite bis zum rechten Rand
        } else {
            // Nutze volle verfügbare Breite links vom Inventar
            viewerX = 10; // Starte am linken Rand mit Padding
            maxViewerWidth = availableWidthLeft; // Volle Breite bis zum Inventar
        }
        
        // Berechne Viewer-Dimensionen
        int gridWidth = ItemViewerGrid.getGridWidth();
        int minViewerWidth = gridWidth + VIEWER_PADDING * 2;
        // Nutze maximale verfügbare Breite, aber mindestens die minimale Breite
        int viewerWidth = Math.max(minViewerWidth, maxViewerWidth);
        
        // Berechne Dropdown-Breite
        int maxDropdownWidth = 0;
        for (SortMode mode : SortMode.values()) {
            int textWidth = client.textRenderer.getWidth(mode.getDisplayName());
            maxDropdownWidth = Math.max(maxDropdownWidth, textWidth);
        }
        int dropdownWidth = maxDropdownWidth + SORT_DROPDOWN_PADDING * 2 + 15;
        
        // Suchfeld nutzt volle Breite (minus Hilfe-Button)
        int searchWidth = viewerWidth - VIEWER_PADDING * 2 - HELP_BUTTON_SIZE - 5; // 5px Abstand zum Hilfe-Button
        
        // Nutze volle verfügbare Höhe (von oben bis unten mit Padding)
        int viewerY = 10; // 10px Padding oben
        int viewerHeight = screen.height - 20; // 10px oben + 10px unten = 20px Padding
        
        // Dropdown wird unter die Suchleiste platziert
        int dropdownX = viewerX + VIEWER_PADDING;
        int dropdownY = viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5; // Unter der Suchleiste
        
        // Hilfe-Button Position (links neben der Suchleiste)
        int helpButtonX = viewerX + VIEWER_PADDING;
        int helpButtonY = viewerY + VIEWER_PADDING + 2;
        
        return new ViewerPosition(viewerX, viewerY, viewerWidth, viewerHeight, dropdownX, dropdownY, dropdownWidth, searchWidth, helpButtonX, helpButtonY);
    }
    
    /**
     * Hilfsklasse für Viewer-Position
     */
    private static class ViewerPosition {
        final int viewerX, viewerY, viewerWidth, viewerHeight;
        final int dropdownX, dropdownY, dropdownWidth, searchWidth;
        final int helpButtonX, helpButtonY;
        
        ViewerPosition(int viewerX, int viewerY, int viewerWidth, int viewerHeight,
                      int dropdownX, int dropdownY, int dropdownWidth, int searchWidth,
                      int helpButtonX, int helpButtonY) {
            this.viewerX = viewerX;
            this.viewerY = viewerY;
            this.viewerWidth = viewerWidth;
            this.viewerHeight = viewerHeight;
            this.dropdownX = dropdownX;
            this.dropdownY = dropdownY;
            this.dropdownWidth = dropdownWidth;
            this.searchWidth = searchWidth;
            this.helpButtonX = helpButtonX;
            this.helpButtonY = helpButtonY;
        }
    }
    
    /**
     * Behandelt Maus-Klicks (wird vom Mixin aufgerufen)
     */
    public static boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) { // Nur Linksklick
            return false;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null || !(client.currentScreen instanceof HandledScreen<?> handledScreen)) {
            return false;
        }
        
        // Berechne Viewer-Position (gleiche Logik wie beim Rendering)
        ViewerPosition pos = calculateViewerPosition(handledScreen, client.currentScreen);
        
        // Prüfe Klick auf Suchfeld (setzt Fokus)
        int searchX = pos.helpButtonX + HELP_BUTTON_SIZE + 5;
        int searchY = pos.viewerY + VIEWER_PADDING;
        if (mouseX >= searchX && mouseX < searchX + pos.searchWidth &&
            mouseY >= searchY && mouseY < searchY + SEARCH_HEIGHT) {
            searchFieldFocused = true;
            return true;
        }
        
        // Prüfe Klick auf Hilfe-Overlay Schließen-Button
        if (helpScreenOpen) {
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();
            int boxWidth = Math.min(400, screenWidth - 40);
            int boxHeight = Math.min(450, screenHeight - 40);
            int boxX = (screenWidth - boxWidth) / 2;
            int boxY = (screenHeight - boxHeight) / 2;
            int closeButtonX = boxX + boxWidth - 20;
            int closeButtonY = boxY + 5;
            int closeButtonSize = 15;
            
            if (mouseX >= closeButtonX && mouseX < closeButtonX + closeButtonSize &&
                mouseY >= closeButtonY && mouseY < closeButtonY + closeButtonSize) {
                if (button == 0) {
                    helpScreenOpen = false;
                    return true;
                }
            }
            
            // Klick außerhalb des Hilfe-Overlays schließt es
            if (mouseX < boxX || mouseX >= boxX + boxWidth ||
                mouseY < boxY || mouseY >= boxY + boxHeight) {
                if (button == 0) {
                    helpScreenOpen = false;
                    return true;
                }
            }
        }
        
        // Prüfe Klick auf Hilfe-Button
        if (mouseX >= pos.helpButtonX && mouseX < pos.helpButtonX + HELP_BUTTON_SIZE &&
            mouseY >= pos.helpButtonY && mouseY < pos.helpButtonY + HELP_BUTTON_SIZE) {
            helpScreenOpen = !helpScreenOpen;
            searchFieldFocused = false; // Fokus entfernen wenn Hilfe geöffnet wird
            return true;
        }
        
        // Prüfe Klick auf Dropdown-Button
        if (mouseX >= pos.dropdownX && mouseX < pos.dropdownX + pos.dropdownWidth &&
            mouseY >= pos.dropdownY && mouseY < pos.dropdownY + SORT_DROPDOWN_HEIGHT) {
            sortDropdownOpen = !sortDropdownOpen;
            return true;
        }
        
        // Prüfe Klick auf Dropdown-Optionen (wenn geöffnet)
        if (sortDropdownOpen) {
            int dropdownListY = pos.dropdownY + SORT_DROPDOWN_HEIGHT;
            int optionIndex = 0;
            for (SortMode mode : SortMode.values()) {
                int optionY = dropdownListY + optionIndex * SORT_OPTION_HEIGHT;
                if (mouseX >= pos.dropdownX && mouseX < pos.dropdownX + pos.dropdownWidth &&
                    mouseY >= optionY && mouseY < optionY + SORT_OPTION_HEIGHT) {
                    setSortMode(mode);
                    sortDropdownOpen = false;
                    return true;
                }
                optionIndex++;
            }
            
            // Klick außerhalb des Dropdowns schließt es
            if (mouseX < pos.dropdownX || mouseX >= pos.dropdownX + pos.dropdownWidth ||
                mouseY < pos.dropdownY || mouseY >= dropdownListY + SortMode.values().length * SORT_OPTION_HEIGHT) {
                sortDropdownOpen = false;
            }
        }
        
        // Prüfe Klick auf Pagination-Buttons
        int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
        int paginationX = pos.viewerX + VIEWER_PADDING;
        int paginationWidth = pos.viewerWidth - VIEWER_PADDING * 2;
        
        int totalPages = (int) Math.ceil((double) filteredItems.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        
        // Berechne Button-Breite dynamisch (gleiche Logik wie beim Rendering)
        int prevTextWidth = client.textRenderer.getWidth("< Zurück");
        int nextTextWidth = client.textRenderer.getWidth("Weiter >");
        int buttonWidth = Math.max(prevTextWidth, nextTextWidth) + 10; // 10px Padding
        
        // Previous-Button
        int prevX = paginationX;
        if (mouseX >= prevX && mouseX < prevX + buttonWidth &&
            mouseY >= paginationY && mouseY < paginationY + PAGINATION_HEIGHT) {
            if (currentPage > 0) {
                currentPage--;
                return true;
            }
        }
        
        // Next-Button
        int nextX = paginationX + paginationWidth - buttonWidth;
        if (mouseX >= nextX && mouseX < nextX + buttonWidth &&
            mouseY >= paginationY && mouseY < paginationY + PAGINATION_HEIGHT) {
            if (currentPage < totalPages - 1) {
                currentPage++;
                return true;
            }
        }
        
        return false;
    }
    
    private static List<ItemData> getCurrentPageItems() {
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filteredItems.size());
        if (start >= filteredItems.size()) {
            return new ArrayList<>();
        }
        return filteredItems.subList(start, end);
    }
    
    public static void setSearch(String search) {
        currentSearch = search;
        applyFilters();
    }
    
    public static void setSortMode(SortMode mode) {
        currentSortMode = mode;
        applySorting();
    }
    
    private static void applyFilters() {
        if (currentSearch == null || currentSearch.trim().isEmpty()) {
            filteredItems = new ArrayList<>(allItems);
        } else {
            SearchQuery query = ItemSearchParser.parse(currentSearch);
            filteredItems = allItems.stream()
                .filter(item -> ItemFilter.matchesSearch(item, query))
                .collect(Collectors.toList());
        }
        applySorting();
        currentPage = 0; // Zurück zur ersten Seite
    }
    
    private static void applySorting() {
        Comparator<ItemData> comparator = currentSortMode.getComparator();
        filteredItems.sort(comparator);
    }
    
    public static boolean isVisible() {
        return isVisible;
    }
    
    /**
     * Prüft, ob ein Blueprint gefunden wurde (ähnlich wie im Moblexicon)
     * @param blueprintName Der Name des Blueprints (z.B. "Abgenutze Plattenstampfer")
     * @return true wenn gefunden, false wenn nicht gefunden
     */
    private static boolean isBlueprintFound(String blueprintName) {
        if (blueprintName == null || blueprintName.isEmpty()) {
            return false;
        }
        
        try {
            BPViewerUtility bpViewer = BPViewerUtility.getInstance();
            
            // Prüfe foundBlueprints (ähnlich wie in BPViewerUtility.isBlueprintFoundAnywhere)
            // Für Drachenzahn: prüfe beide Rarities separat
            if (blueprintName.equals("Drachenzahn")) {
                return bpViewer.isBlueprintFoundAnywhere(blueprintName + ":epic") ||
                       bpViewer.isBlueprintFoundAnywhere(blueprintName + ":legendary");
            } else {
                // Für andere Blueprints: prüfe mit und ohne Rarity-Suffix
                return bpViewer.isBlueprintFoundAnywhere(blueprintName) ||
                       bpViewer.isBlueprintFoundAnywhere(blueprintName + ":epic") ||
                       bpViewer.isBlueprintFoundAnywhere(blueprintName + ":legendary");
            }
        } catch (Exception e) {
            // Falls BPViewerUtility nicht verfügbar ist
            return false;
        }
    }
    
    /**
     * Gibt den gefundenen-Status als Text zurück (für Anzeige)
     * @param item Das Item, das geprüft werden soll
     * @return "[Gefunden]" in grün oder "[Nicht Gefunden]" in rot
     */
    public static String getFoundStatusText(ItemData item) {
        if (item.info == null || !Boolean.TRUE.equals(item.info.blueprint)) {
            return null; // Kein Bauplan, kein Status
        }
        
        boolean isFound = isBlueprintFound(item.name);
        return isFound ? "[Gefunden]" : "[Nicht Gefunden]";
    }
    
    /**
     * Gibt die Farbe für den gefundenen-Status zurück
     * @param item Das Item, das geprüft werden soll
     * @return 0xFF00FF00 (grün) wenn gefunden, 0xFFFF0000 (rot) wenn nicht gefunden, 0xFFFFFFFF (weiß) wenn kein Bauplan
     */
    public static int getFoundStatusColor(ItemData item) {
        if (item.info == null || !Boolean.TRUE.equals(item.info.blueprint)) {
            return 0xFFFFFFFF; // Kein Bauplan, weiß
        }
        
        boolean isFound = isBlueprintFound(item.name);
        return isFound ? 0xFF00FF00 : 0xFFFF0000; // Grün wenn gefunden, rot wenn nicht
    }
}


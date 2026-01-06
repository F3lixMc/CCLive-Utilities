package net.felix.utilities.ItemViewer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
    private static int cursorPosition = 0;  // Cursor-Position im Suchfeld
    
    // Pagination
    // Aktuelle verfügbare Grid-Breite (wird dynamisch berechnet)
    private static int currentGridAvailableWidth = 108; // Standard: 6 Spalten * 18px
    // Aktuelle verfügbare Grid-Höhe (wird dynamisch berechnet)
    private static int currentGridAvailableHeight = 144; // Standard: 8 Zeilen * 18px
    private static int currentPage = 0;
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String ITEMS_CONFIG_FILE = "assets/cclive-utilities/items.json";
    
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        try {
            // Initialisiere Favoriten-Manager
            FavoriteBlueprintsManager.initialize();
            
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
                // Lese die gesamte Datei als String, um sie zweimal zu parsen
                String jsonContent = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                
                // Parse als JsonObject für vollständige JSON-Objekte
                JsonObject rootJson = JsonParser.parseString(jsonContent).getAsJsonObject();
                
                // Parse als ItemListData für ItemData-Objekte
                try (var reader = new java.io.StringReader(jsonContent)) {
                    Type type = new TypeToken<ItemListData>(){}.getType();
                    ItemListData data = gson.fromJson(reader, type);
                    
                    if (data != null) {
                        allItems = new ArrayList<>();
                        
                        // Unterstütze verschiedene Strukturen:
                        // 1. Alte Struktur: {"items": [...]}
                        if (data.items != null && !data.items.isEmpty()) {
                            for (ItemData item : data.items) {
                                item.category = "items";
                            }
                            allItems.addAll(data.items);
                        }
                        
                        // 2. Neue Struktur (Deutsch): {"bauplan": [...], "fähigkeiten": [...], "module": [...]}
                        if (data.bauplan != null && !data.bauplan.isEmpty()) {
                            // Extrahiere JSON-Objekte für Baupläne
                            if (rootJson.has("bauplan") && rootJson.get("bauplan").isJsonArray()) {
                                var bauplanArray = rootJson.getAsJsonArray("bauplan");
                                for (int i = 0; i < bauplanArray.size() && i < data.bauplan.size(); i++) {
                                    JsonElement element = bauplanArray.get(i);
                                    if (element.isJsonObject()) {
                                        JsonObject jsonObj = element.getAsJsonObject();
                                        ItemData item = data.bauplan.get(i);
                                        item.jsonObject = jsonObj;
                                        item.category = "blueprints"; // Normalisiere zu englisch
                                        
                                        // Speichere in Map für Favoriten
                                        if (item.name != null && !item.name.isEmpty()) {
                                            blueprintJsonMap.put(item.name, jsonObj);
                                        }
                                    }
                                }
                            } else {
                                // Fallback: Keine JSON-Objekte verfügbar
                                for (ItemData item : data.bauplan) {
                                    item.category = "blueprints"; // Normalisiere zu englisch
                                }
                            }
                            allItems.addAll(data.bauplan);
                        }
                        if (data.fähigkeiten != null && !data.fähigkeiten.isEmpty()) {
                            for (ItemData item : data.fähigkeiten) {
                                item.category = "abilities"; // Normalisiere zu englisch
                            }
                            allItems.addAll(data.fähigkeiten);
                        }
                        if (data.module != null && !data.module.isEmpty()) {
                            for (ItemData item : data.module) {
                                item.category = "modules"; // Normalisiere zu englisch
                            }
                            allItems.addAll(data.module);
                        }
                        
                        // 3. Neue Struktur (Englisch): {"blueprints": [...], "abilities": [...], "modules": [...]}
                        if (data.blueprints != null && !data.blueprints.isEmpty()) {
                            // Extrahiere JSON-Objekte für Blueprints
                            if (rootJson.has("blueprints") && rootJson.get("blueprints").isJsonArray()) {
                                var blueprintsArray = rootJson.getAsJsonArray("blueprints");
                                for (int i = 0; i < blueprintsArray.size() && i < data.blueprints.size(); i++) {
                                    JsonElement element = blueprintsArray.get(i);
                                    if (element.isJsonObject()) {
                                        JsonObject jsonObj = element.getAsJsonObject();
                                        ItemData item = data.blueprints.get(i);
                                        item.jsonObject = jsonObj;
                                        item.category = "blueprints";
                                        
                                        // Speichere in Map für Favoriten
                                        if (item.name != null && !item.name.isEmpty()) {
                                            blueprintJsonMap.put(item.name, jsonObj);
                                        }
                                    }
                                }
                            } else {
                                // Fallback: Keine JSON-Objekte verfügbar
                                for (ItemData item : data.blueprints) {
                                    item.category = "blueprints";
                                }
                            }
                            allItems.addAll(data.blueprints);
                        }
                        if (data.abilities != null && !data.abilities.isEmpty()) {
                            for (ItemData item : data.abilities) {
                                item.category = "abilities";
                            }
                            allItems.addAll(data.abilities);
                        }
                        if (data.modules != null && !data.modules.isEmpty()) {
                            for (ItemData item : data.modules) {
                                item.category = "modules";
                            }
                            allItems.addAll(data.modules);
                        }
                        if (data.runes != null && !data.runes.isEmpty()) {
                            for (ItemData item : data.runes) {
                                item.category = "runes";
                            }
                            allItems.addAll(data.runes);
                        }
                        if (data.power_crystals != null && !data.power_crystals.isEmpty()) {
                            for (ItemData item : data.power_crystals) {
                                item.category = "power_crystals";
                            }
                            allItems.addAll(data.power_crystals);
                        }
                        if (data.power_crystal_slots != null && !data.power_crystal_slots.isEmpty()) {
                            for (ItemData item : data.power_crystal_slots) {
                                item.category = "power_crystal_slots";
                            }
                            allItems.addAll(data.power_crystal_slots);
                        }
                        if (data.essences != null && !data.essences.isEmpty()) {
                            for (ItemData item : data.essences) {
                                item.category = "essences";
                            }
                            allItems.addAll(data.essences);
                        }
                        if (data.module_bags != null && !data.module_bags.isEmpty()) {
                            for (ItemData item : data.module_bags) {
                                item.category = "module_bags";
                            }
                            allItems.addAll(data.module_bags);
                        }
                        
                        if (!allItems.isEmpty()) {
                            filteredItems = new ArrayList<>(allItems);
                            applySorting();
                        }
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
                cursorPosition = start;
                clearSelection();
                applyFilters();
                currentPage = 0;
            } else if (cursorPosition > 0) {
                // Lösche Zeichen vor Cursor
                currentSearch = currentSearch.substring(0, cursorPosition - 1) + currentSearch.substring(cursorPosition);
                cursorPosition--;
                applyFilters();
                currentPage = 0;
            }
            return true;
        }
        
        // Delete-Taste
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection()) {
                // Lösche markierten Text
                int start = Math.min(selectionStart, selectionEnd);
                int end = Math.max(selectionStart, selectionEnd);
                currentSearch = currentSearch.substring(0, start) + currentSearch.substring(end);
                cursorPosition = start;
                clearSelection();
                applyFilters();
                currentPage = 0;
            } else if (cursorPosition < currentSearch.length()) {
                // Lösche Zeichen nach Cursor
                currentSearch = currentSearch.substring(0, cursorPosition) + currentSearch.substring(cursorPosition + 1);
                applyFilters();
                currentPage = 0;
            }
            return true;
        }
        
        // Pfeiltasten für Cursor-Bewegung
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (modifiers == 1) { // Shift + Links = Selektion erweitern
                if (selectionStart == -1) {
                    selectionStart = cursorPosition;
                    selectionEnd = cursorPosition;
                }
                if (cursorPosition > 0) {
                    cursorPosition--;
                    selectionEnd = cursorPosition;
                }
            } else {
                clearSelection();
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
            }
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (modifiers == 1) { // Shift + Rechts = Selektion erweitern
                if (selectionStart == -1) {
                    selectionStart = cursorPosition;
                    selectionEnd = cursorPosition;
                }
                if (cursorPosition < currentSearch.length()) {
                    cursorPosition++;
                    selectionEnd = cursorPosition;
                }
            } else {
                clearSelection();
                if (cursorPosition < currentSearch.length()) {
                    cursorPosition++;
                }
            }
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            clearSelection();
            cursorPosition = 0;
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_END) {
            clearSelection();
            cursorPosition = currentSearch.length();
            return true;
        }
        
        // Leertaste
        if (keyCode == 32) {
            insertCharacter(' ');
            return true;
        }
        
        // AltGr-Kombinationen (müssen VOR normalen Komma/Punkt-Behandlungen geprüft werden)
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
                case 188: // Komma-Taste mit AltGr = <
                    insertCharacter('<');
                    return true;
                case 190: // Punkt-Taste mit AltGr = >
                    insertCharacter('>');
                    return true;
            }
        }
        
        // < und > mit Shift (müssen VOR normalen Komma/Punkt-Behandlungen geprüft werden)
        if (modifiers == 1) { // Shift
            if (keyCode == 188) { // Komma-Taste mit Shift = < (auf einigen Layouts)
                insertCharacter('<');
                return true;
            }
            if (keyCode == 190) { // Punkt-Taste mit Shift = > (auf einigen Layouts)
                insertCharacter('>');
                return true;
            }
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
        
        // Separate +-Taste (wie im Bauplan-Shop)
        if (keyCode == 93) {
            insertCharacter('+');
            return true;
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
        
        // Direkte Unterstützung für < und > (wie im Bauplan-Shop)
        // keyCode 162 ohne Shift = <, mit Shift = >
        if (keyCode == 162) {
            if (modifiers == 0) {
                insertCharacter('<');
            } else if (modifiers == 1) {
                insertCharacter('>');
            }
            return true;
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
            cursorPosition = start + 1;
            clearSelection();
        } else {
            // Füge an Cursor-Position ein
            currentSearch = currentSearch.substring(0, cursorPosition) + character + currentSearch.substring(cursorPosition);
            cursorPosition++;
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
    
    /**
     * Berechnet die Cursor-Position basierend auf der Mausklick-Position
     */
    private static int calculateCursorPosition(int clickX, String text, net.minecraft.client.font.TextRenderer textRenderer) {
        if (text.isEmpty()) {
            return 0;
        }
        
        // Finde die beste Position basierend auf der Klick-Position
        int bestPosition = 0;
        int bestDistance = Integer.MAX_VALUE;
        
        for (int i = 0; i <= text.length(); i++) {
            int textWidth = textRenderer.getWidth(text.substring(0, i));
            int distance = Math.abs(clickX - textWidth);
            
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPosition = i;
            }
        }
        
        return bestPosition;
    }
    
    private static void onHudRender(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        // Hilfe-Overlay wird jetzt in den Mixins gerendert (HelpOverlayMixin/HelpOverlayScreenMixin am RETURN-Punkt),
        // damit es wirklich über allen Items liegt (nicht hier im HudRenderCallback)
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
        // Nutze maximale verfügbare Breite, aber mindestens die minimale Breite (Standard-Grid)
        int minViewerWidth = ItemViewerGrid.getDefaultGridWidth() + VIEWER_PADDING * 2;
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
        
        // Symbol-Button Position (rechts neben dem Dropdown, gleiche Höhe für symmetrisches Aussehen)
        int searchX = helpButtonX + HELP_BUTTON_SIZE + 5;
        int symbolButtonX = searchX + searchWidth - SYMBOL_BUTTON_SIZE; // Rechts bündig mit Suchleiste
        int symbolButtonY = dropdownY; // Gleiche Y-Position wie Dropdown
        
        // Favoriten-Button Position (rechts vom Dropdown, gleiche Höhe)
        int favoritesButtonX = dropdownX + dropdownWidth + 5; // 5px Abstand zum Dropdown
        int favoritesButtonY = dropdownY; // Gleiche Y-Position wie Dropdown
        
        // Kit-Button Position (rechts vom Favoriten-Button, gleiche Höhe)
        int kitButtonX = favoritesButtonX + SORT_DROPDOWN_HEIGHT + 5; // 5px Abstand zum Favoriten-Button
        int kitButtonY = dropdownY; // Gleiche Y-Position wie Dropdown
        
        return new ViewerPosition(viewerX, viewerY, viewerWidth, viewerHeight, dropdownX, dropdownY, dropdownWidth, searchWidth, helpButtonX, helpButtonY, symbolButtonX, symbolButtonY, favoritesButtonX, favoritesButtonY, kitButtonX, kitButtonY);
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
    private static final int HELP_BUTTON_SIZE = 20; // Gleiche Höhe wie Suchleiste
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
    private static int helpScreenScrollOffset = 0;
    private static boolean helpScreenHovered = false;
    
    // Suchfeld-Fokus
    private static boolean searchFieldFocused = false;
    
    // Favoriten-Modus
    private static boolean favoritesMode = false;
    private static final Identifier STAR_ICON_TEXTURE = Identifier.of("cclive-utilities", "textures/icons/star_icon.png");
    
    // Map von Blueprint-Namen zu vollständigen JSON-Objekten (für Favoriten)
    private static java.util.Map<String, JsonObject> blueprintJsonMap = new java.util.HashMap<>();
    
    // Gehovertes Item (für Rechtsklick-Handling)
    private static ItemData hoveredItemForClick = null;
    
    // Symbol-Button Variablen (für @, <, >)
    private static final int SYMBOL_BUTTON_SIZE = 20; // Gleiche Höhe wie Dropdown für symmetrisches Aussehen
    private static boolean isSymbolMenuOpen = false;
    private static boolean isSymbolButtonHovered = false;
    private static final Identifier APPLE_ICON_TEXTURE = Identifier.of("cclive-utilities", "textures/icons/apple_icon_2.png");
    
    // Kit-Button Variablen
    private static final int KIT_BUTTON_SIZE = 20; // Gleiche Höhe wie andere Buttons
    private static final Identifier KIT_ICON_TEXTURE = Identifier.of("cclive-utilities", "textures/icons/kits_icon_2.png");
    private static boolean kitFilterActive = false; // Ob Kit-Filter aktiv ist (Linksklick)
    private static final int KIT_BUTTON_INDEX = 0; // Verwende Kit Button 1 aus KitFilterUtility

    // Kategorie-Buttons im Hilfe-Overlay
    private static final java.util.List<CategoryButton> CATEGORY_BUTTON_DEFS = Arrays.asList(
            new CategoryButton("Baupläne", java.util.List.of("bauplan")),
            new CategoryButton("Module", java.util.List.of("modul")),
            new CategoryButton("Modul-Taschen", java.util.List.of("modultasche")),
            new CategoryButton("Machtkristall", java.util.List.of("machtkristall")),
            new CategoryButton("MK-Slots", java.util.List.of("slot"))
    );
    private static CategoryButton activeCategoryOverlay = null;
    
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
        
        // Rendere Suchfeld (rechts neben Hilfe-Button)
        int searchX = pos.helpButtonX + HELP_BUTTON_SIZE + 5; // 5px Abstand
        renderSearchField(context, searchX, currentY, pos.searchWidth);
        
        // Prüfe Hover über Symbol-Button (für Menü-Öffnung)
        checkSymbolButtonHover(pos.symbolButtonX, pos.symbolButtonY);
        
        // Rendere Symbol-Button (oben rechts neben der Suchleiste)
        renderSymbolButton(context, pos.symbolButtonX, pos.symbolButtonY);
        
        // Rendere Symbol-Menü wenn geöffnet
        if (isSymbolMenuOpen) {
            renderSymbolMenu(context, pos.symbolButtonX, pos.symbolButtonY);
        }
        
        currentY += SEARCH_HEIGHT + 5;
        
        // Rendere Sortierungs-Dropdown-Button (ohne Liste, Liste wird später gerendert)
        renderSortDropdownButton(context, pos.dropdownX, currentY);
        
        // Rendere Favoriten-Button (rechts vom Dropdown)
        renderFavoritesButton(context, pos.favoritesButtonX, pos.favoritesButtonY);
        
        // Rendere Kit-Button (rechts vom Favoriten-Button)
        renderKitButton(context, pos.kitButtonX, pos.kitButtonY);
        
        currentY += SORT_DROPDOWN_HEIGHT + 5;
        
        // Rendere Item-Grid
        // Berechne verfügbare Breite für das Grid
        int gridAvailableWidth = pos.viewerWidth - VIEWER_PADDING * 2;
        currentGridAvailableWidth = gridAvailableWidth; // Speichere für Pagination etc.
        
        // Berechne verfügbare Höhe für das Grid (viewerHeight minus Suchfeld, Dropdown, Pagination)
        int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
        int gridAvailableHeight = paginationY - currentY; // Von currentY (nach Dropdown) bis Pagination
        currentGridAvailableHeight = gridAvailableHeight; // Speichere für Pagination etc.
        
        List<ItemData> currentPageItems = getCurrentPageItems();
        if (currentPageItems != null && !currentPageItems.isEmpty()) {
            int gridX = pos.viewerX + VIEWER_PADDING;
            int gridY = currentY;
            ItemViewerGrid grid = new ItemViewerGrid(currentPageItems, gridX, gridY, lastMouseX, lastMouseY, gridAvailableWidth, gridAvailableHeight);
            grid.render(context);
            
            // Speichere gehoveres Item für Rechtsklick-Handling
            hoveredItemForClick = grid.getHoveredItem();
            
            // Rendere Tooltip
            grid.renderTooltip(context);
        } else {
            hoveredItemForClick = null;
        }
        
        // Rendere Dropdown-Liste NACH dem Grid (damit sie darüber liegt)
        if (sortDropdownOpen) {
            renderSortDropdownList(context, pos.dropdownX, pos.viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5);
        }
        
        // Rendere Pagination am unteren Rand (paginationY wurde bereits oben berechnet)
        renderPagination(context, pos.viewerX + VIEWER_PADDING, paginationY, pos.viewerWidth - VIEWER_PADDING * 2);
        
        // Rendere Button-Tooltips (am Ende, damit sie über allem liegen)
        renderButtonTooltips(context, pos);
        
        // Hilfe-Overlay wird jetzt in den Mixins (ScreenMixin/HandledScreenMixin) gerendert,
        // nach dem ItemViewer, damit es über allen Items liegt
        // AspectOverlay wird auch in den Mixins gerendert
    }
    
    /**
     * Rendert Tooltips für die Buttons (Hilfe, Symbol, Favoriten)
     */
    private static void renderButtonTooltips(DrawContext context, ViewerPosition pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Prüfe Hover über Hilfe-Button
        boolean helpHovered = lastMouseX >= pos.helpButtonX && lastMouseX < pos.helpButtonX + HELP_BUTTON_SIZE &&
                             lastMouseY >= pos.helpButtonY && lastMouseY < pos.helpButtonY + HELP_BUTTON_SIZE;
        
        // Prüfe Hover über Symbol-Button (nur wenn Menü nicht geöffnet ist, um Konflikte zu vermeiden)
        boolean symbolHovered = !isSymbolMenuOpen && 
                               lastMouseX >= pos.symbolButtonX && lastMouseX < pos.symbolButtonX + SYMBOL_BUTTON_SIZE &&
                               lastMouseY >= pos.symbolButtonY && lastMouseY < pos.symbolButtonY + SYMBOL_BUTTON_SIZE;
        
        // Prüfe Hover über Favoriten-Button
        boolean favoritesHovered = lastMouseX >= pos.favoritesButtonX && lastMouseX < pos.favoritesButtonX + SORT_DROPDOWN_HEIGHT &&
                                   lastMouseY >= pos.favoritesButtonY && lastMouseY < pos.favoritesButtonY + SORT_DROPDOWN_HEIGHT;
        
        // Prüfe Hover über Kit-Button
        boolean kitHovered = lastMouseX >= pos.kitButtonX && lastMouseX < pos.kitButtonX + KIT_BUTTON_SIZE &&
                             lastMouseY >= pos.kitButtonY && lastMouseY < pos.kitButtonY + KIT_BUTTON_SIZE;
        
        // Rendere Tooltip für Hilfe-Button
        if (helpHovered && !helpScreenOpen) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.literal("Hilfs Übersicht"));
            // Minecraft's drawTooltip passt automatisch die Position an, damit der Tooltip nicht aus dem Bildschirm rausgeht
            context.drawTooltip(client.textRenderer, tooltip, lastMouseX, lastMouseY);
        }
        
        // Rendere Tooltip für Symbol-Button
        if (symbolHovered) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.literal("Sonderzeichen"));
            // Berechne Tooltip-Position: Wenn nicht genug Platz rechts ist, verschiebe nach links
            int tooltipX = lastMouseX;
            int tooltipWidth = client.textRenderer.getWidth("Sonderzeichen") + 10; // Geschätzte Breite
            int screenWidth = client.getWindow().getScaledWidth();
            
            // Wenn Tooltip rechts rausgehen würde, verschiebe nach links
            if (tooltipX + tooltipWidth > screenWidth - 10) {
                tooltipX = screenWidth - tooltipWidth - 10;
            }
            
            context.drawTooltip(client.textRenderer, tooltip, tooltipX, lastMouseY);
        }
        
        // Rendere Tooltip für Favoriten-Button
        if (favoritesHovered) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.literal("Favoriten"));
            // Minecraft's drawTooltip passt automatisch die Position an
            context.drawTooltip(client.textRenderer, tooltip, lastMouseX, lastMouseY);
        }
        
        // Rendere Tooltip für Kit-Button
        if (kitHovered) {
            List<Text> tooltip = new ArrayList<>();
            // Hole aktuelle Kit-Auswahl
            net.felix.utilities.Town.KitFilterUtility.KitSelection kitSelection = 
                net.felix.utilities.Town.KitFilterUtility.getKitSelection(KIT_BUTTON_INDEX);
            
            if (kitSelection != null) {
                tooltip.add(Text.literal("Linksklick: Nach Kit Suchen"));
                tooltip.add(Text.literal("Rechtsklick: Kit auswählen"));
                tooltip.add(Text.empty());
                tooltip.add(Text.literal(kitSelection.getDisplayName())); // z.B. "Münz-Kit Stufe 3"
            } else {
                tooltip.add(Text.literal("Linksklick: Nach Kit Suchen"));
                tooltip.add(Text.literal("Rechtsklick: Kit auswählen"));
            }
            // Minecraft's drawTooltip passt automatisch die Position an
            context.drawTooltip(client.textRenderer, tooltip, lastMouseX, lastMouseY);
        }
    }
    
    /**
     * Rendert das Hilfe-Overlay (wird von den Mixins aufgerufen, damit es über allem liegt)
     */
    public static void renderHelpOverlay(DrawContext context) {
        if (helpScreenOpen) {
            MinecraftClient client = MinecraftClient.getInstance();
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
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Berechne boxHeight mit gleicher Logik wie beim Rendering
        int buttonHeight = 16;
        int maxButtonsPerRow = 4;
        int buttonSpacingY = 20; // Muss mit computeCategoryButtons übereinstimmen
        int numButtonRows = (int) Math.ceil((double) CATEGORY_BUTTON_DEFS.size() / maxButtonsPerRow);
        int baseHeight = 500; // Erhöht von 450 auf 500 für mehr Platz
        // Berechne benötigte Höhe für Button-Bereich (inkl. Header und Abstände)
        int headerHeight = client.textRenderer.fontHeight;
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
            int lineHeight = client.textRenderer.fontHeight + 2;
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
                    columnWidth = Math.max(columnWidth, client.textRenderer.getWidth(activeCategoryOverlay.tags.get(i)));
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
        java.util.List<CategoryButton> buttons = computeCategoryButtons(client.textRenderer, boxX, boxY, boxWidth, boxHeight, buttonY, startX);
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
    private static void drawHelpScreen(DrawContext context, MinecraftClient client) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
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
        int headerHeight = client.textRenderer.fontHeight;
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
        
        // Hilfe-Text mit Scrolling
        int textStartY = boxY + 35;
        int lineHeight = 11;
        int textX = boxX + 15;
        int maxTextWidth = boxWidth - 30;
        
        // Berechne verfügbare Höhe für Text (oberhalb der Buttons)
        int numRows = (int) Math.ceil((double) CATEGORY_BUTTON_DEFS.size() / maxButtonsPerRow);
        // headerHeight und spacing wurden bereits oben definiert
        int buttonAreaHeightForText = headerHeight + spacing + (numRows * buttonHeight) + ((numRows - 1) * buttonSpacingY);
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
            "• #Tag - Suche nach Tags (z.B. #Ring, #Rüstung)",
            "• @Stat>Wert - Suche nach Stats (z.B. @Abbaugeschwindigkeit>100)",
            "• @Ebene>Wert - Suche nach Ebenen (z.B. @Ebene>50, @Ebene<30)",
            "• +Modifier - Suche nach Modifiern (z.B. +Andere, +Andere:2)",
            "• Kosten:Anzahl - Suche nach Kosten (z.B. amboss:0)",
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
            if (client.textRenderer.getWidth(line) > maxTextWidth && !line.isEmpty()) {
                String[] words = line.split(" ");
                String currentLine = "";
                for (String word : words) {
                    String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                    if (client.textRenderer.getWidth(testLine) > maxTextWidth) {
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
                context.drawText(
                    client.textRenderer,
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
            int moreTextWidth = client.textRenderer.getWidth(moreText);
            context.drawText(
                client.textRenderer,
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
        
        // Zeige Buttons nur an, wenn bereits gescrollt wurde
        if (helpScreenScrollOffset > 0) {
            // Zeige "↓ X weitere (Scrollen)" nur wenn noch mehr Text vorhanden ist
            if (helpScreenScrollOffset < maxScrollOffset) {
                int remainingLines = allTextLines.size() - (startLineIndex + visibleLines);
                if (remainingLines > 0) {
                    String moreText = String.format("↓ %d weitere (Scrollen)", remainingLines);
                    int moreTextWidth = client.textRenderer.getWidth(moreText);
                    context.drawText(
                        client.textRenderer,
                        moreText,
                        boxX + (boxWidth - moreTextWidth) / 2,
                        buttonY - lineHeight - 2,
                        0xFFFFFF00,
                        true
                    );
                }
            }
            renderCategoryButtons(context, client.textRenderer, boxX, boxY, boxWidth, boxHeight, finalButtonY);
        } else {
            // Zeige Hinweis zum Scrollen in der untersten Zeile (nur wenn noch mehr Text vorhanden ist)
            if (maxScrollOffset > 0) {
                String scrollHint = "↓ Scrollen für Kategorien mit Tags";
                int scrollHintWidth = client.textRenderer.getWidth(scrollHint);
                int scrollHintY = buttonY - lineHeight - 2 + 15; // 15px tiefer
                context.drawText(
                    client.textRenderer,
                    scrollHint,
                    boxX + (boxWidth - scrollHintWidth) / 2,
                    scrollHintY,
                    0xFFFFFF00,
                    true
                );
            }
        }
    }

    /**
     * Rendert die Kategorie-Buttons und ein optionales Tag-Overlay
     */
    private static void renderCategoryButtons(DrawContext context, net.minecraft.client.font.TextRenderer tr,
                                              int boxX, int boxY, int boxWidth, int boxHeight, int buttonY) {
        // Berechne Position: Buttons stehen bei buttonY (bereits berechnet)
        int headerHeight = tr.fontHeight;
        int spacing = 4; // Kleiner Abstand zwischen Header und Buttons (Text direkt über Buttons)
        int headerY = buttonY - headerHeight - spacing; // Header steht direkt über den Buttons
        int startX = boxX + 15;

        context.drawText(tr, Text.literal("Kategorien mit Tags"), startX, headerY, 0xFFFFFF00, true);

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

            int textW = tr.getWidth(btn.name);
            int textX = btn.x + (btn.width - textW) / 2;
            int textY = btn.y + (btn.height - tr.fontHeight) / 2;
            context.drawText(tr, Text.literal(btn.name), textX, textY, 0xFFFFFFFF, false);
        }

        // Overlay mit Tags
        if (activeCategoryOverlay != null && activeCategoryOverlay.tags != null && !activeCategoryOverlay.tags.isEmpty()) {
            int lineHeight = tr.fontHeight + 2;
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
                    columnWidth = Math.max(columnWidth, tr.getWidth(activeCategoryOverlay.tags.get(i)));
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
                    context.drawText(tr, Text.literal(activeCategoryOverlay.tags.get(i)), columnX + 6, ty, 0xFF55FFFF, false);
                    ty += lineHeight;
                }
            }
        }
    }

    /**
     * Berechnet die Positionen der Kategorie-Buttons
     */
    private static java.util.List<CategoryButton> computeCategoryButtons(net.minecraft.client.font.TextRenderer tr,
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
            
            int btnW = tr.getWidth(def.name) + 12;
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
        }
        
        // Sortiere Tags alphabetisch
        java.util.List<String> sortedTags = new java.util.ArrayList<>(tagSet);
        sortedTags.sort(String.CASE_INSENSITIVE_ORDER);
        return sortedTags;
    }

    /**
     * Datenstruktur für Kategorie-Buttons
     */
    private static class CategoryButton {
        final String name;
        final java.util.List<String> tags;
        int x, y, width, height;

        CategoryButton(String name, java.util.List<String> tags) {
            this.name = name;
            this.tags = tags;
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
        boolean isTextTruncated = false;
        if (client.textRenderer.getWidth(displayText) > maxTextWidth) {
            // Kürze Text von hinten
            String originalText = displayText;
            while (client.textRenderer.getWidth(displayText + "...") > maxTextWidth && displayText.length() > 0) {
                displayText = displayText.substring(0, displayText.length() - 1);
            }
            displayText += "...";
            // Prüfe ob Text tatsächlich gekürzt wurde (nicht nur "..." hinzugefügt)
            isTextTruncated = !displayText.equals(originalText + "...");
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
            if (cursorPosition > 0 && cursorPosition <= currentSearch.length()) {
                cursorX += client.textRenderer.getWidth(currentSearch.substring(0, cursorPosition));
            }
            // Blinkender Cursor (einfache Version, blinkt nicht)
            context.fill(cursorX, y + 4, cursorX + 1, y + SEARCH_HEIGHT - 4, 0xFFFFFFFF);
        }
        
        // Tooltip anzeigen wenn Text abgeschnitten wurde und Maus über Suchleiste ist
        if (isTextTruncated && !currentSearch.isEmpty()) {
            boolean isHovered = lastMouseX >= x && lastMouseX < x + width &&
                               lastMouseY >= y && lastMouseY < y + SEARCH_HEIGHT;
            if (isHovered) {
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(Text.literal(currentSearch));
                // Minecraft's drawTooltip passt automatisch die Position an
                context.drawTooltip(client.textRenderer, tooltip, lastMouseX, lastMouseY);
            }
        }
    }
    
    /**
     * Rendert den Favoriten-Button
     */
    private static void renderFavoritesButton(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        
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
            context.drawTexture(
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
            int starTextWidth = client.textRenderer.getWidth(starSymbol);
            int starTextHeight = client.textRenderer.fontHeight;
            int starX = x + (SORT_DROPDOWN_HEIGHT - starTextWidth) / 2;
            int starY = y + (SORT_DROPDOWN_HEIGHT - starTextHeight) / 2;
            int starColor = favoritesMode ? 0x80FFFF00 : 0xFFFFFF00; // Gelb, 50% transparenter wenn aktiv
            context.drawText(client.textRenderer, Text.literal(starSymbol), starX, starY, starColor, false);
        }
    }
    
    /**
     * Rendert den Kit-Button
     */
    private static void renderKitButton(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Prüfe ob Maus über Button ist
        boolean isHovered = lastMouseX >= x && lastMouseX < x + KIT_BUTTON_SIZE &&
                           lastMouseY >= y && lastMouseY < y + KIT_BUTTON_SIZE;
        
        // Hole aktuelle Kit-Auswahl
        net.felix.utilities.Town.KitFilterUtility.KitSelection kitSelection = 
            net.felix.utilities.Town.KitFilterUtility.getKitSelection(KIT_BUTTON_INDEX);
        boolean hasKitSelected = kitSelection != null;
        
        // Bestimme Kit-Farbe (basierend auf Kit-Typ)
        int kitColor = getKitColor(kitSelection);
        
        // Button-Hintergrund
        int bgColor;
        if (kitFilterActive && hasKitSelected) {
            // Kit aktiv: 50% transparenter Marker
            bgColor = isHovered ? (kitColor & 0x80FFFFFF) | 0x80606060 : (kitColor & 0x80FFFFFF) | 0x80404040;
        } else {
            // Kit nicht aktiv oder nicht ausgewählt: normal
            bgColor = isHovered ? 0xFF404040 : 0xFF202020;
        }
        context.fill(x, y, x + KIT_BUTTON_SIZE, y + KIT_BUTTON_SIZE, bgColor);
        
        // Button-Rahmen
        int borderColor;
        if (hasKitSelected && !kitFilterActive) {
            // Kit ausgewählt aber nicht aktiv: farbiger Rahmen
            borderColor = kitColor;
        } else if (kitFilterActive && hasKitSelected) {
            // Kit aktiv: 50% transparenter Rahmen
            borderColor = (kitColor & 0x80FFFFFF) | 0x80000000;
        } else {
            // Kein Kit ausgewählt: normal
            borderColor = isHovered ? 0xFFFFFFFF : 0xFF808080;
        }
        context.fill(x, y, x + KIT_BUTTON_SIZE, y + 1, borderColor); // Oben
        context.fill(x, y + KIT_BUTTON_SIZE - 1, x + KIT_BUTTON_SIZE, y + KIT_BUTTON_SIZE, borderColor); // Unten
        context.fill(x, y, x + 1, y + KIT_BUTTON_SIZE, borderColor); // Links
        context.fill(x + KIT_BUTTON_SIZE - 1, y, x + KIT_BUTTON_SIZE, y + KIT_BUTTON_SIZE, borderColor); // Rechts
        
        // Rendere Kit-Icon (Textur)
        try {
            // Versuche Textur zu rendern (16x16 Pixel, zentriert im 20x20 Button)
            int iconSize = 16;
            int iconX = x + (KIT_BUTTON_SIZE - iconSize) / 2;
            int iconY = y + (KIT_BUTTON_SIZE - iconSize) / 2;
            
            // Verwende drawTexture mit RenderPipeline
            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                KIT_ICON_TEXTURE,
                iconX, iconY,
                0.0f, 0.0f,
                iconSize, iconSize,
                iconSize, iconSize
            );
        } catch (Exception e) {
            // Fallback: Zeige "K" als Text wenn Textur nicht geladen werden kann
            String kitSymbol = "K";
            int textWidth = client.textRenderer.getWidth(kitSymbol);
            int textHeight = client.textRenderer.fontHeight;
            int textX = x + (KIT_BUTTON_SIZE - textWidth) / 2;
            int textY = y + (KIT_BUTTON_SIZE - textHeight) / 2;
            context.drawText(client.textRenderer, Text.literal(kitSymbol), textX, textY, 0xFFFFFFFF, false);
        }
    }
    
    /**
     * Gibt die Farbe für ein Kit zurück (basierend auf Kit-Typ)
     */
    private static int getKitColor(net.felix.utilities.Town.KitFilterUtility.KitSelection kitSelection) {
        if (kitSelection == null) {
            return 0xFF808080; // Grau als Standard
        }
        
        switch (kitSelection.kitType) {
            case MÜNZ_KIT:
                return 0xFFFC54FC; // Helles Pink (wie modifier Andere)
            case SCHADEN_KIT:
                return 0xFFFC5454; // Rot (wie modifier Schaden)
            case RESSOURCEN_KIT:
                return 0xFF5454FC; // Blau (wie modifier Fähigkeiten)
            case HERSTELLUNGS_KIT:
                return 0xFFFCFC54; // Gelb (wie modifier Herstellung)
            case TANK_KIT:
                return 0xFFFCA800; // Orange (wie modifier Verteidigung)
            default:
                return 0xFF808080; // Grau als Fallback
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
     * Prüft ob die Maus über dem Symbol-Button ist und öffnet/schließt das Menü entsprechend
     */
    private static void checkSymbolButtonHover(int buttonX, int buttonY) {
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
    private static void renderSymbolButton(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        
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
            context.drawTexture(
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
            int exclamationTextWidth = client.textRenderer.getWidth(exclamationSymbol);
            int exclamationTextHeight = client.textRenderer.fontHeight;
            int exclamationX = x + (SYMBOL_BUTTON_SIZE - exclamationTextWidth) / 2;
            int exclamationY = y + (SYMBOL_BUTTON_SIZE - exclamationTextHeight) / 2;
            context.drawText(
                client.textRenderer,
                Text.literal(exclamationSymbol),
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
    private static void renderSymbolMenu(DrawContext context, int buttonX, int buttonY) {
        MinecraftClient client = MinecraftClient.getInstance();
        
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
        int textY = menuY + (menuButtonHeight - client.textRenderer.fontHeight) / 2;
        context.drawText(
            client.textRenderer,
            Text.literal("@"),
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
        context.drawText(
            client.textRenderer,
            Text.literal("<"),
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
        context.drawText(
            client.textRenderer,
            Text.literal(">"),
            greaterButtonX + 7,
            textY,
            0xFFFFFFFF,
            false
        );
    }
    
    /**
     * Rendert Pagination-Buttons
     */
    private static void renderPagination(DrawContext context, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Berechne Items pro Seite basierend auf aktueller Grid-Breite und -Höhe
        int slotSize = ItemViewerGrid.getSlotSize();
        int gridColumns = Math.max(1, currentGridAvailableWidth / slotSize);
        int gridRows = Math.max(1, currentGridAvailableHeight / slotSize);
        int itemsPerPage = gridColumns * gridRows;
        
        int totalPages = (int) Math.ceil((double) filteredItems.size() / (double) itemsPerPage);
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
        
        // Verwende Standard-Grid-Breite für die Platzierungsprüfung
        boolean placeRight = availableWidthRight >= ItemViewerGrid.getDefaultGridWidth() + VIEWER_PADDING * 2;
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
        // Nutze maximale verfügbare Breite, aber mindestens die minimale Breite (Standard-Grid)
        int minViewerWidth = ItemViewerGrid.getDefaultGridWidth() + VIEWER_PADDING * 2;
        int viewerWidth = Math.max(minViewerWidth, maxViewerWidth);
        
        // Berechne Dropdown-Breite
        int maxDropdownWidth = 0;
        for (SortMode mode : SortMode.values()) {
            int textWidth = client.textRenderer.getWidth(mode.getDisplayName());
            maxDropdownWidth = Math.max(maxDropdownWidth, textWidth);
        }
        int dropdownWidth = maxDropdownWidth + SORT_DROPDOWN_PADDING * 2 + 15;
        
        // Nutze volle verfügbare Höhe (von oben bis unten mit Padding)
        int viewerY = 10; // 10px Padding oben
        int viewerHeight = screen.height - 20; // 10px oben + 10px unten = 20px Padding
        
        // Hilfe-Button Position (links neben der Suchleiste)
        int helpButtonX = viewerX + VIEWER_PADDING;
        int helpButtonY = viewerY + VIEWER_PADDING;
        
        // Symbol-Button Position (oben rechts neben der Suchleiste)
        int symbolButtonX = viewerX + viewerWidth - VIEWER_PADDING - SYMBOL_BUTTON_SIZE; // Rechts bündig mit Viewer
        int symbolButtonY = viewerY + VIEWER_PADDING; // Gleiche Y-Position wie Suchleiste
        
        // Suchfeld nutzt Breite zwischen Hilfe-Button und Symbol-Button
        int searchX = helpButtonX + HELP_BUTTON_SIZE + 5; // 5px Abstand zum Hilfe-Button
        int searchWidth = symbolButtonX - searchX - 5; // 5px Abstand zum Symbol-Button
        
        // Dropdown wird unter die Suchleiste platziert
        int dropdownX = viewerX + VIEWER_PADDING;
        int dropdownY = viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5; // Unter der Suchleiste
        
        // Favoriten-Button Position (rechts vom Dropdown, gleiche Höhe)
        int favoritesButtonX = dropdownX + dropdownWidth + 5; // 5px Abstand zum Dropdown
        int favoritesButtonY = dropdownY; // Gleiche Y-Position wie Dropdown
        
        // Kit-Button Position (rechts vom Favoriten-Button, gleiche Höhe)
        int kitButtonX = favoritesButtonX + SORT_DROPDOWN_HEIGHT + 5; // 5px Abstand zum Favoriten-Button
        int kitButtonY = dropdownY; // Gleiche Y-Position wie Dropdown
        
        return new ViewerPosition(viewerX, viewerY, viewerWidth, viewerHeight, dropdownX, dropdownY, dropdownWidth, searchWidth, helpButtonX, helpButtonY, symbolButtonX, symbolButtonY, favoritesButtonX, favoritesButtonY, kitButtonX, kitButtonY);
    }
    
    /**
     * Hilfsklasse für Viewer-Position
     */
    private static class ViewerPosition {
        final int viewerX, viewerY, viewerWidth, viewerHeight;
        final int dropdownX, dropdownY, dropdownWidth, searchWidth;
        final int helpButtonX, helpButtonY;
        final int symbolButtonX, symbolButtonY;
        final int favoritesButtonX, favoritesButtonY;
        final int kitButtonX, kitButtonY;
        
        ViewerPosition(int viewerX, int viewerY, int viewerWidth, int viewerHeight,
                      int dropdownX, int dropdownY, int dropdownWidth, int searchWidth,
                      int helpButtonX, int helpButtonY, int symbolButtonX, int symbolButtonY,
                      int favoritesButtonX, int favoritesButtonY, int kitButtonX, int kitButtonY) {
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
            this.symbolButtonX = symbolButtonX;
            this.symbolButtonY = symbolButtonY;
            this.favoritesButtonX = favoritesButtonX;
            this.favoritesButtonY = favoritesButtonY;
            this.kitButtonX = kitButtonX;
            this.kitButtonY = kitButtonY;
        }
    }
    
    /**
     * Behandelt Mausrad-Scrolling (wird vom Mixin aufgerufen)
     * @param mouseX Maus X-Position
     * @param mouseY Maus Y-Position
     * @param vertical Scroll-Delta (positiv = runter, negativ = hoch)
     * @return true wenn das Scroll-Event behandelt wurde, false sonst
     */
    public static boolean handleMouseScroll(double mouseX, double mouseY, double vertical) {
        // Prüfe zuerst ob Hilfe-Overlay geöffnet ist und Maus darüber ist
        if (helpScreenOpen && helpScreenHovered) {
            int scrollAmount = (int) (vertical * 12);
            helpScreenScrollOffset -= scrollAmount;
            helpScreenScrollOffset = Math.max(0, helpScreenScrollOffset);
            return true;
        }
        
        if (!isVisible) {
            return false;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null || !(client.currentScreen instanceof HandledScreen<?> handledScreen)) {
            return false;
        }
        
        // Berechne Viewer-Position
        ViewerPosition pos = calculateViewerPosition(handledScreen, client.currentScreen);
        
        // Prüfe ob Maus über dem Viewer ist
        if (mouseX >= pos.viewerX && mouseX < pos.viewerX + pos.viewerWidth &&
            mouseY >= pos.viewerY && mouseY < pos.viewerY + pos.viewerHeight) {
            
            // Berechne Items pro Seite
            int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
            int gridAvailableWidth = pos.viewerWidth - VIEWER_PADDING * 2;
            int gridAvailableHeight = paginationY - (pos.viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5 + SORT_DROPDOWN_HEIGHT + 5);
            int slotSize = ItemViewerGrid.getSlotSize();
            int gridColumns = Math.max(1, gridAvailableWidth / slotSize);
            int gridRows = Math.max(1, gridAvailableHeight / slotSize);
            int itemsPerPage = gridColumns * gridRows;
            
            int totalPages = (int) Math.ceil((double) filteredItems.size() / (double) itemsPerPage);
            if (totalPages == 0) totalPages = 1;
            
            // Scrolle durch Seiten
            if (vertical > 0) {
                // Hoch scrollen = vorherige Seite
                if (currentPage > 0) {
                    currentPage--;
                    return true;
                }
            } else if (vertical < 0) {
                // Runter scrollen = nächste Seite
                if (currentPage < totalPages - 1) {
                    currentPage++;
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Behandelt Maus-Klicks (wird vom Mixin aufgerufen)
     */
    public static boolean handleMouseClick(double mouseX, double mouseY, int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null || !(client.currentScreen instanceof HandledScreen<?> handledScreen)) {
            return false;
        }
        
        // Prüfe ZUERST Klick auf Hilfe-Overlay Schließen-Button (höchste Priorität wenn geöffnet)
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
            // Wenn Hilfe-Overlay geöffnet ist, verhindere alle anderen Klicks
            return true;
        }
        
        // Berechne Viewer-Position (gleiche Logik wie beim Rendering)
        ViewerPosition pos = calculateViewerPosition(handledScreen, client.currentScreen);
        
        // Prüfe Klick auf Symbol-Menü (wenn geöffnet, VOR Suchleiste, nur Linksklick)
        // Priorisiere Symbol-Menü vor Suchleiste, damit Klicks auf Menü-Buttons funktionieren
        if (button == 0 && isSymbolMenuOpen) {
            int menuButtonWidth = 20;
            int menuButtonHeight = SYMBOL_BUTTON_SIZE; // Gleiche Höhe wie Stern-Button
            int menuWidth = menuButtonWidth * 3 + 2;
            int menuX = pos.symbolButtonX - menuWidth; // Links vom Button
            int menuY = pos.symbolButtonY;
            
            // @-Button im Menü (links)
            if (mouseX >= menuX && mouseX <= menuX + menuButtonWidth &&
                mouseY >= menuY && mouseY <= menuY + menuButtonHeight) {
                insertCharacter('@');
                isSymbolMenuOpen = false;
                return true;
            }
            
            // <-Button im Menü (mitte)
            if (mouseX >= menuX + menuButtonWidth + 1 && mouseX <= menuX + menuButtonWidth * 2 + 1 &&
                mouseY >= menuY && mouseY <= menuY + menuButtonHeight) {
                insertCharacter('<');
                isSymbolMenuOpen = false;
                return true;
            }
            
            // >-Button im Menü (rechts)
            if (mouseX >= menuX + (menuButtonWidth + 1) * 2 && mouseX <= menuX + menuButtonWidth * 3 + 2 &&
                mouseY >= menuY && mouseY <= menuY + menuButtonHeight) {
                insertCharacter('>');
                isSymbolMenuOpen = false;
                return true;
            }
            
            // Wenn Klick im Menü-Bereich ist (auch wenn nicht genau auf einem Button), verhindere andere Klicks
            if (mouseX >= menuX && mouseX <= pos.symbolButtonX + SYMBOL_BUTTON_SIZE &&
                mouseY >= menuY && mouseY <= menuY + menuButtonHeight) {
                // Klick im Menü-Bereich, aber nicht auf einem Button - verhindere andere Klicks
                return true;
            }
            
            // Wenn außerhalb des Menüs geklickt wird, schließe es
            if (mouseX < menuX || mouseX >= pos.symbolButtonX + SYMBOL_BUTTON_SIZE ||
                mouseY < menuY || mouseY >= menuY + menuButtonHeight) {
                isSymbolMenuOpen = false;
            }
        }
        
        // Berechne Suchfeld-Position (wird mehrfach verwendet)
        int searchX = pos.helpButtonX + HELP_BUTTON_SIZE + 5;
        int searchY = pos.viewerY + VIEWER_PADDING;
        
        // Prüfe Klick auf Suchfeld (setzt Fokus)
        if (mouseX >= searchX && mouseX < searchX + pos.searchWidth &&
            mouseY >= searchY && mouseY < searchY + SEARCH_HEIGHT) {
            if (button == 0) {
                // Linksklick: Fokus setzen und Cursor-Position berechnen
                searchFieldFocused = true;
                // Berechne Cursor-Position basierend auf Mausklick-Position
                int clickX = (int) (mouseX - searchX - 3); // 3px Padding links
                cursorPosition = calculateCursorPosition(clickX, currentSearch, client.textRenderer);
                clearSelection(); // Auswahl löschen beim Klick
                return true;
            } else if (button == 1) {
                // Rechtsklick: Text löschen
                currentSearch = "";
                cursorPosition = 0;
                clearSelection();
                applyFilters();
                return true;
            }
        }
        
        // Prüfe Rechtsklick auf gehoverten Blueprint (vor anderen Klicks)
        if (button == 1 && hoveredItemForClick != null) {
            // Prüfe ob Klick im Grid-Bereich ist
            int gridX = pos.viewerX + VIEWER_PADDING;
            int gridY = pos.dropdownY + SORT_DROPDOWN_HEIGHT + 5;
            int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
            int gridAvailableHeight = paginationY - gridY;
            int slotSize = ItemViewerGrid.getSlotSize();
            int gridAvailableWidth = pos.viewerWidth - VIEWER_PADDING * 2;
            int gridColumns = Math.max(1, gridAvailableWidth / slotSize);
            int gridRows = Math.max(1, gridAvailableHeight / slotSize);
            int gridWidth = gridColumns * slotSize;
            int gridHeight = gridRows * slotSize;
            
            if (mouseX >= gridX && mouseX < gridX + gridWidth &&
                mouseY >= gridY && mouseY < gridY + gridHeight) {
                
                // Prüfe ob es ein Blueprint ist
                if (hoveredItemForClick.info != null && 
                    Boolean.TRUE.equals(hoveredItemForClick.info.blueprint) &&
                    hoveredItemForClick.name != null && !hoveredItemForClick.name.isEmpty()) {
                    
                    // Toggle Favoriten-Status
                    boolean isFavorite = FavoriteBlueprintsManager.isFavorite(hoveredItemForClick.name);
                    if (isFavorite) {
                        // Entferne aus Favoriten
                        FavoriteBlueprintsManager.removeFavorite(hoveredItemForClick.name);
                    } else {
                        // Füge zu Favoriten hinzu
                        JsonObject jsonObj = hoveredItemForClick.jsonObject;
                        if (jsonObj == null) {
                            // Fallback: Hole aus Map
                            jsonObj = blueprintJsonMap.get(hoveredItemForClick.name);
                        }
                        if (jsonObj != null) {
                            FavoriteBlueprintsManager.addFavorite(jsonObj);
                        }
                    }
                    
                    // Aktualisiere Filter wenn im Favoriten-Modus
                    if (favoritesMode) {
                        applyFilters();
                    }
                    
                    return true;
                }
            }
        }
        
        // Prüfe Klick auf Hilfe-Button (nur Linksklick)
        if (button == 0 && mouseX >= pos.helpButtonX && mouseX < pos.helpButtonX + HELP_BUTTON_SIZE &&
            mouseY >= pos.helpButtonY && mouseY < pos.helpButtonY + HELP_BUTTON_SIZE) {
            helpScreenOpen = !helpScreenOpen;
            searchFieldFocused = false; // Fokus entfernen wenn Hilfe geöffnet wird
            return true;
        }
        
        // Prüfe Klick auf Symbol-Button (nur Linksklick)
        if (button == 0 && mouseX >= pos.symbolButtonX && mouseX < pos.symbolButtonX + SYMBOL_BUTTON_SIZE &&
            mouseY >= pos.symbolButtonY && mouseY < pos.symbolButtonY + SYMBOL_BUTTON_SIZE) {
            // Toggle Menü
            isSymbolMenuOpen = !isSymbolMenuOpen;
            return true;
        }
        
        // Prüfe Klick auf Favoriten-Button (nur Linksklick)
        if (button == 0 && mouseX >= pos.favoritesButtonX && mouseX < pos.favoritesButtonX + SORT_DROPDOWN_HEIGHT &&
            mouseY >= pos.favoritesButtonY && mouseY < pos.favoritesButtonY + SORT_DROPDOWN_HEIGHT) {
            favoritesMode = !favoritesMode;
            applyFilters(); // Aktualisiere Filter
            return true;
        }
        
        // Prüfe Klick auf Kit-Button
        if (mouseX >= pos.kitButtonX && mouseX < pos.kitButtonX + KIT_BUTTON_SIZE &&
            mouseY >= pos.kitButtonY && mouseY < pos.kitButtonY + KIT_BUTTON_SIZE) {
            if (button == 0) {
                // Linksklick: Toggle Kit-Filter (Hintergrund-Filterung)
                net.felix.utilities.Town.KitFilterUtility.KitSelection kitSelection = 
                    net.felix.utilities.Town.KitFilterUtility.getKitSelection(KIT_BUTTON_INDEX);
                if (kitSelection != null) {
                    kitFilterActive = !kitFilterActive;
                    applyFilters(); // Aktualisiere Filter
                } else {
                    // Kein Kit ausgewählt - öffne Kit-Auswahlmenü
                    net.felix.utilities.Town.KitFilterUtility.openKitSelectionScreen(KIT_BUTTON_INDEX);
                }
                return true;
            } else if (button == 1) {
                // Rechtsklick: Öffne Kit-Auswahlmenü
                net.felix.utilities.Town.KitFilterUtility.openKitSelectionScreen(KIT_BUTTON_INDEX);
                return true;
            }
        }
        
        // Prüfe Klick auf Dropdown-Button (nur Linksklick)
        if (button == 0 && mouseX >= pos.dropdownX && mouseX < pos.dropdownX + pos.dropdownWidth &&
            mouseY >= pos.dropdownY && mouseY < pos.dropdownY + SORT_DROPDOWN_HEIGHT) {
            sortDropdownOpen = !sortDropdownOpen;
            return true;
        }
        
        // Prüfe Klick auf Dropdown-Optionen (wenn geöffnet, nur Linksklick)
        if (button == 0 && sortDropdownOpen) {
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
        
        // Prüfe Klick auf Pagination-Buttons (nur Linksklick)
        if (button == 0) {
            int paginationY = pos.viewerY + pos.viewerHeight - PAGINATION_HEIGHT - VIEWER_PADDING;
            int paginationX = pos.viewerX + VIEWER_PADDING;
            int paginationWidth = pos.viewerWidth - VIEWER_PADDING * 2;
            
            // Berechne Items pro Seite basierend auf aktueller Grid-Breite und -Höhe
            int gridAvailableWidth = pos.viewerWidth - VIEWER_PADDING * 2;
            int gridAvailableHeight = paginationY - (pos.viewerY + VIEWER_PADDING + SEARCH_HEIGHT + 5 + SORT_DROPDOWN_HEIGHT + 5);
            int slotSize = ItemViewerGrid.getSlotSize();
            int gridColumns = Math.max(1, gridAvailableWidth / slotSize);
            int gridRows = Math.max(1, gridAvailableHeight / slotSize);
            int itemsPerPage = gridColumns * gridRows;
            
            int totalPages = (int) Math.ceil((double) filteredItems.size() / (double) itemsPerPage);
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
        }
        
        // Wenn Item Viewer sichtbar ist und Suchfeld fokussiert ist,
        // aber Klick außerhalb des Suchfeldes -> Fokus entfernen
        if (isVisible && searchFieldFocused) {
            // Prüfe ob Klick außerhalb des Suchfeldes ist
            if (mouseX < searchX || mouseX >= searchX + pos.searchWidth ||
                mouseY < searchY || mouseY >= searchY + SEARCH_HEIGHT) {
                searchFieldFocused = false;
                // Kein return true, damit andere Klicks weiterhin funktionieren
            }
        }
        
        return false;
    }
    
    private static List<ItemData> getCurrentPageItems() {
        // Berechne Items pro Seite basierend auf aktueller Grid-Breite und -Höhe
        int slotSize = ItemViewerGrid.getSlotSize();
        int gridColumns = Math.max(1, currentGridAvailableWidth / slotSize);
        int gridRows = Math.max(1, currentGridAvailableHeight / slotSize);
        int itemsPerPage = gridColumns * gridRows;
        
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filteredItems.size());
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
        List<ItemData> itemsToFilter = allItems;
        
        // Wenn Favoriten-Modus aktiv ist, filtere nur Favoriten-Blueprints
        if (favoritesMode) {
            Set<String> favoriteNames = FavoriteBlueprintsManager.getFavoriteNames();
            itemsToFilter = allItems.stream()
                .filter(item -> item.info != null && 
                               Boolean.TRUE.equals(item.info.blueprint) &&
                               item.name != null && 
                               favoriteNames.contains(item.name))
                .collect(Collectors.toList());
        }
        
        // Wenn Kit-Filter aktiv ist, filtere nach Kit-Items (Hintergrund-Filterung, ohne Suchleiste zu füllen)
        if (kitFilterActive) {
            net.felix.utilities.Town.KitFilterUtility.KitSelection kitSelection = 
                net.felix.utilities.Town.KitFilterUtility.getKitSelection(KIT_BUTTON_INDEX);
            if (kitSelection != null) {
                // Hole Kit-Item-Namen
                java.util.Set<String> kitItemNames = net.felix.utilities.Town.KitFilterUtility.getKitItemNames(
                    kitSelection.kitType, kitSelection.level);
                
                if (!kitItemNames.isEmpty()) {
                    // Filtere Items: Nur Items die in der Kit-Liste sind
                    itemsToFilter = itemsToFilter.stream()
                        .filter(item -> {
                            if (item.name == null || item.name.isEmpty()) {
                                return false;
                            }
                            // Entferne Formatierungs-Codes für Vergleich
                            String cleanItemName = item.name.replaceAll("§[0-9a-fk-or]", "");
                            // Prüfe ob der Item-Name mit einem der Kit-Item-Namen übereinstimmt
                            for (String kitItemName : kitItemNames) {
                                String cleanKitItemName = kitItemName.replaceAll("§[0-9a-fk-or]", "");
                                if (cleanItemName.equalsIgnoreCase(cleanKitItemName) || 
                                    cleanItemName.contains(cleanKitItemName) ||
                                    cleanKitItemName.contains(cleanItemName)) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .collect(Collectors.toList());
                } else {
                    // Keine Kit-Items gefunden - zeige nichts
                    itemsToFilter = new ArrayList<>();
                }
            } else {
                // Kein Kit ausgewählt - deaktiviere Filter
                kitFilterActive = false;
            }
        }
        
        // Wende Suchfilter an
        if (currentSearch == null || currentSearch.trim().isEmpty()) {
            filteredItems = new ArrayList<>(itemsToFilter);
        } else {
            SearchQuery query = ItemSearchParser.parse(currentSearch);
            filteredItems = itemsToFilter.stream()
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


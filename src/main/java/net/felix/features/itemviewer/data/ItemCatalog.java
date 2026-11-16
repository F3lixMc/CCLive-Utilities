package net.felix.features.itemviewer.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.felix.features.itemviewer.ui.ItemStackCache;
import net.minecraft.world.item.ItemStack;

import static net.felix.features.itemviewer.data.ItemsConfigUpdater.ITEMS_CONFIG_FILE;
import static net.felix.features.itemviewer.data.ItemsConfigUpdater.getLocalItemsPath;
import static net.felix.features.itemviewer.inventory.FoundStatus.blueprintJsonMap;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.filteredItems;
import static net.felix.features.itemviewer.search.ItemFilterPipeline.scheduleApplyFiltersAfterLoad;
import static net.felix.features.itemviewer.search.SearchSuggestions.knownCostItemNames;
import static net.felix.features.itemviewer.search.SearchSuggestions.knownMaterialNames;
import static net.felix.features.itemviewer.search.SearchSuggestions.knownMaterialPickerEntries;
import static net.felix.features.itemviewer.search.SearchSuggestions.knownResourceNames;
import static net.felix.features.itemviewer.search.SearchSuggestions.knownResourcePickerEntries;
import static net.felix.features.itemviewer.search.SearchSuggestions.knownSearchTags;
import static net.felix.features.itemviewer.search.SearchSuggestions.rebuildKnownCostItemNames;
import static net.felix.features.itemviewer.search.SearchSuggestions.rebuildKnownSearchTags;

/**
 * Katalog aller Items: laedt items.json, baut den Namensindex auf und sucht Items nach Name.
 */
public final class ItemCatalog {

    private ItemCatalog() {
    }

    public static List<ItemData> allItems = new ArrayList<>();

    static final Map<String, ItemData> itemByName = new HashMap<>();

    // Flag für asynchrones Laden
    public static volatile boolean itemsLoaded = false;

    private static Thread loadItemsThread = null;

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Lädt die Items-Datei asynchron im Hintergrund
     */
    public static void loadItemsAsync() {
        if (loadItemsThread != null && loadItemsThread.isAlive()) {
            return; // Bereits am Laden
        }
        
        loadItemsThread = new Thread(() -> {
            try {
                loadItems();
                itemsLoaded = true;
                scheduleApplyFiltersAfterLoad();
                System.out.println("[ItemViewer] Items erfolgreich geladen (" + allItems.size() + " Items)");
            } catch (Exception e) {
                System.err.println("Fehler beim asynchronen Laden der Items: " + e.getMessage());
                e.printStackTrace();
                itemsLoaded = true; // Setze Flag trotzdem, um Endlosschleifen zu vermeiden
                scheduleApplyFiltersAfterLoad();
            }
        }, "CCLive-ItemViewer-Loader");
        loadItemsThread.setDaemon(true); // Thread wird beendet, wenn Hauptthread beendet wird
        loadItemsThread.start();
    }

    /**
     * Prüft ob die Items geladen sind
     */
    public static boolean areItemsLoaded() {
        return itemsLoaded;
    }

    public static void loadItems() {
        try {
            // Versuche zuerst lokale Datei zu laden
            java.nio.file.Path localFile = getLocalItemsPath();
            if (localFile != null && java.nio.file.Files.exists(localFile)) {
                try {
                    loadFromFile(localFile);
                    return;
                } catch (Exception e) {
                    System.err.println("⚠️ [ItemViewer] Fehler beim Laden der lokalen items.json: " + e.getMessage());
                }
            }
            
            // Fallback: Lade aus Mod-Ressourcen
            var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
                .orElseThrow(() -> new RuntimeException("Mod container not found"))
                .findPath(ITEMS_CONFIG_FILE)
                .orElseThrow(() -> new RuntimeException("Items config file not found"));
            
            loadFromFile(resource);
        } catch (Exception e) {
            // Erstelle leere Liste als Fallback
            allItems = new ArrayList<>();
            filteredItems = new ArrayList<>();
            knownSearchTags = null;
            knownCostItemNames = null;
            knownMaterialNames = null;
            knownResourceNames = null;
            knownMaterialPickerEntries = null;
            knownResourcePickerEntries = null;
        }
    }

    /**
     * Lädt Items aus einer Datei (lokal oder Mod-Ressource)
     */
    public static void loadFromFile(java.nio.file.Path file) throws Exception {
        try (var inputStream = java.nio.file.Files.newInputStream(file)) {
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
                    knownSearchTags = null;
            knownCostItemNames = null;
            knownMaterialNames = null;
            knownResourceNames = null;
            knownMaterialPickerEntries = null;
            knownResourcePickerEntries = null;
                    
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
                    if (data.fishing_components != null && !data.fishing_components.isEmpty()) {
                        if (rootJson.has("fishing_components") && rootJson.get("fishing_components").isJsonArray()) {
                            var fishingComponentsArray = rootJson.getAsJsonArray("fishing_components");
                            for (int i = 0; i < fishingComponentsArray.size() && i < data.fishing_components.size(); i++) {
                                JsonElement element = fishingComponentsArray.get(i);
                                if (element.isJsonObject()) {
                                    JsonObject jsonObj = element.getAsJsonObject();
                                    ItemData item = data.fishing_components.get(i);
                                    item.jsonObject = jsonObj;
                                    item.category = "fishing_components";
                                }
                            }
                        } else {
                            for (ItemData item : data.fishing_components) {
                                item.category = "fishing_components";
                            }
                        }
                        allItems.addAll(data.fishing_components);
                    }
                    if (data.fish_traps != null && !data.fish_traps.isEmpty()) {
                        if (rootJson.has("fish_traps") && rootJson.get("fish_traps").isJsonArray()) {
                            var fishTrapsArray = rootJson.getAsJsonArray("fish_traps");
                            for (int i = 0; i < fishTrapsArray.size() && i < data.fish_traps.size(); i++) {
                                JsonElement element = fishTrapsArray.get(i);
                                if (element.isJsonObject()) {
                                    JsonObject jsonObj = element.getAsJsonObject();
                                    ItemData item = data.fish_traps.get(i);
                                    item.jsonObject = jsonObj;
                                    item.category = "fish_traps";
                                    if (item.name != null && !item.name.isEmpty()) {
                                        blueprintJsonMap.put(item.name, jsonObj);
                                    }
                                }
                            }
                        } else {
                            for (ItemData item : data.fish_traps) {
                                item.category = "fish_traps";
                            }
                        }
                        allItems.addAll(data.fish_traps);
                    }
                    if (data.module_bags != null && !data.module_bags.isEmpty()) {
                        for (ItemData item : data.module_bags) {
                            item.category = "module_bags";
                        }
                        allItems.addAll(data.module_bags);
                    }
                    if (data.card_slots != null && !data.card_slots.isEmpty()) {
                        // Extrahiere JSON-Objekte für Card Slots
                        if (rootJson.has("card_slots") && rootJson.get("card_slots").isJsonArray()) {
                            var cardSlotArray = rootJson.getAsJsonArray("card_slots");
                            for (int i = 0; i < cardSlotArray.size() && i < data.card_slots.size(); i++) {
                                JsonElement element = cardSlotArray.get(i);
                                if (element.isJsonObject()) {
                                    JsonObject jsonObj = element.getAsJsonObject();
                                    ItemData item = data.card_slots.get(i);
                                    item.jsonObject = jsonObj;
                                    item.category = "card_slots";
                                }
                            }
                        } else {
                            // Fallback: Keine JSON-Objekte verfügbar
                            for (ItemData item : data.card_slots) {
                                item.category = "card_slots";
                            }
                        }
                        allItems.addAll(data.card_slots);
                    }
                    if (data.licence != null && !data.licence.isEmpty()) {
                        // Extrahiere JSON-Objekte für Lizenzen
                        if (rootJson.has("licence") && rootJson.get("licence").isJsonArray()) {
                            var licenceArray = rootJson.getAsJsonArray("licence");
                            for (int i = 0; i < licenceArray.size() && i < data.licence.size(); i++) {
                                JsonElement element = licenceArray.get(i);
                                if (element.isJsonObject()) {
                                    JsonObject jsonObj = element.getAsJsonObject();
                                    ItemData item = data.licence.get(i);
                                    item.jsonObject = jsonObj;
                                    item.category = "licence";
                                }
                            }
                        } else {
                            // Fallback: Keine JSON-Objekte verfügbar
                            for (ItemData item : data.licence) {
                                item.category = "licence";
                            }
                        }
                        allItems.addAll(data.licence);
                    }
                    
                    if (!allItems.isEmpty()) {
                        rebuildItemNameIndex();
                        rebuildKnownSearchTags();
                        rebuildKnownCostItemNames();
                        ItemStackCache.clearItemStackCache();
                    }
                }
            }
        }
    }

    /**
     * Prüft ob Items geladen sind und wartet ggf. kurz
     */
    public static void ensureItemsLoaded() {
        if (!itemsLoaded) {
            // Warte maximal 100ms (10 Checks à 10ms)
            int maxWait = 10;
            int waited = 0;
            while (!itemsLoaded && waited < maxWait) {
                try {
                    Thread.sleep(10);
                    waited++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Findet ein Item nach Namen in der items.json
     * @param itemName Name des Items
     * @return ItemData oder null wenn nicht gefunden
     */
    public static ItemData findItemByName(String itemName) {
        ensureItemsLoaded();
        if (itemName == null || itemName.isEmpty()) {
            return null;
        }
        
        if (allItems == null) {
            return null;
        }
        
        ItemData indexed = itemByName.get(itemName);
        if (indexed != null) {
            return indexed;
        }
        for (ItemData item : allItems) {
            if (item.name != null && item.name.equals(itemName)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Findet ein Item per exaktem oder normalisiertem Namen (ohne Formatierungscodes).
     */
    public static ItemStack createDisplayStackForItemName(String itemName) {
        ItemData itemData = findItemByNameFuzzy(itemName);
        if (itemData == null) {
            return ItemStack.EMPTY;
        }
        return ItemStackCache.createDisplayStack(itemData);
    }

    public static ItemData findItemByNameFuzzy(String itemName) {
        ItemData exact = findItemByName(itemName);
        if (exact != null) {
            return exact;
        }
        ensureItemsLoaded();
        if (allItems == null || itemName == null || itemName.isEmpty()) {
            return null;
        }
        String cleanSearch = normalizeItemNameForLookup(itemName);
        for (ItemData item : allItems) {
            if (item.name != null && normalizeItemNameForLookup(item.name).equalsIgnoreCase(cleanSearch)) {
                return item;
            }
        }
        return null;
    }

    private static String normalizeItemNameForLookup(String name) {
        return name.replaceAll("§[0-9a-fk-or]", "").trim();
    }

    private static void rebuildItemNameIndex() {
        itemByName.clear();
        for (ItemData item : allItems) {
            if (item.name != null && !item.name.isEmpty()) {
                itemByName.putIfAbsent(item.name, item);
            }
        }
    }

    /**
     * Findet ein Item nach Namen und clipboard_id in der items.json
     * @param itemName Name des Items
     * @param clipboardId Interne clipboard_id (optional)
     * @return ItemData oder null wenn nicht gefunden
     */
    public static ItemData findItemByNameAndClipboardId(String itemName, Integer clipboardId) {
        ensureItemsLoaded();
        if (itemName == null || itemName.isEmpty()) {
            return null;
        }
        
        if (allItems == null) {
            return null;
        }
        
        for (ItemData item : allItems) {
            if (item.name != null && item.name.equals(itemName)) {
                // Wenn clipboardId angegeben ist, muss sie übereinstimmen
                if (clipboardId != null) {
                    if (item.clipboard_id != null && item.clipboard_id.equals(clipboardId)) {
                        return item;
                    }
                } else {
                    // Wenn keine clipboardId angegeben, nimm das erste ohne clipboard_id
                    if (item.clipboard_id == null) {
                        return item;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Findet ein Item nach Namen und foundAt (Floor) in der items.json
     * @param itemName Name des Items
     * @param foundAtFloor Floor-Information aus foundAt (optional)
     * @return ItemData oder null wenn nicht gefunden
     */
    public static ItemData findItemByNameAndFoundAt(String itemName, String foundAtFloor) {
        ensureItemsLoaded();
        if (itemName == null || itemName.isEmpty()) {
            return null;
        }
        
        if (allItems == null) {
            return null;
        }
        
        // Wenn foundAtFloor nicht angegeben ist, verwende normale Suche
        if (foundAtFloor == null || foundAtFloor.isEmpty()) {
            return findItemByName(itemName);
        }
        
        // Suche nach Name + foundAt
        for (ItemData item : allItems) {
            if (item.name != null && item.name.equals(itemName)) {
                // Prüfe ob foundAt übereinstimmt
                if (item.foundAt != null && !item.foundAt.isEmpty()) {
                    for (LocationData location : item.foundAt) {
                        if (location.floor != null && location.floor.equals(foundAtFloor)) {
                            return item;
                        }
                    }
                }
            }
        }
        
        return null;
    }
}

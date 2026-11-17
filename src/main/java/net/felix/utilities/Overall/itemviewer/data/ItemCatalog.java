package net.felix.utilities.Overall.itemviewer.data;

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
import net.felix.utilities.Overall.itemviewer.ui.ItemStackCache;
import net.minecraft.world.item.ItemStack;

import static net.felix.utilities.Overall.itemviewer.data.ItemsConfigUpdater.ITEMS_CONFIG_FILE;
import static net.felix.utilities.Overall.itemviewer.data.ItemsConfigUpdater.getLocalItemsPath;
import static net.felix.utilities.Overall.itemviewer.inventory.FoundStatus.blueprintJsonMap;
import static net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline.filteredItems;
import static net.felix.utilities.Overall.itemviewer.search.ItemFilterPipeline.scheduleApplyFiltersAfterLoad;
import static net.felix.utilities.Overall.itemviewer.search.SearchSuggestions.knownCostItemNames;
import static net.felix.utilities.Overall.itemviewer.search.SearchSuggestions.knownMaterialNames;
import static net.felix.utilities.Overall.itemviewer.search.SearchSuggestions.knownMaterialPickerEntries;
import static net.felix.utilities.Overall.itemviewer.search.SearchSuggestions.knownResourceNames;
import static net.felix.utilities.Overall.itemviewer.search.SearchSuggestions.knownResourcePickerEntries;
import static net.felix.utilities.Overall.itemviewer.search.SearchSuggestions.knownSearchTags;
import static net.felix.utilities.Overall.itemviewer.search.SearchSuggestions.rebuildKnownCostItemNames;
import static net.felix.utilities.Overall.itemviewer.search.SearchSuggestions.rebuildKnownSearchTags;

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
            clearKnownSuggestions();
        }
    }

    /**
     * Lädt Items aus einer Datei (lokal oder Mod-Ressource)
     */
    public static void loadFromFile(java.nio.file.Path file) throws Exception {
        try (var inputStream = java.nio.file.Files.newInputStream(file)) {
            // Die Datei wird zweimal geparst: als JsonObject (vollständige JSON-Objekte) und als ItemListData
            String jsonContent = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            JsonObject rootJson = JsonParser.parseString(jsonContent).getAsJsonObject();

            ItemListData data;
            try (var reader = new java.io.StringReader(jsonContent)) {
                Type type = new TypeToken<ItemListData>(){}.getType();
                data = gson.fromJson(reader, type);
            }
            if (data == null) {
                return;
            }

            allItems = new ArrayList<>();
            clearKnownSuggestions();
            collectAllItems(data, rootJson);

            if (!allItems.isEmpty()) {
                rebuildItemNameIndex();
                rebuildKnownSearchTags();
                rebuildKnownCostItemNames();
                ItemStackCache.clearItemStackCache();
            }
        }
    }

    private static void clearKnownSuggestions() {
        knownSearchTags = null;
        knownCostItemNames = null;
        knownMaterialNames = null;
        knownResourceNames = null;
        knownMaterialPickerEntries = null;
        knownResourcePickerEntries = null;
    }

    /**
     * Sammelt die Items aller unterstützten Strukturen in {@link #allItems}; die Kategorien werden dabei
     * auf englische Namen normalisiert.
     * Unterstützt: alte Struktur ({"items": [...]}), deutsche Struktur ({"bauplan", "fähigkeiten", "module"})
     * und englische Struktur ({"blueprints", "abilities", "modules", ...}).
     */
    private static void collectAllItems(ItemListData data, JsonObject rootJson) {
        // 1. Alte Struktur
        addItems(data.items, "items");

        // 2. Neue Struktur (Deutsch)
        addItemsWithJson(rootJson, "bauplan", data.bauplan, "blueprints", true);
        addItems(data.fähigkeiten, "abilities");
        addItems(data.module, "modules");

        // 3. Neue Struktur (Englisch)
        addItemsWithJson(rootJson, "blueprints", data.blueprints, "blueprints", true);
        addItems(data.abilities, "abilities");
        addItems(data.modules, "modules");
        addItems(data.runes, "runes");
        addItems(data.power_crystals, "power_crystals");
        addItems(data.power_crystal_slots, "power_crystal_slots");
        addItems(data.essences, "essences");
        addItemsWithJson(rootJson, "fishing_components", data.fishing_components, "fishing_components", false);
        addItemsWithJson(rootJson, "fish_traps", data.fish_traps, "fish_traps", true);
        addItems(data.module_bags, "module_bags");
        addItemsWithJson(rootJson, "card_slots", data.card_slots, "card_slots", false);
        addItemsWithJson(rootJson, "licence", data.licence, "licence", false);
    }

    /** Setzt die Kategorie und hängt die Items an, falls die Liste vorhanden und nicht leer ist. */
    private static void addItems(List<ItemData> items, String category) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (ItemData item : items) {
            item.category = category;
        }
        allItems.addAll(items);
    }

    /**
     * Wie {@link #addItems}, hängt aber zusätzlich das zugehörige JSON-Objekt (gleicher Index im Array
     * {@code jsonKey} der Datei) an jedes Item. Ohne passendes JSON-Array bekommen die Items nur die Kategorie.
     *
     * @param registerInBlueprintMap legt benannte Items in {@code blueprintJsonMap} ab (für Favoriten)
     */
    private static void addItemsWithJson(JsonObject rootJson, String jsonKey, List<ItemData> items, String category,
            boolean registerInBlueprintMap) {
        if (items == null || items.isEmpty()) {
            return;
        }
        if (rootJson.has(jsonKey) && rootJson.get(jsonKey).isJsonArray()) {
            var jsonArray = rootJson.getAsJsonArray(jsonKey);
            for (int i = 0; i < jsonArray.size() && i < items.size(); i++) {
                JsonElement element = jsonArray.get(i);
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject jsonObj = element.getAsJsonObject();
                ItemData item = items.get(i);
                item.jsonObject = jsonObj;
                item.category = category;
                if (registerInBlueprintMap && item.name != null && !item.name.isEmpty()) {
                    blueprintJsonMap.put(item.name, jsonObj);
                }
            }
        } else {
            // Fallback: Keine JSON-Objekte verfügbar
            for (ItemData item : items) {
                item.category = category;
            }
        }
        allItems.addAll(items);
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

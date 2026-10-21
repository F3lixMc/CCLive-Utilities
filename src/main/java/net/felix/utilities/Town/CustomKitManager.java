package net.felix.utilities.Town;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Speichert und lädt benutzerdefinierte Kits global für alle Kit-Filter-Buttons.
 */
public final class CustomKitManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "custom_kits.json";

    private static final List<CustomKit> kits = new ArrayList<>();
    private static boolean loaded = false;

    private CustomKitManager() {
    }

    public static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    public static List<CustomKit> getKitsForButton(int buttonIndex) {
        ensureLoaded();
        return new ArrayList<>(kits);
    }

    public static List<CustomKit> getAllKits() {
        ensureLoaded();
        return new ArrayList<>(kits);
    }

    public static CustomKit getKit(String kitId) {
        if (kitId == null || kitId.isEmpty()) {
            return null;
        }
        ensureLoaded();
        for (CustomKit kit : kits) {
            if (kitId.equals(kit.id)) {
                return kit;
            }
        }
        return null;
    }

    /** @return true wenn das Kit hinzugefügt und dauerhaft gespeichert wurde */
    public static boolean addKit(int buttonIndex, CustomKit kit) {
        return addKit(kit);
    }

    /** @return true wenn das Kit hinzugefügt und dauerhaft gespeichert wurde */
    public static boolean addKit(CustomKit kit) {
        if (kit == null) {
            return false;
        }
        ensureLoaded();
        if (kit.cardNames == null) {
            kit.cardNames = new ArrayList<>();
        }
        if (kit.itemNames == null) {
            kit.itemNames = new ArrayList<>();
        }
        if (kit.fishingComponents == null) {
            kit.fishingComponents = new ArrayList<>();
        }
        if (kit.itemCategories == null) {
            kit.itemCategories = new java.util.HashMap<>();
        }
        kits.add(kit);
        if (!save()) {
            kits.remove(kit);
            return false;
        }
        return true;
    }

    /** @return true wenn das Kit aktualisiert und dauerhaft gespeichert wurde */
    public static boolean updateKit(CustomKit kit) {
        if (kit == null || kit.id == null) {
            return false;
        }
        ensureLoaded();
        if (kit.cardNames == null) {
            kit.cardNames = new ArrayList<>();
        }
        if (kit.itemNames == null) {
            kit.itemNames = new ArrayList<>();
        }
        if (kit.fishingComponents == null) {
            kit.fishingComponents = new ArrayList<>();
        }
        if (kit.itemCategories == null) {
            kit.itemCategories = new java.util.HashMap<>();
        }
        for (int i = 0; i < kits.size(); i++) {
            if (kit.id.equals(kits.get(i).id)) {
                CustomKit previous = kits.get(i);
                kits.set(i, kit);
                if (!save()) {
                    kits.set(i, previous);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public static boolean deleteKit(String kitId) {
        if (kitId == null || kitId.isEmpty()) {
            return false;
        }
        ensureLoaded();
        CustomKit removed = null;
        for (CustomKit kit : kits) {
            if (kitId.equals(kit.id)) {
                removed = kit;
                break;
            }
        }
        if (removed == null) {
            return false;
        }
        kits.remove(removed);
        if (!save()) {
            kits.add(removed);
            return false;
        }
        return true;
    }

    private static Path getSavePath() {
        return net.felix.CCLiveUtilities.getConfigDir()
                .resolve("cclive-utilities")
                .resolve(FILE_NAME);
    }

    private static void load() {
        kits.clear();
        try {
            Path path = getSavePath();
            if (!Files.exists(path)) {
                loaded = true;
                return;
            }
            String json = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (root.has("kits") && root.get("kits").isJsonArray()) {
                loadKitsFromArray(root.getAsJsonArray("kits"));
            } else if (root.has("buttonKits") && root.get("buttonKits").isJsonObject()) {
                loadKitsFromLegacyButtonFormat(root.getAsJsonObject("buttonKits"));
            }
        } catch (Exception e) {
            // Silent error handling on load
        }
        loaded = true;
    }

    private static void loadKitsFromArray(JsonArray arr) {
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                continue;
            }
            CustomKit kit = parseKit(el.getAsJsonObject());
            if (kit != null) {
                kits.add(kit);
            }
        }
    }

    private static void loadKitsFromLegacyButtonFormat(JsonObject buttonKits) {
        Set<String> seenIds = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : buttonKits.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            for (JsonElement el : entry.getValue().getAsJsonArray()) {
                if (!el.isJsonObject()) {
                    continue;
                }
                CustomKit kit = parseKit(el.getAsJsonObject());
                if (kit != null && kit.id != null && seenIds.add(kit.id)) {
                    kits.add(kit);
                }
            }
        }
    }

    private static CustomKit parseKit(JsonObject obj) {
        String id = obj.has("id") ? obj.get("id").getAsString() : null;
        String name = obj.has("name") ? obj.get("name").getAsString() : "Neues Kit";
        String icon = obj.has("icon") ? obj.get("icon").getAsString() : "minecraft:gold_nugget";
        String iconName = obj.has("iconName") ? obj.get("iconName").getAsString() : null;
        List<String> items = new ArrayList<>();
        if (obj.has("items") && obj.get("items").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("items")) {
                if (el.isJsonPrimitive()) {
                    items.add(el.getAsString());
                }
            }
        }
        List<String> cards = new ArrayList<>();
        if (obj.has("cards") && obj.get("cards").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("cards")) {
                if (el.isJsonPrimitive()) {
                    cards.add(el.getAsString());
                }
            }
        }
        List<CustomKit.FishingComponent> fishing = new ArrayList<>();
        if (obj.has("fishingComponents") && obj.get("fishingComponents").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("fishingComponents")) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject fc = el.getAsJsonObject();
                String type = fc.has("type") ? fc.get("type").getAsString() : null;
                String itemName = fc.has("itemName") ? fc.get("itemName").getAsString() : null;
                if (itemName != null && !itemName.isEmpty()) {
                    fishing.add(new CustomKit.FishingComponent(type, itemName));
                }
            }
        }
        CustomKit kit = new CustomKit(id, name, icon, iconName, items, cards, fishing);
        if (obj.has("itemCategories") && obj.get("itemCategories").isJsonObject()) {
            JsonObject cats = obj.getAsJsonObject("itemCategories");
            for (Map.Entry<String, JsonElement> e : cats.entrySet()) {
                if (e.getValue().isJsonPrimitive()) {
                    kit.itemCategories.put(e.getKey(), e.getValue().getAsString());
                }
            }
        }
        return kit;
    }

    /** @return true wenn die Datei erfolgreich geschrieben wurde */
    private static boolean save() {
        try {
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (CustomKit kit : kits) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", kit.id);
                obj.addProperty("name", kit.name);
                obj.addProperty("icon", kit.iconItemId);
                if (kit.iconItemName != null && !kit.iconItemName.isEmpty()) {
                    obj.addProperty("iconName", kit.iconItemName);
                }
                JsonArray items = new JsonArray();
                if (kit.itemNames != null) {
                    for (String itemName : kit.itemNames) {
                        items.add(itemName);
                    }
                }
                obj.add("items", items);
                JsonArray cards = new JsonArray();
                if (kit.cardNames != null) {
                    for (String cardName : kit.cardNames) {
                        cards.add(cardName);
                    }
                }
                obj.add("cards", cards);
                JsonArray fishing = new JsonArray();
                if (kit.fishingComponents != null) {
                    for (CustomKit.FishingComponent fc : kit.fishingComponents) {
                        if (fc == null || fc.itemName == null || fc.itemName.isEmpty()) {
                            continue;
                        }
                        JsonObject entry = new JsonObject();
                        if (fc.type != null && !fc.type.isEmpty()) {
                            entry.addProperty("type", fc.type);
                        }
                        entry.addProperty("itemName", fc.itemName);
                        fishing.add(entry);
                    }
                }
                obj.add("fishingComponents", fishing);
                if (kit.itemCategories != null && !kit.itemCategories.isEmpty()) {
                    JsonObject cats = new JsonObject();
                    for (Map.Entry<String, String> e : kit.itemCategories.entrySet()) {
                        if (e.getKey() != null && e.getValue() != null) {
                            cats.addProperty(e.getKey(), e.getValue());
                        }
                    }
                    obj.add("itemCategories", cats);
                }
                arr.add(obj);
            }
            root.add("kits", arr);
            Path path = getSavePath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

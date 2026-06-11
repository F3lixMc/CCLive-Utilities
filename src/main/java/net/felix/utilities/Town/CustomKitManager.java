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

    public static void addKit(int buttonIndex, CustomKit kit) {
        addKit(kit);
    }

    public static void addKit(CustomKit kit) {
        if (kit == null) {
            return;
        }
        ensureLoaded();
        kits.add(kit);
        save();
    }

    public static void updateKit(CustomKit kit) {
        if (kit == null || kit.id == null) {
            return;
        }
        ensureLoaded();
        for (int i = 0; i < kits.size(); i++) {
            if (kit.id.equals(kits.get(i).id)) {
                kits.set(i, kit);
                save();
                return;
            }
        }
    }

    public static boolean deleteKit(String kitId) {
        if (kitId == null || kitId.isEmpty()) {
            return false;
        }
        ensureLoaded();
        if (kits.removeIf(kit -> kitId.equals(kit.id))) {
            save();
            return true;
        }
        return false;
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
            // Silent error handling
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
        return new CustomKit(id, name, icon, iconName, items);
    }

    private static void save() {
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
                for (String itemName : kit.itemNames) {
                    items.add(itemName);
                }
                obj.add("items", items);
                arr.add(obj);
            }
            root.add("kits", arr);
            Path path = getSavePath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Silent error handling
        }
    }
}

package net.felix.utilities.Town;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.felix.chat.ChatTooltipLine;
import net.felix.utilities.ItemViewer.ItemData;

import java.util.ArrayList;
import java.util.List;

/**
 * Kompakte Persistenz für Original-Share-Items (Modell, CMD, Tooltip).
 */
public final class KitItemDataIo {

    private KitItemDataIo() {
    }

    public static ItemData copyShareItem(ItemData source) {
        if (source == null) {
            return null;
        }
        ItemData copy = new ItemData();
        copy.id = source.id;
        copy.name = source.name;
        copy.customModelData = source.customModelData;
        copy.customModelStrings = source.customModelStrings == null ? null : new ArrayList<>(source.customModelStrings);
        copy.customModelFlags = source.customModelFlags == null ? null : new ArrayList<>(source.customModelFlags);
        copy.customModelColors = source.customModelColors == null ? null : new ArrayList<>(source.customModelColors);
        copy.category = source.category;
        copy.texture = source.texture;
        if (source.chatTooltip != null) {
            copy.chatTooltip = new ArrayList<>(source.chatTooltip);
        }
        return copy;
    }

    public static List<ItemData> copyShareItems(List<ItemData> source) {
        List<ItemData> out = new ArrayList<>();
        if (source == null) {
            return out;
        }
        for (ItemData item : source) {
            ItemData copy = copyShareItem(item);
            if (copy != null) {
                out.add(copy);
            }
        }
        return out;
    }

    public static JsonObject toJson(ItemData item) {
        if (item == null) {
            return null;
        }
        JsonObject obj = new JsonObject();
        if (item.id != null) {
            obj.addProperty("id", item.id);
        }
        if (item.name != null) {
            obj.addProperty("name", item.name);
        }
        if (item.customModelData != null) {
            obj.addProperty("customModelData", item.customModelData);
        }
        if (item.category != null) {
            obj.addProperty("category", item.category);
        }
        if (item.texture != null) {
            obj.addProperty("texture", item.texture);
        }
        addStringArray(obj, "customModelStrings", item.customModelStrings);
        if (item.customModelFlags != null) {
            JsonArray arr = new JsonArray();
            for (Boolean flag : item.customModelFlags) {
                if (flag != null) {
                    arr.add(flag);
                }
            }
            obj.add("customModelFlags", arr);
        }
        if (item.customModelColors != null) {
            JsonArray arr = new JsonArray();
            for (Integer color : item.customModelColors) {
                if (color != null) {
                    arr.add(color);
                }
            }
            obj.add("customModelColors", arr);
        }
        if (item.chatTooltip != null && !item.chatTooltip.isEmpty()) {
            JsonArray tips = new JsonArray();
            for (ChatTooltipLine line : item.chatTooltip) {
                tips.add(tooltipLineToJson(line));
            }
            obj.add("chatTooltip", tips);
        }
        return obj;
    }

    public static ItemData fromJson(JsonObject obj) {
        if (obj == null) {
            return null;
        }
        ItemData item = new ItemData();
        item.id = stringOrNull(obj, "id");
        item.name = stringOrNull(obj, "name");
        if (obj.has("customModelData") && obj.get("customModelData").isJsonPrimitive()) {
            item.customModelData = obj.get("customModelData").getAsInt();
        }
        item.category = stringOrNull(obj, "category");
        item.texture = stringOrNull(obj, "texture");
        item.customModelStrings = readStringArray(obj, "customModelStrings");
        if (obj.has("customModelFlags") && obj.get("customModelFlags").isJsonArray()) {
            item.customModelFlags = new ArrayList<>();
            for (JsonElement el : obj.getAsJsonArray("customModelFlags")) {
                if (el != null && el.isJsonPrimitive()) {
                    item.customModelFlags.add(el.getAsBoolean());
                }
            }
        }
        if (obj.has("customModelColors") && obj.get("customModelColors").isJsonArray()) {
            item.customModelColors = new ArrayList<>();
            for (JsonElement el : obj.getAsJsonArray("customModelColors")) {
                if (el != null && el.isJsonPrimitive()) {
                    item.customModelColors.add(el.getAsInt());
                }
            }
        }
        if (obj.has("chatTooltip") && obj.get("chatTooltip").isJsonArray()) {
            item.chatTooltip = new ArrayList<>();
            for (JsonElement el : obj.getAsJsonArray("chatTooltip")) {
                if (el != null && el.isJsonObject()) {
                    item.chatTooltip.add(tooltipLineFromJson(el.getAsJsonObject()));
                }
            }
        }
        return item;
    }

    private static JsonObject tooltipLineToJson(ChatTooltipLine line) {
        JsonObject obj = new JsonObject();
        JsonArray parts = new JsonArray();
        if (line != null && line.parts != null) {
            for (ChatTooltipLine.ChatTooltipPart part : line.parts) {
                if (part == null) {
                    continue;
                }
                JsonObject p = new JsonObject();
                p.addProperty("text", part.text != null ? part.text : "");
                if (part.color != null) {
                    p.addProperty("color", part.color);
                }
                if (part.italic != null) {
                    p.addProperty("italic", part.italic);
                }
                if (part.bold != null) {
                    p.addProperty("bold", part.bold);
                }
                if (part.underlined != null) {
                    p.addProperty("underlined", part.underlined);
                }
                if (part.strikethrough != null) {
                    p.addProperty("strikethrough", part.strikethrough);
                }
                parts.add(p);
            }
        }
        obj.add("parts", parts);
        return obj;
    }

    private static ChatTooltipLine tooltipLineFromJson(JsonObject obj) {
        ChatTooltipLine line = new ChatTooltipLine();
        if (obj.has("parts") && obj.get("parts").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("parts")) {
                if (el == null || !el.isJsonObject()) {
                    continue;
                }
                JsonObject p = el.getAsJsonObject();
                ChatTooltipLine.ChatTooltipPart part = new ChatTooltipLine.ChatTooltipPart();
                part.text = stringOrNull(p, "text");
                if (part.text == null) {
                    part.text = "";
                }
                if (p.has("color") && p.get("color").isJsonPrimitive()) {
                    part.color = p.get("color").getAsInt();
                }
                if (p.has("italic")) {
                    part.italic = p.get("italic").getAsBoolean();
                }
                if (p.has("bold")) {
                    part.bold = p.get("bold").getAsBoolean();
                }
                if (p.has("underlined")) {
                    part.underlined = p.get("underlined").getAsBoolean();
                }
                if (p.has("strikethrough")) {
                    part.strikethrough = p.get("strikethrough").getAsBoolean();
                }
                line.parts.add(part);
            }
        }
        return line;
    }

    private static void addStringArray(JsonObject obj, String key, List<String> values) {
        if (values == null) {
            return;
        }
        JsonArray arr = new JsonArray();
        for (String value : values) {
            if (value != null) {
                arr.add(value);
            }
        }
        obj.add(key, arr);
    }

    private static List<String> readStringArray(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonArray()) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (JsonElement el : obj.getAsJsonArray(key)) {
            if (el != null && el.isJsonPrimitive()) {
                out.add(el.getAsString());
            }
        }
        return out;
    }

    private static String stringOrNull(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return null;
        }
        String value = obj.get(key).getAsString();
        return value != null && !value.isEmpty() ? value : null;
    }
}

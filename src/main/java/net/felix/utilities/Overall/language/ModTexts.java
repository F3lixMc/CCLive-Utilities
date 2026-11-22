package net.felix.utilities.Overall.language;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.network.chat.Component;

/**
 * Modtexte aus den eigenen Sprachdateien. Minecrafts Spracheinstellung bleibt unberührt.
 */
public final class ModTexts {

    private static final Map<String, String> GERMAN = load("de_de.json");
    private static final Map<String, String> ENGLISH = load("en_us.json");

    private ModTexts() {
    }

    public static String language() {
        try {
            String code = CCLiveUtilitiesConfig.HANDLER.instance().modLanguage;
            return "en".equals(code) ? "en" : "de";
        } catch (Exception ignored) {
            return "de";
        }
    }

    public static String get(String key) {
        String value = active().get(key);
        if (value != null) {
            return value;
        }
        value = GERMAN.get(key);
        return value != null ? value : key;
    }

    public static Component text(String key) {
        return Component.literal(get(key));
    }

    private static Map<String, String> active() {
        return "en".equals(language()) ? ENGLISH : GERMAN;
    }

    private static Map<String, String> load(String file) {
        try (InputStream in = ModTexts.class.getResourceAsStream("/assets/cclive-utilities/lang/" + file)) {
            if (in == null) {
                return Map.of();
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    map.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            return Collections.unmodifiableMap(map);
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}

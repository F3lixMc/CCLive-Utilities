package net.felix.devtools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.felix.CCLiveUtilities;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistenz: An/Aus, Hotkey (null = ungebunden), gewählter Modus.
 */
public final class DevToolsStateStore {

    private static final String FILE_NAME = "devtools_state.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, ToolState> STATES = new ConcurrentHashMap<>();
    private static boolean loaded;

    public static final class ToolState {
        public boolean enabled;
        /** GLFW keycode oder null = ungebunden. */
        public Integer hotkey;
        public String modeId;

        public ToolState() {
        }

        public ToolState(boolean enabled, Integer hotkey, String modeId) {
            this.enabled = enabled;
            this.hotkey = hotkey;
            this.modeId = modeId;
        }
    }

    private DevToolsStateStore() {
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        load();
        loaded = true;
    }

    public static ToolState get(String toolId) {
        ensureLoaded();
        return STATES.computeIfAbsent(toolId, id -> new ToolState(false, null, null));
    }

    public static boolean isEnabled(String toolId) {
        return get(toolId).enabled;
    }

    public static void setEnabled(String toolId, boolean enabled) {
        ToolState state = get(toolId);
        state.enabled = enabled;
        save();
    }

    public static Integer getHotkey(String toolId) {
        return get(toolId).hotkey;
    }

    public static void setHotkey(String toolId, Integer keyCode) {
        ToolState state = get(toolId);
        state.hotkey = keyCode;
        save();
    }

    public static String getModeId(String toolId) {
        return get(toolId).modeId;
    }

    public static void setModeId(String toolId, String modeId) {
        ToolState state = get(toolId);
        state.modeId = modeId;
        save();
    }

    private static Path path() {
        return CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve(FILE_NAME);
    }

    private static void load() {
        STATES.clear();
        try {
            Path file = path();
            if (!Files.exists(file)) {
                return;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject tools = root.has("tools") && root.get("tools").isJsonObject()
                ? root.getAsJsonObject("tools")
                : root;
            for (Map.Entry<String, JsonElement> entry : tools.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject obj = entry.getValue().getAsJsonObject();
                ToolState state = new ToolState();
                state.enabled = obj.has("enabled") && obj.get("enabled").getAsBoolean();
                if (obj.has("hotkey") && !obj.get("hotkey").isJsonNull()) {
                    state.hotkey = obj.get("hotkey").getAsInt();
                } else {
                    state.hotkey = null;
                }
                if (obj.has("modeId") && !obj.get("modeId").isJsonNull()) {
                    state.modeId = obj.get("modeId").getAsString();
                }
                STATES.put(entry.getKey(), state);
            }
        } catch (Exception ignored) {
            // Defekt: Defaults behalten, nicht überschreiben beim Speichern ohne Absicht
        }
    }

    public static synchronized void save() {
        try {
            Path file = path();
            Files.createDirectories(file.getParent());
            JsonObject tools = new JsonObject();
            for (Map.Entry<String, ToolState> entry : new HashMap<>(STATES).entrySet()) {
                ToolState state = entry.getValue();
                JsonObject obj = new JsonObject();
                obj.addProperty("enabled", state.enabled);
                if (state.hotkey != null) {
                    obj.addProperty("hotkey", state.hotkey);
                } else {
                    obj.add("hotkey", JsonNull.INSTANCE);
                }
                if (state.modeId != null) {
                    obj.addProperty("modeId", state.modeId);
                } else {
                    obj.add("modeId", JsonNull.INSTANCE);
                }
                tools.add(entry.getKey(), obj);
            }
            JsonObject root = new JsonObject();
            root.add("tools", tools);
            Path tmp = file.resolveSibling(FILE_NAME + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            try {
                Path file = path();
                Path tmp = file.resolveSibling(FILE_NAME + ".tmp");
                if (Files.exists(tmp)) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {
            }
        }
    }
}

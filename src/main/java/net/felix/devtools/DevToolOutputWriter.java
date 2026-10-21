package net.felix.devtools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.felix.CCLiveUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Atomarer Append in config/cclive-utilities/devtool_output.json
 */
public final class DevToolOutputWriter {

    private static final String FILE_NAME = "devtool_output.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Queue<JsonObject> QUEUE = new ArrayDeque<>();
    private static boolean writing;

    private DevToolOutputWriter() {
    }

    public static Path outputPath() {
        return CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve(FILE_NAME);
    }

    public static synchronized boolean writeNow(JsonObject capture, int itemCount) {
        try {
            appendOne(capture);
            notifySuccess(itemCount);
            return true;
        } catch (Exception e) {
            notifyPlayer("§cdevtool_output.json: " + e.getMessage());
            return false;
        }
    }

    public static synchronized void enqueue(JsonObject capture) {
        QUEUE.add(capture);
        drain();
    }

    private static synchronized void drain() {
        if (writing) {
            return;
        }
        writing = true;
        try {
            while (!QUEUE.isEmpty()) {
                JsonObject next = QUEUE.poll();
                if (next != null) {
                    try {
                        appendOne(next);
                    } catch (Exception e) {
                        notifyPlayer("§cdevtool_output.json: " + e.getMessage());
                    }
                }
            }
        } finally {
            writing = false;
        }
    }

    private static void appendOne(JsonObject capture) throws Exception {
        Path file = outputPath();
        Files.createDirectories(file.getParent());

        JsonObject root;
        JsonArray captures;
        if (Files.exists(file)) {
            String raw = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (raw.isEmpty()) {
                root = new JsonObject();
                captures = new JsonArray();
                root.add("captures", captures);
            } else {
                try {
                    JsonElement parsed = JsonParser.parseString(raw);
                    if (!parsed.isJsonObject()) {
                        throw new IllegalStateException("Root ist kein Objekt");
                    }
                    root = parsed.getAsJsonObject();
                    if (root.has("captures") && root.get("captures").isJsonArray()) {
                        captures = root.getAsJsonArray("captures");
                    } else {
                        captures = new JsonArray();
                        root.add("captures", captures);
                    }
                } catch (Exception e) {
                    notifyPlayer("§cdevtool_output.json ist beschädigt — nicht überschrieben.");
                    throw e;
                }
            }
        } else {
            root = new JsonObject();
            captures = new JsonArray();
            root.add("captures", captures);
        }

        captures.add(capture);

        Path tmp = file.resolveSibling(FILE_NAME + ".tmp");
        Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void notifySuccess(int itemCount) {
        notifyPlayer("§aItemDataReader: §f" + itemCount + " §aItem(s) → §7" + outputPath().toAbsolutePath());
    }

    public static void notifyPlayer(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
    }
}

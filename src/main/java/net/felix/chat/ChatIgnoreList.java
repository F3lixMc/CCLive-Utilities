package net.felix.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.felix.CCLiveUtilities;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Persönliche Ignore-Liste, getrennt nach lokalem Spielerkonto. Nur die Anzeige, keine Serverdaten.
 */
public final class ChatIgnoreList {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile ChatIgnoreList shared;

    private final Path file;
    private final Map<String, LinkedHashMap<String, Stored>> accounts = new LinkedHashMap<>();

    private record Stored(String name, Long playerId) {
    }
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private ChatIgnoreList(Path file) {
        this.file = file;
        load();
    }

    public static ChatIgnoreList shared() {
        ChatIgnoreList current = shared;
        if (current == null) {
            synchronized (ChatIgnoreList.class) {
                current = shared;
                if (current == null) {
                    Path path = CCLiveUtilities.getConfigDir()
                        .resolve("cclive-utilities")
                        .resolve("chat-ignore.json");
                    current = new ChatIgnoreList(path);
                    shared = current;
                }
            }
        }
        return current;
    }

    public static ChatIgnoreList load(Path file) {
        return new ChatIgnoreList(file);
    }

    public static String localAccountId() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return null;
        }
        return client.player.getUUID().toString();
    }

    public static String localPlayerName() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return null;
        }
        return client.player.getName().getString();
    }

    public static boolean isOwn(String player, String localName, Long storedPlayerId, Long localPlayerId) {
        if (player != null && localName != null && key(player).equals(key(localName))) {
            return true;
        }
        return storedPlayerId != null && localPlayerId != null && storedPlayerId.equals(localPlayerId);
    }

    public void addListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public boolean ignores(String accountId, String player) {
        if (accountId == null || player == null || player.isBlank()) {
            return false;
        }
        synchronized (accounts) {
            LinkedHashMap<String, Stored> names = accounts.get(accountId);
            return names != null && names.containsKey(key(player));
        }
    }

    public void ignore(String accountId, String player) {
        ignore(accountId, player, null);
    }

    public void ignore(String accountId, String player, Long playerId) {
        if (accountId == null || player == null || player.isBlank()) {
            return;
        }
        synchronized (accounts) {
            LinkedHashMap<String, Stored> names = accounts.computeIfAbsent(accountId, id -> new LinkedHashMap<>());
            Stored previous = names.get(key(player));
            Long id = playerId != null ? playerId : (previous == null ? null : previous.playerId);
            names.put(key(player), new Stored(player.trim(), id));
        }
        save();
        notifyListeners();
    }

    public Long playerId(String accountId, String player) {
        if (accountId == null || player == null) {
            return null;
        }
        synchronized (accounts) {
            LinkedHashMap<String, Stored> names = accounts.get(accountId);
            Stored stored = names == null ? null : names.get(key(player));
            return stored == null ? null : stored.playerId;
        }
    }

    public void unignore(String accountId, String player) {
        if (accountId == null || player == null) {
            return;
        }
        synchronized (accounts) {
            LinkedHashMap<String, Stored> names = accounts.get(accountId);
            if (names != null) {
                names.remove(key(player));
            }
        }
        save();
        notifyListeners();
    }

    public List<String> names(String accountId) {
        synchronized (accounts) {
            LinkedHashMap<String, Stored> names = accounts.get(accountId);
            if (names == null) {
                return List.of();
            }
            List<String> shown = new ArrayList<>();
            for (Stored stored : names.values()) {
                shown.add(stored.name);
            }
            return shown;
        }
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    private void load() {
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null || !root.has("accounts") || !root.get("accounts").isJsonObject()) {
                return;
            }
            synchronized (accounts) {
                accounts.clear();
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("accounts").entrySet()) {
                    if (!entry.getValue().isJsonArray()) {
                        continue;
                    }
                    LinkedHashMap<String, Stored> names = new LinkedHashMap<>();
                    for (JsonElement element : entry.getValue().getAsJsonArray()) {
                        Stored stored = readStored(element);
                        if (stored != null) {
                            names.put(key(stored.name), stored);
                        }
                    }
                    accounts.put(entry.getKey(), names);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void save() {
        if (file == null) {
            return;
        }
        JsonObject accountsJson = new JsonObject();
        synchronized (accounts) {
            for (Map.Entry<String, LinkedHashMap<String, Stored>> entry : accounts.entrySet()) {
                JsonArray names = new JsonArray();
                for (Stored stored : entry.getValue().values()) {
                    if (stored.playerId == null) {
                        names.add(stored.name);
                    } else {
                        JsonObject row = new JsonObject();
                        row.addProperty("name", stored.name);
                        row.addProperty("playerId", stored.playerId);
                        names.add(row);
                    }
                }
                accountsJson.add(entry.getKey(), names);
            }
        }
        JsonObject root = new JsonObject();
        root.add("accounts", accountsJson);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Stored readStored(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            String display = element.getAsString();
            return display == null || display.isBlank() ? null : new Stored(display.trim(), null);
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject row = element.getAsJsonObject();
        if (!row.has("name") || !row.get("name").isJsonPrimitive()) {
            return null;
        }
        String display = row.get("name").getAsString();
        if (display == null || display.isBlank()) {
            return null;
        }
        Long playerId = null;
        if (row.has("playerId") && row.get("playerId").isJsonPrimitive()) {
            try {
                playerId = row.get("playerId").getAsLong();
            } catch (Exception ignored) {
                playerId = null;
            }
        }
        return new Stored(display.trim(), playerId);
    }

    private static String key(String player) {
        return player.trim().toLowerCase(Locale.ROOT);
    }
}

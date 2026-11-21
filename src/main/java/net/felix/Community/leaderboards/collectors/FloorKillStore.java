package net.felix.Community.leaderboards.collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.felix.CCLiveUtilities;

/**
 * Datei der Ebenenstände. Die Summe dieser Stände ist der Alltime-Wert.
 */
public final class FloorKillStore {
    private static final Gson GSON = new GsonBuilder().create();
    private static FloorKillStore shared;

    private final Path file;
    private final Object lock = new Object();
    private FloorKillBook book = new FloorKillBook();
    private boolean loaded;

    public FloorKillStore(Path file) {
        this.file = file;
    }

    public static FloorKillStore shared() {
        if (shared == null) {
            Path path = CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve("floor-kills.json");
            shared = new FloorKillStore(path);
        }
        return shared;
    }

    public long read(String account, int seasonId, boolean preseason, String floor, long reading) {
        synchronized (lock) {
            load();
            long previous = book.stored(account, seasonId, preseason, floor);
            long next = book.read(account, seasonId, preseason, floor, reading);
            if (next != previous) {
                save();
            }
            return next;
        }
    }

    public long sum(String account, int seasonId, boolean preseason) {
        synchronized (lock) {
            load();
            return book.sum(account, seasonId, preseason);
        }
    }

    public Map<String, Long> floors(String account, int seasonId, boolean preseason) {
        synchronized (lock) {
            load();
            return book.floors(account, seasonId, preseason);
        }
    }

    public void clear(String account, int seasonId, boolean preseason) {
        synchronized (lock) {
            load();
            if (book.clear(account, seasonId, preseason)) {
                save();
            }
        }
    }

    public void remove(String account, int seasonId, boolean preseason, String floor) {
        synchronized (lock) {
            load();
            if (book.remove(account, seasonId, preseason, floor)) {
                save();
            }
        }
    }

    private void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            Map<String, Map<String, Long>> snapshot = new LinkedHashMap<>();
            if (root != null && root.has("phases") && root.get("phases").isJsonObject()) {
                for (var phase : root.getAsJsonObject("phases").entrySet()) {
                    if (!phase.getValue().isJsonObject()) {
                        continue;
                    }
                    Map<String, Long> floors = new LinkedHashMap<>();
                    for (var floor : phase.getValue().getAsJsonObject().entrySet()) {
                        if (floor.getValue().isJsonPrimitive() && floor.getValue().getAsJsonPrimitive().isNumber()) {
                            floors.put(floor.getKey(), floor.getValue().getAsLong());
                        }
                    }
                    snapshot.put(phase.getKey(), floors);
                }
            }
            book = FloorKillBook.restore(snapshot);
        } catch (Exception ignored) {
            book = new FloorKillBook();
        }
    }

    private void save() {
        if (file == null) {
            return;
        }
        try {
            JsonObject phases = new JsonObject();
            for (Map.Entry<String, Map<String, Long>> phase : book.snapshot().entrySet()) {
                JsonObject floors = new JsonObject();
                for (Map.Entry<String, Long> floor : phase.getValue().entrySet()) {
                    floors.addProperty(floor.getKey(), floor.getValue());
                }
                phases.add(phase.getKey(), floors);
            }
            JsonObject root = new JsonObject();
            root.add("phases", phases);
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException first) {
            try {
                Files.move(
                        file.resolveSibling(file.getFileName() + ".tmp"),
                        file,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            } catch (IOException ignored) {
            }
        }
    }
}

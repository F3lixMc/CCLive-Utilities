package net.felix.profile;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.SeasonChangeGate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

final class PlaytimeTracker {
    private static final int CHECKPOINT_TICKS = 1200;
    private static final Gson GSON = new Gson();
    private static final Logger LOG = Logger.getLogger(PlaytimeTracker.class.getName());

    private record Bucket(UUID account, Integer season, boolean preseason) {
        String key() {
            return account + ":" + season + ":" + preseason;
        }

        String unassignedKey() {
            return new Bucket(account, null, false).key();
        }
    }

    private static final class Counter {
        String id = UUID.randomUUID().toString();
        long ticks;
        transient long acknowledgedSeconds;
    }

    private static final class State {
        Map<String, Counter> counters = new HashMap<>();
    }

    private final Path file;
    private final ExecutorService disk = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "CCLive-Playtime");
        thread.setDaemon(true);
        return thread;
    });
    private State state;
    private boolean uploadInFlight;

    // Only accessed from the Minecraft client thread.
    private Bucket active;
    private int pendingTicks;

    PlaytimeTracker(Path file) {
        this.file = file;
        disk.execute(this::load);
    }

    void tick(UUID account) {
        Integer season = SeasonChangeGate.confirmedSeasonId();
        boolean preseason = season != null && CCLiveUtilitiesConfig.HANDLER.instance().lastProcessedPreseason;
        if (active == null || !active.account().equals(account)
                || !java.util.Objects.equals(active.season(), season) || active.preseason() != preseason) {
            checkpoint(false);
            active = new Bucket(account, season, preseason);
        }
        if (++pendingTicks >= CHECKPOINT_TICKS) {
            checkpoint(true);
        }
    }

    void checkpoint(boolean allowUpload) {
        if (pendingTicks == 0) {
            return;
        }
        Bucket bucket = active;
        int ticks = pendingTicks;
        pendingTicks = 0;
        LeaderboardManager manager = LeaderboardManager.getInstance();
        String token = allowUpload && bucket.season() != null && manager.canUploadBufferedScore()
                ? manager.getPlayerToken() : null;
        disk.execute(() -> persist(bucket, ticks, manager, token));
    }

    private void load() {
        try {
            state = Files.exists(file) ? GSON.fromJson(Files.readString(file), State.class) : new State();
            if (state == null || state.counters == null) {
                throw new IOException("Invalid playtime file");
            }
        } catch (Exception e) {
            LOG.warning("Spielzeitdatei konnte nicht geladen werden: " + e.getMessage());
            // Preserve the original for recovery and use new counter IDs.
            state = new State();
            try {
                Files.move(file, file.resolveSibling("playtime-corrupt-" + UUID.randomUUID() + ".json"));
            } catch (IOException moveError) {
                LOG.warning("Spielzeitdatei konnte nicht gesichert werden: " + moveError.getMessage());
            }
        }
    }

    private void persist(Bucket bucket, int ticks, LeaderboardManager manager, String token) {
        Counter counter = state.counters.computeIfAbsent(bucket.key(), key -> new Counter());
        if (bucket.season() != null) {
            Counter unassigned = state.counters.remove(bucket.unassignedKey());
            if (unassigned != null) {
                counter.ticks += unassigned.ticks;
            }
        }
        counter.ticks += ticks;
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling("playtime.json.tmp");
            Files.writeString(temp, GSON.toJson(state));
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.warning("Spielzeit konnte nicht gespeichert werden: " + e.getMessage());
            return;
        }
        long seconds = counter.ticks / 20;
        if (token == null || uploadInFlight || seconds <= counter.acknowledgedSeconds) {
            return;
        }
        JsonObject request = new JsonObject();
        request.addProperty("counterId", counter.id);
        request.addProperty("seasonId", bucket.season());
        request.addProperty("isPreseason", bucket.preseason());
        request.addProperty("seconds", seconds);
        uploadInFlight = true;
        CompletableFuture.supplyAsync(() -> {
            try {
                if (!manager.canUploadBufferedScore() || !token.equals(manager.getPlayerToken())) {
                    return false;
                }
                JsonObject response = manager.getHttpClient().postWithToken("/playtime/progress", request, token);
                return response != null && response.has("success") && response.get("success").getAsBoolean();
            } catch (Exception e) {
                return false;
            }
        }).whenComplete((success, error) -> {
            if (!disk.isShutdown()) {
                disk.execute(() -> {
                    uploadInFlight = false;
                    if (Boolean.TRUE.equals(success)) {
                        counter.acknowledgedSeconds = seconds;
                    }
                });
            }
        });
    }

    void close() {
        checkpoint(false);
        disk.shutdown();
        try {
            disk.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

package net.felix.leaderboards;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.http.HttpClient;
import net.felix.profile.ProfileStatsManager;
import net.felix.utilities.Farmworld.fishing.found.LocalSeasonProgressReset;
import net.felix.utilities.Overall.server.ServerConnectionGate;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Season-/Preseason-Pruefung am Join: Upload-Gate, lokaler Progress-Reset, Epoch gegen stale Disconnect-Antworten.
 */
public final class SeasonChangeGate {
    private static SeasonChangeGate instance;

    private static final long RETRY_INITIAL_MS = 3000L;
    private static final long RETRY_MAX_MS = 60000L;

    private final AtomicBoolean uploadsAllowed = new AtomicBoolean(false);
    private final AtomicBoolean checkInFlight = new AtomicBoolean(false);
    private final AtomicLong checkEpoch = new AtomicLong(0L);
    private final AtomicBoolean retryEnabled = new AtomicBoolean(false);
    private final AtomicLong retryDelayMs = new AtomicLong(RETRY_INITIAL_MS);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CCLive-SeasonChangeGate");
        t.setDaemon(true);
        return t;
    });
    private volatile ScheduledFuture<?> pendingRetry;

    private boolean initialized;

    private SeasonChangeGate() {
    }

    public static SeasonChangeGate getInstance() {
        if (instance == null) {
            instance = new SeasonChangeGate();
        }
        return instance;
    }

    public void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
            client.execute(this::onJoin));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(this::onDisconnect));

        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null && client.getConnection() != null) {
            onJoin();
        }
    }

    /** Saisonale Statistik-Uploads (Leaderboard/Profile) erst nach erfolgreicher Season-Pruefung. */
    public boolean areSeasonUploadsAllowed() {
        return uploadsAllowed.get();
    }

    /**
     * Zuletzt erfolgreich verarbeitete Season.
     * Eine fehlende oder noch laufende Antwort ändert {@code lastProcessedSeasonId} nicht.
     */
    public static Integer confirmedSeasonId() {
        return confirmedSeasonId(CCLiveUtilitiesConfig.HANDLER.instance().lastProcessedSeasonId);
    }

    static Integer confirmedSeasonId(int lastProcessedSeasonId) {
        return lastProcessedSeasonId < 0 ? null : lastProcessedSeasonId;
    }

    private void onJoin() {
        uploadsAllowed.set(false);
        long epoch = checkEpoch.incrementAndGet();
        cancelPendingRetry();
        retryEnabled.set(true);
        retryDelayMs.set(RETRY_INITIAL_MS);
        ServerConnectionGate.refresh();
        scheduleCheck(epoch, 0L);
    }

    private void onDisconnect() {
        uploadsAllowed.set(false);
        checkEpoch.incrementAndGet();
        retryEnabled.set(false);
        cancelPendingRetry();
        checkInFlight.set(false);
    }

    private void cancelPendingRetry() {
        ScheduledFuture<?> f = pendingRetry;
        pendingRetry = null;
        if (f != null) {
            f.cancel(false);
        }
    }

    private void scheduleCheck(long epoch, long delayMs) {
        if (!retryEnabled.get() || epoch != checkEpoch.get()) {
            return;
        }
        cancelPendingRetry();
        pendingRetry = scheduler.schedule(() -> runCheck(epoch), Math.max(0L, delayMs), TimeUnit.MILLISECONDS);
    }

    private void runCheck(long epoch) {
        if (epoch != checkEpoch.get() || !retryEnabled.get()) {
            return;
        }
        if (!checkInFlight.compareAndSet(false, true)) {
            return;
        }

        try {
            if (epoch != checkEpoch.get()) {
                return;
            }

            HttpClient http = LeaderboardManager.getInstance().getHttpClient();
            if (http == null) {
                scheduleRetry(epoch);
                return;
            }

            JsonObject response;
            try {
                response = http.get("/season/current");
            } catch (Exception e) {
                scheduleRetry(epoch);
                return;
            }

            if (epoch != checkEpoch.get()) {
                return;
            }

            SeasonInfo info = parseSeasonInfo(response);
            if (info == null) {
                scheduleRetry(epoch);
                return;
            }

            retryDelayMs.set(RETRY_INITIAL_MS);

            CCLiveUtilitiesConfig cfg = CCLiveUtilitiesConfig.HANDLER.instance();
            int lastId = cfg.lastProcessedSeasonId;
            boolean lastPre = cfg.lastProcessedPreseason;
            boolean needsReset = needsLocalReset(lastId, lastPre, info.seasonId, info.isPreseason);

            if (!needsReset) {
                if (epoch != checkEpoch.get()) {
                    return;
                }
                uploadsAllowed.set(true);
                return;
            }

            boolean filesOk = LocalSeasonProgressReset.resetAllSixProgressFiles();
            if (epoch != checkEpoch.get()) {
                return;
            }
            if (!filesOk) {
                scheduleRetry(epoch);
                return;
            }

            LeaderboardManager.getInstance().discardSeasonComparisonState();
            try {
                ProfileStatsManager.getInstance().discardPendingSeasonUploads();
            } catch (Exception ignored) {
            }

            if (epoch != checkEpoch.get()) {
                return;
            }

            cfg.lastProcessedSeasonId = info.seasonId;
            cfg.lastProcessedPreseason = info.isPreseason;
            cfg.lastSeasonResetAt = Instant.now().toString();
            try {
                CCLiveUtilitiesConfig.HANDLER.save();
            } catch (Exception e) {
                cfg.lastProcessedSeasonId = lastId;
                cfg.lastProcessedPreseason = lastPre;
                scheduleRetry(epoch);
                return;
            }

            if (epoch != checkEpoch.get()) {
                return;
            }

            uploadsAllowed.set(true);
            notifyPlayer(info);
        } finally {
            checkInFlight.set(false);
        }
    }

    private void scheduleRetry(long epoch) {
        if (!retryEnabled.get() || epoch != checkEpoch.get()) {
            return;
        }
        long delay = retryDelayMs.get();
        long next = Math.min(RETRY_MAX_MS, Math.max(RETRY_INITIAL_MS, delay * 2));
        retryDelayMs.set(next);
        scheduleCheck(epoch, delay);
    }

    private static SeasonInfo parseSeasonInfo(JsonObject response) {
        if (response == null || !response.has("season_id") || response.get("season_id").isJsonNull()) {
            return null;
        }
        int seasonId;
        try {
            seasonId = response.get("season_id").getAsInt();
        } catch (Exception e) {
            return null;
        }
        if (!response.has("is_preseason") || response.get("is_preseason").isJsonNull()) {
            return null;
        }
        boolean isPreseason;
        try {
            JsonElement pre = response.get("is_preseason");
            if (pre.isJsonPrimitive() && pre.getAsJsonPrimitive().isBoolean()) {
                isPreseason = pre.getAsBoolean();
            } else if (pre.isJsonPrimitive() && pre.getAsJsonPrimitive().isNumber()) {
                isPreseason = pre.getAsInt() != 0;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        String name = "";
        if (response.has("name") && !response.get("name").isJsonNull()) {
            try {
                name = response.get("name").getAsString();
            } catch (Exception ignored) {
            }
        }
        return new SeasonInfo(seasonId, isPreseason, name);
    }

    private static void notifyPlayer(SeasonInfo info) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        String label = resetLabel(info.seasonId, info.isPreseason);
        String message = "§d[CCLive] §fLokaler Spielerfortschritt für " + label + " zurückgesetzt.";
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal(message));
            }
        });
    }

    /** Erneuter Reset bei neuer Id und wenn dieselbe Id von der Preseason in die Season wechselt. */
    static boolean needsLocalReset(int lastId, boolean lastPre, int seasonId, boolean isPreseason) {
        return lastId == -1 || lastId != seasonId || lastPre != isPreseason;
    }

    /** Angezeigte Nummer ist die Id minus 1. Id 9 ist Season 8, Id 10 in der Preseason ist Preseason 9. */
    static String resetLabel(int seasonId, boolean isPreseason) {
        int shown = Math.max(0, seasonId - 1);
        return (isPreseason ? "Preseason " : "Season ") + shown;
    }

    private static final class SeasonInfo {
        final int seasonId;
        final boolean isPreseason;
        final String name;

        SeasonInfo(int seasonId, boolean isPreseason, String name) {
            this.seasonId = seasonId;
            this.isPreseason = isPreseason;
            this.name = name;
        }
    }
}

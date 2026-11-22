package net.felix.Community.leaderboards.upload;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.felix.CCLiveUtilities;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Other.debug.DebugUtility;
import net.felix.Community.leaderboards.LeaderboardManager;
import net.felix.Community.leaderboards.sync.LeaderboardSync;
import net.felix.Community.leaderboards.reset.PlayerFullReset;
import net.felix.Community.leaderboards.sync.SeasonChangeGate;

/**
 * Prüft den Fabriklevel-Upload auf einem Hintergrundthread, unabhängig vom Lesen der Erfahrungsleiste.
 */
public final class FactoryLevelUpload {
    private static final FactoryLevelUpload INSTANCE = new FactoryLevelUpload();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "CCLive-FactoryLevel");
        thread.setDaemon(true);
        return thread;
    });
    private final FactoryLevelOutbox outbox;
    private volatile boolean loaded;
    private volatile boolean started;

    private FactoryLevelUpload() {
        Path file = CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve("factory-level-playlegend.json");
        this.outbox = new FactoryLevelOutbox(file);
    }

    public static FactoryLevelUpload getInstance() {
        return INSTANCE;
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        executor.execute(() -> {
            outbox.load();
            loaded = true;
        });
        executor.scheduleWithFixedDelay(this::tick, 1L, 1L, TimeUnit.SECONDS);
    }

    public void submit(int level) {
        if (!CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            return;
        }
        String account = net.felix.Community.chat.ignore.ChatIgnoreList.localAccountId();
        Integer seasonId = SeasonChangeGate.confirmedSeasonId();
        if (account == null || seasonId == null || !FactoryLevel.fits(level)) {
            return;
        }
        boolean preseason = CCLiveUtilitiesConfig.HANDLER.instance().lastProcessedPreseason;
        SmithSkillOutbox.Phase phase = new SmithSkillOutbox.Phase(seasonId, preseason);
        executor.execute(() -> outbox.observe(account, phase, level));
    }

    public void forget(String account, int seasonId, boolean preseason) {
        if (account == null || seasonId < 0) {
            return;
        }
        executor.execute(() -> {
            if (!loaded) {
                outbox.load();
                loaded = true;
            }
            outbox.forget(account, new SmithSkillOutbox.Phase(seasonId, preseason));
        });
    }

    private void tick() {
        if (!loaded) {
            return;
        }
        try {
            String account = net.felix.Community.chat.ignore.ChatIgnoreList.localAccountId();
            Integer seasonId = SeasonChangeGate.confirmedSeasonId();
            LeaderboardManager manager = LeaderboardManager.getInstance();
            SmithSkillOutbox.Phase phase = seasonId == null
                    ? null
                    : new SmithSkillOutbox.Phase(seasonId, CCLiveUtilitiesConfig.HANDLER.instance().lastProcessedPreseason);
            LeaderboardSync.SyncClock sync = LeaderboardSync.getInstance().syncClock();
            SmithSkillOutbox.Clock clock = sync == null
                    ? null
                    : new SmithSkillOutbox.Clock(sync.receivedAt(), sync.serverTime(), sync.nextSyncAt());
            boolean allowed = manager != null && manager.canUploadBufferedScore()
                    && !PlayerFullReset.blocks(account, seasonId == null ? -1 : seasonId,
                    phase != null && phase.preseason());
            long now = System.currentTimeMillis();
            FactoryLevelOutbox.Attempt attempt = outbox.begin(account, phase, now, clock, allowed);
            if (attempt == null) {
                return;
            }
            boolean success = false;
            try {
                success = manager.postFactoryLevel(
                        attempt.phase().seasonId(),
                        attempt.phase().preseason(),
                        attempt.level());
            } finally {
                outbox.complete(attempt, success, System.currentTimeMillis());
            }
            if (success) {
                DebugUtility.debugPlayerStats("factory-send", "Fabriklevel gesendet: " + attempt.level());
            } else {
                DebugUtility.debugPlayerStats("factory-send", "Fabriklevel " + attempt.level() + " nicht gesendet");
            }
        } catch (Exception e) {
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            DebugUtility.debugPlayerStats("factory-send", "Fabriklevel nicht gesendet: " + reason);
        }
    }
}

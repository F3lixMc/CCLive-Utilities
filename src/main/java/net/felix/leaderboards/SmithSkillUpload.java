package net.felix.leaderboards;

import net.felix.CCLiveUtilities;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.panel.SmithSkill;
import net.felix.utilities.Other.DebugUtility;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Prüft den Schmiede-Upload auf einem Hintergrundthread, unabhängig vom Widget-Lesen.
 */
public final class SmithSkillUpload {
    private static final SmithSkillUpload INSTANCE = new SmithSkillUpload();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "CCLive-SmithSkill");
        thread.setDaemon(true);
        return thread;
    });
    private final SmithSkillOutbox outbox;
    private volatile boolean loaded;

    private SmithSkillUpload() {
        Path file = CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve("smith-skill.json");
        this.outbox = new SmithSkillOutbox(file);
    }

    public static SmithSkillUpload getInstance() {
        return INSTANCE;
    }

    private volatile boolean started;

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

    public void submit(int level, long xp) {
        if (!CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            return;
        }
        String account = net.felix.chat.ChatIgnoreList.localAccountId();
        Integer seasonId = SeasonChangeGate.confirmedSeasonId();
        if (account == null || seasonId == null) {
            return;
        }
        boolean preseason = CCLiveUtilitiesConfig.HANDLER.instance().lastProcessedPreseason;
        SmithSkillOutbox.Phase phase = new SmithSkillOutbox.Phase(seasonId, preseason);
        SmithSkill.Stand stand = new SmithSkill.Stand(level, xp);
        executor.execute(() -> outbox.observe(account, phase, stand));
    }

    private void tick() {
        if (!loaded) {
            return;
        }
        try {
            String account = net.felix.chat.ChatIgnoreList.localAccountId();
            Integer seasonId = SeasonChangeGate.confirmedSeasonId();
            LeaderboardManager manager = LeaderboardManager.getInstance();
            SmithSkillOutbox.Phase phase = seasonId == null
                    ? null
                    : new SmithSkillOutbox.Phase(seasonId, CCLiveUtilitiesConfig.HANDLER.instance().lastProcessedPreseason);
            LeaderboardSync.SyncClock sync = LeaderboardSync.getInstance().syncClock();
            SmithSkillOutbox.Clock clock = sync == null
                    ? null
                    : new SmithSkillOutbox.Clock(sync.receivedAt(), sync.serverTime(), sync.nextSyncAt());
            boolean allowed = manager != null && manager.canUploadSmithSkill();
            long now = System.currentTimeMillis();
            SmithSkillOutbox.Attempt attempt = outbox.begin(account, phase, now, clock, allowed);
            if (attempt == null) {
                return;
            }
            boolean success = false;
            try {
                success = manager.postSmithSkill(
                        attempt.phase().seasonId(),
                        attempt.phase().preseason(),
                        attempt.stand().level(),
                        attempt.stand().xp());
            } finally {
                outbox.complete(attempt, success, System.currentTimeMillis());
            }
            String shown = SmithSkill.display(attempt.stand());
            if (success) {
                DebugUtility.debugPlayerStats("smith-send", "Schmiedeskill gesendet: " + shown);
            } else {
                DebugUtility.debugPlayerStats("smith-send", "Schmiedeskill " + shown + " nicht gesendet");
            }
        } catch (Exception e) {
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            DebugUtility.debugPlayerStats("smith-send", "Schmiedeskill nicht gesendet: " + reason);
        }
    }
}

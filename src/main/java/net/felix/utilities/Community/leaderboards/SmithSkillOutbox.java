package net.felix.utilities.Community.leaderboards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.felix.utilities.Community.leaderboards.panel.SmithSkill;

/**
 * Höchster Schmiedestand pro Konto und Seasonphase, plus das letzte bestätigte Uploadfenster.
 * Dateizugriffe passieren nur, wenn der Aufrufer sie anstößt.
 */
public final class SmithSkillOutbox {
    public static final long WINDOW_MS = 5L * 60L * 1000L;
    public static final long RETRY_INITIAL_MS = 3_000L;
    public static final long RETRY_MAX_MS = 60_000L;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final Map<String, Map<String, Saved>> accounts = new LinkedHashMap<>();
    private final Map<String, Attempt> inflight = new LinkedHashMap<>();
    private int saves;

    public SmithSkillOutbox(Path file) {
        this.file = file;
    }

    public record Phase(int seasonId, boolean preseason) {
    }

    public record Clock(long receivedAt, long serverTime, long nextSyncAt) {
    }

    public record Attempt(String account, Phase phase, SmithSkill.Stand stand, long nextSyncAt) {
    }

    /** Dieselbe Zielzeit wie der Sync, ohne den Abruf-Versatz. */
    public static long clientSyncAt(Clock clock) {
        return clock.receivedAt() + (clock.nextSyncAt() - clock.serverTime());
    }

    public static boolean windowOpen(long now, Clock clock) {
        if (clock == null) {
            return false;
        }
        long target = clientSyncAt(clock);
        return now >= target - WINDOW_MS && now < target;
    }

    public static boolean better(SmithSkill.Stand next, SmithSkill.Stand current) {
        if (next == null || !SmithSkill.fits(next.level(), next.xp())) {
            return false;
        }
        if (current == null) {
            return true;
        }
        if (next.level() != current.level()) {
            return next.level() > current.level();
        }
        return next.xp() > current.xp();
    }

    public void load() {
        accounts.clear();
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null || !root.has("accounts") || !root.get("accounts").isJsonObject()) {
                return;
            }
            for (Map.Entry<String, JsonElement> account : root.getAsJsonObject("accounts").entrySet()) {
                if (account.getKey() == null || account.getKey().isBlank() || !account.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject phases = account.getValue().getAsJsonObject();
                if (!phases.has("phases") || !phases.get("phases").isJsonObject()) {
                    continue;
                }
                Map<String, Saved> stored = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> phase : phases.getAsJsonObject("phases").entrySet()) {
                    Saved saved = readSaved(phase.getValue());
                    if (phase.getKey() != null && saved != null && saved.best != null) {
                        stored.put(phase.getKey(), saved);
                    }
                }
                if (!stored.isEmpty()) {
                    accounts.put(account.getKey(), stored);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** Ein fehlender Season-Wert oder ein fehlendes Konto legt nichts an und löscht nichts. */
    public void observe(String account, Phase phase, SmithSkill.Stand stand) {
        if (account == null || account.isBlank() || phase == null || !better(stand, null)) {
            return;
        }
        Saved saved = ensure(account, phase);
        if (!better(stand, saved.best)) {
            return;
        }
        saved.best = stand;
        save();
    }

    public Attempt begin(String account, Phase phase, long now, Clock clock, boolean allowed) {
        if (!allowed || account == null || account.isBlank() || phase == null || !windowOpen(now, clock)) {
            return null;
        }
        if (inflight.containsKey(account)) {
            return null;
        }
        Saved saved = existing(account, phase);
        if (saved == null || saved.best == null) {
            return null;
        }
        if (saved.uploadedSyncAt != null && saved.uploadedSyncAt == clock.nextSyncAt()) {
            return null;
        }
        if (!better(saved.best, saved.uploaded)) {
            return null;
        }
        if (saved.backoffSyncAt == clock.nextSyncAt() && now < saved.nextAttemptAt) {
            return null;
        }
        Attempt attempt = new Attempt(account, phase, saved.best, clock.nextSyncAt());
        inflight.put(account, attempt);
        return attempt;
    }

    public void complete(Attempt attempt, boolean success, long now) {
        if (attempt == null || attempt.account() == null || attempt.phase() == null) {
            return;
        }
        Attempt current = inflight.get(attempt.account());
        if (current == attempt) {
            inflight.remove(attempt.account());
        } else if (!success) {
            return;
        }
        Saved saved = existing(attempt.account(), attempt.phase());
        if (saved == null) {
            return;
        }
        if (!success) {
            long delay = saved.retryDelay <= 0L ? RETRY_INITIAL_MS : Math.min(RETRY_MAX_MS, saved.retryDelay * 2L);
            saved.retryDelay = delay;
            saved.nextAttemptAt = now + delay;
            saved.backoffSyncAt = attempt.nextSyncAt();
            return;
        }
        if (saved.uploaded != null
                && saved.uploaded.equals(attempt.stand())
                && saved.uploadedSyncAt != null
                && saved.uploadedSyncAt == attempt.nextSyncAt()) {
            return;
        }
        saved.uploaded = attempt.stand();
        saved.uploadedSyncAt = attempt.nextSyncAt();
        saved.retryDelay = 0L;
        saved.nextAttemptAt = 0L;
        save();
    }

    public SmithSkill.Stand best(String account, Phase phase) {
        Saved saved = existing(account, phase);
        return saved == null ? null : saved.best;
    }

    public SmithSkill.Stand uploaded(String account, Phase phase) {
        Saved saved = existing(account, phase);
        return saved == null ? null : saved.uploaded;
    }

    public Long uploadedSyncAt(String account, Phase phase) {
        Saved saved = existing(account, phase);
        return saved == null ? null : saved.uploadedSyncAt;
    }

    public int saves() {
        return saves;
    }

    public boolean inFlight(String account) {
        return account != null && inflight.containsKey(account);
    }

    private Saved ensure(String account, Phase phase) {
        Map<String, Saved> phases = accounts.computeIfAbsent(account, key -> new LinkedHashMap<>());
        return phases.computeIfAbsent(key(phase), key -> new Saved());
    }

    private Saved existing(String account, Phase phase) {
        if (account == null || phase == null) {
            return null;
        }
        Map<String, Saved> phases = accounts.get(account);
        if (phases == null) {
            return null;
        }
        return phases.get(key(phase));
    }

    static String key(Phase phase) {
        return phase.seasonId() + ":" + phase.preseason();
    }

    private void save() {
        if (file == null) {
            saves++;
            return;
        }
        JsonObject accountsJson = new JsonObject();
        for (Map.Entry<String, Map<String, Saved>> account : accounts.entrySet()) {
            JsonObject phases = new JsonObject();
            for (Map.Entry<String, Saved> phase : account.getValue().entrySet()) {
                Saved saved = phase.getValue();
                if (saved.best == null) {
                    continue;
                }
                JsonObject row = new JsonObject();
                row.addProperty("level", saved.best.level());
                row.addProperty("xp", saved.best.xp());
                if (saved.uploaded != null && saved.uploadedSyncAt != null) {
                    row.addProperty("uploadedLevel", saved.uploaded.level());
                    row.addProperty("uploadedXp", saved.uploaded.xp());
                    row.addProperty("uploadedSyncAt", saved.uploadedSyncAt);
                }
                phases.add(phase.getKey(), row);
            }
            JsonObject accountJson = new JsonObject();
            accountJson.add("phases", phases);
            accountsJson.add(account.getKey(), accountJson);
        }
        JsonObject root = new JsonObject();
        root.add("accounts", accountsJson);
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
            saves++;
        } catch (IOException ignored) {
        }
    }

    private static Saved readSaved(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject row = element.getAsJsonObject();
        SmithSkill.Stand best = stand(row, "level", "xp");
        if (best == null) {
            return null;
        }
        Saved saved = new Saved();
        saved.best = best;
        saved.uploaded = stand(row, "uploadedLevel", "uploadedXp");
        if (row.has("uploadedSyncAt") && row.get("uploadedSyncAt").isJsonPrimitive()) {
            try {
                saved.uploadedSyncAt = row.get("uploadedSyncAt").getAsLong();
            } catch (RuntimeException ignored) {
                saved.uploadedSyncAt = null;
            }
        }
        if (saved.uploaded == null) {
            saved.uploadedSyncAt = null;
        }
        return saved;
    }

    private static SmithSkill.Stand stand(JsonObject row, String levelField, String xpField) {
        if (!row.has(levelField) || !row.has(xpField)) {
            return null;
        }
        try {
            int level = row.get(levelField).getAsInt();
            long xp = row.get(xpField).getAsLong();
            if (!SmithSkill.fits(level, xp)) {
                return null;
            }
            return new SmithSkill.Stand(level, xp);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static final class Saved {
        private SmithSkill.Stand best;
        private SmithSkill.Stand uploaded;
        private Long uploadedSyncAt;
        private long retryDelay;
        private long nextAttemptAt;
        private long backoffSyncAt;
    }
}

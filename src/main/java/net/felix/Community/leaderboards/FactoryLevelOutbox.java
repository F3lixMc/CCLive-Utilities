package net.felix.Community.leaderboards;

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

/**
 * Höchstes Fabriklevel pro Konto und Seasonphase, plus das letzte bestätigte Uploadfenster.
 * Dateizugriffe passieren nur, wenn der Aufrufer sie anstößt.
 */
public final class FactoryLevelOutbox {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final Map<String, Map<String, Saved>> accounts = new LinkedHashMap<>();
    private final Map<String, Attempt> inflight = new LinkedHashMap<>();
    private int saves;

    public FactoryLevelOutbox(Path file) {
        this.file = file;
    }

    public record Attempt(String account, SmithSkillOutbox.Phase phase, int level, long nextSyncAt) {
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
    public void observe(String account, SmithSkillOutbox.Phase phase, Integer level) {
        if (account == null || account.isBlank() || phase == null || !better(level, null)) {
            return;
        }
        Saved saved = ensure(account, phase);
        if (!better(level, saved.best)) {
            return;
        }
        saved.best = level;
        save();
    }

    public Attempt begin(String account, SmithSkillOutbox.Phase phase, long now, SmithSkillOutbox.Clock clock, boolean allowed) {
        if (!allowed || account == null || account.isBlank() || phase == null || !SmithSkillOutbox.windowOpen(now, clock)) {
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
            long delay = saved.retryDelay <= 0L ? SmithSkillOutbox.RETRY_INITIAL_MS : Math.min(SmithSkillOutbox.RETRY_MAX_MS, saved.retryDelay * 2L);
            saved.retryDelay = delay;
            saved.nextAttemptAt = now + delay;
            saved.backoffSyncAt = attempt.nextSyncAt();
            return;
        }
        if (saved.uploaded != null
                && saved.uploaded.equals(attempt.level())
                && saved.uploadedSyncAt != null
                && saved.uploadedSyncAt == attempt.nextSyncAt()) {
            return;
        }
        saved.uploaded = attempt.level();
        saved.uploadedSyncAt = attempt.nextSyncAt();
        saved.retryDelay = 0L;
        saved.nextAttemptAt = 0L;
        save();
    }

    public Integer best(String account, SmithSkillOutbox.Phase phase) {
        Saved saved = existing(account, phase);
        return saved == null ? null : saved.best;
    }

    public Integer uploaded(String account, SmithSkillOutbox.Phase phase) {
        Saved saved = existing(account, phase);
        return saved == null ? null : saved.uploaded;
    }

    public Long uploadedSyncAt(String account, SmithSkillOutbox.Phase phase) {
        Saved saved = existing(account, phase);
        return saved == null ? null : saved.uploadedSyncAt;
    }

    public int saves() {
        return saves;
    }

    public boolean inFlight(String account) {
        return account != null && inflight.containsKey(account);
    }

    /** Entfernt den gespeicherten Höchststand dieser Phase, auch einen laufenden Versuch. */
    public void forget(String account, SmithSkillOutbox.Phase phase) {
        if (account == null || phase == null) {
            return;
        }
        Attempt current = inflight.get(account);
        if (current != null && phase.equals(current.phase())) {
            inflight.remove(account);
        }
        Map<String, Saved> phases = accounts.get(account);
        if (phases != null) {
            phases.remove(SmithSkillOutbox.key(phase));
            if (phases.isEmpty()) {
                accounts.remove(account);
            }
        }
        save();
    }

    static boolean better(Integer next, Integer current) {
        if (next == null || !FactoryLevel.fits(next)) {
            return false;
        }
        return current == null || next > current;
    }

    private Saved ensure(String account, SmithSkillOutbox.Phase phase) {
        Map<String, Saved> phases = accounts.computeIfAbsent(account, key -> new LinkedHashMap<>());
        return phases.computeIfAbsent(SmithSkillOutbox.key(phase), key -> new Saved());
    }

    private Saved existing(String account, SmithSkillOutbox.Phase phase) {
        if (account == null || phase == null) {
            return null;
        }
        Map<String, Saved> phases = accounts.get(account);
        if (phases == null) {
            return null;
        }
        return phases.get(SmithSkillOutbox.key(phase));
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
                row.addProperty("level", saved.best);
                if (saved.uploaded != null && saved.uploadedSyncAt != null) {
                    row.addProperty("uploadedLevel", saved.uploaded);
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
        Integer best = level(row, "level");
        if (best == null) {
            return null;
        }
        Saved saved = new Saved();
        saved.best = best;
        saved.uploaded = level(row, "uploadedLevel");
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

    private static Integer level(JsonObject row, String field) {
        if (!row.has(field) || row.get(field).isJsonNull()) {
            return null;
        }
        try {
            int value = row.get(field).getAsInt();
            return FactoryLevel.fits(value) ? value : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static final class Saved {
        private Integer best;
        private Integer uploaded;
        private Long uploadedSyncAt;
        private long retryDelay;
        private long nextAttemptAt;
        private long backoffSyncAt;
    }
}

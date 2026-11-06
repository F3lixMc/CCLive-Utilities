package net.felix.staff;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.http.HttpClient;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class StaffClientAccess {
    private static final long CACHE_MS = 3000L;
    private static volatile String cachedRole;
    private static volatile long cachedAt;
    private static final AtomicBoolean refreshing = new AtomicBoolean(false);
    private static final AtomicLong epoch = new AtomicLong();
    private static final AtomicReference<String> lastToken = new AtomicReference<>();

    private StaffClientAccess() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            token();
            if (System.currentTimeMillis() - cachedAt < CACHE_MS) return;
            refreshAsync();
        });
    }

    public static void invalidate() {
        String previous = cachedRole;
        cachedRole = null;
        cachedAt = 0L;
        epoch.incrementAndGet();
        if (!Objects.equals(previous, cachedRole)) {
            applyCommandVisibility();
        }
    }

    public static long epoch() {
        return epoch.get();
    }

    public static boolean isCurrent(long capturedEpoch, String capturedToken) {
        return capturedEpoch == epoch.get() && Objects.equals(capturedToken, token());
    }

    public static boolean isAdminCommandVisible() {
        return "admin".equals(cachedRole);
    }

    public static boolean isStaffCommandVisible() {
        return "admin".equals(cachedRole) || "moderator".equals(cachedRole);
    }

    public static boolean sanctionStoredWithoutLiveUpdate(JsonObject body) {
        if (body == null || !body.has("liveUpdated") || !body.get("liveUpdated").isJsonPrimitive()) {
            return false;
        }
        if (body.get("liveUpdated").getAsBoolean()) {
            return false;
        }
        return !body.has("stored") || !body.get("stored").isJsonPrimitive() || body.get("stored").getAsBoolean();
    }

    public static String sanctionStoredWarning(JsonObject body) {
        if (body != null && body.has("error") && body.get("error").isJsonPrimitive()) {
            String text = body.get("error").getAsString();
            if (!text.isBlank()) {
                return text;
            }
        }
        return "Die Sanktion ist gespeichert. Laufende Chatverbindungen wurden nicht aktualisiert.";
    }

    public static void refreshAsync() {
        if (!refreshing.compareAndSet(false, true)) return;
        CompletableFuture.runAsync(() -> {
            try {
                refresh();
            } finally {
                refreshing.set(false);
            }
        });
    }

    public static void refresh() {
        LeaderboardManager manager = LeaderboardManager.getInstance();
        String previousRole = cachedRole;
        if (manager == null || manager.getHttpClient() == null) {
            cachedRole = null;
            cachedAt = System.currentTimeMillis();
            notifyRoleChanged(previousRole, cachedRole);
            return;
        }
        String current = token();
        if (current == null || current.isBlank()) {
            cachedRole = null;
            cachedAt = System.currentTimeMillis();
            notifyRoleChanged(previousRole, cachedRole);
            return;
        }
        try {
            HttpClient.JsonResponse response = manager.getHttpClient().getResultWithToken("/moderation/staff/me", current);
            if (!Objects.equals(current, token())) {
                return;
            }
            if (response.status() == 401 || response.status() == 403) {
                cachedRole = null;
            } else if (response.status() == 200 && response.body() != null && response.body().has("staffRole")) {
                cachedRole = response.body().get("staffRole").getAsString().toLowerCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
            /* bekannte Rolle behalten, bis die nächste erfolgreiche Prüfung kommt */
        }
        cachedAt = System.currentTimeMillis();
        notifyRoleChanged(previousRole, cachedRole);
    }

    private static void notifyRoleChanged(String previous, String next) {
        if (Objects.equals(previous, next)) return;
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.execute(StaffClientAccess::applyCommandVisibility);
    }

    public static void applyCommandVisibility() {
        StaffClientCommandTree.applyVisibility();
    }

    public static String token() {
        LeaderboardManager manager = LeaderboardManager.getInstance();
        String current = manager == null ? null : manager.getPlayerToken();
        if (current != null && current.isBlank()) current = null;
        String previous = lastToken.getAndSet(current);
        if (!Objects.equals(previous, current)) {
            cachedRole = null;
            cachedAt = 0L;
            epoch.incrementAndGet();
            refreshAsync();
        }
        return current;
    }

    public static HttpClient http() {
        LeaderboardManager manager = LeaderboardManager.getInstance();
        return manager == null ? null : manager.getHttpClient();
    }

    public static String publicError(HttpClient.JsonResponse response, String fallback) {
        if (response == null) return fallback;
        String error = response.publicError();
        return error != null ? error : fallback;
    }

    public static String playerFrom(JsonObject body) {
        if (body != null && body.has("player") && body.get("player").isJsonPrimitive()) {
            return body.get("player").getAsString();
        }
        return null;
    }
}

package net.felix.identity;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lokaler Mojang-joinServer. Access-Token verlassen den Client nicht Richtung CCLive-Backend.
 * Ein Nachweisvorgang gleichzeitig (Mutex), unabhängig vom PlayLegend-Spielserver.
 */
public final class MinecraftIdentityProof {
    private static final AtomicBoolean BUSY = new AtomicBoolean(false);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public record SessionInfo(String name, UUID uuid, String accessToken) {}

    public static final class JoinFailedException extends IOException {
        private final int status;

        public JoinFailedException(int status) {
            super("Mojang-joinServer fehlgeschlagen (" + status + ")");
            this.status = status;
        }

        public int status() {
            return status;
        }
    }

    private MinecraftIdentityProof() {}

    public static boolean tryBegin() {
        return BUSY.compareAndSet(false, true);
    }

    public static void end() {
        BUSY.set(false);
    }

    public static boolean isBusy() {
        return BUSY.get();
    }

    public static SessionInfo currentSession() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }
        String name = null;
        UUID uuid = null;
        if (client.player != null) {
            name = client.player.getName().getString();
            uuid = client.player.getUUID();
        }
        User user = client.getUser();
        String accessToken = invokeString(user, "getAccessToken", "accessToken");
        if (uuid == null) {
            Object profileId = invoke(user, "getProfileId", "getUuid", "uuid");
            if (profileId instanceof UUID id) {
                uuid = id;
            }
        }
        if (name == null || name.isBlank()) {
            name = invokeString(user, "getName", "name");
        }
        if (name == null || uuid == null || accessToken == null || accessToken.isBlank()) {
            return null;
        }
        return new SessionInfo(name, uuid, accessToken);
    }

    public static void joinServer(String serverId) throws IOException, InterruptedException {
        SessionInfo session = currentSession();
        if (session == null) {
            throw new IOException("Kein Minecraft-Konto");
        }
        JsonObject body = new JsonObject();
        body.addProperty("accessToken", session.accessToken());
        body.addProperty("selectedProfile", session.uuid().toString().replace("-", ""));
        body.addProperty("serverId", serverId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/join"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status != 204 && status != 200) {
            throw new JoinFailedException(status);
        }
    }

    private static String invokeString(Object target, String... methods) {
        Object value = invoke(target, methods);
        return value != null ? String.valueOf(value) : null;
    }

    private static Object invoke(Object target, String... methods) {
        if (target == null) {
            return null;
        }
        for (String name : methods) {
            try {
                Method method = target.getClass().getMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }
}

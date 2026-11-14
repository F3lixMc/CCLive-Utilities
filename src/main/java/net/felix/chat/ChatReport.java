package net.felix.chat;

import com.google.gson.JsonObject;
import net.felix.leaderboards.http.HttpClient;
import net.felix.staff.StaffClientAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Meldung eines CCLive-Spielers. Der Server bestimmt den Melder aus dem Token.
 */
public final class ChatReport {
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private ChatReport() {
    }

    static boolean begin(String key) {
        return key != null && !key.isEmpty() && IN_FLIGHT.add(key);
    }

    static void finish(String key) {
        if (key != null) {
            IN_FLIGHT.remove(key);
        }
    }

    static JsonObject body(String player, Long messageId, String reason) {
        JsonObject body = new JsonObject();
        body.addProperty("player", player == null ? "" : player);
        if (messageId != null && messageId > 0L) {
            body.addProperty("messageId", messageId);
        }
        body.addProperty("reason", reason == null ? "" : reason);
        return body;
    }

    static String feedback(int status, String message, String error) {
        if (status == 200 && message != null && !message.isBlank()) {
            return message;
        }
        if (error != null && !error.isBlank()) {
            return error;
        }
        return status == 200 ? "Meldung gesendet." : "Meldung abgelehnt.";
    }

    public static void request(String player, Long messageId, String reason) {
        String name = player == null ? "" : player.trim();
        if (name.isEmpty()) {
            return;
        }
        String key = name.toLowerCase(java.util.Locale.ROOT) + "#" + (messageId == null ? 0L : messageId);
        if (!begin(key)) {
            return;
        }
        HttpClient http = StaffClientAccess.http();
        String token = StaffClientAccess.token();
        CompletableFuture.runAsync(() -> {
            try {
                if (http == null || token == null || token.isBlank()) {
                    tell(feedback(0, null, "Keine gültige Mod-Verbindung."));
                    return;
                }
                HttpClient.JsonResponse response = http.postResultWithToken(
                    "/moderation/reports", body(name, messageId, reason), token);
                String message = response.body() != null
                    && response.body().has("message")
                    && response.body().get("message").isJsonPrimitive()
                    ? response.body().get("message").getAsString()
                    : null;
                String error = StaffClientAccess.publicError(response, null);
                tell(feedback(response.status(), message, error));
            } catch (Exception e) {
                tell(feedback(0, null, "Server nicht erreichbar."));
            } finally {
                finish(key);
            }
        });
    }

    private static void tell(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.gui != null && client.gui.hud != null && client.gui.hud.getChat() != null) {
                client.gui.hud.getChat().addClientSystemMessage(Component.literal(text));
            }
        });
    }
}

package net.felix.utilities.Community.chat;

import com.google.gson.JsonObject;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.felix.utilities.Community.leaderboards.http.HttpClient;
import net.felix.utilities.Community.staff.StaffClientAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Löscht die angeklickte Nachricht direkt. Das Aktionsformular bleibt zu.
 */
public final class ChatMessageDelete {
    private static final Set<Long> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private ChatMessageDelete() {
    }

    static boolean begin(long messageId) {
        return messageId > 0L && IN_FLIGHT.add(messageId);
    }

    static void finish(long messageId) {
        IN_FLIGHT.remove(messageId);
    }

    static JsonObject body(long messageId) {
        JsonObject body = new JsonObject();
        body.addProperty("messageId", messageId);
        body.addProperty("reason", "");
        return body;
    }

    static String feedback(int status, String error, boolean already) {
        if (status == 200) {
            return already ? "Diese Nachricht ist bereits gelöscht." : "Nachricht gelöscht.";
        }
        if (error != null && !error.isBlank()) {
            return error;
        }
        return "Löschen abgelehnt.";
    }

    public static void request(Long messageId) {
        if (messageId == null || !begin(messageId)) {
            return;
        }
        long id = messageId;
        HttpClient http = StaffClientAccess.http();
        String token = StaffClientAccess.token();
        CompletableFuture.runAsync(() -> {
            try {
                if (http == null || token == null || token.isBlank()) {
                    tell(feedback(0, "Keine gültige Mod-Verbindung.", false));
                    return;
                }
                HttpClient.JsonResponse response = http.postResultWithToken(
                    "/moderation/actions/message/delete", body(id), token);
                boolean already = response.body() != null
                    && response.body().has("already")
                    && response.body().get("already").isJsonPrimitive()
                    && response.body().get("already").getAsBoolean();
                String error = StaffClientAccess.publicError(response, null);
                tell(feedback(response.status(), error, already));
            } catch (Exception e) {
                tell(feedback(0, "Server nicht erreichbar.", false));
            } finally {
                finish(id);
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

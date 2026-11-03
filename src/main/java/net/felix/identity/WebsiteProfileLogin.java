package net.felix.identity;

import com.google.gson.JsonObject;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.http.HttpClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class WebsiteProfileLogin {
    private WebsiteProfileLogin() {}

    public static void start(Minecraft client, String rawCode) {
        if (client.player == null) {
            return;
        }
        if (MinecraftIdentityProof.isBusy()) {
            client.player.sendSystemMessage(Component.literal("§cBitte warten — ein Nachweis läuft bereits."));
            return;
        }
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (code.length() < 4) {
            client.player.sendSystemMessage(Component.literal("§cUngültiger Code."));
            return;
        }
        CompletableFuture.runAsync(() -> run(client, code));
    }

    private static void run(Minecraft client, String code) {
        if (!MinecraftIdentityProof.tryBegin()) {
            tell(client, "§cBitte warten — ein Nachweis läuft bereits.");
            return;
        }
        try {
            MinecraftIdentityProof.SessionInfo session = MinecraftIdentityProof.currentSession();
            if (session == null) {
                tell(client, "§cKein Minecraft-Konto verfügbar.");
                return;
            }
            HttpClient http = LeaderboardManager.getInstance().getHttpClient();
            String token = LeaderboardManager.getInstance().getPlayerToken();
            if (token != null && !token.isBlank()) {
                JsonObject existingBody = new JsonObject();
                existingBody.addProperty("code", code);
                HttpClient.JsonResponse existing = http.postResultWithToken("/website/link/existing", existingBody, token);
                String existingCode = existing.body() != null && existing.body().has("code")
                    ? existing.body().get("code").getAsString()
                    : null;
                if (WebsiteLinkMessages.isAlreadyConnected(existing.status(), existingCode)) {
                    tell(client, "§e" + WebsiteLinkMessages.ALREADY_CONNECTED);
                    return;
                }
            }
            String proofSessionId = UUID.randomUUID().toString() + UUID.randomUUID();
            JsonObject challengeBody = new JsonObject();
            challengeBody.addProperty("code", code);
            challengeBody.addProperty("proofSessionId", proofSessionId);
            HttpClient.JsonResponse challenge = http.postResult("/website/link/challenge", challengeBody);
            if (challenge.body() == null || !challenge.body().has("serverId") || !challenge.body().has("challengeId")) {
                tell(client, messageFrom(challenge, "§cCode ungültig oder abgelaufen (HTTP " + challenge.status() + ")."));
                return;
            }
            try {
                MinecraftIdentityProof.joinServer(challenge.body().get("serverId").getAsString());
            } catch (MinecraftIdentityProof.JoinFailedException joinErr) {
                tell(client, WebsiteLinkMessages.mojangJoinFailure(joinErr.status()));
                return;
            }
            JsonObject confirm = new JsonObject();
            confirm.addProperty("code", code);
            confirm.addProperty("player", session.name());
            confirm.addProperty("uuid", session.uuid().toString());
            confirm.addProperty("challengeId", challenge.body().get("challengeId").getAsString());
            confirm.addProperty("proofSessionId", proofSessionId);
            HttpClient.JsonResponse result = http.postResult("/website/link/confirm", confirm);
            String resultCode = result.body() != null && result.body().has("code")
                ? result.body().get("code").getAsString()
                : null;
            if (WebsiteLinkMessages.isAlreadyConnected(result.status(), resultCode)) {
                tell(client, "§e" + WebsiteLinkMessages.ALREADY_CONNECTED);
            } else if (result.status() == 200) {
                tell(client, "§aProfil bestätigt. Du kannst das Passwort jetzt im Browser setzen.");
            } else if (result.status() == 409) {
                tell(client, messageFrom(result, "§cDieser Code wurde bereits bestätigt."));
            } else if (result.status() == 401) {
                tell(client, messageFrom(result, "§cFalscher Spieler oder Nachweis fehlgeschlagen."));
            } else {
                tell(client, messageFrom(result, "§cBestätigung fehlgeschlagen (HTTP " + result.status() + ")."));
            }
        } catch (MinecraftIdentityProof.JoinFailedException joinErr) {
            tell(client, WebsiteLinkMessages.mojangJoinFailure(joinErr.status()));
        } catch (Exception e) {
            tell(client, "§cBestätigung fehlgeschlagen: " + e.getMessage());
        } finally {
            MinecraftIdentityProof.end();
        }
    }

    private static String messageFrom(HttpClient.JsonResponse response, String fallback) {
        if (response != null) {
            String error = response.publicError();
            if (error != null) {
                return "§c" + error;
            }
        }
        return fallback;
    }

    private static void tell(Minecraft client, String message) {
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal(message));
            }
        });
    }
}

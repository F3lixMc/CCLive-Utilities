package net.felix.identity;

import com.google.gson.JsonObject;
import net.felix.leaderboards.http.HttpClient;

public final class ModRegistrationTransport implements PlayerRegistrationService.Transport {
    private final HttpClient http;

    public ModRegistrationTransport(HttpClient http) {
        this.http = http;
    }

    @Override
    public PlayerRegistrationService.Session session() {
        MinecraftIdentityProof.SessionInfo info = MinecraftIdentityProof.currentSession();
        if (info == null) {
            return null;
        }
        return new PlayerRegistrationService.Session(info.name(), info.uuid().toString());
    }

    @Override
    public PlayerRegistrationService.BackendCall challenge(String proofSessionId) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("proofSessionId", proofSessionId);
        return from(http.postResult("/register/challenge", body), false);
    }

    @Override
    public void joinServer(String serverId) throws Exception {
        if (!MinecraftIdentityProof.tryBegin()) {
            throw PlayerRegistrationService.JoinFailed.busy();
        }
        try {
            MinecraftIdentityProof.joinServer(serverId);
        } catch (MinecraftIdentityProof.JoinFailedException e) {
            throw PlayerRegistrationService.JoinFailed.http(e.status());
        } finally {
            MinecraftIdentityProof.end();
        }
    }

    @Override
    public PlayerRegistrationService.BackendCall register(String player, String uuid, String challengeId, String proofSessionId) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("player", player);
        body.addProperty("uuid", uuid);
        body.addProperty("challengeId", challengeId);
        body.addProperty("proofSessionId", proofSessionId);
        return from(http.postResult("/register", body), true);
    }

    private static PlayerRegistrationService.BackendCall from(HttpClient.JsonResponse response, boolean expectToken) {
        if (response == null) {
            return new PlayerRegistrationService.BackendCall(0, "keine Antwort", null, null, null, null, null);
        }
        JsonObject body = response.body();
        String error = null;
        String challengeId = null;
        String serverId = null;
        String token = null;
        String playerName = null;
        if (body != null) {
            if (body.has("error") && body.get("error").isJsonPrimitive()) {
                error = PlayerRegistrationService.publicErrorFromJson(body.get("error").getAsString());
            }
            if (body.has("challengeId")) {
                challengeId = body.get("challengeId").getAsString();
            }
            if (body.has("serverId")) {
                serverId = body.get("serverId").getAsString();
            }
            if (expectToken && body.has("token") && body.get("token").isJsonPrimitive()) {
                token = body.get("token").getAsString();
            }
            if (body.has("player") && body.get("player").isJsonPrimitive()) {
                playerName = body.get("player").getAsString();
            }
        }
        Integer retryAfter = null;
        int parsed = PlayerRegistrationService.parseRetryAfter(response.retryAfter());
        if (parsed >= 0) {
            retryAfter = parsed;
        }
        return new PlayerRegistrationService.BackendCall(
                response.status(),
                error,
                retryAfter,
                challengeId,
                serverId,
                token,
                playerName
        );
    }
}

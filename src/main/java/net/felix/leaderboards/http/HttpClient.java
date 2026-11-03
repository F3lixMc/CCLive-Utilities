package net.felix.leaderboards.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.felix.leaderboards.config.LeaderboardConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP-Client für die Kommunikation mit dem Leaderboard-Server
 */
public class HttpClient {
    private final java.net.http.HttpClient client;
    private final LeaderboardConfig config;
    private final Gson gson = new Gson();
    
    public HttpClient(LeaderboardConfig config) {
        this.config = config;
        this.client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(config.getConnectionTimeout()))
            .build();
    }

    public String getServerUrl() {
        return config.getServerUrl();
    }
    
    /**
     * Sendet eine GET-Anfrage
     */
    public JsonObject get(String endpoint) throws IOException, InterruptedException {
        return sendGet(endpoint, null);
    }

    /**
     * Sendet eine GET-Anfrage mit Auth-Token
     */
    public JsonObject getWithToken(String endpoint, String token) throws IOException, InterruptedException {
        return sendGet(endpoint, token);
    }

    private JsonObject sendGet(String endpoint, String token) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(config.getServerUrl() + endpoint))
            .timeout(Duration.ofMillis(config.getReadTimeout()))
            .GET();

        if (token != null) {
            requestBuilder.header("x-auth-token", token);
        }

        HttpRequest request = requestBuilder.build();
            
        if (config.isDebugMode()) {
            System.out.println("🌐 HTTP GET: " + config.getServerUrl() + endpoint);
        }
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (config.isDebugMode()) {
            System.out.println("📡 HTTP Response: " + response.statusCode() + " - " + response.body());
        }
        
        if (response.statusCode() == 200) {
            try {
                return gson.fromJson(response.body(), JsonObject.class);
            } catch (JsonSyntaxException e) {
                System.err.println("❌ JSON Parse Error: " + e.getMessage());
                return null;
            }
        } else {
            // 404 für Profil-Stats / Shares ist erwartbar - nicht loggen
            if (response.statusCode() == 404
                    && (endpoint.contains("/profile/") || endpoint.contains("/share/"))) {
                return null;
            }
            System.err.println("❌ HTTP Error " + response.statusCode() + ": " + response.body());
            return null;
        }
    }
    
    /**
     * Sendet eine GET-Anfrage und gibt ein JSON-Array zurück
     */
    public JsonArray getArray(String endpoint) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getServerUrl() + endpoint))
            .timeout(Duration.ofMillis(config.getReadTimeout()))
            .GET()
            .build();
            
        if (config.isDebugMode()) {
            System.out.println("🌐 HTTP GET: " + config.getServerUrl() + endpoint);
        }
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (config.isDebugMode()) {
            System.out.println("📡 HTTP Response: " + response.statusCode() + " - " + response.body());
        }
        
        if (response.statusCode() == 200) {
            try {
                return gson.fromJson(response.body(), JsonArray.class);
            } catch (JsonSyntaxException e) {
                System.err.println("❌ JSON Parse Error: " + e.getMessage());
                return null;
            }
        } else {
            System.err.println("❌ HTTP Error " + response.statusCode() + ": " + response.body());
            return null;
        }
    }
    
    /**
     * Sendet eine POST-Anfrage ohne Token
     */
    public JsonObject post(String endpoint, JsonObject data) throws IOException, InterruptedException {
        JsonResponse response = sendPostResult(endpoint, data, null);
        if (response.status() == 200 || response.status() == 201) {
            return response.body();
        }
        logHttpFailure("POST", endpoint, response);
        return null;
    }

    public JsonResponse postResult(String endpoint, JsonObject data) throws IOException, InterruptedException {
        return sendPostResult(endpoint, data, null);
    }

    public JsonResponse postResultWithToken(String endpoint, JsonObject data, String token) throws IOException, InterruptedException {
        return sendPostResult(endpoint, data, token);
    }

    public record JsonResponse(int status, JsonObject body, String raw, String retryAfter) {
        public String publicError() {
            if (body != null && body.has("error") && body.get("error").isJsonPrimitive()) {
                String error = body.get("error").getAsString();
                String lower = error.toLowerCase();
                if (lower.contains("token") || lower.contains("secret") || lower.contains("password")
                        || lower.contains("proofsession") || lower.contains("accesstoken")) {
                    return null;
                }
                return error;
            }
            return null;
        }
    }
    
    /**
     * Sendet eine POST-Anfrage mit Token
     */
    public JsonObject postWithToken(String endpoint, JsonObject data, String token) throws IOException, InterruptedException {
        return sendPost(endpoint, data, token);
    }
    
    /**
     * Sendet eine DELETE-Anfrage mit Token
     */
    public JsonObject deleteWithToken(String endpoint, String token) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(config.getServerUrl() + endpoint))
            .timeout(Duration.ofMillis(config.getReadTimeout()))
            .header("Content-Type", "application/json");
            
        // Token hinzufügen falls vorhanden
        if (token != null) {
            requestBuilder.header("x-auth-token", token);
        }
        
        HttpRequest request = requestBuilder
            .DELETE()
            .build();
            
        if (config.isDebugMode()) {
            System.out.println("🌐 HTTP DELETE: " + config.getServerUrl() + endpoint);
            if (token != null) {
                System.out.println("🔐 With Token: vorhanden");
            }
        }
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (config.isDebugMode()) {
            System.out.println("📡 HTTP Response: " + response.statusCode() + " - " + response.body());
        }
        
        if (response.statusCode() == 200 || response.statusCode() == 204) {
            try {
                if (response.body() != null && !response.body().trim().isEmpty()) {
                    return gson.fromJson(response.body(), JsonObject.class);
                } else {
                    // Leere Antwort ist OK für DELETE
                    JsonObject emptyResponse = new JsonObject();
                    emptyResponse.addProperty("success", true);
                    return emptyResponse;
                }
            } catch (JsonSyntaxException e) {
                System.err.println("❌ JSON Parse Error: " + e.getMessage());
                return null;
            }
        } else {
            System.err.println("❌ HTTP Error " + response.statusCode() + ": " + response.body());
            return null;
        }
    }
    
    /**
     * Interne Methode für POST-Anfragen
     */
    private JsonObject sendPost(String endpoint, JsonObject data, String token) throws IOException, InterruptedException {
        JsonResponse response = sendPostResult(endpoint, data, token);
        if (response.status() == 200 || response.status() == 201) {
            return response.body();
        }
        logHttpFailure("POST", endpoint, response);
        return null;
    }

    private JsonResponse sendPostResult(String endpoint, JsonObject data, String token) throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(config.getServerUrl() + endpoint))
            .timeout(Duration.ofMillis(config.getReadTimeout()))
            .header("Content-Type", "application/json");

        if (token != null) {
            requestBuilder.header("x-auth-token", token);
        }

        String jsonBody = gson.toJson(data);
        HttpRequest request = requestBuilder
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        if (config.isDebugMode()) {
            System.out.println("🌐 HTTP POST: " + config.getServerUrl() + endpoint);
            if (!isSensitiveEndpoint(endpoint)) {
                System.out.println("📤 Request Body: " + jsonBody);
            }
            if (token != null) {
                System.out.println("🔐 With Token: vorhanden");
            }
        }

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject parsed = null;
        try {
            if (response.body() != null && !response.body().isBlank()) {
                parsed = gson.fromJson(response.body(), JsonObject.class);
            }
        } catch (JsonSyntaxException e) {
            System.err.println("❌ JSON Parse Error: " + e.getMessage());
        }
        if (config.isDebugMode()) {
            if (isSensitiveEndpoint(endpoint)) {
                System.out.println("📡 HTTP Response: " + response.statusCode() + " (Inhalt unterdrückt)");
            } else {
                System.out.println("📡 HTTP Response: " + response.statusCode() + " - " + response.body());
            }
        }
        String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
        return new JsonResponse(response.statusCode(), parsed, response.body(), retryAfter);
    }

    private static boolean isSensitiveEndpoint(String endpoint) {
        String path = endpoint == null ? "" : endpoint.toLowerCase();
        return path.contains("/register")
                || path.contains("/website/")
                || path.contains("/login")
                || path.contains("/challenge")
                || path.contains("/confirm");
    }

    private static void logHttpFailure(String method, String endpoint, JsonResponse response) {
        String error = response.publicError();
        if (isSensitiveEndpoint(endpoint)) {
            System.err.println("❌ HTTP Error " + method + " " + endpoint + " " + response.status()
                    + (error != null ? " " + error : ""));
            return;
        }
        System.err.println("❌ HTTP Error " + response.status() + (response.raw() != null ? ": " + response.raw() : ""));
    }
}

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
    
    /**
     * Sendet eine GET-Anfrage
     */
    public JsonObject get(String endpoint) throws IOException, InterruptedException {
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
                return gson.fromJson(response.body(), JsonObject.class);
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
        return sendPost(endpoint, data, null);
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
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(config.getServerUrl() + endpoint))
            .timeout(Duration.ofMillis(config.getReadTimeout()))
            .header("Content-Type", "application/json");
            
        // Token hinzufügen falls vorhanden
        if (token != null) {
            requestBuilder.header("x-auth-token", token);
        }
        
        String jsonBody = gson.toJson(data);
        HttpRequest request = requestBuilder
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
            
        if (config.isDebugMode()) {
            System.out.println("🌐 HTTP POST: " + config.getServerUrl() + endpoint);
            System.out.println("📤 Request Body: " + jsonBody);
            if (token != null) {
                System.out.println("🔐 With Token: vorhanden");
            }
        }
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (config.isDebugMode()) {
            System.out.println("📡 HTTP Response: " + response.statusCode() + " - " + response.body());
        }
        
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            try {
                return gson.fromJson(response.body(), JsonObject.class);
            } catch (JsonSyntaxException e) {
                System.err.println("❌ JSON Parse Error: " + e.getMessage());
                return null;
            }
        } else {
            System.err.println("❌ HTTP Error " + response.statusCode() + ": " + response.body());
            return null;
        }
    }
}

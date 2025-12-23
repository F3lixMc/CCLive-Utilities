package net.felix.utilities.Other;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.config.LeaderboardConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class UpdateCheckerUtility {
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/nBXDNiuw/version";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();
    private static final LeaderboardConfig config = new LeaderboardConfig();
    
    private static boolean updateCheckCompleted = false;
    private static boolean updateAvailable = false;
    private static String latestVersion = "";
    
    public static void initialize() {
        // Registriere den Update-Check beim Server-Beitritt
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!updateCheckCompleted) {
                checkForUpdates();
            }
        });
    }
    
    private static String getCurrentVersion() {
        try {
            return FabricLoader.getInstance().getModContainer("cclive-utilities")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("1.4.2"); // Fallback falls das Laden fehlschlägt
        } catch (Exception e) {
            System.out.println("Fehler beim Laden der Mod-Version: " + e.getMessage());
            return "1.4.2"; // Fallback
        }
    }
    
    private static void checkForUpdates() {
        // Prüfe ob der Update-Checker aktiviert ist
        if (!CCLiveUtilitiesConfig.HANDLER.instance().updateCheckerEnabled) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                // Erstelle HTTP-Request
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MODRINTH_API_URL))
                    .header("User-Agent", "CCLive-Utilities-UpdateChecker/1.0")
                    .GET()
                    .build();
                
                // Sende Request und hole Response
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    parseUpdateResponse(response.body());
                } else {
                    System.out.println("Update-Check fehlgeschlagen: HTTP " + response.statusCode());
                }
                
            } catch (IOException | InterruptedException e) {
                System.out.println("Fehler beim Update-Check: " + e.getMessage());
            }
        });
    }
    
    private static void parseUpdateResponse(String jsonResponse) {
        try {
            JsonArray versions = gson.fromJson(jsonResponse, JsonArray.class);
            String currentVersion = getCurrentVersion();
            
            if (versions != null && versions.size() > 0) {
                // Finde die neueste stabile Version
                for (JsonElement element : versions) {
                    JsonObject version = element.getAsJsonObject();
                    String versionNumber = version.get("version_number").getAsString();
                    String versionType = version.get("version_type").getAsString();
                    
                    // Nur stabile Versionen berücksichtigen
                    if ("release".equals(versionType)) {
                        if (isNewerVersion(versionNumber, currentVersion)) {
                            latestVersion = versionNumber;
                            updateAvailable = true;
                            break;
                        }
                    }
                }
                
                // Zeige Update-Nachricht im Chat
                if (updateAvailable) {
                    showUpdateMessage();
                }
            }
            
        } catch (Exception e) {
            System.out.println("Fehler beim Parsen der Update-Response: " + e.getMessage());
        }
        
        updateCheckCompleted = true;
    }
    
    private static boolean isNewerVersion(String newVersion, String currentVersion) {
        // Korrekte Versionsvergleich-Logik
        String[] newParts = newVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");
        
        int maxLength = Math.max(newParts.length, currentParts.length);
        
        for (int i = 0; i < maxLength; i++) {
            int newPart = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            
            if (newPart > currentPart) {
                return true;
            } else if (newPart < currentPart) {
                return false;
            }
        }
        
        // Versionen sind identisch
        return false;
    }
    
    private static void showUpdateMessage() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // Erstelle einfache Nachricht
            Text updateMessage = Text.literal("§b[CCLive-Utilities] §fNeue Version verfügbar: §a" + latestVersion);
            
            // Erstelle klickbare Nachricht
            MutableText infoMessage = Text.literal("§7Neuste Version hier: §bhttps://modrinth.com/mod/cclive-utilities");
            infoMessage.setStyle(infoMessage.getStyle()
                .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://modrinth.com/mod/cclive-utilities")))
            );
            
            // Sende Nachrichten an den Spieler
            client.player.sendMessage(updateMessage, false);
            client.player.sendMessage(infoMessage, false);
            
            // Hole zusätzliche Update-Message vom Server (falls vorhanden)
            fetchAndShowServerMessage();
        }
    }
    
    /**
     * Holt die Update-Message vom Server und zeigt sie an (falls vorhanden)
     */
    private static void fetchAndShowServerMessage() {
        CompletableFuture.runAsync(() -> {
            try {
                String serverUrl = config.getServerUrl();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/update-message"))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "CCLive-Utilities-UpdateChecker/1.0")
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    if (json != null && json.has("message")) {
                        String message = json.get("message").getAsString();
                        if (message != null && !message.trim().isEmpty()) {
                            // Zeige Server-Message im Chat (mehrzeilig unterstützt)
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.player != null) {
                                // Splitte nach Zeilenumbrüchen (\n) und sende jede Zeile als separate Nachricht
                                String[] lines = message.split("\\n");
                                for (int i = 0; i < lines.length; i++) {
                                    if (i == 0) {
                                        // Erste Zeile: Komplett gelb (Update-Info + Text)
                                        Text serverMessage = Text.literal("§e[Update-Info] " + lines[i]);
                                        client.player.sendMessage(serverMessage, false);
                                    } else {
                                        // Weitere Zeilen: Komplett aqua
                                        Text serverMessage = Text.literal("§b" + lines[i]);
                                        client.player.sendMessage(serverMessage, false);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Silent fail - wenn Server nicht erreichbar ist, einfach ignorieren
                // System.out.println("Fehler beim Abrufen der Server-Update-Message: " + e.getMessage());
            }
        });
    }
    
    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public static String getLatestVersion() {
        return latestVersion;
    }
}

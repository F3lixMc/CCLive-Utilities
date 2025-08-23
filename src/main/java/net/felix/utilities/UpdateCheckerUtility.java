package net.felix.utilities;

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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class UpdateCheckerUtility {
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/nBXDNiuw/version";
    private static final String CURRENT_VERSION = "1.4.3"; // Diese Version aus der fabric.mod.json lesen
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();
    
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
            
            if (versions != null && versions.size() > 0) {
                // Finde die neueste stabile Version
                for (JsonElement element : versions) {
                    JsonObject version = element.getAsJsonObject();
                    String versionNumber = version.get("version_number").getAsString();
                    String versionType = version.get("version_type").getAsString();
                    
                    // Nur stabile Versionen berücksichtigen
                    if ("release".equals(versionType)) {
                        if (isNewerVersion(versionNumber, CURRENT_VERSION)) {
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
        // Einfacher Versionsvergleich (kann erweitert werden)
        return !newVersion.equals(currentVersion);
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
        }
    }
    
    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public static String getLatestVersion() {
        return latestVersion;
    }
}

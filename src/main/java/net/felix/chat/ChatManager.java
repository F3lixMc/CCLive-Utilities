package net.felix.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.felix.leaderboards.http.HttpClient;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.chat.config.ChatConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manager für das CCLive Chat-System
 * Verwaltet das Senden und Empfangen von Chat-Nachrichten über den Server
 */
public class ChatManager {
    private static ChatManager instance;
    
    private final ChatConfig config;
    private final HttpClient httpClient;
    
    // Letzter abgerufener Timestamp (für Polling)
    private final AtomicLong lastMessageTimestamp = new AtomicLong(0);
    
    // Chat-Sichtbarkeit
    private boolean showDefaultChat = true;
    private boolean showCCLiveChat = true;
    
    // Aktueller Chat-Modus (default oder cclive)
    private ChatMode currentChatMode = ChatMode.DEFAULT;
    
    // Status
    private boolean isEnabled = false; // Vorübergehend deaktiviert bis vollständig einsatzbereit
    private boolean isInitialized = false;
    
    public enum ChatMode {
        DEFAULT,
        CCLIVE
    }
    
    private ChatManager() {
        this.config = new ChatConfig();
        // Nutze den HttpClient vom LeaderboardManager
        LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
        this.httpClient = leaderboardManager.getHttpClient();
        loadChatSettings();
    }
    
    public static ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }
    
    /**
     * Initialisiert das Chat-System
     */
    public void initialize() {
        if (isInitialized) {
            return;
        }
        
        // Überspringe Initialisierung wenn Chat deaktiviert ist
        if (!isEnabled) {
            System.out.println("⚠️ ChatManager: Chat-System ist deaktiviert");
            isInitialized = true; // Markiere als initialisiert, um erneute Versuche zu vermeiden
            return;
        }
        
        // Starte Polling für neue Nachrichten
        startMessagePolling();
        
        // Registriere Server-Join Event
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Setze Timestamp auf aktuelle Zeit beim Join (keine alten Nachrichten laden)
            long joinTimestamp = System.currentTimeMillis();
            lastMessageTimestamp.set(joinTimestamp);
            System.out.println("[CCLive-Utilities] 🎮 Server-Join: Lade nur Nachrichten ab jetzt");
            // Lade keine Historie beim Join - nur neue Nachrichten ab jetzt
        });
        
        isInitialized = true;
        System.out.println("✅ ChatManager initialisiert");
    }
    
    /**
     * Sendet eine Nachricht an den CCLive Chat
     */
    public CompletableFuture<Boolean> sendMessage(String message) {
        if (!isEnabled) {
            return CompletableFuture.completedFuture(false);
        }
        
        LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
        if (!leaderboardManager.isRegistered() || leaderboardManager.getPlayerToken() == null) {
            System.err.println("⚠️ Chat: Spieler nicht registriert");
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestData = new JsonObject();
                requestData.addProperty("message", message);
                
                JsonObject response = httpClient.postWithToken(
                    "/chat/send",
                    requestData,
                    leaderboardManager.getPlayerToken()
                );
                
                if (response != null && response.has("success")) {
                    return true;
                }
            } catch (Exception e) {
                System.err.println("❌ Fehler beim Senden der Chat-Nachricht: " + e.getMessage());
            }
            return false;
        });
    }
    
    /**
     * Lädt neue Chat-Nachrichten vom Server
     */
    private void loadChatHistory() {
        if (!isEnabled || !showCCLiveChat) {
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                long since = lastMessageTimestamp.get();
                String endpoint = "/chat/messages?since=" + since;
                
                JsonArray messages = httpClient.getArray(endpoint);
                if (messages != null) {
                    processMessages(messages);
                }
            } catch (Exception e) {
                // Silently fail - Server könnte nicht erreichbar sein
            }
            return null;
        });
    }
    
    /**
     * Verarbeitet empfangene Nachrichten und zeigt sie im Chat an
     */
    private void processMessages(JsonArray messages) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        
        for (int i = 0; i < messages.size(); i++) {
            JsonObject msgObj = messages.get(i).getAsJsonObject();
            String player = msgObj.get("player").getAsString();
            String message = msgObj.get("message").getAsString();
            long timestamp = msgObj.get("timestamp").getAsLong();
            
            // Update last timestamp
            if (timestamp > lastMessageTimestamp.get()) {
                lastMessageTimestamp.set(timestamp);
            }
            
            // Erstelle Chat-Nachricht mit [CCLive] Prefix
            // Farbe #d478f0 = RGB(212, 120, 240) = 0xFFD478F0
            // Das Icon wird im ChatHudRenderMixin direkt nach "[CCLive]" gerendert
            MutableText chatMessage = Text.literal("[CCLive] ")
                .setStyle(Style.EMPTY.withColor(0xD478F0)) // #d478f0 (ohne Alpha, Minecraft fügt FF hinzu)
                .append(Text.literal(player + ": ").setStyle(Style.EMPTY.withFormatting(Formatting.WHITE)))
                .append(Text.literal(message).setStyle(Style.EMPTY.withFormatting(Formatting.WHITE)));
            
            // Zeige Nachricht im Chat an
            if (client.inGameHud != null && client.inGameHud.getChatHud() != null) {
                client.execute(() -> {
                    client.inGameHud.getChatHud().addMessage(chatMessage);
                });
            }
        }
    }
    
    /**
     * Startet das Polling für neue Nachrichten
     */
    private void startMessagePolling() {
        // Poll alle 2 Sekunden für neue Nachrichten
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !isEnabled || !showCCLiveChat) {
                return;
            }
            
            // Alle 20 Ticks (1 Sekunde) neue Nachrichten abrufen
            if (client.player.age > 0 && client.player.age % 20 == 0) {
                loadChatHistory();
            }
        });
    }
    
    /**
     * Lädt Chat-Einstellungen (Toggle-Status)
     */
    private void loadChatSettings() {
        // TODO: Später aus Config-Datei laden
        // Für jetzt: Standardwerte (beide Chats an)
        showDefaultChat = true;
        showCCLiveChat = true;
        currentChatMode = ChatMode.DEFAULT;
    }
    
    /**
     * Speichert Chat-Einstellungen
     */
    private void saveChatSettings() {
        // TODO: Später in Config-Datei speichern
    }
    
    /**
     * Wechselt den Chat-Modus
     */
    public void setChatMode(ChatMode mode) {
        this.currentChatMode = mode;
        saveChatSettings();
    }
    
    /**
     * Togglet die Sichtbarkeit eines Chat-Kanals
     */
    public void toggleChat(String chatType) {
        if (chatType.equalsIgnoreCase("default")) {
            boolean wasVisible = showDefaultChat;
            showDefaultChat = !showDefaultChat;
            
            // Prüfe: Wenn beide Chats jetzt aus wären, aktiviere den anderen wieder
            if (!showDefaultChat && !showCCLiveChat) {
                showCCLiveChat = true;
            }
            
            // Wenn Default deaktiviert wurde, wechsle automatisch zu CCLIVE
            if (wasVisible && !showDefaultChat && showCCLiveChat) {
                currentChatMode = ChatMode.CCLIVE;
            }
            // Wenn Default aktiviert wurde und wir im CCLIVE Modus sind, wechsle zu DEFAULT
            else if (!wasVisible && showDefaultChat && currentChatMode == ChatMode.CCLIVE) {
                currentChatMode = ChatMode.DEFAULT;
            }
        } else if (chatType.equalsIgnoreCase("cclive")) {
            boolean wasVisible = showCCLiveChat;
            showCCLiveChat = !showCCLiveChat;
            
            // Prüfe: Wenn beide Chats jetzt aus wären, aktiviere den anderen wieder
            if (!showDefaultChat && !showCCLiveChat) {
                showDefaultChat = true;
            }
            
            // Wenn CCLIVE deaktiviert wurde, wechsle automatisch zu DEFAULT
            if (wasVisible && !showCCLiveChat && showDefaultChat) {
                currentChatMode = ChatMode.DEFAULT;
            }
            // Wenn CCLIVE aktiviert wurde und wir im DEFAULT Modus sind, wechsle zu CCLIVE
            else if (!wasVisible && showCCLiveChat && currentChatMode == ChatMode.DEFAULT) {
                currentChatMode = ChatMode.CCLIVE;
            }
        }
        saveChatSettings();
    }
    
    /**
     * Prüft ob ein Chat-Kanal sichtbar ist
     */
    public boolean isChatVisible(String chatType) {
        if (chatType.equalsIgnoreCase("default")) {
            return showDefaultChat;
        } else if (chatType.equalsIgnoreCase("cclive")) {
            return showCCLiveChat;
        }
        return true;
    }
    
    
    // Getter
    public ChatMode getCurrentChatMode() { return currentChatMode; }
    public boolean isShowDefaultChat() { return showDefaultChat; }
    public boolean isShowCCLiveChat() { return showCCLiveChat; }
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { this.isEnabled = enabled; }
}


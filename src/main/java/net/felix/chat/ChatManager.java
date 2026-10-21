package net.felix.chat;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.LeaderboardManager;
import net.minecraft.client.Minecraft;
import net.felix.profile.PlayerHoverStatsUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manager für das CCLive Chat-System.
 * Transport: WebSocket (Send + Push). Item/Build-Shares bleiben HTTP.
 */
public class ChatManager {
    private static ChatManager instance;

    /** Gleiche Glyphe wie im normalen Chat → Custom-Font rendert das Icon mit Text-Opacity. */
    private static final char ICON_MARKER = 'ѳ';
    private static final Identifier ICON_FONT = Identifier.fromNamespaceAndPath("cclive-utilities", "default");

    /** Prefix-Farbe (#9b029e) — dunkles Lila. */
    private static final int CCLIVE_PREFIX_COLOR = 0x9B029E;
    /** Name / >> / Nachricht (#fba9fc). */
    private static final int CCLIVE_TEXT_COLOR = ItemShareManager.CCLIVE_MESSAGE_COLOR;

    private final ChatWebSocket webSocket = new ChatWebSocket();

    /** Deduplizierung für eingehende WS-Nachrichten. */
    private final AtomicLong lastMessageTimestamp = new AtomicLong(0);
    private final Object displayDedupLock = new Object();
    private volatile String lastDisplayedDedupKey = "";
    private volatile long lastDisplayedAtMs;
    private volatile long lastSendAtMs;
    private volatile String lastSendPayload = "";

    private boolean showDefaultChat = true;
    private boolean showCCLiveChat = true;
    private ChatMode currentChatMode = ChatMode.DEFAULT;

    private boolean isEnabled = true;
    private boolean isInitialized = false;

    public enum ChatMode {
        DEFAULT,
        CCLIVE
    }

    private ChatManager() {
        loadChatSettings();
        webSocket.setOnChatMessage(this::onIncomingChatMessage);
    }

    public static ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }

    public void initialize() {
        if (isInitialized) {
            return;
        }

        // Config ist zu diesem Zeitpunkt geladen (inkl. Migration)
        loadChatSettings();

        if (!isEnabled) {
            isInitialized = true;
            return;
        }

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Sichtbarkeit/Schreibmodus bleiben aus Config — nur WS neu verbinden
            lastMessageTimestamp.set(0);
            client.execute(this::ensureSocketConnected);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(webSocket::disconnect));

        // Periodisch Verbindung halten / reconnecten
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !isEnabled || !showCCLiveChat) {
                return;
            }
            if (client.player.tickCount > 0 && client.player.tickCount % 100 == 0) {
                ensureSocketConnected();
            }
        });

        ensureSocketConnected();
        isInitialized = true;
    }

    private void ensureSocketConnected() {
        if (!isEnabled || !showCCLiveChat) {
            return;
        }
        if (!CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            // Bypass/unbekannte IP oder falscher Server: keine Verbindung zum Backend
            if (webSocket.isConnected()) {
                webSocket.disconnect();
            }
            return;
        }
        webSocket.connectIfNeeded();
    }

    /**
     * Sendet eine Nachricht an den CCLive Chat (WebSocket).
     * Ersetzt !item / !build durch Share-Ref wenn nötig.
     * @return null bei Erfolg, sonst Fehlermeldung
     */
    public CompletableFuture<String> sendMessage(String message) {
        if (!isEnabled) {
            return CompletableFuture.completedFuture("CCLive-Chat ist deaktiviert.");
        }

        if (message == null || message.trim().isEmpty()) {
            return CompletableFuture.completedFuture("Nachricht ist leer.");
        }

        StringBuilder prepared = new StringBuilder();
        String prepareError = ItemShareManager.getInstance().prepareCcliveOutgoing(message.trim(), prepared);
        if (prepareError != null) {
            return CompletableFuture.completedFuture(prepareError);
        }
        final String trimmed = prepared.toString();

        // Doppel-Enter / doppelter Mixin-Aufruf: gleiche Payload innerhalb 750ms nur einmal senden
        long now = System.currentTimeMillis();
        if (trimmed.equals(lastSendPayload) && now - lastSendAtMs < 750L) {
            return CompletableFuture.completedFuture(null);
        }
        lastSendPayload = trimmed;
        lastSendAtMs = now;

        if (!CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            return CompletableFuture.completedFuture(
                "Datentracking ist aus — Chat-Senden braucht Registrierung. In den Mod-Einstellungen aktivieren."
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            LeaderboardManager leaderboardManager = LeaderboardManager.getInstance();
            if (!leaderboardManager.ensureRegisteredSync()) {
                return "Nicht registriert. Warte kurz nach Server-Join oder nutze /cclive lb refresh.";
            }
            return null;
        }).thenCompose(regError -> {
            if (regError != null) {
                return CompletableFuture.completedFuture(regError);
            }
            ensureSocketConnected();
            return webSocket.sendChat(trimmed);
        });
    }

    private void onIncomingChatMessage(JsonObject msgObj) {
        if (!isEnabled || !showCCLiveChat) {
            return;
        }
        if (msgObj == null || !msgObj.has("player") || !msgObj.has("message")) {
            return;
        }

        String player = msgObj.get("player").getAsString();
        String message = msgObj.get("message").getAsString();
        long timestamp = msgObj.has("timestamp") ? msgObj.get("timestamp").getAsLong() : System.currentTimeMillis();
        int nameColor = ChatNameColorTracker.parseNameColor(
            msgObj.has("name_color") && !msgObj.get("name_color").isJsonNull()
                ? msgObj.get("name_color").getAsString()
                : null);
        int cactusRank = parseRankField(msgObj, "cactus_rank");
        int soulRank = parseRankField(msgObj, "soul_rank");

        // Dedup: doppelte WS-Pushs / Race bei zwei Sockets (gleicher Spieler+Text kurz hintereinander)
        String softKey = player + "|" + message;
        String exactKey = softKey + "|" + timestamp;
        synchronized (displayDedupLock) {
            long now = System.currentTimeMillis();
            if (exactKey.equals(lastDisplayedDedupKey)
                    || (softKey.equals(stripTimestamp(lastDisplayedDedupKey))
                        && now - lastDisplayedAtMs < 2000L)) {
                return;
            }
            long last = lastMessageTimestamp.get();
            if (timestamp > 0 && timestamp < last) {
                return;
            }
            if (timestamp >= last) {
                lastMessageTimestamp.set(timestamp);
            }
            lastDisplayedDedupKey = exactKey;
            lastDisplayedAtMs = now;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        HoverEvent statsHover = null;
        if (PlayerHoverStatsUtility.isInitialized()) {
            Component hoverText = PlayerHoverStatsUtility.createStatsHoverForPlayer(
                player, cactusRank, soulRank);
            if (hoverText != null) {
                statsHover = PlayerHoverStatsUtility.createHoverEvent(hoverText);
            }
        }
        final HoverEvent statsHoverFinal = statsHover;

        // Body OHNE Stats-Hover — sonst vererbt sich Hover vom Root auf Item-/Build-Links
        ItemShareManager.getInstance().buildMessageBodyAsync(message, null).thenAccept(body -> {
            // Stats-Hover NUR auf den Blatt-Segmenten [CCLive]/Name/Icon/>> — nie am Root
            Style prefixStyle = Style.EMPTY.withColor(CCLIVE_PREFIX_COLOR);
            Style nameStyle = Style.EMPTY.withColor(nameColor);
            Style arrowStyle = Style.EMPTY.withColor(0xFFFFFF);
            if (statsHoverFinal != null) {
                prefixStyle = prefixStyle.withHoverEvent(statsHoverFinal);
                nameStyle = nameStyle.withHoverEvent(statsHoverFinal);
                arrowStyle = arrowStyle.withHoverEvent(statsHoverFinal);
            }

            MutableComponent chatMessage = Component.empty();
            chatMessage.append(Component.literal("[CCLive] ").setStyle(prefixStyle));
            chatMessage.append(Component.literal(player).setStyle(nameStyle));

            if (CCLiveUtilitiesConfig.HANDLER.instance().chatIconEnabled
                    && CCLiveUtilitiesConfig.isModRuntimeEnabled()) {
                Style iconStyle = Style.EMPTY
                    .withFont(new FontDescription.Resource(ICON_FONT))
                    .withColor(0xFFFFFF);
                if (statsHoverFinal != null) {
                    iconStyle = iconStyle.withHoverEvent(statsHoverFinal);
                }
                chatMessage.append(Component.literal(" ").setStyle(arrowStyle));
                chatMessage.append(Component.literal(String.valueOf(ICON_MARKER)).setStyle(iconStyle));
            }

            chatMessage.append(Component.literal(">> ").setStyle(arrowStyle));
            chatMessage.append(body);

            client.execute(() -> {
                if (client.gui != null && client.gui.getChat() != null) {
                    client.gui.getChat().addClientSystemMessage(chatMessage);
                }
            });
        });
    }

    private static int parseRankField(JsonObject msgObj, String key) {
        if (msgObj == null || key == null || !msgObj.has(key) || msgObj.get(key).isJsonNull()) {
            return 0;
        }
        try {
            return Math.max(0, msgObj.get(key).getAsInt());
        } catch (Exception e) {
            return 0;
        }
    }

    private void loadChatSettings() {
        CCLiveUtilitiesConfig cfg = CCLiveUtilitiesConfig.HANDLER.instance();
        showDefaultChat = cfg.chatShowDefault;
        showCCLiveChat = cfg.chatShowCCLive;
        // Mindestens ein Chat muss an sein
        if (!showDefaultChat && !showCCLiveChat) {
            showDefaultChat = true;
            cfg.chatShowDefault = true;
        }
        currentChatMode = parseWriteMode(cfg.chatWriteMode);
        currentChatMode = clampWriteMode(currentChatMode);
    }

    private void saveChatSettings() {
        CCLiveUtilitiesConfig cfg = CCLiveUtilitiesConfig.HANDLER.instance();
        cfg.chatShowDefault = showDefaultChat;
        cfg.chatShowCCLive = showCCLiveChat;
        cfg.chatWriteMode = currentChatMode == ChatMode.CCLIVE ? "cclive" : "default";
        CCLiveUtilitiesConfig.HANDLER.save();
    }

    private static ChatMode parseWriteMode(String raw) {
        if (raw != null && raw.equalsIgnoreCase("cclive")) {
            return ChatMode.CCLIVE;
        }
        return ChatMode.DEFAULT;
    }

    /** Schreibmodus an sichtbare Chats anpassen. */
    private ChatMode clampWriteMode(ChatMode mode) {
        if (mode == ChatMode.CCLIVE && !showCCLiveChat) {
            return ChatMode.DEFAULT;
        }
        if (mode == ChatMode.DEFAULT && !showDefaultChat) {
            return showCCLiveChat ? ChatMode.CCLIVE : ChatMode.DEFAULT;
        }
        return mode;
    }

    /**
     * Effektives Schreibziel: respektiert {@link #currentChatMode}, falls der Kanal sichtbar ist.
     * Sind beide an, gilt der gespeicherte Modus (Standard: Default).
     */
    public ChatMode getEffectiveWriteMode() {
        return clampWriteMode(currentChatMode);
    }

    public void setChatMode(ChatMode mode) {
        if (mode == ChatMode.CCLIVE && !showCCLiveChat) {
            return;
        }
        if (mode == ChatMode.DEFAULT && !showDefaultChat) {
            return;
        }
        this.currentChatMode = mode;
        saveChatSettings();
    }

    public void toggleChat(String chatType) {
        boolean wasDefaultVisible = showDefaultChat;

        if (chatType.equalsIgnoreCase("default")) {
            showDefaultChat = !showDefaultChat;
            if (!showDefaultChat && !showCCLiveChat) {
                showCCLiveChat = true;
            }
            // Default aus → Schreiben CCLive; Default wieder an → Schreiben Default
            if (!showDefaultChat && showCCLiveChat) {
                currentChatMode = ChatMode.CCLIVE;
            } else if (showDefaultChat && !wasDefaultVisible) {
                currentChatMode = ChatMode.DEFAULT;
            }
        } else if (chatType.equalsIgnoreCase("cclive")) {
            showCCLiveChat = !showCCLiveChat;
            if (!showDefaultChat && !showCCLiveChat) {
                showDefaultChat = true;
            }
        }

        currentChatMode = clampWriteMode(currentChatMode);
        saveChatSettings();

        if (showCCLiveChat) {
            ensureSocketConnected();
        } else {
            webSocket.disconnect();
        }
    }

    public boolean isChatVisible(String chatType) {
        if (chatType.equalsIgnoreCase("default")) {
            return showDefaultChat;
        } else if (chatType.equalsIgnoreCase("cclive")) {
            return showCCLiveChat;
        }
        return true;
    }

    public boolean isWebSocketConnected() {
        return webSocket.isConnected();
    }

    /** exactKey = player|message|timestamp → softKey = player|message */
    private static String stripTimestamp(String exactKey) {
        if (exactKey == null || exactKey.isEmpty()) {
            return "";
        }
        int last = exactKey.lastIndexOf('|');
        return last > 0 ? exactKey.substring(0, last) : exactKey;
    }

    public ChatMode getCurrentChatMode() { return currentChatMode; }
    public boolean isShowDefaultChat() { return showDefaultChat; }
    public boolean isShowCCLiveChat() { return showCCLiveChat; }
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (!enabled) {
            webSocket.disconnect();
        } else if (showCCLiveChat) {
            ensureSocketConnected();
        }
    }
}

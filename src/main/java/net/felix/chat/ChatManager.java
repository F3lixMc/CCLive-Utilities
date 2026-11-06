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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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

    private final ChatWebSocket webSocket = new ChatWebSocket();

    /** Deduplizierung für exakt gleiche WS-Kopien. */
    private final Object displayDedupLock = new Object();
    private volatile String lastDisplayedExactKey = "";
    private final Set<Long> displayedMessageIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    static final String PLACEHOLDER = "Nachricht von Moderatoren gelöscht!";
    static final int DELETED_BODY_COLOR = 0xC8C8C8;
    public static final String MESSAGE_ID_PREFIX = "cclive-msg:";
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
        webSocket.setOnChatDeleted(this::onIncomingDeleted);
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
            client.execute(() -> {
                ensureSocketConnected();
                ChatBlockedGlyphs.refreshFromServerAsync();
            });
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

        if (ChatBlockedGlyphs.containsBlockedGlyph(message)) {
            return CompletableFuture.completedFuture(ChatBlockedGlyphs.ERROR_MESSAGE);
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
            if (!leaderboardManager.awaitRegistration(20_000L)) {
                return "Nicht registriert. Warte kurz nach Server-Join oder nutze /cclive connection refresh.";
            }
            return null;
        }).thenCompose(regError -> {
            if (regError != null) {
                return CompletableFuture.completedFuture(regError);
            }
            ensureSocketConnected();
            return webSocket.sendChat(trimmed).thenApply(result -> {
                if (result == null || result.error() != null) {
                    return ChatBlockedGlyphs.clientErrorMessage(
                        result == null ? "Senden fehlgeschlagen." : result.error());
                }
                echoOwnMessage(trimmed, result.messageId(), result.timestamp());
                return null;
            });
        });
    }

    private void echoOwnMessage(String message, Long messageId, Long timestamp) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        JsonObject echo = new JsonObject();
        echo.addProperty("player", client.player.getGameProfile().name());
        echo.addProperty("message", message);
        echo.addProperty("timestamp", timestamp != null ? timestamp : System.currentTimeMillis());
        echo.addProperty("local_echo", true);
        if (messageId != null) {
            echo.addProperty("id", messageId);
        }
        int[] ownColors = ChatNameColorTracker.getInstance().getOwnNameColors();
        if (ownColors.length > 0) {
            echo.addProperty("name_color", ChatNameColors.toHex(ownColors[0]));
            echo.add("name_colors", ChatNameColors.toJsonArray(ownColors));
        }
        onIncomingChatMessage(echo);
    }

    private void onIncomingChatMessage(JsonObject msgObj) {
        if (!isEnabled || !showCCLiveChat) {
            return;
        }
        if (msgObj == null || !msgObj.has("player") || !msgObj.has("message")) {
            return;
        }

        String player;
        String message;
        try {
            player = msgObj.get("player").getAsString();
            message = msgObj.get("message").getAsString();
        } catch (Exception e) {
            return;
        }
        if (player == null || message == null) {
            return;
        }
        Long messageId = null;
        if (msgObj.has("id") && !msgObj.get("id").isJsonNull()) {
            try {
                messageId = msgObj.get("id").getAsLong();
            } catch (Exception ignored) {
                messageId = null;
            }
        }
        long timestamp = msgObj.has("timestamp") ? msgObj.get("timestamp").getAsLong() : System.currentTimeMillis();
        boolean deleted = isExplicitlyDeleted(msgObj);
        boolean localEcho = msgObj.has("local_echo") && msgObj.get("local_echo").getAsBoolean();
        if (ChatDisplayDedup.skipKnownId(displayedMessageIds, messageId)) {
            return;
        }
        Minecraft selfClient = Minecraft.getInstance();
        String selfName = selfClient != null && selfClient.player != null
            ? selfClient.player.getGameProfile().name()
            : null;
        long now = System.currentTimeMillis();
        if (ChatDisplayDedup.skipOwnBroadcast(
                localEcho, player, selfName, message, lastSendPayload, lastSendAtMs, now)) {
            return;
        }
        int[] nameColors = ChatNameColorTracker.getInstance().colorsFor(
            player, ChatNameColors.parseFromMessage(msgObj));
        int cactusRank = parseRankField(msgObj, "cactus_rank");
        int soulRank = parseRankField(msgObj, "soul_rank");

        String exactKey = ChatDisplayDedup.exactKey(messageId, player, message, timestamp);
        synchronized (displayDedupLock) {
            if (ChatDisplayDedup.skipExactDuplicate(lastDisplayedExactKey, exactKey)) {
                return;
            }
            lastDisplayedExactKey = exactKey;
            if (messageId != null && messageId > 0L) {
                displayedMessageIds.add(messageId);
            }
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        final String playerFinal = player;
        final String messageFinal = message;
        final int[] nameColorsFinal = nameColors;
        final int cactusRankFinal = cactusRank;
        final int soulRankFinal = soulRank;
        final Long messageIdFinal = messageId;
        final boolean deletedFinal = deleted;

        CompletableFuture
            .supplyAsync(() -> buildIncomingChatLine(
                playerFinal, messageFinal, nameColorsFinal, cactusRankFinal, soulRankFinal, messageIdFinal, deletedFinal))
            .thenAccept(chatMessage -> {
                if (chatMessage == null) {
                    return;
                }
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.execute(() -> addCcliveChatLine(mc, chatMessage));
                }
            })
            .exceptionally(ex -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    Component fallback = Component.literal("[CCLive] " + playerFinal + ">> " + messageFinal);
                    mc.execute(() -> addCcliveChatLine(mc, fallback));
                }
                return null;
            });
    }

    private void onIncomingDeleted(JsonObject obj) {
        if (obj == null || !obj.has("id") || obj.get("id").isJsonNull()) {
            return;
        }
        long id;
        try {
            id = obj.get("id").getAsLong();
        } catch (Exception e) {
            return;
        }
        String placeholder = obj.has("placeholder") && obj.get("placeholder").isJsonPrimitive()
            ? obj.get("placeholder").getAsString()
            : PLACEHOLDER;
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> ChatHudMessageIds.replacePlaceholder(client, id, placeholder));
    }

    static MutableComponent deletedPlaceholderBody() {
        return Component.literal(PLACEHOLDER).setStyle(Style.EMPTY.withColor(DELETED_BODY_COLOR));
    }

    static boolean isExplicitlyDeleted(com.google.gson.JsonObject msgObj) {
        if (msgObj == null || !msgObj.has("deleted") || msgObj.get("deleted").isJsonNull()) {
            return false;
        }
        try {
            return msgObj.get("deleted").getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    private Component buildIncomingChatLine(
            String player, String message, int[] nameColors, int cactusRank, int soulRank, Long messageId, boolean deleted) {
        HoverEvent statsHover = null;
        try {
            if (PlayerHoverStatsUtility.isInitialized()) {
                Component hoverText = PlayerHoverStatsUtility.createStatsHoverForPlayer(
                    player, cactusRank, soulRank);
                if (hoverText != null) {
                    statsHover = PlayerHoverStatsUtility.createHoverEvent(hoverText);
                }
            }
        } catch (Exception ignored) {
            statsHover = null;
        }

        Component body;
        if (deleted) {
            body = deletedPlaceholderBody();
        } else {
            try {
                body = ItemShareManager.getInstance().buildMessageBodyAsync(message, null).join();
            } catch (Exception e) {
                body = Component.literal(message);
            }
            if (body == null) {
                body = Component.literal(message);
            }
        }

        Style prefixStyle = Style.EMPTY.withColor(CCLIVE_PREFIX_COLOR);
        if (messageId != null && messageId > 0L) {
            prefixStyle = prefixStyle.withInsertion(MESSAGE_ID_PREFIX + messageId);
        }
        Style arrowStyle = Style.EMPTY.withColor(0xFFFFFF);
        if (statsHover != null) {
            prefixStyle = prefixStyle.withHoverEvent(statsHover);
            arrowStyle = arrowStyle.withHoverEvent(statsHover);
        }

        MutableComponent chatMessage = Component.empty();
        chatMessage.append(Component.literal("[CCLive] ").setStyle(prefixStyle));
        chatMessage.append(ChatNameColors.coloredName(player, nameColors, statsHover));

        if (CCLiveUtilitiesConfig.HANDLER.instance().chatIconEnabled
                && CCLiveUtilitiesConfig.isModRuntimeEnabled()) {
            Style iconStyle = Style.EMPTY
                .withFont(new FontDescription.Resource(ICON_FONT))
                .withColor(0xFFFFFF);
            if (statsHover != null) {
                iconStyle = iconStyle.withHoverEvent(statsHover);
            }
            chatMessage.append(Component.literal(" ").setStyle(arrowStyle));
            chatMessage.append(Component.literal(String.valueOf(
                net.felix.utilities.Other.PlayericonUtility.PlayerIconUtility.chatIconMarker(player)
            )).setStyle(iconStyle));
        }

        chatMessage.append(Component.literal(">> ").setStyle(arrowStyle));
        chatMessage.append(body);
        return chatMessage;
    }

    private static void addCcliveChatLine(Minecraft client, Component chatMessage) {
        try {
            if (client.gui == null || client.gui.hud == null || client.gui.hud.getChat() == null) {
                return;
            }
            // SYSTEM_SERVER bleibt sichtbar, wenn ein Filter SYSTEM_CLIENT unterdrückt.
            client.gui.hud.getChat().addServerSystemMessage(chatMessage);
        } catch (Exception e) {
            try {
                if (client.gui != null && client.gui.hud.getChat() != null) {
                    client.gui.hud.getChat().addClientSystemMessage(chatMessage);
                }
            } catch (Exception ignored) {
            }
        }
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
        currentChatMode = ChatChannelRules.clamp(currentChatMode, showDefaultChat, showCCLiveChat);
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

    /**
     * Effektives Schreibziel: respektiert {@link #currentChatMode}, falls der Kanal sichtbar ist.
     * Sind beide an, gilt der gespeicherte Modus (Standard: Default).
     */
    public ChatMode getEffectiveWriteMode() {
        return ChatChannelRules.clamp(currentChatMode, showDefaultChat, showCCLiveChat);
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
        ChatChannelRules.State next = ChatChannelRules.toggle(
            new ChatChannelRules.State(showDefaultChat, showCCLiveChat, currentChatMode),
            chatType);
        showDefaultChat = next.showDefault();
        showCCLiveChat = next.showCCLive();
        currentChatMode = next.writeMode();
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

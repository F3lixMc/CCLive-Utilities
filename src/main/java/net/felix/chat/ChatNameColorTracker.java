package net.felix.chat;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.http.HttpClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Erkennt Namensfarben (inkl. Gradient, Buchstabe für Buchstabe) aus dem
 * Default-PlayLegend-Chat — Segment unmittelbar vor {@code >>}.
 * Eigene Farben werden lokal gespeichert und zum Backend synchronisiert.
 *
 * <p>Viele Default-Chat-Nachrichten werden in {@code PlayerHoverStatsMixin}
 * abgefangen — dort muss {@link #observeDefaultChatMessage(Component)}
 * auf dem Original aufgerufen werden.
 */
public final class ChatNameColorTracker {
    private static final ChatNameColorTracker INSTANCE = new ChatNameColorTracker();
    private final AtomicBoolean syncPending = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, int[]> colorsByPlayer = new ConcurrentHashMap<>();

    private ChatNameColorTracker() {
    }

    public static ChatNameColorTracker getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
            client.execute(() -> {
                rememberOwnFromConfig();
                if (hasOwnColors()) {
                    syncPending.set(true);
                }
                trySyncPending();
            }));
    }

    public int getOwnNameColorOrWhite() {
        int[] colors = getOwnNameColors();
        return colors.length == 0 ? ChatNameColors.WHITE : colors[0];
    }

    public int[] getOwnNameColors() {
        rememberOwnFromConfig();
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            int[] cached = colorsByPlayer.get(key(client.player.getGameProfile().name()));
            if (cached != null && cached.length > 0) {
                return cached.clone();
            }
        }
        return ChatNameColors.parse(CCLiveUtilitiesConfig.HANDLER.instance().ownChatNameColors);
    }

    public int[] colorsFor(String player, int[] payloadColors) {
        if (payloadColors != null && payloadColors.length > 0) {
            if (player != null && !player.isBlank()) {
                colorsByPlayer.put(key(player), payloadColors.clone());
            }
            return payloadColors;
        }
        if (player != null && !player.isBlank()) {
            int[] cached = colorsByPlayer.get(key(player));
            if (cached != null && cached.length > 0) {
                return cached;
            }
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.player != null
                    && player.equalsIgnoreCase(client.player.getGameProfile().name())) {
                return getOwnNameColors();
            }
        }
        return new int[0];
    }

    public static int parseNameColor(String raw) {
        return ChatNameColors.parseSolid(raw);
    }

    public static boolean isValidRgb(int rgb) {
        return rgb >= 0 && rgb <= ChatNameColors.WHITE;
    }

    public static String toHex(int rgb) {
        return ChatNameColors.toHex(rgb);
    }

    private void onGameMessage(Component message, boolean overlay) {
        if (overlay) {
            return;
        }
        observeDefaultChatMessage(message);
    }

    /**
     * Aus Default-Chat (nicht CCLive) Namensfarben lesen.
     * Öffentlich für PlayerHoverStatsMixin (Original-Paket).
     */
    public void observeDefaultChatMessage(Component message) {
        if (message == null) {
            return;
        }
        ChatNameColors.Sample sample = ChatNameColors.fromComponent(message);
        if (sample == null || sample.name == null || sample.colors == null || sample.colors.length == 0) {
            return;
        }

        colorsByPlayer.put(key(sample.name), sample.colors.clone());

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        String self = client.player.getGameProfile().name();
        if (self == null || self.isEmpty() || !self.equalsIgnoreCase(sample.name)) {
            return;
        }

        CCLiveUtilitiesConfig cfg = CCLiveUtilitiesConfig.HANDLER.instance();
        String encoded = ChatNameColors.encode(sample.colors);
        int first = sample.colors[0] & ChatNameColors.WHITE;
        if (encoded.equals(cfg.ownChatNameColors) && cfg.ownChatNameColorRgb == first && !syncPending.get()) {
            return;
        }
        cfg.ownChatNameColors = encoded;
        cfg.ownChatNameColorRgb = first;
        CCLiveUtilitiesConfig.HANDLER.save();
        syncPending.set(true);
        trySyncPending();
    }

    public void trySyncPending() {
        if (!CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            return;
        }
        int[] colors = getOwnNameColors();
        if (colors.length == 0) {
            syncPending.set(false);
            return;
        }
        syncPending.set(true);
        final int[] toSend = colors.clone();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                LeaderboardManager lb = LeaderboardManager.getInstance();
                lb.ensureRegisteredAsync();
                if (!lb.ensureRegisteredSync()) {
                    return;
                }
                HttpClient http = lb.getHttpClient();
                JsonObject body = new JsonObject();
                body.addProperty("name_color", ChatNameColors.toHex(toSend[0]));
                body.add("name_colors", ChatNameColors.toJsonArray(toSend));
                JsonObject response = http.postWithToken("/player/name-color", body, lb.getPlayerToken());
                if (response != null && (response.has("success") || response.has("name_color"))) {
                    syncPending.set(false);
                }
            } catch (Exception ignored) {
            }
        });
    }

    private boolean hasOwnColors() {
        int[] colors = getOwnNameColors();
        return colors.length > 0 && !(colors.length == 1 && colors[0] == ChatNameColors.WHITE);
    }

    private void rememberOwnFromConfig() {
        CCLiveUtilitiesConfig cfg = CCLiveUtilitiesConfig.HANDLER.instance();
        int[] colors = ChatNameColors.parse(cfg.ownChatNameColors);
        if (colors.length == 0 && isValidRgb(cfg.ownChatNameColorRgb) && cfg.ownChatNameColorRgb != ChatNameColors.WHITE) {
            colors = new int[] { cfg.ownChatNameColorRgb & ChatNameColors.WHITE };
            cfg.ownChatNameColors = ChatNameColors.encode(colors);
        }
        if (colors.length == 0) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            colorsByPlayer.put(key(client.player.getGameProfile().name()), colors);
        }
    }

    private static String key(String player) {
        return player == null ? "" : player.toLowerCase(Locale.ROOT);
    }
}

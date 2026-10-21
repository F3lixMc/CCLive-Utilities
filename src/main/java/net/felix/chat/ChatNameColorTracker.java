package net.felix.chat;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.http.HttpClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Erkennt die eigene Namensfarbe aus dem Default-PlayLegend-Chat
 * (Segment direkt vor {@code >>}, Style-Farbe wie {@code #7FE4CA}),
 * speichert lokal und synchronisiert authentifiziert zum Backend.
 *
 * <p>Wichtig: Viele Default-Chat-Nachrichten werden in
 * {@code PlayerHoverStatsMixin} abgefangen — dort muss
 * {@link #observeDefaultChatMessage(Component)} auf dem Original aufgerufen werden,
 * sonst sieht der GAME-Event die Nachricht nie.
 */
public final class ChatNameColorTracker {
    private static final ChatNameColorTracker INSTANCE = new ChatNameColorTracker();
    private final AtomicBoolean syncPending = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private ChatNameColorTracker() {
    }

    public static ChatNameColorTracker getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        // Fallback für Nachrichten, die nicht vom Hover-Mixin gecancelt werden
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
            client.execute(() -> {
                int rgb = CCLiveUtilitiesConfig.HANDLER.instance().ownChatNameColorRgb;
                if (isValidRgb(rgb) && rgb != 0xFFFFFF) {
                    syncPending.set(true);
                }
                trySyncPending();
            }));
    }

    public int getOwnNameColorOrWhite() {
        int color = CCLiveUtilitiesConfig.HANDLER.instance().ownChatNameColorRgb;
        return isValidRgb(color) ? (color & 0xFFFFFF) : 0xFFFFFF;
    }

    public static int parseNameColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0xFFFFFF;
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        try {
            int value = Integer.parseInt(s, 16);
            return isValidRgb(value) ? (value & 0xFFFFFF) : 0xFFFFFF;
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }

    public static boolean isValidRgb(int rgb) {
        return rgb >= 0 && rgb <= 0xFFFFFF;
    }

    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private void onGameMessage(Component message, boolean overlay) {
        if (overlay) {
            return;
        }
        observeDefaultChatMessage(message);
    }

    /**
     * Aus Default-Chat (nicht CCLive) die eigene Namensfarbe lesen.
     * Öffentlich für PlayerHoverStatsMixin (Original-Paket).
     */
    public void observeDefaultChatMessage(Component message) {
        if (message == null) {
            return;
        }
        String plain = message.getString();
        if (plain == null || plain.isEmpty() || plain.contains("[CCLive]")) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        String self = client.player.getGameProfile().name();
        if (self == null || self.isEmpty()) {
            return;
        }
        if (!plain.contains(self) || !plain.contains(">>")) {
            return;
        }

        Integer color = extractOwnNameColor(message, self);
        if (color == null) {
            return;
        }
        int rgb = color & 0xFFFFFF;
        // Rein weiß aus Style ohne Farbe → als „unbekannt“ nicht speichern,
        // außer der Style setzt explizit weiß (selten); wir akzeptieren jeden erkannten Wert inkl. Weiß nur bei TextColor != null
        CCLiveUtilitiesConfig cfg = CCLiveUtilitiesConfig.HANDLER.instance();
        if (cfg.ownChatNameColorRgb == rgb && !syncPending.get()) {
            return;
        }
        cfg.ownChatNameColorRgb = rgb;
        CCLiveUtilitiesConfig.HANDLER.save();
        syncPending.set(true);
        trySyncPending();
    }

    /**
     * Wie PlayerHoverStats: Segment unmittelbar vor {@code >>} finden
     * und dessen Style-Farbe lesen (PlayLegend: {@code color: "#7FE4CA"}).
     */
    private Integer extractOwnNameColor(Component message, String selfName) {
        Component nameComponent = findNameComponentBeforeArrow(message, selfName);
        if (nameComponent == null) {
            return null;
        }
        TextColor color = resolveColor(nameComponent);
        if (color == null) {
            return null;
        }
        return color.getValue() & 0xFFFFFF;
    }

    private static TextColor resolveColor(Component component) {
        TextColor direct = component.getStyle() != null ? component.getStyle().getColor() : null;
        if (direct != null) {
            return direct;
        }
        // In verschachtelten Parts die erste gesetzte Farbe am eigenen Literal suchen
        return findFirstColor(component, Style.EMPTY);
    }

    private static TextColor findFirstColor(Component component, Style parent) {
        Style style = component.getStyle();
        Style merged = style != null ? style.applyTo(parent) : parent;
        TextColor c = merged.getColor();
        String own = component.getContents() != null ? component.getString() : "";
        // getString includes children — use visit for own? Prefer merged color if any text
        if (c != null && component.getSiblings().isEmpty()) {
            return c;
        }
        if (c != null) {
            String only = component.getString();
            if (only != null && !only.isEmpty()) {
                // keep searching children for more specific
            }
        }
        for (Component sibling : component.getSiblings()) {
            TextColor found = findFirstColor(sibling, merged);
            if (found != null) {
                return found;
            }
        }
        return c;
    }

    private Component findNameComponentBeforeArrow(Component message, String selfName) {
        Component found = findNameBeforeArrowInSiblings(message, selfName);
        if (found != null) {
            return found;
        }
        for (Component sibling : message.getSiblings()) {
            found = findNameComponentBeforeArrow(sibling, selfName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private Component findNameBeforeArrowInSiblings(Component parent, String selfName) {
        List<Component> siblings = parent.getSiblings();
        if (siblings == null || siblings.isEmpty()) {
            return null;
        }
        for (int i = 0; i < siblings.size(); i++) {
            Component current = siblings.get(i);
            for (int j = i + 1; j < siblings.size() && j <= i + 3; j++) {
                String nextText = siblings.get(j).getString();
                boolean isArrow = nextText != null && (nextText.contains(">>") || nextText.trim().equals(">>"));
                boolean isSpace = nextText != null && (nextText.trim().isEmpty() || " ".equals(nextText));
                boolean arrowAfterSpace = isSpace && j + 1 < siblings.size()
                    && siblings.get(j + 1).getString() != null
                    && (siblings.get(j + 1).getString().contains(">>")
                    || siblings.get(j + 1).getString().trim().equals(">>"));
                // optional Icon-Glyphe zwischen Name und >>
                boolean isIconThenArrow = nextText != null && nextText.indexOf('ѳ') >= 0 && j + 1 < siblings.size()
                    && siblings.get(j + 1).getString() != null
                    && siblings.get(j + 1).getString().contains(">>");

                if (isArrow || arrowAfterSpace || isIconThenArrow) {
                    String name = current.getString() != null ? current.getString().trim() : "";
                    if (selfName.equalsIgnoreCase(name)
                            || selfName.equalsIgnoreCase(name.replaceAll("[^A-Za-z0-9_]", ""))) {
                        return current;
                    }
                    // ggf. zwischen current und >> liegt der Name
                    for (int k = i; k < j; k++) {
                        String between = siblings.get(k).getString();
                        if (between == null) {
                            continue;
                        }
                        String n = between.trim();
                        if (selfName.equalsIgnoreCase(n)
                                || selfName.equalsIgnoreCase(n.replaceAll("[^A-Za-z0-9_]", ""))) {
                            return siblings.get(k);
                        }
                    }
                }
            }
        }
        return null;
    }

    public void trySyncPending() {
        if (!CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            return;
        }
        int rgb = CCLiveUtilitiesConfig.HANDLER.instance().ownChatNameColorRgb;
        if (!isValidRgb(rgb)) {
            syncPending.set(false);
            return;
        }
        if (!syncPending.get() && rgb == 0xFFFFFF) {
            return;
        }
        syncPending.set(true);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                LeaderboardManager lb = LeaderboardManager.getInstance();
                if (!lb.ensureRegisteredSync()) {
                    return;
                }
                HttpClient http = lb.getHttpClient();
                JsonObject body = new JsonObject();
                body.addProperty("name_color", toHex(rgb));
                JsonObject response = http.postWithToken("/player/name-color", body, lb.getPlayerToken());
                if (response != null && (response.has("success") || response.has("name_color"))) {
                    syncPending.set(false);
                }
            } catch (Exception ignored) {
            }
        });
    }
}

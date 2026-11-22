package net.felix.Community.chat.share;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Other.server.ServerConnectionGate;
import net.felix.Community.chat.share.ChatShareCatalog.Bind;
import net.felix.Community.chat.share.ChatShareInbox.Line;
import net.felix.Community.leaderboards.LeaderboardManager;
import net.felix.Community.leaderboards.http.HttpClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.felix.mixin.ChatHudMessagesAccess;
import net.felix.Community.chat.buildshare.BuildShareManager;
import net.felix.Community.chat.buildshare.BuildShareOverviewLayout;

/**
 * Sendeversuche, eigenes Echo und der eine gebündelte Abruf.
 * Chatzeilen werden nur gegen Zuordnungen geprüft, die schon im Speicher liegen.
 */
public final class ChatShareClient {
    private static final ChatShareClient INSTANCE = new ChatShareClient();
    private static final int POLL_TICKS = 40;

    private final ChatShareAttempts attempts = new ChatShareAttempts();
    private final ChatSharePublish publish = new ChatSharePublish();
    private final ChatShareClock clock = new ChatShareClock();
    private final ChatShareInbox inbox = new ChatShareInbox();
    private final Map<Line, GuiMessage> shown = new IdentityHashMap<>();
    private final Queue<ChatSharePublish.Next> posts = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean posting = new AtomicBoolean();
    private final AtomicBoolean fetching = new AtomicBoolean();
    private final Set<String> tooltipLoads = ConcurrentHashMap.newKeySet();
    private final ThreadLocal<Boolean> addingVanilla = ThreadLocal.withInitial(() -> false);
    private volatile boolean accepting;
    private static boolean vanillaInstalled;
    private ChatShareAttempts.Attempt noted;
    private Bind ownFallback;
    private long cursor;
    private long lastPollAt;
    private int tick;

    private ChatShareClient() {
    }

    public static void tick(Minecraft client) {
        INSTANCE.onTick(client);
    }

    public static void install() {
        if (vanillaInstalled) {
            return;
        }
        vanillaInstalled = true;
        ClientReceiveMessageEvents.ALLOW_CHAT.register(ChatShareClient::allowVanillaChat);
    }

    /**
     * Singleplayer hat keinen PlayLegend-Datumsbaustein. In der Dev-Umgebung hängt das Item-Hover
     * direkt an der eigenen Vanilla-Zeile.
     */
    private static boolean allowVanillaChat(
            Component message,
            PlayerChatMessage signed,
            GameProfile sender,
            ChatType.Bound bound,
            Instant time) {
        if (INSTANCE.addingVanilla.get() || !ServerConnectionGate.isDevelopmentEnvironment()) {
            return true;
        }
        if (message == null || sender == null) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !sender.name().equals(client.player.getGameProfile().name())) {
            return true;
        }
        Component decorated = INSTANCE.decorateVanillaOwn(message);
        if (decorated == message) {
            return true;
        }
        client.execute(() -> {
            if (client.gui == null || client.gui.hud == null || client.gui.hud.getChat() == null) {
                return;
            }
            INSTANCE.addingVanilla.set(true);
            try {
                client.gui.hud.getChat().addPlayerMessage(
                        decorated,
                        signed == null ? null : signed.signature(),
                        null);
            } finally {
                INSTANCE.addingVanilla.set(false);
            }
        });
        return false;
    }

    public static void notePlain(String body) {
        synchronized (INSTANCE) {
            INSTANCE.notePlainLocked(body);
        }
    }

    private void notePlainLocked(String body) {
        if (!enabled() || body == null) {
            return;
        }
        noted = attempts.addPlain(body, System.currentTimeMillis());
    }

    public static void noteShare(ChatShareTexts.Replacement replacement, String shareId, Integer nameColor) {
        synchronized (INSTANCE) {
            INSTANCE.noteShareLocked(replacement, shareId, nameColor);
        }
    }

    private void noteShareLocked(ChatShareTexts.Replacement replacement, String shareId, Integer nameColor) {
        if (!enabled() || replacement == null || !replacement.share()) {
            return;
        }
        noted = attempts.addShare(
                replacement.text(),
                replacement.shareType(),
                shareId,
                replacement.displayName(),
                replacement.nameStart(),
                replacement.nameLength(),
                System.currentTimeMillis(),
                nameColor);
        ChatSharePublish.Next prepare = publish.noted(noted);
        if (prepare != null) {
            posts.add(prepare);
            drainPosts();
        }
    }

    public static void dropNoted() {
        synchronized (INSTANCE) {
            ChatShareAttempts.Attempt attempt = INSTANCE.noted;
            INSTANCE.noted = null;
            INSTANCE.publish.cancel(attempt);
            INSTANCE.attempts.drop(attempt);
        }
    }

    public static void observeEcho(ChatShareLine.Parsed parsed) {
        synchronized (INSTANCE) {
            INSTANCE.onEcho(parsed);
        }
    }

    public static Component decorate(Component shownComponent, ChatShareLine.Parsed parsed) {
        synchronized (INSTANCE) {
            return INSTANCE.decorateShown(shownComponent, parsed);
        }
    }

    public static void hold(GuiMessage message, ChatShareLine.Parsed parsed) {
        synchronized (INSTANCE) {
            INSTANCE.keep(message, parsed, System.currentTimeMillis());
        }
    }

    public static void retarget(GuiMessage from, GuiMessage to) {
        if (from == null || to == null) {
            return;
        }
        synchronized (INSTANCE) {
            INSTANCE.retargetLocked(from, to);
        }
    }

    private void retargetLocked(GuiMessage from, GuiMessage to) {
        for (Map.Entry<Line, GuiMessage> entry : shown.entrySet()) {
            if (entry.getValue() == from) {
                entry.setValue(to);
                return;
            }
        }
    }

    private void onTick(Minecraft client) {
        long now = System.currentTimeMillis();
        boolean online = client != null && client.player != null && client.getConnection() != null && enabled();
        accepting = online;
        long since;
        synchronized (this) {
            inbox.waiting(now, lastPollAt);
            if (!online) {
                attempts.clear();
                publish.clear();
                posts.clear();
                tooltipLoads.clear();
                noted = null;
                ownFallback = null;
                lastPollAt = 0L;
                return;
            }
            since = cursor;
        }
        if (++tick % POLL_TICKS != 0 || !fetching.compareAndSet(false, true)) {
            return;
        }
        Thread worker = new Thread(() -> fetch(client, since), "cclive-share-bind");
        worker.setDaemon(true);
        worker.start();
    }

    private Component decorateVanillaOwn(Component message) {
        String plain = message.getString();
        long now = System.currentTimeMillis();
        ChatShareAttempts.Attempt pending;
        synchronized (this) {
            pending = attempts.shareBodyAtEnd(plain, now);
        }
        if (pending == null) {
            return message;
        }
        ChatShareAttempts.Attempt confirmed;
        synchronized (this) {
            confirmed = attempts.confirmEcho(pending.body(), now);
        }
        if (confirmed == null) {
            return message;
        }
        Minecraft client = Minecraft.getInstance();
        String player = client.player == null ? "" : client.player.getGameProfile().name();
        Bind bind = localBind(confirmed, player);
        ChatSharePublish.Next confirm;
        synchronized (this) {
            ownFallback = bind;
            confirm = publish.echo(confirmed);
            if (confirm != null) {
                posts.add(confirm);
            }
        }
        if (confirm != null) {
            drainPosts();
        }
        int absStart = plain.length() - confirmed.body().length() + confirmed.nameStart();
        Component styled = ChatShareLine.decorateRange(
                message, absStart, confirmed.nameLength(), linkStyle(bind));
        return styled == null ? message : styled;
    }

    private void onEcho(ChatShareLine.Parsed parsed) {
        if (!enabled() || parsed == null || parsed.body() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Minecraft client = Minecraft.getInstance();
        String me = client.player == null ? null : client.player.getGameProfile().name();
        if (me == null || parsed.sender() == null || !parsed.sender().equals(me)) {
            String waiting = attempts.diagnose(parsed.body(), now);
            if (!waiting.startsWith("passendeShares=0")) {
                System.out.println("[CCLive-Share] Echo eines anderen Namens zeile="
                        + parsed.sender() + " ich=" + me + " " + waiting);
            }
            return;
        }
        ChatShareAttempts.Attempt confirmed = attempts.confirmEcho(parsed.body(), now);
        if (confirmed == null) {
            System.out.println("[CCLive-Share] Echo ohne eindeutigen Share body=[" + parsed.body() + "] "
                    + attempts.diagnose(parsed.body(), now));
            return;
        }
        ownFallback = localBind(confirmed, parsed.sender());
        ChatSharePublish.Next confirm = publish.echo(confirmed);
        if (confirm != null) {
            posts.add(confirm);
            drainPosts();
        }
    }

    private static Bind localBind(ChatShareAttempts.Attempt attempt, String player) {
        return new Bind(
                0L,
                attempt.attemptId(),
                player,
                ChatShareTexts.sha256(attempt.body()),
                attempt.displayName(),
                0L,
                attempt.shareType(),
                attempt.shareId(),
                attempt.nameStart(),
                attempt.nameLength(),
                attempt.nameColor());
    }

    private Component decorateShown(Component shownComponent, ChatShareLine.Parsed parsed) {
        if (!enabled() || shownComponent == null || parsed == null) {
            return shownComponent;
        }
        Bind bind = fallbackFor(parsed);
        if (bind == null) {
            return shownComponent;
        }
        String shownBody = ChatShareTexts.bodyAfterArrow(shownComponent.getString());
        if (!ChatShareTexts.spanMatches(shownBody, bind.nameStart(), bind.nameLength(), bind.displayName())) {
            return shownComponent;
        }
        Component decorated = ChatShareLine.decorate(
                shownComponent, bind.nameStart(), bind.nameLength(), linkStyle(bind));
        if (decorated == null || decorated == shownComponent) {
            System.out.println("[CCLive-Share] Name gefunden, Hover nicht gesetzt name="
                    + bind.displayName() + " start=" + bind.nameStart());
            return shownComponent;
        }
        requestTooltip(bind);
        return decorated;
    }

    /** Eigenes Echo ohne Datumsbaustein: die Zuordnung hängt nicht am Katalog-Zeitstempel. */
    private Bind fallbackFor(ChatShareLine.Parsed parsed) {
        Bind local = ownFallback;
        if (local == null || parsed.sender() == null || parsed.body() == null) {
            return null;
        }
        if (!parsed.sender().equals(local.player())) {
            return null;
        }
        if (!ChatShareTexts.sha256(parsed.body()).equals(local.bodyHash())) {
            return null;
        }
        return local;
    }

    private void keep(GuiMessage message, ChatShareLine.Parsed parsed, long now) {
        if (!enabled() || message == null || parsed == null || !parsed.complete()) {
            return;
        }
        for (GuiMessage existing : shown.values()) {
            if (existing == message) {
                return;
            }
        }
        Line line = new Line(parsed.sender(), parsed.body(), now);
        inbox.accept(line);
        shown.put(line, message);
        apply(List.of(), cursor, clock.usable(now), lastPollAt);
    }

    private void fetch(Minecraft client, long since) {
        try {
            LeaderboardManager manager = LeaderboardManager.getInstance();
            if (manager == null || !manager.ensureRegisteredSync()) {
                return;
            }
            String token = manager.getPlayerToken();
            HttpClient http = manager.getHttpClient();
            if (token == null || http == null) {
                return;
            }
            long started = System.currentTimeMillis();
            HttpClient.JsonResponse response = http.getResultWithToken("/share/bind?since=" + since, token);
            long finished = System.currentTimeMillis();
            if (response == null || response.status() != 200 || response.body() == null) {
                return;
            }
            JsonObject body = response.body();
            observeClock(started, finished, body);
            List<Bind> binds = readBinds(body);
            long next = body.has("cursor") && body.get("cursor").isJsonPrimitive()
                    ? body.get("cursor").getAsLong()
                    : since;
            ChatShareClock.Sample sample = clock.usable(finished);
            long polled = System.currentTimeMillis();
            client.execute(() -> {
                synchronized (ChatShareClient.this) {
                    apply(binds, next, sample, lastPollAt);
                    if (sample != null) {
                        lastPollAt = polled;
                    }
                }
            });
        } catch (Exception ignored) {
            // Der nächste Takt versucht denselben Cursor noch einmal.
        } finally {
            fetching.set(false);
        }
    }

    private void apply(List<Bind> binds, long nextCursor, ChatShareClock.Sample sample, long previousPollAt) {
        if (nextCursor > cursor) {
            cursor = nextCursor;
        }
        if (!enabled()) {
            return;
        }
        if (binds != null) {
            for (Bind bind : binds) {
                requestTooltip(bind);
            }
        }
        List<ChatShareInbox.Hit> linked = inbox.applyBundle(
                binds, System.currentTimeMillis(), sample, previousPollAt);
        Minecraft client = Minecraft.getInstance();
        if (client.gui == null || client.gui.hud == null || client.gui.hud.getChat() == null) {
            return;
        }
        var chat = client.gui.hud.getChat();
        if (!(chat instanceof ChatHudMessagesAccess access)) {
            return;
        }
        var messages = access.cclive$allMessages();
        boolean changed = false;
        for (ChatShareInbox.Hit hit : linked) {
            Line line = hit.line();
            GuiMessage message = shown.remove(line);
            if (message == null) {
                continue;
            }
            Bind bind = hit.bind();
            if (bind == null) {
                continue;
            }
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i) != message) {
                    continue;
                }
                Component updated = ChatShareLine.decorate(
                        message.content(), bind.nameStart(), bind.nameLength(), linkStyle(bind));
                if (updated != null && updated != message.content()) {
                    messages.set(i, new GuiMessage(
                            message.addedTime(), updated, message.signature(), message.source(), message.tag()));
                    changed = true;
                    System.out.println("[CCLive-Share] Hover gesetzt name=" + bind.displayName());
                    requestTooltip(bind);
                }
                break;
            }
        }
        if (changed) {
            access.cclive$refreshTrimmedMessages();
        }
    }

    private void drainPosts() {
        if (!posting.compareAndSet(false, true)) {
            return;
        }
        Thread worker = new Thread(() -> {
            try {
                ChatSharePublish.Next job;
                while ((job = posts.poll()) != null) {
                    sendJob(job);
                }
            } finally {
                posting.set(false);
                if (!posts.isEmpty()) {
                    drainPosts();
                }
            }
        }, "cclive-share-bind-post");
        worker.setDaemon(true);
        worker.start();
    }

    private void sendJob(ChatSharePublish.Next job) {
        ChatShareAttempts.Attempt attempt = job.attempt();
        if (attempt == null) {
            return;
        }
        boolean confirm = job.step() == ChatSharePublish.Step.CONFIRM;
        while (accepting && System.currentTimeMillis() - attempt.sentAt() <= ChatShareAttempts.LOCAL_TTL_MS) {
            try {
                LeaderboardManager manager = LeaderboardManager.getInstance();
                if (manager == null || !manager.ensureRegisteredSync()) {
                    sleepQuietly();
                    continue;
                }
                String token = manager.getPlayerToken();
                HttpClient http = manager.getHttpClient();
                if (token == null || http == null) {
                    sleepQuietly();
                    continue;
                }
                long started = System.currentTimeMillis();
                HttpClient.JsonResponse response = http.postResultWithToken("/share/bind", postBody(job), token);
                long finished = System.currentTimeMillis();
                int status = response == null ? 0 : response.status();
                JsonObject body = response == null ? null : response.body();
                if (body != null) {
                    observeClock(started, finished, body);
                }
                if (status == 200) {
                    System.out.println("[CCLive-Share] " + (confirm ? "Bind bestätigt" : "Bind vorbereiten")
                            + " name=" + attempt.displayName());
                    if (confirm) {
                        return;
                    }
                    ChatSharePublish.Next follow;
                    synchronized (this) {
                        follow = publish.preparedOk(attempt.attemptId());
                        if (follow != null) {
                            posts.add(follow);
                        }
                    }
                    return;
                }
                if (!confirm && (status == 400 || status == 403 || status == 404)) {
                    synchronized (this) {
                        publish.preparedRejected(attempt.attemptId());
                    }
                    System.out.println("[CCLive-Share] Bind Vorbereitung abgelehnt " + status
                            + " name=" + attempt.displayName());
                    return;
                }
                if (confirm && (status == 400 || status == 403)) {
                    System.out.println("[CCLive-Share] Bind Bestätigung abgelehnt " + status
                            + " name=" + attempt.displayName());
                    return;
                }
            } catch (Exception ignored) {
                // Timeout und Serverfehler treffen dieselbe attemptId noch einmal.
            }
            sleepQuietly();
        }
        System.out.println("[CCLive-Share] Bind nicht gespeichert name=" + attempt.displayName());
    }

    private void observeClock(long started, long finished, JsonObject body) {
        if (body != null && body.has("serverTime") && body.get("serverTime").isJsonPrimitive()) {
            clock.observe(started, finished, body.get("serverTime").getAsLong());
        }
    }

    private static JsonObject postBody(ChatSharePublish.Next job) {
        ChatShareAttempts.Attempt attempt = job.attempt();
        JsonObject body = new JsonObject();
        body.addProperty("attemptId", attempt.attemptId());
        body.addProperty("op", job.step() == ChatSharePublish.Step.CONFIRM ? "confirm" : "prepare");
        if (job.step() == ChatSharePublish.Step.PREPARE) {
            body.addProperty("bodyHash", ChatShareTexts.sha256(attempt.body()));
            body.addProperty("displayName", attempt.displayName());
            body.addProperty("shareType", attempt.shareType());
            body.addProperty("shareId", attempt.shareId());
            body.addProperty("nameStart", attempt.nameStart());
            body.addProperty("nameLength", attempt.nameLength());
            if ("ITEM".equals(attempt.shareType()) && attempt.nameColor() != null) {
                body.addProperty("nameColor", attempt.nameColor());
            }
        }
        return body;
    }

    private static List<Bind> readBinds(JsonObject body) {
        List<Bind> binds = new ArrayList<>();
        if (body == null || !body.has("binds") || !body.get("binds").isJsonArray()) {
            return binds;
        }
        JsonArray array = body.getAsJsonArray("binds");
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            try {
                Integer nameColor = null;
                if (row.has("nameColor") && row.get("nameColor").isJsonPrimitive()
                        && row.get("nameColor").getAsJsonPrimitive().isNumber()) {
                    nameColor = ChatShareNames.validRgb(row.get("nameColor").getAsInt());
                }
                binds.add(new Bind(
                        row.get("cursor").getAsLong(),
                        row.get("attemptId").getAsString(),
                        row.get("player").getAsString(),
                        row.get("bodyHash").getAsString(),
                        row.get("displayName").getAsString(),
                        row.get("acceptedAt").getAsLong(),
                        row.get("shareType").getAsString(),
                        row.get("shareId").getAsString(),
                        row.get("nameStart").getAsInt(),
                        row.get("nameLength").getAsInt(),
                        nameColor));
            } catch (Exception ignored) {
                // Eine kaputte Zeile lässt das übrige Bündel stehen.
            }
        }
        return binds;
    }

    private static Style linkStyle(Bind bind) {
        String prefix = "BUILD".equals(bind.shareType())
                ? BuildShareManager.BUILD_INSERTION_PREFIX
                : ItemShareManager.SHARE_INSERTION_PREFIX;
        Component tip = Component.literal(bind.displayName() == null ? "" : bind.displayName());
        if ("BUILD".equals(bind.shareType())) {
            BuildShareManager.BuildSnapshot data = BuildShareManager.getInstance().getResolvedBuild(bind.shareId());
            if (data != null) {
                tip = BuildShareOverviewLayout.toHoverComponent(data);
            }
        } else {
            var data = ItemShareManager.getInstance().getResolvedItem(bind.shareId());
            if (data != null) {
                tip = ItemShareManager.hoverTooltip(data);
            }
        }
        Style style = Style.EMPTY.withUnderlined(Boolean.TRUE).withInsertion(prefix + bind.shareId());
        Integer color = ChatShareNames.chatColor(bind.shareType(), bind.nameColor());
        if (color != null) {
            style = style.withColor(color);
        }
        HoverEvent hover = ItemShareManager.createShowTextHoverPublic(tip);
        if (hover != null) {
            style = style.withHoverEvent(hover);
        }
        return style;
    }

    private void requestTooltip(Bind bind) {
        if (bind == null || bind.shareId() == null || tooltipReady(bind)) {
            return;
        }
        if (!tooltipLoads.add(bind.shareId())) {
            return;
        }
        Thread worker = new Thread(() -> {
            boolean loaded = false;
            try {
                loaded = "BUILD".equals(bind.shareType())
                        ? BuildShareManager.getInstance().ensureCached(bind.shareId())
                        : ItemShareManager.getInstance().ensureCached(bind.shareId());
            } finally {
                tooltipLoads.remove(bind.shareId());
            }
            if (!loaded) {
                System.out.println("[CCLive-Share] Tooltip nicht geladen " + bind.shareId());
                return;
            }
            Minecraft.getInstance().execute(() -> refreshShareHover(bind));
        }, "cclive-share-tooltip");
        worker.setDaemon(true);
        worker.start();
    }

    private static boolean tooltipReady(Bind bind) {
        if ("BUILD".equals(bind.shareType())) {
            return BuildShareManager.getInstance().getResolvedBuild(bind.shareId()) != null;
        }
        return ItemShareManager.getInstance().getResolvedItem(bind.shareId()) != null;
    }

    /** Ersetzt den Namens-Hover durch den geladenen Share-Tooltip, sobald der Snapshot da ist. */
    private void refreshShareHover(Bind bind) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui == null || client.gui.hud == null || client.gui.hud.getChat() == null) {
            return;
        }
        Style link = linkStyle(bind);
        HoverEvent hover = link.getHoverEvent();
        String token = link.getInsertion();
        if (hover == null || token == null) {
            return;
        }
        if (!(client.gui.hud.getChat() instanceof ChatHudMessagesAccess access)) {
            return;
        }
        var messages = access.cclive$allMessages();
        boolean changed = false;
        for (int i = 0; i < messages.size(); i++) {
            GuiMessage message = messages.get(i);
            if (message.content() == null || !hasInsertion(message.content(), token)) {
                continue;
            }
            Component updated = ChatShareLine.decorate(
                    message.content(), bind.nameStart(), bind.nameLength(), link);
            if (updated == null || updated == message.content() || !hasInsertion(updated, token)) {
                boolean[] did = {false};
                updated = replaceShareHover(message.content(), token, hover, did);
                if (!did[0] || updated == null || !hasInsertion(updated, token)) {
                    continue;
                }
            }
            messages.set(i, new GuiMessage(
                    message.addedTime(), updated, message.signature(), message.source(), message.tag()));
            changed = true;
        }
        if (changed) {
            String tip = hover instanceof HoverEvent.ShowText show ? show.value().getString() : "";
            System.out.println("[CCLive-Share] Tooltip gesetzt " + bind.shareId()
                    + " zeichen=" + tip.length());
            access.cclive$refreshTrimmedMessages();
        }
    }

    private static boolean hasInsertion(Component component, String token) {
        if (component == null) {
            return false;
        }
        Style style = component.getStyle();
        if (style != null && token.equals(style.getInsertion())) {
            return true;
        }
        var contents = component.getContents();
        if (contents instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component child && hasInsertion(child, token)) {
                    return true;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (hasInsertion(sibling, token)) {
                return true;
            }
        }
        return false;
    }

    private static Component replaceShareHover(Component component, String token, HoverEvent hover, boolean[] changed) {
        if (component == null) {
            return Component.empty();
        }
        Style style = component.getStyle() == null ? Style.EMPTY : component.getStyle();
        if (token.equals(style.getInsertion())) {
            style = style.withHoverEvent(hover);
            changed[0] = true;
        }
        MutableComponent out;
        var contents = component.getContents();
        if (contents instanceof net.minecraft.network.chat.contents.PlainTextContents plain) {
            out = Component.literal(plain.text() == null ? "" : plain.text()).setStyle(style);
        } else if (contents instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
            Object[] args = translatable.getArgs();
            Object[] next = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component child) {
                    next[i] = replaceShareHover(child, token, hover, changed);
                } else {
                    next[i] = args[i];
                }
            }
            out = Component.translatable(translatable.getKey(), next).setStyle(style);
        } else {
            out = Component.empty().setStyle(style);
        }
        for (Component sibling : component.getSiblings()) {
            out.append(replaceShareHover(sibling, token, hover, changed));
        }
        return out;
    }

    private static boolean enabled() {
        return CCLiveUtilitiesConfig.isDataTrackingEnabled();
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}

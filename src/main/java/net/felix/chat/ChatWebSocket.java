package net.felix.chat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.config.LeaderboardConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * WebSocket-Client für CCLive-Chat (Send + Push-Receive).
 * Auth über Token-Nachricht; Share-Uploads bleiben HTTP.
 */
public final class ChatWebSocket implements WebSocket.Listener {
    private static final Gson GSON = new Gson();
    private static final long SEND_TIMEOUT_MS = 8000L;
    private static final long RECONNECT_BASE_MS = 1500L;
    private static final long RECONNECT_MAX_MS = 20000L;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private final AtomicReference<WebSocket> socket = new AtomicReference<>();
    private final AtomicBoolean intentionalClose = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicBoolean authenticated = new AtomicBoolean(false);
    private final StringBuilder textBuffer = new StringBuilder();
    private final Map<String, CompletableFuture<String>> pendingSends = new ConcurrentHashMap<>();

    private volatile Consumer<JsonObject> onChatMessage;
    private volatile long reconnectDelayMs = RECONNECT_BASE_MS;
    private volatile boolean wantConnected;

    public void setOnChatMessage(Consumer<JsonObject> onChatMessage) {
        this.onChatMessage = onChatMessage;
    }

    public boolean isConnected() {
        WebSocket ws = socket.get();
        return ws != null && !ws.isInputClosed() && authenticated.get();
    }

    public void connectIfNeeded() {
        wantConnected = true;
        intentionalClose.set(false);
        if (isConnected() || connecting.get()) {
            return;
        }
        connectAsync();
    }

    public void disconnect() {
        wantConnected = false;
        intentionalClose.set(true);
        authenticated.set(false);
        WebSocket ws = socket.getAndSet(null);
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
            } catch (Exception ignored) {
            }
        }
        failAllPending("WebSocket getrennt.");
    }

    /**
     * Sendet eine Chat-Nachricht. Rückgabe: null = ok, sonst Fehlermeldung.
     */
    public CompletableFuture<String> sendChat(String message) {
        if (message == null || message.isBlank()) {
            return CompletableFuture.completedFuture("Nachricht ist leer.");
        }
        return ensureReady().thenCompose(readyError -> {
            if (readyError != null) {
                return CompletableFuture.completedFuture(readyError);
            }
            WebSocket ws = socket.get();
            if (ws == null || !authenticated.get()) {
                return CompletableFuture.completedFuture("WebSocket nicht verbunden.");
            }

            String requestId = UUID.randomUUID().toString();
            CompletableFuture<String> future = new CompletableFuture<>();
            pendingSends.put(requestId, future);

            JsonObject payload = new JsonObject();
            payload.addProperty("type", "chat.send");
            payload.addProperty("message", message);
            payload.addProperty("requestId", requestId);

            try {
                ws.sendText(GSON.toJson(payload), true);
            } catch (Exception e) {
                pendingSends.remove(requestId);
                return CompletableFuture.completedFuture("Senden fehlgeschlagen: " + e.getMessage());
            }

            return future.orTimeout(SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    pendingSends.remove(requestId);
                    return "Timeout beim Chat-Senden.";
                });
        });
    }

    private CompletableFuture<String> ensureReady() {
        if (isConnected()) {
            return CompletableFuture.completedFuture(null);
        }
        wantConnected = true;
        intentionalClose.set(false);
        connectAsync();
        return CompletableFuture.supplyAsync(() -> {
            long deadline = System.currentTimeMillis() + 6000L;
            while (System.currentTimeMillis() < deadline) {
                if (isConnected()) {
                    return null;
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "Unterbrochen.";
                }
            }
            return "WebSocket-Verbindung fehlgeschlagen.";
        });
    }

    private void connectAsync() {
        if (!wantConnected || !CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            return;
        }
        // CAS: verhindert zwei parallele Connects → sonst zwei WS-Clients und doppelte chat.message
        if (!connecting.compareAndSet(false, true)) {
            return;
        }
        authenticated.set(false);

        CompletableFuture.runAsync(() -> {
            try {
                LeaderboardManager lb = LeaderboardManager.getInstance();
                if (!lb.ensureRegisteredSync()) {
                    connecting.set(false);
                    scheduleReconnect();
                    return;
                }
                String token = lb.getPlayerToken();
                if (token == null || token.isEmpty()) {
                    connecting.set(false);
                    scheduleReconnect();
                    return;
                }

                String base = lb.getHttpClient() != null
                    ? lb.getHttpClient().getServerUrl()
                    : new LeaderboardConfig().getServerUrl();
                URI uri = URI.create(toWebSocketUrl(base) + "/ws/chat");

                WebSocket previous = socket.getAndSet(null);
                if (previous != null) {
                    try {
                        previous.abort();
                    } catch (Exception ignored) {
                    }
                }

                WebSocket ws = httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(uri, this)
                    .join();
                socket.set(ws);

                JsonObject auth = new JsonObject();
                auth.addProperty("type", "auth");
                auth.addProperty("token", token);
                ws.sendText(GSON.toJson(auth), true);
            } catch (Exception e) {
                connecting.set(false);
                authenticated.set(false);
                scheduleReconnect();
            }
        });
    }

    static String toWebSocketUrl(String httpUrl) {
        if (httpUrl == null || httpUrl.isEmpty()) {
            return "ws://localhost:2062";
        }
        String url = httpUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.startsWith("https://")) {
            return "wss://" + url.substring("https://".length());
        }
        if (url.startsWith("http://")) {
            return "ws://" + url.substring("http://".length());
        }
        if (url.startsWith("wss://") || url.startsWith("ws://")) {
            return url;
        }
        return "wss://" + url;
    }

    private void scheduleReconnect() {
        if (!wantConnected || intentionalClose.get()) {
            return;
        }
        long delay = reconnectDelayMs;
        reconnectDelayMs = Math.min(RECONNECT_MAX_MS, reconnectDelayMs * 2);
        CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS).execute(() -> {
            if (wantConnected && !intentionalClose.get() && !isConnected()) {
                connecting.set(false);
                connectAsync();
            }
        });
    }

    private void failAllPending(String error) {
        for (Map.Entry<String, CompletableFuture<String>> entry : pendingSends.entrySet()) {
            entry.getValue().complete(error);
        }
        pendingSends.clear();
    }

    private void handleServerPayload(String json) {
        JsonObject obj;
        try {
            obj = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return;
        }
        if (!obj.has("type")) {
            return;
        }
        String type = obj.get("type").getAsString();
        switch (type) {
            case "auth.ok" -> {
                authenticated.set(true);
                connecting.set(false);
                reconnectDelayMs = RECONNECT_BASE_MS;
            }
            case "auth.error" -> {
                authenticated.set(false);
                connecting.set(false);
                scheduleReconnect();
            }
            case "chat.message" -> {
                Consumer<JsonObject> cb = onChatMessage;
                if (cb != null) {
                    cb.accept(obj);
                }
            }
            case "chat.ack" -> {
                if (obj.has("requestId")) {
                    String id = obj.get("requestId").getAsString();
                    CompletableFuture<String> f = pendingSends.remove(id);
                    if (f != null) {
                        f.complete(null);
                    }
                }
            }
            case "chat.error" -> {
                String err = obj.has("error") ? obj.get("error").getAsString() : "Chat-Fehler";
                if (obj.has("requestId") && !obj.get("requestId").isJsonNull()) {
                    String id = obj.get("requestId").getAsString();
                    CompletableFuture<String> f = pendingSends.remove(id);
                    if (f != null) {
                        f.complete(err);
                        return;
                    }
                }
            }
            case "pong" -> {
                // keep-alive ok
            }
            default -> {
            }
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        textBuffer.append(data);
        if (last) {
            String payload = textBuffer.toString();
            textBuffer.setLength(0);
            handleServerPayload(payload);
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        authenticated.set(false);
        connecting.set(false);
        socket.compareAndSet(webSocket, null);
        failAllPending("WebSocket geschlossen.");
        if (!intentionalClose.get() && wantConnected) {
            scheduleReconnect();
        }
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        authenticated.set(false);
        connecting.set(false);
        socket.compareAndSet(webSocket, null);
        failAllPending("WebSocket-Fehler.");
        if (!intentionalClose.get() && wantConnected) {
            scheduleReconnect();
        }
    }
}

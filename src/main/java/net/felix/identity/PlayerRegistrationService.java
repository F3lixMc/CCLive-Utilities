package net.felix.identity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Eine gemeinsame Mod-Registrierung. Weitere Aufrufer hängen sich an denselben Vorgang
 * und starten keine parallelen Mojang-Nachweise oder Backendversuche.
 */
public final class PlayerRegistrationService {
    public static final int MAX_ATTEMPTS = 8;
    public static final long BASE_DELAY_MS = 5_000L;
    public static final long MAX_DELAY_MS = 300_000L;
    public static final long BUSY_DELAY_MS = 2_000L;

    public record Session(String name, String uuid) {}

    public record BackendCall(
            int status,
            String error,
            Integer retryAfterSeconds,
            String challengeId,
            String serverId,
            String token,
            String playerName
    ) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }

        public boolean rateLimited() {
            return status == 429;
        }
    }

    public static final class JoinFailed extends Exception {
        private final int status;
        private final boolean occupied;

        private JoinFailed(int status, boolean occupied, String message) {
            super(message);
            this.status = status;
            this.occupied = occupied;
        }

        public static JoinFailed busy() {
            return new JoinFailed(0, true, "Nachweis läuft bereits");
        }

        public static JoinFailed http(int status) {
            return new JoinFailed(status, false, "Mojang-joinServer fehlgeschlagen (" + status + ")");
        }

        public int status() {
            return status;
        }

        public boolean occupied() {
            return occupied;
        }

        public boolean rateLimited() {
            return status == 429;
        }
    }

    public interface Transport {
        Session session();

        BackendCall challenge(String proofSessionId) throws Exception;

        void joinServer(String serverId) throws JoinFailed, Exception;

        BackendCall register(String player, String uuid, String challengeId, String proofSessionId) throws Exception;
    }

    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    public interface Log {
        void info(String message);

        void warn(String message);
    }

    private final Transport transport;
    private final Sleeper sleeper;
    private final Log log;
    private final Object lock = new Object();
    private final AtomicReference<CompletableFuture<Boolean>> inFlight = new AtomicReference<>();
    private final AtomicReference<String> token = new AtomicReference<>();
    private final AtomicReference<String> playerName = new AtomicReference<>();
    private final AtomicReference<String> playerUuid = new AtomicReference<>();
    private final AtomicInteger challengeCalls = new AtomicInteger();
    private final AtomicInteger registerCalls = new AtomicInteger();
    private final AtomicInteger joinCalls = new AtomicInteger();
    private final AtomicLong cooldownUntilMs = new AtomicLong(0);

    public PlayerRegistrationService(Transport transport, Sleeper sleeper, Log log) {
        this.transport = transport;
        this.sleeper = sleeper;
        this.log = log;
    }

    public boolean hasToken() {
        String value = token.get();
        return value != null && !value.isBlank();
    }

    public String token() {
        return token.get();
    }

    public String playerName() {
        return playerName.get();
    }

    public String playerUuid() {
        return playerUuid.get();
    }

    public boolean registrationRunning() {
        CompletableFuture<Boolean> current = inFlight.get();
        return current != null && !current.isDone();
    }

    public long retryAfterMs() {
        return Math.max(0L, cooldownUntilMs.get() - System.currentTimeMillis());
    }

    public record Status(
            boolean trackingEnabled,
            boolean hasToken,
            boolean registrationRunning,
            String playerName,
            String playerUuid,
            long retryAfterMs
    ) {}

    public Status status(boolean trackingEnabled) {
        return new Status(
                trackingEnabled,
                hasToken(),
                registrationRunning(),
                playerName(),
                playerUuid(),
                retryAfterMs()
        );
    }

    public enum RefreshKind {
        STARTED,
        ATTACHED,
        RATE_LIMITED
    }

    public record RefreshOutcome(RefreshKind kind, CompletableFuture<Boolean> future, long retryAfterMs) {}

    public int challengeCalls() {
        return challengeCalls.get();
    }

    public int registerCalls() {
        return registerCalls.get();
    }

    public int joinCalls() {
        return joinCalls.get();
    }

    public CompletableFuture<Boolean> inFlightOrNull() {
        return inFlight.get();
    }

    public CompletableFuture<Boolean> ensureRegistered() {
        if (hasToken()) {
            return CompletableFuture.completedFuture(true);
        }
        synchronized (lock) {
            if (hasToken()) {
                return CompletableFuture.completedFuture(true);
            }
            CompletableFuture<Boolean> current = inFlight.get();
            if (current != null && !current.isDone()) {
                return current;
            }
            long now = System.currentTimeMillis();
            if (now < cooldownUntilMs.get()) {
                log.warn("Registrierung pausiert bis Ablauf der Wartezeit");
                return CompletableFuture.completedFuture(false);
            }
            CompletableFuture<Boolean> next = CompletableFuture.supplyAsync(this::runLoop);
            inFlight.set(next);
            return next;
        }
    }

    /**
     * Manueller Refresh über dieselbe zentrale Operation.
     * Läuft bereits ein Vorgang, wird kein zweiter gestartet.
     * Eine Wartezeit nach HTTP 429 bleibt bestehen.
     */
    public RefreshOutcome refresh() {
        synchronized (lock) {
            CompletableFuture<Boolean> current = inFlight.get();
            if (current != null && !current.isDone()) {
                return new RefreshOutcome(RefreshKind.ATTACHED, current, retryAfterMs());
            }
            long remaining = retryAfterMs();
            if (remaining > 0) {
                log.warn("Refresh übersprungen: Wartezeit nach Rate-Limit");
                return new RefreshOutcome(RefreshKind.RATE_LIMITED, CompletableFuture.completedFuture(hasToken()), remaining);
            }
            CompletableFuture<Boolean> next = CompletableFuture.supplyAsync(this::runLoop);
            inFlight.set(next);
            return new RefreshOutcome(RefreshKind.STARTED, next, 0);
        }
    }

    private boolean runLoop() {
        try {
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                AttemptResult result = oneAttempt(attempt + 1);
                if (result.success) {
                    cooldownUntilMs.set(0);
                    return true;
                }
                if (result.giveUp) {
                    break;
                }
                long wait = result.waitMs;
                if (wait > 0) {
                    cooldownUntilMs.set(System.currentTimeMillis() + wait);
                    log.info("Nächster Registrierungsversuch in " + (wait / 1000) + "s");
                    sleeper.sleep(wait);
                }
            }
            cooldownUntilMs.set(System.currentTimeMillis() + BASE_DELAY_MS);
            log.warn("Registrierung nach " + MAX_ATTEMPTS + " Versuchen pausiert");
            return hasToken();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return hasToken();
        } catch (Exception e) {
            log.warn("Registrierung abgebrochen: " + e.getMessage());
            return hasToken();
        }
    }

    private AttemptResult oneAttempt(int number) throws InterruptedException {
        Session session = transport.session();
        if (session == null || session.name() == null || session.uuid() == null) {
            log.warn("Registrierung Versuch " + number + ": kein Minecraft-Konto für den Nachweis");
            return AttemptResult.retry(backoffMs(number, null));
        }
        playerName.compareAndSet(null, session.name());

        String proofSessionId = UUID.randomUUID().toString() + UUID.randomUUID();
        BackendCall challenge;
        try {
            challengeCalls.incrementAndGet();
            challenge = transport.challenge(proofSessionId);
        } catch (Exception e) {
            log.warn("Challenge fehlgeschlagen: " + e.getMessage());
            return AttemptResult.retry(backoffMs(number, null));
        }
        logBackend("Challenge", challenge);
        if (challenge.rateLimited()) {
            return AttemptResult.retry(backoffMs(number, challenge.retryAfterSeconds()));
        }
        if (!challenge.ok() || challenge.challengeId() == null || challenge.serverId() == null) {
            log.warn("Challenge ohne gültige Antwort (HTTP " + challenge.status() + ", " + safeError(challenge) + ")");
            return AttemptResult.retry(backoffMs(number, challenge.retryAfterSeconds()));
        }

        try {
            joinCalls.incrementAndGet();
            transport.joinServer(challenge.serverId());
        } catch (JoinFailed e) {
            if (e.occupied()) {
                log.info("Nachweis belegt — warte, ohne den Zustand zu deaktivieren");
                return AttemptResult.retry(BUSY_DELAY_MS);
            }
            log.warn("Mojang-joinServer fehlgeschlagen (HTTP " + e.status() + ")");
            long wait = e.rateLimited() ? backoffMs(number, 30) : backoffMs(number, null);
            return AttemptResult.retry(wait);
        } catch (Exception e) {
            log.warn("Mojang-joinServer fehlgeschlagen: " + e.getMessage());
            return AttemptResult.retry(backoffMs(number, null));
        }

        BackendCall registered;
        try {
            registerCalls.incrementAndGet();
            registered = transport.register(session.name(), session.uuid(), challenge.challengeId(), proofSessionId);
        } catch (Exception e) {
            log.warn("Register-Anfrage fehlgeschlagen: " + e.getMessage());
            return AttemptResult.retry(backoffMs(number, null));
        }
        logBackend("Register", registered);
        if (registered.rateLimited()) {
            return AttemptResult.retry(backoffMs(number, registered.retryAfterSeconds()));
        }
        if (registered.ok() && registered.token() != null && !registered.token().isBlank()) {
            token.set(registered.token());
            playerUuid.set(session.uuid());
            if (registered.playerName() != null && !registered.playerName().isBlank()) {
                playerName.set(registered.playerName());
            } else {
                playerName.set(session.name());
            }
            log.info("Spieler '" + playerName.get() + "' erfolgreich registriert");
            return AttemptResult.ok();
        }
        log.warn("Register ohne Token (HTTP " + registered.status() + ", " + safeError(registered) + ")");
        return AttemptResult.retry(backoffMs(number, registered.retryAfterSeconds()));
    }

    private void logBackend(String step, BackendCall call) {
        log.info(step + " HTTP " + call.status() + " " + safeError(call));
    }

    static String safeError(BackendCall call) {
        if (call == null) {
            return "kein Fehlertext";
        }
        String error = call.error();
        if (error == null || error.isBlank()) {
            return "kein Fehlertext";
        }
        if (looksConfidential(error)) {
            return "Fehlertext unterdrückt";
        }
        return error;
    }

    static boolean looksConfidential(String value) {
        String text = value.toLowerCase();
        return text.contains("token")
                || text.contains("secret")
                || text.contains("password")
                || text.contains("proofsession")
                || text.contains("accesstoken")
                || text.contains("serverid");
    }

    static long backoffMs(int attemptNumber, Integer retryAfterSeconds) {
        long exponential = Math.min(MAX_DELAY_MS, BASE_DELAY_MS * (1L << Math.min(8, Math.max(0, attemptNumber - 1))));
        long fromHeader = retryAfterSeconds == null ? 0L : Math.max(0, retryAfterSeconds) * 1000L;
        return Math.max(exponential, fromHeader);
    }

    private record AttemptResult(boolean success, boolean giveUp, long waitMs) {
        static AttemptResult ok() {
            return new AttemptResult(true, false, 0);
        }

        static AttemptResult retry(long waitMs) {
            return new AttemptResult(false, false, waitMs);
        }
    }

    public static int parseRetryAfter(String header) {
        if (header == null || header.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(header.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String publicErrorFromJson(String errorField) {
        if (errorField == null || errorField.isBlank() || looksConfidential(errorField)) {
            return null;
        }
        return errorField;
    }

    public boolean await(long timeoutMs) {
        try {
            Boolean ok = ensureRegistered().get(timeoutMs, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(ok) && hasToken();
        } catch (Exception e) {
            return hasToken();
        }
    }
}

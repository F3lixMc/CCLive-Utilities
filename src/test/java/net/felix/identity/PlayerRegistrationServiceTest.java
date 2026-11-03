package net.felix.identity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRegistrationServiceTest {
    private static final PlayerRegistrationService.Session SESSION =
            new PlayerRegistrationService.Session("Steve", "069a79f4-44e1-4626-a5b0-bac67538bf5a");

    @Test
    void concurrentCallersShareOneAttempt() throws Exception {
        RecordingTransport transport = RecordingTransport.successAfter(1, 0);
        PlayerRegistrationService service = service(transport);
        CountDownLatch start = new CountDownLatch(1);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    start.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return service.ensureRegistered().join();
            }));
        }
        start.countDown();
        for (CompletableFuture<Boolean> future : futures) {
            assertTrue(future.get(5, TimeUnit.SECONDS));
        }
        assertEquals(1, transport.challenges.get());
        assertEquals(1, transport.joins.get());
        assertEquals(1, transport.registers.get());
        assertEquals("tok-1", service.token());
    }

    @Test
    void rateLimitThenSuccessHonorsRetryAfter() {
        RecordingTransport transport = RecordingTransport.successAfter(2, 429);
        PlayerRegistrationService service = service(transport);
        assertTrue(service.ensureRegistered().join());
        assertEquals(2, transport.challenges.get());
        assertEquals(1, transport.registers.get());
        assertTrue(transport.slept.stream().anyMatch(ms -> ms >= 7_000L));
        assertEquals("tok-1", service.token());
    }

    @Test
    void failedProofLogsBackendStatusAndDoesNotDisableLaterSuccessPath() {
        RecordingTransport transport = RecordingTransport.registerStatus(401, "Nachweis fehlgeschlagen");
        PlayerRegistrationService service = service(transport);
        assertFalse(service.ensureRegistered().join());
        assertFalse(service.hasToken());
        assertTrue(transport.registers.get() >= 1);
        assertEquals(transport.challenges.get(), transport.joins.get());
    }

    @Test
    void busyJoinDoesNotCountAsAuthFailureAndCallersAttach() throws Exception {
        RecordingTransport transport = RecordingTransport.busyThenSuccess();
        PlayerRegistrationService service = service(transport);
        CompletableFuture<Boolean> first = service.ensureRegistered();
        CompletableFuture<Boolean> second = service.ensureRegistered();
        assertTrue(first.get(5, TimeUnit.SECONDS));
        assertTrue(second.get(5, TimeUnit.SECONDS));
        assertEquals(first, second);
        assertTrue(transport.busyJoins.get() >= 1);
        assertEquals(1, transport.registers.get());
        assertEquals("tok-ok", service.token());
    }

    @Test
    void refreshAttachesToRunningAttempt() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        RecordingTransport transport = RecordingTransport.successAfter(1, 0);
        transport.blockChallenge(entered, release);
        PlayerRegistrationService service = service(transport);
        CompletableFuture<Boolean> running = service.ensureRegistered();
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        PlayerRegistrationService.RefreshOutcome refresh = service.refresh();
        assertEquals(PlayerRegistrationService.RefreshKind.ATTACHED, refresh.kind());
        assertEquals(1, transport.challenges.get());
        release.countDown();
        assertTrue(running.get(5, TimeUnit.SECONDS));
        assertTrue(refresh.future().get(5, TimeUnit.SECONDS));
        assertEquals(1, transport.registers.get());
        assertFalse(String.valueOf(service.status(true)).toLowerCase().contains("tok-"));
    }

    @Test
    void refreshDoesNotBypassCooldown() {
        RecordingTransport transport = RecordingTransport.registerStatus(401, "Nachweis fehlgeschlagen");
        PlayerRegistrationService service = service(transport);
        assertFalse(service.ensureRegistered().join());
        int challenges = transport.challenges.get();
        PlayerRegistrationService.RefreshOutcome refresh = service.refresh();
        assertEquals(PlayerRegistrationService.RefreshKind.RATE_LIMITED, refresh.kind());
        assertTrue(refresh.retryAfterMs() > 0);
        assertEquals(challenges, transport.challenges.get());
    }

    private static PlayerRegistrationService service(RecordingTransport transport) {
        return new PlayerRegistrationService(transport, transport::sleep, new PlayerRegistrationService.Log() {
            @Override
            public void info(String message) {
            }

            @Override
            public void warn(String message) {
            }
        });
    }

    private static final class RecordingTransport implements PlayerRegistrationService.Transport {
        private final AtomicInteger challenges = new AtomicInteger();
        private final AtomicInteger joins = new AtomicInteger();
        private final AtomicInteger registers = new AtomicInteger();
        private final AtomicInteger busyJoins = new AtomicInteger();
        private final CopyOnWriteArrayList<Long> slept = new CopyOnWriteArrayList<>();
        private final int successOnChallenge;
        private final int firstChallengeStatus;
        private final int registerStatus;
        private final String registerError;
        private final boolean busyFirstJoin;
        private CountDownLatch enteredChallenge;
        private CountDownLatch releaseChallenge;

        private RecordingTransport(int successOnChallenge, int firstChallengeStatus, int registerStatus, String registerError, boolean busyFirstJoin) {
            this.successOnChallenge = successOnChallenge;
            this.firstChallengeStatus = firstChallengeStatus;
            this.registerStatus = registerStatus;
            this.registerError = registerError;
            this.busyFirstJoin = busyFirstJoin;
        }

        void blockChallenge(CountDownLatch entered, CountDownLatch release) {
            this.enteredChallenge = entered;
            this.releaseChallenge = release;
        }

        static RecordingTransport successAfter(int attempt, int firstStatus) {
            return new RecordingTransport(attempt, firstStatus, 200, null, false);
        }

        static RecordingTransport registerStatus(int status, String error) {
            return new RecordingTransport(1, 200, status, error, false);
        }

        static RecordingTransport busyThenSuccess() {
            return new RecordingTransport(1, 200, 200, null, true);
        }

        @Override
        public PlayerRegistrationService.Session session() {
            return SESSION;
        }

        @Override
        public PlayerRegistrationService.BackendCall challenge(String proofSessionId) {
            int n = challenges.incrementAndGet();
            if (enteredChallenge != null) {
                enteredChallenge.countDown();
            }
            if (releaseChallenge != null) {
                try {
                    releaseChallenge.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (n < successOnChallenge) {
                return new PlayerRegistrationService.BackendCall(firstChallengeStatus == 0 ? 429 : firstChallengeStatus,
                        "Zu viele Versuche", 7, null, null, null, null);
            }
            return new PlayerRegistrationService.BackendCall(200, null, null, "ch-" + n, "sid-" + n, null, null);
        }

        @Override
        public void joinServer(String serverId) throws PlayerRegistrationService.JoinFailed {
            if (busyFirstJoin && busyJoins.get() == 0) {
                busyJoins.incrementAndGet();
                throw PlayerRegistrationService.JoinFailed.busy();
            }
            joins.incrementAndGet();
        }

        @Override
        public PlayerRegistrationService.BackendCall register(String player, String uuid, String challengeId, String proofSessionId) {
            int n = registers.incrementAndGet();
            if (registerStatus != 200) {
                return new PlayerRegistrationService.BackendCall(registerStatus, registerError, null, null, null, null, null);
            }
            return new PlayerRegistrationService.BackendCall(200, null, null, null, null, "tok-" + (busyFirstJoin ? "ok" : n), player);
        }

        void sleep(long millis) {
            slept.add(millis);
        }
    }
}

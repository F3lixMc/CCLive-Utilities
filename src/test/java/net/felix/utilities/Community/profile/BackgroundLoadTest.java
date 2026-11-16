package net.felix.utilities.Community.profile;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundLoadTest {
    @Test
    void delayedPlaytimeLoadDoesNotBlockAndSharesOneRun() throws Exception {
        BackgroundLoad loads = new BackgroundLoad();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        long started = System.nanoTime();
        boolean first = loads.start("ada", () -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Spielzeit blieb fünf Sekunden aus");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        boolean second = loads.start("ada", () -> calls.incrementAndGet());
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;

        assertTrue(first);
        assertFalse(second);
        assertTrue(elapsedMs < 1_000L, "Aufrufer wartete " + elapsedMs + " ms");
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        assertEquals(1, calls.get());
        release.countDown();
    }
}

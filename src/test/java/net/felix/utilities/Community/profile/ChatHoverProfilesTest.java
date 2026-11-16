package net.felix.utilities.Community.profile;

import com.google.gson.JsonObject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHoverProfilesTest {
    @Test
    void delayedProfileReturnsImmediatelyAndTheCallerContinues() throws Exception {
        ProfileFetchCoalescer fetches = new ProfileFetchCoalescer(60_000L);
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ProfileFetchCoalescer.Fetcher fetcher = player -> {
            calls.incrementAndGet();
            entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Profilantwort blieb fünf Sekunden aus");
            }
            JsonObject body = new JsonObject();
            body.addProperty("player", player);
            return new ProfileFetchCoalescer.Result(200, body);
        };

        long started = System.nanoTime();
        JsonObject first = ChatHoverProfiles.current(fetches, "Ada", "current", fetcher, 1L);
        JsonObject second = ChatHoverProfiles.current(fetches, "ada", "current", fetcher, 1L);
        AtomicBoolean callerContinued = new AtomicBoolean(true);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;

        assertNull(first);
        assertNull(second);
        assertTrue(callerContinued.get());
        assertTrue(elapsedMs < 1_000L, "Aufrufer wartete " + elapsedMs + " ms");
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        assertEquals(1, calls.get());
        assertTrue(fetches.pending("Ada", "current"));

        release.countDown();
        long deadline = System.currentTimeMillis() + 2_000L;
        JsonObject loaded = null;
        while (loaded == null && System.currentTimeMillis() < deadline) {
            loaded = fetches.available("Ada", "current");
            if (loaded == null) {
                Thread.sleep(10L);
            }
        }
        assertEquals("Ada", loaded.get("player").getAsString());
        assertFalse(fetches.pending("Ada", "current"));

        JsonObject cached = ChatHoverProfiles.current(fetches, "Ada", "current", player -> {
            calls.incrementAndGet();
            return new ProfileFetchCoalescer.Result(404, null);
        }, 1L);
        assertEquals("Ada", cached.get("player").getAsString());
        assertEquals(1, calls.get());
    }

    @Test
    void databaseErrorIsNotRememberedAndAStaleProfileStaysVisible() throws Exception {
        ProfileFetchCoalescer fetches = new ProfileFetchCoalescer(30L);
        CountDownLatch failed = new CountDownLatch(1);
        fetches.request("Bo", "current", player -> {
            failed.countDown();
            throw new java.io.IOException("down");
        }, 10L);
        assertTrue(failed.await(2, TimeUnit.SECONDS));
        long deadline = System.currentTimeMillis() + 2_000L;
        while (fetches.pending("Bo", "current") && System.currentTimeMillis() < deadline) {
            Thread.sleep(10L);
        }
        assertFalse(fetches.pending("Bo", "current"));
        assertFalse(fetches.remembersMissing("Bo", "current", 10L));

        JsonObject body = new JsonObject();
        body.addProperty("player", "Bo");
        CountDownLatch stored = new CountDownLatch(1);
        fetches.request("Bo", "current", player -> {
            stored.countDown();
            return new ProfileFetchCoalescer.Result(200, body);
        }, 10L);
        assertTrue(stored.await(2, TimeUnit.SECONDS));
        deadline = System.currentTimeMillis() + 2_000L;
        while (fetches.available("Bo", "current") == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(10L);
        }

        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger refresh = new AtomicInteger();
        long started = System.nanoTime();
        JsonObject shown = ChatHoverProfiles.current(fetches, "Bo", "current", player -> {
            refresh.incrementAndGet();
            entered.countDown();
            release.await(5, TimeUnit.SECONDS);
            return new ProfileFetchCoalescer.Result(200, body);
        }, 50L);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        assertEquals("Bo", shown.get("player").getAsString());
        assertTrue(elapsedMs < 1_000L);
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        assertEquals(1, refresh.get());
        release.countDown();
    }
}

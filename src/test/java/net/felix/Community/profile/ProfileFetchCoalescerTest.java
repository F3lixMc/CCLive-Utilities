package net.felix.Community.profile;

import com.google.gson.JsonObject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileFetchCoalescerTest {
    @Test
    void parallelRequestsForTheSamePlayerAndSeasonShareOneFetch() throws Exception {
        ProfileFetchCoalescer coalescer = new ProfileFetchCoalescer(60_000L);
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ProfileFetchCoalescer.Fetcher fetcher = player -> {
            calls.incrementAndGet();
            inside.countDown();
            release.await();
            JsonObject body = new JsonObject();
            body.addProperty("player", player);
            return new ProfileFetchCoalescer.Result(200, body);
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<JsonObject> first = pool.submit(() -> coalescer.fetch("Ada", "current", fetcher, 1L));
            inside.await();
            Future<JsonObject> second = pool.submit(() -> coalescer.fetch("Ada", "current", fetcher, 1L));
            release.countDown();
            assertEquals("Ada", first.get().get("player").getAsString());
            assertEquals("Ada", second.get().get("player").getAsString());
            assertEquals(1, calls.get());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void confirmedMissingProfileIsRememberedAndANetworkErrorIsNot() throws Exception {
        ProfileFetchCoalescer coalescer = new ProfileFetchCoalescer(60_000L);
        AtomicInteger calls = new AtomicInteger();
        assertNull(coalescer.fetch("Ada", "current", player -> {
            calls.incrementAndGet();
            return new ProfileFetchCoalescer.Result(404, null);
        }, 10L));
        assertNull(coalescer.fetch("Ada", "current", player -> {
            calls.incrementAndGet();
            return new ProfileFetchCoalescer.Result(200, new JsonObject());
        }, 20L));
        assertTrue(coalescer.remembersMissing("Ada", "current", 30L));
        assertEquals(1, calls.get());

        assertNull(coalescer.fetch("Bo", "9", player -> {
            throw new java.io.IOException("down");
        }, 10L));
        assertFalse(coalescer.remembersMissing("Bo", "9", 10L));
        JsonObject body = new JsonObject();
        body.addProperty("player", "Bo");
        assertEquals("Bo", coalescer.fetch("Bo", "9", player -> new ProfileFetchCoalescer.Result(200, body), 11L).get("player").getAsString());
    }

    @Test
    void anotherSeasonDoesNotReuseTheCurrentRequest() throws Exception {
        ProfileFetchCoalescer coalescer = new ProfileFetchCoalescer(60_000L);
        AtomicInteger calls = new AtomicInteger();
        ProfileFetchCoalescer.Fetcher fetcher = player -> {
            calls.incrementAndGet();
            return new ProfileFetchCoalescer.Result(404, null);
        };
        coalescer.fetch("Ada", "current", fetcher, 1L);
        coalescer.fetch("Ada", "9", fetcher, 1L);
        assertEquals(2, calls.get());
        assertTrue(coalescer.remembersMissing("Ada", "9", 2L));
    }
}

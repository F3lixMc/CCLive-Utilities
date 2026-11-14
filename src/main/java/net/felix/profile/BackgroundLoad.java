package net.felix.profile;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ein Hintergrundlauf pro Schlüssel. Der Aufrufer kehrt zurück, ohne auf das Ergebnis zu warten.
 */
public final class BackgroundLoad {
    private final Set<String> inflight = ConcurrentHashMap.newKeySet();

    public boolean start(String key, Runnable work) {
        if (key == null || key.isEmpty() || work == null || !inflight.add(key)) {
            return false;
        }
        CompletableFuture.runAsync(() -> {
            try {
                work.run();
            } finally {
                inflight.remove(key);
            }
        });
        return true;
    }
}

package net.felix.leaderboards;

/**
 * Wartezeit aus der ersten Antwort einer Sync-Runde.
 * Der Minecraft-Client und die Tests teilen sich diese Rechnung.
 */
public final class LeaderboardSyncSchedule {
    private LeaderboardSyncSchedule() {
    }

    /**
     * Ziel ist der Empfang der ersten Antwort plus den Serverabstand plus Versatz.
     * Liegt das Ziel schon hinter {@code nowMillis}, bleibt nur der Versatz.
     */
    public static long delayUntilNext(long receivedAtMillis, long serverTime, long nextSyncAt,
                                      long jitterMillis, long nowMillis) {
        long target = receivedAtMillis + (nextSyncAt - serverTime) + jitterMillis;
        long delay = target - nowMillis;
        if (delay < 0) {
            return Math.max(0L, jitterMillis);
        }
        return delay;
    }

    public static int pageCount(int rowCount) {
        if (rowCount <= 0) {
            return 1;
        }
        return (rowCount + 9) / 10;
    }

    public static int clampPage(int page, int rowCount) {
        int count = pageCount(rowCount);
        if (page < 0) {
            return 0;
        }
        if (page >= count) {
            return count - 1;
        }
        return page;
    }

    /** Der bisherige Stand bleibt, bis die Runde vollständig ist. */
    public static <T> T keepUntilComplete(T current, T incoming, boolean complete) {
        return complete && incoming != null ? incoming : current;
    }
}

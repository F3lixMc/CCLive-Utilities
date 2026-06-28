package net.felix.utilities.Overall;

public final class SessionRateUtility {

    public static final long UPDATE_INTERVAL_MS = 1000L;

    private SessionRateUtility() {
    }

    public static boolean shouldRecalculate(long lastUpdateTime, boolean valueChanged) {
        if (valueChanged) {
            return true;
        }
        if (lastUpdateTime == 0) {
            return true;
        }
        return System.currentTimeMillis() - lastUpdateTime >= UPDATE_INTERVAL_MS;
    }
}

package net.felix.Community.chat.share;

/**
 * Schätzt die Abweichung zwischen Clientuhr und Backenduhr.
 * Eine Probe mit größerer Unsicherheit ersetzt eine frische engere Probe nicht.
 */
public final class ChatShareClock {
    /** Laufzeit über PlayLegend nach dem Senden. */
    public static final long WINDOW_MS = 3_000L;
    /** sendChat kann vor der Annahme der Vorbereitung ankommen. */
    public static final long EARLY_MS = 1_000L;
    public static final long MAX_UNCERTAINTY_MS = 1_000L;
    public static final long SAMPLE_TTL_MS = 120_000L;

    public record Sample(long offsetMs, long uncertaintyMs, long takenAtClient) {
    }

    private Sample best;

    public synchronized void observe(long clientStart, long clientEnd, long serverTime) {
        if (clientEnd < clientStart) {
            return;
        }
        long uncertainty = (clientEnd - clientStart) / 2;
        long midpoint = clientStart + uncertainty;
        long offset = serverTime - midpoint;
        Sample sample = new Sample(offset, uncertainty, clientEnd);
        if (best == null || clientEnd - best.takenAtClient > SAMPLE_TTL_MS || uncertainty < best.uncertaintyMs) {
            best = sample;
        }
    }

    public synchronized Sample usable(long clientNow) {
        if (best == null || clientNow - best.takenAtClient > SAMPLE_TTL_MS || best.uncertaintyMs > MAX_UNCERTAINTY_MS) {
            return null;
        }
        return best;
    }

    /** Geschätzte Serverzeit des Empfangs liegt in {@code [T0 - Vorlauf - U, T0 + 3s + U]}. */
    public static boolean inWindow(long acceptedAt, long receivedClient, Sample sample) {
        if (sample == null) {
            return false;
        }
        long server = receivedClient + sample.offsetMs;
        long uncertainty = sample.uncertaintyMs;
        return server >= acceptedAt - EARLY_MS - uncertainty
                && server <= acceptedAt + WINDOW_MS + uncertainty;
    }

    /** Nach {@code T0 + 3s + U} kann keine weitere Zeile mehr in das Fenster fallen. */
    public static boolean windowClosed(long acceptedAt, long clientNow, Sample sample) {
        if (sample == null) {
            return false;
        }
        long server = clientNow + sample.offsetMs;
        return server > acceptedAt + WINDOW_MS + sample.uncertaintyMs;
    }
}

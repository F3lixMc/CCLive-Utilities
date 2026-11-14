package net.felix.utilities.Factory;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * Schätzt die Restzeit aus dem Zuwachs von {@code Level + Leistenfortschritt}.
 * Es gibt keine Fabrik-XP-Tabelle; die Rate ist nur der gemessene Fortschritt pro Zeit.
 */
public final class FactoryLevelEstimate {
    public enum Status {
        MEASURING,
        READY,
        STALLED
    }

    /** Mehrere Sekunden, bevor eine Rate gilt. */
    public static final long MIN_SPAN_MS = 8_000L;
    /** Kleinster Fortschritt, der als echter Zuwachs gilt. */
    public static final double MIN_GAIN = 0.005;
    /** So lange ohne Zuwachs, bis die alte Rate verworfen wird. */
    public static final long STALL_MS = 30_000L;
    public static final long WINDOW_MS = 120_000L;
    private static final double EPSILON = 0.0001;

    private final List<Sample> samples = new ArrayList<>();

    public void reset() {
        samples.clear();
    }

    public void observe(long timeMs, double progress) {
        if (!samples.isEmpty()) {
            double previous = samples.get(samples.size() - 1).progress;
            boolean dropped = progress + EPSILON < previous;
            boolean grewAfterStall = progress > previous + EPSILON && status(timeMs) == Status.STALLED;
            if (dropped || grewAfterStall) {
                samples.clear();
            }
        }
        long cutoff = timeMs - WINDOW_MS;
        samples.removeIf(sample -> sample.timeMs < cutoff);
        samples.add(new Sample(timeMs, progress));
    }

    public Status status(long now) {
        if (samples.isEmpty()) {
            return Status.MEASURING;
        }
        Sample first = samples.get(0);
        long lastIncrease = first.timeMs;
        double peak = first.progress;
        for (Sample sample : samples) {
            if (sample.progress > peak + EPSILON) {
                peak = sample.progress;
                lastIncrease = sample.timeMs;
            }
        }
        if (now - lastIncrease >= STALL_MS && now - first.timeMs >= STALL_MS) {
            return Status.STALLED;
        }
        Sample last = samples.get(samples.size() - 1);
        double gain = last.progress - first.progress;
        long span = last.timeMs - first.timeMs;
        if (gain >= MIN_GAIN && span >= MIN_SPAN_MS) {
            return Status.READY;
        }
        if (now - first.timeMs >= STALL_MS) {
            return Status.STALLED;
        }
        return Status.MEASURING;
    }

    /**
     * Millisekunden bis {@code targetLevel}. Leer, solange keine aktuelle Rate vorliegt.
     * {@code 0}, wenn das Ziel mit dem aktuellen Fortschritt schon erreicht ist.
     */
    public OptionalLong remainingMillis(long now, double currentProgress, double targetLevel) {
        if (status(now) != Status.READY || samples.size() < 2) {
            return OptionalLong.empty();
        }
        Sample first = samples.get(0);
        Sample last = samples.get(samples.size() - 1);
        double gain = last.progress - first.progress;
        long span = last.timeMs - first.timeMs;
        if (gain < MIN_GAIN || span < MIN_SPAN_MS) {
            return OptionalLong.empty();
        }
        double left = targetLevel - currentProgress;
        if (left <= 0.0) {
            return OptionalLong.of(0L);
        }
        double millis = left / (gain / span);
        if (millis >= Long.MAX_VALUE) {
            return OptionalLong.of(Long.MAX_VALUE);
        }
        return OptionalLong.of((long) Math.ceil(millis));
    }

    private record Sample(long timeMs, double progress) {
    }
}

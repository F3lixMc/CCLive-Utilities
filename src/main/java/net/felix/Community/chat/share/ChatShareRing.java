package net.felix.Community.chat.share;

import java.util.ArrayList;
import java.util.List;

/**
 * Sichtbare Chatzeilen, die auf eine Zuordnung warten.
 * Ein leeres Bündel lässt sie liegen. Heraus fallen sie erst nach Treffer oder Frist.
 */
public final class ChatShareRing<T> {
    public static final long HOLD_MS = 20_000L;

    public record Held<T>(T line, long receivedAt) {
    }

    private final List<Held<T>> lines = new ArrayList<>();

    public void add(T line, long now) {
        lines.add(new Held<>(line, now));
    }

    public List<Held<T>> live(long now) {
        return live(now, now);
    }

    /** {@code previousPollAt} vor dem Empfang hält die Zeile, bis ein Abruf sie gesehen hat. */
    public List<Held<T>> live(long now, long previousPollAt) {
        List<Held<T>> open = new ArrayList<>();
        for (Held<T> held : lines) {
            if (retained(held.receivedAt(), now, previousPollAt)) {
                open.add(held);
            }
        }
        return open;
    }

    public void matched(T line) {
        lines.removeIf(held -> held.line() == line);
    }

    public void replace(T from, T to) {
        for (int i = 0; i < lines.size(); i++) {
            Held<T> held = lines.get(i);
            if (held.line() == from) {
                lines.set(i, new Held<>(to, held.receivedAt()));
            }
        }
    }

    public int expire(long now) {
        return expire(now, now);
    }

    public int expire(long now, long previousPollAt) {
        int before = lines.size();
        lines.removeIf(held -> !retained(held.receivedAt(), now, previousPollAt));
        return before - lines.size();
    }

    private static boolean retained(long receivedAt, long now, long previousPollAt) {
        long age = now - receivedAt;
        if (age <= HOLD_MS) {
            return true;
        }
        return previousPollAt < receivedAt && age <= HOLD_MS * 3;
    }

    public int size() {
        return lines.size();
    }
}

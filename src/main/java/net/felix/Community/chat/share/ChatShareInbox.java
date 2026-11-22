package net.felix.Community.chat.share;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.felix.Community.chat.share.ChatShareCatalog.Bind;

/**
 * Lokale Prüfung gegen schon geladene Zuordnungen.
 * Der erste passende Empfang verbraucht eine Zuordnung sofort innerhalb des Zeitfensters.
 */
public final class ChatShareInbox {
    public record Line(String sender, String body, long receivedAt) {
    }

    public record Hit(Line line, Bind bind) {
    }

    private final ChatShareCatalog catalog = new ChatShareCatalog();
    private final ChatShareRing<Line> ring = new ChatShareRing<>();
    private final Set<String> consumed = new HashSet<>();

    public void accept(Line line) {
        if (line == null || line.sender() == null || line.body() == null) {
            return;
        }
        ring.add(line, line.receivedAt());
    }

    public List<Hit> applyBundle(List<Bind> binds, long now, ChatShareClock.Sample sample) {
        return applyBundle(binds, now, sample, now);
    }

    /**
     * {@code previousPollAt} ist der letzte erfolgreiche Abruf vor diesem.
     * Liegt er vor dem Empfang, bleibt eine ältere Zeile für genau diesen Abruf sichtbar.
     */
    public List<Hit> applyBundle(List<Bind> binds, long now, ChatShareClock.Sample sample, long previousPollAt) {
        if (binds != null) {
            for (Bind bind : binds) {
                catalog.put(bind);
            }
        }
        List<Hit> linked = new ArrayList<>();
        if (sample != null) {
            linked.addAll(settle(now, sample, previousPollAt));
        }
        ring.expire(now, sample == null ? previousPollAt : now);
        return linked;
    }

    public void remember(Bind bind) {
        catalog.put(bind);
    }

    public int waiting(long now) {
        return waiting(now, now);
    }

    public int waiting(long now, long previousPollAt) {
        ring.expire(now, previousPollAt);
        return ring.size();
    }

    private List<Hit> settle(long now, ChatShareClock.Sample sample, long previousPollAt) {
        List<Hit> linked = new ArrayList<>();
        List<Bind> available = catalog.all();
        for (ChatShareRing.Held<Line> held : ring.live(now, previousPollAt)) {
            Line line = held.line();
            for (Bind bind : available) {
                if (!consumed.contains(bind.attemptId()) && matches(bind, line, sample)) {
                    consumed.add(bind.attemptId());
                    linked.add(new Hit(line, bind));
                    ring.matched(line);
                    break;
                }
            }
        }
        return linked;
    }

    private static boolean matches(Bind bind, Line line, ChatShareClock.Sample sample) {
        if (bind == null || line.sender() == null || line.body() == null) {
            return false;
        }
        if (!line.sender().equals(bind.player()) || !ChatShareTexts.sha256(line.body()).equals(bind.bodyHash())) {
            return false;
        }
        if (!ChatShareClock.inWindow(bind.acceptedAt(), line.receivedAt(), sample)) {
            return false;
        }
        return ChatShareTexts.spanMatches(line.body(), bind.nameStart(), bind.nameLength(), bind.displayName());
    }
}

package net.felix.Community.chat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Geladene, bereits bestätigte Zuordnungen. Die Auswahl trifft der Ring, nicht der erste Treffer. */
public final class ChatShareCatalog {
    public record Bind(
            long cursor,
            String attemptId,
            String player,
            String bodyHash,
            String displayName,
            long acceptedAt,
            String shareType,
            String shareId,
            int nameStart,
            int nameLength,
            Integer nameColor) {
    }

    private final Map<String, Bind> byAttempt = new LinkedHashMap<>();

    public void put(Bind bind) {
        if (bind == null || bind.attemptId() == null) {
            return;
        }
        byAttempt.put(bind.attemptId(), bind);
    }

    public List<Bind> matching(String sender, String body, long receivedAt, ChatShareClock.Sample sample) {
        List<Bind> found = new ArrayList<>();
        if (sender == null || body == null || sample == null) {
            return found;
        }
        String hash = ChatShareTexts.sha256(body);
        for (Bind bind : byAttempt.values()) {
            if (!sender.equals(bind.player()) || !hash.equals(bind.bodyHash())) {
                continue;
            }
            if (!ChatShareClock.inWindow(bind.acceptedAt(), receivedAt, sample)) {
                continue;
            }
            if (!ChatShareTexts.spanMatches(body, bind.nameStart(), bind.nameLength(), bind.displayName())) {
                continue;
            }
            found.add(bind);
        }
        return found;
    }

    public List<Bind> all() {
        return new ArrayList<>(byAttempt.values());
    }

    public int size() {
        return byAttempt.size();
    }
}

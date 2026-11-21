package net.felix.Community.chat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Lokale Sendeversuche. Normale Nachrichten und Shares liegen in einer Schlange,
 * damit ein späteres gleiches Echo einen noch wartenden Share nicht bestätigt.
 */
public final class ChatShareAttempts {
    public static final long LOCAL_TTL_MS = 20_000L;

    public record Attempt(
            String attemptId,
            long sentAt,
            String body,
            boolean share,
            String shareType,
            String shareId,
            String displayName,
            int nameStart,
            int nameLength,
            Integer nameColor) {
    }

    private final List<Attempt> waiting = new ArrayList<>();

    public Attempt addPlain(String body, long now) {
        Attempt attempt = new Attempt(UUID.randomUUID().toString(), now, body, false, null, null, null, 0, 0, null);
        waiting.add(attempt);
        return attempt;
    }

    public Attempt addShare(
            String body,
            String shareType,
            String shareId,
            String displayName,
            int nameStart,
            int nameLength,
            long now) {
        return addShare(body, shareType, shareId, displayName, nameStart, nameLength, now, null);
    }

    public Attempt addShare(
            String body,
            String shareType,
            String shareId,
            String displayName,
            int nameStart,
            int nameLength,
            long now,
            Integer nameColor) {
        Attempt attempt = new Attempt(
                UUID.randomUUID().toString(), now, body, true, shareType, shareId, displayName, nameStart, nameLength,
                ChatShareNames.validRgb(nameColor));
        waiting.add(attempt);
        return attempt;
    }

    /** Genau ein Share und kein normaler Versuch: dieser Share. Sonst nichts, die Schlange bleibt. */
    public Attempt confirmEcho(String body, long now) {
        prune(now);
        Attempt share = null;
        int shares = 0;
        int plains = 0;
        for (Attempt attempt : waiting) {
            if (!attempt.body().equals(body)) {
                continue;
            }
            if (attempt.share()) {
                shares++;
                share = attempt;
            } else {
                plains++;
            }
        }
        if (shares == 1 && plains == 0) {
            waiting.remove(share);
            return share;
        }
        if (shares == 0 && plains == 1) {
            removePlain(body);
        }
        return null;
    }

    /** Genau ein Share, dessen Body am Ende der sichtbaren Zeile steht. */
    public Attempt shareBodyAtEnd(String plain, long now) {
        prune(now);
        if (plain == null) {
            return null;
        }
        Attempt found = null;
        int count = 0;
        for (Attempt attempt : waiting) {
            if (!attempt.share() || attempt.body() == null || attempt.body().isEmpty()) {
                continue;
            }
            if (!endsWithBody(plain, attempt.body())) {
                continue;
            }
            count++;
            found = attempt;
        }
        return count == 1 ? found : null;
    }

    private static boolean endsWithBody(String plain, String body) {
        if (!plain.endsWith(body)) {
            return false;
        }
        if (plain.length() == body.length()) {
            return true;
        }
        char before = plain.charAt(plain.length() - body.length() - 1);
        return before == ' ' || before == '>';
    }

    public int waitingCount() {
        return waiting.size();
    }

    /** Kurzer Grund, warum ein Echo keinen Share bestätigt hat. */
    public String diagnose(String body, long now) {
        prune(now);
        int shares = 0;
        int plains = 0;
        StringBuilder queue = new StringBuilder();
        for (Attempt attempt : waiting) {
            if (queue.length() > 0) {
                queue.append(" | ");
            }
            String text = attempt.body() == null ? "" : attempt.body();
            if (text.length() > 80) {
                text = text.substring(0, 80);
            }
            queue.append(attempt.share() ? "share:" : "plain:").append(text);
            if (body != null && body.equals(attempt.body())) {
                if (attempt.share()) {
                    shares++;
                } else {
                    plains++;
                }
            }
        }
        return "passendeShares=" + shares + " passendeNormal=" + plains + " wartend=[" + queue + "]";
    }

    public void drop(Attempt attempt) {
        if (attempt != null) {
            waiting.remove(attempt);
        }
    }

    public void clear() {
        waiting.clear();
    }

    private void removePlain(String body) {
        Iterator<Attempt> iterator = waiting.iterator();
        while (iterator.hasNext()) {
            Attempt attempt = iterator.next();
            if (!attempt.share() && attempt.body().equals(body)) {
                iterator.remove();
                return;
            }
        }
    }

    private void prune(long now) {
        waiting.removeIf(attempt -> now - attempt.sentAt() > LOCAL_TTL_MS);
    }
}

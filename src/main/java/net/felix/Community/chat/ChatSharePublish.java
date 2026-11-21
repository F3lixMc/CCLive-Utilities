package net.felix.Community.chat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Vorbereitung und Bestätigung eines Versuchs.
 * Das Echo darf vor der Annahme eintreffen. Bestätigt wird höchstens einmal.
 */
public final class ChatSharePublish {
    public enum Step {
        PREPARE,
        CONFIRM
    }

    public record Next(ChatShareAttempts.Attempt attempt, Step step) {
    }

    private final Map<String, ChatShareAttempts.Attempt> attempts = new HashMap<>();
    private final Set<String> prepared = new HashSet<>();
    private final Set<String> echo = new HashSet<>();
    private final Set<String> cancelled = new HashSet<>();
    private final Set<String> failed = new HashSet<>();
    private final Set<String> confirmQueued = new HashSet<>();

    public Next noted(ChatShareAttempts.Attempt attempt) {
        if (attempt == null || attempt.attemptId() == null) {
            return null;
        }
        attempts.put(attempt.attemptId(), attempt);
        return new Next(attempt, Step.PREPARE);
    }

    public void cancel(ChatShareAttempts.Attempt attempt) {
        if (attempt != null && attempt.attemptId() != null) {
            cancelled.add(attempt.attemptId());
        }
    }

    public Next preparedOk(String attemptId) {
        if (!open(attemptId)) {
            return null;
        }
        prepared.add(attemptId);
        return confirmIfReady(attemptId);
    }

    public void preparedRejected(String attemptId) {
        if (attemptId != null) {
            failed.add(attemptId);
        }
    }

    public Next echo(ChatShareAttempts.Attempt attempt) {
        if (attempt == null || !open(attempt.attemptId())) {
            return null;
        }
        attempts.put(attempt.attemptId(), attempt);
        echo.add(attempt.attemptId());
        return confirmIfReady(attempt.attemptId());
    }

    public void clear() {
        attempts.clear();
        prepared.clear();
        echo.clear();
        cancelled.clear();
        failed.clear();
        confirmQueued.clear();
    }

    private Next confirmIfReady(String attemptId) {
        if (!prepared.contains(attemptId) || !echo.contains(attemptId) || !confirmQueued.add(attemptId)) {
            return null;
        }
        return new Next(attempts.get(attemptId), Step.CONFIRM);
    }

    private boolean open(String attemptId) {
        return attemptId != null
                && attempts.containsKey(attemptId)
                && !cancelled.contains(attemptId)
                && !failed.contains(attemptId);
    }
}

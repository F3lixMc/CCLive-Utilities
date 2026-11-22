package net.felix.Community.chat.ignore;

import java.util.List;

/**
 * Zustand der Ignore-Suche. Neue Vorschläge tauschen das Eingabefeld nicht aus.
 */
final class ChatIgnoreTyping {
    private final Object field = new Object();
    private String text = "";
    private int cursor;
    private int selectionAnchor;
    private long generation;
    private List<ChatIgnoreRules.Match> suggestions = List.of();
    private String status = "";
    private int submits;

    Object field() {
        return field;
    }

    String text() {
        return text;
    }

    int cursor() {
        return cursor;
    }

    int selectionAnchor() {
        return selectionAnchor;
    }

    long generation() {
        return generation;
    }

    List<ChatIgnoreRules.Match> suggestions() {
        return suggestions;
    }

    String status() {
        return status;
    }

    int submits() {
        return submits;
    }

    void replaceText(String value, int cursor, int selectionAnchor) {
        text = value == null ? "" : value;
        this.cursor = cursor;
        this.selectionAnchor = selectionAnchor;
        generation++;
    }

    boolean offer(long generation, String query, List<ChatIgnoreRules.Match> matches, String error) {
        if (generation != this.generation || query == null || !query.equals(text.trim())) {
            return false;
        }
        if (error != null) {
            status = error;
            suggestions = List.of();
            return true;
        }
        suggestions = matches == null ? List.of() : List.copyOf(matches);
        status = suggestions.isEmpty() && text.trim().length() >= 2 ? "Keine passenden Spieler." : "";
        return true;
    }

    void pick(ChatIgnoreRules.Match match) {
        if (match == null || match.name() == null) {
            return;
        }
        text = match.name();
        cursor = text.length();
        selectionAnchor = cursor;
        generation++;
    }

    String submit(List<ChatIgnoreRules.Match> matches, String localName, Long localPlayerId) {
        submits++;
        return ChatIgnoreRules.rejection(text, matches, localName, localPlayerId);
    }
}

package net.felix.utilities.Overall.eventsound.data;

import java.util.UUID;

/**
 * Eine gespeicherte Zuordnung von Chat-Ereignis zu Sounddatei und Lautstärke.
 */
public final class EventSoundRule {
    public String id;
    public EventSoundKind kind;
    public String target;
    public String soundFile;
    public float volume;
    /** 0 spielt den Sound bis zum Ende. Sonst die maximale Dauer in Sekunden. */
    public float maxSeconds;
    public String pattern;
    public boolean fromPlayer;
    public boolean fromServer;
    public EventSoundMatchMode matchMode;

    public EventSoundRule() {
        this.id = UUID.randomUUID().toString();
        this.volume = 1f;
        this.target = "";
        this.soundFile = "";
        this.pattern = "";
        this.fromPlayer = true;
        this.fromServer = true;
        this.matchMode = EventSoundMatchMode.CONTAINS;
    }

    public EventSoundRule copy() {
        EventSoundRule copy = new EventSoundRule();
        copy.id = id;
        copy.kind = kind;
        copy.target = target;
        copy.soundFile = soundFile;
        copy.volume = volume;
        copy.maxSeconds = maxSeconds;
        copy.pattern = pattern;
        copy.fromPlayer = fromPlayer;
        copy.fromServer = fromServer;
        copy.matchMode = matchMode;
        return copy;
    }

    public String listLabel() {
        if (kind == EventSoundKind.SMITHING) {
            return "Schmiedezustand: " + target;
        }
        if (kind == EventSoundKind.BLUEPRINT) {
            return "Bauplan: " + target;
        }
        return pattern == null || pattern.isBlank() ? "Custom" : pattern;
    }

    /** Leere Eingabe ist 0. Ungültiger Text ist null. */
    public static Float parseMaxSeconds(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0f;
        }
        String normalized = raw.trim().replace(',', '.');
        try {
            float seconds = Float.parseFloat(normalized);
            if (!Float.isFinite(seconds) || seconds <= 0f) {
                return null;
            }
            return seconds;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static String formatMaxSeconds(float seconds) {
        if (!(seconds > 0f) || !Float.isFinite(seconds)) {
            return "";
        }
        float rounded = Math.round(seconds * 100f) / 100f;
        if (Math.abs(rounded - Math.round(rounded)) < 0.001f) {
            return String.valueOf(Math.round(rounded));
        }
        String text = String.format(java.util.Locale.US, "%.2f", rounded);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}

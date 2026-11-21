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
}

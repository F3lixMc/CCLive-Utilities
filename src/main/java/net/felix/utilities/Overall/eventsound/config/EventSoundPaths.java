package net.felix.utilities.Overall.eventsound.config;

import java.nio.file.Path;
import net.felix.CCLiveUtilities;

/**
 * Ordner für Event-Sound-Regeln und die kopierten Dateien.
 */
public final class EventSoundPaths {
    private static Path root;

    private EventSoundPaths() {
    }

    public static Path root() {
        if (root == null) {
            root = CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve("event-sounds");
        }
        return root;
    }

    public static Path files() {
        return root().resolve("files");
    }

    public static Path rulesFile() {
        return root().resolve("rules.json");
    }

    public static void useRoot(Path path) {
        root = path;
    }
}

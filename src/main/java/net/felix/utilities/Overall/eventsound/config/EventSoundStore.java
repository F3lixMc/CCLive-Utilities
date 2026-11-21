package net.felix.utilities.Overall.eventsound.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.felix.utilities.Overall.eventsound.data.EventSoundKind;
import net.felix.utilities.Overall.eventsound.data.EventSoundRule;

/**
 * Lädt und speichert die Event-Sound-Regeln als JSON.
 */
public final class EventSoundStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<EventSoundRule> RULES = new ArrayList<>();
    private static boolean loaded;

    private EventSoundStore() {
    }

    public static void load() {
        synchronized (RULES) {
            RULES.clear();
            Path file = EventSoundPaths.rulesFile();
            if (Files.isRegularFile(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    FileContents contents = GSON.fromJson(reader, FileContents.class);
                    if (contents != null && contents.rules != null) {
                        for (EventSoundRule rule : contents.rules) {
                            if (rule != null && rule.kind != null && rule.id != null && !rule.id.isBlank()) {
                                RULES.add(rule);
                            }
                        }
                    }
                } catch (IOException | RuntimeException ignored) {
                    RULES.clear();
                }
            }
            loaded = true;
        }
    }

    public static List<EventSoundRule> rules() {
        synchronized (RULES) {
            if (!loaded) {
                load();
            }
            List<EventSoundRule> copies = new ArrayList<>();
            for (EventSoundRule rule : RULES) {
                copies.add(rule.copy());
            }
            return Collections.unmodifiableList(copies);
        }
    }

    public static Optional<EventSoundRule> findSmithing(String state) {
        return rules().stream()
                .filter(rule -> rule.kind == EventSoundKind.SMITHING && state.equals(rule.target))
                .findFirst();
    }

    public static Optional<EventSoundRule> findBlueprint(String name) {
        return rules().stream()
                .filter(rule -> rule.kind == EventSoundKind.BLUEPRINT && name.equals(rule.target))
                .findFirst();
    }

    public static void save(EventSoundRule rule) {
        if (rule == null || rule.kind == null || rule.id == null) {
            return;
        }
        rule.volume = Math.max(0f, Math.min(1f, rule.volume));
        synchronized (RULES) {
            if (!loaded) {
                load();
            }
            RULES.removeIf(existing -> sameSlot(existing, rule));
            RULES.add(rule.copy());
            write();
        }
    }

    public static void remove(String id) {
        if (id == null) {
            return;
        }
        synchronized (RULES) {
            if (!loaded) {
                load();
            }
            if (RULES.removeIf(rule -> id.equals(rule.id))) {
                write();
            }
        }
    }

    private static boolean sameSlot(EventSoundRule existing, EventSoundRule rule) {
        if (existing.id.equals(rule.id)) {
            return true;
        }
        if (rule.kind == EventSoundKind.SMITHING && existing.kind == EventSoundKind.SMITHING) {
            return rule.target != null && rule.target.equals(existing.target);
        }
        if (rule.kind == EventSoundKind.BLUEPRINT && existing.kind == EventSoundKind.BLUEPRINT) {
            return rule.target != null && rule.target.equals(existing.target);
        }
        return false;
    }

    private static void write() {
        try {
            Files.createDirectories(EventSoundPaths.root());
            FileContents contents = new FileContents();
            contents.rules = new ArrayList<>(RULES);
            Files.writeString(EventSoundPaths.rulesFile(), GSON.toJson(contents), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static void resetForTests() {
        synchronized (RULES) {
            RULES.clear();
            loaded = false;
        }
    }

    private static final class FileContents {
        List<EventSoundRule> rules = new ArrayList<>();
    }
}

package net.felix.utilities.Overall.eventsound.sound;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.felix.utilities.Overall.eventsound.config.EventSoundPaths;
import net.minecraft.client.Minecraft;

/**
 * Kopiert gewählte Sounddateien in den lokalen Ordner und listet sie auf.
 */
public final class EventSoundLibrary {
    private EventSoundLibrary() {
    }

    public static List<String> fileNames() {
        Path directory = EventSoundPaths.files();
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        try (var stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(EventSoundLibrary::isSupportedName)
                    .sorted(Comparator.naturalOrder())
                    .forEach(names::add);
        } catch (IOException ignored) {
            return List.of();
        }
        return names;
    }

    public static Path resolve(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.contains("..") || fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
            return null;
        }
        Path path = EventSoundPaths.files().resolve(fileName);
        return Files.isRegularFile(path) ? path : null;
    }

    public static boolean isSupportedName(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".ogg") || lower.endsWith(".wav");
    }

    /**
     * Kopiert eine Datei in den Sound-Ordner und gibt den gespeicherten Dateinamen zurück.
     */
    public static String importFile(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("Datei fehlt");
        }
        String original = source.getFileName().toString();
        if (!isSupportedName(original)) {
            throw new IOException("Nur .ogg und .wav");
        }
        Path directory = EventSoundPaths.files();
        Files.createDirectories(directory);
        Path sourceAbsolute = source.toAbsolutePath().normalize();
        if (sourceAbsolute.startsWith(directory.toAbsolutePath().normalize()) && Files.isRegularFile(sourceAbsolute)) {
            return sourceAbsolute.getFileName().toString();
        }
        String safe = sanitize(original);
        Path destination = directory.resolve(safe);
        int suffix = 2;
        while (Files.exists(destination) && !Files.isSameFile(destination, sourceAbsolute)) {
            destination = directory.resolve(withSuffix(safe, suffix));
            suffix++;
        }
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.getFileName().toString();
    }

    public static void openFolder() throws IOException {
        Path directory = EventSoundPaths.files().toAbsolutePath();
        Files.createDirectories(directory);
        String path = directory.toString();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            new ProcessBuilder("explorer.exe", path).start();
            return;
        }
        if (os.contains("mac")) {
            new ProcessBuilder("open", path).start();
            return;
        }
        new ProcessBuilder("xdg-open", path).start();
    }

    public static void pickFile(Consumer<Path> onPicked, Consumer<String> onError) {
        Minecraft client = Minecraft.getInstance();
        Thread thread = new Thread(() -> {
            try {
                System.setProperty("java.awt.headless", "false");
                FileDialog dialog = new FileDialog((Frame) null, "Sound auswählen", FileDialog.LOAD);
                dialog.setFile("*.ogg;*.wav");
                dialog.setVisible(true);
                String file = dialog.getFile();
                String directory = dialog.getDirectory();
                dialog.dispose();
                if (file == null || directory == null) {
                    return;
                }
                Path selected = Path.of(directory, file);
                if (client != null) {
                    client.execute(() -> onPicked.accept(selected));
                } else {
                    onPicked.accept(selected);
                }
            } catch (RuntimeException exception) {
                if (client != null) {
                    client.execute(() -> onError.accept("Dateiauswahl konnte nicht geöffnet werden"));
                }
            }
        }, "event-sound-file-dialog");
        thread.setDaemon(true);
        thread.start();
    }

    private static String sanitize(String fileName) {
        String cleaned = fileName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (cleaned.isBlank() || ".".equals(cleaned) || "..".equals(cleaned)) {
            return "sound.ogg";
        }
        return cleaned;
    }

    private static String withSuffix(String fileName, int suffix) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return fileName + "-" + suffix;
        }
        return fileName.substring(0, dot) + "-" + suffix + fileName.substring(dot);
    }
}

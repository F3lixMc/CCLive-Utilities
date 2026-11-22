package net.felix.utilities.Overall.eventsound.sound;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    public static final String BUNDLED_STARFORGED = "starforged.ogg";
    public static final String BUNDLED_RICK = "Rick.ogg";
    private static final String BUNDLED_RESOURCE_ROOT = "/eventsounds/";
    private static final List<String> BUNDLED = List.of(BUNDLED_STARFORGED, BUNDLED_RICK);

    private EventSoundLibrary() {
    }

    /** Legt mitgelieferte Sounds in den lokalen Ordner, falls sie dort noch fehlen. */
    public static void ensureBundled() {
        try {
            Path directory = EventSoundPaths.files();
            Files.createDirectories(directory);
            for (String name : BUNDLED) {
                Path destination = directory.resolve(name);
                if (Files.isRegularFile(destination)) {
                    continue;
                }
                try (InputStream input = EventSoundLibrary.class.getResourceAsStream(BUNDLED_RESOURCE_ROOT + name)) {
                    if (input == null) {
                        continue;
                    }
                    Files.copy(input, destination);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static List<String> fileNames() {
        ensureBundled();
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
        ensureBundled();
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
                Path selected = pickNative();
                if (selected == null) {
                    return;
                }
                if (client != null) {
                    client.execute(() -> onPicked.accept(selected));
                } else {
                    onPicked.accept(selected);
                }
            } catch (Exception exception) {
                if (client != null) {
                    client.execute(() -> onError.accept("Dateiauswahl konnte nicht geöffnet werden"));
                }
            }
        }, "event-sound-file-dialog");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Minecraft startet Java ohne AWT-Fenster. Der Windows-Dialog läuft deshalb außerhalb der JVM.
     */
    private static Path pickNative() throws IOException, InterruptedException {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return pickWindows();
        }
        return pickAwt();
    }

    private static Path pickWindows() throws IOException, InterruptedException {
        Path output = Files.createTempFile("cclive-sound-pick", ".txt");
        Files.delete(output);
        String quoted = output.toAbsolutePath().toString().replace("'", "''");
        String script = """
                $ProgressPreference = 'SilentlyContinue'
                $ErrorActionPreference = 'Stop'
                Add-Type -AssemblyName System.Windows.Forms
                $owner = New-Object System.Windows.Forms.Form
                $owner.TopMost = $true
                $owner.ShowInTaskbar = $false
                $owner.Opacity = 0
                $owner.Show()
                $owner.Activate()
                $dialog = New-Object System.Windows.Forms.OpenFileDialog
                $dialog.Title = 'Sound auswählen'
                $dialog.Filter = 'Sounddateien (*.ogg;*.wav)|*.ogg;*.wav'
                $dialog.CheckFileExists = $true
                $ok = $dialog.ShowDialog($owner) -eq [System.Windows.Forms.DialogResult]::OK
                if ($ok) {
                    [System.IO.File]::WriteAllText('%s', $dialog.FileName, (New-Object System.Text.UTF8Encoding $false))
                }
                $owner.Close()
                $owner.Dispose()
                """.formatted(quoted);
        Process process = new ProcessBuilder("powershell.exe", "-STA", "-NoProfile", "-Command", script)
                .redirectErrorStream(true)
                .start();
        String log = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new IOException(log.isBlank() ? "Dateidialog fehlgeschlagen" : log.strip());
        }
        if (!Files.isRegularFile(output)) {
            return null;
        }
        String chosen = Files.readString(output, StandardCharsets.UTF_8).trim();
        Files.deleteIfExists(output);
        return chosen.isEmpty() ? null : Path.of(chosen);
    }

    private static Path pickAwt() {
        System.setProperty("java.awt.headless", "false");
        Frame frame = new Frame();
        try {
            frame.setAlwaysOnTop(true);
            FileDialog dialog = new FileDialog(frame, "Sound auswählen", FileDialog.LOAD);
            dialog.setFile("*.ogg;*.wav");
            dialog.setVisible(true);
            String file = dialog.getFile();
            String directory = dialog.getDirectory();
            dialog.dispose();
            if (file == null || directory == null) {
                return null;
            }
            return Path.of(directory, file);
        } finally {
            frame.dispose();
        }
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

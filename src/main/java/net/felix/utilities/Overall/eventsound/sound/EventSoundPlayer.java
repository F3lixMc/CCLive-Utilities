package net.felix.utilities.Overall.eventsound.sound;

import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.SoundBuffer;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;

/**
 * Spielt eine lokale OGG- oder WAV-Datei über Minecrafts OpenAL-Kontext ab.
 */
public final class EventSoundPlayer {
    private static final Map<String, CachedBuffer> CACHE = new HashMap<>();
    private static final List<Playing> PLAYING = new ArrayList<>();
    private static Field soundEngineField;
    private static Field channelAccessField;

    private EventSoundPlayer() {
    }

    public static void playFile(String fileName, float userVolume) {
        Path path = EventSoundLibrary.resolve(fileName);
        if (path == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> playNow(client, path, userVolume));
    }

    private static void playNow(Minecraft client, Path path, float userVolume) {
        float gain = gainFor(client, userVolume);
        if (gain <= 0f) {
            return;
        }
        SoundBuffer buffer = bufferFor(path);
        ChannelAccess access = channelAccess(client);
        if (buffer == null || access == null) {
            return;
        }
        String fileName = path.getFileName().toString();
        access.createHandle(Library.Pool.STATIC).thenAccept(handle -> {
            if (handle == null) {
                return;
            }
            synchronized (PLAYING) {
                PLAYING.add(new Playing(handle, fileName));
            }
            handle.execute(channel -> {
                channel.attachStaticBuffer(buffer);
                channel.disableAttenuation();
                channel.setRelative(true);
                channel.setVolume(gain);
                channel.setPitch(1f);
                channel.play();
            });
        });
    }

    /** Passt die Lautstärke der gerade laufenden Wiedergabe dieser Datei an. */
    public static void updatePlayingVolume(String fileName, float userVolume) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        float gain = gainFor(client, userVolume);
        List<Playing> active = activeCopies();
        for (Playing playing : active) {
            if (fileName.equals(playing.fileName)) {
                playing.handle.execute(channel -> channel.setVolume(gain));
            }
        }
    }

    /** Stoppt alle Sounds, die über die Event-Sounds gestartet wurden. */
    public static void stopAll() {
        List<Playing> active = activeCopies();
        synchronized (PLAYING) {
            PLAYING.clear();
        }
        for (Playing playing : active) {
            playing.handle.execute(channel -> channel.stop());
        }
    }

    private static float gainFor(Minecraft client, float userVolume) {
        float volume = Float.isFinite(userVolume) ? userVolume : 1f;
        return Math.max(0f, Math.min(1f, volume)) * client.options.getFinalSoundSourceVolume(SoundSource.UI);
    }

    private static List<Playing> activeCopies() {
        synchronized (PLAYING) {
            Iterator<Playing> iterator = PLAYING.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().handle.isStopped()) {
                    iterator.remove();
                }
            }
            return new ArrayList<>(PLAYING);
        }
    }

    private static SoundBuffer bufferFor(Path path) {
        try {
            long modified = Files.getLastModifiedTime(path).toMillis();
            String key = path.toAbsolutePath().normalize() + "@" + modified;
            synchronized (CACHE) {
                CachedBuffer cached = CACHE.get(key);
                if (cached != null) {
                    return cached.buffer;
                }
                SoundBuffer decoded = decode(path);
                if (decoded == null) {
                    return null;
                }
                CACHE.put(key, new CachedBuffer(decoded));
                return decoded;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static SoundBuffer decode(Path path) throws Exception {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".ogg")) {
            try (InputStream input = Files.newInputStream(path); JOrbisAudioStream stream = new JOrbisAudioStream(input)) {
                return new SoundBuffer(direct(stream.readAll()), stream.getFormat());
            }
        }
        if (name.endsWith(".wav")) {
            try (AudioInputStream source = AudioSystem.getAudioInputStream(path.toFile())) {
                AudioFormat format = source.getFormat();
                AudioFormat pcm = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        format.getSampleRate(),
                        16,
                        format.getChannels(),
                        format.getChannels() * 2,
                        format.getSampleRate(),
                        false);
                try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcm, source)) {
                    byte[] bytes = pcmStream.readAllBytes();
                    ByteBuffer data = ByteBuffer.allocateDirect(bytes.length);
                    data.put(bytes);
                    data.flip();
                    return new SoundBuffer(data, pcm);
                }
            }
        }
        return null;
    }

    private static ByteBuffer direct(ByteBuffer data) {
        if (data == null) {
            return ByteBuffer.allocateDirect(0);
        }
        if (data.isDirect()) {
            return data;
        }
        ByteBuffer copy = ByteBuffer.allocateDirect(data.remaining());
        copy.put(data);
        copy.flip();
        return copy;
    }

    private static ChannelAccess channelAccess(Minecraft client) {
        try {
            SoundManager manager = client.getSoundManager();
            if (manager == null) {
                return null;
            }
            if (soundEngineField == null) {
                soundEngineField = SoundManager.class.getDeclaredField("soundEngine");
                soundEngineField.setAccessible(true);
            }
            if (channelAccessField == null) {
                channelAccessField = SoundEngine.class.getDeclaredField("channelAccess");
                channelAccessField.setAccessible(true);
            }
            Object engine = soundEngineField.get(manager);
            if (engine == null) {
                return null;
            }
            return (ChannelAccess) channelAccessField.get(engine);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private record CachedBuffer(SoundBuffer buffer) {
    }

    private record Playing(ChannelAccess.ChannelHandle handle, String fileName) {
    }
}

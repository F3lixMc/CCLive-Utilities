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
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
    private static boolean durationTickRegistered;

    private EventSoundPlayer() {
    }

    /** Länge der Datei in Sekunden, oder -1 wenn sie nicht lesbar ist. */
    public static float durationSeconds(String fileName) {
        Path path = EventSoundLibrary.resolve(fileName);
        if (path == null) {
            return -1f;
        }
        try {
            String key = cacheKey(path);
            synchronized (CACHE) {
                CachedBuffer cached = CACHE.get(key);
                if (cached != null) {
                    return cached.seconds;
                }
            }
            if (bufferFor(path) == null) {
                return -1f;
            }
            synchronized (CACHE) {
                CachedBuffer cached = CACHE.get(key);
                return cached == null ? -1f : cached.seconds;
            }
        } catch (Exception ignored) {
            return -1f;
        }
    }

    public static void playFile(String fileName, float userVolume, float maxSeconds) {
        Path path = EventSoundLibrary.resolve(fileName);
        if (path == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        ensureDurationTick();
        float limit = Float.isFinite(maxSeconds) && maxSeconds > 0f ? maxSeconds : 0f;
        client.execute(() -> playNow(client, path, userVolume, limit));
    }

    private static void playNow(Minecraft client, Path path, float userVolume, float maxSeconds) {
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
        long stopAtMs = maxSeconds > 0f ? System.currentTimeMillis() + (long) (maxSeconds * 1000f) : 0L;
        access.createHandle(Library.Pool.STATIC).thenAccept(handle -> {
            if (handle == null) {
                return;
            }
            synchronized (PLAYING) {
                PLAYING.add(new Playing(handle, fileName, stopAtMs));
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

    private static void ensureDurationTick() {
        if (durationTickRegistered) {
            return;
        }
        durationTickRegistered = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> stopExpired());
    }

    private static void stopExpired() {
        long now = System.currentTimeMillis();
        List<Playing> expired = new ArrayList<>();
        synchronized (PLAYING) {
            Iterator<Playing> iterator = PLAYING.iterator();
            while (iterator.hasNext()) {
                Playing playing = iterator.next();
                if (playing.stopAtMs > 0L && now >= playing.stopAtMs) {
                    iterator.remove();
                    expired.add(playing);
                }
            }
        }
        for (Playing playing : expired) {
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

    private static String cacheKey(Path path) throws Exception {
        long modified = Files.getLastModifiedTime(path).toMillis();
        return path.toAbsolutePath().normalize() + "@" + modified;
    }

    private static SoundBuffer bufferFor(Path path) {
        try {
            String key = cacheKey(path);
            synchronized (CACHE) {
                CachedBuffer cached = CACHE.get(key);
                if (cached != null) {
                    return cached.buffer;
                }
                Decoded decoded = decode(path);
                if (decoded == null) {
                    return null;
                }
                CACHE.put(key, new CachedBuffer(decoded.buffer, decoded.seconds));
                return decoded.buffer;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Decoded decode(Path path) throws Exception {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".ogg")) {
            try (InputStream input = Files.newInputStream(path); JOrbisAudioStream stream = new JOrbisAudioStream(input)) {
                ByteBuffer pcm = direct(stream.readAll());
                AudioFormat format = stream.getFormat();
                float seconds = secondsOf(pcm, format);
                return new Decoded(new SoundBuffer(pcm, format), seconds);
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
                    float seconds = secondsOf(data, pcm);
                    return new Decoded(new SoundBuffer(data, pcm), seconds);
                }
            }
        }
        return null;
    }

    private static float secondsOf(ByteBuffer pcm, AudioFormat format) {
        if (pcm == null || format == null) {
            return -1f;
        }
        int channels = Math.max(1, format.getChannels());
        int bytesPerSample = Math.max(1, format.getSampleSizeInBits() / 8);
        float rate = format.getSampleRate();
        if (rate <= 0f) {
            return -1f;
        }
        return pcm.remaining() / (channels * bytesPerSample * rate);
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

    private record Decoded(SoundBuffer buffer, float seconds) {
    }

    private record CachedBuffer(SoundBuffer buffer, float seconds) {
    }

    private record Playing(ChannelAccess.ChannelHandle handle, String fileName, long stopAtMs) {
    }
}

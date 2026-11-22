package net.felix.utilities.Other.serverpack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps the PlayLegend server resource pack as a normal resource pack, so it loads together with
 * the other packs at game start. When the server pushes the same pack again, the push is answered
 * as "loaded" without a second resource reload. A new pack version still goes through the vanilla
 * download; afterwards it replaces the cached copy for the next start.
 */
public final class ServerPackCache {

    private static final Logger LOGGER = LoggerFactory.getLogger("cclive-utilities/serverpack");

    /** Pushes that went the vanilla way: pack id to expected hash, until the server gets the final status. */
    private static final Map<UUID, String> PENDING = new ConcurrentHashMap<>();

    private static boolean initialized;

    private ServerPackCache() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientLifecycleEvents.CLIENT_STARTED.register(ServerPackCache::onClientStarted);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PENDING.clear());
    }

    private static boolean isEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().serverPackCacheEnabled;
    }

    /**
     * Called on the client thread for every server pack push.
     *
     * @return true if the cached pack is already active and the push was answered here
     */
    public static boolean handlePush(Connection connection, ClientboundResourcePackPushPacket packet) {
        if (!isEnabled() || !ServerPackRules.isPlayLegendPackUrl(packet.url())) {
            return false;
        }
        String hash = ServerPackRules.normalizeHash(packet.hash());
        if (hash == null) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        boolean active = client.getResourcePackRepository().getSelectedIds().contains(ServerPackRules.packId(hash))
            && Files.isRegularFile(resourcePackDir(client).resolve(ServerPackRules.fileName(hash)));
        if (!active) {
            PENDING.put(packet.id(), hash);
            LOGGER.info("Server pack {} not cached yet, using the vanilla download", hash);
            return false;
        }
        UUID id = packet.id();
        connection.send(new ServerboundResourcePackPacket(id, ServerboundResourcePackPacket.Action.ACCEPTED));
        connection.send(new ServerboundResourcePackPacket(id, ServerboundResourcePackPacket.Action.DOWNLOADED));
        connection.send(new ServerboundResourcePackPacket(id, ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
        LOGGER.info("Server pack {} already active, skipped the reload", hash);
        return true;
    }

    /** Called for every outgoing pack status; caches the pack once vanilla has loaded it. */
    public static void onStatusSent(ServerboundResourcePackPacket packet) {
        if (!packet.action().isTerminal()) {
            return;
        }
        String hash = PENDING.remove(packet.id());
        if (hash == null || packet.action() != ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Path source = client.gameDirectory.toPath().resolve("downloads").resolve(packet.id().toString()).resolve(hash);
        CompletableFuture
            .runAsync(() -> copyIntoResourcePacks(client, source, hash), Util.ioPool())
            .thenRunAsync(() -> select(client, hash), client)
            .exceptionally(e -> {
                LOGGER.warn("Could not cache server pack {}", hash, e);
                return null;
            });
    }

    private static void copyIntoResourcePacks(Minecraft client, Path source, String hash) {
        Path target = resourcePackDir(client).resolve(ServerPackRules.fileName(hash));
        try {
            if (Files.isRegularFile(target) && hash.equals(sha1(target))) {
                return;
            }
            if (!Files.isRegularFile(source)) {
                throw new IOException("Downloaded pack missing: " + source);
            }
            if (!hash.equals(sha1(source))) {
                throw new IOException("Hash mismatch for " + source);
            }
            Files.createDirectories(target.getParent());
            // Temp file outside resourcepacks so a half-written zip is never picked up as a pack.
            Path temp = source.resolveSibling(hash + ".cclive.tmp");
            Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            LOGGER.info("Cached server pack as {}", target.getFileName());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Swaps the selection to the new cached pack without a reload; the next reload or start uses it. */
    private static void select(Minecraft client, String hash) {
        PackRepository repository = client.getResourcePackRepository();
        repository.reload();
        String packId = ServerPackRules.packId(hash);
        if (!repository.isAvailable(packId)) {
            LOGGER.warn("Cached server pack {} not found after rescan", packId);
            return;
        }
        List<String> selection = ServerPackRules.withCachedPack(
            new ArrayList<>(repository.getSelectedIds()), packId, id -> isFixedTop(repository, id));
        repository.setSelected(selection);
        saveOptions(client, repository);
        deleteUnselectedCachedPacks(client, repository);
    }

    private static void onClientStarted(Minecraft client) {
        PackRepository repository = client.getResourcePackRepository();
        if (!isEnabled()) {
            List<String> selected = new ArrayList<>(repository.getSelectedIds());
            List<String> without = ServerPackRules.withoutCachedPacks(selected);
            if (without.size() != selected.size()) {
                // Still loaded for this session; gone after the next start.
                repository.setSelected(without);
                saveOptions(client, repository);
            }
            return;
        }
        Util.ioPool().execute(() -> deleteUnselectedCachedPacks(client, repository));
    }

    /** Mirrors {@code Options.updateResourcePacks} without its resource reload. */
    private static void saveOptions(Minecraft client, PackRepository repository) {
        client.options.resourcePacks.clear();
        client.options.incompatibleResourcePacks.clear();
        for (Pack pack : repository.getSelectedPacks()) {
            if (pack.isFixedPosition()) {
                continue;
            }
            client.options.resourcePacks.add(pack.getId());
            if (!pack.getCompatibility().isCompatible()) {
                client.options.incompatibleResourcePacks.add(pack.getId());
            }
        }
        client.options.save();
    }

    /** Old versions are only deletable once they are no longer opened (Windows locks open zips). */
    private static void deleteUnselectedCachedPacks(Minecraft client, PackRepository repository) {
        List<String> selected = List.copyOf(repository.getSelectedIds());
        Path dir = resourcePackDir(client);
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, ServerPackRules.FILE_PREFIX + "*")) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                if (!ServerPackRules.isCachedFileName(name)
                    || selected.contains(ServerPackRules.FILE_PACK_ID_PREFIX + name)) {
                    continue;
                }
                try {
                    Files.deleteIfExists(file);
                    LOGGER.info("Deleted old cached server pack {}", name);
                } catch (IOException e) {
                    LOGGER.debug("Old cached server pack {} still in use, retry next start", name, e);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not clean up cached server packs", e);
        }
    }

    private static boolean isFixedTop(PackRepository repository, String id) {
        Pack pack = repository.getPack(id);
        return pack != null && pack.isFixedPosition() && pack.getDefaultPosition() == Pack.Position.TOP;
    }

    private static Path resourcePackDir(Minecraft client) {
        return client.gameDirectory.toPath().resolve("resourcepacks");
    }

    private static String sha1(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}

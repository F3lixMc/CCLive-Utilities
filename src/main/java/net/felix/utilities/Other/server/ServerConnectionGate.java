package net.felix.utilities.Other.server;

import java.util.Locale;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Steuert, ob die Mod-Features und Daten-Uploads auf dem aktuellen Server erlaubt sind.
 *
 * <ul>
 *   <li>Erlaubt: Verbindung zu {@code playlegend.net:25565} oder {@code beta.playlegend.net:25565} (same proxy)</li>
 *   <li>Dev/stage servers {@code dev.playlegend.net}, {@code stage.playlegend.net} (port 25565): features on, data uploads off</li>
 *   <li>Dev-Umgebung: Features und Uploads erlaubt, auch in einer Singleplayer-Welt</li>
 *   <li>Bypass-Spieler (firestarter03, F3lixMc_): Features immer an;
 *       Uploads nur auf erlaubtem Server. Unbekannte/nicht erkannte IP → Features an, keine Uploads</li>
 *   <li>Andere Spieler auf falschem/unerkanntem Server: Mod-Features aus</li>
 * </ul>
 */
public final class ServerConnectionGate {

    public enum MatchState {
        /** Erkannter PlayLegend-Proxy. */
        ALLOWED,
        /** PlayLegend dev server: local features allowed, no data uploads to the backend. */
        FEATURES_ONLY,
        /** Adresse nicht ermittelbar / unklar. */
        UNKNOWN,
        /** Klar anderer Server (oder Singleplayer). */
        DISALLOWED
    }

    /** Both names reach the same PlayLegend proxy. */
    private static final Set<String> ALLOWED_HOSTS = Set.of("playlegend.net", "beta.playlegend.net");
    /** Hosts where features run but nothing is uploaded (test data must not reach the backend). */
    private static final Set<String> FEATURES_ONLY_HOSTS = Set.of("dev.playlegend.net", "stage.playlegend.net");
    private static final int ALLOWED_PORT = 25565;

    private static final Set<String> BYPASS_PLAYERS = Set.of(
        "firestarter03",
        "f3lixmc_"
    );

    private static boolean initialized;
    private static volatile MatchState cachedState = MatchState.UNKNOWN;
    private static volatile String cachedRawAddress = "";

    private ServerConnectionGate() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
            client.execute(ServerConnectionGate::refresh));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(() -> {
                cachedState = MatchState.DISALLOWED;
                cachedRawAddress = "";
            }));
        refresh();
    }

    public static void refresh() {
        cachedState = detectMatchState(Minecraft.getInstance());
    }

    public static MatchState getMatchState() {
        return cachedState;
    }

    public static String getCachedRawAddress() {
        return cachedRawAddress;
    }

    public static boolean isDevelopmentEnvironment() {
        try {
            return FabricLoader.getInstance().isDevelopmentEnvironment();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBypassPlayer(Minecraft client) {
        if (client == null || client.player == null) {
            return false;
        }
        String name = client.player.getGameProfile().name();
        if (name == null || name.isEmpty()) {
            name = client.player.getName().getString();
        }
        if (name == null) {
            return false;
        }
        return BYPASS_PLAYERS.contains(name.toLowerCase(Locale.ROOT).trim());
    }

    /** UI / Overlays / lokale Features. */
    public static boolean areFeaturesAllowed() {
        if (isDevelopmentEnvironment()) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        if (isBypassPlayer(client)) {
            return true;
        }
        return cachedState == MatchState.ALLOWED || cachedState == MatchState.FEATURES_ONLY;
    }

    /** Register, Uploads, Chat-WS Auth-pflichtige Sends, Shares. In der Dev-Umgebung auch Singleplayer. */
    public static boolean isDataUploadAllowed() {
        if (isDevelopmentEnvironment()) {
            return true;
        }
        return cachedState == MatchState.ALLOWED;
    }

    private static MatchState detectMatchState(Minecraft client) {
        cachedRawAddress = "";
        if (client == null) {
            return MatchState.UNKNOWN;
        }

        var connection = client.getConnection();
        if (connection == null || client.hasSingleplayerServer()) {
            return MatchState.DISALLOWED;
        }

        // Check the address selected by the player. DNS/SRV can route different names
        // to the same proxy, so its resolved address cannot identify the selected server.
        ServerData data = connection.getServerData();
        if (data == null) {
            data = client.getCurrentServer();
        }
        if (data == null || data.ip == null || data.ip.isBlank()) {
            return MatchState.UNKNOWN;
        }

        cachedRawAddress = data.ip;
        return classify(data.ip);
    }

    static MatchState classify(String address) {
        if (matchesAllowed(address)) {
            return MatchState.ALLOWED;
        }
        return FEATURES_ONLY_HOSTS.contains(normalizeHost(address)) ? MatchState.FEATURES_ONLY : MatchState.DISALLOWED;
    }

    static boolean matchesAllowed(String address) {
        return ALLOWED_HOSTS.contains(normalizeHost(address));
    }

    /** Lowercase host without the default port; other ports stay attached and never match. */
    private static String normalizeHost(String address) {
        String requested = address.trim().toLowerCase(Locale.ROOT);
        String portSuffix = ":" + ALLOWED_PORT;
        if (requested.endsWith(portSuffix)) {
            requested = requested.substring(0, requested.length() - portSuffix.length());
        }
        return requested;
    }
}

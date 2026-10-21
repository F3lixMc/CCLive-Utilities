package net.felix.utilities.Overall;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Locale;
import java.util.Set;

/**
 * Steuert, ob die Mod-Features und Daten-Uploads auf dem aktuellen Server erlaubt sind.
 *
 * <ul>
 *   <li>Erlaubt: Verbindung zu {@code playlegend.net:25565} (Proxy reicht; Lobby/CC egal)</li>
 *   <li>Dev-Umgebung: Features + Uploads erlaubt</li>
 *   <li>Bypass-Spieler (firestarter03, F3lixMc_): Features immer an;
 *       Uploads nur auf erlaubtem Server. Unbekannte/nicht erkannte IP → Features an, keine Uploads</li>
 *   <li>Andere Spieler auf falschem/unerkanntem Server: Mod-Features aus</li>
 * </ul>
 */
public final class ServerConnectionGate {

    public enum MatchState {
        /** Erkannter PlayLegend-Proxy. */
        ALLOWED,
        /** Adresse nicht ermittelbar / unklar. */
        UNKNOWN,
        /** Klar anderer Server (oder Singleplayer). */
        DISALLOWED
    }

    private static final String ALLOWED_HOST = "playlegend.net";
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
        return cachedState == MatchState.ALLOWED;
    }

    /** Register, Uploads, Chat-WS Auth-pflichtige Sends, Shares. */
    public static boolean isDataUploadAllowed() {
        if (isDevelopmentEnvironment()) {
            return true;
        }
        return cachedState == MatchState.ALLOWED;
    }

    private static MatchState detectMatchState(Minecraft client) {
        if (client == null) {
            cachedRawAddress = "";
            return MatchState.UNKNOWN;
        }

        // Singleplayer / kein Multiplayer-Connection
        if (client.getConnection() == null) {
            cachedRawAddress = "singleplayer/offline";
            return MatchState.DISALLOWED;
        }

        String serverListIp = null;
        try {
            ServerData data = client.getCurrentServer();
            if (data == null && client.getConnection().getServerData() != null) {
                data = client.getConnection().getServerData();
            }
            if (data != null && data.ip != null) {
                serverListIp = data.ip.trim();
            }
        } catch (Exception ignored) {
        }

        String remoteHost = null;
        int remotePort = -1;
        try {
            SocketAddress remote = client.getConnection().getConnection().getRemoteAddress();
            cachedRawAddress = remote != null ? remote.toString() : "";
            if (remote instanceof InetSocketAddress isa) {
                remotePort = isa.getPort();
                if (isa.getHostString() != null) {
                    remoteHost = isa.getHostString();
                }
            } else if (remote != null) {
                String raw = remote.toString();
                if (raw.startsWith("/")) {
                    raw = raw.substring(1);
                }
                // Formen: "host/1.2.3.4:25565" oder "1.2.3.4:25565"
                int slash = raw.indexOf('/');
                if (slash >= 0) {
                    String before = raw.substring(0, slash);
                    String after = raw.substring(slash + 1);
                    if (!before.isEmpty() && !isNumericHost(before)) {
                        remoteHost = before;
                    }
                    raw = after;
                }
                int colon = raw.lastIndexOf(':');
                if (colon > 0) {
                    if (remoteHost == null) {
                        remoteHost = raw.substring(0, colon);
                    }
                    try {
                        remotePort = Integer.parseInt(raw.substring(colon + 1));
                    } catch (NumberFormatException ignored) {
                    }
                } else if (remoteHost == null) {
                    remoteHost = raw;
                }
            }
        } catch (Exception e) {
            cachedRawAddress = "error:" + e.getMessage();
            // Serverlisten-IP kann trotzdem reichen
        }

        if (matchesAllowed(serverListIp, -1) || matchesAllowed(remoteHost, remotePort)) {
            return MatchState.ALLOWED;
        }

        // Überhaupt keine Adresse → unbekannt
        boolean hasAny = (serverListIp != null && !serverListIp.isEmpty())
            || (remoteHost != null && !remoteHost.isEmpty());
        if (!hasAny) {
            return MatchState.UNKNOWN;
        }

        // Localhost zählt in Prod als disallowed (Dev-Flag deckt IDE ab)
        String host = remoteHost != null ? remoteHost : serverListIp;
        if (host != null) {
            String h = host.toLowerCase(Locale.ROOT);
            if (h.contains("localhost") || h.startsWith("127.") || h.equals("::1")) {
                return MatchState.DISALLOWED;
            }
        }

        return MatchState.DISALLOWED;
    }

    private static boolean matchesAllowed(String hostOrAddress, int port) {
        if (hostOrAddress == null || hostOrAddress.isEmpty()) {
            return false;
        }
        String raw = hostOrAddress.trim().toLowerCase(Locale.ROOT);
        if (raw.startsWith("/")) {
            raw = raw.substring(1);
        }

        String host = raw;
        int parsedPort = port;
        int colon = raw.lastIndexOf(':');
        if (colon > 0 && raw.indexOf(']') < colon) {
            // host:port (nicht IPv6 ohne Klammern – PlayLegend ist Hostname)
            String maybePort = raw.substring(colon + 1);
            try {
                parsedPort = Integer.parseInt(maybePort);
                host = raw.substring(0, colon);
            } catch (NumberFormatException ignored) {
            }
        }

        // "playlegend.net/1.2.3.4" → host Teil
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }

        if (!host.equals(ALLOWED_HOST) && !host.endsWith("." + ALLOWED_HOST)) {
            // auch plain contains für "playlegend.net"
            if (!host.contains(ALLOWED_HOST)) {
                return false;
            }
        }

        // Port: wenn bekannt, muss 25565 sein; wenn unbekannt (Serverliste oft ohne Port) → ok
        if (parsedPort > 0 && parsedPort != ALLOWED_PORT) {
            return false;
        }
        return true;
    }

    private static boolean isNumericHost(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (!(c >= '0' && c <= '9') && c != '.' && c != ':') {
                return false;
            }
        }
        return true;
    }
}

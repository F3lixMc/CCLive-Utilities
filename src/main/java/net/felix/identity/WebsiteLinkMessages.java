package net.felix.identity;

public final class WebsiteLinkMessages {
    public static final String ALREADY_CONNECTED = "Dein Profil ist bereits verbunden. Bitte nutze den Login.";
    public static final String LINKED = "Dein Minecraft-Profil ist jetzt mit der Website verbunden.";

    private WebsiteLinkMessages() {}

    public static String mojangJoinFailure(int status) {
        if (status >= 500) {
            return "§cMojang ist vorübergehend nicht erreichbar. Bitte später erneut versuchen.";
        }
        if (status == 429) {
            return "§cZu viele Mojang-Anfragen. Bitte kurz warten.";
        }
        return "§cBestätigung fehlgeschlagen: Mojang-joinServer fehlgeschlagen (" + status + ").";
    }

    public static boolean isAlreadyConnected(int status, String code) {
        return status == 409 && "already_connected".equals(code);
    }
}

package net.felix.utilities.Community.profile;

import com.google.gson.JsonObject;

/**
 * Profilwerte für eine Chatzeile. Vorhandener Cache wird sofort zurückgegeben,
 * ein fehlender oder abgelaufener Stand lädt im Hintergrund.
 */
public final class ChatHoverProfiles {
    private ChatHoverProfiles() {
    }

    public static JsonObject current(ProfileFetchCoalescer fetches, String player, String season,
                                     ProfileFetchCoalescer.Fetcher fetcher, long now) {
        if (fetches == null || player == null || player.isBlank()) {
            return null;
        }
        JsonObject available = fetches.available(player, season);
        if (!fetches.fresh(player, season, now)) {
            fetches.request(player, season, fetcher, now);
        }
        return available;
    }
}

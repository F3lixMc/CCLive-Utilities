package net.felix.utilities.Overall.eventsound.chat;

import java.util.HashMap;
import net.felix.utilities.Overall.eventsound.config.EventSoundStore;
import net.felix.utilities.Overall.eventsound.data.EventSoundRule;
import net.felix.utilities.Overall.eventsound.sound.EventSoundPlayer;
import java.util.Map;
import net.minecraft.network.chat.Component;

/**
 * Spielt passende Event-Sounds, wenn eine System-Chat-Nachricht ankommt.
 */
public final class EventSoundChat {
    private static final long DUPLICATE_COOLDOWN_MS = 500L;
    private static final Map<String, Long> LAST_PLAYED = new HashMap<>();

    private EventSoundChat() {
    }

    public static void handleIncomingMessage(Component message, boolean overlay) {
        handle(message, overlay, false, null);
    }

    /** Spielerchat aus dem signierten oder getarnten Chat-Paket. */
    public static void handlePlayerMessage(Component message, String senderName) {
        handle(message, false, true, senderName);
    }

    private static void handle(Component message, boolean overlay, boolean playerPacket, String senderName) {
        if (overlay || message == null) {
            return;
        }
        String raw = message.getString();
        boolean player = playerPacket || EventSoundMatcher.isPlayerChat(raw);
        String normalized = EventSoundMatcher.normalize(raw);
        String customText = player ? playerText(raw, senderName) : normalized;
        if (customText.isEmpty() && normalized.isEmpty()) {
            return;
        }
        String smithing = player ? null : EventSoundMatcher.smithingState(normalized);
        // The starforged animation brings its own sounds, so its event sound stays silent while it is enabled.
        if ("Sternengeschmiedet".equals(smithing)
                && net.felix.utilities.Overall.starforged.StarforgedAnimationUtility.isAnimationEnabled()) {
            smithing = null;
        }
        String blueprint = player ? null : EventSoundMatcher.blueprintName(normalized);
        long now = System.currentTimeMillis();
        for (EventSoundRule rule : EventSoundStore.rules()) {
            if (!matches(rule, customText, player, smithing, blueprint)) {
                continue;
            }
            synchronized (LAST_PLAYED) {
                Long last = LAST_PLAYED.get(rule.id);
                if (last != null && now - last < DUPLICATE_COOLDOWN_MS) {
                    continue;
                }
                LAST_PLAYED.put(rule.id, now);
            }
            EventSoundPlayer.playFile(rule.soundFile, rule.volume, rule.maxSeconds);
        }
    }

    private static String playerText(String raw, String senderName) {
        if (EventSoundMatcher.isPlayerChat(raw)) {
            return EventSoundMatcher.messageBody(raw);
        }
        String normalized = EventSoundMatcher.normalize(raw);
        if (senderName == null || senderName.isBlank()) {
            return normalized;
        }
        String name = senderName.trim();
        if (normalized.regionMatches(true, 0, name, 0, name.length())) {
            return EventSoundMatcher.normalize(normalized.substring(name.length()).replaceFirst("^[\\s:>-]+", ""));
        }
        return normalized;
    }

    static boolean matches(EventSoundRule rule, String customText, boolean player, String smithing, String blueprint) {
        if (rule == null || rule.kind == null) {
            return false;
        }
        return switch (rule.kind) {
            case SMITHING -> smithing != null && smithing.equals(rule.target);
            case BLUEPRINT -> blueprint != null && blueprint.equals(rule.target);
            case CUSTOM -> EventSoundMatcher.matchesPrepared(rule, player, customText);
        };
    }
}

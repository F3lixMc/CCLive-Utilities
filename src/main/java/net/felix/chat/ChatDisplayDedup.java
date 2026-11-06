package net.felix.chat;

/**
 * Einmal-Anzeige für CCLive-Zeilen: lokale Echo vs. WebSocket-Kopie.
 */
public final class ChatDisplayDedup {
    static final long OWN_BROADCAST_SKIP_MS = 8000L;

    private ChatDisplayDedup() {
    }

    public static String exactKey(String player, String message, long timestamp) {
        return exactKey(null, player, message, timestamp);
    }

    public static String exactKey(Long messageId, String player, String message, long timestamp) {
        if (messageId != null && messageId > 0L) {
            return "id:" + messageId;
        }
        return String.valueOf(player) + "|" + message + "|" + timestamp;
    }

    public static boolean skipKnownId(java.util.Set<Long> seenIds, Long messageId) {
        return messageId != null && messageId > 0L && seenIds != null && seenIds.contains(messageId);
    }

    /**
     * Eigene WS-Kopie überspringen, wenn dieselbe Payload gerade erfolgreich
     * lokal gesendet wurde — die Echo-Zeile übernimmt die Anzeige.
     */
    public static boolean skipOwnBroadcast(
            boolean localEcho,
            String player,
            String selfName,
            String message,
            String lastSendPayload,
            long lastSendAtMs,
            long nowMs) {
        if (localEcho) {
            return false;
        }
        if (selfName == null || selfName.isEmpty() || player == null || !selfName.equalsIgnoreCase(player)) {
            return false;
        }
        if (message == null || lastSendPayload == null || !message.equals(lastSendPayload)) {
            return false;
        }
        long age = nowMs - lastSendAtMs;
        return age >= 0L && age < OWN_BROADCAST_SKIP_MS;
    }

    public static boolean skipExactDuplicate(String lastExactKey, String exactKey) {
        return exactKey != null && !exactKey.isEmpty() && exactKey.equals(lastExactKey);
    }
}

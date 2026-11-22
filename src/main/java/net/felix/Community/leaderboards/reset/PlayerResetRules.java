package net.felix.Community.leaderboards.reset;

import java.util.Set;

/**
 * Regeln für den Spieler-Fullreset, unabhängig von Datenbank und Verbindung.
 */
public final class PlayerResetRules {

    private PlayerResetRules() {
    }

    /** Eine schon angewendete Id löscht nicht erneut und bleibt nicht die gültige Upload-Id. */
    public static boolean appliesFreshReset(String resetId, Set<String> appliedIds) {
        return resetId != null && !resetId.isEmpty() && (appliedIds == null || !appliedIds.contains(resetId));
    }

    /** Die gültige Upload-Id bleibt die zuletzt neu angewendete Id. */
    public static String currentUploadId(String previousCurrent, String resetId, boolean fresh) {
        if (!fresh) {
            return previousCurrent;
        }
        return resetId;
    }

    /**
     * Ohne gespeicherte Phase wird nichts gelöscht.
     * Die Phase des Auftrags bleibt das Ziel, auch wenn inzwischen eine andere Season aktuell ist.
     */
    public static Integer seasonToDelete(Integer orderSeasonId) {
        if (orderSeasonId == null || orderSeasonId < 0) {
            return null;
        }
        return orderSeasonId;
    }

    /** Fehlende Id bleibt erlaubt, solange für die Phase noch kein Reset angewendet wurde. Spielzeit immer. */
    public static boolean acceptsUpload(String board, String uploadResetId, String currentResetId) {
        if ("playtime".equals(board)) {
            return true;
        }
        if (currentResetId == null || currentResetId.isEmpty()) {
            return true;
        }
        return currentResetId.equals(uploadResetId);
    }
}

package net.felix.utilities.Other.clipboard.collector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merkt einen Lagerstand nach dem Linksklick und schreibt ihn erst gut,
 * wenn die nächste bestätigte Menge 0 ist. Ein unveränderter Tooltip bleibt offen.
 */
final class ResourcePickupGate {

    static final int CONFIRM_TICKS = 60;

    private String token;
    private Map<String, Long> pending;
    private int ticksRemaining;

    boolean isPending() {
        return pending != null;
    }

    /** Ein weiterer Klick ersetzt einen offenen Vorgang nicht. */
    void arm(String screenToken, Map<String, Long> snapshot) {
        if (pending != null || screenToken == null || screenToken.isEmpty() || snapshot == null) {
            return;
        }
        Map<String, Long> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : snapshot.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null || entry.getValue() <= 0L) {
                continue;
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        if (copy.isEmpty()) {
            return;
        }
        token = screenToken;
        pending = copy;
        ticksRemaining = CONFIRM_TICKS;
    }

    void discard() {
        token = null;
        pending = null;
        ticksRemaining = 0;
    }

    /**
     * @param screenToken aktuelles Menü; ein anderes Menü verwirft den Vorgang
     * @param connected   false bei Disconnect
     * @return Mengen, die in diesem Tick genau einmal gutzuschreiben sind
     */
    Map<String, Long> observe(String screenToken, Map<String, Long> current, boolean connected) {
        return observe(screenToken, current, connected, true);
    }

    /**
     * @param tooltipPresent das Lager-Item ist noch da und sein Tooltip wurde gelesen.
     *                        Eine fehlende Mengen-Zeile gilt dann als eingesammelt.
     */
    Map<String, Long> observe(String screenToken, Map<String, Long> current, boolean connected, boolean tooltipPresent) {
        if (pending == null) {
            return Map.of();
        }
        if (!connected || screenToken == null || !screenToken.equals(token)) {
            discard();
            return Map.of();
        }
        Map<String, Long> credit = new LinkedHashMap<>();
        List<String> done = new ArrayList<>();
        for (Map.Entry<String, Long> entry : pending.entrySet()) {
            boolean gone = tooltipPresent && (current == null || !current.containsKey(entry.getKey()));
            long now = current == null ? -1L : current.getOrDefault(entry.getKey(), -1L);
            if (gone || now == 0L) {
                credit.put(entry.getKey(), entry.getValue());
                done.add(entry.getKey());
            }
        }
        for (String key : done) {
            pending.remove(key);
        }
        ticksRemaining--;
        if (pending.isEmpty() || ticksRemaining <= 0) {
            discard();
        }
        return credit;
    }
}

package net.felix.utilities.Other.clipboard.collector;

import java.util.HashMap;
import java.util.Map;

/**
 * Produktionsrate aus der aktuellen Menge im Lager-Tooltip.
 * Dieselbe Menge erneut gelesen ändert die Rate nicht. Ein Rückgang startet die Stichprobe neu.
 */
public final class StorageAmountRate {

    private final Map<String, Sample> open = new HashMap<>();
    private final Map<String, Double> rates = new HashMap<>();

    /**
     * Wenn sich die Ressourcen in diesem Bereich ändern, beginnt die Rechnung neu.
     */
    public void replaceScope(String scope, Map<String, Long> amounts, long now) {
        String prefix = scope + "\u0000";
        java.util.Set<String> next = new java.util.HashSet<>();
        if (amounts != null) {
            for (String name : amounts.keySet()) {
                if (name != null && !name.isEmpty()) {
                    next.add(prefix + name);
                }
            }
        }
        java.util.Set<String> current = new java.util.HashSet<>();
        for (String key : open.keySet()) {
            if (key.startsWith(prefix)) {
                current.add(key);
            }
        }
        for (String key : rates.keySet()) {
            if (key.startsWith(prefix)) {
                current.add(key);
            }
        }
        if (!current.equals(next)) {
            open.keySet().removeIf(key -> key.startsWith(prefix));
            rates.keySet().removeIf(key -> key.startsWith(prefix));
        }
        if (amounts == null) {
            return;
        }
        amounts.forEach((name, amount) -> observe(prefix + name, amount, now));
    }

    public double rateForResource(String resource) {
        if (resource == null || resource.isBlank()) {
            return 0.0;
        }
        String wanted = resource.trim().toLowerCase(java.util.Locale.ROOT);
        double best = 0.0;
        for (Map.Entry<String, Double> entry : rates.entrySet()) {
            int split = entry.getKey().lastIndexOf('\u0000');
            String name = split < 0 ? entry.getKey() : entry.getKey().substring(split + 1);
            if (wanted.equals(name.trim().toLowerCase(java.util.Locale.ROOT)) && entry.getValue() > best) {
                best = entry.getValue();
            }
        }
        return best;
    }

    public double observe(String key, long amount, long now) {
        if (key == null || key.isEmpty() || amount < 0L) {
            return 0.0;
        }
        Sample sample = open.get(key);
        if (sample == null) {
            open.put(key, new Sample(now, amount));
            rates.putIfAbsent(key, 0.0);
            return 0.0;
        }
        if (amount < sample.amount) {
            open.put(key, new Sample(now, amount));
            return rates.getOrDefault(key, 0.0);
        }
        long elapsed = now - sample.at;
        if (elapsed < 1000L || amount == sample.amount) {
            return rates.getOrDefault(key, 0.0);
        }
        double perMinute = (amount - sample.amount) * 60000.0 / elapsed;
        rates.put(key, perMinute);
        return perMinute;
    }

    public double rate(String key) {
        return key == null ? 0.0 : rates.getOrDefault(key, 0.0);
    }

    private record Sample(long at, long amount) {
    }
}

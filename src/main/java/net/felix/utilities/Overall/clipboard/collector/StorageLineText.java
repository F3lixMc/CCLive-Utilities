package net.felix.utilities.Overall.clipboard.collector;

import java.util.regex.Pattern;

/** Entfernt ein angehängtes (x/min) oder (x/h), damit die Mengen-Zeile weiter gelesen werden kann. */
final class StorageLineText {

    private static final Pattern RATE_SUFFIX = Pattern.compile("\\s*\\([^)]*/(?:min|h)\\)\\s*$");

    private StorageLineText() {
    }

    static String withoutRate(String line) {
        if (line == null) {
            return "";
        }
        return RATE_SUFFIX.matcher(line).replaceFirst("").trim();
    }
}

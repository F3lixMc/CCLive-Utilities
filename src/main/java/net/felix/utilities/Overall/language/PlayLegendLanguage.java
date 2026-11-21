package net.felix.utilities.Overall.language;

/**
 * Erkennt die PlayLegend-Bestätigung für einen Sprachwechsel am Klartext.
 */
public final class PlayLegendLanguage {

    public static final String ENGLISH = "[Legend] You set your language to English.";
    public static final String GERMAN = "[Legend] Du hast deine Sprache zu Deutsch geändert.";

    private PlayLegendLanguage() {
    }

    /**
     * @return {@code en}, {@code de} oder {@code null}, wenn der Text keine Sprachmeldung ist
     */
    public static String detect(String plain) {
        String text = stripFormatting(plain);
        if (ENGLISH.equals(text)) {
            return "en";
        }
        if (GERMAN.equals(text)) {
            return "de";
        }
        return null;
    }

    static String stripFormatting(String plain) {
        if (plain == null) {
            return "";
        }
        return plain.replaceAll("(?i)§[0-9a-fk-or]", "").trim();
    }
}

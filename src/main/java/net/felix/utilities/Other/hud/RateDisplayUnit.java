package net.felix.utilities.Other.hud;

/**
 * Anzeige-Einheit für Raten (intern immer pro Minute berechnet).
 */
public enum RateDisplayUnit {
    PER_MINUTE,
    PER_HOUR;

    public double fromPerMinute(double perMinute) {
        return this == PER_HOUR ? perMinute * 60.0 : perMinute;
    }

    public String suffix() {
        return this == PER_HOUR ? "/h" : "/min";
    }

    public String idlePlaceholder() {
        return "x" + suffix();
    }

    public String switcherLabel() {
        return this == PER_HOUR ? "X/h" : "X/min";
    }

    public RateDisplayUnit toggled() {
        return this == PER_HOUR ? PER_MINUTE : PER_HOUR;
    }

    /** CPM → CPH, KPM → KPH, DPM → DPH (sonst unverändert). */
    public String abbreviated(String perMinuteAbbrev) {
        if (this == PER_HOUR && perMinuteAbbrev != null && perMinuteAbbrev.endsWith("PM")) {
            return perMinuteAbbrev.substring(0, perMinuteAbbrev.length() - 2) + "PH";
        }
        return perMinuteAbbrev;
    }

    public String withBase(String base) {
        return base + suffix();
    }

    public String xpLabel() {
        return this == PER_HOUR ? "XP/h" : "XP/Min";
    }

    public String settingsSwitcherLabel() {
        return "Rate: " + switcherLabel();
    }

    public String formatAbbrevLine(String perMinuteAbbrev, String value) {
        return abbreviated(perMinuteAbbrev) + ": " + value;
    }

    public String formatAbbrevIdle(String perMinuteAbbrev) {
        return formatAbbrevLine(perMinuteAbbrev, "-");
    }

    public String formatLabeledRate(String base, String valueOrDash) {
        return withBase(base) + ": " + valueOrDash;
    }

    public String formatXpRate(String valueOrDash) {
        return xpLabel() + ": " + valueOrDash;
    }

    public static RateDisplayUnit orDefault(RateDisplayUnit unit) {
        return unit != null ? unit : PER_MINUTE;
    }
}

package net.felix.utilities.Overall.itemviewer.search;

/**
 * Filter für einen spezifischen Stat-Wert
 */
public class StatFilter {
    public String statName;  // Stat-Name (z.B. "Abbaugeschwindigkeit")
    public Double value;     // Vergleichswert (z.B. 100.0)
    public String operator;  // Vergleichsoperator: ">", "<", ">=", "<=", "="
}


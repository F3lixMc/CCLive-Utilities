package net.felix.utilities.Aincraft.npcalerts.overlay;


/**
 * Formatiert Fuellstand als Prozenttext.
 */
public final class PercentFormatter {

	private PercentFormatter() {
	}

	/**
	 * Berechnet den Prozentsatz (aktuell / maximal * 100)
	 */
	public static String formatPercent(int current, int max) {
		if (max <= 0) {
			return "?%";
		}
		double percent = ((double)current / (double)max) * 100.0;
		// Runde auf 1 Dezimalstelle
		return String.format("%.1f%%", percent);
	}

	/**
	 * Berechnet den Prozentsatz (aktuell / maximal * 100) für BigInteger
	 */
	public static String formatPercent(java.math.BigInteger current, java.math.BigInteger max) {
		if (max.compareTo(java.math.BigInteger.ZERO) <= 0) {
			return "?%";
		}
		// Konvertiere zu BigDecimal für präzise Division
		java.math.BigDecimal currentBD = new java.math.BigDecimal(current);
		java.math.BigDecimal maxBD = new java.math.BigDecimal(max);
		java.math.BigDecimal percent = currentBD.divide(maxBD, 4, java.math.RoundingMode.HALF_UP)
			.multiply(java.math.BigDecimal.valueOf(100));
		// Runde auf 1 Dezimalstelle
		return String.format("%.1f%%", percent.doubleValue());
	}
}

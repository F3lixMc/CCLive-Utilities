package net.felix.utilities.Aincraft.npcalerts.data;


/**
 * Abgeleitete Kennzahlen einer Kapazitaet: voll, leer und Fuellstand in Prozent.
 */
public final class CapacityStatus {

	private CapacityStatus() {
	}

	public static boolean isFull(CapacityData data) {
		return data.isValid() && data.max > 0 && data.current >= data.max;
	}

	public static boolean isDepleted(CapacityData data) {
		return data.isValid() && data.current <= 0;
	}

	public static double percentOf(CapacityData data) {
		if (!data.isValid() || data.max <= 0) {
			return 0;
		}
		return ((double) data.current / (double) data.max) * 100.0;
	}
}

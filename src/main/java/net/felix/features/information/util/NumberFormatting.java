package net.felix.features.information.util;


/**
 * Formatierung von Zahlen und Zeitangaben fuer Overlays.
 */
public final class NumberFormatting {

	private NumberFormatting() {
	}

	/**
	 * Formats a number with thousand separators (points)
	 */
	public static String formatNumberWithSeparator(long number) {
		if (number == 0) {
			return "0";
		}
		// Use German locale format (points as thousand separators)
		java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.GERMAN);
		return formatter.format(number);
	}

	/**
	 * Formats a double number with thousand separators (points) and one decimal place
	 */
	public static String formatDoubleWithSeparator(double number) {
		if (number == 0) {
			return "0,0";
		}
		// Use German locale format (points as thousand separators, comma as decimal separator)
		java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.GERMAN);
		formatter.setMinimumFractionDigits(1);
		formatter.setMaximumFractionDigits(1);
		return formatter.format(number);
	}

	/**
	 * Formats time in minutes to readable string
	 */
	public static String formatTime(double minutes) {
		if (minutes < 0) {
			return "Unbekannt";
		}
		
		if (minutes < 1) {
			return "< 1 Min";
		}
		
		if (minutes < 60) {
			return String.format("%.1f Min", minutes);
		}
		
		int hours = (int) (minutes / 60);
		int mins = (int) (minutes % 60);
		return String.format("%d Std %d Min", hours, mins);
	}

	/**
	 * Formats time in minutes to readable string
	 */
	public static String formatTimeMinutes(double minutes) {
		if (minutes < 0) {
			return "-";
		}
		
		if (minutes < 1) {
			return "<1m";
		} else if (minutes < 60) {
			return String.format("%.1f min", minutes);
		} else {
			int hours = (int)(minutes / 60);
			int mins = (int)(minutes % 60);
			return String.format("%d:%02d h", hours, mins);
		}
	}
}

package net.felix.utilities.Factory.essencefarm;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.gui.controllers.string.IStringController;
import java.math.BigDecimal;

/**
 * Text field for a number. YACL's number field only keeps the typed text once it loses focus,
 * and it rewrites the text with the locale format, so a value snapped back when the screen was saved.
 * This field stores each keystroke immediately and does not reformat it.
 */
final class EssenceFarmNumberController implements IStringController<String> {

	private final Option<String> option;
	private final boolean decimal;
	private final double max;

	private EssenceFarmNumberController(Option<String> option, boolean decimal, double max) {
		this.option = option;
		this.decimal = decimal;
		this.max = max;
	}

	static EssenceFarmNumberController whole(Option<String> option, int max) {
		return new EssenceFarmNumberController(option, false, max);
	}

	static EssenceFarmNumberController decimal(Option<String> option, double max) {
		return new EssenceFarmNumberController(option, true, max);
	}

	@Override
	public Option<String> option() {
		return option;
	}

	@Override
	public String getString() {
		return option.pendingValue();
	}

	@Override
	public void setFromString(String value) {
		option.requestSet(value);
	}

	@Override
	public boolean isInputValid(String value) {
		return decimal ? isDecimalText(value, max) : isWholeText(value, (int) max);
	}

	/** Whole number in {@code 0..max}, {@code 0} for a blank field, {@code -1} if it is not a number. */
	static int parseWhole(String text, int max) {
		if (text == null) {
			return -1;
		}
		String trimmed = text.trim();
		if (trimmed.isEmpty()) {
			return 0;
		}
		if (trimmed.length() > 6) {
			return -1;
		}
		for (int i = 0; i < trimmed.length(); i++) {
			if (!Character.isDigit(trimmed.charAt(i))) {
				return -1;
			}
		}
		try {
			int value = Integer.parseInt(trimmed);
			return value >= 0 && value <= max ? value : -1;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	static boolean isWholeText(String value, int max) {
		return value.isEmpty() || parseWhole(value, max) >= 0;
	}

	/** Non-negative number, {@code 0} for a blank field, {@link Double#NaN} if it is not a number. */
	static double parseDecimal(String text, double max) {
		if (text == null) {
			return Double.NaN;
		}
		String trimmed = text.trim();
		if (trimmed.isEmpty()) {
			return 0;
		}
		if (trimmed.length() > 12) {
			return Double.NaN;
		}
		String normalized = trimmed.replace(',', '.');
		if (normalized.endsWith(".")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		if (normalized.isEmpty()) {
			return 0;
		}
		int dots = 0;
		for (int i = 0; i < normalized.length(); i++) {
			char c = normalized.charAt(i);
			if (c == '.') {
				dots++;
				continue;
			}
			if (!Character.isDigit(c)) {
				return Double.NaN;
			}
		}
		if (dots > 1) {
			return Double.NaN;
		}
		try {
			double value = Double.parseDouble(normalized);
			if (Double.isNaN(value) || value < 0 || value > max) {
				return Double.NaN;
			}
			return value;
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	static boolean isDecimalText(String value, double max) {
		return value.isEmpty() || !Double.isNaN(parseDecimal(value, max));
	}

	/** Plain text without grouping separators. Unknown and zero are {@code "0"}. Fractions use a comma. */
	static String formatDropChance(double value) {
		if (!(value > 0) || Double.isNaN(value) || Double.isInfinite(value)) {
			return "0";
		}
		if (Math.abs(value - Math.rint(value)) < 1e-6 && value < Long.MAX_VALUE) {
			return Long.toString(Math.round(value));
		}
		String plain = BigDecimal.valueOf(value).toPlainString();
		int dot = plain.indexOf('.');
		if (dot >= 0) {
			int end = plain.length();
			while (end > dot + 1 && plain.charAt(end - 1) == '0') {
				end--;
			}
			if (plain.charAt(end - 1) == '.') {
				end--;
			}
			plain = plain.substring(0, end);
		}
		return plain.replace('.', ',');
	}
}

package net.felix.utilities.Factory.essencefarm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.felix.utilities.Other.glyphs.ZeichenUtility;
import net.felix.utilities.Other.glyphs.ZeichenUtility.ProfileStatsGlyph;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

/**
 * Reads "Essn Drp Chc" from the server-side stats overview (compass menu).
 *
 * <p>The server writes the stat values into the screen title with glyphs from the resource pack's
 * {@code profile/numbers.png}: one font provider per text row ({@code numbers_row_N}, different ascent).
 * {@link ZeichenUtility#getProfileStatsGlyphs()} resolves them from the active pack. The labels are part
 * of the background image, so a value is identified by its row and its horizontal position.
 * "Essn Drp Chc" is the leftmost value in row 19, 0-based (the same row holds "Wood Yield" further right).
 */
public final class EssenceFarmStatsReader {

	static final int ESSENCE_DROP_CHANCE_ROW = 19;

	/** Values further apart than this (in GUI pixels) belong to different stat columns. */
	private static final float COLUMN_GAP = 24f;

	/** Glyph order in numbers.png; only digits, separators and number suffixes matter. */
	private static final char[] GLYPH_CHARS = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'|', '%', '+', '|', '.', 'K', 'M', 'B', 'T',
	};

	/** A title glyph and its rendered x position. */
	public record Glyph(int codePoint, float x) {
	}

	private EssenceFarmStatsReader() {
	}

	/** Lays out the title like the renderer does and returns each code point with its x position. */
	public static List<Glyph> layout(Component title, Font font) {
		List<Glyph> glyphs = new ArrayList<>();
		float[] x = {0f};
		title.visit((style, text) -> {
			text.codePoints().forEach(cp -> {
				glyphs.add(new Glyph(cp, x[0]));
				x[0] += font.width(FormattedText.of(new String(Character.toChars(cp)), style));
			});
			return Optional.empty();
		}, Style.EMPTY);
		return glyphs;
	}

	/** Essence drop chance in percent, or empty if the row holds no readable value. */
	public static Optional<Double> essenceDropChance(List<Glyph> glyphs, Map<Integer, ProfileStatsGlyph> glyphMap) {
		List<String> values = rowValues(glyphs, glyphMap, ESSENCE_DROP_CHANCE_ROW);
		return values.isEmpty() ? Optional.empty() : parseValue(values.get(0));
	}

	/** Text of every value in a row, ordered left to right. */
	static List<String> rowValues(List<Glyph> glyphs, Map<Integer, ProfileStatsGlyph> glyphMap, int row) {
		List<Glyph> inRow = new ArrayList<>();
		for (Glyph glyph : glyphs) {
			ProfileStatsGlyph known = glyphMap.get(glyph.codePoint());
			if (known != null && known.row() == row) {
				inRow.add(glyph);
			}
		}
		inRow.sort(Comparator.comparingDouble(Glyph::x));
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		float lastX = Float.NEGATIVE_INFINITY;
		for (Glyph glyph : inRow) {
			if (current.length() > 0 && glyph.x() - lastX > COLUMN_GAP) {
				values.add(current.toString());
				current.setLength(0);
			}
			int column = glyphMap.get(glyph.codePoint()).index();
			current.append(column < GLYPH_CHARS.length ? GLYPH_CHARS[column] : '?');
			lastX = glyph.x();
		}
		if (current.length() > 0) {
			values.add(current.toString());
		}
		return values;
	}

	/** Parses "409%", "5.114K%" or "119|16%" (first part before the separator). */
	static Optional<Double> parseValue(String text) {
		String first = text.split("\\|", 2)[0].replace("%", "").replace("+", "").trim();
		if (first.isEmpty()) {
			return Optional.empty();
		}
		double factor = 1.0;
		char suffix = first.charAt(first.length() - 1);
		switch (suffix) {
			case 'K' -> factor = 1e3;
			case 'M' -> factor = 1e6;
			case 'B' -> factor = 1e9;
			case 'T' -> factor = 1e12;
			default -> {
			}
		}
		if (factor != 1.0) {
			first = first.substring(0, first.length() - 1);
		}
		try {
			return Optional.of(Double.parseDouble(first) * factor);
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}

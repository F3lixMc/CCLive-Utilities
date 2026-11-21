package net.felix.utilities.Factory.essencefarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EssenceFarmStatsReaderTest {

	/** Glyph column in numbers.png for a character. */
	private static int column(char c) {
		if (c >= '0' && c <= '9') {
			return c - '0';
		}
		return switch (c) {
			case '|' -> 13;
			case '%' -> 11;
			case '+' -> 12;
			case '.' -> 14;
			case 'K' -> 15;
			default -> throw new IllegalArgumentException(String.valueOf(c));
		};
	}

	/** Adds a value in a row starting at x; spaces only move x. Glyph advance is 6 px, space 4 px. */
	private static void put(List<EssenceFarmStatsReader.Glyph> out, int row, float x, String text) {
		for (char c : text.toCharArray()) {
			if (c == ' ') {
				x += 4;
				continue;
			}
			int cp = EssenceFarmStatsReader.FIRST_GLYPH + row * EssenceFarmStatsReader.GLYPHS_PER_ROW + column(c);
			out.add(new EssenceFarmStatsReader.Glyph(cp, x));
			x += 6;
		}
	}

	@Test
	void readsLeftmostValueOfEssenceRow() {
		List<EssenceFarmStatsReader.Glyph> glyphs = new ArrayList<>();
		put(glyphs, 18, 60, "278%");
		put(glyphs, 18, 190, "76%");
		// Middle column comes first in the title, the reader must still pick the left one.
		put(glyphs, 19, 190, "431%");
		put(glyphs, 19, 60, "409%");
		glyphs.add(new EssenceFarmStatsReader.Glyph('A', 0));
		assertEquals(Optional.of(409.0), EssenceFarmStatsReader.essenceDropChance(glyphs));
	}

	@Test
	void valueWithSeparatorKeepsFirstPart() {
		List<EssenceFarmStatsReader.Glyph> glyphs = new ArrayList<>();
		put(glyphs, 19, 60, "241 | 4%");
		assertEquals(List.of("241|4%"), EssenceFarmStatsReader.rowValues(glyphs, 19));
		assertEquals(Optional.of(241.0), EssenceFarmStatsReader.essenceDropChance(glyphs));
	}

	@Test
	void parsesSuffixesAndDecimals() {
		assertEquals(Optional.of(5114.0), EssenceFarmStatsReader.parseValue("5.114K%"));
		assertEquals(Optional.of(0.39), EssenceFarmStatsReader.parseValue("0.39"));
		assertEquals(Optional.of(3.0), EssenceFarmStatsReader.parseValue("+3"));
		assertTrue(EssenceFarmStatsReader.parseValue("%").isEmpty());
	}

	@Test
	void missingRowGivesNothing() {
		List<EssenceFarmStatsReader.Glyph> glyphs = new ArrayList<>();
		put(glyphs, 3, 60, "96");
		assertTrue(EssenceFarmStatsReader.essenceDropChance(glyphs).isEmpty());
	}

	@Test
	void detectsStatsTitle() {
		assertTrue(EssenceFarmStatsReader.isStatsTitle("x㲴y"));
		assertFalse(EssenceFarmStatsReader.isStatsTitle("Essenz-Tasche"));
	}

	@Test
	void recognizesWaveCompletionMessage() {
		assertTrue(EssenceFarmTracker.isWaveCompletedMessage("+824,558t XP +287,161bh Seelen"));
		assertFalse(EssenceFarmTracker.isWaveCompletedMessage("+12 Kaktus [4.000]"));
	}

	@Test
	void crystalTargetNeverBelowCurrentLevel() {
		EssenceFarmCrystals.Crystal crystal = new EssenceFarmCrystals.Crystal();
		crystal.level = 57;
		crystal.target = 67;
		assertEquals(67, crystal.effectiveTarget());
		crystal.target = 50;
		assertEquals(0, crystal.effectiveTarget());
	}
}

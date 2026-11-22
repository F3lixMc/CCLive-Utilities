package net.felix.utilities.Farmworld.collections.tracker;

/**
 * Hält nach einem Zonenwechsel die Blockzahl der vorherigen Zone zurück.
 * Die Bossbar liefert diesen Stand oft noch, nachdem das Scoreboard schon die neue Zone zeigt.
 * Dieser alte Stand markiert in der neuen Zone nichts. Der erste abweichende Stand wird nur angezeigt,
 * der nächste davon verschiedene Stand darf Baupläne als gefunden markieren.
 */
final class ZoneBossBarGate {
	private int carryOverBlocks = -1;
	private boolean seenFresh = true;

	void onZoneChanged(int blocksLeftBehind) {
		carryOverBlocks = blocksLeftBehind > 0 ? blocksLeftBehind : -1;
		seenFresh = false;
	}

	Decision decide(int blocks) {
		if (carryOverBlocks >= 0 && blocks == carryOverBlocks) {
			return Decision.IGNORE;
		}
		if (!seenFresh) {
			seenFresh = true;
			return Decision.TRACK_ONLY;
		}
		return Decision.TRACK_AND_UNLOCK;
	}

	enum Decision {
		/** Alter Zonenstand, Tracker und Fundmarkierung unverändert lassen. */
		IGNORE,
		/** Neuen Stand anzeigen, aber noch nicht als gefunden markieren. */
		TRACK_ONLY,
		/** Stand gehört zur aktuellen Zone und darf Baupläne markieren. */
		TRACK_AND_UNLOCK
	}
}

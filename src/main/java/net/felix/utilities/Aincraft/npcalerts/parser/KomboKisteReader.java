package net.felix.utilities.Aincraft.npcalerts.parser;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Farmworld.farmzone.FarmzoneScoreboard;
import net.felix.utilities.Overall.glyphs.ZeichenUtility;
import net.minecraft.client.Minecraft;

import static net.felix.utilities.Aincraft.npcalerts.data.NpcAlertState.komboKiste;

/**
 * Liest den Kombo-Kisten-Fortschritt aus Bossbar-Ziffern und Scoreboard.
 */
public final class KomboKisteReader {

	private KomboKisteReader() {
	}

	/** 1-basiert: nur diese Bossbar (typisch die dritte von unten/oben) liefert die Kombo-Ziffern */
	public static final int KOMBO_KISTE_BOSS_BAR_INDEX = 3;

	private static int komboKisteBossBarScanBest = -1;

	/**
	 * Pro Bossbar-Render: vor der Schleife aufrufen.
	 */
	public static void beginKomboKisteBossBarScan() {
		komboKisteBossBarScanBest = -1;
	}

	/**
	 * @param bossBarIndexOneBased wie in {@link net.felix.mixin.BossBarMixin} (erste Bar = 1)
	 */
	public static void observeKomboKisteBossBarTitle(String title, int bossBarIndexOneBased) {
		if (bossBarIndexOneBased != KOMBO_KISTE_BOSS_BAR_INDEX) {
			return;
		}
		int v = decodeKomboKisteBossBarDigits(title);
		komboKisteBossBarScanBest = v;
	}

	/**
	 * Liest die gesamte Bossbar: Sonderziffern aus {@code zeichen.json} ({@code npc_alerts_kombo_kiste_bossbar_digits})
	 * in Lesereihenfolge zu einer Zahl verbunden; andere Codepoints werden übersprungen.
	 *
	 * @return -1 wenn keine solche Ziffer vorkommt
	 */
	public static int decodeKomboKisteBossBarDigits(String text) {
		if (text == null || text.isEmpty()) {
			return -1;
		}
		Map<Integer, Integer> digitMap = ZeichenUtility.getNpcAlertsKomboKisteBossBarDigitCodePoints();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); ) {
			int cp = text.codePointAt(i);
			i += Character.charCount(cp);
			Integer d = digitMap.get(cp);
			if (d != null) {
				sb.append((char) ('0' + d));
			}
		}
		if (sb.isEmpty()) {
			return -1;
		}
		try {
			return Integer.parseInt(sb.toString());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static final String KOMBO_SCOREBOARD_STAR = "🌟";

	/**
	 * Sucht in den Sidebar-Zeilen nach dem Stern-Emoji (🌟) und liest die <strong>erste</strong> Zahl
	 * direkt danach (Rest der Zeile wird ignoriert).
	 */
	private static int parseKomboScoreFromSidebarLines(List<String> lines) {
		if (lines == null || lines.isEmpty()) {
			return -1;
		}
		Pattern firstNumber = Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})+|\\d+)");
		for (String line : lines) {
			if (line == null) {
				continue;
			}
			int starAt = line.indexOf(KOMBO_SCOREBOARD_STAR);
			if (starAt < 0) {
				continue;
			}
			String afterStar = line.substring(starAt + KOMBO_SCOREBOARD_STAR.length());
			Matcher m = firstNumber.matcher(afterStar);
			if (m.find()) {
				try {
					String raw = m.group(1).replace(".", "").replace(",", "");
					long v = Long.parseLong(raw);
					if (v >= 0 && v <= Integer.MAX_VALUE) {
						return (int) v;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return -1;
	}

	/**
	 * Kombo-Kiste-Daten nur in Dimensionen, deren ID „floor“ enthält (Groß-/Kleinschreibung egal), z. B. {@code mymod:floor_5}.
	 */
	public static boolean isKomboKisteReadingDimension(Minecraft client) {
		if (client == null || client.level == null) {
			return false;
		}
		String id = client.level.dimension().identifier().toString();
		return id.toLowerCase(Locale.ROOT).contains("floor");
	}

	static void refreshKomboKisteFromBossbarAndScoreboard(Minecraft client) {
		if (!isKomboKisteReadingDimension(client)) {
			clearKomboKisteProgress();
			return;
		}
		int fromBar = komboKisteBossBarScanBest;
		List<String> sidebar = FarmzoneScoreboard.readCleanSidebarLines(client);
		int fromBoard = parseKomboScoreFromSidebarLines(sidebar);
		if (fromBar >= 0 && fromBoard >= 0) {
			int sum = fromBar + fromBoard;
			if (sum < 0) {
				sum = Integer.MAX_VALUE;
			}
			int goal = CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteZielwert;
			if (goal < 1) {
				goal = 1;
			}
			komboKiste.current = sum;
			komboKiste.max = goal;
			komboKiste.currentFormatted = null;
			komboKiste.maxFormatted = null;
		} else {
			clearKomboKisteProgress();
		}
	}

	/**
	 * Bruchteil für die Anzeige (z. B. "245 / 1000"), ohne "Kombo Kiste:"-Präfix.
	 * Links: Bossbar (Bar 3, 㟮…㟷) + erste Zahl nach 🌟 im Scoreboard; rechts Zielwert aus Config.
	 */
	public static String getKomboKisteFractionDisplay() {
		if (komboKiste.isValid()) {
			return komboKiste.getDisplayString();
		}
		return "? / ?";
	}

	public static void setKomboKisteProgress(int current, int max) {
		komboKiste.current = current;
		komboKiste.max = max;
	}

	public static void clearKomboKisteProgress() {
		komboKiste.current = -1;
		komboKiste.max = -1;
		komboKiste.currentFormatted = null;
		komboKiste.maxFormatted = null;
	}
}

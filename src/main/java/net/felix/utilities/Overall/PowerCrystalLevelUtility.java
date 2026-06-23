package net.felix.utilities.Overall;

/**
 * Machtkristall-Level und Fortschritt aus Tablist-XP (benötigte XP → Level, aktuelle XP → Prozent).
 */
public final class PowerCrystalLevelUtility {

	private static final long[] XP_TABLE = {
		3450L, 8600L, 17500L, 31500L, 52150L, 80400L, 119250L, 170000L, 235400L, 317400L,
		418600L, 542500L, 691500L, 868800L, 1078650L, 1323000L, 1607400L, 1935000L, 2310000L, 2737900L,
		3221150L, 3765600L, 4376250L, 5058300L, 5817150L, 6657000L, 7584950L, 8607000L, 9729350L, 10958400L,
		12297450L, 13758100L, 15344000L, 17064000L, 18923650L, 20930400L, 23093850L, 25420000L, 27916900L, 30594900L,
		33458300L, 36520000L, 39786750L, 43267600L, 46974150L, 50911200L, 55090700L, 59522500L, 64216650L, 69183400L,
		74430550L, 79971300L, 85816500L, 91977200L, 98464650L, 105284500L, 112456950L, 119988000L, 127892600L, 136179900L,
		144862200L, 153955200L, 163468500L, 173415000L, 183811150L, 194663600L, 205992600L, 217808500L, 230125200L, 242960400L,
		256321250L, 270225800L, 284688750L, 299725000L, 315349650L, 331574100L, 348417650L, 365896000L, 384025050L, 402820900L,
		422291550L, 442465800L, 463352000L, 484971200L, 507336150L, 530464000L, 554376550L, 579087000L, 604613100L, 630977400L,
		658188900L, 686275200L, 715250250L, 745132800L, 775946650L, 807701300L, 840420900L, 874125000L, 908833350L, 944565900L,
		981337650L, 1019174000L, 1058095500L, 1098122900L, 1139277150L, 1181568600L, 1225034650L, 1269686000L, 1315549800L, 1362642400L,
		1410985800L, 1460607900L, 1511525500L, 1563761200L, 1617343650L, 1672284200L, 1728617800L, 1786362000L, 1845540400L, 1906182900L,
		1968301350L, 2031926000L, 2097081250L, 2163791700L, 2232082150L, 2301971200L, 2373490350L, 2446665000L, 2521520750L, 2598083400L,
		2676365650L, 2756413500L, 2838240000L, 2921878400L, 3007348650L, 3094677600L, 3183899250L, 3275034000L, 3368109300L, 3463159900L,
		3560199500L, 3659270400L, 3760393750L, 3863598000L, 3968919150L, 4076371400L, 4185991100L, 4297807500L, 4411850050L, 4528148400L,
		4646724750L, 4767616700L, 4890854500L, 5016468600L, 5144489650L, 5274932700L, 5407852350L, 5543264000L, 5681207000L, 5821704900L,
		5964789400L, 6110500600L, 6258862500L, 6409907400L, 6563676150L, 6720184800L, 6879483000L, 7041595500L, 7206555600L, 7374405400L,
		7545161450L, 7718866200L, 7895553750L, 8075258400L, 8258014650L, 8443848300L, 8632803050L, 8824914000L, 9020216450L, 9218745900L,
		9420519750L, 9625601200L, 9834008000L, 10045785600L, 10260961150L, 10479571200L, 10701661950L, 10927261000L, 11156405500L, 11389142400L,
		11625490100L, 11865505600L, 12109217250L, 12356663200L, 12607891650L, 12862921500L, 13121801300L, 13384570000L, 13651266750L, 13921930900L
	};

	private PowerCrystalLevelUtility() {
	}

	public static boolean isUnknownLevel(long requiredXp) {
		return requiredXp > XP_TABLE[XP_TABLE.length - 1];
	}

	/**
	 * Level aus benötigter XP (Tablist-Wert nach "/"), wie CC-Addon {@code levelFromXP}.
	 */
	public static int levelFromRequiredXp(long requiredXp) {
		if (isUnknownLevel(requiredXp)) {
			return -1;
		}
		for (int i = 0; i < XP_TABLE.length; i++) {
			if (requiredXp <= XP_TABLE[i]) {
				return i + 1;
			}
		}
		return -1;
	}

	/**
	 * Berechnet den Fortschritt aus aktueller und benötigter XP.
	 * Unter 100 %: aktuell / benötigt.
	 * Ab erreichten benötigten XP: Level-Schwellen nacheinander abziehen, Rest relativ zur nächsten Schwelle (&gt; 100 % möglich).
	 * Unbekanntes Level (&gt; 200): maximal 100 %.
	 */
	public static double calculatePercent(long currentXp, long requiredXp) {
		if (requiredXp <= 0) {
			return 0.0;
		}
		if (isUnknownLevel(requiredXp)) {
			double pct = (double) currentXp / (double) requiredXp * 100.0;
			return Math.min(100.0, pct);
		}

		int levelIndex = findLevelIndex(requiredXp);
		if (levelIndex < 0) {
			double pct = (double) currentXp / (double) requiredXp * 100.0;
			return Math.min(100.0, pct);
		}

		if (currentXp < requiredXp) {
			return (double) currentXp / (double) requiredXp * 100.0;
		}

		long remaining = currentXp;
		double completedPercent = 0.0;

		for (int i = levelIndex; i < XP_TABLE.length; i++) {
			long threshold = XP_TABLE[i];
			if (remaining >= threshold) {
				remaining -= threshold;
				completedPercent += 100.0;
			} else {
				return completedPercent + (double) remaining / (double) threshold * 100.0;
			}
		}

		return completedPercent;
	}

	public static String formatPercent(double percent) {
		if (percent < 0) {
			return null;
		}
		// Wie Server-Tablist: ganze Prozent, nie aufrunden (24,97… % → 24 %, nicht 25 %)
		long floored = (long) Math.floor(percent + 1e-9);
		return floored + "%";
	}

	private static int findLevelIndex(long requiredXp) {
		for (int i = 0; i < XP_TABLE.length; i++) {
			if (requiredXp <= XP_TABLE[i]) {
				return i;
			}
		}
		return -1;
	}
}

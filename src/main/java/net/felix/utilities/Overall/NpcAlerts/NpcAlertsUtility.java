package net.felix.utilities.Overall.NpcAlerts;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.felix.CCLiveUtilities;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.InformationenUtility;
import net.felix.utilities.Overall.KeyBindingUtility;
import net.felix.utilities.Overall.PowerCrystalLevelUtility;
import net.felix.utilities.Overall.ZeichenUtility;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility zum Auslesen von Informationen aus der Tab-Liste
 * Liest verschiedene Kapazitäten und Fortschrittsinformationen aus
 */
public class NpcAlertsUtility {
	
	private static boolean isInitialized = false;
	private static long lastTabListCheck = 0; // Cache für Tab-Liste Checks (jede 1 Sekunde)
	private static boolean showOverlays = true; // Overlay-Sichtbarkeit
	
	// Icon Identifier für Forschung, Amboss, Schmelzofen, Recycler, Seelen, Essenzen, Jäger und Machtkristalle
	private static final Identifier FORSCHUNG_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_forschung.png");
	private static final Identifier AMBOSS_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_anvil.png");
	private static final Identifier SCHMELZOFEN_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_ofen.png");
	private static final Identifier RECYCLER_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_recycler.png");
	private static final Identifier SEELEN_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_seelen.png");
	private static final Identifier ESSENZEN_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_essences.png");
	private static final Identifier JAEGER_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_bogen.png");
	private static final Identifier KOMBO_KISTE_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_kombo_kiste.png");
	private static final Identifier MACHTKRISTALL_ICON = Identifier.of(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_machtkristall.png");
	
	// Datenstrukturen für die verschiedenen Informationen
	public static class CapacityData {
		public int current = -1;
		public int max = -1;
		public String currentFormatted = null; // Formatierter String (z.B. "35,241K")
		public String maxFormatted = null; // Formatierter String (z.B. "100K")
		
		public boolean isValid() {
			return current >= 0 && max >= 0;
		}
		
		public String getDisplayString() {
			if (!isValid()) return "Nicht im Tab-Widget";
			// Verwende formatierte Strings wenn verfügbar, sonst rohe Zahlen
			if (currentFormatted != null && maxFormatted != null) {
				return currentFormatted + " / " + maxFormatted;
			}
			return current + " / " + max;
		}
		
		/**
		 * Gibt den Display-String zurück, wobei das "k" Suffix aus dem current-Wert entfernt wird
		 * (für Jäger, wo "1,513k" zu "1,513" werden soll)
		 */
		public String getDisplayStringWithoutCurrentSuffix() {
			if (!isValid()) return "Nicht im Tab-Widget";
			// Verwende formatierte Strings wenn verfügbar, sonst rohe Zahlen
			if (currentFormatted != null && maxFormatted != null) {
				// Entferne "k", "K", "m", "M" etc. vom Ende des current-Werts
				String currentWithoutSuffix = currentFormatted.replaceAll("(?i)[km]$", "");
				return currentWithoutSuffix + " / " + maxFormatted;
			}
			return current + " / " + max;
		}
	}
	
	public static class XPData {
		public java.math.BigInteger current = java.math.BigInteger.valueOf(-1);
		public java.math.BigInteger required = java.math.BigInteger.valueOf(-1);
		public String currentFormatted = null; // Formatierter String (z.B. "1,234")
		public String requiredFormatted = null; // Formatierter String (z.B. "5,000")
		public String percentFormatted = null; // Formatierter Prozentwert (z.B. "10%")
		
		public boolean isValid() {
			return current.compareTo(java.math.BigInteger.ZERO) >= 0 && required.compareTo(java.math.BigInteger.ZERO) >= 0;
		}
		
		public String getDisplayString() {
			if (!isValid()) return "? / ?";
			// Verwende formatierte Strings wenn verfügbar, sonst rohe Zahlen
			if (currentFormatted != null && requiredFormatted != null) {
				return currentFormatted + " / " + requiredFormatted;
			}
			return current.toString() + " / " + required.toString();
		}
	}
	
	// Forschung
	public static final CapacityData forschung = new CapacityData();
	
	// Kapazitäten
	public static final CapacityData ambossKapazitaet = new CapacityData();
	public static final CapacityData schmelzofenKapazitaet = new CapacityData();
	public static final CapacityData jaegerKapazitaet = new CapacityData();
	public static final CapacityData seelenKapazitaet = new CapacityData();
	public static final CapacityData essenzenKapazitaet = new CapacityData();
	public static final CapacityData komboKiste = new CapacityData();
	
	
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
	public static boolean isKomboKisteReadingDimension(MinecraftClient client) {
		if (client == null || client.world == null) {
			return false;
		}
		String id = client.world.getRegistryKey().getValue().toString();
		return id.toLowerCase(Locale.ROOT).contains("floor");
	}
	
	private static void refreshKomboKisteFromBossbarAndScoreboard(MinecraftClient client) {
		if (!isKomboKisteReadingDimension(client)) {
			clearKomboKisteProgress();
			return;
		}
		int fromBar = komboKisteBossBarScanBest;
		List<String> sidebar = InformationenUtility.readCleanSidebarLines(client);
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
	
	// Machtkristalle (3 Slots, in Reihenfolge)
	public static class MachtkristallSlot {
		public String name = null; // null wenn leer
		public XPData xpData = new XPData();
		
		public boolean isEmpty() {
			return name == null;
		}
		
		/**
		 * Prüft ob der Slot nicht gefunden wurde (keine Machtkristall-Einträge in Tab-Liste)
		 */
		public boolean isNotFound() {
			return name != null && name.isEmpty();
		}
		
		public String getDisplayText() {
			if (isEmpty()) {
				return null; // Wird von außen mit Slot-Nummer versehen
			}
			// Prüfe ob Slot nicht gefunden wurde (keine Einträge in Tab-Liste)
			if (isNotFound()) {
				return null; // Wird von außen mit Slot-Nummer versehen
			}
			// Prüfe ob Daten nicht gefunden wurden (nicht valid)
			// Wenn name gesetzt ist, aber xpData nicht valid, dann wurden die Daten nicht gefunden
			if (name != null && !name.isEmpty() && !xpData.isValid()) {
				return "MK " + name + ": Nicht im Tab-Widget";
			}
			if (name != null && !name.isEmpty()) {
				boolean showLevel = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleLevel;
				if (showLevel) {
					return "MK " + name + " lvl." + getLevelDisplay() + ":";
				}
				return "MK " + name + ":";
			}
			return null;
		}
		
		public String getLevelDisplay() {
			if (!xpData.isValid()) {
				return "?";
			}
			if (PowerCrystalLevelUtility.isUnknownLevel(xpData.required.longValue())) {
				return "?";
			}
			int level = PowerCrystalLevelUtility.levelFromRequiredXp(xpData.required.longValue());
			return level > 0 ? String.valueOf(level) : "?";
		}
		
		public double getPercentValue() {
			if (isEmpty() || isNotFound() || !xpData.isValid()) {
				return -1.0;
			}
			return PowerCrystalLevelUtility.calculatePercent(
				xpData.current.longValue(),
				xpData.required.longValue()
			);
		}
		
		public double getDisplayPercentValue() {
			double percent = getPercentValue();
			if (percent < 0) {
				return -1.0;
			}
			if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristallePercentOver100) {
				return Math.min(percent, 100.0);
			}
			return percent;
		}
		
		public String getPercentText() {
			double percent = getDisplayPercentValue();
			if (percent < 0) {
				return null;
			}
			return PowerCrystalLevelUtility.formatPercent(percent);
		}
		
		public static String stripIconPrefix(String displayText) {
			if (displayText == null) {
				return null;
			}
			String stripped = displayText.replaceFirst("^MK .+ (?=lvl\\.)", "");
			if (!stripped.equals(displayText)) {
				return stripped;
			}
			return displayText.replaceFirst("^MK [^:]+: ", "");
		}
		
		public String getDisplayTextForEmptySlot(int slotNumber) {
			return "MK " + slotNumber + ": -";
		}
		
		/**
		 * Gibt den Display-Text zurück, wenn der Slot nicht gefunden wurde
		 */
		public String getDisplayTextForNotFoundSlot(int slotNumber) {
			return "MK " + slotNumber + ": Nicht im Tab-Widget";
		}
		
		/**
		 * Prüft ob der Machtkristall gefunden wurde, aber die Daten nicht verfügbar sind
		 */
		public boolean isDataNotFound() {
			return !isEmpty() && !isNotFound() && !xpData.isValid();
		}
	}
	
	public static final MachtkristallSlot[] machtkristallSlots = new MachtkristallSlot[3];
	
	static {
		// Initialisiere alle 3 Slots
		for (int i = 0; i < 3; i++) {
			machtkristallSlots[i] = new MachtkristallSlot();
		}
	}
	
	// Legacy: Für Kompatibilität mit bestehendem Code
	@Deprecated
	public static final Map<String, XPData> machtkristalle = new HashMap<>();
	
	// Recycler Slots
	public static final CapacityData recyclerSlot1 = new CapacityData();
	public static final CapacityData recyclerSlot2 = new CapacityData();
	public static final CapacityData recyclerSlot3 = new CapacityData();
	
	public static void initialize() {
		if (isInitialized) {
			return;
		}
		
		try {
			// Client-seitige Events registrieren
			ClientTickEvents.END_CLIENT_TICK.register(NpcAlertsUtility::onClientTick);
			
			// Registriere HUD-Rendering
			HudElementRegistry.addLast(
					Identifier.of("cclive-utilities", "npc_alerts"),
					NpcAlertsUtility::onHudRender);
			
			isInitialized = true;
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	private static void onClientTick(MinecraftClient client) {
		// Check Tab key for overlay visibility
		checkTabKey();
		
		if (client == null || client.player == null || client.world == null) {
			return;
		}
		
		try {
			updateNpcAlerts(client);
			updateScreenMessages();
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	private static void checkTabKey() {
		// Check if player list key is pressed (respects custom key bindings)
		if (KeyBindingUtility.isPlayerListKeyPressed()) {
			showOverlays = false; // Hide overlays when player list key is pressed
		} else {
			showOverlays = true; // Show overlays when player list key is released
		}
	}
	
	/**
	 * Setzt ein CapacityData-Objekt auf "nicht gefunden" zurück
	 */
	private static void resetCapacityData(CapacityData data) {
		data.current = -1;
		data.max = -1;
		data.currentFormatted = null;
		data.maxFormatted = null;
	}
	
	/**
	 * Aktualisiert alle Informationen aus der Tab-Liste
	 */
	private static void updateNpcAlerts(MinecraftClient client) {
		// Nur alle 1 Sekunde prüfen, um Performance-Probleme zu vermeiden
		long currentTime = System.currentTimeMillis();
		long timeSinceLastCheck = currentTime - lastTabListCheck;
		
		if (timeSinceLastCheck < 1000) {
			return;
		}
		lastTabListCheck = currentTime;
		
		if (client == null || client.getNetworkHandler() == null) {
			return;
		}
		
		var playerList = client.getNetworkHandler().getPlayerList();
		if (playerList == null) {
			return;
		}
		
		// Konvertiere zu Liste für Index-Iteration
		java.util.List<net.minecraft.client.network.PlayerListEntry> entries = 
			new java.util.ArrayList<>(playerList);
		
		// Helper-Methode zum Entfernen von Minecraft-Formatierungscodes (§ codes)
		java.util.function.Function<String, String> removeFormatting = (text) -> {
			if (text == null) return "";
			// Entferne alle § Codes (Formatierungscodes)
			return text.replaceAll("§[0-9a-fk-or]", "").trim();
		};
		
		// Helper-Methode zum Abrufen des Textes von einem Eintrag
		java.util.function.Function<Integer, String> getEntryText = (entryIndex) -> {
			if (entryIndex < 0 || entryIndex >= entries.size()) {
				return null;
			}
			var entry = entries.get(entryIndex);
			if (entry == null) {
				return null;
			}
			
			net.minecraft.text.Text displayName = entry.getDisplayName();
			if (displayName != null) {
				return displayName.getString();
			} else if (entry.getProfile() != null) {
				return entry.getProfile().getName();
			}
			return null;
		};
		
		// Track welche Werte im Tab-Widget gefunden wurden
		boolean foundForschung = false;
		boolean foundAmboss = false;
		boolean foundSchmelzofen = false;
		boolean foundJaeger = false;
		boolean foundSeelen = false;
		boolean foundEssenzen = false;
		boolean foundRecyclerSlot1 = false;
		boolean foundRecyclerSlot2 = false;
		boolean foundRecyclerSlot3 = false;
		
		// Durchsuche alle Einträge
		for (int i = 0; i < entries.size(); i++) {
			String entryText = getEntryText.apply(i);
			if (entryText == null) {
				continue;
			}
			
			String cleanEntryText = removeFormatting.apply(entryText);
			
			// [Forschung]
			if (cleanEntryText.contains("[Forschung]")) {
				foundForschung = true;
				parseCapacityData(entries, i, getEntryText, removeFormatting, forschung, "Forschung");
			}
			
			// [Amboss Kapazität]
			if (cleanEntryText.contains("[Amboss Kapazität]")) {
				foundAmboss = true;
				parseCapacityData(entries, i, getEntryText, removeFormatting, ambossKapazitaet, "Amboss");
			}
			
			// [Schmelzofen Kapazität]
			if (cleanEntryText.contains("[Schmelzofen Kapazität]")) {
				foundSchmelzofen = true;
				parseCapacityData(entries, i, getEntryText, removeFormatting, schmelzofenKapazitaet, "Schmelzofen");
			}
			
			// [Jäger Kapazität]
			if (cleanEntryText.contains("[Jäger Kapazität]")) {
				foundJaeger = true;
				parseCapacityData(entries, i, getEntryText, removeFormatting, jaegerKapazitaet, "Jäger");
			}
			
			// [Seelen Kapazität]
			if (cleanEntryText.contains("[Seelen Kapazität]")) {
				foundSeelen = true;
				parseCapacityData(entries, i, getEntryText, removeFormatting, seelenKapazitaet, "Seelen");
			}
			
			// [Essenzen Kapazität]
			if (cleanEntryText.contains("[Essenzen Kapazität]")) {
				foundEssenzen = true;
				parseCapacityData(entries, i, getEntryText, removeFormatting, essenzenKapazitaet, "Essenzen");
			}
			
			// Machtkristall-Einträge werden separat verarbeitet (siehe unten)
			
			// [Recycler Slot 1]
			if (cleanEntryText.contains("[Recycler Slot 1]")) {
				foundRecyclerSlot1 = true;
				parseCapacityData(entries, i, getEntryText, removeFormatting, recyclerSlot1, "Recycler Slot 1");
			}
			
			// [Recycler Slot 2]
			if (cleanEntryText.contains("[Recycler Slot 2]")) {
				foundRecyclerSlot2 = true;
				parseCapacityData(entries, i, getEntryText, removeFormatting, recyclerSlot2, "Recycler Slot 2");
			}
			
			// [Recycler Slot 3]
			if (cleanEntryText.contains("[Recycler Slot 3]")) {
				foundRecyclerSlot3 = true;
				parseCapacityData(entries, i, getEntryText, removeFormatting, recyclerSlot3, "Recycler Slot 3");
			}
		}
		
		// Setze nur die Werte zurück, die nicht im Tab-Widget gefunden wurden
		if (!foundForschung) {
			resetCapacityData(forschung);
		}
		if (!foundAmboss) {
			resetCapacityData(ambossKapazitaet);
		}
		if (!foundSchmelzofen) {
			resetCapacityData(schmelzofenKapazitaet);
		}
		if (!foundJaeger) {
			resetCapacityData(jaegerKapazitaet);
		}
		if (!foundSeelen) {
			resetCapacityData(seelenKapazitaet);
		}
		if (!foundEssenzen) {
			resetCapacityData(essenzenKapazitaet);
		}
		if (!foundRecyclerSlot1) {
			resetCapacityData(recyclerSlot1);
		}
		if (!foundRecyclerSlot2) {
			resetCapacityData(recyclerSlot2);
		}
		if (!foundRecyclerSlot3) {
			resetCapacityData(recyclerSlot3);
		}
		
		// Verarbeite Machtkristalle separat (müssen in Reihenfolge geparst werden)
		parseMachtkristalle(entries, getEntryText, removeFormatting);
		
		refreshKomboKisteFromBossbarAndScoreboard(client);
	}
	
	/**
	 * Parst Machtkristalle aus der Tab-Liste
	 * Erkennt sowohl "[Machtkristall der XXX]" als auch "[Kein Machtkristall ausgewählt]"
	 * Behält die Reihenfolge bei (Slot 1, 2, 3)
	 */
	private static void parseMachtkristalle(
		java.util.List<net.minecraft.client.network.PlayerListEntry> entries,
		java.util.function.Function<Integer, String> getEntryText,
		java.util.function.Function<String, String> removeFormatting
	) {
		// Setze alle Slots zunächst auf leer
		for (int i = 0; i < 3; i++) {
			machtkristallSlots[i].name = null;
			machtkristallSlots[i].xpData = new XPData();
		}
		
		// Leere Legacy-HashMap
		machtkristalle.clear();
		
		int currentSlot = 0; // Aktueller Slot (0, 1, 2)
		boolean foundAnyMachtkristallEntry = false; // Prüfe ob überhaupt Machtkristall-Einträge gefunden wurden
		
		// Durchsuche alle Einträge nach Machtkristall-Einträgen
		for (int i = 0; i < entries.size() && currentSlot < 3; i++) {
			String entryText = getEntryText.apply(i);
			if (entryText == null) {
				continue;
			}
			
			String cleanEntryText = removeFormatting.apply(entryText);
			
			// [Machtkristall der XXX] oder [Machtkristall des XXX]
			if (cleanEntryText.contains("[Machtkristall der ") || cleanEntryText.contains("[Machtkristall des ")) {
				foundAnyMachtkristallEntry = true;
				// Extrahiere den Namen des Machtkristalls
				String kristallName = extractMachtkristallName(cleanEntryText);
				if (kristallName != null && !kristallName.isEmpty()) {
					// Setze Namen und parse XP-Daten
					machtkristallSlots[currentSlot].name = kristallName;
					parseXPData(entries, i, getEntryText, removeFormatting, 
						machtkristallSlots[currentSlot].xpData, "Machtkristall");
					
					// Legacy: Für Kompatibilität
					machtkristalle.put(kristallName, machtkristallSlots[currentSlot].xpData);
					
					currentSlot++;
				}
			}
			// [Kein Machtkristall ausgewählt]
			else if (cleanEntryText.contains("[Kein Machtkristall ausgewählt]") || 
			         cleanEntryText.contains("[Kein Machtkristall")) {
				foundAnyMachtkristallEntry = true;
				// Slot bleibt leer (name = null)
				currentSlot++;
			}
		}
		
		// Wenn keine Machtkristall-Einträge gefunden wurden, markiere alle Slots als "nicht gefunden"
		if (!foundAnyMachtkristallEntry) {
			for (int i = 0; i < 3; i++) {
				// Setze einen speziellen Marker-Namen, um zu signalisieren, dass der Slot nicht gefunden wurde
				machtkristallSlots[i].name = ""; // Leerer String signalisiert "nicht gefunden"
				// Stelle sicher, dass xpData auch als ungültig markiert ist
				machtkristallSlots[i].xpData = new XPData(); // current = -1, required = -1
			}
		} else {
			// Wenn Machtkristall-Einträge gefunden wurden, markiere die restlichen Slots als "nicht gefunden"
			// (Slots, die nicht in der Tab-Liste stehen)
			for (int i = currentSlot; i < 3; i++) {
				// Diese Slots wurden nicht in der Tab-Liste gefunden
				machtkristallSlots[i].name = ""; // Leerer String signalisiert "nicht gefunden"
				machtkristallSlots[i].xpData = new XPData(); // current = -1, required = -1
			}
		}
	}
	
	/**
	 * Extrahiert den Namen des Machtkristalls aus dem Text
	 * Format: "[Machtkristall der XXX]" oder "[Machtkristall des XXX]"
	 */
	private static String extractMachtkristallName(String text) {
		try {
			// Versuche zuerst "der"
			Pattern pattern = Pattern.compile("\\[Machtkristall der (.+)\\]", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(text);
			if (matcher.find()) {
				return matcher.group(1).trim();
			}
			
			// Versuche dann "des"
			pattern = Pattern.compile("\\[Machtkristall des (.+)\\]", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(text);
			if (matcher.find()) {
				return matcher.group(1).trim();
			}
		} catch (Exception e) {
			// Silent error handling
		}
		return null;
	}
	
	/**
	 * Parst Kapazitätsdaten (Aktuell / Maximal) aus der Tab-Liste
	 * Sucht nach dem Header und dann nach der Datenzeile darunter
	 */
	private static void parseCapacityData(
		java.util.List<net.minecraft.client.network.PlayerListEntry> entries,
		int headerIndex,
		java.util.function.Function<Integer, String> getEntryText,
		java.util.function.Function<String, String> removeFormatting,
		CapacityData data,
		String debugName
	) {
		// Suche nach der Datenzeile nach dem Header
		// Es kann sein, dass ein Spielername dazwischen steht, also suchen wir in den nächsten 10 Einträgen
		for (int j = headerIndex + 1; j < Math.min(headerIndex + 11, entries.size()); j++) {
			String dataText = getEntryText.apply(j);
			if (dataText == null) {
				continue;
			}
			
			String cleanDataText = removeFormatting.apply(dataText);
			if (cleanDataText == null || cleanDataText.trim().isEmpty()) {
				continue;
			}
			
			// Überspringe Spielernamen (wenn der Text wie ein Spielername aussieht)
			// Spielernamen enthalten normalerweise keine Zahlen oder "/"
			if (!cleanDataText.contains("/") && !cleanDataText.matches(".*\\d+.*")) {
				// Möglicherweise ein Spielername, aber wir prüfen trotzdem weiter
				// da manche Einträge auch ohne "/" sein könnten
			}
			
			// Prüfe, ob dies eine Kapazitätszeile ist (enthält "/")
			if (cleanDataText.contains("/")) {
				// Versuche, die formatierten Strings zu extrahieren
				// Format: "AKTUELL / MAXIMAL" oder "AKTUELL/MAXIMAL"
				// Beispiel: "35,241K / 100K" oder "123 / 500"
				try {
					// Teile durch "/" auf
					String[] parts = cleanDataText.split("/", 2);
					if (parts.length == 2) {
						String currentPart = parts[0].trim();
						String maxPart = parts[1].trim();
						
						// Speichere formatierte Strings
						data.currentFormatted = currentPart;
						data.maxFormatted = maxPart;
						
						// Extrahiere auch rohe Zahlen für isValid() Prüfung
						// Für Jäger: "k" ist nur ein Anzeige-Suffix, kein Multiplikator
						// "1,513k" sollte als 1513 interpretiert werden, nicht 1513000
						String currentNumbersOnly;
						String maxNumbersOnly;
						
						if ("Jäger".equals(debugName)) {
							// Bei Jäger: "k" ist nur ein Anzeige-Suffix, kein Multiplikator
							// "1,513k" sollte als 1513 interpretiert werden (Komma ist Tausendertrennzeichen)
							// Entferne alles außer Zahlen und Kommas, dann entferne Komma (Tausendertrennzeichen)
							String currentWithComma = currentPart.replaceAll("[^0-9.,]", "");
							// Entferne Komma (ist Tausendertrennzeichen, nicht Dezimaltrennzeichen)
							currentNumbersOnly = currentWithComma.replaceAll("[.,]", "");
							// Für max-Wert bei Jäger: normal konvertieren (sollte kein "k" haben)
							String maxNumbersOnlyRaw = maxPart.replaceAll("[^0-9.,]", "").replaceAll("[.,]", "");
							maxNumbersOnly = convertSuffixToNumber(maxPart, maxNumbersOnlyRaw);
						} else {
							// Für alle anderen: normale Konvertierung
							// Entferne Wörter wie "materials" vor der Konvertierung, damit sie nicht als Suffixe interpretiert werden
							String currentPartClean = currentPart.replaceAll("(?i)\\s*(materials?|materialien?)\\s*", "").trim();
							String maxPartClean = maxPart.replaceAll("(?i)\\s*(materials?|materialien?)\\s*", "").trim();
							
							currentNumbersOnly = currentPartClean.replaceAll("[^0-9.,]", "").replaceAll("[.,]", "");
							maxNumbersOnly = maxPartClean.replaceAll("[^0-9.,]", "").replaceAll("[.,]", "");
							currentNumbersOnly = convertSuffixToNumber(currentPartClean, currentNumbersOnly);
							maxNumbersOnly = convertSuffixToNumber(maxPartClean, maxNumbersOnly);
						}
						
						if (!currentNumbersOnly.isEmpty() && !maxNumbersOnly.isEmpty()) {
							try {
								// Verwende long für große Zahlen (z.B. 2,700,000,000 würde Integer.MAX_VALUE überschreiten)
								long currentLong = Long.parseLong(currentNumbersOnly);
								long maxLong = Long.parseLong(maxNumbersOnly);
								
								// Prüfe ob die Werte in int-Bereich passen
								if (currentLong > Integer.MAX_VALUE || maxLong > Integer.MAX_VALUE) {
									// Verwende -1 wenn zu groß (wird als ungültig angezeigt)
									data.current = -1;
									data.max = -1;
									return;
								}
								
								data.current = (int) currentLong;
								data.max = (int) maxLong;
								return; // Erfolgreich geparst
							} catch (NumberFormatException e) {
								// Wenn Parsing fehlschlägt, setze trotzdem die formatierten Strings
								// und verwende -1 für isValid() Prüfung (wird dann als ungültig angezeigt)
								data.current = -1;
								data.max = -1;
								return;
							}
						}
					}
				} catch (Exception e) {
					// Weiter suchen
					continue;
				}
			}
		}
		
		// Nicht gefunden - setze auf ungültig
		data.current = -1;
		data.max = -1;
		data.currentFormatted = null;
		data.maxFormatted = null;
	}
	
	/**
	 * Konvertiert Suffixe wie "K" (Tausend), "M" (Million) zu Zahlen
	 * Gibt einen String zurück, der für BigInteger-Parsing verwendet werden kann
	 */
	private static String convertSuffixToNumber(String original, String numbersOnly) {
		if (original == null || original.isEmpty()) {
			return numbersOnly;
		}
		
		String upper = original.toUpperCase();
		if (upper.contains("K")) {
			// Tausend
			try {
				java.math.BigInteger value = new java.math.BigInteger(numbersOnly);
				return value.multiply(java.math.BigInteger.valueOf(1000)).toString();
			} catch (NumberFormatException e) {
				return numbersOnly;
			}
		} else if (upper.contains("M")) {
			// Million
			try {
				java.math.BigInteger value = new java.math.BigInteger(numbersOnly);
				return value.multiply(java.math.BigInteger.valueOf(1000000)).toString();
			} catch (NumberFormatException e) {
				return numbersOnly;
			}
		}
		
		return numbersOnly;
	}
	
	/**
	 * Liest den vollständigen Tablist-Text inkl. Custom-Font-Glyphen.
	 */
	private static String getTablistEntryText(net.minecraft.client.network.PlayerListEntry entry) {
		if (entry == null) {
			return null;
		}
		net.minecraft.text.Text displayName = entry.getDisplayName();
		if (displayName != null) {
			StringBuilder sb = new StringBuilder();
			displayName.visit((style, asString) -> {
				sb.append(asString);
				return java.util.Optional.empty();
			}, net.minecraft.text.Style.EMPTY);
			return sb.toString();
		}
		if (entry.getProfile() != null) {
			return entry.getProfile().getName();
		}
		return null;
	}
	
	/**
	 * Parst XP-Daten (Aktuell / Benötigt) aus der Tab-Liste
	 * Sucht nach dem Header und dann nach der Datenzeile darunter
	 */
	private static void parseXPData(
		java.util.List<net.minecraft.client.network.PlayerListEntry> entries,
		int headerIndex,
		java.util.function.Function<Integer, String> getEntryText,
		java.util.function.Function<String, String> removeFormatting,
		XPData data,
		String debugName
	) {
		// Suche nach der Datenzeile nach dem Header
		// Es kann sein, dass ein Spielername dazwischen steht, also suchen wir in den nächsten 10 Einträgen
		for (int j = headerIndex + 1; j < Math.min(headerIndex + 11, entries.size()); j++) {
			String dataText = getTablistEntryText(entries.get(j));
			if (dataText == null) {
				dataText = getEntryText.apply(j);
			}
			if (dataText == null) {
				continue;
			}
			
			dataText = ZeichenUtility.decodeTabWidgetText(dataText);
			String cleanDataText = removeFormatting.apply(dataText);
			if (cleanDataText == null || cleanDataText.trim().isEmpty()) {
				continue;
			}
			
			// Überspringe Spielernamen (wenn der Text wie ein Spielername aussieht)
			// Spielernamen enthalten normalerweise keine Zahlen oder "/"
			if (!cleanDataText.contains("/") && !cleanDataText.matches(".*\\d+.*")) {
				// Möglicherweise ein Spielername, aber wir prüfen trotzdem weiter
			}
			
			// Prüfe, ob dies eine XP-Zeile ist (enthält "/")
			if (cleanDataText.contains("/")) {
				// Format: "AKTUELL / BENÖTIGT XP [PROZENT%]" — Prozent wird für MK nicht verwendet
				try {
					String dataWithoutPercent = cleanDataText
						.replaceAll("\\[[^\\]]*%\\]", "")
						.replaceAll("\\s*XP\\s*", " ")
						.trim();
					
					// Teile durch "/" auf
					String[] parts = dataWithoutPercent.split("/", 2);
					if (parts.length == 2) {
						String currentPart = parts[0].trim();
						String requiredPart = parts[1].trim();
						
						// Speichere formatierte Strings
						data.currentFormatted = currentPart;
						data.requiredFormatted = requiredPart;
						data.percentFormatted = null;
						
						// Extrahiere auch rohe Zahlen für isValid() Prüfung
						// Entferne alles außer Zahlen, Punkten, Kommas für Parsing
						String currentNumbersOnly = currentPart.replaceAll("[^0-9.,]", "").replaceAll("[.,]", "");
						String requiredNumbersOnly = requiredPart.replaceAll("[^0-9.,]", "").replaceAll("[.,]", "");
						
						// Versuche, Buchstaben wie "K", "M" zu konvertieren
						currentNumbersOnly = convertSuffixToNumber(currentPart, currentNumbersOnly);
						requiredNumbersOnly = convertSuffixToNumber(requiredPart, requiredNumbersOnly);
						
						if (!currentNumbersOnly.isEmpty() && !requiredNumbersOnly.isEmpty()) {
							try {
								data.current = new java.math.BigInteger(currentNumbersOnly);
								data.required = new java.math.BigInteger(requiredNumbersOnly);
								return; // Erfolgreich geparst
							} catch (NumberFormatException e) {
								// Wenn Parsing fehlschlägt, setze trotzdem die formatierten Strings
								// und verwende -1 für isValid() Prüfung (wird dann als ungültig angezeigt)
								data.current = java.math.BigInteger.valueOf(-1);
								data.required = java.math.BigInteger.valueOf(-1);
								return;
							}
						}
					}
				} catch (Exception e) {
					// Weiter suchen
					continue;
				}
			}
		}
		
		// Nicht gefunden - setze auf ungültig
		data.current = java.math.BigInteger.valueOf(-1);
		data.required = java.math.BigInteger.valueOf(-1);
		data.currentFormatted = null;
		data.requiredFormatted = null;
		data.percentFormatted = null;
	}
	
	/**
	 * Gibt alle Machtkristall-Namen zurück
	 */
	public static java.util.Set<String> getMachtkristallNames() {
		return machtkristalle.keySet();
	}
	
	/**
	 * Gibt XP-Daten für einen bestimmten Machtkristall zurück
	 */
	public static XPData getMachtkristallXP(String kristallName) {
		return machtkristalle.getOrDefault(kristallName, new XPData());
	}
	
	/**
	 * Prüft, ob der Spieler in der "general_lobby" Dimension ist
	 */
	private static boolean isInGeneralLobby() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.world == null) {
			return false;
		}
		
		String dimensionPath = client.world.getRegistryKey().getValue().getPath();
		return dimensionPath.equals("general_lobby");
	}
	
	/**
	 * HUD Render callback für das NPC Alerts Overlay
	 */
	private static final List<String> activeScreenMessages = new ArrayList<>();
	private static final int SCREEN_MESSAGE_COLOR = 0xFFFF5555;
	
	private static boolean isCapacityFull(CapacityData data) {
		return data.isValid() && data.max > 0 && data.current >= data.max;
	}
	
	private static boolean isCapacityEmpty(CapacityData data) {
		return data.isValid() && data.current <= 0;
	}
	
	private static double getCapacityPercent(CapacityData data) {
		if (!data.isValid() || data.max <= 0) {
			return 0;
		}
		return ((double) data.current / (double) data.max) * 100.0;
	}
	
	private static boolean shouldShowHighCapacityScreenMessage(CapacityData data, double warnPercent) {
		if (!data.isValid()) {
			return false;
		}
		double currentPercent = getCapacityPercent(data);
		return isCapacityFull(data) || (warnPercent >= 0 && currentPercent >= warnPercent);
	}
	
	private static boolean shouldShowLowCapacityScreenMessage(CapacityData data, double warnPercent) {
		if (!data.isValid()) {
			return false;
		}
		double currentPercent = getCapacityPercent(data);
		return isCapacityEmpty(data) || (warnPercent >= 0 && currentPercent <= warnPercent);
	}

	private static boolean shouldShowForschungWarning(int warnValue) {
		if (!forschung.isValid() || warnValue < 0) {
			return false;
		}
		return forschung.current <= warnValue;
	}
	
	private static boolean shouldShowMachtkristallScreenMessage(MachtkristallSlot slot, double warnPercent) {
		return getMachtkristallShowWarning(slot, warnPercent);
	}
	
	public static boolean shouldShowMachtkristallWarning(MachtkristallSlot slot) {
		return getMachtkristallShowWarning(
			slot,
			CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleWarnPercent
		);
	}
	
	private static boolean getMachtkristallShowWarning(MachtkristallSlot slot, double warnPercent) {
		if (slot.isEmpty() || slot.isNotFound() || !slot.xpData.isValid()) {
			return false;
		}
		double currentPercent = slot.getDisplayPercentValue();
		return currentPercent >= 0 && warnPercent >= 0 && currentPercent >= warnPercent;
	}
	
	private static boolean isScreenMessageEnabled(String configKey) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return switch (configKey) {
			case "forschung" -> config.npcAlertsForschungScreenMessage;
			case "amboss" -> config.npcAlertsAmbossScreenMessage;
			case "schmelzofen" -> config.npcAlertsSchmelzofenScreenMessage;
			case "jaeger" -> config.npcAlertsJaegerScreenMessage;
			case "seelen" -> config.npcAlertsSeelenScreenMessage;
			case "essenzen" -> config.npcAlertsEssenzenScreenMessage;
			case "komboKiste" -> config.npcAlertsKomboKisteScreenMessage;
			case "machtkristalle" -> config.npcAlertsMachtkristalleScreenMessage;
			case "recycler", "recyclerSlot1", "recyclerSlot2", "recyclerSlot3" -> config.npcAlertsRecyclerScreenMessage;
			default -> false;
		};
	}
	
	private static boolean isScreenMessageWhenOverlayDisabledEnabled(String configKey) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return switch (configKey) {
			case "forschung" -> config.npcAlertsForschungScreenMessageWhenOverlayDisabled;
			case "amboss" -> config.npcAlertsAmbossScreenMessageWhenOverlayDisabled;
			case "schmelzofen" -> config.npcAlertsSchmelzofenScreenMessageWhenOverlayDisabled;
			case "jaeger" -> config.npcAlertsJaegerScreenMessageWhenOverlayDisabled;
			case "seelen" -> config.npcAlertsSeelenScreenMessageWhenOverlayDisabled;
			case "essenzen" -> config.npcAlertsEssenzenScreenMessageWhenOverlayDisabled;
			case "komboKiste" -> config.npcAlertsKomboKisteScreenMessageWhenOverlayDisabled;
			case "machtkristalle" -> config.npcAlertsMachtkristalleScreenMessageWhenOverlayDisabled;
			case "recycler", "recyclerSlot1", "recyclerSlot2", "recyclerSlot3" -> config.npcAlertsRecyclerScreenMessageWhenOverlayDisabled;
			default -> false;
		};
	}
	
	private static boolean shouldMonitorAlertForScreenMessage(String configKey) {
		if (isNpcAlertCollectorEnabled(configKey)) {
			return true;
		}
		return isScreenMessageWhenOverlayDisabledEnabled(configKey);
	}
	
	private static boolean shouldMonitorMachtkristallSlotForScreenMessage(int slotIndex) {
		if (isNpcAlertMachtkristallSlotEnabled(slotIndex)) {
			return true;
		}
		return isScreenMessageWhenOverlayDisabledEnabled("machtkristalle");
	}
	
	private static boolean shouldMonitorRecyclerSlotForScreenMessage(int slotIndex) {
		if (isNpcAlertRecyclerSlotEnabled(slotIndex)) {
			return true;
		}
		return isScreenMessageWhenOverlayDisabledEnabled("recycler");
	}
	
	private static boolean shouldShowScreenMessageForAlert(String configKey) {
		if (isNpcAlertCollectorEnabled(configKey)) {
			return isScreenMessageEnabled(configKey);
		}
		return isScreenMessageWhenOverlayDisabledEnabled(configKey);
	}
	
	private static void addScreenMessageIfEnabled(String configKey, boolean condition, String message) {
		if (condition && shouldShowScreenMessageForAlert(configKey)) {
			activeScreenMessages.add(message);
		}
	}
	
	private static void updateScreenMessages() {
		activeScreenMessages.clear();
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (!config.npcAlertsUtilityEnabled) {
			return;
		}
		
		if (shouldMonitorAlertForScreenMessage("forschung")) {
			addScreenMessageIfEnabled("forschung",
				shouldShowForschungWarning(config.npcAlertsForschungWarnValue),
				"Forschungen sind leer");
		}
		if (shouldMonitorAlertForScreenMessage("amboss")) {
			addScreenMessageIfEnabled("amboss",
				shouldShowHighCapacityScreenMessage(ambossKapazitaet, config.npcAlertsAmbossWarnPercent),
				"Amboss ist voll");
		}
		if (shouldMonitorAlertForScreenMessage("schmelzofen")) {
			addScreenMessageIfEnabled("schmelzofen",
				shouldShowHighCapacityScreenMessage(schmelzofenKapazitaet, config.npcAlertsSchmelzofenWarnPercent),
				"Schmelzofen ist voll");
		}
		if (shouldMonitorAlertForScreenMessage("jaeger")) {
			addScreenMessageIfEnabled("jaeger",
				shouldShowHighCapacityScreenMessage(jaegerKapazitaet, config.npcAlertsJaegerWarnPercent),
				"Jäger ist voll");
		}
		if (shouldMonitorAlertForScreenMessage("seelen")) {
			addScreenMessageIfEnabled("seelen",
				shouldShowHighCapacityScreenMessage(seelenKapazitaet, config.npcAlertsSeelenWarnPercent),
				"Seelen sind voll");
		}
		if (shouldMonitorAlertForScreenMessage("essenzen")) {
			addScreenMessageIfEnabled("essenzen",
				shouldShowHighCapacityScreenMessage(essenzenKapazitaet, config.npcAlertsEssenzenWarnPercent),
				"Essenzen sind voll");
		}
		if (shouldMonitorAlertForScreenMessage("komboKiste")) {
			addScreenMessageIfEnabled("komboKiste",
				komboKiste.isValid() && komboKiste.current >= komboKiste.max,
				"Kombo Kiste ist voll");
		}
		if (shouldMonitorAlertForScreenMessage("machtkristalle")) {
			for (int i = 0; i < 3; i++) {
				if (shouldMonitorMachtkristallSlotForScreenMessage(i)
					&& shouldShowMachtkristallScreenMessage(machtkristallSlots[i], config.npcAlertsMachtkristalleWarnPercent)) {
					addScreenMessageIfEnabled("machtkristalle", true, "Machtkristall ist voll");
					break;
				}
			}
		}
		if (shouldMonitorRecyclerSlotForScreenMessage(0)) {
			addScreenMessageIfEnabled("recycler",
				shouldShowLowCapacityScreenMessage(recyclerSlot1, config.npcAlertsRecyclerWarnPercent),
				"Recycler Slot 1 ist leer");
		}
		if (shouldMonitorRecyclerSlotForScreenMessage(1)) {
			addScreenMessageIfEnabled("recycler",
				shouldShowLowCapacityScreenMessage(recyclerSlot2, config.npcAlertsRecyclerWarnPercent),
				"Recycler Slot 2 ist leer");
		}
		if (shouldMonitorRecyclerSlotForScreenMessage(2)) {
			addScreenMessageIfEnabled("recycler",
				shouldShowLowCapacityScreenMessage(recyclerSlot3, config.npcAlertsRecyclerWarnPercent),
				"Recycler Slot 3 ist leer");
		}
	}
	
	private static void renderScreenMessages(DrawContext context, MinecraftClient client) {
		if (activeScreenMessages.isEmpty()) {
			return;
		}
		
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		int baseY = screenHeight / 4;
		int lineSpacing = 24;
		int startY = baseY - ((activeScreenMessages.size() - 1) * lineSpacing) / 2;
		
		Matrix3x2fStack matrices = context.getMatrices();
		for (int i = 0; i < activeScreenMessages.size(); i++) {
			String message = activeScreenMessages.get(i);
			matrices.pushMatrix();
			matrices.translate(screenWidth / 2f, startY + i * lineSpacing);
			matrices.scale(2.0f, 2.0f);
			int textWidth = client.textRenderer.getWidth(message);
			context.drawText(client.textRenderer, message, -textWidth / 2, 0, SCREEN_MESSAGE_COLOR, true);
			matrices.popMatrix();
		}
	}
	
	/**
	 * HUD Render callback für das NPC Alerts Overlay
	 */
	private static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.player == null || client.world == null) {
			return;
		}
		
		// Hide overlay if F1 menu (debug screen) is open
		if (client.options.hudHidden) {
			return;
		}
		
		// Hide overlay if in general_lobby dimension
		if (isInGeneralLobby()) {
			return;
		}
		
		// Hide overlay if Tab list is open (like other overlays)
		if (KeyBindingUtility.isPlayerListKeyPressed()) {
			return;
		}
		
		if (!showOverlays) {
			return;
		}
		
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		if (config.npcAlertsUtilityEnabled) {
			renderScreenMessages(context, client);
		}
		if (areNpcAlertsOverlaysActive()) {
			renderNpcAlertsDisplay(context, client);
		}
	}
	
	/** NPC Alerts Utility aktiv und Overlays nicht global ausgeblendet („Overlays Ein/Aus“). */
	private static boolean areNpcAlertsOverlaysActive() {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return config.npcAlertsUtilityEnabled && config.npcAlertsOverlaysVisible;
	}
	
	/** Einzelner Collector aktiv (Overlay-Eintrag in den Einstellungen). */
	private static boolean isNpcAlertCollectorEnabled(String configKey) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return switch (configKey) {
			case "forschung" -> config.showNpcAlertsForschung;
			case "amboss" -> config.showNpcAlertsAmboss;
			case "schmelzofen" -> config.showNpcAlertsSchmelzofen;
			case "jaeger" -> config.showNpcAlertsJaeger;
			case "seelen" -> config.showNpcAlertsSeelen;
			case "essenzen" -> config.showNpcAlertsEssenzen;
			case "komboKiste" -> config.showNpcAlertsKomboKiste;
			case "machtkristalle" -> config.showNpcAlertsMachtkristalle;
			case "recycler", "recyclerSlot1", "recyclerSlot2", "recyclerSlot3" ->
				config.showNpcAlertsRecyclerSlot1 || config.showNpcAlertsRecyclerSlot2 || config.showNpcAlertsRecyclerSlot3;
			default -> true;
		};
	}
	
	private static boolean isNpcAlertRecyclerSlotEnabled(int slotIndex) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return switch (slotIndex) {
			case 0 -> config.showNpcAlertsRecyclerSlot1;
			case 1 -> config.showNpcAlertsRecyclerSlot2;
			case 2 -> config.showNpcAlertsRecyclerSlot3;
			default -> false;
		};
	}
	
	private static boolean isNpcAlertMachtkristallSlotEnabled(int slotIndex) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return switch (slotIndex) {
			case 0 -> config.showNpcAlertsMachtkristalleSlot1;
			case 1 -> config.showNpcAlertsMachtkristalleSlot2;
			case 2 -> config.showNpcAlertsMachtkristalleSlot3;
			default -> false;
		};
	}
	
	/**
	 * Gibt die Anzahl der Machtkristalle zurück (für DraggableOverlay)
	 * Gibt immer 3 zurück (maximal 3 Slots)
	 */
	public static int getMachtkristallCount() {
		return 3;
	}
	
	/**
	 * Berechnet den Prozentsatz (aktuell / maximal * 100)
	 */
	public static String calculatePercent(int current, int max) {
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
	public static String calculatePercent(java.math.BigInteger current, java.math.BigInteger max) {
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
	
	/**
	 * Gibt die Anzahl der Zeilen im Haupt-Overlay zurück (für DraggableOverlay)
	 */
	public static int getMainOverlayLineCount() {
		int count = 0;
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaeger && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsKomboKiste && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay) {
			count += 3; // Immer 3 Slots (auch wenn leer)
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay) {
			count++;
		}
		return Math.max(1, count); // Mindestens 1 Zeile
	}
	
	/**
	 * Innere Klasse für Zeilen mit Prozent-Informationen
	 */
	public static class LineWithPercent {
		public String text;
		public String percentText;
		public boolean showPercent;
		public boolean showWarning; // Zeigt an, ob Warnung angezeigt werden soll
		public String configKey; // Für Warn-Prozentwert-Prüfung
		public boolean showIcon; // Zeigt an, ob Icon statt Text angezeigt werden soll
		
		public LineWithPercent(String text, String percentText, boolean showPercent, boolean showWarning, String configKey) {
			this(text, percentText, showPercent, showWarning, configKey, false);
		}
		
		public LineWithPercent(String text, String percentText, boolean showPercent, boolean showWarning, String configKey, boolean showIcon) {
			this.text = text;
			this.percentText = percentText;
			this.showPercent = showPercent;
			this.showWarning = showWarning;
			this.configKey = configKey;
			this.showIcon = showIcon;
		}
	}
	
	/**
	 * Gibt die Zeilen für das Haupt-Overlay zurück (für F6-Editor mit "?/?" als Werte)
	 */
	public static List<LineWithPercent> getMainOverlayLinesForEditMode() {
		List<LineWithPercent> lines = new ArrayList<>();
		
		// Forschung
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschungPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowIcon;
			String displayText = showIcon ? "? / ?" : "Forschung: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "forschung", showIcon));
		}
		
		// Amboss Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowIcon;
			String displayText = showIcon ? "? / ?" : "Amboss: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "amboss", showIcon));
		}
		
		// Schmelzofen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofenPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowIcon;
			String displayText = showIcon ? "? / ?" : "Schmelzofen: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "schmelzofen", showIcon));
		}
		
		// Jäger Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaeger && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaegerPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowIcon;
			String displayText = showIcon ? "? / ?" : "Jäger: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "jaeger", showIcon));
		}
		
		// Seelen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelenPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowIcon;
			String displayText = showIcon ? "? / ?" : "Seelen: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "seelen", showIcon));
		}
		
		// Essenzen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzenPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowIcon;
			String displayText = showIcon ? "? / ?" : "Essenzen: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "essenzen", showIcon));
		}
		
		// Kombo Kiste
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsKomboKiste && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteSeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowIcon;
			String displayText = showIcon ? "? / ?" : "Kombo Kiste: ? / ?";
			lines.add(new LineWithPercent(displayText, null, false, false, "komboKiste", showIcon));
		}
		
		// Machtkristalle
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowIcon;
			// Zeige alle 3 Slots mit Beispielwerten im Edit-Modus
			// Im Edit-Modus zeigen wir immer "0.0%" als Beispiel, um zu zeigen wie es aussehen würde
			for (int i = 0; i < 3; i++) {
				// Prüfe ob dieser Slot aktiviert ist
				boolean slotEnabled;
				switch (i) {
					case 0:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1;
						break;
					case 1:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2;
						break;
					case 2:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3;
						break;
					default:
						slotEnabled = true;
						break;
				}
				
				// Überspringe diesen Slot, wenn er deaktiviert ist
				if (!slotEnabled) {
					continue;
				}
				
				String displayText = showIcon ? "? / ?" : "MK " + (i + 1) + ": ? / ?";
				// Prüfe ob Prozente in echten Daten verfügbar wären (für Anzeige im Edit-Modus)
				MachtkristallSlot slot = machtkristallSlots[i];
				String percentText = slot.getPercentText();
				// Wenn keine Prozente verfügbar sind, zeige trotzdem "0.0%" als Beispiel im Edit-Modus
				if (percentText == null) {
					percentText = "0.0%";
				}
				lines.add(new LineWithPercent(displayText, percentText, true, false, "machtkristalle", showIcon));
			}
		}
		
		// Recycler Slots
		boolean showRecyclerPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1ShowIcon;
			String displayText = showIcon ? "? / ?" : "Recycler Slot 1: ? / ?";
			String percent = null;
			if (showRecyclerPercent && recyclerSlot1.isValid()) {
				percent = calculatePercent(recyclerSlot1.current, recyclerSlot1.max);
			}
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, false, "recyclerSlot1", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2ShowIcon;
			String displayText = showIcon ? "? / ?" : "Recycler Slot 2: ? / ?";
			String percent = null;
			if (showRecyclerPercent && recyclerSlot2.isValid()) {
				percent = calculatePercent(recyclerSlot2.current, recyclerSlot2.max);
			}
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, false, "recyclerSlot2", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3ShowIcon;
			String displayText = showIcon ? "? / ?" : "Recycler Slot 3: ? / ?";
			String percent = null;
			if (showRecyclerPercent && recyclerSlot3.isValid()) {
				percent = calculatePercent(recyclerSlot3.current, recyclerSlot3.max);
			}
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, false, "recyclerSlot3", showIcon));
		}
		
		return lines;
	}
	
	/**
	 * Gibt die tatsächlichen Zeilen für das Haupt-Overlay zurück (mit echten Werten)
	 * Wird für die Breitenberechnung im F6-Editor verwendet
	 */
	public static List<LineWithPercent> getMainOverlayLines() {
		List<LineWithPercent> lines = new ArrayList<>();
		
		// Forschung
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschungPercent;
			if (showPercent) {
				if (forschung.isValid()) {
					// Forschung zählt runter: wenn current näher an max ist, ist der Prozent höher
					// Prozent = (current / max) * 100
					percent = calculatePercent(forschung.current, forschung.max);
				} else {
					// Prüfe ob "Nicht im Tab-Widget" angezeigt wird
					String displayString = forschung.getDisplayString();
					if (displayString != null && displayString.contains("Nicht im Tab-Widget")) {
						// Keine Prozentanzeige wenn "Nicht im Tab-Widget"
						showPercent = false;
						percent = null;
					} else {
						// Zeige "?%" wenn Daten noch nicht verfügbar sind
						percent = "?%";
					}
				}
			}
			// Warnung: wenn verbleibender Wert <= Schwelle (0-23)
			int warnValue = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungWarnValue;
			boolean showWarning = shouldShowForschungWarning(warnValue);
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowIcon;
			String displayText = showIcon ? forschung.getDisplayString() : "Forschung: " + forschung.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "forschung", showIcon));
		}
		
		// Amboss Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent;
			if (showPercent && ambossKapazitaet.isValid()) {
				percent = calculatePercent(ambossKapazitaet.current, ambossKapazitaet.max);
			}
			double currentPercent = ambossKapazitaet.isValid() ? 
				((double)ambossKapazitaet.current / (double)ambossKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossWarnPercent;
			boolean showWarning = ambossKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowIcon;
			String displayText = showIcon ? ambossKapazitaet.getDisplayString() : "Amboss: " + ambossKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "amboss", showIcon));
		}
		
		// Schmelzofen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofenPercent;
			if (showPercent && schmelzofenKapazitaet.isValid()) {
				percent = calculatePercent(schmelzofenKapazitaet.current, schmelzofenKapazitaet.max);
			}
			double currentPercent = schmelzofenKapazitaet.isValid() ? 
				((double)schmelzofenKapazitaet.current / (double)schmelzofenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenWarnPercent;
			boolean showWarning = schmelzofenKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowIcon;
			String displayText = showIcon ? schmelzofenKapazitaet.getDisplayString() : "Schmelzofen: " + schmelzofenKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "schmelzofen", showIcon));
		}
		
		// Jäger Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaeger && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaegerPercent;
			if (showPercent && jaegerKapazitaet.isValid()) {
				percent = calculatePercent(jaegerKapazitaet.current, jaegerKapazitaet.max);
			}
			double currentPercent = jaegerKapazitaet.isValid() ? 
				((double)jaegerKapazitaet.current / (double)jaegerKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerWarnPercent;
			boolean showWarning = jaegerKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowIcon;
			String displayText = showIcon ? jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix() : "Jäger: " + jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "jaeger", showIcon));
		}
		
		// Seelen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelenPercent;
			if (showPercent && seelenKapazitaet.isValid()) {
				percent = calculatePercent(seelenKapazitaet.current, seelenKapazitaet.max);
			}
			double currentPercent = seelenKapazitaet.isValid() ? 
				((double)seelenKapazitaet.current / (double)seelenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenWarnPercent;
			boolean showWarning = seelenKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowIcon;
			String displayText = showIcon ? seelenKapazitaet.getDisplayString() : "Seelen: " + seelenKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "seelen", showIcon));
		}
		
		// Essenzen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzenPercent;
			if (showPercent && essenzenKapazitaet.isValid()) {
				percent = calculatePercent(essenzenKapazitaet.current, essenzenKapazitaet.max);
			}
			double currentPercent = essenzenKapazitaet.isValid() ? 
				((double)essenzenKapazitaet.current / (double)essenzenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenWarnPercent;
			boolean showWarning = essenzenKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowIcon;
			String displayText = showIcon ? essenzenKapazitaet.getDisplayString() : "Essenzen: " + essenzenKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "essenzen", showIcon));
		}
		
		// Kombo Kiste (Werte später z. B. aus Bossbar/Leaderboard)
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsKomboKiste && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteSeparateOverlay) {
			boolean showWarning = komboKiste.isValid() && komboKiste.current >= komboKiste.max;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowIcon;
			String fraction = getKomboKisteFractionDisplay();
			String displayText = showIcon ? fraction : "Kombo Kiste: " + fraction;
			lines.add(new LineWithPercent(displayText, null, false, showWarning, "komboKiste", showIcon));
		}
		
		// Machtkristalle
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowIcon;
			for (int i = 0; i < 3; i++) {
				// Prüfe ob dieser Slot aktiviert ist
				boolean slotEnabled;
				switch (i) {
					case 0:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1;
						break;
					case 1:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2;
						break;
					case 2:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3;
						break;
					default:
						slotEnabled = true;
						break;
				}
				
				// Überspringe diesen Slot, wenn er deaktiviert ist
				if (!slotEnabled) {
					continue;
				}
				
				MachtkristallSlot slot = machtkristallSlots[i];
				// Prüfe zuerst ob Slot nicht gefunden wurde (keine Machtkristall-Einträge in Tab-Liste)
				// Dies muss VOR isEmpty() geprüft werden, da isNotFound() true ist wenn name = ""
				if (slot.isNotFound()) {
					// Slot nicht gefunden (keine Machtkristall-Einträge in Tab-Liste)
						String displayText = showIcon ? "Nicht im Tab-Widget" : "MK " + (i + 1) + ": Nicht im Tab-Widget";
					lines.add(new LineWithPercent(displayText, null, false, false, "machtkristalle", showIcon));
				} else if (slot.isEmpty()) {
					// Slot ist leer (durch "[Kein Machtkristall ausgewählt]" markiert)
					String displayText = showIcon ? "-" : "MK " + (i + 1) + ": -";
					lines.add(new LineWithPercent(displayText, null, false, false, "machtkristalle", showIcon));
				} else {
					// Machtkristall gefunden - prüfe ob Daten verfügbar sind
					String displayText = slot.getDisplayText();
					String percentText = slot.getPercentText();
					// displayText sollte nie null sein, wenn isEmpty() und isNotFound() beide false sind
					// Aber zur Sicherheit prüfen wir trotzdem
					if (displayText == null) {
						// Fallback: sollte nicht passieren, aber falls doch, zeige Slot-Nummer
						displayText = showIcon ? "?" : "MK " + (i + 1) + ": ?";
					} else if (showIcon) {
						displayText = MachtkristallSlot.stripIconPrefix(displayText);
					}
					double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleWarnPercent;
					boolean showWarning = getMachtkristallShowWarning(slot, warnPercent);
					lines.add(new LineWithPercent(displayText, percentText, percentText != null, showWarning, "machtkristalle", showIcon));
				}
			}
		}
		
		// Recycler Slots
		boolean showRecyclerPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay) {
			String percent = null;
			if (showRecyclerPercent && recyclerSlot1.isValid()) {
				percent = calculatePercent(recyclerSlot1.current, recyclerSlot1.max);
			}
			double currentPercent = recyclerSlot1.isValid() ? 
				((double)recyclerSlot1.current / (double)recyclerSlot1.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot1.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1ShowIcon;
			String displayText = showIcon ? recyclerSlot1.getDisplayString() : "Recycler Slot 1: " + recyclerSlot1.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, showWarning, "recyclerSlot1", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay) {
			String percent = null;
			if (showRecyclerPercent && recyclerSlot2.isValid()) {
				percent = calculatePercent(recyclerSlot2.current, recyclerSlot2.max);
			}
			double currentPercent = recyclerSlot2.isValid() ? 
				((double)recyclerSlot2.current / (double)recyclerSlot2.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot2.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2ShowIcon;
			String displayText = showIcon ? recyclerSlot2.getDisplayString() : "Recycler Slot 2: " + recyclerSlot2.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, showWarning, "recyclerSlot2", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay) {
			String percent = null;
			if (showRecyclerPercent && recyclerSlot3.isValid()) {
				percent = calculatePercent(recyclerSlot3.current, recyclerSlot3.max);
			}
			double currentPercent = recyclerSlot3.isValid() ? 
				((double)recyclerSlot3.current / (double)recyclerSlot3.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot3.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3ShowIcon;
			String displayText = showIcon ? recyclerSlot3.getDisplayString() : "Recycler Slot 3: " + recyclerSlot3.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, showWarning, "recyclerSlot3", showIcon));
		}
		
		return lines;
	}
	
	/**
	 * Rendert das NPC Alerts Overlay links auf dem Bildschirm
	 */
	private static void renderNpcAlertsDisplay(DrawContext context, MinecraftClient client) {
		if (client.getWindow() == null) {
			return;
		}
		
		// Prüfe ob NPC Alerts Utility aktiviert ist
		if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsUtilityEnabled) {
			return;
		}
		if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsOverlaysVisible) {
			return;
		}
		
		// Hilfsklasse für Zeilen mit optionalen Prozenten und Warnung
		
		// Sammle alle anzuzeigenden Zeilen
		List<LineWithPercent> lines = new ArrayList<>();
		
		// Forschung
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschungPercent;
			if (showPercent) {
				if (forschung.isValid()) {
					// Forschung zählt runter: wenn current näher an max ist, ist der Prozent höher
					// Prozent = (current / max) * 100
					percent = calculatePercent(forschung.current, forschung.max);
				} else {
					// Prüfe ob "Nicht im Tab-Widget" angezeigt wird
					String displayString = forschung.getDisplayString();
					if (displayString != null && displayString.contains("Nicht im Tab-Widget")) {
						// Keine Prozentanzeige wenn "Nicht im Tab-Widget"
						showPercent = false;
						percent = null;
					} else {
						// Zeige "?%" wenn Daten noch nicht verfügbar sind
						percent = "?%";
					}
				}
			}
			// Warnung: wenn verbleibender Wert <= Schwelle (0-23)
			int warnValue = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungWarnValue;
			boolean showWarning = shouldShowForschungWarning(warnValue);
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowIcon;
			String displayText = showIcon ? forschung.getDisplayString() : "Forschung: " + forschung.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "forschung", showIcon));
		}
		
		// Amboss Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent;
			if (showPercent && ambossKapazitaet.isValid()) {
				percent = calculatePercent(ambossKapazitaet.current, ambossKapazitaet.max);
			}
			double currentPercent = ambossKapazitaet.isValid() ? 
				((double)ambossKapazitaet.current / (double)ambossKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossWarnPercent;
			boolean showWarning = ambossKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowIcon;
			String displayText = showIcon ? ambossKapazitaet.getDisplayString() : "Amboss: " + ambossKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "amboss", showIcon));
		}
		
		// Schmelzofen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofenPercent;
			if (showPercent && schmelzofenKapazitaet.isValid()) {
				percent = calculatePercent(schmelzofenKapazitaet.current, schmelzofenKapazitaet.max);
			}
			double currentPercent = schmelzofenKapazitaet.isValid() ? 
				((double)schmelzofenKapazitaet.current / (double)schmelzofenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenWarnPercent;
			boolean showWarning = schmelzofenKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowIcon;
			String displayText = showIcon ? schmelzofenKapazitaet.getDisplayString() : "Schmelzofen: " + schmelzofenKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "schmelzofen", showIcon));
		}
		
		// Jäger Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaeger && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaegerPercent;
			if (showPercent && jaegerKapazitaet.isValid()) {
				percent = calculatePercent(jaegerKapazitaet.current, jaegerKapazitaet.max);
			}
			double currentPercent = jaegerKapazitaet.isValid() ? 
				((double)jaegerKapazitaet.current / (double)jaegerKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerWarnPercent;
			boolean showWarning = jaegerKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowIcon;
			String displayText = showIcon ? jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix() : "Jäger: " + jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "jaeger", showIcon));
		}
		
		// Seelen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelenPercent;
			if (showPercent && seelenKapazitaet.isValid()) {
				percent = calculatePercent(seelenKapazitaet.current, seelenKapazitaet.max);
			}
			double currentPercent = seelenKapazitaet.isValid() ? 
				((double)seelenKapazitaet.current / (double)seelenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenWarnPercent;
			boolean showWarning = seelenKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowIcon;
			String displayText = showIcon ? seelenKapazitaet.getDisplayString() : "Seelen: " + seelenKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "seelen", showIcon));
		}
		
		// Essenzen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzenPercent;
			if (showPercent && essenzenKapazitaet.isValid()) {
				percent = calculatePercent(essenzenKapazitaet.current, essenzenKapazitaet.max);
			}
			double currentPercent = essenzenKapazitaet.isValid() ? 
				((double)essenzenKapazitaet.current / (double)essenzenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenWarnPercent;
			boolean showWarning = essenzenKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowIcon;
			String displayText = showIcon ? essenzenKapazitaet.getDisplayString() : "Essenzen: " + essenzenKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "essenzen", showIcon));
		}
		
		// Kombo Kiste (Werte später z. B. aus Bossbar/Leaderboard)
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsKomboKiste && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteSeparateOverlay) {
			boolean showWarning = komboKiste.isValid() && komboKiste.current >= komboKiste.max;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowIcon;
			String fraction = getKomboKisteFractionDisplay();
			String displayText = showIcon ? fraction : "Kombo Kiste: " + fraction;
			lines.add(new LineWithPercent(displayText, null, false, showWarning, "komboKiste", showIcon));
		}
		
		// Machtkristalle
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowIcon;
			for (int i = 0; i < 3; i++) {
				// Prüfe ob dieser Slot aktiviert ist
				boolean slotEnabled;
				switch (i) {
					case 0:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1;
						break;
					case 1:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2;
						break;
					case 2:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3;
						break;
					default:
						slotEnabled = true;
						break;
				}
				
				// Überspringe diesen Slot, wenn er deaktiviert ist
				if (!slotEnabled) {
					continue;
				}
				
				MachtkristallSlot slot = machtkristallSlots[i];
				// Prüfe zuerst ob Slot nicht gefunden wurde (keine Machtkristall-Einträge in Tab-Liste)
				// Dies muss VOR isEmpty() geprüft werden, da isNotFound() true ist wenn name = ""
				if (slot.isNotFound()) {
					// Slot nicht gefunden (keine Machtkristall-Einträge in Tab-Liste)
					String displayText = showIcon ? "Nicht im Tab-Widget" : "MK " + (i + 1) + ": Nicht im Tab-Widget";
					lines.add(new LineWithPercent(displayText, null, false, false, "machtkristalle", showIcon));
				} else if (slot.isEmpty()) {
					// Slot ist leer (durch "[Kein Machtkristall ausgewählt]" markiert)
					String displayText = showIcon ? "-" : "MK " + (i + 1) + ": -";
					lines.add(new LineWithPercent(displayText, null, false, false, "machtkristalle", showIcon));
				} else {
					// Machtkristall gefunden - prüfe ob Daten verfügbar sind
					String displayText = slot.getDisplayText();
					String percentText = slot.getPercentText();
					// displayText sollte nie null sein, wenn isEmpty() und isNotFound() beide false sind
					// Aber zur Sicherheit prüfen wir trotzdem
					if (displayText == null) {
						// Fallback: sollte nicht passieren, aber falls doch, zeige Slot-Nummer
						displayText = showIcon ? "?" : "MK " + (i + 1) + ": ?";
					} else if (showIcon) {
						displayText = MachtkristallSlot.stripIconPrefix(displayText);
					}
					double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleWarnPercent;
					boolean showWarning = getMachtkristallShowWarning(slot, warnPercent);
					lines.add(new LineWithPercent(displayText, percentText, percentText != null, showWarning, "machtkristalle", showIcon));
				}
			}
		}
		
		// Recycler Slots
		boolean showRecyclerPercent2 = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			if (showRecyclerPercent2 && recyclerSlot1.isValid()) {
				percent = calculatePercent(recyclerSlot1.current, recyclerSlot1.max);
			}
			double currentPercent = recyclerSlot1.isValid() ? 
				((double)recyclerSlot1.current / (double)recyclerSlot1.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot1.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1ShowIcon;
			String displayText = showIcon ? recyclerSlot1.getDisplayString() : "Recycler Slot 1: " + recyclerSlot1.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent2 && percent != null, showWarning, "recyclerSlot1", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			if (showRecyclerPercent2 && recyclerSlot2.isValid()) {
				percent = calculatePercent(recyclerSlot2.current, recyclerSlot2.max);
			}
			double currentPercent = recyclerSlot2.isValid() ? 
				((double)recyclerSlot2.current / (double)recyclerSlot2.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot2.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2ShowIcon;
			String displayText = showIcon ? recyclerSlot2.getDisplayString() : "Recycler Slot 2: " + recyclerSlot2.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent2 && percent != null, showWarning, "recyclerSlot2", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			if (showRecyclerPercent2 && recyclerSlot3.isValid()) {
				percent = calculatePercent(recyclerSlot3.current, recyclerSlot3.max);
			}
			double currentPercent = recyclerSlot3.isValid() ? 
				((double)recyclerSlot3.current / (double)recyclerSlot3.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot3.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3ShowIcon;
			String displayText = showIcon ? recyclerSlot3.getDisplayString() : "Recycler Slot 3: " + recyclerSlot3.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent2 && percent != null, showWarning, "recyclerSlot3", showIcon));
		}
		
		// Das Overlay zeigt jetzt immer die eingeschalteten Informationen an, auch wenn noch keine Werte gefunden wurden
		// Wenn keine Informationen für das Haupt-Overlay eingeschaltet sind, wird nichts angezeigt
		
		// Berechne die maximale Breite des Textes (inklusive Prozente und Warnung direkt danach)
		int maxWidth = 0;
		for (LineWithPercent line : lines) {
			int width = 0;
			// Wenn Icon aktiviert ist, füge Icon-Breite hinzu
			if (line.showIcon && (line.configKey != null && ("forschung".equals(line.configKey) || "amboss".equals(line.configKey) || 
			                                                   "schmelzofen".equals(line.configKey) || "seelen".equals(line.configKey) || 
			                                                   "essenzen".equals(line.configKey) || "jaeger".equals(line.configKey) || "komboKiste".equals(line.configKey) || 
			                                                   "machtkristalle".equals(line.configKey) ||
			                                                   "recyclerSlot1".equals(line.configKey) || "recyclerSlot2".equals(line.configKey) || 
			                                                   "recyclerSlot3".equals(line.configKey)))) {
				int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
				width += iconSize + 2; // Icon + Abstand
				width += client.textRenderer.getWidth(": "); // Doppelpunkt nach Icon
			}
			width += client.textRenderer.getWidth(line.text);
			if (line.showPercent && line.percentText != null) {
				width += client.textRenderer.getWidth(" " + line.percentText); // Abstand + Prozente
			}
			if (width > maxWidth) {
				maxWidth = width;
			}
		}
		
		// Padding
		final int PADDING = 5;
		final int LINE_HEIGHT = client.textRenderer.fontHeight + 2; // Verwende tatsächliche Schrift-Höhe
		
		// Berechne die tatsächliche Zeilenhöhe unter Berücksichtigung von Icons
		int actualLineHeight = LINE_HEIGHT;
		int iconLineCount = 0; // Zähle Zeilen mit Icons für zusätzlichen Abstand
		for (LineWithPercent line : lines) {
			if (line.showIcon && line.configKey != null) {
				int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
				// Die tatsächliche Höhe ist das Maximum aus Icon-Höhe und Text-Höhe
				actualLineHeight = Math.max(actualLineHeight, iconSize);
				iconLineCount++;
			}
		}
		
		// Wenn keine Zeilen vorhanden sind, rendere nichts (auch keinen Hintergrund)
		// Rendere nur separate Overlays
		if (lines.isEmpty()) {
			renderSeparateOverlays(context, client);
			return;
		}
		
		// Berechne unskalierte Overlay-Dimensionen komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
		// Berücksichtige zusätzlichen Abstand für Zeilen mit Icons (2 Pixel pro Icon-Zeile)
		int unscaledWidth = maxWidth + (PADDING * 2);
		int unscaledHeight = (lines.size() * actualLineHeight) + (iconLineCount * 2) + (PADDING * 2);
		
		// Get scale
		float scale = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayScale;
		if (scale <= 0) scale = 1.0f;
		
		// Berechne skalierte Dimensionen
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);
		
		// Position aus Config: baseX ist die linke Kante (wie beim Mining-Overlay)
		int baseX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayX;
		int yPosition = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayY;
		
		// Determine if overlay is on left or right side of screen
		int screenWidth = client.getWindow().getScaledWidth();
		boolean isOnLeftSide = baseX < screenWidth / 2;
		
		// Calculate X position based on side (same logic as Mining overlays)
		int posX;
		if (isOnLeftSide) {
			// On left side: keep left edge fixed, expand to the right
			posX = baseX;
		} else {
			// On right side: keep right edge fixed, expand to the left
			// Right edge is: baseX (since baseX is on the right side, it represents the right edge)
			// Keep this right edge fixed, so left edge moves left when width increases
			posX = baseX - overlayWidth;
		}
		
		// Ensure overlay stays within screen bounds
		posX = Math.max(0, Math.min(posX, screenWidth - overlayWidth));
		int posY = yPosition;
		
		// Zeichne semi-transparenten Hintergrund (wenn aktiviert) - skaliert
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMainOverlayShowBackground) {
			context.fill(posX, posY, posX + overlayWidth, posY + overlayHeight, 0x80000000);
		}
		
		// Render content with scale using matrix transformation
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(posX, posY);
		matrices.scale(scale, scale);
		
		// Zeichne alle Zeilen - vertikal zentriert (nur wenn Zeilen vorhanden sind)
		if (!lines.isEmpty()) {
			// Berechne die tatsächliche Text-Höhe (inkl. zusätzlicher Abstände für Icon-Zeilen)
			int totalTextHeight = 0;
			for (LineWithPercent line : lines) {
				totalTextHeight += actualLineHeight;
				if (line.showIcon && line.configKey != null) {
					totalTextHeight += 2; // Zusätzlicher Abstand für Icon-Zeilen
				}
			}
			// Zentriere: Overlay-Mitte, dann verschiebe nach oben um die Hälfte der Text-Höhe
			// Die erste Zeile beginnt bei overlayCenterY - totalTextHeight / 2
			// Da die Text-Baseline unten ist, müssen wir die erste Zeile etwas nach oben verschieben
			// Verwende unskalierte Höhe für Zentrierung (Koordinaten sind nach Matrix-Transformation relativ)
			int overlayCenterY = unscaledHeight / 2;
			int currentY = overlayCenterY - totalTextHeight / 2;
			int warningColor = 0xFFFF0000; // Rot für Warnungen
			
			for (LineWithPercent line : lines) {
			// Hole konfigurierte Farben für diese Zeile
			int textColor = getTextColorForConfigKey(line.configKey);
			int percentColor = getPercentColorForConfigKey(line.configKey);
			// Prüfe, ob die Zeile nicht leer ist
			if (line.text == null || line.text.trim().isEmpty()) {
				currentY += actualLineHeight;
				continue;
			}
			
			// Zeichne Haupttext
			Text textComponent = Text.literal(line.text);
			
			try {
				int currentX = PADDING; // Relativ zu (xPosition, yPosition) nach Matrix-Transformation
				
				// Bestimme Textfarbe: rot und blinkend wenn Warnung aktiv ist
				int currentTextColor = textColor;
				if (line.showWarning) {
					// Blink-Animation: alle 300ms wechseln
					boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
					if (isVisible) {
						currentTextColor = warningColor;
					} else {
						currentTextColor = textColor;
					}
				}
				
				// Zeichne Icon statt Text, wenn aktiviert (für Forschung, Amboss, Schmelzofen, Seelen, Essenzen und Recycler)
				if (line.showIcon && (line.configKey != null)) {
					int iconSize = (int)(client.textRenderer.fontHeight * 1.5); // Icon-Größe = 1.5x Text-Höhe
					// Zentriere Icon vertikal: Overlay-Mitte minus die Hälfte der Icon-Höhe
					int lineCenterY = currentY + actualLineHeight / 2;
					int iconY = lineCenterY - iconSize / 2;
					Identifier iconToUse = null;
					String fallbackText = null;
					
					if ("forschung".equals(line.configKey)) {
						iconToUse = FORSCHUNG_ICON;
						fallbackText = "Forschung: ";
					} else if ("amboss".equals(line.configKey)) {
						iconToUse = AMBOSS_ICON;
						fallbackText = "Amboss: ";
					} else if ("schmelzofen".equals(line.configKey)) {
						iconToUse = SCHMELZOFEN_ICON;
						fallbackText = "Schmelzofen: ";
					} else if ("seelen".equals(line.configKey)) {
						iconToUse = SEELEN_ICON;
						fallbackText = "Seelen: ";
					} else if ("essenzen".equals(line.configKey)) {
						iconToUse = ESSENZEN_ICON;
						fallbackText = "Essenzen: ";
					} else if ("jaeger".equals(line.configKey)) {
						iconToUse = JAEGER_ICON;
						fallbackText = "Jäger: ";
					} else if ("komboKiste".equals(line.configKey)) {
						iconToUse = KOMBO_KISTE_ICON;
						fallbackText = "Kombo Kiste: ";
					} else if ("recyclerSlot1".equals(line.configKey)) {
						iconToUse = RECYCLER_ICON;
						fallbackText = "Recycler Slot 1: ";
					} else if ("recyclerSlot2".equals(line.configKey)) {
						iconToUse = RECYCLER_ICON;
						fallbackText = "Recycler Slot 2: ";
					} else if ("recyclerSlot3".equals(line.configKey)) {
						iconToUse = RECYCLER_ICON;
						fallbackText = "Recycler Slot 3: ";
					} else if ("machtkristalle".equals(line.configKey)) {
						iconToUse = MACHTKRISTALL_ICON;
						fallbackText = "MK: ";
					}
					
					if (iconToUse != null) {
						boolean iconDrawn = false;
						int textYForIcon = currentY; // Standard Y-Position für Text
						try {
							context.drawTexture(
								RenderPipelines.GUI_TEXTURED,
								iconToUse,
								currentX, iconY,
								0.0f, 0.0f,
								iconSize, iconSize,
								iconSize, iconSize
							);
							currentX += iconSize + 2; // Abstand nach Icon
							iconDrawn = true;
							// Zentriere Text vertikal zum Icon: Icon-Mitte minus die Hälfte der Text-Höhe
							textYForIcon = lineCenterY - client.textRenderer.fontHeight / 2;
						} catch (Exception e) {
							// Fallback: Zeichne Text wenn Icon nicht geladen werden kann
							if (fallbackText != null) {
								context.drawText(
									client.textRenderer,
									Text.literal(fallbackText),
									currentX,
									currentY,
									currentTextColor,
									true
								);
								currentX += client.textRenderer.getWidth(fallbackText);
							}
						}
						// Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
						if (iconDrawn) {
							context.drawText(
								client.textRenderer,
								Text.literal(": "),
								currentX,
								textYForIcon,
								currentTextColor,
								true
							);
							currentX += client.textRenderer.getWidth(": ");
						}
						// Zeichne die Werte nach dem Doppelpunkt (vertikal zentriert zum Icon)
						context.drawText(
							client.textRenderer,
							Text.literal(line.text),
							currentX,
							textYForIcon,
							currentTextColor,
							true
						);
						currentX += client.textRenderer.getWidth(line.text);
					} else {
						// Fallback: Zeichne normalen Text
						context.drawText(
							client.textRenderer,
							textComponent,
							currentX,
							currentY,
							currentTextColor,
							true
						);
						currentX += client.textRenderer.getWidth(line.text);
					}
				} else {
					// Zeichne Haupttext (inkl. "Amboss:" wenn kein Icon)
					context.drawText(
						client.textRenderer,
						textComponent,
						currentX,
						currentY,
						currentTextColor,
						true // Mit Schatten für bessere Lesbarkeit
					);
					currentX += client.textRenderer.getWidth(line.text);
				}
				
				// Zeichne Prozente direkt nach dem Text, wenn vorhanden
				// Wenn Warnung aktiv ist, blinkt auch der Prozentwert rot
				if (line.showPercent && line.percentText != null) {
					Text percentComponent = Text.literal(" " + line.percentText);
					int currentPercentColor = line.showWarning ? currentTextColor : percentColor;
					// Wenn Icon vorhanden ist, verwende die zentrierte Y-Position
					int percentY = (line.showIcon && line.configKey != null) ? 
						(currentY + actualLineHeight / 2 - client.textRenderer.fontHeight / 2) : currentY;
					context.drawText(
						client.textRenderer,
						percentComponent,
						currentX,
						percentY,
						currentPercentColor,
						true // Mit Schatten für bessere Lesbarkeit
					);
					currentX += client.textRenderer.getWidth(" " + line.percentText);
				}
			} catch (Exception e) {
				// Fallback: Versuche mit String direkt
				try {
					// Bestimme Textfarbe: rot und blinkend wenn Warnung aktiv ist
					int currentTextColor = textColor;
					if (line.showWarning) {
						// Blink-Animation: alle 300ms wechseln
						boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
						if (isVisible) {
							currentTextColor = warningColor;
						} else {
							currentTextColor = textColor;
						}
					}
					
					context.drawText(
						client.textRenderer,
						line.text,
						PADDING, // Relativ zu (xPosition, yPosition) nach Matrix-Transformation
						currentY,
						currentTextColor,
						true
					);
					
					// Zeichne Prozente direkt nach dem Text, wenn vorhanden
					// Wenn Warnung aktiv ist, blinkt auch der Prozentwert rot
					int currentX = PADDING + client.textRenderer.getWidth(line.text);
					if (line.showPercent && line.percentText != null) {
						int currentPercentColor = line.showWarning ? currentTextColor : percentColor;
						context.drawText(
							client.textRenderer,
							" " + line.percentText,
							currentX,
							currentY,
							currentPercentColor,
							true
						);
						currentX += client.textRenderer.getWidth(" " + line.percentText);
					}
				} catch (Exception e2) {
					// Ignoriere Fehler
				}
			}
			// Verwende actualLineHeight statt LINE_HEIGHT, um größere Icons zu berücksichtigen
			// Wenn diese Zeile ein Icon hat, füge zusätzlichen Abstand hinzu
			int lineSpacing = actualLineHeight;
			if (line.showIcon && line.configKey != null) {
				// Zusätzlicher Abstand für Zeilen mit Icons
				lineSpacing += 2;
			}
			currentY += lineSpacing;
		}
		} // Ende if (!lines.isEmpty())
		
		matrices.popMatrix();
		
		// Rendere separate Overlays für Informationen, die diese Option aktiviert haben
		renderSeparateOverlays(context, client);
	}
	
	/**
	 * Rendert separate Overlays für einzelne Informationen
	 */
	private static void renderSeparateOverlays(DrawContext context, MinecraftClient client) {
		// Forschung
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschung && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsForschungPercent;
			if (showPercent) {
				if (forschung.isValid()) {
					// Forschung zählt runter: wenn current näher an max ist, ist der Prozent höher
					// Prozent = (current / max) * 100
					percent = calculatePercent(forschung.current, forschung.max);
				} else {
					// Prüfe ob "Nicht im Tab-Widget" angezeigt wird
					String displayString = forschung.getDisplayString();
					if (displayString != null && displayString.contains("Nicht im Tab-Widget")) {
						// Keine Prozentanzeige wenn "Nicht im Tab-Widget"
						showPercent = false;
						percent = null;
					} else {
						// Zeige "?%" wenn Daten noch nicht verfügbar sind
						percent = "?%";
					}
				}
			}
			// Warnung: wenn verbleibender Wert <= Schwelle (0-23)
			int warnValue = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungWarnValue;
			boolean showWarning = shouldShowForschungWarning(warnValue);
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowIcon;
			String displayText = showIcon ? forschung.getDisplayString() : "Forschung: " + forschung.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "forschung", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungShowBackground);
		}
		
		// Amboss
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmboss && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsAmbossPercent;
			if (showPercent && ambossKapazitaet.isValid()) {
				percent = calculatePercent(ambossKapazitaet.current, ambossKapazitaet.max);
			}
			double currentPercent = ambossKapazitaet.isValid() ? 
				((double)ambossKapazitaet.current / (double)ambossKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowIcon;
			String displayText = showIcon ? ambossKapazitaet.getDisplayString() : "Amboss: " + ambossKapazitaet.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "amboss", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossShowBackground);
		}
		
		// Schmelzofen
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofen && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSchmelzofenPercent;
			if (showPercent && schmelzofenKapazitaet.isValid()) {
				percent = calculatePercent(schmelzofenKapazitaet.current, schmelzofenKapazitaet.max);
			}
			double currentPercent = schmelzofenKapazitaet.isValid() ? 
				((double)schmelzofenKapazitaet.current / (double)schmelzofenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowIcon;
			String displayText = showIcon ? schmelzofenKapazitaet.getDisplayString() : "Schmelzofen: " + schmelzofenKapazitaet.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "schmelzofen", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenShowBackground);
		}
		
		// Jäger
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaeger && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsJaegerPercent;
			if (showPercent && jaegerKapazitaet.isValid()) {
				percent = calculatePercent(jaegerKapazitaet.current, jaegerKapazitaet.max);
			}
			double currentPercent = jaegerKapazitaet.isValid() ? 
				((double)jaegerKapazitaet.current / (double)jaegerKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowIcon;
			String displayText = showIcon ? jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix() : "Jäger: " + jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "jaeger", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerShowBackground);
		}
		
		// Seelen
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelen && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsSeelenPercent;
			if (showPercent && seelenKapazitaet.isValid()) {
				percent = calculatePercent(seelenKapazitaet.current, seelenKapazitaet.max);
			}
			double currentPercent = seelenKapazitaet.isValid() ? 
				((double)seelenKapazitaet.current / (double)seelenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowIcon;
			String displayText = showIcon ? seelenKapazitaet.getDisplayString() : "Seelen: " + seelenKapazitaet.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "seelen", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenShowBackground);
		}
		
		// Essenzen
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzen && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsEssenzenPercent;
			if (showPercent && essenzenKapazitaet.isValid()) {
				percent = calculatePercent(essenzenKapazitaet.current, essenzenKapazitaet.max);
			}
			double currentPercent = essenzenKapazitaet.isValid() ? 
				((double)essenzenKapazitaet.current / (double)essenzenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowIcon;
			String displayText = showIcon ? essenzenKapazitaet.getDisplayString() : "Essenzen: " + essenzenKapazitaet.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "essenzen", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenShowBackground);
		}
		
		// Kombo Kiste
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsKomboKiste && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteSeparateOverlay) {
			boolean showWarning = komboKiste.isValid() && komboKiste.current >= komboKiste.max;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowIcon;
			String fraction = getKomboKisteFractionDisplay();
			String displayText = showIcon ? fraction : "Kombo Kiste: " + fraction;
			renderSingleInfoOverlay(context, client, displayText, 
				null, false, showWarning, "komboKiste", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteShowBackground);
		}
		
		// Machtkristalle - prüfe ob einzeln oder zusammen gerendert werden soll
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalle && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSeparateOverlay) {
			// Prüfe welche Slots einzeln gerendert werden sollen
			boolean slot1Separate = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Separate;
			boolean slot2Separate = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Separate;
			boolean slot3Separate = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Separate;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowIcon;
			
			// Liste für Slots, die zusammen gerendert werden sollen
			List<LineWithPercent> mkLines = new ArrayList<>();
			int yOffset = 0; // Für einzelne Overlays
			
			for (int i = 0; i < 3; i++) {
				// Prüfe ob dieser Slot aktiviert ist
				boolean slotEnabled;
				switch (i) {
					case 0:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot1;
						break;
					case 1:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot2;
						break;
					case 2:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleSlot3;
						break;
					default:
						slotEnabled = true;
						break;
				}
				
				// Überspringe diesen Slot, wenn er deaktiviert ist
				if (!slotEnabled) {
					continue;
				}
				
				MachtkristallSlot slot = machtkristallSlots[i];
				boolean slotSeparate = (i == 0 && slot1Separate) || (i == 1 && slot2Separate) || (i == 2 && slot3Separate);
				
				String displayText;
				String percentText = null;
				
				// Prüfe zuerst ob Slot nicht gefunden wurde (keine Machtkristall-Einträge in Tab-Liste)
				// Dies muss VOR isEmpty() geprüft werden, da isNotFound() true ist wenn name = ""
				if (slot.isNotFound()) {
					// Slot nicht gefunden (keine Machtkristall-Einträge in Tab-Liste)
					displayText = showIcon ? "Nicht im Tab-Widget" : "MK " + (i + 1) + ": Nicht im Tab-Widget";
				} else if (slot.isEmpty()) {
					// Slot ist leer (durch "[Kein Machtkristall ausgewählt]" markiert)
					displayText = showIcon ? "-" : "MK " + (i + 1) + ": -";
				} else {
					// Machtkristall gefunden - prüfe ob Daten verfügbar sind
					displayText = slot.getDisplayText();
					percentText = slot.getPercentText();
					if (displayText == null) {
						displayText = showIcon ? "?" : "MK " + (i + 1) + ": ?";
					} else if (showIcon) {
						// Entferne "MK [Name]: " Präfix wenn Icon angezeigt wird
						displayText = MachtkristallSlot.stripIconPrefix(displayText);
					}
				}
				
				// Warnung: wenn Prozent >= dem Warnwert ist (wie bei Amboss)
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleWarnPercent;
				boolean showWarning = getMachtkristallShowWarning(slot, warnPercent);
				
				if (slotSeparate) {
					// Rendere einzeln mit individueller Position
					int slotX, slotY;
					switch (i) {
						case 0:
							slotX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1X;
							slotY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Y;
							break;
						case 1:
							slotX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2X;
							slotY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Y;
							break;
						case 2:
							slotX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3X;
							slotY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Y;
							break;
						default:
							slotX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleX;
							slotY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleY;
							break;
					}
					// Bestimme den richtigen configKey für die Skalierung
					String slotConfigKey;
					switch (i) {
						case 0:
							slotConfigKey = "machtkristalleSlot1";
							break;
						case 1:
							slotConfigKey = "machtkristalleSlot2";
							break;
						case 2:
							slotConfigKey = "machtkristalleSlot3";
							break;
						default:
							slotConfigKey = "machtkristalle";
							break;
					}
					renderSingleInfoOverlay(context, client, 
						displayText, 
						percentText, percentText != null, showWarning, slotConfigKey, showIcon,
						slotX,
						slotY,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowBackground);
				} else {
					// Füge zur Liste hinzu (wird zusammen gerendert)
					mkLines.add(new LineWithPercent(displayText, percentText, percentText != null, showWarning, "machtkristalle", showIcon));
				}
			}
			
			// Rendere alle Zeilen, die zusammen gerendert werden sollen, in einem einzigen Overlay
			if (!mkLines.isEmpty()) {
				// Berechne Y-Position für das Multi-Line-Overlay (nach den einzelnen Overlays)
				int multiLineY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleY + yOffset;
				renderMultiLineOverlay(context, client, mkLines,
					net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleX,
					multiLineY,
					net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleShowBackground,
					"machtkristalle");
			}
		}
		
		// Recycler Slots - prüfe ob einzeln oder zusammen gerendert werden soll
		// Prüfe ob mindestens ein Recycler-Slot aktiviert ist und "Separates Overlay" hat
		boolean recyclerSlot1Active = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot1 && 
		                              net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1SeparateOverlay;
		boolean recyclerSlot2Active = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot2 && 
		                              net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2SeparateOverlay;
		boolean recyclerSlot3Active = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerSlot3 && 
		                              net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3SeparateOverlay;
		
		if (recyclerSlot1Active || recyclerSlot2Active || recyclerSlot3Active) {
			// Prüfe welche Slots einzeln gerendert werden sollen
			boolean slot1Separate = recyclerSlot1Active && net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Separate;
			boolean slot2Separate = recyclerSlot2Active && net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Separate;
			boolean slot3Separate = recyclerSlot3Active && net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Separate;
			
			// Liste für Slots, die zusammen gerendert werden sollen
			List<LineWithPercent> recyclerLines = new ArrayList<>();
			
			// Recycler Slot 1
			if (recyclerSlot1Active) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
				if (showPercent && recyclerSlot1.isValid()) {
					percent = calculatePercent(recyclerSlot1.current, recyclerSlot1.max);
				}
				double currentPercent = recyclerSlot1.isValid() ? 
					((double)recyclerSlot1.current / (double)recyclerSlot1.max) * 100.0 : 0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
				// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
				boolean showWarning = warnPercent >= 0 && currentPercent <= warnPercent;
				boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1ShowIcon;
				// Wenn Icon aktiviert ist, zeige nur die Werte (oder "Nicht im Tab-Widget" wenn nicht gültig)
				// Das Icon wird dann automatisch vor dem Text angezeigt
				String displayText = showIcon ? recyclerSlot1.getDisplayString() : "Recycler Slot 1: " + recyclerSlot1.getDisplayString();
				
				if (slot1Separate) {
					// Rendere einzeln mit individueller Position
					renderSingleInfoOverlay(context, client, displayText, 
						percent, showPercent, showWarning, "recyclerSlot1", showIcon,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1X,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Y,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerShowBackground);
				} else {
					// Füge zur Liste hinzu (wird zusammen gerendert)
					recyclerLines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "recyclerSlot1", showIcon));
				}
			}
			
			// Recycler Slot 2
			if (recyclerSlot2Active) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
				if (showPercent && recyclerSlot2.isValid()) {
					percent = calculatePercent(recyclerSlot2.current, recyclerSlot2.max);
				}
				double currentPercent = recyclerSlot2.isValid() ? 
					((double)recyclerSlot2.current / (double)recyclerSlot2.max) * 100.0 : 0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
				// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
				boolean showWarning = warnPercent >= 0 && currentPercent <= warnPercent;
				boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2ShowIcon;
				String displayText = showIcon ? recyclerSlot2.getDisplayString() : "Recycler Slot 2: " + recyclerSlot2.getDisplayString();
				
				if (slot2Separate) {
					// Rendere einzeln mit individueller Position
					renderSingleInfoOverlay(context, client, displayText, 
						percent, showPercent, showWarning, "recyclerSlot2", showIcon,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2X,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Y,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerShowBackground);
				} else {
					// Füge zur Liste hinzu (wird zusammen gerendert)
					recyclerLines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "recyclerSlot2", showIcon));
				}
			}
			
			// Recycler Slot 3
			if (recyclerSlot3Active) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsRecyclerPercent;
				if (showPercent && recyclerSlot3.isValid()) {
					percent = calculatePercent(recyclerSlot3.current, recyclerSlot3.max);
				}
				double currentPercent = recyclerSlot3.isValid() ? 
					((double)recyclerSlot3.current / (double)recyclerSlot3.max) * 100.0 : 0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerWarnPercent;
				// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
				boolean showWarning = warnPercent >= 0 && currentPercent <= warnPercent;
				boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3ShowIcon;
				String displayText = showIcon ? recyclerSlot3.getDisplayString() : "Recycler Slot 3: " + recyclerSlot3.getDisplayString();
				
				if (slot3Separate) {
					// Rendere einzeln mit individueller Position
					renderSingleInfoOverlay(context, client, displayText, 
						percent, showPercent, showWarning, "recyclerSlot3", showIcon,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3X,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Y,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerShowBackground);
				} else {
					// Füge zur Liste hinzu (wird zusammen gerendert)
					recyclerLines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "recyclerSlot3", showIcon));
				}
			}
			
			// Rendere alle Zeilen, die zusammen gerendert werden sollen, in einem einzigen Overlay
			if (!recyclerLines.isEmpty()) {
				// Verwende die gemeinsame Position für das Multi-Line-Overlay
				int baseX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerX;
				int baseY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerY;
				boolean showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerShowBackground;
				
				renderMultiLineOverlay(context, client, recyclerLines,
					baseX,
					baseY,
					showBackground,
					"recycler"); // Verwende "recycler" als configKey für Farben
			}
		}
	}
	
	/**
	 * Rendert ein Multi-Line-Overlay (für Machtkristalle, die alle 3 Slots in einem Overlay anzeigen)
	 */
	private static void renderMultiLineOverlay(DrawContext context, MinecraftClient client,
		List<LineWithPercent> lines, int xPosition, int yPosition, boolean showBackground, String configKey) {
		if (lines.isEmpty()) {
			return;
		}
		
		final int PADDING = 5;
		final int LINE_HEIGHT = client.textRenderer.fontHeight + 2;
		
		// Berechne die maximale Breite des Textes
		int maxWidth = 0;
		for (LineWithPercent line : lines) {
			int width = 0;
			// Wenn Icon aktiviert ist, füge Icon-Breite hinzu
			if (line.showIcon && (line.configKey != null && ("forschung".equals(line.configKey) || "amboss".equals(line.configKey) || 
			                                                   "schmelzofen".equals(line.configKey) || "seelen".equals(line.configKey) || 
			                                                   "essenzen".equals(line.configKey) || "jaeger".equals(line.configKey) || "komboKiste".equals(line.configKey) || 
			                                                   "machtkristalle".equals(line.configKey) ||
			                                                   "recyclerSlot1".equals(line.configKey) || "recyclerSlot2".equals(line.configKey) || 
			                                                   "recyclerSlot3".equals(line.configKey)))) {
				int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
				width += iconSize + 2; // Icon + Abstand
				width += client.textRenderer.getWidth(": "); // Doppelpunkt nach Icon
			}
			width += client.textRenderer.getWidth(line.text);
			if (line.showPercent && line.percentText != null) {
				width += client.textRenderer.getWidth(" " + line.percentText);
			}
			if (width > maxWidth) {
				maxWidth = width;
			}
		}
		
		// Berechne die tatsächliche Zeilenhöhe
		int actualLineHeight = LINE_HEIGHT;
		// Berücksichtige Icons bei der Höhenberechnung
		for (LineWithPercent line : lines) {
			if (line.showIcon && line.configKey != null) {
				int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
				actualLineHeight = Math.max(actualLineHeight, iconSize);
			}
		}
		
		// Berechne unskalierte Overlay-Dimensionen komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
		int unscaledWidth = maxWidth + (PADDING * 2);
		int unscaledHeight = (lines.size() * actualLineHeight) + (PADDING * 2);
		
		// Get scale
		float scale = getSeparateOverlayScale(configKey);
		if (scale <= 0) scale = 1.0f;
		
		// Berechne skalierte Dimensionen
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);
		
		// Position aus Config: baseX ist die linke Kante (wie beim Mining-Overlay)
		int baseX = xPosition;
		
		// Determine if overlay is on left or right side of screen
		int screenWidth = client.getWindow().getScaledWidth();
		boolean isOnLeftSide = baseX < screenWidth / 2;
		
		// Calculate X position based on side (same logic as Mining overlays)
		int posX;
		if (isOnLeftSide) {
			// On left side: keep left edge fixed, expand to the right
			posX = baseX;
		} else {
			// On right side: keep right edge fixed, expand to the left
			// Right edge is: baseX (since baseX is on the right side, it represents the right edge)
			// Keep this right edge fixed, so left edge moves left when width increases
			posX = baseX - overlayWidth;
		}
		
		// Ensure overlay stays within screen bounds
		posX = Math.max(0, Math.min(posX, screenWidth - overlayWidth));
		int posY = yPosition;
		
		// Zeichne Hintergrund (wenn aktiviert) - skaliert
		if (showBackground) {
			context.fill(posX, posY, posX + overlayWidth, posY + overlayHeight, 0x80000000);
		}
		
		// Render content with scale using matrix transformation
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(posX, posY);
		matrices.scale(scale, scale);
		
		// Zeichne alle Zeilen
		int currentY = PADDING;
		int warningColor = 0xFFFF0000; // Rot für Warnungen
		for (LineWithPercent line : lines) {
			if (line.text == null || line.text.trim().isEmpty()) {
				currentY += actualLineHeight;
				continue;
			}
			
			// Hole konfigurierte Farben für diese Zeile (basierend auf line.configKey, nicht configKey)
			// Wenn line.configKey null ist, verwende den übergebenen configKey als Fallback
			String colorConfigKey = (line.configKey != null && !line.configKey.isEmpty()) ? line.configKey : configKey;
			int textColor = getTextColorForConfigKey(colorConfigKey);
			int percentColor = getPercentColorForConfigKey(colorConfigKey);
			
			// Bestimme Textfarbe: rot und blinkend wenn Warnung aktiv ist
			int currentTextColor = textColor;
			if (line.showWarning) {
				// Blink-Animation: alle 300ms wechseln
				boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
				if (isVisible) {
					currentTextColor = warningColor;
				} else {
					currentTextColor = textColor;
				}
			}
			
			int currentX = PADDING;
			
			// Zeichne Icon statt Text, wenn aktiviert (für Forschung, Amboss, Schmelzofen, Seelen, Essenzen, Jäger, Machtkristalle und Recycler)
			if (line.showIcon && (line.configKey != null && ("forschung".equals(line.configKey) || "amboss".equals(line.configKey) || 
			                                        "schmelzofen".equals(line.configKey) || "seelen".equals(line.configKey) || 
			                                        "essenzen".equals(line.configKey) || "jaeger".equals(line.configKey) || "komboKiste".equals(line.configKey) || 
			                                        "machtkristalle".equals(line.configKey) ||
			                                        "recyclerSlot1".equals(line.configKey) || "recyclerSlot2".equals(line.configKey) || 
			                                        "recyclerSlot3".equals(line.configKey)))) {
				int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
				// Zentriere Icon vertikal: Zeilen-Mitte minus die Hälfte der Icon-Höhe
				int lineCenterY = currentY + actualLineHeight / 2;
				int iconY = lineCenterY - iconSize / 2;
				Identifier iconToUse = null;
				String fallbackText = null;
				
				if ("forschung".equals(line.configKey)) {
					iconToUse = FORSCHUNG_ICON;
					fallbackText = "Forschung: ";
				} else if ("amboss".equals(line.configKey)) {
					iconToUse = AMBOSS_ICON;
					fallbackText = "Amboss: ";
				} else if ("schmelzofen".equals(line.configKey)) {
					iconToUse = SCHMELZOFEN_ICON;
					fallbackText = "Schmelzofen: ";
				} else if ("seelen".equals(line.configKey)) {
					iconToUse = SEELEN_ICON;
					fallbackText = "Seelen: ";
				} else if ("essenzen".equals(line.configKey)) {
					iconToUse = ESSENZEN_ICON;
					fallbackText = "Essenzen: ";
				} else if ("jaeger".equals(line.configKey)) {
					iconToUse = JAEGER_ICON;
					fallbackText = "Jäger: ";
				} else if ("komboKiste".equals(line.configKey)) {
					iconToUse = KOMBO_KISTE_ICON;
					fallbackText = "Kombo Kiste: ";
				} else if ("recyclerSlot1".equals(line.configKey)) {
					iconToUse = RECYCLER_ICON;
					fallbackText = "Recycler Slot 1: ";
				} else if ("recyclerSlot2".equals(line.configKey)) {
					iconToUse = RECYCLER_ICON;
					fallbackText = "Recycler Slot 2: ";
				} else if ("recyclerSlot3".equals(line.configKey)) {
					iconToUse = RECYCLER_ICON;
					fallbackText = "Recycler Slot 3: ";
				} else if ("machtkristalle".equals(line.configKey)) {
					iconToUse = MACHTKRISTALL_ICON;
					fallbackText = "MK: ";
				}
				
				if (iconToUse != null) {
					int textYForIcon = currentY; // Standard Y-Position für Text
					try {
						context.drawTexture(
							RenderPipelines.GUI_TEXTURED,
							iconToUse,
							currentX, iconY,
							0.0f, 0.0f,
							iconSize, iconSize,
							iconSize, iconSize
						);
						currentX += iconSize + 2; // Abstand nach Icon
						// Zentriere Text vertikal zum Icon: Icon-Mitte minus die Hälfte der Text-Höhe
						textYForIcon = lineCenterY - client.textRenderer.fontHeight / 2;
					} catch (Exception e) {
						// Fallback: Zeichne Text wenn Icon nicht geladen werden kann
						if (fallbackText != null) {
							context.drawText(client.textRenderer, Text.literal(fallbackText), currentX, currentY, currentTextColor, true);
							currentX += client.textRenderer.getWidth(fallbackText);
						}
					}
					// Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
					context.drawText(client.textRenderer, Text.literal(": "), currentX, textYForIcon, currentTextColor, true);
					currentX += client.textRenderer.getWidth(": ");
					// Zeichne die Werte nach dem Doppelpunkt (vertikal zentriert zum Icon)
					context.drawText(client.textRenderer, Text.literal(line.text), currentX, textYForIcon, currentTextColor, true);
					currentX += client.textRenderer.getWidth(line.text);
				} else {
					// Fallback: Zeichne normalen Text
					context.drawText(client.textRenderer, Text.literal(line.text), currentX, currentY, currentTextColor, true);
					currentX += client.textRenderer.getWidth(line.text);
				}
			} else {
				// Zeichne Text ohne Icon
				context.drawText(
					client.textRenderer,
					line.text,
					currentX,
					currentY,
					currentTextColor,
					true
				);
				currentX += client.textRenderer.getWidth(line.text);
			}
			
			// Zeichne Prozente, wenn vorhanden
			if (line.showPercent && line.percentText != null) {
				int percentY = currentY;
				// Wenn Icon aktiviert ist, zentriere Prozente vertikal zum Icon
				if (line.showIcon && line.configKey != null) {
					int lineCenterY = currentY + actualLineHeight / 2;
					percentY = lineCenterY - client.textRenderer.fontHeight / 2;
				}
				// Wenn Warnung aktiv ist, blinkt auch der Prozentwert rot
				int currentPercentColor = line.showWarning ? currentTextColor : percentColor;
				context.drawText(
					client.textRenderer,
					" " + line.percentText,
					currentX,
					percentY,
					currentPercentColor,
					true
				);
			}
			
			currentY += actualLineHeight;
		}
		
		matrices.popMatrix();
	}
	
	/**
	 * Gibt den Scale-Faktor für ein separates Overlay zurück
	 */
	private static float getSeparateOverlayScale(String configKey) {
		if (configKey == null) return 1.0f;
		switch (configKey) {
			case "forschung":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungScale;
			case "amboss":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossScale;
			case "schmelzofen":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenScale;
			case "jaeger":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerScale;
			case "komboKiste":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteScale;
			case "seelen":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenScale;
			case "essenzen":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenScale;
			case "machtkristalle":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleScale;
			case "machtkristalleSlot1":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot1Scale;
			case "machtkristalleSlot2":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot2Scale;
			case "machtkristalleSlot3":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleSlot3Scale;
			case "recycler":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerScale;
			case "recyclerSlot1":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1Scale;
			case "recyclerSlot2":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2Scale;
			case "recyclerSlot3":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3Scale;
			default:
				return 1.0f;
		}
	}
	
	/**
	 * Gibt die konfigurierte Textfarbe für einen NPC Alerts-Eintrag zurück
	 */
	public static int getTextColorForConfigKey(String configKey) {
		if (configKey == null) return 0xFFFFFFFF;
		java.awt.Color color;
		switch (configKey) {
			case "forschung":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungTextColor;
				break;
			case "amboss":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossTextColor;
				break;
			case "schmelzofen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenTextColor;
				break;
			case "jaeger":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerTextColor;
				break;
			case "komboKiste":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteTextColor;
				break;
			case "seelen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenTextColor;
				break;
			case "essenzen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenTextColor;
				break;
			case "machtkristalle":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleTextColor;
				break;
			case "recyclerSlot1":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1TextColor;
				break;
			case "recyclerSlot2":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2TextColor;
				break;
			case "recyclerSlot3":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3TextColor;
				break;
			default:
				return 0xFFFFFFFF;
		}
		return (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
	}
	
	/**
	 * Gibt die konfigurierte Prozentfarbe für einen NPC Alerts-Eintrag zurück
	 */
	public static int getPercentColorForConfigKey(String configKey) {
		if (configKey == null) return 0xFFFFFF00;
		java.awt.Color color;
		switch (configKey) {
			case "forschung":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungPercentColor;
				break;
			case "amboss":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossPercentColor;
				break;
			case "schmelzofen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenPercentColor;
				break;
			case "jaeger":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerPercentColor;
				break;
			case "seelen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenPercentColor;
				break;
			case "essenzen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenPercentColor;
				break;
			case "machtkristalle":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristallePercentColor;
				break;
			case "recyclerSlot1":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1PercentColor;
				break;
			case "recyclerSlot2":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2PercentColor;
				break;
			case "recyclerSlot3":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3PercentColor;
				break;
			default:
				return 0xFFFFFF00;
		}
		return (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
	}
	
	/**
	 * Rendert ein einzelnes Overlay für eine Information (mit Icon-Support und Hintergrund-Option)
	 */
	private static void renderSingleInfoOverlay(DrawContext context, MinecraftClient client, 
		String text, String percentText, boolean showPercent, boolean showWarning, String configKey,
		boolean showIcon, int xPosition, int yPosition, boolean showBackground) {
		final int PADDING = 5;
		final int LINE_HEIGHT = client.textRenderer.fontHeight + 2;
		// Hole konfigurierte Farben
		int textColor = getTextColorForConfigKey(configKey);
		int percentColor = getPercentColorForConfigKey(configKey);
		
		// Berechne Breite
		int width;
		if (showIcon && (configKey != null && ("forschung".equals(configKey) || "amboss".equals(configKey) || 
		                                        "schmelzofen".equals(configKey) || "seelen".equals(configKey) || 
		                                        "essenzen".equals(configKey) || "jaeger".equals(configKey) || "komboKiste".equals(configKey) || 
		                                        "machtkristalle".equals(configKey) ||
		                                        "machtkristalleSlot1".equals(configKey) || "machtkristalleSlot2".equals(configKey) || 
		                                        "machtkristalleSlot3".equals(configKey) ||
		                                        "recyclerSlot1".equals(configKey) || "recyclerSlot2".equals(configKey) || 
		                                        "recyclerSlot3".equals(configKey)))) {
			int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
			width = iconSize + 2; // Icon + Abstand
			width += client.textRenderer.getWidth(": "); // Doppelpunkt nach Icon
			width += client.textRenderer.getWidth(text); // Werte nach dem Doppelpunkt
		} else {
			width = client.textRenderer.getWidth(text);
		}
		if (showPercent && percentText != null) {
			width += client.textRenderer.getWidth(" " + percentText);
		}
		// Berechne die tatsächliche Zeilenhöhe unter Berücksichtigung von Icons
		int actualLineHeight = LINE_HEIGHT;
		if (showIcon && configKey != null) {
			int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
			// Die tatsächliche Höhe ist das Maximum aus Icon-Höhe und Text-Höhe
			actualLineHeight = Math.max(actualLineHeight, iconSize);
		}
		
		// Berechne unskalierte Dimensionen komplett dynamisch basierend auf Inhalt (wie BossHP-Overlay)
		int unscaledWidth = width + (PADDING * 2);
		int unscaledHeight = actualLineHeight + (PADDING * 2);
		
		// Get scale
		float scale = getSeparateOverlayScale(configKey);
		if (scale <= 0) scale = 1.0f;
		
		// Berechne skalierte Dimensionen
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);
		
		// Position aus Config: baseX ist die linke Kante (wie beim Mining-Overlay)
		int baseX = xPosition;
		
		// Determine if overlay is on left or right side of screen
		int screenWidth = client.getWindow().getScaledWidth();
		boolean isOnLeftSide = baseX < screenWidth / 2;
		
		// Calculate X position based on side (same logic as Mining overlays)
		int posX;
		if (isOnLeftSide) {
			// On left side: keep left edge fixed, expand to the right
			posX = baseX;
		} else {
			// On right side: keep right edge fixed, expand to the left
			// Right edge is: baseX (since baseX is on the right side, it represents the right edge)
			// Keep this right edge fixed, so left edge moves left when width increases
			posX = baseX - overlayWidth;
		}
		
		// Ensure overlay stays within screen bounds
		posX = Math.max(0, Math.min(posX, screenWidth - overlayWidth));
		int posY = yPosition;
		
		// Zeichne Hintergrund (wenn aktiviert) - skaliert
		if (showBackground) {
			context.fill(posX, posY, posX + overlayWidth, posY + overlayHeight, 0x80000000);
		}
		
		// Render content with scale using matrix transformation
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(posX, posY);
		matrices.scale(scale, scale);
		
		// Zeichne Text - vertikal zentriert (actualLineHeight wurde bereits berechnet)
		int currentX = PADDING; // Relativ zu (posX, posY) nach Matrix-Transformation
		// Zentriere: Overlay-Mitte, dann verschiebe nach oben um die Hälfte der fontHeight (da Text-Baseline unten ist)
		// Verwende unskalierte Höhe für Zentrierung (Koordinaten sind nach Matrix-Transformation relativ)
		int overlayCenterY = unscaledHeight / 2;
		int currentY = overlayCenterY - client.textRenderer.fontHeight / 2;
		
		// Bestimme Textfarbe: rot und blinkend wenn Warnung aktiv ist
		int currentTextColor = textColor;
		if (showWarning) {
			// Blink-Animation: alle 300ms wechseln
			boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
			if (isVisible) {
				currentTextColor = 0xFFFF0000; // Rot
			} else {
				currentTextColor = textColor;
			}
		}
		
		try {
			// Zeichne Icon statt Text, wenn aktiviert (für Forschung, Amboss, Schmelzofen, Seelen, Essenzen, Jäger, Machtkristalle und Recycler)
			if (showIcon && (configKey != null && ("forschung".equals(configKey) || "amboss".equals(configKey) || 
			                                        "schmelzofen".equals(configKey) || "seelen".equals(configKey) || 
			                                        "essenzen".equals(configKey) || "jaeger".equals(configKey) || "komboKiste".equals(configKey) || 
			                                        "machtkristalle".equals(configKey) ||
			                                        "machtkristalleSlot1".equals(configKey) || "machtkristalleSlot2".equals(configKey) || 
			                                        "machtkristalleSlot3".equals(configKey) ||
			                                        "recyclerSlot1".equals(configKey) || "recyclerSlot2".equals(configKey) || 
			                                        "recyclerSlot3".equals(configKey)))) {
				int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
				// Zentriere Icon vertikal: Overlay-Mitte minus die Hälfte der Icon-Höhe
				int iconY = overlayCenterY - iconSize / 2;
				Identifier iconToUse = null;
				String fallbackText = null;
				
				if ("forschung".equals(configKey)) {
					iconToUse = FORSCHUNG_ICON;
					fallbackText = "Forschung: ";
				} else if ("amboss".equals(configKey)) {
					iconToUse = AMBOSS_ICON;
					fallbackText = "Amboss: ";
				} else if ("schmelzofen".equals(configKey)) {
					iconToUse = SCHMELZOFEN_ICON;
					fallbackText = "Schmelzofen: ";
				} else if ("seelen".equals(configKey)) {
					iconToUse = SEELEN_ICON;
					fallbackText = "Seelen: ";
				} else if ("essenzen".equals(configKey)) {
					iconToUse = ESSENZEN_ICON;
					fallbackText = "Essenzen: ";
				} else if ("jaeger".equals(configKey)) {
					iconToUse = JAEGER_ICON;
					fallbackText = "Jäger: ";
				} else if ("komboKiste".equals(configKey)) {
					iconToUse = KOMBO_KISTE_ICON;
					fallbackText = "Kombo Kiste: ";
				} else if ("recyclerSlot1".equals(configKey)) {
					iconToUse = RECYCLER_ICON;
					fallbackText = "Recycler Slot 1: ";
				} else if ("recyclerSlot2".equals(configKey)) {
					iconToUse = RECYCLER_ICON;
					fallbackText = "Recycler Slot 2: ";
				} else if ("recyclerSlot3".equals(configKey)) {
					iconToUse = RECYCLER_ICON;
					fallbackText = "Recycler Slot 3: ";
				} else if ("machtkristalle".equals(configKey) || "machtkristalleSlot1".equals(configKey) || 
				           "machtkristalleSlot2".equals(configKey) || "machtkristalleSlot3".equals(configKey)) {
					iconToUse = MACHTKRISTALL_ICON;
					fallbackText = "MK: ";
				}
				
				if (iconToUse != null) {
					int textYForIcon = currentY; // Standard Y-Position für Text
					try {
						context.drawTexture(
							RenderPipelines.GUI_TEXTURED,
							iconToUse,
							currentX, iconY,
							0.0f, 0.0f,
							iconSize, iconSize,
							iconSize, iconSize
						);
						currentX += iconSize + 2; // Abstand nach Icon
						// Zentriere Text vertikal zum Icon: Icon-Mitte minus die Hälfte der Text-Höhe
						textYForIcon = overlayCenterY - client.textRenderer.fontHeight / 2;
					} catch (Exception e) {
						// Fallback: Zeichne Text wenn Icon nicht geladen werden kann
						if (fallbackText != null) {
							context.drawText(client.textRenderer, Text.literal(fallbackText), currentX, currentY, currentTextColor, true);
							currentX += client.textRenderer.getWidth(fallbackText);
						}
					}
					// Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
					context.drawText(client.textRenderer, Text.literal(": "), currentX, textYForIcon, currentTextColor, true);
					currentX += client.textRenderer.getWidth(": ");
					// Zeichne die Werte nach dem Doppelpunkt (vertikal zentriert zum Icon)
					context.drawText(client.textRenderer, Text.literal(text), currentX, textYForIcon, currentTextColor, true);
					currentX += client.textRenderer.getWidth(text);
				} else {
					// Fallback: Zeichne normalen Text
					context.drawText(client.textRenderer, Text.literal(text), currentX, currentY, currentTextColor, true);
					currentX += client.textRenderer.getWidth(text);
				}
			} else {
				context.drawText(client.textRenderer, Text.literal(text), currentX, currentY, currentTextColor, true);
				currentX += client.textRenderer.getWidth(text);
			}
			
			// Prozente
			// Wenn Warnung aktiv ist, blinkt auch der Prozentwert rot
			if (showPercent && percentText != null) {
				Text percentComponent = Text.literal(" " + percentText);
				int currentPercentColor = showWarning ? currentTextColor : percentColor;
				// Wenn Icon vorhanden ist, verwende die zentrierte Y-Position
				int percentY = (showIcon && configKey != null) ? 
					(overlayCenterY - client.textRenderer.fontHeight / 2) : currentY;
				context.drawText(client.textRenderer, percentComponent, currentX, percentY, currentPercentColor, true);
				currentX += client.textRenderer.getWidth(" " + percentText);
			}
		} catch (Exception e) {
			// Fallback
			try {
				if (showIcon && configKey != null) {
					int iconSize = (int)(client.textRenderer.fontHeight * 1.5);
					// Zentriere Icon vertikal: Overlay-Mitte minus die Hälfte der Icon-Höhe
					// Verwende unskalierte Höhe für Zentrierung (Koordinaten sind nach Matrix-Transformation relativ)
					int fallbackOverlayCenterY = unscaledHeight / 2;
					int iconY = fallbackOverlayCenterY - iconSize / 2;
					Identifier iconToUse = null;
					String fallbackText = null;
					
					if ("forschung".equals(configKey)) {
						iconToUse = FORSCHUNG_ICON;
						fallbackText = "Forschung: ";
					} else if ("amboss".equals(configKey)) {
						iconToUse = AMBOSS_ICON;
						fallbackText = "Amboss: ";
					} else if ("schmelzofen".equals(configKey)) {
						iconToUse = SCHMELZOFEN_ICON;
						fallbackText = "Schmelzofen: ";
					} else if ("seelen".equals(configKey)) {
						iconToUse = SEELEN_ICON;
						fallbackText = "Seelen: ";
					} else if ("essenzen".equals(configKey)) {
						iconToUse = ESSENZEN_ICON;
						fallbackText = "Essenzen: ";
					} else if ("jaeger".equals(configKey)) {
						iconToUse = JAEGER_ICON;
						fallbackText = "Jäger: ";
					} else if ("komboKiste".equals(configKey)) {
						iconToUse = KOMBO_KISTE_ICON;
						fallbackText = "Kombo Kiste: ";
					} else if ("recyclerSlot1".equals(configKey)) {
						iconToUse = RECYCLER_ICON;
						fallbackText = "Recycler Slot 1: ";
					} else if ("recyclerSlot2".equals(configKey)) {
						iconToUse = RECYCLER_ICON;
						fallbackText = "Recycler Slot 2: ";
					} else if ("recyclerSlot3".equals(configKey)) {
						iconToUse = RECYCLER_ICON;
						fallbackText = "Recycler Slot 3: ";
					}
					
					if (iconToUse != null) {
						int fallbackTextYForIcon = currentY; // Standard Y-Position für Text
						try {
							context.drawTexture(
								RenderPipelines.GUI_TEXTURED,
								iconToUse,
								currentX, iconY,
								0.0f, 0.0f,
								iconSize, iconSize,
								iconSize, iconSize
							);
							currentX += iconSize + 2;
							// Zentriere Text vertikal zum Icon: Icon-Mitte minus die Hälfte der Text-Höhe
							fallbackTextYForIcon = fallbackOverlayCenterY - client.textRenderer.fontHeight / 2;
						} catch (Exception e3) {
							// Fallback: Zeichne Text wenn Icon nicht geladen werden kann
							if (fallbackText != null) {
								context.drawText(client.textRenderer, fallbackText, currentX, currentY, currentTextColor, true);
								currentX += client.textRenderer.getWidth(fallbackText);
							}
						}
						// Zeichne Doppelpunkt nach dem Icon (vertikal zentriert zum Icon)
						context.drawText(client.textRenderer, ": ", currentX, fallbackTextYForIcon, currentTextColor, true);
						currentX += client.textRenderer.getWidth(": ");
						// Zeichne die Werte nach dem Doppelpunkt (vertikal zentriert zum Icon)
						context.drawText(client.textRenderer, text, currentX, fallbackTextYForIcon, currentTextColor, true);
						currentX += client.textRenderer.getWidth(text);
					} else {
						// Fallback: Zeichne normalen Text
						context.drawText(client.textRenderer, text, currentX, currentY, currentTextColor, true);
						currentX += client.textRenderer.getWidth(text);
					}
				} else {
					context.drawText(client.textRenderer, text, currentX, currentY, currentTextColor, true);
					currentX += client.textRenderer.getWidth(text);
				}
				// Wenn Warnung aktiv ist, blinkt auch der Prozentwert rot
				if (showPercent && percentText != null) {
					int currentPercentColor = showWarning ? currentTextColor : percentColor;
					// Wenn Icon vorhanden ist, verwende die zentrierte Y-Position
					int percentY = (showIcon && configKey != null) ? 
						(unscaledHeight / 2 - client.textRenderer.fontHeight / 2) : currentY;
					context.drawText(client.textRenderer, " " + percentText, currentX, percentY, currentPercentColor, true);
					currentX += client.textRenderer.getWidth(" " + percentText);
				}
			} catch (Exception e2) {
				// Ignoriere Fehler
			}
		}
		
		matrices.popMatrix();
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


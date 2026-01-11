package net.felix.utilities.Overall.TabInfo;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.felix.CCLiveUtilities;
import net.felix.utilities.Overall.KeyBindingUtility;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility zum Auslesen von Informationen aus der Tab-Liste
 * Liest verschiedene Kapazitäten und Fortschrittsinformationen aus
 */
public class TabInfoUtility {
	
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
			// Nur der Text ohne Prozent (Prozent wird separat angezeigt)
			if (name != null && !name.isEmpty()) {
				return "MK " + name + ":";
			}
			// Fallback: sollte nicht passieren
			return null;
		}
		
		public String getPercentText() {
			if (isEmpty() || isNotFound()) {
				return null;
			}
			// Wenn Daten nicht gefunden wurden, kein Prozent anzeigen
			if (!xpData.isValid()) {
				return null;
			}
			// Verwende extrahierten Prozentwert aus der Tab-Liste, falls verfügbar
			// Sonst berechne Prozent: aktuell / benötigt * 100
			if (xpData.percentFormatted != null) {
				return xpData.percentFormatted;
			} else if (xpData.isValid()) {
				return calculatePercent(xpData.current, xpData.required);
			} else {
				return "?%";
			}
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
			ClientTickEvents.END_CLIENT_TICK.register(TabInfoUtility::onClientTick);
			
			// Registriere HUD-Rendering
			HudRenderCallback.EVENT.register((drawContext, tickDelta) -> onHudRender(drawContext, tickDelta));
			
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
			updateTabInfo(client);
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
	private static void updateTabInfo(MinecraftClient client) {
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
				System.out.println("[Recycler-DEBUG] ✅ Header '[Recycler Slot 1]' gefunden bei Index " + i + ", Text: '" + entryText + "'");
				parseCapacityData(entries, i, getEntryText, removeFormatting, recyclerSlot1, "Recycler Slot 1");
				System.out.println("[Recycler-DEBUG] Nach Parsing - Slot 1: current=" + recyclerSlot1.current + ", max=" + recyclerSlot1.max + ", isValid=" + recyclerSlot1.isValid());
			}
			
			// [Recycler Slot 2]
			if (cleanEntryText.contains("[Recycler Slot 2]")) {
				foundRecyclerSlot2 = true;
				System.out.println("[Recycler-DEBUG] ✅ Header '[Recycler Slot 2]' gefunden bei Index " + i + ", Text: '" + entryText + "'");
				parseCapacityData(entries, i, getEntryText, removeFormatting, recyclerSlot2, "Recycler Slot 2");
				System.out.println("[Recycler-DEBUG] Nach Parsing - Slot 2: current=" + recyclerSlot2.current + ", max=" + recyclerSlot2.max + ", isValid=" + recyclerSlot2.isValid());
			}
			
			// [Recycler Slot 3]
			if (cleanEntryText.contains("[Recycler Slot 3]")) {
				foundRecyclerSlot3 = true;
				System.out.println("[Recycler-DEBUG] ✅ Header '[Recycler Slot 3]' gefunden bei Index " + i + ", Text: '" + entryText + "'");
				parseCapacityData(entries, i, getEntryText, removeFormatting, recyclerSlot3, "Recycler Slot 3");
				System.out.println("[Recycler-DEBUG] Nach Parsing - Slot 3: current=" + recyclerSlot3.current + ", max=" + recyclerSlot3.max + ", isValid=" + recyclerSlot3.isValid());
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
		// Debug: Nur für Recycler-Slots
		boolean isRecycler = debugName != null && debugName.startsWith("Recycler Slot");
		if (isRecycler) {
			System.out.println("[Recycler-DEBUG] 🔍 Suche Datenzeile für " + debugName + " ab Index " + (headerIndex + 1));
		}
		
		// Suche nach der Datenzeile nach dem Header
		// Es kann sein, dass ein Spielername dazwischen steht, also suchen wir in den nächsten 10 Einträgen
		for (int j = headerIndex + 1; j < Math.min(headerIndex + 11, entries.size()); j++) {
			String dataText = getEntryText.apply(j);
			if (dataText == null) {
				if (isRecycler) {
					System.out.println("[Recycler-DEBUG] ⚠️ Index " + j + ": dataText ist null");
				}
				continue;
			}
			
			String cleanDataText = removeFormatting.apply(dataText);
			if (cleanDataText == null || cleanDataText.trim().isEmpty()) {
				if (isRecycler) {
					System.out.println("[Recycler-DEBUG] ⚠️ Index " + j + ": cleanDataText ist leer, Original: '" + dataText + "'");
				}
				continue;
			}
			
			if (isRecycler) {
				System.out.println("[Recycler-DEBUG] 📝 Index " + j + ": '" + cleanDataText + "' (Original: '" + dataText + "')");
			}
			
			// Überspringe Spielernamen (wenn der Text wie ein Spielername aussieht)
			// Spielernamen enthalten normalerweise keine Zahlen oder "/"
			if (!cleanDataText.contains("/") && !cleanDataText.matches(".*\\d+.*")) {
				// Möglicherweise ein Spielername, aber wir prüfen trotzdem weiter
				// da manche Einträge auch ohne "/" sein könnten
				if (isRecycler) {
					System.out.println("[Recycler-DEBUG] ⏭️ Index " + j + ": Überspringe (kein '/' und keine Zahl)");
				}
			}
			
			// Prüfe, ob dies eine Kapazitätszeile ist (enthält "/")
			if (cleanDataText.contains("/")) {
				if (isRecycler) {
					System.out.println("[Recycler-DEBUG] ✅ Index " + j + ": Potenzielle Datenzeile gefunden: '" + cleanDataText + "'");
				}
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
									if (isRecycler) {
										System.out.println("[Recycler-DEBUG] ⚠️ Werte zu groß für int: current=" + currentLong + ", max=" + maxLong);
									}
									// Verwende -1 wenn zu groß (wird als ungültig angezeigt)
									data.current = -1;
									data.max = -1;
									return;
								}
								
								data.current = (int) currentLong;
								data.max = (int) maxLong;
								if (isRecycler) {
									System.out.println("[Recycler-DEBUG] ✅✅✅ Erfolgreich geparst für " + debugName + ": current=" + data.current + ", max=" + data.max);
								}
								return; // Erfolgreich geparst
							} catch (NumberFormatException e) {
								// Wenn Parsing fehlschlägt, setze trotzdem die formatierten Strings
								// und verwende -1 für isValid() Prüfung (wird dann als ungültig angezeigt)
								data.current = -1;
								data.max = -1;
								if (isRecycler) {
									System.out.println("[Recycler-DEBUG] ❌ NumberFormatException beim Parsing: currentNumbersOnly='" + currentNumbersOnly + "', maxNumbersOnly='" + maxNumbersOnly + "', Exception: " + e.getMessage());
								}
								return;
							}
						} else {
							if (isRecycler) {
								System.out.println("[Recycler-DEBUG] ⚠️ Leere Zahlen: currentNumbersOnly='" + currentNumbersOnly + "', maxNumbersOnly='" + maxNumbersOnly + "'");
							}
						}
					} else {
						if (isRecycler) {
							System.out.println("[Recycler-DEBUG] ⚠️ Split ergab nicht 2 Teile: parts.length=" + (parts != null ? parts.length : 0));
						}
					}
				} catch (Exception e) {
					// Weiter suchen
					if (isRecycler) {
						System.out.println("[Recycler-DEBUG] ❌ Exception beim Parsing: " + e.getMessage());
					}
					continue;
				}
			}
		}
		
		// Nicht gefunden - setze auf ungültig
		if (isRecycler) {
			System.out.println("[Recycler-DEBUG] ❌❌❌ Keine Datenzeile gefunden für " + debugName + " in den nächsten 10 Einträgen!");
		}
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
			}
			
			// Prüfe, ob dies eine XP-Zeile ist (enthält "/")
			if (cleanDataText.contains("/")) {
				// Versuche, die formatierten Strings zu extrahieren
				// Format: "AKTUELL / BENÖTIGT XP [PROZENT%]" oder "AKTUELL/BENÖTIGT[PROZENT%]"
				// Beispiel: "50/ 500 XP [10%]" oder "1,234 / 5,000[20.5%]"
				try {
					// Extrahiere Prozentwert aus eckigen Klammern (z.B. "[10%]" -> "10%")
					String percentValue = null;
					java.util.regex.Pattern percentPattern = java.util.regex.Pattern.compile("\\[(\\d+[.,]?\\d*)%\\]");
					java.util.regex.Matcher percentMatcher = percentPattern.matcher(cleanDataText);
					if (percentMatcher.find()) {
						percentValue = percentMatcher.group(1) + "%";
						// Ersetze Komma durch Punkt für einheitliche Formatierung
						percentValue = percentValue.replace(",", ".");
					}
					
					// Entferne Prozentzeichen in eckigen Klammern (z.B. "[10%]")
					// Entferne auch "XP" falls vorhanden (vor oder nach dem Prozentwert)
					String dataWithoutPercent = cleanDataText
						.replaceAll("\\[\\d+[.,]?\\d*%\\]", "") // Entferne [10%]
						.replaceAll("\\s*XP\\s*", " ") // Entferne "XP" mit umgebenden Leerzeichen
						.trim();
					
					// Teile durch "/" auf
					String[] parts = dataWithoutPercent.split("/", 2);
					if (parts.length == 2) {
						String currentPart = parts[0].trim();
						String requiredPart = parts[1].trim();
						
						// Speichere formatierte Strings
						data.currentFormatted = currentPart;
						data.requiredFormatted = requiredPart;
						data.percentFormatted = percentValue; // Speichere extrahierten Prozentwert
						
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
	 * HUD Render callback für das Tab-Info Overlay
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
		
		// Render nur wenn Overlays sichtbar sind
		if (!showOverlays) {
			return;
		}
		
		renderTabInfoDisplay(context, client);
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
	 * Extrahiert den numerischen Prozentwert aus einem String (z.B. "10%" -> 10.0)
	 */
	private static double parsePercentValue(String percentText) {
		if (percentText == null || percentText.trim().isEmpty()) {
			return -1.0;
		}
		try {
			// Entferne "%" und Leerzeichen, ersetze Komma durch Punkt
			String cleaned = percentText.replace("%", "").trim().replace(",", ".");
			return Double.parseDouble(cleaned);
		} catch (NumberFormatException e) {
			return -1.0;
		}
	}
	
	/**
	 * Gibt die Anzahl der Zeilen im Haupt-Overlay zurück (für DraggableOverlay)
	 */
	public static int getMainOverlayLineCount() {
		int count = 0;
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmboss && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaeger && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenSeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay) {
			count += 3; // Immer 3 Slots (auch wenn leer)
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay) {
			count++;
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay) {
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
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschungPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungShowIcon;
			String displayText = showIcon ? "? / ?" : "Forschung: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "forschung", showIcon));
		}
		
		// Amboss Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmboss && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmbossPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossShowIcon;
			String displayText = showIcon ? "? / ?" : "Amboss: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "amboss", showIcon));
		}
		
		// Schmelzofen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofenPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenShowIcon;
			String displayText = showIcon ? "? / ?" : "Schmelzofen: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "schmelzofen", showIcon));
		}
		
		// Jäger Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaeger && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaegerPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerShowIcon;
			String displayText = showIcon ? "? / ?" : "Jäger: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "jaeger", showIcon));
		}
		
		// Seelen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelenPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenShowIcon;
			String displayText = showIcon ? "? / ?" : "Seelen: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "seelen", showIcon));
		}
		
		// Essenzen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenSeparateOverlay) {
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzenPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenShowIcon;
			String displayText = showIcon ? "? / ?" : "Essenzen: ? / ?";
			lines.add(new LineWithPercent(displayText, showPercent ? "0.0%" : null, showPercent, false, "essenzen", showIcon));
		}
		
		// Machtkristalle
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleShowIcon;
			// Zeige alle 3 Slots mit Beispielwerten im Edit-Modus
			// Im Edit-Modus zeigen wir immer "0.0%" als Beispiel, um zu zeigen wie es aussehen würde
			for (int i = 0; i < 3; i++) {
				// Prüfe ob dieser Slot aktiviert ist
				boolean slotEnabled;
				switch (i) {
					case 0:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot1;
						break;
					case 1:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot2;
						break;
					case 2:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot3;
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
		boolean showRecyclerPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerPercent;
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1ShowIcon;
			String displayText = showIcon ? "? / ?" : "Recycler Slot 1: ? / ?";
			String percent = null;
			if (showRecyclerPercent && recyclerSlot1.isValid()) {
				percent = calculatePercent(recyclerSlot1.current, recyclerSlot1.max);
			}
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, false, "recyclerSlot1", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2ShowIcon;
			String displayText = showIcon ? "? / ?" : "Recycler Slot 2: ? / ?";
			String percent = null;
			if (showRecyclerPercent && recyclerSlot2.isValid()) {
				percent = calculatePercent(recyclerSlot2.current, recyclerSlot2.max);
			}
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, false, "recyclerSlot2", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3ShowIcon;
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
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschungPercent;
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
			// Warnung: wenn Prozent UNTER dem Warnwert ist (da Forschung runter zählt)
			double currentPercent = forschung.isValid() ? 
				((double)forschung.current / (double)forschung.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungWarnPercent;
			boolean showWarning = forschung.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungShowIcon;
			String displayText = showIcon ? forschung.getDisplayString() : "Forschung: " + forschung.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "forschung", showIcon));
		}
		
		// Amboss Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmboss && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmbossPercent;
			if (showPercent && ambossKapazitaet.isValid()) {
				percent = calculatePercent(ambossKapazitaet.current, ambossKapazitaet.max);
			}
			double currentPercent = ambossKapazitaet.isValid() ? 
				((double)ambossKapazitaet.current / (double)ambossKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossWarnPercent;
			boolean showWarning = ambossKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossShowIcon;
			String displayText = showIcon ? ambossKapazitaet.getDisplayString() : "Amboss: " + ambossKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "amboss", showIcon));
		}
		
		// Schmelzofen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofenPercent;
			if (showPercent && schmelzofenKapazitaet.isValid()) {
				percent = calculatePercent(schmelzofenKapazitaet.current, schmelzofenKapazitaet.max);
			}
			double currentPercent = schmelzofenKapazitaet.isValid() ? 
				((double)schmelzofenKapazitaet.current / (double)schmelzofenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenWarnPercent;
			boolean showWarning = schmelzofenKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenShowIcon;
			String displayText = showIcon ? schmelzofenKapazitaet.getDisplayString() : "Schmelzofen: " + schmelzofenKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "schmelzofen", showIcon));
		}
		
		// Jäger Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaeger && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaegerPercent;
			if (showPercent && jaegerKapazitaet.isValid()) {
				percent = calculatePercent(jaegerKapazitaet.current, jaegerKapazitaet.max);
			}
			double currentPercent = jaegerKapazitaet.isValid() ? 
				((double)jaegerKapazitaet.current / (double)jaegerKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerWarnPercent;
			boolean showWarning = jaegerKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerShowIcon;
			String displayText = showIcon ? jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix() : "Jäger: " + jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "jaeger", showIcon));
		}
		
		// Seelen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelenPercent;
			if (showPercent && seelenKapazitaet.isValid()) {
				percent = calculatePercent(seelenKapazitaet.current, seelenKapazitaet.max);
			}
			double currentPercent = seelenKapazitaet.isValid() ? 
				((double)seelenKapazitaet.current / (double)seelenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenWarnPercent;
			boolean showWarning = seelenKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenShowIcon;
			String displayText = showIcon ? seelenKapazitaet.getDisplayString() : "Seelen: " + seelenKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "seelen", showIcon));
		}
		
		// Essenzen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzenPercent;
			if (showPercent && essenzenKapazitaet.isValid()) {
				percent = calculatePercent(essenzenKapazitaet.current, essenzenKapazitaet.max);
			}
			double currentPercent = essenzenKapazitaet.isValid() ? 
				((double)essenzenKapazitaet.current / (double)essenzenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenWarnPercent;
			boolean showWarning = essenzenKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenShowIcon;
			String displayText = showIcon ? essenzenKapazitaet.getDisplayString() : "Essenzen: " + essenzenKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "essenzen", showIcon));
		}
		
		// Machtkristalle
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleShowIcon;
			for (int i = 0; i < 3; i++) {
				// Prüfe ob dieser Slot aktiviert ist
				boolean slotEnabled;
				switch (i) {
					case 0:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot1;
						break;
					case 1:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot2;
						break;
					case 2:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot3;
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
						// Entferne "MK [Name]: " Präfix wenn Icon angezeigt wird
						displayText = displayText.replaceFirst("^MK [^:]+: ", "");
					}
					lines.add(new LineWithPercent(displayText, percentText, percentText != null, false, "machtkristalle", showIcon));
				}
			}
		}
		
		// Recycler Slots
		boolean showRecyclerPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerPercent;
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay) {
			String percent = null;
			if (showRecyclerPercent && recyclerSlot1.isValid()) {
				percent = calculatePercent(recyclerSlot1.current, recyclerSlot1.max);
			}
			double currentPercent = recyclerSlot1.isValid() ? 
				((double)recyclerSlot1.current / (double)recyclerSlot1.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot1.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1ShowIcon;
			String displayText = showIcon ? recyclerSlot1.getDisplayString() : "Recycler Slot 1: " + recyclerSlot1.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, showWarning, "recyclerSlot1", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay) {
			String percent = null;
			if (showRecyclerPercent && recyclerSlot2.isValid()) {
				percent = calculatePercent(recyclerSlot2.current, recyclerSlot2.max);
			}
			double currentPercent = recyclerSlot2.isValid() ? 
				((double)recyclerSlot2.current / (double)recyclerSlot2.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot2.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2ShowIcon;
			String displayText = showIcon ? recyclerSlot2.getDisplayString() : "Recycler Slot 2: " + recyclerSlot2.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, showWarning, "recyclerSlot2", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay) {
			String percent = null;
			if (showRecyclerPercent && recyclerSlot3.isValid()) {
				percent = calculatePercent(recyclerSlot3.current, recyclerSlot3.max);
			}
			double currentPercent = recyclerSlot3.isValid() ? 
				((double)recyclerSlot3.current / (double)recyclerSlot3.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot3.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3ShowIcon;
			String displayText = showIcon ? recyclerSlot3.getDisplayString() : "Recycler Slot 3: " + recyclerSlot3.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent && percent != null, showWarning, "recyclerSlot3", showIcon));
		}
		
		return lines;
	}
	
	/**
	 * Rendert das Tab-Info Overlay links auf dem Bildschirm
	 */
	private static void renderTabInfoDisplay(DrawContext context, MinecraftClient client) {
		if (client.getWindow() == null) {
			return;
		}
		
		// Prüfe ob Tab Info Utility aktiviert ist
		if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoUtilityEnabled) {
			return;
		}
		
		// Hilfsklasse für Zeilen mit optionalen Prozenten und Warnung
		
		// Sammle alle anzuzeigenden Zeilen
		List<LineWithPercent> lines = new ArrayList<>();
		
		// Forschung
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschungPercent;
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
			// Warnung: wenn Prozent UNTER dem Warnwert ist (da Forschung runter zählt)
			double currentPercent = forschung.isValid() ? 
				((double)forschung.current / (double)forschung.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungWarnPercent;
			boolean showWarning = forschung.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungShowIcon;
			String displayText = showIcon ? forschung.getDisplayString() : "Forschung: " + forschung.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "forschung", showIcon));
		}
		
		// Amboss Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmboss && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmbossPercent;
			if (showPercent && ambossKapazitaet.isValid()) {
				percent = calculatePercent(ambossKapazitaet.current, ambossKapazitaet.max);
			}
			double currentPercent = ambossKapazitaet.isValid() ? 
				((double)ambossKapazitaet.current / (double)ambossKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossWarnPercent;
			boolean showWarning = ambossKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossShowIcon;
			String displayText = showIcon ? ambossKapazitaet.getDisplayString() : "Amboss: " + ambossKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "amboss", showIcon));
		}
		
		// Schmelzofen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofenPercent;
			if (showPercent && schmelzofenKapazitaet.isValid()) {
				percent = calculatePercent(schmelzofenKapazitaet.current, schmelzofenKapazitaet.max);
			}
			double currentPercent = schmelzofenKapazitaet.isValid() ? 
				((double)schmelzofenKapazitaet.current / (double)schmelzofenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenWarnPercent;
			boolean showWarning = schmelzofenKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenShowIcon;
			String displayText = showIcon ? schmelzofenKapazitaet.getDisplayString() : "Schmelzofen: " + schmelzofenKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "schmelzofen", showIcon));
		}
		
		// Jäger Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaeger && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaegerPercent;
			if (showPercent && jaegerKapazitaet.isValid()) {
				percent = calculatePercent(jaegerKapazitaet.current, jaegerKapazitaet.max);
			}
			double currentPercent = jaegerKapazitaet.isValid() ? 
				((double)jaegerKapazitaet.current / (double)jaegerKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerWarnPercent;
			boolean showWarning = jaegerKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerShowIcon;
			String displayText = showIcon ? jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix() : "Jäger: " + jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "jaeger", showIcon));
		}
		
		// Seelen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelenPercent;
			if (showPercent && seelenKapazitaet.isValid()) {
				percent = calculatePercent(seelenKapazitaet.current, seelenKapazitaet.max);
			}
			double currentPercent = seelenKapazitaet.isValid() ? 
				((double)seelenKapazitaet.current / (double)seelenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenWarnPercent;
			boolean showWarning = seelenKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenShowIcon;
			String displayText = showIcon ? seelenKapazitaet.getDisplayString() : "Seelen: " + seelenKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "seelen", showIcon));
		}
		
		// Essenzen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenSeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzenPercent;
			if (showPercent && essenzenKapazitaet.isValid()) {
				percent = calculatePercent(essenzenKapazitaet.current, essenzenKapazitaet.max);
			}
			double currentPercent = essenzenKapazitaet.isValid() ? 
				((double)essenzenKapazitaet.current / (double)essenzenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenWarnPercent;
			boolean showWarning = essenzenKapazitaet.isValid() && warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenShowIcon;
			String displayText = showIcon ? essenzenKapazitaet.getDisplayString() : "Essenzen: " + essenzenKapazitaet.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "essenzen", showIcon));
		}
		
		// Machtkristalle
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay) {
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleShowIcon;
			for (int i = 0; i < 3; i++) {
				// Prüfe ob dieser Slot aktiviert ist
				boolean slotEnabled;
				switch (i) {
					case 0:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot1;
						break;
					case 1:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot2;
						break;
					case 2:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot3;
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
						// Entferne "MK [Name]: " Präfix wenn Icon angezeigt wird
						displayText = displayText.replaceFirst("^MK [^:]+: ", "");
					}
					lines.add(new LineWithPercent(displayText, percentText, percentText != null, false, "machtkristalle", showIcon));
				}
			}
		}
		
		// Recycler Slots
		boolean showRecyclerPercent2 = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerPercent;
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			if (showRecyclerPercent2 && recyclerSlot1.isValid()) {
				percent = calculatePercent(recyclerSlot1.current, recyclerSlot1.max);
			}
			double currentPercent = recyclerSlot1.isValid() ? 
				((double)recyclerSlot1.current / (double)recyclerSlot1.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot1.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1ShowIcon;
			String displayText = showIcon ? recyclerSlot1.getDisplayString() : "Recycler Slot 1: " + recyclerSlot1.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent2 && percent != null, showWarning, "recyclerSlot1", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			if (showRecyclerPercent2 && recyclerSlot2.isValid()) {
				percent = calculatePercent(recyclerSlot2.current, recyclerSlot2.max);
			}
			double currentPercent = recyclerSlot2.isValid() ? 
				((double)recyclerSlot2.current / (double)recyclerSlot2.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot2.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2ShowIcon;
			String displayText = showIcon ? recyclerSlot2.getDisplayString() : "Recycler Slot 2: " + recyclerSlot2.getDisplayString();
			lines.add(new LineWithPercent(displayText, percent, showRecyclerPercent2 && percent != null, showWarning, "recyclerSlot2", showIcon));
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay) {
			// Zeige immer an, auch wenn noch keine Werte gefunden wurden
			String percent = null;
			if (showRecyclerPercent2 && recyclerSlot3.isValid()) {
				percent = calculatePercent(recyclerSlot3.current, recyclerSlot3.max);
			}
			double currentPercent = recyclerSlot3.isValid() ? 
				((double)recyclerSlot3.current / (double)recyclerSlot3.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
			// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
			boolean showWarning = recyclerSlot3.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3ShowIcon;
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
			                                                   "essenzen".equals(line.configKey) || "jaeger".equals(line.configKey) || 
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
		float scale = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayScale;
		if (scale <= 0) scale = 1.0f;
		
		// Berechne skalierte Dimensionen
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);
		
		// Position aus Config: baseX ist die linke Kante (wie beim Mining-Overlay)
		int baseX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayX;
		int yPosition = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayY;
		
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
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayShowBackground) {
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
		final int LINE_HEIGHT = client.textRenderer.fontHeight + 2;
		
		// Forschung
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschung && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschungPercent;
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
			// Warnung: wenn Prozent UNTER dem Warnwert ist (da Forschung runter zählt)
			double currentPercent = forschung.isValid() ? 
				((double)forschung.current / (double)forschung.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungWarnPercent;
			boolean showWarning = forschung.isValid() && warnPercent >= 0 && currentPercent <= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungShowIcon;
			String displayText = showIcon ? forschung.getDisplayString() : "Forschung: " + forschung.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "forschung", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungShowBackground);
		}
		
		// Amboss
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmboss && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmbossPercent;
			if (showPercent && ambossKapazitaet.isValid()) {
				percent = calculatePercent(ambossKapazitaet.current, ambossKapazitaet.max);
			}
			double currentPercent = ambossKapazitaet.isValid() ? 
				((double)ambossKapazitaet.current / (double)ambossKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossShowIcon;
			String displayText = showIcon ? ambossKapazitaet.getDisplayString() : "Amboss: " + ambossKapazitaet.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "amboss", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossShowBackground);
		}
		
		// Schmelzofen
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofen && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofenPercent;
			if (showPercent && schmelzofenKapazitaet.isValid()) {
				percent = calculatePercent(schmelzofenKapazitaet.current, schmelzofenKapazitaet.max);
			}
			double currentPercent = schmelzofenKapazitaet.isValid() ? 
				((double)schmelzofenKapazitaet.current / (double)schmelzofenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenShowIcon;
			String displayText = showIcon ? schmelzofenKapazitaet.getDisplayString() : "Schmelzofen: " + schmelzofenKapazitaet.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "schmelzofen", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenShowBackground);
		}
		
		// Jäger
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaeger && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaegerPercent;
			if (showPercent && jaegerKapazitaet.isValid()) {
				percent = calculatePercent(jaegerKapazitaet.current, jaegerKapazitaet.max);
			}
			double currentPercent = jaegerKapazitaet.isValid() ? 
				((double)jaegerKapazitaet.current / (double)jaegerKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerShowIcon;
			String displayText = showIcon ? jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix() : "Jäger: " + jaegerKapazitaet.getDisplayStringWithoutCurrentSuffix();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "jaeger", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerShowBackground);
		}
		
		// Seelen
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelen && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelenPercent;
			if (showPercent && seelenKapazitaet.isValid()) {
				percent = calculatePercent(seelenKapazitaet.current, seelenKapazitaet.max);
			}
			double currentPercent = seelenKapazitaet.isValid() ? 
				((double)seelenKapazitaet.current / (double)seelenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenShowIcon;
			String displayText = showIcon ? seelenKapazitaet.getDisplayString() : "Seelen: " + seelenKapazitaet.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "seelen", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenShowBackground);
		}
		
		// Essenzen
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzen && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenSeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzenPercent;
			if (showPercent && essenzenKapazitaet.isValid()) {
				percent = calculatePercent(essenzenKapazitaet.current, essenzenKapazitaet.max);
			}
			double currentPercent = essenzenKapazitaet.isValid() ? 
				((double)essenzenKapazitaet.current / (double)essenzenKapazitaet.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenWarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenShowIcon;
			String displayText = showIcon ? essenzenKapazitaet.getDisplayString() : "Essenzen: " + essenzenKapazitaet.getDisplayString();
			renderSingleInfoOverlay(context, client, displayText, 
				percent, showPercent, showWarning, "essenzen", showIcon,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenY,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenShowBackground);
		}
		
		// Machtkristalle - prüfe ob einzeln oder zusammen gerendert werden soll
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay) {
			// Prüfe welche Slots einzeln gerendert werden sollen
			boolean slot1Separate = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Separate;
			boolean slot2Separate = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Separate;
			boolean slot3Separate = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Separate;
			boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleShowIcon;
			
			// Liste für Slots, die zusammen gerendert werden sollen
			List<LineWithPercent> mkLines = new ArrayList<>();
			int yOffset = 0; // Für einzelne Overlays
			int mkLineHeight = client.textRenderer.fontHeight + 2;
			
			for (int i = 0; i < 3; i++) {
				// Prüfe ob dieser Slot aktiviert ist
				boolean slotEnabled;
				switch (i) {
					case 0:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot1;
						break;
					case 1:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot2;
						break;
					case 2:
						slotEnabled = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalleSlot3;
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
						displayText = displayText.replaceFirst("^MK [^:]+: ", "");
					}
				}
				
				// Warnung: wenn Prozent >= dem Warnwert ist (wie bei Amboss)
				boolean showWarning = false;
				if (percentText != null && slot.xpData.isValid()) {
					double currentPercent = parsePercentValue(percentText);
					double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleWarnPercent;
					showWarning = currentPercent >= 0 && warnPercent >= 0 && currentPercent >= warnPercent;
				}
				
				if (slotSeparate) {
					// Rendere einzeln mit individueller Position
					int slotX, slotY;
					switch (i) {
						case 0:
							slotX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1X;
							slotY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Y;
							break;
						case 1:
							slotX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2X;
							slotY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Y;
							break;
						case 2:
							slotX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3X;
							slotY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Y;
							break;
						default:
							slotX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleX;
							slotY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleY;
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
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleShowBackground);
				} else {
					// Füge zur Liste hinzu (wird zusammen gerendert)
					mkLines.add(new LineWithPercent(displayText, percentText, percentText != null, showWarning, "machtkristalle", showIcon));
				}
			}
			
			// Rendere alle Zeilen, die zusammen gerendert werden sollen, in einem einzigen Overlay
			if (!mkLines.isEmpty()) {
				// Berechne Y-Position für das Multi-Line-Overlay (nach den einzelnen Overlays)
				int multiLineY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleY + yOffset;
				renderMultiLineOverlay(context, client, mkLines,
					net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleX,
					multiLineY,
					net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleShowBackground,
					"machtkristalle");
			}
		}
		
		// Recycler Slots - prüfe ob einzeln oder zusammen gerendert werden soll
		// Prüfe ob mindestens ein Recycler-Slot aktiviert ist und "Separates Overlay" hat
		boolean recyclerSlot1Active = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
		                              net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay;
		boolean recyclerSlot2Active = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
		                              net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay;
		boolean recyclerSlot3Active = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
		                              net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay;
		
		if (recyclerSlot1Active || recyclerSlot2Active || recyclerSlot3Active) {
			// Prüfe welche Slots einzeln gerendert werden sollen
			boolean slot1Separate = recyclerSlot1Active && net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Separate;
			boolean slot2Separate = recyclerSlot2Active && net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Separate;
			boolean slot3Separate = recyclerSlot3Active && net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Separate;
			
			// Liste für Slots, die zusammen gerendert werden sollen
			List<LineWithPercent> recyclerLines = new ArrayList<>();
			int yOffset = 0; // Für einzelne Overlays
			int recyclerLineHeight = client.textRenderer.fontHeight + 2;
			
			// Recycler Slot 1
			if (recyclerSlot1Active) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerPercent;
				if (showPercent && recyclerSlot1.isValid()) {
					percent = calculatePercent(recyclerSlot1.current, recyclerSlot1.max);
				}
				double currentPercent = recyclerSlot1.isValid() ? 
					((double)recyclerSlot1.current / (double)recyclerSlot1.max) * 100.0 : 0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
				// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
				boolean showWarning = warnPercent >= 0 && currentPercent <= warnPercent;
				boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1ShowIcon;
				// Wenn Icon aktiviert ist, zeige nur die Werte (oder "Nicht im Tab-Widget" wenn nicht gültig)
				// Das Icon wird dann automatisch vor dem Text angezeigt
				String displayText = showIcon ? recyclerSlot1.getDisplayString() : "Recycler Slot 1: " + recyclerSlot1.getDisplayString();
				
				if (slot1Separate) {
					// Rendere einzeln mit individueller Position
					renderSingleInfoOverlay(context, client, displayText, 
						percent, showPercent, showWarning, "recyclerSlot1", showIcon,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1X,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Y,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerShowBackground);
					yOffset += recyclerLineHeight;
				} else {
					// Füge zur Liste hinzu (wird zusammen gerendert)
					recyclerLines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "recyclerSlot1", showIcon));
				}
			}
			
			// Recycler Slot 2
			if (recyclerSlot2Active) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerPercent;
				if (showPercent && recyclerSlot2.isValid()) {
					percent = calculatePercent(recyclerSlot2.current, recyclerSlot2.max);
				}
				double currentPercent = recyclerSlot2.isValid() ? 
					((double)recyclerSlot2.current / (double)recyclerSlot2.max) * 100.0 : 0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
				// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
				boolean showWarning = warnPercent >= 0 && currentPercent <= warnPercent;
				boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2ShowIcon;
				String displayText = showIcon ? recyclerSlot2.getDisplayString() : "Recycler Slot 2: " + recyclerSlot2.getDisplayString();
				
				if (slot2Separate) {
					// Rendere einzeln mit individueller Position
					renderSingleInfoOverlay(context, client, displayText, 
						percent, showPercent, showWarning, "recyclerSlot2", showIcon,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2X,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Y,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerShowBackground);
					yOffset += recyclerLineHeight;
				} else {
					// Füge zur Liste hinzu (wird zusammen gerendert)
					recyclerLines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "recyclerSlot2", showIcon));
				}
			}
			
			// Recycler Slot 3
			if (recyclerSlot3Active) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerPercent;
				if (showPercent && recyclerSlot3.isValid()) {
					percent = calculatePercent(recyclerSlot3.current, recyclerSlot3.max);
				}
				double currentPercent = recyclerSlot3.isValid() ? 
					((double)recyclerSlot3.current / (double)recyclerSlot3.max) * 100.0 : 0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerWarnPercent;
				// Recycler zählt runter, daher warnen wenn currentPercent <= warnPercent
				boolean showWarning = warnPercent >= 0 && currentPercent <= warnPercent;
				boolean showIcon = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3ShowIcon;
				String displayText = showIcon ? recyclerSlot3.getDisplayString() : "Recycler Slot 3: " + recyclerSlot3.getDisplayString();
				
				if (slot3Separate) {
					// Rendere einzeln mit individueller Position
					renderSingleInfoOverlay(context, client, displayText, 
						percent, showPercent, showWarning, "recyclerSlot3", showIcon,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3X,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Y,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerShowBackground);
					yOffset += recyclerLineHeight;
				} else {
					// Füge zur Liste hinzu (wird zusammen gerendert)
					recyclerLines.add(new LineWithPercent(displayText, percent, showPercent, showWarning, "recyclerSlot3", showIcon));
				}
			}
			
			// Rendere alle Zeilen, die zusammen gerendert werden sollen, in einem einzigen Overlay
			if (!recyclerLines.isEmpty()) {
				// Verwende die gemeinsame Position für das Multi-Line-Overlay
				int baseX = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerX;
				int baseY = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerY;
				boolean showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerShowBackground;
				
				renderMultiLineOverlay(context, client, recyclerLines,
					baseX,
					baseY,
					showBackground,
					"recycler"); // Verwende "recycler" als configKey für Farben
			}
		}
	}
	
	/**
	 * Rendert ein einzelnes Overlay für eine Information
	 */
	private static void renderSingleInfoOverlay(DrawContext context, MinecraftClient client, 
		String text, String percentText, boolean showPercent, boolean showWarning, String configKey,
		int xPosition, int yPosition) {
		renderSingleInfoOverlay(context, client, text, percentText, showPercent, showWarning, configKey, false, xPosition, yPosition);
	}
	
	/**
	 * Rendert ein einzelnes Overlay für eine Information (mit Icon-Support)
	 */
	private static void renderSingleInfoOverlay(DrawContext context, MinecraftClient client, 
		String text, String percentText, boolean showPercent, boolean showWarning, String configKey,
		boolean showIcon, int xPosition, int yPosition) {
		// Standardmäßig Hintergrund anzeigen, wenn nicht spezifiziert
		boolean showBackground = true;
		if (configKey != null) {
			switch (configKey) {
				case "forschung":
					showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungShowBackground;
					break;
				case "amboss":
					showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossShowBackground;
					break;
				case "schmelzofen":
					showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenShowBackground;
					break;
				case "jaeger":
					showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerShowBackground;
					break;
				case "seelen":
					showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenShowBackground;
					break;
				case "essenzen":
					showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenShowBackground;
					break;
				case "machtkristalle":
				case "machtkristalleSlot1":
				case "machtkristalleSlot2":
				case "machtkristalleSlot3":
					showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleShowBackground;
					break;
				case "recycler":
				case "recyclerSlot1":
				case "recyclerSlot2":
				case "recyclerSlot3":
					showBackground = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerShowBackground;
					break;
			}
		}
		renderSingleInfoOverlay(context, client, text, percentText, showPercent, showWarning, configKey, showIcon, xPosition, yPosition, showBackground);
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
			                                                   "essenzen".equals(line.configKey) || "jaeger".equals(line.configKey) || 
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
			                                        "essenzen".equals(line.configKey) || "jaeger".equals(line.configKey) || 
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
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungScale;
			case "amboss":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossScale;
			case "schmelzofen":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenScale;
			case "jaeger":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerScale;
			case "seelen":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenScale;
			case "essenzen":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenScale;
			case "machtkristalle":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleScale;
			case "machtkristalleSlot1":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot1Scale;
			case "machtkristalleSlot2":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot2Scale;
			case "machtkristalleSlot3":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSlot3Scale;
			case "recycler":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerScale;
			case "recyclerSlot1":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Scale;
			case "recyclerSlot2":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Scale;
			case "recyclerSlot3":
				return net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Scale;
			default:
				return 1.0f;
		}
	}
	
	/**
	 * Gibt die konfigurierte Textfarbe für einen Tab-Info-Eintrag zurück
	 */
	public static int getTextColorForConfigKey(String configKey) {
		if (configKey == null) return 0xFFFFFFFF;
		java.awt.Color color;
		switch (configKey) {
			case "forschung":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungTextColor;
				break;
			case "amboss":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossTextColor;
				break;
			case "schmelzofen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenTextColor;
				break;
			case "jaeger":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerTextColor;
				break;
			case "seelen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenTextColor;
				break;
			case "essenzen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenTextColor;
				break;
			case "machtkristalle":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleTextColor;
				break;
			case "recyclerSlot1":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1TextColor;
				break;
			case "recyclerSlot2":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2TextColor;
				break;
			case "recyclerSlot3":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3TextColor;
				break;
			default:
				return 0xFFFFFFFF;
		}
		return (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
	}
	
	/**
	 * Gibt die konfigurierte Prozentfarbe für einen Tab-Info-Eintrag zurück
	 */
	public static int getPercentColorForConfigKey(String configKey) {
		if (configKey == null) return 0xFFFFFF00;
		java.awt.Color color;
		switch (configKey) {
			case "forschung":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungPercentColor;
				break;
			case "amboss":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossPercentColor;
				break;
			case "schmelzofen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenPercentColor;
				break;
			case "jaeger":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerPercentColor;
				break;
			case "seelen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenPercentColor;
				break;
			case "essenzen":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenPercentColor;
				break;
			case "machtkristalle":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristallePercentColor;
				break;
			case "recyclerSlot1":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1PercentColor;
				break;
			case "recyclerSlot2":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2PercentColor;
				break;
			case "recyclerSlot3":
				color = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3PercentColor;
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
		                                        "essenzen".equals(configKey) || "jaeger".equals(configKey) || 
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
			                                        "essenzen".equals(configKey) || "jaeger".equals(configKey) || 
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
}


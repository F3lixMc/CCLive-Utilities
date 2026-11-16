package net.felix.utilities.Overall.npcalerts.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.felix.utilities.Overall.glyphs.ZeichenUtility;
import net.felix.utilities.Overall.npcalerts.data.CapacityData;
import net.felix.utilities.Overall.npcalerts.data.XpProgress;
import net.minecraft.client.Minecraft;

import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.ambossCapacity;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.essenzenCapacity;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.forschung;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.jaegerCapacity;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.machtkristallSlots;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.machtkristalle;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.recyclerSlot1;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.recyclerSlot2;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.recyclerSlot3;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.schmelzofenCapacity;
import static net.felix.utilities.Overall.npcalerts.data.NpcAlertState.seelenCapacity;
import static net.felix.utilities.Overall.npcalerts.parser.KomboKisteReader.refreshKomboKisteFromBossbarAndScoreboard;

/**
 * Liest Forschung, Kapazitaeten, Machtkristalle und Recycler-Slots aus der Tab-Liste.
 */
public final class TabListParser {

	private TabListParser() {
	}

	public static long lastTabListCheck = 0; // Cache für Tab-Liste Checks (jede 1 Sekunde)

	/**
	 * Setzt ein CapacityData-Objekt auf "nicht gefunden" zurück
	 */
	private static void resetCapacityData(CapacityData data) {
		data.current = -1;
		data.max = -1;
		data.currentFormatted = null;
		data.maxFormatted = null;
	}

	/** Tab-Widget-Überschriften der Kapazitäten; Reihenfolge wie {@link #capacityTargets()} und {@link #CAPACITY_LABELS}. */
	private static final String[] CAPACITY_MARKERS = {
		"[Forschung]",
		"[Amboss Kapazität]",
		"[Schmelzofen Kapazität]",
		"[Jäger Kapazität]",
		"[Seelen Kapazität]",
		"[Essenzen Kapazität]",
		"[Recycler Slot 1]",
		"[Recycler Slot 2]",
		"[Recycler Slot 3]"
	};

	private static final String[] CAPACITY_LABELS = {
		"Forschung", "Amboss", "Schmelzofen", "Jäger", "Seelen", "Essenzen",
		"Recycler Slot 1", "Recycler Slot 2", "Recycler Slot 3"
	};

	private static CapacityData[] capacityTargets() {
		return new CapacityData[] {
			forschung, ambossCapacity, schmelzofenCapacity, jaegerCapacity, seelenCapacity, essenzenCapacity,
			recyclerSlot1, recyclerSlot2, recyclerSlot3
		};
	}

	/**
	 * Aktualisiert alle Informationen aus der Tab-Liste
	 */
	public static void updateNpcAlerts(Minecraft client) {
		// Nur alle 1 Sekunde prüfen, um Performance-Probleme zu vermeiden
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastTabListCheck < 1000) {
			return;
		}
		lastTabListCheck = currentTime;

		if (client == null || client.getConnection() == null) {
			return;
		}

		var playerList = client.getConnection().getOnlinePlayers();
		if (playerList == null) {
			return;
		}

		// Konvertiere zu Liste für Index-Iteration
		java.util.List<net.minecraft.client.multiplayer.PlayerInfo> entries =
			new java.util.ArrayList<>(playerList);
		java.util.function.Function<String, String> removeFormatting = TabListParser::removeFormattingCodes;
		java.util.function.Function<Integer, String> getEntryText = entryIndex -> getEntryDisplayText(entries, entryIndex);

		CapacityData[] targets = capacityTargets();
		// Track welche Werte im Tab-Widget gefunden wurden
		boolean[] found = new boolean[targets.length];

		// Durchsuche alle Einträge
		for (int i = 0; i < entries.size(); i++) {
			String entryText = getEntryText.apply(i);
			if (entryText == null) {
				continue;
			}
			String cleanEntryText = removeFormatting.apply(entryText);

			// Machtkristall-Einträge werden separat verarbeitet (siehe unten)
			for (int target = 0; target < targets.length; target++) {
				if (cleanEntryText.contains(CAPACITY_MARKERS[target])) {
					found[target] = true;
					parseCapacityData(entries, i, getEntryText, removeFormatting, targets[target], CAPACITY_LABELS[target]);
				}
			}
		}

		// Setze nur die Werte zurück, die nicht im Tab-Widget gefunden wurden
		for (int target = 0; target < targets.length; target++) {
			if (!found[target]) {
				resetCapacityData(targets[target]);
			}
		}

		// Verarbeite Machtkristalle separat (müssen in Reihenfolge geparst werden)
		parseMachtkristalle(entries, getEntryText, removeFormatting);

		refreshKomboKisteFromBossbarAndScoreboard(client);
	}

	/** Entfernt alle § Codes (Formatierungscodes) und trimmt den Text. */
	private static String removeFormattingCodes(String text) {
		if (text == null) return "";
		return text.replaceAll("§[0-9a-fk-or]", "").trim();
	}

	/** Anzeigename des Tab-Listen-Eintrags (oder Profilname); null bei ungültigem Index oder leerem Eintrag. */
	private static String getEntryDisplayText(java.util.List<net.minecraft.client.multiplayer.PlayerInfo> entries, int entryIndex) {
		if (entryIndex < 0 || entryIndex >= entries.size()) {
			return null;
		}
		var entry = entries.get(entryIndex);
		if (entry == null) {
			return null;
		}
		net.minecraft.network.chat.Component displayName = entry.getTabListDisplayName();
		if (displayName != null) {
			return displayName.getString();
		} else if (entry.getProfile() != null) {
			return entry.getProfile().name();
		}
		return null;
	}
	/**
	 * Parst Machtkristalle aus der Tab-Liste
	 * Erkennt sowohl "[Machtkristall der XXX]" als auch "[Kein Machtkristall ausgewählt]"
	 * Behält die Reihenfolge bei (Slot 1, 2, 3)
	 */
	private static void parseMachtkristalle(
		java.util.List<net.minecraft.client.multiplayer.PlayerInfo> entries,
		java.util.function.Function<Integer, String> getEntryText,
		java.util.function.Function<String, String> removeFormatting
	) {
		// Setze alle Slots zunächst auf leer
		for (int i = 0; i < 3; i++) {
			machtkristallSlots[i].name = null;
			machtkristallSlots[i].xpData = new XpProgress();
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
					parseXpData(entries, i, getEntryText, removeFormatting, 
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
				machtkristallSlots[i].xpData = new XpProgress(); // current = -1, required = -1
			}
		} else {
			// Wenn Machtkristall-Einträge gefunden wurden, markiere die restlichen Slots als "nicht gefunden"
			// (Slots, die nicht in der Tab-Liste stehen)
			for (int i = currentSlot; i < 3; i++) {
				// Diese Slots wurden nicht in der Tab-Liste gefunden
				machtkristallSlots[i].name = ""; // Leerer String signalisiert "nicht gefunden"
				machtkristallSlots[i].xpData = new XpProgress(); // current = -1, required = -1
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
		java.util.List<net.minecraft.client.multiplayer.PlayerInfo> entries,
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
	private static String getTablistEntryText(net.minecraft.client.multiplayer.PlayerInfo entry) {
		if (entry == null) {
			return null;
		}
		net.minecraft.network.chat.Component displayName = entry.getTabListDisplayName();
		if (displayName != null) {
			StringBuilder sb = new StringBuilder();
			displayName.visit((style, asString) -> {
				sb.append(asString);
				return java.util.Optional.empty();
			}, net.minecraft.network.chat.Style.EMPTY);
			return sb.toString();
		}
		if (entry.getProfile() != null) {
			return entry.getProfile().name();
		}
		return null;
	}

	/**
	 * Parst XP-Daten (Aktuell / Benötigt) aus der Tab-Liste
	 * Sucht nach dem Header und dann nach der Datenzeile darunter
	 */
	private static void parseXpData(
		java.util.List<net.minecraft.client.multiplayer.PlayerInfo> entries,
		int headerIndex,
		java.util.function.Function<Integer, String> getEntryText,
		java.util.function.Function<String, String> removeFormatting,
		XpProgress data,
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
}

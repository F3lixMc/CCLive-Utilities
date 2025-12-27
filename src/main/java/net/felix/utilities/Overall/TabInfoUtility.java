package net.felix.utilities.Overall;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

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
			if (!isValid()) return "? / ?";
			// Verwende formatierte Strings wenn verfügbar, sonst rohe Zahlen
			if (currentFormatted != null && maxFormatted != null) {
				return currentFormatted + " / " + maxFormatted;
			}
			return current + " / " + max;
		}
	}
	
	public static class XPData {
		public int current = -1;
		public int required = -1;
		public String currentFormatted = null; // Formatierter String (z.B. "1,234")
		public String requiredFormatted = null; // Formatierter String (z.B. "5,000")
		
		public boolean isValid() {
			return current >= 0 && required >= 0;
		}
		
		public String getDisplayString() {
			if (!isValid()) return "? / ?";
			// Verwende formatierte Strings wenn verfügbar, sonst rohe Zahlen
			if (currentFormatted != null && requiredFormatted != null) {
				return currentFormatted + " / " + requiredFormatted;
			}
			return current + " / " + required;
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
	
	// Machtkristalle (3 verschiedene, unterschieden durch Namen)
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
		
		// Durchsuche alle Einträge
		for (int i = 0; i < entries.size(); i++) {
			String entryText = getEntryText.apply(i);
			if (entryText == null) {
				continue;
			}
			
			String cleanEntryText = removeFormatting.apply(entryText);
			
			// [Forschung]
			if (cleanEntryText.contains("[Forschung]")) {
				parseCapacityData(entries, i, getEntryText, removeFormatting, forschung, "Forschung");
			}
			
			// [Amboss Kapazität]
			if (cleanEntryText.contains("[Amboss Kapazität]")) {
				parseCapacityData(entries, i, getEntryText, removeFormatting, ambossKapazitaet, "Amboss");
			}
			
			// [Schmelzofen Kapazität]
			if (cleanEntryText.contains("[Schmelzofen Kapazität]")) {
				parseCapacityData(entries, i, getEntryText, removeFormatting, schmelzofenKapazitaet, "Schmelzofen");
			}
			
			// [Jäger Kapazität]
			if (cleanEntryText.contains("[Jäger Kapazität]")) {
				parseCapacityData(entries, i, getEntryText, removeFormatting, jaegerKapazitaet, "Jäger");
			}
			
			// [Seelen Kapazität]
			if (cleanEntryText.contains("[Seelen Kapazität]")) {
				parseCapacityData(entries, i, getEntryText, removeFormatting, seelenKapazitaet, "Seelen");
			}
			
			// [Essenzen Kapazität]
			if (cleanEntryText.contains("[Essenzen Kapazität]")) {
				parseCapacityData(entries, i, getEntryText, removeFormatting, essenzenKapazitaet, "Essenzen");
			}
			
			// [Machtkristall der XXX]
			if (cleanEntryText.contains("[Machtkristall der ")) {
				// Extrahiere den Namen des Machtkristalls
				String kristallName = extractMachtkristallName(cleanEntryText);
				if (kristallName != null && !kristallName.isEmpty()) {
					// Erstelle oder hole XPData für diesen Machtkristall
					XPData xpData = machtkristalle.computeIfAbsent(kristallName, k -> new XPData());
					parseXPData(entries, i, getEntryText, removeFormatting, xpData, "Machtkristall");
				}
			}
			
			// [Recycler Slot 1]
			if (cleanEntryText.contains("[Recycler Slot 1]")) {
				parseCapacityData(entries, i, getEntryText, removeFormatting, recyclerSlot1, "Recycler Slot 1");
			}
			
			// [Recycler Slot 2]
			if (cleanEntryText.contains("[Recycler Slot 2]")) {
				parseCapacityData(entries, i, getEntryText, removeFormatting, recyclerSlot2, "Recycler Slot 2");
			}
			
			// [Recycler Slot 3]
			if (cleanEntryText.contains("[Recycler Slot 3]")) {
				parseCapacityData(entries, i, getEntryText, removeFormatting, recyclerSlot3, "Recycler Slot 3");
			}
		}
	}
	
	/**
	 * Extrahiert den Namen des Machtkristalls aus dem Text
	 * Format: "[Machtkristall der XXX]"
	 */
	private static String extractMachtkristallName(String text) {
		try {
			Pattern pattern = Pattern.compile("\\[Machtkristall der (.+)\\]", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(text);
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
						// Entferne alles außer Zahlen, Punkten, Kommas für Parsing
						String currentNumbersOnly = currentPart.replaceAll("[^0-9.,]", "").replaceAll("[.,]", "");
						String maxNumbersOnly = maxPart.replaceAll("[^0-9.,]", "").replaceAll("[.,]", "");
						
						// Versuche, Buchstaben wie "K", "M" zu konvertieren
						currentNumbersOnly = convertSuffixToNumber(currentPart, currentNumbersOnly);
						maxNumbersOnly = convertSuffixToNumber(maxPart, maxNumbersOnly);
						
						if (!currentNumbersOnly.isEmpty() && !maxNumbersOnly.isEmpty()) {
							try {
								data.current = Integer.parseInt(currentNumbersOnly);
								data.max = Integer.parseInt(maxNumbersOnly);
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
	 */
	private static String convertSuffixToNumber(String original, String numbersOnly) {
		if (original == null || original.isEmpty()) {
			return numbersOnly;
		}
		
		String upper = original.toUpperCase();
		if (upper.contains("K")) {
			// Tausend
			try {
				double value = Double.parseDouble(numbersOnly);
				return String.valueOf((int)(value * 1000));
			} catch (NumberFormatException e) {
				return numbersOnly;
			}
		} else if (upper.contains("M")) {
			// Million
			try {
				double value = Double.parseDouble(numbersOnly);
				return String.valueOf((int)(value * 1000000));
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
				// Format: "AKTUELL / BENÖTIGT" oder "AKTUELL/BENÖTIGT"
				// Beispiel: "1,234 / 5,000" oder "1234/5000"
				try {
					// Teile durch "/" auf
					String[] parts = cleanDataText.split("/", 2);
					if (parts.length == 2) {
						String currentPart = parts[0].trim();
						String requiredPart = parts[1].trim();
						
						// Speichere formatierte Strings
						data.currentFormatted = currentPart;
						data.requiredFormatted = requiredPart;
						
						// Extrahiere auch rohe Zahlen für isValid() Prüfung
						// Entferne alles außer Zahlen, Punkten, Kommas für Parsing
						String currentNumbersOnly = currentPart.replaceAll("[^0-9.,]", "").replaceAll("[.,]", "");
						String requiredNumbersOnly = requiredPart.replaceAll("[^0-9.,]", "").replaceAll("[.,]", "");
						
						// Versuche, Buchstaben wie "K", "M" zu konvertieren
						currentNumbersOnly = convertSuffixToNumber(currentPart, currentNumbersOnly);
						requiredNumbersOnly = convertSuffixToNumber(requiredPart, requiredNumbersOnly);
						
						if (!currentNumbersOnly.isEmpty() && !requiredNumbersOnly.isEmpty()) {
							try {
								data.current = Integer.parseInt(currentNumbersOnly);
								data.required = Integer.parseInt(requiredNumbersOnly);
								return; // Erfolgreich geparst
							} catch (NumberFormatException e) {
								// Wenn Parsing fehlschlägt, setze trotzdem die formatierten Strings
								// und verwende -1 für isValid() Prüfung (wird dann als ungültig angezeigt)
								data.current = -1;
								data.required = -1;
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
		data.required = -1;
		data.currentFormatted = null;
		data.requiredFormatted = null;
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
		
		// Render nur wenn Overlays sichtbar sind
		if (!showOverlays) {
			return;
		}
		
		renderTabInfoDisplay(context, client);
	}
	
	/**
	 * Gibt die Anzahl der Machtkristalle zurück (für DraggableOverlay)
	 */
	public static int getMachtkristallCount() {
		return machtkristalle.size();
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
			count += machtkristalle.size();
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
		class LineWithPercent {
			String text;
			String percentText;
			boolean showPercent;
			boolean showWarning; // Zeigt an, ob Warnung angezeigt werden soll
			String configKey; // Für Warn-Prozentwert-Prüfung
			
			LineWithPercent(String text, String percentText, boolean showPercent, boolean showWarning, String configKey) {
				this.text = text;
				this.percentText = percentText;
				this.showPercent = showPercent;
				this.showWarning = showWarning;
				this.configKey = configKey;
			}
		}
		
		// Sammle alle anzuzeigenden Zeilen
		List<LineWithPercent> lines = new ArrayList<>();
		
		// Forschung
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschung && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungSeparateOverlay) {
			if (forschung.isValid()) {
				lines.add(new LineWithPercent("Forschung: " + forschung.getDisplayString(), null, false, false, null));
			} else if (forschung.current != -1 || forschung.max != -1) {
				lines.add(new LineWithPercent("Forschung: " + forschung.getDisplayString() + " (Debug)", null, false, false, null));
			}
		}
		
		// Amboss Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmboss && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossSeparateOverlay) {
			if (ambossKapazitaet.isValid()) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoAmbossPercent;
				if (showPercent) {
					percent = calculatePercent(ambossKapazitaet.current, ambossKapazitaet.max);
				}
				double currentPercent = ((double)ambossKapazitaet.current / (double)ambossKapazitaet.max) * 100.0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossWarnPercent;
				boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
				lines.add(new LineWithPercent("Amboss: " + ambossKapazitaet.getDisplayString(), percent, showPercent, showWarning, "amboss"));
			} else if (ambossKapazitaet.current != -1 || ambossKapazitaet.max != -1) {
				lines.add(new LineWithPercent("Amboss: " + ambossKapazitaet.getDisplayString() + " (Debug)", null, false, false, null));
			}
		}
		
		// Schmelzofen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenSeparateOverlay) {
			if (schmelzofenKapazitaet.isValid()) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSchmelzofenPercent;
				if (showPercent) {
					percent = calculatePercent(schmelzofenKapazitaet.current, schmelzofenKapazitaet.max);
				}
				double currentPercent = ((double)schmelzofenKapazitaet.current / (double)schmelzofenKapazitaet.max) * 100.0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenWarnPercent;
				boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
				lines.add(new LineWithPercent("Schmelzofen: " + schmelzofenKapazitaet.getDisplayString(), percent, showPercent, showWarning, "schmelzofen"));
			} else if (schmelzofenKapazitaet.current != -1 || schmelzofenKapazitaet.max != -1) {
				lines.add(new LineWithPercent("Schmelzofen: " + schmelzofenKapazitaet.getDisplayString() + " (Debug)", null, false, false, null));
			}
		}
		
		// Jäger Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaeger && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerSeparateOverlay) {
			if (jaegerKapazitaet.isValid()) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoJaegerPercent;
				if (showPercent) {
					percent = calculatePercent(jaegerKapazitaet.current, jaegerKapazitaet.max);
				}
				double currentPercent = ((double)jaegerKapazitaet.current / (double)jaegerKapazitaet.max) * 100.0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerWarnPercent;
				boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
				lines.add(new LineWithPercent("Jäger: " + jaegerKapazitaet.getDisplayString(), percent, showPercent, showWarning, "jaeger"));
			} else if (jaegerKapazitaet.current != -1 || jaegerKapazitaet.max != -1) {
				lines.add(new LineWithPercent("Jäger: " + jaegerKapazitaet.getDisplayString() + " (Debug)", null, false, false, null));
			}
		}
		
		// Seelen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenSeparateOverlay) {
			if (seelenKapazitaet.isValid()) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoSeelenPercent;
				if (showPercent) {
					percent = calculatePercent(seelenKapazitaet.current, seelenKapazitaet.max);
				}
				double currentPercent = ((double)seelenKapazitaet.current / (double)seelenKapazitaet.max) * 100.0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenWarnPercent;
				boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
				lines.add(new LineWithPercent("Seelen: " + seelenKapazitaet.getDisplayString(), percent, showPercent, showWarning, "seelen"));
			} else if (seelenKapazitaet.current != -1 || seelenKapazitaet.max != -1) {
				lines.add(new LineWithPercent("Seelen: " + seelenKapazitaet.getDisplayString() + " (Debug)", null, false, false, null));
			}
		}
		
		// Essenzen Kapazität
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzen && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenSeparateOverlay) {
			if (essenzenKapazitaet.isValid()) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoEssenzenPercent;
				if (showPercent) {
					percent = calculatePercent(essenzenKapazitaet.current, essenzenKapazitaet.max);
				}
				double currentPercent = ((double)essenzenKapazitaet.current / (double)essenzenKapazitaet.max) * 100.0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenWarnPercent;
				boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
				lines.add(new LineWithPercent("Essenzen: " + essenzenKapazitaet.getDisplayString(), percent, showPercent, showWarning, "essenzen"));
			} else if (essenzenKapazitaet.current != -1 || essenzenKapazitaet.max != -1) {
				lines.add(new LineWithPercent("Essenzen: " + essenzenKapazitaet.getDisplayString() + " (Debug)", null, false, false, null));
			}
		}
		
		// Machtkristalle
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay) {
			for (Map.Entry<String, XPData> entry : machtkristalle.entrySet()) {
				if (entry.getValue().isValid()) {
					lines.add(new LineWithPercent("Machtkristall " + entry.getKey() + ": " + entry.getValue().getDisplayString(), null, false, false, null));
				} else if (entry.getValue().current != -1 || entry.getValue().required != -1) {
					lines.add(new LineWithPercent("Machtkristall " + entry.getKey() + ": " + entry.getValue().getDisplayString() + " (Debug)", null, false, false, null));
				}
			}
		}
		
		// Recycler Slots
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay) {
			if (recyclerSlot1.isValid()) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1Percent;
				if (showPercent) {
					percent = calculatePercent(recyclerSlot1.current, recyclerSlot1.max);
				}
				double currentPercent = ((double)recyclerSlot1.current / (double)recyclerSlot1.max) * 100.0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1WarnPercent;
				boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
				lines.add(new LineWithPercent("Recycler Slot 1: " + recyclerSlot1.getDisplayString(), percent, showPercent, showWarning, "recyclerSlot1"));
			} else if (recyclerSlot1.current != -1 || recyclerSlot1.max != -1) {
				lines.add(new LineWithPercent("Recycler Slot 1: " + recyclerSlot1.getDisplayString() + " (Debug)", null, false, false, null));
			}
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay) {
			if (recyclerSlot2.isValid()) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2Percent;
				if (showPercent) {
					percent = calculatePercent(recyclerSlot2.current, recyclerSlot2.max);
				}
				double currentPercent = ((double)recyclerSlot2.current / (double)recyclerSlot2.max) * 100.0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2WarnPercent;
				boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
				lines.add(new LineWithPercent("Recycler Slot 2: " + recyclerSlot2.getDisplayString(), percent, showPercent, showWarning, "recyclerSlot2"));
			} else if (recyclerSlot2.current != -1 || recyclerSlot2.max != -1) {
				lines.add(new LineWithPercent("Recycler Slot 2: " + recyclerSlot2.getDisplayString() + " (Debug)", null, false, false, null));
			}
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
		    !net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay) {
			if (recyclerSlot3.isValid()) {
				String percent = null;
				boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3Percent;
				if (showPercent) {
					percent = calculatePercent(recyclerSlot3.current, recyclerSlot3.max);
				}
				double currentPercent = ((double)recyclerSlot3.current / (double)recyclerSlot3.max) * 100.0;
				double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3WarnPercent;
				boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
				lines.add(new LineWithPercent("Recycler Slot 3: " + recyclerSlot3.getDisplayString(), percent, showPercent, showWarning, "recyclerSlot3"));
			} else if (recyclerSlot3.current != -1 || recyclerSlot3.max != -1) {
				lines.add(new LineWithPercent("Recycler Slot 3: " + recyclerSlot3.getDisplayString() + " (Debug)", null, false, false, null));
			}
		}
		
		// Wenn keine Daten vorhanden sind, zeige Debug-Info
		if (lines.isEmpty()) {
			lines.add(new LineWithPercent("Keine Daten gefunden", null, false, false, null));
			lines.add(new LineWithPercent("(Warte auf Tab-Liste...)", null, false, false, null));
		}
		
		// Berechne die maximale Breite des Textes (inklusive Prozente und Warnung direkt danach)
		int maxWidth = 0;
		for (LineWithPercent line : lines) {
			int width = client.textRenderer.getWidth(line.text);
			if (line.showPercent && line.percentText != null) {
				width += client.textRenderer.getWidth(" " + line.percentText); // Abstand + Prozente
			}
			if (line.showWarning) {
				width += client.textRenderer.getWidth(" !"); // Abstand + Ausrufezeichen
			}
			if (width > maxWidth) {
				maxWidth = width;
			}
		}
		
		// Padding
		final int PADDING = 5;
		final int LINE_HEIGHT = client.textRenderer.fontHeight + 2; // Verwende tatsächliche Schrift-Höhe
		
		// Berechne Overlay-Dimensionen
		int overlayWidth = maxWidth + (PADDING * 2);
		int overlayHeight = (lines.size() * LINE_HEIGHT) + (PADDING * 2);
		
		// Position aus Config (wenn nicht gesetzt, Standard links oben)
		int xPosition = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayX;
		int yPosition = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMainOverlayY;
		
		// Zeichne semi-transparenten Hintergrund
		context.fill(xPosition, yPosition, xPosition + overlayWidth, yPosition + overlayHeight, 0x80000000);
		
		// Zeichne alle Zeilen
		int currentY = yPosition + PADDING;
		int textColor = 0xFFFFFFFF; // Weiß mit vollem Alpha
		int percentColor = 0xFFFFFF00; // Gelb für Prozente
		
		// Test: Zeige immer mindestens eine Zeile, um zu sehen ob Rendering funktioniert
		if (lines.isEmpty()) {
			lines.add(new LineWithPercent("Test: Overlay funktioniert!", null, false, false, null));
		}
		
		for (LineWithPercent line : lines) {
			// Prüfe, ob die Zeile nicht leer ist
			if (line.text == null || line.text.trim().isEmpty()) {
				currentY += LINE_HEIGHT;
				continue;
			}
			
			// Zeichne Haupttext
			Text textComponent = Text.literal(line.text);
			
			try {
				// Zeichne Haupttext
				context.drawText(
					client.textRenderer,
					textComponent,
					xPosition + PADDING,
					currentY,
					textColor,
					true // Mit Schatten für bessere Lesbarkeit
				);
				
				// Zeichne Prozente in gelb direkt nach dem Text, wenn vorhanden
				int currentX = xPosition + PADDING + client.textRenderer.getWidth(line.text);
				if (line.showPercent && line.percentText != null) {
					Text percentComponent = Text.literal(" " + line.percentText);
					context.drawText(
						client.textRenderer,
						percentComponent,
						currentX,
						currentY,
						percentColor,
						true // Mit Schatten für bessere Lesbarkeit
					);
					currentX += client.textRenderer.getWidth(" " + line.percentText);
				}
				
				// Zeichne blinkendes rotes Ausrufezeichen, wenn Warnung aktiv ist
				if (line.showWarning) {
					// Blink-Animation: alle 300ms wechseln (schneller)
					boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
					if (isVisible) {
						int warningColor = 0xFFFF0000; // Rot
						Text warningComponent = Text.literal(" !");
						context.drawText(
							client.textRenderer,
							warningComponent,
							currentX,
							currentY,
							warningColor,
							true // Mit Schatten für bessere Lesbarkeit
						);
					}
				}
			} catch (Exception e) {
				// Fallback: Versuche mit String direkt
				try {
					context.drawText(
						client.textRenderer,
						line.text,
						xPosition + PADDING,
						currentY,
						textColor,
						true
					);
					
					// Zeichne Prozente in gelb direkt nach dem Text, wenn vorhanden
					int currentX = xPosition + PADDING + client.textRenderer.getWidth(line.text);
					if (line.showPercent && line.percentText != null) {
						context.drawText(
							client.textRenderer,
							" " + line.percentText,
							currentX,
							currentY,
							percentColor,
							true
						);
						currentX += client.textRenderer.getWidth(" " + line.percentText);
					}
					
					// Zeichne blinkendes rotes Ausrufezeichen, wenn Warnung aktiv ist
					if (line.showWarning) {
						// Blink-Animation: alle 300ms wechseln (schneller)
						boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
						if (isVisible) {
							int warningColor = 0xFFFF0000; // Rot
							context.drawText(
								client.textRenderer,
								" !",
								currentX,
								currentY,
								warningColor,
								true
							);
						}
					}
				} catch (Exception e2) {
					// Ignoriere Fehler
				}
			}
			currentY += LINE_HEIGHT;
		}
		
		// Rendere separate Overlays für Informationen, die diese Option aktiviert haben
		renderSeparateOverlays(context, client);
	}
	
	/**
	 * Rendert separate Overlays für einzelne Informationen
	 */
	private static void renderSeparateOverlays(DrawContext context, MinecraftClient client) {
		final int PADDING = 5;
		final int LINE_HEIGHT = client.textRenderer.fontHeight + 2;
		int textColor = 0xFFFFFFFF;
		int percentColor = 0xFFFFFF00;
		
		// Forschung
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoForschung && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungSeparateOverlay) {
			renderSingleInfoOverlay(context, client, "Forschung: " + forschung.getDisplayString(), 
				null, false, false, null,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoForschungY);
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
			renderSingleInfoOverlay(context, client, "Amboss: " + ambossKapazitaet.getDisplayString(), 
				percent, showPercent, showWarning, "amboss",
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoAmbossY);
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
			renderSingleInfoOverlay(context, client, "Schmelzofen: " + schmelzofenKapazitaet.getDisplayString(), 
				percent, showPercent, showWarning, "schmelzofen",
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSchmelzofenY);
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
			renderSingleInfoOverlay(context, client, "Jäger: " + jaegerKapazitaet.getDisplayString(), 
				percent, showPercent, showWarning, "jaeger",
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoJaegerY);
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
			renderSingleInfoOverlay(context, client, "Seelen: " + seelenKapazitaet.getDisplayString(), 
				percent, showPercent, showWarning, "seelen",
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoSeelenY);
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
			renderSingleInfoOverlay(context, client, "Essenzen: " + essenzenKapazitaet.getDisplayString(), 
				percent, showPercent, showWarning, "essenzen",
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenX,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoEssenzenY);
		}
		
		// Machtkristalle
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoMachtkristalle && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleSeparateOverlay) {
			int yOffset = 0;
			for (Map.Entry<String, XPData> entry : machtkristalle.entrySet()) {
				if (entry.getValue().isValid()) {
					renderSingleInfoOverlay(context, client, 
						"Machtkristall " + entry.getKey() + ": " + entry.getValue().getDisplayString(), 
						null, false, false, null,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleX,
						net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoMachtkristalleY + yOffset);
					yOffset += LINE_HEIGHT;
				}
			}
		}
		
		// Recycler Slots
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1 && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1SeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot1Percent;
			if (showPercent && recyclerSlot1.isValid()) {
				percent = calculatePercent(recyclerSlot1.current, recyclerSlot1.max);
			}
			double currentPercent = recyclerSlot1.isValid() ? 
				((double)recyclerSlot1.current / (double)recyclerSlot1.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1WarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			renderSingleInfoOverlay(context, client, "Recycler Slot 1: " + recyclerSlot1.getDisplayString(), 
				percent, showPercent, showWarning, "recyclerSlot1",
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1X,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot1Y);
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2 && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2SeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot2Percent;
			if (showPercent && recyclerSlot2.isValid()) {
				percent = calculatePercent(recyclerSlot2.current, recyclerSlot2.max);
			}
			double currentPercent = recyclerSlot2.isValid() ? 
				((double)recyclerSlot2.current / (double)recyclerSlot2.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2WarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			renderSingleInfoOverlay(context, client, "Recycler Slot 2: " + recyclerSlot2.getDisplayString(), 
				percent, showPercent, showWarning, "recyclerSlot2",
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2X,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot2Y);
		}
		if (net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3 && 
		    net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3SeparateOverlay) {
			String percent = null;
			boolean showPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showTabInfoRecyclerSlot3Percent;
			if (showPercent && recyclerSlot3.isValid()) {
				percent = calculatePercent(recyclerSlot3.current, recyclerSlot3.max);
			}
			double currentPercent = recyclerSlot3.isValid() ? 
				((double)recyclerSlot3.current / (double)recyclerSlot3.max) * 100.0 : 0;
			double warnPercent = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3WarnPercent;
			boolean showWarning = warnPercent >= 0 && currentPercent >= warnPercent;
			renderSingleInfoOverlay(context, client, "Recycler Slot 3: " + recyclerSlot3.getDisplayString(), 
				percent, showPercent, showWarning, "recyclerSlot3",
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3X,
				net.felix.CCLiveUtilitiesConfig.HANDLER.instance().tabInfoRecyclerSlot3Y);
		}
	}
	
	/**
	 * Rendert ein einzelnes Overlay für eine Information
	 */
	private static void renderSingleInfoOverlay(DrawContext context, MinecraftClient client, 
		String text, String percentText, boolean showPercent, boolean showWarning, String configKey,
		int xPosition, int yPosition) {
		final int PADDING = 5;
		final int LINE_HEIGHT = client.textRenderer.fontHeight + 2;
		int textColor = 0xFFFFFFFF;
		int percentColor = 0xFFFFFF00;
		
		// Berechne Breite
		int width = client.textRenderer.getWidth(text);
		if (showPercent && percentText != null) {
			width += client.textRenderer.getWidth(" " + percentText);
		}
		if (showWarning) {
			width += client.textRenderer.getWidth(" !");
		}
		int overlayWidth = width + (PADDING * 2);
		int overlayHeight = LINE_HEIGHT + (PADDING * 2);
		
		// Zeichne Hintergrund
		context.fill(xPosition, yPosition, xPosition + overlayWidth, yPosition + overlayHeight, 0x80000000);
		
		// Zeichne Text
		int currentX = xPosition + PADDING;
		int currentY = yPosition + PADDING;
		
		try {
			context.drawText(client.textRenderer, Text.literal(text), currentX, currentY, textColor, true);
			currentX += client.textRenderer.getWidth(text);
			
			// Prozente
			if (showPercent && percentText != null) {
				Text percentComponent = Text.literal(" " + percentText);
				context.drawText(client.textRenderer, percentComponent, currentX, currentY, percentColor, true);
				currentX += client.textRenderer.getWidth(" " + percentText);
			}
			
			// Warnung
			if (showWarning) {
				boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
				if (isVisible) {
					int warningColor = 0xFFFF0000;
					Text warningComponent = Text.literal(" !");
					context.drawText(client.textRenderer, warningComponent, currentX, currentY, warningColor, true);
				}
			}
		} catch (Exception e) {
			// Fallback
			try {
				context.drawText(client.textRenderer, text, currentX, currentY, textColor, true);
				currentX += client.textRenderer.getWidth(text);
				if (showPercent && percentText != null) {
					context.drawText(client.textRenderer, " " + percentText, currentX, currentY, percentColor, true);
					currentX += client.textRenderer.getWidth(" " + percentText);
				}
				if (showWarning) {
					boolean isVisible = (System.currentTimeMillis() / 300) % 2 == 0;
					if (isVisible) {
						int warningColor = 0xFFFF0000;
						context.drawText(client.textRenderer, " !", currentX, currentY, warningColor, true);
					}
				}
			} catch (Exception e2) {
				// Ignoriere Fehler
			}
		}
	}
}


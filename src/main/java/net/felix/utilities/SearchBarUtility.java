package net.felix.utilities;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.felix.CCLiveUtilitiesConfig;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SearchBarUtility {
	
	private static boolean isSearchBarVisible = false;
	private static String searchText = "";
	private static boolean isSearchBarFocused = false;
	private static int cursorPosition = 0;
	private static int selectionStart = -1; // Start der Text-Selektion (-1 = keine Selektion)
	private static int selectionEnd = -1;   // Ende der Text-Selektion
	private static int selectionStart = 0;
	private static int selectionEnd = 0;
	private static int searchBarX = 0;
	private static int searchBarY = 0;
	private static int searchBarWidth = 200;
	private static int searchBarHeight = 20;
	private static long cursorBlinkTime = 0;
	private static boolean cursorVisible = true;
	
	// Hilfe-Button Variablen
	private static int helpButtonX = 0;
	private static int helpButtonY = 0;
	private static int helpButtonSize = 16;
	private static boolean helpScreenOpen = false;
	
	// @-Button Variablen (für MacBook-Nutzer)
	private static int atButtonX = 0;
	private static int atButtonY = 0;
	private static int atButtonSize = 16;
	
	private static Set<Integer> matchingSlots = new HashSet<>();
	private static final int SLOT_SIZE = 16;
	
	// Inventar-Überwachung
	private static List<ItemStack> previousInventory = new ArrayList<>();
	
	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(SearchBarUtility::onClientTick);
	}
	
	private static void onClientTick(MinecraftClient client) {
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().searchBarEnabled || 
			!CCLiveUtilitiesConfig.HANDLER.instance().showSearchBar) {
			isSearchBarVisible = false;
			clearSearchBar();
			return;
		}
		
		if (client.player == null || client.currentScreen == null) {
			isSearchBarVisible = false;
			clearSearchBar();
			return;
		}

		if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
			String title = handledScreen.getTitle().getString();
			
			// Entferne Farbcodes für den Vergleich
			String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "");
			
			if (cleanTitle.contains("㭦") || cleanTitle.contains("Bauplan") || cleanTitle.contains("Baupläne") ||
				cleanTitle.contains("Umschmieden") || cleanTitle.contains("Zerlegen") || 
				cleanTitle.contains("Ausrüstung") || cleanTitle.contains("Essenz") || 
				cleanTitle.contains("Essenz-Tasche") || cleanTitle.contains("CACTUS_CLICKER.CACTUS_CLICKER") ||
				cleanTitle.contains("Runen [Baupläne]") || cleanTitle.contains("Werkzeug Sammlung") ||
				cleanTitle.contains("Waffen Sammlung") || cleanTitle.contains("Rüstungs Sammlung") ||
				cleanTitle.contains("Favorisierte [Rüstungsbaupläne]") || cleanTitle.contains("Favorisierte [Waffenbaupläne]") ||
				cleanTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools")) {
				isSearchBarVisible = true;
				
				// Überprüfe Inventaränderungen
				checkInventoryChanges(handledScreen);
				
				// Cursor blinken lassen
				if (isSearchBarFocused) {
					long currentTime = System.currentTimeMillis();
					if (currentTime - cursorBlinkTime > 500) { // 500ms = 0.5 Sekunden
						cursorVisible = !cursorVisible;
						cursorBlinkTime = currentTime;
					}
				} else {
					cursorVisible = false;
				}
			} else {
				isSearchBarVisible = false;
				clearSearchBar();
			}
		} else {
			isSearchBarVisible = false;
			clearSearchBar();
		}
	}
	
	/**
	 * Überprüft ob eine Text-Selektion aktiv ist
	 */
	private static boolean hasSelection() {
		return selectionStart >= 0 && selectionEnd > selectionStart;
	}
	
	/**
	 * Löscht die aktuelle Text-Selektion
	 */
	private static void clearSelection() {
		selectionStart = -1;
		selectionEnd = -1;
	}
	
	/**
	 * Überprüft ob sich das Inventar geändert hat und führt bei Änderungen eine neue Suche durch
	 */
	private static void checkInventoryChanges(HandledScreen<?> screen) {
		List<ItemStack> currentInventory = new ArrayList<>();
		
		// Bestimme die zu überprüfenden Slots basierend auf dem Menütyp
		int[] slotsToCheck = getSlotsForMenu(screen);
		
		for (int slot : slotsToCheck) {
			if (slot < screen.getScreenHandler().slots.size()) {
				Slot slotObj = screen.getScreenHandler().slots.get(slot);
				currentInventory.add(slotObj.getStack());
			}
		}
		
		// Vergleiche mit vorherigem Inventar
		if (!inventoryEquals(previousInventory, currentInventory)) {
			previousInventory = new ArrayList<>(currentInventory);
			
			// Führe neue Suche durch wenn Suchtext vorhanden ist
			if (!searchText.isEmpty()) {
				performSearch();
			}
		}
	}
	
	/**
	 * Bestimmt die zu überprüfenden Slots basierend auf dem Menütyp
	 */
	private static int[] getSlotsForMenu(HandledScreen<?> screen) {
		String title = screen.getTitle().getString();
		String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "");
		
		if (cleanTitle.contains("㭦")) {
			// Spezielle Slots für Kartenmenü
			return new int[]{ 0, 1, 2, 3, 5, 6, 7, 9, 10, 11, 12, 14, 15, 16, 18, 19, 20, 21, 23, 24, 25, 27, 28, 29, 30, 32, 33, 34};
		} else if (cleanTitle.contains("Umschmieden") || cleanTitle.contains("CACTUS_CLICKER.CACTUS_CLICKER")) {
			// Slots für Umschmieden und Zerlegen (umfassender Bereich)
			return new int[]{ 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
		} else if (cleanTitle.contains("Runen [Baupläne]")) {
			return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
		} else if (cleanTitle.contains("Ausrüstung") || cleanTitle.contains("Zerlegen")) {
			// Slots für Ausrüstungsmenü (umfassender Bereich)
			return new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
		} else if (cleanTitle.contains("Essenz")) {
			// Slots für Essenz-Menüs (umfassender Bereich)
			return new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43};
		} else if (cleanTitle.contains("Werkzeug Sammlung") || cleanTitle.contains("Waffen Sammlung") || cleanTitle.contains("Rüstungs Sammlung")) {
			// Slots für Sammlungs-Menüs (umfassender Bereich)
			return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
		} else {
			// Standard-Slots für Bauplan-Menüs und andere
			return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
		}
	}
	
	/**
	 * Vergleicht zwei Inventare auf Änderungen
	 */
	private static boolean inventoryEquals(List<ItemStack> inv1, List<ItemStack> inv2) {
		if (inv1.size() != inv2.size()) {
			return false;
		}
		
		for (int i = 0; i < inv1.size(); i++) {
			ItemStack stack1 = inv1.get(i);
			ItemStack stack2 = inv2.get(i);
			
			// Prüfe ob Items gleich sind (gleicher Typ, gleiche Anzahl, gleiche NBT)
			if (!ItemStack.areEqual(stack1, stack2)) {
				return false;
			}
		}
		
		return true;
	}
	

	
	public static void renderInScreen(DrawContext context, HandledScreen<?> screen, int screenX, int screenY) {
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().searchBarEnabled || 
			!CCLiveUtilitiesConfig.HANDLER.instance().showSearchBar) {
			return;
		}
		
		if (!isSearchBarVisible) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}

		searchBarX = screenX + (176 - searchBarWidth) / 2;
		searchBarY = screenY + 166 + 65;
		
		// Hilfe-Button Position (links neben der Suchleiste)
		helpButtonX = searchBarX - helpButtonSize - 5;
		helpButtonY = searchBarY + 2;
		
		// @-Button Position (rechts neben der Suchleiste)
		atButtonX = searchBarX + searchBarWidth + 5;
		atButtonY = searchBarY + 2;

		// Hilfe-Button zeichnen
		drawHelpButton(context, client);
		
		// @-Button zeichnen
		drawAtButton(context, client);
		
		// Hintergrund nur wenn aktiviert
		if (CCLiveUtilitiesConfig.HANDLER.instance().searchBarShowBackground) {
			context.fill(searchBarX, searchBarY, searchBarX + searchBarWidth, searchBarY + searchBarHeight, 0x80000000);
		}
		
		// Rahmen
		context.fill(searchBarX, searchBarY, searchBarX + searchBarWidth, searchBarY + 1, 0xFFFFFFFF);
		context.fill(searchBarX, searchBarY + searchBarHeight - 1, searchBarX + searchBarWidth, searchBarY + searchBarHeight, 0xFFFFFFFF);
		context.fill(searchBarX, searchBarY, searchBarX + 1, searchBarY + searchBarHeight, 0xFFFFFFFF);
		context.fill(searchBarX + searchBarWidth - 1, searchBarY, searchBarX + searchBarWidth, searchBarY + searchBarHeight, 0xFFFFFFFF);

		// Text mit Scrolling
		String displayText = searchText.isEmpty() ? "Suche..." : searchText;
		int textColor = searchText.isEmpty() ? 0x80808080 : 0xFFFFFFFF; // Weißer Text wie gewohnt
		
		// Berechne Text-Position mit Scrolling
		int textX = searchBarX + 5;
		int availableWidth = searchBarWidth - 10; // 5px Padding auf jeder Seite
		
		if (!searchText.isEmpty()) {
			int textWidth = client.textRenderer.getWidth(displayText);
			
			if (textWidth > availableWidth) {
				// Text ist zu lang - scrolle nach rechts
				int cursorX = client.textRenderer.getWidth(searchText.substring(0, cursorPosition));
				int scrollOffset = Math.max(0, cursorX - availableWidth + 20); // 20px Abstand vom rechten Rand
				
				// Zeichne nur den sichtbaren Teil des Texts
				String visibleText = getVisibleText(displayText, scrollOffset, availableWidth, client.textRenderer);
				drawTextWithSelection(context, client, visibleText, textX, searchBarY + 6, textColor, scrollOffset);
			} else {

				// Text passt - zeichne normal
				drawTextWithSelection(context, client, displayText, textX, searchBarY + 6, textColor, 0);
				// Text passt - zeichne normal mit Auswahl-Highlight
				if (hasSelection() && isSearchBarFocused) {
					renderTextWithSelection(context, client, displayText, textX, searchBarY + 6, textColor);
				} else {
					context.drawText(
						client.textRenderer,
						displayText,
						textX,
						searchBarY + 6,
						textColor,
						true
					);
				}
			}
		} else {
			// Placeholder-Text
			context.drawText(
				client.textRenderer,
				displayText,
				textX,
				searchBarY + 6,
				textColor,
				true
			);
		}

		// Cursor mit Scrolling
		if (isSearchBarFocused && cursorVisible) {
			int cursorX;
			
			if (searchText.isEmpty()) {
				// Cursor am Anfang wenn Text leer ist
				cursorX = searchBarX + 5;
			} else {
				// Cursor an der aktuellen Position
				cursorX = searchBarX + 5 + client.textRenderer.getWidth(searchText.substring(0, cursorPosition));
				int textWidth = client.textRenderer.getWidth(searchText);
				
				if (textWidth > availableWidth) {
					// Cursor-Position anpassen für Scrolling
					int scrollOffset = Math.max(0, cursorX - searchBarX - 5 - availableWidth + 20);
					cursorX = searchBarX + 5 + client.textRenderer.getWidth(searchText.substring(0, cursorPosition)) - scrollOffset;
				}
			}
			
			// Stelle sicher, dass der Cursor im sichtbaren Bereich ist
			if (cursorX >= searchBarX + 5 && cursorX <= searchBarX + searchBarWidth - 5) {
				context.fill(cursorX, searchBarY + 3, cursorX + 1, searchBarY + searchBarHeight - 3, 0xFFFFFFFF);
			}
		}


		
		// Hilfe-Screen zeichnen wenn geöffnet
		if (helpScreenOpen) {
			drawHelpScreen(context, client);
		}
	}
	
	/**
	 * Zeichnet Text mit Selektion (Minecraft-typischer Negativ-Effekt)
	 */
	private static void drawTextWithSelection(DrawContext context, MinecraftClient client, String text, int x, int y, int textColor, int scrollOffset) {
		if (!hasSelection()) {
			// Keine Selektion - zeichne normal
			context.drawText(client.textRenderer, text, x, y, textColor, true);
			return;
		}
		
		// Berechne die sichtbaren Selektion-Grenzen
		int visibleSelectionStart = Math.max(0, selectionStart - scrollOffset);
		int visibleSelectionEnd = Math.min(text.length(), selectionEnd - scrollOffset);
		
		if (visibleSelectionStart >= text.length() || visibleSelectionEnd <= 0) {
			// Selektion ist nicht sichtbar - zeichne normal
			context.drawText(client.textRenderer, text, x, y, textColor, true);
			return;
		}
		
		// ZUERST: Zeichne den weißen Hintergrund für die Selektion (80% Deckkraft)
		if (visibleSelectionEnd > visibleSelectionStart) {
			String selectedText = text.substring(visibleSelectionStart, visibleSelectionEnd);
			int selectionX = x + client.textRenderer.getWidth(text.substring(0, visibleSelectionStart));
			int selectionWidth = client.textRenderer.getWidth(selectedText);
			
			// Weißer Hintergrund mit 80% Deckkraft
			context.fill(selectionX, y - 1, selectionX + selectionWidth, y + 9, 0xCCFFFFFF); // 0xCC = 80% Deckkraft
		}
		
		// DANN: Zeichne den gesamten Text normal darüber
		context.drawText(client.textRenderer, text, x, y, textColor, true);
		
		// ZULETZT: Zeichne den selektierten Text in einer anderen Farbe über dem normalen Text
		if (visibleSelectionEnd > visibleSelectionStart) {
			String selectedText = text.substring(visibleSelectionStart, visibleSelectionEnd);
			int selectionX = x + client.textRenderer.getWidth(text.substring(0, visibleSelectionStart));
			
			// Dunkelblauer Text für besseren Kontrast auf dem weißen Hintergrund
			context.drawText(client.textRenderer, selectedText, selectionX, y, 0x000080, false); // Dunkelblau für besseren Kontrast
		}
	}
	
	// Hilfsmethode für Text-Scrolling
	private static String getVisibleText(String text, int scrollOffset, int availableWidth, net.minecraft.client.font.TextRenderer textRenderer) {
		if (scrollOffset <= 0) {
			// Kein Scrolling nötig
			return text;
		}
		
		// Finde die Position im Text, die dem Scroll-Offset entspricht
		int charIndex = 0;
		int currentWidth = 0;
		
		for (int i = 0; i < text.length(); i++) {
			int charWidth = textRenderer.getWidth(String.valueOf(text.charAt(i)));
			if (currentWidth + charWidth > scrollOffset) {
				charIndex = i;
				break;
			}
			currentWidth += charWidth;
		}
		
		// Extrahiere den sichtbaren Teil des Texts
		String visibleText = text.substring(charIndex);
		
		// Kürze den Text, wenn er immer noch zu lang ist
		while (textRenderer.getWidth(visibleText) > availableWidth && visibleText.length() > 1) {
			visibleText = visibleText.substring(0, visibleText.length() - 1);
		}
		
		return visibleText;
	}
	

	

	
	public static void setSearchText(String text) {
		searchText = text;
		cursorPosition = text.length();
		performSearch();
	}
	
	private static void performSearch() {
		if (searchText.isEmpty()) {
			matchingSlots.clear();
			return;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen == null || !(client.currentScreen instanceof HandledScreen<?> handledScreen)) {
			matchingSlots.clear();
			return;
		}
		
		matchingSlots.clear();
		
		// Prüfe ob es eine @-Suche ist (ein oder mehrere @)
		boolean isAtSearch = searchText.startsWith("@");
		String searchTerm;
		
		if (isAtSearch) {
			// Entferne nur das erste @ und führende Leerzeichen
			searchTerm = searchText.substring(1).trim();
			if (searchTerm.isEmpty()) {
				return;
			}
		} else {
			// Normale Suche
			searchTerm = searchText;
		}
		
		String searchLower = searchTerm.toLowerCase();
		String searchExact = searchTerm;
		
		// Prüfe auf mehrere Filter (durch Komma getrennt)
		List<SearchFilter> filters = parseMultipleFilters(searchTerm);
		
		// Verwende die gleichen Slots wie bei der Inventarüberwachung
		int[] slotsToSearch = getSlotsForMenu(handledScreen);
		
		for (int slot : slotsToSearch) {
			if (slot < handledScreen.getScreenHandler().slots.size()) {
				Slot slotObj = handledScreen.getScreenHandler().slots.get(slot);
				ItemStack itemStack = slotObj.getStack();
				
				if (!itemStack.isEmpty()) {
					boolean matches = false;
					
					if (isAtSearch) {
						// @-Suche: Prüfe alle Filter
						if (!filters.isEmpty()) {
							matches = checkAllFilters(itemStack, filters);
						} else {
							// Fallback: Normale @-Suche
							matches = searchInItemTooltips(itemStack, searchExact, searchLower);
						}
					} else {
						// Normale Suche: Nur Item-Name
						String itemName = itemStack.getName().getString();
						String itemNameLower = itemName.toLowerCase();
						matches = itemName.contains(searchExact) || itemNameLower.contains(searchLower);
					}
					
					if (matches) {
						matchingSlots.add(slot);
					}
				}
			}
		}
	}
	
	// Hilfsklasse für Suchfilter
	private static class SearchFilter {
		enum Type {
			COMPARISON,  // >, <, =
			TEXT         // Text-Suche
		}
		
		Type type;
		String attribute;
		String operator;
		double value;
		String text;
		
		// Konstruktor für Vergleichsoperatoren
		SearchFilter(String attribute, String operator, double value) {
			this.type = Type.COMPARISON;
			this.attribute = attribute;
			this.operator = operator;
			this.value = value;
		}
		
		// Konstruktor für Text-Suche
		SearchFilter(String text) {
			this.type = Type.TEXT;
			this.text = text;
		}
	}
	
	// Parst mehrere Filter aus dem Suchtext
	private static List<SearchFilter> parseMultipleFilters(String searchTerm) {
		List<SearchFilter> filters = new ArrayList<>();
		
		// Teile durch Kommas auf
		String[] parts = searchTerm.split(",");
		
		for (String part : parts) {
			part = part.trim();
			if (part.isEmpty()) continue;
			
			// Prüfe ob der Teil mit @ beginnt
			boolean isAtFilter = part.startsWith("@");
			String filterText = isAtFilter ? part.substring(1).trim() : part;
			
			if (filterText.isEmpty()) continue;
			
			// Prüfe ob es ein Vergleichsoperator ist
			ComparisonOperator comparison = parseComparisonOperator(filterText);
			if (comparison != null) {
				filters.add(new SearchFilter(comparison.attribute, comparison.operator, comparison.value));
			} else {
				// Normale Text-Suche (immer als @-Suche behandeln wenn ursprünglich @ vorhanden war)
				filters.add(new SearchFilter(filterText));
			}
		}
		
		return filters;
	}
	
	// Prüft alle Filter für ein Item
	private static boolean checkAllFilters(ItemStack itemStack, List<SearchFilter> filters) {
		for (SearchFilter filter : filters) {
			if (!checkSingleFilter(itemStack, filter)) {
				return false; // Ein Filter schlägt fehl = Item wird nicht angezeigt
			}
		}
		return true; // Alle Filter erfolgreich
	}
	
	// Prüft einen einzelnen Filter
	private static boolean checkSingleFilter(ItemStack itemStack, SearchFilter filter) {
		switch (filter.type) {
			case COMPARISON:
				return searchWithComparison(itemStack, new ComparisonOperator(filter.attribute, filter.operator, filter.value));
			case TEXT:
				// Alle Text-Filter werden als @-Suche behandelt (in Tooltips suchen)
				return searchInItemTooltips(itemStack, filter.text, filter.text.toLowerCase());
			default:
				return false;
		}
	}
	
	// Hilfsklasse für Vergleichsoperatoren
	private static class ComparisonOperator {
		String attribute;
		String operator;
		double value;
		
		ComparisonOperator(String attribute, String operator, double value) {
			this.attribute = attribute;
			this.operator = operator;
			this.value = value;
		}
	}
	
	// Parst Vergleichsoperatoren aus dem Suchtext
	private static ComparisonOperator parseComparisonOperator(String searchTerm) {
		// Suche nach >, <, = in der Suchanfrage
		if (searchTerm.contains(">")) {
			String[] parts = searchTerm.split(">", 2);
			if (parts.length == 2) {
				try {
					double value = Double.parseDouble(parts[1].trim());
					return new ComparisonOperator(parts[0].trim(), ">", value);
				} catch (NumberFormatException e) {
					// Ignoriere ungültige Zahlen
				}
			}
		} else if (searchTerm.contains("<")) {
			String[] parts = searchTerm.split("<", 2);
			if (parts.length == 2) {
				try {
					double value = Double.parseDouble(parts[1].trim());
					return new ComparisonOperator(parts[0].trim(), "<", value);
				} catch (NumberFormatException e) {
					// Ignoriere ungültige Zahlen
				}
			}
		} else if (searchTerm.contains("=")) {
			String[] parts = searchTerm.split("=", 2);
			if (parts.length == 2) {
				try {
					double value = Double.parseDouble(parts[1].trim());
					return new ComparisonOperator(parts[0].trim(), "=", value);
				} catch (NumberFormatException e) {
					// Ignoriere ungültige Zahlen
				}
			}
		}
		return null;
	}
	
	// Sucht mit Vergleichsoperatoren
	private static boolean searchWithComparison(ItemStack itemStack, ComparisonOperator comparison) {
		try {
			// Suche in Lore-Komponente nach dem Attribut
			var loreComponent = itemStack.get(DataComponentTypes.LORE);
			if (loreComponent != null) {
				List<Text> lore = loreComponent.lines();
				for (Text loreText : lore) {
					String loreString = extractTextWithColors(loreText);
					String loreLower = loreString.toLowerCase();
					
					// Prüfe ob das Attribut in der Lore enthalten ist
					if (loreLower.contains(comparison.attribute.toLowerCase())) {
						// Extrahiere numerische Werte aus der Zeile
						double extractedValue = extractNumericValue(loreString);
						if (extractedValue != -1) {
							// Führe Vergleich durch
							switch (comparison.operator) {
								case ">":
									return extractedValue > comparison.value;
								case "<":
									return extractedValue < comparison.value;
								case "=":
									return Math.abs(extractedValue - comparison.value) < 0.01; // Toleranz für Fließkommazahlen
							}
						}
					}
				}
			}
			
			// Suche auch im Item-Namen
			String itemName = itemStack.getName().getString();
			String itemNameLower = itemName.toLowerCase();
			if (itemNameLower.contains(comparison.attribute.toLowerCase())) {
				double extractedValue = extractNumericValue(itemName);
				if (extractedValue != -1) {
					switch (comparison.operator) {
						case ">":
							return extractedValue > comparison.value;
						case "<":
							return extractedValue < comparison.value;
						case "=":
							return Math.abs(extractedValue - comparison.value) < 0.01;
					}
				}
			}
			
		} catch (Exception e) {
			// Fallback: Ignoriere Fehler
		}
		return false;
	}
	
	// Extrahiert numerische Werte aus einem String
	private static double extractNumericValue(String text) {
		// Entferne Farbcodes und Formatierung
		String cleanText = text.replaceAll("§[0-9a-fk-or]", "").trim();
		
		// Suche nach Zahlen (inkl. Dezimalzahlen)
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)");
		java.util.regex.Matcher matcher = pattern.matcher(cleanText);
		
		if (matcher.find()) {
			try {
				return Double.parseDouble(matcher.group(1));
			} catch (NumberFormatException e) {
				// Ignoriere ungültige Zahlen
			}
		}
		return -1; // Keine Zahl gefunden
	}
	
	private static boolean searchInItemTooltips(ItemStack itemStack, String searchExact, String searchLower) {
		try {
			// Suche im Item-Namen
			String itemName = itemStack.getName().getString();
			String itemNameLower = itemName.toLowerCase();
			if (itemName.contains(searchExact) || itemNameLower.contains(searchLower)) {
				return true;
			}
			
			// Suche in Lore-Komponente
			var loreComponent = itemStack.get(DataComponentTypes.LORE);
			if (loreComponent != null) {
				List<Text> lore = loreComponent.lines();
				for (Text loreText : lore) {
					String loreString = extractTextWithColors(loreText);
					String loreLower = loreString.toLowerCase();
					
					if (loreString.contains(searchExact) || loreLower.contains(searchLower)) {
						return true;
					}
				}
			}
			
			// Suche in anderen Komponenten
			String customName = extractCustomName(itemStack);
			if (customName != null) {
				String customNameLower = customName.toLowerCase();
				if (customName.contains(searchExact) || customNameLower.contains(searchLower)) {
					return true;
				}
			}
			
		} catch (Exception e) {
			// Fallback: Nur Item-Name durchsuchen
			String itemName = itemStack.getName().getString();
			String itemNameLower = itemName.toLowerCase();
			return itemName.contains(searchExact) || itemNameLower.contains(searchLower);
		}
		
		return false;
	}
	
	private static String extractTextWithColors(Text text) {
		if (text == null) return "";
		
		StringBuilder result = new StringBuilder();
		extractTextColor(text, result);
		return result.toString();
	}
	
	private static void extractTextColor(Text text, StringBuilder result) {
		if (text == null) return;
		
		// Füge den Text hinzu
		result.append(text.getString());
		
		// Verarbeite Kinder-Texts
		for (Text sibling : text.getSiblings()) {
			extractTextColor(sibling, result);
		}
	}
	
	private static String extractCustomName(ItemStack itemStack) {
		try {
			Text customName = itemStack.get(DataComponentTypes.CUSTOM_NAME);
			if (customName != null) {
				return extractTextWithColors(customName);
			}
		} catch (Exception e) {
			// Ignoriere Fehler
		}
		return null;
	}
	
	public static void setFocused(boolean focused) {
		isSearchBarFocused = focused;
	}
	
	public static boolean isVisible() {
		return isSearchBarVisible;
	}
	
	public static boolean isHelpScreenOpen() {
		return helpScreenOpen;
	}
	
	public static String getSearchText() {
		return searchText;
	}
	
	private static void clearSearchBar() {
		searchText = "";
		cursorPosition = 0;
		selectionStart = 0;
		selectionEnd = 0;
		isSearchBarFocused = false;
		matchingSlots.clear();
		cursorVisible = false;
		helpScreenOpen = false;
		
		// Inventar-Tracking zurücksetzen
		previousInventory.clear();
	}
	
	/**
	 * Prüft ob Text markiert ist
	 */
	private static boolean hasSelection() {
		return selectionStart != selectionEnd;
	}
	
	/**
	 * Löscht die aktuelle Textauswahl
	 */
	private static void clearSelection() {
		selectionStart = 0;
		selectionEnd = 0;
	}
	
	/**
	 * Fügt ein Zeichen ein und ersetzt dabei markierten Text falls vorhanden
	 */
	private static void insertCharacter(char character) {
		if (hasSelection()) {
			// Ersetze markierten Text
			int start = Math.min(selectionStart, selectionEnd);
			int end = Math.max(selectionStart, selectionEnd);
			searchText = searchText.substring(0, start) + character + searchText.substring(end);
			cursorPosition = start + 1;
			clearSelection();
		} else {
			// Füge an Cursor-Position ein
			searchText = searchText.substring(0, cursorPosition) + character + searchText.substring(cursorPosition);
			cursorPosition++;
		}
		performSearch();
	}
	
	/**
	 * Rendert Text mit Auswahl-Highlighting
	 */
	private static void renderTextWithSelection(DrawContext context, MinecraftClient client, String text, int x, int y, int textColor) {
		int start = Math.min(selectionStart, selectionEnd);
		int end = Math.max(selectionStart, selectionEnd);
		
		// Zeichne Text vor der Auswahl
		if (start > 0) {
			String beforeSelection = text.substring(0, start);
			context.drawText(client.textRenderer, beforeSelection, x, y, textColor, true);
		}
		
		// Berechne Position für die Auswahl
		int selectionX = x + client.textRenderer.getWidth(text.substring(0, start));
		String selectedText = text.substring(start, end);
		int selectionWidth = client.textRenderer.getWidth(selectedText);
		
		// Zeichne Auswahl-Hintergrund
		context.fill(selectionX, y - 1, selectionX + selectionWidth, y + 9, 0xFF0078D4); // Blauer Auswahl-Hintergrund
		
		// Zeichne ausgewählten Text (weiß auf blau)
		context.drawText(client.textRenderer, selectedText, selectionX, y, 0xFFFFFFFF, true);
		
		// Zeichne Text nach der Auswahl
		if (end < text.length()) {
			String afterSelection = text.substring(end);
			int afterX = selectionX + selectionWidth;
			context.drawText(client.textRenderer, afterSelection, afterX, y, textColor, true);
		}
	}
	
	public static boolean handleMouseClick(double mouseX, double mouseY, int button) {
		if (!isSearchBarVisible) {
			return false;
		}
		
		// Hilfe-Button Klick prüfen
		if (mouseX >= helpButtonX && mouseX <= helpButtonX + helpButtonSize &&
			mouseY >= helpButtonY && mouseY <= helpButtonY + helpButtonSize) {
			
			if (button == 0) {
				helpScreenOpen = !helpScreenOpen;
				return true;
			}
		}
		
		// @-Button Klick prüfen
		if (mouseX >= atButtonX && mouseX <= atButtonX + atButtonSize &&
			mouseY >= atButtonY && mouseY <= atButtonY + atButtonSize) {
			
			if (button == 0) {
				// @-Symbol in die Suchleiste einfügen
				searchText = searchText.substring(0, cursorPosition) + "@" + searchText.substring(cursorPosition);
				cursorPosition++;
				
				// Selektion löschen falls vorhanden
				clearSelection();
				
				// Suche durchführen
				performSearch();
				
				return true;
			}
		}
		
		// Hilfe-Screen Schließen-Button prüfen
		if (helpScreenOpen) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null) {
				int screenWidth = client.getWindow().getScaledWidth();
				int screenHeight = client.getWindow().getScaledHeight();
				int boxWidth = Math.min(350, screenWidth - 40);
				int boxHeight = Math.min(400, screenHeight - 40);
				int boxX = (screenWidth - boxWidth) / 2;
				int boxY = (screenHeight - boxHeight) / 2;
				int closeButtonX = boxX + boxWidth - 20;
				int closeButtonY = boxY + 10;
				
				if (mouseX >= closeButtonX && mouseX <= closeButtonX + 10 &&
					mouseY >= closeButtonY && mouseY <= closeButtonY + 10) {
					
					if (button == 0) {
						helpScreenOpen = false;
						return true;
					}
				}
			}
		}
		
		// Hilfe-Screen schließen wenn außerhalb geklickt wird
		if (helpScreenOpen) {
			helpScreenOpen = false;
			return false;
		}
		
		if (mouseX >= searchBarX && mouseX <= searchBarX + searchBarWidth &&
			mouseY >= searchBarY && mouseY <= searchBarY + searchBarHeight) {
			
			if (button == 0) {
				isSearchBarFocused = true;
				clearSelection(); // Auswahl löschen beim Klick
				
				// Cursor-Position basierend auf Mausklick berechnen
				MinecraftClient client = MinecraftClient.getInstance();
				if (client != null) {
					int clickX = (int) (mouseX - searchBarX - 5); // 5px Padding
					cursorPosition = calculateCursorPosition(clickX, searchText, client.textRenderer);
				} else {
					cursorPosition = searchText.length();
				}
				
				// Cursor beim ersten Klick sichtbar machen und Blink-Timer starten
				cursorVisible = true;
				cursorBlinkTime = System.currentTimeMillis();
				
				// Selektion löschen bei Klick
				clearSelection();
				return true;
			}
		} else {
			isSearchBarFocused = false;
		}
		
		return false;
	}
	
	public static boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
		// Hilfe-Screen ESC behandeln
		if (helpScreenOpen) {
			if (keyCode == 256) { // ESC
				helpScreenOpen = false;
				return true;
			}
		}
		
		if (!isSearchBarFocused) {
			return false;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return false;
		}
		
		// Cursor sichtbar machen bei Tastendruck
		cursorVisible = true;
		cursorBlinkTime = System.currentTimeMillis();
		
		// ESC-Taste
		if (keyCode == 256) {
			isSearchBarFocused = false;
			return true;
		}
		
		// Enter-Taste
		if (keyCode == 257) {
			performSearch();
		return true;
		}
		
		// Strg-Funktionen
		if (modifiers == 2) { // 2 = Strg
			switch (keyCode) {
				case 67: // Strg+C - Kopieren
					if (hasSelection()) {
						// Kopiere markierten Text
						String selectedText = searchText.substring(Math.min(selectionStart, selectionEnd), Math.max(selectionStart, selectionEnd));
						client.keyboard.setClipboard(selectedText);
					} else if (!searchText.isEmpty()) {
						// Kopiere gesamten Text wenn nichts markiert ist
						client.keyboard.setClipboard(searchText);
					}
					return true;
				case 86: // Strg+V - Einfügen
					String clipboardText = client.keyboard.getClipboard();
					if (clipboardText != null && !clipboardText.isEmpty()) {
						if (hasSelection()) {
							// Ersetze markierten Text
							int start = Math.min(selectionStart, selectionEnd);
							int end = Math.max(selectionStart, selectionEnd);
							searchText = searchText.substring(0, start) + clipboardText + searchText.substring(end);
							cursorPosition = start + clipboardText.length();
							clearSelection();
						} else {
							// Füge an Cursor-Position ein
							searchText = searchText.substring(0, cursorPosition) + clipboardText + searchText.substring(cursorPosition);
							cursorPosition += clipboardText.length();
						}
						performSearch();
					}
					return true;
				case 65: // Strg+A - Alles markieren
					if (!searchText.isEmpty()) {
						selectionStart = 0;
						selectionEnd = searchText.length();
						cursorPosition = searchText.length();
					}
					return true;
			}
		}
		
		// Backspace-Taste
		if (keyCode == 259) {

			if (cursorPosition > 0) {
				// Wenn eine Selektion aktiv ist, lösche den selektierten Text
				if (hasSelection()) {
					searchText = searchText.substring(0, selectionStart) + searchText.substring(selectionEnd);
					cursorPosition = selectionStart;
					clearSelection();

			if (hasSelection()) {
				// Lösche markierten Text
				int start = Math.min(selectionStart, selectionEnd);
				int end = Math.max(selectionStart, selectionEnd);
				searchText = searchText.substring(0, start) + searchText.substring(end);
				cursorPosition = start;
				clearSelection();
				performSearch();
			} else if (cursorPosition > 0) {
				// Strg+Backspace: Lösche ganzes Wort
				if (modifiers == 2) { // 2 = Strg
					int wordStart = findWordStart(searchText, cursorPosition);
					searchText = searchText.substring(0, wordStart) + searchText.substring(cursorPosition);
					cursorPosition = wordStart;

				} else {
					// Normaler Backspace: Lösche ein Zeichen
					searchText = searchText.substring(0, cursorPosition - 1) + searchText.substring(cursorPosition);
					cursorPosition--;
				}
				performSearch();
			}
			return true;
		}
		



		// Delete-Taste
		if (keyCode == 261) {
			if (hasSelection()) {
				// Lösche markierten Text
				int start = Math.min(selectionStart, selectionEnd);
				int end = Math.max(selectionStart, selectionEnd);
				searchText = searchText.substring(0, start) + searchText.substring(end);
				cursorPosition = start;
				clearSelection();
				performSearch();
			} else if (cursorPosition < searchText.length()) {
				// Delete: Lösche Zeichen nach dem Cursor
				searchText = searchText.substring(0, cursorPosition) + searchText.substring(cursorPosition + 1);
				performSearch();
			}
			return true;
		}

		
		// Pfeiltasten
		if (keyCode == 263) {
			clearSelection(); // Auswahl löschen
			if (cursorPosition > 0) {
				cursorPosition--;
			}
			return true;
		}
		
		if (keyCode == 262) {
			clearSelection(); // Auswahl löschen
			if (cursorPosition < searchText.length()) {
				cursorPosition++;
			}
			return true;
		}
		

		

		
		// Leertaste
		if (keyCode == 32) {

			// Wenn eine Selektion aktiv ist, ersetze den selektierten Text
			if (hasSelection()) {
				searchText = searchText.substring(0, selectionStart) + " " + searchText.substring(selectionEnd);
				cursorPosition = selectionStart + 1;
				clearSelection();
			} else {
				searchText = searchText.substring(0, cursorPosition) + " " + searchText.substring(cursorPosition);
				cursorPosition++;
			}
			performSearch();

			insertCharacter(' ');

			return true;
		}
		
		// Einfache Sonderzeichen
		if (keyCode == 188) {
			insertCharacter(',');
			return true;
		}
		
		if (keyCode == 190) {
			insertCharacter('.');
			return true;
		}
		
		// Alternative KeyCodes für QWERTZ-Layout
		if (keyCode == 44) { // Alternative für Komma
			insertCharacter(',');
			return true;
		}
		
		if (keyCode == 46) { // Alternative für Punkt
			insertCharacter('.');
			return true;
		}
		
		if (keyCode == 189) {
			insertCharacter('-');
			return true;
		}
		
		if (keyCode == 187) {
			insertCharacter('+');
			return true;
		}
		
		if (keyCode == 186) {
			insertCharacter(';');
			return true;
		}
		
		if (keyCode == 222) {
			insertCharacter('"');
			return true;
		}
		
		if (keyCode == 192) {
			insertCharacter('`');
			return true;
		}
		
		// AltGr-Kombinationen
		boolean handled = false;
		if (modifiers == 6) {
			switch (keyCode) {
				case 56:
					insertCharacter('[');
					handled = true;
					break;
				case 57:
					insertCharacter(']');
					handled = true;
					break;
				case 55:
					insertCharacter('{');
					handled = true;
					break;
				case 48:
					insertCharacter('}');
					handled = true;
					break;
				case 81:
					insertCharacter('@');
					handled = true;
					break;
			}
		}
		
		// Shift-Kombinationen
		if (modifiers == 1) {
			switch (keyCode) {
				case 55:
					insertCharacter('/');
					handled = true;
					break;
				case 56:
					insertCharacter('(');
					handled = true;
					break;
				case 57:
					insertCharacter(')');
					handled = true;
					break;
				case 48:
					insertCharacter('=');
					handled = true;
					break;
				case 220:
					insertCharacter('|');
					handled = true;
					break;
			}
		}
		
		if (keyCode == 53 && modifiers == 1) {
			insertCharacter('%');
			return true;
		}
		
		// Separate Sonderzeichen-Tasten
		if (keyCode == 92) {
			insertCharacter('#');
			return true;
		}
		
		if (keyCode == 93) {
			insertCharacter('+');
			return true;
		}
		
		if (keyCode == 47) {
			insertCharacter('-');
			return true;
		}
		
		if (keyCode == 162 && modifiers == 0) {
			insertCharacter('<');
			return true;
		}
		
		if (keyCode == 162 && modifiers == 1) {
			insertCharacter('>');
			return true;
		}
		
		// Separate ß-Taste
		if (keyCode == 45) {
			insertCharacter('ß');
			return true;
		}
		
		// Separate Umlaut-Tasten
		if (keyCode == 39) { // ä
			insertCharacter('ä');
			return true;
		}
		
		if (keyCode == 59) { // ö
			insertCharacter('ö');
			return true;
		}
		
		if (keyCode == 91) { // ü
			insertCharacter('ü');
			return true;
		}
		
		// Numpad-Tasten
		if (keyCode == 334) {
			insertCharacter('+');
			return true;
		}
		
		if (keyCode == 333) {
			insertCharacter('-');
			return true;
		}
		
		// Numpad-Zahlen 0-9
		if (keyCode == 320) {
			insertCharacter('0');
			return true;
		}
		
		if (keyCode >= 321 && keyCode <= 329) {
			char number = (char) ('0' + (keyCode - 321 + 1));
			insertCharacter(number);
			return true;
		}

		if (handled) {
			return true;
		}

		// Zahlen 0-9
		if (modifiers == 0 && keyCode >= 48 && keyCode <= 57) {
			char number = (char) keyCode;

			
			// Wenn eine Selektion aktiv ist, ersetze den selektierten Text
			if (hasSelection()) {
				searchText = searchText.substring(0, selectionStart) + number + searchText.substring(selectionEnd);
				cursorPosition = selectionStart + 1;
				clearSelection();
			} else {
				searchText = searchText.substring(0, cursorPosition) + number + searchText.substring(cursorPosition);
				cursorPosition++;
			}
			
			performSearch();

			insertCharacter(number);

			return true;
		}

		// Buchstaben A-Z (QWERTZ Layout)
		if (keyCode >= 65 && keyCode <= 90) {
			char letter;
			
			// QWERTZ Layout: Y und Z tauschen
			if (keyCode == 89) {
				letter = modifiers == 1 ? 'Z' : 'z';
			} else if (keyCode == 90) {
				letter = modifiers == 1 ? 'Y' : 'y';
			} else {
				// Normale Buchstaben: Großbuchstaben bei Shift, Kleinbuchstaben ohne Shift
				letter = modifiers == 1 ? (char) keyCode : (char) (keyCode + 32);
			}
			

			// Wenn eine Selektion aktiv ist, ersetze den selektierten Text
			if (hasSelection()) {
				searchText = searchText.substring(0, selectionStart) + letter + searchText.substring(selectionEnd);
				cursorPosition = selectionStart + 1;
				clearSelection();
			} else {
				searchText = searchText.substring(0, cursorPosition) + letter + searchText.substring(cursorPosition);
				cursorPosition++;
			}
			
			performSearch();

			insertCharacter(letter);

			return true;
		}

		return false;
	}
	
	/**
	 * Zeichnet den Hilfe-Button
	 */
	private static void drawHelpButton(DrawContext context, MinecraftClient client) {
		// Button-Hintergrund
		context.fill(helpButtonX, helpButtonY, helpButtonX + helpButtonSize, helpButtonY + helpButtonSize, 0x80000000);
		
		// Button-Rahmen
		context.fill(helpButtonX, helpButtonY, helpButtonX + helpButtonSize, helpButtonY + 1, 0xFFFFFFFF);
		context.fill(helpButtonX, helpButtonY + helpButtonSize - 1, helpButtonX + helpButtonSize, helpButtonY + helpButtonSize, 0xFFFFFFFF);
		context.fill(helpButtonX, helpButtonY, helpButtonX + 1, helpButtonY + helpButtonSize, 0xFFFFFFFF);
		context.fill(helpButtonX + helpButtonSize - 1, helpButtonY, helpButtonX + helpButtonSize, helpButtonY + helpButtonSize, 0xFFFFFFFF);
		
		// Fragezeichen-Zeichen
		context.drawText(
			client.textRenderer,
			"?",
			helpButtonX + 5,
			helpButtonY + 3,
			0xFFFFFFFF,
			true
		);
	}
	
	/**
	 * Zeichnet den Hilfe-Screen
	 */
	private static void drawHelpScreen(DrawContext context, MinecraftClient client) {
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		
		// Hintergrund-Overlay
		context.fill(0, 0, screenWidth, screenHeight, 0x80000000);
		
		// Hilfe-Box - Angepasste Größe für verschiedene Bildschirmgrößen
		int boxWidth = Math.min(350, screenWidth - 40); // Max 350px oder Bildschirmbreite - 40px
		int boxHeight = Math.min(400, screenHeight - 40); // Max 400px oder Bildschirmhöhe - 40px
		int boxX = (screenWidth - boxWidth) / 2;
		int boxY = (screenHeight - boxHeight) / 2;
		
		// Box-Hintergrund
		context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
		
		// Box-Rahmen
		context.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFFFFFFFF);
		context.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFFFFFFFF);
		context.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFFFFFFFF);
		context.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFFFFFFFF);
		
		// Titel
		context.drawText(
			client.textRenderer,
			"Suchleiste Hilfe",
			boxX + 10,
			boxY + 10,
			0xFFFFFF00,
			true
		);
		
		// Hilfe-Text
		int textY = boxY + 40;
		int lineHeight = 11;
		
		String[] helpText = {
			"Grundlegende Suche:",
			"• Makiert Items mit dem eingegebenen Namen",
			"",
			"Erweiterte Suche (@):",
			"• @Ring - Makiert alle Ringe",
			"• @Schaden>100 - Makiert items mit mehr als 100 Schaden",
			"• @Ring, @Rüstung>100 - Kombinierte Suche",
			"",
			"Vergleichsoperatoren:",
			"• > (größer als)",
			"• < (kleiner als)",
			"• = (gleich)",
			"",
			"Beispiele:",
			"• Göttlich - Makiert alle items mit Göttlich im Namen",
			"• @Rüstung>50 - Items mit Rüstung>50",
			"• @Ring, @Rüstung>50 - Ringe mit mehr als 50 Rüstung",
			"• @Ring, @Rüstung>50, Göttlich - Ringe mit mehr als 50 Rüstung und ",
			"                                        Göttlich im Namen",
			"",

		};
		
		// Zeichne alle Zeilen
		for (String line : helpText) {
			context.drawText(
				client.textRenderer,
				line,
				boxX + 10,
				textY,
				0xFFFFFFFF,
				true
			);
			textY += lineHeight;
		}
	}
	
	/**
	 * Zeichnet den @-Button
	 */
	private static void drawAtButton(DrawContext context, MinecraftClient client) {
		// Button-Hintergrund
		context.fill(atButtonX, atButtonY, atButtonX + atButtonSize, atButtonY + atButtonSize, 0x80000000);
		
		// Button-Rahmen
		context.fill(atButtonX, atButtonY, atButtonX + atButtonSize, atButtonY + 1, 0xFFFFFFFF);
		context.fill(atButtonX, atButtonY + atButtonSize - 1, atButtonX + atButtonSize, atButtonY + atButtonSize, 0xFFFFFFFF);
		context.fill(atButtonX, atButtonY, atButtonX + 1, atButtonY + atButtonSize, 0xFFFFFFFF);
		context.fill(atButtonX + atButtonSize - 1, atButtonY, atButtonX + atButtonSize, atButtonY + atButtonSize, 0xFFFFFFFF);
		
		// @-Zeichen
		context.drawText(
			client.textRenderer,
			"@",
			atButtonX + 5,
			atButtonY + 3,
			0xFFFFFFFF,
			true
		);
	}
	
	/**
	 * Findet den Anfang des Wortes an der gegebenen Position
	 */
	private static int findWordStart(String text, int position) {
		if (position <= 0) {
			return 0;
		}
		
		// Gehe rückwärts bis zum Anfang des Wortes
		int wordStart = position;
		
		// Überspringe Leerzeichen und Sonderzeichen am Ende
		while (wordStart > 0 && isWordSeparator(text.charAt(wordStart - 1))) {
			wordStart--;
		}
		
		// Gehe weiter rückwärts bis zum Anfang des Wortes
		while (wordStart > 0 && !isWordSeparator(text.charAt(wordStart - 1))) {
			wordStart--;
		}
		
		return wordStart;
	}
	
	/**
	 * Findet das Ende des Wortes an der gegebenen Position
	 */
	private static int findWordEnd(String text, int position) {
		if (position >= text.length()) {
			return text.length();
		}
		
		// Gehe vorwärts bis zum Ende des Wortes
		int wordEnd = position;
		
		// Überspringe Leerzeichen und Sonderzeichen am Anfang
		while (wordEnd < text.length() && isWordSeparator(text.charAt(wordEnd))) {
			wordEnd++;
		}
		
		// Gehe weiter vorwärts bis zum Ende des Wortes
		while (wordEnd < text.length() && !isWordSeparator(text.charAt(wordEnd))) {
			wordEnd++;
		}
		
		return wordEnd;
	}
	
	/**
	 * Prüft ob ein Zeichen ein Wort-Trenner ist
	 */
	private static boolean isWordSeparator(char c) {
		return Character.isWhitespace(c) || c == ',' || c == '.' || c == ';' || c == ':' || c == '!' || c == '?' || c == '@';
	}
	
	/**
	 * Berechnet die Cursor-Position basierend auf der Mausklick-Position
	 */
	private static int calculateCursorPosition(int clickX, String text, net.minecraft.client.font.TextRenderer textRenderer) {
		if (text.isEmpty()) {
			return 0;
		}
		
		// Finde die beste Position basierend auf der Klick-Position
		int bestPosition = 0;
		int bestDistance = Integer.MAX_VALUE;
		
		for (int i = 0; i <= text.length(); i++) {
			int textWidth = textRenderer.getWidth(text.substring(0, i));
			int distance = Math.abs(clickX - textWidth);
			
			if (distance < bestDistance) {
				bestDistance = distance;
				bestPosition = i;
			}
		}
		
		return bestPosition;
	}
	
	/**
	 * Rendert rote Rahmen um Items die mit dem Suchfilter übereinstimmen
	 */
	public static void renderSearchFrames(DrawContext context, HandledScreen<?> screen, int screenX, int screenY) {
		if (!isSearchBarVisible || searchText.isEmpty() || matchingSlots.isEmpty()) {
			return;
		}

		int frameColor = CCLiveUtilitiesConfig.HANDLER.instance().searchBarFrameColor.getRGB();
		
		// Zeichne rote Rahmen um die passenden Items
		for (Integer slotIndex : matchingSlots) {
			if (slotIndex < screen.getScreenHandler().slots.size()) {
				Slot slot = screen.getScreenHandler().slots.get(slotIndex);
				
				// Berechne die absolute Position auf dem Bildschirm
				int slotX = screenX + slot.x;
				int slotY = screenY + slot.y;
				
				// Zeichne 1 Pixel dicken roten Rahmen
				context.fill(slotX - 1, slotY - 1, slotX + SLOT_SIZE + 1, slotY, frameColor); // Oben
				context.fill(slotX - 1, slotY + SLOT_SIZE, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE + 1, frameColor); // Unten
				context.fill(slotX - 1, slotY - 1, slotX, slotY + SLOT_SIZE + 1, frameColor); // Links
				context.fill(slotX + SLOT_SIZE, slotY - 1, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE + 1, frameColor); // Rechts
			}
		}
	}
} 
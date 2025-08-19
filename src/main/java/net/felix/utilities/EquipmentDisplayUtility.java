package net.felix.utilities;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.felix.CCLiveUtilitiesConfig;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EquipmentDisplayUtility {
	
	// Statistik-Tracking Variablen
	private static boolean isInEquipmentChest = false;
	private static boolean showOverlays = true; // Neue Variable für Overlay-Sichtbarkeit
	private static Map<String, Double> percentageStats = new HashMap<>();
	private static Map<String, Double> absoluteStats = new HashMap<>();
	private static double totalArmor = 0.0;
	private static final int[] EQUIPMENT_SLOTS = {1, 2, 6, 10, 11, 15, 19, 20, 24, 29, 33, 38, 42, 48, 50};
	private static final Pattern STAT_PATTERN = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?)");
	private static final Pattern ARMOR_PATTERN = Pattern.compile("Rüstung\\s*([+-]?\\d+(?:\\.\\d+)?)");
	
	// Scroll-Variablen
	private static int leftScrollOffset = 0;
	private static int rightScrollOffset = 0;
	private static boolean leftOverlayHovered = false;
	private static boolean rightOverlayHovered = false;
	
	/**
	 * Wird vom MouseScrollMixin aufgerufen, um Maus-Scroll-Events zu verarbeiten
	 */
	public static void onMouseScroll(double vertical) {
		if (!isInEquipmentChest) {
			return;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return;
		}
		
		// Scroll-Geschwindigkeit anpassen (bei jedem Scroll-Klick)
		int scrollAmount = (int) (vertical * 12);
		
		if (leftOverlayHovered) {
			leftScrollOffset -= scrollAmount; // Negativ, weil nach oben scrollen = weniger Offset
			leftScrollOffset = Math.max(0, leftScrollOffset);
			
			// Begrenze Scroll-Offset basierend auf Anzahl der Einträge
			int baseVisibleEntries = 14; // Konsistent mit drawStatsInOverlay
			int maxScroll = Math.max(0, percentageStats.size() - baseVisibleEntries) * 12;
			leftScrollOffset = Math.min(leftScrollOffset, maxScroll);
			

		}
		
		if (rightOverlayHovered) {
			rightScrollOffset -= scrollAmount; // Negativ, weil nach oben scrollen = weniger Offset
			rightScrollOffset = Math.max(0, rightScrollOffset);
			
			// Begrenze Scroll-Offset basierend auf Anzahl der Einträge
			int baseVisibleEntries = 14; // Konsistent mit drawStatsInOverlay
			int maxScroll = Math.max(0, absoluteStats.size() - baseVisibleEntries) * 12;
			rightScrollOffset = Math.min(rightScrollOffset, maxScroll);
			

		}
	}

	/**
	 * Prüft ob Equipment-Overlays aktiv sind
	 */
	public static boolean isEquipmentOverlayActive() {
		return isInEquipmentChest && showOverlays;
	}
	
	public static boolean isInEquipmentChest() {
		return isInEquipmentChest;
	}
	
	public static void initialize() {
		// Client-seitige Events registrieren
		ClientTickEvents.END_CLIENT_TICK.register(EquipmentDisplayUtility::onClientTick);
		// Registriere HUD-Rendering
		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> onHudRender(drawContext, tickDelta));
		
		// SearchBar initialisieren
		SearchBarUtility.initialize();
	}

	private static void onClientTick(MinecraftClient client) {
		// Check Tab key for overlay visibility
		checkTabKey();
		if (client.player == null || client.currentScreen == null) {
			isInEquipmentChest = false;
			return;
		}

		// Überprüfe ob wir in einem Kisteninventar sind
		if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
			String title = handledScreen.getTitle().getString();
			
			if (title.contains("㬥") || title.contains("㬦") || title.contains("㬧") || title.contains("㬨")) {
				isInEquipmentChest = true;
				updateEquipmentStats(handledScreen, client);
				handleScrolling(client);
			} else {
				isInEquipmentChest = false;
			}
		} else {
			isInEquipmentChest = false;
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
	
	private static void handleScrolling(MinecraftClient client) {
		// Maus-Position abrufen
		int mouseX = (int) client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
		int mouseY = (int) client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
		
		// Overlay-Positionen berechnen
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		int inventoryWidth = 176;
		int inventoryHeight = 166;
		int inventoryX = (screenWidth - inventoryWidth) / 2;
		int inventoryY = (screenHeight - inventoryHeight) / 2;
		
		int overlayWidth = 170;
		int overlayHeight = inventoryHeight + 55;
		int overlaySpacing = 5;
		
		int leftOverlayX = inventoryX - overlayWidth - overlaySpacing;
		int rightOverlayX = inventoryX + inventoryWidth + overlaySpacing;
		int overlayY = inventoryY - 28;
		
		// Prüfe ob Maus über den Overlays ist
		leftOverlayHovered = mouseX >= leftOverlayX && mouseX <= leftOverlayX + overlayWidth &&
							mouseY >= overlayY && mouseY <= overlayY + overlayHeight;
		rightOverlayHovered = mouseX >= rightOverlayX && mouseX <= rightOverlayX + overlayWidth &&
							 mouseY >= overlayY && mouseY <= overlayY + overlayHeight;
		

		
		// Maus-Hover-Erkennung für Scroll-Funktionalität
		// Die eigentliche Scroll-Verarbeitung erfolgt über den MouseScrollMixin
	}

	private static void updateEquipmentStats(HandledScreen<?> screen, MinecraftClient client) {
		// Maps zurücksetzen
		percentageStats.clear();
		absoluteStats.clear();
		totalArmor = 0.0;

		for (int slotIndex : EQUIPMENT_SLOTS) {
			if (slotIndex < screen.getScreenHandler().slots.size()) {
				Slot slot = screen.getScreenHandler().slots.get(slotIndex);
				ItemStack itemStack = slot.getStack();
				
				if (!itemStack.isEmpty()) {
					// Hole die echten Tooltip-Daten des Items
					List<Text> lore = getItemTooltip(itemStack, client.player);
					boolean inStatistics = false;
					for (Text text : lore) {
						String line = text.getString();
						
						// Prüfe auf Rüstungswert
						Matcher armorMatcher = ARMOR_PATTERN.matcher(line);
						if (armorMatcher.find()) {
							try {
								double armorValue = Double.parseDouble(armorMatcher.group(1));
								totalArmor += armorValue;
							} catch (NumberFormatException e) {
								// Ignoriere ungültige Zahlen
							}
						}
						
						if (line.contains("Statistik")) {
							inStatistics = true;
							continue;
						}
						if (inStatistics && (line.contains("+") || line.contains("-"))) {
							// Extrahiere den Namen der Statistik und den Wert
							Map<String, Double> stats = extractStatsFromLine(line);
							for (Map.Entry<String, Double> entry : stats.entrySet()) {
								String statName = entry.getKey();
								Double value = entry.getValue();
								
								if (line.contains("%")) {
									percentageStats.merge(statName, value, Double::sum);
								} else {
									absoluteStats.merge(statName, value, Double::sum);
								}
							}
						}
					}
				}
			}
		}
	}

	private static Map<String, Double> extractStatsFromLine(String line) {
		Map<String, Double> stats = new HashMap<>();
		
		// Suche nach Zahlen und versuche den Namen zu extrahieren
		Matcher matcher = STAT_PATTERN.matcher(line);
		while (matcher.find()) {
			String number = matcher.group(1);
			try {
				double value = Double.parseDouble(number);
				
				// Versuche den Namen der Statistik zu extrahieren
				String statName = extractStatName(line, matcher.start());
				if (statName != null && !statName.isEmpty()) {
					stats.put(statName, value);
				}
			} catch (NumberFormatException e) {
				// Ignoriere ungültige Zahlen
			}
		}
		
		return stats;
	}

	private static String extractStatName(String line, int numberStart) {
		// Entferne die Zahl und +/- Zeichen aus der Zeile
		String lineWithoutNumber = line.replaceAll("[+-]?\\d+(?:\\.\\d+)?%?", "").trim();
		
		// Entferne häufige Wörter, die nicht der Statistik-Name sind
		lineWithoutNumber = lineWithoutNumber
			.replaceAll("(?i)\\b(statistik|bonus|malus|wert|erhöhung|reduzierung|zusatz|extra)\\b", "")
			.trim();
		
		// Entferne Satzzeichen und extra Spaces
		lineWithoutNumber = lineWithoutNumber.replaceAll("[^a-zA-ZäöüÄÖÜß\\s]", " ").trim();
		lineWithoutNumber = lineWithoutNumber.replaceAll("\\s+", " ").trim();
		
		if (!lineWithoutNumber.isEmpty()) {
			return lineWithoutNumber;
		}
		
		// Fallback: Versuche den Namen vor der Zahl zu finden
		String beforeNumber = line.substring(0, numberStart).trim();
		String[] words = beforeNumber.split("\\s+");
		if (words.length > 0) {
			String lastWord = words[words.length - 1];
			lastWord = lastWord.replaceAll("[^a-zA-ZäöüÄÖÜß]", "");
			if (!lastWord.isEmpty()) {
				return lastWord;
			}
		}
		
		// Fallback: Versuche den Namen nach der Zahl zu finden
		String afterNumber = line.substring(numberStart).trim();
		String[] afterWords = afterNumber.split("\\s+");
		if (afterWords.length > 0) {
			String firstWord = afterWords[0];
			firstWord = firstWord.replaceAll("[^a-zA-ZäöüÄÖÜß]", "");
			if (!firstWord.isEmpty()) {
				return firstWord;
			}
		}
		
		return "Unbekannt";
	}

	private static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayEnabled || 
			!CCLiveUtilitiesConfig.HANDLER.instance().showEquipmentDisplay) {
			return;
		}
		
		if (!isInEquipmentChest) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}

		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();

		// Berechne die Position des Inventars (normalerweise in der Mitte)
		int inventoryWidth = 176; // Standard Inventar-Breite
		int inventoryHeight = 166; // Standard Inventar-Höhe
		int inventoryX = (screenWidth - inventoryWidth) / 2;
		int inventoryY = (screenHeight - inventoryHeight) / 2;

		// Overlay-Dimensionen - dynamisch anpassbar
		int baseOverlayWidth = 160;
		int overlayHeight = inventoryHeight + 55;
		int overlaySpacing = 5; // Abstand zum Inventar

		// Berechne die benötigte Breite basierend auf der Länge der Texte
		int leftOverlayWidth = calculateRequiredWidth(percentageStats, baseOverlayWidth, true);
		int rightOverlayWidth = calculateRequiredWidth(absoluteStats, baseOverlayWidth, false);

		// Positionen der Overlays - verhindere Überlappung mit Bildschirmrändern
		int leftOverlayX = inventoryX - leftOverlayWidth - overlaySpacing;
		int rightOverlayX = inventoryX + inventoryWidth + overlaySpacing;
		int overlayY = inventoryY - 28;
		
		// Begrenze die Overlays so, dass sie nicht aus dem Bildschirm ragen
		// und gleiche Abstände zum Bildschirmrand haben
		int screenMargin = 5; // Abstand zum Bildschirmrand
		
		if (leftOverlayX < screenMargin) {
			// Linkes Overlay würde links aus dem Bildschirm ragen
			// Behalte den 5-Pixel-Abstand zum Inventar bei
			leftOverlayWidth = Math.min(leftOverlayWidth, inventoryX - overlaySpacing - screenMargin);
			leftOverlayX = inventoryX - leftOverlayWidth - overlaySpacing;
		}
		
		if (rightOverlayX + rightOverlayWidth > screenWidth - screenMargin) {
			// Rechtes Overlay würde rechts aus dem Bildschirm ragen
			rightOverlayWidth = Math.min(rightOverlayWidth, screenWidth - rightOverlayX - screenMargin);
		}
		
		// Begrenze die Y-Position so, dass das Overlay nicht oben oder unten aus dem Bildschirm ragt
		if (overlayY < screenMargin) {
			overlayY = screenMargin;
		}
		if (overlayY + overlayHeight > screenHeight - screenMargin) {
			overlayY = screenHeight - overlayHeight - screenMargin;
		}

		// Zeichne linkes schwarzes Overlay mit dynamischer Breite nur wenn aktiviert
		if (CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayShowBackground) {
			context.fill(leftOverlayX, overlayY, leftOverlayX + leftOverlayWidth, overlayY + overlayHeight, 0x80000000);
			
			// Zeichne rechtes schwarzes Overlay mit dynamischer Breite
			context.fill(rightOverlayX, overlayY, rightOverlayX + rightOverlayWidth, overlayY + overlayHeight, 0x80000000);
		}

		// Render nur wenn Overlays sichtbar sind
		if (showOverlays) {
			// Zeichne Rüstungswert mit konfigurierbarer Position und halbtransparentem Hintergrund
			if (totalArmor > 0) {
				String armorText = String.format("Rüstung: %s", formatNumber(totalArmor));
				int armorTextWidth = client.textRenderer.getWidth(armorText);
				
				// Berechne X-Position basierend auf Konfiguration
				int armorX = (screenWidth - armorTextWidth) / 2 + CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayArmorX;
				
				// Berechne Y-Position basierend auf Konfiguration
				int armorY = screenHeight - CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayArmorY;
				
				// Zeichne halbtransparentes Hintergrund-Overlay
				int padding = 4; // Padding um den Text
				int armorOverlayWidth = armorTextWidth + (padding * 2);
				int armorOverlayHeight = 12 + (padding * 2); // Text-Höhe + Padding
				int armorOverlayX = armorX - padding;
				int armorOverlayY = armorY - padding;
				
				// Halbtransparentes schwarzes Overlay nur wenn aktiviert
				if (CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayShowBackground) {
					context.fill(armorOverlayX, armorOverlayY, armorOverlayX + armorOverlayWidth, armorOverlayY + armorOverlayHeight, 0x80000000);
				}
				
				// Zeichne den Text
				context.drawText(
					client.textRenderer,
					armorText,
					armorX,
					armorY,
					0xFFFFFFFF,
					true
				);
			}

			// Zeichne Prozentwerte im linken Overlay
			drawStatsInOverlay(context, client.textRenderer, percentageStats, leftOverlayX, overlayY, leftOverlayWidth, overlayHeight, true, leftScrollOffset);

			// Zeichne absolute Werte im rechten Overlay
			drawStatsInOverlay(context, client.textRenderer, absoluteStats, rightOverlayX, overlayY, rightOverlayWidth, overlayHeight, false, rightScrollOffset);
		}
	}

	private static String formatNumber(double value) {
		if (value == (int) value) {
			// Glatte Zahl ohne Nachkommastellen
			return String.valueOf((int) value);
		} else {
			// Zahl mit Nachkommastellen
			return String.format("%.1f", value);
		}
	}
	
	/**
	 * Berechnet die benötigte Breite für das Overlay basierend auf der Länge der Texte
	 */
	private static int calculateRequiredWidth(Map<String, Double> stats, int baseWidth, boolean isPercentage) {
		if (stats.isEmpty()) {
			return baseWidth;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return baseWidth;
		}
		
		net.minecraft.client.font.TextRenderer textRenderer = client.textRenderer;
		int maxTextWidth = 0;
		
		// Finde den längsten Text (nach Kürzung)
		for (Map.Entry<String, Double> entry : stats.entrySet()) {
			double value = entry.getValue();
			String formattedValue = formatNumber(value);
			if (value > 0) {
				formattedValue = "+" + formattedValue;
			}
			String text = isPercentage ? String.format("%s%% %s", formattedValue, entry.getKey()) : String.format("%s %s", formattedValue, entry.getKey());
			
			// Kürze Text auf maximal 40 Zeichen (wie in drawStatEntrySimple)
			text = truncateText(text, 40);
			
			int textWidth = textRenderer.getWidth(text);
			maxTextWidth = Math.max(maxTextWidth, textWidth);
		}
		
		// Überschrift-Breite hinzufügen (kann auch gekürzt werden)
		String header = isPercentage ? "Prozentwerte" : "Flatwerte";
		header = truncateText(header, 40);
		int headerWidth = textRenderer.getWidth(header);
		maxTextWidth = Math.max(maxTextWidth, headerWidth);
		
		// Abstände hinzufügen (3 Pixel links + 3 Pixel rechts = 6 Pixel)
		int requiredWidth = maxTextWidth + 3;
		
		// Mindestens die Basis-Breite verwenden
		return Math.max(requiredWidth, baseWidth);
	}

	/**
	 * Zeichnet Statistiken in einem Overlay mit fester Höhe und dynamischer Breite
	 */
	private static void drawStatsInOverlay(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, Map<String, Double> stats, int overlayX, int overlayY, int overlayWidth, int overlayHeight, boolean isPercentage, int scrollOffset) {
		int textX = overlayX + 3; // 3 Pixel Abstand vom Rand (2 Pixel näher)
		int yOffset = overlayY + 5; // 5 Pixel Abstand vom oberen Rand (5 Pixel höher)
		int maxY = overlayY + overlayHeight - 10; // Maximale Y-Position
		
		// Wenn das Overlay zu schmal ist, zeichne einen Hinweis
		if (overlayWidth < 80) {
			context.drawText(textRenderer, "Zu schmal", textX, yOffset, 0xFFFF0000, true);
			return;
		}
		
		// Überschrift
		String header = isPercentage ? "Prozentwerte" : "Flatwerte";
		int headerColor = CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayHeaderColor.getRGB();
		context.drawText(textRenderer, header, textX, yOffset, headerColor, true);
		yOffset += 15;
		

		int totalEntries = stats.size();
		
		if (totalEntries > 0) {
			// Konvertiere Map zu Liste für einfacheres Scrollen
			List<Map.Entry<String, Double>> entriesList = new ArrayList<>(stats.entrySet());
			
			// Berechne Start-Index basierend auf Scroll-Offset
			int startIndex = scrollOffset / 12;
			int baseVisibleEntries = (overlayHeight - 50) / 12;
			int maxVisibleEntries = baseVisibleEntries + (startIndex == 0 ? 1 : 0);
			
			// Zeige Scroll-Indikator wenn nötig
			if (startIndex > 0) {
				String moreText = String.format("↑ %d weitere (Scrollen)", startIndex);
				context.drawText(textRenderer, moreText, textX, yOffset, 0x80FFFFFF, true);
				yOffset += 12;
			}
			
			// Zeige sichtbare Einträge mit fester Höhe
			int currentIndex = startIndex;
			int visibleCount = 0;
			
			while (currentIndex < totalEntries && visibleCount < maxVisibleEntries && yOffset < maxY) {
				Map.Entry<String, Double> entry = entriesList.get(currentIndex);
				
				// Zeichne den Eintrag ohne Zeilenumbruch
				drawStatEntrySimple(context, textRenderer, entry, textX, yOffset, isPercentage);
				yOffset += 12; // Feste Zeilenhöhe
				
				currentIndex++;
				visibleCount++;
			}
			
			// Zeige Scroll-Indikator wenn nötig
			if (currentIndex < totalEntries) {
				String moreText = String.format("↓ %d weitere (Scrollen)", totalEntries - currentIndex);
				context.drawText(textRenderer, moreText, textX, yOffset, 0x80FFFFFF, true);
			}
		}
	}
	
	/**
	 * Kürzt Text auf maximal 40 Zeichen und fügt "..." hinzu wenn nötig
	 */
	private static String truncateText(String text, int maxLength) {
		if (text.length() <= maxLength) {
			return text;
		}
		return text.substring(0, maxLength - 3) + "...";
	}

	/**
	 * Zeichnet einen einzelnen Statistik-Eintrag ohne Zeilenumbruch mit kleinerer Schrift
	 */
	private static void drawStatEntrySimple(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, Map.Entry<String, Double> entry, int x, int y, boolean isPercentage) {
		double value = entry.getValue();
		String formattedValue = formatNumber(value);
		if (!isPercentage && value > 0) {
			formattedValue = "+" + formattedValue;
		}
		String text = isPercentage ? String.format("%s%% %s", formattedValue, entry.getKey()) : String.format("%s %s", formattedValue, entry.getKey());

		// Kürze Text auf maximal 40 Zeichen
		if (text.length() >= 40) {
			text = text.substring(0, 37) + "...";
		}

		// Zeichne den Text mit konfigurierter Farbe
		int textColor = CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayTextColor.getRGB();
		context.drawText(textRenderer, text, x, y, textColor, true);
	}
	



	
	/**
	 * Extrahiert die Tooltip-Daten (Lore) direkt aus den NBT-Daten eines ItemStacks
	 */
	private static List<Text> getItemTooltip(ItemStack itemStack, PlayerEntity player) {
		List<Text> tooltip = new ArrayList<>();
		// Füge den Item-Namen hinzu
		tooltip.add(itemStack.getName());

		// Lese die Lore über die Data Component API (ab 1.21.7)
		var loreComponent = itemStack.get(DataComponentTypes.LORE);
		if (loreComponent != null) {
			tooltip.addAll(loreComponent.lines());
		}
		return tooltip;
	}
} 
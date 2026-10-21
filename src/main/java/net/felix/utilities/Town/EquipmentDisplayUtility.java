package net.felix.utilities.Town;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.KeyBindingUtility;
import net.felix.utilities.Overall.SearchBarUtility;
import net.felix.utilities.Overall.ZeichenUtility;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class EquipmentDisplayUtility {
	
	// Statistik-Tracking Variablen
	private static boolean isInEquipmentChest = false;
	private static boolean showOverlays = true; // Neue Variable für Overlay-Sichtbarkeit
	private static Map<String, Double> percentageStats = new HashMap<>();
	private static Map<String, Double> absoluteStats = new HashMap<>();
	private static double totalArmor = 0.0;

	public static double getTotalArmor() {
		return totalArmor;
	}
	private static final int[] EQUIPMENT_SLOTS = BuildSlotMapping.getEquipmentStatSlots();
	// Pattern für Zahlen mit Tausendertrennungen: Kommas als Tausendertrennungen, Punkt als Dezimaltrenner
	private static final Pattern STAT_PATTERN = Pattern.compile("([+-]?\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)");
	private static final Pattern ARMOR_PATTERN = Pattern.compile("Rüstung\\s*([+-]?\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?)");
	
	// Overlay texture identifiers
	private static final Identifier LEFT_OVERLAY_TEXTURE = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/gui/left_overlay.png");
	private static final Identifier RIGHT_OVERLAY_TEXTURE = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/gui/right_overlay.png");
	
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
		
		Minecraft client = Minecraft.getInstance();
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
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath("cclive-utilities", "equipment_display"),
				EquipmentDisplayUtility::onHudRender);
		
		// SearchBar initialisieren
		SearchBarUtility.initialize();
	}

	private static void onClientTick(Minecraft client) {
		// Check Tab key for overlay visibility
		checkTabKey();
		if (client.player == null || client.screen == null) {
			isInEquipmentChest = false;
			return;
		}

		// Überprüfe ob wir in einem Kisteninventar sind
		if (client.screen instanceof AbstractContainerScreen<?> handledScreen) {
			String title = handledScreen.getTitle().getString();
			
			if (ZeichenUtility.containsEquipmentDisplay(title)) { //Equipment Display
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
	
	private static void handleScrolling(Minecraft client) {
		// Maus-Position abrufen
		// Add null checks and division by zero protection
		if (client == null || client.getWindow() == null) {
			return;
		}
		
		int windowWidth = client.getWindow().getScreenWidth();
		int windowHeight = client.getWindow().getScreenHeight();
		
		// Prevent division by zero
		if (windowWidth <= 0 || windowHeight <= 0) {
			return;
		}
		
		int mouseX = (int) client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / windowWidth;
		int mouseY = (int) client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / windowHeight;
		
		// Overlay-Positionen berechnen
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		
		// Additional safety check for scaled dimensions
		if (screenWidth <= 0 || screenHeight <= 0) {
			return;
		}
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

	/** Aggregierte Equipment-Stats (Prozent links, Flat rechts). */
	public static final class AggregatedStats {
		public final Map<String, Double> percentageStats = new HashMap<>();
		public final Map<String, Double> absoluteStats = new HashMap<>();
		public double totalArmor;
	}

	private static void updateEquipmentStats(AbstractContainerScreen<?> screen, Minecraft client) {
		AggregatedStats aggregated = new AggregatedStats();
		for (int slotIndex : EQUIPMENT_SLOTS) {
			if (slotIndex < screen.getMenu().slots.size()) {
				Slot slot = screen.getMenu().slots.get(slotIndex);
				ItemStack itemStack = slot.getItem();
				if (!itemStack.isEmpty()) {
					accumulateTooltipLines(aggregated, getItemTooltip(itemStack, client.player));
				}
			}
		}
		percentageStats.clear();
		percentageStats.putAll(aggregated.percentageStats);
		absoluteStats.clear();
		absoluteStats.putAll(aggregated.absoluteStats);
		totalArmor = aggregated.totalArmor;
	}

	/**
	 * Stats aus Tooltip-Zeilen (Component oder Klartext) — für echte Inventare und Build-Share-Fake-Items.
	 */
	public static AggregatedStats aggregateFromTooltipLines(Iterable<? extends Iterable<String>> itemTooltipLines) {
		AggregatedStats out = new AggregatedStats();
		if (itemTooltipLines == null) {
			return out;
		}
		for (Iterable<String> lines : itemTooltipLines) {
			if (lines == null) {
				continue;
			}
			List<Component> asComponents = new ArrayList<>();
			for (String line : lines) {
				asComponents.add(Component.literal(line != null ? line : ""));
			}
			accumulateTooltipLines(out, asComponents);
		}
		return out;
	}

	public static void accumulateTooltipLines(AggregatedStats out, List<Component> lore) {
		if (out == null || lore == null) {
			return;
		}
		boolean inStatistics = false;
		for (Component text : lore) {
			if (text == null) {
				continue;
			}
			String line = text.getString();
			Matcher armorMatcher = ARMOR_PATTERN.matcher(line);
			if (armorMatcher.find()) {
				try {
					out.totalArmor += Double.parseDouble(removeThousandSeparators(armorMatcher.group(1)));
				} catch (NumberFormatException ignored) {
				}
			}
			if (line.contains("Statistik")) {
				inStatistics = true;
				continue;
			}
			if (inStatistics && (line.contains("+") || line.contains("-"))) {
				Map<String, Double> stats = extractStatsFromLine(line);
				for (Map.Entry<String, Double> entry : stats.entrySet()) {
					if (line.contains("%")) {
						out.percentageStats.merge(entry.getKey(), entry.getValue(), Double::sum);
					} else {
						out.absoluteStats.merge(entry.getKey(), entry.getValue(), Double::sum);
					}
				}
			}
		}
	}

	/**
	 * Links/Rechts-Overlays relativ zu einem Inventar-Rechteck zeichnen (Equipment-Menü).
	 */
	public static void renderOverlaysAt(
			GuiGraphicsExtractor context,
			int inventoryX,
			int inventoryY,
			int inventoryWidth,
			int inventoryHeight,
			Map<String, Double> pctStats,
			Map<String, Double> absStats,
			double armorTotal,
			boolean drawContents,
			int leftScroll,
			int rightScroll) {
		renderOverlaysAt(context, inventoryX, inventoryY, inventoryWidth, inventoryHeight,
			pctStats, absStats, armorTotal, drawContents, leftScroll, rightScroll, false);
	}

	/**
	 * @param flushToInventoryBounds true = Overlay-Höhe/Y = Inventar (Build-Share-PNG);
	 *                               false = klassisch höher (+55) und etwas nach oben versetzt
	 */
	public static void renderOverlaysAt(
			GuiGraphicsExtractor context,
			int inventoryX,
			int inventoryY,
			int inventoryWidth,
			int inventoryHeight,
			Map<String, Double> pctStats,
			Map<String, Double> absStats,
			double armorTotal,
			boolean drawContents,
			int leftScroll,
			int rightScroll,
			boolean flushToInventoryBounds) {
		if (context == null || pctStats == null || absStats == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null) {
			return;
		}
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		int baseOverlayWidth = 160;
		// Build-Share: oben bündig mit PNG, unten 22px früher (41 − 19)
		int overlayHeight = flushToInventoryBounds
			? Math.max(40, inventoryHeight - 22)
			: inventoryHeight + 55;
		int overlaySpacing = 5;
		int leftOverlayWidth = calculateRequiredWidth(pctStats, baseOverlayWidth, true);
		int rightOverlayWidth = calculateRequiredWidth(absStats, baseOverlayWidth, false);
		int screenMargin = 5;
		int desiredLeftOverlayX = inventoryX - leftOverlayWidth - overlaySpacing;
		int leftOverlayX = Math.max(screenMargin, desiredLeftOverlayX);
		if (leftOverlayX == screenMargin) {
			leftOverlayWidth = Math.min(leftOverlayWidth, inventoryX - overlaySpacing - screenMargin);
		}
		int rightOverlayX = inventoryX + inventoryWidth + overlaySpacing;
		if (rightOverlayX + rightOverlayWidth > screenWidth - screenMargin) {
			rightOverlayWidth = screenWidth - screenMargin - rightOverlayX;
		}
		int overlayY = flushToInventoryBounds ? inventoryY : inventoryY - 28;
		if (overlayY < screenMargin) {
			overlayY = screenMargin;
		}
		if (overlayY + overlayHeight > screenHeight - screenMargin) {
			overlayY = Math.max(screenMargin, screenHeight - overlayHeight - screenMargin);
		}

		OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayOverlayType;
		if (overlayType == OverlayType.CUSTOM) {
			try {
				context.blit(RenderPipelines.GUI_TEXTURED, LEFT_OVERLAY_TEXTURE,
					leftOverlayX, overlayY, 0.0f, 0.0f,
					leftOverlayWidth, overlayHeight, leftOverlayWidth, overlayHeight);
				context.blit(RenderPipelines.GUI_TEXTURED, RIGHT_OVERLAY_TEXTURE,
					rightOverlayX, overlayY, 0.0f, 0.0f,
					rightOverlayWidth, overlayHeight, rightOverlayWidth, overlayHeight);
			} catch (Exception e) {
				context.fill(leftOverlayX, overlayY, leftOverlayX + leftOverlayWidth, overlayY + overlayHeight, 0x80000000);
				context.fill(rightOverlayX, overlayY, rightOverlayX + rightOverlayWidth, overlayY + overlayHeight, 0x80000000);
			}
		} else if (overlayType == OverlayType.BLACK) {
			context.fill(leftOverlayX, overlayY, leftOverlayX + leftOverlayWidth, overlayY + overlayHeight, 0x80000000);
			context.fill(rightOverlayX, overlayY, rightOverlayX + rightOverlayWidth, overlayY + overlayHeight, 0x80000000);
		}

		if (!drawContents) {
			return;
		}
		if (armorTotal > 0) {
			String armorText = String.format("Rüstung: %s", formatNumber(armorTotal));
			int armorTextWidth = client.font.width(armorText);
			int textHeight = client.font.lineHeight;
			int padding = 4;
			int armorX = inventoryX + (inventoryWidth - armorTextWidth) / 2;
			int armorY;
			if (flushToInventoryBounds) {
				armorY = overlayY + 6;
			} else {
				int gapAboveInventory = 40;
				armorY = inventoryY - textHeight - padding - gapAboveInventory;
			}
			int armorOverlayWidth = armorTextWidth + (padding * 2);
			int armorOverlayHeight = textHeight + (padding * 2);
			int armorOverlayX = armorX - padding;
			int armorOverlayY = armorY - padding;
			if (overlayType != OverlayType.NONE) {
				context.fill(armorOverlayX, armorOverlayY,
					armorOverlayX + armorOverlayWidth, armorOverlayY + armorOverlayHeight, 0x80000000);
			}
			context.text(client.font, armorText, armorX, armorY, 0xFFFFFFFF, true);
		}
		int extraTopPad = flushToInventoryBounds ? 4 : 0;
		drawStatsInOverlay(context, client.font, pctStats, leftOverlayX, overlayY, leftOverlayWidth, overlayHeight, true, leftScroll, extraTopPad);
		drawStatsInOverlay(context, client.font, absStats, rightOverlayX, overlayY, rightOverlayWidth, overlayHeight, false, rightScroll, extraTopPad);
	}

	/** Hover-Rechtecke der Overlays für Scroll (gleiche Geometrie wie {@link #renderOverlaysAt}). */
	public static boolean[] computeOverlayHover(
			int mouseX, int mouseY,
			int inventoryX, int inventoryY, int inventoryWidth, int inventoryHeight,
			Map<String, Double> pctStats, Map<String, Double> absStats) {
		return computeOverlayHover(mouseX, mouseY, inventoryX, inventoryY, inventoryWidth, inventoryHeight,
			pctStats, absStats, false);
	}

	public static boolean[] computeOverlayHover(
			int mouseX, int mouseY,
			int inventoryX, int inventoryY, int inventoryWidth, int inventoryHeight,
			Map<String, Double> pctStats, Map<String, Double> absStats,
			boolean flushToInventoryBounds) {
		Minecraft client = Minecraft.getInstance();
		boolean[] hover = new boolean[]{false, false};
		if (client == null || client.getWindow() == null || pctStats == null || absStats == null) {
			return hover;
		}
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		int baseOverlayWidth = 160;
		int overlayHeight = flushToInventoryBounds
			? Math.max(40, inventoryHeight - 22)
			: inventoryHeight + 55;
		int overlaySpacing = 5;
		int leftOverlayWidth = calculateRequiredWidth(pctStats, baseOverlayWidth, true);
		int rightOverlayWidth = calculateRequiredWidth(absStats, baseOverlayWidth, false);
		int screenMargin = 5;
		int leftOverlayX = Math.max(screenMargin, inventoryX - leftOverlayWidth - overlaySpacing);
		if (leftOverlayX == screenMargin) {
			leftOverlayWidth = Math.min(leftOverlayWidth, inventoryX - overlaySpacing - screenMargin);
		}
		int rightOverlayX = inventoryX + inventoryWidth + overlaySpacing;
		if (rightOverlayX + rightOverlayWidth > screenWidth - screenMargin) {
			rightOverlayWidth = screenWidth - screenMargin - rightOverlayX;
		}
		int overlayY = flushToInventoryBounds ? inventoryY : inventoryY - 28;
		if (overlayY < screenMargin) {
			overlayY = screenMargin;
		}
		if (overlayY + overlayHeight > screenHeight - screenMargin) {
			overlayY = Math.max(screenMargin, screenHeight - overlayHeight - screenMargin);
		}
		hover[0] = mouseX >= leftOverlayX && mouseX <= leftOverlayX + leftOverlayWidth
			&& mouseY >= overlayY && mouseY <= overlayY + overlayHeight;
		hover[1] = mouseX >= rightOverlayX && mouseX <= rightOverlayX + rightOverlayWidth
			&& mouseY >= overlayY && mouseY <= overlayY + overlayHeight;
		return hover;
	}

	private static Map<String, Double> extractStatsFromLine(String line) {
		Map<String, Double> stats = new HashMap<>();
		
		// Suche nach Zahlen und versuche den Namen zu extrahieren
		Matcher matcher = STAT_PATTERN.matcher(line);
		while (matcher.find()) {
			String number = matcher.group(1);
			try {
				String numberStr = removeThousandSeparators(number);
				double value = Double.parseDouble(numberStr);
				
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

	/**
	 * Entfernt Tausendertrennungen (Kommas) aus einer Zahl, bevor sie geparst wird
	 */
	private static String removeThousandSeparators(String number) {
		return number.replace(",", "");
	}

	private static String extractStatName(String line, int numberStart) {
		// Entferne die Zahl und +/- Zeichen aus der Zeile (berücksichtigt auch Tausendertrennungen)
		String lineWithoutNumber = line.replaceAll("[+-]?\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?%?", "").trim();
		
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

	private static void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.isModRuntimeEnabled() ||
			!CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayEnabled || 
			!CCLiveUtilitiesConfig.HANDLER.instance().showEquipmentDisplay) {
			return;
		}
		
		if (!isInEquipmentChest) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null) {
			return;
		}
		
		// Hide overlay if F1 menu (debug screen) is open
		if (client.options.hideGui) {
			return;
		}

		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		int inventoryWidth = 176;
		int inventoryHeight = 166;
		int inventoryX = (screenWidth - inventoryWidth) / 2;
		int inventoryY = (screenHeight - inventoryHeight) / 2;

		renderOverlaysAt(
			context,
			inventoryX, inventoryY, inventoryWidth, inventoryHeight,
			percentageStats, absoluteStats, totalArmor,
			showOverlays, leftScrollOffset, rightScrollOffset);
	}

	private static String formatNumber(double value) {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
		symbols.setGroupingSeparator(',');
		symbols.setDecimalSeparator('.');

		if (Math.abs(value - Math.rint(value)) < 1e-9) {
			DecimalFormat df = new DecimalFormat("#,###", symbols);
			return df.format(Math.rint(value));
		}
		// Bis zu 2 Nachkommastellen, ohne unnötige Nullen (z. B. 0.09 statt 0.1)
		DecimalFormat df = new DecimalFormat("#,##0.##", symbols);
		return df.format(value);
	}
	
	/**
	 * Berechnet die benötigte Breite für das Overlay basierend auf der Länge der Texte
	 */
	private static int calculateRequiredWidth(Map<String, Double> stats, int baseWidth, boolean isPercentage) {
		if (stats.isEmpty()) {
			return baseWidth;
		}
		
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return baseWidth;
		}
		
		net.minecraft.client.gui.Font textRenderer = client.font;
		int maxTextWidth = 0;
		
		// Finde den längsten Text (verwende vollen Text für Breitenberechnung)
		for (Map.Entry<String, Double> entry : stats.entrySet()) {
			double value = entry.getValue();
			String formattedValue = formatNumber(value);
			if (value > 0) {
				formattedValue = "+" + formattedValue;
			}
			String text = isPercentage ? String.format("%s%% %s", formattedValue, entry.getKey()) : String.format("%s %s", formattedValue, entry.getKey());
			
			// Verwende den vollen Text für die Breitenberechnung, aber kürze ihn für die Anzeige
			String displayText = truncateText(text, 32);
			
			int textWidth = textRenderer.width(displayText);
			maxTextWidth = Math.max(maxTextWidth, textWidth);
		}
		
		// Überschrift-Breite hinzufügen (kann auch gekürzt werden)
		String header = isPercentage ? "Prozentwerte" : "Flatwerte";
		header = truncateText(header, 32);
		int headerWidth = textRenderer.width(header);
		maxTextWidth = Math.max(maxTextWidth, headerWidth);
		
		// Abstände hinzufügen (11 Pixel links + 12 Pixel rechts = 23 Pixel)
		int requiredWidth = maxTextWidth + 23;
		
		// Mindestens die Basis-Breite verwenden
		return Math.max(requiredWidth, baseWidth);
	}

	/**
	 * Zeichnet Statistiken in einem Overlay mit fester Höhe und dynamischer Breite
	 */
	private static void drawStatsInOverlay(GuiGraphicsExtractor context, net.minecraft.client.gui.Font textRenderer, Map<String, Double> stats, int overlayX, int overlayY, int overlayWidth, int overlayHeight, boolean isPercentage, int scrollOffset) {
		drawStatsInOverlay(context, textRenderer, stats, overlayX, overlayY, overlayWidth, overlayHeight, isPercentage, scrollOffset, 0);
	}

	private static void drawStatsInOverlay(GuiGraphicsExtractor context, net.minecraft.client.gui.Font textRenderer, Map<String, Double> stats, int overlayX, int overlayY, int overlayWidth, int overlayHeight, boolean isPercentage, int scrollOffset, int extraTopPad) {
		int textX = overlayX + 11; // 11 Pixel Abstand vom Rand (3 + 8 Pixel nach rechts)
		int yOffset = overlayY + 8 + Math.max(0, extraTopPad); // +extraTopPad: erste Textzeile etwas tiefer
		int maxY = overlayY + overlayHeight - 10; // Maximale Y-Position
		
		// Wenn das Overlay zu schmal ist, zeichne einen Hinweis
		if (overlayWidth < 80) {
			context.text(textRenderer, "Zu schmal", textX, yOffset, 0xFFFF0000, true);
			return;
		}
		
		// Überschrift nur anzeigen wenn keine Bild-Overlays verwendet werden
		if (CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayOverlayType != OverlayType.CUSTOM) {
			String header = isPercentage ? "Prozentwerte" : "Flatwerte";
			int headerColor = CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayHeaderColor.getRGB();
			context.text(textRenderer, header, textX, yOffset, headerColor, true);
		}
		// Immer den gleichen Abstand für konsistente Positionierung
		yOffset += 15;
		

		int totalEntries = stats.size();
		
		if (totalEntries > 0) {
			// Konvertiere Map zu Liste und sortiere nach Wert (größte Werte zuerst)
			List<Map.Entry<String, Double>> entriesList = new ArrayList<>(stats.entrySet());
			entriesList.sort((a, b) -> Double.compare(Math.abs(b.getValue()), Math.abs(a.getValue())));
			
			// Berechne Start-Index basierend auf Scroll-Offset
			int startIndex = scrollOffset / 12;
			int baseVisibleEntries = (overlayHeight - 50) / 12;
			int maxVisibleEntries = baseVisibleEntries + (startIndex == 0 ? 1 : 0);
			
			// Zeige Scroll-Indikator wenn nötig
			if (startIndex > 0) {
				String moreText = String.format("↑ %d weitere (Scrollen)", startIndex);
				context.text(textRenderer, moreText, textX, yOffset, 0x80FFFFFF, true);
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
				context.text(textRenderer, moreText, textX, yOffset, 0x80FFFFFF, true);
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
	private static void drawStatEntrySimple(GuiGraphicsExtractor context, net.minecraft.client.gui.Font textRenderer, Map.Entry<String, Double> entry, int x, int y, boolean isPercentage) {
		double value = entry.getValue();
		String formattedValue = formatNumber(value);
		if (!isPercentage && value > 0) {
			formattedValue = "+" + formattedValue;
		}
		String text = isPercentage ? String.format("%s%% %s", formattedValue, entry.getKey()) : String.format("%s %s", formattedValue, entry.getKey());

		// Kürze Text auf maximal 32 Zeichen
		if (text.length() >= 32) {
			text = text.substring(0, 29) + "...";
		}

		// Zeichne den Text mit konfigurierter Farbe
		int textColor = CCLiveUtilitiesConfig.HANDLER.instance().equipmentDisplayTextColor.getRGB();
		context.text(textRenderer, text, x, y, textColor, true);
	}
	



	
	/**
	 * Extrahiert die Tooltip-Daten (Lore) direkt aus den NBT-Daten eines ItemStacks
	 */
	private static List<Component> getItemTooltip(ItemStack itemStack, Player player) {
		List<Component> tooltip = new ArrayList<>();
		// Füge den Item-Namen hinzu
		tooltip.add(itemStack.getHoverName());

		// Lese die Lore über die Data Component API (ab 1.21.7)
		var loreComponent = itemStack.get(DataComponents.LORE);
		if (loreComponent != null) {
			tooltip.addAll(loreComponent.lines());
		}
		return tooltip;
	}
} 
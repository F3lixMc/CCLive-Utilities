package net.felix.utilities.Aincraft;

import net.felix.utilities.Overall.KeyCategories;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.joml.Matrix3x2fStack;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.KeyBindingUtility;
import net.felix.utilities.Town.EquipmentDisplayUtility;
import net.felix.utilities.Town.OverlayType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CardsStatuesUtility {
	

	private static boolean isInitialized = false;
	private static boolean showOverlays = true; // Neue Variable für Overlay-Sichtbarkeit
	private static CardsStatuesUtility INSTANCE;
	
	// Hotkey variable
	private static KeyMapping toggleKeyMapping;
	
	// Daten für Karten und Statuen
	private static CardData currentCard = null;
	private static StatueData currentStatue = null;
	private static Identifier lastDimension = null;
	

	
	// Rendering-Konstanten
	private static final int BACKGROUND_WIDTH = 162;
	private static final int BACKGROUND_HEIGHT = 62;
	
	// Textur-Identifier für den Karten-Hintergrund
	private static final Identifier CARD_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/gui/karten_background.png");
	
	// Textur-Identifier für den Statuen-Hintergrund
	private static final Identifier STATUE_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/gui/statuen_background.png");
	
	/**
	 * Rendert den Karten-Hintergrund basierend auf dem Overlay-Typ
	 */
	private static void renderCardBackground(GuiGraphicsExtractor context, int x, int y) {
		OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayType;
		
		// Verwende Matrix-Transformationen für Skalierung
		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		
		// Skaliere das Overlay basierend auf der Config
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayScale;
		if (scale <= 0) scale = 1.0f; // Sicherheitscheck
		
		// Übersetze zur Position und skaliere von dort aus
		matrices.translate(x, y);
		matrices.scale(scale, scale);
		
		if (overlayType == OverlayType.CUSTOM) {
			// Bild-Overlay mit karten_background.png
			try {
				context.blit(
					RenderPipelines.GUI_TEXTURED,
					CARD_BACKGROUND_TEXTURE,
					-11, -11, // Relative Position (0-basiert, da wir bereits übersetzt haben)
					0.0f, 0.0f, // UV-Koordinaten (Start der Textur)
					BACKGROUND_WIDTH, BACKGROUND_HEIGHT, // Größe
					BACKGROUND_WIDTH, BACKGROUND_HEIGHT // Textur-Größe
				);
			} catch (Exception e) {
				// Fallback: Verwende den schwarzen Hintergrund wenn Textur-Loading fehlschlägt
				context.fill(-11, -11, BACKGROUND_WIDTH - 11, BACKGROUND_HEIGHT - 11, 0x80000000);
			}
		} else if (overlayType == OverlayType.BLACK) {
			// Schwarzes halbtransparentes Overlay
			context.fill(-11, -11, BACKGROUND_WIDTH - 11, BACKGROUND_HEIGHT - 11, 0x80000000);
		}
		// Bei OverlayType.NONE wird kein Hintergrund gezeichnet
		
		matrices.popMatrix();
	}
	
	/**
	 * Rendert den Statuen-Hintergrund basierend auf dem Overlay-Typ
	 */
	private static void renderStatueBackground(GuiGraphicsExtractor context, int x, int y) {
		OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayType;
		
		// Verwende Matrix-Transformationen für Skalierung
		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		
		// Skaliere das Overlay basierend auf der Config
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayScale;
		if (scale <= 0) scale = 1.0f; // Sicherheitscheck
		
		// Übersetze zur Position und skaliere von dort aus
		matrices.translate(x, y);
		matrices.scale(scale, scale);
		
		if (overlayType == OverlayType.CUSTOM) {
			// Bild-Overlay mit statuen_background.png
			try {
				context.blit(
					RenderPipelines.GUI_TEXTURED,
					STATUE_BACKGROUND_TEXTURE,
					-11, -11, // Relative Position (0-basiert, da wir bereits übersetzt haben)
					0.0f, 0.0f, // UV-Koordinaten (Start der Textur)
					BACKGROUND_WIDTH, BACKGROUND_HEIGHT, // Größe
					BACKGROUND_WIDTH, BACKGROUND_HEIGHT // Textur-Größe
				);
			} catch (Exception e) {
				// Fallback: Verwende den schwarzen Hintergrund wenn Textur-Loading fehlschlägt
				context.fill(-11, -11, BACKGROUND_WIDTH - 11, BACKGROUND_HEIGHT - 11, 0x80000000);
			}
		} else if (overlayType == OverlayType.BLACK) {
			// Schwarzes halbtransparentes Overlay
			context.fill(-11, -11, BACKGROUND_WIDTH - 11, BACKGROUND_HEIGHT - 11, 0x80000000);
		}
		// Bei OverlayType.NONE wird kein Hintergrund gezeichnet
		
		matrices.popMatrix();
	}
	

	
	public CardsStatuesUtility() {
		INSTANCE = this;
	}
	
	public static CardsStatuesUtility getInstance() {
		if (INSTANCE == null) {
			throw new IllegalStateException("CardsStatuesUtility instance is null!");
		}
		return INSTANCE;
	}
	
	/**
	 * Gibt die aktuelle Karte zurück (für ProfileStatsManager)
	 */
	public static CardData getCurrentCard() {
		return currentCard;
	}
	
	/**
	 * Gibt die aktuelle Statue zurück (für ProfileStatsManager)
	 */
	public static StatueData getCurrentStatue() {
		return currentStatue;
	}
	
	public static void initialize() {
		if (isInitialized) {
			return;
		}
		
		try {
			// Instance erstellen
			new CardsStatuesUtility();
			
			// Register hotkey
			registerHotkey();
			
			// Register commands
			registerCommands();
			
			// Client-seitige Events registrieren
			ClientTickEvents.END_CLIENT_TICK.register(CardsStatuesUtility::onClientTick);
			
			// Registriere HUD-Rendering
			HudElementRegistry.addLast(
					Identifier.fromNamespaceAndPath("cclive-utilities", "cards_statues"),
					CardsStatuesUtility::onHudRender);
			
			// Chat-Nachrichten Event registrieren
			ClientReceiveMessageEvents.GAME.register(CardsStatuesUtility::onChatMessage);
			
			isInitialized = true;
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	private static void registerHotkey() {
		// Register toggle hotkey
		toggleKeyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.cclive-utilities.cards-toggle",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(), // Unbound key
			KeyCategories.of("cclive-utilities", "cards")
		));
	}
	
	private static void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommands.literal("cards-statues")
				.then(ClientCommands.literal("show")
					.executes(context -> {
						showOverlays = true;
						context.getSource().sendFeedback(Component.literal("§aKarten und Statuen Overlay eingeblendet!"));
						return 1;
					})
				)
			);
		});
	}
	
	private static void onClientTick(Minecraft client) {
		// Check Tab key for overlay visibility
		checkTabKey();
		
		// Handle hotkey
		handleHotkey();
		
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().cardsStatuesEnabled) {
			return;
		}
		
		if (client.player == null || client.level == null) {
			return;
		}
		
		// Prüfe auf Dimensionswechsel
		Identifier currentDimension = client.level.dimension().identifier();
		if (lastDimension != null && !lastDimension.equals(currentDimension)) {
			// Dimension hat sich geändert, setze Anzeige zurück
			clear();
		}
		lastDimension = currentDimension;
	}
	
	private static void checkTabKey() {
		// Check if player list key is pressed (respects custom key bindings)
		if (KeyBindingUtility.isPlayerListKeyPressed()) {
			showOverlays = false; // Hide overlays when player list key is pressed
		} else {
			showOverlays = true; // Show overlays when player list key is released
		}
	}
	
	private static void handleHotkey() {
		// Handle toggle hotkey
		if (toggleKeyMapping != null && toggleKeyMapping.consumeClick()) {
			boolean currentShow = CCLiveUtilitiesConfig.HANDLER.instance().showCard || CCLiveUtilitiesConfig.HANDLER.instance().showStatue;
			boolean newValue = !currentShow;
			CCLiveUtilitiesConfig.HANDLER.instance().showCard = newValue;
			CCLiveUtilitiesConfig.HANDLER.instance().showStatue = newValue;
			CCLiveUtilitiesConfig.HANDLER.instance().cardEnabled = newValue;
			CCLiveUtilitiesConfig.HANDLER.instance().statueEnabled = newValue;
			if (newValue) {
				CCLiveUtilitiesConfig.HANDLER.instance().cardsStatuesEnabled = true;
			}
			CCLiveUtilitiesConfig.HANDLER.save();
		}
	}
	
	private static void onChatMessage(Component message, boolean overlay) {
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().cardsStatuesEnabled) {
			return;
		}
		
		// Prüfe ob wir in einer Floor-Dimension sind
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null) {
			return;
		}
		
		Identifier currentDimension = client.level.dimension().identifier();
		String dimensionPath = currentDimension.getPath();
		
		// Prüfe ob die Dimension "floor_" gefolgt von einer Zahl enthält
		if (!dimensionPath.matches(".*floor_\\d+.*")) {
			return; // Nicht in einer Floor-Dimension
		}
		
		String content = message.getString();
		String cleanContent = content.replaceAll("§[0-9a-fk-or]", "");
		// Nur offizielle Legend-Server-Nachrichten (nicht z.B. [CactusAI-Beta])
		if (!cleanContent.contains("[Legend]")) {
			return;
		}

		HoverEvent hoverEvent = message.getStyle().getHoverEvent();
		
		String hoverContent = content; // Verwende den ursprünglichen Text als Basis
		
		if (hoverEvent != null && hoverEvent.action() == HoverEvent.Action.SHOW_TEXT) {
			try {
				// Versuche direkt das Text-Objekt aus dem HoverEvent zu extrahieren
				Component hoverText = extractHoverTextFromEvent(hoverEvent);
				if (hoverText != null) {
					hoverContent = extractAllTextFromComponent(hoverText);
				} else {
					// Fallback: Verwende String-Parsing
					String hoverString = hoverEvent.toString();
					hoverContent = parseHoverEventData(hoverString);
				}
			} catch (Exception e) {
				// Silent error handling("[CardsStatues] ❌ Fehler beim Extrahieren des Hover-Texts: " + e.getMessage());
				// Silent error handling
			}
		}
		
		if ((content.contains("[") && content.contains("]") && content.contains("Karte")) || 
			content.contains("[Karte]")) {
			handleCardMessage(message, hoverContent);
		} else if ((content.contains("[") && content.contains("]") && content.contains("Statue")) || 
				  content.contains("[Statue]")) {
			handleStatueMessage(message, hoverContent);
		}
	}
	
	/**
	 * Extrahiert Text aus einem HoverEvent
	 */
	private static Component extractHoverTextFromEvent(HoverEvent hoverEvent) {
		if (hoverEvent == null || hoverEvent.action() != HoverEvent.Action.SHOW_TEXT) {
			return null;
		}
		
		// Versuche getValue() Methode
		try {
			java.lang.reflect.Method getValueMethod = HoverEvent.class.getDeclaredMethod("getValue", HoverEvent.Action.class);
			getValueMethod.setAccessible(true);
			Object value = getValueMethod.invoke(hoverEvent, HoverEvent.Action.SHOW_TEXT);
			if (value instanceof Component) {
				return (Component) value;
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Versuche value() Methode (für Records)
		try {
			java.lang.reflect.Method valueMethod = HoverEvent.class.getDeclaredMethod("value");
			valueMethod.setAccessible(true);
			Object value = valueMethod.invoke(hoverEvent);
			if (value instanceof Component) {
				return (Component) value;
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Versuche Record-Komponenten
		try {
			Class<?> hoverEventClass = hoverEvent.getClass();
			if (hoverEventClass.isRecord()) {
				java.lang.reflect.RecordComponent[] components = hoverEventClass.getRecordComponents();
				for (java.lang.reflect.RecordComponent component : components) {
					if (Component.class.isAssignableFrom(component.getType())) {
						try {
							Object value = component.getAccessor().invoke(hoverEvent);
							if (value instanceof Component) {
								return (Component) value;
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		
		return null;
	}
	
	/**
	 * Extrahiert rekursiv allen Text aus einem Text-Objekt inkl. Hex-/Legacy-Farben.
	 * Newlines bleiben erhalten; Farben werden als {@code §#RRGGBB} kodiert.
	 */
	private static String extractAllTextFromComponent(Component text) {
		if (text == null) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		text.visit((style, string) -> {
			if (string == null || string.isEmpty()) {
				return Optional.empty();
			}

			int start = 0;
			while (start <= string.length()) {
				int nl = string.indexOf('\n', start);
				boolean hasNewline = nl >= 0;
				String part = hasNewline ? string.substring(start, nl) : string.substring(start);

				if (!part.isEmpty()) {
					TextColor color = style.getColor();
					if (color != null) {
						result.append(String.format(Locale.ROOT, "§#%06X", color.getValue() & 0xFFFFFF));
					}
					result.append(part);
				}

				if (hasNewline) {
					result.append('\n');
					start = nl + 1;
				} else {
					break;
				}
			}
			return Optional.empty();
		}, Style.EMPTY);

		return result.toString();
	}
	
	private static String parseHoverEventData(String hoverString) {
		
		StringBuilder result = new StringBuilder();
		
		// Extrahiere den Hauptnamen mit Farben
		if (hoverString.contains("value=literal{")) {
			int start = hoverString.indexOf("value=literal{") + 14;
			int end = hoverString.indexOf("}", start);
			if (end > start) {
				String mainName = hoverString.substring(start, end);
				// Behalte Farbcodes bei
				result.append(mainName).append("\n");
			}
		}
		
		// Suche nach allen siblings für weitere Informationen
		if (hoverString.contains("siblings=[")) {
			String siblingsPart = hoverString.substring(hoverString.indexOf("siblings=["));
			
			// Extrahiere alle literal{} Teile mit Farben
			String[] parts = siblingsPart.split("literal\\{");
			for (String part : parts) {
				if (part.contains("}")) {
					int end = part.indexOf("}");
					if (end > 0) {
						String text = part.substring(0, end);
						// Behalte Farbcodes bei und füge hinzu
						if (!text.trim().isEmpty()) {
							result.append(text).append("\n");
						}
					}
				}
			}
		}
		
		String finalResult = result.toString().trim();
		return finalResult;
	}
	
	private static void handleCardMessage(Component message, String hoverContent) {
		
		CardData cardData = new CardData();
		cardData.setColor(getColorFromStyle(message.getStyle()));
		
		// Parse die geparsten Hover-Daten
		List<String> hoverLines = new ArrayList<>();
		java.util.Set<String> seenLines = new java.util.HashSet<>(); // Verhindere Duplikate
		String[] lines = hoverContent.split("\n");
		
		for (String line : lines) {
			String trimmed = stripPixelSpacers(line).trim();
			if (!trimmed.isEmpty()) {
				// Entferne Formatierungscodes für Vergleich (um Duplikate zu erkennen)
				String cleanForComparison = stripFormatting(trimmed).trim();
				if (cleanForComparison.isEmpty()) {
					continue;
				}
				// Füge nur hinzu, wenn wir diese Zeile noch nicht gesehen haben
				if (!seenLines.contains(cleanForComparison)) {
					seenLines.add(cleanForComparison);
					hoverLines.add(trimmed);
				}
			}
		}
		
		cardData.setHoverLines(mergeCardStarAndDiamondLines(hoverLines));
		
		// Extrahiere Daten aus den geparsten Linien
		if (!hoverLines.isEmpty()) {
			// Entferne Formatierungscodes aus dem Namen
			String rawName = hoverLines.get(0);
			String cleanName = stripFormatting(rawName).trim();
			// Entferne "[Karte]" aus dem Namen
			cleanName = cleanName.replaceAll("\\[Karte\\]", "").trim();
			cardData.setName(cleanName);
		}
		
		for (String line : hoverLines) {
			// Karten-Level: Zähle Sterne (⭐)
			int starCount = countStars(line);
			if (starCount > 0) {
				cardData.setLevel(String.valueOf(starCount));
			}
			
			// Backup: Maximale Stufe erreicht
			if (line.contains("Maximale Stufe erreicht!")) {
				cardData.setLevel("5");
			}
			
			// Nächste Stufe (für Anzeige, nicht für Level-Tracking)
			if (line.contains("Nächste Stufe:")) {
				String nextLevel = stripFormatting(line).replaceAll("[^0-9]", "").trim();
				if (!nextLevel.isEmpty()) {
					cardData.setNextLevel(nextLevel);
				}
			} else if (stripFormatting(line).startsWith("+") && !line.contains("Stufe")) {
				cardData.setEffect(line);
			}
		}
		
		currentCard = cardData;
		
		// Informiere ProfileStatsManager sofort über die neue Karte
		try {
			net.felix.profile.ProfileStatsManager.getInstance().onCardFromChat(cardData);
		} catch (Exception e) {
			// Silent error handling("[CardsStatues] ❌ Fehler beim Aufruf von onCardFromChat: " + e.getMessage());
			// Silent error handling
		}
	}
	
	private static void handleStatueMessage(Component message, String hoverContent) {
		StatueData statueData = new StatueData();
		statueData.setColor(getColorFromStyle(message.getStyle()));
		
		// Parse die geparsten Hover-Daten
		List<String> hoverLines = new ArrayList<>();
		java.util.Set<String> seenLines = new java.util.HashSet<>(); // Verhindere Duplikate
		String[] lines = hoverContent.split("\n");
		
		for (String line : lines) {
			String trimmed = stripPixelSpacers(line).trim();
			if (!trimmed.isEmpty()) {
				// Entferne Formatierungscodes für Vergleich (um Duplikate zu erkennen)
				String cleanForComparison = stripFormatting(trimmed).trim();
				if (cleanForComparison.isEmpty()) {
					continue;
				}
				// Füge nur hinzu, wenn wir diese Zeile noch nicht gesehen haben
				if (!seenLines.contains(cleanForComparison)) {
					seenLines.add(cleanForComparison);
					hoverLines.add(trimmed);
				}
			}
		}
		
		statueData.setHoverLines(hoverLines);
		
		// Extrahiere Daten aus den geparsten Linien
		if (!hoverLines.isEmpty()) {
			// Entferne Formatierungscodes aus dem Namen
			String rawName = hoverLines.get(0);
			String cleanName = stripFormatting(rawName).trim();
			// Entferne "[Statue]" aus dem Namen
			cleanName = cleanName.replaceAll("\\[Statue\\]", "").trim();
			statueData.setName(cleanName);
		}
		
		for (String line : hoverLines) {
			// Statuen-Level: Suche nach "Stufe" (mit oder ohne Doppelpunkt, z.B. "Stufe 12" oder "Stufe: 12")
			if (line.contains("Stufe") && !line.contains("Nächste")) {
				// Entferne Formatierungscodes für bessere Suche
				String cleanLine = stripFormatting(line);
				// Suche nach "Stufe" gefolgt von einer Zahl
				java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Stufe[\\s:]*?(\\d+)");
				java.util.regex.Matcher matcher = pattern.matcher(cleanLine);
				if (matcher.find()) {
					String level = matcher.group(1);
					if (!level.isEmpty()) {
						statueData.setLevel(level);
					}
				}
			}
			
			// Backup: Maximale Stufe erreicht (Level 40)
			if (line.contains("Maximale Stufe erreicht!")) {
				statueData.setLevel("40");
			}
			
			// Nächste Stufe (für Anzeige, nicht für Level-Tracking)
			if (line.contains("Nächste Stufe:")) {
				String nextLevel = stripFormatting(line).replaceAll("[^0-9]", "").trim();
				if (!nextLevel.isEmpty()) {
					statueData.setNextLevel(nextLevel);
				}
			} else if (stripFormatting(line).startsWith("+") && !line.contains("Stufe")) {
				statueData.setEffect(line);
			}
		}
		
		currentStatue = statueData;
		
		// Informiere ProfileStatsManager sofort über die neue Statue
		try {
			net.felix.profile.ProfileStatsManager.getInstance().onStatueFromChat(statueData);
		} catch (Exception e) {
			// Silent error handling("[CardsStatues] ❌ Fehler beim Aufruf von onStatueFromChat: " + e.getMessage());
			// Silent error handling
		}
	}
	

	
	private static String getColorFromStyle(Style style) {
		if (style.getColor() != null) {
			return style.getColor().serialize();
		}
		return ChatFormatting.WHITE.getName();
	}
	
	/**
	 * Zählt die Anzahl der Sterne (⭐) in einer Zeile
	 * @param line Die zu prüfende Zeile
	 * @return Anzahl der Sterne (0-5)
	 */
	private static int countStars(String line) {
		if (line == null || line.isEmpty()) {
			return 0;
		}
		// Zähle alle ⭐ Zeichen (U+2B50)
		int count = 0;
		for (char c : line.toCharArray()) {
			if (c == '⭐') {
				count++;
			}
		}
		// Begrenze auf maximal 5 (sollte nicht passieren, aber sicherheitshalber)
		return Math.min(count, 5);
	}

	/**
	 * Packt Rauten (♦) aus dem Hover in dieselbe Zeile wie die Sterne (⭐),
	 * mit genau 3 Leerzeichen dazwischen und behält die Hover-Farben:
	 * {@code §#6E627D⭐⭐⭐   §#FFB600♦}
	 */
	private static List<String> mergeCardStarAndDiamondLines(List<String> hoverLines) {
		if (hoverLines == null || hoverLines.isEmpty()) {
			return hoverLines;
		}

		List<String> merged = new ArrayList<>();
		SymbolRun pendingDiamonds = null;

		for (String line : hoverLines) {
			SymbolRun stars = extractSymbolRun(line, '⭐', "§#6E627D");
			SymbolRun diamonds = extractSymbolRun(line, '♦', "§#FFB600");
			boolean symbolOnlyLine = isOnlyStarsAndOrDiamonds(line);

			if (symbolOnlyLine && diamonds.count() > 0 && stars.count() == 0) {
				int starIdx = findLastLineContaining(merged, '⭐');
				if (starIdx >= 0) {
					merged.set(starIdx, buildStarDiamondLine(extractSymbolRun(merged.get(starIdx), '⭐', "§#6E627D"), diamonds));
				} else {
					pendingDiamonds = diamonds;
				}
				continue;
			}

			if (symbolOnlyLine && stars.count() > 0 && diamonds.count() == 0) {
				SymbolRun diamondRun = pendingDiamonds;
				pendingDiamonds = null;
				if (diamondRun != null && diamondRun.count() > 0) {
					merged.add(buildStarDiamondLine(stars, diamondRun));
				} else {
					merged.add(buildStarDiamondLine(stars, null));
				}
				continue;
			}

			if (symbolOnlyLine && stars.count() > 0 && diamonds.count() > 0) {
				pendingDiamonds = null;
				merged.add(buildStarDiamondLine(stars, diamonds));
				continue;
			}

			if (pendingDiamonds != null) {
				int starIdx = findLastLineContaining(merged, '⭐');
				if (starIdx >= 0) {
					merged.set(starIdx, buildStarDiamondLine(extractSymbolRun(merged.get(starIdx), '⭐', "§#6E627D"), pendingDiamonds));
				} else {
					merged.add(pendingDiamonds.toFormatted());
				}
				pendingDiamonds = null;
			}

			merged.add(line);
		}

		if (pendingDiamonds != null) {
			int starIdx = findLastLineContaining(merged, '⭐');
			if (starIdx >= 0) {
				merged.set(starIdx, buildStarDiamondLine(extractSymbolRun(merged.get(starIdx), '⭐', "§#6E627D"), pendingDiamonds));
			} else {
				merged.add(pendingDiamonds.toFormatted());
			}
		}

		return merged;
	}

	private static String stripFormatting(String text) {
		if (text == null) {
			return "";
		}
		return text
				.replaceAll("§#[0-9a-fA-F]{6}", "")
				.replaceAll("§[0-9a-fk-or]", "");
	}

	private static String stripPixelSpacers(String text) {
		if (text == null || text.isEmpty()) {
			return "";
		}
		// Pixel-Spacer liegen im CJK Extension-A-Bereich (u. a. 㔛㔙㔘㔖)
		return text.replaceAll("[\\u3400-\\u4DBF]", "");
	}

	private static boolean isOnlyStarsAndOrDiamonds(String line) {
		String clean = stripFormatting(line).trim();
		if (clean.isEmpty()) {
			return false;
		}
		for (int i = 0; i < clean.length(); i++) {
			char c = clean.charAt(i);
			if (c != '⭐' && c != '♦' && !Character.isWhitespace(c)) {
				return false;
			}
		}
		return clean.indexOf('⭐') >= 0 || clean.indexOf('♦') >= 0;
	}

	private static int findLastLineContaining(List<String> lines, char symbol) {
		for (int i = lines.size() - 1; i >= 0; i--) {
			if (lines.get(i).indexOf(symbol) >= 0) {
				return i;
			}
		}
		return -1;
	}

	private static SymbolRun extractSymbolRun(String line, char symbol, String defaultColorCode) {
		if (line == null || line.isEmpty()) {
			return SymbolRun.empty();
		}

		String lastColor = "";
		String runColor = "";
		StringBuilder symbols = new StringBuilder();

		for (int i = 0; i < line.length(); ) {
			char c = line.charAt(i);
			if (c == '§' && i + 1 < line.length()) {
				if (line.charAt(i + 1) == '#' && i + 7 < line.length()) {
					lastColor = line.substring(i, i + 8);
					i += 8;
					continue;
				}
				lastColor = line.substring(i, i + 2);
				i += 2;
				continue;
			}
			if (c == symbol) {
				if (symbols.length() == 0) {
					runColor = lastColor;
				}
				symbols.append(c);
			}
			i++;
		}

		if (symbols.length() == 0) {
			return SymbolRun.empty();
		}
		if (runColor.isEmpty()) {
			runColor = defaultColorCode != null ? defaultColorCode : "";
		}
		return new SymbolRun(runColor, symbols.toString());
	}

	private static String buildStarDiamondLine(SymbolRun stars, SymbolRun diamonds) {
		StringBuilder out = new StringBuilder();
		if (stars != null && stars.count() > 0) {
			out.append(stars.toFormatted());
		}
		if (diamonds != null && diamonds.count() > 0) {
			if (out.length() > 0) {
				out.append("   ");
			}
			out.append(diamonds.toFormatted());
		}
		return out.toString();
	}

	private static final class SymbolRun {
		private final String colorCode;
		private final String symbols;

		private SymbolRun(String colorCode, String symbols) {
			this.colorCode = colorCode != null ? colorCode : "";
			this.symbols = symbols != null ? symbols : "";
		}

		private static SymbolRun empty() {
			return new SymbolRun("", "");
		}

		private int count() {
			return symbols.length();
		}

		private String toFormatted() {
			return colorCode + symbols;
		}
	}

	/**
	 * Wandelt Zeilen mit {@code §#RRGGBB} / Legacy-{@code §}-Codes in ein farbiges Text um.
	 */
	private static Component parseFormattedLine(String line) {
		MutableComponent root = Component.empty();
		if (line == null || line.isEmpty()) {
			return root;
		}

		Style current = Style.EMPTY;
		StringBuilder buffer = new StringBuilder();

		for (int i = 0; i < line.length(); ) {
			char c = line.charAt(i);
			if (c == '§' && i + 1 < line.length()) {
				flushFormattedBuffer(root, buffer, current);
				if (line.charAt(i + 1) == '#' && i + 7 < line.length()) {
					String hex = line.substring(i + 2, i + 8);
					try {
						int rgb = Integer.parseInt(hex, 16);
						current = Style.EMPTY.withColor(rgb);
						i += 8;
						continue;
					} catch (NumberFormatException ignored) {
						// Fall through to legacy / literal handling
					}
				}

				ChatFormatting formatting = ChatFormatting.getByCode(line.charAt(i + 1));
				if (formatting != null) {
					if (formatting == ChatFormatting.RESET) {
						current = Style.EMPTY;
					} else if (formatting.isColor()) {
						current = Style.EMPTY.withColor(formatting);
					} else {
						current = current.applyFormat(formatting);
					}
					i += 2;
					continue;
				}
			}

			buffer.append(c);
			i++;
		}

		flushFormattedBuffer(root, buffer, current);
		return root;
	}

	private static void flushFormattedBuffer(MutableComponent root, StringBuilder buffer, Style style) {
		if (buffer.length() == 0) {
			return;
		}
		root.append(Component.literal(buffer.toString()).setStyle(style));
		buffer.setLength(0);
	}
	
	public static void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		
		// Prüfe ob die Cards/Statues Utility aktiviert ist
		if (!config.cardsStatuesEnabled) {
			return;
		}
		if (!net.felix.utilities.Overall.HudOverlayVisibility.shouldRenderWorldHudOverlays()) {
			return;
		}
		
		// Prüfe ob wir in einer Welt sind (ohne weitere Bedingungen)
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null || client.player == null) {
			return;
		}
		
		// Hide overlay if F1 menu (debug screen) is open
		if (client.options.hideGui) {
			return;
		}
		
		// Prüfe ob mindestens eine Anzeige aktiviert ist
		if (!config.showCard && !config.showStatue) {
			return;
		}
		
		// Prüfe ob wir in einer "floor_" Dimension sind
		String dimension = client.level.dimension().identifier().toString();
		if (!dimension.contains("floor_")) {
			return;
		}
		
		// Berechne Positionen basierend auf Bildschirmgröße und Config
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		
		// Karten-Position (unten rechts)
		int cardX = screenWidth - config.cardX;
		int cardY = screenHeight - config.cardY;
		
		// Statuen-Position (unten rechts, über den Karten)
		int statueX = screenWidth - config.statueX;
		int statueY = screenHeight - config.statueY;
		
		// Render nur wenn Overlays sichtbar sind und keine Equipment-Overlays aktiv sind
		if (showOverlays && !EquipmentDisplayUtility.isEquipmentOverlayActive()) {
			// Rendere Karten-Overlay
			if (config.cardEnabled && config.showCard && currentCard != null) {
				renderCardInfo(context, currentCard, cardX, cardY);
			}
			
			// Rendere Statuen-Overlay
			if (config.statueEnabled && config.showStatue && currentStatue != null) {
				renderStatueInfo(context, currentStatue, statueX, statueY);
			}
		}
	}
	

	
	private static void renderCardInfo(GuiGraphicsExtractor context, CardData card, int x, int y) {
		// Zeichne Karten-Hintergrund basierend auf dem Overlay-Typ
		OverlayType cardOverlayType = CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayType;
		if (cardOverlayType == OverlayType.CUSTOM || cardOverlayType == OverlayType.BLACK) {
			renderCardBackground(context, x, y);
		}
		// Bei OverlayType.NONE wird kein Hintergrund gezeichnet
		
		// Verwende Matrix-Transformationen für Text-Skalierung
		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		
		// Skaliere den Text basierend auf der Config
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayScale;
		if (scale <= 0) scale = 1.0f; // Sicherheitscheck
		
		// Übersetze zur Position und skaliere von dort aus
		// Jetzt skaliert der Text von der gleichen Position aus wie der Hintergrund
		matrices.translate(x, y);
		matrices.scale(scale, scale);
		
		// Zusätzliche Text-Skalierung (nur für Text, nicht für Hintergrund)
		float textScale = CCLiveUtilitiesConfig.HANDLER.instance().cardTextScale;
		if (textScale <= 0) textScale = 1.0f; // Sicherheitscheck
		if (textScale > 1.5f) textScale = 1.5f; // Max 1.5
		
		// Zeige alle Hover-Linien an, außer "Statistik" und leere Zeilen
		int lineCount = 0;
		for (int i = 0; i < card.getHoverLines().size(); i++) {
			String line = card.getHoverLines().get(i);
			String cleanLine = stripFormatting(stripPixelSpacers(line)).trim();
			
			// Überspringe "Statistik", leere Zeilen und reine Pixel-Spacer-Zeilen
			if (line.contains("Statistik") || cleanLine.isEmpty()) {
				continue;
			}
			
			// Überspringe auch die Zeile vor "Statistik" (leere Zeile), aber NICHT "Nächste Stufe"
			if (i < card.getHoverLines().size() - 1 && card.getHoverLines().get(i + 1).contains("Statistik")) {
				// Überspringe nur, wenn es keine "Nächste Stufe" Zeile ist
				if (!cleanLine.contains("Nächste Stufe")) {
					continue;
				}
			}
			
			// Berechne die Y-Position basierend auf der Anzahl der Zeilen
			// Verwende relative Positionen (0-basiert) da wir bereits übersetzt haben
			// Der Text soll die gleichen Abstände zu den Rändern haben wie bei der originalen Größe
			int textY = -1 + (lineCount * 12);
			
			// Wende zusätzliche Text-Skalierung an
			matrices.pushMatrix();
			matrices.translate(1, textY); // Verschiebe zur Text-Position
			matrices.scale(textScale, textScale); // Skaliere nur den Text
			matrices.translate(-1, -textY); // Verschiebe zurück
			
			// Erstelle Text-Objekt mit übernommenen Hover-Farben (§#RRGGBB)
			Component textComponent = parseFormattedLine(line);
			
			// Rendere den Text mit den ursprünglichen Farben
			context.text(
				Minecraft.getInstance().font, 
				textComponent, 
				1, // Verwende 1 da wir bereits übersetzt haben (1 Pixel nach rechts verschoben)
				textY, 
				0xFFFFFFFF, // Weiß als Fallback, aber Text-Objekt behält eigene Farben
				true
			);
			
			matrices.popMatrix(); // Entferne Text-Skalierung
			lineCount++;
		}
		
		// Matrix-Transformationen wiederherstellen
		matrices.popMatrix();
	}
	
	private static void renderStatueInfo(GuiGraphicsExtractor context, StatueData statue, int x, int y) {
		// Zeichne Statuen-Hintergrund basierend auf dem Overlay-Typ
		OverlayType statueOverlayType = CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayType;
		if (statueOverlayType == OverlayType.CUSTOM || statueOverlayType == OverlayType.BLACK) {
			renderStatueBackground(context, x, y);
		}
		// Bei OverlayType.NONE wird kein Hintergrund gezeichnet
		
		// Verwende Matrix-Transformationen für Text-Skalierung
		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		
		// Skaliere den Text basierend auf der Config
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayScale;
		if (scale <= 0) scale = 1.0f; // Sicherheitscheck
		
		// Übersetze zur Position und skaliere von dort aus
		// Jetzt skaliert der Text von der gleichen Position aus wie der Hintergrund
		matrices.translate(x, y);
		matrices.scale(scale, scale);
		
		// Zusätzliche Text-Skalierung (nur für Text, nicht für Hintergrund)
		float textScale = CCLiveUtilitiesConfig.HANDLER.instance().statueTextScale;
		if (textScale <= 0) textScale = 1.0f; // Sicherheitscheck
		if (textScale > 1.5f) textScale = 1.5f; // Max 1.5
		
		// Zeige alle Hover-Linien an, außer "Statistik" und leere Zeilen
		int lineCount = 0;
		for (int i = 0; i < statue.getHoverLines().size(); i++) {
			String line = statue.getHoverLines().get(i);
			String cleanLine = stripFormatting(stripPixelSpacers(line)).trim();
			
			// Überspringe "Statistik", leere Zeilen und reine Pixel-Spacer-Zeilen
			if (line.contains("Statistik") || cleanLine.isEmpty()) {
				continue;
			}
			
			// Überspringe auch die Zeile vor "Statistik" (leere Zeile), aber NICHT "Nächste Stufe"
			if (i < statue.getHoverLines().size() - 1 && statue.getHoverLines().get(i + 1).contains("Statistik")) {
				// Überspringe nur, wenn es keine "Nächste Stufe" Zeile ist
				if (!cleanLine.contains("Nächste Stufe")) {
					continue;
				}
			}
			
			// Berechne die Y-Position basierend auf der Anzahl der Zeilen
			// Verwende relative Positionen (0-basiert) da wir bereits übersetzt haben
			// Der Text soll die gleichen Abstände zu den Rändern haben wie bei der originalen Größe
			int textY = -1 + (lineCount * 12);
			
			// Wende zusätzliche Text-Skalierung an
			matrices.pushMatrix();
			matrices.translate(1, textY); // Verschiebe zur Text-Position
			matrices.scale(textScale, textScale); // Skaliere nur den Text
			matrices.translate(-1, -textY); // Verschiebe zurück
			
			// Erstelle Text-Objekt mit übernommenen Hover-Farben (§#RRGGBB)
			Component textComponent = parseFormattedLine(line);
			
			// Rendere den Text mit den ursprünglichen Farben
			context.text(
				Minecraft.getInstance().font, 
				textComponent, 
				1, // Verwende 1 da wir bereits übersetzt haben (1 Pixel nach rechts verschoben)
				textY, 
				0xFFFFFFFF, // Weiß als Fallback, aber Text-Objekt behält eigene Farben
				true
			);
			
			matrices.popMatrix(); // Entferne Text-Skalierung
			lineCount++;
		}
		
		// Matrix-Transformationen wiederherstellen
		matrices.popMatrix();
	}
	

	
	public static void clear() {
		currentCard = null;
		currentStatue = null;
	}
	

	
	// Datenklassen für Karten und Statuen
	public static class CardData {
		private String name;
		private String level;
		private String nextLevel;
		private String effect;
		private String color;
		private List<String> hoverLines;
		
		public CardData() {
			this.hoverLines = new ArrayList<>();
		}
		
		// Getter und Setter
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		
		public String getLevel() { return level; }
		public void setLevel(String level) { this.level = level; }
		
		public String getNextLevel() { return nextLevel; }
		public void setNextLevel(String nextLevel) { this.nextLevel = nextLevel; }
		
		public String getEffect() { return effect; }
		public void setEffect(String effect) { this.effect = effect; }
		
		public String getColor() { return color; }
		public void setColor(String color) { this.color = color; }
		
		public List<String> getHoverLines() { return hoverLines; }
		public void setHoverLines(List<String> hoverLines) { this.hoverLines = hoverLines; }
	}
	
	public static class StatueData {
		private String name;
		private String level;
		private String nextLevel;
		private String effect;
		private String color;
		private List<String> hoverLines;
		
		public StatueData() {
			this.hoverLines = new ArrayList<>();
		}
		
		// Getter und Setter
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		
		public String getLevel() { return level; }
		public void setLevel(String level) { this.level = level; }
		
		public String getNextLevel() { return nextLevel; }
		public void setNextLevel(String nextLevel) { this.nextLevel = nextLevel; }
		
		public String getEffect() { return effect; }
		public void setEffect(String effect) { this.effect = effect; }
		
		public String getColor() { return color; }
		public void setColor(String color) { this.color = color; }
		
		public List<String> getHoverLines() { return hoverLines; }
		public void setHoverLines(List<String> hoverLines) { this.hoverLines = hoverLines; }
	}
}
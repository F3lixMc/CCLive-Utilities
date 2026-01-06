package net.felix.utilities.Aincraft;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.client.gl.RenderPipelines;
import org.joml.Matrix3x2fStack;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.OverlayType;
import net.felix.utilities.Overall.KeyBindingUtility;
import net.felix.utilities.Overall.ZeichenUtility;
import net.felix.utilities.Town.EquipmentDisplayUtility;


import java.util.ArrayList;
import java.util.List;

public class CardsStatuesUtility {
	

	private static boolean isInitialized = false;
	private static boolean showOverlays = true; // Neue Variable für Overlay-Sichtbarkeit
	private static CardsStatuesUtility INSTANCE;
	
	// Hotkey variable
	private static KeyBinding toggleKeyBinding;
	
	// Daten für Karten und Statuen
	private static CardData currentCard = null;
	private static StatueData currentStatue = null;
	private static Identifier lastDimension = null;
	

	
	// Rendering-Konstanten
	private static final int BACKGROUND_WIDTH = 162;
	private static final int BACKGROUND_HEIGHT = 62;
	
	// Textur-Identifier für den Karten-Hintergrund
	private static final Identifier CARD_BACKGROUND_TEXTURE = Identifier.of("cclive-utilities", "textures/gui/karten_background.png");
	
	// Textur-Identifier für den Statuen-Hintergrund
	private static final Identifier STATUE_BACKGROUND_TEXTURE = Identifier.of("cclive-utilities", "textures/gui/statuen_background.png");
	
	/**
	 * Rendert den Karten-Hintergrund basierend auf dem Overlay-Typ
	 */
	private static void renderCardBackground(DrawContext context, int x, int y) {
		OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayType;
		
		// Verwende Matrix-Transformationen für Skalierung
		Matrix3x2fStack matrices = context.getMatrices();
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
				context.drawTexture(
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
	private static void renderStatueBackground(DrawContext context, int x, int y) {
		OverlayType overlayType = CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayType;
		
		// Verwende Matrix-Transformationen für Skalierung
		Matrix3x2fStack matrices = context.getMatrices();
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
				context.drawTexture(
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
			HudRenderCallback.EVENT.register((drawContext, tickDelta) -> onHudRender(drawContext, tickDelta));
			
			// Chat-Nachrichten Event registrieren
			ClientReceiveMessageEvents.GAME.register(CardsStatuesUtility::onChatMessage);
			
			isInitialized = true;
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	private static void registerHotkey() {
		// Register toggle hotkey
		toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.cclive-utilities.cards-toggle",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(), // Unbound key
			"category.cclive-utilities.cards"
		));
	}
	
	private static void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("cards-statues")
				.then(ClientCommandManager.literal("show")
					.executes(context -> {
						showOverlays = true;
						context.getSource().sendFeedback(Text.literal("§aKarten und Statuen Overlay eingeblendet!"));
						return 1;
					})
				)
			);
		});
	}
	
	private static void onClientTick(MinecraftClient client) {
		// Check Tab key for overlay visibility
		checkTabKey();
		
		// Handle hotkey
		handleHotkey();
		
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().cardsStatuesEnabled) {
			return;
		}
		
		if (client.player == null || client.world == null) {
			return;
		}
		
		// Prüfe auf Dimensionswechsel
		Identifier currentDimension = client.world.getRegistryKey().getValue();
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
		if (toggleKeyBinding != null && toggleKeyBinding.wasPressed()) {
			boolean currentShow = CCLiveUtilitiesConfig.HANDLER.instance().showCard || CCLiveUtilitiesConfig.HANDLER.instance().showStatue;
			CCLiveUtilitiesConfig.HANDLER.instance().showCard = !currentShow;
			CCLiveUtilitiesConfig.HANDLER.instance().showStatue = !currentShow;
			CCLiveUtilitiesConfig.HANDLER.save();
		}
	}
	
	private static void onChatMessage(Text message, boolean overlay) {
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().cardsStatuesEnabled) {
			return;
		}
		
		// Prüfe ob wir in einer Floor-Dimension sind
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.world == null) {
			return;
		}
		
		Identifier currentDimension = client.world.getRegistryKey().getValue();
		String dimensionPath = currentDimension.getPath();
		
		// Prüfe ob die Dimension "floor_" gefolgt von einer Zahl enthält
		if (!dimensionPath.matches(".*floor_\\d+.*")) {
			return; // Nicht in einer Floor-Dimension
		}
		
		String content = message.getString();
		HoverEvent hoverEvent = message.getStyle().getHoverEvent();
		
		String hoverContent = content; // Verwende den ursprünglichen Text als Basis
		
		if (hoverEvent != null && hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT) {
			try {
				// Versuche direkt das Text-Objekt aus dem HoverEvent zu extrahieren
				Text hoverText = extractHoverTextFromEvent(hoverEvent);
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
	private static Text extractHoverTextFromEvent(HoverEvent hoverEvent) {
		if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) {
			return null;
		}
		
		// Versuche getValue() Methode
		try {
			java.lang.reflect.Method getValueMethod = HoverEvent.class.getDeclaredMethod("getValue", HoverEvent.Action.class);
			getValueMethod.setAccessible(true);
			Object value = getValueMethod.invoke(hoverEvent, HoverEvent.Action.SHOW_TEXT);
			if (value instanceof Text) {
				return (Text) value;
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Versuche value() Methode (für Records)
		try {
			java.lang.reflect.Method valueMethod = HoverEvent.class.getDeclaredMethod("value");
			valueMethod.setAccessible(true);
			Object value = valueMethod.invoke(hoverEvent);
			if (value instanceof Text) {
				return (Text) value;
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
					if (Text.class.isAssignableFrom(component.getType())) {
						try {
							Object value = component.getAccessor().invoke(hoverEvent);
							if (value instanceof Text) {
								return (Text) value;
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
	 * Extrahiert rekursiv allen Text aus einem Text-Objekt (inklusive Siblings und verschachtelte Strukturen)
	 */
	private static String extractAllTextFromComponent(Text text) {
		if (text == null) {
			return "";
		}
		
		StringBuilder result = new StringBuilder();
		
		// Füge den Haupttext hinzu (mit Formatierungscodes)
		String mainText = text.getString();
		if (mainText != null && !mainText.isEmpty()) {
			result.append(mainText);
		}
		
		// Rekursiv alle Siblings durchgehen
		for (Text sibling : text.getSiblings()) {
			String siblingText = extractAllTextFromComponent(sibling);
			if (!siblingText.isEmpty()) {
				// Füge Newline hinzu, wenn nötig
				if (result.length() > 0) {
					result.append("\n");
				}
				result.append(siblingText);
			}
		}
		
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
	
	private static void handleCardMessage(Text message, String hoverContent) {
		
		CardData cardData = new CardData();
		cardData.setColor(getColorFromStyle(message.getStyle()));
		
		// Parse die geparsten Hover-Daten
		List<String> hoverLines = new ArrayList<>();
		java.util.Set<String> seenLines = new java.util.HashSet<>(); // Verhindere Duplikate
		String[] lines = hoverContent.split("\n");
		
		for (String line : lines) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty()) {
				// Entferne Formatierungscodes für Vergleich (um Duplikate zu erkennen)
				String cleanForComparison = trimmed.replaceAll("§[0-9a-fk-or]", "").trim();
				// Füge nur hinzu, wenn wir diese Zeile noch nicht gesehen haben
				if (!seenLines.contains(cleanForComparison)) {
					seenLines.add(cleanForComparison);
					hoverLines.add(trimmed);
				}
			}
		}
		
		cardData.setHoverLines(hoverLines);
		
		// Extrahiere Daten aus den geparsten Linien
		if (!hoverLines.isEmpty()) {
			// Entferne Formatierungscodes aus dem Namen
			String rawName = hoverLines.get(0);
			String cleanName = rawName.replaceAll("§[0-9a-fk-or]", "").trim();
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
				String nextLevel = line.replaceAll("[^0-9]", "").trim();
				if (!nextLevel.isEmpty()) {
					cardData.setNextLevel(nextLevel);
				}
			} else if (line.startsWith("+") && !line.contains("Stufe")) {
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
	
	private static void handleStatueMessage(Text message, String hoverContent) {
		StatueData statueData = new StatueData();
		statueData.setColor(getColorFromStyle(message.getStyle()));
		
		// Parse die geparsten Hover-Daten
		List<String> hoverLines = new ArrayList<>();
		java.util.Set<String> seenLines = new java.util.HashSet<>(); // Verhindere Duplikate
		String[] lines = hoverContent.split("\n");
		
		for (String line : lines) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty()) {
				// Entferne Formatierungscodes für Vergleich (um Duplikate zu erkennen)
				String cleanForComparison = trimmed.replaceAll("§[0-9a-fk-or]", "").trim();
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
			String cleanName = rawName.replaceAll("§[0-9a-fk-or]", "").trim();
			// Entferne "[Statue]" aus dem Namen
			cleanName = cleanName.replaceAll("\\[Statue\\]", "").trim();
			statueData.setName(cleanName);
		}
		
		for (String line : hoverLines) {
			// Statuen-Level: Suche nach "Stufe" (mit oder ohne Doppelpunkt, z.B. "Stufe 12" oder "Stufe: 12")
			if (line.contains("Stufe") && !line.contains("Nächste")) {
				// Entferne Formatierungscodes für bessere Suche
				String cleanLine = line.replaceAll("§[0-9a-fk-or]", "");
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
				String nextLevel = line.replaceAll("[^0-9]", "").trim();
				if (!nextLevel.isEmpty()) {
					statueData.setNextLevel(nextLevel);
				}
			} else if (line.startsWith("+") && !line.contains("Stufe")) {
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
			return style.getColor().getName();
		}
		return Formatting.WHITE.getName();
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
	
	private static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		
		// Prüfe ob die Cards/Statues Utility aktiviert ist
		if (!config.cardsStatuesEnabled) {
			return;
		}
		
		// Prüfe ob wir in einer Welt sind (ohne weitere Bedingungen)
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.world == null || client.player == null) {
			return;
		}
		
		// Hide overlay if F1 menu (debug screen) is open
		if (client.options.hudHidden) {
			return;
		}
		
		// Prüfe ob mindestens eine Anzeige aktiviert ist
		if (!config.showCard && !config.showStatue) {
			return;
		}
		
		// Prüfe ob wir in einer "floor_" Dimension sind
		String dimension = client.world.getRegistryKey().getValue().toString();
		if (!dimension.contains("floor_")) {
			return;
		}
		
		// Berechne Positionen basierend auf Bildschirmgröße und Config
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		
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
	

	
	private static void renderCardInfo(DrawContext context, CardData card, int x, int y) {
		// Zeichne Karten-Hintergrund basierend auf dem Overlay-Typ
		OverlayType cardOverlayType = CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayType;
		if (cardOverlayType == OverlayType.CUSTOM || cardOverlayType == OverlayType.BLACK) {
			renderCardBackground(context, x, y);
		}
		// Bei OverlayType.NONE wird kein Hintergrund gezeichnet
		
		// Verwende Matrix-Transformationen für Text-Skalierung
		Matrix3x2fStack matrices = context.getMatrices();
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
			
			// Überspringe "Statistik", leere Zeilen und Zeilen mit unsichtbaren chinesischen Zeichen
			if (line.contains("Statistik") || 
				line.trim().isEmpty() || 
				ZeichenUtility.containsPixelSpacer(line)) {
				continue;
			}
			
			// Überspringe auch die Zeile vor "Statistik" (leere Zeile), aber NICHT "Nächste Stufe"
			if (i < card.getHoverLines().size() - 1 && card.getHoverLines().get(i + 1).contains("Statistik")) {
				// Überspringe nur, wenn es keine "Nächste Stufe" Zeile ist
				String cleanLine = line.replaceAll("§[0-9a-fk-or]", "").trim();
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
			
			// Erstelle Text-Objekt mit Farbcodes
			Text textComponent = Text.literal(line);
			
			// Rendere den Text mit den ursprünglichen Farben
			context.drawText(
				MinecraftClient.getInstance().textRenderer, 
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
	
	private static void renderStatueInfo(DrawContext context, StatueData statue, int x, int y) {
		// Zeichne Statuen-Hintergrund basierend auf dem Overlay-Typ
		OverlayType statueOverlayType = CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayType;
		if (statueOverlayType == OverlayType.CUSTOM || statueOverlayType == OverlayType.BLACK) {
			renderStatueBackground(context, x, y);
		}
		// Bei OverlayType.NONE wird kein Hintergrund gezeichnet
		
		// Verwende Matrix-Transformationen für Text-Skalierung
		Matrix3x2fStack matrices = context.getMatrices();
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
			
			// Überspringe "Statistik", leere Zeilen und Zeilen mit unsichtbaren chinesischen Zeichen
			if (line.contains("Statistik" ) || 
				line.trim().isEmpty() || 
				ZeichenUtility.containsPixelSpacer(line)) {
				continue;
			}
			
			// Überspringe auch die Zeile vor "Statistik" (leere Zeile), aber NICHT "Nächste Stufe"
			if (i < statue.getHoverLines().size() - 1 && statue.getHoverLines().get(i + 1).contains("Statistik" )) {
				// Überspringe nur, wenn es keine "Nächste Stufe" Zeile ist
				String cleanLine = line.replaceAll("§[0-9a-fk-or]", "").trim();
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
			
			// Erstelle Text-Objekt mit Farbcodes
			Text textComponent = Text.literal(line);
			
			// Rendere den Text mit den ursprünglichen Farben
			context.drawText(
				MinecraftClient.getInstance().textRenderer, 
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
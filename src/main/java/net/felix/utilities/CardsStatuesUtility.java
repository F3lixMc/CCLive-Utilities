package net.felix.utilities;

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
	 * Rendert den Karten-Hintergrund mit der karten_background.png Textur
	 */
	private static void renderCardBackground(DrawContext context, int x, int y) {
		try {
			// Verwende Matrix-Transformationen für Skalierung
			Matrix3x2fStack matrices = context.getMatrices();
			matrices.pushMatrix();
			
			// Skaliere das Overlay basierend auf der Config
			float scale = CCLiveUtilitiesConfig.HANDLER.instance().cardOverlayScale;
			if (scale <= 0) scale = 1.0f; // Sicherheitscheck
			
			// Übersetze zur Position und skaliere von dort aus
			matrices.translate(x, y);
			matrices.scale(scale, scale);
			
			// Verwende die drawTexture Methode mit der passenden GUI_TEXTURED Pipeline
			// Diese erwartet keine UV2-Attribute und ist für 2D-Overlays geeignet
			context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				CARD_BACKGROUND_TEXTURE,
				-11, -11, // Relative Position (0-basiert, da wir bereits übersetzt haben)
				0.0f, 0.0f, // UV-Koordinaten (Start der Textur)
				BACKGROUND_WIDTH, BACKGROUND_HEIGHT, // Größe
				BACKGROUND_WIDTH, BACKGROUND_HEIGHT // Textur-Größe
			);
			
			matrices.popMatrix();
		} catch (Exception e) {
			// Fallback: Verwende den ursprünglichen schwarzen Hintergrund
			context.fill(x - 11, y - 11, x + BACKGROUND_WIDTH - 11, y + BACKGROUND_HEIGHT - 11, 0x80000000);
		}
	}
	
	/**
	 * Rendert den Statuen-Hintergrund mit der statuen_background.png Textur
	 */
	private static void renderStatueBackground(DrawContext context, int x, int y) {
		try {
			// Verwende Matrix-Transformationen für Skalierung
			Matrix3x2fStack matrices = context.getMatrices();
			matrices.pushMatrix();
			
			// Skaliere das Overlay basierend auf der Config
			float scale = CCLiveUtilitiesConfig.HANDLER.instance().statueOverlayScale;
			if (scale <= 0) scale = 1.0f; // Sicherheitscheck
			
			// Übersetze zur Position und skaliere von dort aus
			matrices.translate(x, y);
			matrices.scale(scale, scale);
			
			// Verwende die drawTexture Methode mit der passenden GUI_TEXTURED Pipeline
			// Diese erwartet keine UV2-Attribute und ist für 2D-Overlays geeignet
			context.drawTexture(
				RenderPipelines.GUI_TEXTURED,
				STATUE_BACKGROUND_TEXTURE,
				-11, -11, // Relative Position (0-basiert, da wir bereits übersetzt haben)
				0.0f, 0.0f, // UV-Koordinaten (Start der Textur)
				BACKGROUND_WIDTH, BACKGROUND_HEIGHT, // Größe
				BACKGROUND_WIDTH, BACKGROUND_HEIGHT // Textur-Größe
			);
			
			matrices.popMatrix();
		} catch (Exception e) {
			// Fallback: Verwende den ursprünglichen schwarzen Hintergrund
			context.fill(x - 11, y - 11, x + BACKGROUND_WIDTH - 11, y + BACKGROUND_HEIGHT - 11, 0x80000000);
		}
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
				String hoverString = hoverEvent.toString();
				hoverContent = parseHoverEventData(hoverString);
			} catch (Exception e) {
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
		String[] lines = hoverContent.split("\n");
		
		for (String line : lines) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty()) {
				hoverLines.add(trimmed);
			}
		}
		
		cardData.setHoverLines(hoverLines);
		
		// Extrahiere Daten aus den geparsten Linien
		if (!hoverLines.isEmpty()) {
			cardData.setName(hoverLines.get(0));
		}
		
		for (String line : hoverLines) {
			if (line.contains("Stufe:") && !line.contains("Nächste")) {
				String level = line.replaceAll("[^0-9]", "").trim();
				if (!level.isEmpty()) {
					cardData.setLevel(level);
				}
			} else if (line.contains("Nächste Stufe:")) {
				String nextLevel = line.replaceAll("[^0-9]", "").trim();
				if (!nextLevel.isEmpty()) {
					cardData.setNextLevel(nextLevel);
				}
			} else if (line.startsWith("+") && !line.contains("Stufe")) {
				cardData.setEffect(line);
			}
		}
		
		currentCard = cardData;
	}
	
	private static void handleStatueMessage(Text message, String hoverContent) {
		StatueData statueData = new StatueData();
		statueData.setColor(getColorFromStyle(message.getStyle()));
		
		// Parse die geparsten Hover-Daten
		List<String> hoverLines = new ArrayList<>();
		String[] lines = hoverContent.split("\n");
		
		for (String line : lines) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty()) {
				hoverLines.add(trimmed);
			}
		}
		
		statueData.setHoverLines(hoverLines);
		
		// Extrahiere Daten aus den geparsten Linien
		if (!hoverLines.isEmpty()) {
			statueData.setName(hoverLines.get(0));
		}
		
		for (String line : hoverLines) {
			if (line.contains("Stufe:") && !line.contains("Nächste")) {
				String level = line.replaceAll("[^0-9]", "").trim();
				if (!level.isEmpty()) {
					statueData.setLevel(level);
				}
			} else if (line.contains("Nächste Stufe:")) {
				String nextLevel = line.replaceAll("[^0-9]", "").trim();
				if (!nextLevel.isEmpty()) {
					statueData.setNextLevel(nextLevel);
				}
			} else if (line.startsWith("+") && !line.contains("Stufe")) {
				statueData.setEffect(line);
			}
		}
		
		currentStatue = statueData;
	}
	

	
	private static String getColorFromStyle(Style style) {
		if (style.getColor() != null) {
			return style.getColor().getName();
		}
		return Formatting.WHITE.getName();
	}
	
	private static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		
		// Prüfe ob die Cards/Statues Utility aktiviert ist
		if (!config.cardsStatuesEnabled) {
			return;
		}
		
		// Prüfe ob wir in einer Welt sind (ohne weitere Bedingungen)
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || client.player == null) {
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
		
		// Zeige alle Hover-Linien an, außer "Statistik" und leere Zeilen
		int lineCount = 0;
		for (int i = 0; i < card.getHoverLines().size(); i++) {
			String line = card.getHoverLines().get(i);
			
			// Überspringe "Statistik", leere Zeilen und Zeilen mit unsichtbaren chinesischen Zeichen
			if (line.contains("Statistik") || 
				line.trim().isEmpty() || 
				line.matches(".*[㓽㓾㓿㔀㔁㔂㔃㔄㔅㔆㔇㔈㔉㔊㔋㔌㔍㔎㔏㔐㔑㔒㔓㔔㔕㔖㔗㔘㔙㔚㔛㔜㔝㔞㔟㔠㔡㔢㔣].*")) {
				continue;
			}
			
			// Überspringe auch die Zeile vor "Statistik" (leere Zeile)
			if (i < card.getHoverLines().size() - 1 && card.getHoverLines().get(i + 1).contains("Statistik")) {
				continue;
			}
			
			// Berechne die Y-Position basierend auf der Anzahl der Zeilen
			// Verwende relative Positionen (0-basiert) da wir bereits übersetzt haben
			// Der Text soll die gleichen Abstände zu den Rändern haben wie bei der originalen Größe
			int textY = -1 + (lineCount * 12);
			
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
		
		// Zeige alle Hover-Linien an, außer "Statistik" und leere Zeilen
		int lineCount = 0;
		for (int i = 0; i < statue.getHoverLines().size(); i++) {
			String line = statue.getHoverLines().get(i);
			
			// Überspringe "Statistik", leere Zeilen und Zeilen mit unsichtbaren chinesischen Zeichen
			if (line.contains("Statistik") || 
				line.trim().isEmpty() || 
				line.matches(".*[㓽㓾㓿㔀㔁㔂㔃㔄㔅㔆㔇㔈㔉㔊㔋㔌㔍㔎㔏㔐㔑㔒㔓㔔㔕㔖㔗㔘㔙㔚㔛㔜㔝㔞㔟㔠㔡㔢㔣].*")) {
				continue;
			}
			
			// Überspringe auch die Zeile vor "Statistik" (leere Zeile)
			if (i < statue.getHoverLines().size() - 1 && statue.getHoverLines().get(i + 1).contains("Statistik")) {
				continue;
			}
			
			// Berechne die Y-Position basierend auf der Anzahl der Zeilen
			// Verwende relative Positionen (0-basiert) da wir bereits übersetzt haben
			// Der Text soll die gleichen Abstände zu den Rändern haben wie bei der originalen Größe
			int textY = -1 + (lineCount * 12);
			
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
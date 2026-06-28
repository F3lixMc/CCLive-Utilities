package net.felix.utilities.Town;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.BPViewerUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Screen für die Auswahl eines Kits und seiner Stufe
 * Zeigt 5 Kits (Reihen) mit jeweils 7 Stufen (Spalten)
 */
public class KitSelectionScreen extends Screen {
	
	private final int buttonIndex; // Welcher Button wurde geklickt (0, 1, oder 2)
	
	// Speichere den vorherigen Screen, um zurückzukehren
	private Screen previousScreen;
	
	// Grid-Positionen
	private static final int SLOT_SIZE = 18;
	private static final int GRID_SPACING_X = SLOT_SIZE + 2;
	private static final int GRID_SPACING_Y = SLOT_SIZE + 2;
	
	// Berechnete Grid-Positionen (werden in init() gesetzt)
	private int gridStartX = 0;
	private int gridStartY = 0;
	private int kitNameStartX = 0;
	private int maxKitNameWidth = 0;
	
	// Hintergrund-Positionen (werden in init() gesetzt)
	private int backgroundX = 0;
	private int backgroundY = 0;
	private int backgroundWidth = 0;
	private int backgroundHeight = 0;
	
	// Textur-Identifier für den Hintergrund
	private static final Identifier KITS_BACKGROUND_TEXTURE = Identifier.of("cclive-utilities", "textures/gui/kits_background.png");
	
	// Anzahl Kits und Stufen
	private static final int KIT_COUNT = 5; // 5 Kits
	private static final int LEVEL_COUNT = 7; // 7 Stufen pro Kit
	private static final int CUSTOM_COLUMNS = 7;
	private static final int TAB_HEIGHT = 14;
	private static final int TAB_GAP = 2;
	private static final int TAB_PADDING_X = 4;
	private static final int TAB_MIN_WIDTH = 26;
	private static final String[] TAB_LABELS = {"Alt", "Neu", "Eigene"};
	
	private static final int TAB_ALT = 0;
	private static final int TAB_NEU = 1;
	private static final int TAB_EIGENE = 2;
	private static final int TAB_COLOR_ACTIVE = 0xFF4C635E;
	private static final int TAB_COLOR_ACTIVE_TOP = 0xFF3D4F4B;
	private static final int TAB_COLOR_INACTIVE = 0xFF4D6E6D;
	private static final int TAB_COLOR_INACTIVE_TOP = 0xFF466363;
	private static final int TOOLTIP_ACTION_GREEN = 0xFF16A80C;
	
	private int activeTab = TAB_ALT;
	
	// Aktuell ausgewähltes Kit und Stufe
	private KitFilterUtility.KitType selectedKitType = null;
	private int selectedLevel = -1;
	private boolean selectedNeuKit = false;
	private String selectedCustomKitId = null;
	
	// Hover-Informationen für Tooltip
	private KitFilterUtility.KitType hoveredKitType = null;
	private int hoveredLevel = -1;
	private boolean hoveredNeuKit = false;
	private CustomKit hoveredCustomKit = null;
	private boolean hoveredPlusSlot = false;
	
	// Button zum Schließen
	private ButtonWidget doneButton;
	private ButtonWidget cancelButton;
	private final KitTooltipHelper tooltipHelper = new KitTooltipHelper();
	
	public KitSelectionScreen(int buttonIndex) {
		super(Text.literal("Kit Auswahl"));
		this.buttonIndex = buttonIndex;
		
		// Speichere den aktuellen Screen (Schmied-Inventar) bevor wir den Kit-Auswahl-Screen öffnen
		this.previousScreen = MinecraftClient.getInstance().currentScreen;
		
		// Lade die aktuelle Auswahl für diesen Button
		KitFilterUtility.KitSelection currentSelection = KitFilterUtility.getKitSelection(buttonIndex);
		if (currentSelection != null) {
			if (currentSelection.isCustom()) {
				this.activeTab = TAB_EIGENE;
				this.selectedCustomKitId = currentSelection.customKitId;
			} else if (currentSelection.neuKit) {
				this.activeTab = TAB_NEU;
				this.selectedKitType = currentSelection.kitType;
				this.selectedLevel = currentSelection.level;
				this.selectedNeuKit = true;
			} else {
				this.selectedKitType = currentSelection.kitType;
				this.selectedLevel = currentSelection.level;
			}
		}
	}
	
	public void switchToCustomTab() {
		this.activeTab = TAB_EIGENE;
	}
	
	@Override
	protected void init() {
		super.init();
		
		// Berechne Grid-Positionen (zentriert)
		// Berechne die tatsächliche Breite der längsten Kit-Namen
		this.maxKitNameWidth = 0;
		KitFilterUtility.KitType[] kitTypes = KitFilterUtility.KitType.values();
		for (KitFilterUtility.KitType kitType : kitTypes) {
			int nameWidth = this.textRenderer.getWidth(kitType.getDisplayName());
			this.maxKitNameWidth = Math.max(this.maxKitNameWidth, nameWidth);
		}
		this.maxKitNameWidth += 10; // Padding rechts vom Namen
		
		// Berechne die tatsächliche Breite der Grid-Kästchen
		// Das erste Kästchen beginnt bei gridStartX, das letzte endet bei gridStartX + (LEVEL_COUNT - 1) * GRID_SPACING_X + SLOT_SIZE
		int actualGridWidth = (LEVEL_COUNT - 1) * GRID_SPACING_X + SLOT_SIZE;
		int actualGridHeight = (KIT_COUNT - 1) * GRID_SPACING_Y + SLOT_SIZE;
		
		// Gleichmäßiges Padding in alle Richtungen
		int padding = 15;
		int extraSize = 2; // Zusätzliche 2 Pixel in alle 4 Richtungen
		
		// Berechne die Hintergrund-Dimensionen
		// Der Hintergrund soll 15 Pixel Abstand zum Grid haben (links und rechts) + 2 Pixel extra in alle Richtungen
		this.backgroundHeight = actualGridHeight + (padding * 2) + (extraSize * 2);
		// Hintergrund-Breite: Tatsächliche Grid-Breite + 15 Pixel links + 15 Pixel rechts + 2 Pixel extra links + 2 Pixel extra rechts
		this.backgroundWidth = actualGridWidth + (padding * 2) + (extraSize * 2);
		
		// ZENTRIERE DEN HINTERGRUND ZUERST - das stellt sicher, dass beide Seiten gleich sind
		this.backgroundX = (this.width - this.backgroundWidth) / 2;
		this.backgroundY = (this.height - this.backgroundHeight) / 2;
		
		// Positioniere das Grid innerhalb des zentrierten Hintergrunds mit exakt gleichen Abständen
		// 15 Pixel Padding links und rechts + 2 Pixel extra
		this.gridStartX = this.backgroundX + padding + extraSize;
		this.gridStartY = this.backgroundY + padding + extraSize;
		
		// Positioniere Kit-Namen links vom Grid (außerhalb des Hintergrunds)
		this.kitNameStartX = this.gridStartX - this.maxKitNameWidth;
		
		int buttonY = this.height - 30;
		int buttonHeight = 20;
		int buttonGap = 10;
		int doneButtonWidth = this.textRenderer.getWidth("Auswählen") + 16;
		int cancelButtonWidth = this.textRenderer.getWidth("Abbrechen") + 16;
		int totalButtonsWidth = doneButtonWidth + buttonGap + cancelButtonWidth;
		int buttonsStartX = (this.width - totalButtonsWidth) / 2;

		this.doneButton = ButtonWidget.builder(
			Text.literal("Auswählen"),
			button -> onDone()
		)
		.dimensions(buttonsStartX, buttonY, doneButtonWidth, buttonHeight)
		.build();
		this.addDrawableChild(this.doneButton);
		
		this.cancelButton = ButtonWidget.builder(
			Text.literal("Abbrechen"),
			button -> onCancel()
		)
		.dimensions(buttonsStartX + doneButtonWidth + buttonGap, buttonY, cancelButtonWidth, buttonHeight)
		.build();
		this.addDrawableChild(this.cancelButton);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Hintergrund rendern (ohne Blur, da bereits vom darunterliegenden Screen angewendet)
		// Rendere einen halbtransparenten dunklen Hintergrund
		context.fill(0, 0, this.width, this.height, 0xC0101010);
		
		// Rendere Kits-Background-Textur als Hintergrund für das Overlay
		// Prüfe ob init() bereits ausgeführt wurde
		if (maxKitNameWidth > 0 && gridStartX > 0 && backgroundWidth > 0) {
			// Verwende die in init() berechneten Werte für konsistente Positionierung
			// Rendere die Hintergrund-Textur (mittig mit gleichmäßigen Abständen)
			try {
				context.drawTexture(
					RenderPipelines.GUI_TEXTURED,
					KITS_BACKGROUND_TEXTURE,
					backgroundX, backgroundY,
					0.0f, 0.0f,
					backgroundWidth, backgroundHeight,
					backgroundWidth, backgroundHeight
				);
			} catch (Exception e) {
				// Fallback: Verwende einen halbtransparenten schwarzen Hintergrund wenn Textur-Loading fehlschlägt
				context.fill(backgroundX, backgroundY, backgroundX + backgroundWidth, backgroundY + backgroundHeight, 0x80000000);
			}
		}
		
		// Titel rendern
		context.drawCenteredTextWithShadow(
			this.textRenderer,
			this.title,
			this.width / 2,
			20,
			0xFFFFFF
		);
		
		// Grid rendern (nur wenn init() bereits ausgeführt wurde)
		if (maxKitNameWidth > 0 && gridStartX > 0) {
			if (activeTab == TAB_ALT) {
				renderKitGrid(context, mouseX, mouseY);
			} else if (activeTab == TAB_NEU) {
				renderNeuKitGrid(context, mouseX, mouseY);
			} else {
				renderCustomKitGrid(context, mouseX, mouseY);
			}
		}
		
		// Buttons rendern (wird automatisch gemacht)
		super.render(context, mouseX, mouseY, delta);

		renderForegroundUi(context, mouseX, mouseY);
		
		// Tooltip nach Tabs rendern, damit er nicht von den Tab-Buttons verdeckt wird
		if (hoveredKitType != null && hoveredLevel > 0) {
			renderTooltip(context, mouseX, mouseY, hoveredNeuKit);
		} else if (hoveredCustomKit != null) {
			renderCustomKitTooltip(context, mouseX, mouseY);
		} else if (hoveredPlusSlot) {
			renderPlusSlotTooltip(context, mouseX, mouseY);
		}
	}

	/**
	 * Tabs ganz am Ende zeichnen.
	 * Nach drawTexture kann ein Scissor aktiv sein, das Tab-Text oberhalb des Panels abschneidet.
	 */
	private void renderForegroundUi(DrawContext context, int mouseX, int mouseY) {
		if (backgroundWidth <= 0) {
			return;
		}
		context.enableScissor(0, 0, this.width, this.height);
		renderTabs(context, mouseX, mouseY);
		context.disableScissor();
	}
	
	/**
	 * Prüft, ob der BPViewer-Status angezeigt werden soll
	 * (nur wenn BPViewer aktiviert ist und wir nicht im Shop sind)
	 * Hinweis: Die Prüfung auf das spezielle Inventar (mit "㬉") wurde entfernt,
	 * damit der Status auch im Kit-Selection-Tooltip angezeigt wird
	 */
	private boolean shouldShowBPViewerStatus() {
		// Prüfe, ob BPViewer in der Config aktiviert ist
		if (!CCLiveUtilitiesConfig.HANDLER.instance().blueprintViewerEnabled) {
			return false;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.currentScreen == null) {
			return false;
		}
		
		// Prüfe, ob wir im Shop sind - dann keine Status-Anzeige
		if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen) {
			net.minecraft.client.gui.screen.ingame.HandledScreen<?> screen = (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
			String title = screen.getTitle().getString();
			String cleanTitle = title.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "");
			cleanTitle = cleanTitle.replaceAll("§[0-9a-fk-or]", "");
			
			boolean isInShop = cleanTitle.contains("Bauplan [Shop]") || 
			                  cleanTitle.contains("Bauplan Shop") ||
			                  (cleanTitle.contains("Bauplan") && cleanTitle.contains("Shop"));
			
			if (isInShop) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Prüft, ob ein Blueprint gefunden wurde (verwendet BPViewerUtility)
	 * Normalisiert den Namen (entfernt Farbcodes, ignoriert Groß-/Kleinschreibung) für den Vergleich
	 */
	private boolean isBlueprintFound(String blueprintName) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			return false;
		}
		
		try {
			BPViewerUtility bpViewer = BPViewerUtility.getInstance();
			
			// Normalisiere den Namen: Entferne Farbcodes und trimme
			String normalizedName = blueprintName.replaceAll("§[0-9a-fk-or]", "").trim();
			
			// Prüfe zuerst mit dem normalisierten Namen
			if (bpViewer.isBlueprintFoundAnywhere(normalizedName)) {
				return true;
			}
			
			// Falls nicht gefunden, prüfe auch mit dem originalen Namen (für Rückwärtskompatibilität)
			if (!normalizedName.equals(blueprintName)) {
				return bpViewer.isBlueprintFoundAnywhere(blueprintName);
			}
			
			return false;
		} catch (Exception e) {
			// Falls BPViewer nicht verfügbar ist, gebe false zurück
			return false;
		}
	}
	
	/**
	 * Rendert einen Tooltip mit Kit-Namen, Stufe und Item-Namen in tabellarischer Form
	 */
	private void renderTooltip(DrawContext context, int mouseX, int mouseY, boolean neuKit) {
		if (hoveredKitType == null || hoveredLevel <= 0) {
			return;
		}
		String tooltipHeader = hoveredKitType.getDisplayName() + " " + hoveredLevel;
		java.util.Set<KitFilterUtility.ItemInfo> itemInfosSet =
				KitFilterUtility.getKitItemInfos(hoveredKitType, hoveredLevel, neuKit);
		renderCachedItemInfosTooltip(
				context,
				mouseX,
				mouseY,
				"kit:" + hoveredKitType.name() + ":" + hoveredLevel + ":" + neuKit + tooltipBpCacheSuffix(),
				tooltipHeader,
				itemInfosSet,
				false,
				false
		);
	}

	private void renderCustomKitTooltip(DrawContext context, int mouseX, int mouseY) {
		if (hoveredCustomKit == null) {
			return;
		}
		String kitName = hoveredCustomKit.name;
		if (kitName == null || kitName.isEmpty()) {
			kitName = "Neues Kit";
		}
		renderCachedItemInfosTooltip(
				context,
				mouseX,
				mouseY,
				"custom:" + KitFilterUtility.getCustomKitItemCacheKey(hoveredCustomKit) + tooltipBpCacheSuffix(),
				kitName,
				KitFilterUtility.getCustomKitItemInfos(hoveredCustomKit),
				true,
				true
		);
	}

	private String tooltipBpCacheSuffix() {
		return ":bp" + (shouldShowBPViewerStatus() ? 1 : 0);
	}

	private void renderCachedItemInfosTooltip(
			DrawContext context,
			int mouseX,
			int mouseY,
			String cacheKey,
			String tooltipHeader,
			java.util.Collection<KitFilterUtility.ItemInfo> itemInfos,
			boolean showSelectActionHint,
			boolean showEditActionHint
	) {
		boolean showBpStatus = shouldShowBPViewerStatus();
		KitTooltipHelper.CachedTooltip cached = tooltipHelper.getOrBuild(
				cacheKey,
				this.textRenderer,
				tooltipHeader,
				itemInfos,
				showSelectActionHint,
				showEditActionHint,
				showBpStatus,
				this::isBlueprintFound
		);
		KitTooltipHelper.render(context, this.textRenderer, cached, mouseX, mouseY, this.width, this.height);
	}

	private void renderPlusSlotTooltip(DrawContext context, int mouseX, int mouseY) {
		renderSingleActionHintTooltip(context, mouseX, mouseY, "Linksklick", "Kit Erstellen");
	}

	private void renderSingleActionHintTooltip(DrawContext context, int mouseX, int mouseY,
			String bracketContent, String actionText) {
		int padding = 4;
		int offset = 10;
		int textHeight = this.textRenderer.fontHeight;
		int totalWidth = this.textRenderer.getWidth("[" + bracketContent + "]: " + actionText);
		int totalHeight = textHeight + padding * 2;
		int tooltipWidth = totalWidth + padding * 2;

		int tooltipX = mouseX + offset;
		if (tooltipX + tooltipWidth > this.width) {
			tooltipX = mouseX - tooltipWidth - offset;
			if (tooltipX < padding) {
				tooltipX = padding;
			}
		}
		if (tooltipX + tooltipWidth > this.width) {
			tooltipX = this.width - tooltipWidth - padding;
		}

		int tooltipY = mouseY - totalHeight - offset;
		if (tooltipY < padding) {
			tooltipY = mouseY + offset;
			if (tooltipY + totalHeight > this.height - padding) {
				tooltipY = this.height - totalHeight - padding;
			}
		}
		if (tooltipY + totalHeight > this.height - padding) {
			tooltipY = this.height - totalHeight - padding;
		}
		if (tooltipY < padding) {
			tooltipY = padding;
		}

		int bgX1 = tooltipX - padding;
		int bgY1 = tooltipY - padding;
		int bgX2 = tooltipX + totalWidth + padding;
		int bgY2 = tooltipY + totalHeight - padding;

		context.fill(bgX1, bgY1, bgX2, bgY2, 0xF0000000);
		context.fill(bgX1, bgY1, bgX2, bgY1 + 1, 0xFFFFFFFF);
		context.fill(bgX1, bgY2 - 1, bgX2, bgY2, 0xFFFFFFFF);
		context.fill(bgX1, bgY1, bgX1 + 1, bgY2, 0xFFFFFFFF);
		context.fill(bgX2 - 1, bgY1, bgX2, bgY2, 0xFFFFFFFF);

		drawActionHintLine(context, tooltipX, tooltipY, bracketContent, actionText);
	}

	private void drawActionHintLine(DrawContext context, int x, int y, String bracketContent, String actionText) {
		int cursor = x;
		context.drawText(this.textRenderer, "[", cursor, y, TOOLTIP_ACTION_GREEN, true);
		cursor += this.textRenderer.getWidth("[");
		context.drawText(this.textRenderer, bracketContent, cursor, y, TOOLTIP_ACTION_GREEN, true);
		cursor += this.textRenderer.getWidth(bracketContent);
		context.drawText(this.textRenderer, "]", cursor, y, TOOLTIP_ACTION_GREEN, true);
		cursor += this.textRenderer.getWidth("]");
		context.drawText(this.textRenderer, ":", cursor, y, TOOLTIP_ACTION_GREEN, true);
		cursor += this.textRenderer.getWidth(":");
		context.drawText(this.textRenderer, " " + actionText, cursor, y, 0xFFFFFFFF, true);
	}
	
	/**
	 * Rendert das Kit-Grid (5 Kits x 7 Stufen)
	 */
	private void renderKitGrid(DrawContext context, int mouseX, int mouseY) {
		KitFilterUtility.KitType[] kitTypes = KitFilterUtility.KitType.values();
		
		// Reset hover-Informationen
		hoveredKitType = null;
		hoveredLevel = -1;
		hoveredNeuKit = false;
		hoveredCustomKit = null;
		hoveredPlusSlot = false;
		
		// Rendere jede Reihe (Kit)
		for (int kitIndex = 0; kitIndex < KIT_COUNT && kitIndex < kitTypes.length; kitIndex++) {
			KitFilterUtility.KitType kitType = kitTypes[kitIndex];
			
			// Hole das Icon-Item für dieses Kit
			ItemStack kitIcon = KitFilterUtility.getKitTypeIcon(kitType);
			
			// Rendere Kit-Name links
			int kitNameY = gridStartY + (kitIndex * GRID_SPACING_Y) + (SLOT_SIZE / 2) - 4;
			// Berechne X-Position für Kit-Namen (rechtsbündig, damit sie am Grid ausgerichtet sind)
			int kitNameWidth = this.textRenderer.getWidth(kitType.getDisplayName());
			int kitNameX = kitNameStartX + (this.maxKitNameWidth - kitNameWidth);
			context.drawText(
				this.textRenderer,
				kitType.getDisplayName(),
				kitNameX,
				kitNameY,
				0xFFFFFF,
				false
			);
			
			// Rendere jede Spalte (Stufe)
			for (int level = 1; level <= LEVEL_COUNT; level++) {
				int slotX = gridStartX + ((level - 1) * GRID_SPACING_X);
				int slotY = gridStartY + (kitIndex * GRID_SPACING_Y);
				
				// Prüfe ob dieser Slot ausgewählt ist
				boolean isSelected = (kitType == selectedKitType && level == selectedLevel);
				
				// Prüfe ob Maus über diesem Slot ist
				boolean isHovered = (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
									 mouseY >= slotY && mouseY < slotY + SLOT_SIZE);
				
				// Speichere Hover-Informationen für Tooltip
				if (isHovered) {
					hoveredKitType = kitType;
					hoveredLevel = level;
				}
				
				// Slot-Hintergrund rendern
				int backgroundColor = isSelected ? 0xFF00FF00 : (isHovered ? 0xFFFFFFFF : 0xFF808080);
				context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, backgroundColor);
				
				// Slot-Rahmen rendern
				int borderColor = isSelected ? 0xFF00AA00 : 0xFF000000;
				context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + 1, borderColor); // Oben
				context.fill(slotX, slotY + SLOT_SIZE - 1, slotX + SLOT_SIZE, slotY + SLOT_SIZE, borderColor); // Unten
				context.fill(slotX, slotY, slotX + 1, slotY + SLOT_SIZE, borderColor); // Links
				context.fill(slotX + SLOT_SIZE - 1, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, borderColor); // Rechts
				
				// Rendere Kit-Icon im Slot
				if (!kitIcon.isEmpty()) {
					context.drawItem(kitIcon, slotX + 1, slotY + 1, 0);
				}

				// Stufen-Nummer als Overlay rendern (rechts unten im Slot)
				String levelText = String.valueOf(level);
				int textX = slotX + SLOT_SIZE - this.textRenderer.getWidth(levelText) - 2;
				int textY = slotY + SLOT_SIZE - 8;
				int textColor = isSelected ? 0xFF000000 : 0xFFFFFF;
				context.drawText(this.textRenderer, levelText, textX, textY, textColor, false);
			}
		}
	}

	private void renderNeuKitGrid(DrawContext context, int mouseX, int mouseY) {
		KitFilterUtility.KitType[] kitTypes = KitFilterUtility.NEU_KIT_TYPES;
		
		hoveredKitType = null;
		hoveredLevel = -1;
		hoveredNeuKit = false;
		hoveredCustomKit = null;
		hoveredPlusSlot = false;
		
		for (int kitIndex = 0; kitIndex < kitTypes.length; kitIndex++) {
			KitFilterUtility.KitType kitType = kitTypes[kitIndex];
			ItemStack kitIcon = KitFilterUtility.getKitTypeIcon(kitType);
			
			int kitNameY = gridStartY + (kitIndex * GRID_SPACING_Y) + (SLOT_SIZE / 2) - 4;
			int kitNameWidth = this.textRenderer.getWidth(kitType.getDisplayName());
			int kitNameX = kitNameStartX + (this.maxKitNameWidth - kitNameWidth);
			context.drawText(this.textRenderer, kitType.getDisplayName(), kitNameX, kitNameY, 0xFFFFFF, false);
			
			for (int level = 1; level <= KitFilterUtility.getNeuLevelCount(kitType); level++) {
				int slotX = gridStartX + ((level - 1) * GRID_SPACING_X);
				int slotY = gridStartY + (kitIndex * GRID_SPACING_Y);
				
				boolean isSelected = selectedNeuKit && kitType == selectedKitType && level == selectedLevel;
				boolean isHovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE
						&& mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
				
				if (isHovered) {
					hoveredKitType = kitType;
					hoveredLevel = level;
					hoveredNeuKit = true;
				}
				
				int backgroundColor = isSelected ? 0xFF00FF00 : (isHovered ? 0xFFFFFFFF : 0xFF808080);
				context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, backgroundColor);
				
				int borderColor = isSelected ? 0xFF00AA00 : 0xFF000000;
				context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + 1, borderColor);
				context.fill(slotX, slotY + SLOT_SIZE - 1, slotX + SLOT_SIZE, slotY + SLOT_SIZE, borderColor);
				context.fill(slotX, slotY, slotX + 1, slotY + SLOT_SIZE, borderColor);
				context.fill(slotX + SLOT_SIZE - 1, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, borderColor);
				
				if (!kitIcon.isEmpty()) {
					context.drawItem(kitIcon, slotX + 1, slotY + 1, 0);
				}

				String levelText = String.valueOf(level);
				int textX = slotX + SLOT_SIZE - this.textRenderer.getWidth(levelText) - 2;
				int textY = slotY + SLOT_SIZE - 8;
				int textColor = isSelected ? 0xFF000000 : 0xFFFFFF;
				context.drawText(this.textRenderer, levelText, textX, textY, textColor, false);
			}
		}
	}

	private int getTabY() {
		return backgroundY - TAB_HEIGHT + 4;
	}

	private void computeTabPositions(int[] outX, int[] outWidth) {
		int total = TAB_GAP * 2;
		for (int i = 0; i < TAB_LABELS.length; i++) {
			outWidth[i] = Math.max(TAB_MIN_WIDTH, this.textRenderer.getWidth(TAB_LABELS[i]) + TAB_PADDING_X * 2);
			total += outWidth[i];
		}
		if (total > backgroundWidth) {
			int equalWidth = Math.max(TAB_MIN_WIDTH, (backgroundWidth - TAB_GAP * 2) / TAB_LABELS.length);
			for (int i = 0; i < TAB_LABELS.length; i++) {
				outWidth[i] = equalWidth;
			}
			total = equalWidth * TAB_LABELS.length + TAB_GAP * 2;
		}
		int startX = backgroundX + (backgroundWidth - total) / 2;
		outX[0] = startX;
		for (int i = 1; i < TAB_LABELS.length; i++) {
			outX[i] = outX[i - 1] + outWidth[i - 1] + TAB_GAP;
		}
	}

	private int getTabAt(double mouseX, double mouseY) {
		if (backgroundWidth <= 0) {
			return -1;
		}
		int tabY = getTabY();
		if (mouseY < tabY || mouseY >= tabY + TAB_HEIGHT) {
			return -1;
		}
		int[] xs = new int[TAB_LABELS.length];
		int[] widths = new int[TAB_LABELS.length];
		computeTabPositions(xs, widths);
		for (int i = 0; i < TAB_LABELS.length; i++) {
			if (mouseX >= xs[i] && mouseX < xs[i] + widths[i]) {
				return i;
			}
		}
		return -1;
	}
	
	private void renderTabs(DrawContext context, int mouseX, int mouseY) {
		if (backgroundWidth <= 0) {
			return;
		}
		int tabY = getTabY();
		int[] xs = new int[TAB_LABELS.length];
		int[] widths = new int[TAB_LABELS.length];
		computeTabPositions(xs, widths);
		int[] tabIds = {TAB_ALT, TAB_NEU, TAB_EIGENE};
		for (int i = 0; i < TAB_LABELS.length; i++) {
			renderTab(context, xs[i], tabY, widths[i], TAB_LABELS[i], activeTab == tabIds[i], mouseX, mouseY);
		}
	}

	private void drawCenteredLabel(DrawContext context, int x, int y, int width, int height, String label, boolean active) {
		int textY = y + (height - this.textRenderer.fontHeight) / 2;
		int textColor = active ? 0xFFFFFFFF : 0xFFCCCCCC;
		int textWidth = this.textRenderer.getWidth(label);
		context.drawText(this.textRenderer, label, x + (width - textWidth) / 2, textY, textColor, true);
	}

	private void drawCenteredPlusInSlot(DrawContext context, int slotX, int slotY, int color) {
		int centerX = slotX + SLOT_SIZE / 2;
		int centerY = slotY + SLOT_SIZE / 2;
		int armLength = 4;
		context.fill(centerX - armLength, centerY - 1, centerX + armLength, centerY + 1, color);
		context.fill(centerX - 1, centerY - armLength, centerX + 1, centerY + armLength, color);
	}
	
	private void renderTab(DrawContext context, int x, int y, int width, String label, boolean active, int mouseX, int mouseY) {
		boolean hovered = mouseX >= x && mouseX < x + width
				&& mouseY >= y && mouseY < y + TAB_HEIGHT;
		int bg = active ? TAB_COLOR_ACTIVE : TAB_COLOR_INACTIVE;
		int topBorder = active ? TAB_COLOR_ACTIVE_TOP : TAB_COLOR_INACTIVE_TOP;
		context.fill(x, y, x + width, y + TAB_HEIGHT, bg);
		context.fill(x, y, x + width, y + 1, topBorder);
		context.fill(x, y + TAB_HEIGHT - 1, x + width, y + TAB_HEIGHT, 0xFF1D2F3B);
		drawCenteredLabel(context, x, y, width, TAB_HEIGHT, label, active);
		if (hovered) {
			context.drawStrokedRectangle(x, y, width, TAB_HEIGHT, 0xFFFFFFFF);
		}
	}
	
	private void renderCustomKitGrid(DrawContext context, int mouseX, int mouseY) {
		hoveredCustomKit = null;
		hoveredPlusSlot = false;
		hoveredKitType = null;
		hoveredLevel = -1;

		java.util.List<CustomKit> kits = CustomKitManager.getKitsForButton(buttonIndex);
		int slotIndex = 0;
		
		for (CustomKit kit : kits) {
			int col = slotIndex % CUSTOM_COLUMNS;
			int row = slotIndex / CUSTOM_COLUMNS;
			int slotX = gridStartX + col * GRID_SPACING_X;
			int slotY = gridStartY + row * GRID_SPACING_Y;
			
			boolean isSelected = kit.id != null && kit.id.equals(selectedCustomKitId);
			boolean isHovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE
					&& mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
			if (isHovered) {
				hoveredCustomKit = kit;
			}
			
			int backgroundColor = isSelected ? 0xFF00FF00 : (isHovered ? 0xFFFFFFFF : 0xFF808080);
			context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, backgroundColor);
			int borderColor = isSelected ? 0xFF00AA00 : 0xFF000000;
			context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + 1, borderColor);
			context.fill(slotX, slotY + SLOT_SIZE - 1, slotX + SLOT_SIZE, slotY + SLOT_SIZE, borderColor);
			context.fill(slotX, slotY, slotX + 1, slotY + SLOT_SIZE, borderColor);
			context.fill(slotX + SLOT_SIZE - 1, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, borderColor);
			
			ItemStack icon = kit.createIconStack();
			if (!icon.isEmpty()) {
				context.drawItem(icon, slotX + 1, slotY + 1, 0);
			}
			slotIndex++;
		}
		
		int plusCol = slotIndex % CUSTOM_COLUMNS;
		int plusRow = slotIndex / CUSTOM_COLUMNS;
		int plusX = gridStartX + plusCol * GRID_SPACING_X;
		int plusY = gridStartY + plusRow * GRID_SPACING_Y;
		boolean plusHovered = mouseX >= plusX && mouseX < plusX + SLOT_SIZE
				&& mouseY >= plusY && mouseY < plusY + SLOT_SIZE;
		if (plusHovered) {
			hoveredPlusSlot = true;
		}
		int plusBg = plusHovered ? 0xFFFFFFFF : 0xFF808080;
		context.fill(plusX, plusY, plusX + SLOT_SIZE, plusY + SLOT_SIZE, plusBg);
		context.fill(plusX, plusY, plusX + SLOT_SIZE, plusY + 1, 0xFF000000);
		context.fill(plusX, plusY + SLOT_SIZE - 1, plusX + SLOT_SIZE, plusY + SLOT_SIZE, 0xFF000000);
		context.fill(plusX, plusY, plusX + 1, plusY + SLOT_SIZE, 0xFF000000);
		context.fill(plusX + SLOT_SIZE - 1, plusY, plusX + SLOT_SIZE, plusY + SLOT_SIZE, 0xFF000000);
		
		drawCenteredPlusInSlot(context, plusX, plusY, 0xFF202020);
	}
	
	@Override
	public boolean mouseClicked(net.minecraft.client.gui.Click event, boolean isDoubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (button == 0) {
			int clickedTab = getTabAt(mouseX, mouseY);
			if (clickedTab == 0) {
				activeTab = TAB_ALT;
				selectedCustomKitId = null;
				selectedNeuKit = false;
				return true;
			}
			if (clickedTab == 1) {
				activeTab = TAB_NEU;
				selectedCustomKitId = null;
				if (!selectedNeuKit) {
					selectedKitType = null;
					selectedLevel = -1;
				}
				return true;
			}
			if (clickedTab == 2) {
				activeTab = TAB_EIGENE;
				selectedKitType = null;
				selectedLevel = -1;
				selectedNeuKit = false;
				return true;
			}
		}
		
		if (activeTab == TAB_NEU) {
			if (button != 0) {
				return false;
			}
			KitFilterUtility.KitType[] kitTypes = KitFilterUtility.NEU_KIT_TYPES;
			for (int kitIndex = 0; kitIndex < kitTypes.length; kitIndex++) {
				KitFilterUtility.KitType kitType = kitTypes[kitIndex];
				for (int level = 1; level <= KitFilterUtility.getNeuLevelCount(kitType); level++) {
					int slotX = gridStartX + ((level - 1) * GRID_SPACING_X);
					int slotY = gridStartY + (kitIndex * GRID_SPACING_Y);
					if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE
							&& mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
						selectedKitType = kitType;
						selectedLevel = level;
						selectedNeuKit = true;
						selectedCustomKitId = null;
						return true;
					}
				}
			}
			return super.mouseClicked(event, isDoubleClick);
		}
		
		if (activeTab == TAB_EIGENE) {
			java.util.List<CustomKit> kits = CustomKitManager.getKitsForButton(buttonIndex);
			int slotIndex = 0;
			for (CustomKit kit : kits) {
				int col = slotIndex % CUSTOM_COLUMNS;
				int row = slotIndex / CUSTOM_COLUMNS;
				int slotX = gridStartX + col * GRID_SPACING_X;
				int slotY = gridStartY + row * GRID_SPACING_Y;
				if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE
						&& mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
					if (button == 0) {
						selectedCustomKitId = kit.id;
						selectedKitType = null;
						selectedLevel = -1;
						selectedNeuKit = false;
						return true;
					}
					if (button == 1) {
						openCustomKitEditor(kit);
						return true;
					}
				}
				slotIndex++;
			}
			
			if (button == 0) {
				int plusCol = slotIndex % CUSTOM_COLUMNS;
				int plusRow = slotIndex / CUSTOM_COLUMNS;
				int plusX = gridStartX + plusCol * GRID_SPACING_X;
				int plusY = gridStartY + plusRow * GRID_SPACING_Y;
				if (mouseX >= plusX && mouseX < plusX + SLOT_SIZE
						&& mouseY >= plusY && mouseY < plusY + SLOT_SIZE) {
					openCustomKitEditor(null);
					return true;
				}
			}
			return super.mouseClicked(event, isDoubleClick);
		}
		
		if (button != 0) {
			return false;
		}
		
		// Prüfe ob ein Slot geklickt wurde
		KitFilterUtility.KitType[] kitTypes = KitFilterUtility.KitType.values();
		
		for (int kitIndex = 0; kitIndex < KIT_COUNT && kitIndex < kitTypes.length; kitIndex++) {
			KitFilterUtility.KitType kitType = kitTypes[kitIndex];
			
			for (int level = 1; level <= LEVEL_COUNT; level++) {
				int slotX = gridStartX + ((level - 1) * GRID_SPACING_X);
				int slotY = gridStartY + (kitIndex * GRID_SPACING_Y);
				
				if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
					mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
					
					// Slot auswählen
					selectedKitType = kitType;
					selectedLevel = level;
					selectedNeuKit = false;
					selectedCustomKitId = null;
					return true;
				}
			}
		}
		
		return super.mouseClicked(event, isDoubleClick);
	}
	
	/**
	 * Wird aufgerufen wenn "Auswählen" geklickt wird
	 */
	private void onDone() {
		if (activeTab == TAB_EIGENE) {
			if (selectedCustomKitId != null && !selectedCustomKitId.isEmpty()) {
				KitFilterUtility.setCustomKitSelection(buttonIndex, selectedCustomKitId);
				this.close();
			}
		} else if (activeTab == TAB_NEU && selectedNeuKit && selectedKitType != null && selectedLevel > 0) {
			KitFilterUtility.setNeuKitSelection(buttonIndex, selectedKitType, selectedLevel);
			this.close();
		} else if (activeTab == TAB_ALT && selectedKitType != null && selectedLevel > 0) {
			KitFilterUtility.setKitSelection(buttonIndex, selectedKitType, selectedLevel);
			this.close();
		}
	}
	
	private void openCustomKitEditor(CustomKit existing) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return;
		}
		HandledScreen<?> itemViewerScreen = KitFilterUtility.resolveKitEditorItemViewerBackground(previousScreen);
		if (existing == null) {
			client.setScreen(new CustomKitEditorScreen(buttonIndex, this, itemViewerScreen));
		} else {
			client.setScreen(new CustomKitEditorScreen(buttonIndex, this, itemViewerScreen, existing));
		}
	}
	
	/**
	 * Wird aufgerufen wenn "Abbrechen" geklickt wird
	 */
	private void onCancel() {
		this.close();
	}
	
	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyInput event) {
		int keyCode = event.key();
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			onCancel();
			return true;
		}
		return super.keyPressed(event);
	}
	
	@Override
	public void close() {
		KitFilterUtility.closeKitSelectionScreen();
		
		// Stelle den vorherigen Screen wieder her (Schmied-Inventar)
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && previousScreen != null) {
			client.setScreen(previousScreen);
		} else {
			super.close();
		}
	}
}


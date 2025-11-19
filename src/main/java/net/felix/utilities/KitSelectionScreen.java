package net.felix.utilities;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
	
	// Aktuell ausgewähltes Kit und Stufe
	private KitFilterUtility.KitType selectedKitType = null;
	private int selectedLevel = -1;
	
	// Hover-Informationen für Tooltip
	private KitFilterUtility.KitType hoveredKitType = null;
	private int hoveredLevel = -1;
	
	// Button zum Schließen
	private ButtonWidget doneButton;
	private ButtonWidget cancelButton;
	
	/**
	 * Gibt das Icon-Item für ein Kit zurück
	 */
	private static ItemStack getKitIcon(KitFilterUtility.KitType kitType) {
		switch (kitType) {
			case MÜNZ_KIT:
				return new ItemStack(Items.GOLD_NUGGET);
			case SCHADEN_KIT:
				return new ItemStack(Items.DIAMOND_SWORD);
			case RESSOURCEN_KIT:
				return new ItemStack(Items.DIAMOND_PICKAXE);
			case HERSTELLUNGS_KIT:
				return new ItemStack(Items.ANVIL);
			case TANK_KIT:
				return new ItemStack(Items.DIAMOND_CHESTPLATE);
			default:
				return ItemStack.EMPTY;
		}
	}
	
	public KitSelectionScreen(int buttonIndex) {
		super(Text.literal("Kit Auswahl"));
		this.buttonIndex = buttonIndex;
		
		// Speichere den aktuellen Screen (Schmied-Inventar) bevor wir den Kit-Auswahl-Screen öffnen
		this.previousScreen = MinecraftClient.getInstance().currentScreen;
		
		// Lade die aktuelle Auswahl für diesen Button
		KitFilterUtility.KitSelection currentSelection = KitFilterUtility.getKitSelection(buttonIndex);
		if (currentSelection != null) {
			this.selectedKitType = currentSelection.kitType;
			this.selectedLevel = currentSelection.level;
		}
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
		
		// Done-Button
		this.doneButton = ButtonWidget.builder(
			Text.literal("Auswählen"),
			button -> onDone()
		)
		.dimensions(this.width / 2 - 100, this.height - 30, 80, 20)
		.build();
		this.addDrawableChild(this.doneButton);
		
		// Cancel-Button
		this.cancelButton = ButtonWidget.builder(
			Text.literal("Abbrechen"),
			button -> onCancel()
		)
		.dimensions(this.width / 2 + 20, this.height - 30, 80, 20)
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
			renderKitGrid(context, mouseX, mouseY);
		}
		
		// Buttons rendern (wird automatisch gemacht)
		super.render(context, mouseX, mouseY, delta);
		
		// Tooltip rendern (nach super.render(), damit er über allem liegt)
		if (hoveredKitType != null && hoveredLevel > 0) {
			renderTooltip(context, mouseX, mouseY);
		}
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
	 */
	private boolean isBlueprintFound(String blueprintName) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			return false;
		}
		
		try {
			BPViewerUtility bpViewer = BPViewerUtility.getInstance();
			// Verwende die neue öffentliche Methode, die sowohl foundBlueprints als auch floorProgress prüft
			return bpViewer.isBlueprintFoundAnywhere(blueprintName);
		} catch (Exception e) {
			// Falls BPViewer nicht verfügbar ist, gebe false zurück
			return false;
		}
	}
	
	/**
	 * Parst die numerische Ebene aus einem String wie "e5", "e3", etc.
	 * Gibt 0 zurück, wenn keine Ebene vorhanden ist oder nicht geparst werden kann.
	 */
	private int parseEbeneNumber(String ebene) {
		if (ebene == null || ebene.isEmpty()) {
			return 0;
		}
		
		// Entferne "e" oder "E" am Anfang und versuche die Zahl zu parsen
		String cleaned = ebene.trim().toLowerCase();
		if (cleaned.startsWith("e")) {
			cleaned = cleaned.substring(1);
		}
		
		try {
			return Integer.parseInt(cleaned);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	/**
	 * Berechnet die Breite des Item-Typs
	 */
	private int calculateItemTypeWidth(KitFilterUtility.ItemInfo itemInfo) {
		if (itemInfo.itemType != null && !itemInfo.itemType.isEmpty()) {
			return this.textRenderer.getWidth(itemInfo.itemType);
		}
		return 0;
	}
	
	/**
	 * Berechnet die Breite der Modifier
	 */
	private int calculateModifierWidth(KitFilterUtility.ItemInfo itemInfo) {
		if (itemInfo.modifier == null || itemInfo.modifier.isEmpty()) {
			return 0;
		}
		
		int width = 0;
		String[] modifierParts = itemInfo.modifier.split(",\\s*");
		for (int i = 0; i < modifierParts.length; i++) {
			String part = modifierParts[i].trim();
			if (i > 0) {
				width += this.textRenderer.getWidth(", "); // Komma und Leerzeichen
			}
			
			// Parse Modifier: [ModifierName]
			if (part.startsWith("[") && part.endsWith("]")) {
				String modifierName = part.substring(1, part.length() - 1);
				width += this.textRenderer.getWidth("[");
				width += this.textRenderer.getWidth(modifierName);
				width += this.textRenderer.getWidth("]");
			} else {
				width += this.textRenderer.getWidth(part);
			}
		}
		return width;
	}
	
	/**
	 * Berechnet die Breite des Item-Namens
	 */
	private int calculateItemNameWidth(KitFilterUtility.ItemInfo itemInfo) {
		if (itemInfo.name != null && !itemInfo.name.isEmpty()) {
			return this.textRenderer.getWidth(itemInfo.name);
		}
		return 0;
	}
	
	/**
	 * Berechnet die Breite der Ebene
	 */
	private int calculateEbeneWidth(KitFilterUtility.ItemInfo itemInfo) {
		if (itemInfo.ebene != null && !itemInfo.ebene.isEmpty()) {
			return this.textRenderer.getWidth(itemInfo.ebene);
		}
		return 0;
	}
	
	/**
	 * Rendert einen Tooltip mit Kit-Namen, Stufe und Item-Namen in tabellarischer Form
	 */
	private void renderTooltip(DrawContext context, int mouseX, int mouseY) {
		// Formatiere Tooltip-Text (z.B. "Münz-Kit 1", "Schaden-Kit 4")
		String kitName = hoveredKitType.getDisplayName();
		String tooltipHeader = kitName + " " + hoveredLevel;
		
		// Hole die Item-Informationen für dieses Kit und Level
		java.util.Set<KitFilterUtility.ItemInfo> itemInfosSet = KitFilterUtility.getKitItemInfos(hoveredKitType, hoveredLevel);
		
		if (itemInfosSet.isEmpty()) {
			return; // Keine Items, kein Tooltip
		}
		
		// Konvertiere Set zu Liste und sortiere nach Ebene (aufsteigend)
		java.util.List<KitFilterUtility.ItemInfo> itemInfos = new java.util.ArrayList<>(itemInfosSet);
		itemInfos.sort((a, b) -> {
			// Extrahiere die numerische Ebene aus Strings wie "e5", "e3", etc.
			int ebeneA = parseEbeneNumber(a.ebene);
			int ebeneB = parseEbeneNumber(b.ebene);
			return Integer.compare(ebeneA, ebeneB);
		});
		
		// Berechne maximale Breiten für jede Spalte
		int maxItemTypeWidth = 0;
		int maxItemNameWidth = 0;
		int maxModifierWidth = 0;
		int maxEbeneWidth = 0;
		
		for (KitFilterUtility.ItemInfo itemInfo : itemInfos) {
			maxItemTypeWidth = Math.max(maxItemTypeWidth, calculateItemTypeWidth(itemInfo));
			maxItemNameWidth = Math.max(maxItemNameWidth, calculateItemNameWidth(itemInfo));
			maxModifierWidth = Math.max(maxModifierWidth, calculateModifierWidth(itemInfo));
			maxEbeneWidth = Math.max(maxEbeneWidth, calculateEbeneWidth(itemInfo));
		}
		
		// Spaltenabstände
		int columnSpacing = 12; // Abstand zwischen Spalten
		
		// Berechne Breite für Aufzählungspunkt
		String bulletPoint = "• ";
		int bulletWidth = this.textRenderer.getWidth(bulletPoint);
		
		// Berechne Breite für BPViewer Status (falls angezeigt)
		int statusWidth = 0;
		if (shouldShowBPViewerStatus()) {
			statusWidth = this.textRenderer.getWidth(" ✓") + columnSpacing; // Platz für Haken/Kreuz
		}
		
		// Berechne Gesamtbreite
		int headerWidth = this.textRenderer.getWidth(tooltipHeader);
		int totalWidth = Math.max(headerWidth, bulletWidth + maxItemTypeWidth + columnSpacing + maxItemNameWidth + columnSpacing + maxModifierWidth + columnSpacing + maxEbeneWidth + statusWidth);
		
		int textHeight = this.textRenderer.fontHeight;
		int padding = 4;
		int lineSpacing = 2;
		int offset = 10; // Abstand vom Mauszeiger
		
		// Berechne die Gesamthöhe (Header + Item-Informationen)
		int totalHeight = textHeight; // Header
		totalHeight += lineSpacing; // Abstand zwischen Header und Items
		totalHeight += (textHeight * itemInfos.size()); // Alle Item-Informationen
		totalHeight += (padding * 2); // Padding oben und unten
		
		int tooltipWidth = totalWidth + padding * 2;
		
		// Berechne X-Position: Versuche rechts vom Mauszeiger, falls nicht möglich dann links
		int tooltipX = mouseX + offset;
		if (tooltipX + tooltipWidth > this.width) {
			// Tooltip würde rechts rausragen, verschiebe nach links
			tooltipX = mouseX - tooltipWidth - offset;
			// Falls es dann links rausragt, positioniere es am linken Rand
			if (tooltipX < padding) {
				tooltipX = padding;
			}
		}
		// Stelle sicher, dass es nicht rechts rausragt
		if (tooltipX + tooltipWidth > this.width) {
			tooltipX = this.width - tooltipWidth - padding;
		}
		
		// Berechne Y-Position: Versuche oberhalb des Mauszeigers, falls nicht möglich dann unterhalb
		int tooltipY = mouseY - totalHeight - offset;
		if (tooltipY < padding) {
			// Tooltip würde oben rausragen, verschiebe nach unten
			tooltipY = mouseY + offset;
			// Falls es dann unten rausragt, positioniere es am unteren Rand
			if (tooltipY + totalHeight > this.height - padding) {
				tooltipY = this.height - totalHeight - padding;
			}
		}
		// Stelle sicher, dass es nicht unten rausragt
		if (tooltipY + totalHeight > this.height - padding) {
			tooltipY = this.height - totalHeight - padding;
		}
		// Stelle sicher, dass es nicht oben rausragt
		if (tooltipY < padding) {
			tooltipY = padding;
		}
		
		// Berechne Hintergrund-Positionen
		int bgX1 = tooltipX - padding;
		int bgY1 = tooltipY - padding;
		int bgX2 = tooltipX + totalWidth + padding;
		int bgY2 = tooltipY + totalHeight - padding;
		
		// Tooltip-Hintergrund (halbtransparent schwarz) - ZUERST
		context.fill(bgX1, bgY1, bgX2, bgY2, 0xF0000000);
		
		// Tooltip-Rahmen (weiß) - VOR Text
		context.fill(bgX1, bgY1, bgX2, bgY1 + 1, 0xFFFFFFFF); // Oben
		context.fill(bgX1, bgY2 - 1, bgX2, bgY2, 0xFFFFFFFF); // Unten
		context.fill(bgX1, bgY1, bgX1 + 1, bgY2, 0xFFFFFFFF); // Links
		context.fill(bgX2 - 1, bgY1, bgX2, bgY2, 0xFFFFFFFF); // Rechts
		
		// Tooltip-Header (Kit-Name + Level)
		context.drawText(this.textRenderer, tooltipHeader, tooltipX, tooltipY, 0xFFFFFFFF, true);
		
		// Tooltip-Item-Informationen in tabellarischer Form
		int currentY = tooltipY + textHeight + lineSpacing;
		for (KitFilterUtility.ItemInfo itemInfo : itemInfos) {
			int currentX = tooltipX;
			
			// Aufzählungspunkt vor jeder Zeile
			context.drawText(this.textRenderer, bulletPoint, currentX, currentY, 0xFFFFFFFF, true);
			currentX += bulletWidth;
			
			// Spalte 1: Item-Typ (linksbündig)
			if (itemInfo.itemType != null && !itemInfo.itemType.isEmpty()) {
				context.drawText(this.textRenderer, itemInfo.itemType, currentX, currentY, 0xFFFFFFFF, true);
			}
			currentX += maxItemTypeWidth + columnSpacing;
			
			// Spalte 2: Item-Name (linksbündig, mit entsprechender Farbe)
			if (itemInfo.name != null && !itemInfo.name.isEmpty()) {
				int nameColor = (itemInfo.nameColorString != null && !itemInfo.nameColorString.isEmpty()) 
					? itemInfo.nameColor 
					: 0xFFFFFFFF;
				context.drawText(this.textRenderer, itemInfo.name, currentX, currentY, nameColor, true);
			}
			currentX += maxItemNameWidth + columnSpacing;
			
			// Spalte 3: Modifier (linksbündig in der Spalte)
			if (itemInfo.modifier != null && !itemInfo.modifier.isEmpty()) {
				// Parse und rendere Modifier mit Farben
				String[] modifierParts = itemInfo.modifier.split(",\\s*");
				int modifierX = currentX;
				for (int i = 0; i < modifierParts.length; i++) {
					String part = modifierParts[i].trim();
					if (i > 0) {
						// Rendere Komma und Leerzeichen in weiß
						String separator = ", ";
						context.drawText(this.textRenderer, separator, modifierX, currentY, 0xFFFFFFFF, true);
						modifierX += this.textRenderer.getWidth(separator);
					}
					
					// Parse Modifier: [ModifierName]
					if (part.startsWith("[") && part.endsWith("]")) {
						String modifierName = part.substring(1, part.length() - 1);
						int modifierColor = KitFilterUtility.ItemInfo.parseModifierColor(modifierName);
						
						// Rendere öffnende Klammer in weiß
						context.drawText(this.textRenderer, "[", modifierX, currentY, 0xFFFFFFFF, true);
						modifierX += this.textRenderer.getWidth("[");
						
						// Rendere Modifier-Name in der entsprechenden Farbe
						context.drawText(this.textRenderer, modifierName, modifierX, currentY, modifierColor, true);
						modifierX += this.textRenderer.getWidth(modifierName);
						
						// Rendere schließende Klammer in weiß
						context.drawText(this.textRenderer, "]", modifierX, currentY, 0xFFFFFFFF, true);
						modifierX += this.textRenderer.getWidth("]");
					} else {
						// Fallback: Rendere als normalen Text in weiß
						context.drawText(this.textRenderer, part, modifierX, currentY, 0xFFFFFFFF, true);
						modifierX += this.textRenderer.getWidth(part);
					}
				}
			}
			currentX += maxModifierWidth + columnSpacing;
			
			// Spalte 4: Ebene (linksbündig in der Spalte)
			if (itemInfo.ebene != null && !itemInfo.ebene.isEmpty()) {
				context.drawText(this.textRenderer, itemInfo.ebene, currentX, currentY, 0xFFFFFFFF, true);
				currentX += this.textRenderer.getWidth(itemInfo.ebene);
			} else {
				currentX += maxEbeneWidth;
			}
			
			// Spalte 5: BPViewer Status (Haken/Kreuz) - nur wenn aktiviert und in speziellem Inventar
			if (shouldShowBPViewerStatus()) {
				boolean isFound = isBlueprintFound(itemInfo.name);
				String statusSymbol = isFound ? " ✓" : " ✗";
				int statusColor = isFound ? 0xFF00FF00 : 0xFFFF0000; // Grün für Haken, Rot für Kreuz
				context.drawText(this.textRenderer, statusSymbol, currentX, currentY, statusColor, true);
			}
			
			currentY += textHeight;
		}
	}
	
	/**
	 * Rendert das Kit-Grid (5 Kits x 7 Stufen)
	 */
	private void renderKitGrid(DrawContext context, int mouseX, int mouseY) {
		KitFilterUtility.KitType[] kitTypes = KitFilterUtility.KitType.values();
		
		// Reset hover-Informationen
		hoveredKitType = null;
		hoveredLevel = -1;
		
		// Rendere jede Reihe (Kit)
		for (int kitIndex = 0; kitIndex < KIT_COUNT && kitIndex < kitTypes.length; kitIndex++) {
			KitFilterUtility.KitType kitType = kitTypes[kitIndex];
			
			// Hole das Icon-Item für dieses Kit
			ItemStack kitIcon = getKitIcon(kitType);
			
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
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) return false; // Nur Linksklick
		
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
					return true;
				}
			}
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	/**
	 * Wird aufgerufen wenn "Auswählen" geklickt wird
	 */
	private void onDone() {
		if (selectedKitType != null && selectedLevel > 0) {
			// Speichere die Auswahl
			KitFilterUtility.setKitSelection(buttonIndex, selectedKitType, selectedLevel);
			
			// Schließe den Screen
			this.close();
		}
	}
	
	/**
	 * Wird aufgerufen wenn "Abbrechen" geklickt wird
	 */
	private void onCancel() {
		this.close();
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			onCancel();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
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


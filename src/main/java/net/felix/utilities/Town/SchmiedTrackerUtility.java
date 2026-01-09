package net.felix.utilities.Town;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.ZeichenUtility;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SchmiedTrackerUtility {
	

	
	private static boolean isInitialized = false;
	private static boolean isInDisassembleChest = false;
	private static Map<Integer, Integer> slotColors = new HashMap<>(); // slotIndex -> color
	
	// Hide Uncraftable Button State
	private static boolean hideUncraftableActive = false;
	private static boolean isInBlueprintInventory = false;
	private static boolean wasInBlueprintInventory = false; // Track previous state
	private static int buttonX = 0;
	private static int buttonY = 0;
	private static int buttonWidth = 120;
	private static int buttonHeight = 20;
	private static Map<Integer, ItemStack> originalItems = new HashMap<>(); // slotIndex -> original ItemStack
	
	// Hide Wrong Class Button State
	private static boolean hideWrongClassActive = false;
	private static int wrongClassButtonX = 0;
	private static int wrongClassButtonY = 0;
	private static Map<Integer, ItemStack> originalItemsWrongClass = new HashMap<>(); // slotIndex -> original ItemStack for wrong class filter
	
	// Frame Toggle State
	private static boolean framesVisible = true; // Standardmäßig sichtbar
	private static KeyBinding toggleFramesKeyBinding;
	
	// Schmied-Typen und ihre Konfigurationsschlüssel
	private static final Map<String, String> SMITHING_CONFIG_KEYS = new HashMap<>();
	static {
		SMITHING_CONFIG_KEYS.put("[Frostgeschmiedet]", "frostgeschmiedet");
		SMITHING_CONFIG_KEYS.put("[Lavageschmiedet]", "lavageschmiedet");
		SMITHING_CONFIG_KEYS.put("[Titangeschmiedet]", "titangeschmiedet");
		SMITHING_CONFIG_KEYS.put("[Drachengeschmiedet]", "drachengeschmiedet");
		SMITHING_CONFIG_KEYS.put("[Dämonengeschmiedet]", "daemonengeschmiedet");
		SMITHING_CONFIG_KEYS.put("[Blitzgeschmiedet]", "blitzgeschmiedet");
		SMITHING_CONFIG_KEYS.put("[Sterngeschmiedet]", "sternengeschmiedet");
		SMITHING_CONFIG_KEYS.put("[Sternengeschmiedet]", "sternengeschmiedet");
	}
	
	// Slot-Größe für Rahmen
	private static final int SLOT_SIZE = 16;

	public static void initialize() {
		if (isInitialized) {
			return;
		}
		
		// Hotkey registrieren
		registerHotkey();
		
		// Client-seitige Events registrieren
		ClientTickEvents.END_CLIENT_TICK.register(SchmiedTrackerUtility::onClientTick);
		
		isInitialized = true;
	}
	
	/**
	 * Registriert den Hotkey zum Togglen der Rahmen
	 */
	private static void registerHotkey() {
		toggleFramesKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.cclive-utilities.toggle-schmied-frames",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_F7, // Standard: F7
			"category.cclive-utilities.schmied"
		));
	}
	
	/**
	 * Loads the materials database from materials.json
	 */


	private static void onClientTick(MinecraftClient client) {
		// Prüfe ob der Hotkey zum Togglen der Rahmen gedrückt wurde
		// Dies sollte IMMER geprüft werden, unabhängig von der Config, damit der Toggle funktioniert
		if (toggleFramesKeyBinding != null && toggleFramesKeyBinding.wasPressed()) {
			framesVisible = !framesVisible;
		}
		
		// Prüfe Konfiguration
				if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().schmiedTrackerEnabled ||
			!CCLiveUtilitiesConfig.HANDLER.instance().showSchmiedTracker) {
			return;
		}
		
		if (client.player == null || client.currentScreen == null) {
			isInDisassembleChest = false;
			slotColors.clear();
			return;
		}

		// Überprüfe ob wir in einem "Zerlegen" Kisteninventar sind
		if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
			String title = handledScreen.getTitle().getString();
			
			// IMPORTANT: Check for Equipment Display BEFORE cleaning, as cleaning removes the characters
			boolean isEquipmentDisplay = ZeichenUtility.containsEquipmentDisplay(title);
			
			// Remove Minecraft formatting codes and Unicode characters for comparison
			String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "")
								   .replaceAll("[\\u3400-\\u4DBF]", "");
			
			// Check for blueprint inventories
			// Inventare für Hide Uncraftable Button
			if (cleanTitle.contains("Baupläne [Waffen]") || cleanTitle.contains("Baupläne [Rüstung]") || cleanTitle.contains("Baupläne [Werkzeuge]") ||
				cleanTitle.contains("Favorisierte [Waffenbaupläne]") || cleanTitle.contains("Favorisierte [Rüstungsbaupläne]") || cleanTitle.contains("Favorisierte [Werkzeugbaupläne]") ||
				cleanTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools") || cleanTitle.contains("Bauplan [Shop]")) {
				// NEUE LOGIK: Beim Betreten eines Blueprint-Inventars immer alle Items anzeigen
				if (!wasInBlueprintInventory) {
					// Wir betreten gerade ein Blueprint-Inventar - stelle sicher dass alle Items sichtbar sind
					hideUncraftableActive = false;
					hideWrongClassActive = false;
					originalItems.clear();
					originalItemsWrongClass.clear();
				}
				
				isInBlueprintInventory = true;
				isInDisassembleChest = false;
				updateButtonPosition(handledScreen, client);
				updateWrongClassButtonPosition(handledScreen, client);
				if (hideUncraftableActive) {
					updateBlueprintItems(handledScreen, client);
				}
				if (hideWrongClassActive) {
					updateBlueprintItemsWrongClass(handledScreen, client);
				}
			// Inventare für Schmiedezustände	
			} else if (cleanTitle.contains("Zerlegen") || cleanTitle.contains("Umschmieden") || 
			(cleanTitle.contains("Ausrüstung") && cleanTitle.contains("Auswählen")) || cleanTitle.contains("Aufwerten") || 
			cleanTitle.contains("Rüstungs Sammlung") || cleanTitle.contains("Waffen Sammlung") || 
			cleanTitle.contains("Werkzeug Sammlung") || cleanTitle.contains("CACTUS_CLICKER.CACTUS_CLICKER") || 
			cleanTitle.contains("Geschützte Items") ||
			isEquipmentDisplay) {//Equipment Display - checked BEFORE cleaning
				isInDisassembleChest = true;
				isInBlueprintInventory = false;
				updateSlotColors(handledScreen, client);
			} else {
				isInDisassembleChest = false;
				// Prüfe ob wir gerade ein Blueprint-Inventar verlassen haben
				if (wasInBlueprintInventory && !isInBlueprintInventory) {
					// Wir haben ein Blueprint-Inventar verlassen - stelle alle Items wieder her
					restoreOriginalItems(handledScreen);
					restoreOriginalItemsWrongClass(handledScreen);
					originalItems.clear();
					originalItemsWrongClass.clear();
					hideUncraftableActive = false; // Deaktiviere den Button-Status
					hideWrongClassActive = false; // Deaktiviere den Button-Status
				}
				isInBlueprintInventory = false;
				slotColors.clear();
			}
		} else {
			isInDisassembleChest = false;
			// Prüfe ob wir gerade ein Blueprint-Inventar verlassen haben
			if (wasInBlueprintInventory && !isInBlueprintInventory) {
				// Wir haben ein Blueprint-Inventar verlassen - stelle alle Items wieder her
				restoreOriginalItems(null);
				restoreOriginalItemsWrongClass(null);
				originalItems.clear();
				originalItemsWrongClass.clear();
				hideUncraftableActive = false; // Deaktiviere den Button-Status
				hideWrongClassActive = false; // Deaktiviere den Button-Status
			}
			isInBlueprintInventory = false;
			slotColors.clear();
		}
		
		// Update the previous state
		wasInBlueprintInventory = isInBlueprintInventory;
	}

	private static void updateSlotColors(HandledScreen<?> screen, MinecraftClient client) {
		slotColors.clear();
		
		// Prüfe ob Schmiedezustände in Ausrüstungs-Menüs angezeigt werden sollen
		if (!CCLiveUtilitiesConfig.HANDLER.instance().showSchmiedezustaendeInAusrüstungsMenü) {
			return;
		}
		
		// Prüfe ob es sich um ein Equipment Display Inventar handelt
		String title = screen.getTitle().getString();
		boolean isEquipmentDisplay = ZeichenUtility.containsEquipmentDisplay(title);
		
		// Bestimme die zu prüfenden Slots basierend auf dem Inventar-Typ
		int startSlot, endSlot;
		if (isEquipmentDisplay) {
			// In Equipment Display Inventaren: Slots 0-53
			startSlot = 0;
			endSlot = 53;
		} else {
			// In anderen Inventaren: Slots 9-44 (Standard)
			startSlot = 9;
			endSlot = 44;
		}
		
		// Prüfe die entsprechenden Slots
		for (int slotIndex = startSlot; slotIndex <= endSlot; slotIndex++) {
			if (slotIndex < screen.getScreenHandler().slots.size()) {
				Slot slot = screen.getScreenHandler().slots.get(slotIndex);
				ItemStack itemStack = slot.getStack();
				
				if (!itemStack.isEmpty()) {
					// Hole die Tooltip-Daten des Items
					List<Text> lore = getItemTooltip(itemStack, client.player);
					
					// Prüfe auf Schmied-Typen
					for (Text text : lore) {
						String line = text.getString();
						for (Map.Entry<String, String> entry : SMITHING_CONFIG_KEYS.entrySet()) {
							if (line.contains(entry.getKey())) {
								String configKey = entry.getValue();
								if (isSchmiedTypeEnabled(configKey)) {
									int color = getSchmiedTypeColor(configKey);
									slotColors.put(slotIndex, color);
								}
								break;
							}
						}
					}
				}
			}
		}
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

	/**
	 * Prüft ob ein Schmied-Typ in der Konfiguration aktiviert ist
	 */
	private static boolean isSchmiedTypeEnabled(String configKey) {
		switch (configKey) {
			case "frostgeschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().frostgeschmiedetEnabled;
			case "lavageschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().lavageschmiedetEnabled;
			case "titangeschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().titangeschmiedetEnabled;
			case "drachengeschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().drachengeschmiedetEnabled;
			case "daemonengeschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().daemonengeschmiedetEnabled;
			case "blitzgeschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().blitzgeschmiedetEnabled;
			case "sternengeschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().sternengeschmiedetEnabled;
			default:
				return false;
		}
	}
	
	/**
	 * Holt die Farbe für einen Schmied-Typ aus der Konfiguration
	 */
	private static int getSchmiedTypeColor(String configKey) {
		switch (configKey) {
			case "frostgeschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().frostgeschmiedetColor.getRGB();
			case "lavageschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().lavageschmiedetColor.getRGB();
			case "titangeschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().titangeschmiedetColor.getRGB();
			case "drachengeschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().drachengeschmiedetColor.getRGB();
			case "daemonengeschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().daemonengeschmiedetColor.getRGB();
			case "blitzgeschmiedet":
				return CCLiveUtilitiesConfig.HANDLER.instance().blitzgeschmiedetColor.getRGB();
			case "sternengeschmiedet":
				if (CCLiveUtilitiesConfig.HANDLER.instance().sternengeschmiedetRainbow) {
					return getRainbowColor();
				} else {
					return CCLiveUtilitiesConfig.HANDLER.instance().sternengeschmiedetColor.getRGB();
				}
			default:
				return 0xFFFFFFFF; // Weiß als Fallback
		}
	}
	
	/**
	 * Prüft ob ein Item die spezifischen Unicode-Crafting-Indikatoren hat
	 * Erweiterte Erkennung für alle Unicode-Zeichen: 㔞㔟㔠㔡㔢㔣㔤㔥㔦㔧㔨㔩㔪㔫㔬㔭㔮㔯㔰㔱㔲㔳㔴㔵㔶㔷㔸㔹㔺㔻㔼㔽㔾㔿㕀㕁㕂㕃㕄
	 * 
	 * Diese Methode erkennt die speziellen Unicode-Zeichen die vor und nach dem Item-Namen stehen:
	 * - Türkise Unicode-Zeichen = Item ist craftbar (wird NICHT ausgeblendet)
	 * - Weiße Unicode-Zeichen = Item ist uncraftbar (wird ausgeblendet)
	 * 
	 * Dies ist eine bessere Methode als die Glint-Erkennung, da sie spezifisch für das Crafting-System ist.
	 */
	private static boolean hasCraftingUnicodeIndicators(String nameText) {
		if (nameText == null || nameText.length() < 2) {
			return false;
		}
		
		char firstChar = nameText.charAt(0);
		char lastChar = nameText.charAt(nameText.length() - 1);
		
		int firstCode = (int)firstChar;
		int lastCode = (int)lastChar;
		
		// Spezifische Unicode-Codes für die Crafting-Indikatoren: 㔞㔟㔠㔡㔢㔣㔤㔥㔦㔧㔨㔩㔪㔫㔬㔭㔮㔯㔰㔱㔲㔳㔴㔵㔶㔷㔸㔹㔺㔻㔼㔽㔾㔿㕀㕁㕂㕃㕄
		// Diese stehen vor und hinter dem Item-Namen
		boolean hasFirstIndicator = (firstCode >= 13617 && firstCode <= 13668); // 㔞 bis 㕄
		boolean hasLastIndicator = (lastCode >= 13617 && lastCode <= 13668); // 㔞 bis 㕄
		
		// Zusätzliche Prüfung: Stelle sicher, dass die Unicode-Zeichen tatsächlich am Anfang und Ende stehen
		// und nicht mitten im Text vorkommen
		if (hasFirstIndicator && hasLastIndicator) {
			// Prüfe, ob der Text zwischen den Unicode-Zeichen nicht leer ist
			String middleText = nameText.substring(1, nameText.length() - 1);
			if (!middleText.trim().isEmpty()) {
				return true; // Unicode-Zeichen am Anfang und Ende mit Text dazwischen
			}
		}
		
		return false;
	}
	
	/**
	 * Prüft ob die Unicode-Zeichen weiß sind (uncraftbar)
	 * 
	 * Weiße Unicode-Zeichen 㔞㔟㔠㔡㔢㔣㔤㔥㔦㔧㔨㔩㔪㔫㔬㔭㔮㔯㔰㔱㔲㔳㔴㔵㔶㔷㔸㔹㔺㔻㔼㔽㔾㔿㕀㕁㕂㕃㕄 bedeuten, dass das Item NICHT craftbar ist.
	 * Diese Items werden vom Hide Uncraftable Button ausgeblendet.
	 */
	private static boolean isUnicodeWhite(net.minecraft.text.Style style) {
		if (style == null || style.getColor() == null) {
			return true; // Keine Farbe = weiß (uncraftbar)
		}
		
		int colorRgb = style.getColor().getRgb();
		
		// Spezifische Farben die NICHT weiß sind (craftbar)
		if (colorRgb == 0x00FFFF) { // Türkis
			return false; // Türkis = craftbar
		}
		
		// Nur reine Weiß-Töne sind uncraftbar
		// Sehr restriktive Weiß-Erkennung
		return (colorRgb == 0xFFFFFF || colorRgb == 0xFFFFFFFF);
	}
	
	/**
	 * Prüft ob die Unicode-Zeichen farbig sind (craftbar)
	 * 
	 * Farbige Unicode-Zeichen 㔞㔟㔠㔡㔢㔣㔤㔥㔦㔧㔨㔩㔪㔫㔬㔭㔮㔯㔰㔱㔲㔳㔴㔵㔶㔷㔸㔹㔺㔻㔼㔽㔾㔿㕀㕁㕂㕃㕄 bedeuten, dass das Item craftbar ist.
	 * Diese Items werden vom Hide Uncraftable Button NICHT ausgeblendet.
	 */
	private static boolean isUnicodeCyan(net.minecraft.text.Style style) {
		if (style == null || style.getColor() == null) {
			return false; // Keine Farbe = nicht farbig
		}
		
		int colorRgb = style.getColor().getRgb();
		
		// Spezifische Erkennung für Türkis (0x00FFFF) = craftbar
		if (colorRgb == 0x00FFFF) {
			return true; // Türkis = craftbar
		}
		
		// Prüfe auf weiße/graue Farben (uncraftbar)
		if (colorRgb == 0xFFFFFF || colorRgb == 0xFFFFFFFF || 
			colorRgb == 0xF0F0F0 || colorRgb == 0xE0E0E0 || 
			colorRgb == 0xD0D0D0 || colorRgb == 0xC0C0C0 ||
			colorRgb == 0xE6E6E6 || colorRgb == 0xCCCCCC ||
			colorRgb == 0xDDDDDD || colorRgb == 0xEEEEEE) {
			return false; // Weiß/grau = uncraftbar
		}
		
		// Alle anderen Farben = craftbar
		return true;
	}
	
	/**
	 * Analysiert die Craftbarkeit basierend auf Unicode-Codes und Text-Inhalt
	 * Diese Methode wird nur verwendet, wenn die Farbanalyse nicht funktioniert
	 */
	private static boolean analyzeCraftabilityByUnicodeCodes(int firstCode, int lastCode, String nameText) {
		// Diese Methode wird nur als Fallback verwendet, wenn die Farbanalyse nicht funktioniert
		// Da die Farbanalyse jetzt korrekt implementiert ist, sollte diese Methode nicht mehr benötigt werden
		
		// Prüfe ob die Unicode-Codes im erwarteten Bereich liegen
		if (firstCode >= 13617 && firstCode <= 13668 && lastCode >= 13617 && lastCode <= 13668) {
			// Ohne Farbinformationen können wir nicht entscheiden
			// Standard: als uncraftbar behandeln (sicherheitshalber)
			return false;
		}
		
		// Falls die Unicode-Codes nicht im erwarteten Bereich liegen,
		// prüfe auf spezielle Item-Namen die craftbar sein könnten
		String lowerName = nameText.toLowerCase();
		
		// Liste von Items die definitiv uncraftbar sind (basierend auf Ihren Logs)
		if (lowerName.contains("champions wappengurt") ||
			lowerName.contains("drachenlederhelm") ||
			lowerName.contains("drachenlederschuhe") ||
			lowerName.contains("enderstahl waffengurt") ||
			lowerName.contains("eroberers schulterpanzer") ||
			lowerName.contains("ewiges lichtband") ||
			lowerName.contains("flora panzerbrust") ||
			lowerName.contains("gebeulte handgelenkmanschetten") ||
			lowerName.contains("halskette des bezwingers") ||
			lowerName.contains("kappe des forschers") ||
			lowerName.contains("kopfgeldjägerring") ||
			lowerName.contains("natürliche lederfinger") ||
			lowerName.contains("pelzgewand des eiswanderers") ||
			lowerName.contains("rote lederhandschuhe") ||
			lowerName.contains("rubinhelmschale") ||
			lowerName.contains("rubinrote kriegshose") ||
			lowerName.contains("rubinrote kriegsschultern") ||
			lowerName.contains("ruhmreicher kopfschutz") ||
			lowerName.contains("solide stahlfingerlinge") ||
			lowerName.contains("stählernde sabatons") ||
			lowerName.contains("zeitloser lederband")) {
			
			// Diese Items sind alle uncraftbar
			return false;
		}
		
		// Standard: Wenn keine spezielle Regel greift, behandle als uncraftbar
		return false;
	}
	
	/**
	 * Analysiert die Farbe eines Textes durch Parsing der Formatierung
	 */
	private static String analyzeTextColor(Text text) {
		if (text == null) {
			return "null";
		}
		
		try {
			// 1. Versuche die Farbe aus dem Style zu extrahieren
			var style = text.getStyle();
			if (style != null && style.getColor() != null) {
				return "0x" + Integer.toHexString(style.getColor().getRgb()).toUpperCase() + " (Style)";
			}
			
			// 2. Rekursive Analyse der Text-Komponenten
			String recursiveResult = analyzeTextColorRecursive(text);
			if (recursiveResult != null && !recursiveResult.equals("null")) {
				return recursiveResult;
			}
			
			// 3. Versuche die Farbe aus dem rohen String zu extrahieren
			String textString = text.getString();
			if (textString.contains("§")) {
				// Suche nach Minecraft-Farbcodes
				for (int i = 0; i < textString.length() - 1; i++) {
					if (textString.charAt(i) == '§') {
						char colorCode = textString.charAt(i + 1);
						switch (colorCode) {
							case 'f': return "0xFFFFFF (Weiß)"; // Weiß
							case 'F': return "0xFFFFFF (Weiß)"; // Weiß
							case 'e': return "0xFFFF55 (Gelb)"; // Gelb
							case 'E': return "0xFFFF55 (Gelb)"; // Gelb
							case 'a': return "0x55FF55 (Grün)"; // Grün
							case 'A': return "0x55FF55 (Grün)"; // Grün
							case 'b': return "0x5555FF (Blau)"; // Blau
							case 'B': return "0x5555FF (Blau)"; // Blau
							case 'c': return "0xFF5555 (Rot)"; // Rot
							case 'C': return "0xFF5555 (Rot)"; // Rot
							case 'd': return "0xFF55FF (Magenta)"; // Magenta
							case 'D': return "0xFF55FF (Magenta)"; // Magenta
							case '3': return "0x00AAAA (Türkis)"; // Türkis
							case '9': return "0x5555FF (Blau)"; // Blau
							case '5': return "0xAA00AA (Lila)"; // Lila
							case '6': return "0xFFAA00 (Gold)"; // Gold
							case '7': return "0xAAAAAA (Grau)"; // Grau
							case '8': return "0x555555 (Dunkelgrau)"; // Dunkelgrau
							case '0': return "0x000000 (Schwarz)"; // Schwarz
							case '1': return "0x0000AA (Dunkelblau)"; // Dunkelblau
							case '2': return "0x00AA00 (Dunkelgrün)"; // Dunkelgrün
							case '4': return "0xAA0000 (Dunkelrot)"; // Dunkelrot
						}
					}
				}
			}
			
			// 4. Spezielle Behandlung für Unicode-Zeichen
			if (textString.length() >= 2 && hasCraftingUnicodeIndicators(textString)) {
				// Versuche die Farbe aus dem Style zu extrahieren (falls verfügbar)
				var unicodeStyle = text.getStyle();
				if (unicodeStyle != null && unicodeStyle.getColor() != null) {
					int colorRgb = unicodeStyle.getColor().getRgb();
					return "0x" + Integer.toHexString(colorRgb).toUpperCase() + " (Unicode Style)";
				}
				
				// Wenn keine Style-Farbe gefunden wurde, gib eine neutrale Antwort zurück
				return "null (keine Style-Farbe für Unicode)";
			}
			
			return "null (keine Farbe erkannt)";
		} catch (Exception e) {
			return "null (Fehler bei Farbanalyse: " + e.getMessage() + ")";
		}
	}
	
	/**
	 * Rekursiv analysiert Text-Komponenten um Farben zu finden
	 */
	private static String analyzeTextColorRecursive(Text text) {
		if (text == null) {
			return null;
		}
		
		try {
			// Prüfe den Style dieser Komponente
			var style = text.getStyle();
			if (style != null && style.getColor() != null) {
				int colorRgb = style.getColor().getRgb();
				return "0x" + Integer.toHexString(colorRgb).toUpperCase() + " (Rekursiv)";
			}
			
			// Prüfe die Siblings (Geschwister-Komponenten)
			var siblings = text.getSiblings();
			if (siblings != null) {
				for (Text sibling : siblings) {
					String siblingResult = analyzeTextColorRecursive(sibling);
					if (siblingResult != null && !siblingResult.equals("null")) {
						return siblingResult;
					}
				}
			}
			
			// Prüfe auf spezielle Text-Typen basierend auf der Klasse
			String className = text.getClass().getSimpleName();
			if (className.contains("Literal") || className.contains("Text")) {
				// Versuche nochmal den Style zu prüfen
				var textStyle = text.getStyle();
				if (textStyle != null && textStyle.getColor() != null) {
					int colorRgb = textStyle.getColor().getRgb();
					return "0x" + Integer.toHexString(colorRgb).toUpperCase() + " (" + className + ")";
				}
			}
			
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Speziell für Unicode-Zeichen: Analysiert die Farbe der ersten und letzten Zeichen
	 */
	private static String analyzeUnicodeColor(Text text) {
		if (text == null) {
			return "null";
		}
		
		try {
			String textString = text.getString();
			if (textString.length() < 2) {
				return "null (Text zu kurz)";
			}
			
			// Prüfe ob es sich um Unicode-Indikatoren handelt
			if (!hasCraftingUnicodeIndicators(textString)) {
				return "null (keine Unicode-Indikatoren)";
			}
			
			// NEUE METHODE: Analysiere die Siblings um die Unicode-Farbe zu finden
			var siblings = text.getSiblings();
			if (siblings != null && !siblings.isEmpty()) {
				// Suche nach den ersten und letzten Siblings (die Unicode-Zeichen enthalten)
				if (siblings.size() >= 2) {
					Text firstSibling = siblings.get(0);
					Text lastSibling = siblings.get(siblings.size() - 1);
					
					// Prüfe ob die ersten und letzten Siblings Unicode-Zeichen enthalten
					String firstSiblingString = firstSibling.getString();
					String lastSiblingString = lastSibling.getString();
					
					if (firstSiblingString.length() > 0 && lastSiblingString.length() > 0) {
						char firstSiblingChar = firstSiblingString.charAt(0);
						char lastSiblingChar = lastSiblingString.charAt(0);
						int firstSiblingCode = (int)firstSiblingChar;
						int lastSiblingCode = (int)lastSiblingChar;
						
						// Prüfe ob es sich um Unicode-Zeichen handelt
						if ((firstSiblingCode >= 13617 && firstSiblingCode <= 13668) && 
							(lastSiblingCode >= 13617 && lastSiblingCode <= 13668)) {
							
							// NEUE LOGIK: Die Unicode-Zeichen erben die Farbe vom umgebenden Kontext
							// Da die Unicode-Zeichen selbst keinen Style haben, verwenden wir die Farbe des Item-Namens
							// als Indikator für die Unicode-Farbe
							
							// Suche nach dem Sibling mit dem Item-Namen (normalerweise der zweite)
							if (siblings.size() >= 3) {
								Text itemNameSibling = siblings.get(1);
								var itemNameStyle = itemNameSibling.getStyle();
								if (itemNameStyle != null && itemNameStyle.getColor() != null) {
									int colorRgb = itemNameStyle.getColor().getRgb();
									return "0x" + Integer.toHexString(colorRgb).toUpperCase() + " (Unicode Context)";
								}
							}
							
							// PRIORITÄT 1: Versuche die Farbe aus dem ersten Unicode-Sibling zu extrahieren
							var firstSiblingStyle = firstSibling.getStyle();
							if (firstSiblingStyle != null && firstSiblingStyle.getColor() != null) {
								int colorRgb = firstSiblingStyle.getColor().getRgb();
								return "0x" + Integer.toHexString(colorRgb).toUpperCase() + " (Unicode Sibling)";
							}
							
							// PRIORITÄT 2: Versuche die Farbe aus dem letzten Unicode-Sibling zu extrahieren
							var lastSiblingStyle = lastSibling.getStyle();
							if (lastSiblingStyle != null && lastSiblingStyle.getColor() != null) {
								int colorRgb = lastSiblingStyle.getColor().getRgb();
								return "0x" + Integer.toHexString(colorRgb).toUpperCase() + " (Unicode Sibling)";
							}
							
							// PRIORITÄT 3: Versuche die Farbe aus dem Root-Style zu extrahieren
							var rootStyle = text.getStyle();
							if (rootStyle != null && rootStyle.getColor() != null) {
								int colorRgb = rootStyle.getColor().getRgb();
								return "0x" + Integer.toHexString(colorRgb).toUpperCase() + " (Unicode Root)";
							}
							
							// Wenn keine Unicode-Farbe gefunden wurde, verwende Standard weiß
							return "0xFFFFFF (Unicode Standard Weiß)";
						}
					}
				}
			}
			
			// Fallback: Verwende die heuristische Methode
			return analyzeUnicodeColorHeuristic(textString);
			
		} catch (Exception e) {
			return "null (Fehler bei Unicode-Analyse: " + e.getMessage() + ")";
		}
	}
	
	/**
	 * Findet automatisch die Blueprint-Slots im Inventar
	 */
	private static List<Integer> findBlueprintSlots(HandledScreen<?> screen) {
		List<Integer> blueprintSlots = new ArrayList<>();
		
		// NEUE LOGIK: Nur Blueprint-Slots (Kisteninventar) berücksichtigen
		// Player-Inventar und Hotbar werden ignoriert
		
		// Standard Blueprint-Slots: 10-16, 19-25, 28-34, 37-43
		// Diese Slots gehören zum Kisteninventar, nicht zum Player-Inventar
		int[] blueprintSlotRanges = {
			10, 11, 12, 13, 14, 15, 16,  // Erste Reihe
			19, 20, 21, 22, 23, 24, 25,  // Zweite Reihe  
			28, 29, 30, 31, 32, 33, 34,  // Dritte Reihe
			37, 38, 39, 40, 41, 42, 43   // Vierte Reihe
		};
		
		// Durchsuche nur die Blueprint-Slots nach Items mit Unicode-Indikatoren
		for (int slotIndex : blueprintSlotRanges) {
			if (slotIndex < screen.getScreenHandler().slots.size()) {
				Slot slot = screen.getScreenHandler().slots.get(slotIndex);
				ItemStack itemStack = slot.getStack();
				
				if (!itemStack.isEmpty()) {
					String itemName = itemStack.getName().getString();
					// Prüfe ob das Item Unicode-Indikatoren hat (Blueprint-Item)
					if (hasCraftingUnicodeIndicators(itemName)) {
						blueprintSlots.add(slotIndex);
					}
				}
			}
		}
		
		// Falls keine Blueprint-Items gefunden wurden, verwende alle Standard-Slots
		if (blueprintSlots.isEmpty()) {
			for (int slotIndex : blueprintSlotRanges) {
				if (slotIndex < screen.getScreenHandler().slots.size()) {
					blueprintSlots.add(slotIndex);
				}
			}
		}
		
		return blueprintSlots;
	}
	
	/**
	 * Heuristische Methode zur Unicode-Farbanalyse
	 * Da die direkte Style-Analyse nicht funktioniert, verwende alternative Methoden
	 */
	private static String analyzeUnicodeColorHeuristic(String textString) {
		try {
			// Methode 1: Prüfe auf spezielle Zeichenkombinationen die auf Farben hindeuten
			if (textString.contains("§")) {
				// Suche nach Minecraft-Farbcodes
				for (int i = 0; i < textString.length() - 1; i++) {
					if (textString.charAt(i) == '§') {
						char colorCode = textString.charAt(i + 1);
						switch (colorCode) {
							case '3': return "0x00FFFF (Türkis - Heuristik)"; // Türkis
							case 'b': return "0x5555FF (Blau - Heuristik)"; // Blau
							case '9': return "0x5555FF (Blau - Heuristik)"; // Blau
							case 'f': return "0xFFFFFF (Weiß - Heuristik)"; // Weiß
							case 'F': return "0xFFFFFF (Weiß - Heuristik)"; // Weiß
							case '7': return "0xAAAAAA (Grau - Heuristik)"; // Grau
							case '8': return "0x555555 (Dunkelgrau - Heuristik)"; // Dunkelgrau
						}
					}
				}
			}
			
			// Methode 2: Prüfe auf spezielle Unicode-Zeichen-Kombinationen
			// Einige Unicode-Zeichen haben standardmäßig bestimmte Farben
			char firstChar = textString.charAt(0);
			char lastChar = textString.charAt(textString.length() - 1);
			int firstCode = (int)firstChar;
			int lastCode = (int)lastChar;
			
			// Prüfe ob es sich um spezielle Unicode-Bereiche handelt
			if ((firstCode >= 13617 && firstCode <= 13668) && (lastCode >= 13617 && lastCode <= 13668)) {
				// Wenn keine spezifische Farbe gefunden wurde, verwende die rekursive Text-Analyse
				// Diese sollte bereits die korrekte Farbe gefunden haben
				return "null (keine heuristische Farbe gefunden)";
			}
			
			return "null (keine heuristische Farbe gefunden)";
		} catch (Exception e) {
			return "null (Fehler bei heuristischer Analyse: " + e.getMessage() + ")";
		}
	}
	
	/**
	 * Generiert eine gleichmäßige Regenbogen-Farbe basierend auf der Zeit
	 */
	private static int getRainbowColor() {
		long time = System.currentTimeMillis();
		float hue = (time % 2000) / 2000.0f; // 2 Sekunden Zyklus
		
		// Gleichmäßige Farbverteilung mit angepassten Sektoren
		float h = hue * 360.0f; // 0-360 Grad für vollständigen Farbkreis
		float s = 1.0f; // Volle Sättigung
		float v = 1.0f; // Volle Helligkeit
		
		// Kontinuierliche Farbinterpolation
		float c = v * s;
		float x = c * (1 - Math.abs((h / 60.0f) % 2 - 1));
		float m = v - c;
		
		float r, g, b;
		if (h < 60) {
			r = c; g = x; b = 0;
		} else if (h < 120) {
			r = x; g = c; b = 0;
		} else if (h < 180) {
			r = 0; g = c; b = x;
		} else if (h < 240) {
			r = 0; g = x; b = c;
		} else if (h < 300) {
			r = x; g = 0; b = c;
		} else {
			r = c; g = 0; b = x;
		}
		
		// Gamma-Korrektur für gleichmäßigere Farbwahrnehmung
		r = (float) Math.pow(r, 0.7);
		g = (float) Math.pow(g, 0.8); // Grün etwas stärker korrigiert
		b = (float) Math.pow(b, 0.7);
		
		return 0xFF000000 | 
			((int)((r + m) * 255) << 16) | 
			((int)((g + m) * 255) << 8) | 
			(int)((b + m) * 255);
	}
	
	/**
	 * Aktualisiert die Button-Position basierend auf dem Screen
	 */
	private static void updateButtonPosition(HandledScreen<?> screen, MinecraftClient client) {
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		
		// Standard-Position in der oberen rechten Ecke des Bildschirms
		int baseX = screenWidth - buttonWidth - 20;
		int baseY = 20;
		
		// Füge die konfigurierten Offsets hinzu
		buttonX = baseX + CCLiveUtilitiesConfig.HANDLER.instance().hideUncraftableButtonX;
		buttonY = baseY + CCLiveUtilitiesConfig.HANDLER.instance().hideUncraftableButtonY;
	}
	
	/**
	 * Aktualisiert die Position des Hide Wrong Class Buttons basierend auf der Bildschirmgröße
	 */
	private static void updateWrongClassButtonPosition(HandledScreen<?> screen, MinecraftClient client) {
		if (client.getWindow() == null) {
			return;
		}
		
		int screenWidth = client.getWindow().getScaledWidth();
		
		// Standard-Position in der oberen rechten Ecke des Bildschirms
		int baseX = screenWidth - buttonWidth - 20;
		int baseY = 20;
		
		// Füge die konfigurierten Offsets hinzu
		wrongClassButtonX = baseX + CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonX;
		wrongClassButtonY = baseY + CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonY;
	}
	
	/**
	 * Aktualisiert die Items in den Blueprint-Slots basierend auf dem Hide Uncraftable Status
	 */
	private static void updateBlueprintItems(HandledScreen<?> screen, MinecraftClient client) {
		if (!hideUncraftableActive) {
			restoreOriginalItems(screen);
			return;
		}
		
		// Finde die korrekten Blueprint-Slots automatisch
		List<Integer> blueprintSlots = findBlueprintSlots(screen);
		int[] targetSlots = blueprintSlots.stream().mapToInt(Integer::intValue).toArray();
		
		for (int slotIndex : targetSlots) {
			if (slotIndex < screen.getScreenHandler().slots.size()) {
				Slot slot = screen.getScreenHandler().slots.get(slotIndex);
				ItemStack itemStack = slot.getStack();
				
				if (!itemStack.isEmpty()) {
					// Speichere das originale Item falls noch nicht gespeichert
					if (!originalItems.containsKey(slotIndex)) {
						originalItems.put(slotIndex, itemStack.copy());
					}
					
					// Prüfe ob das Item Verzauberungen hat
					boolean hasEnchantmentsResult = hasEnchantments(itemStack);
					
					if (!hasEnchantmentsResult) {
						// Ersetze mit schwarzem Beton, aber behalte die ursprünglichen Tooltips
						ItemStack blackConcrete = new ItemStack(Items.BLACK_CONCRETE);
						
						// Kopiere die ursprünglichen Komponenten für Tooltips
						blackConcrete.set(DataComponentTypes.CUSTOM_NAME, itemStack.get(DataComponentTypes.CUSTOM_NAME));
						blackConcrete.set(DataComponentTypes.LORE, itemStack.get(DataComponentTypes.LORE));
						
					// Füge einen Hinweis zum Custom Name hinzu, dass das Item ausgeblendet wurde
					var customName = blackConcrete.get(DataComponentTypes.CUSTOM_NAME);
					if (customName != null) {
						// Füge den Hinweis zum bestehenden Namen hinzu
						String originalName = customName.getString();
						// Prüfe ob "[Ausgeblendet]" bereits vorhanden ist (mit oder ohne Formatierungscodes)
						if (!originalName.contains("[Ausgeblendet]")) {
							Text newName = Text.literal(originalName + " §7[Ausgeblendet]");
							blackConcrete.set(DataComponentTypes.CUSTOM_NAME, newName);
						}
					} else {
						// Erstelle einen neuen Custom Name mit Hinweis
						String originalName = itemStack.getName().getString();
						// Prüfe ob "[Ausgeblendet]" bereits vorhanden ist (mit oder ohne Formatierungscodes)
						if (!originalName.contains("[Ausgeblendet]")) {
							Text newName = Text.literal(originalName + " §7[Ausgeblendet]");
							blackConcrete.set(DataComponentTypes.CUSTOM_NAME, newName);
						}
					}
						
						slot.setStack(blackConcrete);
					}
				}
			}
		}
	}
	

	
	/**
	 * Stellt die originalen Items wieder her
	 */
	private static void restoreOriginalItems(HandledScreen<?> screen) {
		if (screen == null) {
			originalItems.clear();
			return;
		}
		
		// Erstelle eine Kopie der Map, um ConcurrentModificationException zu vermeiden
		Map<Integer, ItemStack> itemsToRestore = new HashMap<>(originalItems);
		
		for (Map.Entry<Integer, ItemStack> entry : itemsToRestore.entrySet()) {
			int slotIndex = entry.getKey();
			ItemStack originalItem = entry.getValue();
			
			if (slotIndex < screen.getScreenHandler().slots.size()) {
				Slot slot = screen.getScreenHandler().slots.get(slotIndex);
				// Prüfe ob der Slot noch den schwarzen Betonblock enthält
				ItemStack currentItem = slot.getStack();
				if (currentItem.getItem() == Items.BLACK_CONCRETE) {
					slot.setStack(originalItem);
				}
			}
		}
		originalItems.clear();
	}
	
	/**
	 * Prüft ob ein Item "Nicht für deine Klasse geeignet!" in der LORE hat (Farbe ist egal)
	 */
	private static boolean isWrongClass(ItemStack itemStack) {
		try {
			var loreComponent = itemStack.get(DataComponentTypes.LORE);
			if (loreComponent != null) {
				List<Text> lore = loreComponent.lines();
				for (Text loreText : lore) {
					String loreString = loreText.getString();
					// Prüfe ob der Text "Nicht für deine Klasse geeignet!" enthält (Farbe ist egal)
					if (loreString.contains("Nicht für deine Klasse geeignet!")) {
						return true; // Item ist für falsche Klasse
					}
				}
			}
		} catch (Exception e) {
			// Ignoriere Fehler
		}
		return false;
	}
	
	/**
	 * Aktualisiert die Items in den Blueprint-Slots basierend auf dem Hide Wrong Class Status
	 */
	private static void updateBlueprintItemsWrongClass(HandledScreen<?> screen, MinecraftClient client) {
		if (!hideWrongClassActive) {
			restoreOriginalItemsWrongClass(screen);
			return;
		}
		
		// Finde die korrekten Blueprint-Slots automatisch
		List<Integer> blueprintSlots = findBlueprintSlots(screen);
		int[] targetSlots = blueprintSlots.stream().mapToInt(Integer::intValue).toArray();
		
		for (int slotIndex : targetSlots) {
			if (slotIndex < screen.getScreenHandler().slots.size()) {
				Slot slot = screen.getScreenHandler().slots.get(slotIndex);
				ItemStack itemStack = slot.getStack();
				
				if (!itemStack.isEmpty()) {
					// Speichere das originale Item falls noch nicht gespeichert
					if (!originalItemsWrongClass.containsKey(slotIndex)) {
						originalItemsWrongClass.put(slotIndex, itemStack.copy());
					}
					
					// Prüfe ob das Item für die falsche Klasse ist
					boolean isWrongClassResult = isWrongClass(itemStack);
					
					if (isWrongClassResult) {
						// Ersetze mit schwarzem Beton, aber behalte die ursprünglichen Tooltips
						ItemStack blackConcrete = new ItemStack(Items.BLACK_CONCRETE);
						
						// Kopiere die ursprünglichen Komponenten für Tooltips
						blackConcrete.set(DataComponentTypes.CUSTOM_NAME, itemStack.get(DataComponentTypes.CUSTOM_NAME));
						blackConcrete.set(DataComponentTypes.LORE, itemStack.get(DataComponentTypes.LORE));
						
						// Füge einen Hinweis zum Custom Name hinzu, dass das Item ausgeblendet wurde
						var customName = blackConcrete.get(DataComponentTypes.CUSTOM_NAME);
						if (customName != null) {
							String originalName = customName.getString();
							if (!originalName.contains("[Ausgeblendet]")) {
								Text newName = Text.literal(originalName + " §7[Ausgeblendet]");
								blackConcrete.set(DataComponentTypes.CUSTOM_NAME, newName);
							}
						} else {
							String originalName = itemStack.getName().getString();
							if (!originalName.contains("[Ausgeblendet]")) {
								Text newName = Text.literal(originalName + " §7[Ausgeblendet]");
								blackConcrete.set(DataComponentTypes.CUSTOM_NAME, newName);
							}
						}
						
						slot.setStack(blackConcrete);
					}
				}
			}
		}
	}
	
	/**
	 * Stellt die originalen Items wieder her (für Hide Wrong Class)
	 */
	private static void restoreOriginalItemsWrongClass(HandledScreen<?> screen) {
		if (screen == null) {
			originalItemsWrongClass.clear();
			return;
		}
		
		// Erstelle eine Kopie der Map, um ConcurrentModificationException zu vermeiden
		Map<Integer, ItemStack> itemsToRestore = new HashMap<>(originalItemsWrongClass);
		
		for (Map.Entry<Integer, ItemStack> entry : itemsToRestore.entrySet()) {
			int slotIndex = entry.getKey();
			ItemStack originalItem = entry.getValue();
			
			if (slotIndex < screen.getScreenHandler().slots.size()) {
				Slot slot = screen.getScreenHandler().slots.get(slotIndex);
				// Prüfe ob der Slot noch den schwarzen Betonblock enthält
				ItemStack currentItem = slot.getStack();
				if (currentItem.getItem() == Items.BLACK_CONCRETE) {
					slot.setStack(originalItem);
				}
			}
		}
		originalItemsWrongClass.clear();
	}
	
	/**
	 * Prüft ob ein Item uncraftbar ist (sollte ausgeblendet werden)
	 * NEUE LOGIK: Items ohne Enchantment Glint Effekt werden ausgeblendet
	 */
	private static boolean hasEnchantments(ItemStack itemStack) {
		try {
			// NEUE LOGIK: Prüfe auf Enchantment Glint Effekt
			// Items ohne Glint Effekt werden ausgeblendet (uncraftbar)
			
			// Prüfe ob das Item einen Glint Effekt hat
			boolean hasGlint = itemStack.hasGlint();
			
			// true = nicht ausblenden (craftbar), false = ausblenden (uncraftbar)
			return hasGlint;
			
		} catch (Exception e) {
			// Ignoriere Fehler und behandle als nicht verzaubert
		}
		
		return false; // Standardmäßig nicht ausblenden (kein Glint = uncraftbar)
	}
	
	/**
	 * Rendert farbige Rahmen oder Hintergrund um Items (wird vom Mixin aufgerufen)
	 */
	public static void renderColoredFrames(DrawContext context, HandledScreen<?> screen, int screenX, int screenY) {
		if (!isInDisassembleChest) {
			return;
		}
		
		// Prüfe ob Rahmen sichtbar sein sollen (kann per Hotkey getoggelt werden)
		if (!framesVisible) {
			return;
		}

		net.felix.ItemDisplayMode displayMode = CCLiveUtilitiesConfig.HANDLER.instance().schmiedTrackerItemDisplayMode;

		// Zeichne farbige Rahmen oder Hintergrund um die Items
		for (Map.Entry<Integer, Integer> entry : slotColors.entrySet()) {
			int slotIndex = entry.getKey();
			int color = entry.getValue();
			
			// Die Farbe wird bereits in getSchmiedTypeColor() korrekt gesetzt
			// (inklusive Regenbogen-Effekt für Sternengeschmiedet wenn aktiviert)
			
			// Hole die Slot-Position direkt aus dem ScreenHandler
			if (slotIndex < screen.getScreenHandler().slots.size()) {
				Slot slot = screen.getScreenHandler().slots.get(slotIndex);
				
				// Berechne die absolute Position auf dem Bildschirm
				int slotX = screenX + slot.x;
				int slotY = screenY + slot.y;
				
				if (displayMode == net.felix.ItemDisplayMode.BACKGROUND) {
					// Zeichne halbtransparenten Hintergrund
					// Verwende die Rahmenfarbe, aber mit reduzierter Transparenz
					int backgroundColor = (color & 0x00FFFFFF) | 0x80000000; // 50% Transparenz für bessere Sichtbarkeit
					context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, backgroundColor);
				} else {
					// Zeichne 1 Pixel dicken Rahmen (Standard)
					context.fill(slotX - 1, slotY - 1, slotX + SLOT_SIZE + 1, slotY, color); // Oben (1 Pixel höher)
					context.fill(slotX - 1, slotY + SLOT_SIZE, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE + 1, color); // Unten
					context.fill(slotX - 1, slotY - 1, slotX, slotY + SLOT_SIZE + 1, color); // Links (erweitert um oberen Rand)
					context.fill(slotX + SLOT_SIZE, slotY - 1, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE + 1, color); // Rechts (erweitert um oberen Rand)
				}
			}
		}
	}
	
	/**
	 * Prüft ob wir im "Bauplan [Shop]" Menü sind
	 */
	private static boolean isInBlueprintShop(HandledScreen<?> screen) {
		if (screen == null) return false;
		String title = screen.getTitle().getString();
		String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "")
								   .replaceAll("[\\u3400-\\u4DBF]", "");
		return cleanTitle.contains("Bauplan [Shop]");
	}
	
	/**
	 * Rendert den Hide Uncraftable Button (wird vom Mixin aufgerufen)
	 */
	public static void renderHideUncraftableButton(DrawContext context, HandledScreen<?> screen) {
		if (!isInBlueprintInventory || !CCLiveUtilitiesConfig.HANDLER.instance().hideUncraftableEnabled) {
			return;
		}
		
		// Hide Uncraftable Button soll nicht im "Bauplan [Shop]" Menü angezeigt werden
		if (isInBlueprintShop(screen)) {
			return;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) return;
		
		// Get scale
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().hideUncraftableButtonScale;
		if (scale <= 0) scale = 1.0f;
		
		int unscaledWidth = buttonWidth;
		int unscaledHeight = buttonHeight;
		int scaledWidth = (int) (unscaledWidth * scale);
		int scaledHeight = (int) (unscaledHeight * scale);
		
		// Use Matrix transformations for scaling
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		
		// Translate to position and scale from there
		matrices.translate(buttonX, buttonY);
		matrices.scale(scale, scale);
		
		// Button-Hintergrund (scaled, relative to matrix)
		int backgroundColor = hideUncraftableActive ? 0xFF4B6A69 : 0xFF4B6A69; // Einheitliche Farbe #4B6A69
		context.fill(0, 0, unscaledWidth, unscaledHeight, backgroundColor);
		
		// Button-Rahmen mit verschiedenen Farben (2 Pixel dick, scaled)
		context.fill(0, 0, unscaledWidth, 2, 0xFF65857C); // Oben - #65857C
		context.fill(0, unscaledHeight - 2, unscaledWidth, unscaledHeight, 0xFF1D2F3B); // Unten - #1D2F3B
		context.fill(0, 0, 2, unscaledHeight, 0xFF314E52); // Links - #314E52
		context.fill(unscaledWidth - 2, 0, unscaledWidth, unscaledHeight, 0xFF314E52); // Rechts - #314E52
		
		// Button-Text (scaled, relative to matrix)
		String buttonText = hideUncraftableActive ? "Show All" : "Hide Uncraftable";
		int textColor = 0xFF404040; // Dunkelgrau
		int textX = (unscaledWidth - client.textRenderer.getWidth(buttonText)) / 2;
		int textY = (unscaledHeight - 8) / 2;
		context.drawText(client.textRenderer, buttonText, textX, textY, textColor, false);
		
		matrices.popMatrix();
	}
	
	/**
	 * Behandelt Mausklicks auf den Hide Uncraftable Button
	 */
	public static boolean handleButtonClick(double mouseX, double mouseY, int button) {
		if (!isInBlueprintInventory || !CCLiveUtilitiesConfig.HANDLER.instance().hideUncraftableEnabled) {
			return false;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.currentScreen instanceof HandledScreen<?> handledScreen) {
			// Hide Uncraftable Button soll nicht im "Bauplan [Shop]" Menü funktionieren
			if (isInBlueprintShop(handledScreen)) {
				return false;
			}
		}
		
		// Get scale for hitbox calculation
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().hideUncraftableButtonScale;
		if (scale <= 0) scale = 1.0f;
		int scaledWidth = (int) (buttonWidth * scale);
		int scaledHeight = (int) (buttonHeight * scale);
		
		if (button == 0 && mouseX >= buttonX && mouseX <= buttonX + scaledWidth &&
			mouseY >= buttonY && mouseY <= buttonY + scaledHeight) {
			
			hideUncraftableActive = !hideUncraftableActive;
			
			// Aktualisiere die Items basierend auf dem neuen Status
			if (client != null && client.currentScreen instanceof HandledScreen<?> handledScreen) {
				if (hideUncraftableActive) {
					updateBlueprintItems(handledScreen, client);
				} else {
					restoreOriginalItems(handledScreen);
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Rendert den Hide Wrong Class Button (wird vom Mixin aufgerufen)
	 */
	public static void renderHideWrongClassButton(DrawContext context, HandledScreen<?> screen) {
		if (!isInBlueprintInventory || !CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassEnabled) {
			return;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) return;
		
		// Get scale
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonScale;
		if (scale <= 0) scale = 1.0f;
		
		int unscaledWidth = buttonWidth;
		int unscaledHeight = buttonHeight;
		int scaledWidth = (int) (unscaledWidth * scale);
		int scaledHeight = (int) (unscaledHeight * scale);
		
		// Use Matrix transformations for scaling
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		
		// Translate to position and scale from there
		matrices.translate(wrongClassButtonX, wrongClassButtonY);
		matrices.scale(scale, scale);
		
		// Button-Hintergrund (scaled, relative to matrix)
		int backgroundColor = hideWrongClassActive ? 0xFF4B6A69 : 0xFF4B6A69; // Einheitliche Farbe #4B6A69
		context.fill(0, 0, unscaledWidth, unscaledHeight, backgroundColor);
		
		// Button-Rahmen mit verschiedenen Farben (2 Pixel dick, scaled)
		context.fill(0, 0, unscaledWidth, 2, 0xFF65857C); // Oben - #65857C
		context.fill(0, unscaledHeight - 2, unscaledWidth, unscaledHeight, 0xFF1D2F3B); // Unten - #1D2F3B
		context.fill(0, 0, 2, unscaledHeight, 0xFF314E52); // Links - #314E52
		context.fill(unscaledWidth - 2, 0, unscaledWidth, unscaledHeight, 0xFF314E52); // Rechts - #314E52
		
		// Button-Text (scaled, relative to matrix)
		String buttonText = hideWrongClassActive ? "Show All" : "Hide wrong class";
		int textColor = 0xFF404040; // Dunkelgrau
		int textX = (unscaledWidth - client.textRenderer.getWidth(buttonText)) / 2;
		int textY = (unscaledHeight - 8) / 2;
		context.drawText(client.textRenderer, buttonText, textX, textY, textColor, false);
		
		matrices.popMatrix();
	}
	
	/**
	 * Behandelt Mausklicks auf den Hide Wrong Class Button
	 */
	public static boolean handleWrongClassButtonClick(double mouseX, double mouseY, int button) {
		if (!isInBlueprintInventory || !CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassEnabled) {
			return false;
		}
		
		// Get scale for hitbox calculation
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().hideWrongClassButtonScale;
		if (scale <= 0) scale = 1.0f;
		int scaledWidth = (int) (buttonWidth * scale);
		int scaledHeight = (int) (buttonHeight * scale);
		
		if (button == 0 && mouseX >= wrongClassButtonX && mouseX <= wrongClassButtonX + scaledWidth &&
			mouseY >= wrongClassButtonY && mouseY <= wrongClassButtonY + scaledHeight) {
			
			hideWrongClassActive = !hideWrongClassActive;
			
			// Aktualisiere die Items basierend auf dem neuen Status
			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null && client.currentScreen instanceof HandledScreen<?> handledScreen) {
				if (hideWrongClassActive) {
					updateBlueprintItemsWrongClass(handledScreen, client);
				} else {
					restoreOriginalItemsWrongClass(handledScreen);
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Getter für den Button-Status
	 */
	public static boolean isHideUncraftableActive() {
		return hideUncraftableActive;
	}
	
	/**
	 * Getter für den Blueprint-Inventar-Status
	 */
	public static boolean isInBlueprintInventory() {
		return isInBlueprintInventory;
	}
	
	/**
	 * Handle key press directly (for use in mixins when screens are open)
	 * @param keyCode The key code (e.g., GLFW.GLFW_KEY_F7)
	 * @return true if the key was handled
	 */
	public static boolean handleKeyPress(int keyCode) {
		try {
			// Check if F7 is pressed (default key for toggling frames)
			// Note: This checks the default key, not the configured key binding
			// For full support of custom key bindings, we'd need to check the KeyBinding's bound key
			if (keyCode == GLFW.GLFW_KEY_F7) {
				framesVisible = !framesVisible;
				return true;
			}
		} catch (Exception e) {
			// Silent error handling
		}
		return false;
	}
	
	/**
	 * Debug-Methode um zu prüfen warum ein Item als verzaubert erkannt wird oder nicht
	 */
	public static String debugItemEnchantmentDetection(ItemStack itemStack) {
		StringBuilder debug = new StringBuilder();
		debug.append("Item: ").append(Registries.ITEM.getId(itemStack.getItem())).append("\n");
		debug.append("Verbesserte Unicode-Erkennung aktiviert: ").append(CCLiveUtilitiesConfig.HANDLER.instance().hideUncraftableUseImprovedDetection).append("\n");
		debug.append("Normale Unicode-Erkennung aktiviert: ").append(CCLiveUtilitiesConfig.HANDLER.instance().hideUncraftableUseUnicodeDetection).append("\n");
		
		try {
			// Prüfe auf Enchantment Glint (Glanz-Effekt)
			boolean hasGlint = itemStack.hasEnchantments();
			debug.append("hasEnchantments(): ").append(hasGlint).append("\n");
			
			// Prüfe auf spezielle Komponenten die auf Verzauberungen hindeuten
			var enchantments = itemStack.getEnchantments();
			boolean hasEnchantments = enchantments != null && !enchantments.isEmpty();
			debug.append("getEnchantments(): ").append(hasEnchantments).append("\n");
			
			// Prüfe auf Lore die Verzauberungen anzeigt
			var loreComponent = itemStack.get(DataComponentTypes.LORE);
			if (loreComponent != null) {
				debug.append("Lore gefunden:\n");
				for (Text line : loreComponent.lines()) {
					String lineText = line.getString();
					debug.append("  - ").append(lineText).append("\n");
				}
			} else {
				debug.append("Keine Lore gefunden\n");
			}
			
			// Prüfe auf Custom Name der auf Verzauberungen hindeutet
			var customName = itemStack.get(DataComponentTypes.CUSTOM_NAME);
			if (customName != null) {
				String nameText = customName.getString();
				debug.append("Custom Name: ").append(nameText).append("\n");
				
				// Unicode-Zeichen-Analyse
				if (nameText.length() > 2) {
					char firstChar = nameText.charAt(0);
					char lastChar = nameText.charAt(nameText.length() - 1);
					int firstCode = (int)firstChar;
					int lastCode = (int)lastChar;
					
					debug.append("Erstes Zeichen: '").append(firstChar).append("' (Code: ").append(firstCode).append(")\n");
					debug.append("Letztes Zeichen: '").append(lastChar).append("' (Code: ").append(lastCode).append(")\n");
					
					// Spezifische Prüfung für Crafting-Indikatoren
					boolean hasCraftingIndicator = hasCraftingUnicodeIndicators(nameText);
					debug.append("Hat Crafting-Indikator: ").append(hasCraftingIndicator).append("\n");
					
					// Spezifische Prüfung für uncraftbare Unicode-Codes
					boolean isUncraftableFirst = (firstCode == 13625); // 㔹
					boolean isUncraftableLast = (lastCode == 13617 || lastCode == 13620); // 㔴 oder 㔱
					
					debug.append("Ist uncraftbar (First): ").append(isUncraftableFirst).append("\n");
					debug.append("Ist uncraftbar (Last): ").append(isUncraftableLast).append("\n");
					
					// Allgemeine Unicode-Prüfung
					boolean isUnicodeFirst = firstChar > 127 || firstChar < 32;
					boolean isUnicodeLast = lastChar > 127 || lastChar < 32;
					debug.append("Erstes Zeichen ist Unicode: ").append(isUnicodeFirst).append("\n");
					debug.append("Letztes Zeichen ist Unicode: ").append(isUnicodeLast).append("\n");
					
					// Zusätzliche Debug-Info für Unicode-Codes
					debug.append("Erwartete Unicode-Codes für Crafting-Indikatoren:\n");
					debug.append("  - 13625 (㔹), 13624 (㔸) für erstes Zeichen\n");
					debug.append("  - 13617 (㔴), 13620 (㔱) für letztes Zeichen\n");
					debug.append("Gefundene Codes: ").append(firstCode).append(" und ").append(lastCode).append("\n");
					
					// Erklärung der verbesserten Erkennung
					debug.append("\nVERBESSERTE UNICODE-ERKENNUNG:\n");
					debug.append("  - Türkise Unicode-Zeichen 㔹㔸㔱 = craftbar (wird NICHT ausgeblendet)\n");
					debug.append("  - Weiße Unicode-Zeichen 㔹㔸㔱 = uncraftbar (wird ausgeblendet)\n");
					debug.append("  - Keine Unicode-Zeichen = wird nicht behandelt\n");
					
					// Farberkennung
					var formatting = customName.getStyle();
					if (formatting != null && formatting.getColor() != null) {
						int colorRgb = formatting.getColor().getRgb();
						debug.append("Textfarbe: 0x").append(Integer.toHexString(colorRgb).toUpperCase()).append("\n");
						
						boolean isWhiteColor = isUnicodeWhite(formatting);
						boolean isCyanColor = isUnicodeCyan(formatting);
						
						debug.append("Ist weiße Farbe: ").append(isWhiteColor).append("\n");
						debug.append("Ist türkise Farbe: ").append(isCyanColor).append("\n");
						
						if (hasCraftingIndicator) {
							if (isWhiteColor) {
								debug.append("*** UNCRAFTABLE ERKANNT - Weiße Unicode-Zeichen, Item wird ausgeblendet ***\n");
							} else if (isCyanColor) {
								debug.append("*** CRAFTABLE ERKANNT - Türkise Unicode-Zeichen, Item bleibt sichtbar ***\n");
							} else {
								debug.append("*** UNBEKANNTE FARBE - Wird als uncraftbar behandelt, Item wird ausgeblendet ***\n");
							}
						} else {
							debug.append("*** KEINE CRAFTING-INDIKATOREN - Item wird nicht behandelt ***\n");
						}
					} else {
						debug.append("Keine Farbe gefunden\n");
						if (hasCraftingIndicator) {
							debug.append("*** UNCRAFTABLE ERKANNT - Keine Farbe = weiß, Item wird ausgeblendet ***\n");
						} else {
							debug.append("*** KEINE CRAFTING-INDIKATOREN - Item wird nicht behandelt ***\n");
						}
					}
				}
			} else {
				debug.append("Kein Custom Name\n");
			}
			
			// Prüfe auf spezielle Komponenten
			var components = itemStack.getComponents();
			if (components != null) {
				debug.append("Komponenten gefunden:\n");
				for (var component : components) {
					debug.append("  - ").append(component.toString()).append("\n");
				}
			} else {
				debug.append("Keine speziellen Komponenten\n");
			}
			
		} catch (Exception e) {
			debug.append("Fehler beim Debuggen: ").append(e.getMessage()).append("\n");
		}
		
		return debug.toString();
	}
} 
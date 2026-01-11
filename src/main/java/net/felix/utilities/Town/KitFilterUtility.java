package net.felix.utilities.Town;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.felix.CCLiveUtilitiesConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KitFilterUtility {
	
	private static boolean isInitialized = false;
	
	// Kit-Typen
	public enum KitType {
		MÜNZ_KIT("Münz-Kit"),
		SCHADEN_KIT("Schaden-Kit"),
		RESSOURCEN_KIT("Ressourcen-Kit"),
		HERSTELLUNGS_KIT("Herstellungs-Kit"),
		TANK_KIT("Tank-Kit");
		
		private final String displayName;
		
		KitType(String displayName) {
			this.displayName = displayName;
		}
		
		public String getDisplayName() {
			return displayName;
		}
	}
	
	// Gespeicherte Kit-Auswahlen für jeden Button (Kit 1, Kit 2, Kit 3)
	private static Map<Integer, KitSelection> selectedKits = new HashMap<>();
	
	// Aktive Filter-Status für jeden Button (true = Filter ist aktiv)
	private static Map<Integer, Boolean> activeFilters = new HashMap<>();
	
	// Button-Konfiguration
	private static final int BUTTON_COUNT = 3;
	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 20;
	
	// Button-Positionen (werden dynamisch berechnet)
	private static Map<Integer, ButtonPosition> buttonPositions = new HashMap<>();
	
	// Aktuell geöffnetes Inventar für Kit-Auswahl
	private static int currentButtonIndex = -1; // -1 = kein Button ausgewählt
	
	// Status-Tracking
	private static boolean wasInRelevantInventory = false;
	
	// Flag für verzögertes Öffnen des KitViewScreen (nach Chat-Schließung)
	private static boolean pendingKitViewScreen = false;
	
	// Originale Items speichern (für Wiederherstellung)
	private static Map<Integer, ItemStack> originalItems = new HashMap<>(); // slotIndex -> original ItemStack
	private static Map<Integer, ItemStack> lastKnownItems = new HashMap<>(); // slotIndex -> letztes bekanntes ItemStack (für Änderungserkennung)
	
	// Item-Informationen für jedes Kit und jede Stufe
	// Map: KitType -> Level -> Set von Item-Informationen
	private static Map<KitType, Map<Integer, Set<ItemInfo>>> kitItemInfos = new HashMap<>();
	
	/**
	 * Repräsentiert Informationen über ein Item in einem Kit
	 */
	public static class ItemInfo {
		public final String name;
		public final String ebene; // Ebene wo das Item gefunden wird (optional)
		public final String info; // Zusätzliche Informationen (optional)
		public final int nameColor; // Farbe für den Item-Namen (optional, Standard: 0xFFFFFFFF = weiß)
		public final String nameColorString; // Farb-String (z.B. "Epic", "Legendary") für die Farbe
		public final String itemType; // Item-Typ (z.B. "Helm", "Brust", "Schuhe") für Anzeige vor dem Namen
		public final String modifier; // Modifier-String (z.B. "[Andere], [Andere], [Schaden]") (optional)
		
		public ItemInfo(String name) {
			this.name = name;
			this.ebene = null;
			this.info = null;
			this.nameColor = 0xFFFFFFFF; // Standard: weiß
			this.nameColorString = null;
			this.itemType = null;
			this.modifier = null;
		}
		
		public ItemInfo(String name, String ebene, String info) {
			this.name = name;
			this.ebene = ebene;
			this.info = info;
			this.nameColor = 0xFFFFFFFF; // Standard: weiß
			this.nameColorString = null;
			this.itemType = null;
			this.modifier = null;
		}
		
		public ItemInfo(String name, String ebene, String info, int nameColor) {
			this.name = name;
			this.ebene = ebene;
			this.info = info;
			this.nameColor = nameColor;
			this.nameColorString = null;
			this.itemType = null;
			this.modifier = null;
		}
		
		public ItemInfo(String name, String ebene, String info, int nameColor, String nameColorString) {
			this.name = name;
			this.ebene = ebene;
			this.info = info;
			this.nameColor = nameColor;
			this.nameColorString = nameColorString;
			this.itemType = null;
			this.modifier = null;
		}
		
		public ItemInfo(String name, String ebene, String info, int nameColor, String nameColorString, String itemType) {
			this.name = name;
			this.ebene = ebene;
			this.info = info;
			this.nameColor = nameColor;
			this.nameColorString = nameColorString;
			this.itemType = itemType;
			this.modifier = null;
		}
		
		public ItemInfo(String name, String ebene, String info, int nameColor, String nameColorString, String itemType, String modifier) {
			this.name = name;
			this.ebene = ebene;
			this.info = info;
			this.nameColor = nameColor;
			this.nameColorString = nameColorString;
			this.itemType = itemType;
			this.modifier = modifier;
		}
		
		/**
		 * Konvertiert einen Farb-String (z.B. "Epic", "Legendary") zu einem Farbcode
		 */
		public static int parseColorString(String colorString) {
			if (colorString == null || colorString.isEmpty()) {
				return 0xFFFFFFFF; // Standard: weiß
			}
			
			switch (colorString.toLowerCase()) {
				case "epic":
					return 0xFFA134EB; // #A134EB
				case "legendary":
					return 0xFFFC7E00; // #FC7E00
				default:
					return 0xFFFFFFFF; // Standard: weiß
			}
		}
		
		/**
		 * Konvertiert einen Modifier-Namen zu einem Farbcode
		 */
		public static int parseModifierColor(String modifierName) {
			if (modifierName == null || modifierName.isEmpty()) {
				return 0xFFFFFFFF; // Standard: weiß
			}
			
			switch (modifierName.toLowerCase()) {
				case "attribut":
				case "attribute":
					return 0xFF00A8A8; // #00A8A8
				case "verteidigung":
					return 0xFFFCA800; // #FCA800
				case "fähigkeiten":
					return 0xFF5454FC; // #5454FC
				case "schaden":
					return 0xFFFC5454; // #FC5454
				case "andere":
					return 0xFFFC54FC; // #FC54FC
				case "herstellung":
					return 0xFFFCFC54; // #FCFC54
				default:
					return 0xFFFFFFFF; // Standard: weiß
			}
		}
		
		/**
		 * Gibt den Anzeige-Text für das Item zurück (mit optionalen Infos)
		 */
		public String getDisplayText() {
			StringBuilder result = new StringBuilder(name);
			
			// Füge Ebene direkt hinter dem Namen hinzu
			if (ebene != null && !ebene.isEmpty()) {
				result.append(" ").append(ebene);
			}
			
			// Füge zusätzliche Info in Klammern hinzu (falls vorhanden)
			if (info != null && !info.isEmpty()) {
				result.append(" (").append(info).append(")");
			}
			
			return result.toString();
		}
	}
	
	// Pfad zur JSON-Konfigurationsdatei
	private static final String KITS_CONFIG_FILE = "assets/cclive-utilities/Kits.json";
	
	// Slots die gefiltert werden sollen (Blueprint-Slots)
	private static final int[] FILTER_SLOTS = {
		10, 11, 12, 13, 14, 15, 16,  // Erste Reihe
		19, 20, 21, 22, 23, 24, 25,  // Zweite Reihe
		28, 29, 30, 31, 32, 33, 34,  // Dritte Reihe
		37, 38, 39, 40, 41, 42, 43   // Vierte Reihe
	};
	
	/**
	 * Repräsentiert eine Kit-Auswahl (Kit-Typ + Stufe)
	 */
	public static class KitSelection {
		public final KitType kitType;
		public final int level; // 1-7
		
		public KitSelection(KitType kitType, int level) {
			this.kitType = kitType;
			this.level = Math.max(1, Math.min(7, level)); // Begrenze auf 1-7
		}
		
		public String getDisplayName() {
			return kitType.getDisplayName() + " Stufe " + level;
		}
	}
	
	/**
	 * Repräsentiert die Position eines Buttons
	 */
	private static class ButtonPosition {
		public int x;
		public int y;
		
		public ButtonPosition(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
	public static void initialize() {
		if (isInitialized) {
			return;
		}
		
		try {
			// Lade Kit-Item-Namen aus JSON-Datei
			loadKitItemNames();
			
			// Lade gespeicherte Kit-Auswahlen aus der Config
			loadSavedKitSelections();
			
			// Register commands
			registerCommands();
			
			// Client-seitige Events registrieren
			ClientTickEvents.END_CLIENT_TICK.register(KitFilterUtility::onClientTick);
			
			isInitialized = true;
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	/**
	 * Registriert Commands für Kit-Filter
	 */
	private static void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("cclive")
				.then(ClientCommandManager.literal("kits")
					.executes(context -> {
						// Setze Flag, um das Screen im nächsten Tick zu öffnen (nach Chat-Schließung)
						pendingKitViewScreen = true;
						return 1;
					})
				)
			);
		});
	}
	
	/**
	 * Lädt die gespeicherten Kit-Auswahlen aus der Config
	 */
	private static void loadSavedKitSelections() {
		try {
			CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
			
			// Button 1
			if (config.kitFilterButton1KitType != null && !config.kitFilterButton1KitType.isEmpty()) {
				KitType kitType = getKitTypeFromString(config.kitFilterButton1KitType);
				if (kitType != null) {
					int level = Math.max(1, Math.min(7, config.kitFilterButton1Level));
					selectedKits.put(0, new KitSelection(kitType, level));
				}
			}
			
			// Button 2
			if (config.kitFilterButton2KitType != null && !config.kitFilterButton2KitType.isEmpty()) {
				KitType kitType = getKitTypeFromString(config.kitFilterButton2KitType);
				if (kitType != null) {
					int level = Math.max(1, Math.min(7, config.kitFilterButton2Level));
					selectedKits.put(1, new KitSelection(kitType, level));
				}
			}
			
			// Button 3
			if (config.kitFilterButton3KitType != null && !config.kitFilterButton3KitType.isEmpty()) {
				KitType kitType = getKitTypeFromString(config.kitFilterButton3KitType);
				if (kitType != null) {
					int level = Math.max(1, Math.min(7, config.kitFilterButton3Level));
					selectedKits.put(2, new KitSelection(kitType, level));
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	/**
	 * Konvertiert einen String zu einem KitType
	 */
	private static KitType getKitTypeFromString(String kitTypeString) {
		try {
			return KitType.valueOf(kitTypeString);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	/**
	 * Konvertiert einen KitType zu einem String
	 */
	private static String getStringFromKitType(KitType kitType) {
		return kitType != null ? kitType.name() : "";
	}
	
	/**
	 * Speichert die Kit-Auswahl in der Config
	 */
	private static void saveKitSelectionToConfig(int buttonIndex, KitType kitType, int level) {
		try {
			CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
			
			switch (buttonIndex) {
				case 0:
					config.kitFilterButton1KitType = getStringFromKitType(kitType);
					config.kitFilterButton1Level = level;
					break;
				case 1:
					config.kitFilterButton2KitType = getStringFromKitType(kitType);
					config.kitFilterButton2Level = level;
					break;
				case 2:
					config.kitFilterButton3KitType = getStringFromKitType(kitType);
					config.kitFilterButton3Level = level;
					break;
			}
			
			// Speichere die Config
			CCLiveUtilitiesConfig.HANDLER.save();
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	/**
	 * Lädt die Kit-Item-Namen aus der JSON-Konfigurationsdatei
	 */
	private static void loadKitItemNames() {
		try {
			// Lade aus Mod-Ressourcen
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(KITS_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Kits config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					
					// Durchlaufe alle Kit-Typen
					for (String kitKey : json.keySet()) {
						// Konvertiere String zu KitType
						KitType kitType = getKitTypeFromKey(kitKey);
						if (kitType == null) {
							continue;
						}
						
						JsonObject kitData = json.getAsJsonObject(kitKey);
						
						// Durchlaufe alle Stufen (1-7)
						for (int level = 1; level <= 7; level++) {
							String levelKey = String.valueOf(level);
							if (kitData.has(levelKey)) {
								JsonArray itemsArray = kitData.getAsJsonArray(levelKey);
								Set<ItemInfo> itemInfos = new HashSet<>();
								
								// Füge alle Item-Informationen hinzu
								ItemInfo lastItemInfo = null; // Speichere das letzte Item für Kombination
								for (var element : itemsArray) {
									if (element.isJsonObject()) {
										// Neues Format: Objekt mit zusätzlichen Informationen
										com.google.gson.JsonObject itemObj = element.getAsJsonObject();
										String itemName = itemObj.has("name") ? itemObj.get("name").getAsString() : "";
										
										if (!itemName.isEmpty()) {
											// Vollständiges Objekt mit Name
											String ebene = itemObj.has("floor") ? itemObj.get("floor").getAsString() : null;
											String info = itemObj.has("info") ? itemObj.get("info").getAsString() : null;
											String nameColorStr = itemObj.has("rarity") ? itemObj.get("rarity").getAsString() : null;
											String itemType = itemObj.has("item_type") ? itemObj.get("item_type").getAsString() : null;
											String modifier = itemObj.has("modifier") ? itemObj.get("modifier").getAsString() : null;
											int nameColor = ItemInfo.parseColorString(nameColorStr);
											lastItemInfo = new ItemInfo(itemName, ebene, info, nameColor, nameColorStr, itemType, modifier);
											itemInfos.add(lastItemInfo);
										} else if (lastItemInfo != null) {
											// Objekt ohne Name: Füge Ebene/Info/Farbe/Item-Typ/Modifier zum vorherigen Item hinzu
											String ebene = itemObj.has("floor") ? itemObj.get("floor").getAsString() : null;
											String info = itemObj.has("info") ? itemObj.get("info").getAsString() : null;
											String nameColorStr = itemObj.has("rarity") ? itemObj.get("rarity").getAsString() : null;
											String itemType = itemObj.has("item_type") ? itemObj.get("item_type").getAsString() : null;
											String modifier = itemObj.has("modifier") ? itemObj.get("modifier").getAsString() : null;
											
											// Erstelle neues ItemInfo mit kombinierten Informationen
											String combinedEbene = (ebene != null && !ebene.isEmpty()) ? ebene : lastItemInfo.ebene;
											String combinedInfo = (info != null && !info.isEmpty()) ? info : lastItemInfo.info;
											String combinedColorStr = (nameColorStr != null && !nameColorStr.isEmpty()) 
												? nameColorStr 
												: lastItemInfo.nameColorString;
											String combinedItemType = (itemType != null && !itemType.isEmpty())
												? itemType
												: lastItemInfo.itemType;
											String combinedModifier = (modifier != null && !modifier.isEmpty())
												? modifier
												: lastItemInfo.modifier;
											int combinedColor = (combinedColorStr != null && !combinedColorStr.isEmpty())
												? ItemInfo.parseColorString(combinedColorStr)
												: lastItemInfo.nameColor;
											ItemInfo combinedItemInfo = new ItemInfo(lastItemInfo.name, combinedEbene, combinedInfo, combinedColor, combinedColorStr, combinedItemType, combinedModifier);
											
											// Entferne das alte und füge das neue hinzu
											itemInfos.remove(lastItemInfo);
											itemInfos.add(combinedItemInfo);
											lastItemInfo = combinedItemInfo;
										}
									} else {
										// Altes Format: Nur String (rückwärtskompatibel)
										String itemName = element.getAsString();
										if (!itemName.isEmpty()) {
											lastItemInfo = new ItemInfo(itemName);
											itemInfos.add(lastItemInfo);
										}
									}
								}
								
								// Speichere die Item-Informationen
								setKitItemInfos(kitType, level, itemInfos);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	/**
	 * Konvertiert einen String-Key zu einem KitType
	 */
	private static KitType getKitTypeFromKey(String key) {
		switch (key.toLowerCase()) {
			case "münz_kit":
			case "muenz_kit":
				return KitType.MÜNZ_KIT;
			case "schaden_kit":
				return KitType.SCHADEN_KIT;
			case "ressourcen_kit":
				return KitType.RESSOURCEN_KIT;
			case "herstellungs_kit":
				return KitType.HERSTELLUNGS_KIT;
			case "tank_kit":
				return KitType.TANK_KIT;
			default:
				return null;
		}
	}
	
	private static void onClientTick(MinecraftClient client) {
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod) {
			return;
		}
		
		// Prüfe ob KitViewScreen geöffnet werden soll (nach Chat-Schließung)
		if (pendingKitViewScreen) {
			// Prüfe ob Chat geschlossen wurde (kein ChatScreen mehr)
			if (client.currentScreen == null || !(client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) {
				// Chat ist geschlossen, öffne das Screen
				pendingKitViewScreen = false;
				client.setScreen(new KitViewScreen());
				return; // Früh zurückkehren, damit andere Logik nicht ausgeführt wird
			}
		}
		
		if (client.player == null || client.currentScreen == null) {
			// Wenn wir das Inventar verlassen haben, stelle alle Items wieder her und setze Filter zurück
			if (wasInRelevantInventory) {
				restoreOriginalItems(null);
				originalItems.clear();
				lastKnownItems.clear();
				// Setze alle Filter-Toggles zurück
				activeFilters.clear();
				wasInRelevantInventory = false;
			}
			return;
		}
		
		// Aktualisiere Button-Positionen und Filter-Logik wenn in einem relevanten Inventar
		if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
			if (isRelevantInventory(handledScreen)) {
				// Prüfe ob Kit Filter Buttons aktiviert sind
				if (!CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButtonsEnabled) {
					// Wenn Buttons deaktiviert sind, stelle alle Items wieder her und setze Filter zurück
					if (wasInRelevantInventory) {
						restoreOriginalItems(handledScreen);
						originalItems.clear();
						lastKnownItems.clear();
						activeFilters.clear();
						wasInRelevantInventory = false;
					}
					return;
				}
				
				updateButtonPositions(handledScreen, client);
				
				// Beim Betreten eines relevanten Inventars: Speichere originale Items und wende Filter sofort an
				if (!wasInRelevantInventory) {
					// Beim ersten Betreten: Stelle sicher, dass alle originalen Items gespeichert werden
					originalItems.clear();
					lastKnownItems.clear();
					// Speichere alle originalen Items zuerst
					saveOriginalItems(handledScreen);
					// Speichere auch als letzte bekannte Items
					for (int slotIndex : FILTER_SLOTS) {
						if (slotIndex < handledScreen.getScreenHandler().slots.size()) {
							Slot slot = handledScreen.getScreenHandler().slots.get(slotIndex);
							ItemStack itemStack = slot.getStack();
							if (!itemStack.isEmpty() && itemStack.getItem() != Items.BLACK_CONCRETE) {
								lastKnownItems.put(slotIndex, itemStack.copy());
							}
						}
					}
				}
				
				wasInRelevantInventory = true;
				
				// Prüfe ob sich das Inventar geändert hat
				if (hasInventoryChanged(handledScreen)) {
					// Inventar hat sich geändert - aktualisiere originale Items und wende Filter neu an
					updateOriginalItemsAfterChange(handledScreen);
					lastKnownItems.clear();
					// Speichere aktuelle Items als letzte bekannte Items
					for (int slotIndex : FILTER_SLOTS) {
						if (slotIndex < handledScreen.getScreenHandler().slots.size()) {
							Slot slot = handledScreen.getScreenHandler().slots.get(slotIndex);
							ItemStack itemStack = slot.getStack();
							if (!itemStack.isEmpty() && itemStack.getItem() != Items.BLACK_CONCRETE) {
								lastKnownItems.put(slotIndex, itemStack.copy());
							}
						}
					}
				} else {
					// Speichere aktuelle Items als letzte bekannte Items (für nächsten Vergleich)
					for (int slotIndex : FILTER_SLOTS) {
						if (slotIndex < handledScreen.getScreenHandler().slots.size()) {
							Slot slot = handledScreen.getScreenHandler().slots.get(slotIndex);
							ItemStack itemStack = slot.getStack();
							if (!itemStack.isEmpty() && itemStack.getItem() != Items.BLACK_CONCRETE) {
								lastKnownItems.put(slotIndex, itemStack.copy());
							} else {
								lastKnownItems.remove(slotIndex);
							}
						}
					}
				}
				
				// Wende Filter sofort an (wird bei jedem Tick aufgerufen, um Änderungen sofort zu reflektieren)
				updateFilteredItems(handledScreen, client);
			} else {
				// Prüfe ob wir gerade ein relevantes Inventar verlassen haben
				if (wasInRelevantInventory) {
					// Stelle alle Items wieder her
					restoreOriginalItems(handledScreen);
					originalItems.clear();
					lastKnownItems.clear();
					// Setze alle Filter-Toggles zurück
					activeFilters.clear();
					wasInRelevantInventory = false;
				}
			}
		} else {
			// Prüfe ob der aktuelle Screen ein KitSelectionScreen ist
			// In diesem Fall haben wir das Inventar nicht verlassen, sondern nur ein Menü geöffnet
			if (client.currentScreen instanceof KitSelectionScreen) {
				// Kit-Auswahl-Menü ist offen - Filter bleiben aktiv
				// Keine Aktion erforderlich
				return;
			}
			
			// Prüfe ob wir gerade ein relevantes Inventar verlassen haben
			if (wasInRelevantInventory) {
				restoreOriginalItems(null);
				originalItems.clear();
				// Setze alle Filter-Toggles zurück
				activeFilters.clear();
				wasInRelevantInventory = false;
			}
		}
	}
	
	/**
	 * Prüft ob das aktuelle Inventar relevant für Kit-Filter ist
	 * Kit-Filter werden nur in "Baupläne [Rüstung]" und "Bauplan [Shop]" angezeigt
	 */
	private static boolean isRelevantInventory(HandledScreen<?> screen) {
		String title = screen.getTitle().getString();
		// Entferne Minecraft-Formatierungs-Codes und Unicode-Zeichen für Vergleich
		String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "")
								 .replaceAll("[\\u3400-\\u4DBF]", "");
		
		// Prüfe auf die spezifischen Inventare
		return cleanTitle.contains("Baupläne [Rüstung]") ||
			   cleanTitle.contains("Bauplan [Shop]") ||
			   cleanTitle.contains("Favorisierte [Rüstungsbaupläne]"); 
	}
	
	/**
	 * Aktualisiert die Button-Positionen basierend auf dem Screen
	 */
	private static void updateButtonPositions(HandledScreen<?> screen, MinecraftClient client) {
		int screenWidth = client.getWindow().getScaledWidth();
		
		// Positioniere Buttons basierend auf gespeicherten Positionen aus der Config
		// Verwende die gleiche Logik wie in den DraggableOverlay-Klassen
		int baseX = screenWidth - BUTTON_WIDTH - 20;
		
		// Button 1
		int button1X = baseX + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton1X;
		int button1Y = 50 + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton1Y;
		buttonPositions.put(0, new ButtonPosition(button1X, button1Y));
		
		// Button 2
		int button2X = baseX + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2X;
		int button2Y = 75 + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2Y;
		buttonPositions.put(1, new ButtonPosition(button2X, button2Y));
		
		// Button 3
		int button3X = baseX + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton3X;
		int button3Y = 100 + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton3Y;
		buttonPositions.put(2, new ButtonPosition(button3X, button3Y));
	}
	
	/**
	 * Rendert die Kit-Filter-Buttons
	 */
	public static void renderKitFilterButtons(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY) {
		if (!isRelevantInventory(screen)) {
			return;
		}
		
		// Prüfe ob Kit Filter Buttons aktiviert sind
		if (!CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButtonsEnabled) {
			return;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) return;
		
		// Berechne Positionen direkt aus der Config (wie beim Hide Uncraftable Button)
		// Das stellt sicher, dass Änderungen sofort sichtbar sind
		int screenWidth = client.getWindow().getScaledWidth();
		int baseX = screenWidth - BUTTON_WIDTH - 20;
		
		Integer hoveredButtonIndex = null;
		
		// Rendere jeden Button
		for (int i = 0; i < BUTTON_COUNT; i++) {
			// Berechne Position direkt aus Config
			int buttonX, buttonY;
			switch (i) {
				case 0:
					buttonX = baseX + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton1X;
					buttonY = 50 + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton1Y;
					break;
				case 1:
					buttonX = baseX + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2X;
					buttonY = 75 + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2Y;
					break;
				case 2:
					buttonX = baseX + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton3X;
					buttonY = 100 + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton3Y;
					break;
				default:
					continue;
			}
			
			ButtonPosition pos = new ButtonPosition(buttonX, buttonY);
			
			// Prüfe ob Maus über diesem Button ist
			if (mouseX >= pos.x && mouseX <= pos.x + BUTTON_WIDTH &&
				mouseY >= pos.y && mouseY <= pos.y + BUTTON_HEIGHT) {
				hoveredButtonIndex = i;
			}
			
			// Button-Hintergrund (andere Farbe wenn Filter aktiv ist)
			boolean isActive = activeFilters.getOrDefault(i, false) && selectedKits.containsKey(i);
			int backgroundColor = isActive ? 0xFF5A8A7A : 0xFF4B6A69; // Heller wenn aktiv
			context.fill(pos.x, pos.y, pos.x + BUTTON_WIDTH, pos.y + BUTTON_HEIGHT, backgroundColor);
			
			// Button-Rahmen
			context.fill(pos.x, pos.y, pos.x + BUTTON_WIDTH, pos.y + 2, 0xFF65857C); // Oben
			context.fill(pos.x, pos.y + BUTTON_HEIGHT - 2, pos.x + BUTTON_WIDTH, pos.y + BUTTON_HEIGHT, 0xFF1D2F3B); // Unten
			context.fill(pos.x, pos.y, pos.x + 2, pos.y + BUTTON_HEIGHT, 0xFF314E52); // Links
			context.fill(pos.x + BUTTON_WIDTH - 2, pos.y, pos.x + BUTTON_WIDTH, pos.y + BUTTON_HEIGHT, 0xFF314E52); // Rechts
			
			// Button-Text
			String buttonText = getButtonText(i);
			int textColor = 0xFF404040; // Dunkelgrau
			int textX = pos.x + (BUTTON_WIDTH - client.textRenderer.getWidth(buttonText)) / 2;
			int textY = pos.y + (BUTTON_HEIGHT - 8) / 2;
			context.drawText(client.textRenderer, buttonText, textX, textY, textColor, false);
		}
		
		// Rendere Tooltip wenn über einem Button gehovered wird
		if (hoveredButtonIndex != null) {
			renderButtonTooltip(context, mouseX, mouseY, client);
		}
	}
	
	/**
	 * Rendert einen Tooltip für die Kit-Filter-Buttons
	 */
	private static void renderButtonTooltip(DrawContext context, int mouseX, int mouseY, MinecraftClient client) {
		String line1 = "Linksklick : Filtern";
		String line2 = "Rechtsklick : Bearbeiten";
		
		int line1Width = client.textRenderer.getWidth(line1);
		int line2Width = client.textRenderer.getWidth(line2);
		int maxWidth = Math.max(line1Width, line2Width);
		int textHeight = client.textRenderer.fontHeight;
		int padding = 4;
		int lineSpacing = 2;
		
		int tooltipX = mouseX + 10;
		int tooltipY = mouseY - 20;
		
		// Stelle sicher, dass der Tooltip nicht außerhalb des Bildschirms ist
		int screenWidth = client.getWindow().getScaledWidth();
		if (tooltipX + maxWidth + padding * 2 > screenWidth) {
			tooltipX = mouseX - maxWidth - padding * 2 - 10;
		}
		if (tooltipY < 0) {
			tooltipY = mouseY + 10;
		}
		
		// Berechne Hintergrund-Dimensionen
		int totalHeight = (textHeight * 2) + (lineSpacing) + (padding * 2);
		int bgX1 = tooltipX - padding;
		int bgY1 = tooltipY - padding;
		int bgX2 = tooltipX + maxWidth + padding;
		int bgY2 = tooltipY + totalHeight - padding;
		
		// Tooltip-Hintergrund (halbtransparent schwarz)
		context.fill(bgX1, bgY1, bgX2, bgY2, 0xF0000000);
		
		// Tooltip-Rahmen (weiß)
		context.fill(bgX1, bgY1, bgX2, bgY1 + 1, 0xFFFFFFFF); // Oben
		context.fill(bgX1, bgY2 - 1, bgX2, bgY2, 0xFFFFFFFF); // Unten
		context.fill(bgX1, bgY1, bgX1 + 1, bgY2, 0xFFFFFFFF); // Links
		context.fill(bgX2 - 1, bgY1, bgX2, bgY2, 0xFFFFFFFF); // Rechts
		
		// Tooltip-Text (zwei Zeilen)
		context.drawText(client.textRenderer, line1, tooltipX, tooltipY, 0xFFFFFFFF, true);
		context.drawText(client.textRenderer, line2, tooltipX, tooltipY + textHeight + lineSpacing, 0xFFFFFFFF, true);
	}
	
	/**
	 * Gibt den Text für einen Button zurück
	 */
	private static String getButtonText(int buttonIndex) {
		KitSelection selection = selectedKits.get(buttonIndex);
		if (selection != null) {
			// Zeige Kit-Name und Stufe an
			return selection.kitType.getDisplayName() + " " + selection.level;
		}
		// Standard-Text
		return "Kit " + (buttonIndex + 1);
	}
	
	/**
	 * Behandelt Mausklicks auf die Kit-Filter-Buttons
	 * Linksklick: Toggle Filter
	 * Rechtsklick: Öffne Auswahlmenü
	 */
	public static boolean handleButtonClick(double mouseX, double mouseY, int button) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.currentScreen == null) return false;
		
		if (!(client.currentScreen instanceof HandledScreen<?> handledScreen)) return false;
		if (!isRelevantInventory(handledScreen)) return false;
		
		// Prüfe ob Kit Filter Buttons aktiviert sind
		if (!CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButtonsEnabled) {
			return false;
		}
		
		// Berechne Positionen direkt aus der Config (wie beim Rendern)
		int screenWidth = client.getWindow().getScaledWidth();
		int baseX = screenWidth - BUTTON_WIDTH - 20;
		
		// Prüfe ob ein Button geklickt wurde
		for (int i = 0; i < BUTTON_COUNT; i++) {
			// Berechne Position direkt aus Config
			int buttonX, buttonY;
			switch (i) {
				case 0:
					buttonX = baseX + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton1X;
					buttonY = 50 + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton1Y;
					break;
				case 1:
					buttonX = baseX + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2X;
					buttonY = 75 + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton2Y;
					break;
				case 2:
					buttonX = baseX + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton3X;
					buttonY = 100 + CCLiveUtilitiesConfig.HANDLER.instance().kitFilterButton3Y;
					break;
				default:
					continue;
			}
			
			if (mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH &&
				mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT) {
				
				if (button == 0) {
					// Linksklick: Toggle Filter
					toggleFilter(i, handledScreen, client);
					return true;
				} else if (button == 1) {
					// Rechtsklick: Öffne Auswahlmenü
					openKitSelectionScreen(i);
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Toggelt den Filter für einen Button
	 */
	private static void toggleFilter(int buttonIndex, HandledScreen<?> screen, MinecraftClient client) {
		// Prüfe ob ein Kit für diesen Button ausgewählt ist
		if (!selectedKits.containsKey(buttonIndex)) {
			// Kein Kit ausgewählt - öffne stattdessen das Auswahlmenü
			openKitSelectionScreen(buttonIndex);
			return;
		}
		
		// Toggle Filter-Status
		boolean currentStatus = activeFilters.getOrDefault(buttonIndex, false);
		activeFilters.put(buttonIndex, !currentStatus);
		
		// Stelle sicher, dass originale Items gespeichert sind
		saveOriginalItems(screen);
		
		// Aktualisiere die gefilterten Items sofort
		updateFilteredItems(screen, client);
	}
	
	/**
	 * Öffnet den Kit-Auswahl-Screen
	 */
	public static void openKitSelectionScreen(int buttonIndex) {
		currentButtonIndex = buttonIndex;
		
		// Öffne den Kit-Auswahl-Screen
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null) {
			client.setScreen(new KitSelectionScreen(buttonIndex));
		}
	}
	
	/**
	 * Setzt die Kit-Auswahl für einen Button
	 */
	public static void setKitSelection(int buttonIndex, KitType kitType, int level) {
		// Prüfe ob die Kit-Auswahl geändert wurde
		KitSelection oldSelection = selectedKits.get(buttonIndex);
		boolean selectionChanged = (oldSelection == null || 
			oldSelection.kitType != kitType || oldSelection.level != level);
		
		// Setze die neue Kit-Auswahl
		selectedKits.put(buttonIndex, new KitSelection(kitType, level));
		
		// Speichere die Auswahl in der Config
		saveKitSelectionToConfig(buttonIndex, kitType, level);
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.currentScreen instanceof HandledScreen<?> handledScreen) {
			if (isRelevantInventory(handledScreen)) {
				// Stelle zuerst alle Items wieder her (falls sie ausgeblendet waren)
				// Dies stellt sicher, dass wir mit einem sauberen Zustand starten
				restoreOriginalItems(handledScreen, false); // Nicht löschen, da wir sie noch brauchen
				
				// Wenn die Auswahl geändert wurde, aktualisiere die originalen Items
				if (selectionChanged) {
					// Aktualisiere die originalen Items, falls sich das Inventar geändert hat
					saveOriginalItems(handledScreen);
				} else {
					// Stelle sicher, dass originale Items gespeichert sind
					saveOriginalItems(handledScreen);
				}
				
				// Prüfe ob für das neue Kit Item-Namen definiert sind
				Set<String> itemNames = getKitItemNames(kitType, level);
				if (itemNames.isEmpty()) {
					// Keine Item-Namen definiert - deaktiviere nur diesen Filter
					// Die anderen aktiven Filter bleiben aktiv
					activeFilters.put(buttonIndex, false);
				} else {
					// Filter wird NICHT automatisch aktiviert - muss manuell durch Linksklick aktiviert werden
					// Behalte den aktuellen Filter-Status bei (oder setze auf false wenn noch nicht gesetzt)
					if (!activeFilters.containsKey(buttonIndex)) {
						activeFilters.put(buttonIndex, false);
					}
				}
				
				// Wende ALLE aktiven Filter zusammen an (inklusive der anderen aktiven Filter)
				// Dies stellt sicher, dass andere aktive Filter weiterhin funktionieren
				updateFilteredItems(handledScreen, client);
			}
		} else {
			// Wenn nicht im Inventar, setze den Filter-Status auf false (muss manuell aktiviert werden)
			Set<String> itemNames = getKitItemNames(kitType, level);
			if (itemNames.isEmpty()) {
				activeFilters.put(buttonIndex, false);
			} else {
				// Filter wird NICHT automatisch aktiviert - muss manuell durch Linksklick aktiviert werden
				if (!activeFilters.containsKey(buttonIndex)) {
					activeFilters.put(buttonIndex, false);
				}
			}
		}
	}
	
	/**
	 * Gibt zurück, ob ein Filter für einen Button aktiv ist
	 */
	public static boolean isFilterActive(int buttonIndex) {
		return activeFilters.getOrDefault(buttonIndex, false);
	}
	
	/**
	 * Gibt die aktuelle Kit-Auswahl für einen Button zurück
	 */
	public static KitSelection getKitSelection(int buttonIndex) {
		return selectedKits.get(buttonIndex);
	}
	
	/**
	 * Prüft, ob sich das Inventar geändert hat (Items hinzugefügt, entfernt oder geändert)
	 */
	private static boolean hasInventoryChanged(HandledScreen<?> screen) {
		if (screen == null || lastKnownItems.isEmpty()) {
			return false;
		}
		
		// Prüfe alle Filter-Slots
		for (int slotIndex : FILTER_SLOTS) {
			if (slotIndex >= screen.getScreenHandler().slots.size()) {
				continue;
			}
			
			Slot slot = screen.getScreenHandler().slots.get(slotIndex);
			ItemStack currentItem = slot.getStack();
			
			// Wenn es ein schwarzer Betonblock ist, verwende das originale Item für den Vergleich
			if (currentItem.getItem() == Items.BLACK_CONCRETE) {
				currentItem = originalItems.get(slotIndex);
				if (currentItem == null) {
					currentItem = ItemStack.EMPTY;
				}
			}
			
			ItemStack lastKnownItem = lastKnownItems.get(slotIndex);
			
			if (currentItem == null || currentItem.isEmpty()) {
				// Item wurde entfernt
				if (lastKnownItem != null && !lastKnownItem.isEmpty()) {
					return true;
				}
			} else {
				// Item ist vorhanden
				if (lastKnownItem == null || lastKnownItem.isEmpty()) {
					// Neues Item wurde hinzugefügt
					return true;
				} else {
					// Prüfe ob sich das Item geändert hat (Name oder andere Eigenschaften)
					if (!areItemsEqual(currentItem, lastKnownItem)) {
						return true;
					}
				}
			}
		}
		
		// Prüfe auch, ob Items entfernt wurden (in lastKnownItems aber nicht mehr im Inventar)
		for (Map.Entry<Integer, ItemStack> entry : lastKnownItems.entrySet()) {
			int slotIndex = entry.getKey();
			if (slotIndex < screen.getScreenHandler().slots.size()) {
				Slot slot = screen.getScreenHandler().slots.get(slotIndex);
				ItemStack currentItem = slot.getStack();
				
				// Wenn es ein schwarzer Betonblock ist, verwende das originale Item
				if (currentItem.getItem() == Items.BLACK_CONCRETE) {
					currentItem = originalItems.get(slotIndex);
					if (currentItem == null) {
						currentItem = ItemStack.EMPTY;
					}
				}
				
				// Wenn das Item leer ist, aber in lastKnownItems vorhanden war, wurde es entfernt
				if ((currentItem == null || currentItem.isEmpty()) && !entry.getValue().isEmpty()) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Prüft, ob zwei ItemStacks gleich sind (basierend auf Name und anderen wichtigen Eigenschaften)
	 */
	private static boolean areItemsEqual(ItemStack item1, ItemStack item2) {
		if (item1.isEmpty() != item2.isEmpty()) {
			return false;
		}
		if (item1.isEmpty()) {
			return true;
		}
		
		// Prüfe Item-Typ
		if (item1.getItem() != item2.getItem()) {
			return false;
		}
		
		// Prüfe Custom Name
		var name1 = item1.get(DataComponentTypes.CUSTOM_NAME);
		var name2 = item2.get(DataComponentTypes.CUSTOM_NAME);
		if (name1 != null && name2 != null) {
			if (!name1.getString().equals(name2.getString())) {
				return false;
			}
		} else if (name1 != null || name2 != null) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Aktualisiert die originalen Items nach einer Inventaränderung
	 */
	private static void updateOriginalItemsAfterChange(HandledScreen<?> screen) {
		if (screen == null) {
			return;
		}
		
		// Stelle zuerst alle originalen Items wieder her
		restoreOriginalItems(screen, false);
		
		// Durchlaufe alle Filter-Slots und aktualisiere originale Items
		for (int slotIndex : FILTER_SLOTS) {
			if (slotIndex >= screen.getScreenHandler().slots.size()) {
				continue;
			}
			
			Slot slot = screen.getScreenHandler().slots.get(slotIndex);
			ItemStack currentItem = slot.getStack();
			
			// Überspringe schwarze Betonblöcke
			if (currentItem.getItem() == Items.BLACK_CONCRETE) {
				continue;
			}
			
			if (currentItem.isEmpty()) {
				// Item wurde entfernt - entferne auch aus originalItems
				originalItems.remove(slotIndex);
			} else {
				// Item ist vorhanden - aktualisiere oder füge hinzu
				originalItems.put(slotIndex, currentItem.copy());
			}
		}
	}
	
	/**
	 * Speichert alle originalen Items aus den Filter-Slots
	 */
	private static void saveOriginalItems(HandledScreen<?> screen) {
		if (screen == null) return;
		
		// Durchlaufe nur die Filter-Slots und speichere originale Items
		for (int slotIndex : FILTER_SLOTS) {
			if (slotIndex < screen.getScreenHandler().slots.size()) {
				Slot slot = screen.getScreenHandler().slots.get(slotIndex);
				ItemStack itemStack = slot.getStack();
				
				if (!itemStack.isEmpty()) {
					// Prüfe ob es bereits ein schwarzer Betonblock ist (dann ist es bereits gefiltert)
					if (itemStack.getItem() != Items.BLACK_CONCRETE) {
						// Speichere das originale Item
						originalItems.put(slotIndex, itemStack.copy());
					}
				}
			}
		}
	}
	
	/**
	 * Aktualisiert die Items basierend auf den ausgewählten Kit-Filtern
	 */
	private static void updateFilteredItems(HandledScreen<?> screen, MinecraftClient client) {
		// Stelle sicher, dass alle originalen Items in den Filter-Slots gespeichert sind
		// (falls neue Items hinzugefügt wurden oder das Inventar sich geändert hat)
		for (int slotIndex : FILTER_SLOTS) {
			if (slotIndex < screen.getScreenHandler().slots.size()) {
				Slot slot = screen.getScreenHandler().slots.get(slotIndex);
				ItemStack itemStack = slot.getStack();
				
				if (!itemStack.isEmpty()) {
					// Prüfe ob es bereits ein schwarzer Betonblock ist (dann ist es bereits gefiltert)
					if (itemStack.getItem() != Items.BLACK_CONCRETE) {
						// Speichere das originale Item falls noch nicht gespeichert
						if (!originalItems.containsKey(slotIndex)) {
							originalItems.put(slotIndex, itemStack.copy());
						}
					}
				}
			}
		}
		
		// Prüfe ob mindestens ein aktiver Filter vorhanden ist
		boolean hasActiveFilter = false;
		for (int i = 0; i < BUTTON_COUNT; i++) {
			if (selectedKits.containsKey(i) && activeFilters.getOrDefault(i, false)) {
				hasActiveFilter = true;
				break;
			}
		}
		
		if (!hasActiveFilter) {
			// Kein Filter aktiv - stelle alle Items wieder her
			restoreOriginalItems(screen);
			return;
		}
		
		// Durchlaufe nur die Filter-Slots und wende Filter an
		for (int slotIndex : FILTER_SLOTS) {
			if (slotIndex >= screen.getScreenHandler().slots.size()) {
				continue;
			}
			
			Slot slot = screen.getScreenHandler().slots.get(slotIndex);
			ItemStack itemStack = slot.getStack();
			
			// Wenn der Slot bereits schwarzen Beton enthält, hole das originale Item
			if (itemStack.getItem() == Items.BLACK_CONCRETE && originalItems.containsKey(slotIndex)) {
				itemStack = originalItems.get(slotIndex);
			}
			
			if (!itemStack.isEmpty() && itemStack.getItem() != Items.BLACK_CONCRETE) {
				
				// Prüfe ob das Item zu einem der aktiven Kits gehört
				boolean matchesAnyKit = false;
				for (int i = 0; i < BUTTON_COUNT; i++) {
					KitSelection selection = selectedKits.get(i);
					boolean isFilterActive = activeFilters.getOrDefault(i, false);
					if (selection != null && isFilterActive && isItemInKit(itemStack, selection)) {
						matchesAnyKit = true;
						break;
					}
				}
				
				if (!matchesAnyKit) {
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
				} else {
					// Item passt zu einem Kit - stelle das originale Item wieder her falls es ausgeblendet war
					ItemStack currentItem = slot.getStack();
					if (currentItem.getItem() == Items.BLACK_CONCRETE && originalItems.containsKey(slotIndex)) {
						slot.setStack(originalItems.get(slotIndex));
					} else if (currentItem.getItem() != Items.BLACK_CONCRETE && originalItems.containsKey(slotIndex)) {
						// Stelle sicher, dass das originale Item angezeigt wird (falls es geändert wurde)
						ItemStack originalItem = originalItems.get(slotIndex);
						if (!currentItem.equals(originalItem)) {
							// Item hat sich geändert - aktualisiere die gespeicherte Version
							originalItems.put(slotIndex, currentItem.copy());
						}
					}
				}
			}
		}
	}
	
	/**
	 * Stellt die originalen Items wieder her
	 * @param clearOriginalItems Wenn true, wird die originalItems Map nach der Wiederherstellung geleert
	 */
	private static void restoreOriginalItems(HandledScreen<?> screen, boolean clearOriginalItems) {
		if (screen == null) {
			if (clearOriginalItems) {
				originalItems.clear();
			}
			return;
		}
		
		// Durchlaufe nur die Filter-Slots und stelle Items wieder her
		for (int slotIndex : FILTER_SLOTS) {
			if (slotIndex >= screen.getScreenHandler().slots.size()) {
				continue;
			}
			
			Slot slot = screen.getScreenHandler().slots.get(slotIndex);
			ItemStack currentItem = slot.getStack();
			
			// Wenn der Slot schwarzen Beton enthält, stelle das originale Item wieder her
			if (currentItem.getItem() == Items.BLACK_CONCRETE) {
				if (originalItems.containsKey(slotIndex)) {
					slot.setStack(originalItems.get(slotIndex));
				}
			}
		}
		
		if (clearOriginalItems) {
			originalItems.clear();
		}
	}
	
	/**
	 * Stellt die originalen Items wieder her (ohne originalItems zu löschen)
	 */
	private static void restoreOriginalItems(HandledScreen<?> screen) {
		restoreOriginalItems(screen, true);
	}
	
	/**
	 * Prüft ob ein Item zu einem bestimmten Kit gehört
	 * Vergleicht den Item-Namen mit den gespeicherten Item-Namen für das Kit
	 */
	public static boolean isItemInKit(ItemStack itemStack, KitSelection kitSelection) {
		if (itemStack == null || itemStack.isEmpty() || kitSelection == null) {
			return false;
		}
		
		// Hole die Item-Informationen für dieses Kit und diese Stufe
		Set<ItemInfo> itemInfos = getKitItemInfos(kitSelection.kitType, kitSelection.level);
		if (itemInfos == null || itemInfos.isEmpty()) {
			return false; // Keine Item-Informationen für dieses Kit und diese Stufe definiert
		}
		
		// Extrahiere die Item-Namen aus den Item-Informationen
		Set<String> expectedNames = new HashSet<>();
		for (ItemInfo itemInfo : itemInfos) {
			expectedNames.add(itemInfo.name);
		}
		
		// Hole den Item-Namen (ohne Formatierung)
		String itemName = itemStack.getName().getString();
		// Entferne Formatierungs-Codes für Vergleich
		String cleanItemName = itemName.replaceAll("§[0-9a-fk-or]", "");
		
		// Prüfe ob der Item-Name mit einem der erwarteten Namen übereinstimmt (exakt)
		for (String expectedName : expectedNames) {
			String cleanExpectedName = expectedName.replaceAll("§[0-9a-fk-or]", "");
			// Nur exakte Matches, keine Teilstring-Matches
			if (cleanItemName.equalsIgnoreCase(cleanExpectedName)) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Setzt die Item-Informationen für ein Kit und eine Stufe
	 */
	private static void setKitItemInfos(KitType kitType, int level, Set<ItemInfo> itemInfos) {
		kitItemInfos.computeIfAbsent(kitType, k -> new HashMap<>()).put(level, itemInfos);
	}
	
	/**
	 * Gibt die Item-Informationen für ein Kit und eine Stufe zurück
	 */
	public static Set<ItemInfo> getKitItemInfos(KitType kitType, int level) {
		Map<Integer, Set<ItemInfo>> levelMap = kitItemInfos.get(kitType);
		if (levelMap == null) {
			return new HashSet<>();
		}
		return levelMap.getOrDefault(level, new HashSet<>());
	}
	
	/**
	 * Gibt die Item-Namen für ein Kit und eine Stufe zurück (nur Namen, für Rückwärtskompatibilität)
	 */
	public static Set<String> getKitItemNames(KitType kitType, int level) {
		Set<ItemInfo> itemInfos = getKitItemInfos(kitType, level);
		Set<String> itemNames = new HashSet<>();
		for (ItemInfo itemInfo : itemInfos) {
			itemNames.add(itemInfo.name);
		}
		return itemNames;
	}
	
	/**
	 * Getter für den Button-Index der aktuell geöffnet ist
	 */
	public static int getCurrentButtonIndex() {
		return currentButtonIndex;
	}
	
	/**
	 * Schließt den Kit-Auswahl-Screen
	 */
	public static void closeKitSelectionScreen() {
		currentButtonIndex = -1;
	}
}


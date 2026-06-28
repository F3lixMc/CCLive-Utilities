package net.felix.utilities.Aincraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.felix.utilities.Overall.InformationenUtility;
import net.felix.utilities.Overall.ZeichenUtility;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import net.minecraft.util.Identifier;

public class ItemInfoUtility {

	/** Item-Extraktion (Hotkeys, Auto-Klick, Overlays) – derzeit deaktiviert. */
	private static boolean ENABLED = false;

	private static boolean isInitialized = false;
	private static KeyBinding extractKeyBinding;
	
	// JSON file paths
	private static final String BLUEPRINTS_CONFIG_FILE = "assets/cclive-utilities/blueprints.json";
	private static final String ASPECTS_CONFIG_FILE = "assets/cclive-utilities/Aspekte.json";
	private static final String AINCRAFT_CONFIG_FILE = "assets/cclive-utilities/Aincraft.json";
	private static final String FARMWORLD_CONFIG_FILE = "assets/cclive-utilities/Farmworld.json";
	
	// Cache for loaded JSON data
	private static JsonObject blueprintsData = null;
	private static JsonObject aspectsData = null;
	private static JsonObject aincraftData = null;
	private static JsonObject farmworldData = null;
	
	// Set of registered item names (from extracted_items.json)
	private static Set<String> registeredItemNames = new HashSet<>();
	
	// Set of all materials from Aincraft.json
	private static Set<String> aincraftMaterials = new HashSet<>();
	
	// Queue for auto-clicking items (to spread clicks over multiple frames)
	private static final Queue<Integer> pendingClicks = new LinkedList<>();
	private static final int clicksPerFrame = 2; // Number of clicks per frame to avoid lag
	
	/** Fisch-Materialien: „Legendäres Thunfischfleisch“, „Epische Kaninchenfischwirbelsäule“, … */
	private static final Pattern FISHING_RARITY_MATERIAL_PATTERN = Pattern.compile(
		"^(Gewöhnlich|Ungewöhnlich|Selten|Episch|Legendär)[a-zäöüß]*\\s+.+"
	);
	
	/** Angel-Komponenten ohne Bindestrich: „Meister Gewicht“ → „Meister-Gewicht“ */
	private static final Pattern FISHING_COMPONENT_NAME_PATTERN = Pattern.compile(
		"^([A-Za-zÄÖÜäöüß]+)\\s+([A-Za-zÄÖÜäöüß]+)$"
	);
	
	private static final String[] MATERIAL_RARITY_ROOTS = {
		"Gewöhnlich", "Ungewöhnlich", "Selten", "Episch", "Legendär"
	};
	
	// Set of Amboss items
	private static final Set<String> AMBOSS_ITEMS = Set.of(
		"Spinnfaden", "Stoff", "Ziegelstein", "Kettenglied", "Leder", "Zahnrad", 
		"Klammer", "Schraube", "Stabile Schnur", "Kupferplatten", "Gehärteter Ziegelstein", 
		"Eisenplatten", "Verstärkte Schraube", "Verbessertes Zahnrad"
	);
	
	// Target slots: 10-16, 19-25, 28-34, 37-43 (Bauplan [Shop])
	private static final int[] BLUEPRINT_SHOP_SLOTS = {
		10, 11, 12, 13, 14, 15, 16,
		19, 20, 21, 22, 23, 24, 25,
		28, 29, 30, 31, 32, 33, 34,
		37, 38, 39, 40, 41, 42, 43
	};
	
	private static final int[] FISHING_COMPONENTS_FULL_SLOTS = IntStream.rangeClosed(0, 44).toArray();
	private static final int[] FISHING_COMPONENTS_PARTIAL_SLOTS = IntStream.rangeClosed(9, 44).toArray();
	
	public static void initialize() {
		if (!ENABLED || isInitialized) {
			return;
		}
		
		try {
			// Load JSON data
			loadBlueprintsData();
			loadAspectsData();
			loadAincraftData();
			loadFarmworldData();
			
			// Load registered items
			loadRegisteredItems();
			
			// Register hotkey
			registerHotkey();
			
			// Register client tick event
			ClientTickEvents.END_CLIENT_TICK.register(ItemInfoUtility::onClientTick);
			
			isInitialized = true;
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	private static KeyBinding autoClickKeyBinding;
	
	private static void registerHotkey() {
		extractKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.cclive-utilities.item-info-extract",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(), // Unbound by default
			new KeyBinding.Category(Identifier.of("cclive-utilities", "item-info"))
		));
		
		// Register auto-click hotkey
		autoClickKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.cclive-utilities.item-info-auto-click",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(), // Unbound by default
			new KeyBinding.Category(Identifier.of("cclive-utilities", "item-info"))
		));
	}
	
	private static void onClientTick(MinecraftClient client) {
		// Check hotkey even when inventory is open (backup check)
		if (extractKeyBinding != null && extractKeyBinding.wasPressed()) {
			extractItemInfo(client);
		}
		
		// Check auto-click hotkey
		if (autoClickKeyBinding != null && autoClickKeyBinding.wasPressed()) {
			autoRightClickUnregisteredItems(client);
		}
		
		// Process pending clicks (spread over multiple frames)
		processPendingClicks(client);
	}
	
	/**
	 * Processes pending clicks from the queue (called every tick)
	 */
	private static void processPendingClicks(MinecraftClient client) {
		if (pendingClicks.isEmpty() || client.currentScreen == null || !(client.currentScreen instanceof HandledScreen)) {
			return;
		}
		
		HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;
		net.minecraft.screen.ScreenHandler handler = screen.getScreenHandler();
		
		// Process a few clicks per frame
		int processed = 0;
		while (!pendingClicks.isEmpty() && processed < clicksPerFrame) {
			Integer slotIndex = pendingClicks.poll();
			if (slotIndex != null && slotIndex < handler.slots.size()) {
				try {
					if (client.player != null && client.interactionManager != null) {
						client.interactionManager.clickSlot(
							handler.syncId,
							slotIndex,
							1, // button: 0 = left, 1 = right
							net.minecraft.screen.slot.SlotActionType.PICKUP,
							client.player
						);
					}
				} catch (Exception e) {
					// Ignore errors for individual slots
				}
				processed++;
			}
		}
	}
	
	/**
	 * Handles key press in inventory screens
	 * Called from SearchBarInputMixin
	 */
	public static boolean handleKeyPress(int keyCode) {
		if (extractKeyBinding == null) {
			return false;
		}
		
		try {
			// Check if the pressed key matches our key binding
			// Use -1 as scanCode to ignore scanCode comparison (like OverlayEditorUtility)
			if (extractKeyBinding.matchesKey(new net.minecraft.client.input.KeyInput(keyCode, -1, 0))) {
				MinecraftClient client = MinecraftClient.getInstance();
				if (client != null) {
					extractItemInfo(client);
					return true;
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
		
		return false;
	}
	
	/**
	 * Handles auto-click key press in inventory screens
	 * Called from SearchBarInputMixin
	 * @param screenX X position of the screen (from mixin)
	 * @param screenY Y position of the screen (from mixin)
	 */
	public static boolean handleAutoClickKeyPress(int keyCode, int screenX, int screenY) {
		if (autoClickKeyBinding == null) {
			return false;
		}
		
		try {
			// Check if the pressed key matches our key binding
			// Use -1 as scanCode to ignore scanCode comparison (like OverlayEditorUtility)
			if (autoClickKeyBinding.matchesKey(new net.minecraft.client.input.KeyInput(keyCode, -1, 0))) {
				MinecraftClient client = MinecraftClient.getInstance();
				if (client != null) {
					autoRightClickUnregisteredItems(client, screenX, screenY);
					return true;
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
		
		return false;
	}
	
	/**
	 * Overload without screen position (for backward compatibility)
	 */
	public static boolean handleAutoClickKeyPress(int keyCode) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.currentScreen == null || !(client.currentScreen instanceof HandledScreen)) {
			return false;
		}
		
		HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;
		// Try to get position from mixin via reflection
		try {
			java.lang.reflect.Field[] fields = HandledScreen.class.getDeclaredFields();
			int screenX = 0, screenY = 0;
			for (java.lang.reflect.Field field : fields) {
				if (field.getType() == int.class) {
					field.setAccessible(true);
					int value = field.getInt(screen);
					// Heuristic: x and y are usually small positive values
					if (value > 0 && value < 1000) {
						if (screenX == 0) {
							screenX = value;
						} else if (screenY == 0) {
							screenY = value;
							break;
						}
					}
				}
			}
			if (screenX > 0 && screenY > 0) {
				return handleAutoClickKeyPress(keyCode, screenX, screenY);
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Fallback: use 0,0
		return handleAutoClickKeyPress(keyCode, 0, 0);
	}
	
	/**
	 * Automatically right-clicks on unregistered items in target slots
	 * This simulates right-clicking to "view" the items
	 * @param screenX X position of the screen (from mixin)
	 * @param screenY Y position of the screen (from mixin)
	 */
	public static void autoRightClickUnregisteredItems(MinecraftClient client, int screenX, int screenY) {
		if (client.currentScreen == null || !(client.currentScreen instanceof HandledScreen)) {
			client.player.sendMessage(Text.literal("§c[ItemInfo] Kein Inventar geöffnet!"), false);
			return;
		}
		
		HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;
		
		// Check if we're in a blueprint shop inventory
		String title = screen.getTitle().getString();
		String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\u3400-\\u4DBF]", "");
		if (!cleanTitle.contains("Bauplan [Shop]") && !cleanTitle.contains("Blueprint Store")) {
			client.player.sendMessage(Text.literal("§c[ItemInfo] Nicht im Bauplan [Shop] Inventar!"), false);
			return;
		}
		
		int clickedCount = 0;
		int checkedCount = 0;
		
		// Clear any pending clicks from previous runs
		pendingClicks.clear();
		
		// Iterate through target slots and queue unregistered items for clicking
		for (int slotIndex : BLUEPRINT_SHOP_SLOTS) {
			if (slotIndex >= screen.getScreenHandler().slots.size()) {
				continue;
			}
			
			Slot slot = screen.getScreenHandler().slots.get(slotIndex);
			if (slot.hasStack()) {
				ItemStack stack = slot.getStack();
				if (stack != null && !stack.isEmpty()) {
					checkedCount++;
					
					// Check if item is not registered (click all unregistered items)
					boolean isRegistered = isItemRegistered(stack, client);
					
					if (!isRegistered) {
						// Add to queue instead of clicking immediately
						pendingClicks.offer(slotIndex);
						clickedCount++;
					}
				}
			}
		}
		
		if (clickedCount > 0) {
			client.player.sendMessage(Text.literal("§a[ItemInfo] §f" + clickedCount + " Items automatisch angeklickt!"), false);
		} else {
			client.player.sendMessage(Text.literal("§e[ItemInfo] §fKeine nicht registrierten Items gefunden (geprüft: " + checkedCount + ")"), false);
		}
	}
	
	/**
	 * Overload without screen position (for backward compatibility)
	 */
	public static void autoRightClickUnregisteredItems(MinecraftClient client) {
		// Try to get screen position from mixin or use default
		if (client.currentScreen instanceof HandledScreen) {
			HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;
			// Try to get position from mixin via reflection as fallback
			try {
				java.lang.reflect.Field xField = HandledScreen.class.getDeclaredField("field_2776"); // Obfuscated name
				java.lang.reflect.Field yField = HandledScreen.class.getDeclaredField("field_2777"); // Obfuscated name
				xField.setAccessible(true);
				yField.setAccessible(true);
				int screenX = xField.getInt(screen);
				int screenY = yField.getInt(screen);
				autoRightClickUnregisteredItems(client, screenX, screenY);
				return;
			} catch (Exception e) {
				// Try other possible field names
				try {
					java.lang.reflect.Field[] fields = HandledScreen.class.getDeclaredFields();
					int screenX = 0, screenY = 0;
					for (java.lang.reflect.Field field : fields) {
						if (field.getType() == int.class) {
							field.setAccessible(true);
							int value = field.getInt(screen);
							// Heuristic: x and y are usually small positive values
							if (value > 0 && value < 1000) {
								if (screenX == 0) {
									screenX = value;
								} else if (screenY == 0) {
									screenY = value;
									break;
								}
							}
						}
					}
					if (screenX > 0 && screenY > 0) {
						autoRightClickUnregisteredItems(client, screenX, screenY);
						return;
					}
				} catch (Exception e2) {
					// Ignore
				}
			}
		}
		// Fallback: use 0,0 if we can't get position
		autoRightClickUnregisteredItems(client, 0, 0);
	}
	
	private static void extractItemInfo(MinecraftClient client) {
		if (client.currentScreen == null || !(client.currentScreen instanceof HandledScreen)) {
			client.player.sendMessage(Text.literal("§c[ItemInfo] Kein Inventar geöffnet!"), false);
			return;
		}
		
		HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;
		if (!isSupportedInventory(screen)) {
			client.player.sendMessage(Text.literal("§c[ItemInfo] Nicht in einem unterstützten Inventar (Bauplan [Shop] oder Angel-Komponenten)!"), false);
			return;
		}
		
		List<ItemInfoData> items = new ArrayList<>();
		int[] targetSlots = getTargetSlotsForScreen(screen);
		
		// Read items from target slots
		for (int slotIndex : targetSlots) {
			if (slotIndex >= screen.getScreenHandler().slots.size()) {
				continue;
			}
			
			Slot slot = screen.getScreenHandler().slots.get(slotIndex);
			if (slot.hasStack()) {
				ItemStack stack = slot.getStack();
				if (stack != null && !stack.isEmpty()) {
					if (isExtractableItem(stack, client)) {
						ItemInfoData itemData = parseItem(stack, client);
						if (itemData != null) {
							items.add(itemData);
						}
					}
				}
			}
		}
		
		if (items.isEmpty()) {
			client.player.sendMessage(Text.literal("§c[ItemInfo] Keine extrahierbaren Items in den angegebenen Slots gefunden!"), false);
			return;
		}
		
		// Write to file
		writeItemsToFile(items, client);
	}
	
	public static boolean isSupportedInventory(HandledScreen<?> screen) {
		if (!ENABLED || !isInitialized || screen == null) {
			return false;
		}
		String cleanTitle = cleanInventoryTitle(screen.getTitle().getString());
		return cleanTitle.contains("Bauplan [Shop]")
			|| cleanTitle.contains("Blueprint Store")
			|| ZeichenUtility.isFishingEquipmentSearchMenu(cleanTitle);
	}
	
	/**
	 * Nur Farbcodes entfernen – Menü-Glyphen (z. B. ui_components_craft / 㶄) müssen erhalten bleiben,
	 * damit {@link ZeichenUtility#isFishingEquipmentSearchMenu(String)} greift (wie in SearchBarUtility).
	 */
	private static String cleanInventoryTitle(String title) {
		if (title == null) {
			return "";
		}
		return title.replaceAll("§[0-9a-fk-or]", "");
	}
	
	private static int[] getTargetSlotsForScreen(HandledScreen<?> screen) {
		String cleanTitle = cleanInventoryTitle(screen.getTitle().getString());
		if (ZeichenUtility.isFishingEquipmentSearchMenu(cleanTitle)) {
			if (ZeichenUtility.isFishingComponentsCraftedTitle(cleanTitle)
					|| ZeichenUtility.isFishingCraftedComponentsTitle(cleanTitle)) {
				return FISHING_COMPONENTS_FULL_SLOTS;
			}
			return FISHING_COMPONENTS_PARTIAL_SLOTS;
		}
		return BLUEPRINT_SHOP_SLOTS;
	}

	/** Öffentlicher Name-Extractor für Inventar-Scan (Angel-Komponenten). */
	public static String getFishingComponentNameFromStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}
		InformationenUtility.BlueprintNameAndColor nameAndColor =
			InformationenUtility.extractBlueprintNameAndColorFromItemName(stack.getName());
		String name = nameAndColor != null && nameAndColor.name != null ? nameAndColor.name : "";
		if (name.isEmpty()) {
			name = cleanBlueprintName(stack.getName().getString());
		}
		return normalizeFishingComponentName(name);
	}

	/** Öffentlicher Name-Extractor für Inventar-Scan (Fischreusen). */
	public static String getFishTrapNameFromStack(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "";
		}
		InformationenUtility.BlueprintNameAndColor nameAndColor =
			InformationenUtility.extractBlueprintNameAndColorFromItemName(stack.getName());
		String name = nameAndColor != null && nameAndColor.name != null ? nameAndColor.name : "";
		if (name.isEmpty()) {
			name = stack.getName().getString();
		}
		return normalizeFishTrapName(name);
	}

	private static String normalizeFishTrapName(String name) {
		if (name == null || name.isEmpty()) {
			return "";
		}
		return cleanBlueprintName(name)
			.replaceAll("[\\u4E00-\\u9FFF]", "")
			.replaceAll("\\s+", " ")
			.trim();
	}
	
	/**
	 * Checks if the item tooltip is extractable (Bauplan shop items or Angel-Komponenten)
	 */
	private static boolean isExtractableItem(ItemStack stack, MinecraftClient client) {
		return isBlueprintShopItem(stack, client) || isFishingComponentItem(stack, client);
	}
	
	private static boolean isBlueprintShopItem(ItemStack stack, MinecraftClient client) {
		return tooltipContains(stack, client, "[Rechtsklick] Kaufkosten");
	}
	
	private static boolean isFishingComponentItem(ItemStack stack, MinecraftClient client) {
		return tooltipContains(stack, client, "[Linksklick] um die Komponente herzustellen");
	}
	
	private static boolean tooltipContains(ItemStack stack, MinecraftClient client, String needle) {
		try {
			List<Text> tooltip = getItemTooltip(stack, client.player);
			if (tooltip == null || tooltip.isEmpty()) {
				return false;
			}
			
			for (Text line : tooltip) {
				String text = cleanTooltipLine(line.getString());
				if (text.contains(needle)) {
					return true;
				}
			}
		} catch (Exception e) {
			// Ignore errors, return false
		}
		
		return false;
	}
	
	private static String cleanTooltipLine(String text) {
		if (text == null) {
			return "";
		}
		return text.replaceAll("§[0-9a-fk-or]", "")
			.replaceAll("[\\u3400-\\u4DBF]", "")
			.trim();
	}
	
	private static List<String> buildCleanTooltipLines(List<Text> tooltip) {
		List<String> lines = new ArrayList<>();
		for (Text line : tooltip) {
			String text = line.getString();
			if (text != null && !text.trim().isEmpty()) {
				text = text.replaceAll("[\\u3400-\\u4DBF]", "");
				if (ZeichenUtility.containsPixelSpacer(text)) {
					continue;
				}
				if (text.trim().isEmpty()) {
					continue;
				}
				lines.add(text);
			}
		}
		return lines;
	}
	
	private static int findLineIndex(List<String> lines, String exactMatch) {
		for (int i = 0; i < lines.size(); i++) {
			if (exactMatch.equals(cleanTooltipLine(lines.get(i)))) {
				return i;
			}
		}
		return -1;
	}
	
	private static String cleanBlueprintName(String name) {
		if (name == null) {
			return "";
		}
		return name.replaceAll("§[0-9a-fk-or]", "")
			.replaceAll("[\\u3400-\\u4DBF]", "")
			.replaceAll("\\s*-\\s*\\[Bauplan\\]", "")
			.replaceAll("\\s*\\[Bauplan\\]", "")
			.replaceAll("\\s*-\\s*\\[Blueprint\\]", "")
			.replaceAll("\\s*\\[Blueprint\\]", "")
			.trim();
	}
	
	private static boolean hasMaterialRarityPrefix(String name) {
		if (name == null) {
			return false;
		}
		for (String root : MATERIAL_RARITY_ROOTS) {
			if (name.startsWith(root)) {
				return true;
			}
		}
		return false;
	}
	
	/** „Meister Gewicht“ → „Meister-Gewicht“ (nur Komponenten-Namen, keine Seltenheits-Materialien). */
	private static String normalizeFishingComponentName(String name) {
		if (name == null || name.isEmpty()) {
			return "";
		}
		name = name.trim();
		if (name.contains("-") || hasMaterialRarityPrefix(name)) {
			return name;
		}
		var matcher = FISHING_COMPONENT_NAME_PATTERN.matcher(name);
		if (matcher.matches()) {
			return matcher.group(1) + "-" + matcher.group(2);
		}
		return name;
	}
	
	private static String extractFishingComponentName(ItemStack stack, List<String> lines) {
		String nameFromTooltip = !lines.isEmpty() ? cleanBlueprintName(lines.get(0)) : "";
		InformationenUtility.BlueprintNameAndColor nameAndColor =
			InformationenUtility.extractBlueprintNameAndColorFromItemName(stack.getName());
		String nameFromStack = nameAndColor != null && nameAndColor.name != null ? nameAndColor.name : "";
		
		String name = !nameFromTooltip.isEmpty() ? nameFromTooltip : nameFromStack;
		if (name.isEmpty()) {
			name = nameFromStack;
		}
		return normalizeFishingComponentName(name);
	}
	
	private static String resolveItemDisplayName(ItemStack stack, MinecraftClient client) {
		try {
			List<Text> tooltip = getItemTooltip(stack, client.player);
			if (tooltip == null || tooltip.isEmpty()) {
				return "";
			}
			if (isFishingComponentItem(stack, client)) {
				List<String> lines = buildCleanTooltipLines(tooltip);
				return extractFishingComponentName(stack, lines);
			}
			return cleanBlueprintName(tooltip.get(0).getString());
		} catch (Exception e) {
			return "";
		}
	}
	
	private static ItemInfoData parseItem(ItemStack stack, MinecraftClient client) {
		// Get tooltip
		List<Text> tooltip = getItemTooltip(stack, client.player);
		if (tooltip == null || tooltip.isEmpty()) {
			return null;
		}
		
		List<String> lines = buildCleanTooltipLines(tooltip);
		if (lines.isEmpty()) {
			return null;
		}
		
		if (isFishingComponentItem(stack, client)) {
			return parseFishingComponent(stack, client, lines);
		}
		
		ItemInfoData data = new ItemInfoData();
		
		// Extract item ID and customModelData
		String itemId = stack.getItem().toString();
		data.id = itemId;
		
		data.customModelData = extractCustomModelData(stack);
		
		// Extract name (first line, remove "[Bauplan]")
		if (!lines.isEmpty()) {
			data.name = cleanBlueprintName(lines.get(0));
		}
		
		// Find stats first (needed for type/piece determination)
		// Find stats (3 lines after name, look for "Rüstung", "Schaden", "Abbaugeschwindigkeit", "Reichweite")
		int statsLineIndex = -1;
		String statsLineContent = null;
		for (int i = 2; i < Math.min(lines.size(), 5); i++) {
			String line = lines.get(i).replaceAll("§[0-9a-fk-or]", "").trim();
			// Remove chinese characters
			line = line.replaceAll("[\\u3400-\\u4DBF]", "").trim();
			if (line.contains("Rüstung") || line.contains("Schaden") || line.contains("Abbaugeschwindigkeit") || line.contains("Reichweite")) {
				statsLineIndex = i;
				statsLineContent = line;
				break;
			}
		}
		
		// Find type and piece (line after name)
		// Special rules:
		// - "Bogen, Armbrust, Schwert" → type = "Waffe", piece = what's in the line
		// - "Axt" with "Schaden X" → type = "Waffe"
		// - "Spitzhacke, Hacke, Axt" → type = "Werkzeug"
		// - "Axt" with "Abbaugeschwindigkeit X" (and NOT "Schaden X") → type = "Werkzeug"
		// - For armor: "piece" + chinese characters + "type" OR "pieceType" (e.g., "HoseLeder")
		//   Split by capital letter: "HoseLeder" → piece="Hose", type="Leder"
		if (lines.size() > 1) {
			String typePieceLine = lines.get(1).replaceAll("§[0-9a-fk-or]", "").trim();
			
			// First, check if line contains chinese characters as separator
			// Format: "piece" + chinese characters + "type"
			String originalLine = typePieceLine;
			boolean hasChineseSeparator = originalLine.matches(".*[\\u3400-\\u4DBF].*");
			
			if (hasChineseSeparator) {
				// Split by chinese characters
				String[] parts = originalLine.split("[\\u3400-\\u4DBF]+");
				if (parts.length >= 2) {
					// First part is piece, second part is type
					data.piece = parts[0].trim();
					data.type = parts[1].trim();
				} else if (parts.length == 1) {
					// Only one part found, try to split by capital letter
					String part = parts[0].trim();
					// Find position where a capital letter appears (after first word)
					int capitalPos = -1;
					for (int i = 1; i < part.length(); i++) {
						if (Character.isUpperCase(part.charAt(i))) {
							capitalPos = i;
							break;
						}
					}
					if (capitalPos > 0) {
						data.piece = part.substring(0, capitalPos).trim();
						data.type = part.substring(capitalPos).trim();
					} else {
						// No capital letter found, treat as type
						data.type = part;
					}
				}
			} else {
				// No chinese separator, check if words are concatenated (e.g., "HoseLeder")
				// Look for capital letter in the middle of the string
				int capitalPos = -1;
				for (int i = 1; i < originalLine.length(); i++) {
					if (Character.isUpperCase(originalLine.charAt(i))) {
						capitalPos = i;
						break;
					}
				}
				
				if (capitalPos > 0) {
					// Split by capital letter: first part is piece, second part is type
					data.piece = originalLine.substring(0, capitalPos).trim();
					data.type = originalLine.substring(capitalPos).trim();
				} else {
					// No capital letter found, remove chinese characters and parse normally
					typePieceLine = typePieceLine.replaceAll("[\\u3400-\\u4DBF]", "").trim();
					
					// Check for special weapon types
					if (typePieceLine.contains("Bogen") || typePieceLine.contains("Armbrust") || typePieceLine.contains("Schwert")) {
						// These are weapons
						data.type = "Waffe";
						// piece is what's in the line (could be "Bogen", "Armbrust", "Schwert", etc.)
						data.piece = typePieceLine;
					} else if (typePieceLine.contains("Axt")) {
						// Check stats to determine if it's a weapon or tool
						boolean hasSchaden = false;
						boolean hasAbbaugeschwindigkeit = false;
						
						// Check stats for "Schaden" or "Abbaugeschwindigkeit"
						if (statsLineContent != null) {
							hasSchaden = statsLineContent.contains("Schaden");
							hasAbbaugeschwindigkeit = statsLineContent.contains("Abbaugeschwindigkeit");
						}
						
						if (hasSchaden) {
							// Axt with Schaden → Waffe
							data.type = "Waffe";
							data.piece = typePieceLine;
						} else if (hasAbbaugeschwindigkeit) {
							// Axt with Abbaugeschwindigkeit (and no Schaden) → Werkzeug
							data.type = "Werkzeug";
							data.piece = typePieceLine;
						} else {
							// Default: treat as piece, type from line
							String[] parts = typePieceLine.split("\\s+");
							if (parts.length == 1) {
								data.type = parts[0];
							} else if (parts.length >= 2) {
								data.piece = parts[0];
								data.type = parts[1];
							}
						}
					} else if (typePieceLine.contains("Spitzhacke") || typePieceLine.contains("Hacke")) {
						// These are tools
						data.type = "Werkzeug";
						data.piece = typePieceLine;
					} else {
						// Normal parsing: if one word = type, if two words = piece + type
						String[] parts = typePieceLine.split("\\s+");
						if (parts.length == 1) {
							// Only type
							data.type = parts[0];
						} else if (parts.length >= 2) {
							// First is piece, second is type
							data.piece = parts[0];
							data.type = parts[1];
						}
					}
				}
			}
		}
		
		if (statsLineIndex >= 0) {
			String statsLine = lines.get(statsLineIndex).replaceAll("§[0-9a-fk-or]", "").trim();
			// Remove chinese characters
			statsLine = statsLine.replaceAll("[\\u3400-\\u4DBF]", "").trim();
			// Check next line for "Angriffsgeschwindigkeit"
			if (statsLineIndex + 1 < lines.size()) {
				String nextLine = lines.get(statsLineIndex + 1).replaceAll("§[0-9a-fk-or]", "").trim();
				// Remove chinese characters
				nextLine = nextLine.replaceAll("[\\u3400-\\u4DBF]", "").trim();
				if (nextLine.contains("Angriffsgeschwindigkeit")) {
					// Format: ["Angriffsgeschwindigkeit X", "Schaden X"] or similar
					data.stats = new ArrayList<>();
					data.stats.add(nextLine);
					data.stats.add(statsLine);
				} else {
					data.stats = new ArrayList<>();
					data.stats.add(statsLine);
				}
			} else {
				data.stats = new ArrayList<>();
				data.stats.add(statsLine);
			}
		}
		
		// Find modifiers (after "Statistik" line)
		int statistikIndex = -1;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i).replaceAll("§[0-9a-fk-or]", "").trim();
			// Remove chinese characters
			line = line.replaceAll("[\\u3400-\\u4DBF]", "").trim();
			if (line.equals("Statistik")) {
				statistikIndex = i;
				break;
			}
		}
		
		if (statistikIndex >= 0) {
			for (int i = statistikIndex + 1; i < lines.size(); i++) {
				String line = lines.get(i).replaceAll("§[0-9a-fk-or]", "").trim();
				// Remove chinese characters
				line = line.replaceAll("[\\u3400-\\u4DBF]", "").trim();
				
				// Skip empty lines
				if (line.isEmpty()) {
					continue;
				}
				
				// Check if line contains modifier format: [MODIFIER]
				// Look for pattern: [ followed by text followed by ]
				if (line.contains("[") && line.contains("]")) {
					// Extract modifier between brackets
					int startBracket = line.indexOf('[');
					int endBracket = line.indexOf(']', startBracket);
					if (startBracket >= 0 && endBracket > startBracket) {
						String modifier = line.substring(startBracket + 1, endBracket).trim();
						if (!modifier.isEmpty()) {
							data.modifiers.add(modifier);
							if (data.modifiers.size() >= 4) {
								break; // Max 4 modifiers
							}
						}
					}
				} else if (line.contains("Benötigt:")) {
					break; // Stop at "Benötigt:"
				}
			}
		}
		
		// Find prices (after "Benötigt:" line)
		int benoetigtIndex = -1;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i).replaceAll("§[0-9a-fk-or]", "").trim();
			// Remove chinese characters
			line = line.replaceAll("[\\u3400-\\u4DBF]", "").trim();
			if (line.contains("Benötigt:")) {
				benoetigtIndex = i;
				break;
			}
		}
		
		if (benoetigtIndex >= 0) {
			parsePriceLines(lines, benoetigtIndex + 1, data, true, false);
		}
		
		// Get floor and rarity from blueprints.json
		ItemInfoBlueprintInfo blueprintInfo = getBlueprintInfo(data.name);
		if (blueprintInfo != null) {
			data.floor = blueprintInfo.floor;
			data.rarity = blueprintInfo.rarity;
		}
		
		// Get aspect info
		data.aspect = checkAspect(data.name, data.rarity);
		
		// Build tags
		data.tags = buildTags(data);
		
		// Blueprint is always true
		data.blueprint = true;
		
		return data;
	}
	
	private static ItemInfoData parseFishingComponent(ItemStack stack, MinecraftClient client, List<String> lines) {
		ItemInfoData data = new ItemInfoData();
		data.id = stack.getItem().toString();
		data.customModelData = extractCustomModelData(stack);
		data.fishingComponent = true;
		
		InformationenUtility.BlueprintNameAndColor nameAndColor =
			InformationenUtility.extractBlueprintNameAndColorFromItemName(stack.getName());
		data.name = extractFishingComponentName(stack, lines);
		if (nameAndColor != null) {
			data.rarity = nameAndColor.rarity;
		}
		
		if (lines.size() > 1) {
			data.type = cleanTooltipLine(lines.get(1));
		}
		
		int modifikatorenIndex = findLineIndex(lines, "Modifikatoren:");
		if (modifikatorenIndex >= 0) {
			for (int i = modifikatorenIndex + 1; i < lines.size(); i++) {
				String line = cleanTooltipLine(lines.get(i));
				if (line.isEmpty()) {
					continue;
				}
				if (line.equals("Kosten:")) {
					break;
				}
				if (line.startsWith("•")) {
					data.modifiers.add(line.substring(1).trim());
				}
			}
		}
		
		int kostenIndex = findLineIndex(lines, "Kosten:");
		if (kostenIndex >= 0) {
			parsePriceLines(lines, kostenIndex + 1, data, false, true);
		}
		
		data.aspect = false;
		data.blueprint = true;
		data.tags = buildFishingComponentTags(data);
		return data;
	}
	
	public static Integer extractCustomModelData(ItemStack stack) {
		try {
			var customModelData = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
			if (customModelData == null) {
				return null;
			}
			
			Integer customModelDataValue = null;
			String componentStr = customModelData.toString();
			
			try {
				java.lang.reflect.Method valueMethod = customModelData.getClass().getMethod("value");
				Object result = valueMethod.invoke(customModelData);
				if (result instanceof Integer) {
					customModelDataValue = (Integer) result;
				}
			} catch (Exception e) {
				// Try alternative methods
			}
			
			if (customModelDataValue == null) {
				try {
					java.lang.reflect.Method getValueMethod = customModelData.getClass().getMethod("getValue");
					Object result = getValueMethod.invoke(customModelData);
					if (result instanceof Integer) {
						customModelDataValue = (Integer) result;
					}
				} catch (Exception e) {
					// Try alternative methods
				}
			}
			
			if (customModelDataValue == null) {
				try {
					java.lang.reflect.Field valueField = customModelData.getClass().getDeclaredField("value");
					valueField.setAccessible(true);
					Object result = valueField.get(customModelData);
					if (result instanceof Integer) {
						customModelDataValue = (Integer) result;
					}
				} catch (Exception e) {
					// Try alternative field names
				}
			}
			
			if (customModelDataValue == null) {
				try {
					java.lang.reflect.Field[] fields = customModelData.getClass().getDeclaredFields();
					for (java.lang.reflect.Field field : fields) {
						field.setAccessible(true);
						Object result = field.get(customModelData);
						
						if (result instanceof java.util.List) {
							java.util.List<?> list = (java.util.List<?>) result;
							if (!list.isEmpty() && (list.get(0) instanceof Float || list.get(0) instanceof Double)) {
								Number firstValue = (Number) list.get(0);
								customModelDataValue = firstValue.intValue();
								break;
							}
						} else if (result != null && result.getClass().isArray()) {
							if (result.getClass().getComponentType() == float.class || result.getClass().getComponentType() == Float.class) {
								float[] floatArray = (float[]) result;
								if (floatArray.length > 0) {
									customModelDataValue = (int) floatArray[0];
									break;
								}
							}
						} else if (result instanceof Integer) {
							customModelDataValue = (Integer) result;
							break;
						}
					}
				} catch (Exception e) {
					// Ignore
				}
			}
			
			if (customModelDataValue == null) {
				try {
					java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("floats=\\[([0-9.]+)\\]");
					java.util.regex.Matcher matcher = pattern.matcher(componentStr);
					if (matcher.find()) {
						double floatValue = Double.parseDouble(matcher.group(1));
						customModelDataValue = (int) floatValue;
					}
				} catch (Exception e) {
					// Ignore
				}
			}
			
			return customModelDataValue;
		} catch (Exception e) {
			return null;
		}
	}
	
	private static void parsePriceLines(List<String> lines, int startIndex, ItemInfoData data, boolean allowBlueprintShop, boolean fishingComponent) {
		boolean foundBlueprintShop = false;
		for (int i = startIndex; i < lines.size(); i++) {
			String line = cleanTooltipLine(lines.get(i));
			if (line.isEmpty()) {
				continue;
			}
			if (line.startsWith("[Linksklick]") || line.startsWith("[Rechtsklick]")) {
				break;
			}
			
			String[] parts = line.split("\\s+/\\s+");
			if (parts.length < 2) {
				continue;
			}
			
			String neededPart = parts[1].trim();
			String[] neededParts = neededPart.split("\\s+", 2);
			if (neededParts.length < 2) {
				continue;
			}
			
			String amountStr = neededParts[0];
			String materialName = cleanTooltipLine(neededParts[1]);
			
			if (allowBlueprintShop && materialName.equals("Coins") && !foundBlueprintShop) {
				if (data.price.containsKey("coin")) {
					foundBlueprintShop = true;
					Map<String, Map<String, Object>> blueprintShopPrice = new LinkedHashMap<>();
					Map<String, Object> coinPrice = new LinkedHashMap<>();
					coinPrice.put("itemName", "Coins");
					coinPrice.put("amount", parseAmount(amountStr));
					blueprintShopPrice.put("coin", coinPrice);
					
					if (i + 1 < lines.size()) {
						String nextLine = cleanTooltipLine(lines.get(i + 1));
						String[] nextParts = nextLine.split("\\s+/\\s+");
						if (nextParts.length >= 2) {
							String nextNeededPart = nextParts[1].trim();
							String[] nextNeededParts = nextNeededPart.split("\\s+", 2);
							if (nextNeededParts.length >= 2 && nextNeededParts[1].equals("Pergamentfetzen")) {
								String paperAmount = nextNeededParts[0];
								Map<String, Object> paperPrice = new LinkedHashMap<>();
								paperPrice.put("itemName", "Pergamentfetzen");
								paperPrice.put("amount", parseAmount(paperAmount));
								blueprintShopPrice.put("paper_shreds", paperPrice);
							}
						}
					}
					
					data.blueprintShopPrice = blueprintShopPrice;
					break;
				} else {
					Map<String, Object> coinPrice = new LinkedHashMap<>();
					coinPrice.put("itemName", "Coins");
					coinPrice.put("amount", parseAmount(amountStr));
					data.price.put("coin", coinPrice);
				}
			} else {
				String key = getPriceKey(materialName, data.price, fishingComponent);
				Map<String, Object> materialPrice = new LinkedHashMap<>();
				materialPrice.put("itemName", materialName);
				materialPrice.put("amount", parseAmount(amountStr));
				data.price.put(key, materialPrice);
			}
		}
	}
	
	private static String parseAmount(String amountStr) {
		// Replace commas with dots in number values (e.g., "473,122ag" -> "473.122ag")
		// Keep letters and other characters, just replace comma separators
		return amountStr.trim().replace(",", ".");
	}
	
	public static boolean isFishingRarityMaterial(String materialName) {
		return materialName != null && FISHING_RARITY_MATERIAL_PATTERN.matcher(materialName).matches();
	}
	
	private static String nextMaterialKey(Map<String, Map<String, Object>> priceMap) {
		int materialNum = 1;
		while (priceMap.containsKey("material" + materialNum)) {
			materialNum++;
		}
		return "material" + materialNum;
	}
	
	private static String getPriceKey(String materialName, Map<String, Map<String, Object>> priceMap, boolean fishingComponent) {
		if (materialName.equals("Coins")) {
			return "coin";
		}
		if (materialName.equals("Kaktus")) {
			return "cactus";
		}
		if (materialName.equals("Seelen")) {
			return "soul";
		}
		if (AMBOSS_ITEMS.contains(materialName)) {
			return "Amboss";
		}
		if (fishingComponent) {
			if (isFishingRarityMaterial(materialName) || aincraftMaterials.contains(materialName)) {
				return nextMaterialKey(priceMap);
			}
			return "Ressource";
		}
		if (aincraftMaterials.contains(materialName) || isFishingRarityMaterial(materialName)) {
			return nextMaterialKey(priceMap);
		}
		return "Ressource";
	}
	
	private static ItemInfoBlueprintInfo getBlueprintInfo(String blueprintName) {
		if (blueprintsData == null || blueprintName == null) {
			return null;
		}
		
		try {
			JsonObject floors = blueprintsData.getAsJsonObject("floors");
			if (floors == null) {
				return null;
			}
			
			for (String floorKey : floors.keySet()) {
				JsonObject floorData = floors.getAsJsonObject(floorKey);
				JsonObject blueprints = floorData.getAsJsonObject("blueprints");
				if (blueprints == null) {
					continue;
				}
				
				for (String rarity : blueprints.keySet()) {
					JsonObject rarityData = blueprints.getAsJsonObject(rarity);
					JsonArray items = rarityData.getAsJsonArray("items");
					if (items == null) {
						continue;
					}
					
					for (int i = 0; i < items.size(); i++) {
						String itemName = items.get(i).getAsString();
						if (itemName.equals(blueprintName)) {
							ItemInfoBlueprintInfo info = new ItemInfoBlueprintInfo();
							info.floor = floorKey.replace("floor_", "Ebene ");
							info.rarity = rarity;
							return info;
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("[ItemInfoUtility] Fehler beim Lesen von blueprints.json: " + e.getMessage());
		}
		
		return null;
	}
	
	private static boolean checkAspect(String blueprintName, String rarity) {
		// Only legendary can have aspects
		if (!"legendary".equals(rarity)) {
			return false;
		}
		
		if (aspectsData == null || blueprintName == null) {
			return false;
		}
		
		try {
			if (!aspectsData.has(blueprintName)) {
				return false;
			}
			
			JsonObject blueprintAspect = aspectsData.getAsJsonObject(blueprintName);
			if (blueprintAspect == null) {
				return false;
			}
			
			String aspectDescription = blueprintAspect.has("aspect_description") 
				? blueprintAspect.get("aspect_description").getAsString() 
				: "";
			
			// If description contains "Kein Aspekt auf diesem Bauplan", return false
			if (aspectDescription.contains("Kein Aspekt auf diesem Bauplan")) {
				return false;
			}
			
			return true;
		} catch (Exception e) {
			// Silent error handling("[ItemInfoUtility] Fehler beim Lesen von Aspekte.json: " + e.getMessage());
			return false;
		}
	}
	
	private static List<String> buildFishingComponentTags(ItemInfoData data) {
		List<String> tags = new ArrayList<>();
		tags.add("bauplan");
		if (data.type != null && !data.type.isEmpty()) {
			tags.add(data.type.toLowerCase());
		}
		tags.add("angel");
		tags.add("komponente");
		if (data.rarity != null && !data.rarity.isEmpty()) {
			tags.add(data.rarity);
		}
		return tags;
	}
	
	private static List<String> buildTags(ItemInfoData data) {
		List<String> tags = new ArrayList<>();
		
		// Always add "bauplan"
		tags.add("bauplan");
		
		// Add "rüstung" if type contains "Platte", "Leder", or "Accessoire"
		if (data.type != null && (data.type.contains("Platte") || data.type.contains("Leder") || data.type.contains("Accessoire"))) {
			tags.add("rüstung");
		}
		
		// Add rarity
		if (data.rarity != null) {
			tags.add(data.rarity);
		}
		
		// Add aspect if true
		if (data.aspect) {
			tags.add("aspekt");
		}
		
		// Add piece if present
		if (data.piece != null && !data.piece.isEmpty()) {
			tags.add(data.piece.toLowerCase());
		}
		
		return tags;
	}
	
	private static List<Text> getItemTooltip(ItemStack itemStack, net.minecraft.entity.player.PlayerEntity player) {
		List<Text> tooltip = new ArrayList<>();
		// Add item name
		tooltip.add(itemStack.getName());
		
		// Read lore from Data Component API (1.21.7)
		var loreComponent = itemStack.get(DataComponentTypes.LORE);
		if (loreComponent != null) {
			tooltip.addAll(loreComponent.lines());
		}
		return tooltip;
	}
	
	private static void loadBlueprintsData() {
		try {
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(BLUEPRINTS_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Blueprints config file not found"));
			
			try (var inputStream = Files.newInputStream(resource)) {
				try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
					blueprintsData = JsonParser.parseReader(reader).getAsJsonObject();
				}
			}
		} catch (Exception e) {
			// Silent error handling("[ItemInfoUtility] ❌ Fehler beim Laden von blueprints.json: " + e.getMessage());
			// Silent error handling
		}
	}
	
	private static void loadAspectsData() {
		try {
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(ASPECTS_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Aspects config file not found"));
			
			try (var inputStream = Files.newInputStream(resource)) {
				try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
					aspectsData = JsonParser.parseReader(reader).getAsJsonObject();
				}
			}
		} catch (Exception e) {
			// Silent error handling("[ItemInfoUtility] ❌ Fehler beim Laden von Aspekte.json: " + e.getMessage());
			// Silent error handling
		}
	}
	
	private static void loadAincraftData() {
		try {
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(AINCRAFT_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Aincraft config file not found"));
			
			try (var inputStream = Files.newInputStream(resource)) {
				try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
					aincraftData = JsonParser.parseReader(reader).getAsJsonObject();
					
					// Extract all materials from all floors
					aincraftMaterials.clear();
					if (aincraftData.has("floors")) {
						JsonObject floors = aincraftData.getAsJsonObject("floors");
						for (String floorKey : floors.keySet()) {
							JsonObject floor = floors.getAsJsonObject(floorKey);
							if (floor.has("materials")) {
								JsonObject materials = floor.getAsJsonObject("materials");
								for (String rarityKey : materials.keySet()) {
									JsonObject rarity = materials.getAsJsonObject(rarityKey);
									if (rarity.has("materials")) {
										JsonArray materialArray = rarity.getAsJsonArray("materials");
										for (int i = 0; i < materialArray.size(); i++) {
											String materialName = materialArray.get(i).getAsString();
											if (!materialName.isEmpty()) {
												aincraftMaterials.add(materialName);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("[ItemInfoUtility] ❌ Fehler beim Laden von Aincraft.json: " + e.getMessage());
			// Silent error handling
		}
	}

	private static void loadFarmworldData() {
		try {
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(FARMWORLD_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Farmworld config file not found"));

			try (var inputStream = Files.newInputStream(resource)) {
				try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
					farmworldData = JsonParser.parseReader(reader).getAsJsonObject();

					if (farmworldData.has("ponds")) {
						JsonArray pondsArray = farmworldData.getAsJsonArray("ponds");
						for (var element : pondsArray) {
							JsonObject pondData = element.getAsJsonObject();
							if (!pondData.has("materials")) {
								continue;
							}
							JsonObject materials = pondData.getAsJsonObject("materials");
							for (String rarityKey : materials.keySet()) {
								JsonObject rarity = materials.getAsJsonObject(rarityKey);
								if (rarity.has("materials")) {
									JsonArray materialArray = rarity.getAsJsonArray("materials");
									for (int i = 0; i < materialArray.size(); i++) {
										String materialName = materialArray.get(i).getAsString();
										if (!materialName.isEmpty()) {
											aincraftMaterials.add(materialName);
										}
									}
								}
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
	 * Data class to store material floor and rarity information
	 */
	public static class MaterialFloorInfo {
		public final int floor;
		public final String pond;
		public final String rarity;
		public final String color;
		
		public MaterialFloorInfo(int floor, String rarity, String color) {
			this(floor, null, rarity, color);
		}
		
		public MaterialFloorInfo(int floor, String pond, String rarity, String color) {
			this.floor = floor;
			this.pond = pond;
			this.rarity = rarity;
			this.color = color;
		}
	}
	
	/**
	 * Finds the floor and rarity information for a given material name
	 * @param materialName Name of the material
	 * @return MaterialFloorInfo with floor number, rarity, and color, or null if not found
	 */
	public static MaterialFloorInfo getMaterialFloorInfo(String materialName) {
		if (materialName == null || (aincraftData == null && farmworldData == null)) {
			return null;
		}
		
		try {
			MaterialFloorInfo pondInfo = findMaterialInPonds(materialName);
			if (pondInfo != null) {
				return pondInfo;
			}
			if (aincraftData.has("floors")) {
				JsonObject floors = aincraftData.getAsJsonObject("floors");
				for (String floorKey : floors.keySet()) {
					// Extract floor number from "floor_1", "floor_2", etc.
					int floorNumber = 0;
					try {
						String floorNumStr = floorKey.replace("floor_", "");
						floorNumber = Integer.parseInt(floorNumStr);
					} catch (NumberFormatException e) {
						continue;
					}
					
					JsonObject floor = floors.getAsJsonObject(floorKey);
					if (floor.has("materials")) {
						JsonObject materials = floor.getAsJsonObject("materials");
						for (String rarityKey : materials.keySet()) {
							JsonObject rarity = materials.getAsJsonObject(rarityKey);
							if (rarity.has("materials")) {
								JsonArray materialArray = rarity.getAsJsonArray("materials");
								for (int i = 0; i < materialArray.size(); i++) {
									String materialNameFromJson = materialArray.get(i).getAsString();
									if (materialNameFromJson.equals(materialName)) {
										// Found the material, get color
										String color = rarity.has("color") ? rarity.get("color").getAsString() : "WHITE";
										return new MaterialFloorInfo(floorNumber, rarityKey, color);
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
		
		return null;
	}
	
	private static MaterialFloorInfo findMaterialInPonds(String materialName) {
		if (farmworldData == null || !farmworldData.has("ponds")) {
			return null;
		}
		JsonArray pondsArray = farmworldData.getAsJsonArray("ponds");
		for (var element : pondsArray) {
			JsonObject pondData = element.getAsJsonObject();
			String pondName = pondData.has("pond") ? pondData.get("pond").getAsString() : "";
			if (pondName.isEmpty() || !pondData.has("materials")) {
				continue;
			}
			JsonObject materials = pondData.getAsJsonObject("materials");
			for (String rarityKey : materials.keySet()) {
				JsonObject rarity = materials.getAsJsonObject(rarityKey);
				if (!rarity.has("materials")) {
					continue;
				}
				JsonArray materialArray = rarity.getAsJsonArray("materials");
				for (int i = 0; i < materialArray.size(); i++) {
					if (materialName.equals(materialArray.get(i).getAsString())) {
						String color = rarity.has("color") ? rarity.get("color").getAsString() : "WHITE";
						return new MaterialFloorInfo(0, pondName, rarityKey, color);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Converts a color string (WHITE, GREEN, BLUE, PURPLE, GOLD) to an integer color value
	 * @param colorString Color string from Aincraft.json
	 * @return Integer color value (0xFFFFFFFF format)
	 */
	public static int getRarityColorFromString(String colorString) {
		if (colorString == null) {
			return 0xFFFFFFFF; // Default to white
		}
		
		switch (colorString.toUpperCase()) {
			case "WHITE":
				return 0xFFFFFFFF;
			case "GREEN":
				return 0xFF55FF55;
			case "BLUE":
				return 0xFF5555FF;
			case "PURPLE":
				return 0xFFAA00AA;
			case "GOLD":
				return 0xFFFFAA00;
			default:
				return 0xFFFFFFFF; // Default to white
		}
	}
	
	private static void loadRegisteredItems() {
		registeredItemNames.clear();
		registeredItemNames.addAll(ExtractedItemsStorage.loadRegisteredItemNames());
	}
	
	/**
	 * Checks if an item is already registered in extracted_items.json
	 */
	public static boolean isItemRegistered(ItemStack stack, MinecraftClient client) {
		if (stack == null || stack.isEmpty() || client == null || client.player == null) {
			return false;
		}
		
		try {
			// Get item name from tooltip (same way as in parseItem)
			List<Text> tooltip = getItemTooltip(stack, client.player);
			if (tooltip == null || tooltip.isEmpty()) {
				return false;
			}
			
			String name = resolveItemDisplayName(stack, client);
			if (!name.isEmpty()) {
				return registeredItemNames.contains(name);
			}
		} catch (Exception e) {
			// Ignore errors
		}
		
		return false;
	}
	
	// Track recently extracted items (to show green background)
	private static Set<String> recentlyExtractedItems = new HashSet<>();
	
	/**
	 * Marks an item as recently extracted (for green background display)
	 */
	public static void markItemAsExtracted(String itemName) {
		if (itemName != null && !itemName.isEmpty()) {
			recentlyExtractedItems.add(itemName);
			// Remove from set after 3 seconds (to show green briefly)
			new Thread(() -> {
				try {
					Thread.sleep(3000);
					recentlyExtractedItems.remove(itemName);
				} catch (InterruptedException e) {
					// Ignore
				}
			}).start();
		}
	}
	
	/**
	 * Renders red/green background overlay on items in target slots
	 * Called from HandledScreenMixin
	 */
	public static void renderUnregisteredItemOverlays(DrawContext context, HandledScreen<?> screen, int screenX, int screenY) {
		if (!ENABLED || !isInitialized) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.player == null) {
			return;
		}
		
		if (!isSupportedInventory(screen)) {
			return;
		}
		
		int[] targetSlots = getTargetSlotsForScreen(screen);
		
		// Render background on items in target slots
		for (int slotIndex : targetSlots) {
			if (slotIndex >= screen.getScreenHandler().slots.size()) {
				continue;
			}
			
			Slot slot = screen.getScreenHandler().slots.get(slotIndex);
			if (slot.hasStack()) {
				ItemStack stack = slot.getStack();
				if (stack != null && !stack.isEmpty()) {
					String itemName = resolveItemDisplayName(stack, client);
					
					if (itemName == null || itemName.isEmpty()) {
						continue; // Skip if we can't get the name
					}
					
					// Check if item is registered
					boolean isRegistered = registeredItemNames.contains(itemName);
					
					// If registered, don't draw any background
					if (isRegistered) {
						continue;
					}
					
					boolean hasKaufkosten = isExtractableItem(stack, client);
					
					// Calculate slot position
					int slotX = screenX + slot.x;
					int slotY = screenY + slot.y;
					
					if (hasKaufkosten) {
						// Draw green semi-transparent background (0x8000FF00 = 50% transparent green)
						context.fill(slotX, slotY, slotX + 18, slotY + 18, 0x8000FF00);
					} else {
						// Draw red semi-transparent background (0x80FF0000 = 50% transparent red)
						context.fill(slotX, slotY, slotX + 18, slotY + 18, 0x80FF0000);
					}
				}
			}
		}
		
	}
	
	private static void writeItemsToFile(List<ItemInfoData> items, MinecraftClient client) {
		try {
			ExtractedItemsStorage.WriteResult result = ExtractedItemsStorage.appendItems(items);
			
			for (ItemInfoData item : items) {
				if (item.name != null && !item.name.isEmpty()) {
					markItemAsExtracted(item.name);
				}
			}
			
			loadRegisteredItems();
			
			if (result.total() > 0) {
				if (result.newFishingCount > 0 && result.newBlueprintCount > 0) {
					client.player.sendMessage(Text.literal(
						"§a[ItemInfo] §f" + result.newBlueprintCount + " Baupläne + " + result.newFishingCount + " Angel-Komponenten hinzugefügt!"
					), false);
				} else if (result.newFishingCount > 0) {
					client.player.sendMessage(Text.literal(
						"§a[ItemInfo] §f" + result.newFishingCount + " Angel-Komponenten in fishing_components hinzugefügt!"
					), false);
				} else {
					client.player.sendMessage(Text.literal(
						"§a[ItemInfo] §f" + result.newBlueprintCount + " Baupläne in blueprints hinzugefügt!"
					), false);
				}
			} else {
				client.player.sendMessage(Text.literal("§e[ItemInfo] §fAlle Items waren bereits vorhanden."), false);
			}
			client.player.sendMessage(Text.literal("§7Datei: §f" + ExtractedItemsStorage.getOutputFile().getName()), false);
		} catch (Exception e) {
			if (client.player != null) {
				client.player.sendMessage(Text.literal("§c[ItemInfo] Fehler beim Speichern der Datei!"), false);
				client.player.sendMessage(Text.literal("§7Fehler: §f" + e.getMessage()), false);
			}
		}
	}
}


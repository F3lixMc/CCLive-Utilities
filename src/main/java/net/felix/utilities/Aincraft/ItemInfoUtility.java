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
import net.felix.utilities.Overall.ZeichenUtility;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class ItemInfoUtility {
	
	private static boolean isInitialized = false;
	private static KeyBinding extractKeyBinding;
	
	// JSON file paths
	private static final String BLUEPRINTS_CONFIG_FILE = "assets/cclive-utilities/blueprints.json";
	private static final String ASPECTS_CONFIG_FILE = "assets/cclive-utilities/Aspekte.json";
	private static final String AINCRAFT_CONFIG_FILE = "assets/cclive-utilities/Aincraft.json";
	
	// Cache for loaded JSON data
	private static JsonObject blueprintsData = null;
	private static JsonObject aspectsData = null;
	private static JsonObject aincraftData = null;
	
	// Set of registered item names (from extracted_items.json)
	private static Set<String> registeredItemNames = new HashSet<>();
	
	// Set of all materials from Aincraft.json
	private static Set<String> aincraftMaterials = new HashSet<>();
	
	// Queue for auto-clicking items (to spread clicks over multiple frames)
	private static final Queue<Integer> pendingClicks = new LinkedList<>();
	private static final int clicksPerFrame = 2; // Number of clicks per frame to avoid lag
	
	// Set of Amboss items
	private static final Set<String> AMBOSS_ITEMS = Set.of(
		"Spinnfaden", "Stoff", "Ziegelstein", "Kettenglied", "Leder", "Zahnrad", 
		"Klammer", "Schraube", "Stabile Schnur", "Kupferplatten", "Gehärteter Ziegelstein", 
		"Eisenplatten", "Verstärkte Schraube", "Verbessertes Zahnrad"
	);
	
	// Target slots: 10-16, 19-25, 28-34, 37-43
	private static final int[] TARGET_SLOTS = {
		10, 11, 12, 13, 14, 15, 16,
		19, 20, 21, 22, 23, 24, 25,
		28, 29, 30, 31, 32, 33, 34,
		37, 38, 39, 40, 41, 42, 43
	};
	
	public static void initialize() {
		if (isInitialized) {
			return;
		}
		
		try {
			// Load JSON data
			loadBlueprintsData();
			loadAspectsData();
			loadAincraftData();
			
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
			"category.cclive-utilities.item-info"
		));
		
		// Register auto-click hotkey
		autoClickKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.cclive-utilities.item-info-auto-click",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(), // Unbound by default
			"category.cclive-utilities.item-info"
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
			if (extractKeyBinding.matchesKey(keyCode, -1)) {
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
			if (autoClickKeyBinding.matchesKey(keyCode, -1)) {
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
		for (int slotIndex : TARGET_SLOTS) {
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
		List<ItemData> items = new ArrayList<>();
		
		// Read items from target slots
		for (int slotIndex : TARGET_SLOTS) {
			if (slotIndex >= screen.getScreenHandler().slots.size()) {
				continue;
			}
			
			Slot slot = screen.getScreenHandler().slots.get(slotIndex);
			if (slot.hasStack()) {
				ItemStack stack = slot.getStack();
				if (stack != null && !stack.isEmpty()) {
					// Check if item contains "[Rechtsklick] Kaufkosten" before parsing
					if (hasKaufkostenLine(stack, client)) {
						ItemData itemData = parseItem(stack, client);
						if (itemData != null) {
							items.add(itemData);
						}
					}
				}
			}
		}
		
		if (items.isEmpty()) {
			client.player.sendMessage(Text.literal("§c[ItemInfo] Keine Items in den angegebenen Slots gefunden!"), false);
			return;
		}
		
		// Write to file
		writeItemsToFile(items, client);
	}
	
	/**
	 * Checks if the item tooltip contains the "[Rechtsklick] Kaufkosten" line
	 * This is required for items to be extracted
	 */
	private static boolean hasKaufkostenLine(ItemStack stack, MinecraftClient client) {
		try {
			List<Text> tooltip = getItemTooltip(stack, client.player);
			if (tooltip == null || tooltip.isEmpty()) {
				return false;
			}
			
			// Check all tooltip lines for "[Rechtsklick] Kaufkosten"
			for (Text line : tooltip) {
				String text = line.getString();
				if (text != null) {
					// Remove formatting codes
					text = text.replaceAll("§[0-9a-fk-or]", "").trim();
					// Remove chinese characters
					text = text.replaceAll("[\\u3400-\\u4DBF]", "").trim();
					// Check if line contains "[Rechtsklick] Kaufkosten" (not "Baukosten")
					if (text.contains("[Rechtsklick] Kaufkosten")) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			// Ignore errors, return false
		}
		
		return false;
	}
	
	private static ItemData parseItem(ItemStack stack, MinecraftClient client) {
		// Get tooltip
		List<Text> tooltip = getItemTooltip(stack, client.player);
		if (tooltip == null || tooltip.isEmpty()) {
			return null;
		}
		
		// Convert tooltip to string lines and filter chinese characters
		List<String> lines = new ArrayList<>();
		for (Text line : tooltip) {
			String text = line.getString();
			if (text != null && !text.trim().isEmpty()) {
				// Remove chinese characters (Unicode range U+3400 to U+4DBF)
				text = text.replaceAll("[\\u3400-\\u4DBF]", "");
				// Skip lines that only contain chinese pixel spacers
				if (ZeichenUtility.containsPixelSpacer(text)) {
					continue;
				}
				// Skip empty lines after removing chinese characters
				if (text.trim().isEmpty()) {
					continue;
				}
				lines.add(text);
			}
		}
		
		if (lines.isEmpty()) {
			return null;
		}
		
		ItemData data = new ItemData();
		
		// Extract item ID and customModelData
		String itemId = stack.getItem().toString();
		data.id = itemId;
		
		// Try to get customModelData from DataComponentTypes (1.21.7 API)
		try {
			var customModelData = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
			if (customModelData != null) {
				Integer customModelDataValue = null;
				
				String componentStr = customModelData.toString();
				
				// Method 1: Try value() method (most common)
				try {
					java.lang.reflect.Method valueMethod = customModelData.getClass().getMethod("value");
					Object result = valueMethod.invoke(customModelData);
					if (result instanceof Integer) {
						customModelDataValue = (Integer) result;
					}
				} catch (Exception e) {
					// Try alternative methods
				}
				
				// Method 2: Try getValue() method
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
				
				// Method 3: Try direct field access (value field)
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
				
				// Method 4: Try to get floats array and use first value
				if (customModelDataValue == null) {
					try {
						// Try to get floats field/array
						java.lang.reflect.Field[] fields = customModelData.getClass().getDeclaredFields();
						for (java.lang.reflect.Field field : fields) {
							field.setAccessible(true);
							Object result = field.get(customModelData);
							
							// Check if it's a floats array (List<Float> or float[])
							if (result instanceof java.util.List) {
								java.util.List<?> list = (java.util.List<?>) result;
								if (!list.isEmpty() && (list.get(0) instanceof Float || list.get(0) instanceof Double)) {
									Number firstValue = (Number) list.get(0);
									customModelDataValue = firstValue.intValue();
									break;
								}
							} else if (result != null && result.getClass().isArray()) {
								// Check if it's a float array
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
				
				// Method 5: Try toString() and parse floats array (last resort)
				if (customModelDataValue == null) {
					try {
						// Try to extract number from string like "class_9280[floats=[100.0], ...]"
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
				
				// Set the value if found
				if (customModelDataValue != null) {
					data.customModelData = customModelDataValue;
				}
			}
		} catch (Exception e) {
			// Ignore - customModelData is optional
		}
		
		// Extract name (first line, remove "- [Bauplan]")
		if (!lines.isEmpty()) {
			String name = lines.get(0).replaceAll("§[0-9a-fk-or]", "").trim();
			// Remove chinese characters
			name = name.replaceAll("[\\u3400-\\u4DBF]", "").trim();
			name = name.replaceAll(" - \\[Bauplan\\]", "").trim();
			data.name = name;
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
			boolean foundBlueprintShop = false;
			for (int i = benoetigtIndex + 1; i < lines.size(); i++) {
				String line = lines.get(i).replaceAll("§[0-9a-fk-or]", "").trim();
				// Remove chinese characters
				line = line.replaceAll("[\\u3400-\\u4DBF]", "").trim();
				if (line.isEmpty()) {
					continue;
				}
				
				// Parse price line: "EIGENE / BENÖTIGTE Material"
				// Format: "X / Y Material" or "X / Y.XXXk Material"
				String[] parts = line.split("\\s+/\\s+");
				if (parts.length >= 2) {
					String neededPart = parts[1].trim();
					String[] neededParts = neededPart.split("\\s+", 2);
					if (neededParts.length >= 2) {
						String amountStr = neededParts[0];
						String materialName = neededParts[1];
						// Remove chinese characters from material name
						materialName = materialName.replaceAll("[\\u3400-\\u4DBF]", "").trim();
						
						// Check if this is blueprint_shop (second occurrence of "Coins")
						if (materialName.equals("Coins") && !foundBlueprintShop) {
							// Check if we already have a coin entry
							if (data.price.containsKey("coin")) {
								foundBlueprintShop = true;
								// This is blueprint_shop
								Map<String, Map<String, Object>> blueprintShopPrice = new LinkedHashMap<>();
								Map<String, Object> coinPrice = new LinkedHashMap<>();
								coinPrice.put("itemName", "Coins");
								coinPrice.put("amount", parseAmount(amountStr));
								blueprintShopPrice.put("coin", coinPrice);
								
								// Next line should be Pergamentfetzen
								if (i + 1 < lines.size()) {
									String nextLine = lines.get(i + 1).replaceAll("§[0-9a-fk-or]", "").trim();
									// Remove chinese characters
									nextLine = nextLine.replaceAll("[\\u3400-\\u4DBF]", "").trim();
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
								// First occurrence, regular price
								Map<String, Object> coinPrice = new LinkedHashMap<>();
								coinPrice.put("itemName", "Coins");
								coinPrice.put("amount", parseAmount(amountStr));
								data.price.put("coin", coinPrice);
							}
						} else {
							// Regular material
							String key = getPriceKey(materialName, data.price);
							Map<String, Object> materialPrice = new LinkedHashMap<>();
							materialPrice.put("itemName", materialName);
							materialPrice.put("amount", parseAmount(amountStr));
							data.price.put(key, materialPrice);
						}
					}
				}
			}
		}
		
		// Get floor and rarity from blueprints.json
		BlueprintInfo blueprintInfo = getBlueprintInfo(data.name);
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
	
	private static String parseAmount(String amountStr) {
		// Replace commas with dots in number values (e.g., "473,122ag" -> "473.122ag")
		// Keep letters and other characters, just replace comma separators
		return amountStr.trim().replace(",", ".");
	}
	
	private static String getPriceKey(String materialName, Map<String, Map<String, Object>> priceMap) {
		// Map common material names to keys
		if (materialName.equals("Coins")) {
			return "coin";
		} else if (materialName.equals("Kaktus")) {
			return "cactus";
		} else if (materialName.equals("Seelen")) {
			return "soul";
		} else if (AMBOSS_ITEMS.contains(materialName)) {
			// Amboss items: just "Amboss" (overwrites if multiple)
			return "Amboss";
		} else if (aincraftMaterials.contains(materialName)) {
			// Materials from Aincraft.json: material1, material2, etc.
			int materialNum = 1;
			while (priceMap.containsKey("material" + materialNum)) {
				materialNum++;
			}
			return "material" + materialNum;
		} else {
			// Everything else: just "Ressource" (overwrites if multiple)
			return "Ressource";
		}
	}
	
	private static BlueprintInfo getBlueprintInfo(String blueprintName) {
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
							BlueprintInfo info = new BlueprintInfo();
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
	
	private static List<String> buildTags(ItemData data) {
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
			// Silent error handling("[ItemInfoUtility] ❌ Fehler beim Laden von Aincraft.json: " + e.getMessage());
			// Silent error handling
		}
	}
	
	/**
	 * Loads registered item names from extracted_items.json
	 */
	private static void loadRegisteredItems() {
		registeredItemNames.clear();
		try {
			File outputDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "cclive-utilities");
			File extractedItemsFile = new File(outputDir, "extracted_items.json");
			
			if (extractedItemsFile.exists()) {
				try (FileReader reader = new FileReader(extractedItemsFile, StandardCharsets.UTF_8)) {
					JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();
					for (int i = 0; i < jsonArray.size(); i++) {
						JsonObject item = jsonArray.get(i).getAsJsonObject();
						if (item.has("name")) {
							String name = item.get("name").getAsString();
							if (name != null && !name.isEmpty()) {
								registeredItemNames.add(name);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// Ignore errors - file might not exist yet
		}
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
			
			// Extract name from first line
			String name = tooltip.get(0).getString();
			if (name != null) {
				// Remove formatting codes and chinese characters
				name = name.replaceAll("§[0-9a-fk-or]", "").trim();
				name = name.replaceAll("[\\u3400-\\u4DBF]", "").trim();
				name = name.replaceAll(" - \\[Bauplan\\]", "").trim();
				
				// Check if name is in registered items
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
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.player == null) {
			return;
		}
		
		// Check if we're in a blueprint shop inventory (where items with "[Rechtsklick] Kaufkosten" appear)
		String title = screen.getTitle().getString();
		String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\u3400-\\u4DBF]", "");
		
		// Debug: Check if we're in the right inventory
		boolean isBlueprintShop = cleanTitle.contains("Bauplan [Shop]") || cleanTitle.contains("Blueprint Store");
		if (!isBlueprintShop) {
			return;
		}
		
		// Render background on items in target slots
		for (int slotIndex : TARGET_SLOTS) {
			if (slotIndex >= screen.getScreenHandler().slots.size()) {
				continue;
			}
			
			Slot slot = screen.getScreenHandler().slots.get(slotIndex);
			if (slot.hasStack()) {
				ItemStack stack = slot.getStack();
				if (stack != null && !stack.isEmpty()) {
					// Get item name for checking
					String itemName = null;
					try {
						// Use getName() which is always available
						Text nameText = stack.getName();
						if (nameText != null) {
							itemName = nameText.getString();
							if (itemName != null) {
								itemName = itemName.replaceAll("§[0-9a-fk-or]", "").trim();
								itemName = itemName.replaceAll("[\\u3400-\\u4DBF]", "").trim();
								itemName = itemName.replaceAll(" - \\[Bauplan\\]", "").trim();
							}
						}
					} catch (Exception e) {
						// Ignore
					}
					
					if (itemName == null || itemName.isEmpty()) {
						continue; // Skip if we can't get the name
					}
					
					// Check if item is registered
					boolean isRegistered = registeredItemNames.contains(itemName);
					
					// If registered, don't draw any background
					if (isRegistered) {
						continue;
					}
					
					// Check if item has "[Rechtsklick] Kaufkosten" in tooltip
					boolean hasKaufkosten = hasKaufkostenLine(stack, client);
					
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
	
	private static void writeItemsToFile(List<ItemData> items, MinecraftClient client) {
		try {
			// Create output directory
			File outputDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "cclive-utilities");
			outputDir.mkdirs();
			
			// Create output file
			File outputFile = new File(outputDir, "extracted_items.json");
			
			// Load existing items from file (if it exists)
			JsonArray existingArray = new JsonArray();
			Set<String> existingItemNames = new HashSet<>();
			if (outputFile.exists()) {
				try (FileReader reader = new FileReader(outputFile, StandardCharsets.UTF_8)) {
					existingArray = JsonParser.parseReader(reader).getAsJsonArray();
					// Collect existing item names to avoid duplicates
					for (int i = 0; i < existingArray.size(); i++) {
						JsonObject existingItem = existingArray.get(i).getAsJsonObject();
						if (existingItem.has("name")) {
							String name = existingItem.get("name").getAsString();
							if (name != null && !name.isEmpty()) {
								existingItemNames.add(name);
							}
						}
					}
				} catch (Exception e) {
					// Silent error handling("[ItemInfoUtility] Fehler beim Lesen der bestehenden Datei: " + e.getMessage());
					// If reading fails, start with empty array
					existingArray = new JsonArray();
				}
			}
			
			// Build JSON array starting with existing items
			JsonArray jsonArray = new JsonArray();
			// Add all existing items first
			for (int i = 0; i < existingArray.size(); i++) {
				jsonArray.add(existingArray.get(i));
			}
			
			// Add new items (only if they don't already exist)
			int newItemsCount = 0;
			for (ItemData item : items) {
				// Check if item already exists by name
				if (item.name != null && !item.name.isEmpty() && existingItemNames.contains(item.name)) {
					continue; // Skip duplicate
				}
				
				// Add new item
				newItemsCount++;
				JsonObject jsonItem = new JsonObject();
				
				jsonItem.addProperty("id", item.id);
				if (item.customModelData != null) {
					jsonItem.addProperty("customModelData", item.customModelData);
				}
				jsonItem.addProperty("name", item.name);
				
				// foundAt
				JsonArray foundAtArray = new JsonArray();
				JsonObject foundAtObj = new JsonObject();
				foundAtObj.addProperty("floor", item.floor != null ? item.floor : "");
				foundAtArray.add(foundAtObj);
				jsonItem.add("foundAt", foundAtArray);
				
				// price
				JsonObject priceObj = new JsonObject();
				for (Map.Entry<String, Map<String, Object>> entry : item.price.entrySet()) {
					JsonObject priceItem = new JsonObject();
					Map<String, Object> priceData = entry.getValue();
					priceItem.addProperty("itemName", (String) priceData.get("itemName"));
					Object amount = priceData.get("amount");
					if (amount instanceof Number) {
						priceItem.addProperty("amount", ((Number) amount).intValue());
					} else {
						priceItem.addProperty("amount", amount.toString());
					}
					priceObj.add(entry.getKey(), priceItem);
				}
				jsonItem.add("price", priceObj);
				
				// blueprint_shop
				if (item.blueprintShopPrice != null && !item.blueprintShopPrice.isEmpty()) {
					JsonObject blueprintShopObj = new JsonObject();
					JsonObject blueprintShopPriceObj = new JsonObject();
					for (Map.Entry<String, Map<String, Object>> entry : item.blueprintShopPrice.entrySet()) {
						JsonObject priceItem = new JsonObject();
						Map<String, Object> priceData = entry.getValue();
						priceItem.addProperty("itemName", (String) priceData.get("itemName"));
						Object amount = priceData.get("amount");
						if (amount instanceof Number) {
							priceItem.addProperty("amount", ((Number) amount).intValue());
						} else {
							priceItem.addProperty("amount", amount.toString());
						}
						blueprintShopPriceObj.add(entry.getKey(), priceItem);
					}
					blueprintShopObj.add("price", blueprintShopPriceObj);
					jsonItem.add("blueprint_shop", blueprintShopObj);
				}
				
				// info
				JsonObject infoObj = new JsonObject();
				infoObj.addProperty("aspect", item.aspect);
				infoObj.addProperty("rarity", item.rarity != null ? item.rarity : "");
				infoObj.addProperty("description", "");
				infoObj.addProperty("type", item.type != null ? item.type : "");
				infoObj.addProperty("piece", item.piece != null ? item.piece : "");
				// modifier: empty array if no modifiers, otherwise array
				JsonArray modifierArray = new JsonArray();
				if (item.modifiers != null && !item.modifiers.isEmpty()) {
					for (String modifier : item.modifiers) {
						modifierArray.add(modifier);
					}
				}
				infoObj.add("modifier", modifierArray);
				// stats as array
				if (item.stats != null && !item.stats.isEmpty()) {
					JsonArray statsArray = new JsonArray();
					for (String stat : item.stats) {
						statsArray.add(stat);
					}
					infoObj.add("stats", statsArray);
				} else {
					infoObj.add("stats", new JsonArray());
				}
				infoObj.addProperty("blueprint", item.blueprint);
				infoObj.addProperty("module", false);
				infoObj.addProperty("ability", false);
				infoObj.addProperty("rune", false);
				infoObj.addProperty("power_crystal", false);
				infoObj.addProperty("essence", false);
				jsonItem.add("info", infoObj);
				
				// tags
				JsonArray tagsArray = new JsonArray();
				for (String tag : item.tags) {
					tagsArray.add(tag);
				}
				jsonItem.add("tags", tagsArray);
				
				jsonArray.add(jsonItem);
			}
			
			// Write to file
			try (FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
				com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
				gson.toJson(jsonArray, writer);
			}
			
			// Mark items as recently extracted (for green background)
			for (ItemData item : items) {
				if (item.name != null && !item.name.isEmpty()) {
					markItemAsExtracted(item.name);
				}
			}
			
			// Reload registered items after writing
			loadRegisteredItems();
			
			// Send success message to chat
			if (newItemsCount > 0) {
				client.player.sendMessage(Text.literal("§a[ItemInfo] §f" + newItemsCount + " neue Items erfolgreich hinzugefügt!"), false);
			} else {
				client.player.sendMessage(Text.literal("§e[ItemInfo] §fAlle Items waren bereits vorhanden."), false);
			}
			client.player.sendMessage(Text.literal("§7Datei: §f" + outputFile.getName()), false);
		} catch (Exception e) {
			// Silent error handling("[ItemInfoUtility] ❌ Fehler beim Schreiben der Datei: " + e.getMessage());
			// Silent error handling
			if (client.player != null) {
				client.player.sendMessage(Text.literal("§c[ItemInfo] Fehler beim Speichern der Datei!"), false);
				client.player.sendMessage(Text.literal("§7Fehler: §f" + e.getMessage()), false);
			}
		}
	}
	
	// Data classes
	private static class ItemData {
		String id;
		Integer customModelData;
		String name;
		String floor;
		String rarity;
		Map<String, Map<String, Object>> price = new LinkedHashMap<>();
		Map<String, Map<String, Object>> blueprintShopPrice;
		boolean aspect;
		String type;
		String piece;
		List<String> modifiers = new ArrayList<>();
		List<String> stats;
		boolean blueprint;
		List<String> tags = new ArrayList<>();
	}
	
	private static class BlueprintInfo {
		String floor;
		String rarity;
	}
}


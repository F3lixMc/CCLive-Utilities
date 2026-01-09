package net.felix.utilities.Overall;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.client.gui.hud.InGameHud;
import java.util.Collection;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.ZeichenUtility;
import net.felix.utilities.Overall.Aspekte.AspectOverlay;
import net.felix.utilities.Overall.Aspekte.AspectOverlayRenderer;
import net.fabricmc.loader.api.FabricLoader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InformationenUtility {
	
	private static boolean isInitialized = false;
	
	// Store last mouse position from HandledScreen render method
	private static int lastMouseX = -1;
	private static int lastMouseY = -1;
	
	/**
	 * Sets the last mouse position (called from HandledScreenMixin)
	 */
	public static void setLastMousePosition(int mouseX, int mouseY) {
		lastMouseX = mouseX;
		lastMouseY = mouseY;
	}
	
	// Materials tracking
	private static Map<String, MaterialInfo> materialsDatabase = new HashMap<>();
	private static final String MATERIALS_CONFIG_FILE = "assets/cclive-utilities/Aincraft.json";
	
	// Essence tracking
	private static Map<String, EssenceInfo> essencesDatabase = new HashMap<>();
	private static final String ESSENCES_CONFIG_FILE = "assets/cclive-utilities/Essenz.json";
	
	// Aspect tracking
	private static Map<String, AspectInfo> aspectsDatabase = new HashMap<>();
	private static final String ASPECTS_CONFIG_FILE = "assets/cclive-utilities/Aspekte.json";
	
	// License tracking
	private static Map<String, LicenseInfo> licensesDatabase = new HashMap<>();
	private static final String LICENSES_CONFIG_FILE = "assets/cclive-utilities/Farmworld.json";
	
	// Cards and Statues effects tracking
	private static Map<String, String> cardsEffects = new HashMap<>();
	private static Map<String, String> statuesEffects = new HashMap<>();
	// Cards and Statues floor tracking
	private static Map<String, List<Integer>> cardsFloors = new HashMap<>();
	private static Map<String, List<Integer>> statuesFloors = new HashMap<>();
	private static final String CARDS_STATUES_CONFIG_FILE = "assets/cclive-utilities/CardsStatues.json";
	
	// Gadget tracking - Maps: "name/alias" -> Map<"level", "location">
	private static Map<String, Map<String, String>> gadgetsDatabase = new HashMap<>();
	
	// Blueprint floor tracking - Maps: "blueprint name" -> floor number (e.g., "Drachenzahn" -> 1)
	private static Map<String, Integer> blueprintFloorMap = new HashMap<>();
	private static final String BLUEPRINTS_CONFIG_FILE = "assets/cclive-utilities/blueprints.json";
	
	// MKLevel tracking - List of level data from MKLevel.json
	private static List<MKLevelInfo> mkLevelDatabase = new ArrayList<>();
	private static List<CombinedWaveInfo> mkLevelCombinedWavesDatabase = new ArrayList<>();
	private static final String MKLEVEL_CONFIG_FILE = "assets/MKLevel.json";
	
	// MKLevel overlay scroll variables - separate offsets for each tab
	private static int mkLevelScrollOffsetIndividual = 0; // Scroll offset for "Einzelne Wellen"
	private static int mkLevelScrollOffsetCombined = 0; // Scroll offset for "Kombinierte Wellen"
	private static boolean mkLevelOverlayHovered = false;
	private static boolean isInMKLevelInventory = false;
	
	// MKLevel overlay search variables
	private static String mkLevelSearchText = "";
	private static boolean mkLevelSearchFocused = false;
	private static int mkLevelSearchCursorPosition = 0;
	private static long mkLevelSearchCursorBlinkTime = 0;
	private static boolean mkLevelSearchCursorVisible = true;
	
	// MKLevel overlay height cache (updated when overlay is rendered, used by F6 editor)
	private static int mkLevelLastKnownHeight = 166; // Default height
	
	// MKLevel overlay mode: true = "Einzelne Wellen", false = "Kombinierte Wellen"
	private static boolean mkLevelShowIndividualWaves = true;
	
	// Helper method to get current scroll offset based on active tab
	private static int getMKLevelScrollOffset() {
		return mkLevelShowIndividualWaves ? mkLevelScrollOffsetIndividual : mkLevelScrollOffsetCombined;
	}
	
	// Helper method to set current scroll offset based on active tab
	private static void setMKLevelScrollOffset(int offset) {
		if (mkLevelShowIndividualWaves) {
			mkLevelScrollOffsetIndividual = offset;
		} else {
			mkLevelScrollOffsetCombined = offset;
		}
	}
	
	// MKLevel scrollbar dragging state
	private static boolean mkLevelScrollbarDragging = false;
	private static double mkLevelScrollbarDragStartY = 0;
	private static int mkLevelScrollbarDragStartOffset = 0;
	
	/**
	 * Gets the last known height of the MKLevel overlay (cached from last render)
	 * Used by F6 editor to avoid reflection issues on server
	 */
	public static int getMKLevelLastKnownHeight() {
		return mkLevelLastKnownHeight;
	}
	
	// Collection tracking
	private static final String COLLECTIONS_CONFIG_FILE = "assets/cclive-utilities/Collections.json";
	private static Map<Integer, Integer> collectionsDatabase = new HashMap<>(); // collection number -> amount needed
	private static int initialBlocks = -1; // Blocks when tracking started
	private static int currentBlocks = 0; // Current total blocks from bossbar
	private static int sessionBlocks = 0; // Blocks mined in this session (currentBlocks - initialBlocks)
	private static long sessionStartTime = 0; // When tracking started
	private static double blocksPerMinute = 0.0; // Blocks per minute
	private static boolean isTrackingCollections = false; // Whether we're currently tracking
	private static boolean firstBossBarUpdate = true; // First bossbar update flag
	private static String collectionDimension = null; // Track current dimension for collection tracking
	private static int nextCollectionNumber = 1; // Next collection to achieve
	private static int blocksNeededForNextCollection = 0; // Blocks needed for next collection
	private static String currentBiomName = null; // Track current biom name from scoreboard
	private static long lastScoreboardCheck = 0; // Last time we checked the scoreboard
	private static boolean biomDetected = false; // Whether a biom was detected in the scoreboard
	private static boolean lastShowCollectionOverlayState = true; // Track previous state of showCollectionOverlay
	private static int pendingResets = 0; // Number of pending resets after biome change
	
	// Collection hotkey
	private static KeyBinding collectionResetKeyBinding;
	
	// Mining & Lumberjack XP Tracking
	private static class XPData {
		long currentXP = 0;
		long requiredXP = 0;
		int level = 0;
		long initialXP = -1; // XP when tracking started (like initialKills in KillsUtility)
		long newXP = 0; // New XP gained since tracking started (current - initial)
		long sessionStartTime = 0; // When tracking started
		double xpPerMinute = 0.0; // Average XP per minute (calculated continuously)
		long lastGainedXP = 0; // Last XP gained in a single update (for display)
		boolean isTracking = false; // Whether we're currently tracking XP
		long lastXPChangeTime = 0; // When XP last changed (for overlay visibility timer)
		boolean shouldShowOverlay = false; // Whether overlay should be visible
		boolean isInitializedInCurrentDimension = false; // Whether values have been initialized in current dimension
	}
	
	private static XPData miningXP = new XPData();
	private static XPData lumberjackXP = new XPData();
	private static long lastTabListCheck = 0;
	private static final long OVERLAY_DISPLAY_DURATION = 10000; // 10 seconds in milliseconds
	private static String currentDimension = null; // Track current dimension to detect dimension changes
	private static String miningLumberjackDimension = null; // Track current dimension for mining/lumberjack overlays
	private static boolean showOverlays = true; // Whether overlays should be shown (hidden when Tab or F1 is active)
	
	/**
	 * Gets the header text for mining overlay (includes level if available)
	 */
	public static String getMiningOverlayHeader() {
		return miningXP.level > 0 ? String.format("Bergbau [lvl. %d]", miningXP.level) : "Bergbau";
	}
	
	/**
	 * Gets the header text for lumberjack overlay (includes level if available)
	 */
	public static String getLumberjackOverlayHeader() {
		return lumberjackXP.level > 0 ? String.format("Holzfäller [lvl. %d]", lumberjackXP.level) : "Holzfäller";
	}
	
	/**
	 * Gets the current overlay width for mining overlay (for use in overlay editor)
	 */
	public static int getMiningOverlayWidth(MinecraftClient client) {
		if (client == null || client.textRenderer == null) {
			return 200;
		}
		
		int padding = 5;
		String header = getMiningOverlayHeader();
		String lastXP = "Letzte XP: " + (miningXP.lastGainedXP > 0 ? formatNumberWithSeparator(miningXP.lastGainedXP) : "0");
		String xpPerMin = miningXP.xpPerMinute > 0 ? "XP/Min: " + formatDoubleWithSeparator(miningXP.xpPerMinute) : "XP/Min: -";
		long xpNeeded = miningXP.requiredXP - miningXP.currentXP;
		String requiredXP = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(miningXP));
		
		int maxWidth = Math.max(
			Math.max(client.textRenderer.getWidth(header),
				Math.max(client.textRenderer.getWidth(lastXP),
					Math.max(client.textRenderer.getWidth(xpPerMin),
						Math.max(client.textRenderer.getWidth(requiredXP),
							client.textRenderer.getWidth(timeToNext))))),
			100);
		return maxWidth + padding * 2;
	}
	
	/**
	 * Gets the actual text strings for mining overlay (for use in overlay editor)
	 */
	public static String[] getMiningOverlayTexts(MinecraftClient client) {
		if (client == null || client.textRenderer == null) {
			return new String[]{"Bergbau", "Letzte XP: 0", "XP/Min: -", "Zeit bis Level: Unbekannt", "Benötigte XP: 0"};
		}
		
		String header = getMiningOverlayHeader();
		String lastXP = "Letzte XP: " + (miningXP.lastGainedXP > 0 ? formatNumberWithSeparator(miningXP.lastGainedXP) : "0");
		String xpPerMin = miningXP.xpPerMinute > 0 ? "XP/Min: " + formatDoubleWithSeparator(miningXP.xpPerMinute) : "XP/Min: -";
		long xpNeeded = miningXP.requiredXP - miningXP.currentXP;
		String requiredXP = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(miningXP));
		
		return new String[]{header, lastXP, xpPerMin, timeToNext, requiredXP};
	}
	
	/**
	 * Gets the actual text strings for lumberjack overlay (for use in overlay editor)
	 */
	public static String[] getLumberjackOverlayTexts(MinecraftClient client) {
		if (client == null || client.textRenderer == null) {
			return new String[]{"Holzfäller", "Letzte XP: 0", "XP/Min: -", "Zeit bis Level: Unbekannt", "Benötigte XP: 0"};
		}
		
		String header = getLumberjackOverlayHeader();
		String lastXP = "Letzte XP: " + (lumberjackXP.lastGainedXP > 0 ? formatNumberWithSeparator(lumberjackXP.lastGainedXP) : "0");
		String xpPerMin = lumberjackXP.xpPerMinute > 0 ? "XP/Min: " + formatDoubleWithSeparator(lumberjackXP.xpPerMinute) : "XP/Min: -";
		long xpNeeded = lumberjackXP.requiredXP - lumberjackXP.currentXP;
		String requiredXP = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(lumberjackXP));
		
		return new String[]{header, lastXP, xpPerMin, timeToNext, requiredXP};
	}
	
	/**
	 * Gets the current overlay width for lumberjack overlay (for use in overlay editor)
	 */
	public static int getLumberjackOverlayWidth(MinecraftClient client) {
		if (client == null || client.textRenderer == null) {
			return 200;
		}
		
		int padding = 5;
		String header = getLumberjackOverlayHeader();
		String lastXP = "Letzte XP: " + (lumberjackXP.lastGainedXP > 0 ? formatNumberWithSeparator(lumberjackXP.lastGainedXP) : "0");
		String xpPerMin = lumberjackXP.xpPerMinute > 0 ? "XP/Min: " + formatDoubleWithSeparator(lumberjackXP.xpPerMinute) : "XP/Min: -";
		long xpNeeded = lumberjackXP.requiredXP - lumberjackXP.currentXP;
		String requiredXP = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(lumberjackXP));
		
		int maxWidth = Math.max(
			Math.max(client.textRenderer.getWidth(header),
				Math.max(client.textRenderer.getWidth(lastXP),
					Math.max(client.textRenderer.getWidth(xpPerMin),
						Math.max(client.textRenderer.getWidth(requiredXP),
							client.textRenderer.getWidth(timeToNext))))),
			100);
		return maxWidth + padding * 2;
	}

	public static void initialize() {
		if (isInitialized) {
			return;
		}
		
		// Load materials database
		loadMaterialsDatabase();
		
		// Load essences database
		loadEssencesDatabase();
		
		// Load cards and statues effects database
		loadCardsStatuesDatabase();
		
		// Load aspects database
		loadAspectsDatabase();
		
		// Load licenses database
		loadLicensesDatabase();
		
		// Load gadgets database
		loadGadgetsDatabase();
		
		// Load blueprints database for floor numbers
		loadBlueprintsDatabase();
		
		// Load MKLevel database
		loadMKLevelDatabase();
		
		// Load collections database
		loadCollectionsDatabase();
		
		// Register collection hotkeys
		registerCollectionHotkeys();
		
		// Initialize aspect overlay and renderer
		AspectOverlay.initialize();
		AspectOverlayRenderer.initialize();
		
		// Register client tick event for continuous XP calculation (like KillsUtility)
		ClientTickEvents.END_CLIENT_TICK.register(InformationenUtility::onClientTick);
		
		// Register HUD render callback for mining and lumberjack overlays
		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> 
			onHudRender(drawContext, tickDelta));
		
		
		// Register tooltip callback for material information
		ItemTooltipCallback.EVENT.register((stack, context, tooltipType, lines) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			
		// Always process aspect information (even if informationenUtilityEnabled is off)
		// Add aspect name to tooltip (always visible) - works in inventories
		addAspectNameToTooltip(lines, client, stack);
		
		// Add floor number to blueprint names in inventories
		addFloorNumberToBlueprintNames(lines, client);
		
		// Add floor numbers to cards and statues names in inventories
		addFloorNumberToCardsStatuesNames(lines, client);
		
		// Add slot-specific text for "Aspekt [tranferieren]" inventory
		addAspectTransferSlotText(lines, client, stack);
		
		// Only process other information if Informationen Utility is enabled in config
			if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
				!CCLiveUtilitiesConfig.HANDLER.instance().informationenUtilityEnabled) {
				return;
			}
			
			// Check if we're in the special inventory (Moblexicon)
			boolean isSpecialInventory = false;
			boolean isLicenseInventory = false;
			boolean isEssenceHarvesterUi = false;
			String screenTitle = "";
			if (client.currentScreen != null) {
				screenTitle = client.currentScreen.getTitle().getString();
				if (ZeichenUtility.containsMoblexicon(screenTitle)) {
					isSpecialInventory = true;
				}
				// Check for license inventory character (friends_request_accept_deny)
				if (ZeichenUtility.containsFriendsRequestAcceptDeny(screenTitle)) {
					isLicenseInventory = true;
				}
				// Check for essence harvester UI - don't add floor numbers here
				if (ZeichenUtility.containsEssenceHarvesterUi(screenTitle)) {
					isEssenceHarvesterUi = true;
				}
			}
			
			// If we're in essence harvester UI, don't add floor numbers
			if (isEssenceHarvesterUi) {
				return;
			}
			
			// Check if the respective setting is enabled for this inventory type
			if (isSpecialInventory && !CCLiveUtilitiesConfig.HANDLER.instance().showEbenenInSpecialInventory) {
				return; // Special inventory disabled
			}
			if (!isSpecialInventory && !CCLiveUtilitiesConfig.HANDLER.instance().showEbenenInNormalInventories) {
				return; // Normal inventories disabled
			}
			
			// In special inventory (㬉), check all lines for [Karte]/[Statue] but only first line for material names
			// In other inventories, check all lines for material names
			int startIndex = 0;
			int endIndex = lines.size(); // Always check all lines to find [Karte] or [Statue]
			
			for (int i = startIndex; i < endIndex; i++) {
				Text line = lines.get(i);
				String lineText = line.getString();
				
				// Skip if lineText is null or empty
				if (lineText == null || lineText.isEmpty()) {
					continue;
				}
				
				// Remove Minecraft formatting codes (like §a, §b, etc.)
				String cleanLineText = lineText.replaceAll("§[0-9a-fk-or]", "");
				
				// Also remove the Unicode formatting characters that appear as 㔻㔶
				cleanLineText = cleanLineText.replaceAll("[\\u3400-\\u4DBF]", "");
				
				// Check if we're in the "Machtkristalle Verbessern" inventory for essence information
				boolean isEssenceImprovementInventory = screenTitle.contains("Machtkristalle Verbessern");
				
				// Check for essence information in "Machtkristalle Verbessern" inventory FIRST
				if (isEssenceImprovementInventory && lineText.contains("[Essenz]")) {
					// Get the text before the colon (if present) to remove the count info
					String textBeforeColon = cleanLineText;
					if (cleanLineText.contains(":")) {
						textBeforeColon = cleanLineText.substring(0, cleanLineText.indexOf(":")).trim();
					}
					
					// Remove leading dash/minus if present
					if (textBeforeColon.startsWith("-")) {
						textBeforeColon = textBeforeColon.substring(1).trim();
					}
					
					// The textBeforeColon already contains the full essence name including [Essenz] and Tier
					String essenceNameToSearch = textBeforeColon;
					
					// Look for essence in database
					EssenceInfo essenceInfo = essencesDatabase.get(essenceNameToSearch);
					if (essenceInfo != null && CCLiveUtilitiesConfig.HANDLER.instance().showWaveDisplay) {
						// Add essence information as a new line with mixed colors
						Text essenceInfoText = Text.literal(" -> Welle: ")
							.styled(style -> style.withColor(0xC0C0C0)) // Light gray
							.append(Text.literal(String.valueOf(essenceInfo.wave))
								.styled(style -> style.withColor(0x55FF55))); // Light green
						
						// Add the essence info line after the current line
						lines.add(i + 1, essenceInfoText);
						
						// Skip the next line since we just added essence info
						i++;
						continue; // Skip other processing for this line
					}
				}
				
				// Skip Essenz items that should not show level information (but allow essence improvement inventory)
				if (screenTitle.contains("Essenz [Auswahl]") || screenTitle.contains("Essenz-Tasche") ||
					screenTitle.contains("Essenzernter") ||screenTitle.contains("Legend+ Menü") ||
					 ZeichenUtility.containsHunterUiBackground(screenTitle)){ //Hunter ui_background
					
					continue;
				}
				
				// Extract the part before the first '[' to get just the material name
				String materialNamePart = cleanLineText;
				if (cleanLineText.contains("[")) {
					materialNamePart = cleanLineText.substring(0, cleanLineText.indexOf("[")).trim();
				}
				
				// Skip if the material name part is empty
				if (materialNamePart.isEmpty()) {
					continue;
				}
				
				// Process lines with [Karte] or [Statue] in Moblexicon to add effect information
				if ((lineText.contains("[Karte]") || lineText.contains("[Statue]")) && isSpecialInventory) {
					// Extract the name (text before [Karte] or [Statue])
					String name = "";
					String effect = null;
					boolean isCard = false;
					
					if (lineText.contains("[Karte]")) {
						int index = cleanLineText.indexOf("[Karte]");
						if (index > 0) {
							name = cleanLineText.substring(0, index).trim();
							// Remove any leading dashes or special characters
							name = name.replaceAll("^[-\\s]+", "").trim();
							effect = cardsEffects.get(name);
							isCard = true;
							
							// Try to find the card with case-insensitive search if not found
							if (effect == null) {
								for (Map.Entry<String, String> entry : cardsEffects.entrySet()) {
									if (entry.getKey().equalsIgnoreCase(name)) {
										effect = entry.getValue();
										break;
									}
								}
							}
						}
					} else if (lineText.contains("[Statue]")) {
						int index = cleanLineText.indexOf("[Statue]");
						if (index > 0) {
							name = cleanLineText.substring(0, index).trim();
							// Remove any leading dashes or special characters
							name = name.replaceAll("^[-\\s]+", "").trim();
							effect = statuesEffects.get(name);
							isCard = false;
							
							// Try to find the statue with case-insensitive search if not found
							if (effect == null) {
								for (Map.Entry<String, String> entry : statuesEffects.entrySet()) {
									if (entry.getKey().equalsIgnoreCase(name)) {
										effect = entry.getValue();
										break;
									}
								}
							}
						}
					}
					
					// If effect found, append it to the line
					if (effect != null && !effect.isEmpty()) {
						// Determine color: Green (0x55FF55) for cards, Aqua (0x55FFFF) for statues
						int effectColor = isCard ? 0x55FF55 : 0x55FFFF; // Green for cards, Aqua for statues
						
						// Create new text: original line (with preserved formatting) + " - " + effect
						MutableText newLine = line.copy() // Preserve original formatting and style
							.append(Text.literal(" - ").styled(style -> style.withColor(0xC0C0C0))) // Light gray separator
							.append(Text.literal(effect).styled(style -> style.withColor(effectColor))); // Green for cards, Aqua for statues
						
						// Replace the line
						lines.set(i, newLine);
					}
					continue; // Skip further processing for this line
				}
				
				// Skip if the line contains [Karte] or [Statue] in non-special inventories
				if (lineText.contains("[Karte]") || lineText.contains("[Statue]")) {
					continue;
				}
				
				// In special inventory, only process material names on the first line
				if (isSpecialInventory && i > 0) {
					continue;
				}
				
				// Sort materials by length (longest first) to avoid shorter names matching before longer ones
				List<Map.Entry<String, MaterialInfo>> sortedMaterials = new ArrayList<>(materialsDatabase.entrySet());
				sortedMaterials.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
				
				// Look for materials in the material name part (longest first)
				for (Map.Entry<String, MaterialInfo> entry : sortedMaterials) {
					String materialName = entry.getKey();
					MaterialInfo info = entry.getValue();
					
					// Use word boundaries to avoid partial matches
					// This ensures "Schweinehuf" doesn't match "Kräftiger Schweinehuf"
					// Also handles German characters like "ß" properly
					String escapedMaterialName = java.util.regex.Pattern.quote(materialName);
					String pattern = "\\b" + escapedMaterialName + "\\b";
					
					if (java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.UNICODE_CHARACTER_CLASS).matcher(materialNamePart).find()) {
						// Skip if this is a blueprint line - let BPViewerUtility handle it
						if (lineText.contains("[Bauplan]")) {
							continue; // Skip blueprint lines to avoid conflicts
						}
						
						// In special inventory, only show floor info for mob names, not materials
						if (isSpecialInventory && !info.rarity.equals("mob")) {
							continue; // Skip materials in special inventory
						}
						
						// Create new text with material info appended to the same line
						int color = getRarityColor(info.rarity);
						
						// Different format for special inventory
						Text materialInfo;
						if (isSpecialInventory) {
							// In special inventory: show just the floor number behind the mob name
							materialInfo = Text.literal(" [" + info.floor + "]")
								.styled(style -> style.withColor(color));
						} else {
							// In normal inventory: show full floor information
							materialInfo = Text.literal(" [Ebene " + info.floor + "]")
								.styled(style -> style.withColor(color));
						}
						
						// Combine original line with material info
						Text combinedText = line.copy().append(materialInfo);
						
						// Replace the original line
						lines.set(i, combinedText);
						break; // Only add info for the first match in this line
					}
				}
			}
			
			// Check for license information in inventories with ち
			if (isLicenseInventory && CCLiveUtilitiesConfig.HANDLER.instance().showLicenseInformation) {
				// Check if the hovered item is in one of the license slots
				if (isItemInLicenseSlot(stack, client)) {
					checkForLicenseInformation(lines, client);
				}
			}
			
			// Check for gadget information in "Module [Upgraden]" inventory
			if (CCLiveUtilitiesConfig.HANDLER.instance().showModuleInformation) {
				boolean isModuleUpgradeInventory = false;
				boolean isEngineerInventory = false;
				if (client.currentScreen != null) {
					String cleanScreenTitle = screenTitle.replaceAll("§[0-9a-fk-or]", "")
						.replaceAll("[\\u3400-\\u4DBF]", "");
					if (cleanScreenTitle.contains("Module [Upgraden]")) {
						isModuleUpgradeInventory = true;
					}
					if (cleanScreenTitle.contains("[Ingenieur]")) {
						isEngineerInventory = true;
					}
				}
				
				if (isModuleUpgradeInventory) {
					checkForGadgetInformation(lines, false);
				}
				
				if (isEngineerInventory) {
					checkForGadgetInformation(lines, true);
				}
			}
		});
		
		
		isInitialized = true;
	}
	
	/**
	 * Updates the aspect overlay based on current tooltip content
	 */
	private static void updateAspectOverlayFromTooltip(MinecraftClient client) {
		// Check if aspect overlay is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
			return; // Don't update overlay if aspect overlay is disabled
		}
		
		// Check if shift is pressed
		boolean isShiftPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), 
													   InputUtil.GLFW_KEY_LEFT_SHIFT) || 
								InputUtil.isKeyPressed(client.getWindow().getHandle(), 
													   InputUtil.GLFW_KEY_RIGHT_SHIFT);
		
		if (!isShiftPressed) {
			AspectOverlay.hideOverlay();
			return;
		}
		
		// For now, we'll use a simple approach - check if we're hovering over a blueprint item
		// In a real implementation, you'd need to access the current tooltip content
		// This is a placeholder that will be improved
		
		// Since we can't easily access the current tooltip content from here,
		// we'll use a different approach - we'll modify the tooltip callback to also update the overlay
	}
	
	/**
	 * Adds aspect name to tooltip (in blueprint inventories only)
	 */
	public static void addAspectNameToTooltip(List<Text> lines, MinecraftClient client, ItemStack stack) {
		// Check if aspect overlay is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
			return; // Don't show aspect information if aspect overlay is disabled
		}
		
		// First, extract the item name from the tooltip (usually the first line)
		String itemName = extractItemNameFromTooltip(lines, stack);
		
		// Check for lines containing "⭐" (star symbol) - this is for items with aspect info in tooltip
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			// Skip if lineText is null or empty
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}
			
			// Check if this line contains "⭐"
			if (lineText.contains("⭐")) {
				// If we have an item name, look it up directly in the aspects database
				if (itemName != null && !itemName.isEmpty()) {
					// Get aspect info for this item directly from the database
					AspectInfo aspectInfo = aspectsDatabase.get(itemName);
					
					if (aspectInfo != null && !aspectInfo.aspectName.equals("-")) {
						// Modify the line to add "(shift für info)" after the "]"
						Text modifiedLine = modifyStarLineWithShiftInfo(line, aspectInfo, itemName);
						if (modifiedLine != null) {
							lines.set(i, modifiedLine);
							// Set up aspect overlay to show when Shift is pressed
							net.felix.utilities.Overall.Aspekte.AspectOverlay.updateAspectInfoFromName(itemName, aspectInfo);
						}
					}
				}
			}
		}
		
		// Note: The "⭐" check above works in ALL inventories, not just blueprint inventories
		// The code below is only for blueprint items with "[Bauplan]" in the tooltip
		
		boolean isInBlueprintInventory = false;
		
		// Check if we're in a blueprint inventory
		if (client.currentScreen != null) {
		String screenTitle = client.currentScreen.getTitle().getString();
		
		// Remove Minecraft formatting codes and Unicode characters for comparison
		String cleanScreenTitle = screenTitle.replaceAll("§[0-9a-fk-or]", "")
											.replaceAll("[\\u3400-\\u4DBF]", "");
		
		// Only show aspect information in specific blueprint inventories
			isInBlueprintInventory = cleanScreenTitle.contains("Baupläne [Waffen]") ||
									  cleanScreenTitle.contains("Baupläne [Rüstung]") ||
									  cleanScreenTitle.contains("Baupläne [Werkzeuge]") ||
									  cleanScreenTitle.contains("Bauplan [Shop]") || 
									  cleanScreenTitle.contains("Favorisierte [Rüstungsbaupläne]") ||
									  cleanScreenTitle.contains("Favorisierte [Waffenbaupläne]") ||
									  cleanScreenTitle.contains("Favorisierte [Werkzeugbaupläne]") ||
									  cleanScreenTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools");
		}
		
		// Only process blueprint items if we're in a blueprint inventory
		// Items with "⭐" are processed above and work in all inventories
		if (!isInBlueprintInventory) {
			return; // Don't show aspect information for blueprint items in other inventories
		}
		
		// Check tooltip lines for inventory tooltips (they contain "[Bauplan]")
		String itemNameToCheck = null;
		Text blueprintLine = null;
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			// Skip if lineText is null or empty
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}
			
			// Check if this is a blueprint line
			if (lineText.contains("[Bauplan]")) {
				// For inventory tooltips, use the line text directly
				itemNameToCheck = lineText;
				blueprintLine = line;
				break;
			}
		}
		
		// Check if the blueprint line contains Epic colors - if so, don't show aspect info
		if (blueprintLine != null && hasEpicColor(blueprintLine)) {
			return; // Don't show aspect information for Epic blueprints
		}
		
		// If we found a blueprint item, add aspect information
		// Note: itemName from tooltip extraction above is for "⭐" items, here we need a separate variable for blueprint items
		String blueprintItemName = null;
		if (itemNameToCheck != null && itemNameToCheck.contains("[Bauplan]")) {
				// Extract the item name (everything before "[Bauplan]")
			blueprintItemName = itemNameToCheck.substring(0, itemNameToCheck.indexOf("[Bauplan]")).trim();
				
				// Remove leading dash/minus if present
				if (blueprintItemName.startsWith("-")) {
					blueprintItemName = blueprintItemName.substring(1).trim();
				}
				
				// Remove trailing dash/minus if present
				if (blueprintItemName.endsWith("-")) {
					blueprintItemName = blueprintItemName.substring(0, blueprintItemName.length() - 1).trim();
				}
				
				// Remove Minecraft formatting codes and Unicode characters
				blueprintItemName = blueprintItemName.replaceAll("§[0-9a-fk-or]", "");
				blueprintItemName = blueprintItemName.replaceAll("[\\u3400-\\u4DBF]", "");
				blueprintItemName = blueprintItemName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		}
				
				// Look for this item in the aspects database
		if (blueprintItemName != null && !blueprintItemName.isEmpty()) {
				AspectInfo aspectInfo = aspectsDatabase.get(blueprintItemName);
				if (aspectInfo != null) {
				// Find the position of "Benötigt" line
				int benötigtPosition = -1;
				for (int i = 0; i < lines.size(); i++) {
					Text line = lines.get(i);
					String lineText = line.getString();
					if (lineText != null && lineText.contains("Benötigt")) {
						benötigtPosition = i;
						break;
					}
				}
				
				// If "Benötigt" not found, use fallback position (5th line from bottom)
				int targetPosition;
				if (benötigtPosition >= 0) {
					// Insert right before "Benötigt"
					targetPosition = benötigtPosition;
				} else {
					// Fallback: 5th line from bottom
					targetPosition = Math.max(0, lines.size() - 5);
				}
					
					// Create aspect name text without "(Shift)" suffix
					Text aspectNameText = Text.literal("Enthält: ")
						.styled(style -> style.withColor(0xFFFFFFFF)) // White color
						.append(Text.literal(aspectInfo.aspectName)
							.styled(style -> style.withColor(0xFFFCA800))); // Same color as overlay (#FCA800)
					
					// Insert aspect name at the target position
					lines.add(targetPosition, aspectNameText);
					
					// Add "(Shift für mehr Info)" on the next line
					Text shiftInfoText = Text.literal("(Shift für mehr Info)")
						.styled(style -> style.withColor(0xFFCCCCCC)); // Light gray color
					lines.add(targetPosition + 1, shiftInfoText);
					
				// Add empty line after shift info (only if we're not inserting before "Benötigt")
				if (benötigtPosition < 0) {
					Text emptyLineText = Text.literal(" ");
					lines.add(targetPosition + 2, emptyLineText);
				}
			}
		}
	}
	
	/**
	 * Gets the aspect name for a blueprint item name
	 * @param blueprintName The cleaned blueprint name (without "[Bauplan]")
	 * @return The aspect name, or null if not found
	 */
	public static String getAspectInfoForBlueprint(String blueprintName) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			return null;
		}
		
		// Remove any remaining formatting
		String cleanName = blueprintName.replaceAll("§[0-9a-fk-or]", "");
		cleanName = cleanName.replaceAll("[\\u3400-\\u4DBF]", "");
		cleanName = cleanName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		
		AspectInfo aspectInfo = aspectsDatabase.get(cleanName);
		if (aspectInfo != null) {
			return aspectInfo.aspectName;
		}
		
		return null;
	}
	
	/**
	 * Extracts the item name from the tooltip lines
	 * Usually the first line contains the item name, but we need to clean it
	 * @param lines The tooltip lines
	 * @param stack The item stack (as fallback)
	 * @return The cleaned item name, or null if not found
	 */
	private static String extractItemNameFromTooltip(List<Text> lines, ItemStack stack) {
		if (lines == null || lines.isEmpty()) {
			return null;
		}
		
		// Try to get item name from first line (usually the item name)
		if (lines.size() > 0) {
			Text firstLine = lines.get(0);
			if (firstLine != null) {
				String itemName = firstLine.getString();
				if (itemName != null && !itemName.isEmpty()) {
					// Remove Minecraft formatting codes
					itemName = itemName.replaceAll("§[0-9a-fk-or]", "");
					itemName = itemName.replaceAll("§#[0-9a-fA-F]{6}", "");
					
					// Remove Unicode formatting characters (like 㔜㔙㔕)
					itemName = itemName.replaceAll("[\\u3400-\\u4DBF]", "");
					
					// Remove any leading/trailing whitespace
					itemName = itemName.trim();
					
					// Remove prefixes like [+XXX] at the beginning
					itemName = itemName.replaceAll("^\\[\\+[^\\]]+\\]\\s*", "");
					
					// Remove suffixes like [SCHMIEDEZUSTAND] at the end
					itemName = itemName.replaceAll("\\s*\\[[^\\]]+\\]$", "");
					
					// Remove any other brackets and their content (like [Bauplan], etc.)
					// But keep the item name itself
					itemName = itemName.replaceAll("\\[[^\\]]+\\]", "");
					
					// Remove any leading/trailing whitespace again
					itemName = itemName.trim();
					
					if (!itemName.isEmpty()) {
						return itemName;
					}
				}
			}
		}
		
		// Fallback: try to get name from ItemStack
		if (stack != null && !stack.isEmpty()) {
			Text displayName = stack.getName();
			if (displayName != null) {
				String itemName = displayName.getString();
				if (itemName != null && !itemName.isEmpty()) {
					// Remove Minecraft formatting codes
					itemName = itemName.replaceAll("§[0-9a-fk-or]", "");
					itemName = itemName.replaceAll("§#[0-9a-fA-F]{6}", "");
					// Remove Unicode formatting characters
					itemName = itemName.replaceAll("[\\u3400-\\u4DBF]", "");
					
					// Remove prefixes like [+XXX] at the beginning
					itemName = itemName.replaceAll("^\\[\\+[^\\]]+\\]\\s*", "");
					
					// Remove suffixes like [SCHMIEDEZUSTAND] at the end
					itemName = itemName.replaceAll("\\s*\\[[^\\]]+\\]$", "");
					
					// Remove any other brackets and their content
					itemName = itemName.replaceAll("\\[[^\\]]+\\]", "");
					
					itemName = itemName.trim();
					
					return itemName;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Finds the item name that has a given aspect name (reverse lookup)
	 * @param aspectName The aspect name to search for
	 * @return The item name, or null if not found
	 */
	private static String findItemNameByAspectName(String aspectName) {
		if (aspectName == null || aspectName.isEmpty()) {
			return null;
		}
		
		// Clean the aspect name for comparison
		String cleanAspectName = aspectName.replaceAll("§[0-9a-fk-or]", "");
		cleanAspectName = cleanAspectName.replaceAll("§#[0-9a-fA-F]{6}", "");
		cleanAspectName = cleanAspectName.trim();
		
		// Search through the aspects database
		int checkedCount = 0;
		for (Map.Entry<String, AspectInfo> entry : aspectsDatabase.entrySet()) {
			String itemName = entry.getKey();
			AspectInfo aspectInfo = entry.getValue();
			checkedCount++;
			
			if (aspectInfo != null && aspectInfo.aspectName != null) {
				// Clean the stored aspect name for comparison
				String cleanStoredAspectName = aspectInfo.aspectName.replaceAll("§[0-9a-fk-or]", "");
				cleanStoredAspectName = cleanStoredAspectName.replaceAll("§#[0-9a-fA-F]{6}", "");
				cleanStoredAspectName = cleanStoredAspectName.trim();
				
				// Compare (case-insensitive)
				if (cleanStoredAspectName.equalsIgnoreCase(cleanAspectName)) {
					return itemName;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Modifies a line containing "⭐ [ASPEKT DES ITEMS]" to add "(shift für info)" after the "]"
	 * @param originalLine The original Text line
	 * @param aspectInfo The aspect info for this item
	 * @param itemName The item name
	 * @return The modified Text line, or null if modification failed
	 */
	private static Text modifyStarLineWithShiftInfo(Text originalLine, AspectInfo aspectInfo, String itemName) {
		if (originalLine == null || aspectInfo == null || itemName == null) {
			return null;
		}
		
		String lineText = originalLine.getString();
		if (lineText == null || !lineText.contains("⭐") || !lineText.contains("]")) {
			return null;
		}
		
		// Check if "(shift für info)" is already in the line
		if (lineText.contains("(Shift für info)") || lineText.contains("(Shift für Info)")) {
			return originalLine; // Already modified
		}
		
		// Find the position of "]" after "⭐"
		int starIndex = lineText.indexOf("⭐");
		int bracketEnd = lineText.indexOf("]", starIndex);
		if (bracketEnd == -1) {
			return null;
		}
		
		// Create the shift info text in italic and gray
		net.minecraft.text.MutableText shiftInfoText = Text.literal(" (Shift für Info)")
			.styled(style -> style
				.withColor(0xFF808080) // Gray color
				.withItalic(true)); // Italic
		
		// Copy the original line to preserve its format and structure
		net.minecraft.text.MutableText result = originalLine.copy();
		
		// Simply append the shift info text as a sibling
		// This preserves the original format while adding the new text
		result.append(shiftInfoText);
		
		return result;
	}
	
	/**
	 * Gets the full AspectInfo for a blueprint item name
	 * @param blueprintName The cleaned blueprint name (without "[Bauplan]")
	 * @return The AspectInfo, or null if not found
	 */
	public static AspectInfo getAspectInfoForBlueprintFull(String blueprintName) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			return null;
		}
		
		// Remove any remaining formatting
		String cleanName = blueprintName.replaceAll("§[0-9a-fk-or]", "");
		cleanName = cleanName.replaceAll("[\\u3400-\\u4DBF]", "");
		cleanName = cleanName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		
		return aspectsDatabase.get(cleanName);
	}
	
	/**
	 * Gets the floor number for a blueprint item name
	 * @param blueprintName The cleaned blueprint name (without "[Bauplan]")
	 * @param itemNameText Optional Text object for color detection (for "Drachenzahn")
	 * @return The floor number, or null if not found
	 */
	public static Integer getFloorNumberForBlueprint(String blueprintName, Text itemNameText) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			return null;
		}
		
		// Remove any remaining formatting
		String cleanName = blueprintName.replaceAll("§[0-9a-fk-or]", "");
		cleanName = cleanName.replaceAll("[\\u3400-\\u4DBF]", "");
		cleanName = cleanName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		
		// Special handling for "Drachenzahn" which appears in multiple floors
		if (cleanName.equals("Drachenzahn")) {
			if (itemNameText != null) {
				return getDrachenzahnFloorFromColor(itemNameText);
			}
			// If no Text provided, return null (can't determine floor without color)
			return null;
		}
		
		// For other blueprints, get floor number from map
		return blueprintFloorMap.get(cleanName);
	}
	
	/**
	 * Gets the floor number for a blueprint item name (simplified version without color detection)
	 * @param blueprintName The cleaned blueprint name (without "[Bauplan]")
	 * @return The floor number, or null if not found
	 */
	public static Integer getFloorNumberForBlueprint(String blueprintName) {
		return getFloorNumberForBlueprint(blueprintName, null);
	}
	
	/**
	 * Modifies chat messages to add aspect information to hover events
	 * Called from ClientPlayNetworkHandlerTestMixin
	 * Adds aspect info and "Shift für Info" to blueprint hover events
	 * If chat aspect overlay is enabled, no lines are added to hover events
	 */
	public static Text modifyChatMessageForAspectInfo(Text message) {
		if (message == null) {
			return null;
		}
		
		// Check if message contains [Bauplan]
		String fullMessageText = message.getString();
		if (fullMessageText == null || !fullMessageText.contains("[Bauplan]")) {
			return null; // Not a blueprint message
		}
		
		// Check if aspect overlay is enabled
		if (!CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
			return null; // Aspect overlay disabled
		}
		
		// If chat aspect overlay is disabled, don't add lines to hover events
		// When enabled, both hover event lines and overlay will be shown
		if (!CCLiveUtilitiesConfig.HANDLER.instance().chatAspectOverlayEnabled) {
			return null; // Chat aspect overlay disabled, don't modify hover event
		}
		
		// Extract blueprint name from message
		String blueprintName = extractBlueprintNameFromChatMessage(message);
		if (blueprintName == null || blueprintName.isEmpty()) {
			return null; // Could not extract blueprint name
		}
		
		// Get aspect information
		AspectInfo aspectInfo = getAspectInfoForBlueprintFull(blueprintName);
		if (aspectInfo == null || aspectInfo.aspectName == null || aspectInfo.aspectName.isEmpty()) {
			return null; // No aspect info found
		}
		
		// Find hover event in message
		HoverEvent hoverEvent = findHoverEventInText(message);
		if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) {
			return null; // No hover event found
		}
		
		// Extract original hover text
		Text originalHoverText = extractHoverTextFromEvent(hoverEvent, message);
		
		// Ensure aspectInfo.aspectName is not null or empty
		String aspectName = aspectInfo.aspectName != null && !aspectInfo.aspectName.isEmpty() ? aspectInfo.aspectName : "-";
		
		// Create new hover text with aspect info inserted above "Benötigt:" line
		Text newHoverText;
		try {
			if (originalHoverText != null) {
				// Find the line containing "Benötigt:" and get its style/color
				net.minecraft.text.Style benötigtStyle = findStyleForLineContaining(originalHoverText, "Benötigt");
				
				// Get the default color from the original text if no specific style found
				net.minecraft.text.Style defaultStyle = originalHoverText.getStyle();
				net.minecraft.text.Style infoStyle = benötigtStyle != null ? benötigtStyle : 
					(defaultStyle != null ? defaultStyle : net.minecraft.text.Style.EMPTY);
				
				// Create aspect info text with the same color as the original text
				// Extract color from the style
				final Integer textColor;
				if (infoStyle != null && infoStyle.getColor() != null) {
					textColor = infoStyle.getColor().getRgb();
				} else {
					textColor = null;
				}
				
				Text aspectNameText = Text.literal("Enthält: ")
					.styled(style -> style.withColor(0xFFFFFFFF)) // White color
					.append(Text.literal(aspectName)
						.styled(style -> style.withColor(0xFFFCA800))); // Aspect name in overlay color
				
				Text shiftInfoText = Text.literal("(Shift für mehr Info)")
					.styled(style -> style.withColor(0xFFCCCCCC)); // Light gray
				
				// Build the new text by iterating through OrderedText and inserting before "Benötigt:"
				// This preserves all formatting from the original text character by character
				net.minecraft.text.MutableText result = Text.empty();
				final boolean[] inserted = {false};
				java.util.List<Integer> currentLine = new java.util.ArrayList<>();
				final java.util.List<net.minecraft.text.Style> lineStyles = new java.util.ArrayList<>();
				
				net.minecraft.text.OrderedText orderedText = originalHoverText.asOrderedText();
				orderedText.accept((index, style, codePoint) -> {
					char ch = (char) codePoint;
					
					if (ch == '\n') {
						// End of line - check if it contains "Benötigt:"
						StringBuilder lineBuilder = new StringBuilder();
						for (int cp : currentLine) {
							lineBuilder.appendCodePoint(cp);
						}
						String lineText = lineBuilder.toString();
						
						if (!inserted[0] && lineText.contains("Benötigt")) {
							// Insert aspect info before this line
							result.append(aspectNameText);
							result.append(Text.literal("\n"));
							result.append(shiftInfoText);
							result.append(Text.literal("\n"));
							inserted[0] = true;
						}
						
						// Append the line character by character with formatting preserved
						for (int i = 0; i < currentLine.size(); i++) {
							int cp = currentLine.get(i);
							net.minecraft.text.Style charStyle = i < lineStyles.size() ? lineStyles.get(i) : style;
							net.minecraft.text.MutableText charText = Text.literal(new String(Character.toChars(cp)));
							if (charStyle != null) {
								charText = charText.styled(s -> charStyle);
							}
							result.append(charText);
						}
						result.append(Text.literal("\n"));
						currentLine.clear();
						lineStyles.clear();
					} else {
						currentLine.add(codePoint);
						lineStyles.add(style);
					}
					
					return true;
				});
				
				// Handle the last line
				for (int i = 0; i < currentLine.size(); i++) {
					int cp = currentLine.get(i);
					net.minecraft.text.Style charStyle = i < lineStyles.size() ? lineStyles.get(i) : null;
					net.minecraft.text.MutableText charText = Text.literal(new String(Character.toChars(cp)));
					if (charStyle != null) {
						charText = charText.styled(s -> charStyle);
					}
					result.append(charText);
				}
				
				// If "Benötigt:" was not found, append at the end
				if (inserted[0] == false) {
					result.append(Text.literal("\n"));
					result.append(aspectNameText);
					result.append(Text.literal("\n"));
					result.append(shiftInfoText);
				}
				
				newHoverText = result;
			} else {
				// Could not extract original text, create new one with just aspect info
				Text aspectNameText = Text.literal("Enthält: ")
					.styled(style -> style.withColor(0xFFFFFFFF)) // White color
					.append(Text.literal(aspectName)
						.styled(style -> style.withColor(0xFFFCA800))); // Same color as overlay (#FCA800)
				
				Text shiftInfoText = Text.literal("(Shift für mehr Info)")
					.styled(style -> style.withColor(0xFFCCCCCC)); // Light gray color
				
				newHoverText = aspectNameText.copy()
					.append(Text.literal("\n"))
					.append(shiftInfoText);
			}
			
			// Validate that newHoverText is not null
			if (newHoverText == null) {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
		
		// Create new hover event
		HoverEvent newHoverEvent = createHoverEventForAspect(newHoverText);
		if (newHoverEvent == null) {
			return null; // Failed to create hover event
		}
		
		// Modify message with new hover event (preserving all formatting)
		Text modified = modifyTextWithHoverEvent(message, newHoverEvent);
		return modified;
	}
	
	/**
	 * Finds the style of the line containing the specified text
	 */
	private static net.minecraft.text.Style findStyleForLineContaining(Text text, String searchText) {
		if (text == null || searchText == null) {
			return null;
		}
		
		// Use OrderedText to iterate through the text and find the style at the position
		net.minecraft.text.OrderedText orderedText = text.asOrderedText();
		final net.minecraft.text.Style[] foundStyle = {null};
		final java.lang.StringBuilder currentLine = new java.lang.StringBuilder();
		
		orderedText.accept((index, style, codePoint) -> {
			char ch = (char) codePoint;
			
			if (ch == '\n') {
				// Check if current line contains the search text
				if (currentLine.toString().contains(searchText) && foundStyle[0] == null) {
					foundStyle[0] = style;
					return false; // Stop searching
				}
				currentLine.setLength(0); // Clear for next line
			} else {
				currentLine.appendCodePoint(codePoint);
				// Check if we found it in the current line
				if (currentLine.toString().contains(searchText) && foundStyle[0] == null) {
					foundStyle[0] = style;
					return false; // Stop searching
				}
			}
			
			return true; // Continue
		});
		
		return foundStyle[0];
	}
	
	/**
	 * Splits a Text component into individual lines while preserving formatting
	 */
	private static java.util.List<Text> splitTextIntoLinesWithFormatting(Text text) {
		java.util.List<Text> lines = new java.util.ArrayList<>();
		if (text == null) {
			return lines;
		}
		
		// Use OrderedText to preserve formatting while splitting
		net.minecraft.text.OrderedText orderedText = text.asOrderedText();
		java.util.List<java.lang.StringBuilder> lineBuilders = new java.util.ArrayList<>();
		java.util.List<net.minecraft.text.Style> lineStyles = new java.util.ArrayList<>();
		lineBuilders.add(new java.lang.StringBuilder());
		lineStyles.add(null);
		
		final int[] currentLineIndex = {0};
		final net.minecraft.text.Style[] currentStyle = {null};
		
		orderedText.accept((index, style, codePoint) -> {
			char ch = (char) codePoint;
			
			// Update current style
			currentStyle[0] = style;
			
			if (ch == '\n') {
				// Start a new line
				currentLineIndex[0]++;
				lineBuilders.add(new java.lang.StringBuilder());
				lineStyles.add(style); // Store style for the new line
			} else {
				// Append character to current line
				if (lineBuilders.size() <= currentLineIndex[0]) {
					lineBuilders.add(new java.lang.StringBuilder());
					lineStyles.add(style);
				}
				lineBuilders.get(currentLineIndex[0]).appendCodePoint(codePoint);
				// Update style for current line (use the last style in the line)
				if (lineStyles.size() > currentLineIndex[0]) {
					lineStyles.set(currentLineIndex[0], style);
				}
			}
			
			return true;
		});
		
		// Build Text objects for each line with preserved formatting
		for (int i = 0; i < lineBuilders.size(); i++) {
			String lineContent = lineBuilders.get(i).toString();
			net.minecraft.text.Style lineStyle = i < lineStyles.size() ? lineStyles.get(i) : null;
			
			if (lineContent.isEmpty()) {
				lines.add(Text.empty());
			} else {
				// Create line text - we'll preserve the style information separately
				// For now, create simple text (formatting is complex to preserve exactly)
				lines.add(Text.literal(lineContent));
			}
		}
		
		return lines;
	}
	
	/**
	 * Updates aspect overlay from blueprint name (for chat messages)
	 */
	public static void updateAspectOverlayFromBlueprintName(String blueprintName) {
		if (blueprintName == null || blueprintName.isEmpty()) {
			AspectOverlay.onHoverStopped();
			return;
		}
		
		// Get aspect information
		AspectInfo aspectInfo = getAspectInfoForBlueprintFull(blueprintName);
		
		if (aspectInfo == null) {
			AspectOverlay.onHoverStopped();
			return;
		}
		
		// Clean the blueprint name
		String cleanItemName = blueprintName.trim();
		
		// Remove leading dash/minus if present
		if (cleanItemName.startsWith("-")) {
			cleanItemName = cleanItemName.substring(1).trim();
		}
		
		// Remove trailing dash/minus if present
		if (cleanItemName.endsWith("-")) {
			cleanItemName = cleanItemName.substring(0, cleanItemName.length() - 1).trim();
		}
		
		// Remove Minecraft formatting codes and Unicode characters
		cleanItemName = cleanItemName.replaceAll("§[0-9a-fk-or]", "");
		cleanItemName = cleanItemName.replaceAll("[\\u3400-\\u4DBF]", "");
		cleanItemName = cleanItemName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		
		// Update overlay for chat
		AspectOverlay.updateAspectInfoFromNameForChat(cleanItemName, aspectInfo);
	}
	
	/**
	 * Updates aspect overlay from blueprint name with Text object (for color checking)
	 */
	public static void updateAspectOverlayFromBlueprintName(String blueprintName, Text textWithColor) {
		// Check if the text contains Epic colors - if so, don't show overlay
		if (textWithColor != null && hasEpicColor(textWithColor)) {
			AspectOverlay.onHoverStopped();
			return;
		}
		
		// Otherwise, proceed with normal update
		updateAspectOverlayFromBlueprintName(blueprintName);
	}
	
	/**
	 * Extracts item name from hover event in chat message
	 * Works for all items, not just blueprints
	 * Returns the first line of the hover event text, which is typically the item name
	 */
	public static String extractItemNameFromHoverEvent(Text message) {
		if (message == null) {
			return null;
		}
		
		// Find hover event in message
		HoverEvent hoverEvent = findHoverEventInText(message);
		if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) {
			return null;
		}
		
		// Extract hover text from event
		Text hoverText = extractHoverTextFromEvent(hoverEvent, message);
		if (hoverText == null) {
			return null;
		}
		
		// Get the first line of the hover text (typically the item name)
		String hoverTextString = hoverText.getString();
		if (hoverTextString == null || hoverTextString.isEmpty()) {
			return null;
		}
		
		// Split by newline and take the first line
		String[] lines = hoverTextString.split("\n");
		if (lines.length == 0) {
			return null;
		}
		
		String itemName = lines[0].trim();
		
		// Clean the item name
		// Remove leading dash/minus if present
		if (itemName.startsWith("-")) {
			itemName = itemName.substring(1).trim();
		}
		
		// Remove trailing dash/minus if present
		if (itemName.endsWith("-")) {
			itemName = itemName.substring(0, itemName.length() - 1).trim();
		}
		
		// Remove Minecraft formatting codes and Unicode characters
		itemName = itemName.replaceAll("§[0-9a-fk-or]", "");
		itemName = itemName.replaceAll("[\\u3400-\\u4DBF]", "");
		itemName = itemName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s-]", "").trim();
		
		// Remove "Du erhältst" if present
		if (itemName.contains("Du erhältst")) {
			int erhältstIndex = itemName.indexOf("Du erhältst");
			if (erhältstIndex >= 0) {
				itemName = itemName.substring(erhältstIndex + "Du erhältst".length()).trim();
			}
		}
		
		// Remove quantity patterns like "1x ", "2x ", "10x " at the start
		itemName = itemName.replaceAll("^\\d+x\\s+", "");
		
		if (itemName.isEmpty()) {
			return null;
		}
		
		return itemName;
	}
	
	/**
	 * Data class to store blueprint name and color information
	 */
	public static class BlueprintNameAndColor {
		public final String name;
		public final String rarity; // "epic" or "legendary" based on color
		
		public BlueprintNameAndColor(String name, String rarity) {
			this.name = name;
			this.rarity = rarity;
		}
	}
	
	/**
	 * Extracts blueprint name and color from chat message text
	 * Returns the name and rarity (epic or legendary) based on color
	 * Epic: #A134EB, Legendary: #FC7E00
	 */
	public static BlueprintNameAndColor extractBlueprintNameAndColorFromChatMessage(Text message) {
		if (message == null) {
			return null;
		}
		
		// Target colors: 
		// Epic: #A134EB (RGB: 161, 52, 235) OR #A335EE (RGB: 163, 53, 238) - both are used in game
		// Legendary: #FC7E00 (RGB: 252, 126, 0) OR #FF8000 (RGB: 255, 128, 0) - both are used in game
		int epicColor1 = 0xFFA134EB; // Original epic color
		int epicColor2 = 0xFFA335EE; // Actual epic color used in game
		int legendaryColor1 = 0xFFFC7E00; // Original legendary color
		int legendaryColor2 = 0xFFFF8000; // Actual legendary color used in game
		int epicColor1RGB = epicColor1 & 0x00FFFFFF;
		int epicColor2RGB = epicColor2 & 0x00FFFFFF;
		int legendaryColor1RGB = legendaryColor1 & 0x00FFFFFF;
		int legendaryColor2RGB = legendaryColor2 & 0x00FFFFFF;
		
		final StringBuilder blueprintName = new StringBuilder();
		final boolean[] inBlueprintNameColor = {false};
		final StringBuilder[] currentSection = {new StringBuilder()};
		final String[] detectedRarity = {null};
		
		// Convert to OrderedText to iterate through styled parts
		net.minecraft.text.OrderedText orderedText = message.asOrderedText();
		orderedText.accept((index, style, codePoint) -> {
			char ch = (char) codePoint;
			
			// Check if this style has one of the target colors
			boolean hasTargetColor = false;
			String currentRarity = null;
			if (style != null && style.getColor() != null) {
				int styleColor = style.getColor().getRgb();
				int styleColorRGB = styleColor & 0x00FFFFFF; // Remove alpha
				
				// Check for Epic color (both variants)
				if (styleColorRGB == epicColor1RGB || styleColorRGB == epicColor2RGB) {
					hasTargetColor = true;
					currentRarity = "epic";
				} else if (styleColorRGB == legendaryColor1RGB || styleColorRGB == legendaryColor2RGB) {
					hasTargetColor = true;
					currentRarity = "legendary";
				}
			}
			
			if (hasTargetColor) {
				// This text has the blueprint name color
				if (!inBlueprintNameColor[0]) {
					// Starting a new section
					inBlueprintNameColor[0] = true;
					currentSection[0] = new StringBuilder();
					if (detectedRarity[0] == null) {
						detectedRarity[0] = currentRarity;
					}
				}
				currentSection[0].appendCodePoint(codePoint);
			} else {
				// Color changed
				if (inBlueprintNameColor[0]) {
					// We were collecting blueprint name
					String section = currentSection[0].toString();
					
					// Check if this section matches "ZAHL x" pattern (e.g., "1x ", "2x ", "10x ")
					if (!section.matches("^\\d+x\\s*$")) {
						// Not a "ZAHL x" pattern, add to blueprint name
						if (blueprintName.length() > 0) {
							blueprintName.append(" ");
						}
						blueprintName.append(section);
					}
					
					currentSection[0] = new StringBuilder();
					inBlueprintNameColor[0] = false;
				}
			}
			
			return true; // Continue processing
		});
		
		// Handle last section if we were still collecting
		if (inBlueprintNameColor[0] && currentSection[0].length() > 0) {
			String section = currentSection[0].toString();
			if (!section.matches("^\\d+x\\s*$")) {
				if (blueprintName.length() > 0) {
					blueprintName.append(" ");
				}
				blueprintName.append(section);
			}
		}
		
		String result = blueprintName.toString().trim();
		
		// Clean up: remove leading/trailing dashes and spaces
		result = result.replaceAll("^[-\\s]+", "").replaceAll("[-\\s]+$", "");
		
		// Remove "Du erhältst" if present
		if (result.contains("Du erhältst")) {
			int erhältstIndex = result.indexOf("Du erhältst");
			if (erhältstIndex >= 0) {
				result = result.substring(erhältstIndex + "Du erhältst".length()).trim();
			}
		}
		
		// Return the result if we found a name with a target color
		if (!result.isEmpty() && detectedRarity[0] != null) {
			return new BlueprintNameAndColor(result, detectedRarity[0]);
		}
		
		return null;
	}
	
	/**
	 * Extracts blueprint name and color from item name text (for inventory)
	 * Returns the name and rarity (epic or legendary) based on color
	 * Epic: #A134EB or #A335EE, Legendary: #FC7E00 or #FF8000
	 * Only considers the color of the blueprint name itself (before "-" and "[Bauplan]")
	 */
	public static BlueprintNameAndColor extractBlueprintNameAndColorFromItemName(Text itemNameText) {
		if (itemNameText == null) {
			return null;
		}
		
		// Target colors: 
		// Epic: #A134EB (RGB: 161, 52, 235) OR #A335EE (RGB: 163, 53, 238) - both are used in game
		// Legendary: #FC7E00 (RGB: 252, 126, 0) OR #FF8000 (RGB: 255, 128, 0) - both are used in game
		int epicColor1 = 0xFFA134EB; // Original epic color
		int epicColor2 = 0xFFA335EE; // Actual epic color used in game
		int legendaryColor1 = 0xFFFC7E00; // Original legendary color
		int legendaryColor2 = 0xFFFF8000; // Actual legendary color used in game
		int epicColor1RGB = epicColor1 & 0x00FFFFFF;
		int epicColor2RGB = epicColor2 & 0x00FFFFFF;
		int legendaryColor1RGB = legendaryColor1 & 0x00FFFFFF;
		int legendaryColor2RGB = legendaryColor2 & 0x00FFFFFF;
		
		// First, collect all text with their styles to find the blueprint name section
		final java.util.List<net.minecraft.text.Style> styles = new java.util.ArrayList<>();
		final java.util.List<Integer> codePoints = new java.util.ArrayList<>();
		final StringBuilder fullText = new StringBuilder();
		
		net.minecraft.text.OrderedText orderedText = itemNameText.asOrderedText();
		orderedText.accept((index, style, codePoint) -> {
			styles.add(style);
			codePoints.add(codePoint);
			fullText.appendCodePoint(codePoint);
			return true;
		});
		
		// Find the blueprint name section (BEFORE "-" and "[Bauplan]")
		String fullTextStr = fullText.toString();
		int dashIndex = -1;
		int bauplanIndex = -1;
		
		// Find the dash before [Bauplan]
		int bauplanStart = fullTextStr.indexOf("[Bauplan]");
		if (bauplanStart > 0) {
			// Look for dash before [Bauplan]
			for (int i = bauplanStart - 1; i >= 0; i--) {
				char ch = fullTextStr.charAt(i);
				if (ch == '-' || ch == '—' || ch == '–') {
					dashIndex = i;
					break;
				} else if (ch != ' ') {
					break; // Stop if we hit non-space, non-dash character
				}
			}
			bauplanIndex = bauplanStart;
		}
		
		if (dashIndex < 0 || bauplanIndex < 0) {
			return null; // Could not find blueprint name section
		}
		
		// The blueprint name is BEFORE the dash, not between dash and [Bauplan]
		// So we need to extract from position 0 to dashIndex (excluding trailing spaces)
		
		// Extract the blueprint name section and check its color
		// The blueprint name is BEFORE the dash, so we extract from position 0 to dashIndex
		final StringBuilder blueprintName = new StringBuilder();
		final String[] detectedRarity = {null};
		
		// Build a mapping from string position to codePoint index
		int stringPos = 0;
		for (int i = 0; i < codePoints.size(); i++) {
			int codePoint = codePoints.get(i);
			net.minecraft.text.Style style = styles.get(i);
			
			// Check if this codePoint is in the blueprint name section (BEFORE the dash)
			if (stringPos < dashIndex) {
				char ch = (char) codePoint;
				
				blueprintName.appendCodePoint(codePoint);
				
				// Check the color only for the blueprint name section
				if (style != null && style.getColor() != null) {
					int styleColor = style.getColor().getRgb();
					int styleColorRGB = styleColor & 0x00FFFFFF; // Remove alpha
					if ((styleColorRGB == epicColor1RGB || styleColorRGB == epicColor2RGB) && detectedRarity[0] == null) {
						detectedRarity[0] = "epic";
					} else if ((styleColorRGB == legendaryColor1RGB || styleColorRGB == legendaryColor2RGB) && detectedRarity[0] == null) {
						detectedRarity[0] = "legendary";
					}
				}
			} else {
				// Stop if we've reached the dash
				if (stringPos >= dashIndex) {
					break;
				}
			}
			
			// Update string position based on codePoint
			if (codePoint < 0x10000) {
				stringPos++;
			} else {
				stringPos += 2; // Surrogate pair
			}
		}
		
		String result = blueprintName.toString().trim();
		
		// Remove Unicode formatting characters
		result = result.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "");
		
		// Remove any remaining formatting codes
		result = result.replaceAll("§[0-9a-fk-or]", "");
		
		// Remove trailing spaces, dashes, and other common suffixes
		result = result.replaceAll("\\s*-\\s*$", "").trim();
		
		// Return the result if we found a name with a target color
		if (!result.isEmpty() && detectedRarity[0] != null) {
			return new BlueprintNameAndColor(result, detectedRarity[0]);
		}
		
		return null;
	}
	
	/**
	 * Checks if a Text object contains any of the Epic colors (#A134EB or #A335EE)
	 * Returns true if any part of the text has one of these colors
	 */
	public static boolean hasEpicColor(Text text) {
		if (text == null) {
			return false;
		}
		
		// Epic colors: #A134EB (RGB: 161, 52, 235) OR #A335EE (RGB: 163, 53, 238)
		int epicColor1 = 0xFFA134EB;
		int epicColor2 = 0xFFA335EE;
		int epicColor1RGB = epicColor1 & 0x00FFFFFF;
		int epicColor2RGB = epicColor2 & 0x00FFFFFF;
		
		// Convert to OrderedText to iterate through styled parts
		net.minecraft.text.OrderedText orderedText = text.asOrderedText();
		final boolean[] hasEpicColor = {false};
		
		orderedText.accept((index, style, codePoint) -> {
			if (style != null && style.getColor() != null) {
				int styleColor = style.getColor().getRgb();
				int styleColorRGB = styleColor & 0x00FFFFFF; // Remove alpha
				
				if (styleColorRGB == epicColor1RGB || styleColorRGB == epicColor2RGB) {
					hasEpicColor[0] = true;
					return false; // Stop processing
				}
			}
			return true; // Continue processing
		});
		
		return hasEpicColor[0];
	}
	
	/**
	 * Extracts blueprint name from chat message text
	 * Looks for text with color #FC7E00 or #FF8000, but ignores "ZAHL x" patterns
	 * Works for all items with this color, not just blueprints
	 */
	public static String extractBlueprintNameFromChatMessage(Text message) {
		if (message == null) {
			return null;
		}
		
		// Target colors: #FC7E00 (RGB: 252, 126, 0) OR #FF8000 (RGB: 255, 128, 0) - both are used in game
		int targetColor1 = 0xFFFC7E00; // Original legendary color
		int targetColor2 = 0xFFFF8000; // Alternative legendary color
		int targetColor1RGB = targetColor1 & 0x00FFFFFF; // Remove alpha
		int targetColor2RGB = targetColor2 & 0x00FFFFFF; // Remove alpha
		
		StringBuilder blueprintName = new StringBuilder();
		final boolean[] inBlueprintNameColor = {false};
		final StringBuilder[] currentSection = {new StringBuilder()};
		
		// Convert to OrderedText to iterate through styled parts
		net.minecraft.text.OrderedText orderedText = message.asOrderedText();
		orderedText.accept((index, style, codePoint) -> {
			// Check if this style has one of the target colors
			boolean hasTargetColor = false;
			if (style != null && style.getColor() != null) {
				int styleColor = style.getColor().getRgb();
				int styleColorRGB = styleColor & 0x00FFFFFF; // Remove alpha
				if (styleColorRGB == targetColor1RGB || styleColorRGB == targetColor2RGB) {
					hasTargetColor = true;
				}
			}
			
			if (hasTargetColor) {
				// This text has the blueprint name color
				if (!inBlueprintNameColor[0]) {
					// Starting a new section
					inBlueprintNameColor[0] = true;
					currentSection[0] = new StringBuilder();
				}
				currentSection[0].appendCodePoint(codePoint);
			} else {
				// Color changed
				if (inBlueprintNameColor[0]) {
					// We were collecting blueprint name
					String section = currentSection[0].toString();
					
					// Check if this section matches "ZAHL x" pattern (e.g., "1x ", "2x ", "10x ")
					// Pattern: starts with digits, followed by "x" and optional space
					if (!section.matches("^\\d+x\\s*$")) {
						// Not a "ZAHL x" pattern, add to blueprint name
						if (blueprintName.length() > 0) {
							blueprintName.append(" ");
						}
						blueprintName.append(section);
					}
					
					currentSection[0] = new StringBuilder();
					inBlueprintNameColor[0] = false;
				}
			}
			
			return true; // Continue processing
		});
		
		// Handle last section if we were still collecting
		if (inBlueprintNameColor[0] && currentSection[0].length() > 0) {
			String section = currentSection[0].toString();
			if (!section.matches("^\\d+x\\s*$")) {
				if (blueprintName.length() > 0) {
					blueprintName.append(" ");
				}
				blueprintName.append(section);
			}
		}
		
		String result = blueprintName.toString().trim();
		
		// Clean up: remove leading/trailing dashes and spaces
		result = result.replaceAll("^[-\\s]+", "").replaceAll("[-\\s]+$", "");
		
		// Remove "Du erhältst" if present
		if (result.contains("Du erhältst")) {
			int erhältstIndex = result.indexOf("Du erhältst");
			if (erhältstIndex >= 0) {
				result = result.substring(erhältstIndex + "Du erhältst".length()).trim();
			}
		}
		
		// Remove "+ " prefix if present (e.g., "+ 1x Item")
		result = result.replaceAll("^\\+\\s*", "");
		
		// Remove quantity patterns like "1x ", "2x ", "10x " at the start
		// This handles cases where the entire section is "1x ItemName" in the same color
		result = result.replaceAll("^\\d+x\\s+", "");
		
		// Return the result if we found a name with the target color
		if (!result.isEmpty()) {
			return result;
		}
		
		return null;
	}
	
	/**
	 * Modifies a message's hover event to include aspect information
	 * Returns a new Text with modified hover event
	 */
	private static Text modifyMessageHoverEvent(Text message, AspectInfo aspectInfo) {
		if (message == null || aspectInfo == null) {
			return message;
		}
		
		// Check if there's an existing hover event (we can't read it, but we know it exists)
		HoverEvent existingHoverEvent = findHoverEventInText(message);
		boolean hasExistingHover = existingHoverEvent != null && existingHoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT;
		
		// In Minecraft 1.21.7, we cannot read the existing hover text, so we create a new one
		// with our aspect information. The original hover text will be replaced.
		
		// Create aspect information text
		Text aspectNameText = Text.literal("Enthält: ")
			.styled(style -> style.withColor(0xFFFFFFFF)) // White color
			.append(Text.literal(aspectInfo.aspectName)
				.styled(style -> style.withColor(0xFFFCA800))); // Same color as overlay (#FCA800)
		
		Text shiftInfoText = Text.literal("(Shift für mehr Info)")
			.styled(style -> style.withColor(0xFFCCCCCC)); // Light gray color
		
		// Create new hover text with our information
		// Note: Original hover text cannot be preserved in 1.21.7
		Text newHoverText = aspectNameText.copy()
			.append(Text.literal("\n"))
			.append(shiftInfoText);
		
		// Create new HoverEvent directly (constructor works at runtime even if not visible at compile time)
		HoverEvent newHoverEvent = createHoverEventDirect(newHoverText);
		
		if (newHoverEvent == null) {
			return message;
		}
		
		// Create new text with modified hover event
		Text modified = modifyTextWithHoverEvent(message, newHoverEvent);
		return modified;
	}
	
	/**
	 * Extracts the tooltip from a SHOW_ITEM HoverEvent by getting the ItemStack and generating its tooltip
	 */
	private static Text extractTooltipFromShowItem(HoverEvent hoverEvent) {
		if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_ITEM) {
			return null;
		}
		
		try {
			// Get the ShowItem value using method_10892 (same as ShowText)
			Object showItemValue = getShowTextValue(hoverEvent);
			if (showItemValue == null) {
				return null;
			}
			
			// Try to extract ItemStack from ShowItem
			Class<?> showItemClass = showItemValue.getClass();
			
			// Try all methods that might return ItemStack
			java.lang.reflect.Method[] methods = showItemClass.getDeclaredMethods();
			for (java.lang.reflect.Method method : methods) {
				if (method.getParameterCount() == 0 && !method.getReturnType().isPrimitive()) {
					try {
						method.setAccessible(true);
						Object result = method.invoke(showItemValue);
						if (result != null && result instanceof net.minecraft.item.ItemStack) {
							net.minecraft.item.ItemStack itemStack = (net.minecraft.item.ItemStack) result;
							
							// Get tooltip from ItemStack
							net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
							if (client != null && client.player != null) {
								// Use the same approach as SchmiedTrackerUtility - get tooltip via DataComponentTypes
								java.util.List<Text> tooltip = new java.util.ArrayList<>();
								// Add item name
								tooltip.add(itemStack.getName());
								
								// Get lore from DataComponentTypes (1.21.7 API)
								try {
									var loreComponent = itemStack.get(net.minecraft.component.DataComponentTypes.LORE);
									if (loreComponent != null) {
										tooltip.addAll(loreComponent.lines());
									}
								} catch (Exception e) {
									// Ignore
								}
								
								if (!tooltip.isEmpty()) {
									// Combine all tooltip lines into one Text
									net.minecraft.text.MutableText combined = Text.empty();
									boolean first = true;
									for (Text line : tooltip) {
										if (!first) {
											combined.append(Text.literal("\n"));
										}
										combined.append(line);
										first = false;
									}
									return combined;
								}
							}
						}
					} catch (Exception e) {
						// Continue to next method
					}
				}
			}
			
			// Try all fields that might contain ItemStack
			java.lang.reflect.Field[] fields = showItemClass.getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				Class<?> fieldType = field.getType();
				if (fieldType == net.minecraft.item.ItemStack.class || fieldType.getName().contains("ItemStack")) {
					try {
						field.setAccessible(true);
						Object value = field.get(showItemValue);
						if (value instanceof net.minecraft.item.ItemStack) {
							net.minecraft.item.ItemStack itemStack = (net.minecraft.item.ItemStack) value;
							
							// Get tooltip from ItemStack
							net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
							if (client != null && client.player != null) {
								// Use the same approach as SchmiedTrackerUtility - get tooltip via DataComponentTypes
								java.util.List<Text> tooltip = new java.util.ArrayList<>();
								// Add item name
								tooltip.add(itemStack.getName());
								
								// Get lore from DataComponentTypes (1.21.7 API)
								try {
									var loreComponent = itemStack.get(net.minecraft.component.DataComponentTypes.LORE);
									if (loreComponent != null) {
										tooltip.addAll(loreComponent.lines());
									}
								} catch (Exception e) {
									// Ignore
								}
								
								if (!tooltip.isEmpty()) {
									// Combine all tooltip lines into one Text
									net.minecraft.text.MutableText combined = Text.empty();
									boolean first = true;
									for (Text line : tooltip) {
										if (!first) {
											combined.append(Text.literal("\n"));
										}
										combined.append(line);
										first = false;
									}
									return combined;
								}
							}
						}
					} catch (Exception e) {
						// Continue to next field
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		
		return null;
	}
	
	/**
	 * Recursively searches for a HoverEvent in a Text component and its siblings
	 */
	private static HoverEvent findHoverEventInText(Text text) {
		if (text == null) {
			return null;
		}
		
		// Check the main text's style
		if (text.getStyle() != null) {
			HoverEvent hoverEvent = text.getStyle().getHoverEvent();
			if (hoverEvent != null) {
				// Accept both SHOW_TEXT and SHOW_ITEM
				if (hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT || hoverEvent.getAction() == HoverEvent.Action.SHOW_ITEM) {
					return hoverEvent;
				}
			}
		}
		
		// Recursively check siblings
		for (Text sibling : text.getSiblings()) {
			HoverEvent siblingHoverEvent = findHoverEventInText(sibling);
			if (siblingHoverEvent != null) {
				return siblingHoverEvent;
			}
		}
		
		return null;
	}
	
	/**
	 * Extracts the Text from a HoverEvent using multiple approaches
	 * Note: In Minecraft 1.21.7, the text is not stored in the HoverEvent itself,
	 * but is generated dynamically. We try to extract it from the ShowText object.
	 */
	private static Text extractHoverTextFromEvent(HoverEvent hoverEvent, Text message) {
		// Try to extract from HoverEvent first (this is what we want - the actual tooltip text)
		Text result = extractHoverTextFromEvent(hoverEvent);
		if (result != null) {
			return result;
		}
		
		// Fallback: If we can't extract from HoverEvent, try to extract from the message itself
		// (but this is NOT what we want - we want the tooltip text, not the chat message text)
		if (message != null) {
			Text textWithHover = findTextComponentWithHoverEvent(message, hoverEvent);
			if (textWithHover != null) {
				return textWithHover.copy();
			}
		}
		
		return null;
	}
	
	/**
	 * Finds the Text component in a message that has the specified HoverEvent
	 */
	private static Text findTextComponentWithHoverEvent(Text message, HoverEvent targetHoverEvent) {
		if (message == null || targetHoverEvent == null) {
			return null;
		}
		
		// Check if this text component has the hover event
		if (message.getStyle() != null && message.getStyle().getHoverEvent() == targetHoverEvent) {
			return message;
		}
		
		// Recursively check siblings
		for (Text sibling : message.getSiblings()) {
			Text result = findTextComponentWithHoverEvent(sibling, targetHoverEvent);
			if (result != null) {
				return result;
			}
		}
		
		return null;
	}
	
	/**
	 * Extracts the Text from a HoverEvent using multiple approaches
	 * Note: In Minecraft 1.21.7, the text is not stored in the HoverEvent itself,
	 * but is generated dynamically. We try to extract it from the ShowText object.
	 */
	private static Text extractHoverTextFromEvent(HoverEvent hoverEvent) {
		if (hoverEvent == null) {
			return null;
		}
		
		HoverEvent.Action action = hoverEvent.getAction();
		
		// Only handle SHOW_TEXT for now
		if (action != HoverEvent.Action.SHOW_TEXT) {
			return null;
		}
		
		// Try HoverEvent#getValue(Action) - the official API method
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
		
		// Try method getValue() without parameters (might exist in some versions)
		try {
			java.lang.reflect.Method getValueMethod = HoverEvent.class.getDeclaredMethod("getValue");
			getValueMethod.setAccessible(true);
			Object value = getValueMethod.invoke(hoverEvent);
			if (value instanceof Text) {
				return (Text) value;
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try method value() (for records)
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
		
		// Try reading all fields in HoverEvent
		try {
			java.lang.reflect.Field[] fields = HoverEvent.class.getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				// Skip static fields
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				
				Class<?> fieldType = field.getType();
				if (fieldType == Text.class || fieldType == Object.class) {
					try {
						field.setAccessible(true);
						Object value = field.get(hoverEvent);
						if (value instanceof Text) {
							return (Text) value;
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try ShowText inner class - FIRST get the ShowText value
		Object showTextValue = getShowTextValue(hoverEvent);
		if (showTextValue != null) {
			// CRITICAL: If showTextValue IS the hoverEvent itself, we need to extract from it directly
			// Don't recurse, just process it as ShowText
			if (showTextValue == hoverEvent) {
				// Continue with extraction below
			} else if (showTextValue instanceof HoverEvent) {
				Text recursiveResult = extractHoverTextFromEvent((HoverEvent) showTextValue);
				if (recursiveResult != null) {
					return recursiveResult;
				}
			}
			
			try {
				Class<?> showTextClass = showTextValue.getClass();
				
				// CRITICAL: Check if ShowText is a Record and extract via Record components
				if (showTextClass.isRecord()) {
					try {
						java.lang.reflect.RecordComponent[] components = showTextClass.getRecordComponents();
						for (java.lang.reflect.RecordComponent component : components) {
							try {
								Object value = component.getAccessor().invoke(showTextValue);
								if (value instanceof Text) {
									// Found a Text object! Copy it to create a fresh instance
									try {
										Text textValue = (Text) value;
										Text copiedText = textValue.copy();
										return copiedText;
									} catch (Exception e) {
										// If copy fails, return the original
										return (Text) value;
									}
								}
							} catch (Exception e) {
								// Ignore
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
				
				// CRITICAL: Try to find the Component object and serialize/deserialize it
				// This is the key approach: Component → JSON → Text (to get a fresh Text instance)
				// We need to find the actual Text/Component object first, then serialize/deserialize it
				try {
					// Try to find any Text object in the ShowText instance
					java.lang.reflect.Field[] showTextFields = showTextClass.getDeclaredFields();
					for (java.lang.reflect.Field field : showTextFields) {
						if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
							continue;
						}
						
						Class<?> fieldType = field.getType();
						// Look for fields that might contain a Component (Text or Object that could be Text)
						if (fieldType == Text.class || fieldType == Object.class || 
						    (fieldType.getName().contains("Text") && !fieldType.isPrimitive() && fieldType != String.class)) {
							try {
								field.setAccessible(true);
								Object value = field.get(showTextValue);
								
								if (value instanceof Text) {
									// Found a Text object! Copy it to create a fresh instance
									try {
										Text textValue = (Text) value;
										Text copiedText = textValue.copy();
										return copiedText;
									} catch (Exception e) {
										// If copy fails, return the original
										return (Text) value;
									}
								}
							} catch (Exception e) {
								// Continue to next field
							}
						}
					}
				} catch (Exception e) {
					// Ignore
				}
				
				// CRITICAL: Try method_10893() first - this is the obfuscated method that returns the Text!
				try {
					java.lang.reflect.Method method10893 = showTextClass.getDeclaredMethod("method_10893");
					method10893.setAccessible(true);
					Object textValue = method10893.invoke(showTextValue);
					if (textValue instanceof Text) {
						return (Text) textValue;
					}
				} catch (Exception e) {
					// Ignore
				}
				
				// Try method value() on ShowText (for records)
				try {
					java.lang.reflect.Method valueMethod = showTextClass.getDeclaredMethod("value");
					valueMethod.setAccessible(true);
					Object textValue = valueMethod.invoke(showTextValue);
					if (textValue instanceof Text) {
						return (Text) textValue;
					}
				} catch (Exception e) {
					// Ignore
				}
				
				// Try method text() or getText() on ShowText
				try {
					java.lang.reflect.Method textMethod = showTextClass.getDeclaredMethod("text");
					textMethod.setAccessible(true);
					Object textValue = textMethod.invoke(showTextValue);
					if (textValue instanceof Text) {
						return (Text) textValue;
					}
				} catch (Exception e) {
					// Try getText()
					try {
						java.lang.reflect.Method getTextMethod = showTextClass.getDeclaredMethod("getText");
						getTextMethod.setAccessible(true);
						Object textValue = getTextMethod.invoke(showTextValue);
						if (textValue instanceof Text) {
							return (Text) textValue;
						}
					} catch (Exception e2) {
						// Ignore
					}
				}
				
				// Try all fields in ShowText (excluding static)
				java.lang.reflect.Field[] showTextFields = showTextClass.getDeclaredFields();
				
				// CRITICAL: Check ALL fields first, including ALL types, to find the Text object
				for (java.lang.reflect.Field field : showTextFields) {
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					
					Class<?> fieldType = field.getType();
					
					try {
						field.setAccessible(true);
						Object value = field.get(showTextValue);
						
						// Check if it's Text directly
						if (value instanceof Text) {
							Text textValue = (Text) value;
							try {
								Text copiedText = textValue.copy();
								return copiedText;
							} catch (Exception e) {
								return textValue;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
				
				// First pass: Check fields with Text type (highest priority)
				for (java.lang.reflect.Field field : showTextFields) {
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					
					Class<?> fieldType = field.getType();
					if (fieldType == Text.class || (fieldType.getName().contains("Text") && !fieldType.isPrimitive() && fieldType != String.class)) {
						try {
							field.setAccessible(true);
							Object value = field.get(showTextValue);
							if (value instanceof Text) {
								return (Text) value;
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
				
				// Second pass: Check ALL fields (including primitives for debugging, but only process non-primitives)
				// IMPORTANT: We need to check ALL 9 fields to find the Text!
				for (java.lang.reflect.Field field : showTextFields) {
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					
					Class<?> fieldType = field.getType();
					
					// Skip if already checked in first pass
					if (fieldType == Text.class || (fieldType.getName().contains("Text") && !fieldType.isPrimitive() && fieldType != String.class)) {
						continue;
					}
					
					// Check all non-primitive fields for Text
					if (!fieldType.isPrimitive()) {
						try {
							field.setAccessible(true);
							Object value = field.get(showTextValue);
							
							// Check if it's Text directly
							if (value instanceof Text) {
								return (Text) value;
							}
							
							// Check if it's a String (but skip action names)
							if (value instanceof String) {
								String stringValue = (String) value;
								if (!stringValue.equals("show_text") && !stringValue.equals("<action show_text>") && 
								    !stringValue.equals("SHOW_TEXT") && !stringValue.isEmpty()) {
									// Use as literal text
									return Text.literal(stringValue);
								}
							}
							
							// Check if it's a List of Text components
							if (value instanceof java.util.List) {
								java.util.List<?> list = (java.util.List<?>) value;
								if (!list.isEmpty() && list.get(0) instanceof Text) {
									// Combine all Text components into one
									net.minecraft.text.MutableText combined = Text.empty();
									for (Object item : list) {
										if (item instanceof Text) {
											combined.append((Text) item);
										}
									}
									return combined;
								}
							}
							
							// Check if it's an object that might contain Text (recursive search)
							if (value != null && !(value instanceof String) && !(value instanceof java.util.List) && 
							    !value.getClass().isPrimitive() && !value.getClass().getName().startsWith("java.")) {
								// Try to find Text in nested object
								try {
									java.lang.reflect.Field[] nestedFields = value.getClass().getDeclaredFields();
									for (java.lang.reflect.Field nestedField : nestedFields) {
										if (java.lang.reflect.Modifier.isStatic(nestedField.getModifiers())) {
											continue;
										}
										if (nestedField.getType() == Text.class) {
											try {
												nestedField.setAccessible(true);
												Object nestedValue = nestedField.get(value);
												if (nestedValue instanceof Text) {
													return (Text) nestedValue;
												}
											} catch (Exception e) {
												// Continue
											}
										}
									}
								} catch (Exception e) {
									// Continue
								}
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
				
				// Try all methods that might return Text (including inherited methods)
				java.lang.reflect.Method[] showTextMethods = showTextClass.getDeclaredMethods();
				java.lang.reflect.Method[] allMethods = showTextClass.getMethods();
				
				// Try declared methods first
				for (java.lang.reflect.Method method : showTextMethods) {
					if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
						continue;
					}
					// Check ALL methods that take no parameters and return non-primitive (including String)
					if (method.getParameterCount() == 0 && !method.getReturnType().isPrimitive() && method.getReturnType() != Void.TYPE) {
						try {
							method.setAccessible(true);
							Object textValue = method.invoke(showTextValue);
							if (textValue instanceof Text) {
								return (Text) textValue;
							} else if (textValue instanceof String) {
								// Maybe the text is stored as a String? Use it as literal (but skip action names)
								String stringValue = (String) textValue;
								if (!stringValue.equals("show_text") && !stringValue.equals("<action show_text>") && 
								    !stringValue.equals("SHOW_TEXT") && !stringValue.isEmpty()) {
									// Use as literal text
									return Text.literal(stringValue);
								}
							} else if (textValue instanceof java.util.List) {
								// Maybe it's a list of Text components?
								java.util.List<?> list = (java.util.List<?>) textValue;
								if (!list.isEmpty() && list.get(0) instanceof Text) {
									// Combine all Text components into one
									net.minecraft.text.MutableText combined = Text.empty();
									for (Object item : list) {
										if (item instanceof Text) {
											combined.append((Text) item);
										}
									}
									return combined;
								}
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
				
				// Also try inherited methods
				for (java.lang.reflect.Method method : allMethods) {
					if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
						continue;
					}
					// Skip if already checked in declared methods
					boolean alreadyChecked = false;
					for (java.lang.reflect.Method declaredMethod : showTextMethods) {
						if (declaredMethod.getName().equals(method.getName()) && 
						    java.util.Arrays.equals(declaredMethod.getParameterTypes(), method.getParameterTypes())) {
							alreadyChecked = true;
							break;
						}
					}
					if (alreadyChecked) continue;
					
					// Check ALL methods that take no parameters and return non-primitive (including String)
					if (method.getParameterCount() == 0 && !method.getReturnType().isPrimitive() && method.getReturnType() != Void.TYPE) {
						try {
							method.setAccessible(true);
							Object textValue = method.invoke(showTextValue);
							if (textValue instanceof Text) {
								return (Text) textValue;
							} else if (textValue instanceof String) {
								// Maybe the text is stored as a String? Use it as literal (but skip action names)
								String stringValue = (String) textValue;
								if (!stringValue.equals("show_text") && !stringValue.equals("<action show_text>") && 
								    !stringValue.equals("SHOW_TEXT") && !stringValue.isEmpty()) {
									// Use as literal text
									return Text.literal(stringValue);
								}
							} else if (textValue instanceof java.util.List) {
								// Maybe it's a list of Text components?
								java.util.List<?> list = (java.util.List<?>) textValue;
								if (!list.isEmpty() && list.get(0) instanceof Text) {
									// Combine all Text components into one
									net.minecraft.text.MutableText combined = Text.empty();
									for (Object item : list) {
										if (item instanceof Text) {
											combined.append((Text) item);
										}
									}
									return combined;
								}
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the ShowText value from a HoverEvent
	 * Tries various methods to extract the ShowText instance
	 */
	private static Object getShowTextValue(HoverEvent hoverEvent) {
		try {
			// CRITICAL: In Minecraft 1.21.7, HoverEvent is a sealed interface/class
			// If the action is SHOW_TEXT, the HoverEvent itself IS the ShowText object!
			HoverEvent.Action action = hoverEvent.getAction();
			if (action == HoverEvent.Action.SHOW_TEXT) {
				// Check if the HoverEvent itself is a ShowText instance
				String className = hoverEvent.getClass().getName();
				if (className.contains("ShowText")) {
					return hoverEvent;
				}
				
				// Check if it's an inner class of HoverEvent and has a Text constructor (ShowText has this)
				Class<?> hoverEventClass = hoverEvent.getClass();
				Class<?> enclosingClass = hoverEventClass.getEnclosingClass();
				if (enclosingClass == HoverEvent.class || (enclosingClass != null && enclosingClass.getName().contains("HoverEvent"))) {
					// It's an inner class of HoverEvent - check if it has a constructor that takes Text
					try {
						java.lang.reflect.Constructor<?>[] constructors = hoverEventClass.getDeclaredConstructors();
						for (java.lang.reflect.Constructor<?> constructor : constructors) {
							Class<?>[] paramTypes = constructor.getParameterTypes();
							if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
								return hoverEvent;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
				
				// Try to cast it to ShowText by class name
				try {
					Class<?> showTextClass = Class.forName("net.minecraft.text.HoverEvent$ShowText");
					if (showTextClass.isInstance(hoverEvent)) {
						return hoverEvent;
					}
				} catch (Exception e) {
					// Ignore
				}
			}
			
			// Try method value() first (for records)
			try {
				java.lang.reflect.Method valueMethod = HoverEvent.class.getDeclaredMethod("value");
				valueMethod.setAccessible(true);
				Object value = valueMethod.invoke(hoverEvent);
				if (value != null) {
					return value;
				}
			} catch (Exception e) {
				// Ignore
			}
			
			// Try all declared fields (excluding static)
			java.lang.reflect.Field[] fields = HoverEvent.class.getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				// Skip static fields
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				
				Class<?> fieldType = field.getType();
				String fieldTypeName = fieldType.getName();
				
				if (fieldTypeName.contains("ShowText") || fieldType == Object.class || !fieldType.isPrimitive()) {
					try {
						field.setAccessible(true);
						Object value = field.get(hoverEvent);
						if (value != null) {
							// Check if it's a ShowText-like object
							String className = value.getClass().getName();
							
							boolean isShowText = false;
							if (className.contains("ShowText")) {
								isShowText = true;
							} else {
								// Check if it's an inner class of HoverEvent and assignable to HoverEvent with Text constructor
								Class<?> valueClass = value.getClass();
								Class<?> enclosingClass = valueClass.getEnclosingClass();
								if (enclosingClass == HoverEvent.class || (enclosingClass != null && enclosingClass.getName().contains("HoverEvent"))) {
									if (HoverEvent.class.isAssignableFrom(valueClass)) {
										try {
											java.lang.reflect.Constructor<?>[] constructors = valueClass.getDeclaredConstructors();
											for (java.lang.reflect.Constructor<?> constructor : constructors) {
												Class<?>[] paramTypes = constructor.getParameterTypes();
												if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
													isShowText = true;
													break;
												}
											}
										} catch (Exception e) {
											// Continue
										}
									}
								}
							}
							
							if (isShowText) {
								return value;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
			
			// Try all methods that might return the value
			java.lang.reflect.Method[] methods = HoverEvent.class.getDeclaredMethods();
			
			// First, try methods that return ShowText directly (by return type name)
			for (java.lang.reflect.Method method : methods) {
				// Skip static methods
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
					continue;
				}
				// Skip getAction() - that returns Action, not ShowText!
				if (method.getName().equals("getAction")) {
					continue;
				}
				// Check if return type contains ShowText
				String returnTypeName = method.getReturnType().getName();
				if (returnTypeName.contains("ShowText")) {
					try {
						method.setAccessible(true);
						Object value = method.invoke(hoverEvent);
						if (value != null) {
							if (value.getClass().getName().contains("ShowText")) {
								return value;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
			
			// Then try all other methods that might return ShowText
			for (java.lang.reflect.Method method : methods) {
				// Skip static methods
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
					continue;
				}
				// Skip getAction() - that returns Action, not ShowText!
				if (method.getName().equals("getAction")) {
					continue;
				}
				// Look for methods that take no parameters and return non-primitive
				if (method.getParameterCount() == 0 && !method.getReturnType().isPrimitive() && method.getReturnType() != Void.TYPE) {
					try {
						method.setAccessible(true);
						Object value = method.invoke(hoverEvent);
						if (value != null) {
							// Check if it's a ShowText-like object
							String className = value.getClass().getName();
							
							// CRITICAL: Check if it contains "ShowText" OR if it's an inner class of HoverEvent that is assignable to HoverEvent
							// (ShowText is assignable to HoverEvent, but Action is NOT)
							boolean isShowText = false;
							
							if (className.contains("ShowText")) {
								isShowText = true;
							} else {
								// Check if it's an inner class of HoverEvent and assignable to HoverEvent
								// This handles obfuscated ShowText classes
								Class<?> valueClass = value.getClass();
								Class<?> enclosingClass = valueClass.getEnclosingClass();
								if (enclosingClass == HoverEvent.class || (enclosingClass != null && enclosingClass.getName().contains("HoverEvent"))) {
									// It's an inner class of HoverEvent
									// Check if it's assignable to HoverEvent (ShowText is, Action is NOT)
									if (HoverEvent.class.isAssignableFrom(valueClass)) {
										// Check if it has a constructor that takes Text (ShowText has this, Action doesn't)
										try {
											java.lang.reflect.Constructor<?>[] constructors = valueClass.getDeclaredConstructors();
											for (java.lang.reflect.Constructor<?> constructor : constructors) {
												Class<?>[] paramTypes = constructor.getParameterTypes();
												if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
													isShowText = true;
													break;
												}
											}
										} catch (Exception e) {
											// Continue
										}
									}
								}
							}
							
							if (isShowText) {
								return value;
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		return null;
	}
	
	/**
	 * Creates a HoverEvent for aspect information
	 * Uses the same approach as TestUtility.createHoverEvent
	 */
	private static HoverEvent createHoverEventForAspect(Text hoverText) {
		if (hoverText == null) {
			return null;
		}
		
		// Use createHoverEventDirect which has more comprehensive logic
		HoverEvent result = createHoverEventDirect(hoverText);
		if (result != null) {
			return result;
		}
		
		// Try HoverEvent.showText() static factory method first
		try {
			java.lang.reflect.Method showTextMethod = HoverEvent.class.getDeclaredMethod("showText", Text.class);
			showTextMethod.setAccessible(true);
			HoverEvent result2 = (HoverEvent) showTextMethod.invoke(null, hoverText);
			return result2;
		} catch (Exception e) {
			// Ignore
		}
		
		// Try all static methods in HoverEvent
		try {
			for (java.lang.reflect.Method method : HoverEvent.class.getDeclaredMethods()) {
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
					Class<?>[] paramTypes = method.getParameterTypes();
					if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
						try {
							method.setAccessible(true);
							HoverEvent result3 = (HoverEvent) method.invoke(null, hoverText);
							return result3;
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Try using HoverEvent$ShowText inner class
		try {
			Class<?> showTextClass = Class.forName("net.minecraft.text.HoverEvent$ShowText");
			
			// Try ShowText constructors
			java.lang.reflect.Constructor<?>[] constructors = showTextClass.getDeclaredConstructors();
			for (java.lang.reflect.Constructor<?> constructor : constructors) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
					try {
						constructor.setAccessible(true);
						Object showTextInstance = constructor.newInstance(hoverText);
						
						// Check if ShowText is assignable to HoverEvent (sealed interface/class pattern)
						if (HoverEvent.class.isAssignableFrom(showTextInstance.getClass())) {
							return (HoverEvent) showTextInstance;
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		
		return null;
	}
	
	/**
	 * Gets the text from a HoverEvent by reading fields directly
	 * In Minecraft 1.21.7, getValue() doesn't exist, so we try to read fields
	 */
	private static Text getHoverEventText(HoverEvent hoverEvent) {
		if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) {
			return null;
		}
		
		// Try to read fields directly
		try {
			java.lang.reflect.Field[] fields = HoverEvent.class.getDeclaredFields();
			for (java.lang.reflect.Field field : fields) {
				Class<?> fieldType = field.getType();
				if (fieldType == Text.class || fieldType == Object.class) {
					try {
						field.setAccessible(true);
						Object value = field.get(hoverEvent);
						if (value instanceof Text) {
							return (Text) value;
						}
					} catch (Exception e) {
						// Continue to next field
					}
				}
			}
		} catch (Exception e) {
		}
		return null;
	}
	
	/**
	 * Creates a HoverEvent directly using the constructor
	 * In Minecraft 1.21.7, the constructor works at runtime even if not visible at compile time
	 */
	private static HoverEvent createHoverEventDirect(Text hoverText) {
		if (hoverText == null) {
			return null;
		}
		
		// First, try to find ShowText class dynamically by searching inner classes
		Class<?> showTextClass = null;
		try {
			// Try direct name first (for non-obfuscated environments)
			showTextClass = Class.forName("net.minecraft.text.HoverEvent$ShowText");
		} catch (ClassNotFoundException e) {
			// If direct name fails, search inner classes dynamically
			Class<?>[] innerClasses = HoverEvent.class.getDeclaredClasses();
			for (Class<?> innerClass : innerClasses) {
				// Check if this inner class is assignable to HoverEvent or has a constructor that takes Text
				java.lang.reflect.Constructor<?>[] constructors = innerClass.getDeclaredConstructors();
				for (java.lang.reflect.Constructor<?> constructor : constructors) {
					Class<?>[] paramTypes = constructor.getParameterTypes();
					if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
						// This looks like ShowText!
						showTextClass = innerClass;
						break;
					}
				}
				if (showTextClass != null) break;
			}
		}
		
		// Try using ShowText inner class if we found it
		if (showTextClass != null) {
			try {
			
			// Check for static factory methods in ShowText
			for (java.lang.reflect.Method method : showTextClass.getDeclaredMethods()) {
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
					Class<?>[] paramTypes = method.getParameterTypes();
					if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
						try {
							method.setAccessible(true);
							HoverEvent result = (HoverEvent) method.invoke(null, hoverText);
							return result;
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			}
			
			// Try constructors in ShowText
			java.lang.reflect.Constructor<?>[] constructors = showTextClass.getDeclaredConstructors();
			
			for (java.lang.reflect.Constructor<?> constructor : constructors) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 1 && paramTypes[0] == Text.class) {
					try {
						constructor.setAccessible(true);
						Object showTextInstance = constructor.newInstance(hoverText);
						
						// Check if ShowText is directly assignable to HoverEvent (sealed interface/class pattern)
						if (HoverEvent.class.isAssignableFrom(showTextInstance.getClass())) {
							return (HoverEvent) showTextInstance;
						}
						
						// Check interfaces
						Class<?>[] interfaces = showTextClass.getInterfaces();
						for (Class<?> iface : interfaces) {
							if (iface == HoverEvent.class || HoverEvent.class.isAssignableFrom(iface)) {
								return (HoverEvent) showTextInstance;
							}
						}
						
						// Check superclass
						Class<?> superclass = showTextClass.getSuperclass();
						if (superclass != null) {
							if (superclass == HoverEvent.class || HoverEvent.class.isAssignableFrom(superclass)) {
								return (HoverEvent) showTextInstance;
							}
						}
						
						// Check if ShowText has a method to create HoverEvent
						for (java.lang.reflect.Method method : showTextClass.getDeclaredMethods()) {
							if (method.getReturnType() == HoverEvent.class) {
								Class<?>[] methodParamTypes = method.getParameterTypes();
								try {
									method.setAccessible(true);
									HoverEvent result = (HoverEvent) method.invoke(showTextInstance);
									return result;
								} catch (Exception e) {
									// Ignore
								}
							}
						}
						
						// Check if ShowText implements an interface or extends a class that has a method to create HoverEvent
						Class<?>[] showTextInterfaces = showTextClass.getInterfaces();
						for (Class<?> iface : showTextInterfaces) {
							for (java.lang.reflect.Method method : iface.getMethods()) {
								if (method.getReturnType() == HoverEvent.class) {
									try {
										HoverEvent result = (HoverEvent) method.invoke(showTextInstance);
										return result;
									} catch (Exception e) {
										// Ignore
									}
								}
							}
						}
						
						// Try to find a static method in HoverEvent that takes ShowText or Object
						for (java.lang.reflect.Method method : HoverEvent.class.getDeclaredMethods()) {
							if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && 
							    method.getReturnType() == HoverEvent.class) {
								Class<?>[] methodParamTypes = method.getParameterTypes();
								if (methodParamTypes.length == 1) {
									if (methodParamTypes[0].isAssignableFrom(showTextInstance.getClass()) || 
									    methodParamTypes[0] == Object.class ||
									    methodParamTypes[0] == HoverEvent.Action.class) {
										try {
											method.setAccessible(true);
											Object param = methodParamTypes[0] == HoverEvent.Action.class ? HoverEvent.Action.SHOW_TEXT : showTextInstance;
											HoverEvent result = (HoverEvent) method.invoke(null, param);
											return result;
										} catch (Exception e) {
											// Ignore
										}
									}
								} else if (methodParamTypes.length == 2 && 
								          methodParamTypes[0] == HoverEvent.Action.class &&
								          (methodParamTypes[1].isAssignableFrom(showTextInstance.getClass()) || methodParamTypes[1] == Object.class)) {
									try {
										method.setAccessible(true);
										HoverEvent result = (HoverEvent) method.invoke(null, HoverEvent.Action.SHOW_TEXT, showTextInstance);
										return result;
									} catch (Exception e) {
										// Ignore
									}
								}
							}
						}
						
						// Now try to create HoverEvent with ShowText instance using reflection
						java.lang.reflect.Constructor<?>[] hoverEventConstructors = HoverEvent.class.getDeclaredConstructors();
						for (java.lang.reflect.Constructor<?> hoverEventConstructor : hoverEventConstructors) {
							Class<?>[] hoverEventParamTypes = hoverEventConstructor.getParameterTypes();
							if (hoverEventParamTypes.length == 2 && 
							    hoverEventParamTypes[0] == HoverEvent.Action.class &&
							    hoverEventParamTypes[1].isAssignableFrom(showTextInstance.getClass())) {
								try {
									hoverEventConstructor.setAccessible(true);
									HoverEvent result = (HoverEvent) hoverEventConstructor.newInstance(HoverEvent.Action.SHOW_TEXT, showTextInstance);
									return result;
								} catch (Exception e) {
									// Ignore
								}
							}
						}
					} catch (Exception e) {
						// Ignore
					}
				}
			}
			} catch (Exception e) {
				// Ignore
			}
		}
		
		// Fallback: Try using reflection on HoverEvent directly
		HoverEvent result = createHoverEventWithReflection(hoverText);
		return result;
	}
	
	/**
	 * Creates a HoverEvent using reflection as fallback
	 * In 1.21.7, HoverEvent can be created with: new HoverEvent(Action, Text)
	 * We use reflection since the constructor may not be visible at compile time
	 */
	private static HoverEvent createHoverEventWithReflection(Text hoverText) {
		
		// Check if HoverEvent is a record
		boolean isRecord = HoverEvent.class.isRecord();
		
		// List all constructors for debugging
		java.lang.reflect.Constructor<?>[] declaredConstructors = HoverEvent.class.getDeclaredConstructors();
		for (java.lang.reflect.Constructor<?> constructor : declaredConstructors) {
			Class<?>[] paramTypes = constructor.getParameterTypes();
		}
		
		java.lang.reflect.Constructor<?>[] publicConstructors = HoverEvent.class.getConstructors();
		for (java.lang.reflect.Constructor<?> constructor : publicConstructors) {
			Class<?>[] paramTypes = constructor.getParameterTypes();
		}
		
		// List ALL methods of HoverEvent for debugging
		for (java.lang.reflect.Method method : HoverEvent.class.getMethods()) {
		}
		
		// List all static methods that return HoverEvent
		for (java.lang.reflect.Method method : HoverEvent.class.getDeclaredMethods()) {
			if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
				Class<?>[] paramTypes = method.getParameterTypes();
			}
		}
		
		// Check if there are inner classes
		for (Class<?> innerClass : HoverEvent.class.getDeclaredClasses()) {
			// Check for static factory methods in inner classes
			for (java.lang.reflect.Method method : innerClass.getDeclaredMethods()) {
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
					Class<?>[] paramTypes = method.getParameterTypes();
				}
			}
		}
		
		try {
			// Try to find and call the constructor
			for (java.lang.reflect.Constructor<?> constructor : declaredConstructors) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 2 && paramTypes[0] == HoverEvent.Action.class) {
					try {
						constructor.setAccessible(true);
						HoverEvent result = (HoverEvent) constructor.newInstance(HoverEvent.Action.SHOW_TEXT, hoverText);
						return result;
					} catch (Exception e) {
						// Silent error handling
					}
				}
			}
			
			// Also try public constructors
			for (java.lang.reflect.Constructor<?> constructor : publicConstructors) {
				Class<?>[] paramTypes = constructor.getParameterTypes();
				if (paramTypes.length == 2 && paramTypes[0] == HoverEvent.Action.class) {
					try {
						HoverEvent result = (HoverEvent) constructor.newInstance(HoverEvent.Action.SHOW_TEXT, hoverText);
						return result;
					} catch (Exception e) {
						// Silent error handling
					}
				}
			}
			
			// Try static factory methods
			for (java.lang.reflect.Method method : HoverEvent.class.getDeclaredMethods()) {
				if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getReturnType() == HoverEvent.class) {
					Class<?>[] paramTypes = method.getParameterTypes();
					if (paramTypes.length == 2 && paramTypes[0] == HoverEvent.Action.class) {
						try {
							method.setAccessible(true);
							HoverEvent result = (HoverEvent) method.invoke(null, HoverEvent.Action.SHOW_TEXT, hoverText);
							return result;
						} catch (Exception e) {
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
		return null;
	}
	
	/**
	 * Recursively modifies a Text component to replace hover events while preserving all formatting.
	 * Only replaces hover events where they exist (SHOW_TEXT action).
	 */
	private static Text modifyTextWithHoverEvent(Text text, HoverEvent newHoverEvent) {
		if (text == null) return text;
		
		// Copy the text to preserve its structure - copy() returns MutableText which has setStyle()
		net.minecraft.text.MutableText newText = text.copy();
		
		// Check if this text component has a hover event that needs to be replaced
		Style currentStyle = text.getStyle();
		if (currentStyle != null && currentStyle.getHoverEvent() != null) {
			// This text has a hover event - replace it with the new one
			Style newStyle = currentStyle.withHoverEvent(newHoverEvent);
			newText.setStyle(newStyle);
		}
		// If no hover event, the style is already preserved by copy()
		
		// Recursively process all siblings to preserve their formatting
		newText.getSiblings().clear(); // Clear existing siblings to avoid duplication
		for (Text sibling : text.getSiblings()) {
			Text modifiedSibling = modifyTextWithHoverEvent(sibling, newHoverEvent);
			newText.getSiblings().add(modifiedSibling);
		}
		
		return newText;
	}
	
	/**
	 * Gets the item under the mouse cursor (simplified implementation)
	 */
	private static ItemStack getHoveredItem(MinecraftClient client) {
		// This is a simplified approach - in a real implementation you'd need to
		// check the actual GUI elements and their item stacks
		// For now, we'll return an empty stack to indicate no item found
		return ItemStack.EMPTY;
	}
	
	/**
	 * Checks for aspect information in blueprint items and adds it to the tooltip
	 */
	private static void checkForAspectInformation(List<Text> lines, MinecraftClient client) {
		// Check if aspect overlay is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().aspectOverlayEnabled) {
			return; // Don't show aspect information if aspect overlay is disabled
		}
		
		// Find blueprint line and add aspect information
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			// Skip if lineText is null or empty
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}
			
			// Check if this is a blueprint line
			if (lineText.contains("[Bauplan]")) {
				
				// Extract the item name (everything before "[Bauplan]")
				String itemName = lineText.substring(0, lineText.indexOf("[Bauplan]")).trim();
				
				// Remove leading dash/minus if present
				if (itemName.startsWith("-")) {
					itemName = itemName.substring(1).trim();
				}
				
				// Remove trailing dash/minus if present
				if (itemName.endsWith("-")) {
					itemName = itemName.substring(0, itemName.length() - 1).trim();
				}
				
				// Remove Minecraft formatting codes and Unicode characters
				itemName = itemName.replaceAll("§[0-9a-fk-or]", "");
				itemName = itemName.replaceAll("[\\u3400-\\u4DBF]", "");
				itemName = itemName.replaceAll("[^a-zA-ZäöüßÄÖÜ\\s]", "").trim();
				
				// Look for this item in the aspects database
				AspectInfo aspectInfo = aspectsDatabase.get(itemName);
				
				if (aspectInfo != null && !aspectInfo.aspectName.isEmpty()) {
					// Calculate position: 5th line from bottom (2 lines higher)
					int targetPosition = Math.max(0, lines.size() - 5);
					
					// Create aspect name text
					Text aspectNameText = Text.literal("Enthält: ")
						.styled(style -> style.withColor(0xFFFFFFFF)) // White color
						.append(Text.literal(aspectInfo.aspectName)
							.styled(style -> style.withColor(0xFFFCA800))); // Same color as overlay (#FCA800)
					
					// Insert aspect name at the target position
					lines.add(targetPosition, aspectNameText);
					
					// Add empty line after aspect name
					Text emptyLineText = Text.literal(" ");
					lines.add(targetPosition + 1, emptyLineText);
					
					// Add aspect description right after the empty line (only if shift is pressed)
					boolean isShiftPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), 
																   InputUtil.GLFW_KEY_LEFT_SHIFT) || 
											InputUtil.isKeyPressed(client.getWindow().getHandle(), 
																   InputUtil.GLFW_KEY_RIGHT_SHIFT);
					
					if (isShiftPressed && !aspectInfo.aspectDescription.isEmpty()) {
						Text aspectDescText = Text.literal("  ")
							.styled(style -> style.withColor(0xC0C0C0)) // Light gray
							.append(Text.literal(aspectInfo.aspectDescription)
								.styled(style -> style.withColor(0xFFFFFF))); // White color
						
						// Insert description after the empty line
						lines.add(targetPosition + 2, aspectDescText);
					}
					
					break; // Only process the first blueprint line
				}
			}
		}
	}
	
	/**
	 * Loads the materials database from materials.json
	 */
	private static void loadMaterialsDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(MATERIALS_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Materials config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					JsonObject floors = json.getAsJsonObject("floors");
					
					for (String floorKey : floors.keySet()) {
						JsonObject floorData = floors.getAsJsonObject(floorKey);
						
						// Extract floor number
						int floorNumber = Integer.parseInt(floorKey.replace("floor_", ""));
						
						// Add mob name to database
						if (floorData.has("mob")) {
							var mobElement = floorData.get("mob");
							// Support both string and array formats
							if (mobElement.isJsonArray()) {
								// Array format: ["Name1", "Name2", ...]
								com.google.gson.JsonArray mobArray = mobElement.getAsJsonArray();
								for (var element : mobArray) {
									String mobName = element.getAsString();
									if (!mobName.isEmpty()) {
										materialsDatabase.put(mobName, new MaterialInfo(floorNumber, "mob", "WHITE"));
									}
								}
							} else {
								// String format: "Name" (backward compatibility)
								String mobName = mobElement.getAsString();
								if (!mobName.isEmpty()) {
									materialsDatabase.put(mobName, new MaterialInfo(floorNumber, "mob", "WHITE"));
								}
							}
						}
						
						// Add materials to database
						JsonObject materials = floorData.getAsJsonObject("materials");
						for (String rarityKey : materials.keySet()) {
							JsonObject rarityData = materials.getAsJsonObject(rarityKey);
							String color = rarityData.get("color").getAsString();
							com.google.gson.JsonArray materialsArray = rarityData.getAsJsonArray("materials");
							
							for (var element : materialsArray) {
								String materialName = element.getAsString();
								if (!materialName.isEmpty()) {
									materialsDatabase.put(materialName, new MaterialInfo(floorNumber, rarityKey, color));
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
	 * Loads the essences database from Essenz.json
	 */
	private static void loadEssencesDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(ESSENCES_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Essences config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					com.google.gson.JsonArray essencesArray = json.getAsJsonArray("essences");
					
					for (var element : essencesArray) {
						JsonObject essenceData = element.getAsJsonObject();
						String name = essenceData.get("name").getAsString();
						int wave = essenceData.get("wave").getAsInt();
						
						// Store the full essence name including Tier information
						if (!name.isEmpty()) {
							essencesDatabase.put(name, new EssenceInfo(name, "Essenz", "Tier", wave));
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	/**
	 * Loads cards and statues effects from JSON file
	 */
	private static void loadCardsStatuesDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(CARDS_STATUES_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Cards/Statues config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					
					// Load cards effects
					if (json.has("karten")) {
						JsonObject kartenObject = json.getAsJsonObject("karten");
						for (var entry : kartenObject.entrySet()) {
							String cardName = entry.getKey();
							String effect = null;
							List<Integer> floors = new ArrayList<>();
							
							// Support both old format (string) and new format (object with effect and floor)
							if (entry.getValue().isJsonObject()) {
								// New format: object with "effect" and "floor"
								JsonObject cardObject = entry.getValue().getAsJsonObject();
								if (cardObject.has("effect")) {
									effect = cardObject.get("effect").getAsString();
								}
								if (cardObject.has("floor")) {
									var floorElement = cardObject.get("floor");
									if (floorElement.isJsonArray()) {
										var floorArray = floorElement.getAsJsonArray();
										for (var floorValue : floorArray) {
											if (floorValue.isJsonPrimitive() && floorValue.getAsJsonPrimitive().isNumber()) {
												floors.add(floorValue.getAsInt());
											}
										}
									}
								}
							} else {
								// Old format: direct string
								effect = entry.getValue().getAsString();
							}
							
							if (!cardName.isEmpty() && effect != null && !effect.isEmpty()) {
								cardsEffects.put(cardName, effect);
							}
							if (!floors.isEmpty()) {
								cardsFloors.put(cardName, floors);
							}
						}
					}
					
					// Load statues effects
					if (json.has("statuen")) {
						JsonObject statuenObject = json.getAsJsonObject("statuen");
						for (var entry : statuenObject.entrySet()) {
							String statueName = entry.getKey();
							String effect = null;
							List<Integer> floors = new ArrayList<>();
							
							// Support both old format (string) and new format (object with effect and floor)
							if (entry.getValue().isJsonObject()) {
								// New format: object with "effect" and "floor"
								JsonObject statueObject = entry.getValue().getAsJsonObject();
								if (statueObject.has("effect")) {
									effect = statueObject.get("effect").getAsString();
								}
								if (statueObject.has("floor")) {
									var floorElement = statueObject.get("floor");
									if (floorElement.isJsonArray()) {
										var floorArray = floorElement.getAsJsonArray();
										for (var floorValue : floorArray) {
											if (floorValue.isJsonPrimitive() && floorValue.getAsJsonPrimitive().isNumber()) {
												floors.add(floorValue.getAsInt());
											}
										}
									}
								}
							} else {
								// Old format: direct string
								effect = entry.getValue().getAsString();
							}
							
							if (!statueName.isEmpty() && effect != null && !effect.isEmpty()) {
								statuesEffects.put(statueName, effect);
							}
							if (!floors.isEmpty()) {
								statuesFloors.put(statueName, floors);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load cards/statues database: " + e.getMessage());
			// Silent error handling
		}
	}
	
	/**
	 * Extracts plain text from a Text component recursively, ignoring all formatting
	 */
	private static String extractPlainText(Text text) {
		if (text == null) {
			return "";
		}
		
		// Use getString() which already extracts all text recursively
		return text.getString();
	}
	
	/**
	 * Converts an arabic number to roman numeral
	 */
	private static String convertToRomanNumeral(int number) {
		if (number < 1 || number > 8) {
			return String.valueOf(number); // Return as string if out of range
		}
		
		String[] romanNumerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
		return romanNumerals[number - 1];
	}
	
	/**
	 * Converts a roman numeral to arabic number
	 */
	private static int romanToArabic(String roman) {
		java.util.Map<String, Integer> romanMap = new java.util.HashMap<>();
		romanMap.put("I", 1);
		romanMap.put("II", 2);
		romanMap.put("III", 3);
		romanMap.put("IV", 4);
		romanMap.put("V", 5);
		romanMap.put("VI", 6);
		romanMap.put("VII", 7);
		romanMap.put("VIII", 8);
		
		return romanMap.getOrDefault(roman, -1);
	}
	
	// License slots that should be checked
	private static final int[] LICENSE_SLOTS = {1, 2, 3, 10, 11, 12, 19, 20, 21};
	
	/**
	 * Finds the slot index by comparing the item stack
	 * If multiple slots have the same item, finds the one closest to mouse position
	 * @param client The Minecraft client
	 * @param stack The item stack to find
	 * @return The slot index, or -1 if not found
	 */
	private static int findSlotByItemStack(MinecraftClient client, ItemStack stack) {
		if (client.currentScreen == null || !(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) {
			return -1;
		}
		
		net.minecraft.client.gui.screen.ingame.HandledScreen<?> screen = (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
		net.minecraft.screen.ScreenHandler handler = screen.getScreenHandler();
		
		// Get mouse position - use stored position from mixin if available
		double mouseX;
		double mouseY;
		if (lastMouseX >= 0 && lastMouseY >= 0) {
			mouseX = lastMouseX;
			mouseY = lastMouseY;
		} else {
			mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
			mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
		}
		
		// Get screen position using same method as getHoveredSlotIndex
		int screenX = 0;
		int screenY = 0;
		try {
			// Try common field names (deobfuscated and obfuscated)
			String[] possibleXNames = {"x", "field_2776", "field_2777"};
			String[] possibleYNames = {"y", "field_2777", "field_2776"};
			
			java.lang.reflect.Field xField = null;
			java.lang.reflect.Field yField = null;
			
			// Try to find x field
			for (String name : possibleXNames) {
				try {
					java.lang.reflect.Field field = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField(name);
					field.setAccessible(true);
					if (field.getType() == int.class) {
						if (xField == null) {
							xField = field;
						} else if (yField == null) {
							yField = field;
							break;
						}
					}
				} catch (Exception e) {
					// Try next name
				}
			}
			
			// If we didn't find y, try the y names
			if (yField == null) {
				for (String name : possibleYNames) {
					try {
						java.lang.reflect.Field field = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField(name);
						field.setAccessible(true);
						if (field.getType() == int.class && field != xField) {
							yField = field;
							break;
						}
					} catch (Exception e) {
						// Try next name
					}
				}
			}
			
			// If still not found, search all int fields
			if (xField == null || yField == null) {
				java.lang.reflect.Field[] fields = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredFields();
				for (java.lang.reflect.Field field : fields) {
					if (field.getType() == int.class) {
						field.setAccessible(true);
						int value = field.getInt(screen);
						// Heuristic: x and y are usually small positive values (0-1000)
						if (value >= 0 && value < 1000) {
							if (xField == null) {
								xField = field;
							} else if (yField == null && field != xField) {
								yField = field;
								break;
							}
						}
					}
				}
			}
			
			if (xField != null && yField != null) {
				screenX = xField.getInt(screen);
				screenY = yField.getInt(screen);
			}
		} catch (Exception e) {
			// Ignore
		}
		
		// Find all slots with matching item
		java.util.List<Integer> matchingSlots = new java.util.ArrayList<>();
		for (int slotIndex = 0; slotIndex < handler.slots.size(); slotIndex++) {
			net.minecraft.screen.slot.Slot slot = handler.slots.get(slotIndex);
			if (slot.hasStack()) {
				ItemStack slotStack = slot.getStack();
				// First try reference comparison (faster and more accurate)
				if (slotStack == stack) {
					matchingSlots.add(slotIndex);
					continue;
				}
				// Then try content comparison (in case of reference mismatch)
				if (ItemStack.areEqual(stack, slotStack)) {
					matchingSlots.add(slotIndex);
				}
			}
		}
		
		if (matchingSlots.isEmpty()) {
			return -1;
		}
		
		// If only one match, return it
		if (matchingSlots.size() == 1) {
			return matchingSlots.get(0);
		}
		
		// If multiple matches (like slot 11 and 13), use X position to determine which one
		java.util.List<Integer> targetSlots = new java.util.ArrayList<>();
		for (int slotIndex : matchingSlots) {
			if (slotIndex == 11 || slotIndex == 13) {
				targetSlots.add(slotIndex);
			}
		}
		
		if (targetSlots.size() == 2) {
			// Both slot 11 and 13 have the same item - use X position to determine which one
			net.minecraft.screen.slot.Slot slot11 = handler.slots.get(11);
			net.minecraft.screen.slot.Slot slot13 = handler.slots.get(13);
			
			int slot11Left = slot11.x + screenX;
			int slot11Right = slot11Left + 18;
			int slot11Center = (slot11Left + slot11Right) / 2;
			int slot13Left = slot13.x + screenX;
			int slot13Right = slot13Left + 18;
			int slot13Center = (slot13Left + slot13Right) / 2;
			
			// Check which slot the mouse X is closer to (using center of slot)
			double distToSlot11 = Math.abs(mouseX - slot11Center);
			double distToSlot13 = Math.abs(mouseX - slot13Center);
			
			int selectedSlot = distToSlot11 < distToSlot13 ? 11 : 13;
			return selectedSlot;
		} else if (targetSlots.size() == 1) {
			// Only one target slot found
			return targetSlots.get(0);
		}
		
		// If no slot 11 or 13 found, return first match
		return matchingSlots.get(0);
	}
	
	/**
	 * Gets the slot index of the currently hovered item using mouse position
	 * @param client The Minecraft client
	 * @return The slot index, or -1 if not found
	 */
	private static int getHoveredSlotIndex(MinecraftClient client) {
		if (client.currentScreen == null || !(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) {
			return -1;
		}
		
		net.minecraft.client.gui.screen.ingame.HandledScreen<?> screen = (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
		net.minecraft.screen.ScreenHandler handler = screen.getScreenHandler();
		
		// Get mouse position - use stored position from mixin if available, otherwise calculate
		double mouseX;
		double mouseY;
		if (lastMouseX >= 0 && lastMouseY >= 0) {
			mouseX = lastMouseX;
			mouseY = lastMouseY;
		} else {
			mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
			mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
		}
		
		// Get screen position using reflection - try multiple field names
		int screenX = 0;
		int screenY = 0;
		
		try {
			// Try common field names (deobfuscated and obfuscated)
			String[] possibleXNames = {"x", "field_2776", "field_2777"};
			String[] possibleYNames = {"y", "field_2777", "field_2776"};
			
			java.lang.reflect.Field xField = null;
			java.lang.reflect.Field yField = null;
			
			// Try to find x field
			for (String name : possibleXNames) {
				try {
					java.lang.reflect.Field field = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField(name);
					field.setAccessible(true);
					if (field.getType() == int.class) {
						if (xField == null) {
							xField = field;
						} else if (yField == null) {
							yField = field;
							break;
						}
					}
				} catch (Exception e) {
					// Try next name
				}
			}
			
			// If we didn't find y, try the y names
			if (yField == null) {
				for (String name : possibleYNames) {
					try {
						java.lang.reflect.Field field = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField(name);
						field.setAccessible(true);
						if (field.getType() == int.class && field != xField) {
							yField = field;
							break;
						}
					} catch (Exception e) {
						// Try next name
					}
				}
			}
			
			// If still not found, search all int fields
			if (xField == null || yField == null) {
				java.lang.reflect.Field[] fields = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredFields();
				for (java.lang.reflect.Field field : fields) {
					if (field.getType() == int.class) {
						field.setAccessible(true);
						int value = field.getInt(screen);
						// Heuristic: x and y are usually small positive values (0-1000)
						if (value >= 0 && value < 1000) {
							if (xField == null) {
								xField = field;
							} else if (yField == null && field != xField) {
								yField = field;
								break;
							}
						}
					}
				}
			}
			
			if (xField != null && yField != null) {
				screenX = xField.getInt(screen);
				screenY = yField.getInt(screen);
			}
		} catch (Exception e) {
			// Continue with screenX=0, screenY=0 as fallback
		}
		
		// Find the hovered slot by checking mouse position
		// Minecraft slots are 18x18 pixels
		try {
			// Debug: Check slots 11 and 13 specifically
			for (int slotIndex = 0; slotIndex < handler.slots.size(); slotIndex++) {
				net.minecraft.screen.slot.Slot slot = handler.slots.get(slotIndex);
				
				// Only check slots 11 and 13 to reduce noise
				if (slotIndex != 11 && slotIndex != 13) {
					continue;
				}
				
				// Check if mouse is over this slot (Minecraft slots are 18x18)
				int slotLeft = slot.x + screenX;
				int slotRight = slotLeft + 18;
				int slotTop = slot.y + screenY;
				int slotBottom = slotTop + 18;
				
				if (slotLeft <= mouseX && mouseX < slotRight &&
					slotTop <= mouseY && mouseY < slotBottom) {
					return slotIndex;
				}
			}
			
			// If not found in slots 11/13, check all slots
			for (int slotIndex = 0; slotIndex < handler.slots.size(); slotIndex++) {
				net.minecraft.screen.slot.Slot slot = handler.slots.get(slotIndex);
				
				// Skip slots 11 and 13 (already checked)
				if (slotIndex == 11 || slotIndex == 13) {
					continue;
				}
				
				int slotLeft = slot.x + screenX;
				int slotRight = slotLeft + 18;
				int slotTop = slot.y + screenY;
				int slotBottom = slotTop + 18;
				
				if (slotLeft <= mouseX && mouseX < slotRight &&
					slotTop <= mouseY && mouseY < slotBottom) {
					// Don't return this, we only want 11 or 13
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		
		return -1;
	}
	
	/**
	 * Extracts all text recursively from a Text object, including all siblings
	 * This ensures we get all text even if it's split across multiple Text components
	 */
	private static String extractAllTextRecursively(Text text) {
		if (text == null) {
			return "";
		}
		
		StringBuilder result = new StringBuilder();
		
		// Add the main text
		String mainText = text.getString();
		if (mainText != null && !mainText.isEmpty()) {
			result.append(mainText);
		}
		
		// Recursively process all siblings
		for (Text sibling : text.getSiblings()) {
			String siblingText = extractAllTextRecursively(sibling);
			if (!siblingText.isEmpty()) {
				result.append(siblingText);
			}
		}
		
		return result.toString();
	}
	
	/**
	 * Adds slot-specific text to item names in "Aspekt [tranferieren]" inventory
	 * Slot 11: "(Wird zerstört)" in red
	 * Slot 13: "(Bleibt erhalten)" in green
	 */
	private static void addAspectTransferSlotText(List<Text> lines, MinecraftClient client, ItemStack stack) {
		if (lines == null || lines.isEmpty() || stack == null || stack.isEmpty()) {
			return;
		}
		
		// Check if we're in the "Aspekt [tranferieren]" inventory
		if (client.currentScreen == null) {
			return;
		}
		
		// Get the title Text object and extract all text recursively
		Text titleText = client.currentScreen.getTitle();
		if (titleText == null) {
			return;
		}
		
		// Extract all text recursively (handles nested Text components with colors)
		String screenTitle = extractAllTextRecursively(titleText);
		
		if (screenTitle == null || screenTitle.isEmpty()) {
			return;
		}
		
		// Remove Minecraft formatting codes (like §f, §r, §b, etc.)
		String cleanScreenTitle = screenTitle.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "");
		cleanScreenTitle = cleanScreenTitle.replaceAll("§#[0-9a-fA-F]{6}", "");
		cleanScreenTitle = cleanScreenTitle.replaceAll("§x(§[0-9a-fA-F]){6}", "");
		
		// Remove Unicode formatting characters (like 㔅㓩㔉㔇㔆㓾㔘)
		cleanScreenTitle = cleanScreenTitle.replaceAll("[\\u3400-\\u4DBF]", "");
		
		// Check for "Aspekt" and "[transferieren]" or "[tranferieren]" (both spellings)
		boolean hasAspekt = cleanScreenTitle.contains("Aspekt");
		boolean hasTransfer = cleanScreenTitle.contains("[transferieren]") || cleanScreenTitle.contains("[tranferieren]");
		
		if (!hasAspekt || !hasTransfer) {
			return;
		}
		
		// Get the slot index of the currently hovered item
		// First try mouse position (more accurate for hover detection)
		int slotIndex = getHoveredSlotIndex(client);
		
		// If mouse position failed OR found a slot that's not 11 or 13, try item stack comparison as fallback
		if (slotIndex == -1 || (slotIndex != 11 && slotIndex != 13)) {
			int foundSlot = findSlotByItemStack(client, stack);
			// Only use itemStack result if it's one of our target slots
			if (foundSlot == 11 || foundSlot == 13) {
				slotIndex = foundSlot;
			}
		}
		
		if (slotIndex == -1) {
			return;
		}
		
		// Only modify if the slot is exactly 11 or 13
		if (slotIndex != 11 && slotIndex != 13) {
			return;
		}
		
		// Check if modification was already added to avoid duplicates
		Text firstLine = lines.get(0);
		if (firstLine == null) {
			return;
		}
		
		String firstLineText = firstLine.getString();
		boolean alreadyHasWirdZerstoert = firstLineText != null && firstLineText.contains("(Wird zerstört)");
		boolean alreadyHasBleibtErhalten = firstLineText != null && firstLineText.contains("(Bleibt erhalten)");
		
		if (alreadyHasWirdZerstoert || alreadyHasBleibtErhalten) {
			return;
		}
		
		// Modify the first line (item name) based on slot
		if (slotIndex == 11) {
			// Slot 11: Add "(Wird zerstört)" in red
			MutableText modifiedLine = firstLine.copy().append(
				Text.literal(" (Wird zerstört)")
					.styled(style -> style.withColor(0xFFFF5555)) // Red color
			);
			lines.set(0, modifiedLine);
		} else if (slotIndex == 13) {
			// Slot 13: Add "(Bleibt erhalten)" in green
			MutableText modifiedLine = firstLine.copy().append(
				Text.literal(" (Bleibt erhalten)")
					.styled(style -> style.withColor(0xFF55FF55)) // Green color
			);
			lines.set(0, modifiedLine);
		}
	}
	
	/**
	 * Checks if the hovered item is in one of the license slots
	 */
	private static boolean isItemInLicenseSlot(ItemStack stack, MinecraftClient client) {
		if (client.currentScreen == null || !(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) {
			return false;
		}
		
		net.minecraft.client.gui.screen.ingame.HandledScreen<?> screen = (net.minecraft.client.gui.screen.ingame.HandledScreen<?>) client.currentScreen;
		net.minecraft.screen.ScreenHandler handler = screen.getScreenHandler();
		
		// Check if the hovered item is in one of the license slots
		for (int slotIndex : LICENSE_SLOTS) {
			if (slotIndex < handler.slots.size()) {
				net.minecraft.screen.slot.Slot slot = handler.slots.get(slotIndex);
				if (slot.hasStack() && slot.getStack() == stack) {
					return true;
				}
			}
		}
		
		// Also check by comparing item stacks (in case of reference mismatch)
		for (int slotIndex : LICENSE_SLOTS) {
			if (slotIndex < handler.slots.size()) {
				net.minecraft.screen.slot.Slot slot = handler.slots.get(slotIndex);
				if (slot.hasStack()) {
					ItemStack slotStack = slot.getStack();
					if (ItemStack.areEqual(stack, slotStack)) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Checks for license information in tooltip lines and adds location information
	 */
	private static void checkForLicenseInformation(List<Text> lines, MinecraftClient client) {
		// Check if database is loaded
		if (licensesDatabase.isEmpty()) {
			return;
		}
		
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			// Skip if lineText is null or empty
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}
			
			// Extract plain text from Text component recursively to handle nested formatting
			// This ensures we get all text regardless of color/formatting
			String cleanLineText = line.getString();
			
			// Remove all Minecraft formatting codes (including all possible format codes)
			// § followed by any character (0-9, a-f, k-o, r, x for hex colors, etc.)
			cleanLineText = cleanLineText.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "");
			
			// Remove hex color codes (format: §#RRGGBB or §x§r§r§g§g§b§b)
			cleanLineText = cleanLineText.replaceAll("§#[0-9a-fA-F]{6}", "");
			cleanLineText = cleanLineText.replaceAll("§x(§[0-9a-fA-F]){6}", "");
			
			// Also remove Unicode formatting characters that might interfere
			cleanLineText = cleanLineText.replaceAll("[\\u3400-\\u4DBF]", "");
			
			// Trim whitespace
			cleanLineText = cleanLineText.trim();
			
			// Skip lines that contain "Schaltet das" - these are description lines, not license lines
			if (cleanLineText.contains("Schaltet das")) {
				continue;
			}
			
			// Check if this line contains "Benötigte Lizenz:" and extract the license name/alias
			if (cleanLineText.contains("Benötigte Lizenz:")) {
				// Extract the part after "Benötigte Lizenz:"
				String afterColon = cleanLineText.substring(cleanLineText.indexOf("Benötigte Lizenz:") + "Benötigte Lizenz:".length()).trim();
				
				// Try to find a license name or alias in the remaining text
				// Sort by length (longest first) to avoid shorter names matching before longer ones
				List<Map.Entry<String, LicenseInfo>> sortedLicenses = new ArrayList<>(licensesDatabase.entrySet());
				sortedLicenses.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
				
				for (Map.Entry<String, LicenseInfo> entry : sortedLicenses) {
					String searchTerm = entry.getKey();
					LicenseInfo licenseInfo = entry.getValue();
					
					// Remove formatting from search term as well for comparison
					String cleanSearchTerm = searchTerm.replaceAll("§[0-9a-fk-orx]", "")
						.replaceAll("§#[0-9a-fA-F]{6}", "")
						.replaceAll("[\\u3400-\\u4DBF]", "")
						.trim();
					
					// Case-insensitive search to be more robust
					String lowerAfterColon = afterColon.toLowerCase();
					String lowerSearchTerm = cleanSearchTerm.toLowerCase();
					
					if (lowerAfterColon.contains(lowerSearchTerm)) {
						// Add location information as a new line with format: → Location
						Text locationText = Text.literal(" → " + licenseInfo.location)
							.styled(style -> style.withColor(0xC0C0C0)); // Light gray
						
						// Insert after the current line
						lines.add(i + 1, locationText);
						
						// Skip the next line since we just added location info
						i++;
						break; // Only add info for the first match in this line
					}
				}
			} else {
				// Also check if this line contains any license name or alias directly (fallback)
				// Sort by length (longest first) to avoid shorter names matching before longer ones
				List<Map.Entry<String, LicenseInfo>> sortedLicenses = new ArrayList<>(licensesDatabase.entrySet());
				sortedLicenses.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
				
				for (Map.Entry<String, LicenseInfo> entry : sortedLicenses) {
					String searchTerm = entry.getKey();
					LicenseInfo licenseInfo = entry.getValue();
					
					// Remove formatting from search term as well for comparison
					String cleanSearchTerm = searchTerm.replaceAll("§[0-9a-fk-orx]", "")
						.replaceAll("§#[0-9a-fA-F]{6}", "")
						.replaceAll("[\\u3400-\\u4DBF]", "")
						.trim();
					
					// Case-insensitive search to be more robust
					String lowerCleanLine = cleanLineText.toLowerCase();
					String lowerSearchTerm = cleanSearchTerm.toLowerCase();
					
					if (lowerCleanLine.contains(lowerSearchTerm)) {
						// Add location information as a new line with format: → Location
						Text locationText = Text.literal(" → " + licenseInfo.location)
							.styled(style -> style.withColor(0xC0C0C0)); // Light gray
						
						// Insert after the current line
						lines.add(i + 1, locationText);
						
						// Skip the next line since we just added location info
						i++;
						break; // Only add info for the first match in this line
					}
				}
			}
		}
	}
	
	/**
	 * Checks for gadget information in tooltip lines and adds location information
	 * @param lines The tooltip lines
	 * @param isEngineerInventory If true, reads level from line 3 with format "[Stufe]: "LEVEL"", otherwise from "Stufe: [LEVEL]"
	 */
	private static void checkForGadgetInformation(List<Text> lines, boolean isEngineerInventory) {
		if (gadgetsDatabase.isEmpty()) {
			return;
		}
		
		// Find the line with level information and extract level, and identify the item
		String itemName = null;
		String level = null;
		int stufeLineIndex = -1;
		
		// First pass: identify the item by checking all lines for gadget names/aliases
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			if (lineText == null || lineText.isEmpty()) {
				continue;
			}
			
			// Remove formatting codes
			String cleanLineText = lineText.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "")
				.replaceAll("§#[0-9a-fA-F]{6}", "")
				.replaceAll("§x(§[0-9a-fA-F]){6}", "")
				.replaceAll("[\\u3400-\\u4DBF]", "")
				.trim();
			
			// Try to identify the item by checking if any gadget name/alias appears in the tooltip
			// Sort by length (longest first) to avoid shorter names matching before longer ones
			List<String> sortedGadgetKeys = new ArrayList<>(gadgetsDatabase.keySet());
			sortedGadgetKeys.sort((a, b) -> Integer.compare(b.length(), a.length()));
			
			for (String gadgetKey : sortedGadgetKeys) {
				String cleanGadgetKey = gadgetKey.replaceAll("§[0-9a-fk-orx]", "")
					.replaceAll("§#[0-9a-fA-F]{6}", "")
					.replaceAll("[\\u3400-\\u4DBF]", "")
					.trim();
				
				if (cleanLineText.toLowerCase().contains(cleanGadgetKey.toLowerCase())) {
					itemName = gadgetKey;
					break;
				}
			}
			
			if (itemName != null) {
				break; // Found the item, stop searching
			}
		}
		
		// Second pass: find the level line
		if (isEngineerInventory) {
			// For Engineer inventory: search all lines for "[Stufe]: "LEVEL"" pattern
			for (int i = 0; i < lines.size(); i++) {
				Text line = lines.get(i);
				String lineText = line.getString();
				
				if (lineText == null || lineText.isEmpty()) {
					continue;
				}
				
				// Remove formatting codes
				String cleanLineText = lineText.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "")
					.replaceAll("§#[0-9a-fA-F]{6}", "")
					.replaceAll("§x(§[0-9a-fA-F]){6}", "")
					.replaceAll("[\\u3400-\\u4DBF]", "")
					.trim();
				
				// Check for "[Stufe]: "LEVEL"" pattern (with quotes) or "[Stufe]: LEVEL" pattern (without quotes)
				if (cleanLineText.contains("[Stufe]:")) {
					stufeLineIndex = i;
					// Extract level from "[Stufe]: "LEVEL"" or "[Stufe]: LEVEL"
					int startIndex = cleanLineText.indexOf("[Stufe]:") + "[Stufe]:".length();
					String afterStufe = cleanLineText.substring(startIndex).trim();
					
					// Check if it starts with a quote
					if (afterStufe.startsWith("\"")) {
						// Format: "[Stufe]: "LEVEL""
						int endIndex = afterStufe.indexOf("\"", 1);
						if (endIndex > 1) {
							level = afterStufe.substring(1, endIndex).trim();
						}
					} else {
						// Format: "[Stufe]: LEVEL" - extract everything after ": " until end or space
						level = afterStufe.trim();
						// Remove any trailing text if there is any
						if (level.contains(" ")) {
							level = level.substring(0, level.indexOf(" ")).trim();
						}
					}
					
					if (level != null && !level.isEmpty()) {
						// Level found, continue
					}
					break; // Found the level, stop searching
				}
			}
		} else {
			// For Module [Upgraden] inventory: find the "Stufe: [" line
			for (int i = 0; i < lines.size(); i++) {
				Text line = lines.get(i);
				String lineText = line.getString();
				
				if (lineText == null || lineText.isEmpty()) {
					continue;
				}
				
				// Remove formatting codes
				String cleanLineText = lineText.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "")
					.replaceAll("§#[0-9a-fA-F]{6}", "")
					.replaceAll("§x(§[0-9a-fA-F]){6}", "")
					.replaceAll("[\\u3400-\\u4DBF]", "")
					.trim();
				
				// Check for "Stufe: [" pattern
				if (cleanLineText.contains("Stufe: [") && stufeLineIndex == -1) {
					stufeLineIndex = i;
					// Extract level from "Stufe: [LEVEL]"
					int startIndex = cleanLineText.indexOf("Stufe: [") + "Stufe: [".length();
					int endIndex = cleanLineText.indexOf("]", startIndex);
					if (endIndex > startIndex) {
						level = cleanLineText.substring(startIndex, endIndex).trim();
					}
					break; // Found the level, stop searching
				}
			}
		}
		
		// If we found a level and item name, look up the location
		if (stufeLineIndex >= 0 && level != null && itemName != null) {
			Map<String, String> levelMap = gadgetsDatabase.get(itemName);
			if (levelMap != null) {
				String location = levelMap.get(level);
				
				// If not found and level is a number, try converting to roman numeral
				if (location == null) {
					try {
						int levelNum = Integer.parseInt(level);
						String romanLevel = convertToRomanNumeral(levelNum);
						location = levelMap.get(romanLevel);
					} catch (NumberFormatException e) {
						// Level is not a number, try converting roman to number
						try {
							int levelNum = romanToArabic(level);
							String arabicLevel = String.valueOf(levelNum);
							location = levelMap.get(arabicLevel);
						} catch (Exception ex) {
							// Conversion failed, continue
						}
					}
				}
				
				if (location != null && !location.isEmpty()) {
					// Add location information as a new line after the level line
					// Format: " → Nächstes Level: location" (without quotes)
					Text locationText = Text.literal(" → Nächstes Level: " + location)
						.styled(style -> style.withColor(0xC0C0C0)); // Light gray
					
					lines.add(stufeLineIndex + 1, locationText);
				}
			}
		}
	}
	
	/**
	 * Loads the gadgets database from Farmworld.json
	 */
	private static void loadGadgetsDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(LICENSES_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Gadgets config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					
					// Load all gadget categories
					String[] gadgetCategories = {"mining_gadgets", "logging_gadgets", "gadget_bags", 
						"magnet_gadgets", "smelting_gadgets", "distribution_gadgets"};
					
					for (String category : gadgetCategories) {
						if (json.has(category)) {
							com.google.gson.JsonArray gadgetsArray = json.getAsJsonArray(category);
							
							for (var element : gadgetsArray) {
								JsonObject gadgetData = element.getAsJsonObject();
								String name = gadgetData.get("name").getAsString();
								String level = gadgetData.get("level").getAsString();
								String location = gadgetData.get("location").getAsString();
								
								if (!name.isEmpty() && !level.isEmpty() && !location.isEmpty()) {
									// Store by name
									gadgetsDatabase.computeIfAbsent(name, k -> new HashMap<>()).put(level, location);
									
									// Also store by alias if it exists
									if (gadgetData.has("aliases")) {
										String aliases = gadgetData.get("aliases").getAsString();
										if (!aliases.isEmpty()) {
											gadgetsDatabase.computeIfAbsent(aliases, k -> new HashMap<>()).put(level, location);
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load gadgets database: " + e.getMessage());
			// Silent error handling
		}
	}
	
	/**
	 * Loads the licenses database from Farmworld.json
	 */
	private static void loadLicensesDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(LICENSES_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Licenses config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					com.google.gson.JsonArray licensesArray = json.getAsJsonArray("licenses");
					
					for (var element : licensesArray) {
						JsonObject licenseData = element.getAsJsonObject();
						String name = licenseData.get("name").getAsString();
						String location = licenseData.get("location").getAsString();
						
						// Store the license with its name as key
						if (!name.isEmpty() && !location.isEmpty()) {
							LicenseInfo licenseInfo = new LicenseInfo(name, location);
							licensesDatabase.put(name, licenseInfo);
							
							// Also store the alias if it exists
							if (licenseData.has("aliases")) {
								String aliases = licenseData.get("aliases").getAsString();
								if (!aliases.isEmpty()) {
									licensesDatabase.put(aliases, licenseInfo);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load licenses database: " + e.getMessage());
			// Silent error handling
		}
	}
	
	/**
	 * Loads the aspects database from Aspekte.json
	 */
	private static void loadAspectsDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(ASPECTS_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Aspects config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					
					for (String itemName : json.keySet()) {
						JsonObject itemData = json.getAsJsonObject(itemName);
						String aspectName = itemData.get("aspect_name").getAsString();
						String aspectDescription = itemData.get("aspect_description").getAsString();
						
						// Only store items that have aspect information
						if (!aspectName.isEmpty() || !aspectDescription.isEmpty()) {
							aspectsDatabase.put(itemName, new AspectInfo(aspectName, aspectDescription));
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load aspects database: " + e.getMessage());
			// Silent error handling
		}
	}
	
	/**
	 * Loads the MKLevel database from MKLevel.json
	 */
	private static void loadMKLevelDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(MKLEVEL_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("MKLevel config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
					
					// Load levels array
					if (root.has("levels") && root.get("levels").isJsonArray()) {
						com.google.gson.JsonArray levelsArray = root.getAsJsonArray("levels");
						
						for (var element : levelsArray) {
							JsonObject levelData = element.getAsJsonObject();
							int level = levelData.get("Level").getAsInt();
							String essence = levelData.get("Essence").getAsString();
							int amount = levelData.get("Amount").getAsInt();
							
							mkLevelDatabase.add(new MKLevelInfo(level, essence, amount));
						}
					}
					
					// Load combined_waves array
					if (root.has("combined_waves") && root.get("combined_waves").isJsonArray()) {
						com.google.gson.JsonArray combinedWavesArray = root.getAsJsonArray("combined_waves");
						
						for (var element : combinedWavesArray) {
							JsonObject waveData = element.getAsJsonObject();
							String level = waveData.get("Level").getAsString();
							int wave = waveData.get("Wave").getAsInt();
							
							// Parse "without" array if present
							List<String> without = new ArrayList<>();
							if (waveData.has("without")) {
								if (waveData.get("without").isJsonArray()) {
									com.google.gson.JsonArray withoutArray = waveData.getAsJsonArray("without");
									for (var withoutElement : withoutArray) {
										String withoutStr = withoutElement.getAsString();
										// Split by comma in case a single array element contains multiple essences
										String[] parts = withoutStr.split(",");
										for (String part : parts) {
											String trimmed = part.trim();
											if (!trimmed.isEmpty()) {
												without.add(trimmed);
											}
										}
									}
								} else if (waveData.get("without").isJsonPrimitive()) {
									// Handle case where "without" is a single string (comma-separated)
									String withoutStr = waveData.get("without").getAsString();
									String[] parts = withoutStr.split(",");
									for (String part : parts) {
										String trimmed = part.trim();
										if (!trimmed.isEmpty()) {
											without.add(trimmed);
										}
									}
								}
							}
							
							mkLevelCombinedWavesDatabase.add(new CombinedWaveInfo(level, wave, without));
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load MKLevel database: " + e.getMessage());
			// Silent error handling
		}
	}
	
	/**
	 * Loads the blueprints database from blueprints.json to map blueprint names to floor numbers
	 */
	private static void loadBlueprintsDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(BLUEPRINTS_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Blueprints config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					JsonObject floors = json.getAsJsonObject("floors");
					
					for (String floorKey : floors.keySet()) {
						// Extract floor number from "floor_X" format
						final int floorNumber;
						try {
							String floorNumStr = floorKey.replace("floor_", "");
							floorNumber = Integer.parseInt(floorNumStr);
						} catch (NumberFormatException e) {
							// If parsing fails, default to 1
							continue; // Skip this floor if we can't parse the number
						}
						
						JsonObject floorData = floors.getAsJsonObject(floorKey);
						JsonObject blueprints = floorData.getAsJsonObject("blueprints");
						
						// Iterate through all rarities
						for (String rarityKey : blueprints.keySet()) {
							JsonObject rarityData = blueprints.getAsJsonObject(rarityKey);
							com.google.gson.JsonArray itemsArray = rarityData.getAsJsonArray("items");
							
							// Add each blueprint item to the map
							itemsArray.forEach(element -> {
								String blueprintName = element.getAsString();
								// Store the floor number for this blueprint
								blueprintFloorMap.put(blueprintName, floorNumber);
							});
						}
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load blueprints database: " + e.getMessage());
			// Silent error handling
		}
	}
	
	/**
	 * Adds floor number to blueprint names in inventory tooltips
	 * Format: "BAUPLAN NAME - [Bauplan] [eX]" where X is the floor number (at the end)
	 */
	private static void addFloorNumberToBlueprintNames(List<Text> lines, MinecraftClient client) {
		// Check if blueprint floor number display is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().showBlueprintFloorNumber) {
			return;
		}
		
		if (client == null || client.currentScreen == null) {
			return;
		}
		
		// Check if we're in a blueprint inventory
		String screenTitle = client.currentScreen.getTitle().getString();
		
		// Check if we're in essence harvester UI - don't add floor numbers here
		if (ZeichenUtility.containsEssenceHarvesterUi(screenTitle)) {
			return;
		}
		
		String cleanScreenTitle = screenTitle.replaceAll("§[0-9a-fk-or]", "")
											.replaceAll("[\\u3400-\\u4DBF]", "");
		
		boolean isInBlueprintInventory = cleanScreenTitle.contains("Baupläne [Waffen]") ||
										  cleanScreenTitle.contains("Baupläne [Rüstung]") ||
										  cleanScreenTitle.contains("Baupläne [Werkzeuge]") ||
										  cleanScreenTitle.contains("Bauplan [Shop]") ||
										  cleanScreenTitle.contains("Favorisierte [Rüstungsbaupläne]") ||
										  cleanScreenTitle.contains("Favorisierte [Waffenbaupläne]") ||
										  cleanScreenTitle.contains("Favorisierte [Werkzeugbaupläne]") ||
										  cleanScreenTitle.contains("Favorisierte [Shop-Baupläne]") ||
										  cleanScreenTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools");
		
		if (!isInBlueprintInventory) {
			return;
		}
		
		// Look for lines containing "[Bauplan]"
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			if (lineText == null || !lineText.contains("[Bauplan]")) {
				continue;
			}
			
			// Check if the floor number is already added
			if (lineText.contains("[e")) {
				continue; // Already has floor number
			}
			
			// Extract blueprint name and color
			BlueprintNameAndColor nameAndColor = extractBlueprintNameAndColorFromItemName(line);
			
			String blueprintName = null;
			if (nameAndColor != null) {
				blueprintName = nameAndColor.name;
			} else {
				// Fallback: extract from string
				blueprintName = lineText.replace(" - [Bauplan]", "");
				// Remove Unicode formatting characters
				blueprintName = blueprintName.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "");
				// Remove percentage and parentheses
				blueprintName = blueprintName.replaceAll("\\([^)]*\\)", "").trim();
				// Remove level information if present
				blueprintName = blueprintName.replaceAll("\\[Ebene \\d+\\]", "").trim();
				// Remove trailing dash
				blueprintName = blueprintName.replaceAll("\\s*-\\s*$", "").trim();
			}
			
			if (blueprintName == null || blueprintName.isEmpty()) {
				continue;
			}
			
			// Special handling for "Drachenzahn" which appears in multiple floors
			// We need to check the color to determine the floor
			Integer floorNumber = null;
			if (blueprintName.equals("Drachenzahn")) {
				// Check the color of the blueprint name
				// Epic color (#A134EB or #A335EE) = floor 1
				// Legendary color (#FC7E00 or #FF8000) = floor 85
				if (nameAndColor != null) {
					String rarity = nameAndColor.rarity;
					if ("epic".equals(rarity)) {
						floorNumber = 1;
					} else if ("legendary".equals(rarity)) {
						floorNumber = 85;
					}
				}
				
				// If we couldn't determine from nameAndColor, try to extract color from the line directly
				if (floorNumber == null) {
					floorNumber = getDrachenzahnFloorFromColor(line);
				}
			} else {
				// For other blueprints, get floor number from map
				floorNumber = blueprintFloorMap.get(blueprintName);
			}
			
			if (floorNumber == null) {
				continue; // Blueprint not found in database or couldn't determine floor
			}
			
			// Create new text with floor number added at the end
			// Format: "BAUPLAN NAME - [Bauplan] [eX]"
			String floorTag = " [e" + floorNumber + "]";
			
			// Simply append the floor tag at the end of the text
			// We need to preserve the original styling
			Text newLine = appendFloorNumberToText(line, floorTag);
			if (newLine != null) {
				lines.set(i, newLine);
			}
		}
	}
	
	/**
	 * Adds floor numbers to cards and statues names in inventories
	 * Only works in inventories that contain the cards/statues characters (㭆 or 㭂)
	 */
	private static void addFloorNumberToCardsStatuesNames(List<Text> lines, MinecraftClient client) {
		if (client == null || client.currentScreen == null) {
			return;
		}
		
		// Check if we're in a cards/statues inventory
		String screenTitle = client.currentScreen.getTitle().getString();
		if (screenTitle == null) {
			return;
		}
		
		// Check if the inventory contains one of the cards/statues characters
		String[] cardsStatuesChars = ZeichenUtility.getCardsStatues();
		boolean isCardsInventory = screenTitle.contains(cardsStatuesChars[0]); // 㭆
		boolean isStatuesInventory = screenTitle.contains(cardsStatuesChars[1]); // 㭂
		
		if (!isCardsInventory && !isStatuesInventory) {
			return;
		}
		
		// Look for lines containing "[Karte]" or "[Statue]"
		for (int i = 0; i < lines.size(); i++) {
			Text line = lines.get(i);
			String lineText = line.getString();
			
			if (lineText == null) {
				continue;
			}
			
			boolean isCard = false;
			boolean isStatue = false;
			String itemName = null;
			
			// Check for [Karte] or [Statue]
			if (lineText.contains("[Karte]")) {
				isCard = true;
				// Extract name: everything before "[Karte]"
				int index = lineText.indexOf("[Karte]");
				if (index > 0) {
					itemName = lineText.substring(0, index).trim();
					// Remove any leading dashes or special characters
					itemName = itemName.replaceAll("^[-\\s]+", "").trim();
					// Remove formatting codes
					itemName = itemName.replaceAll("§[0-9a-fk-or]", "").trim();
					// Remove Unicode formatting characters (like the ones used in inventories)
					itemName = itemName.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "").trim();
					// Remove trailing dashes
					itemName = itemName.replaceAll("\\s*-\\s*$", "").trim();
				}
			} else if (lineText.contains("[Statue]")) {
				isStatue = true;
				// Extract name: everything before "[Statue]"
				int index = lineText.indexOf("[Statue]");
				if (index > 0) {
					itemName = lineText.substring(0, index).trim();
					// Remove any leading dashes or special characters
					itemName = itemName.replaceAll("^[-\\s]+", "").trim();
					// Remove formatting codes
					itemName = itemName.replaceAll("§[0-9a-fk-or]", "").trim();
					// Remove Unicode formatting characters (like the ones used in inventories)
					itemName = itemName.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "").trim();
					// Remove trailing dashes
					itemName = itemName.replaceAll("\\s*-\\s*$", "").trim();
				}
			}
			
			if (itemName == null || itemName.isEmpty()) {
				continue;
			}
			
			// Check if floor number is already added
			if (lineText.contains("[e")) {
				continue; // Already has floor number
			}
			
			// Get floor numbers from database
			List<Integer> floors = null;
			if (isCard) {
				// Try exact match first
				floors = cardsFloors.get(itemName);
				// If not found, try case-insensitive search
				if (floors == null) {
					for (Map.Entry<String, List<Integer>> entry : cardsFloors.entrySet()) {
						if (entry.getKey().equalsIgnoreCase(itemName)) {
							floors = entry.getValue();
							break;
						}
					}
				}
			} else if (isStatue) {
				// Try exact match first
				floors = statuesFloors.get(itemName);
				// If not found, try case-insensitive search
				if (floors == null) {
					for (Map.Entry<String, List<Integer>> entry : statuesFloors.entrySet()) {
						if (entry.getKey().equalsIgnoreCase(itemName)) {
							floors = entry.getValue();
							break;
						}
					}
				}
			}
			
			if (floors == null || floors.isEmpty()) {
				continue; // No floor data found
			}
			
			// Format floor numbers: if multiple floors, show as "e1, e22", if single, show as "e1"
			String floorTag;
			if (floors.size() == 1) {
				floorTag = " [e" + floors.get(0) + "]";
			} else {
				// Multiple floors: " [e1, e22]"
				StringBuilder floorBuilder = new StringBuilder(" [");
				for (int j = 0; j < floors.size(); j++) {
					if (j > 0) {
						floorBuilder.append(", ");
					}
					floorBuilder.append("e").append(floors.get(j));
				}
				floorBuilder.append("]");
				floorTag = floorBuilder.toString();
			}
			
			// Append the floor tag at the end of the text, preserving styling
			Text newLine = appendFloorNumberToText(line, floorTag);
			if (newLine != null) {
				lines.set(i, newLine);
			}
		}
	}
	
	/**
	 * Gets the floor number for "Drachenzahn" based on the color of the name
	 * Epic color (#A134EB or #A335EE) = floor 1
	 * Legendary color (#FC7E00 or #FF8000) = floor 85
	 */
	private static Integer getDrachenzahnFloorFromColor(Text itemNameText) {
		if (itemNameText == null) {
			return null;
		}
		
		// Target colors: 
		// Epic: #A134EB (RGB: 161, 52, 235) OR #A335EE (RGB: 163, 53, 238) - floor 1
		// Legendary: #FC7E00 (RGB: 252, 126, 0) OR #FF8000 (RGB: 255, 128, 0) - floor 85
		int epicColor1 = 0xFFA134EB;
		int epicColor2 = 0xFFA335EE;
		int legendaryColor1 = 0xFFFC7E00;
		int legendaryColor2 = 0xFFFF8000;
		int epicColor1RGB = epicColor1 & 0x00FFFFFF;
		int epicColor2RGB = epicColor2 & 0x00FFFFFF;
		int legendaryColor1RGB = legendaryColor1 & 0x00FFFFFF;
		int legendaryColor2RGB = legendaryColor2 & 0x00FFFFFF;
		
		// Convert to OrderedText to iterate through styled parts
		net.minecraft.text.OrderedText orderedText = itemNameText.asOrderedText();
		final StringBuilder fullText = new StringBuilder();
		final java.util.List<net.minecraft.text.Style> styles = new java.util.ArrayList<>();
		final java.util.List<Integer> codePoints = new java.util.ArrayList<>();
		
		orderedText.accept((index, style, codePoint) -> {
			styles.add(style);
			codePoints.add(codePoint);
			fullText.appendCodePoint(codePoint);
			return true;
		});
		
		// Find the blueprint name section (BEFORE "-" and "[Bauplan]")
		String fullTextStr = fullText.toString();
		int dashIndex = -1;
		
		// Find the dash before [Bauplan]
		int bauplanStart = fullTextStr.indexOf("[Bauplan]");
		if (bauplanStart > 0) {
			// Look for dash before [Bauplan]
			for (int i = bauplanStart - 1; i >= 0; i--) {
				char ch = fullTextStr.charAt(i);
				if (ch == '-' || ch == '—' || ch == '–') {
					dashIndex = i;
					break;
				} else if (ch != ' ') {
					break; // Stop if we hit non-space, non-dash character
				}
			}
		}
		
		if (dashIndex < 0) {
			return null; // Could not find blueprint name section
		}
		
		// Check the color of the blueprint name section (BEFORE the dash)
		int stringPos = 0;
		for (int i = 0; i < codePoints.size(); i++) {
			int codePoint = codePoints.get(i);
			net.minecraft.text.Style style = styles.get(i);
			
			// Check if this codePoint is in the blueprint name section (BEFORE the dash)
			if (stringPos < dashIndex) {
				// Check the color only for the blueprint name section
				if (style != null && style.getColor() != null) {
					int styleColor = style.getColor().getRgb();
					int styleColorRGB = styleColor & 0x00FFFFFF; // Remove alpha
					
					// Check for Epic color (floor 1)
					if (styleColorRGB == epicColor1RGB || styleColorRGB == epicColor2RGB) {
						return 1;
					}
					// Check for Legendary color (floor 85)
					if (styleColorRGB == legendaryColor1RGB || styleColorRGB == legendaryColor2RGB) {
						return 85;
					}
				}
			} else {
				// Stop if we've reached the dash
				if (stringPos >= dashIndex) {
					break;
				}
			}
			
			// Update string position based on codePoint
			if (codePoint < 0x10000) {
				stringPos++;
			} else {
				stringPos += 2; // Surrogate pair
			}
		}
		
		return null; // Could not determine floor from color
	}
	
	/**
	 * Appends the floor number tag at the end of the text (after "[Bauplan]")
	 */
	private static Text appendFloorNumberToText(Text originalText, String floorTag) {
		try {
			// Convert to OrderedText to preserve styling
			net.minecraft.text.OrderedText orderedText = originalText.asOrderedText();
			final java.util.List<net.minecraft.text.Style> styles = new java.util.ArrayList<>();
			final java.util.List<Integer> codePoints = new java.util.ArrayList<>();
			
			orderedText.accept((index, style, codePoint) -> {
				styles.add(style);
				codePoints.add(codePoint);
				return true;
			});
			
			// Build new text: original text + floor tag at the end
			net.minecraft.text.MutableText result = Text.empty().copy();
			
			// Add all original text - group consecutive characters with same style
			net.minecraft.text.Style currentStyle = null;
			StringBuilder currentSegment = new StringBuilder();
			
			for (int i = 0; i < codePoints.size(); i++) {
				int codePoint = codePoints.get(i);
				net.minecraft.text.Style style = styles.get(i);
				
				if (currentStyle == null || !currentStyle.equals(style)) {
					// Flush current segment
					if (currentSegment.length() > 0) {
						result.append(Text.literal(currentSegment.toString()).setStyle(currentStyle));
						currentSegment = new StringBuilder();
					}
					currentStyle = style;
				}
				currentSegment.appendCodePoint(codePoint);
			}
			
			// Flush remaining segment
			if (currentSegment.length() > 0) {
				result.append(Text.literal(currentSegment.toString()).setStyle(currentStyle));
			}
			
			// Add floor tag at the end with white color
			net.minecraft.text.Style floorStyle = net.minecraft.text.Style.EMPTY.withColor(0xFFFFFF);
			result.append(Text.literal(floorTag).setStyle(floorStyle));
			
			return result;
		} catch (Exception e) {
			// Fallback: simple string append
			String originalString = originalText.getString();
			String newString = originalString + floorTag;
			return Text.literal(newString);
		}
	}
	
	/**
	 * Gets the custom hex color for a rarity level
	 */
	private static int getRarityColor(String rarity) {
		switch (rarity.toLowerCase()) {
			case "common":
				return 0xFFFFFF; // White
			case "uncommon":
				return 0x1EFC00; // #1EFC00
			case "rare":
				return 0x006FDA; // #006FDA
			case "epic":
				return 0xA134EB; // #A134EB
			case "legendary":
				return 0xFC7E00; // #FC7E00
			case "mob":
				return 0xFFFFFF; // White for mob names
			default:
				return 0x808080; // Gray
		}
	}
	
	/**
	 * Gets the formatting for a rarity level
	 * Note: Custom hex colors would be:
	 * - Uncommon: #1EFC00
	 * - Rare: #006FDA  
	 * - Epic: #A134EB
	 * - Legendary: #FC7E00
	 */
	private static Formatting getRarityFormatting(String rarity) {
		switch (rarity.toLowerCase()) {
			case "common":
				return Formatting.WHITE;
			case "uncommon":
				return Formatting.GREEN; // Would be #1EFC00
			case "rare":
				return Formatting.BLUE; // Would be #006FDA
			case "epic":
				return Formatting.LIGHT_PURPLE; // Would be #A134EB
			case "legendary":
				return Formatting.GOLD; // Would be #FC7E00
			default:
				return Formatting.GRAY;
		}
	}
	
	/**
	 * Data class to store material information
	 */
	private static class MaterialInfo {
		public final int floor;
		public final String rarity;
		public final String color;
		
		public MaterialInfo(int floor, String rarity, String color) {
			this.floor = floor;
			this.rarity = rarity;
			this.color = color;
		}
	}
	
	/**
	 * Data class to store essence information
	 */
	private static class EssenceInfo {
		public final String name;
		public final String type;
		public final String tier;
		public final int wave;
		
		public EssenceInfo(String name, String type, String tier, int wave) {
			this.name = name;
			this.type = type;
			this.tier = tier;
			this.wave = wave;
		}
	}
	
	/**
	 * Data class to store aspect information
	 */
	public static class AspectInfo {
		public final String aspectName;
		public final String aspectDescription;
		
		public AspectInfo(String aspectName, String aspectDescription) {
			this.aspectName = aspectName;
			this.aspectDescription = aspectDescription;
		}
	}
	
	/**
	 * Data class to store license information
	 */
	private static class LicenseInfo {
		public final String name;
		public final String location;
		
		public LicenseInfo(String name, String location) {
			this.name = name;
			this.location = location;
		}
	}
	
	/**
	 * Data class to store MKLevel information
	 */
	private static class MKLevelInfo {
		public final int level;
		public final String essence;
		public final int amount;
		
		public MKLevelInfo(int level, String essence, int amount) {
			this.level = level;
			this.essence = essence;
			this.amount = amount;
		}
	}
	
	/**
	 * Data class to store Combined Wave information
	 */
	private static class CombinedWaveInfo {
		public final String level; // e.g., "1-13", "25+30"
		public final int wave;
		public final List<String> without; // Optional list of essences to exclude
		
		public CombinedWaveInfo(String level, int wave, List<String> without) {
			this.level = level;
			this.wave = wave;
			this.without = without != null ? without : new ArrayList<>();
		}
	}
	
	/**
	 * Data class to represent a single line to render
	 */
	private static class RenderLine {
		public final String text;
		public final int color;
		public final int xOffset; // Additional X offset for alignment
		public final boolean hasSecondPart; // If true, has a second part with different color
		public final String secondPartText;
		public final int secondPartColor;
		public final int secondPartXOffset; // X offset for second part
		public final boolean hasThirdPart; // If true, has a third part (for wave numbers in green)
		public final String thirdPartText;
		public final int thirdPartColor;
		public final int thirdPartXOffset; // X offset for third part
		
		public RenderLine(String text, int color) {
			this.text = text;
			this.color = color;
			this.xOffset = 0;
			this.hasSecondPart = false;
			this.secondPartText = null;
			this.secondPartColor = 0;
			this.secondPartXOffset = 0;
			this.hasThirdPart = false;
			this.thirdPartText = null;
			this.thirdPartColor = 0;
			this.thirdPartXOffset = 0;
		}
		
		public RenderLine(String text, int color, int xOffset, String secondPartText, int secondPartColor, int secondPartXOffset) {
			this.text = text;
			this.color = color;
			this.xOffset = xOffset;
			this.hasSecondPart = true;
			this.secondPartText = secondPartText;
			this.secondPartColor = secondPartColor;
			this.secondPartXOffset = secondPartXOffset;
			this.hasThirdPart = false;
			this.thirdPartText = null;
			this.thirdPartColor = 0;
			this.thirdPartXOffset = 0;
		}
		
		public RenderLine(String text, int color, int xOffset, String secondPartText, int secondPartColor, int secondPartXOffset, String thirdPartText, int thirdPartColor, int thirdPartXOffset) {
			this.text = text;
			this.color = color;
			this.xOffset = xOffset;
			this.hasSecondPart = true;
			this.secondPartText = secondPartText;
			this.secondPartColor = secondPartColor;
			this.secondPartXOffset = secondPartXOffset;
			this.hasThirdPart = true;
			this.thirdPartText = thirdPartText;
			this.thirdPartColor = thirdPartColor;
			this.thirdPartXOffset = thirdPartXOffset;
		}
	}
	
	/**
	 * Client tick callback for continuous XP calculation (like KillsUtility)
	 */
	private static void onClientTick(MinecraftClient client) {
		if (client == null || client.world == null || client.player == null) {
			return;
		}
		
		// Handle collection hotkeys
		handleCollectionHotkeys();
		
		// Check Tab key for overlay visibility
		checkTabKey();
		
		// Check for dimension changes for mining/lumberjack overlays
		checkMiningLumberjackDimensionChange(client);
		
		// Update XP per minute calculation (only if overlay is currently showing)
		if (miningXP.shouldShowOverlay && miningXP.sessionStartTime > 0) {
		updateXPM(miningXP);
		}
		if (lumberjackXP.shouldShowOverlay && lumberjackXP.sessionStartTime > 0) {
		updateXPM(lumberjackXP);
		}
		
		// Update overlay visibility timers
		updateOverlayVisibility(miningXP);
		updateOverlayVisibility(lumberjackXP);
		
		// Collection tracking
		if (CCLiveUtilitiesConfig.HANDLER.instance().enableMod) {
			// Check if overlay was disabled/enabled and handle timer accordingly
			boolean currentShowCollectionOverlay = CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay;
			if (currentShowCollectionOverlay != lastShowCollectionOverlayState) {
				if (!currentShowCollectionOverlay) {
					// Overlay was disabled - stop and reset timer
					resetCollectionTracking();
					sessionStartTime = 0; // Stop timer
				} else if (isTrackingCollections) {
					// Overlay was enabled - restart timer if we're tracking
					resetCollectionTracking();
					sessionStartTime = System.currentTimeMillis();
				}
				lastShowCollectionOverlayState = currentShowCollectionOverlay;
			}
			
			// Check for dimension changes and reset if necessary
			checkCollectionDimensionChange(client);
			
			// Check if we should track collections (in farmworld dimension)
			boolean shouldTrack = isInFarmworldDimension(client);
			if (shouldTrack != isTrackingCollections) {
				isTrackingCollections = shouldTrack;
				if (shouldTrack) {
					// Just entered farmworld, reset everything
					resetCollectionTracking();
					// Reset biom detection
					biomDetected = false;
					currentBiomName = null;
					// Check for biom in scoreboard
					checkCollectionBiomChange(client);
					// Only start timer if overlay is enabled and biom is detected
					if (currentShowCollectionOverlay && biomDetected) {
					sessionStartTime = System.currentTimeMillis();
					} else {
						sessionStartTime = 0;
					}
				} else {
					// Left farmworld, reset biom detection
					biomDetected = false;
					currentBiomName = null;
					sessionStartTime = 0;
				}
			}
			
			// Check for biom changes (always check when in farmworld, not just when tracking)
			// This ensures biomDetected is up to date for the overlay editor
			if (isTrackingCollections) {
				checkCollectionBiomChange(client);
				
				// Handle pending resets (multiple resets in quick succession after biome change)
				if (pendingResets > 0) {
					resetCollectionTracking();
					// Restart timer if overlay is enabled
					if (CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay) {
						sessionStartTime = System.currentTimeMillis();
					} else {
						sessionStartTime = 0;
					}
					pendingResets--;
				}
				
				// Update timer based on biom detection
				if (biomDetected && currentShowCollectionOverlay && sessionStartTime == 0) {
					// Biom detected and overlay enabled, but timer not started - start it
					sessionStartTime = System.currentTimeMillis();
				} else if (!biomDetected && sessionStartTime > 0) {
					// Biom no longer detected, stop timer
					sessionStartTime = 0;
				}
			} else if (isInFarmworldDimension(client)) {
				// Also check when in farmworld but not tracking yet (for overlay editor)
				checkCollectionBiomChange(client);
			}
			
			// Update blocks per minute calculation (only if overlay is enabled and tracking)
			if (isTrackingCollections && currentShowCollectionOverlay) {
				updateBlocksPerMinute();
			}
		}
		
		// Check if we're in MKLevel inventory
		if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen) {
			Text titleText = handledScreen.getTitle();
			String title = getPlainTextFromText(titleText);
			String titleWithUnicode = titleText.getString(); // Behält Unicode-Zeichen für Essence Harvester UI
			boolean wasInMKLevelInventory = isInMKLevelInventory;
			// Prüfe sowohl "Machtkristalle Verbessern" als auch Essence Harvester UI
			isInMKLevelInventory = title.contains("Machtkristalle Verbessern") || 
			                        net.felix.utilities.Overall.ZeichenUtility.containsEssenceHarvesterUi(titleWithUnicode);
			
			// Reset search when leaving inventory (nur wenn man wirklich ein anderes Inventar öffnet)
			// Die Scroll-Position wird NICHT zurückgesetzt, damit sie beim erneuten Öffnen erhalten bleibt
			if (wasInMKLevelInventory && !isInMKLevelInventory) {
				mkLevelSearchText = "";
				mkLevelSearchFocused = false;
				mkLevelSearchCursorPosition = 0;
				setMKLevelScrollOffset(0);
			}
			
			if (isInMKLevelInventory) {
				// Update cursor blink
				if (mkLevelSearchFocused) {
					long currentTime = System.currentTimeMillis();
					if (currentTime - mkLevelSearchCursorBlinkTime > 500) {
						mkLevelSearchCursorVisible = !mkLevelSearchCursorVisible;
						mkLevelSearchCursorBlinkTime = currentTime;
					}
				} else {
					mkLevelSearchCursorVisible = false;
				}
				// Get actual inventory dimensions using reflection
				try {
					java.lang.reflect.Field xField = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField("x");
					java.lang.reflect.Field yField = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField("y");
					java.lang.reflect.Field bgWidthField = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField("backgroundWidth");
					java.lang.reflect.Field bgHeightField = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField("backgroundHeight");
					
					xField.setAccessible(true);
					yField.setAccessible(true);
					bgWidthField.setAccessible(true);
					bgHeightField.setAccessible(true);
					
					int inventoryX = xField.getInt(handledScreen);
					int inventoryY = yField.getInt(handledScreen);
					int inventoryWidth = bgWidthField.getInt(handledScreen);
					int inventoryHeight = bgHeightField.getInt(handledScreen);
					
					handleMKLevelScrolling(client, inventoryX, inventoryY, inventoryWidth, inventoryHeight);
				} catch (Exception e) {
					// Fallback to default values
					int inventoryWidth = 176;
					int inventoryHeight = 166;
					int inventoryX = (client.getWindow().getScaledWidth() - inventoryWidth) / 2;
					int inventoryY = (client.getWindow().getScaledHeight() - inventoryHeight) / 2;
					handleMKLevelScrolling(client, inventoryX, inventoryY, inventoryWidth, inventoryHeight);
				}
			}
		} else {
			isInMKLevelInventory = false;
		}
	}
	
	/**
	 * Handles scrolling for MKLevel overlay
	 */
	private static void handleMKLevelScrolling(MinecraftClient client, int inventoryX, int inventoryY, int inventoryWidth, int inventoryHeight) {
		if (client == null || client.getWindow() == null) {
			return;
		}
		
		int windowWidth = client.getWindow().getWidth();
		int windowHeight = client.getWindow().getHeight();
		
		if (windowWidth <= 0 || windowHeight <= 0) {
			return;
		}
		
		int mouseX = (int) client.mouse.getX() * client.getWindow().getScaledWidth() / windowWidth;
		int mouseY = (int) client.mouse.getY() * client.getWindow().getScaledHeight() / windowHeight;
		
		// Calculate overlay position using config (same as renderMKLevelOverlay)
		int screenWidth = client.getWindow().getScaledWidth();
		
		if (screenWidth <= 0) {
			return;
		}
		
		// Get position from config (same as renderMKLevelOverlay)
		int xPos = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelX;
		int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelY;
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale;
		if (scale <= 0) scale = 1.0f;
		
		// Unscaled dimensions
		int unscaledWidth = 200;
		int unscaledHeight = inventoryHeight;
		
		// Scaled dimensions
		int overlayWidth = Math.round(unscaledWidth * scale);
		int overlayHeight = Math.round(unscaledHeight * scale);
		
		// Calculate overlay X position
		// Wenn xPos -1 ist, berechne automatisch rechts (für Kompatibilität)
		int overlayX;
		if (xPos == -1) {
			overlayX = screenWidth - overlayWidth - 10; // 10px Abstand vom rechten Rand
		} else {
			// Verwende die absolute X-Position (obere linke Ecke)
			overlayX = xPos;
		}
		
		// Y-Position: Wenn yOffset -1 ist (Standard), verwende die Inventar-Y-Position
		// Ansonsten verwende die absolute Y-Position aus der Config
		int overlayY = (yOffset == -1) ? inventoryY : yOffset;
		
		// Check if mouse is over the overlay (excluding search bar area for scrolling)
		// Coordinates need to account for scaling
		int searchBarHeight = Math.round(16 * scale);
		int padding = Math.round(5 * scale);
		int contentOffset = Math.round(3 * scale); // Shift content 3px up (same as in render)
		int searchBarY = overlayY + padding - contentOffset; // Shift search bar 3px up
		int contentY = searchBarY + searchBarHeight + Math.round(2 * scale);
		
		// Mouse is over overlay if it's in the content area (not search bar) or search bar itself
		boolean overSearchBar = mouseX >= overlayX + padding && mouseX <= overlayX + overlayWidth - padding &&
								mouseY >= searchBarY && mouseY <= searchBarY + searchBarHeight;
		boolean overContent = mouseX >= overlayX && mouseX <= overlayX + overlayWidth &&
							  mouseY >= contentY && mouseY <= overlayY + overlayHeight - padding;
		
		mkLevelOverlayHovered = overSearchBar || overContent;
	}
	
	/**
	 * Handles mouse scroll for MKLevel overlay
	 */
	public static void onMKLevelMouseScroll(double vertical) {
		if (!isInMKLevelInventory) {
			return;
		}
		
		if (mkLevelOverlayHovered) {
			// Scroll by line height for smooth line-by-line scrolling
			int lineHeight = 12;
			int scrollAmount = (int) (vertical * lineHeight);
			int currentOffset = getMKLevelScrollOffset();
			currentOffset -= scrollAmount;
			currentOffset = Math.max(0, currentOffset);
			setMKLevelScrollOffset(currentOffset);
			
			// Limit scroll offset based on actual entry heights (including spacing)
			MinecraftClient client = MinecraftClient.getInstance();
			if (client != null && client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen) {
				try {
					java.lang.reflect.Field bgHeightField = net.minecraft.client.gui.screen.ingame.HandledScreen.class.getDeclaredField("backgroundHeight");
					bgHeightField.setAccessible(true);
					int inventoryHeight = bgHeightField.getInt(handledScreen);
					
					int padding = 5;
					int searchBarHeight = 16;
					int contentOffset = 3; // Same as in render
					// Account for contentOffset in available height calculation
					int availableHeight = inventoryHeight - padding * 2 - searchBarHeight - 2 - contentOffset;
					
					int totalHeight = 0;
					int lastEntryHeight = 0;
					
					if (mkLevelShowIndividualWaves) {
						// Get filtered entries
						List<MKLevelInfo> filteredEntries = filterMKLevelEntries(mkLevelSearchText);
						
						// Calculate actual heights for all entries (including spacing)
						for (MKLevelInfo levelInfo : filteredEntries) {
							// Base height: Level (1 line) + Essenz (1 line) = 2 lines
							int height = 2 * lineHeight;
							// Add 1 line if wave is present
							if (findWaveForEssence(levelInfo.essence) != null) {
								height += lineHeight;
							}
							// Add 1 line for spacing after each entry
							height += lineHeight;
							totalHeight += height;
							lastEntryHeight = height; // Store the last entry height
						}
					} else {
						// Get filtered combined waves
						List<CombinedWaveInfo> filteredCombinedWaves = filterCombinedWaves(mkLevelSearchText);
						
						// Calculate actual heights for all entries (including spacing)
						for (CombinedWaveInfo waveInfo : filteredCombinedWaves) {
							// Base height: Level (1 line) + Wave (1 line) = 2 lines
							int height = 2 * lineHeight;
							// Add 1 line for "Ohne:" header if "without" is present
							// Then add 1 line for each essence in "without" list
							if (waveInfo.without != null && !waveInfo.without.isEmpty()) {
								height += lineHeight; // "Ohne:" header
								height += waveInfo.without.size() * lineHeight; // One line per essence
							}
							// Add 1 line for spacing after each entry
							height += lineHeight;
							totalHeight += height;
							lastEntryHeight = height; // Store the last entry height
						}
					}
					
					// Maximum scroll should allow us to show the last entry at the bottom
					// Add the height of the last entry to ensure it's fully visible
					int maxScroll = Math.max(0, totalHeight - availableHeight + lastEntryHeight);
					setMKLevelScrollOffset(Math.min(getMKLevelScrollOffset(), maxScroll));
				} catch (Exception e) {
					// Fallback: use default calculation with actual heights
					int totalHeight = 0;
					int lastEntryHeight = 0;
					
					if (mkLevelShowIndividualWaves) {
						List<MKLevelInfo> filteredEntries = filterMKLevelEntries(mkLevelSearchText);
						
						// Calculate actual heights for all entries (including spacing)
						for (MKLevelInfo levelInfo : filteredEntries) {
							int height = 2 * lineHeight;
							if (findWaveForEssence(levelInfo.essence) != null) {
								height += lineHeight;
							}
							height += lineHeight; // Spacing
							totalHeight += height;
							lastEntryHeight = height;
						}
					} else {
						List<CombinedWaveInfo> filteredCombinedWaves = filterCombinedWaves(mkLevelSearchText);
						
						// Calculate actual heights for all entries (including spacing)
						for (CombinedWaveInfo waveInfo : filteredCombinedWaves) {
							int height = 2 * lineHeight;
							if (waveInfo.without != null && !waveInfo.without.isEmpty()) {
								height += lineHeight; // "Ohne:" header
								height += waveInfo.without.size() * lineHeight; // One line per essence
							}
							height += lineHeight; // Spacing
							totalHeight += height;
							lastEntryHeight = height;
						}
					}
					
					// Estimate available height (default inventory height minus search bar and padding)
					int estimatedAvailableHeight = 166 - 16 - 10 - 3; // Default inventory height minus search bar, padding, contentOffset
					// Add the height of the last entry to ensure it's fully visible
					int maxScroll = Math.max(0, totalHeight - estimatedAvailableHeight + lastEntryHeight);
					setMKLevelScrollOffset(Math.min(getMKLevelScrollOffset(), maxScroll));
				}
			} else {
				// Fallback: use default calculation with actual heights
				int totalHeight = 0;
				int lastEntryHeight = 0;
				
				if (mkLevelShowIndividualWaves) {
					List<MKLevelInfo> filteredEntries = filterMKLevelEntries(mkLevelSearchText);
					
					// Calculate actual heights for all entries (including spacing)
					for (MKLevelInfo levelInfo : filteredEntries) {
						int height = 2 * lineHeight;
						if (findWaveForEssence(levelInfo.essence) != null) {
							height += lineHeight;
						}
						height += lineHeight; // Spacing
						totalHeight += height;
						lastEntryHeight = height;
					}
				} else {
					List<CombinedWaveInfo> filteredCombinedWaves = filterCombinedWaves(mkLevelSearchText);
					
					// Calculate actual heights for all entries (including spacing)
					for (CombinedWaveInfo waveInfo : filteredCombinedWaves) {
						int height = 2 * lineHeight;
						if (waveInfo.without != null && !waveInfo.without.isEmpty()) {
							height += lineHeight; // "Ohne:" header
							height += waveInfo.without.size() * lineHeight; // One line per essence
						}
						height += lineHeight; // Spacing
						totalHeight += height;
						lastEntryHeight = height;
					}
				}
				
				// Estimate available height (default inventory height minus search bar and padding)
				int estimatedAvailableHeight = 166 - 16 - 10 - 3; // Default inventory height minus search bar, padding, contentOffset
				// Add the height of the last entry to ensure it's fully visible
				int maxScroll = Math.max(0, totalHeight - estimatedAvailableHeight + lastEntryHeight);
				setMKLevelScrollOffset(Math.min(getMKLevelScrollOffset(), maxScroll));
			}
		}
	}
	
	/**
	 * Checks if Tab key is pressed and hides overlays accordingly
	 */
	private static void checkTabKey() {
		// Check if player list key is pressed (respects custom key bindings)
		if (net.felix.utilities.Overall.KeyBindingUtility.isPlayerListKeyPressed()) {
			showOverlays = false; // Hide overlays when player list key is pressed
		} else {
			showOverlays = true; // Show overlays when player list key is released
		}
	}
	
	/**
	 * Updates overlay visibility based on timer
	 * Overlay is visible for 10 seconds after last XP change
	 */
	private static void updateOverlayVisibility(XPData xpData) {
		long currentTime = System.currentTimeMillis();
		
		if (xpData.lastXPChangeTime > 0) {
			long timeSinceLastChange = currentTime - xpData.lastXPChangeTime;
			// Show overlay if less than 10 seconds have passed since last XP change
			boolean wasShowing = xpData.shouldShowOverlay;
			xpData.shouldShowOverlay = timeSinceLastChange < OVERLAY_DISPLAY_DURATION;
			
			// Stop timer when overlay is hidden
			if (wasShowing && !xpData.shouldShowOverlay) {
				// Overlay just got hidden - stop timer
				xpData.sessionStartTime = 0;
				xpData.xpPerMinute = 0.0;
				xpData.isTracking = false;
			}
			
			// Reset XP calculation 10 seconds after overlay is hidden (20 seconds total)
			// This prevents stale data from lingering
			if (timeSinceLastChange >= OVERLAY_DISPLAY_DURATION + 10000) {
				// Reset all tracking data
				xpData.sessionStartTime = 0;
				xpData.newXP = 0;
				xpData.xpPerMinute = 0.0;
				xpData.isTracking = false;
				xpData.initialXP = -1;
				// Keep lastXPChangeTime for potential future use
			}
		} else {
			xpData.shouldShowOverlay = false;
			// If overlay is not showing and timer is running, stop it
			if (xpData.sessionStartTime > 0) {
				xpData.sessionStartTime = 0;
				xpData.xpPerMinute = 0.0;
				xpData.isTracking = false;
			}
		}
	}
	
	/**
	 * Updates XP per minute calculation continuously (like updateKPM in KillsUtility)
	 */
	private static void updateXPM(XPData xpData) {
		if (xpData.sessionStartTime != 0 && xpData.newXP > 0) {
			long currentTime = System.currentTimeMillis();
			long sessionDuration = currentTime - xpData.sessionStartTime;
			if (sessionDuration > 0) {
				double minutesElapsed = (double)sessionDuration / 60000.0;
				xpData.xpPerMinute = (double)xpData.newXP / minutesElapsed;
			}
		} else {
			xpData.xpPerMinute = 0.0;
		}
	}
	
	/**
	 * HUD Render callback for mining and lumberjack overlays
	 */
	private static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.world == null || client.player == null) {
			return;
		}
		
		// Hide overlay if F1 menu (debug screen) is open
		if (client.options.hudHidden) {
			return;
		}
		
		// Hide overlays if Tab key is pressed or F1 is toggled
		if (!showOverlays) {
			return;
		}
		
		// Update XP data from tab list (works even when tab list is closed)
		updateXPFromTabList(client);
		
		// Update overlay visibility timers (after XP data update)
		updateOverlayVisibility(miningXP);
		updateOverlayVisibility(lumberjackXP);
		
		// Check if player is in their own dimension (dimension name matches player name)
		// or in a dimension that contains "floor"
		// If so, don't show overlays
		boolean shouldHideOverlays = false;
		if (client.player != null && client.world != null) {
			String playerName = client.player.getName().getString().toLowerCase();
			String dimensionPath = client.world.getRegistryKey().getValue().getPath();
			boolean isInPlayerNameDimension = dimensionPath.equals(playerName);
			boolean isInFloorDimension = dimensionPath.toLowerCase().contains("floor");
			shouldHideOverlays = isInPlayerNameDimension || isInFloorDimension;
		}
		
		// Render mining overlay (only when XP changed recently and not in player's own dimension or floor dimension)
		if (CCLiveUtilitiesConfig.HANDLER.instance().enableMod &&
			CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().miningOverlayEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().showMiningOverlay &&
			miningXP.shouldShowOverlay &&
			!shouldHideOverlays) {
			renderMiningOverlay(context, client);
		}
		
		// Render collection overlay (only if biom is detected in scoreboard)
		if (CCLiveUtilitiesConfig.HANDLER.instance().enableMod &&
			isTrackingCollections &&
			biomDetected &&
			!shouldHideOverlays) {
			// Try to read scoreboard when overlay is active
			checkCollectionBiomChange(client);
			renderCollectionOverlay(context, client);
		}
		
		// Render lumberjack overlay (only when XP changed recently and not in player's own dimension or floor dimension)
		if (CCLiveUtilitiesConfig.HANDLER.instance().enableMod &&
			CCLiveUtilitiesConfig.HANDLER.instance().miningLumberjackOverlayEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().lumberjackOverlayEnabled &&
			CCLiveUtilitiesConfig.HANDLER.instance().showLumberjackOverlay &&
			lumberjackXP.shouldShowOverlay &&
			!shouldHideOverlays) {
			renderLumberjackOverlay(context, client);
		}
	}
	
	/**
	 * Updates XP data from tab list for both mining and lumberjack
	 * Works even when the tab list is closed - the player list is always available
	 */
	private static void updateXPFromTabList(MinecraftClient client) {
		// Check for dimension change and reset initialization flags
		if (client.world != null) {
			String newDimension = client.world.getRegistryKey().getValue().getPath();
			if (currentDimension == null || !currentDimension.equals(newDimension)) {
				// Dimension changed - reset initialization flags
				miningXP.isInitializedInCurrentDimension = false;
				lumberjackXP.isInitializedInCurrentDimension = false;
				currentDimension = newDimension;
			}
		}
		// Only check every 0.5 seconds to avoid performance issues
		long currentTime = System.currentTimeMillis();
		long timeSinceLastCheck = currentTime - lastTabListCheck;
		
		if (timeSinceLastCheck < 500) {
			return;
		}
		lastTabListCheck = currentTime;
		
		if (client == null || client.getNetworkHandler() == null) {
			return;
		}
		
		// Get player list - this works even when tab list is closed
		// The player list is always available in the network handler
		var playerList = client.getNetworkHandler().getPlayerList();
		if (playerList == null) {
			return;
		}
		
		// Convert to list to iterate with index
		java.util.List<net.minecraft.client.network.PlayerListEntry> entries = 
			new java.util.ArrayList<>(playerList);
		
		// Helper method to remove Minecraft formatting codes (§ codes)
		java.util.function.Function<String, String> removeFormatting = (text) -> {
			if (text == null) return "";
			// Remove all § codes (formatting codes)
			return text.replaceAll("§[0-9a-fk-or]", "").trim();
		};
		
		// Helper method to get text from an entry
		java.util.function.Function<Integer, String> getEntryText = (entryIndex) -> {
			if (entryIndex < 0 || entryIndex >= entries.size()) {
				return null;
			}
			var entry = entries.get(entryIndex);
			if (entry == null) {
				return null;
			}
			
			net.minecraft.text.Text displayName = entry.getDisplayName();
			if (displayName != null) {
				return displayName.getString();
			} else if (entry.getProfile() != null) {
				return entry.getProfile().getName();
			}
			return null;
		};
		
		// Search for "[Bergbau]" and "[Holzfäller]"
		for (int i = 0; i < entries.size(); i++) {
			String entryText = getEntryText.apply(i);
			if (entryText == null) {
				continue;
			}
			
			String cleanEntryText = removeFormatting.apply(entryText);
			
			// Check for "[Bergbau]" - case insensitive and check for variations
			String lowerCleanText = cleanEntryText.toLowerCase();
			if (lowerCleanText.contains("[bergbau]") || cleanEntryText.contains("[Bergbau]")) {
				// Search for XP line after [Bergbau] - it might not be at index + 1
				// Look for a line matching the XP format: "NUMBER / NUMBER [NUMBER]"
				for (int j = i + 1; j < Math.min(i + 5, entries.size()); j++) {
					String xpText = getEntryText.apply(j);
					if (xpText != null) {
						String cleanXPText = removeFormatting.apply(xpText);
						// Check if this line matches the XP format (contains "/" and "[")
						if (cleanXPText.contains("/") && cleanXPText.contains("[")) {
							parseXPLine(cleanXPText, miningXP);
							break;
						}
					}
				}
			}
			
			// Check for "[Holzfäller]" - case insensitive and check for variations
			// Also check for variations like "Holzfaeller" (without umlaut)
			boolean isHolzfaeller = lowerCleanText.contains("[holzfäller]") || 
			                        cleanEntryText.contains("[Holzfäller]") ||
			                        lowerCleanText.contains("[holzfaeller]") ||
			                        cleanEntryText.contains("[Holzfaeller]");
			if (isHolzfaeller) {
				// Search for XP line after [Holzfäller] - it might not be at index + 1
				// Look for a line matching the XP format: "NUMBER / NUMBER [NUMBER]"
				for (int j = i + 1; j < Math.min(i + 5, entries.size()); j++) {
					String xpText = getEntryText.apply(j);
					if (xpText != null) {
						String cleanXPText = removeFormatting.apply(xpText);
						// Check if this line matches the XP format (contains "/" and "[")
						if (cleanXPText.contains("/") && cleanXPText.contains("[")) {
							parseXPLine(cleanXPText, lumberjackXP);
							break;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Parses XP line in format: "AKTUELLE XP / BENÖTIGTE XP [Level]"
	 */
	private static void parseXPLine(String xpLine, XPData xpData) {
		if (xpLine == null || xpLine.isEmpty()) {
			return;
		}
		
		try {
			// Format: "AKTUELLE XP / BENÖTIGTE XP [Level]"
			// Example: "1234 / 5000 [5]"
			
			// Extract level from [Level]
			java.util.regex.Pattern levelPattern = java.util.regex.Pattern.compile("\\[(\\d+)\\]");
			java.util.regex.Matcher levelMatcher = levelPattern.matcher(xpLine);
			if (levelMatcher.find()) {
				xpData.level = Integer.parseInt(levelMatcher.group(1));
			}
			
			// Extract current XP and required XP
			// Remove level part first
			String xpPart = xpLine.replaceAll("\\[\\d+\\]", "").trim();
			
			// Split by "/"
			String[] parts = xpPart.split("/");
			if (parts.length == 2) {
				// Remove all non-digit characters except dots and commas (for thousands separators)
				String currentXPStr = parts[0].trim().replaceAll("[^0-9.,]", "").replace(",", "").replace(".", "");
				String requiredXPStr = parts[1].trim().replaceAll("[^0-9.,]", "").replace(",", "").replace(".", "");
				
				long newCurrentXP = Long.parseLong(currentXPStr);
				long newRequiredXP = Long.parseLong(requiredXPStr);
				
				// Always update current XP and required XP first (before checking for changes)
				// This ensures we have the latest values for comparison
				long oldCurrentXP = xpData.currentXP;
				xpData.currentXP = newCurrentXP;
				xpData.requiredXP = newRequiredXP;
				
				// Check if this is the first time we're seeing values in this dimension
				boolean isFirstTimeInDimension = !xpData.isInitializedInCurrentDimension;
				
				// Mark as initialized if we have valid XP data
				if (newCurrentXP > 0) {
					xpData.isInitializedInCurrentDimension = true;
				}
				
				// Check if XP changed (but ignore if this is the first time in this dimension)
				boolean xpChanged = (newCurrentXP != oldCurrentXP);
				
				// Track previous overlay state to detect when overlay is shown for the first time
				boolean wasShowingOverlay = xpData.shouldShowOverlay;
				
				// Only show overlay if XP changed AND it's not the first time we're seeing values in this dimension
				if (xpChanged && !isFirstTimeInDimension) {
					long xpGained = newCurrentXP - oldCurrentXP;
					
					// XP increased - track the gain and show overlay
					if (xpGained > 0) {
						xpData.lastGainedXP = xpGained;
						// Reset overlay timer - show overlay for 10 seconds
						xpData.lastXPChangeTime = System.currentTimeMillis();
						xpData.shouldShowOverlay = true;
						
						// Start timer when overlay is shown for the first time
						if (!wasShowingOverlay && xpData.sessionStartTime == 0) {
							xpData.isTracking = true;
							xpData.initialXP = newCurrentXP - xpGained; // Set initial to value before this gain
							xpData.newXP = xpGained;
							xpData.sessionStartTime = System.currentTimeMillis();
						} else if (xpData.sessionStartTime > 0) {
							// Timer already running, just update newXP
							if (xpData.initialXP >= 0) {
								xpData.newXP = newCurrentXP - xpData.initialXP;
							}
						}
						
						// Hide the other overlay when this one gets XP
						if (xpData == miningXP) {
							lumberjackXP.shouldShowOverlay = false;
							lumberjackXP.lastXPChangeTime = 0;
						} else if (xpData == lumberjackXP) {
							miningXP.shouldShowOverlay = false;
							miningXP.lastXPChangeTime = 0;
						}
					} else if (xpGained < 0) {
						// XP decreased (level up or reset) - reset tracking like KillsUtility
						xpData.initialXP = newCurrentXP;
						xpData.newXP = 0;
						// Only start timer if overlay is being shown
						if (!wasShowingOverlay) {
						xpData.sessionStartTime = System.currentTimeMillis();
						} else {
							xpData.sessionStartTime = System.currentTimeMillis(); // Reset timer on level up
						}
						xpData.isTracking = true;
						// Also reset overlay timer on level up
						xpData.lastXPChangeTime = System.currentTimeMillis();
						xpData.shouldShowOverlay = true;
					}
				}
				
				// Calculate newXP (like newKills in KillsUtility)
				// Only update if timer is running (overlay is showing)
				if (xpData.initialXP >= 0 && xpData.sessionStartTime > 0) {
					xpData.newXP = newCurrentXP - xpData.initialXP;
					if (xpData.newXP < 0) {
						// Level up happened, reset
						xpData.initialXP = newCurrentXP;
						xpData.newXP = 0;
						xpData.sessionStartTime = System.currentTimeMillis();
					}
				}
			}
		} catch (Exception e) {
			// Silently ignore parsing errors
		}
	}
	
	/**
	 * Calculates time until next level in minutes
	 * Always uses average XP per minute (xpPerMinute) for consistent calculation
	 */
	private static double calculateTimeToNextLevel(XPData xpData) {
		// Always use average XP per minute for time calculation
		// This gives a consistent estimate based on your overall mining speed
		if (xpData.xpPerMinute <= 0) {
			return -1; // Unknown - no data yet
		}
		
		long xpNeeded = xpData.requiredXP - xpData.currentXP;
		if (xpNeeded <= 0) {
			return 0; // Already at max or level up
		}
		
		return xpNeeded / xpData.xpPerMinute;
	}
	
	/**
	 * Formats a number with thousand separators (points)
	 */
	private static String formatNumberWithSeparator(long number) {
		if (number == 0) {
			return "0";
		}
		// Use German locale format (points as thousand separators)
		java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.GERMAN);
		return formatter.format(number);
	}
	
	/**
	 * Formats a double number with thousand separators (points) and one decimal place
	 */
	private static String formatDoubleWithSeparator(double number) {
		if (number == 0) {
			return "0,0";
		}
		// Use German locale format (points as thousand separators, comma as decimal separator)
		java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(java.util.Locale.GERMAN);
		formatter.setMinimumFractionDigits(1);
		formatter.setMaximumFractionDigits(1);
		return formatter.format(number);
	}
	
	/**
	 * Formats time in minutes to readable string
	 */
	private static String formatTime(double minutes) {
		if (minutes < 0) {
			return "Unbekannt";
		}
		
		if (minutes < 1) {
			return "< 1 Min";
		}
		
		if (minutes < 60) {
			return String.format("%.1f Min", minutes);
		}
		
		int hours = (int) (minutes / 60);
		int mins = (int) (minutes % 60);
		return String.format("%d Std %d Min", hours, mins);
	}
	
	/**
	 * Renders mining overlay
	 */
	private static void renderMiningOverlay(DrawContext context, MinecraftClient client) {
		if (client.getWindow() == null) {
			return;
		}
		
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		int screenWidth = client.getWindow().getScaledWidth();
		
		// Base position from config
		int baseX = config.miningOverlayX;
		int y = config.miningOverlayY;
		
		// Calculate overlay dimensions
		int padding = 5;
		int lineHeight = 12;
		int lines = 5; // Header, Last XP, XP/Min, Required XP, Time to next level
		int overlayHeight = padding * 2 + lineHeight * lines;
		int minOverlayWidth = 100;
		
		// Calculate text width
		String header = miningXP.level > 0 ? String.format("Bergbau [lvl. %d]", miningXP.level) : "Bergbau";
		String lastXP = "Letzte XP: " + (miningXP.lastGainedXP > 0 ? formatNumberWithSeparator(miningXP.lastGainedXP) : "0");
		String xpPerMin = miningXP.xpPerMinute > 0 ? "XP/Min: " + formatDoubleWithSeparator(miningXP.xpPerMinute) : "XP/Min: -";
		long xpNeeded = miningXP.requiredXP - miningXP.currentXP;
		String requiredXP = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(miningXP));
		
		int maxWidth = Math.max(
			Math.max(client.textRenderer.getWidth(header),
				Math.max(client.textRenderer.getWidth(lastXP),
					Math.max(client.textRenderer.getWidth(xpPerMin),
						Math.max(client.textRenderer.getWidth(requiredXP),
							client.textRenderer.getWidth(timeToNext))))),
			minOverlayWidth);
		int overlayWidth = maxWidth + padding * 2;
		
		// Determine if overlay is on left or right side of screen
		// baseX is the left edge position at minimum width
		boolean isOnLeftSide = baseX < screenWidth / 2;
		
		// Calculate X position based on side
		int x;
		if (isOnLeftSide) {
			// On left side: keep left edge fixed, expand to the right
			x = baseX;
		} else {
			// On right side: keep right edge fixed, expand to the left
			// Right edge at minimum width is: baseX + minOverlayWidth
			// Keep this right edge fixed, so left edge moves left when width increases
			int rightEdge = baseX + minOverlayWidth;
			x = rightEdge - overlayWidth;
		}
		
		// Ensure overlay stays within screen bounds
		x = Math.max(0, Math.min(x, screenWidth - overlayWidth));
		
		// Get scale
		float scale = config.miningLumberjackOverlayScale;
		if (scale <= 0) scale = 1.0f;
		
		// Apply matrix transformations for scaling
		var matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(x, y);
		matrices.scale(scale, scale);
		
		// Draw background (scaled, relative to matrix)
		if (config.miningOverlayShowBackground) {
			context.fill(0, 0, overlayWidth, overlayHeight, 0x80000000);
		}
		
		// Get colors from config
		int headerColor = config.miningLumberjackOverlayHeaderColor.getRGB();
		int textColor = config.miningLumberjackOverlayTextColor.getRGB();
		
		// Draw text (scaled, relative to matrix)
		int textY = padding;
		context.drawText(client.textRenderer, header, padding, textY, headerColor, true);
		textY += lineHeight;
		context.drawText(client.textRenderer, lastXP, padding, textY, textColor, true);
		textY += lineHeight;
		context.drawText(client.textRenderer, xpPerMin, padding, textY, textColor, true);
		textY += lineHeight;
		context.drawText(client.textRenderer, timeToNext, padding, textY, textColor, true);
		textY += lineHeight;
		context.drawText(client.textRenderer, requiredXP, padding, textY, textColor, true);
		
		matrices.popMatrix();
	}
	
	/**
	 * Renders lumberjack overlay
	 */
	private static void renderLumberjackOverlay(DrawContext context, MinecraftClient client) {
		if (client.getWindow() == null) {
			return;
		}
		
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		int screenWidth = client.getWindow().getScaledWidth();
		
		// Base position from config (same as mining overlay)
		int baseX = config.miningOverlayX;
		int y = config.miningOverlayY;
		
		// Calculate overlay dimensions
		int padding = 5;
		int lineHeight = 12;
		int lines = 5; // Header, Last XP, XP/Min, Required XP, Time to next level
		int overlayHeight = padding * 2 + lineHeight * lines;
		int minOverlayWidth = 100;
		
		// Calculate text width
		String header = lumberjackXP.level > 0 ? String.format("Holzfäller [lvl. %d]", lumberjackXP.level) : "Holzfäller";
		String lastXP = "Letzte XP: " + (lumberjackXP.lastGainedXP > 0 ? formatNumberWithSeparator(lumberjackXP.lastGainedXP) : "0");
		String xpPerMin = lumberjackXP.xpPerMinute > 0 ? "XP/Min: " + formatDoubleWithSeparator(lumberjackXP.xpPerMinute) : "XP/Min: -";
		long xpNeeded = lumberjackXP.requiredXP - lumberjackXP.currentXP;
		String requiredXP = "Benötigte XP: " + (xpNeeded > 0 ? formatNumberWithSeparator(xpNeeded) : "0");
		String timeToNext = "Zeit bis Level: " + formatTime(calculateTimeToNextLevel(lumberjackXP));
		
		int maxWidth = Math.max(
			Math.max(client.textRenderer.getWidth(header),
				Math.max(client.textRenderer.getWidth(lastXP),
					Math.max(client.textRenderer.getWidth(xpPerMin),
						Math.max(client.textRenderer.getWidth(requiredXP),
							client.textRenderer.getWidth(timeToNext))))),
			minOverlayWidth);
		int overlayWidth = maxWidth + padding * 2;
		
		// Determine if overlay is on left or right side of screen
		// baseX is the left edge position at minimum width
		boolean isOnLeftSide = baseX < screenWidth / 2;
		
		// Calculate X position based on side
		int x;
		if (isOnLeftSide) {
			// On left side: keep left edge fixed, expand to the right
			x = baseX;
		} else {
			// On right side: keep right edge fixed, expand to the left
			// Right edge at minimum width is: baseX + minOverlayWidth
			// Keep this right edge fixed, so left edge moves left when width increases
			int rightEdge = baseX + minOverlayWidth;
			x = rightEdge - overlayWidth;
		}
		
		// Ensure overlay stays within screen bounds
		x = Math.max(0, Math.min(x, screenWidth - overlayWidth));
		
		// Get scale
		float scale = config.miningLumberjackOverlayScale;
		if (scale <= 0) scale = 1.0f;
		
		// Apply matrix transformations for scaling
		var matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(x, y);
		matrices.scale(scale, scale);
		
		// Draw background (scaled, relative to matrix)
		if (config.lumberjackOverlayShowBackground) {
			context.fill(0, 0, overlayWidth, overlayHeight, 0x80000000);
		}
		
		// Get colors from config
		int headerColor = config.miningLumberjackOverlayHeaderColor.getRGB();
		int textColor = config.miningLumberjackOverlayTextColor.getRGB();
		
		// Draw text (scaled, relative to matrix)
		int textY = padding;
		context.drawText(client.textRenderer, header, padding, textY, headerColor, true);
		textY += lineHeight;
		context.drawText(client.textRenderer, lastXP, padding, textY, textColor, true);
		textY += lineHeight;
		context.drawText(client.textRenderer, xpPerMin, padding, textY, textColor, true);
		textY += lineHeight;
		context.drawText(client.textRenderer, timeToNext, padding, textY, textColor, true);
		textY += lineHeight;
		context.drawText(client.textRenderer, requiredXP, padding, textY, textColor, true);
		
		matrices.popMatrix();
	}
	
	/**
	 * Renders MKLevel overlay
	 */
	public static void renderMKLevelOverlay(DrawContext context, MinecraftClient client, int inventoryX, int inventoryY, int inventoryWidth, int inventoryHeight) {
		if (client == null || client.getWindow() == null || !isInMKLevelInventory) {
			return;
		}
		
		// Check if MKLevel overlay is enabled
		if (!CCLiveUtilitiesConfig.HANDLER.instance().mkLevelEnabled) {
			return;
		}
		
		int screenWidth = client.getWindow().getScaledWidth();
		
		if (screenWidth <= 0) {
			return;
		}
		
		// Get position from config
		int xPos = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelX;
		int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelY;
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale;
		if (scale <= 0) scale = 1.0f;
		
		// Unscaled dimensions
		int unscaledWidth = 200;
		int unscaledHeight = inventoryHeight; // Same height as inventory
		
		// Cache the height for F6 editor (avoids reflection issues on server)
		mkLevelLastKnownHeight = inventoryHeight;
		
		// Scaled dimensions
		int overlayWidth = Math.round(unscaledWidth * scale);
		
		// Calculate overlay X position
		// Wenn xPos -1 ist, berechne automatisch rechts (für Kompatibilität)
		int overlayX;
		if (xPos == -1) {
			overlayX = screenWidth - overlayWidth - 10; // 10px Abstand vom rechten Rand
		} else {
			// Verwende die absolute X-Position (obere linke Ecke)
			overlayX = xPos;
		}
		
		// Y-Position: Wenn yOffset -1 ist (Standard), verwende die Inventar-Y-Position
		// Ansonsten verwende die absolute Y-Position aus der Config
		int overlayY = (yOffset == -1) ? inventoryY : yOffset;
		
		// Button height (unscaled)
		int buttonHeight = 20;
		
		// Apply matrix transformation for scaling
		org.joml.Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(overlayX, overlayY);
		matrices.scale(scale, scale);
		
		// Unscaled coordinates (will be scaled by matrix)
		int padding = 5;
		int lineHeight = 12;
		int searchBarHeight = 16; // Height of search bar
		int contentOffset = 3; // Shift content 3px up
		int textX = padding;
		
		// Draw buttons above overlay (at negative Y, aligned with top edge)
		int buttonY = -buttonHeight; // Above the overlay, aligned with top edge
		int buttonWidth = unscaledWidth / 2; // Each button takes half the width
		
		// Left button: "Einzelne Wellen"
		boolean leftButtonActive = mkLevelShowIndividualWaves;
		int leftButtonBgColor = leftButtonActive ? 0xFF404040 : 0xFF202020; // Highlighted: lighter gray, inactive: darker gray
		int leftButtonBorderColor = leftButtonActive ? 0xFFFFFF00 : 0xFF808080; // Highlighted: yellow, inactive: gray
		context.fill(0, buttonY, buttonWidth, buttonY + buttonHeight, leftButtonBgColor);
		context.drawBorder(0, buttonY, buttonWidth, buttonHeight, leftButtonBorderColor);
		
		// Center text in left button
		String leftButtonText = "Einzelne Wellen";
		int leftTextWidth = client.textRenderer.getWidth(leftButtonText);
		int leftTextX = (buttonWidth - leftTextWidth) / 2;
		int leftTextY = buttonY + (buttonHeight - client.textRenderer.fontHeight) / 2;
		context.drawText(client.textRenderer, leftButtonText, leftTextX, leftTextY, 0xFFFFFFFF, false);
		
		// Right button: "Kombinierte Wellen"
		boolean rightButtonActive = !mkLevelShowIndividualWaves;
		int rightButtonBgColor = rightButtonActive ? 0xFF404040 : 0xFF202020; // Highlighted: lighter gray, inactive: darker gray
		int rightButtonBorderColor = rightButtonActive ? 0xFFFFFF00 : 0xFF808080; // Highlighted: yellow, inactive: gray
		context.fill(buttonWidth, buttonY, unscaledWidth, buttonY + buttonHeight, rightButtonBgColor);
		context.drawBorder(buttonWidth, buttonY, buttonWidth, buttonHeight, rightButtonBorderColor);
		
		// Center text in right button
		String rightButtonText = "Kombinierte Wellen";
		int rightTextWidth = client.textRenderer.getWidth(rightButtonText);
		int rightTextX = buttonWidth + (buttonWidth - rightTextWidth) / 2;
		int rightTextY = buttonY + (buttonHeight - client.textRenderer.fontHeight) / 2;
		context.drawText(client.textRenderer, rightButtonText, rightTextX, rightTextY, 0xFFFFFFFF, false);
		
		int searchBarY = padding - contentOffset; // Shift search bar 3px up
		int contentY = searchBarY + searchBarHeight + 2; // Content starts below search bar
		int maxY = unscaledHeight - padding;
		
		// Draw background (scaled)
		context.fill(0, 0, unscaledWidth, unscaledHeight, 0x80000000);
		
		// Draw border (scaled)
		context.drawBorder(0, 0, unscaledWidth, unscaledHeight, 0xFFFFFFFF);
		
		// Draw search bar background (relative to matrix)
		int searchBarX = padding;
		int searchBarWidth = unscaledWidth - padding * 2;
		context.fill(searchBarX, searchBarY, searchBarX + searchBarWidth, searchBarY + searchBarHeight, 0xFF000000);
		context.drawBorder(searchBarX, searchBarY, searchBarWidth, searchBarHeight, mkLevelSearchFocused ? 0xFFFFFF00 : 0xFF808080);
		
		// Draw search text (vertically centered, relative to matrix)
		String displayText = mkLevelSearchText.isEmpty() ? "Suchen..." : mkLevelSearchText;
		int textColor = mkLevelSearchText.isEmpty() ? 0xFF808080 : 0xFFFFFFFF;
		// Calculate text height and center it vertically
		int textHeight = client.textRenderer.fontHeight; // Usually 9 pixels
		int searchTextY = searchBarY + (searchBarHeight - textHeight) / 2;
		context.drawText(client.textRenderer, displayText, searchBarX + 2, searchTextY, textColor, false);
		
		// Draw cursor if focused (vertically centered, relative to matrix)
		if (mkLevelSearchFocused && mkLevelSearchCursorVisible) {
			int cursorX = searchBarX + 2;
			if (!mkLevelSearchText.isEmpty() && mkLevelSearchCursorPosition > 0) {
				String textBeforeCursor = mkLevelSearchText.substring(0, Math.min(mkLevelSearchCursorPosition, mkLevelSearchText.length()));
				cursorX += client.textRenderer.getWidth(textBeforeCursor);
			}
			// Center cursor vertically
			int cursorY = searchBarY + (searchBarHeight - textHeight) / 2;
			context.fill(cursorX, cursorY, cursorX + 1, cursorY + textHeight, 0xFFFFFFFF);
		}
		
		// Calculate available height for content
		int availableHeight = unscaledHeight - padding * 2 - searchBarHeight - 2 - contentOffset;
		
		if (mkLevelShowIndividualWaves) {
			// Render "Einzelne Wellen" mode - convert to lines and render line by line
			List<MKLevelInfo> filteredEntries = filterMKLevelEntries(mkLevelSearchText);
			
			// Convert all entries to lines
			List<RenderLine> allLines = new ArrayList<>();
			for (MKLevelInfo levelInfo : filteredEntries) {
				// Level header (yellow)
				allLines.add(new RenderLine("-Level " + levelInfo.level, 0xFFFFFF00));
				
				// Essence and amount (white)
				allLines.add(new RenderLine(" " + levelInfo.essence + ", " + formatNumberWithSeparator(levelInfo.amount), 0xFFFFFFFF));
				
				// Wave if present
				Integer wave = findWaveForEssence(levelInfo.essence);
				if (wave != null) {
					String wavePrefix = "-> Welle: ";
					String waveNumber = String.valueOf(wave);
					allLines.add(new RenderLine(wavePrefix, 0xFFC0C0C0, 0, waveNumber, 0xFF55FF55, client.textRenderer.getWidth(wavePrefix)));
				}
				
				// Empty line for spacing
				allLines.add(new RenderLine("", 0xFFFFFFFF));
			}
			
			// Calculate total height and max scroll
			int totalHeight = allLines.size() * lineHeight;
			int maxScrollOffset = Math.max(0, totalHeight - availableHeight);
			
			// Limit scroll offset
			int scrollOffset = getMKLevelScrollOffset();
			if (scrollOffset > maxScrollOffset) {
				setMKLevelScrollOffset(maxScrollOffset);
				scrollOffset = maxScrollOffset;
			}
			
			// Calculate start line based on scroll offset
			int startLine = scrollOffset / lineHeight;
			int pixelOffset = scrollOffset % lineHeight;
			
			// Render lines
			int textY = contentY;
			int linesRendered = 0;
			
			// Show scroll indicator if needed (always at contentY, not affected by pixelOffset)
			if (startLine > 0) {
				String moreText = "↑ Weitere Level (Scrollen)";
				context.drawText(client.textRenderer, moreText, textX, textY, 0x80FFFFFF, true);
				textY += lineHeight;
				linesRendered++;
			}
			
			// Adjust textY for pixel offset after rendering the indicator
			textY -= pixelOffset;
			
			// Minimum Y position for rendering lines (below the indicator if present)
			int minRenderY = startLine > 0 ? contentY + lineHeight : contentY;
			
			// Render visible lines (reserve space for bottom indicator if needed)
			// First, check if there might be more lines below
			boolean mightHaveMoreLines = startLine + (availableHeight / lineHeight) < allLines.size();
			int maxRenderY = mightHaveMoreLines ? maxY - lineHeight : maxY;
			
			int lastProcessedIndex = startLine - 1;
			for (int i = startLine; i < allLines.size() && textY < maxRenderY; i++) {
				// Only render if the line is below the minimum render position
				if (textY >= minRenderY) {
					RenderLine line = allLines.get(i);
					
					if (line.hasSecondPart) {
						// Draw first part
						context.drawText(client.textRenderer, line.text, textX + line.xOffset, textY, line.color, true);
						// Draw second part
						context.drawText(client.textRenderer, line.secondPartText, textX + line.secondPartXOffset, textY, line.secondPartColor, true);
					} else {
						// Draw single line
						context.drawText(client.textRenderer, line.text, textX + line.xOffset, textY, line.color, true);
					}
					linesRendered++;
				}
				
				lastProcessedIndex = i;
				textY += lineHeight;
			}
			
			// Show scroll indicator only if there are actually more lines below
			// Check if we've scrolled to the bottom: if scrollOffset >= maxScrollOffset, we're at the bottom
			boolean hasMoreLines = getMKLevelScrollOffset() < maxScrollOffset;
			if (hasMoreLines && textY < maxY) {
				String moreText = "↓ Weitere Level (Scrollen)";
				context.drawText(client.textRenderer, moreText, textX, textY, 0x80FFFFFF, true);
			}
		} else {
			// Render "Kombinierte Wellen" mode - convert to lines and render line by line
			List<CombinedWaveInfo> filteredCombinedWaves = filterCombinedWaves(mkLevelSearchText);
			
			// Convert all entries to lines
			List<RenderLine> allLines = new ArrayList<>();
			for (CombinedWaveInfo waveInfo : filteredCombinedWaves) {
				// Level header (yellow)
				allLines.add(new RenderLine("-Level (" + waveInfo.level + ")", 0xFFFFFF00));
				
				// Wave (gray prefix + green number)
				String wavePrefix = "-> Welle: ";
				String waveNumber = String.valueOf(waveInfo.wave);
				allLines.add(new RenderLine(wavePrefix, 0xFFC0C0C0, 0, waveNumber, 0xFF55FF55, client.textRenderer.getWidth(wavePrefix)));
				
				// "without" if present
				if (waveInfo.without != null && !waveInfo.without.isEmpty()) {
					String withoutHeader = "Ohne: ";
					int withoutHeaderWidth = client.textRenderer.getWidth(withoutHeader);
					
					// First essence on same line as "Ohne:"
					if (waveInfo.without.size() > 0) {
						String firstEssence = waveInfo.without.get(0).trim();
						Integer essenceWave = findWaveForEssence(firstEssence);
						
						if (essenceWave != null) {
							String wavePrefixText = " -> Welle: ";
							int essenceNameWidth = client.textRenderer.getWidth(firstEssence);
							String waveNumStr = String.valueOf(essenceWave);
							int wavePrefixX = withoutHeaderWidth + essenceNameWidth;
							int waveNumX = wavePrefixX + client.textRenderer.getWidth(wavePrefixText);
							// First part: "Ohne: " + essence name (red)
							// Second part: " -> Welle: " (gray)
							// Third part: wave number (green)
							allLines.add(new RenderLine(withoutHeader + firstEssence, 0xFFFF5555, 0, wavePrefixText, 0xFFC0C0C0, wavePrefixX, waveNumStr, 0xFF55FF55, waveNumX));
						} else {
							allLines.add(new RenderLine(withoutHeader + firstEssence, 0xFFFF5555));
						}
					}
					
					// Remaining essences
					for (int i = 1; i < waveInfo.without.size(); i++) {
						String essenceName = waveInfo.without.get(i).trim();
						Integer essenceWave = findWaveForEssence(essenceName);
						
						if (essenceWave != null) {
							String wavePrefixText = " -> Welle: ";
							int essenceNameWidth = client.textRenderer.getWidth(essenceName);
							String waveNumStr = String.valueOf(essenceWave);
							int wavePrefixX = withoutHeaderWidth + essenceNameWidth;
							int waveNumX = wavePrefixX + client.textRenderer.getWidth(wavePrefixText);
							// First part: essence name (red)
							// Second part: " -> Welle: " (gray)
							// Third part: wave number (green)
							allLines.add(new RenderLine(essenceName, 0xFFFF5555, withoutHeaderWidth, wavePrefixText, 0xFFC0C0C0, wavePrefixX, waveNumStr, 0xFF55FF55, waveNumX));
						} else {
							allLines.add(new RenderLine(essenceName, 0xFFFF5555, withoutHeaderWidth, "", 0, 0));
						}
					}
				}
				
				// Empty line for spacing
				allLines.add(new RenderLine("", 0xFFFFFFFF));
			}
			
			// Calculate total height and max scroll
			int totalHeight = allLines.size() * lineHeight;
			int maxScrollOffset = Math.max(0, totalHeight - availableHeight);
			
			// Limit scroll offset
			int scrollOffset = getMKLevelScrollOffset();
			if (scrollOffset > maxScrollOffset) {
				setMKLevelScrollOffset(maxScrollOffset);
				scrollOffset = maxScrollOffset;
			}
			
			// Calculate start line based on scroll offset
			int startLine = scrollOffset / lineHeight;
			int pixelOffset = scrollOffset % lineHeight;
			
			// Render lines
			int textY = contentY;
			int linesRendered = 0;
			
			// Show scroll indicator if needed (always at contentY, not affected by pixelOffset)
			if (startLine > 0) {
				String moreText = "↑ Weitere Level (Scrollen)";
				context.drawText(client.textRenderer, moreText, textX, textY, 0x80FFFFFF, true);
				textY += lineHeight;
				linesRendered++;
			}
			
			// Adjust textY for pixel offset after rendering the indicator
			textY -= pixelOffset;
			
			// Minimum Y position for rendering lines (below the indicator if present)
			int minRenderY = startLine > 0 ? contentY + lineHeight : contentY;
			
			// Render visible lines (reserve space for bottom indicator if needed)
			// First, check if there might be more lines below
			boolean mightHaveMoreLines = startLine + (availableHeight / lineHeight) < allLines.size();
			int maxRenderY = mightHaveMoreLines ? maxY - lineHeight : maxY;
			
			int lastProcessedIndex = startLine - 1;
			for (int i = startLine; i < allLines.size() && textY < maxRenderY; i++) {
				// Only render if the line is below the minimum render position
				if (textY >= minRenderY) {
					RenderLine line = allLines.get(i);
					
					if (line.hasThirdPart) {
						// Draw first part
						context.drawText(client.textRenderer, line.text, textX + line.xOffset, textY, line.color, true);
						// Draw second part
						if (line.secondPartText != null && !line.secondPartText.isEmpty()) {
							context.drawText(client.textRenderer, line.secondPartText, textX + line.secondPartXOffset, textY, line.secondPartColor, true);
						}
						// Draw third part
						if (line.thirdPartText != null && !line.thirdPartText.isEmpty()) {
							context.drawText(client.textRenderer, line.thirdPartText, textX + line.thirdPartXOffset, textY, line.thirdPartColor, true);
						}
					} else if (line.hasSecondPart) {
						// Draw first part
						context.drawText(client.textRenderer, line.text, textX + line.xOffset, textY, line.color, true);
						// Draw second part
						if (line.secondPartText != null && !line.secondPartText.isEmpty()) {
							context.drawText(client.textRenderer, line.secondPartText, textX + line.secondPartXOffset, textY, line.secondPartColor, true);
						}
					} else {
						// Draw single line
						context.drawText(client.textRenderer, line.text, textX + line.xOffset, textY, line.color, true);
					}
					linesRendered++;
				}
				
				lastProcessedIndex = i;
				textY += lineHeight;
			}
			
			// Show scroll indicator only if there are actually more lines below
			// Check if we've scrolled to the bottom: if scrollOffset >= maxScrollOffset, we're at the bottom
			boolean hasMoreLines = getMKLevelScrollOffset() < maxScrollOffset;
			if (hasMoreLines && textY < maxY) {
				String moreText = "↓ Weitere Level (Scrollen)";
				context.drawText(client.textRenderer, moreText, textX, textY, 0x80FFFFFF, true);
			}
		}
		
		// Draw scrollbar on the right side
		// Scrollbar goes from bottom of overlay to bottom of search bar
		int scrollbarWidth = 6; // Width of scrollbar
		int scrollbarX = unscaledWidth - scrollbarWidth - padding;
		int scrollbarTop = contentY; // Start below search bar
		int scrollbarBottom = unscaledHeight - padding; // End at bottom of overlay
		int scrollbarHeight = scrollbarBottom - scrollbarTop;
		
		// Only show scrollbar if there's content to scroll
		if (scrollbarHeight > 0) {
			// Calculate total content height using the same method as rendering
			// We need to count the actual number of lines that will be rendered
			int totalContentHeight = 0;
			if (mkLevelShowIndividualWaves) {
				List<MKLevelInfo> filteredEntries = filterMKLevelEntries(mkLevelSearchText);
				// Count lines exactly as in rendering: each entry creates lines
				for (MKLevelInfo levelInfo : filteredEntries) {
					totalContentHeight += lineHeight; // Level line
					totalContentHeight += lineHeight; // Essence line
					if (findWaveForEssence(levelInfo.essence) != null) {
						totalContentHeight += lineHeight; // Wave line
					}
					totalContentHeight += lineHeight; // Spacing line
				}
			} else {
				List<CombinedWaveInfo> filteredCombinedWaves = filterCombinedWaves(mkLevelSearchText);
				// Count lines exactly as in rendering - must match allLines.size() calculation
				for (CombinedWaveInfo waveInfo : filteredCombinedWaves) {
					totalContentHeight += lineHeight; // Level line
					totalContentHeight += lineHeight; // Wave line
					if (waveInfo.without != null && !waveInfo.without.isEmpty()) {
						// "Ohne:" header with first essence on same line = 1 line
						totalContentHeight += lineHeight;
						// Remaining essences, each on its own line
						if (waveInfo.without.size() > 1) {
							totalContentHeight += (waveInfo.without.size() - 1) * lineHeight;
						}
					}
					totalContentHeight += lineHeight; // Spacing line
				}
			}
			
			// Only show scrollbar if content is taller than visible area
			if (totalContentHeight > availableHeight) {
				// Draw scrollbar background
				context.fill(scrollbarX, scrollbarTop, scrollbarX + scrollbarWidth, scrollbarBottom, 0xFF404040);
				context.drawBorder(scrollbarX, scrollbarTop, scrollbarWidth, scrollbarHeight, 0xFF808080);
				
				// Calculate handle size and position
				float scrollRatio = (float) availableHeight / totalContentHeight;
				int handleHeight = Math.max(10, (int) (scrollbarHeight * scrollRatio));
				int scrollableTrackHeight = scrollbarHeight - handleHeight;
				
				// Calculate handle position based on scroll offset
				int maxScroll = totalContentHeight - availableHeight;
				float scrollProgress = maxScroll > 0 ? 
					(float) getMKLevelScrollOffset() / maxScroll : 0.0f;
				scrollProgress = Math.max(0.0f, Math.min(1.0f, scrollProgress));
				
				// When scrollProgress is 1.0, handle should be at the bottom
				// handleY = scrollbarTop + scrollableTrackHeight when scrollProgress = 1.0
				// This ensures handleY + handleHeight = scrollbarBottom when at max scroll
				int handleY = scrollbarTop + (int) (scrollableTrackHeight * scrollProgress);
				// Ensure handle doesn't go beyond bounds
				int maxHandleY = scrollbarBottom - handleHeight;
				handleY = Math.max(scrollbarTop, Math.min(handleY, maxHandleY));
				
				// Draw scrollbar handle
				int handleColor = mkLevelScrollbarDragging ? 0xFF808080 : 0xFF606060;
				context.fill(scrollbarX + 1, handleY, scrollbarX + scrollbarWidth - 1, handleY + handleHeight, handleColor);
				context.drawBorder(scrollbarX + 1, handleY, scrollbarWidth - 2, handleHeight, 0xFFC0C0C0);
			}
		}
		
		// Pop matrix transformation
		matrices.popMatrix();
	}
	
	/**
	 * Finds the wave for an essence from MKLevel format (e.g., "Pferd T1") by converting it to Essenz.json format
	 * @param mkLevelEssenceName The essence name from MKLevel.json (e.g., "Pferd T1")
	 * @return The wave number if found, null otherwise
	 */
	private static Integer findWaveForEssence(String mkLevelEssenceName) {
		if (mkLevelEssenceName == null || mkLevelEssenceName.isEmpty()) {
			return null;
		}
		
		// Parse the MKLevel format: "Pferd T1" -> name="Pferd", tier="1"
		String[] parts = mkLevelEssenceName.split("\\s+");
		if (parts.length < 2) {
			return null;
		}
		
		String name = parts[0];
		String tierStr = parts[1].replace("T", "").trim();
		
		// Convert name to plural form (matching Essenz.json format)
		String pluralName = convertToPlural(name);
		
		// Build the Essenz.json format: "Pferde [Essenz] Tier 1"
		String essenceJsonName = pluralName + " [Essenz] Tier " + tierStr;
		
		// Look up in database
		EssenceInfo essenceInfo = essencesDatabase.get(essenceJsonName);
		if (essenceInfo != null) {
			return essenceInfo.wave;
		}
		
		// If not found, try alternative formats (e.g., "Piglin" vs "Pigling")
		// Try with "Pigling" if it was "Piglin"
		if (name.equals("Piglin")) {
			String alternativeName = "Pigling [Essenz] Tier " + tierStr;
			EssenceInfo altInfo = essencesDatabase.get(alternativeName);
			if (altInfo != null) {
				return altInfo.wave;
			}
		}
		// Also try "Piglin" if it was "Pigling" (though this shouldn't happen in MKLevel.json)
		if (name.equals("Pigling")) {
			String alternativeName = "Piglin [Essenz] Tier " + tierStr;
			EssenceInfo altInfo = essencesDatabase.get(alternativeName);
			if (altInfo != null) {
				return altInfo.wave;
			}
		}
		
		return null;
	}
	
	/**
	 * Converts a singular essence name to plural form (matching Essenz.json format)
	 * @param singular The singular name (e.g., "Pferd")
	 * @return The plural form (e.g., "Pferde")
	 */
	private static String convertToPlural(String singular) {
		if (singular == null || singular.isEmpty()) {
			return singular;
		}
		
		// Check if already in plural form (e.g., "Hühner" in MKLevel.json)
		// This handles cases where MKLevel.json already has plural forms
		if (singular.equals("Hühner")) {
			return "Hühner";
		}
		
		// Mapping for special cases
		switch (singular) {
			case "Pferd": return "Pferde";
			case "Lohe": return "Lohen";
			case "Ziege": return "Ziegen";
			case "Biene": return "Bienen";
			case "Huhn": return "Hühner";
			case "Piglin": return "Piglin"; // Can be "Piglin" or "Pigling" in Essenz.json
			case "Schneemann": return "Schneemann"; // Stays the same
			case "Wächter": return "Wächter"; // Stays the same
			case "Fuchs": return "Fuchs"; // Stays the same
			case "Creeper": return "Creeper"; // Stays the same
			case "Frosch": return "Frosch"; // Stays the same
			case "Vindicator": return "Vindicator"; // Stays the same
			case "Hexe": return "Hexen";
			case "Schildkröte": return "Schildkröten";
			case "Spinne": return "Spinnen";
			case "Axolotl": return "Axolotl"; // Stays the same
			case "Enderman": return "Enderman"; // Stays the same
			case "Zoglin": return "Zoglin"; // Stays the same
			case "Panda": return "Panda"; // Stays the same
			case "Evoker": return "Evoker"; // Stays the same
			case "Ertrunkener": return "Ertrunkener"; // Stays the same
			case "Ocelot": return "Ocelot"; // Stays the same
			case "Zombiepferd": return "Zombiepferde";
			case "Husk": return "Husk"; // Stays the same
			case "Wither": return "Wither"; // Stays the same
			case "Skelett": return "Skelett"; // Stays the same
			case "Witherskelett": return "Witherskelett"; // Stays the same
			case "Stray": return "Stray"; // Stays the same
			case "Zombie": return "Zombie"; // Stays the same
			case "Wolf": return "Wolf"; // Stays the same
			case "Endermite": return "Endermite"; // Stays the same
			case "Hoglin": return "Hoglin"; // Stays the same
			default:
				// For names ending in "e", often just add "n"
				if (singular.endsWith("e")) {
					return singular + "n";
				}
				// For other names, try adding "e" or "en"
				return singular + "e";
		}
	}
	
	/**
	 * Filters MKLevel entries based on search text
	 * Searches in Level, Essence, and Amount
	 */
	private static List<MKLevelInfo> filterMKLevelEntries(String searchText) {
		if (searchText == null || searchText.trim().isEmpty()) {
			return mkLevelDatabase;
		}
		
		String searchLower = searchText.toLowerCase().trim();
		List<MKLevelInfo> filtered = new ArrayList<>();
		
		// Check if search text contains "level" followed by a number (e.g., "Level 10", "level10", "level 10", "Level: 10")
		Integer levelFromSearch = null;
		boolean isLevelOnlySearch = false;
		// Pattern matches: "level" (case-insensitive), optional ":", optional whitespace, then digits
		java.util.regex.Pattern levelPattern = java.util.regex.Pattern.compile("level\\s*:?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
		java.util.regex.Matcher levelMatcher = levelPattern.matcher(searchText);
		if (levelMatcher.find()) {
			try {
				levelFromSearch = Integer.parseInt(levelMatcher.group(1));
			} catch (NumberFormatException e) {
				// Ignore
			}
		} else {
			// Check if search text starts with "level" (for live search, e.g., "lev", "leve", "level", "level:")
			if (searchLower.startsWith("level") || searchLower.equals("level:")) {
				isLevelOnlySearch = true;
			}
		}
		
		// Check if search text contains "welle" followed by a number (e.g., "Welle 701", "welle: 701", "welle701")
		Integer waveFromSearch = null;
		boolean isWaveOnlySearch = false;
		// Pattern matches: "welle" (case-insensitive), optional ":", optional whitespace, then digits
		java.util.regex.Pattern wavePattern = java.util.regex.Pattern.compile("welle\\s*:?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
		java.util.regex.Matcher waveMatcher = wavePattern.matcher(searchText);
		if (waveMatcher.find()) {
			try {
				waveFromSearch = Integer.parseInt(waveMatcher.group(1));
			} catch (NumberFormatException e) {
				// Ignore
			}
		} else {
			// Check if search text starts with "welle" (for live search, e.g., "wel", "welle", "welle:")
			if (searchLower.startsWith("welle") || searchLower.equals("welle:")) {
				isWaveOnlySearch = true;
			}
		}
		
		for (MKLevelInfo entry : mkLevelDatabase) {
			boolean matches = false;
			
			// If search text starts with "Level" (for live search), show all entries
			if (isLevelOnlySearch) {
				matches = true;
			}
			// If search text starts with "Welle" (for live search), show all entries
			else if (isWaveOnlySearch) {
				matches = true;
			}
			// Search for "Welle X" pattern - search for waves containing the number as substring
			else if (waveFromSearch != null) {
				Integer entryWave = findWaveForEssence(entry.essence);
				if (entryWave != null) {
					// Check if the wave number contains the searched number as substring
					// e.g., "welle 8" should match "welle 800", "welle 581", etc.
					String waveStr = String.valueOf(entryWave);
					String searchWaveStr = String.valueOf(waveFromSearch);
					if (waveStr.contains(searchWaveStr)) {
						matches = true;
					}
				}
			}
			// Search for "Level X" pattern (prioritize exact level match)
			else if (levelFromSearch != null && entry.level == levelFromSearch) {
				matches = true;
			}
			// General search: check all fields for matches
			else {
				// Search in displayed text "-Level X" - check if search text matches any part of the displayed text
				String displayedLevelText = "-Level " + entry.level;
				if (displayedLevelText.toLowerCase().contains(searchLower)) {
					matches = true;
				}
				
				// Search in Level (as number) - only if not already matched
				if (!matches && String.valueOf(entry.level).contains(searchLower)) {
					matches = true;
				}
				
				// Search in Essence
				if (!matches && entry.essence.toLowerCase().contains(searchLower)) {
					matches = true;
				}
				
				// Search in Amount
				if (!matches) {
					String amountStr = formatNumberWithSeparator(entry.amount);
					if (amountStr.contains(searchLower) || String.valueOf(entry.amount).contains(searchLower)) {
						matches = true;
					}
				}
				
				// Search in Wave (as number) - only if not already matched
				if (!matches) {
					Integer entryWave = findWaveForEssence(entry.essence);
					if (entryWave != null && String.valueOf(entryWave).contains(searchLower)) {
						matches = true;
					}
				}
			}
			
			if (matches) {
				filtered.add(entry);
			}
		}
		
		return filtered;
	}
	
	/**
	 * Filters combined waves entries based on search text
	 * Searches in Level, Wave, and Without fields
	 */
	private static List<CombinedWaveInfo> filterCombinedWaves(String searchText) {
		if (searchText == null || searchText.trim().isEmpty()) {
			return mkLevelCombinedWavesDatabase;
		}
		
		String searchLower = searchText.toLowerCase().trim();
		List<CombinedWaveInfo> filtered = new ArrayList<>();
		
		// Check if search text contains "welle" followed by a number
		Integer waveFromSearch = null;
		boolean isWaveOnlySearch = false;
		java.util.regex.Pattern wavePattern = java.util.regex.Pattern.compile("welle\\s*:?\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
		java.util.regex.Matcher waveMatcher = wavePattern.matcher(searchText);
		if (waveMatcher.find()) {
			try {
				waveFromSearch = Integer.parseInt(waveMatcher.group(1));
			} catch (NumberFormatException e) {
				// Ignore
			}
		} else {
			// Check if search text starts with "welle" (for live search)
			if (searchLower.startsWith("welle") || searchLower.equals("welle:")) {
				isWaveOnlySearch = true;
			}
		}
		
		for (CombinedWaveInfo entry : mkLevelCombinedWavesDatabase) {
			boolean matches = false;
			
			// If search text starts with "Welle" (for live search), show all entries
			if (isWaveOnlySearch) {
				matches = true;
			}
			// Search for "Welle X" pattern - search for waves containing the number as substring
			else if (waveFromSearch != null) {
				String waveStr = String.valueOf(entry.wave);
				String searchWaveStr = String.valueOf(waveFromSearch);
				if (waveStr.contains(searchWaveStr)) {
					matches = true;
				}
			}
			// General search: check all fields for matches
			else {
				// Search in Level (e.g., "1-13", "25+30")
				if (entry.level.toLowerCase().contains(searchLower)) {
					matches = true;
				}
				
				// Search in Wave (as number)
				if (!matches && String.valueOf(entry.wave).contains(searchLower)) {
					matches = true;
				}
				
				// Search in Without
				if (!matches && entry.without != null) {
					for (String withoutItem : entry.without) {
						if (withoutItem.toLowerCase().contains(searchLower)) {
							matches = true;
							break;
						}
					}
				}
			}
			
			if (matches) {
				filtered.add(entry);
			}
		}
		
		return filtered;
	}
	
	/**
	 * Extracts plain text from Text component, removing all formatting codes
	 */
	public static String getPlainTextFromText(Text text) {
		if (text == null) {
			return "";
		}
		
		// Get the string representation (includes formatting codes)
		String textString = text.getString();
		
		// Remove all Minecraft formatting codes (including all possible format codes)
		// § followed by any character (0-9, a-f, k-o, r, x for hex colors, etc.)
		textString = textString.replaceAll("§[0-9a-fk-orxA-FK-ORX]", "");
		
		// Remove hex color codes (format: §#RRGGBB or §x§r§r§g§g§b§b)
		textString = textString.replaceAll("§#[0-9a-fA-F]{6}", "");
		textString = textString.replaceAll("§x(§[0-9a-fA-F]){6}", "");
		
		// Also remove Unicode formatting characters that might interfere
		textString = textString.replaceAll("[\\u3400-\\u4DBF]", "");
		
		// Trim whitespace
		return textString.trim();
	}
	
	/**
	 * Handles mouse click for MKLevel search bar
	 * @param inventoryX X position of the inventory (from mixin shadow field)
	 * @param inventoryY Y position of the inventory (from mixin shadow field)
	 * @param inventoryHeight Height of the inventory (from mixin shadow field)
	 */
	public static boolean handleMKLevelSearchClick(double mouseX, double mouseY, int button, int inventoryX, int inventoryY, int inventoryHeight) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getWindow() == null) {
			return false;
		}
		
		// Check if we're in the MKLevel inventory - check directly instead of relying on isInMKLevelInventory
		// This ensures it works even if onClientTick hasn't run yet
		boolean inMKLevelInventory = false;
		if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen) {
			Text titleText = handledScreen.getTitle();
			String title = getPlainTextFromText(titleText);
			inMKLevelInventory = title.contains("Machtkristalle Verbessern");
		}
		
		if (!inMKLevelInventory) {
			return false;
		}
		
		// Check if MKLevel overlay is enabled
		if (!CCLiveUtilitiesConfig.HANDLER.instance().mkLevelEnabled) {
			return false;
		}
		
		int screenWidth = client.getWindow().getScaledWidth();
		int xPos = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelX;
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale;
		if (scale <= 0) scale = 1.0f;
		
		// Unscaled dimensions (width is fixed, height will be set from inventory)
		int unscaledWidth = 200;
		int overlayWidth = Math.round(unscaledWidth * scale);
		
		// Calculate overlay X position
		// Wenn xPos -1 ist, berechne automatisch rechts (für Kompatibilität)
		int overlayX;
		if (xPos == -1) {
			overlayX = screenWidth - overlayWidth - 10; // 10px Abstand vom rechten Rand
		} else {
			// Verwende die absolute X-Position (obere linke Ecke)
			overlayX = xPos;
		}
		
		// Use inventory position and height passed from mixin (from @Shadow fields)
		int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelY;
		int overlayY = (yOffset == -1) ? inventoryY : yOffset;
		
		// Use the same unscaledHeight as in render (inventoryHeight)
		int unscaledHeight = inventoryHeight;
		
		// Button dimensions (unscaled, same as in render)
		int unscaledButtonHeight = 20;
		int unscaledButtonWidth = unscaledWidth / 2;
		
		// Calculate button positions in screen coordinates
		// Buttons are rendered at negative Y (above overlay), aligned with top edge
		int buttonY = overlayY - Math.round(unscaledButtonHeight * scale);
		int leftButtonX = overlayX;
		int leftButtonWidth = Math.round(unscaledButtonWidth * scale);
		int rightButtonX = overlayX + leftButtonWidth;
		int rightButtonWidth = Math.round(unscaledButtonWidth * scale);
		int buttonHeight = Math.round(unscaledButtonHeight * scale);
		
		// Check if click is on left button ("Einzelne Wellen")
		if (mouseX >= leftButtonX && mouseX <= leftButtonX + leftButtonWidth &&
			mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
			if (button == 0) { // Left click
				mkLevelShowIndividualWaves = true;
				// Keep scroll offset when switching modes
				return true;
			}
		}
		
		// Check if click is on right button ("Kombinierte Wellen")
		if (mouseX >= rightButtonX && mouseX <= rightButtonX + rightButtonWidth &&
			mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
			if (button == 0) { // Left click
				mkLevelShowIndividualWaves = false;
				// Keep scroll offset when switching modes
				return true;
			}
		}
		
		// Calculate search bar position in screen coordinates (same as SearchBarUtility)
		// In render: searchBarX = padding (5), searchBarY = padding - contentOffset (5 - 3 = 2)
		// These are unscaled coordinates within the matrix, so we need to scale them
		int unscaledPadding = 5;
		int unscaledSearchBarHeight = 16;
		int unscaledContentOffset = 3; // Shift content 3px up (same as in render)
		int unscaledSearchBarWidth = unscaledWidth - unscaledPadding * 2;
		
		// Calculate absolute screen coordinates of search bar
		// The search bar is rendered at (padding, padding - contentOffset) within the matrix
		// After transformation: (overlayX + padding * scale, overlayY + (padding - contentOffset) * scale)
		int searchBarX = overlayX + Math.round(unscaledPadding * scale);
		int searchBarY = overlayY + Math.round((unscaledPadding - unscaledContentOffset) * scale);
		int searchBarWidth = Math.round(unscaledSearchBarWidth * scale);
		int searchBarHeight = Math.round(unscaledSearchBarHeight * scale);
		
		// Check if click is on search bar (in screen coordinates)
		if (mouseX >= searchBarX && mouseX <= searchBarX + searchBarWidth &&
			mouseY >= searchBarY && mouseY <= searchBarY + searchBarHeight) {
			
			if (button == 0) { // Left click
				mkLevelSearchFocused = true;
				// Set cursor position based on click (in screen coordinates, then convert to text position)
				String textBeforeClick = mkLevelSearchText;
				// Calculate click position relative to search bar, then scale down for text measurement
				int clickX = (int) ((mouseX - searchBarX - 2) / scale);
				mkLevelSearchCursorPosition = findCursorPosition(client, textBeforeClick, clickX);
				return true;
			} else if (button == 1) { // Right click - clear search
				mkLevelSearchText = "";
				mkLevelSearchCursorPosition = 0;
				setMKLevelScrollOffset(0);
				return true;
			}
		} else {
			// Click outside search bar - unfocus
			mkLevelSearchFocused = false;
		}
		
		// Check if click is on scrollbar
		int scrollbarWidth = 6;
		int scrollbarX = overlayX + Math.round((unscaledWidth - scrollbarWidth - unscaledPadding) * scale);
		int scrollbarTop = overlayY + Math.round((unscaledPadding - unscaledContentOffset + unscaledSearchBarHeight + 2) * scale);
		int scrollbarBottom = overlayY + Math.round((unscaledHeight - unscaledPadding) * scale);
		int scrollbarHeight = scrollbarBottom - scrollbarTop;
		
		if (mouseX >= scrollbarX && mouseX <= scrollbarX + Math.round(scrollbarWidth * scale) &&
			mouseY >= scrollbarTop && mouseY <= scrollbarBottom) {
			
			if (button == 0) { // Left click
				// Calculate total content height
				int lineHeight = 12;
				int totalContentHeight = 0;
				int availableHeight = unscaledHeight - unscaledPadding * 2 - unscaledSearchBarHeight - 2 - unscaledContentOffset;
				
				if (mkLevelShowIndividualWaves) {
					List<MKLevelInfo> filteredEntries = filterMKLevelEntries(mkLevelSearchText);
					for (MKLevelInfo levelInfo : filteredEntries) {
						totalContentHeight += 2 * lineHeight;
						if (findWaveForEssence(levelInfo.essence) != null) {
							totalContentHeight += lineHeight;
						}
						totalContentHeight += lineHeight;
					}
				} else {
					List<CombinedWaveInfo> filteredCombinedWaves = filterCombinedWaves(mkLevelSearchText);
					for (CombinedWaveInfo waveInfo : filteredCombinedWaves) {
						totalContentHeight += 2 * lineHeight;
						if (waveInfo.without != null && !waveInfo.without.isEmpty()) {
							totalContentHeight += lineHeight;
							totalContentHeight += waveInfo.without.size() * lineHeight;
						}
						totalContentHeight += lineHeight;
					}
				}
				
				if (totalContentHeight > availableHeight) {
					// Calculate handle size and position
					float scrollRatio = (float) availableHeight / totalContentHeight;
					int handleHeight = Math.max(10, (int) (scrollbarHeight * scrollRatio));
					
					// Calculate current handle position
					float scrollProgress = (float) getMKLevelScrollOffset() / (totalContentHeight - availableHeight);
					scrollProgress = Math.max(0.0f, Math.min(1.0f, scrollProgress));
					int handleY = scrollbarTop + (int) ((scrollbarHeight - handleHeight) * scrollProgress);
					
					// Check if click is on handle
					if (mouseY >= handleY && mouseY <= handleY + Math.round(handleHeight * scale)) {
						// Start dragging
						mkLevelScrollbarDragging = true;
						mkLevelScrollbarDragStartY = mouseY;
						mkLevelScrollbarDragStartOffset = getMKLevelScrollOffset();
						return true;
					} else {
						// Click on scrollbar track - jump to position
						float clickProgress = (float) (mouseY - scrollbarTop) / scrollbarHeight;
						clickProgress = Math.max(0.0f, Math.min(1.0f, clickProgress));
						int newOffset = (int) (clickProgress * (totalContentHeight - availableHeight));
						setMKLevelScrollOffset(Math.max(0, Math.min(newOffset, totalContentHeight - availableHeight)));
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Handles mouse drag for MKLevel scrollbar
	 */
	public static boolean handleMKLevelScrollbarDrag(double mouseX, double mouseY, int button, int inventoryX, int inventoryY, int inventoryHeight) {
		if (!mkLevelScrollbarDragging || button != 0) {
			return false;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.getWindow() == null) {
			return false;
		}
		
		int screenWidth = client.getWindow().getScaledWidth();
		int xPos = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelX;
		int yOffset = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelY;
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().mkLevelScale;
		if (scale <= 0) scale = 1.0f;
		
		int unscaledWidth = 200;
		int unscaledHeight = inventoryHeight;
		int overlayWidth = Math.round(unscaledWidth * scale);
		
		int overlayX;
		if (xPos == -1) {
			overlayX = screenWidth - overlayWidth - 10;
		} else {
			overlayX = xPos;
		}
		
		int overlayY = (yOffset == -1) ? inventoryY : yOffset;
		
		int padding = 5;
		int lineHeight = 12;
		int searchBarHeight = 16;
		int contentOffset = 3;
		
		// Calculate scrollbar position
		int scrollbarWidth = 6;
		int scrollbarX = overlayX + Math.round((unscaledWidth - scrollbarWidth - padding) * scale);
		int scrollbarTop = overlayY + Math.round((padding - contentOffset + searchBarHeight + 2) * scale);
		int scrollbarBottom = overlayY + Math.round((unscaledHeight - padding) * scale);
		int scrollbarHeight = scrollbarBottom - scrollbarTop;
		
		// Calculate total content height
		int availableHeight = unscaledHeight - padding * 2 - searchBarHeight - 2 - contentOffset;
		int totalContentHeight = 0;
		
		if (mkLevelShowIndividualWaves) {
			List<MKLevelInfo> filteredEntries = filterMKLevelEntries(mkLevelSearchText);
			for (MKLevelInfo levelInfo : filteredEntries) {
				totalContentHeight += 2 * lineHeight;
				if (findWaveForEssence(levelInfo.essence) != null) {
					totalContentHeight += lineHeight;
				}
				totalContentHeight += lineHeight;
			}
		} else {
			List<CombinedWaveInfo> filteredCombinedWaves = filterCombinedWaves(mkLevelSearchText);
			for (CombinedWaveInfo waveInfo : filteredCombinedWaves) {
				totalContentHeight += 2 * lineHeight;
				if (waveInfo.without != null && !waveInfo.without.isEmpty()) {
					totalContentHeight += lineHeight;
					totalContentHeight += waveInfo.without.size() * lineHeight;
				}
				totalContentHeight += lineHeight;
			}
		}
		
		if (totalContentHeight <= availableHeight) {
			return false;
		}
		
		// Calculate new scroll position based on mouse Y position
		float scrollRatio = (float) availableHeight / totalContentHeight;
		int handleHeight = Math.max(10, (int) (scrollbarHeight * scrollRatio));
		int scrollableHeight = scrollbarHeight - handleHeight;
		
		// Calculate progress based on mouse position
		float progress = (float) (mouseY - scrollbarTop - handleHeight / 2) / scrollableHeight;
		progress = Math.max(0.0f, Math.min(1.0f, progress));
		
		// Calculate new scroll offset
		int maxScroll = totalContentHeight - availableHeight;
		int newOffset = (int) (progress * maxScroll);
		setMKLevelScrollOffset(Math.max(0, Math.min(newOffset, maxScroll)));
		
		return true;
	}
	
	/**
	 * Handles mouse release for MKLevel scrollbar
	 */
	public static boolean handleMKLevelScrollbarRelease(double mouseX, double mouseY, int button) {
		if (mkLevelScrollbarDragging && button == 0) {
			mkLevelScrollbarDragging = false;
			return true;
		}
		return false;
	}
	
	/**
	 * Finds cursor position based on click X position
	 */
	private static int findCursorPosition(MinecraftClient client, String text, int clickX) {
		if (text.isEmpty()) {
			return 0;
		}
		
		for (int i = 0; i <= text.length(); i++) {
			String substring = text.substring(0, i);
			int width = client.textRenderer.getWidth(substring);
			if (width >= clickX) {
				// Check if we're closer to this position or the previous one
				if (i > 0) {
					int prevWidth = client.textRenderer.getWidth(text.substring(0, i - 1));
					if (clickX - prevWidth < width - clickX) {
						return i - 1;
					}
				}
				return i;
			}
		}
		
		return text.length();
	}
	
	/**
	 * Handles character input for MKLevel search bar
	 */
	public static boolean handleMKLevelCharTyped(char chr, int modifiers) {
		if (!isInMKLevelInventory || !mkLevelSearchFocused) {
			return false;
		}
		
		// Handle backspace
		if (chr == '\b') {
			if (!mkLevelSearchText.isEmpty() && mkLevelSearchCursorPosition > 0) {
				mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition - 1) +
									mkLevelSearchText.substring(mkLevelSearchCursorPosition);
				mkLevelSearchCursorPosition--;
				setMKLevelScrollOffset(0); // Reset scroll when search changes
			}
			return true;
		}
		
		// Handle delete
		if (chr == 127) {
			if (mkLevelSearchCursorPosition < mkLevelSearchText.length()) {
				mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition) +
									mkLevelSearchText.substring(mkLevelSearchCursorPosition + 1);
				setMKLevelScrollOffset(0); // Reset scroll when search changes
			}
			return true;
		}
		
		// Handle printable characters
		if (chr >= 32 && chr != 127) {
			mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition) +
								chr +
								mkLevelSearchText.substring(mkLevelSearchCursorPosition);
			mkLevelSearchCursorPosition++;
			setMKLevelScrollOffset(0); // Reset scroll when search changes
			return true;
		}
		
		return false;
	}
	
	/**
	 * Handles key press for MKLevel search bar
	 * Based on SearchBarUtility.handleKeyPress
	 */
	public static boolean handleMKLevelKeyPressed(int keyCode, int scanCode, int modifiers) {
		// Check if search bar is focused - if it is, we're definitely in the MKLevel inventory
		if (!mkLevelSearchFocused) {
			return false;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return false;
		}
		
		// Double-check that we're still in the MKLevel inventory
		boolean inMKLevelInventory = false;
		if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen) {
			Text titleText = handledScreen.getTitle();
			String title = getPlainTextFromText(titleText);
			inMKLevelInventory = title.contains("Machtkristalle Verbessern");
		}
		
		if (!inMKLevelInventory) {
			// Unfocus if we're no longer in the inventory
			mkLevelSearchFocused = false;
			return false;
		}
		
		// Cursor sichtbar machen bei Tastendruck
		mkLevelSearchCursorVisible = true;
		mkLevelSearchCursorBlinkTime = System.currentTimeMillis();
		
		// ESC-Taste
		if (keyCode == 256) {
			mkLevelSearchFocused = false;
			return true;
		}
		
		// Backspace-Taste
		if (keyCode == 259) {
			// Strg+Backspace: Lösche alles
			if (modifiers == 2) { // 2 = Strg
				mkLevelSearchText = "";
				mkLevelSearchCursorPosition = 0;
				setMKLevelScrollOffset(0); // Reset scroll when search changes
			} else if (!mkLevelSearchText.isEmpty() && mkLevelSearchCursorPosition > 0) {
				// Normaler Backspace: Lösche ein Zeichen
				mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition - 1) +
									mkLevelSearchText.substring(mkLevelSearchCursorPosition);
				mkLevelSearchCursorPosition--;
				setMKLevelScrollOffset(0); // Reset scroll when search changes
			}
			return true;
		}
		
		// Delete-Taste
		if (keyCode == 261) {
			if (mkLevelSearchCursorPosition < mkLevelSearchText.length()) {
				mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition) +
									mkLevelSearchText.substring(mkLevelSearchCursorPosition + 1);
				setMKLevelScrollOffset(0); // Reset scroll when search changes
			}
			return true;
		}
		
		// Pfeiltasten
		if (keyCode == 263) { // Left arrow
			if (mkLevelSearchCursorPosition > 0) {
				mkLevelSearchCursorPosition--;
			}
			return true;
		}
		
		if (keyCode == 262) { // Right arrow
			if (mkLevelSearchCursorPosition < mkLevelSearchText.length()) {
				mkLevelSearchCursorPosition++;
			}
			return true;
		}
		
		// Handle home/end
		if (keyCode == 268) { // Home
			mkLevelSearchCursorPosition = 0;
			return true;
		}
		
		if (keyCode == 269) { // End
			mkLevelSearchCursorPosition = mkLevelSearchText.length();
			return true;
		}
		
		// Strg-Funktionen
		if (modifiers == 2) { // 2 = Strg
			switch (keyCode) {
				case 67: // Strg+C - Kopieren
					if (!mkLevelSearchText.isEmpty()) {
						client.keyboard.setClipboard(mkLevelSearchText);
					}
					return true;
				case 86: // Strg+V - Einfügen
					String clipboardText = client.keyboard.getClipboard();
					if (clipboardText != null && !clipboardText.isEmpty()) {
						mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition) +
											clipboardText +
											mkLevelSearchText.substring(mkLevelSearchCursorPosition);
						mkLevelSearchCursorPosition += clipboardText.length();
						setMKLevelScrollOffset(0); // Reset scroll when search changes
					}
					return true;
				case 65: // Strg+A - Alles markieren (nicht nötig, aber für Konsistenz)
					return true;
			}
		}
		
		// Zeicheneingabe (basierend auf SearchBarUtility)
		// Leertaste
		if (keyCode == 32) {
			insertMKLevelCharacter(' ');
			return true;
		}
		
		// Einfache Sonderzeichen
		if (keyCode == 188) {
			insertMKLevelCharacter(',');
			return true;
		}
		
		if (keyCode == 190) {
			insertMKLevelCharacter('.');
			return true;
		}
		
		// Alternative KeyCodes für QWERTZ-Layout
		if (keyCode == 44) {
			insertMKLevelCharacter(',');
			return true;
		}
		
		if (keyCode == 46) {
			insertMKLevelCharacter('.');
			return true;
		}
		
		if (keyCode == 189) {
			insertMKLevelCharacter('-');
			return true;
		}
		
		if (keyCode == 187) {
			insertMKLevelCharacter('+');
			return true;
		}
		
		if (keyCode == 186) {
			insertMKLevelCharacter(';');
			return true;
		}
		
		if (keyCode == 222) {
			insertMKLevelCharacter('"');
			return true;
		}
		
		if (keyCode == 192) {
			insertMKLevelCharacter('`');
			return true;
		}
		
		// AltGr-Kombinationen
		boolean handled = false;
		if (modifiers == 6) {
			switch (keyCode) {
				case 56:
					insertMKLevelCharacter('[');
					handled = true;
					break;
				case 57:
					insertMKLevelCharacter(']');
					handled = true;
					break;
				case 55:
					insertMKLevelCharacter('{');
					handled = true;
					break;
				case 48:
					insertMKLevelCharacter('}');
					handled = true;
					break;
				case 81:
					insertMKLevelCharacter('@');
					handled = true;
					break;
			}
		}
		
		// Shift-Kombinationen
		if (modifiers == 1) {
			switch (keyCode) {
				case 55:
					insertMKLevelCharacter('/');
					handled = true;
					break;
				case 56:
					insertMKLevelCharacter('(');
					handled = true;
					break;
				case 57:
					insertMKLevelCharacter(')');
					handled = true;
					break;
				case 48:
					insertMKLevelCharacter('=');
					handled = true;
					break;
				case 220:
					insertMKLevelCharacter('|');
					handled = true;
					break;
			}
		}
		
		if (keyCode == 53 && modifiers == 1) {
			insertMKLevelCharacter('%');
			return true;
		}
		
		// Separate Sonderzeichen-Tasten
		if (keyCode == 92) {
			insertMKLevelCharacter('#');
			return true;
		}
		
		if (keyCode == 93) {
			insertMKLevelCharacter('+');
			return true;
		}
		
		if (keyCode == 47) {
			insertMKLevelCharacter('-');
			return true;
		}
		
		if (keyCode == 162 && modifiers == 0) {
			insertMKLevelCharacter('<');
			return true;
		}
		
		if (keyCode == 162 && modifiers == 1) {
			insertMKLevelCharacter('>');
			return true;
		}
		
		// Separate ß-Taste
		if (keyCode == 45) {
			insertMKLevelCharacter('ß');
			return true;
		}
		
		// Separate Umlaut-Tasten
		if (keyCode == 39) {
			insertMKLevelCharacter('ä');
			return true;
		}
		
		if (keyCode == 59) {
			insertMKLevelCharacter('ö');
			return true;
		}
		
		if (keyCode == 91) {
			insertMKLevelCharacter('ü');
			return true;
		}
		
		// Numpad-Tasten
		if (keyCode == 334) {
			insertMKLevelCharacter('+');
			return true;
		}
		
		if (keyCode == 333) {
			insertMKLevelCharacter('-');
			return true;
		}
		
		// Numpad-Zahlen 0-9
		if (keyCode == 320) {
			insertMKLevelCharacter('0');
			return true;
		}
		
		if (keyCode >= 321 && keyCode <= 329) {
			char number = (char) ('0' + (keyCode - 321 + 1));
			insertMKLevelCharacter(number);
			return true;
		}
		
		if (handled) {
			return true;
		}
		
		// Zahlen 0-9
		if (modifiers == 0 && keyCode >= 48 && keyCode <= 57) {
			char number = (char) keyCode;
			insertMKLevelCharacter(number);
			return true;
		}
		
		// Buchstaben A-Z (QWERTZ Layout)
		if (keyCode >= 65 && keyCode <= 90) {
			char letter;
			
			// QWERTZ Layout: Y und Z tauschen
			if (keyCode == 89) {
				letter = modifiers == 1 ? 'Z' : 'z';
			} else if (keyCode == 90) {
				letter = modifiers == 1 ? 'Y' : 'y';
			} else {
				// Normale Buchstaben: Großbuchstaben bei Shift, Kleinbuchstaben ohne Shift
				letter = modifiers == 1 ? (char) keyCode : (char) (keyCode + 32);
			}
			
			insertMKLevelCharacter(letter);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Inserts a character at the cursor position
	 */
	private static void insertMKLevelCharacter(char character) {
		mkLevelSearchText = mkLevelSearchText.substring(0, mkLevelSearchCursorPosition) +
							character +
							mkLevelSearchText.substring(mkLevelSearchCursorPosition);
		mkLevelSearchCursorPosition++;
		setMKLevelScrollOffset(0); // Reset scroll when search changes
	}
	
	/**
	 * Loads the collections database from Collections.json
	 */
	private static void loadCollectionsDatabase() {
		try {
			// Load from mod resources
			var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
				.orElseThrow(() -> new RuntimeException("Mod container not found"))
				.findPath(COLLECTIONS_CONFIG_FILE)
				.orElseThrow(() -> new RuntimeException("Collections config file not found"));
			
			try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
				try (var reader = new java.io.InputStreamReader(inputStream)) {
					JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
					JsonObject collections = json.getAsJsonObject("collections");
					
					for (String collectionKey : collections.keySet()) {
						JsonObject collectionData = collections.getAsJsonObject(collectionKey);
						int collectionNumber = Integer.parseInt(collectionKey.replace("collection_", ""));
						int amount = collectionData.get("amount").getAsInt();
						
						collectionsDatabase.put(collectionNumber, amount);
					}
				}
			}
		} catch (Exception e) {
			// Silent error handling("Failed to load collections database: " + e.getMessage());
			// Silent error handling
		}
	}
	
	/**
	 * Process bossbar collection from mixin
	 * This method is called by BossBarMixin when a bossbar with collection information is found
	 */
	public static void processBossBarCollection(String bossBarName) {
		try {
			if (!isTrackingCollections) {
				return;
			}
			
			int blocks = decodeChineseNumber(bossBarName);
			
			if (blocks >= 0) {
				if (firstBossBarUpdate) {
					initialBlocks = blocks;
					currentBlocks = blocks;
					sessionBlocks = 0;
					firstBossBarUpdate = false;
					updateNextCollection();
				} else if (blocks > currentBlocks) {
					currentBlocks = blocks;
					sessionBlocks = currentBlocks - initialBlocks;
					updateNextCollection();
				} else if (blocks < currentBlocks) {
					// Blocks decreased (dimension change or reset)
					initialBlocks = blocks;
					currentBlocks = blocks;
					sessionBlocks = 0;
					updateNextCollection();
				} else {
					currentBlocks = blocks;
					sessionBlocks = currentBlocks - initialBlocks;
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	/**
	 * Decodes a Chinese number from text using aincraft_bottom_font
	 */
	private static int decodeChineseNumber(String text) {
		try {
			Map<Character, Integer> chineseNumbers = ZeichenUtility.getAincraftBottomFontNumbers();
			StringBuilder numberStr = new StringBuilder();
			
			for (char c : text.toCharArray()) {
				Integer digit = chineseNumbers.get(c);
				if (digit != null) {
					numberStr.append(digit);
				}
			}
			
			if (numberStr.length() > 0) {
				return Integer.parseInt(numberStr.toString());
			}
		} catch (Exception e) {
			// Silent error handling
		}
		return -1;
	}
	
	/**
	 * Updates the next collection information based on current blocks
	 */
	private static void updateNextCollection() {
		if (collectionsDatabase.isEmpty()) {
			nextCollectionNumber = 1;
			blocksNeededForNextCollection = 0;
			return;
		}
		
		// Find the next collection we haven't reached yet
		for (int i = 1; i <= 10; i++) {
			Integer requiredBlocks = collectionsDatabase.get(i);
			if (requiredBlocks != null && currentBlocks < requiredBlocks) {
				nextCollectionNumber = i;
				blocksNeededForNextCollection = requiredBlocks - currentBlocks;
				return;
			}
		}
		
		// All collections reached
		nextCollectionNumber = 11; // Beyond max
		blocksNeededForNextCollection = 0;
	}
	
	/**
	 * Checks for dimension changes for mining/lumberjack overlays and resets tracking if necessary
	 */
	private static void checkMiningLumberjackDimensionChange(MinecraftClient client) {
		try {
			if (client.world != null && client.player != null) {
				String newDimension = client.world.getRegistryKey().getValue().toString();
				
				if (miningLumberjackDimension != null && !miningLumberjackDimension.equals(newDimension)) {
					// Dimension changed, reset mining/lumberjack tracking
					resetMiningLumberjackTracking();
				}
				
				miningLumberjackDimension = newDimension;
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	/**
	 * Resets mining/lumberjack tracking (timer and XP data)
	 */
	private static void resetMiningLumberjackTracking() {
		// Reset mining XP tracking
		miningXP.sessionStartTime = 0;
		miningXP.newXP = 0;
		miningXP.xpPerMinute = 0.0;
		miningXP.isTracking = false;
		miningXP.initialXP = -1;
		miningXP.shouldShowOverlay = false;
		miningXP.lastXPChangeTime = 0;
		miningXP.isInitializedInCurrentDimension = false;
		
		// Reset lumberjack XP tracking
		lumberjackXP.sessionStartTime = 0;
		lumberjackXP.newXP = 0;
		lumberjackXP.xpPerMinute = 0.0;
		lumberjackXP.isTracking = false;
		lumberjackXP.initialXP = -1;
		lumberjackXP.shouldShowOverlay = false;
		lumberjackXP.lastXPChangeTime = 0;
		lumberjackXP.isInitializedInCurrentDimension = false;
	}
	
	/**
	 * Checks if player is in farmworld dimension
	 */
	private static boolean isInFarmworldDimension(MinecraftClient client) {
		try {
			if (client == null || client.world == null) {
				return false;
			}
			
			String dimensionId = client.world.getRegistryKey().getValue().toString().toLowerCase();
			// Farmworld is usually minecraft:overworld
			return dimensionId.equals("minecraft:overworld");
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Checks for dimension changes and resets collection tracking if necessary
	 */
	private static void checkCollectionDimensionChange(MinecraftClient client) {
		try {
			if (client.world != null && client.player != null) {
				String newDimension = client.world.getRegistryKey().getValue().toString();
				
				if (collectionDimension != null && !collectionDimension.equals(newDimension)) {
					// Dimension changed, reset collection tracking
					resetCollectionTracking();
					// Reset biom detection
					biomDetected = false;
					currentBiomName = null;
					// Stop timer
					sessionStartTime = 0;
					// Check if we're in farmworld and if biom is detected
					if (isInFarmworldDimension(client)) {
						checkCollectionBiomChange(client);
						// Start timer if overlay is enabled and biom is detected
						boolean currentShowCollectionOverlay = CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay;
						if (currentShowCollectionOverlay && biomDetected) {
							sessionStartTime = System.currentTimeMillis();
						}
					}
				}
				
				collectionDimension = newDimension;
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	/**
	 * Resets collection tracking
	 */
	private static void resetCollectionTracking() {
		initialBlocks = -1;
		currentBlocks = 0;
		sessionBlocks = 0;
		firstBossBarUpdate = true;
		blocksPerMinute = 0.0;
		// Don't reset sessionStartTime here - it's set separately when starting tracking
		collectionDimension = null;
		nextCollectionNumber = 1;
		blocksNeededForNextCollection = 0;
		// Don't reset currentBiomName here - it should persist across resets
	}
	
	/**
	 * Checks for biom changes in scoreboard and resets collection tracking if necessary
	 */
	private static void checkCollectionBiomChange(MinecraftClient client) {
		try {
			// Only check every 500ms (0.5 seconds) to avoid performance issues
			long currentTime = System.currentTimeMillis();
			long timeSinceLastCheck = currentTime - lastScoreboardCheck;
			
			if (timeSinceLastCheck < 500) {
				return;
			}
			lastScoreboardCheck = currentTime;
			
			if (client == null || client.world == null) {
				// Scoreboard disappeared - hide overlay temporarily
				if (biomDetected) {
					biomDetected = false;
					sessionStartTime = 0;
				}
				return;
			}
			
			// Get scoreboard
			net.minecraft.scoreboard.Scoreboard scoreboard = client.world.getScoreboard();
			if (scoreboard == null) {
				// Scoreboard disappeared - hide overlay temporarily
				if (biomDetected) {
					biomDetected = false;
					sessionStartTime = 0;
				}
				return;
			}
			
			// Get sidebar objective
			net.minecraft.scoreboard.ScoreboardObjective sidebarObjective = 
				scoreboard.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR);
			if (sidebarObjective == null) {
				// Scoreboard disappeared - hide overlay temporarily
				if (biomDetected) {
					biomDetected = false;
					sessionStartTime = 0;
				}
				return;
			}
			
			// Check if title is "Cactus Clicker"
			String titleStr = sidebarObjective.getDisplayName().getString();
			String cleanTitle = titleStr.replaceAll("§[0-9a-fk-or]", "").trim();
			
			if (!cleanTitle.equalsIgnoreCase("Cactus Clicker")) {
				// Scoreboard disappeared (wrong scoreboard) - hide overlay temporarily
				if (biomDetected) {
					biomDetected = false;
					sessionStartTime = 0;
				}
				return; // Not the right scoreboard
			}
			
			// Read all scoreboard lines
			List<String> lines = readScoreboardLines(scoreboard, sidebarObjective);
			
			// Extract biom and ressource
			String biomName = extractBiomFromLines(lines);
			String ressourceName = extractRessourceFromLines(lines);
			
			// Check if biom changed
			if (biomName != null) {
				boolean wasDetected = biomDetected;
				biomDetected = true; // Biom was detected
				if (currentBiomName != null && !currentBiomName.equals(biomName)) {
					// Biom changed, schedule multiple resets in quick succession
					pendingResets = 5; // Reset 5 times
					currentBiomName = biomName;
				} else if (currentBiomName == null) {
					// First time setting biom name, just set it without resetting
					currentBiomName = biomName;
				}
				// If biom was just detected (wasn't detected before), reactivate overlay if it was enabled
				if (!wasDetected && CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay) {
					// Overlay will automatically show because biomDetected is now true
					// Start timer if we're tracking
					if (isTrackingCollections && sessionStartTime == 0) {
						sessionStartTime = System.currentTimeMillis();
					}
				}
			} else {
				// No biom detected - hide overlay temporarily
				if (biomDetected) {
					biomDetected = false;
					sessionStartTime = 0;
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
	}
	
	/**
	 * Reads all scoreboard lines from sidebar in order (top to bottom)
	 * Uses the official 1.21.x API: getScoreboardEntries(ScoreboardObjective)
	 * No Reflection - uses direct Minecraft classes
	 */
	private static List<String> readScoreboardLines(net.minecraft.scoreboard.Scoreboard scoreboard, 
			net.minecraft.scoreboard.ScoreboardObjective sidebarObjective) {
		List<String> lines = new ArrayList<>();
		
		try {
			// Helper method to remove Minecraft formatting codes (§ codes)
			java.util.function.Function<String, String> removeFormatting = (text) -> {
				if (text == null) return "";
				return text.replaceAll("§[0-9a-fk-or]", "").trim();
			};
			
			// Use official 1.21.x API: getScoreboardEntries(ScoreboardObjective)
			Collection<ScoreboardEntry> rawEntries = scoreboard.getScoreboardEntries(sidebarObjective);
			
			if (rawEntries == null || rawEntries.isEmpty()) {
				return lines;
			}
			
			// Filter hidden entries and sort like in HUD
			List<ScoreboardEntry> filteredEntries = rawEntries.stream()
					.filter(e -> !e.hidden())
					.toList();
			
			// Try to sort with InGameHud.SCOREBOARD_ENTRY_COMPARATOR (via reflection if needed)
			List<ScoreboardEntry> entries;
			try {
				java.lang.reflect.Field comparatorField = InGameHud.class.getField("SCOREBOARD_ENTRY_COMPARATOR");
				@SuppressWarnings("unchecked")
				java.util.Comparator<ScoreboardEntry> comparator = (java.util.Comparator<ScoreboardEntry>) comparatorField.get(null);
				entries = filteredEntries.stream()
						.sorted(comparator)
						.toList();
			} catch (Exception e) {
				// Fallback: sort by score value (descending)
				entries = filteredEntries.stream()
						.sorted((a, b) -> Integer.compare(b.value(), a.value()))
						.toList();
			}
			
			// Extract text from each entry
			for (int i = 0; i < entries.size(); i++) {
				ScoreboardEntry entry = entries.get(i);
				
				String owner = entry.owner();
				
				// Get the team for this owner using getScoreHolderTeam
				Team team = null;
				try {
					java.lang.reflect.Method getScoreHolderTeamMethod = scoreboard.getClass().getMethod("getScoreHolderTeam", String.class);
					team = (Team) getScoreHolderTeamMethod.invoke(scoreboard, owner);
				} catch (Exception e) {
					// Try alternative method names
					try {
						java.lang.reflect.Method method = scoreboard.getClass().getMethod("method_1164", String.class);
						team = (Team) method.invoke(scoreboard, owner);
					} catch (Exception e2) {
						// Ignore
					}
				}
				
				// WICHTIG: sichtbarer Text kommt aus entry.name()
				// Aber wenn entry.name() nur den Owner zurückgibt, müssen wir Team.decorateName() manuell verwenden
				Text lineText = entry.name();
				String nameString = lineText != null ? lineText.getString() : "";
				
				// Prüfe ob entry.name() nur den Owner zurückgibt (dann ist Team.decorateName() nötig)
				if (nameString.equals(owner)) {
					// entry.name() hat nur den Owner zurückgegeben - versuche Teams zu finden
					
					// Versuche alle Teams im Scoreboard zu durchsuchen
					try {
						java.lang.reflect.Field[] fields = scoreboard.getClass().getDeclaredFields();
						for (java.lang.reflect.Field field : fields) {
							if (java.util.Map.class.isAssignableFrom(field.getType())) {
								field.setAccessible(true);
								Object fieldValue = field.get(scoreboard);
								if (fieldValue instanceof java.util.Map) {
									@SuppressWarnings("unchecked")
									java.util.Map<?, ?> map = (java.util.Map<?, ?>) fieldValue;
									// Prüfe ob dies die Teams-Map ist (Map<String, Team>)
									if (!map.isEmpty()) {
										Object firstKey = map.keySet().iterator().next();
										Object firstValue = map.get(firstKey);
										if (firstKey instanceof String && firstValue instanceof Team) {
											@SuppressWarnings("unchecked")
											java.util.Map<String, Team> teamsMap = (java.util.Map<String, Team>) map;
											
											// Suche nach einem Team, das diesen Owner enthält
											for (java.util.Map.Entry<String, Team> teamEntry : teamsMap.entrySet()) {
												Team t = teamEntry.getValue();
												if (t != null) {
													// Prüfe ob dieses Team den Owner als Mitglied hat
													try {
														java.lang.reflect.Method getMembersMethod = t.getClass().getMethod("getMembers");
														java.util.Collection<?> members = (java.util.Collection<?>) getMembersMethod.invoke(t);
														if (members != null && members.contains(owner)) {
															team = t;
															break;
														}
													} catch (Exception ex) {
														// Ignoriere Fehler
													}
												}
											}
											
											// Wenn kein Team über Members gefunden wurde, versuche über Team-Name zu suchen
											if (team == null) {
												// Versuche Team-Name, der dem Owner entspricht (z.B. Team "§e" für Owner "§e")
												Team possibleTeam = teamsMap.get(owner);
												if (possibleTeam != null) {
													team = possibleTeam;
												}
											}
											
											if (team != null) {
												Text base = Text.literal(owner);
												lineText = Team.decorateName(team, base);
											}
											break;
										}
									}
								}
							}
						}
					} catch (Exception e) {
						// Silent error handling
					}
				}
				
				if (lineText != null) {
					String rawText = lineText.getString();   // z.B. "Biom", "-> Kohle", "[Kohle Mine]"
					String cleanText = removeFormatting.apply(rawText);
					
					lines.add(cleanText);
				}
			}
		} catch (Exception e) {
			// Silent error handling
		}
		
		return lines;
	}
	
	/**
	 * Helper class to store score entry information
	 */
	private static class ScoreEntry {
		int score;
		String displayText;
		String playerName;
		
		ScoreEntry(int score, String displayText, String playerName) {
			this.score = score;
			this.displayText = displayText;
			this.playerName = playerName;
		}
	}
	
	/**
	 * Gets the raw display text from a score entry (with formatting codes)
	 */
	private static String getScoreEntryTextRaw(net.minecraft.scoreboard.Scoreboard scoreboard, Object scoreObj) {
		try {
			// Try to get display name directly (preferred method)
			try {
				java.lang.reflect.Method getDisplayNameMethod = scoreObj.getClass().getMethod("getDisplayName");
				net.minecraft.text.Text displayName = (net.minecraft.text.Text) getDisplayNameMethod.invoke(scoreObj);
				if (displayName != null) {
					return displayName.getString();
				}
			} catch (Exception e) {
				// Fallback to owner name
			}
			
			// Get owner name (internal name) as fallback
			java.lang.reflect.Method getPlayerNameMethod = scoreObj.getClass().getMethod("getPlayerName");
			String ownerName = (String) getPlayerNameMethod.invoke(scoreObj);
			
			if (ownerName == null) {
				return null;
			}
			
			// Try to get team for prefix/suffix using reflection
			net.minecraft.scoreboard.Team team = null;
			try {
				java.lang.reflect.Method getPlayerTeamMethod = scoreboard.getClass().getMethod("getPlayerTeam", String.class);
				team = (net.minecraft.scoreboard.Team) getPlayerTeamMethod.invoke(scoreboard, ownerName);
			} catch (Exception e) {
				// Method not available, continue without team
			}
			
			// Build display text like in HUD
			net.minecraft.text.Text displayText;
			if (team != null) {
				displayText = net.minecraft.scoreboard.Team.decorateName(team, net.minecraft.text.Text.literal(ownerName));
			} else {
				displayText = net.minecraft.text.Text.literal(ownerName);
			}
			
			return displayText.getString();
		} catch (Exception e) {
			// Silent error handling
			return null;
		}
	}
	
	/**
	 * Helper: Checks if a line is empty (only whitespace/formatting)
	 */
	private static boolean isEmptyLine(String s) {
		if (s == null) return true;
		String clean = s.replaceAll("§[0-9a-fk-or]", "").trim();
		return clean.isEmpty();
	}
	
	/**
	 * Helper: Checks if a line is the "Biom" label
	 * Can be "Biom" or "▌ Biom"
	 */
	private static boolean isBiomLabel(String s) {
		if (s == null) return false;
		String clean = s.replaceAll("§[0-9a-fk-or]", "").trim();
		// Remove the ▌ symbol if present
		clean = clean.replace("▌", "").trim();
		return clean.equalsIgnoreCase("Biom");
	}
	
	/**
	 * Helper: Checks if a line is the "Ressource" label
	 * Can be "Ressource" or "▌ Ressource"
	 */
	private static boolean isRessourceLabel(String s) {
		if (s == null) return false;
		String clean = s.replaceAll("§[0-9a-fk-or]", "").trim();
		// Remove the ▌ symbol if present
		clean = clean.replace("▌", "").trim();
		return clean.equalsIgnoreCase("Ressource");
	}
	
	/**
	 * Helper: Checks if a line starts with arrow (-> or ➥)
	 */
	private static boolean isArrowLine(String s) {
		if (s == null) return false;
		String clean = s.replaceAll("§[0-9a-fk-or]", "").trim();
		return clean.startsWith("->") || clean.startsWith("➥");
	}
	
	/**
	 * Helper: Extracts text after arrow (-> or ➥)
	 * Removes square brackets if present: "[Kohle Mine]" -> "Kohle Mine"
	 */
	private static String extractAfterArrow(String s) {
		if (s == null) return null;
		String clean = s.replaceAll("§[0-9a-fk-or]", "").trim();
		
		// Find arrow position
		int arrowIdx = -1;
		if (clean.startsWith("➥")) {
			arrowIdx = 0;
			clean = clean.substring(1).trim();
		} else {
			int idx = clean.indexOf("->");
			if (idx != -1) {
				arrowIdx = idx;
				clean = clean.substring(idx + 2).trim();
			}
		}
		
		if (arrowIdx == -1) return clean;
		
		// Remove square brackets if present
		if (clean.startsWith("[") && clean.endsWith("]")) {
			clean = clean.substring(1, clean.length() - 1).trim();
		}
		
		return clean;
	}
	
	/**
	 * Extracts biom name from scoreboard lines
	 */
	private static String extractBiomFromLines(List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (isBiomLabel(line)) {
				// Look for next non-empty line with arrow
				for (int j = i + 1; j < lines.size(); j++) {
					String next = lines.get(j);
					if (isEmptyLine(next)) continue;
					if (isArrowLine(next)) {
						return extractAfterArrow(next);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Extracts ressource name from scoreboard lines
	 */
	private static String extractRessourceFromLines(List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (isRessourceLabel(line)) {
				// Look for next non-empty line with arrow
				for (int j = i + 1; j < lines.size(); j++) {
					String next = lines.get(j);
					if (isEmptyLine(next)) continue;
					if (isArrowLine(next)) {
						return extractAfterArrow(next);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Registers collection hotkeys
	 */
	private static void registerCollectionHotkeys() {
		// Register reset hotkey
		collectionResetKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.cclive-utilities.collection-reset",
			InputUtil.Type.KEYSYM,
			InputUtil.UNKNOWN_KEY.getCode(), // Unbound by default
			"category.cclive-utilities.collection"
		));
	}
	
	/**
	 * Handles collection hotkeys
	 */
	private static void handleCollectionHotkeys() {
		// Handle reset hotkey
		if (collectionResetKeyBinding != null && collectionResetKeyBinding.wasPressed()) {
			resetCollectionTracking();
			// Restart timer if overlay is enabled and we're tracking
			if (CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay && isTrackingCollections) {
				sessionStartTime = System.currentTimeMillis();
			} else {
				sessionStartTime = 0;
			}
		}
	}
	
	/**
	 * Updates blocks per minute calculation
	 */
	private static void updateBlocksPerMinute() {
		// Only calculate if overlay is enabled and timer is running
		if (CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay && 
		    sessionStartTime > 0 && sessionBlocks > 0) {
			long elapsedTime = System.currentTimeMillis() - sessionStartTime;
			if (elapsedTime > 0) {
				double minutes = elapsedTime / 60000.0;
				blocksPerMinute = sessionBlocks / minutes;
			}
		} else {
			blocksPerMinute = 0.0;
		}
	}
	
	/**
	 * Calculates time until next collection in minutes
	 */
	private static double calculateTimeUntilNextCollection() {
		if (blocksNeededForNextCollection <= 0 || blocksPerMinute <= 0) {
			return -1;
		}
		
		return (double)blocksNeededForNextCollection / blocksPerMinute;
	}
	
	/**
	 * Formats time in minutes to readable string
	 */
	private static String formatTimeMinutes(double minutes) {
		if (minutes < 0) {
			return "-";
		}
		
		if (minutes < 1) {
			return "<1m";
		} else if (minutes < 60) {
			return String.format("%.1f min", minutes);
		} else {
			int hours = (int)(minutes / 60);
			int mins = (int)(minutes % 60);
			return String.format("%d:%02d h", hours, mins);
		}
	}
	
	/**
	 * Renders the collection overlay
	 */
	private static void renderCollectionOverlay(DrawContext context, MinecraftClient client) {
		if (!isTrackingCollections || client.player == null) {
			return;
		}
		
		// Hide overlay if F1 menu (debug screen) is open
		if (client.options.hudHidden) {
			return;
		}
		
		// Render only when overlays are visible
		if (!showOverlays) {
			return;
		}
		
		// Check if overlay is enabled in config
		if (!CCLiveUtilitiesConfig.HANDLER.instance().showCollectionOverlay) {
			return;
		}
		
		// Hide overlay if biom is not detected (scoreboard disappeared)
		if (!biomDetected) {
			return;
		}
		
		// Get position and scale from config
		int x = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayX;
		int y = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayY;
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayScale;
		if (scale <= 0) scale = 1.0f;
		
		int padding = 5;
		
		// Calculate elapsed time (format like Kill Tracker: XX:XX)
		long elapsedTime = System.currentTimeMillis() - sessionStartTime;
		long minutes = elapsedTime / 60000;
		long seconds = (elapsedTime % 60000) / 1000;
		String timeText = String.format("Zeit: %02d:%02d", minutes, seconds);
		
		// Build overlay text
		String header = "Collection:";
		String timeLine = timeText; // timeText already contains "Zeit: XX:XX"
		String blocksLine = "Abgebaut: " + formatNumberWithSeparator(sessionBlocks);
		String blocksPerMinLine = "Blöcke/min: " + (blocksPerMinute > 0 ? formatDoubleWithSeparator(blocksPerMinute) : "-");
		String blocksNeededLine = "Benötigte Blöcke: " + formatNumberWithSeparator(blocksNeededForNextCollection);
		String timeToNextLine = "Nächste Collection: " + formatTimeMinutes(calculateTimeUntilNextCollection());
		
		// Calculate overlay width (unscaled)
		int maxWidth = Math.max(
			Math.max(client.textRenderer.getWidth(header),
				Math.max(client.textRenderer.getWidth(timeLine),
					Math.max(client.textRenderer.getWidth(blocksLine),
						Math.max(client.textRenderer.getWidth(blocksPerMinLine),
							Math.max(client.textRenderer.getWidth(blocksNeededLine),
								client.textRenderer.getWidth(timeToNextLine)))))),
			100);
		
		int unscaledWidth = maxWidth + padding * 2;
		int unscaledHeight = 6 * 11 + padding * 2; // 6 lines with 11px line height
		
		// Use Matrix transformations for scaling
		org.joml.Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		
		// Translate to position and scale from there
		matrices.translate(x, y);
		matrices.scale(scale, scale);
		
		// Draw background (scaled, relative to matrix)
		context.fill(0, 0, unscaledWidth, unscaledHeight, 0x80000000);
		
		// Get colors from config
		int headerColor = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayHeaderColor.getRGB();
		int textColor = CCLiveUtilitiesConfig.HANDLER.instance().collectionOverlayTextColor.getRGB();
		
		// Draw text (scaled, relative to matrix)
		int textY = padding;
		context.drawText(client.textRenderer, header, padding, textY, headerColor, true);
		textY += 11;
		context.drawText(client.textRenderer, timeLine, padding, textY, textColor, true);
		textY += 11;
		context.drawText(client.textRenderer, blocksLine, padding, textY, textColor, true);
		textY += 11;
		context.drawText(client.textRenderer, blocksPerMinLine, padding, textY, textColor, true);
		textY += 11;
		context.drawText(client.textRenderer, blocksNeededLine, padding, textY, textColor, true);
		textY += 11;
		context.drawText(client.textRenderer, timeToNextLine, padding, textY, textColor, true);
		
		matrices.popMatrix();
	}
	
	/**
	 * Returns whether collection tracking is currently active
	 * Used by CollectionDraggableOverlay to determine if overlay should be enabled
	 */
	public static boolean isTrackingCollections() {
		return isTrackingCollections;
	}
	
	/**
	 * Returns whether a biome was detected in the scoreboard
	 * Used by OverlayEditorScreen to determine if overlays should be shown
	 */
	public static boolean isBiomDetected() {
		return biomDetected;
	}
	
	/**
	 * Get current overlay width based on actual text content
	 * Used by CollectionDraggableOverlay to match the actual overlay size
	 */
	public static int getCurrentCollectionOverlayWidth(MinecraftClient client) {
		if (client == null || client.textRenderer == null) {
			return 200; // Default fallback width
		}
		
		int padding = 5;
		
		// Calculate elapsed time (format like Kill Tracker: XX:XX)
		long elapsedTime = sessionStartTime > 0 ? System.currentTimeMillis() - sessionStartTime : 0;
		long minutes = elapsedTime / 60000;
		long seconds = (elapsedTime % 60000) / 1000;
		String timeText = String.format("Zeit: %02d:%02d", minutes, seconds);
		
		// Build overlay text (same as in renderCollectionOverlay)
		String header = "Collection:";
		String timeLine = timeText;
		String blocksLine = "Abgebaut: " + formatNumberWithSeparator(sessionBlocks);
		String blocksPerMinLine = "Blöcke/min: " + (blocksPerMinute > 0 ? formatDoubleWithSeparator(blocksPerMinute) : "-");
		String blocksNeededLine = "Benötigte Blöcke: " + formatNumberWithSeparator(blocksNeededForNextCollection);
		String timeToNextLine = "Nächste Collection: " + formatTimeMinutes(calculateTimeUntilNextCollection());
		
		// Calculate overlay width (same as renderCollectionOverlay)
		int maxWidth = Math.max(
			Math.max(client.textRenderer.getWidth(header),
				Math.max(client.textRenderer.getWidth(timeLine),
					Math.max(client.textRenderer.getWidth(blocksLine),
						Math.max(client.textRenderer.getWidth(blocksPerMinLine),
							Math.max(client.textRenderer.getWidth(blocksNeededLine),
								client.textRenderer.getWidth(timeToNextLine)))))),
			100);
		
		return maxWidth + padding * 2;
	}
	
	/**
	 * Get current overlay height based on actual content
	 */
	public static int getCurrentCollectionOverlayHeight() {
		int padding = 5;
		return 6 * 11 + padding * 2; // 6 lines with 11px line height
	}
}


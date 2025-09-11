package net.felix.utilities;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.felix.CCLiveUtilitiesConfig;


import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BossHPUtility {
	
	
	private static boolean isInitialized = false;
	private static boolean showOverlays = true; // Neue Variable für Overlay-Sichtbarkeit
	private static BossHPUtility INSTANCE;
	
	// Boss-Status-Variablen
	private String currentBossName = null;
	private String currentBossText = null;
	private String lastKnownHP = null;
	private String lastKnownBossName = null;
	private boolean bossDefeated = false;
	
	// Validierte Boss-Namen
	private final Set<String> validBossNames = new HashSet<>();
	
	// Regex-Pattern für Boss-Text-Parsing
	private final Pattern bossPattern = Pattern.compile("(.+?)\\|{5}(\\d+)\\|{5}");
	
	// Client-seitige Variablen
	private String lastDimension = "";
	private final Set<String> ignoredNames = new HashSet<>();
	private boolean wavesActive = false;
	private boolean lastWaveState = false;
	

	
	// Boss-Ergebnis-Erkennung
	private boolean waitingForBossResult = false;
	private long bossDisappearTimeWithoutReward = 0;
	private static final long BOSS_DISAPPEAR_TIMEOUT = 5000; // 5 Sekunden
	private boolean bossLost = false; // Separate variable for boss lost (vs defeated)
	
	public BossHPUtility() {
		INSTANCE = this;
		initializeBossNames();
		initializeIgnoredNames();
	}
	
	public static BossHPUtility getInstance() {
		if (INSTANCE == null) {
			throw new IllegalStateException("BossHPUtility instance is null!");
		}
		return INSTANCE;
	}
	
	private void initializeBossNames() {
		validBossNames.add("Großer Wächter");
		validBossNames.add("Sniffer");
		validBossNames.add("Kamel");
		validBossNames.add("Wither");
		validBossNames.add("Warden");
		validBossNames.add("Riese");
		validBossNames.add("Ravager");
	}
	
	private void initializeIgnoredNames() {
		// Räume
		ignoredNames.add("(Forschraum)");
		ignoredNames.add("(Schmied)");
		ignoredNames.add("(Kaktusfarm)");
		ignoredNames.add("(Seelenraum)");
		// Status
		ignoredNames.add("Freigeschalten!");
		// NPCs
		ignoredNames.add("Kaktusmacs");
		ignoredNames.add("Forschermacs");
		ignoredNames.add("Schmiedmacs");
		ignoredNames.add("Seelenmacs");
		ignoredNames.add("Runenmacs");
		ignoredNames.add("Farmermacs");
		ignoredNames.add("Soul Harvester");
		ignoredNames.add("Essence Harvester");
		// Ranglisten und UI Elemente
		ignoredNames.add("Top Rangliste");
		ignoredNames.add("#");
		ignoredNames.add("Kaktus");
		ignoredNames.add("Seelen");
		ignoredNames.add("Nächste Aktualisierung");
		ignoredNames.add("Klicke für Ranglisten-Wechsel");
		ignoredNames.add("Deine Rangliste");
		ignoredNames.add("Aktuelles Tier");
		ignoredNames.add("Nächstes Tier in");
	}
	
	public static void initialize() {
		if (isInitialized) {
			return;
		}
		
		try {
			// Instance erstellen
			new BossHPUtility();
			
			// Register commands
			registerCommands();
			
			// Client-seitige Events registrieren
			ClientTickEvents.END_CLIENT_TICK.register(BossHPUtility::onClientTick);
			
			// Registriere HUD-Rendering
			HudRenderCallback.EVENT.register((drawContext, tickDelta) -> onHudRender(drawContext, tickDelta));
			
			// Chat-Nachrichten Event registrieren
			ClientReceiveMessageEvents.GAME.register(BossHPUtility::onChatMessage);
			
			isInitialized = true;
		} catch (Exception e) {
			// Error initializing BossHPUtility
		}
	}
	
	private static void registerCommands() {
		// Keine Commands mehr nötig, da Test-Overlay entfernt wurde
	}
	
	private static void onClientTick(MinecraftClient client) {
		// Check Tab key for overlay visibility
		checkTabKey();
		BossHPUtility instance = BossHPUtility.getInstance();
		

		
		if (client.player == null || client.world == null) {
			return;
		}

        String playerName = client.player.getName().getString().toLowerCase();
        String dimensionPath = client.world.getRegistryKey().getValue().getPath();
        
        // Check dimension change
        if (!dimensionPath.equals(instance.lastDimension)) {
            if (instance.lastDimension.equals(playerName)) {
                instance.clearCurrentBoss();
            }
            instance.lastDimension = dimensionPath;
        }
        
        // Check if dimension matches player name (player is in their own dimension)
        if (dimensionPath.equals(playerName)) {
            // Player is in their own dimension - waves are active
            instance.wavesActive = true;
            
            									// Only scan for bosses when in player's own dimension
			
			for (Entity entity : client.world.getEntities()) {
                String displayText = null;
                
                // Check if it's a TextDisplay or Display entity
                if (entity.getType().toString().contains("text_display") || 
                    entity.getType().toString().contains("display")) {
                    displayText = getTextFromDisplayEntity(entity);
                }
                // Fallback to custom name check
                else if (entity.hasCustomName()) {
                    Text customName = entity.getCustomName();
                    if (customName != null) {
                        displayText = customName.getString().trim();
                    }
                }
                
                if (displayText != null && !instance.shouldIgnoreEntity(displayText)) {
                    instance.processTextDisplay(displayText);
                }
            }
            

        } else {
            // Player is not in their own dimension - waves are inactive
            instance.wavesActive = false;
            
            // Clear boss if waves become inactive
            if (instance.lastWaveState && !instance.wavesActive) {
                if (instance.waitingForBossResult) {
                    instance.handleBossLost();
                } else {
                    instance.clearCurrentBoss();
                }
            }
        }
        
        instance.lastWaveState = instance.wavesActive;
	}
	
	/**
	 * Attempts to extract text from various types of display entities
	 */
	private static String getTextFromDisplayEntity(Entity entity) {
		try {
			// Try to get text using reflection for different entity types
			String entityType = entity.getClass().getSimpleName();
			
			// For TextDisplayEntity or similar
			if (entityType.contains("TextDisplay") || entityType.contains("Display")) {
				// Try different methods to get text content
				try {
					// Method 1: Try to get text component
					java.lang.reflect.Method getTextMethod = entity.getClass().getMethod("getText");
					Object textComponent = getTextMethod.invoke(entity);
					if (textComponent instanceof Text) {
						String result = ((Text) textComponent).getString();
						return result;
					}
				} catch (Exception e) {
					// Method not found, continue to next method
				}
				
				try {
					// Method 2: Try to get text as string
					java.lang.reflect.Method getTextStringMethod = entity.getClass().getMethod("getTextString");
					Object textString = getTextStringMethod.invoke(entity);
					if (textString instanceof String) {
						String result = (String) textString;
						return result;
					}
				} catch (Exception e) {
					// Method not found, continue to next method
				}
				
				try {
					// Method 3: Try to get custom name
					if (entity.hasCustomName()) {
						Text customName = entity.getCustomName();
						if (customName != null) {
							String result = customName.getString();
							return result;
						}
					}
				} catch (Exception e) {
					// Could not get custom name, continue to next method
				}
				
				// Method 4: Try to get entity name
				String entityName = entity.getName().getString();
				if (entityName != null && !entityName.isEmpty() && !entityName.equals("entity.minecraft.text_display")) {
					return entityName;
				}
			}
		} catch (Exception e) {
			// Error extracting text, return null
		}
		
		return null;
	}
	

	
	private boolean shouldIgnoreEntity(String name) {
		// Entferne zusätzliche Leerzeichen am Anfang und Ende
		name = name.trim();
		// Entferne mehrfache Leerzeichen innerhalb des Textes
		name = name.replaceAll("\\s+", " ");
		
		for (String ignoredName : ignoredNames) {
			if (name.contains(ignoredName)) {
				return true;
			}
		}
		return false;
	}
	
	public void processTextDisplay(String text) {
		
		boolean containsBossName = validBossNames.stream()
			.anyMatch(text::contains);
		
		if (containsBossName) {
			Matcher matcher = bossPattern.matcher(text);
			
			if (matcher.matches()) {
				String bossName = matcher.group(1).trim();
				
				if (validBossNames.contains(bossName)) {
					// Reset boss defeated flag when a new boss appears
					if (bossDefeated) {
						bossDefeated = false;
					}
					
					currentBossName = bossName;
					currentBossText = text;
				}
			}
		}
	}
	

	
	public void hideOverlay() {
		lastKnownHP = null;
		lastKnownBossName = null;
		bossDisappearTimeWithoutReward = 0;
		waitingForBossResult = false;
		bossDefeated = false;
		bossLost = false;
		currentBossName = null;
		currentBossText = null;
	}
	
	public void clearCurrentBoss() {
		if (currentBossName != null || currentBossText != null) {
			// Wenn Boss bereits besiegt wurde, verstecke Overlay sofort
			if (bossDefeated) {
				hideOverlay();
				return;
			}
			
			// Wenn Boss bereits verloren wurde, verstecke Overlay sofort
			if (bossLost) {
				hideOverlay();
				return;
			}
			
			// Boss verschwindet - speichere verbleibende HP für 5-Sekunden-Anzeige
			if (currentBossText != null) {
				String[] parts = currentBossText.split("\\|{5}");
				if (parts.length >= 2) {
					String hp = parts[1].trim();
					// Boss verschwunden - warte auf Ergebnis (Actionbar oder Chat-Nachricht)
					lastKnownHP = hp;
					lastKnownBossName = currentBossName;
					bossDisappearTimeWithoutReward = System.currentTimeMillis();
					waitingForBossResult = true;
				} else {
					hideOverlay();
				}
			} else {
				hideOverlay();
			}
			
			// Lösche aktuellen Boss
			currentBossName = null;
			currentBossText = null;
		}
	}
	
	public boolean shouldShowDisappearedBoss() {
		// Wenn Boss besiegt wurde, zeige kein verschwundenes Overlay
		if (bossDefeated) {
			return false;
		}
		
		// Wenn Boss verloren wurde, zeige verbleibende HP für 5 Sekunden
		if (bossLost) {
			if (lastKnownHP == null || bossDisappearTimeWithoutReward == 0) {
				return false;
			}
			
			long timeSinceDisappear = System.currentTimeMillis() - bossDisappearTimeWithoutReward;
			boolean shouldShow = timeSinceDisappear < BOSS_DISAPPEAR_TIMEOUT;
			
			// Wenn Zeit abgelaufen ist, verstecke das Overlay
			if (!shouldShow) {
				bossLost = false;
				hideOverlay();
			}
			
			return shouldShow;
		}
		
		// Für verschwundenen Boss ohne Ergebnis (warten auf Actionbar/Chat)
		if (lastKnownHP == null || bossDisappearTimeWithoutReward == 0) {
			return false;
		}
		
		if (!waitingForBossResult) {
			return false;
		}
		
		long timeSinceDisappear = System.currentTimeMillis() - bossDisappearTimeWithoutReward;
		boolean shouldShow = timeSinceDisappear < BOSS_DISAPPEAR_TIMEOUT;
		
		// Wenn Zeit abgelaufen ist, verstecke das Overlay
		if (!shouldShow && waitingForBossResult) {
			waitingForBossResult = false;
			hideOverlay();
		}
		
		return shouldShow;
	}
	
	private static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return;
		}
		
		// Prüfe Konfiguration
		if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
			!CCLiveUtilitiesConfig.HANDLER.instance().bossHPEnabled ||
			!CCLiveUtilitiesConfig.HANDLER.instance().showBossHP) {
			return;
		}
		
		// Prüfe ob Spieler in seiner eigenen Dimension ist
		if (client.player == null || client.world == null) {
			return;
		}
		
		String playerName = client.player.getName().getString().toLowerCase();
		String dimensionPath = client.world.getRegistryKey().getValue().getPath();
		
		// Zeige Boss HP Overlay nur in Dimensionen, die den Spielernamen enthalten
		if (!dimensionPath.equals(playerName)) {
			return;
		}
		
		BossHPUtility instance = BossHPUtility.getInstance();
		
		// Render nur wenn Overlays sichtbar sind und keine Equipment-Overlays aktiv sind
		if (showOverlays && !EquipmentDisplayUtility.isEquipmentOverlayActive()) {
			renderBossHealthBars(context, client, instance);
		}
	}
	

	private static void renderBossHealthBars(DrawContext context, MinecraftClient client, BossHPUtility instance) {
		// Verwende Konfigurationspositionen
		int configX = CCLiveUtilitiesConfig.HANDLER.instance().bossHPX;
		int configY = CCLiveUtilitiesConfig.HANDLER.instance().bossHPY;
		
		// Zeichne aktuellen Boss (nur wenn nicht besiegt)
		if (instance.currentBossName != null && instance.currentBossText != null && !instance.bossDefeated) {
			renderBossBar(context, client, configX, configY, instance.currentBossName, instance.currentBossText, false);
		}
		// Zeichne verschwundenen Boss oder verlorenen Boss (für 5 Sekunden)
		else if (instance.shouldShowDisappearedBoss()) {
			renderBossBar(context, client, configX, configY, instance.lastKnownBossName, instance.lastKnownHP, true);
		}
	}
	
	private static void renderBossBar(DrawContext context, MinecraftClient client, int x, int y, String bossName, String hpText, boolean isDisappeared) {
		// Konstanten für das Layout
		final int TEXT_COLOR = 0xFFFFFFFF; // Weiß
		final int HP_COLOR = 0xFFFF5555;   // Rot
		final int BACKGROUND_COLOR = 0x80000000; // Halbtransparentes Schwarz
		final int PADDING = 4;
		
		// Parse HP-Werte
		String displayText = null;
		String displayHP = null;
		
		if (isDisappeared) {
			displayText = bossName + " verbleibend";
			displayHP = hpText;
		} else if (hpText.isEmpty()) {
			// Fallback: zeige nur den Boss-Namen
			displayText = bossName;
			displayHP = "";
		} else {
			String[] parts = hpText.split("\\|{5}");
			if (parts.length >= 2) {
				displayText = parts[0].trim();
				displayHP = parts[1].trim();
			}
		}
		
		if (displayText == null) {
			return;
		}
		
		// Berechne die Breiten für das Layout
		int nameWidth = client.textRenderer.getWidth(displayText);
		int hpWidth = displayHP.isEmpty() ? 0 : client.textRenderer.getWidth(displayHP);
		int totalWidth = nameWidth + (displayHP.isEmpty() ? 0 : 10 + hpWidth); // 10 Pixel Abstand zwischen Name und HP nur wenn HP vorhanden
		
		// Verwende absolute Konfigurationspositionen - Overlay wächst nach links
		int posX = x - totalWidth - PADDING * 2; // Position von rechts nach links
		int posY = y;
		
		// Zeichne den Hintergrund nur wenn aktiviert
		if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowBackground) {
			context.fill(posX, posY, 
					 posX + totalWidth + PADDING * 2, posY + client.textRenderer.fontHeight + PADDING * 2, 
					 BACKGROUND_COLOR);
		}
		
		// Zeichne den Boss-Namen (weiß)
		context.drawText(client.textRenderer, displayText, posX + PADDING, posY + PADDING, TEXT_COLOR, true);
		
		// Zeichne die HP (rot) nur wenn vorhanden
		if (!displayHP.isEmpty()) {
			context.drawText(client.textRenderer, displayHP, posX + PADDING + nameWidth + 10, posY + PADDING, HP_COLOR, true);
		}
		

	}
	

	

	

	
	/**
	 * Chat-Nachrichten Event Handler
	 * Note: Actionbar messages are now handled via ActionBarMixin
	 */
	private static void checkTabKey() {
		// Check if player list key is pressed (respects custom key bindings)
		if (KeyBindingUtility.isPlayerListKeyPressed()) {
			showOverlays = false; // Hide overlays when player list key is pressed
		} else {
			showOverlays = true; // Show overlays when player list key is released
		}
	}
	
	private static void onChatMessage(Text message, boolean overlay) {
		BossHPUtility instance = BossHPUtility.getInstance();
		String content = message.getString();
		
		// Only handle regular chat messages (not actionbar)
		if (!overlay) {
			// Prüfe auf "Ein Monster hat XXX Kakteen und XXX Seelen geklaut!" (Chat-Nachricht)
			if (content.contains("Ein Monster hat") && content.contains("Kakteen") && content.contains("Seelen geklaut")) {
				instance.handleBossLost();
			}
			// Prüfe auf "Du warst zu lange außerhalb der Kampfzone" (Chat-Nachricht)
			if (content.toLowerCase().contains("zu lange außerhalb") && content.toLowerCase().contains("kampfzone")) {
				instance.handleBossLost();
			}
			// Prüfe auf "Die welle wurde nicht geschafft! für die nächsten X minuten: farmen deaktiviert und -X% aktiver Ertrag" (Chat-Nachricht)
			if (content.toLowerCase().contains("die welle wurde nicht geschafft") && 
				content.toLowerCase().contains("farmen deaktiviert") && 
				content.toLowerCase().contains("aktiver ertrag")) {
				instance.handleWaveFailed();
			}
		}
	}
	

	
	/**
	 * Handler für Boss verloren (Chat-Nachricht)
	 */
	public void handleBossLost() {
		bossLost = true;
		waitingForBossResult = false;
		
		// Setze die verbleibenden HP für 5 Sekunden Anzeige
		if (currentBossText != null) {
			String[] parts = currentBossText.split("\\|{5}");
			if (parts.length >= 2) {
				String hp = parts[1].trim();
				lastKnownHP = hp;
				lastKnownBossName = currentBossName;
				bossDisappearTimeWithoutReward = System.currentTimeMillis();
			}
		}
		
		// Lösche aktuellen Boss
		currentBossName = null;
		currentBossText = null;
	}
	
	/**
	 * Handler für gescheiterte Welle (Chat-Nachricht)
	 * Zeigt verbleibende Boss-HP an, wenn eine Welle nicht geschafft wurde
	 */
	public void handleWaveFailed() {
		bossLost = true;
		waitingForBossResult = false;
		
		// Setze die verbleibenden HP für 5 Sekunden Anzeige
		if (currentBossText != null) {
			String[] parts = currentBossText.split("\\|{5}");
			if (parts.length >= 2) {
				String hp = parts[1].trim();
				lastKnownHP = hp;
				lastKnownBossName = currentBossName;
				bossDisappearTimeWithoutReward = System.currentTimeMillis();
			}
		}
		
		// Lösche aktuellen Boss
		currentBossName = null;
		currentBossText = null;
	}
	
	/**
	 * Handler für Boss besiegt (Actionbar-Nachricht)
	 */
	public void handleBossDefeated() {
		bossDefeated = true;
		waitingForBossResult = false;
		hideOverlay();
	}
	
	/**
	 * Prüft, ob aktuell ein Boss aktiv ist
	 */
	public static boolean isBossActive() {
		BossHPUtility instance = getInstance();
		return instance != null && instance.currentBossName != null && instance.currentBossText != null && !instance.bossDefeated;
	}
	
	/**
	 * Gibt den aktuellen Boss-Namen zurück
	 */
	public static String getCurrentBossName() {
		BossHPUtility instance = getInstance();
		return instance != null ? instance.currentBossName : null;
	}
	
	/**
	 * Gibt den aktuellen Boss-Text zurück
	 */
	public static String getCurrentBossText() {
		BossHPUtility instance = getInstance();
		return instance != null ? instance.currentBossText : null;
	}
	

	

} 
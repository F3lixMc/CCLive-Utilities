package net.felix.utilities.Factory;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.KeyBindingUtility;
import net.felix.utilities.Town.EquipmentDisplayUtility;
import org.joml.Matrix3x2fStack;


import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
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
	
	// DPM-Tracking-Variablen
	private BigInteger initialBossHP = null; // Initiale HP beim ersten Erkennen des Bosses
	private long bossFightStartTime = 0; // Zeitpunkt, wann der Kampf begonnen hat
	private String lastTrackedBossName = null; // Letzter Boss-Name für Erkennung von Boss-Wechsel
	
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
	
	// Boss-Blockierung nach "Kampfzone"-Nachricht
	private long bossBlockedUntil = 0;
	private static final long BOSS_BLOCK_DURATION = 10000; // 10 Sekunden Blockierung
	
	// Retry-Logik für "Welle nicht geschafft"
	private int waveFailedRetryCount = 0;
	private static final int WAVE_FAILED_MAX_RETRIES = 5;
	private boolean waitingForWaveFailedRetry = false;
	
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
	
	private static int entityScanTickCounter = 0;
	private static final int ENTITY_SCAN_INTERVAL = 5; // Alle 5 Ticks scannen (4x pro Sekunde) - Performance-Optimierung
	
	private static void onClientTick(MinecraftClient client) {
		// Check Tab key for overlay visibility
		checkTabKey();
		BossHPUtility instance = BossHPUtility.getInstance();
		
		// Prüfe auf Retry für "Welle nicht geschafft"
		if (instance.waitingForWaveFailedRetry && instance.waveFailedRetryCount < WAVE_FAILED_MAX_RETRIES) {
			// Versuche Boss zu erkennen (wird in processTextDisplay behandelt)
			// Erhöhe Retry-Counter nach jedem Tick
			instance.waveFailedRetryCount++;
			
			// Wenn Max-Retries erreicht, stoppe das Warten
			if (instance.waveFailedRetryCount >= WAVE_FAILED_MAX_RETRIES) {
				instance.waitingForWaveFailedRetry = false;
				instance.waveFailedRetryCount = 0;
			}
		}
		
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
            
            // Performance-Optimierung: Nur alle ENTITY_SCAN_INTERVAL Ticks scannen
            entityScanTickCounter++;
            if (entityScanTickCounter >= ENTITY_SCAN_INTERVAL) {
                entityScanTickCounter = 0;
                
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
		// Prüfe, ob Boss-Erkennung blockiert ist
		if (System.currentTimeMillis() < bossBlockedUntil) {
			return; // Boss-Erkennung ist blockiert
		}
		
		boolean containsBossName = validBossNames.stream()
			.anyMatch(text::contains);
		
		if (containsBossName) {
			Matcher matcher = bossPattern.matcher(text);
			
			if (matcher.matches()) {
				String bossName = matcher.group(1).trim();
				
				if (validBossNames.contains(bossName)) {
					// Wenn wir auf Retry für "Welle nicht geschafft" warten, speichere die HP
					if (waitingForWaveFailedRetry && waveFailedRetryCount < WAVE_FAILED_MAX_RETRIES) {
						String[] parts = text.split("\\|{5}");
						if (parts.length >= 2) {
							String hp = parts[1].trim();
							lastKnownHP = hp;
							lastKnownBossName = bossName;
							bossDisappearTimeWithoutReward = System.currentTimeMillis();
							bossLost = true;
							waitingForWaveFailedRetry = false;
							waveFailedRetryCount = 0;
							// Boss wurde erkannt, zeige verbleibende HP
							return;
						}
					}
					
					// Reset boss defeated flag when a new boss appears
					if (bossDefeated) {
						bossDefeated = false;
					}
					
					// Prüfe ob ein neuer Boss erscheint (anderer Name als vorher oder noch kein Boss getrackt)
					boolean isNewBoss = !bossName.equals(lastTrackedBossName) || lastTrackedBossName == null;
					
					if (isNewBoss) {
						// Neuer Boss - speichere initiale HP und starte Timer
						String[] parts = text.split("\\|{5}");
						if (parts.length >= 2) {
							try {
								initialBossHP = new BigInteger(parts[1].trim());
								bossFightStartTime = System.currentTimeMillis();
								lastTrackedBossName = bossName;
							} catch (NumberFormatException e) {
								// HP konnte nicht geparst werden, ignoriere
								initialBossHP = null;
								bossFightStartTime = 0;
							}
						}
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
		// Reset DPM-Tracking
		initialBossHP = null;
		bossFightStartTime = 0;
		lastTrackedBossName = null;
		// Blockierung wird automatisch nach BOSS_BLOCK_DURATION ablaufen
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
			
			// Reset DPM-Tracking wenn Boss verschwindet
			initialBossHP = null;
			bossFightStartTime = 0;
			lastTrackedBossName = null;
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
		
		// Hide overlay if F1 menu (debug screen) is open
		if (client.options.hudHidden) {
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
		final int LINE_SPACING = 2; // Abstand zwischen Zeilen
		
		// Hole Farben aus Config
		int dpmColor = CCLiveUtilitiesConfig.HANDLER.instance().bossHPDPMColor.getRGB();
		int percentageColor = CCLiveUtilitiesConfig.HANDLER.instance().bossHPPercentageColor.getRGB();
		
		// Get scale from config
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().bossHPScale;
		if (scale <= 0) scale = 1.0f; // Safety check
		
		BossHPUtility instance = BossHPUtility.getInstance();
		
		// Parse HP-Werte
		String displayText = null;
		String displayHP = null;
		BigInteger currentHP = null;
		
		if (isDisappeared) {
			displayText = bossName + " verbleibend";
			// Formatiere HP mit Tausender-Trennung
			try {
				BigInteger hpValue = new BigInteger(hpText);
				displayHP = formatBigInteger(hpValue);
			} catch (NumberFormatException e) {
				// HP konnte nicht geparst werden, verwende Original-Text
				displayHP = hpText;
			}
		} else if (hpText.isEmpty()) {
			// Fallback: zeige nur den Boss-Namen
			displayText = bossName;
			displayHP = "";
		} else {
			String[] parts = hpText.split("\\|{5}");
			if (parts.length >= 2) {
				displayText = parts[0].trim();
				String rawHP = parts[1].trim();
				try {
					currentHP = new BigInteger(rawHP);
					// Formatiere HP mit Tausender-Trennung
					displayHP = formatBigInteger(currentHP);
				} catch (NumberFormatException e) {
					// HP konnte nicht geparst werden, verwende Original-Text
					displayHP = rawHP;
					currentHP = null;
				}
			}
		}
		
		if (displayText == null) {
			return;
		}
		
		// Berechne Prozentwert (nur wenn initialBossHP und currentHP verfügbar sind und Prozentwert-Anzeige aktiviert ist)
		String percentageText = null;
		if (!isDisappeared && CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowPercentage &&
			instance.initialBossHP != null && currentHP != null && 
			instance.initialBossHP.compareTo(BigInteger.ZERO) > 0) {
			// Berechne Prozent: (currentHP / initialBossHP) * 100
			double percentage = currentHP.doubleValue() / instance.initialBossHP.doubleValue() * 100.0;
			// Formatiere mit einer Dezimalstelle
			percentageText = String.format(Locale.US, "%.1f%%", percentage);
		}
		
		// Berechne DPM (nur wenn Boss aktiv ist und nicht verschwunden und DPM-Anzeige aktiviert ist)
		String dpmText = null;
		if (!isDisappeared && CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowDPM && 
			instance.initialBossHP != null && currentHP != null && instance.bossFightStartTime > 0) {
			long currentTime = System.currentTimeMillis();
			long fightDuration = currentTime - instance.bossFightStartTime;
			
			if (fightDuration > 0) {
				// Berechne abgezogene HP
				BigInteger damageDealt = instance.initialBossHP.subtract(currentHP);
				
				// Berechne DPM (Damage Per Minute)
				// fightDuration ist in Millisekunden, also teilen durch 60000 für Minuten
				double minutes = fightDuration / 60000.0;
				if (minutes > 0) {
					// Konvertiere BigInteger zu double für Division
					double damageDealtDouble = damageDealt.doubleValue();
					double dpm = damageDealtDouble / minutes;
					// Formatiere DPM mit Tausendertrennzeichen
					dpmText = String.format("DPM: %,.0f", dpm);
				}
			}
		}
		
		// Berechne die Breiten für das Layout (unscaled)
		int nameWidth = client.textRenderer.getWidth(displayText);
		int hpWidth = displayHP.isEmpty() ? 0 : client.textRenderer.getWidth(displayHP);
		int separatorWidth = percentageText != null ? client.textRenderer.getWidth("|") : 0;
		int percentageWidth = percentageText != null ? client.textRenderer.getWidth(percentageText) : 0;
		int dpmWidth = dpmText != null ? client.textRenderer.getWidth(dpmText) : 0;
		int totalWidth = Math.max(
			nameWidth + (displayHP.isEmpty() ? 0 : 10 + hpWidth + (percentageText != null ? 5 + separatorWidth + 5 + percentageWidth : 0)), // Erste Zeile
			dpmWidth // Zweite Zeile (DPM)
		);
		int totalHeight = client.textRenderer.fontHeight + PADDING * 2;
		if (dpmText != null) {
			totalHeight += client.textRenderer.fontHeight + LINE_SPACING; // Zusätzliche Zeile für DPM
		}
		
		// Die Gesamtbreite inklusive Padding (unscaled)
		int unscaledTotalWidth = totalWidth + PADDING * 2;
		
		// Berechne skalierte Breite
		float scaledTotalWidth = unscaledTotalWidth * scale;
		int overlayWidth = Math.round(scaledTotalWidth);
		
		// Position aus Config: baseX ist die linke Kante (wie beim Mining-Overlay)
		int baseX = x;
		int screenWidth = client.getWindow().getScaledWidth();
		
		// Determine if overlay is on left or right side of screen
		boolean isOnLeftSide = baseX < screenWidth / 2;
		
		// Calculate X position based on side (same logic as Mining overlays)
		int posX;
		if (isOnLeftSide) {
			// On left side: keep left edge fixed, expand to the right
			posX = baseX;
		} else {
			// On right side: keep right edge fixed, expand to the left
			// Right edge is: baseX (since baseX is on the right side, it represents the right edge)
			// Keep this right edge fixed, so left edge moves left when width increases
			posX = baseX - overlayWidth;
		}
		
		// Ensure overlay stays within screen bounds
		posX = Math.max(0, Math.min(posX, screenWidth - overlayWidth));
		int posY = y;
		
		// Verwende Matrix-Transformationen für Skalierung
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		
		// Translate to position and scale from there
		matrices.translate(posX, posY);
		matrices.scale(scale, scale);
		
		// Zeichne den Hintergrund nur wenn aktiviert (innerhalb der Matrix-Transformation)
		if (CCLiveUtilitiesConfig.HANDLER.instance().bossHPShowBackground) {
			context.fill(0, 0, 
					 unscaledTotalWidth, totalHeight, 
					 BACKGROUND_COLOR);
		}
		
		// Zeichne den Boss-Namen (weiß) - relativ zur Matrix
		context.drawText(client.textRenderer, displayText, PADDING, PADDING, TEXT_COLOR, true);
		
		// Zeichne die HP (rot) nur wenn vorhanden - relativ zur Matrix
		if (!displayHP.isEmpty()) {
			int hpX = PADDING + nameWidth + 10;
			context.drawText(client.textRenderer, displayHP, hpX, PADDING, HP_COLOR, true);
			
			// Zeichne Prozentwert direkt nach den HP
			if (percentageText != null) {
				int separatorX = hpX + hpWidth + 5; // 5 Pixel Abstand nach den HP
				context.drawText(client.textRenderer, "|", separatorX, PADDING, HP_COLOR, true);
				int percentageX = separatorX + separatorWidth + 5; // 5 Pixel Abstand nach dem Separator
				context.drawText(client.textRenderer, percentageText, percentageX, PADDING, percentageColor, true);
			}
		}
		
		// Zeichne DPM in zweiter Zeile, wenn verfügbar - relativ zur Matrix
		if (dpmText != null) {
			int dpmY = PADDING + client.textRenderer.fontHeight + LINE_SPACING;
			context.drawText(client.textRenderer, dpmText, PADDING, dpmY, dpmColor, true);
		}

		matrices.popMatrix();
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
				// Blockiere Boss-Erkennung für 10 Sekunden
				instance.bossBlockedUntil = System.currentTimeMillis() + BOSS_BLOCK_DURATION;
				
				// Rufe handleBossLost() mehrmals auf, um sicherzustellen, dass der Boss auf "verloren" gesetzt wird
				instance.handleBossLost();
				instance.handleBossLost();
				instance.handleBossLost();
			}
			// Prüfe auf "Die welle wurde nicht geschafft! für die nächsten X minuten: farmen deaktiviert und -X% aktiver Ertrag" (Chat-Nachricht)
			if (content.toLowerCase().contains("die welle wurde nicht geschafft") && 
				content.toLowerCase().contains("farmen deaktiviert") && 
				content.toLowerCase().contains("aktiver ertrag")) {
				instance.handleWaveFailed();
			}
			// Prüfe auf "Die Welle wurde nicht geschafft! Versuche sie erneut." (Chat-Nachricht)
			if (content.contains("Die Welle wurde nicht geschafft! Versuche sie erneut.") || 
				content.contains("Die welle wurde nicht geschafft! Versuche sie erneut.")) {
				instance.handleWaveFailedRetry();
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
		
		// Reset DPM-Tracking
		initialBossHP = null;
		bossFightStartTime = 0;
		lastTrackedBossName = null;
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
		
		// Reset DPM-Tracking
		initialBossHP = null;
		bossFightStartTime = 0;
		lastTrackedBossName = null;
	}
	
	/**
	 * Handler für "Die Welle wurde nicht geschafft! Versuche sie erneut." (Chat-Nachricht)
	 * Versucht 5 mal, die verbleibenden HP anzuzeigen, falls der Boss nochmal erkannt wird
	 */
	public void handleWaveFailedRetry() {
		bossLost = true;
		waitingForBossResult = false;
		waitingForWaveFailedRetry = true;
		waveFailedRetryCount = 0;
		
		// Setze die verbleibenden HP für Anzeige, falls vorhanden
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
		
		// Reset DPM-Tracking
		initialBossHP = null;
		bossFightStartTime = 0;
		lastTrackedBossName = null;
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
	
	/**
	 * Gibt die initialen Boss-HP zurück (für Prozentwert-Berechnung)
	 */
	public static BigInteger getInitialBossHP() {
		BossHPUtility instance = getInstance();
		return instance != null ? instance.initialBossHP : null;
	}
	
	/**
	 * Gibt die Startzeit des Boss-Kampfes zurück (für DPM-Berechnung)
	 */
	public static long getBossFightStartTime() {
		BossHPUtility instance = getInstance();
		return instance != null ? instance.bossFightStartTime : 0;
	}
	
	/**
	 * Formatiert eine BigInteger-Zahl mit Tausendertrennzeichen
	 */
	public static String formatBigInteger(BigInteger value) {
		NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
		return formatter.format(value);
	}

} 
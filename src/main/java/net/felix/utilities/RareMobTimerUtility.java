package net.felix.utilities;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import net.felix.CCLiveUtilitiesConfig;

import java.time.Duration;
import java.time.Instant;

public class RareMobTimerUtility {
    
    private static boolean isInitialized = false;
    private static boolean isRareMobActive = false;
    private static Instant rareMobStartTime = null;
    private static boolean showOverlays = true;
    private static boolean timerVisible = false; // Timer wird angezeigt, bis Taste gedrückt wird
    
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        // Register HUD render callback
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            if (timerVisible && showOverlays && 
                CCLiveUtilitiesConfig.HANDLER.instance().enableMod &&
                CCLiveUtilitiesConfig.HANDLER.instance().rareMobTimerEnabled &&
                CCLiveUtilitiesConfig.HANDLER.instance().showRareMobTimer) {
                onHudRender(context, tickDelta);
            }
        });
        
        // Register client tick events
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Check Tab key for overlay visibility
            checkTabKey();
            
            // Check for key press to hide timer
            checkKeyPressToHideTimer(client);
            
            // Check for key press to start timer manually (for testing)
            checkKeyPressToStartTimer(client);
        });
        
        // Register chat message listener for rare mob messages (server messages)
        ClientReceiveMessageEvents.GAME.register(RareMobTimerUtility::onChatMessage);
        

        
        isInitialized = true;
    }
    
    /**
     * Chat-Nachrichten Event Handler für seltene Mobs
     */
    private static void onChatMessage(Text message, boolean overlay) {
        // Prüfe Konfiguration
        if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
            !CCLiveUtilitiesConfig.HANDLER.instance().rareMobTimerEnabled) {
            return;
        }
        
        String content = message.getString();
        
        // Prüfe auf "Ein seltener Mob ist erschienen"
        if (content.contains("Ein seltener Mob ist erschienen")) {
            startRareMobTimer();
        }
        
        // Prüfe auf Nachrichten, die das Verschwinden des seltenen Mobs anzeigen
        if (content.contains("Der seltene Mob ist verschwunden") || 
            content.contains("Der seltene Mob ist verschwunden") ||
            content.contains("Der seltene Mob wurde besiegt") ||
            content.contains("Der seltene Mob ist geflohen")) {
            stopRareMobTimer();
        }
    }
    

    
    /**
     * Startet den Timer für den seltenen Mob
     */
    private static void startRareMobTimer() {
        isRareMobActive = true;
        rareMobStartTime = Instant.now();
        timerVisible = true; // Timer wird sofort angezeigt
        System.out.println("[RareMobTimer] Seltener Mob erschienen - Timer gestartet und angezeigt");
    }
    
    /**
     * Stoppt den Timer für den seltenen Mob
     */
    private static void stopRareMobTimer() {
        if (isRareMobActive && rareMobStartTime != null) {
            Duration duration = Duration.between(rareMobStartTime, Instant.now());
            long minutes = duration.toMinutes();
            long seconds = duration.getSeconds() % 60;
            System.out.println("[RareMobTimer] Seltener Mob verschwunden - Timer gestoppt. Dauer: " + 
                             minutes + "m " + seconds + "s");
        }
        
        isRareMobActive = false;
        rareMobStartTime = null;
        // timerVisible bleibt true - Timer wird weiterhin angezeigt bis Taste gedrückt wird
    }
    
    /**
     * Prüft Tab-Taste für Overlay-Sichtbarkeit
     */
    private static void checkTabKey() {
        // Check if player list key is pressed (respects custom key bindings)
        if (KeyBindingUtility.isPlayerListKeyPressed()) {
            showOverlays = false; // Hide overlays when player list key is pressed
        } else {
            showOverlays = true; // Show overlays when player list key is released
        }
    }
    
    /**
     * Prüft Tastendruck zum Verstecken des Timers
     */
    private static void checkKeyPressToHideTimer(MinecraftClient client) {
        // Prüfe ob der Timer sichtbar ist
        if (!timerVisible) {
            return;
        }
        
        // Prüfe auf Leertaste (Space) zum Verstecken des Timers
        if (client.getWindow() != null && 
            net.minecraft.client.util.InputUtil.isKeyPressed(client.getWindow().getHandle(), 
            net.minecraft.client.util.InputUtil.GLFW_KEY_SPACE)) {
            hideTimer();
        }
    }
    
    /**
     * Prüft Tastendruck zum manuellen Starten des Timers (für Tests)
     */
    private static void checkKeyPressToStartTimer(MinecraftClient client) {
        // Prüfe auf R-Taste zum manuellen Starten des Timers
        if (client.getWindow() != null && 
            net.minecraft.client.util.InputUtil.isKeyPressed(client.getWindow().getHandle(), 
            net.minecraft.client.util.InputUtil.GLFW_KEY_R)) {
            // Nur starten wenn kein Timer läuft
            if (!timerVisible) {
                startRareMobTimer();
                System.out.println("[RareMobTimer] Timer manuell mit R-Taste gestartet (für Tests)");
            }
        }
    }
    
    /**
     * Rendert den HUD für den seltenen Mob Timer
     */
    private static void onHudRender(DrawContext context, Object tickDelta) {
        if (!timerVisible || rareMobStartTime == null) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        
        // Berechne verstrichene Zeit
        Duration duration = Duration.between(rareMobStartTime, Instant.now());
        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds() % 60;
        
        // Formatiere Zeit-String
        String timeString = String.format("%02d:%02d", minutes, seconds);
        
        // Hole Konfiguration
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        int posX = config.rareMobTimerX;
        int posY = config.rareMobTimerY;
        
        // Zeichne Hintergrund falls aktiviert
        if (config.rareMobTimerShowBackground) {
            int headerWidth = client.textRenderer.getWidth("Seltener Mob Timer");
            int timeWidth = client.textRenderer.getWidth(timeString);
            int maxWidth = Math.max(headerWidth, timeWidth);
            
            context.fill(posX - 2, posY - 2, posX + maxWidth + 2, posY + 30, 0x80000000);
        }
        
        // Zeichne Header
        Text headerText = Text.literal("Seltener Mob Timer");
        context.drawText(client.textRenderer, headerText, posX, posY, 
                        config.rareMobTimerHeaderColor.getRGB(), true);
        
        // Zeichne Zeit
        Text timeText = Text.literal(timeString);
        context.drawText(client.textRenderer, timeText, posX, posY + 12, 
                        config.rareMobTimerTextColor.getRGB(), true);
    }
    
    /**
     * Gibt zurück, ob ein seltener Mob aktiv ist
     */
    public static boolean isRareMobActive() {
        return isRareMobActive;
    }
    
    /**
     * Gibt die verstrichene Zeit seit dem Erscheinen des seltenen Mobs zurück
     */
    public static Duration getRareMobDuration() {
        if (!isRareMobActive || rareMobStartTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(rareMobStartTime, Instant.now());
    }
    
    /**
     * Versteckt den Timer durch Tastendruck
     */
    public static void hideTimer() {
        timerVisible = false;
        System.out.println("[RareMobTimer] Timer durch Tastendruck versteckt");
    }
    
    /**
     * Gibt zurück, ob der Timer sichtbar ist
     */
    public static boolean isTimerVisible() {
        return timerVisible;
    }
}

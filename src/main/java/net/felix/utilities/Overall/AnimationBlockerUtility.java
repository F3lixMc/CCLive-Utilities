package net.felix.utilities.Overall;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import net.felix.CCLiveUtilitiesConfig;

import java.util.HashSet;
import java.util.Set;

public class AnimationBlockerUtility {

    private static boolean isInitialized = false;
    
    // Animation blocking state
    private static boolean animationBlockingEnabled = false;
    
    // Unicode characters to block - separated by animation type
    private static final Set<String> EPIC_DROPS_CHARACTERS = new HashSet<>();
    private static final Set<String> LEGENDARY_DROPS_CHARACTERS = new HashSet<>();
    private static final Set<String> LOGGING_LEVEL_UP_CHARACTERS = new HashSet<>();
    private static final Set<String> MOBLEXICON_CHARACTERS = new HashSet<>();
    private static final Set<String> MINING_LEVEL_UP_CHARACTERS = new HashSet<>();
    private static final Set<String> AIRSHIP_CHARACTERS = new HashSet<>();
    
    // Combined set for checking (will be updated based on config)
    private static final Set<String> BLOCKED_CHARACTERS = new HashSet<>();
    
    // Helper method to add individual characters from a string to a specific set
    private static void addCharactersFromString(String charString, Set<String> targetSet) {
        for (int i = 0; i < charString.length(); i++) {
            String singleChar = String.valueOf(charString.charAt(i));
            targetSet.add(singleChar);
        }
    }
    
    // Helper method to update the combined blocked characters set based on config
    private static void updateBlockedCharacters() {
        BLOCKED_CHARACTERS.clear();
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().epicDropsBlockingEnabled) {
            BLOCKED_CHARACTERS.addAll(EPIC_DROPS_CHARACTERS);
        }
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().legendaryDropsBlockingEnabled) {
            BLOCKED_CHARACTERS.addAll(LEGENDARY_DROPS_CHARACTERS);
        }
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().loggingLevelUpBlockingEnabled) {
            BLOCKED_CHARACTERS.addAll(LOGGING_LEVEL_UP_CHARACTERS);
        }
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().moblexiconBlockingEnabled) {
            BLOCKED_CHARACTERS.addAll(MOBLEXICON_CHARACTERS);
        }
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().miningLevelUpBlockingEnabled) {
            BLOCKED_CHARACTERS.addAll(MINING_LEVEL_UP_CHARACTERS);
        }
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().airshipBlockingEnabled) {
            BLOCKED_CHARACTERS.addAll(AIRSHIP_CHARACTERS);
        }
    }
    
    static {
        // Lade Zeichen aus ZeichenUtility
        // Epic drops
        addCharactersFromString(ZeichenUtility.getEpicDrops(), EPIC_DROPS_CHARACTERS);
        
        // Legendary drops
        addCharactersFromString(ZeichenUtility.getLegendaryDrops(), LEGENDARY_DROPS_CHARACTERS);
        
        // Logging level up
        addCharactersFromString(ZeichenUtility.getLoggingLevelUp(), LOGGING_LEVEL_UP_CHARACTERS);
        
        // Moblexicon
        addCharactersFromString(ZeichenUtility.getMoblexiconAnimation(), MOBLEXICON_CHARACTERS);
        
        // Mining level up
        addCharactersFromString(ZeichenUtility.getMiningLevelUp(), MINING_LEVEL_UP_CHARACTERS);
        
        // Airship
        addCharactersFromString(ZeichenUtility.getAirship(), AIRSHIP_CHARACTERS);
        
        // Update the combined set based on config
        updateBlockedCharacters();
    }
    
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        try {
            // Client-seitige Events registrieren
            ClientTickEvents.END_CLIENT_TICK.register(AnimationBlockerUtility::onClientTick);
            
            // Registriere Message-Event Handler für alle Nachrichten
            ClientReceiveMessageEvents.GAME.register(AnimationBlockerUtility::onGameMessage);
            
            isInitialized = true;
        } catch (Exception e) {
            // Error initializing AnimationBlockerUtility
        }
    }
    
    private static void onGameMessage(Text message, boolean overlay) {
        if (!animationBlockingEnabled) {
            return;
        }
        
        if (message == null) {
            return;
        }
        
        String messageString = message.getString();
        
        if (messageString.trim().isEmpty()) {
            return;
        }
        
        // Check if message contains blocked characters
        boolean foundBlockedChar = false;
        
        // Check each character in the message against blocked characters
        for (int i = 0; i < messageString.length(); i++) {
            String messageChar = String.valueOf(messageString.charAt(i));
            if (BLOCKED_CHARACTERS.contains(messageChar)) {
                foundBlockedChar = true;
                break;
            }
        }
        
        if (foundBlockedChar) {
            // Block the message
            return;
        }
    }
    
    private static void onClientTick(MinecraftClient client) {
        // Update blocked characters if config changed
        updateBlockedCharacters();
        
        // Prüfe Konfiguration und setze animationBlockingEnabled entsprechend
        if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod ||
            !CCLiveUtilitiesConfig.HANDLER.instance().animationBlockerEnabled) {
            animationBlockingEnabled = false;
            return;
        }
        
        if (client.player == null || client.world == null) {
            animationBlockingEnabled = false;
            return;
        }
        
        // Enable animation blocking if all conditions are met
        animationBlockingEnabled = true;
    }
    

    
    public static void reset() {
        animationBlockingEnabled = false;
    }
    
    public static boolean isAnimationBlockingEnabled() {
        return animationBlockingEnabled;
    }
    
    // Method to get all blocked characters
    public static Set<String> getBlockedCharacters() {
        return new HashSet<>(BLOCKED_CHARACTERS);
    }
} 
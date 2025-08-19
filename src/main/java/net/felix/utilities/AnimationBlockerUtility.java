package net.felix.utilities;

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
        // Epic drops
        addCharactersFromString("㪍㪎㪏㪐㪑㪒㪓㪔", EPIC_DROPS_CHARACTERS);
        addCharactersFromString("㪕㪖㪗㪘㪙㪚㪛㪜", EPIC_DROPS_CHARACTERS);
        addCharactersFromString("㪝㪞㪟㪠㪡㪢㪣㪤", EPIC_DROPS_CHARACTERS);
        addCharactersFromString("㪥㪦㪧㪨㪩㪪㪫㪬", EPIC_DROPS_CHARACTERS);
        addCharactersFromString("㪭㪮㪯㪰㪱㪲㪳㪴", EPIC_DROPS_CHARACTERS);
        
        // Legendary drops
        addCharactersFromString("㩥㩦㩧㩨㩩㩪㩫㩬", LEGENDARY_DROPS_CHARACTERS);
        addCharactersFromString("㩭㩮㩯㩰㩱㩲㩳㩴", LEGENDARY_DROPS_CHARACTERS);
        addCharactersFromString("㩵㩶㩷㩸㩹㩺㩻㩼", LEGENDARY_DROPS_CHARACTERS);
        addCharactersFromString("㩽㩾㩿㪀㪁㪂㪃㪄", LEGENDARY_DROPS_CHARACTERS);
        addCharactersFromString("㪅㪆㪇㪈㪉㪊㪋㪌", LEGENDARY_DROPS_CHARACTERS);
        
        // Logging level up
        addCharactersFromString("㫄㫅㫆㫇", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫈㫉㫊㫋", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫌㫍㫎㫏", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫐㫑㫒㫓", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫔㫕㫖㫗", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫘㫙㫚㫛", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫜㫝㫞㫟", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫠㫡㫢㫣", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫤㫥㫦㫧", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫨㫩㫪㫫", LOGGING_LEVEL_UP_CHARACTERS);
        
        // Moblexicon
        addCharactersFromString("㔦㔠", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬫㬬㬭㬮㬯㬰", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬱㬲㬳㬴㬵㬶", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬷㬸㬹㬺㬻㬼", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬽㬾㬿㭀㭁㭂", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㭃㭄㭅㭆㭇㭈", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㭉㭊㭋㭌㭍㭎", MOBLEXICON_CHARACTERS);
        
        // Mining level up
        addCharactersFromString("㫻㫼㫽㫾", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫿㬀㬁㬂", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㬃㬄㬅㬆", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㬇㬈㬉㬊", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㬋㬌㬍㬎", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㬏㬐㬑㬒", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㬓㬔㬕㬖", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㬗㬘㬙㬚", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㬛㬜㬝㬞", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㬟㬠㬡㬢", MINING_LEVEL_UP_CHARACTERS);
        
        // Airship
        addCharactersFromString("㭩㭪㭫㭬㭭㭮㭯", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭰㭱㭲㭳㭴㭵㭶", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭷㭸㭹㭺㭻㭼㭽", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭾㭿㮀㮁㮂㮃㮄", AIRSHIP_CHARACTERS);
        addCharactersFromString("㮅㮆㮇㮈㮉㮊㮋", AIRSHIP_CHARACTERS);
        addCharactersFromString("㮌㮍㮎㮏㮐㮑㮒", AIRSHIP_CHARACTERS);
        addCharactersFromString("㮓㮔㮕㮖㮗㮘㮙", AIRSHIP_CHARACTERS);
        addCharactersFromString("㮚㮛㮜㮝㮞㮟㮠", AIRSHIP_CHARACTERS);
        

        
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
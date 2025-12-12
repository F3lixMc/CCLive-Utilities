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
        // Epic drops
        addCharactersFromString("㩬㩭㩮㩯㩰㩱㩲㩳", EPIC_DROPS_CHARACTERS);
        addCharactersFromString("㩴㩵㩶㩷㩸㩹㩺㩻", EPIC_DROPS_CHARACTERS);
        addCharactersFromString("㩼㩽㩾㩿㪀㪁㪂㪃", EPIC_DROPS_CHARACTERS);
        addCharactersFromString("㪄㪅㪆㪇㪈㪉㪊㪋", EPIC_DROPS_CHARACTERS);
        addCharactersFromString("㪌㪍㪎㪏㪐㪑㪒㪓", EPIC_DROPS_CHARACTERS);
        
        // Legendary drops
        addCharactersFromString("㩄㩅㩆㩇㩈㩉㩊㩋", LEGENDARY_DROPS_CHARACTERS);
        addCharactersFromString("㩌㩍㩎㩏㩐㩑㩒㩓", LEGENDARY_DROPS_CHARACTERS);
        addCharactersFromString("㩔㩕㩖㩗㩘㩙㩚㩛", LEGENDARY_DROPS_CHARACTERS);
        addCharactersFromString("㩜㩝㩞㩟㩠㩡㩢㩣", LEGENDARY_DROPS_CHARACTERS);
        addCharactersFromString("㩤㩥㩦㩧㩨㩩㩪㩫", LEGENDARY_DROPS_CHARACTERS);
        
        // Logging level up
        addCharactersFromString("㪣㪤㪥㪦", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪧㪨㪩㪪", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪫㪬㪭㪮", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪯㪰㪱㪲", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪳㪴㪵㪶", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪷㪸㪹㪺", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪻㪼㪽㪾", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪿㫀㫁㫂", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫃㫄㫅㫆", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫇㫈㫉㫊", LOGGING_LEVEL_UP_CHARACTERS);
        
        // Moblexicon
        addCharactersFromString("㬋㬌㬍㬎㬏㬐", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬑㬒㬓㬔㬕㬖", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬗㬘㬙㬚㬛㬜", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬝㬞㬟㬠㬡㬢", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬣㬤㬥㬦㬧㬨", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬩㬪㬫㬬㬭㬮", MOBLEXICON_CHARACTERS);
        
        
        // Mining level up
        addCharactersFromString("㫚㫛㫜㫝", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫞㫟㫠㫡", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫢㫣㫤㫥", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫦㫧㫨㫩", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫪㫫㫬㫭", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫮㫯㫰㫱", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫲㫳㫴㫵", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫶㫷㫸㫹", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫺㫻㫼㫽", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫾㫿㬀㬁", MINING_LEVEL_UP_CHARACTERS);
        
        // Airship
        addCharactersFromString("㭉㭊㭋㭌㭍㭎㭏", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭐㭑㭒㭓㭔㭕㭖", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭗㭘㭙㭚㭛㭜㭝", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭞㭟㭠㭡㭢㭣㭤", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭥㭦㭧㭨㭩㭪㭫", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭬㭭㭮㭯㭰㭱㭲", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭳㭴㭵㭶㭷㭸㭹", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭺㭻㭼㭽㭾㭿㮀", AIRSHIP_CHARACTERS);
        

        
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
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
        addCharactersFromString("㩫㩬㩭㩮㩯㩰㩱㩲", EPIC_DROPS_CHARACTERS);
        addCharactersFromString("㩳㩴㩵㩶㩷㩸㩹㩺", EPIC_DROPS_CHARACTERS);
        addCharactersFromString("㩻㩼㩽㩾㩿㪀㪁㪂", EPIC_DROPS_CHARACTERS);
        addCharactersFromString("㩛㩜㩝㩞㩟㩠㩡㩢", EPIC_DROPS_CHARACTERS);
        addCharactersFromString("㩣㩤㩥㩦㩧㩨㩩㩪", EPIC_DROPS_CHARACTERS);
        
        // Legendary drops
        addCharactersFromString("㩃㩄㩅㩆㩇㩈㩉㩊", LEGENDARY_DROPS_CHARACTERS);
        addCharactersFromString("㩋㩌㩍㩎㩏㩐㩑㩒", LEGENDARY_DROPS_CHARACTERS);
        addCharactersFromString("㩓㩔㩕㩖㩗㩘㩙㩚", LEGENDARY_DROPS_CHARACTERS);
        addCharactersFromString("㩽㩾㩿㪀㪁㪂㪃㪄", LEGENDARY_DROPS_CHARACTERS);
        addCharactersFromString("㪅㪆㪇㪈㪉㪊㪋㪌", LEGENDARY_DROPS_CHARACTERS);
        
        // Logging level up
        addCharactersFromString("㪢㪣㪤㪥", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪦㪧㪨㪩", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪪㪫㪬㪭", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪮㪯㪰㪱", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪲㪳㪴㪵", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪶㪷㪸㪹", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪺㪻㪼㪽", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㪾㪿㫀㫁", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫂㫃㫄㫅", LOGGING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫆㫇㫈㫉", LOGGING_LEVEL_UP_CHARACTERS);
        
        // Moblexicon
        addCharactersFromString("㔄㓾", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬊㬋㬌㬍㬎㬏", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬐㬑㬒㬓㬔㬕", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬖㬗㬘㬙㬚㬛", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬜㬝㬞㬟㬠㬡", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬢㬣㬤㬥㬦㬧", MOBLEXICON_CHARACTERS);
        addCharactersFromString("㬨㬩㬪㬫㬬㬭", MOBLEXICON_CHARACTERS);
        
        // Mining level up
        addCharactersFromString("㫙㫚㫛㫜", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫝㫞㫟㫠", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫡㫢㫣㫤", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫥㫦㫧㫨", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫩㫪㫫㫬", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫭㫮㫯㫰", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫱㫲㫳㫴", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫵㫶㫷㫸", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫹㫺㫻㫼", MINING_LEVEL_UP_CHARACTERS);
        addCharactersFromString("㫽㫾㫿㬀", MINING_LEVEL_UP_CHARACTERS);
        
        // Airship
        addCharactersFromString("㭈㭉㭊㭋㭌㭍㭎", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭏㭐㭑㭒㭓㭔㭕", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭖㭗㭘㭙㭚㭛㭜", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭝㭞㭟㭠㭡㭢㭣", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭤㭥㭦㭧㭨㭩㭪", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭫㭬㭭㭮㭯㭰㭱", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭲㭳㭴㭵㭶㭷㭸", AIRSHIP_CHARACTERS);
        addCharactersFromString("㭹㭺㭻㭼㭽㭾㭿", AIRSHIP_CHARACTERS);
        

        
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
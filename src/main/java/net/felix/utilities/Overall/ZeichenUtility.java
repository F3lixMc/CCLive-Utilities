package net.felix.utilities.Overall;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility-Klasse zum Laden und Bereitstellen von chinesischen Zeichen aus zeichen.json
 */
public class ZeichenUtility {
    
    private static final String ZEICHEN_CONFIG_FILE = "assets/cclive-utilities/zeichen.json";
    private static boolean isInitialized = false;
    
    // Zeichen-Speicher
    private static String aincraftBottomFont = "";
    private static Map<Character, Integer> aincraftBottomFontNumbers = new HashMap<>();
    private static String[] equipmentDisplay = new String[4];
    private static String moblexicon = "";
    private static String pixelSpacer = "";
    private static String[] cardsStatues = new String[2];
    private static String friendsRequestAcceptDeny = "";
    private static String hunterUiBackground = "";
    private static String epicDrops = "";
    private static String legendaryDrops = "";
    private static String loggingLevelUp = "";
    private static String moblexiconAnimation = "";
    private static String miningLevelUp = "";
    private static String airship = "";
    private static String essenceHarvesterUi = "";
    
    /**
     * Initialisiert die ZeichenUtility und lädt die Zeichen aus der JSON-Datei
     */
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        try {
            // Lade aus Mod-Ressourcen
            var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
                .orElseThrow(() -> new RuntimeException("Mod container not found"))
                .findPath(ZEICHEN_CONFIG_FILE)
                .orElseThrow(() -> new RuntimeException("Zeichen config file not found"));
            
            try (var inputStream = java.nio.file.Files.newInputStream(resource)) {
                try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    
                    // Lade Aincraft Bottom Font
                    if (json.has("aincraft_bottom_font")) {
                        JsonObject aincraftObj = json.getAsJsonObject("aincraft_bottom_font");
                        aincraftBottomFont = aincraftObj.get("characters").getAsString();
                        
                        // Lade Zahlen-Mapping
                        if (aincraftObj.has("numbers")) {
                            JsonObject numbers = aincraftObj.getAsJsonObject("numbers");
                            for (String key : numbers.keySet()) {
                                String charStr = numbers.get(key).getAsString();
                                if (charStr.length() > 0) {
                                    aincraftBottomFontNumbers.put(charStr.charAt(0), Integer.parseInt(key));
                                }
                            }
                        }
                    }
                    
                    // Lade Equipment Display
                    if (json.has("equipment_display")) {
                        JsonObject equipmentObj = json.getAsJsonObject("equipment_display");
                        var charactersArray = equipmentObj.getAsJsonArray("characters");
                        for (int i = 0; i < charactersArray.size() && i < 4; i++) {
                            equipmentDisplay[i] = charactersArray.get(i).getAsString();
                        }
                    }
                    
                    // Lade Moblexicon
                    if (json.has("moblexicon")) {
                        JsonObject moblexiconObj = json.getAsJsonObject("moblexicon");
                        moblexicon = moblexiconObj.get("character").getAsString();
                    }
                    
                    // Lade Pixel Spacer
                    if (json.has("pixel_spacer")) {
                        JsonObject pixelSpacerObj = json.getAsJsonObject("pixel_spacer");
                        pixelSpacer = pixelSpacerObj.get("characters").getAsString();
                    }
                    
                    // Lade Cards/Statues
                    if (json.has("cards_statues")) {
                        JsonObject cardsStatuesObj = json.getAsJsonObject("cards_statues");
                        var charactersArray = cardsStatuesObj.getAsJsonArray("characters");
                        for (int i = 0; i < charactersArray.size() && i < 2; i++) {
                            cardsStatues[i] = charactersArray.get(i).getAsString();
                        }
                    }
                    
                    // Lade Friends Request Accept Deny
                    if (json.has("friends_request_accept_deny")) {
                        JsonObject friendsObj = json.getAsJsonObject("friends_request_accept_deny");
                        friendsRequestAcceptDeny = friendsObj.get("character").getAsString();
                    }
                    
                    // Lade Hunter UI Background
                    if (json.has("hunter_ui_background")) {
                        JsonObject hunterObj = json.getAsJsonObject("hunter_ui_background");
                        hunterUiBackground = hunterObj.get("character").getAsString();
                    }
                    
                    // Lade Epic Drops
                    if (json.has("epic_drops")) {
                        JsonObject epicDropsObj = json.getAsJsonObject("epic_drops");
                        epicDrops = epicDropsObj.get("characters").getAsString();
                    }
                    
                    // Lade Legendary Drops
                    if (json.has("legendary_drops")) {
                        JsonObject legendaryDropsObj = json.getAsJsonObject("legendary_drops");
                        legendaryDrops = legendaryDropsObj.get("characters").getAsString();
                    }
                    
                    // Lade Logging Level Up
                    if (json.has("logging_level_up")) {
                        JsonObject loggingObj = json.getAsJsonObject("logging_level_up");
                        loggingLevelUp = loggingObj.get("characters").getAsString();
                    }
                    
                    // Lade Moblexicon Animation
                    if (json.has("moblexicon_animation")) {
                        JsonObject moblexiconAnimObj = json.getAsJsonObject("moblexicon_animation");
                        moblexiconAnimation = moblexiconAnimObj.get("characters").getAsString();
                    }
                    
                    // Lade Mining Level Up
                    if (json.has("mining_level_up")) {
                        JsonObject miningObj = json.getAsJsonObject("mining_level_up");
                        miningLevelUp = miningObj.get("characters").getAsString();
                    }
                    
                    // Lade Airship
                    if (json.has("airship")) {
                        JsonObject airshipObj = json.getAsJsonObject("airship");
                        airship = airshipObj.get("characters").getAsString();
                    }
                    
                    // Lade Essence Harvester UI
                    if (json.has("essence_harvester_ui")) {
                        JsonObject essenceHarvesterObj = json.getAsJsonObject("essence_harvester_ui");
                        essenceHarvesterUi = essenceHarvesterObj.get("character").getAsString();
                    }
                    
                    isInitialized = true;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load zeichen.json: " + e.getMessage());
            e.printStackTrace();
            // Fallback zu hardcodierten Werten
            initializeFallback();
        }
    }
    
    /**
     * Fallback-Initialisierung mit hardcodierten Werten
     */
    private static void initializeFallback() {
        aincraftBottomFont = "㚏㚐㚑㚒㚓㚔㚕㚖㚗㚘";
        aincraftBottomFontNumbers.put('㚏', 0);
        aincraftBottomFontNumbers.put('㚐', 1);
        aincraftBottomFontNumbers.put('㚑', 2);
        aincraftBottomFontNumbers.put('㚒', 3);
        aincraftBottomFontNumbers.put('㚓', 4);
        aincraftBottomFontNumbers.put('㚔', 5);
        aincraftBottomFontNumbers.put('㚕', 6);
        aincraftBottomFontNumbers.put('㚖', 7);
        aincraftBottomFontNumbers.put('㚗', 8);
        aincraftBottomFontNumbers.put('㚘', 9);
        
        equipmentDisplay[0] = "㬄";
        equipmentDisplay[1] = "㬅";
        equipmentDisplay[2] = "㬆";
        equipmentDisplay[3] = "㬇";
        
        moblexicon = "㬊";
        pixelSpacer = "㓾㓿㔀㔁㔂㔃㔄㔅㔆㔇㔈㔉㔊㔋㔌㔍㔎㔏㔐㔑㔒㔓㔔㔕㔖㔗㔘㔙㔚㔛㔜㔝㔞㔟㔠㔡㔢㔤";
        cardsStatues[0] = "㭆";
        cardsStatues[1] = "㭂";
        friendsRequestAcceptDeny = "ぢ";
        hunterUiBackground = "㨷";
        epicDrops = "㩬㩭㩮㩯㩰㩱㩲㩳㩴㩵㩶㩷㩸㩹㩺㩻㩼㩽㩾㩿㪀㪁㪂㪃㪄㪅㪆㪇㪈㪉㪊㪋㪌㪍㪎㪏㪐㪑㪒㪓";
        legendaryDrops = "㩄㩅㩆㩇㩈㩉㩊㩋㩌㩍㩎㩏㩐㩑㩒㩓㩔㩕㩖㩗㩘㩙㩚㩛㩜㩝㩞㩟㩠㩡㩢㩣㩤㩥㩦㩧㩨㩩㩪㩫";
        loggingLevelUp = "㪣㪤㪥㪦㪧㪨㪩㪪㪫㪬㪭㪮㪯㪰㪱㪲㪳㪴㪵㪶㪷㪸㪹㪺㪻㪼㪽㪾㪿㫀㫁㫂㫃㫄㫅㫆㫇㫈㫉㫊";
        moblexiconAnimation = "㬋㬌㬍㬎㬏㬐㬑㬒㬓㬔㬕㬖㬗㬘㬙㬚㬛㬜㬝㬞㬟㬠㬡㬢㬣㬤㬥㬦㬧㬨㬩㬪㬫㬬㬭㬮";
        miningLevelUp = "㫚㫛㫜㫝㫞㫟㫠㫡㫢㫣㫤㫥㫦㫧㫨㫩㫪㫫㫬㫭㫮㫯㫰㫱㫲㫳㫴㫵㫶㫷㫸㫹㫺㫻㫼㫽㫾㫿㬀㬁";
        airship = "㭉㭊㭋㭌㭍㭎㭏㭐㭑㭒㭓㭔㭕㭖㭗㭘㭙㭚㭛㭜㭝㭞㭟㭠㭡㭢㭣㭤㭥㭦㭧㭨㭩㭪㭫㭬㭭㭮㭯㭰㭱㭲㭳㭴㭵㭶㭷㭸㭹㭺㭻㭼㭽㭾㭿㮀";
        essenceHarvesterUi = "㮌";
        
        isInitialized = true;
    }
    
    // Getter-Methoden
    
    /**
     * Gibt alle Aincraft Bottom Font Zeichen als String zurück
     */
    public static String getAincraftBottomFont() {
        ensureInitialized();
        return aincraftBottomFont;
    }
    
    /**
     * Gibt die Map der Aincraft Bottom Font Zahlen zurück
     */
    public static Map<Character, Integer> getAincraftBottomFontNumbers() {
        ensureInitialized();
        return new HashMap<>(aincraftBottomFontNumbers);
    }
    
    /**
     * Gibt die Equipment Display Zeichen als Array zurück
     */
    public static String[] getEquipmentDisplay() {
        ensureInitialized();
        return equipmentDisplay.clone();
    }
    
    /**
     * Prüft ob ein String eines der Equipment Display Zeichen enthält
     */
    public static boolean containsEquipmentDisplay(String text) {
        ensureInitialized();
        if (text == null) return false;
        for (String character : equipmentDisplay) {
            if (character != null && text.contains(character)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gibt das Moblexicon Zeichen zurück
     */
    public static String getMoblexicon() {
        ensureInitialized();
        return moblexicon;
    }
    
    /**
     * Prüft ob ein String das Moblexicon Zeichen enthält
     */
    public static boolean containsMoblexicon(String text) {
        ensureInitialized();
        return text != null && text.contains(moblexicon);
    }
    
    /**
     * Gibt alle Pixel Spacer Zeichen als String zurück
     */
    public static String getPixelSpacer() {
        ensureInitialized();
        return pixelSpacer;
    }
    
    /**
     * Prüft ob ein String Pixel Spacer Zeichen enthält
     */
    public static boolean containsPixelSpacer(String text) {
        ensureInitialized();
        if (text == null || pixelSpacer == null) return false;
        return text.matches(".*[" + pixelSpacer + "].*");
    }
    
    /**
     * Gibt die Cards/Statues Zeichen als Array zurück
     */
    public static String[] getCardsStatues() {
        ensureInitialized();
        return cardsStatues.clone();
    }
    
    /**
     * Prüft ob ein String eines der Cards/Statues Zeichen enthält
     */
    public static boolean containsCardsStatues(String text) {
        ensureInitialized();
        if (text == null) return false;
        for (String character : cardsStatues) {
            if (character != null && text.contains(character)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gibt das Friends Request Accept Deny Zeichen zurück
     */
    public static String getFriendsRequestAcceptDeny() {
        ensureInitialized();
        return friendsRequestAcceptDeny;
    }
    
    /**
     * Prüft ob ein String das Friends Request Accept Deny Zeichen enthält
     */
    public static boolean containsFriendsRequestAcceptDeny(String text) {
        ensureInitialized();
        return text != null && text.contains(friendsRequestAcceptDeny);
    }
    
    /**
     * Gibt das Hunter UI Background Zeichen zurück
     */
    public static String getHunterUiBackground() {
        ensureInitialized();
        return hunterUiBackground;
    }
    
    /**
     * Prüft ob ein String das Hunter UI Background Zeichen enthält
     */
    public static boolean containsHunterUiBackground(String text) {
        ensureInitialized();
        return text != null && text.contains(hunterUiBackground);
    }
    
    /**
     * Gibt alle Epic Drops Zeichen als String zurück
     */
    public static String getEpicDrops() {
        ensureInitialized();
        return epicDrops;
    }
    
    /**
     * Gibt alle Legendary Drops Zeichen als String zurück
     */
    public static String getLegendaryDrops() {
        ensureInitialized();
        return legendaryDrops;
    }
    
    /**
     * Gibt alle Logging Level Up Zeichen als String zurück
     */
    public static String getLoggingLevelUp() {
        ensureInitialized();
        return loggingLevelUp;
    }
    
    /**
     * Gibt alle Moblexicon Animation Zeichen als String zurück
     */
    public static String getMoblexiconAnimation() {
        ensureInitialized();
        return moblexiconAnimation;
    }
    
    /**
     * Gibt alle Mining Level Up Zeichen als String zurück
     */
    public static String getMiningLevelUp() {
        ensureInitialized();
        return miningLevelUp;
    }
    
    /**
     * Gibt alle Airship Zeichen als String zurück
     */
    public static String getAirship() {
        ensureInitialized();
        return airship;
    }
    
    /**
     * Gibt das Essence Harvester UI Zeichen zurück
     */
    public static String getEssenceHarvesterUi() {
        ensureInitialized();
        return essenceHarvesterUi;
    }
    
    /**
     * Prüft ob ein String das Essence Harvester UI Zeichen enthält
     */
    public static boolean containsEssenceHarvesterUi(String text) {
        ensureInitialized();
        return text != null && text.contains(essenceHarvesterUi);
    }
    
    /**
     * Prüft ob die Utility initialisiert wurde und initialisiert sie falls nötig
     */
    private static void ensureInitialized() {
        if (!isInitialized) {
            initialize();
        }
    }
}


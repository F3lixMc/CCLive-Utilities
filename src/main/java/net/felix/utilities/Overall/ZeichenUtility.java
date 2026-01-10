package net.felix.utilities.Overall;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.felix.CCLiveUtilities;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.leaderboards.LeaderboardManager;
import net.felix.leaderboards.config.LeaderboardConfig;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Utility-Klasse zum Laden und Bereitstellen von chinesischen Zeichen aus zeichen.json
 * Unterstützt dynamisches Laden vom Server
 */
public class ZeichenUtility {
    
    private static final String ZEICHEN_CONFIG_FILE = "assets/cclive-utilities/zeichen.json";
    private static final String LOCAL_ZEICHEN_FILE = "zeichen.json";
    private static boolean isInitialized = false;
    private static boolean serverCheckRegistered = false;
    
    // Zeichen-Speicher
    private static String aincraftBottomFont = "";
    private static Map<Character, Integer> aincraftBottomFontNumbers = new HashMap<>();
    private static String factoryBottomFont = "";
    private static Map<Character, Integer> factoryBottomFontNumbers = new HashMap<>();
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
    private static String uiCategoriesFactory = "";
    private static String uiCategoriesAincrad = "";
    private static String uiCategoriesFarmzone = "";
    private static String uiCategoriesHub = "";
    private static String uiCategoriesClassSelection = "";
    private static String uiRessourceBag = "";
    private static Map<Character, String> factoryFontFirstLine = new HashMap<>();
    private static Map<Character, String> aincraftFontFirstLine = new HashMap<>();
    
    /**
     * Initialisiert die ZeichenUtility und lädt die Zeichen aus der JSON-Datei
     * Versucht zuerst die lokale Datei, dann Mod-Ressourcen
     */
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        // Registriere Server-Join-Event für Version-Check (nur einmal)
        if (!serverCheckRegistered) {
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                checkAndUpdateZeichenConfig();
            });
            serverCheckRegistered = true;
        }
        
        // Versuche zuerst lokale Datei zu laden
        Path localFile = getLocalZeichenPath();
        if (localFile != null && Files.exists(localFile)) {
            try {
                loadFromFile(localFile);
                isInitialized = true;
                return;
            } catch (Exception e) {
                // Silent error handling("Failed to load local zeichen.json: " + e.getMessage());
            }
        }
        
        // Fallback: Lade aus Mod-Ressourcen
        try {
            var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
                .orElseThrow(() -> new RuntimeException("Mod container not found"))
                .findPath(ZEICHEN_CONFIG_FILE)
                .orElseThrow(() -> new RuntimeException("Zeichen config file not found"));
            
            loadFromFile(resource);
            isInitialized = true;
        } catch (Exception e) {
            // Silent error handling("Failed to load zeichen.json: " + e.getMessage());
            // Silent error handling
            // Fallback zu hardcodierten Werten
            initializeFallback();
        }
    }
    
    /**
     * Prüft die Server-Version und lädt ggf. eine neue Datei
     */
    private static void checkAndUpdateZeichenConfig() {
        CompletableFuture.runAsync(() -> {
            try {
                LeaderboardManager manager = LeaderboardManager.getInstance();
                if (manager == null || !manager.isEnabled() || !manager.isRegistered()) {
                    return; // Leaderboard-System nicht aktiv, kein Server-Check
                }
                
                LeaderboardConfig config = new LeaderboardConfig();
                String serverUrl = config.getServerUrl();
                
                // Prüfe Server-Version
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest versionRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(serverUrl + "/zeichen/version"))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .header("User-Agent", "CCLive-Utilities/1.0")
                    .GET()
                    .build();
                
                java.net.http.HttpResponse<String> versionResponse = httpClient.send(versionRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
                
                if (versionResponse.statusCode() == 200) {
                    JsonObject versionJson = JsonParser.parseString(versionResponse.body()).getAsJsonObject();
                    int serverVersion = versionJson.has("version") ? versionJson.get("version").getAsInt() : 0;
                    int localVersion = CCLiveUtilitiesConfig.HANDLER.instance().zeichenConfigVersion;
                    
                    if (serverVersion > localVersion) {
                        // Neue Version verfügbar, lade Datei
                        java.net.http.HttpRequest dataRequest = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create(serverUrl + "/zeichen/data"))
                            .timeout(java.time.Duration.ofSeconds(10))
                            .header("User-Agent", "CCLive-Utilities/1.0")
                            .GET()
                            .build();
                        
                        java.net.http.HttpResponse<String> dataResponse = httpClient.send(dataRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
                        
                        if (dataResponse.statusCode() == 200) {
                            JsonObject dataJson = JsonParser.parseString(dataResponse.body()).getAsJsonObject();
                            String jsonData = dataJson.has("data") ? dataJson.get("data").getAsString() : null;
                            
                            if (jsonData != null) {
                                // Validiere JSON
                                JsonParser.parseString(jsonData);
                                
                                // Speichere lokal
                                Path localFile = getLocalZeichenPath();
                                if (localFile != null) {
                                    Files.createDirectories(localFile.getParent());
                                    try (OutputStreamWriter writer = new OutputStreamWriter(
                                            Files.newOutputStream(localFile), StandardCharsets.UTF_8)) {
                                        writer.write(jsonData);
                                    }
                                    
                                    // Aktualisiere Version in Config
                                    CCLiveUtilitiesConfig.HANDLER.instance().zeichenConfigVersion = serverVersion;
                                    CCLiveUtilitiesConfig.HANDLER.save();
                                    
                                    // Lade die neue Datei
                                    isInitialized = false;
                                    initialize();
                                    
                                    // Silent error handling("✅ Zeichen-Config aktualisiert (Version " + serverVersion + ")");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Fehler beim Server-Check sind nicht kritisch
                // Silent error handling("⚠️ Fehler beim Prüfen der Zeichen-Config-Version: " + e.getMessage());
            }
        });
    }
    
    /**
     * Gibt den Pfad zur lokalen zeichen.json zurück
     */
    private static Path getLocalZeichenPath() {
        try {
            Path configDir = CCLiveUtilities.getConfigDir();
            Path modConfigDir = configDir.resolve("cclive-utilities");
            return modConfigDir.resolve(LOCAL_ZEICHEN_FILE);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Lädt Zeichen aus einer Datei (lokal oder Mod-Ressource)
     */
    private static void loadFromFile(Path file) throws Exception {
        try (var inputStream = Files.newInputStream(file)) {
            try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                parseJsonObject(json);
            }
        }
    }
    
    /**
     * Parst ein JSON-Objekt und lädt alle Zeichen
     */
    private static void parseJsonObject(JsonObject json) {
        // Lade Aincraft Bottom Font
        if (json.has("aincraft_bottom_font")) {
            JsonObject aincraftObj = json.getAsJsonObject("aincraft_bottom_font");
            aincraftBottomFont = aincraftObj.get("characters").getAsString();
            
            // Lade Zahlen-Mapping
            if (aincraftObj.has("numbers")) {
                aincraftBottomFontNumbers.clear();
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
        
        // Lade UI Categories (Factory, Aincrad, Farmzone, Hub)
        if (json.has("ui_categories_factory")) {
            JsonObject factoryObj = json.getAsJsonObject("ui_categories_factory");
            uiCategoriesFactory = factoryObj.get("character").getAsString();
        }
        if (json.has("ui_categories_aincrad")) {
            JsonObject aincradObj = json.getAsJsonObject("ui_categories_aincrad");
            uiCategoriesAincrad = aincradObj.get("character").getAsString();
        }
        if (json.has("ui_categories_farmzone")) {
            JsonObject farmzoneObj = json.getAsJsonObject("ui_categories_farmzone");
            uiCategoriesFarmzone = farmzoneObj.get("character").getAsString();
        }
        if (json.has("ui_categories_hub")) {
            JsonObject hubObj = json.getAsJsonObject("ui_categories_hub");
            uiCategoriesHub = hubObj.get("character").getAsString();
        }
        if (json.has("ui_categories_class_selection")) {
            JsonObject classSelectionObj = json.getAsJsonObject("ui_categories_class_selection");
            uiCategoriesClassSelection = classSelectionObj.get("character").getAsString();
        }
        
        // Lade UI Ressource Bag
        if (json.has("ui_ressource_bag")) {
            JsonObject ressourceBagObj = json.getAsJsonObject("ui_ressource_bag");
            uiRessourceBag = ressourceBagObj.get("character").getAsString();
        }
        
        // Lade Factory Bottom Font
        if (json.has("factory_bottom_font")) {
            JsonObject factoryObj = json.getAsJsonObject("factory_bottom_font");
            factoryBottomFont = factoryObj.get("characters").getAsString();
            
            // Lade Zahlen-Mapping
            if (factoryObj.has("numbers")) {
                factoryBottomFontNumbers.clear();
                JsonObject numbers = factoryObj.getAsJsonObject("numbers");
                for (String key : numbers.keySet()) {
                    String charStr = numbers.get(key).getAsString();
                    if (charStr.length() > 0) {
                        factoryBottomFontNumbers.put(charStr.charAt(0), Integer.parseInt(key));
                    }
                }
            }
        }
        
        // Lade Factory Font First Line
        if (json.has("factory_font_first_line")) {
            JsonObject factoryFontObj = json.getAsJsonObject("factory_font_first_line");
            
            // Lade Mapping
            if (factoryFontObj.has("mapping")) {
                factoryFontFirstLine.clear();
                JsonObject mapping = factoryFontObj.getAsJsonObject("mapping");
                for (String key : mapping.keySet()) {
                    String charStr = mapping.get(key).getAsString();
                    if (charStr.length() > 0) {
                        factoryFontFirstLine.put(charStr.charAt(0), key);
                    }
                }
            }
        }
        
        // Lade Aincraft Font First Line
        if (json.has("aincraft_font_first_line")) {
            JsonObject aincraftFontObj = json.getAsJsonObject("aincraft_font_first_line");
            
            // Lade Mapping
            if (aincraftFontObj.has("mapping")) {
                aincraftFontFirstLine.clear();
                JsonObject mapping = aincraftFontObj.getAsJsonObject("mapping");
                for (String key : mapping.keySet()) {
                    String charStr = mapping.get(key).getAsString();
                    if (charStr.length() > 0) {
                        aincraftFontFirstLine.put(charStr.charAt(0), key);
                    }
                }
            }
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
        uiCategoriesFactory = "㮕";
        uiCategoriesAincrad = "㮖";
        uiCategoriesFarmzone = "㮗";
        uiCategoriesHub = "㮘";
        uiCategoriesClassSelection = "㬈";
        uiRessourceBag = "Ⳅ";
        
        factoryBottomFont = "㝡㝢㝣㝤㝥㝦㝧㝨㝩㝪";
        factoryBottomFontNumbers.put('㝡', 0);
        factoryBottomFontNumbers.put('㝢', 1);
        factoryBottomFontNumbers.put('㝣', 2);
        factoryBottomFontNumbers.put('㝤', 3);
        factoryBottomFontNumbers.put('㝥', 4);
        factoryBottomFontNumbers.put('㝦', 5);
        factoryBottomFontNumbers.put('㝧', 6);
        factoryBottomFontNumbers.put('㝨', 7);
        factoryBottomFontNumbers.put('㝩', 8);
        factoryBottomFontNumbers.put('㝪', 9);
        
        // Factory Font First Line Fallback
        factoryFontFirstLine.put('㜌', "A");
        factoryFontFirstLine.put('㜍', "B");
        factoryFontFirstLine.put('㜎', "C");
        factoryFontFirstLine.put('㜏', "D");
        factoryFontFirstLine.put('㜐', "E");
        factoryFontFirstLine.put('㜑', "F");
        factoryFontFirstLine.put('㜒', "G");
        factoryFontFirstLine.put('㜓', "H");
        factoryFontFirstLine.put('㜔', "I");
        factoryFontFirstLine.put('㜕', "J");
        factoryFontFirstLine.put('㜖', "K");
        factoryFontFirstLine.put('㜗', "L");
        factoryFontFirstLine.put('㜘', "M");
        factoryFontFirstLine.put('㜙', "N");
        factoryFontFirstLine.put('㜚', "O");
        factoryFontFirstLine.put('㜛', "P");
        factoryFontFirstLine.put('㜜', "Q");
        factoryFontFirstLine.put('㜝', "R");
        factoryFontFirstLine.put('㜞', "S");
        factoryFontFirstLine.put('㜟', "T");
        factoryFontFirstLine.put('㜠', "U");
        factoryFontFirstLine.put('㜡', "V");
        factoryFontFirstLine.put('㜢', "W");
        factoryFontFirstLine.put('㜣', "X");
        factoryFontFirstLine.put('㜤', "Y");
        factoryFontFirstLine.put('㜥', "Z");
        factoryFontFirstLine.put('㜦', "+");
        factoryFontFirstLine.put('㜧', ".");
        factoryFontFirstLine.put('㜨', "[");
        factoryFontFirstLine.put('㜩', "]");
        factoryFontFirstLine.put('㜪', "Ä");
        factoryFontFirstLine.put('㜫', "Ö");
        factoryFontFirstLine.put('㜬', "Ü");
        factoryFontFirstLine.put('㜭', "ß");
        factoryFontFirstLine.put('㜮', "0");
        factoryFontFirstLine.put('㜯', "1");
        factoryFontFirstLine.put('㜰', "2");
        factoryFontFirstLine.put('㜱', "3");
        factoryFontFirstLine.put('㜲', "4");
        factoryFontFirstLine.put('㜳', "5");
        factoryFontFirstLine.put('㜴', "6");
        factoryFontFirstLine.put('㜵', "7");
        factoryFontFirstLine.put('㜶', "8");
        factoryFontFirstLine.put('㜷', "9");
        factoryFontFirstLine.put('㜸', ":");
        factoryFontFirstLine.put('㜹', "-");
        factoryFontFirstLine.put('㜺', "(");
        factoryFontFirstLine.put('㜻', ")");
        factoryFontFirstLine.put('㜼', ",");
        factoryFontFirstLine.put('㜽', "!");
        factoryFontFirstLine.put('㜾', "?");
        
        // Aincraft Font First Line Fallback
        aincraftFontFirstLine.put('㘺', "A");
        aincraftFontFirstLine.put('㘻', "B");
        aincraftFontFirstLine.put('㘼', "C");
        aincraftFontFirstLine.put('㘽', "D");
        aincraftFontFirstLine.put('㘾', "E");
        aincraftFontFirstLine.put('㘿', "F");
        aincraftFontFirstLine.put('㙀', "G");
        aincraftFontFirstLine.put('㙁', "H");
        aincraftFontFirstLine.put('㙂', "I");
        aincraftFontFirstLine.put('㙃', "J");
        aincraftFontFirstLine.put('㙄', "K");
        aincraftFontFirstLine.put('㙅', "L");
        aincraftFontFirstLine.put('㙆', "M");
        aincraftFontFirstLine.put('㙇', "N");
        aincraftFontFirstLine.put('㙈', "O");
        aincraftFontFirstLine.put('㙉', "P");
        aincraftFontFirstLine.put('㙊', "Q");
        aincraftFontFirstLine.put('㙋', "R");
        aincraftFontFirstLine.put('㙌', "S");
        aincraftFontFirstLine.put('㙍', "T");
        aincraftFontFirstLine.put('㙎', "U");
        aincraftFontFirstLine.put('㙏', "V");
        aincraftFontFirstLine.put('㙐', "W");
        aincraftFontFirstLine.put('㙑', "X");
        aincraftFontFirstLine.put('㙒', "Y");
        aincraftFontFirstLine.put('㙓', "Z");
        aincraftFontFirstLine.put('㙔', "+");
        aincraftFontFirstLine.put('㙕', ".");
        aincraftFontFirstLine.put('㙖', "[");
        aincraftFontFirstLine.put('㙗', "]");
        aincraftFontFirstLine.put('㙘', "Ä");
        aincraftFontFirstLine.put('㙙', "Ö");
        aincraftFontFirstLine.put('㙚', "Ü");
        aincraftFontFirstLine.put('㙛', "ß");
        aincraftFontFirstLine.put('㙜', "0");
        aincraftFontFirstLine.put('㙝', "1");
        aincraftFontFirstLine.put('㙞', "2");
        aincraftFontFirstLine.put('㙟', "3");
        aincraftFontFirstLine.put('㙠', "4");
        aincraftFontFirstLine.put('㙡', "5");
        aincraftFontFirstLine.put('㙢', "6");
        aincraftFontFirstLine.put('㙣', "7");
        aincraftFontFirstLine.put('㙤', "8");
        aincraftFontFirstLine.put('㙥', "9");
        aincraftFontFirstLine.put('㙦', ":");
        aincraftFontFirstLine.put('㙧', "-");
        aincraftFontFirstLine.put('㙨', "(");
        aincraftFontFirstLine.put('㙩', ")");
        aincraftFontFirstLine.put('㙪', ",");
        aincraftFontFirstLine.put('㙫', "!");
        aincraftFontFirstLine.put('㙬', "?");
        
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
     * Prüft ob ein String eines der UI Categories Zeichen enthält (Factory, Aincrad, Farmzone, Hub)
     * Diese Menüs sollten keine JEI UI anzeigen
     */
    public static boolean containsSpecialMenusNoJei(String text) {
        ensureInitialized();
        if (text == null) {
            return false;
        }
        // Prüfe ob eines der UI Categories Zeichen im Text enthalten ist
        return (uiCategoriesFactory != null && !uiCategoriesFactory.isEmpty() && text.contains(uiCategoriesFactory)) ||
               (uiCategoriesAincrad != null && !uiCategoriesAincrad.isEmpty() && text.contains(uiCategoriesAincrad)) ||
               (uiCategoriesFarmzone != null && !uiCategoriesFarmzone.isEmpty() && text.contains(uiCategoriesFarmzone)) ||
               (uiCategoriesHub != null && !uiCategoriesHub.isEmpty() && text.contains(uiCategoriesHub)) ||
               (uiCategoriesClassSelection != null && !uiCategoriesClassSelection.isEmpty() && text.contains(uiCategoriesClassSelection));
    }
    
    /**
     * Gibt das UI Ressource Bag Zeichen zurück
     */
    public static String getUiRessourceBag() {
        ensureInitialized();
        return uiRessourceBag;
    }
    
    /**
     * Prüft ob ein String das UI Ressource Bag Zeichen enthält
     */
    public static boolean containsUiRessourceBag(String text) {
        ensureInitialized();
        return text != null && text.contains(uiRessourceBag);
    }
    
    /**
     * Gibt alle Factory Bottom Font Zeichen als String zurück
     */
    public static String getFactoryBottomFont() {
        ensureInitialized();
        return factoryBottomFont;
    }
    
    /**
     * Gibt die Map der Factory Bottom Font Zahlen zurück
     */
    public static Map<Character, Integer> getFactoryBottomFontNumbers() {
        ensureInitialized();
        return new HashMap<>(factoryBottomFontNumbers);
    }
    
    /**
     * Gibt die Map der Factory Font First Line Zeichen zurück
     */
    public static Map<Character, String> getFactoryFontFirstLine() {
        ensureInitialized();
        return new HashMap<>(factoryFontFirstLine);
    }
    
    /**
     * Gibt die Map der Aincraft Font First Line Zeichen zurück
     */
    public static Map<Character, String> getAincraftFontFirstLine() {
        ensureInitialized();
        return new HashMap<>(aincraftFontFirstLine);
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

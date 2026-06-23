package net.felix.utilities.Overall;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.felix.CCLiveUtilities;
import net.felix.utilities.Aincraft.KillsUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static net.felix.utilities.Overall.ZeichenFontLoader.DIGIT_DECODE;
import static net.felix.utilities.Overall.ZeichenFontLoader.STANDARD_LINE_DECODE;
import static net.felix.utilities.Overall.ZeichenFontLoader.buildCodePointDigitMap;
import static net.felix.utilities.Overall.ZeichenFontLoader.buildDigitMap;
import static net.felix.utilities.Overall.ZeichenFontLoader.buildDigitString;
import static net.felix.utilities.Overall.ZeichenFontLoader.buildGlyphToDecodedMap;
import static net.felix.utilities.Overall.ZeichenFontLoader.codePointsToString;
import static net.felix.utilities.Overall.ZeichenFontLoader.collectPixelSpacers;
import static net.felix.utilities.Overall.ZeichenFontLoader.find;
import static net.felix.utilities.Overall.ZeichenFontLoader.indexProviders;
import static net.felix.utilities.Overall.ZeichenFontLoader.singleChar;

/**
 * Lädt Custom-Font-Glyphen aus dem aktiven Resourcepack ({@code minecraft:font/default.json})
 * anhand von {@code group} und {@code name}. Fallback: {@code zeichen.json} oder Hardcoded-Werte.
 */
public class ZeichenUtility {

    /**
     * Zum Testen: kein zeichen.json / Hardcoded-Fallback. Nur Resourcepack-Provider zählen.
     * Nach dem Test wieder auf {@code false} setzen.
     */
    public static final boolean DISABLE_ZEICHEN_FALLBACK_FOR_TESTING = false;

    /** Lade-Status in die Konsole (Start, Join, F3+T). */
    private static final boolean LOG_ZEICHEN_LOAD_STATUS = true;

    private static final String ZEICHEN_CONFIG_FILE = "assets/cclive-utilities/zeichen.json";
    private static final String LOCAL_ZEICHEN_FILE = "zeichen.json";
    private static final Identifier RELOAD_LISTENER_ID = Identifier.of("cclive-utilities", "zeichen_font_reload");
    private static final ZeichenFontLoader.FontProviderKey AINCRAFT_BOTTOM_KEY =
            new ZeichenFontLoader.FontProviderKey("cactusclicker_aincraft_overlay", "font_bottom_line");

    private static boolean isInitialized = false;
    private static boolean loadedFromResourcePack = false;
    private static String lastLoadTrigger = "unbekannt";
    private static boolean reloadListenerRegistered = false;
    private static boolean joinReloadRegistered = false;

    private static String aincraftBottomFont = "";
    private static Map<Character, Integer> aincraftBottomFontNumbers = new HashMap<>();
    private static String factoryBottomFont = "";
    private static Map<Character, Integer> factoryBottomFontNumbers = new HashMap<>();
    private static String[] equipmentDisplay = new String[4];
    private static String moblexicon = "";
    private static String pixelSpacer = "";
    private static String cardsMenu = "";
    private static String statuesMenu = "";
    private static String friendsRequestAcceptDeny = "";
    private static String confirmationUiBackground = "";
    private static String hunterUiBackground = "";
    private static String epicDrops = "";
    private static String legendaryDrops = "";
    private static String loggingLevelUp = "";
    private static String moblexiconAnimation = "";
    private static String miningLevelUp = "";
    private static String fishingLevelUp = "";
    private static String airship = "";
    /** Glyph aus {@code cactusclicker_legend_plus} / {@code ui_background} – Legend+-Inventar (Item-Viewer aus). */
    private static String legendPlusUiBackground = "";
    /** Glyph aus {@code cactusclicker_airship} / {@code main_gui} – Luftschiff-Haupt-GUI (Item-Viewer aus). */
    private static String airshipMainGui = "";
    /** Glyph aus {@code cactusclicker_shop} / {@code ui_background} – Glimfang-Shop (Item-Viewer aus). */
    private static String glimfangShopUi = "";
    /** Glyph aus {@code cactusclicker_anvil} / {@code main_ui} – Amboss-Inventar. */
    private static String anvilMainUi = "";
    /** Glyph aus {@code cactusclicker_furnace} / {@code main_ui} – Schmelzofen-Inventar. */
    private static String furnaceMainUi = "";
    private static String essenceHarvesterUi = "";
    private static String essenceBagUi = "";
    private static String essenceSelectionUi = "";
    private static String uiCategoriesFactory = "";
    private static String uiCategoriesAincrad = "";
    private static String uiCategoriesFarmzone = "";
    private static String uiCategoriesHub = "";
    private static String uiCategoriesClassSelection = "";
    private static String uiRessourceBag = "";
    private static String uiMaterialBag = "";
    private static String fishingComponentsCrafted = "";
    private static String fishingCraftedComponents = "";
    private static String fishingComponentsCraft = "";
    private static String fishingComponentsRecycle = "";
    private static String uiQuickTravelOverworld = "";
    private static String uiQuickTravelMining = "";
    private static String uiQuickTravelOcean = "";
    private static Map<Character, String> factoryFontFirstLine = new HashMap<>();
    private static Map<Character, String> aincraftFontFirstLine = new HashMap<>();
    private static Map<Integer, Integer> npcAlertsKomboKisteBossBarDigitCodePoints = new HashMap<>();

    public static void initialize() {
        if (!reloadListenerRegistered) {
            registerResourceReloadListener();
            reloadListenerRegistered = true;
        }
        if (!joinReloadRegistered) {
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                    reloadFromActiveResourcePack("server-join"));
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                    reloadFromActiveResourcePack("server-disconnect"));
            ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                    reloadFromActiveResourcePack("client-started"));
            joinReloadRegistered = true;
        }
        if (isInitialized) {
            return;
        }
        loadAllSources();
        isInitialized = true;
    }

    /**
     * Lädt Zeichen neu (z. B. nach Resourcepack-Reload).
     */
    public static void reloadFromActiveResourcePack() {
        reloadFromActiveResourcePack("reload");
    }

    public static void reloadFromActiveResourcePack(String trigger) {
        lastLoadTrigger = trigger;
        loadAllSources();
        isInitialized = true;
        notifyDependents();
    }

    private static void loadAllSources() {
        clearAllZeichen();
        if (tryLoadFromResourcePack(lastLoadTrigger)) {
            return;
        }
        loadedFromResourcePack = false;
        if (DISABLE_ZEICHEN_FALLBACK_FOR_TESTING) {
            logStatus("[" + lastLoadTrigger + "] Kein CactusClicker-Font – Zeichen geleert (Fallback aus).");
            return;
        }
        Path localFile = getLocalZeichenPath();
        if (localFile != null && Files.exists(localFile)) {
            try {
                loadFromFile(localFile);
                logStatus("Zeichen aus lokaler zeichen.json geladen.");
                return;
            } catch (Exception ignored) {
            }
        }
        try {
            var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
                    .orElseThrow(() -> new RuntimeException("Mod container not found"))
                    .findPath(ZEICHEN_CONFIG_FILE)
                    .orElseThrow(() -> new RuntimeException("Zeichen config file not found"));
            loadFromFile(resource);
            logStatus("Zeichen aus Mod-zeichen.json geladen.");
        } catch (Exception e) {
            initializeFallback();
            logStatus("Hardcoded-Fallback aktiv.");
        }
    }

    private static boolean tryLoadFromResourcePack(String trigger) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            logStatus("[" + trigger + "] Kein MinecraftClient – Resourcepack noch nicht prüfbar.");
            return false;
        }
        ResourceManager manager = client.getResourceManager();
        if (manager == null) {
            logStatus("[" + trigger + "] Kein ResourceManager.");
            return false;
        }
        boolean defaultJsonPresent = manager.getResource(ZeichenFontLoader.MINECRAFT_DEFAULT_FONT).isPresent();
        Map<ZeichenFontLoader.FontProviderKey, List<Integer>> index = indexProviders(manager);
        int providersWithGroup = index.size();
        if (!index.containsKey(AINCRAFT_BOTTOM_KEY)) {
            logStatus("[" + trigger + "] minecraft:font/default.json "
                    + (defaultJsonPresent ? "vorhanden" : "NICHT gefunden")
                    + ", bitmap-Provider mit group/name: " + providersWithGroup
                    + " – CactusClicker-Provider fehlt (Server-Pack evtl. noch nicht geladen).");
            return false;
        }
        applyFromFontProviderIndex(index);
        loadedFromResourcePack = true;
        logStatus("[" + trigger + "] OK aus Resourcepack – moblexicon='"
                + moblexicon + "', aincraft-Ziffern=" + aincraftBottomFontNumbers.size()
                + ", Provider gesamt: " + providersWithGroup);
        return true;
    }

    private static void logStatus(String message) {
        if (LOG_ZEICHEN_LOAD_STATUS) {
            System.out.println("[CCLive-Utilities/Zeichen] " + message);
        }
    }

    /** Ob die letzte erfolgreiche Ladung aus dem Resourcepack kam. */
    public static boolean isLoadedFromResourcePack() {
        return loadedFromResourcePack;
    }

    private static void registerResourceReloadListener() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return RELOAD_LISTENER_ID;
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        lastLoadTrigger = "resource-reload";
                        // Immer neu laden (leert zuerst den Cache, dann Pack oder Fallback)
                        loadAllSources();
                        isInitialized = true;
                        notifyDependents();
                    }
                }
        );
    }

    /** Setzt alle gecachten Zeichen zurück (z. B. wenn Resourcepack entfernt wurde). */
    private static void clearAllZeichen() {
        aincraftBottomFont = "";
        aincraftBottomFontNumbers = new HashMap<>();
        factoryBottomFont = "";
        factoryBottomFontNumbers = new HashMap<>();
        equipmentDisplay = new String[4];
        moblexicon = "";
        pixelSpacer = "";
        cardsMenu = "";
        statuesMenu = "";
        friendsRequestAcceptDeny = "";
        confirmationUiBackground = "";
        hunterUiBackground = "";
        epicDrops = "";
        legendaryDrops = "";
        loggingLevelUp = "";
        moblexiconAnimation = "";
        miningLevelUp = "";
        fishingLevelUp = "";
        airship = "";
        legendPlusUiBackground = "";
        airshipMainGui = "";
        glimfangShopUi = "";
        anvilMainUi = "";
        furnaceMainUi = "";
        essenceHarvesterUi = "";
        essenceBagUi = "";
        essenceSelectionUi = "";
        uiCategoriesFactory = "";
        uiCategoriesAincrad = "";
        uiCategoriesFarmzone = "";
        uiCategoriesHub = "";
        uiCategoriesClassSelection = "";
        uiRessourceBag = "";
        uiMaterialBag = "";
        fishingComponentsCrafted = "";
        fishingCraftedComponents = "";
        fishingComponentsCraft = "";
        fishingComponentsRecycle = "";
        uiQuickTravelOverworld = "";
        uiQuickTravelMining = "";
        uiQuickTravelOcean = "";
        factoryFontFirstLine = new HashMap<>();
        aincraftFontFirstLine = new HashMap<>();
        npcAlertsKomboKisteBossBarDigitCodePoints = new HashMap<>();
    }

    private static void applyFromFontProviderIndex(Map<ZeichenFontLoader.FontProviderKey, List<Integer>> index) {
        find(index, "cactusclicker_aincraft_overlay", "font_bottom_line").ifPresent(cps -> {
            aincraftBottomFontNumbers = buildDigitMap(cps, STANDARD_LINE_DECODE);
            aincraftBottomFont = buildDigitString(aincraftBottomFontNumbers);
        });

        find(index, "cactusclicker_factory_overlay", "font_bottom_line").ifPresent(cps -> {
            factoryBottomFontNumbers = buildDigitMap(cps, STANDARD_LINE_DECODE);
            factoryBottomFont = buildDigitString(factoryBottomFontNumbers);
        });

        find(index, "combo_chest", "numbers").ifPresent(cps -> {
            npcAlertsKomboKisteBossBarDigitCodePoints = buildCodePointDigitMap(cps, DIGIT_DECODE);
        });
        if (npcAlertsKomboKisteBossBarDigitCodePoints.isEmpty() && !DISABLE_ZEICHEN_FALLBACK_FOR_TESTING) {
            fillDefaultNpcAlertsKomboKisteBossBarDigits();
        }

        equipmentDisplay[0] = singleChar(index, "equipment", "ui_background");
        equipmentDisplay[1] = singleChar(index, "equipment", "ui_open_tool_bag");
        equipmentDisplay[2] = singleChar(index, "equipment", "ui_open_gadget_bag");
        equipmentDisplay[3] = singleChar(index, "equipment", "ui_open_both");

        moblexicon = singleChar(index, "cactusclicker_mob_lexicon", "main_gui");
        pixelSpacer = collectPixelSpacers(index);
        cardsMenu = singleChar(index, "cactusclicker_cards", "ui_background");
        statuesMenu = singleChar(index, "cactusclicker_statues", "ui_background");
        friendsRequestAcceptDeny = singleChar(index, "friends", "friends_request_accept_deny");
        confirmationUiBackground = singleChar(index, "confirmation", "ui_background");
        hunterUiBackground = singleChar(index, "hunter", "ui_background");

        find(index, "cactusclicker_drop_animation", "epic").ifPresent(cps -> epicDrops = codePointsToString(cps));
        find(index, "cactusclicker_drop_animation", "legendary").ifPresent(cps -> legendaryDrops = codePointsToString(cps));
        find(index, "cactusclicker_logging_animation", "logging_level_up").ifPresent(cps -> loggingLevelUp = codePointsToString(cps));
        find(index, "cactusclicker_mining_animation", "mining_level_up").ifPresent(cps -> miningLevelUp = codePointsToString(cps));
        find(index, "cactusclicker_fishing_animation", "level_up").ifPresent(cps -> fishingLevelUp = codePointsToString(cps));
        find(index, "cactusclicker_mob_lexicon", "main_gui_opening_animation").ifPresent(cps -> moblexiconAnimation = codePointsToString(cps));
        find(index, "cactusclicker_airship", "main_gui_opening_animation").ifPresent(cps -> airship = codePointsToString(cps));
        legendPlusUiBackground = singleChar(index, "cactusclicker_legend_plus", "ui_background");
        airshipMainGui = singleChar(index, "cactusclicker_airship", "main_gui");
        glimfangShopUi = singleChar(index, "cactusclicker_shop", "ui_background");
        anvilMainUi = singleChar(index, "cactusclicker_anvil", "main_ui");
        furnaceMainUi = singleChar(index, "cactusclicker_furnace", "main_ui");

        essenceHarvesterUi = singleChar(index, "cactusclicker_harvesters", "essence_harvester_ui");
        essenceBagUi = singleChar(index, "cactusclicker_essence", "ui_bag");
        essenceSelectionUi = singleChar(index, "cactusclicker_essence", "ui_selection");
        uiCategoriesFactory = singleChar(index, "cc_main_menu", "ui_categories_factory");
        uiCategoriesAincrad = singleChar(index, "cc_main_menu", "ui_categories_aincrad");
        uiCategoriesFarmzone = singleChar(index, "cc_main_menu", "ui_categories_farmzone");
        uiCategoriesHub = singleChar(index, "cc_main_menu", "ui_categories_hub");
        uiCategoriesClassSelection = singleChar(index, "class_selection", "ui_background");
        uiRessourceBag = singleChar(index, "cactusclicker_bag_types", "resource_bag_background");
        uiMaterialBag = singleChar(index, "cactusclicker_bag_types", "material_bag_background");

        fishingComponentsCrafted = singleChar(index, "cactusclicker_fishing_equipment", "ui_components_crafted");
        fishingCraftedComponents = singleChar(index, "cactusclicker_fishing_equipment", "ui_crafted_components");
        fishingComponentsCraft = singleChar(index, "cactusclicker_fishing_equipment", "ui_components_craft");
        fishingComponentsRecycle = singleChar(index, "cactusclicker_fishing_equipment", "ui_components_recycle");

        uiQuickTravelOverworld = singleChar(index, "farmzone_quick_travel", "ui_overworld");
        uiQuickTravelMining = singleChar(index, "farmzone_quick_travel", "ui_mining");
        uiQuickTravelOcean = singleChar(index, "farmzone_quick_travel", "ui_ocean");

        find(index, "cactusclicker_aincraft_overlay", "font_first_line").ifPresent(cps ->
                aincraftFontFirstLine = buildGlyphToDecodedMap(cps, STANDARD_LINE_DECODE));
        find(index, "cactusclicker_factory_overlay", "font_first_line").ifPresent(cps ->
                factoryFontFirstLine = buildGlyphToDecodedMap(cps, STANDARD_LINE_DECODE));
    }

    private static void notifyDependents() {
        AnimationBlockerUtility.reloadCharacters();
        KillsUtility.invalidateNumberCache();
    }

    private static Path getLocalZeichenPath() {
        try {
            Path configDir = CCLiveUtilities.getConfigDir();
            Path modConfigDir = configDir.resolve("cclive-utilities");
            return modConfigDir.resolve(LOCAL_ZEICHEN_FILE);
        } catch (Exception e) {
            return null;
        }
    }

    private static void loadFromFile(Path file) throws Exception {
        try (var inputStream = Files.newInputStream(file)) {
            try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (tryApplyDefinitionsFromJson(json)) {
                    return;
                }
                parseLegacyJsonObject(json);
            }
        }
    }

    /**
     * Neue zeichen.json: nur group/name – Auflösung über aktuelles Resourcepack.
     */
    private static boolean tryApplyDefinitionsFromJson(JsonObject json) {
        if (!json.has("aincraft_bottom_font")) {
            return false;
        }
        var first = json.getAsJsonObject("aincraft_bottom_font");
        if (!first.has("group") || !first.has("name")) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getResourceManager() == null) {
            return false;
        }
        Map<ZeichenFontLoader.FontProviderKey, List<Integer>> index = indexProviders(client.getResourceManager());
        if (index.isEmpty()) {
            return false;
        }
        applyFromFontProviderIndex(index);
        return true;
    }

    /** Legacy-Format mit fest eingetragenen Zeichen */
    private static void parseLegacyJsonObject(JsonObject json) {
        if (json.has("aincraft_bottom_font")) {
            JsonObject aincraftObj = json.getAsJsonObject("aincraft_bottom_font");
            if (aincraftObj.has("characters")) {
                aincraftBottomFont = aincraftObj.get("characters").getAsString();
            }
            if (aincraftObj.has("numbers")) {
                aincraftBottomFontNumbers.clear();
                JsonObject numbers = aincraftObj.getAsJsonObject("numbers");
                for (String key : numbers.keySet()) {
                    String charStr = numbers.get(key).getAsString();
                    if (!charStr.isEmpty()) {
                        aincraftBottomFontNumbers.put(charStr.charAt(0), Integer.parseInt(key));
                    }
                }
            }
        }

        if (json.has("equipment_display")) {
            JsonObject equipmentObj = json.getAsJsonObject("equipment_display");
            var charactersArray = equipmentObj.getAsJsonArray("characters");
            for (int i = 0; i < charactersArray.size() && i < 4; i++) {
                equipmentDisplay[i] = charactersArray.get(i).getAsString();
            }
        }

        if (json.has("moblexicon")) {
            moblexicon = json.getAsJsonObject("moblexicon").get("character").getAsString();
        }
        if (json.has("pixel_spacer")) {
            pixelSpacer = json.getAsJsonObject("pixel_spacer").get("characters").getAsString();
        }
        if (json.has("cards")) {
            cardsMenu = json.getAsJsonObject("cards").get("character").getAsString();
        } else if (json.has("cards_statues")) {
            var charactersArray = json.getAsJsonObject("cards_statues").getAsJsonArray("characters");
            if (!charactersArray.isEmpty()) {
                cardsMenu = charactersArray.get(0).getAsString();
            }
        }
        if (json.has("statues")) {
            statuesMenu = json.getAsJsonObject("statues").get("character").getAsString();
        } else if (json.has("cards_statues")) {
            var charactersArray = json.getAsJsonObject("cards_statues").getAsJsonArray("characters");
            if (charactersArray.size() > 1) {
                statuesMenu = charactersArray.get(1).getAsString();
            }
        }
        if (json.has("friends_request_accept_deny")) {
            friendsRequestAcceptDeny = json.getAsJsonObject("friends_request_accept_deny").get("character").getAsString();
        }
        if (json.has("confirmation_ui_background")) {
            JsonObject o = json.getAsJsonObject("confirmation_ui_background");
            if (o.has("character")) {
                confirmationUiBackground = o.get("character").getAsString();
            }
        }
        if (json.has("hunter_ui_background")) {
            hunterUiBackground = json.getAsJsonObject("hunter_ui_background").get("character").getAsString();
        }
        if (json.has("epic_drops")) {
            epicDrops = json.getAsJsonObject("epic_drops").get("characters").getAsString();
        }
        if (json.has("legendary_drops")) {
            legendaryDrops = json.getAsJsonObject("legendary_drops").get("characters").getAsString();
        }
        if (json.has("logging_level_up")) {
            loggingLevelUp = json.getAsJsonObject("logging_level_up").get("characters").getAsString();
        }
        if (json.has("moblexicon_animation")) {
            moblexiconAnimation = json.getAsJsonObject("moblexicon_animation").get("characters").getAsString();
        }
        if (json.has("mining_level_up")) {
            miningLevelUp = json.getAsJsonObject("mining_level_up").get("characters").getAsString();
        }
        if (json.has("fishing_level_up")) {
            fishingLevelUp = json.getAsJsonObject("fishing_level_up").get("characters").getAsString();
        }
        if (json.has("airship")) {
            airship = json.getAsJsonObject("airship").get("characters").getAsString();
        }
        if (json.has("legend_plus_ui_background")) {
            JsonObject o = json.getAsJsonObject("legend_plus_ui_background");
            if (o.has("character")) {
                legendPlusUiBackground = o.get("character").getAsString();
            }
        }
        if (json.has("airship_main_gui")) {
            JsonObject o = json.getAsJsonObject("airship_main_gui");
            if (o.has("character")) {
                airshipMainGui = o.get("character").getAsString();
            }
        }
        if (json.has("anvil_main_ui")) {
            JsonObject o = json.getAsJsonObject("anvil_main_ui");
            if (o.has("character")) {
                anvilMainUi = o.get("character").getAsString();
            }
        }
        if (json.has("furnace_main_ui")) {
            JsonObject o = json.getAsJsonObject("furnace_main_ui");
            if (o.has("character")) {
                furnaceMainUi = o.get("character").getAsString();
            }
        }
        if (json.has("essence_harvester_ui")) {
            essenceHarvesterUi = json.getAsJsonObject("essence_harvester_ui").get("character").getAsString();
        }
        if (json.has("essence_bag_ui")) {
            essenceBagUi = json.getAsJsonObject("essence_bag_ui").get("character").getAsString();
        }
        if (json.has("essence_selection_ui")) {
            essenceSelectionUi = json.getAsJsonObject("essence_selection_ui").get("character").getAsString();
        }
        if (json.has("ui_categories_factory")) {
            uiCategoriesFactory = json.getAsJsonObject("ui_categories_factory").get("character").getAsString();
        }
        if (json.has("ui_categories_aincrad")) {
            uiCategoriesAincrad = json.getAsJsonObject("ui_categories_aincrad").get("character").getAsString();
        }
        if (json.has("ui_categories_farmzone")) {
            uiCategoriesFarmzone = json.getAsJsonObject("ui_categories_farmzone").get("character").getAsString();
        }
        if (json.has("ui_categories_hub")) {
            uiCategoriesHub = json.getAsJsonObject("ui_categories_hub").get("character").getAsString();
        }
        if (json.has("ui_categories_class_selection")) {
            uiCategoriesClassSelection = json.getAsJsonObject("ui_categories_class_selection").get("character").getAsString();
        }
        if (json.has("ui_ressource_bag")) {
            uiRessourceBag = json.getAsJsonObject("ui_ressource_bag").get("character").getAsString();
        }
        if (json.has("ui_material_bag")) {
            uiMaterialBag = json.getAsJsonObject("ui_material_bag").get("character").getAsString();
        }
        if (json.has("ui_quick_travel_overworld")) {
            uiQuickTravelOverworld = json.getAsJsonObject("ui_quick_travel_overworld").get("character").getAsString();
        }
        if (json.has("ui_quick_travel_mining")) {
            uiQuickTravelMining = json.getAsJsonObject("ui_quick_travel_mining").get("character").getAsString();
        }
        if (json.has("ui_quick_travel_ocean")) {
            uiQuickTravelOcean = json.getAsJsonObject("ui_quick_travel_ocean").get("character").getAsString();
        }
        if (json.has("factory_bottom_font")) {
            JsonObject factoryObj = json.getAsJsonObject("factory_bottom_font");
            if (factoryObj.has("characters")) {
                factoryBottomFont = factoryObj.get("characters").getAsString();
            }
            if (factoryObj.has("numbers")) {
                factoryBottomFontNumbers.clear();
                JsonObject numbers = factoryObj.getAsJsonObject("numbers");
                for (String key : numbers.keySet()) {
                    String charStr = numbers.get(key).getAsString();
                    if (!charStr.isEmpty()) {
                        factoryBottomFontNumbers.put(charStr.charAt(0), Integer.parseInt(key));
                    }
                }
            }
        }
        if (json.has("factory_font_first_line") && json.getAsJsonObject("factory_font_first_line").has("mapping")) {
            factoryFontFirstLine.clear();
            JsonObject mapping = json.getAsJsonObject("factory_font_first_line").getAsJsonObject("mapping");
            for (String key : mapping.keySet()) {
                String charStr = mapping.get(key).getAsString();
                if (!charStr.isEmpty()) {
                    factoryFontFirstLine.put(charStr.charAt(0), key);
                }
            }
        }
        if (json.has("aincraft_font_first_line") && json.getAsJsonObject("aincraft_font_first_line").has("mapping")) {
            aincraftFontFirstLine.clear();
            JsonObject mapping = json.getAsJsonObject("aincraft_font_first_line").getAsJsonObject("mapping");
            for (String key : mapping.keySet()) {
                String charStr = mapping.get(key).getAsString();
                if (!charStr.isEmpty()) {
                    aincraftFontFirstLine.put(charStr.charAt(0), key);
                }
            }
        }
        String komboDigitsKey = json.has("npc_alerts_kombo_kiste_bossbar_digits")
                ? "npc_alerts_kombo_kiste_bossbar_digits"
                : (json.has("tab_info_kombo_kiste_bossbar_digits") ? "tab_info_kombo_kiste_bossbar_digits" : null);
        if (komboDigitsKey != null) {
            JsonObject komboDigitsObj = json.getAsJsonObject(komboDigitsKey);
            npcAlertsKomboKisteBossBarDigitCodePoints.clear();
            if (komboDigitsObj.has("numbers")) {
                JsonObject numbers = komboDigitsObj.getAsJsonObject("numbers");
                for (String key : numbers.keySet()) {
                    String charStr = numbers.get(key).getAsString();
                    if (!charStr.isEmpty()) {
                        npcAlertsKomboKisteBossBarDigitCodePoints.put(charStr.codePointAt(0), Integer.parseInt(key));
                    }
                }
            }
        }
        if (npcAlertsKomboKisteBossBarDigitCodePoints.isEmpty() && !DISABLE_ZEICHEN_FALLBACK_FOR_TESTING) {
            fillDefaultNpcAlertsKomboKisteBossBarDigits();
        }
    }

    private static void fillDefaultNpcAlertsKomboKisteBossBarDigits() {
        npcAlertsKomboKisteBossBarDigitCodePoints.clear();
        String chars = "㟮㟯㟰㟱㟲㟳㟴㟵㟶㟷";
        int idx = 0;
        for (int d = 0; d < 10 && idx < chars.length(); d++) {
            int cp = chars.codePointAt(idx);
            npcAlertsKomboKisteBossBarDigitCodePoints.put(cp, d);
            idx += Character.charCount(cp);
        }
    }

    private static void initializeFallback() {
        aincraftBottomFont = "㚏㚐㚑㚒㚓㚔㚕㚖㚗㚘";
        aincraftBottomFontNumbers.clear();
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

        equipmentDisplay[0] = "㵸";
        equipmentDisplay[1] = "㵹";
        equipmentDisplay[2] = "㵺";
        equipmentDisplay[3] = "㵻";

        moblexicon = "㶉";
        pixelSpacer = "㓾㓿㔀㔁㔂㔃㔄㔅㔆㔇㔈㔉㔊㔋㔌㔍㔎㔏㔐㔑㔒㔓㔔㔕㔖㔗㔘㔙㔚㔛㔜㔝㔞㔟㔠㔡㔢㔣㔤";
        cardsMenu = "㷆";
        statuesMenu = "㷂";
        friendsRequestAcceptDeny = "ぢ";
        confirmationUiBackground = "㡟";
        hunterUiBackground = "㲂";
        epicDrops = "㲷㲸㲹㲺㲻㲼㲽㲾㲿㳀㳁㳂㳃㳄㳅㳆㳇㳈㳉㳊㳋㳌㳍㳎㳏㳐㳑㳒㳓㳔㳕㳖㳗㳘㳙㳚㳛㳜㳝㳞";
        legendaryDrops = "㲏㲐㲑㲒㲓㲔㲕㲖㲗㲘㲙㲚㲛㲜㲝㲞㲟㲠㲡㲢㲣㲤㲥㲦㲧㲨㲩㲪㲫㲬㲭㲮㲯㲰㲱㲲㲳㲴㲵㲶";
        loggingLevelUp = "㳮㳯㳰㳱㳲㳳㳴㳵㳶㳷㳸㳹㳺㳻㳼㳽㳾㳿㴀㴁㴂㴃㴄㴅㴆㴇㴈㴉㴊㴋㴌㴍㴎㴏㴐㴑㴒㴓㴔㴕";
        moblexiconAnimation = "㶊㶋㶌㶍㶎㶏㶐㶑㶒㶓㶔㶕㶖㶗㶘㶙㶚㶛㶜㶝㶞㶟㶠㶡㶢㶣㶤㶥㶦㶧㶨㶩㶪㶫㶬㶭";
        miningLevelUp = "㵍㵎㵏㵐㵑㵒㵓㵔㵕㵖㵗㵘㵙㵚㵛㵜㵝㵞㵟㵠㵡㵢㵣㵤㵥㵦㵧㵨㵩㵪㵫㵬㵭㵮㵯㵰㵱㵲㵳㵴";
        fishingLevelUp = "㴖㴗㴘㴙㴚㴛㴜㴝㴞㴟㴠㴡㴢㴣㴤㴥㴦㴧㴨㴩㴪㴫㴬㴭㴮㴯㴰㴱㴲㴳㴴㴵㴶㴷㴸㴹㴺㴻㴼㴽";
        airship = "㷉㷊㷋㷌㷍㷎㷏㷐㷑㷒㷓㷔㷕㷖㷗㷘㷙㷚㷛㷜㷝㷞㷟㷠㷡㷢㷣㷤㷥㷦㷧㷨㷩㷪㷫㷬㷭㷮㷯㷰㷱㷲㷳㷴㷵㷶㷷㷸㷹㷺㷻㷼㷽㷾㷿㸀";
        legendPlusUiBackground = "㵷";
        airshipMainGui = "㷈";
        glimfangShopUi = "㷇";
        anvilMainUi = "㸒";
        furnaceMainUi = "㷒";
        essenceHarvesterUi = "㸏";
        essenceBagUi = "㸊";
        essenceSelectionUi = "㸋";
        uiCategoriesFactory = "㸞";
        uiCategoriesAincrad = "㸟";
        uiCategoriesFarmzone = "㸠";
        uiCategoriesHub = "㸡";
        uiCategoriesClassSelection = "㵼";
        uiRessourceBag = "Ⳅ";
        uiMaterialBag = "ⳅ";

        fishingComponentsCrafted = "㶂";
        fishingCraftedComponents = "㶃";
        fishingComponentsCraft = "㶄";
        fishingComponentsRecycle = "㶅";

        uiQuickTravelOverworld = "㷔";
        uiQuickTravelMining = "㷕";
        uiQuickTravelOcean = "㷖";

        factoryBottomFont = "㝡㝢㝣㝤㝥㝦㝧㝨㝩㝪";
        factoryBottomFontNumbers.clear();
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

        factoryFontFirstLine = buildGlyphToDecodedMap(
                ZeichenFontLoader.extractCodePoints(legacyFirstLineChars("㜌㜍㜎㜏㜐㜑㜒㜓㜔㜕㜖㜗㜘㜙㜚㜛㜜㜝㜞㜟㜠㜡㜢㜣㜤㜥㜦㜧㜨㜩㜪㜫㜬㜭㜮㜯㜰㜱㜲㜳㜴㜵㜶㜷㜸㜹㜺㜻㜼㜽㜾")),
                STANDARD_LINE_DECODE);
        aincraftFontFirstLine = buildGlyphToDecodedMap(
                ZeichenFontLoader.extractCodePoints(legacyFirstLineChars("㘺㘻㘼㘽㘾㘿㙀㙁㙂㙃㙄㙅㙆㙇㙈㙉㙊㙋㙌㙍㙎㙏㙐㙑㙒㙓㙔㙕㙖㙗㙘㙙㙚㙛㙜㙝㙞㙟㙠㙡㙢㙣㙤㙥㙦㙧㙨㙩㙪㙫㙬")),
                STANDARD_LINE_DECODE);

        fillDefaultNpcAlertsKomboKisteBossBarDigits();
    }

    private static com.google.gson.JsonArray legacyFirstLineChars(String chars) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        array.add(chars);
        return array;
    }

    public static String getAincraftBottomFont() {
        ensureInitialized();
        return aincraftBottomFont;
    }

    public static Map<Character, Integer> getAincraftBottomFontNumbers() {
        ensureInitialized();
        return new HashMap<>(aincraftBottomFontNumbers);
    }

    public static String[] getEquipmentDisplay() {
        ensureInitialized();
        return equipmentDisplay.clone();
    }

    public static boolean containsEquipmentDisplay(String text) {
        ensureInitialized();
        if (text == null) {
            return false;
        }
        for (String character : equipmentDisplay) {
            if (character != null && !character.isEmpty() && text.contains(character)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inventartitel, in denen der Item-Viewer nicht gezeigt werden soll
     * (Equipment-Display, Legend+-UI, Luftschiff-Haupt-GUI, Glimfang-Shop).
     */
    public static boolean shouldHideItemViewerForInventoryTitle(String text) {
        ensureInitialized();
        if (text == null) {
            return false;
        }
        if (containsEquipmentDisplay(text)) {
            return true;
        }
        if (containsLegendPlusUiBackground(text)) {
            return true;
        }
        if (airshipMainGui != null && !airshipMainGui.isEmpty() && text.contains(airshipMainGui)) {
            return true;
        }
        if (glimfangShopUi != null && !glimfangShopUi.isEmpty() && text.contains(glimfangShopUi)) {
            return true;
        }
        return false;
    }

    public static String getMoblexicon() {
        ensureInitialized();
        return moblexicon;
    }

    public static boolean containsMoblexicon(String text) {
        ensureInitialized();
        return text != null && moblexicon != null && !moblexicon.isEmpty() && text.contains(moblexicon);
    }

    public static String getPixelSpacer() {
        ensureInitialized();
        return pixelSpacer;
    }

    public static boolean containsPixelSpacer(String text) {
        ensureInitialized();
        if (text == null || pixelSpacer == null || pixelSpacer.isEmpty()) {
            return false;
        }
        return text.matches(".*[" + pixelSpacer + "].*");
    }

    public static String getCardsMenuCharacter() {
        ensureInitialized();
        return cardsMenu;
    }

    public static String getStatuesMenuCharacter() {
        ensureInitialized();
        return statuesMenu;
    }

    public static boolean containsCardsStatues(String text) {
        ensureInitialized();
        return isCardsMenuTitle(text) || isStatuesMenuTitle(text);
    }

    public static boolean isCardsMenuTitle(String text) {
        ensureInitialized();
        return text != null && cardsMenu != null && !cardsMenu.isEmpty() && text.contains(cardsMenu);
    }

    public static boolean isStatuesMenuTitle(String text) {
        ensureInitialized();
        return text != null && statuesMenu != null && !statuesMenu.isEmpty() && text.contains(statuesMenu);
    }

    public static boolean isFishingComponentsCraftTitle(String text) {
        ensureInitialized();
        return containsGlyph(text, fishingComponentsCraft);
    }

    public static boolean isFishingComponentsCraftedTitle(String text) {
        ensureInitialized();
        return containsGlyph(text, fishingComponentsCrafted);
    }

    public static boolean isFishingCraftedComponentsTitle(String text) {
        ensureInitialized();
        return containsGlyph(text, fishingCraftedComponents);
    }

    public static boolean isFishingComponentsRecycleTitle(String text) {
        ensureInitialized();
        return containsGlyph(text, fishingComponentsRecycle);
    }

    /** Fishing-Equipment-Menüs mit Suchleiste (ui_components_craft / ui_components_crafted / …). */
    public static boolean isFishingEquipmentSearchMenu(String text) {
        return isFishingComponentsCraftTitle(text)
                || isFishingComponentsCraftedTitle(text)
                || isFishingCraftedComponentsTitle(text)
                || isFishingComponentsRecycleTitle(text);
    }

    private static boolean containsGlyph(String text, String glyph) {
        return text != null && glyph != null && !glyph.isEmpty() && text.contains(glyph);
    }

    public static String getFriendsRequestAcceptDeny() {
        ensureInitialized();
        return friendsRequestAcceptDeny;
    }

    public static boolean containsFriendsRequestAcceptDeny(String text) {
        ensureInitialized();
        return text != null && friendsRequestAcceptDeny != null && !friendsRequestAcceptDeny.isEmpty()
                && text.contains(friendsRequestAcceptDeny);
    }

    public static String getConfirmationUiBackground() {
        ensureInitialized();
        return confirmationUiBackground;
    }

    public static boolean containsConfirmationUiBackground(String text) {
        ensureInitialized();
        return text != null && confirmationUiBackground != null && !confirmationUiBackground.isEmpty()
                && text.contains(confirmationUiBackground);
    }

    public static boolean containsFarmzoneQuickTravel(String text) {
        ensureInitialized();
        if (text == null) {
            return false;
        }
        return containsGlyph(text, uiQuickTravelOverworld)
                || containsGlyph(text, uiQuickTravelMining)
                || containsGlyph(text, uiQuickTravelOcean);
    }

    public static String getHunterUiBackground() {
        ensureInitialized();
        return hunterUiBackground;
    }

    public static boolean containsHunterUiBackground(String text) {
        ensureInitialized();
        return text != null && hunterUiBackground != null && !hunterUiBackground.isEmpty()
                && text.contains(hunterUiBackground);
    }

    public static String getEpicDrops() {
        ensureInitialized();
        return epicDrops;
    }

    public static String getLegendaryDrops() {
        ensureInitialized();
        return legendaryDrops;
    }

    public static String getLoggingLevelUp() {
        ensureInitialized();
        return loggingLevelUp;
    }

    public static String getMoblexiconAnimation() {
        ensureInitialized();
        return moblexiconAnimation;
    }

    public static String getMiningLevelUp() {
        ensureInitialized();
        return miningLevelUp;
    }

    public static String getFishingLevelUp() {
        ensureInitialized();
        return fishingLevelUp;
    }

    public static String getAirship() {
        ensureInitialized();
        return airship;
    }

    public static String getEssenceHarvesterUi() {
        ensureInitialized();
        return essenceHarvesterUi;
    }

    public static boolean containsEssenceHarvesterUi(String text) {
        ensureInitialized();
        return text != null && essenceHarvesterUi != null && !essenceHarvesterUi.isEmpty()
                && text.contains(essenceHarvesterUi);
    }

    public static boolean containsEssenceBagUi(String text) {
        ensureInitialized();
        return text != null && essenceBagUi != null && !essenceBagUi.isEmpty() && text.contains(essenceBagUi);
    }

    public static boolean containsEssenceSelectionUi(String text) {
        ensureInitialized();
        return text != null && essenceSelectionUi != null && !essenceSelectionUi.isEmpty()
                && text.contains(essenceSelectionUi);
    }

    public static boolean containsLegendPlusUiBackground(String text) {
        ensureInitialized();
        return text != null && legendPlusUiBackground != null && !legendPlusUiBackground.isEmpty()
                && text.contains(legendPlusUiBackground);
    }

    public static boolean containsAnvilMainUi(String text) {
        ensureInitialized();
        return text != null && anvilMainUi != null && !anvilMainUi.isEmpty() && text.contains(anvilMainUi);
    }

    public static boolean containsFurnaceMainUi(String text) {
        ensureInitialized();
        return text != null && furnaceMainUi != null && !furnaceMainUi.isEmpty() && text.contains(furnaceMainUi);
    }

    /** Essence-Bag, -Auswahl und Harvester: keine Ebenen-Nummern in Tooltips. */
    public static boolean shouldHideFloorNumbersInEssenceMenus(String text) {
        return containsEssenceHarvesterUi(text)
                || containsEssenceBagUi(text)
                || containsEssenceSelectionUi(text);
    }

    public static boolean containsSpecialMenusNoJei(String text) {
        ensureInitialized();
        if (text == null) {
            return false;
        }
        return (uiCategoriesFactory != null && !uiCategoriesFactory.isEmpty() && text.contains(uiCategoriesFactory))
                || (uiCategoriesAincrad != null && !uiCategoriesAincrad.isEmpty() && text.contains(uiCategoriesAincrad))
                || (uiCategoriesFarmzone != null && !uiCategoriesFarmzone.isEmpty() && text.contains(uiCategoriesFarmzone))
                || (uiCategoriesHub != null && !uiCategoriesHub.isEmpty() && text.contains(uiCategoriesHub))
                || containsClassSelectionUi(text);
    }

    public static boolean containsClassSelectionUi(String text) {
        ensureInitialized();
        return text != null && uiCategoriesClassSelection != null && !uiCategoriesClassSelection.isEmpty()
                && text.contains(uiCategoriesClassSelection);
    }

    public static String getUiRessourceBag() {
        ensureInitialized();
        return uiRessourceBag;
    }

    public static boolean containsUiRessourceBag(String text) {
        ensureInitialized();
        return text != null && uiRessourceBag != null && !uiRessourceBag.isEmpty() && text.contains(uiRessourceBag);
    }

    public static String getUiMaterialBag() {
        ensureInitialized();
        return uiMaterialBag;
    }

    public static boolean containsUiMaterialBag(String text) {
        ensureInitialized();
        return text != null && uiMaterialBag != null && !uiMaterialBag.isEmpty() && text.contains(uiMaterialBag);
    }

    public static String getFactoryBottomFont() {
        ensureInitialized();
        return factoryBottomFont;
    }

    public static Map<Character, Integer> getFactoryBottomFontNumbers() {
        ensureInitialized();
        return new HashMap<>(factoryBottomFontNumbers);
    }

    /**
     * Dekodiert Custom-Font-Ziffern in Tab-Widget-Zeilen (Aincraft/Factory bottom font).
     */
    public static String decodeTabWidgetText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        ensureInitialized();
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Integer digit = aincraftBottomFontNumbers.get(c);
            if (digit == null) {
                digit = factoryBottomFontNumbers.get(c);
            }
            if (digit != null) {
                sb.append(digit);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static Map<Character, String> getFactoryFontFirstLine() {
        ensureInitialized();
        return new HashMap<>(factoryFontFirstLine);
    }

    public static Map<Character, String> getAincraftFontFirstLine() {
        ensureInitialized();
        return new HashMap<>(aincraftFontFirstLine);
    }

    public static Map<Integer, Integer> getNpcAlertsKomboKisteBossBarDigitCodePoints() {
        ensureInitialized();
        if (npcAlertsKomboKisteBossBarDigitCodePoints.isEmpty()) {
            fillDefaultNpcAlertsKomboKisteBossBarDigits();
        }
        return Collections.unmodifiableMap(npcAlertsKomboKisteBossBarDigitCodePoints);
    }

    private static void ensureInitialized() {
        if (!isInitialized) {
            initialize();
        }
    }
}

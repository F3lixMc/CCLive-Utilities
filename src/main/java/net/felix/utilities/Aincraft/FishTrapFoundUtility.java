package net.felix.utilities.Aincraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.felix.CCLiveUtilities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Verfolgt gefundene Fischreusen anhand des Inventars "Reusen herstellen".
 */
public class FishTrapFoundUtility {

    private static final String SAVE_FILE_NAME = "found_fish_traps.json";
    private static final String ITEMS_CONFIG_FILE = "assets/cclive-utilities/items.json";
    private static final String LOCAL_ITEMS_FILE = "items.json";
    private static final int[] SCAN_SLOTS = Stream.concat(
        IntStream.rangeClosed(10, 16).boxed(),
        IntStream.rangeClosed(19, 25).boxed()
    ).mapToInt(Integer::intValue).toArray();

    private static boolean isInitialized = false;
    private static FishTrapFoundUtility INSTANCE;

    private final Set<String> foundFishTraps = new HashSet<>();
    private final Set<String> knownFishTraps = new HashSet<>();
    private final File saveFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private FishTrapFoundUtility() {
        Path configDir = CCLiveUtilities.getConfigDir().resolve("cclive-utilities");
        this.saveFile = configDir.resolve(SAVE_FILE_NAME).toFile();
        loadKnownFishTraps();
        loadFoundFishTraps();
    }

    public static void initialize() {
        if (isInitialized) {
            return;
        }
        INSTANCE = new FishTrapFoundUtility();
        isInitialized = true;
    }

    public static FishTrapFoundUtility getInstance() {
        if (INSTANCE == null) {
            initialize();
        }
        return INSTANCE;
    }

    public static boolean isFound(String fishTrapName) {
        if (fishTrapName == null || fishTrapName.isEmpty()) {
            return false;
        }
        try {
            return getInstance().foundFishTraps.contains(fishTrapName);
        } catch (Exception e) {
            return false;
        }
    }

    public static void reset() {
        try {
            FishTrapFoundUtility instance = getInstance();
            instance.foundFishTraps.clear();
            instance.saveFoundFishTraps();
        } catch (Exception e) {
            // Silent error handling
        }
    }

    public static void onClientTick(MinecraftClient client) {
        if (client == null || !(client.currentScreen instanceof HandledScreen<?> screen)) {
            return;
        }

        String cleanTitle = cleanInventoryTitle(screen.getTitle().getString());
        if (!cleanTitle.contains("Reusen herstellen")) {
            return;
        }

        getInstance().scanSlots(screen);
    }

    private void scanSlots(HandledScreen<?> screen) {
        if (screen.getScreenHandler() == null) {
            return;
        }

        for (int slotIndex : SCAN_SLOTS) {
            if (slotIndex >= screen.getScreenHandler().slots.size()) {
                continue;
            }

            Slot slot = screen.getScreenHandler().slots.get(slotIndex);
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            String fishTrapName = resolveKnownFishTrap(ItemInfoUtility.getFishTrapNameFromStack(stack));
            if (fishTrapName.isEmpty()) {
                continue;
            }

            if (foundFishTraps.add(fishTrapName)) {
                saveFoundFishTraps();
                net.felix.utilities.ItemViewer.ItemViewerUtility.onFoundStatusChanged();
            }
        }
    }

    private String resolveKnownFishTrap(String extractedName) {
        if (extractedName == null || extractedName.isEmpty()) {
            return "";
        }
        if (knownFishTraps.contains(extractedName)) {
            return extractedName;
        }
        for (String known : knownFishTraps) {
            if (known.equalsIgnoreCase(extractedName)) {
                return known;
            }
        }
        return "";
    }

    private static String cleanInventoryTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.replaceAll("§[0-9a-fk-or]", "");
    }

    private void loadKnownFishTraps() {
        knownFishTraps.clear();
        try {
            Path localFile = CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve(LOCAL_ITEMS_FILE);
            if (Files.exists(localFile)) {
                parseKnownFishTraps(Files.readString(localFile));
                return;
            }

            var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
                .orElseThrow(() -> new RuntimeException("Mod container not found"))
                .findPath(ITEMS_CONFIG_FILE)
                .orElseThrow(() -> new RuntimeException("items.json not found"));

            parseKnownFishTraps(Files.readString(resource));
        } catch (Exception e) {
            // Silent error handling
        }
    }

    private void parseKnownFishTraps(String jsonContent) {
        JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
        JsonArray fishTraps = root.getAsJsonArray("fish_traps");
        if (fishTraps == null) {
            return;
        }

        for (JsonElement element : fishTraps) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            if (item.has("name") && !item.get("name").isJsonNull()) {
                String name = item.get("name").getAsString();
                if (name != null && !name.isEmpty()) {
                    knownFishTraps.add(name);
                }
            }
        }
    }

    private void loadFoundFishTraps() {
        File oldSaveFile = new File("config", SAVE_FILE_NAME);
        if (oldSaveFile.exists() && !saveFile.exists()) {
            try {
                saveFile.getParentFile().mkdirs();
                Files.move(oldSaveFile.toPath(), saveFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // Silent error handling
            }
        }

        if (!saveFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(saveFile)) {
            Type type = new TypeToken<HashSet<String>>(){}.getType();
            Set<String> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                foundFishTraps.clear();
                foundFishTraps.addAll(loaded);
            }
        } catch (IOException e) {
            // Silent error handling
        }
    }

    private void saveFoundFishTraps() {
        try {
            saveFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(saveFile)) {
                gson.toJson(foundFishTraps, writer);
            }
        } catch (IOException e) {
            // Silent error handling
        }
    }
}

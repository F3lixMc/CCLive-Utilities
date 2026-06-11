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
import net.felix.utilities.Overall.ZeichenUtility;

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

/**
 * Verfolgt gefundene Angel-Komponenten anhand der Inventare
 * ui_components_crafted, ui_crafted_components, ui_components_craft und ui_components_recycle.
 */
public class FishingComponentFoundUtility {

    private static final String SAVE_FILE_NAME = "found_fishing_components.json";
    private static final String ITEMS_CONFIG_FILE = "assets/cclive-utilities/items.json";
    private static final String LOCAL_ITEMS_FILE = "items.json";
    private static final int[] FULL_SLOTS = IntStream.rangeClosed(0, 44).toArray();
    private static final int[] PARTIAL_SLOTS = IntStream.rangeClosed(9, 44).toArray();

    private static boolean isInitialized = false;
    private static FishingComponentFoundUtility INSTANCE;

    private final Set<String> foundComponents = new HashSet<>();
    private final Set<String> knownComponents = new HashSet<>();
    private final File saveFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private FishingComponentFoundUtility() {
        Path configDir = CCLiveUtilities.getConfigDir().resolve("cclive-utilities");
        this.saveFile = configDir.resolve(SAVE_FILE_NAME).toFile();
        loadKnownComponents();
        loadFoundComponents();
    }

    public static void initialize() {
        if (isInitialized) {
            return;
        }
        INSTANCE = new FishingComponentFoundUtility();
        isInitialized = true;
    }

    public static FishingComponentFoundUtility getInstance() {
        if (INSTANCE == null) {
            initialize();
        }
        return INSTANCE;
    }

    public static boolean isFound(String componentName) {
        if (componentName == null || componentName.isEmpty()) {
            return false;
        }
        try {
            return getInstance().foundComponents.contains(componentName);
        } catch (Exception e) {
            return false;
        }
    }

    public static void reset() {
        try {
            FishingComponentFoundUtility instance = getInstance();
            instance.foundComponents.clear();
            instance.saveFoundComponents();
        } catch (Exception e) {
            // Silent error handling
        }
    }

    public static void onClientTick(MinecraftClient client) {
        if (client == null || !(client.currentScreen instanceof HandledScreen<?> screen)) {
            return;
        }

        String cleanTitle = cleanInventoryTitle(screen.getTitle().getString());
        if (!ZeichenUtility.isFishingEquipmentSearchMenu(cleanTitle)) {
            return;
        }

        int[] slots = getSlotsForMenu(cleanTitle);
        if (slots == null) {
            return;
        }

        getInstance().scanSlots(screen, slots);
    }

    private void scanSlots(HandledScreen<?> screen, int[] slotIndices) {
        if (screen.getScreenHandler() == null) {
            return;
        }

        for (int slotIndex : slotIndices) {
            if (slotIndex >= screen.getScreenHandler().slots.size()) {
                continue;
            }

            Slot slot = screen.getScreenHandler().slots.get(slotIndex);
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            String componentName = ItemInfoUtility.getFishingComponentNameFromStack(stack);
            if (componentName.isEmpty() || !knownComponents.contains(componentName)) {
                continue;
            }

            if (foundComponents.add(componentName)) {
                saveFoundComponents();
                net.felix.utilities.ItemViewer.ItemViewerUtility.onFoundStatusChanged();
            }
        }
    }

    private static int[] getSlotsForMenu(String cleanTitle) {
        if (ZeichenUtility.isFishingComponentsCraftedTitle(cleanTitle)
                || ZeichenUtility.isFishingCraftedComponentsTitle(cleanTitle)) {
            return FULL_SLOTS;
        }
        if (ZeichenUtility.isFishingComponentsCraftTitle(cleanTitle)
                || ZeichenUtility.isFishingComponentsRecycleTitle(cleanTitle)) {
            return PARTIAL_SLOTS;
        }
        return null;
    }

    private static String cleanInventoryTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.replaceAll("§[0-9a-fk-or]", "");
    }

    private void loadKnownComponents() {
        knownComponents.clear();
        try {
            Path localFile = CCLiveUtilities.getConfigDir().resolve("cclive-utilities").resolve(LOCAL_ITEMS_FILE);
            if (Files.exists(localFile)) {
                parseKnownComponents(Files.readString(localFile));
                return;
            }

            var resource = FabricLoader.getInstance().getModContainer("cclive-utilities")
                .orElseThrow(() -> new RuntimeException("Mod container not found"))
                .findPath(ITEMS_CONFIG_FILE)
                .orElseThrow(() -> new RuntimeException("items.json not found"));

            parseKnownComponents(Files.readString(resource));
        } catch (Exception e) {
            // Silent error handling
        }
    }

    private void parseKnownComponents(String jsonContent) {
        JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
        JsonArray fishingComponents = root.getAsJsonArray("fishing_components");
        if (fishingComponents == null) {
            return;
        }

        for (JsonElement element : fishingComponents) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            if (item.has("name") && !item.get("name").isJsonNull()) {
                String name = item.get("name").getAsString();
                if (name != null && !name.isEmpty()) {
                    knownComponents.add(name);
                }
            }
        }
    }

    private void loadFoundComponents() {
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
                foundComponents.clear();
                foundComponents.addAll(loaded);
            }
        } catch (IOException e) {
            // Silent error handling
        }
    }

    private void saveFoundComponents() {
        try {
            saveFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(saveFile)) {
                gson.toJson(foundComponents, writer);
            }
        } catch (IOException e) {
            // Silent error handling
        }
    }
}

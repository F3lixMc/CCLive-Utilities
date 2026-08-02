package net.felix.utilities.ItemViewer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.ItemInfoUtility;
import net.felix.utilities.Overall.ZeichenUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Wendet die aktuelle Item-Viewer-Filterung auf geöffnete Schmiede-/Angel-Inventare an
 * (nicht passende Items werden als schwarzer Beton ausgeblendet).
 */
public class ItemViewerInventoryFilterUtility {

    private static final int[] DEFAULT_BLUEPRINT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private static final int[] FISHING_COMPONENTS_FULL_SLOTS = IntStream.rangeClosed(0, 44).toArray();
    private static final int[] FISHING_COMPONENTS_PARTIAL_SLOTS = IntStream.rangeClosed(9, 44).toArray();
    private static final int[] EQUIPMENT_SLOTS = IntStream.rangeClosed(9, 44).toArray();
    private static final int[] UMSCHMIEDEN_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    private static boolean initialized = false;
    private static boolean filterActive = false;
    private static AbstractContainerScreen<?> activeFilteredScreen = null;

    private static final Map<Integer, ItemStack> originalItems = new HashMap<>();
    private static final Set<Integer> hiddenSlots = new HashSet<>();
    private static final Map<Integer, ItemStack> lastKnownItems = new HashMap<>();

    public static void initialize() {
        if (initialized) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(ItemViewerInventoryFilterUtility::onClientTick);
        initialized = true;
    }

    public static boolean isFilterActive() {
        return filterActive;
    }

    public static void toggleFilter() {
        if (filterActive) {
            deactivateFilter();
        } else {
            filterActive = true;
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.screen instanceof AbstractContainerScreen<?> screen && isFilterableInventory(screen)) {
                enterSmithyInventory(screen);
                activeFilteredScreen = screen;
                applyFilter(screen);
            }
        }
    }

    public static void deactivateFilter() {
        resetFilter(activeFilteredScreen);
    }

    public static void onFilteredItemsChanged() {
        if (!filterActive) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.screen instanceof AbstractContainerScreen<?> screen && isFilterableInventory(screen)) {
            restoreHiddenItems(screen);
            applyFilter(screen);
        }
    }

    private static void onClientTick(Minecraft client) {
        if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod) {
            return;
        }

        if (client.player == null) {
            if (filterActive || !hiddenSlots.isEmpty()) {
                resetFilter(activeFilteredScreen);
            }
            return;
        }

        if (!(client.screen instanceof AbstractContainerScreen<?> handledScreen)) {
            if (filterActive || !hiddenSlots.isEmpty()) {
                resetFilter(activeFilteredScreen);
            }
            return;
        }

        if (filterActive && activeFilteredScreen != null && activeFilteredScreen != handledScreen) {
            resetFilter(activeFilteredScreen);
            return;
        }

        if (!filterActive) {
            return;
        }

        if (!isFilterableInventory(handledScreen)) {
            resetFilter(activeFilteredScreen);
            return;
        }

        if (activeFilteredScreen != handledScreen) {
            enterSmithyInventory(handledScreen);
            activeFilteredScreen = handledScreen;
        }

        if (hasInventoryChanged(handledScreen)) {
            updateOriginalItemsAfterChange(handledScreen);
            refreshLastKnownItems(handledScreen);
        } else {
            refreshLastKnownItems(handledScreen);
        }

        applyFilter(handledScreen);
    }

    private static void resetFilter(AbstractContainerScreen<?> screen) {
        restoreHiddenItems(screen);
        clearState();
        filterActive = false;
        activeFilteredScreen = null;
    }

    private static void enterSmithyInventory(AbstractContainerScreen<?> screen) {
        originalItems.clear();
        hiddenSlots.clear();
        lastKnownItems.clear();
        saveOriginalItems(screen);
        refreshLastKnownItems(screen);
    }

    private static void clearState() {
        originalItems.clear();
        hiddenSlots.clear();
        lastKnownItems.clear();
    }

    public static boolean isFilterableInventory(AbstractContainerScreen<?> screen) {
        if (screen == null) {
            return false;
        }
        String title = screen.getTitle().getString();
        String cleanForGlyphs = stripColorCodes(title);

        if (ZeichenUtility.isCardsMenuTitle(cleanForGlyphs) || ZeichenUtility.isStatuesMenuTitle(cleanForGlyphs)) {
            return false;
        }
        if (ZeichenUtility.isFishingEquipmentSearchMenu(cleanForGlyphs)) {
            return true;
        }
        if (isNebenhandInventoryTitle(title)) {
            return true;
        }

        String cleanTitle = cleanBlueprintTitle(title);
        return cleanTitle.contains("Bauplan")
                || cleanTitle.contains("Baupläne")
                || cleanTitle.contains("Runen [Baupläne]")
                || cleanTitle.contains("Werkzeug Sammlung")
                || cleanTitle.contains("Waffen Sammlung")
                || cleanTitle.contains("Rüstungs Sammlung")
                || cleanTitle.contains("Favorisierte [Rüstungsbaupläne]")
                || cleanTitle.contains("Favorisierte [Waffenbaupläne]")
                || cleanTitle.contains("Favorisierte [Werkzeugbaupläne]")
                || cleanTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools")
                || cleanTitle.contains("Zerlegen")
                || cleanTitle.contains("Umschmieden")
                || cleanTitle.contains("CACTUS_CLICKER.CACTUS_CLICKER")
                || cleanTitle.contains("Geschützte Items")
                || (cleanTitle.contains("Ausrüstung") && cleanTitle.contains("Auswählen"));
    }

    public static int[] getSlotsForMenu(AbstractContainerScreen<?> screen) {
        String title = screen.getTitle().getString();
        String cleanForGlyphs = stripColorCodes(title);

        if (ZeichenUtility.isFishingComponentsCraftedTitle(cleanForGlyphs)) {
            return FISHING_COMPONENTS_FULL_SLOTS;
        }
        if (ZeichenUtility.isFishingComponentsCraftTitle(cleanForGlyphs)
                || ZeichenUtility.isFishingCraftedComponentsTitle(cleanForGlyphs)
                || ZeichenUtility.isFishingComponentsRecycleTitle(cleanForGlyphs)) {
            return FISHING_COMPONENTS_PARTIAL_SLOTS;
        }

        String cleanTitle = cleanBlueprintTitle(title);
        if (cleanTitle.contains("Umschmieden") || cleanTitle.contains("CACTUS_CLICKER.CACTUS_CLICKER")) {
            return UMSCHMIEDEN_SLOTS;
        }
        if (cleanTitle.contains("Ausrüstung") && cleanTitle.contains("Auswählen")) {
            return DEFAULT_BLUEPRINT_SLOTS;
        }
        if (cleanTitle.contains("Zerlegen") || cleanTitle.contains("Geschützte Items")) {
            return EQUIPMENT_SLOTS;
        }
        if (cleanTitle.contains("Runen [Baupläne]")) {
            return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        }
        if (cleanTitle.contains("Werkzeug Sammlung")
                || cleanTitle.contains("Waffen Sammlung")
                || cleanTitle.contains("Rüstungs Sammlung")) {
            return DEFAULT_BLUEPRINT_SLOTS;
        }
        return DEFAULT_BLUEPRINT_SLOTS;
    }

    private static void applyFilter(AbstractContainerScreen<?> screen) {
        if (!filterActive || screen == null) {
            return;
        }

        Set<String> allowedNames = ItemViewerUtility.getFilteredItemNamesForInventory(screen);
        int[] slots = getSlotsForMenu(screen);

        for (int slotIndex : slots) {
            if (slotIndex >= screen.getMenu().slots.size()) {
                continue;
            }

            Slot slot = screen.getMenu().slots.get(slotIndex);
            ItemStack currentStack = slot.getItem();
            ItemStack itemForMatch = resolveItemForMatching(slotIndex, currentStack);

            if (itemForMatch.isEmpty()) {
                if (hiddenSlots.contains(slotIndex)) {
                    restoreSlot(screen, slotIndex);
                }
                continue;
            }

            if (!originalItems.containsKey(slotIndex) && currentStack.getItem() != Items.BLACK_CONCRETE) {
                originalItems.put(slotIndex, currentStack.copy());
            }

            boolean matches = isItemAllowed(itemForMatch, allowedNames);
            if (matches) {
                if (hiddenSlots.contains(slotIndex)) {
                    restoreSlot(screen, slotIndex);
                }
            } else if (currentStack.getItem() != Items.BLACK_CONCRETE || hiddenSlots.contains(slotIndex)) {
                hideSlot(screen, slotIndex, itemForMatch);
            }
        }
    }

    private static ItemStack resolveItemForMatching(int slotIndex, ItemStack currentStack) {
        if (currentStack == null || currentStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (currentStack.getItem() == Items.BLACK_CONCRETE) {
            if (hiddenSlots.contains(slotIndex) && originalItems.containsKey(slotIndex)) {
                return originalItems.get(slotIndex);
            }
            return ItemStack.EMPTY;
        }
        return currentStack;
    }

    private static boolean isItemAllowed(ItemStack itemStack, Set<String> allowedNames) {
        if (allowedNames.isEmpty()) {
            if (!ItemViewerUtility.areItemsLoaded()) {
                return true;
            }
            return false;
        }

        String fishingName = ItemInfoUtility.getFishingComponentNameFromStack(itemStack);
        if (!fishingName.isEmpty() && matchesAllowedName(fishingName, allowedNames)) {
            return true;
        }

        String itemName = extractNameForMatching(itemStack);
        return !itemName.isEmpty() && matchesAllowedName(itemName, allowedNames);
    }

    private static boolean matchesAllowedName(String itemName, Set<String> allowedNames) {
        for (String allowedName : allowedNames) {
            if (itemName.equalsIgnoreCase(allowedName)) {
                return true;
            }
        }
        return false;
    }

    static String extractNameForMatching(ItemStack itemStack) {
        String itemName = itemStack.getHoverName().getString();
        itemName = itemName.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\u3400-\\u4DBF]", "");
        itemName = itemName.replace(" [Ausgeblendet]", "").trim();
        if (itemName.contains("[Bauplan]")) {
            itemName = itemName.substring(0, itemName.indexOf("[Bauplan]")).trim();
        }
        if (itemName.startsWith("-")) {
            itemName = itemName.substring(1).trim();
        }
        if (itemName.endsWith("-")) {
            itemName = itemName.substring(0, itemName.length() - 1).trim();
        }
        return itemName.trim();
    }

    private static void hideSlot(AbstractContainerScreen<?> screen, int slotIndex, ItemStack originalItem) {
        if (!originalItems.containsKey(slotIndex)) {
            originalItems.put(slotIndex, originalItem.copy());
        }

        Slot slot = screen.getMenu().slots.get(slotIndex);
        ItemStack blackConcrete = new ItemStack(Items.BLACK_CONCRETE);
        blackConcrete.set(DataComponents.CUSTOM_NAME, originalItem.get(DataComponents.CUSTOM_NAME));
        blackConcrete.set(DataComponents.LORE, originalItem.get(DataComponents.LORE));

        String originalName = blackConcrete.getHoverName().getString();
        if (!originalName.contains("[Ausgeblendet]")) {
            blackConcrete.set(DataComponents.CUSTOM_NAME, Component.literal(originalName + " §7[Ausgeblendet]"));
        }

        slot.setByPlayer(blackConcrete);
        hiddenSlots.add(slotIndex);
    }

    private static void restoreSlot(AbstractContainerScreen<?> screen, int slotIndex) {
        if (!hiddenSlots.contains(slotIndex)) {
            return;
        }
        Slot slot = screen.getMenu().slots.get(slotIndex);
        ItemStack currentItem = slot.getItem();
        if (currentItem.getItem() == Items.BLACK_CONCRETE && originalItems.containsKey(slotIndex)) {
            slot.setByPlayer(originalItems.get(slotIndex).copy());
        }
        hiddenSlots.remove(slotIndex);
    }

    private static void restoreHiddenItems(AbstractContainerScreen<?> screen) {
        if (screen == null) {
            return;
        }
        for (int slotIndex : new HashSet<>(hiddenSlots)) {
            if (slotIndex < screen.getMenu().slots.size()) {
                restoreSlot(screen, slotIndex);
            }
        }
    }

    private static void saveOriginalItems(AbstractContainerScreen<?> screen) {
        for (int slotIndex : getSlotsForMenu(screen)) {
            if (slotIndex >= screen.getMenu().slots.size()) {
                continue;
            }
            ItemStack itemStack = screen.getMenu().slots.get(slotIndex).getItem();
            if (!itemStack.isEmpty() && itemStack.getItem() != Items.BLACK_CONCRETE) {
                originalItems.put(slotIndex, itemStack.copy());
            }
        }
    }

    private static void updateOriginalItemsAfterChange(AbstractContainerScreen<?> screen) {
        restoreHiddenItems(screen);
        for (int slotIndex : getSlotsForMenu(screen)) {
            if (slotIndex >= screen.getMenu().slots.size()) {
                continue;
            }
            ItemStack currentItem = screen.getMenu().slots.get(slotIndex).getItem();
            if (currentItem.getItem() == Items.BLACK_CONCRETE) {
                continue;
            }
            if (currentItem.isEmpty()) {
                originalItems.remove(slotIndex);
            } else {
                originalItems.put(slotIndex, currentItem.copy());
            }
        }
    }

    private static void refreshLastKnownItems(AbstractContainerScreen<?> screen) {
        for (int slotIndex : getSlotsForMenu(screen)) {
            if (slotIndex >= screen.getMenu().slots.size()) {
                continue;
            }
            ItemStack itemStack = screen.getMenu().slots.get(slotIndex).getItem();
            ItemStack comparable = itemStack;
            if (itemStack.getItem() == Items.BLACK_CONCRETE && hiddenSlots.contains(slotIndex) && originalItems.containsKey(slotIndex)) {
                comparable = originalItems.get(slotIndex);
            }
            if (comparable != null && !comparable.isEmpty()) {
                lastKnownItems.put(slotIndex, comparable.copy());
            } else {
                lastKnownItems.remove(slotIndex);
            }
        }
    }

    private static boolean hasInventoryChanged(AbstractContainerScreen<?> screen) {
        if (lastKnownItems.isEmpty()) {
            return false;
        }
        for (int slotIndex : getSlotsForMenu(screen)) {
            if (slotIndex >= screen.getMenu().slots.size()) {
                continue;
            }
            ItemStack currentItem = screen.getMenu().slots.get(slotIndex).getItem();
            if (currentItem.getItem() == Items.BLACK_CONCRETE && hiddenSlots.contains(slotIndex) && originalItems.containsKey(slotIndex)) {
                currentItem = originalItems.get(slotIndex);
            }
            ItemStack lastKnownItem = lastKnownItems.get(slotIndex);
            if (!areItemsEqual(currentItem, lastKnownItem)) {
                return true;
            }
        }
        return false;
    }

    private static boolean areItemsEqual(ItemStack item1, ItemStack item2) {
        if (item1 == null || item1.isEmpty()) {
            return item2 == null || item2.isEmpty();
        }
        if (item2 == null || item2.isEmpty()) {
            return false;
        }
        if (item1.getItem() != item2.getItem()) {
            return false;
        }
        Component name1 = item1.get(DataComponents.CUSTOM_NAME);
        Component name2 = item2.get(DataComponents.CUSTOM_NAME);
        if (name1 != null && name2 != null) {
            return name1.getString().equals(name2.getString());
        }
        return name1 == null && name2 == null;
    }

    private static String stripColorCodes(String title) {
        if (title == null) {
            return "";
        }
        return title.replaceAll("§[0-9a-fk-or]", "");
    }

    private static String cleanBlueprintTitle(String title) {
        return stripColorCodes(title).replaceAll("[\\u3400-\\u4DBF]", "");
    }

    private static boolean isNebenhandInventoryTitle(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        if (titleContainsNebenhand(stripColorCodes(title))) {
            return true;
        }
        return titleContainsNebenhand(cleanBlueprintTitle(title));
    }

    private static boolean titleContainsNebenhand(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        if (text.contains("Nebenhand") || text.contains("[Nebenhand]")) {
            return true;
        }
        return text.replaceAll("[^\\p{L}\\d\\[\\]]", "").contains("Nebenhand");
    }

    /**
     * Kategorie aus dem geöffneten Inventar, damit die Filterung auch ohne Suchtext greift.
     */
    public static String resolveItemCategoryForInventory(AbstractContainerScreen<?> screen) {
        if (screen == null) {
            return null;
        }
        String cleanForGlyphs = stripColorCodes(screen.getTitle().getString());
        if (ZeichenUtility.isFishingEquipmentSearchMenu(cleanForGlyphs)) {
            return "fishing_components";
        }
        String cleanTitle = cleanBlueprintTitle(screen.getTitle().getString());
        if (cleanTitle.contains("Bauplan")
                || cleanTitle.contains("Baupläne")
                || cleanTitle.contains("Runen [Baupläne]")
                || cleanTitle.contains("Werkzeug Sammlung")
                || cleanTitle.contains("Waffen Sammlung")
                || cleanTitle.contains("Rüstungs Sammlung")
                || cleanTitle.contains("Favorisierte [Rüstungsbaupläne]")
                || cleanTitle.contains("Favorisierte [Waffenbaupläne]")
                || cleanTitle.contains("Favorisierte [Werkzeugbaupläne]")
                || cleanTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools")
                || (cleanTitle.contains("Ausrüstung") && cleanTitle.contains("Auswählen"))
                || isNebenhandInventoryTitle(screen.getTitle().getString())) {
            return "blueprints";
        }
        return null;
    }
}

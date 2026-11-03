package net.felix.utilities.Town;

import net.felix.utilities.ItemViewer.ItemData;
import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Benutzerdefiniertes Kit (Name, Icon, Ausrüstungseinträge, Karten, Angel, Kristalle).
 */
public class CustomKit {

    public enum Origin {
        MANUAL,
        COPIED
    }

    public String id;
    public String name;
    public String iconItemId;
    public String iconItemName;
    /**
     * Abgeleitete Ausrüstungsnamen (Duplikate erlaubt). Laufzeitquelle sind {@link #equipmentEntries}.
     */
    public List<String> itemNames;
    public List<String> cardNames;
    public List<String> powerCrystalNames;
    public List<String> aspectNames;
    public List<FishingComponent> fishingComponents;
    public Map<String, String> itemCategories;
    public Origin origin;
    public List<EquipmentEntry> equipmentEntries;
    public List<ItemData> originalCards;
    public ItemData originalFishingRod;
    public boolean fishingRodComponentsEdited;
    public ItemData originalPowerCrystalOverview;
    public boolean powerCrystalOverviewEdited;

    public static final class FishingComponent {
        public String type;
        public String itemName;

        public FishingComponent() {
        }

        public FishingComponent(String type, String itemName) {
            this.type = type;
            this.itemName = itemName;
        }

        public FishingComponent copy() {
            return new FishingComponent(type, itemName);
        }
    }

    public static final class EquipmentEntry {
        public String jeiName;
        public String slotType;
        public Integer inventorySlot;
        public ItemData originalItem;
        public List<String> capturedAspectNames;

        public EquipmentEntry copy() {
            EquipmentEntry copy = new EquipmentEntry();
            copy.jeiName = jeiName;
            copy.slotType = slotType;
            copy.inventorySlot = inventorySlot;
            copy.originalItem = KitItemDataIo.copyShareItem(originalItem);
            copy.capturedAspectNames = capturedAspectNames == null ? null : new ArrayList<>(capturedAspectNames);
            return copy;
        }
    }

    public CustomKit() {
        this.id = UUID.randomUUID().toString();
        this.name = "Neues Kit";
        this.iconItemId = "minecraft:gold_nugget";
        this.itemNames = new ArrayList<>();
        this.cardNames = new ArrayList<>();
        this.powerCrystalNames = null;
        this.aspectNames = null;
        this.fishingComponents = new ArrayList<>();
        this.itemCategories = new HashMap<>();
        this.origin = Origin.MANUAL;
        this.equipmentEntries = new ArrayList<>();
        this.originalCards = new ArrayList<>();
    }

    public CustomKit(String id, String name, String iconItemId, List<String> itemNames) {
        this(id, name, iconItemId, null, itemNames, null, null);
    }

    public CustomKit(String id, String name, String iconItemId, String iconItemName, List<String> itemNames) {
        this(id, name, iconItemId, iconItemName, itemNames, null, null);
    }

    public CustomKit(
            String id,
            String name,
            String iconItemId,
            String iconItemName,
            List<String> itemNames,
            List<String> cardNames) {
        this(id, name, iconItemId, iconItemName, itemNames, cardNames, null);
    }

    public CustomKit(
            String id,
            String name,
            String iconItemId,
            String iconItemName,
            List<String> itemNames,
            List<String> cardNames,
            List<FishingComponent> fishingComponents) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name != null ? name : "Neues Kit";
        this.iconItemId = iconItemId != null && !iconItemId.isEmpty() ? iconItemId : "minecraft:gold_nugget";
        this.iconItemName = iconItemName;
        this.itemNames = itemNames != null ? new ArrayList<>(itemNames) : new ArrayList<>();
        this.cardNames = cardNames != null ? new ArrayList<>(cardNames) : new ArrayList<>();
        this.powerCrystalNames = null;
        this.aspectNames = null;
        this.fishingComponents = copyFishingComponents(fishingComponents);
        this.itemCategories = new HashMap<>();
        this.origin = Origin.MANUAL;
        this.equipmentEntries = new ArrayList<>();
        this.originalCards = new ArrayList<>();
    }

    public Origin resolvedOrigin() {
        return origin != null ? origin : Origin.MANUAL;
    }

    public boolean isCopied() {
        return resolvedOrigin() == Origin.COPIED;
    }

    public void ensureEquipmentEntries() {
        if (equipmentEntries == null) {
            equipmentEntries = new ArrayList<>();
        }
        if (!equipmentEntries.isEmpty()) {
            syncDerivedLists();
            return;
        }
        if (itemNames == null || itemNames.isEmpty()) {
            return;
        }
        for (String itemName : itemNames) {
            if (itemName == null || itemName.isEmpty()) {
                continue;
            }
            ItemData db = ItemViewerUtility.findItemByName(itemName);
            if (db == null) {
                db = ItemViewerUtility.findItemByNameFuzzy(itemName);
            }
            BuildSlotMapping.PieceType type = BuildSlotMapping.resolvePieceType(db);
            if (type == null && itemCategories != null) {
                type = pieceTypeFromLegacyCategory(itemCategories.get(itemName), db);
            }
            Integer slot = CustomKitSlotAssigner.firstFreeSlot(this, type);
            EquipmentEntry entry = new EquipmentEntry();
            entry.jeiName = itemName;
            entry.slotType = type != null ? type.getLabel() : null;
            entry.inventorySlot = slot;
            equipmentEntries.add(entry);
        }
        syncDerivedLists();
    }

    public void syncDerivedLists() {
        if (equipmentEntries == null) {
            equipmentEntries = new ArrayList<>();
        }
        List<String> names = new ArrayList<>();
        Map<String, String> cats = new HashMap<>();
        for (EquipmentEntry entry : equipmentEntries) {
            if (entry == null || entry.jeiName == null || entry.jeiName.isEmpty()) {
                continue;
            }
            names.add(entry.jeiName);
            BuildSlotMapping.PieceType type = entry.inventorySlot != null
                ? BuildSlotMapping.pieceTypeForSlot(entry.inventorySlot)
                : null;
            BuildSlotMapping.EquipmentCategory category = BuildSlotMapping.categoryForPiece(type);
            if (category != null) {
                cats.put(entry.jeiName, category.categoryKey());
            }
        }
        this.itemNames = names;
        this.itemCategories = cats;
    }

    public void rebuildAspectNamesFromEntries() {
        if (equipmentEntries == null) {
            return;
        }
        boolean anyCaptured = false;
        List<String> names = new ArrayList<>();
        for (EquipmentEntry entry : equipmentEntries) {
            if (entry == null || entry.capturedAspectNames == null) {
                continue;
            }
            anyCaptured = true;
            for (String aspect : entry.capturedAspectNames) {
                if (aspect != null && !aspect.isEmpty() && !names.contains(aspect)) {
                    names.add(aspect);
                }
            }
        }
        if (anyCaptured) {
            this.aspectNames = names;
        }
    }

    public ItemStack createIconStack() {
        if (iconItemName != null && !iconItemName.isEmpty()) {
            ItemStack fromViewer = ItemViewerUtility.createDisplayStackForItemName(iconItemName);
            if (!fromViewer.isEmpty()) {
                return fromViewer;
            }
        }
        Identifier id = Identifier.tryParse(iconItemId);
        if (id == null) {
            return new ItemStack(Items.GOLD_NUGGET);
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR) {
            return new ItemStack(Items.GOLD_NUGGET);
        }
        return new ItemStack(item);
    }

    public ItemStack createItemDisplayStack(String itemName) {
        return ItemViewerUtility.createDisplayStackForItemName(itemName);
    }

    public CustomKit copy() {
        CustomKit copy = new CustomKit(
            id,
            name,
            iconItemId,
            iconItemName,
            new ArrayList<>(itemNames),
            new ArrayList<>(cardNames != null ? cardNames : List.of()),
            copyFishingComponents(fishingComponents));
        copy.itemCategories = copyCategories(itemCategories);
        copy.powerCrystalNames = copyNullableStringList(powerCrystalNames);
        copy.aspectNames = copyNullableStringList(aspectNames);
        copy.origin = origin;
        copy.equipmentEntries = copyEntries(equipmentEntries);
        copy.originalCards = KitItemDataIo.copyShareItems(originalCards);
        copy.originalFishingRod = KitItemDataIo.copyShareItem(originalFishingRod);
        copy.fishingRodComponentsEdited = fishingRodComponentsEdited;
        copy.originalPowerCrystalOverview = KitItemDataIo.copyShareItem(originalPowerCrystalOverview);
        copy.powerCrystalOverviewEdited = powerCrystalOverviewEdited;
        return copy;
    }

    private static List<FishingComponent> copyFishingComponents(List<FishingComponent> source) {
        List<FishingComponent> out = new ArrayList<>();
        if (source == null) {
            return out;
        }
        for (FishingComponent c : source) {
            if (c != null) {
                out.add(c.copy());
            }
        }
        return out;
    }

    private static List<EquipmentEntry> copyEntries(List<EquipmentEntry> source) {
        List<EquipmentEntry> out = new ArrayList<>();
        if (source == null) {
            return out;
        }
        for (EquipmentEntry entry : source) {
            if (entry != null) {
                out.add(entry.copy());
            }
        }
        return out;
    }

    private static Map<String, String> copyCategories(Map<String, String> source) {
        return source != null ? new HashMap<>(source) : new HashMap<>();
    }

    private static List<String> copyNullableStringList(List<String> source) {
        return source == null ? null : new ArrayList<>(source);
    }

    private static BuildSlotMapping.PieceType pieceTypeFromLegacyCategory(String category, ItemData data) {
        BuildSlotMapping.PieceType resolved = BuildSlotMapping.resolvePieceType(data);
        if (resolved != null) {
            return resolved;
        }
        if (category == null) {
            return null;
        }
        return switch (category.toLowerCase()) {
            case "weapons" -> BuildSlotMapping.PieceType.WEAPON;
            case "modules" -> BuildSlotMapping.PieceType.MODULE;
            case "tools" -> null;
            default -> null;
        };
    }
}

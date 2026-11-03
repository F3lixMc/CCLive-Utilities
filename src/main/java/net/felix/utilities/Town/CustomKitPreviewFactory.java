package net.felix.utilities.Town;

import net.felix.chat.BuildShareExtraParser;
import net.felix.chat.BuildShareManager;
import net.felix.chat.BuildShareManager.CategoryBucket;
import net.felix.chat.ChatTooltipLine;
import net.felix.utilities.ItemViewer.ItemData;
import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.felix.utilities.Town.BuildSlotMapping.EquipmentCategory;
import net.felix.utilities.Town.BuildSlotMapping.PieceType;
import net.felix.utilities.Town.CustomKit.EquipmentEntry;
import net.felix.utilities.Town.CustomKit.FishingComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Baut eine Build-Share-Vorschau aus einem lokalen CustomKit.
 */
public final class CustomKitPreviewFactory {

    private CustomKitPreviewFactory() {
    }

    public static BuildShareManager.BuildSnapshot fromKit(CustomKit kit) {
        BuildShareManager.BuildSnapshot snapshot = new BuildShareManager.BuildSnapshot();
        if (kit == null) {
            return snapshot;
        }
        kit.ensureEquipmentEntries();
        snapshot.kitName = kit.name;
        snapshot.equipmentRecognized = true;
        snapshot.cardsRecognized = kit.cardNames != null && !kit.cardNames.isEmpty();
        snapshot.modulesScanned = true;
        snapshot.equipment = new LinkedHashMap<>();
        for (EquipmentCategory category : EquipmentCategory.values()) {
            snapshot.equipment.put(category.categoryKey(), emptyBucket(category));
        }

        if (kit.equipmentEntries != null) {
            for (EquipmentEntry entry : kit.equipmentEntries) {
                if (entry == null || entry.inventorySlot == null) {
                    continue;
                }
                PieceType type = BuildSlotMapping.pieceTypeForSlot(entry.inventorySlot);
                EquipmentCategory category = BuildSlotMapping.categoryForPiece(type);
                if (category == null) {
                    continue;
                }
                CategoryBucket bucket = snapshot.equipment.get(category.categoryKey());
                ItemData display = displayForEntry(entry);
                if (display == null) {
                    continue;
                }
                placeFilled(bucket, entry.inventorySlot, display, category);
            }
        }

        ItemData rod = fishingRodDisplay(kit);
        if (rod != null) {
            CategoryBucket tools = snapshot.equipment.get(EquipmentCategory.TOOLS.categoryKey());
            placeFilled(tools, BuildSlotMapping.FISHING_ROD_SLOT, rod, EquipmentCategory.TOOLS);
        }

        snapshot.powerCrystalNames = kit.powerCrystalNames == null ? null : new ArrayList<>(kit.powerCrystalNames);
        snapshot.aspectNames = kit.aspectNames == null ? null : new ArrayList<>(kit.aspectNames);
        snapshot.powerCrystalOverview = powerCrystalOverviewDisplay(kit);

        snapshot.cards = emptyCardBucket();
        if (kit.cardNames != null) {
            for (int i = 0; i < kit.cardNames.size(); i++) {
                ItemData card = cardDisplay(kit, i);
                if (card == null) {
                    continue;
                }
                snapshot.cards.items.add(card);
                snapshot.cards.filled++;
            }
        }
        snapshot.cards.total = BuildSlotMapping.getCardCapacity();
        BuildShareManager.normalizeSnapshot(snapshot);
        return snapshot;
    }

    private static ItemData displayForEntry(EquipmentEntry entry) {
        if (entry.originalItem != null) {
            return KitItemDataIo.copyShareItem(entry.originalItem);
        }
        return jeiDisplay(entry.jeiName);
    }

    private static ItemData fishingRodDisplay(CustomKit kit) {
        boolean hasComponents = kit.fishingComponents != null && !kit.fishingComponents.isEmpty();
        if (kit.originalFishingRod != null && !kit.fishingRodComponentsEdited) {
            return KitItemDataIo.copyShareItem(kit.originalFishingRod);
        }
        if (!hasComponents && kit.originalFishingRod == null) {
            return null;
        }
        ItemData rod = kit.originalFishingRod != null
            ? KitItemDataIo.copyShareItem(kit.originalFishingRod)
            : baseFishingRod();
        rod.chatTooltip = rebuildFishingTooltip(rod, kit.fishingComponents);
        return rod;
    }

    private static ItemData powerCrystalOverviewDisplay(CustomKit kit) {
        if (kit.powerCrystalNames == null || kit.powerCrystalNames.isEmpty()) {
            return null;
        }
        if (kit.originalPowerCrystalOverview != null && !kit.powerCrystalOverviewEdited) {
            ItemData original = KitItemDataIo.copyShareItem(kit.originalPowerCrystalOverview);
            BuildShareExtraParser.stripUnconfirmedCrystalLines(original, kit.powerCrystalNames);
            return original;
        }
        ItemData overview = kit.originalPowerCrystalOverview != null
            ? KitItemDataIo.copyShareItem(kit.originalPowerCrystalOverview)
            : baseCrystalOverview();
        overview.chatTooltip = rebuildCrystalTooltip(kit.powerCrystalNames);
        return overview;
    }

    private static ItemData cardDisplay(CustomKit kit, int index) {
        if (kit.originalCards != null && index < kit.originalCards.size() && kit.originalCards.get(index) != null) {
            return KitItemDataIo.copyShareItem(kit.originalCards.get(index));
        }
        String name = kit.cardNames.get(index);
        ItemData item = new ItemData();
        item.name = name;
        item.id = "minecraft:paper";
        ChatTooltipLine line = new ChatTooltipLine();
        ChatTooltipLine.ChatTooltipPart part = new ChatTooltipLine.ChatTooltipPart();
        part.text = name != null ? name : "Karte";
        line.parts.add(part);
        item.chatTooltip = List.of(line);
        return item;
    }

    private static ItemData jeiDisplay(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        ItemData db = ItemViewerUtility.findItemByName(name);
        if (db == null) {
            db = ItemViewerUtility.findItemByNameFuzzy(name);
        }
        if (db == null) {
            ItemData fallback = new ItemData();
            fallback.name = name;
            fallback.id = "minecraft:paper";
            return fallback;
        }
        return db;
    }

    private static ItemData baseFishingRod() {
        ItemData rod = new ItemData();
        rod.id = "minecraft:fishing_rod";
        rod.name = "Angelrute";
        return rod;
    }

    private static ItemData baseCrystalOverview() {
        ItemData item = new ItemData();
        item.id = "minecraft:name_tag";
        item.name = "Machtkristalle";
        item.customModelData = 2650;
        return item;
    }

    static List<ChatTooltipLine> rebuildFishingTooltip(ItemData rod, List<FishingComponent> components) {
        List<ChatTooltipLine> lines = new ArrayList<>();
        lines.add(plainLine(rod != null && rod.name != null ? rod.name : "Angelrute"));
        lines.add(plainLine(""));
        lines.add(plainLine("Komponenten"));
        if (components != null) {
            for (String type : BuildSlotMapping.FISHING_COMPONENT_TYPES) {
                FishingComponent match = findComponent(components, type);
                if (match == null) {
                    continue;
                }
                String name = match.itemName != null ? match.itemName : "";
                lines.add(plainLine(name + " [" + type + "]"));
            }
        }
        return lines;
    }

    static List<ChatTooltipLine> rebuildCrystalTooltip(List<String> names) {
        List<ChatTooltipLine> lines = new ArrayList<>();
        lines.add(plainLine(BuildShareExtraParser.EQUIPPED_CRYSTALS_HEADER));
        if (names != null) {
            for (String name : names) {
                if (name != null && !name.isEmpty()) {
                    lines.add(plainLine("• " + name));
                }
            }
        }
        return lines;
    }

    private static FishingComponent findComponent(List<FishingComponent> components, String type) {
        for (FishingComponent component : components) {
            if (component != null && type.equalsIgnoreCase(component.type)) {
                return component;
            }
        }
        return null;
    }

    private static ChatTooltipLine plainLine(String text) {
        ChatTooltipLine line = new ChatTooltipLine();
        ChatTooltipLine.ChatTooltipPart part = new ChatTooltipLine.ChatTooltipPart();
        part.text = text != null ? text : "";
        line.parts.add(part);
        return line;
    }

    private static CategoryBucket emptyBucket(EquipmentCategory category) {
        CategoryBucket bucket = new CategoryBucket();
        int[] slots = category.getSlots();
        bucket.total = slots.length;
        bucket.filled = 0;
        bucket.items = new ArrayList<>();
        bucket.slotIndices = new ArrayList<>();
        bucket.slotStatuses = new ArrayList<>();
        bucket.slotLabels = new ArrayList<>();
        bucket.statusesSlotIndices = new ArrayList<>();
        for (int slot : slots) {
            bucket.slotStatuses.add("empty");
            bucket.slotLabels.add(BuildSlotMapping.labelForSlot(slot));
            bucket.statusesSlotIndices.add(slot);
        }
        return bucket;
    }

    private static CategoryBucket emptyCardBucket() {
        CategoryBucket cards = new CategoryBucket();
        cards.items = new ArrayList<>();
        cards.slotIndices = new ArrayList<>();
        cards.slotStatuses = new ArrayList<>();
        cards.slotLabels = new ArrayList<>();
        cards.statusesSlotIndices = new ArrayList<>();
        cards.filled = 0;
        cards.total = BuildSlotMapping.getCardCapacity();
        return cards;
    }

    private static void placeFilled(CategoryBucket bucket, int slot, ItemData item, EquipmentCategory category) {
        if (bucket == null || item == null) {
            return;
        }
        int[] slots = category.getSlots();
        int index = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        while (bucket.slotStatuses.size() <= index) {
            bucket.slotStatuses.add("empty");
        }
        bucket.slotStatuses.set(index, "filled");
        bucket.items.add(item);
        bucket.slotIndices.add(slot);
        bucket.filled = 0;
        for (String status : bucket.slotStatuses) {
            if ("filled".equals(status)) {
                bucket.filled++;
            }
        }
    }
}

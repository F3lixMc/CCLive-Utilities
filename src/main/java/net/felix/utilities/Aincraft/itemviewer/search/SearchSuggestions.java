package net.felix.utilities.Aincraft.itemviewer.search;

import java.util.Locale;
import net.felix.utilities.Aincraft.itemviewer.data.CostItem;
import net.felix.utilities.Aincraft.itemviewer.data.ItemData;
import net.felix.utilities.Aincraft.itemviewer.data.PriceData;

import static net.felix.utilities.Aincraft.itemviewer.data.ItemCatalog.allItems;

/**
 * Vorschlagslisten fuer die Suche: bekannte Tags, Kosten-Items, Materialien und Ressourcen.
 */
public final class SearchSuggestions {

    private SearchSuggestions() {
    }

    public static java.util.Set<String> knownSearchTags = null;

    public static java.util.Set<String> knownCostItemNames = null;

    public static java.util.List<String> knownMaterialNames = null;

    public static java.util.List<String> knownResourceNames = null;

    public static java.util.List<CostNamePickerEntry> knownMaterialPickerEntries = null;

    public static java.util.List<CostNamePickerEntry> knownResourcePickerEntries = null;

    private static final java.util.List<CostNamePickerEntry> AMBOSS_PICKER_ENTRIES = buildStaticPickerEntries(
            "Spinnfaden", "Stoff", "Ziegelstein", "Kettenglied", "Leder", "Zahnrad", "Klammer", "Schraube",
            "Stabile Schnur", "Kupferplatte", "Gehärteter Ziegelstein", "Eisenplatte", "Verstärkte Schraube",
            "Verbessertes Zahnrad");

    private static final java.util.List<CostNamePickerEntry> OFEN_PICKER_ENTRIES = buildStaticPickerEntries(
            "Kupferbarren", "Eisenbarren", "Goldbarren", "Netherite Schrott");

    /**
     * Alle bekannten Tags für die #Tag-Suche (item.tags, info.type/piece/rarity).
     * Wird genutzt um exakte vs. Teilstring-Suche zu unterscheiden.
     */
    public static java.util.Set<String> getKnownSearchTags() {
        if (knownSearchTags == null) {
            rebuildKnownSearchTags();
        }
        return knownSearchTags;
    }

    public static void rebuildKnownSearchTags() {
        java.util.Set<String> tags = new java.util.HashSet<>();
        if (allItems != null) {
            for (ItemData item : allItems) {
                collectSearchTagsFromItem(item, tags);
            }
        }
        knownSearchTags = java.util.Collections.unmodifiableSet(tags);
    }

    /**
     * Alle bekannten Kosten-Item-Namen (ressource:, material:, amboss:, etc.).
     * Vollständiger Name → exakter Match; Prefix → Teilstring (Live-Suche).
     */
    public static java.util.Set<String> getKnownCostItemNames() {
        if (knownCostItemNames == null) {
            rebuildKnownCostItemNames();
        }
        return knownCostItemNames;
    }

    public static java.util.List<CostNamePickerEntry> getKnownMaterialPickerEntries() {
        if (knownMaterialPickerEntries == null) {
            rebuildKnownCostItemNames();
        }
        return knownMaterialPickerEntries;
    }

    public static java.util.List<CostNamePickerEntry> getKnownResourcePickerEntries() {
        if (knownResourcePickerEntries == null) {
            rebuildKnownCostItemNames();
        }
        return knownResourcePickerEntries;
    }

    public static java.util.List<CostNamePickerEntry> getKnownAmbossPickerEntries() {
        return AMBOSS_PICKER_ENTRIES;
    }

    public static java.util.List<CostNamePickerEntry> getKnownOfenPickerEntries() {
        return OFEN_PICKER_ENTRIES;
    }

    public static java.util.List<String> getKnownMaterialNames() {
        if (knownMaterialNames == null) {
            rebuildKnownCostItemNames();
        }
        return knownMaterialNames;
    }

    public static java.util.List<String> getKnownResourceNames() {
        if (knownResourceNames == null) {
            rebuildKnownCostItemNames();
        }
        return knownResourceNames;
    }

    public static void rebuildKnownCostItemNames() {
        java.util.Set<String> names = new java.util.HashSet<>();
        java.util.Map<String, String> materials = new java.util.LinkedHashMap<>();
        java.util.Map<String, String> resources = new java.util.LinkedHashMap<>();
        if (allItems != null) {
            for (ItemData item : allItems) {
                collectCostItemNamesFromItem(item, names, materials, resources);
            }
        }
        knownCostItemNames = java.util.Collections.unmodifiableSet(names);
        knownMaterialPickerEntries = buildMaterialPickerEntries(materials);
        knownResourcePickerEntries = buildResourcePickerEntries(resources);
        knownMaterialNames = java.util.Collections.unmodifiableList(
                knownMaterialPickerEntries.stream().map(CostNamePickerEntry::name).toList());
        knownResourceNames = java.util.Collections.unmodifiableList(
                knownResourcePickerEntries.stream().map(CostNamePickerEntry::name).toList());
    }

    private static final java.util.List<String> FISH_PART_ORDER = java.util.List.of(
            "fleisch", "schuppen", "flosse", "kopf", "wirbelsäule");

    private static java.util.List<CostNamePickerEntry> buildMaterialPickerEntries(
            java.util.Map<String, String> materials) {
        java.util.List<MaterialPickerSortable> dungeonMaterials = new java.util.ArrayList<>();
        java.util.List<MaterialPickerSortable> prismarinMaterials = new java.util.ArrayList<>();
        java.util.List<FishingMaterialPickerSortable> fishingMaterials = new java.util.ArrayList<>();
        for (String name : materials.values()) {
            net.felix.utilities.Aincraft.materials.data.MaterialFloorInfo info =
                    net.felix.utilities.Aincraft.materials.data.MaterialDatabase.getMaterialFloorInfo(name);
            int color = info != null
                    ? net.felix.utilities.Aincraft.materials.data.MaterialRarityColors.getMaterialLocationRarityColorArgb(info)
                    : 0xFFFFFFFF;
            if (net.felix.utilities.Aincraft.fishing.ponds.FishingMaterials.isFishingRarityMaterial(name)) {
                String family = net.felix.utilities.Aincraft.fishing.ponds.FishingMaterials.getFishFamilyFromMaterial(name);
                String rarity = info != null && info.rarity != null
                        ? info.rarity
                        : net.felix.utilities.Aincraft.fishing.ponds.FishingMaterials.getFishingMaterialRarityKey(name);
                String part = net.felix.utilities.Aincraft.fishing.ponds.FishingMaterials.getFishPartSuffixFromMaterial(
                        name, family);
                int familyOrder = net.felix.utilities.Aincraft.fishing.ponds.FishingMaterials.getFishFamilyDisplayOrderIndex(
                        family);
                fishingMaterials.add(new FishingMaterialPickerSortable(name, familyOrder, rarity, part, color));
            } else if (net.felix.utilities.Aincraft.materials.data.PrismarinMaterials.isPrismarinMaterial(name)) {
                String rarity = info != null && info.rarity != null ? info.rarity : "common";
                prismarinMaterials.add(new MaterialPickerSortable(name, 0, rarity, color));
            } else {
                int floor = info != null ? info.floor : 9999;
                String rarity = info != null && info.rarity != null ? info.rarity : "common";
                dungeonMaterials.add(new MaterialPickerSortable(name, floor, rarity, color));
            }
        }
        dungeonMaterials.sort(java.util.Comparator
                .<MaterialPickerSortable>comparingInt(entry -> materialFloorSortKey(entry.floor()))
                .thenComparingInt(entry -> raritySortOrder(entry.rarity()))
                .thenComparing(MaterialPickerSortable::name, String.CASE_INSENSITIVE_ORDER));
        prismarinMaterials.sort(java.util.Comparator
                .comparingInt((MaterialPickerSortable entry) -> raritySortOrder(entry.rarity()))
                .thenComparing(MaterialPickerSortable::name, String.CASE_INSENSITIVE_ORDER));
        fishingMaterials.sort(java.util.Comparator
                .<FishingMaterialPickerSortable>comparingInt(FishingMaterialPickerSortable::familyOrder)
                .thenComparingInt(entry -> raritySortOrder(entry.rarity()))
                .thenComparingInt(entry -> fishPartSortOrder(entry.part()))
                .thenComparing(FishingMaterialPickerSortable::name, String.CASE_INSENSITIVE_ORDER));
        java.util.List<CostNamePickerEntry> entries = new java.util.ArrayList<>(
                dungeonMaterials.size() + prismarinMaterials.size() + fishingMaterials.size());
        for (MaterialPickerSortable entry : dungeonMaterials) {
            int displayFloor = entry.floor() > 0 && entry.floor() < 9999 ? entry.floor() : 0;
            entries.add(new CostNamePickerEntry(entry.name(), entry.color(), displayFloor));
        }
        for (MaterialPickerSortable entry : prismarinMaterials) {
            entries.add(new CostNamePickerEntry(entry.name(), entry.color(), 0));
        }
        for (FishingMaterialPickerSortable entry : fishingMaterials) {
            entries.add(new CostNamePickerEntry(entry.name(), entry.color(), 0));
        }
        return java.util.Collections.unmodifiableList(entries);
    }

    private static java.util.List<CostNamePickerEntry> buildResourcePickerEntries(
            java.util.Map<String, String> resources) {
        java.util.List<String> sorted = sortedCostNames(resources);
        java.util.List<CostNamePickerEntry> entries = new java.util.ArrayList<>(sorted.size());
        for (String name : sorted) {
            entries.add(new CostNamePickerEntry(name, 0xFFFFFFFF, 0));
        }
        return java.util.Collections.unmodifiableList(entries);
    }

    private static java.util.List<CostNamePickerEntry> buildStaticPickerEntries(String... names) {
        java.util.List<CostNamePickerEntry> entries = new java.util.ArrayList<>(names.length);
        for (String name : names) {
            entries.add(new CostNamePickerEntry(name, 0xFFFFFFFF, 0));
        }
        return java.util.Collections.unmodifiableList(entries);
    }

    private static int materialFloorSortKey(int floor) {
        return floor > 0 ? floor : Integer.MAX_VALUE;
    }

    private static int fishPartSortOrder(String partSuffix) {
        if (partSuffix == null || partSuffix.isEmpty()) {
            return 99;
        }
        int index = FISH_PART_ORDER.indexOf(partSuffix.toLowerCase(java.util.Locale.ROOT));
        return index >= 0 ? index : 99;
    }

    private static int raritySortOrder(String rarity) {
        if (rarity == null) {
            return 99;
        }
        return switch (rarity.toLowerCase(java.util.Locale.ROOT)) {
            case "common" -> 0;
            case "uncommon" -> 1;
            case "rare" -> 2;
            case "epic" -> 3;
            case "legendary" -> 4;
            default -> 99;
        };
    }

    private static java.util.List<String> sortedCostNames(java.util.Map<String, String> names) {
        java.util.List<String> sorted = new java.util.ArrayList<>(names.values());
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }

    private static void addCostItemName(java.util.Set<String> names, CostItem costItem) {
        if (costItem != null && costItem.itemName != null && !costItem.itemName.isEmpty()) {
            names.add(costItem.itemName.toLowerCase());
        }
    }

    private static void addCostItemName(java.util.Map<String, String> names, CostItem costItem) {
        if (costItem != null && costItem.itemName != null && !costItem.itemName.isEmpty()) {
            names.putIfAbsent(costItem.itemName.toLowerCase(java.util.Locale.ROOT), costItem.itemName);
        }
    }

    private static void collectCostItemNamesFromItem(ItemData item, java.util.Set<String> names,
            java.util.Map<String, String> materials, java.util.Map<String, String> resources) {
        if (item.price == null) {
            return;
        }
        PriceData price = item.price;
        addCostItemName(names, price.coin);
        addCostItemName(names, price.cactus);
        addCostItemName(names, price.soul);
        addCostItemName(materials, price.material1);
        addCostItemName(materials, price.material2);
        addCostItemName(materials, price.material3);
        addCostItemName(materials, price.material4);
        addCostItemName(materials, price.material5);
        addCostItemName(names, price.material1);
        addCostItemName(names, price.material2);
        addCostItemName(names, price.material3);
        addCostItemName(names, price.material4);
        addCostItemName(names, price.material5);
        addCostItemName(names, price.Amboss);
        addCostItemName(names, price.amboss);
        addCostItemName(resources, price.Ressource);
        addCostItemName(resources, price.ressource);
        addCostItemName(names, price.Ressource);
        addCostItemName(names, price.ressource);
        addCostItemName(names, price.Level);
        addCostItemName(names, price.paper_shreds);
        addCostItemName(names, price.time);
    }

    private static void collectSearchTagsFromItem(ItemData item, java.util.Set<String> tags) {
        if (item.tags != null) {
            for (String tag : item.tags) {
                if (tag != null && !tag.isEmpty()) {
                    tags.add(tag.toLowerCase());
                }
            }
        }
        if (item.info != null) {
            if (item.info.type != null && !item.info.type.isEmpty()) {
                tags.add(item.info.type.toLowerCase());
            }
            if (item.info.piece != null && !item.info.piece.isEmpty()) {
                tags.add(item.info.piece.toLowerCase());
            }
            if (item.info.rarity != null && !item.info.rarity.isEmpty()) {
                tags.add(item.info.rarity.toLowerCase());
            }
        }
    }

    /**
     * Normalisiert Such-Tags (z. B. Plural-Aliase).
     */
    public static String normalizeSearchTag(String searchTag) {
        if (searchTag == null || searchTag.isEmpty()) {
            return searchTag;
        }
        String normalized = searchTag.toLowerCase(Locale.ROOT);
        if ("werkzeuge".equals(normalized)) {
            return "werkzeug";
        }
        return normalized;
    }

    /**
     * Prüft ob ein Item-Tag zur #Tag-Suche passt.
     * Vollständiger Tag (z.B. #Schuhe) → exakter Match; Prefix (z.B. #Schuh) → Teilstring.
     */
    public static boolean matchesSearchTag(String itemTag, String searchTag) {
        if (itemTag == null || searchTag == null || searchTag.isEmpty()) {
            return false;
        }
        String itemTagLower = itemTag.toLowerCase(Locale.ROOT);
        String searchTagLower = normalizeSearchTag(searchTag);
        if (getKnownSearchTags().contains(searchTagLower)) {
            return itemTagLower.equals(searchTagLower);
        }
        return itemTagLower.contains(searchTagLower);
    }

    /**
     * Prüft ob ein Kosten-Item-Name zur Suche passt (z.B. ressource:Eichenholz).
     * Vollständiger bekannter Name → exakt; Prefix → Teilstring.
     */
    public static boolean matchesCostItemName(String itemCostName, String searchName) {
        if (itemCostName == null || searchName == null || searchName.isEmpty()) {
            return false;
        }
        String itemNameLower = itemCostName.toLowerCase();
        String searchNameLower = searchName.toLowerCase();
        if (getKnownCostItemNames().contains(searchNameLower)) {
            return itemNameLower.equals(searchNameLower);
        }
        return itemNameLower.contains(searchNameLower);
    }
}

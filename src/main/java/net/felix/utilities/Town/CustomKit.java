package net.felix.utilities.Town;

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
 * Benutzerdefiniertes Kit (Name, Icon, Item-Liste, optionale Karten und Angelkomponenten).
 */
public class CustomKit {

    public String id;
    public String name;
    public String iconItemId;
    public String iconItemName;
    public List<String> itemNames;
    /** Anzeigenamen der Karten (nicht in JEI-Filter / items.json). */
    public List<String> cardNames;
    /** Angelkomponenten (Typ + Itemname); nicht in der normalen Ausrüstungsliste. */
    public List<FishingComponent> fishingComponents;
    /**
     * Optional: Itemname → Kategorie ({@code armor}/{@code weapons}/{@code tools}/{@code modules}).
     * Alte Kits ohne Map werden per ItemViewer klassifiziert.
     */
    public Map<String, String> itemCategories;

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

    public CustomKit() {
        this.id = UUID.randomUUID().toString();
        this.name = "Neues Kit";
        this.iconItemId = "minecraft:gold_nugget";
        this.itemNames = new ArrayList<>();
        this.cardNames = new ArrayList<>();
        this.fishingComponents = new ArrayList<>();
        this.itemCategories = new HashMap<>();
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
        this.fishingComponents = copyFishingComponents(fishingComponents);
        this.itemCategories = new HashMap<>();
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

    private static Map<String, String> copyCategories(Map<String, String> source) {
        return source != null ? new HashMap<>(source) : new HashMap<>();
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
        return copy;
    }
}

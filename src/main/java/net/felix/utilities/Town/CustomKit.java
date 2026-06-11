package net.felix.utilities.Town;

import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Benutzerdefiniertes Kit (Name, Icon, Item-Liste).
 */
public class CustomKit {

    public String id;
    public String name;
    public String iconItemId;
    public String iconItemName;
    public List<String> itemNames;

    public CustomKit() {
        this.id = UUID.randomUUID().toString();
        this.name = "Neues Kit";
        this.iconItemId = "minecraft:gold_nugget";
        this.itemNames = new ArrayList<>();
    }

    public CustomKit(String id, String name, String iconItemId, List<String> itemNames) {
        this(id, name, iconItemId, null, itemNames);
    }

    public CustomKit(String id, String name, String iconItemId, String iconItemName, List<String> itemNames) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name != null ? name : "Neues Kit";
        this.iconItemId = iconItemId != null && !iconItemId.isEmpty() ? iconItemId : "minecraft:gold_nugget";
        this.iconItemName = iconItemName;
        this.itemNames = itemNames != null ? new ArrayList<>(itemNames) : new ArrayList<>();
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
        Item item = Registries.ITEM.get(id);
        if (item == null || item == Items.AIR) {
            return new ItemStack(Items.GOLD_NUGGET);
        }
        return new ItemStack(item);
    }

    public ItemStack createItemDisplayStack(String itemName) {
        return ItemViewerUtility.createDisplayStackForItemName(itemName);
    }

    public CustomKit copy() {
        CustomKit copy = new CustomKit(id, name, iconItemId, iconItemName, new ArrayList<>(itemNames));
        return copy;
    }
}

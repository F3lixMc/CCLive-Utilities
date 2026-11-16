package net.felix.utilities.Overall.itemviewer.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.felix.utilities.Overall.itemviewer.data.ItemData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import static net.felix.utilities.Overall.itemviewer.tooltip.TooltipCache.invalidateTooltipCache;
import static net.felix.utilities.Overall.itemviewer.tooltip.TooltipColors.getRarityColor;

/**
 * Erzeugt und cached ItemStacks und Texturen fuer die Item-Icons im Viewer.
 */
public final class ItemStackCache {

    private ItemStackCache() {
    }

    private static final Map<ItemData, ItemStack> ITEM_STACK_CACHE = new WeakHashMap<>();

    private static final Map<String, Identifier> TEXTURE_ID_CACHE = new HashMap<>();

    /** Immer volles {@code drawItem} — flache Sprites zeigen bei CMD-/3D-Modellen oft das Basis-Item (z. B. Buch). */
    static void renderItemStackGuiIcon(GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
        context.item(stack, x, y, 0);
    }

    static Identifier getTextureId(String texturePath) {
        return TEXTURE_ID_CACHE.computeIfAbsent(texturePath,
            path -> Identifier.fromNamespaceAndPath("cclive-utilities", "textures/" + path));
    }

    public static void clearItemStackCache() {
        ITEM_STACK_CACHE.clear();
        TEXTURE_ID_CACHE.clear();
        invalidateTooltipCache();
    }

    public static ItemStack createDisplayStack(ItemData itemData) {
        return getCachedItemStack(itemData);
    }

    static ItemStack getCachedItemStack(ItemData itemData) {
        if (itemData == null) {
            return ItemStack.EMPTY;
        }
        ItemStack cached = ITEM_STACK_CACHE.get(itemData);
        if (cached != null) {
            return cached;
        }
        ItemStack stack = createItemStack(itemData);
        if (!stack.isEmpty()) {
            ITEM_STACK_CACHE.put(itemData, stack);
        }
        return stack;
    }

    /**
     * Erstellt einen ItemStack aus ItemData
     */
    private static ItemStack createItemStack(ItemData itemData) {
        if (itemData == null || itemData.id == null) {
            return ItemStack.EMPTY;
        }
        
        try {
            // Parse Item-ID
            Identifier itemId = Identifier.tryParse(itemData.id);
            if (itemId == null) {
                return ItemStack.EMPTY;
            }
            
            // Finde Item in Registry
            var itemEntryOpt = BuiltInRegistries.ITEM.get(itemId);
            if (itemEntryOpt.isEmpty()) {
                return ItemStack.EMPTY;
            }
            
            Item item = itemEntryOpt.get().value();
            ItemStack stack = new ItemStack(item);
            
            // Setze Custom Name falls vorhanden
            if (itemData.name != null && !itemData.name.isEmpty()) {
                int nameColor;
                // Power Crystals haben immer aqua als Name-Farbe
                if (itemData.info != null && itemData.info.power_crystal != null && itemData.info.power_crystal) {
                    nameColor = 0xFF00FFFF; // aqua
                } else if (itemData.info != null && itemData.info.ability != null && itemData.info.ability) {
                    // Abilities haben immer Minecraft-Rot als Name-Farbe
                    nameColor = 0xFFFF5555; // Minecraft-Rot (FF5555)
                } else {
                    // Bestimme die Farbe basierend auf der Rarity
                    nameColor = getRarityColor(itemData.info != null ? itemData.info.rarity : null);
                }
                
                // Erstelle Text mit Farbe und ohne kursiv
                Component nameText = Component.literal(itemData.name)
                    .setStyle(Style.EMPTY.withColor(nameColor).withItalic(false));
                
                stack.set(DataComponents.CUSTOM_NAME, nameText);
            }
            
            // CustomModelData (floats/flags/strings/colors) — Strings oft für Platzhalter-Modelle
            boolean hasCmd = itemData.customModelData != null
                || (itemData.customModelStrings != null && !itemData.customModelStrings.isEmpty())
                || (itemData.customModelFlags != null && !itemData.customModelFlags.isEmpty())
                || (itemData.customModelColors != null && !itemData.customModelColors.isEmpty());
            if (hasCmd) {
                List<Float> floats = itemData.customModelData != null
                    ? List.of(itemData.customModelData.floatValue())
                    : List.of();
                List<Boolean> flags = itemData.customModelFlags != null
                    ? itemData.customModelFlags : List.of();
                List<String> strings = itemData.customModelStrings != null
                    ? itemData.customModelStrings : List.of();
                List<Integer> colors = itemData.customModelColors != null
                    ? itemData.customModelColors : List.of();
                stack.set(DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(floats, flags, strings, colors));
            }
            
            return stack;
        } catch (Exception e) {
            // Falls Fehler beim Erstellen des ItemStacks
            return ItemStack.EMPTY;
        }
    }

    /**
     * Prüft ob es sich um einen "Passiver Fähigkeits Slot" handelt
     */
    static boolean isPassiveSkillSlot(ItemData itemData) {
        if (itemData == null || itemData.name == null) {
            return false;
        }
        return itemData.name.equals("Passiver Fähigkeits Slot");
    }

    /**
     * Prüft ob es sich um einen Karten Slot handelt (basierend auf category)
     */
    static boolean isCardSlot(ItemData itemData) {
        if (itemData == null) {
            return false;
        }
        return "card_slots".equals(itemData.category);
    }
}

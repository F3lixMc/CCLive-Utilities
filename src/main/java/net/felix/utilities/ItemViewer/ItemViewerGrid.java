package net.felix.utilities.ItemViewer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Rendert Items in einem Grid-Layout (ähnlich JEI)
 */
public class ItemViewerGrid {
    
    private static final int SLOT_SIZE = 18; // 16x16 Item + 1px Padding auf jeder Seite
    private static final int GRID_COLUMNS = 6; // 6 Spalten
    private static final int GRID_ROWS = 8; // 8 Zeilen
    private static final int ITEMS_PER_PAGE = GRID_COLUMNS * GRID_ROWS; // 48 Items
    
    private final List<ItemData> items;
    private final int startX;
    private final int startY;
    private final int mouseX;
    private final int mouseY;
    
    private ItemData hoveredItem = null;
    
    public ItemViewerGrid(List<ItemData> items, int startX, int startY, int mouseX, int mouseY) {
        this.items = items;
        this.startX = startX;
        this.startY = startY;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
    
    /**
     * Rendert das Grid mit allen Items
     */
    public void render(DrawContext context) {
        // Rendere alle Slots
        for (int i = 0; i < Math.min(items.size(), ITEMS_PER_PAGE); i++) {
            int row = i / GRID_COLUMNS;
            int col = i % GRID_COLUMNS;
            
            int slotX = startX + col * SLOT_SIZE;
            int slotY = startY + row * SLOT_SIZE;
            
            ItemData item = items.get(i);
            
            // Prüfe ob Maus über diesem Slot ist
            boolean isHovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                               mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
            
            if (isHovered) {
                hoveredItem = item;
            }
            
            // Rendere Slot-Hintergrund
            renderSlot(context, slotX, slotY, isHovered);
            
            // Rendere Item
            ItemStack itemStack = createItemStack(item);
            if (!itemStack.isEmpty()) {
                context.drawItem(itemStack, slotX + 1, slotY + 1, 0);
            }
        }
    }
    
    /**
     * Rendert Tooltip für das gehoverte Item
     */
    public void renderTooltip(DrawContext context) {
        if (hoveredItem != null) {
            ItemStack itemStack = createItemStack(hoveredItem);
            if (!itemStack.isEmpty()) {
                context.drawItemTooltip(
                    MinecraftClient.getInstance().textRenderer,
                    itemStack,
                    mouseX,
                    mouseY
                );
            }
        }
    }
    
    /**
     * Rendert einen einzelnen Slot
     */
    private void renderSlot(DrawContext context, int x, int y, boolean isHovered) {
        // Slot-Hintergrund
        int backgroundColor = isHovered ? 0x80FFFFFF : 0x40000000;
        context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, backgroundColor);
        
        // Slot-Rahmen
        int borderColor = isHovered ? 0xFFFFFFFF : 0xFF808080;
        context.fill(x, y, x + SLOT_SIZE, y + 1, borderColor); // Oben
        context.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, borderColor); // Unten
        context.fill(x, y, x + 1, y + SLOT_SIZE, borderColor); // Links
        context.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, borderColor); // Rechts
    }
    
    /**
     * Erstellt einen ItemStack aus ItemData
     */
    private ItemStack createItemStack(ItemData itemData) {
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
            var itemEntryOpt = Registries.ITEM.getEntry(itemId);
            if (itemEntryOpt.isEmpty()) {
                return ItemStack.EMPTY;
            }
            
            Item item = itemEntryOpt.get().value();
            ItemStack stack = new ItemStack(item);
            
            // Setze CustomModelData falls vorhanden
            // TODO: CustomModelData über Data Components setzen (für 1.21.7+)
            // Für jetzt: ItemStack ohne CustomModelData (wird später implementiert)
            if (itemData.customModelData != null) {
                // CustomModelData wird später über Data Components gesetzt
                // Siehe: https://minecraft.wiki/w/Data_components
            }
            
            return stack;
        } catch (Exception e) {
            // Falls Fehler beim Erstellen des ItemStacks
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * Gibt das gehoverte Item zurück
     */
    public ItemData getHoveredItem() {
        return hoveredItem;
    }
    
    /**
     * Gibt die Größe eines Slots zurück
     */
    public static int getSlotSize() {
        return SLOT_SIZE;
    }
    
    /**
     * Gibt die Anzahl der Spalten zurück
     */
    public static int getColumns() {
        return GRID_COLUMNS;
    }
    
    /**
     * Gibt die Anzahl der Zeilen zurück
     */
    public static int getRows() {
        return GRID_ROWS;
    }
    
    /**
     * Gibt die Anzahl der Items pro Seite zurück
     */
    public static int getItemsPerPage() {
        return ITEMS_PER_PAGE;
    }
    
    /**
     * Berechnet die benötigte Breite für das Grid
     */
    public static int getGridWidth() {
        return GRID_COLUMNS * SLOT_SIZE;
    }
    
    /**
     * Berechnet die benötigte Höhe für das Grid
     */
    public static int getGridHeight() {
        return GRID_ROWS * SLOT_SIZE;
    }
}


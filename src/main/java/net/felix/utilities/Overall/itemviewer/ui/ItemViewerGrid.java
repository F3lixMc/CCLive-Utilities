package net.felix.utilities.Overall.itemviewer.ui;

import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.itemviewer.data.ItemData;
import net.felix.utilities.Overall.itemviewer.search.ForgingConditionCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import static net.felix.utilities.Overall.itemviewer.tooltip.ForgingTooltipText.resolveForgingCondition;
import static net.felix.utilities.Overall.itemviewer.tooltip.ItemTooltipBuilder.buildTooltipLines;
import static net.felix.utilities.Overall.itemviewer.tooltip.ItemTooltipBuilder.updateItemViewerAspectOverlay;
import static net.felix.utilities.Overall.itemviewer.tooltip.TooltipCache.cachedTooltipLines;
import static net.felix.utilities.Overall.itemviewer.tooltip.TooltipCache.lastTooltipItem;
import static net.felix.utilities.Overall.itemviewer.ui.ItemStackCache.getCachedItemStack;
import static net.felix.utilities.Overall.itemviewer.ui.ItemStackCache.getTextureId;
import static net.felix.utilities.Overall.itemviewer.ui.ItemStackCache.isCardSlot;
import static net.felix.utilities.Overall.itemviewer.ui.ItemStackCache.isPassiveSkillSlot;
import static net.felix.utilities.Overall.itemviewer.ui.ItemStackCache.renderItemStackGuiIcon;

/**
 * Rendert Items in einem Grid-Layout (ähnlich JEI)
 */
public class ItemViewerGrid {

    private static final int SLOT_SIZE = 18; // 16x16 Item + 1px Padding auf jeder Seite

    private static final Identifier PASSIVE_SKILL_SLOT_TEXTURE = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/icons/passive_skill_slot.png");

    private static final Identifier CARD_SLOT_TEXTURE = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/icons/card_slot.png");

    private static final int DEFAULT_GRID_ROWS = 8; // Standard: 8 Zeilen (für Fallback)

    private static final int DEFAULT_GRID_COLUMNS = 6; // Standard: 6 Spalten (für Fallback)

    public static int getMaxGridSlots() {
        return CCLiveUtilitiesConfig.getItemViewerMaxSlots();
    }

    private final List<ItemData> items;

    private final int startX;

    private final int startY;

    private final int mouseX;

    private final int mouseY;

    private final int gridColumns; // Dynamische Spaltenanzahl

    private final int gridRows; // Dynamische Zeilenanzahl

    private ItemData hoveredItem = null;

    public ItemViewerGrid(List<ItemData> items, int startX, int startY, int mouseX, int mouseY, int availableWidth, int availableHeight) {
        this.items = items;
        this.startX = startX;
        this.startY = startY;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        int[] dims = computeGridDimensions(availableWidth, availableHeight);
        this.gridColumns = dims[0];
        this.gridRows = dims[1];
    }

    /**
     * Spalten/Zeilen aus verfügbarem Platz und Config-Maximum ({@link #getMaxGridSlots()}).
     * Spalten nutzen die volle verfügbare Breite (kein Limit bei 18).
     */
    public static int[] computeGridDimensions(int availableWidth, int availableHeight) {
        int maxSlots = getMaxGridSlots();
        int maxColsFit = Math.max(1, availableWidth / SLOT_SIZE);
        int maxRowsFit = Math.max(1, availableHeight / SLOT_SIZE);

        int cols = Math.min(maxColsFit, maxSlots);
        int rows = (maxSlots + cols - 1) / cols;

        if (rows > maxRowsFit) {
            rows = maxRowsFit;
            cols = Math.min(maxColsFit, (maxSlots + rows - 1) / rows);
            if (cols < 1) {
                cols = 1;
            }
            rows = Math.min(maxRowsFit, (maxSlots + cols - 1) / cols);
        }

        if (cols * rows > maxSlots) {
            rows = maxSlots / cols;
            if (rows < 1) {
                rows = 1;
                cols = Math.min(cols, maxSlots);
            }
        }
        return new int[] { cols, rows };
    }

    /**
     * Rendert das Grid mit allen Items
     */
    public void render(GuiGraphicsExtractor context) {
        int itemsPerPage = getItemsPerPage();
        int count = Math.min(items.size(), itemsPerPage);
        if (count <= 0) {
            hoveredItem = null;
            return;
        }
        
        int usedRows = (count + gridColumns - 1) / gridColumns;
        
        hoveredItem = null;
        int hoverIndex = resolveHoverIndex(count);
        if (hoverIndex >= 0) {
            hoveredItem = items.get(hoverIndex);
        }
        
        renderGridChrome(context, count, usedRows);
        if (hoverIndex >= 0) {
            int row = hoverIndex / gridColumns;
            int col = hoverIndex % gridColumns;
            int slotX = startX + col * SLOT_SIZE;
            int slotY = startY + row * SLOT_SIZE;
            context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x80FFFFFF);
        }
        
        for (int i = 0; i < count; i++) {
            int row = i / gridColumns;
            int col = i % gridColumns;
            int slotX = startX + col * SLOT_SIZE;
            int slotY = startY + row * SLOT_SIZE;
            renderItemIcon(context, items.get(i), slotX, slotY);
        }
        
        if (hoverIndex >= 0) {
            int row = hoverIndex / gridColumns;
            int col = hoverIndex % gridColumns;
            renderSlotHighlightBorder(context, startX + col * SLOT_SIZE, startY + row * SLOT_SIZE);
        }
    }

    /** Hintergrund + Raster nur so breit wie Items in jeder Zeile (keine leeren Slots am Zeilenende). */
    private void renderGridChrome(GuiGraphicsExtractor context, int count, int usedRows) {
        int borderColor = 0xFF808080;
        for (int r = 0; r < usedRows; r++) {
            int itemsInRow = Math.min(gridColumns, count - r * gridColumns);
            if (itemsInRow <= 0) {
                continue;
            }
            int rowY = startY + r * SLOT_SIZE;
            int rowWidth = itemsInRow * SLOT_SIZE;
            context.fill(startX, rowY, startX + rowWidth, rowY + SLOT_SIZE, 0x40000000);
            context.fill(startX, rowY, startX + rowWidth, rowY + 1, borderColor);
            context.fill(startX, rowY + SLOT_SIZE - 1, startX + rowWidth, rowY + SLOT_SIZE, borderColor);
            for (int c = 0; c <= itemsInRow; c++) {
                int x = startX + c * SLOT_SIZE;
                context.fill(x, rowY, x + 1, rowY + SLOT_SIZE, borderColor);
            }
        }
    }

    /** Weißer Hover-Rahmen über Items und Gitterlinien. */
    private void renderSlotHighlightBorder(GuiGraphicsExtractor context, int x, int y) {
        int borderColor = 0xFFFFFFFF;
        context.fill(x, y, x + SLOT_SIZE, y + 1, borderColor);
        context.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);
        context.fill(x, y, x + 1, y + SLOT_SIZE, borderColor);
        context.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, borderColor);
    }

    private int resolveHoverIndex(int count) {
        if (mouseX < startX || mouseY < startY) {
            return -1;
        }
        int col = (mouseX - startX) / SLOT_SIZE;
        int row = (mouseY - startY) / SLOT_SIZE;
        if (col < 0 || col >= gridColumns || row < 0 || row >= gridRows) {
            return -1;
        }
        int index = row * gridColumns + col;
        return index < count ? index : -1;
    }

    private void renderItemIcon(GuiGraphicsExtractor context, ItemData item, int slotX, int slotY) {
        int iconX = slotX + 1;
        int iconY = slotY + 1;
        if (item.texture != null && !item.texture.isEmpty()) {
            context.blit(
                RenderPipelines.GUI_TEXTURED,
                getTextureId(item.texture),
                iconX, iconY,
                0, 0,
                16, 16,
                16, 16
            );
            return;
        }
        if (isPassiveSkillSlot(item)) {
            context.blit(RenderPipelines.GUI_TEXTURED, PASSIVE_SKILL_SLOT_TEXTURE, iconX, iconY, 0, 0, 16, 16, 16, 16);
            return;
        }
        if (isCardSlot(item)) {
            context.blit(RenderPipelines.GUI_TEXTURED, CARD_SLOT_TEXTURE, iconX, iconY, 0, 0, 16, 16, 16, 16);
            return;
        }
        ItemStack itemStack = getCachedItemStack(item);
        if (!itemStack.isEmpty()) {
            renderItemStackGuiIcon(context, itemStack, iconX, iconY);
        }
    }

    /**
     * Rendert Tooltip für das gehoverte Item
     */
    public void renderTooltip(GuiGraphicsExtractor context) {
        if (hoveredItem != null) {
            Minecraft client = Minecraft.getInstance();
            ItemStack itemStack = getCachedItemStack(hoveredItem);
            if (!itemStack.isEmpty()) {
                boolean animatedForgingRainbow = ForgingConditionCalculator.usesAnimatedRainbow(
                        resolveForgingCondition(hoveredItem));
                boolean itemChanged = hoveredItem != lastTooltipItem;
                if (animatedForgingRainbow || itemChanged) {
                    lastTooltipItem = hoveredItem;
                    cachedTooltipLines = buildTooltipLines(hoveredItem, itemStack, client, true, true);
                }
                // Aspect overlay must refresh every hover frame: HandledScreenMixin can clear it
                // before Item Viewer renders, and same ItemData must re-apply after that clear.
                updateItemViewerAspectOverlay(hoveredItem);
                java.util.List<Component> tooltipLines = cachedTooltipLines;
                if (tooltipLines == null || tooltipLines.isEmpty()) {
                    return;
                }
                net.felix.utilities.Overall.itemviewer.tooltip.ItemTooltipBuilder.show(context, tooltipLines, mouseX, mouseY);
            } else {
                lastTooltipItem = null;
                cachedTooltipLines = null;
                net.felix.utilities.Overall.aspekte.overlay.AspectOverlay.onHoverStopped();
            }
        } else {
            lastTooltipItem = null;
            cachedTooltipLines = null;
            net.felix.utilities.Overall.aspekte.overlay.AspectOverlay.onHoverStopped();
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
     * Gibt die Anzahl der Spalten zurück (für diese Instanz)
     */
    public int getColumns() {
        return gridColumns;
    }

    /**
     * Gibt die Anzahl der Zeilen zurück
     */
    public static int getRows() {
        return DEFAULT_GRID_ROWS; // Für Rückwärtskompatibilität
    }

    public int getGridRows() {
        return gridRows;
    }

    /**
     * Gibt die Anzahl der Items pro Seite zurück (für diese Instanz)
     */
    public int getItemsPerPage() {
        return Math.min(gridColumns * gridRows, getMaxGridSlots());
    }

    public static int computeItemsPerPage(int availableWidth, int availableHeight) {
        int[] dims = computeGridDimensions(availableWidth, availableHeight);
        return Math.min(dims[0] * dims[1], getMaxGridSlots());
    }

    /**
     * Berechnet die benötigte Breite für das Grid (für diese Instanz)
     */
    public int getGridWidth() {
        return gridColumns * SLOT_SIZE;
    }

    /**
     * Statische Methode für Fallback-Berechnungen (verwendet Standard-Spaltenanzahl)
     */
    public static int getDefaultGridWidth() {
        return DEFAULT_GRID_COLUMNS * SLOT_SIZE;
    }

    /**
     * Statische Methode für Fallback-Berechnungen (verwendet Standard-Spaltenanzahl)
     */
    public static int getDefaultItemsPerPage() {
        return DEFAULT_GRID_COLUMNS * DEFAULT_GRID_ROWS;
    }

    /**
     * Berechnet die benötigte Höhe für das Grid
     */
    public static int getGridHeight() {
        return DEFAULT_GRID_ROWS * SLOT_SIZE;
    }

}

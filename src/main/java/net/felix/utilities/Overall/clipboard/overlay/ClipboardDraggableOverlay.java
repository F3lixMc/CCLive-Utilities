package net.felix.utilities.Overall.clipboard.overlay;

import com.mojang.blaze3d.platform.InputConstants;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.blueprints.viewer.BPViewerUtility;
import net.felix.utilities.Aincraft.materials.data.MaterialDatabase;
import net.felix.utilities.Aincraft.materials.data.MaterialFloorInfo;
import net.felix.utilities.Aincraft.materials.data.MaterialRarityColors;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.felix.utilities.Other.hud.HudNumberSuffixUtility;
import net.felix.utilities.Other.keybinding.KeyBindingUtility;
import net.felix.utilities.Overall.clipboard.ClipboardLayout;
import net.felix.utilities.Overall.clipboard.ClipboardTargetTime;
import net.felix.utilities.Overall.clipboard.ClipboardUtility;
import net.felix.utilities.Overall.clipboard.collector.ClipboardAmbossRessourceCollector;
import net.felix.utilities.Overall.clipboard.collector.ClipboardCoinCollector;
import net.felix.utilities.Overall.clipboard.collector.ClipboardPaperShredsCollector;
import net.felix.utilities.Overall.clipboard.storage.CollectedMaterialsResourcesStorage;
import net.felix.utilities.Overall.itemviewer.data.CostItem;
import net.felix.utilities.Overall.itemviewer.data.ItemCatalog;
import net.felix.utilities.Overall.itemviewer.data.ItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/**
 * Draggable Overlay für das Clipboard (Bauplan-Kosten)
 */
public class ClipboardDraggableOverlay implements DraggableOverlay {
    
    /**
     * Hilfsklasse für CostItem mit Kategorie-Information
     */
    private static class CostItemWithCategory {
        CostItem costItem;
        int category; // 0 = Coins, 1 = Materialien, 2 = Amboss/Ofen, 3 = Ressource
        
        CostItemWithCategory(CostItem costItem, int category) {
            this.costItem = costItem;
            this.category = category;
        }
    }
    
    private static CostItem resolveAmboss(net.felix.utilities.Overall.itemviewer.data.PriceData price) {
        if (price == null) {
            return null;
        }
        return price.Amboss != null ? price.Amboss : price.amboss;
    }
    
    private static CostItem resolveRessource(net.felix.utilities.Overall.itemviewer.data.PriceData price) {
        if (price == null) {
            return null;
        }
        return price.Ressource != null ? price.Ressource : price.ressource;
    }

    private static void addPriceCostItemsToTotal(Map<String, CostItemWithCategory> totalCostsMap, net.felix.utilities.Overall.itemviewer.data.PriceData price, int quantity) {
        if (price == null || totalCostsMap == null) {
            return;
        }
        addCategorizedCost(totalCostsMap, multiplyCostItem(price.coin, quantity), 0);
        addCategorizedCost(totalCostsMap, multiplyCostItem(price.material1, quantity), 1);
        addCategorizedCost(totalCostsMap, multiplyCostItem(price.material2, quantity), 1);
        addCategorizedCost(totalCostsMap, multiplyCostItem(price.material3, quantity), 1);
        addCategorizedCost(totalCostsMap, multiplyCostItem(price.material4, quantity), 1);
        addCategorizedCost(totalCostsMap, multiplyCostItem(price.material5, quantity), 1);
        addCategorizedCost(totalCostsMap, multiplyCostItem(resolveAmboss(price), quantity), 2);
        addCategorizedCost(totalCostsMap, multiplyCostItem(resolveRessource(price), quantity), 3);
    }
    
    private static void collectRequiredMaterialsFromPrice(net.felix.utilities.Overall.itemviewer.data.PriceData price, Set<String> requiredMaterials) {
        if (price == null || requiredMaterials == null) {
            return;
        }
        CostItem[] items = {
            price.coin,
            price.material1, price.material2, price.material3, price.material4, price.material5,
            resolveAmboss(price), resolveRessource(price)
        };
        for (CostItem costItem : items) {
            if (costItem != null && costItem.itemName != null &&
                !"Coins".equalsIgnoreCase(costItem.itemName) &&
                !"Pergamentfetzen".equalsIgnoreCase(costItem.itemName)) {
                requiredMaterials.add(normalizeMaterialName(costItem.itemName));
            }
        }
    }
    
    private static final int DEFAULT_WIDTH = 180;

    private static final DecimalFormat INTEGER_GROUPED_FORMAT;
    private static final DecimalFormat DECIMAL_GROUPED_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(java.util.Locale.US);
        symbols.setGroupingSeparator('.');
        INTEGER_GROUPED_FORMAT = new DecimalFormat("#,###", symbols);
        DECIMAL_GROUPED_FORMAT = new DecimalFormat("#,###.##", symbols);
        DECIMAL_GROUPED_FORMAT.setDecimalSeparatorAlwaysShown(false);
    }

    /** Vorbereitete Kostenzeile – Text/Farben/Breite einmal pro Tick berechnet. */
    private static final class PreparedCostLine {
        final String mainText;
        final int mainColor;
        final String abbrevText;
        final String floorText;
        final int floorColor;
        final String timeText;
        final String timeTextDetailed;
        final int mainWidth;
        final int abbrevWidth;
        final int floorWidth;
        final int totalWidth;

        PreparedCostLine(String mainText, int mainColor, String abbrevText, String floorText,
                         int floorColor, String timeText, String timeTextDetailed,
                         int mainWidth, int abbrevWidth, int floorWidth, int totalWidth) {
            this.mainText = mainText;
            this.mainColor = mainColor;
            this.abbrevText = abbrevText != null ? abbrevText : "";
            this.floorText = floorText != null ? floorText : "";
            this.floorColor = floorColor;
            this.timeText = timeText != null ? timeText : "";
            this.timeTextDetailed = timeTextDetailed != null ? timeTextDetailed : this.timeText;
            this.mainWidth = mainWidth;
            this.abbrevWidth = abbrevWidth;
            this.floorWidth = floorWidth;
            this.totalWidth = totalWidth;
        }
    }

    /** Gecachte Render-Daten – pro Tick nur einmal berechnet (Kosten, Breite, Zeilen). */
    private static final class RenderSnapshot {
        final int tick;
        final int entriesRevision;
        final int currentPage;
        final int displayQuantity;
        final List<PreparedCostLine> mainCostLines;
        final String statusMessage;
        final boolean scrollable;
        final boolean overflow;
        final int scrollOffset;
        final int scrollMax;
        final int renderWidth;
        final int lineCount;
        final int quantityAreaWidth;
        final int maxUnscaledHeight;

        RenderSnapshot(int tick, int entriesRevision, int currentPage, int displayQuantity,
                       List<PreparedCostLine> mainCostLines, List<PreparedCostLine> shopCostLines,
                       String statusMessage, boolean scrollable, boolean overflow, int scrollOffset, int scrollMax,
                       int renderWidth, int lineCount, int quantityAreaWidth, int maxUnscaledHeight) {
            this.tick = tick;
            this.entriesRevision = entriesRevision;
            this.currentPage = currentPage;
            this.displayQuantity = displayQuantity;
            this.mainCostLines = mainCostLines;
            this.statusMessage = statusMessage;
            this.scrollable = scrollable;
            this.overflow = overflow;
            this.scrollOffset = scrollOffset;
            this.scrollMax = scrollMax;
            this.renderWidth = renderWidth;
            this.lineCount = lineCount;
            this.quantityAreaWidth = quantityAreaWidth;
            this.maxUnscaledHeight = maxUnscaledHeight;
        }
    }

    private static RenderSnapshot cachedRenderSnapshot;
    private static int costScrollOffset;
    private static boolean costScrollable;
    private static int costScrollPage = -1;
    private static boolean costScrollbarDragging;
    private static int costScrollTrackX;
    private static int costScrollTrackY;
    private static int costScrollTrackWidth;
    private static int costScrollTrackHeight;
    private static int costScrollHandleY;
    private static int costScrollHandleHeight;
    private static boolean costScrollbarVisible;
    private static int costOverlayScreenX;
    private static int costOverlayScreenY;
    private static int costOverlayScreenWidth;
    private static int costOverlayScreenHeight;
    private static int costOverlayMaxScaledHeight;
    private static int[] visibleStoredIndexes = new int[0];
    private static boolean visiblePagesReady;
    private static int lastMappedRevision = -1;
    private static final int COST_SCROLLBAR_WIDTH = 6;
    private static final int REMOVE_BUTTON_HEIGHT = 20;
    private static final int REMOVE_BUTTON_GAP = 5;
    private static final int DEFAULT_HEIGHT = 150;
    private static final String QUANTITY_PREFIX = "x";
    private static final int QUANTITY_FIELD_PADDING = 4;
    private static final int QUANTITY_TEXT_FIELD_WIDTH = 50;
    private static final int QUANTITY_FIELD_OUTLINE_PADDING = 2;
    private static final int QUANTITY_AFTER_BUTTONS_SPACING = 8;
    private static final class FoundStatusLabel {
        final String text;
        final int color;

        FoundStatusLabel(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }

    private static final int HEADER_BUTTON_GAP = 5;
    private static final int HEADER_COUNT_PADDING = 5;
    private static final int HEADER_NAME_PADDING = 5;
    private static final int COST_LINE_HORIZONTAL_PADDING = 10;
    
    // TextField für [Anzahl] Eingabe
    private static EditBox quantityTextField = null;
    private static int quantityTextFieldPage = -1; // Seite, für die das Textfeld aktiv ist
    /** Cache für Seite-1-Anzahl; negativ = noch nicht aus der Config geladen. */
    private static int page1Quantity = -1;
    
    // Bestätigungs-Overlay für "Alle Baupläne entfernen"
    private static boolean showDeleteConfirmation = false;
    
    // Button-Positionen (werden beim Rendering gesetzt)
    private static int deleteButtonX = 0;
    private static int deleteButtonY = 0;
    private static int deleteButtonWidth = 0;
    private static int deleteButtonHeight = 0;
    private static boolean deleteButtonVisible = false;
    
    // Bestätigungs-Overlay Button-Positionen (werden beim Rendering gesetzt)
    private static int jaButtonX = 0;
    private static int jaButtonY = 0;
    private static int jaButtonWidth = 0;
    private static int jaButtonHeight = 0;
    private static int neinButtonX = 0;
    private static int neinButtonY = 0;
    private static int neinButtonWidth = 0;
    private static int neinButtonHeight = 0;
    private static boolean confirmationButtonsVisible = false;
    
    // ButtonWidget-Instanzen für die Buttons (werden beim Rendering erstellt)
    private static Button deleteButton = null;
    private static Button confirmationJaButton = null;
    private static Button confirmationNeinButton = null;
    
    // Flag für Clipboard-Hover (wird bei Tab/F1 ausgeblendet)
    private static boolean hideHover = false;
    
    // Zuletzt gerendeter Hover-Bereich für die Anzahl (für Tooltips, synchron mit renderInGame)
    private static boolean quantityHoverBoundsValid = false;
    private static int quantityHoverX = 0;
    private static int quantityHoverY = 0;
    private static int quantityHoverWidth = 0;
    private static int quantityHoverHeight = 0;
    
    private static void updateQuantityHoverBounds(int x, int y, int width, int height) {
        quantityHoverX = x;
        quantityHoverY = y;
        quantityHoverWidth = width;
        quantityHoverHeight = height;
        quantityHoverBoundsValid = true;
    }
    
    private static void clearQuantityHoverBounds() {
        quantityHoverBoundsValid = false;
    }
    
    // Separate Speicherung für Clipboard-Materialien (bleibt beim Dimensionswechsel erhalten)
    private static final Map<String, Long> clipboardMaterials = new HashMap<>();
    /** Pro Tick nur einmal Materialien aus Storage/ActionBar synchronisieren. */
    private static int materialsSyncTick = -1;
    
    private static void ensureClipboardMaterialsSynced() {
        Minecraft client = Minecraft.getInstance();
        int tick = client != null && client.level != null ? (int) client.level.getGameTime() : -1;
        if (tick >= 0 && materialsSyncTick == tick) {
            return;
        }
        materialsSyncTick = tick;
        updateClipboardMaterials();
    }
    
    /**
     * Material-Sync passiert bei echten Updates (Actionbar-Nachricht, Material-Bag, Kosten-Inventar).
     * Kein erneutes Überschreiben aus dem ActionBar-Cache pro Frame — der würde Abgaben ignorieren.
     */
    public static void updateClipboardMaterials() {
    }
    
    public static void invalidateMaterialsSyncCache() {
        materialsSyncTick = -1;
    }

    public static void invalidateRenderSnapshot() {
        cachedRenderSnapshot = null;
    }

    private static RenderSnapshot ensureRenderSnapshot(Minecraft client) {
        if (client == null) {
            return null;
        }
        int tick = client.level != null ? (int) client.level.getGameTime() : -1;
        int revision = ClipboardUtility.getEntriesRevision();
        int page = ClipboardUtility.getCurrentPage();
        ClipboardUtility.ClipboardEntry entry = ClipboardUtility.getEntryForPage(page);
        int displayQuantity = readDisplayQuantity(page, entry);

        if (costScrollPage != page) {
            costScrollOffset = 0;
            costScrollPage = page;
        }

        RenderSnapshot cached = cachedRenderSnapshot;
        if (cached != null && cached.tick == tick && cached.entriesRevision == revision
                && cached.currentPage == page && cached.displayQuantity == displayQuantity
                && cached.scrollOffset == costScrollOffset) {
            return cached;
        }

        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        if (lastMappedRevision != revision) {
            visiblePagesReady = false;
            lastMappedRevision = revision;
        }
        page = retainVisiblePage(entries);
        entry = ClipboardUtility.getEntryForPage(page);
        displayQuantity = readDisplayQuantity(page, entry);
        if (costScrollPage != page) {
            costScrollOffset = 0;
            costScrollPage = page;
        }

        boolean inventory = client.gui.screen() instanceof AbstractContainerScreen;
        int navigationWidth = getNavigationButtonsWidth(client);
        int maxLines = CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaxCostLines;
        int entryCount = entries.size();
        int[] lineWidths = new int[entryCount];
        int[] nameBlocks = new int[entryCount];
        int[] entryLineCounts = new int[entryCount];
        int[] quantityWidths = new int[entryCount];
        boolean anyScrollable = false;
        for (int i = 0; i < entryCount; i++) {
            ClipboardUtility.ClipboardEntry pinned = entries.get(i);
            int quantity = pinned.quantity > 0 ? pinned.quantity : 1;
            PreparedEntry prepared = prepareEntry(client, pinned, quantity);
            int lineWidth = widestLine(prepared.normal, List.of());
            lineWidths[i] = lineWidth;
            nameBlocks[i] = headerNameBlock(client, pinned.blueprintName, resolveEntryFoundStatus(pinned), navigationWidth);
            quantityWidths[i] = getQuantityAreaWidth(client, formatQuantityText(quantity), inventory);
            ClipboardTargetTime.CostWindow entryWindow = ClipboardTargetTime.window(
                    prepared.normal.size(), 0, maxLines, 0);
            if (entryWindow.scrollable) {
                anyScrollable = true;
            }
            List<PreparedCostLine> shown = prepared.normal.subList(0, entryWindow.count);
            String entryStatus = ClipboardTargetTime.statusText(true, prepared.normal.size(), shown.size());
            entryLineCounts[i] = computeLineCount(2, shown, List.of(), entryStatus);
        }

        List<ClipboardUtility.ClipboardEntry> visibleEntries = new ArrayList<>();
        for (int index : visibleStoredIndexes) {
            if (index >= 0 && index < entryCount) {
                visibleEntries.add(entries.get(index));
            }
        }
        int overviewQuantity = readDisplayQuantity(1, null);
        TotalCostsBundle overviewCosts = buildTotalCostsBundle(visibleEntries, overviewQuantity);
        List<PreparedCostLine> overviewNormal = prepareCategorizedCostLines(client, overviewCosts.normal, overviewQuantity, false);
        List<PreparedCostLine> overviewShop = List.of();
        ClipboardTargetTime.CostWindow overviewWindow = ClipboardTargetTime.window(
                overviewNormal.size(), 0, maxLines, 0);
        if (overviewWindow.scrollable) {
            anyScrollable = true;
        }
        int overviewQuantityWidth = getQuantityAreaWidth(client, formatQuantityText(overviewQuantity), inventory);
        int maxQuantityWidth = overviewQuantityWidth;
        for (int quantityWidth : quantityWidths) {
            maxQuantityWidth = Math.max(maxQuantityWidth, quantityWidth);
        }
        FoundStatusLabel overviewStatus = resolveOverallFoundStatus(visibleEntries);
        int overviewNameBlock = headerNameBlock(client, "Gesamtliste", overviewStatus, navigationWidth);
        int overviewWidth = Math.max(widestLine(overviewNormal, List.of()),
                overviewNameBlock + maxQuantityWidth + HEADER_COUNT_PADDING);
        int[] pinnedWidths = new int[entryCount];
        for (int i = 0; i < entryCount; i++) {
            pinnedWidths[i] = Math.max(lineWidths[i], nameBlocks[i] + maxQuantityWidth + HEADER_COUNT_PADDING);
        }
        int minimum = DEFAULT_WIDTH;
        minimum = Math.max(minimum, client.font.width("Bauplan Kosten:") + 10);
        int renderWidth = ClipboardLayout.sharedWidth(minimum, overviewWidth, pinnedWidths, anyScrollable,
                COST_SCROLLBAR_WIDTH + 2);

        List<PreparedCostLine> shownOverview = overviewNormal.subList(0, overviewWindow.count);
        String overviewMessage = ClipboardTargetTime.statusText(!visibleEntries.isEmpty(), overviewNormal.size(), shownOverview.size());
        int overviewLineCount = computeLineCount(1, shownOverview, overviewShop, overviewMessage);
        int maxLineCount = overviewLineCount;
        for (int entryLineCount : entryLineCounts) {
            maxLineCount = Math.max(maxLineCount, entryLineCount);
        }

        List<PreparedCostLine> fullNormalLines;
        List<PreparedCostLine> shopCostLines;
        boolean hasEntries = !visibleEntries.isEmpty();
        if (page == 1) {
            fullNormalLines = overviewNormal;
            shopCostLines = overviewShop;
        } else if (entry != null) {
            int quantity = entry.quantity > 0 ? entry.quantity : 1;
            PreparedEntry current = prepareEntry(client, entry, quantity);
            fullNormalLines = current.normal;
            shopCostLines = current.shop;
        } else {
            fullNormalLines = List.of();
            shopCostLines = List.of();
            hasEntries = false;
        }

        ClipboardTargetTime.CostWindow window = ClipboardTargetTime.window(
                fullNormalLines.size(), 0, maxLines, costScrollOffset);
        costScrollOffset = window.start;
        costScrollable = window.scrollable;
        List<PreparedCostLine> mainCostLines = new ArrayList<>(
                fullNormalLines.subList(window.start, window.start + window.count));
        String statusMessage = ClipboardTargetTime.statusText(hasEntries, fullNormalLines.size(), mainCostLines.size());
        int lineCount = computeLineCount(page, mainCostLines, shopCostLines, statusMessage);
        int lineHeight = client.font.lineHeight + 2;
        int maxUnscaledHeight = net.felix.utilities.DragOverlay.OverlayInsets.heightForLines(
                5, lineHeight, client.font.lineHeight, maxLineCount);

        cachedRenderSnapshot = new RenderSnapshot(
                tick, revision, page, displayQuantity, mainCostLines, shopCostLines,
                statusMessage, window.scrollable, window.overflow, costScrollOffset, window.maxOffset,
                renderWidth, lineCount, maxQuantityWidth, maxUnscaledHeight);
        return cachedRenderSnapshot;
    }

    private static int retainVisiblePage(List<ClipboardUtility.ClipboardEntry> entries) {
        int currentPage = ClipboardUtility.getCurrentPage();
        int shown = -1;
        if (currentPage > 1) {
            shown = visiblePagesReady
                    ? ClipboardLayout.storedIndexForPage(visibleStoredIndexes, currentPage)
                    : currentPage - 2;
        }
        boolean[] completed = new boolean[entries.size()];
        boolean hideCompleted = CCLiveUtilitiesConfig.HANDLER.instance().clipboardHideCompletedBlueprints;
        boolean totalsMet = overviewCostsComplete(entries);
        for (int i = 0; i < entries.size(); i++) {
            ClipboardUtility.ClipboardEntry entry = entries.get(i);
            int quantity = entry.quantity > 0 ? entry.quantity : 1;
            if (mergedCostTotals(entry, quantity).isEmpty()) {
                completed[i] = entryComplete(entry, quantity);
            } else {
                completed[i] = totalsMet;
            }
        }
        int[] next = ClipboardLayout.visibleStoredIndexes(completed, hideCompleted);
        int newPage = ClipboardLayout.pageForStoredIndex(next, shown);
        int previousStored = shown;
        visibleStoredIndexes = next;
        visiblePagesReady = true;
        if (newPage != currentPage) {
            setCurrentPage(newPage);
            int nowStored = ClipboardLayout.storedIndexForPage(next, newPage);
            if (quantityTextField != null && previousStored != nowStored) {
                resetQuantityTextField();
            } else if (quantityTextFieldPage > 1) {
                quantityTextFieldPage = newPage;
            }
        }
        return newPage;
    }

    /**
     * Gesamtkosten wie auf Seite 1: jede Bauplan-Anzahl ist eingerechnet, danach die Anzeige-Menge der Übersicht.
     * Ein Material zählt erst, wenn der gemeinsame Vorrat für alle Baupläne reicht.
     */
    private static boolean overviewCostsComplete(List<ClipboardUtility.ClipboardEntry> entries) {
        Map<String, CostItemWithCategory> totals = new HashMap<>();
        if (entries != null) {
            for (ClipboardUtility.ClipboardEntry entry : entries) {
                if (entry == null) {
                    continue;
                }
                int quantity = entry.quantity > 0 ? entry.quantity : 1;
                for (CostItemWithCategory item : mergedCostTotals(entry, quantity).values()) {
                    addCategorizedCost(totals, item.costItem, item.category);
                }
            }
        }
        if (totals.isEmpty()) {
            return true;
        }
        int overviewQuantity = getPage1Quantity();
        for (CostItemWithCategory item : totals.values()) {
            if (!isCostItemComplete(item.costItem, overviewQuantity)) {
                return false;
            }
        }
        return true;
    }

    private static boolean entryComplete(ClipboardUtility.ClipboardEntry entry, int quantity) {
        if (entry == null) {
            return false;
        }
        Map<String, CostItemWithCategory> totals = mergedCostTotals(entry, 1);
        if (totals.isEmpty()) {
            return entry.price != null;
        }
        boolean[] flags = new boolean[totals.size()];
        int index = 0;
        for (CostItemWithCategory item : totals.values()) {
            flags[index++] = isCostItemComplete(item.costItem, quantity);
        }
        return ClipboardLayout.normalCostsComplete(flags);
    }

    private static boolean includeShopCosts() {
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts;
    }

    private static Map<String, CostItemWithCategory> mergedCostTotals(ClipboardUtility.ClipboardEntry entry, int quantity) {
        Map<String, CostItemWithCategory> totals = new HashMap<>();
        if (entry == null) {
            return totals;
        }
        if (entry.price != null) {
            addPriceCostItemsToTotal(totals, entry.price, quantity);
        }
        if (includeShopCosts()) {
            addShopCosts(totals, entry.blueprintShop, quantity);
        }
        return totals;
    }

    private static void addShopCosts(Map<String, CostItemWithCategory> totals,
            net.felix.utilities.Overall.itemviewer.data.BlueprintShopData shop, int quantity) {
        if (totals == null || shop == null || shop.price == null) {
            return;
        }
        addCategorizedCost(totals, multiplyCostItem(shop.price.coin, quantity), 0);
        addCategorizedCost(totals, multiplyCostItem(shop.price.paper_shreds, quantity), 1);
    }

    private static final class PreparedEntry {
        final List<PreparedCostLine> normal;
        final List<PreparedCostLine> shop;

        PreparedEntry(List<PreparedCostLine> normal, List<PreparedCostLine> shop, boolean complete) {
            this.normal = normal;
            this.shop = shop;
        }
    }

    private static PreparedEntry prepareEntry(Minecraft client, ClipboardUtility.ClipboardEntry entry, int quantity) {
        boolean complete = entryComplete(entry, quantity);
        List<CostItemWithCategory> items = categorizeCategorizedTotals(mergedCostTotals(entry, 1), quantity);
        List<PreparedCostLine> normal = prepareCategorizedCostLines(client, items, quantity, false);
        return new PreparedEntry(normal, List.of(), complete);
    }

    private static int widestLine(List<PreparedCostLine> normal, List<PreparedCostLine> shop) {
        int width = 0;
        if (normal != null) {
            for (PreparedCostLine line : normal) {
                width = Math.max(width, line.totalWidth + COST_LINE_HORIZONTAL_PADDING);
            }
        }
        if (shop != null) {
            for (PreparedCostLine line : shop) {
                width = Math.max(width, line.totalWidth + COST_LINE_HORIZONTAL_PADDING);
            }
        }
        return width;
    }

    private static int headerNameBlock(Minecraft client, String name, FoundStatusLabel status, int navigationWidth) {
        int nameEnd = HEADER_NAME_PADDING + client.font.width(name != null ? name : "");
        if (status != null && status.text != null && !status.text.isEmpty()) {
            nameEnd += client.font.width(" ") + client.font.width(status.text);
        }
        return nameEnd + HEADER_BUTTON_GAP + navigationWidth + QUANTITY_AFTER_BUTTONS_SPACING;
    }
    
    public static void resetCollectedMaterials() {
        clipboardMaterials.clear();
        ClipboardTimeEstimate.reset();
        invalidateMaterialsSyncCache();
        invalidateRenderSnapshot();
    }
    
    /**
     * Entfernt Materialien aus der Clipboard-Speicherung, die nicht mehr benötigt werden
     * Wird aufgerufen, wenn ein Bauplan aus dem Clipboard entfernt wird
     */
    public static void cleanupUnusedMaterials() {
        // Hole alle Materialien, die von Bauplänen im Clipboard benötigt werden
        Set<String> requiredMaterials = new HashSet<>();
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (entry.price != null) {
                collectRequiredMaterialsFromPrice(entry.price, requiredMaterials);
            }
            if (entry.blueprintShop != null && entry.blueprintShop.price != null) {
                if (entry.blueprintShop.price.coin != null && entry.blueprintShop.price.coin.itemName != null &&
                    !"Coins".equalsIgnoreCase(entry.blueprintShop.price.coin.itemName) &&
                    !"Pergamentfetzen".equalsIgnoreCase(entry.blueprintShop.price.coin.itemName)) {
                    requiredMaterials.add(normalizeMaterialName(entry.blueprintShop.price.coin.itemName));
                }
                if (entry.blueprintShop.price.paper_shreds != null && entry.blueprintShop.price.paper_shreds.itemName != null &&
                    !"Coins".equalsIgnoreCase(entry.blueprintShop.price.paper_shreds.itemName) &&
                    !"Pergamentfetzen".equalsIgnoreCase(entry.blueprintShop.price.paper_shreds.itemName)) {
                    requiredMaterials.add(normalizeMaterialName(entry.blueprintShop.price.paper_shreds.itemName));
                }
            }
        }
        
        // Entferne Materialien, die nicht mehr benötigt werden
        CollectedMaterialsResourcesStorage.copyMaterialsInto(clipboardMaterials);
        clipboardMaterials.entrySet().removeIf(entry -> {
            String normalizedName = normalizeMaterialName(entry.getKey());
            return !requiredMaterials.contains(normalizedName);
        });
        ClipboardTimeEstimate.retainOnly(requiredMaterials);
        invalidateRenderSnapshot();
    }
    
    private static void setCurrentPage(int page) {
        int totalPages = getTotalPages();
        // Clamp page to valid range
        page = Math.max(1, Math.min(page, totalPages));
        if (page != costScrollPage) {
            costScrollOffset = 0;
            costScrollPage = page;
            cachedRenderSnapshot = null;
        }
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardCurrentPage = page;
        CCLiveUtilitiesConfig.HANDLER.save();
    }
    
    // Gesamtanzahl der Seiten (wird später dynamisch berechnet)
    private static int getTotalPages() {
        return ClipboardUtility.getTotalPages();
    }

    /** 0, solange die sichtbaren Seiten noch nicht aus den angepinnten Einträgen gebaut wurden. */
    public static int visiblePageCountOrZero() {
        return visiblePagesReady ? 1 + visibleStoredIndexes.length : 0;
    }

    /** Vor dem ersten Aufbau bleibt die alte Seitenzahl die Position in der vollen Liste. */
    public static int storedIndexForVisiblePage(int page) {
        if (!visiblePagesReady) {
            return page - 2;
        }
        return ClipboardLayout.storedIndexForPage(visibleStoredIndexes, page);
    }
    
    @Override
    public String getOverlayName() {
        return "Clipboard";
    }
    
    @Override
    public int getX() {
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardX;
    }
    
    @Override
    public int getY() {
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardY;
    }
    
    @Override
    public int getWidth() {
        // Return scaled width
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) scale = 1.0f; // Safety check
        return Math.round(calculateUnscaledWidth() * scale);
    }
    
    @Override
    public int getHeight() {
        // Return scaled height
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) scale = 1.0f; // Safety check
        return Math.round(calculateUnscaledHeight() * scale);
    }
    
    
    @Override
    public void setPosition(int x, int y) {
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardX = x;
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardY = y;
    }
    
    @Override
    public void setSize(int width, int height) {
        // Get current unscaled dimensions
        int unscaledWidth = calculateUnscaledWidth();
        int unscaledHeight = calculateUnscaledHeight();
        
        // Calculate scale based on width and height
        float scaleX = (float) width / unscaledWidth;
        float scaleY = (float) height / unscaledHeight;
        float scale = (scaleX + scaleY) / 2.0f;
        
        // Clamp scale to reasonable values (0.1 to 5.0)
        scale = Math.max(0.1f, Math.min(5.0f, scale));
        
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale = scale;
    }
    
    /**
     * Calculate unscaled width (base width without scale factor)
     * Berechnet basierend auf dem neuen Format: "Bauplanname [Anzahl]", Kostenliste
     * Berücksichtigt tatsächliche Bauplan-Namen aus dem Clipboard
     */
    private static int calculateUnscaledWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return DEFAULT_WIDTH;
        }
        RenderSnapshot snapshot = ensureRenderSnapshot(client);
        return snapshot != null ? snapshot.renderWidth : DEFAULT_WIDTH;
    }

    private static int computeLineCount(int currentPage, List<PreparedCostLine> mainCostLines,
            List<PreparedCostLine> shopCostLines, String statusMessage) {
        int lineCount = 1;
        boolean noBlueprints = ClipboardTargetTime.NO_BLUEPRINTS.equals(statusMessage);
        boolean allCollected = ClipboardTargetTime.ALL_COLLECTED.equals(statusMessage);
        if (currentPage != 1 && noBlueprints) {
            return lineCount + 1;
        }
        if (noBlueprints) {
            lineCount += 2;
            return lineCount;
        }
        if (!mainCostLines.isEmpty() || allCollected) {
            lineCount += 1 + mainCostLines.size();
            if (allCollected) {
                lineCount += 1;
            }
        }
        if (lineCount == 1) {
            lineCount += 1;
        }
        return lineCount;
    }
    
    /**
     * Calculate unscaled height (base height without scale factor)
     * Entspricht dem F6-Vorschau-Layout in renderInEditMode (inkl. Titel und Trennlinie)
     */
    private static int calculateUnscaledHeight() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return DEFAULT_HEIGHT;
        
        int lineHeight = client.font.lineHeight + 2;
        int padding = 5;
        
        int height = padding;
        height += lineHeight; // "Pinnwand"
        height += 2; // Trennlinie unter dem Titel
        height += lineHeight; // "Bauplanname xAnzahl"
        height += lineHeight; // "Bauplan Kosten:"
        height += lineHeight * 4; // Coins, Material, Amboss, Ressource
        
        height += padding;
        height -= Math.max(0, lineHeight - client.font.lineHeight);
        return height;
    }
    
    @Override
    public void renderInEditMode(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Get scale
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) scale = 1.0f;
        
        // Render border for edit mode (scaled)
        context.outline(x, y, width, height, 0xFFFF0000);
        
        // Render background (scaled)
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // Render content with scale using matrix transformation
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            matrices.popMatrix();
            return;
        }
        
        int lineHeight = client.font.lineHeight + 2;
        int padding = 5;
        int currentY = padding;
        int unscaledWidth = calculateUnscaledWidth();
        
        // Titel "Pinnwand"
        context.text(
            client.font,
            "Pinnwand",
            5, currentY,
            0xFFFFFFFF,
            true
        );
        currentY += lineHeight;
        
        // Trennlinie unter dem Titel
        int lineY = currentY - 1;
        context.fill(5, lineY, unscaledWidth - 5, lineY + 1, 0xFF808080); // Graue Linie
        currentY += 2; // Kleiner Abstand nach der Linie
        
        // Im F6-Menü: Immer statisches Beispiel-Design
        // "Bauplanname [Anzahl]" - Anzahl oben rechts, mit Pfeil-Buttons davor
        String blueprintName = "Bauplan Name";
        String countText = formatQuantityText(1);
        
        int countWidth = client.font.width(countText);
        
        // Berechne Positionen für Buttons und Text
        int countX = unscaledWidth - countWidth - 5; // Position für [Anzahl]
        
        // Pfeil-Buttons: « und » links vom [Anzahl] Feld
        String leftArrow = "«";
        String rightArrow = "»";
        String separator = "|";
        String space = " "; // Leerzeichen für gleichmäßigen Abstand
        int leftArrowWidth = client.font.width(leftArrow);
        int rightArrowWidth = client.font.width(rightArrow);
        int separatorWidth = client.font.width(separator);
        int spaceWidth = client.font.width(space);
        // Gesamtbreite: « + Leerzeichen + | + Leerzeichen + »
        int totalButtonWidth = leftArrowWidth + spaceWidth + separatorWidth + spaceWidth + rightArrowWidth;
        
        // Positionen der Buttons (links vom [Anzahl] Feld)
        // Berechne von rechts nach links: » -> Leerzeichen -> | -> Leerzeichen -> «
        // Aber rendere von links nach rechts: « -> Leerzeichen -> | -> Leerzeichen -> »
        int rightButtonX = countX - totalButtonWidth - 5; // 5px Abstand zum [Anzahl] Feld
        // Berechne Positionen von links nach rechts: « kommt zuerst, dann Leerzeichen, dann |, dann Leerzeichen, dann »
        int leftButtonX = rightButtonX; // « startet hier
        int spaceAfterLeftX = leftButtonX + leftArrowWidth; // Nach «
        int separatorX = spaceAfterLeftX + spaceWidth; // Nach Leerzeichen nach «
        int spaceAfterSeparatorX = separatorX + separatorWidth; // Nach |
        int spaceAfterRightX = spaceAfterSeparatorX + spaceWidth; // Nach Leerzeichen nach |
        rightButtonX = spaceAfterRightX; // » startet hier
        
        // Bauplan-Name links
        context.text(
            client.font,
            blueprintName,
            5, currentY,
            0xFFFFFFFF,
            true
        );
        
        // Pfeil-Buttons rendern (im F6-Menü: Beispiel-Buttons)
        int currentPage = 2; // Beispiel-Seite
        int totalPages = 3; // Beispiel-Seiten
        
        // Rendere von links nach rechts: « -> Leerzeichen -> | -> Leerzeichen -> »
        // Linker Button « (vorherige Seite)
        boolean canGoLeft = currentPage > 1;
        int leftButtonColor = canGoLeft ? 0xFFFFFFFF : 0xFF808080; // Weiß wenn aktiv, grau wenn inaktiv
        context.text(
            client.font,
            leftArrow,
            leftButtonX, currentY,
            leftButtonColor,
            true
        );
        
        // Leerzeichen nach « (vor |)
        context.text(
            client.font,
            space,
            spaceAfterLeftX, currentY,
            0xFFFFFFFF, // Weiß (unsichtbar, nur für Abstand)
            true
        );
        
        // Trennstrich | zwischen den Buttons
        context.text(
            client.font,
            separator,
            separatorX, currentY,
            0xFF808080, // Grau
            true
        );
        
        // Leerzeichen nach | (vor »)
        context.text(
            client.font,
            space,
            spaceAfterSeparatorX, currentY,
            0xFFFFFFFF, // Weiß (unsichtbar, nur für Abstand)
            true
        );
        
        // Rechter Button » (nächste Seite)
        boolean canGoRight = currentPage < totalPages;
        int rightButtonColor = canGoRight ? 0xFFFFFFFF : 0xFF808080; // Weiß wenn aktiv, grau wenn inaktiv
        context.text(
            client.font,
            rightArrow,
            rightButtonX, currentY,
            rightButtonColor,
            true
        );
        
        // Anzahl oben rechts
        context.text(
            client.font,
            countText,
            countX, currentY,
            0xFFFFFF00, // Gelb für Anzahl
            true
        );
        currentY += lineHeight;
        
        // Im F6-Menü: Immer statisches Beispiel-Design
        context.text(
            client.font,
            "Bauplan Kosten:",
            5, currentY,
            0xFFAAAAAA,
            true
        );
        currentY += lineHeight;
        
        context.text(
            client.font,
            "Coins",
            5, currentY,
            0xFFFFFFFF,
            true
        );
        currentY += lineHeight;
        
        context.text(
            client.font,
            "Material",
            5, currentY,
            0xFFFFFFFF,
            true
        );
        currentY += lineHeight;
        
        context.text(
            client.font,
            "Ressource",
            5, currentY,
            0xFFFFFFFF,
            true
        );
        currentY += lineHeight;
        
        context.text(
            client.font,
            "Amboss",
            5, currentY,
            0xFFFFFFFF,
            true
        );
        currentY += lineHeight;
        
        matrices.popMatrix();
    }
    
    /**
     * Rendert das Overlay im normalen Spiel (außerhalb des F6-Menüs)
     * Zeigt die tatsächlichen Clipboard-Daten an
     */
    public static void renderInGame(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled ||
            !CCLiveUtilitiesConfig.HANDLER.instance().showClipboard) {
            return;
        }

        // Deaktiviere in der general_lobby Dimension
        try {
            var client = net.minecraft.client.Minecraft.getInstance();
            if (client != null && client.level != null) {
                String dimensionId = client.level.dimension().identifier().toString();
                if ("minecraft:general_lobby".equals(dimensionId)) {
                    return;
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        
        ensureClipboardMaterialsSynced();
        
        RenderSnapshot snapshot = ensureRenderSnapshot(client);
        if (snapshot == null) {
            return;
        }
        
        // Aktualisiere hideHover Flag (prüft ob Zeit abgelaufen ist)
        updateHideHover();
        
        // Prüfe ob Hover ausgeblendet werden soll (bei Tab/F1)
        if (hideHover) {
            clearQuantityHoverBounds();
            return;
        }
        
        // Throttling entfernt - verursacht Flackern
        // Das Problem: Wenn das Rendering wegen Throttling übersprungen wird,
        // wird das Overlay nicht gerendert, aber beim nächsten Frame wieder.
        // Das führt zu ständigem Ein- und Ausblenden = Flackern.
        // Lösung: Rendering immer erlauben, um konsistente Anzeige zu gewährleisten
        
        // Hide overlay if F1 menu (debug screen) is open
        if (client.gui.hud.isHidden()) {
            return;
        }
        
        // Hide overlay if Tab list is open (like other overlays)
        if (KeyBindingUtility.isPlayerListKeyPressed()) {
            return;
        }
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardHideEmpty && visibleStoredIndexes.length == 0) {
            clearQuantityHoverBounds();
            costOverlayScreenWidth = 0;
            costOverlayScreenHeight = 0;
            deleteButtonVisible = false;
            return;
        }

        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) scale = 1.0f;
        int lineHeight = client.font.lineHeight + 2;
        int padding = 5;
        int estimatedLineCount = snapshot.lineCount;
        int estimatedHeight = net.felix.utilities.DragOverlay.OverlayInsets.heightForLines(
                padding, lineHeight, client.font.lineHeight, estimatedLineCount);
        int unscaledWidthForOrigin = snapshot.renderWidth;
        int scaledWidth = Math.round(unscaledWidthForOrigin * scale);
        boolean inventoryOpen = client.gui.screen() instanceof AbstractContainerScreen;
        int footprintHeight = Math.round(snapshot.maxUnscaledHeight * scale);
        if (inventoryOpen) {
            footprintHeight += REMOVE_BUTTON_GAP + REMOVE_BUTTON_HEIGHT;
        }
        ClipboardLayout.Point origin = screenOrigin(client, scaledWidth, footprintHeight);
        int x = origin.x();
        int y = origin.y();
        costOverlayMaxScaledHeight = footprintHeight;
        
        // Render content with scale using matrix transformation
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        int currentPage = snapshot.currentPage;
        ClipboardUtility.ClipboardEntry entry = ClipboardUtility.getEntryForPage(currentPage);
        int unscaledWidth = snapshot.renderWidth;
        context.fill(0, 0, unscaledWidth, estimatedHeight, 0x80000000);
        
        int currentY = padding;
        clearQuantityHoverBounds();
        int lineCount = 0;
        
        // "Bauplanname [Anzahl]" - Anzahl oben rechts, mit Pfeil-Buttons davor
        String blueprintName;
        String countText;
        
        FoundStatusLabel foundStatus;
        if (currentPage == 1) {
            // Seite 1: Gesamtliste
            blueprintName = "Gesamtliste";
            // Hole aktuelle Anzahl für Seite 1 (aus Textfeld oder Standard 1)
            int quantityForPage1 = getQuantityForPage(1, null);
            countText = formatQuantityText(quantityForPage1);
            foundStatus = resolveOverallFoundStatus();
        } else if (entry != null) {
            // Seite 2+: Einzelner Bauplan
            blueprintName = entry.blueprintName != null ? entry.blueprintName : "Bauplan Name";
            countText = formatQuantityText(entry.quantity > 0 ? entry.quantity : 1);
            foundStatus = resolveEntryFoundStatus(entry);
        } else {
            // Fallback
            blueprintName = "Bauplan Name";
            countText = formatQuantityText(1);
            foundStatus = null;
        }

        int[] buttonPositions = calculateButtonPositions(client, unscaledWidth, snapshot.quantityAreaWidth);
        int leftButtonX = buttonPositions[0];
        int rightButtonX = buttonPositions[1];
        int spaceAfterLeftX = buttonPositions[2];
        int separatorX = buttonPositions[3];
        int spaceAfterSeparatorX = buttonPositions[4];
        int countX = buttonPositions[6];
        
        String leftArrow = "«";
        String rightArrow = "»";
        String separator = "|";
        String space = " ";
        
        // Bauplan-Name links
        context.text(
            client.font,
            blueprintName,
            5, currentY,
            0xFFFFFFFF,
            true
        );

        // Gefunden-Status direkt neben dem Namen
        if (foundStatus != null) {
            int statusX = 5 + client.font.width(blueprintName) + client.font.width(" ");
            context.text(
                client.font,
                foundStatus.text,
                statusX, currentY,
                foundStatus.color,
                true
            );
        }
        
        // Pfeil-Buttons rendern
        int totalPages = ClipboardUtility.getTotalPages();
        
        // Rendere von links nach rechts: « -> Leerzeichen -> | -> Leerzeichen -> »
        // Linker Button « (vorherige Seite)
        boolean canGoLeft = currentPage > 1;
        int leftButtonColor = canGoLeft ? 0xFFFFFFFF : 0xFF808080; // Weiß wenn aktiv, grau wenn inaktiv
        context.text(
            client.font,
            leftArrow,
            leftButtonX, currentY,
            leftButtonColor,
            true
        );
        
        // Leerzeichen nach « (vor |)
        context.text(
            client.font,
            space,
            spaceAfterLeftX, currentY,
            0xFFFFFFFF, // Weiß (unsichtbar, nur für Abstand)
            true
        );
        
        // Trennstrich | zwischen den Buttons
        context.text(
            client.font,
            separator,
            separatorX, currentY,
            0xFF808080, // Grau
            true
        );
        
        // Leerzeichen nach | (vor »)
        context.text(
            client.font,
            space,
            spaceAfterSeparatorX, currentY,
            0xFFFFFFFF, // Weiß (unsichtbar, nur für Abstand)
            true
        );
        
        // Rechter Button » (nächste Seite)
        boolean canGoRight = currentPage < totalPages;
        int rightButtonColor = canGoRight ? 0xFFFFFFFF : 0xFF808080; // Weiß wenn aktiv, grau wenn inaktiv
        context.text(
            client.font,
            rightArrow,
            rightButtonX, currentY,
            rightButtonColor,
            true
        );
        
        // Anzahl oben rechts - als Textfeld wenn in Inventar, sonst als Text
        boolean isInInventory = client.gui.screen() instanceof AbstractContainerScreen;
        int quantity = getQuantityForPage(currentPage, entry);
        
        if (isInInventory) {
            renderQuantityTextField(context, client, countX, currentY, currentPage, quantity, unscaledWidth, scale);
            updateQuantityHoverBounds(countX, currentY, getQuantityAreaWidth(client, countText, true), lineHeight);
        } else {
            // Außerhalb von Inventar: Rendere als Text
            context.text(
                client.font,
                countText,
                countX, currentY,
                0xFFFFFF00, // Gelb für Anzahl
                true
            );
            updateQuantityHoverBounds(countX, currentY, client.font.width(countText), lineHeight);
        }
        currentY += lineHeight;
        lineCount++; // Bauplan-Name Zeile
        
        // Rendere Kosten basierend auf aktueller Seite (vorberechnete Zeilen aus Snapshot)
        int normalStartY = currentY;
        boolean noBlueprints = ClipboardTargetTime.NO_BLUEPRINTS.equals(snapshot.statusMessage);
        if (currentPage != 1 && noBlueprints) {
            context.text(client.font, snapshot.statusMessage, 5, currentY, 0xFF808080, true);
            currentY += lineHeight;
            lineCount++;
        } else if (noBlueprints) {
            context.text(client.font, "Kosten:", 5, currentY, 0xFFAAAAAA, true);
            currentY += lineHeight;
            lineCount++;
            context.text(client.font, snapshot.statusMessage, 5, currentY, 0xFF808080, true);
            currentY += lineHeight;
            lineCount++;
        } else {
            boolean allCollected = ClipboardTargetTime.ALL_COLLECTED.equals(snapshot.statusMessage);
            if (!snapshot.mainCostLines.isEmpty() || allCollected) {
                context.text(client.font, "Kosten:", 5, currentY, 0xFFAAAAAA, true);
                currentY += lineHeight;
                lineCount++;
                normalStartY = currentY;
                for (PreparedCostLine line : snapshot.mainCostLines) {
                    drawPreparedCostLine(context, client, line, currentY, unscaledWidth, isInInventory && snapshot.scrollable);
                    currentY += lineHeight;
                    lineCount++;
                }
                if (allCollected) {
                    context.text(client.font, snapshot.statusMessage, 5, currentY, ClipboardTargetTime.COLLECTED_COLOR, true);
                    currentY += lineHeight;
                    lineCount++;
                }
            }
        }
        int normalEndY = normalStartY;
        if (!snapshot.mainCostLines.isEmpty()) {
            normalEndY = normalStartY + snapshot.mainCostLines.size() * lineHeight;
        }
        drawCostOverflow(context, snapshot, isInInventory, unscaledWidth, normalStartY, normalEndY, scale, x, y);
        
        // Berechne tatsächliche Höhe basierend auf gerenderten Zeilen
        int actualHeight = net.felix.utilities.DragOverlay.OverlayInsets.heightForLines(
                padding, lineHeight, client.font.lineHeight, lineCount);
        
        matrices.popMatrix();
        
        // Berechne skalierte Dimensionen für Button-Positionierung (außerhalb der Matrix-Transformation)
        int width = Math.round(unscaledWidth * scale);
        int actualScaledHeight = Math.round(actualHeight * scale);
        costOverlayScreenX = x;
        costOverlayScreenY = y;
        costOverlayScreenWidth = width;
        costOverlayScreenHeight = actualScaledHeight;
        
        // Rendere Button zum Entfernen (mittig unterhalb des Overlays) - nur in Inventaren
        // WICHTIG: Button wird NACH dem Matrix-Pop gerendert, damit er nicht skaliert wird
        if (isInInventory) {
            int buttonHeight = REMOVE_BUTTON_HEIGHT;
            int buttonSpacing = REMOVE_BUTTON_GAP;
            int buttonY = y + actualScaledHeight + buttonSpacing; // Skalierte Y-Position
            int buttonWidth = (int)((unscaledWidth - 10) * scale); // Skalierte Breite
            int buttonX = x + (width - buttonWidth) / 2; // Mittig im Overlay
            
            // Speichere Button-Positionen für Click-Handler
            deleteButtonX = buttonX;
            deleteButtonY = buttonY;
            deleteButtonWidth = buttonWidth;
            deleteButtonHeight = buttonHeight;
            deleteButtonVisible = true;
            
            // Bestimme Button-Text basierend auf aktueller Seite
            int buttonPage = ClipboardUtility.getCurrentPage();
            Component buttonText = Component.literal(buttonPage == 1 ? "Alle Baupläne entfernen" : "Bauplan entfernen");
            
            // Erstelle oder aktualisiere ButtonWidget (wie im KitViewScreen)
            if (deleteButton == null || deleteButton.getX() != buttonX || deleteButton.getY() != buttonY || 
                deleteButton.getWidth() != buttonWidth || deleteButton.getHeight() != buttonHeight) {
                deleteButton = Button.builder(buttonText, button -> {
                    // Click-Handler wird in handleButtonClick behandelt
                    // Diese Lambda wird nicht direkt aufgerufen, da wir mouseClicked verwenden
                })
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build();
            }
            
            // Vanilla-Buttons mit laufendem Text crashen in 26.3, wenn der Schnitt leer ist (0x40).
            if (buttonWidth >= 8 && buttonIntersectsGui(buttonX, buttonY, buttonWidth, buttonHeight)) {
                deleteButton.extractRenderState(context, mouseX, mouseY, 0.0f);
            } else if (buttonWidth > 0 && buttonHeight > 0) {
                context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, 0xFF000000);
                context.outline(buttonX, buttonY, buttonWidth, buttonHeight, 0xFFFFFFFF);
            }
        } else {
            deleteButtonVisible = false;
        }
        
        // Rendere Bestätigungs-Overlay (wenn aktiv) - nur in Inventaren
        if (showDeleteConfirmation && isInInventory) {
            renderDeleteConfirmation(context, mouseX, mouseY, x, y, width, actualScaledHeight, scale, actualHeight, unscaledWidth);
        } else {
            confirmationButtonsVisible = false;
        }
    }
    
    /**
     * Rendert das Bestätigungs-Overlay für "Alle Baupläne entfernen"
     */
    private static void renderDeleteConfirmation(GuiGraphicsExtractor context, int mouseX, int mouseY, 
                                                  int overlayX, int overlayY, int overlayWidth, int overlayHeight, 
                                                  float scale, int unscaledHeight, int unscaledWidth) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        
        int boxWidth = 250;
        int boxHeight = 100;
        int boxX = overlayX + (overlayWidth - (int)(boxWidth * scale)) / 2;
        int boxY = overlayY + ((int)(unscaledHeight * scale) - (int)(boxHeight * scale)) / 2;
        
        // Hintergrund
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        
        // Rahmen
        context.outline(boxX, boxY, boxWidth, boxHeight, 0xFFFFFFFF);
        
        // Text (in zwei Zeilen)
        String questionText1 = "Sicher das alle Baupläne";
        String questionText2 = "entfernt werden sollen?";
        int textY = boxY + 15;
        int text1Width = client.font.width(questionText1);
        int text2Width = client.font.width(questionText2);
        
        // Erste Zeile
        context.text(
            client.font,
            questionText1,
            boxX + (boxWidth - text1Width) / 2,
            textY,
            0xFFFFFFFF,
            true
        );
        
        // Zweite Zeile
        context.text(
            client.font,
            questionText2,
            boxX + (boxWidth - text2Width) / 2,
            textY + client.font.lineHeight + 2,
            0xFFFFFFFF,
            true
        );
        
        // Buttons (etwas weiter unten, da Text jetzt zwei Zeilen hat)
        int buttonY = boxY + 55;
        int buttonHeight = 20;
        int buttonWidth = 80;
        int buttonSpacing = 20;
        
        // "Ja" Button
        int jaButtonXLocal = boxX + (boxWidth - (buttonWidth * 2 + buttonSpacing)) / 2;
        
        // Erstelle oder aktualisiere ButtonWidget
        if (confirmationJaButton == null || confirmationJaButton.getX() != jaButtonXLocal || 
            confirmationJaButton.getY() != buttonY || confirmationJaButton.getWidth() != buttonWidth || 
            confirmationJaButton.getHeight() != buttonHeight) {
            confirmationJaButton = Button.builder(Component.literal("Ja"), button -> {
                // Click-Handler wird in handleDeleteButtonClick behandelt
            })
            .bounds(jaButtonXLocal, buttonY, buttonWidth, buttonHeight)
            .build();
        }
        
        // Speichere Button-Positionen für Click-Handler
        jaButtonX = jaButtonXLocal;
        jaButtonY = buttonY;
        jaButtonWidth = buttonWidth;
        jaButtonHeight = buttonHeight;
        
        if (buttonIntersectsGui(jaButtonXLocal, buttonY, buttonWidth, buttonHeight)) {
            confirmationJaButton.extractRenderState(context, mouseX, mouseY, 0.0f);
        }
        
        // "Nein" Button
        int neinButtonXLocal = jaButtonXLocal + buttonWidth + buttonSpacing;
        
        // Erstelle oder aktualisiere ButtonWidget
        if (confirmationNeinButton == null || confirmationNeinButton.getX() != neinButtonXLocal || 
            confirmationNeinButton.getY() != buttonY || confirmationNeinButton.getWidth() != buttonWidth || 
            confirmationNeinButton.getHeight() != buttonHeight) {
            confirmationNeinButton = Button.builder(Component.literal("Nein"), button -> {
                // Click-Handler wird in handleButtonClick behandelt
                // Diese Lambda wird nicht direkt aufgerufen, da wir mouseClicked verwenden
            })
            .bounds(neinButtonXLocal, buttonY, buttonWidth, buttonHeight)
            .build();
        }
        
        // Speichere Button-Positionen für Click-Handler
        neinButtonX = neinButtonXLocal;
        neinButtonY = buttonY;
        neinButtonWidth = buttonWidth;
        neinButtonHeight = buttonHeight;
        confirmationButtonsVisible = true;
        
        if (buttonIntersectsGui(neinButtonXLocal, buttonY, buttonWidth, buttonHeight)) {
            confirmationNeinButton.extractRenderState(context, mouseX, mouseY, 0.0f);
        }
    }

    /** True, wenn der Button das GUI schneidet. Sonst erzeugt die laufende Beschriftung einen leeren Scissor. */
    private static boolean buttonIntersectsGui(int x, int y, int buttonWidth, int buttonHeight) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null || buttonWidth <= 0 || buttonHeight <= 0) {
            return false;
        }
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        return x < screenWidth && y < screenHeight && x + buttonWidth > 0 && y + buttonHeight > 0;
    }
    
    /**
     * Multipliziert ein CostItem mit einer Anzahl
     * @param costItem CostItem zum Multiplizieren
     * @param quantity Anzahl zum Multiplizieren
     * @return Neues CostItem mit multipliziertem Amount
     */
    private static CostItem multiplyCostItem(CostItem costItem, int quantity) {
        if (costItem == null || quantity <= 0) {
            return costItem;
        }
        
        if (quantity == 1) {
            return costItem; // Keine Multiplikation nötig
        }
        
        CostItem multiplied = new CostItem();
        multiplied.itemName = costItem.itemName;
        
        // Multipliziere Amount
        Object amount = costItem.amount;
        if (amount == null) {
            multiplied.amount = null;
        } else if (amount instanceof Number) {
            double value = ((Number) amount).doubleValue() * quantity;
            if (amount instanceof Integer) {
                multiplied.amount = (int) value;
            } else if (amount instanceof Long) {
                multiplied.amount = (long) value;
            } else {
                multiplied.amount = value;
            }
        } else if (amount instanceof String) {
            try {
                // Entferne Tausendertrennzeichen und parse
                String cleaned = removeThousandSeparators((String) amount);
                double value = Double.parseDouble(cleaned) * quantity;
                multiplied.amount = value;
            } catch (NumberFormatException e) {
                multiplied.amount = amount; // Fallback: Original behalten
            }
        } else {
            multiplied.amount = amount;
        }
        
        return multiplied;
    }
    
    /**
     * Formatiert einen Amount-Wert: Ganze Zahlen ohne ".0", andere bleiben wie sie sind
     * Alle Werte werden mit Tausendertrennzeichen formatiert (1.000.000)
     */
    private static String formatAmount(Object amount, boolean useGrouping) {
        if (amount == null) {
            return "0";
        }

        if (amount instanceof BigDecimal bigDecimal) {
            return HudNumberSuffixUtility.formatWithSeparators(bigDecimal);
        }

        if (amount instanceof String) {
            try {
                String cleaned = removeThousandSeparators((String) amount);
                BigDecimal parsed = HudNumberSuffixUtility.parseSuffixedValue(cleaned);
                if (parsed != null) {
                    return HudNumberSuffixUtility.formatWithSeparators(parsed);
                }
                double value = Double.parseDouble(cleaned);
                return formatNumberWithSeparators(value);
            } catch (NumberFormatException e) {
                return (String) amount;
            }
        }

        if (amount instanceof Number) {
            Number num = (Number) amount;
            if (num instanceof Double || num instanceof Float) {
                double value = num.doubleValue();
                if (Double.isFinite(value) && value == Math.floor(value)) {
                    return HudNumberSuffixUtility.formatWithSeparators(BigDecimal.valueOf((long) value));
                }
                return formatNumberWithSeparators(value);
            }
            return HudNumberSuffixUtility.formatWithSeparators(new BigDecimal(num.toString()));
        }

        return amount.toString();
    }
    
    /**
     * Formatiert eine Zahl mit Tausendertrennzeichen (1.000.000)
     */
    private static String formatNumberWithSeparators(double value) {
        if (value == Math.floor(value)) {
            return INTEGER_GROUPED_FORMAT.format((long) value);
        }
        return DECIMAL_GROUPED_FORMAT.format(value);
    }
    
    /**
     * Formatiert eine Zahl mit Abkürzung (K, M, B, T) - übernommen aus PlayerHoverStatsUtility
     * @param number Die zu formatierende Zahl
     * @return Formatierter String z.B. "1.5M" für 1.500.000
     */
    private static String formatNumberAbbreviated(BigDecimal number) {
        try {
            return HudNumberSuffixUtility.formatAbbreviated(number);
        } catch (Exception e) {
            return number != null ? number.toPlainString() : "0";
        }
    }

    private static BigDecimal parseAmountToBigDecimal(Object amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (amount instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (amount instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String str = removeThousandSeparators(amount.toString());
        BigDecimal parsed = HudNumberSuffixUtility.parseSuffixedValue(str);
        if (parsed != null) {
            return parsed;
        }
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Entfernt Tausendertrennzeichen aus einem String (z.B. "1.577.697" -> "1577697")
     */
    private static String removeThousandSeparators(String str) {
        if (str == null) return null;
        // Entferne alle Punkte (Tausendertrennzeichen im deutschen Format)
        return str.replace(".", "");
    }
    
    /**
     * Parst einen Amount-Wert zu einer Zahl (behandelt Tausendertrennzeichen)
     */
    private static double parseAmountToDouble(Object amount) {
        if (amount == null) {
            return 0.0;
        }
        if (amount instanceof Number) {
            return ((Number) amount).doubleValue();
        }
        // Wenn es ein String ist, entferne Tausendertrennzeichen und parse
        String str = amount.toString();
        str = removeThousandSeparators(str);
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Normalisiert einen Materialnamen für den Vergleich (entfernt Leerzeichen, ignoriert Groß-/Kleinschreibung)
     */
    private static String normalizeMaterialName(String materialName) {
        return CollectedMaterialsResourcesStorage.normalizeName(materialName);
    }
    
    /**
     * Prüft ob ein CostItem "fertig" ist (owned >= needed)
     * @param costItem Das CostItem zu prüfen
     * @param quantity Die Multiplikationsmenge
     * @return true wenn owned >= needed (oder needed == 0)
     */
    private static boolean isCostItemComplete(CostItem costItem, int quantity) {
        if (costItem == null || costItem.itemName == null || costItem.amount == null) {
            return false;
        }
        
        // Multipliziere CostItem mit Anzahl
        CostItem multipliedItem = multiplyCostItem(costItem, quantity);
        
        // Berechne benötigte Menge
        BigDecimal neededAmount = parseAmountToBigDecimal(multipliedItem.amount);
        if (neededAmount.compareTo(BigDecimal.ZERO) == 0) {
            return true;
        }

        boolean isCoins = "Coins".equalsIgnoreCase(multipliedItem.itemName);
        BigDecimal ownedAmount = isCoins
                ? ClipboardCoinCollector.getCurrentCoins()
                : BigDecimal.valueOf(getOwnedMaterialAmount(costItem.itemName));

        return ownedAmount.compareTo(neededAmount) >= 0;
    }
    
    private static double getOwnedMaterialAmount(String materialName) {
        if (materialName == null) {
            return 0.0;
        }
        if (CollectedMaterialsResourcesStorage.OTHER_KAKTUS.equalsIgnoreCase(materialName)
                || CollectedMaterialsResourcesStorage.OTHER_SEELEN.equalsIgnoreCase(materialName)
                || ClipboardPaperShredsCollector.STORAGE_NAME.equalsIgnoreCase(materialName)) {
            return CollectedMaterialsResourcesStorage.getOtherAmount(materialName);
        }
        // materials + resources in collected_materials-ressources.json
        long storedAmount = CollectedMaterialsResourcesStorage.getSyncedOwnedAmount(materialName);
        if (storedAmount > 0) {
            return storedAmount;
        }

        long ambossAmount = ClipboardAmbossRessourceCollector.getAmbossAmount(materialName);
        if (ambossAmount > 0) {
            return ambossAmount;
        }
        long ressourceAmount = ClipboardAmbossRessourceCollector.getRessourceAmount(materialName);
        if (ressourceAmount > 0) {
            return ressourceAmount;
        }

        return 0.0;
    }
    
    /**
     * Addiert zwei Amount-Werte zusammen
     * @param amount1 Erster Amount-Wert
     * @param amount2 Zweiter Amount-Wert
     * @return Summe als Object (Integer, Double oder String)
     */
    private static Object addAmounts(Object amount1, Object amount2) {
        if (amount1 == null && amount2 == null) {
            return 0;
        }
        if (amount1 == null) {
            return amount2;
        }
        if (amount2 == null) {
            return amount1;
        }
        
        // Parse beide Werte zu Double (behandelt Tausendertrennzeichen)
        double val1 = parseAmountToDouble(amount1);
        double val2 = parseAmountToDouble(amount2);
        double sum = val1 + val2;
        
        // Wenn beide ursprünglich ganze Zahlen waren, gib ganze Zahl zurück
        if (amount1 instanceof Integer && amount2 instanceof Integer) {
            return (int) sum;
        }
        // Prüfe ob Summe eine ganze Zahl ist
        if (sum == Math.floor(sum)) {
            return (long) sum;
        }
        return sum;
    }

    private static final class TotalCostsBundle {
        final List<CostItemWithCategory> normal;

        TotalCostsBundle(List<CostItemWithCategory> normal, List<CostItem> shop) {
            this.normal = normal;
        }
    }

    private static TotalCostsBundle buildTotalCostsBundle(List<ClipboardUtility.ClipboardEntry> entries, int filterQuantity) {
        Map<String, CostItemWithCategory> normalMap = new HashMap<>();
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            int quantity = entry.quantity > 0 ? entry.quantity : 1;
            if (entry.price != null) {
                addPriceCostItemsToTotal(normalMap, entry.price, quantity);
            }
            if (includeShopCosts()) {
                addShopCosts(normalMap, entry.blueprintShop, quantity);
            }
        }

        return new TotalCostsBundle(
                categorizeCategorizedTotals(normalMap, filterQuantity),
                List.of()
        );
    }

    private static void addCategorizedCost(Map<String, CostItemWithCategory> totals, CostItem costItem, int category) {
        if (totals == null || costItem == null || costItem.itemName == null || costItem.amount == null) {
            return;
        }
        int storedCategory = costItem.itemName.equalsIgnoreCase("Coins") ? 0 : category;
        String key = ClipboardTargetTime.totalKey(storedCategory, costItem.itemName);
        CostItemWithCategory existing = totals.get(key);
        if (existing != null) {
            existing.costItem.amount = addAmounts(existing.costItem.amount, costItem.amount);
            return;
        }
        CostItem copy = new CostItem();
        copy.itemName = storedCategory == 0 ? "Coins" : costItem.itemName;
        copy.amount = costItem.amount;
        totals.put(key, new CostItemWithCategory(copy, storedCategory));
    }

    private static List<CostItemWithCategory> categorizeCategorizedTotals(
            Map<String, CostItemWithCategory> totals, int filterQuantity) {
        List<CostItemWithCategory> coins = new ArrayList<>();
        List<CostItemWithCategory> materials = new ArrayList<>();
        List<CostItemWithCategory> ambossItems = new ArrayList<>();
        List<CostItemWithCategory> ressourceItems = new ArrayList<>();
        for (CostItemWithCategory item : totals.values()) {
            if (item == null || item.costItem == null) {
                continue;
            }
            switch (item.category) {
                case 0 -> coins.add(item);
                case 2 -> ambossItems.add(item);
                case 3 -> ressourceItems.add(item);
                default -> materials.add(item);
            }
        }
        Comparator<CostItemWithCategory> byName = Comparator.comparing(item -> item.costItem.itemName);
        boolean materialSortEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaterialSortEnabled;
        if (materialSortEnabled) {
            boolean ascending = CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaterialSortAscending;
            materials.sort((item1, item2) -> {
                MaterialFloorInfo info1 = MaterialDatabase.getMaterialFloorInfo(item1.costItem.itemName);
                MaterialFloorInfo info2 = MaterialDatabase.getMaterialFloorInfo(item2.costItem.itemName);
                int floor1 = info1 != null ? info1.floor : Integer.MAX_VALUE;
                int floor2 = info2 != null ? info2.floor : Integer.MAX_VALUE;
                int floorCompare = ascending ? Integer.compare(floor1, floor2) : Integer.compare(floor2, floor1);
                if (floorCompare != 0) {
                    return floorCompare;
                }
                return item1.costItem.itemName.compareTo(item2.costItem.itemName);
            });
        } else {
            materials.sort(byName);
        }
        ambossItems.sort(byName);
        ressourceItems.sort(byName);

        List<CostItemWithCategory> result = new ArrayList<>();
        boolean costDisplayEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayEnabled;
        int costDisplayMode = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayMode;
        if (costDisplayEnabled && costDisplayMode == 1) {
            coins.removeIf(item -> isCostItemComplete(item.costItem, filterQuantity));
            materials.removeIf(item -> isCostItemComplete(item.costItem, filterQuantity));
            ambossItems.removeIf(item -> isCostItemComplete(item.costItem, filterQuantity));
            ressourceItems.removeIf(item -> isCostItemComplete(item.costItem, filterQuantity));
            result.addAll(coins);
            result.addAll(materials);
            result.addAll(ressourceItems);
            result.addAll(ambossItems);
        } else if (costDisplayEnabled && costDisplayMode == 2) {
            result.addAll(incomplete(coins, filterQuantity));
            result.addAll(incomplete(materials, filterQuantity));
            result.addAll(incomplete(ressourceItems, filterQuantity));
            result.addAll(incomplete(ambossItems, filterQuantity));
            result.addAll(complete(coins, filterQuantity));
            result.addAll(complete(materials, filterQuantity));
            result.addAll(complete(ressourceItems, filterQuantity));
            result.addAll(complete(ambossItems, filterQuantity));
        } else {
            result.addAll(coins);
            result.addAll(materials);
            result.addAll(ressourceItems);
            result.addAll(ambossItems);
        }
        return result;
    }

    private static List<CostItemWithCategory> incomplete(List<CostItemWithCategory> items, int quantity) {
        List<CostItemWithCategory> result = new ArrayList<>();
        for (CostItemWithCategory item : items) {
            if (!isCostItemComplete(item.costItem, quantity)) {
                result.add(item);
            }
        }
        return result;
    }

    private static List<CostItemWithCategory> complete(List<CostItemWithCategory> items, int quantity) {
        List<CostItemWithCategory> result = new ArrayList<>();
        for (CostItemWithCategory item : items) {
            if (isCostItemComplete(item.costItem, quantity)) {
                result.add(item);
            }
        }
        return result;
    }

    private static PreparedCostLine prepareCostLine(Minecraft client, CostItem costItem, int quantity, int category, boolean shopLine) {
        if (costItem == null || costItem.itemName == null || costItem.amount == null) {
            return null;
        }

        CostItem multipliedItem = multiplyCostItem(costItem, quantity);
        boolean isCoins = "Coins".equalsIgnoreCase(multipliedItem.itemName);
        BigDecimal neededAmount = parseAmountToBigDecimal(multipliedItem.amount);

        BigDecimal ownedAmount = isCoins
                ? ClipboardCoinCollector.getCurrentCoins()
                : BigDecimal.valueOf(getOwnedMaterialAmount(costItem.itemName));

        int textColor;
        if (neededAmount.compareTo(BigDecimal.ZERO) == 0) {
            textColor = 0xFF00FF00;
        } else if (ownedAmount.compareTo(neededAmount) >= 0) {
            textColor = 0xFF00FF00;
        } else {
            textColor = 0xFFFF5555;
        }

        String ownedAmountStr = formatAmount(ownedAmount, isCoins);
        String neededAmountStr = formatAmount(multipliedItem.amount, isCoins);
        String materialLine = ownedAmountStr + " / " + neededAmountStr + " " + costItem.itemName;

        String abbreviatedText = "";
        if (isCoins && neededAmount.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            abbreviatedText = " (" + formatNumberAbbreviated(neededAmount) + ")";
        }

        String floorText = "";
        int floorColor = 0xFFFFFFFF;
        MaterialFloorInfo floorInfo =
                MaterialDatabase.getMaterialFloorInfo(multipliedItem.itemName);
        if (floorInfo != null) {
            floorText = MaterialRarityColors.formatMaterialLocationSuffix(floorInfo);
            floorColor = MaterialRarityColors.getMaterialLocationRarityColorArgb(floorInfo);
        }

        String timeText = "";
        String timeTextDetailed = "";
        if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardApproxTimeEnabled) {
            BigDecimal missing = neededAmount.subtract(ownedAmount);
            if (isCoins) {
                double rate = ClipboardTargetTime.coinRate(
                        CCLiveUtilitiesConfig.HANDLER.instance().coinTrackerEnabled,
                        net.felix.utilities.Aincraft.cointracker.CoinTrackerUtility.isTracking()
                                && net.felix.utilities.Aincraft.cointracker.CoinTrackerUtility.isAllowedDimension(),
                        0,
                        net.felix.utilities.Aincraft.cointracker.CoinTrackerUtility.getCoinsPerMinute());
                String[] labels = ClipboardTargetTime.timeLabels(missing, rate);
                timeText = labels[0];
                timeTextDetailed = labels[1];
            } else if (!shopLine) {
                ClipboardTimeEstimate.TimeLabel timeLabel = ClipboardTimeEstimate.buildTimeText(costItem.itemName, missing);
                timeText = timeLabel.compact();
                timeTextDetailed = timeLabel.detailed();
            }
        }

        int mainWidth = client.font.width(materialLine);
        int abbrevWidth = abbreviatedText.isEmpty() ? 0 : client.font.width(abbreviatedText);
        int floorWidth = floorText.isEmpty() ? 0 : client.font.width(floorText);
        int timeWidth = timeTextWidth(client, timeText);
        int totalWidth = mainWidth + abbrevWidth + floorWidth + timeWidth;

        return new PreparedCostLine(materialLine, textColor, abbreviatedText, floorText,
                floorColor, timeText, timeTextDetailed, mainWidth, abbrevWidth, floorWidth, totalWidth);
    }

    private static List<PreparedCostLine> prepareCategorizedCostLines(Minecraft client,
            List<CostItemWithCategory> items, int quantity, boolean shopLine) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<PreparedCostLine> lines = new ArrayList<>(items.size());
        for (CostItemWithCategory item : items) {
            PreparedCostLine line = prepareCostLine(client, item.costItem, quantity, item.category, shopLine);
            if (line != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static void drawPreparedCostLine(GuiGraphicsExtractor context, Minecraft client, PreparedCostLine line, int y,
            int unscaledWidth, boolean reserveScrollbar) {
        context.text(client.font, line.mainText, 5, y, line.mainColor, true);
        int cursor = 5 + line.mainWidth;
        if (!line.abbrevText.isEmpty()) {
            context.text(client.font, line.abbrevText, cursor, y, 0xFF808080, true);
            cursor += line.abbrevWidth;
        }
        if (!line.floorText.isEmpty()) {
            context.text(client.font, line.floorText, cursor, y, line.floorColor, true);
            cursor += line.floorWidth;
        }
        if (!line.timeText.isEmpty()) {
            int right = unscaledWidth - 5;
            if (reserveScrollbar) {
                right = unscaledWidth - COST_SCROLLBAR_WIDTH - 4;
            }
            int detailedWidth = ClipboardTargetTime.isCalculating(line.timeTextDetailed)
                    ? timeTextWidth(client, line.timeTextDetailed)
                    : client.font.width(line.timeTextDetailed);
            String shown = ClipboardTargetTime.chooseTimeText(
                    line.timeText, line.timeTextDetailed, cursor, detailedWidth, right);
            drawTimeText(context, client, shown, cursor, y);
        }
    }

    private static int timeTextWidth(Minecraft client, String timeText) {
        if (timeText == null || timeText.isEmpty()) {
            return 0;
        }
        if (ClipboardTargetTime.isCalculating(timeText)) {
            return client.font.width(ClipboardTargetTime.CALCULATING_PREFIX) + ClipboardTargetTime.calculatingDotsWidth();
        }
        return client.font.width(timeText);
    }

    private static void drawTimeText(GuiGraphicsExtractor context, Minecraft client, String timeText, int x, int y) {
        if (ClipboardTargetTime.isCalculating(timeText)) {
            context.text(client.font, ClipboardTargetTime.CALCULATING_PREFIX, x, y, ClipboardTargetTime.TIME_COLOR, true);
            int dotsX = x + client.font.width(ClipboardTargetTime.CALCULATING_PREFIX);
            int dotTop = y + ClipboardTargetTime.CALCULATING_DOT_TOP;
            int size = ClipboardTargetTime.CALCULATING_DOT_SIZE;
            int step = size + ClipboardTargetTime.CALCULATING_DOT_GAP;
            for (int i = 0; i < ClipboardTargetTime.CALCULATING_DOT_COUNT; i++) {
                int left = dotsX + i * step;
                context.fill(left + 1, dotTop + 1, left + size + 1, dotTop + size + 1, 0xFF3F3F00);
                context.fill(left, dotTop, left + size, dotTop + size, ClipboardTargetTime.TIME_COLOR);
            }
            return;
        }
        context.text(client.font, timeText, x, y, ClipboardTargetTime.TIME_COLOR, true);
    }

    private static void drawCostOverflow(GuiGraphicsExtractor context, RenderSnapshot snapshot, boolean inInventory,
            int unscaledWidth, int normalStartY, int normalEndY, float scale, int overlayX, int overlayY) {
        costScrollbarVisible = false;
        if (snapshot == null || normalEndY <= normalStartY) {
            return;
        }
        if (inInventory && snapshot.scrollable) {
            int trackX = unscaledWidth - COST_SCROLLBAR_WIDTH - 2;
            int trackHeight = normalEndY - normalStartY;
            context.fill(trackX, normalStartY, trackX + COST_SCROLLBAR_WIDTH, normalEndY, 0xFF404040);
            int fullCount = Math.max(1, snapshot.scrollMax + snapshot.mainCostLines.size());
            int handleHeight = Math.max(8, trackHeight * snapshot.mainCostLines.size() / fullCount);
            handleHeight = Math.min(handleHeight, trackHeight);
            int maxOffset = Math.max(1, snapshot.scrollMax);
            int travel = Math.max(1, trackHeight - handleHeight);
            int handleY = normalStartY + (int) (travel * (snapshot.scrollOffset / (float) maxOffset));
            int handleColor = costScrollbarDragging ? 0xFF808080 : 0xFF606060;
            context.fill(trackX + 1, handleY, trackX + COST_SCROLLBAR_WIDTH - 1, handleY + handleHeight, handleColor);
            costScrollbarVisible = true;
            costScrollTrackX = overlayX + Math.round(trackX * scale);
            costScrollTrackY = overlayY + Math.round(normalStartY * scale);
            costScrollTrackWidth = Math.max(6, Math.round(COST_SCROLLBAR_WIDTH * scale));
            costScrollTrackHeight = Math.round(trackHeight * scale);
            costScrollHandleY = overlayY + Math.round(handleY * scale);
            costScrollHandleHeight = Math.max(4, Math.round(handleHeight * scale));
            costScrollMaxOffset = snapshot.scrollMax;
        } else if (!inInventory && snapshot.overflow) {
            context.fill(unscaledWidth - 2, normalStartY, unscaledWidth, normalEndY, 0x66FFFF55);
        }
    }

    private static int costScrollMaxOffset;

    private static int readDisplayQuantity(int page, ClipboardUtility.ClipboardEntry entry) {
        if (quantityTextField != null && quantityTextFieldPage == page) {
            String text = quantityTextField.getValue();
            if (text != null && !text.trim().isEmpty()) {
                try {
                    int quantity = Integer.parseInt(text.trim());
                    if (quantity > 0) {
                        return quantity;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (page == 1) {
            return getPage1Quantity();
        }
        if (entry == null) {
            return 1;
        }
        return entry.quantity > 0 ? entry.quantity : 1;
    }

    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        
        if (!CCLiveUtilitiesConfig.isModRuntimeEnabled()) {
            return false;
        }
// Deaktiviere in der general_lobby Dimension
        try {
            var client = net.minecraft.client.Minecraft.getInstance();
            if (client != null && client.level != null) {
                String dimensionId = client.level.dimension().identifier().toString();
                if ("minecraft:general_lobby".equals(dimensionId)) {
                    return false;
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
        
        return isConfigEnabled();
    }

    @Override
    public boolean isConfigEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled &&
               CCLiveUtilitiesConfig.HANDLER.instance().showClipboard;
    }
    
    @Override
    public Component getTooltip() {
        return Component.literal("Clipboard - Zeigt Bauplan-Kosten an");
    }
    
    @Override
    public void resetToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardX = 10;
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardY = 10;
    }
    
    @Override
    public void resetSizeToDefault() {
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale = 1.0f;
    }
    
    /**
     * Berechnet die Button-Positionen basierend auf dem Bauplan-Namen
     * @param client MinecraftClient
     * @param blueprintName Name des Bauplans
     * @param statusLabel Optionaler Status neben dem Namen (z. B. "[Gefunden]")
     * @param countText Text für [Anzahl]
     * @param unscaledWidth Unskalierte Breite des Overlays
     * @return Array mit [leftButtonX, rightButtonX, spaceAfterLeftX, separatorX, spaceAfterSeparatorX, spaceAfterRightX, countX]
     */
    private static int[] calculateButtonPositions(Minecraft client, int unscaledWidth, int quantityAreaWidth) {
        String leftArrow = "«";
        String rightArrow = "»";
        String separator = "|";
        String space = " ";
        int leftArrowWidth = client.font.width(leftArrow);
        int rightArrowWidth = client.font.width(rightArrow);
        int separatorWidth = client.font.width(separator);
        int spaceWidth = client.font.width(space);
        int totalButtonWidth = leftArrowWidth + spaceWidth + separatorWidth + spaceWidth + rightArrowWidth;
        int leftButtonX = ClipboardLayout.arrowLeft(unscaledWidth, quantityAreaWidth, totalButtonWidth,
                HEADER_COUNT_PADDING, QUANTITY_AFTER_BUTTONS_SPACING);
        int spaceAfterLeftX = leftButtonX + leftArrowWidth;
        int separatorX = spaceAfterLeftX + spaceWidth;
        int spaceAfterSeparatorX = separatorX + separatorWidth;
        int spaceAfterRightX = spaceAfterSeparatorX + spaceWidth;
        int rightButtonX = spaceAfterRightX;
        int countX = unscaledWidth - quantityAreaWidth - HEADER_COUNT_PADDING;
        return new int[]{leftButtonX, rightButtonX, spaceAfterLeftX, separatorX, spaceAfterSeparatorX, spaceAfterRightX, countX};
    }

    private static ClipboardLayout.Point screenOrigin(Minecraft client, int scaledWidth, int footprintHeight) {
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        return ClipboardLayout.clampOrigin(
                CCLiveUtilitiesConfig.HANDLER.instance().clipboardX,
                CCLiveUtilitiesConfig.HANDLER.instance().clipboardY,
                scaledWidth, footprintHeight, screenWidth, screenHeight);
    }

    private static ClipboardLayout.Point currentScreenOrigin(Minecraft client) {
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) {
            scale = 1.0f;
        }
        int width = costOverlayScreenWidth > 0
                ? costOverlayScreenWidth
                : Math.round(calculateUnscaledWidth() * scale);
        int height = costOverlayMaxScaledHeight > 0 ? costOverlayMaxScaledHeight : costOverlayScreenHeight;
        return screenOrigin(client, width, height);
    }
    
    /**
     * Prüft ob Maus über einem der Pfeil-Buttons ist
     * @param mouseX Maus-X-Position (unskaliert)
     * @param mouseY Maus-Y-Position (unskaliert)
     * @param buttonY Y-Position der Buttons (unskaliert)
     * @param leftButtonX X-Position des linken Buttons (unskaliert)
     * @param rightButtonX X-Position des rechten Buttons (unskaliert)
     * @return 0 = kein Button, 1 = linker Button, 2 = rechter Button
     */
    private static int getHoveredButton(int mouseX, int mouseY, int buttonY, int leftButtonX, int rightButtonX, Minecraft client) {
        int lineHeight = client.font.lineHeight + 2;
        if (mouseY < buttonY || mouseY > buttonY + lineHeight) {
            return 0; // Nicht über der Zeile
        }
        
        String leftArrow = "«";
        String rightArrow = "»";
        int leftArrowWidth = client.font.width(leftArrow);
        int rightArrowWidth = client.font.width(rightArrow);
        
        // Prüfe linker Button
        if (mouseX >= leftButtonX && mouseX <= leftButtonX + leftArrowWidth) {
            return 1; // Linker Button
        }
        
        // Prüfe rechter Button
        if (mouseX >= rightButtonX && mouseX <= rightButtonX + rightArrowWidth) {
            return 2; // Rechter Button
        }
        
        return 0; // Kein Button
    }
    
    /**
     * Behandelt Klicks auf die Pfeil-Buttons
     * @param mouseX Maus-X-Position (skaliert)
     * @param mouseY Maus-Y-Position (skaliert)
     * @return true wenn ein Button geklickt wurde
     */
    public static boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (net.felix.utilities.Overall.itemviewer.ui.ViewerInputHandler.blocksUnderlyingOverlay(mouseX, mouseY)) {
            return false;
        }
        if (vertical == 0.0 || !clipboardInputEnabled() || !pointInClipboard(mouseX, mouseY)) {
            return false;
        }
        if (!costScrollable) {
            return false;
        }
        setCostScroll(costScrollOffset + (vertical > 0.0 ? -1 : 1));
        return true;
    }

    public static boolean handleScrollbarDrag(double mouseY) {
        if (!costScrollbarDragging) {
            return false;
        }
        int travel = Math.max(1, costScrollTrackHeight - costScrollHandleHeight);
        float progress = (float) (mouseY - costScrollTrackY - costScrollHandleHeight / 2.0) / travel;
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        setCostScroll(Math.round(progress * Math.max(0, costScrollMaxOffset)));
        return true;
    }

    public static boolean handleScrollbarRelease(int button) {
        if (costScrollbarDragging && button == InputConstants.MOUSE_BUTTON_LEFT) {
            costScrollbarDragging = false;
            return true;
        }
        return false;
    }

    private static void setCostScroll(int offset) {
        int clamped = Math.max(0, offset);
        if (clamped == costScrollOffset) {
            return;
        }
        costScrollOffset = clamped;
        cachedRenderSnapshot = null;
    }

    private static boolean clipboardInputEnabled() {
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled
                && CCLiveUtilitiesConfig.HANDLER.instance().showClipboard
                && !(CCLiveUtilitiesConfig.HANDLER.instance().clipboardHideEmpty
                && visiblePagesReady && visibleStoredIndexes.length == 0);
    }

    private static boolean pointInClipboard(double mouseX, double mouseY) {
        return mouseX >= costOverlayScreenX && mouseX <= costOverlayScreenX + costOverlayScreenWidth
                && mouseY >= costOverlayScreenY && mouseY <= costOverlayScreenY + costOverlayScreenHeight;
    }

    private static boolean handleScrollbarClick(double mouseX, double mouseY) {
        if (!costScrollbarVisible) {
            return false;
        }
        int trackRight = costScrollTrackX + costScrollTrackWidth;
        if (mouseX < costScrollTrackX || mouseX > trackRight
                || mouseY < costScrollTrackY || mouseY > costScrollTrackY + costScrollTrackHeight) {
            return false;
        }
        if (mouseY >= costScrollHandleY && mouseY <= costScrollHandleY + costScrollHandleHeight) {
            costScrollbarDragging = true;
            return true;
        }
        int travel = Math.max(1, costScrollTrackHeight - costScrollHandleHeight);
        float progress = (float) (mouseY - costScrollTrackY - costScrollHandleHeight / 2.0) / travel;
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        setCostScroll(Math.round(progress * Math.max(0, costScrollMaxOffset)));
        return true;
    }

    public static boolean handleButtonClick(int mouseX, int mouseY) {
        if (net.felix.utilities.Overall.itemviewer.ui.ViewerInputHandler.blocksUnderlyingOverlay(mouseX, mouseY)) {
            return false;
        }
        if (!clipboardInputEnabled()) {
            return false;
        }
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        
        // Nur in Inventaren funktionieren
        if (!(client.gui.screen() instanceof AbstractContainerScreen)) {
            return false;
        }
        if (handleScrollbarClick(mouseX, mouseY)) {
            return true;
        }
        
        // Prüfe zuerst, ob Bestätigungs-Overlay offen ist - dann prüfe Ja/Nein Buttons
        if (showDeleteConfirmation) {
            if (confirmationJaButton != null && confirmationJaButton.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false)) {
                // Alle Baupläne entfernen
                ClipboardUtility.clearClipboard();
                showDeleteConfirmation = false;
                confirmationButtonsVisible = false;
                // Setze Textfeld zurück
                resetQuantityTextField();
                // Gehe zurück zu Seite 1
                ClipboardUtility.setCurrentPage(1);
                return true;
            }
            
            if (confirmationNeinButton != null && confirmationNeinButton.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false)) {
                // Bestätigung abbrechen
                showDeleteConfirmation = false;
                confirmationButtonsVisible = false;
                return true;
            }
            
            // Klick war nicht auf Ja/Nein Buttons
            return false;
        }
        
        // Bestätigungs-Overlay ist nicht offen - prüfe auf Haupt-Delete-Button
        if (deleteButton != null && deleteButton.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false)) {
            // Bestätigungs-Overlay öffnen (für Seite 1) oder direkt entfernen (für Seiten 2+)
            int buttonPage = ClipboardUtility.getCurrentPage();
            if (buttonPage == 1) {
                showDeleteConfirmation = true;
            } else {
                // Entferne aktuellen Bauplan
                ClipboardUtility.ClipboardEntry currentEntry = ClipboardUtility.getEntryForPage(buttonPage);
                if (currentEntry != null && currentEntry.blueprintName != null) {
                    // Verwende clipboardId falls vorhanden, um den richtigen Bauplan zu entfernen
                    ClipboardUtility.removeBlueprint(currentEntry.blueprintName, currentEntry.clipboardId);
                    // Setze Textfeld zurück, damit der nachrückende Bauplan sein eigenes Textfeld bekommt
                    resetQuantityTextField();
                }
            }
            return true;
        }
        
        RenderSnapshot clickSnapshot = ensureRenderSnapshot(client);
        if (clickSnapshot == null) {
            return false;
        }
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) scale = 1.0f;
        ClipboardLayout.Point origin = currentScreenOrigin(client);
        int x = origin.x();
        int y = origin.y();
        
        // Konvertiere skalierten Maus-Koordinaten zu unskalierten
        int unscaledMouseX = (int) ((mouseX - x) / scale);
        int unscaledMouseY = (int) ((mouseY - y) / scale);
        
        int padding = 5;
        int currentY = padding;
        
        // Y-Position der Buttons (bei "Bauplanname [Anzahl]" Zeile)
        // Muss exakt mit renderInGame übereinstimmen!
        int buttonY = currentY;
        int currentPage = clickSnapshot.currentPage;
        
        int[] buttonPositions = calculateButtonPositions(client, clickSnapshot.renderWidth, clickSnapshot.quantityAreaWidth);
        int leftButtonX = buttonPositions[0];
        int rightButtonX = buttonPositions[1];
        
        // Prüfe welcher Button gehovered ist
        int hoveredButton = getHoveredButton(unscaledMouseX, unscaledMouseY, buttonY, leftButtonX, rightButtonX, client);
        
        if (hoveredButton == 1) {
            // Linker Button: Vorherige Seite
            if (currentPage > 1) {
                setCurrentPage(currentPage - 1);
                return true;
            }
        } else if (hoveredButton == 2) {
            // Rechter Button: Nächste Seite
            int totalPages = getTotalPages();
            if (currentPage < totalPages) {
                setCurrentPage(currentPage + 1);
                return true;
            }
        }
        
        // Prüfe Delete-Button
        if (handleDeleteButtonClick(mouseX, mouseY)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Behandelt Klicks auf den Delete-Button
     * Verwendet die Button-Positionen, die beim Rendering gesetzt wurden
     */
    private static boolean handleDeleteButtonClick(int mouseX, int mouseY) {
        if (!CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled || 
            !CCLiveUtilitiesConfig.HANDLER.instance().showClipboard) {
            return false;
        }
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        
        // Nur in Inventaren funktionieren
        if (!(client.gui.screen() instanceof AbstractContainerScreen)) {
            return false;
        }
        
        // Prüfe zuerst, ob Bestätigungs-Overlay offen ist
        if (showDeleteConfirmation) {
            // Bestätigungs-Overlay ist offen - prüfe Ja/Nein Buttons
            // Verwende die beim Rendering gespeicherten Positionen
            if (!confirmationButtonsVisible) {
                return false;
            }
            
            // "Ja" Button
            if (mouseX >= jaButtonX && mouseX <= jaButtonX + jaButtonWidth &&
                mouseY >= jaButtonY && mouseY <= jaButtonY + jaButtonHeight) {
                // Alle Baupläne entfernen
                ClipboardUtility.clearClipboard();
                showDeleteConfirmation = false;
                confirmationButtonsVisible = false;
                // Setze Textfeld zurück
                resetQuantityTextField();
                // Gehe zurück zu Seite 1
                ClipboardUtility.setCurrentPage(1);
                return true;
            }
            
            // "Nein" Button
            if (mouseX >= neinButtonX && mouseX <= neinButtonX + neinButtonWidth &&
                mouseY >= neinButtonY && mouseY <= neinButtonY + neinButtonHeight) {
                // Bestätigung abbrechen
                showDeleteConfirmation = false;
                confirmationButtonsVisible = false;
                return true;
            }
            
            // Klick war nicht auf Ja/Nein Buttons
            return false;
        }
        
        // Bestätigungs-Overlay ist nicht offen - prüfe auf Haupt-Delete-Button
        // Verwende die beim Rendering gespeicherten Positionen
        if (!deleteButtonVisible) {
            return false;
        }
        
        // Prüfe ob Maus über Button ist
        if (mouseX >= deleteButtonX && mouseX <= deleteButtonX + deleteButtonWidth &&
            mouseY >= deleteButtonY && mouseY <= deleteButtonY + deleteButtonHeight) {
            
            int deletePage = ClipboardUtility.getCurrentPage();
            ClipboardUtility.ClipboardEntry deleteEntry = ClipboardUtility.getEntryForPage(deletePage);
            
            if (deletePage == 1) {
                // Seite 1: Öffne Bestätigungs-Overlay
                showDeleteConfirmation = true;
                return true;
            } else {
                // Seite 2+: Entferne direkt den Bauplan
                if (deleteEntry != null) {
                    String blueprintNameToRemove = deleteEntry.blueprintName;
                    // Verwende clipboardId falls vorhanden, um den richtigen Bauplan zu entfernen
                    ClipboardUtility.removeBlueprint(blueprintNameToRemove, deleteEntry.clipboardId);
                    // Setze Textfeld zurück, damit der nachrückende Bauplan sein eigenes Textfeld bekommt
                    resetQuantityTextField();
                    // Gehe zur vorherigen Seite, wenn möglich
                    int totalPages = ClipboardUtility.getTotalPages();
                    if (deletePage > totalPages) {
                        ClipboardUtility.setCurrentPage(totalPages);
                    }
                    // Die Seiten-Nummerierung wird automatisch angepasst (Bauplan von Seite 3 rückt auf Seite 2 vor)
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Gibt den Tooltip-Text für einen Button zurück
     * @param button 1 = linker Button, 2 = rechter Button
     * @return Tooltip-Text
     */
    public static Component getButtonTooltip(int button) {
        int currentPage = ClipboardUtility.getCurrentPage();
        int totalPages = ClipboardUtility.getTotalPages();
        
        if (button == 1) {
            // Linker Button: Vorherige Seite
            if (currentPage > 1) {
                return Component.literal("Seite " + (currentPage - 1));
            } else {
                return Component.literal("Keine vorherige Seite");
            }
        } else if (button == 2) {
            // Rechter Button: Nächste Seite
            if (currentPage < totalPages) {
                return Component.literal("Seite " + (currentPage + 1));
            } else {
                return Component.literal("Keine nächste Seite");
            }
        }
        
        return Component.empty();
    }
    
    /**
     * Rendert Tooltips für die Pfeil-Buttons
     * @param context GuiGraphicsExtractor
     * @param mouseX Maus-X-Position (skaliert)
     * @param mouseY Maus-Y-Position (skaliert)
     */
    public static void renderButtonTooltips(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (!CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled || 
            !CCLiveUtilitiesConfig.HANDLER.instance().showClipboard) {
            return;
        }
        
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        
        // Hide tooltips if F1 menu (debug screen) is open
        if (client.gui.hud.isHidden()) {
            return;
        }
        
        // Hide tooltips if Tab list is open (like other overlays)
        if (KeyBindingUtility.isPlayerListKeyPressed()) {
            return;
        }
        
        if (hideHover) {
            return;
        }
        
        RenderSnapshot tooltipSnapshot = ensureRenderSnapshot(client);
        if (tooltipSnapshot == null || (CCLiveUtilitiesConfig.HANDLER.instance().clipboardHideEmpty
                && visibleStoredIndexes.length == 0)) {
            return;
        }
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) scale = 1.0f;
        ClipboardLayout.Point origin = currentScreenOrigin(client);
        int x = origin.x();
        int y = origin.y();
        
        // Konvertiere skalierten Maus-Koordinaten zu unskalierten
        int unscaledMouseX = (int) ((mouseX - x) / scale);
        int unscaledMouseY = (int) ((mouseY - y) / scale);
        
        int padding = 5;
        int currentY = padding;
        
        // Y-Position der Buttons
        int buttonY = currentY;
        
        int[] buttonPositions = calculateButtonPositions(client, tooltipSnapshot.renderWidth, tooltipSnapshot.quantityAreaWidth);
        int leftButtonX = buttonPositions[0];
        int rightButtonX = buttonPositions[1];
        
        // Prüfe welcher Button gehovered ist
        int hoveredButton = getHoveredButton(unscaledMouseX, unscaledMouseY, buttonY, leftButtonX, rightButtonX, client);
        
        if (hoveredButton > 0) {
            Component tooltip = getButtonTooltip(hoveredButton);
            if (tooltip != null && !tooltip.getString().isEmpty()) {
                // Konvertiere Button-Position zurück zu skalierten Koordinaten für Tooltip
                int tooltipX = (int) (x + (hoveredButton == 1 ? leftButtonX : rightButtonX) * scale);
                int tooltipY = (int) (y + buttonY * scale);
                context.setComponentTooltipForNextFrame(client.font, java.util.List.of(tooltip), tooltipX, tooltipY);
            }
            return;
        }
        
        // Anzahl-Tooltip: verwende gerenderte Hover-Bounds (exakt synchron mit renderInGame)
        if (quantityHoverBoundsValid
            && unscaledMouseX >= quantityHoverX && unscaledMouseX <= quantityHoverX + quantityHoverWidth
            && unscaledMouseY >= quantityHoverY && unscaledMouseY <= quantityHoverY + quantityHoverHeight) {
            int tooltipX = (int) (x + quantityHoverX * scale);
            int tooltipY = (int) (y + quantityHoverY * scale);
            context.setComponentTooltipForNextFrame(client.font, java.util.List.of(Component.literal("Anzahl")), tooltipX, tooltipY);
        }
    }
    
    /**
     * Setzt das Textfeld zurück, damit es beim nächsten Rendern mit dem korrekten Wert neu erstellt wird
     * Wird aufgerufen, wenn ein Bauplan entfernt wird, damit der nachrückende Bauplan sein eigenes Textfeld bekommt
     */
    private static void resetQuantityTextField() {
        quantityTextField = null;
        quantityTextFieldPage = -1;
    }
    
    /**
     * Speichert die aktuelle Mengeneingabe und setzt leere oder ungültige Werte auf 1.
     */
    public static void finalizeQuantityTextField() {
        if (quantityTextField == null) {
            return;
        }
        
        int page = quantityTextFieldPage;
        int quantity = parseQuantityText(quantityTextField.getValue());
        quantityTextField.setValue(String.valueOf(quantity));
        applyQuantityForPage(page, quantity);
        
        quantityTextField.setFocused(false);
        resetQuantityTextField();
        invalidateRenderSnapshot();
    }
    
    private static int getPage1Quantity() {
        if (page1Quantity < 1) {
            page1Quantity = Math.max(1, CCLiveUtilitiesConfig.HANDLER.instance().clipboardPage1Quantity);
        }
        return page1Quantity;
    }

    private static void setPage1Quantity(int quantity) {
        page1Quantity = Math.max(1, quantity);
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardPage1Quantity = page1Quantity;
        CCLiveUtilitiesConfig.HANDLER.save();
    }

    private static void applyQuantityForPage(int page, int quantity) {
        quantity = Math.max(1, quantity);
        if (page == 1) {
            setPage1Quantity(quantity);
            return;
        }

        ClipboardUtility.ClipboardEntry entry = ClipboardUtility.getEntryForPage(page);
        if (entry != null) {
            entry.quantity = quantity;
            ClipboardUtility.saveClipboardEntries();
        }
    }
    
    private static void normalizeQuantityTextFieldIfNeeded() {
        if (quantityTextField == null || quantityTextField.isFocused()) {
            return;
        }
        
        String text = quantityTextField.getValue();
        if (text == null || text.trim().isEmpty()) {
            quantityTextField.setValue("1");
            applyQuantityForPage(quantityTextFieldPage, 1);
            invalidateRenderSnapshot();
        }
    }
    
    private static int parseQuantityText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 1;
        }
        try {
            int quantity = Integer.parseInt(text.trim());
            return quantity > 0 ? quantity : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }
    
    private static String formatQuantityText(int quantity) {
        return QUANTITY_PREFIX + quantity;
    }
    
    private static int getQuantityPrefixWidth(Minecraft client) {
        return client.font.width(QUANTITY_PREFIX);
    }
    
    private static int getQuantityPrefixAreaWidth(Minecraft client) {
        return QUANTITY_FIELD_PADDING + getQuantityPrefixWidth(client);
    }
    
    private static int getQuantityInputWidth(Minecraft client) {
        return getQuantityPrefixAreaWidth(client) + QUANTITY_TEXT_FIELD_WIDTH;
    }
    
    private static int getQuantityAreaWidth(Minecraft client, String countText, boolean isInInventory) {
        if (isInInventory) {
            return getQuantityInputWidth(client) + QUANTITY_FIELD_OUTLINE_PADDING;
        }
        return client.font.width(countText);
    }
    
    private static int getNavigationButtonsWidth(Minecraft client) {
        return client.font.width("«")
            + client.font.width(" ")
            + client.font.width("|")
            + client.font.width(" ")
            + client.font.width("»");
    }
    
    private static FoundStatusLabel resolveOverallFoundStatus() {
        if (!visiblePagesReady) {
            return resolveOverallFoundStatus(ClipboardUtility.getEntries());
        }
        List<ClipboardUtility.ClipboardEntry> visible = new ArrayList<>();
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        for (int index : visibleStoredIndexes) {
            if (index >= 0 && index < entries.size()) {
                visible.add(entries.get(index));
            }
        }
        return resolveOverallFoundStatus(visible);
    }

    private static FoundStatusLabel resolveOverallFoundStatus(List<ClipboardUtility.ClipboardEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (!isClipboardBlueprintFound(entry)) {
                return new FoundStatusLabel("[Nicht alle Gefunden]", 0xFFFF5555);
            }
        }
        return new FoundStatusLabel("[Alle Gefunden]", 0xFF55FF55);
    }

    private static FoundStatusLabel resolveEntryFoundStatus(ClipboardUtility.ClipboardEntry entry) {
        if (entry == null || entry.blueprintName == null || entry.blueprintName.isEmpty()) {
            return null;
        }
        if (isClipboardBlueprintFound(entry)) {
            return new FoundStatusLabel("[Gefunden]", 0xFF55FF55);
        }
        return new FoundStatusLabel("[Nicht gefunden]", 0xFFFF5555);
    }

    private static boolean isClipboardBlueprintFound(ClipboardUtility.ClipboardEntry entry) {
        if (entry == null || entry.blueprintName == null || entry.blueprintName.isEmpty()) {
            return false;
        }
        ItemData item = null;
        try {
            if (entry.clipboardId != null) {
                item = ItemCatalog.findItemByNameAndClipboardId(entry.blueprintName, entry.clipboardId);
            }
            if (item == null) {
                item = ItemCatalog.findItemByName(entry.blueprintName);
            }
        } catch (Exception ignored) {
        }
        String rarity = (item != null && item.info != null) ? item.info.rarity : null;
        try {
            return BPViewerUtility.getInstance().isBlueprintFoundExact(entry.blueprintName, rarity);
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void renderQuantityFieldBackground(GuiGraphicsExtractor context, int x, int y, int width, int height, boolean focused) {
        int outlineColor = focused ? 0xFFFFFFFF : 0xFFA0A0A0;
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, outlineColor);
        context.fill(x, y, x + width, y + height, 0xFF000000);
    }
    
    private static int getQuantityFieldTextY(int fieldY, int fieldHeight) {
        return fieldY + (fieldHeight - 8) / 2;
    }
    
    /**
     * Gibt die aktuelle Anzahl für eine Seite zurück (mit Fallback auf 1)
     */
    private static int getQuantityForPage(int page, ClipboardUtility.ClipboardEntry entry) {
        return readDisplayQuantity(page, entry);
    }
    
    /**
     * Rendert das Textfeld für die [Anzahl] Eingabe
     */
    private static void renderQuantityTextField(GuiGraphicsExtractor context, Minecraft client, 
                                                 int quantityAreaX, int countY, int page, int currentQuantity,
                                                 int unscaledWidth, float scale) {
        // Erstelle oder aktualisiere Textfeld wenn nötig
        if (quantityTextField != null && quantityTextFieldPage != page) {
            finalizeQuantityTextField();
        }
        if (quantityTextField == null || quantityTextFieldPage != page) {
            quantityTextField = new EditBox(
                client.font,
                0, 0, // Wird später gesetzt
                QUANTITY_TEXT_FIELD_WIDTH, // Breite
                client.font.lineHeight + 2, // Höhe
                Component.literal("")
            );
            quantityTextField.setMaxLength(10); // Maximal 10 Ziffern
            quantityTextField.setBordered(false);
            quantityTextField.setTextColor(0xFFFFFF00);
            quantityTextField.setResponder(text -> {
                // Validierung: Stelle sicher, dass es eine positive Zahl ist
                if (text != null && !text.trim().isEmpty()) {
                    try {
                        int value = Integer.parseInt(text.trim());
                        if (value <= 0) {
                            quantityTextField.setValue("1");
                            if (quantityTextFieldPage == 1) {
                                page1Quantity = 1;
                            }
                        } else if (quantityTextFieldPage == 1) {
                            page1Quantity = value;
                        } else {
                            ClipboardUtility.ClipboardEntry pageEntry =
                                    ClipboardUtility.getEntryForPage(quantityTextFieldPage);
                            if (pageEntry != null) {
                                pageEntry.quantity = value;
                            }
                        }
                    } catch (NumberFormatException e) {
                        quantityTextField.setValue("1");
                        if (quantityTextFieldPage == 1) {
                            page1Quantity = 1;
                        }
                    }
                }
                invalidateRenderSnapshot();
            });
            quantityTextFieldPage = page;
            
            // Setze initialen Wert
            ClipboardUtility.ClipboardEntry entry = ClipboardUtility.getEntryForPage(page);
            if (entry != null) {
                quantityTextField.setValue(String.valueOf(entry.quantity > 0 ? entry.quantity : 1));
            } else if (page == 1) {
                quantityTextField.setValue(String.valueOf(getPage1Quantity()));
            } else {
                quantityTextField.setValue("1");
            }
        }
        
        normalizeQuantityTextFieldIfNeeded();
        
        // Berechne Position (unskaliert, relativ zum Overlay)
        // Textfeld soll rechts von den Buttons stehen, nicht überlappen
        // FESTE Position: rechts vom rechten Button, damit es sich nicht verschiebt
        int fieldHeight = client.font.lineHeight + 2;
        int innerFieldHeight = 8;
        int prefixAreaWidth = getQuantityPrefixAreaWidth(client);
        int totalWidth = getQuantityInputWidth(client);
        int textFieldX = quantityAreaX + prefixAreaWidth;
        int fieldY = countY;
        int textY = getQuantityFieldTextY(fieldY, fieldHeight);
        
        renderQuantityFieldBackground(
            context,
            quantityAreaX,
            fieldY,
            totalWidth,
            fieldHeight,
            quantityTextField.isFocused()
        );
        
        context.text(
            client.font,
            QUANTITY_PREFIX,
            quantityAreaX + QUANTITY_FIELD_PADDING,
            textY,
            0xFFFFFF00,
            true
        );
        
        quantityTextField.setX(textFieldX);
        quantityTextField.setY(textY);
        quantityTextField.setWidth(QUANTITY_TEXT_FIELD_WIDTH);
        quantityTextField.setHeight(innerFieldHeight);
        
        // Rendere Textfeld (Matrix-Stack ist bereits aktiv in renderInGame)
        // Mouse position für Textfeld-Rendering (unskaliert, relativ zum Overlay)
        ClipboardLayout.Point fieldOrigin = currentScreenOrigin(client);
        int unscaledMouseX = (int) ((client.mouseHandler.xpos() - fieldOrigin.x()) / scale);
        int unscaledMouseY = (int) ((client.mouseHandler.ypos() - fieldOrigin.y()) / scale);
        quantityTextField.extractRenderState(context, unscaledMouseX, unscaledMouseY, 0.0f);
        
        // Aktualisiere entry.quantity wenn Textfeld geändert wurde
        String text = quantityTextField.getValue();
        if (text != null && !text.trim().isEmpty()) {
            try {
                int quantity = Integer.parseInt(text.trim());
                if (quantity > 0) {
                    ClipboardUtility.ClipboardEntry entry = ClipboardUtility.getEntryForPage(page);
                    if (entry != null) {
                        entry.quantity = quantity;
                        // Speichere in Config
                        ClipboardUtility.saveClipboardEntries();
                    }
                }
            } catch (NumberFormatException e) {
                // Ignoriere
            }
        }
    }
    
    /**
     * Behandelt Klicks auf das [Anzahl] Textfeld
     */
    public static boolean handleQuantityTextFieldClick(int mouseX, int mouseY, int button) {
        if (net.felix.utilities.Overall.itemviewer.ui.ViewerInputHandler.blocksUnderlyingOverlay(mouseX, mouseY)) {
            return false;
        }
        if (quantityTextField == null) {
            return false;
        }
        
        Minecraft client = Minecraft.getInstance();
        if (client == null || !(client.gui.screen() instanceof AbstractContainerScreen)) {
            return false;
        }
        
        // Hole Overlay-Position und Skalierung
        ClipboardLayout.Point overlayOrigin = currentScreenOrigin(client);
        int overlayX = overlayOrigin.x();
        int overlayY = overlayOrigin.y();
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) scale = 1.0f;
        
        // Konvertiere Maus-Koordinaten zu unskalierten Koordinaten (relativ zum Overlay)
        int unscaledMouseX = (int) ((mouseX - overlayX) / scale);
        int unscaledMouseY = (int) ((mouseY - overlayY) / scale);
        
        // Prüfe ob Klick innerhalb des kombinierten x+Zahlenfelds ist (unskaliert)
        int textFieldX = quantityTextField.getX();
        int textFieldY = quantityTextField.getY();
        int innerFieldHeight = quantityTextField.getHeight();
        int quantityAreaX = textFieldX - getQuantityPrefixAreaWidth(client);
        int totalWidth = getQuantityAreaWidth(client, formatQuantityText(1), true);
        int outerFieldHeight = client.font.lineHeight + 2;
        int fieldY = textFieldY - (outerFieldHeight - innerFieldHeight) / 2;
        
        if (unscaledMouseX >= quantityAreaX && unscaledMouseX <= quantityAreaX + totalWidth
            && unscaledMouseY >= fieldY && unscaledMouseY <= fieldY + outerFieldHeight) {
            if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                quantityTextField.setFocused(true);
                quantityTextField.setValue("");
                return true;
            }
            
            quantityTextField.setFocused(true);
            int clickX = Math.max(unscaledMouseX, textFieldX);
            quantityTextField.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(clickX, unscaledMouseY, new net.minecraft.client.input.MouseButtonInfo(button, 0)), false);
            return true;
        }
        
        if (quantityTextField.isFocused()) {
            normalizeQuantityTextFieldIfNeeded();
            quantityTextField.setFocused(false);
        }
        
        return false;
    }
    
    /**
     * Behandelt Tasteneingaben für das [Anzahl] Textfeld
     * Verarbeitet auch Zeicheneingaben, indem es die Zeichen aus den KeyCodes extrahiert
     */
    public static boolean isQuantityTextFieldFocused() {
        return quantityTextField != null && quantityTextField.isFocused();
    }

    public static boolean handleQuantityTextFieldKeyPress(int keyCode, int scanCode, int modifiers) {
        if (quantityTextField != null && quantityTextField.isFocused()) {
            // Verarbeite normale Tasteneingaben (Backspace, Enter, etc.)
            if (quantityTextField.keyPressed(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, modifiers))) {
                return true;
            }
            
            // Für Zeicheneingaben: Konvertiere KeyCode zu Zeichen (Zahlen 0-9 und Nummernfeld)
            char digit = 0;
            Character typedDigit = net.felix.utilities.Other.keybinding.KeyBindingUtility.digitFromScancode(keyCode);
            if (typedDigit != null) {
                digit = typedDigit;
            }
            
            if (digit != 0) {
                return quantityTextField.charTyped(new net.minecraft.client.input.CharacterEvent(digit));
            }
        }
        return false;
    }
    
    /**
     * Behandelt Zeicheneingaben für das [Anzahl] Textfeld (wird über keyPressed aufgerufen)
     */
    public static boolean handleQuantityTextFieldCharTyped(char chr, int modifiers) {
        if (quantityTextField != null && quantityTextField.isFocused()) {
            return quantityTextField.charTyped(new net.minecraft.client.input.CharacterEvent(chr));
        }
        return false;
    }
    
    
    /**
     * Blendet das Clipboard-Hover aus (wird bei Tab/F1 aufgerufen)
     */
    public static void hideHover() {
        hideHover = true;
    }
    
    /**
     * Prüft ob das Hover ausgeblendet werden soll (wird jeden Tick aufgerufen)
     */
    public static void updateHideHover() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        // Prüfe ob die Spielerliste-Taste gedrückt ist (respektiert benutzerdefinierte Key Bindings)
        boolean playerListKeyPressed = KeyBindingUtility.isPlayerListKeyPressed();

        // Prüfe ob F1 aktiv ist (wie alle anderen GUIs - über hudHidden)
        boolean f1Active = client.gui.hud.isHidden();

        // Wenn Spielerliste-Taste gedrückt ist oder F1 aktiv ist, dann ausblenden
        if (playerListKeyPressed || f1Active) {
            hideHover = true;
        } else {
            // Weder Spielerliste-Taste noch F1 aktiv - zeige Overlay sofort wieder an (ohne Delay)
            hideHover = false;
        }
    }
}


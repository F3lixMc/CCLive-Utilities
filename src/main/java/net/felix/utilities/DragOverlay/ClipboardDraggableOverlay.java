package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.ItemViewer.CostItem;
import net.felix.utilities.Overall.HudNumberSuffixUtility;
import net.felix.utilities.Overall.InformationenUtility;
import net.felix.utilities.Overall.KeyBindingUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Draggable Overlay für das Clipboard (Bauplan-Kosten)
 */
public class ClipboardDraggableOverlay implements DraggableOverlay {
    
    /**
     * Hilfsklasse für CostItem mit Kategorie-Information
     */
    private static class CostItemWithCategory {
        CostItem costItem;
        int category; // 0 = Coins, 1 = Materialien, 2 = Amboss, 3 = Ressource
        
        CostItemWithCategory(CostItem costItem, int category) {
            this.costItem = costItem;
            this.category = category;
        }
    }
    
    private static CostItem resolveAmboss(net.felix.utilities.ItemViewer.PriceData price) {
        if (price == null) {
            return null;
        }
        return price.Amboss != null ? price.Amboss : price.amboss;
    }
    
    private static CostItem resolveRessource(net.felix.utilities.ItemViewer.PriceData price) {
        if (price == null) {
            return null;
        }
        return price.Ressource != null ? price.Ressource : price.ressource;
    }
    
    private static void addPriceCostItemsToDisplayList(List<CostItemWithCategory> list, net.felix.utilities.ItemViewer.PriceData price) {
        if (price == null || list == null) {
            return;
        }
        if (price.coin != null) list.add(new CostItemWithCategory(price.coin, 0));
        if (price.material1 != null) list.add(new CostItemWithCategory(price.material1, 1));
        if (price.material2 != null) list.add(new CostItemWithCategory(price.material2, 1));
        if (price.material3 != null) list.add(new CostItemWithCategory(price.material3, 1));
        if (price.material4 != null) list.add(new CostItemWithCategory(price.material4, 1));
        if (price.material5 != null) list.add(new CostItemWithCategory(price.material5, 1));
        CostItem amboss = resolveAmboss(price);
        if (amboss != null) list.add(new CostItemWithCategory(amboss, 2));
        CostItem ressource = resolveRessource(price);
        if (ressource != null) list.add(new CostItemWithCategory(ressource, 3));
    }

    /**
     * Wendet die Kostenanzeige-Filterung an (Ausblenden / Ans Ende setzen).
     * Modus 1 entfernt fertige Items (außer Coins), Modus 2 sortiert sie ans Ende.
     */
    private static void filterCostItemsForDisplay(List<CostItemWithCategory> costItemsWithCategory, int quantity) {
        if (costItemsWithCategory == null || costItemsWithCategory.isEmpty()) {
            return;
        }

        boolean costDisplayEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayEnabled;
        if (!costDisplayEnabled) {
            return;
        }

        int costDisplayMode = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayMode;
        if (costDisplayMode == 1) {
            costItemsWithCategory.removeIf(item -> {
                if (item.category == 0) {
                    return false;
                }
                return isCostItemComplete(item.costItem, quantity);
            });
        } else if (costDisplayMode == 2) {
            List<CostItemWithCategory> notComplete = new ArrayList<>();
            List<CostItemWithCategory> complete = new ArrayList<>();

            for (CostItemWithCategory item : costItemsWithCategory) {
                if (item.category == 0) {
                    notComplete.add(item);
                } else if (isCostItemComplete(item.costItem, quantity)) {
                    complete.add(item);
                } else {
                    notComplete.add(item);
                }
            }

            notComplete.sort((item1, item2) -> Integer.compare(item1.category, item2.category));
            complete.sort((item1, item2) -> Integer.compare(item1.category, item2.category));

            costItemsWithCategory.clear();
            costItemsWithCategory.addAll(notComplete);
            costItemsWithCategory.addAll(complete);
        }
    }

    private static int countVisiblePriceCostLines(net.felix.utilities.ItemViewer.PriceData price, int quantity) {
        List<CostItemWithCategory> costItemsWithCategory = new ArrayList<>();
        addPriceCostItemsToDisplayList(costItemsWithCategory, price);
        filterCostItemsForDisplay(costItemsWithCategory, quantity);
        return costItemsWithCategory.size();
    }
    
    private static void addPriceCostItemsToTotal(Map<String, CostItem> totalCostsMap, net.felix.utilities.ItemViewer.PriceData price, int quantity) {
        if (price == null || totalCostsMap == null) {
            return;
        }
        addCostItemToTotal(totalCostsMap, multiplyCostItem(price.coin, quantity));
        addCostItemToTotal(totalCostsMap, multiplyCostItem(price.material1, quantity));
        addCostItemToTotal(totalCostsMap, multiplyCostItem(price.material2, quantity));
        addCostItemToTotal(totalCostsMap, multiplyCostItem(price.material3, quantity));
        addCostItemToTotal(totalCostsMap, multiplyCostItem(price.material4, quantity));
        addCostItemToTotal(totalCostsMap, multiplyCostItem(price.material5, quantity));
        addCostItemToTotal(totalCostsMap, multiplyCostItem(resolveAmboss(price), quantity));
        addCostItemToTotal(totalCostsMap, multiplyCostItem(resolveRessource(price), quantity));
    }
    
    private static void collectRequiredMaterialsFromPrice(net.felix.utilities.ItemViewer.PriceData price, Set<String> requiredMaterials) {
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
    private static final int DEFAULT_HEIGHT = 150;
    private static final String QUANTITY_PREFIX = "x";
    private static final int QUANTITY_FIELD_PADDING = 4;
    private static final int QUANTITY_TEXT_FIELD_WIDTH = 50;
    private static final int QUANTITY_FIELD_OUTLINE_PADDING = 2;
    private static final int QUANTITY_AFTER_BUTTONS_SPACING = 8;
    private static final int HEADER_BUTTON_GAP = 5;
    private static final int HEADER_COUNT_PADDING = 5;
    private static final int HEADER_NAME_PADDING = 5;
    private static final int COST_LINE_HORIZONTAL_PADDING = 10;
    
    // TextField für [Anzahl] Eingabe
    private static TextFieldWidget quantityTextField = null;
    private static int quantityTextFieldPage = -1; // Seite, für die das Textfeld aktiv ist
    private static int page1Quantity = 1; // Gespeicherte Anzahl für Seite 1 (Gesamtliste)
    
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
    private static ButtonWidget deleteButton = null;
    private static ButtonWidget confirmationJaButton = null;
    private static ButtonWidget confirmationNeinButton = null;
    
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
        MinecraftClient client = MinecraftClient.getInstance();
        int tick = client != null && client.world != null ? (int) client.world.getTime() : -1;
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
    
    public static void resetCollectedMaterials() {
        clipboardMaterials.clear();
        invalidateMaterialsSyncCache();
        invalidateWidthCache();
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
    }
    
    private static void setCurrentPage(int page) {
        int totalPages = getTotalPages();
        // Clamp page to valid range
        page = Math.max(1, Math.min(page, totalPages));
        CCLiveUtilitiesConfig.HANDLER.instance().clipboardCurrentPage = page;
        CCLiveUtilitiesConfig.HANDLER.save();
    }
    
    // Gesamtanzahl der Seiten (wird später dynamisch berechnet)
    private static int getTotalPages() {
        return Math.max(1, CCLiveUtilitiesConfig.HANDLER.instance().clipboardTotalPages);
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
    
    /** Gecachte Breitenberechnung – pro Tick nur einmal neu berechnen. */
    private static int cachedUnscaledWidth = -1;
    private static int widthCacheTick = -1;
    
    private static void invalidateWidthCache() {
        cachedUnscaledWidth = -1;
        widthCacheTick = -1;
    }
    
    /**
     * Calculate unscaled width (base width without scale factor)
     * Berechnet basierend auf dem neuen Format: "Bauplanname [Anzahl]", Kostenliste
     * Berücksichtigt tatsächliche Bauplan-Namen aus dem Clipboard
     */
    private static int calculateUnscaledWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return DEFAULT_WIDTH;
        int tick = client.world != null ? (int) client.world.getTime() : -1;
        if (tick >= 0 && widthCacheTick == tick && cachedUnscaledWidth >= 0) {
            return cachedUnscaledWidth;
        }
        ensureClipboardMaterialsSynced();
        int result = computeUnscaledWidth(client);
        if (tick >= 0) {
            widthCacheTick = tick;
            cachedUnscaledWidth = result;
        }
        return result;
    }
    
    private static int computeUnscaledWidth(MinecraftClient client) {
        // Berechne maximale Breite basierend auf typischen Texten
        int maxWidth = DEFAULT_WIDTH;
        
        // "Bauplan Kosten:" oder "Kosten:"
        int costsHeaderWidth = client.textRenderer.getWidth("Bauplan Kosten:");
        maxWidth = Math.max(maxWidth, costsHeaderWidth + 10);
        
        // Beispiel-Kosten: "Eichenholz: 10x"
        int costWidth = client.textRenderer.getWidth("Eichenholz: 10x");
        maxWidth = Math.max(maxWidth, costWidth + 10);
        
        // Optional: "Bauplan Shop Kosten:"
        if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts) {
            int shopCostsHeaderWidth = client.textRenderer.getWidth("Bauplan Shop Kosten:");
            maxWidth = Math.max(maxWidth, shopCostsHeaderWidth + 10);
        }
        
        // Berechne Breite für "Bauplanname xAnzahl" + Buttons
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (entry.blueprintName != null) {
                String countText = formatQuantityText(entry.quantity > 0 ? entry.quantity : 1);
                maxWidth = Math.max(maxWidth, computeHeaderLineWidth(client, entry.blueprintName, countText));
            }
        }
        
        int quantityForPage1 = getQuantityForPage(1, null);
        maxWidth = Math.max(maxWidth, computeHeaderLineWidth(
            client,
            "Gesamtliste",
            formatQuantityText(quantityForPage1)
        ));
        
        // Prüfe alle Kostenzeilen inkl. "(Ebene X)" und Coin-Abkürzungen
        int page1Quantity = getQuantityForPage(1, null);
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            int entryQuantity = entry.quantity > 0 ? entry.quantity : 1;
            
            if (entry.price != null) {
                CostItem[] costItems = {
                    entry.price.coin, entry.price.material1, entry.price.material2,
                    entry.price.Amboss, entry.price.Ressource
                };
                for (CostItem costItem : costItems) {
                    maxWidth = Math.max(maxWidth, measureCostLineWidth(client, costItem, entryQuantity));
                }
            }
            
            if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts
                && entry.blueprintShop != null && entry.blueprintShop.price != null) {
                CostItem[] shopCostItems = {
                    entry.blueprintShop.price.coin, entry.blueprintShop.price.paper_shreds
                };
                for (CostItem costItem : shopCostItems) {
                    maxWidth = Math.max(maxWidth, measureCostLineWidth(client, costItem, entryQuantity));
                }
            }
        }
        
        for (CostItem costItem : calculateTotalCosts(false)) {
            maxWidth = Math.max(maxWidth, measureCostLineWidth(client, costItem, page1Quantity));
        }
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts) {
            for (CostItem costItem : calculateTotalCosts(true)) {
                maxWidth = Math.max(maxWidth, measureCostLineWidth(client, costItem, page1Quantity));
            }
        }
        
        return maxWidth;
    }
    
    /**
     * Calculate unscaled height (base height without scale factor)
     * Entspricht dem F6-Vorschau-Layout in renderInEditMode (inkl. Titel und Trennlinie)
     */
    private static int calculateUnscaledHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return DEFAULT_HEIGHT;
        
        int lineHeight = client.textRenderer.fontHeight + 2;
        int padding = 5;
        
        int height = padding;
        height += lineHeight; // "Pinnwand"
        height += 2; // Trennlinie unter dem Titel
        height += lineHeight; // "Bauplanname xAnzahl"
        height += lineHeight; // "Bauplan Kosten:"
        height += lineHeight * 4; // Coins, Material, Amboss, Ressource
        
        if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts) {
            height += lineHeight; // "Bauplan Shop Kosten:"
            height += lineHeight * 2; // Coins, Pergamentfetzen
        }
        
        height += padding;
        return height;
    }
    
    @Override
    public void renderInEditMode(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        
        // Get scale
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) scale = 1.0f;
        
        // Render border for edit mode (scaled)
        context.drawStrokedRectangle(x, y, width, height, 0xFFFF0000);
        
        // Render background (scaled)
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // Render content with scale using matrix transformation
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            matrices.popMatrix();
            return;
        }
        
        int lineHeight = client.textRenderer.fontHeight + 2;
        int padding = 5;
        int currentY = padding;
        int unscaledWidth = calculateUnscaledWidth();
        
        // Titel "Pinnwand"
        context.drawText(
            client.textRenderer,
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
        
        int countWidth = client.textRenderer.getWidth(countText);
        
        // Berechne Positionen für Buttons und Text
        int countX = unscaledWidth - countWidth - 5; // Position für [Anzahl]
        
        // Pfeil-Buttons: « und » links vom [Anzahl] Feld
        String leftArrow = "«";
        String rightArrow = "»";
        String separator = "|";
        String space = " "; // Leerzeichen für gleichmäßigen Abstand
        int leftArrowWidth = client.textRenderer.getWidth(leftArrow);
        int rightArrowWidth = client.textRenderer.getWidth(rightArrow);
        int separatorWidth = client.textRenderer.getWidth(separator);
        int spaceWidth = client.textRenderer.getWidth(space);
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
        context.drawText(
            client.textRenderer,
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
        context.drawText(
            client.textRenderer,
            leftArrow,
            leftButtonX, currentY,
            leftButtonColor,
            true
        );
        
        // Leerzeichen nach « (vor |)
        context.drawText(
            client.textRenderer,
            space,
            spaceAfterLeftX, currentY,
            0xFFFFFFFF, // Weiß (unsichtbar, nur für Abstand)
            true
        );
        
        // Trennstrich | zwischen den Buttons
        context.drawText(
            client.textRenderer,
            separator,
            separatorX, currentY,
            0xFF808080, // Grau
            true
        );
        
        // Leerzeichen nach | (vor »)
        context.drawText(
            client.textRenderer,
            space,
            spaceAfterSeparatorX, currentY,
            0xFFFFFFFF, // Weiß (unsichtbar, nur für Abstand)
            true
        );
        
        // Rechter Button » (nächste Seite)
        boolean canGoRight = currentPage < totalPages;
        int rightButtonColor = canGoRight ? 0xFFFFFFFF : 0xFF808080; // Weiß wenn aktiv, grau wenn inaktiv
        context.drawText(
            client.textRenderer,
            rightArrow,
            rightButtonX, currentY,
            rightButtonColor,
            true
        );
        
        // Anzahl oben rechts
        context.drawText(
            client.textRenderer,
            countText,
            countX, currentY,
            0xFFFFFF00, // Gelb für Anzahl
            true
        );
        currentY += lineHeight;
        
        // Im F6-Menü: Immer statisches Beispiel-Design
        context.drawText(
            client.textRenderer,
            "Bauplan Kosten:",
            5, currentY,
            0xFFAAAAAA,
            true
        );
        currentY += lineHeight;
        
        context.drawText(
            client.textRenderer,
            "Coins",
            5, currentY,
            0xFFFFFFFF,
            true
        );
        currentY += lineHeight;
        
        context.drawText(
            client.textRenderer,
            "Material",
            5, currentY,
            0xFFFFFFFF,
            true
        );
        currentY += lineHeight;
        
        context.drawText(
            client.textRenderer,
            "Amboss",
            5, currentY,
            0xFFFFFFFF,
            true
        );
        currentY += lineHeight;
        
        context.drawText(
            client.textRenderer,
            "Ressource",
            5, currentY,
            0xFFFFFFFF,
            true
        );
        currentY += lineHeight;
        
        // Optional: "Bauplan Shop Kosten:" (wenn aktiviert)
        if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts) {
            context.drawText(
                client.textRenderer,
                "Bauplan Shop Kosten:",
                5, currentY,
                0xFFAAAAAA,
                true
            );
            currentY += lineHeight;
            
            context.drawText(
                client.textRenderer,
                "Coins",
                5, currentY,
                0xFFFFFFFF,
                true
            );
            currentY += lineHeight;
            
            context.drawText(
                client.textRenderer,
                "Pergamentfetzen",
                5, currentY,
                0xFFFFFFFF,
                true
            );
        }
        
        matrices.popMatrix();
    }
    
    /**
     * Rendert das Overlay im normalen Spiel (außerhalb des F6-Menüs)
     * Zeigt die tatsächlichen Clipboard-Daten an
     */
    public static void renderInGame(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled ||
            !CCLiveUtilitiesConfig.HANDLER.instance().showClipboard) {
            return;
        }

        // Deaktiviere in der general_lobby Dimension
        try {
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.world != null) {
                String dimensionId = client.world.getRegistryKey().getValue().toString();
                if ("minecraft:general_lobby".equals(dimensionId)) {
                    return;
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        
        ensureClipboardMaterialsSynced();
        
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
        if (client.options.hudHidden) {
            return;
        }
        
        // Hide overlay if Tab list is open (like other overlays)
        if (KeyBindingUtility.isPlayerListKeyPressed()) {
            return;
        }
        
        int x = CCLiveUtilitiesConfig.HANDLER.instance().clipboardX;
        int y = CCLiveUtilitiesConfig.HANDLER.instance().clipboardY;
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) scale = 1.0f;
        
        // Render content with scale using matrix transformation
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        // Berechne Höhe für Background (nach Matrix-Transformation, damit Background und Text in derselben Ebene sind)
        int lineHeight = client.textRenderer.fontHeight + 2;
        int padding = 5;
        int estimatedLineCount = calculateEstimatedLineCount();
        int estimatedHeight = padding + (estimatedLineCount * lineHeight) + padding;
        
        int currentPage = ClipboardUtility.getCurrentPage();
        ClipboardUtility.ClipboardEntry entry = ClipboardUtility.getEntryForPage(currentPage);
        int unscaledWidth = resolveRenderWidth(client, currentPage, entry);
        context.fill(0, 0, unscaledWidth, estimatedHeight, 0x80000000);
        
        int currentY = padding;
        clearQuantityHoverBounds();
        int lineCount = 0;
        
        // "Bauplanname [Anzahl]" - Anzahl oben rechts, mit Pfeil-Buttons davor
        String blueprintName;
        String countText;
        
        if (currentPage == 1) {
            // Seite 1: Gesamtliste
            blueprintName = "Gesamtliste";
            // Hole aktuelle Anzahl für Seite 1 (aus Textfeld oder Standard 1)
            int quantityForPage1 = getQuantityForPage(1, null);
            countText = formatQuantityText(quantityForPage1);
        } else if (entry != null) {
            // Seite 2+: Einzelner Bauplan
            blueprintName = entry.blueprintName != null ? entry.blueprintName : "Bauplan Name";
            countText = formatQuantityText(entry.quantity > 0 ? entry.quantity : 1);
        } else {
            // Fallback
            blueprintName = "Bauplan Name";
            countText = formatQuantityText(1);
        }
        
        // Berechne Button-Positionen mit Hilfsmethode
        int[] buttonPositions = calculateButtonPositions(client, blueprintName, countText, unscaledWidth);
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
        context.drawText(
            client.textRenderer,
            blueprintName,
            5, currentY,
            0xFFFFFFFF,
            true
        );
        
        // Pfeil-Buttons rendern
        int totalPages = ClipboardUtility.getTotalPages();
        
        // Rendere von links nach rechts: « -> Leerzeichen -> | -> Leerzeichen -> »
        // Linker Button « (vorherige Seite)
        boolean canGoLeft = currentPage > 1;
        int leftButtonColor = canGoLeft ? 0xFFFFFFFF : 0xFF808080; // Weiß wenn aktiv, grau wenn inaktiv
        context.drawText(
            client.textRenderer,
            leftArrow,
            leftButtonX, currentY,
            leftButtonColor,
            true
        );
        
        // Leerzeichen nach « (vor |)
        context.drawText(
            client.textRenderer,
            space,
            spaceAfterLeftX, currentY,
            0xFFFFFFFF, // Weiß (unsichtbar, nur für Abstand)
            true
        );
        
        // Trennstrich | zwischen den Buttons
        context.drawText(
            client.textRenderer,
            separator,
            separatorX, currentY,
            0xFF808080, // Grau
            true
        );
        
        // Leerzeichen nach | (vor »)
        context.drawText(
            client.textRenderer,
            space,
            spaceAfterSeparatorX, currentY,
            0xFFFFFFFF, // Weiß (unsichtbar, nur für Abstand)
            true
        );
        
        // Rechter Button » (nächste Seite)
        boolean canGoRight = currentPage < totalPages;
        int rightButtonColor = canGoRight ? 0xFFFFFFFF : 0xFF808080; // Weiß wenn aktiv, grau wenn inaktiv
        context.drawText(
            client.textRenderer,
            rightArrow,
            rightButtonX, currentY,
            rightButtonColor,
            true
        );
        
        // Anzahl oben rechts - als Textfeld wenn in Inventar, sonst als Text
        boolean isInInventory = client.currentScreen instanceof HandledScreen;
        int quantity = getQuantityForPage(currentPage, entry);
        
        if (isInInventory) {
            renderQuantityTextField(context, client, countX, currentY, currentPage, quantity, unscaledWidth, scale);
            updateQuantityHoverBounds(countX, currentY, getQuantityAreaWidth(client, countText, true), lineHeight);
        } else {
            // Außerhalb von Inventar: Rendere als Text
            context.drawText(
                client.textRenderer,
                countText,
                countX, currentY,
                0xFFFFFF00, // Gelb für Anzahl
                true
            );
            updateQuantityHoverBounds(countX, currentY, client.textRenderer.getWidth(countText), lineHeight);
        }
        currentY += lineHeight;
        lineCount++; // Bauplan-Name Zeile
        
        // Rendere Kosten basierend auf aktueller Seite
        if (currentPage == 1) {
            // Seite 1: Gesamtliste - Zeige zusammengerechnete Kosten
            List<CostItem> totalCosts = calculateTotalCosts(false);
            
            if (!totalCosts.isEmpty()) {
                context.drawText(
                    client.textRenderer,
                    "Kosten:",
                    5, currentY,
                    0xFFAAAAAA,
                    true
                );
                currentY += lineHeight;
                lineCount++;
                
                // Rendere alle zusammengerechneten Kosten in der Reihenfolge (Materialien, Amboss, Ressourcen)
                // Verwende quantity aus dem Textfeld (wenn vorhanden)
                for (int i = 0; i < totalCosts.size(); i++) {
                    CostItem costItem = totalCosts.get(i);
                    if (renderCostItem(context, client, costItem, currentY, quantity)) {
                        currentY += lineHeight;
                        lineCount++;
                    }
                }
                
                // Optional: Bauplan Shop Kosten (wenn aktiviert)
                if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts) {
                    List<CostItem> totalShopCosts = calculateTotalCosts(true);
                    
                    if (!totalShopCosts.isEmpty()) {
                        context.drawText(
                            client.textRenderer,
                            "Bauplan Shop Kosten:",
                            5, currentY,
                            0xFFAAAAAA,
                            true
                        );
                        currentY += lineHeight;
                        lineCount++;
                        
                        // Rendere alle zusammengerechneten Shop-Kosten in der Reihenfolge
                        // Verwende quantity aus dem Textfeld (wenn vorhanden)
                        for (int i = 0; i < totalShopCosts.size(); i++) {
                            CostItem costItem = totalShopCosts.get(i);
                            if (renderCostItem(context, client, costItem, currentY, quantity)) {
                                currentY += lineHeight;
                                lineCount++;
                            }
                        }
                    }
                }
            } else {
                // Keine Kosten vorhanden
                context.drawText(
                    client.textRenderer,
                    "Kosten:",
                    5, currentY,
                    0xFFAAAAAA,
                    true
                );
                currentY += lineHeight;
                lineCount++;
                context.drawText(
                    client.textRenderer,
                    "Keine Baupläne im Clipboard",
                    5, currentY,
                    0xFF808080,
                    true
                );
                currentY += lineHeight;
                lineCount++;
            }
        } else if (entry != null && entry.price != null) {
            // Seite 2+: Einzelner Bauplan - Zeige tatsächliche Kosten
            context.drawText(
                client.textRenderer,
                "Kosten:",
                5, currentY,
                0xFFAAAAAA,
                true
            );
            currentY += lineHeight;
            lineCount++;
            
            // Rendere Kosten (nur Coins, Material1, Material2, Amboss, Ressource) - multipliziert mit Anzahl
            // quantity wurde bereits oben berechnet
            
            // Erstelle Liste der Kosten-Items mit Kategorie-Information
            List<CostItemWithCategory> costItemsWithCategory = new ArrayList<>();
            addPriceCostItemsToDisplayList(costItemsWithCategory, entry.price);
            
            // Kostenanzeige-Filterung/Sortierung (nur für normale Kosten, nicht für Bauplan Shop Kosten)
            boolean costDisplayEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayEnabled;
            if (costDisplayEnabled) {
                filterCostItemsForDisplay(costItemsWithCategory, quantity);
            }
            
            for (CostItemWithCategory itemWithCategory : costItemsWithCategory) {
                if (renderCostItem(context, client, itemWithCategory.costItem, currentY, quantity)) {
                    currentY += lineHeight;
                    lineCount++;
                }
            }
            
            // Optional: "Bauplan Shop Kosten:" (wenn aktiviert und vorhanden) - multipliziert mit Anzahl
            if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts && 
                entry.blueprintShop != null && entry.blueprintShop.price != null) {
                context.drawText(
                    client.textRenderer,
                    "Bauplan Shop Kosten:",
                    5, currentY,
                    0xFFAAAAAA,
                    true
                );
                currentY += lineHeight;
                lineCount++;
                
                if (renderCostItem(context, client, entry.blueprintShop.price.coin, currentY, quantity)) {
                    currentY += lineHeight;
                    lineCount++;
                }
                
                if (renderCostItem(context, client, entry.blueprintShop.price.paper_shreds, currentY, quantity)) {
                    currentY += lineHeight;
                    lineCount++;
                }
            }
        } else {
            // Keine Daten: Zeige leere Meldung
            context.drawText(
                client.textRenderer,
                "Keine Baupläne im Clipboard",
                5, currentY,
                0xFF808080,
                true
            );
            currentY += lineHeight;
            lineCount++;
        }
        
        // Berechne tatsächliche Höhe basierend auf gerenderten Zeilen
        int actualHeight = padding + (lineCount * lineHeight) + padding;
        
        matrices.popMatrix();
        
        // Berechne skalierte Dimensionen für Button-Positionierung (außerhalb der Matrix-Transformation)
        int width = Math.round(unscaledWidth * scale);
        int actualScaledHeight = Math.round(actualHeight * scale);
        
        // Rendere Button zum Entfernen (mittig unterhalb des Overlays) - nur in Inventaren
        // WICHTIG: Button wird NACH dem Matrix-Pop gerendert, damit er nicht skaliert wird
        if (isInInventory) {
            int buttonHeight = 20;
            int buttonSpacing = 5; // Abstand zwischen Overlay und Button
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
            Text buttonText = Text.literal(buttonPage == 1 ? "Alle Baupläne entfernen" : "Bauplan entfernen");
            
            // Erstelle oder aktualisiere ButtonWidget (wie im KitViewScreen)
            if (deleteButton == null || deleteButton.getX() != buttonX || deleteButton.getY() != buttonY || 
                deleteButton.getWidth() != buttonWidth || deleteButton.getHeight() != buttonHeight) {
                deleteButton = ButtonWidget.builder(buttonText, button -> {
                    // Click-Handler wird in handleButtonClick behandelt
                    // Diese Lambda wird nicht direkt aufgerufen, da wir mouseClicked verwenden
                })
                .dimensions(buttonX, buttonY, buttonWidth, buttonHeight)
                .build();
            }
            
            // Rendere ButtonWidget (verwendet automatisch die korrekte Textur)
            deleteButton.render(context, mouseX, mouseY, 0.0f);
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
    private static void renderDeleteConfirmation(DrawContext context, int mouseX, int mouseY, 
                                                  int overlayX, int overlayY, int overlayWidth, int overlayHeight, 
                                                  float scale, int unscaledHeight, int unscaledWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        int boxWidth = 250;
        int boxHeight = 100;
        int boxX = overlayX + (overlayWidth - (int)(boxWidth * scale)) / 2;
        int boxY = overlayY + ((int)(unscaledHeight * scale) - (int)(boxHeight * scale)) / 2;
        
        // Hintergrund
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);
        
        // Rahmen
        context.drawStrokedRectangle(boxX, boxY, boxWidth, boxHeight, 0xFFFFFFFF);
        
        // Text (in zwei Zeilen)
        String questionText1 = "Sicher das alle Baupläne";
        String questionText2 = "entfernt werden sollen?";
        int textY = boxY + 15;
        int text1Width = client.textRenderer.getWidth(questionText1);
        int text2Width = client.textRenderer.getWidth(questionText2);
        
        // Erste Zeile
        context.drawText(
            client.textRenderer,
            questionText1,
            boxX + (boxWidth - text1Width) / 2,
            textY,
            0xFFFFFFFF,
            true
        );
        
        // Zweite Zeile
        context.drawText(
            client.textRenderer,
            questionText2,
            boxX + (boxWidth - text2Width) / 2,
            textY + client.textRenderer.fontHeight + 2,
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
            confirmationJaButton = ButtonWidget.builder(Text.literal("Ja"), button -> {
                // Click-Handler wird in handleDeleteButtonClick behandelt
            })
            .dimensions(jaButtonXLocal, buttonY, buttonWidth, buttonHeight)
            .build();
        }
        
        // Speichere Button-Positionen für Click-Handler
        jaButtonX = jaButtonXLocal;
        jaButtonY = buttonY;
        jaButtonWidth = buttonWidth;
        jaButtonHeight = buttonHeight;
        
        // Rendere ButtonWidget
        confirmationJaButton.render(context, mouseX, mouseY, 0.0f);
        
        // "Nein" Button
        int neinButtonXLocal = jaButtonXLocal + buttonWidth + buttonSpacing;
        
        // Erstelle oder aktualisiere ButtonWidget
        if (confirmationNeinButton == null || confirmationNeinButton.getX() != neinButtonXLocal || 
            confirmationNeinButton.getY() != buttonY || confirmationNeinButton.getWidth() != buttonWidth || 
            confirmationNeinButton.getHeight() != buttonHeight) {
            confirmationNeinButton = ButtonWidget.builder(Text.literal("Nein"), button -> {
                // Click-Handler wird in handleButtonClick behandelt
                // Diese Lambda wird nicht direkt aufgerufen, da wir mouseClicked verwenden
            })
            .dimensions(neinButtonXLocal, buttonY, buttonWidth, buttonHeight)
            .build();
        }
        
        // Speichere Button-Positionen für Click-Handler
        neinButtonX = neinButtonXLocal;
        neinButtonY = buttonY;
        neinButtonWidth = buttonWidth;
        neinButtonHeight = buttonHeight;
        confirmationButtonsVisible = true;
        
        // Rendere ButtonWidget
        confirmationNeinButton.render(context, mouseX, mouseY, 0.0f);
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
     * Rendert eine Kosten-Zeile (mit Multiplikation)
     * @param context DrawContext
     * @param client MinecraftClient
     * @param costItem CostItem (kann null sein)
     * @param y Y-Position
     * @param quantity Anzahl zum Multiplizieren
     * @return true wenn eine Zeile gerendert wurde, false wenn nicht
     */
    private static boolean renderCostItem(DrawContext context, MinecraftClient client, CostItem costItem, int y, int quantity) {
        if (costItem != null && costItem.itemName != null && costItem.amount != null) {
            // Multipliziere CostItem mit Anzahl
            CostItem multipliedItem = multiplyCostItem(costItem, quantity);
            
            // Prüfe ob es Coins oder Pergamentfetzen sind (für Trennzeichen)
            boolean isCoins = "Coins".equalsIgnoreCase(multipliedItem.itemName);
            BigDecimal neededAmount = parseAmountToBigDecimal(multipliedItem.amount);

            BigDecimal ownedAmount;
            if (isCoins) {
                ownedAmount = ClipboardCoinCollector.getCurrentCoins();
            } else {
                ownedAmount = BigDecimal.valueOf(getOwnedMaterialAmount(costItem.itemName));
            }

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
            
            // Rendere den Material-Text in der entsprechenden Farbe (rot/grün)
            int materialLineWidth = client.textRenderer.getWidth(materialLine);
            int abbreviatedWidth = abbreviatedText.isEmpty() ? 0 : client.textRenderer.getWidth(abbreviatedText);
            
            context.drawText(
                client.textRenderer,
                materialLine,
                5, y,
                textColor, // Rot oder Grün basierend auf vorhandener vs. benötigter Menge
                true
            );
            
            // Rendere abgekürzte Version (grau) für Coins
            if (!abbreviatedText.isEmpty()) {
                context.drawText(
                    client.textRenderer,
                    abbreviatedText,
                    5 + materialLineWidth, y,
                    0xFF808080, // Grau für Abkürzung
                    true
                );
            }
            
            // Prüfe ob es ein Aincraft-Material ist und füge Ebene hinzu
            InformationenUtility.MaterialFloorInfo floorInfo = InformationenUtility.getMaterialFloorInfo(multipliedItem.itemName);
            
            if (floorInfo != null) {
                String floorText = InformationenUtility.formatMaterialLocationSuffix(floorInfo);
                int rarityColor = InformationenUtility.getMaterialLocationRarityColorArgb(floorInfo);
                int floorTextX = 5 + materialLineWidth + abbreviatedWidth;
                
                context.drawText(
                    client.textRenderer,
                    floorText,
                    floorTextX, y,
                    rarityColor,
                    true
                );
            }
            
            return true;
        }
        return false;
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
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(java.util.Locale.US);
        symbols.setGroupingSeparator('.');
        
        if (value == Math.floor(value)) {
            // Ganze Zahl
            DecimalFormat df = new DecimalFormat("#,###", symbols);
            return df.format((long) value);
        } else {
            DecimalFormat df = new DecimalFormat("#,###.##", symbols);
            df.setDecimalSeparatorAlwaysShown(false);
            return df.format(value);
        }
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
     * Gibt die vorhandene Menge eines Materials zurück
     * Für Pergamentfetzen wird der ClipboardPaperShredsCollector verwendet,
     * für Materialien/Ressourcen werden die persistierten Dateien genutzt.
     */
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
        // Prüfe ob es Pergamentfetzen sind
        if ("Pergamentfetzen".equalsIgnoreCase(materialName)) {
            return ClipboardPaperShredsCollector.getCurrentPaperShreds();
        }
        if (materialName == null) {
            return 0.0;
        }
        
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
    
    /**
     * Sammelt und addiert alle CostItems aus allen Bauplänen
     * @param forBlueprintShop true für Bauplan-Shop-Kosten, false für normale Bauplan-Kosten
     * @return Liste von CostItems sortiert nach Kategorien (Coins, Materialien, Amboss, Ressource)
     */
    private static List<CostItem> calculateTotalCosts(boolean forBlueprintShop) {
        // Verwende Map für Addition, dann sortiere nach Kategorien
        Map<String, CostItem> totalCostsMap = new HashMap<>();
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            // Hole Anzahl für diesen Bauplan (mit Fallback auf 1)
            int quantity = entry.quantity > 0 ? entry.quantity : 1;
            
            if (forBlueprintShop) {
                // Bauplan-Shop-Kosten
                if (entry.blueprintShop != null && entry.blueprintShop.price != null) {
                    addCostItemToTotal(totalCostsMap, multiplyCostItem(entry.blueprintShop.price.coin, quantity));
                    addCostItemToTotal(totalCostsMap, multiplyCostItem(entry.blueprintShop.price.paper_shreds, quantity));
                }
            } else {
                // Normale Kosten (Baupläne, Module, Lizenzen, etc.)
                if (entry.price != null) {
                    addPriceCostItemsToTotal(totalCostsMap, entry.price, quantity);
                }
            }
        }
        
        // Sortiere nach Kategorien: Coins, Materialien, Amboss, Ressource
        List<CostItem> result = new ArrayList<>();
        
        // 1. Coins (case-insensitive)
        for (Map.Entry<String, CostItem> entry : totalCostsMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("Coins")) {
                result.add(entry.getValue());
                break;
            }
        }
        
        // 2. Materialien (alle außer Coins, Amboss, Ressource)
        List<CostItem> materials = new ArrayList<>();
        for (Map.Entry<String, CostItem> entry : totalCostsMap.entrySet()) {
            String itemName = entry.getKey();
            if (!itemName.equalsIgnoreCase("Coins") && 
                !isAmbossOrRessource(itemName, totalCostsMap)) {
                materials.add(entry.getValue());
            }
        }
        // Sortiere Materialien
        boolean materialSortEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaterialSortEnabled;
        if (materialSortEnabled) {
            // Sortiere nach Ebenen
            boolean ascending = CCLiveUtilitiesConfig.HANDLER.instance().clipboardMaterialSortAscending;
            materials.sort((item1, item2) -> {
                InformationenUtility.MaterialFloorInfo info1 = InformationenUtility.getMaterialFloorInfo(item1.itemName);
                InformationenUtility.MaterialFloorInfo info2 = InformationenUtility.getMaterialFloorInfo(item2.itemName);
                
                int floor1 = (info1 != null) ? info1.floor : Integer.MAX_VALUE; // Materialien ohne Ebene ans Ende
                int floor2 = (info2 != null) ? info2.floor : Integer.MAX_VALUE;
                
                if (ascending) {
                    // Aufsteigend: 1-100
                    int floorCompare = Integer.compare(floor1, floor2);
                    if (floorCompare != 0) {
                        return floorCompare;
                    }
                } else {
                    // Absteigend: 100-1
                    int floorCompare = Integer.compare(floor2, floor1);
                    if (floorCompare != 0) {
                        return floorCompare;
                    }
                }
                // Bei gleicher Ebene: alphabetisch sortieren
                return item1.itemName.compareTo(item2.itemName);
            });
        } else {
            // Sortiere Materialien alphabetisch (wie geadded wurden - aktuelle Reihenfolge)
            materials.sort(Comparator.comparing(item -> item.itemName));
        }
        
        // 3. Amboss (alle Amboss-Items)
        List<CostItem> ambossItems = new ArrayList<>();
        for (Map.Entry<String, CostItem> entry : totalCostsMap.entrySet()) {
            CostItem item = entry.getValue();
            if (item != null && isAmbossItem(item.itemName, totalCostsMap)) {
                ambossItems.add(item);
            }
        }
        // Sortiere Amboss-Items alphabetisch
        ambossItems.sort(Comparator.comparing(item -> item.itemName));
        
        // 4. Ressource (alle Ressource-Items)
        List<CostItem> ressourceItems = new ArrayList<>();
        for (Map.Entry<String, CostItem> entry : totalCostsMap.entrySet()) {
            CostItem item = entry.getValue();
            if (item != null && isRessourceItem(item.itemName, totalCostsMap)) {
                ressourceItems.add(item);
            }
        }
        // Sortiere Ressource-Items alphabetisch
        ressourceItems.sort(Comparator.comparing(item -> item.itemName));
        
        // Kostenanzeige-Filterung/Sortierung (nur für normale Kosten, nicht für Bauplan Shop Kosten)
        if (!forBlueprintShop) {
            boolean costDisplayEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayEnabled;
            if (costDisplayEnabled) {
                int costDisplayMode = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayMode;
                // Hole quantity für Gesamtliste (Seite 1)
                int quantity = getQuantityForPage(1, null);
                
                if (costDisplayMode == 1) {
                    // Mode 1: Ausblenden fertiger Items
                    materials.removeIf(item -> isCostItemComplete(item, quantity));
                    ambossItems.removeIf(item -> isCostItemComplete(item, quantity));
                    ressourceItems.removeIf(item -> isCostItemComplete(item, quantity));
                    // Coins werden nicht gefiltert (immer sichtbar)
                    
                    // Füge alle nicht-fertigen Items hinzu (in Kategorien-Reihenfolge)
                    result.addAll(materials);
                    result.addAll(ambossItems);
                    result.addAll(ressourceItems);
                } else if (costDisplayMode == 2) {
                    // Mode 2: Fertige Items ans Ende ALLER Kategorien setzen
                    // Trenne nicht-fertige und fertige Items für jede Kategorie
                    List<CostItem> materialsNotComplete = new ArrayList<>();
                    List<CostItem> materialsComplete = new ArrayList<>();
                    for (CostItem item : materials) {
                        if (isCostItemComplete(item, quantity)) {
                            materialsComplete.add(item);
                        } else {
                            materialsNotComplete.add(item);
                        }
                    }
                    
                    List<CostItem> ambossNotComplete = new ArrayList<>();
                    List<CostItem> ambossComplete = new ArrayList<>();
                    for (CostItem item : ambossItems) {
                        if (isCostItemComplete(item, quantity)) {
                            ambossComplete.add(item);
                        } else {
                            ambossNotComplete.add(item);
                        }
                    }
                    
                    List<CostItem> ressourceNotComplete = new ArrayList<>();
                    List<CostItem> ressourceComplete = new ArrayList<>();
                    for (CostItem item : ressourceItems) {
                        if (isCostItemComplete(item, quantity)) {
                            ressourceComplete.add(item);
                        } else {
                            ressourceNotComplete.add(item);
                        }
                    }
                    
                    // Füge zuerst alle nicht-fertigen Items hinzu (in Kategorien-Reihenfolge)
                    result.addAll(materialsNotComplete);
                    result.addAll(ambossNotComplete);
                    result.addAll(ressourceNotComplete);
                    
                    // Dann alle fertigen Items (in Kategorien-Reihenfolge)
                    result.addAll(materialsComplete);
                    result.addAll(ambossComplete);
                    result.addAll(ressourceComplete);
                } else {
                    // Kein Filter: Füge alle Items hinzu (in Kategorien-Reihenfolge)
                    result.addAll(materials);
                    result.addAll(ambossItems);
                    result.addAll(ressourceItems);
                }
            } else {
                // Kostenanzeige deaktiviert: Füge alle Items hinzu (in Kategorien-Reihenfolge)
                result.addAll(materials);
                result.addAll(ambossItems);
                result.addAll(ressourceItems);
            }
        } else {
            // Bauplan Shop Kosten: Füge alle Items hinzu (in Kategorien-Reihenfolge)
            result.addAll(materials);
            result.addAll(ambossItems);
            result.addAll(ressourceItems);
        }
        
        return result;
    }
    
    /**
     * Prüft ob ein Item ein Amboss-Item ist (basierend auf dem ursprünglichen entry.price.Amboss)
     */
    private static boolean isAmbossItem(String itemName, Map<String, CostItem> allCosts) {
        // Prüfe alle Einträge, ob dieses Item als Amboss verwendet wurde
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (entry.price != null && entry.price.Amboss != null && 
                entry.price.Amboss.itemName != null && entry.price.Amboss.itemName.equals(itemName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Prüft ob ein Item ein Ressource-Item ist (basierend auf dem ursprünglichen entry.price.Ressource)
     */
    private static boolean isRessourceItem(String itemName, Map<String, CostItem> allCosts) {
        // Prüfe alle Einträge, ob dieses Item als Ressource verwendet wurde
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (entry.price != null && entry.price.Ressource != null && 
                entry.price.Ressource.itemName != null && entry.price.Ressource.itemName.equals(itemName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Prüft ob ein Item ein Amboss- oder Ressource-Item ist
     */
    private static boolean isAmbossOrRessource(String itemName, Map<String, CostItem> allCosts) {
        return isAmbossItem(itemName, allCosts) || isRessourceItem(itemName, allCosts);
    }
    
    /**
     * Fügt ein CostItem zu den Gesamtkosten hinzu (addiert bei gleichem itemName)
     */
    private static void addCostItemToTotal(Map<String, CostItem> totalCosts, CostItem costItem) {
        if (costItem == null || costItem.itemName == null || costItem.amount == null) {
            return;
        }
        
        String itemName = costItem.itemName;
        
        // Verwende case-insensitive Vergleich für Coins
        String keyToUse = itemName;
        if (itemName.equalsIgnoreCase("Coins")) {
            // Normalisiere zu "Coins" für konsistente Keys
            keyToUse = "Coins";
        }
        
        if (totalCosts.containsKey(keyToUse)) {
            // Bereits vorhanden: addiere Amount
            CostItem existing = totalCosts.get(keyToUse);
            existing.amount = addAmounts(existing.amount, costItem.amount);
        } else {
            // Neu: erstelle Kopie
            CostItem newItem = new CostItem();
            newItem.itemName = keyToUse; // Verwende normalisierten Namen
            newItem.amount = costItem.amount;
            totalCosts.put(keyToUse, newItem);
        }
    }
    
    @Override
    public void savePosition() {
        // Position is already saved in setPosition()
    }
    
    @Override
    public boolean isEnabled() {
        // Deaktiviere in der general_lobby Dimension
        try {
            var client = net.minecraft.client.MinecraftClient.getInstance();
            if (client != null && client.world != null) {
                String dimensionId = client.world.getRegistryKey().getValue().toString();
                if ("minecraft:general_lobby".equals(dimensionId)) {
                    return false;
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
        
        return CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled && 
               CCLiveUtilitiesConfig.HANDLER.instance().showClipboard;
    }
    
    @Override
    public Text getTooltip() {
        return Text.literal("Clipboard - Zeigt Bauplan-Kosten an");
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
     * @param countText Text für [Anzahl]
     * @param unscaledWidth Unskalierte Breite des Overlays
     * @return Array mit [leftButtonX, rightButtonX, spaceAfterLeftX, separatorX, spaceAfterSeparatorX, spaceAfterRightX, countX]
     */
    private static int[] calculateButtonPositions(MinecraftClient client, String blueprintName, String countText, int unscaledWidth) {
        int nameWidth = client.textRenderer.getWidth(blueprintName);
        
        String leftArrow = "«";
        String rightArrow = "»";
        String separator = "|";
        String space = " ";
        int leftArrowWidth = client.textRenderer.getWidth(leftArrow);
        int rightArrowWidth = client.textRenderer.getWidth(rightArrow);
        int separatorWidth = client.textRenderer.getWidth(separator);
        int spaceWidth = client.textRenderer.getWidth(space);
        int totalButtonWidth = leftArrowWidth + spaceWidth + separatorWidth + spaceWidth + rightArrowWidth;
        
        // Berücksichtige Textfeld-Breite + "x"-Präfix wenn in Inventar
        boolean isInInventory = client.currentScreen instanceof HandledScreen;
        int quantityAreaWidth = getQuantityAreaWidth(client, countText, isInInventory);
        int quantityAreaX = unscaledWidth - quantityAreaWidth - HEADER_COUNT_PADDING;
        
        int countX = quantityAreaX;
        int nameX = HEADER_NAME_PADDING;
        int buttonSpacing = HEADER_BUTTON_GAP;
        
        int buttonsX = quantityAreaX - QUANTITY_AFTER_BUTTONS_SPACING - totalButtonWidth;
        
        int nameEndX = nameX + nameWidth;
        int buttonsAfterNameX = nameEndX + buttonSpacing;
        
        int leftButtonX, rightButtonX, spaceAfterLeftX, separatorX, spaceAfterSeparatorX, spaceAfterRightX;
        
        if (buttonsAfterNameX + totalButtonWidth <= buttonsX) {
            leftButtonX = buttonsX;
            spaceAfterLeftX = leftButtonX + leftArrowWidth;
            separatorX = spaceAfterLeftX + spaceWidth;
            spaceAfterSeparatorX = separatorX + separatorWidth;
            spaceAfterRightX = spaceAfterSeparatorX + spaceWidth;
            rightButtonX = spaceAfterRightX;
        } else {
            leftButtonX = nameEndX + buttonSpacing;
            spaceAfterLeftX = leftButtonX + leftArrowWidth;
            separatorX = spaceAfterLeftX + spaceWidth;
            spaceAfterSeparatorX = separatorX + separatorWidth;
            spaceAfterRightX = spaceAfterSeparatorX + spaceWidth;
            rightButtonX = spaceAfterRightX;
            countX = rightButtonX + rightArrowWidth + QUANTITY_AFTER_BUTTONS_SPACING;
        }
        
        return new int[]{leftButtonX, rightButtonX, spaceAfterLeftX, separatorX, spaceAfterSeparatorX, spaceAfterRightX, countX};
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
    private static int getHoveredButton(int mouseX, int mouseY, int buttonY, int leftButtonX, int rightButtonX, MinecraftClient client) {
        int lineHeight = client.textRenderer.fontHeight + 2;
        if (mouseY < buttonY || mouseY > buttonY + lineHeight) {
            return 0; // Nicht über der Zeile
        }
        
        String leftArrow = "«";
        String rightArrow = "»";
        int leftArrowWidth = client.textRenderer.getWidth(leftArrow);
        int rightArrowWidth = client.textRenderer.getWidth(rightArrow);
        
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
    public static boolean handleButtonClick(int mouseX, int mouseY) {
        if (!CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled || 
            !CCLiveUtilitiesConfig.HANDLER.instance().showClipboard) {
            return false;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        
        // Nur in Inventaren funktionieren
        if (!(client.currentScreen instanceof HandledScreen)) {
            return false;
        }
        
        // Prüfe zuerst, ob Bestätigungs-Overlay offen ist - dann prüfe Ja/Nein Buttons
        if (showDeleteConfirmation) {
            if (confirmationJaButton != null && confirmationJaButton.mouseClicked(new net.minecraft.client.gui.Click(mouseX, mouseY, new net.minecraft.client.input.MouseInput(0, 1)), false)) {
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
            
            if (confirmationNeinButton != null && confirmationNeinButton.mouseClicked(new net.minecraft.client.gui.Click(mouseX, mouseY, new net.minecraft.client.input.MouseInput(0, 1)), false)) {
                // Bestätigung abbrechen
                showDeleteConfirmation = false;
                confirmationButtonsVisible = false;
                return true;
            }
            
            // Klick war nicht auf Ja/Nein Buttons
            return false;
        }
        
        // Bestätigungs-Overlay ist nicht offen - prüfe auf Haupt-Delete-Button
        if (deleteButton != null && deleteButton.mouseClicked(new net.minecraft.client.gui.Click(mouseX, mouseY, new net.minecraft.client.input.MouseInput(0, 1)), false)) {
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
        
        int x = CCLiveUtilitiesConfig.HANDLER.instance().clipboardX;
        int y = CCLiveUtilitiesConfig.HANDLER.instance().clipboardY;
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) scale = 1.0f;
        
        // Konvertiere skalierten Maus-Koordinaten zu unskalierten
        int unscaledMouseX = (int) ((mouseX - x) / scale);
        int unscaledMouseY = (int) ((mouseY - y) / scale);
        
        int padding = 5;
        int currentY = padding;
        
        // Y-Position der Buttons (bei "Bauplanname [Anzahl]" Zeile)
        // Muss exakt mit renderInGame übereinstimmen!
        int buttonY = currentY;
        
        // Berechne Button-Positionen (MUSS exakt mit renderInGame übereinstimmen!)
        // Hole aktuelle Seite und Daten (wie in renderInGame)
        int currentPage = ClipboardUtility.getCurrentPage();
        ClipboardUtility.ClipboardEntry entry = ClipboardUtility.getEntryForPage(currentPage);
        
        String countText;
        if (currentPage == 1) {
            // Seite 1: Gesamtliste
            // Hole aktuelle Anzahl für Seite 1 (aus Textfeld oder Standard 1)
            int quantityForPage1 = getQuantityForPage(1, null);
            countText = formatQuantityText(quantityForPage1);
        } else if (entry != null) {
            // Seite 2+: Einzelner Bauplan
            countText = formatQuantityText(entry.quantity > 0 ? entry.quantity : 1);
        } else {
            // Fallback
            countText = formatQuantityText(1);
        }
        
        // Hole Bauplan-Name für Button-Positionierung
        String blueprintName;
        if (currentPage == 1) {
            blueprintName = "Gesamtliste";
        } else if (entry != null) {
            blueprintName = entry.blueprintName != null ? entry.blueprintName : "Bauplan Name";
        } else {
            blueprintName = "Bauplan Name";
        }
        
        int unscaledWidth = calculateUnscaledWidth();
        
        // Berechne Button-Positionen mit Hilfsmethode (exakt wie in renderInGame)
        int[] buttonPositions = calculateButtonPositions(client, blueprintName, countText, unscaledWidth);
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
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        
        // Nur in Inventaren funktionieren
        if (!(client.currentScreen instanceof HandledScreen)) {
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
    public static Text getButtonTooltip(int button) {
        int currentPage = ClipboardUtility.getCurrentPage();
        int totalPages = ClipboardUtility.getTotalPages();
        
        if (button == 1) {
            // Linker Button: Vorherige Seite
            if (currentPage > 1) {
                return Text.literal("Seite " + (currentPage - 1));
            } else {
                return Text.literal("Keine vorherige Seite");
            }
        } else if (button == 2) {
            // Rechter Button: Nächste Seite
            if (currentPage < totalPages) {
                return Text.literal("Seite " + (currentPage + 1));
            } else {
                return Text.literal("Keine nächste Seite");
            }
        }
        
        return Text.empty();
    }
    
    /**
     * Rendert Tooltips für die Pfeil-Buttons
     * @param context DrawContext
     * @param mouseX Maus-X-Position (skaliert)
     * @param mouseY Maus-Y-Position (skaliert)
     */
    public static void renderButtonTooltips(DrawContext context, int mouseX, int mouseY) {
        if (!CCLiveUtilitiesConfig.HANDLER.instance().clipboardEnabled || 
            !CCLiveUtilitiesConfig.HANDLER.instance().showClipboard) {
            return;
        }
        
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        // Hide tooltips if F1 menu (debug screen) is open
        if (client.options.hudHidden) {
            return;
        }
        
        // Hide tooltips if Tab list is open (like other overlays)
        if (KeyBindingUtility.isPlayerListKeyPressed()) {
            return;
        }
        
        if (hideHover) {
            return;
        }
        
        int x = CCLiveUtilitiesConfig.HANDLER.instance().clipboardX;
        int y = CCLiveUtilitiesConfig.HANDLER.instance().clipboardY;
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) scale = 1.0f;
        
        // Konvertiere skalierten Maus-Koordinaten zu unskalierten
        int unscaledMouseX = (int) ((mouseX - x) / scale);
        int unscaledMouseY = (int) ((mouseY - y) / scale);
        
        int padding = 5;
        int currentY = padding;
        
        // Y-Position der Buttons
        int buttonY = currentY;
        
        // Hole aktuelle Seite und Daten für Button-Positionierung
        int currentPage = ClipboardUtility.getCurrentPage();
        ClipboardUtility.ClipboardEntry entry = ClipboardUtility.getEntryForPage(currentPage);
        
        String blueprintName;
        String countText;
        if (currentPage == 1) {
            blueprintName = "Gesamtliste";
            // Hole aktuelle Anzahl für Seite 1 (aus Textfeld oder Standard 1)
            int quantityForPage1 = getQuantityForPage(1, null);
            countText = formatQuantityText(quantityForPage1);
        } else if (entry != null) {
            blueprintName = entry.blueprintName != null ? entry.blueprintName : "Bauplan Name";
            countText = formatQuantityText(entry.quantity > 0 ? entry.quantity : 1);
        } else {
            blueprintName = "Bauplan Name";
            countText = formatQuantityText(1);
        }
        
        int unscaledWidth = resolveRenderWidth(client, currentPage, entry);
        
        // Berechne Button-Positionen mit Hilfsmethode (exakt wie in renderInGame)
        int[] buttonPositions = calculateButtonPositions(client, blueprintName, countText, unscaledWidth);
        int leftButtonX = buttonPositions[0];
        int rightButtonX = buttonPositions[1];
        
        // Prüfe welcher Button gehovered ist
        int hoveredButton = getHoveredButton(unscaledMouseX, unscaledMouseY, buttonY, leftButtonX, rightButtonX, client);
        
        if (hoveredButton > 0) {
            Text tooltip = getButtonTooltip(hoveredButton);
            if (tooltip != null && !tooltip.getString().isEmpty()) {
                // Konvertiere Button-Position zurück zu skalierten Koordinaten für Tooltip
                int tooltipX = (int) (x + (hoveredButton == 1 ? leftButtonX : rightButtonX) * scale);
                int tooltipY = (int) (y + buttonY * scale);
                context.drawTooltip(client.textRenderer, java.util.List.of(tooltip), tooltipX, tooltipY);
            }
            return;
        }
        
        // Anzahl-Tooltip: verwende gerenderte Hover-Bounds (exakt synchron mit renderInGame)
        if (quantityHoverBoundsValid
            && unscaledMouseX >= quantityHoverX && unscaledMouseX <= quantityHoverX + quantityHoverWidth
            && unscaledMouseY >= quantityHoverY && unscaledMouseY <= quantityHoverY + quantityHoverHeight) {
            int tooltipX = (int) (x + quantityHoverX * scale);
            int tooltipY = (int) (y + quantityHoverY * scale);
            context.drawTooltip(client.textRenderer, java.util.List.of(Text.literal("Anzahl")), tooltipX, tooltipY);
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
        int quantity = parseQuantityText(quantityTextField.getText());
        quantityTextField.setText(String.valueOf(quantity));
        applyQuantityForPage(page, quantity);
        
        quantityTextField.setFocused(false);
        resetQuantityTextField();
        invalidateWidthCache();
    }
    
    private static void applyQuantityForPage(int page, int quantity) {
        if (page == 1) {
            page1Quantity = quantity;
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
        
        String text = quantityTextField.getText();
        if (text == null || text.trim().isEmpty()) {
            quantityTextField.setText("1");
            applyQuantityForPage(quantityTextFieldPage, 1);
            invalidateWidthCache();
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
    
    private static int getQuantityPrefixWidth(MinecraftClient client) {
        return client.textRenderer.getWidth(QUANTITY_PREFIX);
    }
    
    private static int getQuantityPrefixAreaWidth(MinecraftClient client) {
        return QUANTITY_FIELD_PADDING + getQuantityPrefixWidth(client);
    }
    
    private static int getQuantityInputWidth(MinecraftClient client) {
        return getQuantityPrefixAreaWidth(client) + QUANTITY_TEXT_FIELD_WIDTH;
    }
    
    private static int getQuantityAreaWidth(MinecraftClient client, String countText, boolean isInInventory) {
        if (isInInventory) {
            return getQuantityInputWidth(client) + QUANTITY_FIELD_OUTLINE_PADDING;
        }
        return client.textRenderer.getWidth(countText);
    }
    
    private static int getNavigationButtonsWidth(MinecraftClient client) {
        return client.textRenderer.getWidth("«")
            + client.textRenderer.getWidth(" ")
            + client.textRenderer.getWidth("|")
            + client.textRenderer.getWidth(" ")
            + client.textRenderer.getWidth("»");
    }
    
    private static int computeHeaderLineWidth(MinecraftClient client, String blueprintName, String countText) {
        boolean isInInventory = client.currentScreen instanceof HandledScreen;
        int quantityAreaWidth = getQuantityAreaWidth(client, countText, isInInventory);
        int totalButtonWidth = getNavigationButtonsWidth(client);
        int nameEndX = HEADER_NAME_PADDING + client.textRenderer.getWidth(blueprintName);
        
        int flowLayoutWidth = nameEndX + HEADER_BUTTON_GAP + totalButtonWidth
            + QUANTITY_AFTER_BUTTONS_SPACING + quantityAreaWidth + HEADER_COUNT_PADDING;
        
        int rightAlignedMinWidth = nameEndX + HEADER_BUTTON_GAP + QUANTITY_AFTER_BUTTONS_SPACING + totalButtonWidth
            + QUANTITY_AFTER_BUTTONS_SPACING + quantityAreaWidth + HEADER_COUNT_PADDING;
        
        return Math.max(flowLayoutWidth, rightAlignedMinWidth);
    }
    
    private static int resolveRenderWidth(MinecraftClient client, int currentPage, ClipboardUtility.ClipboardEntry entry) {
        int width = calculateUnscaledWidth();
        String blueprintName;
        String countText;
        
        if (currentPage == 1) {
            blueprintName = "Gesamtliste";
            countText = formatQuantityText(getQuantityForPage(1, null));
        } else if (entry != null) {
            blueprintName = entry.blueprintName != null ? entry.blueprintName : "Bauplan Name";
            countText = formatQuantityText(entry.quantity > 0 ? entry.quantity : 1);
        } else {
            blueprintName = "Bauplan Name";
            countText = formatQuantityText(1);
        }
        
        width = Math.max(width, computeHeaderLineWidth(client, blueprintName, countText));
        
        int quantity = getQuantityForPage(currentPage, entry);
        if (currentPage == 1) {
            for (CostItem costItem : calculateTotalCosts(false)) {
                width = Math.max(width, measureCostLineWidth(client, costItem, quantity));
            }
            if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts) {
                for (CostItem costItem : calculateTotalCosts(true)) {
                    width = Math.max(width, measureCostLineWidth(client, costItem, quantity));
                }
            }
        } else if (entry != null && entry.price != null) {
            CostItem[] costItems = {
                entry.price.coin, entry.price.material1, entry.price.material2,
                entry.price.Amboss, entry.price.Ressource
            };
            for (CostItem costItem : costItems) {
                width = Math.max(width, measureCostLineWidth(client, costItem, quantity));
            }
            if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts
                && entry.blueprintShop != null && entry.blueprintShop.price != null) {
                width = Math.max(width, measureCostLineWidth(client, entry.blueprintShop.price.coin, quantity));
                width = Math.max(width, measureCostLineWidth(client, entry.blueprintShop.price.paper_shreds, quantity));
            }
        }
        
        return width;
    }
    
    private static int measureCostLineWidth(MinecraftClient client, CostItem costItem, int quantity) {
        if (costItem == null || costItem.itemName == null || costItem.amount == null) {
            return 0;
        }
        
        CostItem multipliedItem = multiplyCostItem(costItem, quantity);
        boolean isCoins = "Coins".equalsIgnoreCase(multipliedItem.itemName);

        BigDecimal ownedAmount = isCoins
                ? ClipboardCoinCollector.getCurrentCoins()
                : BigDecimal.valueOf(getOwnedMaterialAmount(costItem.itemName));

        String ownedAmountStr = formatAmount(ownedAmount, isCoins);
        String neededAmountStr = formatAmount(multipliedItem.amount, isCoins);
        String materialLine = ownedAmountStr + " / " + neededAmountStr + " " + costItem.itemName;

        String abbreviatedText = "";
        if (isCoins) {
            BigDecimal coinAmount = parseAmountToBigDecimal(multipliedItem.amount);
            if (coinAmount.compareTo(BigDecimal.valueOf(1000)) >= 0) {
                abbreviatedText = " (" + formatNumberAbbreviated(coinAmount) + ")";
            }
        }
        
        InformationenUtility.MaterialFloorInfo floorInfo =
            InformationenUtility.getMaterialFloorInfo(multipliedItem.itemName);
        String floorText = InformationenUtility.formatMaterialLocationSuffix(floorInfo);
        
        return client.textRenderer.getWidth(materialLine)
            + client.textRenderer.getWidth(abbreviatedText)
            + client.textRenderer.getWidth(floorText)
            + COST_LINE_HORIZONTAL_PADDING;
    }
    
    private static void renderQuantityFieldBackground(DrawContext context, int x, int y, int width, int height, boolean focused) {
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
        // Wenn Textfeld existiert und für diese Seite aktiv ist, verwende den Wert aus dem Textfeld
        if (quantityTextField != null && quantityTextFieldPage == page) {
            String text = quantityTextField.getText();
            if (text == null || text.trim().isEmpty()) {
                return 1;
            }
            try {
                int quantity = Integer.parseInt(text.trim());
                if (quantity > 0) {
                    if (page == 1) {
                        page1Quantity = quantity;
                    } else if (entry != null) {
                        entry.quantity = quantity;
                        ClipboardUtility.saveClipboardEntries();
                    }
                    return quantity;
                }
            } catch (NumberFormatException e) {
                // Ungültige Eingabe, verwende Fallback
            }
        }
        
        if (page == 1) {
            // Gesamtliste: Verwende gespeicherte Anzahl, wenn Textfeld nicht aktiv
            return page1Quantity;
        }
        
        if (entry == null) {
            return 1;
        }
        
        // Fallback: Verwende entry.quantity oder 1
        return entry.quantity > 0 ? entry.quantity : 1;
    }
    
    /**
     * Rendert das Textfeld für die [Anzahl] Eingabe
     */
    private static void renderQuantityTextField(DrawContext context, MinecraftClient client, 
                                                 int quantityAreaX, int countY, int page, int currentQuantity,
                                                 int unscaledWidth, float scale) {
        // Erstelle oder aktualisiere Textfeld wenn nötig
        if (quantityTextField != null && quantityTextFieldPage != page) {
            finalizeQuantityTextField();
        }
        if (quantityTextField == null || quantityTextFieldPage != page) {
            quantityTextField = new TextFieldWidget(
                client.textRenderer,
                0, 0, // Wird später gesetzt
                QUANTITY_TEXT_FIELD_WIDTH, // Breite
                client.textRenderer.fontHeight + 2, // Höhe
                Text.literal("")
            );
            quantityTextField.setMaxLength(10); // Maximal 10 Ziffern
            quantityTextField.setTextPredicate(s -> s.matches("\\d*")); // Nur Zahlen erlauben
            quantityTextField.setDrawsBackground(false);
            quantityTextField.setEditableColor(0xFFFFFF00);
            quantityTextField.setChangedListener(text -> {
                // Validierung: Stelle sicher, dass es eine positive Zahl ist
                if (text != null && !text.trim().isEmpty()) {
                    try {
                        int value = Integer.parseInt(text.trim());
                        if (value <= 0) {
                            quantityTextField.setText("1");
                            // Aktualisiere gespeicherte Anzahl für Seite 1
                            if (quantityTextFieldPage == 1) {
                                page1Quantity = 1;
                            }
                        } else {
                            // Aktualisiere gespeicherte Anzahl für Seite 1
                            if (quantityTextFieldPage == 1) {
                                page1Quantity = value;
                            }
                        }
                    } catch (NumberFormatException e) {
                        quantityTextField.setText("1");
                        if (quantityTextFieldPage == 1) {
                            page1Quantity = 1;
                        }
                    }
                }
                invalidateWidthCache();
            });
            quantityTextFieldPage = page;
            
            // Setze initialen Wert
            ClipboardUtility.ClipboardEntry entry = ClipboardUtility.getEntryForPage(page);
            if (entry != null) {
                quantityTextField.setText(String.valueOf(entry.quantity > 0 ? entry.quantity : 1));
            } else if (page == 1) {
                // Für Seite 1: Verwende gespeicherte Anzahl
                quantityTextField.setText(String.valueOf(page1Quantity));
            } else {
                quantityTextField.setText("1");
            }
        }
        
        normalizeQuantityTextFieldIfNeeded();
        
        // Berechne Position (unskaliert, relativ zum Overlay)
        // Textfeld soll rechts von den Buttons stehen, nicht überlappen
        // FESTE Position: rechts vom rechten Button, damit es sich nicht verschiebt
        int fieldHeight = client.textRenderer.fontHeight + 2;
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
        
        context.drawText(
            client.textRenderer,
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
        int unscaledMouseX = (int) ((client.mouse.getX() - CCLiveUtilitiesConfig.HANDLER.instance().clipboardX) / scale);
        int unscaledMouseY = (int) ((client.mouse.getY() - CCLiveUtilitiesConfig.HANDLER.instance().clipboardY) / scale);
        quantityTextField.render(context, unscaledMouseX, unscaledMouseY, 0);
        
        // Aktualisiere entry.quantity wenn Textfeld geändert wurde
        String text = quantityTextField.getText();
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
        if (quantityTextField == null) {
            return false;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !(client.currentScreen instanceof HandledScreen)) {
            return false;
        }
        
        // Hole Overlay-Position und Skalierung
        int overlayX = CCLiveUtilitiesConfig.HANDLER.instance().clipboardX;
        int overlayY = CCLiveUtilitiesConfig.HANDLER.instance().clipboardY;
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
        int outerFieldHeight = client.textRenderer.fontHeight + 2;
        int fieldY = textFieldY - (outerFieldHeight - innerFieldHeight) / 2;
        
        if (unscaledMouseX >= quantityAreaX && unscaledMouseX <= quantityAreaX + totalWidth
            && unscaledMouseY >= fieldY && unscaledMouseY <= fieldY + outerFieldHeight) {
            if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                quantityTextField.setFocused(true);
                quantityTextField.setText("");
                return true;
            }
            
            quantityTextField.setFocused(true);
            int clickX = Math.max(unscaledMouseX, textFieldX);
            quantityTextField.mouseClicked(new net.minecraft.client.gui.Click(clickX, unscaledMouseY, new net.minecraft.client.input.MouseInput(button, 1)), false);
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
            if (quantityTextField.keyPressed(new net.minecraft.client.input.KeyInput(keyCode, scanCode, modifiers))) {
                return true;
            }
            
            // Für Zeicheneingaben: Konvertiere KeyCode zu Zeichen (Zahlen 0-9 und Nummernfeld)
            char digit = 0;
            if (keyCode >= org.lwjgl.glfw.GLFW.GLFW_KEY_0 && keyCode <= org.lwjgl.glfw.GLFW.GLFW_KEY_9) {
                // Haupttastatur 0-9
                digit = (char) ('0' + (keyCode - org.lwjgl.glfw.GLFW.GLFW_KEY_0));
            } else {
                // Nummernfeld 0-9 - verwende explizite KeyCodes
                switch (keyCode) {
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_0:
                        digit = '0';
                        break;
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_1:
                        digit = '1';
                        break;
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_2:
                        digit = '2';
                        break;
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_3:
                        digit = '3';
                        break;
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_4:
                        digit = '4';
                        break;
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_5:
                        digit = '5';
                        break;
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_6:
                        digit = '6';
                        break;
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_7:
                        digit = '7';
                        break;
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_8:
                        digit = '8';
                        break;
                    case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_9:
                        digit = '9';
                        break;
                }
            }
            
            if (digit != 0) {
                return quantityTextField.charTyped(new net.minecraft.client.input.CharInput((int)digit, modifiers));
            }
        }
        return false;
    }
    
    /**
     * Behandelt Zeicheneingaben für das [Anzahl] Textfeld (wird über keyPressed aufgerufen)
     */
    public static boolean handleQuantityTextFieldCharTyped(char chr, int modifiers) {
        if (quantityTextField != null && quantityTextField.isFocused()) {
            return quantityTextField.charTyped(new net.minecraft.client.input.CharInput((int)chr, modifiers));
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        // Prüfe ob die Spielerliste-Taste gedrückt ist (respektiert benutzerdefinierte Key Bindings)
        boolean playerListKeyPressed = KeyBindingUtility.isPlayerListKeyPressed();

        // Prüfe ob F1 aktiv ist (wie alle anderen GUIs - über hudHidden)
        boolean f1Active = client.options.hudHidden;

        // Wenn Spielerliste-Taste gedrückt ist oder F1 aktiv ist, dann ausblenden
        if (playerListKeyPressed || f1Active) {
            hideHover = true;
        } else {
            // Weder Spielerliste-Taste noch F1 aktiv - zeige Overlay sofort wieder an (ohne Delay)
            hideHover = false;
        }
    }
    
    /**
     * Schätzt die Anzahl der Zeilen für das Overlay (ohne zu rendern)
     * Wird verwendet, um die Background-Höhe vor dem Rendering zu berechnen
     */
    private static int calculateEstimatedLineCount() {
        int currentPage = ClipboardUtility.getCurrentPage();
        ClipboardUtility.ClipboardEntry entry = ClipboardUtility.getEntryForPage(currentPage);
        
        int lineCount = 1; // Bauplan-Name Zeile
        
        if (currentPage == 1) {
            // Seite 1: Gesamtliste
            List<CostItem> totalCosts = calculateTotalCosts(false);
            if (!totalCosts.isEmpty()) {
                lineCount += 1; // "Kosten:" Zeile
                lineCount += totalCosts.size(); // Kosten-Items
                
                // Optional: Bauplan Shop Kosten
                if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts) {
                    List<CostItem> totalShopCosts = calculateTotalCosts(true);
                    if (!totalShopCosts.isEmpty()) {
                        lineCount += 1; // "Bauplan Shop Kosten:" Zeile
                        lineCount += totalShopCosts.size(); // Shop-Kosten-Items
                    }
                }
            } else {
                lineCount += 2; // "Kosten:" + "Keine Baupläne im Clipboard"
            }
        } else if (entry != null && entry.price != null) {
            // Seite 2+: Einzelner Eintrag
            lineCount += 1; // "Kosten:" Zeile
            int quantity = getQuantityForPage(currentPage, entry);
            lineCount += countVisiblePriceCostLines(entry.price, quantity);
            
            // Optional: Bauplan Shop Kosten
            if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts && 
                entry.blueprintShop != null && entry.blueprintShop.price != null) {
                lineCount += 1; // "Bauplan Shop Kosten:" Zeile
                if (entry.blueprintShop.price.coin != null) lineCount++;
                if (entry.blueprintShop.price.paper_shreds != null) lineCount++;
            }
        } else {
            lineCount += 1; // "Keine Baupläne im Clipboard"
        }
        
        return lineCount;
    }
}


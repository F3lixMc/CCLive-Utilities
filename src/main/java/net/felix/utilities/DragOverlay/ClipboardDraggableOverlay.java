package net.felix.utilities.DragOverlay;

import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.ItemViewer.CostItem;
import net.felix.utilities.Overall.InformationenUtility;
import net.felix.utilities.Overall.ActionBarData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

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
    
    private static final int DEFAULT_WIDTH = 180;
    private static final int DEFAULT_HEIGHT = 150;
    
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
    
    /**
     * Calculate unscaled width (base width without scale factor)
     * Berechnet basierend auf dem neuen Format: "Bauplanname [Anzahl]", Kostenliste
     * Berücksichtigt tatsächliche Bauplan-Namen aus dem Clipboard
     */
    private static int calculateUnscaledWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return DEFAULT_WIDTH;
        
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
        
        // Berechne Breite für "Bauplanname [Anzahl]" + Buttons
        // Hole alle tatsächlichen Bauplan-Namen aus dem Clipboard
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        
        // Buttons: « + Leerzeichen + | + Leerzeichen + » + Abstand
        String leftArrow = "«";
        String rightArrow = "»";
        String separator = "|";
        String space = " ";
        int leftArrowWidth = client.textRenderer.getWidth(leftArrow);
        int rightArrowWidth = client.textRenderer.getWidth(rightArrow);
        int separatorWidth = client.textRenderer.getWidth(separator);
        int spaceWidth = client.textRenderer.getWidth(space);
        int totalButtonWidth = leftArrowWidth + spaceWidth + separatorWidth + spaceWidth + rightArrowWidth;
        int buttonSpacing = 5; // Abstand zwischen Buttons und [Anzahl]
        int countPadding = 5; // Padding rechts
        int namePadding = 5; // Padding links für Bauplan-Name
        
        // Berücksichtige Textfeld-Breite (50px) wenn in Inventar
        int textFieldWidth = 50;
        boolean isInInventory = client.currentScreen instanceof HandledScreen;
        
        // Prüfe alle Bauplan-Namen
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (entry.blueprintName != null) {
                // Berechne Breite für "Bauplanname [Anzahl]"
                String countText = "[" + entry.quantity + "]";
                int countWidth = client.textRenderer.getWidth(countText);
                int nameWidth = client.textRenderer.getWidth(entry.blueprintName);
                
                // Berechne benötigte Breite: Name + Buttons + Abstand + [Anzahl/Textfeld] + Padding
                int countOrTextFieldWidth = isInInventory ? textFieldWidth : countWidth;
                int totalLineWidth = namePadding + nameWidth + buttonSpacing + totalButtonWidth + buttonSpacing + countOrTextFieldWidth + countPadding;
                maxWidth = Math.max(maxWidth, totalLineWidth);
            }
        }
        
        // Prüfe auch "Gesamtliste" für Seite 1
        String gesamtlisteText = "Gesamtliste";
        // Hole aktuelle Anzahl für Seite 1 (aus Textfeld oder Standard 1)
        int quantityForPage1 = getQuantityForPage(1, null);
        String countText = "[" + quantityForPage1 + "]";
        int countWidth = client.textRenderer.getWidth(countText);
        int nameWidth = client.textRenderer.getWidth(gesamtlisteText);
        int countOrTextFieldWidth = isInInventory ? textFieldWidth : countWidth;
        int totalLineWidth = namePadding + nameWidth + buttonSpacing + totalButtonWidth + buttonSpacing + countOrTextFieldWidth + countPadding;
        maxWidth = Math.max(maxWidth, totalLineWidth);
        
        // Prüfe alle Materialien in den Einträgen für "(Ebene X)" Text
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (entry.price != null) {
                // Prüfe alle CostItems
                CostItem[] costItems = {
                    entry.price.coin, entry.price.material1, entry.price.material2,
                    entry.price.Amboss, entry.price.Ressource
                };
                
                for (CostItem costItem : costItems) {
                    if (costItem != null && costItem.itemName != null && costItem.amount != null) {
                        boolean isCoins = "Coins".equalsIgnoreCase(costItem.itemName);
                        
                        // Verwende das neue Format: "0 / 15 Materialname"
                        double neededAmount = parseAmountToDouble(costItem.amount);
                        double ownedAmount = 0.0;
                        if (isCoins) {
                            ownedAmount = ClipboardCoinCollector.getCurrentCoins();
                        } else {
                            ownedAmount = getOwnedMaterialAmount(costItem.itemName);
                        }
                        String ownedAmountStr = formatAmount(ownedAmount, true);
                        String neededAmountStr = formatAmount(costItem.amount, true);
                        String materialLine = ownedAmountStr + " / " + neededAmountStr + " " + costItem.itemName;
                        
                        // Prüfe ob es ein Aincraft-Material ist
                        InformationenUtility.MaterialFloorInfo floorInfo = 
                            InformationenUtility.getMaterialFloorInfo(costItem.itemName);
                        
                        if (floorInfo != null) {
                            // Füge Breite für "(Ebene X)" hinzu
                            String floorText = " (Ebene " + floorInfo.floor + ")";
                            int floorTextWidth = client.textRenderer.getWidth(floorText);
                            int materialLineWidth = client.textRenderer.getWidth(materialLine);
                            int totalCostWidth = materialLineWidth + floorTextWidth + 5; // 5px padding
                            maxWidth = Math.max(maxWidth, totalCostWidth);
                        } else {
                            // Normale Breite ohne Ebene
                            int materialLineWidth = client.textRenderer.getWidth(materialLine) + 5;
                            maxWidth = Math.max(maxWidth, materialLineWidth);
                        }
                    }
                }
            }
            
            // Prüfe auch Blueprint Shop Kosten
            if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts && 
                entry.blueprintShop != null && entry.blueprintShop.price != null) {
                CostItem[] shopCostItems = {
                    entry.blueprintShop.price.coin, entry.blueprintShop.price.paper_shreds
                };
                
                for (CostItem costItem : shopCostItems) {
                    if (costItem != null && costItem.itemName != null && costItem.amount != null) {
                        boolean isCoins = "Coins".equalsIgnoreCase(costItem.itemName);
                        
                        // Verwende das neue Format: "0 / 15 Materialname"
                        double neededAmount = parseAmountToDouble(costItem.amount);
                        double ownedAmount = 0.0;
                        if (isCoins) {
                            ownedAmount = ClipboardCoinCollector.getCurrentCoins();
                        } else {
                            ownedAmount = getOwnedMaterialAmount(costItem.itemName);
                        }
                        String ownedAmountStr = formatAmount(ownedAmount, true);
                        String neededAmountStr = formatAmount(costItem.amount, true);
                        String materialLine = ownedAmountStr + " / " + neededAmountStr + " " + costItem.itemName;
                        
                        // Prüfe ob es ein Aincraft-Material ist
                        InformationenUtility.MaterialFloorInfo floorInfo = 
                            InformationenUtility.getMaterialFloorInfo(costItem.itemName);
                        
                        if (floorInfo != null) {
                            // Füge Breite für "(Ebene X)" hinzu
                            String floorText = " (Ebene " + floorInfo.floor + ")";
                            int floorTextWidth = client.textRenderer.getWidth(floorText);
                            int materialLineWidth = client.textRenderer.getWidth(materialLine);
                            int totalCostWidth = materialLineWidth + floorTextWidth + 5; // 5px padding
                            maxWidth = Math.max(maxWidth, totalCostWidth);
                        } else {
                            // Normale Breite ohne Ebene
                            int materialLineWidth = client.textRenderer.getWidth(materialLine) + 5;
                            maxWidth = Math.max(maxWidth, materialLineWidth);
                        }
                    }
                }
            }
        }
        
        // Prüfe auch zusammengerechnete Kosten für Seite 1 (Gesamtliste)
        List<CostItem> totalCosts = calculateTotalCosts(false);
        for (CostItem costItem : totalCosts) {
            if (costItem != null && costItem.itemName != null && costItem.amount != null) {
                boolean isCoins = "Coins".equalsIgnoreCase(costItem.itemName);
                // Verwende das neue Format: "0 / 15 Materialname"
                double neededAmount = parseAmountToDouble(costItem.amount);
                double ownedAmount = 0.0;
                if (isCoins) {
                    ownedAmount = ClipboardCoinCollector.getCurrentCoins();
                } else {
                    ownedAmount = getOwnedMaterialAmount(costItem.itemName);
                }
                String ownedAmountStr = formatAmount(ownedAmount, true);
                String neededAmountStr = formatAmount(costItem.amount, true);
                String materialLine = ownedAmountStr + " / " + neededAmountStr + " " + costItem.itemName;
                
                // Für Coins: Berechne abgekürzte Version (für alle Seiten)
                String abbreviatedText = "";
                if (isCoins) {
                    try {
                        long coinAmount = (long) neededAmount;
                        if (coinAmount >= 1000) {
                            String abbreviated = formatNumberAbbreviated(coinAmount);
                            abbreviatedText = " (" + abbreviated + ")";
                        }
                    } catch (Exception e) {
                        // Ignoriere Fehler bei Abkürzung
                    }
                }
                
                // Prüfe ob es ein Aincraft-Material ist
                InformationenUtility.MaterialFloorInfo floorInfo = 
                    InformationenUtility.getMaterialFloorInfo(costItem.itemName);
                
                int materialLineWidth = client.textRenderer.getWidth(materialLine);
                int abbreviatedWidth = abbreviatedText.isEmpty() ? 0 : client.textRenderer.getWidth(abbreviatedText);
                
                if (floorInfo != null) {
                    // Füge Breite für "(Ebene X)" hinzu
                    String floorText = " (Ebene " + floorInfo.floor + ")";
                    int floorTextWidth = client.textRenderer.getWidth(floorText);
                    int totalCostWidth = materialLineWidth + abbreviatedWidth + floorTextWidth + 5; // 5px padding
                    maxWidth = Math.max(maxWidth, totalCostWidth);
                } else {
                    // Normale Breite ohne Ebene, aber mit Abkürzung falls vorhanden
                    int totalCostWidth = materialLineWidth + abbreviatedWidth + 5;
                    maxWidth = Math.max(maxWidth, totalCostWidth);
                }
            }
        }
        
        // Prüfe auch zusammengerechnete Shop-Kosten für Seite 1
        if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts) {
            List<CostItem> totalShopCosts = calculateTotalCosts(true);
            for (CostItem costItem : totalShopCosts) {
                if (costItem != null && costItem.itemName != null && costItem.amount != null) {
                    boolean isCoins = "Coins".equalsIgnoreCase(costItem.itemName);
                    boolean needsSeparators = needsThousandSeparators(costItem.itemName);
                    
                    // Verwende das neue Format: "0 / 15 Materialname"
                    double neededAmount = parseAmountToDouble(costItem.amount);
                    double ownedAmount = 0.0;
                    if (isCoins) {
                        ownedAmount = ClipboardCoinCollector.getCurrentCoins();
                    } else {
                        ownedAmount = getOwnedMaterialAmount(costItem.itemName);
                    }
                    String ownedAmountStr = formatAmount(ownedAmount, needsSeparators);
                    String neededAmountStr = formatAmount(costItem.amount, needsSeparators);
                    String materialLine = ownedAmountStr + " / " + neededAmountStr + " " + costItem.itemName;
                    
                    // Für Coins auf Seite 1: Berechne abgekürzte Version
                    String abbreviatedText = "";
                    if (isCoins && ClipboardUtility.getCurrentPage() == 1) {
                        try {
                            long coinAmount = (long) neededAmount;
                            if (coinAmount >= 1000) {
                                String abbreviated = formatNumberAbbreviated(coinAmount);
                                abbreviatedText = " (" + abbreviated + ")";
                            }
                        } catch (Exception e) {
                            // Ignoriere Fehler bei Abkürzung
                        }
                    }
                    
                    // Prüfe ob es ein Aincraft-Material ist
                    InformationenUtility.MaterialFloorInfo floorInfo = 
                        InformationenUtility.getMaterialFloorInfo(costItem.itemName);
                    
                    int materialLineWidth = client.textRenderer.getWidth(materialLine);
                    int abbreviatedWidth = abbreviatedText.isEmpty() ? 0 : client.textRenderer.getWidth(abbreviatedText);
                    
                    if (floorInfo != null) {
                        // Füge Breite für "(Ebene X)" hinzu
                        String floorText = " (Ebene " + floorInfo.floor + ")";
                        int floorTextWidth = client.textRenderer.getWidth(floorText);
                        int totalCostWidth = materialLineWidth + abbreviatedWidth + floorTextWidth + 5; // 5px padding
                        maxWidth = Math.max(maxWidth, totalCostWidth);
                    } else {
                        // Normale Breite ohne Ebene, aber mit Abkürzung falls vorhanden
                        int totalCostWidth = materialLineWidth + abbreviatedWidth + 5;
                        maxWidth = Math.max(maxWidth, totalCostWidth);
                    }
                }
            }
        }
        
        return maxWidth;
    }
    
    /**
     * Calculate unscaled height (base height without scale factor)
     * Berechnet basierend auf dem neuen Format (ohne Titel und Trennlinie im Spiel)
     */
    private static int calculateUnscaledHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return DEFAULT_HEIGHT;
        
        int lineHeight = client.textRenderer.fontHeight + 2;
        int padding = 5;
        
        // "Bauplanname [Anzahl]" - 1 Zeile
        int height = lineHeight;
        
        // "Bauplan Kosten:" oder "Kosten:" - 1 Zeile
        height += lineHeight;
        
        // Beispiel-Kosten (4 Zeilen: Coins, Material, Amboss, Ressource)
        height += lineHeight * 4;
        
        // Optional: "Bauplan Shop Kosten" + Beispiel-Kosten (Coins, Pergamentfetzen)
        if (CCLiveUtilitiesConfig.HANDLER.instance().clipboardShowBlueprintShopCosts) {
            height += lineHeight; // "Bauplan Shop Kosten"
            height += lineHeight * 2; // Coins, Pergamentfetzen
        }
        
        // Padding oben und unten
        height += padding * 2;
        
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
        context.drawBorder(x, y, width, height, 0xFFFF0000);
        
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
        String countText = "[Anzahl]";
        
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
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        int x = CCLiveUtilitiesConfig.HANDLER.instance().clipboardX;
        int y = CCLiveUtilitiesConfig.HANDLER.instance().clipboardY;
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().clipboardScale;
        if (scale <= 0) scale = 1.0f;
        
        // Berechne Höhe ZUERST (ohne zu rendern), um Background vor dem Text zu rendern
        int lineHeight = client.textRenderer.fontHeight + 2;
        int padding = 5;
        int estimatedLineCount = calculateEstimatedLineCount();
        int estimatedHeight = padding + (estimatedLineCount * lineHeight) + padding;
        
        // Render background VOR dem Text, damit Text in voller Deckkraft angezeigt wird
        int width = Math.round(calculateUnscaledWidth() * scale);
        int height = Math.round(estimatedHeight * scale);
        context.fill(x, y, x + width, y + height, 0x80000000);
        
        // Render content with scale using matrix transformation
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        
        // lineHeight und padding wurden bereits oben definiert
        int currentY = padding;
        int unscaledWidth = calculateUnscaledWidth();
        
        // Zähle Zeilen für dynamische Höhenberechnung
        int lineCount = 0;
        
        // Hole aktuelle Seite und Daten
        int currentPage = ClipboardUtility.getCurrentPage();
        ClipboardUtility.ClipboardEntry entry = ClipboardUtility.getEntryForPage(currentPage);
        
        // "Bauplanname [Anzahl]" - Anzahl oben rechts, mit Pfeil-Buttons davor
        String blueprintName;
        String countText;
        
        if (currentPage == 1) {
            // Seite 1: Gesamtliste
            blueprintName = "Gesamtliste";
            // Hole aktuelle Anzahl für Seite 1 (aus Textfeld oder Standard 1)
            int quantityForPage1 = getQuantityForPage(1, null);
            countText = "[" + quantityForPage1 + "]";
        } else if (entry != null) {
            // Seite 2+: Einzelner Bauplan
            blueprintName = entry.blueprintName != null ? entry.blueprintName : "Bauplan Name";
            countText = "[" + entry.quantity + "]";
        } else {
            // Fallback
            blueprintName = "Bauplan Name";
            countText = "[Anzahl]";
        }
        
        // Berechne Button-Positionen mit Hilfsmethode
        int[] buttonPositions = calculateButtonPositions(client, blueprintName, countText, unscaledWidth);
        int leftButtonX = buttonPositions[0];
        int rightButtonX = buttonPositions[1];
        int spaceAfterLeftX = buttonPositions[2];
        int separatorX = buttonPositions[3];
        int spaceAfterSeparatorX = buttonPositions[4];
        int spaceAfterRightX = buttonPositions[5];
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
            // In Inventar: Rendere Textfeld für [Anzahl] (auch auf Seite 1)
            // rightButtonX wird aus buttonPositions[1] geholt
            int textFieldRightButtonX = buttonPositions[1];
            renderQuantityTextField(context, client, countX, currentY, currentPage, quantity, unscaledWidth, scale, textFieldRightButtonX);
        } else {
            // Außerhalb von Inventar: Rendere als Text
            context.drawText(
                client.textRenderer,
                countText,
                countX, currentY,
                0xFFFFFF00, // Gelb für Anzahl
                true
            );
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
            if (entry.price.coin != null) costItemsWithCategory.add(new CostItemWithCategory(entry.price.coin, 0)); // Coins
            if (entry.price.material1 != null) costItemsWithCategory.add(new CostItemWithCategory(entry.price.material1, 1)); // Materialien
            if (entry.price.material2 != null) costItemsWithCategory.add(new CostItemWithCategory(entry.price.material2, 1)); // Materialien
            if (entry.price.Amboss != null) costItemsWithCategory.add(new CostItemWithCategory(entry.price.Amboss, 2)); // Amboss
            if (entry.price.Ressource != null) costItemsWithCategory.add(new CostItemWithCategory(entry.price.Ressource, 3)); // Ressource
            
            // Kostenanzeige-Filterung/Sortierung (nur für normale Kosten, nicht für Bauplan Shop Kosten)
            boolean costDisplayEnabled = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayEnabled;
            if (costDisplayEnabled) {
                int costDisplayMode = CCLiveUtilitiesConfig.HANDLER.instance().clipboardCostDisplayMode;
                
                if (costDisplayMode == 1) {
                    // Mode 1: Ausblenden fertiger Items (außer Coins)
                    costItemsWithCategory.removeIf(item -> {
                        if (item.category == 0) { // Coins
                            return false; // Coins immer anzeigen
                        }
                        return isCostItemComplete(item.costItem, quantity);
                    });
                } else if (costDisplayMode == 2) {
                    // Mode 2: Fertige Items ans Ende ALLER Kategorien setzen
                    // Trenne nicht-fertige und fertige Items
                    List<CostItemWithCategory> notComplete = new ArrayList<>();
                    List<CostItemWithCategory> complete = new ArrayList<>();
                    
                    for (CostItemWithCategory item : costItemsWithCategory) {
                        if (item.category == 0) { // Coins
                            notComplete.add(item); // Coins immer zuerst
                        } else if (isCostItemComplete(item.costItem, quantity)) {
                            complete.add(item);
                        } else {
                            notComplete.add(item);
                        }
                    }
                    
                    // Sortiere nicht-fertige Items nach Kategorie
                    notComplete.sort((item1, item2) -> Integer.compare(item1.category, item2.category));
                    
                    // Sortiere fertige Items nach Kategorie
                    complete.sort((item1, item2) -> Integer.compare(item1.category, item2.category));
                    
                    // Setze Liste zurück: zuerst nicht-fertige, dann fertige
                    costItemsWithCategory.clear();
                    costItemsWithCategory.addAll(notComplete);
                    costItemsWithCategory.addAll(complete);
                }
            }
            
            // Rendere gefilterte/sortierte Liste
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
        
        // Background wurde bereits VOR dem Text gerendert, damit Text in voller Deckkraft angezeigt wird
        // Falls die tatsächliche Höhe von der geschätzten Höhe abweicht, aktualisiere den Background
        int actualScaledHeight = Math.round(actualHeight * scale);
        if (actualScaledHeight != height) {
            // Aktualisiere Background mit korrekter Höhe
            context.fill(x, y, x + width, y + actualScaledHeight, 0x80000000);
        }
        
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
        context.drawBorder(boxX, boxY, boxWidth, boxHeight, 0xFFFFFFFF);
        
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
     * Rendert eine Kosten-Zeile
     * @param context DrawContext
     * @param client MinecraftClient
     * @param costItem CostItem (kann null sein)
     * @param y Y-Position
     * @return true wenn eine Zeile gerendert wurde, false wenn nicht
     */
    private static boolean renderCostItem(DrawContext context, MinecraftClient client, CostItem costItem, int y) {
        return renderCostItem(context, client, costItem, y, 1);
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
            // Berechne benötigte Menge
            double neededAmount = parseAmountToDouble(multipliedItem.amount);
            
            // Hole vorhandene Menge aus dem Material-Tracker (für Materialien) oder ClipboardCoinCollector (für Coins)
            double ownedAmount = 0.0;
            if (isCoins) {
                ownedAmount = ClipboardCoinCollector.getCurrentCoins();
            } else {
                ownedAmount = getOwnedMaterialAmount(costItem.itemName);
            }
            
            // Bestimme Farbe basierend auf vorhandener vs. benötigter Menge
            int textColor;
            if (neededAmount == 0) {
                // Benötigt = 0: immer grün
                textColor = 0xFF00FF00; // Grün
            } else if (ownedAmount >= neededAmount) {
                // Genug vorhanden: grün
                textColor = 0xFF00FF00; // Grün
            } else {
                // Nicht genug vorhanden: rot
                textColor = 0xFFFF5555; // Rot (Minecraft Standard)
            }
            
            // Formatiere vorhandene und benötigte Menge (immer mit Trennzeichen)
            String ownedAmountStr = formatAmount(ownedAmount, true);
            String neededAmountStr = formatAmount(multipliedItem.amount, true);
            String materialLine = ownedAmountStr + " / " + neededAmountStr + " " + costItem.itemName;
            
            // Für Coins: Füge abgekürzte Version hinzu (für alle Seiten)
            // Hinweis: Pergamentfetzen bekommen keine Abkürzung, nur Trennzeichen
            String abbreviatedText = "";
            if (isCoins) {
                try {
                    // Konvertiere amount zu long
                    long coinAmount = 0;
                    if (multipliedItem.amount instanceof Number) {
                        coinAmount = ((Number) multipliedItem.amount).longValue();
                    } else if (multipliedItem.amount instanceof String) {
                        try {
                            coinAmount = Long.parseLong(removeThousandSeparators((String) multipliedItem.amount));
                        } catch (NumberFormatException e) {
                            // Ignoriere, wenn nicht parsbar
                        }
                    }
                    
                    if (coinAmount >= 1000) {
                        String abbreviated = formatNumberAbbreviated(coinAmount);
                        abbreviatedText = " (" + abbreviated + ")";
                    }
                } catch (Exception e) {
                    // Ignoriere Fehler bei Abkürzung
                }
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
                // Rendere "(Ebene X)" in der Rarity-Farbe
                String floorText = " (Ebene " + floorInfo.floor + ")";
                int rarityColor = getRarityColor(floorInfo.rarity);
                int floorTextX = 5 + materialLineWidth + abbreviatedWidth; // Nach Material-Text und Abkürzung
                
                context.drawText(
                    client.textRenderer,
                    floorText,
                    floorTextX, y,
                    rarityColor, // Rarity-Farbe nur für "(Ebene X)"
                    true
                );
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Gets the color for a rarity level (same as InformationenUtility.getRarityColor)
     */
    private static int getRarityColor(String rarity) {
        if (rarity == null) {
            return 0xFFFFFFFF; // Default white
        }
        
        switch (rarity.toLowerCase()) {
            case "common":
                return 0xFFFFFFFF; // White
            case "uncommon":
                return 0xFF1EFC00; // #1EFC00
            case "rare":
                return 0xFF006FDA; // #006FDA
            case "epic":
                return 0xFFA134EB; // #A134EB
            case "legendary":
                return 0xFFFF7E00; // #FC7E00
            case "mob":
                return 0xFFFFFFFF; // White for mob names
            default:
                return 0xFF808080; // Gray
        }
    }
    
    /**
     * Prüft ob ein Item-Name Tausendertrennzeichen benötigt (Coins oder Pergamentfetzen)
     */
    private static boolean needsThousandSeparators(String itemName) {
        if (itemName == null) return false;
        return "Coins".equalsIgnoreCase(itemName) || "Pergamentfetzen".equalsIgnoreCase(itemName);
    }
    
    /**
     * Formatiert einen Amount-Wert: Ganze Zahlen ohne ".0", andere bleiben wie sie sind
     * Alle Werte werden mit Tausendertrennzeichen formatiert (1.000.000)
     */
    private static String formatAmount(Object amount, boolean isCoins) {
        if (amount == null) {
            return "0";
        }
        
        // Wenn es bereits ein String ist, versuche zu parsen und zu formatieren
        if (amount instanceof String) {
            // Versuche String zu parsen und zu formatieren
            // Entferne zuerst Tausendertrennzeichen, falls vorhanden
            try {
                String cleaned = removeThousandSeparators((String) amount);
                double value = Double.parseDouble(cleaned);
                return formatNumberWithSeparators(value);
            } catch (NumberFormatException e) {
                // Wenn Parsing fehlschlägt, gib den ursprünglichen String zurück
                return (String) amount;
            }
        }
        
        // Wenn es eine Zahl ist
        if (amount instanceof Number) {
            Number num = (Number) amount;
            double doubleValue = num.doubleValue();
            
            // Verwende immer Tausendertrennzeichen
            return formatNumberWithSeparators(doubleValue);
        }
        
        // Fallback: toString()
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
            // Zahl mit Dezimalstellen
            DecimalFormat df = new DecimalFormat("#,###.0", symbols);
            return df.format(value);
        }
    }
    
    /**
     * Formatiert eine Zahl mit Abkürzung (K, M, B, T) - übernommen aus PlayerHoverStatsUtility
     * @param number Die zu formatierende Zahl
     * @return Formatierter String z.B. "1.5M" für 1.500.000
     */
    private static String formatNumberAbbreviated(long number) {
        try {
            if (number < 1000) {
                return String.valueOf(number);
            }
            
            // Trillion (T): 1.000.000.000.000+
            if (number >= 1_000_000_000_000L) {
                double value = number / 1_000_000_000_000.0;
                return formatAbbreviatedWithSeparator(value, "T");
            }
            
            // Billion (B): 1.000.000.000 - 999.999.999.999
            if (number >= 1_000_000_000L) {
                double value = number / 1_000_000_000.0;
                return formatAbbreviatedWithSeparator(value, "B");
            }
            
            // Million (M): 1.000.000 - 999.999.999
            if (number >= 1_000_000L) {
                double value = number / 1_000_000.0;
                return formatAbbreviatedWithSeparator(value, "M");
            }
            
            // Thousand (K): 1.000 - 999.999
            if (number >= 1_000L) {
                double value = (double) number / 1_000.0;
                return formatAbbreviatedWithSeparator(value, "K");
            }
            
            return String.valueOf(number);
        } catch (Exception e) {
            return String.valueOf(number);
        }
    }
    
    /**
     * Formatiert einen Wert mit Tausendertrennzeichen (Punkte) und Suffix
     * z.B. 1.5M für 1.500.000
     */
    private static String formatAbbreviatedWithSeparator(double value, String suffix) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            return "0" + suffix;
        }
        
        // Runde auf 1 Dezimalstelle
        double rounded = Math.round(value * 10.0) / 10.0;
        
        // Wenn gerundet eine ganze Zahl ist, zeige keine Dezimalstelle
        if (rounded == Math.floor(rounded)) {
            return String.format("%.0f%s", rounded, suffix);
        } else {
            return String.format("%.1f%s", rounded, suffix);
        }
    }
    
    /**
     * Formatiert einen Amount-Wert (Standard, nicht für Coins)
     */
    private static String formatAmount(Object amount) {
        return formatAmount(amount, false);
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
        if (materialName == null) {
            return "";
        }
        // Entferne Leerzeichen und konvertiere zu Kleinbuchstaben
        return materialName.replaceAll("\\s+", "").toLowerCase();
    }
    
    /**
     * Ruft die vorhandene Menge eines Materials aus dem Material-Tracker ab
     * Funktioniert unabhängig davon, ob der Material-Tracker-Overlay aktiviert ist,
     * da ActionBarData die Materialien immer sammelt (via ActionBarMixin)
     */
    /**
     * Gibt die vorhandene Menge eines Materials zurück
     * Für Pergamentfetzen wird der ClipboardPaperShredsCollector verwendet
     * Für andere Materialien wird ActionBarData verwendet
     */
    /**
     * Bestimmt die Kategorie eines CostItems für die Sortierung
     * @param costItem Das CostItem
     * @return 0 = Coins, 1 = Materialien, 2 = Amboss, 3 = Ressource
     */
    private static int getCostItemCategory(CostItem costItem) {
        if (costItem == null || costItem.itemName == null) {
            return 1; // Default: Materialien
        }
        String itemName = costItem.itemName;
        if ("Coins".equalsIgnoreCase(itemName)) {
            return 0; // Coins
        }
        // Prüfe ob es Amboss oder Ressource ist (basierend auf entry.price)
        List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
        for (ClipboardUtility.ClipboardEntry entry : entries) {
            if (entry.price != null) {
                if (entry.price.Amboss != null && itemName.equals(entry.price.Amboss.itemName)) {
                    return 2; // Amboss
                }
                if (entry.price.Ressource != null && itemName.equals(entry.price.Ressource.itemName)) {
                    return 3; // Ressource
                }
            }
        }
        return 1; // Materialien
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
        double neededAmount = parseAmountToDouble(multipliedItem.amount);
        
        // Wenn needed == 0, ist es immer "fertig" (grün)
        if (neededAmount == 0) {
            return true;
        }
        
        // Hole vorhandene Menge
        boolean isCoins = "Coins".equalsIgnoreCase(multipliedItem.itemName);
        double ownedAmount = 0.0;
        if (isCoins) {
            ownedAmount = ClipboardCoinCollector.getCurrentCoins();
        } else {
            ownedAmount = getOwnedMaterialAmount(costItem.itemName);
        }
        
        // Prüfe ob owned >= needed
        return ownedAmount >= neededAmount;
    }
    
    private static double getOwnedMaterialAmount(String materialName) {
        // Prüfe ob es Pergamentfetzen sind
        if ("Pergamentfetzen".equalsIgnoreCase(materialName)) {
            return ClipboardPaperShredsCollector.getCurrentPaperShreds();
        }
        if (materialName == null) {
            return 0.0;
        }
        
        // Prüfe ob es ein Amboss-Item ist
        long ambossAmount = ClipboardAmbossRessourceCollector.getAmbossAmount(materialName);
        if (ambossAmount > 0) {
            return ambossAmount;
        }
        
        // Prüfe ob es ein Ressource-Item ist
        long ressourceAmount = ClipboardAmbossRessourceCollector.getRessourceAmount(materialName);
        if (ressourceAmount > 0) {
            return ressourceAmount;
        }
        
        try {
            // Hole Materialien aus ActionBarData
            // ActionBarData sammelt Materialien immer, unabhängig von Overlay-Einstellungen
            Map<String, Integer> materials = ActionBarData.getMaterials();
            if (materials == null || materials.isEmpty()) {
                return 0.0;
            }
            
            // Normalisiere den Materialnamen für den Vergleich
            String normalizedName = normalizeMaterialName(materialName);
            
            // Suche nach einem passenden Materialnamen (normalisiert)
            for (Map.Entry<String, Integer> entry : materials.entrySet()) {
                String normalizedEntryName = normalizeMaterialName(entry.getKey());
                if (normalizedName.equals(normalizedEntryName)) {
                    return entry.getValue().doubleValue();
                }
            }
        } catch (Exception e) {
            // Fallback: Wenn etwas schiefgeht, gebe 0 zurück
            return 0.0;
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
                // Normale Bauplan-Kosten
                if (entry.price != null) {
                    // Addiere alle Kosten (multipliziert mit Anzahl)
                    addCostItemToTotal(totalCostsMap, multiplyCostItem(entry.price.coin, quantity));
                    addCostItemToTotal(totalCostsMap, multiplyCostItem(entry.price.material1, quantity));
                    addCostItemToTotal(totalCostsMap, multiplyCostItem(entry.price.material2, quantity));
                    addCostItemToTotal(totalCostsMap, multiplyCostItem(entry.price.Amboss, quantity));
                    addCostItemToTotal(totalCostsMap, multiplyCostItem(entry.price.Ressource, quantity));
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
        int countWidth = client.textRenderer.getWidth(countText);
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
        
        // countX wird für die Position des [Anzahl] Textes berechnet (wenn kein Textfeld)
        int countX = unscaledWidth - countWidth - 5;
        int nameX = 5;
        int buttonSpacing = 5;
        
        // Berücksichtige Textfeld-Breite (50px) wenn in Inventar
        int textFieldWidth = 50;
        int textFieldX = unscaledWidth - textFieldWidth - 5; // Textfeld rechts am Ende
        
        // Buttons sollen links vom Textfeld stehen
        // Position für Buttons: textFieldX - buttonSpacing - totalButtonWidth
        int buttonsX = textFieldX - buttonSpacing - totalButtonWidth;
        
        int nameEndX = nameX + nameWidth;
        int buttonsAfterNameX = nameEndX + buttonSpacing;
        
        int leftButtonX, rightButtonX, spaceAfterLeftX, separatorX, spaceAfterSeparatorX, spaceAfterRightX;
        
        // Prüfe ob Buttons nach dem Namen passen oder rechts positioniert werden müssen
        if (buttonsAfterNameX + totalButtonWidth <= buttonsX) {
            // Name ist kurz: Buttons rechts (links vom Textfeld)
            leftButtonX = buttonsX;
            spaceAfterLeftX = leftButtonX + leftArrowWidth;
            separatorX = spaceAfterLeftX + spaceWidth;
            spaceAfterSeparatorX = separatorX + separatorWidth;
            spaceAfterRightX = spaceAfterSeparatorX + spaceWidth;
            rightButtonX = spaceAfterRightX;
        } else {
            // Name ist lang: Buttons direkt nach dem Namen
            leftButtonX = nameEndX + buttonSpacing;
            spaceAfterLeftX = leftButtonX + leftArrowWidth;
            separatorX = spaceAfterLeftX + spaceWidth;
            spaceAfterSeparatorX = separatorX + separatorWidth;
            spaceAfterRightX = spaceAfterSeparatorX + spaceWidth;
            rightButtonX = spaceAfterRightX;
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
            if (confirmationJaButton != null && confirmationJaButton.mouseClicked(mouseX, mouseY, 0)) {
                // Alle Baupläne entfernen
                ClipboardUtility.clearClipboard();
                showDeleteConfirmation = false;
                confirmationButtonsVisible = false;
                // Gehe zurück zu Seite 1
                ClipboardUtility.setCurrentPage(1);
                return true;
            }
            
            if (confirmationNeinButton != null && confirmationNeinButton.mouseClicked(mouseX, mouseY, 0)) {
                // Bestätigung abbrechen
                showDeleteConfirmation = false;
                confirmationButtonsVisible = false;
                return true;
            }
            
            // Klick war nicht auf Ja/Nein Buttons
            return false;
        }
        
        // Bestätigungs-Overlay ist nicht offen - prüfe auf Haupt-Delete-Button
        if (deleteButton != null && deleteButton.mouseClicked(mouseX, mouseY, 0)) {
            // Bestätigungs-Overlay öffnen (für Seite 1) oder direkt entfernen (für Seiten 2+)
            int buttonPage = ClipboardUtility.getCurrentPage();
            if (buttonPage == 1) {
                showDeleteConfirmation = true;
            } else {
                // Entferne aktuellen Bauplan
                ClipboardUtility.ClipboardEntry currentEntry = ClipboardUtility.getEntryForPage(buttonPage);
                if (currentEntry != null && currentEntry.blueprintName != null) {
                    ClipboardUtility.removeBlueprint(currentEntry.blueprintName);
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
            countText = "[" + quantityForPage1 + "]";
        } else if (entry != null) {
            // Seite 2+: Einzelner Bauplan
            countText = "[" + entry.quantity + "]";
        } else {
            // Fallback
            countText = "[Anzahl]";
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
                    ClipboardUtility.removeBlueprint(blueprintNameToRemove);
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
     * Berechnet die Anzahl der Zeilen für das Overlay
     */
    private static int calculateLineCount(int currentPage, ClipboardUtility.ClipboardEntry entry) {
        int lineCount = 0;
        
        // Titel "Pinnwand" + Trennlinie
        lineCount += 2;
        
        // "Bauplanname [Anzahl]" Zeile
        lineCount += 1;
        
        // Kosten-Zeilen
        if (currentPage == 1) {
            // Seite 1: Gesamtliste
            List<ClipboardUtility.ClipboardEntry> entries = ClipboardUtility.getEntries();
            if (!entries.isEmpty()) {
                List<CostItem> totalCosts = calculateTotalCosts(false);
                if (!totalCosts.isEmpty()) {
                    lineCount += 1; // "Kosten:" Zeile
                    lineCount += totalCosts.size(); // Kosten-Items
                }
                
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
            // Seite 2+: Einzelner Bauplan
            lineCount += 1; // "Kosten:" Zeile
            
            // Zähle vorhandene Kosten-Items
            if (entry.price.coin != null) lineCount++;
            if (entry.price.material1 != null) lineCount++;
            if (entry.price.material2 != null) lineCount++;
            if (entry.price.Amboss != null) lineCount++;
            if (entry.price.Ressource != null) lineCount++;
            
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
            countText = "[" + quantityForPage1 + "]";
        } else if (entry != null) {
            blueprintName = entry.blueprintName != null ? entry.blueprintName : "Bauplan Name";
            countText = "[" + entry.quantity + "]";
        } else {
            blueprintName = "Bauplan Name";
            countText = "[Anzahl]";
        }
        
        int unscaledWidth = calculateUnscaledWidth();
        
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
        }
    }
    
    /**
     * Gibt die aktuelle Anzahl für eine Seite zurück (mit Fallback auf 1)
     */
    private static int getQuantityForPage(int page, ClipboardUtility.ClipboardEntry entry) {
        // Wenn Textfeld existiert und für diese Seite aktiv ist, verwende den Wert aus dem Textfeld
        if (quantityTextField != null && quantityTextFieldPage == page) {
            String text = quantityTextField.getText();
            if (text != null && !text.trim().isEmpty()) {
                    try {
                        int quantity = Integer.parseInt(text.trim());
                        if (quantity > 0) {
                            // Für Seite 1: Speichere in statischer Variable
                            if (page == 1) {
                                page1Quantity = quantity;
                            } else if (entry != null) {
                                // Für andere Seiten: Aktualisiere entry.quantity
                                entry.quantity = quantity;
                                // Speichere in Config
                                ClipboardUtility.saveClipboardEntries();
                            }
                            return quantity;
                        }
                } catch (NumberFormatException e) {
                    // Ungültige Eingabe, verwende Fallback
                }
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
     * @param rightButtonX X-Position des rechten Buttons (wird verwendet, um Textfeld rechts davon zu positionieren)
     */
    private static void renderQuantityTextField(DrawContext context, MinecraftClient client, 
                                                 int countX, int countY, int page, int currentQuantity,
                                                 int unscaledWidth, float scale, int rightButtonX) {
        // Erstelle oder aktualisiere Textfeld wenn nötig
        if (quantityTextField == null || quantityTextFieldPage != page) {
            quantityTextField = new TextFieldWidget(
                client.textRenderer,
                0, 0, // Wird später gesetzt
                50, // Breite
                client.textRenderer.fontHeight + 2, // Höhe
                Text.literal("")
            );
            quantityTextField.setMaxLength(10); // Maximal 10 Ziffern
            quantityTextField.setTextPredicate(s -> s.matches("\\d*")); // Nur Zahlen erlauben
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
                        // Aktualisiere gespeicherte Anzahl für Seite 1
                        if (quantityTextFieldPage == 1) {
                            page1Quantity = 1;
                        }
                    }
                }
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
        
        // Berechne Position (unskaliert, relativ zum Overlay)
        // Textfeld soll rechts von den Buttons stehen, nicht überlappen
        // FESTE Position: rechts vom rechten Button, damit es sich nicht verschiebt
        int textFieldWidth = 50;
        int textFieldHeight = client.textRenderer.fontHeight + 2;
        int buttonSpacing = 8; // 5px + 3px mehr Abstand = 8px
        
        // Berechne Textfeld-Position: rechts vom rechten Button
        // rightButtonX + buttonSpacing = Position rechts vom Button
        // Diese Position ist FEST und ändert sich nicht, auch wenn der Text im Textfeld sich ändert
        int textFieldX = rightButtonX + buttonSpacing;
        
        int textFieldY = countY;
        
        // Setze Textfeld-Position (unskaliert, wird durch Matrix-Transformation skaliert)
        quantityTextField.setX(textFieldX);
        quantityTextField.setY(textFieldY);
        quantityTextField.setWidth(textFieldWidth);
        quantityTextField.setHeight(textFieldHeight);
        
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
        
        // Prüfe ob Klick innerhalb des Textfelds ist (unskaliert)
        int textFieldX = quantityTextField.getX();
        int textFieldY = quantityTextField.getY();
        int textFieldWidth = quantityTextField.getWidth();
        int textFieldHeight = quantityTextField.getHeight();
        
        if (unscaledMouseX >= textFieldX && unscaledMouseX <= textFieldX + textFieldWidth && 
            unscaledMouseY >= textFieldY && unscaledMouseY <= textFieldY + textFieldHeight) {
            quantityTextField.setFocused(true);
            // Textfeld erwartet unskalierte Koordinaten
            quantityTextField.mouseClicked(unscaledMouseX, unscaledMouseY, button);
            return true;
        } else {
            quantityTextField.setFocused(false);
        }
        
        return false;
    }
    
    /**
     * Behandelt Tasteneingaben für das [Anzahl] Textfeld
     * Verarbeitet auch Zeicheneingaben, indem es die Zeichen aus den KeyCodes extrahiert
     */
    public static boolean handleQuantityTextFieldKeyPress(int keyCode, int scanCode, int modifiers) {
        if (quantityTextField != null && quantityTextField.isFocused()) {
            // Verarbeite normale Tasteneingaben (Backspace, Enter, etc.)
            if (quantityTextField.keyPressed(keyCode, scanCode, modifiers)) {
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
                return quantityTextField.charTyped(digit, modifiers);
            }
        }
        return false;
    }
    
    /**
     * Behandelt Zeicheneingaben für das [Anzahl] Textfeld (wird über keyPressed aufgerufen)
     */
    public static boolean handleQuantityTextFieldCharTyped(char chr, int modifiers) {
        if (quantityTextField != null && quantityTextField.isFocused()) {
            return quantityTextField.charTyped(chr, modifiers);
        }
        return false;
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
            // Seite 2+: Einzelner Bauplan
            lineCount += 1; // "Kosten:" Zeile
            
            // Zähle vorhandene Kosten-Items
            if (entry.price.coin != null) lineCount++;
            if (entry.price.material1 != null) lineCount++;
            if (entry.price.material2 != null) lineCount++;
            if (entry.price.Amboss != null) lineCount++;
            if (entry.price.Ressource != null) lineCount++;
            
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


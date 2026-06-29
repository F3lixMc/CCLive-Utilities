package net.felix.utilities.ItemViewer;

import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rendert Items in einem Grid-Layout (ähnlich JEI)
 */
public class ItemViewerGrid {
    
    private static final int COST_GREEN = 0xFF55FF55;
    private static final Pattern FISHING_MODIFIER_VALUE = Pattern.compile("[+-]?[\\d.,]+(?:%|cg|lo|k|m|b)?");
    
    private static final Map<ItemData, ItemStack> ITEM_STACK_CACHE = new WeakHashMap<>();
    private static final Map<String, Identifier> TEXTURE_ID_CACHE = new HashMap<>();
    
    private static ItemData lastTooltipItem = null;
    private static List<Text> cachedTooltipLines = null;

    public static void invalidateTooltipCache() {
        lastTooltipItem = null;
        cachedTooltipLines = null;
    }
    
    private static final int SLOT_SIZE = 18; // 16x16 Item + 1px Padding auf jeder Seite
    private static final Identifier PASSIVE_SKILL_SLOT_TEXTURE = Identifier.of("cclive-utilities", "textures/icons/passive_skill_slot.png");
    private static final Identifier CARD_SLOT_TEXTURE = Identifier.of("cclive-utilities", "textures/icons/card_slot.png");
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
    public void render(DrawContext context) {
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
    private void renderGridChrome(DrawContext context, int count, int usedRows) {
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
    private void renderSlotHighlightBorder(DrawContext context, int x, int y) {
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
    
    private void renderItemIcon(DrawContext context, ItemData item, int slotX, int slotY) {
        int iconX = slotX + 1;
        int iconY = slotY + 1;
        if (item.texture != null && !item.texture.isEmpty()) {
            context.drawTexture(
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
            context.drawTexture(RenderPipelines.GUI_TEXTURED, PASSIVE_SKILL_SLOT_TEXTURE, iconX, iconY, 0, 0, 16, 16, 16, 16);
            return;
        }
        if (isCardSlot(item)) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, CARD_SLOT_TEXTURE, iconX, iconY, 0, 0, 16, 16, 16, 16);
            return;
        }
        ItemStack itemStack = getCachedItemStack(item);
        if (!itemStack.isEmpty()) {
            renderItemStackGuiIcon(context, itemStack, iconX, iconY);
        }
    }

    /** Immer volles {@code drawItem} — flache Sprites zeigen bei CMD-/3D-Modellen oft das Basis-Item (z. B. Buch). */
    private static void renderItemStackGuiIcon(DrawContext context, ItemStack stack, int x, int y) {
        context.drawItem(stack, x, y, 0);
    }
    
    private static Identifier getTextureId(String texturePath) {
        return TEXTURE_ID_CACHE.computeIfAbsent(texturePath,
            path -> Identifier.of("cclive-utilities", "textures/" + path));
    }
    
    /**
     * Rendert Tooltip für das gehoverte Item
     */
    public void renderTooltip(DrawContext context) {
        if (hoveredItem != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            ItemStack itemStack = getCachedItemStack(hoveredItem);
            if (!itemStack.isEmpty()) {
                boolean animatedForgingRainbow = ForgingConditionUtility.usesAnimatedRainbow(
                        resolveForgingCondition(hoveredItem));
                if (animatedForgingRainbow || hoveredItem != lastTooltipItem) {
                    if (!animatedForgingRainbow) {
                        lastTooltipItem = hoveredItem;
                    }
                    cachedTooltipLines = buildTooltipLines(hoveredItem, itemStack, client, true);
                    updateItemViewerAspectOverlay(hoveredItem);
                }
                java.util.List<Text> tooltipLines = cachedTooltipLines;
                if (tooltipLines == null || tooltipLines.isEmpty()) {
                    return;
                }
                context.drawTooltip(client.textRenderer, tooltipLines, mouseX, mouseY);
            } else {
                net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
            }
        } else {
            lastTooltipItem = null;
            cachedTooltipLines = null;
            net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
        }
    }

    private void updateItemViewerAspectOverlay(ItemData hoveredItem) {
        if (hoveredItem == null || hoveredItem.info == null || hoveredItem.name == null || hoveredItem.name.isEmpty()) {
            net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
            return;
        }

        ItemInfo info = hoveredItem.info;
        boolean hasAspect = info.aspect != null && !info.aspect.isEmpty()
            && !"false".equalsIgnoreCase(info.aspect);
        if (!hasAspect) {
            net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
            return;
        }

        net.felix.utilities.Overall.InformationenUtility.AspectInfo aspectInfo =
            net.felix.utilities.Overall.InformationenUtility.getAspectInfoForBlueprintFull(hoveredItem.name);
        if (aspectInfo != null && aspectInfo.aspectName != null && !aspectInfo.aspectName.isEmpty()
            && !"-".equals(aspectInfo.aspectName)) {
            net.felix.utilities.Overall.Aspekte.AspectOverlay.updateAspectInfoFromItemViewer(hoveredItem.name, aspectInfo);
        } else {
            net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
        }
    }
    
    private List<Text> buildTooltipLines(ItemData hoveredItem, ItemStack itemStack, MinecraftClient client,
            boolean includeInteractionHints) {
                java.util.List<Text> tooltipLines = new java.util.ArrayList<>();
                try {
                    tooltipLines.add(itemStack.getName());
                    
                    // Wenn Advanced Tooltips aktiv sind, füge Item-ID hinzu
                    if (client.options.advancedItemTooltips) {
                        tooltipLines.add(Text.literal(itemStack.getItem().toString())
                            .setStyle(Style.EMPTY.withColor(0xFF808080)));
                    }
                } catch (Exception e) {
                    // Fallback: Nur Item-Name
                    tooltipLines.add(itemStack.getName());
                }
                
                // Leere Zeile zwischen Name und Tooltip
                tooltipLines.add(Text.empty());

                if ("fishing_components".equals(hoveredItem.category)) {
                    appendFoundAtLocationLines(tooltipLines, hoveredItem);
                }
                
                // Zusätzliche Informationen aus ItemInfo und anderen Daten
                if (hoveredItem.info != null) {
                    ItemInfo info = hoveredItem.info;

                    if ("fish_traps".equals(hoveredItem.category) || Boolean.TRUE.equals(info.fish_trap)) {
                        appendFishTrapTooltipLines(tooltipLines, hoveredItem, info, includeInteractionHints);
                        return tooltipLines;
                    }
                    
                    // Power Crystals haben ein spezielles Format
                    if (info.power_crystal != null && info.power_crystal) {
                        // Power Crystal Format:
                        // 1. Description (weiß)
                        // 2. Leere Zeile
                        // 3. level_info Zeilen (aqua)
                        // 4. first_upgrade (dark_aqua)
                        
                        // Description (weiß, nicht kursiv) - mit Zeilenumbruch bei "Spieler," oder "Owyn," und \n Unterstützung
                        if (info.description != null && !info.description.isEmpty()) {
                            String description = info.description;
                            
                            // Unterstütze \n Zeilenumbrüche
                            if (description.contains("\n")) {
                                // Teile bei \n und rendere jede Zeile separat
                                String[] lines = description.split("\n");
                                for (String line : lines) {
                                    // Auch leere Zeilen rendern (für Absätze)
                                    tooltipLines.add(Text.literal(line)
                                        .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                                }
                            } else {
                                // Kein \n vorhanden - prüfe ob "Spieler," oder "Owyn," vorhanden ist und teile dort
                                String splitMarker = null;
                                if (description.contains("Spieler,")) {
                                    splitMarker = "Spieler,";
                                } else if (description.contains("Owyn,")) {
                                    splitMarker = "Owyn,";
                                }
                                
                                if (splitMarker != null) {
                                    String[] parts = description.split(splitMarker, 2);
                                    if (parts.length == 2) {
                                        // Erste Zeile: Teil vor Split-Marker + Split-Marker
                                        tooltipLines.add(Text.literal(parts[0] + splitMarker)
                                            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                                        // Zweite Zeile: Teil nach Split-Marker (trim, um führende Leerzeichen zu entfernen)
                                        String secondPart = parts[1].trim();
                                        if (!secondPart.isEmpty()) {
                                            tooltipLines.add(Text.literal(secondPart)
                                                .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                                        }
                                    } else {
                                        // Fallback: normale Anzeige
                                        tooltipLines.add(Text.literal(description)
                                            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                                    }
                                } else {
                                    // Kein Split-Marker gefunden - normale Anzeige
                                    tooltipLines.add(Text.literal(description)
                                        .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                                }
                            }
                        }
                        
                        // Leere Zeile nach Description
                        tooltipLines.add(Text.empty());
                        
                        // level_info (aqua, nicht kursiv)
                        if (info.level_info != null && !info.level_info.isEmpty()) {
                            for (String levelLine : info.level_info) {
                                if (levelLine != null && !levelLine.isEmpty()) {
                                    tooltipLines.add(Text.literal(levelLine)
                                        .setStyle(Style.EMPTY.withColor(0xFF00FFFF).withItalic(false))); // aqua
                                }
                            }
                        }
                        
                        // first_upgrade (dark_aqua, nicht kursiv)
                        if (info.first_upgrade != null && !info.first_upgrade.isEmpty()) {
                            tooltipLines.add(Text.literal(info.first_upgrade)
                                .setStyle(Style.EMPTY.withColor(0xFF00AAAA).withItalic(false))); // dark_aqua
                        }
                        
                        // Kosten für Power Crystal Slots (nach first_upgrade)
                        appendPriceSection(tooltipLines, hoveredItem, true);
                        
                        // Forschungs Zeit separat anzeigen (gelb Header, dann grün)
                        if (hoveredItem.time != null && hoveredItem.time.hours != null) {
                            // Leere Zeile zwischen Kosten und Forschungs Zeit
                            tooltipLines.add(Text.empty());
                            tooltipLines.add(Text.literal("Forschungs Zeit:")
                                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false))); // yellow
                            
                            // Formatiere Zeit: hoursh
                            String timeAmount = formatAmount(hoveredItem.time.hours);
                            String timeText = timeAmount + "h";
                            tooltipLines.add(Text.literal(timeText)
                                .setStyle(Style.EMPTY.withColor(0xFF55FF55).withItalic(false))); // green
                        }
                        
                        // Leere Zeilen am Ende
                        tooltipLines.add(Text.empty());
                        tooltipLines.add(Text.empty());
                    } else {
                        // Normales Format für Blueprints/Module/etc.
                        
                        // Description (weiß, nicht kursiv) - mit Zeilenumbrüchen unterstützen
                        // Wird für Module/Modulbags/Abilities angezeigt, nicht für Power Crystals (die haben ihr eigenes Format)
                        boolean hasDescOrUpdateInfo = false;
                        if (info.description != null && !info.description.isEmpty()) {
                            String description = info.description;
                            // Unterstütze Zeilenumbrüche (\n)
                            String[] lines = description.split("\n");
                            for (String line : lines) {
                                // Auch leere Zeilen rendern (für Absätze bei \n\n)
                                tooltipLines.add(Text.literal(line)
                                    .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                            }
                            hasDescOrUpdateInfo = true;
                        }
                        
                        // Leere Zeile zwischen Description und Update-Info
                        if (info.description != null && !info.description.isEmpty() && 
                            info.update_info != null) {
                            // Prüfe ob update_info nicht leer ist
                            boolean hasUpdateInfo = false;
                            if (info.update_info instanceof String) {
                                hasUpdateInfo = !((String) info.update_info).isEmpty();
                            } else if (info.update_info instanceof List) {
                                hasUpdateInfo = !((List<?>) info.update_info).isEmpty();
                            }
                            if (hasUpdateInfo) {
                                tooltipLines.add(Text.empty());
                            }
                        }
                        
                        // Update-Info (z.B. für Autoschmelzer) direkt unter der Description, Farbe #55FFFF
                        if (info.update_info != null) {
                            List<String> updateLines = new java.util.ArrayList<>();
                            
                            // Unterstütze sowohl String (mit \n getrennt) als auch List<String>
                            if (info.update_info instanceof String) {
                                String updateInfoStr = (String) info.update_info;
                                if (!updateInfoStr.isEmpty()) {
                                    String[] lines = updateInfoStr.split("\n");
                                    for (String line : lines) {
                                        if (!line.isEmpty()) {
                                            updateLines.add(line);
                                        }
                                    }
                                }
                            } else if (info.update_info instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> updateInfoList = (List<String>) info.update_info;
                                for (String line : updateInfoList) {
                                    if (line != null && !line.isEmpty()) {
                                        updateLines.add(line);
                                    }
                                }
                            }
                            
                            // Rendere alle Zeilen in Aqua-Farbe
                            if (!updateLines.isEmpty()) {
                                for (String line : updateLines) {
                                    tooltipLines.add(Text.literal(line)
                                        .setStyle(Style.EMPTY.withColor(0xFF55FFFF).withItalic(false)));
                                }
                                hasDescOrUpdateInfo = true;
                            }
                        }
                        
                        // Leere Zeile nach Description/Update-Info Block
                        if (hasDescOrUpdateInfo) {
                            tooltipLines.add(Text.empty());
                        }
                        
                        // Typ und Piece (weiß, nicht kursiv)
                        // Format: "type - piece" wenn beide vorhanden, sonst nur das vorhandene
                        boolean hasType = info.type != null && !info.type.isEmpty();
                        boolean hasPiece = info.piece != null && !info.piece.isEmpty();
                        
                        if (hasType || hasPiece) {
                            String typePieceText;
                            if (hasType && hasPiece) {
                                typePieceText = info.type + " - " + info.piece;
                            } else if (hasType) {
                                typePieceText = info.type;
                            } else {
                                typePieceText = info.piece;
                            }
                            tooltipLines.add(Text.literal(typePieceText)
                                .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                        }
                    
                    // Stats (weiß, nicht kursiv)
                    // Unterstützt sowohl String als auch Array von Strings
                    if (info.stats != null) {
                        if (info.stats instanceof String) {
                            String statsStr = (String) info.stats;
                            if (!statsStr.isEmpty()) {
                                tooltipLines.add(Text.literal(statsStr)
                                    .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                            }
                        } else if (info.stats instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> statsList = (List<Object>) info.stats;
                            for (Object statObj : statsList) {
                                if (statObj != null) {
                                    String statLine = statObj.toString();
                                    if (!statLine.isEmpty()) {
                                        tooltipLines.add(Text.literal(statLine)
                                            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                                    }
                                }
                            }
                        }
                    }
                    
                    // Level/Collection (aus foundAt) – bei Angel-Komponenten bereits oben nach dem Namen
                    if (!"fishing_components".equals(hoveredItem.category)) {
                        appendFoundAtLocationLines(tooltipLines, hoveredItem, false);
                    }
                    
                    // Leere Zeile als Trenner (vor Aspekt)
                    tooltipLines.add(Text.empty());
                    
                    // Aspekt (nur wenn aspect == true oder nicht leer)
                    // Der Benutzer möchte "aspect": true/false in der JSON verwenden
                    // Für Rückwärtskompatibilität prüfen wir auch, ob aspect nicht leer ist
                    boolean hasAspect = false;
                    if (info.aspect != null && !info.aspect.isEmpty()) {
                        // Wenn aspect "true" ist oder nicht "false", dann hat es einen Aspekt
                        if (!"false".equalsIgnoreCase(info.aspect)) {
                            hasAspect = true;
                        }
                    }
                    
                    // Aspekt (nur wenn aspect == true, kommt nach Level, vor Modifier)
                    if (hasAspect) {
                        // Hole Aspekt-Info für diesen Blueprint
                        net.felix.utilities.Overall.InformationenUtility.AspectInfo aspectInfo = 
                            net.felix.utilities.Overall.InformationenUtility.getAspectInfoForBlueprintFull(hoveredItem.name);
                        
                        if (aspectInfo != null && aspectInfo.aspectName != null && !aspectInfo.aspectName.isEmpty()) {
                            // "Aspekt: AspektName" - "Aspekt:" in Gelb, AspektName in Orange/Gold (wie in AspectOverlay)
                            MutableText aspectText = Text.literal("Aspekt: ")
                                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)); // yellow
                            aspectText.append(Text.literal(aspectInfo.aspectName)
                                .setStyle(Style.EMPTY.withColor(0xFFFCA800).withItalic(false))); // orange/gold (aggressiver, wie in AspectOverlay)
                            tooltipLines.add(aspectText);
                            
                            // "(Shift für mehr Info)" - ohne Einrückung, hellgrau
                            tooltipLines.add(Text.literal("(Shift für mehr Info)")
                                .setStyle(Style.EMPTY.withColor(0xFFCCCCCC).withItalic(false)));
                            
                            // Leere Zeile nach Aspekt (vor Modifier)
                            tooltipLines.add(Text.empty());
                        }
                    }
                    
                    // Modifier (jeder Modifier-Typ hat seine eigene Farbe, jeder auf eigener Zeile)
                    // Format: ⦁ [ModifierX] - nur ModifierX hat die Farbe, Rest ist weiß
                    // Kommt nach Aspekt, vor Status
                    if (info.modifier != null && !info.modifier.isEmpty()) {
                        boolean fishingComponent = "fishing_components".equals(hoveredItem.category);
                        for (String modifier : info.modifier) {
                            if (modifier != null && !modifier.isEmpty()) {
                                if (fishingComponent) {
                                    tooltipLines.add(buildFishingComponentModifierText(modifier));
                                } else {
                                    int modifierColor = getModifierColor(modifier);
                                    
                                    // Erstelle Text im Format: ⦁ [ModifierX]
                                    MutableText modifierText = Text.literal("⦁")
                                        .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)); // weiß
                                    modifierText.append(Text.literal(" [")
                                        .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false))); // weiß
                                    modifierText.append(Text.literal(modifier)
                                        .setStyle(Style.EMPTY.withColor(modifierColor).withItalic(false))); // Modifier-Farbe
                                    modifierText.append(Text.literal("]")
                                        .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false))); // weiß
                                    
                                    tooltipLines.add(modifierText);
                                }
                            }
                        }
                        // Leere Zeile nach Modifier (vor Status)
                        tooltipLines.add(Text.empty());
                    }
                    
                    // Status (gelb Header, dann grün/rot für gefunden/nicht gefunden)
                    // Nur für Blueprints anzeigen, nicht für Module/Modulbags
                    if (info.blueprint != null && info.blueprint) {
                        boolean isFound = isItemFound(hoveredItem);
                        MutableText statusText = Text.literal("Status:")
                            .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)); // yellow
                        statusText.append(Text.literal(" ")
                            .setStyle(Style.EMPTY.withItalic(false)));
                        if (isFound) {
                            statusText.append(Text.literal("[Gefunden]")
                                .setStyle(Style.EMPTY.withColor(0xFF00FF00).withItalic(false))); // green
                        } else {
                            statusText.append(Text.literal("[Nicht Gefunden]")
                                .setStyle(Style.EMPTY.withColor(0xFFFF5555).withItalic(false))); // red (less aggressive)
                        }
                        tooltipLines.add(statusText);
                        
                        if (ItemViewerUtility.isShowCosts()) {
                            tooltipLines.add(Text.empty());
                        }
                    }
                    
                    appendPriceSection(tooltipLines, hoveredItem, false);
                    appendBlueprintShopSection(tooltipLines, hoveredItem);
                    
                    if (includeInteractionHints) {
                        appendInteractionHints(tooltipLines, hoveredItem, info);
                    }
                    }
                }
                return tooltipLines;
    }
    
    public static void clearItemStackCache() {
        ITEM_STACK_CACHE.clear();
        TEXTURE_ID_CACHE.clear();
        invalidateTooltipCache();
    }
    
    private static final ItemViewerGrid TOOLTIP_BUILDER =
            new ItemViewerGrid(java.util.Collections.emptyList(), 0, 0, 0, 0, 1, 1);

    public static ItemStack createDisplayStack(ItemData itemData) {
        return getCachedItemStack(itemData);
    }

    public static java.util.List<Text> buildTooltipLinesForItem(ItemData itemData) {
        if (itemData == null) {
            return java.util.Collections.emptyList();
        }
        ItemStack stack = createDisplayStack(itemData);
        if (stack.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        MinecraftClient client = MinecraftClient.getInstance();
        return TOOLTIP_BUILDER.buildTooltipLines(itemData, stack, client, false);
    }

    private void appendInteractionHints(java.util.List<Text> tooltipLines, ItemData hoveredItem, ItemInfo info) {
        if (info.blueprint != null && info.blueprint) {
            boolean isFavorite = net.felix.utilities.ItemViewer.FavoriteBlueprintsManager.isFavorite(hoveredItem.name);
            tooltipLines.add(Text.empty());
            if (isFavorite) {
                tooltipLines.add(Text.literal("Aus Favoriten entfernen:")
                    .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)));
                tooltipLines.add(Text.literal("Mit Rechtsklick aus Favoriten entfernen")
                    .setStyle(Style.EMPTY.withColor(0xFFFF5555).withItalic(false)));
            } else {
                tooltipLines.add(Text.literal("Zu Favoriten hinzufügen:")
                    .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)));
                tooltipLines.add(Text.literal("Mit Rechtsklick zu Favoriten hinzufügen")
                    .setStyle(Style.EMPTY.withColor(0xFF16A80C).withItalic(false)));
            }
        }

        if (net.felix.utilities.DragOverlay.ClipboardUtility.isClipboardPinnable(hoveredItem)) {
            String hotkeyText = net.felix.utilities.ItemViewer.ItemViewerUtility.getClipboardPinHotkeyText();
            tooltipLines.add(Text.literal("An Pinnwand anheften mit Hotkey: " + hotkeyText)
                .setStyle(Style.EMPTY.withColor(0xFF808080).withItalic(false)));
        }
    }

    public static void renderItemTooltip(DrawContext context, ItemData itemData, int mouseX, int mouseY) {
        java.util.List<Text> lines = buildTooltipLinesForItem(itemData);
        if (lines.isEmpty()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        context.drawTooltip(client.textRenderer, lines, mouseX, mouseY);
    }

    public static void updateAspectOverlayForItem(ItemData itemData) {
        TOOLTIP_BUILDER.updateItemViewerAspectOverlay(itemData);
    }

    private static ItemStack getCachedItemStack(ItemData itemData) {
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
            var itemEntryOpt = Registries.ITEM.getEntry(itemId);
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
                Text nameText = Text.literal(itemData.name)
                    .setStyle(Style.EMPTY.withColor(nameColor).withItalic(false));
                
                stack.set(DataComponentTypes.CUSTOM_NAME, nameText);
            }
            
            // Setze CustomModelData falls vorhanden
            // CustomModelDataComponent in 1.21.7/1.21.8 verwendet einen 4-Listen-Konstruktor:
            // Liste 0: List<Float> - hier gehört der CustomModelData-Wert (als Float)
            // Liste 1: List<Boolean>
            // Liste 2: List<String>
            // Liste 3: List<Integer>
            if (itemData.customModelData != null) {
                stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                        List.of(itemData.customModelData.floatValue()),
                        List.of(),
                        List.of(),
                        List.of()));
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
    private boolean isPassiveSkillSlot(ItemData itemData) {
        if (itemData == null || itemData.name == null) {
            return false;
        }
        return itemData.name.equals("Passiver Fähigkeits Slot");
    }
    
    /**
     * Prüft ob es sich um einen Karten Slot handelt (basierend auf category)
     */
    private boolean isCardSlot(ItemData itemData) {
        if (itemData == null) {
            return false;
        }
        return "card_slots".equals(itemData.category);
    }
    
    /**
     * Gibt die Farbe für eine Rarity zurück
     */
    private static int getRarityColor(String rarity) {
        if (rarity == null) {
            return 0xFFFFFFFF; // Weiß als Standard
        }
        
        switch (rarity.toLowerCase()) {
            case "common": return 0xFFFFFFFF;      // Weiß
            case "uncommon": return 0xFF25D119;    // #25d119
            case "rare": return 0xFF5555FF;        // #5555FF (gleiche Farbe wie Fähigkeiten-Modifier)
            case "epic": return 0xFF8409DB;        // #8409db
            case "legendary": return 0xFFDE680D;    // #de680d
            default: return 0xFFFFFFFF;            // Weiß als Standard
        }
    }
    
    /**
     * Angel-Komponenten: weißer Text, Zahlen/Vorzeichen in [] in Kosten-Grün.
     * Beispiel: Anbeißgeschwindigkeit: [-10% - 5%]
     */
    private MutableText buildFishingComponentModifierText(String modifier) {
        MutableText text = Text.literal("⦁ ")
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false));
        
        int bracketStart = modifier.indexOf('[');
        int bracketEnd = modifier.lastIndexOf(']');
        if (bracketStart < 0 || bracketEnd <= bracketStart) {
            text.append(Text.literal(modifier)
                .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
            return text;
        }
        
        text.append(Text.literal(modifier.substring(0, bracketStart + 1))
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
        appendFishingModifierBracketContent(text, modifier.substring(bracketStart + 1, bracketEnd));
        text.append(Text.literal(modifier.substring(bracketEnd))
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
        return text;
    }
    
    private void appendFishingModifierBracketContent(MutableText text, String inside) {
        Matcher matcher = FISHING_MODIFIER_VALUE.matcher(inside);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                text.append(Text.literal(inside.substring(lastEnd, matcher.start()))
                    .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
            }
            text.append(Text.literal(matcher.group())
                .setStyle(Style.EMPTY.withColor(COST_GREEN).withItalic(false)));
            lastEnd = matcher.end();
        }
        if (lastEnd < inside.length()) {
            text.append(Text.literal(inside.substring(lastEnd))
                .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
        }
    }
    
    /**
     * Gibt die Farbe für einen Modifier zurück
     * Jeder Modifier-Typ hat seine eigene Farbe für bessere Erkennbarkeit
     * Farbcodes basierend auf Minecraft-Farbcodes
     */
    private int getModifierColor(String modifier) {
        if (modifier == null || modifier.isEmpty()) {
            return 0xFFFFFFFF; // Weiß als Standard
        }
        
        // Normalisiere den Modifier-String (lowercase, trim)
        String normalizedModifier = modifier.toLowerCase().trim();
        
        // Prüfe auf bekannte Modifier-Kategorien und gebe entsprechende Farbe zurück
        // Attribute: #00AAAA (Cyan/Aqua)
        if (normalizedModifier.contains("attribute")) {
            return 0xFF00AAAA; // #00AAAA
        }
        // Verteidigung: #FFAA00 (Orange/Gold)
        else if (normalizedModifier.contains("verteidigung") || normalizedModifier.contains("defense") || 
                 normalizedModifier.contains("defence") || normalizedModifier.contains("schutz")) {
            return 0xFFFFAA00; // #FFAA00
        }
        // Fähigkeiten: #5555FF (Blau)
        else if (normalizedModifier.contains("fähigkeit") || normalizedModifier.contains("ability") || 
                 normalizedModifier.contains("skill")) {
            return 0xFF5555FF; // #5555FF
        }
        // Schaden: #FF5555 (Rot)
        else if (normalizedModifier.contains("schaden") || normalizedModifier.contains("damage") || 
                 normalizedModifier.contains("angriff") || normalizedModifier.contains("attack")) {
            return 0xFFFF5555; // #FF5555
        }
        // Herstellung: #FFFF55 (Gelb)
        else if (normalizedModifier.contains("herstellung") || normalizedModifier.contains("crafting") || 
                 normalizedModifier.contains("craft") || normalizedModifier.contains("bau")) {
            return 0xFFFFFF55; // #FFFF55
        }
        // Andere: #FF55FF (Magenta) - Fallback für alle anderen Modifier
        else {
            return 0xFFFF55FF; // #FF55FF
        }
    }
    
    /**
     * Fügt eine Kosten-Zeile zum Tooltip hinzu, falls vorhanden
     */
    private void appendPriceSection(java.util.List<Text> tooltipLines, ItemData hoveredItem, boolean leadingEmptyLine) {
        if (!ItemViewerUtility.isShowCosts() || hoveredItem == null || hoveredItem.price == null) {
            return;
        }
        if (leadingEmptyLine) {
            tooltipLines.add(Text.empty());
        }
        addCostSectionHeader(tooltipLines, "Kosten:", hoveredItem.price, hoveredItem);
        addCostItem(tooltipLines, hoveredItem.price.coin);
        addCostItem(tooltipLines, hoveredItem.price.cactus);
        addCostItem(tooltipLines, hoveredItem.price.soul);
        addCostItem(tooltipLines, hoveredItem.price.material1);
        addCostItem(tooltipLines, hoveredItem.price.material2);
        addCostItem(tooltipLines, hoveredItem.price.material3);
        addCostItem(tooltipLines, hoveredItem.price.material4);
        addCostItem(tooltipLines, hoveredItem.price.material5);
        addCostItem(tooltipLines, hoveredItem.price.Amboss != null ? hoveredItem.price.Amboss : hoveredItem.price.amboss);
        addCostItem(tooltipLines, hoveredItem.price.Ressource != null ? hoveredItem.price.Ressource : hoveredItem.price.ressource);
        addLevelCostItem(tooltipLines, hoveredItem.price.Level);
    }

    private void appendBlueprintShopSection(java.util.List<Text> tooltipLines, ItemData hoveredItem) {
        if (!ItemViewerUtility.isShowCosts() || hoveredItem == null
                || hoveredItem.blueprint_shop == null || hoveredItem.blueprint_shop.price == null) {
            return;
        }
        tooltipLines.add(Text.empty());
        tooltipLines.add(Text.literal("Bauplan-Shop:")
                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)));
        PriceData shopPrice = hoveredItem.blueprint_shop.price;
        addCostItem(tooltipLines, shopPrice.coin);
        addCostItem(tooltipLines, shopPrice.cactus);
        addCostItem(tooltipLines, shopPrice.soul);
        addCostItem(tooltipLines, shopPrice.material1);
        addCostItem(tooltipLines, shopPrice.material2);
        addCostItem(tooltipLines, shopPrice.material3);
        addCostItem(tooltipLines, shopPrice.material4);
        addCostItem(tooltipLines, shopPrice.material5);
        addCostItem(tooltipLines, shopPrice.Amboss != null ? shopPrice.Amboss : shopPrice.amboss);
        addCostItem(tooltipLines, shopPrice.Ressource != null ? shopPrice.Ressource : shopPrice.ressource);
        addCostItem(tooltipLines, shopPrice.paper_shreds);
    }

    private void addCostSectionHeader(java.util.List<Text> tooltipLines, String label, PriceData price, ItemData item) {
        if (!ItemViewerUtility.isShowOwnedResources() || !shouldShowCraftableCount(item)) {
            tooltipLines.add(Text.literal(label)
                    .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)));
            return;
        }
        int craftable = ItemViewerOwnedResources.calculateCraftableCount(price);
        if (craftable < 0) {
            tooltipLines.add(Text.literal(label)
                    .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)));
            return;
        }
        MutableText header = Text.literal(label)
                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false))
                .append(Text.literal("     " + craftable + "x Herstellbar")
                        .setStyle(Style.EMPTY.withColor(ITEM_SCORE_VALUE_COLOR).withItalic(false)));
        tooltipLines.add(header);
    }

    private static boolean shouldShowCraftableCount(ItemData item) {
        if (item == null) {
            return true;
        }
        if (item.category != null) {
            String category = item.category.toLowerCase(java.util.Locale.ROOT);
            if ("modules".equals(category) || "module_bags".equals(category) || "licence".equals(category)) {
                return false;
            }
        }
        if (item.info != null) {
            if (Boolean.TRUE.equals(item.info.module) || Boolean.TRUE.equals(item.info.licence)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fügt eine Kosten-Zeile zum Tooltip hinzu, falls vorhanden
     */
    private void addCostItem(java.util.List<Text> tooltipLines, CostItem costItem) {
        if (costItem == null || costItem.itemName == null || costItem.amount == null) {
            return;
        }

        if (ItemViewerUtility.isShowOwnedResources()) {
            addCostItemWithOwned(tooltipLines, costItem);
            return;
        }

        String amountStr = formatAmount(costItem.amount);
        String materialLine = amountStr + " " + costItem.itemName;
        
        // Prüfe ob es ein Aincraft-Material ist und füge Ebene hinzu
        net.felix.utilities.Overall.InformationenUtility.MaterialFloorInfo floorInfo = 
            net.felix.utilities.Overall.InformationenUtility.getMaterialFloorInfo(costItem.itemName);
        
        if (floorInfo != null) {
            MutableText materialText = Text.literal(materialLine)
                .setStyle(Style.EMPTY.withColor(0xFF55FF55).withItalic(false));
            
            String floorText = net.felix.utilities.Overall.InformationenUtility.formatMaterialLocationSuffix(floorInfo);
            int rarityColor = net.felix.utilities.Overall.InformationenUtility.getMaterialLocationRarityColorArgb(floorInfo);
            Text floorTextObj = Text.literal(floorText)
                .setStyle(Style.EMPTY.withColor(rarityColor).withItalic(false));
            
            tooltipLines.add(materialText.append(floorTextObj));
        } else {
            tooltipLines.add(Text.literal(materialLine)
                .setStyle(Style.EMPTY.withColor(0xFF55FF55).withItalic(false)));
        }
    }

    private void addCostItemWithOwned(java.util.List<Text> tooltipLines, CostItem costItem) {
        java.math.BigDecimal owned = ItemViewerOwnedResources.getOwnedAmount(costItem.itemName);
        java.math.BigDecimal needed = ItemViewerOwnedResources.parseNeededAmount(costItem.amount);
        int lineColor;
        if (needed.signum() == 0) {
            lineColor = 0xFF00FF00;
        } else if (owned.compareTo(needed) >= 0) {
            lineColor = 0xFF00FF00;
        } else {
            lineColor = 0xFFFF5555;
        }

        String ownedStr = ItemViewerOwnedResources.formatOwnedAmountForDisplay(costItem.itemName);
        String neededStr = ItemViewerOwnedResources.formatNeededAmountForDisplay(costItem.itemName, costItem.amount);
        String materialLine = ownedStr + " / " + neededStr + " " + costItem.itemName;

        net.felix.utilities.Overall.InformationenUtility.MaterialFloorInfo floorInfo =
            net.felix.utilities.Overall.InformationenUtility.getMaterialFloorInfo(costItem.itemName);
        if (floorInfo != null) {
            MutableText materialText = Text.literal(materialLine)
                .setStyle(Style.EMPTY.withColor(lineColor).withItalic(false));
            String floorText = net.felix.utilities.Overall.InformationenUtility.formatMaterialLocationSuffix(floorInfo);
            int rarityColor = net.felix.utilities.Overall.InformationenUtility.getMaterialLocationRarityColorArgb(floorInfo);
            materialText.append(Text.literal(floorText)
                .setStyle(Style.EMPTY.withColor(rarityColor).withItalic(false)));
            tooltipLines.add(materialText);
        } else {
            tooltipLines.add(Text.literal(materialLine)
                .setStyle(Style.EMPTY.withColor(lineColor).withItalic(false)));
        }
    }
    
    /**
     * Fügt eine Level-Kosten-Zeile zum Tooltip hinzu (Zahl in gelb)
     */
    private void addLevelCostItem(java.util.List<Text> tooltipLines, CostItem costItem) {
        if (costItem != null && costItem.itemName != null && costItem.amount != null) {
            String amountStr = formatAmount(costItem.amount);
            
            // Erstelle Text mit Zahl in gelb und Item-Name in grün
            String amountPart = amountStr + " ";
            String namePart = costItem.itemName;
            
            MutableText levelText = Text.literal(amountPart)
                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)) // Gelb für Zahl
                .append(Text.literal(namePart)
                    .setStyle(Style.EMPTY.withColor(0xFF55FF55).withItalic(false))); // Grün für Name
            
            tooltipLines.add(levelText);
        }
    }
    
    /**
     * Formatiert einen Amount-Wert: Ganze Zahlen ohne ".0", andere bleiben wie sie sind
     */
    private String formatAmount(Object amount) {
        if (amount == null) {
            return "0";
        }
        
        // Wenn es bereits ein String ist, gib ihn zurück
        if (amount instanceof String) {
            return (String) amount;
        }
        
        // Wenn es eine Zahl ist, prüfe ob es eine ganze Zahl ist
        if (amount instanceof Number) {
            Number num = (Number) amount;
            double doubleValue = num.doubleValue();
            
            // Prüfe ob es eine ganze Zahl ist (ohne Dezimalstellen)
            if (doubleValue == Math.floor(doubleValue)) {
                // Ganze Zahl: ohne Dezimalstellen anzeigen
                return String.valueOf(num.longValue());
            } else {
                // Zahl mit Dezimalstellen: so anzeigen wie sie ist
                return String.valueOf(doubleValue);
            }
        }
        
        // Fallback: toString()
        return amount.toString();
    }
    
    private static final int FISH_TRAP_LABEL_GREEN = 0xFF55FF55;
    private static final int FISH_TRAP_BLUEPRINT_BLUE = 0xFF5555FF;
    private static final int[] SHINY_LETTER_COLORS = {
        0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF
    };

    private void appendFoundAtLocationLines(java.util.List<Text> tooltipLines, ItemData hoveredItem) {
        appendFoundAtLocationLines(tooltipLines, hoveredItem, true);
    }

    private static final int ITEM_SCORE_VALUE_COLOR = 0xFF54FCFC;

    private void appendFoundAtLocationLines(java.util.List<Text> tooltipLines, ItemData hoveredItem, boolean trailingEmptyLine) {
        boolean isBlueprint = hoveredItem.info != null && Boolean.TRUE.equals(hoveredItem.info.blueprint);
        Text itemScoreText = isBlueprint ? buildItemScoreText(hoveredItem.itemScore) : null;
        Text forgingConditionText = isBlueprint
                ? buildForgingConditionText(resolveForgingCondition(hoveredItem)) : null;
        boolean hasLocation = hoveredItem.foundAt != null && !hoveredItem.foundAt.isEmpty();
        String locationLine = null;

        if (hasLocation) {
            LocationData firstLocation = hoveredItem.foundAt.get(0);
            if (firstLocation.floor != null && !firstLocation.floor.isEmpty()) {
                locationLine = firstLocation.floor;
            } else if (firstLocation.collection != null && !firstLocation.collection.isEmpty()) {
                locationLine = firstLocation.collection;
            }
        }

        if (itemScoreText == null && forgingConditionText == null && locationLine == null) {
            return;
        }

        if (itemScoreText != null) {
            tooltipLines.add(Text.empty());
            tooltipLines.add(itemScoreText);
        }
        if (forgingConditionText != null) {
            tooltipLines.add(forgingConditionText);
        }
        if (locationLine != null) {
            tooltipLines.add(Text.literal(locationLine)
                .setStyle(Style.EMPTY.withColor(0xFFFF00FF).withItalic(false)));
        }
        if (trailingEmptyLine) {
            tooltipLines.add(Text.empty());
        }
    }

    private static String resolveForgingCondition(ItemData item) {
        if (item == null) {
            return null;
        }
        if (item.forgingCondition != null && !item.forgingCondition.isBlank()) {
            return item.forgingCondition;
        }
        return ForgingConditionUtility.resolveMaxForgingCondition(item.itemScore);
    }

    private static Text buildForgingConditionText(String forgingCondition) {
        if (forgingCondition == null || forgingCondition.isBlank()) {
            return null;
        }

        int conditionColor = ForgingConditionUtility.getDisplayColorArgb(forgingCondition);
        MutableText text = Text.literal("[")
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false));
        text.append(Text.literal(forgingCondition)
            .setStyle(Style.EMPTY.withColor(conditionColor).withItalic(false)));
        text.append(Text.literal("] möglich")
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
        return text;
    }

    private static Text buildItemScoreText(String itemScore) {
        String value = formatItemScoreValue(itemScore);
        if (value == null) {
            return null;
        }

        MutableText text = Text.literal("[")
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false));
        text.append(Text.literal("ItemScore")
            .setStyle(Style.EMPTY.withColor(ITEM_SCORE_VALUE_COLOR).withItalic(false)));
        text.append(Text.literal("] ")
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
        text.append(Text.literal(value)
            .setStyle(Style.EMPTY.withColor(ITEM_SCORE_VALUE_COLOR).withItalic(false)));
        return text;
    }

    private static String formatItemScoreValue(String itemScore) {
        if (itemScore == null || itemScore.isBlank()) {
            return null;
        }
        if ("NaN".equalsIgnoreCase(itemScore.trim())) {
            return "NaN";
        }
        try {
            double value = Double.parseDouble(itemScore.trim().replace(',', '.'));
            if (Double.isNaN(value)) {
                return "NaN";
            }
            if (value == Math.rint(value)) {
                return String.valueOf((long) value);
            }
            String formatted = String.valueOf(value);
            if (formatted.contains(".") && formatted.endsWith("0")) {
                formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
            }
            return formatted;
        } catch (NumberFormatException ignored) {
            return itemScore.trim();
        }
    }

    private void appendFishTrapTooltipLines(java.util.List<Text> tooltipLines, ItemData hoveredItem, ItemInfo info,
            boolean includeInteractionHints) {
        appendFoundAtLocationLines(tooltipLines, hoveredItem);

        if (info.catch_time != null && !info.catch_time.isEmpty()) {
            MutableText catchTime = Text.literal("Fangzeit: ")
                .setStyle(Style.EMPTY.withColor(FISH_TRAP_LABEL_GREEN).withItalic(false));
            catchTime.append(Text.literal(info.catch_time)
                .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
            tooltipLines.add(catchTime);
        }

        if (info.capacity != null) {
            MutableText capacity = Text.literal("Kapazität: ")
                .setStyle(Style.EMPTY.withColor(FISH_TRAP_LABEL_GREEN).withItalic(false));
            capacity.append(Text.literal(String.valueOf(info.capacity))
                .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
            tooltipLines.add(capacity);
        }

        if (info.catch_chances != null && !info.catch_chances.isEmpty()) {
            tooltipLines.add(Text.empty());
            tooltipLines.add(Text.literal("Fangchancen:")
                .setStyle(Style.EMPTY.withColor(FISH_TRAP_LABEL_GREEN).withItalic(false)));
            for (CatchChance chance : info.catch_chances) {
                if (chance == null || chance.label == null || chance.label.isEmpty()) {
                    continue;
                }
                tooltipLines.add(buildFishTrapCatchChanceLine(chance));
            }
        }

        if (info.blueprint != null && info.blueprint) {
            tooltipLines.add(Text.empty());
            boolean isFound = isItemFound(hoveredItem);
            MutableText statusText = Text.literal("Status:")
                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false));
            statusText.append(Text.literal(" ")
                .setStyle(Style.EMPTY.withItalic(false)));
            if (isFound) {
                statusText.append(Text.literal("[Gefunden]")
                    .setStyle(Style.EMPTY.withColor(0xFF00FF00).withItalic(false)));
            } else {
                statusText.append(Text.literal("[Nicht Gefunden]")
                    .setStyle(Style.EMPTY.withColor(0xFFFF5555).withItalic(false)));
            }
            tooltipLines.add(statusText);
        }

        appendPriceSection(tooltipLines, hoveredItem, true);

        if (info.prerequisites != null && !info.prerequisites.isEmpty()) {
            tooltipLines.add(Text.empty());
            tooltipLines.add(Text.literal("Voraussetzungen:")
                .setStyle(Style.EMPTY.withColor(FISH_TRAP_LABEL_GREEN).withItalic(false)));
            for (String prerequisite : info.prerequisites) {
                if (prerequisite == null || prerequisite.isEmpty()) {
                    continue;
                }
                MutableText prereqLine = Text.literal("  • ")
                    .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false));
                prereqLine.append(Text.literal(prerequisite)
                    .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                prereqLine.append(Text.literal(" [Bauplan]")
                    .setStyle(Style.EMPTY.withColor(FISH_TRAP_BLUEPRINT_BLUE).withItalic(false)));
                tooltipLines.add(prereqLine);
            }
        }

        if (includeInteractionHints) {
            appendInteractionHints(tooltipLines, hoveredItem, info);
        }
    }

    private MutableText buildFishTrapCatchChanceLine(CatchChance chance) {
        MutableText line = Text.literal("  • ")
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false));
        if ("shiny".equalsIgnoreCase(chance.rarity)) {
            line.append(buildShinyRarityLabel(chance.label));
        } else {
            line.append(Text.literal(chance.label)
                .setStyle(Style.EMPTY.withColor(getFishTrapCatchRarityColor(chance.rarity)).withItalic(false)));
        }
        String percent = chance.percent != null ? chance.percent : "";
        line.append(Text.literal(": " + percent)
            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
        return line;
    }

    private MutableText buildShinyRarityLabel(String label) {
        MutableText shinyText = Text.empty();
        String text = label != null ? label : "Shiny";
        for (int i = 0; i < text.length(); i++) {
            int color = i < SHINY_LETTER_COLORS.length
                ? SHINY_LETTER_COLORS[i]
                : SHINY_LETTER_COLORS[SHINY_LETTER_COLORS.length - 1];
            shinyText.append(Text.literal(String.valueOf(text.charAt(i)))
                .setStyle(Style.EMPTY.withColor(color).withItalic(false)));
        }
        return shinyText;
    }

    private int getFishTrapCatchRarityColor(String rarity) {
        if (rarity == null) {
            return 0xFFFFFFFF;
        }
        return switch (rarity.toLowerCase()) {
            case "uncommon" -> 0xFF55FF55;
            case "rare" -> 0xFF5555FF;
            case "epic" -> 0xFFAA00AA;
            case "legendary" -> 0xFFFFAA00;
            default -> 0xFFFFFFFF;
        };
    }

    private boolean isItemFound(ItemData item) {
        return ItemViewerFoundUtility.isFound(item);
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


package net.felix.utilities.ItemViewer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;

import java.util.List;

/**
 * Rendert Items in einem Grid-Layout (ähnlich JEI)
 */
public class ItemViewerGrid {
    
    private static final int SLOT_SIZE = 18; // 16x16 Item + 1px Padding auf jeder Seite
    private static final Identifier PASSIVE_SKILL_SLOT_TEXTURE = Identifier.of("cclive-utilities", "textures/icons/passive_skill_slot.png");
    private static final int DEFAULT_GRID_ROWS = 8; // Standard: 8 Zeilen (für Fallback)
    private static final int DEFAULT_GRID_COLUMNS = 6; // Standard: 6 Spalten (für Fallback)
    
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
        // Berechne Spaltenanzahl basierend auf verfügbarer Breite
        this.gridColumns = Math.max(1, availableWidth / SLOT_SIZE);
        // Berechne Zeilenanzahl basierend auf verfügbarer Höhe
        this.gridRows = Math.max(1, availableHeight / SLOT_SIZE);
    }
    
    /**
     * Rendert das Grid mit allen Items
     */
    public void render(DrawContext context) {
        int itemsPerPage = gridColumns * gridRows;
        // Rendere alle Slots
        for (int i = 0; i < Math.min(items.size(), itemsPerPage); i++) {
            int row = i / gridColumns;
            int col = i % gridColumns;
            
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
            
            // Prüfe ob es ein "Passiver Fähigkeits Slot" ist
            if (isPassiveSkillSlot(item)) {
                // Rendere spezielle Textur für Passiven Fähigkeits Slot
                context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    PASSIVE_SKILL_SLOT_TEXTURE,
                    slotX + 1, slotY + 1,
                    0, 0,
                    16, 16,
                    16, 16
                );
            } else {
                // Rendere normales Item
                ItemStack itemStack = createItemStack(item);
                if (!itemStack.isEmpty()) {
                    context.drawItem(itemStack, slotX + 1, slotY + 1, 0);
                }
            }
        }
    }
    
    /**
     * Rendert Tooltip für das gehoverte Item
     */
    public void renderTooltip(DrawContext context) {
        if (hoveredItem != null) {
            // Verstecke AspectOverlay wenn kein Item gehovered wird (wird später wieder aktiviert wenn Aspekt vorhanden)
            net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
            MinecraftClient client = MinecraftClient.getInstance();
            ItemStack itemStack = createItemStack(hoveredItem);
            if (!itemStack.isEmpty()) {
                // Erstelle Tooltip-Liste mit zusätzlichen Informationen
                java.util.List<Text> tooltipLines = new java.util.ArrayList<>();
                
                // Standard Item Tooltip (Name, etc.) - verwende Reflection für getTooltipFromItem
                try {
                    // Versuche, getTooltipFromItem über HandledScreen zu bekommen
                    // Da wir nicht in einem HandledScreen sind, verwenden wir einen einfacheren Ansatz
                    // Füge den Item-Namen hinzu
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
                
                // Zusätzliche Informationen aus ItemInfo und anderen Daten
                if (hoveredItem.info != null) {
                    ItemInfo info = hoveredItem.info;
                    
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
                        if (hoveredItem.price != null) {
                            tooltipLines.add(Text.empty());
                            tooltipLines.add(Text.literal("Kosten:")
                                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false))); // yellow
                            
                            // Füge alle Kosten hinzu
                            addCostItem(tooltipLines, hoveredItem.price.coin);
                            addCostItem(tooltipLines, hoveredItem.price.cactus);
                            addCostItem(tooltipLines, hoveredItem.price.soul);
                            addCostItem(tooltipLines, hoveredItem.price.material1);
                            addCostItem(tooltipLines, hoveredItem.price.material2);
                            addCostItem(tooltipLines, hoveredItem.price.Amboss);
                            addCostItem(tooltipLines, hoveredItem.price.Ressource);
                        }
                        
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
                    
                    // Level/Collection (aus foundAt, purple/magenta, nicht kursiv)
                    // Zeigt floor für Baupläne oder collection für Module/Modultaschen
                    if (hoveredItem.foundAt != null && !hoveredItem.foundAt.isEmpty()) {
                        LocationData firstLocation = hoveredItem.foundAt.get(0);
                        if (firstLocation.floor != null && !firstLocation.floor.isEmpty()) {
                            tooltipLines.add(Text.literal(firstLocation.floor)
                                .setStyle(Style.EMPTY.withColor(0xFFFF00FF).withItalic(false))); // purple/magenta (alte Epic-Farbe)
                        } else if (firstLocation.collection != null && !firstLocation.collection.isEmpty()) {
                            tooltipLines.add(Text.literal(firstLocation.collection)
                                .setStyle(Style.EMPTY.withColor(0xFFFF00FF).withItalic(false))); // purple/magenta (gleiche Farbe wie floor)
                        }
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
                            
                            // Update AspectOverlay für Shift-Funktion
                            net.felix.utilities.Overall.Aspekte.AspectOverlay.updateAspectInfoFromName(hoveredItem.name, aspectInfo);
                            
                            // Leere Zeile nach Aspekt (vor Modifier)
                            tooltipLines.add(Text.empty());
                        }
                    }
                    
                    // Modifier (jeder Modifier-Typ hat seine eigene Farbe, jeder auf eigener Zeile)
                    // Format: ⦁ [ModifierX] - nur ModifierX hat die Farbe, Rest ist weiß
                    // Kommt nach Aspekt, vor Status
                    if (info.modifier != null && !info.modifier.isEmpty()) {
                        for (String modifier : info.modifier) {
                            if (modifier != null && !modifier.isEmpty()) {
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
                        // Leere Zeile nach Modifier (vor Status)
                        tooltipLines.add(Text.empty());
                    }
                    
                    // Status (gelb Header, dann grün/rot für gefunden/nicht gefunden)
                    // Nur für Blueprints anzeigen, nicht für Module/Modulbags
                    if (info.blueprint != null && info.blueprint) {
                        boolean isFound = isBlueprintFound(hoveredItem.name);
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
                        
                        // Leere Zeile als Trenner
                        tooltipLines.add(Text.empty());
                    }
                    
                    // Kosten (yellow Header, dann green Preise)
                    if (hoveredItem.price != null) {
                        tooltipLines.add(Text.literal("Kosten:")
                            .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false))); // yellow
                        
                        // Füge alle Kosten hinzu
                        addCostItem(tooltipLines, hoveredItem.price.coin);
                        addCostItem(tooltipLines, hoveredItem.price.cactus);
                        addCostItem(tooltipLines, hoveredItem.price.soul);
                        addCostItem(tooltipLines, hoveredItem.price.material1);
                        addCostItem(tooltipLines, hoveredItem.price.material2);
                        addCostItem(tooltipLines, hoveredItem.price.Amboss);
                        addCostItem(tooltipLines, hoveredItem.price.Ressource);
                    }
                    
                    // Bauplan-Shop (yellow Header, dann green Preise)
                    if (hoveredItem.blueprint_shop != null && hoveredItem.blueprint_shop.price != null) {
                        tooltipLines.add(Text.empty());
                        tooltipLines.add(Text.literal("Bauplan-Shop:")
                            .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false))); // yellow
                        
                        // Füge alle Bauplan-Shop Preise hinzu
                        addCostItem(tooltipLines, hoveredItem.blueprint_shop.price.coin);
                        addCostItem(tooltipLines, hoveredItem.blueprint_shop.price.cactus);
                        addCostItem(tooltipLines, hoveredItem.blueprint_shop.price.soul);
                        addCostItem(tooltipLines, hoveredItem.blueprint_shop.price.material1);
                        addCostItem(tooltipLines, hoveredItem.blueprint_shop.price.material2);
                        addCostItem(tooltipLines, hoveredItem.blueprint_shop.price.Amboss);
                        addCostItem(tooltipLines, hoveredItem.blueprint_shop.price.Ressource);
                        addCostItem(tooltipLines, hoveredItem.blueprint_shop.price.paper_shreds);
                    }
                    
                    // Favoriten-Information (ganz unten im Tooltip, nach Bauplan-Shop)
                    // Nur für Blueprints anzeigen
                    if (info.blueprint != null && info.blueprint) {
                        boolean isFavorite = net.felix.utilities.ItemViewer.FavoriteBlueprintsManager.isFavorite(hoveredItem.name);
                        if (isFavorite) {
                            // In Favoriten: "Aus Favoriten entfernen:" (gelb) + "Mit Rechtsklick aus Favoriten entfernen" (rot)
                            tooltipLines.add(Text.empty());
                            tooltipLines.add(Text.literal("Aus Favoriten entfernen:")
                                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false))); // yellow
                            tooltipLines.add(Text.literal("Mit Rechtsklick aus Favoriten entfernen")
                                .setStyle(Style.EMPTY.withColor(0xFFFF5555).withItalic(false))); // rot (gleiche Farbe wie "[Nicht Gefunden]")
                        } else {
                            // Nicht in Favoriten: "Zu Favoriten hinzufügen:" (gelb) + "Mit Rechtsklick zu Favoriten hinzufügen" (grün)
                            tooltipLines.add(Text.empty());
                            tooltipLines.add(Text.literal("Zu Favoriten hinzufügen:")
                                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false))); // yellow
                            tooltipLines.add(Text.literal("Mit Rechtsklick zu Favoriten hinzufügen")
                                .setStyle(Style.EMPTY.withColor(0xFF16A80C).withItalic(false))); // uncommon grün #16a80c (dunkleres grün)
                        }
                        
                        // Bauplan Anpinnen Hotkey-Information (nach Favoriten, in grau)
                        // Keine leere Zeile davor, da es die letzte Zeile sein soll
                        String hotkeyText = net.felix.utilities.ItemViewer.ItemViewerUtility.getClipboardPinHotkeyText();
                        tooltipLines.add(Text.literal("Bauplan Anpinnen mit Hotkey: " + hotkeyText)
                            .setStyle(Style.EMPTY.withColor(0xFF808080).withItalic(false))); // grau
                    }
                    }
                }
                
                // Rendere Custom Tooltip
                context.drawTooltip(client.textRenderer, tooltipLines, mouseX, mouseY);
            } else {
                // Kein Item gehovered - verstecke AspectOverlay
                net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
            }
        } else {
            // Kein Item gehovered - verstecke AspectOverlay
            net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
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
            
            // Setze Custom Name falls vorhanden
            if (itemData.name != null && !itemData.name.isEmpty()) {
                int nameColor;
                // Power Crystals haben immer aqua als Name-Farbe
                if (itemData.info != null && itemData.info.power_crystal != null && itemData.info.power_crystal) {
                    nameColor = 0xFF00FFFF; // aqua
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
                try {
                    java.lang.reflect.Constructor<?>[] constructors = 
                        net.minecraft.component.type.CustomModelDataComponent.class.getDeclaredConstructors();
                    
                    // Finde den 4-Listen-Konstruktor
                    java.lang.reflect.Constructor<?> listConstructor = null;
                    for (java.lang.reflect.Constructor<?> c : constructors) {
                        if (c.getParameterCount() == 4) {
                            listConstructor = c;
                            break;
                        }
                    }
                    
                    if (listConstructor != null) {
                        listConstructor.setAccessible(true);
                        java.util.List<?> emptyList = java.util.Collections.emptyList();
                        
                        // CustomModelData-Wert als Float in die erste Liste (Liste 0)
                        java.util.List<Float> floatList = new java.util.ArrayList<>();
                        floatList.add(itemData.customModelData.floatValue());
                        
                        var customModelData = (net.minecraft.component.type.CustomModelDataComponent) 
                            listConstructor.newInstance(floatList, emptyList, emptyList, emptyList);
                        
                        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, customModelData);
                    }
                } catch (Exception e) {
                    // Fehler beim Setzen von CustomModelData - ignoriere stillschweigend
                    // Das Item wird ohne CustomModelData gerendert
                }
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
     * Gibt die Farbe für eine Rarity zurück
     */
    private int getRarityColor(String rarity) {
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
    private void addCostItem(java.util.List<Text> tooltipLines, CostItem costItem) {
        if (costItem != null && costItem.itemName != null && costItem.amount != null) {
            String amountStr = formatAmount(costItem.amount);
            String materialLine = amountStr + " " + costItem.itemName;
            
            // Prüfe ob es ein Aincraft-Material ist und füge Ebene hinzu
            net.felix.utilities.Overall.InformationenUtility.MaterialFloorInfo floorInfo = 
                net.felix.utilities.Overall.InformationenUtility.getMaterialFloorInfo(costItem.itemName);
            
            if (floorInfo != null) {
                // Erstelle Text mit Material-Name in grün und "(Ebene X)" in Rarity-Farbe
                MutableText materialText = Text.literal(materialLine)
                    .setStyle(Style.EMPTY.withColor(0xFF55FF55).withItalic(false)); // Grün für Material
                
                String floorText = " (Ebene " + floorInfo.floor + ")";
                int rarityColor = getMaterialRarityColor(floorInfo.rarity);
                Text floorTextObj = Text.literal(floorText)
                    .setStyle(Style.EMPTY.withColor(rarityColor).withItalic(false)); // Rarity-Farbe für "(Ebene X)"
                
                // Kombiniere beide Texte
                tooltipLines.add(materialText.append(floorTextObj));
            } else {
                // Normale Anzeige ohne Ebene
                tooltipLines.add(Text.literal(materialLine)
                    .setStyle(Style.EMPTY.withColor(0xFF55FF55).withItalic(false)));
            }
        }
    }
    
    /**
     * Gets the color for a material rarity level (same as InformationenUtility.getRarityColor)
     */
    private int getMaterialRarityColor(String rarity) {
        if (rarity == null) {
            return 0xFF55FF55; // Default green
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
    
    /**
     * Prüft ob ein Bauplan gefunden wurde
     */
    private boolean isBlueprintFound(String blueprintName) {
        if (blueprintName == null || blueprintName.isEmpty()) {
            return false;
        }
        
        try {
            net.felix.utilities.Aincraft.BPViewerUtility bpViewer = 
                net.felix.utilities.Aincraft.BPViewerUtility.getInstance();
            
            // Prüfe foundBlueprints (ähnlich wie in BPViewerUtility.isBlueprintFoundAnywhere)
            // Für Drachenzahn: prüfe beide Rarities separat
            if (blueprintName.equals("Drachenzahn")) {
                return bpViewer.isBlueprintFoundAnywhere(blueprintName + ":epic") ||
                       bpViewer.isBlueprintFoundAnywhere(blueprintName + ":legendary");
            } else {
                // Für andere Blueprints: prüfe mit und ohne Rarity-Suffix
                return bpViewer.isBlueprintFoundAnywhere(blueprintName) ||
                       bpViewer.isBlueprintFoundAnywhere(blueprintName + ":epic") ||
                       bpViewer.isBlueprintFoundAnywhere(blueprintName + ":legendary");
            }
        } catch (Exception e) {
            // Falls BPViewerUtility nicht verfügbar ist
            return false;
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
        return gridColumns * gridRows;
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


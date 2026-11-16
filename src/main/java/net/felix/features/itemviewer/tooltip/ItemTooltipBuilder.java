package net.felix.features.itemviewer.tooltip;

import java.util.List;
import net.felix.features.itemviewer.data.ItemData;
import net.felix.features.itemviewer.data.ItemInfo;
import net.felix.features.itemviewer.ui.ViewerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import static net.felix.features.itemviewer.tooltip.CostTooltipSections.appendBlueprintShopSection;
import static net.felix.features.itemviewer.tooltip.CostTooltipSections.appendPriceSection;
import static net.felix.features.itemviewer.tooltip.CostTooltipSections.formatAmount;
import static net.felix.features.itemviewer.tooltip.CostTooltipSections.isItemFound;
import static net.felix.features.itemviewer.tooltip.FishingTooltipSections.appendFishTrapTooltipLines;
import static net.felix.features.itemviewer.tooltip.FishingTooltipSections.buildFishingComponentModifierText;
import static net.felix.features.itemviewer.tooltip.FoundAtTooltipSection.appendFoundAtLocationLines;
import static net.felix.features.itemviewer.tooltip.TooltipColors.getModifierColor;
import static net.felix.features.itemviewer.ui.ItemStackCache.createDisplayStack;

/**
 * Baut die Tooltip-Zeilen eines Items und rendert sie.
 */
public final class ItemTooltipBuilder {

    private ItemTooltipBuilder() {
    }

    public static void updateItemViewerAspectOverlay(ItemData hoveredItem) {
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

        net.felix.features.information.aspects.AspectInfo aspectInfo =
            net.felix.features.information.aspects.AspectDatabase.getAspectInfoForBlueprintFull(hoveredItem.name);
        if (aspectInfo != null && aspectInfo.aspectName != null && !aspectInfo.aspectName.isEmpty()
            && !"-".equals(aspectInfo.aspectName)) {
            net.felix.utilities.Overall.Aspekte.AspectOverlay.updateAspectInfoFromItemViewer(hoveredItem.name, aspectInfo);
        } else {
            net.felix.utilities.Overall.Aspekte.AspectOverlay.onHoverStopped();
        }
    }

    public static List<Component> buildTooltipLines(ItemData hoveredItem, ItemStack itemStack, Minecraft client,
            boolean includeInteractionHints) {
        return buildTooltipLines(hoveredItem, itemStack, client, includeInteractionHints, true);
    }

    public static List<Component> buildTooltipLines(ItemData hoveredItem, ItemStack itemStack, Minecraft client,
            boolean includeInteractionHints, boolean includeCosts) {
                java.util.List<Component> tooltipLines = new java.util.ArrayList<>();
                try {
                    tooltipLines.add(itemStack.getHoverName());
                    
                    // Wenn Advanced Tooltips aktiv sind, füge Item-ID hinzu
                    if (client.options.advancedItemTooltips) {
                        tooltipLines.add(Component.literal(itemStack.getItem().toString())
                            .setStyle(Style.EMPTY.withColor(0xFF808080)));
                    }
                } catch (Exception e) {
                    // Fallback: Nur Item-Name
                    tooltipLines.add(itemStack.getHoverName());
                }
                
                // Leere Zeile zwischen Name und Tooltip
                tooltipLines.add(Component.empty());

                if ("fishing_components".equals(hoveredItem.category)) {
                    appendFoundAtLocationLines(tooltipLines, hoveredItem);
                }
                
                // Zusätzliche Informationen aus ItemInfo und anderen Daten
                if (hoveredItem.info != null) {
                    ItemInfo info = hoveredItem.info;

                    if ("fish_traps".equals(hoveredItem.category) || Boolean.TRUE.equals(info.fish_trap)) {
                        appendFishTrapTooltipLines(tooltipLines, hoveredItem, info, includeInteractionHints, includeCosts);
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
                                    tooltipLines.add(Component.literal(line)
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
                                        tooltipLines.add(Component.literal(parts[0] + splitMarker)
                                            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                                        // Zweite Zeile: Teil nach Split-Marker (trim, um führende Leerzeichen zu entfernen)
                                        String secondPart = parts[1].trim();
                                        if (!secondPart.isEmpty()) {
                                            tooltipLines.add(Component.literal(secondPart)
                                                .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                                        }
                                    } else {
                                        // Fallback: normale Anzeige
                                        tooltipLines.add(Component.literal(description)
                                            .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                                    }
                                } else {
                                    // Kein Split-Marker gefunden - normale Anzeige
                                    tooltipLines.add(Component.literal(description)
                                        .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                                }
                            }
                        }
                        
                        // Leere Zeile nach Description
                        tooltipLines.add(Component.empty());
                        
                        // level_info (aqua, nicht kursiv)
                        if (info.level_info != null && !info.level_info.isEmpty()) {
                            for (String levelLine : info.level_info) {
                                if (levelLine != null && !levelLine.isEmpty()) {
                                    tooltipLines.add(Component.literal(levelLine)
                                        .setStyle(Style.EMPTY.withColor(0xFF00FFFF).withItalic(false))); // aqua
                                }
                            }
                        }
                        
                        // first_upgrade (dark_aqua, nicht kursiv)
                        if (info.first_upgrade != null && !info.first_upgrade.isEmpty()) {
                            tooltipLines.add(Component.literal(info.first_upgrade)
                                .setStyle(Style.EMPTY.withColor(0xFF00AAAA).withItalic(false))); // dark_aqua
                        }
                        
                        // Kosten für Power Crystal Slots (nach first_upgrade)
                        appendPriceSection(tooltipLines, hoveredItem, true, includeCosts);
                        
                        // Forschungs Zeit separat anzeigen (gelb Header, dann grün)
                        if (hoveredItem.time != null && hoveredItem.time.hours != null) {
                            // Leere Zeile zwischen Kosten und Forschungs Zeit
                            tooltipLines.add(Component.empty());
                            tooltipLines.add(Component.literal("Forschungs Zeit:")
                                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false))); // yellow
                            
                            // Formatiere Zeit: hoursh
                            String timeAmount = formatAmount(hoveredItem.time.hours);
                            String timeText = timeAmount + "h";
                            tooltipLines.add(Component.literal(timeText)
                                .setStyle(Style.EMPTY.withColor(0xFF55FF55).withItalic(false))); // green
                        }
                        
                        // Leere Zeilen am Ende
                        tooltipLines.add(Component.empty());
                        tooltipLines.add(Component.empty());
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
                                tooltipLines.add(Component.literal(line)
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
                                tooltipLines.add(Component.empty());
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
                                    tooltipLines.add(Component.literal(line)
                                        .setStyle(Style.EMPTY.withColor(0xFF55FFFF).withItalic(false)));
                                }
                                hasDescOrUpdateInfo = true;
                            }
                        }
                        
                        // Leere Zeile nach Description/Update-Info Block
                        if (hasDescOrUpdateInfo) {
                            tooltipLines.add(Component.empty());
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
                            tooltipLines.add(Component.literal(typePieceText)
                                .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                        }
                    
                    // Stats (weiß, nicht kursiv)
                    // Unterstützt sowohl String als auch Array von Strings
                    if (info.stats != null) {
                        if (info.stats instanceof String) {
                            String statsStr = (String) info.stats;
                            if (!statsStr.isEmpty()) {
                                tooltipLines.add(Component.literal(statsStr)
                                    .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)));
                            }
                        } else if (info.stats instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> statsList = (List<Object>) info.stats;
                            for (Object statObj : statsList) {
                                if (statObj != null) {
                                    String statLine = statObj.toString();
                                    if (!statLine.isEmpty()) {
                                        tooltipLines.add(Component.literal(statLine)
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
                    tooltipLines.add(Component.empty());
                    
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
                        net.felix.features.information.aspects.AspectInfo aspectInfo = 
                            net.felix.features.information.aspects.AspectDatabase.getAspectInfoForBlueprintFull(hoveredItem.name);
                        
                        if (aspectInfo != null && aspectInfo.aspectName != null && !aspectInfo.aspectName.isEmpty()) {
                            // "Aspekt: AspektName" - "Aspekt:" in Gelb, AspektName in Orange/Gold (wie in AspectOverlay)
                            MutableComponent aspectText = Component.literal("Aspekt: ")
                                .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)); // yellow
                            aspectText.append(Component.literal(aspectInfo.aspectName)
                                .setStyle(Style.EMPTY.withColor(0xFFFCA800).withItalic(false))); // orange/gold (aggressiver, wie in AspectOverlay)
                            tooltipLines.add(aspectText);
                            
                            // "(Shift für mehr Info)" - ohne Einrückung, hellgrau
                            tooltipLines.add(Component.literal("(Shift für mehr Info)")
                                .setStyle(Style.EMPTY.withColor(0xFFCCCCCC).withItalic(false)));
                            
                            // Leere Zeile nach Aspekt (vor Modifier)
                            tooltipLines.add(Component.empty());
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
                                    MutableComponent modifierText = Component.literal("⦁")
                                        .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false)); // weiß
                                    modifierText.append(Component.literal(" [")
                                        .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false))); // weiß
                                    modifierText.append(Component.literal(modifier)
                                        .setStyle(Style.EMPTY.withColor(modifierColor).withItalic(false))); // Modifier-Farbe
                                    modifierText.append(Component.literal("]")
                                        .setStyle(Style.EMPTY.withColor(0xFFFFFFFF).withItalic(false))); // weiß
                                    
                                    tooltipLines.add(modifierText);
                                }
                            }
                        }
                        // Leere Zeile nach Modifier (vor Status)
                        tooltipLines.add(Component.empty());
                    }
                    
                    // Status (gelb Header, dann grün/rot für gefunden/nicht gefunden)
                    // Nur für Blueprints anzeigen, nicht für Module/Modulbags
                    if (info.blueprint != null && info.blueprint) {
                        boolean isFound = isItemFound(hoveredItem);
                        MutableComponent statusText = Component.literal("Status:")
                            .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)); // yellow
                        statusText.append(Component.literal(" ")
                            .setStyle(Style.EMPTY.withItalic(false)));
                        if (isFound) {
                            statusText.append(Component.literal("[Gefunden]")
                                .setStyle(Style.EMPTY.withColor(0xFF00FF00).withItalic(false))); // green
                        } else {
                            statusText.append(Component.literal("[Nicht Gefunden]")
                                .setStyle(Style.EMPTY.withColor(0xFFFF5555).withItalic(false))); // red (less aggressive)
                        }
                        tooltipLines.add(statusText);
                        
                        if (includeCosts && ViewerState.isShowCosts()) {
                            tooltipLines.add(Component.empty());
                        }
                    }
                    
                    appendPriceSection(tooltipLines, hoveredItem, false, includeCosts);
                    appendBlueprintShopSection(tooltipLines, hoveredItem, includeCosts);
                    
                    if (includeInteractionHints) {
                        appendInteractionHints(tooltipLines, hoveredItem, info);
                    }
                    }
                }
                return tooltipLines;
    }

    public static java.util.List<Component> buildTooltipLinesForItem(ItemData itemData) {
        return buildTooltipLinesForItem(itemData, true);
    }

    public static java.util.List<Component> buildTooltipLinesForItem(ItemData itemData, boolean includeCosts) {
        if (itemData == null) {
            return java.util.Collections.emptyList();
        }
        ItemStack stack = createDisplayStack(itemData);
        if (stack.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        Minecraft client = Minecraft.getInstance();
        return buildTooltipLines(itemData, stack, client, false, includeCosts);
    }

    static void appendInteractionHints(java.util.List<Component> tooltipLines, ItemData hoveredItem, ItemInfo info) {
        if (info.blueprint != null && info.blueprint) {
            boolean isFavorite = net.felix.features.itemviewer.inventory.FavoriteBlueprintsStore.isFavorite(hoveredItem.name);
            tooltipLines.add(Component.empty());
            if (isFavorite) {
                tooltipLines.add(Component.literal("Aus Favoriten entfernen:")
                    .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)));
                tooltipLines.add(Component.literal("Mit Rechtsklick aus Favoriten entfernen")
                    .setStyle(Style.EMPTY.withColor(0xFFFF5555).withItalic(false)));
            } else {
                tooltipLines.add(Component.literal("Zu Favoriten hinzufügen:")
                    .setStyle(Style.EMPTY.withColor(0xFFFFFF00).withItalic(false)));
                tooltipLines.add(Component.literal("Mit Rechtsklick zu Favoriten hinzufügen")
                    .setStyle(Style.EMPTY.withColor(0xFF16A80C).withItalic(false)));
            }
        }

        if (net.felix.utilities.Other.Clipboard.ClipboardUtility.isClipboardPinnable(hoveredItem)) {
            String hotkeyText = net.felix.features.itemviewer.ItemViewerHotkeys.getClipboardPinHotkeyText();
            tooltipLines.add(Component.literal("An Pinnwand anheften mit Hotkey: " + hotkeyText)
                .setStyle(Style.EMPTY.withColor(0xFF808080).withItalic(false)));
        }
    }

    public static void renderItemTooltip(GuiGraphicsExtractor context, ItemData itemData, int mouseX, int mouseY) {
        java.util.List<Component> lines = buildTooltipLinesForItem(itemData);
        if (lines.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        context.setComponentTooltipForNextFrame(client.font, lines, mouseX, mouseY);
    }

    public static void updateAspectOverlayForItem(ItemData itemData) {
        updateItemViewerAspectOverlay(itemData);
    }
}

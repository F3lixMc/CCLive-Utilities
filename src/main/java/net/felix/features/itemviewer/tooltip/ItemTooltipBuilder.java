package net.felix.features.itemviewer.tooltip;

import java.util.ArrayList;
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
        List<Component> tooltipLines = new ArrayList<>();
        appendNameLines(tooltipLines, itemStack, client);

        // Leere Zeile zwischen Name und Tooltip
        tooltipLines.add(Component.empty());

        if ("fishing_components".equals(hoveredItem.category)) {
            appendFoundAtLocationLines(tooltipLines, hoveredItem);
        }

        // Zusätzliche Informationen aus ItemInfo und anderen Daten
        if (hoveredItem.info == null) {
            return tooltipLines;
        }
        ItemInfo info = hoveredItem.info;

        if ("fish_traps".equals(hoveredItem.category) || Boolean.TRUE.equals(info.fish_trap)) {
            appendFishTrapTooltipLines(tooltipLines, hoveredItem, info, includeInteractionHints, includeCosts);
            return tooltipLines;
        }

        if (info.power_crystal != null && info.power_crystal) {
            appendPowerCrystalLines(tooltipLines, hoveredItem, info, includeCosts);
        } else {
            appendStandardLines(tooltipLines, hoveredItem, info, includeInteractionHints, includeCosts);
        }
        return tooltipLines;
    }

    /** Textzeile in der angegebenen Farbe, nicht kursiv. */
    private static MutableComponent styledLine(String text, int color) {
        return Component.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
    }

    /** Item-Name; bei aktiven Advanced Tooltips zusätzlich die Item-ID in Grau. */
    private static void appendNameLines(List<Component> tooltipLines, ItemStack itemStack, Minecraft client) {
        try {
            tooltipLines.add(itemStack.getHoverName());

            if (client.options.advancedItemTooltips) {
                tooltipLines.add(Component.literal(itemStack.getItem().toString())
                        .setStyle(Style.EMPTY.withColor(0xFF808080)));
            }
        } catch (Exception e) {
            // Fallback: Nur Item-Name
            tooltipLines.add(itemStack.getHoverName());
        }
    }

    /**
     * Power Crystals haben ein spezielles Format: Beschreibung (weiß), Leerzeile, level_info (aqua),
     * first_upgrade (dark_aqua), Kosten und Forschungszeit.
     */
    private static void appendPowerCrystalLines(List<Component> tooltipLines, ItemData hoveredItem, ItemInfo info,
            boolean includeCosts) {
        if (info.description != null && !info.description.isEmpty()) {
            appendPowerCrystalDescription(tooltipLines, info.description);
        }

        // Leere Zeile nach Description
        tooltipLines.add(Component.empty());

        if (info.level_info != null && !info.level_info.isEmpty()) {
            for (String levelLine : info.level_info) {
                if (levelLine != null && !levelLine.isEmpty()) {
                    tooltipLines.add(styledLine(levelLine, 0xFF00FFFF)); // aqua
                }
            }
        }

        if (info.first_upgrade != null && !info.first_upgrade.isEmpty()) {
            tooltipLines.add(styledLine(info.first_upgrade, 0xFF00AAAA)); // dark_aqua
        }

        // Kosten für Power Crystal Slots (nach first_upgrade)
        appendPriceSection(tooltipLines, hoveredItem, true, includeCosts);

        // Forschungs Zeit separat anzeigen (gelb Header, dann grün)
        if (hoveredItem.time != null && hoveredItem.time.hours != null) {
            // Leere Zeile zwischen Kosten und Forschungs Zeit
            tooltipLines.add(Component.empty());
            tooltipLines.add(styledLine("Forschungs Zeit:", 0xFFFFFF00)); // yellow
            tooltipLines.add(styledLine(formatAmount(hoveredItem.time.hours) + "h", 0xFF55FF55)); // green
        }

        // Leere Zeilen am Ende
        tooltipLines.add(Component.empty());
        tooltipLines.add(Component.empty());
    }

    /**
     * Beschreibung eines Power Crystals (weiß): Zeilenumbrüche bei "\n", sonst Umbruch nach
     * "Spieler," oder "Owyn,", falls vorhanden.
     */
    private static void appendPowerCrystalDescription(List<Component> tooltipLines, String description) {
        if (description.contains("\n")) {
            // Jede Zeile separat, auch leere (für Absätze)
            for (String line : description.split("\n")) {
                tooltipLines.add(styledLine(line, 0xFFFFFFFF));
            }
            return;
        }

        String splitMarker = null;
        if (description.contains("Spieler,")) {
            splitMarker = "Spieler,";
        } else if (description.contains("Owyn,")) {
            splitMarker = "Owyn,";
        }

        String[] parts = splitMarker != null ? description.split(splitMarker, 2) : null;
        if (parts == null || parts.length != 2) {
            // Kein (passender) Split-Marker: normale Anzeige
            tooltipLines.add(styledLine(description, 0xFFFFFFFF));
            return;
        }

        // Erste Zeile: Teil vor Split-Marker + Split-Marker
        tooltipLines.add(styledLine(parts[0] + splitMarker, 0xFFFFFFFF));
        // Zweite Zeile: Teil nach Split-Marker (trim, um führende Leerzeichen zu entfernen)
        String secondPart = parts[1].trim();
        if (!secondPart.isEmpty()) {
            tooltipLines.add(styledLine(secondPart, 0xFFFFFFFF));
        }
    }

    /** Normales Format für Blueprints, Module usw. */
    private static void appendStandardLines(List<Component> tooltipLines, ItemData hoveredItem, ItemInfo info,
            boolean includeInteractionHints, boolean includeCosts) {
        boolean hasDescOrUpdateInfo = appendDescriptionAndUpdateInfo(tooltipLines, info);

        // Leere Zeile nach Description/Update-Info Block
        if (hasDescOrUpdateInfo) {
            tooltipLines.add(Component.empty());
        }

        appendTypeAndPiece(tooltipLines, info);
        appendStats(tooltipLines, info);

        // Level/Collection (aus foundAt) – bei Angel-Komponenten bereits oben nach dem Namen
        if (!"fishing_components".equals(hoveredItem.category)) {
            appendFoundAtLocationLines(tooltipLines, hoveredItem, false);
        }

        // Leere Zeile als Trenner (vor Aspekt)
        tooltipLines.add(Component.empty());

        appendAspectLines(tooltipLines, hoveredItem, info);
        appendModifierLines(tooltipLines, hoveredItem, info);
        appendStatusLines(tooltipLines, hoveredItem, info, includeCosts);

        appendPriceSection(tooltipLines, hoveredItem, false, includeCosts);
        appendBlueprintShopSection(tooltipLines, hoveredItem, includeCosts);

        if (includeInteractionHints) {
            appendInteractionHints(tooltipLines, hoveredItem, info);
        }
    }

    /**
     * Description (weiß, mit Zeilenumbrüchen), danach Update-Info (z.B. für Autoschmelzer, Farbe #55FFFF).
     * Description wird für Module/Modulbags/Abilities angezeigt.
     *
     * @return true, wenn mindestens eine Description- oder Update-Info-Zeile ausgegeben wurde
     */
    private static boolean appendDescriptionAndUpdateInfo(List<Component> tooltipLines, ItemInfo info) {
        boolean hasDescOrUpdateInfo = false;
        boolean hasDescription = info.description != null && !info.description.isEmpty();
        if (hasDescription) {
            // Auch leere Zeilen rendern (für Absätze bei \n\n)
            for (String line : info.description.split("\n")) {
                tooltipLines.add(styledLine(line, 0xFFFFFFFF));
            }
            hasDescOrUpdateInfo = true;
        }

        if (info.update_info == null) {
            return hasDescOrUpdateInfo;
        }
        List<String> updateLines = collectUpdateInfoLines(info.update_info);

        // Leere Zeile zwischen Description und Update-Info
        if (hasDescription && isUpdateInfoNotEmpty(info.update_info)) {
            tooltipLines.add(Component.empty());
        }

        if (!updateLines.isEmpty()) {
            for (String line : updateLines) {
                tooltipLines.add(styledLine(line, 0xFF55FFFF));
            }
            hasDescOrUpdateInfo = true;
        }
        return hasDescOrUpdateInfo;
    }

    private static boolean isUpdateInfoNotEmpty(Object updateInfo) {
        if (updateInfo instanceof String) {
            return !((String) updateInfo).isEmpty();
        }
        if (updateInfo instanceof List) {
            return !((List<?>) updateInfo).isEmpty();
        }
        return false;
    }

    /** Update-Info kann ein String (mit \n getrennt) oder eine Liste von Strings sein; leere Zeilen entfallen. */
    private static List<String> collectUpdateInfoLines(Object updateInfo) {
        List<String> updateLines = new ArrayList<>();
        if (updateInfo instanceof String) {
            String updateInfoStr = (String) updateInfo;
            if (!updateInfoStr.isEmpty()) {
                for (String line : updateInfoStr.split("\n")) {
                    if (!line.isEmpty()) {
                        updateLines.add(line);
                    }
                }
            }
        } else if (updateInfo instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> updateInfoList = (List<String>) updateInfo;
            for (String line : updateInfoList) {
                if (line != null && !line.isEmpty()) {
                    updateLines.add(line);
                }
            }
        }
        return updateLines;
    }

    /** Typ und Piece (weiß): "type - piece" wenn beide vorhanden, sonst nur das vorhandene. */
    private static void appendTypeAndPiece(List<Component> tooltipLines, ItemInfo info) {
        boolean hasType = info.type != null && !info.type.isEmpty();
        boolean hasPiece = info.piece != null && !info.piece.isEmpty();
        if (!hasType && !hasPiece) {
            return;
        }
        String typePieceText;
        if (hasType && hasPiece) {
            typePieceText = info.type + " - " + info.piece;
        } else if (hasType) {
            typePieceText = info.type;
        } else {
            typePieceText = info.piece;
        }
        tooltipLines.add(styledLine(typePieceText, 0xFFFFFFFF));
    }

    /** Stats (weiß); unterstützt sowohl einen String als auch eine Liste. */
    private static void appendStats(List<Component> tooltipLines, ItemInfo info) {
        if (info.stats instanceof String) {
            String statsStr = (String) info.stats;
            if (!statsStr.isEmpty()) {
                tooltipLines.add(styledLine(statsStr, 0xFFFFFFFF));
            }
        } else if (info.stats instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> statsList = (List<Object>) info.stats;
            for (Object statObj : statsList) {
                if (statObj != null) {
                    String statLine = statObj.toString();
                    if (!statLine.isEmpty()) {
                        tooltipLines.add(styledLine(statLine, 0xFFFFFFFF));
                    }
                }
            }
        }
    }

    /**
     * Aspekt (nach Level, vor Modifier). Ein Item hat einen Aspekt, wenn "aspect" nicht leer und nicht "false" ist
     * (rückwärtskompatibel zu "aspect": true/false in der JSON).
     */
    private static void appendAspectLines(List<Component> tooltipLines, ItemData hoveredItem, ItemInfo info) {
        boolean hasAspect = info.aspect != null && !info.aspect.isEmpty() && !"false".equalsIgnoreCase(info.aspect);
        if (!hasAspect) {
            return;
        }
        net.felix.features.information.aspects.AspectInfo aspectInfo =
                net.felix.features.information.aspects.AspectDatabase.getAspectInfoForBlueprintFull(hoveredItem.name);
        if (aspectInfo == null || aspectInfo.aspectName == null || aspectInfo.aspectName.isEmpty()) {
            return;
        }

        // "Aspekt: AspektName" - "Aspekt:" in Gelb, AspektName in Orange/Gold (wie in AspectOverlay)
        MutableComponent aspectText = styledLine("Aspekt: ", 0xFFFFFF00); // yellow
        aspectText.append(styledLine(aspectInfo.aspectName, 0xFFFCA800)); // orange/gold
        tooltipLines.add(aspectText);

        // "(Shift für mehr Info)" - ohne Einrückung, hellgrau
        tooltipLines.add(styledLine("(Shift für mehr Info)", 0xFFCCCCCC));

        // Leere Zeile nach Aspekt (vor Modifier)
        tooltipLines.add(Component.empty());
    }

    /**
     * Modifier, jeder auf eigener Zeile im Format "⦁ [ModifierX]": nur ModifierX hat die
     * Modifier-Farbe, der Rest ist weiß. Kommt nach Aspekt, vor Status.
     */
    private static void appendModifierLines(List<Component> tooltipLines, ItemData hoveredItem, ItemInfo info) {
        if (info.modifier == null || info.modifier.isEmpty()) {
            return;
        }
        boolean fishingComponent = "fishing_components".equals(hoveredItem.category);
        for (String modifier : info.modifier) {
            if (modifier == null || modifier.isEmpty()) {
                continue;
            }
            if (fishingComponent) {
                tooltipLines.add(buildFishingComponentModifierText(modifier));
                continue;
            }
            MutableComponent modifierText = styledLine("⦁", 0xFFFFFFFF);
            modifierText.append(styledLine(" [", 0xFFFFFFFF));
            modifierText.append(styledLine(modifier, getModifierColor(modifier)));
            modifierText.append(styledLine("]", 0xFFFFFFFF));
            tooltipLines.add(modifierText);
        }
        // Leere Zeile nach Modifier (vor Status)
        tooltipLines.add(Component.empty());
    }

    /** Status (gelb Header, dann grün/rot für gefunden/nicht gefunden); nur für Blueprints, nicht für Module/Modulbags. */
    private static void appendStatusLines(List<Component> tooltipLines, ItemData hoveredItem, ItemInfo info,
            boolean includeCosts) {
        if (info.blueprint == null || !info.blueprint) {
            return;
        }
        MutableComponent statusText = styledLine("Status:", 0xFFFFFF00); // yellow
        statusText.append(Component.literal(" ").setStyle(Style.EMPTY.withItalic(false)));
        if (isItemFound(hoveredItem)) {
            statusText.append(styledLine("[Gefunden]", 0xFF00FF00)); // green
        } else {
            statusText.append(styledLine("[Nicht Gefunden]", 0xFFFF5555)); // red (less aggressive)
        }
        tooltipLines.add(statusText);

        if (includeCosts && ViewerState.isShowCosts()) {
            tooltipLines.add(Component.empty());
        }
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

        if (net.felix.utilities.Other.clipboard.ClipboardUtility.isClipboardPinnable(hoveredItem)) {
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

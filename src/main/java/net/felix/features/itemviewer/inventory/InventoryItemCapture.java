package net.felix.features.itemviewer.inventory;

import java.util.ArrayList;
import net.felix.features.itemviewer.data.ItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import static net.felix.features.itemviewer.data.ItemCatalog.findItemByName;
import static net.felix.features.itemviewer.data.ItemCatalog.findItemByNameAndFoundAt;
import static net.felix.features.itemviewer.ui.ViewerState.lastHoveredScreen;
import static net.felix.features.itemviewer.ui.ViewerState.lastHoveredSlot;
import static net.felix.features.itemviewer.ui.ViewerState.lastMouseX;
import static net.felix.features.itemviewer.ui.ViewerState.lastMouseY;

/**
 * Liest Items aus dem geoeffneten Inventar (Hover, Tooltip) fuer Pin und Teilen.
 */
public final class InventoryItemCapture {

    private InventoryItemCapture() {
    }

    /**
     * Prüft ob ein Inventar geöffnet ist (AbstractContainerScreen oder InventoryScreen)
     */
    public static boolean isInventoryOpen(Minecraft client) {
        if (client == null || client.gui.screen() == null) {
            return false;
        }
        return client.gui.screen() instanceof AbstractContainerScreen<?> || 
               client.gui.screen() instanceof InventoryScreen;
    }

    /**
     * Versucht das aktuell gehoverte Inventar-Item an die Pinnwand anzuheften.
     */
    public static boolean tryPinHoveredInventoryItem(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> handledScreen, Minecraft client) {
        net.minecraft.world.inventory.Slot hoveredSlot = null;
        if (lastHoveredScreen == handledScreen && lastHoveredSlot != null && lastHoveredSlot.hasItem()) {
            hoveredSlot = lastHoveredSlot;
        } else {
            hoveredSlot = getHoveredSlot(handledScreen, client);
        }
        
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return false;
        }
        
        net.minecraft.world.item.ItemStack stack = hoveredSlot.getItem();
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        String itemName = extractItemNameFromTooltip(handledScreen, client, stack);
        if (itemName == null || itemName.isEmpty()) {
            itemName = stack.getHoverName().getString();
        }
        itemName = cleanItemNameForLookup(itemName);
        
        String foundAtFloor = extractFoundAtFromTooltip(handledScreen, client, stack);
        ItemData itemData = findItemByNameAndFoundAt(itemName, foundAtFloor);
        if (itemData == null) {
            itemData = findItemByName(itemName);
        }
        
        if (itemData != null && net.felix.utilities.Other.clipboard.ClipboardUtility.isClipboardPinnable(itemData)) {
            return net.felix.utilities.Other.clipboard.ClipboardUtility.addBlueprint(itemData);
        }
        return false;
    }

    /**
     * Kopiert das gehoverte Inventar-/Chest-Item als Share-Snapshot (nicht ItemViewer-Grid).
     * Template-Felder für Icon + Live-Tooltip-Stats.
     */
    public static ItemData captureHoveredInventoryItemForShare(Minecraft client) {
        if (client == null || !(client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> handledScreen)) {
            return null;
        }
        // ItemViewer-Grid bewusst nicht — nur echte Inventar-Slots
        net.minecraft.world.inventory.Slot hoveredSlot;
        if (lastHoveredScreen == handledScreen && lastHoveredSlot != null && lastHoveredSlot.hasItem()) {
            hoveredSlot = lastHoveredSlot;
        } else {
            hoveredSlot = getHoveredSlot(handledScreen, client);
        }
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return null;
        }
        net.minecraft.world.item.ItemStack stack = hoveredSlot.getItem();
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        java.util.List<net.felix.chat.ChatTooltipLine> tooltipLines =
            extractShareTooltipLines(handledScreen, client, stack);
        String rawName = extractItemNameFromTooltip(handledScreen, client, stack);
        if (rawName == null || rawName.isEmpty()) {
            rawName = stack.getHoverName().getString();
        }
        String displayName = cleanItemNameForDisplay(rawName);
        String lookupName = cleanItemNameForLookup(rawName);

        String foundAtFloor = extractFoundAtFromTooltip(handledScreen, client, stack);
        ItemData template = findItemByNameAndFoundAt(lookupName, foundAtFloor);
        if (template == null) {
            template = findItemByName(lookupName);
        }

        return net.felix.chat.ItemShareManager.buildSnapshotFromTooltip(displayName, tooltipLines, template);
    }

    /**
     * Share-Snapshot aus beliebigem Inventar-Stack (nicht nur Cursor).
     */
    public static ItemData captureInventoryStackForShare(
            Minecraft client,
            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> handledScreen,
            net.minecraft.world.item.ItemStack stack) {
        if (client == null || handledScreen == null || stack == null || stack.isEmpty()) {
            return null;
        }

        java.util.List<net.felix.chat.ChatTooltipLine> tooltipLines =
            extractShareTooltipLines(handledScreen, client, stack);
        String rawName = extractItemNameFromTooltip(handledScreen, client, stack);
        if (rawName == null || rawName.isEmpty()) {
            rawName = stack.getHoverName().getString();
        }
        String displayName = cleanItemNameForDisplay(rawName);
        String lookupName = cleanItemNameForLookup(rawName);

        String foundAtFloor = extractFoundAtFromTooltip(handledScreen, client, stack);
        ItemData template = findItemByNameAndFoundAt(lookupName, foundAtFloor);
        if (template == null) {
            template = findItemByName(lookupName);
        }

        ItemData snapshot = net.felix.chat.ItemShareManager.buildSnapshotFromTooltip(
            displayName, tooltipLines, template);
        // Live-Stack hat Vorrang für Icon (Modul-Platzhalter oft nicht in ItemViewer)
        applyLiveStackVisuals(snapshot, stack);
        return snapshot;
    }

    /**
     * Item-ID + vollständige CustomModelData vom echten Inventar-Stack übernehmen
     * (Platzhalter/Locked-Module brauchen floats/strings vom Live-Item).
     */
    public static void applyLiveStackVisuals(ItemData snapshot, ItemStack stack) {
        if (snapshot == null || stack == null || stack.isEmpty()) {
            return;
        }
        try {
            Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null) {
                snapshot.id = id.toString();
            }
        } catch (Exception ignored) {
        }
        try {
            var cmd = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
            if (cmd == null) {
                return;
            }
            if (cmd.floats() != null && !cmd.floats().isEmpty()) {
                snapshot.customModelData = cmd.floats().get(0).intValue();
            } else {
                Integer legacy = net.felix.utilities.Aincraft.iteminfo.ItemInfoUtility.extractCustomModelData(stack);
                if (legacy != null) {
                    snapshot.customModelData = legacy;
                }
            }
            if (cmd.strings() != null && !cmd.strings().isEmpty()) {
                snapshot.customModelStrings = new ArrayList<>(cmd.strings());
            }
            if (cmd.flags() != null && !cmd.flags().isEmpty()) {
                snapshot.customModelFlags = new ArrayList<>(cmd.flags());
            }
            if (cmd.colors() != null && !cmd.colors().isEmpty()) {
                snapshot.customModelColors = new ArrayList<>(cmd.colors());
            }
        } catch (Exception ignored) {
        }
    }

    /** Anzeigename eines Inventar-Stacks (für Kit-Namen etc.). */
    public static String extractInventoryItemDisplayName(
            Minecraft client,
            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> handledScreen,
            net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        String rawName = extractItemNameFromTooltip(handledScreen, client, stack);
        if (rawName == null || rawName.isEmpty()) {
            rawName = stack.getHoverName().getString();
        }
        return cleanItemNameForDisplay(rawName);
    }

    /**
     * Inventar-Tooltip als formatierte Zeilen, ohne Vanilla-Attribute und ohne Advanced-IDs.
     */
    private static java.util.List<net.felix.chat.ChatTooltipLine> extractShareTooltipLines(
            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen,
            Minecraft client,
            net.minecraft.world.item.ItemStack stack) {
        java.util.List<net.minecraft.network.chat.Component> rawLines = new java.util.ArrayList<>();
        try {
            java.util.List<net.minecraft.network.chat.Component> tooltip = null;
            Class<?> clazz = screen.getClass();
            while (clazz != null && tooltip == null) {
                for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
                    if (!"getTooltipFromItem".equals(method.getName())) {
                        continue;
                    }
                    Class<?>[] params = method.getParameterTypes();
                    method.setAccessible(true);
                    try {
                        if (params.length == 2
                                && net.minecraft.client.Minecraft.class.isAssignableFrom(params[0])
                                && net.minecraft.world.item.ItemStack.class.isAssignableFrom(params[1])) {
                            @SuppressWarnings("unchecked")
                            java.util.List<net.minecraft.network.chat.Component> result =
                                (java.util.List<net.minecraft.network.chat.Component>) method.invoke(screen, client, stack);
                            tooltip = result;
                            break;
                        }
                        if (params.length == 1 && net.minecraft.world.item.ItemStack.class.isAssignableFrom(params[0])) {
                            @SuppressWarnings("unchecked")
                            java.util.List<net.minecraft.network.chat.Component> result =
                                (java.util.List<net.minecraft.network.chat.Component>) method.invoke(screen, stack);
                            tooltip = result;
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }
                clazz = clazz.getSuperclass();
            }
            if (tooltip != null) {
                rawLines.addAll(tooltip);
            }
        } catch (Exception ignored) {
        }

        if (rawLines.size() <= 1) {
            rawLines.clear();
            rawLines.add(stack.getHoverName());
            try {
                net.minecraft.world.item.component.ItemLore lore =
                    stack.get(net.minecraft.core.component.DataComponents.LORE);
                if (lore != null) {
                    for (net.minecraft.network.chat.Component line : lore.lines()) {
                        if (line != null) {
                            rawLines.add(line);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        java.util.List<net.minecraft.network.chat.Component> filtered =
            net.felix.chat.ChatTooltipLine.filterInventoryTooltip(rawLines);
        java.util.List<net.felix.chat.ChatTooltipLine> result = new java.util.ArrayList<>();
        for (net.minecraft.network.chat.Component line : filtered) {
            result.add(net.felix.chat.ChatTooltipLine.fromComponent(line));
        }
        return result;
    }

    /**
     * Findet den gehoverten Slot in einem AbstractContainerScreen
     * Verwendet die gespeicherte Mausposition (lastMouseX, lastMouseY) wenn verfügbar
     * Oder berechnet die Mausposition direkt aus client.mouse
     */
    private static net.minecraft.world.inventory.Slot getHoveredSlot(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen, Minecraft client) {
        try {
            // Hole Mouse-Position - verwende gespeicherte Position wenn verfügbar, sonst berechne direkt
            double mouseX;
            double mouseY;
            if (lastMouseX >= 0 && lastMouseY >= 0) {
                // Verwende gespeicherte Mausposition (wird im Render-Update gesetzt)
                mouseX = lastMouseX;
                mouseY = lastMouseY;
            } else {
                // Fallback: Berechne Mausposition direkt aus client.mouse
                // Verwende die gleiche Methode wie in AbstractContainerScreenMixin
                mouseX = client.mouseHandler.xpos() * (double) client.getWindow().getGuiScaledWidth() / (double) client.getWindow().getScreenWidth();
                mouseY = client.mouseHandler.ypos() * (double) client.getWindow().getGuiScaledHeight() / (double) client.getWindow().getScreenHeight();
            }
            
            // Hole Screen-Position (über Reflection, da @Shadow nicht immer funktioniert)
            int screenX = 0;
            int screenY = 0;
            try {
                java.lang.reflect.Field xField = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("leftPos");
                java.lang.reflect.Field yField = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredField("topPos");
                xField.setAccessible(true);
                yField.setAccessible(true);
                screenX = xField.getInt(screen);
                screenY = yField.getInt(screen);
            } catch (NoSuchFieldException e) {
                // Fallback: Versuche mit getter-Methoden
                try {
                    java.lang.reflect.Method getXMethod = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredMethod("getX");
                    java.lang.reflect.Method getYMethod = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredMethod("getY");
                    getXMethod.setAccessible(true);
                    getYMethod.setAccessible(true);
                    screenX = (Integer) getXMethod.invoke(screen);
                    screenY = (Integer) getYMethod.invoke(screen);
                } catch (Exception ex) {
                    // Ignoriere Fehler, verwende 0,0 als Fallback
                }
            } catch (Exception e) {
                // Ignoriere Fehler, verwende 0,0 als Fallback
            }
            
            // Finde gehoverten Slot (gleiche Logik wie in AbstractContainerScreenMixin)
            for (net.minecraft.world.inventory.Slot slot : screen.getMenu().slots) {
                if (slot.x + screenX <= mouseX && mouseX < slot.x + screenX + 16 &&
                    slot.y + screenY <= mouseY && mouseY < slot.y + screenY + 16) {
                    return slot;
                }
            }
        } catch (Exception e) {
            // Ignoriere Fehler
        }
        return null;
    }

    /**
     * Extrahiert den Item-Namen aus dem Tooltip (erste Zeile: "name - [Bauplan]")
     */
    public static String extractItemNameFromTooltip(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen, Minecraft client, net.minecraft.world.item.ItemStack stack) {
        try {
            // Hole Tooltip über Reflection (ähnlich wie ClipboardPaperShredsCollector)
            java.util.List<net.minecraft.network.chat.Component> tooltip = null;
            
            try {
                java.lang.reflect.Method[] methods = screen.getClass().getDeclaredMethods();
                for (java.lang.reflect.Method method : methods) {
                    if (!"getTooltipFromItem".equals(method.getName())) {
                        continue;
                    }
                    
                    Class<?>[] params = method.getParameterTypes();
                    method.setAccessible(true);
                    
                    try {
                        // Signatur: getTooltipFromItem(MinecraftClient, ItemStack)
                        if (params.length == 2 &&
                            net.minecraft.client.Minecraft.class.isAssignableFrom(params[0]) &&
                            net.minecraft.world.item.ItemStack.class.isAssignableFrom(params[1])) {
                            @SuppressWarnings("unchecked")
                            java.util.List<net.minecraft.network.chat.Component> result = (java.util.List<net.minecraft.network.chat.Component>) method.invoke(screen, client, stack);
                            tooltip = result;
                            break;
                        }
                        
                        // Signatur: getTooltipFromItem(ItemStack)
                        if (params.length == 1 &&
                            net.minecraft.world.item.ItemStack.class.isAssignableFrom(params[0])) {
                            @SuppressWarnings("unchecked")
                            java.util.List<net.minecraft.network.chat.Component> result = (java.util.List<net.minecraft.network.chat.Component>) method.invoke(screen, stack);
                            tooltip = result;
                            break;
                        }
                    } catch (Exception e) {
                        // Versuche nächste Signatur
                        continue;
                    }
                }
            } catch (Exception e) {
                // Ignoriere Fehler
            }
            
            if (tooltip != null && !tooltip.isEmpty()) {
                // Nimm erste Zeile
                String firstLine = tooltip.get(0).getString();
                
                // Entferne Formatierungscodes
                firstLine = firstLine.replaceAll("§[0-9a-fk-or]", "").trim();
                
                // Entferne " - [Bauplan]" oder ähnliche Suffixe
                // Pattern: "name - [Bauplan]" -> "name"
                // Unterstützt auch Varianten wie "name - [Bauplan] [e1]" -> "name"
                String itemName = firstLine.replaceAll("\\s*-\\s*\\[.*?\\](\\s*\\[.*?\\])?$", "").trim();
                
                return itemName;
            }
        } catch (Exception e) {
            // Ignoriere Fehler
        }
        return null;
    }

    /**
     * Extrahiert "found at" (Floor) Information aus Tooltip
     * Sucht nach purple/magenta Zeilen, die Floor-Informationen enthalten
     * @param screen Der Screen
     * @param client Der Minecraft Client
     * @param stack Der ItemStack
     * @return Floor-Information oder null wenn nicht gefunden
     */
    private static String extractFoundAtFromTooltip(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen, Minecraft client, net.minecraft.world.item.ItemStack stack) {
        try {
            // Hole Tooltip über Reflection (ähnlich wie extractItemNameFromTooltip)
            java.util.List<net.minecraft.network.chat.Component> tooltip = null;
            
            try {
                java.lang.reflect.Method[] methods = screen.getClass().getDeclaredMethods();
                for (java.lang.reflect.Method method : methods) {
                    if (!"getTooltipFromItem".equals(method.getName())) {
                        continue;
                    }
                    
                    Class<?>[] params = method.getParameterTypes();
                    method.setAccessible(true);
                    
                    try {
                        // Signatur: getTooltipFromItem(MinecraftClient, ItemStack)
                        if (params.length == 2 &&
                            net.minecraft.client.Minecraft.class.isAssignableFrom(params[0]) &&
                            net.minecraft.world.item.ItemStack.class.isAssignableFrom(params[1])) {
                            @SuppressWarnings("unchecked")
                            java.util.List<net.minecraft.network.chat.Component> result = (java.util.List<net.minecraft.network.chat.Component>) method.invoke(screen, client, stack);
                            tooltip = result;
                            break;
                        }
                        
                        // Signatur: getTooltipFromItem(ItemStack)
                        if (params.length == 1 &&
                            net.minecraft.world.item.ItemStack.class.isAssignableFrom(params[0])) {
                            @SuppressWarnings("unchecked")
                            java.util.List<net.minecraft.network.chat.Component> result = (java.util.List<net.minecraft.network.chat.Component>) method.invoke(screen, stack);
                            tooltip = result;
                            break;
                        }
                    } catch (Exception e) {
                        // Versuche nächste Signatur
                        continue;
                    }
                }
            } catch (Exception e) {
                // Ignoriere Fehler
            }
            
            if (tooltip != null && !tooltip.isEmpty()) {
                // Durchsuche alle Tooltip-Zeilen nach Floor-Information
                // Floor-Informationen sind typischerweise in purple/magenta (0xFFFF00FF)
                for (net.minecraft.network.chat.Component line : tooltip) {
                    if (line != null) {
                        String lineText = line.getString();
                        if (lineText != null && !lineText.isEmpty()) {
                            // Prüfe ob die Zeile purple/magenta ist (foundAt Information)
                            // Die Farbe wird in ItemViewerGrid als 0xFFFF00FF gesetzt
                            net.minecraft.network.chat.Style style = line.getStyle();
                            if (style != null && style.getColor() != null) {
                                int color = style.getColor().getValue();
                                // Prüfe ob es purple/magenta ist (0xFFFF00FF oder ähnlich)
                                if (color == 0xFFFF00FF || (color >= 0xFF8000FF && color <= 0xFFFF00FF)) {
                                    // Entferne Formatierungscodes
                                    String cleanLine = lineText.replaceAll("§[0-9a-fk-or]", "").trim();
                                    if (!cleanLine.isEmpty()) {
                                        return cleanLine;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignoriere Fehler
        }
        return null;
    }

    /**
     * Bereinigt einen Item-Namen für die Suche in items.json
     * Entfernt Formatierungszeichen, chinesische Zeichen und "- [Bauplan]" Suffixe
     */
    private static String cleanItemNameForLookup(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return itemName;
        }
        
        // 1. Entferne alle Minecraft-Formatierungscodes (§[0-9a-fk-or])
        itemName = itemName.replaceAll("§[0-9a-fk-or]", "");

        // 1b. Schmiedegrad / Suffix in eckigen Klammern: "Name [Drachengeschmiedet]" → "Name"
        int bracketIdx = itemName.indexOf('[');
        if (bracketIdx >= 0) {
            itemName = itemName.substring(0, bracketIdx).trim();
        }
        
        // 2. Entferne " - [Bauplan]" oder ähnliche Suffixe komplett (inkl. Bindestrich)
        // Pattern: "name - [Bauplan]" -> "name"
        // Unterstützt auch Varianten wie "name - [Bauplan] [e1]" -> "name"
        // Entferne zuerst alles ab " - [" bis zum Ende (inkl. mehrerer eckiger Klammern)
        itemName = itemName.replaceAll("\\s*-\\s*\\[.*$", "");
        
        // 3. Entferne chinesische Formatierungszeichen (㔚㔘 etc.) am Anfang und Ende
        // Diese Zeichen sind typischerweise am Anfang und Ende des Namens
        // Entferne alle Zeichen, die nicht zu den erlaubten Zeichen gehören
        // Erlaubt: Lateinische Buchstaben (inkl. Umlaute), Zahlen, Leerzeichen, Bindestriche
        // Entfernt: Chinesische Zeichen und andere Sonderzeichen
        StringBuilder cleaned = new StringBuilder();
        for (char c : itemName.toCharArray()) {
            // Erlaube nur: ASCII-Buchstaben, Umlaute (ä, ö, ü, ß), Zahlen, Leerzeichen, Bindestriche
            // Chinesische Zeichen haben Unicode-Bereiche außerhalb des lateinischen Alphabets
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                (c >= '0' && c <= '9') || 
                c == ' ' || c == '-' ||
                c == 'ä' || c == 'ö' || c == 'ü' || c == 'Ä' || c == 'Ö' || c == 'Ü' || c == 'ß' ||
                // Erlaube auch andere europäische Sonderzeichen (é, è, ê, etc.)
                (c >= 0x00C0 && c <= 0x024F)) { // Lateinische erweiterte Zeichen
                cleaned.append(c);
            }
        }
        itemName = cleaned.toString();
        
        // 4. Entferne mehrfache Leerzeichen
        itemName = itemName.replaceAll("\\s+", " ");
        
        // 5. Trimme Leerzeichen
        itemName = itemName.trim();
        
        return itemName;
    }

    /** Anzeigename: Layout-Glyphen/Spacer weg, Schmiedegrad in [] bleibt erhalten. */
    private static String cleanItemNameForDisplay(String itemName) {
        return net.felix.chat.ShareTextCleaner.cleanDisplayName(itemName);
    }
}

package net.felix.utilities.Other.Clipboard;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.utilities.Overall.ZeichenUtility;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erkennt Ressourcen-Sammlungen im Legend+-Menü über das Item {@code [Sammler]}.
 * Format: {@code Amboss [Verbessertes Zahnrad]: 129/307 (283) [+1]}
 * Nach Linksklick wird kurz geprüft, ob der abholbare Wert gesunken ist;
 * dann wird die volle Menge vor dem Klick addiert (nicht nur die Differenz).
 */
public final class LegendPlusSammlerCollector {

    /**
     * Amboss/Schmelzofen [Ressource]: abholbar/max
     */
    private static final Pattern SAMMLER_RESOURCE_PATTERN = Pattern.compile(
            "^(Amboss|Schmelzofen)\\s+\\[([^\\]]+)]\\s*:\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)",
            Pattern.CASE_INSENSITIVE);

    /** Kurzes Fenster (~250 ms), damit neue Produktion den Tooltip nicht verfälscht. */
    private static final int VERIFY_TICKS = 5;

    private static Map<String, Long> pendingBeforeClick = null;
    private static int pendingTicksRemaining = 0;

    private LegendPlusSammlerCollector() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(LegendPlusSammlerCollector::onClientTick);
    }

    /**
     * Vor dem eigentlichen Slot-Klick aufrufen (AbstractContainerScreen.mouseClicked).
     */
    public static void handleMouseClick(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button, int screenX, int screenY) {
        if (button != 0 || screen == null) {
            return;
        }

        String title = screen.getTitle() != null ? screen.getTitle().getString() : "";
        if (!ZeichenUtility.containsLegendPlusUiBackground(title)) {
            return;
        }

        Slot hoveredSlot = findHoveredSlot(screen, mouseX, mouseY, screenX, screenY);
        if (hoveredSlot == null || !hoveredSlot.hasItem()) {
            return;
        }

        ItemStack stack = hoveredSlot.getItem();
        if (!isSammlerItem(stack)) {
            return;
        }

        List<Component> tooltip = getTooltip(stack);
        Map<String, Long> snapshot = parseSammlerResources(tooltip);
        if (snapshot.isEmpty()) {
            return;
        }

        pendingBeforeClick = snapshot;
        pendingTicksRemaining = VERIFY_TICKS;
    }

    private static void onClientTick(net.minecraft.client.Minecraft client) {
        if (pendingBeforeClick == null || pendingBeforeClick.isEmpty() || pendingTicksRemaining <= 0) {
            clearPending();
            return;
        }

        if (client.player == null || client.level == null || !(client.screen instanceof AbstractContainerScreen<?> handledScreen)) {
            pendingTicksRemaining--;
            if (pendingTicksRemaining <= 0) {
                clearPending();
            }
            return;
        }

        String title = handledScreen.getTitle() != null ? handledScreen.getTitle().getString() : "";
        if (!ZeichenUtility.containsLegendPlusUiBackground(title)) {
            clearPending();
            return;
        }

        ItemStack sammlerStack = findSammlerStack(handledScreen);
        if (sammlerStack == null) {
            pendingTicksRemaining--;
            if (pendingTicksRemaining <= 0) {
                clearPending();
            }
            return;
        }

        Map<String, Long> current = parseSammlerResources(getTooltip(sammlerStack));
        Map<String, Long> collected = new HashMap<>();

        for (Map.Entry<String, Long> entry : pendingBeforeClick.entrySet()) {
            String resourceName = entry.getKey();
            long beforeCollectable = entry.getValue();
            if (beforeCollectable <= 0) {
                continue;
            }
            if (!current.containsKey(resourceName)) {
                continue;
            }
            long afterCollectable = current.get(resourceName);
            if (afterCollectable < beforeCollectable) {
                collected.put(resourceName, beforeCollectable);
            }
        }

        if (!collected.isEmpty()) {
            CollectedMaterialsResourcesStorage.addResources(collected);
            pendingBeforeClick.keySet().removeAll(collected.keySet());
        }

        pendingTicksRemaining--;
        if (pendingBeforeClick.isEmpty() || pendingTicksRemaining <= 0) {
            clearPending();
        }
    }

    private static void clearPending() {
        pendingBeforeClick = null;
        pendingTicksRemaining = 0;
    }

    static Map<String, Long> parseSammlerResources(List<Component> tooltip) {
        Map<String, Long> resources = new HashMap<>();
        if (tooltip == null) {
            return resources;
        }
        for (Component line : tooltip) {
            String cleanLine = cleanTooltipLine(line.getString());
            if (cleanLine.isEmpty()) {
                continue;
            }
            Matcher matcher = SAMMLER_RESOURCE_PATTERN.matcher(cleanLine);
            if (matcher.find()) {
                String resourceName = cleanTooltipLine(matcher.group(2));
                long collectable = parseAmount(matcher.group(3));
                if (!resourceName.isEmpty()) {
                    resources.put(resourceName, collectable);
                }
            }
        }
        return resources;
    }

    private static boolean isSammlerItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String name = cleanTooltipLine(stack.getHoverName().getString());
        return "[Sammler]".equalsIgnoreCase(name);
    }

    private static ItemStack findSammlerStack(AbstractContainerScreen<?> screen) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot.hasItem() && isSammlerItem(slot.getItem())) {
                return slot.getItem();
            }
        }
        return null;
    }

    private static Slot findHoveredSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int screenX, int screenY) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot.x + screenX <= mouseX && mouseX < slot.x + screenX + 16
                    && slot.y + screenY <= mouseY && mouseY < slot.y + screenY + 16) {
                return slot;
            }
        }
        return null;
    }

    private static List<Component> getTooltip(ItemStack stack) {
        List<Component> tooltip = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return tooltip;
        }
        try {
            tooltip.add(stack.getHoverName());
            var loreComponent = stack.get(net.minecraft.core.component.DataComponents.LORE);
            if (loreComponent != null) {
                tooltip.addAll(loreComponent.lines());
            }
        } catch (Exception ignored) {
            // Silent error handling
        }
        return tooltip;
    }

    private static String cleanTooltipLine(String lineText) {
        if (lineText == null) {
            return "";
        }
        return lineText.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF]", "")
                .trim();
    }

    private static long parseAmount(String amountText) {
        if (amountText == null) {
            return 0L;
        }
        String cleaned = amountText.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}

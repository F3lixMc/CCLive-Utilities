package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.felix.utilities.Overall.ZeichenUtility;

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
     * Vor dem eigentlichen Slot-Klick aufrufen (HandledScreen.mouseClicked).
     */
    public static void handleMouseClick(HandledScreen<?> screen, double mouseX, double mouseY, int button, int screenX, int screenY) {
        if (button != 0 || screen == null) {
            return;
        }

        String title = screen.getTitle() != null ? screen.getTitle().getString() : "";
        if (!ZeichenUtility.containsLegendPlusUiBackground(title)) {
            return;
        }

        Slot hoveredSlot = findHoveredSlot(screen, mouseX, mouseY, screenX, screenY);
        if (hoveredSlot == null || !hoveredSlot.hasStack()) {
            return;
        }

        ItemStack stack = hoveredSlot.getStack();
        if (!isSammlerItem(stack)) {
            return;
        }

        List<Text> tooltip = getTooltip(stack);
        Map<String, Long> snapshot = parseSammlerResources(tooltip);
        if (snapshot.isEmpty()) {
            return;
        }

        pendingBeforeClick = snapshot;
        pendingTicksRemaining = VERIFY_TICKS;
    }

    private static void onClientTick(net.minecraft.client.MinecraftClient client) {
        if (pendingBeforeClick == null || pendingBeforeClick.isEmpty() || pendingTicksRemaining <= 0) {
            clearPending();
            return;
        }

        if (client.player == null || client.world == null || !(client.currentScreen instanceof HandledScreen<?> handledScreen)) {
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

    static Map<String, Long> parseSammlerResources(List<Text> tooltip) {
        Map<String, Long> resources = new HashMap<>();
        if (tooltip == null) {
            return resources;
        }
        for (Text line : tooltip) {
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
        String name = cleanTooltipLine(stack.getName().getString());
        return "[Sammler]".equalsIgnoreCase(name);
    }

    private static ItemStack findSammlerStack(HandledScreen<?> screen) {
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.hasStack() && isSammlerItem(slot.getStack())) {
                return slot.getStack();
            }
        }
        return null;
    }

    private static Slot findHoveredSlot(HandledScreen<?> screen, double mouseX, double mouseY, int screenX, int screenY) {
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.x + screenX <= mouseX && mouseX < slot.x + screenX + 16
                    && slot.y + screenY <= mouseY && mouseY < slot.y + screenY + 16) {
                return slot;
            }
        }
        return null;
    }

    private static List<Text> getTooltip(ItemStack stack) {
        List<Text> tooltip = new ArrayList<>();
        if (stack == null || stack.isEmpty()) {
            return tooltip;
        }
        try {
            tooltip.add(stack.getName());
            var loreComponent = stack.get(net.minecraft.component.DataComponentTypes.LORE);
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

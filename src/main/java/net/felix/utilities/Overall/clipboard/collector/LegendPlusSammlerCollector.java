package net.felix.utilities.Overall.clipboard.collector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.utilities.Other.glyphs.ZeichenUtility;
import net.felix.utilities.Overall.clipboard.storage.CollectedMaterialsResourcesStorage;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Erkennt Ressourcen-Sammlungen im Legend+-Menü über das Item {@code [Sammler]}.
 * Format: {@code Amboss [Verbessertes Zahnrad] 129/307 (283) [+1]}
 * Der Doppelpunkt der älteren Zeile bleibt gültig. Gutgeschrieben wird die Menge
 * erst, wenn sie innerhalb von 60 Ticks auf 0 fällt.
 */
public final class LegendPlusSammlerCollector {

    static final String EMPTY_CONTAINERS_HINT = "[Linksklick] Alle Container leeren.";

    /**
     * Amboss/Schmelzofen [Ressource] abholbar/max, mit oder ohne Doppelpunkt.
     */
    private static final Pattern SAMMLER_RESOURCE_PATTERN = Pattern.compile(
            "^(Amboss|Schmelzofen)\\s+\\[([^\\]]+)]\\s*:?\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)",
            Pattern.CASE_INSENSITIVE);

    private static final ResourcePickupGate PICKUP = new ResourcePickupGate();

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

        List<String> lines = tooltipLines(getTooltip(stack));
        if (!containsEmptyContainersHint(lines)) {
            return;
        }
        PICKUP.arm("legend", parseSammlerLines(lines));
    }

    private static void onClientTick(net.minecraft.client.Minecraft client) {
        if (!PICKUP.isPending()) {
            return;
        }
        boolean connected = client != null && client.player != null;
        if (!connected || client.gui.screen() == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> handledScreen)) {
            PICKUP.observe(null, Map.of(), connected);
            return;
        }
        String title = handledScreen.getTitle() != null ? handledScreen.getTitle().getString() : "";
        String token = ZeichenUtility.containsLegendPlusUiBackground(title) ? "legend" : "";
        ItemStack sammlerStack = findSammlerStack(handledScreen);
        Map<String, Long> current = sammlerStack == null ? Map.of() : parseSammlerResources(getTooltip(sammlerStack));
        Map<String, Long> collected = collapseByResource(PICKUP.observe(token, current, true, sammlerStack != null));
        if (!collected.isEmpty()) {
            CollectedMaterialsResourcesStorage.addResources(collected);
        }
    }

    static Map<String, Long> parseSammlerResources(List<Component> tooltip) {
        return parseSammlerLines(tooltipLines(tooltip));
    }

    static Map<String, Long> parseSammlerLines(List<String> lines) {
        Map<String, Long> resources = new HashMap<>();
        if (lines == null) {
            return resources;
        }
        for (String cleanLine : lines) {
            if (cleanLine == null || cleanLine.isEmpty()) {
                continue;
            }
            Matcher matcher = SAMMLER_RESOURCE_PATTERN.matcher(cleanLine);
            if (!matcher.find()) {
                continue;
            }
            String resourceName = cleanTooltipLine(matcher.group(2));
            long collectable = parseAmount(matcher.group(3));
            if (!resourceName.isEmpty()) {
                resources.put(lineKey(matcher.group(1), resourceName), collectable);
            }
        }
        return resources;
    }

    static boolean containsEmptyContainersHint(List<String> lines) {
        if (lines == null) {
            return false;
        }
        for (String line : lines) {
            if (line != null && cleanTooltipLine(line).equalsIgnoreCase(EMPTY_CONTAINERS_HINT)) {
                return true;
            }
        }
        return false;
    }

    static String lineKey(String source, String resource) {
        return source.toLowerCase(java.util.Locale.ROOT) + "\u0000" + resource;
    }

    static Map<String, Long> collapseByResource(Map<String, Long> keyed) {
        Map<String, Long> resources = new HashMap<>();
        if (keyed == null) {
            return resources;
        }
        for (Map.Entry<String, Long> entry : keyed.entrySet()) {
            String key = entry.getKey();
            int split = key.indexOf('\u0000');
            String resource = split < 0 ? key : key.substring(split + 1);
            if (resource.isEmpty() || entry.getValue() == null || entry.getValue() <= 0L) {
                continue;
            }
            resources.merge(resource, entry.getValue(), Long::sum);
        }
        return resources;
    }

    private static List<String> tooltipLines(List<Component> tooltip) {
        List<String> lines = new ArrayList<>();
        if (tooltip == null) {
            return lines;
        }
        for (Component line : tooltip) {
            String cleanLine = cleanTooltipLine(line.getString());
            if (!cleanLine.isEmpty()) {
                lines.add(cleanLine);
            }
        }
        return lines;
    }

    private static boolean isSammlerItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String name = cleanTooltipLine(stack.getHoverName().getString());
        return "[Sammler]".equalsIgnoreCase(name);
    }

    static ItemStack findSammlerStack(AbstractContainerScreen<?> screen) {
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

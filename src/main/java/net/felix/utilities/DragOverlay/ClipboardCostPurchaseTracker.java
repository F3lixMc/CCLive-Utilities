package net.felix.utilities.DragOverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.felix.utilities.Overall.ZeichenUtility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Zieht Kosten nach Kauf in Kosten-Inventaren ab.
 * Kauf-Bestätigung: Tooltip-Menge gesunken, geklicktes Item/Tooltip weg oder geändert,
 * oder Kosten-Inventar geschlossen.
 */
public final class ClipboardCostPurchaseTracker {

    private static final int VERIFY_TICKS = 12;
    private static final int INVISIBLE_TICKS_BEFORE_SUBTRACT = 2;

    private static final Map<String, PendingCost> pendingCosts = new HashMap<>();
    private static int ticksRemaining = 0;
    private static boolean pendingIsFishingCraft = false;
    private static int clickedSlotId = -1;
    private static String clickedTooltipSnapshot = "";

    private ClipboardCostPurchaseTracker() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(ClipboardCostPurchaseTracker::onClientTick);
    }

    public static void handleMouseClick(
            HandledScreen<?> screen,
            double mouseX,
            double mouseY,
            int button,
            int screenX,
            int screenY) {
        if (button != 0 || screen == null) {
            return;
        }

        String title = screen.getTitle() != null ? screen.getTitle().getString() : "";
        boolean isFishingCraft = ZeichenUtility.isFishingComponentsCraftTitle(title);
        if (!ClipboardCostInventoryUtility.usesSchmiedCostTooltipFormat(title) && !isFishingCraft) {
            return;
        }

        Slot hoveredSlot = findHoveredSlot(screen, mouseX, mouseY, screenX, screenY);
        if (hoveredSlot == null || !hoveredSlot.hasStack()) {
            return;
        }

        ItemStack stack = hoveredSlot.getStack();
        List<ClipboardCostTooltipParser.CostLine> costs = ClipboardCostTooltipParser.parseTooltip(stack, isFishingCraft);
        if (costs.isEmpty()) {
            return;
        }

        pendingIsFishingCraft = isFishingCraft;
        clickedSlotId = hoveredSlot.id;
        clickedTooltipSnapshot = ClipboardCostTooltipParser.buildTooltipSnapshot(stack);
        pendingCosts.clear();
        for (ClipboardCostTooltipParser.CostLine cost : costs) {
            pendingCosts.put(cost.name(), new PendingCost(
                    cost.name(),
                    cost.current(),
                    cost.required(),
                    cost.isMaterial()));
        }
        ticksRemaining = VERIFY_TICKS;
    }

    private static void onClientTick(net.minecraft.client.MinecraftClient client) {
        if (pendingCosts.isEmpty()) {
            return;
        }

        if (client.player == null || client.world == null) {
            return;
        }

        boolean matchingScreenOpen = false;
        HandledScreen<?> handledScreen = null;
        boolean isFishingCraft = pendingIsFishingCraft;

        if (client.currentScreen instanceof HandledScreen<?> screen) {
            handledScreen = screen;
            String title = screen.getTitle() != null ? screen.getTitle().getString() : "";
            isFishingCraft = ZeichenUtility.isFishingComponentsCraftTitle(title);
            matchingScreenOpen = ClipboardCostInventoryUtility.usesSchmiedCostTooltipFormat(title)
                    || isFishingCraft;
        }

        if (matchingScreenOpen && handledScreen != null) {
            ticksRemaining = VERIFY_TICKS;
            Map<String, Long> visibleCurrents = ClipboardCostTooltipParser.scanInventoryCurrents(
                    handledScreen, isFishingCraft);
            boolean clickedItemPurchase = isClickedItemPurchase(handledScreen);
            processPendingCosts(visibleCurrents, false, clickedItemPurchase);
        } else {
            processPendingCosts(Map.of(), true, false);
            ticksRemaining--;
        }

        if (pendingCosts.isEmpty() || ticksRemaining <= 0) {
            clearPending();
        }
    }

    private static void processPendingCosts(
            Map<String, Long> visibleCurrents,
            boolean costInventoryClosed,
            boolean clickedItemPurchase) {
        boolean definitivePurchase = costInventoryClosed || clickedItemPurchase;

        Iterator<Map.Entry<String, PendingCost>> iterator = pendingCosts.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingCost pending = iterator.next().getValue();
            if (pending.applied) {
                iterator.remove();
                continue;
            }

            Long visibleCurrent = visibleCurrents.get(pending.name);

            if (visibleCurrent != null) {
                pending.invisibleTicks = 0;
                pending.lowestSeenCurrent = Math.min(pending.lowestSeenCurrent, visibleCurrent);
                if (visibleCurrent < pending.currentBefore || definitivePurchase) {
                    pending.purchaseConfirmed = true;
                }
                if (pending.purchaseConfirmed) {
                    applyPurchaseCost(pending);
                    pending.applied = true;
                    iterator.remove();
                }
                continue;
            }

            pending.invisibleTicks++;
            if (definitivePurchase) {
                pending.purchaseConfirmed = true;
            }

            if (definitivePurchase && pending.purchaseConfirmed) {
                applyPurchaseCost(pending);
                pending.applied = true;
                iterator.remove();
                continue;
            }

            if (pending.invisibleTicks < INVISIBLE_TICKS_BEFORE_SUBTRACT) {
                continue;
            }

            if (!pending.purchaseConfirmed && pending.lowestSeenCurrent < pending.currentBefore) {
                pending.purchaseConfirmed = true;
            }

            if (pending.purchaseConfirmed) {
                applyPurchaseCost(pending);
                pending.applied = true;
                iterator.remove();
            }
        }
    }

    private static boolean isClickedItemPurchase(HandledScreen<?> screen) {
        if (clickedSlotId < 0 || clickedTooltipSnapshot.isEmpty()) {
            return false;
        }
        Slot slot = findSlotById(screen, clickedSlotId);
        if (slot == null || !slot.hasStack() || slot.getStack().isEmpty()) {
            return true;
        }
        String currentSnapshot = ClipboardCostTooltipParser.buildTooltipSnapshot(slot.getStack());
        return !currentSnapshot.equals(clickedTooltipSnapshot);
    }

    private static void applyPurchaseCost(PendingCost pending) {
        if (pending.lowestSeenCurrent < pending.currentBefore) {
            if (pending.isMaterial) {
                CollectedMaterialsResourcesStorage.updateMaterial(pending.name, pending.lowestSeenCurrent);
            } else {
                CollectedMaterialsResourcesStorage.updateResource(pending.name, pending.lowestSeenCurrent);
            }
            return;
        }
        if (pending.requiredCost <= 0) {
            return;
        }
        if (pending.isMaterial) {
            CollectedMaterialsResourcesStorage.subtractMaterial(pending.name, pending.requiredCost);
        } else {
            CollectedMaterialsResourcesStorage.subtractResource(pending.name, pending.requiredCost);
        }
    }

    private static void clearPending() {
        pendingCosts.clear();
        ticksRemaining = 0;
        pendingIsFishingCraft = false;
        clickedSlotId = -1;
        clickedTooltipSnapshot = "";
    }

    private static Slot findSlotById(HandledScreen<?> screen, int slotId) {
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.id == slotId) {
                return slot;
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

    private static final class PendingCost {
        final String name;
        final long currentBefore;
        final long requiredCost;
        final boolean isMaterial;
        long lowestSeenCurrent;
        long invisibleTicks;
        boolean purchaseConfirmed;
        boolean applied;

        PendingCost(String name, long currentBefore, long requiredCost, boolean isMaterial) {
            this.name = name;
            this.currentBefore = currentBefore;
            this.requiredCost = requiredCost;
            this.isMaterial = isMaterial;
            this.lowestSeenCurrent = currentBefore;
        }
    }
}

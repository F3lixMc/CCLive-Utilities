package net.felix.utilities.Other;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Debug-Tools zum Analysieren von Display-ähnlichen Entities.
 *
 * - /cclive debug itemdisplay  → schaut auf das anvisierte Entity
 * - (optional) Nearby-Scan     → scanNearby(source) für zukünftige Erweiterung
 */
public final class ItemDisplayDebugUtility {

    private ItemDisplayDebugUtility() {
    }

    /**
     * Dumppt die Daten des anvisierten Display-ähnlichen Entities in den Chat.
     */
    public static int dump(FabricClientCommandSource source) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || client.player == null) {
            source.sendError(Component.literal("§cClient oder Welt nicht verfügbar."));
            return 0;
        }

        var hit = client.hitResult;
        if (hit == null || !(client.hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr)) {
            source.sendError(Component.literal("§cKein Entity im Fadenkreuz."));
            return 0;
        }

        Entity entity = ehr.getEntity();

        // ItemDisplay
        if (entity instanceof Display.ItemDisplay itemDisplay) {
            return dumpItemDisplay(source, itemDisplay);
        }

        // TextDisplay
        if (entity instanceof Display.TextDisplay textDisplay) {
            return dumpTextDisplay(source, textDisplay);
        }

        // ItemFrame
        if (entity instanceof ItemFrame frame) {
            return dumpItemFrame(source, frame);
        }

        // ArmorStand (häufig für Hologramme)
        if (entity instanceof ArmorStand armorStand) {
            return dumpArmorStand(source, armorStand);
        }

        source.sendError(Component.literal("§cUnbekannter Entity-Typ: §f" + entity.getType().toString()));
        return 0;
    }

    // ========= Crosshair-Dumps =========

    private static int dumpItemDisplay(FabricClientCommandSource source, Display.ItemDisplay display) {
        ItemStack stack = display.getItemStack();
        source.sendFeedback(Component.literal("§6=== ItemDisplay Debug ==="));
        source.sendFeedback(Component.literal(posString(display)));
        source.sendFeedback(Component.literal("§7Item: §f" + stack.getHoverName().getString() + " §8x" + stack.getCount()));
        source.sendFeedback(Component.literal("§7Item (ID): §8" + stack.getItem().toString()));
        source.sendFeedback(Component.literal("§7Billboard: §f" + display.getBillboardConstraints()));
        source.sendFeedback(Component.literal("§7Helligkeit: §f" + display.getPackedBrightnessOverride()));
        source.sendFeedback(Component.literal("§7Schatten: §fRadius=" + display.getShadowRadius() + " Stärke=" + display.getShadowStrength()));
        return 1;
    }

    private static int dumpTextDisplay(FabricClientCommandSource source, Display.TextDisplay display) {
        var text = display.getText();
        source.sendFeedback(Component.literal("§6=== TextDisplay Debug ==="));
        source.sendFeedback(Component.literal(posString(display)));
        source.sendFeedback(Component.literal("§7Text: §f" + text.getString()));
        source.sendFeedback(Component.literal("§7Billboard: §f" + display.getBillboardConstraints()));
        source.sendFeedback(Component.literal("§7Linienbreite: §f" + display.getLineWidth()));
        source.sendFeedback(Component.literal("§7Hintergrund: §f" + String.format("#%08X", display.getBackgroundColor())));
        source.sendFeedback(Component.literal("§7Helligkeit: §f" + display.getPackedBrightnessOverride()));
        source.sendFeedback(Component.literal("§7Schatten: §fRadius=" + display.getShadowRadius() + " Stärke=" + display.getShadowStrength()));
        return 1;
    }

    private static int dumpItemFrame(FabricClientCommandSource source, ItemFrame frame) {
        ItemStack stack = frame.getItem();
        source.sendFeedback(Component.literal("§6=== ItemFrame Debug ==="));
        source.sendFeedback(Component.literal(posString(frame)));
        source.sendFeedback(Component.literal("§7Item: §f" + stack.getHoverName().getString() + " §8x" + stack.getCount()));
        source.sendFeedback(Component.literal("§7Item (ID): §8" + stack.getItem().toString()));
        return 1;
    }

    private static int dumpArmorStand(FabricClientCommandSource source, ArmorStand armorStand) {
        source.sendFeedback(Component.literal("§6=== ArmorStand Debug ==="));
        source.sendFeedback(Component.literal(posString(armorStand)));
        if (armorStand.hasCustomName()) {
            source.sendFeedback(Component.literal("§7CustomName: §f" + armorStand.getCustomName().getString()));
        }
        ItemStack head = armorStand.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        ItemStack mainHand = armorStand.getMainHandItem();
        if (!head.isEmpty()) {
            source.sendFeedback(Component.literal("§7Head: §f" + head.getHoverName().getString()));
        }
        if (!mainHand.isEmpty()) {
            source.sendFeedback(Component.literal("§7MainHand: §f" + mainHand.getHoverName().getString()));
        }
        return 1;
    }

    private static String posString(Entity e) {
        return "§7Position: §f" + String.format("x=%.2f y=%.2f z=%.2f", e.getX(), e.getY(), e.getZ());
    }

    // ========= Nearby-Scan =========

    /**
     * Scannt Entities in der Nähe des Spielers und listet relevante Typen auf.
     */
    public static int scanNearby(FabricClientCommandSource source, double radius) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || client.player == null) {
            source.sendError(Component.literal("§cClient oder Welt nicht verfügbar."));
            return 0;
        }

        LocalPlayer player = client.player;
        AABB box = player.getBoundingBox().inflate(radius);

        var world = client.level;
        var entities = world.getEntities(player, box, e ->
            e instanceof Display ||
                e instanceof ItemFrame ||
                e instanceof ArmorStand
        );

        if (entities.isEmpty()) {
            source.sendFeedback(Component.literal("§7Keine relevanten Entities im Umkreis von §f" + radius + "§7 Blöcken."));
            return 1;
        }

        source.sendFeedback(Component.literal("§6=== Nearby Entity Scan (r=" + radius + ") ==="));
        for (Entity e : entities) {
            String type = e.getType().toString();
            String extra = "";
            if (e instanceof Display.ItemDisplay) {
                extra = " §8[ItemDisplay]";
            } else if (e instanceof Display.TextDisplay textDisplay) {
                extra = " §8[TextDisplay: " + safeText(textDisplay.getText()) + "]";
            } else if (e instanceof ItemFrame frame) {
                extra = " §8[ItemFrame: " + safeItem(frame.getItem()) + "]";
            } else if (e instanceof ArmorStand armorStand) {
                extra = " §8[ArmorStand" + (armorStand.hasCustomName() ? (": " + safeText(armorStand.getCustomName())) : "") + "]";
            }
            source.sendFeedback(Component.literal("§7" + type + extra + " §f@ " +
                String.format("x=%.2f y=%.2f z=%.2f", e.getX(), e.getY(), e.getZ())));
        }

        return 1;
    }

    private static String safeText(net.minecraft.network.chat.Component text) {
        return text == null ? "<null>" : text.getString();
    }

    private static String safeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<leer>";
        }
        return stack.getHoverName().getString() + " x" + stack.getCount();
    }
}



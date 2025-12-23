package net.felix.utilities.Other;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            source.sendError(Text.literal("§cClient oder Welt nicht verfügbar."));
            return 0;
        }

        var hit = client.crosshairTarget;
        if (hit == null || !(client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult ehr)) {
            source.sendError(Text.literal("§cKein Entity im Fadenkreuz."));
            return 0;
        }

        Entity entity = ehr.getEntity();

        // ItemDisplay
        if (entity instanceof DisplayEntity.ItemDisplayEntity itemDisplay) {
            return dumpItemDisplay(source, itemDisplay);
        }

        // TextDisplay
        if (entity instanceof DisplayEntity.TextDisplayEntity textDisplay) {
            return dumpTextDisplay(source, textDisplay);
        }

        // ItemFrame
        if (entity instanceof ItemFrameEntity frame) {
            return dumpItemFrame(source, frame);
        }

        // ArmorStand (häufig für Hologramme)
        if (entity instanceof ArmorStandEntity armorStand) {
            return dumpArmorStand(source, armorStand);
        }

        source.sendError(Text.literal("§cUnbekannter Entity-Typ: §f" + entity.getType().toString()));
        return 0;
    }

    // ========= Crosshair-Dumps =========

    private static int dumpItemDisplay(FabricClientCommandSource source, DisplayEntity.ItemDisplayEntity display) {
        ItemStack stack = display.getItemStack();
        source.sendFeedback(Text.literal("§6=== ItemDisplay Debug ==="));
        source.sendFeedback(Text.literal(posString(display)));
        source.sendFeedback(Text.literal("§7Item: §f" + stack.getName().getString() + " §8x" + stack.getCount()));
        source.sendFeedback(Text.literal("§7Item (ID): §8" + stack.getItem().toString()));
        source.sendFeedback(Text.literal("§7Billboard: §f" + display.getBillboardMode()));
        source.sendFeedback(Text.literal("§7Helligkeit: §f" + display.getBrightness()));
        source.sendFeedback(Text.literal("§7Schatten: §fRadius=" + display.getShadowRadius() + " Stärke=" + display.getShadowStrength()));
        return 1;
    }

    private static int dumpTextDisplay(FabricClientCommandSource source, DisplayEntity.TextDisplayEntity display) {
        var text = display.getText();
        source.sendFeedback(Text.literal("§6=== TextDisplay Debug ==="));
        source.sendFeedback(Text.literal(posString(display)));
        source.sendFeedback(Text.literal("§7Text: §f" + text.getString()));
        source.sendFeedback(Text.literal("§7Billboard: §f" + display.getBillboardMode()));
        source.sendFeedback(Text.literal("§7Linienbreite: §f" + display.getLineWidth()));
        source.sendFeedback(Text.literal("§7Hintergrund: §f" + String.format("#%08X", display.getBackground())));
        source.sendFeedback(Text.literal("§7Helligkeit: §f" + display.getBrightness()));
        source.sendFeedback(Text.literal("§7Schatten: §fRadius=" + display.getShadowRadius() + " Stärke=" + display.getShadowStrength()));
        return 1;
    }

    private static int dumpItemFrame(FabricClientCommandSource source, ItemFrameEntity frame) {
        ItemStack stack = frame.getHeldItemStack();
        source.sendFeedback(Text.literal("§6=== ItemFrame Debug ==="));
        source.sendFeedback(Text.literal(posString(frame)));
        source.sendFeedback(Text.literal("§7Item: §f" + stack.getName().getString() + " §8x" + stack.getCount()));
        source.sendFeedback(Text.literal("§7Item (ID): §8" + stack.getItem().toString()));
        return 1;
    }

    private static int dumpArmorStand(FabricClientCommandSource source, ArmorStandEntity armorStand) {
        source.sendFeedback(Text.literal("§6=== ArmorStand Debug ==="));
        source.sendFeedback(Text.literal(posString(armorStand)));
        if (armorStand.hasCustomName()) {
            source.sendFeedback(Text.literal("§7CustomName: §f" + armorStand.getCustomName().getString()));
        }
        ItemStack head = armorStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
        ItemStack mainHand = armorStand.getMainHandStack();
        if (!head.isEmpty()) {
            source.sendFeedback(Text.literal("§7Head: §f" + head.getName().getString()));
        }
        if (!mainHand.isEmpty()) {
            source.sendFeedback(Text.literal("§7MainHand: §f" + mainHand.getName().getString()));
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            source.sendError(Text.literal("§cClient oder Welt nicht verfügbar."));
            return 0;
        }

        ClientPlayerEntity player = client.player;
        Box box = player.getBoundingBox().expand(radius);

        var world = client.world;
        var entities = world.getOtherEntities(player, box, e ->
            e instanceof DisplayEntity ||
                e instanceof ItemFrameEntity ||
                e instanceof ArmorStandEntity
        );

        if (entities.isEmpty()) {
            source.sendFeedback(Text.literal("§7Keine relevanten Entities im Umkreis von §f" + radius + "§7 Blöcken."));
            return 1;
        }

        source.sendFeedback(Text.literal("§6=== Nearby Entity Scan (r=" + radius + ") ==="));
        for (Entity e : entities) {
            String type = e.getType().toString();
            String extra = "";
            if (e instanceof DisplayEntity.ItemDisplayEntity itemDisplay) {
                extra = " §8[ItemDisplay]";
            } else if (e instanceof DisplayEntity.TextDisplayEntity textDisplay) {
                extra = " §8[TextDisplay: " + safeText(textDisplay.getText()) + "]";
            } else if (e instanceof ItemFrameEntity frame) {
                extra = " §8[ItemFrame: " + safeItem(frame.getHeldItemStack()) + "]";
            } else if (e instanceof ArmorStandEntity armorStand) {
                extra = " §8[ArmorStand" + (armorStand.hasCustomName() ? (": " + safeText(armorStand.getCustomName())) : "") + "]";
            }
            source.sendFeedback(Text.literal("§7" + type + extra + " §f@ " +
                String.format("x=%.2f y=%.2f z=%.2f", e.getX(), e.getY(), e.getZ())));
        }

        return 1;
    }

    private static String safeText(net.minecraft.text.Text text) {
        return text == null ? "<null>" : text.getString();
    }

    private static String safeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<leer>";
        }
        return stack.getName().getString() + " x" + stack.getCount();
    }
}



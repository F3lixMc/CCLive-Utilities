package net.felix.devtools;

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
 * Nearby-Scan (Display / ItemFrame / ArmorStand) mit Chat-Ausgabe.
 */
public final class NearbyEntityScan {

    public static final double DEFAULT_RADIUS = 8.0;

    private NearbyEntityScan() {
    }

    public static void scan(double radius) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || client.player == null) {
            send(client, "§cClient oder Welt nicht verfügbar.");
            return;
        }

        LocalPlayer player = client.player;
        AABB box = player.getBoundingBox().inflate(radius);
        var entities = client.level.getEntities(player, box, e ->
            e instanceof Display || e instanceof ItemFrame || e instanceof ArmorStand
        );

        if (entities.isEmpty()) {
            send(client, "§7Keine relevanten Entities im Umkreis von §f" + radius + "§7 Blöcken.");
            return;
        }

        send(client, "§6=== Nearby Entity Scan (r=" + radius + ") ===");
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
                extra = " §8[ArmorStand"
                    + (armorStand.hasCustomName() ? (": " + safeText(armorStand.getCustomName())) : "")
                    + "]";
            }
            send(client, "§7" + type + extra + " §f@ "
                + String.format("x=%.2f y=%.2f z=%.2f", e.getX(), e.getY(), e.getZ()));
        }
    }

    private static void send(Minecraft client, String message) {
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
    }

    private static String safeText(Component text) {
        return text == null ? "<null>" : text.getString();
    }

    private static String safeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<leer>";
        }
        return stack.getHoverName().getString() + " x" + stack.getCount();
    }
}

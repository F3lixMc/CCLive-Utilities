package net.felix.devtools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Liest ItemStacks aus dem geöffneten Container (nur Komponenten als Primärquelle).
 */
public final class ItemDataCapture {

    public enum Mode {
        SINGLE,
        CONTAINER
    }

    private static Field leftPosField;
    private static Field topPosField;
    private static boolean fieldsResolved;

    private ItemDataCapture() {
    }

    public static void capture(Mode mode) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            DevToolOutputWriter.notifyPlayer("§cKein Spieler.");
            return;
        }
        if (!(client.screen instanceof AbstractContainerScreen<?> screen)) {
            DevToolOutputWriter.notifyPlayer("§cKein Container-Menü geöffnet.");
            return;
        }

        Inventory playerInv = client.player.getInventory();
        List<CapturedItem> items = new ArrayList<>();

        if (mode == Mode.SINGLE) {
            Slot slot = findSlotUnderMouse(screen, client);
            if (slot == null || !slot.hasItem()) {
                DevToolOutputWriter.notifyPlayer("§cKein Item unter dem Cursor.");
                return;
            }
            items.add(new CapturedItem(slot.index, slot.getItem().copy()));
        } else {
            for (Slot slot : screen.getMenu().slots) {
                if (slot == null || !slot.hasItem()) {
                    continue;
                }
                if (slot.container == playerInv) {
                    continue;
                }
                items.add(new CapturedItem(slot.index, slot.getItem().copy()));
            }
            if (items.isEmpty()) {
                DevToolOutputWriter.notifyPlayer("§cKeine Container-Items gefunden (Spielerinventar ausgeschlossen).");
                return;
            }
        }

        JsonObject capture = buildCaptureJson(client, screen, mode, items);
        DevToolOutputWriter.writeNow(capture, items.size());
    }

    private static JsonObject buildCaptureJson(Minecraft client, AbstractContainerScreen<?> screen,
                                               Mode mode, List<CapturedItem> items) {
        JsonObject root = new JsonObject();
        root.addProperty("timestamp", Instant.now().toString());
        root.addProperty("mode", mode.name());
        root.addProperty("menuTitle", screen.getTitle().getString());
        root.addProperty("menuClass", screen.getClass().getName());

        JsonArray itemsArr = new JsonArray();
        var registryAccess = client.level != null
            ? client.level.registryAccess()
            : client.player.registryAccess();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);

        for (CapturedItem captured : items) {
            itemsArr.add(serializeItem(captured.slotIndex, captured.stack, ops));
        }
        root.add("items", itemsArr);
        return root;
    }

    private static JsonObject serializeItem(int slotIndex, ItemStack stack, RegistryOps<JsonElement> ops) {
        JsonObject obj = new JsonObject();
        obj.addProperty("slot", slotIndex);
        obj.addProperty("itemId", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        obj.addProperty("count", stack.getCount());

        // Primärquelle: unveränderte Komponenten via Codec
        try {
            var encoded = ItemStack.CODEC.encodeStart(ops, stack);
            if (encoded.result().isPresent()) {
                obj.add("components", encoded.result().get());
            } else {
                obj.add("components", JsonNull.INSTANCE);
                encoded.error().ifPresent(err -> obj.addProperty("componentsError", err.message()));
            }
        } catch (Exception e) {
            obj.add("components", JsonNull.INSTANCE);
            obj.addProperty("componentsError", e.getMessage());
        }

        addCustomModelData(obj, stack);
        addDerivedHints(obj, stack);
        return obj;
    }

    private static void addCustomModelData(JsonObject obj, ItemStack stack) {
        CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd == null) {
            return;
        }
        JsonObject cmdObj = new JsonObject();
        JsonArray floats = new JsonArray();
        if (cmd.floats() != null) {
            for (Float f : cmd.floats()) {
                floats.add(f);
            }
        }
        JsonArray strings = new JsonArray();
        if (cmd.strings() != null) {
            for (String s : cmd.strings()) {
                strings.add(s);
            }
        }
        JsonArray flags = new JsonArray();
        if (cmd.flags() != null) {
            for (Boolean b : cmd.flags()) {
                flags.add(b);
            }
        }
        JsonArray colors = new JsonArray();
        if (cmd.colors() != null) {
            for (Integer c : cmd.colors()) {
                colors.add(c);
            }
        }
        cmdObj.add("floats", floats);
        cmdObj.add("strings", strings);
        cmdObj.add("flags", flags);
        cmdObj.add("colors", colors);
        obj.add("customModelData", cmdObj);
    }

    /**
     * Abgeleitete Anzeigehinweise — klar getrennt von components, nie als Ersatz.
     */
    private static void addDerivedHints(JsonObject obj, ItemStack stack) {
        JsonObject derived = new JsonObject();
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            derived.addProperty("nameFormatted", customName.getString());
        } else {
            derived.addProperty("nameFormatted", stack.getHoverName().getString());
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        JsonArray loreArr = new JsonArray();
        if (lore != null && lore.lines() != null) {
            for (Component line : lore.lines()) {
                if (line != null) {
                    loreArr.add(line.getString());
                }
            }
        }
        derived.add("loreFormatted", loreArr);
        obj.add("derived", derived);
    }

    public static Slot findSlotUnderMouse(AbstractContainerScreen<?> screen, Minecraft client) {
        double mouseX = client.mouseHandler.xpos()
            * (double) client.getWindow().getGuiScaledWidth()
            / (double) client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos()
            * (double) client.getWindow().getGuiScaledHeight()
            / (double) client.getWindow().getScreenHeight();

        int left = readInt(screen, "leftPos", leftPosField);
        int top = readInt(screen, "topPos", topPosField);
        if (left == Integer.MIN_VALUE || top == Integer.MIN_VALUE) {
            resolveFields();
            left = readInt(screen, "leftPos", leftPosField);
            top = readInt(screen, "topPos", topPosField);
        }
        if (left == Integer.MIN_VALUE) {
            left = 0;
        }
        if (top == Integer.MIN_VALUE) {
            top = 0;
        }

        for (Slot slot : screen.getMenu().slots) {
            if (slot == null) {
                continue;
            }
            int sx = left + slot.x;
            int sy = top + slot.y;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                return slot;
            }
        }
        return null;
    }

    private static void resolveFields() {
        if (fieldsResolved) {
            return;
        }
        try {
            leftPosField = AbstractContainerScreen.class.getDeclaredField("leftPos");
            leftPosField.setAccessible(true);
        } catch (Exception ignored) {
        }
        try {
            topPosField = AbstractContainerScreen.class.getDeclaredField("topPos");
            topPosField.setAccessible(true);
        } catch (Exception ignored) {
        }
        fieldsResolved = true;
    }

    private static int readInt(AbstractContainerScreen<?> screen, String name, Field cached) {
        try {
            Field f = cached;
            if (f == null) {
                f = AbstractContainerScreen.class.getDeclaredField(name);
                f.setAccessible(true);
                if ("leftPos".equals(name)) {
                    leftPosField = f;
                } else if ("topPos".equals(name)) {
                    topPosField = f;
                }
            }
            return f.getInt(screen);
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }

    private record CapturedItem(int slotIndex, ItemStack stack) {
    }
}

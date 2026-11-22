package net.felix.utilities.DragOverlay.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Schreibt die erfasste Taste in die bestehende KeyMapping-Instanz und hält die Sperre,
 * bis diese Taste losgelassen ist.
 */
public final class DetailKeybindActions {

    private static KeyMapping listening;
    private static boolean rejected;

    private DetailKeybindActions() {
    }

    public static void begin(KeyMapping mapping) {
        listening = mapping;
        rejected = false;
        DetailKeybindCapture.begin();
    }

    public static boolean listeningTo(KeyMapping mapping) {
        return DetailKeybindCapture.isCapturing() && mapping != null && mapping == listening;
    }

    public static void onKey(int keyCode) {
        boolean rejectAssign = conflictsWithOtherBinding(keyCode, listening);
        DetailKeybindCapture.Decision decision = DetailKeybindCapture.onKey(keyCode, rejectAssign);
        rejected = decision == DetailKeybindCapture.Decision.CONFLICT;
        if (decision == DetailKeybindCapture.Decision.CONFLICT) {
            return;
        }
        if (decision == DetailKeybindCapture.Decision.ASSIGN && listening != null) {
            applyKey(listening, InputConstants.Type.KEYBOARD.getOrCreate(keyCode));
        } else if (decision == DetailKeybindCapture.Decision.UNBIND && listening != null) {
            applyKey(listening, InputConstants.UNKNOWN);
        }
        if (decision != DetailKeybindCapture.Decision.IGNORE) {
            listening = null;
            ModHotkeyGate.drainAll();
        }
    }

    public static void abort() {
        listening = null;
        rejected = false;
        DetailKeybindCapture.abort();
    }

    public static void tick() {
        if (!DetailKeybindCapture.blocksModHotkeys()) {
            return;
        }
        ModHotkeyGate.drainAll();
        int held = DetailKeybindCapture.heldKey();
        boolean down = held != Integer.MIN_VALUE && InputConstants.isKeyDown(held);
        DetailKeybindCapture.pollRelease(down);
    }

    public static String rowLabel(KeyMapping mapping) {
        if (mapping == null) {
            return "—";
        }
        String name = Component.translatable(mapping.getName()).getString();
        boolean thisRow = listeningTo(mapping);
        String binding = DetailKeybindCapture.bindingLabel(
                thisRow,
                mapping.isUnbound(),
                mapping.getTranslatedKeyMessage().getString(),
                thisRow && rejected);
        return name + ": " + binding;
    }

    /**
     * Andere Funktion nutzt die Taste schon.
     * Dieselbe Belegung zählt nicht, auch wenn Controls und das Panel zwei Instanzen davon halten.
     */
    static boolean conflictsWithOtherBinding(int keyCode, KeyMapping editing) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null || client.options.keyMappings == null) {
            return false;
        }
        return conflictsWith(keyCode, editing, java.util.Arrays.asList(client.options.keyMappings));
    }

    static boolean conflictsWith(int keyCode, KeyMapping editing, Iterable<KeyMapping> mappings) {
        InputConstants.Key pressed = InputConstants.Type.KEYBOARD.getOrCreate(keyCode);
        for (KeyMapping mapping : mappings) {
            if (mapping == null || sameBinding(editing, mapping) || mapping.getCategory() == KeyMapping.Category.DEBUG) {
                continue;
            }
            try {
                if (mapping.isUnbound()) {
                    continue;
                }
            } catch (Exception ignored) {
                continue;
            }
            try {
                if (mapping.matches(pressed)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static void applyKey(KeyMapping editing, InputConstants.Key key) {
        String name = editing.getName();
        editing.setKey(key);
        KeyMapping canonical = KeyMapping.get(name);
        if (canonical != null && canonical != editing) {
            canonical.setKey(key);
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.options != null && client.options.keyMappings != null) {
            for (KeyMapping mapping : client.options.keyMappings) {
                if (mapping != null && mapping != editing && name.equals(mapping.getName())) {
                    mapping.setKey(key);
                }
            }
        }
        KeyMapping.resetMapping();
        save();
    }

    private static boolean sameBinding(KeyMapping editing, KeyMapping other) {
        if (editing == null || other == null) {
            return false;
        }
        if (editing == other) {
            return true;
        }
        String name = editing.getName();
        return name != null && name.equals(other.getName());
    }

    private static void save() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.options != null) {
            client.options.save();
        }
    }
}

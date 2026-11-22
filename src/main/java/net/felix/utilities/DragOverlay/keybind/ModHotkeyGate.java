package net.felix.utilities.DragOverlay.keybind;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Verwirft gezählte Klicks, solange die Keybind-Erfassung die Mod-Hotkeys sperrt.
 */
public final class ModHotkeyGate {

    private ModHotkeyGate() {
    }

    public static boolean blocks() {
        return DetailKeybindCapture.blocksModHotkeys();
    }

    public static void discard(KeyMapping mapping) {
        if (mapping == null) {
            return;
        }
        while (mapping.consumeClick()) {
            // gezählten Druck verwerfen
        }
    }

    public static void drainAll() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null || client.options.keyMappings == null) {
            return;
        }
        for (KeyMapping mapping : client.options.keyMappings) {
            discard(mapping);
        }
    }
}

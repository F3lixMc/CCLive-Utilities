package net.felix.devtools;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/**
 * Versteckte Hotkeys ohne KeyMapping-Registrierung.
 * Ein physischer Tastendruck = eine Auslöserunde (Dedup Tick + Screen).
 */
public final class DevToolsHotkeyDispatcher {

    /** Keys, die in diesem Press-Zyklus bereits ausgelöst wurden. */
    private static final Set<Integer> handledWhileDown = new HashSet<>();

    private static boolean capturingHotkey;
    private static String capturingToolId;
    private static Runnable onCaptureDone;

    /** Letzter Hotkey-Konflikt (für GUI-Hover, nicht Chat). */
    private static String hotkeyConflictToolId;
    private static String hotkeyConflictMessage;

    private DevToolsHotkeyDispatcher() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(DevToolsHotkeyDispatcher::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DevToolsAccess.invalidate());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> DevToolsAccess.invalidate());
    }

    public static boolean isCapturingHotkey() {
        return capturingHotkey;
    }

    public static String getCapturingToolId() {
        return capturingToolId;
    }

    public static void beginHotkeyCapture(String toolId, Runnable onDone) {
        capturingHotkey = true;
        capturingToolId = toolId;
        onCaptureDone = onDone;
        clearHotkeyConflict();
    }

    public static void cancelHotkeyCapture() {
        capturingHotkey = false;
        capturingToolId = null;
        onCaptureDone = null;
    }

    public static String getHotkeyConflictMessage(String toolId) {
        if (toolId == null || hotkeyConflictToolId == null) {
            return null;
        }
        if (!toolId.equals(hotkeyConflictToolId)) {
            return null;
        }
        return hotkeyConflictMessage;
    }

    public static void clearHotkeyConflict() {
        hotkeyConflictToolId = null;
        hotkeyConflictMessage = null;
    }

    /**
     * Screen-Hook (Inventar o. ä.). Gibt true zurück wenn der Druck verarbeitet wurde.
     */
    public static boolean handleScreenKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!DevToolsAccess.isAllowed()) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && capturingHotkey) {
            cancelHotkeyCapture();
            return true;
        }
        if (capturingHotkey) {
            return finishCapture(keyCode);
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.screen instanceof DevToolsScreen) {
            return false; // GUI offen: keine Tool-Auslösung, nur Capture
        }
        if (shouldIgnoreInput()) {
            return false;
        }
        return tryFire(keyCode);
    }

    private static void onClientTick(Minecraft client) {
        if (!DevToolsAccess.isAllowed() || client == null || client.getWindow() == null) {
            handledWhileDown.clear();
            return;
        }
        if (capturingHotkey || shouldIgnoreInput()) {
            syncReleasedKeys(client);
            return;
        }
        // Wenn ein Screen offen ist, kommt der Press über ScreenMixin — Tick pollt nur Welt
        if (client.screen != null && !(client.screen instanceof DevToolsScreen)) {
            // Inventar: Screen-Hook feuert; Tick hält nur Release-Tracking
            syncReleasedKeys(client);
            return;
        }

        Set<Integer> boundKeys = collectBoundEnabledKeys();
        for (Integer keyCode : boundKeys) {
            boolean down = InputConstants.isKeyDown(client.getWindow(), keyCode);
            if (down) {
                tryFire(keyCode);
            } else {
                handledWhileDown.remove(keyCode);
            }
        }
        // Auch Keys freigeben die nicht mehr gebunden sind
        handledWhileDown.removeIf(k -> !boundKeys.contains(k) && !InputConstants.isKeyDown(client.getWindow(), k));
    }

    private static void syncReleasedKeys(Minecraft client) {
        if (client.getWindow() == null) {
            handledWhileDown.clear();
            return;
        }
        handledWhileDown.removeIf(k -> !InputConstants.isKeyDown(client.getWindow(), k));
    }

    /**
     * Ein Press = eine Runde: alle enabled Tools mit dieser Taste, je einmal.
     */
    private static boolean tryFire(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode < 0) {
            return false;
        }
        if (handledWhileDown.contains(keyCode)) {
            return false;
        }

        boolean any = false;
        for (DevTool tool : DevToolsRegistry.all()) {
            if (!DevToolsStateStore.isEnabled(tool.id())) {
                continue;
            }
            Integer bound = DevToolsStateStore.getHotkey(tool.id());
            if (bound != null && bound == keyCode) {
                any = true;
                break;
            }
        }
        if (!any) {
            return false;
        }

        handledWhileDown.add(keyCode);
        for (DevTool tool : DevToolsRegistry.all()) {
            if (!DevToolsStateStore.isEnabled(tool.id())) {
                continue;
            }
            Integer bound = DevToolsStateStore.getHotkey(tool.id());
            if (bound != null && bound == keyCode) {
                try {
                    tool.execute();
                } catch (Exception e) {
                    notify("§cDevTool §f" + tool.id() + "§c: " + e.getMessage());
                }
            }
        }
        return true;
    }

    private static boolean finishCapture(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancelHotkeyCapture();
            return true;
        }
        // Backspace / Delete = unbinden
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            if (capturingToolId != null) {
                DevToolsStateStore.setHotkey(capturingToolId, null);
            }
            clearHotkeyConflict();
            Runnable done = onCaptureDone;
            cancelHotkeyCapture();
            if (done != null) {
                done.run();
            }
            return true;
        }

        if (isModifierOnly(keyCode)) {
            return true;
        }

        if (conflictsWithVanilla(keyCode)) {
            hotkeyConflictToolId = capturingToolId;
            hotkeyConflictMessage = "Hotkey-Konflikt mit Vanilla/Mod-Steuerung.";
            // Capture bleibt aktiv — erneuter Versuch möglich; Hinweis nur im Hover
            return true;
        }

        if (capturingToolId != null) {
            DevToolsStateStore.setHotkey(capturingToolId, keyCode);
        }
        clearHotkeyConflict();
        Runnable done = onCaptureDone;
        cancelHotkeyCapture();
        if (done != null) {
            done.run();
        }
        return true;
    }

    public static boolean conflictsWithVanilla(int keyCode) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            return false;
        }
        KeyEvent probe = new KeyEvent(keyCode, -1, 0);
        for (KeyMapping mapping : client.options.keyMappings) {
            if (mapping == null) {
                continue;
            }
            try {
                if (mapping.isUnbound()) {
                    continue;
                }
            } catch (Exception ignored) {
                // Fallback: unbekannt
            }
            try {
                if (mapping.matches(probe)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static String formatKey(Integer keyCode) {
        if (keyCode == null) {
            return "—";
        }
        try {
            return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
        } catch (Exception e) {
            return "Key" + keyCode;
        }
    }

    private static Set<Integer> collectBoundEnabledKeys() {
        Set<Integer> keys = new HashSet<>();
        for (DevTool tool : DevToolsRegistry.all()) {
            if (!DevToolsStateStore.isEnabled(tool.id())) {
                continue;
            }
            Integer k = DevToolsStateStore.getHotkey(tool.id());
            if (k != null) {
                keys.add(k);
            }
        }
        return keys;
    }

    private static boolean shouldIgnoreInput() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return true;
        }
        if (client.screen instanceof ChatScreen) {
            return true;
        }
        if (client.screen instanceof DevToolsScreen) {
            return true;
        }
        Screen screen = client.screen;
        if (screen != null) {
            try {
                var focused = screen.getFocused();
                if (focused instanceof EditBox) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        if (net.felix.utilities.ItemViewer.ItemViewerUtility.isTextFieldFocusedPublic(client)) {
            return true;
        }
        return false;
    }

    private static boolean isModifierOnly(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
            || keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
            || keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT
            || keyCode == GLFW.GLFW_KEY_LEFT_SUPER || keyCode == GLFW.GLFW_KEY_RIGHT_SUPER;
    }

    private static void notify(String msg) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal(msg));
        }
    }
}

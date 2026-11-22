package net.felix.utilities.DragOverlay;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Erfassung wie in den Devtools, plus eine Nachsperre bis die Taste losgelassen ist.
 * Die Sperre gilt für alle normalen Mod-Hotkeys, nicht nur für die bearbeitete Belegung.
 */
public final class DetailKeybindCapture {

    public enum Decision {
        CANCEL,
        UNBIND,
        IGNORE,
        ASSIGN,
        /** Taste ist schon einer anderen Belegung zugeordnet. Erfassung bleibt offen. */
        CONFLICT
    }

    private static boolean capturing;
    private static boolean holding;
    private static int heldKey = Integer.MIN_VALUE;

    private DetailKeybindCapture() {
    }

    public static void reset() {
        capturing = false;
        holding = false;
        heldKey = Integer.MIN_VALUE;
    }

    public static void begin() {
        capturing = true;
    }

    public static boolean isCapturing() {
        return capturing;
    }

    public static boolean blocksModHotkeys() {
        return capturing || holding;
    }

    public static int heldKey() {
        return heldKey;
    }

    public static Decision onKey(int keyCode) {
        return onKey(keyCode, false);
    }

    /**
     * @param rejectAssign die Taste ist schon belegt; dann bleibt die Erfassung offen und es wird nichts geschrieben
     */
    public static Decision onKey(int keyCode, boolean rejectAssign) {
        if (!capturing) {
            return Decision.IGNORE;
        }
        if (keyCode == InputConstants.KEY_ESCAPE) {
            finish(keyCode);
            return Decision.CANCEL;
        }
        if (keyCode == InputConstants.KEY_BACKSPACE || keyCode == InputConstants.KEY_DELETE) {
            finish(keyCode);
            return Decision.UNBIND;
        }
        if (isModifier(keyCode)) {
            return Decision.IGNORE;
        }
        if (rejectAssign) {
            return Decision.CONFLICT;
        }
        finish(keyCode);
        return Decision.ASSIGN;
    }

    /** Panel zu: laufende Erfassung abbrechen. Eine schon gehaltene Taste bleibt gesperrt. */
    public static void abort() {
        capturing = false;
    }

    public static void pollRelease(boolean keyDown) {
        if (!holding) {
            return;
        }
        if (!keyDown) {
            holding = false;
            heldKey = Integer.MIN_VALUE;
        }
    }

    public static String bindingLabel(boolean thisRowCapturing, boolean unbound, String translated) {
        return bindingLabel(thisRowCapturing, unbound, translated, false);
    }

    public static String bindingLabel(boolean thisRowCapturing, boolean unbound, String translated, boolean rejected) {
        if (thisRowCapturing) {
            return rejected ? "Belegt" : "...";
        }
        if (unbound || translated == null || translated.isBlank()) {
            return "—";
        }
        return translated;
    }

    private static void finish(int keyCode) {
        capturing = false;
        holding = true;
        heldKey = keyCode;
    }

    private static boolean isModifier(int keyCode) {
        return keyCode == InputConstants.KEY_LSHIFT || keyCode == InputConstants.KEY_RSHIFT
                || keyCode == InputConstants.KEY_LCONTROL || keyCode == InputConstants.KEY_RCONTROL
                || keyCode == InputConstants.KEY_LALT || keyCode == InputConstants.KEY_RALT
                || keyCode == InputConstants.KEY_LGUI || keyCode == InputConstants.KEY_RGUI;
    }
}

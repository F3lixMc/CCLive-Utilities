package net.felix.devtools;

import net.minecraft.resources.Identifier;

import java.util.List;

public final class ItemDataReaderDevTool implements DevTool {

    public static final String ID = "itemdata_reader";

    public static final DevToolMode MODE_SINGLE = new DevToolMode(
        "single", "M1", "Modus 1: Single Item Scan");
    public static final DevToolMode MODE_CONTAINER = new DevToolMode(
        "container", "M2", "Modus 2: Menüs Items Scan");

    private static final List<DevToolMode> MODES = List.of(MODE_SINGLE, MODE_CONTAINER);

    private static final Identifier ICON = Identifier.fromNamespaceAndPath(
        "cclive-utilities", "textures/icons/devtool_itemdata_icon.png");

    private static final List<String> HOVER = List.of(
        "ItemData",
        "Liest ItemData aus Items",
        "aus und schreibt diese",
        "in devtools_output.json",
        "Datei."
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "ItemData";
    }

    @Override
    public String description() {
        return "Read ItemData";
    }

    @Override
    public List<String> hoverLines() {
        return HOVER;
    }

    @Override
    public List<DevToolMode> modes() {
        return MODES;
    }

    @Override
    public Identifier iconTexture() {
        return ICON;
    }

    @Override
    public int iconTextureSize() {
        return 64;
    }

    @Override
    public String iconFallback() {
        return "ID";
    }

    public static DevToolMode resolveMode() {
        String modeId = DevToolsStateStore.getModeId(ID);
        for (DevToolMode mode : MODES) {
            if (mode.id().equals(modeId)) {
                return mode;
            }
        }
        return MODE_SINGLE;
    }

    public static void cycleMode() {
        DevToolMode current = resolveMode();
        int idx = MODES.indexOf(current);
        DevToolMode next = MODES.get((idx + 1) % MODES.size());
        DevToolsStateStore.setModeId(ID, next.id());
    }

    @Override
    public void execute() {
        DevToolMode mode = resolveMode();
        if (MODE_CONTAINER.id().equals(mode.id())) {
            ItemDataCapture.capture(ItemDataCapture.Mode.CONTAINER);
        } else {
            ItemDataCapture.capture(ItemDataCapture.Mode.SINGLE);
        }
    }
}

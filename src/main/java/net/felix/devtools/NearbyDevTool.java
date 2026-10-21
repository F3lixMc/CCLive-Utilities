package net.felix.devtools;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.List;

public final class NearbyDevTool implements DevTool {

    public static final String ID = "nearby";

    private static final Identifier ICON = Identifier.fromNamespaceAndPath(
        "cclive-utilities", "textures/icons/devtool_nearby_icon.png");

    private static final List<String> HOVER = List.of(
        "Nearby",
        "Scant nach Entities in einem",
        "8 Blk Radius und zeigt sie im",
        "Chat an."
    );

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Nearby";
    }

    @Override
    public String description() {
        return "Scan Entities";
    }

    @Override
    public List<String> hoverLines() {
        return HOVER;
    }

    @Override
    public List<DevToolMode> modes() {
        return Collections.emptyList();
    }

    @Override
    public Identifier iconTexture() {
        return ICON;
    }

    @Override
    public String iconFallback() {
        return "NB";
    }

    @Override
    public void execute() {
        NearbyEntityScan.scan(NearbyEntityScan.DEFAULT_RADIUS);
    }
}

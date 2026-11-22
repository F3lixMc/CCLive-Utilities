package net.felix.utilities.Factory.factorylevel;

import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Other.hud.HudOverlayVisibility;
import net.felix.utilities.Other.keybinding.KeyBindingUtility;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Fabriklevel-Tracker. Misst nur in der Fabrik und zeichnet das Overlay über die HUD-Registry.
 */
public final class FactoryLevelTracker {
    public static final int PADDING = 4;
    public static final int LINE_HEIGHT = 12;
    public static final int MIN_OVERLAY_WIDTH = 120;
    private static final long UPDATE_INTERVAL_MS = 1000L;

    private static final FactoryLevelEstimate estimate = new FactoryLevelEstimate();
    private static final List<String> EMPTY_LINES = List.of();
    private static boolean initialized;
    private static String activeDimension;
    private static long lastUpdateAt;
    private static boolean shown;
    private static List<String> cachedLines = EMPTY_LINES;
    private static int cachedWidth = MIN_OVERLAY_WIDTH;
    private static int cachedHeight = PADDING * 2 + LINE_HEIGHT;

    private FactoryLevelTracker() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(FactoryLevelTracker::onClientTick);
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(FactoryLevelTracker::onLevelChange);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            FactoryLevelPresence.reset();
            clearMeasurement();
            note(client, client.level);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            FactoryLevelPresence.reset();
            clearMeasurement();
        });
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("cclive-utilities", "factory_level"),
                FactoryLevelTracker::onHudRender);
        initialized = true;
    }

    public static boolean isInFactory() {
        return FactoryLevelPresence.inFactory();
    }

    public static int unscaledWidth(Minecraft client) {
        return cachedWidth;
    }

    public static int unscaledHeight() {
        return cachedHeight;
    }

    public static int x() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return 0;
        }
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int xOffset = CCLiveUtilitiesConfig.HANDLER.instance().factoryLevelX;
        int unscaledWidth = unscaledWidth(client);
        int baseX = screenWidth - MIN_OVERLAY_WIDTH - xOffset;
        if (baseX < screenWidth / 2) {
            return screenWidth - MIN_OVERLAY_WIDTH - xOffset;
        }
        return screenWidth - unscaledWidth - xOffset;
    }

    public static int y() {
        return CCLiveUtilitiesConfig.HANDLER.instance().factoryLevelY;
    }

    public static void render(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return;
        }
        List<String> lines = cachedLines;
        int width = cachedWidth;
        int height = cachedHeight;
        float scale = CCLiveUtilitiesConfig.HANDLER.instance().factoryLevelScale;
        if (scale <= 0f) {
            scale = 1f;
        }

        net.felix.utilities.DragOverlay.dock.OverlayDockRect factoryDock = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.placeLive(
                net.felix.utilities.DragOverlay.dock.OverlayDockIds.FACTORY,
                x(), y(), Math.round(width * scale), Math.round(height * scale));
        if (net.felix.utilities.DragOverlay.dock.OverlayDockLayout.isMeasuring() || context == null) {
            return;
        }
        scale = net.felix.utilities.DragOverlay.dock.OverlayDockLayout.effectiveScale(
                net.felix.utilities.DragOverlay.dock.OverlayDockIds.FACTORY, scale);
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(factoryDock.x(), factoryDock.y());
        matrices.scale(scale, scale);

        if (CCLiveUtilitiesConfig.HANDLER.instance().factoryLevelShowBackground) {
            context.fill(0, 0, width, height, 0x80000000);
        }

        int textY = PADDING;
        context.text(client.font, Component.literal(FactoryLevelDisplay.TITLE), PADDING, textY,
                FactoryLevelDisplay.TITLE_COLOR, true);
        textY += LINE_HEIGHT;
        for (String line : lines) {
            if (!line.isEmpty()) {
                FactoryLevelDisplay.LinePaint paint = FactoryLevelDisplay.paint(line);
                int textX = PADDING;
                if (!paint.label().isEmpty()) {
                    context.text(client.font, Component.literal(paint.label()), textX, textY,
                            paint.labelColor(), true);
                    textX += client.font.width(paint.label());
                }
                if (!paint.value().isEmpty()) {
                    context.text(client.font, Component.literal(paint.value()), textX, textY,
                            paint.valueColor(), true);
                }
            }
            textY += LINE_HEIGHT;
        }
        matrices.popMatrix();
    }

    private static void onLevelChange(Minecraft client, ClientLevel level) {
        note(client, level);
    }

    private static void note(Minecraft client, ClientLevel level) {
        if (client == null || client.player == null || level == null) {
            return;
        }
        String dimensionId = level.dimension().identifier().toString();
        if (!FactoryLevelPresence.observe(level, dimensionId, client.player.getGameProfile().name())) {
            return;
        }
        if (activeDimension != null && !activeDimension.equals(dimensionId)) {
            estimate.reset();
        }
        activeDimension = dimensionId;
        if (!FactoryLevelPresence.inFactory()) {
            hideMeasurement();
        }
    }

    private static void onClientTick(Minecraft client) {
        if (client.player != null && client.level != null) {
            note(client, client.level);
        }
        long now = System.currentTimeMillis();
        if (lastUpdateAt != 0L && now - lastUpdateAt < UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdateAt = now;
        if (client.player == null || client.level == null || !FactoryLevelPresence.inFactory()) {
            return;
        }
        float bar = client.player.experienceProgress;
        if (bar < 0f) {
            bar = 0f;
        } else if (bar > 1f) {
            bar = 1f;
        }
        int level = client.player.experienceLevel;
        estimate.observe(now, level + bar);
        cachedLines = linesFor(level, bar, now);
        cachedWidth = measureWidth(client, cachedLines);
        cachedHeight = PADDING * 2 + (1 + cachedLines.size()) * LINE_HEIGHT;
        shown = true;
    }

    private static List<String> linesFor(int level, float bar, long now) {
        FactoryLevelEstimate.Status status = estimate.status(now);
        long remaining = 0L;
        Integer target = FactoryLevelDisplay.targetLevel(level);
        if (target != null && status == FactoryLevelEstimate.Status.READY) {
            var millis = estimate.remainingMillis(now, level + bar, target);
            if (millis.isPresent()) {
                remaining = millis.getAsLong();
            } else {
                status = FactoryLevelEstimate.Status.MEASURING;
            }
        }
        return FactoryLevelDisplay.body(level, status, remaining);
    }

    private static int measureWidth(Minecraft client, List<String> lines) {
        if (client == null || client.font == null) {
            return MIN_OVERLAY_WIDTH;
        }
        int max = client.font.width(FactoryLevelDisplay.TITLE);
        for (String line : lines) {
            if (!line.isEmpty()) {
                max = Math.max(max, client.font.width(line));
            }
        }
        return Math.max(MIN_OVERLAY_WIDTH, max + PADDING * 2);
    }

    private static void hideMeasurement() {
        estimate.reset();
        shown = false;
        cachedLines = EMPTY_LINES;
        cachedWidth = MIN_OVERLAY_WIDTH;
        cachedHeight = PADDING * 2 + LINE_HEIGHT;
    }

    private static void clearMeasurement() {
        hideMeasurement();
        activeDimension = null;
    }

    public static void contributeLayout(Minecraft client) {
        if (client == null) {
            return;
        }
        onHudRender(null, client.getDeltaTracker());
    }

    public static void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        if (!HudOverlayVisibility.shouldRenderWorldHudOverlays()) {
            return;
        }
        if (KeyBindingUtility.isPlayerListKeyPressed()) {
            return;
        }
        if (client.gui.hud.isHidden()) {
            return;
        }
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        if (!CCLiveUtilitiesConfig.isModRuntimeEnabled()
                || !config.factoryLevelEnabled
                || !config.showFactoryLevel) {
            return;
        }
        if (!shown || !FactoryLevelPresence.inFactory()) {
            return;
        }
        render(context);
    }
}

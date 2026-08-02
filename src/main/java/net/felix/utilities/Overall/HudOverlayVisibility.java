package net.felix.utilities.Overall;

import net.felix.utilities.Aincraft.BPViewerUtility;
import net.felix.utilities.Aincraft.CardsStatuesUtility;
import net.felix.utilities.Aincraft.KillsUtility;
import net.felix.utilities.Aincraft.MaterialTrackerUtility;
import net.felix.utilities.DragOverlay.OverlayEditorScreen;
import net.felix.utilities.Factory.BossHPUtility;
import net.felix.utilities.Overall.NpcAlerts.NpcAlertsUtility;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * In 26.1 können {@code HudElementRegistry.addLast}-Overlays über dem Inventar-Dim landen.
 * Deshalb: bei Inventar nicht im HUD-Layer zeichnen, sondern direkt vor {@code extractTransparentBackground},
 * damit der dunkle Hintergrund darüber liegt und die Overlays abgedunkelt sichtbar bleiben.
 */
public final class HudOverlayVisibility {

    private static final ThreadLocal<Boolean> RENDERING_BEHIND_DIM = ThreadLocal.withInitial(() -> false);

    private HudOverlayVisibility() {
    }

    /**
     * {@code true}, wenn Welt-HUD-Overlays im normalen HUD-Layer gezeichnet werden dürfen.
     * Bei Inventaren/Containern auslassen (dort über {@link #renderBehindInventoryDim}).
     */
    public static boolean shouldRenderWorldHudOverlays() {
        if (Boolean.TRUE.equals(RENDERING_BEHIND_DIM.get())) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        if (client.screen == null || client.screen instanceof OverlayEditorScreen) {
            return true;
        }
        return !(client.screen instanceof AbstractContainerScreen);
    }

    /**
     * Zeichnet Welt-HUD-Overlays in den aktuellen Screen-Stratum, bevor der dunkle Hintergrund kommt.
     */
    public static void renderBehindInventoryDim(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || context == null) {
            return;
        }
        if (!(client.screen instanceof AbstractContainerScreen) || client.screen instanceof OverlayEditorScreen) {
            return;
        }

        DeltaTracker tickCounter = client.getDeltaTracker();
        RENDERING_BEHIND_DIM.set(true);
        try {
            CoinTrackerUtility.onHudRender(context, tickCounter);
            KillsUtility.onHudRender(context, tickCounter);
            MaterialTrackerUtility.onHudRender(context, tickCounter);
            CardsStatuesUtility.onHudRender(context, tickCounter);
            InformationenUtility.onHudRender(context, tickCounter);
            NpcAlertsUtility.onHudRender(context, tickCounter);
            BossHPUtility.onHudRender(context, tickCounter);
            BPViewerUtility bp = BPViewerUtility.getInstance();
            if (bp != null) {
                bp.onHudRender(context, tickCounter);
            }
        } finally {
            RENDERING_BEHIND_DIM.set(false);
        }
    }
}

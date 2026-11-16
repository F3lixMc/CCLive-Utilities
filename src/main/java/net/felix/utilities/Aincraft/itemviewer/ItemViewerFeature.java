package net.felix.utilities.Aincraft.itemviewer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.felix.utilities.Aincraft.itemviewer.inventory.FavoriteBlueprintsStore;
import net.felix.utilities.Aincraft.itemviewer.inventory.InventoryFilter;
import net.felix.utilities.Aincraft.itemviewer.search.ItemFilterPipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import static net.felix.utilities.Aincraft.itemviewer.data.ItemCatalog.loadItemsAsync;
import static net.felix.utilities.Aincraft.itemviewer.data.ItemsConfigUpdater.checkAndUpdateItemsConfig;
import static net.felix.utilities.Aincraft.itemviewer.data.ItemsConfigUpdater.serverCheckRegistered;
import static net.felix.utilities.Aincraft.itemviewer.ui.ViewerRenderer.renderMinimizedButton;
import static net.felix.utilities.Aincraft.itemviewer.ui.ViewerState.isMinimized;
import static net.felix.utilities.Aincraft.itemviewer.ui.ViewerState.isVisible;
import static net.felix.utilities.Aincraft.itemviewer.ui.ViewerState.shouldShowItemViewerButtons;

/**
 * Einstiegspunkt des Item Viewers: initialisiert die Teilbereiche und registriert Tick- und HUD-Callbacks.
 */
public final class ItemViewerFeature {

    public static boolean isInitialized = false;

    private ItemViewerFeature() {
    }

    public static void initialize() {
        if (isInitialized) {
            return;
        }

        try {
            FavoriteBlueprintsStore.initialize();

            // Server-Join-Event für den Versions-Check (nur einmal registrieren)
            if (!serverCheckRegistered) {
                ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> checkAndUpdateItemsConfig());
                serverCheckRegistered = true;
            }

            // Spieler kann bereits auf einem Server sein, wenn die Mod während des Spiels geladen wird
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.player != null && client.getConnection() != null) {
                checkAndUpdateItemsConfig();
            }

            // Items-Datei asynchron laden, um Lag beim Start zu vermeiden
            loadItemsAsync();

            ItemViewerHotkeys.registerKeybind();

            InventoryFilter.initialize();

            ClientTickEvents.END_CLIENT_TICK.register(ItemViewerFeature::onClientTick);

            HudElementRegistry.addLast(
                    Identifier.fromNamespaceAndPath("cclive-utilities", "item_viewer"),
                    ItemViewerFeature::onHudRender);

            isInitialized = true;
        } catch (Exception e) {
            // Silent error handling
        }
    }

    private static void onClientTick(Minecraft client) {
        ItemFilterPipeline.tickDebounce();
        ItemViewerHotkeys.onClientTick(client);
    }

    /**
     * Rendert den minimierten Button (rechts unten), solange kein Screen offen ist.
     * Bei offenem Screen rendern ihn die Mixins, damit er über dem dunklen Hintergrund liegt.
     * Das Hilfe-Overlay und der Viewer selbst werden ebenfalls über Mixins gerendert.
     */
    private static void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (isMinimized && isVisible && shouldShowItemViewerButtons(client)) {
            renderMinimizedButton(context, client);
        }
    }
}
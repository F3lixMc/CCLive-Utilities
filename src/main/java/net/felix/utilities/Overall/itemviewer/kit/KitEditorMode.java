package net.felix.utilities.Overall.itemviewer.kit;

import net.felix.utilities.Overall.itemviewer.data.ItemData;
import net.felix.utilities.Overall.itemviewer.inventory.InventoryFilter;
import net.felix.utilities.Overall.itemviewer.ui.ViewerPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static net.felix.utilities.Overall.itemviewer.data.ItemCatalog.itemsLoaded;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.SORT_DROPDOWN_HEIGHT;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.calculateViewerPositionBesideCenteredInventory;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerLayout.getOrCalculateViewerPosition;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerRenderer.renderItemViewer;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.isMinimized;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.lastMouseX;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.lastMouseY;
import static net.felix.utilities.Overall.itemviewer.ui.ViewerState.updateMousePosition;

/**
 * Kit-Editor-Modus des Item Viewers: Auswahlziel, Buttons und eingebettete Anzeige.
 */
public final class KitEditorMode {

    private KitEditorMode() {
    }

    // Kit-Button Variablen
    public static final int KIT_BUTTON_SIZE = 20; // Gleiche Höhe wie andere Buttons

    private static final Identifier KIT_ICON_TEXTURE = Identifier.fromNamespaceAndPath("cclive-utilities", "textures/icons/kits_icon_2.png");

    public static boolean kitFilterActive = false; // Ob Kit-Filter aktiv ist (Linksklick)

    public static final int KIT_BUTTON_INDEX = 0; // Verwende Kit Button 1 aus KitFilterUtility

    // Kit-Editor-Modus (eigenes Kit zusammenstellen)
    public static AbstractContainerScreen<?> kitEditorBackgroundScreen = null;

    public static java.util.function.BiConsumer<ItemData, net.felix.utilities.Town.kits.editor.CustomKitEditorScreen.PickTarget> kitEditorPickHandler = null;

    public static net.felix.utilities.Town.kits.editor.CustomKitEditorScreen.PickTarget kitEditorPickTarget =
            net.felix.utilities.Town.kits.editor.CustomKitEditorScreen.PickTarget.ADD_ITEM;

    // Anwenden-Button (Item-Viewer-Filterung ins Schmiede-Inventar übernehmen)
    private static final String APPLY_BUTTON_LABEL = "Anwenden";

    /**
     * Rendert den Kit-Button
     */
    public static void renderKitButton(GuiGraphicsExtractor context, int x, int y) {
        Minecraft client = Minecraft.getInstance();
        
        // Prüfe ob Maus über Button ist
        boolean isHovered = lastMouseX >= x && lastMouseX < x + KIT_BUTTON_SIZE &&
                           lastMouseY >= y && lastMouseY < y + KIT_BUTTON_SIZE;
        
        // Hole aktuelle Kit-Auswahl
        net.felix.utilities.Town.kits.filter.KitFilterUtility.KitSelection kitSelection = 
            net.felix.utilities.Town.kits.filter.KitFilterUtility.getKitSelection(KIT_BUTTON_INDEX);
        boolean hasKitSelected = kitSelection != null;
        
        // Bestimme Kit-Farbe (basierend auf Kit-Typ)
        int kitColor = getKitColor(kitSelection);
        
        // Button-Hintergrund
        int bgColor;
        if (kitFilterActive && hasKitSelected) {
            // Kit aktiv: 50% transparenter Marker
            bgColor = isHovered ? (kitColor & 0x80FFFFFF) | 0x80606060 : (kitColor & 0x80FFFFFF) | 0x80404040;
        } else {
            // Kit nicht aktiv oder nicht ausgewählt: normal
            bgColor = isHovered ? 0xFF404040 : 0xFF202020;
        }
        context.fill(x, y, x + KIT_BUTTON_SIZE, y + KIT_BUTTON_SIZE, bgColor);
        
        // Button-Rahmen
        int borderColor;
        if (hasKitSelected && !kitFilterActive) {
            // Kit ausgewählt aber nicht aktiv: farbiger Rahmen
            borderColor = kitColor;
        } else if (kitFilterActive && hasKitSelected) {
            // Kit aktiv: 50% transparenter Rahmen
            borderColor = (kitColor & 0x80FFFFFF) | 0x80000000;
        } else {
            // Kein Kit ausgewählt: normal
            borderColor = isHovered ? 0xFFFFFFFF : 0xFF808080;
        }
        context.fill(x, y, x + KIT_BUTTON_SIZE, y + 1, borderColor); // Oben
        context.fill(x, y + KIT_BUTTON_SIZE - 1, x + KIT_BUTTON_SIZE, y + KIT_BUTTON_SIZE, borderColor); // Unten
        context.fill(x, y, x + 1, y + KIT_BUTTON_SIZE, borderColor); // Links
        context.fill(x + KIT_BUTTON_SIZE - 1, y, x + KIT_BUTTON_SIZE, y + KIT_BUTTON_SIZE, borderColor); // Rechts
        
        // Rendere Kit-Icon (Textur)
        try {
            // Versuche Textur zu rendern (16x16 Pixel, zentriert im 20x20 Button)
            int iconSize = 16;
            int iconX = x + (KIT_BUTTON_SIZE - iconSize) / 2;
            int iconY = y + (KIT_BUTTON_SIZE - iconSize) / 2;
            
            // Verwende drawTexture mit RenderPipeline
            context.blit(
                RenderPipelines.GUI_TEXTURED,
                KIT_ICON_TEXTURE,
                iconX, iconY,
                0.0f, 0.0f,
                iconSize, iconSize,
                iconSize, iconSize
            );
        } catch (Exception e) {
            // Fallback: Zeige "K" als Text wenn Textur nicht geladen werden kann
            String kitSymbol = "K";
            int textWidth = client.font.width(kitSymbol);
            int textHeight = client.font.lineHeight;
            int textX = x + (KIT_BUTTON_SIZE - textWidth) / 2;
            int textY = y + (KIT_BUTTON_SIZE - textHeight) / 2;
            context.text(client.font, Component.literal(kitSymbol), textX, textY, 0xFFFFFFFF, false);
        }
    }

    public static int getApplyButtonWidth(Minecraft client) {
        return client.font.width(APPLY_BUTTON_LABEL) + 10;
    }

    public static void renderApplyButton(GuiGraphicsExtractor context, int x, int y, int width) {
        Minecraft client = Minecraft.getInstance();
        boolean isHovered = lastMouseX >= x && lastMouseX < x + width &&
                           lastMouseY >= y && lastMouseY < y + SORT_DROPDOWN_HEIGHT;
        boolean isActive = InventoryFilter.isFilterActive();
        
        int bgColor;
        if (isActive) {
            bgColor = isHovered ? 0x8060A060 : 0x80408040;
        } else {
            bgColor = isHovered ? 0xFF404040 : 0xFF202020;
        }
        context.fill(x, y, x + width, y + SORT_DROPDOWN_HEIGHT, bgColor);
        
        int borderColor = isActive
            ? (isHovered ? 0xFF80FF80 : 0xFF40C040)
            : (isHovered ? 0xFFFFFFFF : 0xFF808080);
        context.fill(x, y, x + width, y + 1, borderColor);
        context.fill(x, y + SORT_DROPDOWN_HEIGHT - 1, x + width, y + SORT_DROPDOWN_HEIGHT, borderColor);
        context.fill(x, y, x + 1, y + SORT_DROPDOWN_HEIGHT, borderColor);
        context.fill(x + width - 1, y, x + width, y + SORT_DROPDOWN_HEIGHT, borderColor);
        
        int textWidth = client.font.width(APPLY_BUTTON_LABEL);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (SORT_DROPDOWN_HEIGHT - client.font.lineHeight) / 2;
        int textColor = isActive ? 0xFFAAFFAA : 0xFFFFFFFF;
        context.text(client.font, Component.literal(APPLY_BUTTON_LABEL), textX, textY, textColor, false);
    }

    /**
     * Gibt die Farbe für ein Kit zurück (basierend auf Kit-Typ)
     */
    public static void startKitEditorMode(AbstractContainerScreen<?> backgroundScreen,
            java.util.function.BiConsumer<ItemData, net.felix.utilities.Town.kits.editor.CustomKitEditorScreen.PickTarget> pickHandler) {
        kitEditorBackgroundScreen = backgroundScreen;
        kitEditorPickHandler = pickHandler;
        kitEditorPickTarget = net.felix.utilities.Town.kits.editor.CustomKitEditorScreen.PickTarget.ADD_ITEM;
        isMinimized = false;
    }

    public static void stopKitEditorMode() {
        kitEditorBackgroundScreen = null;
        kitEditorPickHandler = null;
        kitEditorPickTarget = net.felix.utilities.Town.kits.editor.CustomKitEditorScreen.PickTarget.ADD_ITEM;
    }

    public static boolean isKitEditorMode() {
        return kitEditorPickHandler != null;
    }

    public static AbstractContainerScreen<?> getKitEditorBackgroundScreen() {
        return kitEditorBackgroundScreen;
    }

    public static void setKitEditorPickTarget(net.felix.utilities.Town.kits.editor.CustomKitEditorScreen.PickTarget target) {
        kitEditorPickTarget = target != null
                ? target
                : net.felix.utilities.Town.kits.editor.CustomKitEditorScreen.PickTarget.ADD_ITEM;
    }

    public static int[] getKitEditorViewerBounds() {
        ViewerPosition pos = resolveKitEditorViewerPosition();
        if (pos == null) {
            return null;
        }
        return new int[] { pos.viewerX, pos.viewerY, pos.viewerWidth, pos.viewerHeight };
    }

    public static ViewerPosition resolveKitEditorViewerPosition() {
        if (!isKitEditorMode()) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui.screen() == null) {
            return null;
        }
        if (kitEditorBackgroundScreen != null) {
            return getOrCalculateViewerPosition(kitEditorBackgroundScreen, client.gui.screen());
        }
        return calculateViewerPositionBesideCenteredInventory(client.gui.screen());
    }

    public static boolean isMouseOverKitEditorItemViewer(double mouseX, double mouseY) {
        int[] bounds = getKitEditorViewerBounds();
        if (bounds == null) {
            return false;
        }
        return mouseX >= bounds[0] && mouseX < bounds[0] + bounds[2]
                && mouseY >= bounds[1] && mouseY < bounds[1] + bounds[3];
    }

    public static void renderKitEditorItemViewer(GuiGraphicsExtractor context, Minecraft client, int mouseX, int mouseY) {
        if (!isKitEditorMode()) {
            return;
        }
        if (!itemsLoaded) {
            return;
        }
        updateMousePosition(mouseX, mouseY);
        ViewerPosition pos = resolveKitEditorViewerPosition();
        if (pos != null) {
            renderItemViewer(context, client, pos);
        }
    }

    private static int getKitColor(net.felix.utilities.Town.kits.filter.KitFilterUtility.KitSelection kitSelection) {
        if (kitSelection == null) {
            return 0xFF808080; // Grau als Standard
        }
        if (kitSelection.isCustom()) {
            return 0xFFAA55FF;
        }

        switch (kitSelection.kitType) {
            case MÜNZ_KIT:
                return 0xFFFC54FC; // Helles Pink (wie modifier Andere)
            case SCHADEN_KIT:
                return 0xFFFC5454; // Rot (wie modifier Schaden)
            case RESSOURCEN_KIT:
                return 0xFF5454FC; // Blau (wie modifier Fähigkeiten)
            case HERSTELLUNGS_KIT:
                return 0xFFFCFC54; // Gelb (wie modifier Herstellung)
            case TANK_KIT:
                return 0xFFFCA800; // Orange (wie modifier Verteidigung)
            default:
                return 0xFF808080; // Grau als Fallback
        }
    }
}

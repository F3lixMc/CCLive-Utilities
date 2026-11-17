package net.felix.utilities.Overall.itemviewer.filtermenu;

import java.util.List;
import net.felix.utilities.Overall.itemviewer.ui.ViewerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterCatalog.FILTER_ROW_TITLES;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenu.open;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuDimensions.CLOSE_BUTTON_SIZE;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuDimensions.COLUMN_GAP;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuDimensions.PICKER_BUTTON_SIZE;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuDimensions.ROW_HEIGHT;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuDimensions.TITLE_Y;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuInput.findOperatorFieldAt;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuLayout.computePanelLayout;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.groups;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.hovered;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.scrollOffset;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterSelection.displayGroupTitle;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterSelection.getRowGroups;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterWidgets.drawCenteredButtonLabel;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterWidgets.drawCheckbox;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterWidgets.drawPanel;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterWidgets.drawSmallButton;
import static net.felix.utilities.Overall.itemviewer.filtermenu.ItemPicker.pickerListDisplayName;
import static net.felix.utilities.Overall.itemviewer.filtermenu.ItemPicker.renderOpenItemPicker;
import static net.felix.utilities.Overall.itemviewer.filtermenu.MainScrollbar.clampScrollOffset;
import static net.felix.utilities.Overall.itemviewer.filtermenu.MainScrollbar.finalizeScrollLayout;
import static net.felix.utilities.Overall.itemviewer.filtermenu.MainScrollbar.renderScrollbar;

/**
 * Zeichnet das Filter-Menue.
 */
public final class FilterMenuRenderer {

    private FilterMenuRenderer() {
    }

    public static void render(GuiGraphicsExtractor context) {
        if (!open) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        PanelLayout layout = computePanelLayout(client);
        Font tr = client.font;

        context.fill(0, 0, layout.screenWidth, layout.screenHeight, 0x80000000);
        drawPanel(context, layout.boxX, layout.boxY, layout.boxWidth, layout.boxHeight);

        boolean closeHovered = ViewerState.getLastMouseX() >= layout.closeX && ViewerState.getLastMouseX() < layout.closeX + CLOSE_BUTTON_SIZE
                && ViewerState.getLastMouseY() >= layout.closeY && ViewerState.getLastMouseY() < layout.closeY + CLOSE_BUTTON_SIZE;
        drawCloseButton(context, layout.closeX, layout.closeY, closeHovered);

        String title = "Filter";
        int titleWidth = tr.width(title);
        int titleY = layout.boxY + TITLE_Y;
        context.text(tr, title, layout.boxX + (layout.boxWidth - titleWidth) / 2, titleY, 0xFFFFFF00, true);

        hovered = ViewerState.getLastMouseX() >= layout.boxX && ViewerState.getLastMouseX() < layout.boxX + layout.boxWidth
                && ViewerState.getLastMouseY() >= layout.boxY && ViewerState.getLastMouseY() < layout.boxY + layout.boxHeight;

        finalizeScrollLayout(layout, tr);
        scrollOffset = clampScrollOffset(scrollOffset);

        context.enableScissor(layout.contentX, layout.contentTop, layout.contentScrollRight, layout.contentBottom);
        for (int row = 0; row < FILTER_ROW_TITLES.size(); row++) {
            List<FilterGroup> rowGroups = getRowGroups(row);
            for (int i = 0; i < rowGroups.size(); i++) {
                FilterGroup group = rowGroups.get(i);
                int headerY = group.headerY - scrollOffset;
                if (headerY + tr.lineHeight >= layout.contentTop && headerY <= layout.contentBottom) {
                    context.text(tr, displayGroupTitle(group.title), group.columnX, headerY, 0xFFFFFF55, false);
                }
                if (i < rowGroups.size() - 1) {
                    FilterGroup next = rowGroups.get(i + 1);
                    int dividerX = group.columnX + group.columnWidth + COLUMN_GAP / 2;
                    int dividerTop = Math.min(group.headerY, next.headerY) - scrollOffset;
                    int dividerBottom = Math.max(group.bottomY, next.bottomY) - scrollOffset;
                    if (dividerBottom > layout.contentTop && dividerTop < layout.contentBottom) {
                        context.fill(dividerX, Math.max(dividerTop, layout.contentTop),
                                dividerX + 1, Math.min(dividerBottom, layout.contentBottom), 0xFF505050);
                    }
                }
            }
        }
        for (FilterGroup group : groups) {
            for (FilterOption option : group.options) {
                int drawY = option.y - scrollOffset;
                if (drawY + ROW_HEIGHT < layout.contentTop || drawY > layout.contentBottom) {
                    continue;
                }
                drawCheckbox(context, tr, group, option, drawY);
            }
        }
        context.disableScissor();

        if (layout.scrollable) {
            renderScrollbar(context, layout);
        }

        boolean clearHovered = ViewerState.getLastMouseX() >= layout.clearX && ViewerState.getLastMouseX() < layout.clearX + layout.clearW
                && ViewerState.getLastMouseY() >= layout.clearY && ViewerState.getLastMouseY() < layout.clearY + 16;
        drawSmallButton(context, layout.clearX, layout.clearY, layout.clearW, 16, clearHovered);
        drawCenteredButtonLabel(context, tr, "Alle löschen", layout.clearX, layout.clearY, layout.clearW, 16);
        renderOpenItemPicker(context, tr, layout);
        renderHoverTooltips(context, tr);
    }

    private static void renderHoverTooltips(GuiGraphicsExtractor context, Font tr) {
        double mouseX = ViewerState.getLastMouseX();
        double mouseY = ViewerState.getLastMouseY();

        for (FilterGroup group : groups) {
            if (!"Item".equals(group.title)) {
                continue;
            }
            for (FilterOption option : group.options) {
                if (option.pickerButtonX < 0) {
                    continue;
                }
                int rowY = option.y - scrollOffset;
                int pickerY = rowY + (ROW_HEIGHT - PICKER_BUTTON_SIZE) / 2;
                if (mouseX >= option.pickerButtonX && mouseX < option.pickerButtonX + PICKER_BUTTON_SIZE
                        && mouseY >= pickerY && mouseY < pickerY + PICKER_BUTTON_SIZE) {
                    String listName = pickerListDisplayName(option.label);
                    context.setComponentTooltipForNextFrame(tr,
                            List.of(Component.literal("Klicken um " + listName + "-Liste zu öffnen")),
                            (int) mouseX, (int) mouseY);
                    return;
                }
            }
        }

        if (findOperatorFieldAt(mouseX, mouseY) != null) {
            context.setComponentTooltipForNextFrame(tr,
                    List.of(Component.literal("Klicken zum wechseln (=, >=, <=)")),
                    (int) mouseX, (int) mouseY);
        }
    }

    private static void drawCloseButton(GuiGraphicsExtractor context, int x, int y, boolean hovered) {
        drawSmallButton(context, x, y, CLOSE_BUTTON_SIZE, hovered);
        drawCloseX(context, x, y, CLOSE_BUTTON_SIZE);
    }

    private static void drawCloseX(GuiGraphicsExtractor context, int boxX, int boxY, int size) {
        int color = 0xFFFFFFFF;
        int margin = 4;
        int span = size - margin * 2;
        for (int i = 0; i < span; i++) {
            int px = boxX + margin + i;
            context.fill(px, boxY + margin + i, px + 1, boxY + margin + i + 1, color);
            context.fill(px, boxY + size - margin - 1 - i, px + 1, boxY + size - margin - i, color);
        }
    }
}

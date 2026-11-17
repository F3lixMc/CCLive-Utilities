package net.felix.utilities.Overall.itemviewer.filtermenu;

import net.felix.utilities.Overall.itemviewer.ui.ViewerState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuDimensions.SCROLLBAR_END_CAP;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuDimensions.SCROLLBAR_GAP;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuDimensions.SCROLLBAR_WIDTH;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuLayout.layoutGroups;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.contentHeight;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.scrollOffset;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.scrollbarDragOffset;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.scrollbarDragging;
import static net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenuState.viewportHeight;

/**
 * Scrollbar des Filter-Menues.
 */
public final class MainScrollbar {

    private MainScrollbar() {
    }

    static int clampScrollOffset(int offset) {
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        return Math.max(0, Math.min(offset, maxScroll));
    }

    static void finalizeScrollLayout(PanelLayout layout, Font tr) {
        layoutGroups(tr, layout.contentX, layout.contentTop, layout.contentWidth);
        viewportHeight = layout.contentBottom - layout.contentTop;
        layout.scrollable = contentHeight > viewportHeight;
        if (layout.scrollable) {
            int scrollContentWidth = layout.contentWidth - SCROLLBAR_WIDTH - SCROLLBAR_GAP;
            layoutGroups(tr, layout.contentX, layout.contentTop, scrollContentWidth);
            layout.scrollable = contentHeight > viewportHeight;
            layout.scrollBarX = layout.contentX + layout.contentWidth - SCROLLBAR_WIDTH;
            layout.contentScrollRight = layout.scrollBarX - SCROLLBAR_GAP;
        } else {
            layout.scrollBarX = 0;
            layout.contentScrollRight = layout.contentX + layout.contentWidth;
        }
    }

    private static int computeThumbHeight(int trackHeight) {
        if (contentHeight <= 0) {
            return trackHeight;
        }
        return Math.max(16, (int) ((long) trackHeight * viewportHeight / contentHeight));
    }

    private static int computeThumbY(PanelLayout layout) {
        int trackHeight = layout.contentBottom - layout.contentTop;
        int thumbHeight = computeThumbHeight(trackHeight);
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (maxScroll <= 0) {
            return layout.contentTop;
        }
        return layout.contentTop + (int) ((long) (trackHeight - thumbHeight) * scrollOffset / maxScroll);
    }

    static void renderScrollbar(GuiGraphicsExtractor context, PanelLayout layout) {
        int trackTop = layout.contentTop;
        int trackBottom = layout.contentBottom;
        int barX = layout.scrollBarX;
        int trackHeight = trackBottom - trackTop;

        context.fill(barX, trackTop, barX + SCROLLBAR_WIDTH, trackBottom, 0xFF2A2A2A);
        context.fill(barX, trackTop, barX + SCROLLBAR_WIDTH, trackTop + SCROLLBAR_END_CAP, 0xFF606060);
        context.fill(barX, trackBottom - SCROLLBAR_END_CAP, barX + SCROLLBAR_WIDTH, trackBottom, 0xFF606060);
        context.fill(barX, trackTop, barX + 1, trackBottom, 0xFF606060);
        context.fill(barX + SCROLLBAR_WIDTH - 1, trackTop, barX + SCROLLBAR_WIDTH, trackBottom, 0xFF606060);
        context.outline(barX, trackTop, SCROLLBAR_WIDTH, trackHeight, 0xFF808080);

        int thumbY = computeThumbY(layout);
        int thumbHeight = computeThumbHeight(trackHeight);
        boolean thumbHovered = !scrollbarDragging && ViewerState.getLastMouseX() >= barX
                && ViewerState.getLastMouseX() < barX + SCROLLBAR_WIDTH
                && ViewerState.getLastMouseY() >= thumbY
                && ViewerState.getLastMouseY() < thumbY + thumbHeight;
        int thumbColor = scrollbarDragging ? 0xFFB0B0B0 : (thumbHovered ? 0xFFA0A0A0 : 0xFF888888);
        context.fill(barX + 1, thumbY, barX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, thumbColor);
        context.fill(barX + 1, thumbY, barX + SCROLLBAR_WIDTH - 1, thumbY + SCROLLBAR_END_CAP, 0xFFC8C8C8);
        context.fill(barX + 1, thumbY + thumbHeight - SCROLLBAR_END_CAP, barX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, 0xFF707070);
        context.outline(barX + 1, thumbY, SCROLLBAR_WIDTH - 2, thumbHeight, 0xFFC0C0C0);
    }

    static void setScrollFromThumbTop(int thumbTop, PanelLayout layout) {
        int trackHeight = layout.contentBottom - layout.contentTop;
        int thumbHeight = computeThumbHeight(trackHeight);
        int thumbTravel = trackHeight - thumbHeight;
        int target = thumbTop - layout.contentTop;
        target = Math.max(0, Math.min(target, thumbTravel));
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (thumbTravel <= 0 || maxScroll <= 0) {
            scrollOffset = 0;
            return;
        }
        scrollOffset = (int) ((long) target * maxScroll / thumbTravel);
        scrollOffset = clampScrollOffset(scrollOffset);
    }

    static boolean handleScrollbarClick(double mouseX, double mouseY, PanelLayout layout) {
        if (!layout.scrollable) {
            return false;
        }
        int barX = layout.scrollBarX;
        if (mouseX < barX || mouseX >= barX + SCROLLBAR_WIDTH
                || mouseY < layout.contentTop || mouseY >= layout.contentBottom) {
            return false;
        }
        int trackHeight = layout.contentBottom - layout.contentTop;
        int thumbHeight = computeThumbHeight(trackHeight);
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (maxScroll <= 0) {
            return true;
        }

        int thumbY = computeThumbY(layout);
        if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
            scrollbarDragging = true;
            scrollbarDragOffset = (int) mouseY - thumbY;
            return true;
        }

        setScrollFromThumbTop((int) mouseY - thumbHeight / 2, layout);
        thumbY = computeThumbY(layout);
        scrollbarDragging = true;
        scrollbarDragOffset = (int) mouseY - thumbY;
        return true;
    }
}

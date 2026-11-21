package net.felix.utilities.Factory.mklevel.ui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.felix.utilities.Factory.mklevel.data.MkLevelDatabase;
import net.felix.utilities.Factory.mklevel.data.MkLevelState;
import net.felix.utilities.Factory.mklevel.overlay.MkLevelOverlayRenderer;
import net.felix.utilities.Factory.mklevel.overlay.MkLevelOverlayTab;
import net.felix.utilities.Factory.mklevel.overlay.MkLevelRenderLine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Dieselbe Machtkristall-Liste wie im Menü: Einzelne, Kombiniert und Gesamt.
 */
public final class MkOverviewScreen extends Screen {

    private static final MkLevelOverlayTab[] TABS = {
            MkLevelOverlayTab.INDIVIDUAL,
            MkLevelOverlayTab.COMBINED,
            MkLevelOverlayTab.TOTAL_ESSENCES
    };
    private static final String[] TAB_LABELS = {"Einzelne", "Kombiniert", "Gesamt"};

    private static final int[] SAVED_SCROLL = new int[TABS.length];
    private static int savedTab;
    private static final int SCROLLBAR_WIDTH = 6;

    private EditBox search;
    private MkLevelOverlayTab tab = TABS[Math.floorMod(savedTab, TABS.length)];
    private int scroll = SAVED_SCROLL[Math.floorMod(savedTab, TABS.length)];
    private String appliedSearch = "";
    private boolean draggingScrollbar;
    private int dragGrabOffset;

    public MkOverviewScreen() {
        super(Component.literal("Machtkristalle"));
    }

    @Override
    protected void init() {
        MkLevelDatabase.loadMkLevelDatabase();
        int panelX = panelX();
        search = new EditBox(this.font, panelX + 8, panelY() + 28, panelWidth() - 16, 18, Component.literal("Suche"));
        search.setHint(Component.empty());
        search.setTextColor(0xFFFFFFFF);
        addRenderableWidget(search);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int x = panelX();
        int y = panelY();
        int width = panelWidth();
        graphics.fill(x, y, x + width, y + height, 0xC0101010);
        graphics.outline(x, y, width, height, 0xFF555555);
        rememberSearch();
        if (search != null) {
            graphics.outline(search.getX() - 1, search.getY() - 1, search.getWidth() + 2, search.getHeight() + 2, 0xFFDDDDDD);
            if (!search.isFocused() && search.getValue().isEmpty()) {
                graphics.text(this.font, "Suche", search.getX() + 4, search.getY() + (search.getHeight() - 8) / 2, 0xFFFFFFFF, false);
            }
        }

        int tabWidth = width / TABS.length;
        for (int index = 0; index < TABS.length; index++) {
            int tabX = x + index * tabWidth;
            boolean active = TABS[index] == tab;
            graphics.fill(tabX + 2, y + 4, tabX + tabWidth - 2, y + 22, active ? 0xFF3A5F3A : 0xFF2A2A2A);
            String label = TAB_LABELS[index];
            graphics.text(this.font, label, tabX + (tabWidth - this.font.width(label)) / 2, y + 8,
                    active ? 0xFF55FFFF : 0xFFAAAAAA, false);
        }

        List<MkLevelRenderLine> lines = lines();
        ContentView view = contentView(lines.size());
        int lineHeight = this.font.lineHeight + 2;
        int textX = x + 10;
        int textY = view.top;
        for (int index = scroll; index < lines.size() && textY + this.font.lineHeight <= view.bottom; index++) {
            MkLevelRenderLine line = lines.get(index);
            graphics.text(this.font, line.text, textX + line.xOffset, textY, line.color, false);
            if (line.hasSecondPart && line.secondPartText != null) {
                graphics.text(this.font, line.secondPartText, textX + line.secondPartXOffset, textY, line.secondPartColor, false);
            }
            if (line.hasThirdPart && line.thirdPartText != null) {
                graphics.text(this.font, line.thirdPartText, textX + line.thirdPartXOffset, textY, line.thirdPartColor, false);
            }
            textY += lineHeight;
        }
        drawScrollbar(graphics, view);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int x = panelX();
        int y = panelY();
        if (mouseX < x || mouseX > x + panelWidth() || mouseY < y || mouseY > y + panelHeight()) {
            this.onClose();
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && beginScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        boolean handled = super.mouseClicked(event, doubled);
        if (search != null && !search.isMouseOver(mouseX, mouseY)) {
            search.setFocused(false);
        }
        if (handled) {
            return true;
        }
        int tabWidth = panelWidth() / TABS.length;
        if (mouseY >= y + 4 && mouseY <= y + 22) {
            for (int index = 0; index < TABS.length; index++) {
                int tabX = x + index * tabWidth;
                if (mouseX >= tabX && mouseX < tabX + tabWidth) {
                    saveScroll();
                    tab = TABS[index];
                    savedTab = index;
                    scroll = SAVED_SCROLL[index];
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingScrollbar && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            applyDrag(event.y(), contentView(lines().size()));
            saveScroll();
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (draggingScrollbar && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            draggingScrollbar = false;
            saveScroll();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ContentView view = contentView(lines().size());
        if (scrollY < 0) {
            scroll = Math.min(view.maxScroll, scroll + 1);
        } else if (scrollY > 0) {
            scroll = Math.max(0, scroll - 1);
        }
        saveScroll();
        return true;
    }

    private boolean beginScrollbarDrag(double mouseX, double mouseY) {
        ContentView view = contentView(lines().size());
        if (!view.canScroll() || mouseX < view.barX || mouseX > view.barX + SCROLLBAR_WIDTH
                || mouseY < view.top || mouseY > view.bottom) {
            return false;
        }
        int handleY = view.handleY(scroll);
        int handleHeight = view.handleHeight();
        if (search != null) {
            search.setFocused(false);
        }
        if (mouseY >= handleY && mouseY < handleY + handleHeight) {
            dragGrabOffset = (int) Math.round(mouseY - handleY);
        } else {
            dragGrabOffset = handleHeight / 2;
            applyDrag(mouseY, view);
        }
        draggingScrollbar = true;
        saveScroll();
        return true;
    }

    private void applyDrag(double mouseY, ContentView view) {
        int travel = Math.max(1, view.trackHeight() - view.handleHeight());
        float progress = (float) (mouseY - view.top - dragGrabOffset) / travel;
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        scroll = Math.round(progress * view.maxScroll);
        scroll = Math.max(0, Math.min(scroll, view.maxScroll));
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, ContentView view) {
        if (!view.canScroll()) {
            return;
        }
        int handleHeight = view.handleHeight();
        int handleY = view.handleY(scroll);
        graphics.fill(view.barX, view.top, view.barX + SCROLLBAR_WIDTH, view.bottom, 0xFF404040);
        graphics.outline(view.barX, view.top, SCROLLBAR_WIDTH, view.trackHeight(), 0xFF808080);
        int handleColor = draggingScrollbar ? 0xFF808080 : 0xFF606060;
        graphics.fill(view.barX + 1, handleY, view.barX + SCROLLBAR_WIDTH - 1, handleY + handleHeight, handleColor);
        graphics.outline(view.barX + 1, handleY, SCROLLBAR_WIDTH - 2, handleHeight, 0xFFC0C0C0);
    }

    private ContentView contentView(int lineCount) {
        int top = panelY() + 52;
        int bottom = panelY() + panelHeight() - 8;
        int lineHeight = this.font.lineHeight + 2;
        int visible = Math.max(1, (bottom - top) / lineHeight);
        int maxScroll = Math.max(0, lineCount - visible);
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
        int barX = panelX() + panelWidth() - SCROLLBAR_WIDTH - 4;
        return new ContentView(top, bottom, barX, visible, lineCount, maxScroll);
    }

    private void saveScroll() {
        SAVED_SCROLL[tabIndex()] = scroll;
    }

    private void rememberSearch() {
        String value = search == null ? "" : search.getValue();
        if (!value.equals(appliedSearch)) {
            appliedSearch = value;
            scroll = 0;
            saveScroll();
        }
    }

    private int tabIndex() {
        for (int index = 0; index < TABS.length; index++) {
            if (TABS[index] == tab) {
                return index;
            }
        }
        return 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<MkLevelRenderLine> lines() {
        MkLevelOverlayTab previousTab = MkLevelState.mkLevelActiveTab;
        String previousSearch = MkLevelState.mkLevelSearchText;
        MkLevelState.mkLevelActiveTab = tab;
        MkLevelState.mkLevelSearchText = search == null ? "" : search.getValue();
        try {
            return MkLevelOverlayRenderer.buildMkLevelRenderLines(this.minecraft);
        } finally {
            MkLevelState.mkLevelActiveTab = previousTab;
            MkLevelState.mkLevelSearchText = previousSearch;
        }
    }

    private int panelWidth() {
        return Math.min(360, this.width - 40);
    }

    private int panelHeight() {
        return Math.min(420, this.height - 40);
    }

    private int panelX() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelY() {
        return (this.height - panelHeight()) / 2;
    }

    private record ContentView(int top, int bottom, int barX, int visible, int lineCount, int maxScroll) {
        boolean canScroll() {
            return maxScroll > 0 && trackHeight() > 0;
        }

        int trackHeight() {
            return bottom - top;
        }

        int handleHeight() {
            int height = Math.max(10, (int) (trackHeight() * (float) visible / Math.max(lineCount, 1)));
            return Math.min(height, trackHeight());
        }

        int handleY(int scroll) {
            int travel = Math.max(0, trackHeight() - handleHeight());
            float progress = maxScroll > 0 ? (float) scroll / maxScroll : 0.0f;
            progress = Math.max(0.0f, Math.min(1.0f, progress));
            return top + Math.round(travel * progress);
        }
    }
}

package net.felix.utilities.Overall.eventsound.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.felix.utilities.Overall.eventsound.data.EventSoundKind;
import net.felix.utilities.Overall.eventsound.sound.EventSoundPlayer;
import net.felix.utilities.Overall.eventsound.data.EventSoundRule;
import net.felix.utilities.Overall.eventsound.config.EventSoundStore;
import net.felix.utilities.Other.keybinding.KeyBindingUtility;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Liste der gespeicherten Event-Sounds.
 */
public final class EventSoundListScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int ICON = 16;

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private boolean draggingScroll;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;

    public EventSoundListScreen(Screen parent) {
        super(Component.literal("Event-Sounds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int boxX = boxX();
        int boxY = boxY();
        int boxHeight = boxHeight();
        int y = boxY + boxHeight - 28;
        int remaining = Math.max(1, boxWidth() - 16);
        int plusW = Math.min(20, Math.max(1, remaining / 5));
        remaining -= plusW + 4;
        int stopW = Math.min(56, Math.max(1, remaining / 2));
        int fertigW = Math.min(80, Math.max(1, remaining - stopW - 4));
        addRenderableWidget(EventSoundUi.button("+", button -> openType(), boxX + 8, y, plusW, 20, "Event hinzufügen"));
        Button stop = EventSoundUi.stopButton(boxX + 12 + plusW, y);
        stop.setWidth(stopW);
        addRenderableWidget(stop);
        addRenderableWidget(EventSoundUi.button("Fertig", button -> onClose(), boxX + boxWidth() - fertigW - 8, y, fertigW, 20));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        EventSoundUi.dim(context, this.width, this.height);
        int boxX = boxX();
        int boxY = boxY();
        int boxHeight = boxHeight();
        EventSoundUi.panel(context, boxX, boxY, boxWidth(), boxHeight);
        context.centeredText(this.font, this.title, this.width / 2, boxY + 8, 0xFFFFFFFF);
        listX = boxX + 8;
        listY = boxY + 28;
        listWidth = boxWidth() - 16;
        listHeight = boxHeight - 64;
        rows.clear();
        String hoveredTip = null;
        List<EventSoundRule> rules = new ArrayList<>(EventSoundStore.rules());
        rules.sort(Comparator.comparingInt(rule -> kindOrder(rule.kind)));
        int contentHeight = rules.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, contentHeight - listHeight);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int contentWidth = maxScroll > 0 ? listWidth - 8 : listWidth;
        context.enableScissor(listX, listY, listX + contentWidth, listY + listHeight);
        for (int index = 0; index < rules.size(); index++) {
            int rowY = listY + index * ROW_HEIGHT - scroll;
            if (rowY + ROW_HEIGHT < listY || rowY > listY + listHeight) {
                continue;
            }
            EventSoundRule rule = rules.get(index);
            int deleteX = listX + contentWidth - ICON - 4;
            int speakerX = deleteX - ICON - 4;
            int gearX = speakerX - ICON - 4;
            boolean gearHover = EventSoundUi.inside(mouseX, mouseY, gearX, rowY + 2, ICON, ICON);
            boolean speakerHover = EventSoundUi.inside(mouseX, mouseY, speakerX, rowY + 2, ICON, ICON);
            boolean deleteHover = EventSoundUi.inside(mouseX, mouseY, deleteX, rowY + 2, ICON, ICON);
            boolean rowHover = EventSoundUi.inside(mouseX, mouseY, listX, rowY, contentWidth, ROW_HEIGHT);
            context.fill(listX, rowY, listX + contentWidth, rowY + ROW_HEIGHT - 2, rowHover ? 0xFF243044 : 0xFF1A2330);
            context.fill(listX, rowY, listX + 3, rowY + ROW_HEIGHT - 2, EventSoundUi.kindColor(rule.kind));
            String sound = rule.soundFile == null || rule.soundFile.isBlank() ? "Kein Sound" : rule.soundFile;
            int labelWidth = Math.max(0, gearX - listX - 12);
            if (contentWidth > 220) {
                EventSoundUi.text(context, this.font, EventSoundUi.trim(this.font, sound, 90), listX + contentWidth - 160, rowY + 6, 0xFFB7C3D0);
                labelWidth = Math.max(0, contentWidth - 170);
            }
            EventSoundUi.text(context, this.font, EventSoundUi.trim(this.font, rule.listLabel(), labelWidth), listX + 8, rowY + 6, 0xFFFFFFFF);
            drawIcon(context, speakerX, rowY + 2, "♪", speakerHover);
            drawIcon(context, deleteX, rowY + 2, "x", deleteHover);
            if (gearHover) {
                context.fill(gearX, rowY + 2, gearX + ICON, rowY + 2 + ICON, 0xFF3A4A5C);
            }
            EventSoundUi.drawGear(context, gearX, rowY + 2, ICON);
            if (gearHover) {
                hoveredTip = "Einstellungen öffnen";
            } else if (speakerHover) {
                hoveredTip = "Sound abspielen";
            } else if (deleteHover) {
                hoveredTip = "Event entfernen";
            }
            rows.add(new Row(rule, rowY, gearX, speakerX, deleteX));
        }
        context.disableScissor();
        if (maxScroll > 0) {
            EventSoundUi.scrollbar(context, listX + listWidth - 6, listY, listHeight, contentHeight, scroll);
        }
        if (hoveredTip != null) {
            EventSoundUi.hoverTip(context, this.font, hoveredTip, mouseX, mouseY);
        }
        if (rules.isEmpty()) {
            EventSoundUi.text(context, this.font, "Noch keine Events. Mit + eines anlegen.", listX + 4, listY + 6, 0xFFAAAAAA);
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (KeyBindingUtility.legacyMouseButton(event.button()) != 0) {
            return super.mouseClicked(event, doubled);
        }
        double mouseX = event.x();
        double mouseY = event.y();
        if (overScrollbar(mouseX, mouseY)) {
            draggingScroll = true;
            scrollTo(mouseY);
            return true;
        }
        for (Row row : rows) {
            if (EventSoundUi.inside(mouseX, mouseY, row.gearX, row.y + 2, ICON, ICON)) {
                openRule(row.rule.copy());
                return true;
            }
            if (EventSoundUi.inside(mouseX, mouseY, row.speakerX, row.y + 2, ICON, ICON)) {
                EventSoundPlayer.playFile(row.rule.soundFile, row.rule.volume);
                return true;
            }
            if (EventSoundUi.inside(mouseX, mouseY, row.deleteX, row.y + 2, ICON, ICON)) {
                EventSoundStore.remove(row.rule.id);
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!draggingScroll) {
            return super.mouseDragged(event, deltaX, deltaY);
        }
        scrollTo(event.y());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScroll = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (EventSoundUi.inside(mouseX, mouseY, listX, listY, listWidth, listHeight)) {
            scroll -= (int) (vertical * ROW_HEIGHT);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    private static int kindOrder(EventSoundKind kind) {
        if (kind == EventSoundKind.BLUEPRINT) {
            return 0;
        }
        if (kind == EventSoundKind.CUSTOM) {
            return 1;
        }
        return 2;
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        int contentHeight = EventSoundStore.rules().size() * ROW_HEIGHT;
        if (contentHeight <= listHeight) {
            return false;
        }
        return EventSoundUi.inside(mouseX, mouseY, listX + listWidth - 8, listY, 8, listHeight);
    }

    private void scrollTo(double mouseY) {
        int contentHeight = EventSoundStore.rules().size() * ROW_HEIGHT;
        scroll = EventSoundUi.scrollFromMouse(mouseY, listY, listHeight, contentHeight);
    }

    private void openType() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(new EventSoundTypeScreen(this));
        }
    }

    private void openRule(EventSoundRule rule) {
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.gui.setScreen(new EventSoundEditScreen(this, rule));
    }

    private void drawIcon(GuiGraphicsExtractor context, int x, int y, String label, boolean hovered) {
        context.fill(x, y, x + ICON, y + ICON, hovered ? 0xFF505050 : 0xFF3A3A3A);
        int textX = x + (ICON - this.font.width(label)) / 2;
        EventSoundUi.text(context, this.font, label, textX, y + 4, 0xFFFFFFFF);
    }

    private int boxWidth() {
        return EventSoundUi.fit(this.width, 460, 8);
    }

    private int boxX() {
        return Math.max(4, (this.width - boxWidth()) / 2);
    }

    private int boxHeight() {
        return EventSoundUi.fit(this.height, 360, 8);
    }

    private int boxY() {
        return Math.max(4, (this.height - boxHeight()) / 2);
    }

    private record Row(EventSoundRule rule, int y, int gearX, int speakerX, int deleteX) {
    }
}

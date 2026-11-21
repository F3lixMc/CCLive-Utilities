package net.felix.utilities.Overall.eventsound.ui;

import net.felix.utilities.Overall.eventsound.data.EventSoundKind;
import net.felix.utilities.Overall.eventsound.chat.EventSoundMatcher;
import net.felix.utilities.Overall.eventsound.data.EventSoundRule;
import net.felix.utilities.Overall.eventsound.config.EventSoundStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Auswahl des Schmiedezustands, bei dem ein Sound abgespielt wird.
 */
public final class SmithingStateScreen extends Screen {
    private final Screen listScreen;
    private final Screen backScreen;
    private final EventSoundRule editing;

    public SmithingStateScreen(Screen listScreen, Screen backScreen) {
        this(listScreen, backScreen, null);
    }

    public SmithingStateScreen(Screen listScreen, Screen backScreen, EventSoundRule editing) {
        super(Component.literal("Schmiedezustand"));
        this.listScreen = listScreen;
        this.backScreen = backScreen;
        this.editing = editing;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.max(20, panelW() - 24);
        int x = panelX() + Math.max(0, (panelW() - buttonWidth) / 2);
        int rows = EventSoundMatcher.SMITHING_STATES.size() + 1;
        int available = Math.max(rows, panelH() - 20);
        int step = Math.min(24, available / rows);
        int buttonH = Math.max(8, Math.min(20, step - 2));
        int y = panelY() + Math.max(8, Math.min(24, panelH() - step * rows - 8));
        for (int index = 0; index < EventSoundMatcher.SMITHING_STATES.size(); index++) {
            String state = EventSoundMatcher.SMITHING_STATES.get(index);
            addRenderableWidget(EventSoundUi.button(state, button -> choose(state), x, y + index * step, buttonWidth, buttonH));
        }
        addRenderableWidget(EventSoundUi.button("Zurück", button -> onClose(), x, y + EventSoundMatcher.SMITHING_STATES.size() * step, buttonWidth, buttonH));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        EventSoundUi.dim(context, this.width, this.height);
        EventSoundUi.panel(context, panelX(), panelY(), panelW(), panelH());
        context.centeredText(this.font, this.title, this.width / 2, panelY() + 8, 0xFFFFFFFF);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(backScreen);
        }
    }

    private int panelW() {
        return EventSoundUi.fit(this.width, 240, 8);
    }

    private int panelH() {
        return EventSoundUi.fit(this.height, EventSoundMatcher.SMITHING_STATES.size() * 24 + 72, 8);
    }

    private int panelX() {
        return Math.max(4, (this.width - panelW()) / 2);
    }

    private int panelY() {
        return Math.max(4, (this.height - panelH()) / 2);
    }

    private void choose(String state) {
        if (this.minecraft == null) {
            return;
        }
        if (editing != null) {
            editing.kind = EventSoundKind.SMITHING;
            editing.target = state;
            this.minecraft.gui.setScreen(backScreen);
            return;
        }
        EventSoundRule rule = EventSoundStore.findSmithing(state).orElseGet(() -> {
            EventSoundRule created = new EventSoundRule();
            created.kind = EventSoundKind.SMITHING;
            created.target = state;
            return created;
        });
        this.minecraft.gui.setScreen(new EventSoundEditScreen(listScreen, rule));
    }
}

package net.felix.utilities.Overall.eventsound.ui;

import net.felix.utilities.Overall.eventsound.data.EventSoundKind;
import net.felix.utilities.Overall.eventsound.data.EventSoundRule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Auswahl, welche Art von Event angelegt wird.
 */
public final class EventSoundTypeScreen extends Screen {
    private final Screen listScreen;

    public EventSoundTypeScreen(Screen listScreen) {
        super(Component.literal("Event hinzufügen"));
        this.listScreen = listScreen;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.max(20, panelW() - 24);
        int x = panelX() + Math.max(0, (panelW() - buttonWidth) / 2);
        int rows = 4;
        int available = Math.max(rows, panelH() - 28);
        int step = Math.min(28, available / rows);
        int buttonH = Math.max(8, Math.min(20, step - 2));
        int y = panelY() + Math.max(8, Math.min(36, panelH() - step * rows - 8));
        addRenderableWidget(EventSoundUi.button("Schmiedezustand", button -> openSmithing(), x, y, buttonWidth, buttonH));
        addRenderableWidget(EventSoundUi.button("Bauplan-Drop", button -> openBlueprint(), x, y + step, buttonWidth, buttonH));
        addRenderableWidget(EventSoundUi.button("Custom", button -> openCustom(), x, y + step * 2, buttonWidth, buttonH));
        addRenderableWidget(EventSoundUi.button("Zurück", button -> onClose(), x, y + step * 3, buttonWidth, buttonH));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        EventSoundUi.dim(context, this.width, this.height);
        EventSoundUi.panel(context, panelX(), panelY(), panelW(), panelH());
        context.centeredText(this.font, this.title, this.width / 2, panelY() + 12, 0xFFFFFFFF);
        EventSoundUi.text(context, this.font, "Wähle, wobei ein Sound kommt.", panelX() + 16, panelY() + 28, 0xFFB7C3D0);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(listScreen);
        }
    }

    private int panelW() {
        return EventSoundUi.fit(this.width, 220, 8);
    }

    private int panelH() {
        return EventSoundUi.fit(this.height, 176, 8);
    }

    private int panelX() {
        return Math.max(4, (this.width - panelW()) / 2);
    }

    private int panelY() {
        return Math.max(4, (this.height - panelH()) / 2);
    }

    private void openSmithing() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(new SmithingStateScreen(listScreen, this));
        }
    }

    private void openBlueprint() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(new BlueprintPickScreen(listScreen, this));
        }
    }

    private void openCustom() {
        if (this.minecraft == null) {
            return;
        }
        EventSoundRule rule = new EventSoundRule();
        rule.kind = EventSoundKind.CUSTOM;
        this.minecraft.gui.setScreen(new EventSoundEditScreen(listScreen, rule));
    }
}

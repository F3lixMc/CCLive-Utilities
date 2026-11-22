package net.felix.utilities.Overall.eventsound.ui;

import net.felix.utilities.Overall.eventsound.data.EventSoundKind;
import net.felix.utilities.Overall.eventsound.chat.EventSoundMatcher;
import net.felix.utilities.Overall.eventsound.data.EventSoundRule;
import net.felix.utilities.Overall.eventsound.config.EventSoundStore;
import net.felix.utilities.Other.keybinding.KeyBindingUtility;
import net.felix.utilities.Overall.itemviewer.data.ItemCatalog;
import net.felix.utilities.Overall.itemviewer.data.ItemData;
import net.felix.utilities.Overall.itemviewer.tooltip.TooltipColors;
import net.felix.utilities.Overall.itemviewer.filtermenu.FilterMenu;
import net.felix.utilities.Overall.itemviewer.ui.FilterOverlayControls;
import net.felix.utilities.Overall.itemviewer.ui.HelpOverlay;
import net.felix.utilities.Overall.itemviewer.ui.ViewerInputHandler;
import net.felix.utilities.Overall.itemviewer.ui.ViewerLayout;
import net.felix.utilities.Overall.itemviewer.ui.ViewerPosition;
import net.felix.utilities.Overall.itemviewer.ui.ViewerRenderer;
import net.felix.utilities.Overall.itemviewer.ui.ViewerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Zeigt die Chat-Zeile eines Bauplan-Drops und lässt nur Aincraft-Baupläne im ItemViewer wählen.
 */
public final class BlueprintPickScreen extends Screen {
    private final Screen listScreen;
    private final Screen backScreen;
    private final EventSoundRule editing;
    private String selectedName = "";
    private int selectedColor = 0xFF8A97A6;
    private String status = "";

    public BlueprintPickScreen(Screen listScreen, Screen backScreen) {
        this(listScreen, backScreen, null);
    }

    public BlueprintPickScreen(Screen listScreen, Screen backScreen, EventSoundRule editing) {
        super(Component.literal("Bauplan-Drop"));
        this.listScreen = listScreen;
        this.backScreen = backScreen;
        this.editing = editing;
    }

    @Override
    protected void init() {
        if (selectedName.isEmpty() && editing != null && editing.target != null && !editing.target.isBlank()) {
            ItemData known = ItemCatalog.findItemByName(editing.target);
            if (known != null && "blueprints".equals(known.category)) {
                remember(known);
            } else {
                selectedName = editing.target;
            }
        }
        BlueprintSoundPick.start(this::choose);
        int[] card = cardBounds();
        int buttonW = Math.max(1, Math.min(80, (card[2] - 26) / 2));
        addRenderableWidget(EventSoundUi.button("Speichern", button -> save(), card[0] + 10, card[1] + card[3] - 28, buttonW, 20));
        addRenderableWidget(EventSoundUi.button("Zurück", button -> onClose(), card[0] + 16 + buttonW, card[1] + card[3] - 28, buttonW, 20));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        EventSoundUi.dim(context, this.width, this.height);
        int[] card = cardBounds();
        EventSoundUi.panel(context, card[0], card[1], card[2], card[3]);
        EventSoundUi.text(context, this.font, "Bauplan auswählen", card[0] + 10, card[1] + 8, 0xFFFFFFFF);
        EventSoundUi.text(context, this.font, "Wähle einen Bauplan aus.", card[0] + 10, card[1] + 22, 0xFFB7C3D0);
        EventSoundUi.drawBlueprintMessage(context, this.font, card[0] + 10, card[1] + 76, card[2] - 20, selectedName, selectedColor);
        if (!status.isEmpty()) {
            EventSoundUi.text(context, this.font, status, card[0] + 10, card[1] + 102, 0xFFFF5555);
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            ViewerState.updateMousePosition(mouseX, mouseY);
            ViewerRenderer.renderItemViewer(context, client, ViewerLayout.calculateViewerPositionBesideCenteredInventory(this));
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = KeyBindingUtility.legacyMouseButton(event.button());
        if (HelpOverlay.isHelpOverlayOpen()) {
            HelpOverlay.handleHelpOverlayClick(mouseX, mouseY, button);
            return true;
        }
        if (FilterMenu.isOpen() && FilterOverlayControls.handleFilterOverlayClick(mouseX, mouseY, button)) {
            return true;
        }
        if (ViewerInputHandler.handleMouseClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        int button = KeyBindingUtility.legacyMouseButton(event.button());
        if (FilterMenu.isOpen() && FilterOverlayControls.handleFilterOverlayDrag(event.x(), event.y(), button)) {
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        int button = KeyBindingUtility.legacyMouseButton(event.button());
        if (FilterMenu.isOpen() && FilterOverlayControls.handleFilterOverlayRelease(event.x(), event.y(), button)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void removed() {
        BlueprintSoundPick.stop();
        super.removed();
    }

    @Override
    public void onClose() {
        BlueprintSoundPick.stop();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(backScreen);
        }
    }

    private void choose(ItemData item) {
        if (!remember(item)) {
            return;
        }
        status = "";
    }

    private void save() {
        if (this.minecraft == null) {
            return;
        }
        if (selectedName.isBlank()) {
            status = "Bitte einen Bauplan auswählen";
            return;
        }
        if (editing != null) {
            editing.kind = EventSoundKind.BLUEPRINT;
            editing.target = selectedName;
            this.minecraft.gui.setScreen(backScreen);
            return;
        }
        String name = selectedName;
        EventSoundRule rule = EventSoundStore.findBlueprint(name).orElseGet(() -> {
            EventSoundRule created = new EventSoundRule();
            created.kind = EventSoundKind.BLUEPRINT;
            created.target = name;
            return created;
        });
        this.minecraft.gui.setScreen(new EventSoundEditScreen(listScreen, rule));
    }

    private boolean remember(ItemData item) {
        if (item == null || !"blueprints".equals(item.category)) {
            return false;
        }
        String name = EventSoundMatcher.cleanName(item.name);
        if (name.isEmpty()) {
            return false;
        }
        selectedName = name;
        String rarity = item.info == null ? null : item.info.rarity;
        selectedColor = TooltipColors.getRarityColor(rarity);
        return true;
    }

    private int[] cardBounds() {
        ViewerPosition viewer = ViewerLayout.calculateViewerPositionBesideCenteredInventory(this);
        int width = Math.min(420, Math.max(0, this.width - 16));
        int beside = viewer.viewerX - 28;
        if (beside > 0) {
            width = Math.min(width, beside);
        }
        int height = Math.min(168, Math.max(0, this.height - 16));
        int x = 8;
        int y = Math.max(8, (this.height - height) / 2);
        if (viewer.viewerX < width + 24) {
            x = Math.max(8, (this.width - width) / 2);
            y = Math.max(8, this.height - height - 8);
        }
        x = Math.min(x, Math.max(4, this.width - width - 4));
        y = Math.min(y, Math.max(4, this.height - height - 4));
        return new int[] { x, y, width, height };
    }
}

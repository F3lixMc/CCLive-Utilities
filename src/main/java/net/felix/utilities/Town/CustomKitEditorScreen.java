package net.felix.utilities.Town;

import net.felix.utilities.ItemViewer.ItemData;
import net.felix.utilities.ItemViewer.ItemViewerGrid;
import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;

/**
 * Editor zum Erstellen/Bearbeiten eines eigenen Kits.
 * Gleiche Panel-Größe wie {@link KitSelectionScreen}.
 */
public class CustomKitEditorScreen extends Screen {

    private static final int SLOT_SIZE = 18;
    private static final int GRID_SPACING = SLOT_SIZE + 2;
    private static final int GRID_COLUMNS = 7;
    private static final int GRID_ROWS = 5;
    private static final int PADDING = 15;
    private static final int EXTRA_SIZE = 2;
    private static final Identifier KITS_BACKGROUND_TEXTURE =
            Identifier.of("cclive-utilities", "textures/gui/kits_background.png");
    private static final Text EMPTY_ITEM_SLOT_TOOLTIP =
            Text.literal("Klicke mit der linken Maustaste auf ein Item, um es auszuwählen");
    private static final int ICON_PICKER_COLUMNS = 3;
    private static final int ICON_PICKER_ROWS = 5;
    private static final int ICON_PICKER_GAP = 10;
    private static final Item[] PRESET_ICON_ITEMS = {

            Items.CACTUS,
            Items.GOLD_NUGGET,
            Items.DIAMOND_SWORD,
            Items.CHORUS_FRUIT,
            Items.ANVIL,                       
            Items.DIAMOND_PICKAXE,
            Items.SOUL_TORCH,
            Items.FURNACE,           
            Items.DIAMOND_AXE,
            Items.NETHER_STAR,  
            Items.DIAMOND_CHESTPLATE,                      
            Items.DIAMOND_HOE,
            Items.WITHER_SKELETON_SKULL,
            Items.SHIELD,
            Items.FISHING_ROD

    };

    public enum PickTarget {
        ADD_ITEM,
        ICON
    }

    private final int buttonIndex;
    private final Screen returnScreen;
    private final HandledScreen<?> itemViewerScreen;
    private final CustomKit kit;
    private final boolean editingExisting;

    private TextFieldWidget nameField;
    private ButtonWidget doneButton;
    private ButtonWidget cancelButton;
    private ButtonWidget deleteButton;
    private ButtonWidget confirmDeleteButton;
    private ButtonWidget confirmCancelDialogButton;
    private boolean showDeleteConfirmation = false;

    private PickTarget pickTarget = PickTarget.ADD_ITEM;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int gridStartX;
    private int gridStartY;
    private int iconSlotX;
    private int iconSlotY;
    private int iconPickerX;
    private int iconPickerY;
    private int iconPickerWidth;
    private int iconPickerHeight;
    private int iconPickerGridStartX;
    private int iconPickerGridStartY;
    private ItemData hoveredSlotItem = null;
    private boolean hoveredEmptyItemSlot = false;
    private String hoveredPresetIconName = null;

    public CustomKitEditorScreen(int buttonIndex, Screen returnScreen, HandledScreen<?> itemViewerScreen) {
        super(Text.literal("Eigenes Kit"));
        this.buttonIndex = buttonIndex;
        this.returnScreen = returnScreen;
        this.itemViewerScreen = itemViewerScreen;
        this.kit = new CustomKit();
        this.editingExisting = false;
    }

    public CustomKitEditorScreen(int buttonIndex, Screen returnScreen, HandledScreen<?> itemViewerScreen, CustomKit existing) {
        super(Text.literal("Eigenes Kit"));
        this.buttonIndex = buttonIndex;
        this.returnScreen = returnScreen;
        this.itemViewerScreen = itemViewerScreen;
        this.kit = existing != null ? existing.copy() : new CustomKit();
        this.editingExisting = existing != null;
    }

    private void computePanelLayout() {
        int actualGridWidth = (GRID_COLUMNS - 1) * GRID_SPACING + SLOT_SIZE;
        int actualGridHeight = (GRID_ROWS - 1) * GRID_SPACING + SLOT_SIZE;
        panelWidth = actualGridWidth + PADDING * 2 + EXTRA_SIZE * 2;
        panelHeight = actualGridHeight + PADDING * 2 + EXTRA_SIZE * 2;
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        gridStartX = panelX + PADDING + EXTRA_SIZE;
        gridStartY = panelY + PADDING + EXTRA_SIZE;
        iconSlotX = gridStartX;
        iconSlotY = gridStartY;

        int pickerGridWidth = (ICON_PICKER_COLUMNS - 1) * GRID_SPACING + SLOT_SIZE;
        int pickerGridHeight = (ICON_PICKER_ROWS - 1) * GRID_SPACING + SLOT_SIZE;
        iconPickerWidth = pickerGridWidth + PADDING * 2 + EXTRA_SIZE * 2;
        iconPickerHeight = pickerGridHeight + PADDING * 2 + EXTRA_SIZE * 2;
        iconPickerX = panelX - ICON_PICKER_GAP - iconPickerWidth;
        iconPickerY = panelY;
        iconPickerGridStartX = iconPickerX + PADDING + EXTRA_SIZE;
        iconPickerGridStartY = iconPickerY + PADDING + EXTRA_SIZE;
    }

    @Override
    protected void init() {
        super.init();
        computePanelLayout();

        int nameFieldX = gridStartX + GRID_SPACING;
        int nameFieldWidth = (GRID_COLUMNS - 1) * GRID_SPACING - 2;
        nameField = new TextFieldWidget(this.textRenderer, nameFieldX, gridStartY + 1, nameFieldWidth, SLOT_SIZE - 2, Text.literal("Kit-Name"));
        nameField.setMaxLength(64);
        nameField.setText(kit.name);
        nameField.setFocused(false);
        this.addDrawableChild(nameField);

        int buttonY = this.height - 30;
        int buttonWidth = 70;
        int buttonGap = 10;
        if (editingExisting) {
            int totalWidth = buttonWidth * 3 + buttonGap * 2;
            int startX = (this.width - totalWidth) / 2;

            deleteButton = ButtonWidget.builder(Text.literal("Löschen"), button -> openDeleteConfirmation())
                    .dimensions(startX, buttonY, buttonWidth, 20)
                    .build();
            this.addDrawableChild(deleteButton);

            doneButton = ButtonWidget.builder(Text.literal("Fertig"), button -> onDone())
                    .dimensions(startX + buttonWidth + buttonGap, buttonY, buttonWidth, 20)
                    .build();
            this.addDrawableChild(doneButton);

            cancelButton = ButtonWidget.builder(Text.literal("Abbrechen"), button -> onCancel())
                    .dimensions(startX + (buttonWidth + buttonGap) * 2, buttonY, buttonWidth, 20)
                    .build();
            this.addDrawableChild(cancelButton);
        } else {
            doneButton = ButtonWidget.builder(Text.literal("Fertig"), button -> onDone())
                    .dimensions(this.width / 2 - 100, buttonY, 80, 20)
                    .build();
            this.addDrawableChild(doneButton);

            cancelButton = ButtonWidget.builder(Text.literal("Abbrechen"), button -> onCancel())
                    .dimensions(this.width / 2 + 20, buttonY, 80, 20)
                    .build();
            this.addDrawableChild(cancelButton);
        }

        ItemViewerUtility.startKitEditorMode(itemViewerScreen, this::onItemPickedFromViewer);
        ItemViewerUtility.setKitEditorPickTarget(PickTarget.ADD_ITEM);

        if (showDeleteConfirmation) {
            initDeleteConfirmationButtons();
        }
    }

    private void getItemSlotPosition(int itemIndex, int[] out) {
        int slotIndex = itemIndex + GRID_COLUMNS;
        out[0] = gridStartX + (slotIndex % GRID_COLUMNS) * GRID_SPACING;
        out[1] = gridStartY + (slotIndex / GRID_COLUMNS) * GRID_SPACING;
    }

    private void onItemPickedFromViewer(ItemData item, PickTarget target) {
        if (item == null) {
            return;
        }
        if (target == PickTarget.ICON) {
            if (item.id != null && !item.id.isEmpty()) {
                kit.iconItemId = item.id;
            }
            if (item.name != null && !item.name.isEmpty()) {
                kit.iconItemName = item.name;
            }
            clearIconFocus();
        } else if (item.name != null && !item.name.isEmpty()) {
            int maxItems = GRID_COLUMNS * (GRID_ROWS - 1);
            if (!kit.itemNames.contains(item.name) && kit.itemNames.size() < maxItems) {
                kit.itemNames.add(item.name);
            }
        }
    }

    private boolean isMouseOverNameField(double mouseX, double mouseY) {
        if (nameField == null || !nameField.isVisible()) {
            return false;
        }
        return mouseX >= nameField.getX() && mouseX < nameField.getX() + nameField.getWidth()
                && mouseY >= nameField.getY() && mouseY < nameField.getY() + nameField.getHeight();
    }

    private void renderDimBackground(DrawContext context) {
        context.fill(0, 0, this.width, this.height, 0xA0101010);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ItemViewerUtility.updateMousePosition(mouseX, mouseY);

        renderDimBackground(context);

        try {
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    KITS_BACKGROUND_TEXTURE,
                    panelX, panelY,
                    0.0f, 0.0f,
                    panelWidth, panelHeight,
                    panelWidth, panelHeight
            );
        } catch (Exception e) {
            context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0202020);
        }

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        hoveredSlotItem = null;
        hoveredEmptyItemSlot = false;
        hoveredPresetIconName = null;

        boolean iconHovered = mouseX >= iconSlotX && mouseX < iconSlotX + SLOT_SIZE
                && mouseY >= iconSlotY && mouseY < iconSlotY + SLOT_SIZE;
        renderSlot(context, iconSlotX, iconSlotY, pickTarget == PickTarget.ICON, iconHovered);
        ItemStack iconStack = kit.createIconStack();
        if (!iconStack.isEmpty()) {
            context.drawItem(iconStack, iconSlotX + 1, iconSlotY + 1, 0);
        }
        if (iconHovered) {
            hoveredSlotItem = resolveItemData(kit.iconItemName);
        }

        renderItemList(context, mouseX, mouseY);

        if (!showDeleteConfirmation && pickTarget == PickTarget.ICON) {
            renderIconPicker(context, mouseX, mouseY);
        }

        if (showDeleteConfirmation) {
            renderDeleteConfirmationDialog(context);
        }

        super.render(context, mouseX, mouseY, delta);

        if (!showDeleteConfirmation) {
            ItemViewerUtility.renderKitEditorItemViewer(context, client, mouseX, mouseY);
        }

        if (!showDeleteConfirmation && hoveredSlotItem != null) {
            ItemViewerGrid.updateAspectOverlayForItem(hoveredSlotItem);
            ItemViewerGrid.renderItemTooltip(context, hoveredSlotItem, mouseX, mouseY);
        } else if (!showDeleteConfirmation && hoveredPresetIconName != null) {
            context.drawTooltip(
                    this.textRenderer,
                    Collections.singletonList(Text.literal(hoveredPresetIconName)),
                    mouseX,
                    mouseY
            );
        } else if (!showDeleteConfirmation && hoveredEmptyItemSlot) {
            context.drawTooltip(this.textRenderer, Collections.singletonList(EMPTY_ITEM_SLOT_TOOLTIP), mouseX, mouseY);
        }
    }

    private void renderIconPicker(DrawContext context, int mouseX, int mouseY) {
        hoveredPresetIconName = null;
        try {
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    KITS_BACKGROUND_TEXTURE,
                    iconPickerX, iconPickerY,
                    0.0f, 0.0f,
                    iconPickerWidth, iconPickerHeight,
                    iconPickerWidth, iconPickerHeight
            );
        } catch (Exception e) {
            context.fill(iconPickerX, iconPickerY, iconPickerX + iconPickerWidth, iconPickerY + iconPickerHeight, 0xE0202020);
        }

        for (int i = 0; i < PRESET_ICON_ITEMS.length; i++) {
            int col = i % ICON_PICKER_COLUMNS;
            int row = i / ICON_PICKER_COLUMNS;
            int slotX = iconPickerGridStartX + col * GRID_SPACING;
            int slotY = iconPickerGridStartY + row * GRID_SPACING;
            boolean hovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                    && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
            boolean selected = isPresetIconSelected(PRESET_ICON_ITEMS[i]);
            renderSlot(context, slotX, slotY, selected, hovered);

            ItemStack stack = new ItemStack(PRESET_ICON_ITEMS[i]);
            context.drawItem(stack, slotX + 1, slotY + 1, 0);
            if (hovered) {
                hoveredPresetIconName = stack.getName().getString();
            }
        }
    }

    private boolean isPresetIconSelected(Item item) {
        if (item == null || kit.iconItemId == null) {
            return false;
        }
        Identifier presetId = Registries.ITEM.getId(item);
        Identifier kitId = Identifier.tryParse(kit.iconItemId);
        if (kitId == null) {
            return false;
        }
        boolean idMatches = presetId.equals(kitId);
        boolean noCustomName = kit.iconItemName == null || kit.iconItemName.isEmpty();
        return idMatches && noCustomName;
    }

    private void selectPresetIcon(Item item) {
        if (item == null) {
            return;
        }
        kit.iconItemId = Registries.ITEM.getId(item).toString();
        kit.iconItemName = null;
        clearIconFocus();
    }

    private void clearIconFocus() {
        if (pickTarget == PickTarget.ICON) {
            pickTarget = PickTarget.ADD_ITEM;
            ItemViewerUtility.setKitEditorPickTarget(PickTarget.ADD_ITEM);
        }
    }

    private boolean handleIconPickerClick(double mouseX, double mouseY) {
        if (pickTarget != PickTarget.ICON) {
            return false;
        }
        for (int i = 0; i < PRESET_ICON_ITEMS.length; i++) {
            int col = i % ICON_PICKER_COLUMNS;
            int row = i / ICON_PICKER_COLUMNS;
            int slotX = iconPickerGridStartX + col * GRID_SPACING;
            int slotY = iconPickerGridStartY + row * GRID_SPACING;
            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                    && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                selectPresetIcon(PRESET_ICON_ITEMS[i]);
                return true;
            }
        }
        return false;
    }

    private ItemData resolveItemData(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return null;
        }
        return ItemViewerUtility.findItemByNameFuzzy(itemName);
    }

    private void renderItemList(DrawContext context, int mouseX, int mouseY) {
        int maxItems = GRID_COLUMNS * (GRID_ROWS - 1);
        int[] pos = new int[2];
        for (int i = 0; i < kit.itemNames.size(); i++) {
            getItemSlotPosition(i, pos);
            boolean hovered = mouseX >= pos[0] && mouseX < pos[0] + SLOT_SIZE
                    && mouseY >= pos[1] && mouseY < pos[1] + SLOT_SIZE;
            renderSlot(context, pos[0], pos[1], false, hovered);

            ItemStack itemStack = kit.createItemDisplayStack(kit.itemNames.get(i));
            if (!itemStack.isEmpty()) {
                context.drawItem(itemStack, pos[0] + 1, pos[1] + 1, 0);
            }
            if (hovered) {
                hoveredSlotItem = resolveItemData(kit.itemNames.get(i));
            }
        }

        if (kit.itemNames.size() < maxItems) {
            getItemSlotPosition(kit.itemNames.size(), pos);
            boolean hovered = mouseX >= pos[0] && mouseX < pos[0] + SLOT_SIZE
                    && mouseY >= pos[1] && mouseY < pos[1] + SLOT_SIZE;
            renderSlot(context, pos[0], pos[1], false, hovered);
            if (hovered) {
                hoveredEmptyItemSlot = true;
            }
        }
    }

    private void renderSlot(DrawContext context, int x, int y, boolean selected, boolean hovered) {
        int bg = selected ? 0xFF00AA00 : (hovered ? 0xFFFFFFFF : 0xFF808080);
        context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bg);
        int border = selected ? 0xFF006600 : 0xFF000000;
        context.fill(x, y, x + SLOT_SIZE, y + 1, border);
        context.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, border);
        context.fill(x, y, x + 1, y + SLOT_SIZE, border);
        context.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, border);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showDeleteConfirmation) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (net.felix.utilities.ItemViewer.ItemViewerFilterMenu.isOpen()) {
            if (ItemViewerUtility.handleFilterOverlayClick(mouseX, mouseY, button)) {
                return true;
            }
        }

        ItemViewerUtility.blurSearchFieldFocusUnlessClickOnField(mouseX, mouseY, button);

        if (nameField != null && !isMouseOverNameField(mouseX, mouseY)) {
            nameField.setFocused(false);
        }

        if (button == 0 && handleIconPickerClick(mouseX, mouseY)) {
            return true;
        }

        if (ItemViewerUtility.isMouseOverKitEditorItemViewer(mouseX, mouseY)) {
            if (ItemViewerUtility.handleMouseClick(mouseX, mouseY, button)) {
                if (button == 0) {
                    clearIconFocus();
                }
                return true;
            }
            if (button == 0) {
                clearIconFocus();
            }
        }

        if (button == 0) {
            if (mouseX >= iconSlotX && mouseX < iconSlotX + SLOT_SIZE
                    && mouseY >= iconSlotY && mouseY < iconSlotY + SLOT_SIZE) {
                pickTarget = PickTarget.ICON;
                ItemViewerUtility.setKitEditorPickTarget(PickTarget.ICON);
                return true;
            }

            int[] pos = new int[2];
            for (int i = 0; i < kit.itemNames.size(); i++) {
                getItemSlotPosition(i, pos);
                if (mouseX >= pos[0] && mouseX < pos[0] + SLOT_SIZE
                        && mouseY >= pos[1] && mouseY < pos[1] + SLOT_SIZE) {
                    pickTarget = PickTarget.ADD_ITEM;
                    ItemViewerUtility.setKitEditorPickTarget(PickTarget.ADD_ITEM);
                    return true;
                }
            }

            int maxItems = GRID_COLUMNS * (GRID_ROWS - 1);
            if (kit.itemNames.size() < maxItems) {
                getItemSlotPosition(kit.itemNames.size(), pos);
                if (mouseX >= pos[0] && mouseX < pos[0] + SLOT_SIZE
                        && mouseY >= pos[1] && mouseY < pos[1] + SLOT_SIZE) {
                    pickTarget = PickTarget.ADD_ITEM;
                    ItemViewerUtility.setKitEditorPickTarget(PickTarget.ADD_ITEM);
                    return true;
                }
            }

            clearIconFocus();
        } else if (button == 1) {
            int[] pos = new int[2];
            for (int i = 0; i < kit.itemNames.size(); i++) {
                getItemSlotPosition(i, pos);
                if (mouseX >= pos[0] && mouseX < pos[0] + SLOT_SIZE
                        && mouseY >= pos[1] && mouseY < pos[1] + SLOT_SIZE) {
                    kit.itemNames.remove(i);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (net.felix.utilities.ItemViewer.ItemViewerFilterMenu.isOpen()) {
            if (ItemViewerUtility.handleFilterOverlayDrag(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (ItemViewerUtility.handleFilterOverlayRelease(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void onDone() {
        String name = nameField.getText().trim();
        kit.name = name.isEmpty() ? "Neues Kit" : name;

        if (editingExisting) {
            CustomKitManager.updateKit(kit);
        } else {
            CustomKitManager.addKit(buttonIndex, kit);
        }

        closeToReturnScreen();
    }

    private void onCancel() {
        closeToReturnScreen();
    }

    private void openDeleteConfirmation() {
        if (!editingExisting || kit.id == null || kit.id.isEmpty()) {
            return;
        }
        showDeleteConfirmation = true;
        setEditorWidgetsVisible(false);
        initDeleteConfirmationButtons();
    }

    private void initDeleteConfirmationButtons() {
        if (confirmDeleteButton != null) {
            this.remove(confirmDeleteButton);
        }
        if (confirmCancelDialogButton != null) {
            this.remove(confirmCancelDialogButton);
        }

        int boxWidth = 220;
        int boxHeight = 80;
        int boxX = (this.width - boxWidth) / 2;
        int boxY = (this.height - boxHeight) / 2;
        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonGap = 10;
        int buttonsY = boxY + boxHeight - buttonHeight - 12;
        int buttonsStartX = boxX + (boxWidth - (buttonWidth * 2 + buttonGap)) / 2;

        confirmDeleteButton = ButtonWidget.builder(Text.literal("Löschen"), button -> performDelete())
                .dimensions(buttonsStartX, buttonsY, buttonWidth, buttonHeight)
                .build();
        confirmCancelDialogButton = ButtonWidget.builder(Text.literal("Abbrechen"), button -> closeDeleteConfirmation())
                .dimensions(buttonsStartX + buttonWidth + buttonGap, buttonsY, buttonWidth, buttonHeight)
                .build();
        this.addDrawableChild(confirmDeleteButton);
        this.addDrawableChild(confirmCancelDialogButton);
    }

    private void renderDeleteConfirmationDialog(DrawContext context) {
        context.fill(0, 0, this.width, this.height, 0xC0000000);

        int boxWidth = 220;
        int boxHeight = 80;
        int boxX = (this.width - boxWidth) / 2;
        int boxY = (this.height - boxHeight) / 2;
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF202020);
        context.drawBorder(boxX, boxY, boxWidth, boxHeight, 0xFFFFFFFF);

        String question = "Wirklich Löschen?";
        int textWidth = this.textRenderer.getWidth(question);
        int textX = boxX + (boxWidth - textWidth) / 2;
        int textY = boxY + 14;
        context.drawText(this.textRenderer, question, textX, textY, 0xFFFFFFFF, true);
    }

    private void closeDeleteConfirmation() {
        showDeleteConfirmation = false;
        if (confirmDeleteButton != null) {
            this.remove(confirmDeleteButton);
            confirmDeleteButton = null;
        }
        if (confirmCancelDialogButton != null) {
            this.remove(confirmCancelDialogButton);
            confirmCancelDialogButton = null;
        }
        setEditorWidgetsVisible(true);
    }

    private void setEditorWidgetsVisible(boolean visible) {
        if (nameField != null) {
            nameField.setVisible(visible);
            if (!visible) {
                nameField.setFocused(false);
            }
        }
        if (doneButton != null) {
            doneButton.visible = visible;
        }
        if (cancelButton != null) {
            cancelButton.visible = visible;
        }
        if (deleteButton != null) {
            deleteButton.visible = visible;
        }
    }

    private void performDelete() {
        if (!editingExisting || kit.id == null || kit.id.isEmpty()) {
            closeDeleteConfirmation();
            return;
        }
        String kitId = kit.id;
        closeDeleteConfirmation();
        CustomKitManager.deleteKit(kitId);
        KitFilterUtility.clearCustomKitSelectionIfDeleted(kitId);
        closeToReturnScreen();
    }

    private void closeToReturnScreen() {
        ItemViewerUtility.stopKitEditorMode();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && returnScreen != null) {
            if (returnScreen instanceof KitSelectionScreen kitScreen) {
                kitScreen.switchToCustomTab();
            } else if (returnScreen instanceof KitViewScreen viewScreen) {
                viewScreen.switchToCustomTab();
            }
            client.setScreen(returnScreen);
        } else {
            this.close();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (showDeleteConfirmation) {
                closeDeleteConfirmation();
                return true;
            }
            onCancel();
            return true;
        }
        if (ItemViewerUtility.handleFilterOverlayKeyPress(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        ItemViewerUtility.stopKitEditorMode();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

}

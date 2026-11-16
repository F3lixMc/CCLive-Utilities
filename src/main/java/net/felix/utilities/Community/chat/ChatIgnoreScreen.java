package net.felix.utilities.Community.chat;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.felix.utilities.Overall.keybinding.KeyBindingUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Liste der ignorierten Spieler für das lokale Konto. Dieselbe Liste wie das Rechtsklick-Menü.
 * Vorschläge zeichnen nur Text und lassen das Suchfeld stehen. Die Listen scrollen, das Feld nicht.
 */
public class ChatIgnoreScreen extends Screen {
    private final Runnable listener = this::refreshButtons;
    private final AtomicBoolean pending = new AtomicBoolean(false);
    private final ChatIgnoreTyping typing = new ChatIgnoreTyping();
    private final ChatIgnoreLayout layout = new ChatIgnoreLayout();
    private final List<Button> removeButtons = new ArrayList<>();
    private EditBox nameBox;
    private boolean refreshing;
    private boolean listening;
    private boolean applying;
    private String draft = "";
    private String status = "";
    private int mouseX;
    private int mouseY;
    private List<ChatIgnoreRules.Match> suggestions = List.of();

    public ChatIgnoreScreen() {
        super(Component.literal("Ignorierte Spieler"));
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> client.gui.setScreen(new ChatIgnoreScreen()));
        }
    }

    @Override
    protected void init() {
        super.init();
        if (!listening) {
            ChatIgnoreList.shared().addListener(listener);
            listening = true;
        }
        removeButtons.clear();
        ensureField();
        layoutButtons();
    }

    private void ensureField() {
        if (nameBox != null) {
            return;
        }
        nameBox = new EditBox(this.font, fieldX(), layout.fieldY(), ChatIgnoreLayout.NAME_WIDTH, ChatIgnoreLayout.FIELD_HEIGHT, Component.literal("Spielername"));
        nameBox.setHint(Component.literal("Spielername"));
        nameBox.setMaxLength(32);
        nameBox.setResponder(this::onType);
    }

    private void layoutButtons() {
        int x = fieldX();
        int y = layout.fieldY();
        nameBox.setX(x);
        nameBox.setY(y);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.literal("Hinzufügen"), button -> addName())
            .bounds(x + ChatIgnoreLayout.NAME_WIDTH + ChatIgnoreLayout.BUTTON_GAP, y, ChatIgnoreLayout.BUTTON_WIDTH, 20)
            .build());
        addRenderableWidget(Button.builder(Component.literal("Schließen"), button -> onClose())
            .bounds(this.width / 2 - 50, layout.closeY(this.height), 100, 20)
            .build());
        syncRemoveButtons(currentNames());
    }

    private int fieldX() {
        return this.width / 2 - 140;
    }

    private void onType(String value) {
        if (applying || nameBox == null) {
            return;
        }
        draft = value == null ? "" : value;
        typing.replaceText(draft, nameBox.getCursorPosition(), nameBox.getCursorPosition());
        if (draft.trim().length() < 2) {
            ChatPlayerSearch.cancel();
            suggestions = List.of();
            status = "";
            layout.resetSuggestionScroll();
            syncRemoveButtons(currentNames());
            return;
        }
        status = "Suche …";
        suggestions = List.of();
        layout.resetSuggestionScroll();
        syncRemoveButtons(currentNames());
        long generation = typing.generation();
        ChatPlayerSearch.search(draft, result -> showSearch(generation, result));
    }

    private void showSearch(long generation, ChatPlayerSearch.Result result) {
        if (nameBox == null || result == null) {
            return;
        }
        String current = nameBox.getValue() == null ? "" : nameBox.getValue();
        if (!ChatPlayerSearch.isCurrent(generation, result.query(), typing.generation(), current)) {
            return;
        }
        List<ChatIgnoreRules.Match> shown = ChatIgnoreRules.suggestions(
            result.matches(),
            ChatIgnoreList.localPlayerName(),
            result.selfId());
        if (!typing.offer(generation, result.query(), shown, result.error())) {
            return;
        }
        suggestions = typing.suggestions();
        status = typing.status();
        layout.resetSuggestionScroll();
        syncRemoveButtons(currentNames());
    }

    private void addName() {
        String account = ChatIgnoreList.localAccountId();
        if (account == null || nameBox == null || !pending.compareAndSet(false, true)) {
            return;
        }
        String name = nameBox.getValue() == null ? "" : nameBox.getValue().trim();
        if (name.isEmpty()) {
            pending.set(false);
            return;
        }
        status = "Suche …";
        ChatPlayerSearch.search(name, result -> finishAdd(account, name, result));
    }

    private void finishAdd(String account, String name, ChatPlayerSearch.Result result) {
        pending.set(false);
        if (nameBox == null || result == null) {
            return;
        }
        if (result.error() != null) {
            status = result.error();
            return;
        }
        typing.replaceText(name, name.length(), name.length());
        String problem = typing.submit(result.matches(), ChatIgnoreList.localPlayerName(), result.selfId());
        if (problem != null) {
            status = problem;
            return;
        }
        ChatIgnoreRules.Match match = ChatIgnoreRules.confirmed(
            name, result.matches(), ChatIgnoreList.localPlayerName(), result.selfId());
        draft = "";
        suggestions = List.of();
        status = "";
        layout.resetSuggestionScroll();
        applying = true;
        nameBox.setValue("");
        applying = false;
        ChatIgnoreList.shared().ignore(account, match.name(), match.playerId());
    }

    private void pick(ChatIgnoreRules.Match match) {
        if (nameBox == null || match == null || match.name() == null) {
            return;
        }
        typing.pick(match);
        applying = true;
        nameBox.setValue(match.name());
        nameBox.setCursorPosition(match.name().length());
        nameBox.setHighlightPos(nameBox.getCursorPosition());
        applying = false;
        nameBox.setFocused(true);
        draft = match.name();
        long generation = typing.generation();
        ChatPlayerSearch.search(match.name(), result -> showSearch(generation, result));
    }

    private List<String> currentNames() {
        String account = ChatIgnoreList.localAccountId();
        return account == null ? List.of() : ChatIgnoreList.shared().names(account);
    }

    private void syncRemoveButtons(List<String> names) {
        layout.clamp(suggestions.size(), names.size(), this.height);
        int slots = Math.min(layout.visibleListRows(this.height), Math.max(0, names.size() - layout.listScroll()));
        while (removeButtons.size() > slots) {
            removeWidget(removeButtons.remove(removeButtons.size() - 1));
        }
        int x = fieldX() + ChatIgnoreLayout.NAME_WIDTH + ChatIgnoreLayout.BUTTON_GAP;
        while (removeButtons.size() < slots) {
            int slot = removeButtons.size();
            Button button = Button.builder(Component.literal("Entfernen"), pressed -> {
                int index = layout.listScroll() + slot;
                List<String> current = currentNames();
                if (index >= 0 && index < current.size()) {
                    removeName(current.get(index));
                }
            }).bounds(x, layout.listRowY(slot), ChatIgnoreLayout.BUTTON_WIDTH, 18).build();
            removeButtons.add(button);
            addRenderableWidget(button);
        }
        for (int slot = 0; slot < removeButtons.size(); slot++) {
            Button button = removeButtons.get(slot);
            button.setX(x);
            button.setY(layout.listRowY(slot));
            boolean covered = layout.listSlotCovered(slot, suggestions.size(), this.height);
            button.visible = !covered;
            button.active = !covered;
        }
    }

    private void removeName(String name) {
        String account = ChatIgnoreList.localAccountId();
        if (account == null) {
            return;
        }
        ChatIgnoreList.shared().unignore(account, name);
    }

    private void refreshButtons() {
        if (refreshing) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui.screen() != this) {
            return;
        }
        refreshing = true;
        boolean focused = nameBox != null && nameBox.isFocused();
        client.execute(() -> {
            try {
                if (client.gui.screen() == this) {
                    clearWidgets();
                    removeButtons.clear();
                    ensureField();
                    if (nameBox.getValue() == null || !nameBox.getValue().equals(draft)) {
                        applying = true;
                        nameBox.setValue(draft);
                        applying = false;
                    }
                    layoutButtons();
                    if (focused) {
                        nameBox.setFocused(true);
                    }
                }
            } finally {
                refreshing = false;
            }
        });
    }

    private int suggestionAt(double pointerX, double pointerY) {
        return layout.suggestionIndex(pointerX, pointerY, this.width, suggestions.size(), this.height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (KeyBindingUtility.legacyMouseButton(event.button()) == 0) {
            int row = suggestionAt(event.x(), event.y());
            if (row >= 0) {
                pick(suggestions.get(row));
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (nameBox != null && nameBox.isFocused()
            && (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)) {
            addName();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int listBefore = layout.listScroll();
        if (!layout.scroll(mouseX, mouseY, vertical, this.width, this.height, suggestions.size(), currentNames().size())) {
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }
        if (layout.listScroll() != listBefore) {
            syncRemoveButtons(currentNames());
        }
        return true;
    }

    @Override
    public void onClose() {
        ChatIgnoreList.shared().removeListener(listener);
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        context.text(this.font, "Ignorierte Spieler", fieldX(), 16, 0xFFFFFFFF, false);
        super.extractRenderState(context, mouseX, mouseY, delta);
        List<String> names = currentNames();
        int x = fieldX();
        if (names.isEmpty()) {
            context.text(this.font, "Keine Spieler ignoriert.", x, layout.listTop(), 0xFFAAAAAA, false);
        } else {
            int start = layout.listScroll();
            int rows = Math.min(layout.visibleListRows(this.height), names.size() - start);
            context.enableScissor(x, layout.listTop(), x + ChatIgnoreLayout.NAME_WIDTH, layout.listBottom(this.height));
            for (int slot = 0; slot < rows; slot++) {
                context.text(this.font, names.get(start + slot), x, layout.listRowY(slot) + 4, 0xFFFFFFFF, false);
            }
            context.disableScissor();
        }
        drawDropdown(context);
    }

    private void drawDropdown(GuiGraphicsExtractor context) {
        int x = fieldX();
        int y = ChatIgnoreLayout.DROPDOWN_TOP;
        if (!suggestions.isEmpty()) {
            int rows = layout.dropdownVisible(suggestions.size(), this.height);
            int height = rows * ChatIgnoreLayout.DROPDOWN_ROW;
            context.fill(x, y, x + ChatIgnoreLayout.ROW_WIDTH, y + height, 0xF0101410);
            context.outline(x, y, ChatIgnoreLayout.ROW_WIDTH, height, 0xFF3C4440);
            String typed = nameBox == null || nameBox.getValue() == null ? "" : nameBox.getValue().trim();
            int hover = suggestionAt(mouseX, mouseY);
            int start = layout.suggestionScroll();
            for (int slot = 0; slot < rows; slot++) {
                ChatIgnoreRules.Match match = suggestions.get(start + slot);
                boolean over = start + slot == hover;
                boolean chosen = match.name().equalsIgnoreCase(typed);
                if (over || chosen) {
                    context.fill(x + 1, y + slot * ChatIgnoreLayout.DROPDOWN_ROW, x + ChatIgnoreLayout.ROW_WIDTH - 1,
                        y + (slot + 1) * ChatIgnoreLayout.DROPDOWN_ROW, over ? 0x33FFFFFF : 0x22FFFFFF);
                }
                context.text(this.font, match.name(), x + 4, y + slot * ChatIgnoreLayout.DROPDOWN_ROW + 2, 0xFFE8E8E8, false);
            }
            return;
        }
        if (status != null && !status.isEmpty()) {
            context.text(this.font, status, x, y + 2, 0xFFD0C080, false);
        }
    }
}

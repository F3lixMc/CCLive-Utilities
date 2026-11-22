package net.felix.utilities.Overall.eventsound.ui;

import java.io.IOException;
import java.util.List;
import net.felix.utilities.Overall.eventsound.data.EventSoundKind;
import net.felix.utilities.Overall.eventsound.sound.EventSoundLibrary;
import net.felix.utilities.Overall.eventsound.data.EventSoundMatchMode;
import net.felix.utilities.Overall.eventsound.sound.EventSoundPlayer;
import net.felix.utilities.Overall.eventsound.data.EventSoundRule;
import net.felix.utilities.Overall.eventsound.config.EventSoundStore;
import net.felix.utilities.Other.keybinding.KeyBindingUtility;
import net.felix.utilities.Overall.itemviewer.data.ItemCatalog;
import net.felix.utilities.Overall.itemviewer.data.ItemData;
import net.felix.utilities.Overall.itemviewer.tooltip.TooltipColors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Sounddatei und Lautstärke für ein Event.
 */
public final class EventSoundEditScreen extends Screen {
    private static final int ROW_HEIGHT = 16;
    private static final int HEADER = 32;
    private static final int FOOTER = 36;
    private static final String CHOOSE_SOUND = "Wähle einen Sound";
    private static final String NEED_MESSAGE = "Bitte eine Nachricht eingeben";
    private static final int PATTERN_ERROR_Y = 22;
    private static final int CHECK_PLAYER_Y = 42;
    private static final int CHECK_SERVER_Y = 62;
    private static final int CHECK_EXACT_Y = 90;
    private static final int CHECK_CONTAINS_Y = 110;
    private static final int SOUND_BUTTONS_Y = 138;
    private static final int SOUND_LABEL_Y = 162;
    private static final int MAX_TIME_Y = 184;
    private static final int SLIDER_OFFSET_Y = 224;
    private static final int STOP_OFFSET_Y = 250;
    private static final String MAX_TIME_LABEL = "Max. Spielzeit:";
    private static final String[] INFO_LINES = {
            "#Text",
            "Nur der Nachrichtentext wird verglichen.",
            "Farbcodes werden ignoriert.",
            "Groß- und Kleinschreibung muss stimmen.",
            "#Herkunft",
            "Spieler: Nur Nachrichten die vom Spieler kommen",
            "Server: Nur Nachrichten die vom Server kommen",
            "Wenn beide an, aus beiden Quellen",
            "Wenn beide aus, kommt kein Sound.",
            "#Vergleich",
            "Enthält: die Eingabe kommt in der Chat Nachricht vor.",
            "Entspricht: die Eingabe muss mit der Nachricht übereinstimmen.",
            "#Platzhalter",
            "Benutze <name> um einen Platzhalter zu setzen.", 
            "<name> entspricht einem Wort.",
            "Beispiel: Du hast <name>fragment gefunden.",
            "Würde bei: 'Du hast Erz-Talismanfragment gefunden' auslösen."
    };

    private final Screen listScreen;
    private final EventSoundRule draft;
    private Button auswahlButton;
    private Button browseButton;
    private Button playButton;
    private Button stopButtonWidget;
    private Button saveButton;
    private Button cancelButton;
    private Button folderButton;
    private EditBox patternBox;
    private EditBox durationBox;
    private String durationInput;
    private boolean dropdownOpen;
    private int dropdownScroll;
    private boolean draggingDropdown;
    private boolean draggingVolume;
    private int browseToken;
    private int scroll;
    private boolean infoHovered;
    private int infoX;
    private int infoY;
    private String status = "";
    private boolean chooseSound;
    private int sliderX;
    private int sliderY;
    private int sliderWidth;

    public EventSoundEditScreen(Screen listScreen, EventSoundRule draft) {
        super(Component.literal("Sound"));
        this.listScreen = listScreen;
        this.draft = draft;
        this.durationInput = EventSoundRule.formatMaxSeconds(draft.maxSeconds);
    }

    @Override
    protected void init() {
        if (patternBox != null && draft.kind == EventSoundKind.CUSTOM) {
            draft.pattern = patternBox.getValue() == null ? "" : patternBox.getValue();
        }
        if (durationBox != null && durationBox.getValue() != null) {
            durationInput = durationBox.getValue();
        }
        int x = boxX() + 12;
        int y = boxY() + 32;
        auswahlButton = EventSoundUi.button("Auswahl", button -> dropdownOpen = !dropdownOpen, x, y, 70, 20, "Gespeicherten Sound auswählen");
        browseButton = EventSoundUi.button("Durchsuchen", button -> browse(), x + 76, y, 90, 20, "Sounddatei vom Computer wählen");
        playButton = EventSoundUi.button("♪", button -> playDraft(), x + 172, y, 20, 20, "Sound abspielen");
        stopButtonWidget = EventSoundUi.stopButton(x, y);
        saveButton = EventSoundUi.button("Speichern", button -> save(), x, y, 90, 20);
        cancelButton = EventSoundUi.button("Abbrechen", button -> onClose(), x, y, 90, 20);
        folderButton = EventSoundUi.button("", button -> openFolder(), x, y, 20, 20, "Sound-Ordner öffnen");
        folderButton.visible = false;
        addRenderableWidget(auswahlButton);
        addRenderableWidget(browseButton);
        addRenderableWidget(playButton);
        addRenderableWidget(stopButtonWidget);
        addRenderableWidget(saveButton);
        addRenderableWidget(cancelButton);
        addRenderableWidget(folderButton);
        durationBox = new EditBox(this.font, x, y, 72, 20, Component.literal(MAX_TIME_LABEL));
        durationBox.setMaxLength(16);
        durationBox.setHint(Component.literal("Sekunden"));
        durationBox.setValue(durationInput == null ? "" : durationInput);
        addRenderableWidget(durationBox);
        if (draft.kind == EventSoundKind.CUSTOM) {
            patternBox = new EditBox(this.font, x, y, 80, 20, Component.literal("Nachricht"));
            patternBox.setMaxLength(256);
            patternBox.setHint(Component.literal("Nachricht"));
            patternBox.setValue(draft.pattern == null ? "" : draft.pattern);
            addRenderableWidget(patternBox);
            placeCustomWidgets();
            return;
        }
        placeStandardWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (draft.kind == EventSoundKind.CUSTOM) {
            renderCustom(context, mouseX, mouseY, delta);
            return;
        }
        placeStandardWidgets();
        placeFolderButton();
        EventSoundUi.dim(context, this.width, this.height);
        int boxX = boxX();
        int boxY = boxY();
        EventSoundUi.panel(context, boxX, boxY, boxWidth(), boxHeight());
        int titleRoom = draft.kind == EventSoundKind.BLUEPRINT ? boxWidth() - 24 : boxWidth() - 48;
        context.centeredText(this.font, Component.literal(EventSoundUi.trim(this.font, draft.listLabel(), titleRoom)), this.width / 2, boxY + 10, 0xFFFFFFFF);
        EventSoundUi.text(context, this.font, EventSoundUi.trim(this.font, soundLabel(), Math.max(20, boxWidth() - 24)), boxX + 12, boxY + 56, soundLabelColor());
        drawMaxTimeLabel(context, sliderX, durationBox == null ? boxY + 74 : durationBox.getY());
        EventSoundUi.text(context, this.font, "Lautstärke: " + Math.round(draft.volume * 100f) + "%", sliderX, sliderY - 14, 0xFFFFFFFF);
        context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + 8, 0xFF2A3542);
        int fill = (int) (sliderWidth * draft.volume);
        context.fill(sliderX, sliderY, sliderX + fill, sliderY + 8, 0xFFFFAA00);
        context.fill(sliderX + Math.max(0, fill - 3), sliderY - 3, sliderX + fill + 3, sliderY + 11, 0xFFFFFFFF);
        if (draft.kind == EventSoundKind.BLUEPRINT) {
            int messageY = sliderY + 20;
            int gearX = gearX();
            int gearY = messageY + 3;
            EventSoundUi.drawBlueprintMessage(context, this.font, sliderX, messageY, gearX - 8 - sliderX, draft.target, blueprintColor());
            drawGear(context, gearX, gearY, mouseX, mouseY);
            if (EventSoundUi.inside(mouseX, mouseY, gearX, gearY, 16, 16)) {
                EventSoundUi.hoverTip(context, this.font, "Bauplan ändern", mouseX, mouseY);
            }
        } else {
            int gearX = gearX();
            int gearY = gearY();
            drawGear(context, gearX, gearY, mouseX, mouseY);
            if (EventSoundUi.inside(mouseX, mouseY, gearX, gearY, 16, 16)) {
                EventSoundUi.hoverTip(context, this.font, gearTip(), mouseX, mouseY);
            }
        }
        if (!status.isEmpty() && stopButtonWidget != null) {
            int statusX = stopButtonWidget.getX() + stopButtonWidget.getWidth() + 6;
            EventSoundUi.text(context, this.font, EventSoundUi.trim(this.font, status, Math.max(20, boxX() + boxWidth() - statusX - 8)), statusX, stopButtonWidget.getY() + 6, 0xFFFF5555);
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
        if (dropdownOpen && auswahlButton != null) {
            drawDropdown(context, mouseX, mouseY);
            drawOpenFolderButton(context, mouseX, mouseY, delta);
        }
    }

    private void placeStandardWidgets() {
        int x = boxX() + 12;
        int top = boxY() + 32;
        int inner = Math.max(20, boxWidth() - 24);
        int playW = Math.min(20, Math.max(1, inner / 5));
        int gap = inner > 60 ? 4 : 2;
        int rest = Math.max(0, inner - playW - gap * 2);
        int auswahlW = Math.min(70, Math.max(1, rest / 2));
        int browseW = Math.min(90, Math.max(1, rest - auswahlW));
        move(auswahlButton, x, top, auswahlW, 20, true);
        move(browseButton, x + auswahlW + gap, top, browseW, 20, true);
        move(playButton, x + auswahlW + gap + browseW + gap, top, playW, 20, true);
        int saveY = boxY() + boxHeight() - 28;
        int saveW = Math.min(90, Math.max(1, (inner - 8) / 2));
        int cancelW = Math.min(90, Math.max(1, inner - saveW - 8));
        move(saveButton, x, saveY, saveW, 20, true);
        move(cancelButton, x + saveW + 8, saveY, cancelW, 20, true);
        int stopY = saveY - 24;
        move(stopButtonWidget, x, stopY, Math.min(56, inner), 20, true);
        sliderX = x;
        sliderWidth = inner;
        sliderY = stopY - (draft.kind == EventSoundKind.BLUEPRINT ? 78 : 50);
        placeDurationBox(x, sliderY - 40, inner, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (draft.kind == EventSoundKind.CUSTOM) {
            placeCustomWidgets();
        }
        if (KeyBindingUtility.legacyMouseButton(event.button()) != 0) {
            return super.mouseClicked(event, doubled);
        }
        double mouseX = event.x();
        double mouseY = event.y();
        if (folderButton != null && folderButton.visible && folderButton.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(event, doubled);
        }
        if (dropdownOpen && overDropdown(mouseX, mouseY)) {
            clickDropdown(mouseX, mouseY);
            return true;
        }
        if (draft.kind == EventSoundKind.CUSTOM && inViewport(mouseX, mouseY)) {
            int x = boxX() + 12;
            if (hitCheckbox(mouseX, mouseY, x, contentY(CHECK_PLAYER_Y))) {
                draft.fromPlayer = !draft.fromPlayer;
                return true;
            }
            if (hitCheckbox(mouseX, mouseY, x, contentY(CHECK_SERVER_Y))) {
                draft.fromServer = !draft.fromServer;
                return true;
            }
            if (hitCheckbox(mouseX, mouseY, x, contentY(CHECK_EXACT_Y))) {
                draft.matchMode = EventSoundMatchMode.EXACT;
                return true;
            }
            if (hitCheckbox(mouseX, mouseY, x, contentY(CHECK_CONTAINS_Y))) {
                draft.matchMode = EventSoundMatchMode.CONTAINS;
                return true;
            }
        }
        if (draft.kind != EventSoundKind.CUSTOM && EventSoundUi.inside(mouseX, mouseY, gearX(), gearY(), 16, 16)) {
            openTargetSettings();
            return true;
        }
        if (inViewport(mouseX, mouseY) && EventSoundUi.inside(mouseX, mouseY, sliderX, sliderY - 4, sliderWidth, 16)) {
            draggingVolume = true;
            setVolume(mouseX);
            dropdownOpen = false;
            return true;
        }
        boolean onAuswahl = auswahlButton != null && auswahlButton.isMouseOver(mouseX, mouseY);
        boolean onFolder = folderButton != null && folderButton.visible && folderButton.isMouseOver(mouseX, mouseY);
        if (!onAuswahl && !onFolder) {
            dropdownOpen = false;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draft.kind == EventSoundKind.CUSTOM) {
            placeCustomWidgets();
        }
        if (draggingDropdown) {
            scrollDropdown(event.y());
            return true;
        }
        if (draggingVolume) {
            setVolume(event.x());
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingVolume = false;
        draggingDropdown = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (dropdownOpen && overDropdown(mouseX, mouseY)) {
            dropdownScroll -= (int) Math.signum(vertical);
            return true;
        }
        if (draft.kind == EventSoundKind.CUSTOM && EventSoundUi.inside(mouseX, mouseY, boxX(), boxY(), boxWidth(), boxHeight())) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (vertical * 16)));
            placeCustomWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void onClose() {
        browseToken++;
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(listScreen);
        }
    }

    private void save() {
        if (draft.kind == EventSoundKind.CUSTOM) {
            draft.pattern = patternBox == null || patternBox.getValue() == null ? "" : patternBox.getValue().trim();
            if (draft.pattern.isEmpty()) {
                chooseSound = false;
                status = NEED_MESSAGE;
                return;
            }
        }
        if (draft.soundFile == null || draft.soundFile.isBlank()) {
            status = "";
            chooseSound = true;
            return;
        }
        Float maxSeconds = EventSoundRule.parseMaxSeconds(durationBox == null ? "" : durationBox.getValue());
        if (maxSeconds == null) {
            status = "Ungültige Spielzeit";
            return;
        }
        draft.maxSeconds = maxSeconds;
        chooseSound = false;
        EventSoundStore.save(draft);
        onClose();
    }

    private void playDraft() {
        Float maxSeconds = EventSoundRule.parseMaxSeconds(durationBox == null ? "" : durationBox.getValue());
        EventSoundPlayer.playFile(draft.soundFile, draft.volume, maxSeconds == null ? 0f : maxSeconds);
    }

    private void browse() {
        int token = ++browseToken;
        dropdownOpen = false;
        status = "";
        chooseSound = false;
        EventSoundLibrary.pickFile(path -> {
            if (token != browseToken || this.minecraft == null || this.minecraft.gui.screen() != this) {
                return;
            }
            try {
                draft.soundFile = EventSoundLibrary.importFile(path);
                status = "";
                chooseSound = false;
            } catch (IOException exception) {
                status = exception.getMessage() == null ? "Datei konnte nicht gespeichert werden" : exception.getMessage();
            }
        }, message -> {
            if (token == browseToken) {
                status = message;
            }
        });
    }

    private void drawDropdown(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        List<String> names = EventSoundLibrary.fileNames();
        int x = auswahlButton.getX();
        int y = auswahlButton.getY() + 22;
        int width = dropdownWidth();
        int visible = dropdownVisibleCount(names.size());
        int height = dropdownHeader() + Math.max(ROW_HEIGHT, visible * ROW_HEIGHT);
        EventSoundUi.panel(context, x, y, width, height + 4);
        if (names.isEmpty()) {
            EventSoundUi.text(context, this.font, "Keine Sounds gespeichert", x + 4, y + dropdownHeader() + 4, 0xFFAAAAAA);
            return;
        }
        boolean overflow = names.size() > visible;
        int maxScroll = Math.max(0, names.size() - visible);
        dropdownScroll = Math.max(0, Math.min(dropdownScroll, maxScroll));
        int filesY = y + dropdownHeader();
        int rowWidth = overflow ? width - 8 : width;
        for (int index = 0; index < visible; index++) {
            String name = names.get(index + dropdownScroll);
            int rowY = filesY + index * ROW_HEIGHT;
            boolean hovered = EventSoundUi.inside(mouseX, mouseY, x, rowY, rowWidth, ROW_HEIGHT);
            if (hovered || name.equals(draft.soundFile)) {
                context.fill(x + 2, rowY, x + rowWidth - 2, rowY + ROW_HEIGHT, hovered ? 0xFF404040 : 0xFF303030);
            }
            EventSoundUi.text(context, this.font, EventSoundUi.trim(this.font, name, rowWidth - 8), x + 4, rowY + 4, 0xFFFFFFFF);
        }
        if (overflow) {
            EventSoundUi.scrollbar(context, x + width - 7, filesY, visible * ROW_HEIGHT, names.size() * ROW_HEIGHT, dropdownScroll * ROW_HEIGHT);
        }
    }

    private boolean clickDropdown(double mouseX, double mouseY) {
        if (!overDropdown(mouseX, mouseY)) {
            dropdownOpen = false;
            return false;
        }
        List<String> names = EventSoundLibrary.fileNames();
        if (names.isEmpty()) {
            return true;
        }
        int y = auswahlButton.getY() + 22 + dropdownHeader();
        if (mouseY < y) {
            return true;
        }
        int visible = dropdownVisibleCount(names.size());
        if (names.size() > visible && mouseX >= auswahlButton.getX() + dropdownWidth() - 8) {
            draggingDropdown = true;
            scrollDropdown(mouseY);
            return true;
        }
        int index = (int) ((mouseY - y) / ROW_HEIGHT) + dropdownScroll;
        if (index >= 0 && index < names.size()) {
            draft.soundFile = names.get(index);
            dropdownOpen = false;
            status = "";
            chooseSound = false;
        }
        return true;
    }

    private boolean overDropdown(double mouseX, double mouseY) {
        if (auswahlButton == null) {
            return false;
        }
        List<String> names = EventSoundLibrary.fileNames();
        int visible = dropdownVisibleCount(names.size());
        int height = dropdownHeader() + visible * ROW_HEIGHT + 4;
        return EventSoundUi.inside(mouseX, mouseY, auswahlButton.getX(), auswahlButton.getY() + 22, dropdownWidth(), height);
    }

    private void setVolume(double mouseX) {
        float next = (float) ((mouseX - sliderX) / sliderWidth);
        draft.volume = Math.max(0f, Math.min(1f, next));
        EventSoundPlayer.updatePlayingVolume(draft.soundFile, draft.volume);
    }

    private void openTargetSettings() {
        if (this.minecraft == null || draft.kind == null) {
            return;
        }
        if (draft.kind == EventSoundKind.SMITHING) {
            this.minecraft.gui.setScreen(new SmithingStateScreen(listScreen, this, draft));
        } else if (draft.kind == EventSoundKind.BLUEPRINT) {
            this.minecraft.gui.setScreen(new BlueprintPickScreen(listScreen, this, draft));
        }
    }

    private int boxWidth() {
        return EventSoundUi.fit(this.width, 380, 8);
    }

    private int boxX() {
        return Math.max(4, (this.width - boxWidth()) / 2);
    }

    private int boxHeight() {
        int preferred = draft.kind == EventSoundKind.CUSTOM
                ? HEADER + customContentHeight() + FOOTER
                : draft.kind == EventSoundKind.BLUEPRINT ? 264 : 228;
        return EventSoundUi.fit(this.height, preferred, 8);
    }

    private int gearX() {
        return boxX() + boxWidth() - 28;
    }

    private String gearTip() {
        if (draft.kind == EventSoundKind.SMITHING) {
            return "Schmiedezustand ändern";
        }
        if (draft.kind == EventSoundKind.CUSTOM) {
            return "Nachricht ändern";
        }
        return "Einstellungen öffnen";
    }

    private int gearY() {
        if (draft.kind == EventSoundKind.BLUEPRINT) {
            return sliderY + 23;
        }
        return boxY() + 8;
    }

    private void drawGear(GuiGraphicsExtractor context, int x, int y, int mouseX, int mouseY) {
        if (EventSoundUi.inside(mouseX, mouseY, x, y, 16, 16)) {
            context.fill(x - 1, y - 1, x + 17, y + 17, 0xFF3A4A5C);
        }
        EventSoundUi.drawGear(context, x, y, 16);
    }

    private void renderCustom(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        placeCustomWidgets();
        EventSoundUi.dim(context, this.width, this.height);
        int boxX = boxX();
        int boxY = boxY();
        EventSoundUi.panel(context, boxX, boxY, boxWidth(), boxHeight());
        context.centeredText(this.font, Component.literal("Custom"), this.width / 2, boxY + 10, 0xFFFFFFFF);
        int x = boxX + 12;
        context.enableScissor(boxX + 1, viewportY(), boxX + boxWidth() - 1, viewportY() + viewportHeight());
        if (NEED_MESSAGE.equals(status)) {
            EventSoundUi.text(context, this.font, EventSoundUi.trim(this.font, status, Math.max(20, boxWidth() - 24)), x, contentY(PATTERN_ERROR_Y), 0xFFFF5555);
        }
        drawCheckbox(context, x, contentY(CHECK_PLAYER_Y), draft.fromPlayer, "Gesendet von Spieler", mouseX, mouseY);
        drawCheckbox(context, x, contentY(CHECK_SERVER_Y), draft.fromServer, "Gesendet von Server", mouseX, mouseY);
        drawCheckbox(context, x, contentY(CHECK_EXACT_Y), draft.matchMode == EventSoundMatchMode.EXACT, "Entspricht genau der Eingabe", mouseX, mouseY);
        drawCheckbox(context, x, contentY(CHECK_CONTAINS_Y), draft.matchMode == EventSoundMatchMode.CONTAINS, "Enthält die Eingabe", mouseX, mouseY);
        EventSoundUi.text(context, this.font, EventSoundUi.trim(this.font, soundLabel(), Math.max(20, boxWidth() - 24)), x, contentY(SOUND_LABEL_Y), soundLabelColor());
        if (overlapsViewport(contentY(MAX_TIME_Y), 20)) {
            drawMaxTimeLabel(context, x, contentY(MAX_TIME_Y));
        }
        EventSoundUi.text(context, this.font, "Lautstärke: " + Math.round(draft.volume * 100f) + "%", sliderX, sliderY - 14, 0xFFFFFFFF);
        context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + 8, 0xFF2A3542);
        int fill = (int) (sliderWidth * draft.volume);
        context.fill(sliderX, sliderY, sliderX + fill, sliderY + 8, 0xFFFFAA00);
        context.fill(sliderX + Math.max(0, fill - 3), sliderY - 3, sliderX + fill + 3, sliderY + 11, 0xFFFFFFFF);
        infoHovered = inViewport(mouseX, mouseY) && EventSoundUi.inside(mouseX, mouseY, infoX, infoY, 20, 20);
        drawInfoMark(context);
        context.disableScissor();
        if (!status.isEmpty() && !NEED_MESSAGE.equals(status)) {
            EventSoundUi.text(context, this.font, EventSoundUi.trim(this.font, status, Math.max(20, boxWidth() - 24)), boxX() + 12, boxY() + boxHeight() - 46, 0xFFFF5555);
        }
        drawScrollbar(context);
        placeFolderButton();
        super.extractRenderState(context, mouseX, mouseY, delta);
        if (infoHovered) {
            drawInfo(context, mouseX, mouseY);
        }
        if (dropdownOpen && auswahlButton != null) {
            drawDropdown(context, mouseX, mouseY);
            drawOpenFolderButton(context, mouseX, mouseY, delta);
        }
    }

    private void placeCustomWidgets() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        int x = boxX() + 12;
        int inner = Math.max(20, boxWidth() - 24);
        int fieldWidth = Math.max(16, inner - 28);
        sliderX = x;
        sliderY = contentY(SLIDER_OFFSET_Y);
        sliderWidth = inner;
        if (patternBox != null) {
            patternBox.setX(x);
            patternBox.setY(contentY(0));
            patternBox.setWidth(fieldWidth);
            patternBox.visible = overlapsViewport(patternBox.getY(), patternBox.getHeight());
        }
        infoX = Math.min(x + fieldWidth + 6, boxX() + boxWidth() - 26);
        infoY = patternBox != null ? patternBox.getY() : contentY(0);
        int playW = Math.min(20, Math.max(1, inner / 5));
        int gap = inner > 60 ? 4 : 2;
        int rest = Math.max(0, inner - playW - gap * 2);
        int auswahlW = Math.min(70, Math.max(1, rest / 2));
        int browseW = Math.min(90, Math.max(1, rest - auswahlW));
        move(auswahlButton, x, contentY(SOUND_BUTTONS_Y), auswahlW, 20, overlapsViewport(contentY(SOUND_BUTTONS_Y), 20));
        move(browseButton, x + auswahlW + gap, contentY(SOUND_BUTTONS_Y), browseW, 20, overlapsViewport(contentY(SOUND_BUTTONS_Y), 20));
        move(playButton, x + auswahlW + gap + browseW + gap, contentY(SOUND_BUTTONS_Y), playW, 20, overlapsViewport(contentY(SOUND_BUTTONS_Y), 20));
        placeDurationBox(x, contentY(MAX_TIME_Y), inner, overlapsViewport(contentY(MAX_TIME_Y), 20));
        move(stopButtonWidget, x, contentY(STOP_OFFSET_Y), Math.min(56, inner), 20, overlapsViewport(contentY(STOP_OFFSET_Y), 20));
        int saveY = boxY() + boxHeight() - 28;
        int saveW = Math.min(90, Math.max(1, (inner - 8) / 2));
        move(saveButton, x, saveY, saveW, 20, true);
        move(cancelButton, x + saveW + 8, saveY, Math.min(90, Math.max(1, inner - saveW - 8)), 20, true);
    }

    private int contentY(int contentOffset) {
        return viewportY() + contentOffset - scroll;
    }

    private int viewportY() {
        return boxY() + HEADER;
    }

    private int viewportHeight() {
        return Math.max(20, boxHeight() - HEADER - FOOTER);
    }

    private int customContentHeight() {
        return 280;
    }

    private void placeDurationBox(int x, int y, int inner, boolean visible) {
        if (durationBox == null) {
            return;
        }
        int labelWidth = this.font.width(MAX_TIME_LABEL);
        int fieldWidth = Math.max(36, Math.min(80, inner - labelWidth - 8));
        durationBox.setX(x + labelWidth + 6);
        durationBox.setY(y);
        durationBox.setWidth(fieldWidth);
        durationBox.setHeight(20);
        durationBox.visible = visible;
    }

    private void drawMaxTimeLabel(GuiGraphicsExtractor context, int x, int y) {
        EventSoundUi.text(context, this.font, MAX_TIME_LABEL, x, y + 6, 0xFFFFFFFF);
    }

    private void move(Button button, int x, int y, int width, int height, boolean visible) {
        if (button == null) {
            return;
        }
        button.setX(x);
        button.setY(y);
        button.setWidth(Math.max(1, width));
        button.setHeight(height);
        button.visible = visible;
    }

    private void drawOpenFolderButton(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (folderButton == null || !folderButton.visible) {
            return;
        }
        folderButton.extractRenderState(context, mouseX, mouseY, delta);
        int x = folderButton.getX() + 4;
        int y = folderButton.getY() + 5;
        context.fill(x, y + 2, x + 12, y + 11, 0xFF6E5424);
        context.fill(x + 1, y, x + 6, y + 3, 0xFF6E5424);
        context.fill(x + 1, y + 1, x + 5, y + 3, 0xFFD7B15A);
        context.fill(x + 1, y + 3, x + 11, y + 10, 0xFFE8C56A);
        context.fill(x + 1, y + 8, x + 11, y + 10, 0xFFC4963A);
    }

    private void placeFolderButton() {
        if (folderButton == null || auswahlButton == null) {
            return;
        }
        folderButton.visible = dropdownOpen;
        if (!dropdownOpen) {
            return;
        }
        folderButton.setX(auswahlButton.getX() + dropdownWidth() - 22);
        folderButton.setY(auswahlButton.getY() + 23);
        folderButton.setWidth(20);
        folderButton.setHeight(20);
    }

    private int dropdownWidth() {
        if (auswahlButton == null) {
            return 80;
        }
        return Math.min(220, Math.max(24, Math.min(this.width - auswahlButton.getX() - 4, boxWidth() - 16)));
    }

    private int dropdownHeader() {
        return 22;
    }

    private int dropdownVisibleCount(int nameCount) {
        if (nameCount <= 0 || auswahlButton == null) {
            return 1;
        }
        int filesY = auswahlButton.getY() + 22 + dropdownHeader();
        int room = this.height - filesY - 8;
        int fit = Math.max(1, room / ROW_HEIGHT);
        return Math.min(nameCount, fit);
    }

    private void scrollDropdown(double mouseY) {
        if (auswahlButton == null) {
            return;
        }
        List<String> names = EventSoundLibrary.fileNames();
        int visible = dropdownVisibleCount(names.size());
        int filesY = auswahlButton.getY() + 22 + dropdownHeader();
        int pixels = EventSoundUi.scrollFromMouse(mouseY, filesY, visible * ROW_HEIGHT, names.size() * ROW_HEIGHT);
        dropdownScroll = pixels / ROW_HEIGHT;
    }

    private void openFolder() {
        try {
            EventSoundLibrary.openFolder();
        } catch (IOException exception) {
            status = "Ordner konnte nicht geöffnet werden";
        }
    }

    private int maxScroll() {
        return Math.max(0, customContentHeight() - viewportHeight());
    }

    private boolean overlapsViewport(int y, int height) {
        return y + height > viewportY() && y < viewportY() + viewportHeight();
    }

    private boolean inViewport(double mouseX, double mouseY) {
        if (draft.kind != EventSoundKind.CUSTOM) {
            return true;
        }
        return EventSoundUi.inside(mouseX, mouseY, boxX(), viewportY(), boxWidth(), viewportHeight());
    }

    private void drawScrollbar(GuiGraphicsExtractor context) {
        EventSoundUi.scrollbar(context, boxX() + boxWidth() - 7, viewportY(), viewportHeight(), customContentHeight(), scroll);
    }

    private String soundLabel() {
        if (draft.soundFile != null && !draft.soundFile.isBlank()) {
            float seconds = EventSoundPlayer.durationSeconds(draft.soundFile);
            if (seconds > 0f) {
                return draft.soundFile + " (" + EventSoundRule.formatMaxSeconds(seconds) + "s)";
            }
            return draft.soundFile;
        }
        return chooseSound ? CHOOSE_SOUND : "Kein Sound";
    }

    private int soundLabelColor() {
        return CHOOSE_SOUND.equals(soundLabel()) ? 0xFFFF5555 : 0xFFB7C3D0;
    }

    private void drawInfoMark(GuiGraphicsExtractor context) {
        int size = patternBox != null ? patternBox.getHeight() : 20;
        int x = infoX;
        int y = infoY;
        context.fill(x, y, x + size, y + size, infoHovered ? 0xFFFFCC44 : 0xFFFFAA00);
        int markTop = y + (size - 12) / 2;
        int markX = x + size / 2 - 1;
        context.fill(markX, markTop, markX + 2, markTop + 8, 0xFF1A1200);
        context.fill(markX, markTop + 10, markX + 2, markTop + 12, 0xFF1A1200);
    }

    private void drawCheckbox(GuiGraphicsExtractor context, int x, int y, boolean checked, String label, int mouseX, int mouseY) {
        boolean hovered = hitCheckbox(mouseX, mouseY, x, y);
        context.fill(x, y, x + 12, y + 12, hovered ? 0xFF505050 : 0xFF2A2A2A);
        context.fill(x, y, x + 12, y + 1, 0xFF808080);
        context.fill(x, y + 11, x + 12, y + 12, 0xFF808080);
        context.fill(x, y, x + 1, y + 12, 0xFF808080);
        context.fill(x + 11, y, x + 12, y + 12, 0xFF808080);
        if (checked) {
            context.fill(x + 3, y + 3, x + 9, y + 9, 0xFF55FF55);
        }
        EventSoundUi.text(context, this.font, EventSoundUi.trim(this.font, label, boxWidth() - 48), x + 18, y + 2, 0xFFFFFFFF);
    }

    private boolean hitCheckbox(double mouseX, double mouseY, int x, int y) {
        return inViewport(mouseX, mouseY) && EventSoundUi.inside(mouseX, mouseY, x, y, Math.max(40, boxWidth() - 36), 14);
    }

    private void drawInfo(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int width = 0;
        for (String line : INFO_LINES) {
            String shown = line.startsWith("#") ? line.substring(1) : "• " + line;
            width = Math.max(width, this.font.width(shown));
        }
        int boxWidth = Math.min(width + 16, Math.max(80, this.width - 8));
        int boxHeight = INFO_LINES.length * 13 + 10;
        int x = Math.max(4, Math.min(mouseX + 12, this.width - boxWidth - 4));
        int y = Math.max(4, Math.min(mouseY + 12, this.height - boxHeight - 4));
        EventSoundUi.panel(context, x, y, boxWidth, boxHeight);
        for (int index = 0; index < INFO_LINES.length; index++) {
            String line = INFO_LINES[index];
            boolean header = line.startsWith("#");
            String shown = header ? line.substring(1) : "• " + line;
            EventSoundUi.text(context, this.font, EventSoundUi.trim(this.font, shown, boxWidth - 12), x + 6, y + 6 + index * 13, header ? 0xFFFFAA00 : 0xFFFFFFFF);
        }
    }

    private int blueprintColor() {
        ItemData item = ItemCatalog.findItemByName(draft.target);
        String rarity = item == null || item.info == null ? null : item.info.rarity;
        return TooltipColors.getRarityColor(rarity);
    }

    private int boxY() {
        return Math.max(4, (this.height - boxHeight()) / 2);
    }
}

package net.felix.utilities.Overall.eventsound.ui;

import java.time.Duration;
import net.felix.CCLiveUtilities;
import net.felix.utilities.Overall.eventsound.data.EventSoundKind;
import net.felix.utilities.Overall.eventsound.sound.EventSoundPlayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Gemeinsame Fläche für die Event-Sound-Menüs.
 */
final class EventSoundUi {
    private static final Identifier GEAR = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "textures/alert_icons/alert_icons_settings.png");

    private EventSoundUi() {
    }

    static int fit(int available, int preferred, int margin) {
        return Math.min(preferred, Math.max(0, available - margin));
    }

    static Button button(String label, Button.OnPress onPress, int x, int y, int width, int height) {
        return button(label, onPress, x, y, width, height, null);
    }

    static Button button(String label, Button.OnPress onPress, int x, int y, int width, int height, String tooltip) {
        Button button = Button.builder(Component.literal(label), onPress).bounds(x, y, width, height).build();
        button.setOverrideRenderHighlightedSprite(button::isHovered);
        if (tooltip != null && !tooltip.isBlank()) {
            button.setTooltip(Tooltip.create(Component.literal(tooltip)));
            button.setTooltipDelay(Duration.ZERO);
        }
        return button;
    }

    static Button stopButton(int x, int y) {
        return button("Stop", ignored -> EventSoundPlayer.stopAll(), x, y, 56, 20, "Stoppt alle gerade laufenden Event-Sounds");
    }

    static void hoverTip(GuiGraphicsExtractor context, Font font, String text, int mouseX, int mouseY) {
        if (text == null || text.isBlank()) {
            return;
        }
        context.setTooltipForNextFrame(font, Component.literal(text), mouseX, mouseY);
    }

    static int kindColor(EventSoundKind kind) {
        if (kind == EventSoundKind.SMITHING) {
            return 0xFFFFAA00;
        }
        if (kind == EventSoundKind.BLUEPRINT) {
            return 0xFF3DDCFF;
        }
        return 0xFFC080FF;
    }

    static void drawGear(GuiGraphicsExtractor context, int x, int y, int size) {
        try {
            context.blit(RenderPipelines.GUI_TEXTURED, GEAR, x, y, 0.0f, 0.0f, size, size, size, size);
        } catch (RuntimeException ignored) {
            context.fill(x + 2, y + 2, x + size - 2, y + size - 2, 0xFFFFFFFF);
        }
    }

    static void dim(GuiGraphicsExtractor context, int width, int height) {
        context.fill(0, 0, width, height, 0xC0101010);
    }

    static void panel(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0xF0121820);
        context.fill(x, y, x + width, y + 2, 0xFFFFAA00);
        context.fill(x, y + height - 1, x + width, y + height, 0xFF808080);
        context.fill(x, y, x + 1, y + height, 0xFF808080);
        context.fill(x + width - 1, y, x + width, y + height, 0xFF808080);
    }

    static void text(GuiGraphicsExtractor context, Font font, String value, int x, int y, int color) {
        int opaque = (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
        context.text(font, value, x, y, opaque, false);
    }

    static String trim(Font font, String value, int width) {
        if (value == null) {
            return "";
        }
        if (font.width(value) <= width) {
            return value;
        }
        String ellipsis = "...";
        return font.plainSubstrByWidth(value, Math.max(0, width - font.width(ellipsis))) + ellipsis;
    }

    static void scrollbar(GuiGraphicsExtractor context, int x, int y, int height, int content, int scroll) {
        int range = content - height;
        if (range <= 0 || height <= 0) {
            return;
        }
        int thumb = Math.min(height, Math.max(12, height * height / content));
        int thumbY = y + (int) ((long) (height - thumb) * Math.max(0, Math.min(scroll, range)) / range);
        context.fill(x, y, x + 4, y + height, 0xFF1A2330);
        context.fill(x, thumbY, x + 4, thumbY + thumb, 0xFFFFAA00);
    }

    static int scrollFromMouse(double mouseY, int y, int height, int content) {
        int range = content - height;
        if (range <= 0 || height <= 0) {
            return 0;
        }
        int thumb = Math.min(height, Math.max(12, height * height / content));
        int travel = Math.max(1, height - thumb);
        double ratio = (mouseY - y - thumb / 2.0) / travel;
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        return (int) Math.round(ratio * range);
    }

    static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    static void drawBlueprintMessage(GuiGraphicsExtractor context, Font font, int x, int y, int width, String name, int nameColor) {
        context.fill(x, y, x + width, y + 22, 0xFF0E141C);
        context.fill(x, y, x + width, y + 1, 0xFF3DDCFF);
        context.fill(x, y + 21, x + width, y + 22, 0xFF3DDCFF);
        context.fill(x, y, x + 1, y + 22, 0xFF3DDCFF);
        context.fill(x + width - 1, y, x + width, y + 22, 0xFF3DDCFF);
        boolean empty = name == null || name.isBlank();
        String shownName = empty ? "<Name>" : name;
        int color = empty ? 0xFF8A97A6 : nameColor;
        int fixed = font.width("[Legend] Du erhältst  - [Bauplan]");
        String shown = trim(font, shownName, Math.max(8, width - 16 - fixed));
        int textX = x + 8;
        int textY = y + 7;
        textX = piece(context, font, "[", textX, textY, 0xFFFFFFFF);
        textX = piece(context, font, "Legend", textX, textY, 0xFFFFAA00);
        textX = piece(context, font, "] Du erhältst ", textX, textY, 0xFFFFFFFF);
        textX = piece(context, font, shown, textX, textY, color);
        textX = piece(context, font, " - ", textX, textY, 0xFFFFFFFF);
        piece(context, font, "[Bauplan]", textX, textY, 0xFF00AAAA);
    }

    private static int piece(GuiGraphicsExtractor context, Font font, String value, int x, int y, int color) {
        text(context, font, value, x, y, color);
        return x + font.width(value);
    }
}

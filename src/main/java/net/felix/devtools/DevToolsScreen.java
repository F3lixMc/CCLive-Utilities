package net.felix.devtools;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Zentrale DevTools-GUI.
 */
public class DevToolsScreen extends Screen {

    private static final Identifier BG = Identifier.fromNamespaceAndPath(
        "cclive-utilities", "textures/gui/devtools_ui.png");
    private static final Identifier ROW = Identifier.fromNamespaceAndPath(
        "cclive-utilities", "textures/gui/devtools_row_ui.png");

    private static final int TEX_W = 180;
    private static final int TEX_H = 115;
    private static final int ROW_W = 162;
    private static final int ROW_H = 18;
    private static final int ROW_ORIGIN_X = 9;
    private static final int ROW_ORIGIN_Y = 17;
    private static final int SLOT = 18;
    private static final int ICON = 16;

    /** Textfarbe Modus + Beschreibung. */
    private static final int TEXT_COLOR = 0xFF3F3F47;
    private static final int BORDER_ON = 0xFF16A80C;
    private static final int BORDER_OFF = 0xFFC41E1E;

    // Slot indices in row: 0=icon, 1=mode, 2=gap, 3-6=desc (4 slots), 7=gap, 8=hotkey
    private static final int SLOT_ICON = 0;
    private static final int SLOT_MODE = 1;
    private static final int SLOT_HOTKEY = 8;

    private float uiScale = 1f;
    private int leftPos;
    private int topPos;
    private int drawW;
    private int drawH;

    private String hoverToolId;
    private int hoverSlot = -1;

    public DevToolsScreen() {
        super(Component.literal("DevTools"));
    }

    @Override
    protected void init() {
        super.init();
        recomputeLayout();
    }

    private void recomputeLayout() {
        int availW = Math.max(1, this.width - 8);
        int availH = Math.max(1, this.height - 8);
        uiScale = Math.min(1f, Math.min(availW / (float) TEX_W, availH / (float) TEX_H));
        drawW = Math.max(1, Math.round(TEX_W * uiScale));
        drawH = Math.max(1, Math.round(TEX_H * uiScale));
        leftPos = (this.width - drawW) / 2;
        topPos = (this.height - drawH) / 2;
    }

    private int sx(int texX) {
        return leftPos + Math.round(texX * uiScale);
    }

    private int sy(int texY) {
        return topPos + Math.round(texY * uiScale);
    }

    private int ss(int texSize) {
        return Math.max(1, Math.round(texSize * uiScale));
    }

    private int rowY(int index) {
        return ROW_ORIGIN_Y + index * ROW_H;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        recomputeLayout();
        hoverToolId = null;
        hoverSlot = -1;

        context.pose().pushMatrix();
        context.pose().translate(leftPos, topPos);
        context.pose().scale(uiScale, uiScale);

        context.blit(RenderPipelines.GUI_TEXTURED, BG, 0, 0, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H);

        List<DevTool> tools = DevToolsRegistry.all();
        for (int i = 0; i < tools.size(); i++) {
            renderRow(context, tools.get(i), i, mouseX, mouseY);
        }

        context.pose().popMatrix();

        // Tooltips in Screen-Koordinaten
        if (hoverToolId != null && hoverSlot >= 0) {
            renderHoverTooltip(context, mouseX, mouseY);
        }

        if (DevToolsHotkeyDispatcher.isCapturingHotkey()) {
            context.centeredText(this.font, Component.literal("Taste drücken… (Entf=löschen, ESC=abbrechen)"),
                this.width / 2, topPos + drawH + 6, 0xFFFFFF);
        }
    }

    private void renderRow(GuiGraphicsExtractor context, DevTool tool, int index, int mouseX, int mouseY) {
        int ry = rowY(index);
        context.blit(RenderPipelines.GUI_TEXTURED, ROW, ROW_ORIGIN_X, ry, 0f, 0f, ROW_W, ROW_H, ROW_W, ROW_H);

        boolean enabled = DevToolsStateStore.isEnabled(tool.id());
        int iconCellX = ROW_ORIGIN_X + SLOT_ICON * SLOT;
        int iconCellY = ry;
        int iconX = iconCellX + 1;
        int iconY = iconCellY + 1;

        drawToolIcon(context, tool, iconX, iconY);
        drawStatusBorder(context, iconCellX, iconCellY, enabled);

        // Mode
        if (!tool.modes().isEmpty()) {
            DevToolMode mode = resolveMode(tool);
            int modeX = ROW_ORIGIN_X + SLOT_MODE * SLOT;
            drawCenteredSlotText(context, mode.shortLabel(), modeX, ry, TEXT_COLOR);
        }

        // Beschreibung zentriert zwischen Modus-Slot und Hotkey-Slot
        int descAreaX = ROW_ORIGIN_X + (SLOT_MODE + 1) * SLOT;
        int descAreaW = (SLOT_HOTKEY - SLOT_MODE - 1) * SLOT;
        int descY = ry + (ROW_H - 8) / 2;
        String desc = tool.description();
        int descTw = this.font.width(desc);
        int descX = descAreaX + Math.max(0, (descAreaW - descTw) / 2);
        context.text(this.font, Component.literal(desc), descX, descY, TEXT_COLOR, false);

        // Hotkey
        Integer hotkey = DevToolsStateStore.getHotkey(tool.id());
        String capturingId = DevToolsHotkeyDispatcher.getCapturingToolId();
        String hotkeyLabel = capturingId != null && capturingId.equals(tool.id())
            ? "..."
            : DevToolsHotkeyDispatcher.formatKey(hotkey);
        int hkX = ROW_ORIGIN_X + SLOT_HOTKEY * SLOT;
        drawCenteredSlotText(context, truncate(hotkeyLabel, 4), hkX, ry, TEXT_COLOR);

        // Hover tracking (screen coords)
        updateHover(tool, index, mouseX, mouseY, descAreaX, descAreaW);
    }

    private void drawToolIcon(GuiGraphicsExtractor context, DevTool tool, int x, int y) {
        Identifier tex = tool.iconTexture();
        if (tex != null) {
            try {
                // blit(width,height,texW,texH) sampelt width×height aus der Textur —
                // bei 64px-Quelle daher per Pose auf 16×16 skalieren (sonst nur Ecke).
                int texSize = Math.max(1, tool.iconTextureSize());
                if (texSize == ICON) {
                    context.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, ICON, ICON, ICON, ICON);
                } else {
                    float scale = ICON / (float) texSize;
                    context.pose().pushMatrix();
                    context.pose().translate(x, y);
                    context.pose().scale(scale, scale);
                    context.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0f, 0f, texSize, texSize, texSize, texSize);
                    context.pose().popMatrix();
                }
                return;
            } catch (Exception ignored) {
            }
        }
        drawCenteredSlotText(context, tool.iconFallback(), x - 1, y - 1, TEXT_COLOR);
    }

    /**
     * 1px-Rahmen der 18×18-Zelle um das 16×16-Icon (An=grün, Aus=rot).
     */
    private void drawStatusBorder(GuiGraphicsExtractor context, int cellX, int cellY, boolean enabled) {
        int color = enabled ? BORDER_ON : BORDER_OFF;
        int x0 = cellX;
        int y0 = cellY;
        int x1 = cellX + SLOT;
        int y1 = cellY + SLOT;
        context.fill(x0, y0, x1, y0 + 1, color);
        context.fill(x0, y1 - 1, x1, y1, color);
        context.fill(x0, y0, x0 + 1, y1, color);
        context.fill(x1 - 1, y0, x1, y1, color);
    }

    private void drawCenteredSlotText(GuiGraphicsExtractor context, String text, int cellX, int cellY, int color) {
        int tw = this.font.width(text);
        int tx = cellX + (SLOT - tw) / 2;
        int ty = cellY + (SLOT - 8) / 2;
        context.text(this.font, Component.literal(text), tx, ty, color, false);
    }

    private void updateHover(DevTool tool, int rowIndex, int mouseX, int mouseY, int descAreaX, int descAreaW) {
        int ry = rowY(rowIndex);
        if (hit(mouseX, mouseY, ROW_ORIGIN_X + SLOT_ICON * SLOT, ry, SLOT, ROW_H)) {
            hoverToolId = tool.id();
            hoverSlot = SLOT_ICON;
            return;
        }
        if (!tool.modes().isEmpty()
            && hit(mouseX, mouseY, ROW_ORIGIN_X + SLOT_MODE * SLOT, ry, SLOT, ROW_H)) {
            hoverToolId = tool.id();
            hoverSlot = SLOT_MODE;
            return;
        }
        if (hit(mouseX, mouseY, ROW_ORIGIN_X + SLOT_HOTKEY * SLOT, ry, SLOT, ROW_H)) {
            hoverToolId = tool.id();
            hoverSlot = SLOT_HOTKEY;
            return;
        }
        if (hit(mouseX, mouseY, descAreaX, ry, descAreaW, ROW_H)) {
            hoverToolId = tool.id();
            hoverSlot = 3;
        }
    }

    private boolean hit(int mouseX, int mouseY, int texX, int texY, int texW, int texH) {
        int x = sx(texX);
        int y = sy(texY);
        int w = ss(texW);
        int h = ss(texH);
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void renderHoverTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        DevTool tool = DevToolsRegistry.get(hoverToolId);
        if (tool == null) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        if (hoverSlot == SLOT_ICON) {
            boolean on = DevToolsStateStore.isEnabled(tool.id());
            lines.add(Component.literal(on ? "An" : "Aus"));
            for (String line : tool.hoverLines()) {
                lines.add(Component.literal(line));
            }
        } else if (hoverSlot == SLOT_MODE && !tool.modes().isEmpty()) {
            for (DevToolMode mode : tool.modes()) {
                lines.add(Component.literal(mode.hoverLabel()));
            }
        } else if (hoverSlot == SLOT_HOTKEY) {
            String conflict = DevToolsHotkeyDispatcher.getHotkeyConflictMessage(tool.id());
            if (conflict != null) {
                lines.add(Component.literal(conflict).withColor(0xFF5555));
            }
            if (DevToolsHotkeyDispatcher.isCapturingHotkey()
                && tool.id().equals(DevToolsHotkeyDispatcher.getCapturingToolId())) {
                lines.add(Component.literal("Taste drücken…"));
                lines.add(Component.literal("Entf = löschen, ESC = abbrechen"));
            } else {
                Integer hk = DevToolsStateStore.getHotkey(tool.id());
                lines.add(Component.literal("Hotkey: " + DevToolsHotkeyDispatcher.formatKey(hk)));
                lines.add(Component.literal("Klick zum Zuweisen"));
            }
        } else if (hoverSlot == 3) {
            for (String line : tool.hoverLines()) {
                lines.add(Component.literal(line));
            }
        }
        if (!lines.isEmpty()) {
            context.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }
    }

    private DevToolMode resolveMode(DevTool tool) {
        if (tool instanceof ItemDataReaderDevTool) {
            return ItemDataReaderDevTool.resolveMode();
        }
        String modeId = DevToolsStateStore.getModeId(tool.id());
        for (DevToolMode mode : tool.modes()) {
            if (mode.id().equals(modeId)) {
                return mode;
            }
        }
        return tool.modes().isEmpty() ? null : tool.modes().get(0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        recomputeLayout();
        if (event.button() != 0) {
            return super.mouseClicked(event, doubled);
        }
        double mouseX = event.x();
        double mouseY = event.y();

        List<DevTool> tools = DevToolsRegistry.all();
        for (int i = 0; i < tools.size(); i++) {
            DevTool tool = tools.get(i);
            int ry = rowY(i);

            // Icon → toggle
            if (hit((int) mouseX, (int) mouseY, ROW_ORIGIN_X + SLOT_ICON * SLOT, ry, SLOT, ROW_H)) {
                DevToolsStateStore.setEnabled(tool.id(), !DevToolsStateStore.isEnabled(tool.id()));
                return true;
            }

            // Mode → cycle
            if (!tool.modes().isEmpty()
                && hit((int) mouseX, (int) mouseY, ROW_ORIGIN_X + SLOT_MODE * SLOT, ry, SLOT, ROW_H)) {
                if (tool instanceof ItemDataReaderDevTool) {
                    ItemDataReaderDevTool.cycleMode();
                } else {
                    cycleGenericMode(tool);
                }
                return true;
            }

            // Hotkey → capture
            if (hit((int) mouseX, (int) mouseY, ROW_ORIGIN_X + SLOT_HOTKEY * SLOT, ry, SLOT, ROW_H)) {
                DevToolsHotkeyDispatcher.beginHotkeyCapture(tool.id(), () -> {
                });
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    private void cycleGenericMode(DevTool tool) {
        List<DevToolMode> modes = tool.modes();
        if (modes.isEmpty()) {
            return;
        }
        String current = DevToolsStateStore.getModeId(tool.id());
        int idx = 0;
        for (int i = 0; i < modes.size(); i++) {
            if (modes.get(i).id().equals(current)) {
                idx = i;
                break;
            }
        }
        DevToolMode next = modes.get((idx + 1) % modes.size());
        DevToolsStateStore.setModeId(tool.id(), next.id());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (DevToolsHotkeyDispatcher.isCapturingHotkey()) {
            return DevToolsHotkeyDispatcher.handleScreenKeyPress(event.key(), event.scancode(), event.modifiers());
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        DevToolsHotkeyDispatcher.cancelHotkeyCapture();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(new DevToolsScreen());
        }
    }
}

package net.felix.utilities.Overall;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Util;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * Server-Listen-Eintrag fÃ¼r playlegend.net im Mehrspieler-Menü.
 */
public class PlayLegendServerEntryWidget extends AbstractWidget {

    public static final int ENTRY_WIDTH = 200;
    public static final int ENTRY_HEIGHT = 36;

    private static final String SERVER_NAME = "PlayLegend";
    private static final String SERVER_IP = "playlegend.net";

    private static final int ICON_SIZE = 32;
    private static final int STATUS_ICON_WIDTH = 10;
    private static final int STATUS_ICON_HEIGHT = 8;
    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int MOTD_COLOR = 0xFF808080;
    private static final int BG_COLOR = 0x80000000;
    private static final int BG_HOVER_COLOR = 0xA0000000;
    private static final int HOVER_BORDER = 0xFFFFFFFF;

    private static final Identifier PING_1 = Identifier.withDefaultNamespace("server_list/ping_1");
    private static final Identifier PING_2 = Identifier.withDefaultNamespace("server_list/ping_2");
    private static final Identifier PING_3 = Identifier.withDefaultNamespace("server_list/ping_3");
    private static final Identifier PING_4 = Identifier.withDefaultNamespace("server_list/ping_4");
    private static final Identifier PING_5 = Identifier.withDefaultNamespace("server_list/ping_5");
    private static final Identifier PINGING_1 = Identifier.withDefaultNamespace("server_list/pinging_1");
    private static final Identifier PINGING_2 = Identifier.withDefaultNamespace("server_list/pinging_2");
    private static final Identifier PINGING_3 = Identifier.withDefaultNamespace("server_list/pinging_3");
    private static final Identifier PINGING_4 = Identifier.withDefaultNamespace("server_list/pinging_4");
    private static final Identifier PINGING_5 = Identifier.withDefaultNamespace("server_list/pinging_5");
    private static final Identifier INCOMPATIBLE = Identifier.withDefaultNamespace("server_list/incompatible");
    private static final Identifier UNREACHABLE = Identifier.withDefaultNamespace("server_list/unreachable");

    private final Screen parent;
    private final Minecraft minecraft;
    private final ServerData serverData;
    private final ServerStatusPinger pinger = new ServerStatusPinger();
    private final FaviconTexture icon;

    private byte[] lastIconBytes;
    private Identifier statusIcon;
    private Component statusIconTooltip;
    private boolean pingStarted;
    private boolean closed;
    private int pingEpoch;
    private boolean countLoading;
    private Integer onlineCount;
    private long nextCountRequest;
    private static final java.net.http.HttpClient COUNT_HTTP = java.net.http.HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(5)).build();

    public PlayLegendServerEntryWidget(Screen parent, int x, int y) {
        super(x, y, ENTRY_WIDTH, ENTRY_HEIGHT, Component.literal(SERVER_NAME));
        this.parent = parent;
        this.minecraft = Minecraft.getInstance();
        this.serverData = new ServerData(SERVER_NAME, SERVER_IP, ServerData.Type.OTHER);
        this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        // Eigene Texture-ID, damit close() nicht das Favicon eines gespeicherten playlegend.net-Eintrags löscht.
        this.icon = FaviconTexture.forServer(this.minecraft.getTextureManager(), "cclive-pinned:" + SERVER_IP);
        refreshStatus();
    }

    public void tick() {
        this.pinger.tick();
        if (!closed && !countLoading && Util.getMillis() >= nextCountRequest
                && net.felix.CCLiveUtilitiesConfig.HANDLER.instance().dataTrackingEnabled) {
            countLoading = true;
            nextCountRequest = Util.getMillis() + 30000;
            String base = new net.felix.leaderboards.config.LeaderboardConfig().getServerUrl();
            var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/presence/count"))
                .timeout(java.time.Duration.ofSeconds(5)).GET().build();
            final int epoch = this.pingEpoch;
            COUNT_HTTP.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> minecraft.execute(() -> {
                    if (closed || epoch != pingEpoch) return;
                    countLoading = false;
                    onlineCount = null;
                    try {
                        if (error == null && response.statusCode() == 200) {
                            int count = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject().get("online").getAsInt();
                            if (count >= 0) onlineCount = count;
                        }
                    } catch (RuntimeException ignored) { }
                }));
        }
    }

    public void pausePings() {
        this.pingEpoch++;
        this.pinger.removeAll();
        this.pingStarted = false;
        if (this.serverData.state() == ServerData.State.PINGING) {
            this.serverData.setState(ServerData.State.INITIAL);
        }
    }

    public void close() {
        this.closed = true;
        this.pingEpoch++;
        this.pinger.removeAll();
        this.icon.close();
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubled) {
        playDownSound(this.minecraft.getSoundManager());
        ConnectScreen.startConnecting(
            this.parent,
            this.minecraft,
            ServerAddress.parseString(this.serverData.ip),
            this.serverData,
            false,
            null
        );
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (closed) {
            return;
        }
        startPingIfNeeded();

        int x = getX();
        int y = getY();
        boolean hovered = isHoveredOrFocused();

        graphics.fill(x, y, x + this.width, y + this.height, hovered ? BG_HOVER_COLOR : BG_COLOR);
        if (hovered) {
            graphics.fill(x, y, x + this.width, y + 1, HOVER_BORDER);
            graphics.fill(x, y + this.height - 1, x + this.width, y + this.height, HOVER_BORDER);
            graphics.fill(x, y, x + 1, y + this.height, HOVER_BORDER);
            graphics.fill(x + this.width - 1, y, x + this.width, y + this.height, HOVER_BORDER);
        }

        Font font = this.minecraft.font;
        int textX = x + ICON_SIZE + 3;
        graphics.text(font, this.serverData.name, textX, y + 1, NAME_COLOR);

        Component motd = this.serverData.motd != null ? this.serverData.motd : CommonComponents.EMPTY;
        // Split only at the server's line breaks, not at the narrow widget width.
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(motd, Integer.MAX_VALUE);
        int availableWidth = getRight() - textX - 3;
        for (int i = 0; i < Math.min(lines.size(), 2); i++) {
            var line = lines.get(i);
            float scale = Math.min(0.85f, (float) availableWidth / Math.max(1, font.width(line)));
            graphics.pose().pushMatrix();
            graphics.pose().translate(textX, y + 13 + 10 * i);
            graphics.pose().scale(scale, scale);
            graphics.text(font, line, 0, 0, MOTD_COLOR);
            graphics.pose().popMatrix();
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, this.icon.textureLocation(), x, y + 2, 0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        if (this.serverData.state() == ServerData.State.PINGING) {
            int iconIndex = (int) ((Util.getMillis() / 100L) & 7L);
            if (iconIndex > 4) {
                iconIndex = 8 - iconIndex;
            }
            this.statusIcon = switch (iconIndex) {
                case 1 -> PINGING_2;
                case 2 -> PINGING_3;
                case 3 -> PINGING_4;
                case 4 -> PINGING_5;
                default -> PINGING_1;
            };
        } else {
            refreshStatus();
        }

        int statusIconX = getRight() - STATUS_ICON_WIDTH - 5;
        if (this.statusIcon != null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.statusIcon, statusIconX, y, STATUS_ICON_WIDTH, STATUS_ICON_HEIGHT);
        }

        byte[] iconBytes = this.serverData.getIconBytes();
        if (!Arrays.equals(iconBytes, this.lastIconBytes)) {
            if (uploadServerIcon(iconBytes)) {
                this.lastIconBytes = iconBytes;
            } else {
                this.serverData.setIconBytes(null);
            }
        }

        Component status = this.serverData.state() == ServerData.State.INCOMPATIBLE && this.serverData.version != null
            ? this.serverData.version.copy().withStyle(ChatFormatting.RED)
            : this.serverData.status;
        if (status != null && status != CommonComponents.EMPTY) {
            int statusWidth = font.width(status);
            int statusX = statusIconX - statusWidth - 5;
            graphics.text(font, status, statusX, y + 1, MOTD_COLOR);
        }

    }

    /** Reuse the widget-owned ping texture; no second download or texture lifecycle. */
    public void extractGroupIcon(GuiGraphicsExtractor graphics, int x, int y, int size) {
        if (closed || icon.isClosed() || lastIconBytes == null) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon.textureLocation(),
            x, y, 0f, 0f, size, size, 64, 64, 64, 64);
    }

    /** Schedule after the pinned multiplayer entry. */
    public void extractOnlineTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!visible || mouseX < getX() || mouseX >= getRight()
                || mouseY < getY() || mouseY >= getY() + height) return;
        String value = onlineCount == null ? (countLoading ? "wird geladen …" : "nicht verfügbar") : onlineCount.toString();
        Component tooltip = Component.literal("CCLive Spieler Online: " + value);
        if (statusIconTooltip != null && mouseX >= getRight() - STATUS_ICON_WIDTH - 5
                && mouseY <= getY() + STATUS_ICON_HEIGHT) {
            tooltip = tooltip.copy().append(Component.literal("\n")).append(statusIconTooltip);
        }
        graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        if (this.serverData.motd != null && this.serverData.motd != CommonComponents.EMPTY) {
            output.add(NarratedElementType.HINT, this.serverData.motd);
        }
    }

    private void startPingIfNeeded() {
        if (this.pingStarted || this.serverData.state() != ServerData.State.INITIAL) {
            return;
        }
        this.pingStarted = true;
        this.serverData.setState(ServerData.State.PINGING);
        this.serverData.motd = CommonComponents.EMPTY;
        this.serverData.status = CommonComponents.EMPTY;
        Util.backgroundExecutor().execute(this::pingServer);
    }

    private void pingServer() {
        final int epoch = this.pingEpoch;
        try {
            this.pinger.pingServer(
                this.serverData,
                () -> refreshStatusIfCurrent(epoch),
                () -> refreshStatusIfCurrent(epoch),
                EventLoopGroupHolder.remote(this.minecraft.options.useNativeTransport())
            );
        } catch (UnknownHostException e) {
            this.serverData.setState(ServerData.State.UNREACHABLE);
            this.serverData.motd = Component.translatable("multiplayer.status.cannot_resolve");
            this.minecraft.execute(() -> refreshStatusIfCurrent(epoch));
        } catch (Exception e) {
            this.serverData.setState(ServerData.State.UNREACHABLE);
            this.serverData.motd = Component.translatable("multiplayer.status.cannot_connect");
            this.minecraft.execute(() -> refreshStatusIfCurrent(epoch));
        }
    }

    private void refreshStatusIfCurrent(int epoch) {
        if (closed || epoch != pingEpoch) {
            return;
        }
        refreshStatus();
    }

    private void refreshStatus() {
        this.statusIconTooltip = null;
        switch (this.serverData.state()) {
            case INITIAL, PINGING -> {
                this.statusIcon = PING_1;
                this.statusIconTooltip = Component.translatable("multiplayer.status.pinging");
            }
            case INCOMPATIBLE -> {
                this.statusIcon = INCOMPATIBLE;
                this.statusIconTooltip = Component.translatable("multiplayer.status.incompatible");
            }
            case UNREACHABLE -> {
                this.statusIcon = UNREACHABLE;
                this.statusIconTooltip = Component.translatable("multiplayer.status.no_connection");
            }
            case SUCCESSFUL -> {
                long ping = this.serverData.ping;
                if (ping < 150L) {
                    this.statusIcon = PING_5;
                } else if (ping < 300L) {
                    this.statusIcon = PING_4;
                } else if (ping < 600L) {
                    this.statusIcon = PING_3;
                } else if (ping < 1000L) {
                    this.statusIcon = PING_2;
                } else {
                    this.statusIcon = PING_1;
                }
                this.statusIconTooltip = Component.translatable("multiplayer.status.ping", ping);
            }
        }
    }

    private boolean uploadServerIcon(byte[] bytes) {
        if (closed || icon.isClosed()) {
            return false;
        }
        try {
            if (bytes == null) {
                this.icon.clear();
            } else {
                this.icon.upload(NativeImage.read(bytes));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

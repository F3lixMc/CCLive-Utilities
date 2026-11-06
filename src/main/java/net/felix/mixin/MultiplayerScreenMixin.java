package net.felix.mixin;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.felix.utilities.Overall.PlayLegendServerEntryWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Angehefteter PlayLegend-Bereich über der normalen Serverliste.
 * Nach Link-Dialogen ruft Vanilla nur {@code repositionElements} auf, nicht erneut {@code init}.
 */
@Mixin(JoinMultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    @Shadow protected ServerSelectionList serverSelectionList;
    @Shadow private HeaderAndFooterLayout layout;

    @Unique private PlayLegendServerEntryWidget cclive$entry;
    @Unique private final List<Button> cclive$links = new ArrayList<>();
    @Unique private boolean cclive$decorationsAdded;
    @Unique private static final int CCLIVE_PINNED_HEIGHT = 52;

    protected MultiplayerScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("HEAD"))
    private void cclive$preparePinnedServer(CallbackInfo ci) {
        // Vollständiger Rebuild (clearWidgets ist bereits gelaufen): alte Instanz schließen.
        cclive$closeEntry();
        cclive$links.clear();
        cclive$decorationsAdded = false;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void cclive$initPinnedServer(CallbackInfo ci) {
        cclive$ensurePinnedWidgets();
        cclive$layoutPinnedServer();
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void cclive$resizePinnedServer(CallbackInfo ci) {
        cclive$ensurePinnedWidgets();
        cclive$layoutPinnedServer();
    }

    @Unique private void cclive$ensurePinnedWidgets() {
        if (cclive$entry != null && cclive$links.size() == 4) {
            return;
        }
        cclive$closeEntry();
        cclive$links.clear();
        cclive$entry = addRenderableWidget(new PlayLegendServerEntryWidget(this, 0, 0));
        String[] labels = {"PlayLegend: Shop", "PlayLegend: Discord", "CCLive: Discord", "CCLive: Website"};
        String[] urls = {"https://shop.playlegend.net", "https://discord.gg/playlegend", "https://discord.gg/7geExrDeqF", "https://cclive-utilities.net/"};
        for (int i = 0; i < labels.length; i++) {
            Button.OnPress action = urls[i] == null ? button -> { } : ConfirmLinkScreen.confirmLink(this, URI.create(urls[i]));
            Button button = Button.builder(Component.literal(""), action)
                    .bounds(0, 0, 20, 20).tooltip(Tooltip.create(Component.literal(labels[i]))).build();
            button.setOverrideRenderHighlightedSprite(button::isHovered);
            cclive$links.add(addRenderableWidget(button));
        }
        if (!cclive$decorationsAdded) {
            addRenderableOnly(this::cclive$extractPinnedDecorations);
            cclive$decorationsAdded = true;
        }
    }

    @Unique private void cclive$layoutPinnedServer() {
        if (cclive$entry == null || serverSelectionList == null || layout == null || cclive$links.size() != 4) {
            return;
        }
        // Immer Vanilla-Content-Rechteck, nie die bereits verkleinerte Liste.
        int vanillaTop = layout.getHeaderHeight();
        int vanillaHeight = layout.getContentHeight();
        int vanillaX = 0;
        int vanillaWidth = this.width;
        int rowWidth = Math.min(serverSelectionList.getRowWidth(), Math.max(200, width - 16));
        int left = (width - rowWidth) / 2;
        int linkWidth = 44;
        int entryX = left + linkWidth + 8;
        cclive$entry.setWidth(rowWidth - linkWidth - 8);
        cclive$entry.setPosition(entryX, vanillaTop + 8);
        for (int i = 0; i < cclive$links.size(); i++) {
            cclive$links.get(i).setPosition(left + (i % 2) * 21, vanillaTop + 4 + (i / 2) * 21);
        }
        serverSelectionList.updateSizeAndPosition(
            vanillaWidth,
            Math.max(0, vanillaHeight - CCLIVE_PINNED_HEIGHT),
            vanillaX,
            vanillaTop + CCLIVE_PINNED_HEIGHT);
    }

    @Unique private void cclive$extractPinnedDecorations(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (cclive$entry == null || cclive$links.size() != 4) return;
        Button website = cclive$links.get(0);
        cclive$entry.extractGroupIcon(graphics, website.getX() + 2, website.getY() + 2, 16);
        String[] textures = {"plicon_discord.png", "ccicon_discord.png"};
        for (int i = 0; i < textures.length; i++) {
            Button button = cclive$links.get(i + 1);
            graphics.blit(RenderPipelines.GUI_TEXTURED,
                    Identifier.fromNamespaceAndPath("cclive-utilities", "textures/icons/" + textures[i]),
                    button.getX() + 2, button.getY() + 2, 0f, 0f, 16, 16, 128, 128, 128, 128);
        }
        Button ccliveWebsite = cclive$links.get(3);
        graphics.blit(RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath("cclive-utilities", "textures/icon.png"),
                ccliveWebsite.getX() + 2, ccliveWebsite.getY() + 2, 0f, 0f, 16, 16, 128, 128, 128, 128);
        cclive$entry.extractOnlineTooltip(graphics, mouseX, mouseY);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void cclive$tickPinnedServer(CallbackInfo ci) {
        if (cclive$entry != null) cclive$entry.tick();
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void cclive$removePinnedServer(CallbackInfo ci) {
        // Nur Pings stoppen. Widget und Textur bleiben: nach ConfirmLinkScreen kommt kein init().
        if (cclive$entry != null) {
            cclive$entry.pausePings();
        }
    }

    @Unique private void cclive$closeEntry() {
        if (cclive$entry != null) {
            cclive$entry.close();
            cclive$entry = null;
        }
    }
}

package net.felix.mixin;

import java.util.List;
import net.felix.Community.playericon.PlayerNametagIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayRenderer.TextDisplayRenderer.class)
abstract class PlayerTextDisplayIconMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Display$TextDisplay;Lnet/minecraft/client/renderer/entity/state/TextDisplayEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void cclive$appendNametagIcon(TextDisplay display, TextDisplayEntityRenderState state,
                                         float partialTick, CallbackInfo ci) {
        TextDisplay.CachedInfo original = state.cachedInfo;
        Minecraft client = Minecraft.getInstance();
        if (original == null || original.lines().size() != 1 || !PlayerNametagIcons.enabled(client)) {
            return;
        }

        Player player = PlayerNametagIcons.findOwner(display, display.getText(), client, partialTick);
        Component suffix = PlayerNametagIcons.suffix(client, player);
        if (suffix == null) {
            return;
        }

        // Extend this render state's line. The server text and vanilla's cached layout stay intact.
        TextDisplay.CachedLine line = original.lines().getFirst();
        FormattedCharSequence glyphs = suffix.getVisualOrderText();
        int width = line.width() + client.font.width(glyphs);
        var withIcon = new TextDisplay.CachedLine(FormattedCharSequence.composite(line.contents(), glyphs), width);
        state.cachedInfo = new TextDisplay.CachedInfo(List.of(withIcon), width);
    }
}

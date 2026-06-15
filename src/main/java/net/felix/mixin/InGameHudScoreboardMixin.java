package net.felix.mixin;

import net.felix.utilities.Overall.CoinTrackerCustomSidebar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudScoreboardMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cclive$replaceScoreboardSidebar(
            DrawContext context,
            ScoreboardObjective objective,
            CallbackInfo ci) {
        if (!CoinTrackerCustomSidebar.shouldReplaceVanillaSidebar()) {
            return;
        }

        ci.cancel();
        CoinTrackerCustomSidebar.render(context, objective, getTextRenderer(), client);
    }
}

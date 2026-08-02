package net.felix.mixin;

import net.felix.utilities.Overall.CoinTrackerCustomSidebar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudScoreboardMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract Font getFont();

    @Inject(
            method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cclive$replaceScoreboardSidebar(
            GuiGraphicsExtractor context,
            Objective objective,
            CallbackInfo ci) {
        if (!CoinTrackerCustomSidebar.shouldReplaceVanillaSidebar()) {
            return;
        }

        ci.cancel();
        CoinTrackerCustomSidebar.render(context, objective, getFont(), minecraft);
    }
}

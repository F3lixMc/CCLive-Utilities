package net.felix.mixin;

import net.felix.utilities.SchmiedTrackerUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow protected int x;
    @Shadow protected int y;

    @Inject(method = "render", at = @At("TAIL"))
    private void renderColoredFrames(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        SchmiedTrackerUtility.renderColoredFrames(context, (HandledScreen<?>) (Object) this, x, y);
        SchmiedTrackerUtility.renderHideUncraftableButton(context, (HandledScreen<?>) (Object) this);
    }
} 
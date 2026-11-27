package net.felix.mixin;

import net.felix.utilities.Overall.starforged.StarforgedAnimationUtility;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks mouse presses, scrolling and camera turning while the starforged animation runs. */
@Mixin(MouseHandler.class)
public class StarforgedMouseInputMixin {

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void cclive$blockButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        // Releases (action 0) pass so no button stays stuck down after the animation.
        if (action != 0 && StarforgedAnimationUtility.isInputBlocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void cclive$blockScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (StarforgedAnimationUtility.isInputBlocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void cclive$blockTurn(double movementTime, CallbackInfo ci) {
        if (StarforgedAnimationUtility.isInputBlocked()) {
            // Drop the movement instead of letting it pile up into a jump after the animation.
            accumulatedDX = 0.0;
            accumulatedDY = 0.0;
            ci.cancel();
        }
    }
}

package net.felix.mixin;

import net.felix.utilities.Overall.starforged.StarforgedAnimationUtility;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks key presses and typing while the starforged animation runs; F2 (screenshot) still works and
 * Escape skips the animation.
 */
@Mixin(KeyboardHandler.class)
public class StarforgedKeyboardInputMixin {

    // GLFW values; LWJGL is not on the compile classpath here.
    private static final int GLFW_RELEASE = 0;
    private static final int GLFW_PRESS = 1;
    private static final int GLFW_KEY_F2 = 291;
    private static final int GLFW_KEY_ESCAPE = 256;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void cclive$blockKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (action == GLFW_PRESS && event.key() == GLFW_KEY_ESCAPE && StarforgedAnimationUtility.isInputBlocked()) {
            // Skip the animation; swallow the key so the pause menu does not open as well.
            StarforgedAnimationUtility.cancelAnimation();
            ci.cancel();
            return;
        }
        // Releases pass so no key (e.g. walking) stays stuck down after the animation.
        if (action != GLFW_RELEASE && event.key() != GLFW_KEY_F2
                && StarforgedAnimationUtility.isInputBlocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void cclive$blockChar(long window, CharacterEvent event, CallbackInfo ci) {
        if (StarforgedAnimationUtility.isInputBlocked()) {
            ci.cancel();
        }
    }
}

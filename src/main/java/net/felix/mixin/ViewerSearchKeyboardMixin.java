package net.felix.mixin;

import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes committed text to the custom viewer before container/widget dispatch. */
@Mixin(KeyboardHandler.class)
public class ViewerSearchKeyboardMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void cclive$viewerCharacter(long window, CharacterEvent event, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (window != 0 && window != client.getWindow().handle()) return;
        if (client.gui.overlay() != null) return;
        if (ItemViewerUtility.handleSearchFieldCharTyped(event)) ci.cancel();
    }
}

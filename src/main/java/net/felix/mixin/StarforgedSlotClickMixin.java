package net.felix.mixin;

import net.felix.utilities.Overall.starforged.StarforgedAnimationUtility;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the starforged animation which blueprint the player clicked to craft, and swallows container
 * clicks while the animation runs.
 */
@Mixin(AbstractContainerScreen.class)
public class StarforgedSlotClickMixin {

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"), cancellable = true)
    private void cclive$onSlotClicked(Slot slot, int slotId, int button, ContainerInput input, CallbackInfo ci) {
        if (StarforgedAnimationUtility.isInputBlocked()) {
            ci.cancel();
            return;
        }
        // Left click crafts one item, shift + left click a batch of 10.
        if (slot != null && button == 0 && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            StarforgedAnimationUtility.onSlotClicked(screen.getTitle(), slot.getItem(), input == ContainerInput.QUICK_MOVE);
        } else if (slot != null) {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            StarforgedAnimationUtility.logFilteredClick(screen.getTitle(), button, String.valueOf(input));
        }
    }
}

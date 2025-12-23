package net.felix.mixin;

import net.felix.utilities.Town.EquipmentDisplayUtility;
import net.felix.utilities.Overall.InformationenUtility;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseScrollMixin {
    
    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        // Übergebe das Scroll-Event an die EquipmentDisplayUtility
        EquipmentDisplayUtility.onMouseScroll(vertical);
        // Übergebe das Scroll-Event an die InformationenUtility für MKLevel Overlay
        InformationenUtility.onMKLevelMouseScroll(vertical);
    }
} 
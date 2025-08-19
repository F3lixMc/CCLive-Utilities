package net.felix.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {



    // This mixin is no longer needed since we've replaced kill tracking
    // with a text display scanning system in KillsUtility
    // Keeping the mixin class for potential future use
    
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        // Kill tracking is now handled by text display scanning in KillsUtility
        // This method is kept for potential future use
    }
} 
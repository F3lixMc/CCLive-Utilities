package net.felix.mixin;

import net.felix.utilities.Overall.KillAnimationUtility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected int deathTime;

    // This mixin is no longer needed since we've replaced kill tracking
    // with a text display scanning system in KillsUtility
    // Keeping the mixin class for potential future use
    
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        try {
            LivingEntity entity = (LivingEntity)(Object)this;
            
            // Only disable for monsters (non-player entities)
            if (entity instanceof PlayerEntity) {
                return;
            }
            
            // Check if kill animation utility is enabled
            if (KillAnimationUtility.isKillAnimationDisabled()) {
                // Set deathTime to 20 to immediately remove the entity (skips death animation)
                // In Minecraft, entities are removed when deathTime >= 20
                deathTime = 20;
                // Also make entity invisible immediately to prevent any rendering
                entity.setInvisible(true);
            }
        } catch (Exception e) {
            // Silently fail to prevent crashes
        }
    }
    
    // Disable death animation for monsters (non-player entities)
    // This runs every tick to ensure deathTime stays at 20 if entity is dead
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickStart(CallbackInfo ci) {
        try {
            LivingEntity entity = (LivingEntity)(Object)this;
            
            // Only disable for monsters (non-player entities)
            if (entity instanceof PlayerEntity) {
                return;
            }
            
            // Check if kill animation utility is enabled
            if (KillAnimationUtility.isKillAnimationDisabled()) {
                // If entity is dead or dying, immediately set deathTime to 20 and make invisible
                if (entity.isDead() || entity.getHealth() <= 0) {
                    if (deathTime < 20) {
                        deathTime = 20;
                    }
                    // Make entity invisible immediately to prevent any rendering
                    entity.setInvisible(true);
                    // On client side, we can't remove the entity directly, but setting deathTime to 20
                    // will cause it to be removed in the next tick
                }
            }
        } catch (Exception e) {
            // Silently fail to prevent crashes
        }
    }
    
    // Make entity invisible when dead to prevent any rendering
    // Note: isInvisible() is in Entity class, so we need to check if entity is dead
    // and make it invisible by setting the invisible flag
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickEnd(CallbackInfo ci) {
        try {
            LivingEntity entity = (LivingEntity)(Object)this;
            
            // Only disable for monsters (non-player entities)
            if (entity instanceof PlayerEntity) {
                return;
            }
            
            // Check if kill animation utility is enabled and entity is dead
            if (KillAnimationUtility.isKillAnimationDisabled() && entity.isDead()) {
                // Set entity as invisible to prevent rendering
                // This is done by setting the invisible NBT data
                entity.setInvisible(true);
            }
        } catch (Exception e) {
            // Silently fail to prevent crashes
        }
    }
} 
package net.felix.mixin;

import net.felix.Community.playericon.PlayerNametagIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
abstract class PlayerNametagIconMixin {
    @Inject(
            method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
            at = @At("TAIL")
    )
    private void cclive$appendNametagIcon(Entity entity, EntityRenderState state, float partialTick,
                                          double nameDistance, double scoreDistance, CallbackInfo ci) {
        if (state.nameTag == null || !(entity instanceof Player || entity instanceof ArmorStand)) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!PlayerNametagIcons.enabled(client)) {
            return;
        }

        Player player;
        if (entity instanceof Player nativePlayer) {
            player = nativePlayer;
        } else if (entity instanceof ArmorStand stand && stand.isInvisible()
                && stand.isCustomNameVisible() && stand.hasCustomName()) {
            player = PlayerNametagIcons.findOwner(stand, state.nameTag, client, partialTick);
        } else {
            return;
        }

        Component suffix = PlayerNametagIcons.suffix(client, player);
        if (suffix != null) {
            state.nameTag = Component.empty().append(state.nameTag).append(suffix);
        }
    }
}

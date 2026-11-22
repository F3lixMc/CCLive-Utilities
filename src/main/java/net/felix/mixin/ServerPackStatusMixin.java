package net.felix.mixin;

import io.netty.channel.ChannelFutureListener;
import net.felix.utilities.Other.serverpack.ServerPackCache;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Watches outgoing pack status packets to learn when vanilla finished loading a new server pack. */
@Mixin(Connection.class)
public abstract class ServerPackStatusMixin {

    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
        at = @At("HEAD"))
    private void cclive$onPackStatus(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (packet instanceof ServerboundResourcePackPacket status) {
            ServerPackCache.onStatusSent(status);
        }
    }
}

package net.felix.mixin;

import net.felix.utilities.Other.serverpack.ServerPackCache;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Answers a server pack push directly when the cached copy of that pack is already loaded. */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ServerPackPushMixin {

    @Shadow @Final protected Connection connection;

    // After the thread hop, so the handler runs on the client thread.
    @Inject(
        method = "handleResourcePackPush",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
            shift = At.Shift.AFTER),
        cancellable = true)
    private void cclive$skipCachedServerPack(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        if (ServerPackCache.handlePush(connection, packet)) {
            ci.cancel();
        }
    }
}

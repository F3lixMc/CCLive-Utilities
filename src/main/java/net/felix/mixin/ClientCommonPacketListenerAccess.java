package net.felix.mixin;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Server entry of the connection; also set in the configuration phase, unlike {@code Minecraft.getCurrentServer()}. */
@Mixin(ClientCommonPacketListenerImpl.class)
public interface ClientCommonPacketListenerAccess {
    @Accessor("serverData")
    ServerData cclive$getServerData();
}

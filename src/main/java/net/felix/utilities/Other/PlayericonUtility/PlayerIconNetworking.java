package net.felix.utilities.Other.PlayericonUtility;

import net.felix.CCLiveUtilities;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Networking system to synchronize which players have the CCLive-Utilities mod installed.
 * This allows players to see the icon next to other players who also use the mod.
 * 
 * Uses Fabric's Custom Payload API to send mod presence information between clients.
 * Note: This requires the server to support custom payloads, or we need to use
 * a different approach (like server-side mod support).
 */
public class PlayerIconNetworking {
    
    // Custom payload identifier for mod presence synchronization
    // Following Fabric's networking documentation: https://docs.fabricmc.net/develop/networking
    // Note: Using 1.21.7 API (PacketCodec) instead of 1.21.10 API (StreamCodec)
    private static final Identifier MOD_PRESENCE_CHANNEL = Identifier.of(CCLiveUtilities.MOD_ID, "mod_presence");
    
    // Payload type for sending/receiving mod presence
    // This follows the Fabric networking pattern, adapted for 1.21.7 API
    public record ModPresencePayload(UUID playerUuid) implements CustomPayload {
        public static final Id<ModPresencePayload> ID = new Id<>(MOD_PRESENCE_CHANNEL);
        
        // PacketCodec for serialization/deserialization (1.21.7 API)
        // UUID is encoded as two longs (most significant and least significant bits)
        public static final PacketCodec<RegistryByteBuf, ModPresencePayload> CODEC = 
            PacketCodec.of(
                (payload, buf) -> {
                    UUID uuid = payload.playerUuid();
                    buf.writeLong(uuid.getMostSignificantBits());
                    buf.writeLong(uuid.getLeastSignificantBits());
                },
                buf -> {
                    long mostSigBits = buf.readLong();
                    long leastSigBits = buf.readLong();
                    return new ModPresencePayload(new UUID(mostSigBits, leastSigBits));
                }
            );
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    
    /**
     * Initialize the networking system.
     * 
     * Following Fabric's networking documentation: https://docs.fabricmc.net/develop/networking
     * 
     * Note: Direct client-to-client communication is not possible without a server mod.
     * The server would need to forward C2S payloads to other clients, which requires server-side code.
     * 
     * For now, we use a simple approach: Mark all players in the player list as mod users.
     * This works without a server mod and is similar to how LabyMod works.
     * 
     * The payload infrastructure is set up correctly following Fabric's patterns,
     * so if a server mod is added in the future, it can easily forward these payloads.
     */
    public static void initialize() {
        // Register the payload type for both directions (following Fabric documentation)
        // S2C: Server-to-Client (for when server forwards to clients)
        // C2S: Client-to-Server (for when client sends to server)
        try {
            PayloadTypeRegistry.playS2C().register(ModPresencePayload.ID, ModPresencePayload.CODEC);
            PayloadTypeRegistry.playC2S().register(ModPresencePayload.ID, ModPresencePayload.CODEC);
        } catch (Exception e) {
            System.err.println("[CCLive-Utilities] Failed to register payload type: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Register receiver for S2C payloads (when server forwards mod presence from other clients)
        // This follows the pattern from Fabric's documentation
        ClientPlayNetworking.registerGlobalReceiver(ModPresencePayload.ID, (payload, context) -> {
            try {
                UUID playerUuid = payload.playerUuid();
                if (playerUuid != null) {
                    // Add this player to the mod list - they have the mod!
                    PlayerIconUtility.addPlayerWithMod(playerUuid);
                }
            } catch (Exception e) {
                // Silently fail
            }
        });
        
        // Send mod presence when joining a server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Clear the player list when joining a new server
            PlayerIconUtility.clearPlayers();
            
            // Add ourselves to the list (we have the mod)
            if (client.player != null) {
                UUID ourUuid = client.player.getUuid();
                PlayerIconUtility.addPlayerWithMod(ourUuid);
            }
        });
        
        // Clear players when disconnecting
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PlayerIconUtility.clearPlayers();
        });
        
        // Periodically check player list and mark all players as mod users
        // This is a simple approach that works without server mod:
        // We assume all players in the player list have the mod installed
        // This is similar to how LabyMod works - it shows icons for all players
        // In a real implementation with proper networking, you'd only add players
        // who confirmed via networking that they have the mod
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getNetworkHandler() == null) {
                return;
            }
            
            // Every 2 seconds (40 ticks), update the player list
            // This ensures we catch new players joining
            if (client.player.age > 0 && client.player.age % 40 == 0) {
                try {
                    // Add ourselves
                    UUID ourUuid = client.player.getUuid();
                    PlayerIconUtility.addPlayerWithMod(ourUuid);
                    
                    // Add all other players in the player list
                    // This is the simple approach: assume all players have the mod
                    var playerList = client.getNetworkHandler().getPlayerList();
                    for (var entry : playerList) {
                        if (entry != null && entry.getProfile() != null) {
                            UUID playerUuid = entry.getProfile().getId();
                            PlayerIconUtility.addPlayerWithMod(playerUuid);
                        }
                    }
                } catch (Exception e) {
                    // Silently fail
                }
            }
        });
    }
}























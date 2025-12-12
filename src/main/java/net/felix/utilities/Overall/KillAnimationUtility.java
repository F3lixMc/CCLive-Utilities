package net.felix.utilities.Overall;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.felix.CCLiveUtilitiesConfig;

public class KillAnimationUtility {
    
    private static boolean isInitialized = false;
    
    public static void initialize() {
        if (isInitialized) {
            return;
        }
        
        try {
            // Client-seitige Events registrieren
            ClientTickEvents.END_CLIENT_TICK.register(KillAnimationUtility::onClientTick);
            
            isInitialized = true;
        } catch (Exception e) {
            // Error initializing KillAnimationUtility
        }
    }
    
    private static void onClientTick(MinecraftClient client) {
        // This method is called every tick to check if the utility should be active
        // The actual disabling of death animations is handled in the mixin
    }
    
    public static boolean isKillAnimationDisabled() {
        if (!CCLiveUtilitiesConfig.HANDLER.instance().enableMod) {
            return false;
        }
        
        if (!CCLiveUtilitiesConfig.HANDLER.instance().killAnimationUtilityEnabled) {
            return false;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return false;
        }
        
        return true;
    }
    
    public static void reset() {
        // Reset state if needed
    }
}


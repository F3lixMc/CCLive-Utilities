package net.felix.utilities.Community.leaderboards.collectors;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.utilities.Community.leaderboards.FactoryLevel;
import net.felix.utilities.Community.leaderboards.FactoryLevelUpload;
import net.felix.utilities.Factory.factorylevel.FactoryLevelPresence;
import net.minecraft.client.Minecraft;

/**
 * Liest das Fabriklevel nur in der Fabrik. Ein fehlender oder ungültiger Wert legt keinen Eintrag an.
 */
public class FactoryLevelCollector implements DataCollector {
    private boolean isActive = false;
    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 20;

    @Override
    public void initialize() {
        if (isActive) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        isActive = true;
    }

    private void onClientTick(Minecraft client) {
        if (!isActive || client == null || client.player == null || client.level == null) {
            return;
        }
        FactoryLevelPresence.observe(
                client.level,
                client.level.dimension().identifier().toString(),
                client.player.getGameProfile().name());
        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;
        if (!net.felix.CCLiveUtilitiesConfig.isDataTrackingEnabled() || !FactoryLevelPresence.inFactory()) {
            return;
        }
        Integer level = FactoryLevel.capture(true, client.player.experienceLevel);
        if (level == null) {
            return;
        }
        FactoryLevelUpload.getInstance().submit(level);
    }

    @Override
    public void shutdown() {
        isActive = false;
    }

    @Override
    public String getName() {
        return "Fabriklevel";
    }

    @Override
    public boolean isActive() {
        return isActive;
    }
}

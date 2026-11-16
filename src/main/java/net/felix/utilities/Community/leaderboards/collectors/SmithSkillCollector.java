package net.felix.utilities.Community.leaderboards.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.felix.utilities.Community.leaderboards.SmithSkillUpload;
import net.felix.utilities.Community.leaderboards.panel.SmithSkill;
import net.felix.utilities.Other.debug.DebugUtility;
import net.felix.utilities.Overall.glyphs.ZeichenUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Liest [Schmiede Skill] aus derselben Tab-Liste wie die NPC-Alerts.
 * Nur eine gültige Wertezeile wird gemerkt. Ein fehlendes Widget lässt den letzten Stand stehen.
 */
public class SmithSkillCollector implements DataCollector {
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
        if (!isActive || client == null || client.player == null || client.getConnection() == null) {
            return;
        }
        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;
        List<String> lines = tabLines(client);
        SmithSkill.Stand stand = SmithSkill.read(lines);
        if (stand == null) {
            DebugUtility.debugPlayerStats("smith-read", SmithSkill.diagnose(lines));
            return;
        }
        if (!net.felix.CCLiveUtilitiesConfig.isDataTrackingEnabled()) {
            DebugUtility.debugPlayerStats("smith-read", SmithSkill.display(stand) + " gelesen, Daten Tracking ist aus");
            return;
        }
        SmithSkillUpload.getInstance().submit(stand.level(), stand.xp());
    }

    static List<String> tabLines(Minecraft client) {
        List<String> lines = new ArrayList<>();
        if (client == null || client.getConnection() == null) {
            return lines;
        }
        for (PlayerInfo entry : client.getConnection().getOnlinePlayers()) {
            if (entry == null) {
                continue;
            }
            String raw = tabText(entry);
            if (raw == null) {
                continue;
            }
            lines.add(ZeichenUtility.decodeTabWidgetText(raw));
        }
        return lines;
    }

    private static String tabText(PlayerInfo entry) {
        Component displayName = entry.getTabListDisplayName();
        if (displayName != null) {
            StringBuilder text = new StringBuilder();
            displayName.visit((style, part) -> {
                text.append(part);
                return Optional.empty();
            }, Style.EMPTY);
            if (!text.isEmpty()) {
                return text.toString();
            }
            return displayName.getString();
        }
        if (entry.getProfile() != null) {
            return entry.getProfile().name();
        }
        return null;
    }

    @Override
    public void shutdown() {
        isActive = false;
    }

    @Override
    public String getName() {
        return "Schmiedeskill";
    }

    @Override
    public boolean isActive() {
        return isActive;
    }
}

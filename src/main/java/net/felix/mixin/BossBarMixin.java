package net.felix.mixin;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.felix.utilities.Aincraft.KillsUtility;
import net.felix.utilities.Factory.WaveUtility;
import net.felix.leaderboards.collectors.FarmworldCollectionsCollector;
import net.felix.utilities.Overall.ZeichenUtility;
import net.felix.utilities.Overall.CoinTrackerUtility;
import net.felix.utilities.Overall.InformationenUtility;
import net.felix.utilities.Overall.NpcAlerts.NpcAlertsUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class BossBarMixin {

    private static volatile Field cachedBossBarsField;
    private static volatile boolean bossBarsFieldLookupFailed;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderBossBar(DrawContext context, CallbackInfo ci) {
        try {
            BossBarHud bossBarHud = (BossBarHud) (Object) this;
            Map<UUID, ClientBossBar> bossBars = resolveBossBars(bossBarHud);
            if (bossBars == null || bossBars.isEmpty()) {
                return;
            }

            NpcAlertsUtility.beginKomboKisteBossBarScan();
            MinecraftClient mc = MinecraftClient.getInstance();
            boolean komboKisteDim = NpcAlertsUtility.isKomboKisteReadingDimension(mc);
            String aincraftBottomFont = ZeichenUtility.getAincraftBottomFont();

            int index = 0;
            for (ClientBossBar bossBar : bossBars.values()) {
                index++;
                String name = bossBar.getName().getString();
                if (komboKisteDim) {
                    NpcAlertsUtility.observeKomboKisteBossBarTitle(name, index);
                }

                CoinTrackerUtility.processBossBar(name);
                net.felix.utilities.DragOverlay.ClipboardCoinCollector.processBossBar(name);
                WaveUtility.processBossBarWave(name, index);

                boolean hasAincraftFont = !aincraftBottomFont.isEmpty()
                        && name.matches(".*[" + aincraftBottomFont + "].*");

                // Kills-Bossbar (Floors) – auch wenn nur chinesische Ziffern sichtbar sind
                if (name.contains("Kills") || name.contains("Kill") || hasAincraftFont) {
                    KillsUtility.processBossBarKills(name);
                }

                // Collection-Bossbar (Farmwelt) – separat, nicht als else-if (gleiche Schrift-Glyphen)
                if (!name.contains("Kills") && !name.contains("Kill") && hasAincraftFont) {
                    FarmworldCollectionsCollector.processBossBarCollection(name);
                    InformationenUtility.processBossBarCollection(name);
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ClientBossBar> resolveBossBars(BossBarHud bossBarHud) {
        try {
            Field field = cachedBossBarsField;
            if (field == null && !bossBarsFieldLookupFailed) {
                field = findBossBarsField(bossBarHud);
                if (field != null) {
                    field.setAccessible(true);
                    cachedBossBarsField = field;
                } else {
                    bossBarsFieldLookupFailed = true;
                    return null;
                }
            }
            if (field == null) {
                return null;
            }
            Object value = field.get(bossBarHud);
            if (!(value instanceof Map<?, ?> map)) {
                cachedBossBarsField = null;
                return null;
            }
            if (!map.isEmpty()) {
                for (Object entry : map.values()) {
                    if (!(entry instanceof ClientBossBar)) {
                        cachedBossBarsField = null;
                        bossBarsFieldLookupFailed = false;
                        return null;
                    }
                }
            }
            return (Map<UUID, ClientBossBar>) map;
        } catch (Exception e) {
            cachedBossBarsField = null;
            return null;
        }
    }

    private static Field findBossBarsField(BossBarHud bossBarHud) {
        Class<?> bossBarHudClass = bossBarHud.getClass();
        Field mapField = null;
        for (Field field : bossBarHudClass.getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(bossBarHud);
                if (value instanceof Map<?, ?> map && !map.isEmpty()) {
                    for (Object entry : map.values()) {
                        if (entry instanceof ClientBossBar) {
                            return field;
                        }
                    }
                }
                if (mapField == null) {
                    mapField = field;
                }
            } catch (Exception ignored) {
                if (mapField == null) {
                    mapField = field;
                }
            }
        }
        if (mapField != null) {
            return mapField;
        }

        String[] possibleFieldNames = {
                "field_2060", "field_2061", "field_2062",
                "bossBars", "bossbars", "bars", "bossBarMap",
                "clientBossBars", "bossBarEntries", "entries", "bossBarList"
        };
        for (String fieldName : possibleFieldNames) {
            try {
                Field field = bossBarHudClass.getDeclaredField(fieldName);
                if (Map.class.isAssignableFrom(field.getType())) {
                    return field;
                }
            } catch (Exception ignored) {
                // Try next name
            }
        }
        return null;
    }
}

package net.felix.mixin;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import net.felix.utilities.Aincraft.cointracker.CoinTrackerUtility;
import net.felix.utilities.Aincraft.kills.KillsUtility;
import net.felix.utilities.Community.leaderboards.collectors.FarmworldCollectionsCollector;
import net.felix.utilities.Factory.wave.WaveUtility;
import net.felix.utilities.Farmworld.collections.tracker.CollectionTracker;
import net.felix.utilities.Overall.glyphs.ZeichenUtility;
import net.felix.utilities.Overall.itemviewer.inventory.HudStatsReader;
import net.felix.utilities.Overall.npcalerts.parser.KomboKisteReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(BossHealthOverlay.class)
public class BossBarMixin {

    private static volatile Field cachedBossBarsField;
    private static volatile boolean bossBarsFieldLookupFailed;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onRenderBossBar(GuiGraphicsExtractor context, CallbackInfo ci) {
        try {
            BossHealthOverlay bossBarHud = (BossHealthOverlay) (Object) this;
            Map<UUID, LerpingBossEvent> bossBars = resolveBossBars(bossBarHud);
            if (bossBars == null || bossBars.isEmpty()) {
                return;
            }

            KomboKisteReader.beginKomboKisteBossBarScan();
            Minecraft mc = Minecraft.getInstance();
            boolean komboKisteDim = KomboKisteReader.isKomboKisteReadingDimension(mc);
            String aincraftBottomFont = ZeichenUtility.getAincraftBottomFont();

            int index = 0;
            for (LerpingBossEvent bossBar : bossBars.values()) {
                index++;
                String name = bossBar.getName().getString();
                if (komboKisteDim) {
                    KomboKisteReader.observeKomboKisteBossBarTitle(name, index);
                }

                CoinTrackerUtility.processBossBar(name);
                HudStatsReader.processBossBar(name);
                net.felix.utilities.Other.clipboard.collector.ClipboardCoinCollector.processBossBar(name);
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
                    CollectionTracker.processBossBarCollection(name);
                }
            }
        } catch (Exception e) {
            // Silent error handling
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, LerpingBossEvent> resolveBossBars(BossHealthOverlay bossBarHud) {
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
                    if (!(entry instanceof LerpingBossEvent)) {
                        cachedBossBarsField = null;
                        bossBarsFieldLookupFailed = false;
                        return null;
                    }
                }
            }
            return (Map<UUID, LerpingBossEvent>) map;
        } catch (Exception e) {
            cachedBossBarsField = null;
            return null;
        }
    }

    private static Field findBossBarsField(BossHealthOverlay bossBarHud) {
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
                        if (entry instanceof LerpingBossEvent) {
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

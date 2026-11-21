package net.felix.utilities.DragOverlay;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Aincraft.blueprints.viewer.BlueprintViewerDraggableOverlay;
import net.felix.utilities.Aincraft.cardsstatues.overlay.CardsDraggableOverlay;
import net.felix.utilities.Aincraft.cardsstatues.overlay.StatuesDraggableOverlay;
import net.felix.utilities.Aincraft.cointracker.CoinTrackerDisplayMode;
import net.felix.utilities.Aincraft.cointracker.CoinTrackerDraggableOverlay;
import net.felix.utilities.Factory.bosshp.BossHPDraggableOverlay;
import net.felix.utilities.Factory.factorylevel.FactoryLevelDraggableOverlay;
import net.felix.utilities.Farmworld.collections.overlay.CollectionDraggableOverlay;
import net.felix.utilities.Farmworld.collections.tracker.ResourceTrackerDisplayMode;
import net.felix.utilities.Farmworld.miningxp.overlay.MiningLumberjackDraggableOverlay;
import net.felix.utilities.Other.hud.RateDisplayUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Detailbuttons der Tracker, deren Config-Optionen im F6-Menü liegen.
 * Die Aktivieren-Checkbox bleibt die Zeile in der Overlay-Liste.
 */
public final class TrackerDetailSettings {
    private TrackerDetailSettings() {
    }

    public record Button(String label, boolean enabled, Runnable onClick, Runnable onRightClick) {
        public Button(String label, boolean enabled, Runnable onClick) {
            this(label, enabled, onClick, null);
        }
    }

    public static boolean supports(DraggableOverlay overlay) {
        return title(overlay) != null;
    }

    public static String title(DraggableOverlay overlay) {
        if (overlay instanceof CoinTrackerDraggableOverlay) {
            return "Coin Tracker";
        }
        if (overlay instanceof CardsDraggableOverlay) {
            return "Cards";
        }
        if (overlay instanceof StatuesDraggableOverlay) {
            return "Statues";
        }
        if (overlay instanceof BlueprintViewerDraggableOverlay) {
            return "Blueprint Viewer";
        }
        if (overlay instanceof BossHPDraggableOverlay) {
            return "BossHP";
        }
        if (overlay instanceof FactoryLevelDraggableOverlay) {
            return "Fabriklevel Progress";
        }
        if (overlay instanceof MiningLumberjackDraggableOverlay) {
            return "Mining / Holzfäller";
        }
        if (overlay instanceof CollectionDraggableOverlay) {
            return "Collection";
        }
        return null;
    }

    public static List<Button> buttons(DraggableOverlay overlay, Screen returnTo) {
        CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
        List<Button> buttons = new ArrayList<>();
        if (overlay instanceof CoinTrackerDraggableOverlay) {
            RateDisplayUnit coinUnit = RateDisplayUnit.orDefault(config.coinTrackerRateUnit);
            buttons.add(rateUnitToggle(
                    () -> config.coinTrackerRateUnit,
                    value -> config.coinTrackerRateUnit = value));
            buttons.add(toggle(coinUnit.abbreviated("CPM") + ": "
                    + (config.coinTrackerDisplayMode == CoinTrackerDisplayMode.SCOREBOARD
                    ? "Scoreboard" : "Overlay"), () -> config.coinTrackerDisplayMode =
                    config.coinTrackerDisplayMode == CoinTrackerDisplayMode.SCOREBOARD
                            ? CoinTrackerDisplayMode.OVERLAY
                            : CoinTrackerDisplayMode.SCOREBOARD));
            buttons.add(toggle("Hintergrund: " + onOff(config.coinTrackerShowBackground),
                    () -> config.coinTrackerShowBackground = !config.coinTrackerShowBackground));
            buttons.add(colors(returnTo, "Coin Tracker Farben", List.of(
                    color("Überschriftenfarbe", "Farbe für die Überschrift im Coin Tracker", new Color(0xFFFFFF00),
                            () -> config.coinTrackerHeaderColor, value -> config.coinTrackerHeaderColor = value),
                    color("Textfarbe", "Farbe für den Text im Coin Tracker", new Color(0xE6FFFFFF),
                            () -> config.coinTrackerTextColor, value -> config.coinTrackerTextColor = value))));
        } else if (overlay instanceof CardsDraggableOverlay) {
            buttons.add(toggle("Overlay-Typ: " + overlayTypeLabel(config.cardOverlayType),
                    () -> config.cardOverlayType = nextOverlayType(config.cardOverlayType)));
            buttons.add(textScale(scaleLabel(config.cardTextScale), config.cardTextScale,
                    value -> config.cardTextScale = value));
        } else if (overlay instanceof StatuesDraggableOverlay) {
            buttons.add(toggle("Overlay-Typ: " + overlayTypeLabel(config.statueOverlayType),
                    () -> config.statueOverlayType = nextOverlayType(config.statueOverlayType)));
            buttons.add(textScale(scaleLabel(config.statueTextScale), config.statueTextScale,
                    value -> config.statueTextScale = value));
        } else if (overlay instanceof BlueprintViewerDraggableOverlay) {
            buttons.add(toggle("Aktivieren: " + onOff(config.blueprintViewerEnabled),
                    () -> {
                        config.blueprintViewerEnabled = !config.blueprintViewerEnabled;
                        net.felix.utilities.Aincraft.blueprints.viewer.BPViewerUtility.resetHotkeyVisibility();
                    }));
            buttons.add(toggle("Overlay-Typ: " + overlayTypeLabel(config.blueprintViewerOverlayType),
                    () -> config.blueprintViewerOverlayType = nextOverlayType(config.blueprintViewerOverlayType)));
            buttons.add(toggle("Missing Mode: " + onOff(config.blueprintViewerMissingMode),
                    () -> config.blueprintViewerMissingMode = !config.blueprintViewerMissingMode));
            buttons.add(toggle("Haken im Moblexicon: " + onOff(config.moblexiconBlueprintCheckmarksEnabled),
                    () -> config.moblexiconBlueprintCheckmarksEnabled = !config.moblexiconBlueprintCheckmarksEnabled));
        } else if (overlay instanceof BossHPDraggableOverlay) {
            RateDisplayUnit bossUnit = RateDisplayUnit.orDefault(config.bossHPRateUnit);
            buttons.add(rateUnitToggle(
                    () -> config.bossHPRateUnit,
                    value -> config.bossHPRateUnit = value));
            buttons.add(toggle("Hintergrund: " + onOff(config.bossHPShowBackground),
                    () -> config.bossHPShowBackground = !config.bossHPShowBackground));
            buttons.add(toggle(bossUnit.abbreviated("DPM") + ": " + onOff(config.bossHPShowDPM),
                    () -> config.bossHPShowDPM = !config.bossHPShowDPM));
            buttons.add(toggle("Last Dmg: " + onOff(config.bossHPShowLastDmg),
                    () -> config.bossHPShowLastDmg = !config.bossHPShowLastDmg));
            buttons.add(toggle("Gesamtschaden: " + onOff(config.bossHPShowOverallDmg),
                    () -> config.bossHPShowOverallDmg = !config.bossHPShowOverallDmg));
            buttons.add(toggle("Restzeit: " + onOff(config.bossHPShowEta),
                    () -> config.bossHPShowEta = !config.bossHPShowEta));
            buttons.add(colors(returnTo, "BossHP Farben", List.of(
                    color("Textfarbe", "Farbe für den Text in der Boss-HP Anzeige", new Color(0xFFFFFFFF),
                            () -> config.bossHPTextColor, value -> config.bossHPTextColor = value))));
        } else if (overlay instanceof FactoryLevelDraggableOverlay) {
            buttons.add(toggle("Hintergrund: " + onOff(config.factoryLevelShowBackground),
                    () -> config.factoryLevelShowBackground = !config.factoryLevelShowBackground));
        } else if (overlay instanceof MiningLumberjackDraggableOverlay) {
            buttons.add(rateUnitToggle(
                    () -> config.miningLumberjackRateUnit,
                    value -> config.miningLumberjackRateUnit = value));
            buttons.add(colors(returnTo, "Mining / Holzfäller Farben", List.of(
                    color("Überschriftenfarbe", "Farbe für die Überschriften (Bergbau/Holzfäller)", new Color(0xFFFFFF00),
                            () -> config.miningLumberjackOverlayHeaderColor,
                            value -> config.miningLumberjackOverlayHeaderColor = value),
                    color("Textfarbe", "Farbe für den normalen Text im Holzfäller/Bergbau Overlay", new Color(0xFFFFFFFF),
                            () -> config.miningLumberjackOverlayTextColor,
                            value -> config.miningLumberjackOverlayTextColor = value))));
        } else if (overlay instanceof CollectionDraggableOverlay) {
            RateDisplayUnit resourceUnit = RateDisplayUnit.orDefault(config.resourceTrackerRateUnit);
            buttons.add(rateUnitToggle(
                    () -> config.resourceTrackerRateUnit,
                    value -> config.resourceTrackerRateUnit = value));
            buttons.add(toggle("Benötigte Blöcke: " + onOff(config.collectionOverlayShowBlocksNeeded),
                    () -> config.collectionOverlayShowBlocksNeeded = !config.collectionOverlayShowBlocksNeeded));
            buttons.add(toggle("Benötigte Zeit: " + onOff(config.collectionOverlayShowTimeToNext),
                    () -> config.collectionOverlayShowTimeToNext = !config.collectionOverlayShowTimeToNext));
            buttons.add(toggle(resourceUnit.withBase("Ressourcen") + ": " + onOff(config.resourceTrackerRateEnabled),
                    () -> config.resourceTrackerRateEnabled = !config.resourceTrackerRateEnabled));
            buttons.add(new Button(
                    "Anzeige: " + (config.resourceTrackerDisplayMode == ResourceTrackerDisplayMode.SCOREBOARD
                            ? "Scoreboard" : "Overlay"),
                    config.resourceTrackerRateEnabled,
                    () -> {
                        config.resourceTrackerDisplayMode =
                                config.resourceTrackerDisplayMode == ResourceTrackerDisplayMode.SCOREBOARD
                                        ? ResourceTrackerDisplayMode.OVERLAY
                                        : ResourceTrackerDisplayMode.SCOREBOARD;
                        CCLiveUtilitiesConfig.HANDLER.save();
                    }));
            buttons.add(colors(returnTo, "Collection Farben", List.of(
                    color("Überschriftenfarbe", "Farbe für die Überschrift (Collection:)", new Color(0xFFFFFF00),
                            () -> config.collectionOverlayHeaderColor,
                            value -> config.collectionOverlayHeaderColor = value),
                    color("Textfarbe", "Farbe für den normalen Text im Collection Overlay", new Color(0xFFFFFFFF),
                            () -> config.collectionOverlayTextColor,
                            value -> config.collectionOverlayTextColor = value))));
        }
        return buttons;
    }

    private static Button toggle(String label, Runnable change) {
        return new Button(label, true, () -> {
            change.run();
            CCLiveUtilitiesConfig.HANDLER.save();
        });
    }

    private static Button rateUnitToggle(
            java.util.function.Supplier<RateDisplayUnit> getter,
            java.util.function.Consumer<RateDisplayUnit> setter) {
        RateDisplayUnit unit = RateDisplayUnit.orDefault(getter.get());
        return new Button(unit.settingsSwitcherLabel(), true, () -> {
            setter.accept(RateDisplayUnit.orDefault(getter.get()).toggled());
            CCLiveUtilitiesConfig.HANDLER.save();
        });
    }

    private static Button colors(Screen returnTo, String title, List<TrackerColorScreen.ColorOption> options) {
        return new Button("Farben", true, () -> {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                client.gui.setScreen(TrackerColorScreen.create(returnTo, title, options));
            }
        });
    }

    private static TrackerColorScreen.ColorOption color(String name, String description, Color defaultValue,
            java.util.function.Supplier<Color> getter, java.util.function.Consumer<Color> setter) {
        return new TrackerColorScreen.ColorOption(name, description, defaultValue, getter, setter);
    }

    private static String onOff(boolean enabled) {
        return enabled ? "An" : "Aus";
    }

    private static String overlayTypeLabel(OverlayType type) {
        return switch (type) {
            case CUSTOM -> "Bild-Overlay";
            case BLACK -> "Schwarzes Overlay";
            case NONE -> "Kein Hintergrund";
        };
    }

    private static OverlayType nextOverlayType(OverlayType type) {
        return switch (type) {
            case CUSTOM -> OverlayType.BLACK;
            case BLACK -> OverlayType.NONE;
            case NONE -> OverlayType.CUSTOM;
        };
    }

    private static String scaleLabel(float scale) {
        return String.format(Locale.US, "Text-Größe: %.1f", scale);
    }

    private static Button textScale(String label, float scale, java.util.function.Consumer<Float> apply) {
        return new Button(label, true,
                () -> {
                    apply.accept(stepTextScale(scale, 1));
                    CCLiveUtilitiesConfig.HANDLER.save();
                },
                () -> {
                    apply.accept(stepTextScale(scale, -1));
                    CCLiveUtilitiesConfig.HANDLER.save();
                });
    }

    private static float stepTextScale(float scale, int direction) {
        float next = Math.round((scale + direction * 0.1f) * 10f) / 10f;
        if (next < 1.0f) {
            return 1.0f;
        }
        if (next > 1.5f) {
            return 1.5f;
        }
        return next;
    }
}

package net.felix.utilities.Aincraft.kills;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import java.awt.Color;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Farbauswahl für Überschrift und Text des Kill Trackers.
 */
public final class KillTrackerColors {
    private KillTrackerColors() {
    }

    public static Screen createScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Kill Tracker Farben"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Farben"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Farben"))
                                .option(colorOption(
                                        "Überschriftenfarbe",
                                        "Farbe für die Überschrift in der Kills-Anzeige",
                                        new Color(0xFFFFFF00),
                                        () -> CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityHeaderColor,
                                        value -> CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityHeaderColor = value))
                                .option(colorOption(
                                        "Textfarbe",
                                        "Farbe für den Text in der Kills-Anzeige",
                                        new Color(0xE6FFFFFF),
                                        () -> CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityTextColor,
                                        value -> CCLiveUtilitiesConfig.HANDLER.instance().killsUtilityTextColor = value))
                                .build())
                        .build())
                .save(CCLiveUtilitiesConfig.HANDLER::save)
                .build()
                .generateScreen(parent);
    }

    private static Option<Color> colorOption(String name, String description, Color defaultValue,
            java.util.function.Supplier<Color> getter, java.util.function.Consumer<Color> setter) {
        return Option.<Color>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .binding(defaultValue, getter, setter)
                .controller(ColorControllerBuilder::create)
                .build();
    }
}

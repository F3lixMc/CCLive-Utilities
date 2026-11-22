package net.felix.utilities.DragOverlay.detail;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * YACL-Farbauswahl für Tracker-Detailsettings.
 */
public final class TrackerColorScreen {
    private TrackerColorScreen() {
    }

    public record ColorOption(String name, String description, Color defaultValue,
            Supplier<Color> getter, Consumer<Color> setter) {
    }

    public static Screen create(Screen parent, String title, List<ColorOption> options) {
        OptionGroup.Builder group = OptionGroup.createBuilder().name(Component.literal("Farben"));
        for (ColorOption option : options) {
            group.option(Option.<Color>createBuilder()
                    .name(Component.literal(option.name()))
                    .description(OptionDescription.of(Component.literal(option.description())))
                    .binding(option.defaultValue(), option.getter(), option.setter())
                    .controller(ColorControllerBuilder::create)
                    .build());
        }
        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal(title))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Farben"))
                        .group(group.build())
                        .build())
                .save(CCLiveUtilitiesConfig.HANDLER::save)
                .build()
                .generateScreen(parent);
    }
}

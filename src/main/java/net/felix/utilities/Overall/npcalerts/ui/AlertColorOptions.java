package net.felix.utilities.Overall.npcalerts.ui;

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
 * YACL-Farboptionen (Text- und Prozentfarbe) eines NPC-Alerts.
 */
public final class AlertColorOptions {

    private AlertColorOptions() {
    }

    /**
     * Erstellt einen YACL-Screen für die Farboptionen eines NPC Alerts-Eintrags
     * Enthält sowohl Textfarbe als auch Prozentfarbe
     */
    private static OptionGroup buildColorOptionGroup(String configKey) {
        var b = OptionGroup.createBuilder()
                .name(Component.literal("Farben"))
                .option(createColorOption(configKey, true, "Textfarbe", "Farbe für den Text"));
        if (!"komboKiste".equals(configKey)) {
            b.option(createColorOption(configKey, false, "Prozentfarbe", "Farbe für die Prozentwerte"));
        }
        return b.build();
    }

    static Screen createColorConfigScreen(Screen parent, String configKey) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("NPC Alerts Farben"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Farben"))
                        .group(buildColorOptionGroup(configKey))
                        .build())
                .save(() -> {
                    CCLiveUtilitiesConfig.HANDLER.save();
                })
                .build()
                .generateScreen(parent);
    }

    /**
     * Erstellt eine Color-Option für einen NPC Alerts-Eintrag
     */
    private static Option<Color> createColorOption(String configKey, boolean isTextColor, String name, String description) {
        Color defaultValue = isTextColor ? new Color(0xFFFFFFFF) : new Color(0xFFFFFF00);
        
        // Getter und Setter basierend auf configKey
        java.util.function.Supplier<Color> getter;
        java.util.function.Consumer<Color> setter;
        
        if (isTextColor) {
            switch (configKey) {
                case "forschung":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungTextColor = val;
                    break;
                case "amboss":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossTextColor = val;
                    break;
                case "schmelzofen":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenTextColor = val;
                    break;
                case "jaeger":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerTextColor = val;
                    break;
                case "komboKiste":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsKomboKisteTextColor = val;
                    break;
                case "seelen":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenTextColor = val;
                    break;
                case "essenzen":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenTextColor = val;
                    break;
                case "machtkristalle":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleTextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristalleTextColor = val;
                    break;
                case "recycler":
                    // Für "recycler" verwende Slot 1 als Standard (für Multi-Line-Overlay)
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1TextColor;
                    setter = (val) -> {
                        // Setze alle drei Slots gleichzeitig
                        CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1TextColor = val;
                        CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2TextColor = val;
                        CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3TextColor = val;
                    };
                    break;
                case "recyclerSlot1":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1TextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1TextColor = val;
                    break;
                case "recyclerSlot2":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2TextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2TextColor = val;
                    break;
                case "recyclerSlot3":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3TextColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3TextColor = val;
                    break;
                default:
                    getter = () -> defaultValue;
                    setter = (val) -> {};
            }
        } else {
            switch (configKey) {
                case "forschung":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungPercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsForschungPercentColor = val;
                    break;
                case "amboss":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossPercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsAmbossPercentColor = val;
                    break;
                case "schmelzofen":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenPercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSchmelzofenPercentColor = val;
                    break;
                case "jaeger":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerPercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsJaegerPercentColor = val;
                    break;
                case "seelen":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenPercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsSeelenPercentColor = val;
                    break;
                case "essenzen":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenPercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsEssenzenPercentColor = val;
                    break;
                case "machtkristalle":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristallePercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristallePercentColor = val;
                    break;
                case "recycler":
                    // Für "recycler" verwende Slot 1 als Standard (für Multi-Line-Overlay)
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1PercentColor;
                    setter = (val) -> {
                        // Setze alle drei Slots gleichzeitig
                        CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1PercentColor = val;
                        CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2PercentColor = val;
                        CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3PercentColor = val;
                    };
                    break;
                case "recyclerSlot1":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1PercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot1PercentColor = val;
                    break;
                case "recyclerSlot2":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2PercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot2PercentColor = val;
                    break;
                case "recyclerSlot3":
                    getter = () -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3PercentColor;
                    setter = (val) -> CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsRecyclerSlot3PercentColor = val;
                    break;
                default:
                    getter = () -> defaultValue;
                    setter = (val) -> {};
            }
        }
        
        return Option.<Color>createBuilder()
                .name(Component.literal(name))
                .description(OptionDescription.of(Component.literal(description)))
                .binding(defaultValue, getter, setter)
                .controller(ColorControllerBuilder::create)
                .build();
    }
}

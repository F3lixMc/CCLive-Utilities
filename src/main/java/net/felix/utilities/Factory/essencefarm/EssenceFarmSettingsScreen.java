package net.felix.utilities.Factory.essencefarm;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import java.util.List;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings of the essence farm planner: target level per power crystal, drop chance, auto detection.
 * Opened from the F6 detail menu; the groups are also part of the "Fabrik" category of the main config screen.
 */
public final class EssenceFarmSettingsScreen {

	private static final int MAX_LEVEL = 1000;

	private EssenceFarmSettingsScreen() {
	}

	public static Screen create(Screen parent) {
		return YetAnotherConfigLib.createBuilder()
				.title(Component.literal("Essenzen-Farmplan"))
				.category(category())
				.save(EssenceFarmSettingsScreen::save)
				.build()
				.generateScreen(parent);
	}

	/** Built fresh on every open so the current crystal levels are the minimum of each field. */
	private static ConfigCategory category() {
		return ConfigCategory.createBuilder()
				.name(Component.literal("Essenzen-Farmplan"))
				.groups(groups())
				.build();
	}

	private static void save() {
		EssenceFarmCrystals.save();
		CCLiveUtilitiesConfig.HANDLER.save();
		EssenceFarmTracker.invalidate();
	}

	/** Option groups, built fresh so the current crystal levels are the minimum of each target field. */
	public static List<OptionGroup> groups() {
		EssenceFarmCrystals.load();
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();

		OptionGroup.Builder crystals = OptionGroup.createBuilder()
				.name(Component.literal("Essenzen-Farmplan: Ziellevel"))
				.description(OptionDescription.of(Component.literal(
						"Ziellevel je Machtkristall. Gleich dem aktuellen Level = nicht planen. Das aktuelle Level "
								+ "kommt aus dem Menü \"Machtkristalle Verbessern\" oder der Tab-Liste.")));
		for (String name : EssenceFarmCrystals.names()) {
			EssenceFarmCrystals.Crystal crystal = EssenceFarmCrystals.get(name);
			int current = crystal.level;
			int min = Math.max(1, current);
			String levelText = current > 0 ? "aktuell " + current : "Level unbekannt";
			crystals.option(Option.<Integer>createBuilder()
					.name(Component.literal(name + " (" + levelText + ")"))
					.description(OptionDescription.of(Component.literal(
							"Ziellevel für den Machtkristall " + name + ". Kann nicht unter dem aktuellen Level liegen.")))
					.binding(min,
							() -> Math.max(min, crystal.target > 0 ? crystal.target : min),
							value -> {
								EssenceFarmCrystals.setTarget(name, value);
								EssenceFarmCrystals.save();
							})
					.controller(opt -> IntegerFieldControllerBuilder.create(opt).min(min).max(MAX_LEVEL))
					.build());
		}

		OptionGroup dropChance = OptionGroup.createBuilder()
				.name(Component.literal("Essenzen-Farmplan: Dropchance"))
				.option(Option.<Boolean>createBuilder()
						.name(Component.literal("Automatisch aus Stats-Übersicht"))
						.description(OptionDescription.of(Component.literal(
								"Übernimmt \"Essn Drp Chc\" beim Öffnen der Stats-Übersicht im Kompass. "
										+ "Ausschalten, um den Wert unten fest einzutragen.")))
						.binding(true, () -> config.essenceFarmAutoDropChance, value -> config.essenceFarmAutoDropChance = value)
						.controller(TickBoxControllerBuilder::create)
						.build())
				.option(Option.<Double>createBuilder()
						.name(Component.literal("Dropchance in %"))
						.description(OptionDescription.of(Component.literal(
								"Wert wie in der Stats-Übersicht: 409 bedeutet im Schnitt 4,09 Essenzen pro Mob.")))
						.binding(0.0,
								() -> Math.max(0.0, config.essenceFarmDropChance),
								value -> {
									// Saving with the untouched 0 must not turn "unknown" into 0 %.
									if (!(config.essenceFarmDropChance < 0 && value == 0.0)) {
										config.essenceFarmDropChance = value;
									}
								})
						.controller(opt -> DoubleFieldControllerBuilder.create(opt).min(0.0).max(1_000_000.0))
						.build())
				.build();

		OptionGroup display = OptionGroup.createBuilder()
				.name(Component.literal("Essenzen-Farmplan: Anzeige"))
				.option(Option.<Boolean>createBuilder()
						.name(Component.literal("Overlay anzeigen"))
						.binding(true, () -> config.essenceFarmEnabled && config.showEssenceFarm, value -> {
							config.essenceFarmEnabled = value;
							config.showEssenceFarm = value;
						})
						.controller(TickBoxControllerBuilder::create)
						.build())
				.option(Option.<Boolean>createBuilder()
						.name(Component.literal("Hintergrund"))
						.binding(true, () -> config.essenceFarmShowBackground, value -> config.essenceFarmShowBackground = value)
						.controller(TickBoxControllerBuilder::create)
						.build())
				.option(Option.<Boolean>createBuilder()
						.name(Component.literal("Debug-Infos"))
						.description(OptionDescription.of(Component.literal(
								"Zeigt Fabrik-Erkennung, Bossbar-Werte, Kills und HUD-Status im Overlay (zur Fehlersuche).")))
						.binding(false, () -> config.essenceFarmDebug, value -> config.essenceFarmDebug = value)
						.controller(TickBoxControllerBuilder::create)
						.build())
				.build();

		return List.of(crystals.build(), dropChance, display);
	}
}

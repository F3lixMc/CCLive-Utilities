package net.felix.utilities.Factory.essencefarm;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings of the essence farm planner: target level per power crystal, drop chance, auto detection.
 * Opened from the F6 detail menu; the groups are also part of the "Fabrik" category of the main config screen.
 */
public final class EssenceFarmSettingsScreen {

	private static final int MAX_LEVEL = 1000;
	private static final double MAX_DROP_CHANCE = 1_000_000;

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

	/** Built fresh on every open so each label shows the crystal level from the current session. */
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

	/** Option groups, built fresh so each label shows the crystal level from the current session. */
	public static List<OptionGroup> groups() {
		EssenceFarmCrystals.load();
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();

		OptionGroup.Builder crystals = OptionGroup.createBuilder()
				.name(Component.literal("Essenzen-Farmplan: Ziellevel"))
				.description(OptionDescription.of(Component.literal(
						"Ziellevel je Machtkristall. 0 = nicht planen. Das aktuelle Level kommt aus dem Menü "
								+ "\"Machtkristalle Verbessern\" oder der Tab-Liste.")));
		for (String name : EssenceFarmCrystals.names()) {
			EssenceFarmCrystals.Crystal crystal = EssenceFarmCrystals.get(name);
			int current = crystal.level;
			String shown = Integer.toString(Math.max(0, crystal.target));
			AtomicReference<String> text = new AtomicReference<>(shown);
			String levelText = current > 0 ? "aktuell " + current : "Level unbekannt";
			crystals.option(Option.<String>createBuilder()
					.name(Component.literal(name + " (" + levelText + ")"))
					.description(OptionDescription.of(Component.literal(
							"Ziellevel für den Machtkristall " + name + ". 0 = nicht planen. "
									+ "Liegt das Ziel nicht über dem aktuellen Level, überspringt der Plan den Kristall.")))
					.binding(shown, text::get, value -> rememberTarget(name, text, value))
					.customController(opt -> EssenceFarmNumberController.whole(opt, MAX_LEVEL))
					.build());
		}

		String dropShown = EssenceFarmNumberController.formatDropChance(config.essenceFarmDropChance);
		AtomicReference<String> dropText = new AtomicReference<>(dropShown);
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
				.option(Option.<String>createBuilder()
						.name(Component.literal("Dropchance in %"))
						.description(OptionDescription.of(Component.literal(
								"Wert wie in der Stats-Übersicht: 409 bedeutet im Schnitt 4,09 Essenzen pro Mob. "
										+ "Komma und Punkt sind erlaubt.")))
						.binding(dropShown, dropText::get, value -> rememberDropChance(config, dropText, value))
						.customController(opt -> EssenceFarmNumberController.decimal(opt, MAX_DROP_CHANCE))
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

	private static void rememberTarget(String name, AtomicReference<String> text, String value) {
		int parsed = EssenceFarmNumberController.parseWhole(value, MAX_LEVEL);
		if (parsed < 0) {
			return;
		}
		text.set(value);
		EssenceFarmCrystals.setTarget(name, parsed);
	}

	private static void rememberDropChance(CCLiveUtilitiesConfig config, AtomicReference<String> text, String value) {
		double parsed = EssenceFarmNumberController.parseDecimal(value, MAX_DROP_CHANCE);
		if (Double.isNaN(parsed)) {
			return;
		}
		text.set(value);
		// Leaving the untouched placeholder 0 must not turn "unknown" into 0 %.
		if (!(config.essenceFarmDropChance < 0 && parsed == 0)) {
			config.essenceFarmDropChance = parsed;
		}
	}
}

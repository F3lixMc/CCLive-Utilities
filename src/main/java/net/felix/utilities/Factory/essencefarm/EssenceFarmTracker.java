package net.felix.utilities.Factory.essencefarm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Factory.factorylevel.FactoryLevelTracker;
import net.felix.utilities.Factory.mklevel.data.MkLevelDatabase;
import net.felix.utilities.Factory.mklevel.data.MkLevelInfo;
import net.felix.utilities.Factory.wave.WaveUtility;
import net.felix.utilities.Other.clipboard.storage.CollectedMaterialsResourcesStorage;
import net.felix.utilities.Other.glyphs.ZeichenUtility;
import net.felix.utilities.Other.hud.HudOverlayVisibility;
import net.felix.utilities.Other.keybinding.KeyBindingUtility;
import net.felix.utilities.Other.util.NumberFormatting;
import net.felix.utilities.Overall.npcalerts.data.MachtkristallSlot;
import net.felix.utilities.Overall.npcalerts.data.NpcAlertState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Essence farm planner: counts wave mobs live, estimates essences gained since the last essence bag
 * snapshot and shows which waves to farm (and how often) for the selected power crystals.
 */
public final class EssenceFarmTracker {

	public static final int PADDING = 4;
	public static final int LINE_HEIGHT = 11;
	public static final int MIN_WIDTH = 140;

	private static final int TITLE_COLOR = 0xFFFFFF55;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int DIM_COLOR = 0xFFAAAAAA;
	private static final int ACTIVE_COLOR = 0xFF55FF55;
	private static final int DONE_COLOR = 0xFF55FF55;
	private static final int WARN_COLOR = 0xFFFF5555;

	/** The server repeats the completion action bar while it is shown; ignore repeats within this window. */
	private static final long COMPLETION_REPEAT_MS = 3000L;
	/** A longer gap between two completions is a break, not one wave cycle. */
	private static final long MAX_CYCLE_MS = 15L * 60L * 1000L;
	/** "+824,558t XP +287,161bh Seelen": shown in the action bar when a wave is completed. */
	private static final Pattern WAVE_COMPLETED = Pattern.compile("^\\+\\S+\\s+XP\\s+\\+\\S+\\s+Seelen$");
	/** Runs starting this soon after entering the level may have missed spawns. */
	private static final int ENTER_GRACE_TICKS = 40;
	private static final long PLAN_INTERVAL_MS = 1000L;
	private static final int CRYSTAL_MENU_SCAN_INTERVAL = 10;
	private static final Logger LOGGER = LoggerFactory.getLogger("cclive-utilities");

	private record Line(String text, int color) {
	}

	// Live run state
	private static final Map<Integer, String> aliveMobs = new HashMap<>();
	private static final Set<Integer> countedKills = new HashSet<>();
	private static final Map<String, Set<Integer>> runSpawns = new HashMap<>();
	private static boolean runActive;
	private static boolean runComplete;
	private static int runWave = -1;
	private static long runStartMs;
	private static long lastCompletionAt;
	private static String lastCompletionText;
	private static int ticksSinceLevelChange;

	// Essences gained since the last bag snapshot, by bag key
	private static final Map<String, Double> gained = new HashMap<>();
	private static final Map<String, Long> lastSeenStock = new HashMap<>();

	private static boolean initialized;
	private static long lastPlanAt;
	private static String lastStatsTitle;
	private static int crystalMenuCooldown;

	// Diagnostics shown when "Debug-Infos" is on
	private static long hudCalls;
	private static long hudDrawn;
	private static String hudSkipReason = "-";
	private static String lastError;
	private static int completions;
	private static long kills;
	private static double gainedTotal;
	private static List<Line> lines = List.of();
	private static int cachedWidth = MIN_WIDTH;
	private static int cachedHeight = PADDING * 2 + LINE_HEIGHT;

	private EssenceFarmTracker() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		EssenceFarmData.load();
		EssenceFarmCrystals.load();
		ClientEntityEvents.ENTITY_LOAD.register(EssenceFarmTracker::onEntityLoad);
		ClientEntityEvents.ENTITY_UNLOAD.register(EssenceFarmTracker::onEntityUnload);
		ClientTickEvents.END_CLIENT_TICK.register(EssenceFarmTracker::onClientTick);
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> resetRun());
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath("cclive-utilities", "essence_farm"),
				EssenceFarmTracker::onHudRender);
	}

	/** Overlay and live counting only run in the factory. */
	public static boolean isInFactory() {
		return FactoryLevelTracker.isInFactory();
	}

	public static int unscaledWidth() {
		return cachedWidth;
	}

	public static int unscaledHeight() {
		return cachedHeight;
	}

	public static int x() {
		return CCLiveUtilitiesConfig.HANDLER.instance().essenceFarmX;
	}

	public static int y() {
		return CCLiveUtilitiesConfig.HANDLER.instance().essenceFarmY;
	}

	public static float scale() {
		float scale = CCLiveUtilitiesConfig.HANDLER.instance().essenceFarmScale;
		return scale <= 0f ? 1.0f : scale;
	}

	/** Forces a fresh plan on the next tick (after a selection change in F6). */
	public static void invalidate() {
		lastPlanAt = 0L;
	}

	// ---- live mob counting ----

	private static String waveMobType(Entity entity) {
		if (!(entity instanceof LivingEntity) || entity instanceof Player) {
			return null;
		}
		String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
		return EssenceFarmData.tiers().knows(type) ? type : null;
	}

	private static void onEntityLoad(Entity entity, ClientLevel level) {
		if (!isInFactory()) {
			return;
		}
		String type = waveMobType(entity);
		if (type == null) {
			return;
		}
		if (!runActive) {
			startRun(ticksSinceLevelChange >= ENTER_GRACE_TICKS && aliveMobs.isEmpty());
		}
		aliveMobs.put(entity.getId(), type);
		runSpawns.computeIfAbsent(type, t -> new HashSet<>()).add(entity.getId());
	}

	private static void onEntityUnload(Entity entity, ClientLevel level) {
		String type = aliveMobs.remove(entity.getId());
		if (type == null) {
			return;
		}
		if (entity instanceof LivingEntity living && (living.isDeadOrDying() || living.getHealth() <= 0)) {
			countKill(entity.getId(), type);
		} else if (aliveMobs.isEmpty() && runActive) {
			// The last mob vanished without dying (waves stopped, or out of range): this run is incomplete,
			// and the pause must not count as one wave cycle.
			abortRun();
			lastCompletionAt = 0L;
		}
		countedKills.remove(entity.getId());
	}

	private static void countKill(int entityId, String type) {
		if (!countedKills.add(entityId)) {
			return;
		}
		int wave = WaveUtility.getCurrentWave();
		int tier = EssenceFarmData.tiers().tierAt(type, wave);
		String bagKey = tier > 0 ? EssenceFarmData.tiers().bagKey(type, tier) : null;
		kills++;
		if (bagKey != null) {
			gained.merge(bagKey, dropMultiplier(), Double::sum);
			gainedTotal += dropMultiplier();
		}
	}

	private static void startRun(boolean complete) {
		runActive = true;
		runComplete = complete;
		runWave = -1;
		runStartMs = System.currentTimeMillis();
		runSpawns.clear();
	}

	/**
	 * Called from the action bar mixin. A wave completion ends the current run; the time between two
	 * completions is one full wave cycle (spawn delay included), which is what farming really costs.
	 */
	public static void onActionBar(String text) {
		if (text == null || !isInFactory()) {
			return;
		}
		String clean = text.replaceAll("§.", "").trim();
		if (!isWaveCompletedMessage(clean)) {
			return;
		}
		long now = System.currentTimeMillis();
		if (clean.equals(lastCompletionText) && now - lastCompletionAt < COMPLETION_REPEAT_MS) {
			return;
		}
		long cycle = lastCompletionAt > 0 && now - lastCompletionAt < MAX_CYCLE_MS ? now - lastCompletionAt : 0L;
		lastCompletionAt = now;
		lastCompletionText = clean;
		completions++;
		// The boss bar still shows the completed wave at this moment.
		runWave = WaveUtility.getCurrentWave();
		finishRun(cycle);
		// Every mob of the wave is gone now; entries left over (missed unload events) must not leak into the next run.
		aliveMobs.clear();
		countedKills.clear();
	}

	static boolean isWaveCompletedMessage(String clean) {
		return clean != null && WAVE_COMPLETED.matcher(clean).matches();
	}

	private static void finishRun(long cycleMs) {
		if (runActive && runWave > 0) {
			Map<String, Integer> counts = new HashMap<>();
			if (runComplete) {
				for (Map.Entry<String, Set<Integer>> entry : runSpawns.entrySet()) {
					counts.put(entry.getKey(), entry.getValue().size());
				}
			}
			EssenceFarmData.recordRun(runWave, counts, cycleMs);
			invalidate();
		}
		abortRun();
	}

	/** Drops the current run without recording it. */
	private static void abortRun() {
		runActive = false;
		runComplete = false;
		runWave = -1;
		runSpawns.clear();
	}

	private static void resetRun() {
		abortRun();
		aliveMobs.clear();
		countedKills.clear();
		ticksSinceLevelChange = 0;
		lastCompletionAt = 0L;
	}

	private static void onClientTick(Minecraft client) {
		if (client.player == null || client.level == null) {
			return;
		}
		if (ticksSinceLevelChange < Integer.MAX_VALUE) {
			ticksSinceLevelChange++;
		}
		if (isInFactory()) {
			trackRun(client.level);
		} else if (runActive || !aliveMobs.isEmpty()) {
			resetRun();
		}
		readStatsScreen(client);
		readCrystalMenu(client);
		long now = System.currentTimeMillis();
		if (now - lastPlanAt >= PLAN_INTERVAL_MS) {
			lastPlanAt = now;
			rememberCrystalLevels();
			rebuildLines(client);
		}
	}

	private static void trackRun(ClientLevel level) {
		// Death is visible via synced health before the entity unloads; count it as early as possible.
		for (Map.Entry<Integer, String> entry : aliveMobs.entrySet()) {
			Entity entity = level.getEntity(entry.getKey());
			if (entity instanceof LivingEntity living && (living.isDeadOrDying() || living.getHealth() <= 0)) {
				countKill(entry.getKey(), entry.getValue());
			}
		}
	}

	// ---- drop chance, crystal levels and stock ----

	/** Takes "Essn Drp Chc" from the stats overview once per opened stats screen (unless disabled in F6). */
	private static void readStatsScreen(Minecraft client) {
		if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
			lastStatsTitle = null;
			return;
		}
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		String title = screen.getTitle().getString();
		if (!config.essenceFarmAutoDropChance || !ZeichenUtility.containsProfileStatsUi(title)
				|| title.equals(lastStatsTitle) || client.font == null) {
			return;
		}
		lastStatsTitle = title;
		EssenceFarmStatsReader.essenceDropChance(EssenceFarmStatsReader.layout(screen.getTitle(), client.font),
				ZeichenUtility.getProfileStatsGlyphs())
				.ifPresent(value -> {
					if (Math.abs(value - config.essenceFarmDropChance) > 1e-6) {
						config.essenceFarmDropChance = value;
						CCLiveUtilitiesConfig.HANDLER.save();
						invalidate();
					}
				});
	}

	/** Reads level and essence stock of every crystal in the "Machtkristalle Verbessern" menu. */
	private static void readCrystalMenu(Minecraft client) {
		if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)
				|| !screen.getTitle().getString().contains(EssenceFarmCrystalMenu.TITLE)) {
			crystalMenuCooldown = 0;
			return;
		}
		if (crystalMenuCooldown-- > 0) {
			return;
		}
		crystalMenuCooldown = CRYSTAL_MENU_SCAN_INTERVAL;
		for (Slot slot : screen.getMenu().slots) {
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			List<String> lines = new ArrayList<>();
			lines.add(stack.getHoverName().getString());
			var lore = stack.get(DataComponents.LORE);
			if (lore != null) {
				for (Component line : lore.lines()) {
					lines.add(line.getString().replaceAll("§.", ""));
				}
			}
			EssenceFarmCrystalMenu.parse(lines).ifPresent(info -> {
				onCrystalLevel(info.name(), info.level());
				if (info.essenceKey() != null && info.owned() >= 0) {
					setStock(info.essenceKey(), info.owned());
				}
			});
		}
	}

	/** Equipped crystals show their level in the tab list; remember it for every crystal. */
	private static void rememberCrystalLevels() {
		for (MachtkristallSlot slot : NpcAlertState.machtkristallSlots) {
			int level = parseLevel(slot);
			if (level > 0) {
				onCrystalLevel(slot.name, level);
			}
		}
	}

	/** Stores a crystal level; on a level up the essences paid for the passed levels leave the stock. */
	private static void onCrystalLevel(String name, int level) {
		if (name == null || name.isBlank()) {
			return;
		}
		int previous = EssenceFarmCrystals.get(name.trim()).level;
		if (!EssenceFarmCrystals.updateLevel(name, level)) {
			return;
		}
		invalidate();
		if (previous <= 0 || level <= previous || level - previous > 20) {
			return;
		}
		EssenceTierTable tiers = EssenceFarmData.tiers();
		for (int passed = previous + 1; passed <= level; passed++) {
			MkLevelInfo info = MkLevelDatabase.getLevelInfo(passed);
			if (info == null) {
				continue;
			}
			EssenceFarmNeeds.Result spent = EssenceFarmNeeds.compute(
					List.of(new EssenceFarmNeeds.LevelRange(passed - 1, passed)),
					l -> new EssenceFarmNeeds.LevelCost(info.essence, info.amount), tiers, key -> 0L);
			for (EssenceFarmNeeds.Requirement req : spent.requirements()) {
				String bagKey = tiers.bagKey(req.type(), req.tier());
				if (bagKey != null) {
					setStock(bagKey, Math.max(0L, stockOf(bagKey) - req.total()));
				}
			}
		}
	}

	/** Sets an authoritative stock value; estimated gains for that essence start over. */
	private static void setStock(String bagKey, long amount) {
		CollectedMaterialsResourcesStorage.updateEssence(bagKey, amount);
		lastSeenStock.put(bagKey, amount);
		gained.remove(bagKey);
	}

	/** "Essn Drp Chc" in percent as shown in the stats overview (409 means x4.09), NaN if unknown. */
	public static double dropChancePercent() {
		double value = CCLiveUtilitiesConfig.HANDLER.instance().essenceFarmDropChance;
		return value > 0 ? value : Double.NaN;
	}

	/** Average essences per kill: the stats value already includes the base drop (409 % = 4.09). */
	public static double dropMultiplier() {
		double percent = dropChancePercent();
		return Double.isNaN(percent) ? 1.0 : percent / 100.0;
	}

	/** Bag snapshot plus essences estimated from kills since that snapshot. */
	private static long stockOf(String bagKey) {
		long stored = CollectedMaterialsResourcesStorage.getEssenceAmount(bagKey);
		Long seen = lastSeenStock.get(bagKey);
		if (seen == null || seen != stored) {
			// New bag snapshot: it already contains everything gained so far.
			lastSeenStock.put(bagKey, stored);
			if (seen != null) {
				gained.remove(bagKey);
			}
		}
		return stored + Math.round(gained.getOrDefault(bagKey, 0.0));
	}

	// ---- plan and display ----

	private static void rebuildLines(Minecraft client) {
		List<Line> out = new ArrayList<>();
		out.add(new Line("Essenzen-Farmplan", TITLE_COLOR));

		List<EssenceFarmNeeds.LevelRange> ranges = new ArrayList<>();
		for (String name : EssenceFarmCrystals.names()) {
			EssenceFarmCrystals.Crystal crystal = EssenceFarmCrystals.get(name);
			if (crystal.target <= 0) {
				continue;
			}
			if (crystal.level <= 0) {
				out.add(new Line(name + ": Level unbekannt (Machtkristall-Menü öffnen)", WARN_COLOR));
				continue;
			}
			int target = crystal.effectiveTarget();
			if (target > 0) {
				ranges.add(new EssenceFarmNeeds.LevelRange(crystal.level, target));
				out.add(new Line(name + ": " + crystal.level + " -> " + target, TEXT_COLOR));
			} else {
				out.add(new Line(name + ": Ziel " + crystal.target + " erreicht", DONE_COLOR));
			}
		}
		if (ranges.isEmpty()) {
			out.add(new Line("Ziellevel im F6-Menü eintragen", DIM_COLOR));
			addDebugLines(out);
			finishLines(client, out);
			return;
		}

		double percent = dropChancePercent();
		double multiplier = dropMultiplier();
		if (Double.isNaN(percent)) {
			out.add(new Line("Dropchance unbekannt: Stats im Kompass öffnen", WARN_COLOR));
		} else {
			out.add(new Line(String.format(Locale.GERMANY, "Dropchance %.0f%% (x%.2f)", percent, multiplier),
					DIM_COLOR));
		}

		EssenceTierTable tiers = EssenceFarmData.tiers();
		EssenceFarmNeeds.Result needs = EssenceFarmNeeds.compute(ranges,
				level -> {
					MkLevelInfo info = MkLevelDatabase.getLevelInfo(level);
					return info != null ? new EssenceFarmNeeds.LevelCost(info.essence, info.amount) : null;
				},
				tiers, EssenceFarmTracker::stockOf);
		Map<String, EssenceFarmNeeds.Requirement> reqByKey = new HashMap<>();
		boolean anyStock = false;
		boolean allDone = true;
		for (EssenceFarmNeeds.Requirement req : needs.requirements()) {
			reqByKey.put(req.type() + "#" + req.tier(), req);
			anyStock |= req.stock() > 0;
			allDone &= req.missing() == 0;
		}
		if (!anyStock) {
			out.add(new Line("Essenz-Tasche öffnen für Bestand", WARN_COLOR));
		}

		EssenceFarmPlanner.Plan plan = EssenceFarmPlanner.plan(needs.needs(), EssenceFarmData.candidateWaves(), tiers,
				multiplier, EssenceFarmPlanner.DEFAULT_FACTOR);
		int currentWave = WaveUtility.getCurrentWave();
		for (EssenceFarmPlanner.Step step : plan.steps()) {
			boolean current = step.wave() == currentWave;
			String duration = EssenceFarmNeeds.formatDuration(EssenceFarmData.averageRunMs(step.wave()) * step.runs());
			String head = (current ? "> " : "") + "Welle " + step.wave() + ": "
					+ NumberFormatting.formatNumberWithSeparator(step.runs()) + "x"
					+ (duration.isEmpty() ? "" : " (~" + duration + ")");
			out.add(new Line(head, current ? ACTIVE_COLOR : TEXT_COLOR));
			for (EssenceFarmPlanner.Covered covered : step.covered()) {
				EssenceFarmNeeds.Requirement req = reqByKey.get(covered.type() + "#" + covered.tier());
				if (req == null) {
					continue;
				}
				out.add(new Line("  " + tiers.displayName(covered.type(), covered.tier()) + ": "
						+ NumberFormatting.formatNumberWithSeparator(Math.min(req.stock(), req.total())) + "/"
						+ NumberFormatting.formatNumberWithSeparator(req.total()),
						req.missing() == 0 ? DONE_COLOR : DIM_COLOR));
			}
		}
		for (EssenceFarmPlanner.Need need : plan.unreachable()) {
			out.add(new Line("  " + tiers.displayName(need.type(), need.tier()) + ": keine Welle bekannt", WARN_COLOR));
		}
		for (String essence : needs.unknownEssences()) {
			out.add(new Line("  " + essence + ": nicht über Wellen farmbar", WARN_COLOR));
		}
		if (allDone && needs.unknownEssences().isEmpty()) {
			out.add(new Line("Alle Essenzen vorhanden", DONE_COLOR));
		}
		addDebugLines(out);
		finishLines(client, out);
	}

	private static void addDebugLines(List<Line> out) {
		if (!CCLiveUtilitiesConfig.HANDLER.instance().essenceFarmDebug) {
			return;
		}
		out.add(new Line("-- Debug --", DIM_COLOR));
		out.add(new Line("Fabrik: " + (isInFactory() ? "ja" : "nein") + " | Welle: " + WaveUtility.getCurrentWave(), DIM_COLOR));
		out.add(new Line("Bossbars: " + WaveUtility.debugWaves(), DIM_COLOR));
		out.add(new Line("Lauf: " + (runActive ? "aktiv" : "-") + (runComplete ? " (komplett)" : "")
				+ " | Mobs: " + aliveMobs.size() + " | Spawns: "
				+ runSpawns.values().stream().mapToInt(Set::size).sum(), DIM_COLOR));
		out.add(new Line("Abschlüsse: " + completions + " | Kills: " + kills
				+ String.format(Locale.GERMANY, " | +%.0f Ess.", gainedTotal), DIM_COLOR));
		out.add(new Line("HUD: " + hudCalls + " Aufrufe, " + hudDrawn + " gezeichnet, Grund: " + hudSkipReason, DIM_COLOR));
		if (lastError != null) {
			out.add(new Line("Fehler: " + lastError, WARN_COLOR));
		}
	}

	private static int parseLevel(MachtkristallSlot slot) {
		if (slot == null || slot.isEmpty() || slot.isNotFound()) {
			return -1;
		}
		try {
			return Integer.parseInt(slot.getLevelDisplay());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static void finishLines(Minecraft client, List<Line> out) {
		lines = List.copyOf(out);
		int width = MIN_WIDTH;
		if (client.font != null) {
			for (Line line : lines) {
				width = Math.max(width, client.font.width(line.text()) + PADDING * 2);
			}
		}
		cachedWidth = width;
		cachedHeight = PADDING * 2 + lines.size() * LINE_HEIGHT;
	}

	public static void render(GuiGraphicsExtractor context) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.font == null || context == null) {
			return;
		}
		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		matrices.translate(x(), y());
		matrices.scale(scale(), scale());
		if (CCLiveUtilitiesConfig.HANDLER.instance().essenceFarmShowBackground) {
			context.fill(0, 0, cachedWidth, cachedHeight, 0x80000000);
		}
		int textY = PADDING;
		for (Line line : lines) {
			context.text(client.font, Component.literal(line.text()), PADDING, textY, line.color(), true);
			textY += LINE_HEIGHT;
		}
		matrices.popMatrix();
	}

	public static boolean isConfigEnabled() {
		CCLiveUtilitiesConfig config = CCLiveUtilitiesConfig.HANDLER.instance();
		return config.essenceFarmEnabled && config.showEssenceFarm;
	}

	public static void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
		hudCalls++;
		try {
			Minecraft client = Minecraft.getInstance();
			if (client == null || context == null) {
				hudSkipReason = "kein Client";
				return;
			}
			if (client.gui.hud.isHidden()) {
				hudSkipReason = "HUD versteckt";
				return;
			}
			if (KeyBindingUtility.isPlayerListKeyPressed()) {
				hudSkipReason = "Spielerliste";
				return;
			}
			if (!HudOverlayVisibility.shouldRenderWorldHudOverlays()) {
				hudSkipReason = "Inventar";
				return;
			}
			if (!CCLiveUtilitiesConfig.isModRuntimeEnabled()) {
				hudSkipReason = "Mod/Server aus";
				return;
			}
			if (!isConfigEnabled()) {
				hudSkipReason = "Overlay aus";
				return;
			}
			if (!isInFactory()) {
				hudSkipReason = "nicht in Fabrik";
				return;
			}
			render(context);
			hudDrawn++;
			hudSkipReason = "-";
		} catch (RuntimeException e) {
			if (lastError == null) {
				LOGGER.error("[EssenceFarm] HUD render failed", e);
			}
			lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
		}
	}
}

package net.felix.utilities.Overall.starforged;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.eventsound.chat.EventSoundMatcher;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays a big celebration when the smith grants the very rare smithing state "Sternengeschmiedet", i.e. on
 * {@code [Legend] Schmiedezustand erhalten: [Sternengeschmiedet] ...} (the same message the event sound reacts to):
 * a HUD overlay ({@link StarforgedOverlay}) plus a sound sequence and particles in the world.
 * The crafted item comes from {@link StarforgedCraftTracker} (chat name) and the last clicked blueprint.
 * While it plays, the blueprint inventory is closed and player input is blocked.
 */
public final class StarforgedAnimationUtility {

	private static final long DUPLICATE_COOLDOWN_MS = 1500L;
	private static final Logger LOGGER = LoggerFactory.getLogger("CCLive-Utilities/Starforged");
	private static final String STARFORGED = "Sternengeschmiedet";

	// In-world timings in client ticks (50 ms); IMPACT_TICK matches StarforgedOverlay.IMPACT (2000 ms).
	private static final int IMPACT_TICK = 40;
	private static final int PILLAR_TICKS = 30;
	private static final int STARFALL_END = IMPACT_TICK + 80;
	private static final int WORLD_TICKS = STARFALL_END + 1;
	private static final int[] STRIKE_TICKS = new int[StarforgedOverlay.STRIKES.length];

	static {
		for (int i = 0; i < STRIKE_TICKS.length; i++) {
			STRIKE_TICKS[i] = Math.round(StarforgedOverlay.STRIKES[i] / 50f);
		}
	}

	private static long lastTriggeredAtMs = 0L;
	// Last blueprints clicked with "[Linksklick]: Herstellen": left click crafts one, shift + left click 10.
	// Only touched on the client thread.
	private static ItemStack lastSingleItem;
	private static long lastSingleAtMs = -1L;
	private static ItemStack lastBatchItem;
	private static long lastBatchAtMs = -1L;
	private static boolean blockInput = false;
	// Current tick of the in-world effects, -1 when idle. Only touched on the client thread.
	private static int worldTick = -1;
	private static boolean initialized = false;

	private StarforgedAnimationUtility() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(StarforgedAnimationUtility::onClientTick);
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("cclive-utilities", "starforged_animation"),
				StarforgedOverlay::render);
	}

	/** True if the animation would play for a starforged state; the event sound is muted then. */
	public static boolean isAnimationEnabled() {
		return CCLiveUtilitiesConfig.HANDLER.instance().starforgedAnimationEnabled
				&& CCLiveUtilitiesConfig.isModRuntimeEnabled();
	}

	public static void handleIncomingMessage(Component message, boolean overlay) {
		if (overlay || message == null) {
			return;
		}
		String raw = message.getString();
		boolean mentionsStarforged = raw.contains("Sternengeschmiedet") || raw.contains("Sterngeschmiedet");
		if (mentionsStarforged) {
			LOGGER.info("[Starforged] message: '{}' enabled={} runtime={} matched={}", raw.replace("\n", "\\n"),
					CCLiveUtilitiesConfig.HANDLER.instance().starforgedAnimationEnabled,
					CCLiveUtilitiesConfig.isModRuntimeEnabled(), isStarforgedMessage(raw));
		}
		if (!isAnimationEnabled()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		// handleSystemChat may run on the network thread; state and render calls go to the client thread.
		// One packet may hold several lines (e.g. craft message plus state), so each line is checked.
		long now = System.currentTimeMillis();
		boolean starforged = false;
		for (String line : raw.split("\n")) {
			if (StarforgedCraftTracker.craftedName(line) != null || StarforgedCraftTracker.isBlueprintBatch(line)) {
				client.execute(() -> StarforgedCraftTracker.recordChat(line, now));
			} else if (isStarforgedMessage(line)) {
				starforged = true;
			}
		}
		if (!starforged) {
			return;
		}
		if (now - lastTriggeredAtMs < DUPLICATE_COOLDOWN_MS) {
			return;
		}
		lastTriggeredAtMs = now;
		LOGGER.info("[Starforged] smithing state detected, starting animation");
		client.execute(() -> {
			closeContainerScreen(client);
			playAnimation(visualForCraft(now), true);
		});
	}

	/**
	 * Remembers the item the player clicks to craft in a blueprint inventory; needed for batch crafts,
	 * whose chat message has no item name. Called from {@code StarforgedSlotClickMixin} on the client thread.
	 */
	public static void onSlotClicked(Component screenTitle, ItemStack stack, boolean batch) {
		if (stack == null || stack.isEmpty() || screenTitle == null) {
			return;
		}
		String title = screenTitle.getString();
		if (!StarforgedCraftTracker.isBlueprintInventory(title)) {
			// Diagnostics: log clicks only for titles that look like a blueprint inventory.
			if (title.contains("Bau") || title.contains("Blueprint") || title.contains("Favorisiert")) {
				LOGGER.info("[Starforged] click ignored, title not matched: '{}' item='{}'", title,
						stack.getHoverName().getString());
			}
			return;
		}
		ItemLore lore = stack.get(DataComponents.LORE);
		List<String> lines = new ArrayList<>();
		if (lore != null) {
			for (Component line : lore.lines()) {
				lines.add(line.getString());
			}
		}
		if (!StarforgedCraftTracker.isCraftAction(lines)) {
			LOGGER.info("[Starforged] click ignored, no craft action: item='{}' batch={} lore={}",
					stack.getHoverName().getString(), batch, lines);
			return;
		}
		LOGGER.info("[Starforged] craft click recorded: item='{}' batch={}", stack.getHoverName().getString(), batch);
		if (batch) {
			lastBatchItem = stack.copy();
			lastBatchAtMs = System.currentTimeMillis();
		} else {
			lastSingleItem = stack.copy();
			lastSingleAtMs = System.currentTimeMillis();
		}
	}

	/** Diagnostics for clicks in a blueprint inventory that are neither left click nor shift + left click. */
	public static void logFilteredClick(Component screenTitle, int button, String input) {
		if (screenTitle != null && StarforgedCraftTracker.isBlueprintInventory(screenTitle.getString())) {
			LOGGER.info("[Starforged] click filtered: button={} input={}", button, input);
		}
	}

	/**
	 * True while the animation runs: then mouse buttons, scrolling, camera turning and key presses are
	 * swallowed so the player cannot keep crafting or click anything else. Releases always pass.
	 */
	public static boolean isInputBlocked() {
		return blockInput && StarforgedOverlay.isRunning();
	}

	/**
	 * Feeds a batch craft message and the starforged state message through the normal detection, as if the
	 * server had sent them (for testing the item lookup after a shift-click).
	 */
	public static void simulateBatchCraftMessages() {
		handleIncomingMessage(Component.literal("[Legend] Du hast den Bauplan erfolgreich 10x gebaut!"), false);
		Minecraft client = Minecraft.getInstance();
		// Queued after the craft message, so the tracker has recorded it; the cooldown is skipped for tests.
		client.execute(() -> {
			lastTriggeredAtMs = 0L;
			handleIncomingMessage(Component.literal("[Legend] Schmiedezustand erhalten: [Sternengeschmiedet]"), false);
		});
	}

	/** Skips the running animation (Escape) and gives the input back. */
	public static void cancelAnimation() {
		if (StarforgedOverlay.isRunning()) {
			LOGGER.info("[Starforged] animation cancelled");
		}
		StarforgedOverlay.stop();
		worldTick = -1;
		blockInput = false;
	}

	/**
	 * Plays the animation regardless of the config toggle (for testing). Shows the held item, if any.
	 */
	public static void playTestAnimation() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		client.execute(() -> {
			LocalPlayer player = client.player;
			ItemStack held = player == null ? ItemStack.EMPTY : player.getMainHandItem();
			StarforgedOverlay.Visual visual = held.isEmpty()
					? new StarforgedOverlay.Visual(null, null)
					: new StarforgedOverlay.Visual(held.copy(), held.getHoverName());
			playAnimation(visual, true);
		});
	}

	/**
	 * Item and name to show. A single craft (left click) names the item in chat; the clicked blueprint is
	 * used as icon if it matches. A batch craft (shift + left click) has no name, so the shift-clicked
	 * blueprint provides both.
	 */
	private static StarforgedOverlay.Visual visualForCraft(long nowMs) {
		ItemStack single = recent(lastSingleItem, lastSingleAtMs, nowMs);
		ItemStack batch = recent(lastBatchItem, lastBatchAtMs, nowMs);
		String chatName = StarforgedCraftTracker.recentCraftName(nowMs);
		if (chatName != null) {
			ItemStack icon = matching(single, chatName);
			if (icon == null) {
				icon = matching(batch, chatName);
			}
			LOGGER.info("[Starforged] visual from chat name '{}', icon matched={}", chatName, icon != null);
			return new StarforgedOverlay.Visual(icon == null ? null : icon.copy(), Component.literal(chatName));
		}
		ItemStack clicked;
		if (StarforgedCraftTracker.recentCraftWasBatch(nowMs)) {
			clicked = batch != null ? batch : single;
		} else {
			// No craft message seen: take the most recent click of either kind.
			clicked = batch != null && (single == null || lastBatchAtMs >= lastSingleAtMs) ? batch : single;
		}
		LOGGER.info("[Starforged] visual: craftWasBatch={} single={} batch={} chosen={}",
				StarforgedCraftTracker.recentCraftWasBatch(nowMs), describe(single), describe(batch), describe(clicked));
		if (clicked != null) {
			Component hover = clicked.getHoverName();
			Component name = Component.literal(StarforgedCraftTracker.itemName(hover.getString()))
					.withStyle(hover.getStyle());
			return new StarforgedOverlay.Visual(clicked.copy(), name);
		}
		return new StarforgedOverlay.Visual(null, null);
	}

	private static String describe(ItemStack stack) {
		return stack == null ? "none" : "'" + stack.getHoverName().getString() + "'";
	}

	private static ItemStack recent(ItemStack stack, long atMs, long nowMs) {
		return stack != null && nowMs - atMs <= StarforgedCraftTracker.CLICK_MEMORY_MS ? stack : null;
	}

	private static ItemStack matching(ItemStack stack, String chatName) {
		return stack != null && StarforgedCraftTracker.sameItem(stack.getHoverName().getString(), chatName)
				? stack
				: null;
	}

	/** Closes an open container (e.g. the blueprint inventory) so the player cannot keep crafting. */
	private static void closeContainerScreen(Minecraft client) {
		if (client.player != null && client.gui.screen() instanceof AbstractContainerScreen<?>) {
			client.player.closeContainer();
		}
	}

	private static void playAnimation(StarforgedOverlay.Visual visual, boolean lockInput) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null) {
			LOGGER.info("[Starforged] animation skipped: no player or level");
			return;
		}
		StarforgedOverlay.start(visual);
		blockInput = lockInput;
		worldTick = 0;
	}

	/** True for the official smith message granting "Sternengeschmiedet"; player chat is rejected. */
	static boolean isStarforgedMessage(String rawText) {
		return STARFORGED.equals(EventSoundMatcher.smithingState(EventSoundMatcher.normalize(rawText)));
	}

	/** Drives the in-world part (sounds, sparks, supernova, pillar, starfall) once per client tick. */
	private static void onClientTick(Minecraft client) {
		// Polled as well as handled in StarforgedKeyboardInputMixin: other mods (e.g. LabyMod) may consume
		// the Escape key event before KeyboardHandler.keyPress sees it.
		if (isInputBlocked() && InputConstants.isKeyDown(InputConstants.KEY_ESCAPE)) {
			LOGGER.info("[Starforged] animation skipped with Escape");
			cancelAnimation();
			if (client.gui.screen() instanceof PauseScreen) {
				client.gui.setScreen(null);
			}
			return;
		}
		if (worldTick < 0) {
			return;
		}
		LocalPlayer player = client.player;
		ClientLevel level = client.level;
		if (player == null || level == null || worldTick > WORLD_TICKS) {
			worldTick = -1;
			return;
		}
		playSoundsForTick(level, player, worldTick);
		spawnParticlesForTick(level, player, worldTick);
		worldTick++;
	}

	private static int strikeIndex(int tick) {
		for (int i = 0; i < STRIKE_TICKS.length; i++) {
			if (STRIKE_TICKS[i] == tick) {
				return i;
			}
		}
		return -1;
	}

	private static void playSoundsForTick(ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			play(level, player, SoundEvents.BEACON_POWER_SELECT, 0.7f, 0.6f);
			play(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
			play(level, player, SoundEvents.TRIDENT_THUNDER.value(), 0.12f, 0.5f);
		}
		int strike = strikeIndex(tick);
		if (strike >= 0) {
			// Hammer on the anvil, louder and higher with every strike.
			float k = strike / (STRIKE_TICKS.length - 1f);
			play(level, player, SoundEvents.ANVIL_USE, 0.45f + 0.35f * k, 0.75f + 0.5f * k);
			play(level, player, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.5f + 0.3f * k,
					(float) (0.5 * Math.pow(2.0, strike * 2 / 12.0)));
			if (strike == STRIKE_TICKS.length - 1) {
				play(level, player, SoundEvents.ANVIL_LAND, 0.5f, 1.4f);
			}
		}
		// Riser: a fast chromatic run in the last half second before the impact.
		if (tick >= IMPACT_TICK - 10 && tick < IMPACT_TICK) {
			int step = tick - (IMPACT_TICK - 10);
			play(level, player, SoundEvents.NOTE_BLOCK_BELL.value(), 0.25f + step * 0.04f,
					(float) (0.7 * Math.pow(2.0, step / 12.0)));
		}
		if (tick == IMPACT_TICK) {
			play(level, player, SoundEvents.END_PORTAL_SPAWN, 0.6f, 1.25f);
			play(level, player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
			play(level, player, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 0.75f);
			play(level, player, SoundEvents.TRIDENT_THUNDER.value(), 0.35f, 1.1f);
			play(level, player, SoundEvents.TOTEM_USE, 0.45f, 1.5f);
		} else if (tick == IMPACT_TICK + 6) {
			// Major chord on bells.
			play(level, player, SoundEvents.NOTE_BLOCK_BELL.value(), 0.8f, 1.0f);
			play(level, player, SoundEvents.NOTE_BLOCK_BELL.value(), 0.7f, 1.26f);
			play(level, player, SoundEvents.NOTE_BLOCK_BELL.value(), 0.7f, 1.5f);
		} else if (tick == IMPACT_TICK + 12 || tick == IMPACT_TICK + 24 || tick == IMPACT_TICK + 38
				|| tick == IMPACT_TICK + 55) {
			play(level, player, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 0.7f, 1.0f + (tick - IMPACT_TICK) * 0.006f);
		}
		playChainSounds(level, player, tick);
	}

	/** Ascending chimes while the star chain ignites, then a shimmering chord when it closes. */
	private static void playChainSounds(ClientLevel level, LocalPlayer player, int tick) {
		for (int step = 0; step <= StarforgedOverlay.RING_STEPS; step++) {
			int stepTick = Math.round((StarforgedOverlay.CONSTELLATION_START + step * StarforgedOverlay.RING_STEP) / 50f);
			if (tick == stepTick) {
				// Major pentatonic run upward.
				int[] semitones = { 0, 2, 4, 7, 9, 12, 14 };
				float pitch = (float) (0.7 * Math.pow(2.0, semitones[step % semitones.length] / 12.0));
				play(level, player, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.7f, pitch);
				play(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, pitch);
			}
		}
		if (tick == Math.round(StarforgedOverlay.RING_DONE / 50f)) {
			play(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.2f);
			play(level, player, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.6f);
			play(level, player, SoundEvents.NOTE_BLOCK_BELL.value(), 0.7f, 1.4f);
			play(level, player, SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, 1.78f);
		}
	}

	private static void play(ClientLevel level, LocalPlayer player, SoundEvent sound, float volume, float pitch) {
		level.playLocalSound(player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
	}

	private static void spawnParticlesForTick(ClientLevel level, LocalPlayer player, int tick) {
		if (tick < IMPACT_TICK) {
			spawnChargeGlyphs(level, player, tick);
			int strike = strikeIndex(tick);
			if (strike >= 0) {
				spawnSphere(level, player, player.getY() + 1.0, ParticleTypes.ELECTRIC_SPARK, 14 + strike * 4,
						0.25 + strike * 0.03);
			}
			return;
		}
		int since = tick - IMPACT_TICK;
		if (since == 0) {
			spawnSphere(level, player, player.getY() + 1.2, ParticleTypes.FIREWORK, 90, 0.45);
			spawnSphere(level, player, player.getY() + 1.2, ParticleTypes.TOTEM_OF_UNDYING, 70, 0.6);
			spawnRing(level, player, player.getY() + 0.1, ParticleTypes.END_ROD, 48, 0.35);
			spawnRing(level, player, player.getY() + 0.1, ParticleTypes.FIREWORK, 48, 0.22);
		}
		if (since == 8) {
			spawnRing(level, player, player.getY() + 0.3, ParticleTypes.END_ROD, 40, 0.25);
		}
		if (since <= PILLAR_TICKS) {
			spawnPillar(level, player, since);
		}
		if (since >= 5 && since % 2 == 0 && tick < STARFALL_END) {
			spawnStarfall(level, player);
		}
	}

	/** Enchanting glyphs fly in from all around, denser toward the impact. */
	private static void spawnChargeGlyphs(ClientLevel level, LocalPlayer player, int tick) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		double y = player.getY() + 1.1;
		int count = 4 + tick / 4;
		for (int i = 0; i < count; i++) {
			// The enchant particle travels from (pos + velocity) to pos.
			level.addParticle(ParticleTypes.ENCHANT, player.getX(), y, player.getZ(),
					random.nextDouble(-3.0, 3.0), random.nextDouble(-0.5, 2.0), random.nextDouble(-3.0, 3.0));
		}
		level.addParticle(ParticleTypes.REVERSE_PORTAL, player.getX() + random.nextDouble(-1.5, 1.5),
				y + random.nextDouble(-0.5, 1.2), player.getZ() + random.nextDouble(-1.5, 1.5), 0.0, 0.0, 0.0);
	}

	/** Column of light rising from the player, with three strands spiraling around it. */
	private static void spawnPillar(ClientLevel level, LocalPlayer player, int since) {
		double y = player.getY() + since * 0.12;
		level.addParticle(ParticleTypes.END_ROD, player.getX(), y, player.getZ(), 0.0, 0.12, 0.0);
		double radius = 0.9 - since * 0.015;
		for (int strand = 0; strand < 3; strand++) {
			double angle = since * 0.45 + strand * Math.PI * 2.0 / 3.0;
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.01, 0.0);
			level.addParticle(ParticleTypes.FIREWORK, x, y, z, 0.0, 0.0, 0.0);
		}
	}

	/** Stars falling from the sky around the player. */
	private static void spawnStarfall(ClientLevel level, LocalPlayer player) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < 3; i++) {
			double angle = random.nextDouble(Math.PI * 2.0);
			double distance = random.nextDouble(1.5, 6.0);
			double x = player.getX() + Math.cos(angle) * distance;
			double z = player.getZ() + Math.sin(angle) * distance;
			double y = player.getY() + random.nextDouble(5.0, 8.0);
			level.addParticle(ParticleTypes.END_ROD, x, y, z, random.nextDouble(-0.03, 0.03), -0.35,
					random.nextDouble(-0.03, 0.03));
			level.addParticle(ParticleTypes.FIREWORK, x, y + 0.3, z, 0.0, -0.3, 0.0);
		}
	}

	private static void spawnRing(ClientLevel level, LocalPlayer player, double y, ParticleOptions particle,
			int count, double speed) {
		for (int i = 0; i < count; i++) {
			double angle = i * Math.PI * 2.0 / count;
			level.addParticle(particle, player.getX(), y, player.getZ(),
					Math.cos(angle) * speed, 0.04, Math.sin(angle) * speed);
		}
	}

	private static void spawnSphere(ClientLevel level, LocalPlayer player, double y, ParticleOptions particle,
			int count, double speed) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < count; i++) {
			// Uniform direction on the unit sphere.
			double u = random.nextDouble(-1.0, 1.0);
			double angle = random.nextDouble(Math.PI * 2.0);
			double r = Math.sqrt(1.0 - u * u);
			level.addParticle(particle, player.getX(), y, player.getZ(),
					Math.cos(angle) * r * speed, u * speed, Math.sin(angle) * r * speed);
		}
	}
}

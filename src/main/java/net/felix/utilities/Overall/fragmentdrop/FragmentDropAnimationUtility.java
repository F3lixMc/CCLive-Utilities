package net.felix.utilities.Overall.fragmentdrop;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.felix.CCLiveUtilitiesConfig;
import net.felix.utilities.Overall.customtextures.CharmCatalog;
import net.felix.utilities.Overall.customtextures.CustomItemModelUtility;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Plays a custom celebration when the server announces a talisman fragment drop, e.g.
 * {@code [Legend] Du hast Anhänger der Schmiedekammer [Fragment] gefunden.}:
 * a HUD overlay ({@link FragmentDropOverlay}) plus a sound sequence and particles in the world.
 * Known charms show their fragment texture and the charm name/effect (see {@link CharmCatalog}).
 */
public final class FragmentDropAnimationUtility {

	private static final long DUPLICATE_COOLDOWN_MS = 500L;
	private static final String LABEL = "Talismanfragment gefunden";

	// In-world effect timings in client ticks; IMPACT_TICK matches FragmentDropOverlay.IMPACT (900 ms).
	private static final int IMPACT_TICK = 18;
	private static final int HELIX_TICKS = 36;
	private static final int HELIX_END = IMPACT_TICK + HELIX_TICKS;
	private static final int WORLD_TICKS = HELIX_END + 1;
	private static final double HELIX_RISE = 0.07;

	// Only official server messages; player chat ("Name >> ...") is rejected separately.
	// Current: "Du hast Anhänger der X [Fragment] gefunden."; legacy: "Du hast X-Talismanfragment gefunden."
	private static final Pattern FRAGMENT_DROP_PATTERN = Pattern.compile(
			"^\\[Legend\\]\\s*Du hast (.+?(?:\\s*\\[Fragment\\]|fragment)) gefunden\\.?\\s*$");

	private static long lastTriggeredAtMs = 0L;
	// Current tick of the in-world effects, -1 when idle. Only touched on the client thread.
	private static int worldTick = -1;
	private static boolean initialized = false;

	private FragmentDropAnimationUtility() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		ClientTickEvents.END_CLIENT_TICK.register(FragmentDropAnimationUtility::onClientTick);
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("cclive-utilities", "fragment_drop_animation"),
				FragmentDropOverlay::render);
	}

	public static void handleIncomingMessage(Component message, boolean overlay) {
		if (overlay || message == null) {
			return;
		}
		if (!CCLiveUtilitiesConfig.HANDLER.instance().fragmentDropAnimationEnabled
				|| !CCLiveUtilitiesConfig.isModRuntimeEnabled()) {
			return;
		}
		String fragmentName = extractFragmentName(message.getString());
		if (fragmentName == null) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastTriggeredAtMs < DUPLICATE_COOLDOWN_MS) {
			return;
		}
		lastTriggeredAtMs = now;

		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		// handleSystemChat may run on the network thread; render-related calls go to the client thread.
		client.execute(() -> playAnimation(fragmentName));
	}

	/**
	 * Plays the animation regardless of the config toggle (for testing).
	 * @param charmKey texture key from {@link CharmCatalog}, or null for a random charm
	 * @return the fragment name that was played, or null if the key is unknown
	 */
	public static String playTestAnimation(String charmKey) {
		CharmCatalog.Charm charm;
		if (charmKey == null) {
			List<CharmCatalog.Charm> all = CharmCatalog.all();
			charm = all.get(ThreadLocalRandom.current().nextInt(all.size()));
		} else {
			charm = CharmCatalog.byKey(charmKey);
		}
		if (charm == null) {
			return null;
		}
		String fragmentName = charm.displayName() + " [Fragment]";
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			client.execute(() -> playAnimation(fragmentName));
		}
		return fragmentName;
	}

	private static void playAnimation(String fragmentName) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		FragmentDropOverlay.start(visualFor(fragmentName));
		worldTick = 0;
	}

	/** Charm name + effect + textures for known charms; otherwise the split fragment name. */
	private static FragmentDropOverlay.Visual visualFor(String fragmentName) {
		CharmCatalog.Charm charm = CharmCatalog.matchFragmentName(fragmentName);
		if (charm == null) {
			String plain = fragmentName.replace("[Fragment]", "").trim();
			String[] parts = splitFragmentName(plain);
			return new FragmentDropOverlay.Visual(LABEL, parts[1].isEmpty() ? parts[0] : parts[1], "", null, null);
		}
		boolean textures = charm.textured() && CustomItemModelUtility.isCharmTexturesConfigEnabled();
		return new FragmentDropOverlay.Visual(LABEL, charm.displayName(), charm.effect(),
				textures ? CustomItemModelUtility.fragmentTexture(charm.key()) : null,
				textures ? CustomItemModelUtility.charmTexture(charm.key()) : null);
	}

	/** Drives the in-world part (sound sequence, converging glyphs, helix, bursts) once per client tick. */
	private static void onClientTick(Minecraft client) {
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
		if (worldTick < IMPACT_TICK) {
			spawnChargeGlyphs(level, player);
		} else {
			spawnHelix(level, player, worldTick - IMPACT_TICK);
		}
		if (worldTick == IMPACT_TICK) {
			spawnRing(level, player, player.getY() + 0.1, ParticleTypes.END_ROD, 36, 0.3);
			spawnSphere(level, player, player.getY() + 1.2, ParticleTypes.FIREWORK, 50, 0.35);
		}
		if (worldTick == HELIX_END) {
			spawnRing(level, player, player.getY() + HELIX_TICKS * HELIX_RISE, ParticleTypes.FIREWORK, 40, 0.3);
		}
		worldTick++;
	}

	private static void playSoundsForTick(ClientLevel level, LocalPlayer player, int tick) {
		if (tick == 0) {
			play(level, player, SoundEvents.BEACON_ACTIVATE, 0.6f, 1.2f);
			play(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 0.8f);
			return;
		}
		// Charge: chimes rising in whole tones every two ticks, getting louder.
		if (tick < IMPACT_TICK && tick % 2 == 0) {
			int step = tick / 2 - 1;
			float pitch = (float) (0.6 * Math.pow(2.0, step * 2 / 12.0));
			play(level, player, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.4f + step * 0.07f, pitch);
			return;
		}
		if (tick == IMPACT_TICK) {
			play(level, player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
			play(level, player, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.7f, 0.9f);
			play(level, player, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 0.8f);
			play(level, player, SoundEvents.TRIDENT_THUNDER.value(), 0.25f, 1.8f);
		} else if (tick == IMPACT_TICK + 6) {
			play(level, player, SoundEvents.NOTE_BLOCK_BELL.value(), 0.7f, 1.0f);
			play(level, player, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.7f, 2.0f);
		} else if (tick == HELIX_END) {
			play(level, player, SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.6f, 1.2f);
		}
	}

	private static void play(ClientLevel level, LocalPlayer player, SoundEvent sound, float volume, float pitch) {
		level.playLocalSound(player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
	}

	/** Enchanting glyphs fly from all around into the player during the charge. */
	private static void spawnChargeGlyphs(ClientLevel level, LocalPlayer player) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		double y = player.getY() + 1.2;
		for (int i = 0; i < 8; i++) {
			// The enchant particle travels from (pos + velocity) to pos.
			level.addParticle(ParticleTypes.ENCHANT, player.getX(), y, player.getZ(),
					random.nextDouble(-2.5, 2.5), random.nextDouble(-0.5, 1.5), random.nextDouble(-2.5, 2.5));
		}
		level.addParticle(ParticleTypes.REVERSE_PORTAL, player.getX() + random.nextDouble(-1.5, 1.5),
				y + random.nextDouble(-0.5, 1.0), player.getZ() + random.nextDouble(-1.5, 1.5), 0.0, 0.0, 0.0);
	}

	/** Two strands spiraling upward around the player. */
	private static void spawnHelix(ClientLevel level, LocalPlayer player, int tick) {
		if (tick > HELIX_TICKS) {
			return;
		}
		double y = player.getY() + tick * HELIX_RISE;
		double radius = 1.1 - tick * 0.012;
		for (int strand = 0; strand < 2; strand++) {
			double angle = tick * 0.42 + strand * Math.PI;
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.01, 0.0);
			level.addParticle(ParticleTypes.WITCH, x, y, z, 0.0, 0.0, 0.0);
		}
	}

	private static void spawnRing(ClientLevel level, LocalPlayer player, double y, ParticleOptions particle,
			int count, double speed) {
		for (int i = 0; i < count; i++) {
			double angle = i * Math.PI * 2.0 / count;
			double vx = Math.cos(angle) * speed;
			double vz = Math.sin(angle) * speed;
			level.addParticle(particle, player.getX(), y, player.getZ(), vx, 0.04, vz);
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

	/**
	 * Splits e.g. "Schmiedekammer-Talismanfragment" into {"Talismanfragment", "Schmiedekammer"}.
	 * Without a hyphen the whole name becomes the title and the subtitle stays empty.
	 */
	static String[] splitFragmentName(String fragmentName) {
		int dash = fragmentName.lastIndexOf('-');
		if (dash <= 0 || dash == fragmentName.length() - 1) {
			return new String[] { fragmentName, "" };
		}
		return new String[] { fragmentName.substring(dash + 1), fragmentName.substring(0, dash) };
	}

	/** Returns the fragment name from an official drop message, or null. Package-visible for tests. */
	static String extractFragmentName(String rawText) {
		if (rawText == null) {
			return null;
		}
		String text = rawText.replaceAll("§[0-9a-fk-or]", "")
				.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "")
				.trim();
		if (text.contains(">>")) {
			return null;
		}
		int legendIndex = text.indexOf("[Legend]");
		if (legendIndex < 0) {
			return null;
		}
		Matcher matcher = FRAGMENT_DROP_PATTERN.matcher(text.substring(legendIndex));
		return matcher.matches() ? matcher.group(1) : null;
	}
}

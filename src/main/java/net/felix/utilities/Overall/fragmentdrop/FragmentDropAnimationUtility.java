package net.felix.utilities.Overall.fragmentdrop;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Plays the totem-of-undying activation animation with a prismarine shard
 * when the server announces a talisman fragment drop, e.g.
 * {@code [Legend] Du hast Schmiedekammer-Talismanfragment gefunden.},
 * and shows the fragment name as title/subtitle.
 */
public final class FragmentDropAnimationUtility {

	private static final long DUPLICATE_COOLDOWN_MS = 500L;

	// Title timings in ticks, roughly matching the totem animation length.
	private static final int TITLE_FADE_IN = 5;
	private static final int TITLE_STAY = 30;
	private static final int TITLE_FADE_OUT = 10;

	private static final String TEST_FRAGMENT_NAME = "Schmiedekammer-Talismanfragment";

	// Only official server messages; player chat ("Name >> ...") is rejected separately.
	private static final Pattern FRAGMENT_DROP_PATTERN = Pattern.compile(
			"^\\[Legend\\]\\s*Du hast (\\S+fragment) gefunden\\.?\\s*$");

	private static long lastTriggeredAtMs = 0L;

	private FragmentDropAnimationUtility() {
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

	/** Plays the animation regardless of the config toggle (for testing). */
	public static void playTestAnimation() {
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			client.execute(() -> playAnimation(TEST_FRAGMENT_NAME));
		}
	}

	private static void playAnimation(String fragmentName) {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		client.particleEngine.createTrackingEmitter(client.player, ParticleTypes.TOTEM_OF_UNDYING, 30);
		client.level.playLocalSound(client.player.getX(), client.player.getY(), client.player.getZ(),
				SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f, false);
		client.player.displayItemActivation(new ItemStack(Items.PRISMARINE_SHARD));
		showTitle(client.gui.hud, fragmentName);
	}

	private static void showTitle(Hud hud, String fragmentName) {
		String[] parts = splitFragmentName(fragmentName);
		hud.setTimes(TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT);
		hud.setSubtitle(parts[1].isEmpty()
				? Component.empty()
				: Component.literal(parts[1]).withStyle(ChatFormatting.GRAY));
		hud.setTitle(Component.literal(parts[0]).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
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

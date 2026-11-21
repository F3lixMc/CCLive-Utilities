package net.felix.utilities.Overall.fragmentdrop;

import java.util.regex.Pattern;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Plays the totem-of-undying activation animation with a prismarine shard
 * when the server announces a talisman fragment drop, e.g.
 * {@code [Legend] Du hast Schmiedekammer-Talismanfragment gefunden.}
 */
public final class FragmentDropAnimationUtility {

	private static final long DUPLICATE_COOLDOWN_MS = 500L;

	// Only official server messages; player chat ("Name >> ...") is rejected separately.
	private static final Pattern FRAGMENT_DROP_PATTERN = Pattern.compile(
			"^\\[Legend\\]\\s*Du hast \\S+fragment gefunden\\.?\\s*$");

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
		if (!isFragmentDropMessage(message.getString())) {
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
		client.execute(FragmentDropAnimationUtility::playAnimation);
	}

	/** Plays the animation regardless of the config toggle (for testing). */
	public static void playTestAnimation() {
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			client.execute(FragmentDropAnimationUtility::playAnimation);
		}
	}

	private static void playAnimation() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.level == null) {
			return;
		}
		client.particleEngine.createTrackingEmitter(client.player, ParticleTypes.TOTEM_OF_UNDYING, 30);
		client.level.playLocalSound(client.player.getX(), client.player.getY(), client.player.getZ(),
				SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f, false);
		client.player.displayItemActivation(new ItemStack(Items.PRISMARINE_SHARD));
	}

	/** Pure matcher, package-visible for tests. */
	static boolean isFragmentDropMessage(String rawText) {
		if (rawText == null) {
			return false;
		}
		String text = rawText.replaceAll("§[0-9a-fk-or]", "")
				.replaceAll("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]", "")
				.trim();
		if (text.contains(">>")) {
			return false;
		}
		int legendIndex = text.indexOf("[Legend]");
		if (legendIndex < 0) {
			return false;
		}
		return FRAGMENT_DROP_PATTERN.matcher(text.substring(legendIndex)).matches();
	}
}

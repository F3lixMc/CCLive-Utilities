package net.felix.utilities.Overall.customtextures;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.felix.CCLiveUtilitiesConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gives backpack charms (Anhänger), their fragments and power crystals (Machtkristalle) the bundled
 * custom textures.
 *
 * <p>Instead of shipping a resource pack that must sit above the server pack (and would have to copy the
 * server's whole {@code items/paper.json}), {@code CharmItemModelMixin} swaps the item model id of matching
 * stacks to our own item definitions ({@code assets/cclive-utilities/items/...}). The server pack only
 * defines the {@code minecraft} namespace, so it never overrides these, and the swap applies everywhere an
 * item is drawn. Toggles: {@code charmTexturesEnabled}, {@code powerCrystalTexturesEnabled}, applied live.
 */
public final class CustomItemModelUtility {

	private static final Logger LOGGER = LoggerFactory.getLogger("CCLive-Utilities/CustomTextures");
	private static final String NAMESPACE = "cclive-utilities";
	private static final int CACHE_LIMIT = 512;

	private enum Kind { NONE, CHARM, POWER_CRYSTAL }

	/** Resolved display name; model is null for NONE. */
	private record Resolved(Kind kind, Identifier model) {
	}

	private static final Resolved NONE = new Resolved(Kind.NONE, null);
	// Display name -> resolved model. Item rendering may run on more than one thread.
	private static final Map<String, Resolved> CACHE = new ConcurrentHashMap<>();
	// Models already reported once in the log, so a working swap is visible in latest.log.
	private static final Set<Identifier> LOGGED = ConcurrentHashMap.newKeySet();

	private CustomItemModelUtility() {
	}

	/** Charm config toggle only, without the server gate (the fragment animation is gated separately). */
	public static boolean isCharmTexturesConfigEnabled() {
		return CCLiveUtilitiesConfig.HANDLER.instance().charmTexturesEnabled;
	}

	private static boolean isPowerCrystalTexturesConfigEnabled() {
		return CCLiveUtilitiesConfig.HANDLER.instance().powerCrystalTexturesEnabled;
	}

	/** Raw texture file of a charm, for direct blits (fragment drop animation). */
	public static Identifier charmTexture(String key) {
		return Identifier.fromNamespaceAndPath(NAMESPACE, "textures/item/charm/" + key + ".png");
	}

	/** Raw texture file of a charm fragment, for direct blits (fragment drop animation). */
	public static Identifier fragmentTexture(String key) {
		return Identifier.fromNamespaceAndPath(NAMESPACE, "textures/item/charm/fragment/" + key + ".png");
	}

	/** Item model id to use instead of the stack's own, or null to keep the original. */
	public static Identifier modelOverride(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		Resolved resolved = resolveCached(stack.getHoverName().getString());
		if (resolved.kind() == Kind.NONE || !isKindEnabled(resolved.kind())) {
			return null;
		}
		if (LOGGED.add(resolved.model())) {
			LOGGER.info("Custom item model active: {} -> {}", stack.getHoverName().getString(), resolved.model());
		}
		return resolved.model();
	}

	private static boolean isKindEnabled(Kind kind) {
		boolean toggle = kind == Kind.CHARM ? isCharmTexturesConfigEnabled() : isPowerCrystalTexturesConfigEnabled();
		return toggle && CCLiveUtilitiesConfig.isModRuntimeEnabled();
	}

	private static Resolved resolveCached(String name) {
		Resolved cached = CACHE.get(name);
		if (cached == null) {
			cached = resolve(name);
			if (CACHE.size() >= CACHE_LIMIT) {
				CACHE.clear();
			}
			CACHE.put(name, cached);
		}
		return cached;
	}

	private static Resolved resolve(String name) {
		CharmCatalog.Charm fragment = CharmCatalog.matchFragmentName(name);
		if (fragment != null) {
			return fragment.textured() ? new Resolved(Kind.CHARM, model("charm/fragment/" + fragment.key())) : NONE;
		}
		CharmCatalog.Charm charm = CharmCatalog.matchCharmName(name);
		if (charm != null) {
			return charm.textured() ? new Resolved(Kind.CHARM, model("charm/" + charm.key())) : NONE;
		}
		String crystal = PowerCrystalCatalog.matchName(name);
		return crystal != null ? new Resolved(Kind.POWER_CRYSTAL, model("powercrystal/" + crystal)) : NONE;
	}

	private static Identifier model(String path) {
		return Identifier.fromNamespaceAndPath(NAMESPACE, path);
	}

	/** Human-readable diagnosis for {@code /cclive texture_debug} (held item). */
	public static String describe(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "Kein Item in der Hand";
		}
		String name = stack.getHoverName().getString();
		Resolved resolved = resolve(name);
		return "Name: '" + name.replace("§", "&") + "'"
				+ " | item_model: " + stack.get(DataComponents.ITEM_MODEL)
				+ " | erkannt: " + resolved.kind() + (resolved.model() != null ? " -> " + resolved.model() : "")
				+ " | Anhänger-Texturen: " + isCharmTexturesConfigEnabled()
				+ " | Machtkristall-Texturen: " + isPowerCrystalTexturesConfigEnabled()
				+ " | Mod aktiv (Server-Gate): " + CCLiveUtilitiesConfig.isModRuntimeEnabled()
				+ " | aktueller Override: " + modelOverride(stack);
	}
}

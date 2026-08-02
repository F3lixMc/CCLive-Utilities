package net.felix.utilities.Town;

import net.felix.CCLiveUtilities;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {

	public static final Identifier STARFORGED_ID = Identifier.fromNamespaceAndPath(CCLiveUtilities.MOD_ID, "misc.starforged");
	public static final SoundEvent STARFORGED = register(STARFORGED_ID);

	private ModSounds() {
	}

	private static SoundEvent register(Identifier id) {
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void initialize() {
		// Lädt die statischen Felder und registriert die Sound-Events.
	}
}

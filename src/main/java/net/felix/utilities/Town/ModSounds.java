package net.felix.utilities.Town;

import net.felix.CCLiveUtilities;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {

	public static final Identifier STARFORGED_ID = Identifier.of(CCLiveUtilities.MOD_ID, "misc.starforged");
	public static final SoundEvent STARFORGED = register(STARFORGED_ID);

	private ModSounds() {
	}

	private static SoundEvent register(Identifier id) {
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void initialize() {
		// Lädt die statischen Felder und registriert die Sound-Events.
	}
}

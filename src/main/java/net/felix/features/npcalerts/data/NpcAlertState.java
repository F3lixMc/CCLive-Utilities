package net.felix.features.npcalerts.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Aktueller Zustand aller NPC-Alerts (Forschung, Kapazitaeten, Kombo-Kiste, Machtkristall- und Recycler-Slots).
 */
public final class NpcAlertState {

	private NpcAlertState() {
	}

	// Forschung
	public static final CapacityData forschung = new CapacityData();

	// Kapazitäten
	public static final CapacityData ambossCapacity = new CapacityData();

	public static final CapacityData schmelzofenCapacity = new CapacityData();

	public static final CapacityData jaegerCapacity = new CapacityData();

	public static final CapacityData seelenCapacity = new CapacityData();

	public static final CapacityData essenzenCapacity = new CapacityData();

	public static final CapacityData komboKiste = new CapacityData();

	public static final MachtkristallSlot[] machtkristallSlots = new MachtkristallSlot[3];

	static {
		// Initialisiere alle 3 Slots
		for (int i = 0; i < 3; i++) {
			machtkristallSlots[i] = new MachtkristallSlot();
		}
	}

	// Legacy: Für Kompatibilität mit bestehendem Code
	@Deprecated
	public static final Map<String, XpProgress> machtkristalle = new HashMap<>();

	// Recycler Slots
	public static final CapacityData recyclerSlot1 = new CapacityData();

	public static final CapacityData recyclerSlot2 = new CapacityData();

	public static final CapacityData recyclerSlot3 = new CapacityData();

	/**
	 * Gibt alle Machtkristall-Namen zurück
	 */
	public static java.util.Set<String> getMachtkristallNames() {
		return machtkristalle.keySet();
	}

	/**
	 * Gibt XP-Daten für einen bestimmten Machtkristall zurück
	 */
	public static XpProgress getMachtkristallXp(String kristallName) {
		return machtkristalle.getOrDefault(kristallName, new XpProgress());
	}

	/**
	 * Gibt die Anzahl der Machtkristalle zurück (für DraggableOverlay)
	 * Gibt immer 3 zurück (maximal 3 Slots)
	 */
	public static int getMachtkristallCount() {
		return 3;
	}
}

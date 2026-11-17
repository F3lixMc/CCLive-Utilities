package net.felix.utilities.Overall.npcalerts.data;

import java.util.LinkedHashSet;
import java.util.Set;

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

	// Recycler Slots
	public static final CapacityData recyclerSlot1 = new CapacityData();

	public static final CapacityData recyclerSlot2 = new CapacityData();

	public static final CapacityData recyclerSlot3 = new CapacityData();

	/** Alle gesetzten Machtkristall-Namen aus den Slots. */
	public static Set<String> getMachtkristallNames() {
		Set<String> names = new LinkedHashSet<>();
		for (MachtkristallSlot slot : machtkristallSlots) {
			if (slot != null && slot.name != null && !slot.name.isEmpty()) {
				names.add(slot.name);
			}
		}
		return names;
	}

	/** XP-Daten für einen Machtkristall-Namen; leeres {@link XpProgress}, falls nicht gefunden. */
	public static XpProgress getMachtkristallXp(String kristallName) {
		if (kristallName == null || kristallName.isEmpty()) {
			return new XpProgress();
		}
		for (MachtkristallSlot slot : machtkristallSlots) {
			if (slot != null && kristallName.equals(slot.name) && slot.xpData != null) {
				return slot.xpData;
			}
		}
		return new XpProgress();
	}

	/**
	 * Gibt die Anzahl der Machtkristall-Slots zurück (für DraggableOverlay).
	 * Immer 3 (maximal 3 Slots).
	 */
	public static int getMachtkristallCount() {
		return 3;
	}
}

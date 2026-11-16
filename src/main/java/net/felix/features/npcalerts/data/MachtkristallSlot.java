package net.felix.features.npcalerts.data;

import net.felix.utilities.Factory.mklevel.PowerCrystalLevelUtility;

// Machtkristalle (3 Slots, in Reihenfolge)
public class MachtkristallSlot {
	public String name = null; // null wenn leer
	public XpProgress xpData = new XpProgress();
	
	public boolean isEmpty() {
		return name == null;
	}
	
	/**
	 * Prüft ob der Slot nicht gefunden wurde (keine Machtkristall-Einträge in Tab-Liste)
	 */
	public boolean isNotFound() {
		return name != null && name.isEmpty();
	}
	
	public String getDisplayText() {
		if (isEmpty()) {
			return null; // Wird von außen mit Slot-Nummer versehen
		}
		// Prüfe ob Slot nicht gefunden wurde (keine Einträge in Tab-Liste)
		if (isNotFound()) {
			return null; // Wird von außen mit Slot-Nummer versehen
		}
		// Prüfe ob Daten nicht gefunden wurden (nicht valid)
		// Wenn name gesetzt ist, aber xpData nicht valid, dann wurden die Daten nicht gefunden
		if (name != null && !name.isEmpty() && !xpData.isValid()) {
			return "MK " + name + ": Nicht im Tab-Widget";
		}
		if (name != null && !name.isEmpty()) {
			boolean showLevel = net.felix.CCLiveUtilitiesConfig.HANDLER.instance().showNpcAlertsMachtkristalleLevel;
			if (showLevel) {
				return "MK " + name + " lvl." + getLevelDisplay() + ":";
			}
			return "MK " + name + ":";
		}
		return null;
	}
	
	public String getLevelDisplay() {
		if (!xpData.isValid()) {
			return "?";
		}
		if (PowerCrystalLevelUtility.isUnknownLevel(xpData.required.longValue())) {
			return "?";
		}
		int level = PowerCrystalLevelUtility.levelFromRequiredXp(xpData.required.longValue());
		return level > 0 ? String.valueOf(level) : "?";
	}
	
	public double getPercentValue() {
		if (isEmpty() || isNotFound() || !xpData.isValid()) {
			return -1.0;
		}
		return PowerCrystalLevelUtility.calculatePercent(
			xpData.current.longValue(),
			xpData.required.longValue()
		);
	}
	
	public double getDisplayPercentValue() {
		double percent = getPercentValue();
		if (percent < 0) {
			return -1.0;
		}
		if (!net.felix.CCLiveUtilitiesConfig.HANDLER.instance().npcAlertsMachtkristallePercentOver100) {
			return Math.min(percent, 100.0);
		}
		return percent;
	}
	
	public String getPercentText() {
		double percent = getDisplayPercentValue();
		if (percent < 0) {
			return null;
		}
		return PowerCrystalLevelUtility.formatPercent(percent);
	}
	
	public static String stripIconPrefix(String displayText) {
		if (displayText == null) {
			return null;
		}
		String stripped = displayText.replaceFirst("^MK .+ (?=lvl\\.)", "");
		if (!stripped.equals(displayText)) {
			return stripped;
		}
		return displayText.replaceFirst("^MK [^:]+: ", "");
	}
	
	public String getDisplayTextForEmptySlot(int slotNumber) {
		return "MK " + slotNumber + ": -";
	}
	
	/**
	 * Gibt den Display-Text zurück, wenn der Slot nicht gefunden wurde
	 */
	public String getDisplayTextForNotFoundSlot(int slotNumber) {
		return "MK " + slotNumber + ": Nicht im Tab-Widget";
	}
	
	/**
	 * Prüft ob der Machtkristall gefunden wurde, aber die Daten nicht verfügbar sind
	 */
	public boolean isDataNotFound() {
		return !isEmpty() && !isNotFound() && !xpData.isValid();
	}
}

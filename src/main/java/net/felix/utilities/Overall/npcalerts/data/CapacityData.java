package net.felix.utilities.Overall.npcalerts.data;


// Datenstrukturen für die verschiedenen Informationen
public class CapacityData {
	public int current = -1;
	public int max = -1;
	public String currentFormatted = null; // Formatierter String (z.B. "35,241K")
	public String maxFormatted = null; // Formatierter String (z.B. "100K")
	
	public boolean isValid() {
		return current >= 0 && max >= 0;
	}
	
	public String getDisplayString() {
		if (!isValid()) return "Nicht im Tab-Widget";
		// Verwende formatierte Strings wenn verfügbar, sonst rohe Zahlen
		if (currentFormatted != null && maxFormatted != null) {
			return currentFormatted + " / " + maxFormatted;
		}
		return current + " / " + max;
	}
	
	/**
	 * Gibt den Display-String zurück, wobei das "k" Suffix aus dem current-Wert entfernt wird
	 * (für Jäger, wo "1,513k" zu "1,513" werden soll)
	 */
	public String getDisplayStringWithoutCurrentSuffix() {
		if (!isValid()) return "Nicht im Tab-Widget";
		// Verwende formatierte Strings wenn verfügbar, sonst rohe Zahlen
		if (currentFormatted != null && maxFormatted != null) {
			// Entferne "k", "K", "m", "M" etc. vom Ende des current-Werts
			String currentWithoutSuffix = currentFormatted.replaceAll("(?i)[km]$", "");
			return currentWithoutSuffix + " / " + maxFormatted;
		}
		return current + " / " + max;
	}
}

package net.felix.utilities.Overall.npcalerts.data;


public class XpProgress {
	public java.math.BigInteger current = java.math.BigInteger.valueOf(-1);
	public java.math.BigInteger required = java.math.BigInteger.valueOf(-1);
	public String currentFormatted = null; // Formatierter String (z.B. "1,234")
	public String requiredFormatted = null; // Formatierter String (z.B. "5,000")
	public String percentFormatted = null; // Formatierter Prozentwert (z.B. "10%")
	
	public boolean isValid() {
		return current.compareTo(java.math.BigInteger.ZERO) >= 0 && required.compareTo(java.math.BigInteger.ZERO) >= 0;
	}
	
	public String getDisplayString() {
		if (!isValid()) return "? / ?";
		// Verwende formatierte Strings wenn verfügbar, sonst rohe Zahlen
		if (currentFormatted != null && requiredFormatted != null) {
			return currentFormatted + " / " + requiredFormatted;
		}
		return current.toString() + " / " + required.toString();
	}
}

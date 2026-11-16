package net.felix.features.npcalerts.overlay;


/**
 * Innere Klasse für Zeilen mit Prozent-Informationen
 */
public class LineWithPercent {
	public String text;
	public String percentText;
	public boolean showPercent;
	public boolean showWarning; // Zeigt an, ob Warnung angezeigt werden soll
	public String configKey; // Für Warn-Prozentwert-Prüfung
	public boolean showIcon; // Zeigt an, ob Icon statt Text angezeigt werden soll
	
	public LineWithPercent(String text, String percentText, boolean showPercent, boolean showWarning, String configKey) {
		this(text, percentText, showPercent, showWarning, configKey, false);
	}
	
	public LineWithPercent(String text, String percentText, boolean showPercent, boolean showWarning, String configKey, boolean showIcon) {
		this.text = text;
		this.percentText = percentText;
		this.showPercent = showPercent;
		this.showWarning = showWarning;
		this.configKey = configKey;
		this.showIcon = showIcon;
	}
}

package net.felix.utilities.Overall.npcalerts.ui;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ein Eintrag der NPC-Alerts-Einstellungen (Checkbox mit Name und Zahnrad zum Detail-Screen).
 *
 * @param displayName Name in der Liste
 * @param configKey   Schluessel fuer den Detail-Screen
 * @param isEnabled   liest, ob die Information aktiviert ist
 * @param setEnabled  schaltet die Information um
 */
record NpcAlertsEntry(String displayName, String configKey, Supplier<Boolean> isEnabled, Consumer<Boolean> setEnabled) {
}

package net.felix.utilities.DragOverlay;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import net.felix.utilities.Overall.ZeichenUtility;

/**
 * Inventare mit Schmied-Kosten-Tooltips (AKTUELL / BENÖTIGT NAME) für Clipboard-Material/Ressourcen-Sync.
 */
public final class ClipboardCostInventoryUtility {

    private ClipboardCostInventoryUtility() {
    }

    public static boolean usesSchmiedCostTooltipFormat(HandledScreen<?> handledScreen) {
        if (handledScreen == null) {
            return false;
        }
        Text titleText = handledScreen.getTitle();
        return usesSchmiedCostTooltipFormat(titleText != null ? titleText.getString() : "");
    }

    public static boolean usesSchmiedCostTooltipFormat(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        String cleanTitle = cleanTitle(title);
        if (cleanTitle.contains("Baupläne [Waffen]")
                || cleanTitle.contains("Baupläne [Rüstung]")
                || cleanTitle.contains("Baupläne [Werkzeuge]")
                || cleanTitle.contains("Favorisierte [Waffenbaupläne]")
                || cleanTitle.contains("Favorisierte [Rüstungsbaupläne]")
                || cleanTitle.contains("Favorisierte [Werkzeugbaupläne]")
                || cleanTitle.contains("CACTUS_CLICKER.blueprints.favorites.title.tools")
                || cleanTitle.contains("Bauplan [Shop]")
                || cleanTitle.contains("Module [Upgraden]")
                || cleanTitle.contains("Module [Herstellen]")
                || cleanTitle.contains("[Ingenieur]")) {
            return true;
        }
        return ZeichenUtility.containsFriendsRequestAcceptDeny(title)
                || ZeichenUtility.containsConfirmationUiBackground(title);
    }

    private static String cleanTitle(String title) {
        return title.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF]", "");
    }
}

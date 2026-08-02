package net.felix.utilities.Other.Clipboard;

import net.felix.utilities.Overall.ZeichenUtility;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

/**
 * Inventare mit Schmied-Kosten-Tooltips (AKTUELL / BENÖTIGT NAME) für Clipboard-Material/Ressourcen-Sync.
 */
public final class ClipboardCostInventoryUtility {

    private ClipboardCostInventoryUtility() {
    }

    public static boolean usesSchmiedCostTooltipFormat(AbstractContainerScreen<?> handledScreen) {
        if (handledScreen == null) {
            return false;
        }
        Component titleText = handledScreen.getTitle();
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

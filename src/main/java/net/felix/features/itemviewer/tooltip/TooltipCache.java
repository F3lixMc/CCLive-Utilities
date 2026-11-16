package net.felix.features.itemviewer.tooltip;

import java.util.List;
import net.felix.features.itemviewer.data.ItemData;
import net.minecraft.network.chat.Component;

/**
 * Merkt sich den zuletzt gebauten Tooltip, damit er nicht in jedem Frame neu entsteht.
 */
public final class TooltipCache {

    private TooltipCache() {
    }

    public static ItemData lastTooltipItem = null;

    public static List<Component> cachedTooltipLines = null;

    public static void invalidateTooltipCache() {
        lastTooltipItem = null;
        cachedTooltipLines = null;
    }
}

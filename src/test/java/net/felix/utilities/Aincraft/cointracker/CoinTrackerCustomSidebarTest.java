package net.felix.utilities.Aincraft.cointracker;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoinTrackerCustomSidebarTest {
    @Test
    void materialienHeaderGainsPerMinuteOnce() {
        Component header = Component.empty()
                .append(Component.literal("▌").setStyle(Style.EMPTY.withColor(0x545454)))
                .append(Component.literal(" Materialien").setStyle(Style.EMPTY.withColor(0xFCFC54)));

        Component once = CoinTrackerCustomSidebar.withMaterialienPerMinuteHeader(header);
        assertEquals("▌ Materialien/min", once.getString());
        assertEquals(0x545454, once.getSiblings().get(0).getStyle().getColor().getValue());
        assertEquals(0xFCFC54, once.getSiblings().get(1).getStyle().getColor().getValue());

        Component twice = CoinTrackerCustomSidebar.withMaterialienPerMinuteHeader(once);
        assertEquals("▌ Materialien/min", twice.getString());
    }
}

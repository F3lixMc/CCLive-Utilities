package net.felix.utilities.Overall.clipboard.collector;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoragePickupTest {

    @Test
    void lagerKeepsBracketedAndPlainAmountBlocks() {
        Map<String, Long> parsed = AnvilFurnaceLagerCollector.parseLagerLines(List.of(
                "[Eisen]",
                "[1.234 / 5.000]",
                "[Kohle]",
                "12.345 / 20.000",
                "[Gold]",
                "[100 / 200]"
        ));
        assertEquals(1234L, parsed.get("Eisen"));
        assertEquals(12345L, parsed.get("Kohle"));
        assertEquals(100L, parsed.get("Gold"));
        assertEquals(3, parsed.size());
        Map<String, Long> withRate = AnvilFurnaceLagerCollector.parseLagerLines(List.of(
                "[Eisen]",
                "[1.234 / 5.000] (60/min)",
                "[Kohle]",
                "[0 / 5.000] (x/min)"
        ));
        assertEquals(1234L, withRate.get("Eisen"));
        assertEquals(0L, withRate.get("Kohle"));
    }

    @Test
    void sammlerAcceptsLinesWithoutColonAndIgnoresOtherRows() {
        Map<String, Long> parsed = LegendPlusSammlerCollector.parseSammlerLines(List.of(
                LegendPlusSammlerCollector.EMPTY_CONTAINERS_HINT,
                "Amboss [Eisen] 1.234/5.000 (12) [+1]",
                "Schmelzofen [Kohle] 80/100 (3)",
                "Amboss [Gold]: 10/20 (1)",
                "Kiste [Stein] 5/5 (0)"
        ));
        assertEquals(1234L, parsed.get(LegendPlusSammlerCollector.lineKey("Amboss", "Eisen")));
        assertEquals(80L, parsed.get(LegendPlusSammlerCollector.lineKey("Schmelzofen", "Kohle")));
        assertEquals(10L, parsed.get(LegendPlusSammlerCollector.lineKey("Amboss", "Gold")));
        assertFalse(parsed.containsKey(LegendPlusSammlerCollector.lineKey("Kiste", "Stein")));
        assertTrue(LegendPlusSammlerCollector.containsEmptyContainersHint(List.of(
                LegendPlusSammlerCollector.EMPTY_CONTAINERS_HINT)));
    }

    @Test
    void unchangedTooltipStaysOpenUntilZeroThenCreditsOnce() {
        ResourcePickupGate gate = new ResourcePickupGate();
        gate.arm("anvil", Map.of("Eisen", 1234L));
        Map<String, Long> same = Map.of("Eisen", 1234L);
        assertTrue(gate.observe("anvil", same, true).isEmpty());
        assertTrue(gate.isPending());
        gate.arm("anvil", Map.of("Eisen", 9999L));
        assertTrue(gate.observe("anvil", same, true).isEmpty());
        assertEquals(Map.of("Eisen", 1234L), gate.observe("anvil", Map.of("Eisen", 0L), true));
        assertFalse(gate.isPending());
        assertTrue(gate.observe("anvil", Map.of("Eisen", 0L), true).isEmpty());
    }

    @Test
    void timeoutScreenChangeAndDisconnectDiscardWithoutCredit() {
        ResourcePickupGate timeout = new ResourcePickupGate();
        timeout.arm("anvil", Map.of("Eisen", 10L));
        Map<String, Long> same = Map.of("Eisen", 10L);
        for (int tick = 0; tick < ResourcePickupGate.CONFIRM_TICKS - 1; tick++) {
            assertTrue(timeout.observe("anvil", same, true).isEmpty());
        }
        assertTrue(timeout.isPending());
        assertTrue(timeout.observe("anvil", same, true).isEmpty());
        assertFalse(timeout.isPending());

        ResourcePickupGate closed = new ResourcePickupGate();
        closed.arm("furnace", Map.of("Kohle", 4L));
        assertTrue(closed.observe("", Map.of("Kohle", 0L), true).isEmpty());
        assertFalse(closed.isPending());

        ResourcePickupGate dropped = new ResourcePickupGate();
        dropped.arm("legend", Map.of("amboss\u0000Eisen", 4L));
        assertTrue(dropped.observe("legend", Map.of("amboss\u0000Eisen", 0L), false).isEmpty());
        assertFalse(dropped.isPending());
    }

    @Test
    void sameResourceOnAnvilAndFurnaceStaysSeparateUntilCredit() {
        String anvil = LegendPlusSammlerCollector.lineKey("Amboss", "Eisen");
        String furnace = LegendPlusSammlerCollector.lineKey("Schmelzofen", "Eisen");
        ResourcePickupGate gate = new ResourcePickupGate();
        gate.arm("legend", Map.of(anvil, 10L, furnace, 5L));
        Map<String, Long> credit = gate.observe("legend", Map.of(anvil, 0L, furnace, 0L), true);
        assertEquals(Map.of("Eisen", 15L), LegendPlusSammlerCollector.collapseByResource(credit));
    }

    @Test
    void removedAmountLineCreditsWhileTheItemStays() {
        ResourcePickupGate gate = new ResourcePickupGate();
        gate.arm("anvil", Map.of("Eisen", 40L));
        assertEquals(Map.of("Eisen", 40L), gate.observe("anvil", Map.of(), true, true));
        assertFalse(gate.isPending());

        ResourcePickupGate missingItem = new ResourcePickupGate();
        missingItem.arm("anvil", Map.of("Eisen", 40L));
        assertTrue(missingItem.observe("anvil", Map.of(), true, false).isEmpty());
        assertTrue(missingItem.isPending());
    }
}

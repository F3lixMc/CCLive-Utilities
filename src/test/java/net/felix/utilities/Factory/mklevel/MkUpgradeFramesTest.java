package net.felix.utilities.Factory.mklevel;

import java.util.List;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MkUpgradeFramesTest {
    @Test
    void frameWhenXpCoinsEssenceAndXpCostReachTheirTargets() {
        assertTrue(MkUpgradeFrames.costsFulfilled(tooltip("2.000/2.000", "1.500/1.000", "20/20", "80/80")));
        assertTrue(MkUpgradeFrames.costsFulfilled(tooltip("2000/2000", "1000/1000", "20/10", "80/80")));
    }

    @Test
    void shortAnyTargetKeepsTheFrameOff() {
        assertFalse(MkUpgradeFrames.costsFulfilled(tooltip("1.500/2.000", "1.500/1.000", "20/20", "80/80")));
        assertFalse(MkUpgradeFrames.costsFulfilled(tooltip("2.000/2.000", "500/1.000", "20/20", "80/80")));
        assertFalse(MkUpgradeFrames.costsFulfilled(tooltip("2.000/2.000", "1.500/1.000", "19/20", "80/80")));
        assertFalse(MkUpgradeFrames.costsFulfilled(tooltip("2.000/2.000", "1.500/1.000", "20/20", "70/80")));
        assertFalse(MkUpgradeFrames.costsFulfilled(List.of(
                Component.literal("2.000/2.000 XP [100%]"),
                Component.literal("Kosten:"),
                Component.literal("- Coins: (1.000/1.000)"),
                Component.literal("- Feuer [Essenz] Tier 2: (20/20)"),
                Component.literal("- Eis [Essenz] Tier 1: (1/5)"),
                Component.literal("- XP: (80/80)")
        )));
    }

    private static List<Component> tooltip(String progress, String coins, String essence, String xpCost) {
        return List.of(
                Component.literal("Machtkristall"),
                Component.literal(""),
                Component.literal("xpbar"),
                Component.literal(progress + " XP [75%]"),
                Component.literal("Kosten:"),
                Component.literal("- Coins: (" + coins + ")"),
                Component.literal("- Feuer [Essenz] Tier 2: (" + essence + ")"),
                Component.literal("- XP: (" + xpCost + ")")
        );
    }
}

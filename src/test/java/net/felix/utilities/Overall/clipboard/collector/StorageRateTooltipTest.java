package net.felix.utilities.Overall.clipboard.collector;

import java.util.ArrayList;
import java.util.List;

import net.felix.utilities.Overall.clipboard.collector.StorageAmountRate;
import net.felix.utilities.Overall.clipboard.collector.StorageRateTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageRateTooltipTest {
    @Test
    void lagerAmountKeepsTheLineAndAddsAnUprightAquaRate() {
        List<Component> lines = new ArrayList<>(List.of(
                Component.literal("Lager").withStyle(ChatFormatting.WHITE),
                Component.literal("[Eisen]").withStyle(ChatFormatting.GRAY),
                Component.literal("[1.234 / 5.000]").withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withItalic(true))
        ));
        StorageRateTooltip.decorateLager(lines, "anvil");
        Component amount = lines.get(2);
        assertTrue(amount.getString().startsWith("[1.234 / 5.000] ("));
        assertTrue(amount.getString().endsWith("/min)"));
        Style rate = amount.getSiblings().get(0).getStyle();
        assertEquals(aqua(), rate.getColor().getValue());
        assertFalse(rate.isItalic());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW).getValue(), amount.getStyle().getColor().getValue());
    }

    @Test
    void legendRateKeepsTheServerColors() {
        Component line = Component.literal("Amboss [Eisen] 129/307 ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("(283) [+1]").withStyle(ChatFormatting.GRAY));
        Component decorated = StorageRateTooltip.insertLegendRate(line, 1_000L);
        assertEquals("Amboss [Eisen] 129/307 (x/min) (283) [+1]", decorated.getString());
        boolean gold = false;
        boolean aquaRate = false;
        boolean gray = false;
        for (Component sibling : decorated.getSiblings()) {
            TextColor color = sibling.getStyle().getColor();
            if (color == null) {
                continue;
            }
            if (color.getValue() == TextColor.fromLegacyFormat(ChatFormatting.GOLD).getValue()) {
                gold = true;
            }
            if (color.getValue() == aqua() && !sibling.getStyle().isItalic() && sibling.getString().contains("/min")) {
                aquaRate = true;
            }
            if (color.getValue() == TextColor.fromLegacyFormat(ChatFormatting.GRAY).getValue()
                    && sibling.getString().contains("(283)")) {
                gray = true;
            }
        }
        assertTrue(gold);
        assertTrue(aquaRate);
        assertTrue(gray);
    }

    @Test
    void amountOverTimeBecomesTheDisplayedRate() {
        StorageAmountRate rate = new StorageAmountRate();
        assertEquals(0.0, rate.observe("anvil\u0000Eisen", 100L, 0L));
        assertEquals(0.0, rate.observe("anvil\u0000Eisen", 100L, 30_000L));
        assertEquals(60.0, rate.observe("anvil\u0000Eisen", 160L, 60_000L));
        assertEquals(60.0, rate.observe("anvil\u0000Eisen", 10L, 61_000L));
        rate.observe("anvil\u0000Eisen", 100L, 0L);
        rate.observe("anvil\u0000Eisen", 160L, 60_000L);
        rate.replaceScope("anvil", java.util.Map.of("Gold", 40L), 70_000L);
        assertEquals(0.0, rate.rate("anvil\u0000Eisen"));
        assertEquals(0.0, rate.rateForResource("Gold"));
        assertEquals(30.0, rate.observe("anvil\u0000Gold", 70L, 130_000L));
    }

    @Test
    void harvesterAmountLineKeepsTheCurrentCount() {
        var reading = net.felix.utilities.Overall.clipboard.collector.HarvesterLootRate.read(List.of(
                Component.literal("Loot [Sammeln]"),
                Component.literal("Feuer [Essenz]"),
                Component.literal("Tier 3"),
                Component.literal("Nächste Essenz in: 12"),
                Component.literal("1.200/5.000")
        ));
        org.junit.jupiter.api.Assertions.assertEquals(1200L, reading.amount());
        org.junit.jupiter.api.Assertions.assertTrue(reading.rateKey().contains("Feuer [Essenz]"));
        org.junit.jupiter.api.Assertions.assertTrue(reading.rateKey().contains("Tier 3"));
    }

    private static int aqua() {
        return TextColor.fromLegacyFormat(ChatFormatting.AQUA).getValue();
    }
}

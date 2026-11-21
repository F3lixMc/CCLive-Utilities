package net.felix.utilities.Other.clipboard.collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.felix.utilities.Aincraft.materials.tracker.MaterialRateUtility;
import net.felix.utilities.Other.glyphs.ZeichenUtility;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

/**
 * Hängt die Produktionsrate an die Mengen-Zeile. Die Rate kommt aus der Veränderung der aktuellen Menge.
 */
public final class StorageRateTooltip {

    static final StorageAmountRate RATES = new StorageAmountRate();

    private static final Pattern AMOUNT_MAX_PATTERN = Pattern.compile(
            "^(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*$");
    private static final Pattern BRACKETED_AMOUNT_MAX_PATTERN = Pattern.compile(
            "^\\[(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\]\\s*$");
    private static final Pattern BRACKETED_RESOURCE_NAME_PATTERN = Pattern.compile("^\\[([^\\]]+)]\\s*$");
    private static final Pattern SAMMLER_RESOURCE_PATTERN = Pattern.compile(
            "^(Amboss|Schmelzofen)\\s+\\[([^\\]]+)]\\s*:?\\s*(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(?:\\d{1,3}(?:[.,]\\d{3})*|\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Style AQUA = Style.EMPTY.withColor(ChatFormatting.AQUA).withItalic(false);

    private static int sampleTick;

    private StorageRateTooltip() {
    }

    public static void initialize() {
        ItemTooltipCallback.EVENT.register((stack, context, tooltipType, lines) -> decorate(stack, lines));
        ClientTickEvents.END_CLIENT_TICK.register(StorageRateTooltip::sampleOpenMenu);
    }

    static void decorate(ItemStack stack, List<Component> lines) {
        if (stack == null || stack.isEmpty() || lines == null || lines.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui.screen() == null || client.gui.screen().getTitle() == null) {
            return;
        }
        String title = client.gui.screen().getTitle().getString();
        String itemName = clean(stack.getHoverName().getString());
        if (isLagerName(itemName) && ZeichenUtility.containsAnvilMainUi(title)) {
            decorateLager(lines, "anvil");
        } else if (isLagerName(itemName) && ZeichenUtility.containsFurnaceMainUi(title)) {
            decorateLager(lines, "furnace");
        } else if ("[Sammler]".equalsIgnoreCase(itemName) && ZeichenUtility.containsLegendPlusUiBackground(title)) {
            decorateLegend(lines);
        } else if (HarvesterLootRate.isLootItem(itemName) && ZeichenUtility.containsEssenceHarvesterUi(title)) {
            decorateHarvester(lines);
        }
    }

    private static void sampleOpenMenu(Minecraft client) {
        sampleTick++;
        if (sampleTick % 20 != 0 || client == null || client.gui.screen() == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)
                || screen.getTitle() == null) {
            return;
        }
        String title = screen.getTitle().getString();
        long now = System.currentTimeMillis();
        if (ZeichenUtility.containsAnvilMainUi(title) || ZeichenUtility.containsFurnaceMainUi(title)) {
            ItemStack lager = AnvilFurnaceLagerCollector.findLagerStack(screen);
            if (lager == null) {
                return;
            }
            String scope = ZeichenUtility.containsAnvilMainUi(title) ? "anvil" : "furnace";
            RATES.replaceScope(scope, AnvilFurnaceLagerCollector.parseLagerResources(tooltipOf(lager)), now);
        } else if (ZeichenUtility.containsLegendPlusUiBackground(title)) {
            ItemStack sammler = LegendPlusSammlerCollector.findSammlerStack(screen);
            if (sammler == null) {
                return;
            }
            RATES.replaceScope("legend", LegendPlusSammlerCollector.parseSammlerResources(tooltipOf(sammler)), now);
        } else if (ZeichenUtility.containsEssenceHarvesterUi(title) && screen.getMenu() != null) {
            for (net.minecraft.world.inventory.Slot slot : screen.getMenu().slots) {
                if (!slot.hasItem()) {
                    continue;
                }
                ItemStack stack = slot.getItem();
                if (!HarvesterLootRate.isLootItem(clean(stack.getHoverName().getString()))) {
                    continue;
                }
                HarvesterLootRate.Reading reading = HarvesterLootRate.read(tooltipOf(stack));
                if (reading != null) {
                    RATES.observe(reading.rateKey(), reading.amount(), now);
                }
            }
        }
    }

    public static double rateForItem(String itemName) {
        double direct = RATES.rateForResource(itemName);
        if (direct > 0.0 || itemName == null) {
            return direct;
        }
        String trimmed = itemName.trim();
        if (trimmed.regionMatches(true, Math.max(0, trimmed.length() - 6), "barren", 0, 6) && trimmed.length() > 6) {
            double ore = RATES.rateForResource(trimmed.substring(0, trimmed.length() - 6).trim());
            if (ore > 0.0) {
                return ore;
            }
        }
        double bars = RATES.rateForResource(trimmed + "barren");
        if (bars > 0.0) {
            return bars;
        }
        if (trimmed.equalsIgnoreCase("Netherite Schrott")) {
            for (String alias : new String[] {"Netherite", "Antiker Schrott", "Ancient Debris"}) {
                double rate = RATES.rateForResource(alias);
                if (rate > 0.0) {
                    return rate;
                }
            }
        }
        return 0.0;
    }

    static void decorateLager(List<Component> lines, String scope) {
        String pendingName = null;
        long now = System.currentTimeMillis();
        java.util.Map<String, Long> amounts = new java.util.LinkedHashMap<>();
        for (Component line : lines) {
            String clean = StorageLineText.withoutRate(clean(line.getString()));
            if (isLagerAmountLine(clean)) {
                if (pendingName != null && !pendingName.isEmpty()) {
                    amounts.put(pendingName, currentAmount(clean));
                }
                pendingName = null;
                continue;
            }
            Matcher bracketedName = BRACKETED_RESOURCE_NAME_PATTERN.matcher(clean);
            if (bracketedName.matches()) {
                pendingName = clean(bracketedName.group(1));
            } else if (!clean.isEmpty() && !isLagerName(clean)) {
                pendingName = unwrap(clean);
            }
        }
        RATES.replaceScope(scope, amounts, now);
        pendingName = null;
        for (int index = 0; index < lines.size(); index++) {
            String clean = StorageLineText.withoutRate(clean(lines.get(index).getString()));
            if (isLagerAmountLine(clean)) {
                if (pendingName != null && !pendingName.isEmpty()) {
                    lines.set(index, appendRate(lines.get(index), scope + "\u0000" + pendingName));
                }
                pendingName = null;
                continue;
            }
            Matcher bracketedName = BRACKETED_RESOURCE_NAME_PATTERN.matcher(clean);
            if (bracketedName.matches()) {
                pendingName = clean(bracketedName.group(1));
            } else if (!clean.isEmpty() && !isLagerName(clean)) {
                pendingName = unwrap(clean);
            }
        }
    }

    static void decorateHarvester(List<Component> lines) {
        HarvesterLootRate.Reading reading = HarvesterLootRate.read(lines);
        if (reading == null) {
            return;
        }
        RATES.observe(reading.rateKey(), reading.amount(), System.currentTimeMillis());
        int index = reading.amountLine();
        if (index >= 0 && index < lines.size()) {
            lines.set(index, appendRate(lines.get(index), reading.rateKey()));
        }
    }

    static void decorateLegend(List<Component> lines) {
        long now = System.currentTimeMillis();
        java.util.Map<String, Long> amounts = new java.util.LinkedHashMap<>();
        for (Component line : lines) {
            String trimmed = line.getString().trim();
            Matcher matcher = SAMMLER_RESOURCE_PATTERN.matcher(trimmed);
            if (!matcher.find()) {
                continue;
            }
            String source = matcher.group(1).toLowerCase(java.util.Locale.ROOT);
            amounts.put(source + "\u0000" + clean(matcher.group(2)), parseAmount(matcher.group(3)));
        }
        RATES.replaceScope("legend", amounts, now);
        for (int index = 0; index < lines.size(); index++) {
            lines.set(index, insertLegendRate(lines.get(index), now));
        }
    }

    static boolean isLagerAmountLine(String line) {
        return line != null && (BRACKETED_AMOUNT_MAX_PATTERN.matcher(line).matches()
                || AMOUNT_MAX_PATTERN.matcher(unwrap(line)).matches());
    }

    static Component appendRate(Component line, String rateKey) {
        if (line == null) {
            return Component.empty();
        }
        String suffix = rateSuffix(rateKey);
        if (line.getString().contains(suffix)) {
            return line;
        }
        MutableComponent copy = line.copy();
        copy.append(Component.literal(suffix).setStyle(AQUA));
        return copy;
    }

    static Component insertLegendRate(Component line, long now) {
        if (line == null) {
            return Component.empty();
        }
        String visible = line.getString();
        String trimmed = visible.trim();
        Matcher matcher = SAMMLER_RESOURCE_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return line;
        }
        String source = matcher.group(1).toLowerCase(java.util.Locale.ROOT);
        String resource = clean(matcher.group(2));
        String rateKey = "legend\u0000" + source + "\u0000" + resource;
        String suffix = rateSuffix(rateKey);
        if (trimmed.contains(suffix)) {
            return line;
        }
        int lead = Math.max(0, visible.indexOf(trimmed));
        return insertStyled(line, lead + matcher.end(), suffix);
    }

    static Component insertStyled(Component line, int split, String insertion) {
        List<Run> runs = new ArrayList<>();
        line.visit((style, text) -> {
            if (text != null && !text.isEmpty()) {
                runs.add(new Run(style, text));
            }
            return Optional.empty();
        }, Style.EMPTY);
        MutableComponent result = Component.empty();
        int cursor = 0;
        boolean inserted = false;
        for (Run run : runs) {
            int end = cursor + run.text.length();
            if (!inserted && split <= cursor) {
                result.append(Component.literal(insertion).setStyle(AQUA));
                inserted = true;
            }
            if (inserted || split >= end) {
                result.append(Component.literal(run.text).setStyle(run.style));
            } else {
                int cut = split - cursor;
                result.append(Component.literal(run.text.substring(0, cut)).setStyle(run.style));
                result.append(Component.literal(insertion).setStyle(AQUA));
                inserted = true;
                if (cut < run.text.length()) {
                    result.append(Component.literal(run.text.substring(cut)).setStyle(run.style));
                }
            }
            cursor = end;
        }
        if (!inserted) {
            result.append(Component.literal(insertion).setStyle(AQUA));
        }
        return result;
    }

    private static String rateSuffix(String rateKey) {
        return " (" + MaterialRateUtility.formatRate(RATES.rate(rateKey)) + ")";
    }

    private static long currentAmount(String line) {
        Matcher bracketed = BRACKETED_AMOUNT_MAX_PATTERN.matcher(line);
        if (bracketed.matches()) {
            return parseAmount(bracketed.group(1));
        }
        Matcher plain = AMOUNT_MAX_PATTERN.matcher(unwrap(line));
        return plain.matches() ? parseAmount(plain.group(1)) : 0L;
    }

    private static long parseAmount(String amountText) {
        if (amountText == null) {
            return 0L;
        }
        String cleaned = amountText.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static List<Component> tooltipOf(ItemStack stack) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(stack.getHoverName());
        var lore = stack.get(net.minecraft.core.component.DataComponents.LORE);
        if (lore != null) {
            tooltip.addAll(lore.lines());
        }
        return tooltip;
    }

    private static boolean isLagerName(String name) {
        return "Lager".equalsIgnoreCase(unwrap(name));
    }

    private static String unwrap(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static String clean(String line) {
        if (line == null) {
            return "";
        }
        return line.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF]", "")
                .trim();
    }

    private record Run(Style style, String text) {
    }
}

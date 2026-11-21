package net.felix.utilities.Other.clipboard.collector;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;

/**
 * Menge im Essence-Harvester auf einem {@code Loot [Sammeln]}-Item.
 * Die Zeile {@code aktuell/max} bekommt dieselbe Rate wie das Amboss-Lager.
 */
final class HarvesterLootRate {

    private static final Pattern AMOUNT = Pattern.compile(
            "^(\\d{1,3}(?:[.,]\\d{3})*|\\d+)\\s*/\\s*(?:\\d{1,3}(?:[.,]\\d{3})*|\\d+)$");

    record Reading(String rateKey, long amount, int amountLine) {
    }

    private HarvesterLootRate() {
    }

    static boolean isLootItem(String name) {
        if (name == null) {
            return false;
        }
        String lower = clean(name).toLowerCase(Locale.ROOT);
        return lower.contains("loot") && lower.contains("[sammeln]");
    }

    static Reading read(List<Component> lines) {
        if (lines == null) {
            return null;
        }
        String essence = "";
        String tier = "";
        int amountLine = -1;
        long amount = -1L;
        for (int index = 0; index < lines.size(); index++) {
            String text = StorageLineText.withoutRate(clean(lines.get(index).getString()));
            if (text.isEmpty() || isLootItem(text)) {
                continue;
            }
            if (text.contains("[Essenz]")) {
                essence = text;
            } else if (text.regionMatches(true, 0, "Tier", 0, 4)) {
                tier = text;
            } else {
                Matcher matcher = AMOUNT.matcher(text);
                if (matcher.matches()) {
                    amountLine = index;
                    amount = digits(matcher.group(1));
                }
            }
        }
        if (essence.isEmpty() || amountLine < 0) {
            return null;
        }
        return new Reading("harvester\u0000" + essence + "\u0000" + tier, amount, amountLine);
    }

    private static long digits(String amountText) {
        String cleaned = amountText == null ? "" : amountText.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String clean(String line) {
        if (line == null) {
            return "";
        }
        return line.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("[\\u3400-\\u4DBF]", "")
                .trim();
    }
}

package net.felix.Community.chat.share;

import net.felix.utilities.Overall.itemviewer.data.ItemData;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.felix.Community.chat.ChatTooltipLine;

/**
 * „Magnet [Modul]“ hat in der JEI einen Namen pro Stufe.
 * I = Kupfermagnet, II = Eisenmagnet, III = Goldmagnet, IV = Diamantmagnet.
 */
public final class MagnetModuleNames {
    private static final Pattern STUFE = Pattern.compile(
            "Stufe:\\s*\\[(IV|III|II|I)\\]", Pattern.CASE_INSENSITIVE);

    private MagnetModuleNames() {
    }

    public static String jeiName(String rawName, List<ChatTooltipLine> tooltip) {
        if (!isMagnet(rawName)) {
            return null;
        }
        String tier = tier(tooltip);
        if (tier == null) {
            return null;
        }
        return switch (tier) {
            case "I" -> "Kupfermagnet";
            case "II" -> "Eisenmagnet";
            case "III" -> "Goldmagnet";
            case "IV" -> "Diamantmagnet";
            default -> null;
        };
    }

    public static void apply(ItemData data) {
        if (data == null) {
            return;
        }
        String jei = jeiName(data.name, data.chatTooltip);
        if (jei == null) {
            return;
        }
        data.name = jei;
        replaceFirstVisibleLine(data.chatTooltip, jei);
    }

    private static boolean isMagnet(String rawName) {
        String name = ShareTextCleaner.cleanDisplayName(rawName);
        if (name.isEmpty()) {
            return false;
        }
        if (name.endsWith("]")) {
            int start = name.lastIndexOf('[');
            if (start >= 0 && "modul".equalsIgnoreCase(name.substring(start + 1, name.length() - 1).trim())) {
                name = name.substring(0, start).trim();
            }
        }
        return "magnet".equalsIgnoreCase(name);
    }

    private static String tier(List<ChatTooltipLine> tooltip) {
        if (tooltip == null) {
            return null;
        }
        for (ChatTooltipLine line : tooltip) {
            if (line == null) {
                continue;
            }
            String plain = ShareTextCleaner.cleanDisplayName(line.plainText());
            Matcher matcher = STUFE.matcher(plain);
            if (matcher.find()) {
                return matcher.group(1).toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }

    private static void replaceFirstVisibleLine(List<ChatTooltipLine> tooltip, String jeiName) {
        if (tooltip == null) {
            return;
        }
        for (ChatTooltipLine line : tooltip) {
            if (line == null || line.parts == null) {
                continue;
            }
            String plain = line.plainText();
            if (plain == null || plain.trim().isEmpty()) {
                continue;
            }
            Integer color = null;
            for (ChatTooltipLine.ChatTooltipPart part : line.parts) {
                if (part != null && part.text != null && !part.text.trim().isEmpty()) {
                    color = part.color;
                    break;
                }
            }
            ChatTooltipLine.ChatTooltipPart kept = new ChatTooltipLine.ChatTooltipPart();
            kept.text = jeiName;
            kept.color = color;
            line.parts.clear();
            line.parts.add(kept);
            return;
        }
    }
}

package net.felix.Community.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * Sichtbarer Share-Name und seine lokale Farbe.
 * PlayLegend bekommt nur den Klartext. Level und Schmiedegrad bleiben im Tooltip.
 */
public final class ChatShareNames {
    public static final int BUILD_COLOR = 0x55FFFF;

    public record Piece(String text, Integer color) {
    }

    private ChatShareNames() {
    }

    /** Level-Präfix und erkannter Schmiedegrad weg, der übrige Itemname bleibt. */
    public static String itemChatName(String raw) {
        String cleaned = ShareTextCleaner.cleanDisplayName(raw);
        if (cleaned.isEmpty()) {
            return "";
        }
        String withoutLevel = stripLeadingLevel(cleaned);
        if (withoutLevel.isEmpty()) {
            withoutLevel = cleaned;
        }
        String base = ShareKitImportHelper.extractEquipmentBaseName(withoutLevel);
        return base.isEmpty() ? withoutLevel : base;
    }

    /** Farbe nur des bereinigten Namensabschnitts. Ohne Treffer bleibt der Chat unverändert. */
    public static Integer itemNameColor(List<ChatTooltipLine> tooltip, String chatName) {
        if (tooltip == null || chatName == null || chatName.isEmpty()) {
            return null;
        }
        for (ChatTooltipLine line : tooltip) {
            if (line == null || line.parts == null || line.plainText() == null || line.plainText().isBlank()) {
                continue;
            }
            List<Piece> pieces = new ArrayList<>();
            for (ChatTooltipLine.ChatTooltipPart part : line.parts) {
                if (part == null) {
                    continue;
                }
                pieces.add(new Piece(part.text, part.color));
            }
            return colorOfItemName(pieces, chatName);
        }
        return null;
    }

    public static Integer colorOfItemName(List<Piece> pieces, String chatName) {
        if (pieces == null || chatName == null || chatName.isEmpty()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        List<Integer> colors = new ArrayList<>();
        boolean[] started = {false};
        for (Piece piece : pieces) {
            appendVisible(piece == null ? null : piece.text(), piece == null ? null : piece.color(), text, colors, started);
        }
        trimTrailingSpace(text, colors);
        int at = text.indexOf(chatName);
        if (at < 0) {
            return null;
        }
        int[] bestCount = {-1};
        Integer[] best = {null};
        for (int i = at; i < at + chatName.length() && i < colors.size(); i++) {
            Integer color = validRgb(colors.get(i));
            if (color == null) {
                continue;
            }
            int count = 0;
            for (int j = at; j < at + chatName.length() && j < colors.size(); j++) {
                if (color.equals(validRgb(colors.get(j)))) {
                    count++;
                }
            }
            if (count > bestCount[0]) {
                bestCount[0] = count;
                best[0] = color;
            }
        }
        return best[0];
    }

    /** Build ist immer Aqua. Item nur bei einem gültigen RGB-Wert. */
    public static Integer chatColor(String shareType, Integer nameColor) {
        if ("BUILD".equals(shareType)) {
            return BUILD_COLOR;
        }
        return validRgb(nameColor);
    }

    public static Integer validRgb(Integer value) {
        if (value == null || value < 0 || value > 0xFFFFFF) {
            return null;
        }
        return value;
    }

    private static String stripLeadingLevel(String name) {
        String next = name.replaceFirst("^\\[\\+\\d+\\]\\s*", "");
        next = next.replaceFirst("^\\+\\d+\\s+", "");
        return next.trim();
    }

    private static void appendVisible(String raw, Integer color, StringBuilder text, List<Integer> colors, boolean[] started) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        String piece = ShareTextCleaner.stripLayoutGlyphsPreserveSpaces(raw)
                .replaceAll("§#[0-9a-fA-F]{6}", "")
                .replaceAll("§[0-9a-fk-or]", "");
        for (int i = 0; i < piece.length(); i++) {
            char ch = piece.charAt(i);
            if (Character.isWhitespace(ch)) {
                if (!started[0] || text.isEmpty() || text.charAt(text.length() - 1) == ' ') {
                    continue;
                }
                text.append(' ');
                colors.add(color);
                continue;
            }
            started[0] = true;
            text.append(ch);
            colors.add(color);
        }
    }

    private static void trimTrailingSpace(StringBuilder text, List<Integer> colors) {
        while (!text.isEmpty() && text.charAt(text.length() - 1) == ' ') {
            text.setLength(text.length() - 1);
            colors.remove(colors.size() - 1);
        }
    }
}

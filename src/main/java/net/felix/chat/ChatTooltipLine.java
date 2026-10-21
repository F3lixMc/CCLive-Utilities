package net.felix.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Eine Tooltip-Zeile mit formatierten Segmenten (Farbe/Stil) für Chat-Item-Shares.
 */
public class ChatTooltipLine {
    public List<ChatTooltipPart> parts = new ArrayList<>();

    public static class ChatTooltipPart {
        public String text = "";
        public Integer color; // 0xRRGGBB
        public Boolean italic;
        public Boolean bold;
        public Boolean underlined;
        public Boolean strikethrough;
    }

    public static ChatTooltipLine fromComponent(Component line) {
        ChatTooltipLine out = new ChatTooltipLine();
        if (line != null) {
            flatten(line, Style.EMPTY, out.parts);
        }
        if (out.parts.isEmpty()) {
            ChatTooltipPart part = new ChatTooltipPart();
            part.text = line != null ? line.getString() : "";
            out.parts.add(part);
        }
        return out;
    }

    public Component toComponent() {
        MutableComponent result = Component.empty();
        if (parts == null || parts.isEmpty()) {
            return result;
        }
        boolean leading = true;
        for (ChatTooltipPart part : parts) {
            if (part == null || part.text == null) {
                continue;
            }
            // §-Farben in Style übernehmen, falls part.color fehlt
            Integer color = part.color;
            String raw = part.text;
            if (color == null) {
                ParsedColoredText parsed = parseLeadingSectionColor(raw);
                color = parsed.color;
                raw = parsed.text;
            }
            String cleaned = ShareTextCleaner.stripLayoutGlyphsPreserveSpaces(raw);
            if (leading) {
                cleaned = trimLeadingWhitespace(cleaned);
            }
            // Space-only-Parts behalten (Abstand zwischen Farbsegmenten)
            boolean spaceOnly = !cleaned.isEmpty() && cleaned.trim().isEmpty();
            if (cleaned.isEmpty()) {
                continue;
            }
            if (leading && spaceOnly) {
                continue;
            }
            leading = false;

            Style style = Style.EMPTY.withItalic(false);
            if (color != null) {
                style = style.withColor(color & 0xFFFFFF);
            }
            if (Boolean.TRUE.equals(part.italic)) {
                style = style.withItalic(true);
            }
            if (Boolean.TRUE.equals(part.bold)) {
                style = style.withBold(true);
            }
            if (Boolean.TRUE.equals(part.underlined)) {
                style = style.withUnderlined(true);
            }
            if (Boolean.TRUE.equals(part.strikethrough)) {
                style = style.withStrikethrough(true);
            }
            result.append(Component.literal(cleaned).setStyle(style));
        }
        return result;
    }

    private static String trimLeadingWhitespace(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return s.substring(i);
    }

    private static final class ParsedColoredText {
        final Integer color;
        final String text;

        ParsedColoredText(Integer color, String text) {
            this.color = color;
            this.text = text;
        }
    }

    /** Optional führendes §#RRGGBB / §[0-9a-f] in Farbe + Resttext zerlegen. */
    private static ParsedColoredText parseLeadingSectionColor(String text) {
        if (text == null || text.isEmpty()) {
            return new ParsedColoredText(null, text != null ? text : "");
        }
        Integer color = null;
        String rest = text;
        if (rest.startsWith("§#") && rest.length() >= 8) {
            try {
                color = Integer.parseInt(rest.substring(2, 8), 16);
                rest = rest.substring(8);
            } catch (Exception ignored) {
            }
        } else if (rest.length() >= 2 && rest.charAt(0) == '§') {
            char code = Character.toLowerCase(rest.charAt(1));
            Integer mapped = vanillaColor(code);
            if (mapped != null) {
                color = mapped;
                rest = rest.substring(2);
            }
        }
        // weitere §-Codes im Rest entfernen, Farbe behalten
        rest = rest.replaceAll("§#[0-9a-fA-F]{6}", "").replaceAll("§[0-9a-fk-or]", "");
        return new ParsedColoredText(color, rest);
    }

    private static Integer vanillaColor(char code) {
        return switch (code) {
            case '0' -> 0x000000;
            case '1' -> 0x0000AA;
            case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            case 'f' -> 0xFFFFFF;
            default -> null;
        };
    }

    public String plainText() {
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatTooltipPart part : parts) {
            if (part != null && part.text != null) {
                sb.append(part.text);
            }
        }
        return sb.toString();
    }

    private static void flatten(Component component, Style parent, List<ChatTooltipPart> out) {
        Style style = component.getStyle();
        Style merged = style != null ? style.applyTo(parent) : parent;
        String own = extractOwnLiteral(component);
        if (own != null && !own.isEmpty()) {
            ChatTooltipPart part = new ChatTooltipPart();
            part.text = own;
            TextColor color = merged.getColor();
            if (color != null) {
                part.color = color.getValue() & 0xFFFFFF;
            }
            if (merged.isItalic()) {
                part.italic = true;
            }
            if (merged.isBold()) {
                part.bold = true;
            }
            if (merged.isUnderlined()) {
                part.underlined = true;
            }
            if (merged.isStrikethrough()) {
                part.strikethrough = true;
            }
            out.add(part);
        }
        for (Component sibling : component.getSiblings()) {
            flatten(sibling, merged, out);
        }
    }

    private static String extractOwnLiteral(Component component) {
        try {
            Object contents = component.getContents();
            if (contents != null) {
                for (java.lang.reflect.Method method : contents.getClass().getMethods()) {
                    if (method.getParameterCount() != 0 || method.getReturnType() != String.class) {
                        continue;
                    }
                    String name = method.getName();
                    if ("text".equals(name) || "string".equals(name) || "getString".equals(name) || "literal".equals(name)) {
                        try {
                            Object value = method.invoke(contents);
                            if (value instanceof String s) {
                                return s;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (component.getSiblings().isEmpty()) {
            return component.getString();
        }
        return "";
    }

    /** Vanilla-Attribute-Blöcke + Advanced-Tooltips herausfiltern. */
    public static boolean shouldOmitTooltipLine(Component line) {
        if (line == null) {
            return true;
        }
        return shouldOmitPlainText(line.getString());
    }

    public static boolean shouldOmitPlainText(String text) {
        if (text == null) {
            return true;
        }
        String raw = text;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return false; // leere Zeilen behalten (Absätze)
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);

        // Advanced: Item-ID / Resource-Location
        if (lower.matches("^[a-z0-9_.-]+:[a-z0-9_./-]+$")) {
            return true;
        }
        if (lower.startsWith("minecraft:")) {
            return true;
        }
        if (lower.startsWith("nbt:")) {
            return true;
        }
        if (lower.matches("^\\d+\\s+component\\(s\\)$")) {
            return true;
        }

        // Attribute-Header EN/DE
        if (lower.startsWith("when in main hand")
                || lower.startsWith("when in off hand")
                || lower.startsWith("when on ")
                || lower.startsWith("wenn in der haupthand")
                || lower.startsWith("wenn in der nebenhand")
                || lower.startsWith("wenn an ")) {
            return true;
        }

        // Eingrückte Vanilla-Attribute-Werte (" 7 Attack Damage")
        if (raw.matches("(?s)^\\s+[+-]?[\\d.,]+\\s+.+")) {
            if (lower.contains("attack")
                    || lower.contains("damage")
                    || lower.contains("speed")
                    || lower.contains("armor")
                    || lower.contains("knockback")
                    || lower.contains("angriff")
                    || lower.contains("schaden")
                    || lower.contains("geschwindigkeit")
                    || lower.contains("rüstung")
                    || lower.contains("ruestung")
                    || lower.contains("rückstoß")
                    || lower.contains("rueckstoss")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Inventar-Bedienhinweise, die nur in Share-Tooltips ausgeblendet werden dürfen.
     * Nicht in {@link #shouldOmitPlainText} / {@link #filterInventoryTooltip} — normales Inventar behält die Hinweise.
     */
    public static boolean shouldOmitShareInventoryActionHint(String text) {
        if (text == null) {
            return false;
        }
        String normalized = text
            .replaceAll("§#[0-9a-fA-F]{6}", "")
            .replaceAll("§[0-9a-fk-or]", "")
            .replaceAll("\\s+", " ")
            .trim();
        if (normalized.isEmpty()) {
            return false;
        }
        return "[Linksklick] Ausrüstung auswählen".equals(normalized)
            || "[Rechtsklick] Cosmetic auswählen".equals(normalized);
    }

    /** Share-Anzeige: Action-Hinweise weglassen und trailing Leerzeilen strippen. */
    public static List<ChatTooltipLine> filterShareDisplayTooltipLines(List<ChatTooltipLine> lines) {
        List<ChatTooltipLine> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (ChatTooltipLine line : lines) {
            if (line == null) {
                continue;
            }
            String plain = line.plainText();
            if (shouldOmitShareInventoryActionHint(plain)) {
                continue;
            }
            out.add(line);
        }
        while (!out.isEmpty()) {
            ChatTooltipLine last = out.get(out.size() - 1);
            String plain = last != null ? last.plainText() : "";
            if (plain == null || plain.trim().isEmpty()) {
                out.remove(out.size() - 1);
            } else {
                break;
            }
        }
        return out;
    }

    /**
     * Nach Attribute-Header folgende eingerückte Zeilen überspringen.
     */
    public static List<Component> filterInventoryTooltip(List<Component> tooltip) {
        List<Component> filtered = new ArrayList<>();
        if (tooltip == null) {
            return filtered;
        }
        boolean skippingAttributeBlock = false;
        for (Component line : tooltip) {
            if (line == null) {
                continue;
            }
            String raw = line.getString();
            String trimmed = raw != null ? raw.trim() : "";
            String lower = trimmed.toLowerCase(Locale.ROOT);

            if (lower.startsWith("when in main hand")
                    || lower.startsWith("when in off hand")
                    || lower.startsWith("when on ")
                    || lower.startsWith("wenn in der haupthand")
                    || lower.startsWith("wenn in der nebenhand")
                    || lower.startsWith("wenn an ")) {
                skippingAttributeBlock = true;
                continue;
            }

            if (skippingAttributeBlock) {
                if (trimmed.isEmpty()) {
                    continue;
                }
                // Weiterhin eingerückt = Attributzeile
                if (raw != null && !raw.isEmpty() && Character.isWhitespace(raw.charAt(0))) {
                    continue;
                }
                skippingAttributeBlock = false;
            }

            if (shouldOmitPlainText(raw)) {
                continue;
            }
            filtered.add(line);
        }
        return filtered;
    }
}

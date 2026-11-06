package net.felix.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Namensfarben inkl. Gradient: eine RGB-Farbe pro Buchstaben.
 */
public final class ChatNameColors {
    public static final int WHITE = 0xFFFFFF;
    private static final int MAX_NAME_LEN = 16;
    private static final char ICON_MARKER = 'ѳ';

    private ChatNameColors() {
    }

    public static final class Sample {
        public final String name;
        public final int[] colors;

        public Sample(String name, int[] colors) {
            this.name = name;
            this.colors = colors;
        }
    }

    public static String encode(int[] colors) {
        if (colors == null || colors.length == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(colors.length * 8);
        for (int i = 0; i < colors.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(toHex(colors[i]));
        }
        return out.toString();
    }

    public static String toHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & WHITE);
    }

    public static int parseSolid(String raw) {
        int[] parsed = parse(raw);
        return parsed.length == 0 ? WHITE : parsed[0];
    }

    public static int[] parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new int[0];
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1, trimmed.endsWith("]") ? trimmed.length() - 1 : trimmed.length());
        }
        String[] parts = trimmed.split("[,;\\s]+");
        List<Integer> values = new ArrayList<>();
        for (String part : parts) {
            Integer rgb = parseOne(part);
            if (rgb != null) {
                values.add(rgb);
                if (values.size() >= MAX_NAME_LEN) {
                    break;
                }
            }
        }
        return toArray(values);
    }

    public static int[] parseFromMessage(JsonObject msgObj) {
        if (msgObj == null) {
            return new int[0];
        }
        if (msgObj.has("name_colors") && !msgObj.get("name_colors").isJsonNull()) {
            JsonElement el = msgObj.get("name_colors");
            if (el.isJsonArray()) {
                int[] fromArray = parseArray(el.getAsJsonArray());
                if (fromArray.length > 0) {
                    return fromArray;
                }
            } else {
                int[] fromString = parse(el.getAsString());
                if (fromString.length > 0) {
                    return fromString;
                }
            }
        }
        if (msgObj.has("name_color") && !msgObj.get("name_color").isJsonNull()) {
            return parse(msgObj.get("name_color").getAsString());
        }
        return new int[0];
    }

    public static int[] parseArray(JsonArray array) {
        if (array == null || array.isEmpty()) {
            return new int[0];
        }
        List<Integer> values = new ArrayList<>();
        for (JsonElement el : array) {
            if (el == null || el.isJsonNull()) {
                continue;
            }
            Integer rgb = parseOne(el.getAsString());
            if (rgb != null) {
                values.add(rgb);
                if (values.size() >= MAX_NAME_LEN) {
                    break;
                }
            }
        }
        return toArray(values);
    }

    public static JsonArray toJsonArray(int[] colors) {
        JsonArray array = new JsonArray();
        if (colors == null) {
            return array;
        }
        for (int color : colors) {
            array.add(toHex(color));
        }
        return array;
    }

    public static boolean equals(int[] a, int[] b) {
        return Arrays.equals(a == null ? new int[0] : a, b == null ? new int[0] : b);
    }

    public static Sample fromComponent(Component message) {
        if (message == null) {
            return null;
        }
        StringBuilder plain = new StringBuilder();
        List<Integer> rgb = new ArrayList<>();
        int[] last = {WHITE};
        message.visit((style, text) -> {
            if (text == null || text.isEmpty()) {
                return Optional.empty();
            }
            int color = last[0];
            if (style != null) {
                TextColor textColor = style.getColor();
                if (textColor != null) {
                    color = textColor.getValue() & WHITE;
                    last[0] = color;
                }
            }
            for (int i = 0; i < text.length(); i++) {
                plain.append(text.charAt(i));
                rgb.add(color);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return extract(plain.toString(), toArray(rgb));
    }

    /**
     * Nimmt den Spielernamen unmittelbar vor {@code >>} (optional Icon/Leerzeichen dazwischen)
     * und die Farbe jedes Buchstabens.
     */
    public static Sample extract(String plain, int[] perCharRgb) {
        if (plain == null || plain.contains("[CCLive]") || perCharRgb == null) {
            return null;
        }
        int arrow = plain.indexOf(">>");
        if (arrow <= 0) {
            return null;
        }
        int limit = Math.min(arrow, perCharRgb.length);
        int end = limit;
        while (end > 0 && Character.isWhitespace(plain.charAt(end - 1))) {
            end--;
        }
        if (end > 0 && net.felix.utilities.Other.PlayericonUtility.PlayerIconUtility.isChatIconMarker(plain.charAt(end - 1))) {
            end--;
            while (end > 0 && Character.isWhitespace(plain.charAt(end - 1))) {
                end--;
            }
        }
        int start = end;
        while (start > 0 && isNameChar(plain.charAt(start - 1))) {
            start--;
        }
        int length = end - start;
        if (length < 2 || length > MAX_NAME_LEN) {
            return null;
        }
        String name = plain.substring(start, end);
        int[] colors = new int[length];
        for (int i = 0; i < length; i++) {
            colors[i] = perCharRgb[start + i] & WHITE;
        }
        return new Sample(name, colors);
    }

    public static Component coloredName(String player, int[] colors, HoverEvent hover) {
        if (player == null || player.isEmpty()) {
            return Component.empty();
        }
        if (colors == null || colors.length == 0) {
            return letter(player, WHITE, hover);
        }
        if (colors.length == 1 || allSame(colors)) {
            return letter(player, colors[0], hover);
        }
        MutableComponent out = Component.empty();
        for (int i = 0; i < player.length(); i++) {
            out.append(letter(String.valueOf(player.charAt(i)), colors[Math.min(i, colors.length - 1)], hover));
        }
        return out;
    }

    private static Component letter(String text, int rgb, HoverEvent hover) {
        Style style = Style.EMPTY.withColor(rgb);
        if (hover != null) {
            style = style.withHoverEvent(hover);
        }
        return Component.literal(text).setStyle(style);
    }

    private static boolean allSame(int[] colors) {
        int first = colors[0];
        for (int i = 1; i < colors.length; i++) {
            if (colors[i] != first) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNameChar(char ch) {
        return (ch >= 'A' && ch <= 'Z')
            || (ch >= 'a' && ch <= 'z')
            || (ch >= '0' && ch <= '9')
            || ch == '_';
    }

    private static Integer parseOne(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1).trim();
        }
        if (s.startsWith("#")) {
            s = s.substring(1);
        } else if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        if (s.length() != 6) {
            return null;
        }
        try {
            int value = Integer.parseInt(s, 16);
            return value >= 0 && value <= WHITE ? value : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static int[] toArray(List<Integer> values) {
        int[] out = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i);
        }
        return out;
    }
}

package net.felix.utilities.Overall;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Liest CactusClicker-Font-Provider aus {@code minecraft:font/default.json}
 * (group + name) und mappt Glyphen auf dekodierte Zeichen.
 */
final class ZeichenFontLoader {

    static final Identifier MINECRAFT_DEFAULT_FONT = Identifier.fromNamespaceAndPath("minecraft", "font/default.json");

    /** Reihenfolge der Glyphen in font_bottom_line / font_first_line */
    static final String STANDARD_LINE_DECODE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ+.[]ÄÖÜß0123456789:-(),!";

    /** Reihenfolge der Bossbar-Ziffern (Kombo-Kiste) */
    static final String DIGIT_DECODE = "0123456789";

    record FontProviderKey(String group, String name) {}

    private ZeichenFontLoader() {}

    static final Identifier MOD_DEFAULT_FONT = Identifier.fromNamespaceAndPath("cclive-utilities", "font/default.json");

    /**
     * Sammelt Grafikglyphen aus Custom-Bitmap-/Space-Providern (mit group)
     * plus referenzierten Nicht-Vanilla-Fonts. Vanilla-ASCII/Unifont bleibt draußen.
     * @return empty = Datei fehlte oder JSON unlesbar
     */
    static Optional<Set<Integer>> collectCustomGraphicCodePoints(ResourceManager manager) {
        if (manager == null) {
            return Optional.empty();
        }
        try {
            Set<Integer> out = new HashSet<>();
            Set<String> visited = new HashSet<>();
            boolean minecraftFontRead = collectFontProviders(manager, MINECRAFT_DEFAULT_FONT, out, visited, true);
            collectFontProviders(manager, MOD_DEFAULT_FONT, out, visited, false);
            if (!minecraftFontRead) {
                return Optional.empty();
            }
            return Optional.of(out);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static boolean collectFontProviders(
            ResourceManager manager,
            Identifier resource,
            Set<Integer> out,
            Set<String> visited,
            boolean requireReadable) {
        String key = resource.toString();
        if (!visited.add(key)) {
            return true;
        }
        Optional<Resource> file = manager.getResource(resource);
        if (file.isEmpty()) {
            return !requireReadable;
        }
        try (var reader = new InputStreamReader(file.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("providers") || !root.get("providers").isJsonArray()) {
                return true;
            }
            for (JsonElement element : root.getAsJsonArray("providers")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject provider = element.getAsJsonObject();
                String type = provider.has("type") ? provider.get("type").getAsString() : "";
                boolean custom = provider.has("group");
                if ("bitmap".equals(type) && provider.has("chars") && (custom || isModFont(resource))) {
                    out.addAll(extractCodePoints(provider.getAsJsonArray("chars")));
                } else if ("space".equals(type) && provider.has("advances") && provider.get("advances").isJsonObject()) {
                    collectSpaceAdvances(provider.getAsJsonObject("advances"), out, custom);
                } else if ("reference".equals(type) && provider.has("id")) {
                    Identifier referenced = parseFontReference(provider.get("id").getAsString());
                    if (referenced != null && shouldFollowReference(referenced)) {
                        collectFontProviders(manager, referenced, out, visited, false);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return !requireReadable;
        }
    }

    private static boolean isModFont(Identifier resource) {
        return resource != null && "cclive-utilities".equals(resource.getNamespace());
    }

    private static boolean shouldFollowReference(Identifier fontId) {
        if (fontId == null) {
            return false;
        }
        if (!"minecraft".equals(fontId.getNamespace())) {
            return true;
        }
        String path = fontId.getPath();
        return path != null && !path.contains("include/");
    }

    private static Identifier parseFontReference(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Identifier id = Identifier.parse(raw.trim());
            String path = id.getPath();
            if (path.endsWith(".json")) {
                if (!path.startsWith("font/")) {
                    return Identifier.fromNamespaceAndPath(id.getNamespace(), "font/" + path);
                }
                return id;
            }
            if (path.startsWith("font/")) {
                return Identifier.fromNamespaceAndPath(id.getNamespace(), path + ".json");
            }
            return Identifier.fromNamespaceAndPath(id.getNamespace(), "font/" + path + ".json");
        } catch (Exception e) {
            return null;
        }
    }

    private static void collectSpaceAdvances(JsonObject advances, Set<Integer> out, boolean customProvider) {
        for (Map.Entry<String, JsonElement> entry : advances.entrySet()) {
            String glyph = entry.getKey();
            if (glyph == null || glyph.isEmpty()) {
                continue;
            }
            int index = 0;
            while (index < glyph.length()) {
                int codePoint = glyph.codePointAt(index);
                if (customProvider) {
                    if (codePoint != ' ') {
                        out.add(codePoint);
                    }
                } else if (isPrivateUse(codePoint)) {
                    out.add(codePoint);
                }
                index += Character.charCount(codePoint);
            }
        }
    }

    private static boolean isPrivateUse(int codePoint) {
        return (codePoint >= 0xE000 && codePoint <= 0xF8FF)
            || (codePoint >= 0xF0000 && codePoint <= 0xFFFFD)
            || (codePoint >= 0x100000 && codePoint <= 0x10FFFD);
    }

    static Map<FontProviderKey, List<Integer>> indexProviders(ResourceManager manager) {
        Map<FontProviderKey, List<Integer>> index = new HashMap<>();
        Optional<Resource> resource = manager.getResource(MINECRAFT_DEFAULT_FONT);
        if (resource.isEmpty()) {
            return index;
        }
        try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("providers")) {
                return index;
            }
            collectBitmapProviders(root.getAsJsonArray("providers"), index);
        } catch (Exception ignored) {
            return index;
        }
        return index;
    }

    private static void collectBitmapProviders(JsonArray providers, Map<FontProviderKey, List<Integer>> index) {
        for (JsonElement element : providers) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject provider = element.getAsJsonObject();
            String type = provider.has("type") ? provider.get("type").getAsString() : "";
            if ("bitmap".equals(type)) {
                if (provider.has("group") && provider.has("name") && provider.has("chars")) {
                    FontProviderKey key = new FontProviderKey(
                            provider.get("group").getAsString(),
                            provider.get("name").getAsString()
                    );
                    index.put(key, extractCodePoints(provider.getAsJsonArray("chars")));
                }
            } else if ("reference".equals(type) && provider.has("id")) {
                // Referenzierte Fonts werden separat nicht aufgelöst – Server-Pack enthält
                // CactusClicker-Provider typischerweise direkt in default.json.
            }
        }
    }

    static List<Integer> extractCodePoints(JsonArray charsArray) {
        List<Integer> codePoints = new ArrayList<>();
        for (JsonElement rowElement : charsArray) {
            String row = rowElement.getAsString();
            int index = 0;
            while (index < row.length()) {
                int codePoint = row.codePointAt(index);
                codePoints.add(codePoint);
                index += Character.charCount(codePoint);
            }
        }
        return codePoints;
    }

    static String codePointsToString(List<Integer> codePoints) {
        StringBuilder builder = new StringBuilder();
        for (int codePoint : codePoints) {
            builder.appendCodePoint(codePoint);
        }
        return builder.toString();
    }

    static Map<Character, String> buildGlyphToDecodedMap(List<Integer> codePoints, String decode) {
        Map<Character, String> mapping = new HashMap<>();
        int count = Math.min(codePoints.size(), decode.length());
        for (int i = 0; i < count; i++) {
            char glyph = (char) codePoints.get(i).intValue();
            mapping.put(glyph, String.valueOf(decode.charAt(i)));
        }
        return mapping;
    }

    static Map<Character, Integer> buildDigitMap(List<Integer> codePoints, String decode) {
        Map<Character, Integer> digits = new HashMap<>();
        int count = Math.min(codePoints.size(), decode.length());
        for (int i = 0; i < count; i++) {
            char decoded = decode.charAt(i);
            if (decoded >= '0' && decoded <= '9') {
                digits.put((char) codePoints.get(i).intValue(), decoded - '0');
            }
        }
        return digits;
    }

    static String buildDigitString(Map<Character, Integer> digitMap) {
        StringBuilder builder = new StringBuilder(10);
        for (int digit = 0; digit <= 9; digit++) {
            for (Map.Entry<Character, Integer> entry : digitMap.entrySet()) {
                if (entry.getValue() == digit) {
                    builder.append(entry.getKey());
                    break;
                }
            }
        }
        return builder.toString();
    }

    static Map<Integer, Integer> buildCodePointDigitMap(List<Integer> codePoints, String decode) {
        Map<Integer, Integer> digits = new HashMap<>();
        int count = Math.min(codePoints.size(), decode.length());
        for (int i = 0; i < count; i++) {
            char decoded = decode.charAt(i);
            if (decoded >= '0' && decoded <= '9') {
                digits.put(codePoints.get(i), decoded - '0');
            }
        }
        return digits;
    }

    static Optional<List<Integer>> find(Map<FontProviderKey, List<Integer>> index, String group, String name) {
        return Optional.ofNullable(index.get(new FontProviderKey(group, name)));
    }

    static String collectPixelSpacers(Map<FontProviderKey, List<Integer>> index) {
        List<Map.Entry<FontProviderKey, List<Integer>>> pixelProviders = index.entrySet().stream()
                .filter(e -> "pixel_split".equals(e.getKey().group()))
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .toList();
        StringBuilder builder = new StringBuilder();
        for (var entry : pixelProviders) {
            if (!entry.getValue().isEmpty()) {
                builder.appendCodePoint(entry.getValue().get(0));
            }
        }
        return builder.toString();
    }

    static String singleChar(Map<FontProviderKey, List<Integer>> index, String group, String name) {
        return find(index, group, name)
                .filter(cps -> !cps.isEmpty())
                .map(cps -> String.valueOf(Character.toChars(cps.get(0))))
                .orElse("");
    }
}

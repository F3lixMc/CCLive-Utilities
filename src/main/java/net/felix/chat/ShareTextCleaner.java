package net.felix.chat;

import net.felix.utilities.Overall.ZeichenUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Bereinigt Texturepack-/Zentrierungs-Glyphen für Share-Anzeige.
 * Erhält Farben und normale Wortabstände; strippt nur Layout-Glyphen.
 */
public final class ShareTextCleaner {
    private ShareTextCleaner() {
    }

    /** Plain-Text: Layout-Glyphen weg, Whitespace normalisieren (Lookup/Kit/Klartext). */
    public static String cleanDisplayName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String text = raw.replace('\n', ' ').replace('\r', ' ');
        text = stripLayoutGlyphs(text);
        text = text.replaceAll("§#[0-9a-fA-F]{6}", "");
        text = text.replaceAll("§[0-9a-fk-or]", "");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    /** True, wenn nach Layout-Bereinigung kein sichtbarer Name übrig bleibt. */
    public static boolean isLayoutOnlyName(String raw) {
        return cleanDisplayName(raw).isEmpty();
    }

    /**
     * Layout-Glyphen entfernen, normale Spaces und innere Abstände behalten.
     * Kein trim — für formatierte Share-Parts.
     */
    public static String stripLayoutGlyphsPreserveSpaces(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return stripLayoutGlyphs(text.replace('\n', ' ').replace('\r', ' '));
    }

    /**
     * Formatierten Text bereinigen: Spacer-Zeichen in Segmenten entfernen,
     * Farben/Styles behalten; nur führende Layout-Spaces am Zeilenanfang trimmen.
     */
    public static Component cleanFormatted(Component source) {
        if (source == null) {
            return Component.empty();
        }
        MutableComponent out = Component.empty();
        flattenClean(source, Style.EMPTY, out, new boolean[]{true});
        return out;
    }

    private static void flattenClean(Component component, Style parent, MutableComponent out, boolean[] leading) {
        Style style = component.getStyle();
        Style merged = style != null ? style.applyTo(parent) : parent;
        String own = extractOwnLiteral(component);
        if (own != null && !own.isEmpty()) {
            String cleaned = stripLayoutGlyphsPreserveSpaces(own);
            if (leading[0]) {
                cleaned = trimLeadingSpaces(cleaned);
            }
            // Space-only nach Leading-Trim behalten, wenn nicht mehr am Zeilenanfang
            if (!cleaned.isEmpty() || (!leading[0] && own.chars().anyMatch(Character::isWhitespace))) {
                if (cleaned.isEmpty()) {
                    cleaned = " ";
                }
                leading[0] = false;
                out.append(Component.literal(cleaned).setStyle(styleFromMerged(merged)));
            }
        }
        for (Component sibling : component.getSiblings()) {
            flattenClean(sibling, merged, out, leading);
        }
    }

    private static Style styleFromMerged(Style merged) {
        Style partStyle = Style.EMPTY;
        TextColor color = merged.getColor();
        if (color != null) {
            partStyle = partStyle.withColor(color);
        }
        if (merged.isBold()) {
            partStyle = partStyle.withBold(true);
        }
        if (merged.isItalic()) {
            partStyle = partStyle.withItalic(true);
        }
        if (merged.isUnderlined()) {
            partStyle = partStyle.withUnderlined(true);
        }
        if (merged.isStrikethrough()) {
            partStyle = partStyle.withStrikethrough(true);
        }
        return partStyle;
    }

    static String stripLayoutGlyphs(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String spacer = ZeichenUtility.getPixelSpacer();
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (spacer != null && !spacer.isEmpty() && spacer.indexOf(c) >= 0) {
                continue;
            }
            if ((c >= '\u3400' && c <= '\u4DBF') || (c >= '\uE000' && c <= '\uF8FF')) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String trimLeadingSpaces(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return s.substring(i);
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
                    if (!"text".equals(name) && !"string".equals(name) && !"contents".equals(name)
                            && !"getString".equals(name) && !"literal".equals(name)) {
                        continue;
                    }
                    try {
                        Object value = method.invoke(contents);
                        if (value instanceof String str) {
                            return str;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}

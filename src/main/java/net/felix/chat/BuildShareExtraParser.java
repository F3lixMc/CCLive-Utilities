package net.felix.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.felix.utilities.ItemViewer.ItemData;
import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.felix.utilities.Overall.InformationenUtility;
import net.felix.utilities.Overall.InformationenUtility.AspectInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Machtkristalle und Aspekte aus erfassten Ausrüstungs-Tooltips.
 */
public final class BuildShareExtraParser {

    public static final String EQUIPPED_CRYSTALS_HEADER = "Ausgerüstete Machtkristalle:";
    private static final int MAX_CRYSTALS = 3;

    private BuildShareExtraParser() {
    }

    public static List<String> parsePowerCrystalNames(List<ChatTooltipLine> tooltip) {
        List<String> names = new ArrayList<>();
        if (tooltip == null || tooltip.isEmpty()) {
            return names;
        }
        boolean inSection = false;
        for (ChatTooltipLine line : tooltip) {
            String plain = stripCodes(line != null ? line.plainText() : null);
            if (!inSection) {
                if (isEquippedCrystalsHeader(plain)) {
                    inSection = true;
                }
                continue;
            }
            if (plain.isEmpty()) {
                continue;
            }
            if (isSectionEnd(plain)) {
                break;
            }
            String candidate = stripListItem(plain);
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            ItemData db = ItemViewerUtility.findItemByName(candidate);
            if (!isPowerCrystal(db) || db.name == null || db.name.isEmpty()) {
                continue;
            }
            if (!names.contains(db.name)) {
                names.add(db.name);
            }
            if (names.size() >= MAX_CRYSTALS) {
                break;
            }
        }
        return names;
    }

    public static List<String> parseAspectNames(Iterable<ItemData> items) {
        List<String> names = new ArrayList<>();
        if (items == null) {
            return names;
        }
        for (ItemData item : items) {
            if (item == null || item.chatTooltip == null) {
                continue;
            }
            for (ChatTooltipLine line : item.chatTooltip) {
                if (line == null) {
                    continue;
                }
                String plain = line.plainText();
                if (plain == null || !plain.contains("⭐")) {
                    continue;
                }
                String tooltipName = InformationenUtility.extractBracketAspectNameFromStarLine(plain);
                if (tooltipName == null || tooltipName.isEmpty()) {
                    continue;
                }
                AspectInfo info = InformationenUtility.getAspectInfoByTooltipDisplayName(tooltipName);
                if (info == null || info.aspectName == null || info.aspectName.isEmpty()
                        || "-".equals(info.aspectName)) {
                    continue;
                }
                if (!names.contains(info.aspectName)) {
                    names.add(info.aspectName);
                }
            }
        }
        return names;
    }

    public static void stripUnconfirmedCrystalLines(ItemData overview, List<String> confirmedNames) {
        if (overview == null || overview.chatTooltip == null || overview.chatTooltip.isEmpty()) {
            return;
        }
        Set<String> confirmed = new HashSet<>();
        if (confirmedNames != null) {
            for (String name : confirmedNames) {
                String key = normalizeCrystalName(name);
                if (!key.isEmpty()) {
                    confirmed.add(key);
                }
            }
        }
        List<ChatTooltipLine> out = new ArrayList<>();
        boolean inSection = false;
        for (ChatTooltipLine line : overview.chatTooltip) {
            String plain = stripCodes(line != null ? line.plainText() : null);
            if (!inSection) {
                out.add(line);
                if (isEquippedCrystalsHeader(plain)) {
                    inSection = true;
                }
                continue;
            }
            if (isSectionEnd(plain)) {
                inSection = false;
                out.add(line);
                continue;
            }
            if (plain.isEmpty()) {
                out.add(line);
                continue;
            }
            if (isConfirmedCrystalLine(plain, confirmed)) {
                out.add(line);
            }
        }
        overview.chatTooltip = out;
    }

    private static boolean isConfirmedCrystalLine(String plain, Set<String> confirmed) {
        if (confirmed.isEmpty()) {
            return false;
        }
        String candidate = stripListItem(plain);
        if (candidate == null || candidate.isEmpty()) {
            return false;
        }
        if (confirmed.contains(normalizeCrystalName(candidate))) {
            return true;
        }
        ItemData db = ItemViewerUtility.findItemByName(candidate);
        return db != null && db.name != null && confirmed.contains(normalizeCrystalName(db.name));
    }

    private static String normalizeCrystalName(String name) {
        return stripCodes(name).toLowerCase(Locale.ROOT);
    }

    public static boolean isPowerCrystal(ItemData item) {
        if (item == null) {
            return false;
        }
        if ("power_crystals".equalsIgnoreCase(item.category)) {
            return true;
        }
        return item.info != null && Boolean.TRUE.equals(item.info.power_crystal);
    }

    public static List<String> copyNullable(List<String> source) {
        return source == null ? null : new ArrayList<>(source);
    }

    public static void addJsonStringArray(JsonObject obj, String key, List<String> values) {
        if (obj == null || key == null || values == null) {
            return;
        }
        JsonArray arr = new JsonArray();
        for (String value : values) {
            if (value != null) {
                arr.add(value);
            }
        }
        obj.add(key, arr);
    }

    public static List<String> readJsonStringArray(JsonObject obj, String key) {
        if (obj == null || key == null || !obj.has(key) || !obj.get(key).isJsonArray()) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (JsonElement el : obj.getAsJsonArray(key)) {
            if (el != null && el.isJsonPrimitive()) {
                String value = el.getAsString();
                if (value != null && !value.isEmpty()) {
                    out.add(value);
                }
            }
        }
        return out;
    }

    private static boolean isEquippedCrystalsHeader(String plain) {
        if (plain == null || plain.isEmpty()) {
            return false;
        }
        return plain.equals(EQUIPPED_CRYSTALS_HEADER)
            || plain.startsWith("Ausgerüstete Machtkristalle");
    }

    private static boolean isSectionEnd(String plain) {
        if (plain.startsWith("Klicken") || plain.startsWith("Shift")
                || plain.startsWith("Linksklick") || plain.startsWith("Rechtsklick")) {
            return true;
        }
        return plain.endsWith(":") && !isEquippedCrystalsHeader(plain) && !plain.startsWith("•");
    }

    private static String stripListItem(String plain) {
        String s = plain.trim();
        s = s.replaceFirst("^[•·\\-*]\\s*", "").trim();
        if (s.startsWith("[") && s.endsWith("]") && s.length() > 2) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private static String stripCodes(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("§[0-9a-fk-or]", "").trim();
    }
}

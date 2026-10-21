package net.felix.chat;

import net.felix.utilities.ItemViewer.ItemData;
import net.felix.utilities.ItemViewer.ItemViewerGrid;
import net.felix.utilities.ItemViewer.ItemViewerUtility;
import net.felix.utilities.Town.CustomKit;
import net.felix.utilities.Town.CustomKitManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Speichert einen Build-Share-Snapshot als eigenes CustomKit
 * (items.json-Basisnamen, Karten separat, Angelkomponenten aus Tooltip).
 */
public final class ShareKitImportHelper {

    private static final Pattern LEADING_UPGRADE = Pattern.compile("^\\[\\+\\d+\\]\\s*");
    private static final int MAX_KIT_ITEMS = 28;
    private static final String[] EQUIPMENT_KEYS = {"armor", "weapons", "tools", "modules"};
    private static final String[] FISHING_COMPONENT_TYPES = {
        "Schnur", "Rolle", "Köder", "Rute", "Haken", "Pose", "Gewicht", "Spinner"
    };
    private static final String FISHING_ROD_ID = "minecraft:fishing_rod";

    private ShareKitImportHelper() {
    }

    /**
     * Basisname für items.json: Layout, führendes [+x], Schmiedegrad am Ende,
     * optional abschließendes [Modul] für Modul-Abgleich.
     */
    public static String extractEquipmentBaseName(String rawName, boolean stripModuleSuffix) {
        if (rawName == null || rawName.isEmpty()) {
            return "";
        }
        String name = ShareTextCleaner.cleanDisplayName(rawName);
        if (name.isEmpty()) {
            return "";
        }
        Matcher upgrade = LEADING_UPGRADE.matcher(name);
        if (upgrade.find()) {
            name = name.substring(upgrade.end()).trim();
        }
        if (name.endsWith("]")) {
            int start = name.lastIndexOf('[');
            if (start >= 0) {
                String inner = name.substring(start + 1, name.length() - 1).trim();
                if (isKnownForgingInner(inner)) {
                    name = name.substring(0, start).trim();
                } else if (stripModuleSuffix && "modul".equalsIgnoreCase(inner)) {
                    name = name.substring(0, start).trim();
                }
            }
        }
        return name;
    }

    public static String extractEquipmentBaseName(String rawName) {
        return extractEquipmentBaseName(rawName, false);
    }

    private static boolean isKnownForgingInner(String inner) {
        if (inner == null || inner.isEmpty()) {
            return false;
        }
        String lower = inner.toLowerCase(Locale.ROOT)
            .replace('ä', 'a')
            .replace('ö', 'o')
            .replace('ü', 'u');
        return switch (lower) {
            case "sternengeschmiedet", "sterngeschmiedet",
                 "blitzgeschmiedet",
                 "damonengeschmiedet", "daemonengeschmiedet",
                 "drachengeschmiedet",
                 "titangeschmiedet",
                 "lavageschmiedet",
                 "frostgeschmiedet" -> true;
            default -> ItemViewerGrid.extractForgingConditionFromName("[" + inner + "]") != null;
        };
    }

    public static ItemData findExactItemsJsonEntry(String rawShareName, boolean stripModuleSuffix) {
        String base = extractEquipmentBaseName(rawShareName, stripModuleSuffix);
        if (base.isEmpty()) {
            return null;
        }
        return ItemViewerUtility.findItemByName(base);
    }

    public static ItemData findExactItemsJsonEntry(String rawShareName) {
        return findExactItemsJsonEntry(rawShareName, false);
    }

    /** Basisname einer Angelkomponenten-Zeile für exakten items.json-Abgleich. */
    public static String extractFishingComponentBaseName(String rawLine) {
        if (rawLine == null || rawLine.isEmpty()) {
            return "";
        }
        String name = ShareTextCleaner.cleanDisplayName(rawLine);
        name = name.replaceFirst("^[•·▪▸►\\-–—\\s]+", "").trim();
        if (name.endsWith("]")) {
            int start = name.lastIndexOf('[');
            if (start >= 0) {
                String inner = name.substring(start + 1, name.length() - 1).trim();
                if (matchFishingComponentType(inner) != null) {
                    name = name.substring(0, start).trim();
                }
            }
        }
        return name;
    }

    public static String matchFishingComponentType(String inner) {
        if (inner == null || inner.isEmpty()) {
            return null;
        }
        for (String type : FISHING_COMPONENT_TYPES) {
            if (type.equalsIgnoreCase(inner.trim())) {
                return type;
            }
        }
        return null;
    }

    private static boolean isFishingRod(ItemData item) {
        return item != null && item.id != null && FISHING_ROD_ID.equalsIgnoreCase(item.id.trim());
    }

    /**
     * Parst Komponentenzeilen aus Share-Tooltip (nach „Komponenten“, vor „Statistik“).
     */
    public static List<ParsedFishingComponent> parseFishingComponentsFromTooltip(List<ChatTooltipLine> tooltip) {
        List<ParsedFishingComponent> out = new ArrayList<>();
        if (tooltip == null || tooltip.isEmpty()) {
            return out;
        }
        int start = -1;
        int end = tooltip.size();
        for (int i = 0; i < tooltip.size(); i++) {
            ChatTooltipLine line = tooltip.get(i);
            if (line == null) {
                continue;
            }
            String plain = normalizeHeader(line.plainText());
            if (start < 0 && "komponenten".equals(plain)) {
                start = i + 1;
                continue;
            }
            if (start >= 0 && "statistik".equals(plain)) {
                end = i;
                break;
            }
        }
        if (start < 0) {
            return out;
        }
        for (int i = start; i < end && out.size() < FISHING_COMPONENT_TYPES.length; i++) {
            ChatTooltipLine line = tooltip.get(i);
            if (line == null) {
                continue;
            }
            String raw = ShareTextCleaner.cleanDisplayName(line.plainText());
            if (raw.isEmpty()) {
                continue;
            }
            String cleaned = raw.replaceFirst("^[•·▪▸►\\-–—\\s]+", "").trim();
            if (cleaned.isEmpty() || !cleaned.endsWith("]")) {
                continue;
            }
            int bracket = cleaned.lastIndexOf('[');
            if (bracket < 0) {
                continue;
            }
            String typeInner = cleaned.substring(bracket + 1, cleaned.length() - 1).trim();
            String type = matchFishingComponentType(typeInner);
            if (type == null) {
                continue;
            }
            String baseName = cleaned.substring(0, bracket).trim();
            if (baseName.isEmpty()) {
                continue;
            }
            out.add(new ParsedFishingComponent(type, baseName, cleaned));
        }
        return out;
    }

    private static String normalizeHeader(String text) {
        if (text == null) {
            return "";
        }
        return ShareTextCleaner.cleanDisplayName(text)
            .replaceAll("\\s+", " ")
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    public static SaveResult saveSnapshotAsCustomKit(BuildShareManager.BuildSnapshot snapshot) {
        if (snapshot == null) {
            return SaveResult.failed("Kein Build zum Speichern.");
        }
        String kitName = snapshot.kitName;
        if (kitName == null || kitName.isEmpty()) {
            return SaveResult.failed("Build ohne Namen — Speichern abgebrochen.");
        }

        CustomKit kit = new CustomKit();
        kit.name = kitName;

        List<String> unresolved = new ArrayList<>();
        Set<String> seenItems = new LinkedHashSet<>();
        int filledEquipment = 0;
        int overflow = 0;

        if (snapshot.equipment != null) {
            for (String key : EQUIPMENT_KEYS) {
                BuildShareManager.CategoryBucket bucket = snapshot.equipment.get(key);
                if (bucket == null || bucket.items == null) {
                    continue;
                }
                boolean modules = "modules".equals(key);
                for (ItemData shareItem : bucket.items) {
                    if (shareItem == null) {
                        continue;
                    }
                    if (isFishingRod(shareItem)) {
                        importFishingRodComponents(shareItem, kit, unresolved);
                        continue;
                    }
                    filledEquipment++;
                    String raw = shareItem.name;
                    if (raw == null || raw.isEmpty()) {
                        unresolved.add("(ohne Namen)");
                        continue;
                    }
                    ItemData db = findExactItemsJsonEntry(raw, modules);
                    if (db == null || db.name == null || db.name.isEmpty()) {
                        String shown = extractEquipmentBaseName(raw, modules);
                        if (shown.isEmpty()) {
                            shown = ShareTextCleaner.cleanDisplayName(raw);
                        }
                        unresolved.add(shown.isEmpty() ? raw : shown);
                        continue;
                    }
                    if (!seenItems.add(db.name)) {
                        continue;
                    }
                    if (kit.itemNames.size() >= MAX_KIT_ITEMS) {
                        overflow++;
                        continue;
                    }
                    kit.itemNames.add(db.name);
                    if (kit.itemCategories == null) {
                        kit.itemCategories = new java.util.HashMap<>();
                    }
                    kit.itemCategories.put(db.name, key);
                    if (kit.iconItemName == null) {
                        kit.iconItemName = db.name;
                        if (db.id != null && !db.id.isEmpty()) {
                            kit.iconItemId = db.id;
                        }
                    }
                }
            }
        }

        if (overflow > 0) {
            unresolved.add("(" + overflow + " weitere Items über Kit-Limit " + MAX_KIT_ITEMS + ")");
        }

        if (snapshot.cards != null && snapshot.cards.items != null) {
            for (ItemData card : snapshot.cards.items) {
                if (card == null) {
                    continue;
                }
                String cardLabel = cardDisplayName(card);
                if (!cardLabel.isEmpty() && !kit.cardNames.contains(cardLabel)) {
                    kit.cardNames.add(cardLabel);
                }
            }
        }

        boolean persisted = CustomKitManager.addKit(kit);
        if (!persisted) {
            return SaveResult.failed("Kit konnte nicht gespeichert werden (Schreibfehler).");
        }

        int resolved = kit.itemNames.size()
            + (kit.fishingComponents != null ? kit.fishingComponents.size() : 0);
        if (!unresolved.isEmpty()) {
            return SaveResult.partial(kit.name, unresolved, resolved, filledEquipment);
        }
        if (filledEquipment == 0
                && kit.cardNames.isEmpty()
                && (kit.fishingComponents == null || kit.fishingComponents.isEmpty())) {
            return SaveResult.partial(kit.name, List.of("(keine Ausrüstung/Karten/Angelkomponenten im Share)"), 0, 0);
        }
        return SaveResult.success(kit.name);
    }

    private static void importFishingRodComponents(
            ItemData rod, CustomKit kit, List<String> unresolved) {
        List<ParsedFishingComponent> parsed = parseFishingComponentsFromTooltip(rod.chatTooltip);
        if (parsed.isEmpty()) {
            return;
        }
        Set<String> seenTypes = new LinkedHashSet<>();
        for (ParsedFishingComponent pc : parsed) {
            if (pc == null || pc.type == null || !seenTypes.add(pc.type)) {
                continue;
            }
            ItemData db = ItemViewerUtility.findItemByName(pc.baseName);
            String storeName;
            if (db != null && db.name != null && !db.name.isEmpty()) {
                storeName = db.name;
            } else {
                storeName = pc.baseName;
                unresolved.add(pc.baseName + " [" + pc.type + "]");
            }
            kit.fishingComponents.add(new CustomKit.FishingComponent(pc.type, storeName));
        }
    }

    private static String cardDisplayName(ItemData card) {
        String raw = null;
        if (card.name != null && !card.name.isEmpty()) {
            raw = ShareTextCleaner.cleanDisplayName(card.name);
        }
        if ((raw == null || raw.isEmpty()) && card.chatTooltip != null) {
            for (ChatTooltipLine line : card.chatTooltip) {
                if (line == null) {
                    continue;
                }
                String plain = ShareTextCleaner.cleanDisplayName(line.plainText());
                if (!plain.isEmpty()) {
                    raw = plain;
                    break;
                }
            }
        }
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        // „Lama [Karte]“ → „Lama“ für CardsStatues-/Level-Lookup
        if (raw.endsWith("]")) {
            int start = raw.lastIndexOf('[');
            if (start >= 0) {
                String inner = raw.substring(start + 1, raw.length() - 1).trim();
                if ("karte".equalsIgnoreCase(inner)) {
                    raw = raw.substring(0, start).trim();
                }
            }
        }
        return raw;
    }

    public static void sendFeedback(SaveResult result) {
        if (result == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client != null ? client.player : null;
        if (player == null) {
            return;
        }
        if (result.ok && !result.partial) {
            player.sendSystemMessage(
                Component.literal("§aEigenes Kit gespeichert: §f" + result.kitName));
            return;
        }
        if (result.ok && result.partial) {
            player.sendSystemMessage(
                Component.literal("§eKit teilweise gespeichert: §f" + result.kitName
                    + " §7(" + result.resolvedCount + " Einträge)"));
            if (result.unresolvedNames != null) {
                for (String name : result.unresolvedNames) {
                    player.sendSystemMessage(
                        Component.literal("§cNicht gefunden: §f" + name));
                }
            }
            return;
        }
        player.sendSystemMessage(
            Component.literal("§c" + (result.errorMessage != null ? result.errorMessage : "Speichern fehlgeschlagen.")));
    }

    public static final class ParsedFishingComponent {
        public final String type;
        public final String baseName;
        public final String displayWithType;

        public ParsedFishingComponent(String type, String baseName, String displayWithType) {
            this.type = type;
            this.baseName = baseName;
            this.displayWithType = displayWithType;
        }
    }

    public static final class SaveResult {
        public final boolean ok;
        public final boolean partial;
        public final String kitName;
        public final String errorMessage;
        public final List<String> unresolvedNames;
        public final int resolvedCount;
        public final int filledCount;

        private SaveResult(
                boolean ok,
                boolean partial,
                String kitName,
                String errorMessage,
                List<String> unresolvedNames,
                int resolvedCount,
                int filledCount) {
            this.ok = ok;
            this.partial = partial;
            this.kitName = kitName;
            this.errorMessage = errorMessage;
            this.unresolvedNames = unresolvedNames;
            this.resolvedCount = resolvedCount;
            this.filledCount = filledCount;
        }

        static SaveResult success(String kitName) {
            return new SaveResult(true, false, kitName, null, null, 0, 0);
        }

        static SaveResult partial(String kitName, List<String> unresolved, int resolved, int filled) {
            return new SaveResult(true, true, kitName, null, unresolved, resolved, filled);
        }

        static SaveResult failed(String message) {
            return new SaveResult(false, false, null, message, null, 0, 0);
        }
    }
}

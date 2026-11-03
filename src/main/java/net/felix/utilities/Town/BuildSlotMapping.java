package net.felix.utilities.Town;

import net.felix.utilities.ItemViewer.ItemData;
import net.felix.utilities.ItemViewer.ItemInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Zentrale Slot-Zuordnung für Build-Share, Kit-Editor und Vorschau.
 * Container-Slots sind nullbasiert 0–53.
 */
public final class BuildSlotMapping {

    public enum SlotState {
        UNKNOWN,
        EMPTY,
        LOCKED,
        FILLED
    }

    public enum PieceType {
        AXE(1, "Axt", false),
        HELM(2, "Helm", false),
        NECKLACE(6, "Halskette", false),
        MODULE(new int[]{7, 16, 25, 34, 43, 52}, "Modul", true),
        HOE(10, "Hacke", false),
        CHEST(11, "Brust", false),
        POWER_CRYSTAL_OVERVIEW(13, "Machtkristalle", false),
        SHOULDER(15, "Schulter", false),
        PICKAXE(19, "Spitzhacke", false),
        RING(new int[]{20, 24}, "Ring", true),
        FISHING_ROD(28, "Angelrute", false),
        GLOVES(29, "Handschuhe", false),
        BELT(33, "Gürtel", false),
        BOOTS(38, "Schuhe", false),
        LEGS(42, "Hose", false),
        WEAPON(new int[]{48, 50}, "Waffe", true);

        private final int[] slots;
        private final String label;
        private final boolean multiSlot;

        PieceType(int slot, String label, boolean multiSlot) {
            this(new int[]{slot}, label, multiSlot);
        }

        PieceType(int[] slots, String label, boolean multiSlot) {
            this.slots = slots;
            this.label = label;
            this.multiSlot = multiSlot;
        }

        public int[] getSlots() {
            return slots.clone();
        }

        public String getLabel() {
            return label;
        }

        public boolean isMultiSlot() {
            return multiSlot;
        }

        public String slotLabel(int slot) {
            if (!multiSlot) {
                return label;
            }
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] == slot) {
                    if (this == MODULE) {
                        return "Modulslot " + (i + 1);
                    }
                    return label + " " + (i + 1);
                }
            }
            return label;
        }
    }

    public enum EquipmentCategory {
        ARMOR("Rüstung", new int[]{2, 11, 20, 29, 38, 6, 15, 24, 33, 42}, false),
        WEAPONS("Waffen", new int[]{48, 50}, false),
        /** Scan inkl. Angelrute (28). Automatische Werkzeugvergabe nutzt {@link #TOOL_ASSIGNMENT_SLOTS}. */
        TOOLS("Werkzeuge", new int[]{1, 10, 19, 28}, false),
        MODULES("Module", new int[]{7, 16, 25, 34, 43, 52}, true);

        private final String displayName;
        private final int[] slots;
        private final boolean usesPlaceholders;

        EquipmentCategory(String displayName, int[] slots, boolean usesPlaceholders) {
            this.displayName = displayName;
            this.slots = slots;
            this.usesPlaceholders = usesPlaceholders;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int[] getSlots() {
            return slots.clone();
        }

        public int getCapacity() {
            return slots.length;
        }

        public boolean usesPlaceholders() {
            return usesPlaceholders;
        }

        public String slotLabel(int slotIndexInCategory) {
            if (slotIndexInCategory < 0 || slotIndexInCategory >= slots.length) {
                return displayName;
            }
            PieceType type = pieceTypeForSlot(slots[slotIndexInCategory]);
            return type != null ? type.slotLabel(slots[slotIndexInCategory]) : displayName;
        }

        public String categoryKey() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final int[] TOOL_ASSIGNMENT_SLOTS = {1, 10, 19};
    public static final int POWER_CRYSTAL_OVERVIEW_SLOT = 13;
    public static final int FISHING_ROD_SLOT = 28;

    public static final String[] FISHING_COMPONENT_TYPES = {
        "Schnur", "Rolle", "Köder", "Rute", "Haken", "Pose", "Gewicht", "Spinner"
    };

    public static final int[] CARD_SLOTS = {
        4, 5, 6, 7, 8,
        13, 14, 15, 16, 17,
        22, 23, 24, 25, 26,
        31, 32, 33, 34, 35
    };

    public static final String PLACEHOLDER_ITEM_NAME = "Slot gesperrt";
    public static final String LOCKED_SLOT_LORE = "Nicht freigeschalten!";
    public static final String LOCKED_SLOT_LORE_ALT = "Nicht freigeschaltet!";

    private static final Map<Integer, PieceType> SLOT_TO_PIECE;

    static {
        Map<Integer, PieceType> map = new LinkedHashMap<>();
        for (PieceType type : PieceType.values()) {
            for (int slot : type.slots) {
                map.put(slot, type);
            }
        }
        SLOT_TO_PIECE = Collections.unmodifiableMap(map);
    }

    private BuildSlotMapping() {
    }

    public static PieceType pieceTypeForSlot(int slot) {
        return SLOT_TO_PIECE.get(slot);
    }

    public static String labelForSlot(int slot) {
        PieceType type = pieceTypeForSlot(slot);
        return type != null ? type.slotLabel(slot) : ("Slot " + slot);
    }

    /**
     * Itemtyp aus JEI-/items.json-Daten. Unbekannt → {@code null} (nicht raten).
     */
    public static PieceType resolvePieceType(ItemData data) {
        if (data == null) {
            return null;
        }
        if (isModuleItem(data)) {
            return PieceType.MODULE;
        }
        if (isFishingRod(data)) {
            return PieceType.FISHING_ROD;
        }
        if (isPowerCrystal(data)) {
            return null;
        }
        PieceType fromMeta = matchToolPieceType(data);
        if (fromMeta != null) {
            return fromMeta;
        }
        String blob = joinTypeBlob(data);
        if (blob.isEmpty()) {
            return null;
        }
        PieceType fromBlob = matchToolTokens(blob);
        if (fromBlob != null) {
            return fromBlob;
        }
        if (containsAny(blob, "ring")) {
            return PieceType.RING;
        }
        if (containsAny(blob, "helm", "helmkappe", "helmhaube", "helmpanzer", "helmvisier")) {
            return PieceType.HELM;
        }
        if (containsAny(blob, "brust", "brustplatte", "chest")) {
            return PieceType.CHEST;
        }
        if (containsAny(blob, "schulter")) {
            return PieceType.SHOULDER;
        }
        if (containsAny(blob, "handschuh")) {
            return PieceType.GLOVES;
        }
        if (containsAny(blob, "schuh", "stiefel", "boots")) {
            return PieceType.BOOTS;
        }
        if (containsAny(blob, "hose", "bein", "leggings")) {
            return PieceType.LEGS;
        }
        if (containsAny(blob, "halskette", "kette", "amulett", "necklace")) {
            return PieceType.NECKLACE;
        }
        if (containsAny(blob, "gürtel", "gurtel", "belt")) {
            return PieceType.BELT;
        }
        if (containsAny(blob, "waffe", "weapon", "schwert", "bogen", "armbrust", "dolch", "stab")) {
            return PieceType.WEAPON;
        }
        return null;
    }

    public static EquipmentCategory categoryForPiece(PieceType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case WEAPON -> EquipmentCategory.WEAPONS;
            case AXE, HOE, PICKAXE, FISHING_ROD -> EquipmentCategory.TOOLS;
            case MODULE -> EquipmentCategory.MODULES;
            case POWER_CRYSTAL_OVERVIEW -> null;
            default -> EquipmentCategory.ARMOR;
        };
    }

    public static int[] assignmentSlots(PieceType type) {
        if (type == null || type == PieceType.FISHING_ROD || type == PieceType.POWER_CRYSTAL_OVERVIEW) {
            return new int[0];
        }
        if (type == PieceType.AXE || type == PieceType.HOE || type == PieceType.PICKAXE) {
            return type.getSlots();
        }
        return type.getSlots();
    }

    public static boolean isAssignmentToolSlot(int slot) {
        for (int s : TOOL_ASSIGNMENT_SLOTS) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFishingRod(ItemData data) {
        if (data == null) {
            return false;
        }
        if (data.id != null && "minecraft:fishing_rod".equalsIgnoreCase(data.id.trim())) {
            return true;
        }
        String blob = joinTypeBlob(data);
        return containsAny(blob, "angelrute", "fishing_rod", "fishing rod");
    }

    public static boolean isModuleItem(ItemData data) {
        if (data == null) {
            return false;
        }
        if (data.info != null && Boolean.TRUE.equals(data.info.module)) {
            return true;
        }
        return data.category != null && data.category.toLowerCase(Locale.ROOT).contains("module");
    }

    public static boolean isPowerCrystal(ItemData data) {
        if (data == null) {
            return false;
        }
        if (data.info != null && Boolean.TRUE.equals(data.info.power_crystal)) {
            return true;
        }
        return "power_crystals".equalsIgnoreCase(data.category);
    }

    public static boolean isFishingComponent(ItemData data) {
        return data != null && "fishing_components".equalsIgnoreCase(data.category);
    }

    public static String matchFishingComponentType(ItemData data) {
        if (data == null) {
            return null;
        }
        String fromPiece = matchFishingComponentType(data.info != null ? data.info.piece : null);
        if (fromPiece != null) {
            return fromPiece;
        }
        fromPiece = matchFishingComponentType(data.info != null ? data.info.type : null);
        if (fromPiece != null) {
            return fromPiece;
        }
        if (data.tags != null) {
            for (String tag : data.tags) {
                String matched = matchFishingComponentType(tag);
                if (matched != null) {
                    return matched;
                }
            }
        }
        if (data.name != null && data.name.endsWith("]")) {
            int start = data.name.lastIndexOf('[');
            if (start >= 0) {
                return matchFishingComponentType(data.name.substring(start + 1, data.name.length() - 1));
            }
        }
        return null;
    }

    public static String matchFishingComponentType(String inner) {
        if (inner == null || inner.isEmpty()) {
            return null;
        }
        String needle = normalize(inner);
        for (String type : FISHING_COMPONENT_TYPES) {
            if (normalize(type).equals(needle)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isPlaceholderName(String plainName) {
        if (plainName == null) {
            return false;
        }
        String n = plainName.replaceAll("§.", "").trim();
        return PLACEHOLDER_ITEM_NAME.equals(n);
    }

    public static boolean isLockedLoreLine(String line) {
        if (line == null) {
            return false;
        }
        String plain = line.replaceAll("§.", "").trim();
        return plain.contains(LOCKED_SLOT_LORE) || plain.contains(LOCKED_SLOT_LORE_ALT);
    }

    public static int[] getEquipmentStatSlots() {
        return new int[]{1, 2, 6, 10, 11, 15, 19, 20, 24, 28, 29, 33, 38, 42, 48, 50};
    }

    public static Map<EquipmentCategory, int[]> getEquipmentCategorySlots() {
        Map<EquipmentCategory, int[]> map = new LinkedHashMap<>();
        for (EquipmentCategory category : EquipmentCategory.values()) {
            map.put(category, category.getSlots());
        }
        return Collections.unmodifiableMap(map);
    }

    public static int getCardCapacity() {
        return CARD_SLOTS.length;
    }

    public static int[] getCardSlots() {
        return Arrays.copyOf(CARD_SLOTS, CARD_SLOTS.length);
    }

    private static String joinTypeBlob(ItemData data) {
        StringBuilder out = new StringBuilder();
        ItemInfo info = data.info;
        if (info != null) {
            append(out, info.piece);
            append(out, info.type);
        }
        if (data.tags != null) {
            for (String tag : data.tags) {
                append(out, tag);
            }
        }
        append(out, data.category);
        return out.toString();
    }

    private static PieceType matchToolPieceType(ItemData data) {
        if (data.info != null) {
            PieceType fromPiece = matchToolTokens(normalize(data.info.piece));
            if (fromPiece != null) {
                return fromPiece;
            }
        }
        if (data.tags != null) {
            for (String tag : data.tags) {
                PieceType fromTag = matchToolTokens(normalize(tag));
                if (fromTag != null) {
                    return fromTag;
                }
            }
        }
        return null;
    }

    private static PieceType matchToolTokens(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        if (hasWord(text, "spitzhacke", "pickaxe")) {
            return PieceType.PICKAXE;
        }
        if (hasWord(text, "axt", "axe") && !hasWord(text, "pickaxe")) {
            return PieceType.AXE;
        }
        if (hasWord(text, "hacke", "hoe") && !hasWord(text, "spitzhacke")) {
            return PieceType.HOE;
        }
        return null;
    }

    private static boolean hasWord(String blob, String... needles) {
        if (blob == null || blob.isEmpty()) {
            return false;
        }
        String[] parts = blob.split("[^a-z0-9]+");
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            for (String needle : needles) {
                if (part.equals(normalize(needle))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void append(StringBuilder out, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (!out.isEmpty()) {
            out.append(' ');
        }
        out.append(normalize(value));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
            .replace('ä', 'a')
            .replace('ö', 'o')
            .replace('ü', 'u')
            .replace("ß", "ss")
            .trim();
    }

    private static boolean containsAny(String blob, String... needles) {
        for (String needle : needles) {
            if (blob.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }
}

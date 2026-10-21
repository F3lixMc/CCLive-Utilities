package net.felix.utilities.Town;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Zentrale Slot-Zuordnung für Build-Share (Ausrüstung + Karten).
 * Nicht an anderen Stellen hart kodieren — hier erweitern.
 */
public final class BuildSlotMapping {

    /** Erfassungszustand eines Build-Slots. */
    public enum SlotState {
        UNKNOWN,
        EMPTY,
        LOCKED,
        FILLED
    }

    public enum EquipmentCategory {
        /** Rüstung/Waffen/Tools: leere Slots = Paper-Platzhalter (nicht isEmpty). */
        ARMOR("Rüstung", new int[]{2, 11, 20, 29, 38, 6, 15, 24, 33, 42}, false),
        WEAPONS("Waffen", new int[]{48, 50}, false),
        TOOLS("Werkzeuge", new int[]{1, 10, 19, 28}, false),
        /** Module: "Slot gesperrt" / Locked-Lore / ggf. Paper. Fünf Slots laut Mapping. */
        MODULES("Module", new int[]{7, 16, 25, 34, 43}, true);

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

        /** Waffen/Rüstung/Tools: keine Platzhalter; Module: ja. */
        public boolean usesPlaceholders() {
            return usesPlaceholders;
        }

        public String slotLabel(int slotIndexInCategory) {
            if (this == MODULES) {
                return "Modulslot " + (slotIndexInCategory + 1);
            }
            return displayName + " " + (slotIndexInCategory + 1);
        }
    }

    /**
     * Karten-Slots im Karten-Inventar (Zeichen {@code cards}).
     * 20 Slots (Erweiterung von zuvor 12): mittlere 5 Spalten × 4 Reihen.
     */
    public static final int[] CARD_SLOTS = {
        4, 5, 6, 7, 8,
        13, 14, 15, 16, 17,
        22, 23, 24, 25, 26,
        31, 32, 33, 34, 35
    };

    public static final String PLACEHOLDER_ITEM_NAME = "Slot gesperrt";

    /** Gesperrte Modul-/Karten-Slots (Lore, Schreibweise wie im Spiel). */
    public static final String LOCKED_SLOT_LORE = "Nicht freigeschalten!";

    /** Alternative Schreibweise, falls Server korrigiert. */
    public static final String LOCKED_SLOT_LORE_ALT = "Nicht freigeschaltet!";

    private BuildSlotMapping() {
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

    /**
     * Slots für Equipment-Statistik-Overlays (ohne Module).
     * Entspricht dem bisherigen {@code EquipmentDisplayUtility}-Mapping.
     */
    public static int[] getEquipmentStatSlots() {
        int[] armor = EquipmentCategory.ARMOR.slots;
        int[] weapons = EquipmentCategory.WEAPONS.slots;
        int[] tools = EquipmentCategory.TOOLS.slots;
        int[] result = new int[armor.length + weapons.length + tools.length];
        int i = 0;
        for (int s : tools) result[i++] = s;
        for (int s : armor) result[i++] = s;
        for (int s : weapons) result[i++] = s;
        // stabile Reihenfolge wie bisher: 1,2,6,10,11,15,19,20,24,28,29,33,38,42,48,50
        int[] ordered = {1, 2, 6, 10, 11, 15, 19, 20, 24, 28, 29, 33, 38, 42, 48, 50};
        return ordered.clone();
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
}

package net.felix.utilities.Town;

import net.felix.utilities.ItemViewer.ItemData;
import net.felix.utilities.Town.BuildSlotMapping.PieceType;
import net.felix.utilities.Town.CustomKit.EquipmentEntry;

import java.util.HashSet;
import java.util.Set;

/**
 * Typgerechte Slotvergabe für eigene und neu hinzugefügte Kit-Items.
 */
public final class CustomKitSlotAssigner {

    public enum AddResult {
        ADDED,
        REPLACED,
        NEEDS_REPLACE,
        UNKNOWN_TYPE,
        IGNORED
    }

    public static final class EquipmentAdd {
        public final AddResult result;
        public final Integer occupiedSlot;
        public final String occupiedName;

        public EquipmentAdd(AddResult result, Integer occupiedSlot, String occupiedName) {
            this.result = result;
            this.occupiedSlot = occupiedSlot;
            this.occupiedName = occupiedName;
        }
    }

    private CustomKitSlotAssigner() {
    }

    private static int firstOccupiedSlot(CustomKit kit, int[] slots) {
        Set<Integer> used = usedSlots(kit);
        for (int slot : slots) {
            if (used.contains(slot)) {
                return slot;
            }
        }
        return slots[0];
    }

    public static Integer firstFreeSlot(CustomKit kit, PieceType type) {
        if (type == null) {
            return null;
        }
        int[] slots = BuildSlotMapping.assignmentSlots(type);
        if (slots.length == 0) {
            return null;
        }
        Set<Integer> used = usedSlots(kit);
        for (int slot : slots) {
            if (!used.contains(slot)) {
                return slot;
            }
        }
        return null;
    }

    public static EquipmentAdd addOrOfferReplace(CustomKit kit, ItemData item, boolean replaceOccupied) {
        kit.ensureEquipmentEntries();
        PieceType type = BuildSlotMapping.resolvePieceType(item);
        if (type == null || type == PieceType.FISHING_ROD || type == PieceType.POWER_CRYSTAL_OVERVIEW) {
            return new EquipmentAdd(AddResult.UNKNOWN_TYPE, null, null);
        }
        Integer free = firstFreeSlot(kit, type);
        if (free != null) {
            EquipmentEntry entry = new EquipmentEntry();
            entry.jeiName = item.name;
            entry.slotType = type.getLabel();
            entry.inventorySlot = free;
            entry.originalItem = null;
            kit.equipmentEntries.add(entry);
            kit.syncDerivedLists();
            return new EquipmentAdd(AddResult.ADDED, free, item.name);
        }
        int[] slots = BuildSlotMapping.assignmentSlots(type);
        if (slots.length == 0) {
            return new EquipmentAdd(AddResult.UNKNOWN_TYPE, null, null);
        }
        int occupied = firstOccupiedSlot(kit, slots);
        if (!replaceOccupied) {
            EquipmentEntry existing = entryAtSlot(kit, occupied);
            return new EquipmentAdd(AddResult.NEEDS_REPLACE,
                occupied,
                existing != null ? existing.jeiName : null);
        }
        replaceSlot(kit, occupied, item, type);
        return new EquipmentAdd(AddResult.REPLACED, occupied, item.name);
    }

    public static void replaceSlot(CustomKit kit, int slot, ItemData item, PieceType type) {
        kit.ensureEquipmentEntries();
        EquipmentEntry existing = entryAtSlot(kit, slot);
        if (existing != null) {
            existing.jeiName = item.name;
            existing.slotType = type != null ? type.getLabel() : existing.slotType;
            existing.inventorySlot = slot;
            existing.originalItem = null;
            existing.capturedAspectNames = null;
        } else {
            EquipmentEntry entry = new EquipmentEntry();
            entry.jeiName = item.name;
            entry.slotType = type != null ? type.getLabel() : null;
            entry.inventorySlot = slot;
            kit.equipmentEntries.add(entry);
        }
        kit.rebuildAspectNamesFromEntries();
        kit.syncDerivedLists();
    }

    public static void removeSlot(CustomKit kit, int index) {
        kit.ensureEquipmentEntries();
        if (index < 0 || index >= kit.equipmentEntries.size()) {
            return;
        }
        kit.equipmentEntries.remove(index);
        kit.rebuildAspectNamesFromEntries();
        kit.syncDerivedLists();
    }

    public static EquipmentEntry entryAtSlot(CustomKit kit, int slot) {
        if (kit.equipmentEntries == null) {
            return null;
        }
        for (EquipmentEntry entry : kit.equipmentEntries) {
            if (entry != null && entry.inventorySlot != null && entry.inventorySlot == slot) {
                return entry;
            }
        }
        return null;
    }

    public static Set<Integer> usedSlots(CustomKit kit) {
        Set<Integer> used = new HashSet<>();
        if (kit.equipmentEntries == null) {
            return used;
        }
        for (EquipmentEntry entry : kit.equipmentEntries) {
            if (entry != null && entry.inventorySlot != null) {
                used.add(entry.inventorySlot);
            }
        }
        return used;
    }
}

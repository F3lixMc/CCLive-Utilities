package net.felix.utilities.ItemViewer;

/**
 * Datenstruktur für einen Fundort eines Items
 */
public class LocationData {
    public String location;      // "Chest", "Shop", "Mine", etc. (optional)
    public String floor;         // "floor_1", "floor_2", "Ebene 28", etc.
    public String collection;   // "Eichenholz III", "Kohle I", etc. (optional, für Module/Modultaschen)
    public String coordinates;   // "X: 100, Y: 50, Z: -200" (optional)
    public String biome;         // "Plains" (optional)
    public String chestType;     // "common", "rare", etc. (optional)
    public String shopName;      // Name des Shops (optional)
}


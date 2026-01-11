package net.felix.utilities.ItemViewer;

import com.google.gson.annotations.SerializedName;

/**
 * Datenstruktur für die Kosten eines Items
 */
public class PriceData {
    public CostItem coin;        // Coins
    public CostItem cactus;      // Kaktus
    public CostItem soul;        // Seelen
    public CostItem material1;   // Ebenen Material 1
    public CostItem material2;   // Ebenen Material 2
    public CostItem material3;   // Ebenen Material 3
    public CostItem material4;   // Ebenen Material 4
    public CostItem material5;   // Ebenen Material 5
    public CostItem Amboss;     // Amboss Item (großgeschrieben in JSON)
    public CostItem amboss;     // Amboss Item (kleingeschrieben in JSON)
    public CostItem Ressource;  // Ressourcen Item (großgeschrieben in JSON)
    public CostItem ressource;  // Ressourcen Item (kleingeschrieben in JSON)
    @SerializedName("Level")
    public CostItem Level;       // Farmzone Level (für Lizenzen)
    public CostItem paper_shreds; // Pergamentfetzen (für blueprint_shop)
    public CostItem time;        // Zeit (für Power Crystal Slots)
}


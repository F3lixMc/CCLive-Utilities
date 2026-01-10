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
    @SerializedName("amboss")
    public CostItem Amboss;      // Amboss Item
    @SerializedName("ressource")
    public CostItem Ressource;   // Ressourcen Item
    @SerializedName("Level")
    public CostItem Level;       // Farmzone Level (für Lizenzen)
    public CostItem paper_shreds; // Pergamentfetzen (für blueprint_shop)
    public CostItem time;        // Zeit (für Power Crystal Slots)
}


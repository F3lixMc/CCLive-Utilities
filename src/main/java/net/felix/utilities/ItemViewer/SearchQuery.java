package net.felix.utilities.ItemViewer;

import java.util.ArrayList;
import java.util.List;

/**
 * Datenstruktur für eine Suchanfrage
 */
public class SearchQuery {
    public List<String> tags = new ArrayList<>();      // #tag
    public String aspect;                              // Aspekt-Name
    public String nameSearch = "";                     // Name-Suche
    public Integer floor;                              // Floor/Ebene (z.B. 49) - exakte Suche
    public List<CostFilter> costFilters = new ArrayList<>(); // Kosten-Filter
    public List<ModifierFilter> modifierFilters = new ArrayList<>(); // Modifier-Filter
    public List<StatFilter> statFilters = new ArrayList<>(); // Stat-Filter (z.B. @Abbaugeschwindigkeit>100)
    public List<FloorFilter> floorFilters = new ArrayList<>(); // Floor-Filter mit Vergleichsoperatoren (z.B. @Ebene>50)
}


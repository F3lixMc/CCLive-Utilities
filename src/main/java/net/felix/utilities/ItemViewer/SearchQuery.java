package net.felix.utilities.ItemViewer;

import java.util.ArrayList;
import java.util.List;

/**
 * Datenstruktur für eine Suchanfrage
 */
public class SearchQuery {
    public List<String> tags = new ArrayList<>();      // @tag
    public String aspect;                              // Aspekt-Name
    public String nameSearch = "";                     // Name-Suche
    public List<CostFilter> costFilters = new ArrayList<>(); // Kosten-Filter
}


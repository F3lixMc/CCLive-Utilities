package net.felix.utilities.Aincraft.itemviewer.filtermenu;

import java.util.List;

public class FilterGroupDef {
    final String title;
    final List<FilterOptionDef> options;

    FilterGroupDef(String title, List<FilterOptionDef> options) {
        this.title = title;
        this.options = options;
    }
}

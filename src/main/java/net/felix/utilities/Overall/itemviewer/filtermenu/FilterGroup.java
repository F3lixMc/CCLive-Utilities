package net.felix.utilities.Overall.itemviewer.filtermenu;

import java.util.List;

public class FilterGroup {
    final String title;
    final List<FilterOption> options;
    int columnX;
    int columnWidth;
    int headerY;
    int bottomY;

    FilterGroup(String title, List<FilterOption> options) {
        this.title = title;
        this.options = options;
    }
}

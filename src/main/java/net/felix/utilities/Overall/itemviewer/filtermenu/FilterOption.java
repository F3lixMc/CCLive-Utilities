package net.felix.utilities.Overall.itemviewer.filtermenu;

import java.util.List;

public class FilterOption {
    final String label;
    final List<String> tokens;
    final RowInputType inputType;
    int x;
    int y;
    int width;
    int countFieldX;
    int countFieldW;
    int pickerButtonX = -1;

    FilterOption(String label, List<String> tokens, RowInputType inputType) {
        this.label = label;
        this.tokens = tokens;
        this.inputType = inputType;
    }
}

package net.felix.utilities.Overall.itemviewer.search;


public record CostNamePickerEntry(String name, int textColor, int floor) {
    public String floorLabel() {
        return floor > 0 ? "e" + floor : "";
    }
}

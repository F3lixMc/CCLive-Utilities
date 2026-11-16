package net.felix.features.itemviewer.search;


public record CostNamePickerEntry(String name, int textColor, int floor) {
    public String floorLabel() {
        return floor > 0 ? "e" + floor : "";
    }
}

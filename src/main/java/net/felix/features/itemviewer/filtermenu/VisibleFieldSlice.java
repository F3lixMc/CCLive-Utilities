package net.felix.features.itemviewer.filtermenu;


public record VisibleFieldSlice(String display, int displayCursor, int displaySelStart, int displaySelEnd,
                                 boolean centered) {
}

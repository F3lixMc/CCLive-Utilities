package net.felix.utilities.DragOverlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class F6MenuBackgroundTest {

    @Test
    void transparencyMapsOntoThePreviousDarkness() {
        assertEquals(255, F6MenuBackground.alpha(0));
        assertEquals(0, F6MenuBackground.alpha(100));
        assertEquals(33, F6MenuBackground.alpha(87));
    }
}

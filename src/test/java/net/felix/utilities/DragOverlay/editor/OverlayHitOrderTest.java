package net.felix.utilities.DragOverlay.editor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OverlayHitOrderTest {

    @Test
    void overlappingRectanglesHitTheLaterOne() {
        List<Rect> rects = List.of(new Rect(0, 0, 10, 10), new Rect(5, 5, 10, 10));
        assertEquals(rects.get(1), OverlayHitOrder.topmost(rects, rect -> rect.contains(6, 6)));
        assertEquals(rects.get(0), OverlayHitOrder.topmost(rects, rect -> rect.contains(1, 1)));
    }

    @Test
    void capturedOverlayIsNotReplacedByALowerOne() {
        Rect captured = new Rect(0, 0, 10, 10);
        Rect above = new Rect(5, 5, 10, 10);
        Rect chosen = captured;
        Rect hit = OverlayHitOrder.topmost(List.of(captured, above), rect -> rect.contains(6, 6));
        assertEquals(above, hit);
        assertEquals(captured, chosen);
    }

    @Test
    void missReturnsNull() {
        assertNull(OverlayHitOrder.topmost(List.of(new Rect(0, 0, 4, 4)), rect -> rect.contains(9, 9)));
    }

    private record Rect(int x, int y, int width, int height) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}

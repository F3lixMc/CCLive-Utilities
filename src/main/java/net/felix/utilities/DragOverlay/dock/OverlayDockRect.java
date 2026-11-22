package net.felix.utilities.DragOverlay.dock;

/** Achsenparalleles Rechteck in GUI-Pixeln. */
public record OverlayDockRect(int x, int y, int w, int h) {
	public int right() {
		return x + w;
	}

	public int bottom() {
		return y + h;
	}

	public OverlayDockRect withPos(int nextX, int nextY) {
		return new OverlayDockRect(nextX, nextY, w, h);
	}

	public OverlayDockRect withSize(int nextW, int nextH) {
		return new OverlayDockRect(x, y, nextW, nextH);
	}
}

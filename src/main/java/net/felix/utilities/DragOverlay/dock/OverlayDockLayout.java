package net.felix.utilities.DragOverlay.dock;

import net.felix.utilities.DragOverlay.dock.model.OverlayDockRect;
import net.felix.utilities.DragOverlay.dock.resolve.OverlayDockEngine;
import net.felix.utilities.DragOverlay.dock.resolve.OverlayDockScreenFit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.felix.utilities.DragOverlay.DraggableOverlay;
import net.felix.utilities.DragOverlay.editor.OverlayEditorUtility;
import net.minecraft.client.Minecraft;

/**
 * Aufgelöste Rechtecke für Editor und HUD. Schreibt keine Config.
 * {@link #placeLive} merkt sich die Live-Größe und lässt die Editor-Vorschau unverändert.
 */
public final class OverlayDockLayout {
	public static final int GAP = 0;
	public static final int SNAP_DISTANCE = 10;
	public static final int DETACH_THRESHOLD = 3;

	private static final class Sample {
		private final String id;
		private final int x;
		private final int y;
		private final int w;
		private final int h;
		private final boolean drawn;

		private Sample(String id, int x, int y, int w, int h, boolean drawn) {
			this.id = id;
			this.x = x;
			this.y = y;
			this.w = Math.max(1, w);
			this.h = Math.max(1, h);
			this.drawn = drawn;
		}


		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Sample sample)) {
				return false;
			}
			return drawn == sample.drawn
					&& x == sample.x && y == sample.y && w == sample.w && h == sample.h
					&& Objects.equals(id, sample.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, x, y, w, h, drawn);
		}
	}

	private static final Map<String, Sample> samples = new LinkedHashMap<>();
	private static final Map<String, int[]> lastLiveSize = new LinkedHashMap<>();
	private static final Map<String, BooleanSupplier> runtimeDrawn = new HashMap<>();
	private static final Set<String> seenLive = new HashSet<>();
	private static final Map<String, OverlayDockRect> reports = new LinkedHashMap<>();
	private static Map<String, OverlayDockRect> cache;
	private static Map<String, OverlayDockRect> liveVisual = new LinkedHashMap<>();
	private static Map<String, Float> liveFactors = new LinkedHashMap<>();
	private static Map<String, OverlayDockScreenFit.Pin> livePins = new LinkedHashMap<>();
	private static Map<String, Float> editorFactors = new LinkedHashMap<>();
	private static Map<String, OverlayDockScreenFit.Pin> editorPins = new LinkedHashMap<>();
	private static boolean measuring;
	private static boolean prepared;
	private static String skipId;
	private static int cachedScreenW = -1;
	private static int cachedScreenH = -1;
	private static int cachedVisibility = Integer.MIN_VALUE;

	private OverlayDockLayout() {
	}

	public static void initialize() {
		// Kein Tick-Wipe: ein geleerter Cache hat die Vorschau zittern lassen.
	}

	/** Laufzeit-Sichtbarkeit, zum Beispiel die Spielregion. Die Config-Verbindung bleibt davon unberührt. */
	public static void bindRuntimeDrawn(String id, BooleanSupplier drawn) {
		if (id != null && drawn != null) {
			runtimeDrawn.put(id, drawn);
		}
	}

	public static void invalidate() {
		cache = null;
		samples.clear();
		lastLiveSize.clear();
		liveVisual = new LinkedHashMap<>();
		liveFactors = new LinkedHashMap<>();
		livePins = new LinkedHashMap<>();
		editorFactors = new LinkedHashMap<>();
		editorPins = new LinkedHashMap<>();
		prepared = false;
	}

	public static void invalidatePositions() {
		cache = null;
		editorPins = new LinkedHashMap<>();
		livePins = new LinkedHashMap<>();
	}

	public static void setEditorDragSkip(String dockId) {
		skipId = dockId;
		cache = null;
	}

	public static void clearEditorDragSkip() {
		skipId = null;
		cache = null;
		editorPins = new LinkedHashMap<>();
	}

	public static void onEditorClosed() {
		clearEditorDragSkip();
	}

	/**
	 * Overlays, die seit dem letzten Tick nicht gezeichnet wurden, sind keine Anker.
	 * Die gespeicherte Verbindung bleibt. Der Aufruf liegt vor dem HUD-Rendern.
	 */
	public static void expireUnseen() {
		if (OverlayEditorUtility.isOverlayEditorOpen()) {
			return;
		}
		List<Sample> previous = new ArrayList<>(samples.values());
		for (Sample sample : previous) {
			if (seenLive.contains(sample.id) || !sample.drawn) {
				continue;
			}
			remember(sample.id, sample.x, sample.y, sample.w, sample.h, false);
		}
		seenLive.clear();
		prepared = false;
	}

	public static boolean isMeasuring() {
		return measuring;
	}

	/**
	 * Sammelt zuerst die sichtbaren Größen, rechnet das Layout einmal und gibt es danach zum Zeichnen frei.
	 * Ein zweiter Aufruf im selben Bild ändert nichts. Ausgeblendete Overlays werden nicht abgewartet.
	 */
	public static void prepareFrame() {
		if (prepared || OverlayEditorUtility.isOverlayEditorOpen()) {
			return;
		}
		measuring = true;
		reports.clear();
		try {
			OverlayDockMeasures.collect();
		} finally {
			measuring = false;
		}
		int screenW = screenWidth();
		int screenH = screenHeight();
		OverlayDockScreenFit.Fit fit = OverlayDockEngine.resolveFitted(
				reports, OverlayDockStore.links(), null, screenW, screenH, livePins);
		liveVisual = fit.rects();
		liveFactors = fit.factors();
		livePins = fit.pins();
		seenLive.clear();
		seenLive.addAll(reports.keySet());
		prepared = true;
	}

	public static float effectiveScale(String id, float savedScale) {
		float saved = savedScale > 0f ? savedScale : 1f;
		return saved * factor(id);
	}

	public static float factor(String id) {
		if (id == null) {
			return 1f;
		}
		Float value = OverlayEditorUtility.isOverlayEditorOpen() ? editorFactors.get(id) : liveFactors.get(id);
		if (value == null || value <= 0f || value > 1f) {
			return 1f;
		}
		return value;
	}

	public static int shownWidth(DraggableOverlay overlay) {
		OverlayDockRect rect = shown(overlay);
		return rect == null ? (overlay == null ? 0 : overlay.getWidth()) : rect.w();
	}

	public static int shownHeight(DraggableOverlay overlay) {
		OverlayDockRect rect = shown(overlay);
		return rect == null ? (overlay == null ? 0 : overlay.getHeight()) : rect.h();
	}

	private static OverlayDockRect shown(DraggableOverlay overlay) {
		if (overlay == null || overlay.dockId() == null || overlay.dockId().isBlank()) {
			return null;
		}
		if (OverlayEditorUtility.isOverlayEditorOpen()) {
			return cache == null ? null : cache.get(overlay.dockId());
		}
		return liveVisual.get(overlay.dockId());
	}

	public static boolean shouldDetach(int deltaX, int deltaY) {
		return Math.max(Math.abs(deltaX), Math.abs(deltaY)) > DETACH_THRESHOLD;
	}

	public static int x(DraggableOverlay overlay) {
		if (overlay == null || overlay.dockId() == null || overlay.dockId().isBlank()) {
			return overlay == null ? 0 : overlay.undockedX();
		}
		if (overlay.dockId().equals(skipId)) {
			return overlay.undockedX();
		}
		if (OverlayEditorUtility.isOverlayEditorOpen()) {
			ensureResolved();
			OverlayDockRect rect = cache == null ? null : cache.get(overlay.dockId());
			return rect == null ? overlay.undockedX() : rect.x();
		}
		OverlayDockRect rect = liveVisual.get(overlay.dockId());
		return rect == null ? overlay.undockedX() : rect.x();
	}

	public static int y(DraggableOverlay overlay) {
		if (overlay == null || overlay.dockId() == null || overlay.dockId().isBlank()) {
			return overlay == null ? 0 : overlay.undockedY();
		}
		if (overlay.dockId().equals(skipId)) {
			return overlay.undockedY();
		}
		if (OverlayEditorUtility.isOverlayEditorOpen()) {
			ensureResolved();
			OverlayDockRect rect = cache == null ? null : cache.get(overlay.dockId());
			return rect == null ? overlay.undockedY() : rect.y();
		}
		OverlayDockRect rect = liveVisual.get(overlay.dockId());
		return rect == null ? overlay.undockedY() : rect.y();
	}

	/** Editor-Vorschau aus den freien Rechtecken der gerade gezeichneten Overlays. */
	public static void syncEditor(List<DraggableOverlay> overlays, int screenW, int screenH) {
		List<Sample> previous = new ArrayList<>(samples.values());
		for (Sample sample : previous) {
			samples.put(sample.id, new Sample(sample.id, sample.x, sample.y, sample.w, sample.h, false));
			cache = null;
		}
		if (overlays != null) {
			for (DraggableOverlay overlay : overlays) {
				if (overlay == null || overlay.dockId() == null || overlay.dockId().isBlank()) {
					continue;
				}
				if (!overlay.isDrawnInEditor()) {
					continue;
				}
				remember(overlay.dockId(), overlay.undockedX(), overlay.undockedY(), overlay.getWidth(), overlay.getHeight(), true);
			}
		}
		resolveIfNeeded(screenW, screenH, true);
	}

	/**
	 * Live-Rechteck. Die übergebene Größe ist die gerade gezeichnete, skalierte Fläche.
	 * Bei offenem Editor bleibt die Vorschau unverändert und das HUD teilt sich ihre Ecke.
	 */
	public static OverlayDockRect placeLive(String id, int freeX, int freeY, int scaledW, int scaledH) {
		int w = Math.max(1, scaledW);
		int h = Math.max(1, scaledH);
		if (id == null || id.isBlank()) {
			return new OverlayDockRect(freeX, freeY, w, h);
		}
		if (measuring) {
			reports.put(id, new OverlayDockRect(freeX, freeY, w, h));
			return new OverlayDockRect(freeX, freeY, w, h);
		}
		if (!OverlayEditorUtility.isOverlayEditorOpen()) {
			OverlayDockRect placed = liveVisual.get(id);
			return placed == null ? new OverlayDockRect(freeX, freeY, w, h) : placed;
		}
		ensureResolved();
		OverlayDockRect preview = cache == null ? null : cache.get(id);
		return preview == null ? new OverlayDockRect(freeX, freeY, w, h) : preview;
	}

	private static void remember(String id, int x, int y, int w, int h, boolean drawn) {
		Sample next = new Sample(id, x, y, w, h, drawn);
		Sample previous = samples.get(id);
		if (!next.equals(previous)) {
			samples.put(id, next);
			cache = null;
		}
	}

	private static void ensureResolved() {
		resolveIfNeeded(screenWidth(), screenHeight(), OverlayEditorUtility.isOverlayEditorOpen());
	}

	private static void resolveIfNeeded(int screenW, int screenH, boolean editor) {
		int visibility = editor ? 0 : visibilityStamp();
		if (cache != null && screenW == cachedScreenW && screenH == cachedScreenH && visibility == cachedVisibility) {
			return;
		}
		cachedScreenW = screenW;
		cachedScreenH = screenH;
		cachedVisibility = visibility;
		Map<String, OverlayDockRect> desired = new LinkedHashMap<>();
		for (Sample sample : samples.values()) {
			if (!include(sample, editor)) {
				continue;
			}
			int w = sample.w;
			int h = sample.h;
			if (!editor) {
				int[] live = lastLiveSize.get(sample.id);
				if (live != null) {
					w = live[0];
					h = live[1];
				}
			}
			desired.put(sample.id, new OverlayDockRect(sample.x, sample.y, w, h));
		}
		OverlayDockScreenFit.Fit fit = OverlayDockEngine.resolveFitted(
				desired, OverlayDockStore.links(), skipId, screenW, screenH, editor ? editorPins : livePins);
		cache = fit.rects();
		if (editor) {
			editorFactors = fit.factors();
			editorPins = fit.pins();
		}
	}

	private static boolean include(Sample sample, boolean editor) {
		if (sample.id.equals(skipId)) {
			return true;
		}
		if (editor) {
			return sample.drawn;
		}
		return runtimeVisible(sample.id, sample.drawn);
	}

	private static boolean runtimeVisible(String id, boolean sampleDrawn) {
		BooleanSupplier supplier = runtimeDrawn.get(id);
		if (supplier == null) {
			return sampleDrawn;
		}
		try {
			return supplier.getAsBoolean();
		} catch (RuntimeException ignored) {
			return false;
		}
	}

	private static int visibilityStamp() {
		int stamp = 1;
		for (Map.Entry<String, BooleanSupplier> entry : runtimeDrawn.entrySet()) {
			stamp = 31 * stamp + entry.getKey().hashCode();
			stamp = 31 * stamp + Boolean.hashCode(runtimeVisible(entry.getKey(), false));
		}
		return stamp;
	}

	private static int screenWidth() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getWindow() == null) {
			return Math.max(1, cachedScreenW);
		}
		return client.getWindow().getGuiScaledWidth();
	}

	private static int screenHeight() {
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.getWindow() == null) {
			return Math.max(1, cachedScreenH);
		}
		return client.getWindow().getGuiScaledHeight();
	}
}

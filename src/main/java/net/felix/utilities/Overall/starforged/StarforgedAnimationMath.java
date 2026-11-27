package net.felix.utilities.Overall.starforged;

/**
 * Pure easing and color helpers for the starforged animation overlay.
 * Kept free of Minecraft classes so it can be unit tested.
 */
final class StarforgedAnimationMath {

	private StarforgedAnimationMath() {
	}

	static float clamp01(float value) {
		return value < 0f ? 0f : (value > 1f ? 1f : value);
	}

	/** Linear progress of {@code time} inside [start, start + duration], clamped to 0..1. */
	static float progress(float time, float start, float duration) {
		if (duration <= 0f) {
			return time >= start ? 1f : 0f;
		}
		return clamp01((time - start) / duration);
	}

	static float easeOutCubic(float t) {
		float inv = 1f - clamp01(t);
		return 1f - inv * inv * inv;
	}

	static float easeInCubic(float t) {
		float c = clamp01(t);
		return c * c * c;
	}

	static float easeInQuad(float t) {
		float c = clamp01(t);
		return c * c;
	}

	/** Overshoots slightly past 1 before settling; good for "pop in" motions. */
	static float easeOutBack(float t) {
		float c = clamp01(t) - 1f;
		final float s = 1.70158f;
		return 1f + c * c * ((s + 1f) * c + s);
	}

	/** Springy bounce that settles at 1. */
	static float easeOutElastic(float t) {
		float c = clamp01(t);
		if (c == 0f || c == 1f) {
			return c;
		}
		final double period = (2.0 * Math.PI) / 3.0;
		return (float) (Math.pow(2.0, -10.0 * c) * Math.sin((c * 10.0 - 0.75) * period) + 1.0);
	}

	/** Decaying pulse: 1 at {@code start}, then fades to 0 over {@code duration}; 0 before start. */
	static float pulse(float time, float start, float duration) {
		if (time < start) {
			return 0f;
		}
		return 1f - easeOutCubic(progress(time, start, duration));
	}

	/** Reveal progress (0..1) of the letter at {@code index}; letters start {@code stagger} ms apart. */
	static float letterProgress(float time, int index, float start, float stagger, float duration) {
		return progress(time, start + index * stagger, duration);
	}

	/** Highlight strength (0..1) of a letter for a shimmer band centered at {@code bandCenter}. */
	static float shimmer(int index, float bandCenter, float bandHalfWidth) {
		if (bandHalfWidth <= 0f) {
			return 0f;
		}
		return clamp01(1f - Math.abs(index - bandCenter) / bandHalfWidth);
	}

	/** Mixes two RGB colors (alpha bits ignored); {@code t} = 0 gives {@code from}. */
	static int lerpRgb(int from, int to, float t) {
		float c = clamp01(t);
		int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * c);
		int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * c);
		int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * c);
		return (r << 16) | (g << 8) | b;
	}

	/**
	 * RGB for a hue in turns (0..1 wraps), with the given saturation and brightness in 0..1.
	 * Used for the rainbow parts of the animation.
	 */
	static int hsvToRgb(float hue, float saturation, float value) {
		float h = hue - (float) Math.floor(hue);
		float s = clamp01(saturation);
		float v = clamp01(value);
		float sector = h * 6f;
		int i = (int) Math.floor(sector) % 6;
		float f = sector - (float) Math.floor(sector);
		float p = v * (1f - s);
		float q = v * (1f - s * f);
		float t = v * (1f - s * (1f - f));
		float r;
		float g;
		float b;
		switch (i) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
	}

	/** Builds an ARGB color from an RGB value and an alpha in 0..1. */
	static int argb(float alpha, int rgb) {
		int a = Math.round(clamp01(alpha) * 255f);
		return (a << 24) | (rgb & 0xFFFFFF);
	}
}

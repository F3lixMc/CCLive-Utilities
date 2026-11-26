package net.felix.utilities.Overall.fragmentdrop;

/**
 * Pure easing and color helpers for the talisman fragment drop overlay.
 * Kept free of Minecraft classes so it can be unit tested.
 */
final class FragmentDropAnimationMath {

	private FragmentDropAnimationMath() {
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

	/** Builds an ARGB color from an RGB value and an alpha in 0..1. */
	static int argb(float alpha, int rgb) {
		int a = Math.round(clamp01(alpha) * 255f);
		return (a << 24) | (rgb & 0xFFFFFF);
	}
}

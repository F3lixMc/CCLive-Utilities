package net.felix.utilities.Overall.fragmentdrop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FragmentDropAnimationMathTest {

	private static final float EPS = 1e-4f;

	@Test
	void easingsStartAtZeroAndEndAtOne() {
		assertEquals(0f, FragmentDropAnimationMath.easeOutCubic(0f), EPS);
		assertEquals(1f, FragmentDropAnimationMath.easeOutCubic(1f), EPS);
		assertEquals(0f, FragmentDropAnimationMath.easeInCubic(0f), EPS);
		assertEquals(1f, FragmentDropAnimationMath.easeInCubic(1f), EPS);
		assertEquals(0f, FragmentDropAnimationMath.easeOutBack(0f), EPS);
		assertEquals(1f, FragmentDropAnimationMath.easeOutBack(1f), EPS);
		assertEquals(0f, FragmentDropAnimationMath.easeOutElastic(0f), EPS);
		assertEquals(1f, FragmentDropAnimationMath.easeOutElastic(1f), EPS);
	}

	@Test
	void easingsClampOutOfRangeInput() {
		assertEquals(1f, FragmentDropAnimationMath.easeOutCubic(5f), EPS);
		assertEquals(0f, FragmentDropAnimationMath.easeInCubic(-2f), EPS);
		assertEquals(1f, FragmentDropAnimationMath.easeOutElastic(3f), EPS);
	}

	@Test
	void easeOutBackOvershoots() {
		assertTrue(FragmentDropAnimationMath.easeOutBack(0.7f) > 1f);
	}

	@Test
	void progressIsClampedAndHandlesZeroDuration() {
		assertEquals(0f, FragmentDropAnimationMath.progress(50f, 100f, 200f), EPS);
		assertEquals(0.5f, FragmentDropAnimationMath.progress(200f, 100f, 200f), EPS);
		assertEquals(1f, FragmentDropAnimationMath.progress(900f, 100f, 200f), EPS);
		assertEquals(1f, FragmentDropAnimationMath.progress(100f, 100f, 0f), EPS);
		assertEquals(0f, FragmentDropAnimationMath.progress(99f, 100f, 0f), EPS);
	}

	@Test
	void lettersRevealStaggered() {
		// Letter 2 starts at 100 + 2 * 40 = 180 ms.
		assertEquals(0f, FragmentDropAnimationMath.letterProgress(180f, 2, 100f, 40f, 200f), EPS);
		assertEquals(0.5f, FragmentDropAnimationMath.letterProgress(280f, 2, 100f, 40f, 200f), EPS);
		assertEquals(1f, FragmentDropAnimationMath.letterProgress(280f, 0, 100f, 40f, 100f), EPS);
	}

	@Test
	void shimmerPeaksAtBandCenter() {
		assertEquals(1f, FragmentDropAnimationMath.shimmer(5, 5f, 2f), EPS);
		assertEquals(0.5f, FragmentDropAnimationMath.shimmer(4, 5f, 2f), EPS);
		assertEquals(0f, FragmentDropAnimationMath.shimmer(1, 5f, 2f), EPS);
		assertEquals(0f, FragmentDropAnimationMath.shimmer(5, 5f, 0f), EPS);
	}

	@Test
	void colorHelpers() {
		assertEquals(0x000000, FragmentDropAnimationMath.lerpRgb(0x000000, 0xFFFFFF, 0f));
		assertEquals(0xFFFFFF, FragmentDropAnimationMath.lerpRgb(0x000000, 0xFFFFFF, 1f));
		assertEquals(0x808080, FragmentDropAnimationMath.lerpRgb(0x000000, 0xFFFFFF, 0.5f));
		assertEquals(0xFF55FFFF, FragmentDropAnimationMath.argb(1f, 0x55FFFF));
		assertEquals(0x0055FFFF, FragmentDropAnimationMath.argb(-1f, 0xAA55FFFF));
		assertEquals(0x8012AB34, FragmentDropAnimationMath.argb(0.5f, 0x12AB34));
	}
}

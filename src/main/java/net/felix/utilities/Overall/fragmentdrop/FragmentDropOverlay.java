package net.felix.utilities.Overall.fragmentdrop;

import static net.felix.utilities.Overall.fragmentdrop.FragmentDropAnimationMath.argb;
import static net.felix.utilities.Overall.fragmentdrop.FragmentDropAnimationMath.clamp01;
import static net.felix.utilities.Overall.fragmentdrop.FragmentDropAnimationMath.easeInCubic;
import static net.felix.utilities.Overall.fragmentdrop.FragmentDropAnimationMath.easeOutBack;
import static net.felix.utilities.Overall.fragmentdrop.FragmentDropAnimationMath.easeOutCubic;
import static net.felix.utilities.Overall.fragmentdrop.FragmentDropAnimationMath.easeOutElastic;
import static net.felix.utilities.Overall.fragmentdrop.FragmentDropAnimationMath.letterProgress;
import static net.felix.utilities.Overall.fragmentdrop.FragmentDropAnimationMath.lerpRgb;
import static net.felix.utilities.Overall.fragmentdrop.FragmentDropAnimationMath.progress;
import static net.felix.utilities.Overall.fragmentdrop.FragmentDropAnimationMath.shimmer;

import java.util.Locale;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

/**
 * Screen-space part of the talisman fragment drop animation, in three acts:
 * <ol>
 * <li>Charge (0..{@link #IMPACT}): cinematic bars, dimmed screen, motes rushing into a growing core.</li>
 * <li>Impact: flash, screen shake, shockwave rings, spark burst, light pillar, rotating rays and the
 * fragment popping in over a ghost of the full charm, with orbiting stars.</li>
 * <li>Reveal: label, letter-by-letter shimmering title, effect subtitle; then everything fades out.</li>
 * </ol>
 */
final class FragmentDropOverlay {

	/** What to show; textures may be null, then a prismarine shard is drawn instead. */
	record Visual(String label, String title, String subtitle, Identifier fragmentTexture, Identifier ghostTexture) {
	}

	// Timeline in milliseconds.
	static final long TOTAL_MS = 5600L;
	static final float IMPACT = 900f;
	private static final float OUTRO_START = 4800f;
	private static final float TITLE_START = IMPACT + 320f;
	private static final float LETTER_STAGGER = 32f;
	private static final float LETTER_DURATION = 300f;
	private static final float SHIMMER_PERIOD = 1400f;

	// Charm palette: gold rim and amethyst plate, like the charm textures.
	private static final int COLOR_GOLD_HI = 0xF9EC6B;
	private static final int COLOR_GOLD = 0xEAAB4A;
	private static final int COLOR_AMETHYST = 0x9B5DE5;
	private static final int COLOR_LILAC = 0xD7B8FF;
	private static final int COLOR_WHITE = 0xFFFFFF;
	private static final int COLOR_CREAM = 0xFFF6D8;
	private static final int COLOR_DEEP = 0x14081F;
	private static final int COLOR_BLACK = 0x000000;

	private static final int MOTE_COUNT = 48;
	private static final int BURST_COUNT = 64;
	private static final int TWINKLE_COUNT = 18;
	private static final int ORBIT_COUNT = 10;
	private static final int RING_SEGMENTS = 48;
	private static final float ITEM_SCALE = 4.0f;

	private static final ItemStack SHARD = new ItemStack(Items.PRISMARINE_SHARD);

	private static long startedAtMs = -1L;
	private static Visual visual = new Visual("", "", "", null, null);

	// Per animation: angle, start radius, arrival time, color.
	private static final float[][] motes = new float[MOTE_COUNT][4];
	// Per animation: angle, speed, life, size, color.
	private static final float[][] burst = new float[BURST_COUNT][5];
	// Per animation: x offset, y offset, phase, size.
	private static final float[][] twinkles = new float[TWINKLE_COUNT][4];
	// Per animation: shake direction samples.
	private static final float[] shake = new float[16];

	private FragmentDropOverlay() {
	}

	static void start(Visual newVisual) {
		visual = new Visual(newVisual.label().toUpperCase(Locale.ROOT), newVisual.title(), newVisual.subtitle(),
				newVisual.fragmentTexture(), newVisual.ghostTexture());
		Random random = new Random();
		int[] palette = { COLOR_WHITE, COLOR_GOLD_HI, COLOR_GOLD, COLOR_LILAC, COLOR_AMETHYST };
		for (float[] m : motes) {
			m[0] = (float) (random.nextFloat() * Math.PI * 2.0);
			m[1] = 110f + random.nextFloat() * 170f;
			m[2] = 300f + random.nextFloat() * (IMPACT - 330f);
			m[3] = palette[random.nextInt(palette.length)];
		}
		for (float[] p : burst) {
			p[0] = (float) (random.nextFloat() * Math.PI * 2.0);
			p[1] = 90f + random.nextFloat() * 220f;
			p[2] = 700f + random.nextFloat() * 1100f;
			p[3] = 1f + random.nextInt(3);
			p[4] = palette[random.nextInt(palette.length)];
		}
		for (float[] t : twinkles) {
			double angle = random.nextFloat() * Math.PI * 2.0;
			float radius = 44f + random.nextFloat() * 56f;
			t[0] = (float) Math.cos(angle) * radius * 1.6f;
			t[1] = (float) Math.sin(angle) * radius * 0.8f;
			t[2] = random.nextFloat();
			t[3] = 1f + random.nextInt(3);
		}
		for (int i = 0; i < shake.length; i++) {
			shake[i] = random.nextFloat() * 2f - 1f;
		}
		startedAtMs = Util.getMillis();
	}

	static void render(GuiGraphicsExtractor g, DeltaTracker tickCounter) {
		if (startedAtMs < 0L) {
			return;
		}
		float t = Util.getMillis() - startedAtMs;
		if (t > TOTAL_MS) {
			startedAtMs = -1L;
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return;
		}
		int width = g.guiWidth();
		int height = g.guiHeight();
		float cx = width / 2f;
		float cy = height * 0.34f;
		float outro = progress(t, OUTRO_START, TOTAL_MS - OUTRO_START);
		float fade = 1f - easeInCubic(outro);
		float sinceImpact = t - IMPACT;

		// Unshaken backdrop.
		renderDim(g, t, fade, width, height);
		renderLetterbox(g, t, outro, width, height);

		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		applyShake(pose, sinceImpact);

		if (sinceImpact < 0f) {
			renderCharge(g, t, cx, cy);
		} else {
			renderFlash(g, sinceImpact, width, height);
			renderPillar(g, sinceImpact, fade, cx, height);
			renderRays(g, sinceImpact, fade, cx, cy);
			renderRings(g, sinceImpact, cx, cy);
			renderHalo(g, sinceImpact, fade, cx, cy);
			renderOrbit(g, sinceImpact, fade, cx, cy, false);
			renderBurst(g, sinceImpact, cx, cy);
			renderFragment(g, t, sinceImpact, outro, cx, cy);
			renderOrbit(g, sinceImpact, fade, cx, cy, true);
			renderTwinkles(g, sinceImpact, fade, cx, cy);
			renderTexts(g, client.font, t, fade, cx, cy + ITEM_SCALE * 8f + 14f, width);
		}
		pose.popMatrix();
	}

	// ------------------------------------------------------------------ backdrop

	private static void renderDim(GuiGraphicsExtractor g, float t, float fade, int width, int height) {
		float alpha = 0.38f * easeOutCubic(progress(t, 0f, 450f)) * fade;
		if (alpha > 0.01f) {
			g.fill(0, 0, width, height, argb(alpha, COLOR_DEEP));
		}
	}

	/** Black cinematic bars that slide in at the start and out with the outro. */
	private static void renderLetterbox(GuiGraphicsExtractor g, float t, float outro, int width, int height) {
		float open = easeOutCubic(progress(t, 0f, 420f)) * (1f - easeOutCubic(outro));
		int bar = Math.round(height * 0.085f * open);
		if (bar <= 0) {
			return;
		}
		int color = argb(0.9f, COLOR_BLACK);
		g.fill(0, 0, width, bar, color);
		g.fill(0, height - bar, width, height, color);
		int edge = argb(0.55f * open, COLOR_GOLD);
		g.fill(0, bar, width, bar + 1, edge);
		g.fill(0, height - bar - 1, width, height - bar, edge);
	}

	private static void applyShake(Matrix3x2fStack pose, float sinceImpact) {
		if (sinceImpact < 0f || sinceImpact > 450f) {
			return;
		}
		float amplitude = 5f * (1f - sinceImpact / 450f);
		int i = (int) (sinceImpact / 30f) % (shake.length - 1);
		pose.translate(shake[i] * amplitude, shake[i + 1] * amplitude);
	}

	// ------------------------------------------------------------------ act 1: charge

	private static void renderCharge(GuiGraphicsExtractor g, float t, float cx, float cy) {
		// Motes rush inward along a slight spiral and vanish at the core.
		for (float[] m : motes) {
			float arrival = m[2];
			float p = clamp01(t / arrival);
			if (p >= 1f) {
				continue;
			}
			float eased = easeInCubic(p);
			float radius = m[1] * (1f - eased);
			float angle = m[0] + eased * 1.4f;
			float x = cx + (float) Math.cos(angle) * radius * 1.3f;
			float y = cy + (float) Math.sin(angle) * radius * 0.85f;
			float alpha = clamp01(p * 3f);
			int color = argb(alpha, (int) m[3]);
			// Short trail toward the previous position.
			float trailAngle = m[0] + easeInCubic(Math.max(0f, p - 0.06f)) * 1.4f;
			float trailRadius = m[1] * (1f - easeInCubic(Math.max(0f, p - 0.06f)));
			float tx = cx + (float) Math.cos(trailAngle) * trailRadius * 1.3f;
			float ty = cy + (float) Math.sin(trailAngle) * trailRadius * 0.85f;
			g.fill(Math.round(tx), Math.round(ty), Math.round(tx) + 1, Math.round(ty) + 1, argb(alpha * 0.4f, (int) m[3]));
			g.fill(Math.round(x), Math.round(y), Math.round(x) + 2, Math.round(y) + 2, color);
		}

		// Core: a rotating, growing diamond stack that pulses faster toward the impact.
		float grow = easeInCubic(progress(t, 150f, IMPACT - 150f));
		float pulse = 1f + 0.18f * (float) Math.sin(t * (0.01 + 0.03 * grow));
		float size = (4f + 22f * grow) * pulse;
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		pose.rotate(t * 0.004f + grow * 3f);
		drawSquare(g, size * 1.8f, argb(0.12f * grow + 0.05f, COLOR_AMETHYST));
		drawSquare(g, size * 1.2f, argb(0.25f * grow + 0.1f, COLOR_GOLD));
		drawSquare(g, size * 0.6f, argb(0.6f * grow + 0.3f, COLOR_WHITE));
		pose.popMatrix();

		// Thin, faint rays already start to spin.
		float rayAlpha = 0.25f * grow;
		if (rayAlpha > 0.01f) {
			pose.pushMatrix();
			pose.translate(cx, cy);
			pose.rotate(-t * 0.002f);
			for (int i = 0; i < 8; i++) {
				pose.pushMatrix();
				pose.rotate((float) (i * Math.PI / 4.0));
				drawBar(g, size, size + 40f * grow, 1.5f, argb(rayAlpha, COLOR_GOLD_HI));
				pose.popMatrix();
			}
			pose.popMatrix();
		}
	}

	// ------------------------------------------------------------------ act 2: impact

	private static void renderFlash(GuiGraphicsExtractor g, float s, int width, int height) {
		if (s >= 420f) {
			return;
		}
		float k = 1f - s / 420f;
		int rgb = lerpRgb(COLOR_GOLD_HI, COLOR_WHITE, k);
		g.fill(0, 0, width, height, argb(k * k * 0.75f, rgb));
	}

	/** Vertical beam of light through the fragment, made of soft vertical slices. */
	private static void renderPillar(GuiGraphicsExtractor g, float s, float fade, float cx, int height) {
		float burstOpen = easeOutCubic(progress(s, 0f, 180f));
		float settle = easeOutCubic(progress(s, 180f, 600f));
		float halfWidth = (6f + 34f * burstOpen - 18f * settle)
				* (1f + 0.12f * (float) Math.sin(s * 0.006));
		float alpha = (0.55f - 0.25f * settle) * fade;
		if (alpha <= 0.01f || halfWidth <= 0f) {
			return;
		}
		final int slices = 10;
		for (int i = 0; i < slices; i++) {
			float edge = i / (float) slices;
			float a = alpha * (1f - edge) * (1f - edge);
			int inner = Math.round(halfWidth * edge);
			int outer = Math.round(halfWidth * (i + 1) / slices);
			int color = argb(a, i < 2 ? COLOR_CREAM : COLOR_GOLD);
			g.fill(Math.round(cx) + inner, 0, Math.round(cx) + outer, height, color);
			g.fill(Math.round(cx) - outer, 0, Math.round(cx) - inner, height, color);
		}
	}

	private static void renderRays(GuiGraphicsExtractor g, float s, float fade, float cx, float cy) {
		float grow = easeOutBack(progress(s, 0f, 600f));
		if (grow <= 0f || fade <= 0f) {
			return;
		}
		Matrix3x2fStack pose = g.pose();
		// Long golden rays, clockwise.
		pose.pushMatrix();
		pose.translate(cx, cy);
		pose.rotate(s * 0.0004f);
		for (int i = 0; i < 12; i++) {
			float pulse = 0.8f + 0.2f * (float) Math.sin(s * 0.004 + i * 1.3);
			float length = 120f * grow * pulse;
			pose.pushMatrix();
			pose.rotate((float) (i * Math.PI * 2.0 / 12));
			drawBar(g, 18f, length * 0.55f, 9f, argb(fade * 0.20f, COLOR_GOLD));
			drawBar(g, 18f, length * 0.8f, 5f, argb(fade * 0.16f, COLOR_GOLD_HI));
			drawBar(g, 18f, length, 2f, argb(fade * 0.18f, COLOR_WHITE));
			pose.popMatrix();
		}
		pose.popMatrix();
		// Short amethyst rays, counter-clockwise, in between.
		pose.pushMatrix();
		pose.translate(cx, cy);
		pose.rotate(-s * 0.0007f + (float) (Math.PI / 12.0));
		for (int i = 0; i < 12; i++) {
			float pulse = 0.7f + 0.3f * (float) Math.sin(s * 0.005 + i * 2.1);
			float length = 70f * grow * pulse;
			pose.pushMatrix();
			pose.rotate((float) (i * Math.PI * 2.0 / 12));
			drawBar(g, 16f, length * 0.7f, 6f, argb(fade * 0.22f, COLOR_AMETHYST));
			drawBar(g, 16f, length, 2f, argb(fade * 0.2f, COLOR_LILAC));
			pose.popMatrix();
		}
		pose.popMatrix();
	}

	private static void renderRings(GuiGraphicsExtractor g, float s, float cx, float cy) {
		renderRing(g, progress(s, 0f, 650f), 150f, 4f, COLOR_WHITE, cx, cy);
		renderRing(g, progress(s, 90f, 800f), 210f, 3f, COLOR_GOLD_HI, cx, cy);
		renderRing(g, progress(s, 200f, 950f), 270f, 2f, COLOR_AMETHYST, cx, cy);
	}

	private static void renderRing(GuiGraphicsExtractor g, float p, float maxRadius, float thickness, int rgb,
			float cx, float cy) {
		if (p <= 0f || p >= 1f) {
			return;
		}
		float radius = 10f + maxRadius * easeOutCubic(p);
		float alpha = (1f - p) * 0.8f;
		float segment = (float) (Math.PI * 2.0 * radius / RING_SEGMENTS) + 1.5f;
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		pose.scale(1.25f, 0.9f);
		for (int i = 0; i < RING_SEGMENTS; i++) {
			pose.pushMatrix();
			pose.rotate((float) (i * Math.PI * 2.0 / RING_SEGMENTS));
			pose.translate(radius, 0f);
			pose.rotate((float) (Math.PI / 2.0));
			drawBar(g, -segment / 2f, segment / 2f, thickness * (1f - p * 0.5f), argb(alpha, rgb));
			pose.popMatrix();
		}
		pose.popMatrix();
	}

	/** Soft layered glow behind the fragment. */
	private static void renderHalo(GuiGraphicsExtractor g, float s, float fade, float cx, float cy) {
		float grow = easeOutBack(progress(s, 0f, 500f));
		float breathe = 1f + 0.08f * (float) Math.sin(s * 0.005);
		if (grow <= 0f) {
			return;
		}
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		pose.rotate((float) (Math.PI / 4.0) - s * 0.0006f);
		float[] sizes = { 78f, 62f, 48f, 34f, 22f };
		float[] alphas = { 0.08f, 0.11f, 0.14f, 0.18f, 0.24f };
		int[] colors = { COLOR_AMETHYST, COLOR_AMETHYST, COLOR_GOLD, COLOR_GOLD_HI, COLOR_CREAM };
		for (int i = 0; i < sizes.length; i++) {
			drawSquare(g, sizes[i] * grow * breathe, argb(alphas[i] * fade, colors[i]));
		}
		pose.popMatrix();
	}

	/** Stars on a tilted ellipse; the back half is drawn behind the fragment, the front half over it. */
	private static void renderOrbit(GuiGraphicsExtractor g, float s, float fade, float cx, float cy, boolean front) {
		float appear = easeOutCubic(progress(s, 150f, 500f)) * fade;
		if (appear <= 0.02f) {
			return;
		}
		for (int i = 0; i < ORBIT_COUNT; i++) {
			double angle = s * 0.0022 + i * Math.PI * 2.0 / ORBIT_COUNT;
			float depth = (float) Math.sin(angle);
			if ((depth >= 0f) != front) {
				continue;
			}
			float x = cx + (float) Math.cos(angle) * 58f;
			float y = cy + depth * 14f - (float) Math.cos(angle) * 6f;
			float brightness = appear * (0.55f + 0.45f * (depth + 1f) / 2f);
			int arm = depth > 0.3f ? 2 : 1;
			drawStar(g, x, y, arm, argb(brightness, i % 2 == 0 ? COLOR_GOLD_HI : COLOR_LILAC),
					argb(brightness, COLOR_WHITE));
		}
	}

	private static void renderBurst(GuiGraphicsExtractor g, float s, float cx, float cy) {
		final float drag = 2.4f;
		final float gravity = 60f;
		for (float[] p : burst) {
			float life = p[2];
			if (s >= life) {
				continue;
			}
			float seconds = s / 1000f;
			// Exponential drag: distance approaches speed / drag.
			float distance = p[1] * (1f - (float) Math.exp(-drag * seconds)) / drag;
			float x = cx + (float) Math.cos(p[0]) * distance * 1.3f;
			float y = cy + (float) Math.sin(p[0]) * distance + gravity * seconds * seconds;
			float alpha = 1f - easeInCubic(s / life);
			int size = (int) p[3];
			int color = argb(alpha, (int) p[4]);
			if (size >= 3) {
				drawStar(g, x, y, 2, color, argb(alpha, COLOR_WHITE));
			} else {
				int ix = Math.round(x);
				int iy = Math.round(y);
				g.fill(ix, iy, ix + size, iy + size, color);
			}
		}
	}

	private static void renderFragment(GuiGraphicsExtractor g, float t, float s, float outro, float cx, float cy) {
		float pop = easeOutElastic(progress(s, 0f, 1000f));
		float exit = easeInCubic(outro);
		float scale = ITEM_SCALE * pop * (1f - exit);
		if (scale <= 0.05f) {
			return;
		}
		float bob = (float) Math.sin(t * 0.003) * 3f;
		float wobble = (float) Math.sin(t * 0.0019) * 0.1f;
		// Two fast turns while popping in, then a gentle wobble.
		float spin = (1f - easeOutCubic(progress(s, 0f, 700f))) * (float) (Math.PI * 4.0);
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy + bob - exit * 34f);
		pose.rotate(wobble - spin);
		pose.scale(scale, scale);
		pose.translate(-8f, -8f);
		if (visual.fragmentTexture() != null) {
			if (visual.ghostTexture() != null) {
				// Ghost of the complete charm: the missing piece glows faintly.
				float ghost = (0.22f + 0.12f * (float) Math.sin(t * 0.006)) * (1f - exit);
				g.blit(RenderPipelines.GUI_TEXTURED, visual.ghostTexture(), 0, 0, 0f, 0f, 16, 16, 16, 16,
						argb(ghost, COLOR_WHITE));
			}
			g.blit(RenderPipelines.GUI_TEXTURED, visual.fragmentTexture(), 0, 0, 0f, 0f, 16, 16, 16, 16);
		} else {
			g.item(SHARD, 0, 0);
		}
		pose.popMatrix();
	}

	private static void renderTwinkles(GuiGraphicsExtractor g, float s, float fade, float cx, float cy) {
		float appear = progress(s, 250f, 400f);
		if (appear <= 0f) {
			return;
		}
		for (float[] tw : twinkles) {
			double phase = (s / 1000.0 + tw[2]) * Math.PI;
			float sin = (float) Math.sin(phase);
			float intensity = sin * sin * sin * sin * appear * fade;
			if (intensity < 0.05f) {
				continue;
			}
			int arm = Math.max(1, Math.round(tw[3] * intensity + 0.5f));
			drawStar(g, cx + tw[0], cy + tw[1], arm, argb(intensity * 0.9f, COLOR_GOLD_HI),
					argb(intensity, COLOR_WHITE));
		}
	}

	// ------------------------------------------------------------------ act 3: texts

	private static void renderTexts(GuiGraphicsExtractor g, Font font, float t, float fade, float cx, float top,
			int screenWidth) {
		if (t < TITLE_START - 250f) {
			return;
		}
		float labelBottom = renderLabel(g, font, t, fade, cx, top);
		renderTitle(g, font, t, fade, cx, labelBottom + 5f, screenWidth);
	}

	/** Small spaced-out label like "TALISMANFRAGMENT GEFUNDEN"; returns its bottom y. */
	private static float renderLabel(GuiGraphicsExtractor g, Font font, float t, float fade, float cx, float top) {
		String label = visual.label();
		if (label.isEmpty()) {
			return top;
		}
		float p = easeOutCubic(progress(t, TITLE_START - 250f, 450f));
		float spacing = 1f + 2f * (1f - p);
		float width = 0f;
		for (int i = 0; i < label.length(); i++) {
			width += font.width(String.valueOf(label.charAt(i))) + spacing;
		}
		float x = cx - width / 2f;
		int color = argb(p * fade, COLOR_GOLD_HI);
		for (int i = 0; i < label.length(); i++) {
			String ch = String.valueOf(label.charAt(i));
			g.text(font, ch, Math.round(x), Math.round(top), color, true);
			x += font.width(ch) + spacing;
		}
		drawDiamond(g, cx - width / 2f - 7f, top + 3.5f, 2f, argb(p * fade, COLOR_AMETHYST));
		drawDiamond(g, cx + width / 2f + 6f, top + 3.5f, 2f, argb(p * fade, COLOR_AMETHYST));
		return top + font.lineHeight;
	}

	private static void renderTitle(GuiGraphicsExtractor g, Font font, float t, float fade, float cx, float top,
			int screenWidth) {
		String title = visual.title();
		if (title.isEmpty()) {
			return;
		}
		int letters = title.length();
		Component[] glyphs = new Component[letters];
		float[] advances = new float[letters];
		float rawWidth = 0f;
		for (int i = 0; i < letters; i++) {
			glyphs[i] = Component.literal(String.valueOf(title.charAt(i))).withStyle(ChatFormatting.BOLD);
			advances[i] = font.width(glyphs[i]) + 0.5f;
			rawWidth += advances[i];
		}
		float scale = Math.min(2.0f, (screenWidth - 24f) / Math.max(1f, rawWidth));
		float textWidth = rawWidth * scale;
		float textHeight = font.lineHeight * scale;

		renderBanner(g, t, fade, cx, top, textWidth, textHeight);

		float revealEnd = TITLE_START + (letters - 1) * LETTER_STAGGER + LETTER_DURATION;
		float shimmerT = Math.max(0f, t - revealEnd) % SHIMMER_PERIOD / SHIMMER_PERIOD;
		float band = -4f + shimmerT * (letters + 8f);

		Matrix3x2fStack pose = g.pose();
		float x = cx - textWidth / 2f;
		for (int i = 0; i < letters; i++) {
			float p = letterProgress(t, i, TITLE_START, LETTER_STAGGER, LETTER_DURATION);
			float advance = advances[i] * scale;
			if (p > 0f) {
				float alpha = easeOutCubic(p) * fade;
				float pop = easeOutBack(p);
				float drop = (1f - easeOutCubic(p)) * -16f;
				float highlight = t < revealEnd ? 1f - p : shimmer(i, band, 2.8f);
				int rgb = lerpRgb(COLOR_GOLD, COLOR_WHITE, highlight * 0.85f + 0.15f);
				if (alpha > 0.02f) {
					pose.pushMatrix();
					pose.translate(x + advance / 2f, top + textHeight / 2f + drop);
					pose.scale(scale * pop, scale * pop);
					float glyphWidth = advances[i] - 0.5f;
					g.text(font, glyphs[i], Math.round(-glyphWidth / 2f), -font.lineHeight / 2 + 1,
							argb(alpha, rgb), true);
					pose.popMatrix();
				}
			}
			x += advance;
		}

		renderSubtitle(g, font, t, fade, revealEnd, cx, top + textHeight + 6f, textWidth);
	}

	/** Dark translucent band behind the title that fades out toward both ends. */
	private static void renderBanner(GuiGraphicsExtractor g, float t, float fade, float cx, float top,
			float textWidth, float textHeight) {
		float open = easeOutCubic(progress(t, TITLE_START - 150f, 500f));
		if (open <= 0f) {
			return;
		}
		float halfWidth = (textWidth / 2f + 50f) * open;
		int y0 = Math.round(top - 5f);
		int y1 = Math.round(top + textHeight + 3f);
		final int slices = 24;
		for (int i = 0; i < slices; i++) {
			float edge = i / (float) slices;
			float alpha = 0.6f * (1f - edge * edge) * fade;
			int inner = Math.round(halfWidth * edge);
			int outer = Math.round(halfWidth * (i + 1) / slices);
			int color = argb(alpha, COLOR_DEEP);
			g.fill(Math.round(cx) + inner, y0, Math.round(cx) + outer, y1, color);
			g.fill(Math.round(cx) - outer, y0, Math.round(cx) - inner, y1, color);
		}
		int edgeColor = argb(0.6f * fade * open, COLOR_GOLD);
		int edgeHalf = Math.round(halfWidth * 0.8f);
		g.fill(Math.round(cx) - edgeHalf, y0 - 1, Math.round(cx) + edgeHalf, y0, edgeColor);
		g.fill(Math.round(cx) - edgeHalf, y1, Math.round(cx) + edgeHalf, y1 + 1, edgeColor);
	}

	private static void renderSubtitle(GuiGraphicsExtractor g, Font font, float t, float fade, float revealEnd,
			float cx, float top, float titleWidth) {
		float start = revealEnd - 150f;
		float lines = easeOutCubic(progress(t, start, 550f));
		if (lines <= 0f) {
			return;
		}
		float alpha = easeOutCubic(progress(t, start + 120f, 400f)) * fade;
		float slide = (1f - easeOutCubic(progress(t, start + 120f, 400f))) * 6f;
		Component text = visual.subtitle().isEmpty() ? null : Component.literal(visual.subtitle());
		float scale = 1.25f;
		float textWidth = text == null ? 0f : font.width(text) * scale;

		float gap = text == null ? 4f : textWidth / 2f + 8f;
		float reach = Math.max(18f, titleWidth / 2f - gap + 10f) * lines;
		int lineColor = argb(0.85f * fade, COLOR_AMETHYST);
		int lineY = Math.round(top + font.lineHeight * scale / 2f);
		g.fill(Math.round(cx + gap), lineY, Math.round(cx + gap + reach), lineY + 1, lineColor);
		g.fill(Math.round(cx - gap - reach), lineY, Math.round(cx - gap), lineY + 1, lineColor);
		drawDiamond(g, cx + gap + reach, lineY + 0.5f, 2.5f, argb(fade, COLOR_GOLD_HI));
		drawDiamond(g, cx - gap - reach, lineY + 0.5f, 2.5f, argb(fade, COLOR_GOLD_HI));

		if (text != null && alpha > 0.02f) {
			Matrix3x2fStack pose = g.pose();
			pose.pushMatrix();
			pose.translate(cx - textWidth / 2f, top + slide);
			pose.scale(scale, scale);
			g.text(font, text, 0, 0, argb(alpha, COLOR_LILAC), true);
			pose.popMatrix();
		}
	}

	// ------------------------------------------------------------------ primitives

	/** Bar along +x from {@code from} to {@code to} in the current pose, centered on y = 0. */
	private static void drawBar(GuiGraphicsExtractor g, float from, float to, float thickness, int color) {
		if (to <= from) {
			return;
		}
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(from, -thickness / 2f);
		pose.scale((to - from) / 16f, thickness / 16f);
		g.fill(0, 0, 16, 16, color);
		pose.popMatrix();
	}

	/** Square of the given edge length centered on the current origin. */
	private static void drawSquare(GuiGraphicsExtractor g, float size, int color) {
		if (size <= 0f) {
			return;
		}
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(-size / 2f, -size / 2f);
		pose.scale(size / 16f, size / 16f);
		g.fill(0, 0, 16, 16, color);
		pose.popMatrix();
	}

	/** Four-pointed sparkle: two crossed arms plus a bright core. */
	private static void drawStar(GuiGraphicsExtractor g, float x, float y, int arm, int armColor, int coreColor) {
		int ix = Math.round(x);
		int iy = Math.round(y);
		g.fill(ix - arm, iy, ix + arm + 1, iy + 1, armColor);
		g.fill(ix, iy - arm, ix + 1, iy + arm + 1, armColor);
		g.fill(ix, iy, ix + 1, iy + 1, coreColor);
	}

	private static void drawDiamond(GuiGraphicsExtractor g, float x, float y, float half, int color) {
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.rotate((float) (Math.PI / 4.0));
		drawSquare(g, half * 2f, color);
		pose.popMatrix();
	}
}

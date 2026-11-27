package net.felix.utilities.Overall.starforged;

import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.argb;
import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.clamp01;
import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.easeInCubic;
import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.easeInQuad;
import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.easeOutBack;
import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.easeOutCubic;
import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.easeOutElastic;
import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.hsvToRgb;
import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.letterProgress;
import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.lerpRgb;
import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.progress;
import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.pulse;
import static net.felix.utilities.Overall.starforged.StarforgedAnimationMath.shimmer;

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
 * Screen-space part of the starforged animation, in three acts:
 * <ol>
 * <li>Forge (0..{@link #IMPACT}): the screen turns into deep space with nebulas, a planet and a forming
 * spiral galaxy; stars accelerate into a warp while accelerating hammer strikes throw sparks off a rainbow core.</li>
 * <li>Supernova: white flash, heavy shake, the galaxy explodes outward, rainbow shockwaves, rotating star rays,
 * a lens flare, a spark storm and the item popping in over a nebula halo with orbiting stars.</li>
 * <li>Reveal: a star chain ignites around the item with an eight-pointed star figure inside, shooting stars,
 * the letter-by-letter rainbow title "STERNENGESCHMIEDET" and the item name; then everything fades out.</li>
 * </ol>
 */
final class StarforgedOverlay {

	/** What to show; {@code item} and {@code itemName} may be null (then a nether star and a fallback text are used). */
	record Visual(ItemStack item, Component itemName) {
	}

	// Timeline in milliseconds.
	static final long TOTAL_MS = 8600L;
	static final float IMPACT = 2000f;
	private static final float OUTRO_START = 7700f;
	private static final float LABEL_START = IMPACT + 350f;
	private static final float TITLE_START = IMPACT + 550f;
	private static final float LETTER_STAGGER = 45f;
	private static final float LETTER_DURATION = 380f;
	private static final float SHIMMER_PERIOD = 1600f;

	/** The star chain grows from the top in both directions, one segment per step. */
	static final float CONSTELLATION_START = IMPACT + 700f;
	static final float RING_STEP = 140f;
	static final int RING_POINTS = 12;
	static final int RING_STEPS = RING_POINTS / 2;
	/** When the chain closes at the bottom. */
	static final float RING_DONE = CONSTELLATION_START + (RING_STEPS + 0.25f) * RING_STEP;
	private static final float INNER_STEP = 85f;
	private static final int INNER_POINTS = 8;
	private static final float RING_RADIUS_X = 100f;
	private static final float RING_RADIUS_Y = 44f;

	/** Hammer strikes during the forge act, getting faster toward the impact. */
	static final float[] STRIKES = { 250f, 700f, 1080f, 1380f, 1600f, 1760f, 1880f };

	private static final String LABEL = "SCHMIEDEZUSTAND ERHALTEN";
	private static final String TITLE = "STERNENGESCHMIEDET";
	private static final String FALLBACK_SUBTITLE = "Der seltenste Schmiedezustand";

	private static final int COLOR_WHITE = 0xFFFFFF;
	private static final int COLOR_STARLIGHT = 0xEAF2FF;
	private static final int COLOR_LILAC = 0xD7B8FF;
	private static final int COLOR_BLACK = 0x000000;
	private static final int COLOR_GOLD = 0xFFD477;

	private static final int STAR_COUNT = 150;
	private static final int DEEP_STAR_COUNT = 230;
	private static final int NEBULA_COUNT = 5;
	private static final int MOTE_COUNT = 64;
	private static final int STRIKE_SPARKS = 14;
	private static final int BURST_COUNT = 110;
	private static final int RAY_COUNT = 16;
	private static final int ORBIT_COUNT = 14;
	private static final int METEOR_COUNT = 11;
	private static final int RING_SEGMENTS = 56;
	private static final float ITEM_SCALE = 4.0f;

	private static final String TEXTURE_DIR = "textures/gui/starforged/";
	private static final Identifier TEX_GLOW = texture("glow");
	private static final Identifier TEX_STAR = texture("star");
	private static final Identifier TEX_GALAXY = texture("galaxy");
	private static final Identifier TEX_PLANET = texture("planet");
	private static final Identifier TEX_VIGNETTE = texture("vignette");
	private static final Identifier[] TEX_NEBULA = { texture("nebula_0"), texture("nebula_1"), texture("nebula_2") };
	private static final int SMALL_TEXTURE = 64;
	private static final int LARGE_TEXTURE = 256;
	private static final int[] NEBULA_TINTS = { 0xA43CFF, 0x1FA8FF, 0xFF3D9E, 0x3B4DFF, 0x7A2BD9 };

	// Created lazily so the class (and its timings) can load without a bootstrapped registry.
	private static ItemStack fallbackItem;

	private static long startedAtMs = -1L;
	private static Visual visual = new Visual(null, null);

	// Per animation: angle, start depth, hue, size.
	private static final float[][] stars = new float[STAR_COUNT][4];
	// Per animation: x (0..1), y (0..1), twinkle phase, twinkle speed, kind (0 dot, 1 flare), hue.
	private static final float[][] deepStars = new float[DEEP_STAR_COUNT][6];
	// Per animation: x (0..1), y (0..1), scale, rotation, rotation speed, drift phase.
	private static final float[][] nebulas = new float[NEBULA_COUNT][6];
	// Per animation: angle, start radius, arrival time, hue.
	private static final float[][] motes = new float[MOTE_COUNT][4];
	// Per animation and strike: spark angle, speed.
	private static final float[][][] strikeSparks = new float[STRIKES.length][STRIKE_SPARKS][2];
	// Per animation: angle, speed, life, size, hue.
	private static final float[][] burst = new float[BURST_COUNT][5];
	// Per animation: angle jitter, radius factor of each star in the chain.
	private static final float[][] ring = new float[RING_POINTS][2];
	// Per animation: start time, start x (0..1 of width), length factor, hue.
	private static final float[][] meteors = new float[METEOR_COUNT][4];
	// Per animation: shake direction samples.
	private static final float[] shake = new float[24];

	private StarforgedOverlay() {
	}

	static void stop() {
		startedAtMs = -1L;
	}

	/** Time based, so it also ends when the HUD is hidden and render() is not called. */
	static boolean isRunning() {
		return startedAtMs >= 0L && Util.getMillis() - startedAtMs <= TOTAL_MS;
	}

	static void start(Visual newVisual) {
		visual = newVisual == null ? new Visual(null, null) : newVisual;
		Random random = new Random();
		for (float[] s : stars) {
			s[0] = (float) (random.nextFloat() * Math.PI * 2.0);
			s[1] = random.nextFloat();
			s[2] = random.nextFloat();
			s[3] = random.nextFloat() < 0.15f ? 2f : 1f;
		}
		for (float[] s : deepStars) {
			s[0] = random.nextFloat();
			s[1] = random.nextFloat();
			s[2] = (float) (random.nextFloat() * Math.PI * 2.0);
			s[3] = 0.5f + random.nextFloat() * 2.5f;
			s[4] = random.nextFloat() < 0.07f ? 1f : 0f;
			s[5] = random.nextFloat();
		}
		for (int i = 0; i < NEBULA_COUNT; i++) {
			float[] n = nebulas[i];
			// Spread the clouds around the screen, away from the exact center.
			double angle = i * Math.PI * 2.0 / NEBULA_COUNT + random.nextFloat() * 0.8;
			n[0] = 0.5f + (float) Math.cos(angle) * (0.22f + random.nextFloat() * 0.18f);
			n[1] = 0.45f + (float) Math.sin(angle) * (0.2f + random.nextFloat() * 0.15f);
			n[2] = 0.9f + random.nextFloat() * 0.7f;
			n[3] = (float) (random.nextFloat() * Math.PI * 2.0);
			n[4] = (random.nextFloat() - 0.5f) * 0.00012f;
			n[5] = (float) (random.nextFloat() * Math.PI * 2.0);
		}
		for (float[] m : motes) {
			m[0] = (float) (random.nextFloat() * Math.PI * 2.0);
			m[1] = 120f + random.nextFloat() * 200f;
			m[2] = 400f + random.nextFloat() * (IMPACT - 450f);
			m[3] = random.nextFloat();
		}
		for (float[][] strike : strikeSparks) {
			for (float[] spark : strike) {
				spark[0] = (float) (random.nextFloat() * Math.PI * 2.0);
				spark[1] = 18f + random.nextFloat() * 42f;
			}
		}
		for (float[] p : burst) {
			p[0] = (float) (random.nextFloat() * Math.PI * 2.0);
			p[1] = 80f + random.nextFloat() * 300f;
			p[2] = 900f + random.nextFloat() * 1500f;
			p[3] = 1f + random.nextInt(3);
			p[4] = random.nextFloat();
		}
		for (float[] p : ring) {
			p[0] = (random.nextFloat() - 0.5f) * 0.22f;
			p[1] = 0.9f + random.nextFloat() * 0.2f;
		}
		for (int i = 0; i < METEOR_COUNT; i++) {
			meteors[i][0] = IMPACT + 250f + i * 430f + random.nextFloat() * 260f;
			meteors[i][1] = 0.15f + random.nextFloat() * 1.0f;
			meteors[i][2] = 0.7f + random.nextFloat() * 0.6f;
			meteors[i][3] = random.nextFloat();
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
		float cy = height * 0.36f;
		float outro = progress(t, OUTRO_START, TOTAL_MS - OUTRO_START);
		float fade = 1f - easeInCubic(outro);
		float sinceImpact = t - IMPACT;

		// Unshaken backdrop.
		renderSpace(g, t, fade, width, height);
		renderLetterbox(g, t, outro, width, height);

		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		applyShake(pose, t, sinceImpact);

		renderGalaxy(g, t, fade, cx, cy, height);
		renderStarfield(g, t, fade, cx, cy, width, height);
		if (sinceImpact < 0f) {
			renderMotes(g, t, cx, cy);
			renderStrikes(g, t, cx, cy, width, height);
			renderCore(g, t, cx, cy);
		} else {
			renderMeteors(g, t, fade, width, height);
			renderRays(g, t, sinceImpact, fade, cx, cy);
			renderFlare(g, t, sinceImpact, fade, cx, cy, width);
			renderRings(g, t, sinceImpact, cx, cy);
			renderHalo(g, t, sinceImpact, fade, cx, cy);
			renderConstellation(g, t, fade, cx, cy);
			renderOrbit(g, t, sinceImpact, fade, cx, cy, false);
			renderBurst(g, sinceImpact, cx, cy);
			renderItem(g, t, sinceImpact, outro, cx, cy);
			renderOrbit(g, t, sinceImpact, fade, cx, cy, true);
			renderTexts(g, client.font, t, fade, cx, cy + ITEM_SCALE * 8f + 22f, width);
			renderFlash(g, t, sinceImpact, width, height);
		}
		pose.popMatrix();
	}

	private static Identifier texture(String name) {
		return Identifier.fromNamespaceAndPath("cclive-utilities", TEXTURE_DIR + name + ".png");
	}

	private static float hueAt(float t, float offset) {
		return t * 0.00025f + offset;
	}

	// ------------------------------------------------------------------ backdrop

	/**
	 * Deep space backdrop: dark gradient, drifting colored nebulas, a planet with a moon,
	 * twinkling distant stars and a dark vignette. Nebulas flare up with the impact.
	 */
	private static void renderSpace(GuiGraphicsExtractor g, float t, float fade, int width, int height) {
		float appear = easeOutCubic(progress(t, 0f, 700f)) * fade;
		if (appear <= 0.01f) {
			return;
		}
		g.fillGradient(0, 0, width, height, argb(0.93f * appear, 0x0A0424), argb(0.93f * appear, 0x01030D));

		float warp = easeInQuad(progress(t, 0f, IMPACT));
		float boost = t >= IMPACT ? pulse(t, IMPACT, 1600f) : 0f;
		for (int i = 0; i < NEBULA_COUNT; i++) {
			float[] n = nebulas[i];
			float drift = (float) Math.sin(t * 0.00025 + n[5]) * 12f;
			float size = height * 0.75f * n[2] * (1f + 0.2f * warp + 0.15f * boost);
			float alpha = (0.42f + 0.35f * boost) * appear;
			int rgb = lerpRgb(NEBULA_TINTS[i % NEBULA_TINTS.length], COLOR_WHITE, 0.25f * boost);
			drawSprite(g, TEX_NEBULA[i % TEX_NEBULA.length], LARGE_TEXTURE, width * n[0] + drift,
					height * n[1] + drift * 0.5f, size, n[3] + t * n[4], argb(alpha, rgb));
		}

		// Planet in the lower left with a small moon in the upper right, both rising slowly.
		float rise = t * 0.003f;
		drawSprite(g, TEX_PLANET, LARGE_TEXTURE, width * 0.1f, height * 0.95f - rise, height * 0.62f, 0f,
				argb(appear, COLOR_WHITE));
		drawSprite(g, TEX_PLANET, LARGE_TEXTURE, width * 0.88f, height * 0.2f - rise * 0.5f, height * 0.08f, 0.6f,
				argb(0.9f * appear, 0xFFD9B8));

		for (float[] s : deepStars) {
			float twinkle = 0.35f + 0.65f * Math.abs((float) Math.sin(t * 0.001f * s[3] + s[2]));
			float x = width * s[0];
			float y = height * s[1];
			if (s[4] > 0f) {
				int rgb = lerpRgb(COLOR_STARLIGHT, hsvToRgb(s[5], 0.6f, 1f), 0.5f);
				drawSprite(g, TEX_STAR, SMALL_TEXTURE, x, y, 9f + 7f * twinkle, t * 0.0004f, argb(twinkle * appear, rgb));
			} else {
				int ix = Math.round(x);
				int iy = Math.round(y);
				g.fill(ix, iy, ix + 1, iy + 1, argb(twinkle * 0.8f * appear, COLOR_STARLIGHT));
			}
		}

		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.scale(width / (float) SMALL_TEXTURE, height / (float) SMALL_TEXTURE);
		g.blit(RenderPipelines.GUI_TEXTURED, TEX_VIGNETTE, 0, 0, 0f, 0f, SMALL_TEXTURE, SMALL_TEXTURE,
				SMALL_TEXTURE, SMALL_TEXTURE, argb(0.85f * appear, COLOR_WHITE));
		pose.popMatrix();
	}

	/** Black cinematic bars with a rainbow edge. */
	private static void renderLetterbox(GuiGraphicsExtractor g, float t, float outro, int width, int height) {
		float open = easeOutCubic(progress(t, 0f, 500f)) * (1f - easeOutCubic(outro));
		int bar = Math.round(height * 0.1f * open);
		if (bar <= 0) {
			return;
		}
		int color = argb(0.92f, COLOR_BLACK);
		g.fill(0, 0, width, bar, color);
		g.fill(0, height - bar, width, height, color);
		// Rainbow edge drawn in chunks so the hue runs along the bar.
		int chunks = 24;
		for (int i = 0; i < chunks; i++) {
			int x0 = width * i / chunks;
			int x1 = width * (i + 1) / chunks;
			int rgb = hsvToRgb(hueAt(t, i / (float) chunks), 0.75f, 1f);
			int edge = argb(0.75f * open, rgb);
			g.fill(x0, bar, x1, bar + 1, edge);
			g.fill(x0, height - bar - 1, x1, height - bar, edge);
		}
	}

	private static void applyShake(Matrix3x2fStack pose, float t, float sinceImpact) {
		float amplitude = 0f;
		for (float strike : STRIKES) {
			amplitude = Math.max(amplitude, 2.6f * pulse(t, strike, 160f));
		}
		if (sinceImpact >= 0f && sinceImpact < 900f) {
			amplitude = Math.max(amplitude, 11f * (1f - easeOutCubic(sinceImpact / 900f)));
		}
		if (amplitude <= 0.05f) {
			return;
		}
		int i = (int) (t / 28f) % (shake.length - 1);
		pose.translate(shake[i] * amplitude, shake[i + 1] * amplitude);
	}

	/**
	 * Spiral galaxy around the forge point: it forms and spins up during the charge, explodes outward
	 * with the impact and keeps turning slowly behind the item.
	 */
	private static void renderGalaxy(GuiGraphicsExtractor g, float t, float fade, float cx, float cy, int height) {
		float charge = easeInQuad(progress(t, 200f, IMPACT - 200f));
		float size;
		float alpha;
		float rotation;
		if (t < IMPACT) {
			size = height * (0.25f + 0.45f * charge);
			alpha = 0.1f + 0.45f * charge;
			// Spins faster and faster toward the impact.
			rotation = -(t * 0.0006f + charge * charge * charge * 9f);
		} else {
			float s = t - IMPACT;
			size = height * (0.7f + 0.75f * easeOutBack(progress(s, 0f, 900f)));
			alpha = 0.45f + 0.35f * pulse(s, 0f, 1500f);
			rotation = -(IMPACT * 0.0006f + 9f + s * 0.00035f);
		}
		alpha *= fade * easeOutCubic(progress(t, 0f, 500f));
		if (alpha <= 0.02f) {
			return;
		}
		drawSprite(g, TEX_GLOW, SMALL_TEXTURE, cx, cy, size * 0.9f, 0f, argb(alpha * 0.35f, 0x8C5CFF));
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		// Tilted disk: squash vertically, then turn inside the disk plane.
		pose.scale(1f, 0.55f);
		drawSprite(g, TEX_GALAXY, LARGE_TEXTURE, 0f, 0f, size, rotation, argb(alpha, COLOR_WHITE));
		pose.popMatrix();
	}

	// ------------------------------------------------------------------ starfield

	/** Warp tunnel during the forge act; slow drifting stars afterwards. */
	private static void renderStarfield(GuiGraphicsExtractor g, float t, float fade, float cx, float cy,
			int width, int height) {
		float maxRadius = (float) Math.hypot(width, height) * 0.6f;
		float warp = easeInQuad(progress(t, 0f, IMPACT));
		float seconds = Math.min(t, IMPACT) / 1000f;
		float phase = 0.04f * seconds + 0.55f * seconds * seconds * seconds;
		if (t > IMPACT) {
			phase += 0.025f * (t - IMPACT) / 1000f;
			warp = 1f - easeOutCubic(progress(t, IMPACT, 500f));
		}
		float appear = easeOutCubic(progress(t, 0f, 500f)) * fade;
		Matrix3x2fStack pose = g.pose();
		for (float[] s : stars) {
			float depth = s[1] + phase;
			depth -= (float) Math.floor(depth);
			float radius = maxRadius * depth * depth;
			float alpha = clamp01(depth * 4f) * appear * (t > IMPACT ? 0.7f : 0.95f);
			if (alpha < 0.03f || radius < 2f) {
				continue;
			}
			int rgb = lerpRgb(COLOR_STARLIGHT, hsvToRgb(s[2], 0.7f, 1f), 0.35f + 0.5f * warp);
			float length = 1f + radius * (0.02f + 0.3f * warp);
			pose.pushMatrix();
			pose.translate(cx, cy);
			pose.rotate(s[0]);
			drawBar(g, radius, radius + length, s[3] * (0.8f + warp * 0.6f), argb(alpha, rgb));
			pose.popMatrix();
		}
	}

	// ------------------------------------------------------------------ act 1: forge

	/** Energy motes spiraling into the core. */
	private static void renderMotes(GuiGraphicsExtractor g, float t, float cx, float cy) {
		for (float[] m : motes) {
			float p = progress(t, m[2] - 650f, 650f);
			if (p <= 0f || p >= 1f) {
				continue;
			}
			float travel = easeInCubic(p);
			float radius = m[1] * (1f - travel);
			double angle = m[0] + travel * 2.4;
			float x = cx + (float) Math.cos(angle) * radius;
			float y = cy + (float) Math.sin(angle) * radius * 0.75f;
			int rgb = hsvToRgb(m[3] + t * 0.0004f, 0.65f, 1f);
			int arm = p > 0.6f ? 2 : 1;
			drawStar(g, x, y, arm, argb(0.85f * p, rgb), argb(p, COLOR_WHITE));
		}
	}

	/** Spark bursts and shock rings of the hammer strikes. */
	private static void renderStrikes(GuiGraphicsExtractor g, float t, float cx, float cy, int width, int height) {
		Matrix3x2fStack pose = g.pose();
		for (int k = 0; k < STRIKES.length; k++) {
			float since = t - STRIKES[k];
			if (since < 0f || since > 520f) {
				continue;
			}
			float p = since / 520f;
			float strength = 0.6f + 0.4f * k / (STRIKES.length - 1f);
			float flash = pulse(t, STRIKES[k], 140f);
			if (flash > 0.02f) {
				g.fill(0, 0, width, height, argb(0.16f * flash * strength, COLOR_WHITE));
			}
			renderRing(g, easeOutCubic(p), 40f + 30f * strength, 2.5f, hsvToRgb(hueAt(t, k * 0.14f), 0.6f, 1f),
					0.8f * (1f - p), cx, cy);
			for (float[] spark : strikeSparks[k]) {
				float dist = 8f + spark[1] * strength * easeOutCubic(p) * 1.6f;
				float length = 2f + 9f * (1f - p);
				int rgb = lerpRgb(COLOR_GOLD, COLOR_WHITE, 1f - p);
				pose.pushMatrix();
				pose.translate(cx, cy + 30f * p * p);
				pose.rotate(spark[0]);
				drawBar(g, dist, dist + length, 1.2f, argb(1f - p, rgb));
				pose.popMatrix();
			}
		}
	}

	/** Rainbow core that grows with the charge and jumps on every strike. */
	private static void renderCore(GuiGraphicsExtractor g, float t, float cx, float cy) {
		float charge = easeInQuad(progress(t, 150f, IMPACT - 150f));
		float kick = 0f;
		for (float strike : STRIKES) {
			kick = Math.max(kick, pulse(t, strike, 220f));
		}
		float base = 3f + 16f * charge + 7f * kick;
		float wobble = 1f + 0.08f * (float) Math.sin(t * 0.03);
		float appear = clamp01(t / 300f);
		drawSprite(g, TEX_GLOW, SMALL_TEXTURE, cx, cy, base * 7f, 0f,
				argb(0.5f * appear, hsvToRgb(hueAt(t, 0f), 0.5f, 1f)));
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		for (int layer = 4; layer >= 0; layer--) {
			float size = base * wobble * (1f + layer * 0.55f);
			float alpha = (layer == 0 ? 0.95f : 0.22f / layer) * appear;
			int rgb = layer == 0 ? COLOR_WHITE : hsvToRgb(hueAt(t, layer * 0.18f), 0.7f, 1f);
			pose.pushMatrix();
			pose.rotate((float) (Math.PI / 4.0) + t * 0.002f * (layer % 2 == 0 ? 1f : -1f));
			drawSquare(g, size, argb(alpha, rgb));
			pose.popMatrix();
		}
		// Cross flare that stretches with the charge.
		float arm = base * (2.5f + 4f * charge);
		int flare = argb(0.65f * appear, COLOR_WHITE);
		drawBar(g, -arm, arm, 1.5f, flare);
		pose.rotate((float) (Math.PI / 2.0));
		drawBar(g, -arm * 0.7f, arm * 0.7f, 1.5f, flare);
		pose.popMatrix();
	}

	// ------------------------------------------------------------------ act 2: supernova

	/** White flash at the impact, then a rainbow afterglow. Drawn last so it covers everything. */
	private static void renderFlash(GuiGraphicsExtractor g, float t, float s, int width, int height) {
		float white = 1f - easeOutCubic(progress(s, 0f, 520f));
		if (white > 0.01f) {
			g.fill(0, 0, width, height, argb(white, COLOR_WHITE));
		}
		float tint = 0.2f * pulse(s, 150f, 1300f);
		if (tint > 0.01f) {
			g.fill(0, 0, width, height, argb(tint, hsvToRgb(hueAt(t, 0f), 0.8f, 1f)));
		}
	}

	/** Rotating rainbow star rays, alternating long and short. */
	private static void renderRays(GuiGraphicsExtractor g, float t, float s, float fade, float cx, float cy) {
		float grow = easeOutCubic(progress(s, 0f, 650f));
		float strength = (0.28f + 0.72f * pulse(s, 0f, 2200f)) * fade;
		if (strength < 0.02f) {
			return;
		}
		Matrix3x2fStack pose = g.pose();
		for (int i = 0; i < RAY_COUNT; i++) {
			float length = (i % 2 == 0 ? 300f : 150f) * grow * (0.9f + 0.1f * (float) Math.sin(t * 0.005 + i));
			float angle = (float) (i * Math.PI * 2.0 / RAY_COUNT) + s * 0.00045f;
			int rgb = hsvToRgb(hueAt(t, i / (float) RAY_COUNT), 0.65f, 1f);
			pose.pushMatrix();
			pose.translate(cx, cy);
			pose.rotate(angle);
			drawBar(g, 6f, length, i % 2 == 0 ? 9f : 6f, argb(0.12f * strength, rgb));
			drawBar(g, 6f, length * 0.8f, i % 2 == 0 ? 3.5f : 2.5f, argb(0.3f * strength, rgb));
			drawBar(g, 6f, length * 0.55f, 1f, argb(0.6f * strength, COLOR_WHITE));
			pose.popMatrix();
		}
	}

	/** Anamorphic lens flare: a long horizontal streak and a shorter vertical one. */
	private static void renderFlare(GuiGraphicsExtractor g, float t, float s, float fade, float cx, float cy,
			int width) {
		float strength = (0.25f + 0.75f * pulse(s, 0f, 1800f)) * fade;
		float span = width * 0.55f * easeOutCubic(progress(s, 0f, 400f));
		if (strength < 0.02f || span < 1f) {
			return;
		}
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		int tint = hsvToRgb(hueAt(t, 0.6f), 0.5f, 1f);
		drawBar(g, -span, span, 5f, argb(0.18f * strength, tint));
		drawBar(g, -span * 0.8f, span * 0.8f, 1.5f, argb(0.7f * strength, COLOR_WHITE));
		pose.rotate((float) (Math.PI / 2.0));
		drawBar(g, -span * 0.3f, span * 0.3f, 1.5f, argb(0.55f * strength, COLOR_WHITE));
		pose.popMatrix();
	}

	private static void renderRings(GuiGraphicsExtractor g, float t, float s, float cx, float cy) {
		float[] starts = { 0f, 110f, 240f, 400f, 600f };
		float[] radii = { 240f, 180f, 130f, 300f, 90f };
		for (int i = 0; i < starts.length; i++) {
			float p = progress(s, starts[i], 1200f);
			if (p <= 0f || p >= 1f) {
				continue;
			}
			int rgb = hsvToRgb(hueAt(t, i * 0.2f), 0.7f, 1f);
			renderRing(g, easeOutCubic(p), radii[i], 4.5f * (1f - p) + 0.8f, rgb, 0.9f * (1f - p), cx, cy);
		}
	}

	/** Ring of short tangential bars; flattened vertically for a slight perspective. */
	private static void renderRing(GuiGraphicsExtractor g, float p, float maxRadius, float thickness, int rgb,
			float alpha, float cx, float cy) {
		float radius = maxRadius * p;
		if (radius < 1f || alpha < 0.02f) {
			return;
		}
		int color = argb(alpha, rgb);
		float segment = (float) (Math.PI * 2.0 * radius / RING_SEGMENTS) + 1f;
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		pose.scale(1f, 0.72f);
		for (int i = 0; i < RING_SEGMENTS; i++) {
			pose.pushMatrix();
			pose.rotate((float) (i * Math.PI * 2.0 / RING_SEGMENTS));
			pose.translate(radius, 0f);
			pose.rotate((float) (Math.PI / 2.0));
			drawBar(g, -segment / 2f, segment / 2f, thickness, color);
			pose.popMatrix();
		}
		pose.popMatrix();
	}

	/** Soft rainbow glow and slowly turning layers behind the item. */
	private static void renderHalo(GuiGraphicsExtractor g, float t, float s, float fade, float cx, float cy) {
		float open = easeOutBack(progress(s, 80f, 700f)) * fade;
		if (open <= 0.02f) {
			return;
		}
		float breathe = 1f + 0.06f * (float) Math.sin(t * 0.004);
		drawSprite(g, TEX_GLOW, SMALL_TEXTURE, cx, cy, 150f * open * breathe, 0f,
				argb(0.45f * clamp01(open), hsvToRgb(hueAt(t, 0f), 0.55f, 1f)));
		drawSprite(g, TEX_GLOW, SMALL_TEXTURE, cx, cy, 70f * open * breathe, 0f, argb(0.5f * clamp01(open), COLOR_WHITE));
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		for (int layer = 4; layer >= 1; layer--) {
			float size = (26f + layer * 15f) * open * breathe;
			int rgb = hsvToRgb(hueAt(t, layer * 0.16f), 0.6f, 1f);
			pose.pushMatrix();
			pose.rotate((float) (Math.PI / 4.0) + t * 0.0006f * (layer % 2 == 0 ? 1f : -1f));
			drawSquare(g, size, argb(0.07f * clamp01(open), rgb));
			pose.popMatrix();
		}
		pose.popMatrix();
	}

	/** Stars on a tilted ellipse; the back half is drawn behind the item, the front half in front. */
	private static void renderOrbit(GuiGraphicsExtractor g, float t, float s, float fade, float cx, float cy,
			boolean front) {
		float open = easeOutCubic(progress(s, 200f, 800f)) * fade;
		if (open <= 0.02f) {
			return;
		}
		for (int i = 0; i < ORBIT_COUNT; i++) {
			double angle = i * Math.PI * 2.0 / ORBIT_COUNT + t * 0.0016;
			double depth = Math.sin(angle);
			if ((depth > 0) != front) {
				continue;
			}
			float x = cx + (float) Math.cos(angle) * 58f * open;
			float y = cy + (float) depth * 16f * open - (float) Math.cos(angle) * 8f * open;
			int rgb = hsvToRgb(hueAt(t, i / (float) ORBIT_COUNT), 0.55f, 1f);
			float alpha = (front ? 1f : 0.55f) * open;
			drawSprite(g, TEX_STAR, SMALL_TEXTURE, x, y, front ? 11f : 7f, 0f, argb(alpha, rgb));
		}
	}

	/** Spark storm thrown out by the impact, falling slightly. */
	private static void renderBurst(GuiGraphicsExtractor g, float s, float cx, float cy) {
		Matrix3x2fStack pose = g.pose();
		for (float[] p : burst) {
			float life = s / p[2];
			if (life >= 1f) {
				continue;
			}
			float dist = p[1] * easeOutCubic(life);
			float fall = life * life * 45f;
			float alpha = 1f - easeInQuad(life);
			int rgb = lerpRgb(hsvToRgb(p[4], 0.7f, 1f), COLOR_WHITE, 1f - life);
			float length = p[3] + 10f * (1f - life);
			pose.pushMatrix();
			pose.translate(cx, cy + fall);
			pose.rotate(p[0]);
			drawBar(g, dist, dist + length, p[3] * 0.8f, argb(alpha, rgb));
			pose.popMatrix();
		}
	}

	private static void renderItem(GuiGraphicsExtractor g, float t, float s, float outro, float cx, float cy) {
		float pop = easeOutElastic(progress(s, 60f, 1100f));
		float exit = easeInCubic(outro);
		float scale = ITEM_SCALE * pop * (1f + exit * 0.6f);
		float alpha = 1f - exit;
		if (scale <= 0.05f || alpha <= 0.02f) {
			return;
		}
		float bob = (float) Math.sin(t * 0.003) * 3f;
		float wobble = (float) Math.sin(t * 0.0019) * 0.08f;
		// Three fast turns while popping in, then a gentle wobble.
		float spin = (1f - easeOutCubic(progress(s, 60f, 800f))) * (float) (Math.PI * 6.0);
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy + bob);
		pose.rotate(wobble - spin);
		pose.scale(scale, scale);
		pose.translate(-8f, -8f);
		g.item(visual.item() != null && !visual.item().isEmpty() ? visual.item() : fallbackItem(), 0, 0);
		pose.popMatrix();
	}

	private static ItemStack fallbackItem() {
		if (fallbackItem == null) {
			fallbackItem = new ItemStack(Items.NETHER_STAR);
		}
		return fallbackItem;
	}

	// ------------------------------------------------------------------ act 3: reveal

	/**
	 * The star chain: a ring of twelve stars around the item that ignites from the top in both directions
	 * with glowing, comet-tipped lines; when it closes, an eight-pointed star figure is drawn inside,
	 * the stars flare up and a light pulse keeps running around the chain.
	 */
	private static void renderConstellation(GuiGraphicsExtractor g, float t, float fade, float cx, float cy) {
		if (t < CONSTELLATION_START - 80f || fade <= 0.02f) {
			return;
		}
		float spin = (t - CONSTELLATION_START) * 0.00012f;
		float[][] points = new float[RING_POINTS][2];
		for (int i = 0; i < RING_POINTS; i++) {
			double angle = -Math.PI / 2.0 + i * Math.PI * 2.0 / RING_POINTS + ring[i][0] + spin;
			points[i][0] = cx + (float) Math.cos(angle) * RING_RADIUS_X * ring[i][1];
			points[i][1] = cy + (float) Math.sin(angle) * RING_RADIUS_Y * ring[i][1];
		}
		float closed = pulse(t, RING_DONE, 900f);
		float runner = t > RING_DONE ? ((t - RING_DONE) * 0.00045f) % 1f : -1f;

		// Inner eight-pointed star {8/3}, turning the other way.
		float innerStart = RING_DONE - 100f;
		for (int i = 0; i < INNER_POINTS; i++) {
			float p = easeOutCubic(progress(t, innerStart + i * INNER_STEP, INNER_STEP * 1.6f));
			if (p <= 0f) {
				continue;
			}
			float[] a = innerPoint(i, cx, cy, -spin * 1.6f);
			float[] b = innerPoint((i + 3) % INNER_POINTS, cx, cy, -spin * 1.6f);
			int rgb = lerpRgb(COLOR_GOLD, COLOR_LILAC, 0.5f + 0.5f * (float) Math.sin(t * 0.003 + i));
			drawGlowLine(g, a[0], a[1], b[0], b[1], p, rgb, 0.45f * fade, 0.7f);
		}

		// Ring segments: step k draws k -> k+1 and -k -> -k-1.
		for (int k = 0; k < RING_STEPS; k++) {
			float p = easeOutCubic(progress(t, CONSTELLATION_START + k * RING_STEP, RING_STEP * 1.25f));
			if (p <= 0f) {
				continue;
			}
			int[][] segments = { { k, k + 1 }, { (RING_POINTS - k) % RING_POINTS, RING_POINTS - k - 1 } };
			for (int[] seg : segments) {
				int from = seg[0];
				int to = seg[1] % RING_POINTS;
				float[] a = points[from];
				float[] b = points[to];
				int rgb = hsvToRgb(hueAt(t, from / (float) RING_POINTS), 0.5f, 1f);
				float strength = 0.85f + 0.6f * closed;
				if (runner >= 0f) {
					// Light pulse running around the chain.
					float middle = Math.abs(from - to) > 1 ? RING_POINTS - 0.5f : (from + to) / 2f;
					float distance = Math.abs(middle / RING_POINTS - runner);
					distance = Math.min(distance, 1f - distance);
					strength += 0.9f * clamp01(1f - distance * 9f);
				}
				drawGlowLine(g, a[0], a[1], b[0], b[1], p, rgb, Math.min(1f, strength) * fade, 1f + 0.4f * closed);
			}
		}

		// Stars, ignited when the chain reaches them.
		for (int i = 0; i < RING_POINTS; i++) {
			int steps = Math.min(i, RING_POINTS - i);
			float ignite = CONSTELLATION_START + (steps == 0 ? 0f : (steps - 0.1f) * RING_STEP);
			float appear = easeOutBack(progress(t, ignite - 60f, 300f));
			if (appear <= 0.02f) {
				continue;
			}
			boolean major = i % 3 == 0;
			float flash = pulse(t, ignite, 450f) + closed;
			float twinkle = 0.85f + 0.15f * (float) Math.sin(t * 0.009 + i * 1.9);
			float size = (major ? 24f : 14f) * appear * twinkle * (1f + 0.9f * flash);
			int rgb = lerpRgb(hsvToRgb(hueAt(t, i / (float) RING_POINTS), 0.45f, 1f), COLOR_WHITE, 0.35f);
			float alpha = clamp01(appear) * fade;
			drawSprite(g, TEX_GLOW, SMALL_TEXTURE, points[i][0], points[i][1], size * 1.4f, 0f,
					argb(0.4f * alpha, rgb));
			drawSprite(g, TEX_STAR, SMALL_TEXTURE, points[i][0], points[i][1], size,
					t * 0.0008f * (i % 2 == 0 ? 1f : -1f), argb(alpha, rgb));
		}
	}

	private static float[] innerPoint(int i, float cx, float cy, float spin) {
		double angle = -Math.PI / 2.0 + i * Math.PI * 2.0 / INNER_POINTS + spin;
		return new float[] { cx + (float) Math.cos(angle) * RING_RADIUS_X * 0.74f,
				cy + (float) Math.sin(angle) * RING_RADIUS_Y * 0.74f };
	}

	/**
	 * Glowing line from (x0, y0) toward (x1, y1), drawn up to {@code progress}; while it is still growing,
	 * a bright comet head sits on its tip.
	 */
	private static void drawGlowLine(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, float progress,
			int rgb, float alpha, float width) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float length = (float) Math.hypot(dx, dy) * progress;
		if (length < 0.5f || alpha <= 0.02f) {
			return;
		}
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(x0, y0);
		pose.rotate((float) Math.atan2(dy, dx));
		drawBar(g, 0f, length, 6f * width, argb(0.1f * alpha, rgb));
		drawBar(g, 0f, length, 2.5f * width, argb(0.3f * alpha, rgb));
		drawBar(g, 0f, length, 1f, argb(0.95f * alpha, lerpRgb(rgb, COLOR_WHITE, 0.6f)));
		pose.popMatrix();
		if (progress < 1f) {
			float hx = x0 + dx * progress;
			float hy = y0 + dy * progress;
			drawSprite(g, TEX_GLOW, SMALL_TEXTURE, hx, hy, 18f, 0f, argb(0.7f * alpha, rgb));
			drawSprite(g, TEX_STAR, SMALL_TEXTURE, hx, hy, 12f, progress * 3f, argb(alpha, COLOR_WHITE));
		}
	}

	/** Shooting stars crossing the screen from the top right to the bottom left. */
	private static void renderMeteors(GuiGraphicsExtractor g, float t, float fade, int width, int height) {
		Matrix3x2fStack pose = g.pose();
		float angle = (float) Math.atan2(0.55, -1.0);
		for (float[] m : meteors) {
			float p = progress(t, m[0], 750f);
			if (p <= 0f || p >= 1f) {
				continue;
			}
			float travel = width * 0.9f * m[2];
			float x = width * m[1] + (float) Math.cos(angle) * travel * easeInQuad(p);
			float y = -10f + (float) Math.sin(angle) * travel * easeInQuad(p);
			float alpha = (p < 0.15f ? p / 0.15f : 1f - easeInQuad((p - 0.15f) / 0.85f)) * fade;
			int rgb = hsvToRgb(m[3], 0.5f, 1f);
			pose.pushMatrix();
			pose.translate(x, y);
			pose.rotate(angle);
			// The trail points back along the flight direction.
			drawBar(g, -48f * m[2], 0f, 2.5f, argb(0.25f * alpha, rgb));
			drawBar(g, -30f * m[2], 0f, 1f, argb(0.8f * alpha, COLOR_WHITE));
			pose.popMatrix();
			drawSprite(g, TEX_GLOW, SMALL_TEXTURE, x, y, 14f, 0f, argb(0.6f * alpha, rgb));
			drawSprite(g, TEX_STAR, SMALL_TEXTURE, x, y, 10f, 0f, argb(alpha, COLOR_WHITE));
		}
	}

	private static void renderTexts(GuiGraphicsExtractor g, Font font, float t, float fade, float cx, float top,
			int screenWidth) {
		float labelBottom = renderLabel(g, font, t, fade, cx, top);
		renderTitle(g, font, t, fade, cx, labelBottom + 5f, screenWidth);
	}

	/** Small spaced label that fades in; returns its bottom edge. */
	private static float renderLabel(GuiGraphicsExtractor g, Font font, float t, float fade, float cx, float top) {
		float p = easeOutCubic(progress(t, LABEL_START, 400f));
		float alpha = p * fade * 0.9f;
		if (alpha > 0.02f) {
			int textWidth = font.width(LABEL);
			float drift = (1f - p) * 6f;
			g.text(font, LABEL, Math.round(cx - textWidth / 2f), Math.round(top + drift),
					argb(alpha, COLOR_LILAC), true);
			int lineWidth = Math.round((textWidth / 2f + 10f) * p);
			int y = Math.round(top + font.lineHeight / 2f);
			int line = argb(alpha * 0.6f, COLOR_LILAC);
			g.fill(Math.round(cx - textWidth / 2f - 6f - lineWidth), y, Math.round(cx - textWidth / 2f - 6f), y + 1,
					line);
			g.fill(Math.round(cx + textWidth / 2f + 6f), y, Math.round(cx + textWidth / 2f + 6f + lineWidth), y + 1,
					line);
		}
		return top + font.lineHeight;
	}

	/** "STERNENGESCHMIEDET" letter by letter: each drops in, then a rainbow wave and a white shimmer run over it. */
	private static void renderTitle(GuiGraphicsExtractor g, Font font, float t, float fade, float cx, float top,
			int screenWidth) {
		int letters = TITLE.length();
		Component[] glyphs = new Component[letters];
		float[] advances = new float[letters];
		float rawWidth = 0f;
		for (int i = 0; i < letters; i++) {
			glyphs[i] = Component.literal(String.valueOf(TITLE.charAt(i))).withStyle(ChatFormatting.BOLD);
			advances[i] = font.width(glyphs[i]) + 1f;
			rawWidth += advances[i];
		}
		float scale = Math.min(2.6f, (screenWidth - 24f) / Math.max(1f, rawWidth));
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
				float pop = easeOutBack(p) * (1f + 0.6f * (1f - p));
				float drop = (1f - easeOutCubic(p)) * -22f;
				float wave = (float) Math.sin(t * 0.006 - i * 0.55) * 1.6f * easeOutCubic(p);
				float highlight = t < revealEnd ? 1f - p : shimmer(i, band, 2.6f);
				int rainbow = hsvToRgb(hueAt(t, -i * 0.045f) * 2f, 0.6f, 1f);
				int rgb = lerpRgb(rainbow, COLOR_WHITE, highlight * 0.85f);
				if (alpha > 0.02f) {
					pose.pushMatrix();
					pose.translate(x + advance / 2f, top + textHeight / 2f + drop + wave);
					pose.scale(scale * pop, scale * pop);
					float glyphWidth = advances[i] - 1f;
					g.text(font, glyphs[i], Math.round(-glyphWidth / 2f), -font.lineHeight / 2 + 1,
							argb(alpha, rgb), true);
					pose.popMatrix();
				}
			}
			x += advance;
		}

		renderSubtitle(g, font, t, fade, revealEnd, cx, top + textHeight + 7f);
	}

	/** Dark translucent band behind the title with rainbow hairlines. */
	private static void renderBanner(GuiGraphicsExtractor g, float t, float fade, float cx, float top,
			float textWidth, float textHeight) {
		float open = easeOutCubic(progress(t, TITLE_START - 150f, 500f));
		if (open <= 0f) {
			return;
		}
		float halfWidth = (textWidth / 2f + 40f) * open;
		int y0 = Math.round(top - 5f);
		int y1 = Math.round(top + textHeight + 20f);
		int steps = 12;
		for (int i = 0; i < steps; i++) {
			float edge = i / (float) steps;
			int x0 = Math.round(cx - halfWidth * (1f - edge));
			int x1 = Math.round(cx + halfWidth * (1f - edge));
			g.fill(x0, y0, x1, y1, argb(0.06f * fade, COLOR_BLACK));
		}
		int x0 = Math.round(cx - halfWidth);
		int x1 = Math.round(cx + halfWidth);
		int chunks = 16;
		for (int i = 0; i < chunks; i++) {
			int a = x0 + (x1 - x0) * i / chunks;
			int b = x0 + (x1 - x0) * (i + 1) / chunks;
			float edge = 1f - Math.abs(i - (chunks - 1) / 2f) / (chunks / 2f);
			int line = argb(0.8f * edge * fade, hsvToRgb(hueAt(t, i / (float) chunks), 0.7f, 1f));
			g.fill(a, y0 - 1, b, y0, line);
			g.fill(a, y1, b, y1 + 1, line);
		}
	}

	/** Item name (server colors kept) or a fallback text, fading in after the title. */
	private static void renderSubtitle(GuiGraphicsExtractor g, Font font, float t, float fade, float revealEnd,
			float cx, float top) {
		float p = easeOutCubic(progress(t, revealEnd - 100f, 450f));
		float alpha = p * fade;
		if (alpha <= 0.02f) {
			return;
		}
		Component text = visual.itemName() != null && !visual.itemName().getString().isBlank()
				? visual.itemName()
				: Component.literal(FALLBACK_SUBTITLE).withStyle(ChatFormatting.ITALIC);
		int color = visual.itemName() != null ? argb(alpha, COLOR_WHITE) : argb(alpha, COLOR_STARLIGHT);
		int textWidth = font.width(text);
		g.text(font, text, Math.round(cx - textWidth / 2f), Math.round(top + (1f - p) * 5f), color, true);
	}

	// ------------------------------------------------------------------ primitives

	/** Bar along +x from {@code from} to {@code to} in the current pose, centered on y = 0. */
	private static void drawBar(GuiGraphicsExtractor g, float from, float to, float thickness, int color) {
		if (to <= from || thickness <= 0f) {
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

	/** Draws a square texture centered on (x, y) with the given edge length, rotation and ARGB tint. */
	private static void drawSprite(GuiGraphicsExtractor g, Identifier texture, int textureSize, float x, float y,
			float size, float rotation, int color) {
		if (size <= 0.5f || (color >>> 24) == 0) {
			return;
		}
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		if (rotation != 0f) {
			pose.rotate(rotation);
		}
		pose.scale(size / textureSize, size / textureSize);
		pose.translate(-textureSize / 2f, -textureSize / 2f);
		g.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0f, 0f, textureSize, textureSize, textureSize, textureSize,
				color);
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
}

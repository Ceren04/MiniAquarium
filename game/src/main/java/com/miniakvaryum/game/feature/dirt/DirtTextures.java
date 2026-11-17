package com.miniakvaryum.game.feature.dirt;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.miniakvaryum.game.core.ui.ShapeTextures;

/**
 * The dirt spot images. Each variant is a soft, irregular blotch made of a few overlapping
 * blurred circles, drawn in code. A file {@code assets/dirt/spot<n>.png} replaces variant n.
 *
 * <p>The shapes are fixed (no randomness in the UI): randomness comes from the domain, which
 * chooses the variant, size and rotation of every spot.
 *
 * <p>The images are created once at start-up and used until the game ends, like the screens.
 */
final class DirtTextures {

    private static final int SIZE = 128;
    private static final Color DIRT = new Color(0.32f, 0.36f, 0.16f, 1f);
    /** How many rings make one soft circle; more rings give a smoother edge. */
    private static final int RINGS = 20;
    private static final float RING_ALPHA = 0.055f;

    /**
     * Circles per variant as {centerX, centerY, radius}, in parts of the image size.
     * The domain's {@code DirtConfig.variants} must not exceed the number of variants here.
     */
    private static final float[][][] SHAPES = {
        {{0.50f, 0.50f, 0.34f}, {0.30f, 0.38f, 0.16f}, {0.68f, 0.66f, 0.15f}, {0.62f, 0.28f, 0.10f}},
        {{0.45f, 0.55f, 0.28f}, {0.66f, 0.45f, 0.24f}, {0.30f, 0.30f, 0.12f}, {0.78f, 0.74f, 0.08f}},
        {{0.52f, 0.48f, 0.22f}, {0.35f, 0.62f, 0.18f}, {0.66f, 0.30f, 0.17f}, {0.25f, 0.25f, 0.07f},
         {0.80f, 0.60f, 0.09f}},
    };

    private final TextureRegion[] regions = new TextureRegion[SHAPES.length];

    DirtTextures(ShapeTextures shapes) {
        for (int i = 0; i < SHAPES.length; i++) {
            int variant = i;
            Texture texture = shapes.fileOr("dirt/spot" + i + ".png", () -> blotch(SHAPES[variant]));
            regions[i] = new TextureRegion(texture);
        }
    }

    /** Returns the image of a variant; unknown variants fall back to variant 0. */
    TextureRegion region(int variant) {
        return variant >= 0 && variant < regions.length ? regions[variant] : regions[0];
    }

    private Texture blotch(float[][] circles) {
        Pixmap pixmap = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        pixmap.setColor(DIRT.r, DIRT.g, DIRT.b, RING_ALPHA);
        for (float[] c : circles) {
            int cx = Math.round(c[0] * SIZE);
            int cy = Math.round(c[1] * SIZE);
            float radius = c[2] * SIZE;
            // Smaller rings on top of bigger ones: dark in the middle, fading towards the edge.
            for (int ring = 0; ring < RINGS; ring++) {
                int r = Math.round(radius * (1f - ring / (float) RINGS));
                if (r > 0) {
                    pixmap.fillCircle(cx, cy, r);
                }
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }
}

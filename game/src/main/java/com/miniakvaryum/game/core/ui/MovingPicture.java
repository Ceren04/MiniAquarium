package com.miniakvaryum.game.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;

/**
 * A picture that moves gently, made only with code from one image: a plant bends in the water,
 * a shell floats, a hat tilts, a fish swims on the spot. Optionally it switches between two
 * pictures (a chest opens and closes) or lets bubbles rise (a castle).
 *
 * <p>Used by the trophy road for its rewards; the inventory (V11) can use it for decorations in
 * the aquarium. The motion is only drawing: it changes nothing in the game state.
 */
public class MovingPicture extends Widget {

    /** How the picture moves. */
    public enum Motion {
        /** Does not move. */
        STILL,
        /** Bends like a plant in the water: the bottom stays, the top sways most. */
        BEND,
        /** Floats slowly up and down. */
        BOB,
        /** Tilts a little to both sides around its bottom center. */
        TILT,
        /** Swims on the spot: small moves to the sides and up and down. */
        SWIM
    }

    private static final int STRIPS = 32;
    private static final float BEND_AMOUNT = 0.06f;
    private static final float BOB_AMOUNT = 0.04f;
    private static final float TILT_DEGREES = 6f;
    private static final float SWIM_AMOUNT = 0.05f;
    private static final float SECONDS_PER_WAVE = 2.6f;
    private static final float OPEN_SECONDS = 1.8f;
    private static final float CLOSED_SECONDS = 1.2f;
    private static final int BUBBLES = 3;
    private static final float BUBBLE_SIZE = 0.07f;
    private static final float BUBBLE_RISE = 0.55f;
    private static final float BUBBLE_SECONDS = 2.4f;

    private final TextureRegion main;
    private final Motion motion;
    private TextureRegion other;
    private Texture bubble;
    private float bubbleX;
    private float bubbleY;
    private float time;

    /**
     * @param picture the image; drawn into the size the layout gives this widget
     * @param motion  how it moves
     */
    public MovingPicture(Texture picture, Motion motion) {
        this.main = new TextureRegion(picture);
        this.motion = motion;
        // Pictures next to each other should not move in step.
        this.time = (System.identityHashCode(this) % 1000) / 1000f * SECONDS_PER_WAVE;
    }

    /** Switches between the picture and this one, for example an open and a closed chest. */
    public MovingPicture alternateWith(Texture closed) {
        this.other = new TextureRegion(closed);
        return this;
    }

    /**
     * Lets small bubbles rise from a point of the picture.
     *
     * @param x from 0 (left) to 1 (right)
     * @param y from 0 (bottom) to 1 (top)
     */
    public MovingPicture bubbles(Texture bubbleTexture, float x, float y) {
        this.bubble = bubbleTexture;
        this.bubbleX = x;
        this.bubbleY = y;
        return this;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
    }

    @Override
    public float getPrefWidth() {
        return main.getRegionWidth();
    }

    @Override
    public float getPrefHeight() {
        return main.getRegionHeight();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        Color tint = getColor();
        batch.setColor(tint.r, tint.g, tint.b, tint.a * parentAlpha);
        float w = getWidth();
        float h = getHeight();
        float wave = time / SECONDS_PER_WAVE * MathUtils.PI2;
        TextureRegion picture = current();
        switch (motion) {
            case BEND -> drawBent(batch, picture, w, h, wave);
            case BOB -> batch.draw(picture, getX(), getY() + MathUtils.sin(wave) * BOB_AMOUNT * h, w, h);
            case TILT -> batch.draw(picture, getX(), getY(), w / 2f, 0f, w, h, 1f, 1f,
                    MathUtils.sin(wave) * TILT_DEGREES);
            case SWIM -> batch.draw(picture, getX() + MathUtils.sin(wave * 0.5f) * SWIM_AMOUNT * w,
                    getY() + MathUtils.sin(wave) * SWIM_AMOUNT * h * 0.6f, w / 2f, h / 2f, w, h, 1f, 1f,
                    MathUtils.sin(wave) * 3f);
            default -> batch.draw(picture, getX(), getY(), w, h);
        }
        if (bubble != null) {
            drawBubbles(batch, w, h, tint.a * parentAlpha);
        }
        batch.setColor(Color.WHITE);
    }

    private TextureRegion current() {
        if (other == null) {
            return main;
        }
        float cycle = time % (OPEN_SECONDS + CLOSED_SECONDS);
        return cycle < OPEN_SECONDS ? main : other;
    }

    /** Draws the picture in thin horizontal strips; higher strips move further to the side. */
    private void drawBent(Batch batch, TextureRegion picture, float w, float h, float wave) {
        Texture texture = picture.getTexture();
        int srcW = picture.getRegionWidth();
        int srcH = picture.getRegionHeight();
        float stripH = h / STRIPS;
        for (int i = 0; i < STRIPS; i++) {
            float k = (i + 0.5f) / STRIPS;
            float dx = MathUtils.sin(wave + k * 2f) * BEND_AMOUNT * w * (float) Math.pow(k, 1.6);
            int srcY = picture.getRegionY() + srcH - (i + 1) * srcH / STRIPS;
            int srcStrip = (i + 1) * srcH / STRIPS - i * srcH / STRIPS;
            batch.draw(texture, getX() + dx, getY() + i * stripH, w, stripH,
                    picture.getRegionX(), srcY, srcW, srcStrip, false, false);
        }
    }

    private void drawBubbles(Batch batch, float w, float h, float alpha) {
        float size = BUBBLE_SIZE * w;
        for (int i = 0; i < BUBBLES; i++) {
            float life = ((time / BUBBLE_SECONDS) + i / (float) BUBBLES) % 1f;
            float x = getX() + bubbleX * w + MathUtils.sin(life * 9f + i) * size * 0.4f - size / 2f;
            float y = getY() + bubbleY * h + life * BUBBLE_RISE * h;
            float s = size * (0.6f + 0.4f * life);
            batch.setColor(1f, 1f, 1f, alpha * (1f - life) * 0.9f);
            batch.draw(bubble, x, y, s, s);
        }
    }
}

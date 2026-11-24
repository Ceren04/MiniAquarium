package com.miniakvaryum.game.feature.energy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.miniakvaryum.domain.feature.energy.api.EnergyService;
import com.miniakvaryum.game.core.ui.ShapeTextures;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.util.Objects;

/**
 * The energy display in the top bar: a lightning bolt, a bar that fills from left to right, and
 * the number ("Energy 45"). At 100 the bar glows softly.
 *
 * <p>The bolt is {@code assets/icons/energy.png} if that file exists, otherwise two yellow
 * triangles drawn in code.
 */
final class EnergyBar extends Table {

    private static final float BAR_WIDTH = 150f;
    private static final float BAR_HEIGHT = 22f;
    private static final float BOLT_WIDTH = 24f;
    private static final float BOLT_HEIGHT = 32f;
    private static final Color BOLT = Color.valueOf("FDD835");

    private final EnergyService energy;
    private final Label number;
    private int shown = -1;

    EnergyBar(Skin skin, ShapeTextures shapes, EnergyService energy) {
        this.energy = Objects.requireNonNull(energy, "energy");
        number = new Label("", skin);
        add(bolt(shapes)).size(BOLT_WIDTH, BOLT_HEIGHT).padRight(6);
        add(new Meter(skin.getDrawable(UiSkin.WHITE), energy)).size(BAR_WIDTH, BAR_HEIGHT).padRight(10);
        add(number);
        refresh();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        refresh();
    }

    private void refresh() {
        int value = energy.energy();
        if (value != shown) {
            shown = value;
            number.setText(EnergyStrings.ENERGY + value);
        }
    }

    /** The lightning bolt: the image file if present, otherwise two triangles. */
    private static Actor bolt(ShapeTextures shapes) {
        Texture file = shapes.fileOr("icons/energy.png", () -> null);
        if (file != null) {
            return new Image(file);
        }
        Group group = new Group();
        group.setSize(BOLT_WIDTH, BOLT_HEIGHT);
        Texture wedge = shapes.triangle(14, 20, BOLT, ShapeTextures.Tip.UP);
        Image upper = new Image(wedge);
        upper.setOrigin(7, 10);
        upper.setRotation(200);
        upper.setPosition(7, 13);
        Image lower = new Image(wedge);
        lower.setOrigin(7, 10);
        lower.setRotation(200);
        lower.setPosition(1, 1);
        group.addActor(upper);
        group.addActor(lower);
        return group;
    }

    /** The bar itself: dark background, filled part, and a soft glow when full. */
    private static final class Meter extends Widget {
        private static final Color BACK = new Color(0f, 0f, 0f, 0.35f);
        private static final Color FILL = Color.valueOf("FF9F2E");
        private static final Color FULL = Color.valueOf("FDD835");
        private static final float GLOW_SPEED = 3f;
        private static final float GLOW_ALPHA = 0.35f;

        private final Drawable white;
        private final EnergyService energy;
        private float time;

        Meter(Drawable white, EnergyService energy) {
            this.white = white;
            this.energy = energy;
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            time += delta;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Color old = batch.getColor().cpy();
            float x = getX();
            float y = getY();
            float w = getWidth();
            float h = getHeight();
            batch.setColor(BACK.r, BACK.g, BACK.b, BACK.a * parentAlpha);
            white.draw(batch, x, y, w, h);

            float ratio = MathUtils.clamp((float) (energy.exactValue() / energy.max()), 0f, 1f);
            boolean full = energy.energy() >= energy.max();
            Color fill = full ? FULL : FILL;
            batch.setColor(fill.r, fill.g, fill.b, parentAlpha);
            white.draw(batch, x + 2, y + 2, (w - 4) * ratio, h - 4);

            if (full) {
                float glow = (MathUtils.sin(time * GLOW_SPEED) + 1f) / 2f * GLOW_ALPHA;
                batch.setColor(1f, 1f, 1f, glow * parentAlpha);
                white.draw(batch, x, y, w, h);
            }
            batch.setColor(old);
        }

        @Override
        public float getPrefWidth() {
            return BAR_WIDTH;
        }

        @Override
        public float getPrefHeight() {
            return BAR_HEIGHT;
        }
    }
}

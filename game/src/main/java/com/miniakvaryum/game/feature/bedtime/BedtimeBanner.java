package com.miniakvaryum.game.feature.bedtime;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.miniakvaryum.game.core.ui.ShapeTextures;
import com.miniakvaryum.game.core.ui.UiSkin;

/**
 * "Goodnight, little fish!" and "Good morning, little fish!": a card that slides up from the
 * bottom of the aquarium, stays about three seconds and slides away; a tap closes it at once.
 *
 * <p>This is the simple version of V12 (moon and stars, or a rising sun, drawn in code). If
 * {@code assets/bedtime/goodnight.png} or {@code goodmorning.png} exists (V4), it is used as
 * the background instead. The card only moves while the main screen is shown, so a greeting
 * that comes while the child is on another screen plays when the child comes back.
 */
final class BedtimeBanner extends Group {

    /** Which greeting. */
    enum Kind { GOODNIGHT, GOOD_MORNING }

    static final float WIDTH = 760f;
    static final float HEIGHT = 160f;
    private static final float SHOWN_Y = 30f;
    private static final float SLIDE_SECONDS = 0.45f;
    private static final float STAY_SECONDS = 3f;
    private static final float DRAWN_TEXT_X = 150f;
    private static final float IMAGE_TEXT_X = 270f;
    private static final float TEXT_RIGHT_MARGIN = 30f;

    private static final Color NIGHT = Color.valueOf("0B2944");
    private static final Color MOON = Color.valueOf("F2B84B");
    private static final Color MORNING = Color.valueOf("FFE3A8");
    private static final Color SUN = Color.valueOf("FF9F2E");
    private static final Color SKY = Color.valueOf("8ACBEE");

    private final Skin skin;
    private final ShapeTextures shapes;
    private final float centerX;
    private boolean showing;

    BedtimeBanner(Skin skin, ShapeTextures shapes, float worldWidth) {
        this.skin = skin;
        this.shapes = shapes;
        this.centerX = (worldWidth - WIDTH) / 2f;
        setSize(WIDTH, HEIGHT);
        setPosition(centerX, -HEIGHT);
        setTouchable(Touchable.disabled);
        setVisible(false);
    }

    /** Slides the greeting in; replaces a greeting that is still showing. */
    void show(Kind kind) {
        clearActions();
        clearChildren();
        build(kind);
        showing = true;
        setVisible(true);
        setPosition(centerX, -HEIGHT);
        addAction(Actions.sequence(
                Actions.moveTo(centerX, SHOWN_Y, SLIDE_SECONDS, Interpolation.swingOut),
                Actions.delay(STAY_SECONDS),
                Actions.run(this::hide)));
    }

    /** Slides the greeting away. */
    void hide() {
        if (!showing) {
            return;
        }
        showing = false;
        clearActions();
        addAction(Actions.sequence(
                Actions.moveTo(centerX, -HEIGHT, SLIDE_SECONDS, Interpolation.pow2In),
                Actions.visible(false)));
    }

    boolean isShowing() {
        return showing;
    }

    /** A tap on the card closes it. */
    boolean handleTap(float x, float y) {
        if (showing && x >= getX() && x <= getX() + WIDTH && y >= getY() && y <= getY() + HEIGHT) {
            hide();
            return true;
        }
        return false;
    }

    private void build(Kind kind) {
        boolean night = kind == Kind.GOODNIGHT;
        String file = night ? "bedtime/goodnight.png" : "bedtime/goodmorning.png";
        Texture background = shapes.fileOr(file, () -> null);
        if (background != null) {
            background.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            add(new Image(background), 0, 0, WIDTH, HEIGHT);
        } else if (night) {
            add(new Image(shapes.rect(8, 8, NIGHT)), 0, 0, WIDTH, HEIGHT);
            // Crescent: a moon circle partly covered by a circle of the night color.
            add(new Image(shapes.circle(96, MOON)), 40, 32, 96, 96);
            add(new Image(shapes.circle(96, NIGHT)), 70, 48, 96, 96);
            float[][] stars = {{170, 128}, {150, 18}, {690, 132}, {730, 74}, {700, 20}, {20, 132}, {112, 10}};
            int n = 0;
            for (float[] star : stars) {
                Image dot = new Image(shapes.circle(8, Color.WHITE));
                add(dot, star[0], star[1], 8, 8);
                dot.addAction(Actions.forever(Actions.sequence(Actions.delay(0.25f * n++),
                        Actions.alpha(0.2f, 0.6f), Actions.alpha(1f, 0.6f))));
            }
            Label zzz = new Label("Z z z", skin);
            zzz.setColor(Color.WHITE);
            zzz.setFontScale(0.8f);
            add(zzz, 130, 110, 80, 30);
        } else {
            add(new Image(shapes.rect(8, 8, MORNING)), 0, 0, WIDTH, HEIGHT);
            add(new Image(shapes.rect(8, 8, SKY)), 0, 0, WIDTH, 34);
            // The sun rises behind the sea line.
            Image sun = new Image(shapes.circle(110, SUN));
            add(sun, 50, -40, 110, 110);
            sun.addAction(Actions.moveBy(0, 40, 1.2f, Interpolation.sineOut));
        }
        Label title = new Label(night ? BedtimeStrings.GOODNIGHT : BedtimeStrings.GOOD_MORNING, skin, UiSkin.TITLE);
        title.setAlignment(Align.center);
        title.setColor(night ? Color.WHITE : NIGHT);
        // The drawn backgrounds keep moon, sun and fish in the left third; the text goes right of it.
        float textX = background != null ? IMAGE_TEXT_X : DRAWN_TEXT_X;
        float textWidth = WIDTH - textX - TEXT_RIGHT_MARGIN;
        add(title, textX, 62, textWidth, 70);
        Label line = new Label(night ? BedtimeStrings.NIGHT_LINE : BedtimeStrings.MORNING_LINE, skin);
        line.setAlignment(Align.center);
        line.setColor(night ? new Color(1f, 1f, 1f, 0.85f) : NIGHT);
        add(line, textX, 26, textWidth, 36);
    }

    private void add(Actor actor, float x, float y, float width, float height) {
        actor.setBounds(x, y, width, height);
        actor.setTouchable(Touchable.disabled);
        addActor(actor);
    }
}

package com.miniakvaryum.game.feature.trophies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.miniakvaryum.domain.feature.trophies.api.TrophyState;
import com.miniakvaryum.game.core.ui.UiConfig;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.util.ArrayList;
import java.util.List;

/**
 * The present opens (plan "Hediye açılma sırası"), about 2.3 seconds:
 * <ol>
 *   <li>The present shakes and starts to shine.</li>
 *   <li>The lid flies off, light rays shine out of the box, confetti bursts out.</li>
 *   <li>The reward rises out of the box into the middle of a shell card.</li>
 *   <li>Its name appears, then "Collect".</li>
 * </ol>
 * A tap during the animation jumps to the end. The reward was written before this starts.
 */
final class AnimatedReveal implements RewardReveal {

    private static final float CENTER_X = UiConfig.VIEW_WIDTH / 2f;
    private static final float GIFT_WIDTH = 230f;
    private static final float GIFT_BOTTOM = 270f;
    private static final float CARD_WIDTH = 380f;
    private static final float CARD_HEIGHT = 540f;
    private static final float PICTURE = 180f;
    private static final float PICTURE_Y = 500f;
    /** Bottom of the shell card; the card stands higher so "Collect" fits under it. */
    private static final float CARD_BOTTOM = 150f;
    private static final float BUTTON_WIDTH = 220f;
    private static final float RAYS = 620f;
    private static final float SHAKE_DEGREES = 7f;
    private static final float SHAKE_STEP = 0.07f;
    private static final int SHAKES = 5;
    private static final float OPEN_AT = SHAKES * 2 * SHAKE_STEP;
    private static final float SMALL_TEXT = 1.15f;
    private static final float TITLE_TEXT = 2.1f;
    private static final Color SHADE = new Color(0f, 0.05f, 0.12f, 0.78f);
    private static final Color TITLE = Color.valueOf("1C3F66");
    private static final Color NAME = Color.valueOf("23527F");
    private static final Color DETAIL = Color.valueOf("2E5C86");

    private final Skin skin;
    private final TrophyIcons icons;

    AnimatedReveal(Skin skin, TrophyIcons icons) {
        this.skin = skin;
        this.icons = icons;
    }

    @Override
    public void show(Stage stage, Content content, Runnable onClose) {
        Group layer = new Group();
        layer.setSize(UiConfig.VIEW_WIDTH, UiConfig.VIEW_HEIGHT);
        Image shade = new Image(skin.newDrawable(UiSkin.WHITE, SHADE));
        shade.setSize(UiConfig.VIEW_WIDTH, UiConfig.VIEW_HEIGHT);
        layer.addActor(shade);

        // Light rays behind everything; they turn slowly.
        Image rays = new Image(icons.rays());
        rays.setBounds(CENTER_X - RAYS / 2f, PICTURE_Y - RAYS / 2f, RAYS, RAYS);
        rays.setOrigin(Align.center);
        rays.setTouchable(Touchable.disabled);
        rays.getColor().a = 0f;
        rays.setScale(0.4f);
        layer.addActor(rays);

        // The shell card the reward lands on.
        Image card = new Image(icons.frame(TrophyState.CLAIMED));
        card.setBounds(CENTER_X - CARD_WIDTH / 2f, CARD_BOTTOM, CARD_WIDTH, CARD_HEIGHT);
        card.setOrigin(Align.center);
        card.getColor().a = 0f;
        layer.addActor(card);

        // The present.
        GiftPicture gift = icons.gift();
        float giftHeight = gift.heightFor(GIFT_WIDTH);
        gift.setBounds(CENTER_X - GIFT_WIDTH / 2f, GIFT_BOTTOM, GIFT_WIDTH, giftHeight);
        gift.layout();
        gift.setTransform(true);
        gift.setOrigin(GIFT_WIDTH / 2f, 0f);
        layer.addActor(gift);

        // The reward, hidden in the box until it rises.
        Actor picture = content.picture();
        picture.setBounds(CENTER_X - PICTURE / 2f, GIFT_BOTTOM, PICTURE, PICTURE);
        picture.setOrigin(Align.center);
        picture.getColor().a = 0f;
        picture.setScale(0.3f);
        layer.addActor(picture);

        Confetti confetti = new Confetti(icons);
        layer.addActor(confetti);

        // Texts and the button.
        List<Actor> texts = new ArrayList<>();
        Label title = label(content.title(), null, TITLE, TITLE_TEXT);
        place(title, 370f);
        texts.add(title);
        float y = 328f;
        if (content.name() != null) {
            Label name = label(content.name(), null, NAME, 1f);
            place(name, y);
            texts.add(name);
            y -= 38f;
        }
        Label detail = label(content.detail(), null, DETAIL, SMALL_TEXT);
        detail.setWrap(true);
        detail.setWidth(CARD_WIDTH - 90f);
        detail.setHeight(detail.getPrefHeight());
        detail.setPosition(CENTER_X - detail.getWidth() / 2f, y - detail.getHeight() / 2f);
        texts.add(detail);
        for (Actor text : texts) {
            text.getColor().a = 0f;
            layer.addActor(text);
        }
        TextButton close = new TextButton(TrophyStrings.COLLECT, skin);
        close.setBounds(CENTER_X - BUTTON_WIDTH / 2f, CARD_BOTTOM - UiConfig.MIN_TOUCH_SIZE - UiConfig.GAP * 2,
                BUTTON_WIDTH, UiConfig.MIN_TOUCH_SIZE);
        close.getColor().a = 0f;
        close.setTouchable(Touchable.disabled);
        close.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                layer.remove();
                onClose.run();
            }
        });
        layer.addActor(close);

        // 1. Shake and shine.
        SequenceAction shake = Actions.sequence();
        for (int i = 0; i < SHAKES; i++) {
            float angle = SHAKE_DEGREES * (0.5f + 0.5f * i / SHAKES);
            shake.addAction(Actions.rotateTo(angle, SHAKE_STEP));
            shake.addAction(Actions.rotateTo(-angle, SHAKE_STEP));
        }
        shake.addAction(Actions.rotateTo(0f, SHAKE_STEP / 2f));
        gift.addAction(shake);
        rays.addAction(Actions.parallel(Actions.alpha(0.35f, OPEN_AT), Actions.scaleTo(0.6f, 0.6f, OPEN_AT),
                Actions.forever(Actions.rotateBy(-25f, 1f))));

        // 2. Lid off, light, confetti.
        gift.lid().addAction(Actions.sequence(Actions.delay(OPEN_AT), Actions.parallel(
                Actions.moveBy(70f, 320f, 0.6f, Interpolation.pow2Out),
                Actions.rotateBy(-40f, 0.6f),
                Actions.fadeOut(0.6f))));
        rays.addAction(Actions.sequence(Actions.delay(OPEN_AT),
                Actions.parallel(Actions.alpha(1f, 0.3f), Actions.scaleTo(1.15f, 1.15f, 0.5f, Interpolation.swingOut))));
        layer.addAction(Actions.sequence(Actions.delay(OPEN_AT + 0.05f), Actions.run(
                () -> confetti.burst(CENTER_X, GIFT_BOTTOM + giftHeight * 0.6f))));

        // 3. The reward rises, the box sinks away, the card appears.
        float rise = OPEN_AT + 0.25f;
        picture.addAction(Actions.sequence(Actions.delay(rise), Actions.parallel(
                Actions.fadeIn(0.25f),
                Actions.scaleTo(1f, 1f, 0.6f, Interpolation.swingOut),
                Actions.moveTo(CENTER_X - PICTURE / 2f, PICTURE_Y - PICTURE / 2f, 0.6f, Interpolation.pow2Out))));
        gift.addAction(Actions.sequence(Actions.delay(rise + 0.2f),
                Actions.parallel(Actions.moveBy(0f, -60f, 0.4f), Actions.fadeOut(0.4f))));
        card.setScale(0.85f);
        card.addAction(Actions.sequence(Actions.delay(rise + 0.2f),
                Actions.parallel(Actions.fadeIn(0.35f), Actions.scaleTo(1f, 1f, 0.35f, Interpolation.swingOut))));

        // 4. Name, then "Collect".
        float textsAt = rise + 0.6f;
        for (int i = 0; i < texts.size(); i++) {
            texts.get(i).addAction(Actions.sequence(Actions.delay(textsAt + i * 0.12f), Actions.fadeIn(0.25f)));
        }
        Runnable finished = () -> close.setTouchable(Touchable.enabled);
        close.addAction(Actions.sequence(Actions.delay(textsAt + 0.4f), Actions.fadeIn(0.2f), Actions.run(finished)));

        // A tap during the animation jumps to the end.
        shade.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!close.isTouchable()) {
                    for (int i = 0; i < 40; i++) {
                        layer.act(0.1f);
                    }
                }
                return true;
            }
        });
        stage.addActor(layer);
    }

    private Label label(String text, String style, Color color, float scale) {
        Label label = style == null ? new Label(text, skin) : new Label(text, skin, style);
        label.setColor(color);
        if (scale != 1f) {
            label.setFontScale(scale);          // absolute scale; the normal text keeps the skin's size
        }
        label.setAlignment(Align.center);
        return label;
    }

    private static void place(Label label, float centerY) {
        label.pack();
        label.setPosition(CENTER_X - label.getWidth() / 2f, centerY - label.getHeight() / 2f);
    }

    /** Colored paper pieces that burst out of the box, fly, turn and fall. */
    private static final class Confetti extends Actor {

        private static final int PIECES = 90;
        private static final float GRAVITY = -900f;
        private static final float LIFE = 2.2f;
        private static final Color[] COLORS = {
            Color.valueOf("FF6FA5"), Color.valueOf("FFD45E"), Color.valueOf("4FD1C5"),
            Color.valueOf("FF9F43"), Color.valueOf("A78BFA"), Color.WHITE};

        private final TrophyIcons icons;
        private final float[] x = new float[PIECES];
        private final float[] y = new float[PIECES];
        private final float[] vx = new float[PIECES];
        private final float[] vy = new float[PIECES];
        private final float[] angle = new float[PIECES];
        private final float[] spin = new float[PIECES];
        private float age = -1f;

        Confetti(TrophyIcons icons) {
            this.icons = icons;
            setTouchable(Touchable.disabled);
        }

        void burst(float fromX, float fromY) {
            for (int i = 0; i < PIECES; i++) {
                x[i] = fromX + spread(i, 1, -30f, 30f);
                y[i] = fromY;
                vx[i] = spread(i, 2, -520f, 520f);
                vy[i] = spread(i, 3, 380f, 900f);
                angle[i] = spread(i, 4, 0f, 360f);
                spin[i] = spread(i, 5, -720f, 720f);
            }
            age = 0f;
        }

        /**
         * A fixed mix of values between from and to (an integer hash of piece and kind): looks
         * random, but is the same every time; dice belong to the domain rules, not to the picture.
         */
        private static float spread(int piece, int kind, float from, float to) {
            int h = piece * 374_761_393 + kind * 668_265_263;
            h = (h ^ (h >>> 13)) * 1_274_126_177;
            h ^= h >>> 16;
            return from + (to - from) * ((h & 0xFFFFFF) / 16_777_216f);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            if (age < 0f || age > LIFE) {
                return;
            }
            age += delta;
            for (int i = 0; i < PIECES; i++) {
                vy[i] += GRAVITY * delta;
                vx[i] *= 1f - 0.9f * delta;
                x[i] += vx[i] * delta;
                y[i] += vy[i] * delta;
                angle[i] += spin[i] * delta;
            }
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (age < 0f || age > LIFE) {
                return;
            }
            float fade = Math.min(1f, (LIFE - age) / 0.6f);
            for (int i = 0; i < PIECES; i++) {
                Color c = COLORS[i % COLORS.length];
                batch.setColor(c.r, c.g, c.b, fade * parentAlpha);
                batch.draw(icons.confetti(), x[i], y[i], 6f, 3.5f, 12f, 7f, 1f, 1f, angle[i], 0, 0, 12, 7,
                        false, false);
            }
            batch.setColor(Color.WHITE);
        }
    }
}

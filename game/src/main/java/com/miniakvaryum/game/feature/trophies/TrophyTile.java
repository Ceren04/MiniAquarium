package com.miniakvaryum.game.feature.trophies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.miniakvaryum.domain.feature.trophies.api.Milestone;
import com.miniakvaryum.domain.feature.trophies.api.TrophyState;
import com.miniakvaryum.game.core.ui.UiSkin;

/**
 * One trophy on the road. Locked: gray trophy, its reward and "3 more tasks". Ready: a present
 * that breathes; a tap opens it. Collected: gold trophy, its reward and "Collected".
 */
final class TrophyTile extends Table {

    static final float WIDTH = 170f;
    static final float HEIGHT = 260f;
    private static final float ICON = 88f;
    private static final float REWARD = 64f;
    private static final Color LOCKED_REWARD = new Color(1f, 1f, 1f, 0.55f);
    private static final float SMALL_TEXT = 1.15f;
    private static final float PULSE = 1.12f;
    private static final float PULSE_SECONDS = 0.7f;
    private static final Color LOCKED = new Color(1f, 1f, 1f, 0.08f);
    private static final Color READY = Color.valueOf("2F7FB5");
    private static final Color CLAIMED = new Color(1f, 1f, 1f, 0.16f);
    private static final Color GOLD = Color.valueOf("F2B84B");
    private static final Color GREEN = Color.valueOf("8EE39B");
    private static final Color HINT = new Color(1f, 1f, 1f, 0.7f);

    TrophyTile(Skin skin, TrophyIcons icons, Milestone milestone, TrophyState state, int approved, Runnable onOpen) {
        int threshold = milestone.threshold();
        setBackground(skin.newDrawable(UiSkin.WHITE,
                state == TrophyState.READY ? READY : state == TrophyState.CLAIMED ? CLAIMED : LOCKED));
        pad(8f);

        add(small(skin, threshold == 1 ? TrophyStrings.ONE_TASK : String.format(TrophyStrings.TASKS, threshold),
                HINT)).row();

        Image icon = new Image(state == TrophyState.READY ? icons.gift()
                : state == TrophyState.CLAIMED ? icons.trophy() : icons.locked());
        Container<Image> iconBox = new Container<>(icon).size(ICON);
        add(iconBox).size(ICON).padTop(4f).row();

        // The reward is visible in every state, so the child knows what comes (plan).
        Actor picture = icons.picture(milestone.reward());
        if (state == TrophyState.LOCKED) {
            picture.setColor(LOCKED_REWARD);
        }
        add(picture).size(REWARD).padTop(4f).row();

        Label reward = small(skin, TrophyStrings.reward(milestone.reward()), Color.WHITE);
        reward.setWrap(true);
        reward.setAlignment(Align.center);
        add(reward).width(WIDTH - 16f).padTop(2f).row();

        String status;
        Color color;
        if (state == TrophyState.LOCKED) {
            int missing = threshold - approved;
            status = missing == 1 ? TrophyStrings.ONE_MORE_TASK : String.format(TrophyStrings.MORE_TASKS, missing);
            color = HINT;
        } else if (state == TrophyState.READY) {
            status = TrophyStrings.TAP_TO_OPEN;
            color = GOLD;
        } else {
            status = TrophyStrings.COLLECTED;
            color = GREEN;
        }
        add(small(skin, status, color)).expandY().bottom();

        if (state == TrophyState.READY) {
            iconBox.setTransform(true);
            iconBox.setOrigin(ICON / 2f, ICON / 2f);
            iconBox.addAction(Actions.forever(Actions.sequence(
                    Actions.scaleTo(PULSE, PULSE, PULSE_SECONDS, Interpolation.sine),
                    Actions.scaleTo(1f, 1f, PULSE_SECONDS, Interpolation.sine))));
            setTouchable(Touchable.enabled);
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onOpen.run();
                }
            });
        }
    }

    private static Label small(Skin skin, String text, Color color) {
        Label label = new Label(text, skin);
        label.setFontScale(SMALL_TEXT);
        label.setColor(color);
        return label;
    }
}

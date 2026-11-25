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
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.miniakvaryum.domain.feature.trophies.api.Milestone;
import com.miniakvaryum.domain.feature.trophies.api.TrophyState;

/**
 * One trophy on the road, in its sea frame. The reward is a surprise until it is collected:
 * <ul>
 *   <li>Locked (stone frame): gray trophy and "3 more tasks".</li>
 *   <li>Ready (golden coral frame): a present that breathes; a tap opens it.</li>
 *   <li>Collected (shell frame): gold trophy, the reward's picture and name, "Collected".</li>
 * </ul>
 */
final class TrophyTile extends Stack {

    static final float WIDTH = 182f;
    static final float HEIGHT = 260f;
    /** Space for the frame's border, so the content stays on the flat inside. */
    private static final float BORDER_SIDE = 24f;
    private static final float BORDER_TOP = 26f;
    private static final float BORDER_BOTTOM = 28f;
    private static final float BIG_ICON = 104f;
    private static final float SMALL_ICON = 62f;
    /** Width of the present; its lid is a little wider and it breathes up to 10 % bigger. */
    private static final float GIFT = 86f;
    private static final float REWARD = 58f;
    private static final float SMALL_TEXT = 1.1f;
    private static final float PULSE = 1.1f;
    private static final float PULSE_SECONDS = 0.7f;
    private static final Color GOLD = Color.valueOf("FFE08A");
    private static final Color GREEN = Color.valueOf("FFFFFF");
    private static final Color HINT = new Color(1f, 1f, 1f, 0.85f);

    TrophyTile(Skin skin, TrophyIcons icons, Milestone milestone, TrophyState state, int approved, Runnable onOpen) {
        add(new Image(icons.frame(state)));
        Table content = new Table();
        content.pad(BORDER_TOP, BORDER_SIDE, BORDER_BOTTOM, BORDER_SIDE);
        add(content);

        int threshold = milestone.threshold();
        content.add(small(skin, threshold == 1 ? TrophyStrings.ONE_TASK : String.format(TrophyStrings.TASKS, threshold),
                HINT)).row();
        float inner = WIDTH - 2 * BORDER_SIDE;

        if (state == TrophyState.LOCKED) {
            content.add(new Image(icons.locked())).size(BIG_ICON).expandY().row();
            int missing = threshold - approved;
            content.add(small(skin, missing == 1 ? TrophyStrings.ONE_MORE_TASK
                    : String.format(TrophyStrings.MORE_TASKS, missing), HINT));
        } else if (state == TrophyState.READY) {
            GiftPicture gift = icons.gift();
            float giftHeight = gift.heightFor(GIFT);
            Container<GiftPicture> box = new Container<>(gift).size(GIFT, giftHeight);
            box.setTransform(true);
            content.add(box).size(GIFT, giftHeight).expandY().row();
            content.add(small(skin, TrophyStrings.TAP_TO_OPEN, GOLD));
            box.setOrigin(GIFT / 2f, giftHeight / 2f);
            box.addAction(Actions.forever(Actions.sequence(
                    Actions.scaleTo(PULSE, PULSE, PULSE_SECONDS, Interpolation.sine),
                    Actions.scaleTo(1f, 1f, PULSE_SECONDS, Interpolation.sine))));
            setTouchable(Touchable.enabled);
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onOpen.run();
                }
            });
        } else {
            content.add(new Image(icons.trophy())).size(SMALL_ICON).row();
            Actor picture = icons.picture(milestone.reward());
            content.add(picture).size(REWARD).padTop(2f).row();
            Label reward = small(skin, TrophyStrings.reward(milestone.reward()), Color.WHITE);
            reward.setWrap(true);
            reward.setAlignment(Align.center);
            content.add(reward).width(inner).expandY().row();
            content.add(small(skin, TrophyStrings.COLLECTED, GREEN));
        }
    }

    private static Label small(Skin skin, String text, Color color) {
        Label label = new Label(text, skin);
        label.setFontScale(SMALL_TEXT);
        label.setColor(color);
        label.setAlignment(Align.center);
        return label;
    }
}

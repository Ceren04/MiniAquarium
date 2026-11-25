package com.miniakvaryum.game.feature.trophies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.miniakvaryum.game.core.ui.UiConfig;
import com.miniakvaryum.game.core.ui.UiSkin;

/** The reward card of V12: darkened screen, a card that grows in, "Super!" closes it. */
final class SimpleReveal implements RewardReveal {

    private static final float CARD_WIDTH = 560f;
    private static final float CARD_HEIGHT = 470f;
    private static final float ICON = 190f;
    private static final float GROW_SECONDS = 0.35f;
    private static final float SMALL_TEXT = 1.2f;
    private static final Color SHADE = new Color(0f, 0f, 0f, 0.6f);
    private static final Color CARD = Color.valueOf("17507AFF");
    private static final Color GOLD = Color.valueOf("F2B84B");
    private static final Color HINT = new Color(1f, 1f, 1f, 0.75f);

    private final Skin skin;

    SimpleReveal(Skin skin) {
        this.skin = skin;
    }

    @Override
    public void show(Stage stage, Content content, Runnable onClose) {
        Table layer = new Table();
        layer.setFillParent(true);
        layer.setBackground(skin.newDrawable(UiSkin.WHITE, SHADE));
        layer.setTouchable(Touchable.enabled);

        Table card = new Table();
        card.setBackground(skin.newDrawable(UiSkin.WHITE, CARD));
        card.setTransform(true);
        card.pad(UiConfig.GAP * 2);
        card.add(content.picture()).size(ICON).row();
        Label title = new Label(content.title(), skin, UiSkin.TITLE);
        title.setColor(GOLD);
        card.add(title).padTop(UiConfig.GAP).row();
        if (content.name() != null) {
            card.add(new Label(content.name(), skin)).padTop(4).row();
        }
        Label detail = new Label(content.detail(), skin);
        detail.setFontScale(SMALL_TEXT);
        detail.setColor(HINT);
        detail.setAlignment(Align.center);
        detail.setWrap(true);
        card.add(detail).width(CARD_WIDTH - UiConfig.GAP * 4).padTop(UiConfig.GAP).row();
        TextButton close = new TextButton(TrophyStrings.SUPER, skin);
        close.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                layer.remove();
                onClose.run();
            }
        });
        card.add(close).size(200f, UiConfig.MIN_TOUCH_SIZE).padTop(UiConfig.GAP * 2).expandY().bottom();
        layer.add(card).size(CARD_WIDTH, CARD_HEIGHT);
        stage.addActor(layer);

        layer.validate();
        card.setOrigin(Align.center);
        card.setScale(0.5f);
        card.addAction(Actions.scaleTo(1f, 1f, GROW_SECONDS, Interpolation.swingOut));
    }
}

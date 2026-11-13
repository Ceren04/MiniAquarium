package com.miniakvaryum.game.core.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.miniakvaryum.game.core.CoreStrings;

/**
 * A screen that shows only a title, a "not implemented yet" note and a back button.
 *
 * <p>Skeleton screens extend it so that navigation already works. When you implement a screen:
 * change its superclass to {@link BaseScreen} and build the real widgets in its constructor.
 */
public abstract class PlaceholderScreen extends BaseScreen {

    /**
     * @param skin  look of the widgets
     * @param nav   used by the back button
     * @param title headline of the screen
     */
    protected PlaceholderScreen(Skin skin, Navigator nav, String title) {
        super(skin);
        Table root = newRootTable();
        root.add(new Label(title, skin, UiSkin.TITLE)).padBottom(UiConfig.GAP * 4).row();
        root.add(new Label(CoreStrings.NOT_IMPLEMENTED, skin)).padBottom(UiConfig.GAP * 4).row();
        root.add(button(CoreStrings.BACK, nav::showMain)).minSize(UiConfig.MIN_TOUCH_SIZE * 3, UiConfig.MIN_TOUCH_SIZE);
    }
}

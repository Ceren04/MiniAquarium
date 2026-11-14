package com.miniakvaryum.game.feature.devtools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.miniakvaryum.game.core.ui.UiSkin;

/**
 * The developer bar: a strip along the bottom of the world with the dev buttons. It is hidden
 * until the toggle key is pressed.
 *
 * <p>The bar only draws. Its buttons do not receive touches themselves (they are not
 * touchable); the feature forwards world taps to {@link #handleTap}, which keeps the click logic
 * in one place and independent of how the stage distributes touches.
 */
final class DevBar extends Group {

    /** The secret key. A function key has the same position on every keyboard layout. */
    static final int TOGGLE_KEY = Input.Keys.F9;

    private static final float BAR_HEIGHT = 80f;
    private static final float BUTTON_X = 20f;
    private static final float BUTTON_Y = 8f;
    private static final float BUTTON_WIDTH = 220f;
    private static final float BUTTON_HEIGHT = 64f;
    private static final Color STRIP_COLOR = new Color(0.55f, 0.10f, 0.10f, 0.65f);

    /** Everything that is shown or hidden together. */
    private final Group panel = new Group();
    private final Rectangle addFoodArea = new Rectangle(BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
    private final Runnable onAddFood;

    DevBar(Skin skin, float worldWidth, Runnable onAddFood) {
        this.onAddFood = onAddFood;

        Image strip = new Image(skin.newDrawable(UiSkin.WHITE, STRIP_COLOR));
        strip.setBounds(0, 0, worldWidth, BAR_HEIGHT);
        strip.setTouchable(Touchable.disabled);
        panel.addActor(strip);

        TextButton addFood = new TextButton(DevToolsStrings.ADD_FOOD, skin);
        addFood.setBounds(BUTTON_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        addFood.setTouchable(Touchable.disabled);
        panel.addActor(addFood);

        panel.setVisible(false);
        addActor(panel);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (Gdx.input.isKeyJustPressed(TOGGLE_KEY)) {
            panel.setVisible(!panel.isVisible());
        }
    }

    /**
     * Handles a tap in world coordinates.
     *
     * @return true if the tap hit a dev button; the tap is then used up
     */
    boolean handleTap(float x, float y) {
        if (!panel.isVisible() || !addFoodArea.contains(x, y)) {
            return false;
        }
        onAddFood.run();
        return true;
    }
}
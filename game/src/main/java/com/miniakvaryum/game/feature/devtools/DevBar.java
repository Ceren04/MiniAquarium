package com.miniakvaryum.game.feature.devtools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The developer bar: a strip along the bottom of the world with the dev buttons, laid out left
 * to right, and a status text right after the last button. It is hidden until the toggle key is pressed.
 *
 * <p>The bar only draws. Its buttons do not receive touches themselves (they are not
 * touchable); the feature forwards world taps to {@link #handleTap}, which keeps the click logic
 * in one place and independent of how the stage distributes touches.
 */
final class DevBar extends Group {

    /** The secret key. A function key has the same position on every keyboard layout. */
    static final int TOGGLE_KEY = Input.Keys.F9;

    private static final float BAR_HEIGHT = 80f;
    private static final float FIRST_BUTTON_X = 20f;
    private static final float BUTTON_Y = 8f;
    private static final float BUTTON_HEIGHT = 64f;
    private static final float GAP = 12f;
    private static final Color STRIP_COLOR = new Color(0.55f, 0.10f, 0.10f, 0.65f);

    private record Button(Rectangle area, Runnable action) {
    }

    /** Everything that is shown or hidden together. */
    private final Group panel = new Group();
    private final Skin skin;
    private final List<Button> buttons = new ArrayList<>();
    private final Label status;
    private final Supplier<String> statusText;
    private float nextButtonX = FIRST_BUTTON_X;

    /**
     * @param statusText text on the right end of the bar, refreshed every frame while visible
     */
    DevBar(Skin skin, float worldWidth, Supplier<String> statusText) {
        this.skin = skin;
        this.statusText = statusText;

        Image strip = new Image(skin.newDrawable(UiSkin.WHITE, STRIP_COLOR));
        strip.setBounds(0, 0, worldWidth, BAR_HEIGHT);
        strip.setTouchable(Touchable.disabled);
        panel.addActor(strip);

        status = new Label("", skin);
        status.setTouchable(Touchable.disabled);
        // Inside the strip, right after the last button (moved along in addButton).
        status.setPosition(nextButtonX, BUTTON_Y + BUTTON_HEIGHT / 2f - 12f);
        panel.addActor(status);

        panel.setVisible(false);
        addActor(panel);
    }

    /**
     * Adds a button to the right of the previous one.
     *
     * @return the button, so its label can change
     */
    TextButton addButton(String text, float width, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.setBounds(nextButtonX, BUTTON_Y, width, BUTTON_HEIGHT);
        button.setTouchable(Touchable.disabled);
        panel.addActor(button);
        buttons.add(new Button(new Rectangle(nextButtonX, BUTTON_Y, width, BUTTON_HEIGHT), action));
        nextButtonX += width + GAP;
        status.setX(nextButtonX);
        return button;
    }

    /** Adds something that is shown and hidden together with the bar, like the date picker. */
    void addToPanel(Actor actor) {
        panel.addActor(actor);
    }

    /** True while the bar is visible. */
    boolean isShown() {
        return panel.isVisible();
    }

    /** Shows or hides the bar. */
    void setShown(boolean shown) {
        panel.setVisible(shown);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (Gdx.input.isKeyJustPressed(TOGGLE_KEY)) {
            panel.setVisible(!panel.isVisible());
        }
        if (panel.isVisible()) {
            status.setText(statusText.get());
        }
    }

    /**
     * Handles a tap in world coordinates.
     *
     * @return true if the tap hit a dev button; the tap is then used up
     */
    boolean handleTap(float x, float y) {
        if (!panel.isVisible()) {
            return false;
        }
        for (Button button : buttons) {
            if (button.area().contains(x, y)) {
                button.action().run();
                return true;
            }
        }
        return false;
    }
}

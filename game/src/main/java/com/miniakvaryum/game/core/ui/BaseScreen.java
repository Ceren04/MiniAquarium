package com.miniakvaryum.game.core.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.miniakvaryum.game.core.CoreStrings;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class of every screen. It owns a Scene2D {@link Stage} with the shared 1280 x 720
 * viewport, routes input to it, draws it every frame and offers three helpers that all screens
 * need: {@link #button}, {@link #showMessage} and {@link #guarded}.
 *
 * <p>To write a screen: extend this class, keep the {@code UiContext} in a field, build the
 * widgets in the constructor (add them to
 * {@link #stage} or to {@link #newRootTable()}), and refresh changing content in
 * {@link #onShow()} or {@link #update(float)}.
 *
 * <p>The game rules are advanced by {@code MiniAkvaryumGame} before the screen is drawn, on
 * every screen. A screen only reads domain state and calls domain services.
 */
public abstract class BaseScreen extends ScreenAdapter {

    private static final Logger LOG = Logger.getLogger(BaseScreen.class.getName());
    private static final Color DEFAULT_BACKGROUND = new Color(0.05f, 0.16f, 0.27f, 1f);

    protected final Skin skin;
    protected final Stage stage;

    private final Label message;
    private float messageSecondsLeft;

    /**
     * @param skin look of the widgets, normally {@code ui.skin()}
     */
    protected BaseScreen(Skin skin) {
        this.skin = Objects.requireNonNull(skin, "skin");
        this.stage = new Stage(new FitViewport(UiConfig.VIEW_WIDTH, UiConfig.VIEW_HEIGHT));

        message = new Label("", skin, UiSkin.MESSAGE);
        message.setAlignment(Align.center);
        message.setTouchable(Touchable.disabled);
        message.setVisible(false);
    }

    // ------------------------------------------------------------ for subclasses

    /** Called every time this screen becomes visible. Refresh lists and texts here. */
    protected void onShow() {
    }

    /** Called every frame before drawing. Update texts that change continuously here. */
    protected void update(float delta) {
    }

    /** Color behind the widgets. */
    protected Color background() {
        return DEFAULT_BACKGROUND;
    }

    /** Creates a table that fills the screen and adds it to the stage: the usual layout root. */
    protected Table newRootTable() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(UiConfig.GAP * 2);
        stage.addActor(root);
        return root;
    }

    /**
     * Creates a button that is big enough to touch and runs {@code action} when clicked.
     * The action is {@link #guarded}.
     */
    protected TextButton button(String text, Runnable action) {
        return button(text, "default", action);
    }

    /** Like {@link #button(String, Runnable)} with another style of the skin. */
    protected TextButton button(String text, String styleName, Runnable action) {
        TextButton button = new TextButton(text, skin, styleName);
        button.pad(0, UiConfig.GAP * 2, 0, UiConfig.GAP * 2);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                guarded(action);
            }
        });
        return button;
    }

    /** Shows a short text at the bottom of the screen for the given number of seconds. */
    public void showMessage(String text, float seconds) {
        message.setText(text);
        message.setVisible(true);
        // Bring to front; a box just big enough for the text, centered near the bottom edge.
        stage.addActor(message);
        message.pack();
        float width = message.getWidth() + UiConfig.GAP * 4;
        message.setBounds((UiConfig.VIEW_WIDTH - width) / 2, UiConfig.GAP * 2, width, UiConfig.MIN_TOUCH_SIZE);
        messageSecondsLeft = seconds;
    }

    /**
     * Runs an action that calls domain code. If that code is still a skeleton
     * ({@link UnsupportedOperationException} with a "TODO" text), the player sees a short message
     * instead of a crash. This is what keeps the game usable while features are being written.
     */
    protected void guarded(Runnable action) {
        try {
            action.run();
        } catch (UnsupportedOperationException e) {
            LOG.log(Level.INFO, "Skeleton code was called: {0}", e.getMessage());
            String detail = String.valueOf(e.getMessage()).replace("TODO: ", "").replace(" is not implemented yet", "");
            showMessage(CoreStrings.NOT_IMPLEMENTED + ": " + detail, 3f);
        }
    }

    // ------------------------------------------------------------ libGDX life cycle

    @Override
    public final void show() {
        Gdx.input.setInputProcessor(stage);
        onShow();
    }

    @Override
    public final void render(float delta) {
        update(delta);
        if (messageSecondsLeft > 0) {
            messageSecondsLeft -= delta;
            if (messageSecondsLeft <= 0) {
                message.setVisible(false);
            }
        }
        ScreenUtils.clear(background());
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}

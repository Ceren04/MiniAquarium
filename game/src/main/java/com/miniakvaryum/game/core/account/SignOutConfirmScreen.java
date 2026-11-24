package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.miniakvaryum.game.core.ui.BaseScreen;
import com.miniakvaryum.game.core.ui.UiConfig;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.util.Objects;

/**
 * Asks "Do you really want to sign out?" before signing out. "Yes" signs out, "No" (and
 * Escape) goes back without changing anything.
 */
public final class SignOutConfirmScreen extends BaseScreen {

    /** Screen id under which the game registers this screen. */
    public static final String ID = "sign-out-confirm";

    private static final float BUTTON_WIDTH = 240f;

    /**
     * @param skin  look of the widgets
     * @param onYes signs out
     * @param onNo  goes back to the game
     */
    public SignOutConfirmScreen(Skin skin, Runnable onYes, Runnable onNo) {
        super(skin);
        Objects.requireNonNull(onYes, "onYes");
        Objects.requireNonNull(onNo, "onNo");

        Table root = newRootTable();
        root.add(new Label(AccountStrings.SIGN_OUT, skin, UiSkin.TITLE)).padBottom(UiConfig.GAP * 2).row();
        root.add(new Label(AccountStrings.SIGN_OUT_QUESTION, skin)).padBottom(UiConfig.GAP * 5).row();
        Table buttons = new Table();
        buttons.add(button(AccountStrings.NO, onNo)).size(BUTTON_WIDTH, UiConfig.MIN_TOUCH_SIZE)
                .padRight(UiConfig.GAP * 3);
        buttons.add(button(AccountStrings.YES_SIGN_OUT, onYes)).size(BUTTON_WIDTH, UiConfig.MIN_TOUCH_SIZE);
        root.add(buttons);

        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    onNo.run();
                    return true;
                }
                return false;
            }
        });
    }
}

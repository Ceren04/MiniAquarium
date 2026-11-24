package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.miniakvaryum.domain.core.account.Account;
import com.miniakvaryum.game.core.ui.UiConfig;
import java.io.UncheckedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Sign-up step 4: greeting, picture, "Remember me" and "Start", which creates the account. */
final class AllSetScreen extends FlowScreen {

    private static final Logger LOG = Logger.getLogger(AllSetScreen.class.getName());
    private static final float PICTURE = 180f;

    private final Label greeting;
    private final Container<Actor> picture = new Container<>();
    private final TextButton remember;

    AllSetScreen(AccountFlow flow) {
        super(flow);
        greeting = text("");
        remember = rememberToggle();

        Table root = newRootTable();
        root.top();
        root.add(title(AccountStrings.ALL_SET)).padBottom(UiConfig.GAP).row();
        root.add(greeting).padBottom(UiConfig.GAP * 2).row();
        picture.size(PICTURE).fill();
        root.add(picture).size(PICTURE).padBottom(UiConfig.GAP * 3).row();
        root.add(remember).size(WIDE_BUTTON * 1.5f, UiConfig.MIN_TOUCH_SIZE).row();

        Table buttons = new Table();
        buttons.add(button(AccountStrings.BACK, () -> flow.show(AccountFlow.PICTURE)))
                .size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        buttons.add(small(String.format(AccountStrings.STEP, 4), HINT)).expandX();
        buttons.add(button(AccountStrings.START, this::start)).size(WIDE_BUTTON, UiConfig.MIN_TOUCH_SIZE);
        root.add(buttons).growX().expandY().bottom();
    }

    @Override
    protected void onShow() {
        greeting.setText(String.format(AccountStrings.WELCOME_NAME, flow.draft().firstName().trim()));
        picture.setActor(picture(flow.draft().picture(), flow.draft().firstName(), PICTURE));
        setRemember(remember, false);
    }

    private void start() {
        Account account;
        try {
            account = flow.accounts().create(flow.draft());
        } catch (UncheckedIOException e) {
            LOG.log(Level.WARNING, "Could not save the new account", e);
            showMessage(AccountStrings.SAVE_FAILED, 3f);
            return;
        } catch (IllegalArgumentException e) {
            // For example the username was taken in the meantime: back to the form, which shows why.
            flow.show(AccountFlow.ABOUT_YOU);
            return;
        }
        flow.finish(account, remember.isChecked());
    }
}
